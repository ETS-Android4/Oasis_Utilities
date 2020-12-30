package com.akapps.oasisutilities.classes;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Report{

    // status
    private boolean isCompleted;
    private boolean customReport;
    private String datesUsed;
    private String startDate;
    private String endDate;
    private String dateAdded;

    // store
    private String store;

    // date
    private int month;
    private int day;
    private int year;

    // gas
    private double gasGallons;
    private double gasAmount;
    private double dieselGallons;
    private double dieselAmount;

    // sales information (column 1)
    private double atmFee;
    private double beerWine;
    private double beverages;
    private double checkFee;
    private double cigarettes;
    private double coffee;
    private double energyDrinks;
    private double hotFood;
    private double lottery;
    private double moneyOrder;
    private double nonTaxable;
    private double propane;
    private double soda;
    private double taxable;
    private double salesTax;
    private double totalSales;

    // sales information (column 2)
    private double closingCash;
    private double checks;
    private double credit;
    private double debit;
    private double lotto;
    private double secondCreditMachine;
    private double ebt;
    private double paidOut;
    private double aR;
    private double atm;
    private double checksDeposited;

    public Report() {
        dateAdded = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss aa").format(new Date());
    }

    public Report(int day, int month, int year, String store) {
        this.month = month;
        this.day = day;
        this.year = year;
        isCompleted = false;
        customReport = false;
        this.store = store;
        datesUsed = "";
        dateAdded = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss aa").format(new Date());
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Date getDate(){
        DateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");
        try {
            return formatter.parse(month + "-" + day + "-" + year);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getDatesUsed() {
        return datesUsed;
    }

    public void setDatesUsed(String dateUsed) {
        this.datesUsed = dateUsed;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setDateRange(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getGasGallons() {
        return gasGallons;
    }

    public void setGasGallons(double gasGallons) {
        this.gasGallons = gasGallons;
    }

    public double getGasAmount() {
        return gasAmount;
    }

    public void setGasAmount(double gasAmount) {
        this.gasAmount = gasAmount;
    }

    public double getDieselGallons() {
        return dieselGallons;
    }

    public void setDieselGallons(double dieselGallons) {
        this.dieselGallons = dieselGallons;
    }

    public double getDieselAmount() {
        return dieselAmount;
    }

    public void setDieselAmount(double dieselAmount) {
        this.dieselAmount = dieselAmount;
    }

    public double getAtmFee() {
        return atmFee;
    }

    public void setAtmFee(double atmFee) {
        this.atmFee = atmFee;
    }

    public double getBeerWine() {
        return beerWine;
    }

    public void setBeerWine(double beerWine) {
        this.beerWine = beerWine;
    }

    public double getBeverages() {
        return beverages;
    }

    public void setBeverages(double beverages) {
        this.beverages = beverages;
    }

    public double getCheckFee() {
        return checkFee;
    }

    public void setCheckFee(double checkFee) {
        this.checkFee = checkFee;
    }

    public double getCigarettes() {
        return cigarettes;
    }

    public void setCigarettes(double cigarettes) {
        this.cigarettes = cigarettes;
    }

    public double getCoffee() {
        return coffee;
    }

    public void setCoffee(double coffee) {
        this.coffee = coffee;
    }

    public double getEnergyDrinks() {
        return energyDrinks;
    }

    public void setEnergyDrinks(double energyDrinks) {
        this.energyDrinks = energyDrinks;
    }

    public double getHotFood() {
        return hotFood;
    }

    public void setHotFood(double hotFood) {
        this.hotFood = hotFood;
    }

    public double getLottery() {
        return lottery;
    }

    public void setLottery(double lottery) {
        this.lottery = lottery;
    }

    public double getMoneyOrder() {
        return moneyOrder;
    }

    public void setMoneyOrder(double moneyOrder) {
        this.moneyOrder = moneyOrder;
    }

    public double getNonTaxable() {
        return nonTaxable;
    }

    public void setNonTaxable(double nonTaxable) {
        this.nonTaxable = nonTaxable;
    }

    public double getPropane() {
        return propane;
    }

    public void setPropane(double propane) {
        this.propane = propane;
    }

    public double getSoda() {
        return soda;
    }

    public void setSoda(double soda) {
        this.soda = soda;
    }

    public double getTaxable() {
        return taxable;
    }

    public void setTaxable(double taxable) {
        this.taxable = taxable;
    }

    public double getSalesTax() {
        return salesTax;
    }

    public void setSalesTax(double salesTax) {
        this.salesTax = salesTax;
    }

    public double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(double totalSales) {
        this.totalSales = totalSales;
    }

    public double getClosingCash() {
        return closingCash;
    }

    public void setClosingCash(double closingCash) {
        this.closingCash = closingCash;
    }

    public double getChecks() {
        return checks;
    }

    public void setChecks(double checks) {
        this.checks = checks;
    }

    public double getCredit() {
        return credit;
    }

    public void setCredit(double credit) {
        this.credit = credit;
    }

    public double getDebit() {
        return debit;
    }

    public void setDebit(double debit) {
        this.debit = debit;
    }

    public double getLotto() {
        return lotto;
    }

    public void setLotto(double lotto) {
        this.lotto = lotto;
    }

    public double getSecondCreditMachine() {
        return secondCreditMachine;
    }

    public void setSecondCreditMachine(double secondCreditMachine) {
        this.secondCreditMachine = secondCreditMachine;
    }

    public double getEbt() {
        return ebt;
    }

    public void setEbt(double ebt) {
        this.ebt = ebt;
    }

    public double getPaidOut() {
        return paidOut;
    }

    public void setPaidOut(double paidOut) {
        this.paidOut = paidOut;
    }

    public double getaR() {
        return aR;
    }

    public void setaR(double aR) {
        this.aR = aR;
    }

    public double getAtm() {
        return atm;
    }

    public void setAtm(double atm) {
        this.atm = atm;
    }

    public double getChecksDeposited() {
        return checksDeposited;
    }

    public void setChecksDeposited(double checksDeposited) {
        this.checksDeposited = checksDeposited;
    }

    public boolean isCustomReport() {
        return customReport;
    }

    public void setCustomReport(boolean customReport) {
        this.customReport = customReport;
    }
}
