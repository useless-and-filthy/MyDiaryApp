package com.akigon.mydiary.db;

import android.util.Log;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Entity
public class Diary {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private long createdAt;
    private long updatedAt;
    private String objectId;
    private String title;
    private String content;
    private String categories;
    private boolean isArchived;
    private String filesString;

    public Diary(long createdAt, long updatedAt, String objectId, String title, String content, String categories, boolean isArchived, String filesString) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.objectId = objectId;
        this.title = title;
        this.content = content;
        this.categories = categories;
        this.isArchived = isArchived;
        this.filesString = filesString;
    }

    public String getFilesString() {
        return filesString;
    }

    public void setFilesString(String filesString) {
        this.filesString = filesString;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getObjectId() {
        return objectId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }


}
