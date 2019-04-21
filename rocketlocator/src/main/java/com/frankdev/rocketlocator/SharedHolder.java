package com.frankdev.rocketlocator;

import org.broeuschmeul.android.gps.bluetooth.provider.BluetoothGpsManager;

public class SharedHolder {
	public static int maxDownloadDepth = 4;
	public static int maxZoom = 20;

	private BluetoothGpsManager blueGpsMan;
	private final ObservableLogs logs = new ObservableLogs();
	
	public BluetoothGpsManager getBlueGpsMan() {
		return blueGpsMan;
	}

	public void setBlueGpsMan(BluetoothGpsManager blueGpsMan) {
		this.blueGpsMan = blueGpsMan;
	}

	private static final SharedHolder holder = new SharedHolder();

	public static SharedHolder getInstance() {
		return holder;
	}

	public ObservableLogs getLogs() {
		return logs;
	}
}