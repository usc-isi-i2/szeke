package edu.isi.karma.cleaning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;


public class Traces implements GrammarTreeNode {
	public static int time_limit = 20;
	public List<TNode> orgNodes;
	public List<TNode> tarNodes;
	public HashMap<Integer, Template> traceline = new HashMap<Integer, Template>();
	public HashMap<Integer, HashMap<String,Template>> loopline = new HashMap<Integer, HashMap<String,Template>>();
	private int curState = 0;
	private List<Template> totalOrderList = new ArrayList<Template>();
	//keep all the segment expression to prevent repeated construction
	public static HashMap<String, Segment> AllSegs = new HashMap<String, Segment>();

	public Traces(List<TNode> org, List<TNode> tar) {
		this.orgNodes = org;
		this.tarNodes = tar;
		this.createTraces();
		createTotalOrderList();
	}

	public void createTotalOrderList() {
		ArrayList<Integer> xArrayList = new ArrayList<Integer>();
		xArrayList.addAll(traceline.keySet());
		xArrayList.addAll(loopline.keySet());
		Integer[] a = new Integer[xArrayList.size()];
		Arrays.sort(xArrayList.toArray(a));
		for (Integer elem : a) {
			if (loopline.get(elem) != null) {
				totalOrderList.addAll(loopline.get(elem).values());
			}
			if (traceline.get(elem) != null) {
				totalOrderList.add(traceline.get(elem));
			}
		}
	}

	public Traces(HashMap<Integer, Template> t,
			HashMap<Integer,HashMap<String, Template>> l) {
		this.traceline = t;
		this.loopline = l;
		createTotalOrderList();
	}

	// initialize the tree to represent the grammar tree
	public void createTraces() {
		List<List<Segment>> lines = new ArrayList<List<Segment>>();
		HashMap<Integer, List<Segment>> pos2Segs = new HashMap<Integer, List<Segment>>();
		List<Segment> children = findSegs(0);
		List<List<Segment>> tlines = new ArrayList<List<Segment>>();
		for (Segment c : children) {
			List<Segment> vs = new ArrayList<Segment>();
			vs.add(c);
			tlines.add(vs);
		}
		long stime = System.currentTimeMillis();
		// find all possible segments starting from a position
		while (tlines.size() > 0) {
			if((System.currentTimeMillis()-stime)/1000>time_limit) 
			{
				//System.out.println("Exceed the time limit");
				lines.clear();
				break; // otherwise takes too much time
			}
			List<List<Segment>> nlines = new ArrayList<List<Segment>>();
			List<Segment> segs = tlines.remove(0);
			int curPos = segs.get(segs.size() - 1).end;
			if(curPos == 0) // target string is empty
			{
				lines.add(segs);
				break;
			}
			if (pos2Segs.containsKey(curPos))
				children = pos2Segs.get(curPos);
			else {
				children = findSegs(curPos);
			}
			if (children == null || children.size() == 0) {
				lines.add(segs);
			}
			for (Segment s : children) {
				List<Segment> tmp = new ArrayList<Segment>();
				tmp.addAll(segs);
				tmp.add(s);
				nlines.add(tmp);
			}
			tlines.addAll(nlines);
		}
		List<List<GrammarTreeNode>> vSeg = new ArrayList<List<GrammarTreeNode>>();
		List<List<GrammarTreeNode>> lSeg = new ArrayList<List<GrammarTreeNode>>();
		for (List<Segment> vs : lines) {
			List<GrammarTreeNode> vsGrammarTreeNodes = UtilTools
					.convertSegList(vs);
			vSeg.add(vsGrammarTreeNodes);
		}
		// detect loops
		// verify loops
		long vgt_time_limit = System.currentTimeMillis();
		for (List<Segment> vgt : lines) {
			if((System.currentTimeMillis()-vgt_time_limit)/1000>time_limit) 
			{
				break; // otherwise takes too much time
			}
			List<List<GrammarTreeNode>> lLine = this.genLoop(vgt);
			if (lLine != null)
				lSeg.addAll(lLine);
		}
		// consolidate
		this.traceline = consolidateDiffSize(vSeg);
		this.loopline = consolidateDiffLoop(lSeg);
	}

