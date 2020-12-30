package com.akapps.oasisutilities.classes;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Customer {
    private String fullName;
    private String idPath;
    private String dateAdded;
    private String store;
    private int checksCashed;
    private boolean cashedFakeCheck;
    private int lastFourNumID;

    public Customer(){}

    public Customer(String fullName, int lastFourNumID, String store) {
        this.fullName = fullName;
        this.cashedFakeCheck = false;
        idPath = null;
        dateAdded = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss aa").format(new Date());
        checksCashed = 0;
        this.lastFourNumID = lastFourNumID;
        this.store = store;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isCashedFakeCheck() {
        return cashedFakeCheck;
    }

    public void setCashedFakeCheck(boolean cashedFakeCheck) {
        this.cashedFakeCheck = cashedFakeCheck;
    }

    public String getIdPath() {
        return idPath;
    }

    public void setIdPath(String idPath) {
        this.idPath = idPath;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public int getChecksCashed() {
        return checksCashed;
    }

    public void setChecksCashed(int checksCashed) {
        this.checksCashed = checksCashed;
    }

    public int getLastFourNumID() {
        return lastFourNumID;
    }

    public void setLastFourNumID(int lastFourNumID) {
        this.lastFourNumID = lastFourNumID;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }
}
