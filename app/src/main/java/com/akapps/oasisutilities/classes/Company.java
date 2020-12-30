package com.akapps.oasisutilities.classes;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Company {
    private String companyName;
    private String checkPath;
    private String dateAdded;
    private String store;
    private boolean cashStatus;

    public Company(){}

    public Company(String name, String store){
        companyName = name;
        checkPath = null;
        dateAdded = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss aa").format(new Date());
        this.store = store;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCheckPath() {
        return checkPath;
    }

    public void setCheckPath(String checkPath) {
        this.checkPath = checkPath;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public boolean isCashStatus() {
        return cashStatus;
    }

    public void setCashStatus(boolean cashStatus) {
        this.cashStatus = cashStatus;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }
}
