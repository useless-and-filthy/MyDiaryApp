package com.akigon.mydiary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.akigon.mydiary.db.AppDatabase;
import com.akigon.mydiary.db.Diary;
import com.parse.CountCallback;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;


import static com.akigon.mydiary.BFunctions.ADD_NOTE;
import static com.akigon.mydiary.BFunctions.DIARY_CATEGORIES;
import static com.akigon.mydiary.BFunctions.DIARY_CLASS_NAME;
import static com.akigon.mydiary.BFunctions.DIARY_CONTENT;
import static com.akigon.mydiary.BFunctions.DIARY_CREATED_AT;
import static com.akigon.mydiary.BFunctions.DIARY_TITLE;
import static com.akigon.mydiary.BFunctions.DIARY_UPDATED_AT;
import static com.akigon.mydiary.BFunctions.DIARY_USER;
import static com.akigon.mydiary.BFunctions.EDIT_NOTE;
import static com.akigon.mydiary.BFunctions.SETTING_SEND;
import static com.akigon.mydiary.BFunctions.SP_AUTO_SYNC_KEY;
import static com.akigon.mydiary.BFunctions.SP_LAST_SYNC_STAMP;
import static com.akigon.mydiary.BFunctions.getListFromString;
import static com.akigon.mydiary.BFunctions.getStringFromList;
import static com.akigon.mydiary.BFunctions.showAlert;

public class MainActivity extends AppCompatActivity {


    private TextView searchCount;
    private NestedScrollView nestedScrollView;
    private RecyclerView recyclerView;
    // private ProgressBar progressBarSync;
    private List<Diary> list = new ArrayList<>();
    private DiaryRecyclerAdapter adapter = new DiaryRecyclerAdapter(this, list);
    ;

    private LinearLayout tagsLayout;
    private static final String TAG = "MainActivity";

    AppDatabase db;
    // powerful query
    private String searchText = "";
    private String tagText = "";
    private int skip = 0;
    SharedPreferences sharedPreferences;
    private boolean isArchived = false;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = AppDatabase.getInstance(this);

        sharedPreferences = getSharedPreferences(ParseUser.getCurrentUser().getObjectId(), Context.MODE_PRIVATE);
        recyclerView = findViewById(R.id.notice_recycler_view);
        // progressBarSync = findViewById(R.id.progressbar_sync);
        searchCount = findViewById(R.id.searchResultCount);
        tagsLayout = findViewById(R.id.tagLL);
        nestedScrollView = findViewById(R.id.notice_scroll_view);
        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    //when reach last item position
                    int indexStart = list.size();
                    List<Diary> newList = db.diaryDao().getDiaries(getSearchText(), getTagText(), isArchived, indexStart);
                    if (newList.isEmpty()) {
                        return;
                    }
                    for (Diary xx : newList) {
                        if (!list.contains(xx)) {
                            list.add(xx);
                        }
                    }
                    adapter.notifyItemRangeInserted(indexStart, list.size());

                }
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 22);
            }
        }

        if (sharedPreferences.getBoolean(SP_AUTO_SYNC_KEY, false)) {
            Log.d(TAG, "onCreate: "+sharedPreferences.getLong(SP_LAST_SYNC_STAMP, 0));
            sync(sharedPreferences.getLong(SP_LAST_SYNC_STAMP, 0));
        }


