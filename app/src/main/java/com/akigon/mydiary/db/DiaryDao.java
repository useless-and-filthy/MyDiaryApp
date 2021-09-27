package com.akigon.mydiary.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DiaryDao {
    @Query("SELECT * FROM Diary WHERE (categories LIKE :cats) AND (content LIKE :searchText OR title LIKE :searchText) AND (isArchived=:isArchived) ORDER BY createdAt desc LIMIT(5) offset(:ofst)")
    List<Diary> getDiaries(String searchText, String cats, boolean isArchived, int ofst);

    @Query("SELECT  COUNT(*) FROM Diary WHERE (categories LIKE :cats) AND (content LIKE :searchText OR title LIKE :searchText) AND (isArchived=:isArchived)")
    int countDiaries(String searchText, String cats, boolean isArchived);

    @Query("SELECT * FROM Diary WHERE id=:diaryId")
    Diary getDiary(int diaryId);

    @Query("SELECT * FROM Diary ORDER BY createdAt desc")
    List<Diary> getAll();

    @Query("SELECT * FROM Diary WHERE updatedAt>=:laststamp")
    List<Diary> getOfflineDiaries(long laststamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Diary diary);

    @Delete
    void delete(Diary diary);

    @Query("DELETE FROM Diary")
    void deleteAllDiaries();

    @Query("SELECT DISTINCT categories FROM Diary WHERE isArchived=:isArchive")
    List<String> getDistinctTags(boolean isArchive);

    //for deleted diary
    @Query("SELECT createdAt FROM DeletedDiary")
    List<Long> getDeletedIds();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDeleted(DeletedDiary... diaries);

    @Query("DELETE FROM DeletedDiary")
    void deleteAllDeletedDiaries();

    //sync
    @Query("SELECT * FROM Diary WHERE updatedAt>=:afterTimestamp")
    List<Diary> getModifiedDiaries(long afterTimestamp);

    @Query("SELECT EXISTS(SELECT * FROM Diary WHERE objectId=:objId AND updatedAt>=:updatedAt)")
    Boolean doesExist(String objId, long updatedAt);
}
