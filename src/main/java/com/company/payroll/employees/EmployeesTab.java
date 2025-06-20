package com.company.payroll.employees;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Callback;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmployeesTab extends BorderPane {
    private final ObservableList<Employee> employees = FXCollections.observableArrayList();
    private final EmployeeDAO dao = new EmployeeDAO();

    // Listeners for employee data changed events
    public interface EmployeeDataChangeListener {
        void onEmployeeDataChanged(List<Employee> currentList);
    }
    private final List<EmployeeDataChangeListener> listeners = new ArrayList<>();

    public EmployeesTab() {
        // Load from DB on startup
        employees.setAll(dao.getAll());

        // --- SEARCH/FILTER CONTROLS ---
        TextField searchField = new TextField();
        searchField.setPromptText("Search name, license, truck...");

        ComboBox<Employee.Status> statusFilter = new ComboBox<>();
        statusFilter.getItems().add(null); // "Any"
        statusFilter.getItems().addAll(Employee.Status.values());
        statusFilter.setPromptText("Status");

        ComboBox<Employee.DriverType> typeFilter = new ComboBox<>();
        typeFilter.getItems().add(null); // "Any"
        typeFilter.getItems().addAll(Employee.DriverType.values());
        typeFilter.setPromptText("Driver Type");

        HBox filterBox = new HBox(12, searchField, statusFilter, typeFilter);
        filterBox.setPadding(new Insets(10, 10, 0, 10));
        filterBox.setAlignment(Pos.CENTER_LEFT);

        // --- TABLE ---
        TableView<Employee> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Employee, String> nameCol = new TableColumn<>("Driver Name");
        nameCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));

        TableColumn<Employee, String> truckCol = new TableColumn<>("Truck/Unit");
        truckCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getTruckUnit()));

        TableColumn<Employee, Number> driverPctCol = new TableColumn<>("Driver %");
        driverPctCol.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getDriverPercent()));

        TableColumn<Employee, Number> companyPctCol = new TableColumn<>("Company %");
        companyPctCol.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getCompanyPercent()));

        TableColumn<Employee, Number> serviceFeeCol = new TableColumn<>("Service Fee %");
        serviceFeeCol.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getServiceFeePercent()));

        TableColumn<Employee, String> dobCol = new TableColumn<>("DOB");
        dobCol.setCellValueFactory(e -> new SimpleStringProperty(
                e.getValue().getDob() != null ? e.getValue().getDob().toString() : ""));

        TableColumn<Employee, String> licenseCol = new TableColumn<>("License #");
        licenseCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLicenseNumber()));

        TableColumn<Employee, String> driverTypeCol = new TableColumn<>("Driver Type");
        driverTypeCol.setCellValueFactory(e -> new SimpleStringProperty(
                e.getValue().getDriverType() != null ? e.getValue().getDriverType().name().replace("_", " ") : ""));

        TableColumn<Employee, String> llcCol = new TableColumn<>("Employee LLC");
        llcCol.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getEmployeeLLC()));

        TableColumn<Employee, LocalDate> cdlExpiryCol = new TableColumn<>("CDL Expiry");
        cdlExpiryCol.setCellValueFactory(new PropertyValueFactory<>("cdlExpiry"));
        cdlExpiryCol.setCellFactory(getExpiryCellFactory());

        TableColumn<Employee, LocalDate> medExpiryCol = new TableColumn<>("Medical Expiry");
        medExpiryCol.setCellValueFactory(new PropertyValueFactory<>("medicalExpiry"));
        medExpiryCol.setCellFactory(getExpiryCellFactory());

        TableColumn<Employee, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(e -> new SimpleStringProperty(
                e.getValue().getStatus() != null ? e.getValue().getStatus().name().replace("_", " ") : ""));

        table.getColumns().addAll(
            nameCol, truckCol, driverPctCol, companyPctCol, serviceFeeCol,
            dobCol, licenseCol, driverTypeCol, llcCol,
            cdlExpiryCol, medExpiryCol, statusCol
        );

        // --- FILTERED/SORTED VIEW ---
        FilteredList<Employee> filtered = new FilteredList<>(employees, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(emp -> filterEmployee(emp, newVal, statusFilter.getValue(), typeFilter.getValue()));
        });
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(emp -> filterEmployee(emp, searchField.getText(), newVal, typeFilter.getValue()));
        });
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(emp -> filterEmployee(emp, searchField.getText(), statusFilter.getValue(), newVal));
        });

        SortedList<Employee> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // --- DOUBLE-CLICK FOR EDIT ---
        table.setRowFactory(tv -> {
            TableRow<Employee> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEmployeeDialog(row.getItem(), false);
                }
            });
            return row;
        });

        // --- BUTTONS ---
        Button addBtn = new Button("Add");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");

        addBtn.setOnAction(e -> showEmployeeDialog(null, true));
        editBtn.setOnAction(e -> {
            Employee selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showEmployeeDialog(selected, false);
        });
        deleteBtn.setOnAction(e -> {
            Employee selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete driver \"" + selected.getName() + "\"?",
                        ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Confirm Delete");
                confirm.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.YES) {
                        dao.delete(selected.getId());
                        employees.setAll(dao.getAll());
                        notifyEmployeeDataChanged();
                    }
                });
            }
        });

        HBox btnBox = new HBox(12, addBtn, editBtn, deleteBtn);
        btnBox.setPadding(new Insets(12));
        btnBox.setAlignment(Pos.CENTER_LEFT);

        VBox vbox = new VBox(filterBox, table);
        setCenter(vbox);
        setBottom(btnBox);
        setPadding(new Insets(10));
    }

    // Filtering logic for search and filter controls
    private boolean filterEmployee(Employee emp, String text, Employee.Status status, Employee.DriverType type) {
        boolean matches = true;
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (!lower.isEmpty()) {
            matches &= (emp.getName() != null && emp.getName().toLowerCase(Locale.ROOT).contains(lower)) ||
                       (emp.getTruckUnit() != null && emp.getTruckUnit().toLowerCase(Locale.ROOT).contains(lower)) ||
                       (emp.getLicenseNumber() != null && emp.getLicenseNumber().toLowerCase(Locale.ROOT).contains(lower));
        }
        if (status != null) {
            matches &= emp.getStatus() == status;
        }
        if (type != null) {
            matches &= emp.getDriverType() == type;
        }
        return matches;
    }

    // Dialog for Add/Edit, with duplicate driver name detection
    private void showEmployeeDialog(Employee employee, boolean isAdd) {
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle(isAdd ? "Add Employee" : "Edit Employee");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Fields
        TextField nameField = new TextField();
        TextField truckField = new TextField();
        TextField driverPctField = new TextField();
        TextField companyPctField = new TextField();
        TextField serviceFeeField = new TextField();
        DatePicker dobPicker = new DatePicker();
        TextField licenseField = new TextField();
        ComboBox<Employee.DriverType> driverTypeBox = new ComboBox<>(
                FXCollections.observableArrayList(Employee.DriverType.values()));
        TextField llcField = new TextField();
        DatePicker cdlPicker = new DatePicker();
        DatePicker medPicker = new DatePicker();
        ComboBox<Employee.Status> statusBox = new ComboBox<>(
                FXCollections.observableArrayList(Employee.Status.values()));

        if (employee != null) {
            nameField.setText(employee.getName());
            truckField.setText(employee.getTruckUnit());
            driverPctField.setText(String.valueOf(employee.getDriverPercent()));
            companyPctField.setText(String.valueOf(employee.getCompanyPercent()));
            serviceFeeField.setText(String.valueOf(employee.getServiceFeePercent()));
            dobPicker.setValue(employee.getDob());
            licenseField.setText(employee.getLicenseNumber());
            driverTypeBox.setValue(employee.getDriverType());
            llcField.setText(employee.getEmployeeLLC());
            cdlPicker.setValue(employee.getCdlExpiry());
            medPicker.setValue(employee.getMedicalExpiry());
            statusBox.setValue(employee.getStatus());
        }

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        GridPane grid = new GridPane();
        grid.setVgap(7);
        grid.setHgap(12);
        grid.setPadding(new Insets(15, 20, 10, 10));
        int r = 0;
        grid.add(new Label("Driver Name*:"), 0, r);        grid.add(nameField, 1, r++);
        grid.add(new Label("Truck/Unit:"), 0, r);          grid.add(truckField, 1, r++);
        grid.add(new Label("Driver %*:"), 0, r);           grid.add(driverPctField, 1, r++);
        grid.add(new Label("Company %:"), 0, r);           grid.add(companyPctField, 1, r++);
        grid.add(new Label("Service Fee %:"), 0, r);       grid.add(serviceFeeField, 1, r++);
        grid.add(new Label("DOB:"), 0, r);                 grid.add(dobPicker, 1, r++);
        grid.add(new Label("License #:"), 0, r);           grid.add(licenseField, 1, r++);
        grid.add(new Label("Driver Type:"), 0, r);         grid.add(driverTypeBox, 1, r++);
        grid.add(new Label("Employee LLC:"), 0, r);        grid.add(llcField, 1, r++);
        grid.add(new Label("CDL Expiry:"), 0, r);          grid.add(cdlPicker, 1, r++);
        grid.add(new Label("Medical Expiry:"), 0, r);      grid.add(medPicker, 1, r++);
        grid.add(new Label("Status:"), 0, r);              grid.add(statusBox, 1, r++);
        grid.add(errorLabel, 1, r++);

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);

        // Validation logic
        Runnable validate = () -> {
            boolean nameValid = !nameField.getText().trim().isEmpty();
            boolean driverPctValid = isDouble(driverPctField.getText());
            boolean duplicate = checkDuplicateDriverName(
                    nameField.getText().trim(),
                    isAdd ? -1 : (employee != null ? employee.getId() : -1)
            );
            if (duplicate && nameValid) {
                errorLabel.setText("Driver with this name already exists.");
                errorLabel.setVisible(true);
            } else {
                errorLabel.setVisible(false);
            }
            okBtn.setDisable(!(nameValid && driverPctValid) || duplicate);
        };

        nameField.textProperty().addListener((obs, oldV, newV) -> validate.run());
        driverPctField.textProperty().addListener((obs, oldV, newV) -> validate.run());
        validate.run(); // initial

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    String name = nameField.getText().trim();
                    String truck = truckField.getText().trim();
                    double driverPct = parseDouble(driverPctField.getText());
                    double companyPct = parseDouble(companyPctField.getText());
                    double serviceFee = parseDouble(serviceFeeField.getText());
                    LocalDate dob = dobPicker.getValue();
                    String license = licenseField.getText().trim();
                    Employee.DriverType driverType = driverTypeBox.getValue();
                    String llc = llcField.getText().trim();
                    LocalDate cdlExp = cdlPicker.getValue();
                    LocalDate medExp = medPicker.getValue();
                    Employee.Status status = statusBox.getValue();

                    if (isAdd) {
                        Employee emp = new Employee(0, name, truck, driverPct, companyPct, serviceFee, dob, license, driverType, llc, cdlExp, medExp, status);
                        int newId = dao.add(emp);
                        emp.setId(newId);
                        employees.setAll(dao.getAll());
                        notifyEmployeeDataChanged();
                        return emp;
                    } else {
                        employee.setName(name);
                        employee.setTruckUnit(truck);
                        employee.setDriverPercent(driverPct);
                        employee.setCompanyPercent(companyPct);
                        employee.setServiceFeePercent(serviceFee);
                        employee.setDob(dob);
                        employee.setLicenseNumber(license);
                        employee.setDriverType(driverType);
                        employee.setEmployeeLLC(llc);
                        employee.setCdlExpiry(cdlExp);
                        employee.setMedicalExpiry(medExp);
                        employee.setStatus(status);
                        dao.update(employee);
                        employees.setAll(dao.getAll());
                        notifyEmployeeDataChanged();
                        return employee;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // --- Listener registration for other tabs (e.g. LoadsTab) ---
    public void addEmployeeDataChangeListener(EmployeeDataChangeListener listener) {
        listeners.add(listener);
    }
    public void removeEmployeeDataChangeListener(EmployeeDataChangeListener listener) {
        listeners.remove(listener);
    }
    private void notifyEmployeeDataChanged() {
        for (EmployeeDataChangeListener listener : listeners) {
            listener.onEmployeeDataChanged(new ArrayList<>(employees));
        }
    }

    // Check for duplicate driver name, ignoring case and the current record (for edits)
    private boolean checkDuplicateDriverName(String name, int excludeId) {
        String normName = name.trim().toLowerCase(Locale.ROOT);
        for (Employee emp : employees) {
            if (emp.getId() != excludeId && emp.getName() != null &&
                emp.getName().trim().toLowerCase(Locale.ROOT).equals(normName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDouble(String s) {
        try { Double.parseDouble(s); return true; }
        catch (Exception e) { return false; }
    }
    private double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) return 0.0;
        return Double.parseDouble(s.trim());
    }

    // Color coding for expiry columns
    private Callback<TableColumn<Employee, LocalDate>, TableCell<Employee, LocalDate>> getExpiryCellFactory() {
        return col -> new TableCell<Employee, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(date == null ? "" : date.toString());
                if (!empty && date != null) {
                    LocalDate now = LocalDate.now();
                    if (date.isBefore(now)) {
                        setStyle("-fx-background-color: #ffcccc; -fx-font-weight: bold;"); // Red
                    } else if (date.isBefore(now.plusMonths(2))) {
                        setStyle("-fx-background-color: #fff3cd; -fx-font-weight: bold;"); // Yellow
                    } else {
                        setStyle("");
                    }
                } else {
                    setStyle("");
                }
            }
        };
    }

    // Expose current employees list (unmodifiable copy)
    public List<Employee> getCurrentEmployees() {
        return new ArrayList<>(employees);
    }
}