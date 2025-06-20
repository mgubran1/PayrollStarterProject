package com.company.payroll.feesadvances;

import com.company.payroll.employees.Employee;
import com.company.payroll.employees.EmployeeDAO;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FeeAdvancesDAO {

    private static final String DB_URL = "jdbc:sqlite:payroll.db";
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    public FeeAdvancesDAO() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // --- Recurring Fees Table ---
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS recurring_fees (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    driver_id INTEGER NOT NULL,
                    fee_type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    start_date DATE NOT NULL,
                    total_weeks INTEGER NOT NULL,
                    weeks_remaining INTEGER NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1,
                    fee_month INTEGER,
                    fee_year INTEGER,
                    FOREIGN KEY(driver_id) REFERENCES employees(id)
                );
            """);
            // --- Cash Advances Table ---
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS cash_advances (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    driver_id INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    given_date DATE NOT NULL,
                    due_date DATE NOT NULL,
                    payment_weeks INTEGER NOT NULL,
                    weeks_remaining INTEGER NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(driver_id) REFERENCES employees(id)
                );
            """);
            // Add columns fee_month and fee_year if DB already exists
            addColumnIfNotExists(conn, "recurring_fees", "fee_month", "INTEGER");
            addColumnIfNotExists(conn, "recurring_fees", "fee_year", "INTEGER");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addColumnIfNotExists(Connection conn, String table, String column, String type) throws SQLException {
        DatabaseMetaData dbm = conn.getMetaData();
        ResultSet rs = dbm.getColumns(null, null, table, column);
        if (!rs.next()) {
            conn.createStatement().execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
        rs.close();
    }

    // ------------- FEES CRUD --------------

    public List<FeesAdvancesTab.FeeEntry> getAllFees() {
        List<FeesAdvancesTab.FeeEntry> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM recurring_fees");
            while (rs.next()) {
                int id = rs.getInt("id");
                int driverId = rs.getInt("driver_id");
                Employee driver = employeeDAO.getById(driverId);
                FeesAdvancesTab.FeeType feeType = FeesAdvancesTab.FeeType.valueOf(rs.getString("fee_type"));
                double amount = rs.getDouble("amount");
                LocalDate startDate = rs.getDate("start_date").toLocalDate();
                int totalWeeks = rs.getInt("total_weeks");
                int weeksRemaining = rs.getInt("weeks_remaining");
                boolean active = rs.getInt("active") == 1;
                int feeMonth = rs.getObject("fee_month") != null ? rs.getInt("fee_month") : startDate.getMonthValue();
                int feeYear = rs.getObject("fee_year") != null ? rs.getInt("fee_year") : startDate.getYear();
                FeesAdvancesTab.FeeEntry entry = new FeesAdvancesTab.FeeEntry(
                    id, driver, feeType, amount, totalWeeks, weeksRemaining, startDate, active, feeMonth, feeYear
                );
                list.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Returns true if a fee exists for driver, feeType, month, year (for batch duplicate prevention)
     */
    public boolean feeExists(int driverId, FeesAdvancesTab.FeeType feeType, int feeMonth, int feeYear) {
        String sql = "SELECT COUNT(*) FROM recurring_fees WHERE driver_id=? AND fee_type=? AND fee_month=? AND fee_year=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverId);
            ps.setString(2, feeType.name());
            ps.setInt(3, feeMonth);
            ps.setInt(4, feeYear);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addFee(FeesAdvancesTab.FeeEntry fee) {
        String sql = "INSERT INTO recurring_fees (driver_id, fee_type, amount, start_date, total_weeks, weeks_remaining, active, fee_month, fee_year) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, fee.getDriver().getId());
            ps.setString(2, fee.getFeeType().name());
            ps.setDouble(3, fee.getAmount());
            ps.setDate(4, java.sql.Date.valueOf(fee.getStartDate()));
            ps.setInt(5, fee.getTotalWeeks());
            ps.setInt(6, fee.getWeeksRemaining());
            ps.setInt(7, fee.isActive() ? 1 : 0);
            ps.setInt(8, fee.getFeeMonth());
            ps.setInt(9, fee.getFeeYear());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateFee(FeesAdvancesTab.FeeEntry fee, int id) {
        String sql = "UPDATE recurring_fees SET driver_id=?, fee_type=?, amount=?, start_date=?, total_weeks=?, weeks_remaining=?, active=?, fee_month=?, fee_year=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, fee.getDriver().getId());
            ps.setString(2, fee.getFeeType().name());
            ps.setDouble(3, fee.getAmount());
            ps.setDate(4, java.sql.Date.valueOf(fee.getStartDate()));
            ps.setInt(5, fee.getTotalWeeks());
            ps.setInt(6, fee.getWeeksRemaining());
            ps.setInt(7, fee.isActive() ? 1 : 0);
            ps.setInt(8, fee.getFeeMonth());
            ps.setInt(9, fee.getFeeYear());
            ps.setInt(10, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteFee(int id) {
        String sql = "DELETE FROM recurring_fees WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------- CASH ADVANCES CRUD --------------

    public List<FeesAdvancesTab.CashAdvanceEntry> getAllCashAdvances() {
        List<FeesAdvancesTab.CashAdvanceEntry> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM cash_advances");
            while (rs.next()) {
                int id = rs.getInt("id");
                int driverId = rs.getInt("driver_id");
                Employee driver = employeeDAO.getById(driverId);
                double amount = rs.getDouble("amount");
                LocalDate givenDate = rs.getDate("given_date").toLocalDate();
                LocalDate dueDate = rs.getDate("due_date").toLocalDate();
                int paymentWeeks = rs.getInt("payment_weeks");
                int weeksRemaining = rs.getInt("weeks_remaining");
                boolean active = rs.getInt("active") == 1;
                FeesAdvancesTab.CashAdvanceEntry entry = new FeesAdvancesTab.CashAdvanceEntry(
                    id, driver, amount, givenDate, dueDate, paymentWeeks, weeksRemaining, active
                );
                list.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addCashAdvance(FeesAdvancesTab.CashAdvanceEntry advance) {
        String sql = "INSERT INTO cash_advances (driver_id, amount, given_date, due_date, payment_weeks, weeks_remaining, active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, advance.getDriver().getId());
            ps.setDouble(2, advance.getAmount());
            ps.setDate(3, java.sql.Date.valueOf(advance.getGivenDate()));
            ps.setDate(4, java.sql.Date.valueOf(advance.getDueDate()));
            ps.setInt(5, advance.getPaymentWeeks());
            ps.setInt(6, advance.getWeeksRemaining());
            ps.setInt(7, advance.isActive() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateCashAdvance(FeesAdvancesTab.CashAdvanceEntry advance, int id) {
        String sql = "UPDATE cash_advances SET driver_id=?, amount=?, given_date=?, due_date=?, payment_weeks=?, weeks_remaining=?, active=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, advance.getDriver().getId());
            ps.setDouble(2, advance.getAmount());
            ps.setDate(3, java.sql.Date.valueOf(advance.getGivenDate()));
            ps.setDate(4, java.sql.Date.valueOf(advance.getDueDate()));
            ps.setInt(5, advance.getPaymentWeeks());
            ps.setInt(6, advance.getWeeksRemaining());
            ps.setInt(7, advance.isActive() ? 1 : 0);
            ps.setInt(8, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteCashAdvance(int id) {
        String sql = "DELETE FROM cash_advances WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ----------------- ADVANCED SEARCH FOR FEES ---------------------

    public List<FeesAdvancesTab.FeeEntry> searchFees(Integer driverId, Integer month, Integer year) {
        List<FeesAdvancesTab.FeeEntry> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM recurring_fees WHERE 1=1");
        if (driverId != null) sb.append(" AND driver_id=").append(driverId);
        if (month != null) sb.append(" AND fee_month=").append(month);
        if (year != null) sb.append(" AND fee_year=").append(year);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ResultSet rs = conn.createStatement().executeQuery(sb.toString());
            while (rs.next()) {
                int id = rs.getInt("id");
                Employee driver = employeeDAO.getById(rs.getInt("driver_id"));
                FeesAdvancesTab.FeeType feeType = FeesAdvancesTab.FeeType.valueOf(rs.getString("fee_type"));
                double amount = rs.getDouble("amount");
                LocalDate startDate = rs.getDate("start_date").toLocalDate();
                int totalWeeks = rs.getInt("total_weeks");
                int weeksRemaining = rs.getInt("weeks_remaining");
                boolean active = rs.getInt("active") == 1;
                int feeMonth = rs.getObject("fee_month") != null ? rs.getInt("fee_month") : startDate.getMonthValue();
                int feeYear = rs.getObject("fee_year") != null ? rs.getInt("fee_year") : startDate.getYear();
                FeesAdvancesTab.FeeEntry entry = new FeesAdvancesTab.FeeEntry(
                        id, driver, feeType, amount, totalWeeks, weeksRemaining, startDate, active, feeMonth, feeYear
                );
                list.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ----------------- ADVANCED SEARCH FOR CASH ADVANCES ---------------------

    public List<FeesAdvancesTab.CashAdvanceEntry> searchCashAdvances(Integer driverId, LocalDate from, LocalDate to) {
        List<FeesAdvancesTab.CashAdvanceEntry> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM cash_advances WHERE 1=1");
        if (driverId != null) sb.append(" AND driver_id=").append(driverId);
        if (from != null) sb.append(" AND given_date>='").append(from).append("'");
        if (to != null) sb.append(" AND given_date<='").append(to).append("'");
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ResultSet rs = conn.createStatement().executeQuery(sb.toString());
            while (rs.next()) {
                int id = rs.getInt("id");
                Employee driver = employeeDAO.getById(rs.getInt("driver_id"));
                double amount = rs.getDouble("amount");
                LocalDate givenDate = rs.getDate("given_date").toLocalDate();
                LocalDate dueDate = rs.getDate("due_date").toLocalDate();
                int paymentWeeks = rs.getInt("payment_weeks");
                int weeksRemaining = rs.getInt("weeks_remaining");
                boolean active = rs.getInt("active") == 1;
                FeesAdvancesTab.CashAdvanceEntry entry = new FeesAdvancesTab.CashAdvanceEntry(
                        id, driver, amount, givenDate, dueDate, paymentWeeks, weeksRemaining, active
                );
                list.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}