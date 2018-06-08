package com.example.michele.guidasicuro;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import android.support.v4.content.ContextCompat;

/**
 * Created by Michele on 28/01/2018.
 */

public class RoadInfo implements Parcelable {
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
        this.Name = "Unknown";
        this.MaxSpeed = 0;
        this.Highway = "Unknown";
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

    //write object values to parcel for storage
    public void writeToParcel(Parcel dest, int flags){
        dest.writeString(Name);
        dest.writeInt(MaxSpeed);
        dest.writeString(Highway);
    }

    //constructor used for parcel
    public RoadInfo(Parcel parcel){
        this.Name = parcel.readString();
        this.MaxSpeed = parcel.readInt();
        this.Highway = parcel.readString();
    }

    //creator - used when un-parceling our parcle (creating the object)
    public static final Parcelable.Creator<RoadInfo> CREATOR = new Parcelable.Creator<RoadInfo>(){

        @Override
        public RoadInfo createFromParcel(Parcel parcel) {
            return new RoadInfo(parcel);
        }

        @Override
        public RoadInfo[] newArray(int size) {
            return new RoadInfo[size];
        }
    };

    //return hashcode of object
    public int describeContents() {
        return hashCode();
    }
}
