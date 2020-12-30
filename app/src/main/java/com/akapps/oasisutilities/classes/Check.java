package com.akapps.oasisutilities.classes;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Check {
    private String companyName;
    private double checkAmount;
    private String checkDate;
    private String checkCashedBy;
    private boolean fakeCheck;
    private String checkPath;
    private int checkID;

    public Check(){}

    public Check(String companyName, double checkAmount, String checkCashedBy, int checkID) {
        this.companyName = companyName;
        this.checkAmount = checkAmount;
        this.checkDate = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss aa").format(new Date());
        this.checkCashedBy = checkCashedBy;
        this.fakeCheck = false;
        this.checkID = checkID;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCheckName() {
        return companyName;
    }

    public void setCheckName(String companyName) {
        this.companyName = companyName;
    }

    public double getCheckAmount() {
        return checkAmount;
    }

    public void setCheckAmount(double checkAmount) {
        this.checkAmount = checkAmount;
    }

    public String getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(String checkDate) {
        this.checkDate = checkDate;
    }

    public String getCheckCashedBy() {
        return checkCashedBy;
    }

    public void setCheckCashedBy(String checkCashedBy) {
        this.checkCashedBy = checkCashedBy;
    }

    public boolean isFakeCheck() {
        return fakeCheck;
    }

    public void setFakeCheck(boolean fakeCheck) {
        this.fakeCheck = fakeCheck;
    }

    public String getCheckPath() {
        return checkPath;
    }

    public void setCheckPath(String checkPath) {
        this.checkPath = checkPath;
    }

    public int getCheckID() {
        return checkID;
    }

    public void setCheckID(int checkID) {
        this.checkID = checkID;
    }
}
