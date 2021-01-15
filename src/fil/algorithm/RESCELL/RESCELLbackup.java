/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.RESCELL;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import fil.algorithm.routing.NetworkRouting;
import fil.resource.substrate.Rpi;
import fil.resource.virtual.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class RESCELLbackup  {
	
	final static int NUM_PI = 300;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int TOTAL_CAPACITY = 40000;
	final static int EDGE_CAPACITY = 30000;
	final static int CLOUD_CAPACITY = 25000;
	
	private Topology topo;
	private FatTree fatTree;
	private MappingServer mappingServer;
	private NetworkRouting coreNetwork;

	private LinkedList<Rpi> listRpi;
	private LinkedList<Integer> edgePosition;
	private Map<Rpi, LinkedList<SFC>> listSFConRpi;

	public RESCELLbackup() { // default constructor
		
		topo = new Topology();
		fatTree = new FatTree();
		topo = fatTree.genFatTree(K_PORT_SWITCH);
		mappingServer = new MappingServer();	
		coreNetwork = new NetworkRouting();
		
		listSFConRpi = new HashMap<>(NUM_PI); 
		listRpi = new LinkedList<Rpi>();
		
		edgePosition = new LinkedList<>();
		edgePosition.add(10);
		edgePosition.add(5);
		edgePosition.add(13);
		edgePosition.add(14);
		
		for(int i = 0; i < NUM_PI; i++ ) {
			Random rand = new Random ();
			int position = rand.nextInt(4);
			Rpi rpi = new Rpi(i, edgePosition.get(position));
			listRpi.add(rpi);
			listSFConRpi.put(rpi, new LinkedList<SFC>());
		}
		

	}
	

	public void VNFMapping( Topology topo, MappingServer mappingServer, NetworkRouting coreNetwork, Rpi pi, LinkedList<Double> listChainRequest, 
			LinkedList<SFC> listSFConRpi,
			LinkedList<SFC> listSFCFinalPi, IntHolder numSFCTW) {
		//<---- Global variable initiation
		
		
		//<---- local variables - deleted after getting out of this function
		
		boolean doneFlag = false;
		boolean remapPi = false;
		boolean doneMap = false;
		int SFCIndexIncrease = 0;
		int remapLoopCount = 0;
		int numChainSuccessCur = 0;
		double minPower = Integer.MAX_VALUE;
		Capture capture = new Capture();
		Decode decode = new Decode();
		Density density = new Density();
		Receive receive = new Receive();
		
		//<---- count VNF migration
		
		
		MAP_LOOP:
			while (doneFlag == false) {////////////////////TRIAL LOOP////////////////////////
				
				if (remapPi == true && doneMap == false){ //remapp
					System.out.println("Inside remapping edge device number " + remapLoopCount);
					if (remapLoopCount == 0) {
						numSFCTW.value -= listSFConRpi.size();
//						numChainSuccessPre = listSFConRpi.size();
//						numChainRequest += listRpiSFC.size(); //sum of all previous successful mapping chain
						for(SFC sfc : listSFConRpi) {
							listChainRequest.addFirst(sfc.getEndTime());; // add first position 
						}
					}
					if(!listSFConRpi.isEmpty()) {
						coreNetwork.reset(pi); // reset network topology
						mappingServer.getServiceMapping().resetRpiSFC(listSFConRpi, topo); // reset mapped chains pi without remapping at server
						for(SFC sfc : listSFConRpi) { // remove all sfc belongs to pi in listSFCTotal 
							if(mappingServer.getListSFCTotal().contains(sfc))
								mappingServer.getListSFCTotal().remove(sfc);
						}
						
					}
					
					listSFConRpi.clear();
					pi.reset(); //reset RPI
					doneMap = true;
					remapLoopCount ++; // count number of remapping times in MAP_LOOP
				}
				
				int offDecode = 0;
				int offDensity = 0;
				int numChainRequest = listChainRequest.size(); // reallocate as listChainSize may had been changed
				
				OFFDECODE_LOOP:
				for (offDecode = 0; offDecode <= numChainRequest; offDecode++) {
					for(offDensity = offDecode; offDensity >= offDecode && offDensity <= numChainRequest; offDensity++) {
						int numOffDecode = 0;
						int numOffDensity = 0;
						LinkedList<SFC> listSFCTemp = new LinkedList<>();
						
						for(int numSFC = 0; numSFC < numChainRequest; numSFC++) { // initialise SFC list
							double endTime = listChainRequest.get(numSFC); // error after remapping numChain exceeds listChainSize
//							String sfcID = String.valueOf(SFCIndexIncrease);
							SFC sfc = new SFC(SFCIndexIncrease, pi.getId(), endTime); 
							sfc.setServicePosition(capture.getServiceType(), true); // always at Rpi
							sfc.setServicePosition(receive.getServiceType(), false);// always at server
							if(numOffDecode < offDecode) {
								sfc.setServicePosition(decode.getServiceType(), false);
							}
							else sfc.setServicePosition(decode.getServiceType(), true);
							if(numOffDensity < offDensity) {
								sfc.setServicePosition(density.getServiceType(), false);
							}
							else sfc.setServicePosition(density.getServiceType(), true);
							
							listSFCTemp.add(sfc);
							SFCIndexIncrease++;
							numOffDecode++;
							numOffDensity++;
						}
						
						double bandwidthPiUsed = (offDecode)*capture.getBandwidth() + (offDensity - offDecode)*decode.getBandwidth() + (numChainRequest - offDensity)*density.getBandwidth();
						double cpuPiUsed = numChainRequest*capture.getCpu_pi() + (numChainRequest - offDecode)*decode.getCpu_pi() + (numChainRequest - offDensity)*density.getCpu_pi();
						
						
						//<-------- check Pi resource pool 
						if(bandwidthPiUsed > pi.getRemainBandwidth()) {
							if (remapPi == true) {
								listChainRequest.removeLast(); // remove last object
								System.out.println("Request remove 1 SFC due to low BW.");
								if (listChainRequest.size() <= numChainSuccessCur) { // prevent system continue loop even final result has been selected
									break MAP_LOOP;
								} else {
									doneMap = false;
									break OFFDECODE_LOOP; // try to map with lower num of chain
								}
							} else if (numChainSuccessCur == 0) {
								remapPi = true;
								doneMap = false;
								break OFFDECODE_LOOP; // try to remap
							} else {
								break MAP_LOOP; 
							}

						} else if(cpuPiUsed > pi.getRemainCPU()){
							if (offDecode == numChainRequest && remapPi == false) {
								remapPi = true; //turn on remapping Pi
								break OFFDECODE_LOOP;
							} else
								continue; // try to offload service  
						} else {
							double powerPiTemp = numChainRequest*capture.getPower() +(numChainRequest - offDecode)*decode.getPower() + (numChainRequest - offDensity)*density.getPower();
							double powerServerTemp = calculatePseudoPowerServer(numChainRequest*receive.getCpu_server() + offDecode*decode.getCpu_server() + offDensity*density.getCpu_server());
							double totalPowerTemp = powerPiTemp + powerServerTemp;
							
							if (numChainRequest >= numChainSuccessCur && totalPowerTemp <= minPower) { //QoS and acceptance rate priority
								numChainSuccessCur = numChainRequest;
								minPower = totalPowerTemp;
								doneFlag = true; // used to break MAP_LOOP


//								}
								//<------calculate number of VNF migration
//								if(remapPi == true) {
//									for(int i = 0; i < numChainBefore; i++) {
//										numVNFinServerAfter += listSFCTemp.get(i).numServiceInServer();
//									}
//									numVNFMigration += Math.abs(numVNFinServerAfter - numVNFinServerBefore);
//								}
								//<------add to listSFC output
								listSFCFinalPi.clear();
								for(int index = 0; index < listSFCTemp.size(); index++) {
									listSFCFinalPi.add(listSFCTemp.get(index));
								}
			
							} else {
								System.out.println("Power is larger than stored value. Relooping... \n");
								if (offDecode == numChainRequest) { // last loop
									break MAP_LOOP;
								}
								continue; //<----allowing looping, this makes code runs for longer time
							} // MIN_POWER CONDITION
						} // BW CPU CONDITION
					} // OFF_DENSITY LOOP
				} // OFF_DECODE LOOP
				
			} 
	}///end mapping function
	
	public static double calculatePseudoPowerServer(double cpuServer) {
		double numServer = Math.floor(cpuServer/100);
		double cpuFragment = cpuServer - 100*numServer;
		 return numServer*powerServer(100) + powerServer(cpuFragment);
	}
	
	public static double powerServer(double cpu) {
		return (95*(cpu/100) + 221);
	}
	
	public static void write_integer (String filename, LinkedList<Integer> x) throws IOException{ //write result to file
 		 BufferedWriter outputWriter = null;
 		 outputWriter = new BufferedWriter(new FileWriter(filename));
  		for (int i = 0; i < x.size(); i++) {
			outputWriter.write(Integer.toString(x.get(i)));
			outputWriter.newLine();
  		}
		outputWriter.flush();  
		outputWriter.close();  
	}
	
	
	public static void write_integer (String filename, int [] x) throws IOException{ //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
		for (int i = 0; i < x.length; i++) {
			// Maybe:
			//outputWriter.write(x.get(i));
			// Or:
			outputWriter.write(Integer.toString(x[i]));
			outputWriter.newLine();
		}
		outputWriter.flush();  
		outputWriter.close();  
	}

	public static void write_double (String filename, LinkedList<Double> x) throws IOException { //write result to file
 		 BufferedWriter outputWriter = null;
 		 outputWriter = new BufferedWriter(new FileWriter(filename));
  		for (int i = 0; i < x.size(); i++) {
			// Maybe:
//			outputWriter.write(x[i]);
			// Or:
			outputWriter.write(Double.toString(x.get(i)));
			outputWriter.newLine();
  		}
		outputWriter.flush(); 
		outputWriter.close();  
	}
	
	public static void write_double (String filename, double [] x) throws IOException { //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
		for (int i = 0; i < x.length; i++) {
			// Maybe:
//			outputWriter.write(x[i]);
			// Or:
			outputWriter.write(Double.toString(x[i]));
			outputWriter.newLine();
		}
		outputWriter.flush(); 
		outputWriter.close();  
	}

	public void run(Map<Integer,HashMap<Integer,LinkedList<Double>>> listRequest, LinkedList<Integer> timeWindow){

		LinkedList<Double> totalPowerSystemConsolidation = new LinkedList<Double>();
		LinkedList<Double> totalPowerSystem = new LinkedList<Double>();
		LinkedList<Double> totalPowerPerSFC = new LinkedList<Double>();
		LinkedList<Double> serverUtilization = new LinkedList<Double>();
		LinkedList<Double> totalChainAcceptance = new LinkedList<Double>();
		LinkedList<Double> totalPiAcceptance = new LinkedList<Double>();
		LinkedList<Integer> listServerUsed = new LinkedList<Integer>();
		LinkedList<Integer> totalChainSystem = new LinkedList<Integer>();
		LinkedList<Integer> totalChainActive = new LinkedList<Integer>();
		LinkedList<Integer> listDecInCloud = new LinkedList<Integer>();
		LinkedList<Integer> listDenInCloud = new LinkedList<Integer>();
		LinkedList<Integer> listReceiveInCloud = new LinkedList<Integer>();
		LinkedList<Integer> totalChainReject = new LinkedList<Integer>();
		LinkedList<Double> totalLoadEdge = new LinkedList<Double>();
		LinkedList<Double> totalBwEdge = new LinkedList<Double>();
		LinkedList<Integer> totalChainLeave = new LinkedList<Integer>();
		LinkedList<Integer> totalChainRequest = new LinkedList<>();
		LinkedList<Integer> listVNFmigration = new LinkedList<>();
		LinkedList<Double> listLinkUsage = new LinkedList<Double>();
		LinkedList<Double> cpuEdgeUsagePerSFC = new LinkedList<Double>();
		LinkedList<Double> cpuServerUsagePerSFC = new LinkedList<Double>();
		LinkedList<Double> averageBWUsage = new LinkedList<Double>();
		LinkedList<Double> capacity = new LinkedList<Double>();
		LinkedList<Double> capacityEdge = new LinkedList<Double>();
		LinkedList<Double> capacityCloud = new LinkedList<Double>();
		
		
		Map<Integer, LinkedList<Integer>> listLeaveTotal = new HashMap<>();
		Map<Integer, LinkedList<Integer>> listOffForEachPi_temp = new HashMap<>();
		Map<Integer, LinkedList<Integer>> listOffForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Double>> listCPUForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Double>> listBWForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Double>> listCpuEdgePerSFC = new HashMap<>();
		Map<Integer, LinkedList<Double>> listBWPerSFC = new HashMap<>();
		
		for(int i = 0; i < NUM_PI; i++) {
			
			listLeaveTotal.put(i,new LinkedList<Integer>());
			listOffForEachPi_temp.put(i, new LinkedList<Integer>());
			listOffForEachPi.put(i, new LinkedList<Integer>());
			listCPUForEachPi.put(i, new LinkedList<Double>());
			listBWForEachPi.put(i, new LinkedList<Double>());
			listCpuEdgePerSFC.put(i, new LinkedList<Double>());
			listBWPerSFC.put(i, new LinkedList<Double>());
		}
	
		double acceptance = 0;
		double acceptancePi = 0;
		int requestIndex = 0; // number of request
	
		
		LinkedList<Integer> requestRandomReceive = new LinkedList<>();

		//<------REQUEST_LOOP
		while (requestIndex < timeWindow.size()) { //////////////////////////////////////////////////////////////////////////////////////////
			
			HashMap<Integer,LinkedList<Double>> listRequestPi = listRequest.get(requestIndex);
			int numSFCLevThisTW = 0;
			int numDenLevThisTW = 0;
			int numSerJoinCloud = 0;
			int numPiReceive = 0; // number of Pi receives request > 0
			int piAccept = 0;
			int numSFCReqThisTW = 0;
			IntHolder numMapReqThisTW = new IntHolder(0);
			
			LinkedList<Double> loadEdgeNumPi = new LinkedList<>();
			LinkedList<Double> bwEdgeNumPi = new LinkedList<>();			
		
			for (Entry<Integer, LinkedList<Double>> entry : listRequestPi.entrySet()) { //change i < 1 to i < num_pi for mapping every pi/////////////////////////////////
				Rpi pi = listRpi.get(entry.getKey());
				int numSFCReqThisPi = entry.getValue().size();
				numSFCReqThisTW += numSFCReqThisPi;

				if (numSFCReqThisPi != 0)
					numPiReceive ++;
			
				LinkedList<SFC> listSFCFinalPi = new LinkedList<SFC>();
				LinkedList<SFC> listCurSFCOnPi = listSFConRpi.get(pi);
				LinkedList<SFC> listSFCLeave = new LinkedList<>();
				
				System.out.println("Request number " + requestIndex);
				System.out.println("Pi number " + (pi.getId()+1)+ " with " +entry.getValue().size()+ " chains need to be mapped");
				System.out.println("Pi has mapped "+ listCurSFCOnPi.size());
				
				//<--------END--OF--SERVICE--PROCESS
				System.out.println("Start leaving process ....");
				
				if(listCurSFCOnPi.size() != 0 && listCurSFCOnPi != null) {
					boolean flagLeave = false;
					for(SFC sfc : listCurSFCOnPi) {
						if(sfc.getEndTime() <= requestIndex) {
							flagLeave = true;
							listSFCLeave.add(sfc);
							numSFCLevThisTW ++;
						}
					}
					
					if(flagLeave == true) {
						
						for(SFC sfc : listCurSFCOnPi) { // remove all sfc belongs to pi in listSFCTotal 
							if(mappingServer.getListSFCTotal().contains(sfc))
								mappingServer.getListSFCTotal().remove(sfc);
						}
						
						mappingServer.getServiceMapping().resetRpiSFC(listCurSFCOnPi, topo); // reset at server
						coreNetwork.reset(pi); // reset network
						pi.reset(); // reset rpi
						
						//<-----------remap leftover SFC
						for(SFC sfc : listSFCLeave) {
							for(Service ser : sfc.getListService()) {
								if(!ser.isBelongToEdge() && ser.getServiceType() == "density")
									numDenLevThisTW ++;
							}
							if(listCurSFCOnPi.contains(sfc))
								listCurSFCOnPi.remove(sfc);
						}
						LinkedList<Double> listSFCRemapLeave = new LinkedList<>();
						for (SFC sfc : listCurSFCOnPi) {
							double endTime = sfc.getEndTime();
							listSFCRemapLeave.add(endTime);
						}
						listCurSFCOnPi.clear();
						if(listSFCRemapLeave.size() != 0){
							VNFMapping(topo, mappingServer, coreNetwork, pi, listSFCRemapLeave, listCurSFCOnPi, listSFCFinalPi, numMapReqThisTW);
							coreNetwork.run(listSFCFinalPi, pi);
							mappingServer.runMapping(listSFCFinalPi, topo);
							//===finalize after remapping ====//
							listCurSFCOnPi.addAll(mappingServer.getListSFC());
							
							Double cpuEdgeUsage = 0.0;
							Double bwEdgeUsage = 0.0;
							for(SFC sfc : mappingServer.getListSFC()) {
								cpuEdgeUsage += sfc.cpuEdgeUsage();
								bwEdgeUsage += sfc.bandwidthUsageOutDC();
							}
							pi.setUsedCPU(cpuEdgeUsage); // change CPU pi-server
							pi.setUsedBandwidth(bwEdgeUsage); //change Bandwidth used by Pi
							listSFCFinalPi.clear();
						}
					}
				}
				else ;
//				listLeaveForEachPi.get(pi.getId()).add(numSFCLevThisPi);
				//<------------JOIN --PROCESS
				System.out.println("Start joining process ....");
				LinkedList<Double> listEndTime = new LinkedList<>();
				for(int index = 0; index < entry.getValue().size(); index++) {
					double endTime = entry.getValue().get(index) + (double) requestIndex;
					listEndTime.add(endTime);
				}
				
				if(pi.isOverload() == true) {
					System.out.println("This pi is already overloaded");
				}
				else if(listEndTime.size() == 0) {
					System.out.println("This pi receive zero request");

				}
				else {
					VNFMapping(topo, mappingServer, coreNetwork, pi, listEndTime, listCurSFCOnPi, listSFCFinalPi, numMapReqThisTW);
				}
				
				//<-----Only successful mapping can jump into the below condition
				if(listSFCFinalPi.size() != 0 && pi.isOverload() == false && listEndTime.size()!= 0) { // case Pi mapping is success
					
					coreNetwork.run(listSFCFinalPi, pi);
					mappingServer.runMapping(listSFCFinalPi, topo);
					//<------finalize after remapping 
					listCurSFCOnPi.addAll(mappingServer.getListSFC());
					//<------set value for Rpi after mapping successfully
					Double cpuEdgeUsage = 0.0;
					Double bwEdgeUsage = 0.0;
					for(SFC sfc : mappingServer.getListSFC()) {
						cpuEdgeUsage += sfc.cpuEdgeUsage();
						bwEdgeUsage += sfc.bandwidthUsageOutDC();
					}
					pi.setUsedCPU(cpuEdgeUsage); // change CPU pi-server
					pi.setUsedBandwidth(bwEdgeUsage); //change Bandwidth used by Pi
	
					numMapReqThisTW.value += mappingServer.getListSFC().size();
					
					// store number of VNF joins to the Cloud
					for(SFC sfc : mappingServer.getListSFC()) {
						for(Service ser : sfc.getListService()) {
							if(ser.getServiceType() == "density" && !ser.isBelongToEdge())
								numSerJoinCloud ++;
						}
					}
					piAccept ++; //num of accepted Pi (accept if at least 1 SFC has been mapped
				}
				
				//<----Every mapping step has to do this
				loadEdgeNumPi.add(pi.getUsedCPU());
				bwEdgeNumPi.add(pi.getUsedBandwidth());		
				int offServiceCur = 0;

				for(SFC sfc : listCurSFCOnPi) {
					if(sfc.getService(2).getBelongToEdge() == false) {
						offServiceCur ++;
					}
					if(sfc.getService(3).getBelongToEdge() == false) {
						offServiceCur ++;
					}
				}

				listOffForEachPi_temp.get(pi.getId()).add(offServiceCur);
				if(requestIndex == 0) {
					listOffForEachPi.get(pi.getId()).add(offServiceCur);
				}else {
					int offServicePre = listOffForEachPi_temp.get(pi.getId()).get(requestIndex - 1);
					int offService = offServiceCur - offServicePre;
					listOffForEachPi.get(pi.getId()).add(offService);
				}
				double cpuPiCur = pi.getUsedCPU();
				double bwPiCur = pi.getUsedBandwidth();
				listCPUForEachPi.get(pi.getId()).add(cpuPiCur);
				listBWForEachPi.get(pi.getId()).add(bwPiCur);
				
			} //end Rpi for loop
			
			int numDenInCloud = 0;
			int numDecInCloud = 0;
			int numReceiveInCloud = 0;
			
			for(SFC sfc : mappingServer.getListSFCTotal()) {
				if(sfc.getService(2).getBelongToEdge() == false) {
					numDecInCloud ++;
				}
				if(sfc.getService(3).getBelongToEdge() == false) {
					numDenInCloud ++;
				}
				if(sfc.getService(4).getBelongToEdge() == false) {
					numReceiveInCloud ++;
				}
			}
			
			listDecInCloud.add(numDecInCloud);
			listDenInCloud.add(numDenInCloud);
			listReceiveInCloud.add(numReceiveInCloud);
			
			double sumCPUPi = 0.0;
			double sumBwPi = 0.0;
			
			for (int index = 0; index < NUM_PI; index++) {
				sumCPUPi += listRpi.get(index).getUsedCPU();
				sumBwPi += listRpi.get(index).getUsedBandwidth();
			}
			
			totalLoadEdge.add(requestIndex,(sumCPUPi/(NUM_PI)));
			totalBwEdge.add(requestIndex,(sumBwPi/NUM_PI));
			
			acceptance = (numMapReqThisTW.value*1.0)/numSFCReqThisTW; //after a request
			acceptancePi = (piAccept*1.0)/numPiReceive;
			
			totalChainAcceptance.add(requestIndex, acceptance);
			totalPiAcceptance.add(requestIndex, acceptancePi);
			totalChainRequest.add(numMapReqThisTW.value);
			//<------calculate average bandwidth usage
			double totalBandwidthSFC = 0;
			double totalSFCSize = 0;
			for(Entry<Rpi, LinkedList<SFC>>  entry : listSFConRpi.entrySet()) {
				LinkedList<SFC> listSFCRpi = entry.getValue();
				totalSFCSize += listSFCRpi.size();
				for(SFC sfc : listSFCRpi) {
					totalBandwidthSFC += sfc.getBandwidthSFC();
				}
			}
			averageBWUsage.add((totalBandwidthSFC*1.0)/totalSFCSize);
			serverUtilization.add(topo.getCPUServerUtilization());
			listServerUsed.add(topo.getServerUsed());
			totalChainActive.add(mappingServer.getListSFCTotal().size());
			totalPowerSystem.add( mappingServer.getPower() + mappingServer.PowerEdgeUsage());
			
			//<-------calculate system capacity block
			double cpuEdge = 0;
			double cpuCloud = 0;
			double usedCapacity = 0;
			double usedCapacityEdge = 0;
			double usedCapacityCloud = 0;

			cpuEdge = mappingServer.cpuEdgeAllSFC();
			cpuCloud = mappingServer.cpuServerAllSFC();
			usedCapacity = (cpuEdge/2 + cpuCloud)*1.0/TOTAL_CAPACITY;
			usedCapacityEdge = cpuEdge/EDGE_CAPACITY;
			usedCapacityCloud = cpuCloud/CLOUD_CAPACITY;
			capacityEdge.add(usedCapacityEdge);
			capacityCloud.add(usedCapacityCloud);
			capacity.add(usedCapacity);
//			linkUsagePerSFC.add(mappingServer.linkUsagePerSFC());
			cpuEdgeUsagePerSFC.add(mappingServer.cpuEdgePerSFC());
			cpuServerUsagePerSFC.add(mappingServer.cpuServerPerSFC());

//			//===add more VNF migration after consolidate===//
//			numVNFMigration += (mappingServer.getListSFC().size()*4); // 4 for 4 VNF in total
//			numVNFMigration += mappingServer.getNumVNFMigration();
//			//===store number of VNF migration ===========//
//			listVNFmigration.add(numVNFMigration);
			requestIndex++;
		} // end while loop (request)
		//<-------calculate link utilization
//		LinkedList<Double> linkBandwidth = new LinkedList<>();
//		for(int index = 0; index < topo.getLinkBandwidth().size(); index++) {
//			if(topo.getLinkBandwidth().get(index).getBandwidth() < 1000)
//			linkBandwidth.add(topo.getLinkBandwidth().get(index).getBandwidth());
//		}
//		
//		LinkedList<Double> cpuServerUsed = new LinkedList<>();
//		for(PhysicalServer phy : topo.getListPhyServers().values()) {
//			cpuServerUsed.add(phy.getUsedCPUServer());
//		}
		
		try {
			write_double("./PlotRESCE-LL/totalPiAcceptanceRESCE-LL.txt",totalPiAcceptance);
			write_double("./PlotRESCE-LL/capacityRESCE-LL.txt",capacity);
			write_double("./PlotRESCE-LL/capacityEdgeRESCE-LL.txt",capacityEdge);
			write_double("./PlotRESCE-LL/capacityCloudRESCE-LL.txt",capacityCloud);
			write_double("./PlotRESCE-LL/averageBWUsageRESCE-LL.txt",averageBWUsage);
//			write_double("./PlotRESCE-LL/cpuServerUsedRESCE-LL.txt",cpuServerUsed);
			write_double("./PlotRESCE-LL/totalPowerSystemConsolidationRESCE-LL.txt",totalPowerSystemConsolidation);
			write_double("./PlotRESCE-LL/listLinkUsageRESCE-LL.txt",listLinkUsage);
			write_double("./PlotRESCE-LL/cpuEdgeUsagePerSFCRESCE-LL.txt",cpuEdgeUsagePerSFC);
			write_double("./PlotRESCE-LL/cpuServerUsagePerSFCRESCE-LL.txt",cpuServerUsagePerSFC);
//			write_double("./PlotRESCE-LL/linkBandwidthRESCE-LL.txt",linkBandwidth);
			write_double("./PlotRESCE-LL/serverUtilizationRESCE-LL.txt",serverUtilization);
			write_integer("./PlotRESCE-LL/NumVNFMigrationRESCE-LL.txt",listVNFmigration);
//			write_integer("./PlotRESCE-LL/NumServiceDecDenRESCE-LL.txt",NumServiceDecDen);
			write_integer("./PlotRESCE-LL/totalChainLeaveRESCE-LL.txt",totalChainLeave);
			write_integer("./PlotRESCE-LL/listServerUsedRESCE-LL.txt",listServerUsed);
			write_integer("./PlotRESCE-LL/requestRandomRESCE-LL.txt",requestRandomReceive);
//			write_integer("./PlotRESCE-LL/totalDecOffloadRESCE-LL.txt",totalDecOffload);
//			write_integer("./PlotRESCE-LL/totalDenOffloadRESCE-LL.txt",totalDenOffload);
			write_double("./PlotRESCE-LL/totalPowerSystemRESCE-LL.txt",totalPowerSystem);
			write_double("./PlotRESCE-LL/totalPowerSystemPerSFCRESCE-LL.txt",totalPowerPerSFC);
//			write_double("./PlotRESCE-LL/totalEdgePowerSystemRESCE-LL.txt", totalEdgePowerSystem);
//			write_double("./PlotRESCE-LL/totalServerPowerSystemRESCE-LL.txt", totalServerPowerSystem);
			write_double("./PlotRESCE-LL/totalLoadEdgeRESCE-LL.txt",totalLoadEdge);
			write_double("./PlotRESCE-LL/totalBwEdgeRESCE-LL.txt",totalBwEdge);
			write_double("./PlotRESCE-LL/totalChainAcceptanceRESCE-LL.txt",totalChainAcceptance);
//			write_double("./PlotRESCE-LL/sumLoadNumPiRESCE-LL.txt", sumLoadNumPi);
//			write_double("./PlotRESCE-LL/sumBwNumPiRESCE-LL.txt", sumBwNumPi);
			write_integer("./PlotRESCE-LL/totalChainSystemRESCE-LL.txt",totalChainSystem);
			write_integer("./PlotRESCE-LL/totalChainActiveRESCE-LL.txt",totalChainActive);
			write_integer("./PlotRESCE-LL/totalChainRejectRESCE-LL.txt",totalChainReject);
//			write_integer("./PlotRESCE-LL/numChainRequestRESCE-LL.txt",numChainRequest);
//			write_integer("./PlotRESCE-LL/numChainAcceptRESCE-LL.txt",numChainAccept);
			System.out.println("Completed.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Map<Rpi, LinkedList<SFC>> getListSFConRpi() {
		return listSFConRpi;
	}


	public void setListSFConRpi(Map<Rpi, LinkedList<SFC>> listSFConRpi) {
		this.listSFConRpi = listSFConRpi;
	}


	public LinkedList<Rpi> getListRpi() {
		return listRpi;
	}


	public void setListRpi(LinkedList<Rpi> listRpi) {
		this.listRpi = listRpi;
	}


	public LinkedList<Integer> getEdgePosition() {
		return edgePosition;
	}


	public void setEdgePosition(LinkedList<Integer> edgePosition) {
		this.edgePosition = edgePosition;
	}

	
}