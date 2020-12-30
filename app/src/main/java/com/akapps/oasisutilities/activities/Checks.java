package com.akapps.oasisutilities.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.akapps.oasisutilities.classes.Camera;
import com.akapps.oasisutilities.classes.Company;
import com.akapps.oasisutilities.classes.Customer;
import com.akapps.oasisutilities.classes.Helper;
import com.akapps.oasisutilities.classes.OnSwipeTouchListener;
import com.akapps.oasisutilities.R;
import com.akapps.oasisutilities.recyclerviews.CustomerInfo_Recyclerview;
import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;
import com.gigamole.navigationtabstrip.NavigationTabStrip;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import javax.annotation.Nullable;

public class Checks extends AppCompatActivity {

    // layout and activity info
    private BubbleNavigationConstraintView bottomNavigation;
    private Context context;
    private Boolean isCompaniesSelected;
    private RecyclerView recyclerview;
    private RecyclerView.Adapter adapter;
    private MaterialButton cashCheckBtn;
    private MaterialButton goHomeStore;
    private LinearLayout empty_layout;
    public NavigationTabStrip navigationTabStrip;
    private FloatingActionButton addCheckCompany;

    // data
    private ArrayList<Company> companies = new ArrayList<>();
    private ArrayList<Customer> customers = new ArrayList<>();
    private String databasePath;
    private String storeFilter;
    private String selectedStore;
    private Query queryCompany;
    private Query queryCustomer;

    // database
    private FirebaseFirestore db;
    private CollectionReference companyReference;
    private CollectionReference customerReference;
    private ListenerRegistration companyListener;
    private ListenerRegistration customerListener;
    private Dialog progressDialog;

    // camera
    private String FILE_PROVIDER_AUTHORITY;
    private int REQUEST_IMAGE_CAPTURE = 1;
    private String mTempPhotoPath;
    private Bitmap mResultsBitmap;
    private String currentIdPath;
    private Company company_pic;

    // dialog
    private MaterialDialog dialog_Company;
    private ImageView check_image;
    private TextView check_image_text;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // dialog layout
    private MaterialDialog add_Dialog;
    private EditText add_customer;
    private EditText add_id_num;
    private TextView id_message;

    private int selectedActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        // layout is initialized
        initializeLayout(savedInstanceState);

        if(getIntent() != null && getIntent().getExtras() != null) {
            selectedStore = getIntent().getStringExtra("store").split("@")[0].replace("_", " ");
            Helper.savePreference("store", selectedStore, context);
        }
        else{
            selectedStore = Helper.getPreference("store", context);
        }
        setTitle(selectedStore.substring(0, 1).toUpperCase() + selectedStore.substring(1));

        FILE_PROVIDER_AUTHORITY = getString(R.string.fileprovider);

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(progressDialog!=null)
            progressDialog.cancel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        filter(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        customerListener.remove();
        companyListener.remove();
        if(progressDialog!=null)
            progressDialog.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(bottomNavigation.getCurrentActiveItemPosition()!=1)
            bottomNavigation.setCurrentActiveItem(selectedActivity);
    }

    // when orientation changes, then isCompaniesSelected is saved
    @Override
    public void onSaveInstanceState (Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("selected", isCompaniesSelected);
        savedInstanceState.putString("filter", storeFilter);
    }

    public void initializeDatabase(){
        db = FirebaseFirestore.getInstance();
        companyReference = db.collection(getString(R.string.companies_keyword));
        customerReference = db.collection(getString(R.string.customers_keyword));
        storage = FirebaseStorage.getInstance();
    }

