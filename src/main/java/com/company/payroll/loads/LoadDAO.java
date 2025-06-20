package com.company.payroll.loads;

import com.company.payroll.employees.Employee;
import com.company.payroll.employees.EmployeeDAO;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class LoadDAO {

    private static final String DB_URL = "jdbc:sqlite:payroll.db";
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    public LoadDAO() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // If delivery_date doesn't exist, add it (for upgrades)
            conn.createStatement().execute(
                "ALTER TABLE loads ADD COLUMN delivery_date DATE"
            );
        } catch (SQLException ignore) {
            // Ignore error if column already exists
        }
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                CREATE TABLE IF NOT EXISTS loads (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    load_number TEXT NOT NULL UNIQUE,
                    customer TEXT,
                    pick_up_location TEXT,
                    drop_location TEXT,
                    driver_id INTEGER,
                    status TEXT,
                    gross_amount REAL,
                    notes TEXT,
                    delivery_date DATE,
                    FOREIGN KEY(driver_id) REFERENCES employees(id)
                );
            """;
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Load> getAll() {
        List<Load> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM loads";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                list.add(extractLoad(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int add(Load load) {
        String sql = "INSERT INTO loads (load_number, customer, pick_up_location, drop_location, driver_id, status, gross_amount, notes, delivery_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, load.getLoadNumber());
            ps.setString(2, load.getCustomer());
            ps.setString(3, load.getPickUpLocation());
            ps.setString(4, load.getDropLocation());
            ps.setObject(5, load.getDriver() != null ? load.getDriver().getId() : null);
            ps.setString(6, load.getStatus().name());
            ps.setDouble(7, load.getGrossAmount());
            ps.setString(8, load.getNotes());
            if (load.getDeliveryDate() != null)
                ps.setDate(9, java.sql.Date.valueOf(load.getDeliveryDate()));
            else
                ps.setNull(9, Types.DATE);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed: loads.load_number")) {
                throw new RuntimeException("Duplicate Load # not allowed.");
            }
            e.printStackTrace();
        }
        return -1;
    }

    public void update(Load load) {
        String sql = "UPDATE loads SET load_number=?, customer=?, pick_up_location=?, drop_location=?, driver_id=?, status=?, gross_amount=?, notes=?, delivery_date=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, load.getLoadNumber());
            ps.setString(2, load.getCustomer());
            ps.setString(3, load.getPickUpLocation());
            ps.setString(4, load.getDropLocation());
            ps.setObject(5, load.getDriver() != null ? load.getDriver().getId() : null);
            ps.setString(6, load.getStatus().name());
            ps.setDouble(7, load.getGrossAmount());
            ps.setString(8, load.getNotes());
            if (load.getDeliveryDate() != null)
                ps.setDate(9, java.sql.Date.valueOf(load.getDeliveryDate()));
            else
                ps.setNull(9, Types.DATE);
            ps.setInt(10, load.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed: loads.load_number")) {
                throw new RuntimeException("Duplicate Load # not allowed.");
            }
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM loads WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Load getById(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM loads WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractLoad(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Load> getByStatus(Load.Status status) {
        List<Load> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM loads WHERE status = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(extractLoad(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Load> getByDriver(int driverId) {
        List<Load> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM loads WHERE driver_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, driverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(extractLoad(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Load> getByGrossAmountRange(double min, double max) {
        List<Load> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM loads WHERE gross_amount >= ? AND gross_amount <= ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDouble(1, min);
            ps.setDouble(2, max);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(extractLoad(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Load> search(String loadNum, String customer, Integer driverId, Load.Status status) {
        List<Load> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM loads WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (loadNum != null && !loadNum.isBlank()) {
            sql.append(" AND lower(load_number) LIKE ?");
            params.add("%" + loadNum.toLowerCase() + "%");
        }
        if (customer != null && !customer.isBlank()) {
            sql.append(" AND lower(customer) LIKE ?");
            params.add("%" + customer.toLowerCase() + "%");
        }
        if (driverId != null) {
            sql.append(" AND driver_id = ?");
            params.add(driverId);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); ++i)
                ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(extractLoad(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Utility: extract a Load from the current ResultSet row
    private Load extractLoad(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String loadNumber = rs.getString("load_number");
        String customer = rs.getString("customer");
        String pickUp = rs.getString("pick_up_location");
        String drop = rs.getString("drop_location");
        int driverId = rs.getInt("driver_id");
        Employee driver = employeeDAO.getById(driverId);
        Load.Status status = Load.Status.valueOf(rs.getString("status"));
        double gross = rs.getDouble("gross_amount");
        String notes = rs.getString("notes");
        LocalDate deliveryDate = null;
        java.sql.Date sqlDate = rs.getDate("delivery_date");
        if (sqlDate != null) deliveryDate = sqlDate.toLocalDate();
        return new Load(id, loadNumber, customer, pickUp, drop, driver, status, gross, notes, deliveryDate);
    }
}