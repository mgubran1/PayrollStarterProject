package com.company.payroll.loads;

import com.company.payroll.employees.Employee;
import com.company.payroll.employees.EmployeeDAO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LoadsTab extends BorderPane {

    private final LoadDAO loadDAO = new LoadDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private ObservableList<Load> allLoads = FXCollections.observableArrayList();
    private ObservableList<Employee> allDrivers = FXCollections.observableArrayList();

    private final List<StatusTab> statusTabs = new ArrayList<>();

    private enum LoadTabStatus {
        BOOKED, IN_TRANSIT, DELIVERED, PAID, CANCELLED, ALL, SEARCH
    }

    public LoadsTab() {
        // Load all data
        reloadAll();

        // Create main tab pane for each load status
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Main status tabs
        statusTabs.add(makeStatusTab("Booked", Load.Status.BOOKED, LoadTabStatus.BOOKED));
        statusTabs.add(makeStatusTab("In-Transit", Load.Status.IN_TRANSIT, LoadTabStatus.IN_TRANSIT));
        statusTabs.add(makeStatusTab("Delivered", Load.Status.DELIVERED, LoadTabStatus.DELIVERED));
        statusTabs.add(makeStatusTab("Paid", Load.Status.PAID, LoadTabStatus.PAID));
        statusTabs.add(makeStatusTab("Cancelled", Load.Status.CANCELLED, LoadTabStatus.CANCELLED));

        // "All" tab (shows all loads)
        statusTabs.add(makeStatusTab("All", null, LoadTabStatus.ALL));

        // "Advanced Search" tab
        statusTabs.add(makeSearchTab());

        // Add tabs to TabPane
        for (StatusTab sTab : statusTabs) {
            tabs.getTabs().add(sTab.tab);
        }

        setCenter(tabs);
    }

    // Helper class for holding tab and table
    private static class StatusTab {
        Tab tab;
        TableView<Load> table;
        FilteredList<Load> filteredList;
    }

    // Main per-status tab
    private StatusTab makeStatusTab(String title, Load.Status filterStatus, LoadTabStatus loadTabStatus) {
        StatusTab statusTab = new StatusTab();

        // Filtered list for this tab
        statusTab.filteredList = new FilteredList<>(allLoads, l -> filterStatus == null || l.getStatus() == filterStatus);
        TableView<Load> table = makeTableView(statusTab.filteredList);

        // Export, Add, Edit, Delete, Bulk Status Update, Refresh
        Button addBtn = new Button("Add");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        Button exportBtn = new Button("Export CSV");
        Button refreshBtn = new Button("Refresh");
        Button bulkStatusBtn = new Button("Bulk Status Update");

        HBox buttonBox = new HBox(10, addBtn, editBtn, deleteBtn, bulkStatusBtn, exportBtn, refreshBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10, 10, 5, 10));

        // Add/Edit/Delete/Refresh actions
        addBtn.setOnAction(e -> showLoadDialog(null, true));
        editBtn.setOnAction(e -> {
            Load selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showLoadDialog(selected, false);
        });
        deleteBtn.setOnAction(e -> {
            Load selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete load \"" + selected.getLoadNumber() + "\"?",
                        ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Confirm Delete");
                confirm.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.YES) {
                        loadDAO.delete(selected.getId());
                        reloadAll();
                    }
                });
            }
        });
        exportBtn.setOnAction(e -> exportCSV(table));
        refreshBtn.setOnAction(e -> reloadAll());

        // Bulk status update
        bulkStatusBtn.setOnAction(e -> {
            List<Load> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) return;
            ChoiceDialog<Load.Status> dlg = new ChoiceDialog<>(Load.Status.BOOKED, Load.Status.values());
            dlg.setTitle("Bulk Status Update");
            dlg.setHeaderText("Change status for " + selected.size() + " loads");
            dlg.setContentText("New status:");
            dlg.showAndWait().ifPresent(newStatus -> {
                for (Load l : selected) {
                    l.setStatus(newStatus);
                    loadDAO.update(l);
                }
                reloadAll();
            });
        });

        // Double-click for edit
        table.setRowFactory(tv -> {
            TableRow<Load> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showLoadDialog(row.getItem(), false);
                }
            });
            return row;
        });

        // Multi-row selection for bulk update
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        VBox vbox = new VBox(table, buttonBox);
        statusTab.tab = new Tab(title, vbox);
        statusTab.table = table;
        return statusTab;
    }

    // Search/Advanced Filter tab
    private StatusTab makeSearchTab() {
        StatusTab sTab = new StatusTab();

        // Controls
        TextField loadNumField = new TextField();
        loadNumField.setPromptText("Load #");
        TextField customerField = new TextField();
        ComboBox<Employee> driverBox = new ComboBox<>();
        driverBox.setPromptText("Driver");
        driverBox.setMaxWidth(160);
        ComboBox<Load.Status> statusBox = new ComboBox<>(FXCollections.observableArrayList(Load.Status.values()));
        statusBox.setPromptText("Status");
        DatePicker afterDate = new DatePicker();
        afterDate.setPromptText("From Date");
        DatePicker beforeDate = new DatePicker();
        beforeDate.setPromptText("To Date");
        Button searchBtn = new Button("Search");
        Button exportBtn = new Button("Export CSV");

        // Always refresh driver list
        driverBox.setItems(allDrivers);

        // --- FIX: Show driver names in ComboBox
        driverBox.setCellFactory(cb -> new ListCell<Employee>() {
            @Override
            protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty);
                setText((e == null || empty) ? "" : e.getName());
            }
        });
        driverBox.setButtonCell(new ListCell<Employee>() {
            @Override
            protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty);
                setText((e == null || empty) ? "" : e.getName());
            }
        });

        HBox filters = new HBox(10, loadNumField, customerField, driverBox, statusBox, afterDate, beforeDate, searchBtn, exportBtn);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(10));

        // Filtered list for advanced search
        FilteredList<Load> searchFiltered = new FilteredList<>(allLoads, l -> false);
        TableView<Load> table = makeTableView(searchFiltered);

        searchBtn.setOnAction(e -> {
            searchFiltered.setPredicate(makeSearchPredicate(
                    loadNumField.getText(),
                    customerField.getText(),
                    driverBox.getValue(),
                    statusBox.getValue(),
                    afterDate.getValue(),
                    beforeDate.getValue()
            ));
        });

        exportBtn.setOnAction(e -> exportCSV(table));

        VBox vbox = new VBox(filters, table);
        sTab.tab = new Tab("Advanced Search", vbox);
        sTab.table = table;
        return sTab;
    }

    // TableView for loads, with color-coded status
    private TableView<Load> makeTableView(ObservableList<Load> list) {
        TableView<Load> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(list);

        TableColumn<Load, String> loadNumCol = new TableColumn<>("Load #");
        loadNumCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLoadNumber()));

        TableColumn<Load, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getCustomer()));

        TableColumn<Load, String> pickUpCol = new TableColumn<>("Pick Up Location");
        pickUpCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPickUpLocation()));

        TableColumn<Load, String> dropCol = new TableColumn<>("Drop Location");
        dropCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getDropLocation()));

        TableColumn<Load, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(e -> new SimpleStringProperty(
                e.getValue().getDriver() != null ? e.getValue().getDriver().getName() : ""
        ));

        TableColumn<Load, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getStatus().toString()));
        statusCol.setCellFactory(col -> new TableCell<Load, String>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(s);
                if (!empty && s != null) {
                    setStyle("-fx-background-color: " + getStatusColor(s) + "; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        TableColumn<Load, Number> grossCol = new TableColumn<>("Gross Amount");
        grossCol.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getGrossAmount()));

        TableColumn<Load, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getNotes()));

        table.getColumns().addAll(loadNumCol, customerCol, pickUpCol, dropCol, driverCol, statusCol, grossCol, notesCol);
        return table;
    }

    // Color for status
    private String getStatusColor(String s) {
        switch (s) {
            case "BOOKED":      return "#b6d4fe";
            case "IN_TRANSIT":  return "#ffe59b";
            case "DELIVERED":   return "#b7f9b7";
            case "PAID":        return "#c2c2d6";
            case "CANCELLED":   return "#ffc8c8";
            default:            return "#f7f7f7";
        }
    }

    // Show dialog for Add/Edit
    private void showLoadDialog(Load load, boolean isAdd) {
        Dialog<Load> dialog = new Dialog<>();
        dialog.setTitle(isAdd ? "Add Load" : "Edit Load");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Fields
        TextField loadNumField = new TextField();
        TextField customerField = new TextField();
        TextField pickUpField = new TextField();
        TextField dropField = new TextField();
        ComboBox<Employee> driverBox = new ComboBox<>();
        ComboBox<Load.Status> statusBox = new ComboBox<>(FXCollections.observableArrayList(Load.Status.values()));
        TextField grossField = new TextField();
        TextArea notesField = new TextArea();
        notesField.setPrefRowCount(2);

        // Always fetch latest employees for driver selection
        driverBox.setItems(allDrivers);

        // --- FIX: Show driver names in ComboBox
        driverBox.setCellFactory(cb -> new ListCell<Employee>() {
            @Override
            protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty);
                setText((e == null || empty) ? "" : e.getName());
            }
        });
        driverBox.setButtonCell(new ListCell<Employee>() {
            @Override
            protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty);
                setText((e == null || empty) ? "" : e.getName());
            }
        });

        if (load != null) {
            loadNumField.setText(load.getLoadNumber());
            customerField.setText(load.getCustomer());
            pickUpField.setText(load.getPickUpLocation());
            dropField.setText(load.getDropLocation());
            driverBox.setValue(load.getDriver());
            statusBox.setValue(load.getStatus());
            grossField.setText(String.valueOf(load.getGrossAmount()));
            notesField.setText(load.getNotes());
        }

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        GridPane grid = new GridPane();
        grid.setVgap(7);
        grid.setHgap(12);
        grid.setPadding(new Insets(15, 20, 10, 10));
        int r = 0;
        grid.add(new Label("Load #*:"), 0, r);      grid.add(loadNumField, 1, r++);
        grid.add(new Label("Customer:"), 0, r);     grid.add(customerField, 1, r++);
        grid.add(new Label("Pick Up:"), 0, r);      grid.add(pickUpField, 1, r++);
        grid.add(new Label("Drop Location:"), 0, r);grid.add(dropField, 1, r++);
        grid.add(new Label("Driver:"), 0, r);       grid.add(driverBox, 1, r++);
        grid.add(new Label("Status:"), 0, r);       grid.add(statusBox, 1, r++);
        grid.add(new Label("Gross Amount:"), 0, r); grid.add(grossField, 1, r++);
        grid.add(new Label("Notes:"), 0, r);        grid.add(notesField, 1, r++);
        grid.add(errorLabel, 1, r++);

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);

        Runnable validate = () -> {
            boolean loadNumValid = !loadNumField.getText().trim().isEmpty();
            boolean grossValid = grossField.getText().trim().isEmpty() || isDouble(grossField.getText());
            boolean duplicate = checkDuplicateLoadNumber(loadNumField.getText().trim(),
                    isAdd ? -1 : (load != null ? load.getId() : -1));
            if (duplicate && loadNumValid) {
                errorLabel.setText("Load # already exists.");
                errorLabel.setVisible(true);
            } else {
                errorLabel.setVisible(false);
            }
            okBtn.setDisable(!(loadNumValid && grossValid) || duplicate);
        };
        loadNumField.textProperty().addListener((obs, oldV, newV) -> validate.run());
        grossField.textProperty().addListener((obs, oldV, newV) -> validate.run());
        validate.run();

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    String loadNum = loadNumField.getText().trim();
                    String customer = customerField.getText().trim();
                    String pickUp = pickUpField.getText().trim();
                    String drop = dropField.getText().trim();
                    Employee driver = driverBox.getValue();
                    Load.Status status = statusBox.getValue() != null ? statusBox.getValue() : Load.Status.BOOKED;
                    double gross = grossField.getText().isEmpty() ? 0 : Double.parseDouble(grossField.getText());
                    String notes = notesField.getText().trim();

                    if (isAdd) {
                        Load newLoad = new Load(0, loadNum, customer, pickUp, drop, driver, status, gross, notes);
                        int newId = loadDAO.add(newLoad);
                        newLoad.setId(newId);
                        return newLoad;
                    } else {
                        load.setLoadNumber(loadNum);
                        load.setCustomer(customer);
                        load.setPickUpLocation(pickUp);
                        load.setDropLocation(drop);
                        load.setDriver(driver);
                        load.setStatus(status);
                        load.setGrossAmount(gross);
                        load.setNotes(notes);
                        loadDAO.update(load);
                        return load;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> reloadAll());
    }

    // Duplicate Load # check (case-insensitive, trims, ignores current row)
    private boolean checkDuplicateLoadNumber(String loadNum, int excludeId) {
        String norm = loadNum.trim().toLowerCase(Locale.ROOT);
        for (Load l : allLoads) {
            if (l.getId() != excludeId && l.getLoadNumber() != null &&
                l.getLoadNumber().trim().toLowerCase(Locale.ROOT).equals(norm)) {
                return true;
            }
        }
        return false;
    }

    // Reload all Loads and Drivers (called after any add/edit/delete/refresh)
    private void reloadAll() {
        allLoads.setAll(loadDAO.getAll());
        allDrivers.setAll(employeeDAO.getAll());
        // Refresh driver ComboBoxes in search and add/edit dialogs will see the latest
        // Refresh all status tab tables
        for (StatusTab tab : statusTabs) {
            if (tab.filteredList != null)
                tab.filteredList.setPredicate(tab.filteredList.getPredicate());
        }
    }

    private boolean isDouble(String s) {
        try { Double.parseDouble(s); return true; }
        catch (Exception e) { return false; }
    }

    // Advanced search filter
    private Predicate<Load> makeSearchPredicate(
            String loadNum, String customer, Employee driver, Load.Status status, LocalDate after, LocalDate before) {
        String normLoadNum = loadNum == null ? "" : loadNum.trim().toLowerCase();
        String normCustomer = customer == null ? "" : customer.trim().toLowerCase();
        return l -> {
            boolean match = true;
            if (!normLoadNum.isEmpty())
                match &= l.getLoadNumber() != null && l.getLoadNumber().toLowerCase().contains(normLoadNum);
            if (!normCustomer.isEmpty())
                match &= l.getCustomer() != null && l.getCustomer().toLowerCase().contains(normCustomer);
            if (driver != null)
                match &= l.getDriver() != null && l.getDriver().getId() == driver.getId();
            if (status != null)
                match &= l.getStatus() == status;
            // Optionally, add created/delivered date if you track those
            return match;
        };
    }

    // Export table to CSV
    private void exportCSV(TableView<Load> table) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Loads to CSV");
        fileChooser.setInitialFileName("loads-export.csv");
        File file = fileChooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            // Header
            String header = table.getColumns().stream().map(TableColumn::getText).collect(Collectors.joining(","));
            bw.write(header); bw.newLine();
            // Rows
            for (Load l : table.getItems()) {
                String row = String.join(",",
                        safe(l.getLoadNumber()),
                        safe(l.getCustomer()),
                        safe(l.getPickUpLocation()),
                        safe(l.getDropLocation()),
                        safe(l.getDriver() != null ? l.getDriver().getName() : ""),
                        safe(l.getStatus().toString()),
                        String.valueOf(l.getGrossAmount()),
                        safe(l.getNotes())
                );
                bw.write(row); bw.newLine();
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to write file: " + e.getMessage()).showAndWait();
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace(",", " ");
    }
}
