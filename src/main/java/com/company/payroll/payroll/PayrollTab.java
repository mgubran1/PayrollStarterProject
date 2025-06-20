package com.company.payroll.payroll;

import com.company.payroll.employees.Employee;
import com.company.payroll.employees.EmployeeDAO;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Main container that orchestrates summary, table, and actions panes.
 */
public class PayrollTab extends BorderPane {
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    private final PayrollSummaryPane summaryPane;
    private final PayrollTablePane tablePane;
    private final PayrollActionsPane actionsPane;

    // State for week/year navigation
    private int selectedYear;
    private LocalDate selectedWeekStart;

    public PayrollTab() {
        setPadding(new Insets(16));

        selectedYear = LocalDate.now().getYear();
        selectedWeekStart = getCurrentMonday();

        // Summary (top, including header controls)
        summaryPane = new PayrollSummaryPane(this::onYearChange, this::onCompanyEdit, this::onPrevWeek, this::onNextWeek, this::onWeekChange,
                selectedYear, selectedWeekStart);
        setTop(summaryPane);

        // Table with filter and pagination
        tablePane = new PayrollTablePane();
        setCenter(tablePane);

        // Actions bar (bottom)
        actionsPane = new PayrollActionsPane(this::onLock, this::onExportAll, this::onRefresh);
        setBottom(actionsPane);

        // Initial data load
        refreshPayrollData();
    }

    // Called when year is changed in summary pane
    private void onYearChange(int year) {
        this.selectedYear = year;
        refreshPayrollData();
    }

    // Called when company edit is requested
    private void onCompanyEdit() {
        summaryPane.showEditCompanyDialog();
    }

    // Called when user goes to previous week
    private void onPrevWeek() {
        selectedWeekStart = selectedWeekStart.minusWeeks(1);
        summaryPane.setWeek(selectedWeekStart);
        refreshPayrollData();
    }

    // Called when user goes to next week
    private void onNextWeek() {
        selectedWeekStart = selectedWeekStart.plusWeeks(1);
        summaryPane.setWeek(selectedWeekStart);
        refreshPayrollData();
    }

    // Called when week date is changed in summary pane
    private void onWeekChange(LocalDate monday) {
        this.selectedWeekStart = monday;
        refreshPayrollData();
    }

    // Called when lock button is pressed
    private void onLock() {
        summaryPane.toggleLock();
        refreshPayrollData();
    }

    // Called when export all is pressed
    private void onExportAll() {
        tablePane.exportAllPaystubs();
    }

    // Called when refresh is pressed
    private void onRefresh() {
        refreshPayrollData();
    }

    // Loads and refreshes all payroll data for the selected year and week
    public void refreshPayrollData() {
        // Get drivers for the week (stub: use all for now)
        List<Employee> employees = employeeDAO.getAll();

        // Create PayrollRows (stub/demo logic)
        boolean isLocked = summaryPane.isLocked();
        List<PayrollRow> rows = employees.stream()
                .map(emp -> PayrollRow.demo(emp, isLocked))
                .toList();

        // Update tablePane and summaryPane
        tablePane.loadRows(rows);
        summaryPane.updateSummary(rows);
    }

    // Utility: Get current week's Monday
    private static LocalDate getCurrentMonday() {
        LocalDate today = LocalDate.now();
        return today.with(DayOfWeek.MONDAY);
    }
}