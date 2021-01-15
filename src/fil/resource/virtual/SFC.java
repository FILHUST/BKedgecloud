package fil.resource.virtual;

import java.util.ArrayList;
import java.util.LinkedList;

import fil.resource.substrate.PhysicalServer;

public class SFC {
	private ArrayList<Boolean> servicePosition;
	private int piBelong;
	private int sfcID;
	private double totalChainCpu;
	private double totalChainBandwidth;
	private double startTime;
	private double endTime;
	private Service capture;
	private Service decode ;
	private Service density;
	private Service receive;
	private boolean separateService;
	private LinkedList<VirtualLink> vLink;
	
	public SFC(int sfcID, int piBelong, double  endTime) {
		servicePosition = new ArrayList<Boolean>();
		capture = new Capture(sfcID, piBelong);
		decode = new Decode(sfcID, piBelong);
		density = new Density(sfcID, piBelong);
		receive = new Receive(sfcID, piBelong);
		totalChainBandwidth = 0;
		this.setSeparateService(false);
		this.setPiBelong(piBelong);
		this.setSfcID(sfcID);
//		this.setStartTime(startTime);
		this.setEndTime(endTime);
		this.setvLink(new LinkedList<>());
	}
	
	public SFC(int sfcID, int piBelong) {
		servicePosition = new ArrayList<Boolean>();
		capture = new Capture(sfcID, piBelong);
		decode = new Decode(sfcID, piBelong);
		density = new Density(sfcID, piBelong);
		receive = new Receive(sfcID, piBelong);
		totalChainBandwidth = 0;
		this.setSeparateService(false);
		this.setPiBelong(piBelong);
		this.setSfcID(sfcID);
		this.setvLink(new LinkedList<>());
	}
	

	public SFC(SFC sfc) {
		servicePosition = new ArrayList<Boolean>();
		capture = new Service(sfc.getCapture());
		decode = new Service(sfc.getDecode());;
		density = new Service(sfc.getDensity());;
		receive = new Service(sfc.getReceive());;
		totalChainBandwidth = 0;
		this.setPiBelong(sfc.getPiBelong());
		this.setSfcID(sfc.getSfcID());
		this.setEndTime(sfc.getEndTime());
		this.setvLink(new LinkedList<>()); // change if a real replication is required 
	}
	public SFC() {
		// TODO Auto-generated constructor stub
		servicePosition = new ArrayList<Boolean>();
		capture = new Capture(sfcID, piBelong);
		decode = new Decode(sfcID, piBelong);
		density = new Density(sfcID, piBelong);
		receive = new Receive(sfcID, piBelong);
		totalChainBandwidth = 0;
		this.setSeparateService(false);
		this.setvLink(new LinkedList<>());
	}

	public ArrayList<Boolean> getServicePosition() {
		return servicePosition;
	}
	
	public void setServicePosition (String type, boolean position) {
		switch(type) {
		case "capture": this.capture.setBelongToEdge(position);servicePosition.add(position);break;
		case "decode": this.decode.setBelongToEdge(position);servicePosition.add(position);break;
		case "density": this.density.setBelongToEdge(position);servicePosition.add(position);break;
		case "receive": this.receive.setBelongToEdge(position);servicePosition.add(position);break;
		default: System.out.println("error at set position"); break;
		}
	}
	
	public boolean getServicePosition (Service service) {
		String type = service.getServiceType();
		switch(type) {
		case "decode": return this.decode.getBelongToEdge();
		case "density": return this.density.getBelongToEdge();
		default: System.out.println("error at set position"); return false;
		}
	}
	
