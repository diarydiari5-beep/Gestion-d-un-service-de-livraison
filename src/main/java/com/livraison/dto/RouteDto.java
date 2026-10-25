package com.livraison.dto;

import java.util.List;

public class RouteDto {

    private List<double[]> points;
    private double distanceKm;
    private double durationMinutes;
    private boolean approximative;

    public RouteDto() {
    }

    public RouteDto(List<double[]> points, double distanceKm, double durationMinutes, boolean approximative) {
        this.points = points;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.approximative = approximative;
    }

    public List<double[]> getPoints() {
        return points;
    }

    public void setPoints(List<double[]> points) {
        this.points = points;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(double durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isApproximative() {
        return approximative;
    }

    public void setApproximative(boolean approximative) {
        this.approximative = approximative;
    }
}
