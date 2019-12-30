package com.marteczek.photoreporter.ui.misc;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.marteczek.photoreporter.R;

public class ImagePreference extends Preference {

    private ImageView imageView;

    private Bitmap bitmap;

    public ImagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_image);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        imageView = (ImageView) holder.findViewById(R.id.image);
        imageView.setImageBitmap(bitmap);
    }

    public void setImage(Bitmap bitmap) {
        if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
        this.bitmap = bitmap;
    }
}
