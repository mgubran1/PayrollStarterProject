package com.company.payroll.feesadvances;

import com.company.payroll.employees.Employee;
import com.company.payroll.employees.EmployeeDAO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FeesAdvancesTab extends BorderPane {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final FeeAdvancesDAO feeAdvancesDAO = new FeeAdvancesDAO();
    private ObservableList<Employee> allDrivers = FXCollections.observableArrayList();

    // Models
    private ObservableList<FeeEntry> allFeeEntries = FXCollections.observableArrayList();
    private ObservableList<CashAdvanceEntry> allCashAdvances = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Advanced search controls
    private ComboBox<Employee> feeSearchDriverBox;
    private ComboBox<Month> feeSearchMonthBox;
    private Spinner<Integer> feeSearchYearSpinner;

    private ComboBox<Employee> cashSearchDriverBox;
    private DatePicker cashSearchFromPicker;
    private DatePicker cashSearchToPicker;

    public FeesAdvancesTab() {
        allDrivers.setAll(employeeDAO.getAll());
        allFeeEntries.setAll(feeAdvancesDAO.getAllFees());
        allCashAdvances.setAll(feeAdvancesDAO.getAllCashAdvances());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab recurringTab = new Tab("Recurring Fees", buildFeesTab());
        Tab advancesTab = new Tab("Cash Advances", buildAdvancesTab());

        tabPane.getTabs().add(recurringTab);
        tabPane.getTabs().add(advancesTab);

        // Auto-refresh driver ComboBoxes whenever the tab is selected
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> refreshDriverComboBoxes());

        setCenter(tabPane);
    }

    /**
     * Always refreshes the driver search ComboBoxes with the latest employees from the DB.
     */
    private void refreshDriverComboBoxes() {
        ObservableList<Employee> freshDrivers = FXCollections.observableArrayList(employeeDAO.getAll());
        if (feeSearchDriverBox != null) feeSearchDriverBox.setItems(freshDrivers);
        if (cashSearchDriverBox != null) cashSearchDriverBox.setItems(freshDrivers);
    }

    // ========== FEE MANAGEMENT ==========

    private Node buildFeesTab() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        // SEARCH CONTROLS
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        feeSearchDriverBox = new ComboBox<>();
        feeSearchDriverBox.setPromptText("Driver");
        feeSearchDriverBox.setConverter(new StringConverter<>() {
            @Override public String toString(Employee e) { return e == null ? "" : e.getName(); }
            @Override public Employee fromString(String s) { return null; }
        });
        // --- AUTO-REFRESH driver list when search ComboBox is clicked/opened ---
        feeSearchDriverBox.setOnShowing(e -> feeSearchDriverBox.setItems(FXCollections.observableArrayList(employeeDAO.getAll())));

        feeSearchMonthBox = new ComboBox<>(FXCollections.observableArrayList(Month.values()));
        feeSearchMonthBox.setPromptText("Month");

        feeSearchYearSpinner = new Spinner<>();
        int thisYear = LocalDate.now().getYear();
        feeSearchYearSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2020, 2100, thisYear));
        feeSearchYearSpinner.setEditable(true);
        feeSearchYearSpinner.setPrefWidth(100);

        Button searchBtn = new Button("Search");
        Button clearBtn = new Button("Clear");

        searchBtn.setOnAction(e -> {
            Integer driverId = feeSearchDriverBox.getValue() != null ? feeSearchDriverBox.getValue().getId() : null;
            Integer month = feeSearchMonthBox.getValue() != null ? feeSearchMonthBox.getValue().getValue() : null;
            Integer year = feeSearchYearSpinner.getValue();
            allFeeEntries.setAll(feeAdvancesDAO.searchFees(driverId, month, year));
        });
        clearBtn.setOnAction(e -> {
            feeSearchDriverBox.setValue(null);
            feeSearchMonthBox.setValue(null);
            feeSearchYearSpinner.getValueFactory().setValue(LocalDate.now().getYear());
            allFeeEntries.setAll(feeAdvancesDAO.getAllFees());
            refreshDriverComboBoxes();
        });

        searchBox.getChildren().addAll(new Label("Search:"), feeSearchDriverBox, feeSearchMonthBox, feeSearchYearSpinner, searchBtn, clearBtn);

        TableView<FeeEntry> table = new TableView<>(allFeeEntries);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<FeeEntry, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDriver().getName()));

        TableColumn<FeeEntry, String> feeTypeCol = new TableColumn<>("Fee Type");
        feeTypeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFeeType().toString()));

        TableColumn<FeeEntry, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getAmount()).asObject());

        TableColumn<FeeEntry, Integer> weeksCol = new TableColumn<>("Payment Plan (Weeks)");
        weeksCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTotalWeeks()));

        TableColumn<FeeEntry, Integer> remainingCol = new TableColumn<>("Weeks Left");
        remainingCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getWeeksRemaining()));

        TableColumn<FeeEntry, String> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStartDate().format(dateFmt)));

        TableColumn<FeeEntry, Integer> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getFeeMonth()));

        TableColumn<FeeEntry, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getFeeYear()));

        TableColumn<FeeEntry, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActive() ? "Active" : "Completed"));

        table.getColumns().addAll(driverCol, feeTypeCol, amountCol, weeksCol, remainingCol, startCol, monthCol, yearCol, statusCol);

        Button addBtn = new Button("Add Fee");
        Button editBtn = new Button("Edit Fee");
        Button removeBtn = new Button("Remove Fee");
        Button refreshBtn = new Button("Refresh");
        Button batchBtn = new Button("Batch Fees");

        addBtn.setOnAction(e -> showFeeDialog(null, true));
        editBtn.setOnAction(e -> {
            FeeEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showFeeDialog(selected, false);
        });
        removeBtn.setOnAction(e -> {
            FeeEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this fee?", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Confirm Delete");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        feeAdvancesDAO.deleteFee(selected.getId());
                        allFeeEntries.remove(selected);
                        refreshDriverComboBoxes();
                    }
                });
            }
        });
        refreshBtn.setOnAction(e -> {
            allFeeEntries.setAll(feeAdvancesDAO.getAllFees());
            refreshDriverComboBoxes();
        });
        batchBtn.setOnAction(e -> showBatchFeeDialog());

        // Always ensure drivers list is current at build
        refreshDriverComboBoxes();

        HBox btnBox = new HBox(10, addBtn, editBtn, removeBtn, batchBtn, refreshBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(searchBox, table, btnBox);
        return root;
    }

    private void showBatchFeeDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch Apply Recurring Fees");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Fetch latest list of drivers and fees
        List<Employee> drivers = employeeDAO.getAll();
        EnumMap<FeeType, TextField> amountFields = new EnumMap<>(FeeType.class);

        // Default amounts - could be loaded from config or hardcoded as needed
        Map<FeeType, Double> defaultAmounts = new EnumMap<>(FeeType.class);
        for (FeeType ft : FeeType.values()) defaultAmounts.put(ft, 0.0);

        // FeeType grid
        GridPane grid = new GridPane();
        grid.setVgap(7);
        grid.setHgap(10);
        grid.setPadding(new Insets(15));
        int r = 0;
        grid.add(new Label("Set default amount for each Fee Type:"), 0, r, 2, 1);
        r++;

        for (FeeType ft : FeeType.values()) {
            grid.add(new Label(ft.name() + ":"), 0, r);
            TextField tf = new TextField();
            tf.setPromptText("Amount");
            tf.setText(String.valueOf(defaultAmounts.get(ft)));
            amountFields.put(ft, tf);
            grid.add(tf, 1, r++);
        }

        // Month/year selectors
        ComboBox<Month> monthBox = new ComboBox<>(FXCollections.observableArrayList(Month.values()));
        monthBox.setValue(LocalDate.now().getMonth());
        Spinner<Integer> yearSpinner = new Spinner<>();
        int thisYear = LocalDate.now().getYear();
        yearSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2020, 2100, thisYear));
        yearSpinner.setEditable(true);
        yearSpinner.setPrefWidth(100);

        grid.add(new Label("Month:"), 0, r); grid.add(monthBox, 1, r++);
        grid.add(new Label("Year:"), 0, r); grid.add(yearSpinner, 1, r++);

        Label infoLabel = new Label("One-time fees will be applied to ALL drivers not on a payment plan or with existing fee for each fee type (month & year).");
        infoLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 10.5pt;");
        grid.add(infoLabel, 0, r, 2, 1);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);
        grid.add(errorLabel, 0, ++r, 2, 1);

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);

        Runnable validate = () -> {
            boolean valid = true;
            for (TextField tf : amountFields.values()) {
                if (!tf.getText().isEmpty() && !isDouble(tf.getText())) valid = false;
            }
            errorLabel.setVisible(!valid);
            errorLabel.setText(valid ? "" : "All amounts must be valid numbers.");
            okBtn.setDisable(!valid);
        };
        for (TextField tf : amountFields.values()) {
            tf.textProperty().addListener((obs, o, n) -> validate.run());
        }
        validate.run();

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Map<FeeType, Double> amounts = new EnumMap<>(FeeType.class);
                for (FeeType ft : FeeType.values()) {
                    String amtStr = amountFields.get(ft).getText();
                    if (isDouble(amtStr)) {
                        double amt = Double.parseDouble(amtStr);
                        if (amt > 0) {
                            amounts.put(ft, amt);
                        }
                    }
                }
                int selectedMonth = monthBox.getValue().getValue();
                int selectedYear = yearSpinner.getValue();
                LocalDate feeDate = LocalDate.of(selectedYear, selectedMonth, 1);

                // Fetch latest drivers
                List<Employee> allDriversList = employeeDAO.getAll();
                int totalApplied = 0, totalSkipped = 0;
                List<String> skipped = new ArrayList<>();

                for (FeeType ft : amounts.keySet()) {
                    double feeAmount = amounts.get(ft);

                    for (Employee drv : allDriversList) {
                        boolean isDuplicate = feeAdvancesDAO.feeExists(drv.getId(), ft, selectedMonth, selectedYear);
                        boolean onPlan = false;
                        for (FeeEntry entry : feeAdvancesDAO.getAllFees()) {
                            if (entry.getDriver().getId() == drv.getId() &&
                                    entry.getFeeType() == ft &&
                                    entry.isActive() &&
                                    entry.getTotalWeeks() > 1) {
                                onPlan = true;
                                break;
                            }
                        }
                        if (!isDuplicate && !onPlan) {
                            FeeEntry fee = new FeeEntry(0, drv, ft, feeAmount, 1, 1, feeDate, true, selectedMonth, selectedYear);
                            feeAdvancesDAO.addFee(fee);
                            totalApplied++;
                        } else {
                            String why = isDuplicate ? "duplicate" : "payment plan";
                            skipped.add(drv.getName() + " (" + ft.name() + ", " + why + ")");
                            totalSkipped++;
                        }
                    }
                }
                allFeeEntries.setAll(feeAdvancesDAO.getAllFees());
                refreshDriverComboBoxes();
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setHeaderText("Batch Fees Complete");
                info.setContentText("Fees applied for " + totalApplied + " drivers.\n" +
                        (totalSkipped > 0 ? (totalSkipped + " entries skipped (see details below):\n" + String.join(", ", skipped)) : ""));
                info.showAndWait();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showFeeDialog(FeeEntry fee, boolean isAdd) {
        Dialog<FeeEntry> dialog = new Dialog<>();
        dialog.setTitle(isAdd ? "Add Recurring Fee" : "Edit Recurring Fee");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ObservableList<Employee> freshDrivers = FXCollections.observableArrayList(employeeDAO.getAll());
        ComboBox<Employee> driverBox = new ComboBox<>(freshDrivers);
        driverBox.setConverter(new StringConverter<>() {
            @Override public String toString(Employee e) { return e == null ? "" : e.getName(); }
            @Override public Employee fromString(String s) { return null; }
        });

        ComboBox<FeeType> feeTypeBox = new ComboBox<>(FXCollections.observableArrayList(FeeType.values()));
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        ComboBox<Integer> planBox = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4));
        planBox.setPromptText("Weeks");
        planBox.setValue(4);

        DatePicker startDate = new DatePicker(LocalDate.now());
        ComboBox<Month> monthBox = new ComboBox<>(FXCollections.observableArrayList(Month.values()));
        monthBox.setValue(LocalDate.now().getMonth());
        Spinner<Integer> yearSpinner = new Spinner<>();
        int thisYear = LocalDate.now().getYear();
        yearSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2020, 2100, thisYear));
        yearSpinner.setEditable(true);
        yearSpinner.setPrefWidth(100);

        if (fee != null) {
            driverBox.setValue(fee.getDriver());
            feeTypeBox.setValue(fee.getFeeType());
            amountField.setText(String.valueOf(fee.getAmount()));
            planBox.setValue(fee.getTotalWeeks());
            startDate.setValue(fee.getStartDate());
            monthBox.setValue(Month.of(fee.getFeeMonth()));
            yearSpinner.getValueFactory().setValue(fee.getFeeYear());
        }

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        GridPane grid = new GridPane();
        grid.setVgap(7);
        grid.setHgap(10);
        grid.setPadding(new Insets(15));
        int r = 0;
        grid.add(new Label("Driver:"), 0, r); grid.add(driverBox, 1, r++);
        grid.add(new Label("Fee Type:"), 0, r); grid.add(feeTypeBox, 1, r++);
        grid.add(new Label("Amount:"), 0, r); grid.add(amountField, 1, r++);
        grid.add(new Label("Payment Plan (weeks):"), 0, r); grid.add(planBox, 1, r++);
        grid.add(new Label("Start Date:"), 0, r); grid.add(startDate, 1, r++);
        grid.add(new Label("Month:"), 0, r); grid.add(monthBox, 1, r++);
        grid.add(new Label("Year:"), 0, r); grid.add(yearSpinner, 1, r++);
        grid.add(errorLabel, 1, r++);

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);

        Runnable validate = () -> {
            boolean valid = driverBox.getValue() != null &&
                    feeTypeBox.getValue() != null &&
                    isDouble(amountField.getText()) &&
                    planBox.getValue() != null &&
                    startDate.getValue() != null &&
                    monthBox.getValue() != null &&
                    yearSpinner.getValue() != null;
            errorLabel.setVisible(!valid);
            errorLabel.setText(valid ? "" : "All fields required. Amount must be number.");
            okBtn.setDisable(!valid);
        };
        driverBox.valueProperty().addListener((obs, o, n) -> validate.run());
        feeTypeBox.valueProperty().addListener((obs, o, n) -> validate.run());
        amountField.textProperty().addListener((obs, o, n) -> validate.run());
        planBox.valueProperty().addListener((obs, o, n) -> validate.run());
        startDate.valueProperty().addListener((obs, o, n) -> validate.run());
        monthBox.valueProperty().addListener((obs, o, n) -> validate.run());
        yearSpinner.valueProperty().addListener((obs, o, n) -> validate.run());
        validate.run();

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Employee driver = driverBox.getValue();
                FeeType type = feeTypeBox.getValue();
                double amt = Double.parseDouble(amountField.getText());
                int plan = planBox.getValue();
                LocalDate sDate = startDate.getValue();
                int selectedMonth = monthBox.getValue().getValue();
                int selectedYear = yearSpinner.getValue();
                if (isAdd && feeAdvancesDAO.feeExists(driver.getId(), type, selectedMonth, selectedYear)) {
                    errorLabel.setVisible(true);
                    errorLabel.setText("Duplicate: Fee for this driver, type, month, year already exists.");
                    return null;
                }
                if (isAdd) {
                    FeeEntry f = new FeeEntry(0, driver, type, amt, plan, plan, sDate, true, selectedMonth, selectedYear);
                    feeAdvancesDAO.addFee(f);
                    allFeeEntries.setAll(feeAdvancesDAO.getAllFees());
                    refreshDriverComboBoxes();
                    return f;
                } else {
                    fee.setDriver(driver);
                    fee.setFeeType(type);
                    fee.setAmount(amt);
                    fee.setTotalWeeks(plan);
                    fee.setWeeksRemaining(plan);
                    fee.setStartDate(sDate);
                    fee.setActive(true);
                    fee.setFeeMonth(selectedMonth);
                    fee.setFeeYear(selectedYear);
                    feeAdvancesDAO.updateFee(fee, fee.getId());
                    allFeeEntries.setAll(feeAdvancesDAO.getAllFees());
                    refreshDriverComboBoxes();
                    return fee;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ========== CASH ADVANCE MANAGEMENT ==========

    private Node buildAdvancesTab() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        // SEARCH CONTROLS
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        cashSearchDriverBox = new ComboBox<>();
        cashSearchDriverBox.setPromptText("Driver");
        cashSearchDriverBox.setConverter(new StringConverter<>() {
            @Override public String toString(Employee e) { return e == null ? "" : e.getName(); }
            @Override public Employee fromString(String s) { return null; }
        });
        // --- AUTO-REFRESH driver list when search ComboBox is clicked/opened ---
        cashSearchDriverBox.setOnShowing(e -> cashSearchDriverBox.setItems(FXCollections.observableArrayList(employeeDAO.getAll())));
        cashSearchFromPicker = new DatePicker();
        cashSearchFromPicker.setPromptText("From");
        cashSearchToPicker = new DatePicker();
        cashSearchToPicker.setPromptText("To");
        Button searchBtn = new Button("Search");
        Button clearBtn = new Button("Clear");
        searchBtn.setOnAction(e -> {
            Integer driverId = cashSearchDriverBox.getValue() != null ? cashSearchDriverBox.getValue().getId() : null;
            LocalDate from = cashSearchFromPicker.getValue();
            LocalDate to = cashSearchToPicker.getValue();
            allCashAdvances.setAll(feeAdvancesDAO.searchCashAdvances(driverId, from, to));
        });
        clearBtn.setOnAction(e -> {
            cashSearchDriverBox.setValue(null);
            cashSearchFromPicker.setValue(null);
            cashSearchToPicker.setValue(null);
            allCashAdvances.setAll(feeAdvancesDAO.getAllCashAdvances());
            refreshDriverComboBoxes();
        });
        searchBox.getChildren().addAll(new Label("Search:"), cashSearchDriverBox, cashSearchFromPicker, cashSearchToPicker, searchBtn, clearBtn);

        TableView<CashAdvanceEntry> table = new TableView<>(allCashAdvances);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<CashAdvanceEntry, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDriver().getName()));

        TableColumn<CashAdvanceEntry, Double> amountCol = new TableColumn<>("Advance Amount");
        amountCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getAmount()).asObject());

        TableColumn<CashAdvanceEntry, String> givenCol = new TableColumn<>("Date Given");
        givenCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getGivenDate().format(dateFmt)));

        TableColumn<CashAdvanceEntry, String> dueCol = new TableColumn<>("Due Date");
        dueCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDueDate().format(dateFmt)));

        TableColumn<CashAdvanceEntry, Integer> planCol = new TableColumn<>("Repayment Weeks");
        planCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPaymentWeeks()));

        TableColumn<CashAdvanceEntry, Integer> remainCol = new TableColumn<>("Weeks Remaining");
        remainCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getWeeksRemaining()));

        TableColumn<CashAdvanceEntry, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActive() ? "Active" : "Paid Off"));

        table.getColumns().addAll(driverCol, amountCol, givenCol, dueCol, planCol, remainCol, statusCol);

        Button addBtn = new Button("Add Advance");
        Button editBtn = new Button("Edit Advance");
        Button removeBtn = new Button("Remove Advance");
        Button refreshBtn = new Button("Refresh");

        addBtn.setOnAction(e -> showAdvanceDialog(null, true));
        editBtn.setOnAction(e -> {
            CashAdvanceEntry sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) showAdvanceDialog(sel, false);
        });
        removeBtn.setOnAction(e -> {
            CashAdvanceEntry sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this advance?", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Confirm Delete");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        feeAdvancesDAO.deleteCashAdvance(sel.getId());
                        allCashAdvances.remove(sel);
                        refreshDriverComboBoxes();
                    }
                });
            }
        });
        refreshBtn.setOnAction(e -> {
            allCashAdvances.setAll(feeAdvancesDAO.getAllCashAdvances());
            refreshDriverComboBoxes();
        });

        // Always ensure drivers list is current at build
        refreshDriverComboBoxes();

        HBox btnBox = new HBox(10, addBtn, editBtn, removeBtn, refreshBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(searchBox, table, btnBox);
        return root;
    }

    private void showAdvanceDialog(CashAdvanceEntry adv, boolean isAdd) {
        Dialog<CashAdvanceEntry> dialog = new Dialog<>();
        dialog.setTitle(isAdd ? "Add Cash Advance" : "Edit Cash Advance");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ObservableList<Employee> freshDrivers = FXCollections.observableArrayList(employeeDAO.getAll());
        ComboBox<Employee> driverBox = new ComboBox<>(freshDrivers);
        driverBox.setConverter(new StringConverter<>() {
            @Override public String toString(Employee e) { return e == null ? "" : e.getName(); }
            @Override public Employee fromString(String s) { return null; }
        });

        TextField amountField = new TextField();
        amountField.setPromptText("Advance Amount");

        DatePicker givenDate = new DatePicker(LocalDate.now());
        DatePicker dueDate = new DatePicker(LocalDate.now().plusWeeks(1));

        ComboBox<Integer> planBox = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4));
        planBox.setPromptText("Repayment Weeks");
        planBox.setValue(1);

        if (adv != null) {
            driverBox.setValue(adv.getDriver());
            amountField.setText(String.valueOf(adv.getAmount()));
            givenDate.setValue(adv.getGivenDate());
            dueDate.setValue(adv.getDueDate());
            planBox.setValue(adv.getPaymentWeeks());
        }

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        GridPane grid = new GridPane();
        grid.setVgap(7);
        grid.setHgap(10);
        grid.setPadding(new Insets(15));
        int r = 0;
        grid.add(new Label("Driver:"), 0, r); grid.add(driverBox, 1, r++);
        grid.add(new Label("Advance Amount:"), 0, r); grid.add(amountField, 1, r++);
        grid.add(new Label("Date Given:"), 0, r); grid.add(givenDate, 1, r++);
        grid.add(new Label("Due Date:"), 0, r); grid.add(dueDate, 1, r++);
        grid.add(new Label("Repayment Plan (weeks):"), 0, r); grid.add(planBox, 1, r++);
        grid.add(errorLabel, 1, r++);

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);

        Runnable validate = () -> {
            boolean valid = driverBox.getValue() != null &&
                    isDouble(amountField.getText()) &&
                    givenDate.getValue() != null &&
                    dueDate.getValue() != null &&
                    planBox.getValue() != null &&
                    !dueDate.getValue().isBefore(givenDate.getValue());
            errorLabel.setVisible(!valid);
            errorLabel.setText(valid ? "" : "All fields required. Amount must be number. Due date must be after given date.");
            okBtn.setDisable(!valid);
        };
        driverBox.valueProperty().addListener((obs, o, n) -> validate.run());
        amountField.textProperty().addListener((obs, o, n) -> validate.run());
        givenDate.valueProperty().addListener((obs, o, n) -> validate.run());
        dueDate.valueProperty().addListener((obs, o, n) -> validate.run());
        planBox.valueProperty().addListener((obs, o, n) -> validate.run());
        validate.run();

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Employee driver = driverBox.getValue();
                double amt = Double.parseDouble(amountField.getText());
                LocalDate gDate = givenDate.getValue();
                LocalDate dDate = dueDate.getValue();
                int plan = planBox.getValue();
                if (isAdd) {
                    CashAdvanceEntry ca = new CashAdvanceEntry(0, driver, amt, gDate, dDate, plan, plan, true);
                    feeAdvancesDAO.addCashAdvance(ca);
                    allCashAdvances.setAll(feeAdvancesDAO.getAllCashAdvances());
                    refreshDriverComboBoxes();
                    return ca;
                } else {
                    adv.setDriver(driver);
                    adv.setAmount(amt);
                    adv.setGivenDate(gDate);
                    adv.setDueDate(dDate);
                    adv.setPaymentWeeks(plan);
                    adv.setWeeksRemaining(plan);
                    adv.setActive(true);
                    feeAdvancesDAO.updateCashAdvance(adv, adv.getId());
                    allCashAdvances.setAll(feeAdvancesDAO.getAllCashAdvances());
                    refreshDriverComboBoxes();
                    return adv;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private boolean isDouble(String s) {
        try { Double.parseDouble(s); return true; }
        catch (Exception e) { return false; }
    }

    // ======== MODEL CLASSES ========

    public enum FeeType {
        ELD, TVC, PARKING, ACH, OTHER
    }

    public static class FeeEntry {
        private int id;
        private Employee driver;
        private FeeType feeType;
        private double amount;
        private int totalWeeks;
        private int weeksRemaining;
        private LocalDate startDate;
        private boolean active;
        private int feeMonth;
        private int feeYear;

        public FeeEntry(int id, Employee driver, FeeType feeType, double amount, int totalWeeks, int weeksRemaining, LocalDate startDate, boolean active, int feeMonth, int feeYear) {
            this.id = id;
            this.driver = driver;
            this.feeType = feeType;
            this.amount = amount;
            this.totalWeeks = totalWeeks;
            this.weeksRemaining = weeksRemaining;
            this.startDate = startDate;
            this.active = active;
            this.feeMonth = feeMonth;
            this.feeYear = feeYear;
        }

        // Backward compatible constructor for DAO legacy rows
        public FeeEntry(int id, Employee driver, FeeType feeType, double amount, int totalWeeks, int weeksRemaining, LocalDate startDate, boolean active) {
            this(id, driver, feeType, amount, totalWeeks, weeksRemaining, startDate, active, startDate.getMonthValue(), startDate.getYear());
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public Employee getDriver() { return driver; }
        public void setDriver(Employee driver) { this.driver = driver; }
        public FeeType getFeeType() { return feeType; }
        public void setFeeType(FeeType feeType) { this.feeType = feeType; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public int getTotalWeeks() { return totalWeeks; }
        public void setTotalWeeks(int totalWeeks) { this.totalWeeks = totalWeeks; }
        public int getWeeksRemaining() { return weeksRemaining; }
        public void setWeeksRemaining(int weeksRemaining) { this.weeksRemaining = weeksRemaining; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public int getFeeMonth() { return feeMonth; }
        public void setFeeMonth(int feeMonth) { this.feeMonth = feeMonth; }
        public int getFeeYear() { return feeYear; }
        public void setFeeYear(int feeYear) { this.feeYear = feeYear; }
    }

    public static class CashAdvanceEntry {
        private int id;
        private Employee driver;
        private double amount;
        private LocalDate givenDate;
        private LocalDate dueDate;
        private int paymentWeeks;
        private int weeksRemaining;
        private boolean active;

        public CashAdvanceEntry(int id, Employee driver, double amount, LocalDate givenDate, LocalDate dueDate, int paymentWeeks, int weeksRemaining, boolean active) {
            this.id = id;
            this.driver = driver;
            this.amount = amount;
            this.givenDate = givenDate;
            this.dueDate = dueDate;
            this.paymentWeeks = paymentWeeks;
            this.weeksRemaining = weeksRemaining;
            this.active = active;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public Employee getDriver() { return driver; }
        public void setDriver(Employee driver) { this.driver = driver; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public LocalDate getGivenDate() { return givenDate; }
        public void setGivenDate(LocalDate givenDate) { this.givenDate = givenDate; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public int getPaymentWeeks() { return paymentWeeks; }
        public void setPaymentWeeks(int paymentWeeks) { this.paymentWeeks = paymentWeeks; }
        public int getWeeksRemaining() { return weeksRemaining; }
        public void setWeeksRemaining(int weeksRemaining) { this.weeksRemaining = weeksRemaining; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}