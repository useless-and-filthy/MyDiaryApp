package com.akigon.mydiary;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.FragmentTransaction;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.String;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.akigon.mydiary.db.AppDatabase;
import com.akigon.mydiary.db.Diary;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;

import org.apache.commons.text.WordUtils;
import org.xml.sax.XMLReader;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.akigon.mydiary.BFunctions.DIARY_ARCHIVE;
import static com.akigon.mydiary.BFunctions.DIARY_CATEGORIES;
import static com.akigon.mydiary.BFunctions.DIARY_CLASS_NAME;
import static com.akigon.mydiary.BFunctions.DIARY_CONTENT;
import static com.akigon.mydiary.BFunctions.DIARY_CREATED_AT;
import static com.akigon.mydiary.BFunctions.DIARY_FILES;
import static com.akigon.mydiary.BFunctions.DIARY_HTML;
import static com.akigon.mydiary.BFunctions.DIARY_TITLE;
import static com.akigon.mydiary.BFunctions.DIARY_UPDATED_AT;
import static com.akigon.mydiary.BFunctions.DIARY_USER;
import static com.akigon.mydiary.BFunctions.PICK_AUDIO;
import static com.akigon.mydiary.BFunctions.PICK_IMAGE;
import static com.akigon.mydiary.BFunctions.PICK_VIDEO;
import static com.akigon.mydiary.BFunctions.getStringFromList;
import static com.akigon.mydiary.BFunctions.savefile;
import static com.akigon.mydiary.BFunctions.showAlert;


public class AddEditDiary extends AppCompatActivity implements HTMLFragment.DialogListener{

