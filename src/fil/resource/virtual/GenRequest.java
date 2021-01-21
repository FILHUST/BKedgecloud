package fil.resource.virtual;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.math3.distribution.PoissonDistribution;

import fil.resource.substrate.Rpi;


public class GenRequest {
	final static double LIVETIME = 2.0;
	final static double TIMEWINDOW = 1.0;
	final static int COF_FULL = 3; 
	final static int COF_LOAD = 2100; 
	final static int PINUMBER = 300;
	private LinkedList<LinkedList<Event>> listEvent;
	private LinkedList<Event> listEventTW;
	private LinkedList<HashMap<Rpi, Double>> listEventCR;
//	private PoissonDistribution poisson;
	private Double [] LUTFull = {32.0,22.0,20.0,23.0,37.0,89.0,211.0,335.0,337.0,282.0,263.0,269.0,
			276.0,287.0,313.0,350.0,375.0,304.0,269.0,151.0,110.0,80.0,30.0,20.0}; //full 24h
	private Double [] LUTLoad = {0.025,0.05,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0}; // not-full

	public GenRequest() {
//		this.listTotalRequest = new LinkedList<>();
//		this.poisson = new PoissonDistribution();
		this.listEvent = new LinkedList<>();
		this.setListEventCR(new LinkedList<>());
		this.setListEventTW(new LinkedList<>());
	}
	
	public void generator(int opt) {
		
		Double [] LUT = {};
		int COF = 0;
		
		if(opt == 0) { // load increase
			LUT = LUTLoad;
			COF = COF_LOAD;
		}else if(opt == 1) {
			LUT = LUTFull;
			COF = COF_FULL;
		}else
			throw new java.lang.Error();
		int numOfWD = LUT.length;
		int sfcID = 0;
		
		for(int WD = 0; WD < numOfWD; WD ++) {
			
			PoissonDistribution poisson = new PoissonDistribution(LUT[WD]*COF);
			int lambda = poisson.sample(); // get random 
//			int lambda = (int) poisson.getMean(); // get mean 

			double timeArrival = WD;
			System.out.println("lambda: " + lambda);


//			decide number of camera arrives each pi
			HashMap<Integer, Integer> request_temp = new HashMap<>(); // generate number of request for each Pi
			for(int pi = 0; pi < PINUMBER; pi++) {
				request_temp.put(pi, 0);
			}
			
			Random rand = new Random();
			int numChain = lambda;
			while(numChain > 0) {
				int device = rand.nextInt(PINUMBER);
				int camera = rand.nextInt(2); // # of camera need to be opened
				int totalCamera = request_temp.get(device) + camera;
				request_temp.put(device, totalCamera);
				numChain -= camera;
			}
//			end number of camera decision
			
//			set arrival time for each pi
			int piRequest = 0;
			for(Entry<Integer, Integer> entry : request_temp.entrySet()){
				if(entry.getValue() > 0)
					piRequest ++;
				else if(entry.getValue() < 0)
					throw new java.lang.Error();
				else
					;
			}
			
			
//				generate listSFC for each pi 
			
			for(Entry<Integer, Integer> entry : request_temp.entrySet()){
				if(entry.getValue() == 0)
					continue;
				
				double interArrival = (StdRandom.exp(piRequest));
				timeArrival += interArrival; // arrival time of the next request
				timeArrival = (double)Math.round(timeArrival * 100000d) / 100000d;
				
				int piID = entry.getKey();
				int numSfc = entry.getValue();
				LinkedList<SFC> listSfc = new LinkedList<>();

				for(int i = 0; i < numSfc; i ++) {
					SFC sfc = new SFC(sfcID, piID);
					listSfc.add(sfc);
					sfcID ++;
//					System.out.println("SFCID: " + sfcID);
					double endTime = timeArrival + StdRandom.exp(1.0/LIVETIME*1.0);
					endTime = (double)Math.round(endTime * 100000d) / 100000d;
					LinkedList<SFC> listSfcLv = new LinkedList<>();
					listSfcLv.add(sfc);
					Event leave = new Event("leave", listSfcLv, endTime, piID);
					this.listEventTW.add(leave);
				}
				
				Event join = new Event("join", listSfc, timeArrival, piID);
				this.listEventTW.add(join);	
			}
				
		}
		
		this.listEventTW = sortEvent(this.listEventTW);
		
		for(int WD = 0; WD < numOfWD; WD ++) {
			LinkedList<Event> listEvent_temp = new LinkedList<>();
			for(Event event : this.listEventTW) {
				if(event.getTime() > WD && event.getTime() <= (WD + 1))
					listEvent_temp.add(event);
			}
			Collections.sort(listEvent_temp);
			
			this.listEvent.add(listEvent_temp);
		}
	}
	
	
	public LinkedList<Event> sortEvent(LinkedList<Event> listEvent){
		//order = true: low to high, false means high to low
		Collections.sort(listEvent, new Comparator<Event>() {
			@Override
	        public int compare(Event event1, Event event2) { 
	            // for comparison
				if(event1.getTime() > event2.getTime()) {
					return -1;
				}
				else if(event1.getTime() > event2.getTime()) {
					return 1;
				}
				return 0;
	        }
		});
		return listEvent;
	}

	
	public static void main(String[] args) {
//		GenRequest sample = new GenRequest();
//		sample.generator();
//		
////		int TW = 0;
//		for(LinkedList<Event> listEvent :  sample.getListEvent()) {
////			int join = 0;
//			for(Event ev : listEvent) {
//				if(ev.getType() == "join" && ev.getTime() > 9.0 && ev.getTime() < 10.0 )
//					System.out.println("Event join of pi: " + ev.getListSfc().size());
////				else {
////					System.out.println("Event leave time: " + ev.getTime() + " of pi: " + ev.getPiID());
////				}
////				if(ev.getTime() > 1.0)
////					return;
//			}
////			System.out.println("join TW " + TW +" is " + join);
////			TW ++;
//		}
		

		
//		Random generator_uni = new Random();
//		double lambda = 1000;
//		for(int i = 0; i < 100; i++) {
////			double seed = generator_uni.nextDouble();
//			double interArrival = (StdRandom.exp(lambda));
//			System.out.println("interarrival time: " + interArrival);
//		}
//		double seed = generator_uni.nextDouble();
//		double interArrival = -Math.log(1.0 - seed)/lambda;
//		GenRequest sample = new GenRequest();
//		sample.generator();
//		LinkedList<LinkedList<Event>> event = sample.getListEvent();
//		System.out.println("size: " + event.size());
//		int leave = 0;
//		int join = 0;
//		for(Event ev : event.get(11)) {
//			if(ev.getType() == "leave")
//				leave ++;
//			else
//				join ++;
////			System.out.println("Even type " + ev.getType() + " of SFC " + ev.getSfc().getSfcID() + " at Time: " + ev.getTime());
//		}
//		System.out.println("Leave number " + leave + " Join number " + join);
//		int leave = 0;
//		
//		for(int tw = 0; tw < 12; tw ++) {
//			int join = 0;
//			for(int i = 0; i < event.size(); i ++) {
//				if(event.get(i).getType() == "join" && event.get(i).getTime() < (tw+1) && event.get(i).getTime() > tw) {
//					join ++;
//				}
//			}
//			System.out.println("list join size " + join);
//		}
//		System.out.println("list join size " + join);
//		System.out.println("list leave size " + leave);
		
	}

