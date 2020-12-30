package com.akapps.oasisutilities.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.akapps.oasisutilities.classes.Report;
import com.akapps.oasisutilities.recyclerviews.Report_Recyclerview;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.akapps.oasisutilities.classes.Helper;
import com.akapps.oasisutilities.R;
import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.whiteelephant.monthpicker.MonthPickerDialog;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Main extends AppCompatActivity implements DatePickerDialog.OnDateSetListener{

    // layout
    private BubbleNavigationConstraintView bottomNavigation;
    private Context context;
    private int selectedActivity;
    private RecyclerView recyclerview;
    private RecyclerView.Adapter adapter;
    private FloatingActionButton addReport;
    private LinearLayout empty_layout;
    private Dialog progressDialog;

    // data
    private ArrayList<Report> reports = new ArrayList<>();
    private ArrayList<Report> filteredReports = new ArrayList<>();
    private ArrayList<Report> customReports = new ArrayList<>();
    private Query queryReport;
    private Query queryCustomReport;
    private String currentStore;
    private boolean filterCustomReport;
    private boolean filterReports;

    // database
    private FirebaseFirestore db;
    private CollectionReference reportsReference;
    private CollectionReference customReportsReference;
    private ListenerRegistration reportListener;
    private ListenerRegistration customReportListener;
    private ListenerRegistration filterReportListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        currentStore = currentStore();

        if (savedInstanceState != null)
            filterCustomReport = savedInstanceState.getBoolean("customReport");
        else
            filterCustomReport = false;

        initializeLayout();
        createBottomNavigation();
    }

    @Override
    public void onSaveInstanceState (Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("customReport", filterCustomReport);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(progressDialog!=null)
            progressDialog.cancel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        progressDialog = Helper.showLoading(progressDialog, context, true);
        // database initialized
        initializeDatabase();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Helper.showLoading(progressDialog, context, false);
            }
        }, 750);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(filterReportListener!=null)
            filterReportListener.remove();
        customReportListener.remove();
        reportListener.remove();

        if(progressDialog!=null)
            progressDialog.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        filterReports = false;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        // on back press, ask user if he wants to log out
        logoutDialog();
    }

    public void initializeDatabase(){
        db = FirebaseFirestore.getInstance();
        currentStore = currentStore();
        reportsReference = db.collection(getString(R.string.reports_keyword) + "_" + currentStore);
        customReportsReference = db.collection(getString(R.string.custom_reports_keyword) + "_" + currentStore);

        queryReport = reportsReference;
        queryCustomReport = customReportsReference.orderBy("dateAdded", Query.Direction.DESCENDING);;

        customReportListener = queryCustomReport
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    customReports = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Report report = doc.toObject(Report.class);
                        customReports.add(report);
                    }
                    if(filterCustomReport)
                        populateAdapter(customReports);
                });

        reportListener = queryReport
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    reports = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Report report = doc.toObject(Report.class);
                        reports.add(report);
                    }
                    if(!filterCustomReport)
                        populateAdapter(reports);
                });

        if(filterCustomReport)
            setTitle("Custom Reports - " + Helper.toUpperCase(currentStore));
        else
            setTitle("Reports - " + Helper.toUpperCase(currentStore));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem filter, flip;

        if(filterCustomReport)
            filter = menu.getItem(0).setVisible(false);
        else
            filter = menu.getItem(0).setVisible(true);

        filter = menu.getItem(0);
        flip = menu.getItem(1);
        Drawable filterIcon = filter.getIcon();
        Drawable flipIcon = flip.getIcon();
        if (filterIcon != null && flipIcon != null) {
            filterIcon.mutate();
            flipIcon.mutate();
            if(filterCustomReport)
                flipIcon.setColorFilter(getColor(R.color.gold), PorterDuff.Mode.SRC_ATOP);
            if(filterReports)
                filterIcon.setColorFilter(getColor(R.color.gold), PorterDuff.Mode.SRC_ATOP);
            else
                filterIcon.setColorFilter(getColor(R.color.black), PorterDuff.Mode.SRC_ATOP);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if( item.getItemId() == R.id.action_Info){
            showAppInfoDialog(); // displays app version number and company
        }
        else if( item.getItemId() == R.id.actionFilter){
            if(filterCustomReport){
                Helper.showUserMessage(findViewById(android.R.id.content),
                        "No filtering for Custom Reports", Snackbar.LENGTH_LONG, false);
            }
            else if(filterReports) {
                filterReports = false;
                invalidateOptionsMenu();
                populateAdapter(reports);
            }
            else {
                selectReportIntervalDialog(true, "Filter by:");
            }
        }
        else if( item.getItemId() == R.id.actionSwitch){
            if(filterCustomReport) {
                filterCustomReport = false;
                populateAdapter(reports);
                setTitle("Reports - " + Helper.toUpperCase(currentStore));
            }
            else {
                filterCustomReport = true;
                populateAdapter(customReports);
                setTitle("Custom Reports - " + Helper.toUpperCase(currentStore));
            }
            invalidateOptionsMenu();
            changeLayout();
        }
        return true;
    }

    // populates the recyclerview
    private void populateAdapter(ArrayList<Report> reports) {
        isEmpty();
        adapter = new Report_Recyclerview(sortReports(reports, false), db);
        recyclerview.setAdapter(adapter);
    }

    private ArrayList<Report> sortReports(ArrayList<Report> reports, boolean decending){
        if(filterCustomReport && decending)
            reports.sort((o1, o2) -> o1.getDate().compareTo(o2.getDate()));
        else if(filterCustomReport)
            reports.sort((o1, o2) -> o1.getDateAdded().compareTo(o2.getDateAdded()));
        else {
            if(decending)
                reports.sort((o1, o2) -> o1.getDate().compareTo(o2.getDate()));
            else
                reports.sort((o2, o1) -> o1.getDate().compareTo(o2.getDate()));
        }
        return reports;
    }

    private void createNewCustomReport(final String customReportName, final boolean showReport){
        final int month = Integer.parseInt(customReportName.split("~")[0]);
        final int year = Integer.parseInt(customReportName.split("~")[1]);
        boolean selectedYear = false, selectedMonth = false;

        if(month==-1)
            selectedYear = true;
        else
            selectedMonth = true;

        queryReport = reportsReference;

        final boolean finalSelectedMonth = selectedMonth;
        final boolean finalSelectedYear = selectedYear;
        filterReportListener = queryReport
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    filteredReports = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Report report = doc.toObject(Report.class);
                        if(finalSelectedMonth) {
                            if(report.getMonth() == month && report.getYear()==year) {
                                if(report.isCompleted())
                                    filteredReports.add(report);
                                else if(showReport)
                                    filteredReports.add(report);
                            }
                        }
                        else if(finalSelectedYear){
                            if(report.getYear()==year ) {
                                if(report.isCompleted())
                                    filteredReports.add(report);
                                else if(showReport)
                                    filteredReports.add(report);
                            }
                        }
                    }
                    if(showReport) {
                        if(filteredReports.size()>0) {
                            populateAdapter(filteredReports);
                            filterReports = true;
                            invalidateOptionsMenu();
                        }
                        else
                            Helper.showUserMessage(findViewById(android.R.id.content),
                                    "No Reports found", Snackbar.LENGTH_LONG, false);
                    }
                    else
                        createReport(month, year, null, null);
                });
    }

    private void createNewCustomReport(String start, String end, final boolean showReport){
        final String[] startDate = start.split("-");
        final String[] endDate = end.split("-");

        DateFormat format = new SimpleDateFormat("MM-dd-yyyy");
        Date startRange = null;
        Date endRange = null;
        try {
            startRange = format.parse(start);
            endRange = format.parse(end);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date finalStartRange = startRange;
        Date finalEndRange = endRange;

        queryReport = reportsReference;
        filterReportListener = queryReport
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    filteredReports = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Report report = doc.toObject(Report.class);
                        Date currentDate = null;
                        try {
                            currentDate = format.parse(report.getMonth() + "-" + report.getDay() + "-" + report.getYear());
                        } catch (ParseException parseException) {
                            parseException.printStackTrace();
                        }
                        if(currentDate.compareTo(finalStartRange) >= 0 && currentDate.compareTo(finalEndRange) <= 0){
                            if (report.isCompleted())
                                filteredReports.add(report);
                            else if(showReport)
                                filteredReports.add(report);
                        }
                    }
                    if(showReport) {
                        if (filteredReports.size() > 0) {
                            populateAdapter(filteredReports);
                            filterReports = true;
                            invalidateOptionsMenu();
                        }
                        else
                            Helper.showUserMessage(findViewById(android.R.id.content),
                                    "No Reports founds", Snackbar.LENGTH_LONG, false);
                    }
                    else
                        createReport(Integer.parseInt(startDate[0]),
                                Integer.parseInt(endDate[0]), start, end);
                });
    }

    private void createReport(int month, int year, String start, String end){
        filterReportListener.remove();
        if(filteredReports.size()==0) {
            Helper.showUserMessage(findViewById(android.R.id.content), "No reports", Snackbar.LENGTH_LONG, false);
        }
        else if(filteredReports.size()==1) {
            Helper.showUserMessage(findViewById(android.R.id.content), "Only one report exists", Snackbar.LENGTH_LONG, false);
        }
        else {
            String datesUsed = "";
            int numberOfDates = 0;
            double gasGallonsAll = 0;
            double gasAmountAll = 0;
            double dieselGallonsAll = 0;
            double dieselAmountAll = 0;
            double atmFeeAll = 0;
            double beerWineAll = 0;
            double beveragesAll = 0;
            double checkFeeAll = 0;
            double cigarettesAll = 0;
            double coffeeAll = 0;
            double energyDrinksAll = 0;
            double hotFoodAll = 0;
            double lotteryAll = 0;
            double moneyOrderAll = 0;
            double nonTaxableAll = 0;
            double propaneAll = 0;
            double sodaAll = 0;
            double taxableAll = 0;
            double salesTaxAll = 0;
            double totalSalesAll = 0;
            double closingCashAll = 0;
            double checksAll = 0;
            double creditAll = 0;
            double debitAll = 0;
            double lottoAll = 0;
            double secondCreditMachineAll = 0;
            double ebtAll = 0;
            double paidOutAll = 0;
            double aRAll = 0;
            double atmAll = 0;
            double checksDepositedAll = 0;

            filteredReports = sortReports(filteredReports, true);

            for (int i = 0; i < filteredReports.size(); i++) {
                Report currentReport = filteredReports.get(i);
                datesUsed += currentReport.getMonth() + "-" + currentReport.getDay() + "-" + currentReport.getYear() + "\n";
                numberOfDates+=1;
                gasGallonsAll += currentReport.getGasGallons();
                gasAmountAll += currentReport.getGasAmount();
                dieselGallonsAll += currentReport.getDieselGallons();
                dieselAmountAll += currentReport.getDieselAmount();
                atmFeeAll += currentReport.getAtmFee();
                beerWineAll += currentReport.getBeerWine();
                beveragesAll += currentReport.getBeverages();
                checkFeeAll += currentReport.getCheckFee();
                cigarettesAll += currentReport.getCigarettes();
                coffeeAll += currentReport.getCoffee();
                energyDrinksAll += currentReport.getEnergyDrinks();
                hotFoodAll += currentReport.getHotFood();
                lotteryAll += currentReport.getLottery();
                moneyOrderAll += currentReport.getMoneyOrder();
                nonTaxableAll += currentReport.getNonTaxable();
                propaneAll += currentReport.getPropane();
                sodaAll += currentReport.getSoda();
                taxableAll += currentReport.getTaxable();
                salesTaxAll += currentReport.getSalesTax();
                totalSalesAll += currentReport.getTotalSales();
                closingCashAll += currentReport.getClosingCash();
                checksAll += currentReport.getChecks();
                creditAll += currentReport.getCredit();
                debitAll += currentReport.getDebit();
                lottoAll += currentReport.getLotto();
                secondCreditMachineAll += currentReport.getSecondCreditMachine();
                ebtAll += currentReport.getEbt();
                paidOutAll += currentReport.getPaidOut();
                aRAll += currentReport.getaR();
                atmAll += currentReport.getAtm();
                checksDepositedAll += currentReport.getChecksDeposited();
            }
            Report newReport = new Report();
            newReport.setGasGallons(gasGallonsAll);
            newReport.setGasAmount(gasAmountAll);
            newReport.setDieselGallons(dieselGallonsAll);
            newReport.setDieselAmount(dieselAmountAll);
            newReport.setAtmFee(atmFeeAll);
            newReport.setBeerWine(beerWineAll);
            newReport.setBeverages(beveragesAll);
            newReport.setCheckFee(checkFeeAll);
            newReport.setCigarettes(cigarettesAll);
            newReport.setCoffee(coffeeAll);
            newReport.setEnergyDrinks(energyDrinksAll);
            newReport.setHotFood(hotFoodAll);
            newReport.setLottery(lotteryAll);
            newReport.setMoneyOrder(moneyOrderAll);
            newReport.setNonTaxable(nonTaxableAll);
            newReport.setPropane(propaneAll);
            newReport.setSoda(sodaAll);
            newReport.setTaxable(taxableAll);
            newReport.setSalesTax(salesTaxAll);
            newReport.setTotalSales(totalSalesAll);
            newReport.setClosingCash(closingCashAll);
            newReport.setChecks(checksAll);
            newReport.setCredit(creditAll);
            newReport.setDebit(debitAll);
            newReport.setLotto(lottoAll);
            newReport.setSecondCreditMachine(secondCreditMachineAll);
            newReport.setEbt(ebtAll);
            newReport.setPaidOut(paidOutAll);
            newReport.setaR(aRAll);
            newReport.setAtm(atmAll);
            newReport.setChecksDeposited(checksDepositedAll);
            newReport.setCompleted(true);
            newReport.setCustomReport(true);
            newReport.setStore(currentStore);

            int days;
            if(start == null && month!=-1) {
                YearMonth yearMonthObject = YearMonth.of(year, month);
                days = yearMonthObject.lengthOfMonth();
            }
            else if(start==null && month==-1){
                days = Year.of(year).length();
            }
            else{
                DateTimeFormatter format = DateTimeFormatter.ofPattern("MM-dd-yyyy");
                LocalDate dateBefore = LocalDate.parse(start, format);
                LocalDate dateAfter = LocalDate.parse(end, format);
                days = (int) ChronoUnit.DAYS.between(dateBefore, dateAfter);
            }

            newReport.setDatesUsed(datesUsed +
                    "--------------------------------\nNumber of days: " + numberOfDates
                    + "\n--------------------------------\nExpected days: " + ++days);

            newReport.setYear(year);
            newReport.setDay(0);

            if(month!=-1)
                newReport.setMonth(month);
            else
                newReport.setMonth(0);

            String date;

            if(start!=null)
                date = start.replace("-","") + "_0_" + end.replace("-","");
            else
                date = newReport.getMonth() + "_0_"  + newReport.getYear();

            customReportsReference
                    .document(date)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();
                            if (!doc.exists()) {
                                if(start!=null) {
                                    newReport.setMonth(13);
                                    newReport.setDateRange(start, end);
                                }
                                customReportsReference.document(date).set(newReport);

                                Helper.showUserMessage(Main.this.findViewById(android.R.id.content), "Custom Report Added", Snackbar.LENGTH_LONG, false);
                            }
                            else
                                Helper.showUserMessage(Main.this.findViewById(android.R.id.content), "Custom Report exists...\nDelete to Add Again", Snackbar.LENGTH_INDEFINITE, true);

                        }
                    });

            populateAdapter(customReports);
        }
    }

    private void initializeLayout(){
        recyclerview = findViewById(R.id.reports_RecyclerView);
        recyclerview.setHasFixedSize(true);
        recyclerview.setItemViewCacheSize(20);

        changeLayout();

        addReport = findViewById(R.id.add_Report);
        empty_layout = findViewById(R.id.empty_layout);

        addReport.setOnClickListener(v -> {
            if(filterCustomReport)
                selectReportIntervalDialog(false, "Report Type:");
            else
                showDatePickerDialog();
        });
    }

    private void changeLayout(){
        if(filterCustomReport) {
            if (Helper.isTablet(context)) {
                if (Helper.getOrientation(context))
                    recyclerview.setLayoutManager(new GridLayoutManager(context, 3));
                else
                    recyclerview.setLayoutManager(new GridLayoutManager(context, 2));
            }
            else {
                if (Helper.getOrientation(context))
                    recyclerview.setLayoutManager(new GridLayoutManager(context, 2));
                else
                    recyclerview.setLayoutManager(new GridLayoutManager(context, 1));
            }
        }
        else {
            if(Helper.isTablet(context)) {
                if (Helper.getOrientation(context))
                    recyclerview.setLayoutManager(new GridLayoutManager(context, 4));
                else
                    recyclerview.setLayoutManager(new GridLayoutManager(context, 3));
            }
            else {
                if (Helper.getOrientation(context))
                    recyclerview.setLayoutManager(new GridLayoutManager(context, 3));
                else
                    recyclerview.setLayoutManager(new GridLayoutManager(context, 2));
            }
        }
    }

    private void isEmpty(){
        if(customReports.size() ==0 && filterCustomReport)
            empty_layout.setVisibility(View.VISIBLE);
        else if(filterCustomReport){
            empty_layout.setVisibility(View.GONE);
        }
        else if(reports.size()==0)
            empty_layout.setVisibility(View.VISIBLE);
        else
            empty_layout.setVisibility(View.GONE);
    }

    private void selectReportIntervalDialog(final boolean showReports, String title){
        if(reports.size()>1) {
            new MaterialDialog.Builder(context)
                    .title(title)
                    .titleColor(context.getColor(R.color.orange_red))
                    .backgroundColor(context.getColor(R.color.black))
                    .content("Select the date interval of reports")
                    .contentGravity(GravityEnum.CENTER)
                    .positiveText("YEAR")
                    .negativeText("MONTH")
                    .neutralText("RANGE")
                    .canceledOnTouchOutside(true)
                    .autoDismiss(false)
                    .onPositive((dialog, which) -> {
                        showYearDialog(showReports);
                        dialog.dismiss();
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            showMonthYearDialog(showReports);
                            dialog.dismiss();
                        }
                    })
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            showDateRangeDialog(showReports);
                            dialog.dismiss();
                        }
                    })
                    .negativeColor(context.getColor(R.color.gray))
                    .positiveColor(context.getColor(R.color.green))
                    .neutralColor(context.getColor(R.color.gold))
                    .contentColor((context.getColor(R.color.orange_red)))
                    .show();
        }
        else
            Helper.showUserMessage(findViewById(android.R.id.content), "Requirement: More than one report", Snackbar.LENGTH_LONG, false);
    }

    private void showDateRangeDialog(final boolean showReports){
        final MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        final CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        builder.setCalendarConstraints(constraintsBuilder.build());
        final MaterialDatePicker<?> picker = builder.build();
        picker.show(getSupportFragmentManager(), picker.toString());
        picker.setCancelable(false);

        picker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Object selection) {
                Pair selectedDates = (Pair) picker.getSelection();
                final Pair<Date, Date> rangeDate = new Pair<>(new Date((Long) selectedDates.first), new Date((Long) selectedDates.second));
                Date endDate = rangeDate.second;
                Date startDate = rangeDate.first;

                Calendar c = Calendar.getInstance();
                // start date
                c.setTime(startDate);
                c.add(Calendar.DATE, 1);
                startDate = c.getTime();
                //end date
                c.setTime(endDate);
                c.add(Calendar.DATE, 1);
                endDate = c.getTime();

                Format formatter = new SimpleDateFormat("MM-dd-yyyy");
                Main.this.createNewCustomReport(formatter.format(startDate), formatter.format(endDate), showReports);
            }
        });
    }

    private void showMonthYearDialog(final boolean showReport){
        MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(context, new MonthPickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(int month, int year) {
                month++;
                createNewCustomReport(month + "~" + year, showReport);
            }
        }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH));

        builder.setYearRange(2000, 2050)
                .setTitle("Select Month and Year")
                .build()
                .show();
    }

    private void showYearDialog(final boolean showReports){
        final DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                R.style.DialogTheme,
                this,
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().getTouchables().get(0).performClick();
        datePickerDialog.getDatePicker().getTouchables().get(1).setVisibility(View.GONE);
        datePickerDialog.getDatePicker().getTouchables().get(0).setVisibility(View.GONE);
        datePickerDialog.setTitle("Select Year");

        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "CLOSE", datePickerDialog);
        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "", datePickerDialog);


        datePickerDialog.getDatePicker()
                .init(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        createNewCustomReport("-1~" + year, showReports);
                        datePickerDialog.dismiss();
                    }
                });
        datePickerDialog.show();
    }

    public void showDatePickerDialog(){
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                R.style.DialogTheme,
                this,
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        month++;
        final String date = month + "_" + day + "_" + year;
        final Report newReport = new Report(day, month, year, currentStore);

        reportsReference
                .document(date)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if(!document.exists())
                                reportsReference.document(date).set(newReport);
                            else
                                Helper.showUserMessage(findViewById(android.R.id.content),
                                        "Report already exists", Snackbar.LENGTH_INDEFINITE, true);
                        }
                    }
                });

        populateAdapter(reports);
    }

    private String currentStore(){
        String[] stores = getResources().getStringArray(R.array.stores);
        String currentStore;
        if(FirebaseAuth.getInstance().getCurrentUser()!=null) {
            for (int i = 0; i < stores.length; i++) {
                currentStore = FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0].replace("_", " ");
                if (stores[i].toLowerCase().equals(currentStore))
                    return currentStore;
            }
        }
        return null;
    }

    private void createBottomNavigation(){
        bottomNavigation = (BubbleNavigationConstraintView) findViewById(R.id.bottom_navigation);
        selectedActivity = 0;
        bottomNavigation.setCurrentActiveItem(selectedActivity);

        // selects checks icon (position 1) selected instead of default home icon (position 0)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                bottomNavigation.setCurrentActiveItem(selectedActivity);
            }
        }, 250);

        // on click listener for bottom navigation
        bottomNavigation.setNavigationChangeListener(new BubbleNavigationChangeListener() {
            @Override
            public void onNavigationChanged(View view, int position) {
                if(position == 1){
                    Intent home = new Intent(Main.this, Checks.class);
                    home.putExtra("store", currentStore);
                    startActivity(home);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
                else if(position==2){
                    logoutDialog();
                }
            }
        });
    }
    // sends user to login screen
    private void logoutDialog() {
        new MaterialDialog.Builder(context)
                .title("Logout?")
                .titleColor(context.getColor(R.color.orange_red))
                .backgroundColor(context.getColor(R.color.black))
                .content("Are you sure?")
                .contentGravity(GravityEnum.CENTER)
                .positiveText("CONFIRM")
                .negativeText("CLOSE")
                .canceledOnTouchOutside(false)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        FirebaseAuth.getInstance().signOut();
                        Helper.savePreference("login", null, context);
                        Intent home = new Intent(Main.this, Login.class);
                        startActivity(home);
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        bottomNavigation.setCurrentActiveItem(selectedActivity);
                        dialog.dismiss();
                    }
                })
                .negativeColor(context.getColor(R.color.gray))
                .positiveColor(context.getColor(R.color.green))
                .contentColor((context.getColor(R.color.orange_red)))
                .show();
    }
    // shows app information
    private void showAppInfoDialog(){
        new MaterialDialog.Builder(context)
                .title("App Information")
                .titleColor(getColor(R.color.orange_red))
                .contentGravity(GravityEnum.CENTER)
                .content("v1.2\nAK Apps LLC"   + HtmlCompat.fromHtml(getString(R.string.c_symbol), HtmlCompat.FROM_HTML_MODE_LEGACY))
                .contentColor(getColor(R.color.green))
                .backgroundColor(getColor(R.color.black))
                .positiveText("Close")
                .canceledOnTouchOutside(false)
                .autoDismiss(false)
                .neutralText("OPEN IN SETTINGS")
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        openAppInSettings();
                    }
                })
                .positiveColor(getColor(R.color.gray))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void openAppInSettings(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}