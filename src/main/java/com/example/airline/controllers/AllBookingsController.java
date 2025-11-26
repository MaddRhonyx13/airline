package com.example.airline.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AllBookingsController implements Initializable {

    @FXML private Label summaryLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Pagination bookingsPagination;
    @FXML private VBox bookingsContainer;
    @FXML private Label totalBookingsLabel;

    private String[] passengerNames = {"Tumisang Madd"};
    private String[] flights = {"AI101"};
    private String[] routes = {"Maseru-Durban", "Maseru-Johannesburg"};
    private String[] statuses = {"Confirmed", "Waiting", "Cancelled"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPagination();
        refreshBookings();
    }

    private void setupPagination() {
        bookingsPagination.setPageFactory(this::createBookingsPage);
    }

    private VBox createBookingsPage(int pageIndex) {
        VBox page = new VBox(10);

        for (int i = 0; i < 5; i++) {
            int bookingNumber = pageIndex * 5 + i;
            page.getChildren().add(createBookingCard(bookingNumber));
        }

        return page;
    }

    private HBox createBookingCard(int bookingNumber) {
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        card.setPrefWidth(900);

        VBox passengerInfo = new VBox(5);
        passengerInfo.setPrefWidth(200);

        String pnr = "PNR" + (10000 + bookingNumber);
        String passenger = passengerNames[bookingNumber % passengerNames.length];

        Label pnrLabel = new Label(pnr);
        pnrLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label passengerLabel = new Label(passenger);
        passengerLabel.setStyle("-fx-text-fill: #6c757d;");

        passengerInfo.getChildren().addAll(pnrLabel, passengerLabel);

        VBox flightInfo = new VBox(5);
        flightInfo.setPrefWidth(250);

        String flight = flights[bookingNumber % flights.length];
        String route = routes[bookingNumber % routes.length];

        Label flightLabel = new Label(flight + " - " + route);
        flightLabel.setStyle("-fx-font-weight: bold;");

        Label dateLabel = new Label("2024-02-" + (15 + bookingNumber % 10));

        flightInfo.getChildren().addAll(flightLabel, dateLabel);

        VBox classInfo = new VBox(5);
        classInfo.setPrefWidth(150);

        String seatClass = bookingNumber % 3 == 0 ? "Business" : "Economy";
        double fare = seatClass.equals("Business") ? 8000 : 4500;

        Label classLabel = new Label(seatClass);
        Label fareLabel = new Label("M" + fare);

        classInfo.getChildren().addAll(classLabel, fareLabel);

        // Status
        VBox statusInfo = new VBox(5);
        statusInfo.setPrefWidth(100);

        String status = statuses[bookingNumber % statuses.length];
        Label statusLabel = new Label(status);

        if ("Confirmed".equals(status)) {
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        } else if ("Waiting".equals(status)) {
            statusLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        }

        statusInfo.getChildren().addAll(new Label("Status:"), statusLabel);

        HBox.setHgrow(passengerInfo, Priority.ALWAYS);
        HBox.setHgrow(flightInfo, Priority.ALWAYS);
        HBox.setHgrow(classInfo, Priority.ALWAYS);
        HBox.setHgrow(statusInfo, Priority.ALWAYS);

        card.getChildren().addAll(passengerInfo, flightInfo, classInfo, statusInfo);

        return card;
    }

    @FXML
    private void refreshBookings() {
        loadingIndicator.setProgress(-1);

        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1)).play();

        bookingsPagination.setPageCount(5);
        totalBookingsLabel.setText("25");
        summaryLabel.setText("Showing 25 bookings across 5 pages");

        loadingIndicator.setProgress(1.0);

        showAlert(Alert.AlertType.INFORMATION, "Refreshed", "Bookings data refreshed successfully!");
    }

    @FXML
    private void printReport() {
        showAlert(Alert.AlertType.INFORMATION, "Print Report",
                "This would generate a printable report of all bookings.\n" +
                        "In a real application, this would create a PDF or printable view.");
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) summaryLabel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
