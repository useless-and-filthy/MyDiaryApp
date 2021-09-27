package com.akigon.mydiary;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.parse.GetCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import static com.akigon.mydiary.BFunctions.ACTION_AUTO_SYNC;
import static com.akigon.mydiary.BFunctions.CHOOSE_CSV;
import static com.akigon.mydiary.BFunctions.PICK_IMAGE;
import static com.akigon.mydiary.BFunctions.SP_AUTO_SYNC_KEY;
import static com.akigon.mydiary.BFunctions.SP_PASS_CODE_KEY;
import static com.akigon.mydiary.BFunctions.SP_SECURITY_QUESTION_KEY;
import static com.akigon.mydiary.BFunctions.feedback_url;
import static com.akigon.mydiary.BFunctions.open_source_license_url;
import static com.akigon.mydiary.BFunctions.privacy_policy_url;
import static com.akigon.mydiary.BFunctions.savefile;
import static com.akigon.mydiary.BFunctions.terms_condition_url;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "LogoutActivity";


    private Button logout;
    private ProgressDialog progressDialog;
    private TextInputEditText userName;
    private ImageView imageView;
    private TextView passcodeET;
    private LinearLayout passcodeLL;
    private SwitchCompat autoSyncSwitch;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(ParseUser.getCurrentUser().getObjectId(), Context.MODE_PRIVATE);

        userName = findViewById(R.id.username);
        userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptUserName();
            }
        });

        imageView = findViewById(R.id.profilePic);
        passcodeET = findViewById(R.id.passcode_ET);
        passcodeLL = findViewById(R.id.passcode_LL);
        autoSyncSwitch = findViewById(R.id.auto_sync_switch);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");
                Intent chooserIntent = Intent.createChooser(pickIntent, "Select Image");
                startActivityForResult(chooserIntent, PICK_IMAGE);
            }
        });

        logout = findViewById(R.id.logout);
        progressDialog = new ProgressDialog(SettingsActivity.this);

        autoSyncSwitch.setChecked(sharedPreferences.getBoolean(SP_AUTO_SYNC_KEY, false));

        if (sharedPreferences.getBoolean(SP_AUTO_SYNC_KEY, false)) {
            findViewById(R.id.web_link).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.web_link).setVisibility(View.GONE);
        }


        logout.setOnClickListener(v -> {
            ParseUser.logOutInBackground(new LogOutCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        showAlert("Error", e.getMessage());
                    }
                }
            });

        });

        findViewById(R.id.restoreTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] options = {"Internal Storage", "Cloud"};
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Restore Options")
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                                    chooseFile.setType("*/*");
                                    chooseFile = Intent.createChooser(chooseFile, "Choose CSV file");
                                    startActivityForResult(chooseFile, CHOOSE_CSV);
                                } else {
                                    saveFromCloud();
                                }
                            }
                        });

                AlertDialog ok = builder.create();
                ok.show();
            }
        });

        findViewById(R.id.backupTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] options = {"Internal Storage", "Cloud"};
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Save Diary to")
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    backUpToInternalStorage();
                                } else {
                                    //backUpToCloud();
                                }
                            }
                        });

                AlertDialog ok = builder.create();
                ok.show();
            }
        });

        passcodeLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                View view = LayoutInflater.from(SettingsActivity.this).inflate(R.layout.edit_view, null);
                TextInputEditText editText = view.findViewById(R.id.inputET);
                TextInputEditText hintText = view.findViewById(R.id.hintET);

                builder.setTitle("Set Password")
                        .setView(view)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                updatePassword(editText.getText().toString(), hintText.getText().toString());
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
        });

        autoSyncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //it tells what state it has become to
                sharedPreferences.edit().putBoolean(SP_AUTO_SYNC_KEY, isChecked).apply();

                if (isChecked) {
                    findViewById(R.id.web_link).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.web_link).setVisibility(View.GONE);
                }

            }
        });


//        darkModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                sharedPreferences.edit().putBoolean(SP_DARK_MODE_KEY, isChecked).apply();
//
//                if (isChecked) {
//                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//                } else {
//                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//                }
//
//            }
//        });


