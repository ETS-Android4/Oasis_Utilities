package com.akapps.oasisutilities.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;

import com.akapps.oasisutilities.classes.Helper;
import com.akapps.oasisutilities.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.List;

import nl.bryanderidder.themedtogglebuttongroup.ThemedButton;
import nl.bryanderidder.themedtogglebuttongroup.ThemedToggleButtonGroup;

public class Login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String lastLogin;
    private boolean loginAgain;

    // layout
    private EditText userPassword;
    private MaterialButton loginBtn;
    private Dialog progressDialog;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context = this;
        setTitle("Login");

        mAuth = FirebaseAuth.getInstance();

        lastLogin = Helper.getPreference("login", context);
        Calendar now;

        if(lastLogin!=null) {
            loginAgain = minsAgo(lastLogin);
            if(!loginAgain)
                autoLogin();
            else{
                Helper.showUserMessage(findViewById(android.R.id.content), "Last login more than 2 hours ago",
                        Snackbar.LENGTH_LONG, false);
            }
        }

        userPassword = findViewById(R.id.login_password);
        loginBtn = findViewById(R.id.login_btn);

        loginBtn.setOnClickListener(v -> {
            String password = userPassword.getText().toString();
            final String selectedStore = getSelectedStore();

            if(selectedStore!=null && !password.isEmpty()) {
                progressDialog = Helper.showLoading(progressDialog, context, true);
                mAuth.signInWithEmailAndPassword(selectedStore, password)
                        .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Helper.savePreference("login", String.valueOf(Calendar.getInstance().getTimeInMillis()), context);
                                            loginToApp(selectedStore);
                                        }
                                    }, 1000);
                                }
                                else
                                    userPassword.setError("Wrong password");

                                Helper.showLoading(progressDialog, context, false);
                            }
                        });
            }
            else if(password.length()==0)
                userPassword.setError("Empty");
            else
                userPassword.setError("Select Store");
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(progressDialog!=null)
            progressDialog.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        userPassword.setText("");
    }

    @Override
    public void onBackPressed() {
        // catches on back press so nothing happens
    }

    private void autoLogin(){
        String[] stores = getResources().getStringArray(R.array.stores);
        String currentStore = null;
        if(FirebaseAuth.getInstance().getCurrentUser()!=null) {
            for (int i = 0; i < stores.length; i++) {
                currentStore = FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0].replace("_", " ");
                if (stores[i].toLowerCase().equals(currentStore)) {
                    Helper.showUserMessage(findViewById(android.R.id.content), "Logging in " + stores[i] + "...",
                            Snackbar.LENGTH_LONG, false);
                    final String finalCurrentStore = currentStore;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loginToApp(finalCurrentStore);
                        }
                    }, 1000);
                }
            }
        }
    }

    private boolean minsAgo(String datetime) {
        Calendar now = Calendar.getInstance(); // Get time now
        long differenceInMillis = now.getTimeInMillis() - Long.valueOf(datetime);
        long differenceInMins = (differenceInMillis) / 1000L / 60L; 
        return (int)differenceInMins > 120;
    }

    private void loginToApp(String currentStore){
        Helper.showLoading(progressDialog, context, false);
        Intent home = new Intent(Login.this, Main.class);
        home.putExtra("store", currentStore);
        startActivity(home);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public String getSelectedStore(){
        String storeSelected="";
        ThemedToggleButtonGroup themedButtonGroup = findViewById(R.id.storeBtns);
        // get the selected buttons:
        List<ThemedButton> selectedButtons = themedButtonGroup.getButtons();

        for(int i=0; i<selectedButtons.size(); i++){
            if(selectedButtons.get(i).isSelected())
                if(i==0)
                    storeSelected = "lander";
                else if(i==1)
                    storeSelected = "crows_landing";
                else if(i==2)
                    storeSelected = "manteca";

        }

        if(storeSelected.isEmpty())
            return null;
        return storeSelected +"@oasis.com";
    }
}