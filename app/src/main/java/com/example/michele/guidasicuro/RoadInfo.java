package com.example.michele.guidasicuro;

import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import android.support.v4.content.ContextCompat;

/**
 * Created by Michele on 28/01/2018.
 */

public class RoadInfo {
    private String ID;
    private String Name;
    private int MaxSpeed;
    private String Highway;
    private boolean isBridge;
    private boolean isTunnel;

    public RoadInfo() {
        this.isBridge = false;
        this.isTunnel = false;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.MaxSpeed = maxSpeed;
    }

    public void setHighway(String highway) {
        this.Highway = highway;
    }

    public void setBridge() {
        this.isBridge = true;
    }

    public void setTunnel() {
        this.isTunnel = true;
    }

    public String getName() {
        return this.Name;
    }

    public int getMaxSpeed() {
        return this.MaxSpeed;
    }

    public String getHighway() {
        return this.Highway;
    }

    public String getID() {
        return this.ID;
    }

    public boolean isBridge() {
        return this.isBridge;
    }

    public boolean isTunnel() {
        return this.isTunnel;
    }
}
