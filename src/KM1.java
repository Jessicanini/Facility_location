import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class KM1 {
	FileWriter fw;
    BufferedWriter bw;
	ArrayList<Facility> fac=new ArrayList<Facility>();
	HashMap<Integer, ArrayList<DI>> cur_sol=new HashMap<Integer, ArrayList<DI>>();
	//Point[][] points;
	public KM1(ArrayList<Facility> facilities,HashMap<Integer, ArrayList<DI>> cur_sol){
		this.fac=facilities;
		this.cur_sol=cur_sol;
		//this.points=points;
	}
	public ArrayList<Point> getTemp(Point[][] point){
		ArrayList<Point> t1=new ArrayList<Point>();
		for(Integer e:this.cur_sol.keySet()){
			for(DI e1:cur_sol.get(e)){
				t1.add(point[e1.d][e1.k]);
			}
		}
		return t1;
		
	}
	

	public Instances generatePopularInstance(ArrayList<Point> entities) {
			//ArrayList<Attribute> attributes = null;
			//set attributes
			//FastVector attributes = new FastVector();
	        //Object //attributes;
			//attributes.addElement(new Attribute("lon"));
	        //attributes.addElement(new Attribute("lat"));
	        ArrayList<Attribute> attributes = new ArrayList<>();
	        attributes.add(new Attribute("lon"));
	        attributes.add(new Attribute("lat"));

	        //set instances
	        Instances datasets = new Instances("Test-dataset", attributes, 0);
	        //datasets.setClassIndex(datasets.numAttributes() - 1);
	        //add instance
	        for (Point p : entities) {
	            Instance instance = new DenseInstance(attributes.size());
	            instance.setValue(0,p.getlon());
	            instance.setValue(1,p.getlat());
	            datasets.add(instance);
	        }
	        return datasets;
	    }
	public double GetDistance(double lon1,double lat1, double lon2,double lat2) {
	    double s = Math.pow((lat1-lat2),2)+ Math.pow((lon1-lon2),2);
	    //s = new BigDecimal(s).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
	    return s;
	}
	public ArrayList<Point> cluster(Point[][] point,int numc) throws Exception{
			 //set attributes
		ArrayList<Point> temp=new ArrayList<Point>();
		for(Integer e:this.cur_sol.keySet()){
			for(DI e1:this.cur_sol.get(e)){
				temp.add(point[e1.d][e1.k]);
			}
		}
		Instances datasets=generatePopularInstance(temp);
		SimpleKMeans KM = null;
		
		KM = new SimpleKMeans();//
		KM.setNumClusters(numc);//

		KM.buildClusterer(datasets);
		ArrayList<Point> centers=new ArrayList<Point>();
		Instances cinstances = KM.getClusterCentroids();
		for ( int i = 0; i < cinstances.numInstances(); i++ ) {
			 // for each cluster center
			Instance inst = cinstances.instance(i);
			double value1 = inst.value(0);double value2 = inst.value(1);
			centers.add(new Point(i,value1,value2));
					}
		return centers;
				}
	public Set<Integer> elit_gas(Point[][] point,int enumber,int numc) throws Exception{
		ArrayList<Point> cp1=cluster(point,numc);
		ArrayList<Integer> elite=new ArrayList<Integer>();
		Set<Integer> elite1=new HashSet<Integer>();
		ArrayList<int[]> s1=new ArrayList<int[]>();
		int fcount=this.fac.size();
		for(Point e1:cp1){
			double[] arr = new double[fcount];
			for(int i1=0;i1<fcount;i1++){arr[i1]=this.GetDistance(e1.getlon(),e1.getlat(),this.fac.get(i1).location.getlon(),this.fac.get(i1).location.getlat());}
			int[] Index=new int[arr.length];
			Index=Arraysort(arr);
			s1.add(Index);
		}
		//for(int[] e2:s1){
		//	for(int e3:e2){System.err.print(e3);}
	 	//System.err.println();
		//}
		for(int k=0;k<3;k++){
		for(int[] e2:s1){
			if(elite1.size()<enumber){
				elite1.add(e2[k]);
			}
		}
		}
		return elite1;
		
	}
	public int[] Arraysort(double[]arr){
		double temp;
		int index;
		int k=arr.length;
		int[]Index= new int[k];
		for(int i=0;i<k;i++){Index[i]=i;}
		for(int i=0;i<arr.length;i++){
			for(int j=0;j<arr.length-i-1;j++){
				if(arr[j]>arr[j+1]){            
					temp = arr[j];            
					arr[j] = arr[j+1];            
					arr[j+1] = temp;
					index=Index[j];
					Index[j] = Index[j+1];            
					Index[j+1] = index;}}}
		return Index;
		}
			}
