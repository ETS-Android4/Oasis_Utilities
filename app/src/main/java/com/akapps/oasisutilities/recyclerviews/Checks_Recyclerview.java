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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.akapps.oasisutilities.activities.CustomerInfo;
import com.akapps.oasisutilities.classes.Camera;
import com.akapps.oasisutilities.classes.Check;
import com.akapps.oasisutilities.classes.Helper;
import com.akapps.oasisutilities.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class Checks_Recyclerview extends RecyclerView.Adapter<Checks_Recyclerview.MyViewHolder>{

    // project data
    private ArrayList<Check> checks;
    private String currentCheckPath;

    // activity info
    private View view;
    private Context context;

    // database
    private FirebaseFirestore db;
    private CollectionReference collectionReference;
    private StorageReference storageRef;
    private FirebaseStorage storage;

    // dialog layout
    private MaterialDialog edit_Check_Dialog;
    private EditText check_New_Name;
    private EditText check_New_Amount;

    // dialog
    private MaterialDialog dialog_Check;
    private ImageView check_image;
    private TextView check_image_text;
    private Dialog progressDialog;

    private boolean isEditAllowed;

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView company_name;
        private TextView check_amount;
        private TextView date_cashed;
        private TextView cashed_by;
        private ImageView check_view;
        private LinearLayout check_fake_or_real;
        private View view;

        public MyViewHolder(View v) {
            super(v);
            company_name = v.findViewById(R.id.company_name);
            check_amount= v.findViewById(R.id.check_amount);
            date_cashed= v.findViewById(R.id.date_cashed);
            cashed_by= v.findViewById(R.id.cashed_by);
            check_fake_or_real= v.findViewById(R.id.check_status_indicator);
            check_view = v.findViewById(R.id.check_view);
            view = v;
        }
    }

    public Checks_Recyclerview(ArrayList<Check> checks, FirebaseFirestore db, CollectionReference checkRef, boolean isEditAllowed, View view) {
        this.checks = checks;
        this.db = db;
        this.collectionReference = checkRef;
        this.isEditAllowed = isEditAllowed;
        this.view = view;
    }

    @Override
    public Checks_Recyclerview.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v =  LayoutInflater.from(parent.getContext()).inflate(R.layout.check_item, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        context = parent.getContext();
        return vh;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        Check currentCheck = checks.get(position);

        holder.company_name.setText(currentCheck.getCheckName().split("_")[0]);
        holder.check_amount.setText("$" + NumberFormat.getNumberInstance(Locale.US).format(currentCheck.getCheckAmount()));
        holder.date_cashed.setText(currentCheck.getCheckDate());
        holder.cashed_by.setText(currentCheck.getCheckCashedBy());

        if(currentCheck.isFakeCheck())
            holder.check_fake_or_real.setBackgroundColor(context.getColor(R.color.red));

        holder.check_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if(!isEditAllowed) {
                    dialog_Check = new MaterialDialog.Builder(context)
                            .title(currentCheck.getCompanyName() + " Check Photo")
                            .titleColor(context.getColor(R.color.orange_red))
                            .backgroundColor(context.getColor(R.color.black))
                            .customView(R.layout.dialog_company, true)
                            .positiveText("CLOSE")
                            .canceledOnTouchOutside(false)
                            .autoDismiss(false)
                            .onPositive((dialog, which) -> dialog_Check.dismiss())
                            .positiveColor(context.getColor(R.color.gray))
                            .show();
                }
                else {
                    dialog_Check = new MaterialDialog.Builder(context)
                            .title(currentCheck.getCompanyName() + " Check Photo")
                            .titleColor(context.getColor(R.color.orange_red))
                            .backgroundColor(context.getColor(R.color.black))
                            .contentColor(context.getColor(R.color.bluish))
                            .customView(R.layout.dialog_company, true)
                            .positiveText("CLOSE")
                            .neutralText("RETAKE")
                            .negativeText("DELETE")
                            .canceledOnTouchOutside(false)
                            .autoDismiss(false)
                            .onPositive((dialog, which) -> dialog_Check.dismiss())
                            .onNeutral((dialog, which) -> {
                                if (currentCheck.getCheckPath() != null)
                                    deleteCheckDatabase(currentCheck, v, false);
                                dialog_Check.dismiss();
                                ((CustomerInfo) context).launchCamera(currentCheck, true);
                            })
                            .onNegative((dialog, which) -> {
                                if (currentCheck.getCheckPath() != null && currentCheckPath != null) {
                                    progressDialog = Helper.showLoading(progressDialog, context, true);
                                    deleteCheckDatabase(currentCheck, v, true);
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


                check_image = (ImageView) dialog_Check.findViewById(R.id.check_image);
                check_image_text = (TextView) dialog_Check.findViewById(R.id.check_image_text);

                check_image.setOnClickListener(v1 -> {
                    if(currentCheckPath!=null && currentCheck.getCheckPath()!=null) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(currentCheckPath), "image/*");
                        context.startActivity(intent);
                    }
                });

                if(currentCheck.getCheckPath()==null) {
                    check_image.setImageDrawable(context.getDrawable(R.drawable.icon_empty_check));
                    check_image_text.setVisibility(View.VISIBLE);
                }
                else {
                    progressDialog = Helper.showLoading(progressDialog, context, true);
                    downloadFromDatabase(currentCheck, check_image, v);
                    check_image_text.setVisibility(View.GONE);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Helper.showLoading(progressDialog, context, false);
                        }
                    }, 500);
                }
            }
        });

        holder.view.setOnLongClickListener(v -> {
            if(!isEditAllowed) {
                Helper.deniedDialog(context);
            }
            else {
                String changeCheckStatus = "Fake";

                if (currentCheck.isFakeCheck())
                    changeCheckStatus = "Real";

                edit_Check_Dialog = new MaterialDialog.Builder(context)
                        .title("Update " + currentCheck.getCheckName())
                        .titleColor(context.getColor(R.color.orange_red))
                        .backgroundColor(context.getColor(R.color.black))
                        .customView(R.layout.dialog_edit_check, true)
                        .positiveText("CONFIRM")
                        .negativeText(changeCheckStatus)
                        .neutralText("DELETE")
                        .canceledOnTouchOutside(true)
                        .autoDismiss(false)
                        .onPositive((dialog, which) -> {
                            String check_name = check_New_Name.getText().toString();
                            String check_amount = check_New_Amount.getText().toString();
                            if (check_name.equals(""))
                                check_New_Name.setError("Empty");
                            else if (check_amount.equals("")) {
                                check_New_Amount.setError("Empty");
                            } else if (check_amount.length() > 30)
                                check_New_Name.setError("max length is 30");
                            else if(!check_name.matches("^[a-z A-Z'-]*$"))
                                check_New_Name.setError("Only letters");
                            else {
                                double check_amount_int = Double.parseDouble(check_amount);
                                collectionReference.document(currentCheck.getCheckName() + "_" + currentCheck.getCheckID()).delete();
                                currentCheck.setCheckName(check_name);
                                currentCheck.setCheckAmount(check_amount_int);
                                collectionReference.document(check_name+ "_" + currentCheck.getCheckID()).set(currentCheck);
                                dialog.dismiss();
                            }
                        })
                        .onNegative((dialog, which) -> {
                            boolean checkStatus;
                            checkStatus = !currentCheck.isFakeCheck();
                            collectionReference.document(currentCheck.getCheckName()+ "_" + currentCheck.getCheckID())
                                    .update("fakeCheck", checkStatus);
                            Helper.showUserMessage(v, currentCheck.getCompanyName() + " Updated", Snackbar.LENGTH_LONG, false);
                            dialog.dismiss();
                            notifyDataSetChanged();
                        })
                        .onNeutral((dialog, which) -> {
                            collectionReference.document(currentCheck.getCheckName()+ "_" + currentCheck.getCheckID()).delete();
                            notifyDataSetChanged();
                            Helper.showUserMessage(v, currentCheck.getCheckName() + " Deleted", Snackbar.LENGTH_LONG, false);
                            dialog.dismiss();
                        })
                        .neutralColor(context.getColor(R.color.red))
                        .positiveColor(context.getColor(R.color.green))
                        .negativeColor(context.getColor(R.color.gold))
                        .show();

                initializeDialog();
                check_New_Name.setText(currentCheck.getCheckName());
                check_New_Amount.setText(String.valueOf(currentCheck.getCheckAmount()));
            }

            return false;
        });
    }

    private void initializeDialog(){
        check_New_Name = (EditText) edit_Check_Dialog.findViewById(R.id.check_new_name);
        check_New_Amount = (EditText) edit_Check_Dialog.findViewById(R.id.check_new_amount);
    }

    @Override
    public int getItemCount() {
        return checks.size();
    }

    private void deleteCheckDatabase(final Check check, final View v, final boolean update_Image) {
        storage = FirebaseStorage.getInstance();
        // Create a reference to the file to delete
        storageRef = storage.getReference(check.getCheckPath());

        // Delete the file
        storageRef.delete().addOnSuccessListener(aVoid -> {
            updateCheckDatabase(null, check);
            if(update_Image){
                check_image.setImageDrawable(context.getDrawable(R.drawable.icon_empty_check));
                check_image_text.setVisibility(View.VISIBLE);
            }
            Helper.showUserMessage(v, "Check Picture Deleted", Snackbar.LENGTH_LONG, false);
        }).addOnFailureListener(exception -> {
            try{
                Helper.showUserMessage(v, "Error Deleting Picture", Snackbar.LENGTH_LONG, false);
            } catch (Exception e) {
                currentCheckPath = null;
                e.printStackTrace();
            }
        });
    }

    private void updateCheckDatabase(final String path, Check check) {
        collectionReference.document(check.getCheckName()+ "_" + check.getCheckID())
                .update("checkPath", path)
                .addOnCompleteListener(task -> currentCheckPath = path);
    }

    private void downloadFromDatabase(Check check, final ImageView customer_image, final View view){
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference(check.getCheckPath());

        final long ONE_MEGABYTE = 1024 * 1024;
        storageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes , 0, bytes.length);
            currentCheckPath = Camera.saveImage(context, bitmap, true);
            customer_image.setImageBitmap(bitmap);
        }).addOnFailureListener(exception -> {
            check_image.setImageDrawable(context.getDrawable(R.drawable.icon_empty_check));
            check_image_text.setVisibility(View.VISIBLE);
            check_image_text.setText("Error Downloading\nFile might have been deleted");
        });
    }

}

