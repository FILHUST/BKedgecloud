package fil.resource.virtual;

import java.util.LinkedList;

public class Event implements Comparable<Event> {
	private String type;
	private LinkedList<SFC> listSfc;
	private double time;
	private int piID;
	
	public Event() {
		type = "None";
		time = 0;
		piID = 0;
		listSfc = new LinkedList<>();
	}
	
	public Event(Event event) { // replicated object
		type = event.getType();
		time = event.getTime();
		piID = event.getPiID();
		listSfc = new LinkedList<>();
		for(SFC sfc : event.getListSfc()) {
			sfc.reset();
			listSfc.add(sfc);
		}
	}
	
	public Event(String type, LinkedList<SFC> listSfc, double time, int piID) {
		this.type = type;
		this.time = time;
		this.piID = piID;
		this.listSfc = listSfc;
	}
	
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public int getPiID() {
		return piID;
	}
	public void setPiID(int piID) {
		this.piID = piID;
	}

	
	@Override
	public int compareTo(Event compareEv) {
		// TODO Auto-generated method stub
		double compareTime = compareEv.getTime();
		if(compareTime > this.time)
			return -1;
		else
			return 1;
        /* For Ascending order*/
//        return (int) (this.time - compareTime);
	}

	public LinkedList<SFC> getListSfc() {
		return listSfc;
	}

	public void setListSfc(LinkedList<SFC> listSfc) {
		this.listSfc = listSfc;
	}

	
}
