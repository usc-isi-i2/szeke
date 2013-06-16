package edu.isi.karma.cleaning.features;

import java.util.HashMap;
import java.util.List;

import edu.isi.karma.cleaning.Ruler;
import edu.isi.karma.cleaning.TNode;

public class CntFeature implements Feature{
		String name = "";
		double score = 0.0;
		List<TNode> pa;
		public void setName(String name)
		{
			this.name = name;
		}
		public CntFeature(List<List<TNode>> v,List<List<TNode>> n,List<TNode> t)
		{
			pa = t;
			score= calFeatures(v,n);
		}
		// x is the old y is the new example
		public double calFeatures(List<List<TNode>> v,List<List<TNode>> n)
		{
			HashMap<Integer,Integer> tmp = new HashMap<Integer,Integer>();
			for(int i = 0; i<n.size();i++)
			{
				int cnt = 0;
				List<TNode> z = v.get(i);
				List<TNode> z1 = n.get(i);
				int bpos = 0;
				int p = 0;
				int bpos1 = 0;
				int p1 = 0;
				int cnt1 = 0;
				while (p!=-1)
				{
					p = Ruler.Search(z, pa, bpos);
					if(p==-1)
						break;
					bpos = p+1;
					cnt++;
				}
				while (p1!=-1)
				{
					p1 = Ruler.Search(z1, pa, bpos1);
					if(p1==-1)
						break;
					bpos1 = p1+1;
					cnt1++;
				}
				//use the minus value to compute homogenenity 
				//cnt = cnt - cnt1;
				cnt = cnt1;
				if(tmp.containsKey(cnt))
				{
					tmp.put(cnt, tmp.get(cnt)+1);
				}
				else
				{
					tmp.put(cnt, 1);
				}
			}
			Integer a[] = new Integer[tmp.keySet().size()];
			tmp.values().toArray(a);
			int b[] = new int[a.length];
			for(int i = 0; i<a.length;i++)
			{
				b[i] = a[i].intValue();
			}
			double res = RegularityFeatureSet.calShannonEntropy(b)*1.0/Math.log(v.size());
			return res;
		}
		public String getName() {
			
			return this.name;
		}
		
		public double getScore() {
			
			return score;
		}	
}
