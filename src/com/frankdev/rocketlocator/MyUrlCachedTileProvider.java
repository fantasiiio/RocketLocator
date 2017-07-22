package com.frankdev.rocketlocator;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Francois on 2016-06-10.
 */
public class MyUrlCachedTileProvider extends CachedTileProvider {
    //private String baseUrl = "http://a.tile.openstreetmap.org/{z}/{x}/{y}.png";
    //private String baseUrl = "http://mt0.google.com/vt/x={x}&y={y}&z={z}";
    //private String baseUrl = "https://khms1.googleapis.com/kh?v=690&x={x}&y={y}&z={z}";
    private String baseUrl = "";

    public MyUrlCachedTileProvider(int width, int height, String cachePath) {
        super(cachePath, width, height, 1);
    }

    public MyUrlCachedTileProvider(int width, int height, String cachePath, int downloadDepth) {
        super(cachePath, width, height, downloadDepth);
    }

    public void SetBaseURL(String baseUrl){
        this.baseUrl = baseUrl;
    }

    @Override
    public URL getTileUrl(int x, int y, int zoom) {
        try {
            return new URL(baseUrl.replace("{z}", ""+zoom).replace("{x}",""+x).replace("{y}",""+y));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
