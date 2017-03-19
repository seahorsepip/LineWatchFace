package com.seapip.thomas.line_watchface;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.support.wearable.view.WearableRecyclerView;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * The watch-side config activity for {@link WatchFaceService}, which
 * allows for setting complications on the left and right of watch face.
 */
public class ConfigActivity extends WearableActivity implements ConfigAdapter.ItemSelectedListener {
    private ConfigAdapter mAdapter;
    private SharedPreferences mPrefs;
    private WearableRecyclerView mWearableRecyclerView;
    private SparseArray<ComplicationProviderInfo> complicationProviderInfos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        mAdapter = new ConfigAdapter(getApplicationContext(), new ArrayList<ConfigItem>());
        mAdapter.setListener(ConfigActivity.this);
        mWearableRecyclerView = (WearableRecyclerView) findViewById(R.id.recycler_launcher_view);
        mWearableRecyclerView.setAdapter(mAdapter);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        complicationProviderInfos = new SparseArray<>();
        Executor executor = new Executor() {
            @Override
            public void execute(@NonNull Runnable r) {
                new Thread(r).start();
            }
        };

        ProviderInfoRetriever.OnProviderInfoReceivedCallback callback = new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
            @Override
            public void onProviderInfoReceived(int i, @Nullable ComplicationProviderInfo complicationProviderInfo) {
                complicationProviderInfos.put(i, complicationProviderInfo);
                mAdapter.update(getConfigurationItems());
            }
        };


        ProviderInfoRetriever providerInfoRetriever = new ProviderInfoRetriever(getApplicationContext(), executor);

        providerInfoRetriever.init();
        providerInfoRetriever.retrieveProviderInfo(callback,
                new ComponentName(
                        getApplicationContext(),
                        WatchFaceService.class),
                WatchFaceService.COMPLICATION_IDS);
    }

    @Override
    protected void onActivityResult(int position, int resultCode, Intent data) {
        SharedPreferences.Editor mEditor = mPrefs.edit();
        ConfigItem item = getConfigurationItems().get(position);
        if (resultCode == RESULT_OK) {
            switch (item.getRequestCode()) {
                case ConfigItem.PREFERENCE:
                    Preference value = item.getPreferenceValue();
                    mEditor.putInt(item.getPreference(), value.fromValue(value.getValue() + 1).getValue()).apply();
                    break;
                case ConfigItem.COMPLICATION:
                    complicationProviderInfos.put(item.getComplicationId(), (ComplicationProviderInfo) data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO));
                    break;
            }
            mAdapter.update(getConfigurationItems());
        }
    }

    @Override
    public void onItemSelected(int position) {
        Intent activity = getConfigurationItems().get(position).getActivity();
        if (activity != null) {
            startActivityForResult(activity, position);
        } else {
            onActivityResult(position, RESULT_OK, null);
        }
    }

    private String complicationProviderName(int key) {
        return complicationProviderInfos.get(key) != null ? complicationProviderInfos.get(key).providerName : "Empty";
    }

    private ArrayList<ConfigItem> getConfigurationItems() {
        ComponentName watchFace = new ComponentName(
                getApplicationContext(), WatchFaceService.class);

        BackgroundEffect backgroundEffect = BackgroundEffect.NONE.fromValue(
                mPrefs.getInt("setting_background_effect",
                        BackgroundEffect.NONE.getValue()
                )
        );
        NotificationIndicator notificationIndicator = NotificationIndicator.DISABLED.fromValue(
                mPrefs.getInt("setting_notification_indicator",
                        NotificationIndicator.DISABLED.getValue()
                )
        );

        ConfigItem[] items = {
                new ConfigItem(
                        "Top \ncomplication",
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                getApplicationContext(),
                                watchFace,
                                WatchFaceService.COMPLICATION_IDS[0],
                                WatchFaceService.COMPLICATION_SUPPORTED_TYPES[0]),
                        getDrawable(R.drawable.ic_top_complication_black_24dp),
                        complicationProviderName(WatchFaceService.COMPLICATION_IDS[0]),
                        WatchFaceService.COMPLICATION_IDS[0]
                ),
                new ConfigItem(
                        "Left \ncomplication",
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                getApplicationContext(),
                                watchFace,
                                WatchFaceService.COMPLICATION_IDS[1],
                                WatchFaceService.COMPLICATION_SUPPORTED_TYPES[1]),
                        getDrawable(R.drawable.ic_left_complication_black_24dp),
                        complicationProviderName(WatchFaceService.COMPLICATION_IDS[1]),
                        WatchFaceService.COMPLICATION_IDS[1]
                ),
                new ConfigItem(
                        "Right \ncomplication",
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                getApplicationContext(),
                                watchFace,
                                WatchFaceService.COMPLICATION_IDS[2],
                                WatchFaceService.COMPLICATION_SUPPORTED_TYPES[2]),
                        getDrawable(R.drawable.ic_right_complication_black_24dp),
                        complicationProviderName(WatchFaceService.COMPLICATION_IDS[2]),
                        WatchFaceService.COMPLICATION_IDS[2]
                ),
                new ConfigItem(
                        "Color",
                        new Intent(ConfigActivity.this, ConfigColorActivity.class),
                        getDrawable(R.drawable.ic_color_lens_black_24dp),
                        mPrefs.getString("setting_color_name", "Cyan")
                ),
                new ConfigItem(
                        "Background",
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                getApplicationContext(),
                                watchFace,
                                WatchFaceService.COMPLICATION_IDS[3],
                                WatchFaceService.COMPLICATION_SUPPORTED_TYPES[3]),
                        getDrawable(R.drawable.ic_background_black_24dp),
                        complicationProviderName(WatchFaceService.COMPLICATION_IDS[3]),
                        WatchFaceService.COMPLICATION_IDS[3]
                ),
                new ConfigItem(
                        "Background effect",
                        null,
                        getDrawable(R.drawable.ic_background_effect_black_24dp),
                        backgroundEffect.toString(),
                        "setting_background_effect",
                        backgroundEffect
                ),
                new ConfigItem(
                        "Notification indicator",
                        null,
                        getDrawable(notificationIndicator == NotificationIndicator.DISABLED ?
                                R.drawable.ic_notifications_off_black_24dp :
                                R.drawable.ic_notifications_black_24dp),
                        notificationIndicator.toString(),
                        "setting_notification_indicator",
                        notificationIndicator
                ),
                new ConfigItem(
                        "Time format",
                        new Intent(Settings.ACTION_DATE_SETTINGS),
                        getDrawable(R.drawable.ic_time_black_24dp),
                        null
                )
        };
        return new ArrayList<>(Arrays.asList(items));
    }
}