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

import java.io.IOException;
import android.location.Location;

public class SerializableLocation implements java.io.Serializable  {
	private Location location;
	public SerializableLocation(Location loc) {
		this.location = loc;
		// TODO Auto-generated constructor stub
	}

	
	private void writeObject(java.io.ObjectOutputStream stream)
            throws IOException {
		if(location == null)
			return;
        stream.writeFloat(this.location.getAccuracy());
        stream.writeDouble(this.location.getAltitude());
        stream.writeFloat(this.location.getBearing());
        stream.writeFloat(this.location.getSpeed());
        stream.writeDouble(this.location.getLatitude());
        stream.writeDouble(this.location.getLongitude());
    }
	
    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
		if(location == null)
			location = new Location("");
    	this.location.setAccuracy(stream.readFloat());
    	this.location.setAltitude(stream.readDouble());
    	this.location.setBearing(stream.readFloat());
    	this.location.setSpeed(stream.readFloat());
    	this.location.setLatitude(stream.readDouble());
    	this.location.setLongitude(stream.readDouble());
    }
	public Location getLocation() {
		return location;
	}


	public void setLocation(Location location) {
		this.location = location;
	}
	private static final long serialVersionUID = 5198940413858447192L;
}
