/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.SFCCMFF;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import fil.resource.substrate.*;
import fil.resource.virtual.*;

public class LinkMappingBackUp {
	//private LinkedList<VirtualLink> listVirLink;
	private Map<LinkPhyEdge, Double> listBandwidthPhyEdge;
	private LinkedList<SubstrateLink> listLinkCore;
	private LinkedList<SubstrateLink> listLinkAgg;
	private int numLinkSuccess;
	private double powerConsumed;
	
	public LinkMappingBackUp() {
	//	listVirLink = new LinkedList<>();
		listBandwidthPhyEdge = new HashMap<>();
		powerConsumed = 0;
		numLinkSuccess= 0;
		listLinkCore = new LinkedList<>();
		listLinkAgg = new LinkedList<>();
	}

	public void linkMapExternal(Topology topo, LinkedList<SFC> listSFC, ServiceMapping serviceMapping) {
		
		listLinkCore = new LinkedList<>();
		listLinkAgg = new LinkedList<>();
		
		LinkedList<SFC> listFailedSFC = new LinkedList<>();
		
		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
		LinkedList<LinkPhyEdge> listLinkPhyEdge = topo.getListLinkPhyEdge();
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge = topo.getListAggConnectEdge();
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAggMap = topo.getListCoreConnectAgg();	
		
		SFC_LOOP:
		for(SFC sfc : listSFC) { // get edge switch connect to server
			
			SubstrateSwitch substrateAggr = null;
			SubstrateSwitch substrateCore = null;
			SubstrateSwitch edgeSwitch = null;
			
			SubstrateLink edgeToAggr = null;
			SubstrateLink aggrToEdge = null;
			SubstrateLink coreToAggr = null;
			SubstrateLink aggrToCore = null;
			LinkPhyEdge linkPhyEdge = null;
			
			boolean checkAgg = false;
			boolean checkCore = false;
			
			Service service = sfc.getFirstServiceCloud();
			PhysicalServer server = service.getBelongToServer();
			double bandwidthDemand = sfc.getLastServiceEdge().getBandwidth();
			
			for(LinkPhyEdge linkEdge : listLinkPhyEdge) {
				if(linkEdge.getPhysicalServer().equals(server)) { 
					if(linkEdge.getBandwidth() < bandwidthDemand) {
						listFailedSFC.add(sfc);
						continue SFC_LOOP;
					}
					else {
						edgeSwitch = linkEdge.getEdgeSwitch();
						linkPhyEdge = linkEdge;
						break;
					}
				}
			}
			
//			if(edgeSwitch == null && !topo.getListPhyServers().contains(server))
//				System.out.println();
			//find aggregation switch
			LinkedList<SubstrateSwitch> listAgg = sortListSwitch(listAggConnectEdge.get(edgeSwitch));
			
			
			int countLink = 0;

			FIND_AGG_SW:
			for(SubstrateSwitch aggSwitch : listAgg) { // find in MST first
				countLink = 0;
				for(SubstrateLink link : listLinkBandwidth) {
					if(link.getStartSwitch().equals(aggSwitch) && link.getEndSwitch().equals(edgeSwitch) && link.getBandwidth() >= bandwidthDemand) {
						substrateAggr = aggSwitch;
						aggrToEdge = link;
						countLink ++;
					} else if(link.getStartSwitch().equals(edgeSwitch) && link.getEndSwitch().equals(aggSwitch) && link.getBandwidth() >= bandwidthDemand) {
						substrateAggr = aggSwitch;
						edgeToAggr = link;
						countLink ++;
					}
					if(countLink == 2) {
						checkAgg = true;
						break;
					}
					if(link.equals(listLinkBandwidth.getLast()) && countLink != 2) {
						checkAgg = false;
						break;
					}
				}
				
				if(checkAgg == true) {
					LinkedList<SubstrateSwitch> listCore = sortListSwitch(listCoreConnectAggMap.get(substrateAggr));
					
					FIND_CORE_SW:
					for(SubstrateSwitch coreSwitch : listCore) { //error here, find out edge switch again
						if(coreSwitch.equals(edgeSwitch))
							continue;
						countLink = 0;
						checkCore = false;
						for(SubstrateLink link : listLinkBandwidth) {
							if(link.getStartSwitch().equals(substrateAggr) && link.getEndSwitch().equals(coreSwitch)) {
								if(link.getBandwidth() >= bandwidthDemand) {
									substrateCore = coreSwitch;
									aggrToCore = link;
									countLink ++;
								}
							} 
							
							if(link.getStartSwitch().equals(coreSwitch) && link.getEndSwitch().equals(substrateAggr)) {
								if(link.getBandwidth() >= bandwidthDemand) {
									substrateCore = coreSwitch;
									coreToAggr = link;
									countLink ++;
								}
							}
							if(countLink == 2) {
								checkCore = true;
								break;
							}
							if(link.equals(listLinkBandwidth.getLast()) && countLink != 2) {
								checkCore = false;
								break;
							}
						}
						
						if(checkCore == true) {
							VirtualLink vLink = new VirtualLink();
							linkPhyEdge.setBandwidth(linkPhyEdge.getBandwidth() - bandwidthDemand);
//							edgeSwitch.setPort(edgeSwitch, bandwidthDemand);
							edgeToAggr.setBandwidth(edgeToAggr.getBandwidth() - bandwidthDemand);
							edgeSwitch.setPort(substrateAggr, bandwidthDemand);
							aggrToEdge.setBandwidth(aggrToEdge.getBandwidth() - bandwidthDemand);
							substrateAggr.setPort(edgeSwitch, bandwidthDemand);
							aggrToCore.setBandwidth(aggrToCore.getBandwidth() - bandwidthDemand);
							substrateAggr.setPort(substrateCore, bandwidthDemand);
							coreToAggr.setBandwidth(coreToAggr.getBandwidth() - bandwidthDemand);
							substrateCore.setPort(substrateAggr, bandwidthDemand);
							vLink.getLinkSubstrate().add(edgeToAggr);
							vLink.getLinkSubstrate().add(aggrToEdge);
							vLink.getLinkSubstrate().add(aggrToCore);
							vLink.getLinkSubstrate().add(coreToAggr);
							vLink.getLinkPhyEdge().add(linkPhyEdge);
							vLink.setBandwidthRequest(bandwidthDemand);
							sfc.getvLink().add(vLink);
							break FIND_AGG_SW;
						}
						else { // checkCore fails
							if(coreSwitch.equals(listCore.getLast())) {
								listFailedSFC.add(sfc);
								break FIND_AGG_SW;
							}
							else
								continue;
						}
					} // core loop
					
				} 
				else { // checkAggr fails
					if(aggSwitch.equals(listAgg.getLast())) {
						listFailedSFC.add(sfc);
						break FIND_AGG_SW;
					}
					else
						continue;
				}
				
			}// agg loop
			

		} // end for loop SFC
		
		if(!listFailedSFC.isEmpty()) {
			for(SFC sfc : listFailedSFC) {
				listSFC.remove(sfc);
			}
			if(listSFC.isEmpty()) // no SFC can be mapped
				System.out.println("SFC fails due to external link failed.");
			serviceMapping.returnSFC(listFailedSFC); // return CPU for server cause link failed
		}
	}
	
	
	public void linkMapInternal(Topology topo, LinkedList<SFC> listSFC, ServiceMapping serviceMapping) {

		Service serviceA = new Service();
		Service serviceB = new Service();
		
		LinkedList<SFC> listFailedSFC = new LinkedList<>();

		
		// block below examines bandwidth, servers between two services
		for(SFC sfc : listSFC) {
			
			Service service = sfc.getFirstServiceCloud();
			double bandwidth = 0;
			boolean subResult = false;
			
			if(service.getServiceType() == "density") {
				if(!sfc.getService(3).getBelongToServer().equals(sfc.getService(4).getBelongToServer())) {
					bandwidth = sfc.getService(3).getBandwidth();
					serviceA = sfc.getService(3);
					serviceB = sfc.getService(4);
					VirtualLink vLink = new VirtualLink();
					vLink = new VirtualLink(serviceA, serviceB, bandwidth);
					System.out.println("Internal density block");
					subResult = linkMappingSeparate(topo, serviceMapping, vLink);
					if (subResult == true)
						sfc.getvLink().add(vLink);
					else
						listFailedSFC.add(sfc);
				}else {
					subResult = true;
				}
			} else if(service.getServiceType() == "decode") {
				
				boolean subResult1 = false;
				boolean subResult2 = false;
				VirtualLink vLink1 = null;
				VirtualLink vLink2 = null;
				System.out.println("Internal decode block");
				if(!sfc.getService(2).getBelongToServer().equals(sfc.getService(3).getBelongToServer())) {
					bandwidth = sfc.getService(2).getBandwidth();
					serviceA = sfc.getService(2);
					serviceB = sfc.getService(3);	
					vLink1 = new VirtualLink(serviceA, serviceB, bandwidth);
					subResult1 = linkMappingSeparate(topo, serviceMapping, vLink1);
				}else
					subResult1 = true;
				
				if(!sfc.getService(3).getBelongToServer().equals(sfc.getService(4).getBelongToServer())) {
					bandwidth = sfc.getService(3).getBandwidth();
					serviceA = sfc.getService(3);
					serviceB = sfc.getService(4);
					vLink2 = new VirtualLink(serviceA, serviceB, bandwidth);
					subResult2 = linkMappingSeparate(topo, serviceMapping, vLink2);
				}else
					subResult2 = true;
				
				subResult = subResult1 & subResult2;
				if(subResult == true) {
					if(vLink1 != null)
						sfc.getvLink().add(vLink1);
					if(vLink2 != null)
						sfc.getvLink().add(vLink2);
				}
				else
					listFailedSFC.add(sfc);
			} else {
				continue;
			}
		}
		//===remove all the SFC that failed creating internal link=====//
		if(!listFailedSFC.isEmpty()) {
			for (SFC sfc : listFailedSFC) {
				if(listSFC.contains(sfc)) {
					listSFC.remove(sfc);
				}
			}
			serviceMapping.returnSFC(listFailedSFC);  // return CPU to server cause link failed
		}
	}
	
