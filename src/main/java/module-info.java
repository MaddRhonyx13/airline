module com.example.airline {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;


    opens com.example.airline to javafx.fxml;
    opens com.example.airline.controllers to javafx.fxml;

    exports com.example.airline;
    exports com.example.airline.controllers;
}