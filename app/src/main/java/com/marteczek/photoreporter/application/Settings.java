package com.marteczek.photoreporter.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.marteczek.photoreporter.BuildConfig;
import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.application.data.ImgurUserData;

import java.util.UUID;

import static com.marteczek.photoreporter.application.Settings.Debug.D;
import static com.marteczek.photoreporter.application.Settings.Debug.E;
import static java.lang.Math.round;

public class Settings {
    private static final String TAG = "Settings";

    public static class Debug {
        public static final boolean D = BuildConfig.DEBUG;
        public static final boolean E = BuildConfig.DEBUG;
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

    public static final String WATERMARK_FILE_NAME = "watermark";

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

    public static String getGoogleStaticMapKey() {
        byte[] keyBytes = new byte[] {
                65, 73, 122, 97, 83, 121, 67, 67, 102, 77, 76, 120, 105, 115, 73, 88, 71, 80,
                101, 77, 97, 65, 97, 114, 84, 74, 73, 103, 115, 45, 77, 80, 111, 56, 54, 99, 98,
                89, 111
        };
        return new String(keyBytes);
    }

    public static String getMapShape(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getString("map_shape", context.getString(R.string.preference_square));
    }

    public static int getMapTransparency(Context context) {
        return getIntPreference(context, "map_transparency", 0);
    }

    public static int getMapZoom(Context context) {
        return getIntPreference(context, "map_zoom", 0);
    }

    public static int getPictureDimension(Context context) {
        return getIntPreference(context, "picture_dimension", 1024);
    }

    public static String getPictureOrder(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getString("picture_order", "");
    }

    public static int getPicturesPerPost(Context context) {
        return getIntPreference(context, "pictures_per_post", 0);
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

    public static int getWatermarkDistance(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getInt("watermark_distance", 0);
    }

    public static String getWatermarkPosition(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getString("watermark_position",
                context.getString(R.string.preference_bottom_right));
    }

    public static boolean shouldApplyGPSDirection(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getBoolean("apply_gps_direction", false);
    }

    public static boolean shouldApplyMap(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getBoolean("apply_map", false);
    }

    public static boolean shouldApplyWatermark(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getBoolean("apply_watermark", false);
    }

    public static boolean shouldAttachAppFooter(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getBoolean("attach_app_footer", true);
    }

    private static int getIntPreference(Context context, String key, int defaultValue) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return Integer.valueOf(appPreferences.getString(key, ""));
        } catch (NumberFormatException e) {
            if(E) Log.e(TAG, "NumberFormatException", e);
            return defaultValue;
        }
    }
}