	public void copy(SFC copySFC) {
		servicePosition = new ArrayList<Boolean>();
		this.capture.setBelongToEdge(copySFC.getCapture().isBelongToEdge());
		this.decode.setBelongToEdge(copySFC.getDecode().isBelongToEdge());
		this.density.setBelongToEdge(copySFC.getDensity().isBelongToEdge());
		this.receive.setBelongToEdge(copySFC.getReceive().isBelongToEdge());

//		this.setCapture(copySFC.getCapture());
//		this.setDecode(copySFC.getDecode());
//		this.setDensity(copySFC.getDensity());
//		this.setReceive(copySFC.getReceive());
	}
//	public void setServicePosition(Capture capture, boolean position) {
//		this.capture = capture;
//		this.capture.setBelongToEdge(position);
//	}
//	public void setServicePosition(Decode decode, boolean position) {
//		this.decode = decode;
//		this.decode.setBelongToEdge(position);
//	}
//	public void setServicePosition(Density density, boolean position) {
//		this.density = density;
//		this.density.setBelongToEdge(position);
//	}
//	public void setServicePosition(ReceiveDensity receive, boolean position) {
//		this.receive = receive;
//		this.receive.setBelongToEdge(position);
//	}
	public void reset() {
		this.separateService = false;
		this.setvLink(new LinkedList<>());
		for(int i = 1; i <= 4; i++) {
			this.getService(i).setBelongToServer(null);
		}
//		this.capture.setBelongToEdge(true);
//		this.decode.setBelongToEdge(true);
//		this.density.setBelongToEdge(true);
//		this.receive.setBelongToEdge(false);

	}
	
	public void resetEvent() {
		this.separateService = false;
		this.setvLink(new LinkedList<>());
		for(int i = 1; i <= 4; i++) {
			this.getService(i).setBelongToServer(null);
		}
		this.capture.setBelongToEdge(true);
		this.decode.setBelongToEdge(true);
		this.density.setBelongToEdge(true);
		this.receive.setBelongToEdge(false);

	}
	
	public double getCpuDD(int dec, int den) {

		totalChainCpu = dec*decode.getCpu_pi() + den*density.getCpu_pi()
		+ capture.getCpu_pi() + receive.getCpu_pi();
		
		return totalChainCpu;
	}
	
	public double getBandwidthDD(int dec, int den) {
		
		if(dec != den)
			totalChainBandwidth = dec*this.decode.getBandwidth();
		else if(dec*den == 1)
			totalChainBandwidth = this.density.getBandwidth();
		else
			totalChainBandwidth = this.capture.getBandwidth();
		
		return totalChainBandwidth;
	}
	
	public double getBandwidth() {
		for (int i = (servicePosition.size() - 1); i >=0 ; i--) {
			if (servicePosition.get(i) == false) {
				switch(i) {
				case 0: System.out.println("impposible");break;
				case 1: this.totalChainBandwidth = capture.getBandwidth();break;
				case 2: this.totalChainBandwidth = decode.getBandwidth();break;
				case 3: this.totalChainBandwidth = density.getBandwidth();break;
				}
			}
		}
		return this.totalChainBandwidth;
	}
	
	public double getBandwidthSFC() {
		double bandwidth = 0;
		for(int i = 1; i <= 4; i++) {
			if(!this.getService(i).isBelongToEdge()) {
				bandwidth = this.getService(i).getBandwidth();
				break;
			}
		}
		return bandwidth;
	}
	public Service getFirstServiceCloud() {
		Service service = null;
		for(int i = 1; i <= 4; i++) {
			if(!this.getService(i).isBelongToEdge()) {
				service = this.getService(i);
				break;
			}
		}
		return service;
	}
	
	public Service getLastServiceEdge() {
		Service service = null;
		for(int i = 1; i <= 4; i++) {
			if(!this.getService(i).isBelongToEdge()) {
				service = this.getService(i-1);
				break;
			}
		}
		return service;
	}
	
