package com.example.airline.controllers;

import com.example.airline.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ResourceBundle;

public class RegistrationController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressArea;

    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    private void handleRegister() {
        if (validateForm()) {
            try {
                progressIndicator.setVisible(true);
                statusLabel.setText("Creating account...");

                if (registerUser()) {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Account created successfully!");

                    showAlert(Alert.AlertType.INFORMATION, "Registration Successful",
                            "Your account has been created successfully!\n\n" +
                                    "Username: " + usernameField.getText() + "\n" +
                                    "Role: CUSTOMER\n\n" +
                                    "You can now login to the system.");

                    clearForm();
                    openLoginWindow();
                }
            } catch (Exception e) {
                progressIndicator.setVisible(false);
                statusLabel.setText("Registration failed");
                showAlert(Alert.AlertType.ERROR, "Registration Error",
                        "Failed to create account: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleLogin() {
        openLoginWindow();
    }

    private boolean registerUser() throws Exception {
        String sql = "INSERT INTO users (username, password, role, full_name, email, phone, address, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usernameField.getText());
            pstmt.setString(2, passwordField.getText());
            pstmt.setString(3, "CUSTOMER");
            pstmt.setString(4, fullNameField.getText());
            pstmt.setString(5, emailField.getText());
            pstmt.setString(6, phoneField.getText());
            pstmt.setString(7, addressArea.getText());
            pstmt.setBoolean(8, true);

            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (usernameField.getText().isEmpty()) errors.append("• Username\n");
        if (passwordField.getText().isEmpty()) errors.append("• Password\n");
        if (confirmPasswordField.getText().isEmpty()) errors.append("• Confirm Password\n");
        if (fullNameField.getText().isEmpty()) errors.append("• Full Name\n");
        if (emailField.getText().isEmpty()) errors.append("• Email\n");

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            errors.append("• Passwords do not match\n");
        }

        if (passwordField.getText().length() < 6) {
            errors.append("• Password must be at least 6 characters\n");
        }

        if (usernameField.getText().length() < 3) {
            errors.append("• Username must be at least 3 characters\n");
        }

        if (!errors.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Please correct the following:\n" + errors);
            return false;
        }

        return true;
    }

    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        fullNameField.clear();
        emailField.clear();
        phoneField.clear();
        addressArea.clear();
    }

    private void openLoginWindow() {
        try {
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginForm.fxml"));
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Airline Reservation System - Login");
            loginStage.setScene(new Scene(root, 600, 700));
            loginStage.setResizable(false);
            loginStage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot open login window: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