	// find all segments starting from pos
	List<Segment> findSegs(int pos) {
		List<Segment> segs = new ArrayList<Segment>();
		if(tarNodes.size() == 0)
		{
			int[] mapping = {0,0};
			List<int[]> corrm = new ArrayList<int[]>();
			corrm.add(mapping);
			Segment s = new Segment(0, 0, corrm, orgNodes, tarNodes);
			segs.add(s);
			return segs;
		}
		if (pos >= tarNodes.size())
			return segs;
		List<TNode> tmp = new ArrayList<TNode>();
		tmp.add(tarNodes.get(pos));
		// identify the const string	
		int q = Ruler.Search(orgNodes, tmp, 0);
		if (q == -1) {
			int cnt = pos;
			List<TNode> tvec = new ArrayList<TNode>();
			while (q == -1) {
				tvec.add(tarNodes.get(cnt));
				cnt++;
				tmp.clear();
				if(cnt >= tarNodes.size())
					break;
				tmp.add(tarNodes.get(cnt));
				q = Ruler.Search(orgNodes, tmp, 0);
			}
			String key = UtilTools.print(this.tarNodes)+pos+cnt;
			Segment seg;
			if(AllSegs.containsKey(key))
			{
				seg = AllSegs.get(key);
			}
			else
			{
				seg = new Segment(pos,cnt,tvec);
				AllSegs.put(key, seg);
			}
			segs.add(seg);
			return segs;
		}
		for (int i = pos; i < tarNodes.size(); i++) {
			List<TNode> tvec = new ArrayList<TNode>();
			for (int j = pos; j <= i; j++) {
				tvec.add(tarNodes.get(j));
			}
			List<Integer> mappings = new ArrayList<Integer>();
			int r = Ruler.Search(orgNodes, tvec, 0);
			while (r != -1) {
				mappings.add(r);
				r = Ruler.Search(orgNodes, tvec, r + 1);
			}
			if (mappings.size() > 1) {
				List<int[]> corrm = new ArrayList<int[]>();
				for (int t : mappings) {
					int[] m = { t, t + tvec.size() };
					corrm.add(m);
				}
				// create a segment now
				String key = UtilTools.print(this.tarNodes)+pos+(i+1);
				Segment s;
				if(AllSegs.containsKey(key))
				{
					s = AllSegs.get(key);
				}
				else
				{
					s = new Segment(pos, i + 1, corrm, orgNodes, tarNodes);
					AllSegs.put(key, s);
				}
				if(s.section.size() >0)
					segs.add(s);
				continue;
			} else if (mappings.size() == 1) {
				List<int[]> corrm = new ArrayList<int[]>();
				// creating based on whether can find segment with one more
				// token
				if (i >= (tarNodes.size() - 1)) {
					int[] m = { mappings.get(0), mappings.get(0) + tvec.size() };
					corrm.add(m);
					String key = UtilTools.print(this.tarNodes)+pos+(i+1);
					Segment s;
					if(AllSegs.containsKey(key))
					{
						s = AllSegs.get(key);
					}
					else
					{
						s = new Segment(pos, i + 1, corrm, orgNodes, tarNodes);
						AllSegs.put(key, s);
					}
					if(s.section.size() >0)
						segs.add(s);
				} else {
					tvec.add(tarNodes.get(i + 1));
					int p = Ruler.Search(orgNodes, tvec, 0);
					List<TNode> repToken = new ArrayList<TNode>();
					repToken.add(tarNodes.get(i+1));
					int rind = 0;
					int tokenCnt = 0;
					while ((rind=Ruler.Search(orgNodes, repToken, rind))!=-1)
					{
						rind++;
						tokenCnt++;
					}				
					if (p == -1 ||(tokenCnt>1 && tarNodes.get(i + 1).text.compareTo(" ")!=0)) {
						int[] m = { mappings.get(0),
								mappings.get(0) + tvec.size() - 1 };
						corrm.add(m);
						String key = UtilTools.print(this.tarNodes)+pos+(i+1);
						Segment s;
						if(AllSegs.containsKey(key))
						{
							s = AllSegs.get(key);
						}
						else
						{
							s = new Segment(pos, i + 1, corrm, orgNodes, tarNodes);
							AllSegs.put(key, s);
						}
						if(s.section.size() > 0)
							segs.add(s);
					} else {
						continue;
					}
				}
			} else {
				break;
			}
		}
		return segs;
	}