//        findViewById(R.id.loadSamplesTV).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                progressDialog.show();
//                try {
//                    InputStream is = getAssets().open("samples.csv");
//                    InputStreamReader reader = new InputStreamReader(is, Charset.forName("UTF-8"));
//                    CSVReader csvReader = new CSVReader(reader);
//                    String[] nextRecord;
//
//                    // we are going to read data line by line
//                    int p = 0;
//                    long curT = System.currentTimeMillis();
//                    while ((nextRecord = csvReader.readNext()) != null) {
//                        Diary diary = new Diary(nextRecord[0], curT, Long.parseLong(nextRecord[2]), nextRecord[3], nextRecord[4], nextRecord[5], nextRecord[6], Boolean.parseBoolean(nextRecord[7]));
//                        noteDatabase.diaryDao().insert(diary);
//                        p++;
//                    }
//                    intent.putExtra(ACTION_RESTORED, ACTION_RESTORED);
//                    setResult(RESULT_OK, intent);
//                    showAlert("Restore Completed", "Total " + p + " entries added");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    showAlert("Restore Completed", "Exception: " + e.getMessage());
//                }
//            }
//        });

        findViewById(R.id.sendFeedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedback_url));
                startActivity(browserIntent);
            }
        });

        findViewById(R.id.open_source_licence).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(open_source_license_url));
                startActivity(browserIntent);
            }
        });
        findViewById(R.id.termsncondition).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(terms_condition_url));
                startActivity(browserIntent);
            }
        });
        findViewById(R.id.privacy_policy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacy_policy_url));
                startActivity(browserIntent);
            }
        });


        setUserData();
        updatePassText();
    }

    private void updatePassText() {
        String s = sharedPreferences.getString(SP_PASS_CODE_KEY, "");
        passcodeET.setText(s);
    }

    private void updatePassword(String s, String s1) {
        if (sharedPreferences.edit().putString(SP_PASS_CODE_KEY, s).commit() && sharedPreferences.edit().putString(SP_SECURITY_QUESTION_KEY, s1).commit()) {
            showAlert("Security Alert", "Passcode saved successfully");
            updatePassText();
        } else {
            showAlert("Security Alert", "Error saving pass code");
        }
    }


    private void setUserData() {
        ParseUser user=ParseUser.getCurrentUser();
        user.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if(e==null){
                    ((TextView) findViewById(R.id.username)).setText(object.getString("username"));
                    String url="";
                    if (sharedPreferences.getBoolean(SP_AUTO_SYNC_KEY,false)){
                        ParseFile pfp=object.getParseFile("pfp_file");
                        url=pfp.getUrl();
                    }else{
                        url=object.getString("pfp_loc");
                    }

                    Glide.with(SettingsActivity.this).load(url).centerCrop().circleCrop().placeholder(R.mipmap.boy).into(imageView);
                }else{
                    showToast(e.getMessage());
                }
            }
        });

//        if (map != null && map.containsKey("PHOTO_URL")) {
//
//        }
    }

