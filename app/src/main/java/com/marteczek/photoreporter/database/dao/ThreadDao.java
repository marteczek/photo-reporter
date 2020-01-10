package com.marteczek.photoreporter.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.marteczek.photoreporter.database.entity.ForumThread;

import java.util.Date;
import java.util.List;

@Dao
public interface ThreadDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Long insert(ForumThread thread);

    @Query("DELETE FROM threads WHERE thread_id = :threadId")
    void deleteByThreadId(String threadId);

    @Query("SELECT * FROM threads WHERE thread_id = :threadId")
    ForumThread findByThreadId(String threadId);

    @Query("SELECT * FROM threads WHERE thread_id = :threadId")
    LiveData<ForumThread> findByThreadIdAsync(String threadId);

    @Query("SELECT * FROM threads LIMIT 1")
    ForumThread findOne();

    @Query("SELECT * FROM threads ORDER BY name")
    LiveData<List<ForumThread>> findAllOrderByNameAsync();

    @Query("UPDATE threads SET name = :name, last_usage = :lastUsage, thread_path = :threadPath " +
            "WHERE thread_id = :threadId")
    void updateNameLastUsageAndThreadPathByThreadId(String threadId, String name, Date lastUsage,
                                                    String threadPath);
}
