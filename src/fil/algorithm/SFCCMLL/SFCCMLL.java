/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.SFCCMLL;

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

public class SFCCMLL  {
	
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

	public SFCCMLL() { // default constructor
		
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
	

	public static void SFCCMMapping(Topology topo, MappingServer mappingServer, Rpi pi, LinkedList<SFC> listChainRequest, 
			LinkedList<SFC> listSFConRpi, LinkedList<SFC> listSFCFinalPi) {
		
		boolean doneFlag = false;
		
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
			
			LinkedList<SFC> listSFCTemp = new LinkedList<>();
			int numChainRequest = listChainRequest.size();
					
			for(int numSFC = 0; numSFC < numChainRequest; numSFC++ ) { // initialize SFC list
				SFC sfc = new SFC();  // create a default SFC
				//* cost in SFCCM algorithm is applied in this model as remained resource 
				//  of device and remained bandwidth
				//*
				sfc = costModel(sfc, pi, topo);
				
				sfc.setServicePosition(capture.getServiceType(), true);
				sfc.setServicePosition(receive.getServiceType(), false);

				listSFCTemp.add(sfc);
				
			}
					
			double bandwidthPiUsed = numChainRequest*density.getBandwidth();
			double cpuPiUsed = numChainRequest*capture.getCpu_pi() + numChainRequest*decode.getCpu_pi() + numChainRequest*density.getCpu_pi();
					
				if (cpuPiUsed > pi.getRemainCPU()){
					listChainRequest.removeLast();
					if(listChainRequest.size() == 0) {
						pi.setOverload(false);
						break MAP_LOOP;
					}
					doneFlag = false;
					continue;
				}
				else if(bandwidthPiUsed > pi.getRemainBandwidth()) {
					listChainRequest.removeLast(); // remove last object
					if(listChainRequest.size() == 0) {
						break MAP_LOOP;
					} else continue;
				}
				else {
					doneFlag = true; // used to break MAP_LOOP
					listSFCFinalPi.clear();
					for(int index = 0; index < listSFCTemp.size(); index++) {
						listChainRequest.get(index).copy(listSFCTemp.get(index));
						listSFCFinalPi.add(listChainRequest.get(index));
					}
				} 
				
			}
		}
	
	public static SFC costModel(SFC sfc, Rpi pi, Topology topo) {
		
//		double W_e_cpu = 1; // constant weight 
//		double W_c_cpu = 1;
//		double W_bw = 1;
//		
		double minValue = 0.01;
		double maxValue = 1;
		
		double remainCPU = 100 - pi.getUsedCPU();
		if(remainCPU < 1) remainCPU = 1;
		double remainBW = 100 - pi.getUsedBandwidth();
		if(remainCPU < 1) remainBW = 1;
		double remainCPUCloud = topo.getRemainCPUCloud();
		
//		double cpuCostEdge = 1*W_e_cpu/remainCPU;
		double cpuCostCloud = normalize(1/remainCPUCloud, minValue, maxValue);
//		double bwCost = 1*W_bw/remainBW;
		double cpuCostEdge = normalize(1/remainCPU, minValue, maxValue);
//		double cpuCostCloud = 0.01;
		double bwCost = normalize(1/remainBW, minValue, maxValue);
		
		Capture capture = new Capture();
		Decode decode = new Decode();
		Density density = new Density();
		
		double costDecodeEdge = decode.getCpu_pi()*cpuCostEdge + decode.getBandwidth()*bwCost;
		double costDecodeCloud = decode.getCpu_server()*cpuCostCloud + capture.getBandwidth()*bwCost;
		double costDensityEdge = density.getCpu_pi()*cpuCostEdge + density.getBandwidth()*bwCost;
		double costDensityCloud = density.getCpu_server()*cpuCostCloud + decode.getBandwidth()*bwCost;
		
		if(costDecodeEdge < costDecodeCloud)
			sfc.setServicePosition(decode.getServiceType(), true);
		else
			sfc.setServicePosition(decode.getServiceType(), false);
		
		if(costDensityEdge < costDensityCloud)
			sfc.setServicePosition(density.getServiceType(), true);
		else
			sfc.setServicePosition(density.getServiceType(), false);
		
		return sfc;
	}
	
	public static double normalize(double value, double min, double max) {
	    return 1 - ((value - min) / (max - min));
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
//		LinkedList<Double> totalEnergySystem = new LinkedList<Double>();
		LinkedList<Double> serverUtilization = new LinkedList<Double>();
		LinkedList<Double> systemUtilization = new LinkedList<Double>();
		LinkedList<Double> totalChainAcceptance = new LinkedList<Double>();
		LinkedList<Integer> listServerUsed = new LinkedList<Integer>();
		LinkedList<Integer> totalChainSystem = new LinkedList<Integer>();
		LinkedList<Integer> totalChainActive = new LinkedList<Integer>();
		LinkedList<Integer> totalChainLeave = new LinkedList<Integer>();
		LinkedList<Integer> totalChainRequest = new LinkedList<>();
		
		
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
						SFCCMMapping(topo, mappingServer, pi, event.getListSfc(), listCurSFCOnPi, listSFCFinalPi);
					}
					if(listSFCFinalPi.size() != 0) { // case Pi mapping is success
						
//						run mapping server for final listSFC
//						Random rand = new Random();
//						int position = rand.nextInt(4); // random a position where request comes from
//						
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
				power1h += power*(event.getTime() - time)/1000*1.0; //kW
				time = event.getTime();
				
			} // event loop
				
