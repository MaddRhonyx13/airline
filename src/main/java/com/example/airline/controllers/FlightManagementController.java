package com.example.airline.controllers;

import com.example.airline.DatabaseConnection;
import com.example.airline.Flight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;

public class FlightManagementController implements Initializable {

    @FXML private TextField flightCodeField;
    @FXML private TextField flightNameField;
    @FXML private TextField routeField;
    @FXML private ComboBox<String> sourcePlaceCombo;
    @FXML private ComboBox<String> destinationPlaceCombo;
    @FXML private TextField departureTimeField;
    @FXML private TextField arrivalTimeField;
    @FXML private TextField economySeatsField;
    @FXML private TextField businessSeatsField;
    @FXML private Label recordInfoLabel;
    @FXML private Label statusLabel;

    @FXML private TableView<Flight> flightsTable;
    @FXML private TableColumn<Flight, String> colFlightCode;
    @FXML private TableColumn<Flight, String> colFlightName;
    @FXML private TableColumn<Flight, String> colRoute;
    @FXML private TableColumn<Flight, String> colDeparture;
    @FXML private TableColumn<Flight, String> colArrival;
    @FXML private TableColumn<Flight, Integer> colEcoSeats;
    @FXML private TableColumn<Flight, Integer> colBusSeats;
    @FXML private TableColumn<Flight, Integer> colEcoBooked;
    @FXML private TableColumn<Flight, Integer> colBusBooked;
    @FXML private TableColumn<Flight, String> colStatus;
    @FXML private TableColumn<Flight, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ProgressIndicator loadingIndicator;

