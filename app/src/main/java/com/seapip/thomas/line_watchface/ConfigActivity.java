package com.seapip.thomas.line_watchface;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
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
        View item = mWearableRecyclerView.getLayoutManager().findViewByPosition(position).findViewById(R.id.text_wrapper);
        TextView text = (TextView) item.findViewById(R.id.value_item);
        ImageView image = (ImageView) mWearableRecyclerView.getLayoutManager().findViewByPosition(position).findViewById(R.id.image_item);
        Drawable drawable;
        String value = text.getText().toString();
        SharedPreferences.Editor mEditor = mPrefs.edit();
        if (resultCode == RESULT_OK) {
            switch (position) {
                case 0:
                case 1:
                case 2:
                case 4:
                    ComplicationProviderInfo complicationProviderInfo =
                            data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);

                    value = "Empty";
                    if (complicationProviderInfo != null) {
                        value = complicationProviderInfo.providerName;
                    }
                    break;
                case 3:
                    value = mPrefs.getString("setting_color_name", "Cyan");
                    break;
                case 5:
                    BackgroundEffect backgroundEffect = BackgroundEffect.fromValue(mPrefs.getInt("setting_background_effect", BackgroundEffect.NONE.getValue()));
                    assert backgroundEffect != null;
                    switch (backgroundEffect) {
                        case NONE:
                            backgroundEffect = BackgroundEffect.BLUR;
                            break;
                        case BLUR:
                            backgroundEffect = BackgroundEffect.DARKEN;
                            break;
                        case DARKEN:
                            backgroundEffect = BackgroundEffect.DARKEN_BLUR;
                            break;
                        case DARKEN_BLUR:
                            backgroundEffect = BackgroundEffect.NONE;
                            break;
                    }
                    mEditor.putInt("setting_background_effect", backgroundEffect.getValue()).apply();
                    value = backgroundEffect.toString();
                    break;
                case 6:
                    NotificationIndicator notificationIndicator = NotificationIndicator.fromValue(mPrefs.getInt("setting_notification_indicator", NotificationIndicator.DISABLED.getValue()));
                    drawable = getDrawable(R.drawable.ic_notifications_black_24dp);
                    switch (notificationIndicator) {
                        case DISABLED:
                            notificationIndicator = NotificationIndicator.UNREAD;
                            break;
                        case UNREAD:
                            notificationIndicator = NotificationIndicator.ALL;
                            break;
                        case ALL:
                            notificationIndicator = NotificationIndicator.DISABLED;
                            drawable = getDrawable(R.drawable.ic_notifications_off_black_24dp);
                            break;
                    }
                    mEditor.putInt("setting_notification_indicator", notificationIndicator.getValue()).apply();
                    value = notificationIndicator.toString();
                    float radius = 20 * getResources().getDisplayMetrics().density;
                    drawable = new ConfigDrawable(radius, drawable);
                    image.setImageDrawable(drawable);
                    break;
            }
            text.setText(value);
        }
        mWearableRecyclerView.invalidate();
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

        int[] complicationIds = WatchFaceService.COMPLICATION_IDS;

        ArrayList<ConfigItem> items = new ArrayList<>();
        items.add(new ConfigItem("Top \ncomplication",
                getDrawable(R.drawable.ic_top_complication_black_24dp),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[0],
                        WatchFaceService.COMPLICATION_SUPPORTED_TYPES[0]),
                complicationProviderName(0)));
        items.add(new ConfigItem("Left \ncomplication",
                getDrawable(R.drawable.ic_left_complication_black_24dp),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[1],
                        WatchFaceService.COMPLICATION_SUPPORTED_TYPES[1]),
                complicationProviderName(1)));
        items.add(new ConfigItem("Right \ncomplication",
                getDrawable(R.drawable.ic_right_complication_black_24dp),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[2],
                        WatchFaceService.COMPLICATION_SUPPORTED_TYPES[2]),
                complicationProviderName(2)));
        items.add(new ConfigItem("Color",
                getDrawable(R.drawable.ic_color_lens_black_24dp),
                new Intent(ConfigActivity.this, ConfigColorActivity.class),
                mPrefs.getString("setting_color_name", "Cyan")));
        items.add(new ConfigItem("Background",
                getDrawable(R.drawable.ic_background_black_24dp),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[3],
                        WatchFaceService.COMPLICATION_SUPPORTED_TYPES[3]),
                complicationProviderName(3)));
        BackgroundEffect backgroundEffect = BackgroundEffect.fromValue(
                mPrefs.getInt("setting_background_effect",
                        BackgroundEffect.NONE.getValue()
                )
        );
        items.add(new ConfigItem("Background effect",
                getDrawable(R.drawable.ic_background_effect_black_24dp),
                null,
                backgroundEffect.toString()));
        NotificationIndicator notificationIndicator = NotificationIndicator.fromValue(
                mPrefs.getInt("setting_notification_indicator",
                        NotificationIndicator.DISABLED.getValue()
                )
        );
        items.add(new ConfigItem("Notification indicator",
                getDrawable(notificationIndicator == NotificationIndicator.DISABLED ? R.drawable.ic_notifications_off_black_24dp : R.drawable.ic_notifications_black_24dp),
                null,
                notificationIndicator.toString()));
        items.add(new ConfigItem("Time format",
                getDrawable(R.drawable.ic_time_black_24dp),
                new Intent(Settings.ACTION_DATE_SETTINGS)));
        return items;
    }
}