	private boolean linkMappingSeparate(Topology topo, ServiceMapping serviceMapping, VirtualLink vLink) {
		
		boolean result = false;
		
		LinkedList<LinkPhyEdge> listPhyEdge = topo.getListLinkPhyEdge();

		SubstrateSwitch edgeSwitch1 = new SubstrateSwitch();
		SubstrateSwitch edgeSwitch2 = new SubstrateSwitch();
		
		PhysicalServer phy1 = vLink.getsService().getBelongToServer();
		PhysicalServer phy2 = vLink.getdService().getBelongToServer();
		
		LinkPhyEdge phy2Edge1 = null, phy2Edge2 = null;
		
		int countP2E = 0;
		double bandwidth = vLink.getBandwidthRequest();
		
		for(LinkPhyEdge linkPhyEdge : listPhyEdge) {
			if(linkPhyEdge.getPhysicalServer().equals(phy1)) {
				edgeSwitch1 = linkPhyEdge.getEdgeSwitch();
				phy1 = linkPhyEdge.getPhysicalServer();
				phy2Edge1 = linkPhyEdge;
				countP2E++;
			} else if (linkPhyEdge.getPhysicalServer().equals(phy2)) {
				edgeSwitch2 = linkPhyEdge.getEdgeSwitch();
				phy2 = linkPhyEdge.getPhysicalServer();
				phy2Edge2 = linkPhyEdge;
				countP2E++;
			}

			if(countP2E == 2) {
				break;
			}
		}
		
		if(phy2Edge1.getBandwidth() <= bandwidth || phy2Edge2.getBandwidth() <= bandwidth) {
			if(serviceMapping.remappingAggrFarGroup(vLink, topo))
				return true;
			else
				return false;	
		}
		
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge = topo.getListAggConnectEdge();
		LinkedList<SubstrateSwitch> listAggConnectStartEdge = new LinkedList<>();
		LinkedList<SubstrateSwitch> listAggConnectEndEdge = new LinkedList<>();
		
		// near groups
		if (edgeSwitch1.equals(edgeSwitch2)) {
			///////////////////////////////////////////////////////
			LinkedList<LinkPhyEdge> phyEdge = new LinkedList<>();
			phy2Edge1.setBandwidth(phy2Edge1.getBandwidth() - bandwidth);
			phy2Edge2.setBandwidth(phy2Edge2.getBandwidth() - bandwidth);
			phyEdge.add(phy2Edge1);
			phyEdge.add(phy2Edge2);
			vLink.setLinkPhyEdge(phyEdge); 
			//edgeSwitch1.setPort(edgeSwitch1, bandwidth);
			//edgeSwitch2.setPort(edgeSwitch2, bandwidth);
			result = true;
		} else {
			//check if aggregation or core
			int count = 0;
			for (Entry<SubstrateSwitch, LinkedList<SubstrateSwitch>> entry : listAggConnectEdge.entrySet()) {
				if (entry.getKey().equals(edgeSwitch1)) {
					listAggConnectStartEdge = entry.getValue();
					count++;
				}
					
				if (entry.getKey().equals(edgeSwitch2)) {
					listAggConnectEndEdge = entry.getValue();
					count++;
				}
				
				if(count == 2) {
					break;
				}
			}
			// sort list Agg
			listAggConnectStartEdge = sortListSwitch(listAggConnectStartEdge);
			listAggConnectEndEdge = sortListSwitch(listAggConnectEndEdge);

			// check middle groups
			if (listAggConnectStartEdge.equals(listAggConnectEndEdge)) {
				if(serviceMapping.remappingAggrFarGroup(vLink, topo) == true) {
					result = true;
				} else {
					result = linkMappingAggSeparate(vLink, edgeSwitch1, edgeSwitch2, listAggConnectStartEdge, topo);
				}
			} else {
				// check far groups
				if(serviceMapping.remappingAggrFarGroup(vLink, topo) == true) {
					result = true;
				} else {
					result = linkMappingCoreSeparate(vLink, edgeSwitch1, edgeSwitch2, topo);
				}
			}
		} 
		return result;
	}
	
