package com.company.payroll.payroll;

import com.company.payroll.employees.Employee;
import com.company.payroll.loads.Load;
import com.company.payroll.fuel.FuelTransaction;
import com.company.payroll.feesadvances.FeesAdvancesTab;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents the payroll calculation for a single driver for a given period.
 */
public class PayrollEntry {
    private final Employee driver;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final List<Load> loads;
    private final List<FuelTransaction> fuelTransactions;
    private final List<FeesAdvancesTab.FeeEntry> feeDeductions;
    private final List<FeesAdvancesTab.CashAdvanceEntry> cashAdvances;

    private final double grossPay;
    private final double totalFuel;
    private final double totalFees;
    private final double totalAdvances;
    private final double netPay;

    public PayrollEntry(
            Employee driver,
            LocalDate periodStart,
            LocalDate periodEnd,
            List<Load> loads,
            List<FuelTransaction> fuelTransactions,
            List<FeesAdvancesTab.FeeEntry> feeDeductions,
            List<FeesAdvancesTab.CashAdvanceEntry> cashAdvances,
            double grossPay,
            double totalFuel,
            double totalFees,
            double totalAdvances,
            double netPay
    ) {
        this.driver = driver;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.loads = loads;
        this.fuelTransactions = fuelTransactions;
        this.feeDeductions = feeDeductions;
        this.cashAdvances = cashAdvances;
        this.grossPay = grossPay;
        this.totalFuel = totalFuel;
        this.totalFees = totalFees;
        this.totalAdvances = totalAdvances;
        this.netPay = netPay;
    }

    public Employee getDriver() { return driver; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public List<Load> getLoads() { return loads; }
    public List<FuelTransaction> getFuelTransactions() { return fuelTransactions; }
    public List<FeesAdvancesTab.FeeEntry> getFeeDeductions() { return feeDeductions; }
    public List<FeesAdvancesTab.CashAdvanceEntry> getCashAdvances() { return cashAdvances; }
    public double getGrossPay() { return grossPay; }
    public double getTotalFuel() { return totalFuel; }
    public double getTotalFees() { return totalFees; }
    public double getTotalAdvances() { return totalAdvances; }
    public double getNetPay() { return netPay; }
}