//        setting adapter
        ///adapter = new DiaryRecyclerAdapter(this, list);
        adapter.setOnItemClickListener(new DiaryRecyclerAdapter.OnItemClickListener() {
            @Override
            public void OnItemLongClick(Diary journal, int position) {
                showBottomSheet(journal, position);
            }

            @Override
            public void OnItemClick(Diary journal) {
                Intent intent = new Intent(MainActivity.this, ViewDiary.class);
                intent.putExtra("id", journal.getId());
                startActivityForResult(intent, EDIT_NOTE);
            }
        });

        recyclerView.setAdapter(adapter);
        newUI(true);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(false);
                newUI(true);
            }
        });
    }

    private String getSearchText() {
        return "%" + searchText + "%";
    }

    private String getTagText() {
        if (tagText.isEmpty() || tagText.equals("All"))
            return "%" + "" + "%";

        return "%" + tagText + "%";
    }


    private void newUI(boolean isNew) {
        list.clear();
        List<Diary> notes = db.diaryDao().getDiaries(getSearchText(), getTagText(), isArchived, 0);
        if (notes.isEmpty()) {
            Toast.makeText(this, "No Data", Toast.LENGTH_LONG).show();
        }
        list.addAll(notes);
        adapter.notifyDataSetChanged();
        updateSubtitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateTags();
    }

    private void updateSubtitle() {
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(db.diaryDao().countDiaries(getSearchText(), getTagText(), isArchived) + " Entries");
    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchCount.setVisibility(View.GONE);
                    searchText = query;
                    newUI(true);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String query) {
                    if (query.isEmpty()) {
                        searchCount.setVisibility(View.GONE);
                    } else {
                        searchCount.setVisibility(View.VISIBLE);
                        nestedScrollView.scrollTo(0, 0);
                        searchCount.setText("Found " + db.diaryDao().countDiaries("%" + query + "%", getTagText(), isArchived) + " Records");
                        searchCount.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onQueryTextSubmit(query);
                            }
                        });
                    }
                    return true;
                }
            });
            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    searchText = "";
                    newUI(true);
                    return false;
                }
            });
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.add_note:
                Intent intent = new Intent(this, AddEditDiary.class);
                startActivityForResult(intent, ADD_NOTE);
                return true;

            case R.id.settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), SETTING_SEND);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showBottomSheet(Diary journal, int position) {
        View view = getLayoutInflater().inflate(R.layout.bottom_options_diary, null);
        final Dialog mBottomSheetDialog = new Dialog(MainActivity.this, R.style.MaterialDialogSheet);
        mBottomSheetDialog.setContentView(view); // your custom view.
        mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);
        mBottomSheetDialog.show();

        TextView cancel = view.findViewById(R.id.bsd_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
            }
        });

        TextView delete = view.findViewById(R.id.bsd_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
                if (journal.getObjectId() != null) {
                    ParseQuery<ParseObject> query = new ParseQuery<ParseObject>(DIARY_CLASS_NAME);
                    query.getInBackground(journal.getObjectId(), new GetCallback<ParseObject>() {
                        @Override
                        public void done(ParseObject object, ParseException e) {
                            if (e==null && object!=null){
                                object.deleteEventually();
                            }

                        }
                    });
                }
                db.diaryDao().delete(journal);
                list.remove(journal);
                adapter.notifyItemRemoved(position);
                populateTags();
                showToast("Deleted");
            }
        });
    }


    private void populateTags() {
        ArrayList<String> tempList = new ArrayList<>();
        List<String> tags = db.diaryDao().getDistinctTags(isArchived);
        for (String x : tags) {
            String[] splitTags = x.split(",");
            for (String splitTag : splitTags) {
                splitTag = splitTag.trim();
                if (!splitTag.isEmpty() && !tempList.contains(splitTag)) {
                    tempList.add(splitTag);
                }
            }
        }
        appendCats(tempList);
    }

    private void appendCats(List<String> cats) {
        tagsLayout.removeAllViewsInLayout();
        cats.add(0, "All");

        View.OnClickListener tagClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeStyleOfTags();
                v.setBackgroundResource(R.drawable.bg_round);
                if (((TextView) v).getText().toString().equals("All")) {
                    tagText = "";
                } else {
                    tagText = ((TextView) v).getText().toString();
                }
                newUI(true);
            }
        };

        for (String x : cats) {
            TextView v = (TextView) getLayoutInflater().inflate(R.layout.tag_view, tagsLayout, false);
            v.setText(x);
            if (x.equals(tagText)) {
                v.setBackgroundResource(R.drawable.bg_round);
            } else if (tagText.isEmpty() && x.equals("All")) {
                v.setBackgroundResource(R.drawable.bg_round);
            } else {
                v.setBackgroundColor(Color.TRANSPARENT);
            }
            v.setOnClickListener(tagClickListener);
            tagsLayout.addView(v);
        }
    }

    private void removeStyleOfTags() {
        for (int i = 0; i < tagsLayout.getChildCount(); i++) {
            TextView tv = (TextView) tagsLayout.getChildAt(i);
            tv.setBackgroundColor(Color.TRANSPARENT);
        }
    }

//    public void showProgressBar() {
//        progressBarSync.setVisibility(View.VISIBLE);
//    }

