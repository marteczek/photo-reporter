package com.marteczek.photoreporter.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.widget.ImageView;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class PicturePreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PICTURE_PATH = "extra_picture_path";
    public static final String EXTRA_ROTATION = "extra_rotation";

    @Inject
    ViewModelFactory viewModelFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        ImageView imageView = findViewById(R.id.image);
        PicturePreviewViewModel viewModel = new ViewModelProvider(this, viewModelFactory).get(
                PicturePreviewViewModel.class);
        String picturePath =  getIntent().getStringExtra(EXTRA_PICTURE_PATH);
        int rotation = getIntent().getIntExtra(EXTRA_ROTATION, 0);
        viewModel.getPicture(picturePath, rotation).observe(this, imageView::setImageBitmap);
    }
}
