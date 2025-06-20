package com.company.payroll.payroll;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Summary bar with header controls and summary fields.
 * Handles year, company, week navigation and lock status.
 */
public class PayrollSummaryPane extends VBox {
    private final Spinner<Integer> yearSpinner;
    private final Label companyNameLabel;
    private final Button editCompanyBtn;
    private final Button prevWeekBtn, nextWeekBtn;
    private final DatePicker weekStartPicker, weekEndPicker;
    private final Label lockedStatusLabel;

    // Summary fields
    private final Label grossLabel = new Label();
    private final Label serviceFeeLabel = new Label();
    private final Label netAfterFeeLabel = new Label();
    private final Label fuelLabel = new Label();
    private final Label netAfterFuelLabel = new Label();
    private final Label driverShareLabel = new Label();
    private final Label companyShareLabel = new Label();
    private final Label adjustmentsLabel = new Label();
    private final Label recurringLabel = new Label();
    private final Label advanceLabel = new Label();
    private final Label netPayLabel = new Label();

    // State
    private boolean isLocked = false;
    private String companyName = "Acme Logistics LLC";

    private final DateTimeFormatter MMDDYYYY = DateTimeFormatter.ofPattern("M/d/yy");

    public PayrollSummaryPane(
            java.util.function.IntConsumer onYearChange,
            Runnable onCompanyEdit,
            Runnable onPrevWeek,
            Runnable onNextWeek,
            java.util.function.Consumer<LocalDate> onWeekChange,
            int selectedYear,
            LocalDate selectedWeekStart
    ) {
        setSpacing(10);
        setPadding(new Insets(0, 0, 12, 0));

        // Header controls
        HBox headerBox = new HBox(24);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        yearSpinner = new Spinner<>(2020, 2100, selectedYear);
        yearSpinner.setEditable(true);
        yearSpinner.setPrefWidth(80);
        yearSpinner.valueProperty().addListener((obs, oldVal, newVal) -> onYearChange.accept(newVal));
        VBox yearBox = new VBox(new Label("Year:"), yearSpinner);

        companyNameLabel = new Label(companyName);
        companyNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");
        editCompanyBtn = new Button("Edit");
        editCompanyBtn.setOnAction(e -> onCompanyEdit.run());
        HBox companyBox = new HBox(companyNameLabel, editCompanyBtn);
        companyBox.setSpacing(10);
        VBox companyVBox = new VBox(new Label("Company:"), companyBox);

        headerBox.getChildren().addAll(yearBox, companyVBox);

        // Week picker controls - initialize both pickers before listeners
        weekStartPicker = new DatePicker(selectedWeekStart);
        weekEndPicker = new DatePicker(selectedWeekStart.plusDays(6));

        weekStartPicker.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date == null ? "" : MMDDYYYY.format(date); }
            @Override public LocalDate fromString(String s) {
                try { return LocalDate.parse(s, MMDDYYYY); } catch (Exception e) { return null; }
            }
        });
        weekEndPicker.setConverter(weekStartPicker.getConverter());

        weekStartPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                weekEndPicker.setValue(newVal.plusDays(6));
                onWeekChange.accept(newVal);
            }
        });
        weekStartPicker.setPrefWidth(100);
        weekEndPicker.setPrefWidth(100);
        weekEndPicker.setDisable(true);

        prevWeekBtn = new Button("<");
        nextWeekBtn = new Button(">");

        prevWeekBtn.setOnAction(e -> {
            LocalDate newStart = weekStartPicker.getValue().minusWeeks(1);
            weekStartPicker.setValue(newStart);
        });

        nextWeekBtn.setOnAction(e -> {
            LocalDate newStart = weekStartPicker.getValue().plusWeeks(1);
            weekStartPicker.setValue(newStart);
        });

        HBox weekBox = new HBox(10, new Label("Pay Week (Mon-Sun):"), prevWeekBtn, weekStartPicker, new Label("to"), weekEndPicker, nextWeekBtn);
        weekBox.setAlignment(Pos.CENTER_LEFT);

        // Lock status
        HBox lockStatusBox = new HBox();
        lockedStatusLabel = new Label();
        lockedStatusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.05em; -fx-text-fill: #b22222;");
        lockStatusBox.setAlignment(Pos.CENTER_RIGHT);
        lockStatusBox.getChildren().add(lockedStatusLabel);

        // Payroll summary panel
        Node summaryPanel = buildSummaryPanel();

        getChildren().addAll(headerBox, weekBox, lockStatusBox, summaryPanel);
    }

    private Node buildSummaryPanel() {
        HBox summaryBox = new HBox(24);
        summaryBox.setPadding(new Insets(10, 12, 10, 12));
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setStyle(
                "-fx-background-color: #f7fafd; " +
                "-fx-border-color: #d0d7df; " +
                "-fx-border-radius: 7; -fx-background-radius: 7;"
        );
        summaryBox.getChildren().addAll(
                summaryPair("Gross:", grossLabel),
                summaryPair("Service Fee:", serviceFeeLabel),
                summaryPair("Net after Fee:", netAfterFeeLabel),
                summaryPair("Fuel:", fuelLabel),
                summaryPair("Net after Fuel:", netAfterFuelLabel),
                summaryPair("Driver Share:", driverShareLabel),
                summaryPair("Company Share:", companyShareLabel),
                summaryPair("Adjustments:", adjustmentsLabel),
                summaryPair("Recurring:", recurringLabel),
                summaryPair("Advance:", advanceLabel),
                summaryPair("Net:", netPayLabel)
        );
        return summaryBox;
    }

    private Node summaryPair(String labelText, Label valueLabel) {
        VBox box = new VBox(2);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 0.95em; -fx-text-fill: #555;");
        valueLabel.setStyle("-fx-font-size: 1.08em; -fx-font-weight: bold;");
        box.getChildren().addAll(label, valueLabel);
        return box;
    }

    // Update summary values from table rows
    public void updateSummary(List<PayrollRow> rows) {
        double gross = rows.stream().mapToDouble(PayrollRow::getGross).sum();
        double serviceFee = rows.stream().mapToDouble(r -> r.getGross() * r.getServiceFeePercent() / 100.0).sum();
        double netAfterFee = rows.stream().mapToDouble(PayrollRow::getNetAfterServiceFee).sum();
        double fuel = rows.stream().mapToDouble(PayrollRow::getFuelDeducted).sum();
        double netAfterFuel = rows.stream().mapToDouble(PayrollRow::getNetAfterFuel).sum();
        double driverShare = rows.stream().mapToDouble(PayrollRow::getDriverPay).sum();
        double companyShare = rows.stream().mapToDouble(PayrollRow::getCompanyPay).sum();
        double adjustments = rows.stream().mapToDouble(PayrollRow::getAdjustments).sum();
        double recurring = rows.stream().mapToDouble(PayrollRow::getRecurringFees).sum();
        double advance = rows.stream().mapToDouble(PayrollRow::getAdvanceRepayment).sum();
        double netPay = rows.stream().mapToDouble(PayrollRow::getNetPay).sum();

        grossLabel.setText(formatCurrency(gross));
        serviceFeeLabel.setText(formatCurrency(serviceFee));
        netAfterFeeLabel.setText(formatCurrency(netAfterFee));
        fuelLabel.setText(formatCurrency(fuel));
        netAfterFuelLabel.setText(formatCurrency(netAfterFuel));
        driverShareLabel.setText(formatCurrency(driverShare));
        companyShareLabel.setText(formatCurrency(companyShare));
        adjustmentsLabel.setText(formatCurrency(adjustments));
        recurringLabel.setText(formatCurrency(recurring));
        advanceLabel.setText(formatCurrency(advance));
        netPayLabel.setText(formatCurrency(netPay));
    }

    // Week navigation
    public void setWeek(LocalDate monday) {
        weekStartPicker.setValue(monday);
        weekEndPicker.setValue(monday.plusDays(6));
    }

    // Lock status (toggle for now, stub)
    public void toggleLock() {
        isLocked = !isLocked;
        lockedStatusLabel.setText(isLocked ? "PAYROLL LOCKED" : "");
    }
    public boolean isLocked() { return isLocked; }

    // Edit company dialog
    public void showEditCompanyDialog() {
        TextInputDialog dialog = new TextInputDialog(companyName);
        dialog.setTitle("Edit Company Name");
        dialog.setHeaderText("Edit Company Name");
        dialog.setContentText("Company name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            companyName = name;
            companyNameLabel.setText(name);
        });
    }

    private String formatCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }
}