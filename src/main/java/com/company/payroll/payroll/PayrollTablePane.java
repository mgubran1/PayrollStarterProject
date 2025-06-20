package com.company.payroll.payroll;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Table of drivers with filtering and pagination.
 */
public class PayrollTablePane extends BorderPane {
    private static final int ROWS_PER_PAGE = 15;

    private final TableView<PayrollRow> table;
    private final Pagination pagination;
    private final TextField filterField;

    private List<PayrollRow> allRows = List.of(); // All driver payroll rows
    private List<PayrollRow> filteredRows = List.of(); // Filtered by search

    public PayrollTablePane() {
        setPadding(new Insets(0, 8, 0, 8));

        filterField = new TextField();
        filterField.setPromptText("Search Driver...");
        filterField.setPrefWidth(180);
        filterField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        HBox filterBar = new HBox(8, new Label("Filter:"), filterField);
        filterBar.setPadding(new Insets(6, 0, 8, 0));

        setTop(filterBar);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<PayrollRow, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getDriverName()));
        driverCol.setPrefWidth(130);

        TableColumn<PayrollRow, String> grossCol = new TableColumn<>("Gross");
        grossCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getGrossFormatted()));
        grossCol.setPrefWidth(90);

        TableColumn<PayrollRow, String> serviceFeeCol = new TableColumn<>("Service Fee %");
        serviceFeeCol.setCellValueFactory(row -> new SimpleStringProperty(String.format("%.1f%%", row.getValue().getServiceFeePercent())));
        serviceFeeCol.setPrefWidth(95);

        TableColumn<PayrollRow, String> netAfterFeeCol = new TableColumn<>("Net After Fee");
        netAfterFeeCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getNetAfterServiceFeeFormatted()));
        netAfterFeeCol.setPrefWidth(110);

        TableColumn<PayrollRow, String> fuelCol = new TableColumn<>("Fuel");
        fuelCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getFuelDeductedFormatted()));
        fuelCol.setPrefWidth(90);

        TableColumn<PayrollRow, String> netAfterFuelCol = new TableColumn<>("Net After Fuel");
        netAfterFuelCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getNetAfterFuelFormatted()));
        netAfterFuelCol.setPrefWidth(110);

        TableColumn<PayrollRow, String> driverPayCol = new TableColumn<>("Driver %");
        driverPayCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getDriverPayFormatted()));
        driverPayCol.setPrefWidth(90);

        TableColumn<PayrollRow, String> companyPayCol = new TableColumn<>("Company %");
        companyPayCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getCompanyPayFormatted()));
        companyPayCol.setPrefWidth(90);

        TableColumn<PayrollRow, String> adjustmentsCol = new TableColumn<>("Adjustments");
        adjustmentsCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getAdjustmentsFormatted()));
        adjustmentsCol.setPrefWidth(100);

        TableColumn<PayrollRow, String> recurringCol = new TableColumn<>("Recurring Fees");
        recurringCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getRecurringFeesFormatted()));
        recurringCol.setPrefWidth(110);

        TableColumn<PayrollRow, String> advanceCol = new TableColumn<>("Cash Advance Repay");
        advanceCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getAdvanceRepaymentFormatted()));
        advanceCol.setPrefWidth(120);

        TableColumn<PayrollRow, String> netPayCol = new TableColumn<>("Net Pay");
        netPayCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getNetPayFormatted()));
        netPayCol.setPrefWidth(110);

        TableColumn<PayrollRow, String> lockCol = new TableColumn<>("Locked");
        lockCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().isLocked() ? "Yes" : "No"));
        lockCol.setPrefWidth(70);

        TableColumn<PayrollRow, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button previewBtn = new Button("Preview");
            private final Button exportBtn = new Button("Export PDF");
            private final Button adjustmentsBtn = new Button("Adjust");

            {
                previewBtn.setOnAction(e -> showPaystubPreview(getTableView().getItems().get(getIndex())));
                exportBtn.setOnAction(e -> exportPaystubPDF(getTableView().getItems().get(getIndex())));
                adjustmentsBtn.setOnAction(e -> showAdjustmentsDialog(getTableView().getItems().get(getIndex())));
                previewBtn.setTooltip(new Tooltip("Preview paystub"));
                exportBtn.setTooltip(new Tooltip("Export paystub to PDF"));
                adjustmentsBtn.setTooltip(new Tooltip("Adjust earnings/deductions"));

                previewBtn.setPrefWidth(55);
                exportBtn.setPrefWidth(72);
                adjustmentsBtn.setPrefWidth(55);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(3, previewBtn, exportBtn, adjustmentsBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        actionsCol.setPrefWidth(180);

        table.getColumns().addAll(driverCol, grossCol, serviceFeeCol, netAfterFeeCol, fuelCol,
                netAfterFuelCol, driverPayCol, companyPayCol, adjustmentsCol, recurringCol, advanceCol,
                netPayCol, lockCol, actionsCol);

        setCenter(table);

        pagination = new Pagination(1, 0);
        pagination.setMaxHeight(32);
        pagination.currentPageIndexProperty().addListener((obs, oldIdx, newIdx) -> updatePage());
        HBox.setHgrow(pagination, Priority.ALWAYS);

        setBottom(pagination);

        loadRows(new ArrayList<>());
    }

    /** Loads all driver rows */
    public void loadRows(List<PayrollRow> rows) {
        this.allRows = rows;
        applyFilter();
    }

    /** Applies filter based on search text */
    private void applyFilter() {
        String filterText = filterField.getText();
        if (filterText == null || filterText.isBlank()) {
            filteredRows = allRows;
        } else {
            filteredRows = allRows.stream()
                    .filter(r -> r.getDriverName().toLowerCase().contains(filterText.toLowerCase()))
                    .collect(Collectors.toList());
        }
        int pageCount = Math.max(1, (int) Math.ceil(filteredRows.size() / (double) ROWS_PER_PAGE));
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        updatePage();
    }

    /** Updates table for the current page */
    private void updatePage() {
        int pageIndex = pagination.getCurrentPageIndex();
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredRows.size());
        ObservableList<PayrollRow> pageRows = FXCollections.observableArrayList(
                filteredRows.subList(fromIndex, toIndex)
        );
        table.setItems(pageRows);
    }

    // Action stubs
    private void showPaystubPreview(PayrollRow row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Preview for " + row.getDriverName(), ButtonType.OK);
        alert.setTitle("Paystub Preview");
        alert.setHeaderText("Paystub Preview");
        alert.showAndWait();
    }
    private void exportPaystubPDF(PayrollRow row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported PDF for " + row.getDriverName(), ButtonType.OK);
        alert.setTitle("Export PDF");
        alert.setHeaderText("Export PDF");
        alert.showAndWait();
    }
    private void showAdjustmentsDialog(PayrollRow row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Adjustments for " + row.getDriverName(), ButtonType.OK);
        alert.setTitle("Adjustments");
        alert.setHeaderText("Adjustments");
        alert.showAndWait();
    }

    // Export all action stub
    public void exportAllPaystubs() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported all paystubs for week", ButtonType.OK);
        alert.setTitle("Export All Paystubs");
        alert.setHeaderText("Export All Paystubs");
        alert.showAndWait();
    }
}