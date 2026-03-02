package com.carrental.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AvailabilityResponse {

    private String carId;
    private String model;

    @JsonProperty("isAvailable")
    private boolean available;

    private int conflicts;
    private Double estimatedPrice; // nullable

    public AvailabilityResponse() {}

    public AvailabilityResponse(String carId, String model, boolean available, int conflicts, Double estimatedPrice) {
        this.carId = carId;
        this.model = model;
        this.available = available;
        this.conflicts = conflicts;
        this.estimatedPrice = estimatedPrice;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getConflicts() {
        return conflicts;
    }

    public void setConflicts(int conflicts) {
        this.conflicts = conflicts;
    }

    public Double getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(Double estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }
}
