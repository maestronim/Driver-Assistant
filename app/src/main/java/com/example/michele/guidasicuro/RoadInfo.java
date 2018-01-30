package com.example.michele.guidasicuro;

/**
 * Created by Michele on 28/01/2018.
 */

public class RoadInfo {
    private String Name;
    private int MaxSpeed;
    private String Highway;

    public void setName(String name) {
        this.Name = name;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.MaxSpeed = maxSpeed;
    }

    public void setHighway(String highway) {
        this.Highway = highway;
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
}
