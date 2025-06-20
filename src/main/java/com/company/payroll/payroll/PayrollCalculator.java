package com.company.payroll.payroll;

import com.company.payroll.employees.Employee;
import com.company.payroll.employees.EmployeeDAO;
import com.company.payroll.feesadvances.FeeAdvancesDAO;
import com.company.payroll.feesadvances.FeesAdvancesTab;
import com.company.payroll.fuel.FuelTransaction;
import com.company.payroll.fuel.FuelTransactionDAO;
import com.company.payroll.loads.Load;
import com.company.payroll.loads.LoadDAO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for aggregating and calculating payroll for drivers.
 */
public class PayrollCalculator {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final LoadDAO loadDAO = new LoadDAO();
    private final FuelTransactionDAO fuelDAO = new FuelTransactionDAO();
    private final FeeAdvancesDAO feeAdvancesDAO = new FeeAdvancesDAO();

    /**
     * Returns payroll calculations for the given payroll period (inclusive).
     * If driversFilter is not null/empty, only those drivers are processed.
     */
    public List<PayrollEntry> calculatePayroll(
            LocalDate periodStart,
            LocalDate periodEnd,
            List<Employee> driversFilter // null or empty = all drivers
    ) {
        List<Employee> drivers = (driversFilter == null || driversFilter.isEmpty())
                ? employeeDAO.getActive()
                : driversFilter;

        List<PayrollEntry> result = new ArrayList<>();

        for (Employee driver : drivers) {
            // 1. Loads for driver in period
            List<Load> loads = loadDAO.getByDriverAndDateRange(driver.getId(), periodStart, periodEnd);

            // 2. Gross pay for loads (driver percent * load amount)
            double grossPay = loads.stream()
                    .mapToDouble(load -> {
                        double percent = driver.getDriverPercent() / 100.0;
                        return load.getAmount() * percent;
                    }).sum();

            // 3. Fuel transactions for driver/unit in period
            List<FuelTransaction> fuelTx = fuelDAO.getByDriverAndDateRange(driver.getName(), driver.getTruckId(), periodStart, periodEnd);
            double totalFuel = fuelTx.stream().mapToDouble(FuelTransaction::getAmt).sum();

            // 4. Recurring fees for driver and period (month/year match or date in range)
            List<FeesAdvancesTab.FeeEntry> fees = feeAdvancesDAO.getAllFees().stream()
                    .filter(fee -> fee.getDriver().getId() == driver.getId()
                            && !fee.getStartDate().isAfter(periodEnd)
                            && fee.isActive()
                            && (fee.getFeeMonth() == periodStart.getMonthValue() && fee.getFeeYear() == periodStart.getYear()))
                    .collect(Collectors.toList());
            double totalFees = fees.stream().mapToDouble(FeesAdvancesTab.FeeEntry::getAmount).sum();

            // 5. Cash advances for driver, given during this period and still active
            List<FeesAdvancesTab.CashAdvanceEntry> advances = feeAdvancesDAO.getAllCashAdvances().stream()
                    .filter(ca -> ca.getDriver().getId() == driver.getId()
                            && !ca.getGivenDate().isAfter(periodEnd)
                            && ca.isActive())
                    .collect(Collectors.toList());
            double totalAdvances = advances.stream().mapToDouble(FeesAdvancesTab.CashAdvanceEntry::getAmount).sum();

            // 6. Net pay
            double netPay = grossPay - totalFuel - totalFees - totalAdvances;

            PayrollEntry entry = new PayrollEntry(
                    driver,
                    periodStart,
                    periodEnd,
                    loads,
                    fuelTx,
                    fees,
                    advances,
                    grossPay,
                    totalFuel,
                    totalFees,
                    totalAdvances,
                    netPay
            );
            result.add(entry);
        }
        return result;
    }
}