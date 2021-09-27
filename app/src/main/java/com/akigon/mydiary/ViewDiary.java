package com.akigon.mydiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.akigon.mydiary.db.AppDatabase;
import com.akigon.mydiary.db.Diary;
import com.bumptech.glide.Glide;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import static com.akigon.mydiary.BFunctions.DIARY_CATEGORIES;
import static com.akigon.mydiary.BFunctions.DIARY_CLASS_NAME;
import static com.akigon.mydiary.BFunctions.DIARY_CONTENT;
import static com.akigon.mydiary.BFunctions.DIARY_CREATED_AT;
import static com.akigon.mydiary.BFunctions.DIARY_TITLE;
import static com.akigon.mydiary.BFunctions.DIARY_UPDATED_AT;
import static com.akigon.mydiary.BFunctions.EDIT_NOTE;
import static com.akigon.mydiary.BFunctions.SP_AUTO_SYNC_KEY;
import static com.akigon.mydiary.BFunctions.getStringFromList;
import static com.akigon.mydiary.BFunctions.getTimeStringFromDate;
import static com.akigon.mydiary.BFunctions.getTimeStringTS;
import static com.akigon.mydiary.BFunctions.minToRead;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ViewDiary extends AppCompatActivity {

    private static final int EDIT_NOTE = 450;
    private RichEditor webview;
    private TextView titleTV, createdOnTV;
    private ProgressDialog progressDialog;
    private TextView updatedTV, tagsTV;
    private Diary diary;
    private SharedPreferences sharedPreferences;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_diary);
        progressDialog = new ProgressDialog(this);
        db=AppDatabase.getInstance(this);
        webview = findViewById(R.id.mywebview);
        webview.setBackgroundColor(Color.TRANSPARENT);
        titleTV = findViewById(R.id.titleText);
        createdOnTV = findViewById(R.id.createdOnText);
        updatedTV = findViewById(R.id.updatedAtText);
        tagsTV = findViewById(R.id.tagsText);
        webview.setEditorFontSize(18);
        webview.editOff();
        sharedPreferences = getSharedPreferences(ParseUser.getCurrentUser().getObjectId(), Context.MODE_PRIVATE);
        nightMode();

        int crAt = getIntent().getIntExtra("id", -1);
        if(crAt!=-1){
            diary = db.diaryDao().getDiary(crAt);
            displayNote();
        }else{
            showAlert("Error", "Diary not found");
        }

    }

    private static final String TAG = "ViewDiary:";

    private void nightMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                Log.d(TAG, "nightMode: yes");
                webview.nightModeOn();
                break;

            case Configuration.UI_MODE_NIGHT_NO:
                Log.d(TAG, "nightMode: no");
                break;

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                Log.d(TAG, "nightMode: undefined");
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.edit_note:
                Intent intent = new Intent(ViewDiary.this, AddEditDiary.class);
                intent.putExtra("id", diary.getId());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void displayNote() {
        titleTV.setText(diary.getTitle());
        tagsTV.setText(diary.getCategories());
        webview.setHtml(diary.getContent());
        createdOnTV.setText(getTimeStringTS(diary.getCreatedAt()) + "\t • \t" + minToRead(webview.getHtml()) + " min");
        updatedTV.setText("Updated: " + getTimeStringTS(diary.getUpdatedAt()));
        setUserData();
    }

    private void showAlert(String title, String message) {
        progressDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog ok = builder.create();
        ok.show();
    }

    private void setUserData() {
        ParseUser user_temp=ParseUser.getCurrentUser();
        user_temp.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if(e==null){
                    ((TextView) findViewById(R.id.usernameText)).setText(object.getString("username"));
                    String url="";
                    if (sharedPreferences.getBoolean(SP_AUTO_SYNC_KEY,false)){
                        ParseFile pfp=object.getParseFile("pfp_file");
                        url=pfp.getUrl();
                    }else{
                        url=object.getString("pfp_loc");
                    }

                    Glide.with(ViewDiary.this).load(url).centerCrop().circleCrop().placeholder(R.mipmap.boy).into((ImageView) findViewById(R.id.userPfp));
                }else{
                    showAlert("Error",e.getMessage());
                }
            }
        });
    }


}