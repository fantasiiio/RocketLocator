/*
 * Copyright (C) 2010, 2011, 2012 Herbert von Broeuschmeul
 * Copyright (C) 2010, 2011, 2012 BluetoothGPS4Droid Project
 * 
 * This file is part of BluetoothGPS4Droid.
 *
 * BluetoothGPS4Droid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * BluetoothGPS4Droid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with BluetoothGPS4Droid. If not, see <http://www.gnu.org/licenses/>.
 */

package org.broeuschmeul.android.gps.bluetooth.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.broeuschmeul.android.gps.nmea.util.NmeaParser;

import com.frankdev.rocketlocator.NmeaValues;
import com.frankdev.rocketlocator.R;
import com.frankdev.rocketlocator.SharedHolder;
import com.frankdev.rocketlocator.Sounds;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.widget.Toast;

/**
 * This class is used to establish and manage the connection with the bluetooth GPS.
 * 
 * @author Herbert von Broeuschmeul
 *
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ClassicBluetoothGpsManager extends BluetoothGpsManager {
	final Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msgs) {
        //write your code hear which give error
        }
     };
	
	public static BluetoothGpsManager instance;
	/**
	 * Tag used for log messages
	 */
	private static final String LOG_TAG = "BlueGPS";

	/**
	 * A utility class used to manage the communication with the bluetooth GPS whn the connection has been established.
	 * It is used to read NMEA data from the GPS or to send SIRF III binary commands or SIRF III NMEA commands to the GPS.
	 * You should run the main read loop in one thread and send the commands in a separate one.   
	 * 
	 * @author Herbert von Broeuschmeul
	 *
	 */
	private class ConnectedGps extends Thread {
		/**
		 * GPS bluetooth socket used for communication. 
		 */
		private final BluetoothSocket socket;
		/**
		 * GPS InputStream from which we read data. 
		 */
		private final InputStream gpsInputStream;
		/**
		 * GPS output stream to which we send data (SIRF III binary commands). 
		 */
		//private final OutputStream out;
		/**
		 * GPS output stream to which we send data (SIRF III NMEA commands). 
		 */
		//private final PrintStream out2;
		/**
		 * A boolean which indicates if the GPS is ready to receive data. 
		 * In fact we consider that the GPS is ready when it begins to sends data...
		 */
		private boolean ready = false;

		public ConnectedGps(BluetoothSocket socket) {
			this.socket = socket;
			
			InputStream tmpIn = null;
			try {
				tmpIn = socket.getInputStream();
			} catch (IOException e) {
				SharedHolder.getInstance().getLogs().e(LOG_TAG, "error while getting socket streams", e);
			}	
			gpsInputStream = tmpIn;
		}
	

		@Override
		public void run() {
			String sentenseStr;
			try {
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(gpsInputStream,"US-ASCII"));
				long now = SystemClock.uptimeMillis();
				long lastRead = now;
				
				while((enabled)){
					if (reader.ready()){
						sentenseStr = "";
						//gps connected
						if(ready == false) {
							Sounds.gps_connected.start();
							SharedHolder.getInstance().getLogs().v(LOG_TAG,"GPS Connected");
							ready = true;
						}
						
						//Start timeout thread for reading gps
						ExecutorService executor = Executors.newSingleThreadExecutor();
						ReaderTask readerTask = new ReaderTask(reader);						
						Future<String> future = executor.submit(readerTask);

				        try {
				        	//Wait for data and throw TimeoutException if wait for more than 3 sec
				            sentenseStr = future.get(3, TimeUnit.SECONDS);
				        } catch (TimeoutException e) {
				        	SharedHolder.getInstance().getLogs().v(LOG_TAG,"GPS Disconnected");
							Sounds.gps_disconnected.start();
				        	ready = false;
				        } catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
				        
				        if(enabled && sentenseStr != null && !sentenseStr.isEmpty()){
							//SharedHolder.getInstance().getLogs().v(LOG_TAG, "data: "+System.currentTimeMillis()+" "+s);
							notifyNmeaSentence(sentenseStr);
				        }
						lastRead = SystemClock.uptimeMillis();
					} else {
						if(now - lastRead > 3000 && ready)
						{
				        	SharedHolder.getInstance().getLogs().v(LOG_TAG,"GPS Disconnected");
							Sounds.gps_disconnected.start();
							ready = false;		
							//disable(true);
							throw new Exception("GPS Disconnected");
							//enable();
							//reader.close();
							//reader = new BufferedReader(new InputStreamReader(gpsInputStream,"US-ASCII"));
						}
						/*if(!ready){
							SharedHolder.getInstance().getLogs().d(LOG_TAG, "data: not ready "+System.currentTimeMillis());
						}*/
						SystemClock.sleep(500);
					}
					now = SystemClock.uptimeMillis();
				}
			} catch (Exception e) {
				//hasError = true;
				SharedHolder.getInstance().getLogs().e(LOG_TAG, "error while getting data", e);
				//setMockLocationProviderOutOfService();
			} finally {
				// cleanly closing everything...
				this.close();
				//disableIfNeeded();
				//BlueetoothGpsManager.instance.startConnectThread();
			}
						
		}

		class ReaderTask implements Callable<String> {
			public BufferedReader reader;
			
			public ReaderTask(BufferedReader reader){
				this.reader = reader;
			}
		    @Override
		    public String call() throws Exception {
		    	//reader
		    	return reader.readLine();
		    }
		}		
		
		public void close(){
			//playSound(R.raw.bluetooth_disconnected);			
			
			ready = false;
			try {
	        	SharedHolder.getInstance().getLogs().d(LOG_TAG, "closing Bluetooth GPS output sream");
				gpsInputStream.close();
			} catch (IOException e) {
				SharedHolder.getInstance().getLogs().e(LOG_TAG, "error while closing GPS NMEA output stream", e);
			} finally {
				try {
		        	SharedHolder.getInstance().getLogs().d(LOG_TAG, "closing Bluetooth GPS socket");
					socket.close();
				} catch (IOException e) {
					SharedHolder.getInstance().getLogs().e(LOG_TAG, "error while closing GPS socket", e);
				}
			}
		}
	}

	private Context context;
	private BluetoothSocket gpsSocket;
	private String gpsDeviceAddress;
	private boolean enabled = false;
	private ScheduledExecutorService connectionAndReadingPool;
	private ConnectedGps connectedGps;
	private int disableReason = 0;

	private int maxConnectionRetries;
	private int nbRetriesRemaining;
	private boolean connected = false;
	private BluetoothAdapter bluetoothAdapter = null;
	private BluetoothDevice gpsDevice = null;
	/**
	 * @param context
	 * @param deviceAddress
	 * @param maxRetries
	 */
	public ClassicBluetoothGpsManager(Context context, String deviceAddress, int maxRetries) {
		this.context = context;
		ClassicBluetoothGpsManager.instance = this;
		this.gpsDeviceAddress = deviceAddress;
		this.maxConnectionRetries = maxRetries;
		this.nbRetriesRemaining = 1+maxRetries;
	}

	private void setDisableReason(int reasonId){
		disableReason = reasonId;
	}
	
	/**
	 * @return
	 */
	@Override
	public int getDisableReason(){
		return disableReason;
	}
	
	/**
	 * @return true if the bluetooth GPS is enabled
	 */
	@Override
	public synchronized boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables the bluetooth GPS Provider.
	 * @return
	 */
	@Override
	public synchronized void enable() {
		//notificationManager.cancel(R.string.service_closed_because_connection_problem_notification_title);
		if (! enabled){
        	SharedHolder.getInstance().getLogs().d(LOG_TAG, "enabling Bluetooth GPS manager");
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	        if (bluetoothAdapter == null) {
	            // Device does not support Bluetooth
				Toast.makeText(context,
						"Device does not support Bluetooth",
						Toast.LENGTH_SHORT).show();
	        	SharedHolder.getInstance().getLogs().e(LOG_TAG, "Device does not support Bluetooth");
	        	disable(R.string.msg_bluetooth_unsupported);
	        } else if (!bluetoothAdapter.isEnabled()) {
	        	// Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        	// startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				Toast.makeText(context,
						"Bluetooth is not enabled",
						Toast.LENGTH_SHORT).show();

	        	SharedHolder.getInstance().getLogs().e(LOG_TAG, "Bluetooth is not enabled");
	        	disable(R.string.msg_bluetooth_disabled);
	        } else {
				gpsDevice = bluetoothAdapter.getRemoteDevice(gpsDeviceAddress);
				if (gpsDevice == null){
					SharedHolder.getInstance().getLogs().e(LOG_TAG, "GPS device not found");       	    	
		        	disable(R.string.msg_bluetooth_gps_unavaible);
				} else {
	    			SharedHolder.getInstance().getLogs().v(LOG_TAG, "current device: "+gpsDevice.getName() + " -- " + gpsDevice.getAddress());
					try {
						gpsSocket = gpsDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
					} catch (IOException e) {
	    				SharedHolder.getInstance().getLogs().e(LOG_TAG, "Error during connection", e);
	    				gpsSocket = null;
					}
					if (gpsSocket == null){
	    				SharedHolder.getInstance().getLogs().e(LOG_TAG, "Error while establishing connection: no socket");
			        	disable(R.string.msg_bluetooth_gps_unavaible);
					} else {
						this.enabled = true;
			        	SharedHolder.getInstance().getLogs().d(LOG_TAG, "Bluetooth GPS manager enabled");
			        	SharedHolder.getInstance().getLogs().v(LOG_TAG, "starting notification thread");
						//notificationPool = Executors.newSingleThreadExecutor();
			        	SharedHolder.getInstance().getLogs().v(LOG_TAG, "starting connection and reading thread");
						connectionAndReadingPool = Executors.newSingleThreadScheduledExecutor();

						startConnectThread();
						Toast.makeText(context,
								"Blue GPS started",
								Toast.LENGTH_SHORT).show();
						
					}
				}
			}
		}
	}

	public void startConnectThread() {
		Runnable connectThread = new Runnable() {							
			@Override
			public void run() {
				do
				{
					tryConnect(bluetoothAdapter, gpsDevice);
				} while(!connected);
			}
		};
		SharedHolder.getInstance().getLogs().v(LOG_TAG, "starting connection to socket task");
		connectionAndReadingPool.scheduleWithFixedDelay(connectThread, 1000, 1000, TimeUnit.MILLISECONDS);
	}
	
	
	private void tryConnect(final BluetoothAdapter bluetoothAdapter,
			final BluetoothDevice gpsDevice) {
		try {
			connected = false;
			SharedHolder.getInstance().getLogs().v(LOG_TAG, "current device: "+gpsDevice.getName() + " -- " + gpsDevice.getAddress());
			if ((bluetoothAdapter.isEnabled()) && (nbRetriesRemaining > 0 )){										
				try {
					if (connectedGps != null){
						connectedGps.close();
					}
					if ((gpsSocket != null) && ((connectedGps == null) || (connectedGps.socket != gpsSocket))){
						SharedHolder.getInstance().getLogs().d(LOG_TAG, "trying to close old socket");
						gpsSocket.close();
					}
				} catch (IOException e) {
					SharedHolder.getInstance().getLogs().e(LOG_TAG, "Error during disconnection", e);
				}
				try {
					gpsSocket = gpsDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
				} catch (IOException e) {
					SharedHolder.getInstance().getLogs().e(LOG_TAG, "Error during connection", e);
					gpsSocket = null;
				}
				if (gpsSocket == null){
					SharedHolder.getInstance().getLogs().e(LOG_TAG, "Error while establishing connection: no socket");
		        	disable(R.string.msg_bluetooth_gps_unavaible);
				} else {
					// Cancel discovery because it will slow down the connection
					bluetoothAdapter.cancelDiscovery();
					// we increment the number of connection tries
					// Connect the device through the socket. This will block
					// until it succeeds or throws an exception
					SharedHolder.getInstance().getLogs().v(LOG_TAG, "connecting to socket");
					gpsSocket.connect();
					SharedHolder.getInstance().getLogs().d(LOG_TAG, "connected to socket");
					connected = true;
					// reset eventual disabling cause
//											setDisableReason(0);
					// connection obtained so reset the number of connection try
					nbRetriesRemaining = 1+maxConnectionRetries ;
					//notificationManager.cancel(R.string.connection_problem_notification_title);
					SharedHolder.getInstance().getLogs().v(LOG_TAG, "starting socket reading task");
					connectedGps = new ConnectedGps(gpsSocket);
					
					connectionAndReadingPool.execute(connectedGps);					
		        	SharedHolder.getInstance().getLogs().v(LOG_TAG, "socket reading thread started");
				}
			}
		} catch (IOException connectException) {
			// Unable to connect
			Sounds.gps_disconnected.start();
			SharedHolder.getInstance().getLogs().e(LOG_TAG, "error while connecting to socket", connectException);									
			// disable(R.string.msg_bluetooth_gps_unavaible);
		} finally {
			nbRetriesRemaining--;
			if (! connected) {
				disableIfNeeded();
			}
		}
	}	

	/**
	 * Disables the bluetooth GPS Provider if the maximal number of connection retries is exceeded.
	 * This is used when there are possibly non fatal connection problems. 
	 * In these cases the provider will try to reconnect with the bluetooth device 
	 * and only after a given retries number will give up and shutdown the service.
	 */
	private synchronized void disableIfNeeded(){
		if (enabled){
			if (nbRetriesRemaining > 0){
				SharedHolder.getInstance().getLogs().e(LOG_TAG, "Unable to establish connection");
			} else {
				disable(R.string.msg_two_many_connection_problems);
			}
		}
	}
	
	/**
	 * Disables the bluetooth GPS provider.
	 * 
	 * It will: 
	 * <ul>
	 * 	<li>close the connection with the bluetooth device</li>
	 * 	<li>disable the Mock Location Provider used for the bluetooth GPS</li>
	 * 	<li>stop the BlueGPS4Droid service</li>
	 * </ul>
	 * The reasonId parameter indicates the reason to close the bluetooth provider. 
	 * If its value is zero, it's a normal shutdown (normally, initiated by the user).
	 * If it's non-zero this value should correspond a valid localized string id (res/values..../...) 
	 * which will be used to display a notification.
	 * 
	 * @param reasonId	the reason to close the bluetooth provider.
	 */
	@Override
	public synchronized void disable(int reasonId) {
    	//SharedHolder.getInstance().getLogs().d(LOG_TAG, "disabling Bluetooth GPS manager reason: "+callingService.getString(reasonId));
		setDisableReason(reasonId);
    	disable(false);
	}
		
	/**
	 * Disables the bluetooth GPS provider.
	 * 
	 * It will: 
	 * <ul>
	 * 	<li>close the connection with the bluetooth device</li>
	 * 	<li>disable the Mock Location Provider used for the bluetooth GPS</li>
	 * 	<li>stop the BlueGPS4Droid service</li>
	 * </ul>
	 * If the bluetooth provider is closed because of a problem, a notification is displayed.
	 */
	@Override
	public synchronized void disable(final boolean restart) {
		if (enabled){
        	SharedHolder.getInstance().getLogs().d(LOG_TAG, "disabling Bluetooth GPS manager");
			enabled = false;
			connectionAndReadingPool.shutdown();

			try {
				connectionAndReadingPool.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (!connectionAndReadingPool.isTerminated()){
				connectionAndReadingPool.shutdownNow();
				if (connectedGps != null){
					connectedGps.close();
				}
				if ((gpsSocket != null) && ((connectedGps == null) || (connectedGps.socket != gpsSocket))){
					try {
						SharedHolder.getInstance().getLogs().d(LOG_TAG, "closing Bluetooth GPS socket");
						gpsSocket.close();
					} catch (IOException closeException) {
						SharedHolder.getInstance().getLogs().e(LOG_TAG, "error while closing socket", closeException);
					}
				}
			}
			if(restart)
			{
				enable();
			}

        	SharedHolder.getInstance().getLogs().d(LOG_TAG, "Bluetooth GPS manager disabled");
		}
	}


}
