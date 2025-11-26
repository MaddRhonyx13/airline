package com.example.airline.controllers;

import com.example.airline.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roleComboBox.getItems().addAll("ADMIN", "AGENT", "CUSTOMER");
        roleComboBox.setValue("ADMIN");
        roleComboBox.setStyle("-fx-background-color: white; -fx-border-width: 1px; -fx-border-color: #764ba2; -fx-text-fill: #764ba2; -fx-font-weight: bold; -fx-height: 10px;");

        usernameField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> hideError());

        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String selectedRole = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        loginButton.setText("Logging in...");
        loginButton.setDisable(true);

        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1)).play();

        boolean loginSuccess = AuthService.login(username, password);

        if (loginSuccess) {
            String actualRole = AuthService.getCurrentUser().getRole();
            if (!actualRole.equals(selectedRole)) {
                showError("This user does not have " + selectedRole + " role. Actual role: " + actualRole);
                AuthService.logout();
                resetLoginButton();
                return;
            }

            openMainApplication();
        } else {
            showError("Invalid username or password");
            resetLoginButton();
        }
    }

    @FXML
    private void handleRegister() {
        try {
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RegistrationForm.fxml"));
            Parent root = loader.load();

            Stage registerStage = new Stage();
            registerStage.setTitle("Airline Reservation System - Register");
            registerStage.setScene(new Scene(root, 600, 800));
            registerStage.setResizable(true);
            registerStage.show();

        } catch (Exception e) {
            showError("Cannot open registration form: " + e.getMessage());
        }
    }

    private void openMainApplication() {
        try {
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
            Parent root = loader.load();

            Stage mainStage = new Stage();
            mainStage.setTitle("Airline Reservation System - " + AuthService.getCurrentUser().getFullName() + " (" + AuthService.getCurrentUser().getRole() + ")");
            mainStage.setScene(new Scene(root, 1200, 800));
            mainStage.setMinWidth(1000);
            mainStage.setMinHeight(700);
            mainStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error opening application: " + e.getMessage());
            resetLoginButton();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void resetLoginButton() {
        loginButton.setText("Login");
        loginButton.setDisable(false);
    }
}

