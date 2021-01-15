package fil.algorithm.routing;

//import java.util.ArrayList;
import java.util.HashMap;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set; 
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
//import fil.routing.*;

public class NetworkTopology implements java.io.Serializable{
	private int numSwitch;
	private LinkedList<NetworkSwitch> listSwitch;
	private Map<String, NetworkLink> listLink;
	private Map<NetworkSwitch, LinkedList<NetworkSwitch>> neighborOfNode;
	
	//private NetworkTopo topo;
//	private int bwMax;
	public NetworkTopology() {
		numSwitch = 15;
		listSwitch = new LinkedList<NetworkSwitch>();
		listLink = new HashMap<>(); 
		neighborOfNode = new HashMap<>();
	}

	public void initTopo() {
		listSwitch.add(0,null);
		for (int i = 1; i <= numSwitch; i++) { //init each switch
			listSwitch.add(new NetworkSwitch(String.valueOf(i))); // add new switch with name
//			switch(i) {
//			case 0: listSwitch.get(i).
//			}
		}
//		System.out.println("Size: " + listSwitch.size() + "\n");
	//	System.out.println("Size: " + listSwitch + "\n");
		// set neighbor and bandwidth for each switch
		for (int i = 1; i <= numSwitch; i++) { //init each switch
			LinkedList<NetworkSwitch> neighborTemp = new LinkedList<NetworkSwitch>(); // array temp for storing neighbors
			FindNeighbor(listSwitch.get(i), neighborTemp);
			//if (neighborTemp == null) System.out.println("dit me may");
			//System.out.println("Neighbor: " + neighborTemp + " \n");
			neighborOfNode.put(listSwitch.get(i), neighborTemp); // officially assign neighbors array to switch
		}
		// initialize linkbandwidth
		for (int i = 1; i <= numSwitch; i++) {
			NetworkSwitch node = listSwitch.get(i);
			LinkedList<NetworkSwitch> nodeNeighbor = neighborOfNode.get(node);
			for (NetworkSwitch node_temp : nodeNeighbor) {
				String name = node.getNameNetworkSwitch() + node_temp.getNameNetworkSwitch();
				NetworkLink linkNode = new NetworkLink(node, node_temp);
				listLink.put(name, linkNode);		
			}
		}
	}
	
	
	public LinkedList<NetworkSwitch> adjacentNodes(NetworkSwitch node) {
		LinkedList<NetworkSwitch> adjacent = neighborOfNode.get(node);
		if (adjacent == null) {
			return new LinkedList<NetworkSwitch>();
		}
		return new LinkedList<NetworkSwitch>(adjacent);
	}
	 
	
	public void FindNeighbor (NetworkSwitch node, LinkedList<NetworkSwitch> neighbor){
		//LinkedList<NetworkSwitch> neighbor = new LinkedList<NetworkSwitch>();
//		System.out.println("Name switch: " + node.getNameNetworkSwitch());
		switch (node.getNameNetworkSwitch()) {
		//case "0": neighbor.add(listSwitch.get(5));neighbor.add(listSwitch.get(6));neighbor.add(listSwitch.get(7));
		case "1": neighbor.add(listSwitch.get(6));neighbor.add(listSwitch.get(7));neighbor.add(listSwitch.get(8));break;
		case "2": neighbor.add(listSwitch.get(3));neighbor.add(listSwitch.get(5));neighbor.add(listSwitch.get(6));break;
		case "3": neighbor.add(listSwitch.get(2));neighbor.add(listSwitch.get(5));neighbor.add(listSwitch.get(8));break;
		case "4": neighbor.add(listSwitch.get(5));neighbor.add(listSwitch.get(6));break;
		case "5": neighbor.add(listSwitch.get(2));neighbor.add(listSwitch.get(3));neighbor.add(listSwitch.get(4));break;
		case "6": neighbor.add(listSwitch.get(13));neighbor.add(listSwitch.get(1));neighbor.add(listSwitch.get(2));neighbor.add(listSwitch.get(4));break;
		case "7": neighbor.add(listSwitch.get(1));neighbor.add(listSwitch.get(10));neighbor.add(listSwitch.get(14));break;
		case "8": neighbor.add(listSwitch.get(1));neighbor.add(listSwitch.get(3));neighbor.add(listSwitch.get(9));
		neighbor.add(listSwitch.get(15)); break;
		case "9": neighbor.add(listSwitch.get(8));neighbor.add(listSwitch.get(10));neighbor.add(listSwitch.get(12));
		neighbor.add(listSwitch.get(15)); break;
		case "10": neighbor.add(listSwitch.get(7));neighbor.add(listSwitch.get(9));neighbor.add(listSwitch.get(12));break;
		case "11": neighbor.add(listSwitch.get(13));neighbor.add(listSwitch.get(14)); break;
		case "12": neighbor.add(listSwitch.get(9));neighbor.add(listSwitch.get(10)); break;
		case "13": neighbor.add(listSwitch.get(6));neighbor.add(listSwitch.get(11));neighbor.add(listSwitch.get(14));break;
		case "14": neighbor.add(listSwitch.get(7));neighbor.add(listSwitch.get(11));neighbor.add(listSwitch.get(13));break;
		case "15": neighbor.add(listSwitch.get(8));neighbor.add(listSwitch.get(9));break;
		default: System.out.println("error while inserting neighbor!! \n "); break;
		}
	}
	
	public void CheckTopo () {
		Scanner checkNode = new Scanner(System.in);
//		Set<Entry<NetworkSwitch, LinkedList<NetworkSwitch>>> s = neighborOfNode.entrySet(); 
		System.out.println("Insert node you want to check its neighbors: ");
		int node = checkNode.nextInt();
		//getchar();
		//System.out.println("Its neighbors are: ");
		System.out.println("Its neighbors are: " + neighborOfNode.get(listSwitch.get(node)) + "\n"); 
		checkNode.close();
	}
	
//	public static void main(String[] args) {
//		NetworkTopology genTopo = new NetworkTopology();
//		genTopo.initTopo();
//		while(true) {
//			genTopo.CheckTopo();
//		}
//	}

	public Map<String, NetworkLink> getListLink() {
		return listLink;
	}

	public void setListLink(HashMap<String, NetworkLink> listLink) {
		this.listLink = listLink;
	}

	public LinkedList<NetworkSwitch> getListSwitch() {
		return listSwitch;
	}

	public void setListSwitch(LinkedList<NetworkSwitch> listSwitch) {
		this.listSwitch = listSwitch;
	}

	public Map<NetworkSwitch, LinkedList<NetworkSwitch>> getNeighborOfNode() {
		return neighborOfNode;
	}

	public void setNeighborOfNode(HashMap<NetworkSwitch, LinkedList<NetworkSwitch>> neighborOfNode) {
		this.neighborOfNode = neighborOfNode;
	}
	
	
}
