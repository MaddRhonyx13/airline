package com.example.airline;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:airline_reservation.db";
    private static Connection connection = null;
    private static boolean isInitialized = false;

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL);
                System.out.println("Connected to database successfully!");

                if (!isInitialized) {
                    initializeDatabase();
                    isInitialized = true;
                }

                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error connecting to SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }

    public static synchronized void initializeDatabase() {
        System.out.println("Initializing database...");

        String[] createTables = {
                """
            CREATE TABLE IF NOT EXISTS users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(100) NOT NULL,
                role VARCHAR(20) NOT NULL,
                full_name TEXT NOT NULL,
                email VARCHAR(100),
                phone VARCHAR(20),
                address TEXT,
                is_active BOOLEAN DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS customer_details (
                cust_id INTEGER PRIMARY KEY AUTOINCREMENT,
                pnr_number VARCHAR(20) UNIQUE NOT NULL,
                t_date DATE NOT NULL,
                cust_name TEXT NOT NULL,
                father_name TEXT,
                gender TEXT,
                d_o_b DATE,
                address TEXT,
                tel_no VARCHAR(15),
                profession TEXT,
                security TEXT,
                concession TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS flight_information (
                flight_id INTEGER PRIMARY KEY AUTOINCREMENT,
                f_code VARCHAR(10) UNIQUE NOT NULL,
                f_name TEXT NOT NULL,
                route TEXT NOT NULL,
                source_place TEXT NOT NULL,
                destination_place TEXT NOT NULL,
                class_code VARCHAR(5),
                departure_time TIME,
                arrival_time TIME,
                t_eco_seatno INTEGER DEFAULT 150,
                t_exe_seatno INTEGER DEFAULT 30,
                eco_seats_booked INTEGER DEFAULT 0,
                exe_seats_booked INTEGER DEFAULT 0,
                is_active BOOLEAN DEFAULT 1
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS fare (
                fare_id INTEGER PRIMARY KEY AUTOINCREMENT,
                route_code VARCHAR(10),
                f_code VARCHAR(10),
                class_type VARCHAR(20),
                base_fare DECIMAL(10,2) NOT NULL,
                FOREIGN KEY (f_code) REFERENCES flight_information(f_code)
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS reservations (
                reservation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                pnr_number VARCHAR(20) UNIQUE NOT NULL,
                f_code VARCHAR(10),
                cust_id INTEGER,
                class_type VARCHAR(20),
                seat_number VARCHAR(10),
                seat_preference TEXT,
                base_fare DECIMAL(10,2),
                discount_amount DECIMAL(10,2),
                final_fare DECIMAL(10,2),
                concession_type TEXT,
                status TEXT DEFAULT 'Confirmed',
                travel_date DATE,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (pnr_number) REFERENCES customer_details(pnr_number),
                FOREIGN KEY (f_code) REFERENCES flight_information(f_code),
                FOREIGN KEY (cust_id) REFERENCES customer_details(cust_id)
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS cancellations (
                cancellation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                pnr_number VARCHAR(20),
                f_code VARCHAR(10),
                cust_id INTEGER,
                class_type TEXT,
                base_amount DECIMAL(10,2),
                cancellation_charge DECIMAL(10,2),
                refund_amount DECIMAL(10,2),
                reason TEXT,
                cancelled_by INTEGER,
                cancelled_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (pnr_number) REFERENCES customer_details(pnr_number),
                FOREIGN KEY (f_code) REFERENCES flight_information(f_code),
                FOREIGN KEY (cust_id) REFERENCES customer_details(cust_id)
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS seat_allocation (
                seat_id INTEGER PRIMARY KEY AUTOINCREMENT,
                f_code VARCHAR(10),
                class_type VARCHAR(20),
                seat_number VARCHAR(10),
                is_available BOOLEAN DEFAULT 1,
                is_window_seat BOOLEAN DEFAULT 0,
                pnr_number VARCHAR(20),
                FOREIGN KEY (f_code) REFERENCES flight_information(f_code)
            )
            """
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : createTables) {
                stmt.execute(sql);
            }
            System.out.println("Database tables initialized successfully!");

            insertSampleData();

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void insertSampleData() {
        String[] sampleData = {
                """
            INSERT OR IGNORE INTO users (username, password, role, full_name, email) VALUES
            ('admin', 'admin123', 'ADMIN', 'System Administrator', 'admin@airline.com'),
            ('agent1', 'agent123', 'AGENT', 'Booking Agent 1', 'agent1@airline.com'),
            ('agent2', 'agent123', 'AGENT', 'Booking Agent 2', 'agent2@airline.com'),
            ('customer1', 'customer123', 'CUSTOMER', 'John Customer', 'john@example.com')
            """,

                """
            INSERT OR IGNORE INTO flight_information 
            (f_code, f_name, route, source_place, destination_place, class_code, departure_time, arrival_time, t_eco_seatno, t_exe_seatno)
            VALUES 
            ('AE101', 'Air African Express', 'Maseru-Durban', 'Maseru', 'Durban', 'ECO', '08:00', '10:30', 150, 20),
            ('AL102', 'Air Lesotho', 'Maseru-Johannesburg', 'Maseru', 'Johannesburg', 'BUS', '14:00', '16:30', 120, 30)
            """,

                """
            INSERT OR IGNORE INTO fare 
            (route_code, f_code, class_type, base_fare)
            VALUES 
            ('DM001', 'AE101', 'Economy', 2500.00),
            ('DM002', 'AL102', 'Business', 1500.00)
            """,

                """
            INSERT OR IGNORE INTO customer_details 
            (pnr_number, t_date, cust_name, father_name, gender, address, tel_no, profession, security, concession)
            VALUES 
            ('PNR10001', '2024-02-20', 'Tumisang Madd', 'MaddRhonyx Madd', 'Male', 'Maseru, Durban', '9876543210', 'Engineer', 'Standard', 'None')
            """,

                """
            INSERT OR IGNORE INTO reservations 
            (pnr_number, f_code, cust_id, class_type, seat_number, seat_preference, base_fare, discount_amount, final_fare, concession_type, status, travel_date)
            VALUES 
            ('PNR10001', 'AE101', 1, 'Economy', 'E15', 'Window', 2500.00, 0.00, 2500.00, 'None', 'Confirmed', '2024-02-20')
            """
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : sampleData) {
                stmt.execute(sql);
            }
            System.out.println("Sample data inserted successfully!");
        } catch (SQLException e) {
            System.err.println("Error inserting sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
                connection = null;
                isInitialized = false;
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            closeConnection();
        }));
    }
}
