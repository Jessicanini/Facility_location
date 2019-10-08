import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;


import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
public class Solution_L2 {
	
	public static double pi = 3.1415926535897932384626;
	public static double a = 6378245.0;
	public static double ee = 0.00669342162296594323;
	
	public static boolean E1(int a1, int b1,int a2,int b2){
		return !(a1==a2 && b1==b2);
	}
	public static boolean outOfChina(double lat, double lon) {
	    if (lon < 72.004 || lon > 137.8347) return true;
	    if (lat < 0.8293 || lat > 55.8271) return true;
	    return false;
	}
	private static Gps transform(double lat, double lon) {
	    if (outOfChina(lat, lon)) return new Gps(lat, lon);
	    double dLat = transformLat(lon - 105.0, lat - 35.0);
	    double dLon = transformLon(lon - 105.0, lat - 35.0);
	    double radLat = lat / 180.0 * pi;
	    double magic = Math.sin(radLat);
	    magic = 1 - ee * magic * magic;
	    double sqrtMagic = Math.sqrt(magic);
	    dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
	    dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
	    double mgLat = lat + dLat;
	    double mgLon = lon + dLon;
	    return new Gps(mgLat, mgLon);
	}
	private static double transformLat(double x, double y) {
	    double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
	            + 0.2 * Math.sqrt(Math.abs(x));
	    ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
	    ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
	    ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
	    return ret;
	}
	private static double transformLon(double x, double y) {
	    double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
	            * Math.sqrt(Math.abs(x));
	    ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
	    ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
	    ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
	    return ret;
	}
	private final static double EARTH_RADIUS = 6378.137;
	private static double rad(double d) {
	    return d * Math.PI / 180.0;
	}
	public static double interval_time(long t1,long t2){
        //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        Date date1 = new Date(t1);
        Date date2 = new Date(t2);
        long res=(date2.getTime()-date1.getTime());
        return res;
    }
	
	public static double GetDistance(double lat1, double lng1, double lat2, double lng2) {
	    double radLat1 = rad(lat1);
	    double radLat2 = rad(lat2);
	    double a = radLat1 - radLat2;
	    double b = rad(lng1) - rad(lng2);
	    double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
	            Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
	    s = s * EARTH_RADIUS*1000;
	    s = new BigDecimal(s).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
	    return s;
	}

	public static Gps gcj_To_Gps84(double lat, double lon) {
	    Gps gps = transform(lat, lon);
	    double lontitude = lon * 2 - gps.getWgLon();
	    double latitude = lat * 2 - gps.getWgLat();
	    return new Gps(latitude, lontitude);
	}
	
	public static double length(Point k1,Point k2){
		 double x1=k1.lat;
		 double y1=k1.lon;
		 double x2=k2.lat;
		 double y2=k2.lon;
		 Gps n1=gcj_To_Gps84(x1,y1);
		 Gps n2=gcj_To_Gps84(x2,y2);
		 double dis1=GetDistance(n1.a, n1.b, n2.a, n2.b);
		 return dis1; 	 
	}

