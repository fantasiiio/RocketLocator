package com.frankdev.rocketlocator;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

/**
 * Created by Francois on 2016-06-23.
 */
public class TileCountProvider implements TileProvider {
    private int tileCount;
    private int maxZoomLevel;

    @Override
    public Tile getTile(int x, int y, int zoom) {
        int zoomCount = maxZoomLevel - zoom + 1;
        if(zoomCount > SharedHolder.maxDownloadDepth)
            zoomCount = SharedHolder.maxDownloadDepth;

        for(int i = 0; i < zoomCount; i++){
            tileCount += Math.pow(4,i);
        }
        return NO_TILE;
    }

    public int getTileCount() {
        return tileCount;
    }

    public void resetTileCount() {
        tileCount = 0;
    }

    public void setMaxZoom(int maxZoomLevel) {
        this.maxZoomLevel = maxZoomLevel;
    }
}
