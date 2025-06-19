package com.company.payroll.employees;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {
    private static final String DB_URL = "jdbc:sqlite:payroll.db";

    public EmployeeDAO() {
        // Create table if not exists
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                CREATE TABLE IF NOT EXISTS employees (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    truck_unit TEXT,
                    driver_percent REAL,
                    company_percent REAL,
                    service_fee_percent REAL,
                    dob DATE,
                    license_number TEXT,
                    driver_type TEXT,
                    employee_llc TEXT,
                    cdl_expiry DATE,
                    medical_expiry DATE,
                    status TEXT
                );
            """;
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Employee> getAll() {
        List<Employee> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM employees";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int add(Employee emp) {
        String sql = """
            INSERT INTO employees 
            (name, truck_unit, driver_percent, company_percent, service_fee_percent, dob, license_number, driver_type, employee_llc, cdl_expiry, medical_expiry, status) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, emp);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void update(Employee emp) {
        String sql = """
            UPDATE employees SET 
                name=?, truck_unit=?, driver_percent=?, company_percent=?, service_fee_percent=?, dob=?, license_number=?, driver_type=?, employee_llc=?, cdl_expiry=?, medical_expiry=?, status=?
            WHERE id=?
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, emp);
            ps.setInt(13, emp.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM employees WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Employee getById(int id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper to map ResultSet row to Employee
    private Employee mapRow(ResultSet rs) throws SQLException {
        return new Employee(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("truck_unit"),
            rs.getDouble("driver_percent"),
            rs.getDouble("company_percent"),
            rs.getDouble("service_fee_percent"),
            rs.getObject("dob") != null ? rs.getDate("dob").toLocalDate() : null,
            rs.getString("license_number"),
            rs.getString("driver_type") != null ? Employee.DriverType.valueOf(rs.getString("driver_type")) : null,
            rs.getString("employee_llc"),
            rs.getObject("cdl_expiry") != null ? rs.getDate("cdl_expiry").toLocalDate() : null,
            rs.getObject("medical_expiry") != null ? rs.getDate("medical_expiry").toLocalDate() : null,
            rs.getString("status") != null ? Employee.Status.valueOf(rs.getString("status")) : null
        );
    }

    // Helper to set parameters for PreparedStatement
    private void setParams(PreparedStatement ps, Employee emp) throws SQLException {
        ps.setString(1, emp.getName());
        ps.setString(2, emp.getTruckUnit());
        ps.setDouble(3, emp.getDriverPercent());
        ps.setDouble(4, emp.getCompanyPercent());
        ps.setDouble(5, emp.getServiceFeePercent());
        if (emp.getDob() != null)
            ps.setDate(6, Date.valueOf(emp.getDob()));
        else
            ps.setNull(6, Types.DATE);
        ps.setString(7, emp.getLicenseNumber());
        ps.setString(8, emp.getDriverType() != null ? emp.getDriverType().name() : null);
        ps.setString(9, emp.getEmployeeLLC());
        if (emp.getCdlExpiry() != null)
            ps.setDate(10, Date.valueOf(emp.getCdlExpiry()));
        else
            ps.setNull(10, Types.DATE);
        if (emp.getMedicalExpiry() != null)
            ps.setDate(11, Date.valueOf(emp.getMedicalExpiry()));
        else
            ps.setNull(11, Types.DATE);
        ps.setString(12, emp.getStatus() != null ? emp.getStatus().name() : null);
    }
}