//    public void setProgressMessage(String msg) {
//        progressBarSync.setMessage(msg);
//    }

//    public void hideProgressBar() {
//        progressBarSync.setVisibility(View.GONE);
//    }

    public void syncDone() {
        sharedPreferences.edit().putLong(SP_LAST_SYNC_STAMP, System.currentTimeMillis()).apply();
        Log.d(TAG, "syncDone: ");
    }


    //======================================================================================================
    private void sync(long last_sync_stamp) {
        //get all data
        //showProgressBar();
        List<Diary> offDiaries = db.diaryDao().getOfflineDiaries(last_sync_stamp);
        Log.d(TAG, "uploadToCloud: offline size "+offDiaries.size());
        for (Diary dd : offDiaries) {
            uploadOfflineFiles(dd);
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery(DIARY_CLASS_NAME);
        query.whereEqualTo(DIARY_USER, ParseUser.getCurrentUser());
        Date d = new Date();
        d.setTime(last_sync_stamp);
        query.whereGreaterThanOrEqualTo(DIARY_UPDATED_AT, d);
        query.orderByDescending(DIARY_UPDATED_AT);

        query.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                if (e == null) {
                    BigDecimal original = BigDecimal.valueOf((double) count / 100);
                    BigDecimal scaled = original.setScale(0, BigDecimal.ROUND_CEILING);
                    int itr = scaled.intValue();
                    for (int i = 0; i < itr; i++) {
                        fetchFromCloud(query, i * 100);
                    }
                }
            }
        });
    }

    private void fetchFromCloud(ParseQuery<ParseObject> query, int skip) {
        query.setLimit(100);
        query.setSkip(skip);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "found - cloud with skip: " + skip + " got size is " + objects.size());
                    for (ParseObject o : objects) {
                        if (!db.diaryDao().doesExist(o.getObjectId(), o.getUpdatedAt().getTime())) {
                            db.diaryDao().insert(getDiary(o));
                        }
                    }
                    newUI(true);
                } else {
                    //hideProgressBar();
                    showAlert(MainActivity.this, "Error: Sync", e.getMessage());
                }
            }
        });
    }

    private Diary getDiary(ParseObject d) {
        long crt = 0;
        if (d.has(DIARY_CREATED_AT)) {
            crt = Objects.requireNonNull(d.getDate(DIARY_CREATED_AT)).getTime();
        } else {
            crt = d.getCreatedAt().getTime();
        }
        return new Diary(crt, d.getUpdatedAt().getTime(), d.getObjectId(), d.getString(DIARY_TITLE), d.getString(DIARY_CONTENT), getStringFromList(d.getList(DIARY_CATEGORIES)), false, null);
    }


    public void uploadOfflineFiles(Diary di) {
        //upload files
        String main = di.getFilesString();
        if (main != null && !main.trim().isEmpty()) {
            String[] ss = main.split(",");
            for (String ur : ss) {
                if (!ur.trim().isEmpty()) {
                    Log.d(TAG, "getParseObject: " + ur.substring(7));
                    ParseFile parseFile = new ParseFile(new File(ur.substring(7)));
                    parseFile.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                di.setContent(di.getContent().replace(ur, parseFile.getUrl()));
                            } else {
                                Log.d(TAG, "done: file upload " + e.getMessage());
                            }

                            if (!di.getContent().contains("file://")) {
                                createParseObject(di);
                            } else {
                                Log.d(TAG, "done: replacing");
                            }
                        }
                    });
                }
            }

        } else {
            createParseObject(di);
        }
    }

    private void createParseObject(Diary di) {
        ParseObject p = new ParseObject(DIARY_CLASS_NAME);
        p.setACL(new ParseACL(ParseUser.getCurrentUser()));
        Date crT = new Date();
        crT.setTime(di.getCreatedAt());
        p.put(DIARY_CREATED_AT, crT);
        p.put(DIARY_TITLE, di.getTitle());
        p.put(DIARY_CONTENT, di.getContent());
        p.put(DIARY_CATEGORIES, getListFromString(di.getCategories()));
        p.put(DIARY_USER, ParseUser.getCurrentUser());
        p.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "done: saved --");
                    db.diaryDao().delete(di);
                } else {
                    Log.d(TAG, "done: upload error object" + e.getMessage());
                }
            }
        });
    }


}