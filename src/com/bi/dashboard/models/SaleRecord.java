/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bi.dashboard.models;

import java.time.LocalDate;

/**
 *
 * @author garyl
 */
public class SaleRecord {
    private String  Region, Vehicle;
    private int QTR, Quantity,Year;
    private LocalDate date;

    public int getQTR() {
        return QTR;
    }
    public String getRegion() {
        return Region;
    }

    public String getVehicle() {
        return Vehicle;
    }

    public int getYear() {
        return Year;
    }
    public String getYearString(){
        return String.valueOf(Year);
    }
         
    public int getQuantity() {
        return Quantity;
    }
    public LocalDate getDate(){
        return date;
    }   
}
