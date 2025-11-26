package com.example.airline.controllers;

import com.example.airline.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class MainController {

    @FXML private Button reservationButton;
    @FXML private Label welcomeLabel;
    @FXML private Label userRoleLabel;
    @FXML private Menu adminMenu;
    @FXML private MenuItem flightManagementMenuItem;
    @FXML private MenuItem userManagementMenuItem;

    @FXML
    public void initialize() {
        if (AuthService.isLoggedIn()) {
            welcomeLabel.setText("Welcome, " + AuthService.getCurrentUser().getFullName() + "!");
            userRoleLabel.setText("Role: " + AuthService.getCurrentUser().getRole());

            //applyRoleBasedAccess();
        }
    }

    private void applyRoleBasedAccess() {
        boolean isAdmin = AuthService.isAdmin();
        boolean isAgent = AuthService.isAgent();
        boolean isCustomer = AuthService.isCustomer();

        adminMenu.setVisible(isAdmin);
        //adminMenu.setManaged(isAdmin);

        flightManagementMenuItem.setVisible(isAdmin || isAgent);
        //flightManagementMenuItem.setManaged(isAdmin || isAgent);

        userManagementMenuItem.setVisible(isAdmin);
        //userManagementMenuItem.setManaged(isAdmin);

        if (isCustomer) {
            userRoleLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else if (isAgent) {
            userRoleLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        } else if (isAdmin) {
            userRoleLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleExit() {
        System.exit(0);
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Confirm Logout");
        confirm.setContentText("Are you sure you want to logout?");

        if (confirm.showAndWait().get().getText().equals("OK")) {
            AuthService.logout();

            Stage mainStage = (Stage) reservationButton.getScene().getWindow();
            mainStage.close();

            showLoginWindow();
        }
    }

    @FXML
    private void showAbout() {
        String userInfo = AuthService.isLoggedIn() ?
                "\nLogged in as: " + AuthService.getCurrentUser().getFullName() +
                        " (" + AuthService.getCurrentUser().getRole() + ")" : "";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Airline Reservation System");
        alert.setHeaderText("Airline Reservation System");
        alert.setContentText("This is a JavaFX application for airline ticket reservations." + userInfo + "\n\nVersion 2.0 with Role-Based Access Control");
        alert.showAndWait();
    }

    @FXML
    private void showRegistrationForm(){
        loadWindow("/views/RegistrationForm.fxml", "User Registration");
    }

    @FXML
    private void showCustomerForm() {
        if (AuthService.isCustomer()) {
            showAlert(Alert.AlertType.INFORMATION, "Customer Access",
                    "As a customer, you can view your bookings and make new reservations.");
        }
        loadWindow("/views/CustomerForm.fxml", "Customer Reservation");
    }

    @FXML
    private void showFlightManagement() {
        if (!AuthService.isAdmin() && !AuthService.isAgent()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied",
                    "Only Administrators and Agents can manage flights.");
            return;
        }
        loadWindow("/views/FlightManagement.fxml", "Flight Management");
    }

    @FXML
    private void showUserManagement() {
        if (!AuthService.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied",
                    "Only Administrators can manage users.");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "User Management",
                "User management feature would open here for administrators.");
        loadWindow("/views/UserManagement.fxml", "User Management");
    }

    @FXML
    private void showFlightSearch() {
        loadWindow("/views/FlightSearch.fxml", "Flight Search");
    }

    @FXML
    private void showCancellationForm() {
        loadWindow("/views/CancellationForm.fxml", "Cancel Booking");
    }

    @FXML
    private void showAllBookings() {
        loadWindow("/views/AllBookings.fxml", "All Bookings");
    }

    @FXML
    private void showAllReservations() {
        loadWindow("/views/AllReservations.fxml", "All Reservations");
    }

    private void loadWindow(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = new Stage();
            stage.setTitle(title + " - " + AuthService.getCurrentUser().getRole());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load " + title);
        }
    }

    private void showLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginForm.fxml"));
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Airline Reservation System - Login");
            loginStage.setScene(new Scene(root, 600, 700));
            loginStage.setResizable(false);
            loginStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showSystemSettings() {
        loadWindow("/views/SystemSettings.fxml", "System Settings");
    }

    public void showReservationSearch() {
    }
}
