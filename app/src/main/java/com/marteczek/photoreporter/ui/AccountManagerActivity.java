package com.marteczek.photoreporter.ui;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.Settings;
import com.marteczek.photoreporter.application.data.ImgurUserData;

public class AccountManagerActivity extends AppCompatActivity {

    private TextView imgurLoginNameTextView;

    private Switch imgurLoggedInSwitch;

    CompoundButton.OnCheckedChangeListener onCheckedChangeListener =
            (compoundButton, isChecked) -> {
                if (isChecked) {
                    Intent intent = new Intent(this, ImgurLoginActivity.class);
                    startActivity(intent);
                } else {
                    Settings.setImgurUserData(this, null);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_manager);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        imgurLoginNameTextView = findViewById(R.id.imgur_login_name);
        imgurLoggedInSwitch = findViewById(R.id.imgur_logged_in);
    }

    @Override
    protected void onStart() {
        super.onStart();
        imgurLoggedInSwitch.setOnCheckedChangeListener(null);
        setImgurData();
        imgurLoggedInSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    private void setImgurData() {
        ImgurUserData imgurUserData = Settings.getImgurUserData(this);
        if (imgurUserData == null) {
            imgurLoggedInSwitch.setChecked(false);
        } else {
            imgurLoggedInSwitch.setChecked(true);
            imgurLoginNameTextView.setText(imgurUserData.getAccountUsername());
        }
    }
}
