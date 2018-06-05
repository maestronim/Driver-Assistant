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
    private String unit;
    private int maxValue;
    private CarParametersListener carParametersListener;

    public CarParameter() {
        this.name = "N.d.";
        this.value = "0";
        this.unit = "N.d.";
        this.maxValue = 100;
    }

    public CarParameter(String name, String value, String unit,int maxValue) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.maxValue = maxValue;
    }

    public void setCarParametersListener(CarParametersListener carParametersListener) {
        this.carParametersListener = carParametersListener;
    }

    public CarParametersListener getCarParametersListener() {
        return this.carParametersListener;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setUnit(String unit) {
        this.unit = unit;
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

    public String getUnit() {
        return this.unit;
    }

    public int getMaxValue() {
        return this.maxValue;
    }
}
