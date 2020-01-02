package com.marteczek.photoreporter.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.marteczek.photoreporter.database.entity.Report;

import java.util.List;

@Dao
public interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Report report);

    @Query("DELETE FROM reports WHERE id = :id")
    void deleteById(Long id);

    @Query("DELETE FROM reports")
    void deleteAll();

    @Query("SELECT * FROM reports")
    LiveData<List<Report>> findAllAsync();

    @Query("SELECT * FROM reports ORDER BY date DESC")
    LiveData<List<Report>> findAllOrderByDateDescAsync();

    @Query("SELECT * FROM reports WHERE id = :id")
    Report findById(Long id);

    @Query("SELECT * FROM reports WHERE id = :id")
    LiveData<Report> findByIdAsync(Long id);

    @Query("SELECT DISTINCT thread_id FROM reports")
    LiveData<List<String>> findThreadsIds();

    @Query("UPDATE reports SET host_metadata = :metadata WHERE id = :id")
    void updateHostMetadataById(Long id, String metadata);

    @Query("UPDATE reports SET name = :name WHERE id = :id")
    void updateNameById(Long id, String name);

    @Query("UPDATE reports SET status = :status WHERE id = :id")
    void updateStatusById(Long id, String status);

    @Query("UPDATE reports SET thread_id = :threadId WHERE id = :id")
    void updateThreadId(Long id, String threadId);

}
