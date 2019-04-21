package com.frankdev.rocketlocator;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by Francois on 2016-06-09.
 */
public abstract class CachedTileProvider implements TileProvider {
    private int width;
    private int height;
    private int downloadDepth;
    private boolean isOffline;
    private int tileCount;
    private int pendingTileCount;

    TileCache cache;
    private int maxZoom;
    private boolean cancelled;

    public CachedTileProvider(String cachePath, int width, int height, int downloadDepth) {
        this.width = width;
        this.height = height;
        this.downloadDepth = downloadDepth;
        cache = new TileCache(width, height, cachePath);
        pendingTileCount = 0;
    }

    public abstract URL getTileUrl(int x, int y, int zoom);

    private Tile downloadOrLoadTile(int x, int y, int zoom, int depth, boolean load) {
        if(cancelled) return NO_TILE;
        if(zoom > maxZoom) return NO_TILE;

        int maxTile = (int) (Math.pow(2,zoom)) - 1;
        if(x > maxTile || y > maxTile) return NO_TILE;

        Tile tile = NO_TILE;

        tileCount++;

        if(cache.tileExists(x,y,zoom)) {
            if(load) // Don't need to load from disk when download recursive
                tile = cache.getTile(x, y, zoom);
            else
                tile = NO_TILE;
        } else {

            URL tileProviderURL = this.getTileUrl(x, y, zoom);
            if (tileProviderURL == null || isOffline) {
                tile = NO_TILE;
            } else {
                if (!cache.tileExists(x, y, zoom)) {
                    try {
                        byte[] data = getBytesFromStream(tileProviderURL.openStream());
                        tile = new Tile(this.width, this.height, data);
                        cache.putTile(x, y, zoom, data);
                    } catch (IOException e) {
                        e.printStackTrace();
                        tile = NO_TILE;
                    }
                }
            }
        }

        // Download all 4 tiles under this tile
        if (depth > 1 && !cancelled) {
            downloadOrLoadTile(x * 2, y * 2, zoom + 1, depth - 1, false);
            downloadOrLoadTile(x * 2 + 1, y * 2, zoom + 1, depth - 1, false);
            downloadOrLoadTile(x * 2, y * 2 + 1, zoom + 1, depth - 1, false);
            downloadOrLoadTile(x * 2 + 1, y * 2 + 1, zoom + 1, depth - 1, false);
        }
        return tile;
    }


    @Override
    public final Tile getTile(int x, int y, int zoom) {
        pendingTileCount++;
        Tile tile = downloadOrLoadTile(x,y,zoom, downloadDepth, true);
        pendingTileCount--;

        if(pendingTileCount <= 0)
            cancelled = false;
        return tile;
    }

    private static byte[] getBytesFromStream(InputStream onlineStream) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        downloadData(onlineStream, bytes);
        return bytes.toByteArray();
    }

    private static long downloadData(InputStream onlineStream, OutputStream bytes) throws IOException {
        byte[] var2 = new byte[4096];
        long numBytes = 0L;

        while(true) {
            int byteRead = onlineStream.read(var2);
            if(byteRead == -1) {
                return numBytes;
            }

            bytes.write(var2, 0, byteRead);
            numBytes += (long)byteRead;
        }
    }

    public String getCachePath() {
        return cache.getCachePath();
    }

    public void setCachePath(String cachePath) {
        cache.setCachePath(cachePath);
    }

    public boolean isOffline() {
        return isOffline;
    }

    public void setOffline(boolean offline) {
        isOffline = offline;
    }

    public int getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(int maxZoom) {
        this.maxZoom = maxZoom;
    }

    public int getDownloadDepth() {
        return downloadDepth;
    }

    public void setDownloadDepth(int downloadDepth) {
        this.downloadDepth = downloadDepth;
    }

    public int getTileCount() {
        return tileCount;
    }

    public void resetTileCount() {
        this.tileCount = 0;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
