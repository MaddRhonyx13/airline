package com.example.airline.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    @FXML private Label versionLabel;
    @FXML private Label buildDateLabel;
    @FXML private TextArea licenseArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        displaySystemInfo();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) versionLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSystemInfo() {
        showSystemInfo();
    }

    private void displaySystemInfo() {
        versionLabel.setText("Version 2.0.0");
        buildDateLabel.setText("Build Date: 2024-01-15");

        licenseArea.setText("""
            AIRLINE RESERVATION SYSTEM
            Version 2.0.0
            
            LICENSE AGREEMENT
            
            Copyright © 2024 Skyline Airways. All rights reserved.
            
            This software is licensed for use by Skyline Airways and its affiliates.
            Unauthorized reproduction or distribution of this software, or any portion of it,
            may result in severe civil and criminal penalties, and will be prosecuted
            to the maximum extent possible under law.
            
            FEATURES:
            • Complete reservation management
            • Real-time flight search and booking
            • User role-based access control
            • E-ticket generation and printing
            • Database backup and recovery
            • Multi-user support
            
            TECHNOLOGIES:
            • JavaFX 18 for modern UI
            • SQLite for reliable data storage
            • JDBC for database connectivity
            • Maven for build management
            
            SUPPORT:
            For technical support, please contact:
            Email: support@skylineairways.com
            Phone: +266-5151-9191
            """);
    }

    private void showSystemInfo() {
        String systemInfo = String.format("""
            SYSTEM INFORMATION:
            
            Java Version: %s
            JavaFX Version: %s
            Operating System: %s
            Architecture: %s
            Processors: %d
            Memory: %d MB
            """,
                System.getProperty("java.version"),
                System.getProperty("javafx.version"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory() / (1024 * 1024)
        );

        TextArea infoArea = new TextArea(systemInfo);
        infoArea.setEditable(false);
        infoArea.setStyle("-fx-font-family: 'Courier New';");

        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("System Information");
        infoAlert.setHeaderText("Technical Details");
        infoAlert.getDialogPane().setContent(infoArea);
        infoAlert.getDialogPane().setPrefSize(500, 300);
        infoAlert.showAndWait();
    }
}
