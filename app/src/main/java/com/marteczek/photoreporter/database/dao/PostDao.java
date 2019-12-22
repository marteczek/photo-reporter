package com.marteczek.photoreporter.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.marteczek.photoreporter.database.entity.Post;

import java.util.List;

@Dao
public interface PostDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Long insert(Post post);

    @Query("DELETE FROM posts")
    void deleteAll();

    @Query("DELETE FROM posts WHERE id_report = :reportId")
    void deleteByReportId(Long reportId);

    @Query("SELECT * FROM posts")
    LiveData<List<Post>> findAllAsync();

    @Query("SELECT * FROM posts WHERE id_report = :reportId")
    List<Post> findByReportId(Long reportId);

    @Query("SELECT * FROM posts WHERE id_report = :reportId")
    LiveData<List<Post>> findByReportIdAsync(Long reportId);

    @Query("UPDATE posts SET content = :content WHERE id = :id")
    void updateContentById(Long id, String content);

    @Query("UPDATE posts SET generated_content = :content WHERE id = :id")
    void updateGeneratedContentById(Long id, String content);

}
