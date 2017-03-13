package com.seapip.thomas.line_watchface;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.support.wearable.view.WearableListView;
import android.support.wearable.view.WearableRecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The watch-side config activity for {@link LineWatchFaceService}, which
 * allows for setting complications on the left and right of watch face.
 */
public class LineWatchFaceConfigActivity extends Activity {
    private ConfigurationAdapter mAdapter;
    private SharedPreferences mPrefs;
    //private WearableRecyclerView mWearableRecyclerView;
    private HashMap<Integer, ComplicationProviderInfo> complicationProviderInfos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_watch_face_config);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        complicationProviderInfos = new HashMap<>();

        WearableListView wearableListView = (WearableListView) findViewById(R.id.wearable_List);
        wearableListView.setAdapter(new ConfigurationAdapter(this, getConfigurationItems()));
        wearableListView.setClickListener(mClickListener);

        //Magic happens here


    }

    // Handle our Wearable List's click events
    private WearableListView.ClickListener mClickListener = new WearableListView.ClickListener() {
        @Override
        public void onClick(WearableListView.ViewHolder viewHolder) {
            Toast.makeText(LineWatchFaceConfigActivity.this,
                    "Click",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onTopEmptyRegionClick() {

        }
    };


    private String complicationProviderName(HashMap data, int key) {
        return complicationProviderInfos.containsKey(key) && complicationProviderInfos.get(key) != null ? complicationProviderInfos.get(key).providerName : "Empty";
    }

    private ArrayList<ConfigurationItemModel> getConfigurationItems() {
        ComponentName watchFace = new ComponentName(
                getApplicationContext(), LineWatchFaceService.class);

        int[] complicationIds = LineWatchFaceService.COMPLICATION_IDS;

        TypedArray icons = getResources().obtainTypedArray(R.array.line_watch_face_icons);

        ArrayList<ConfigurationItemModel> items = new ArrayList<>();
        items.add(new ConfigurationItemModel("Top \ncomplication",
                getDrawable(R.drawable.ic_top_complication),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[0],
                        LineWatchFaceService.COMPLICATION_SUPPORTED_TYPES[0]),
                complicationProviderName(complicationProviderInfos, 0)));
        items.add(new ConfigurationItemModel("Left \ncomplication",
                getDrawable(R.drawable.ic_left_complication),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[1],
                        LineWatchFaceService.COMPLICATION_SUPPORTED_TYPES[1]),
                complicationProviderName(complicationProviderInfos, 1)));
        items.add(new ConfigurationItemModel("Right \ncomplication",
                getDrawable(R.drawable.ic_right_complication),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[2],
                        LineWatchFaceService.COMPLICATION_SUPPORTED_TYPES[2]),
                complicationProviderName(complicationProviderInfos, 2)));
        items.add(new ConfigurationItemModel("Color",
                getDrawable(R.drawable.ic_color),
                new Intent(LineWatchFaceConfigActivity.this, LineWatchFaceConfigColorActivity.class),
                mPrefs.getString("setting_color_name", "Cyan")));
        items.add(new ConfigurationItemModel("Background",
                getDrawable(R.drawable.ic_background),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[3],
                        LineWatchFaceService.COMPLICATION_SUPPORTED_TYPES[3]),
                complicationProviderName(complicationProviderInfos, 3)));
        BackgroundEffect backgroundEffect = BackgroundEffect.fromValue(
                mPrefs.getInt("setting_background_effect",
                        BackgroundEffect.NONE.getValue()
                )
        );
        items.add(new ConfigurationItemModel("Background effect",
                getDrawable(R.drawable.ic_background_effect),
                null,
                backgroundEffect.toString()));
        NotificationIndicator notificationIndicator = NotificationIndicator.fromValue(
                mPrefs.getInt("setting_notification_indicator",
                        NotificationIndicator.DISABLED.getValue()
                )
        );
        items.add(new ConfigurationItemModel("Notification indicator",
                getDrawable(notificationIndicator == NotificationIndicator.DISABLED ? R.drawable.ic_notification_disabled : R.drawable.ic_notification),
                null,
                notificationIndicator.toString()));
        items.add(new ConfigurationItemModel("Time format",
                getDrawable(R.drawable.ic_time_format),
                new Intent(Settings.ACTION_DATE_SETTINGS)));
        icons.recycle();
        return items;
    }
}