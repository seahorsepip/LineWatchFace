package com.seapip.thomas.line_watchface;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.wearable.view.WearableRecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * The watch-side config activity for {@link LineWatchFaceService}, which
 * allows for setting complications on the left and right of watch face.
 */
public class LineWatchFaceConfigActivity extends WearableActivity implements ConfigurationAdapter.ItemSelectedListener {
    private ConfigurationAdapter mAdapter;
    private SharedPreferences mPrefs;
    private WearableRecyclerView mWearableRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_watch_face_config);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mAdapter = new ConfigurationAdapter(getApplicationContext(), getConfigurationItems());
        mAdapter.setListener(this);
        mWearableRecyclerView = (WearableRecyclerView) findViewById(R.id.recycler_launcher_view);
        mWearableRecyclerView.setAdapter(mAdapter);

        Executor executor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
            }
        };

        ProviderInfoRetriever.OnProviderInfoReceivedCallback callback = new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
            @Override
            public void onProviderInfoReceived(int i, @Nullable ComplicationProviderInfo complicationProviderInfo) {
                Log.d("LINE", complicationProviderInfo.providerName);
            }
        };

        ProviderInfoRetriever providerInfoRetriever = new ProviderInfoRetriever(getApplicationContext(), executor);

        providerInfoRetriever.init();
        providerInfoRetriever.retrieveProviderInfo(callback,
                new ComponentName(
                        getApplicationContext(),
                        LineWatchFaceService.class)
                , LineWatchFaceService.COMPLICATION_IDS);
    }

    @Override
    protected void onActivityResult(int position, int resultCode, Intent data) {
        View item = mWearableRecyclerView.getLayoutManager().findViewByPosition(position).findViewById(R.id.text_wrapper);
        TextView text = (TextView) item.findViewById(R.id.value_item);
        ImageView image = (ImageView) mWearableRecyclerView.getLayoutManager().findViewByPosition(position).findViewById(R.id.image_item);
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

                    mEditor.putString("setting_complication_" + String.valueOf(position), value).commit();
                    break;
                case 3:
                    value = mPrefs.getString("setting_color_name", "Cyan");
                    break;
                case 5:
                    NotificationIndicator notificationIndicator = NotificationIndicator.fromValue(mPrefs.getInt("setting_notification_indicator", NotificationIndicator.NONE.getValue()));
                    Drawable drawable = getDrawable(R.drawable.ic_notification);
                    switch (notificationIndicator) {
                        case NONE:
                            notificationIndicator = NotificationIndicator.UNREAD;
                            break;
                        case UNREAD:
                            notificationIndicator = NotificationIndicator.ALL;
                            break;
                        case ALL:
                            notificationIndicator = NotificationIndicator.NONE;
                            drawable = getDrawable(R.drawable.ic_notification_disabled);
                            break;
                    }
                    mEditor.putInt("setting_notification_indicator", notificationIndicator.getValue()).commit();
                    value = notificationIndicator.toString();
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
        if(activity != null){
            startActivityForResult(activity, position);
        } else {
            onActivityResult(position, RESULT_OK, null);
        }
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
                mPrefs.getString("setting_complication_0", "Empty")));
        items.add(new ConfigurationItemModel("Left \ncomplication",
                getDrawable(R.drawable.ic_left_complication),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[1],
                        LineWatchFaceService.COMPLICATION_SUPPORTED_TYPES[1]),
                mPrefs.getString("setting_complication_1", "Empty")));
        items.add(new ConfigurationItemModel("Right \ncomplication",
                getDrawable(R.drawable.ic_right_complication),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[2],
                        LineWatchFaceService.COMPLICATION_SUPPORTED_TYPES[2]),
                mPrefs.getString("setting_complication_2", "Empty")));
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
                mPrefs.getString("setting_complication_4", "None")));
        NotificationIndicator notificationIndicator = NotificationIndicator.fromValue(mPrefs.getInt("setting_notification_indicator", NotificationIndicator.NONE.getValue()));
        items.add(new ConfigurationItemModel("Notification indicator",
                getDrawable(notificationIndicator == NotificationIndicator.NONE ? R.drawable.ic_notification_disabled: R.drawable.ic_notification),
                null,
                notificationIndicator.toString()));
        items.add(new ConfigurationItemModel("Time format",
                getDrawable(R.drawable.ic_time_format),
                new Intent(Settings.ACTION_DATE_SETTINGS)));
        icons.recycle();
        return items;
    }
}