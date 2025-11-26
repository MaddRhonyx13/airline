package com.example.airline.controllers;

import com.example.airline.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class SystemSettingsController implements Initializable {

    @FXML private TextField airlineNameField;
    @FXML private TextField contactEmailField;
    @FXML private TextField contactPhoneField;
    @FXML private TextField websiteField;
    @FXML private TextArea addressArea;

    @FXML private Spinner<Integer> maxSeatsSpinner;
    @FXML private Spinner<Integer> cancellationHoursSpinner;
    @FXML private Spinner<Double> taxRateSpinner;

    @FXML private CheckBox allowOnlineCheckin;
    @FXML private CheckBox sendEmailNotifications;
    @FXML private CheckBox maintenanceMode;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeSpinners();
        loadSettings();
    }

    private void initializeSpinners() {
        maxSeatsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(50, 500, 150));
        cancellationHoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 72, 24));
        taxRateSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 20.0, 5.0, 0.5));
    }

    @FXML
    private void handleSaveGeneral() {
        try {
            saveGeneralSettings();
            updateStatus("General settings saved successfully");
            showAlert(Alert.AlertType.INFORMATION, "Success", "General settings saved!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save settings: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveBusiness() {
        try {
            saveBusinessSettings();
            updateStatus("Business settings saved successfully");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Business settings saved!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save settings: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveSecurity() {
        if (validatePasswordChange()) {
            try {
                saveSecuritySettings();
                updateStatus("Security settings saved successfully");
                showAlert(Alert.AlertType.INFORMATION, "Success", "Password changed successfully!");
                clearPasswordFields();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to change password: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleResetSettings() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Reset");
        confirm.setHeaderText("Reset All Settings");
        confirm.setContentText("Are you sure you want to reset all settings to default values? This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            resetToDefaults();
            updateStatus("All settings reset to defaults");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Settings reset to defaults!");
        }
    }

    @FXML
    private void handleBackup() {
        try {
            performBackup();
            updateStatus("Database backup created successfully");
            showAlert(Alert.AlertType.INFORMATION, "Backup Complete",
                    "Database backup created successfully!\nBackup file: airline_backup.db");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Backup Error", "Failed to create backup: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) airlineNameField.getScene().getWindow();
        stage.close();
    }

    private void loadSettings() {
        airlineNameField.setText("Skyline Airways");
        contactEmailField.setText("info@skylineairways.com");
        contactPhoneField.setText("+1-555-0123");
        websiteField.setText("www.skylineairways.com");
        addressArea.setText("123 Airport Road\nAviation City, AC 12345");

        allowOnlineCheckin.setSelected(true);
        sendEmailNotifications.setSelected(true);
        maintenanceMode.setSelected(false);

        updateStatus("Settings loaded successfully");
    }

    private void saveGeneralSettings() throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO system_settings (setting_key, setting_value) 
            VALUES 
            ('airline_name', ?),
            ('contact_email', ?),
            ('contact_phone', ?),
            ('website', ?),
            ('address', ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, airlineNameField.getText());
            pstmt.setString(2, contactEmailField.getText());
            pstmt.setString(3, contactPhoneField.getText());
            pstmt.setString(4, websiteField.getText());
            pstmt.setString(5, addressArea.getText());

            pstmt.executeUpdate();
        }
    }

    private void saveBusinessSettings() throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO system_settings (setting_key, setting_value) 
            VALUES 
            ('max_seats', ?),
            ('cancellation_hours', ?),
            ('tax_rate', ?),
            ('online_checking', ?),
            ('email_notifications', ?),
            ('maintenance_mode', ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, maxSeatsSpinner.getValue());
            pstmt.setInt(2, cancellationHoursSpinner.getValue());
            pstmt.setDouble(3, taxRateSpinner.getValue());
            pstmt.setBoolean(4, allowOnlineCheckin.isSelected());
            pstmt.setBoolean(5, sendEmailNotifications.isSelected());
            pstmt.setBoolean(6, maintenanceMode.isSelected());

            pstmt.executeUpdate();
        }
    }

    private void saveSecuritySettings() throws SQLException {
        String verifySql = "SELECT password FROM users WHERE username = 'admin'";
        String updateSql = "UPDATE users SET password = ? WHERE username = 'admin'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            ResultSet rs = verifyStmt.executeQuery();
            if (rs.next()) {
                String currentPassword = rs.getString("password");
                if (!currentPassword.equals(currentPasswordField.getText())) {
                    throw new SQLException("Current password is incorrect");
                }

                updateStmt.setString(1, newPasswordField.getText());
                updateStmt.executeUpdate();
            }
        }
    }

    private void performBackup() throws Exception {
        java.nio.file.Path backupDir = java.nio.file.Paths.get("backups");
        if (!java.nio.file.Files.exists(backupDir)) {
            java.nio.file.Files.createDirectories(backupDir);
        }

        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFile = "airline_backup_" + timestamp + ".db";

        java.nio.file.Path source = java.nio.file.Paths.get("airline_reservation.db");
        java.nio.file.Path target = backupDir.resolve(backupFile);

        java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private void resetToDefaults() {
        airlineNameField.setText("Skyline Airways");
        contactEmailField.setText("info@skylineairways.com");
        contactPhoneField.setText("+1-555-0123");
        websiteField.setText("www.skylineairways.com");
        addressArea.setText("123 Airport Road\nAviation City, AC 12345");

        maxSeatsSpinner.getValueFactory().setValue(150);
        cancellationHoursSpinner.getValueFactory().setValue(24);
        taxRateSpinner.getValueFactory().setValue(5.0);

        allowOnlineCheckin.setSelected(true);
        sendEmailNotifications.setSelected(true);
        maintenanceMode.setSelected(false);

        clearPasswordFields();
    }

    private boolean validatePasswordChange() {
        if (currentPasswordField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please enter current password");
            return false;
        }

        if (newPasswordField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please enter new password");
            return false;
        }

        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            showAlert(Alert.AlertType.WARNING, "Validation", "New passwords do not match");
            return false;
        }

        if (newPasswordField.getText().length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Validation", "New password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void updateStatus(String message) {
        statusLabel.setText("Status: " + message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
