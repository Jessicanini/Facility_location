import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

//what if we already know (open facility set)
public class build_model1 {
	//Calculate the great circle distance between two points 
    //on the earth (specified in decimal degrees)
	public static double pi = 3.1415926535897932384626;
	public static double a = 6378245.0;
	public static double ee = 0.00669342162296594323;
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
	private final static double EARTH_RADIUS = 6378.137;//
	private static double rad(double d) {
	    return d * Math.PI / 180.0;
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

	public static Gps gcj02_To_Bd09(double gg_lat, double gg_lon) {
	    double x = gg_lon, y = gg_lat;
	    double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * pi);
	    double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * pi);
	    double bd_lon = z * Math.cos(theta) + 0.0065;
	    double bd_lat = z * Math.sin(theta) + 0.006;
	    return new Gps(bd_lat, bd_lon);
	}

	public static Gps bd09_To_Gcj02(double bd_lat, double bd_lon) {
	    double x = bd_lon - 0.0065, y = bd_lat - 0.006;
	    double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * pi);
	    double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * pi);
	    double gg_lon = z * Math.cos(theta);
	    double gg_lat = z * Math.sin(theta);
	    return new Gps(gg_lat, gg_lon);
	}

	public static Gps gps84_To_Gcj02(double lat, double lon) {
	    if (outOfChina(lat, lon)) {
	        return null;
	    }
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

	public static Gps gcj_To_Gps84(double lat, double lon) {
	    Gps gps = transform(lat, lon);
	    double lontitude = lon * 2 - gps.getWgLon();
	    double latitude = lat * 2 - gps.getWgLat();
	    return new Gps(latitude, lontitude);
	}
	
	public static double haversine(double lon1, double lat1, double lon2, double lat2){
  
    
    double dlon = Math.PI*lon2/180 - Math.PI*lon1/180;
    double dlat = Math.PI*lat2/180 - Math.PI*lat1/180;
    double a = Math.sin(dlat/2)*Math.sin(dlat/2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon/2)*Math.sin(dlon/2);
    double c = 2 * Math.asin(Math.sqrt(a));
    double r = 6371; 
    return c * r * 1000; 
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
	public static double interval_time(long t1,long t2){
        //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        Date date1 = new Date(t1);
        Date date2 = new Date(t2);
        long res=(date2.getTime()-date1.getTime());
        return res;
    }
	public static String getHour(long t1) {  
        Date currentTime = new Date(t1* 1000L);  
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
        String dateString = formatter.format(currentTime);  
        String hour;  
        hour = dateString.substring(11, 13);  
        return hour;  
    }  
      

    public static String getMinute(long t1) {  
        Date currentTime = new Date(t1*1000L);  
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
        String dateString = formatter.format(currentTime);  
        String min;  
        min = dateString.substring(17, 19);  
        return min;  
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
	    public static void main(String[] args) throws IOException, IloException {
	    	
	    	int C=10000;
	        long startTime = System.currentTimeMillis();   
	        int variable_count=0;
	        int st6_count=0;
	    	
	    	//x i,k j
	    	int aS =0;
	    	int a1=Integer.parseInt(args[0]);
	    	int facility_count=Integer.parseInt(args[1]);
	    	int knumber=Integer.parseInt(args[2]);
	    	double col1=knumber;
	    	System.out.println("Driver_count "+a1+" facility_count "+facility_count+" knumber "+knumber);
	    	int driver_count=0;
	    	int[] Fcap=new int[facility_count];

	    	for(int i=0;i<(int)0.2*facility_count;i++){Fcap[i]=80;}
	        for(int i=(int)0.2*facility_count;i<(int)0.7*facility_count;i++){Fcap[i]=120;}
	        for(int i=(int)0.7*facility_count;i<facility_count;i++){Fcap[i]=150;}
	    	
	    	double penalty=15000;
	    	double per=0.4;
	    	int speed=25;//90KM/H
	    	double setcost=5000;
	    	int st_count=0;
	       	
	    	ArrayList<Double> lons=new ArrayList<Double>();
		    ArrayList<Double> lats=new ArrayList<Double>();
	       	ArrayList<Driver> Drivers=new ArrayList<Driver>();
	    	Scanner in1 =new Scanner(Paths.get("./Drivers.txt"));
	    	int aT=Integer.parseInt(in1.nextLine().split("\\s+")[0]);
	    	for(int i=0;i<a1;i++){
	    		ArrayList<Routine> routines=new ArrayList<Routine>();
	    		String[] temp=in1.nextLine().split("\\s+");
	    		ArrayList<Long> times=new ArrayList<Long>();
	    		for(int j=0;j<temp.length-5;j+=6){
	    		String t1=temp[j];String t2=temp[j+1];
	    		String t3=temp[j+2];String t4=temp[j+3];
	    		String t5=temp[j+4];String t6=temp[j+5];
	    		Point s1=new Point(Long.parseLong(t1),Double.parseDouble(t2),Double.parseDouble(t3));
	    		Point e1=new Point(Long.parseLong(t4),Double.parseDouble(t5),Double.parseDouble(t6));
	    		times.add(Long.parseLong(t1));
	    		times.add(Long.parseLong(t4));
	    		routines.add(new Routine(s1,e1));
	    		lons.add(Double.parseDouble(t2));lons.add(Double.parseDouble(t5));
	    		lats.add(Double.parseDouble(t3));lats.add(Double.parseDouble(t6));
	    		}
	    		for(int i1=0;i1<times.size()-1;i1++){
	    			if(times.get(i1+1)<times.get(i1)){
	    				System.err.println("time error");
	    				System.err.println(i);
	    				System.err.println(times.get(i1+1));}}
	    		if(!routines.isEmpty()){
	    		Drivers.add(new Driver(routines));}
	    	}
	    	
	    	driver_count=Drivers.size();
	    	
	    	//String filePathFac="/Users/jessicanini/Desktop/c2/chengdu/stations-chengdu.csv";
	    	//Scanner in2 =new Scanner(Paths.get(filePathFac));
	    	
	    	String filePathFac="./F50.txt";
	    	Scanner in2 =new Scanner(Paths.get(filePathFac));
	    	
	    	String filePathFac1="./facility/"+"lpF"+facility_count+"D"+a1+"k"+knumber+".txt";
	    	FileWriter fw1 = new FileWriter(filePathFac1);
	    	BufferedWriter bw1 = new BufferedWriter(fw1);
	    	
	    	ArrayList<Facility> facilities=new ArrayList<Facility>();
	    	for(int i=0;i<facility_count;i++){
	    		String[] temp=in2.nextLine().split(" ");
	    		//String name=temp[0];System.out.println(temp[2]);
	    		System.out.println(i+" "+temp[0]+" "+temp[1]+" "+temp[2]); 
	    		double lon1=Double.parseDouble(temp[1]);
	    		double lat1=Double.parseDouble(temp[2]);
	    		/*double x = lon1 - 0.0065, y = lat1 - 0.006;
	    	    double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * pi);
	    	    double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * pi);
	    	    double gg_lon = z * Math.cos(theta);
	    	    double gg_lat = z * Math.sin(theta);*/
	    		facilities.add(new Facility(i,setcost,new Point(0,lon1,lat1)));
	    		bw1.write("GASINDEX"+String.valueOf(i)+" "+String.valueOf(lon1)+" "+String.valueOf(lat1)+'\r');
	    	}
	    	bw1.flush();
	    	bw1.close();
	    	fw1.close();
	    	in1.close();
	    	in2.close();
	    	System.out.println(facility_count);
	    	System.out.printf("driver count:%d",driver_count);
	    	System.out.printf("total driver:%d",aT);
	    	System.out.println();
	    	
	    	assert false;
	    	//Korder
	    	int kmax=0;
	    	int[] K=new int[driver_count];
	        int[][] Korder=new int[driver_count][C];
	       
	    	for(int i=0;i<driver_count;i++){
	    		int w1=0;
	    		K[i]=Drivers.get(i).routines.size()*knumber;
	    		if (K[i]>kmax){kmax=K[i];}
	    		for(int j=0;j<K[i];j++){
	    			//System.out.println(temp[j]);
	    			Korder[i][j]=w1;
	    			w1+=1;
	    		}}
	    	
	    	
	    			
	    	
	    	IloCplex cplex = new IloCplex();
	        IloIntVar[][][] x = new IloIntVar[driver_count][kmax][facility_count];
	        //zi.k
	        IloIntVar[][] s = new IloIntVar[driver_count][kmax];
	        IloIntVar[] z = new IloIntVar[driver_count];
	        IloIntVar[] y=new IloIntVar[facility_count];
	        double maxDistance=0;
	        ArrayList<Double> dis=new ArrayList<Double>();
	        Point[][] points=new Point[driver_count][kmax];
	         // ---build model---//
	         IloLinearNumExpr exprObj1 = cplex.linearNumExpr();
	         for (int i = 0; i < driver_count; i++) {
	             for (int j = 0; j < K[i]; j++) {
	            	 for(int p=0;p<facility_count;p++){
	            		 int knum=Math.floorMod(j, knumber);
	                     Routine r1=Drivers.get(i).routines.get(Math.floorDiv(j, knumber));
	        			 Point s11=r1.start;
	        			 Point e11=r1.end;
	        			 points[i][j]=getPoint(knum,col1,r1);
	        			 double dis1=length(points[i][j],facilities.get(p).location)+length(facilities.get(p).location,e11);
	        			 //if(i==33 && p==17){System.out.printf(":%d,:%f", j,dis1);}
	                     x[i][j][p] = cplex.intVar(0, 1, "x" + i + "," + j + "," + p);
	                     variable_count+=1; //dis.add(length(getPoint(knum,knumber,r1),facilities.get(p).location));
	                     exprObj1.addTerm(dis1,x[i][j][p]);
	                     //exprObj1.addTerm(length(points[i][j],facilities.get(p).location),x[i][j][p]);
	                 }}}
	         
		    /*String filePathFac2="/Users/jessicanini/Desktop/c2/small/point1/"+"F"+facility_count+"D"+driver_count+"k"+knumber+".txt";
		    FileWriter fw2 = new FileWriter(filePathFac2);
		    BufferedWriter bw2 = new BufferedWriter(fw2);
		    for (int i = 0; i < driver_count; i++) {
	             for (int j = 0; j < K[i]; j++) {
		    	}}
		    bw2.flush();bw2.close();fw2.close();*/

	         System.out.println(variable_count);
	         IloLinearNumExpr exprObj2 = cplex.linearNumExpr();
	         for (int i = 0; i < driver_count; i++){
	        	 z[i] = cplex.intVar(0, 1, "z" + i );
	        	 variable_count+=1;
	             exprObj2.addTerm(penalty,z[i]);
	             }
	         System.out.println(variable_count);
	         
	         IloLinearNumExpr exprObj3 = cplex.linearNumExpr();
	         for (int i = 0; i < facility_count; i++) {
	        	 y[i] = cplex.intVar(0, 1, "y" + i );
	             exprObj3.addTerm(setcost,y[i]);
	             variable_count+=1;
	             }
	          cplex.addMinimize(cplex.sum(exprObj1,exprObj2,exprObj3));
		    
	          System.out.println(variable_count);
	          System.out.println("Start constraint");
	         
	       // ---assignment constraint 1 & 2---//
		         for (int i = 0; i < driver_count; i++) {
		             IloLinearIntExpr constraint1 = cplex.linearIntExpr();
		             for (int j = 0; j < K[i]; j++){
		            	 for(int p=0;p<facility_count;p++){
		            		 constraint1.addTerm(1, x[i][j][p]);
		            		 //cplex.addLe(cplex.sum(x[i][j][p],cplex.prod(-1.0,y[p])), 0);
		             } 
		             }
		             constraint1.addTerm(1, z[i]);
		             cplex.addEq(constraint1,1);
		             st_count+=1;
		             }
		         System.out.println(st_count);
	      // ---assignment constraint  2---//
	         for(int p=0;p<facility_count;p++){
	        	 IloLinearIntExpr constraintc = cplex.linearIntExpr();
	        	 int pp=0;
	        	 constraintc.addTerm(-1000000000, y[p]);
	             for (int i = 0; i < driver_count; i++) {
	                 for (int j = 0; j < K[i]; j++){
	            	     constraintc.addTerm(1, x[i][j][p]);
	            	     pp+=1;}
	                 }
	             cplex.addLe(constraintc,0);
	             st_count+=1;
	             }
	         System.out.println(st_count);
	         
	         // ---time constraint 3---//
	         for (int i = 0; i < driver_count; i++){
	        	 for (int j = 0; j < K[i]; j++){
	        		 for(int p=0;p<facility_count;p++){
	        			 int knum=Math.floorMod(j, knumber);
	                     Routine r1=Drivers.get(i).routines.get(Math.floorDiv(j, knumber));
	        			 Point s11=r1.start;
	        			 Point e11=r1.end;
	        			 double dis1=length(s11,points[i][j])+length(points[i][j],facilities.get(p).location)+length(facilities.get(p).location,e11);
          		         //System.out.print(dis1);//System.out.println(" ");
	        			 if(interval_time(s11.time,e11.time)<=0){
	        				 System.err.println();
	        				 System.err.print(r1.start.time);
	        				 System.err.println();
	        			 }
          		         cplex.addLe(cplex.prod(dis1, x[i][j][p]), speed*interval_time(s11.time,e11.time)); 
          		st_count+=1;// c3---time}
           } } }
	         System.out.println(st_count);
	         ArrayList<ArrayList<Point1>> Hours=new ArrayList<ArrayList<Point1>>();
	         for(int i=0;i<24;i++){Hours.add(new ArrayList<Point1>());}
	         for (int i = 0; i < driver_count; i++){
	        	 for (int j = 0; j < K[i]; j++){
	        		 int h=Integer.parseInt(getHour(points[i][j].time));
	        		 Hours.get(h).add(new Point1(i,j,points[i][j]));
	         }}
	        
	         
	         // ---capacity constraint interval---//
	         for(int p=0;p<facility_count;p++){
	        	 for(int i=0;i<24;i++){
	        		IloLinearIntExpr constraint2 = cplex.linearIntExpr();
	        		for(Point1 p1:Hours.get(i)){
	        			constraint2.addTerm(1, x[p1.i1][p1.j1][p]);}
	        		cplex.addLe(constraint2, Fcap[p]); 
	        		st_count+=1;
	        		}
	        		}
	        
	       System.out.println(st_count);
	       // ---open how much facility constraint---//
	       IloLinearIntExpr constraint3 = cplex.linearIntExpr();
	        for(int p=0;p<facility_count;p++){
	        	constraint3.addTerm(1, y[p]);} 
	        	cplex.addLe(constraint3, facility_count*per);
	        	st_count+=1;
	        	System.out.println(st_count);
	          
	        // --- constraint 5&relationship of s,i,k and z,i---//	       
	       /* for (int i = 0; i < a4; i++) {
	             IloLinearIntExpr constraint4 = cplex.linearIntExpr();
	             for (int j = 0; j < K[i+aS]; j++){
	            	 constraint4.addTerm(1, s[i][j]);} 
	             constraint4.addTerm(-1, z[i]);
	             cplex.addEq(constraint4, 0); 
	             st_count+=1;
	            }
	        System.out.println(st_count);*/
	        
	        //constraint 6&max distance---//	 
	         //IloLinearIntExpr constraint5 = cplex.linearIntExpr();
	       /* for (int i = 0; i < driver_count; i++) {
	             for (int j = 0; j < K[i]; j++) {
	            	 for(int p=0;p<facility_count;p++){
	                     st_count+=1;
	                     double dis2=length(points[i][j],facilities.get(p).location);
						cplex.addLe(cplex.prod(dis2, x[i][j][p]), maxDistance);}}}
	         System.out.println(st_count);*/

	 	    cplex.exportModel("./"+"F"+facility_count+"D"+driver_count+"K"+knumber+".lp");
	 	    System.out.println("finish build");
	 	    System.out.println("start solve");
	 	  /*  try {
	 		      cplex.solve();
	 		      double obj=cplex.getObjValue();
	 		      //System.out.println("The optimal objValue is "+String.valueOf(obj));
	 		      boolean feasible=cplex.solve();
	 		      if(feasible){
	 		    	  System.err.println(obj);
	 		      }
	 		      }
	 	   catch (IloException ex) {
	 		      System.err.println("\n!!!Unable to solve the model:\n"
	 		                         + ex.getMessage() + "\n!!!");
	 		      System.exit(2);
	 		    } */
	 	 
	    }
	    }