	public Traces mergewith(Traces t) {
		// merge segment lines
		Set<Integer> keyset = new HashSet<Integer>(this.traceline.keySet());
		keyset.retainAll(t.traceline.keySet());
		HashMap<Integer, Template> nLines = new HashMap<Integer, Template>();
		HashMap<Integer, HashMap<String,Template>> lLines = new HashMap<Integer, HashMap<String,Template>>();
		for (Integer index : keyset) {
			Template line1 = this.traceline.get(index);
			Template line2 = t.traceline.get(index);
			//System.out.println(""+line1+"\n");
			//System.out.println(""+line2+"\n");
			Template nLine = (Template)line1.mergewith(line2);
			if(nLine == null)
				continue;
			boolean isfind = true;
			nLines.put(index, nLine);
		}
		// merge loop and segment lines
		HashMap<Integer, HashMap<String,List<Template>>> allLoops = new HashMap<Integer, HashMap<String,List<Template>>>();
		Set<Integer> keyset1 = new HashSet<Integer>(this.traceline.keySet());
		keyset1.retainAll(t.loopline.keySet());
		for (Integer index : keyset1) {
			Template line1 = this.traceline.get(index);
			Collection<String> line2s = t.loopline.get(index).keySet();
			for(String line2:line2s)
			{
				//System.out.println(""+line1+"\n");
				//System.out.println(""+t.loopline.get(index).get(line2)+"\n");
				Template nLine = (Template)line1.mergewith(t.loopline.get(index).get(line2));
				if(nLine == null)
					continue;
				if (allLoops.containsKey(index)) {
					if(allLoops.get(index).containsKey(line2))
					{
						allLoops.get(index).get(line2).add(nLine);
					}
					else
					{
						List<Template> vTemplates = new ArrayList<Template>();
						vTemplates.add(nLine);
						allLoops.get(index).put(line2, vTemplates);
					}
				} else {
					List<Template> tgt = new ArrayList<Template>();
					tgt.add(nLine);
					HashMap<String, List<Template>> tHashMap = new HashMap<String, List<Template>>();
					tHashMap.put(line2, tgt);
					allLoops.put(index,tHashMap);
				}
			}
		}
		// merge loop and segment
		keyset1 = new HashSet<Integer>(this.loopline.keySet());
		keyset1.retainAll(t.traceline.keySet());
		for (Integer index : keyset1) {
			Collection<String> line2s = this.loopline.get(index).keySet();
			Template line1 = t.traceline.get(index);
			for(String line2:line2s)
			{
				//System.out.println(""+line1+"\n");
				//System.out.println(""+this.loopline.get(index).get(line2)+"\n");
				Template nLine = (Template)line1.mergewith(this.loopline.get(index).get(line2));
				if(nLine == null)
					continue;
				if (allLoops.containsKey(index)) {
					if(allLoops.get(index).containsKey(line2))
					{
						allLoops.get(index).get(line2).add(nLine);
					}
					else
					{
						List<Template> vTemplates = new ArrayList<Template>();
						vTemplates.add(nLine);
						allLoops.get(index).put(line2, vTemplates);
					}
				} else {
					List<Template> tgt = new ArrayList<Template>();
					tgt.add(nLine);
					HashMap<String, List<Template>> tHashMap = new HashMap<String, List<Template>>();
					tHashMap.put(line2, tgt);
					allLoops.put(index,tHashMap);
				}
			}
		}
		// merge loop and loop
		keyset1 = new HashSet<Integer>(this.loopline.keySet());
		keyset1.retainAll(t.loopline.keySet());
		for (Integer index : keyset1) {
			Collection<String> line1s = this.loopline.get(index).keySet();
			Collection<String> line2s = t.loopline.get(index).keySet();
			for(String line1:line1s)
			{
				for(String line2:line2s)
				{
					if(line1.compareTo(line2)!=0)
					{
						continue;
					}
					//System.out.println(""+this.loopline.get(index).get(line1)+"\n");
					//System.out.println(""+t.loopline.get(index).get(line2)+"\n");
					Template nLine = (Template) this.loopline.get(index).get(line1).mergewith(t.loopline.get(index).get(line2));
					if(nLine == null)
						continue;
					if (allLoops.containsKey(index)) {
						if(allLoops.get(index).containsKey(line2))
						{
							allLoops.get(index).get(line2).add(nLine);
						}
						else
						{
							List<Template> vTemplates = new ArrayList<Template>();
							vTemplates.add(nLine);
							allLoops.get(index).put(line2, vTemplates);
						}
					} else {
						List<Template> tgt = new ArrayList<Template>();
						tgt.add(nLine);
						HashMap<String, List<Template>> tHashMap = new HashMap<String, List<Template>>();
						tHashMap.put(line2, tgt);
						allLoops.put(index,tHashMap);
					}
				}
			}
		}
		for (Integer key : allLoops.keySet()) {
			for(String subkey:allLoops.get(key).keySet())
			{
				Template xline = this.consolidate(allLoops.get(key).get(subkey));
				if(lLines.containsKey(key))
				{
					lLines.get(key).put(subkey, xline);
				}
				else
				{
					HashMap<String, Template> xHashMap = new HashMap<String, Template>();
					xHashMap.put(subkey, xline);
					lLines.put(key, xHashMap);
				}
				
			}
		}
		if (lLines.keySet().size() == 0 && nLines.keySet().size() == 0)
			return null;
		Traces rTraces = new Traces(nLines, lLines);
		return rTraces;
	}

