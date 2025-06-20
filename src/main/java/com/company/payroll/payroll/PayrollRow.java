package com.company.payroll.payroll;

import com.company.payroll.employees.Employee;

/**
 * Represents a single row (driver) in the payroll table.
 * Demo data for now; real app would fill from DB/logic.
 */
public class PayrollRow {
    private final Employee employee;
    private final boolean locked;

    public PayrollRow(Employee e, boolean locked) { this.employee = e; this.locked = locked; }

    public String getDriverName() { return employee.getName(); }
    public double getGross() { return 3150.00; } // stub
    public String getGrossFormatted() { return formatCurrency(getGross()); }

    public double getServiceFeePercent() { return employee.getServiceFeePercent(); }
    public double getNetAfterServiceFee() { return 2950.00; }
    public String getNetAfterServiceFeeFormatted() { return formatCurrency(getNetAfterServiceFee()); }

    public double getFuelDeducted() { return 850.00; }
    public String getFuelDeductedFormatted() { return formatCurrency(getFuelDeducted()); }

    public double getNetAfterFuel() { return 2100.00; }
    public String getNetAfterFuelFormatted() { return formatCurrency(getNetAfterFuel()); }

    public double getDriverPay() { return 1680.00; }
    public String getDriverPayFormatted() { return formatCurrency(getDriverPay()); }

    public double getCompanyPay() { return 420.00; }
    public String getCompanyPayFormatted() { return formatCurrency(getCompanyPay()); }

    public double getAdjustments() { return -100.00; }
    public String getAdjustmentsFormatted() { return formatCurrency(getAdjustments()); }

    public double getRecurringFees() { return 110.00; }
    public String getRecurringFeesFormatted() { return formatCurrency(getRecurringFees()); }

    public double getAdvanceRepayment() { return 50.00; }
    public String getAdvanceRepaymentFormatted() { return formatCurrency(getAdvanceRepayment()); }

    public double getNetPay() { return 1530.00; }
    public String getNetPayFormatted() { return formatCurrency(getNetPay()); }

    public boolean isLocked() { return locked; }

    private String formatCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }

    // Demo stub row
    public static PayrollRow demo(Employee emp, boolean locked) {
        return new PayrollRow(emp, locked);
    }
}