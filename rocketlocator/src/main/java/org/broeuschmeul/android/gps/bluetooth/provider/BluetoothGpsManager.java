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

import com.frankdev.rocketlocator.NmeaValues;
import com.frankdev.rocketlocator.SharedHolder;
import com.frankdev.rocketlocator.Sounds;

import org.broeuschmeul.android.gps.nmea.util.NmeaParser;

import java.util.Observable;

public abstract class BluetoothGpsManager extends Observable {
    /**
     * Tag used for log messages
     */
    private static final String LOG_TAG = "BlueGPS";
    private NmeaParser parser = new NmeaParser(10f);

    public abstract int getDisableReason();

    public abstract boolean isEnabled();

    public abstract void enable();

    public abstract void disable(int reasonId);

    public abstract void disable(boolean restart);

    /**
     * Notifies the reception of a NMEA sentence from the bluetooth GPS to registered NMEA listeners.
     *
     * @param nmeaSentence	the complete NMEA sentence received from the bluetooth GPS (i.e. $....*XY where XY is the checksum)
     */
    protected void notifyNmeaSentence(final String nmeaSentence) {
        NmeaValues nmeaValues;
        try {
            nmeaValues = parser.parseNmeaSentence(nmeaSentence + "\r\n");
        } catch (Exception e){
            SharedHolder.getInstance().getLogs().e(LOG_TAG, "error while parsing NMEA sentence: " + nmeaSentence, e);
            nmeaValues = null;
            Sounds.gps_disconnected.start();
        }

        if (nmeaValues != null && nmeaValues.getCommand() != null &&  nmeaValues.getCommand().equals("GPGGA")) {
            SharedHolder.getInstance().getLogs().v(LOG_TAG, nmeaSentence);
            setChanged();
            notifyObservers(nmeaValues);
        }
    }

}
