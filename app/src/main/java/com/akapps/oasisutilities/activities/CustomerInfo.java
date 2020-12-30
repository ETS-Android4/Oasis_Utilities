package com.akapps.oasisutilities.activities;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.akapps.oasisutilities.classes.Camera;
import com.akapps.oasisutilities.classes.Check;
import com.akapps.oasisutilities.classes.Company;
import com.akapps.oasisutilities.classes.Customer;
import com.akapps.oasisutilities.classes.Helper;
import com.akapps.oasisutilities.classes.SampleSearchModel;
import com.akapps.oasisutilities.R;
import com.akapps.oasisutilities.recyclerviews.Checks_Recyclerview;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import javax.annotation.Nullable;

import ir.mirrajabi.searchdialog.SimpleSearchDialogCompat;
import ir.mirrajabi.searchdialog.core.BaseSearchDialogCompat;
import ir.mirrajabi.searchdialog.core.SearchResultListener;

public class CustomerInfo extends AppCompatActivity {

    // activity data
    private Context context;

    // database
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String currentCustomer;
    private Customer customer;
    private CollectionReference checkRef;
    private ListenerRegistration checkListener;
    private ArrayList<Check> checks = new ArrayList<>();
    private ArrayList<String> companies = new ArrayList<>();
    private ListenerRegistration companyListener;

    // camera
    private String FILE_PROVIDER_AUTHORITY;
    private int REQUEST_IMAGE_CAPTURE = 1;
    private String mTempPhotoPath;
    private Bitmap mResultsBitmap;
    private String currentIdPath;

    // layout
    private LinearLayout checkLayout;
    private ImageView customerImage;
    private TextView customerName;
    private TextView customerDate;
    private TextView customerChecks;
    private TextView emptyText;
    private Dialog progressDialog;
    private RecyclerView recyclerviewChecks;
    private RecyclerView.Adapter adapter;
    private MaterialButton addCheck;
    private LinearLayout empty_layout;
    private SwipeRefreshLayout swipeRefreshLayout;

    // check dialog
    private Check check;
    private boolean isCheck;
    private MaterialDialog dialog_Company;
    private ImageView check_image;
    private TextView check_image_text;
    private String currentCheckPath;

    // dialog layout
    private MaterialDialog add_Dialog;
    private EditText cashed_by;
    private EditText check_amount;
    private TextView id_message;

