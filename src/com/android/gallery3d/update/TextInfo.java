package com.android.gallery3d.update;

import com.android.gallery3d.app.Gallery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;



import android.util.Log;

public class TextInfo {
	private String TAG="TextInfo";
	public String[] apkName;
	public String[] verCode;
	public String[] VerName;
	public String[][] URL;
	public String[] Scope;
	public String[] packageName;
	public String[] release;
	public String[] describe;
	public Context mContext;
	
	public String path;
	public int length=0;
	private int URLNum=10;
	
	public TextInfo(String path,Context context)
	{
		this.path=path;
		this.mContext=context;
		readText(path);
		
		
	}
	private void readText(String txt)
	{    String line;
		File file=new File(txt);
		try {
			BufferedReader read=new BufferedReader(new FileReader(file));
			
			while((line=read.readLine())!=null)
			{    	    
			 if (Pattern.matches("^$", line)) {//blank line
			     continue;
			    }
				length++;
			}
			read.close();
			apkName=new String[length];
			verCode=new String[length];
			VerName=new String[length];
			URL=new String[length][URLNum];
			Scope=new String[length];
			packageName=new String[length];
			release=new String[length];
			describe=new String[length];
			int num=0;
			InputStream in=new FileInputStream(file);
			BufferedReader readAgain=new BufferedReader(new InputStreamReader(in,"GB2312"));
			
			while((line=readAgain.readLine())!=null)
				{    	    
				 if (Pattern.matches("^$", line)) {
				     continue;
				    } 
					readValue(line,num);
					num++;
				}
			readAgain.close();
			file.delete();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readValue(String line,int num)
	{   
		int i=0;
		int len2=0;
		String group[]=line.split("#");
		while(i<group.length)
		{   //System.out.println("group:"+group[i]);
			String team[]=group[i].split("=");
			team[0]=removeBlank(team[0]);
			if (!team[0].equals("describe"))
			   team[1]=removeBlank(team[1]);
		
			if(team[0].equals("apkName"))
				{apkName[num]=team[1];
				//System.out.println("test:"+team[1]);
				}
			 else if(team[0].equals("verCode"))
				 verCode[num]=team[1];
			    else if(team[0].equals("VerName"))
			    	VerName[num]=team[1];
				       else if(team[0].equals("Scope"))
					   	 Scope[num]=team[1];
					        else if(team[0].equals("packageName"))
								packageName[num]=team[1];
							     else if (team[0].equals("release"))
								 	 release[num]=team[1];
								        else if(team[0].equals("describe"))
											describe[num]=team[1];
			                                 else {                            
			                                   URL[num][len2++]=team[1];
			                                   //Log.i(TAG,URL[num][len2-1]);
			         	                       } 						
			i++;
		}
	}
	public int getNewVerCode(String Name)
	{   int newVerCode=0;
		for(int i=0;i<length;i++)
		{
			if(Name.equals(apkName[i]))
			{
				int ver=Integer.parseInt(verCode[i]);
				if(newVerCode<ver)
				{
					newVerCode=ver;
				}
			}
		}
		return newVerCode;
		
	}
	public List<String> getNewURL(int num)
	{	
		List<String> list=new ArrayList<String>();
        if(num<length){
          for(int a=0;a<URLNum;a++)
				{ 
				  if(URL[num][a]!=null)
					  list.add(URL[num][a]);
				}
		  return list;
        	}else return null;
		
		
	 }
	public String getNewVerName(String Name)
	{
	    String newVerName=null;
		int newVerCode=getNewVerCode(Name);
		for(int i=0;i<length;i++)
		{
			int ver=Integer.parseInt(verCode[i]);
			if(newVerCode==ver&&Name.equals(apkName[i]))
			{
				newVerName=this.VerName[i];
				
			}
			
		}
       return newVerName;
	}
	public int discoverNew(){
		for(int i=0;i<length;i++)
		{
			if(isPackageExist(packageName[i]))
				continue;
			    else  {
					SharedPreferences.Editor mEditor=Gallery.mableUpdate.edit();						
		            mEditor.putString("packageName",packageName[i]);
		            mEditor.commit();
					return i;

				}
		}
       return -1;
	}
	public String getScope(int num){
		 if(num<length)
		 	 return Scope[num];
		 else
            return null;

	}
	public String getDescribe(int num){
		if(num<length)
		 	 return describe[num];
		 else
            return null;
		 
	}
	public String getPackageName(int num){
		if(num<length)
		 	 return packageName[num];
		 else
            return null;
		 
	}
	public String getRelease(int num){
		if(num<length)
		 	 return release[num];
		 else
            return null;
		 
	}
	public String getApkName(int num)
	{
		return apkName[num];
	}

	public static String removeBlank(String str){ 
        StringBuilder sb = new StringBuilder(); 
        char c=' ';
        char b='<';
        char a='>';
        for(int i=0;i<str.length();i++)
        {
        	char ch=str.charAt(i);
        	if(ch != c&&ch!=a&&ch!=b){ 
                sb.append(ch); 
              } 
        }

        return sb.toString(); 
     }
	public boolean isPackageExist(String packagename)
    {
    	PackageManager pm=mContext.getPackageManager();
    	String name=null;
		      try {
		    	PackageInfo info =pm.getPackageInfo(packagename,0);
				Log.i(TAG,packagename+" is exist");
				name=info.applicationInfo.className;
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
    	Log.i(TAG,"packagename="+name);
    	return true;
    	
    }
	
}
