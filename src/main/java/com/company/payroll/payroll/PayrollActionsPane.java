package com.company.payroll.payroll;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * Bottom bar for lock, export, refresh actions.
 */
public class PayrollActionsPane extends HBox {

    public PayrollActionsPane(Runnable onLock, Runnable onExportAll, Runnable onRefresh) {
        setSpacing(12);
        setPadding(new Insets(16, 0, 0, 0));
        setAlignment(Pos.CENTER_LEFT);

        Button lockBtn = new Button("Lock Payroll");
        Button exportBtn = new Button("Export All Paystubs");
        Button refreshBtn = new Button("Refresh");

        lockBtn.setOnAction(e -> onLock.run());
        exportBtn.setOnAction(e -> onExportAll.run());
        refreshBtn.setOnAction(e -> onRefresh.run());

        getChildren().addAll(lockBtn, exportBtn, refreshBtn);
    }
}