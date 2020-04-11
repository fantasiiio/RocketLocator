/*
 * Copyright (C) 2013 François Girard
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.broeuschmeul.android.gps.bluetooth.provider.BleBluetoothGpsManager;
import org.broeuschmeul.android.gps.bluetooth.provider.BluetoothGpsManager;
import org.broeuschmeul.android.gps.bluetooth.provider.ClassicBluetoothGpsManager;

import com.frankdev.rocketlocator.TouchableWrapper.OnMapMoveListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

@SuppressLint("HandlerLeak")
public class MainActivity extends FragmentActivity implements
        OnMyLocationChangeListener, SensorEventListener, Observer, OnMapMoveListener, OnMapReadyCallback {

    private final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private final String mapCacheFolder = "mapCache/";
    private final static int REQUEST_ENABLE_BT = 1;
    private GoogleMap mMap;
    private Marker rocketMarker;
    private UiSettings mUiSettings;
    private float rocketDistance = 0;
    private Location rocketLocation;
    private double maxAltitude;
    private Settings settings;
    private Location testLocation;
    List<LatLng> rocketPosList;
    List<Location> rocketLocList;

    private Polyline rocketLine;
    private Polyline rocketPath;
    private static Handler updateHandler;
    private static Handler getLocationHandler;
    private SharedPreferences sharedPreferences;
    //BluetoothGpsManager blueGpsMan;
    // protected int myAzimuth;
    LatLng myPosition;
    private GeomagneticField geoField;
    private long radarDelay;
    private boolean beepEnabled;
    private boolean compassEnabled;
    private SensorManager mSensorManager = null;
    private double altitudeOffset = 0;
    // magnetic field vector
    private float[] magnet = new float[3];

    // accelerometer vector
    private float[] accel = new float[3];

    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];

    private ExponentialAverage accelAverage;
    private ExponentialAverage magnetAverage;
    private double currentAltitude;

    private MyUrlCachedTileProvider mTileProvider;
    private TileCountProvider mTileCountProvider;

    private TileOverlay mTileOverlay;
    private TileOverlay mTileCountOverlay;

    File sdCard;

    ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        rocketLocList = new ArrayList<Location>();
        rocketPosList = new ArrayList<LatLng>();

        setContentView(R.layout.activity_main);

        Sounds.init(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        settings = new Settings(this);
        settings.loadSettings();

        boolean logEnabled = sharedPreferences.getBoolean(SettingsActivity.PREF_LOGS_ENABLED, false);
        SharedHolder.getInstance().getLogs().setFileLogEnabled(logEnabled);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            setUpMapIfNeeded();
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        updateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                updateRocketLocation();
            }
        };

        getLocationHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (mMap == null)
                    return;
                if (msg.obj != null) {
                    CameraPosition camPos = (CameraPosition) msg.obj;
                    mMap.animateCamera(CameraUpdateFactory
                            .newCameraPosition(camPos));
                    initBluetoothGPS();
                } else {
                    testLocation = mMap.getMyLocation();
                }

            }
        };

        checkGPSEnabled();
        checkBluetoothEnabled();
        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initListeners();
        // initCompass();

        WaitForMyLocationThread waitForLoc = new WaitForMyLocationThread();
        waitForLoc.start();

        ToggleButton chkRocketCompass = (ToggleButton) findViewById(R.id.chkRocketCompass);
        chkRocketCompass
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        compassEnabled = isChecked;
                    }
                });

        ToggleButton chkRadarBeep = (ToggleButton) findViewById(R.id.chkRadarBeep);
        chkRadarBeep.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                beepEnabled = isChecked;
            }
        });

        accelAverage = new ExponentialAverage(0.2f);
        magnetAverage = new ExponentialAverage(0.5f);
        radarDelay = 1000;
        beepEnabled = false;
        radarBeepThread radarBeep = new radarBeepThread();
        radarBeep.start();
    }

    private void checkBluetoothEnabled() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void checkGPSEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
                "Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                final int id) {
                                startActivity(new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,
                                        final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void initBluetoothGPS() {
        String deviceAddress = sharedPreferences.getString(
                SettingsActivity.PREF_BLUETOOTH_DEVICE, null);
        if (deviceAddress == null) {
            Toast.makeText(this, "Please choose a bluetooth device",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(getBaseContext(), "Starting blue GPS",
                Toast.LENGTH_SHORT).show();

        BluetoothGpsManager blueGpsMan = SharedHolder.getInstance().getBlueGpsMan();
        if (blueGpsMan == null) {
            if (sharedPreferences.getBoolean(SettingsActivity.PREF_USE_BLE, false)) {
                blueGpsMan = new BleBluetoothGpsManager();
            } else {
                blueGpsMan = new ClassicBluetoothGpsManager(getBaseContext(), deviceAddress, 50);
            }
            SharedHolder.getInstance().setBlueGpsMan(blueGpsMan);
            blueGpsMan.addObserver(this);
            /*
             * {
             *
             * @Override public void onPositionChanged(Location location) {
             * rocketLocation = location; updateHandler.sendEmptyMessage(0);
             * settings .setLastRocketLocation(rocketLocation );
             * settings.saveSettings(); } };
             */
        } else {
            blueGpsMan.disable(false);
        }

        blueGpsMan.enable();

    }

    private float normalizeDegree(float value) {
        if (value >= 0.0f && value <= 180.0f) {
            return value;
        } else {
            return 180 + (180 + value);
        }
    }

    private long getRadarDelay(float value) {
        long ret = (long) Math.abs(value);
        if (ret > 180)
            ret = (long) (360 - ret);

        return Math.min(ret * ret + 20, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            setUpMapIfNeeded();
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            SupportMapFragment fragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
            if (fragment != null) {
                fragment.getMapAsync(this);
            }
        }
    }

    public void onMapReady(GoogleMap map) {
        mMap = map;
        setUpMap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);

        return true;
    }

    private int progressBarStatus = 0;
    private Handler progressBarHandler = new Handler();

    private void createCancelProgressDialog(String title, String message, String buttonText) {
        ProgressDialog cancelDialog = new ProgressDialog(this);
        cancelDialog.setTitle(title);
        cancelDialog.setMessage(message);
        cancelDialog.setButton(DialogInterface.BUTTON_NEGATIVE, buttonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Use either finish() or return() to either close the activity or just the dialog
                return;
            }
        });
        cancelDialog.show();
    }

    boolean downloadCanceled = false;

    private void startDownloadProgress() {
        // prepare for a progress bar dialog
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(false);
        progressBar.setMessage("Downloading...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressBar.setProgress(0);
        progressBar.setMax(1);
        progressBar.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Use either finish() or return() to either close the activity or just the dialog
                downloadCanceled = true;
                mTileProvider.setCancelled(true);
                do {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (mTileProvider.isCancelled());
                return;
            }
        });
        progressBar.show();

        progressBarStatus = 0;

        new Thread(new Runnable() {
            public void run() {
                int tileCount, tileCountTotal;

                progressBar.setMax(100);
                tileCount = mTileProvider.getTileCount();
                tileCountTotal = mTileCountProvider.getTileCount();
                do {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (tileCountTotal > 0) {
                        progressBar.setMax(tileCountTotal);
                        progressBar.setProgress(tileCount);
                    }

                    tileCount = mTileProvider.getTileCount();
                    tileCountTotal = mTileCountProvider.getTileCount();
                } while (tileCount < tileCountTotal && !downloadCanceled);

                downloadCanceled = false;
                // Finished
                progressBar.setMax(tileCountTotal);
                progressBar.setProgress(tileCount);

                mTileProvider.setDownloadDepth(1);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // close the progress bar dialog
                progressBar.dismiss();
            }
        }).start();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // On regarde quel item a été cliqué grâce à son id et on déclenche une
        // action

        switch (item.getItemId()) {
            case R.id.menu_settings:
                // Toast.makeText(this, "Option", Toast.LENGTH_SHORT).show();

                Intent intendSettings = new Intent(this, SettingsActivity.class);
                startActivity(intendSettings);

                return true;
            case R.id.menu_save_path:
                savePath();
                return true;
            case R.id.menu_random_pos:
                Location myLoc = mMap.getMyLocation();
                if (myLoc == null) {
                    Toast.makeText(getBaseContext(),
                            "Map location must be initialized", Toast.LENGTH_SHORT)
                            .show();
                    return true;
                }

                Location lastLocation;
                if (rocketLocation == null)
                    lastLocation = myLoc;
                else
                    lastLocation = rocketLocation;
                rocketLocation = destinationPoint(lastLocation, 90, 10);

                rocketLocation.setLongitude(Math.random() * 0.0002 - 0.0001
                        + lastLocation.getLongitude());
                rocketLocation.setLatitude(Math.random() * 0.0002 - 0.0001
                        + lastLocation.getLatitude());


                rocketLocation.setAltitude((double) ((int) (Math.random() * 500 * 100)) / 100);
                updateRocketLocation();
                return true;
            case R.id.menu_reset_blue_gps:
                initBluetoothGPS();
                return true;
            case R.id.menu_load_last_pos:
                if (settings.getLastRocketLocation() != null) {
                    rocketLocation = settings.getLastRocketLocation();
                    updateRocketLocation();
                }
                return true;
            case R.id.menu_reset_altitude:
                if (rocketLocation != null) {
                    altitudeOffset = rocketLocation.getAltitude();
                    maxAltitude = 0;
                    updateRocketLocation();
                }
                return true;
            case R.id.menu_logs_screen:
                Intent intendLogs = new Intent(this, LogsActivity.class);
                startActivity(intendLogs);
                return true;
            case R.id.menu_provider_osm:
                mTileProvider.SetBaseURL(getString(R.string.provider_baseUrl_osm));
                mTileProvider.providerName = "osm";
                ChangeProvider(R.string.provider_osm_ID);
                return true;
            case R.id.menu_provider_google:
                mTileProvider.SetBaseURL(getString(R.string.provider_baseUrl_google));
                mTileProvider.providerName = "google";
                ChangeProvider(R.string.provider_google_ID);
                return true;
            case R.id.menu_download_map:

                //mTileCountOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mTileCountProvider));
                //mTileCountOverlay.setVisible(false);
                mTileCountProvider.resetTileCount();
                mTileCountProvider.setMaxZoom(SharedHolder.maxZoom);
                mTileCountOverlay.clearTileCache();

                mTileProvider.setMaxZoom(SharedHolder.maxZoom);
                //mTileProvider.setMaxZoom((int) mMap.getMaxZoomLevel());
                mTileProvider.setDownloadDepth(SharedHolder.maxDownloadDepth);
                mTileProvider.resetTileCount();
                mTileOverlay.clearTileCache();

                startDownloadProgress();
                return true;
        }

        return false;

    }

    private void ChangeProvider(int providerID) {
        File cachePath;
        cachePath = new File(sdCard.getAbsolutePath() + "/" + mapCacheFolder + getString(providerID));
        if (!cachePath.isDirectory()) {
            cachePath.mkdir();
        }

        if (mTileProvider != null)
            mTileProvider.setCachePath(cachePath.getAbsolutePath());
        if (mTileOverlay != null)
            mTileOverlay.clearTileCache();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            setUpMapIfNeeded();
        }
    }

    @SuppressLint("MissingPermission")
    private void setUpMap() {
        //mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(this);

        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        //MyUrlTileProvider mTileProvider = new MyUrlTileProvider(512, 512, mUrl);
        sdCard = Environment.getExternalStorageDirectory();

        String path = sdCard.getAbsolutePath();
        path += "/" + mapCacheFolder;
        File cachePath = new File(path);
        if(!cachePath.isDirectory()) {
            cachePath.mkdir();
        }


        boolean online = isNetworkAvailable();
        if(!online)
            Toast.makeText(this, "No internet connection. Going offline.", Toast.LENGTH_LONG).show();


        mTileProvider = new MyUrlCachedTileProvider(512, 512, cachePath.getAbsolutePath());
        mTileProvider.setMaxZoom(SharedHolder.maxZoom);
        mTileProvider.SetBaseURL(getString(R.string.provider_baseUrl_google));
        mTileProvider.providerName = "google";
        ChangeProvider(R.string.provider_google_ID);
        mTileProvider.setOffline(!online);
        mTileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mTileProvider));
        mTileOverlay.setZIndex(0);

        mTileCountProvider = new TileCountProvider();
        mTileCountOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mTileCountProvider));
        mTileCountOverlay.setZIndex(-1);
        //mTileCountProvider.resetTileCount();
        //mTileCountProvider.setMaxZoom((int) mMap.getMaxZoomLevel());


        //CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(new LatLng(LAT, LON), ZOOM);
        //mMap.moveCamera(upd);


        if (rocketLocation != null)
            updateRocketLocation();
        /*
         * mMap.moveCamera( CameraUpdateFactory.newCameraPosition(new
         * CameraPosition());//.newLatLngZoom(new
         * LatLng(myLoc.getLatitude(),myLoc.getLongitude()), 10));
         */
        mUiSettings = mMap.getUiSettings();
        mUiSettings.setCompassEnabled(true);
        // mUiSettings.

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Sounds.release();
        // System.runFinalizersOnExit(true);
        System.exit(0);
        // mChecker.onDestroy();
    }

    public static final String MEASURE_UNIT_IMPERIAL = "Imperial";
    public static final String MEASURE_UNIT_METRIC = "Metric";

    void updateLabels(){
        String measureUnit = sharedPreferences.getString(
                SettingsActivity.PREF_MEASURE_UNIT, null);

        if(measureUnit == null)
            measureUnit = MEASURE_UNIT_METRIC;

        String suffix = "m";
        float unitRatio = 1;
        if(measureUnit.equals(MEASURE_UNIT_IMPERIAL)) {
            suffix = "ft";
            unitRatio = 3.28f;
        }

        TextView lblCurrentAltitude = (TextView) findViewById(R.id.lblCurrentAltitude);
        TextView lblMaxAltitude = (TextView) findViewById(R.id.lblMaxAltitude);
        TextView lblDistance = (TextView) findViewById(R.id.lblDistance);

        lblCurrentAltitude.setText(String.format("Current Altitude: %.2f%s",currentAltitude * unitRatio, suffix));
        lblMaxAltitude.setText(String.format("Current Altitude: %.2f%s",maxAltitude * unitRatio, suffix));
        lblDistance.setText(String.format("Current Altitude: %.2f%s",rocketDistance * unitRatio, suffix));
    }

    private void updateRocketLocation() {
        if (rocketLocation == null)
            return;

        if (rocketLocList.size() == 0) {
            addPosition();
        } else {
            Location lastLoc = rocketLocList.get(rocketLocList.size() - 1);
            if (rocketLocation.getLatitude() != lastLoc.getLatitude()
                    && rocketLocation.getLongitude() != lastLoc.getLongitude()
                    && rocketLocation.getLongitude() != lastLoc.getLongitude()) {
                addPosition();
            }
        }
        LatLng rocketPosition = rocketPosList.get(rocketPosList.size() - 1);

        // Draw marker at Rocket position
        if (rocketMarker == null) {
            rocketMarker = mMap.addMarker(new MarkerOptions()
                    .position(rocketPosition));
        } else
            rocketMarker.setPosition(rocketPosition);

        Location myLoc = mMap.getMyLocation();
        // myLoc = null when the android gps is not initialized yet.
        if (myLoc == null) {
            rocketDistance = 0;
            return;
        }

        // Max Altitude
        double altitude = rocketLocation.getAltitude() - altitudeOffset;
        if (altitude > maxAltitude)
            maxAltitude = altitude;
        currentAltitude = altitude;

        updateLabels();

        // Rocket path
        if (rocketPath == null) {
            rocketPath = mMap.addPolyline(new PolylineOptions()
                    .add(rocketPosList.get(0)).width(5.0f)
                    .color(Color.rgb(0, 0, 128)));

            rocketPath.setZIndex(999);
        }
        rocketPath.setPoints(rocketPosList);

        updateRocketLine(rocketPosition);
    }

    private void updateRocketLine(LatLng rocketPosition) {
        if (myPosition == null || rocketPosition == null)
            return;
        // Rocket Distance
        rocketDistance = mMap.getMyLocation().distanceTo(rocketLocation);

        updateLabels();

        // Draw line between myPosition and Rocket
        if (rocketLine == null) {
            rocketLine = mMap.addPolyline(new PolylineOptions()
                    .add(myPosition, rocketPosition).width(5.0f)
                    .color(Color.WHITE));
            rocketLine.setZIndex(999);
        } else {
            List<LatLng> positionList = new ArrayList<LatLng>();
            positionList.add(myPosition);
            positionList.add(rocketPosition);
            rocketLine.setPoints(positionList);
        }
    }

    private void addPosition() {
        LatLng rocketPosition = new LatLng(rocketLocation.getLatitude(),
                rocketLocation.getLongitude());

        rocketPosList.add(rocketPosition);
        rocketLocList.add(rocketLocation);

    }

    private class radarBeepThread extends Thread {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(radarDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (radarDelay > 0 && beepEnabled)
                    Sounds.radar_beep.start();
            }
        }
    }

    private class WaitForMyLocationThread extends Thread {
        public void run() {
            while (testLocation == null) {
                try {
                    Thread.sleep(200);
                    getLocationHandler.sendEmptyMessage(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Sounds.gps_connected.start();
            CameraPosition camPos = new CameraPosition.Builder()
                    .target(new LatLng(testLocation.getLatitude(), testLocation
                            .getLongitude())).zoom(20f).build();
            Message msg = new Message();
            msg.obj = camPos;

            getLocationHandler.sendMessage(msg);

        }
    }

    private String createXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
        xml.append("<Document>");
        xml.append("<name>Rocket Path</name>");
        xml.append("<description>Rocket Path</description>");
        xml.append("<Style id=\"yellowLineGreenPoly\">");
        xml.append("<LineStyle>");
        xml.append("<color>7f00ffff</color>");
        xml.append("<width>4</width>");
        xml.append("</LineStyle>");
        xml.append("<PolyStyle>");
        xml.append("<color>7f00ff00</color>");
        xml.append("</PolyStyle>");
        xml.append("</Style>");
        xml.append("<Placemark>");
        xml.append("<name>Absolute Extruded</name>");
        xml.append("<description>Transparent green wall with yellow outlines</description>");
        xml.append("<styleUrl>#yellowLineGreenPoly</styleUrl>");
        xml.append("<LineString>");
        xml.append("<extrude>1</extrude>");
        xml.append("<tessellate>0</tessellate>");
        xml.append("<altitudeMode>absolute</altitudeMode>");
        xml.append("<coordinates>");
        for (Location loc : rocketLocList) {
            xml.append(loc.getLongitude() + "," + loc.getLatitude() + ","
                    + loc.getAltitude() + "\r\n");
        }
        xml.append("</coordinates>");
        xml.append("</LineString>");
        xml.append("</Placemark>");
        xml.append("</Document>");
        xml.append("</kml>");
        return xml.toString();
    }

    private void savePath() {
        String xmlStr = createXml();
        try {
            // Date now = new Date();
            File myFile = new File(Environment.getExternalStorageDirectory()
                    + "/rocket_path.txt");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(xmlStr);
            myOutWriter.close();
            fOut.close();
            Toast.makeText(getBaseContext(), "Done writing file",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        if (mMap == null || mMap.getMyLocation() == null)
            return;

        geoField = new GeomagneticField(Double.valueOf(location.getLatitude())
                .floatValue(), Double.valueOf(location.getLongitude())
                .floatValue(), Double.valueOf(location.getAltitude())
                .floatValue(), System.currentTimeMillis());
        myPosition = new LatLng(location.getLatitude(), location.getLongitude());

        if (rocketPosList.size() > 0) {
            LatLng rocketPosition = rocketPosList.get(rocketPosList.size() - 1);
            updateRocketLine(rocketPosition);
        }

        ToggleButton chkFollowMe = (ToggleButton) findViewById(R.id.chkFollowMe);

        if (chkFollowMe.isChecked()) {
            CameraPosition camPos = new CameraPosition.Builder(
                    mMap.getCameraPosition()).target(myPosition).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
        }
    }

    // This function registers sensor listeners for the accelerometer,
    // magnetometer and gyroscope.
    public void initListeners() {
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

        /*
         * mSensorManager.registerListener(this,
         * mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
         * SensorManager.SENSOR_DELAY_FASTEST);
         */

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
    }

    // calculates orientation angles from accelerometer and magnetometer output
    public void calculateAccMagOrientation() {
        if (SensorManager
                .getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array and calculate
                // orientation
                System.arraycopy(event.values, 0, accel, 0, 3);
                accel = accelAverage.average(accel);
                calculateAccMagOrientation();
                rotateCamera();
                break;

            /*
             * case Sensor.TYPE_GYROSCOPE: // process gyro data gyroFunction(event);
             * break;
             */

            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                System.arraycopy(event.values, 0, magnet, 0, 3);
                magnet = magnetAverage.average(magnet);

                break;
        }
    }

    private void rotateCamera() {
        if (!compassEnabled || mMap == null)
            return;

        float azimuth = (float) (int) (accMagOrientation[0] * 180 / Math.PI);

        float heading = 0;

        if (geoField == null) {
            heading = azimuth;
        } else {
            heading = azimuth + geoField.getDeclination();
        }

        // Calculate Radar Delay
        if (rocketLocation != null) {
            float rocketBearing = normalizeDegree(mMap.getMyLocation().bearingTo(rocketLocation));
            float deltaBearing = (rocketBearing - azimuth);

            deltaBearing = deltaBearing - geoField.getDeclination();
            radarDelay = getRadarDelay(deltaBearing);

            //TextView lblBearing = (TextView) findViewById(R.id.TextView01);
            //lblBearing.setText("Bearing: " + radarDelay + " Azim: " + azimuth);
        }


        CameraPosition camPos = new CameraPosition.Builder(
                mMap.getCameraPosition()).bearing(heading).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
    }

    class ExponentialAverage {
        private float alpha;
        private float[] oldValue;
        private float[] newValue = new float[3];

        public ExponentialAverage(float alpha) {
            this.alpha = alpha;
        }

        public float[] average(float[] values) {
            if (oldValue == null) {
                oldValue = values;
                return values;
            }

            for (int i = 0; i < 3; i++) {
                newValue[i] = oldValue[i] + alpha * (values[i] - oldValue[i]);
                oldValue[i] = newValue[i];
            }
            return newValue;
        }
    }

    @Override
    public void update(Observable o, Object data) {
        NmeaValues values = (NmeaValues) data;
        Location location = values.getLocation();
        if(location != null){
            rocketLocation = location;
            updateHandler.sendEmptyMessage(0);
            settings.setLastRocketLocation(rocketLocation);
            settings.saveSettings();
        }
    }

    @Override
    public void onMapMove() {
        ToggleButton chkFollowMe = (ToggleButton) findViewById(R.id.chkFollowMe);
        ToggleButton chkRocketCompass = (ToggleButton) findViewById(R.id.chkRocketCompass);
        chkFollowMe.setChecked(false);
        chkRocketCompass.setChecked(false);
    }

    /**
     * Returns the destination point from 'this' point having travelled the given distance on the
     * given initial bearing (bearing normally varies around path followed).
     *
     * @param   {LatLon} start - Initial position.
     * @param   {number} bearing - Initial bearing in degrees.
     * @param   {number} distance - Distance in km (on sphere of 'this' radius).
     * @returns {LatLon} Destination point.
     *
     * @example
     *     var p1 = new LatLon(51.4778, -0.0015);
     *     var p2 = p1.destinationPoint(300.7, 7.794); // p2.toString(): 51.5135�N, 000.0983�W
     */
    Location destinationPoint(Location start, float bearing, float distance)
    {
        double dist = distance / 6378.1; //Radius of earth
        double brng = Math.toRadians(bearing);

        double lat1 = Math.toRadians(start.getLatitude());
        double lon1 = Math.toRadians(start.getLongitude());

        double lat2 = Math.asin( Math.sin(lat1)*Math.cos(dist) + Math.cos(lat1)*Math.sin(dist)*Math.cos(brng) );
        double lon2 = lon1 + Math.atan2(Math.sin(brng)*Math.sin(dist)*Math.cos(lat1), Math.cos(dist)-Math.sin(lat1)*Math.sin(lat2));

        lon2 = (lon2+ 3*Math.PI) % (2*Math.PI) - Math.PI;

        Location newLocation = new Location("");
        newLocation.setLatitude(Math.toDegrees(lat2));
        newLocation.setLongitude(Math.toDegrees(lon2));
        return newLocation;
    }
}