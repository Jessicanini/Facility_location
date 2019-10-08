
public class Gps {
	double a;
	double b;
	public Gps(double a1,double b1){
		this.a=a1;
		this.b=b1;
	}
	public double getWgLat(){
		return this.a;
	}
	public double getWgLon(){
		return this.b;
	}
}
