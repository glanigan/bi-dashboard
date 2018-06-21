package com.bi.dashboard;

import com.bi.dashboard.services.DataService;
import com.bi.dashboard.models.SaleRecord;
import com.bi.dashboard.models.User;
import com.bi.dashboard.services.RefreshService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class DashboardController implements Initializable {

    @FXML
    private MenuItem menuRefreshData, menuSetRefresh;
    @FXML
    private CheckMenuItem menuAutoRefresh;
    @FXML
    private Text lastUpdated, totalSales, highestSellingRegion, bestSellingVehicle, estimatedRevenue, estimatedProfit, usSales,europeSales,asiaSales;
    @FXML
    private TableView<SaleRecord> dataTable;
    @FXML
    private Pane loadingDash, loadingData;
    @FXML
    private VBox dataPane, dashboardPane;
    @FXML
    private HBox barChartHBox, lineChartHBox, footerLoading;
    @FXML
    private PieChart pieChart;
    @FXML
    private BarChart<?, ?> barChart;
    @FXML
    private LineChart lineChart;
    @FXML
    private TableColumn<SaleRecord, ?> columnQuarter, columnRegion, columnVehicle, columnYear, columnQuantity;
    @FXML
    private ChoiceBox yearSelector, qtrSelector;

    private CheckBox[] checkboxes, lineCheckboxes;

    private User user;
    private DataService dataService;
    private RefreshService refreshService;

    private List<SaleRecord> sales;

    private List<Integer> distinctQTR;
    private List<Integer> distinctYear;
    private List<String> distinctRegions;
    private List<String> selectQTRItems;
    private List<String> selectYearItems;

    private double averageVehicleValue = 22000;
    private double costPerSale = 15651.38;

    @Override
    public final void initialize(URL url, ResourceBundle rb) {

        //Below code configures and starts data service//
        dataService = new DataService();
        dataService.setSourceURL("http://glynserver.cms.livjm.ac.uk/DashService/SalesGetSales");
        dataService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent e) {
                sales = (new Gson()).fromJson(e.getSource().getValue().toString(), new TypeToken<LinkedList<SaleRecord>>() {
                }.getType());

                distinctYear = sales.stream().map(s -> s.getYear()).distinct().collect(Collectors.toList());
                distinctQTR = sales.stream().map(s -> (s.getQTR())).distinct().collect(Collectors.toList());
                distinctRegions = sales.stream().map(s -> (s.getRegion())).distinct().collect(Collectors.toList());

                selectYearItems = distinctYear.stream().map(o -> o.toString()).collect(Collectors.toList());
                selectYearItems.add("All");
                selectQTRItems = distinctQTR.stream().map(o -> o.toString()).collect(Collectors.toList());
                selectQTRItems.add("All");

                yearSelector.setItems(FXCollections.observableArrayList(selectYearItems));
                qtrSelector.setItems(FXCollections.observableArrayList(selectQTRItems));

                yearSelector.getSelectionModel().selectLast();
                qtrSelector.getSelectionModel().selectLast();

                dataTable.setItems(FXCollections.observableArrayList(sales));
                setBarChart(sales);
                setLineChart(sales);
                updateLastUpdatedTime();
            }
        });
        dataService.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Data loading error");
                alert.setHeaderText("Data loading error");
                alert.setContentText("Application unable to connect to data service. Please check internet connection and try again.");
                alert.showAndWait();
            }
        });
        dataService.start();

        //Below code configures and starts timed refresh service//
        refreshService = new RefreshService();
        refreshService.setWaitTime(60000);
        refreshService.start();
        menuAutoRefresh.selectedProperty().addListener(e -> {
            if (menuAutoRefresh.isSelected() && !refreshService.isRunning()) {
                refreshService.restart();
            } else {
                refreshService.cancel();
            }
        });

        //Starts data service when refreshService has completed wait time//
        refreshService.messageProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                refreshData();
            }
        });

        //Set menu bindings
        menuRefreshData.disableProperty().bind(dataService.runningProperty());
        menuAutoRefresh.disableProperty().bind(dataService.runningProperty());
        menuSetRefresh.disableProperty().bind(dataService.runningProperty());

        //Set progress indicator bindings
        loadingDash.visibleProperty().bindBidirectional(loadingData.visibleProperty());
        dataPane.visibleProperty().bindBidirectional(dashboardPane.visibleProperty());
        loadingDash.visibleProperty().bind(dataService.runningProperty());
        dataPane.visibleProperty().bind(dataService.runningProperty().not());
        footerLoading.visibleProperty().bind(dataService.runningProperty());

        //Sets table columns
        columnQuarter.setCellValueFactory(new PropertyValueFactory<>("QTR"));
        columnRegion.setCellValueFactory(new PropertyValueFactory<>("Region"));
        columnVehicle.setCellValueFactory(new PropertyValueFactory<>("Vehicle"));
        columnYear.setCellValueFactory(new PropertyValueFactory<>("Year"));
        columnQuantity.setCellValueFactory(new PropertyValueFactory<>("Quantity"));

        yearSelector.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newYear) {
                if (yearSelector.getSelectionModel().getSelectedItem() != null) {
                    setDashboardByYearQTR();
                } else {
                    yearSelector.getSelectionModel().select(oldValue);
                }
            }
        });
        qtrSelector.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newQTR) {
                if (newQTR != null) {
                    setDashboardByYearQTR();
                } else {
                    qtrSelector.getSelectionModel().select(oldValue);
                }
            }
        });
    }

    private int calculateTotalSales(List<SaleRecord> list) {
        return list.stream().collect(Collectors.summingInt(SaleRecord::getQuantity));
    }
    private double calculateRevenue(int totalSales, double averageVehicleValue) {
        return Math.round(totalSales * averageVehicleValue);
    }

    private double calculateProfit(int totalSales, double averageVehicleValue, double costPerSale) {

        return totalSales * averageVehicleValue - totalSales * costPerSale;
    }

    private void setDashboardByYearQTR() {
        if (yearSelector.getSelectionModel().getSelectedItem().toString().equals("All")) {
            qtrSelector.setDisable(true);
            qtrSelector.getSelectionModel().selectLast();
            setCoreDashboardItems(sales);
        } else {
            qtrSelector.setDisable(false);
            List<SaleRecord> salesByYear = sales.stream().filter(s -> s.getYear() == Integer.parseInt(yearSelector.getSelectionModel().getSelectedItem().toString())).collect(Collectors.toList());
            if (qtrSelector.getSelectionModel().getSelectedItem().toString().equals("All")) {
                setCoreDashboardItems(salesByYear);
            } else {
                List<SaleRecord> salesByYearQTR = salesByYear.stream().filter(s -> s.getQTR() == Integer.parseInt(qtrSelector.getSelectionModel().getSelectedItem().toString())).collect(Collectors.toList());
                setCoreDashboardItems(salesByYearQTR);
            }
        }
    }

    private void setCoreDashboardItems(List<SaleRecord> list) {
        int numberOfSales = calculateTotalSales(list);
        setTotalSales(numberOfSales);
        setRevenue(calculateRevenue(numberOfSales, averageVehicleValue));
        setProfit(calculateProfit(numberOfSales, averageVehicleValue, costPerSale));
        setHighestSellingRegion(list);
        setBestSellingVehicle(list);
        setPieChart(list);
        setUSSales(list);
        setEuropeSales(list);
        setAsiaSales(list);
    }

    private void setTotalSales(int totalSales) {
        this.totalSales.setText(String.valueOf(totalSales));
    }
    private void setUSSales(List<SaleRecord> list) {
        int usSalesV = list.stream().filter(s -> s.getRegion().equals("America")).collect(Collectors.summingInt(SaleRecord::getQuantity));
        this.usSales.setText(String.valueOf(usSalesV));
    }
    private void setEuropeSales(List<SaleRecord> list) {
        int europeSalesV = list.stream().filter(s -> s.getRegion().equals("Europe")).collect(Collectors.summingInt(SaleRecord::getQuantity));
        this.europeSales.setText(String.valueOf(europeSalesV));
    }
    private void setAsiaSales(List<SaleRecord> list) {
        int asiaSalesV = list.stream().filter(s -> s.getRegion().equals("Asia")).collect(Collectors.summingInt(SaleRecord::getQuantity));
        this.asiaSales.setText(String.valueOf(asiaSalesV));
    }
    private void setRevenue(double revenue) {
        DecimalFormat df2 = new DecimalFormat(".##");
        estimatedRevenue.setText("£" + df2.format(revenue / 1000000) + "M");
    }

    private void setProfit(double profit) {
        DecimalFormat df2 = new DecimalFormat(".##");

        estimatedProfit.setText("£" + df2.format(profit / 1000000) + "M");
    }

    private void setHighestSellingRegion(List<SaleRecord> list) {
        Map<String, Integer> map = list.stream().collect(Collectors.groupingBy(SaleRecord::getRegion, Collectors.summingInt(SaleRecord::getQuantity)));
        highestSellingRegion.setText(Collections.max(map.entrySet(), Map.Entry.comparingByValue()).getKey());
    }

    private void setBestSellingVehicle(List<SaleRecord> list) {
        Map<String, Integer> map = list.stream().collect(Collectors.groupingBy(SaleRecord::getVehicle, Collectors.summingInt(SaleRecord::getQuantity)));
        bestSellingVehicle.setText(Collections.max(map.entrySet(), Map.Entry.comparingByValue()).getKey());
    }

    private void setPieChart(List<SaleRecord> list) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        Map<String, Integer> data = list.stream().collect(Collectors.groupingBy(SaleRecord::getRegion, Collectors.summingInt(SaleRecord::getQuantity)));
        data.entrySet().forEach((entry) -> {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        });
        this.pieChart.setData(pieChartData);
    }

    private void setBarChart(List<SaleRecord> list) {
        barChartHBox.getChildren().clear();
        checkboxes = new CheckBox[distinctRegions.size()];

        for (byte index = 0; index < distinctRegions.size(); index++) {
            checkboxes[index] = new CheckBox(String.valueOf(distinctRegions.get(index)));
            checkboxes[index].setSelected(true);
            checkboxes[index].addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                }
            });
            checkboxes[index].addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                }
            });
            checkboxes[index].setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    constructBarChartSeries(list);
                }
            });

            barChartHBox.getChildren().add(checkboxes[index]);
        }
        constructBarChartSeries(list);
    }

    private void constructBarChartSeries(List<SaleRecord> list) {
        Map<String, Map<String, Integer>> map = list.stream().collect(Collectors.groupingBy(SaleRecord::getRegion, Collectors.groupingBy(SaleRecord::getVehicle, Collectors.summingInt(SaleRecord::getQuantity))));
        barChart.getData().clear();
        for (CheckBox checkbox : checkboxes) {
            if (checkbox.isSelected()) {
                XYChart.Series series = new XYChart.Series();
                series.setName(checkbox.getText());

                map.entrySet().forEach((entry) -> {
                    if (entry.getKey().equals(checkbox.getText())) {
                        entry.getValue().entrySet().forEach((innerEntry) -> {
                            series.getData().add(new XYChart.Data<>(innerEntry.getKey(), innerEntry.getValue()));
                        });
                    }
                });
                barChart.getData().add(series);
            }
        }
    }

    private void setLineChart(List<SaleRecord> list) {
        lineChartHBox.getChildren().clear();
        lineCheckboxes = new CheckBox[distinctRegions.size()];
        for (byte index = 0; index < distinctRegions.size(); index++) {
            lineCheckboxes[index] = new CheckBox(distinctRegions.get(index));
            lineCheckboxes[index].setSelected(true);

            lineCheckboxes[index].setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    createLineChart(list);
                }
            });

            lineChartHBox.getChildren().add(lineCheckboxes[index]);
        }
        createLineChart(list);
    }

    private void createLineChart(List<SaleRecord> list) {
        Map<String, Map<String, Integer>> map = list.stream().collect(Collectors.groupingBy(SaleRecord::getRegion, Collectors.groupingBy(SaleRecord::getYearString, Collectors.summingInt(SaleRecord::getQuantity))));

        lineChart.getData().clear();
        for (CheckBox checkbox : lineCheckboxes) {
            if (checkbox.isSelected()) {
                XYChart.Series series = new XYChart.Series();
                series.setName(checkbox.getText());

                map.entrySet().forEach((entry) -> {
                    if (entry.getKey().equals(checkbox.getText())) {
                        entry.getValue().entrySet().forEach((innerEntry) -> {
                            series.getData().add(new XYChart.Data<>(innerEntry.getKey(), innerEntry.getValue()));
                        });
                    }
                });
                lineChart.getData().add(series);
            }
        }

    }

    protected final void setUser(User user) {
        this.user = user;
    }

    private void updateLastUpdatedTime() {
        lastUpdated.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    @FXML
    protected final void refreshData() {
        dataService.restart();
    }

    @FXML
    protected final void cancelDataService() {
        dataService.cancel();
    }

    @FXML
    protected final void displaySetRefreshTimeMenu() {
        double currentValue = refreshService.getWaitTime() / 60000.0;
        TextInputDialog dialog = new TextInputDialog(String.valueOf(currentValue));
        dialog.setTitle("Refresh Wait Time");
        dialog.setHeaderText("Configure Refresh Wait Time");
        dialog.setContentText("Please enter the new refresh wait time: (mins)");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(value -> {
            try {
                refreshService.setWaitTime((int) Math.round(Double.parseDouble(value) * 60000));
                refreshService.restart();
            } catch (NumberFormatException e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Input Error");
                alert.setHeaderText("Input Error");
                alert.setContentText("Please insert the value in minuites:");
                alert.showAndWait();
            }
        });
    }

    @FXML
    protected final void displayVehicleValueMenu() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(averageVehicleValue));
        dialog.setTitle("Average Vehicle Value");
        dialog.setHeaderText("Configure Average Vehicle Value");
        dialog.setContentText("Please enter the new Average Vehicle Value: (£)");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(value -> {
            try {
                averageVehicleValue = Double.parseDouble(value);
                setDashboardByYearQTR();
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Input Error");
                alert.setHeaderText("Input Error");
                alert.setContentText("Please insert a number value:");
                alert.showAndWait();
            }
        });
    }

    @FXML
    protected final void displayCostPerSaleMenu() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(costPerSale));
        dialog.setTitle("Cost-Per-Sale");
        dialog.setHeaderText("Configure Cost-Per-Sale");
        dialog.setContentText("Please enter the new Cost-Per-Sale Value: (£)");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(value -> {
            try {
                costPerSale = Double.parseDouble(value);
                setDashboardByYearQTR();
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Input Error");
                alert.setHeaderText("Input Error");
                alert.setContentText("Please insert a number value:");
                alert.showAndWait();
            }
        });
    }

    @FXML
    protected final void about() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Application Information");
        alert.setContentText("This application is a business intelligence dashboard containing information on car sales.");
        alert.showAndWait();
    }

    @FXML
    protected final void logout() {
        if (this.refreshService.isRunning()) {
            refreshService.cancel();
        }
        this.user = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("LoginFXML.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) lastUpdated.getScene().getWindow();
            stage.setScene(scene);
            stage.setHeight(400);
            stage.setWidth(660);
            stage.centerOnScreen();
            stage.setResizable(false);
            stage.show();

        } catch (IOException ex) {
            Logger.getLogger(LoginController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    protected final void close() {
        Platform.exit();
    }
}
