package com.example.michele.guidasicuro;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Property;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Created by Michele on 04/04/2018.
 */

public class UserScore implements Parcelable{
    public interface UserScoreListener {
        public void onSpeedLimitExceeded(int speedLimitExceededCount);
        public void onHardBraking(int hardBrakingCount);
        public void onDangerousTime(int isDangerousTime);
    }

    private int mHardBrakingCount;
    private int mSpeedLimitExceededCount;
    private int mIsDangerousTime;
    private UserScoreListener mUserScoreListener;

    public UserScore() {
        mUserScoreListener = null;
        mHardBrakingCount = 0;
        mSpeedLimitExceededCount = 0;
        mIsDangerousTime = 0;
    }

    public UserScore(int hardBrakingCount, int speedLimitExceededCount, int isDangerousTime) {
        this.mHardBrakingCount = hardBrakingCount;
        this.mSpeedLimitExceededCount = speedLimitExceededCount;
        this.mIsDangerousTime = isDangerousTime;
    }

    public void setHardBrakingCount(int hardBrakingCount) {
        this.mHardBrakingCount = hardBrakingCount;
    }

    public void setSpeedLimitExceededCount(int speedLimitExceededCount) {
        this.mSpeedLimitExceededCount = speedLimitExceededCount;
    }

    public void setDangerousTimeCount(int isDangerousTime) {
        this.mIsDangerousTime = isDangerousTime;
    }

    public void setUserScoreListener(UserScoreListener listener) {
        mUserScoreListener = listener;
    }

    public int getHardBrakingCount() {
        return this.mHardBrakingCount;
    }

    public int getSpeedLimitExceededCount() {
        return this.mSpeedLimitExceededCount;
    }

    public int getDangerousTimeCount() {
        return this.mIsDangerousTime;
    }

    public void checkHardBraking(int[] speedMeasures) {
        int initialSpeed = speedMeasures[0];

        if(initialSpeed >= 32) {
            if(((speedMeasures[0] - speedMeasures[1]) + (speedMeasures[1] - speedMeasures[2])) > 14) {
                mHardBrakingCount += 1;
                mUserScoreListener.onHardBraking(mHardBrakingCount);
            }
        }
    }

    public boolean checkDangerousTime() {
        Calendar rightNow = Calendar.getInstance();
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);

        if(currentHour >= 22 || currentHour <= 6) {
            mIsDangerousTime = 1;
            mUserScoreListener.onDangerousTime(mIsDangerousTime);
            return true;
        }

        return false;
    }

    public void checkSpeedLimitExceeded(int speed, int speedLimit) {
        if(speed > speedLimit) {
            mSpeedLimitExceededCount += 1;
            mUserScoreListener.onSpeedLimitExceeded(mSpeedLimitExceededCount);
        }
    }

    //write object values to parcel for storage
    public void writeToParcel(Parcel dest, int flags){
        dest.writeInt(mHardBrakingCount);
        dest.writeInt(mSpeedLimitExceededCount);
        dest.writeInt(mIsDangerousTime);
    }

    //constructor used for parcel
    public UserScore(Parcel parcel){
        this.mHardBrakingCount = parcel.readInt();
        this.mSpeedLimitExceededCount = parcel.readInt();
        this.mIsDangerousTime = parcel.readInt();
    }

    //creator - used when un-parceling our parcle (creating the object)
    public static final Parcelable.Creator<UserScore> CREATOR = new Parcelable.Creator<UserScore>(){

        @Override
        public UserScore createFromParcel(Parcel parcel) {
            return new UserScore(parcel);
        }

        @Override
        public UserScore[] newArray(int size) {
            return new UserScore[size];
        }
    };

    //return hashcode of object
    public int describeContents() {
        return hashCode();
    }
}
