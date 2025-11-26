package com.example.airline.controllers;

import com.example.airline.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.ResourceBundle;

public class AllReservationsController implements Initializable {

    @FXML private TableView<ReservationView> reservationsTable;
    @FXML private TableColumn<ReservationView, String> colPnr;
    @FXML private TableColumn<ReservationView, String> colCustomer;
    @FXML private TableColumn<ReservationView, String> colFlight;
    @FXML private TableColumn<ReservationView, String> colRoute;
    @FXML private TableColumn<ReservationView, String> colClass;
    @FXML private TableColumn<ReservationView, String> colSeat;
    @FXML private TableColumn<ReservationView, String> colTravelDate;
    @FXML private TableColumn<ReservationView, String> colFare;
    @FXML private TableColumn<ReservationView, String> colStatus;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchType;
    @FXML private Label totalReservationsLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private ObservableList<ReservationView> reservations = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupSearchComboBox();
        loadReservations();
    }

    private void setupTableColumns() {
        colPnr.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colFlight.setCellValueFactory(new PropertyValueFactory<>("flight"));
        colRoute.setCellValueFactory(new PropertyValueFactory<>("route"));
        colClass.setCellValueFactory(new PropertyValueFactory<>("seatClass"));
        colSeat.setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        colTravelDate.setCellValueFactory(new PropertyValueFactory<>("travelDate"));
        colFare.setCellValueFactory(new PropertyValueFactory<>("finalFare"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<ReservationView, String>() {
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
    }

    private void setupSearchComboBox() {
        searchType.getItems().addAll("All", "PNR", "Customer Name", "Flight", "Status");
        searchType.setValue("All");
        searchType.setStyle("-fx-background-color: white; -fx-border-width: 1px; -fx-border-color: #764ba2; -fx-text-fill: #764ba2; -fx-font-weight: bold; -fx-height: 10px;");
    }

    @FXML
    private void loadReservations() {
        loadingIndicator.setVisible(true);
        reservations.clear();

        String sql = """
            SELECT 
                r.pnr_number, 
                c.cust_name, 
                r.f_code, 
                f.f_name,
                f.source_place, 
                f.destination_place,
                r.class_type,
                r.seat_number,
                r.travel_date,
                r.final_fare,
                r.status,
                f.departure_time,
                f.arrival_time
            FROM reservations r
            JOIN customer_details c ON r.pnr_number = c.pnr_number
            JOIN flight_information f ON r.f_code = f.f_code
            ORDER BY r.created_at DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                ReservationView reservation = new ReservationView(
                        rs.getString("pnr_number"),
                        rs.getString("cust_name"),
                        rs.getString("f_code") + " - " + rs.getString("f_name"),
                        rs.getString("source_place") + " → " + rs.getString("destination_place"),
                        rs.getString("class_type"),
                        rs.getString("seat_number"),
                        rs.getString("travel_date"),
                        "₹" + rs.getBigDecimal("final_fare"),
                        rs.getString("status"),
                        rs.getString("source_place"),
                        rs.getString("destination_place"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time")
                );
                reservations.add(reservation);
            }

            totalReservationsLabel.setText("Total Reservations: " + reservations.size());
            loadingIndicator.setVisible(false);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Error loading reservations: " + e.getMessage());
            loadingIndicator.setVisible(false);
        }
    }

    @FXML
    private void searchReservations() {
        String searchText = searchField.getText().trim().toLowerCase();
        String searchCategory = searchType.getValue();

        if (searchText.isEmpty()) {
            reservationsTable.setItems(reservations);
            totalReservationsLabel.setText("Total Reservations: " + reservations.size());
            return;
        }

        ObservableList<ReservationView> filteredReservations = FXCollections.observableArrayList();

        for (ReservationView reservation : reservations) {
            boolean matches = false;

            switch (searchCategory) {
                case "PNR":
                    matches = reservation.getPnr().toLowerCase().contains(searchText);
                    break;
                case "Customer Name":
                    matches = reservation.getCustomerName().toLowerCase().contains(searchText);
                    break;
                case "Flight":
                    matches = reservation.getFlight().toLowerCase().contains(searchText);
                    break;
                case "Status":
                    matches = reservation.getStatus().toLowerCase().contains(searchText);
                    break;
                case "All":
                default:
                    matches = reservation.getPnr().toLowerCase().contains(searchText) ||
                            reservation.getCustomerName().toLowerCase().contains(searchText) ||
                            reservation.getFlight().toLowerCase().contains(searchText) ||
                            reservation.getRoute().toLowerCase().contains(searchText) ||
                            reservation.getStatus().toLowerCase().contains(searchText);
                    break;
            }

            if (matches) {
                filteredReservations.add(reservation);
            }
        }

        reservationsTable.setItems(filteredReservations);
        totalReservationsLabel.setText("Found: " + filteredReservations.size() + " reservations");
    }

    @FXML
    private void refreshReservations() {
        loadReservations();
        searchField.clear();
        showAlert(Alert.AlertType.INFORMATION, "Refresh Complete", "Reservations list updated successfully!");
    }

    @FXML
    private void viewReservationDetails() {
        ReservationView selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showReservationDetails(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a reservation to view details");
        }
    }

    @FXML
    private void cancelReservation() {
        ReservationView selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {

            if ("Cancelled".equals(selected.getStatus())) {
                showAlert(Alert.AlertType.WARNING, "Already Cancelled",
                        "This reservation is already cancelled.");
                return;
            }

            // Calculate cancellation charges
            double cancellationCharge = calculateCancellationCharge(selected);
            double refundAmount = Double.parseDouble(selected.getFinalFare().replace("₹", "")) - cancellationCharge;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Cancellation");
            confirm.setHeaderText("Cancel Reservation: " + selected.getPnr());
            confirm.setContentText(
                    "Are you sure you want to cancel this reservation?\n\n" +
                            "Passenger: " + selected.getCustomerName() + "\n" +
                            "Flight: " + selected.getFlight() + "\n" +
                            "Travel Date: " + selected.getTravelDate() + "\n\n" +
                            "Refund Details:\n" +
                            "Base Fare: M" + selected.getFinalFare() + "\n" +
                            "Cancellation Charge: M" + String.format("%.2f", cancellationCharge) + "\n" +
                            "Refund Amount: M" + String.format("%.2f", refundAmount) + "\n\n" +
                            "This action cannot be undone."
            );

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    if (cancelReservationInDatabase(selected, cancellationCharge, refundAmount)) {
                        selected.setStatus("Cancelled");
                        reservationsTable.refresh();

                        showAlert(Alert.AlertType.INFORMATION, "Cancellation Successful",
                                "Reservation cancelled successfully!\n\n" +
                                        "PNR: " + selected.getPnr() + "\n" +
                                        "Refund Amount: M" + String.format("%.2f", refundAmount) + "\n" +
                                        "Refund will be processed within 7-10 working days.");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Cancellation Error",
                            "Error cancelling reservation: " + e.getMessage());
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a reservation to cancel");
        }
    }

    @FXML
    private void printTicket() {
        ReservationView selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            printTicket(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a reservation to print ticket");
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) reservationsTable.getScene().getWindow();
        stage.close();
    }

    private double calculateCancellationCharge(ReservationView reservation) {
        try {
            LocalDate travelDate = LocalDate.parse(reservation.getTravelDate());
            LocalDate currentDate = LocalDate.now();
            long daysUntilTravel = ChronoUnit.DAYS.between(currentDate, travelDate);

            double baseFare = Double.parseDouble(reservation.getFinalFare().replace("M", ""));

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
            return Double.parseDouble(reservation.getFinalFare().replace("M", "")) * 0.25;
        }
    }

    private boolean cancelReservationInDatabase(ReservationView reservation, double cancellationCharge, double refundAmount) throws SQLException {
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
                pstmt.setDouble(4, Double.parseDouble(reservation.getFinalFare().replace("₹", "")));
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

    private void printTicket(ReservationView reservation) {
        try {
            java.nio.file.Path ticketsDir = java.nio.file.Paths.get("tickets");
            if (!java.nio.file.Files.exists(ticketsDir)) {
                java.nio.file.Files.createDirectories(ticketsDir);
            }

            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "ticket_" + reservation.getPnr() + "_" + timestamp;

            String textContent = createTicketText(reservation);
            java.nio.file.Path textFile = ticketsDir.resolve(filename + ".txt");
            java.nio.file.Files.write(textFile, textContent.getBytes());

            String htmlContent = createTicketHTML(reservation);
            java.nio.file.Path htmlFile = ticketsDir.resolve(filename + ".html");
            java.nio.file.Files.write(htmlFile, htmlContent.getBytes());

            // Show success message with file locations
            String message = String.format("""
            Ticket printed successfully!
            
            Files created:
            Text: %s
            HTML: %s
            
            The HTML file can be printed or converted to PDF.
            """, textFile.toAbsolutePath(), htmlFile.toAbsolutePath());

            showTicketPreview(reservation, message);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Print Error",
                    "Error printing ticket: " + e.getMessage());
        }
    }

    private String createTicketText(ReservationView reservation) {
        return String.format("""
        ╔══════════════════════════════════════════════════════════════════╗
        ║                      AIRLINE E-TICKET                           ║
        ╠══════════════════════════════════════════════════════════════════╣
        ║  PNR NUMBER: %-45s ║
        ║  PASSENGER: %-47s ║
        ║  FLIGHT: %-49s ║
        ║  ROUTE: %-50s ║
        ║  DATE: %-12s DEPARTURE: %-8s ARRIVAL: %-8s ║
        ║  CLASS: %-10s SEAT: %-12s FARE: %-12s ║
        ║  STATUS: %-47s ║
        ╠══════════════════════════════════════════════════════════════════╣
        ║  BOARDING TIME: 45 minutes before departure                     ║
        ║  GATE: To be announced                                          ║
        ║  TERMINAL: Main Terminal                                        ║
        ╠══════════════════════════════════════════════════════════════════╣
        ║               THANK YOU FOR CHOOSING OUR AIRLINE!               ║
        ║                 HAVE A SAFE AND PLEASANT JOURNEY!               ║
        ╚══════════════════════════════════════════════════════════════════╝
        
        Generated on: %s
        """,
                reservation.getPnr(),
                reservation.getCustomerName(),
                reservation.getFlight(),
                reservation.getRoute(),
                reservation.getTravelDate(),
                reservation.getDepartureTime(),
                reservation.getArrivalTime(),
                reservation.getSeatClass(),
                reservation.getSeatNumber(),
                reservation.getFinalFare(),
                reservation.getStatus(),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private String createTicketHTML(ReservationView reservation) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <title>E-Ticket - %s</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                .ticket { border: 2px solid #333; padding: 20px; max-width: 600px; margin: 0 auto; }
                .header { text-align: center; background: #2c3e50; color: white; padding: 10px; margin: -20px -20px 20px -20px; }
                .section { margin: 15px 0; padding: 10px; border-bottom: 1px solid #ddd; }
                .label { font-weight: bold; color: #2c3e50; }
                .value { margin-left: 10px; }
                .footer { text-align: center; margin-top: 20px; padding: 10px; background: #ecf0f1; }
                .barcode { text-align: center; margin: 20px 0; font-family: 'Libre Barcode 128', monospace; font-size: 24px; }
            </style>
        </head>
        <body>
            <div class="ticket">
                <div class="header">
                    <h1>ELECTRONIC TICKET</h1>
                    <h2>BOARDING PASS</h2>
                </div>
                
                <div class="section">
                    <div><span class="label">PNR:</span><span class="value">%s</span></div>
                    <div><span class="label">Passenger:</span><span class="value">%s</span></div>
                </div>
                
                <div class="section">
                    <div><span class="label">Flight:</span><span class="value">%s</span></div>
                    <div><span class="label">Route:</span><span class="value">%s</span></div>
                    <div><span class="label">Date:</span><span class="value">%s</span></div>
                    <div><span class="label">Time:</span><span class="value">%s - %s</span></div>
                </div>
                
                <div class="section">
                    <div><span class="label">Class:</span><span class="value">%s</span></div>
                    <div><span class="label">Seat:</span><span class="value">%s</span></div>
                    <div><span class="label">Fare:</span><span class="value">%s</span></div>
                    <div><span class="label">Status:</span><span class="value">%s</span></div>
                </div>
                
                <div class="barcode">
                    *%s*  <!-- Simulated barcode -->
                </div>
                
                <div class="footer">
                    <p><strong>Boarding Time:</strong> 45 minutes before departure</p>
                    <p><strong>Gate:</strong> To be announced | <strong>Terminal:</strong> Main</p>
                    <p><em>Thank you for choosing our airline! Have a safe journey!</em></p>
                    <p>Generated on: %s</p>
                </div>
            </div>
        </body>
        </html>
        """,
                reservation.getPnr(),
                reservation.getPnr(),
                reservation.getCustomerName(),
                reservation.getFlight(),
                reservation.getRoute(),
                reservation.getTravelDate(),
                reservation.getDepartureTime(),
                reservation.getArrivalTime(),
                reservation.getSeatClass(),
                reservation.getSeatNumber(),
                reservation.getFinalFare(),
                reservation.getStatus(),
                reservation.getPnr(),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private void showTicketPreview(ReservationView reservation, String fileMessage) {
        TextArea ticketArea = new TextArea(createTicketText(reservation));
        ticketArea.setEditable(false);
        ticketArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
        ticketArea.setPrefRowCount(25);

        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Ticket Preview:"),
                ticketArea,
                new Label(fileMessage)
        );

        Alert ticketAlert = new Alert(Alert.AlertType.INFORMATION);
        ticketAlert.setTitle("E-Ticket - " + reservation.getPnr());
        ticketAlert.setHeaderText("Ticket Printed Successfully");
        ticketAlert.getDialogPane().setContent(content);
        ticketAlert.getDialogPane().setPrefSize(700, 600);

        ButtonType openFolderButton = new ButtonType("Open Folder", ButtonBar.ButtonData.OTHER);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        ticketAlert.getButtonTypes().setAll(openFolderButton, closeButton);

        Optional<ButtonType> result = ticketAlert.showAndWait();
        if (result.isPresent() && result.get() == openFolderButton) {
            try {
                java.awt.Desktop.getDesktop().open(new java.io.File("tickets"));
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Folder Error", "Cannot open folder: " + e.getMessage());
            }
        }
    }


    private void showReservationDetails(ReservationView reservation) {
        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Reservation Details");
        details.setHeaderText("Reservation: " + reservation.getPnr());
        details.setContentText(
                "PNR: " + reservation.getPnr() + "\n" +
                        "Passenger: " + reservation.getCustomerName() + "\n" +
                        "Flight: " + reservation.getFlight() + "\n" +
                        "Route: " + reservation.getRoute() + "\n" +
                        "Class: " + reservation.getSeatClass() + "\n" +
                        "Seat: " + reservation.getSeatNumber() + "\n" +
                        "Travel Date: " + reservation.getTravelDate() + "\n" +
                        "Departure: " + reservation.getDepartureTime() + "\n" +
                        "Arrival: " + reservation.getArrivalTime() + "\n" +
                        "Fare: " + reservation.getFinalFare() + "\n" +
                        "Status: " + reservation.getStatus()
        );
        details.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class ReservationView {
        private final String pnr;
        private final String customerName;
        private final String flight;
        private final String route;
        private final String seatClass;
        private final String seatNumber;
        private final String travelDate;
        private final String finalFare;
        private String status;
        private final String sourcePlace;
        private final String destinationPlace;
        private final String departureTime;
        private final String arrivalTime;

        public ReservationView(String pnr, String customerName, String flight, String route,
                               String seatClass, String seatNumber, String travelDate,
                               String finalFare, String status, String sourcePlace,
                               String destinationPlace, String departureTime, String arrivalTime) {
            this.pnr = pnr;
            this.customerName = customerName;
            this.flight = flight;
            this.route = route;
            this.seatClass = seatClass;
            this.seatNumber = seatNumber;
            this.travelDate = travelDate;
            this.finalFare = finalFare;
            this.status = status;
            this.sourcePlace = sourcePlace;
            this.destinationPlace = destinationPlace;
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
        }

        public String getPnr() { return pnr; }
        public String getCustomerName() { return customerName; }
        public String getFlight() { return flight; }
        public String getRoute() { return route; }
        public String getSeatClass() { return seatClass; }
        public String getSeatNumber() { return seatNumber; }
        public String getTravelDate() { return travelDate; }
        public String getFinalFare() { return finalFare; }
        public String getStatus() { return status; }
        public String getSourcePlace() { return sourcePlace; }
        public String getDestinationPlace() { return destinationPlace; }
        public String getDepartureTime() { return departureTime; }
        public String getArrivalTime() { return arrivalTime; }

        public void setStatus(String status) { this.status = status; }
    }
}