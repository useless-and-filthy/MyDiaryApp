package com.akigon.mydiary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import com.parse.ParseUser;

import static com.akigon.mydiary.BFunctions.SP_PASS_CODE_KEY;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
//      SharedPreferences sharedPreferences = getSharedPreferences(FirebaseAuth.getInstance().getCurrentUser().getUid(), Context.MODE_PRIVATE);
//        if (sharedPreferences.getBoolean(SP_DARK_MODE_KEY, false)) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//      }
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            // do stuff with the user
            SharedPreferences prefs = getSharedPreferences(ParseUser.getCurrentUser().getObjectId(), Context.MODE_PRIVATE);
            String s = prefs.getString(SP_PASS_CODE_KEY, "").trim();
            if (s.isEmpty()) {
                Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Intent intent = new Intent(SplashScreen.this, PasscodeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } else {
            // show the signup or login screen
            Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        //startHeavyProcessing();
    }

//    private void startHeavyProcessing() {
//        new LongOperation().execute("");
//    }
//
//    private class LongOperation extends AsyncTask<String, Void, String> {
//
//        @Override
//        protected String doInBackground(String... params) {
//            //some heavy processing resulting in a Data String
//            for (int i = 0; i < 2; i++) {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    Thread.interrupted();
//                }
//            }
//            return "whatever result you have";
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//
//        }
//
//        @Override
//        protected void onPreExecute() {
//
//        }
//
//        @Override
//        protected void onProgressUpdate(Void... values) {
//        }
//    }
}