	private boolean linkMappingAggSeparate(VirtualLink vLink, SubstrateSwitch edgeSwitch1, SubstrateSwitch edgeSwitch2, 
			LinkedList<SubstrateSwitch> listAggConnectStartEdge, Topology topo) {
		// TODO Auto-generated method stub
		boolean success = false;
		boolean checkAgg = false;
		boolean checkEdge = false;
		double bandwidth = vLink.getBandwidthRequest();
		
		LinkedList<LinkPhyEdge> listPhyEdge = topo.getListLinkPhyEdge();
		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
		
		PhysicalServer phy1 = vLink.getsService().getBelongToServer();
		PhysicalServer phy2 = vLink.getdService().getBelongToServer();
		
		SubstrateLink linkAggEdge01 = new SubstrateLink();
		SubstrateLink linkAggEdge10 = new SubstrateLink();
		SubstrateLink linkAggEdge02 = new SubstrateLink();
		SubstrateLink linkAggEdge20 = new SubstrateLink();
		
		LinkPhyEdge linkPhyEdge1 = new LinkPhyEdge();
		LinkPhyEdge linkPhyEdge2 = new LinkPhyEdge();
		
		SubstrateSwitch aggSW = new SubstrateSwitch();
		
		
		for(SubstrateSwitch sw : listAggConnectStartEdge) {
			int count = 0;
			for(SubstrateLink link : listLinkBandwidth) {
				if(link.getStartSwitch().equals(sw) && link.getEndSwitch().equals(edgeSwitch1) && link.getBandwidth() >= bandwidth) {
					count++;
					linkAggEdge01 = link;
				} else if(link.getStartSwitch().equals(edgeSwitch1) && link.getEndSwitch().equals(sw) && link.getBandwidth() >= bandwidth) {
					count++;
					linkAggEdge10 = link;
				}
				
				if(link.getStartSwitch().equals(sw) && link.getEndSwitch().equals(edgeSwitch2) && link.getBandwidth() >= bandwidth) {
					count++;
					linkAggEdge02 = link;
				} else if(link.getStartSwitch().equals(edgeSwitch2) && link.getEndSwitch().equals(sw) && link.getBandwidth() >= bandwidth) {
					count++;
					linkAggEdge20 = link;
				}
				
				if(count == 4) {
					aggSW = sw;
					checkAgg = true;
					break;
				}
			}
		}
		int count = 0;
		for(LinkPhyEdge link : listPhyEdge) {
			if(link.getEdgeSwitch().equals(edgeSwitch1) && link.getPhysicalServer().equals(phy1) && link.getBandwidth() >= bandwidth) {
				linkPhyEdge1 = link;
				count++;
			}
			
			if(link.getEdgeSwitch().equals(edgeSwitch2) && link.getPhysicalServer().equals(phy2) && link.getBandwidth() >= bandwidth) {
				linkPhyEdge2 = link;
				count++;
			}
			
			if(count == 2) {
				checkEdge = true;
				break;
			}
		}
		
		if(checkAgg == true && checkEdge == true) {
			
			success = true;
			
			LinkedList<SubstrateSwitch> listSWUsed = topo.getListSwitchUsed();
			boolean checkContain = false;
			for(SubstrateSwitch sw : listSWUsed) {
				if(sw.getNameSubstrateSwitch().equals(aggSW.getNameSubstrateSwitch())) {
					checkContain = true;
					break;
				}
			}
			
			if(checkContain == false) {
				listSWUsed.add(aggSW);
				topo.setListSwitchUsed(listSWUsed);
			}
			
			linkAggEdge01.setBandwidth(linkAggEdge01.getBandwidth() - bandwidth);
			linkAggEdge01.getStartSwitch().setPort(linkAggEdge01.getEndSwitch(), bandwidth);
			linkAggEdge10.setBandwidth(linkAggEdge10.getBandwidth() - bandwidth);
			linkAggEdge10.getStartSwitch().setPort(linkAggEdge10.getEndSwitch(), bandwidth);
			linkAggEdge02.setBandwidth(linkAggEdge02.getBandwidth() - bandwidth);
			linkAggEdge02.getStartSwitch().setPort(linkAggEdge02.getEndSwitch(), bandwidth);
			linkAggEdge20.setBandwidth(linkAggEdge20.getBandwidth() - bandwidth);
			linkAggEdge20.getStartSwitch().setPort(linkAggEdge20.getEndSwitch(), bandwidth);
			vLink.getLinkSubstrate().add(linkAggEdge01);
			vLink.getLinkSubstrate().add(linkAggEdge10);
			vLink.getLinkSubstrate().add(linkAggEdge02);
			vLink.getLinkSubstrate().add(linkAggEdge20);
			
			linkPhyEdge1.setBandwidth(linkPhyEdge1.getBandwidth() - bandwidth);
			linkPhyEdge1.getEdgeSwitch().setPort(linkPhyEdge1.getEdgeSwitch(), bandwidth);
			linkPhyEdge2.setBandwidth(linkPhyEdge2.getBandwidth() - bandwidth);
			linkPhyEdge2.getEdgeSwitch().setPort(linkPhyEdge2.getEdgeSwitch(), bandwidth);			
			vLink.getLinkPhyEdge().add(linkPhyEdge1);
			vLink.getLinkPhyEdge().add(linkPhyEdge2);
			
		} else {
			success = false;
		}
		
		return success;
	}

