package com.example.michele.guidasicuro;

import android.location.Location;

/**
 * Created by Michele on 01/03/2018.
 */

public class BoundingBox {
    private double southernLimit;
    private double westernLimit;
    private double northernLimit;
    private double easternLimit;

    private static final double earthRadius = 6378;

    public void calculate(Location location, double distance) {
        double lat = degToRad(location.getLatitude());
        double lon = degToRad(location.getLongitude());

        double parallelRadius = this.earthRadius*Math.cos(lat);

        this.southernLimit = radToDeg(lat - distance/this.earthRadius);
        this.northernLimit = radToDeg(lat + distance/this.earthRadius);
        this.westernLimit = radToDeg(lon - distance/parallelRadius);
        this.easternLimit = radToDeg(lon + distance/parallelRadius);
    }

    private double radToDeg(double rad) {
        return 180.0*rad/Math.PI;
    }

    private double degToRad(double deg) {
        return Math.PI*deg/180.0;
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
