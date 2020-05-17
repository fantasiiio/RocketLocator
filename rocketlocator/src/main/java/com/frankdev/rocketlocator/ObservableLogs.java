package com.frankdev.rocketlocator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;

import android.util.Log;

public class ObservableLogs extends Observable {
	private String logText = "";
	private String logFileName;
	private boolean fileLogEnabled;
	private String logDirectory = "sdcard/rocket/";
	
	public ObservableLogs() {
		Date now = new Date();
		Format formatter = new SimpleDateFormat("yyyyMMddHHmm", java.util.Locale.getDefault());
		String dateStr = formatter.format(now);		
		this.logFileName = "log_" + dateStr + ".log";
	}
	
	public void Clear(){
		this.logText = "";
	}
	
	public void AddToLogs(String msg) {	
		if(fileLogEnabled){
			appendLogFile(msg.replace("\n", ""));
		}
		if(!msg.endsWith("\n")){
			this.logText += msg + "\n";
		}
		else{
			this.logText += msg;
		}
		setChanged();	
		notifyObservers();
	}

	public String getLogText() {
		return logText;
	}
	
	public void d(String tag, String msg){
		Log.d(tag, msg);
		AddToLogs(msg);
	}

	public void e(String tag, String msg, Throwable tr){
		Log.e(tag, msg, tr);
		AddToLogs("error:" + msg);
	}

	public void e(String tag, String msg) {
		Log.e(tag, msg);
		AddToLogs("error:" + msg);
	}

	public void v(String tag, String msg){
		Log.v(tag, msg);
		AddToLogs(msg);
	}

	public void w(String tag, String msg){
		Log.w(tag, msg);
		AddToLogs(msg);
	}
	
	public void appendLogFile(String text) {
		File logFile = new File(logDirectory + logFileName);
		File directory = new File(logDirectory);
		
		if(!directory.exists())
			directory.mkdirs();		
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			Date now = new Date();
			Format formatter = new SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
			String dateStr = formatter.format(now);
			
			// BufferedWriter for performance, true to set append to file flag
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
			buf.append("[" + dateStr + "] " + text);
			buf.newLine();
			buf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isFileLogEnabled() {
		return fileLogEnabled;
	}

	public void setFileLogEnabled(boolean fileLogEnabled) {
		this.fileLogEnabled = fileLogEnabled;
	}

}
