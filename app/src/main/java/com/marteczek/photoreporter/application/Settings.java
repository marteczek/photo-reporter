package com.marteczek.photoreporter.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.data.ImgurUserData;

import java.util.UUID;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static java.lang.Math.round;

public class Settings {
    private static final String TAG = "Settings";

    public static class Debug {
        public static final boolean D = false;
        public static final boolean E = false;
        public static final boolean TEST_UPLOAD_CLIENT_ENABLED = false;
        public static final boolean TEST_UPLOAD_CLIENT_SIMULATE_ERROR = false;
        public static final boolean DONT_PUBLISH_POSTS = false;
    }

    public static class Build{
        public static final String TagRestriction = null;
    }

    private static final String SHARED_PREFERENCES_FILE_NAME = "settings";

    private static final String CLIENT_UUID = "client_uuid";

    private static final String IMGUR_USER_DATA = "imgur_user_data";

    public static final String CHANNEL_UPLOAD_ID = "channel_general";

    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0(Linux)";

    public static String getClientId(Context context) {
        return context.getString(R.string.client_id);

    }

    public static String getClientUUID (Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE);
        String clientUUID = sharedPref.getString(CLIENT_UUID, null);
        if (clientUUID == null) {
            clientUUID = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(CLIENT_UUID, clientUUID);
            editor.apply();
        }
        return clientUUID;
    }

    public static String getPictureHost(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getString("picture_host", "");
    }

    public static String getPictureOrder(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getString("picture_order", "");
    }

    public static ImgurUserData getImgurUserData(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE);
        String json = sharedPref.getString(IMGUR_USER_DATA, null);
        ImgurUserData userData = null;
        if (json != null) {
            userData = new Gson().fromJson(json, ImgurUserData.class);
        }
        return  userData;
    }

    public static void setImgurUserData(Context context, ImgurUserData userData) {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (userData != null) {
            String json = new Gson().toJson(userData);
            editor.putString(IMGUR_USER_DATA, json);
        } else {
            editor.remove(IMGUR_USER_DATA);
        }
        editor.apply();
    }

    public static int getPictureDimension(Context context) {
        return getIntPreference(context, "picture_dimension", 1024);
    }

    public static int getPictureQuality(Context context) {
        return getIntPreference(context, "picture_quality", 90);
    }

    public static String getSignature(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getString("signature", "");
    }


    public static int getThumbnailDimension(Context context) {
        int dimDP = getIntPreference(context, "thumbnail_dimension", 200);
        int dim = round(dimDP * (context.getResources().getDisplayMetrics().xdpi
                / DisplayMetrics.DENSITY_DEFAULT));
        if (D) Log.d(TAG, "ThumbnailDimension: " + dim);
        return dim;
    }

    public static int getPicturesPerPost(Context context) {
        return getIntPreference(context, "pictures_per_post", 0);
    }

    private static int getIntPreference(Context context, String key, int defaultValue) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return Integer.valueOf(appPreferences.getString(key, ""));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static boolean shouldAttachAppFooter(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getBoolean("attach_app_footer", true);
    }
}
