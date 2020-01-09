package com.marteczek.photoreporter.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.marteczek.photoreporter.database.entity.Item;

import java.util.List;

@Dao
public interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Long insert(Item item);

    @Query("DELETE FROM items WHERE id = :id")
    void deleteById(Long id);

    @Query("DELETE FROM items")
    void deleteAll();

    @Query("DELETE FROM items WHERE id_report = :reportId")
    void deleteByReportId(Long reportId);

    @Query("SELECT * FROM items WHERE id_report = :reportId AND status IN (:statuses) ORDER BY succession")
    List<Item> findByReportIdAndStatusesOrderBySuccession(Long reportId, String[] statuses);

    @Query("SELECT * FROM items")
    List<Item> findAll();

    @Query("SELECT * FROM items")
    LiveData<List<Item>> findAllAsync();

    @Query("SELECT * FROM items WHERE id_report = :reportId")
    List<Item> findByReportId(Long reportId);

    @Query("SELECT * FROM items WHERE id_report = :reportId ORDER BY succession")
    List<Item> findByReportIdOrderBySuccession(Long reportId);

    @Query("SELECT * FROM items WHERE id_report = :reportId AND picture_uri = :pictureUri")
    List<Item> findByReportIdAndPictureUri(Long reportId, String pictureUri);

    @Query("SELECT * FROM items WHERE id_report = :reportId AND thumbnail_path IS NULL")
    List<Item> findByReportIdAndThumbnailPathIsNull(Long reportId);

    @Query("SELECT * FROM items WHERE id_report = :reportId ORDER BY succession")
    LiveData<List<Item>> findByReportIdOrderBySuccessionAsync(Long reportId);

    @Query("SELECT MAX(succession) FROM items WHERE id_report = :reportId")
    Long findMaxSuccessionByReportId(Long reportId);

    @Query("UPDATE items SET header = :header WHERE id = :id")
    void updateHeaderById(Long id, String header);

    @Query("UPDATE items SET picture_url = :pictureUrl WHERE id = :id")
    void updatePictureUrlById(Long id, String pictureUrl);

    @Query("UPDATE items SET host_metadata = :metadata WHERE id = :id")
    void updateMetadataById(Long id, String metadata);

    @Query("UPDATE items SET picture_path = :path WHERE id = :id")
    void updatePicturePathById(Long id, String path);

    @Query("UPDATE items SET picture_rotation = :imageRotation WHERE id = :id")
    void updatePictureRotationById(Long id, int imageRotation);

    @Query("UPDATE items SET id_post = :postId WHERE id = :id")
    void updatePostIdById(Long id, Long postId);

    @Query("UPDATE items SET id_post = null WHERE id_report = :reportId")
    void updatePostIdSetNullByReportId(Long reportId);

    @Query("UPDATE items SET status = :status WHERE id = :id")
    void updateStatusById(Long id, String status);

    @Query("UPDATE items SET status = :status WHERE id_report = :reportId")
    void updateStatusByReportId(Long reportId, String status);

    @Query("UPDATE items SET succession = :succession WHERE id = :id")
    void updateSuccessionById(Long id, Long succession);

    @Query("UPDATE items SET thumbnail_path = :path WHERE id = :id")
    void updateThumbnailPathById(Long id, String path);

}
