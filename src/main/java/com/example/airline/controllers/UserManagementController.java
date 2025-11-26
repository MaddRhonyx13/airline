package com.example.airline.controllers;

import com.example.airline.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colCreated;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private CheckBox activeCheckbox;

    @FXML private TextField searchField;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;

    private ObservableList<User> users = FXCollections.observableArrayList();
    private User selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        initializeComboBoxes();
        loadUsers();
        clearForm();
    }

    private void setupTableColumns() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Status column with color coding
        colStatus.setCellFactory(column -> new TableCell<User, String>() {
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

        usersTable.setItems(users);

        // Add selection listener
        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        selectedUser = newSelection;
                        displayUserDetails(newSelection);
                    }
                });
    }

    private void initializeComboBoxes() {
        roleCombo.getItems().addAll("ADMIN", "AGENT", "CUSTOMER");
        roleCombo.setValue("CUSTOMER");
    }

    @FXML
    private void handleAddUser() {
        if (validateForm()) {
            try {
                User newUser = createUserFromForm();
                if (addUserToDatabase(newUser)) {
                    users.add(newUser);
                    clearForm();
                    updateStatus("User added successfully: " + newUser.getUsername());
                    showAlert(Alert.AlertType.INFORMATION, "Success", "User added successfully!");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add user: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdateUser() {
        if (selectedUser != null) {
            if (validateForm()) {
                try {
                    User updatedUser = createUserFromForm();
                    updatedUser.setUserId(selectedUser.getUserId());

                    if (updateUserInDatabase(updatedUser)) {
                        int index = users.indexOf(selectedUser);
                        users.set(index, updatedUser);
                        usersTable.refresh();
                        updateStatus("User updated successfully: " + updatedUser.getUsername());
                        showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user: " + e.getMessage());
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to update");
        }
    }

    @FXML
    private void handleDeleteUser() {
        if (selectedUser != null) {
            // Prevent deletion of current user
            if (selectedUser.getUsername().equals("admin")) {
                showAlert(Alert.AlertType.ERROR, "Cannot Delete", "Cannot delete the admin user.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete User");
            confirm.setContentText("Are you sure you want to delete user: " + selectedUser.getUsername() + "?\nThis action cannot be undone.");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    if (deleteUserFromDatabase(selectedUser.getUserId())) {
                        users.remove(selectedUser);
                        clearForm();
                        updateStatus("User deleted successfully");
                        showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted successfully!");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete user: " + e.getMessage());
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to delete");
        }
    }

    @FXML
    private void handleResetPassword() {
        if (selectedUser != null) {
            TextInputDialog dialog = new TextInputDialog("password123");
            dialog.setTitle("Reset Password");
            dialog.setHeaderText("Reset Password for: " + selectedUser.getUsername());
            dialog.setContentText("Enter new password:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                try {
                    if (resetPasswordInDatabase(selectedUser.getUserId(), result.get())) {
                        updateStatus("Password reset for: " + selectedUser.getUsername());
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Password reset successfully!");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to reset password: " + e.getMessage());
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to reset password");
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            usersTable.setItems(users);
        } else {
            ObservableList<User> filtered = FXCollections.observableArrayList();
            for (User user : users) {
                if (user.getUsername().toLowerCase().contains(searchText) ||
                        user.getFullName().toLowerCase().contains(searchText) ||
                        user.getEmail().toLowerCase().contains(searchText) ||
                        user.getRole().toLowerCase().contains(searchText)) {
                    filtered.add(user);
                }
            }
            usersTable.setItems(filtered);
        }
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
        clearForm();
        searchField.clear();
        updateStatus("User list refreshed");
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) usersTable.getScene().getWindow();
        stage.close();
    }

    private void loadUsers() {
        loadingIndicator.setVisible(true);
        users.clear();

        String sql = "SELECT user_id, username, password, role, full_name, email, is_active, created_at FROM users ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("full_name"),
                        rs.getString("email")
                );
                user.setUserId(rs.getInt("user_id"));
                user.setActive(rs.getBoolean("is_active"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                users.add(user);
            }

            updateStatus("Loaded " + users.size() + " users");
            loadingIndicator.setVisible(false);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Error loading users: " + e.getMessage());
            loadingIndicator.setVisible(false);
        }
    }

    private boolean addUserToDatabase(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, full_name, email, is_active) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword()); // In real app, hash this password
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getEmail());
            pstmt.setBoolean(6, user.isActive());

            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean updateUserInDatabase(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, role = ?, full_name = ?, email = ?, is_active = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getRole());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getEmail());
            pstmt.setBoolean(5, user.isActive());
            pstmt.setInt(6, user.getUserId());

            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean deleteUserFromDatabase(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean resetPasswordInDatabase(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newPassword); // In real app, hash this password
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;
        }
    }

    private User createUserFromForm() {
        User user = new User(
                usernameField.getText(),
                passwordField.getText(),
                roleCombo.getValue(),
                fullNameField.getText(),
                emailField.getText()
        );
        user.setActive(activeCheckbox.isSelected());
        return user;
    }

    private void displayUserDetails(User user) {
        usernameField.setText(user.getUsername());
        passwordField.clear(); // Don't show password
        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        roleCombo.setValue(user.getRole());
        activeCheckbox.setSelected(user.isActive());
    }

    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        fullNameField.clear();
        emailField.clear();
        roleCombo.setValue("CUSTOMER");
        activeCheckbox.setSelected(true);
        selectedUser = null;
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (usernameField.getText().isEmpty()) errors.append("• Username\n");
        if (passwordField.getText().isEmpty() && selectedUser == null) errors.append("• Password\n");
        if (fullNameField.getText().isEmpty()) errors.append("• Full Name\n");
        if (emailField.getText().isEmpty()) errors.append("• Email\n");

        // Check if username already exists (for new users)
        if (selectedUser == null) {
            String username = usernameField.getText();
            for (User user : users) {
                if (user.getUsername().equalsIgnoreCase(username)) {
                    errors.append("• Username already exists\n");
                    break;
                }
            }
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Please correct the following:\n" + errors.toString());
            return false;
        }

        return true;
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

    // User model class
    public static class User {
        private int userId;
        private String username;
        private String password;
        private String role;
        private String fullName;
        private String email;
        private boolean isActive;
        private LocalDateTime createdAt;

        public User(String username, String password, String role, String fullName, String email) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.fullName = fullName;
            this.email = email;
            this.isActive = true;
            this.createdAt = LocalDateTime.now();
        }

        // Getters and Setters
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        // Derived properties for table display
        public String getStatus() { return isActive ? "Active" : "Inactive"; }
    }
}

