package com.example.michele.guidasicuro;

import java.util.ArrayList;

/**
 * Created by Michele on 30/04/2018.
 */
public class CarParameter {
    public interface CarParametersListener {
        public void onCarParametersChanged(ArrayList<CarParameter> arrayOfCarParameters);
    }

    private String name;
    private String value;
    private int maxValue;
    private CarParametersListener carParametersListener;

    public CarParameter() {
        this.name = "N.d.";
        this.value = "N.d.";
        this.maxValue = 100;
    }

    public CarParameter(String name, String value, int maxValue) {
        this.name = name;
        this.value = value;
        this.maxValue = maxValue;
    }

    public void setCarParametersListener(CarParametersListener carParametersListener) {
        this.carParametersListener = carParametersListener;
    }

    public void onCarParametersChanged(ArrayList<CarParameter> arrayOfCarParameters) {
        carParametersListener.onCarParametersChanged(arrayOfCarParameters);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public int getMaxValue() {
        return this.maxValue;
    }
}
