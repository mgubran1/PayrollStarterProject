package com.company.payroll.fuel;

import com.company.payroll.employees.Employee;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FuelTransactionDAO {
    private static final String DB_URL = "jdbc:sqlite:payroll.db";

    public FuelTransactionDAO() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                CREATE TABLE IF NOT EXISTS fuel_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    card_number TEXT,
                    tran_date TEXT,
                    tran_time TEXT,
                    invoice TEXT,
                    unit TEXT,
                    driver_name TEXT,
                    odometer TEXT,
                    location_name TEXT,
                    city TEXT,
                    state_prov TEXT,
                    fees REAL,
                    item TEXT,
                    unit_price REAL,
                    disc_ppu REAL,
                    disc_cost REAL,
                    qty REAL,
                    disc_amt REAL,
                    disc_type TEXT,
                    amt REAL,
                    db TEXT,
                    currency TEXT,
                    employee_id INTEGER,
                    UNIQUE(invoice, tran_date, location_name, amt)
                );
            """;
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<FuelTransaction> getAll() {
        List<FuelTransaction> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM fuel_transactions";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                FuelTransaction t = mapRow(rs);
                list.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int add(FuelTransaction t) {
        String sql = """
        INSERT INTO fuel_transactions (
            card_number, tran_date, tran_time, invoice, unit, driver_name, odometer, location_name, city,
            state_prov, fees, item, unit_price, disc_ppu, disc_cost, qty, disc_amt, disc_type, amt, db, currency, employee_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, t.getCardNumber());
            ps.setString(2, t.getTranDate());
            ps.setString(3, t.getTranTime());
            ps.setString(4, t.getInvoice());
            ps.setString(5, t.getUnit());
            ps.setString(6, t.getDriverName());
            ps.setString(7, t.getOdometer());
            ps.setString(8, t.getLocationName());
            ps.setString(9, t.getCity());
            ps.setString(10, t.getStateProv());
            ps.setDouble(11, t.getFees());
            ps.setString(12, t.getItem());
            ps.setDouble(13, t.getUnitPrice());
            ps.setDouble(14, t.getDiscPPU());
            ps.setDouble(15, t.getDiscCost());
            ps.setDouble(16, t.getQty());
            ps.setDouble(17, t.getDiscAmt());
            ps.setString(18, t.getDiscType());
            ps.setDouble(19, t.getAmt());
            ps.setString(20, t.getDb());
            ps.setString(21, t.getCurrency());
            ps.setObject(22, t.getEmployeeId());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            // Handle duplicate
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                return -1; // Duplicate!
            }
            e.printStackTrace();
        }
        return -1;
    }

    // Duplicate logic: Invoice + TranDate + LocationName + Amt
    public boolean exists(String invoice, String tranDate, String locationName, double amt) {
        String sql = """
            SELECT COUNT(*) FROM fuel_transactions
            WHERE LOWER(TRIM(invoice)) = ? AND
                  LOWER(TRIM(tran_date)) = ? AND
                  LOWER(TRIM(location_name)) = ? AND
                  ROUND(amt, 2) = ROUND(?, 2)
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, invoice.trim().toLowerCase());
            ps.setString(2, tranDate.trim().toLowerCase());
            ps.setString(3, locationName.trim().toLowerCase());
            ps.setDouble(4, amt);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void update(FuelTransaction t) {
        String sql = """
            UPDATE fuel_transactions SET
                card_number=?, tran_date=?, tran_time=?, invoice=?, unit=?, driver_name=?, odometer=?, location_name=?,
                city=?, state_prov=?, fees=?, item=?, unit_price=?, disc_ppu=?, disc_cost=?, qty=?, disc_amt=?,
                disc_type=?, amt=?, db=?, currency=?, employee_id=?
            WHERE id=?
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, t.getCardNumber());
            ps.setString(2, t.getTranDate());
            ps.setString(3, t.getTranTime());
            ps.setString(4, t.getInvoice());
            ps.setString(5, t.getUnit());
            ps.setString(6, t.getDriverName());
            ps.setString(7, t.getOdometer());
            ps.setString(8, t.getLocationName());
            ps.setString(9, t.getCity());
            ps.setString(10, t.getStateProv());
            ps.setDouble(11, t.getFees());
            ps.setString(12, t.getItem());
            ps.setDouble(13, t.getUnitPrice());
            ps.setDouble(14, t.getDiscPPU());
            ps.setDouble(15, t.getDiscCost());
            ps.setDouble(16, t.getQty());
            ps.setDouble(17, t.getDiscAmt());
            ps.setString(18, t.getDiscType());
            ps.setDouble(19, t.getAmt());
            ps.setString(20, t.getDb());
            ps.setString(21, t.getCurrency());
            ps.setObject(22, t.getEmployeeId());
            ps.setInt(23, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM fuel_transactions WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all fuel transactions for a given driver name and/or truck unit and date range (inclusive).
     * If driverName or truckUnit is null, match all.
     */
    public List<FuelTransaction> getByDriverAndDateRange(String driverName, String truckUnit, String startDate, String endDate) {
        List<FuelTransaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM fuel_transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (driverName != null && !driverName.isBlank()) {
            sql.append(" AND driver_name = ?");
            params.add(driverName);
        }
        if (truckUnit != null && !truckUnit.isBlank()) {
            sql.append(" AND unit = ?");
            params.add(truckUnit);
        }
        if (startDate != null) {
            sql.append(" AND tran_date >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND tran_date <= ?");
            params.add(endDate);
        }
        sql.append(" ORDER BY tran_date ASC");

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); ++i)
                ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FuelTransaction t = mapRow(rs);
                list.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Overload for getByDriverAndDateRange with LocalDate, returns all transactions in date range.
     */
    public List<FuelTransaction> getByDriverAndDateRange(String driverName, String truckUnit, java.time.LocalDate start, java.time.LocalDate end) {
        String startDate = start != null ? start.toString() : null;
        String endDate = end != null ? end.toString() : null;
        return getByDriverAndDateRange(driverName, truckUnit, startDate, endDate);
    }

    private FuelTransaction mapRow(ResultSet rs) throws SQLException {
        return new FuelTransaction(
                rs.getInt("id"),
                rs.getString("card_number"),
                rs.getString("tran_date"),
                rs.getString("tran_time"),
                rs.getString("invoice"),
                rs.getString("unit"),
                rs.getString("driver_name"),
                rs.getString("odometer"),
                rs.getString("location_name"),
                rs.getString("city"),
                rs.getString("state_prov"),
                rs.getDouble("fees"),
                rs.getString("item"),
                rs.getDouble("unit_price"),
                rs.getDouble("disc_ppu"),
                rs.getDouble("disc_cost"),
                rs.getDouble("qty"),
                rs.getDouble("disc_amt"),
                rs.getString("disc_type"),
                rs.getDouble("amt"),
                rs.getString("db"),
                rs.getString("currency"),
                rs.getInt("employee_id")
        );
    }
}