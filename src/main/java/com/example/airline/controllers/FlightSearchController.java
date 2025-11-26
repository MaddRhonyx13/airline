package com.example.airline.controllers;

import com.example.airline.DatabaseConnection;
import com.example.airline.Flight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class FlightSearchController implements Initializable {

    @FXML private ComboBox<String> sourceCombo;
    @FXML private ComboBox<String> destinationCombo;
    @FXML private DatePicker travelDatePicker;
    @FXML private ComboBox<String> classCombo;
    @FXML private Button searchButton;
    @FXML private ProgressIndicator searchProgress;
    @FXML private VBox flightResultsContainer;
    @FXML private Label resultsLabel;
    @FXML private Pagination pagination;

    private ObservableList<Flight> searchResults = FXCollections.observableArrayList();
    private static final int ITEMS_PER_PAGE = 5;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeComboBoxes();
        setupPagination();

        travelDatePicker.setValue(LocalDate.now().plusDays(1));
    }

    private void initializeComboBoxes() {
        sourceCombo.getItems().addAll("Maseru", "Johannesburg", "Durban", "Capetown", "Bloemfontein", "eSwatini");
        destinationCombo.getItems().addAll("Maseru", "Johannesburg", "Durban", "Capetown", "Bloemfontein", "eSwatini");
        classCombo.getItems().addAll("Economy", "Business");

        sourceCombo.setValue("Maseru");
        destinationCombo.setValue("Maseru");
        classCombo.setValue("Economy");
    }

    private void setupPagination() {
        pagination.setPageFactory(this::createPage);
        pagination.setVisible(false);
    }

    private VBox createPage(int pageIndex) {
        VBox page = new VBox(10);
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, searchResults.size());

        for (int i = fromIndex; i < toIndex; i++) {
            Flight flight = searchResults.get(i);
            page.getChildren().add(createFlightCard(flight));
        }

        return page;
    }

    private HBox createFlightCard(Flight flight) {
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 20; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPrefWidth(800);

        VBox flightInfo = new VBox(8);
        flightInfo.setPrefWidth(250);

        Label airlineLabel = new Label(flight.getFlightCode() + " - " + flight.getFlightName());
        airlineLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

        Label routeLabel = new Label(flight.getSourcePlace() + " → " + flight.getDestinationPlace());
        routeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        Label timeLabel = new Label(flight.getDepartureTime() + " - " + flight.getArrivalTime());
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #34495e;");

        flightInfo.getChildren().addAll(airlineLabel, routeLabel, timeLabel);

        VBox classInfo = new VBox(8);
        classInfo.setPrefWidth(150);

        String selectedClass = classCombo.getValue();
        int availableSeats = selectedClass.equals("Economy") ?
                flight.getAvailableEconomySeats() : flight.getAvailableBusinessSeats();

        Label classLabel = new Label(selectedClass + " Class");
        classLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label seatsLabel = new Label(availableSeats + " seats available");
        seatsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (availableSeats > 10 ? "#27ae60" : availableSeats > 0 ? "#f39c12" : "#e74c3c") + ";");

        classInfo.getChildren().addAll(classLabel, seatsLabel);

        VBox fareInfo = new VBox(8);
        fareInfo.setPrefWidth(150);

        double fare = getFlightFare(flight, selectedClass);
        Label fareLabel = new Label("M" + String.format("%.0f", fare));
        fareLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #e74c3c;");

        Label perPersonLabel = new Label("per person");
        perPersonLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        fareInfo.getChildren().addAll(fareLabel, perPersonLabel);

        Button selectButton = new Button("Select Flight");
        selectButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        selectButton.setOnAction(e -> selectFlight(flight, fare));

        HBox.setHgrow(flightInfo, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(classInfo, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(fareInfo, javafx.scene.layout.Priority.ALWAYS);

        card.getChildren().addAll(flightInfo, classInfo, fareInfo, selectButton);

        return card;
    }

    @FXML
    private void handleSearch() {
        String source = sourceCombo.getValue();
        String destination = destinationCombo.getValue();
        LocalDate travelDate = travelDatePicker.getValue();
        String travelClass = classCombo.getValue();

        if (source == null || destination == null) {
            showAlert(Alert.AlertType.WARNING, "Input Required", "Please select both source and destination cities.");
            return;
        }

        if (source.equals(destination)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Route", "Source and destination cannot be the same.");
            return;
        }

        if (travelDate == null) {
            showAlert(Alert.AlertType.WARNING, "Date Required", "Please select a travel date.");
            return;
        }

        if (travelDate.isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Invalid Date", "Travel date cannot be in the past.");
            return;
        }

        searchProgress.setVisible(true);
        searchResults.clear();
        flightResultsContainer.getChildren().clear();

        new Thread(() -> {
            try {
                searchFlights(source, destination, travelClass);

                javafx.application.Platform.runLater(() -> {
                    searchProgress.setVisible(false);
                    displaySearchResults();
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    searchProgress.setVisible(false);
                    showAlert(Alert.AlertType.ERROR, "Search Error", "Error searching flights: " + e.getMessage());
                });
            }
        }).start();
    }

    private void searchFlights(String source, String destination, String travelClass) {
        String sql = """
            SELECT f.f_code, f.f_name, f.route, f.source_place, f.destination_place,
                   f.departure_time, f.arrival_time, f.t_eco_seatno, f.t_exe_seatno,
                   f.eco_seats_booked, f.exe_seats_booked, f.is_active
            FROM flight_information f
            WHERE f.source_place = ? AND f.destination_place = ? 
            AND f.is_active = 1
            ORDER BY f.departure_time
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, source);
            pstmt.setString(2, destination);

            ResultSet rs = pstmt.executeQuery();

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
                flight.setActive(rs.getBoolean("is_active"));

                int availableSeats = travelClass.equals("Economy") ?
                        flight.getAvailableEconomySeats() : flight.getAvailableBusinessSeats();

                if (availableSeats > 0) {
                    searchResults.add(flight);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error during flight search: " + e.getMessage(), e);
        }
    }

    private void displaySearchResults() {
        flightResultsContainer.getChildren().clear();

        if (searchResults.isEmpty()) {
            Label noResultsLabel = new Label("No flights found for the selected route and criteria.");
            noResultsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
            flightResultsContainer.getChildren().add(noResultsLabel);
            resultsLabel.setText("No flights found");
            pagination.setVisible(false);
        } else {
            resultsLabel.setText("Found " + searchResults.size() + " flights");

            int pageCount = (int) Math.ceil((double) searchResults.size() / ITEMS_PER_PAGE);
            pagination.setPageCount(pageCount);
            pagination.setVisible(true);

            showAlert(Alert.AlertType.INFORMATION, "Search Complete",
                    "Found " + searchResults.size() + " flights from " +
                            sourceCombo.getValue() + " to " + destinationCombo.getValue());
        }
    }

    private double getFlightFare(Flight flight, String travelClass) {
        String sql = "SELECT base_fare FROM fare WHERE f_code = ? AND class_type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, flight.getFlightCode());
            pstmt.setString(2, travelClass);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("base_fare");
            }
        } catch (SQLException e) {
            System.err.println("Error getting fare for flight " + flight.getFlightCode() + ": " + e.getMessage());
        }

        return travelClass.equals("Economy") ? 1500.0 : 2500.0;
    }

    private void selectFlight(Flight flight, double fare) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Flight Selection");
        confirm.setHeaderText("Flight Selected: " + flight.getFlightCode());
        confirm.setContentText(
                "✈️ " + flight.getFlightName() + "\n" +
                        "📍 " + flight.getSourcePlace() + " → " + flight.getDestinationPlace() + "\n" +
                        "🕐 " + flight.getDepartureTime() + " - " + flight.getArrivalTime() + "\n" +
                        "💺 " + classCombo.getValue() + " Class\n" +
                        "💰 Fare: M" + String.format("%.0f", fare) + "\n\n" +
                        "Proceed to reservation form?"
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            openReservationForm(flight, fare);
        }
    }

    private void openReservationForm(Flight selectedFlight, double fare) {
        try {
            Stage searchStage = (Stage) sourceCombo.getScene().getWindow();
            searchStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CustomerForm.fxml"));
            Parent root = loader.load();

            CustomerController customerController = loader.getController();
            customerController.setSelectedFlightFromSearch(
                    selectedFlight,
                    sourceCombo.getValue(),
                    destinationCombo.getValue(),
                    classCombo.getValue(),
                    travelDatePicker.getValue(),
                    fare
            );

            Stage reservationStage = new Stage();
            reservationStage.setTitle("New Reservation - " + selectedFlight.getFlightCode());
            reservationStage.setScene(new Scene(root, 1000, 800));
            reservationStage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open reservation form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClear() {
        sourceCombo.setValue("Maseru");
        destinationCombo.setValue("Maseru");
        travelDatePicker.setValue(LocalDate.now().plusDays(1));
        classCombo.setValue("Economy");
        searchResults.clear();
        flightResultsContainer.getChildren().clear();
        pagination.setVisible(false);
        resultsLabel.setText("Enter search criteria");
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) sourceCombo.getScene().getWindow();
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
