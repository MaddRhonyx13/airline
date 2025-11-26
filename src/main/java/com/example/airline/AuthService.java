package com.example.airline;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {
    private static User currentUser = null;

    public static boolean login(String username, String password) {
        String sql = "SELECT user_id, username, password, role, full_name, email, is_active FROM users WHERE username = ? AND is_active = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                // In real application, use proper password hashing!
                // For demo purposes, we're using plain text comparison
                if (storedPassword.equals(password)) {
                    currentUser = new User();
                    currentUser.setUserId(rs.getInt("user_id"));
                    currentUser.setUsername(rs.getString("username"));
                    currentUser.setRole(rs.getString("role"));
                    currentUser.setFullName(rs.getString("full_name"));
                    currentUser.setEmail(rs.getString("email"));
                    currentUser.setActive(rs.getBoolean("is_active"));

                    System.out.println("Login successful: " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }

        return false;
    }

    public static void logout() {
        currentUser = null;
        System.out.println("User logged out");
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean hasRole(String role) {
        return isLoggedIn() && currentUser.getRole().equals(role);
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static boolean isAgent() {
        return hasRole("AGENT");
    }

    public static boolean isCustomer() {
        return hasRole("CUSTOMER");
    }
}

