
public class Point {
	long time;
	double lon;
	double lat;
public Point(long t,double l1,double l2){
	this.time=t;
	this.lon=l1;
	this.lat=l2;
}
public double getlon(){
	return this.lon;
}
public double getlat(){
	return this.lat;
}
}
