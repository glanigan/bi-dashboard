/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bi.dashboard;

import com.bi.dashboard.models.User;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * FXML Controller class
 *
 * @author cmpglani
 */
public class LoginController implements Initializable {

    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
    @FXML
    private TextField fieldUsername;
    @FXML
    private PasswordField fieldPassword;
    @FXML
    private Button btnLogin;
    private final List<User> users = new LinkedList();
    private User user;
    private int attempts;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        users.add(new User("admin", "password"));

        this.resources = rb;
        this.location = url;
        this.attempts = 0;
    }

    @FXML
    protected final void login() {
        String username = fieldUsername.getText();
        String password = fieldPassword.getText();
        if (username.length() > 0 && password.length() > 0) {
            user = users.stream()
                    .filter(x -> x.authenticUser(username, password))
                    .findFirst()
                    .orElse(null);
            if (user != null) {
                
                startDashboard();
            } 
            else {
                attempts++;
                if (attempts == 3) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.showAndWait();
                    Platform.exit();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.showAndWait();
                }
            }
        }
    }
    private void startDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("DashboardFXML.fxml"));
            loader.load();
            DashboardController dashboardController = loader.getController();
            dashboardController.setUser(this.user);
            Parent root = loader.getRoot();
            Scene scene = new Scene(root);
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("BI Dashboard");
            stage.setResizable(true);
            stage.show();
            stage.setHeight(600);
            stage.setWidth(800);
            stage.centerOnScreen();
        } catch (IOException ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
