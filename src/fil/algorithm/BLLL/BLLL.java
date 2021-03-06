/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.BLLL;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import fil.algorithm.routing.NetworkRouting;
import fil.resource.substrate.Rpi;
import fil.resource.virtual.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BLLL  {
	
	final static int NUM_PI = 300;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int EDGE_CAPACITY = 30000;
	final static int CLOUD_CAPACITY = 25000;
	final static int SYS_CAPACITY = 2100; // maximum SFC can be mapped ideally

	private Topology topo;
	private FatTree fatTree;
	private MappingServer mappingServer;
	private NetworkRouting coreNetwork;

	private LinkedList<Rpi> listRpi;
	private LinkedList<Integer> edgePosition;
	private Map<Rpi, LinkedList<SFC>> listSFConRpi;

	public BLLL() { // default constructor
		
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
	

	public static void BLMapping(Topology topo, MappingServer mappingServer, Rpi pi, LinkedList<SFC> listChainRequest, 
			LinkedList<SFC> listSFConRpi, LinkedList<SFC> listSFCFinalPi) {

		boolean doneFlag = false;
		int numChainSuccessCur = 0;
		double minPower = Integer.MAX_VALUE;
		
		Capture capture = new Capture();
		Decode decode = new Decode();
		Density density = new Density();
		Receive receive = new Receive();
		
		MAP_LOOP:
		while (doneFlag == false) {

			if (listSFConRpi.size() == 7) { 
				pi.setOverload(true); 
				break;
			}
			
			if(listChainRequest.size() == 0) {
				break; 
			}
					
			int offDecode = 0;
			int offDensity = 0;
			int numChainRequest = listChainRequest.size(); // reallocate as listChainSize may had been changed

			for (offDecode = 0; offDecode <= numChainRequest; offDecode++) {
				for(offDensity = offDecode; offDensity >= offDecode && offDensity <= numChainRequest; offDensity++) {
					
					int numOffDecode = 0;
					int numOffDensity = 0;
					LinkedList<SFC> listSFCTemp = new LinkedList<>();
					
					for(int numSFC = 0; numSFC < numChainRequest; numSFC++ ) { // initialize SFC list
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
					
					
					/* check Pi resource pool ************************************************/
					if(bandwidthPiUsed > pi.getRemainBandwidth()) {
						if (listChainRequest.size() <= numChainSuccessCur || numChainSuccessCur > 0) { // prevent system continue loop even final result has been selected
							break MAP_LOOP;
						} else {
							//numChain --;
							doneFlag = false;
						}
					} else if (cpuPiUsed > pi.getRemainCPU())
						continue;
					else {

						double powerPiTemp = numChainRequest*capture.getPower() +(numChainRequest - offDecode)*decode.getPower() + (numChainRequest - offDensity)*density.getPower();
						double powerServerTemp = calculatePseudoPowerServer(numChainRequest*receive.getCpu_server() + offDecode*decode.getCpu_server() + offDensity*density.getCpu_server());
						double totalPowerTemp = powerPiTemp + powerServerTemp;
						
						if (numChainRequest >= numChainSuccessCur && totalPowerTemp <= minPower) { //QoS and acceptance rate priority
							numChainSuccessCur = numChainRequest;
							minPower = totalPowerTemp;
							doneFlag = true; // used to break MAP_LOOP


//							}
							//<------calculate number of VNF migration
//							if(remapPi == true) {
//								for(int i = 0; i < numChainBefore; i++) {
//									numVNFinServerAfter += listSFCTemp.get(i).numServiceInServer();
//								}
//								numVNFMigration += Math.abs(numVNFinServerAfter - numVNFinServerBefore);
//							}
							//<------add to listSFC output
							listSFCFinalPi.clear();
							for(int index = 0; index < listSFCTemp.size(); index++) {
								listChainRequest.get(index).copy(listSFCTemp.get(index));
								listSFCFinalPi.add(listChainRequest.get(index));
							}
		
						} else {
							System.out.println("Maploop has gone so wrong, stop! \n");
							if (offDecode == numChainRequest) { // last loop
								break MAP_LOOP;
							}
							continue;
						}
					}
				}	 // OFF_DENSITY LOOP
			} // OFF_DECODE LOOP
			
			if(doneFlag == false) {
				listChainRequest.removeLast(); // remove last object
				if(listChainRequest.size() <= 0) {
					break MAP_LOOP;
				} else continue;
			}
		}
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

		LinkedList<Double> totalPowerSystemConsolidation = new LinkedList<Double>();
		LinkedList<Double> totalPowerSystem = new LinkedList<Double>();
//		LinkedList<Double> totalEnergySystem = new LinkedList<Double>();
		LinkedList<Double> serverUtilization = new LinkedList<Double>();
		LinkedList<Double> systemUtilization = new LinkedList<Double>();
		LinkedList<Double> totalChainAcceptance = new LinkedList<Double>();
		LinkedList<Integer> listServerUsed = new LinkedList<Integer>();
		LinkedList<Integer> totalChainActive = new LinkedList<Integer>();
		LinkedList<Integer> totalChainLeave = new LinkedList<Integer>();
		LinkedList<Integer> totalChainRequest = new LinkedList<>();
		LinkedList<Double> listLinkUsage = new LinkedList<Double>();	
		
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
			
			double power1h = 0;

			LinkedList<Event> listEvent = listTotalEvent.get(eventInTW);

			int numSFCActive = 0;
			int numSFCReqThisTW = 0;
			int numMapReqThisTW = 0;	
		
			for(int eventIn = 0; eventIn < listEvent.size(); eventIn ++) {
				
				Event event = listEvent.get(eventIn);
				int piID = event.getPiID();
				Rpi pi = listRpi.get(piID);
				LinkedList<SFC> listCurSFCOnPi = listSFConRpi.get(pi);
				LinkedList<SFC> listSFCFinalPi = new LinkedList<SFC>();

				
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
						BLMapping(topo, mappingServer, pi, event.getListSfc(), listCurSFCOnPi, listSFCFinalPi);
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
		
						numMapReqThisTW += mappingServer.getListSFC().size();
						
					}
					
				} // join request
				if(numSFCActive < mappingServer.getListSFCTotal().size())
					numSFCActive = mappingServer.getListSFCTotal().size();	
				double power = mappingServer.getPower() + mappingServer.PowerEdgeUsage();
				power1h += power*(event.getTime() - time)/1000; //kW
				time = event.getTime();
				
			} // event loop
				
			acceptance = (numMapReqThisTW*1.0)/numSFCReqThisTW; //after a request
			totalChainAcceptance.add(acceptance);
			totalPowerSystem.add(power1h);
			totalChainRequest.add(numSFCReqThisTW);
			totalChainActive.add(numSFCActive);
			systemUtilization.add(numSFCActive*1.0/SYS_CAPACITY);
			serverUtilization.add(topo.getCPUServerUtilization());
			listServerUsed.add(topo.getServerUsed());
				
				
				

		} // end while loop (request)
		
		
		try {
			Path path = Paths.get("./PlotBLLL");
		    //java.nio.file.Files;
		    Files.createDirectories(path);

		    System.out.println("Directory is created!");
		    
//			write_double("./PlotBLLL/totalPiAcceptanceBLL.txt",totalPiAcceptance);
//			write_double("./PlotBLLL/capacityBLL.txt",capacity);
//			write_double("./PlotBLLL/capacityEdgeBLL.txt",capacityEdge);
//			write_double("./PlotBLLL/capacityCloudBLL.txt",capacityCloud);
//			write_double("./PlotBLLL/averageBWUsageBLL.txt",averageBWUsage);
//			write_double("./PlotBLLL/cpuServerUsedBLL.txt",cpuServerUsed);
			write_double("./PlotBLLL/totalPowerSystemConsolidationBLLL.txt",totalPowerSystemConsolidation);
			write_double("./PlotBLLL/listLinkUsageBLLL.txt",listLinkUsage);
//			write_double("./PlotBLLL/cpuEdgeUsagePerSFCBLL.txt",cpuEdgeUsagePerSFC);
//			write_double("./PlotBLLL/cpuServerUsagePerSFCBLL.txt",cpuServerUsagePerSFC);
//			write_double("./PlotBLLL/linkBandwidthBLL.txt",linkBandwidth);
			write_double("./PlotBLLL/serverUtilizationBLLL.txt",serverUtilization);
			write_double("./PlotBLLL/systemUtilizationBLLL.txt",systemUtilization);

//			write_integer("./PlotBLLL/NumVNFMigrationBLL.txt",listVNFmigration);
//			write_integer("./PlotBLLL/NumServiceDecDenBLL.txt",NumServiceDecDen);
			write_integer("./PlotBLLL/totalChainLeaveBLLL.txt",totalChainLeave);
			write_integer("./PlotBLLL/listServerUsedBLLL.txt",listServerUsed);
//			write_integer("./PlotBLLL/requestRandomBLL.txt",requestRandomReceive);
//			write_integer("./PlotBLLL/totalDecOffloadBLL.txt",totalDecOffload);
//			write_integer("./PlotBLLL/totalDenOffloadBLL.txt",totalDenOffload);
			write_double("./PlotBLLL/totalPowerSystemBLLL.txt",totalPowerSystem);
//			write_double("./PlotBLLL/totalEnergySystemBLLL.txt",totalEnergySystem);
//			write_double("./PlotBLLL/totalPowerSystemPerSFCBLL.txt",totalPowerPerSFC);
//			write_double("./PlotBLLL/totalEdgePowerSystemBLL.txt", totalEdgePowerSystem);
//			write_double("./PlotBLLL/totalServerPowerSystemBLL.txt", totalServerPowerSystem);
//			write_double("./PlotBLLL/totalLoadEdgeBLL.txt",totalLoadEdge);
//			write_double("./PlotBLLL/totalBwEdgeBLL.txt",totalBwEdge);
			write_double("./PlotBLLL/acceptanceRatioBLLL.txt",totalChainAcceptance);
//			write_double("./PlotBLLL/sumLoadNumPiBLL.txt", sumLoadNumPi);
//			write_double("./PlotBLLL/sumBwNumPiBLL.txt", sumBwNumPi);
//			write_integer("./PlotBLLL/totalChainSystemBLL.txt",totalChainSystem);
			write_integer("./PlotBLLL/totalChainActiveBLLL.txt",totalChainActive);
//			write_integer("./PlotBLLL/totalChainRejectBLL.txt",totalChainReject);
			write_integer("./PlotBLLL/totalChainRequestBLLL.txt",totalChainRequest);
//			write_integer("./PlotBLLL/numChainAcceptBLL.txt",numChainAccept);
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