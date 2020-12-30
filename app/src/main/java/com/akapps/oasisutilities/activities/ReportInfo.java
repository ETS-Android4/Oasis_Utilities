package com.akapps.oasisutilities.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.akapps.oasisutilities.classes.Helper;
import com.akapps.oasisutilities.classes.PdfDocumentAdapter;
import com.akapps.oasisutilities.classes.Report;
import com.akapps.oasisutilities.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.Locale;

public class ReportInfo extends AppCompatActivity {

    private Context context;
    private String currentReportName;
    private String reportDate;
    private String dateFormatted;
    private MaterialButton createReport;
    private Report currentReport;
    private String currentStore;
    private String pdfPath;
    private FloatingActionButton saveFields;

    // report dialog
    private PDFView pdfView;
    private MaterialDialog viewReportDialog;

    // database
    private FirebaseFirestore db;
    private CollectionReference reportReference;
    private Dialog progressDialog;

    private boolean customReport;

    //Date
    TextView storeName;
    TextView date;
    // col 1
    private EditText gasGallons;
    private EditText gasAmount;
    private EditText dieselGallons;
    private EditText dieselAmount;
    private EditText atmFee;
    private EditText beerWine;
    private EditText beverages;
    private EditText checkFee;
    private EditText cigarettes;
    private EditText coffee;
    private EditText energyDrinks;
    private EditText hotFood;
    private EditText lottery;
    private EditText moneyOrder;
    private EditText nonTaxable;
    private EditText propane;
    private EditText soda;
    private EditText taxable;
    private EditText salesTax;
    private EditText totalSales;
    // col 2
    private EditText closingCash;
    private EditText checks;
    private EditText credit;
    private EditText debit;
    private EditText lotto;
    private EditText secondCreditMachine;
    private EditText ebt;
    private EditText paidOut;
    private EditText aR;
    private EditText atm;
    private EditText checksDeposited;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        context = this;

        currentReportName = getIntent().getStringExtra("currentReport");
        currentStore = getIntent().getStringExtra("currentStore");
        customReport = getIntent().getBooleanExtra("customReport", false);
        reportDate = currentReportName.replace("_", "-");

        pdfPath = Helper.getAppPath(context) + currentReportName +".pdf";

