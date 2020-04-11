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
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleBluetoothGpsManager extends BluetoothGpsManager {
    @Override
    public int getDisableReason() {
        return 0;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean enable() {
        return false;
    }

    @Override
    public void startConnectThread() {

    }

    @Override
    public void disable(int reasonId) {

    }

    @Override
    public void disable(boolean restart) {

    }
}