	public GrammarTreeNode union(GrammarTreeNode x, GrammarTreeNode y) {
		if (x.getNodeType().compareTo("segment") == 0
				&& y.getNodeType().compareTo("segment") == 0) {
			Segment s = (Segment) x;
			Segment t = (Segment) y;
			List<Section> sec = new ArrayList<Section>();
			HashSet<String> hset = new HashSet<String>();
			for(Section nSection:s.section)
			{
				String key = nSection.toString();
				if(hset.contains(key))
				{
					continue;
				}
				else
				{
					hset.add(key);
					sec.add(nSection);
				}
			}
			for(Section nSection: t.section)
			{
				String key = nSection.toString();
				if(hset.contains(key))
				{
					continue;
				}
				else
				{
					hset.add(key);
					sec.add(nSection);
				}
			}
			boolean loop = s.isinloop || t.isinloop;
			Segment r;
			if(s.isConstSegment() && t.isConstSegment())
				r = new Segment(sec,loop);
			else
				r = s;
			return r;
		}
		if (x.getNodeType().compareTo("loop") == 0
				&& y.getNodeType().compareTo("loop") == 0) {
			Loop s = (Loop) x;
			Loop t = (Loop) y;
			List<Section> sec = new ArrayList<Section>();
			HashSet<String> hset = new HashSet<String>();
			for(Section nSection:s.loopbody.section)
			{
				String key = nSection.toString();
				if(hset.contains(key))
				{
					continue;
				}
				else
				{
					hset.add(key);
					sec.add(nSection);
				}
			}
			for(Section nSection: t.loopbody.section)
			{
				String key = nSection.toString();
				if(hset.contains(key))
				{
					continue;
				}
				else
				{
					hset.add(key);
					sec.add(nSection);
				}
			}
			Loop r;
			if (s.looptype == t.looptype)
			{
				Segment loopbody;
				if(s.loopbody.isConstSegment() && t.loopbody.isConstSegment())
					loopbody= s.loopbody;
				else {
					loopbody = new Segment(sec,true);
				}
				r = new Loop(loopbody, t.looptype);
			}
			else {
				return null;
			}
			return r;
		} 
		return null;
	}
	public HashMap<Integer, HashMap<String,Template>> consolidateDiffLoop(
			List<List<GrammarTreeNode>> paths) {
		HashMap<Integer, HashMap<String,Template>> resHashMap = new HashMap<Integer, HashMap<String,Template>>();
		HashMap<Integer, HashMap<String, List<Template>>> tmpStore = new HashMap<Integer,HashMap<String, List<Template>>>();
		for (List<GrammarTreeNode> vg : paths) {
			int key = vg.size();
			String subkey = "";
			for(GrammarTreeNode nodetype: vg)
			{
				subkey += nodetype.getNodeType();
			}
			if (tmpStore.containsKey(key)) {
				if(tmpStore.get(key).containsKey(subkey))
				{
					tmpStore.get(key).get(subkey).add(new Template(vg));
				}
				else
				{
					List<Template> vte = new ArrayList<Template>();
					vte.add(new Template(vg));
					tmpStore.get(key).put(subkey, vte);
				}
			} else {
				List<Template> xList = new ArrayList<Template>();
				if(!isLegalVS(vg))
					continue;
				xList.add(new Template(vg));
				HashMap<String, List<Template>> xHashMap = new HashMap<String, List<Template>>();
				xHashMap.put(subkey, xList);
				tmpStore.put(key, xHashMap);
			}
		}
		for (Integer key : tmpStore.keySet()) {
			for(String kInteger:tmpStore.get(key).keySet())
			{
				Template x = this.consolidate(tmpStore.get(key).get(kInteger));
				if(resHashMap.containsKey(key))
				{
					resHashMap.get(key).put(kInteger, x);
				}
				else
				{
					HashMap<String, Template> hashMap = new HashMap<String, Template>();
					hashMap.put(kInteger, x);
					resHashMap.put(key, hashMap);
				}
			}
		}
		return resHashMap;
	}
	public HashMap<Integer, Template> consolidateDiffSize(
			List<List<GrammarTreeNode>> paths) {
		HashMap<Integer, Template> resHashMap = new HashMap<Integer, Template>();
		HashMap<Integer, List<Template>> tmpStore = new HashMap<Integer, List<Template>>();
		long stime = System.currentTimeMillis();
		for (List<GrammarTreeNode> vg : paths) {
			if((System.currentTimeMillis()-stime)/1000>time_limit) 
			{
				//System.out.println("Exceed the time limit");
				break; // otherwise takes too much time
			}
			int key = vg.size();
			if (tmpStore.containsKey(key)) {
				tmpStore.get(key).add(new Template(vg));
			} else {
				List<Template> xList = new ArrayList<Template>();
				if(!isLegalVS(vg))
					continue;
				xList.add(new Template(vg));
				tmpStore.put(key, xList);
			}
		}
		for (Integer key : tmpStore.keySet()) {
			//System.out.println(""+tmpStore.get(key).toString());
			Template x = this.consolidate(tmpStore.get(key));
			resHashMap.put(key, x);
		}
		//System.out.println("end consolidating");
		return resHashMap;
	}
	public boolean isLegalVS(List<GrammarTreeNode> x)
	{
		for(GrammarTreeNode t:x)
		{
			if(t.size()<=0)
				return false;
		}
		return true;
	}
	public Template consolidate(
			List<Template> paths) {
		List<GrammarTreeNode> res = paths.get(0).body;
		List<GrammarTreeNode> result = new ArrayList<GrammarTreeNode>();
		for (int i = 1; i < paths.size(); i++) {
			
			result = new ArrayList<GrammarTreeNode>();
			for (int j = 0; j < res.size(); j++) {
				GrammarTreeNode t = this.union(res.get(j), paths.get(i).body.get(j));
				result.add(t);
			}
			res = result;
		}
		return new Template(res);
	}

