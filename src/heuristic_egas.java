import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class heuristic_egas {
	
	public static int  C=1000;
	public static double pi = 3.1415926535897932384626;
	public static double a = 6378245.0;
	public static double ee = 0.00669342162296594323;
	
	FileWriter fw;
    BufferedWriter bw;
    FileWriter fw1;
    BufferedWriter bw1;
    FileWriter fw2;
    BufferedWriter bw2;
    FileWriter fw3;
    BufferedWriter bw3;
    FileWriter fw_g;
    BufferedWriter bw_g;
    FileWriter fw_s;
    BufferedWriter bw_s;

    ArrayList<Integer> driver_order=new ArrayList<Integer>();
	//HashMap<DIF,Double> curPair=new HashMap<DIF,Double>();
	ArrayList<Integer> curdoDriver=new ArrayList<Integer>();
	ArrayList<Integer> curundoDriver=new ArrayList<Integer>();
	HashMap<Integer,ArrayList<DI>> curFacSol=new HashMap<Integer,ArrayList<DI>>();
	Set<Integer> curOpenset = new HashSet<Integer>();
	Set<Integer> curCloseset = new HashSet<Integer>();
	double curcost=0;
	
	//HashMap<DIF,Double> neighPair=new HashMap<DIF,Double>();
	ArrayList<Integer> neighdoDriver=new ArrayList<Integer>();
	ArrayList<Integer> neighundoDriver=new ArrayList<Integer>();
	HashMap<Integer,ArrayList<DI>> neighFacSol=new HashMap<Integer,ArrayList<DI>>();
	Set<Integer> neighOpenset = new HashSet<Integer>();
	Set<Integer> neighCloseset = new HashSet<Integer>();
	double neighcost=0;
	
	//HashMap<DIF,Double> bestPair=new HashMap<DIF,Double>();
	ArrayList<Integer> bestdoDriver=new ArrayList<Integer>();
	ArrayList<Integer> bestundoDriver=new ArrayList<Integer>();
	HashMap<Integer,ArrayList<DI>> bestFacSol=new HashMap<Integer,ArrayList<DI>>();
	Set<Integer> bestOpenset = new HashSet<Integer>();
	Set<Integer> bestCloseset = new HashSet<Integer>();
	double bestcost=100000000;
	
	double knownopt=0;
	Set<Integer> TOpenset = new HashSet<Integer>();
	
	ArrayList<Driver> Drivers;
	ArrayList<Facility> Facility;
	
	int aS=0;
	int[] Cap;
	int speed;
	int expertime=0;
	int set_cost;
	double[][] penalty;
	
	int[] K;
	Point[][] Points;
	
	int facilitycount;
	int drivercount;
	
	int knumber;
	
	double[][][] djk;
	double[][] dj;
	int[][] dj_k;


	public heuristic_egas(double opt,int etimes,int scost,double[][] pen,int fc,int dc,int spe,int[] cap,int as,int[] k,int knumber,ArrayList<Driver> d1,ArrayList<Facility> f1,Point[][] p1) throws IOException{
		this.knownopt=opt;
		this.expertime=etimes;
		this.set_cost=scost;
		this.penalty=pen;
		this.Drivers=d1;
		this.Facility=f1;
		this.K=k;
		this.aS=as;
		this.Cap=cap;
		this.speed=spe;
		this.drivercount=dc;
		this.facilitycount=fc;
		this.knumber=knumber;
		this.Points=p1;
		for(int i=0;i<this.facilitycount;i++){
			ArrayList<DI> temp=new ArrayList<DI>();
			curFacSol.put(i,temp);
			bestFacSol.put(i,temp);
			neighFacSol.put(i,temp);
		}
		String filePath0="./logFac_"+this.facilitycount+"D"+this.drivercount+".txt";
		fw = new FileWriter(filePath0);bw = new BufferedWriter(fw);
		
		}
	public void write_fac(BufferedWriter bufwri,Set<Integer> openfac, int f1) throws IOException{
		bufwri.write("gas station:"+String.valueOf(f1)+"\n");
		for(Integer f:openfac){
			bufwri.write(String.valueOf(f)+" ");}
		bufwri.write("Finish"+"\n");
		bufwri.flush();
		}

	public boolean timeFeasible(int i,int k,int f){
		// ---time constraint ---//
        Routine r1=this.Drivers.get(i).routines.get(Math.floorDiv(k,this.knumber));
		Point s11=r1.start;
		Point e11=r1.end;
		double dis1=length(s11,this.Points[i][k])+length(this.Points[i][k],this.Facility.get(f).location)+length(this.Facility.get(f).location,e11);
        if(dis1<=speed*interval_time(s11.time,e11.time)){
        	return true;}
        else{return false;}
        }
	
	
	public void cal_djk(){
		int kmax=0;
		for(int a1=0;a1<this.K.length;a1++){if(K[a1]>kmax){kmax=K[a1];}}
		dj=new double[this.drivercount][this.facilitycount];
		dj_k=new int[this.drivercount][this.facilitycount];
		for(int i=0;i<this.drivercount;i++){
			for(int j=0;j<this.facilitycount;j++){
				dj[i][j]=this.penalty[i][0];
				}}
		//System.out.println();
		for(int i=0;i<this.drivercount;i++){
			for(int j=0;j<this.facilitycount;j++){
				double tempcost=0;
				double mincost=10000000;
				int mink=-1;int minf=-1;
				for(int k=0;k<this.K[i];k++){
					if(timeFeasible(i,k,j)){
						 Routine r1=this.Drivers.get(i).routines.get(Math.floorDiv(k,this.knumber));
						 Point e11=r1.end;
						 tempcost=length(this.Points[i][k],this.Facility.get(j).location)+length(this.Facility.get(j).location,e11);
						 //tempcost = length(this.Points[i][k],Facility.get(j).location);
						if(tempcost<mincost){mincost=tempcost;mink=k;minf=j;}
						if(mincost==this.penalty[i][0]){
							System.out.println("cost equal penalty");
						};
						}
				}// for k
				if(mink!=-1){
				//djk[i][j][mink]=Math.min(mincost,djk[i][j][mink]);
				dj_k[i][j]=mink;
				dj[i][j]=Math.min(mincost,dj[i][j]);
				}
				}//for j
			//if(i==33){
				//for(int j=0;j<20;j++){
				//System.out.println(dj[i][j]);}}
			}// for i
		}
	
	public HashMap<Integer,ArrayList<Integer>> rank_djk(double[][] dj2){
		HashMap<Integer,ArrayList<Integer>> DF=new HashMap<Integer,ArrayList<Integer>>();
		for(int i1=0;i1<this.drivercount;i1++){
		double[] arr = new double[this.facilitycount];
		for(int i2=0;i2<this.facilitycount;i2++){arr[i2]=dj2[i1][i2];}
		int[] Index=new int[arr.length];
		Index=Arraysort(arr);
		ArrayList<Integer> t1=new ArrayList<Integer>(); 
	    for(int i3=0;i3<this.facilitycount;i3++){
	    	//if(i1==33){System.out.printf(":%d,:%f",Index[i3],dj2[i1][Index[i3]]);System.out.println(" ");}
	    	if(dj2[i1][Index[i3]]==this.penalty[i1][0]){t1.add(-1);}
	    	else{t1.add(Index[i3]);}
	    	}
	    	DF.put(i1, t1);
		}
	    return DF;
		}

	public HashMap<Integer,ArrayList<DI>> reasign_f_d(HashMap<Integer,ArrayList<Integer>> DF,Set<Integer> openf1,ArrayList<Integer> d,int rankF){
		
		HashMap<Integer,ArrayList<DI>> temp1=new HashMap<Integer,ArrayList<DI>>();
		for(Integer e2:openf1){temp1.put(e2, new ArrayList<DI>());}

		int[][] fcap=new int[this.facilitycount][24];
		for(int q=0;q<this.facilitycount;q++){
			for(int i=0;i<24;i++){fcap[q][i]=Cap[q];}}
		
   		for(int i1=0;i1<this.drivercount;i1++){
   			int driver=d.get(i1);
   			ArrayList<Integer> rF=DF.get(driver);//rF
   			boolean accept2=false;
   			int i3=0;
   			//if(new Random().nextBoolean()){i3=new Random().nextInt(rankF);}
   			while(i3<rF.size() && !accept2){
   				int nj=rF.get(i3);
   				if(nj!=-1){
   				int nk=dj_k[driver][rF.get(i3)];
   				int ntime=get_time(driver,nk);
   				if(fcap[nj][ntime]>0 && openf1.contains(nj)){
   					fcap[nj][ntime]-=1;
   					temp1.get(nj).add(new DI(driver,nk));
   					accept2=true;}}
   			    	i3+=1;
   				}//for while 
   			}//for for 
   		return temp1;
		}// for whole function
	
	 public int get_time(int driver, int nk){
		int h=Integer.parseInt(getHour(this.Points[driver][nk].time));
		return h;
	}

	
	 public HashMap<Integer, ArrayList<DI>> reasign_mix(Set<Integer> openfac,HashMap<Integer,ArrayList<DI>> s1,HashMap<Integer, ArrayList<Integer>> DF,double per,int initial_shuffle,int nbchangedri,int rankF) throws IOException{
		
		 ArrayList<Integer> d1=new ArrayList<Integer>();
		 for(int i=0;i<this.drivercount;i++){d1.add(i);}
		 Collections.shuffle(d1);
		 HashMap<Integer, ArrayList<DI>> t1=new HashMap<Integer, ArrayList<DI>>();
		 t1=this.reasign_f_d(DF, openfac, d1,1);
		 double cost1=this.cal_cost(t1);
		 int count0=0;
		 while(count0<initial_shuffle){
			 count0+=1;
			 HashMap<Integer,ArrayList<DI>> t11=new HashMap<Integer,ArrayList<DI>>();
			 for(int i=0;i<this.facilitycount;i++){t11.put(i, new ArrayList<DI>());}
	  		 ArrayList<Integer> t21=new ArrayList<Integer>();
	  		 for(int i=0;i<this.drivercount;i++){t21.add(i);}
	  		 Collections.shuffle(t21);
	  		 ArrayList<Integer> driver_order1=new ArrayList<Integer>();
	  		 for(int i=0;i<this.drivercount;i++){driver_order1.add(t21.get(i));}
	  		 t11=this.reasign_f_d(DF, openfac, driver_order1,1);
	  		 
	  		 double c11=this.cal_cost(t11);
	  		 if(larger(cost1,c11)){t1=this.copy_F(t11);cost1=c11;
	  		 System.out.println(count0);
	  		 System.out.println("initial solution improve");
	  		 System.out.println(cost1);
	  		 d1 = driver_order1;
	  		 //this.TOpenset=new HashSet<Integer>();for(Integer f:openfac){this.TOpenset.add(f);}
	  		 }
	  		    }
		
		 int count1=0;
		 HashMap<Integer, ArrayList<DI>> temp2=new HashMap<Integer, ArrayList<DI>>();
	     //---change the driver order ---//
		 while(count1<nbchangedri){
			 count1+=1;
			 ArrayList<Integer> opendri=new ArrayList<Integer>();
			 int[] point=generate_index();
			 int[] a1=this.new_order(point, a_to_i(d1));
			 opendri=this.i_to_a(a1);
			 temp2=new HashMap<Integer,ArrayList<DI>>();
			 temp2=this.reasign_f_d(DF, openfac, opendri,rankF);
			 double c2=this.cal_cost(temp2);
			 if(larger(cost1,c2)){
				 t1=this.copy_F(temp2);cost1=c2;
				 d1=this.copy_driver(opendri);
				 //this.TOpenset=new HashSet<Integer>();for(Integer f:openfac){this.TOpenset.add(f);}
			     }
		 }
		 return t1;
	 }
	 public HashMap<Integer, ArrayList<DI>> reasign_mix1(Set<Integer> of1,HashMap<Integer,ArrayList<DI>> s1,HashMap<Integer, ArrayList<Integer>> DF,double per,int initial_shuffle,int nbchangedri,int rankF) throws Exception{
		 //System.out.println("disturb gas station");
		 Set<Integer> openf1=new HashSet<Integer>();
		 openf1=this.disturb(3, s1);
		 //for(Integer e:of1){
	    		//openf1.add(e);}
		 HashMap<Integer, ArrayList<DI>> t1=new HashMap<Integer, ArrayList<DI>>();
		 t1=this.copy_F(s1);
		 double cost1=this.cal_cost(t1);boolean mark1=false;int count1=0;
			while(!mark1 && count1<50){
				int f1=new Random().nextInt(5);
				 Set<Integer> openfac=get_newFac(openf1,f1,t1);
				 write_fac(this.bw,openfac,count1);
				 HashMap<Integer, ArrayList<DI>> temp1=new HashMap<Integer, ArrayList<DI>>();
				 temp1=this.reasign_mix(openfac, s1, DF, per, initial_shuffle, nbchangedri, rankF);
				// System.out.println(temp1);
				 double cost2=this.cal_cost(temp1);count1+=1;
				 if(larger(cost1,cost2)){
					// System.out.print("p");
					 t1=this.copy_F(temp1);cost1=cost2;mark1=true;
					 this.TOpenset=new HashSet<Integer>();for(Integer f:openfac){this.TOpenset.add(f);}
				     }
				else{
					//System.out.print("-");
				}
					
			 }
			return t1;
		}
	 public Set<Integer> get_newFac(Set<Integer> originf,int f1, HashMap<Integer, ArrayList<DI>> t1) throws Exception{
			Set<Integer> openf1=new HashSet<Integer>();
			
			if(f1==0){openf1=open_close_facility1(originf);}
		    if(f1==1){openf1=open2_close2_facility(originf);}
		    if(f1==2){openf1=open3_close3_facility(originf);}
		    if(f1==3){openf1=openr_facility();}
		    if(f1==4){
		    	for(Integer e:originf){
		    		openf1.add(e);}
		    	}
		   
			return openf1;
			}
	public Set<Integer> new_facset(Set<Integer> openfac,int a,int b){
		Set<Integer> res=new HashSet<Integer>();
		int[] opens=new int[(int) (this.facilitycount*0.4)];int i1=0;
		int[] closes=new int[(int) (this.facilitycount*0.6)];int i2=0;
		for(int i=0;i<this.facilitycount;i++){
			if(openfac.contains(i)){opens[i1]=i;i1+=1;}
			if(!openfac.contains(i)){closes[i2]=i;i2+=1;}
		}
		for(int j1=0;j1<i1;j1++){if(j1!=a){res.add(opens[j1]);}}
		for(int j2=0;j2<i2;j2++){if(j2==b){res.add(closes[j2]);}}
		return res;
	}
	public Set<Integer> open_close_facility1(Set<Integer> P){
		Set<Integer> res=new HashSet<Integer>();
		for(Integer fc:P){res.add(fc);}
		int openf;int closef;
		boolean flag=false;
		while(!flag){
		Random r1=new Random();Random r2=new Random();
		openf=r1.nextInt(this.facilitycount);closef=r2.nextInt(this.facilitycount);
		if(!P.contains(openf) && P.contains(closef)){
			flag=true;
			res.add(openf);
			res.remove(closef);}
		}
		return res;
	}
   public Set<Integer> disturb(int fnumber,HashMap<Integer,ArrayList<DI>> sol) throws Exception{
		Set<Integer> openf1=new HashSet<Integer>();
		Set<Integer> originf=new HashSet<Integer>();
		originf=this.copy_fac(this.TOpenset);
		int f1=new Random().nextInt(fnumber);
		KM1 km1=new KM1(this.Facility,sol);
		int enumber=(int) (this.facilitycount*0.4);
		int numc=enumber+2;
		//System.err.println(enumber);
		Set<Integer> elit_gas=km1.elit_gas(this.Points, enumber,numc);
		if(f1==0){openf1=open_close_facility(originf,elit_gas,0.8);}
		if(f1==1){openf1=open2_close2_facility(originf);}
		if(f1==2){openf1=open3_close3_facility(originf);}
		if(f1==3){openf1=openr_facility();}//for(Integer e:openf1){System.err.println(e);}//
			return openf1;
			}
   private Set<Integer> copy_fac(Set<Integer> openfac) {
	    Set<Integer> res=new HashSet<Integer>();
		for(Integer e:openfac){
			res.add(e);
		}
		return res;
	}
  	public HashMap<Integer,ArrayList<DI>> initial_sol(HashMap<Integer, ArrayList<Integer>> DF,double per,int initial_shuffle) throws IOException{
  		HashMap<Integer,ArrayList<DI>> t1=new HashMap<Integer,ArrayList<DI>>();
  		for(int i=0;i<this.facilitycount;i++){t1.put(i, new ArrayList<DI>());}
  		int k=(int) (this.facilitycount*per);
  		System.out.printf("initial gas station count:%d",k);
	    //System.out.println(" ");
  		
	    Set<Integer> open1=new HashSet<Integer>();
  	    while(open1.size()<k){open1.add(new Random().nextInt(this.facilitycount));}
        
        this.TOpenset=new HashSet<Integer>();
        for(Integer f:open1){this.TOpenset.add(f);}
        
       
  		/*Set<Integer> open1=new HashSet<Integer>();
  		int[] F=new int[]{7,10,15,17,19,21,22,23,25,26,28,29,32,35,36,38};
  		for(int i=0;i<F.length;i++){open1.add(F[i]);}
	    System.out.println(open1);*/
  		
  		//write_fac(this.bw,open1,k);
  		ArrayList<Integer> t2=new ArrayList<Integer>();
  		for(int i=0;i<this.drivercount;i++){t2.add(i);}
  		//Collections.shuffle(t2);
  		for(int i=0;i<this.drivercount;i++){driver_order.add(t2.get(i));}
  		t1=this.reasign_f_d(DF, open1, driver_order,1);
  		//int[] D=new int[]{17,93,123,142,143,212,252,260,286,436,501,526,626,665,723,754,785,814,840,849,907,957,963,1055,1072,1097,1102,1126,1180,1186,11,67,85,139,154,215,222,244,256,268,306,337,346,401,435,444,517,678,693,728,865,878,887,964,1037,1061,1158,1168,1179,1192,29,52,56,80,103,135,169,223,280,293,318,331,407,494,496,580,601,703,801,809,902,913,932,942,962,969,1005,1080,1159,1189,3,57,230,287,323,403,406,489,512,514,515,557,566,581,583,653,667,695,770,800,825,842,925,998,1020,1021,1068,1115,1128,1131,63,94,125,157,167,325,328,414,431,443,447,537,613,633,642,733,748,802,824,900,936,1030,1117,1138,1146,1161,1188,1195,1197,1198,18,177,213,229,289,386,416,419,429,499,518,525,535,604,623,645,651,734,753,758,766,803,819,941,956,1073,1075,1088,1164,1182,98,176,178,218,299,334,340,364,374,382,385,392,404,408,442,452,483,488,592,617,634,637,672,675,687,884,927,1025,1083,1124,68,74,129,132,162,201,205,231,271,297,502,546,658,730,768,810,863,896,923,948,966,994,996,1022,1031,1035,1091,1112,1167,1172,10,35,82,88,113,124,172,187,193,214,343,354,373,393,405,420,460,556,647,868,876,951,973,975,1039,1046,1060,1135,1147,1176,22,25,45,69,72,95,424,478,511,533,584,600,615,616,654,735,750,839,877,885,890,922,1038,1040,1043,1062,1084,1101,1110,1171,36,40,58,127,250,317,439,449,451,492,503,579,598,627,689,690,715,760,848,881,889,915,940,947,984,991,1019,1028,1123,1193,64,83,156,174,245,353,417,422,464,567,572,590,599,614,648,659,664,721,731,751,783,798,908,952,960,965,979,987,1144,1155,0,23,30,44,48,160,184,199,206,326,341,379,394,415,428,440,473,476,522,575,619,625,662,706,713,732,859,882,985,989,5,38,71,84,122,145,277,441,465,520,588,594,603,612,638,655,724,738,759,771,777,784,867,919,943,950,997,1065,1069,1111,61,96,147,159,249,273,321,361,371,381,391,484,491,500,542,545,550,558,610,644,805,834,841,855,874,995,1001,1033,1103,1107,53,55,115,141,165,210,227,259,283,295,339,349,376,389,395,437,468,663,670,674,717,737,782,831,843,844,968,1070,1095,1125,120,128,130,131,207,251,303,309,423,456,508,534,539,564,565,641,652,671,743,745,788,793,797,873,901,918,945,1122,1166,1190,6,19,47,133,148,208,253,294,327,569,657,681,696,702,736,756,813,832,836,861,928,929,1002,1045,1056,1132,1134,1165,1175,1185,28,41,86,89,106,117,121,211,304,359,467,471,472,513,585,622,650,699,714,739,806,850,860,1016,1082,1094,1100,1113,1121,1133,20,91,116,168,191,305,378,380,434,477,481,482,577,660,666,707,709,776,780,815,821,829,854,880,906,944,971,1012,1071,1086,4,12,54,73,197,198,228,232,233,234,243,319,480,485,505,570,628,684,686,697,763,845,909,931,946,981,1042,1093,1157,1170,9,46,70,78,87,118,171,247,276,288,300,316,338,370,418,448,530,538,571,620,694,749,764,765,773,847,852,930,1013,1127,79,126,182,224,246,263,265,296,311,336,367,399,469,490,529,549,552,576,762,789,799,811,816,823,851,898,1076,1078,1098,1140,101,137,146,179,217,248,254,262,345,372,413,427,430,462,498,521,547,551,582,636,778,791,864,949,954,1017,1018,1118,1119,1137,39,43,105,150,152,153,242,284,310,314,356,357,366,369,446,495,510,536,609,631,700,716,752,755,838,939,953,992,1099,1162,32,33,50,76,190,219,221,226,264,267,298,308,324,433,531,602,624,643,698,744,869,934,972,1014,1027,1032,1109,1129,1141,1199,60,100,102,112,114,134,196,261,291,320,362,398,411,421,426,493,524,553,596,611,668,718,786,792,926,1000,1015,1120,1150,1163,31,92,104,108,136,138,158,181,220,235,290,292,347,351,355,388,457,474,574,606,673,688,794,822,897,1004,1063,1104,1142,1178,34,75,237,238,270,274,281,302,383,470,527,555,563,656,719,725,742,790,808,837,872,875,879,904,921,1026,1044,1081,1116,1143,8,13,99,119,164,185,189,255,315,322,329,375,390,410,412,487,586,679,769,779,817,853,910,933,955,976,999,1049,1177,1196,21,155,200,335,360,400,461,466,548,561,595,607,635,692,820,830,883,888,895,911,916,938,977,1007,1023,1029,1057,1079,1114,1183,7,16,42,66,151,166,209,239,257,269,272,278,358,397,453,516,568,589,669,741,787,795,862,871,1024,1050,1059,1108,1187,1191,2,27,109,173,180,188,194,204,258,333,458,459,507,621,640,649,682,774,796,807,827,961,974,1011,1053,1077,1087,1090,1145,1149,24,149,175,240,312,402,497,540,605,629,680,685,708,710,711,712,727,746,826,835,857,870,917,1008,1009,1036,1067,1153,1160,1194,81,186,195,241,301,342,363,368,377,387,396,475,486,544,562,573,630,683,747,757,775,846,893,903,912,959,1003,1010,1058,1181,15,26,37,97,216,313,332,348,350,425,432,438,504,519,523,528,543,661,726,729,740,892,905,970,1041,1048,1054,1074,1106,1156,1,14,49,62,110,266,279,285,307,352,445,463,479,597,608,833,858,891,920,935,937,978,986,1034,1047,1096,1105,1139,1151,1174,51,90,163,183,203,454,506,509,560,578,618,691,701,704,720,722,772,866,886,894,899,914,967,990,993,1052,1064,1136,1154,1184,59,107,111,140,170,192,225,275,330,344,384,450,541,559,587,639,646,676,677,761,804,818,828,980,983,988,1066,1085,1089,1169,65,77,144,161,202,236,282,365,409,455,532,554,591,593,632,705,767,781,812,856,924,958,982,1006,1051,1092,1130,1148,1152,1173};
  		//for(int i=0;i<D.length;i++){driver_order.add(D[i]);}
  		double c1=this.cal_cost(t1);
  		System.out.println("initial cost");
  		System.out.println(c1);
  		boolean acp=false;int count1=0;
  		while(count1<initial_shuffle){
  			count1+=1;
		    HashMap<Integer,ArrayList<DI>> t11=new HashMap<Integer,ArrayList<DI>>();
		    for(int i=0;i<this.facilitycount;i++){t11.put(i, new ArrayList<DI>());}
  		    ArrayList<Integer> t21=new ArrayList<Integer>();
  		    for(int i=0;i<this.drivercount;i++){t21.add(i);}
  		    Collections.shuffle(t21);
  		    ArrayList<Integer> driver_order1=new ArrayList<Integer>();
  		    for(int i=0;i<this.drivercount;i++){driver_order1.add(t21.get(i));}
  		    t11=this.reasign_f_d(DF, open1, driver_order1,1);
  		    double c11=this.cal_cost(t11);
  		    if(larger(c1,c11)){t1=this.copy_F(t11);c1=c11;acp=true;
  		    	System.out.println(count1);System.out.println("initial cost improve to ");
  		    	System.out.println(c1);
  		    this.driver_order=driver_order1;}
  		    }
  		return t1;
  		}
	
	private static int countIteration = 0;
	private static final double PRECISION = 1e-5;

	public void Search1_SA(double per,int initial_shuffle,int nbchangedri,int rankF,double rate,double adjust,double initialT,int nbT,int nbIterationPerT,int maxcountTotal,int experimentTimes) throws Exception{
		double startT = System.currentTimeMillis();
		String pres="/Users/jessicanini/Desktop/c2/small";
		pres=".";
	    String filePath1=pres+"/heur_log/log1_"+"F"+this.facilitycount+"D"+this.drivercount+"Cap"+this.Cap[0]+".txt";
		fw1 = new FileWriter(filePath1);bw1 = new BufferedWriter(fw1);
		String filePath2=pres+"/heur_log/log2_"+"F"+this.facilitycount+"D"+this.drivercount+"Cap"+this.Cap[0]+".txt";
		fw2 = new FileWriter(filePath2);bw2 = new BufferedWriter(fw2);
		String filePath3=pres+"/heur_log/log3_"+"F"+this.facilitycount+"D"+this.drivercount+"Cap"+this.Cap[0]+".txt";
		fw3 = new FileWriter(filePath3);bw3 = new BufferedWriter(fw3);
		String filePath4=pres+"/heur_log/log4_"+"F"+this.facilitycount+"D"+this.drivercount+"K"+this.knumber+"e_gas.txt";
		fw_g = new FileWriter(filePath4);bw_g = new BufferedWriter(fw_g);
		String filePath5=pres+"/heur_log/log5_"+"F"+this.facilitycount+"D"+this.drivercount+"K"+this.knumber+"e_gas.txt";
		fw_s = new FileWriter(filePath5);bw_s = new BufferedWriter(fw_s);
		
		//---calculate djk---//important
		this.cal_djk();
		HashMap<Integer,ArrayList<Integer>> DF=rank_djk(this.dj);
		System.out.println("complete djk");System.out.println();
		double initialcost=0;
		double sa_mincost=0;
		int ini_count=0;
		curFacSol=this.initial_sol(DF,per,initial_shuffle);
		initialcost=this.cal_cost(curFacSol);
  	
		Set<Integer> open1=new HashSet<Integer>();
		while(ini_count<200*initial_shuffle){
			ini_count+=1; 
			HashMap<Integer,ArrayList<DI>> s11=new HashMap<Integer,ArrayList<DI>>();
	  		ArrayList<Integer> t2=new ArrayList<Integer>();
	  		for(int i=0;i<this.drivercount;i++){t2.add(i);}
	  		Collections.shuffle(t2);
	  		int k=new Random().nextInt((int) (this.facilitycount*per));
	  		open1=new HashSet<Integer>();
	  		k=(int) (this.facilitycount*per);
	        while(open1.size()<k){open1.add(new Random().nextInt(this.facilitycount));}
	  		s11=this.reasign_f_d(DF, open1, t2,1);
		    double cs1=this.cal_cost(s11);
		    if(initialcost>cs1){curFacSol=this.copy_F(s11);initialcost=cs1; 
		    this.TOpenset=new HashSet<Integer>();
	        for(Integer f:open1){this.TOpenset.add(f);}
		    System.out.printf("initial cost:%f",initialcost);System.out.println();}
		}
		System.out.printf("final initial cost:%f",initialcost);
		System.out.printf("time:%f,initial cost: %f,GAP:%f",(System.currentTimeMillis()-startT)/1000,initialcost,(initialcost-knownopt)/knownopt);
		//System.out.println(curFacSol);
	
		//---initial solution---//
		if(initialcost<bestcost){
			curcost=initialcost;bestcost=initialcost;sa_mincost=initialcost;
			bestdoDriver=this.copy_driver(curdoDriver);
			bestundoDriver=this.copy_driver(curundoDriver);
			bestFacSol=this.copy_F(curFacSol);
			bestOpenset = this.copy_set(curOpenset);
			bestCloseset = this.copy_set(curCloseset);
		}else{sa_mincost=bestcost;}
		/*
		KM1 km1=new KM1(this.Facility,curFacSol);
		int enumber=(int) (this.facilitycount*per);
		Set<Integer> e_gas=km1.elit_gas(this.Points, enumber);
		bw4.write(e_gas.toString());
		bw4.flush();*/
		double[] TOrder = new double[nbT];TOrder[0] = initialT;
		int countT=0;//System.err.println("===Temperature Schedule===\n" + TOrder[0]);
		for (int i = 1; i < nbT; i++) {//System.err.println("T[" + i + "]= " + TOrder[i]);
			TOrder[i] = rate* TOrder[i - 1];}
		boolean NP=false;	
		//---check the temperature schedule---//
		for (int i = 0; i < nbT; i++) {
			double T = TOrder[i];
			if(bestcost!=knownopt){
			for (int j = 0; j < nbIterationPerT; j++) {
				boolean accept = false;
				boolean impro=false;
				int countTotal=0;
				do{
					countTotal+=1;countIteration++;
					//System.out.println("into loop1");
					WriteToFileD(fw3,bw3,this.driver_order);
					neighFacSol=new HashMap<Integer,ArrayList<DI>>();
					neighFacSol=this.reasign_mix1(open1,curFacSol,DF,per,initial_shuffle, nbchangedri,rankF);
					double tempc=this.cal_cost(neighFacSol);
					double improvement=curcost-tempc;//

					System.out.printf("time:%f,GAP:%f,urrent temper:%f,new neighbor:%f,cur sol:%f,improve:%b",(System.currentTimeMillis()-startT)/1000,(bestcost-knownopt)/knownopt,T,tempc,curcost,improvement>0);System.out.println();
					double x = improvement / T;double probWorse = Math.pow(Math.E, adjust*x);double probRandom = Math.random();
				    //---decide accept or reject this neighbor---///
					if (larger(improvement,0)) {//improvement-->accept,update tempMinLength
						curFacSol=this.copy_F(neighFacSol);
						curcost=tempc;
						//System.out.printf("current temper:%f,accept new sol:%f",T,curcost);System.out.println();
						accept=true;impro=true;
						//this.driver_order=this.copy_driver(opendri);
						if(curcost<sa_mincost){sa_mincost=curcost;}  
						} 
					else {
						if(NP){//worse-->accept
							curFacSol=this.copy_F(neighFacSol);curcost=tempc;accept=true;
							//System.err.printf("current temper:%f,accept bad sol:%f",T,curcost);System.err.println();
							}//this.driver_order=this.copy_driver(opendri);}
					
						else {accept = false;} //worse-->reject
						}
					if(sa_mincost<bestcost){bestFacSol=this.copy_F(neighFacSol);bestcost=sa_mincost;}
					}while(!accept && countTotal<maxcountTotal);//for while
				bw1.write("current temper "+String.valueOf(T)+"current sol "+String.valueOf(curcost)+"local sol "+String.valueOf(sa_mincost)+"global best "+String.valueOf(bestcost)+"GAP:"+String.valueOf((bestcost-knownopt)/knownopt));
				WriteToFile(fw1,bw1,bestFacSol);
				}System.out.println("Finish one temper");//
			}
			double endc=System.currentTimeMillis();
			System.out.printf("current temper:%f,time:%f,cur sol:%f,local sol:%f,GAP:%f",T,(endc-startT)/1000,curcost,bestcost,(bestcost-knownopt)/knownopt);
			System.out.println(" ");
			}
		bw1.flush();bw1.close();fw1.close();
		bw2.flush();bw2.close();fw2.close();
		bw3.flush();bw3.close();fw3.close();
	}
	



	//helper function
	
	public int[] generate_index(){
		int indexA=new Random().nextInt(this.drivercount-1);
		 int indexB=new Random().nextInt(this.drivercount-1);
		 while(indexB<indexA){indexB=new Random().nextInt(this.drivercount-1);}
		 int indexC=new Random().nextInt(this.drivercount-1);
		 while(indexC<indexB){indexC=new Random().nextInt(this.drivercount-1);}
		 int[] point=new int[3];point[0]=indexA;point[1]=indexB;point[2]=indexC;
		 return point;
	}

	/*
	 
	public Set<Integer> get_newFac1(HashMap<Integer,ArrayList<DI>> s1,int f1){
		Set<Integer> openf1=new HashSet<Integer>();//
		if(f1==0){openf1=close_facility(s1)[0];}
		if(f1==1){openf1=open_facility(s1)[0];}
		if(f1==2){openf1=open_close_facility(s1)[0];}
	    if(f1==3){openf1=open2_close1_facility(s1)[0];}
		if(f1==4){openf1=open1_close2_facility(s1)[0];}
		if(f1==5){openf1=open0_close0_facility(s1)[0];}//for(Integer e:openf1){System.err.println(e);}//
		return openf1;
		}
		*/
	
	public ArrayList<Integer> get_newDriver(ArrayList<Integer> old_d,int d1){
		ArrayList<Integer> arrd=new ArrayList<Integer>();
		if(d1==0){
			ArrayList<Integer> d=new ArrayList<Integer>();
	        for(int i=0;i<this.drivercount;i++){d.add(i);}
	        Collections.shuffle(d);
	        arrd=this.copy_driver(d);}
		if(d1==1){
			int indexA=new Random().nextInt(this.drivercount-1);
			int indexB=new Random().nextInt(this.drivercount-1);
			while(indexB<indexA){indexB=new Random().nextInt(this.drivercount-1);}
			int indexC=new Random().nextInt(this.drivercount-1);
			while(indexC<indexB){indexC=new Random().nextInt(this.drivercount-1);}
			//int indexD=new Random().nextInt(this.drivercount);
			//while(indexD<indexC){indexD=new Random().nextInt(this.drivercount);}
			int[] point=new int[3];point[0]=indexA;point[1]=indexB;point[2]=indexC;
			int[] a1=this.new_order(point, a_to_i(old_d));
			//int[] a2=this.reverser(indexC, indexD, a1);
			arrd=this.i_to_a(a1);
			
		}
		//System.out.println(arrd.size());
		return arrd;
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

	public boolean larger(double a, double b) {
		double gap = a - b;
		return gap > PRECISION;
	}
	/**
     * @return array to int[]
     * */
	public int[] a_to_i(ArrayList<Integer> ai1) {
		int[] b=new int[ai1.size()];
		for(int i=0;i<ai1.size();i++){b[i]=ai1.get(i);}
		return b;
		}
	/**
     * @return int[] to array
     * */
	public ArrayList<Integer> i_to_a(int[] ai1) {
		ArrayList<Integer> b=new ArrayList<Integer>();
		for(int i=0;i<ai1.length;i++){b.add(ai1[i]);}
		return b;
		}
	
	public int[] new_order(int[] point,int[] old_order){
		int indexA=point[0];
		int indexC=point[1];
		int indexE=point[2];
		int indexB=indexA+1;
		int indexD=indexC+1;
		int indexF=indexE+1;
		int[] order=new int[old_order.length];
		for(int i=0;i<old_order.length;i++){order[i]=old_order[i];}
		int which=new Random().nextInt(7)+1;
		//sol = p[:a+1] + p[b:c+1]    + p[e:d-1:-1] + p[f:] # 2-opt
		if(which==1){
			int nc=0;
			for(int i1=0;i1<indexA+1;i1++){order[nc]=old_order[i1];nc+=1;}
			for(int i2=indexB;i2<indexC+1;i2++){order[nc]=old_order[i2];nc+=1;}
			for(int i3=indexE;i3>indexD-1;i3--){order[nc]=old_order[i3];nc+=1;}
			for(int i4=indexF;i4<old_order.length;i4++){order[nc]=old_order[i4];nc+=1;}
		}
		 //sol = p[:a+1] + p[c:b-1:-1] + p[d:e+1]    + p[f:] # 2-opt
		if(which==2){
			int nc=0;
			for(int i1=0;i1<indexA+1;i1++){order[nc]=old_order[i1];nc+=1;}
			for(int i2=indexC;i2>indexB-1;i2--){order[nc]=old_order[i2];nc+=1;}
			for(int i3=indexD;i3<indexE+1;i3++){order[nc]=old_order[i3];nc+=1;}
			for(int i4=indexF;i4<old_order.length;i4++){order[nc]=old_order[i4];nc+=1;}
		}
		//sol = p[:a+1] + p[e:d-1:-1] + p[c:b-1:-1] + p[f:] # 2-opt
		if(which==3){
			int nc=0;
			for(int i1=0;i1<indexA+1;i1++){order[nc]=old_order[i1];nc+=1;}
			for(int i2=indexE;i2>indexD-1;i2--){order[nc]=old_order[i2];nc+=1;}
			for(int i3=indexC;i3>indexB-1;i3--){order[nc]=old_order[i3];nc+=1;}
			for(int i4=indexF;i4<old_order.length;i4++){order[nc]=old_order[i4];nc+=1;}
		}
        //sol = p[:a+1] + p[c:b-1:-1] + p[e:d-1:-1] + p[f:] # 3-opt
		if(which==4){
			int nc=0;
			for(int i1=0;i1<indexA+1;i1++){order[nc]=old_order[i1];nc+=1;}
			for(int i3=indexC;i3>indexB-1;i3--){order[nc]=old_order[i3];nc+=1;}
			for(int i2=indexE;i2>indexD-1;i2--){order[nc]=old_order[i2];nc+=1;}
			for(int i4=indexF;i4<old_order.length;i4++){order[nc]=old_order[i4];nc+=1;}
		}
		//sol = p[:a+1] + p[d:e+1]    + p[b:c+1]    + p[f:] # 3-opt
		if(which==5){
			int nc=0;
			for(int i1=0;i1<indexA+1;i1++){order[nc]=old_order[i1];nc+=1;}
			for(int i3=indexD;i3<indexE+1;i3++){order[nc]=old_order[i3];nc+=1;}
			for(int i2=indexB;i2<indexC+1;i2++){order[nc]=old_order[i2];nc+=1;}
			for(int i4=indexF;i4<old_order.length;i4++){order[nc]=old_order[i4];nc+=1;}
		}
		// sol = p[:a+1] + p[d:e+1]    + p[c:b-1:-1] + p[f:] # 3-opt
		if(which==6){
			int nc=0;
			for(int i1=0;i1<indexA+1;i1++){order[nc]=old_order[i1];nc+=1;}
			for(int i2=indexD;i2<indexE+1;i2++){order[nc]=old_order[i2];nc+=1;}
			for(int i3=indexC;i3>indexB-1;i3--){order[nc]=old_order[i3];nc+=1;}
			for(int i4=indexF;i4<old_order.length;i4++){order[nc]=old_order[i4];nc+=1;}
		}
	    //sol = p[:a+1] + p[e:d-1:-1] + p[b:c+1]    + p[f:] # 3-opt
		if(which==7){
			int nc=0;
			for(int i1=0;i1<indexA+1;i1++){order[nc]=old_order[i1];nc+=1;}
			for(int i2=indexE;i2>indexD-1;i2--){order[nc]=old_order[i2];nc+=1;}
			for(int i3=indexB;i3<indexC+1;i3++){order[nc]=old_order[i3];nc+=1;}
			for(int i4=indexF;i4<old_order.length;i4++){order[nc]=old_order[i4];nc+=1;}
		}
		return order;
	}


	
	public Set<Integer> get_openset(HashMap<Integer,ArrayList<DI>> s1){
		Set<Integer> temp=new HashSet<Integer>();
		for(Integer f:s1.keySet()){
			if(s1.get(f).size()>0){
				temp.add(f);}
		}
		return temp;
		}
	
	public Set<Integer> get_closeset(HashMap<Integer,ArrayList<DI>> s1){
		Set<Integer> temp=new HashSet<Integer>();
		for(int i=0;i<this.facilitycount;i++){
			if(!s1.containsKey(i)){temp.add(i);}
			else{if(s1.get(i).size()<=0){temp.add(i);}}
			}
		return temp;
		}
	
	public Set<Integer>[] close_facility(HashMap<Integer,ArrayList<DI>> s1){
		Set<Integer> open=get_openset(s1);
		Set<Integer> close=get_closeset(s1);
		Set<Integer> which=new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		Set<Integer>[] res=new Set[3];
		if(open.size()<=1){
			res[0]=open;
		    res[1]=close;
		    which.add(-1);
		    res[2]=which;}
		else{
		int closef;
		boolean flag=false;
		while(!flag){
		Random r=new Random();
		closef=r.nextInt(this.facilitycount);
		if(open.contains(closef)){
			flag=true;
			open.remove(closef);
			close.add(closef);
			which.add(closef);
			res[2]=which;
		}
		}
		res[0]=open;
		res[1]=close;}
		return res;
	}
	
	public Set<Integer>[] open_facility(HashMap<Integer,ArrayList<DI>> s1){
		Set<Integer> open=get_openset(s1);
		Set<Integer> close=get_closeset(s1);
		Set<Integer> which=new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		Set<Integer>[] res=new Set[3];
		if(close.size()<=0){
			res[0]=open;
		    res[1]=close;
		    which.add(-1);
		    res[2]=which;}
		else{
		int openf;
		boolean flag=false;
		while(!flag){
		Random r=new Random();
		openf=r.nextInt(this.facilitycount);
		if(close.contains(openf)){
			flag=true;
			close.remove(openf);
			open.add(openf);
			}}
		
		res[0]=open;
		res[1]=close;
	    which.add(-1);
	    res[2]=which;}
		return res;
	}
	
	 public Set<Integer> open_close_facility(Set<Integer> P,Set<Integer> egas,double pos){
			Set<Integer> res=new HashSet<Integer>();
			
			int[] opens=new int[P.size()];
			int oi=0;
			for(Integer e:P){opens[oi]=e;oi+=1;}
			double[] opens1=new double[P.size()];
			int[] closes=new int[this.facilitycount-P.size()];
			int ci=0;for(int i=0;i<this.facilitycount;i++){if(!P.contains(i)){closes[ci]=i;ci+=1;}}
			double[] closes1=new double[this.facilitycount-P.size()];
			double sum1=0.0;double sum2=0.0;
			for(int i1=0;i1<opens.length;i1++){
				if(egas.contains(opens[i1])){sum1+=pos;opens1[i1]=pos;}
				else{sum1+=pos+0.2;opens1[i1]=0.2+pos;}
			}
			for(int i2=0;i2<opens1.length;i2++){opens1[i2]=opens1[i2]/sum1;}
			for(int i3=1;i3<opens1.length;i3++){opens1[i3]=opens1[i3]+opens1[i3-1];}
			
			for(int i1=0;i1<closes.length;i1++){
				if(egas.contains(closes[i1])){sum2+=pos+0.2;closes1[i1]=pos+0.2;}
				else{sum2+=pos;closes1[i1]=pos;}
			}
			for(int i2=0;i2<closes1.length;i2++){closes1[i2]=closes1[i2]/sum2;}
			for(int i3=1;i3<closes1.length;i3++){closes1[i3]=closes1[i3]+closes1[i3-1];}
			double r1=new Random().nextDouble();
			double r2=new Random().nextDouble();
			int openf = -1;int closef = -1;
			for(int i4=0;i4<opens1.length;i4++){
				if(opens1[i4]>r1){openf=opens[i4];break;}
					}
			for(int i4=0;i4<closes1.length;i4++){
				if(closes1[i4]>r2){closef=closes[i4];break;}
					}
			for(Integer fc:P){res.add(fc);}
			res.add(openf);
			res.remove(closef);
			return res;
		}
	
	public Set<Integer>[] open1_close2_facility(HashMap<Integer,ArrayList<DI>> s1){
		Set<Integer> open=get_openset(s1);
		Set<Integer> close=get_closeset(s1);
		Set<Integer> which=new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		Set<Integer>[] res=new Set[3];
		if(close.size()<=0 || open.size()<=2){
			res[0]=open;
			res[1]=close;
			which.add(-1);
			res[2]=which;}
		else{
		int openf;
		int closef1;
		int closef2;
		boolean flag=false;
		while(!flag){
		Random r1=new Random();
		Random r2=new Random();
		Random r3=new Random();
		openf=r1.nextInt(this.facilitycount);
		closef1=r2.nextInt(this.facilitycount);
		closef2=r3.nextInt(this.facilitycount);
		if(close.contains(openf) && open.contains(closef1) && open.contains(closef2)){
			flag=true;
			close.remove(openf);
			close.add(closef1);
			close.add(closef2);
			open.add(openf);
			open.remove(closef1);
			open.remove(closef2);
			which.add(closef1);
			which.add(closef2);}}
		res[0]=open;
		res[1]=close;
		res[2]=which;}
		return res;
	}
	
	public Set<Integer>[] open2_close1_facility(HashMap<Integer,ArrayList<DI>> s1){
		Set<Integer> open=get_openset(s1);
		Set<Integer> close=get_closeset(s1);
		Set<Integer> which=new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		Set<Integer>[] res=new Set[3];
		if(close.size()<=2 || open.size()<=1){
			res[0]=open;
			res[1]=close;
			which.add(-1);
			res[2]=which;}
		else{
		int openf1;
		int openf2;
		int closef;
		boolean flag=false;
		while(!flag){
		Random r1=new Random();
		Random r2=new Random();
		Random r3=new Random();
		openf1=r1.nextInt(this.facilitycount);
		openf2=r2.nextInt(this.facilitycount);
		closef=r3.nextInt(this.facilitycount);
		if(close.contains(openf1) && close.contains(openf2) && open.contains(closef)){
			flag=true;
			close.remove(openf1);
			close.remove(openf2);
			close.add(closef);
			open.add(openf1);
			open.add(openf2);
			which.add(closef);}}
		res[0]=open;
		res[1]=close;
		res[2]=which;}
		return res;
	}

	public Set<Integer>[] open0_close0_facility(HashMap<Integer,ArrayList<DI>> s1){
		Set<Integer> open=get_openset(s1);
		Set<Integer> close=get_closeset(s1);
		Set<Integer> which=new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		Set<Integer>[] res=new Set[3];
		which.add(-1);
		res[0]=open;
		res[1]=close;
		res[2]=which;
		return res;
	}

	public int[] generate_indexK(int k){
		Set<Integer> t1=new HashSet<Integer>();
		while(t1.size()<k){t1.add(new Random().nextInt(this.facilitycount));}
		int[] point=new int[k];int i=0;
		for(Integer e:t1){point[i]=e;i+=1;}
		return point;
	}

	public Set<Integer> open2_close2_facility(Set<Integer> P){
		Set<Integer> res=new HashSet<Integer>();
		for(Integer fc:P){res.add(fc);}
		int openf1;int openf2;int closef1;int closef2;
		boolean flag=false;
		while(!flag){
		Random r1=new Random();Random r2=new Random();Random r3=new Random();Random r4=new Random();
		openf1=r1.nextInt(this.facilitycount);openf2=r2.nextInt(this.facilitycount);
		while(openf2==openf1){openf2=r2.nextInt(this.facilitycount);}
		closef1=r3.nextInt(this.facilitycount);closef2=r4.nextInt(this.facilitycount);
		while(openf2==openf1){openf2=r1.nextInt(this.facilitycount);}while(closef2==closef1){closef2=r4.nextInt(this.facilitycount);}
		
		if(!P.contains(openf1) && !P.contains(openf2) && P.contains(closef1) && P.contains(closef2)){
			flag=true;
			res.add(openf1);
			res.remove(closef1);
			res.add(openf2);
			res.remove(closef2);}
		}
		return res;}

	public Set<Integer> open3_close3_facility(Set<Integer> P){
		Set<Integer> res=new HashSet<Integer>();
		for(Integer fc:P){res.add(fc);}
		int openf1;int openf2;int openf3;int closef1;int closef2;int closef3;
		boolean flag=false;
		while(!flag){
			int[] point1=generate_indexK(3);int[] point2=generate_indexK(3);
			openf1=point1[0];openf2=point1[1];openf3=point1[2];
			closef1=point2[0];closef2=point2[1];closef3=point2[2];
		if(!P.contains(openf1) && !P.contains(openf2) && !P.contains(openf3) && P.contains(closef1) && P.contains(closef2) && P.contains(closef3)){
			flag=true;
			res.add(openf1);res.add(openf2);res.add(openf3);
			res.remove(closef1);res.remove(closef2);res.remove(closef3);
			}
		}
		return res;}
	
	
	public Set<Integer> openr_facility(){

		int k=(int) (this.facilitycount*0.4);
		Set<Integer> open1=new HashSet<Integer>();
        while(open1.size()<k){open1.add(new Random().nextInt(this.facilitycount));}
        return open1;
        }

	public ArrayList<Integer> getk(int s){
		ArrayList<Integer> r=new ArrayList<Integer>();
		for(int i=0;i<s;i++){r.add(i);}
		Collections.shuffle(r);
		return r;
	}

	public Set<Integer>[] open1_close1_facility(HashMap<Integer,ArrayList<DI>> s1){
		Set<Integer> open=get_openset(s1);
		Set<Integer> close=get_closeset(s1);
		Set<Integer> which=new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		Set<Integer>[] res=new Set[3];
		if(close.size()<=0 || open.size()<=1){
			res[0]=open;
			res[1]=close;
			which.add(-1);
		    res[2]=which;}
		else{
		int openf;
		int closef;
		boolean flag=false;
		while(!flag){
		Random r1=new Random();
		Random r2=new Random();
		openf=r1.nextInt(this.facilitycount);
		closef=r2.nextInt(this.facilitycount);
		if(close.contains(openf) && open.contains(closef)){
			flag=true;
			close.remove(openf);
			close.add(closef);
			open.add(openf);
			open.remove(closef);
            which.add(closef);}}
		res[0]=open;
		res[1]=close;
		res[2]=which;}
		return res;
	}


	public HashMap<Integer,ArrayList<Integer>> rank_jdk(double[][] dj1,int rk){
		HashMap<Integer,ArrayList<Integer>> FD=new HashMap<Integer,ArrayList<Integer>>();
		for(int j=0;j<this.facilitycount;j++){
		
		double[] arr = new double[this.drivercount];
		for(int i=0;i<this.drivercount;i++){arr[i]=dj1[i][j];}
		int[] Index=new int[arr.length];
		Index=Arraysort(arr);
		ArrayList<Integer> t1=new ArrayList<Integer>(); 
	    for(int i1=0;i1<rk;i1++){
	    	t1.add(Index[i1]);}
	    	FD.put(j, t1);
		}
	    return FD;
		}
	public ArrayList<ArrayList<Integer>> permute(int[] nums) {
		   ArrayList<ArrayList<Integer>> list = new ArrayList<ArrayList<Integer>>();
		   // Arrays.sort(nums); // not necessary
		   backtrack(list, new ArrayList<>(), nums,0);
		   return list;
		}
	public void backtrack(ArrayList<ArrayList<Integer>> list, ArrayList<Integer> tempList, int [] nums,int c){
		   if(tempList.size() == nums.length){
		      list.add(new ArrayList<>(tempList));
		      //System.out.println(tempList);
		   } else{
		      for(int i = 0; i < nums.length; i++){ 
		         if(tempList.contains(nums[i])) continue; // element already exists, skip
		         tempList.add(nums[i]);
		         c++;
		         backtrack(list, tempList, nums,c);
		         
		         tempList.remove(tempList.size() - 1);
		      }
		   }
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
	
	 public void handle1(ArrayList<Integer> openf, HashMap<Integer, ArrayList<DI>> temp) {
		 for(Integer e:temp.keySet()){
			 if(!openf.contains(e)){
				 temp.put(e, new  ArrayList<DI>());
				 }}
		 for(Integer e:openf){
			 if(!temp.containsKey(e)){
				 temp.put(e, new  ArrayList<DI>());
				 }}
		}
	
	public HashMap<Integer,Integer> get_df(HashMap<Integer,ArrayList<DI>> s1){
		HashMap<Integer,Integer> df=new HashMap<Integer,Integer>();
		for(Integer e:s1.keySet()){
			ArrayList<DI> a1=new ArrayList<DI>();
			a1=s1.get(e);
			for(DI e1:a1){
				df.put(e1.d, e);
			}
		}
		return df;
	}
	
	
	public HashMap<Integer,ArrayList<Integer>> get_fd(HashMap<Integer,ArrayList<DI>> s1){
		HashMap<Integer,ArrayList<Integer>> r=new HashMap<Integer,ArrayList<Integer>>();
		for(Integer e:s1.keySet()){
			ArrayList<Integer> t=new ArrayList<Integer>();
			for(DI e1:s1.get(e)){
				t.add(e1.d);
			}
			r.put(e, t);
		}
		return r;
	}
	public ArrayList<Integer> get_opendriver(HashMap<Integer,ArrayList<DI>> s1){
		Set<Integer> temp=new HashSet<Integer>();
		for(Integer e1:s1.keySet()){
			for(DI e2:s1.get(e1)){
				temp.add(e2.d);}}
		ArrayList<Integer> do1=new ArrayList<Integer>();
		for(Integer e3:temp){do1.add(e3);}
		return do1;}
	
    public ArrayList<Integer> get_closedriver(HashMap<Integer,ArrayList<DI>> s1){
    	Set<Integer> temp=new HashSet<Integer>();
		for(Integer e1:s1.keySet()){
			for(DI e2:s1.get(e1)){
				temp.add(e2.d);}}
		ArrayList<Integer> do1=new ArrayList<Integer>();
		ArrayList<Integer> undo1=new ArrayList<Integer>();
		for(Integer e3:temp){do1.add(e3);}
		for(int i=0;i<this.drivercount;i++){
			if(!do1.contains(i)){undo1.add(i);}}
		return undo1;}
	public boolean E1(int a1,int b1,int a2,int b2){
		return !(a1==a2 && b1==b2);}
	
	private ArrayList<Integer> copy_driver(ArrayList<Integer> neighundoDriver2) {
		ArrayList<Integer> temp=new ArrayList<Integer>();
		for(Integer d:neighundoDriver2){temp.add(d);}
		return temp;
	}

	private Set<Integer> copy_set(Set<Integer> neighOpenset2) {
		Set<Integer> temp=new HashSet<Integer>();
		for(Integer e:neighOpenset2){temp.add(e);}
		return temp;
	}

	public HashMap<Integer,ArrayList<DI>> copy_F(HashMap<Integer,ArrayList<DI>> fsol){
		HashMap<Integer,ArrayList<DI>> t=new HashMap<Integer,ArrayList<DI>>();
		for(Integer f:fsol.keySet()){
			ArrayList<DI> tdi=new ArrayList<DI>();
			for(DI di:fsol.get(f)){
				tdi.add(new DI(di.d,di.k));
			}
			t.put(f, tdi);
			}
		return t;
	}
	
	public void display_F(HashMap<Integer,ArrayList<DI>> fsol,Set<Integer> opens){
		for(Integer f:opens){
			System.out.printf("gas station:%d ",f);
			for(DI di:fsol.get(f)){
				System.out.printf("driver:%d,demand point:%d ",di.d,di.k);
			}
			System.out.println();
		}
	}

	public void display_D(HashMap<Integer,ArrayList<DI>> fsol,Set<Integer> opens){
		
		ArrayList<Integer> ds=new ArrayList<Integer>();
		ArrayList<Integer> unds=new ArrayList<Integer>();
		for(Integer f:opens){
			for(DI di:fsol.get(f)){
				ds.add(di.d);
			}
		}
		for(int i=0;i<this.drivercount;i++){
			if(!ds.contains(i)){
				unds.add(i);
			}
		}
		System.out.println();
		System.out.println("unsatisfied driver");
		for(Integer d:unds){
			System.out.printf(" %d",d);
		}
		System.out.println("satisfied driver");
		for(Integer d:ds){
			System.out.printf(" %d",d);
		}
	}
	
	public double cal_cost(HashMap<Integer,ArrayList<DI>> fsol){
		double tcost=0;
		Set<Integer> opens=new HashSet<Integer>();
		ArrayList<Integer> ds=new ArrayList<Integer>();
		ArrayList<Integer> unds=new ArrayList<Integer>();
		for(Integer f:fsol.keySet()){
			if(fsol.get(f).size()>0){
			opens.add(f);}
		}
		 
		for(Integer f:opens){
			for(DI di:fsol.get(f)){
				Routine r1=this.Drivers.get(di.d).routines.get(Math.floorDiv(di.k,this.knumber));
				Point e11=r1.end;
				tcost+=length(this.Points[di.d][di.k],this.Facility.get(f).location)+length(this.Facility.get(f).location,e11);
				//tcost+=length(this.Points[di.d][di.k],Facility.get(f).location);
				ds.add(di.d);
			}
		}
		for(int i=0;i<this.drivercount;i++){
			if(!ds.contains(i)){
				tcost+=this.penalty[i][0];
				unds.add(i);
			}
		}
		for(@SuppressWarnings("unused") Integer f:opens){
			tcost+=this.set_cost;
			}
		return tcost;
	}
	
	public boolean check_sol(HashMap<Integer,ArrayList<DI>> fsol){
		
		Set<Integer> opens=new HashSet<Integer>();
		ArrayList<Integer> ds=new ArrayList<Integer>();
		ArrayList<Integer> unds=new ArrayList<Integer>();
		for(Integer f:fsol.keySet()){
			if(fsol.get(f).size()>0){
			opens.add(f);}
		}
		boolean flag=true;
		int[][] fcap=new int[this.facilitycount][24];
		for(int q=0;q<this.facilitycount;q++){
			for(int i=0;i<24;i++){fcap[q][i]=Cap[q];}}
		
		for(Integer f:opens){
			for(DI di:fsol.get(f)){
				assert f!=Facility.get(f).index;
				flag=this.timeFeasible(di.d, di.k, Facility.get(f).index);
				int ntime=get_time(di.d, di.k);
				fcap[Facility.get(f).index][ntime]-=1;
			}
		}
		for(int q=0;q<this.facilitycount;q++){
			for(int i=0;i<24;i++){
				flag=fcap[q][i]>=0;}}
		return flag;
	}
	public void WriteToFileD(FileWriter fw32, BufferedWriter bw32, ArrayList<Integer> driver_order2) throws IOException {
		 bw32.write("driver seq "+"\n");
         for(int i=0;i<this.drivercount;i++){
        	 bw32.write(String.valueOf(driver_order2.get(i))+" ");
        	 }
         bw32.write("Finish"+"\n");
	    bw32.flush();
		
	}
	public void WriteToFile(FileWriter fwk,BufferedWriter bwk,HashMap<Integer,ArrayList<DI>> fsol) {
		try {
			Set<Integer> opens=new HashSet<Integer>();
			for(Integer e:fsol.keySet()){
				if(fsol.get(e).size()>0){opens.add(e);}
			}
	        double tcost=0;
	    	ArrayList<Integer> ds=new ArrayList<Integer>();
	    	ArrayList<Integer> unds=new ArrayList<Integer>();
	    	for(Integer f:opens){
	    		for(DI di:fsol.get(f)){
	    			tcost+=length(this.Points[di.d+aS][di.k],Facility.get(f).location);
	    			ds.add(di.d);
	    			}
	    		}
	    	for(int i=0;i<this.drivercount;i++){
	    		if(!ds.contains(i)){
	    			tcost+=penalty[i][0];
	    			unds.add(i);
	    			}
	    		}
	    	for(@SuppressWarnings("unused") Integer f:opens){
	    		tcost+=this.set_cost;
	    		}
	        bwk.write("check obj"+"\n");
            bwk.write(tcost+"\n");
            bwk.write("check unsatisfied driver"+"\n");
	    	for(int i=0;i<this.drivercount;i++){
	    		if(!ds.contains(i)){
	    			bwk.write(String.valueOf(i)+" ");
	    			}
	    		}
	    	bwk.write("check satisfied driver"+"\n");
	    	for(int i=0;i<this.drivercount;i++){
	    		if(ds.contains(i)){
	    			bwk.write(String.valueOf(i)+" ");
	    			}
	    		}
	    	bwk.write("\n");
	    
	    	for(Integer f:opens){
	    		bwk.write("gas staion ");
	    		bwk.write(f+"\n");
	    		for(DI di:fsol.get(f)){
	    			bwk.write("driver "+String.valueOf(di.d)+" demand point "+String.valueOf(di.k)+" ");
	    			}
	    		bwk.write("\n");
	    		}
	    	bwk.flush();
	        } catch (Exception e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	    }
	
	
}
