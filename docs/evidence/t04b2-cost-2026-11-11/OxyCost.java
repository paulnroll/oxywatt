import com.oxywatt.core.power.*;
import java.util.*;
public class OxyCost {
 static final UUID s=new UUID(0,1), e=new UUID(0,2);
 static PowerInterval p(long a,long b,double w){return new PowerInterval(s,e,0,a,b,w);}
 static void run(String name,int mode){
  PowerCurveEngine c=new PowerCurveEngine(s,new PowerProvenance("cost-only","synthetic"),new PowerCurvePolicy("power-intervals-v1",16384));
  BudgetedPowerCurve b=new BudgetedPowerCurve(s,new PowerCurvePolicy("power-intervals-v1",16384),20000000);
  long t=0; int count=mode==2?2000:14400;
  for(int i=0;i<count;i++){
   if(mode==3 && i%1800==1799){c.breakCoverage(); b.accept(null);t+=10000000000L;}
   long dt= mode==1 ? new long[]{250000001L,749999999L,1500000000L,1500000000L}[i%4] : mode==2 ? 1000000L : 1000000000L;
   PowerInterval x=p(t,t+dt,i%2==0?100:200);c.accept(x);b.accept(x);t+=dt;
  }
  if(mode==2){PowerInterval x=p(t,t+3600000000000L,50);c.accept(x);b.accept(x);}
  PowerCurveSnapshot q=c.snapshot();
  System.out.println(name+": windows="+q.getEvaluatedWindows()+" fragments="+q.getIntegratedFragments()+" retained="+q.getRetainedIntervals()+" wrapperAccepted="+b.getIntervalCount()+" reserved="+b.getReserved()+" limited="+b.getLimited());
 }
 public static void main(String[] a){run("4h alternating",0);run("4h irregular",1);run("2000 dense then 1h",2);run("pauses/gaps",3);}
}
