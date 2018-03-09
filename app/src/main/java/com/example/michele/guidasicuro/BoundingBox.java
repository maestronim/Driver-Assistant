package com.example.michele.guidasicuro;

import android.location.Location;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michele on 01/03/2018.
 */

public class BoundingBox {
    private double southernLimit;
    private double westernLimit;
    private double northernLimit;
    private double easternLimit;

    private static final double earthRadius = 6371000;
    private static final String TAG = BoundingBox.class.getSimpleName();

    public void calculate(Location location, double distance) {

        double lat = degToRad(location.getLatitude());
        double lon = degToRad(location.getLongitude());

        Log.i(TAG, "Lat: " + location.getLatitude());
        Log.i(TAG, "Lon: " + location.getLongitude());

        double parallelRadius = this.earthRadius*Math.cos(lat);

        this.southernLimit = radToDeg(lat - distance/this.earthRadius);
        this.northernLimit = radToDeg(lat + distance/this.earthRadius);
        this.westernLimit = radToDeg(lon - distance/parallelRadius);
        this.easternLimit = radToDeg(lon + distance/parallelRadius);
        Log.i(TAG, "Southernlimit: " + this.southernLimit);
        Log.i(TAG, "Northernlimit: " + this.northernLimit);
        Log.i(TAG, "Westernlimit: " + this.westernLimit);
        Log.i(TAG, "Easternlimit: " + this.easternLimit);
    }

    private double radToDeg(double rad) {
        return 180.0*rad/Math.PI;
    }

    private double degToRad(double deg) {
        return Math.PI*deg/180.0;
    }

    private Map<String, Double> decToDms(double dec) {
        Map<String, Double> dms = new HashMap<String, Double>();
        double intdeg = Math.floor(dec);
        double minutes = (dec - intdeg)*60.0;
        double intMin = Math.floor(minutes);
        double seconds = (minutes - intMin)*60.0;
        double intSec = Math.round(seconds);
        // get rid of fractional part
        dms.put("degrees", Math.floor(intdeg));
        dms.put("minutes", Math.floor(intMin));
        dms.put("seconds", Math.floor(intSec));

        return dms;
    }

    private double dmsToDeg(double degrees, double minutes, double seconds) {
        return degrees + minutes/60.0 + seconds/3600.0;
    }

    public double getSouthernLimit() {
        return this.southernLimit;
    }

    public double getWesternLimit() {
        return this.westernLimit;
    }

    public double getNorthernLimit() {
        return this.northernLimit;
    }

    public double getEasternLimit() {
        return this.easternLimit;
    }
}