    private ObservableList<Flight> flights = FXCollections.observableArrayList();
    private ObservableList<Flight> filteredFlights = FXCollections.observableArrayList();
    private int currentRecordIndex = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeComboBoxes();
        setupTableColumns();
        setupTableSelectionListener();
        loadFlightsFromDatabase();
        clearForm();
        updateNavigationInfo();
    }

    private void initializeComboBoxes() {
        sourcePlaceCombo.getItems().addAll("Maseru", "Johannesburg", "Durban", "Capetown", "Bloemfontein", "eSwatini");
        destinationPlaceCombo.getItems().addAll("Maseru", "Johannesburg", "Durban", "Capetown", "Bloemfontein", "eSwatini");

        sourcePlaceCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateRoute());
        destinationPlaceCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateRoute());
    }

    private void updateRoute() {
        String source = sourcePlaceCombo.getValue();
        String destination = destinationPlaceCombo.getValue();
        if (source != null && destination != null && !source.equals(destination)) {
            routeField.setText(source + " - " + destination);
        } else {
            routeField.clear();
        }
    }

    private void setupTableColumns() {
        colFlightCode.setCellValueFactory(new PropertyValueFactory<>("flightCode"));
        colFlightName.setCellValueFactory(new PropertyValueFactory<>("flightName"));
        colRoute.setCellValueFactory(new PropertyValueFactory<>("route"));
        colDeparture.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        colArrival.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        colEcoSeats.setCellValueFactory(new PropertyValueFactory<>("economySeats"));
        colBusSeats.setCellValueFactory(new PropertyValueFactory<>("businessSeats"));
        colEcoBooked.setCellValueFactory(new PropertyValueFactory<>("ecoSeatsBooked"));
        colBusBooked.setCellValueFactory(new PropertyValueFactory<>("exeSeatsBooked"));

        colStatus.setCellValueFactory(cellData -> {
            Flight flight = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(flight.isActive() ? "Active" : "Inactive");
        });

        colStatus.setCellFactory(column -> new TableCell<Flight, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Active".equals(status)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colActions.setCellFactory(new Callback<TableColumn<Flight, Void>, TableCell<Flight, Void>>() {
            @Override
            public TableCell<Flight, Void> call(final TableColumn<Flight, Void> param) {
                return new TableCell<Flight, Void>() {
                    private final Button editBtn = new Button("✏️");
                    private final Button deleteBtn = new Button("🗑️");
                    private final HBox pane = new HBox(5, editBtn, deleteBtn);

                    {
                        editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px;");
                        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px;");

                        editBtn.setOnAction(event -> {
                            Flight flight = getTableView().getItems().get(getIndex());
                            editFlight(flight);
                        });

                        deleteBtn.setOnAction(event -> {
                            Flight flight = getTableView().getItems().get(getIndex());
                            deleteFlight(flight);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
            }
        });

        flightsTable.setItems(filteredFlights);
    }

    private void setupTableSelectionListener() {
        flightsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        displayFlightDetails(newSelection);
                    }
                });
    }

    private void loadFlightsFromDatabase() {
        loadingIndicator.setVisible(true);
        flights.clear();

        String sql = "SELECT flight_id, f_code, f_name, route, source_place, destination_place, " +
                "departure_time, arrival_time, t_eco_seatno, t_exe_seatno, " +
                "eco_seats_booked, exe_seats_booked, is_active " +
                "FROM flight_information ORDER BY f_code";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

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
                flight.setFlightId(rs.getInt("flight_id"));
                flight.setActive(rs.getBoolean("is_active"));
                flights.add(flight);
            }

            filteredFlights.setAll(flights);
            updateStatusLabel("Loaded " + flights.size() + " flights");
            loadingIndicator.setVisible(false);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Error loading flights: " + e.getMessage());
            loadingIndicator.setVisible(false);
        }
    }


    @FXML
    private void handleNew() {
        clearForm();
        currentRecordIndex = -1;
        updateNavigationInfo();
        updateStatusLabel("Ready to add new flight");
        flightsTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAdd() {
        if (validateForm()) {
            try {
                Flight newFlight = createFlightFromForm();
                if (saveFlightToDatabase(newFlight)) {
                    flights.add(newFlight);
                    filteredFlights.setAll(flights);
                    clearForm();
                    updateStatusLabel("Flight added successfully: " + newFlight.getFlightCode());
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Flight added successfully!");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add flight: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdate() {
        Flight selectedFlight = flightsTable.getSelectionModel().getSelectedItem();
        if (selectedFlight != null) {
            if (validateForm()) {
                try {
                    Flight updatedFlight = createFlightFromForm();
                    updatedFlight.setFlightId(selectedFlight.getFlightId());
                    updatedFlight.setEcoSeatsBooked(selectedFlight.getEcoSeatsBooked());
                    updatedFlight.setExeSeatsBooked(selectedFlight.getExeSeatsBooked());
                    updatedFlight.setActive(selectedFlight.isActive());

                    if (updateFlightInDatabase(updatedFlight)) {
                        int index = flights.indexOf(selectedFlight);
                        flights.set(index, updatedFlight);
                        filteredFlights.setAll(flights);
                        updateStatusLabel("Flight updated successfully: " + updatedFlight.getFlightCode());
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Flight updated successfully!");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update flight: " + e.getMessage());
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a flight to update");
        }
    }

    @FXML
    private void handleDelete() {
        Flight selectedFlight = flightsTable.getSelectionModel().getSelectedItem();
        if (selectedFlight != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete Flight");
            confirm.setContentText("Are you sure you want to delete flight " + selectedFlight.getFlightCode() + "?\nThis action cannot be undone.");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    if (deleteFlightFromDatabase(selectedFlight.getFlightId())) {
                        flights.remove(selectedFlight);
                        filteredFlights.setAll(flights);
                        clearForm();
                        updateNavigationInfo();
                        updateStatusLabel("Flight deleted successfully");
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Flight deleted successfully!");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete flight: " + e.getMessage());
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a flight to delete");
        }
    }

    @FXML
    private void handleSave() {
        if (flightsTable.getSelectionModel().getSelectedItem() == null) {
            handleAdd();
        } else {
            handleUpdate();
        }
    }


    @FXML
    private void handleFirst() {
        if (!flights.isEmpty()) {
            Flight firstFlight = flights.get(0);
            flightsTable.getSelectionModel().select(firstFlight);
            displayFlightDetails(firstFlight);
        }
    }

    @FXML
    private void handlePrevious() {
        Flight current = flightsTable.getSelectionModel().getSelectedItem();
        if (current != null) {
            int currentIndex = flights.indexOf(current);
            if (currentIndex > 0) {
                Flight previousFlight = flights.get(currentIndex - 1);
                flightsTable.getSelectionModel().select(previousFlight);
                displayFlightDetails(previousFlight);
            }
        } else if (!flights.isEmpty()) {
            flightsTable.getSelectionModel().select(0);
        }
    }

    @FXML
    private void handleNext() {
        Flight current = flightsTable.getSelectionModel().getSelectedItem();
        if (current != null) {
            int currentIndex = flights.indexOf(current);
            if (currentIndex < flights.size() - 1) {
                Flight nextFlight = flights.get(currentIndex + 1);
                flightsTable.getSelectionModel().select(nextFlight);
                displayFlightDetails(nextFlight);
            }
        } else if (!flights.isEmpty()) {
            flightsTable.getSelectionModel().select(0);
        }
    }

    @FXML
    private void handleLast() {
        if (!flights.isEmpty()) {
            Flight lastFlight = flights.get(flights.size() - 1);
            flightsTable.getSelectionModel().select(lastFlight);
            displayFlightDetails(lastFlight);
        }
    }


    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            filteredFlights.setAll(flights);
        } else {
            ObservableList<Flight> filtered = FXCollections.observableArrayList();
            for (Flight flight : flights) {
                if (flight.getFlightCode().toLowerCase().contains(searchText) ||
                        flight.getFlightName().toLowerCase().contains(searchText) ||
                        flight.getRoute().toLowerCase().contains(searchText) ||
                        flight.getSourcePlace().toLowerCase().contains(searchText) ||
                        flight.getDestinationPlace().toLowerCase().contains(searchText)) {
                    filtered.add(flight);
                }
            }
            filteredFlights.setAll(filtered);
        }
        updateStatusLabel("Found " + filteredFlights.size() + " flights");
    }

    @FXML
    private void handleRefresh() {
        loadFlightsFromDatabase();
        searchField.clear();
        updateStatusLabel("Flight list refreshed");
    }


    private boolean saveFlightToDatabase(Flight flight) throws SQLException {
        String sql = """
            INSERT INTO flight_information 
            (f_code, f_name, route, source_place, destination_place, departure_time, arrival_time, 
             t_eco_seatno, t_exe_seatno, eco_seats_booked, exe_seats_booked, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setFlightParameters(pstmt, flight);
            pstmt.setBoolean(12, true);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                addFareForFlight(flight);
                return true;
            }
        }
        return false;
    }

    private boolean updateFlightInDatabase(Flight flight) throws SQLException {
        String sql = """
            UPDATE flight_information 
            SET f_name = ?, route = ?, source_place = ?, destination_place = ?, 
                departure_time = ?, arrival_time = ?, t_eco_seatno = ?, t_exe_seatno = ?,
                eco_seats_booked = ?, exe_seats_booked = ?, is_active = ?
            WHERE flight_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, flight.getFlightName());
            pstmt.setString(2, flight.getRoute());
            pstmt.setString(3, flight.getSourcePlace());
            pstmt.setString(4, flight.getDestinationPlace());
            pstmt.setString(5, flight.getDepartureTime());
            pstmt.setString(6, flight.getArrivalTime());
            pstmt.setInt(7, flight.getEconomySeats());
            pstmt.setInt(8, flight.getBusinessSeats());
            pstmt.setInt(9, flight.getEcoSeatsBooked());
            pstmt.setInt(10, flight.getExeSeatsBooked());
            pstmt.setBoolean(11, flight.isActive());
            pstmt.setInt(12, flight.getFlightId());

            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean deleteFlightFromDatabase(int flightId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM reservations WHERE f_code = (SELECT f_code FROM flight_information WHERE flight_id = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, flightId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert(Alert.AlertType.ERROR, "Cannot Delete",
                        "Cannot delete flight because there are existing reservations. " +
                                "You can deactivate the flight instead.");
                return false;
            }
        }

        String sql = "DELETE FROM flight_information WHERE flight_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, flightId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private void addFareForFlight(Flight flight) throws SQLException {
        String[] fareSQL = {
                "INSERT OR IGNORE INTO fare (route_code, f_code, class_type, base_fare) VALUES (?, ?, 'Economy', ?)",
                "INSERT OR IGNORE INTO fare (route_code, f_code, class_type, base_fare) VALUES (?, ?, 'Business', ?)"
        };

        double ecoFare = calculateDefaultFare(flight, "Economy");
        double busFare = calculateDefaultFare(flight, "Business");

        try (Connection conn = DatabaseConnection.getConnection()) {
            for (String sql : fareSQL) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, flight.getFlightCode() + "-ECO");
                    pstmt.setString(2, flight.getFlightCode());
                    pstmt.setDouble(3, sql.contains("Economy") ? ecoFare : busFare);
                    pstmt.executeUpdate();
                }
            }
        }
    }

    private double calculateDefaultFare(Flight flight, String classType) {
        double baseFare = 3000.0;

        switch (flight.getDestinationPlace()) {
            case "Maseru": baseFare += 1000; break;
            case "Johannesburg": baseFare += 1500; break;
            case "Durban": baseFare += 2000; break;
            case "Capetown": baseFare += 2500; break;
            case "Bloemfontein": baseFare += 1200; break;
        }

        if ("Business".equals(classType)) {
            baseFare *= 1.8;
        }

        return baseFare;
    }


    private Flight createFlightFromForm() {
        return new Flight(
                flightCodeField.getText().toUpperCase(),
                flightNameField.getText(),
                routeField.getText(),
                sourcePlaceCombo.getValue(),
                destinationPlaceCombo.getValue(),
                departureTimeField.getText(),
                arrivalTimeField.getText(),
                Integer.parseInt(economySeatsField.getText()),
                Integer.parseInt(businessSeatsField.getText()),
                0, 0
        );
    }

    private void displayFlightDetails(Flight flight) {
        flightCodeField.setText(flight.getFlightCode());
        flightNameField.setText(flight.getFlightName());
        routeField.setText(flight.getRoute());
        sourcePlaceCombo.setValue(flight.getSourcePlace());
        destinationPlaceCombo.setValue(flight.getDestinationPlace());
        departureTimeField.setText(flight.getDepartureTime());
        arrivalTimeField.setText(flight.getArrivalTime());
        economySeatsField.setText(String.valueOf(flight.getEconomySeats()));
        businessSeatsField.setText(String.valueOf(flight.getBusinessSeats()));

        currentRecordIndex = flights.indexOf(flight);
        updateNavigationInfo();
    }

    private void editFlight(Flight flight) {
        displayFlightDetails(flight);
        flightsTable.getSelectionModel().select(flight);
        updateStatusLabel("Editing flight: " + flight.getFlightCode());
    }

    private void deleteFlight(Flight flight) {
        flightsTable.getSelectionModel().select(flight);
        handleDelete();
    }

    private void setFlightParameters(PreparedStatement pstmt, Flight flight) throws SQLException {
        pstmt.setString(1, flight.getFlightCode());
        pstmt.setString(2, flight.getFlightName());
        pstmt.setString(3, flight.getRoute());
        pstmt.setString(4, flight.getSourcePlace());
        pstmt.setString(5, flight.getDestinationPlace());
        pstmt.setString(6, flight.getDepartureTime());
        pstmt.setString(7, flight.getArrivalTime());
        pstmt.setInt(8, flight.getEconomySeats());
        pstmt.setInt(9, flight.getBusinessSeats());
        pstmt.setInt(10, flight.getEcoSeatsBooked());
        pstmt.setInt(11, flight.getExeSeatsBooked());
    }

    private void clearForm() {
        flightCodeField.clear();
        flightNameField.clear();
        routeField.clear();
        sourcePlaceCombo.setValue(null);
        destinationPlaceCombo.setValue(null);
        departureTimeField.clear();
        arrivalTimeField.clear();
        economySeatsField.clear();
        businessSeatsField.clear();
        currentRecordIndex = -1;
        updateNavigationInfo();
    }

    private void updateNavigationInfo() {
        if (flights.isEmpty() || currentRecordIndex == -1) {
            recordInfoLabel.setText("New Flight");
        } else {
            recordInfoLabel.setText("Record " + (currentRecordIndex + 1) + " of " + flights.size());
        }
    }

    private void updateStatusLabel(String message) {
        statusLabel.setText("Status: " + message);
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (flightCodeField.getText().isEmpty()) errors.append("• Flight Code\n");
        if (flightNameField.getText().isEmpty()) errors.append("• Flight Name\n");
        if (routeField.getText().isEmpty()) errors.append("• Route\n");
        if (sourcePlaceCombo.getValue() == null) errors.append("• Source Place\n");
        if (destinationPlaceCombo.getValue() == null) errors.append("• Destination Place\n");
        if (departureTimeField.getText().isEmpty()) errors.append("• Departure Time\n");
        if (arrivalTimeField.getText().isEmpty()) errors.append("• Arrival Time\n");
        if (economySeatsField.getText().isEmpty()) errors.append("• Economy Seats\n");
        if (businessSeatsField.getText().isEmpty()) errors.append("• Business Seats\n");

        if (!departureTimeField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            errors.append("• Departure Time (HH:MM format)\n");
        }
        if (!arrivalTimeField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            errors.append("• Arrival Time (HH:MM format)\n");
        }

        try {
            int ecoSeats = Integer.parseInt(economySeatsField.getText());
            if (ecoSeats <= 0) errors.append("• Economy Seats (must be positive)\n");
        } catch (NumberFormatException e) {
            errors.append("• Economy Seats (must be a number)\n");
        }

        try {
            int busSeats = Integer.parseInt(businessSeatsField.getText());
            if (busSeats <= 0) errors.append("• Business Seats (must be positive)\n");
        } catch (NumberFormatException e) {
            errors.append("• Business Seats (must be a number)\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Please correct the following:\n" + errors.toString());
            return false;
        }

        return true;
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) flightCodeField.getScene().getWindow();
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