	public List<List<GrammarTreeNode>> loopPathes = new ArrayList<List<GrammarTreeNode>>();

	public List<List<GrammarTreeNode>> genLoop(List<Segment> curPath) {
		// cluster chunk with the same head and tail token
		List<List<GrammarTreeNode>> res = new ArrayList<List<GrammarTreeNode>>();
		HashMap<String, List<Integer>> map = new HashMap<String, List<Integer>>();
		for (int i = 0; i < curPath.size(); i++) {
			String rep = curPath.get(i).repString;
			if (map.containsKey(rep)) {
				map.get(rep).add(i);
			} else {
				List<Integer> vIntegers = new ArrayList<Integer>();
				vIntegers.add(i);
				map.put(rep, vIntegers);
			}
		}
		for (String key : map.keySet()) {
			List<Integer> v = map.get(key);
			if (v.size() <= 1 || !this.verfiyLoop(v, curPath))
				continue;
			List<List<GrammarTreeNode>> vtn = this.detectLoop(v, curPath);
			if (vtn != null && vtn.size() > 0) {
				res.addAll(vtn);
			}
		}
		if(res.size() ==0)
			return null;
		else
			return res;
	}
	//if there is a cross of the mapping. the loop doesn't exist
	public boolean verfiyLoop(List<Integer> vx,List<Segment> segs)
	{
		boolean res = true;
		int pre = -1;
		for(int i:vx)
		{
			Segment s = segs.get(i);
			if(s.isConstSegment())
			{
				return true;
			}
			int pivot = pre;
			if(s.mappings.size()>=0)
			{
				boolean isFind = false;
				for(int[] n:s.mappings)
				{
					if(n[0]>pivot && !isFind)
					{
						pre = n[0];
						isFind = true;
					}
					else if(n[0]<pre && n[0]>pivot &&isFind)
					{
						pre = n[0];
					}
				}
				if(!isFind)
				{
					return false;
				}
			}
		}
		return res;
	}
	
