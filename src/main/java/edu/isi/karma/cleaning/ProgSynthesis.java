package edu.isi.karma.cleaning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class ProgSynthesis {
	public static int time_limit = 20;
	List<List<TNode>> orgList = new ArrayList<List<TNode>>();
	List<List<TNode>> tarList = new ArrayList<List<TNode>>();
	String bestRuleString = "";
	// for tracking the stats
	public long learnspan = 0;
	public long genspan = 0;
	public long ruleNo = 0;
	public PartitionClassifierType classifier;

	public void inite(List<String[]> examples) {
		for (int i = 0; i < examples.size(); i++) {
			Ruler r = new Ruler();
			r.setNewInput(examples.get(i)[0]);
			orgList.add(r.vec);
			Ruler r1 = new Ruler();
			r1.setNewInput(examples.get(i)[1]);
			tarList.add(r1.vec);
		}
	}

	public List<List<Integer>> generateCrossIndex(List<Integer> poss,
			List<List<Integer>> p, int index) {
		List<List<Integer>> qList = new ArrayList<List<Integer>>();
		if (index >= poss.size()) {
			return p;
		}
		int curleng = poss.get(index);
		if (p.size() == 0) {
			for (int i = 0; i < curleng; i++) {
				List<Integer> x = new ArrayList<Integer>();
				x.add(i);
				qList.add(x);
			}
		} else {
			for (int j = 0; j < p.size(); j++) {
				for (int i = 0; i < curleng; i++) {
					List<Integer> x = new ArrayList<Integer>(p.get(j));
					x.add(i);
					qList.add(x);
				}
			}
		}
		qList = generateCrossIndex(poss, qList, index + 1);
		return qList;
	}

	public double getCompScore(int i, int j, List<Partition> pars) {
		Partition p = pars.get(i).mergewith(pars.get(j));
		if (p == null) {
			return -Double.MAX_VALUE;
		}
		int validCnt = 0;
		for (int x = 0; x < pars.size(); x++) {
			if (x == i || x == j) {
				continue;
			}
			Partition q = p.mergewith(pars.get(x));
			if (q != null) {
				validCnt++;
			}
		}
		return validCnt;
	}

	public List<Partition> initePartitions() {
		List<Partition> pars = new ArrayList<Partition>();
		// inite partition for each example
		for (int i = 0; i < orgList.size(); i++) {
			List<List<TNode>> ovt = new ArrayList<List<TNode>>();
			List<List<TNode>> tvt = new ArrayList<List<TNode>>();
			ovt.add(this.orgList.get(i));
			tvt.add(this.tarList.get(i));
			Partition pt = new Partition(ovt, tvt);
			pars.add(pt);
		}
		return pars;
	}

	public void mergePartitions(List<Partition> pars) {
		double maxScore = 0;
		int[] pos = { -1, -1 };
		for (int i = 0; i < pars.size(); i++) {
			for (int j = i + 1; j < pars.size(); j++) {
				double s = getCompScore(i, j, pars);
				if (s < 0) {
					continue;
				}
				if (s >= maxScore) {
					pos[0] = i;
					pos[1] = j;
					maxScore = s;
				}
			}
		}
		if (pos[0] != -1 && pos[1] != -1)
			UpdatePartitions(pos[0], pos[1], pars);
	}

	public void UpdatePartitions(int i, int j, List<Partition> pars) {
		Partition p = pars.get(i).mergewith(pars.get(j));
		pars.set(i, p);
		pars.remove(j);
	}

	public String getBestRule() {
		return this.bestRuleString;
	}

	public List<Partition> ProducePartitions(boolean condense) {
		List<Partition> pars = this.initePartitions();
		int size = pars.size();
		while (condense) {
			this.mergePartitions(pars);
			if (size == pars.size()) {
				break;
			} else {
				size = pars.size();
			}
		}
		return pars;
	}

	public Collection<ProgramRule> producePrograms(List<Partition> pars) {
		Program prog = new Program(pars);
		HashSet<ProgramRule> rules = new HashSet<ProgramRule>();
		int prog_cnt = 1;
		int i = 0;
		long startTime = System.currentTimeMillis();
		while (i < prog_cnt) {
			ProgramRule r = prog.toProgram1();
			if (r == null)
				return null;
			String xString = "";
			int termCnt = 0;
			boolean findRule = true;
			while ((xString = this.validRule(r,pars)) != "GOOD" && findRule) {
				if(xString.compareTo("NO_CLASIF")==0)
				{
					return null; // indistinguishable classes.
				}
				if (termCnt == 10) {
					termCnt = 0;
					if((System.currentTimeMillis() - startTime)/1000 >= time_limit)
					{
						findRule = false;
						break;
					}
				}
				for (Partition p : prog.partitions) {
					if (p.label.compareTo(xString) == 0) {
						String newRule = p.toProgram();
						if (ConfigParameters.debug == 1)
							System.out.println("updated Rule: " + p.label
									+ ": " + newRule);
						if (newRule.contains("null")) {
							findRule = false;
							break;
						}
						r.updateClassworker(xString, newRule);
					}
				}
				termCnt++;
			}
			if (findRule)
				rules.add(r);
			this.ruleNo += termCnt; // accumulate the no of rules while the
			i++;
		}
		return rules;
	}

	public Collection<ProgramRule> run_main() {
		long t1 = System.currentTimeMillis();
		List<Partition> vp = this.ProducePartitions(true);
		long t2 = System.currentTimeMillis();
		Collection<ProgramRule> cpr = this.producePrograms(vp);
		long t3 = System.currentTimeMillis();
		learnspan = (t2 - t1);
		genspan = (t3 - t2);
		if (cpr == null || cpr.size() == 0 ) {
			t1 = System.currentTimeMillis();
			vp = this.ProducePartitions(false);
			t2 = System.currentTimeMillis();
			cpr = this.producePrograms(vp);
			t3 = System.currentTimeMillis();
			learnspan += t2 - t1;
			genspan += t3 - t2;
		}
		Traces.AllSegs.clear();
		return cpr;
	}

	public String validRule(ProgramRule p,List<Partition> vp) {
		for(Partition px:vp)
		{
			for (int i = 0; i < px.orgNodes.size(); i++) {
				String s1 = UtilTools.print(px.orgNodes.get(i));
				String labelString = p.getClassForValue(s1);
				if(labelString.compareTo(px.label)!=0)
				{
					return "NO_CLASIF";
				}
				InterpreterType worker = p.getWorkerForClass(labelString);
				String s2 = worker.execute(s1);
				String s3 = UtilTools.print(px.tarNodes.get(i));
				if (s3.compareTo(s2) != 0) {
					return labelString;
				}
			}
		}
		return "GOOD";
	}
}