			acceptance = (numMapReqThisTW*1.0)/numSFCReqThisTW; //after a request
			totalChainAcceptance.add(acceptance);
			totalChainRequest.add(numSFCReqThisTW);
			totalChainActive.add(numSFCActive);
			systemUtilization.add(numSFCActive*1.0/SYS_CAPACITY);
			totalPowerSystem.add(power1h);
			serverUtilization.add(topo.getCPUServerUtilization());
			listServerUsed.add(topo.getServerUsed());
				
				
				

		} // end while loop (request)
		
		
		try {
			Path path = Paths.get("./PlotSFCCMLL");
		    //java.nio.file.Files;
		    Files.createDirectories(path);

		    System.out.println("Directory is created!");
		    
//			write_double("./PlotSFCCMLL/totalPiAcceptanceSFCCMLL.txt",totalPiAcceptance);
//			write_double("./PlotSFCCMLL/capacitySFCCMLL.txt",capacity);
//			write_double("./PlotSFCCMLL/capacityEdgeSFCCMLL.txt",capacityEdge);
//			write_double("./PlotSFCCMLL/capacityCloudSFCCMLL.txt",capacityCloud);
//			write_double("./PlotSFCCMLL/averageBWUsageSFCCMLL.txt",averageBWUsage);
//			write_double("./PlotSFCCMLL/cpuServerUsedSFCCMLL.txt",cpuServerUsed);
//			write_double("./PlotSFCCMLL/totalPowerSystemConsolidationSFCCMLL.txt",totalPowerSystemConsolidation);
//			write_double("./PlotSFCCMLL/listLinkUsageSFCCMLL.txt",listLinkUsage);
//			write_double("./PlotSFCCMLL/cpuEdgeUsagePerSFCSFCCMLL.txt",cpuEdgeUsagePerSFC);
//			write_double("./PlotSFCCMLL/cpuServerUsagePerSFCSFCCMLL.txt",cpuServerUsagePerSFC);
//			write_double("./PlotSFCCMLL/linkBandwidthSFCCMLL.txt",linkBandwidth);
			write_double("./PlotSFCCMLL/serverUtilizationSFCCMLL.txt",serverUtilization);
			write_double("./PlotSFCCMLL/systemUtilizationSFCCMLL.txt",systemUtilization);

//			write_integer("./PlotSFCCMLL/NumVNFMigrationSFCCMLL.txt",listVNFmigration);
//			write_integer("./PlotSFCCMLL/NumServiceDecDenSFCCMLL.txt",NumServiceDecDen);
			write_integer("./PlotSFCCMLL/totalChainLeaveSFCCMLL.txt",totalChainLeave);
			write_integer("./PlotSFCCMLL/listServerUsedSFCCMLL.txt",listServerUsed);
//			write_integer("./PlotSFCCMLL/requestRandomSFCCMLL.txt",requestRandomReceive);
//			write_integer("./PlotSFCCMLL/totalDecOffloadSFCCMLL.txt",totalDecOffload);
//			write_integer("./PlotSFCCMLL/totalDenOffloadSFCCMLL.txt",totalDenOffload);
			write_double("./PlotSFCCMLL/totalPowerSystemSFCCMLL.txt",totalPowerSystem);
//			write_double("./PlotSFCCMLL/totalEnergySystemSFCCMLL.txt",totalEnergySystem);
//			write_double("./PlotSFCCMLL/totalPowerSystemPerSFCSFCCMLL.txt",totalPowerPerSFC);
//			write_double("./PlotSFCCMLL/totalEdgePowerSystemSFCCMLL.txt", totalEdgePowerSystem);
//			write_double("./PlotSFCCMLL/totalServerPowerSystemSFCCMLL.txt", totalServerPowerSystem);
//			write_double("./PlotSFCCMLL/totalLoadEdgeSFCCMLL.txt",totalLoadEdge);
//			write_double("./PlotSFCCMLL/totalBwEdgeSFCCMLL.txt",totalBwEdge);
			write_double("./PlotSFCCMLL/acceptanceRatioSFCCMLL.txt",totalChainAcceptance);
//			write_double("./PlotSFCCMLL/sumLoadNumPiSFCCMLL.txt", sumLoadNumPi);
//			write_double("./PlotSFCCMLL/sumBwNumPiSFCCMLL.txt", sumBwNumPi);
			write_integer("./PlotSFCCMLL/totalChainSystemSFCCMLL.txt",totalChainSystem);
			write_integer("./PlotSFCCMLL/totalChainActiveSFCCMLL.txt",totalChainActive);
//			write_integer("./PlotSFCCMLL/totalChainRejectSFCCMLL.txt",totalChainReject);
			write_integer("./PlotSFCCMLL/totalChainRequestSFCCMLL.txt",totalChainRequest);
//			write_integer("./PlotSFCCMLL/numChainAcceptSFCCMLL.txt",numChainAccept);
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