package com.example.airline;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Reservation {
    private int reservationId;
    private String pnrNumber;
    private String flightCode;
    private int customerId;
    private String seatClass;
    private int seatNumber;
    private String seatPreference;
    private BigDecimal baseFare;
    private BigDecimal finalFare;
    private String concessionType;
    private String status;
    private LocalDateTime createdAt;

    public Reservation(int reservationId, String pnrNumber, String flightCode, int customerId, String seatClass, int seatNumber, String seatPreference, BigDecimal baseFare, BigDecimal finalFare, String concessionType, String status, LocalDateTime createdAt) {
        this.reservationId = reservationId;
        this.pnrNumber = pnrNumber;
        this.flightCode = flightCode;
        this.customerId = customerId;
        this.seatClass = seatClass;
        this.seatNumber = seatNumber;
        this.seatPreference = seatPreference;
        this.baseFare = baseFare;
        this.finalFare = finalFare;
        this.concessionType = concessionType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getReservationId() {
        return reservationId;
    }
    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public String getPnrNumber() {
        return pnrNumber;
    }
    public void setPnrNumber(String pnrNumber) {
        this.pnrNumber = pnrNumber;
    }

    public String getFlightCode() {
        return flightCode;
    }
    public void setFlightCode(String flightCode) {
        this.flightCode = flightCode;
    }

    public int getCustomerId() {
        return customerId;
    }
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getSeatClass() {
        return seatClass;
    }
    public void setSeatClass(String seatClass) {
        this.seatClass = seatClass;
    }

    public int getSeatNumber() {
        return seatNumber;
    }
    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatPreference() {
        return seatPreference;
    }
    public void setSeatPreference(String seatPreference) {
        this.seatPreference = seatPreference;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }
    public void setBaseFare(BigDecimal baseFare) {
        this.baseFare = baseFare;
    }

    public BigDecimal getFinalFare() {
        return finalFare;
    }
    public void setFinalFare(BigDecimal finalFare) {
        this.finalFare = finalFare;
    }

    public String getConcessionType() {
        return concessionType;
    }
    public void setConcessionType(String concessionType) {
        this.concessionType = concessionType;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "pnrNumber=" + pnrNumber + " - " +
                ", flightCode=" + flightCode + " - " +
                ", seatClass=" + seatClass + " - " +
                ", status=" + status + " - " +
                '}';
    }
}
