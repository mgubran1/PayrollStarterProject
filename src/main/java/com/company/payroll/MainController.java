package com.company.payroll;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import com.company.payroll.employees.EmployeesTab;
import com.company.payroll.loads.LoadsTab;
import com.company.payroll.fuel.FuelImportTab;
import com.company.payroll.payroll.PayrollTab;
import com.company.payroll.feesadvances.FeesAdvancesTab; // <-- Import your new tab

public class MainController {
    private TabPane tabPane;

    public MainController() {
        tabPane = new TabPane();

        // Instantiate EmployeesTab first
        EmployeesTab employeesTabContent = new EmployeesTab();
        Tab employeesTab = new Tab("Employees", employeesTabContent);
        employeesTab.setClosable(false);

        // Pass EmployeesTab instance to LoadsTab
        LoadsTab loadsTabContent = new LoadsTab(employeesTabContent);
        Tab loadsTab = new Tab("Loads", loadsTabContent);
        loadsTab.setClosable(false);

        Tab fuelImportTab = new Tab("Fuel Import", new FuelImportTab());
        fuelImportTab.setClosable(false);

        Tab payrollTab = new Tab("Payroll", new PayrollTab());
        payrollTab.setClosable(false);

        Tab feesAdvancesTab = new Tab("Fees & Advances", new FeesAdvancesTab());
        feesAdvancesTab.setClosable(false);

        tabPane.getTabs().addAll(employeesTab, loadsTab, fuelImportTab, feesAdvancesTab, payrollTab);
    }

    public TabPane getTabPane() {
        return tabPane;
    }
}