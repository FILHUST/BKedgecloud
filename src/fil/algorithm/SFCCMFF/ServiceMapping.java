/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/
package fil.algorithm.SFCCMFF;

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
		this.setVNFmigration(0);
	}
	
	public LinkedList<SFC> run(LinkedList<SFC> listSFC, Topology topo) {
		
//		Map<Integer, PhysicalServer> listPhysical = topo.getListPhyServers();  //get list physical server
		LinkedList<SFC> listMappedSFC = new LinkedList<>(); 

		LinkedList<PhysicalServer> listServer = topo.getListPhyServers();
//		for(PhysicalServer phy : listPhysical.values()) {
//			listServer.add(phy);
//		}
		//====Start First-Fit algorithm to map VNF into physical server ===========//
		sfc_loop:
		for(SFC sfc : listSFC) {
			server_loop:
			for(PhysicalServer server : listServer) {
				
				double cpuServer = 0;
				for(int i = 4; i >= 2; i--) { // only consider services on cloud
					if(!sfc.getService(i).getBelongToEdge()) {
						if(sfc.getService(i).getBelongToServer() == null) { // service hasnt been mapped to any server
							cpuServer += sfc.getService(i).getCpu_server();  //calculate all cpu used by chain
						}
					}
				}
				
				if(cpuServer <= server.getRemainCPU()) {
					// finish here
					// break loop server + condition to break server loop
					listMappedSFC.add(sfc);
					// set sfc position
					for(int i = 4; i >= 2; i--) {
						if(!sfc.getService(i).getBelongToEdge()) {
							if(sfc.getService(i).getBelongToServer() == null)
								sfc.getService(i).setBelongToServer(server);
						}
					}
					server.getListSFCInServer().add(sfc);
					server.setUsedCPUServer(cpuServer);
					// only Receive on cloud will be added to the below list 
					if(sfc.getFirstServiceCloud().getServiceType() == "receive") {
						server.getListIndependRev().add(sfc);
					}
					break server_loop;
				}
				else { // case cpu is not enough for the whole sfc
					
					if(listServer.indexOf(server) == (listServer.size() - 1)) {
						continue sfc_loop;
					}
					
					if(sfc.getService(3).getBelongToEdge()) {
						continue; // if only service 4 cannot map then it cannot be separated
					}
					for(int i = 2; i <= 4; i++) { // only consider services on cloud
						if(!sfc.getService(i).getBelongToEdge()) {
							if(sfc.getService(i).getBelongToServer() == null) { // service hasnt been mapped to any server
								double cpuRemain = sfc.getService(i).getCpu_server();
								if(cpuRemain <= server.getRemainCPU()) {
									server.setUsedCPUServer(cpuRemain);
									server.getListSFCInServer().add(sfc);
									sfc.getService(i).setBelongToServer(server);
//									sfc.setSeparateService(true);
								}
								else {
									//continue server_loop;
								}
							} // what happen if belongtoserver != null??
						}
					} // end for i loop
					
				}
			} // end server_loop
		} // end sfc_loop
		
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
					//===return CPU===============================================//
					cpuSFC = sfc.getService(i).getCpu_server();
					server = sfc.getService(i).getBelongToServer();
					server.setUsedCPUServer(-cpuSFC); // return CPU	
					if(server.getListSFCInServer().contains(sfc))
						server.getListSFCInServer().remove(sfc);
					sfc.getService(i).setBelongToServer(null);// Kien fix 12052020
				}
			}
			
			//===remove sfc index inside all kind of list=========================//
			if(isSeparateSFC == false) {
				if(server.getListIndependRev().contains(sfc)) { // delete independent receive (if any)
					server.getListIndependRev().remove(sfc);
				}
			}
			
			
			//===return bandwidth for substratelink===============================//
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
		//===remapping if 2 service connect through aggr/core switch=========//
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
