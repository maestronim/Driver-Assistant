package com.example.michele.guidasicuro;

import android.graphics.Bitmap;

/**
 * Created by Michele on 31/03/2018.
 */

public class WeatherInfo {
    private String city;
    private String icon;
    private double temperature;
    private double minTemperature;
    private double maxTemperature;
    private String description;
    private int humidity;
    private Wind wind;
    private double sunrise;
    private double sunset;

    public WeatherInfo() {
        wind = new Wind();
    }

    public class Wind {
        private double speed;
        private int direction;

        public void setSpeed(double speed) {
            this.speed = speed;
        }

        public void setDirection(int direction) {
            this.direction = direction;
        }

        public double getSpeed() {
            return speed;
        }

        public int getDirection() {
            return direction;
        }
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setMinTemperature(double minTemperature) {
        this.minTemperature = minTemperature;
    }

    public void setMaxTemperature(double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public void setWind(double speed, int direction) {
        this.wind.setSpeed(speed);
        this.wind.setDirection(direction);
    }

    public void setSunrise(double sunrise) {
        this.sunrise = sunrise;
    }

    public void setSunset(double sunset) {
        this.sunset = sunset;
    }

    public String getCity() {
        return city;
    }

    public String getIcon() {
        return icon;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getMinTemperature() {
        return minTemperature;
    }

    public double getMaxTemperature() {
        return maxTemperature;
    }

    public double getSunrise() {
        return sunrise;
    }

    public double getSunset() {
        return sunset;
    }

    public int getHumidity() {
        return humidity;
    }

    public String getDescription() {
        return description;
    }

    public double getWindSpeed() {
        return wind.getSpeed();
    }

    public int getWindDirection() {
        return wind.getDirection();
    }
}
