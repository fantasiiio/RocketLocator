/*
 * Copyright (C) 2020 Balazs Mihaly | mihu86
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

package org.broeuschmeul.android.gps.bluetooth.provider;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.Consumer;
import android.widget.Toast;

import com.frankdev.rocketlocator.SharedHolder;

import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleBluetoothGpsManager extends BluetoothGpsManager {
    // currently "FFE0", for receiving standard NMEA sentences
    // maybe the standard "1819" would require different implementation
    private static final UUID UUID_RW_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_RW_CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    /**
     * Tag used for log messages
     */
    private static final String LOG_TAG = "BlueGPS";

    private final Context context;
    private final String deviceAddress;
    private final int maxRetries;
    private AtomicInteger retries = new AtomicInteger(0);
    private volatile boolean enabled;
    private AtomicBoolean starting = new AtomicBoolean();
    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private Handler uiHandler;
    private NmeaInputMerger merger = new NmeaInputMerger();

    public BleBluetoothGpsManager(Context context, String deviceAddress, int maxRetries) {
        if (deviceAddress == null || deviceAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Device address cannot be empty.");
        }

        this.context = context;
        this.deviceAddress = deviceAddress;
        this.maxRetries = maxRetries;
        uiHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int getDisableReason() {
        // not used
        return 0;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        if (!enabled && starting.compareAndSet(false, true)) {
            enabled = true;
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            device = adapter.getRemoteDevice(deviceAddress);
            gatt = device.connectGatt(context, false, new GpsCallback());
        } else {
            SharedHolder.getInstance().getLogs().w(LOG_TAG, "Manager already enabled or starting.");
        }
    }

    @Override
    public void disable(int reasonId) {
        // reason id not used - just disable
        disable(false);
    }

    @Override
    public void disable(boolean restart) {
        enabled = false;
        starting.set(false);

        if (gatt != null) {

            BluetoothGattService service = gatt.getService(UUID_RW_SERVICE);
            if (service != null) {
                gatt.setCharacteristicNotification(service.getCharacteristic(UUID_RW_CHARACTERISTIC), false);
            }
            gatt.disconnect();
            gatt.close();
        }

        if (restart) {
            enable();
        }
    }

    class GpsCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    starting.set(false);
                    SharedHolder.getInstance().getLogs().d(LOG_TAG,"Connected to BLE device: " +
                            gatt.getDevice().getName() + " (" + gatt.getDevice().getAddress() + ")");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    if (starting.get() && retries.getAndIncrement() < maxRetries) {
                        SharedHolder.getInstance().getLogs().d(LOG_TAG, "Could not connect to BLE, retry...");
                        device.connectGatt(context, false, new GpsCallback());
                    } else {
                        SharedHolder.getInstance().getLogs().d(LOG_TAG, "BLE disconnected.");
                    }
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // connection fully set up, start to get notifications
            BluetoothGattService service = gatt.getService(UUID_RW_SERVICE);
            if (service != null) {
                gatt.setCharacteristicNotification(service.getCharacteristic(UUID_RW_CHARACTERISTIC), true);
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,
                                "BLE GPS connected",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                SharedHolder.getInstance().getLogs().e(LOG_TAG, "BLE service \"FFE0\" not supported!");
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,
                                "Device does not support BLE service \"FFE0\"",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            try {
                String messagePart = new String(characteristic.getValue(), "US-ASCII"); // StandardCharsets.US_ASCII
                SharedHolder.getInstance().getLogs().v(LOG_TAG, "Received: " + messagePart);
                if (!messagePart.isEmpty()) {
                    merger.handleMessagePart(messagePart, new Consumer<String>() {
                        @Override
                        public void accept(String nmeaSentence) {
                            notifyNmeaSentence(nmeaSentence);
                        }
                    });
                }
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Standard ASCII not known...");
            }
        }

    }

    public static class NmeaInputMerger {
        private static final String $ = "$";
        private StringBuilder inputBuffer = new StringBuilder();

        public void handleMessagePart(String messagePart, Consumer<String> action) {
            String[] parts = messagePart.replaceAll("\\s", "").split(Pattern.quote($));

            int start = 0;
            if (parts[0].isEmpty()) {
                if (inputBuffer.length() > 0) {
                    callAction(action);
                }

                start++;
                inputBuffer.append($);
            }

            inputBuffer.append(parts[start++]);
            if (parts.length > start) {
                for (int i = start; i < parts.length; i++) {
                    callAction(action);
                    inputBuffer.append($).append(parts[i]);
                }
            }
        }

        private void callAction(Consumer<String> action) {
            String nmeaSentence = inputBuffer.toString();
            SharedHolder.getInstance().getLogs().v(LOG_TAG, "NMEA: " + nmeaSentence);
            action.accept(nmeaSentence);
            inputBuffer.setLength(0);
        }

    }
}
