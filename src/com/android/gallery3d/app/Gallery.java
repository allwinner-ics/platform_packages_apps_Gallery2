/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import com.android.gallery3d.update.CheckUpdate;
import com.android.gallery3d.update.DownThread;


import java.util.Timer;
import java.util.TimerTask;
import android.content.ServiceConnection;
import android.app.Service;
import android.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.os.Message;
import android.os.Handler;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Context;
import android.content.ComponentName;
import android.os.IBinder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.os.AsyncTask;
import android.widget.RemoteViews;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.util.GalleryUtils;

public final class Gallery extends AbstractGalleryActivity implements OnCancelListener {
    public static final String EXTRA_SLIDESHOW = "slideshow";
    public static final String EXTRA_CROP = "crop";

    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    public static final String KEY_GET_CONTENT = "get-content";
    public static final String KEY_GET_ALBUM = "get-album";
    public static final String KEY_TYPE_BITS = "type-bits";
    public static final String KEY_MEDIA_TYPES = "mediaTypes";
	private Dialog mVersionCheckDialog;
	public static SharedPreferences mableUpdate;
    private static final String TAG = "Gallery";
    private GalleryActionBar mActionBar;
	private static final int HANDLE_DOWNLOAD_FINISH=100;
	private static final int HANDLE_DOWNLOAD_FAIL=111;
	private static final int HANDLE_UPDATE_SUCCEED=222;
	private static final int HANDLE_UPDATE_FAIL=333;
    private int downloadStatus=0;
	private long fileSize=0;  
	private Context mContext;
	private int tryNum=0;
	private boolean downloadFinish=false;
	private DownloadTask mDownloadTask=new DownloadTask();
	private NotificationManager notificationManager;
	private boolean downloadOut=false;
	private final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {			
				switch (msg.what) {
					case HANDLE_DOWNLOAD_FINISH:
						 Log.v(TAG,"download apk finish");
						 downloadFinish=true;
	    	             downloadStatus=msg.what;
						 mPro.cancel();
	    	             isInstallFinish();
						break;
					case HANDLE_UPDATE_SUCCEED:
						showDialog();
						break;
					case HANDLE_UPDATE_FAIL:
						Log.e(TAG,"check update  fail");
						break;
					case HANDLE_DOWNLOAD_FAIL:
						 if(tryNum++<2){
						 	mPro.cancel();
						    download =new Thread(new DownThread(newURL,TARGET,handler));
                            download.start(); 
    	                    mPro.setProgress(0);
    	                    mPro.show();
						 }
						 if(tryNum==2){
						 	mPro.cancel();
						 	} 
						 
						break;
				}
			}
		};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.main);
        mActionBar = new GalleryActionBar(this);

		mableUpdate=getSharedPreferences("ABLE_UPDATE",MODE_PRIVATE);
        if (savedInstanceState != null) {
            getStateManager().restoreFromState(savedInstanceState);
        } else {
            initializeByIntent();
        }
		checkUpdate();
    }

    private void initializeByIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_GET_CONTENT.equalsIgnoreCase(action)) {
            startGetContent(intent);
        } else if (Intent.ACTION_PICK.equalsIgnoreCase(action)) {
            // We do NOT really support the PICK intent. Handle it as
            // the GET_CONTENT. However, we need to translate the type
            // in the intent here.
            Log.w(TAG, "action PICK is not supported");
            String type = Utils.ensureNotNull(intent.getType());
            if (type.startsWith("vnd.android.cursor.dir/")) {
                if (type.endsWith("/image")) intent.setType("image/*");
                if (type.endsWith("/video")) intent.setType("video/*");
            }
            startGetContent(intent);
        } else if (Intent.ACTION_VIEW.equalsIgnoreCase(action)
                || ACTION_REVIEW.equalsIgnoreCase(action)){
            startViewAction(intent);
        } else {
            startDefaultPage();
        }
    }

    public void startDefaultPage() {
        PicasaSource.showSignInReminder(this);
        Bundle data = new Bundle();
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
        getStateManager().startState(AlbumSetPage.class, data);
        mVersionCheckDialog = PicasaSource.getVersionCheckDialog(this);
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.setOnCancelListener(this);
        }
    }

    private void startGetContent(Intent intent) {
        Bundle data = intent.getExtras() != null
                ? new Bundle(intent.getExtras())
                : new Bundle();
        data.putBoolean(KEY_GET_CONTENT, true);
        int typeBits = GalleryUtils.determineTypeBits(this, intent);
        data.putInt(KEY_TYPE_BITS, typeBits);
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(typeBits));
        getStateManager().setLaunchGalleryOnTop(true);
        getStateManager().startState(AlbumSetPage.class, data);
    }

    private String getContentType(Intent intent) {
        String type = intent.getType();
        if (type != null) return type;

        Uri uri = intent.getData();
        try {
            return getContentResolver().getType(uri);
        } catch (Throwable t) {
            Log.w(TAG, "get type fail", t);
            return null;
        }
    }

    private void startViewAction(Intent intent) {
        Boolean slideshow = intent.getBooleanExtra(EXTRA_SLIDESHOW, false);
        getStateManager().setLaunchGalleryOnTop(true);
        if (slideshow) {
            getActionBar().hide();
            DataManager manager = getDataManager();
            Path path = manager.findPathByUri(intent.getData());
            if (path == null || manager.getMediaObject(path)
                    instanceof MediaItem) {
                path = Path.fromString(
                        manager.getTopSetPath(DataManager.INCLUDE_IMAGE));
            }
            Bundle data = new Bundle();
            data.putString(SlideshowPage.KEY_SET_PATH, path.toString());
            data.putBoolean(SlideshowPage.KEY_RANDOM_ORDER, true);
            data.putBoolean(SlideshowPage.KEY_REPEAT, true);
            getStateManager().startState(SlideshowPage.class, data);
        } else {
            Bundle data = new Bundle();
            DataManager dm = getDataManager();
            Uri uri = intent.getData();
            String contentType = getContentType(intent);
            if (contentType == null) {
                Toast.makeText(this,
                        R.string.no_such_item, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (uri == null) {
                int typeBits = GalleryUtils.determineTypeBits(this, intent);
                data.putInt(KEY_TYPE_BITS, typeBits);
                data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                        getDataManager().getTopSetPath(typeBits));
                getStateManager().setLaunchGalleryOnTop(true);
                getStateManager().startState(AlbumSetPage.class, data);
            } else if (contentType.startsWith(
                    ContentResolver.CURSOR_DIR_BASE_TYPE)) {
                int mediaType = intent.getIntExtra(KEY_MEDIA_TYPES, 0);
                if (mediaType != 0) {
                    uri = uri.buildUpon().appendQueryParameter(
                            KEY_MEDIA_TYPES, String.valueOf(mediaType))
                            .build();
                }
                Path setPath = dm.findPathByUri(uri);
                MediaSet mediaSet = null;
                if (setPath != null) {
                    mediaSet = (MediaSet) dm.getMediaObject(setPath);
                }
                if (mediaSet != null) {
                    if (mediaSet.isLeafAlbum()) {
                        data.putString(AlbumPage.KEY_MEDIA_PATH, setPath.toString());
                        getStateManager().startState(AlbumPage.class, data);
                    } else {
                        data.putString(AlbumSetPage.KEY_MEDIA_PATH, setPath.toString());
                        getStateManager().startState(AlbumSetPage.class, data);
                    }
                } else {
                    startDefaultPage();
                }
            } else {
                Path itemPath = dm.findPathByUri(uri);
                Path albumPath = dm.getDefaultSetOf(itemPath);
                // TODO: Make this parameter public so other activities can reference it.
                boolean singleItemOnly = intent.getBooleanExtra("SingleItemOnly", false);
                if (!singleItemOnly && albumPath != null) {
                    data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
                            albumPath.toString());
                }
                data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, itemPath.toString());
                getStateManager().startState(PhotoPage.class, data);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return getStateManager().createOptionsMenu(menu);
    }
	@Override
	 public boolean onPrepareOptionsMenu(Menu menu)
    {
    	super.onPrepareOptionsMenu(menu);
				
	  	MenuItem update=menu.findItem(R.id.action_update);
			if(update!=null){
				String mstr=mableUpdate.getString("UPDATE", null);
				if(mstr.equals("ABLE")){       
                    update.setTitle(R.string.close_update_notice);				
			        }else if(mstr.equals("UNABLE")){      
					    update.setTitle(R.string.allow_update_notice);
						 
				     } 
				}
		return true;
  

	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }

    @Override
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().onBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().destroy();
        } finally {
            root.unlockRenderThread();
        }
		if(serviceCon==true){
		   unbindService(conn);
		  timer.cancel();
			}
		mDownloadTask.cancel(true);
		downloadOut=true;
    }

    @Override
    protected void onResume() {
        Utils.assertTrue(getStateManager().getStateCount() > 0);
        super.onResume();
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.show();
        }
		resumeNum++;
		if(downloadFinish&&!isPackageExist(packageName)&&resumeNum<=2){
			isInstallFinish();
			}
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.dismiss();
        }
    }

    @Override
    public GalleryActionBar getGalleryActionBar() {
        return mActionBar;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == mVersionCheckDialog) {
            mVersionCheckDialog = null;
        }
    }

	CheckUpdate.MyBinder binder;
	String newVerName;
	String newURL;
	Timer timer=new Timer();
	boolean serviceCon=false;	
	int checkTime=0;
	String apkName;
    String TARGET ="/mnt/sdcard/Download/";
	File DOWN_DIR = new File(TARGET); 
	String packageName;
    Thread download;
	ProgressDialog mPro;
    boolean upAble=false;
	int resumeNum=0;
	String describe;
    public void checkUpdate()
    {	
	    packageName=mableUpdate.getString("packageName",null);
        if(isPackageExist(packageName)) {
			Log.v(TAG,packageName+" is exist!");
			return;
			} 
		notificationManager = (NotificationManager) Gallery.this.getSystemService(NOTIFICATION_SERVICE);
        mPro =new ProgressDialog(Gallery.this);
        if( !writePreference()) return;
    	if(!checkInternet())return;
    	Intent intent =new Intent();
    	intent.setAction("org.crazyit.anction.SERVICE");
    	bindService(intent,conn,Service.BIND_AUTO_CREATE);        
    }
   private void  runTimer()
   {    
	   timer.schedule(new TimerTask(){

			@Override
			public void run() {
				// TODO Auto-generated method stub				
				Message msg=new Message();
				boolean status=binder.isReadFinish();
				boolean isFail=binder.getIsFail();
				if(isFail)
				{	
					timer.cancel();
					this.cancel();
				}
				if(checkTime++>=20&&isFail)
				{			  
					  msg.what=HANDLE_UPDATE_FAIL;
					  handler.sendMessage(msg);
					  timer.cancel();
					  this.cancel();
				}
				if(status)
				{   	      
				    if(binder.isDiscoverableNew())	
				         { 
				          newURL=binder.getNewURL();
					      newVerName=binder.getNewVerName();
						  apkName=binder.getApkName();
						  TARGET=TARGET+apkName;
						  describe=binder.getDescribe();
					      Log.i(TAG,"newVerName"+newVerName); 
				          timer.cancel();
						  this.cancel();
				          msg.what=HANDLE_UPDATE_SUCCEED;
				          handler.sendMessage(msg);
				         }else 
				          timer.cancel();
				}
			}
   		
   	}, 0, 1000);
	   
   }
   public void showDialog()
   { 
          
          final Builder builder= new AlertDialog.Builder(Gallery.this);
		  LayoutInflater inflater=(LayoutInflater)getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);   				 
	      View view=inflater.inflate(R.layout.dialog_update, null);
		  TextView text=(TextView)view.findViewById(R.id.update_massege);
		  text.setText(describe);
		  final Button mSure=(Button)view.findViewById(R.id.sure);
		  final Button mCancel=(Button)view.findViewById(R.id.cancel);
		  final ImageView mCheckbox=(ImageView)view.findViewById(R.id.checkbox);
		  builder.setView(view);  
		  final AlertDialog dialog=builder.create();
		  dialog.show();
		  mCancel.setOnClickListener(new View.OnClickListener() {		
			 @Override
			 public void onClick(View v) {
						// TODO Auto-generated method stub
				mCancel.setBackgroundResource(R.drawable.dialog_button_background_2);
				if(upAble){
    	             SharedPreferences.Editor mEditor=mableUpdate.edit();
					 mEditor.putString("UPDATE", "UNABLE");
    		         mEditor.commit();	
					 Log.v(TAG,mableUpdate.getString("UPDATE", null));
				}
			    dialog.dismiss();
						
					}
			});
		  mSure.setOnClickListener(new View.OnClickListener() {			
			 @Override
			 public void onClick(View v) {
							// TODO Auto-generated method stub
				DOWN_DIR.mkdir();
			    mDownloadTask.execute(newURL,TARGET);
				mSure.setBackgroundResource(R.drawable.dialog_button_background_2);
			    dialog.dismiss();
				}
			}); 
				  
           mCheckbox.setOnClickListener(new View.OnClickListener() {
			    @Override
			    public void onClick(View v) {
				// TODO Auto-generated method stub
				      if(upAble){
				        mCheckbox.setImageResource(R.drawable.dialog_checkbox_1);
						 upAble=false;
				      	}
						else{
							 mCheckbox.setImageResource(R.drawable.dialog_checkbox_2);
							 upAble=true;
						 }
				     
			       }
		   });     
	   
   }
    public ServiceConnection conn=new ServiceConnection()
    {
        public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub\
		    binder=(CheckUpdate.MyBinder)service;
			System.out.println("---Service Connected--");
			serviceCon=true;
		    runTimer();
			
		}
        public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub	
		}
    	
    };
    public boolean writePreference()
	{
    	SharedPreferences.Editor mEditor=mableUpdate.edit();
    	
    	String str=mableUpdate.getString("UPDATE", null);
		Log.v(TAG,"starting with the mode "+str);
    	if(str==null){
    		mEditor.putString("UPDATE", "ABLE");
    		mEditor.commit();	
    	}else if(str.equals("UNABLE")){
    	      Log.v(TAG,"It's the mode that unable to check update ");
    	      return false;
		}   
		return true;	
	}
	public  boolean isInstallFinish(){
        SharedPreferences.Editor mEditor=mableUpdate.edit();
		mEditor.putBoolean("has_download",true);
		mEditor.commit();
		Intent intent = new Intent(Intent.ACTION_VIEW);   
	    intent.setDataAndType(Uri.fromFile(new File(DOWN_DIR, mableUpdate.getString("apkName",null))),   
				            "application/vnd.android.package-archive");   
	    startActivity(intent); 
         return true;
	}
    public boolean checkInternet()
    {
      ConnectivityManager cm=(ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo info=cm.getActiveNetworkInfo();	  
	  SharedPreferences.Editor mEditor=mableUpdate.edit();
	  int connNum=mableUpdate.getInt("ConnNum",0);
	  connNum++;
	  Log.v(TAG,"The number of connecting is "+connNum);
      if(info!=null&&info.isConnected())
       {      
		return true;
        
       }
       else
        {            
         Log.v(TAG,"It's can't connect the Internet!");
          return false;
         }    
    }
	 private String setStr(long size,int status)
    {
        String str=null;
    	int k=(int) (size/1024);
    	if(k<1024)
    	{
    	str=status*k/100+"K/"+k+"K";
    	}
    	else if(k>=1024)
    	  {
    		int m=k/1024;
    		    k=k%1024;
    	    str=m*status/100+"."+ status*k/100+"Mb/"+m+"."+k+"Mb";
    	  }
    	return str;	
    }
	public boolean isPackageExist(String packagename)
    {
    	PackageManager pm=getPackageManager();
    	String name=null;
		      try {
		    	PackageInfo info =pm.getPackageInfo(packagename,0);
				Log.i(TAG,packagename+" is exist");
				name=info.applicationInfo.className;
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				Log.i(TAG,packagename+" is not exist");
				return false;
			}
		Log.i(TAG,"packagename="+name);
    	return true;
    	
    }
	private  class DownloadTask extends AsyncTask<String,Integer,Integer> {
    	public ProgressDialog mPro;
    	public long fileSize;
		public int status=0;
        private long count_size=0;
        private int percent=0;
		 RemoteViews view = null;
    	 Notification notification;
    	 Intent intent;
    	 PendingIntent pendingIntent;
		 Timer mTimer=new Timer();
    	protected void onPreExecute (){	
    	 
    	 intent = new Intent();
    	 pendingIntent = PendingIntent.getActivity(Gallery.this, 0, intent, 0);
    	 notification = new Notification (R.drawable.download_current,getResources().getString(R.string.download_massege),0);
    	 view = new RemoteViews(getPackageName(), R.layout.notificat_progress);
    	 view.setProgressBar(R.id.progress, 100, 0, false);
		 view.setTextViewText(R.id.percent,  0 + "%");
		 notification.defaults = Notification.FLAG_ONLY_ALERT_ONCE;
		 notification.icon = R.drawable.download_current;
		 notification.contentView = view;
		 notification.contentIntent = pendingIntent;
		 notificationManager.notify(0, notification);     	
    	}
        protected  Integer doInBackground(String... param) {
        	String URL=param[0];
        	String dir=param[1];

			 mTimer.schedule(new TimerTask(){
			   @Override
			   public void run() {
			     publishProgress(percent);		
			  }
             }, 0, 1000);
        	HttpClient client = new DefaultHttpClient();        
            HttpGet get = new HttpGet(URL);          
            try {   
    			Log.i(TAG,"start client.execute");
            	HttpResponse response = client.execute(get);  // 
    			Log.i(TAG,"finish client.execute");
    			int statusCode = response.getStatusLine().getStatusCode();  
            	String str = String.valueOf(statusCode);  
                if (str.startsWith("4") || str.startsWith("5")) {  
                	Log.v("DownloadTask","The URL is not exist");   
                    return 0;  
                } 
                HttpEntity entity = response.getEntity();   
                fileSize = entity.getContentLength();         
    			Log.i(TAG,"fileSize=:"+fileSize);
                InputStream is = entity.getContent();   
                FileOutputStream fileOutputStream = null;   
                if (is != null) {   
                    File file = new File(dir);   
                    fileOutputStream = new FileOutputStream(file);   
                   
                        byte[] buffer=new byte[1024*100];
        	        	int hasRead=0;
        	        	//long oldCount=0;
        	        	while((hasRead=is.read(buffer))!=-1)
        	        	{ fileOutputStream.write(buffer, 0, hasRead);
        	              count_size+=hasRead;
        	        	   Log.i(TAG,"count_size="+count_size);
        	        	   percent=(int) (count_size*100/fileSize);
        	        	    if(percent==100)  {
                                Message msg=new Message();
		                        msg.what=HANDLE_DOWNLOAD_FINISH;  
                                Gallery.this.handler.sendMessage(msg);
							    notificationManager.cancel(0);
							}
							if(downloadOut){
                               notificationManager.cancel(0);
							   mTimer.cancel();
							   fileOutputStream.flush();
							   fileOutputStream.close(); 
							   return 0;
							}
							
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
                  return 0;
            }
			return 1; 
           
        }

        protected void onProgressUpdate(Integer... param) {
			   view.setProgressBar(R.id.progress, 100, param[0], false);
		       view.setTextViewText(R.id.percent, setStr(fileSize,param[0]));	 
			   notificationManager.notify(0, notification);
			  if(param[0]==100){
			  	  notificationManager.cancel(0);
				  mTimer.cancel();
			  	}
        	
        }
        protected void onPostExecute(Integer... result) {
			Log.v(TAG,"download onPostExecute");
             if(result[0]==1){
			 	Log.v(TAG,"download finish");
                 
			 } else {
					   
			       }
		             
			 	
        }
	    private String setStr(long size,int status)
         {
        String str=null;
    	int k=(int) (size/1024);
    	if(k<1024)
    	{
    	str=status*k/100+"K/"+k+"K";
    	}
    	else if(k>=1024)
    	  {
    		int m=k/1024;
    		    k=k%1024;
    	    str=m*status/100+"."+ status*k/100+"Mb/"+m+"."+k+"Mb";
    	  }
    	return str;
    	
       }
    }
}
