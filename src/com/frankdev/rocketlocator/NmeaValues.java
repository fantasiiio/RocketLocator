package com.frankdev.rocketlocator;

import android.location.Location;

public class NmeaValues {

	private Location location;
	private String nmeaSentence;
	private String command;

	public NmeaValues(String command, String nmeaSentence, Location location) {
		this.command = command;
		this.nmeaSentence = nmeaSentence;
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public String getNmeaSentence() {
		return nmeaSentence;
	}

	public void setNmeaSentence(String nmeaSentence) {
		this.nmeaSentence = nmeaSentence;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}
}
