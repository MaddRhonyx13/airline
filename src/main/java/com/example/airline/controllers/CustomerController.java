package com.example.airline.controllers;

import com.example.airline.DatabaseConnection;
import com.example.airline.Customer;
import com.example.airline.Flight;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CustomerController implements Initializable {

    @FXML private DatePicker travelDate;
    @FXML private TextField custName;
    @FXML private TextField fatherName;
    @FXML private ComboBox<String> gender;
    @FXML private DatePicker dob;
    @FXML private TextField telNo;
    @FXML private TextField profession;
    @FXML private ComboBox<String> concession;
    @FXML private TextArea address;
    @FXML private ComboBox<String> sourcePlace;
    @FXML private ComboBox<String> destinationPlace;
    @FXML private ComboBox<String> seatClass;
    @FXML private ComboBox<String> seatPreference;
    @FXML private ListView<String> availableFlights;
    @FXML private TitledPane flightsSection;
    @FXML private TextField baseFare;
    @FXML private TextField discount;
    @FXML private TextField finalFare;
    @FXML private TextField pnrNumber;
    @FXML private ProgressBar reservationProgress;
    @FXML private Button confirmButton;
    @FXML private Label availableSeatsLabel;


    private List<Flight> flights = new ArrayList<>();
    private Flight selectedFlight;
    private ObservableList<String> flightItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeComboBoxes();
        setupVisualEffects();
        loadFlightsFromDatabase();
        setupListeners();

        travelDate.setValue(LocalDate.now().plusDays(1));
    }

    private void initializeComboBoxes() {
        gender.getItems().addAll("Male", "Female", "Other");
        concession.getItems().addAll("None", "Student", "Senior Citizen", "Cancer Patient");
        sourcePlace.getItems().addAll("Maseru", "Johannesburg", "Durban", "Capetown", "Bloemfontein", "eSwatini");
        destinationPlace.getItems().addAll("Maseru", "Johannesburg", "Durban", "Capetown", "Bloemfontein", "eSwatini");
        seatClass.getItems().addAll("Economy", "Business");
        seatPreference.getItems().addAll("Any", "Window", "Aisle");

        concession.setValue("None");
        seatClass.setValue("Economy");
        seatPreference.setValue("Any");
        sourcePlace.setValue("Delhi");
        destinationPlace.setValue("Mumbai");

        availableFlights.setItems(flightItems);
    }

    private void setupVisualEffects() {
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        confirmButton.setEffect(dropShadow);

        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(2), confirmButton);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.3);
        fadeTransition.setCycleCount(FadeTransition.INDEFINITE);
        fadeTransition.setAutoReverse(true);
        fadeTransition.play();
    }

    private void setupListeners() {
        sourcePlace.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && destinationPlace.getValue() != null && !newVal.equals(destinationPlace.getValue())) {
                searchFlights();
            }
        });

        destinationPlace.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && sourcePlace.getValue() != null && !newVal.equals(sourcePlace.getValue())) {
                searchFlights();
            }
        });

        seatClass.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedFlight != null) {
                calculateFare();
            }
        });

        concession.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedFlight != null) {
                calculateFare();
            }
        });
    }

    private void loadFlightsFromDatabase() {
        String sql = "SELECT f_code, f_name, route, source_place, destination_place, " +
                "departure_time, arrival_time, t_eco_seatno, t_exe_seatno, " +
                "eco_seats_booked, exe_seats_booked FROM flight_information WHERE is_active = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            flights.clear();
            while (rs.next()) {
                Flight flight = new Flight(
                        rs.getString("f_code"),
                        rs.getString("f_name"),
                        rs.getString("route"),
                        rs.getString("source_place"),
                        rs.getString("destination_place"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time"),
                        rs.getInt("t_eco_seatno"),
                        rs.getInt("t_exe_seatno"),
                        rs.getInt("eco_seats_booked"),
                        rs.getInt("exe_seats_booked")
                );
                flights.add(flight);
            }
            System.out.println("Loaded " + flights.size() + " flights from database");

        } catch (SQLException e) {
            System.err.println("Error loading flights: " + e.getMessage());
            loadDemoFlights();
        }
    }

    private void loadDemoFlights() {
        flights.clear();
        flights.add(new Flight("AE101", "Air African Express", "Maseru-Johannesburg", "Maseru", "Johannesburg", "08:00", "10:30", 150, 20, 45, 8));
    }

    @FXML
    private void searchFlights() {
        try {
            if (sourcePlace.getValue() == null || destinationPlace.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Input Required", "Please select both source and destination");
                return;
            }

            if (sourcePlace.getValue().equals(destinationPlace.getValue())) {
                showAlert(Alert.AlertType.WARNING, "Invalid Route", "Source and destination cannot be the same");
                return;
            }

            reservationProgress.setProgress(0.3);
            flightsSection.setExpanded(true);

            String source = sourcePlace.getValue();
            String destination = destinationPlace.getValue();
            String seatClassValue = seatClass.getValue();

            flightItems.clear();
            selectedFlight = null;

            int foundFlights = 0;
            for (Flight flight : flights) {
                if (flight.getSourcePlace().equals(source) && flight.getDestinationPlace().equals(destination)) {
                    int availableSeats = seatClassValue.equals("Economy") ?
                            flight.getEconomySeats() - flight.getEcoSeatsBooked() :
                            flight.getBusinessSeats() - flight.getExeSeatsBooked();

                    if (availableSeats > 0) {
                        double fare = getFlightFare(flight, seatClassValue);
                        String displayText = String.format("%s - %s | %s-%s | %s | %d seats | M%.0f",
                                flight.getFlightCode(),
                                flight.getFlightName(),
                                flight.getDepartureTime(),
                                flight.getArrivalTime(),
                                seatClassValue,
                                availableSeats,
                                fare
                        );
                        flightItems.add(displayText);
                        foundFlights++;
                    }
                }
            }

            if (foundFlights == 0) {
                flightItems.add("No available flights found for the selected route and class.");
                availableSeatsLabel.setText("No flights available");
            } else {
                availableSeatsLabel.setText("Found " + foundFlights + " available flights");
                showAlert(Alert.AlertType.INFORMATION, "Search Complete",
                        "Found " + foundFlights + " flights from " + source + " to " + destination);
            }

            reservationProgress.setProgress(0.6);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Search Error", "Error searching flights: " + e.getMessage());
            reservationProgress.setProgress(0.0);
        }
    }

    private double getFlightFare(Flight flight, String seatClass) {
        String sql = "SELECT base_fare FROM fare WHERE f_code = ? AND class_type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, flight.getFlightCode());
            pstmt.setString(2, seatClass);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("base_fare");
            }
        } catch (SQLException e) {
            System.err.println("Error getting fare: " + e.getMessage());
        }

        return seatClass.equals("Economy") ? 2500.0 : 4500.0;
    }

    @FXML
    private void selectFlight() {
        try {
            int selectedIndex = availableFlights.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < flights.size()) {
                String source = sourcePlace.getValue();
                String destination = destinationPlace.getValue();
                List<Flight> matchingFlights = new ArrayList<>();

                for (Flight flight : flights) {
                    if (flight.getSourcePlace().equals(source) && flight.getDestinationPlace().equals(destination)) {
                        int availableSeats = seatClass.getValue().equals("Economy") ?
                                flight.getEconomySeats() - flight.getEcoSeatsBooked() :
                                flight.getBusinessSeats() - flight.getExeSeatsBooked();
                        if (availableSeats > 0) {
                            matchingFlights.add(flight);
                        }
                    }
                }

                if (selectedIndex < matchingFlights.size()) {
                    selectedFlight = matchingFlights.get(selectedIndex);
                    reservationProgress.setProgress(0.8);
                    calculateFare();
                    updateAvailableSeats();
                    showAlert(Alert.AlertType.INFORMATION, "Flight Selected",
                            "You selected: " + selectedFlight.getFlightCode());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a flight from the list");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Selection Error", "Error selecting flight: " + e.getMessage());
        }
    }

    public void setSelectedFlightFromSearch(Flight flight, String source, String destination,
                                            String seatClass, LocalDate travelDate, double baseFare) {
        this.selectedFlight = flight;

        sourcePlace.setValue(source);
        destinationPlace.setValue(destination);

        this.travelDate.setValue(travelDate);

        this.seatClass.setValue(seatClass);

        searchFlights();

        for (int i = 0; i < availableFlights.getItems().size(); i++) {
            String item = availableFlights.getItems().get(i);
            if (item.contains(flight.getFlightCode())) {
                availableFlights.getSelectionModel().select(i);
                break;
            }
        }

        calculateFare();

        showAlert(Alert.AlertType.INFORMATION, "Flight Auto-Selected",
                "Flight " + flight.getFlightCode() + " has been auto-selected from your search.\n" +
                        "Please complete the passenger details below.");
    }


    private void updateAvailableSeats() {
        if (selectedFlight != null) {
            int availableSeats = seatClass.getValue().equals("Economy") ?
                    selectedFlight.getEconomySeats() - selectedFlight.getEcoSeatsBooked() :
                    selectedFlight.getBusinessSeats() - selectedFlight.getExeSeatsBooked();

            availableSeatsLabel.setText("Available seats: " + availableSeats);
        }
    }

    @FXML
    private void refreshFlights() {
        try {
            flightItems.clear();
            selectedFlight = null;
            loadFlightsFromDatabase();
            searchFlights();
            showAlert(Alert.AlertType.INFORMATION, "Refresh Complete",
                    "Flight list refreshed successfully!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Refresh Error", "Error refreshing flights: " + e.getMessage());
        }
    }

    @FXML
    private void calculateFare() {
        try {
            if (selectedFlight == null) {
                showAlert(Alert.AlertType.WARNING, "No Flight", "Please select a flight first");
                return;
            }

            double base = getFlightFare(selectedFlight, seatClass.getValue());
            baseFare.setText("M" + base);

            double discountRate = getDiscountRate();
            double discountAmount = base * discountRate;
            double finalAmount = base - discountAmount;

            discount.setText("-M" + String.format("%.2f", discountAmount) + " (" + (discountRate * 100) + "%)");
            finalFare.setText("M" + String.format("%.2f", finalAmount));

            if (pnrNumber.getText().isEmpty()) {
                pnrNumber.setText(generatePNR());
            }

            reservationProgress.setProgress(0.9);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Calculation Error", "Error calculating fare: " + e.getMessage());
        }
    }

    private String generatePNR() {
        return "PNR" + (10000 + (int)(Math.random() * 90000));
    }

    private double getDiscountRate() {
        String concessionType = concession.getValue();
        switch (concessionType) {
            case "Student": return 0.25;
            case "Senior Citizen": return 0.13;
            case "Cancer Patient": return 0.569;
            default: return 0.0;
        }
    }

    @FXML
    private void confirmReservation() {
        try {
            if (validateForm() && selectedFlight != null) {
                Customer customer = saveCustomerToDatabase();
                if (customer == null) {
                    throw new SQLException("Failed to save customer details");
                }

                saveReservationToDatabase(customer);
                updateFlightSeats();
                allocateSeat();

                reservationProgress.setProgress(1.0);

                showAlert(Alert.AlertType.INFORMATION, "Reservation Confirmed",
                        "YOUR RESERVATION IS CONFIRMED!\n\n" +
                                "PNR Number: " + pnrNumber.getText() + "\n" +
                                "Passenger: " + custName.getText() + "\n" +
                                "Flight: " + selectedFlight.getFlightCode() + " - " + selectedFlight.getFlightName() + "\n" +
                                "Route: " + selectedFlight.getSourcePlace() + " → " + selectedFlight.getDestinationPlace() + "\n" +
                                "Travel Date: " + travelDate.getValue() + "\n" +
                                "Class: " + seatClass.getValue() + "\n" +
                                "Final Fare: " + finalFare.getText() + "\n\n" +
                                "Your e-ticket has been generated and saved.");

                clearForm();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Reservation Error",
                    "Error confirming reservation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Customer saveCustomerToDatabase() throws SQLException {
        String sql = """
            INSERT INTO customer_details 
            (pnr_number, t_date, cust_name, father_name, gender, d_o_b, address, tel_no, profession, security, concession)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, pnrNumber.getText());
            pstmt.setDate(2, Date.valueOf(travelDate.getValue()));
            pstmt.setString(3, custName.getText());
            pstmt.setString(4, fatherName.getText());
            pstmt.setString(5, gender.getValue());
            pstmt.setDate(6, dob.getValue() != null ? Date.valueOf(dob.getValue()) : null);
            pstmt.setString(7, address.getText());
            pstmt.setString(8, telNo.getText());
            pstmt.setString(9, profession.getText());
            pstmt.setString(10, "Standard");
            pstmt.setString(11, concession.getValue());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating customer failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Customer customer = new Customer();
                    customer.setCustId(generatedKeys.getInt(1));
                    customer.setPnrNumber(pnrNumber.getText());
                    System.out.println("Customer saved with ID: " + customer.getCustId());
                    return customer;
                } else {
                    throw new SQLException("Creating customer failed, no ID obtained.");
                }
            }
        }
    }

    private void saveReservationToDatabase(Customer customer) throws SQLException {
        String sql = """
            INSERT INTO reservations 
            (pnr_number, f_code, cust_id, class_type, seat_number, seat_preference, 
             base_fare, discount_amount, final_fare, concession_type, status, travel_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String seatNum = generateSeatNumber();

            pstmt.setString(1, pnrNumber.getText());
            pstmt.setString(2, selectedFlight.getFlightCode());
            pstmt.setInt(3, customer.getCustId());
            pstmt.setString(4, seatClass.getValue());
            pstmt.setString(5, seatNum);
            pstmt.setString(6, seatPreference.getValue());
            pstmt.setBigDecimal(7, new BigDecimal(baseFare.getText().replace("₹", "")));

            String discountText = discount.getText();
            BigDecimal discountAmount = discountText.isEmpty() ? BigDecimal.ZERO :
                    new BigDecimal(discountText.replace("-₹", "").split(" ")[0]);
            pstmt.setBigDecimal(8, discountAmount);

            pstmt.setBigDecimal(9, new BigDecimal(finalFare.getText().replace("₹", "")));
            pstmt.setString(10, concession.getValue());
            pstmt.setString(11, "Confirmed");
            pstmt.setDate(12, Date.valueOf(travelDate.getValue()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating reservation failed, no rows affected.");
            }
            System.out.println("Reservation saved with seat: " + seatNum);
        }
    }

    private void updateFlightSeats() throws SQLException {
        String sql = seatClass.getValue().equals("Economy") ?
                "UPDATE flight_information SET eco_seats_booked = eco_seats_booked + 1 WHERE f_code = ?" :
                "UPDATE flight_information SET exe_seats_booked = exe_seats_booked + 1 WHERE f_code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, selectedFlight.getFlightCode());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating flight seats failed, no rows affected.");
            }
            System.out.println("Flight seats updated");
        }
    }

    private void allocateSeat() throws SQLException {
        String sql = """
            INSERT INTO seat_allocation (f_code, class_type, seat_number, is_window_seat, pnr_number, is_available)
            VALUES (?, ?, ?, ?, ?, 0)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String seatNum = generateSeatNumber();
            pstmt.setString(1, selectedFlight.getFlightCode());
            pstmt.setString(2, seatClass.getValue());
            pstmt.setString(3, seatNum);
            pstmt.setBoolean(4, "Window".equals(seatPreference.getValue()));
            pstmt.setString(5, pnrNumber.getText());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Allocating seat failed, no rows affected.");
            }
            System.out.println("Seat " + seatNum + " allocated");
        }
    }

    private String generateSeatNumber() {
        String prefix = seatClass.getValue().equals("Economy") ? "E" : "B";
        int seatNum = (int)(Math.random() * 50) + 1;
        return prefix + String.format("%02d", seatNum);
    }

    @FXML
    private void clearForm() {
        travelDate.setValue(LocalDate.now().plusDays(1));
        custName.clear();
        fatherName.clear();
        gender.setValue(null);
        dob.setValue(null);
        telNo.clear();
        profession.clear();
        concession.setValue("None");
        address.clear();
        sourcePlace.setValue("Maseru");
        destinationPlace.setValue("Maseru");
        seatClass.setValue("Economy");
        seatPreference.setValue("Any");
        baseFare.clear();
        discount.clear();
        finalFare.clear();
        pnrNumber.clear();
        reservationProgress.setProgress(0.0);
        availableFlights.getSelectionModel().clearSelection();
        availableSeatsLabel.setText("Select a flight to see available seats");
        selectedFlight = null;
        flightItems.clear();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) travelDate.getScene().getWindow();
        stage.close();
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (custName.getText().isEmpty()) errors.append("• Customer Name\n");
        if (travelDate.getValue() == null) errors.append("• Travel Date\n");
        if (sourcePlace.getValue() == null) errors.append("• Source Place\n");
        if (destinationPlace.getValue() == null) errors.append("• Destination Place\n");
        if (selectedFlight == null) errors.append("• Flight Selection\n");

        if (!errors.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Please fill the following required fields:\n" + errors.toString());
            return false;
        }

        if (sourcePlace.getValue().equals(destinationPlace.getValue())) {
            showAlert(Alert.AlertType.ERROR, "Invalid Route", "Source and destination cannot be the same");
            return false;
        }

        // Check seat availability
        int availableSeats = seatClass.getValue().equals("Economy") ?
                selectedFlight.getEconomySeats() - selectedFlight.getEcoSeatsBooked() :
                selectedFlight.getBusinessSeats() - selectedFlight.getExeSeatsBooked();

        if (availableSeats <= 0) {
            showAlert(Alert.AlertType.ERROR, "Seat Not Available",
                    "Sorry, no " + seatClass.getValue() + " seats available on this flight.");
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