    private String currentStore;
    private boolean isEditable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);
        context = this;

        FILE_PROVIDER_AUTHORITY = getString(R.string.fileprovider);

        currentCustomer = getIntent().getStringExtra(getString(R.string.customers_keyword));
        currentStore = getIntent().getStringExtra("store");
        // show loading screen
        progressDialog = Helper.showLoading(progressDialog, context, true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(progressDialog!=null)
            progressDialog.cancel();
    }

    private void initializeDatabase(boolean update){
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        db.collection(getString(R.string.customers_keyword))
                .document(currentCustomer)
                .get()
                .addOnCompleteListener(task -> {
                    DocumentSnapshot doc = task.getResult();
                    if(update){
                        Customer currentCustomer = doc.toObject(Customer.class);
                        if(currentCustomer.getIdPath()!=null && currentCustomer.getIdPath().equals(customer.getIdPath()) ||
                                currentCustomer.getIdPath()==null && customer.getIdPath()==null)
                            Helper.showUserMessage(findViewById(android.R.id.content), "No changes", Snackbar.LENGTH_SHORT, false);
                        else
                            initializeDatabase(false);
                    }
                    else {
                        customer = doc.toObject(Customer.class);
                        isEditable = currentStore.equals(customer.getStore());
                        populateAdapter();
                        initializeLayout();
                        Helper.showUserMessage(findViewById(android.R.id.content), "Customer is Up-to-Date", Snackbar.LENGTH_SHORT, false);
                        if (customer.getIdPath() != null) {
                            downloadFromDatabase(false);
                            emptyText.setVisibility(View.GONE);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Helper.showLoading(progressDialog, context, false);
                                    checkLayout.setVisibility(View.VISIBLE);
                                }
                            }, 1500);
                        } else {
                            showEmptyCheck();
                            checkLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // initializes layout and database
        initializeDatabase(false);
        //initializes Recyclerview
        initializeRecyclerView();

        checkRef = db.collection(getString(R.string.customers_keyword))
                .document(currentCustomer)
                .collection(getString(R.string.checks_keyword));

        Query queryCheck = checkRef.orderBy("checkDate", Query.Direction.DESCENDING);

        Query queryCompany = db.collection(getString(R.string.companies_keyword)).orderBy("dateAdded", Query.Direction.DESCENDING).whereEqualTo("store", currentStore);

        checkListener = queryCheck
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            System.err.println("Listen failed:" + e);
                            return;
                        }

                        checks = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots) {
                            Check check = doc.toObject(Check.class);
                            checks.add(check);
                        }
                        populateAdapter();

                        if(customerChecks!=null)
                            customerChecks.setText("Checks:  " + checks.size());
                    }
                });

        companyListener = queryCompany
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            System.err.println("Listen failed:" + e);
                            return;
                        }

                        companies = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots) {
                            Company company = doc.toObject(Company.class);
                            companies.add(company.getCompanyName());
                        }
                        populateAdapter();
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        checkListener.remove();
        companyListener.remove();
    }

    public void showEmptyCheck(){
        customerImage.setImageDrawable(getDrawable(R.drawable.icon_profile_empty));
        emptyText.setVisibility(View.VISIBLE);
        Helper.showLoading(progressDialog, context, false);
    }

    // populates the recyclerview
    private void populateAdapter() {
        isEmpty();
        adapter = new Checks_Recyclerview(checks, db, checkRef, isEditable, findViewById(android.R.id.content));
        recyclerviewChecks.setAdapter(adapter);
    }

    public void initializeLayout(){
        setTitle(currentCustomer.split("_")[0]);

        customerImage = findViewById(R.id.customer_image);
        customerName = findViewById(R.id.customer_name);
        customerDate = findViewById(R.id.customer_date);
        customerChecks = findViewById(R.id.customer_checks);
        emptyText = findViewById(R.id.empty_text);
        checkLayout = findViewById(R.id.checks_layout);
        addCheck = findViewById(R.id.cash_Check);
        swipeRefreshLayout = findViewById(R.id.swiperefresh);

        try {
            customerName.setText(customer.getFullName() + "\n\nID: " + customer.getLastFourNumID()
            + "\n\nStore: " + Helper.toUpperCase(customer.getStore()));
            customerDate.setText("Date Added:\n" + customer.getDateAdded());
            customerChecks.setText("Checks:  " + checks.size());
        }catch (Exception e){
            Intent goback = new Intent(CustomerInfo.this, Checks.class);
            startActivity(goback);
        }

        addCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!currentStore.equals(customer.getStore()))
                    Helper.deniedDialog(context);
                else
                    openCheckInput();
            }
        });

        customerImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentIdPath != null && customer.getIdPath() != null) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(currentIdPath), "image/*");
                    startActivity(intent);
                }
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // retrieves data from database to see if there is any updates
                // (this breaks if someone has updated the name of customer in another device)
                initializeDatabase(true);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void initializeRecyclerView(){
        recyclerviewChecks = findViewById(R.id.checks_recyclerview);
        recyclerviewChecks.setHasFixedSize(true);
        recyclerviewChecks.setItemViewCacheSize(20);
        recyclerviewChecks.setNestedScrollingEnabled(false);

        if(Helper.isTablet(context))
            if(Helper.getOrientation(context))
                recyclerviewChecks.setLayoutManager(new GridLayoutManager(context, 3));
            else
                recyclerviewChecks.setLayoutManager(new GridLayoutManager(context, 2));
        else
            if(Helper.getOrientation(context))
                recyclerviewChecks.setLayoutManager(new GridLayoutManager(context, 2));
            else
                recyclerviewChecks.setLayoutManager(new LinearLayoutManager(context));
    }

    public void openCheckInput() {
        add_Dialog = new MaterialDialog.Builder(context)
                .title("Adding Check")
                .titleColor(getColor(R.color.orange_red))
                .backgroundColor(getColor(R.color.black))
                .customView(R.layout.dialog_add_customer, true)
                .positiveText("SELECT COMPANY")
                .negativeText("CLOSE")
                .canceledOnTouchOutside(false)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        String cashed = cashed_by.getText().toString();
                        String amountString = check_amount.getText().toString();
                        if(amountString.length()!=0) {
                            double amount = Double.valueOf(check_amount.getText().toString());
                            if(cashed.equals(""))
                                cashed = "Unknown";
                            selectCompanyDialog(amount, cashed);
                            dialog.dismiss();
                        }
                        else{
                            check_amount.setError("Empty");
                        }
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .positiveColor(getColor(R.color.green))
                .negativeColor(getColor(R.color.red))
                .show();

        initializeDialog();
    }

    private void initializeDialog(){
        cashed_by = (EditText) add_Dialog.findViewById(R.id.add_customer);
        check_amount = (EditText) add_Dialog.findViewById(R.id.add_id_num);
        id_message = (TextView) add_Dialog.findViewById(R.id.id_message);

        check_amount.setHint("Enter Check Amount");
        check_amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        check_amount.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        id_message.setVisibility(View.GONE);
        cashed_by.setHint("Name of Cashier (optional)");
    }

    private void isEmpty(){
        empty_layout = findViewById(R.id.empty_layout);
        if(checks.size()==0)
            empty_layout.setVisibility(View.VISIBLE);
        else{
            empty_layout.setVisibility(View.GONE);
        }
    }

    private ArrayList<SampleSearchModel> createSampleData(){
        ArrayList<SampleSearchModel> items = new ArrayList<>();
        for(int i=0; i<companies.size(); i++){
            items.add(new SampleSearchModel(companies.get(i).split("_")[0]));
        }
        return items;
    }

    public void selectCompanyDialog(final double checkAmount, final String cashed){
        if(companies.size()!=0) {
            SimpleSearchDialogCompat companyDialog = new SimpleSearchDialogCompat(this, "Search...",
                    "Look for company...?", null, createSampleData(),
                    new SearchResultListener<SampleSearchModel>() {
                        @Override
                        public void onSelected(BaseSearchDialogCompat dialog,
                                               SampleSearchModel item, int position) {
                            checkInputDialog(item.getTitle(), checkAmount, cashed);
                            dialog.dismiss();
                        }
                    });
            companyDialog.setCanceledOnTouchOutside(false);
            companyDialog.show();
        }
        else
            Helper.showUserMessage(findViewById(android.R.id.content), "No companies to add", Snackbar.LENGTH_SHORT, false);
    }

    private void checkInputDialog(final String selectedCompany, final double checkAmount, final String cashedBy) {
        new MaterialDialog.Builder(context)
                .title("Check:")
                .titleColor(getColor(R.color.orange_red))
                .backgroundColor(getColor(R.color.black))
                .positiveText("CASH")
                .content("Cashing: \n" + selectedCompany + "\n\nfor $" + checkAmount + "\n\nby " + cashedBy)
                .contentColor(getColor(R.color.green))
                .negativeText("CLOSE")
                .canceledOnTouchOutside(false)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Check check;
                        if (cashedBy != null && cashedBy.length() > 0) {
                            check = new Check(selectedCompany, checkAmount, cashedBy, (int)(Math.random() * (100000 + 1)));
                        } else {
                            check = new Check(selectedCompany, checkAmount, "Unknown", (int)(Math.random() * (100000 + 1)));
                        }
                        checkRef.document(selectedCompany + "_" + check.getCheckID()).set(check);
                        dialog.dismiss();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .positiveColor(getColor(R.color.green))
                .negativeColor(getColor(R.color.red))
                .show();
    }

    // launches camera
    public void launchCamera(Check check, boolean isCheck) {
        if(permissionDialog()){
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, REQUEST_IMAGE_CAPTURE);
        }
        else {
            this.check = check;
            this.isCheck = isCheck;
            boolean orientation = Helper.getOrientation(context);
            if (orientation)
                Helper.setOrientation(this, getString(R.string.landscape));
            else
                Helper.setOrientation(this, getString(R.string.portrait));
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
    public void onActivityResult(int requestCode, int resultCode, Intent data){
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                mResultsBitmap = Camera.resamplePic(context, mTempPhotoPath);
                // Delete the temporary image file
                Camera.deleteImageFile(context, mTempPhotoPath);
                // current photo location
                String photo_Location = Camera.saveImage(context, mResultsBitmap, isCheck);

                if (isCheck) {
                    if (check.getCheckPath() != null) {
                        deleteCheckDatabase(isCheck, true);
                    }
                    currentCheckPath = photo_Location;
                    compress_upload(currentCheckPath, isCheck);
                    dialog_Company = new MaterialDialog.Builder(context)
                            .title(check.getCompanyName() + " Check Photo")
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
                                    isCheck = false;
                                    Helper.setOrientation(CustomerInfo.this, "None");
                                }
                            })
                            .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog_Company.dismiss();
                                    if (check.getCheckPath() != null && currentCheckPath != null) {
                                        progressDialog = Helper.showLoading(progressDialog, context, true);
                                        deleteCheckDatabase(true, true);
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Helper.showLoading(progressDialog, context, false);
                                            }
                                        }, 500);
                                    }
                                    launchCamera(check, true);
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    if (check.getCheckPath() != null && currentCheckPath != null) {
                                        progressDialog = Helper.showLoading(progressDialog, context, true);
                                        deleteCheckDatabase(true, true);
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
                } else {
                    currentIdPath = photo_Location;
                    customerImage.setImageBitmap(mResultsBitmap); //for example I put bmp in an ImageView
                    if (customer.getIdPath() != null) {
                        deleteCheckDatabase(isCheck, false);
                    }
                    compress_upload(photo_Location, isCheck);
                }
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

    public void compress_upload(final String photo_Location, final boolean isCheck){
        Uri file = Uri.fromFile(new File(photo_Location));
        String path;
        if(isCheck)
            path = check.getCheckName()+ "_" + check.getCheckID() + "_" + UUID.randomUUID() + ".jpg";
        else
            path = currentCustomer + "_" + UUID.randomUUID() + ".jpg";
        final String final_path = path;
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
                emptyText.setVisibility(View.GONE);
                updateCheckDatabse(final_path, photo_Location, isCheck);
            }
        });
    }

    private void updateCheckDatabse(final String path, final String photoLocation, boolean isCheck) {
        if(isCheck){
            checkRef.document(check.getCheckName()+ "_" + check.getCheckID())
                    .update("checkPath", path)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @RequiresApi(api = Build.VERSION_CODES.R)
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            check.setCheckPath(path);
                            currentCheckPath = photoLocation;
                            mResultsBitmap = Camera.resamplePic(context, photoLocation);
                            check_image.setImageBitmap(mResultsBitmap);
                            Helper.setOrientation(CustomerInfo.this, "None");
                        }
                    });
        }
        else {
            db.collection(getString(R.string.customers_keyword))
                    .document(currentCustomer)
                    .update("idPath", path)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @RequiresApi(api = Build.VERSION_CODES.R)
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            customer.setIdPath(path);
                            currentIdPath = photoLocation;
                            mResultsBitmap = Camera.resamplePic(context, photoLocation);
                            if(path!=null && photoLocation!=null)
                                customerImage.setImageBitmap(mResultsBitmap);
                            Helper.setOrientation(CustomerInfo.this, "None");
                        }
                    });
        }
    }

    private void deleteCheckDatabase(final boolean isCheck, final boolean update_Image) {
        if(isCheck){
            // Create a reference to the file to delete
            storageRef = storage.getReference(check.getCheckPath());
        }
        else {
            // Create a reference to the file to delete
            storageRef = storage.getReference(customer.getIdPath());
        }
        updateCheckDatabse(null, null, isCheck);

        // Delete the file
        storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Helper.showUserMessage(findViewById(android.R.id.content), "Check Picture Deleted", Snackbar.LENGTH_LONG, false);
                if(isCheck){
                    if(update_Image){
                        check_image.setImageDrawable(context.getDrawable(R.drawable.icon_empty_check));
                        check_image_text.setVisibility(View.VISIBLE);
                    }
                    Helper.showUserMessage(findViewById(android.R.id.content), "Check Picture Deleted", Snackbar.LENGTH_LONG, false);
                }
                else {
                    showEmptyCheck();
                    Helper.showUserMessage(findViewById(android.R.id.content), "ID Picture Deleted", Snackbar.LENGTH_LONG, false);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Helper.showUserMessage(findViewById(android.R.id.content), "Error Deleting Picture", Snackbar.LENGTH_LONG, false);
            }
        });
    }

    private void downloadFromDatabase(final boolean isCheck){
        if(isCheck)
            storageRef = storage.getReference(check.getCheckName()+ "_" + check.getCheckID());
        else
            storageRef = storage.getReference(customer.getIdPath());

        final long ONE_MEGABYTE = 1024 * 1024;
        storageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes , 0, bytes.length);
                if(isCheck){
                    currentCheckPath = Camera.saveImage(context, bitmap, isCheck);
                    check_image.setImageBitmap(bitmap);
                }
                else {
                    currentIdPath = Camera.saveImage(context, bitmap, false);
                    customerImage.setImageBitmap(bitmap);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                showEmptyCheck();
                Helper.showUserMessage(findViewById(android.R.id.content), "Error Downloading check", Snackbar.LENGTH_LONG, false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_customer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        else if(item.getItemId() == R.id.action_delete){
            if(!currentStore.equals(customer.getStore()))
                Helper.deniedDialog(context);
            else if(customer.getIdPath()!=null && customer.getIdPath().length()>0) {
                new MaterialDialog.Builder(context)
                        .title("Customer ID")
                        .titleColor(context.getColor(R.color.orange_red))
                        .backgroundColor(context.getColor(R.color.black))
                        .contentColor(context.getColor(R.color.bluish))
                        .content("Are you sure?")
                        .contentGravity(GravityEnum.CENTER)
                        .positiveText("DELETE")
                        .negativeText("CLOSE")
                        .canceledOnTouchOutside(false)
                        .autoDismiss(false)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                deleteCheckDatabase(isCheck, true);
                                dialog.dismiss();
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick (@NonNull MaterialDialog dialog, @NonNull DialogAction which){
                                dialog.dismiss();
                            }
                        })
                        .positiveColor(context.getColor(R.color.red))
                        .negativeColor(context.getColor(R.color.gray))
                        .show();
            }
            else
                Helper.showUserMessage(findViewById(android.R.id.content), "No Check to delete", Snackbar.LENGTH_LONG, false);
        }
        else if(item.getItemId() == R.id.action_camera){
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (permissionDialog() && currentStore.equals(customer.getStore())) {
                requestPermissions(permissions, REQUEST_IMAGE_CAPTURE);
            }
            else {
                if(!currentStore.equals(customer.getStore()))
                    Helper.deniedDialog(context);
                else {
                    new MaterialDialog.Builder(context)
                            .title("Customer ID")
                            .titleColor(context.getColor(R.color.orange_red))
                            .backgroundColor(context.getColor(R.color.black))
                            .contentColor(context.getColor(R.color.bluish))
                            .content("Take picture of ID?")
                            .contentGravity(GravityEnum.CENTER)
                            .positiveText("CONFIRM")
                            .negativeText("CLOSE")
                            .canceledOnTouchOutside(false)
                            .autoDismiss(false)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    launchCamera(null, isCheck);
                                    dialog.dismiss();
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                }
                            })
                            .positiveColor(context.getColor(R.color.red))
                            .negativeColor(context.getColor(R.color.gray))
                            .show();
                }
            }
        }
        return true;
    }

    private boolean permissionDialog(){
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED;
    }
}