	public static Point getPoint(int knum,double col1,Routine r1){
	    	Point p1=r1.start;
	    	Point p2=r1.end;
	    	double minlon;double maxlon;
	    	double minlat;double maxlat;
	    	minlon=Math.min(p1.lon,p2.lon);maxlon=Math.max(p1.lon,p2.lon);
	    	minlat=Math.min(p1.lat,p2.lat);maxlat=Math.max(p1.lat,p2.lat);
	    	double distance_lon = (maxlon - minlon)/col1;
	    	double distance_lat = (maxlat - minlat)/col1;
	    	return new Point((r1.start.time+r1.end.time)/2,minlon+knum*distance_lon,minlat+knum*distance_lat);
	    }  
	public static void main(String[] args) throws Exception{
		int C=1000;

		double opt=Double.parseDouble(args[0]);//7.4124247490e+06;//
		int a1=Integer.parseInt(args[1]);
    	int facility_count=Integer.parseInt(args[2]);
		int knumber=Integer.parseInt(args[3]);
		double col1=knumber;
		
        long startTime = System.currentTimeMillis();
        
       
    	//x i,k j
    	int aS =0;
    	
    	int driver_count=0;
    	int[] Fcap=new int[facility_count];
    	for(int i=0;i<(int)0.2*facility_count;i++){Fcap[i]=80;}
        for(int i=(int)0.2*facility_count;i<(int)0.7*facility_count;i++){Fcap[i]=120;}
        for(int i=(int)0.7*facility_count;i<facility_count;i++){Fcap[i]=150;}
    
        int rankF=1;
      	int initial_shuffle=20;
		int nbchangedri=3;
    	double iniper=0.4;
		//System.out.println(iniper);

    	
    	//SA 	public void Search1_SA(int per,int rankF,int maxad,int maxshuf,int r2_dc,double rate,double adjust,double initialT,int nbT,int nbIterationPerT,int maxcountTotal,int experimentTimes) 
    	int nbT = 200;
    	int nbIterationPerT = 5;
    	int maxcountTotal= 10;
    	int experimentTimes = 1;
    	double initialT=300;
    	double adjust=0.00;
    	double rate=0.97;

    	double col_row=100;
    	int penalty=15000;
    	int speed=25;//90KM/H
    	int setcost=5000;
    	int expertime=500;
       	
    	ArrayList<Double> lons=new ArrayList<Double>();
	    ArrayList<Double> lats=new ArrayList<Double>();
       	ArrayList<Driver> Drivers=new ArrayList<Driver>();
       	String prex="/Users/jessicanini/Desktop/c2";
       	prex=".";
    	Scanner in1 =new Scanner(Paths.get(prex+"/Drivers.txt"));
    	int aT=Integer.parseInt(in1.nextLine().split("\\s+")[0]);
    	for(int i=0;i<a1;i++){
    		ArrayList<Routine> routines=new ArrayList<Routine>();
    		String[] temp=in1.nextLine().split("\\s+");
    		for(int j=0;j<temp.length-5;j+=6){
    		String t1=temp[j];String t2=temp[j+1];
    		String t3=temp[j+2];String t4=temp[j+3];
    		String t5=temp[j+4];String t6=temp[j+5];
    		Point s1=new Point(Long.parseLong(t1),Double.parseDouble(t2),Double.parseDouble(t3));
    		Point e1=new Point(Long.parseLong(t4),Double.parseDouble(t5),Double.parseDouble(t6));
    		routines.add(new Routine(s1,e1));
    		lons.add(Double.parseDouble(t2));lons.add(Double.parseDouble(t5));
    		lats.add(Double.parseDouble(t3));lats.add(Double.parseDouble(t6));
    		}
    		if(!routines.isEmpty()){
    		Drivers.add(new Driver(routines));}
    	}
    	
    	driver_count=Drivers.size();
    	String filePathFac="./F50.txt";
    	Scanner in2 =new Scanner(Paths.get(filePathFac));
    	

    	ArrayList<Facility> facilities=new ArrayList<Facility>();
    	for(int i=0;i<facility_count;i++){
    		String name=in2.next();
    		//System.out.println(name);
    		double lon1=Double.parseDouble(in2.next());
    		double lat1=Double.parseDouble(in2.next());
    		facilities.add(new Facility(i,setcost,new Point(0,lon1,lat1)));
    	}  
    	in1.close();
    	in2.close();
    	System.out.println(facility_count);
    	System.out.printf("Driver count:%d",driver_count);
    	System.out.printf("Total Driver count:%d",aT);
    	System.out.println();
    	
    	//Korder
    	int kmax=0;
    	int[] K=new int[driver_count];
       
        for(int i=0;i<driver_count;i++){
    		K[i]=Drivers.get(i).routines.size()*knumber;
    		if (K[i]>kmax){kmax=K[i];}}
        int[][] Korder=new int[driver_count][kmax];
        
    	for(int i=0;i<driver_count;i++){
    		int w1=0;
    		K[i]=Drivers.get(i).routines.size()*knumber;
    		for(int j=0;j<K[i];j++){
    			Korder[i][j]=w1;
    			w1+=1;
    		}}
        double[][] penal=new double[driver_count][kmax];
    	for(int i=0;i<driver_count;i++){
			for(int j=0;j<K[i];j++){
    			penal[i][j]=penalty;}
			}
        Point[][] points=new Point[driver_count][kmax];
        for (int i = 0; i < driver_count; i++) {
            for (int j = 0; j < K[i]; j++) {
                    int knum=Math.floorMod(j, knumber);
                    Routine r1=Drivers.get(i).routines.get(Math.floorDiv(j, knumber));
                    points[i][j]=getPoint(knum,col1,r1);}}
      

    	System.out.printf(" Driver count:%d ",driver_count);System.out.printf("Model Driver count:%d ",Drivers.size());
	    System.out.printf(" Total Driver count:%d ",aT);System.out.printf("gas station:%d",facility_count);System.out.printf("Gas station:%d",facilities.size());
	    System.out.println();

	    //heuristic_L3 H1=new heuristic_L3(opt,expertime,setcost,penal,facility_count,driver_count,speed,Fcap,aS,K,knumber,Drivers,facilities,points);
	    heuristic_egas H1=new heuristic_egas(opt,expertime,setcost,penal,facility_count,driver_count,speed,Fcap,aS,K,knumber,Drivers,facilities,points);

	    H1.cal_djk();
		HashMap<Integer,ArrayList<Integer>> DF=H1.rank_djk(H1.dj);
		//260 279 355 589 611 626 665 669 
		//System.out.println(DF.get(611));System.out.println(DF.get(626));System.out.println(DF.get(665));System.out.println(DF.get(669));
	    //System.out.println(H1.djk[355][3][0]);
	    //Routine r1=H1.Drivers.get(355).routines.get(Math.floorDiv(0,knumber));
		//Point s11=r1.start;
		//Point e11=r1.end;
		//double dis1=length(s11,H1.Points[355][0])+length(H1.Points[355][0],H1.Facility.get(3).location)+length(H1.Facility.get(3).location,e11);
		//System.out.println(dis1);
		//System.out.println(interval_time(s11.time,e11.time)*speed);
	   
   		//for(int driver=1;driver<5;driver++){
   		//ArrayList<Integer> rF=DF.get(driver);//rF
   	
   		
   			
	     for (int i = 0; i < experimentTimes; i++){
	    	 System.out.println("Start: ");
	    	 H1.Search1_SA(iniper,initial_shuffle,nbchangedri,rankF,rate,adjust,initialT,nbT,nbIterationPerT,maxcountTotal,experimentTimes);
	    	 }
   
	    System.out.println();
	}
	}
	