	private boolean linkMappingCoreSeparate( VirtualLink vLink, SubstrateSwitch edgeSwitch1,
			SubstrateSwitch edgeSwitch2, Topology topo) {
		// TODO Auto-generated method stub
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge = topo.getListAggConnectEdge();
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAggMap = topo.getListCoreConnectAgg();	
		LinkedList<LinkPhyEdge> listPhyEdge = topo.getListLinkPhyEdge();
		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
		LinkedList<SubstrateSwitch> listAggSort1 = new LinkedList<>();
		LinkedList<SubstrateSwitch> listAggSort2 = new LinkedList<>();
		LinkedList<SubstrateSwitch> listCoreSort1 = new LinkedList<>();
		LinkedList<SubstrateSwitch> listCoreSort2 = new LinkedList<>();
		
		Service sService = vLink.getsService();
		Service dService = vLink.getdService();
		
		double bandwidthDemand = vLink.getBandwidthRequest();
		int count = 0;
		
		SubstrateSwitch edge1 = null, edge2 = null;		
		LinkPhyEdge linkEdge1 = null, linkEdge2 = null;
		
		SubstrateLink linkAggEdge1a = null, linkAggEdge1b = null;
		SubstrateLink linkAggEdge2a = null, linkAggEdge2b = null;
		SubstrateLink linkCoreAgg1a = null, linkCoreAgg1b = null;
		SubstrateLink linkCoreAgg2a = null, linkCoreAgg2b = null;
		//===get edge switch connect to server=====================================//
		for (LinkPhyEdge linkPhyEdge: listPhyEdge) { 
			
			if(linkPhyEdge.getPhysicalServer().equals(sService.getBelongToServer())){
				edge1 = linkPhyEdge.getEdgeSwitch();
				if(linkPhyEdge.getBandwidth() < bandwidthDemand) {
					return false;
				}else {
					linkEdge1 = linkPhyEdge;
				}
					
			}
			if(linkPhyEdge.getPhysicalServer().equals(dService.getBelongToServer())) {
				edge2 = linkPhyEdge.getEdgeSwitch();
				if(linkPhyEdge.getBandwidth() < bandwidthDemand) {
					return false;
				}else {
					linkEdge2 = linkPhyEdge;
				}
			}
		}
		
		boolean checkAgg1 = false; boolean checkAgg2 = false;
		boolean checkCore = false;
		listAggSort1 = sortListSwitch(listAggConnectEdge.get(edge1));
		listAggSort2 = sortListSwitch(listAggConnectEdge.get(edge2));
	
		//=== find link connect Agg to Edge ======================================//
		AGG_EDGE_LOOP1:
		for(SubstrateSwitch agg1 : listAggSort1) {
			count = 0;
			for (SubstrateLink link : listLinkBandwidth) {
				if(link.getStartSwitch() == edge1 && link.getEndSwitch() == agg1) {
					if(link.getBandwidth() < bandwidthDemand) {
						break;
					}else {
						linkAggEdge1a = link;
						count ++;
					}
				}
				if(link.getStartSwitch() == agg1 && link.getEndSwitch() == edge1) {
					if(link.getBandwidth() < bandwidthDemand) {
						break;
					}else {
						linkAggEdge1b = link;
						count ++;
					}
				}
				if(count == 2) {
					checkAgg1 = true;
					break;
				}
				if(link.equals(listLinkBandwidth.getLast()) && count != 2) {
					checkAgg1 = false;
					break;
				}
				
			}
			
			if(checkAgg1) {
				// check Agg 2
				AGG_EDGE_LOOP2:
				for(SubstrateSwitch agg2 : listAggSort2) {
					count = 0;
					for (SubstrateLink link : listLinkBandwidth) {
						if(link.getStartSwitch() == edge2 && link.getEndSwitch() == agg2) {
							if(link.getBandwidth() < bandwidthDemand) {
								break;
							}else {
								linkAggEdge2a = link;
								count ++;
							}
						}
						else if(link.getStartSwitch() == agg2 && link.getEndSwitch() == edge2) {
							if(link.getBandwidth() < bandwidthDemand) {
								break;
							}else {
								linkAggEdge2b = link;
								count ++;
							}
						}
						if(count == 2) {
							checkAgg2 = true;
							break;
						}
						if(link.equals(listLinkBandwidth.getLast()) && count != 2) {
							checkAgg2 = false;
							break;
						}
					}
					if(checkAgg2) {
						listCoreSort1 = sortListSwitch(listCoreConnectAggMap.get(agg1));
						listCoreSort2 = sortListSwitch(listCoreConnectAggMap.get(agg2));
						
						if(!listCoreSort1.equals(listCoreSort2)) {
							if(agg2.equals(listAggSort2.getLast())) {
								return false;
							}
							else
								continue;
						}else { // condition core switch comparison
							for(SubstrateSwitch core : listCoreSort1) {
								count = 0;
								for (SubstrateLink link : listLinkBandwidth) {
									if(link.getStartSwitch() == agg1 && link.getEndSwitch() == core) {
										if(link.getBandwidth() < bandwidthDemand) {
											break;
										}else {
											count ++;
											linkCoreAgg1a = link;
										}
									}
									else if(link.getStartSwitch() == core && link.getEndSwitch() == agg1) {
										if(link.getBandwidth() < bandwidthDemand) {
											break;
										}else {
											count ++;
											linkCoreAgg1b = link;
										}
									}
									else if(link.getStartSwitch() == agg2 && link.getEndSwitch() == core) {
										if(link.getBandwidth() < bandwidthDemand) {
											break;
										}else {
											count ++;
											linkCoreAgg2a = link;
										}
									}
									else if(link.getStartSwitch() == core && link.getEndSwitch() == agg2) {
										if(link.getBandwidth() < bandwidthDemand) {
											break;
										}else {
											count ++;
											linkCoreAgg2b = link;
										}
									}
									if(count == 4) {
										linkEdge1.setBandwidth(linkEdge1.getBandwidth() - bandwidthDemand);
										linkEdge1.getEdgeSwitch().setPort(linkEdge1.getEdgeSwitch(), bandwidthDemand);
										linkEdge2.setBandwidth(linkEdge2.getBandwidth() - bandwidthDemand);
										linkEdge2.getEdgeSwitch().setPort(linkEdge2.getEdgeSwitch(), bandwidthDemand);
										vLink.getLinkPhyEdge().add(linkEdge1);
										vLink.getLinkPhyEdge().add(linkEdge2);
										linkAggEdge1a.setBandwidth(linkAggEdge1a.getBandwidth() - bandwidthDemand);
										linkAggEdge1a.getStartSwitch().setPort(linkAggEdge1a.getEndSwitch(), bandwidthDemand);
										linkAggEdge1b.setBandwidth(linkAggEdge1b.getBandwidth() - bandwidthDemand);
										linkAggEdge1b.getStartSwitch().setPort(linkAggEdge1b.getEndSwitch(), bandwidthDemand);
										linkAggEdge2a.setBandwidth(linkAggEdge2a.getBandwidth() - bandwidthDemand);
										linkAggEdge2a.getStartSwitch().setPort(linkAggEdge2a.getEndSwitch(), bandwidthDemand);
										linkAggEdge2b.setBandwidth(linkAggEdge2b.getBandwidth() - bandwidthDemand);
										linkAggEdge2b.getStartSwitch().setPort(linkAggEdge2b.getEndSwitch(), bandwidthDemand);
										linkCoreAgg1a.setBandwidth(linkCoreAgg1a.getBandwidth() - bandwidthDemand);
										linkCoreAgg1a.getStartSwitch().setPort(linkCoreAgg1a.getEndSwitch(), bandwidthDemand);
										linkCoreAgg1b.setBandwidth(linkCoreAgg1b.getBandwidth() - bandwidthDemand);
										linkCoreAgg1b.getStartSwitch().setPort(linkCoreAgg1b.getEndSwitch(), bandwidthDemand);
										linkCoreAgg2a.setBandwidth(linkCoreAgg2a.getBandwidth() - bandwidthDemand);
										linkCoreAgg2a.getStartSwitch().setPort(linkCoreAgg2a.getEndSwitch(), bandwidthDemand);
										linkCoreAgg2b.setBandwidth(linkCoreAgg2b.getBandwidth() - bandwidthDemand);
										linkCoreAgg2b.getStartSwitch().setPort(linkCoreAgg2b.getEndSwitch(), bandwidthDemand);
										vLink.getLinkSubstrate().add(linkAggEdge1a);
										vLink.getLinkSubstrate().add(linkAggEdge1b);
										vLink.getLinkSubstrate().add(linkAggEdge2a);
										vLink.getLinkSubstrate().add(linkAggEdge2b);
										vLink.getLinkSubstrate().add(linkCoreAgg1a);
										vLink.getLinkSubstrate().add(linkCoreAgg1b);
										vLink.getLinkSubstrate().add(linkCoreAgg2a);
										vLink.getLinkSubstrate().add(linkCoreAgg2b);
										return true;
									} // condition count core equals 4
								}
							}
						}
					}else { // condition checkAgg2
						if(agg2.equals(listAggSort2.getLast())) {
							return false;
						}
						else
							continue;
					}
				} // end for loop 2
				
			}else { // condition checkAgg1
				if(agg1.equals(listAggSort1.getLast())) {
					return false;
				}
				else
					continue;
			}
			
		} // end agg 1 for loop 
		return false;
	}

	
	public LinkedList<SubstrateLink> MapLink(LinkedList<SubstrateSwitch> path, LinkedList<SubstrateLink> listLinkBandwidthTemp, double bandwidth)
	{
		for (int i = 0; i < path.size() - 1; i++) {
		
			SubstrateSwitch switch1 = path.get(i);
			SubstrateSwitch switch2 = path.get(i + 1);
			for (int j = 0; j < listLinkBandwidthTemp.size(); j++) {
				SubstrateLink link = listLinkBandwidthTemp.get(j);
				// update bandwidth, two-direction
				if (link.getStartSwitch().equals(switch1) && link.getEndSwitch().equals(switch2)) {
					link.setBandwidth(link.getBandwidth() - bandwidth);
					listLinkBandwidthTemp.set(j, link);
					// break;
				}
				//Vm1-> Vm2 == Vm2-Vm1
				if (link.getStartSwitch().equals(switch2) && link.getEndSwitch().equals(switch1)) {
					link.setBandwidth(link.getBandwidth() - bandwidth);
					listLinkBandwidthTemp.set(j, link);
					// break;
				}
			}
			switch1.setPort(switch2, bandwidth);
			switch2.setPort(switch1, bandwidth);
		}
		return listLinkBandwidthTemp;
	}
	