//    private void backUpToCloud() {
//        progressDialog.show();
//        List<Diary> diaries = noteDatabase.diaryDao().getAll();
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        // Get a new write batch
//        WriteBatch batch = db.batch();
//        CollectionReference userData = db.collection("USERS").document(FirebaseAuth.getInstance().getUid()).collection("JOURNALS");
//        for (Diary xx : diaries) {
//            batch.set(userData.document(xx.getObjectId()), xx, SetOptions.merge());
//        }
//        // Commit the batch
//        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                if (task.isSuccessful()) {
//                    showAlert("Backup Complete", "Total " + diaries.size() + " entries uploaded");
//                } else {
//                    showAlert("Backup Complete", "Total " + diaries.size() + " entries uploaded");
//                }
//            }
//        });
//    }

    private void promptUserName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        EditText editText = new EditText(this);
        builder.setTitle("Set Username")
                .setView(editText)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userName.setText(editText.getText().toString());
                        updateUsername();
                    }
                });
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            progressDialog.show();

            String pfp_path=ImageFilePath.getPath(SettingsActivity.this,data.getData());
            String dest_path= getExternalFilesDir(null)+ "/" + UUID.randomUUID().toString()+ pfp_path.substring(pfp_path.lastIndexOf("."));
            savefile(pfp_path,dest_path);

            if (sharedPreferences.getBoolean(SP_AUTO_SYNC_KEY,false)){
                //upload parse file
                //set url to user url
                ParseFile parseFile=new ParseFile(new File(dest_path));
                parseFile.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e==null){
                            Log.d(TAG, "done: parse file upload");
                            ParseUser user = ParseUser.getCurrentUser();
                            user.put("pfp_file", parseFile);
                            user.saveEventually(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    progressDialog.dismiss();
                                    if (e == null) {
                                        Glide.with(SettingsActivity.this).load(pfp_path).centerCrop().circleCrop().placeholder(R.mipmap.boy).into(imageView);
                                    } else {
                                        showToast(e.getMessage());
                                        Log.d(TAG, "done: "+e.getMessage());
                                    }

                                }
                            });
                        }else{
                            Log.d(TAG, e.getMessage());
                        }
                    }
                });
            }else{
                //no need to upload
                ParseUser user = ParseUser.getCurrentUser();
                user.put("pfp_loc", dest_path);
                user.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        progressDialog.dismiss();
                        if (e == null) {
                            Glide.with(SettingsActivity.this).load(dest_path).centerCrop().circleCrop().placeholder(R.mipmap.boy).into(imageView);
                        } else {
                            showToast(e.getMessage());
                            Log.d(TAG, "done: "+e.getMessage());
                        }

                    }
                });
            }


        } else if (requestCode == CHOOSE_CSV && resultCode == RESULT_OK && data.getData() != null) {
            restoreFromStorage(data.getData());
        }
    }




    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private void restoreFromStorage(Uri csvFile) {
        progressDialog.show();
//        try {
//            InputStream is = getContentResolver().openInputStream(csvFile);
//            InputStreamReader reader = new InputStreamReader(is, Charset.forName("UTF-8"));
//            CSVReader csvReader = new CSVReader(reader);
//            String[] nextRecord;
//
//            // we are going to read data line by line
//            int p = 0;
//            long curT = System.currentTimeMillis();
//            while ((nextRecord = csvReader.readNext()) != null) {
//                Diary diary = new Diary(nextRecord[0], curT, Long.parseLong(nextRecord[2]), nextRecord[3], nextRecord[4], nextRecord[5], nextRecord[6], Boolean.parseBoolean(nextRecord[7]));
//                noteDatabase.diaryDao().insert(diary);
//                p++;
//            }
//            intent.putExtra(ACTION_RESTORED, ACTION_RESTORED);
//            setResult(RESULT_OK, intent);
//            showAlert("Restore Completed", "Total " + p + " entries added");
//        } catch (Exception e) {
//            e.printStackTrace();
//            showAlert("Restore Completed", "Exception: " + e.getMessage());
//        }
    }

    private void updateUsername() {
        progressDialog.show();
        ParseUser user = ParseUser.getCurrentUser();
        user.setUsername(userName.getText().toString());
        user.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                progressDialog.dismiss();
                if(e==null){
                    showToast("Username updated");
                }else{
                    showToast(e.getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        progressDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this)
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

    private void saveFromCloud() {
        progressDialog.show();
    }

    private void backUpToInternalStorage() {
        progressDialog.show();
//        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/mydiary.csv");
//        if (file.exists()) {
//            file.delete();
//        }
//        try {
//            boolean isCreated = file.createNewFile();
//            if (!isCreated) {
//                showAlert("Backup Alert", "Error: Could not create a new file");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            showAlert("Backup Alert", e.getMessage());
//        }
//        try {
//            List<String[]> diaryList = new ArrayList<>();
//            for (Diary xx : noteDatabase.diaryDao().getAll()) {
//                diaryList.add(getDiaryArray(xx));
//            }
//            // create FileWriter object with file as parameter
//            FileWriter outputfile = new FileWriter(file);
//            // create CSVWriter object filewriter object as parameter
//            CSVWriter writer = new CSVWriter(outputfile);
//            // create a List which contains String array
//            writer.writeAll(diaryList);
//            // closing writer connection
//            writer.close();
//            showAlert("Backup Completed", "Total " + diaryList.size() + " entries saved in mydiary.csv file in Download Folder");
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            showAlert("Backup Alert", e.getMessage());
//        }
    }


    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

}
