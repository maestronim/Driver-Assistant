package com.example.michele.guidasicuro;

import android.widget.TextView;

import java.util.Calendar;

/**
 * Created by Michele on 04/04/2018.
 */

public class UserScore {
    public interface UserScoreListener {
        public void onSpeedLimitExceeded();
        public void onHardBraking();
        public void onDangerousTime();
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
                mUserScoreListener.onHardBraking();
            }
        }
    }

    public void checkDangerousTime() {
        Calendar rightNow = Calendar.getInstance();
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);

        if(currentHour >= 22 || currentHour <= 6) {
            mUserScoreListener.onDangerousTime();
        }
    }
}
