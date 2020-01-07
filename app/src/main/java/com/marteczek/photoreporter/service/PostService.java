package com.marteczek.photoreporter.service;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.database.dao.PostDao;
import com.marteczek.photoreporter.database.dao.ReportDao;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Post;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.database.entity.type.ReportStatus;

import java.util.List;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static com.marteczek.photoreporter.application.Settings.Debug.E;

public class PostService {
    private static final String TAG = "PostService";

    private final ItemDao itemDao;

    private final ReportDao reportDao;

    private final PostDao postDao;

    private final ReportDatabaseHelper dbHelper;

    public PostService(PostDao postDao, ItemDao itemDao, ReportDao reportDao,
                       ReportDatabaseHelper reportDatabaseHelper) {
        this.itemDao = itemDao;
        this.reportDao = reportDao;
        this.postDao = postDao;
        this.dbHelper = reportDatabaseHelper;
    }

    public void generatePosts(final Long reportId, final int picturesPerPost, final String footer,
                              final OnErrorListener onErrorListener) {
        dbHelper.executeInTransaction(() -> {
            try {
                Report report = reportDao.findById(reportId);
                if (report != null) {
                    itemDao.updatePostIdSetNullByReportId(reportId);
                    postDao.deleteByReportId(reportId);
                    List<Item> items = itemDao.findByReportIdOrderBySuccession(reportId);
                    int itemsSize = items.size();
                    if (itemsSize == 0) {
                        Log.d(TAG, "Can't generate posts. Report has no items");
                        return;
                    }
                    int globalIndex = 1;
                    int localIndex = 1;
                    long postIndex = 1;
                    long postsNumber = (picturesPerPost > 0) ?
                            itemsSize / picturesPerPost + (itemsSize % picturesPerPost > 0 ? 1 : 0)
                            : 1;
                    Long postId = null;
                    StringBuilder contentBuilder = new StringBuilder();
                    for(Item item : items) {
                        if (localIndex == 1) {
                            postId = postDao.insert(Post.builder().reportId(reportId).number(postIndex)
                                    .build());
                        }
                        if (postsNumber > 1 && localIndex == 1) {
                            contentBuilder.setLength(0);
                            contentBuilder.append(postIndex);
                            contentBuilder.append(" / ");
                            contentBuilder.append(postsNumber);
                            contentBuilder.append("\n");
                        }
                        contentBuilder.append(globalIndex);
                        contentBuilder.append(". ");
                        String header = item.getHeader();
                        if (header != null) {
                            contentBuilder.append(header);
                        }
                        contentBuilder.append("\n");
                        String pictureUrl = item.getPictureUrl();
                        if (pictureUrl != null) {
                            contentBuilder.append("[IMG]");
                            contentBuilder.append(item.getPictureUrl());
                            contentBuilder.append("[/IMG]\n\n");
                        }
                        itemDao.updatePostIdById(item.getId(), postId);
                        if (globalIndex == itemsSize && footer != null) {
                            contentBuilder.append(footer);
                        }
                        if ((picturesPerPost != 0 && localIndex == picturesPerPost)
                                || globalIndex == itemsSize) {
                            String content = contentBuilder.toString();
                            if(D) Log.d(TAG, "" + postId +"\n" + content);
                            postDao.updateGeneratedContentById(postId, content);
                            postDao.updateContentById(postId, content);
                        }
                        globalIndex++;
                        localIndex++;
                        if (picturesPerPost != 0 && localIndex > picturesPerPost) {
                            localIndex = 1;
                            postIndex++;
                        }
                    }
                }
                reportDao.updateStatusById(reportId, ReportStatus.POST_CREATED);
            } catch (RuntimeException e) {
                if(E) Log.e(TAG, "RuntimeException", e);
                if (onErrorListener != null) {
                    onErrorListener.onError(e);
                }
            }
        });
    }

    public LiveData<List<Post>> findPostsByReportId(Long reportId) {
        return postDao.findByReportIdAsync(reportId);
    }

    public void updatePostById(final Long postId, final String postContent) {
        dbHelper.execute(() -> postDao.updateContentById(postId, postContent));
    }
}
