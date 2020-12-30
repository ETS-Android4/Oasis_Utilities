package com.akapps.oasisutilities.recyclerviews;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.akapps.oasisutilities.activities.Checks;
import com.akapps.oasisutilities.activities.CustomerInfo;
import com.akapps.oasisutilities.classes.Camera;
import com.akapps.oasisutilities.classes.Company;
import com.akapps.oasisutilities.classes.Customer;
import com.akapps.oasisutilities.classes.Helper;
import com.akapps.oasisutilities.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;

public class CustomerInfo_Recyclerview extends RecyclerView.Adapter<CustomerInfo_Recyclerview.MyViewHolder>{

    // project data and database
    private ArrayList<Company> companies;
    private ArrayList<Customer> customers;
    private boolean isCompaniesSelected;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String currentCompanyPath;
    private String currentStore;

    // activity info
    private Context context;
    private View view;

    // dialog
    private MaterialDialog dialog_Company;
    private ImageView check_image;
    private TextView check_image_text;
    private StorageReference storageRef;
    private Dialog progressDialog;

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView check_name;
        private View view;
        private LinearLayout check_fake_or_real;

        public MyViewHolder(View v) {
            super(v);
            check_name = v.findViewById(R.id.name);
            check_fake_or_real= v.findViewById(R.id.check_status_indicator);
            view = v;
        }
    }

    // this constructor is for populating ListView for Searching
    public CustomerInfo_Recyclerview(ArrayList data, FirebaseFirestore db , boolean isCompaniesSelected, View view, String currentStore) {
        if(isCompaniesSelected)
            this.companies = data;
        else
            this.customers = data;
        this.db = db;
        this.isCompaniesSelected = isCompaniesSelected;
        this.view = view;
        this.currentStore = currentStore;
    }

    // this constructor is for populating recyclerview
    public CustomerInfo_Recyclerview(ArrayList<Company> companies, ArrayList<Customer> customers, boolean isCompaniesSelected,
                                     FirebaseFirestore db, View view, String currentStore) {
        this.db = db;
        this.companies = companies;
        this.customers = customers;
        this.isCompaniesSelected = isCompaniesSelected;
        this.view = view;
        this.currentStore = currentStore;
    }

    @Override
    public CustomerInfo_Recyclerview.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v =  LayoutInflater.from(parent.getContext()).inflate(R.layout.company_customer_item, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        context = parent.getContext();
        return vh;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        storage = FirebaseStorage.getInstance();
        // based on selected tab, different data is populated
        // here companies is populated
        if (isCompaniesSelected) {
            Company currentCompany = companies.get(position);
            holder.check_name.setText(currentCompany.getCompanyName().split("_")[0]);

            String changeCompanyStatus = "Fake";
            if (currentCompany.isCashStatus())
                changeCompanyStatus = "Real";

            if (currentCompany.isCashStatus())
                holder.check_fake_or_real.setBackgroundColor(context.getColor(R.color.red));

            holder.view.setOnClickListener(v -> {
                context = v.getContext();

                if (currentStore.equals(currentCompany.getStore())) {
                    dialog_Company = new MaterialDialog.Builder(context)
                            .title(currentCompany.getCompanyName().split("_")[0] + " Check")
                            .titleColor(context.getColor(R.color.orange_red))
                            .backgroundColor(context.getColor(R.color.black))
                            .customView(R.layout.dialog_company, true)
                            .positiveText("CLOSE")
                            .neutralText("RETAKE")
                            .negativeText("DELETE")
                            .canceledOnTouchOutside(false)
                            .autoDismiss(false)
                            .onPositive((dialog, which) -> dialog_Company.dismiss())
                            .onNeutral((dialog, which) -> {
                                dialog_Company.dismiss();
                                ((Checks) context).launchCamera(currentCompany);
                            })
                            .onNegative((dialog, which) -> {
                                if (currentCompany.getCheckPath() != null && currentCompanyPath != null) {
                                    progressDialog = Helper.showLoading(progressDialog, context, true);
                                    deleteCheckDatabase(currentCompany, v, true);
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Helper.showLoading(progressDialog, context, false);
                                        }
                                    }, 500);
                                }
                            })
                            .neutralColor(context.getColor(R.color.colorPrimary))
                            .positiveColor(context.getColor(R.color.gray))
                            .negativeColor(context.getColor(R.color.red))
                            .show();
                }
                else {
                    dialog_Company = new MaterialDialog.Builder(context)
                            .title(currentCompany.getCompanyName().split("_")[0] + " Check")
                            .titleColor(context.getColor(R.color.orange_red))
                            .backgroundColor(context.getColor(R.color.black))
                            .customView(R.layout.dialog_company, true)
                            .positiveText("CLOSE")
                            .canceledOnTouchOutside(false)
                            .autoDismiss(false)
                            .onPositive((dialog, which) -> dialog_Company.dismiss())
                            .positiveColor(context.getColor(R.color.gray))
                            .show();
                }

                check_image = (ImageView) dialog_Company.findViewById(R.id.check_image);
                check_image_text = (TextView) dialog_Company.findViewById(R.id.check_image_text);

                check_image.setOnClickListener(v1 -> {
                    if (currentCompanyPath != null && currentCompany.getCheckPath() != null) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(currentCompanyPath), "image/*");
                        context.startActivity(intent);
                    }
                });

                if (currentCompany.getCheckPath() == null) {
                    check_image.setImageDrawable(context.getDrawable(R.drawable.icon_empty_check));
                    check_image_text.setVisibility(View.VISIBLE);
                } else {
                    progressDialog = Helper.showLoading(progressDialog, context, true);
                    downloadFromDatabase(currentCompany, check_image);
                    check_image_text.setVisibility(View.GONE);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Helper.showLoading(progressDialog, context, false);
                        }
                    }, 500);
                }
            });

            final String finalChangeCompanyStatus = changeCompanyStatus;
            holder.view.setOnLongClickListener(v -> {
                if (currentStore.equals(currentCompany.getStore())) {
                    new MaterialDialog.Builder(context)
                            .title("Update " + currentCompany.getCompanyName().split("_")[0])
                            .titleColor(context.getColor(R.color.orange_red))
                            .backgroundColor(context.getColor(R.color.black))
                            .contentColor(context.getColor(R.color.bluish))
                            .content("Change Name or Delete?")
                            .contentGravity(GravityEnum.CENTER)
                            .positiveText("EDIT")
                            .negativeText(finalChangeCompanyStatus)
                            .neutralText("DELETE")
                            .canceledOnTouchOutside(true)
                            .autoDismiss(false)
                            .input("Enter Company name", currentCompany.getCompanyName().split("_")[0], false, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                    if (!input.toString().matches("^[a-z A-Z'-]*$")) {
                                        dialog.getInputEditText().setError("Only ('-) allowed");
                                    } else if (input.toString().toLowerCase().equals(currentCompany.getCompanyName().split("_")[0].toLowerCase())) {
                                        dialog.getInputEditText().setError("Same name");
                                    } else {
                                        String companyReference = currentCompany.getCompanyName().split("_")[1];
                                        db.collection(context.getString(R.string.companies_keyword)).document(currentCompany.getCompanyName()).delete();
                                        currentCompany.setCompanyName(input.toString() + "_" + companyReference);
                                        db.collection(context.getString(R.string.companies_keyword)).document(input.toString() + "_" + companyReference).set(currentCompany);
                                        notifyDataSetChanged();
                                        dialog.dismiss();
                                    }
                                }
                            })
                            .onNegative((dialog, which) -> {
                                boolean companystatus;
                                companystatus = !currentCompany.isCashStatus();
                                db.collection(context.getString(R.string.companies_keyword)).document(currentCompany.getCompanyName())
                                        .update("cashStatus", companystatus);
                                Helper.showUserMessage(view, currentCompany.getCompanyName().split("_")[0] + " Updated", Snackbar.LENGTH_LONG, false);
                                dialog.dismiss();
                                notifyDataSetChanged();
                            })
                            .onNeutral((dialog, which) -> {
                                db.collection(context.getString(R.string.companies_keyword)).document(currentCompany.getCompanyName()).delete();
                                notifyDataSetChanged();
                                Helper.showUserMessage(view, currentCompany.getCompanyName().split("_")[0] + " Deleted", Snackbar.LENGTH_LONG, false);
                                dialog.dismiss();
                            })
                            .neutralColor(context.getColor(R.color.red))
                            .positiveColor(context.getColor(R.color.green))
                            .negativeColor(context.getColor(R.color.gold))
                            .show();
                }
                else
                    Helper.deniedDialog(context);
                return true;
            });

        }
        else {
            final Customer currentCustomer = customers.get(position);
            holder.check_name.setText(currentCustomer.getFullName());

            String changeCustomerStatus = "Don't Cash";
            if (currentCustomer.isCashedFakeCheck())
                changeCustomerStatus = "Cash";

            if (currentCustomer.isCashedFakeCheck())
                holder.check_fake_or_real.setBackgroundColor(context.getColor(R.color.red));

            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    context = v.getContext();
                    Intent open_Project = new Intent(context, CustomerInfo.class);
                    open_Project.putExtra(context.getString(R.string.customers_keyword), currentCustomer.getFullName() + "_" + currentCustomer.getLastFourNumID());
                    open_Project.putExtra("store", currentStore);
                    context.startActivity(open_Project);
                }
            });

            final String finalChangeCustomerStatus = changeCustomerStatus;
            holder.view.setOnLongClickListener(v -> {
                if (currentStore.equals(currentCustomer.getStore())) {
                    new MaterialDialog.Builder(context)
                            .title("Update " + currentCustomer.getFullName())
                            .titleColor(context.getColor(R.color.orange_red))
                            .backgroundColor(context.getColor(R.color.black))
                            .contentColor(context.getColor(R.color.bluish))
                            .content("Change Name or Delete?")
                            .contentGravity(GravityEnum.CENTER)
                            .positiveText("EDIT")
                            .negativeText(finalChangeCustomerStatus)
                            .neutralText("DELETE")
                            .canceledOnTouchOutside(true)
                            .autoDismiss(false)
                            .input("Enter Customer name", currentCustomer.getFullName(), false, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                    if (!input.toString().matches("^[a-z A-Z'-]*$")) {
                                        dialog.getInputEditText().setError("Only ('-) allowed");
                                    } else if (input.toString().toLowerCase().equals(currentCustomer.getFullName().split("_")[0].toLowerCase())) {
                                        dialog.getInputEditText().setError("Same name");
                                    } else {
                                        db.collection(context.getString(R.string.customers_keyword)).document(currentCustomer.getFullName() + "_" + currentCustomer.getLastFourNumID()).delete();
                                        currentCustomer.setFullName(input.toString());
                                        db.collection(context.getString(R.string.customers_keyword)).document(input.toString() + "_" + currentCustomer.getLastFourNumID()).set(currentCustomer);
                                        notifyDataSetChanged();
                                        dialog.dismiss();
                                    }
                                }
                            })
                            .onPositive((dialog, which) -> {
                            })
                            .onNegative((dialog, which) -> {
                                boolean customerStatus;
                                customerStatus = !currentCustomer.isCashedFakeCheck();
                                db.collection(context.getString(R.string.customers_keyword)).document(currentCustomer.getFullName() + "_" + currentCustomer.getLastFourNumID())
                                        .update("cashedFakeCheck", customerStatus);
                                Helper.showUserMessage(view, currentCustomer.getFullName() + " Updated", Snackbar.LENGTH_LONG, false);
                                dialog.dismiss();
                                notifyDataSetChanged();
                            })
                            .onNeutral((dialog, which) -> {
                                db.collection(context.getString(R.string.customers_keyword)).document(currentCustomer.getFullName() + "_" + currentCustomer.getLastFourNumID()).delete();
                                notifyDataSetChanged();
                                Helper.showUserMessage(view, currentCustomer.getFullName() + " Deleted", Snackbar.LENGTH_LONG, false);
                                dialog.dismiss();
                            })
                            .neutralColor(context.getColor(R.color.red))
                            .positiveColor(context.getColor(R.color.green))
                            .negativeColor(context.getColor(R.color.gold))
                            .show();
                } else {
                    Helper.deniedDialog(context);
                }
                return true;
            });
        }
    }

    private void deleteCheckDatabase(final Company company, final View v, final boolean update_Image) {
        // Create a reference to the file to delete
        storageRef = storage.getReference(company.getCheckPath());

        // Delete the file
        storageRef.delete().addOnSuccessListener(aVoid -> {
            updateCheckDatabase(null, company);
            if(update_Image){
                check_image.setImageDrawable(context.getDrawable(R.drawable.icon_empty_check));
                check_image_text.setVisibility(View.VISIBLE);
            }
            Helper.showUserMessage(view, "Check Picture Deleted", Snackbar.LENGTH_LONG, false);
        }).addOnFailureListener(exception -> {
            try{
                Helper.showUserMessage(view, "Error Deleting Picture", Snackbar.LENGTH_LONG, false);
            } catch (Exception e) {
                currentCompanyPath = null;
                e.printStackTrace();
            }
        });
    }

    private void updateCheckDatabase(final String path, Company company) {
        db.collection(context.getString(R.string.companies_keyword))
                .document(company.getCompanyName())
                .update("checkPath", path)
                .addOnCompleteListener(task -> currentCompanyPath = path);
    }

    @Override
    public int getItemCount() {
        if(isCompaniesSelected)
            return companies.size();
        else
            return customers.size();
    }

    private void downloadFromDatabase(Company currentCompany, final ImageView customer_image){
        storageRef = storage.getReference(currentCompany.getCheckPath());

        final long ONE_MEGABYTE = 1024 * 1024;
        storageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes , 0, bytes.length);
            currentCompanyPath = Camera.saveImage(context, bitmap, false);
            customer_image.setImageBitmap(bitmap);
        }).addOnFailureListener(exception -> {
            check_image.setImageDrawable(context.getDrawable(R.drawable.icon_empty_check));
            check_image_text.setVisibility(View.VISIBLE);
            check_image_text.setText("Error Downloading\nFile might have been deleted");
        });
    }

}

