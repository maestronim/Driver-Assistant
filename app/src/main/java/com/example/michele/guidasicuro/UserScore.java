package com.example.michele.guidasicuro;

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
    private boolean mIsDangerousTime;
    private UserScoreListener mUserScoreListener;

    public UserScore() {
        mUserScoreListener = null;
        mHardBrakingCount = 0;
        mSpeedLimitExceededCount = 0;
        mIsDangerousTime = false;
    }

    public void setHardBrakingCount(int hardBrakingCount) {
        this.mHardBrakingCount = hardBrakingCount;
    }

    public void setSpeedLimitExceededCount(int speedLimitExceededCount) {
        this.mSpeedLimitExceededCount = speedLimitExceededCount;
    }

    public void setIsDangerousTime(boolean isDangerousTime) {
        this.mIsDangerousTime = isDangerousTime;
    }

    public void setUserParametersListener(UserScoreListener listener) {
        mUserScoreListener = listener;
    }

    public int getHardBrakingCount() {
        return this.mHardBrakingCount;
    }

    public int getSpeedLimitExceededCount() {
        return this.mSpeedLimitExceededCount;
    }

    public boolean isIsDangerousTime() {
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

    // TODO: fire the events
}
