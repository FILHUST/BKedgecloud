/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.RESCEFF;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import fil.algorithm.RESCEFF.MappingServer;
import fil.algorithm.routing.NetworkRouting;
import fil.resource.substrate.Rpi;
import fil.resource.virtual.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RESCEFF  {
	
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

	public RESCEFF() { // default constructor
		
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
	

	public void VNFMapping( Topology topo, MappingServer mappingServer, NetworkRouting coreNetwork, Rpi pi, LinkedList<SFC> listChainRequest, 
			LinkedList<SFC> listSFConRpi,
			LinkedList<SFC> listSFCFinalPi, IntHolder numSFCTW, IntHolder numVNF) {
		
		boolean doneFlag = false;
		boolean remapPi = false;
		boolean doneMap = false;
		int remapLoopCount = 0;
		int numChainSuccessCur = 0;
		double minPower = Integer.MAX_VALUE;
		Capture capture = new Capture();
		Decode decode = new Decode();
		Density density = new Density();
		Receive receive = new Receive();		
		
		MAP_LOOP:
			while (doneFlag == false) {////////////////////TRIAL LOOP////////////////////////
				
				if (remapPi == true && doneMap == false){ //remapp
					System.out.println("Inside remapping edge device number " + remapLoopCount);
					if (remapLoopCount == 0) {
						numVNF.value += this.numVNFatEdge(listSFConRpi);
						numSFCTW.value -= listSFConRpi.size();
						for(SFC sfc : listSFConRpi) {
							listChainRequest.addFirst(sfc); // add first position 
						}
					}
					if(!listSFConRpi.isEmpty()) {
						coreNetwork.reset(pi); // reset network topology
						mappingServer.getServiceMapping().resetSFC(listSFConRpi, topo); // reset mapped chains pi without remapping at server
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
//							String sfcID = String.valueOf(SFCIndexIncrease);
							SFC sfc = new SFC();  // create a default SFC
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
								listSFCFinalPi.clear();
								for(int index = 0; index < listSFCTemp.size(); index++) {
									listChainRequest.get(index).copy(listSFCTemp.get(index));
									listSFCFinalPi.add(listChainRequest.get(index));
								}
								
//								break OFFDECODE_LOOP;
			
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
	
	private int numVNFatEdge(LinkedList<SFC> listSFConRpi) {
		// TODO Auto-generated method stub
		int result = 0;
		for(SFC sfc : listSFConRpi) {
			for(Service sv : sfc.getListService()) {
				if(sv.isBelongToEdge() && sv.getServiceType() != "capture")
					result ++;
			}
		}
		return result;
	}

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

	public void run(LinkedList<LinkedList<Event>> listTotalEvent){

		LinkedList<Double> totalPowerSystem = new LinkedList<Double>();
		LinkedList<Double> totalEnergySystem = new LinkedList<Double>();
		LinkedList<Double> totalPowerPerSFC = new LinkedList<Double>();
		LinkedList<Double> serverUtilization = new LinkedList<Double>();
		LinkedList<Double> totalChainAcceptance = new LinkedList<Double>();
		LinkedList<Integer> listServerUsed = new LinkedList<Integer>();
//		LinkedList<Integer> totalChainSystem = new LinkedList<Integer>();
		LinkedList<Integer> totalChainActive = new LinkedList<Integer>();
		LinkedList<Integer> totalChainReject = new LinkedList<Integer>();
		LinkedList<Double> totalLoadEdge = new LinkedList<Double>();
		LinkedList<Double> totalBwEdge = new LinkedList<Double>();
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
		double time = 0.0;
	
		//<------REQUEST_LOOP
		for(int eventInTW = 0; eventInTW < listTotalEvent.size(); eventInTW ++) { // window of 1 hour
			
			double energyTW = 0;
			
			LinkedList<Event> listEvent = listTotalEvent.get(eventInTW);
			
			int numSFCActive = 0;
			int numSFCReqThisTW = 0;
			IntHolder numMapReqThisTW = new IntHolder(0);			
		
			for(int eventIn = 0; eventIn < listEvent.size(); eventIn ++) {
				
				Event event = listEvent.get(eventIn);
				int piID = event.getPiID();
				Rpi pi = listRpi.get(piID);
				LinkedList<SFC> listCurSFCOnPi = listSFConRpi.get(pi);
				LinkedList<SFC> listSFCFinalPi = new LinkedList<SFC>();
				IntHolder numVNFMig = new IntHolder(0);
				
				if(event.getType() == "leave") {
					if(event.getListSfc().size() > 1)
						throw new java.lang.Error();
					
					SFC sfc = event.getListSfc().getFirst();
					
					if(listCurSFCOnPi.contains(sfc)) {
						mappingServer.getServiceMapping().resetSFC(event.getListSfc(), topo); // reset at server
						coreNetwork.resetSFC(pi, event.getListSfc()); // reset network
						pi.resetSFC(event.getListSfc()); // reset listSFC on Rpi
						
						if(mappingServer.getListSFCTotal().contains(sfc))
							mappingServer.getListSFCTotal().remove(sfc);
						
						listCurSFCOnPi.remove(sfc);
					}else {
						continue;
					}
 
				}else {
					System.out.println("Start joining process ....");
					
					numSFCReqThisTW += event.getListSfc().size();
					
					if(listCurSFCOnPi.size() == 7) {
						System.out.println("This pi is already overloaded");
						continue;
					}
					else {
						VNFMapping(topo, mappingServer, coreNetwork, pi, event.getListSfc(), listCurSFCOnPi, listSFCFinalPi, numMapReqThisTW, numVNFMig);
					}
					if(listSFCFinalPi.size() != 0) { // case Pi mapping is success
						
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
						
					}
					
				} // join request
				
				if(numSFCActive < mappingServer.getListSFCTotal().size())
					numSFCActive = mappingServer.getListSFCTotal().size();
				double power = mappingServer.getPower() + mappingServer.PowerEdgeUsage();
				energyTW += power*(event.getTime() - time)/1000; //kWh
				time = event.getTime();
									
			} // event loop
			
			acceptance = (numMapReqThisTW.value*1.0)/numSFCReqThisTW; //after a request
			totalChainAcceptance.add(acceptance);
			totalChainRequest.add(numSFCReqThisTW);
			totalEnergySystem.add(energyTW);
			totalChainActive.add(numSFCActive);
			totalPowerSystem.add(mappingServer.getPower() + mappingServer.PowerEdgeUsage());
			serverUtilization.add(topo.getCPUServerUtilization());
			listServerUsed.add(topo.getServerUsed());

		} // end while loop (request)
		
		
		try {
			Path path = Paths.get("./PlotRESCEFF");
		    //java.nio.file.Files;
		    Files.createDirectories(path);

		    System.out.println("Directory is created!");
		    
			write_double("./PlotRESCEFF/capacityRESCEFF.txt",capacity);
			write_double("./PlotRESCEFF/capacityEdgeRESCEFF.txt",capacityEdge);
			write_double("./PlotRESCEFF/capacityCloudRESCEFF.txt",capacityCloud);
			write_double("./PlotRESCEFF/averageBWUsageRESCEFF.txt",averageBWUsage);
//			write_double("./PlotRESCEFF/totalPowerSystemConsolidationRESCEFF.txt",totalPowerSystemConsolidation);
			write_double("./PlotRESCEFF/listLinkUsageRESCEFF.txt",listLinkUsage);
			write_double("./PlotRESCEFF/cpuEdgeUsagePerSFCRESCEFF.txt",cpuEdgeUsagePerSFC);
			write_double("./PlotRESCEFF/cpuServerUsagePerSFCRESCEFF.txt",cpuServerUsagePerSFC);
//			write_double("./PlotRESCEFF/linkBandwidthRESCEFF.txt",linkBandwidth);
			write_double("./PlotRESCEFF/serverUtilizationRESCEFF.txt",serverUtilization);
			write_integer("./PlotRESCEFF/NumVNFMigrationRESCEFF.txt",listVNFmigration);
//			write_integer("./PlotRESCEFF/NumServiceDecDenRESCEFF.txt",NumServiceDecDen);
//			write_integer("./PlotRESCEFF/totalChainLeaveRESCEFF.txt",totalChainLeave);
			write_integer("./PlotRESCEFF/listServerUsedRESCEFF.txt",listServerUsed);
//			write_integer("./PlotRESCEFF/requestRandomRESCEFF.txt",requestRandomReceive);
//			write_integer("./PlotRESCEFF/totalDecOffloadRESCEFF.txt",totalDecOffload);
//			write_integer("./PlotRESCEFF/totalDenOffloadRESCEFF.txt",totalDenOffload);
//			write_double("./PlotRESCEFF/totalPowerSystemRESCEFF.txt",totalPowerSystem);
			write_double("./PlotRESCEFF/totalEnergySystemRESCEFF.txt",totalEnergySystem);
			write_double("./PlotRESCEFF/totalPowerSystemPerSFCRESCEFF.txt",totalPowerPerSFC);
//			write_double("./PlotRESCEFF/totalEdgePowerSystemRESCEFF.txt", totalEdgePowerSystem);
//			write_double("./PlotRESCEFF/totalServerPowerSystemRESCEFF.txt", totalServerPowerSystem);
			write_double("./PlotRESCEFF/loadEdgeRESCEFF.txt",totalLoadEdge);
			write_double("./PlotRESCEFF/bwEdgeRESCEFF.txt",totalBwEdge);
			write_double("./PlotRESCEFF/acceptanceRatioRESCEFF.txt",totalChainAcceptance);
//			write_double("./PlotRESCEFF/sumLoadNumPiRESCEFF.txt", sumLoadNumPi);
//			write_double("./PlotRESCEFF/sumBwNumPiRESCEFF.txt", sumBwNumPi);
			write_integer("./PlotRESCEFF/chainRequestRESCEFF.txt",totalChainRequest);
			write_integer("./PlotRESCEFF/chainActiveRESCEFF.txt",totalChainActive);
			write_integer("./PlotRESCEFF/totalChainRejectRESCEFF.txt",totalChainReject);
//			write_integer("./PlotRESCEFF/numChainRequestRESCEFF.txt",numChainRequest);
//			write_integer("./PlotRESCEFF/numChainAcceptRESCEFF.txt",numChainAccept);
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