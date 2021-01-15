/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/
package fil.algorithm.BLLL;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import fil.resource.substrate.*;
import fil.resource.virtual.*;


public class ServiceMapping {
	private int VNFmigration;
	
	public ServiceMapping() {
		this.VNFmigration = 0;
	}
	
	public LinkedList<SFC> run(LinkedList<SFC> listSFC, Topology topo) {
		
		LinkedList<SFC> listMappedSFC = new LinkedList<>(); 
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers();

		//<--------Start Least-Loaded algorithm
		for(SFC sfc : listSFC) {
			this.sortServerResource(listServer); // sort server in ascending order of used CPU
			for(int svInd = 0; svInd < listServer.size(); svInd ++) { // loop server
				
				PhysicalServer server = listServer.get(svInd);
				double cpuServer = sfc.cpuServerUsage();
				
				if(cpuServer <= server.getRemainCPU()) {
					for(int i = 2; i <= 4; i++) {
						if(sfc.getService(i).getBelongToServer() == null)
							sfc.getService(i).setBelongToServer(server);
					}
					
					server.setUsedCPUServer(cpuServer);
					server.getListSFCInServer().add(sfc);
					
					if(sfc.getFirstServiceCloud().getServiceType() == "receive") {
						server.getListIndependRev().add(sfc);
					}
					
					listMappedSFC.add(sfc);
					
					break; // break loop server
				}
				else { 
					continue;
				}
			}	

		}// for sfc loop

		return listMappedSFC;
	}
	
	public SubstrateSwitch getSwitchFromID(LinkedList<SubstrateSwitch> listSwitch, String id) {
		SubstrateSwitch s= new SubstrateSwitch();
		for(SubstrateSwitch sw: listSwitch)
			if(sw.getNameSubstrateSwitch().equals(id))
			{
				s= sw;
				break;
			}
		return s;
	}
	
	
public void resetSFC(LinkedList<SFC> listSFC, Topology topo) {
				
		for(SFC sfc : listSFC) {
			
			double cpuSFC = 0;
			PhysicalServer server = null;
			boolean isSeparateSFC = sfc.isSeparateService();
			
			for(int i = 4; i >= 2; i--) {
				if(!sfc.getService(i).getBelongToEdge()) {
					cpuSFC = sfc.getService(i).getCpu_server();
					server = sfc.getService(i).getBelongToServer();
					server.setUsedCPUServer(-cpuSFC); // return CPU	
					if(server.getListSFCInServer().contains(sfc))
						server.getListSFCInServer().remove(sfc);
					sfc.getService(i).setBelongToServer(null);// Kien fix 12052020
				}
			}
			
			if(isSeparateSFC == false) {
				if(server.getListIndependRev().contains(sfc)) { // delete independent receive (if any)
					server.getListIndependRev().remove(sfc);
				}
			}
			
			
			LinkedList<VirtualLink> listVirLink = sfc.getvLink();
			for(VirtualLink vLink : listVirLink) {
				double bandwidth = vLink.getBandwidthRequest();
				LinkedList<LinkPhyEdge> listPhyEdge = vLink.getLinkPhyEdge();
				for(LinkPhyEdge linkEdge : listPhyEdge) {
					linkEdge.setBandwidth(linkEdge.getBandwidth() + bandwidth);
					linkEdge.getEdgeSwitch().setPort(linkEdge.getEdgeSwitch(), (-1.0)*bandwidth);
				}
				LinkedList<SubstrateLink> listSubstrate = vLink.getLinkSubstrate();
				for(SubstrateLink linkSubstrate : listSubstrate) {
					linkSubstrate.setBandwidth(linkSubstrate.getBandwidth() + bandwidth);
					linkSubstrate.getStartSwitch().setPort(linkSubstrate.getEndSwitch(), (-1.0)*bandwidth);
				}
			}
			listVirLink.clear(); 
		}
	}
	
public boolean remappingAggrFarGroup(VirtualLink vLink, Topology topo) {

		boolean isSuccess = false;
//		Service sService = vLink.getsService();
//		Service dService = vLink.getdService();
//		
//		PhysicalServer phyA = sService.getBelongToServer();
//		PhysicalServer phyB = dService.getBelongToServer();
//				
//		LinkedList<SFC> listSFCA = phyA.getListSFCInServer();
//		LinkedList<SFC> listSFCB = phyB.getListSFCInServer();
//						
//		SFC sfcA = null; // sfc contains source service
//		SFC sfcB = null; // sfc contains destination service
//		
//		for(SFC sfc: listSFCA) {
//			if(sfc.getSfcID() == sService.getSfcID()) {
//				sfcA = sfc;
//			}
//		}
//		for(SFC sfc: listSFCB) {
//			if(sfc.getSfcID() == sService.getSfcID()) {
//				sfcB = sfc;
//			}
//		}
//		
//		
//		//===get all the independent receive in server A & B ===================//
//		int numReceiveA = 0;		
//		numReceiveA = phyA.getListIndependRev().size();	
//
//		//=======================================================================//
//		
//		//===get CPU demand for migrating dService to sService===================//
//		
//		double cpuDemand = dService.getCpu_server();
//		double cpuReceive = 5.0;
//		
//		if((numReceiveA)*cpuReceive >= cpuDemand) { //neu demand nho hon so receive doc lap thi tien hanh chuyen
//			double numReceiveEvacuate = Math.ceil(cpuDemand/cpuReceive);
//
//			LinkedList<SFC> sfcEvacuate = new LinkedList<>();
//			int sizeOfSFCEva = sfcEvacuate.size();
//			
//			for(int index = 0; index < numReceiveEvacuate; index++) {
//				SFC sfc = phyA.getListIndependRev().getFirst();
//				sfc.getService(4).setBelongToServer(null);
//				sfcEvacuate.add(sfc);
//				listSFCA.remove(sfc);
//				phyA.getListIndependRev().remove(sfc);
//			}
//			
//			LinkedList<SFC> listSFCMap = run(sfcEvacuate, topo);
//			// link wrong here
//			if(listSFCMap.size() < sizeOfSFCEva) { // return everything
//				isSuccess = false;
//				returnSFC(listSFCMap);
//			}
//			else {
//				this.setVNFmigration((int)numReceiveEvacuate + 1);
//				phyA.setUsedCPUServer(-(numReceiveEvacuate)*cpuReceive + cpuDemand);
//				phyB.setUsedCPUServer(-cpuDemand);
//				dService.setBelongToServer(sService.getBelongToServer());
//				if(sfcA.allServiceInSameServer()) {
//					sfcA.setSeparateService(false);
//					
//				}
//				if(!sfcB.existServiceInServer(phyB))
//					listSFCB.remove(sfcB);
//				
//				isSuccess = true;
//			}
//			
//		}
//		else {
//			isSuccess = false;
//		}
		return isSuccess;
	}

