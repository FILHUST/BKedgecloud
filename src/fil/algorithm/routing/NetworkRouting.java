package fil.algorithm.routing;



import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import fil.resource.substrate.Rpi;
import fil.resource.virtual.SFC;

@SuppressWarnings("serial")
public class NetworkRouting implements java.io.Serializable{
	private NetworkTopology topo;
	private LinkedList<LinkedList<NetworkSwitch>> listPath;
	//private  Map<SFC,LinkedList<NetworkSwitch>> pathOfRequest;
	private Map<Rpi, Map<SFC,LinkedList<NetworkSwitch>>> pathOfRequestOfPi;
	public LinkedList<Double> listBW;
	//private LinkedList<NetworkSwitch> listSwitch;
	public static int SERVER_POSITION = 1;
	public static int count = 0;
	public NetworkRouting() {
		this.topo = new NetworkTopology();
		this.topo.initTopo();
		this.listPath = new LinkedList<>();
		//this.pathOfRequest = new HashMap<>();
		this.pathOfRequestOfPi = new HashMap<>();
		this.listBW = new LinkedList<>();
	}
	
	public boolean run(LinkedList<SFC> sfc, Rpi rpi) {
		//this.pathOfRequest.clear();
		Map<SFC,LinkedList<NetworkSwitch>> pathOfRequest = new HashMap<>();
		LinkedList<LinkedList<NetworkSwitch>> listPath = new LinkedList<LinkedList<NetworkSwitch>>();
		LinkedList<NetworkSwitch> pathChosen = new  LinkedList<NetworkSwitch>();
		NetworkSwitch startNode = topo.getListSwitch().get(rpi.getCluster());
		NetworkSwitch endNode = topo.getListSwitch().get(SERVER_POSITION);
		
		int size = sfc.size();
		for (int i =0; i < size; i++) {
			double bwDemand = sfc.get(i).getBandwidth();
			listBW.add(bwDemand);
			
			BFS testBFS = new BFS(startNode, endNode, bwDemand);
			pathChosen = testBFS.run(topo, pathChosen);
			
			if(pathChosen.isEmpty()) {
				return false;
			}
			listPath.add(pathChosen);
			//Done path finding .................................//
			System.out.println("SFC number " + i + " travels through " + pathChosen.size() + " switches \n");
			for (int j = 0; j < pathChosen.size(); j++) {
				System.out.println("Switch get through is: " + pathChosen.get(j).getNameNetworkSwitch() + " \n");
			}

			System.out.println("Setting used bandwidth for network topology... \n");
        	for ( int j = 0; j < (pathChosen.size() - 1); j++) {
        		String a = pathChosen.get(j).getNameNetworkSwitch();
            	String b = pathChosen.get(j+1).getNameNetworkSwitch();
            	this.topo.getListLink().get(a+b).setUsedBandwidth(bwDemand);
            	this.topo.getListLink().get(b+a).setUsedBandwidth(bwDemand);
        	}
        	pathOfRequest.put(sfc.get(i), listPath.get(i));
		}
    	this.pathOfRequestOfPi.put(rpi, pathOfRequest);
//		System.out.println(listBW);
		return true;
	}
	
	public void  reset(Rpi piIndex) {
		count ++;
		Map<SFC,LinkedList<NetworkSwitch>> listSFCPi = this.pathOfRequestOfPi.get(piIndex);
		if(listSFCPi == null) {
			return;
		}
		for ( SFC sfc : listSFCPi.keySet()) {
			LinkedList<NetworkSwitch> path = listSFCPi.get(sfc);
			double bwDemand = (sfc.getBandwidth()*(-1.0));
			for (int i = 0; i < (path.size() - 1); i++) {
				String a = path.get(i).getNameNetworkSwitch();
            	String b = path.get(i+1).getNameNetworkSwitch();
            	this.topo.getListLink().get(a+b).setUsedBandwidth(bwDemand);
            	this.topo.getListLink().get(b+a).setUsedBandwidth(bwDemand);
			}
		}
		this.pathOfRequestOfPi.remove(piIndex); //delete all chain of this pi
	}
	
	
	public void  resetSFC(Rpi piIndex, LinkedList<SFC> listSFC) {
		count ++;
		Map<SFC,LinkedList<NetworkSwitch>> listSFCPi = this.pathOfRequestOfPi.get(piIndex);
		
		if(listSFCPi == null) {
			return;
		}
		for ( SFC sfc : listSFCPi.keySet()) {
			if(listSFC.contains(sfc)) {
				LinkedList<NetworkSwitch> path = listSFCPi.get(sfc);
				double bwDemand = (sfc.getBandwidth()*(-1.0));
				for (int i = 0; i < (path.size() - 1); i++) {
					String a = path.get(i).getNameNetworkSwitch();
	            	String b = path.get(i+1).getNameNetworkSwitch();
	            	this.topo.getListLink().get(a+b).setUsedBandwidth(bwDemand);
	            	this.topo.getListLink().get(b+a).setUsedBandwidth(bwDemand);
				}
			}
			
		}
		for(SFC sfc : listSFC) {
			if(listSFCPi.keySet().contains(sfc))
				listSFCPi.remove(sfc);
		}
//		this.pathOfRequestOfPi.remove(piIndex); //delete all chain of this pi
	}

	
	public void  NetworkReset(Rpi piIndex, LinkedList <SFC> listSFCLeave) {
		Map<SFC,LinkedList<NetworkSwitch>> listSFCPi = new HashMap<>();
		listSFCPi = this.pathOfRequestOfPi.get(piIndex);
		Iterator<Map.Entry<SFC,LinkedList<NetworkSwitch>>> iterator = listSFCPi.entrySet().iterator();
		
		while(iterator.hasNext()) {
			HashMap.Entry<SFC,LinkedList<NetworkSwitch>> entry = iterator.next();
			SFC sfc = entry.getKey();
			if (sfc == listSFCLeave.getFirst()) {
				listSFCLeave.removeFirst();
				LinkedList<NetworkSwitch> path = listSFCPi.get(sfc);
				double bwDemand = (sfc.getBandwidth()*(-1.0));
				for (int i = 0; i < (path.size() - 1); i++) {
					String a = path.get(i).getNameNetworkSwitch();
	            	String b = path.get(i+1).getNameNetworkSwitch();
	            	this.topo.getListLink().get(a+b).setUsedBandwidth(bwDemand);
	            	this.topo.getListLink().get(b+a).setUsedBandwidth(bwDemand);
				}
			}
		}
	}

	public NetworkTopology getTopo() {
		return topo;
	}

	public void setTopo(NetworkTopology topo) {
		this.topo = topo;
	}

	public LinkedList<LinkedList<NetworkSwitch>> getListPath() {
		return listPath;
	}

	public void setListPath(LinkedList<LinkedList<NetworkSwitch>> listPath) {
		this.listPath = listPath;
	}


	public Map<Rpi, Map<SFC, LinkedList<NetworkSwitch>>> getPathOfRequestOfPi() {
		return pathOfRequestOfPi;
	}

	public void setPathOfRequestOfPi(Map<Rpi, Map<SFC, LinkedList<NetworkSwitch>>> pathOfRequestOfPi) {
		this.pathOfRequestOfPi = pathOfRequestOfPi;
	}
	
}