	public LinkedList<LinkedList<Event>> getListEvent() {
		return listEvent;
	}
	
	public LinkedList<LinkedList<Event>> getListEventRep() {
		LinkedList<LinkedList<Event>> listEventRep = new LinkedList<>();
		for(int i = 0; i < listEvent.size(); i ++) {
			LinkedList<Event> listEventTWRep = new LinkedList<>();
			for(Event ev : listEvent.get(i)) {
				Event ev0 = new Event(ev);
				listEventTWRep.add(ev0);
			}
			listEventRep.add(listEventTWRep);
		}
		return listEventRep;
	}
	
	public void resetEvent() {
		for(int i = 0; i < listEvent.size(); i ++) {
			for(Event ev : listEvent.get(i)) {
				for(SFC sfc :ev.getListSfc())
					sfc.resetEvent();
			}
		}
		System.out.println();
	}
	
	public void setListEvent(LinkedList<LinkedList<Event>> listEvent) {
		this.listEvent = listEvent;
	}

	public LinkedList<Event> getListEventTW() {
		return listEventTW;
	}

	public void setListEventTW(LinkedList<Event> listEventTW) {
		this.listEventTW = listEventTW;
	}

	public LinkedList<HashMap<Rpi, Double>> getListEventCR() {
		return listEventCR;
	}

	public void setListEventCR(LinkedList<HashMap<Rpi, Double>> listEventCR) {
		this.listEventCR = listEventCR;
	}
	
//	public LinkedList<HashMap<Rpi, Double>> getListTotalRequest() {
//		return listTotalRequest;
//	}
//
//	public void setListTotalRequest(LinkedList<HashMap<Rpi, Double>> listTotalRequest) {
//		this.listTotalRequest = listTotalRequest;
//	}
	
}
