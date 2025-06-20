package com.company.payroll.payroll;

import com.company.payroll.employees.Employee;
import com.company.payroll.employees.EmployeeDAO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main Payroll Tab: Select week, view driver settlements, export, etc.
 */
public class PayrollTab extends BorderPane {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final PayrollCalculator calculator = new PayrollCalculator();

    private ObservableList<Employee> allDrivers = FXCollections.observableArrayList();
    private ObservableList<PayrollEntry> payrollEntries = FXCollections.observableArrayList();

    // Controls
    private DatePicker weekStartPicker;
    private DatePicker weekEndPicker;
    private ComboBox<Employee> driverFilterBox;
    private TableView<PayrollEntry> table;

    public PayrollTab() {
        allDrivers.setAll(employeeDAO.getActive());

        // --- TOP FILTER CONTROLS ---
        HBox filterBox = new HBox(12);
        filterBox.setPadding(new Insets(14, 10, 12, 10));
        filterBox.setAlignment(Pos.CENTER_LEFT);

        weekStartPicker = new DatePicker(getDefaultWeekStart());
        weekEndPicker = new DatePicker(getDefaultWeekEnd());

        driverFilterBox = new ComboBox<>(allDrivers);
        driverFilterBox.setPromptText("All Drivers");
        driverFilterBox.setMinWidth(160);
        driverFilterBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Employee e) { return e == null ? "" : e.getName(); }
            @Override public Employee fromString(String s) { return null; }
        });
        Button refreshBtn = new Button("Calculate Payroll");
        Button exportBtn = new Button("Export CSV");
        Button copyBtn = new Button("Copy Table");

        filterBox.getChildren().addAll(new Label("Week Start:"), weekStartPicker,
                new Label("End:"), weekEndPicker,
                new Label("Driver:"), driverFilterBox,
                refreshBtn, exportBtn, copyBtn);

        // --- TABLE ---
        table = new TableView<>(payrollEntries);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No payroll data. Click 'Calculate Payroll'."));

        TableColumn<PayrollEntry, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDriver().getName()));

        TableColumn<PayrollEntry, String> unitCol = new TableColumn<>("Truck/Unit");
        unitCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDriver().getTruckUnit()));

        TableColumn<PayrollEntry, Number> grossCol = new TableColumn<>("Gross Pay");
        grossCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getGrossPay()));

        TableColumn<PayrollEntry, Number> fuelCol = new TableColumn<>("Fuel");
        fuelCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getTotalFuel()));

        TableColumn<PayrollEntry, Number> feesCol = new TableColumn<>("Fees");
        feesCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getTotalFees()));

        TableColumn<PayrollEntry, Number> advancesCol = new TableColumn<>("Advances");
        advancesCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getTotalAdvances()));

        TableColumn<PayrollEntry, Number> netCol = new TableColumn<>("Net Pay");
        netCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getNetPay()));

        table.getColumns().addAll(driverCol, unitCol, grossCol, fuelCol, feesCol, advancesCol, netCol);

        // Double-click for details
        table.setRowFactory(tv -> {
            TableRow<PayrollEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showDetailsDialog(row.getItem());
                }
            });
            return row;
        });

        // --- BUTTON ACTIONS ---
        refreshBtn.setOnAction(e -> recalculatePayroll());
        exportBtn.setOnAction(e -> exportToCSV());
        copyBtn.setOnAction(e -> copyTableToClipboard());

        VBox vbox = new VBox(filterBox, table);
        setCenter(vbox);
        setPadding(new Insets(10));
    }

    private void recalculatePayroll() {
        LocalDate start = weekStartPicker.getValue();
        LocalDate end = weekEndPicker.getValue();
        Employee filter = driverFilterBox.getValue();
        List<Employee> selectedDrivers = filter != null ? List.of(filter) : null;
        payrollEntries.setAll(calculator.calculatePayroll(start, end, selectedDrivers));
    }

    private void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Payroll CSV");
        fileChooser.setInitialFileName("payroll_" + weekStartPicker.getValue() + "_to_" + weekEndPicker.getValue() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Driver,Truck/Unit,Gross Pay,Fuel,Fees,Advances,Net Pay");
                for (PayrollEntry entry : payrollEntries) {
                    writer.println(String.join(",",
                            csv(entry.getDriver().getName()),
                            csv(entry.getDriver().getTruckUnit()),
                            csv(entry.getGrossPay()),
                            csv(entry.getTotalFuel()),
                            csv(entry.getTotalFees()),
                            csv(entry.getTotalAdvances()),
                            csv(entry.getNetPay())
                    ));
                }
                writer.flush();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Exported to: " + file.getAbsolutePath());
                a.setHeaderText("CSV Export Complete");
                a.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert a = new Alert(Alert.AlertType.ERROR, "Failed to export: " + ex.getMessage());
                a.setHeaderText("Export Failed");
                a.showAndWait();
            }
        }
    }

    private void copyTableToClipboard() {
        StringBuilder sb = new StringBuilder();
        sb.append("Driver\tTruck/Unit\tGross Pay\tFuel\tFees\tAdvances\tNet Pay\n");
        for (PayrollEntry entry : payrollEntries) {
            sb.append(entry.getDriver().getName()).append('\t')
              .append(entry.getDriver().getTruckUnit()).append('\t')
              .append(entry.getGrossPay()).append('\t')
              .append(entry.getTotalFuel()).append('\t')
              .append(entry.getTotalFees()).append('\t')
              .append(entry.getTotalAdvances()).append('\t')
              .append(entry.getNetPay()).append('\n');
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Payroll table copied to clipboard.");
        a.setHeaderText("Copy Complete");
        a.showAndWait();
    }

    private void showDetailsDialog(PayrollEntry entry) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Payroll Details: " + entry.getDriver().getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

        VBox root = new VBox(9);
        root.setPadding(new Insets(18));

        root.getChildren().add(new Label("Period: " + entry.getPeriodStart() + " to " + entry.getPeriodEnd()));
        root.getChildren().add(new Label("Truck/Unit: " + entry.getDriver().getTruckUnit()));

        // Loads
        TitledPane loadsPane = new TitledPane("Loads (" + entry.getLoads().size() + ")", buildListTable(
                List.of("Load #", "Date", "Amount"),
                entry.getLoads().stream().map(load ->
                        List.of(
                                load.getLoadNumber(),
                                load.getLoadDate() != null ? load.getLoadDate().toString() : "",
                                String.format("%.2f", load.getAmount())
                        )).collect(Collectors.toList())
        ));
        loadsPane.setExpanded(false);

        // Fuel
        TitledPane fuelPane = new TitledPane("Fuel Transactions (" + entry.getFuelTransactions().size() + ")",
                buildListTable(
                        List.of("Date", "Amt", "Odometer", "Location"),
                        entry.getFuelTransactions().stream().map(fuel ->
                                List.of(
                                        fuel.getTranDate(),
                                        String.format("%.2f", fuel.getAmt()),
                                        fuel.getOdometer(),
                                        fuel.getLocationName()
                                )).collect(Collectors.toList())
                ));
        fuelPane.setExpanded(false);

        // Fees
        TitledPane feesPane = new TitledPane("Fees (" + entry.getFeeDeductions().size() + ")",
                buildListTable(
                        List.of("Type", "Amount", "Start Date", "Weeks Left"),
                        entry.getFeeDeductions().stream().map(fee ->
                                List.of(
                                        fee.getFeeType().name(),
                                        String.format("%.2f", fee.getAmount()),
                                        fee.getStartDate().toString(),
                                        String.valueOf(fee.getWeeksRemaining())
                                )).collect(Collectors.toList())
                ));
        feesPane.setExpanded(false);

        // Advances
        TitledPane advPane = new TitledPane("Advances (" + entry.getCashAdvances().size() + ")",
                buildListTable(
                        List.of("Amount", "Given", "Due", "Weeks Left"),
                        entry.getCashAdvances().stream().map(adv ->
                                List.of(
                                        String.format("%.2f", adv.getAmount()),
                                        adv.getGivenDate().toString(),
                                        adv.getDueDate().toString(),
                                        String.valueOf(adv.getWeeksRemaining())
                                )).collect(Collectors.toList())
                ));
        advPane.setExpanded(false);

        root.getChildren().addAll(loadsPane, fuelPane, feesPane, advPane);

        // Summary
        root.getChildren().add(new Separator());
        root.getChildren().add(new Label(String.format(
                "Gross: $%.2f    Fuel: $%.2f    Fees: $%.2f    Advances: $%.2f    Net Pay: $%.2f",
                entry.getGrossPay(), entry.getTotalFuel(), entry.getTotalFees(), entry.getTotalAdvances(), entry.getNetPay()
        )));

        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    // Helper for showing small table as VBox
    private Node buildListTable(List<String> headers, List<List<String>> rows) {
        GridPane grid = new GridPane();
        grid.setVgap(4);
        grid.setHgap(14);
        grid.setPadding(new Insets(5, 0, 8, 0));

        // Headers
        for (int i = 0; i < headers.size(); i++) {
            Label lbl = new Label(headers.get(i));
            lbl.setStyle("-fx-font-weight: bold; -fx-underline: true;");
            grid.add(lbl, i, 0);
        }
        // Rows
        int r = 1;
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                grid.add(new Label(row.get(i)), i, r);
            }
            r++;
        }
        return grid;
    }

    private LocalDate getDefaultWeekStart() {
        LocalDate today = LocalDate.now();
        // Find last Monday
        return today.minusDays((today.getDayOfWeek().getValue() + 6) % 7);
    }

    private LocalDate getDefaultWeekEnd() {
        return getDefaultWeekStart().plusDays(6);
    }

    private String csv(Object o) {
        String s = o == null ? "" : o.toString();
        if (s.contains(",") || s.contains("\"")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}