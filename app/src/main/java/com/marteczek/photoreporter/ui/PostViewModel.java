package com.marteczek.photoreporter.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.marteczek.photoreporter.database.entity.Post;
import com.marteczek.photoreporter.service.PostService;

import java.util.List;

public class PostViewModel extends AndroidViewModel {

    private PostService postService;

    public PostViewModel(Application application, PostService postService) {
        super(application);
        this.postService = postService;
    }

    LiveData<List<Post>> findPostsByReportId(Long reportId) {
        return postService.findPostsByReportId(reportId);
    }

    void savePost(Long postId, String postContent) {
        postService.updatePostById(postId, postContent);
    }
}