        progressDialog = Helper.showLoading(progressDialog, context, true);
        initializeDatabase();
        initializeLayout();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Helper.showLoading(progressDialog, context, false);
            }
        }, 1000);
    }

    @Override
    public void onSaveInstanceState (Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        if(currentReport.isCompleted() && !customReport)
            validInput();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(progressDialog!=null)
            progressDialog.cancel();
        if(viewReportDialog!=null)
            viewReportDialog.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_report, menu);

        if(!customReport)
            menu.getItem(2).setVisible(false);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.actionViewReports){
            if(customReport) {
                new MaterialDialog.Builder(context)
                        .title("Report Days Used:")
                        .titleColor(context.getColor(R.color.orange_red))
                        .content(currentReport.getDatesUsed())
                        .contentGravity(GravityEnum.CENTER)
                        .backgroundColor(context.getColor(R.color.black))
                        .positiveText("CLOSE")
                        .canceledOnTouchOutside(false)
                        .autoDismiss(false)
                        .onPositive((dialog, which) -> dialog.dismiss())
                        .positiveColor(context.getColor(R.color.gray))
                        .contentColor(context.getColor(R.color.colorPrimaryDark))
                        .show();
            }
        }
        else if(item.getItemId() == R.id.actionPrint){
            if(currentReport.isCompleted())
                printPDF();
            else
                Helper.showUserMessage(findViewById(android.R.id.content), "Create Report to Print", Snackbar.LENGTH_LONG, false);
        }
        else if(item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        else if(item.getItemId() == R.id.actionShare){
            if(currentReport.isCompleted()) {
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                File fileWithinMyDir = new File(pdfPath);

                if (fileWithinMyDir.exists()) {
                    Uri uri = FileProvider.getUriForFile(context, getString(R.string.fileprovider), fileWithinMyDir);
                    String mimeType = "application/pdf";
                    String[] mimeTypeArray = new String[] { mimeType };
                    intentShareFile.setType(mimeType);

                    intentShareFile.setClipData(new ClipData(
                            "Report for " + dateFormatted,
                            mimeTypeArray,
                            new ClipData.Item(uri)
                    ));

                    intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Report for " + reportDate);
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Attaching report for " + reportDate);
                    intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intentShareFile, "Share File"));
                }
                else
                    Helper.showUserMessage(findViewById(android.R.id.content), "Error: File not found", Snackbar.LENGTH_LONG, false);
            }
            else
                Helper.showUserMessage(findViewById(android.R.id.content), "Create Report to Share", Snackbar.LENGTH_LONG, false);
        }
        return true;
    }

    private void initializeLayout(){
        createReport = findViewById(R.id.createReport);
        saveFields = findViewById(R.id.saveFields);

        date = findViewById(R.id.input_date);
        storeName = findViewById(R.id.storeName);
        // col 1
        gasGallons = findViewById(R.id.gasGallons);
        gasAmount = findViewById(R.id.gasAmount);
        dieselGallons = findViewById(R.id.dieselGallons);
        dieselAmount = findViewById(R.id.dieselAmount);
        atmFee = findViewById(R.id.atmFee);
        beerWine = findViewById(R.id.beerWine);
        beverages = findViewById(R.id.beverages);
        checkFee = findViewById(R.id.checkFee);
        cigarettes = findViewById(R.id.cigarettes);
        coffee = findViewById(R.id.coffee);
        energyDrinks = findViewById(R.id.energyDrinks);
        hotFood = findViewById(R.id.hotFood);
        lottery = findViewById(R.id.lottery);
        moneyOrder = findViewById(R.id.moneyOrder);
        nonTaxable = findViewById(R.id.nonTaxable);
        propane = findViewById(R.id.propane);
        soda = findViewById(R.id.soda);
        taxable = findViewById(R.id.taxable);
        salesTax = findViewById(R.id.salesTax);
        totalSales = findViewById(R.id.totalSales);
        // col 2
        closingCash = findViewById(R.id.closingCash);
        checks = findViewById(R.id.checks);
        credit = findViewById(R.id.credit);
        debit = findViewById(R.id.debit);
        lotto = findViewById(R.id.lotto);
        secondCreditMachine = findViewById(R.id.secondCreditMachine);
        ebt = findViewById(R.id.ebt);
        paidOut = findViewById(R.id.paidOut);
        aR = findViewById(R.id.aR);
        atm = findViewById(R.id.atm);
        checksDeposited = findViewById(R.id.checksDeposited);

        String[] currentDate = reportDate.split("-");

        if(currentReportName.length()>10)
            dateFormatted = getDateRange(currentReportName);
        else if(currentDate[0].equals("0"))
            dateFormatted = reportDate.substring(reportDate.length() - 4);
        else if(currentDate[1].equals("0"))
            dateFormatted = Helper.getMonthString(Integer.parseInt(currentDate[0])) + " " + reportDate.substring(reportDate.length() - 4);
        else
            dateFormatted = reportDate;

        date.setText("Date: " + dateFormatted);
        setTitle(dateFormatted);

        storeName.setText("Oasis Market - " + Helper.toUpperCase(currentStore));

        createReport.setOnClickListener(v -> {
            if(currentReport.isCompleted()) {
                createPDFFile(pdfPath);
                viewReport();
            }
            else {
                if(validInput())
                    Helper.showUserMessage(findViewById(android.R.id.content), "Report Created", Snackbar.LENGTH_LONG, false);
            }
            Helper.hideSoftKeyboard(this);
        });

        saveFields.setOnClickListener(v -> {
            if(validInput()) {
                if(saveChanges()) {
                    Helper.showUserMessage(findViewById(android.R.id.content), "Saved", Snackbar.LENGTH_LONG, false);
                    createPDFFile(pdfPath);
                }
                else
                    Helper.showUserMessage(findViewById(android.R.id.content), "No changes to save", Snackbar.LENGTH_LONG, false);
            }
            else
                Helper.showUserMessage(findViewById(android.R.id.content), "Fix Errors", Snackbar.LENGTH_LONG, false);
            Helper.hideSoftKeyboard(this);
        });
    }

    private String getDateRange(String currentReportName){
        String startDate = currentReportName.split("_")[0];
        String endDate = currentReportName.split("_")[2];
        return Helper.intDateToString(startDate) + " to " +
                Helper.intDateToString(endDate);
    }

    private void viewReport(){
        viewReportDialog = new MaterialDialog.Builder(context)
                .title(dateFormatted)
                .titleColor(context.getColor(R.color.orange_red))
                .backgroundColor(context.getColor(R.color.black))
                .customView(R.layout.dialog_report, true)
                .positiveText("CLOSE")
                .canceledOnTouchOutside(false)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        viewReportDialog.dismiss();
                    }
                })
                .positiveColor(context.getColor(R.color.gray))
                .show();

        pdfView = (PDFView) viewReportDialog.findViewById(R.id.pdfView);

        pdfView.fromFile(new File(pdfPath))
                .fitEachPage(true)
                .load();
    }

    private void fillInputFields(){
        gasGallons.setText(Double.toString(currentReport.getGasGallons()));
        gasAmount.setText(Double.toString(currentReport.getGasAmount()));
        dieselGallons.setText(Double.toString(currentReport.getDieselGallons()));
        dieselAmount.setText(Double.toString(currentReport.getDieselAmount()));
        atmFee.setText(Double.toString(currentReport.getAtmFee()));
        beerWine.setText(Double.toString(currentReport.getBeerWine()));
        beverages.setText(Double.toString(currentReport.getBeverages()));
        checkFee.setText(Double.toString(currentReport.getCheckFee()));
        cigarettes.setText(Double.toString(currentReport.getCigarettes()));
        coffee.setText(Double.toString(currentReport.getCoffee()));
        energyDrinks.setText(Double.toString(currentReport.getEnergyDrinks()));
        hotFood.setText(Double.toString(currentReport.getHotFood()));
        lottery.setText(Double.toString(currentReport.getLottery()));
        moneyOrder.setText(Double.toString(currentReport.getMoneyOrder()));
        nonTaxable.setText(Double.toString(currentReport.getNonTaxable()));
        propane.setText(Double.toString(currentReport.getPropane()));
        soda.setText(Double.toString(currentReport.getSoda()));
        taxable.setText(Double.toString(currentReport.getTaxable()));
        salesTax.setText(Double.toString(currentReport.getSalesTax()));
        totalSales.setText(Double.toString(currentReport.getTotalSales()));
        closingCash.setText(Double.toString(currentReport.getClosingCash()));
        checks.setText(Double.toString(currentReport.getChecks()));
        credit.setText(Double.toString(currentReport.getCredit()));
        debit.setText(Double.toString(currentReport.getDebit()));
        lotto.setText(Double.toString(currentReport.getLotto()));
        secondCreditMachine.setText(Double.toString(currentReport.getSecondCreditMachine()));
        ebt.setText(Double.toString(currentReport.getEbt()));
        paidOut.setText(Double.toString(currentReport.getPaidOut()));
        aR.setText(Double.toString(currentReport.getaR()));
        atm.setText(Double.toString(currentReport.getAtm()));
        checksDeposited.setText(Double.toString(currentReport.getChecksDeposited()));
    }

    private boolean validInput(){
        if(gasGallons.getText().toString().equals("")){
            gasGallons.setError("Empty");
        }
        else if(gasAmount.getText().toString().equals("")){
            gasAmount.setError("Empty");
        }
        else if(dieselGallons.getText().toString().equals("")){
            dieselGallons.setError("Empty");
        }
        else if(dieselAmount.getText().toString().equals("")){
            dieselAmount.setError("Empty");
        }
        else if(atmFee.getText().toString().equals("")){
            atmFee.setError("Empty");
        }
        else if(beerWine.getText().toString().equals("")){
            beerWine.setError("Empty");
        }
        else if(beverages.getText().toString().equals("")){
            beverages.setError("Empty");
        }
        else if(checkFee.getText().toString().equals("")){
            checkFee.setError("Empty");
        }
        else if(cigarettes.getText().toString().equals("")){
            cigarettes.setError("Empty");
        }
        else if(coffee.getText().toString().equals("")){
            coffee.setError("Empty");
        }
        else if(energyDrinks.getText().toString().equals("")){
            energyDrinks.setError("Empty");
        }
        else if(hotFood.getText().toString().equals("")){
            hotFood.setError("Empty");
        }
        else if(lottery.getText().toString().equals("")){
            lottery.setError("Empty");
        }
        else if(moneyOrder.getText().toString().equals("")){
            moneyOrder.setError("Empty");
        }
        else if(nonTaxable.getText().toString().equals("")){
            nonTaxable.setError("Empty");
        }
        else if(propane.getText().toString().equals("")){
            propane.setError("Empty");
        }
        else if(soda.getText().toString().equals("")){
            soda.setError("Empty");
        }
        else if(taxable.getText().toString().equals("")){
            taxable.setError("Empty");
        }
        else if(salesTax.getText().toString().equals("")){
            salesTax.setError("Empty");
        }
        else if(totalSales.getText().toString().equals("")){
            totalSales.setError("Empty");
        }
        else if(closingCash.getText().toString().equals("")){
            closingCash.setError("Empty");
        }
        else if(checks.getText().toString().equals("")){
            checks.setError("Empty");
        }
        else if(credit.getText().toString().equals("")){
            credit.setError("Empty");
        }
        else if(debit.getText().toString().equals("")){
            debit.setError("Empty");
        }
        else if(lotto.getText().toString().equals("")){
            lotto.setError("Empty");
        }
        else if(secondCreditMachine.getText().toString().equals("")){
            secondCreditMachine.setError("Empty");
        }
        else if(ebt.getText().toString().equals("")){
            ebt.setError("Empty");
        }
        else if(paidOut.getText().toString().equals("")){
            paidOut.setError("Empty");
        }
        else if(aR.getText().toString().equals("")){
            aR.setError("Empty");
        }
        else if(atm.getText().toString().equals("")){
            atm.setError("Empty");
        }
        else if(checksDeposited.getText().toString().equals("")){
            checksDeposited.setError("Empty");
        }
        else{
            saveInputData();
            return true;
        }
        return false;
    }

    private boolean saveChanges(){
        if(!gasGallons.getText().toString().equals(String.valueOf(currentReport.getGasGallons()))){}
        else if(!gasAmount.getText().toString().equals(String.valueOf(currentReport.getGasAmount()))){}
        else if(!dieselGallons.getText().toString().equals(String.valueOf(currentReport.getDieselGallons()))){}
        else if(!dieselAmount.getText().toString().equals(String.valueOf(currentReport.getDieselAmount()))){}
        else if(!atmFee.getText().toString().equals(String.valueOf(currentReport.getAtmFee()))){}
        else if(!beerWine.getText().toString().equals(String.valueOf(currentReport.getBeerWine()))){}
        else if(!beverages.getText().toString().equals(String.valueOf(currentReport.getBeverages()))){}
        else if(!checkFee.getText().toString().equals(String.valueOf(currentReport.getCheckFee()))){}
        else if(!cigarettes.getText().toString().equals(String.valueOf(currentReport.getCigarettes()))){}
        else if(!coffee.getText().toString().equals(String.valueOf(currentReport.getCoffee()))){}
        else if(!energyDrinks.getText().toString().equals(String.valueOf(currentReport.getEnergyDrinks()))){}
        else if(!hotFood.getText().toString().equals(String.valueOf(currentReport.getHotFood()))){}
        else if(!lottery.getText().toString().equals(String.valueOf(currentReport.getLottery()))){}
        else if(!moneyOrder.getText().toString().equals(String.valueOf(currentReport.getMoneyOrder()))){}
        else if(!nonTaxable.getText().toString().equals(String.valueOf(currentReport.getNonTaxable()))){}
        else if(!propane.getText().toString().equals(String.valueOf(currentReport.getPropane()))){}
        else if(!soda.getText().toString().equals(String.valueOf(currentReport.getSoda()))){}
        else if(!taxable.getText().toString().equals(String.valueOf(currentReport.getTaxable()))){}
        else if(!salesTax.getText().toString().equals(String.valueOf(currentReport.getSalesTax()))){}
        else if(!totalSales.getText().toString().equals(String.valueOf(currentReport.getTotalSales()))){}
        else if(!closingCash.getText().toString().equals(String.valueOf(currentReport.getClosingCash()))){}
        else if(!checks.getText().toString().equals(String.valueOf(currentReport.getChecks()))){}
        else if(!credit.getText().toString().equals(String.valueOf(currentReport.getCredit()))){}
        else if(!debit.getText().toString().equals(String.valueOf(currentReport.getDebit()))){}
        else if(!lotto.getText().toString().equals(String.valueOf(currentReport.getLotto()))){}
        else if(!secondCreditMachine.getText().toString().equals(String.valueOf(currentReport.getSecondCreditMachine()))){}
        else if(!ebt.getText().toString().equals(String.valueOf(currentReport.getEbt()))){}
        else if(!paidOut.getText().toString().equals(String.valueOf(currentReport.getPaidOut()))){}
        else if(!aR.getText().toString().equals(String.valueOf(currentReport.getaR()))){}
        else if(!atm.getText().toString().equals(String.valueOf(currentReport.getAtm()))){}
        else if(!checksDeposited.getText().toString().equals(String.valueOf(currentReport.getChecksDeposited()))){ }
        else
            return false;

        return true;
    }

    private void saveInputData(){
        String[] reportName = currentReportName.split("_");
        int month = Integer.valueOf(reportName[0]);
        int day = Integer.valueOf(reportName[1]);
        int year = Integer.valueOf(reportName[2]);

        Report newReport= new Report(day, month, year, currentStore);

        newReport.setGasGallons(Double.valueOf(gasGallons.getText().toString()));
        newReport.setGasAmount(Double.valueOf(gasAmount.getText().toString()));
        newReport.setDieselGallons(Double.valueOf(dieselGallons.getText().toString()));
        newReport.setDieselAmount(Double.valueOf(dieselAmount.getText().toString()));
        newReport.setAtmFee(Double.valueOf(atmFee.getText().toString()));
        newReport.setBeerWine(Double.valueOf(beerWine.getText().toString()));
        newReport.setBeverages(Double.valueOf(beverages.getText().toString()));
        newReport.setCheckFee(Double.valueOf(checkFee.getText().toString()));
        newReport.setCigarettes(Double.valueOf(cigarettes.getText().toString()));
        newReport.setCoffee(Double.valueOf(coffee.getText().toString()));
        newReport.setEnergyDrinks(Double.valueOf(energyDrinks.getText().toString()));
        newReport.setHotFood(Double.valueOf(hotFood.getText().toString()));
        newReport.setLottery(Double.valueOf(lottery.getText().toString()));
        newReport.setMoneyOrder(Double.valueOf(moneyOrder.getText().toString()));
        newReport.setNonTaxable(Double.valueOf(nonTaxable.getText().toString()));
        newReport.setPropane(Double.valueOf(propane.getText().toString()));
        newReport.setSoda(Double.valueOf(soda.getText().toString()));
        newReport.setTaxable(Double.valueOf(taxable.getText().toString()));
        newReport.setSalesTax(Double.valueOf(salesTax.getText().toString()));
        newReport.setTotalSales(Double.valueOf(totalSales.getText().toString()));
        newReport.setClosingCash(Double.valueOf(closingCash.getText().toString()));
        newReport.setChecks(Double.valueOf(checks.getText().toString()));
        newReport.setCredit(Double.valueOf(credit.getText().toString()));
        newReport.setDebit(Double.valueOf(debit.getText().toString()));
        newReport.setLotto(Double.valueOf(lotto.getText().toString()));
        newReport.setSecondCreditMachine(Double.valueOf(secondCreditMachine.getText().toString()));
        newReport.setEbt(Double.valueOf(ebt.getText().toString()));
        newReport.setPaidOut(Double.valueOf(paidOut.getText().toString()));
        newReport.setaR(Double.valueOf(aR.getText().toString()));
        newReport.setAtm(Double.valueOf(atm.getText().toString()));
        newReport.setChecksDeposited(Double.valueOf(checksDeposited.getText().toString()));
        newReport.setCompleted(true);
        newReport.setCustomReport(customReport);

        reportReference.document(currentReportName).set(newReport);
        initializeDatabase();
    }

    private void initializeDatabase(){
        db = FirebaseFirestore.getInstance();
        reportReference = db.collection(getString(R.string.reports_keyword) + "_" + currentStore);

        if(customReport)
            reportReference = db.collection(getString(R.string.custom_reports_keyword) + "_" +  currentStore);

        reportReference
                .document(currentReportName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot doc = task.getResult();
                        currentReport = doc.toObject(Report.class);
                        if(currentReport.isCompleted()) {
                            createReport.setText("View Report");
                            createReport.setIcon(getDrawable(R.drawable.icon_view));
                            createReport.setIconTintResource(R.color.black);
                            fillInputFields();
                            createPDFFile(pdfPath);
                            if(!customReport)
                                saveFields.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void createPDFFile(String appPath) {
        if(new File(appPath).exists())
            new File(appPath).delete();
        try{
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(appPath));
            document.open();

            final int center = Element.ALIGN_CENTER;

            NumberFormat formatter = NumberFormat.getInstance(Locale.US);
            formatter.setMaximumFractionDigits(2);
            formatter.setMinimumFractionDigits(2);

            document.setPageSize(PageSize.A4);

            // title
            addNewItem(document, "Oasis Market - " + Helper.toUpperCase(currentStore), "", center);
            addNewItem(document, "Daily Sales Sheet", "", center);
            addNewItem(document, "Date: ", dateFormatted, center);

            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "","GALLONS SOLD", "", "", false, false, false);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "GASOLINE", Integer.toString((int)currentReport.getGasGallons()), "$ " + formatter.format(currentReport.getGasAmount()), "", false, true, false);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "DIESEL", Integer.toString((int)currentReport.getDieselGallons()), "$ " + formatter.format(currentReport.getDieselAmount()), "", false, true, false);
            addLineSpace(document);

            addNewItem(document, "SALES","", center);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "ATM FEE", "$ " + formatter.format(currentReport.getAtmFee()), "CLOSING CASH", "$ " + formatter.format(currentReport.getClosingCash()) , true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "BEER & WINE","$ " + formatter.format(currentReport.getBeerWine()) , "CHECKS", "$ " + formatter.format(currentReport.getChecks()), true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "BEVERAGES", "$ " + formatter.format(currentReport.getBeverages()) , "CREDIT", "$ " + formatter.format(currentReport.getCredit()) , true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "CHECK FEE", "$ " + formatter.format(currentReport.getCheckFee()) ,  "DEBIT", "$ " + formatter.format(currentReport.getDebit()), true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "CIGARETTES", "$ " + formatter.format(currentReport.getCigarettes()) , "LOTTO", "$ " + formatter.format(currentReport.getLotto()) , true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "COFFEE", "$ " + formatter.format(currentReport.getCoffee()) , "2ND CARD", "$ " + formatter.format(currentReport.getSecondCreditMachine()) , true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "ENERGY DRINKS", "$ " + formatter.format(currentReport.getEnergyDrinks()) , "EBT", "$ " + formatter.format(currentReport.getEbt()), true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "HOT FOOD","$ " + formatter.format(currentReport.getHotFood()) , "PAID OUT", "$ " + formatter.format(currentReport.getPaidOut()) , true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "LOTTERY", "$ " + formatter.format(currentReport.getLottery()) , "A/R", "$ " + formatter.format(currentReport.getaR()) , true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "MONEY ORDER", "$ " + formatter.format(currentReport.getMoneyOrder()) , "ATM","$ " + formatter.format(currentReport.getAtm()) , true, false, true);
            addLineSpace(document);

            addNewItemWithLeftAndRight(document, "NON TAXABLE", "$ " + formatter.format(currentReport.getNonTaxable()) ,"CHECKS DEPOSITED", "$ " + formatter.format(currentReport.getChecksDeposited()) , true, false, true);
            addLineSpace(document);

            addNewItemWithRight(document, "PROPANE", "$ " + formatter.format(currentReport.getPropane()));
            addLineSpace(document);

            addNewItemWithRight(document, "SODA", "$ " + formatter.format(currentReport.getSoda()) );
            addLineSpace(document);

            addNewItemWithRight(document, "TAXABLE", "$ " + formatter.format(currentReport.getTaxable()));
            addLineSpace(document);

            addNewItemWithRight(document, "SALES TAX", "$ " + formatter.format(currentReport.getSalesTax()));
            addLineSpace(document);

            addNewItemWithRight(document, "TOTAL SALES", "$ " + formatter.format(currentReport.getTotalSales()));
            addLineSpace(document);

            document.close();
        }
        catch (Exception e){}
    }

    private void printPDF() {
        // Get a PrintManager instance
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        String printName = reportDate;
        try {
            PrintDocumentAdapter printDocumentAdapter = new PdfDocumentAdapter(context, pdfPath, printName);
            printManager.print("Report " + printName, printDocumentAdapter, new PrintAttributes.Builder().build());
        }catch (Exception e){}
    }

    private void addNewItemWithLeftAndRight(Document document, String textLeft,String dataLeft, String textRight, String dataRight, boolean showTable, boolean center, boolean underline) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        Chunk underlineWord = new Chunk(dataLeft);
        underlineWord.setUnderline(0.1f, -2f); //0.1 thick, -2 y-location

        PdfPCell cell1 = new PdfPCell(new Paragraph(textLeft));
        PdfPCell cell2 = new PdfPCell(new Paragraph(dataLeft));
        if(underline)
            cell2 = new PdfPCell(new Paragraph(underlineWord));
        PdfPCell cell4 = new PdfPCell(new Paragraph(textRight));
        PdfPCell cell5 = new PdfPCell(new Paragraph(dataRight));

        cell1.setPaddingBottom(10);
        cell1.setPaddingLeft(20);
        cell2.setPaddingLeft(25);
        cell1.setPaddingTop(10);
        cell4.setPaddingTop(10);
        cell2.setPaddingTop(10);
        cell5.setPaddingLeft(25);
        cell5.setPaddingTop(10);

        cell1.setBorder(Rectangle.NO_BORDER);
        cell2.setBorder(Rectangle.NO_BORDER);
        if(!showTable) {
            cell4.setBorder(Rectangle.NO_BORDER);
            cell5.setBorder(Rectangle.NO_BORDER);
        }

        cell1.setHorizontalAlignment(Element.ALIGN_LEFT);
        if(center)
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        else
            cell2.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell5.setHorizontalAlignment(Element.ALIGN_LEFT);

        table.addCell(cell1);
        table.addCell(cell2);
        table.addCell(cell4);
        table.addCell(cell5);

        document.add(table);
    }

    private void addLineSpace(Document document) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setSpacingAfter(20);
        document.add(p);
    }

    private void addNewItem(Document document, String text, String data, int align) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Paragraph(text + "\t" + data));
        cell.setPaddingTop(10);
        cell.setPaddingBottom(10);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);

        document.add(table);
    }

    private void addNewItemWithRight(Document document, String text, String data) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        Chunk underlineWord = new Chunk(data);
        underlineWord.setUnderline(0.1f, -2f); //0.1 thick, -2 y-location

        PdfPCell cell1 = new PdfPCell(new Paragraph(text));
        PdfPCell cell2 = new PdfPCell(new Paragraph(underlineWord));
        PdfPCell cell4 = new PdfPCell();
        PdfPCell cell5 = new PdfPCell();

        cell1.setPaddingBottom(10);
        cell1.setPaddingLeft(20);
        cell2.setPaddingLeft(25);
        cell1.setPaddingTop(10);
        cell2.setPaddingTop(10);

        cell1.setBorder(Rectangle.NO_BORDER);
        cell2.setBorder(Rectangle.NO_BORDER);
        cell4.setBorder(Rectangle.NO_BORDER);
        cell5.setBorder(Rectangle.NO_BORDER);

        cell1.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell2.setHorizontalAlignment(Element.ALIGN_LEFT);

        table.addCell(cell1);
        table.addCell(cell2);
        table.addCell(cell4);
        table.addCell(cell5);

        document.add(table);
    }
}