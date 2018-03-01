package com.example.michele.guidasicuro;

import android.util.Log;

import static java.lang.Math.*;

/**
 * Created by Michele on 08/02/2018.
 */

public class Bearing {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static double getBearing(double previousLatitude, double previousLongitude, double currentLatitude, double currentLongitude) {
        double m1, m2 = 0.0;
        double bearing = 0.0;

        if(Double.compare(currentLongitude, previousLongitude) > 0) {
            m1 = getSlope(previousLongitude, previousLatitude, currentLongitude, currentLatitude);
            if(Double.compare(currentLatitude, previousLatitude) > 0){
                bearing = 90 - getAngle(m1, m2);
            }
            else if(Double.compare(currentLatitude, previousLatitude) < 0){
                bearing = 90 + getAngle(m1, m2);
            }
        }
        else if(Double.compare(currentLongitude, previousLongitude) < 0) {
            m1 = getSlope(previousLongitude, previousLatitude, currentLongitude, currentLatitude);
            if(Double.compare(currentLatitude, previousLatitude) > 0) {
                bearing = 270 + getAngle(m1, m2);
            }
            else if(Double.compare(currentLatitude, previousLatitude) < 0){
                bearing =  180 + (90 - getAngle(m1, m2));
            }
        }

        return bearing;
    }

    private static double getSlope(double previousLatitude, double previousLongitude, double currentLatitude, double currentLongitude) {
        double m = (currentLatitude-previousLatitude) / (currentLongitude-previousLongitude);
        Log.i(TAG, "clat: " + currentLatitude);
        Log.i(TAG, "plat: " + previousLatitude);
        Log.i(TAG, "clon: " + currentLongitude);
        Log.i(TAG, "clon: " + previousLongitude);
        Log.i(TAG, "m: " + m);

        return m;
    }

    private static double getAngle(double m1, double m2) {
        double tan = abs((m1-m2)/(1+m1*m2));
        Log.i(TAG, "tan: " + tan);

        double rad = atan(tan);
        double deg = fromRadToDeg(rad);
        Log.i(TAG, "deg: " + deg);

        return deg;
    }

    private static double fromRadToDeg(double r) {
        double deg = r * 180 / Math.PI;
        return deg;
    }
}
