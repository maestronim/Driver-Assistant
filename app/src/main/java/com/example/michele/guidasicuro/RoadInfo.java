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
    private String type;
    private RoadInfoListener mRoadInfoListener;

    public interface RoadInfoListener {
        public void onRoadChanged(RoadInfo roadInfo);
    }

    public RoadInfo() {
        this.ID = null;
        this.Name = null;
        this.MaxSpeed = 0;
        this.Highway = null;
        this.type = null;
    }

    public void setRoadInfoListener(RoadInfoListener roadInfoListener) {
        this.mRoadInfoListener = roadInfoListener;
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

    public void setType(String type) {
        this.type = type;
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

    public String getType(){
        return this.type;
    }

    public RoadInfoListener getRoadInfoListener() {
        return this.mRoadInfoListener;
    }
}
