package com.carrental.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Reservation {

    private String reservationId;
    private String carId;

    @JsonProperty("from")
    private LocalDate fromDate; // nullable — may be missing

    @JsonProperty("to")
    private LocalDate toDate; // nullable — may be missing

    private String status; // nullable — may be missing

    public Reservation() {}

    public Reservation(String reservationId, String carId, LocalDate fromDate, LocalDate toDate, String status) {
        this.reservationId = reservationId;
        this.carId = carId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.status = status;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
