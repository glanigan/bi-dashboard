/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bi.dashboard.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author garyl
 */
public class DataService extends Service {

    private String url;

    public DataService() {

    }

    @Override
    protected Task createTask() {

        return new Task() {
            private URL url;
            private HttpURLConnection connect;
            String json = "";

            @Override
            protected String call() {
                try {
                    url = new URL(getSourceURL());
                    connect = (HttpURLConnection) url.openConnection();
                    connect.setRequestMethod(
                            "GET");
                    connect.setRequestProperty(
                            "Accept", "application/json");
                    connect.setRequestProperty(
                            "Content-Type", "application/json");
                    json = (new BufferedReader(new InputStreamReader(connect.getInputStream()))).readLine();
                } catch (IOException e) {
                    
                } finally {
                    if (connect != null) {
                        connect.disconnect();
                    }
                    return json;
                }
            }
        };
    }

    public void setSourceURL(String url) {
        this.url = url;

    }

    public String getSourceURL() {
        return this.url;
    }
}
