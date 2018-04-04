package com.example.michele.guidasicuro;

/**
 * Created by Michele on 04/04/2018.
 */

public class UserParameters {
    public interface UserParametersListener {
        public void onSpeedLimitExceeded();
        public void onHardBraking();
        public void onDangerousTime();
    }

    private int[] mSpeedMeasures;
    private UserParametersListener mUserParametersListener;

    public UserParameters() {
        mUserParametersListener = null;
        mSpeedMeasures = null;
    }

    public void setUserParametersListener(UserParametersListener listener) {
        mUserParametersListener = listener;
    }

    public void setSpeedMeasures(int[] speed) {
        mSpeedMeasures = speed;
    }

    // TODO: fire the events
}
