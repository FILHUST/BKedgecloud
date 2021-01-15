package fil.algorithm.routing;

import java.util.HashMap;
import java.util.Map;
//import java.util.Map.Entry;

/**
 * Builds substrate switch
 * 
 * @author Van Huynh Nguyen
 *
 */
@SuppressWarnings("serial")
public class NetworkSwitch implements java.io.Serializable {
	private String nameNetworkSwitch;
	private double cpu;
	//private boolean isConnectServer;
	//private PhysicalServer physicalServer;
	//private int type; // 0: off, 1: edge , 2: agg, 3:core
	private Map<NetworkSwitch, Double> bandwidthPort; // ex: s1-s2:100, s1-s3:
														// 200 =>
														// bandwidthPort=[{s2,
														// 100}, {s3, 200}]

	/**
	 * Constructs substrate switch
	 */
	public NetworkSwitch() {
		this.nameNetworkSwitch = "";
		this.cpu = 0;
//		this.isConnectServer = false;
//		this.type = 0;
		this.bandwidthPort = new HashMap<>();
	}

	/**
	 * Constructs substrate switch
	 * 
	 * @param name
	 *            Name of substrate switch
	 * @param cpu
	 *            CPU capacity of substrate switch
	 */
	public NetworkSwitch(String name) {
		this.nameNetworkSwitch = name;
		//this.cpu = cpu;
		//this.isConnectServer = isConnect;
		//this.bandwidthPort = new HashMap<>();
	}

	public String getNameNetworkSwitch() {
		return nameNetworkSwitch;
	}

	public void setNameNetworkSwitch(String nameNetworkSwitch) {
		this.nameNetworkSwitch = nameNetworkSwitch;
	}

	public double getCpu() {
		return cpu;
	}

	public void setCpu(double cpu) {
		this.cpu = cpu;
	}

//	public PhysicalServer getPhysicalServer() {
//		return physicalServer;
//	}
//
//	public void setPhysicalServer(PhysicalServer physicalServer) {
//		this.physicalServer = physicalServer;
//		this.isConnectServer = true;
//	}
//
//	public int getType() {
//		return type;
//	}
//
//	public void setType(int type) {
//		this.type = type;
//	}

//	public Map<NetworkSwitch, Double> getBandwidthPort() {
//		return bandwidthPort;
//	}
//
////	public void setBandwidthPort(Map<NetworkSwitch, Double> bandwidthPort) {
////		this.bandwidthPort = bandwidthPort;
//	}

	public void setPort(NetworkSwitch sw, Double bandwidth) {
		this.bandwidthPort.put(sw, this.bandwidthPort.get(sw)+ bandwidth);
	}
	

}
