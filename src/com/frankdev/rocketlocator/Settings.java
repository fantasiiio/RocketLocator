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

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;

/**
 * This class is responsible for storing and loading the last used settings for
 * the application.
 * 
 * @author twig
 */
public class Settings {
	private Location lastRocketLocation;
	private Activity activity;

	public Settings(Activity activity) {
		this.activity = activity;
	}

	public void loadSettings() {

		SharedPreferences settings = activity
				.getPreferences(Activity.MODE_PRIVATE);

		String locationStr = settings.getString("last_rocket_location", null);
		if(locationStr != null){
			byte[] bytes = Serializer.GetByteArrayFromString(locationStr);
			SerializableLocation loc = (SerializableLocation) Serializer.deserializeObject(bytes);
			if (loc != null) {
				this.setLastRocketLocation(loc.getLocation());
			}
		}

	}

	public void saveSettings() {
		SharedPreferences settings = activity
				.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		SerializableLocation loc = new SerializableLocation(getLastRocketLocation());
		
		byte[] bytes = Serializer.serializeObject(loc);
		if(bytes != null){
			String bytesStr = Serializer.GetStringFromByteArray(bytes);
			editor.putString("last_rocket_location", bytesStr);
		}
		editor.apply();
	}

	public Location getLastRocketLocation() {
		return lastRocketLocation;
	}

	public void setLastRocketLocation(Location lastRocketLocation) {
		this.lastRocketLocation = lastRocketLocation;
	}
	

}