package fil.resource.virtual;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import fil.resource.substrate.SubstrateSwitch;

public class BFSNew {
	private SubstrateSwitch startNode;
	private SubstrateSwitch endNode;
	LinkedList<SubstrateSwitch> shortestPath;
	public BFSNew(SubstrateSwitch start, SubstrateSwitch end)
	{
		this.startNode = start;
		this.endNode = end;
		this.shortestPath = new LinkedList<>();
	}
	public LinkedList<SubstrateSwitch> getShortestPath(Topology topo, double minBw, int typeMax)
	{
		boolean isUp = true;
		int nextType;
		Map<SubstrateSwitch, Integer> color = new HashMap<>();
		Map<SubstrateSwitch, SubstrateSwitch> back = new HashMap<>();
		for(SubstrateSwitch s: topo.getListSwitch())
		{
			color.put(s, 0);
			back.put(s, null);
		}
		color.put(startNode, 1);
		LinkedList<SubstrateSwitch> queue = new LinkedList<>();
		boolean findPath = false;
		queue.addFirst(startNode);
		while(!queue.isEmpty())
		{
			SubstrateSwitch sFirst = queue.getFirst();
			queue.removeFirst();
			LinkedList<SubstrateSwitch> nodes = topo.adjacentNodes(sFirst);
			
			if (sFirst.getType()==typeMax)
				isUp = false;
				
			if (isUp)
				nextType = sFirst.getType() + 1;
			else
				nextType = sFirst.getType() - 1;
			for(SubstrateSwitch s: nodes)
			{
				
				if(color.get(s)==0 && nextType==s.getType())
				{			
					if (minBw <= (1000 - sFirst.getBandwidthPort().get(s))) {
						color.put(sFirst, 1);
						back.put(s, sFirst);
						queue.addLast(s);
						if (s.equals(endNode)){
							findPath = true;
							break;
						}
					}
				}
			}
			if (findPath)
				break;
		}
		if (findPath) {
			PrintPath(startNode, endNode, back);
		}
		
		return shortestPath;
	}
	
	public LinkedList<SubstrateSwitch> getShortestPathGH(Topology topo)
	{
		Map<SubstrateSwitch, Integer> color = new HashMap<>();
		Map<SubstrateSwitch, SubstrateSwitch> back = new HashMap<>();
		for(SubstrateSwitch s: topo.getListSwitch())
		{
			color.put(s, 0);
			back.put(s, null);
		}
		color.put(startNode, 1);
		LinkedList<SubstrateSwitch> queue = new LinkedList<>();
		boolean findPath = false;
		queue.addFirst(startNode);
		while(!queue.isEmpty())
		{
			SubstrateSwitch sFirst = queue.getFirst();
			queue.removeFirst();
			LinkedList<SubstrateSwitch> nodes = topo.adjacentNodes(sFirst);
			for(SubstrateSwitch s: nodes)
			{
				if(color.get(s)==0)
				{
					color.put(sFirst, 1);
					back.put(s, sFirst);
					queue.addLast(s);
					if (s.equals(endNode)){
						findPath = true;
						break;
					}
				}
			}
			if (findPath)
				break;
		}
		if (findPath) {
			PrintPath(startNode, endNode, back);
		}
		
		return shortestPath;
	}
	
	public void PrintPath(SubstrateSwitch start, SubstrateSwitch end, Map<SubstrateSwitch, SubstrateSwitch> back)
	{
		SubstrateSwitch curSw = end;
		shortestPath.add(curSw);
		SubstrateSwitch prevSw = back.get(curSw);

		while (!prevSw.equals(start)) {
			shortestPath.add(prevSw);
			curSw = prevSw;
			prevSw = back.get(curSw);
			if(prevSw.equals(start)) {
				shortestPath.add(start);
			}
		}
	}
	
}
