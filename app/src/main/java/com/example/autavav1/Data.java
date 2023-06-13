package com.example.autavav1;

public class Data {
    private double latitude;
    private double longitude;
    private double speed;
    private double travelTime;
    private double travelDistance;
    private double fuelConsumption;
    private double timestamp;

    public Data(double latitude, double longitude, double speed, double travelTime,
                double travelDistance, double fuelConsumption, double timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.travelTime = travelTime;
        this.travelDistance = travelDistance;
        this.fuelConsumption = fuelConsumption;
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getSpeed() {
        return speed;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public double getTravelDistance() {
        return travelDistance;
    }

    public double getFuelConsumption() {
        return fuelConsumption;
    }

    public double getTimestamp() {
        return timestamp;
    }
}