	// sort List switch in increasing order by ID
	public LinkedList<SubstrateSwitch> sortListSwitch(LinkedList<SubstrateSwitch> list) {
		Collections.sort(list, new Comparator<SubstrateSwitch>() {
			@Override
			public int compare(SubstrateSwitch o1, SubstrateSwitch o2) {
				if (Integer.parseInt(o1.getNameSubstrateSwitch()) < Integer.parseInt(o2.getNameSubstrateSwitch())) {
					return -1;
				}
				if (Integer.parseInt(o1.getNameSubstrateSwitch()) > Integer.parseInt(o2.getNameSubstrateSwitch())) {
					return 1;
				}
				return 0;
			}
		});
		return list;
	}

	public double getBanwidthOfPath(LinkedList<SubstrateSwitch> path, LinkedList<SubstrateLink> listLinkBandwidth) {
		double bandwidth = Integer.MAX_VALUE;
		for (int i = 0; i < path.size() - 1; i++) {
			SubstrateSwitch switch1 = path.get(i);
			SubstrateSwitch switch2 = path.get(i + 1);
			for (int j = 0; j < listLinkBandwidth.size(); j++) {
				SubstrateLink link = listLinkBandwidth.get(j);
				if (link.getStartSwitch().equals(switch1) && link.getEndSwitch().equals(switch2)) {
					if (link.getBandwidth() < bandwidth)
						bandwidth = link.getBandwidth();
					break;
				}
			}
		}
		return bandwidth;
	}
	
