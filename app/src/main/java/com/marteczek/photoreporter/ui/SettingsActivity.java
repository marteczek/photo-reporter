package com.marteczek.photoreporter.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.configuration.viewmodelfactory.ViewModelFactory;
import com.marteczek.photoreporter.ui.misc.ImagePreference;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.support.AndroidSupportInjection;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.marteczek.photoreporter.application.Settings.Debug.D;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TAG = "SettingsActivity";

    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    private static final int CHOOSE_PICTURES_REQUEST_CODE = 2;

    private static final String TAG_FRAGMENT_WATERMARK = "tag_fragment_watermark";

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.getSupportActionBar().setTitle(R.string.title_activity_settings);
        }
    }

    public static class SettingsWatermarkFragment extends PreferenceFragmentCompat{

        LiveData<Bitmap> thumbnailBitmap;

        @Inject
        ViewModelFactory viewModelFactory;

        private SettingsWatermarkViewModel viewModel;

        public SettingsWatermarkFragment() {
        }

        @Override
        public void onAttach(@NonNull Context context) {
            AndroidSupportInjection.inject(this);
            super.onAttach(context);
            if(D) Log.d(TAG, "viewModelFactory: " + viewModelFactory);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_watermark, rootKey);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            final SettingsActivity activity = (SettingsActivity) getActivity();
            activity.getSupportActionBar().setTitle(R.string.title_fragment_settings_watermark);
            SeekBarPreference distancePreference = findPreference("watermark_distance");
            distancePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                final int progress = Integer.valueOf(String.valueOf(newValue));
                preference.setSummary(this.getString(R.string.preference_distance) + ": " + progress
                        + " px");
                return true;
            });
            ImagePreference imagePreference = findPreference("watermark_image");
            imagePreference.setOnPreferenceClickListener((preference) -> {
                if (ActivityCompat.checkSelfPermission(getContext(), READ_EXTERNAL_STORAGE)
                        == PERMISSION_GRANTED) {
                    activity.choosePicture();
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{READ_EXTERNAL_STORAGE},
                            READ_EXTERNAL_STORAGE_REQUEST_CODE);
                }
                return true;
            });
            viewModel = new ViewModelProvider(this, viewModelFactory).get(SettingsWatermarkViewModel.class);
            thumbnailBitmap = viewModel.getWatermarkThumbnail();
            thumbnailBitmap.observe(getViewLifecycleOwner(), imagePreference::setImage);
        }

        void setWatermark(Uri uri) {
            viewModel.setWatermark(uri);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            SettingsFragment settingsFragment = new SettingsFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, settingsFragment)
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PERMISSION_GRANTED) {
            choosePicture();
        }
    }

    private void choosePicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent,
                getResources().getString(R.string.select_pictures)), CHOOSE_PICTURES_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_PICTURES_REQUEST_CODE && resultCode == RESULT_OK && data != null){
            Uri uri = data.getData();
            SettingsWatermarkFragment fragmentWatermark = (SettingsWatermarkFragment)
                    getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_WATERMARK);
            if (fragmentWatermark != null) {
                fragmentWatermark.setWatermark(uri);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        String tag = null;
        if (fragment instanceof SettingsWatermarkFragment) {
            tag = TAG_FRAGMENT_WATERMARK;
        }
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment, tag)
                .addToBackStack(null)
                .commit();
        return true;
    }
}