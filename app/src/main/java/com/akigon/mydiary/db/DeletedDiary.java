package com.akigon.mydiary.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DeletedDiary {
    @PrimaryKey
    private long createdAt;
    private long deletedAt;


    public DeletedDiary(long createdAt, long deletedAt) {
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(long deletedAt) {
        this.deletedAt = deletedAt;
    }
}
