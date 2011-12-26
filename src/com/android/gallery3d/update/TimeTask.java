package com.android.gallery3d.update;

import com.android.gallery3d.app.Gallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

public class TimeTask extends BroadcastReceiver {
	 private String TAG="TimeTast";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		String str=intent.getStringExtra("UPDATE");
		Log.v(TAG,"get the String from intent:"+str);
		Log.v(TAG," -----onReceive--------");
		if(str.equals("ABLE")){
		 SharedPreferences.Editor mEditor=Gallery.mableUpdate.edit();
		 mEditor.putString("UPDATE",str);
		 mEditor.commit();
		 Log.v(TAG,"The checkUpdate mode has change to "+Gallery.mableUpdate.getString("UPDATE",null));

		}
	}



}
