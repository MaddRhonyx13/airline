package com.example.airline;

public class Flight {
    private int flightId;
    private String flightCode;
    private String flightName;
    private String route;
    private String sourcePlace;
    private String destinationPlace;
    private String departureTime;
    private String arrivalTime;
    private int economySeats;
    private int businessSeats;
    private int ecoSeatsBooked;
    private int exeSeatsBooked;
    private boolean isActive;

    public Flight() {}

    public Flight(String flightCode, String flightName, String route, String sourcePlace,
                  String destinationPlace, String departureTime, String arrivalTime,
                  int economySeats, int businessSeats, int ecoSeatsBooked, int exeSeatsBooked) {
        this.flightCode = flightCode;
        this.flightName = flightName;
        this.route = route;
        this.sourcePlace = sourcePlace;
        this.destinationPlace = destinationPlace;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.economySeats = economySeats;
        this.businessSeats = businessSeats;
        this.ecoSeatsBooked = ecoSeatsBooked;
        this.exeSeatsBooked = exeSeatsBooked;
        this.isActive = true;
    }

    public int getFlightId() { return flightId; }
    public void setFlightId(int flightId) { this.flightId = flightId; }

    public String getFlightCode() { return flightCode; }
    public void setFlightCode(String flightCode) { this.flightCode = flightCode; }

    public String getFlightName() { return flightName; }
    public void setFlightName(String flightName) { this.flightName = flightName; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public String getSourcePlace() { return sourcePlace; }
    public void setSourcePlace(String sourcePlace) { this.sourcePlace = sourcePlace; }

    public String getDestinationPlace() { return destinationPlace; }
    public void setDestinationPlace(String destinationPlace) { this.destinationPlace = destinationPlace; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public int getEconomySeats() { return economySeats; }
    public void setEconomySeats(int economySeats) { this.economySeats = economySeats; }

    public int getBusinessSeats() { return businessSeats; }
    public void setBusinessSeats(int businessSeats) { this.businessSeats = businessSeats; }

    public int getEcoSeatsBooked() { return ecoSeatsBooked; }
    public void setEcoSeatsBooked(int ecoSeatsBooked) { this.ecoSeatsBooked = ecoSeatsBooked; }

    public int getExeSeatsBooked() { return exeSeatsBooked; }
    public void setExeSeatsBooked(int exeSeatsBooked) { this.exeSeatsBooked = exeSeatsBooked; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getAvailableEconomySeats() { return economySeats - ecoSeatsBooked; }
    public int getAvailableBusinessSeats() { return businessSeats - exeSeatsBooked; }

    @Override
    public String toString() {
        return flightCode + " - " + flightName + " (" + sourcePlace + " to " + destinationPlace + ")";
    }
}