	public List<GrammarTreeNode> subList(List<Segment> nodes, int start, int end)
	{
		if(start>=end || start <0 || end>nodes.size())
			return null;
		List<GrammarTreeNode> vgt = new ArrayList<GrammarTreeNode>();
		for(int i = start; i< end; i++)
		{
			vgt.add(nodes.get(i));
		}
		return vgt;
	}
	// merge the longest mergable chunk. 
	// if None chunk could be merged together return null
	public List<GrammarTreeNode> createLoop(List<GrammarTreeNode> nodes,int span)
	{
		List<GrammarTreeNode> res = new ArrayList<GrammarTreeNode>();
		List<List<GrammarTreeNode>> gt = new ArrayList<List<GrammarTreeNode>>();
		int pos = 0;
		while(pos < nodes.size())
		{
			List<GrammarTreeNode> x = new ArrayList<GrammarTreeNode>();
			for(int k = pos; k<pos+span && k< nodes.size(); k++)
			{
				x.add(nodes.get(k));
			}
			gt.add(x);
			pos += span;
		}
		//no loop if only one chunk
		if(gt.size() == 1)
		{
			return null;
		}
		int itercnt = 1; // count of the span
		int curStartPos = 0;// the lowest position the first element
		int presize = gt.size();
		while(gt.size() > 1)
		{
			List<List<GrammarTreeNode>> tmp_gt = new ArrayList<List<GrammarTreeNode>>();
			boolean isLegal = true;
			for(int j = 0; j<gt.size()-1; j++)
			{
				isLegal = true;
				List<GrammarTreeNode> elem = new ArrayList<GrammarTreeNode>();
				List<GrammarTreeNode> mx = gt.get(j);
				List<GrammarTreeNode> nx = gt.get(j+1);
				if(mx.size() != nx.size())
					return null;
				
				for(int r = 0; r< mx.size(); r++)
				{
					GrammarTreeNode treeNode = mx.get(r).mergewith(nx.get(r));
					if(treeNode == null)
					{
						isLegal =false;
						break;
					}
					elem.add(treeNode);
				}
				if(isLegal)
				{
					tmp_gt.add(elem);
				}
				if(!isLegal && tmp_gt.size() ==0)
				{
					curStartPos += span;
				}
			}
			if(!isLegal)
				return null; // nothing merged in this iteration;
			gt = tmp_gt;
			itercnt ++;
			if(gt.size() == presize)
			{
				break;
			}
			else
			{
				presize = gt.size();
			}
		}
		//only detect one loop
		if(gt.size() >1)
			return null;
		//
		for(int i = 0; i<nodes.size(); i++)
		{
			if(i<curStartPos)
			{
				res.add(nodes.get(i));
			}
			else if(i ==curStartPos+itercnt*span-1)
			{
				List<GrammarTreeNode> body = gt.get(0);
				if(span == 1)
				{
					Loop loop = new Loop((Segment)body.get(0), Loop.LOOP_BOTH);
					res.add(loop);
					continue;
				}
				for(int j = 0; j<body.size(); j++)
				{
					if(j == 0)
					{
						Loop loop = new Loop((Segment)body.get(j), Loop.LOOP_START);
						res.add(loop);
					}
					else if(j == body.size()-1)
					{
						Loop loop = new Loop((Segment)body.get(j), Loop.LOOP_END);
						res.add(loop);
					}
					else
					{
						Loop loop = new Loop((Segment)body.get(j), Loop.LOOP_MID);
						res.add(loop);
					}
				}
			}
			else if(i >= curStartPos+itercnt*span)
			{
				res.add(nodes.get(i));
			}
		}
		return res;
	}
	public List<List<GrammarTreeNode>> detectLoop(List<Integer> rep,
			List<Segment> curPath) {
		int span = rep.get(1) - rep.get(0);
		List<List<GrammarTreeNode>> resList = new ArrayList<List<GrammarTreeNode>>();
		List<GrammarTreeNode> nodelist = new ArrayList<GrammarTreeNode>();
		if (span == 1) {
			int startpos = rep.get(0);
			int endpos = rep.get(rep.size() - 1);
			Segment seg = curPath.get(startpos);
			for (int i = 0; i < curPath.size(); i++) {
				Segment segb = curPath.get(i);
				if (i < startpos) {
					nodelist.add(curPath.get(i));
				} else if ( i == endpos) {
					List<GrammarTreeNode> sublist = this.createLoop(subList(curPath, startpos, endpos+1), 1);
					if(sublist == null)
						return null;
					nodelist.addAll(sublist);
				} else if (i > endpos) {
					nodelist.add(curPath.get(i));
				}
			}
		} else {
			int startpos = rep.get(0);
			int endpos = rep.get(rep.size() - 1);
			// left overflow
			if ((startpos - span + 1) < 0 && endpos + span <= curPath.size()) {
				for (int i = 0; i < curPath.size(); i++) {
					Segment segb = curPath.get(i);
					if (i < startpos) {
						nodelist.add(curPath.get(i));
					}
					if (i == endpos + span - 1) {
						List<GrammarTreeNode> sublist = this.createLoop(subList(curPath, startpos, endpos+span), span);
						if(sublist == null)
							return null;
						nodelist.addAll(sublist);
					} else if (i >= endpos + span) {
						nodelist.add(curPath.get(i));
					}
				}
			}
			// right overflow
			else if ((startpos - span + 1) >= 0
					&& endpos + span > curPath.size()) {
				for (int i = 0; i < curPath.size(); i++) {
					Segment segb = curPath.get(i);
					if (i < startpos - span + 1) {
						nodelist.add(curPath.get(i));
					}
					if (i == endpos) {						
						List<GrammarTreeNode> sublist = this.createLoop(subList(curPath, startpos-span+1, endpos+1), span);
						if(sublist == null)
							return null;
						nodelist.addAll(sublist);
					} else if (i > endpos) {
						nodelist.add(curPath.get(i));
					}
				}
			}
			// two direction overflow
			else if ((startpos - span + 1) < 0
					&& endpos + span >= curPath.size()) {
				// skip the startpos
				for (int i = 0; i < curPath.size(); i++) 
				{
					Segment segb = curPath.get(i);
					if (i <= startpos) {
						nodelist.add(curPath.get(i));
					}
					if (i == endpos) {
						List<GrammarTreeNode> sublist = this.createLoop(subList(curPath, startpos+1, endpos+1), span);
						if(sublist == null)
						{
							nodelist.clear();
							break;
						}
						nodelist.addAll(sublist);
					} else if (i > endpos) {						
						nodelist.add(curPath.get(i));
					}
				}
				List<GrammarTreeNode> nodelist1 = new ArrayList<GrammarTreeNode>();
				// skip the endpos
				for (int i = 0; i < curPath.size(); i++) {
					if (i < startpos) {
						nodelist1.add(curPath.get(i));
					}
					if (i == endpos - 1) {
						List<GrammarTreeNode> sublist = this.createLoop(
								subList(curPath, startpos, endpos), span);
						if (sublist == null) {
							nodelist1.clear();
							break;
						}
						nodelist1.addAll(sublist);
					} else if (i >= endpos) {
						nodelist1.add(curPath.get(i));
					}
				}
				if(nodelist1.size()>0)
				{
					resList.add(nodelist1);
				}
			}
			else {
				for (int i = 0; i < curPath.size(); i++) {
				    //shift left
					if (i < startpos - span + 1) {
						nodelist.add(curPath.get(i));
					}
					if (i == endpos) {
						List<GrammarTreeNode> sublist = this.createLoop(subList(curPath, startpos-span+1, endpos+1), span);
						if(sublist == null)
						{
							nodelist.clear();
							break;
						}
						nodelist.addAll(sublist);
					} else if (i > endpos ) {						
						nodelist.add(curPath.get(i));
					}
				}
				List<GrammarTreeNode> nodelist1 = new ArrayList<GrammarTreeNode>();
				for (int i = 0; i < curPath.size(); i++) {
					//shift right
					if (i < startpos) {
						nodelist1.add(curPath.get(i));
					}
					if (i == endpos + span - 1) {
						List<GrammarTreeNode> sublist = this.createLoop(subList(curPath, startpos, endpos+span), span);
						if(sublist == null)
						{
							nodelist1.clear();
							break;
						}
						nodelist1.addAll(sublist);
					} else if (i >= endpos + span ) {						
						nodelist1.add(curPath.get(i));
					}
				}
				if(nodelist1.size()>0)
					resList.add(nodelist1);
			}
		}
		if(nodelist.size()!=0)
		{
			resList.add(nodelist);
		}
		return resList;
	}

