package fil.resource.virtual;

import java.util.LinkedList;
import fil.resource.substrate.*;
/**
 * Builds virtual link
 * 
 * @author Van Huynh Nguyen
 *
 */
public class VirtualLink {
	private Service sService;
	private Service dService;
	private double bandwidthRequest;
	private LinkedList<LinkPhyEdge> linkPhyEdge;
	private LinkedList<SubstrateLink> linkSubstrate;
	private LinkedList<SubstrateSwitch> listSwitch;
	/**
	 * Constructs virtual link
	 */
	
	public VirtualLink() {
		super();
		// TODO Auto-generated constructor stub
		this.setLinkPhyEdge(new LinkedList<>());
		this.setLinkSubstrate(new LinkedList<>());
	}
	
	public VirtualLink(Service sService, Service dService, double bandwidthRequest) {
		super();
		this.sService = sService;
		this.dService = dService;
		this.bandwidthRequest = bandwidthRequest;
		this.setLinkPhyEdge(new LinkedList<>());
		this.setLinkSubstrate(new LinkedList<>());
	}
	
	
	public Service getsService() {
		return sService;
	}
	public void setsService(Service sService) {
		this.sService = sService;
	}
	public Service getdService() {
		return dService;
	}
	public void setdService(Service dService) {
		this.dService = dService;
	}
	public double getBandwidthRequest() {
		return bandwidthRequest;
	}
	public void setBandwidthRequest(double bandwidthRequest) {
		this.bandwidthRequest = bandwidthRequest;
	}

	public LinkedList<LinkPhyEdge> getLinkPhyEdge() {
		return linkPhyEdge;
	}

	public void setLinkPhyEdge(LinkedList<LinkPhyEdge> linkPhyEdge) {
		this.linkPhyEdge = linkPhyEdge;
	}

	public LinkedList<SubstrateLink> getLinkSubstrate() {
		return linkSubstrate;
	}

	public void setLinkSubstrate(LinkedList<SubstrateLink> linkSubstrate) {
		this.linkSubstrate = linkSubstrate;
	}

	public LinkedList<SubstrateSwitch> getListSwitch() {
		return listSwitch;
	}

	public void setListSwitch(LinkedList<SubstrateSwitch> listSwitch) {
		this.listSwitch = listSwitch;
	}
}
