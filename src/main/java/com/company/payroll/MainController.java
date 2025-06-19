package com.company.payroll;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import com.company.payroll.employees.EmployeesTab;
import com.company.payroll.loads.LoadsTab;
import com.company.payroll.fuel.FuelImportTab; // <-- use this, not FuelTab
import com.company.payroll.payroll.PayrollTab;

public class MainController {
    private TabPane tabPane;

    public MainController() {
        tabPane = new TabPane();

        Tab employeesTab = new Tab("Employees", new EmployeesTab());
        employeesTab.setClosable(false);

        Tab loadsTab = new Tab("Loads", new LoadsTab());
        loadsTab.setClosable(false);

        Tab fuelImportTab = new Tab("Fuel Import", new FuelImportTab());
        fuelImportTab.setClosable(false);

        Tab payrollTab = new Tab("Payroll", new PayrollTab());
        payrollTab.setClosable(false);

        tabPane.getTabs().addAll(employeesTab, loadsTab, fuelImportTab, payrollTab);
    }

    public TabPane getTabPane() {
        return tabPane;
    }
}
