package com.frankdev.rocketlocator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

import com.google.android.gms.common.data.DataBuffer;
import com.google.android.gms.maps.model.Tile;

import java.io.*;

/**
 * Created by Francois on 2016-06-09.
 */
public class TileCache {
    private String cachePath;
    int width, height;

    public TileCache(int width, int height, String cachePath) {
        this.cachePath = cachePath;
        this.width = width;
        this.height = height;
    }

    public boolean tileExists(int x,int y, int zoom){
        String filepath = cachePath + "/" + zoom + "_"+ x +"_" + y + ".png";
        File f = new File(filepath);
        return f.exists();
    }

    public Tile getTile(int x,int y, int zoom){
        String filepath = cachePath + "/" + zoom + "_"+ x +"_" + y + ".png";

        File f = new File(filepath);
        if(!f.exists()) {
            return null;
        }

        File file = new File(filepath);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return new Tile(width, height, bytes);
    }

    public void putTile(int x,int y, int zoom, byte[] data) {
        String filepath = cachePath + "/" + zoom + "_"+ x +"_" + y + ".png";

        File f = new File(filepath);
        if(f.exists()) {
            return;
        }

        try {
            BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream(filepath));
            buf.write(data);
            buf.flush();
            buf.close();
        }
        catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }
}
