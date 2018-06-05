package com.example.michele.guidasicuro;

/**
 * Created by Michele on 30/04/2018.
 */

public class CarFaultCodes {
    public interface CarFaultCodesListener {
        public void onCarFaultCodesChanged(String carFaultCodes);
    }

    public CarFaultCodes() {}

    private CarFaultCodesListener carFaultCodesListener;

    public void setCarFaultCodesListener(CarFaultCodesListener carFaultCodesListener) {
        this.carFaultCodesListener = carFaultCodesListener;
    }

    public CarFaultCodesListener getCarFaultCodesListener() {
        return this.carFaultCodesListener;
    }
}
