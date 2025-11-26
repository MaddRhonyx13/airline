package com.example.airline.controllers;

import com.example.airline.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.ResourceBundle;

public class CancellationController implements Initializable {

    @FXML private TextField pnrSearchField;
    @FXML private Button searchButton;
    @FXML private ProgressIndicator searchProgress;

    @FXML private Label passengerNameLabel;
    @FXML private Label flightDetailsLabel;
    @FXML private Label travelDateLabel;
    @FXML private Label classSeatLabel;
    @FXML private Label baseFareLabel;
    @FXML private Label cancellationChargeLabel;
    @FXML private Label refundAmountLabel;
    @FXML private Label pnrLabel;

    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, String> colPnr;
    @FXML private TableColumn<Reservation, String> colPassenger;
    @FXML private TableColumn<Reservation, String> colFlight;
    @FXML private TableColumn<Reservation, String> colDate;
    @FXML private TableColumn<Reservation, String> colClass;
    @FXML private TableColumn<Reservation, String> colFare;
    @FXML private TableColumn<Reservation, String> colStatus;

    @FXML private Button cancelButton;
    @FXML private Button calculateRefundButton;
    @FXML private Button printTicketButton;

    private ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private Reservation selectedReservation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadAllReservations();
        clearBookingDetails();
    }

    private void setupTableColumns() {
        colPnr.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        colPassenger.setCellValueFactory(new PropertyValueFactory<>("passengerName"));
        colFlight.setCellValueFactory(new PropertyValueFactory<>("flight"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("travelDate"));
        colClass.setCellValueFactory(new PropertyValueFactory<>("seatClass"));
        colFare.setCellValueFactory(new PropertyValueFactory<>("finalFare"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<Reservation, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "Confirmed":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "Cancelled":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        case "Waiting":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        reservationsTable.setItems(reservations);

        reservationsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        selectedReservation = newSelection;
                        displayBookingDetails(newSelection);
                    }
                });
    }

    @FXML
    private void handleSearch() {
        String pnr = pnrSearchField.getText().trim().toUpperCase();

        if (pnr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Required", "Please enter a PNR number to search.");
            return;
        }

        searchProgress.setVisible(true);

        try {
            Reservation reservation = findReservationByPNR(pnr);

            if (reservation != null) {
                selectedReservation = reservation;
                displayBookingDetails(reservation);
                showAlert(Alert.AlertType.INFORMATION, "Booking Found",
                        "Booking found for PNR: " + pnr);
            } else {
                showAlert(Alert.AlertType.ERROR, "Not Found",
                        "No booking found with PNR: " + pnr);
                clearBookingDetails();
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Search Error",
                    "Error searching booking: " + e.getMessage());
        } finally {
            searchProgress.setVisible(false);
        }
    }

    @FXML
    private void handleCalculateRefund() {
        if (selectedReservation == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a booking first.");
            return;
        }

        if ("Cancelled".equals(selectedReservation.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Already Cancelled",
                    "This booking is already cancelled. No refund available.");
            return;
        }

        try {
            double baseAmount = selectedReservation.getFinalFare();
            double cancellationCharge = calculateCancellationCharge(selectedReservation);
            double refundAmount = baseAmount - cancellationCharge;

            baseFareLabel.setText("M" + String.format("%.2f", baseAmount));
            cancellationChargeLabel.setText("M" + String.format("%.2f", cancellationCharge));
            refundAmountLabel.setText("M" + String.format("%.2f", refundAmount));

            showAlert(Alert.AlertType.INFORMATION, "Refund Calculated",
                    "Refund Details:\n" +
                            "Base Fare: M" + String.format("%.2f", baseAmount) + "\n" +
                            "Cancellation Charge: M" + String.format("%.2f", cancellationCharge) + "\n" +
                            "Refund Amount: M" + String.format("%.2f", refundAmount));

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Calculation Error",
                    "Error calculating refund: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelBooking() {
        if (selectedReservation == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a booking to cancel.");
            return;
        }

        if ("Cancelled".equals(selectedReservation.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Already Cancelled",
                    "This booking is already cancelled.");
            return;
        }

        // Calculate refund before cancellation
        double baseAmount = selectedReservation.getFinalFare();
        double cancellationCharge = calculateCancellationCharge(selectedReservation);
        double refundAmount = baseAmount - cancellationCharge;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancellation");
        confirm.setHeaderText("Cancel Booking: " + selectedReservation.getPnr());
        confirm.setContentText(
                "Are you sure you want to cancel this booking?\n\n" +
                        "Passenger: " + selectedReservation.getPassengerName() + "\n" +
                        "Flight: " + selectedReservation.getFlight() + "\n" +
                        "Travel Date: " + selectedReservation.getTravelDate() + "\n\n" +
                        "Refund Details:\n" +
                        "Base Fare: M" + String.format("%.2f", baseAmount) + "\n" +
                        "Cancellation Charge: M" + String.format("%.2f", cancellationCharge) + "\n" +
                        "Refund Amount: M" + String.format("%.2f", refundAmount) + "\n\n" +
                        "This action cannot be undone."
        );

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (cancelReservationInDatabase(selectedReservation, cancellationCharge, refundAmount)) {

                    selectedReservation.setStatus("Cancelled");
                    reservationsTable.refresh();

                    clearBookingDetails();
                    pnrSearchField.clear();

                    showAlert(Alert.AlertType.INFORMATION, "Cancellation Successful",
                            "Booking cancelled successfully!\n\n" +
                                    "PNR: " + selectedReservation.getPnr() + "\n" +
                                    "Refund Amount: M" + String.format("%.2f", refundAmount) + "\n" +
                                    "Refund will be processed within 7-10 working days.");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Cancellation Error",
                        "Error cancelling booking: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handlePrintTicket() {
        if (selectedReservation == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a booking to print ticket.");
            return;
        }

        try {
            printTicket(selectedReservation);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Print Error",
                    "Error printing ticket: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadAllReservations();
        clearBookingDetails();
        pnrSearchField.clear();
        showAlert(Alert.AlertType.INFORMATION, "Refreshed", "Booking list updated successfully!");
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) pnrSearchField.getScene().getWindow();
        stage.close();
    }

    private void loadAllReservations() {
        reservations.clear();

        String sql = """
            SELECT r.pnr_number, c.cust_name, r.f_code, f.f_name, r.travel_date,
                   r.class_type, r.seat_number, r.final_fare, r.status,
                   f.source_place, f.destination_place, f.departure_time, f.arrival_time
            FROM reservations r
            JOIN customer_details c ON r.pnr_number = c.pnr_number
            JOIN flight_information f ON r.f_code = f.f_code
            ORDER BY r.travel_date DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Reservation reservation = new Reservation(
                        rs.getString("pnr_number"),
                        rs.getString("cust_name"),
                        rs.getString("f_code") + " - " + rs.getString("f_name"),
                        rs.getString("travel_date"),
                        rs.getString("class_type"),
                        rs.getString("seat_number"),
                        rs.getDouble("final_fare"),
                        rs.getString("status"),
                        rs.getString("source_place"),
                        rs.getString("destination_place"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time")
                );
                reservations.add(reservation);
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Error loading reservations: " + e.getMessage());
        }
    }

    private Reservation findReservationByPNR(String pnr) throws SQLException {
        String sql = """
            SELECT r.pnr_number, c.cust_name, r.f_code, f.f_name, r.travel_date,
                   r.class_type, r.seat_number, r.final_fare, r.status,
                   f.source_place, f.destination_place, f.departure_time, f.arrival_time
            FROM reservations r
            JOIN customer_details c ON r.pnr_number = c.pnr_number
            JOIN flight_information f ON r.f_code = f.f_code
            WHERE r.pnr_number = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, pnr);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Reservation(
                        rs.getString("pnr_number"),
                        rs.getString("cust_name"),
                        rs.getString("f_code") + " - " + rs.getString("f_name"),
                        rs.getString("travel_date"),
                        rs.getString("class_type"),
                        rs.getString("seat_number"),
                        rs.getDouble("final_fare"),
                        rs.getString("status"),
                        rs.getString("source_place"),
                        rs.getString("destination_place"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time")
                );
            }
        }
        return null;
    }

    private double calculateCancellationCharge(Reservation reservation) {
        try {
            LocalDate travelDate = LocalDate.parse(reservation.getTravelDate());
            LocalDate currentDate = LocalDate.now();
            long daysUntilTravel = ChronoUnit.DAYS.between(currentDate, travelDate);

            double baseFare = reservation.getFinalFare();

            if (daysUntilTravel > 30) {
                return baseFare * 0.10;
            } else if (daysUntilTravel > 15) {
                return baseFare * 0.25;
            } else if (daysUntilTravel > 7) {
                return baseFare * 0.50;
            } else if (daysUntilTravel > 2) {
                return baseFare * 0.75;
            } else {
                return baseFare * 0.90;
            }

        } catch (Exception e) {
            return reservation.getFinalFare() * 0.25;
        }
    }

    private boolean cancelReservationInDatabase(Reservation reservation, double cancellationCharge, double refundAmount) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String updateReservationSQL = "UPDATE reservations SET status = 'Cancelled' WHERE pnr_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateReservationSQL)) {
                pstmt.setString(1, reservation.getPnr());
                pstmt.executeUpdate();
            }

            String freeSeatSQL = "UPDATE seat_allocation SET is_available = 1, pnr_number = NULL WHERE pnr_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(freeSeatSQL)) {
                pstmt.setString(1, reservation.getPnr());
                pstmt.executeUpdate();
            }

            String flightCode = reservation.getFlight().split(" - ")[0];
            String updateFlightSQL = reservation.getSeatClass().equals("Economy") ?
                    "UPDATE flight_information SET eco_seats_booked = eco_seats_booked - 1 WHERE f_code = ?" :
                    "UPDATE flight_information SET exe_seats_booked = exe_seats_booked - 1 WHERE f_code = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(updateFlightSQL)) {
                pstmt.setString(1, flightCode);
                pstmt.executeUpdate();
            }

            String insertCancellationSQL = """
                INSERT INTO cancellations 
                (pnr_number, f_code, class_type, base_amount, cancellation_charge, refund_amount, reason, cancelled_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement pstmt = conn.prepareStatement(insertCancellationSQL)) {
                pstmt.setString(1, reservation.getPnr());
                pstmt.setString(2, flightCode);
                pstmt.setString(3, reservation.getSeatClass());
                pstmt.setDouble(4, reservation.getFinalFare());
                pstmt.setDouble(5, cancellationCharge);
                pstmt.setDouble(6, refundAmount);
                pstmt.setString(7, "Customer Request");
                pstmt.setInt(8, 1); // System user ID
                pstmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    private void printTicket(Reservation reservation) {
        String ticket = """
            ╔══════════════════════════════════════════════════╗
            ║                E-TICKET / BOARDING PASS          ║
            ╠══════════════════════════════════════════════════╣
            ║  PNR: %s                                        ║
            ║  Passenger: %-30s ║
            ║  Flight: %-35s ║
            ║  Route: %s → %-25s ║
            ║  Date: %-12s Time: %s - %s          ║
            ║  Class: %-10s Seat: %-10s           ║
            ║  Fare: M%-10.2f Status: %-12s     ║
            ╠══════════════════════════════════════════════════╣
            ║           ✈️  HAVE A SAFE JOURNEY! ✈️            ║
            ╚══════════════════════════════════════════════════╝
            """.formatted(
                reservation.getPnr(),
                reservation.getPassengerName(),
                reservation.getFlight(),
                reservation.getSourcePlace(),
                reservation.getDestinationPlace(),
                reservation.getTravelDate(),
                reservation.getDepartureTime(),
                reservation.getArrivalTime(),
                reservation.getSeatClass(),
                reservation.getSeatNumber(),
                reservation.getFinalFare(),
                reservation.getStatus()
        );

        TextArea ticketArea = new TextArea(ticket);
        ticketArea.setEditable(false);
        ticketArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

        Alert ticketAlert = new Alert(Alert.AlertType.INFORMATION);
        ticketAlert.setTitle("E-Ticket - " + reservation.getPnr());
        ticketAlert.setHeaderText("Electronic Ticket");
        ticketAlert.getDialogPane().setContent(ticketArea);
        ticketAlert.getDialogPane().setPrefSize(600, 400);

        ButtonType printButton = new ButtonType("Print", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        ticketAlert.getButtonTypes().setAll(printButton, closeButton);

        Optional<ButtonType> result = ticketAlert.showAndWait();
        if (result.isPresent() && result.get() == printButton) {
            showAlert(Alert.AlertType.INFORMATION, "Print",
                    "Ticket sent to printer!\nIn a real application, this would connect to a printer.");
        }
    }

    private void displayBookingDetails(Reservation reservation) {
        passengerNameLabel.setText(reservation.getPassengerName());
        flightDetailsLabel.setText(reservation.getFlight());
        travelDateLabel.setText(reservation.getTravelDate());
        classSeatLabel.setText(reservation.getSeatClass() + " | Seat: " + reservation.getSeatNumber());
        baseFareLabel.setText("M" + String.format("%.2f", reservation.getFinalFare()));
        pnrLabel.setText(reservation.getPnr());

        cancellationChargeLabel.setText("--");
        refundAmountLabel.setText("--");

        boolean isCancelled = "Cancelled".equals(reservation.getStatus());
        calculateRefundButton.setDisable(isCancelled);
        cancelButton.setDisable(isCancelled);
        printTicketButton.setDisable(false);

        if (isCancelled) {
            cancelButton.setText("Already Cancelled");
            cancelButton.setStyle("-fx-background-color: #95a5a6;");
        } else {
            cancelButton.setText("Cancel Booking");
            cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        }
    }

    private void clearBookingDetails() {
        passengerNameLabel.setText("--");
        flightDetailsLabel.setText("--");
        travelDateLabel.setText("--");
        classSeatLabel.setText("--");
        baseFareLabel.setText("--");
        cancellationChargeLabel.setText("--");
        refundAmountLabel.setText("--");
        pnrLabel.setText("--");

        selectedReservation = null;
        calculateRefundButton.setDisable(true);
        cancelButton.setDisable(true);
        printTicketButton.setDisable(true);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class Reservation {
        private final String pnr;
        private final String passengerName;
        private final String flight;
        private final String travelDate;
        private final String seatClass;
        private final String seatNumber;
        private final double finalFare;
        private String status;
        private final String sourcePlace;
        private final String destinationPlace;
        private final String departureTime;
        private final String arrivalTime;

        public Reservation(String pnr, String passengerName, String flight, String travelDate,
                           String seatClass, String seatNumber, double finalFare, String status,
                           String sourcePlace, String destinationPlace, String departureTime, String arrivalTime) {
            this.pnr = pnr;
            this.passengerName = passengerName;
            this.flight = flight;
            this.travelDate = travelDate;
            this.seatClass = seatClass;
            this.seatNumber = seatNumber;
            this.finalFare = finalFare;
            this.status = status;
            this.sourcePlace = sourcePlace;
            this.destinationPlace = destinationPlace;
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
        }

        public String getPnr() { return pnr; }
        public String getPassengerName() { return passengerName; }
        public String getFlight() { return flight; }
        public String getTravelDate() { return travelDate; }
        public String getSeatClass() { return seatClass; }
        public String getSeatNumber() { return seatNumber; }
        public double getFinalFare() { return finalFare; }
        public String getStatus() { return status; }
        public String getSourcePlace() { return sourcePlace; }
        public String getDestinationPlace() { return destinationPlace; }
        public String getDepartureTime() { return departureTime; }
        public String getArrivalTime() { return arrivalTime; }

        public void setStatus(String status) { this.status = status; }
    }
}
