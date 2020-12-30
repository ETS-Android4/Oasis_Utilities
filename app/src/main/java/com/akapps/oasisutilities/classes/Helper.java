package com.akapps.oasisutilities.classes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.akapps.oasisutilities.R;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.time.Month;
import static android.content.Context.MODE_PRIVATE;

public class Helper {

    /**
     * Determines current orientation of device
     *
     * @param context   The application context.
     * @return true if phone is in landscape
     */
    public static boolean getOrientation(Context context){
        int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Sets orientation of device
     *
     * @param activity The current activity.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    public static void setOrientation(Activity activity, String desiredOrientation){
        if(desiredOrientation.equals("Portrait"))
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else  if(desiredOrientation.equals("Landscape"))
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    /**
     * Returns if device is a tablet
     *
     * @param context The application context.
     */
    public static boolean isTablet(Context context){
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Shows the user a message
     * @param view   The current view
     */
    public static void showUserMessage(View view, String message, int duration, boolean action){
        if(!action) {
            Snackbar.make(view, message, duration)
                    .setAction("Action", null).show();
        }
        else{
            final Snackbar snackBar = Snackbar.make(view, message, duration);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Call your action method here
                    snackBar.dismiss();
                }
            });
            snackBar.show();
        }
    }

    // returns the string equivalent of a month number
    public static String getMonthString(int month){
        return Month.of(month).name().substring(0,1).toUpperCase() +
                Month.of(month).name().substring(1).toLowerCase();
    }

    // capitalized a word or all words in a sentence
    public static String toUpperCase(String word){
        String[] words = word.split(" ");
        String allWords = "";
        for(int i=0; i<words.length; i++)
            allWords+= words[i].substring(0,1).toUpperCase() + words[i].substring(1) + " ";

        return allWords.trim();
    }

    // hides keyboard
    public static void hideSoftKeyboard(Activity activity) {
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    // converts date to a string
    public static String intDateToString(String date){
        return date.substring(0,2) + "-" + date.substring(2,4) + "-" + date.substring(4);
    }

    // saves a small piece of data
    public static void savePreference(String key, String data, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, data);
        editor.apply();
    }

    // retrieved data saved
    public static String getPreference(String key, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app", MODE_PRIVATE);
        String data = sharedPreferences.getString(key, null);
        return data;
    }

    // returns a path directory where the pdf will save
    public static String getAppPath(Context context){
        File storageDir = new File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        + "/" + context.getString(R.string.app_name).replace(" ", "_"));

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return storageDir.getPath() + File.separator;
    }

    // shows a loading dialog
    public static Dialog showLoading(Dialog progressDialog, Context context, boolean show){
        try {
            if (show) {
                progressDialog = new Dialog(context);
                progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                progressDialog.setContentView(R.layout.custom_dialog_progress);

                /* Custom setting to change TextView text,Color and Text Size according to your Preference*/

                TextView progressTv = progressDialog.findViewById(R.id.progress_tv);
                progressTv.setText(context.getResources().getString(R.string.loading));
                progressTv.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                progressTv.setTextSize(19F);
                if (progressDialog.getWindow() != null)
                    progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                progressDialog.setCancelable(false);
                progressDialog.show();
            } else
                progressDialog.cancel();
        }catch (Exception e){ }
        return progressDialog;
    }

    // shows a dialog message
    public static void deniedDialog(Context context){
        new MaterialDialog.Builder(context)
                .title("No No No!")
                .titleColor(context.getColor(R.color.orange_red))
                .backgroundColor(context.getColor(R.color.black))
                .content("You do no have permission to edit another store's data...")
                .contentGravity(GravityEnum.CENTER)
                .positiveText("CLOSE")
                .canceledOnTouchOutside(false)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .positiveColor(context.getColor(R.color.light_blue))
                .contentColor(context.getColor(R.color.red))
                .show();
    }
}
