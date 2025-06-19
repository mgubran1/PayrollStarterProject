package com.company.payroll.fuel;

import javafx.scene.Node;
import com.company.payroll.employees.Employee;
import com.company.payroll.employees.EmployeeDAO;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class FuelImportTab extends BorderPane {

    private final FuelTransactionDAO dao = new FuelTransactionDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final ObservableList<FuelTransaction> data = FXCollections.observableArrayList();

    public FuelImportTab() {
        setPadding(new Insets(10));

        TableView<FuelTransaction> table = makeTable();

        Button addBtn = new Button("Add");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        Button importBtn = new Button("Import CSV/XLSX");
        Button refreshBtn = new Button("Refresh");

        HBox actions = new HBox(12, addBtn, editBtn, deleteBtn, importBtn, refreshBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8,0,10,0));

        // Actions
        addBtn.setOnAction(e -> showEditDialog(null, true));
        editBtn.setOnAction(e -> {
            FuelTransaction t = table.getSelectionModel().getSelectedItem();
            if (t != null) showEditDialog(t, false);
        });
        deleteBtn.setOnAction(e -> {
            FuelTransaction t = table.getSelectionModel().getSelectedItem();
            if (t != null) {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete invoice " + t.getInvoice() + "?", ButtonType.YES, ButtonType.NO);
                a.setHeaderText("Confirm Delete");
                a.showAndWait().ifPresent(b -> {
                    if (b == ButtonType.YES) {
                        dao.delete(t.getId());
                        reload();
                    }
                });
            }
        });
        refreshBtn.setOnAction(e -> reload());

        importBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Import Fuel Transactions");
            fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );
            File file = fc.showOpenDialog(getScene().getWindow());
            if (file != null) {
                int imported = 0, skipped = 0;
                String lower = file.getName().toLowerCase();
                try {
                    List<FuelTransaction> importedList = null;
                    if (lower.endsWith(".csv")) {
                        importedList = parseCSV(file);
                    } else if (lower.endsWith(".xlsx")) {
                        importedList = parseXLSX(file); // Need Apache POI
                    }
                    if (importedList != null && !importedList.isEmpty()) {
                        for (FuelTransaction tx : importedList) {
                            if (dao.exists(
                                    tx.getInvoice(),
                                    tx.getTranDate(),
                                    tx.getLocationName(),
                                    tx.getAmt())) {
                                skipped++;
                                continue;
                            }
                            dao.add(tx);
                            imported++;
                        }
                        reload();
                        new Alert(Alert.AlertType.INFORMATION,
                                "Import complete!\nImported: " + imported + "\nSkipped (duplicates): " + skipped).showAndWait();
                    }
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Import failed: " + ex.getMessage()).showAndWait();
                }
            }
        });

        setTop(actions);
        setCenter(table);
        reload();
    }

    private void reload() {
        data.setAll(dao.getAll());
    }

    private TableView<FuelTransaction> makeTable() {
        TableView<FuelTransaction> table = new TableView<>(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<FuelTransaction, String> colDate = new TableColumn<>("Tran Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("tranDate"));
        TableColumn<FuelTransaction, String> colInv = new TableColumn<>("Invoice");
        colInv.setCellValueFactory(new PropertyValueFactory<>("invoice"));
        TableColumn<FuelTransaction, String> colUnit = new TableColumn<>("Unit");
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        TableColumn<FuelTransaction, String> colDriver = new TableColumn<>("Driver Name");
        colDriver.setCellValueFactory(new PropertyValueFactory<>("driverName"));
        TableColumn<FuelTransaction, String> colLoc = new TableColumn<>("Location Name");
        colLoc.setCellValueFactory(new PropertyValueFactory<>("locationName"));
        TableColumn<FuelTransaction, Number> colAmt = new TableColumn<>("Amt");
        colAmt.setCellValueFactory(new PropertyValueFactory<>("amt"));
        TableColumn<FuelTransaction, Number> colFees = new TableColumn<>("Fees");
        colFees.setCellValueFactory(new PropertyValueFactory<>("fees"));

        table.getColumns().addAll(colDate, colInv, colUnit, colDriver, colLoc, colAmt, colFees);
        return table;
    }

    private void showEditDialog(FuelTransaction t, boolean isAdd) {
        Dialog<FuelTransaction> dialog = new Dialog<>();
        dialog.setTitle(isAdd ? "Add Fuel Transaction" : "Edit Fuel Transaction");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField dateField = new TextField(t != null ? t.getTranDate() : "");
        TextField invField = new TextField(t != null ? t.getInvoice() : "");
        TextField unitField = new TextField(t != null ? t.getUnit() : "");
        TextField driverField = new TextField(t != null ? t.getDriverName() : "");
        TextField locField = new TextField(t != null ? t.getLocationName() : "");
        TextField amtField = new TextField(t != null ? String.valueOf(t.getAmt()) : "");
        TextField feesField = new TextField(t != null ? String.valueOf(t.getFees()) : "");

        GridPane grid = new GridPane();
        grid.setVgap(8); grid.setHgap(10); grid.setPadding(new Insets(10));
        grid.add(new Label("Tran Date:"), 0, 0); grid.add(dateField, 1, 0);
        grid.add(new Label("Invoice:"), 0, 1); grid.add(invField, 1, 1);
        grid.add(new Label("Unit:"), 0, 2); grid.add(unitField, 1, 2);
        grid.add(new Label("Driver Name:"), 0, 3); grid.add(driverField, 1, 3);
        grid.add(new Label("Location Name:"), 0, 4); grid.add(locField, 1, 4);
        grid.add(new Label("Amt:"), 0, 5); grid.add(amtField, 1, 5);
        grid.add(new Label("Fees:"), 0, 6); grid.add(feesField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(isAdd && invField.getText().trim().isEmpty());

        invField.textProperty().addListener((obs, oldV, newV) -> {
            okBtn.setDisable(newV.trim().isEmpty());
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String tranDate = dateField.getText().trim();
                String invoice = invField.getText().trim();
                String unit = unitField.getText().trim();
                String driver = driverField.getText().trim();
                String location = locField.getText().trim();
                double amt = parseDouble(amtField.getText());
                double fees = parseDouble(feesField.getText());

                // Attempt to match employee
                int employeeId = 0;
                List<Employee> matches = employeeDAO.getAll().stream()
                    .filter(e -> e.getName().equalsIgnoreCase(driver) && e.getTruckId().equalsIgnoreCase(unit))
                    .collect(Collectors.toList());
                if (!matches.isEmpty()) employeeId = matches.get(0).getId();

                FuelTransaction tx = new FuelTransaction(
                    t == null ? 0 : t.getId(), "", tranDate, "", invoice, unit, driver,
                    "", location, "", "", fees, "", 0, 0, 0, 0, 0, "", amt, "", "", employeeId
                );
                if (isAdd) dao.add(tx);
                else dao.update(tx);
                reload();
                return tx;
            }
            return null;
        });

        dialog.showAndWait();
    }

    // CSV Import
    private List<FuelTransaction> parseCSV(File file) throws IOException {
        List<FuelTransaction> list = new ArrayList<>();
        List<Employee> employees = employeeDAO.getAll();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String[] headers = br.readLine().split(",");
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < headers.length; i++)
                map.put(headers[i].trim().toLowerCase(), i);
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split(",", -1);
                String invoice = getVal(arr, map, "invoice");
                if (invoice.isEmpty()) continue; // must have invoice

                String driverName = getVal(arr, map, "driver name");
                String unit = getVal(arr, map, "unit");

                // Match employee by name and unit
                int employeeId = 0;
                for (Employee e : employees) {
                    if (e.getName().equalsIgnoreCase(driverName) && e.getTruckId().equalsIgnoreCase(unit)) {
                        employeeId = e.getId();
                        break;
                    }
                }

                FuelTransaction t = new FuelTransaction(
                    0,
                    getVal(arr, map, "card #"),
                    getVal(arr, map, "tran date"),
                    getVal(arr, map, "tran time"),
                    invoice,
                    unit,
                    driverName,
                    getVal(arr, map, "odometer"),
                    getVal(arr, map, "location name"),
                    getVal(arr, map, "city"),
                    getVal(arr, map, "state/ prov"),
                    parseDouble(getVal(arr, map, "fees")),
                    getVal(arr, map, "item"),
                    parseDouble(getVal(arr, map, "unit price")),
                    parseDouble(getVal(arr, map, "disc ppu")),
                    parseDouble(getVal(arr, map, "disc cost")),
                    parseDouble(getVal(arr, map, "qty")),
                    parseDouble(getVal(arr, map, "disc amt")),
                    getVal(arr, map, "disc type"),
                    parseDouble(getVal(arr, map, "amt")),
                    getVal(arr, map, "db"),
                    getVal(arr, map, "currency"),
                    employeeId
                );
                list.add(t);
            }
        }
        return list;
    }

    // --- XLSX parsing stub (implement using Apache POI if needed) ---
    private List<FuelTransaction> parseXLSX(File file) throws IOException {
        throw new IOException("XLSX import not implemented (ask for code if you want it!)");
    }

    // --- Helpers ---
    private String getVal(String[] arr, Map<String, Integer> map, String key) {
        Integer idx = map.get(key.toLowerCase());
        if (idx == null || idx >= arr.length) return "";
        return arr[idx].trim();
    }
    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
}
