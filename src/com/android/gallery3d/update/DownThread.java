package com.android.gallery3d.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.util.Log;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DownThread implements Runnable {
	private String TAG="DownTread";
    private String path;
    private String targetFile;
    public long fileSize;
    private long count_size=0;
    private Handler handler;
	private int HANDLE_DOWNLOAD_FAIL=111;
    
    public DownThread(String path,String targetFile, Handler handler)
    {
    	this.path=path;
    	this.targetFile=targetFile;
    	this.handler=handler;
    	System.out.println(" ");
		Log.i(TAG," start DownThread");

    } 
	public void run() {
		// TODO Auto-generated method stub
		
        HttpClient client = new DefaultHttpClient();        
        HttpGet get = new HttpGet(path);          
        try {   
        	 
			Log.i(TAG,"start client.execute");
        	HttpResponse response = client.execute(get);  // 
			Log.i(TAG,"finish client.execute");
			int statusCode = response.getStatusLine().getStatusCode();  
        	String str = String.valueOf(statusCode);  
            if (str.startsWith("4") || str.startsWith("5")) {  
            	Log.v("DownThread","The URL is not exist");   
                return ;  
            } 
            HttpEntity entity = response.getEntity();   
            long fileSize = entity.getContentLength(); 
                     
			Log.i(TAG,"fileSize=:"+fileSize);
            InputStream is = entity.getContent();   
            FileOutputStream fileOutputStream = null;   
            
            if (is != null) {   
                File file = new File(targetFile);   
                fileOutputStream = new FileOutputStream(file);   
               
                    byte[] buffer=new byte[1024*100];
    	        	int hasRead=0;
    	        	//long oldCount=0;
    	        	while((hasRead=is.read(buffer))!=-1)
    	        	{ fileOutputStream.write(buffer, 0, hasRead);
    	              count_size+=hasRead;
    	        	   Log.i(TAG,"count_size="+count_size);
    	        	 
    	        	    	  Message msg=new Message();
    	        		      msg.what=(int) (count_size*100/fileSize);  
      	        		      msg.obj=fileSize;
      	                      handler.sendMessage(msg);
    	        	      
    	        	 }           
            }   
            fileOutputStream.flush();   
            if (fileOutputStream != null) {   
                fileOutputStream.close();   
            }   
           
        } catch (ClientProtocolException e) {   
            e.printStackTrace(); 
            System.out.println("ClientProtocolException :downloading fail......");          
        } catch (IOException e) { 
              e.printStackTrace();   
        	  System.out.println("IOException :downloading fail......");
        	  Message msg=new Message();
		      msg.what=HANDLE_DOWNLOAD_FAIL;  
              handler.sendMessage(msg);
              return;
        } 
	}
}