    public void filter(final boolean filter){
        queryCompany = db.collection(getString(R.string.companies_keyword)).orderBy("dateAdded", Query.Direction.DESCENDING);
        queryCustomer = db.collection(getString(R.string.customers_keyword)).orderBy("dateAdded", Query.Direction.DESCENDING);

        if(storeFilter == null || storeFilter.equals(selectedStore)){
            goHomeStore.setVisibility(View.INVISIBLE);
        }
        else{
            goHomeStore.setVisibility(View.VISIBLE);
        }

        companyListener = queryCompany
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        companies = new ArrayList<>();
                        if(filter) {
                            if (storeFilter == null) {
                                storeFilter = selectedStore;
                            }
                            setTitle(storeFilter.substring(0, 1).toUpperCase() + storeFilter.substring(1));
                        }
                        for (DocumentSnapshot doc : snapshots) {
                            Company company = doc.toObject(Company.class);
                            if(filter) {
                                if (company.getStore().equals(storeFilter))
                                    companies.add(company);
                                else if(storeFilter.toLowerCase().equals("all"))
                                    companies.add(company);
                            }
                            else
                                companies.add(company);
                        }
                        populateAdapter();
                    }
                });

        customerListener = queryCustomer
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        customers = new ArrayList<>();
                        if(filter) {
                            if (storeFilter == null) {
                                storeFilter = selectedStore;
                            }
                            setTitle(storeFilter.substring(0, 1).toUpperCase() + storeFilter.substring(1));
                        }
                        for (DocumentSnapshot doc : snapshots) {
                            Customer customer = doc.toObject(Customer.class);
                            if(filter) {
                                if (customer.getStore().equals(storeFilter))
                                    customers.add(customer);
                                else if(storeFilter.toLowerCase().equals("all"))
                                    customers.add(customer);
                            }
                            else
                                customers.add(customer);
                        }
                        populateAdapter();
                    }
                });
    }

    // populates the recyclerview
    private void populateAdapter() {
        isEmpty();
        adapter = new CustomerInfo_Recyclerview(companies, customers, isCompaniesSelected, db, findViewById(android.R.id.content), selectedStore);
        recyclerview.setAdapter(adapter);
    }

    // populates the recyclerview when searching for specific item
    private void populateSearchQuery(ArrayList data) {
        CustomerInfo_Recyclerview adapter = new CustomerInfo_Recyclerview(
                data, db, isCompaniesSelected, findViewById(android.R.id.content), selectedStore);
        recyclerview.setAdapter(adapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void refreshRecyclerView(){
        recyclerview.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onTouch(MotionEvent e) {
                int action = e.getActionMasked();
                // catches click from user after swiping and clicks it again to un-focus the swipe
                if(MotionEvent.ACTION_DOWN == action){
                    long downTime = SystemClock.uptimeMillis();
                    long upTime = downTime + 100;
                    MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN,
                            e.getX(), e.getY(), 0);
                    recyclerview.dispatchTouchEvent(downEvent);
                    MotionEvent upEvent = MotionEvent.obtain(upTime, upTime, MotionEvent.ACTION_UP,
                            e.getX(), e.getY(), 0);
                    recyclerview.dispatchTouchEvent(upEvent);
                    downEvent.recycle();
                    upEvent.recycle();
                }
                super.onTouch(e);
            }

            public void onSwipeRight() {
                switchTab("left", 0);
            }
            public void onSwipeLeft() {
                switchTab("right", 1);
            }
        });
    }

    private void addCompany(){
        add_Dialog = new MaterialDialog.Builder(context)
                .title("Adding " + databasePath)
                .titleColor(getColor(R.color.orange_red))
                .backgroundColor(getColor(R.color.black))
                .customView(R.layout.dialog_add_customer, true)
                .positiveText("ADD")
                .canceledOnTouchOutside(false)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        final String name = add_customer.getText().toString();
                        if(name.equals(""))
                            add_customer.setError("Empty");
                        else if(name.length()>30)
                            add_customer.setError("max length is 30");
                        else if(!name.matches("^[a-z A-Z'-~]*$"))
                            add_customer.setError("Only letters");
                        else{
                            if(isCompaniesSelected) {
                                db.collection(getString(R.string.companies_keyword))
                                        .document(name)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if(!document.exists()){
                                                        int random = (int)(Math.random() * (100000 + 1));
                                                        Company company = new Company(name + "_" + random, selectedStore);
                                                        companyReference.document(name + "_" + random).set(company);
                                                        storeFilter = selectedStore;
                                                        filter(true);
                                                        add_Dialog.dismiss();
                                                    }
                                                    else{
                                                        Helper.showUserMessage(findViewById(android.R.id.content), "Company exists now or has" +
                                                                " been deleted", Snackbar.LENGTH_INDEFINITE, true);
                                                    }
                                                }
                                            }
                                        });
                            }
                            else{
                                final String id_number = add_id_num.getText().toString();
                                if(id_number.equals("") || id_number.length()<4){
                                    add_id_num.setError("Empty");
                                }
                                else {
                                    final int id_num = Integer.parseInt(id_number);
                                    db.collection(getString(R.string.customers_keyword))
                                            .document(name+"_"+id_number)
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        DocumentSnapshot document = task.getResult();
                                                        if (!document.exists() && !name.contains("~add")) {
                                                            db.collection(getString(R.string.customers_keyword))
                                                                    .document(name+"_"+id_number)
                                                                    .collection(getString(R.string.checks_keyword))
                                                                    .get()
                                                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                                                        @Override
                                                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                                            if (queryDocumentSnapshots.isEmpty()) {
                                                                                Customer customer = new Customer(name, id_num, selectedStore);
                                                                                customerReference.document(name+"_"+id_number).set(customer);
                                                                                storeFilter = selectedStore;
                                                                                filter(true);
                                                                                add_Dialog.dismiss();
                                                                            } else {
                                                                                Helper.showUserMessage(findViewById(android.R.id.content), "Company exists now or has" +
                                                                                        " been deleted", Snackbar.LENGTH_INDEFINITE, true);
                                                                            }
                                                                        }
                                                                    });
                                                        } else {
                                                            if (!document.exists() && name.contains("~add")) {
                                                                String customerName = name.replace("~add", "");
                                                                Customer customer = new Customer(customerName, id_num, selectedStore);
                                                                customerReference.document(customerName+ "_"+ id_number).set(customer);
                                                                storeFilter = selectedStore;
                                                                filter(true);
                                                                add_Dialog.dismiss();
                                                                Helper.showUserMessage(findViewById(android.R.id.content), "Admin Added " + customerName, Snackbar.LENGTH_INDEFINITE, true);
                                                            } else {
                                                                Helper.showUserMessage(findViewById(android.R.id.content), "Company exists now or has" +
                                                                        " been deleted", Snackbar.LENGTH_INDEFINITE, true);
                                                            }
                                                        }
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick (@NonNull MaterialDialog dialog, @NonNull DialogAction which){
                        dialog.dismiss();
                    }
                })
                .negativeText("Close")
                .positiveColor(getColor(R.color.green))
                .negativeColor(getColor(R.color.gray))
                .show();

        initializeDialog();
        if(isCompaniesSelected) {
            add_id_num.setVisibility(View.GONE);
            id_message.setVisibility(View.GONE);
            add_customer.setHint("Company Name");
        }
    }

    private void initializeDialog(){
        add_customer = (EditText) add_Dialog.findViewById(R.id.add_customer);
        add_id_num = (EditText) add_Dialog.findViewById(R.id.add_id_num);
        id_message = (TextView) add_Dialog.findViewById(R.id.id_message);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeLayout(Bundle savedInstanceState) {
        setContentView(R.layout.activity_checks);

        // creates bottom navigation for activity
        createBottomNavigation();

        // initializing main layout
        recyclerview = findViewById(R.id.recyclerview);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Helper.showLoading(progressDialog, context, false);
            }
        }, 500);

        if(Helper.getOrientation(context))
            recyclerview.setLayoutManager(new GridLayoutManager(context, 3));
        else
            recyclerview.setLayoutManager(new GridLayoutManager(context, 2));

        recyclerview.setHasFixedSize(true);
        recyclerview.setItemViewCacheSize(20);

        navigationTabStrip = (NavigationTabStrip) findViewById(R.id.navigation);
        navigationTabStrip.setTabIndex(1, true);
        navigationTabStrip.setStripType(NavigationTabStrip.StripType.POINT);
        navigationTabStrip.setStripGravity(NavigationTabStrip.StripGravity.BOTTOM);
        navigationTabStrip.setAnimationDuration(300);
        navigationTabStrip.setActiveColor(getColor(R.color.orange_red));
        navigationTabStrip.setInactiveColor(getColor(R.color.whitish_background));
        navigationTabStrip.setStripColor(getColor(R.color.colorPrimaryDark));
        navigationTabStrip.setStripWeight(15);
        navigationTabStrip.setStripFactor(2);
        navigationTabStrip.setCornersRadius(10);

        // initializing main layout
        cashCheckBtn = findViewById(R.id.cash_check_fab);
        goHomeStore = findViewById(R.id.go_home_store);
        empty_layout = findViewById(R.id.empty_layout);
        addCheckCompany = findViewById(R.id.add_Check_Company);

        storeFilter = null;

        // if orientation changes, then isCompaniesSelected is set to the value it was beforehand
        if (savedInstanceState != null) {
            isCompaniesSelected = savedInstanceState.getBoolean("selected");
            storeFilter = savedInstanceState.getString("filter");
        }
        else
            isCompaniesSelected = false;

        refreshRecyclerView();

        navigationTabStrip.setOnTabStripSelectedIndexListener(new NavigationTabStrip.OnTabStripSelectedIndexListener() {
            @Override
            public void onStartTabSelected(String title, int index) {
                if(index==0){
                    if(!isCompaniesSelected) {
                        isCompaniesSelected = true;
                        databasePath = getString(R.string.companies_keyword);
                        populateAdapter();
                    }
                }
                else if(index==1){
                    if(isCompaniesSelected) {
                        isCompaniesSelected = false;
                        databasePath = getString(R.string.customers_keyword);
                        populateAdapter();
                    }
                }
            }

            @Override
            public void onEndTabSelected(String title, int index) { }
        });

        goHomeStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeFilter = selectedStore;
                filter(true);
                goHomeStore.setVisibility(View.GONE);
            }
        });

        cashCheckBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationTabStrip.setTabIndex(1, true);
                if(isCompaniesSelected) {
                    isCompaniesSelected = false;
                    databasePath = getString(R.string.customers_keyword);
                    populateAdapter();
                }
                if(customers.size()==0)
                    Helper.showUserMessage(findViewById(android.R.id.content), "Add customer to cash a check", Snackbar.LENGTH_LONG, true);
                else
                    Helper.showUserMessage(findViewById(android.R.id.content), "Select Customer", Snackbar.LENGTH_LONG, true);
            }
        });

        addCheckCompany.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCompany();
            }
        });

        updateBtnSelected();
    }

    private void switchTab(String direction, int selectedTab){
        navigationTabStrip.setTabIndex(selectedTab, false);
        if(direction.equals("right")) {
            if (isCompaniesSelected) {
                isCompaniesSelected = false;
                databasePath = getString(R.string.customers_keyword);
                populateAdapter();
            }
        }
        else if(direction.equals("left")){
            if(!isCompaniesSelected) {
                isCompaniesSelected = true;
                databasePath = getString(R.string.companies_keyword);
                populateAdapter();
            }
        }
    }

    private void isEmpty(){
        if(isCompaniesSelected && companies.size()==0)
            empty_layout.setVisibility(View.VISIBLE);
        else if(isCompaniesSelected){
            empty_layout.setVisibility(View.GONE);
        }
        else if(!isCompaniesSelected && customers.size()==0){
            empty_layout.setVisibility(View.VISIBLE);
        }
        else{
            empty_layout.setVisibility(View.GONE);
        }
    }

    private void updateBtnSelected() {
        if(isCompaniesSelected)
            databasePath = getString(R.string.companies_keyword);
        else
            databasePath = getString(R.string.customers_keyword);
    }

    private void createBottomNavigation(){
        bottomNavigation = (BubbleNavigationConstraintView) findViewById(R.id.bottom_navigation);
        selectedActivity = 1;

        // selects checks icon (position 1) selected instead of default home icon (position 0)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                bottomNavigation.setCurrentActiveItem(selectedActivity);
            }
        }, 650);

        // on click listener for bottom navigation
        bottomNavigation.setNavigationChangeListener(new BubbleNavigationChangeListener() {
            @Override
            public void onNavigationChanged(View view, int position) {
                if(position == 0){
                    Intent home = new Intent(Checks.this, Main.class);
                    startActivity(home);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
                else if(position==2){
                    logoutDialog();
                }
            }
        });
    }

    // search menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_checks, menu);

        MenuItem search = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) search.getActionView();
        searchView.setQueryHint("Search name/ID #");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                searchUsers(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchUsers(newText);
                return false;
            }
        });

        return true;
    }

    private void searchUsers(String query) {
        if (query.length() > 0)
            query = query.toLowerCase();

        if(isCompaniesSelected) {
            ArrayList<Company> results = new ArrayList<>();
            for (Company company : companies) {
                if (company.getCompanyName() != null && company.getCompanyName().toLowerCase().contains(query)) {
                    results.add(company);
                }
            }
            populateSearchQuery(results);
        }
        else{
            ArrayList<Customer> results = new ArrayList<>();
            for (Customer customer : customers) {
                if (customer.getFullName() != null && (customer.getFullName().toLowerCase().contains(query) || String.valueOf(customer.getLastFourNumID()).contains(query))) {
                    results.add(customer);
                }
            }
            populateSearchQuery(results);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if( item.getItemId() == R.id.action_search){
        }
        else if(item.getItemId() == R.id.action_filter){
            new MaterialDialog.Builder(context)
                    .title("Select store")
                    .titleColor(context.getColor(R.color.orange_red))
                    .backgroundColor(context.getColor(R.color.black))
                    .positiveText("CONFIRM")
                    .negativeText("CLOSE")
                    .itemsColor(getColor(R.color.colorPrimary))
                    .widgetColor(getColor(R.color.colorPrimary))
                    .canceledOnTouchOutside(false)
                    .autoDismiss(false)
                    .items(R.array.stores)
                    .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            storeFilter = getResources().getStringArray(R.array.stores)[which].toLowerCase();
                            filter(true);
                            dialog.dismiss();
                            return true;
                        }
                    })
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .negativeColor(context.getColor(R.color.gray))
                    .positiveColor(context.getColor(R.color.green))
                    .show();
        }
        return true;
    }

    // launches camera
    public void launchCamera(Company company) {
        if(permissionDialog()){
            String[] permissions = {Manifest.permission.INTERNET, Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(permissions, REQUEST_IMAGE_CAPTURE);
        }
        else {
            boolean orientation = Helper.getOrientation(context);
            if (orientation)
                Helper.setOrientation(this, getString(R.string.landscape));
            else
                Helper.setOrientation(this, getString(R.string.portrait));

            company_pic = company;
            // Create the capture image intent
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the temporary File where the photo should go
                File photoFile = null;
                try {
                    photoFile = Camera.createTempImageFile(context);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    ex.printStackTrace();
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {

                    // Get the path of the temporary file
                    mTempPhotoPath = photoFile.getAbsolutePath();

                    // Get the content URI for the image file
                    Uri photoURI = FileProvider.getUriForFile(context,
                            FILE_PROVIDER_AUTHORITY,
                            photoFile);

                    // Add the URI so the camera can store the image
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                    // Launch the camera activity
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        }
    }

    // handles the result of taking a photo
    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            mResultsBitmap = Camera.resamplePic(context, mTempPhotoPath);
            // Delete the temporary image file
            Camera.deleteImageFile(context, mTempPhotoPath);

            if(company_pic.getCheckPath()!=null)
                deleteCheckDatabase(company_pic, false);

            // Saves the image and sets retrieves the location path to photo_Location
            String photo_Location = Camera.saveImage(context, mResultsBitmap, false);
            compress_upload(photo_Location);
            currentIdPath = photo_Location;

            dialog_Company = new MaterialDialog.Builder(context)
                    .title(company_pic.getCompanyName().split("_")[0] + " Check")
                    .titleColor(context.getColor(R.color.orange_red))
                    .backgroundColor(context.getColor(R.color.black))
                    .contentColor(context.getColor(R.color.bluish))
                    .customView(R.layout.dialog_company, true)
                    .positiveText("CLOSE")
                    .neutralText("RETAKE")
                    .negativeText("DELETE")
                    .canceledOnTouchOutside(false)
                    .autoDismiss(false)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog_Company.dismiss();
                            Helper.setOrientation(Checks.this, "None");
                        }
                    })
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog_Company.dismiss();
                            if(company_pic.getCheckPath()!=null && currentIdPath!=null) {
                                progressDialog = Helper.showLoading(progressDialog, context, true);
                                deleteCheckDatabase(company_pic, true);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Helper.showLoading(progressDialog, context, false);
                                    }
                                }, 500);
                            }
                            launchCamera(company_pic);
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if(company_pic.getCheckPath()!=null) {
                                progressDialog = Helper.showLoading(progressDialog, context, true);
                                deleteCheckDatabase(company_pic, true);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Helper.showLoading(progressDialog, context, false);
                                    }
                                }, 500);
                            }
                        }
                    })
                    .negativeColor(context.getColor(R.color.red))
                    .neutralColor(context.getColor(R.color.colorPrimary))
                    .positiveColor(context.getColor(R.color.gray))
                    .show();

            check_image = (ImageView) dialog_Company.findViewById(R.id.check_image);
            check_image_text = (TextView) dialog_Company.findViewById(R.id.check_image_text);

            check_image.setImageBitmap(mResultsBitmap);
            check_image_text.setVisibility(View.GONE);
        }
        else {
            Helper.showUserMessage(findViewById(android.R.id.content), "Error taking a picture", Snackbar.LENGTH_SHORT, false);
            Helper.setOrientation(this, "None");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED)
                Helper.showUserMessage(findViewById(android.R.id.content), getString(R.string.permission_prompt), Snackbar.LENGTH_SHORT, false);
        }
    }

    public void compress_upload(final String photo_Location){
        Uri file = Uri.fromFile(new File(photo_Location));
        final String path = company_pic.getCompanyName() + "_" + UUID.randomUUID() + ".jpg";
        storageRef = storage.getReference(path);
        UploadTask uploadTask = storageRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Helper.showUserMessage(findViewById(android.R.id.content), "Error Uploading", Snackbar.LENGTH_LONG, false);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                updateCheckDatabse(path);
            }
        });
    }

    private void updateCheckDatabse(final String path) {
        companyReference
                .document(company_pic.getCompanyName())
                .update("checkPath", path)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        company_pic.setCheckPath(path);
                    }
                });
    }

    private void deleteCheckDatabase(final Company company, final boolean update_Image) {
        // Create a reference to the file to delete
        storageRef = storage.getReference(company.getCheckPath());

        // Delete the file
        storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateCheckDatabse(null, company);
                if(update_Image){
                    check_image.setImageDrawable(context.getDrawable(R.drawable.icon_empty_check));
                    check_image_text.setVisibility(View.VISIBLE);
                }
                Helper.showUserMessage(findViewById(android.R.id.content), "Check Picture Deleted", Snackbar.LENGTH_LONG, false);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                try{
                   // Helper.showUserMessage(findViewById(android.R.id.content), "Error Deleting Picture", Snackbar.LENGTH_LONG, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateCheckDatabse(final String path, Company company) {
        db.collection(context.getString(R.string.companies_keyword))
                .document(company.getCompanyName())
                .update("checkPath", path)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        currentIdPath = path;
                    }
                });
    }

    private boolean permissionDialog(){
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED;
    }

    private void logoutDialog(){
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
                .onPositive((dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Helper.savePreference("login", null, context);
                    Intent home = new Intent(Checks.this, Login.class);
                    startActivity(home);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                })
                .onNegative((dialog, which) -> {
                    bottomNavigation.setCurrentActiveItem(selectedActivity);
                    dialog.dismiss();
                })
                .negativeColor(context.getColor(R.color.gray))
                .positiveColor(context.getColor(R.color.green))
                .contentColor((context.getColor(R.color.orange_red)))
                .show();
    }
}