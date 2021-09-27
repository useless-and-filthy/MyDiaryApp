package com.akigon.mydiary;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.parse.ParseUser;

import static com.akigon.mydiary.BFunctions.SP_PASS_CODE_KEY;
import static com.akigon.mydiary.BFunctions.SP_SECURITY_QUESTION_KEY;

public class PasscodeActivity extends AppCompatActivity {

    private TextView codeTV;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passcode);
        codeTV = findViewById(R.id.p_code);
        prefs = getSharedPreferences(ParseUser.getCurrentUser().getObjectId(), Context.MODE_PRIVATE);
        String pass = prefs.getString(SP_PASS_CODE_KEY, "");

        codeTV.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (codeTV.getText().toString().equals(pass)) {
                    sendToMainActivity();
                }else if (s.length()==4){
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(100);
                    }
                }
            }
        });
    }

    public void insertInt(View view) {
        String n = ((TextView) view).getText().toString();
        codeTV.setText(codeTV.getText().toString() + n);
    }

    public void forgotCode(View view1) {
        AlertDialog.Builder builder = new AlertDialog.Builder(PasscodeActivity.this);
        View view = LayoutInflater.from(PasscodeActivity.this).inflate(R.layout.edit_view, null);
        TextInputLayout editText = view.findViewById(R.id.TIlayout);
        editText.setVisibility(View.GONE);
        TextInputEditText hintText = view.findViewById(R.id.hintET);

        String sq = prefs.getString(SP_SECURITY_QUESTION_KEY, "");

        builder.setTitle("Security Question")
                .setView(view)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(sq.equals(hintText.getText().toString())){
                            sendToMainActivity();
                        }else {
                            Toast.makeText(PasscodeActivity.this,"Wrong Answer",Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    public void removeInt(View view) {
        String str = codeTV.getText().toString();
        if (str.length() > 0) {
            str = str.substring(0, str.length() - 1);
        }
        codeTV.setText(str);
    }

    public void clearInt(View view) {
        codeTV.setText("");
    }

    private void sendToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}