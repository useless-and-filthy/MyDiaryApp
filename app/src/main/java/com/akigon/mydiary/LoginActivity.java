package com.akigon.mydiary;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText email;
    private TextInputEditText password;
    private Button login;
    private TextView navigatesignup;
    private ProgressDialog progressDialog;
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {

        }else{

        }


        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setCancelable(false);
        prefs = getSharedPreferences("meta_data", Context.MODE_PRIVATE);
        email = findViewById(R.id.username);
        password = findViewById(R.id.password);
        login = findViewById(R.id.login);
        navigatesignup = findViewById(R.id.navigatesignup);

        login.setOnClickListener(v -> performLogin(email.getText().toString(), password.getText().toString()));

        navigatesignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        findViewById(R.id.textForgotPassword).setOnClickListener(v -> {
            if (!Objects.requireNonNull(email.getText()).toString().isEmpty()) {
                progressDialog.show();

                ParseUser.requestPasswordResetInBackground(email.getText().toString(), new RequestPasswordResetCallback() {
                    public void done(ParseException e) {
                        progressDialog.dismiss();
                        if (e == null) {
                            // An email was successfully sent with reset instructions.
                            showToast("Reset Email Sent");
                        } else {
                            // Something went wrong. Look at the ParseException to see what's up.
                            showToast(e.getMessage());
                        }
                    }
                });
            } else {
                showToast("Fill email address and then click Forgot password");
            }
        });


        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i)) && !(source.charAt(i) == '@') && !(source.charAt(i) == '#') && !(source.charAt(i) == '_')) {
                        return "";
                    }
                }
                return null;
            }
        };
        password.setFilters(new InputFilter[]{filter});
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void performLogin(String username, String password) {
        if (!username.trim().isEmpty() && !password.trim().isEmpty()) {
            progressDialog.show();

            ParseUser.logInInBackground(username, password, new LogInCallback() {
                public void done(ParseUser user, ParseException e) {
                    progressDialog.dismiss();
                    if (e == null) {
                        // Hooray! The user is logged in.
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        // Signup failed. Look at the ParseException to see what happened.
                        showAlert("Error", e.getMessage());
                    }
                }
            });
        } else {
            showToast("Invalid credentials");
        }
    }


    private void showAlert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog ok = builder.create();
        ok.show();
    }
}