	public void returnSFC(LinkedList<SFC> listSFC) {
		for(SFC sfc : listSFC) {
			double cpu;
			PhysicalServer server = null;
			for(int i = 4; i >= 2; i --) {
				if(!sfc.getService(i).isBelongToEdge()) {
					cpu = sfc.getService(i).getCpu_server();
					server = sfc.getService(i).getBelongToServer();
					server.setUsedCPUServer(-cpu);
					if(server.getListSFCInServer().contains(sfc))
						server.getListSFCInServer().remove(sfc);
					if(server.getListIndependRev().contains(sfc))
						server.getListIndependRev().remove(sfc);
				}
				else
					break;
			}
			
		}
	}

	public HashMap<Integer, PhysicalServer> sortListServer(HashMap<Integer, PhysicalServer> list) {
		
		//create a list from elements of HashMap
		LinkedList<Map.Entry<Integer, PhysicalServer> > listMap = new LinkedList<Map.Entry<Integer, PhysicalServer>>(list.entrySet());
		
		//sort the list
		Collections.sort(listMap, new Comparator<Map.Entry<Integer, PhysicalServer>>() {
			@Override
			public int compare(Map.Entry<Integer, PhysicalServer> o1, Map.Entry<Integer, PhysicalServer> o2) {
				if (o1.getValue().getUsedCPUServer() > o2.getValue().getUsedCPUServer()) {
					return -1;
				}
				if (o1.getValue().getUsedCPUServer() < o2.getValue().getUsedCPUServer()) {
					return 1;
				}
				return 0;
			}
		});
		
		//put data from sorted list to HashMap
		HashMap<Integer, PhysicalServer> temp = new LinkedHashMap<Integer, PhysicalServer>();
		for(Map.Entry<Integer, PhysicalServer> aa : listMap) {
			temp.put(aa.getKey(), aa.getValue());
		}
		return temp;
	}

	public LinkedList<PhysicalServer> sortServerResource(LinkedList<PhysicalServer> listServer){
		Collections.sort(listServer, new Comparator<PhysicalServer>() {
			@Override
	        public int compare(PhysicalServer server1, PhysicalServer server2) { 
	            // for comparison
				int cpuCompare = 0;
				if(server1.getUsedCPUServer() < server2.getUsedCPUServer())
					cpuCompare = -1;
				else if(server1.getUsedCPUServer() > server2.getUsedCPUServer())
					cpuCompare = 1;
				else if(server1.getUsedCPUServer() == server2.getUsedCPUServer())
					cpuCompare = 0;
	            int nameCompare =server1.getName().compareTo(server2.getName()); 
	  
	            // 2-level comparison using if-else block 
	            if (cpuCompare == 0) { 
	                return nameCompare; 
	            } else { 
	                return cpuCompare; 
	            } 
	        }
		});
		return listServer;
	}

	public double getPowerServer(Topology topo) {
		double power = 0;
		LinkedList<PhysicalServer> listPhysical = topo.getListPhyServers(); 
		for(PhysicalServer phy : listPhysical) {
			if(phy.getState() == 1) {
				phy.setPowerServer();
				power += phy.getPowerServer();;
			}
			else
				continue;
		}
		return power;
	}

	public int getVNFmigration() {
		return VNFmigration;
	}

	public void setVNFmigration(int vNFmigration) {
		VNFmigration = vNFmigration;
	}

}
