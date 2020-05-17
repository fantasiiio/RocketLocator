package com.frankdev.rocketlocator;

import com.google.android.gms.common.util.IOUtils;
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
        String filepath = getFilepath(x, y, zoom);
        File f = new File(filepath);
        return f.exists();
    }

    public Tile getTile(int x,int y, int zoom){
        String filepath = getFilepath(x, y, zoom);

        File f = new File(filepath);
        if(!f.exists()) {
            return null;
        }

        File file = new File(filepath);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        BufferedInputStream buf = null;
        try {
            buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            IOUtils.closeQuietly(buf);
        }

        return new Tile(width, height, bytes);
    }

    public void putTile(int x,int y, int zoom, byte[] data) {
        String filepath = getFilepath(x, y, zoom);

        File f = new File(filepath);
        if(f.exists()) {
            return;
        }

        BufferedOutputStream buf = null;
        try {
            buf = new BufferedOutputStream(new FileOutputStream(filepath));
            buf.write(data);
            buf.flush();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(buf);
        }
    }

    private String getFilepath(int x, int y, int zoom) {
        return cachePath + "/" + zoom + "_" + x + "_" + y + ".dat";
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }
}