	public void tracePrint() {
		System.out.println("===============printing trace here===============");
		for (Integer key : this.traceline.keySet()) {
			System.out.println("" + traceline.get(key));
		}
		for (Integer key : this.loopline.keySet()) {
			System.out.println("" + loopline.get(key));
		}
	}

	public static boolean test() {
		String[] xStrings = { "<_START>A_BB_C_DD_E<_END>", "ABBCDDE" };
		String[] yStrings = { "<_START>F_GG_H_II_J<_END>", "FGGHIIJ" };
		List<String[]> examples = new ArrayList<String[]>();
		examples.add(xStrings);
		examples.add(yStrings);
		List<List<TNode>> org = new ArrayList<List<TNode>>();
		List<List<TNode>> tar = new ArrayList<List<TNode>>();
		for (int i = 0; i < examples.size(); i++) {
			Ruler r = new Ruler();
			r.setNewInput(examples.get(i)[0]);
			org.add(r.vec);
			Ruler r1 = new Ruler();
			r1.setNewInput(examples.get(i)[1]);
			tar.add(r1.vec);
		}
		Traces traces1 = new Traces(org.get(0), tar.get(0));
		traces1.tracePrint();
		Traces traces2 = new Traces(org.get(1), tar.get(1));
		Traces t = traces1.mergewith(traces2);
		t.tracePrint();
		return true;
	}

	public void emptyState() {
		for(GrammarTreeNode t:this.totalOrderList)
		{
			t.emptyState();
		}
	}
	
	public String toProgram() {
		String resString = "";
		while (curState < totalOrderList.size() ) {
			resString = this.totalOrderList.get(curState).toProgram();
			if(!resString.contains("null"))
			{
				return resString;
			}
			else
			{
				this.totalOrderList.get(curState).emptyState();
				curState ++;
			}
		}
		return "null";
	}
	public long size()
	{
		long size = 0;
		for(Template t:this.totalOrderList)
		{
			size += t.size();
		}
		return size;
	}
	public static void main(String[] args) {
		Traces.test();
	}

	@Override
	public GrammarTreeNode mergewith(GrammarTreeNode a) {
		Traces t = (Traces) a;

		return this.mergewith(t);
	}

	@Override
	public String getNodeType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getScore() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getrepString() {
		// TODO Auto-generated method stub
		return null;
	}
}