    private RichEditor mEditor;
    private EditText titleET;
    private AppCompatEditText tagsET;
    private static final String TAG = "AddEditDiary";
    private Diary diary;
    private String fileList = "";
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_diary);

        db=AppDatabase.getInstance(this);

        titleET = findViewById(R.id.diary_title);
        tagsET = findViewById(R.id.diary_tags);
        tagsET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mEditor = (RichEditor) findViewById(R.id.editor);

        mEditor.setEditorFontSize(18);
        mEditor.setBackgroundColor(Color.TRANSPARENT);
        mEditor.setPlaceholder("Insert text here...");

        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i)) && !(source.charAt(i) == ',') && !(source.charAt(i) == ' ')) {
                        return "";
                    }
                }
                return null;
            }
        };
        tagsET.setFilters(new InputFilter[]{filter});
        int crAt = getIntent().getIntExtra("id", -1);
        if (crAt!=-1){
            diary = db.diaryDao().getDiary(crAt);
            fileList=diary.getFilesString();
            displayNote();
        }

        setEditOptionListeners();
        nightMode();
    }


    private void displayNote() {
        mEditor.setHtml(diary.getContent());
        titleET.setText(diary.getTitle());
        tagsET.setText(diary.getCategories());
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE || requestCode == PICK_AUDIO || requestCode == PICK_VIDEO) {
            if (resultCode == RESULT_OK) {
                String main_path = ImageFilePath.getPath(AddEditDiary.this, data.getData());
                String dest_path = getExternalFilesDir(null) + "/" + UUID.randomUUID().toString() + main_path.substring(main_path.lastIndexOf("."));

                savefile(main_path, dest_path);

                if (requestCode == PICK_IMAGE) {
                    mEditor.insertImage("file://"+dest_path);
                } else if (requestCode == PICK_VIDEO) {
                    mEditor.insertVideo("file://"+dest_path);
                } else {
                    mEditor.insertAudio("file://"+dest_path);
                }

                if (!fileList.contains("file://"+dest_path)) {
                    String pp="file://"+dest_path+",";
                    fileList=fileList.concat(pp);
                }
            }
        }
    }


    public static String extractYTId(String ytUrl) {
        String vId = null;
        Pattern pattern = Pattern.compile(
                "^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch?v=)([^#&?]*).*$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ytUrl);
        if (matcher.matches()) {
            vId = matcher.group(1);
        }
        return vId;
    }


    private void saveNote() {
        //title -- content -- categoryList -- filesList -- archive -- created_at -- updatedAt
        if (isValid()) {
            if(diary==null){
                diary=new Diary(System.currentTimeMillis(),System.currentTimeMillis(),null,titleET.getText().toString(),mEditor.getHtml(),tagsET.getText().toString().trim(),false,fileList);
            }else{
                diary.setTitle(titleET.getText().toString());
                diary.setContent(mEditor.getHtml());
                diary.setCategories(getTagsText());
                diary.setFilesString(fileList);
                diary.setUpdatedAt(System.currentTimeMillis());
            }

            db.diaryDao().insert(diary);
            showToast("Saved!");

        } else {
            showAlert(AddEditDiary.this, "Diary Help", getResources().getString(R.string.diary_edit_help));
        }
    }

    private boolean isValid() {
        if (titleET.getText().toString().trim().isEmpty()) {
            return false;
        } else if (mEditor.getHtml().trim().isEmpty()) {
            return false;
        } else {
            return true;
        }
    }


    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void setEditOptionListeners() {
        findViewById(R.id.action_bold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setBold();
            }
        });
        findViewById(R.id.action_italic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setItalic();
            }
        });
        findViewById(R.id.action_underline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setUnderline();
            }
        });
        findViewById(R.id.action_strikethrough).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setStrikeThrough();
            }
        });

        findViewById(R.id.action_heading1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(1);
            }
        });
        findViewById(R.id.action_heading2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(2);
            }
        });
        findViewById(R.id.action_heading3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(3);
            }
        });
        findViewById(R.id.action_heading4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(4);
            }
        });
        findViewById(R.id.action_indent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setIndent();
            }
        });
        findViewById(R.id.action_outdent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setOutdent();
            }
        });
        findViewById(R.id.action_align_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignLeft();
            }
        });
        findViewById(R.id.action_align_center).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignCenter();
            }
        });
        findViewById(R.id.action_align_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignRight();
            }
        });
        findViewById(R.id.action_insert_numbers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setNumbers();
            }
        });
        findViewById(R.id.action_insert_bullets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setBullets();
            }
        });
        findViewById(R.id.action_insert_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AddEditDiary.this);
                View v1 = LayoutInflater.from(AddEditDiary.this).inflate(R.layout.input_link_view, null, false);
                builder.setView(v1);
                builder.setTitle("Enter URL")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String Ltitle = ((TextInputEditText) v1.findViewById(R.id.link_title)).getText().toString().trim();
                                String link = ((TextInputEditText) v1.findViewById(R.id.link)).getText().toString().trim();

                                if (!link.isEmpty()) {
                                    if (Ltitle.isEmpty()) {
                                        mEditor.insertLink(link, link);
                                    } else {
                                        mEditor.insertLink(link, Ltitle);
                                    }
                                } else {
                                    Toast.makeText(AddEditDiary.this, "No Link", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();

            }
        });
        findViewById(R.id.action_insert_checkbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.insertTodo();
            }
        });
        findViewById(R.id.action_insert_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBottomSheet();
            }
        });
    }

    private void nightMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                Log.d(TAG, "nightMode: yes");
                mEditor.nightModeOn();
                break;

            case Configuration.UI_MODE_NIGHT_NO:
                Log.d(TAG, "nightMode: no");
                break;

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                Log.d(TAG, "nightMode: undefined");
                break;
        }
    }


    private void showBottomSheet() {
        View view = getLayoutInflater().inflate(R.layout.bottom_media_options, null);
        final Dialog mBottomSheetDialog = new Dialog(this, R.style.MaterialDialogSheet);
        mBottomSheetDialog.setContentView(view); // your custom view.
        mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);
        mBottomSheetDialog.show();

        view.findViewById(R.id.action_insert_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(AddEditDiary.this);
                EditText editText = new EditText(AddEditDiary.this);
                builder.setView(editText);
                builder.setTitle("Enter Image Url")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEditor.insertImage(editText.getText().toString().trim());
                            }
                        })
                        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton("STORAGE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                pickIntent.setType("image/*");
                                Intent chooserIntent = Intent.createChooser(pickIntent, "Select Image");
                                startActivityForResult(chooserIntent, PICK_IMAGE);
                            }
                        });
                builder.show();
            }
        });

        view.findViewById(R.id.action_insert_youtube).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(AddEditDiary.this);
                EditText editText = new EditText(AddEditDiary.this);
                builder.setView(editText);
                builder.setTitle("Enter Youtube Url")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                String base_url = "https://www.youtube.com/embed/" + extractYTId(editText.getText().toString().trim());
                                mEditor.insertYoutubeVideo(base_url);
                            }
                        })
                        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();

            }
        });

        view.findViewById(R.id.action_insert_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                pickIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 15);
                pickIntent.setType("video/*");
                Intent chooserIntent = Intent.createChooser(pickIntent, "Select Video");
                startActivityForResult(chooserIntent, PICK_VIDEO);
            }
        });

        view.findViewById(R.id.action_insert_audio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                pickIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 15);
                pickIntent.setType("audio/*");
                Intent chooserIntent = Intent.createChooser(pickIntent, "Select Audio");
                startActivityForResult(chooserIntent, PICK_AUDIO);

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.save_note:
                saveNote();
                return true;
            case R.id.undo_note:
                mEditor.undo();
                return true;
            case R.id.redo_note:
                mEditor.redo();
                return true;
            case R.id.show_html:

               HTMLFragment dialogFragment = new HTMLFragment();
               Bundle bundle = new Bundle();
                bundle.putString(DIARY_HTML, mEditor.getHtml());
                bundle.putBoolean("fullScreen", true);
                bundle.putBoolean("notAlertDialog", true);
                dialogFragment.setArguments(bundle);


                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                dialogFragment.show(ft, "dialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private String getTagsText() {
        String s = tagsET.getText().toString().trim();
        if (s.isEmpty()) {
            return s;
        } else {
            String[] tgs = s.split(",");
            StringBuilder m = new StringBuilder();
            for (String x : tgs) {
                x = WordUtils.capitalize(x.trim());
                if (!x.isEmpty()) {
                    m.append(x).append(",");
                }
            }
            return m.toString();
        }
    }

    @Override
    public void onFinishEditDialog(String inputText) {
        mEditor.setHtml(inputText);
        showToast("updated");
    }
}