package com.example.michele.guidasicuro;

import android.graphics.Bitmap;

/**
 * Created by Michele on 31/03/2018.
 */

public class WeatherInfo {
    private String city;
    private String icon;
    private double temperature;
    private int minTemperature;
    private int maxTemperature;
    private String description;
    private int humidity;
    private Wind wind;
    private long sunrise;
    private long sunset;

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

    public void setMinTemperature(int minTemperature) {
        this.minTemperature = minTemperature;
    }

    public void setMaxTemperature(int maxTemperature) {
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

    public void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    public void setSunset(long sunset) {
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

    public int getMinTemperature() {
        return minTemperature;
    }

    public int getMaxTemperature() {
        return maxTemperature;
    }

    public long getSunrise() {
        return sunrise;
    }

    public long getSunset() {
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
