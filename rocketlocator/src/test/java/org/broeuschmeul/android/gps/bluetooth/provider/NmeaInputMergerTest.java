package org.broeuschmeul.android.gps.bluetooth.provider;

import android.support.v4.util.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NmeaInputMergerTest {

    private static final String NMEA = "$GPGGA,114919.000,4712.4294,N,01815.2761,E,1,04,3.7,178.3,M,0.0,M,,*60";

    @Mock
    private Consumer<String> mockConsumer;

    private BleBluetoothGpsManager.NmeaInputMerger merger = new BleBluetoothGpsManager.NmeaInputMerger();

    @Test
    public void testHandleMessagePartSingleStart() {
        merger.handleMessagePart(NMEA, mockConsumer);
        // only the next message triggers an action
        merger.handleMessagePart("$GPnext", mockConsumer);
        verify(mockConsumer).accept(eq(NMEA));
    }

    @Test
    public void testHandleMessagePartSingleMiddle() {
        merger.handleMessagePart("something", mockConsumer);
        // add extra line break too - it should be skipped
        merger.handleMessagePart(NMEA + "\n", mockConsumer);
        // only the next message triggers an action
        merger.handleMessagePart("$GPnext", mockConsumer);
        verify(mockConsumer).accept(eq("something"));
        verify(mockConsumer).accept(eq(NMEA));
    }

    @Test
    public void testHandleMessagePartMultiStart() {
        String[] nmeaParts = { "$GPGGA,114919.000,47", "12.4294,N,01815.2761", ",E,1,04,3.7,178.3,M,", "0.0,M,,*60" };
        for (String nmeaPart : nmeaParts) {
            merger.handleMessagePart(nmeaPart, mockConsumer);
        }
        // only the next message triggers an action
        merger.handleMessagePart("$GPnext", mockConsumer);
        verify(mockConsumer).accept(eq(NMEA));
    }

    @Test
    public void testHandleMessagePartMultiMiddle() {
        String[] nmeaParts = { "$GPGGA,114919.000,47", "12.4294,N,01815.2761", ",E,1,04,3.7,178.3,M,", "0.0,M,,*60" };
        merger.handleMessagePart("something", mockConsumer);
        for (String nmeaPart : nmeaParts) {
            merger.handleMessagePart(nmeaPart, mockConsumer);
        }
        // only the next message triggers an action
        merger.handleMessagePart("$GPnext", mockConsumer);
        verify(mockConsumer).accept(eq("something"));
        verify(mockConsumer).accept(eq(NMEA));
    }

}