	public Service getService(int number) {
		if(number == 1) return capture;
		if(number == 2) return decode;
		if(number == 3) return density;
		if(number == 4) return receive;
		else return null;
	}


	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}


	public int getPiBelong() {
		return piBelong;
	}

	public void setPiBelong(int piBelong) {
		this.piBelong = piBelong;
	}

	public int getSfcID() {
		return sfcID;
	}

	public void setSfcID(int sfcID) {
		this.sfcID = sfcID;
	}
	
	public int numServiceInServer() {
		int num = 0;
		for(int i = 0; i < servicePosition.size(); i++) {
			if(servicePosition.get(i) == false) {
				num++;
			}
		}
		return num;
	}
	public boolean allServiceInSameServer() {
		PhysicalServer[] server = new PhysicalServer[3];
		boolean result = false;
		for(int i = 2; i <= 4; i++) {
			server[i-2] = this.getService(i).getBelongToServer();	
		}
		if(server[0] == null) {
			if(server[1] == null) {
				result = true;
			}
			else {
				if(!server[1].equals(server[2])) {
					result = false;
				}
				else
					result = true;
			}
		}
		else {
			if(!server[0].equals(server[1])) {
				result = false;
			}
			else if(!server[1].equals(server[2])) {
				result = false;
			}
			else {
				result = true;
			}
		}
		return result;
	}
	
	public void resetSFC() {
		this.separateService = false;
		this.setvLink(new LinkedList<>());
		for(int i = 1; i <= 4; i++) {
			if(!this.getService(i).isBelongToEdge()) {
				this.getService(i).setBelongToServer(null);
			}
		}
	}
	public boolean isSeparateService() {
		return separateService;
	}
	public void setSeparateService(boolean separateService) {
		this.separateService = separateService;
	}
	public LinkedList<VirtualLink> getvLink() {
		return vLink;
	}
	public void setvLink(LinkedList<VirtualLink> vLink) {
		this.vLink = vLink;
	}
	
	public boolean existServiceInServer(PhysicalServer phyB) {
		boolean result = false;
		for(int i = 1; i <= 4; i++) {
			if(!this.getService(i).isBelongToEdge()) {
				if(this.getService(i).getBelongToServer().equals(phyB))
					result = true;
			}
		}
		return result;
	}
	public Double powerEdgeUsage() {
		Double power = 0.0;
		for(int i = 1; i < 4; i++) {
			if(this.getService(i).isBelongToEdge()) {
				power += this.getService(i).getPower();
			}
		}
		return power;
	}
	public Double cpuEdgeUsage() {
		Double cpu = 0.0;
		for(int i = 1; i < 4; i++) {
			if(this.getService(i).isBelongToEdge()) {
				cpu += this.getService(i).getCpu_pi();
			}
		}
		return cpu;
	}
	public Double cpuServerUsage() {
		Double cpu = 0.0;
		for(int i = 2; i <= 4; i++) {
			if(!this.getService(i).isBelongToEdge()) {
				cpu += this.getService(i).getCpu_server();
			}
		}
		return cpu;
	}
	
	public Double bandwidthUsageOutDC() {
		Double bwOutDC = 0.0;
		for(int i = 1; i <= 4; i++) {
			if(!this.getService(i).isBelongToEdge()) {
				bwOutDC = this.getService(i-1).getBandwidth();
				break;
			}
		}
		if(bwOutDC == 0)
			throw new java.lang.Error("BW outside DC is wrong.");
		return bwOutDC;
	}
	
	public Double bandwidthUsageInDC() {
		Service service = this.getFirstServiceCloud();
		Double bwInDC = 0.0;
		if(service.getServiceType() == "decode") {
			if(!this.getService(2).getBelongToServer().equals(this.getService(3).getBelongToServer()))
				bwInDC += this.getService(2).getBandwidth();
			if(!this.getService(3).getBelongToServer().equals(this.getService(4).getBelongToServer()))
				bwInDC += this.getService(3).getBandwidth();
		}
		else if(service.getServiceType() == "density") {
			if(!this.getService(3).getBelongToServer().equals(this.getService(4).getBelongToServer()))
				bwInDC += this.getService(3).getBandwidth();
		}
		else {
			bwInDC = 0.0;
		}
		return bwInDC;
	}
	public double getStartTime() {
		return startTime;
	}
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}
	public Service getCapture() {
		return capture;
	}
	public void setCapture(Capture capture) {
		this.capture = capture;
	}
	public Service getDecode() {
		return decode;
	}
	public void setDecode(Decode decode) {
		this.decode = decode;
	}
	public Service getDensity() {
		return density;
	}
	public void setDensity(Density density) {
		this.density = density;
	}
	public Service getReceive() {
		return receive;
	}
	public void setReceive(Service service) {
		this.receive = (Receive) service;
	}
	public LinkedList<Service> getListService() {
		LinkedList<Service> listService = new LinkedList<>();
		listService.add(this.getCapture());
		listService.add(this.getDecode());
		listService.add(this.getDensity());
		listService.add(this.getReceive());
		return listService;
	}
	
	
	public LinkedList<Service> getListServiceCloud() {
		LinkedList<Service> listService = new LinkedList<>();
		for(Service service : this.getListService()) {
			if(!service.isBelongToEdge())
				listService.add(service);
		}
		return listService;
	}
	
	public void setService(Service service) {
		switch(service.getServiceType()) {
		case "decode":
			this.setDecode((Decode) service);
			break;
		case "density":
			this.setDensity((Density) service);
			break;
		case "receive":
			this.setReceive((Receive) service);
			break;
		}
	}
}
