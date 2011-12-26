package com.android.gallery3d.update;

import com.android.gallery3d.update.DownThread;
import com.android.gallery3d.app.Gallery;

	
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Calendar;

import android.app.PendingIntent;
import android.app.AlarmManager;
import android.app.Service;
import android.app.ActivityManagerNative;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.widget.Toast;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.os.Build;



public class CheckUpdate extends Service {
	private String TAG	="CheckUpdate";
	private Context mContext;
    private String[] OdPath=new String[]{"http://www.allwinnertech.com/update/Gallery2-update-Zh.txt",
		"http://www.x-powers.com/update/Gallery2-update-Zh.txt",
		"http://www.softwinners.com/update/Gallery2-update-Zh.txt"};
    private String path;
    private String target="/mnt/sdcard/apk-info-update.txt";
	private String apkName;
	
    private DownThread downUtil;
    private MyBinder binder;
    private Handler handler;
    private int newVerCode;
    private List<String> newURL=new ArrayList<String>();
	private String randomURL;
	private String fastURL;
	private String newVerName;
	private String describe;
	private String packageName;
    private int oldVerCode;

    public  int status=0;
    private Thread thread;
    private int ThreadNum=0;
	private String[] UrlTeam ;
	private int UrlNum=0;
    private int minPingTime=10000;
    private int fastURLNum=0;
    private boolean  readFinish=false;
	private boolean discoverNew=false;
	private SharedPreferences mShare;
	private int randomDay=4;
	private int testFlag=0;
    private String language;
	private int MSG_PING_TIME=222;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		Log.i(TAG,"CheckUpdate_service is Binded");
		return binder;
	}
	@Override
	public void onCreate()
	{
		super.onCreate();
		mContext=getBaseContext();
		binder=new MyBinder();
		Log.i(TAG,"CheckUpdate_service is Created");
		try{
            language= ActivityManagerNative.getDefault().getConfiguration().locale.getCountry();
		    
		}catch (RemoteException e)
			{}
		Log.v(TAG,"language="+language);
		
		mShare=getSharedPreferences("NUM",MODE_PRIVATE);
		int random=(int)(Math.random()*100); 
		path=OdPath[random%OdPath.length];
		if(language.equals("CN")||language.equals("TW")){
			 Log.v(TAG,"now ,The language is CH orTM");
			}else{
			    path=path.replace("-Zh.txt","-Eg.txt");
			}
		readPreference();
		//Log.v(TAG,"info="+path);
	    handler=new Handler()
	    {   @Override
	    	public  void handleMessage(Message msg)
	    	{ 
                if (msg.what==MSG_PING_TIME)
	    	    {
	    		   Integer[] date=(Integer[]) msg.obj;
	    		   if(date[0]<UrlNum)
	    		   {
	    			  if(date[1]!=0&&date[1]<minPingTime)
	    			   {	    			    					
	    			    minPingTime=date[1];
	    				fastURLNum=date[0];	    				
	    			   }
	    		    }
	    		   if(date[0]==UrlNum-1)
	    		   {
	    		      if(minPingTime>=1300){
                        oldVerCode=newVerCode;
						Log.v(TAG,"all URl is not ping");
						readFinish=true;
						return;
	    		      	}
	    			  fastURL=UrlTeam[fastURLNum];
					  Log.v(TAG,"fastURL="+fastURL);
	    			  readFinish=true;
	    		    }
	    		
	    	   }
			   if(msg.what==100){
		           Log.i(TAG,"download finished");
	    	       readTextInfo();
	    	       status=100;
	    	     }
	    	     else if(msg.what<100){
	    		    Log.i(TAG,"finish percent:"+msg.what+"%");
	    		    status=msg.what;
	    		  }  else if (msg.what==111){ 
	    			          if(ThreadNum<=1) { 
						 	     thread=new Thread(new DownThread(path,target,handler) );
						         Log.i(TAG,"start new DownThread:"+ThreadNum);
	    			             thread.start();
	    			             ThreadNum++;    			      
	    			         }
	    		    }
	    	}
	    };

		
	   thread=new Thread(new DownThread(path,target,handler));
	   thread.start();
	 // readTextInfo();
	    
	}
		
	public class MyBinder extends Binder
	{
		public boolean isDiscoverableNew()
		{ 
			 return discoverNew;
		}
		public String getNewURL()
		{
			if(readFinish)
			 return randomURL;
				else 
					return null;
		}
		public boolean isReadFinish()
		{
			return readFinish;
		}
		public boolean getIsFail()
		{
			 if(ThreadNum>=2)
				 return true;
			 else 
				 return false;
		}
		public String getNewVerName()
		{
               return newVerName;
		
		}
		public String getApkName()
		{
		    return apkName;

		}
		public String getDescribe()
		{
		    return describe;

		}
		public String getPackageName()
		{
		    return packageName;

		}
	}
	
	private void readTextInfo()
	{   
	    PingThread  pingThread;
		TextInfo textInfo=new TextInfo(target,mContext);
		//newVerCode=textInfo.getNewVerCode(apkName);
		String scope;
		String release;
		String FIRMWARE;
		FIRMWARE=Build.FIRMWARE;
		Log.v(TAG,"Build.FIRMWARE="+FIRMWARE);
		FIRMWARE=FIRMWARE.replace(".","");

		if(FIRMWARE.equals("unknown")){
			Log.v(TAG,"The firmware is unkonwn");
			return;

		}
		int num=textInfo.discoverNew();
		if(num!=-1){
          Log.v(TAG,"find new apk to download");
		  scope=textInfo.getScope(num);
		  release=textInfo.getRelease(num);
		  Log.v(TAG,"release="+release);
		  release=release.replace(".","");	 
          if(Integer.parseInt(FIRMWARE)*100<Integer.parseInt(release)*100){
              Log.v(TAG,"The FIRMWARE is to low");
			  return;
		  }
		  
		  Log.v(TAG,"scope="+scope);
		  Log.v(TAG,"release="+release);
		  String mScope[]=scope.split("-");
		  int min=Integer.parseInt(mScope[0]);
		  int max=Integer.parseInt(mScope[1]);
		  if(min<=readPreference()&&max>=readPreference()){
		  	  apkName=textInfo.getApkName(num);
			  SharedPreferences.Editor mEditor=Gallery.mableUpdate.edit();						
		      mEditor.putString("apkName",apkName);
		      mEditor.commit();
			  Log.v(TAG,"apkName="+apkName);
		  	  discoverNew=true;
			  describe=textInfo.getDescribe(num);
			  packageName=textInfo.getPackageName(num);
			  Log.v(TAG,"describe="+describe);
              Log.v(TAG,"This pad Can download apk package");
			  newURL=textInfo.getNewURL(num);
			  Iterator<String> getLen=newURL.iterator();
		      while(getLen.hasNext()){ 
			  	 UrlNum++;
		         Log.i(TAG, getLen.next()); 
		      } 
			  UrlTeam=new String[UrlNum];
		      Iterator<String> getUrl=newURL.iterator();
			  int i=0;
		      while(getUrl.hasNext()){   
			      UrlTeam[i++]=getUrl.next(); 
		      }
			  int random=(int)(Math.random()*(UrlNum-1)); 
			  randomURL=UrlTeam[random];
			 // Log.v(TAG,"randomURL="+randomURL);
			  readFinish=true;
		  }else {
              Log.v(TAG,"This pad Can't download apk package");
		  }
		}

	}

	public String  getWWWURL(String url)
	{
		String mUrl[]=url.split(".com");
		mUrl[0]=mUrl[0].replace("http://", "");
		mUrl[0]=mUrl[0]+".com";
		//Log.v(TAG,mUrl[0]);
		return mUrl[0];
			
	}
	
	public int readPreference()
	{		
    	SharedPreferences.Editor mEditor=mShare.edit();
    	int random=(int)(Math.random()*999); 
    	int shareNum;
		if(testFlag==1){
            mEditor.putInt("shareNum", 1000);
    		mEditor.commit();
			Log.v(TAG,"put test number to shareNUm");
		}
    	if(mShare.getInt("shareNum", 1001)==1001)
    	{
    		mEditor.putInt("shareNum", random);
    		mEditor.commit();
			
    		
    	}
    	
    	 shareNum=mShare.getInt("shareNum", 1001);
    	 Log.v(TAG,"shareNum="+shareNum);   
    	
		return shareNum;
    	
	}
	public boolean TimeTask(){
        
		
		int shareNum=mShare.getInt("shareNum", 10);
		int delayDay=0;
		int delayMinute=0;
		boolean result=false;
		SharedPreferences.Editor mEditor=Gallery.mableUpdate.edit();
        String hasDelay=Gallery.mableUpdate.getString("HASDELAY",null);
		
		Calendar c=Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		int Day=c.get(Calendar.DAY_OF_MONTH);
		int Minute=c.get(Calendar.MINUTE);
		Log.v(TAG,"now Date="+c.getTime());
		if(shareNum!=10){
		   delayDay=shareNum%randomDay;
	       delayMinute=shareNum%randomDay;
		   if(delayMinute!=0){	
                 if(hasDelay==null||hasDelay.equals("NOTDALAY")){
			         mEditor.putString("HASDELAY","HASDELAY");
		             mEditor.commit();
			         Log.v(TAG,"The delay mode is "+Gallery.mableUpdate.getString("HASDELAY",null));

		        }else if(hasDelay.equals("HASDELAY")){
		                   mEditor.putString("HASDELAY","NOTDALAY");
		                   mEditor.commit();
			               Log.v(TAG,"The delay mode is "+Gallery.mableUpdate.getString("HASDELAY",null));
                           return false;
				       }
		
			     mEditor.putString("UPDATE","UNABLE");
			     mEditor.commit();
			     Log.v(TAG,"The checkUpdate mode has change to "+Gallery.mableUpdate.getString("UPDATE",null));
                 result= true;
		   } else return false;
		   Minute+=delayMinute;
		   c.set(Calendar.MINUTE,Minute);
		   Log.v(TAG,"update in Date="+c.getTime());
		   AlarmManager alarm=(AlarmManager)getSystemService(Service.ALARM_SERVICE);
		   Intent intent=new Intent(CheckUpdate.this,TimeTask.class);
		   intent.putExtra("UPDATE","ABLE");
		   PendingIntent pi=PendingIntent.getBroadcast(CheckUpdate.this, 0, intent, 0);
		   alarm.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);//RTC_WAKEUP
		}
        	
		  return result;
	}
	@Override
	public void onDestroy()
	{
		Log.i(TAG,"---CheckUpdae service Destroy  finish---");
	}
	
	private class PingThread extends HandlerThread{
         private int num;
         private String name;
         private Handler handler;
		 
         public PingThread(String name, int num,Handler handler) {
                 super(name);
                 this.num=num;
                 this.handler=handler;
                 this.name=name;
         }

        
         @Override
         protected void onLooperPrepared() { 
        	    
        	    Message msg=new Message();
        	    msg.what=MSG_PING_TIME;
				int pingTime =pingNet(name);
        	    Integer[] date=new Integer[]{num,pingTime};
        	    msg.obj=date;
        	    handler.sendMessage(msg);

				
         }
		 
         public int pingNet(String url)
     	 {   
     		String line;
     		int numTime=0;
     		int averTime=0;
     	    Process localProcess;
			try {
				Log.v(TAG,"start ping");
				localProcess = Runtime.getRuntime().exec("ping -c 2 "+url);
			    			
				InputStream localInputStream = localProcess.getInputStream(); 				
		        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(localInputStream));       
		          
		        while ((line = localBufferedReader.readLine()) != null) 
		         {  
		  
		             if(getTime(line)!=0)
		             {
		                 
		                 numTime++;
		                 averTime+=getTime(line);
		              }
		                 
		           }
		         localBufferedReader.close(); 
				 //localProcess.destroy();  
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
             if(numTime!=0)
             {
             	averTime=averTime/numTime;
             }else 
             	averTime=0;
             Log.v(TAG,name);
             Log.i(TAG, "averTime="+averTime);
             return averTime;
     	}
     	
         public int getTime(String line)
     	{
     		int ms=0;
     		if(line.indexOf("time=")!=-1)
     		{
     			StringBuilder sb = new StringBuilder(); 
     			String strTeam[]=line.split(" ");
     			String time[]=strTeam[6].split("=");
     			
     			for(int i=0;i<time[1].length();i++)
     		    {
     		    	if(time[1].charAt(i)=='.')
     		    	    break;
     		    	sb.append(time[1].charAt(i));
     		    	
     		    }     		 
     			ms=Integer.parseInt(sb.toString());     			    			      		       		    
     		}
     		return ms;
     		
     	}
    }
	
}
