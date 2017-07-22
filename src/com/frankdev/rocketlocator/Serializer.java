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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Serializer {
	public static String GetStringFromByteArray(byte[] array){
		if(array == null)
			return null;
		StringBuilder outStr = new StringBuilder();
		for(Byte b : array){
			outStr.append(b.toString() + ",");
		}
		outStr.deleteCharAt(outStr.length()-1);
		return outStr.toString();
	}
	
	public static byte[] GetByteArrayFromString(String str){
		List<Byte> byteArray = new ArrayList<Byte>();
		
		//fill byte arrayList
		String[] strList = str.split(",");
		for(String numStr: strList){
			byteArray.add(Byte.parseByte(numStr));
		}
		
		//Convert arrayList to array of primitive
		byte[] ret = new byte[byteArray.size()];
	    Iterator<Byte> iterator = byteArray.iterator();
	    for (int i = 0; i < ret.length; i++)
	    {
	        ret[i] = iterator.next().byteValue();
	    }
	    return ret;		
	 }
	
	public static byte[] serializeObject(Object o) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(o);
			out.close();

			// Get the bytes of the serialized object
			byte[] buf = bos.toByteArray();

			return buf;
		} catch (IOException ioe) {
			SharedHolder.getInstance().getLogs().e("serializeObject", "error", ioe);

			return null;
		}
	}

	public static Object deserializeObject(byte[] b) {
		try {
			ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(b));
			Object object = in.readObject();
			in.close();

			return object;
		} catch (ClassNotFoundException cnfe) {
			SharedHolder.getInstance().getLogs().e("deserializeObject", "class not found error", cnfe);
			
			return null;
		} catch (IOException ioe) {
			SharedHolder.getInstance().getLogs().e("deserializeObject", "io error", ioe);

			return null;
		}
	}
}
