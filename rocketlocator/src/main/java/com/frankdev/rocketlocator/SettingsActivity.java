/*
 * Copyright (C) 2013 Fran�ois Girard
 * 
 * This file is part of Rocket Finder.
 *
 * Rocket Finder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Rocket Finder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Rocket Finder. If not, see <http://www.gnu.org/licenses/>.*/
 
package com.frankdev.rocketlocator;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnSharedPreferenceChangeListener{

	public static final String PREF_USE_BLE = "ble";
	public static final String PREF_BLUETOOTH_DEVICE = "bluetoothDevice";
	public static final String PREF_ABOUT = "about";
	public static final String PREF_LOGS_ENABLED = "logsEnabled";
	public static final String PREF_MEASURE_UNIT = "measureUnit";

	private static final String LOG_TAG = "RocketLocator";

	private SharedPreferences sharedPref ;
	private BluetoothAdapter bluetoothAdapter;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);      
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final Preference pref = findPreference(SettingsActivity.PREF_ABOUT);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {		
			@Override
			public boolean onPreferenceClick(Preference preference) {
				SettingsActivity.this.displayAboutDialog();
				return true;
			}
		});
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
        	findPreference(SettingsActivity.PREF_USE_BLE).setEnabled(false);
		}
	}

	@Override
	protected void onResume() {
		this.updateDevicePreferenceSummary();
		super.onResume();
	}

	private void updateMeasureUnit() {

	}

	@SuppressWarnings("deprecation")
	private void updateDevicePreferenceSummary(){
        // update bluetooth device summary
		String deviceName = "";
        ListPreference prefDevices = (ListPreference)findPreference(SettingsActivity.PREF_BLUETOOTH_DEVICE);
        String deviceAddress = sharedPref.getString(SettingsActivity.PREF_BLUETOOTH_DEVICE, null);
        if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)){
        	deviceName = bluetoothAdapter.getRemoteDevice(deviceAddress).getName();
        }
        prefDevices.setSummary(getString(R.string.pref_bluetooth_device_summary, deviceName));
    } 

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {


		if (SettingsActivity.PREF_BLUETOOTH_DEVICE.equals(key)){
			this.updateDevicePreferenceSummary();
		} else if(SettingsActivity.PREF_LOGS_ENABLED.equals(key)){
			this.enableFileLog(sharedPreferences);
		} else if(SettingsActivity.PREF_MEASURE_UNIT.equals(key)) {
			this.updateMeasureUnit();
		}
		this.updateDevicePreferenceSummary();
	}

	private void enableFileLog(SharedPreferences sharedPreferences) {
		boolean logEnabled = sharedPreferences.getBoolean(SettingsActivity.PREF_LOGS_ENABLED, false);
		SharedHolder.getInstance().getLogs().setFileLogEnabled(logEnabled);
	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	private void displayAboutDialog(){
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
       
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.about_title);
		builder.setIcon(R.drawable.gplv3_icon);
        builder.setView(messageView);
		builder.show();
	}
	
}