	public boolean checkPhyEdge(PhysicalServer phy1, SubstrateSwitch edge1, PhysicalServer phy2,
			SubstrateSwitch edge2, double bandwidth, LinkedList<LinkPhyEdge> listPhyEdgeTemp) {
		boolean check = false;
		boolean Satisfied = false;
		int count=0;
		for (LinkPhyEdge link : listPhyEdgeTemp) {
			if ((link.getPhysicalServer().equals(phy1) && link.getEdgeSwitch().equals(edge1)) && link.getBandwidth() >= bandwidth) {
					Satisfied = true;
					count++;
			}
			if ((link.getPhysicalServer().equals(phy2) && link.getEdgeSwitch().equals(edge2)) && link.getBandwidth() >= bandwidth) {
					check = true;
					count++;
			}
			if(count == 2) {
				break;
			}
		}
		
		return (Satisfied&&check);
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
	
	public void reversePhyLinkMapping(Topology topo) {
		LinkedList<SubstrateSwitch> phySwitch = topo.getListPhySwitch();		
		for (LinkPhyEdge link : listBandwidthPhyEdge.keySet()) {
			link.setBandwidth(link.getBandwidth()+listBandwidthPhyEdge.get(link));
			link.getEdgeSwitch().setPort(getSwitchFromID(phySwitch, link.getPhysicalServer().getName()), -listBandwidthPhyEdge.get(link));
		}
	}
	
	public Topology reverseLinkMapping(Topology topo, Map<LinkedList<SubstrateSwitch>, Double> resultsLinkMapping) {

		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
		LinkedList<SubstrateSwitch> listSwitch = topo.getListSwitch();
		for (Entry<LinkedList<SubstrateSwitch>, Double> entry : resultsLinkMapping.entrySet()) {
			LinkedList<SubstrateSwitch> path = entry.getKey();
			double bandwidth = entry.getValue();
			if(path.size()<=1)
				continue;
			for (int i = 0; i < path.size() - 1; i++) {
				
				SubstrateSwitch switch1 = path.get(i);
				SubstrateSwitch switch2 = path.get(i + 1);
				for (int j = 0; j < listLinkBandwidth.size(); j++) {
					SubstrateLink link = listLinkBandwidth.get(j);
					double bw = link.getBandwidth();
					// update bandwidth, two-direction
					//Vm1-> Vm2
					if (link.getStartSwitch().equals(switch1) && link.getEndSwitch().equals(switch2)) {
						
						link.setBandwidth(bw + bandwidth);
						listLinkBandwidth.set(j, link);
						// break;
					}
					//Vm2-> Vm1
					if (link.getStartSwitch().equals(switch2) && link.getEndSwitch().equals(switch1)) {
						link.setBandwidth(bw + bandwidth);
						listLinkBandwidth.set(j, link);
						// break;
					}
				}
				switch1.setPort(switch2, -bandwidth);
				switch2.setPort(switch1, -bandwidth);
			}
		}
		topo.setLinkBandwidth(listLinkBandwidth);
		topo.setListSwitch(listSwitch);
		return topo;
	}
	
	public double getPower(Topology topo)
	{
		double power = 0;
		modelHP HP = new modelHP();
		LinkedList<SubstrateSwitch> listSwitch = topo.getListSwitch();
//		for(SubstrateLink link: topo.getLinkBandwidth())
//		{
//			double bw = link.getBandwidth();
//			SubstrateSwitch s = link.getStartSwitch();
//			if(listSwitch.containsKey(s.getNameSubstrateSwitch()))
//			{
//				SubstrateSwitch sw = listSwitch.get(s.getNameSubstrateSwitch());
//				sw.setPort(link.getEndSwitch(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), sw);
//			}
//			else				
//			{
//				s.setPort(link.getEndSwitch(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), s);
//			}
//			
//		}
		for(SubstrateSwitch entry: listSwitch)
		{
			power+= HP.getPowerOfSwitch(entry);
		}
			
		return power;
	}
	
	public double getPower(LinkedList<SubstrateSwitch> listSwitch)
	{
		double power = 0;
		modelHP HP = new modelHP();
		for(SubstrateSwitch entry : listSwitch)
		{
			power+= HP.getPowerOfSwitch(entry);
		}
			
		return power;
	}

	public int getNumLinkSuccess() {
		return numLinkSuccess;
	}

	public void setNumLinkSuccess(int numLinkSuccess) {
		this.numLinkSuccess = numLinkSuccess;
	}

	public double getPowerConsumed() {
		return powerConsumed;
	}

	public void setPowerConsumed(double powerConsumed) {
		this.powerConsumed = powerConsumed;
	}

	public Map<LinkPhyEdge, Double> getListBandwidthPhyEdge() {
		return listBandwidthPhyEdge;
	}

	public void setListBandwidthPhyEdge(Map<LinkPhyEdge, Double> listBandwidthPhyEdge) {
		this.listBandwidthPhyEdge = listBandwidthPhyEdge;
	}

	public LinkedList<SubstrateLink> getListLinkCore() {
		return listLinkCore;
	}

	public void setListLinkCore(LinkedList<SubstrateLink> listLinkCore) {
		this.listLinkCore = listLinkCore;
	}

	public LinkedList<SubstrateLink> getListLinkAgg() {
		return listLinkAgg;
	}
	
	public void setListLinkAgg(LinkedList<SubstrateLink> listLinkAgg) {
		this.listLinkAgg = listLinkAgg;
	}
}
