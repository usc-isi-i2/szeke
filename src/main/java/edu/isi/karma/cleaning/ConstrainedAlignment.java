package edu.isi.karma.cleaning;

import java.util.ArrayList;
import java.util.List;

public class ConstrainedAlignment {
	public List<List<TNode>> olist;
	public List<List<TNode>> tlist;
	public int expnum;
	public ConstrainedAlignment()
	{
		
	}
	public List<Integer> findAll(TNode t,List<TNode> x)
	{
		return null;
	}
	// inserted same token  
	public void traverse()
	{
		//inite
		List<Integer> spos = new ArrayList<Integer>();
		for(int i = 0; i<expnum;i++)
		{
			spos.add(0);
		}
		int[] iters = new int[expnum];
		for(int i = 0; i<iters.length; i++)
		{
			iters[i] = 0;
		}
		for(int i = 0; i<expnum; i++)
		{
			
		}
		
		
	}
}
class LatentState{
	static final int SEG = 1;//a segement state
	static final int UND = 2;
	static final int ANC = 3;//a anchor state
	public int StateType;
	public LatentState nextState;
	public List<int[]> segments;
	public LatentState(List<Integer> spos)
	{
		this.StateType = LatentState.UND;
		segments = new ArrayList<int[]>();
		if(segments.size() !=spos.size())
		{
			System.out.println("Length different");
			return;
		}
		for(int i = 0; i<spos.size(); i++)
		{
			int p = spos.get(i);
			int[] a = {p,-1};
			segments.add(a);
		}
	}
	public void setStart(int s,int lindex)
	{
		segments.get(lindex)[0] = s;
	}
	public void setEnd(int e,int lindex)
	{
		segments.get(lindex)[1] = e;
	}
	public void createNewState(List<Integer> spos)
	{
		LatentState nState =new LatentState(spos);
		this.nextState = nState;
	}
	public void settype(int type)
	{
		
	}
}