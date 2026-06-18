package com.healthapp.backend.model;

public class DashboardData {
    private int healthScore;
    private int heartRate;
    private int steps;
    private double water;
    private double sleep;
    private String appointmentDoctor;
    private String appointmentTime;

    public DashboardData() {}

    public DashboardData(int healthScore, int heartRate, int steps, double water, double sleep, String appointmentDoctor, String appointmentTime) {
        this.healthScore = healthScore;
        this.heartRate = heartRate;
        this.steps = steps;
        this.water = water;
        this.sleep = sleep;
        this.appointmentDoctor = appointmentDoctor;
        this.appointmentTime = appointmentTime;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public double getWater() {
        return water;
    }

    public void setWater(double water) {
        this.water = water;
    }

    public double getSleep() {
        return sleep;
    }

    public void setSleep(double sleep) {
        this.sleep = sleep;
    }

    public String getAppointmentDoctor() {
        return appointmentDoctor;
    }

    public void setAppointmentDoctor(String appointmentDoctor) {
        this.appointmentDoctor = appointmentDoctor;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
}
