package com.seapip.thomas.line_watchface;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Dimension;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.view.WearableRecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

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
    }

    @Override
    protected void onActivityResult(int position, int resultCode, Intent data) {
        View item = mWearableRecyclerView.getLayoutManager().findViewByPosition(position).findViewById(R.id.text_wrapper);
        TextView text = (TextView) item.findViewById(R.id.value_item);
        ImageView image = (ImageView) mWearableRecyclerView.getLayoutManager().findViewByPosition(position).findViewById(R.id.image_item);
        String value = text.getText().toString();
        SharedPreferences.Editor mEditor = mPrefs.edit();
        if (resultCode == RESULT_OK) {
            Log.d("LINE", String.valueOf(position));
            switch (position) {
                case 0:
                case 1:
                    ComplicationProviderInfo complicationProviderInfo =
                            data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);

                    value = "Empty";
                    if (complicationProviderInfo != null) {
                        value = complicationProviderInfo.providerName;
                    }

                    mEditor.putString("setting_complication_" + String.valueOf(position), value).commit();
                    break;
                case 2:
                    value = mPrefs.getString("setting_color_name", "Cyan");
                    break;
                case 3:
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
        items.add(new ConfigurationItemModel("Left \ncomplication",
                getDrawable(R.drawable.ic_left_complication),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[0],
                        LineWatchFaceService.COMPLICATION_SUPPORTED_TYPES[1]),
                mPrefs.getString("setting_complication_0", "Empty")));
        items.add(new ConfigurationItemModel("Right \ncomplication",
                getDrawable(R.drawable.ic_right_complication),
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        watchFace,
                        complicationIds[1],
                        LineWatchFaceService.COMPLICATION_SUPPORTED_TYPES[1]),
                mPrefs.getString("setting_complication_1", "Empty")));
        items.add(new ConfigurationItemModel("Color",
                getDrawable(R.drawable.ic_color),
                new Intent(LineWatchFaceConfigActivity.this, LineWatchFaceConfigColorActivity.class),
                mPrefs.getString("setting_color_name", "Cyan")));
        NotificationIndicator notificationIndicator = NotificationIndicator.fromValue(mPrefs.getInt("setting_notification_indicator", NotificationIndicator.NONE.getValue()));
        items.add(new ConfigurationItemModel("Notification indicator",
                getDrawable(notificationIndicator == NotificationIndicator.NONE ? R.drawable.ic_notification_disabled: R.drawable.ic_notification),
                null,
                notificationIndicator.toString()));
        //TODO: Add background setting
        /*
        items.add(new ConfigurationItemModel("Background",
                getDrawable(R.drawable.ic_background),
                null,
                3,
                mPrefs.getString("setting_background", "Black")));
                */
        icons.recycle();
        return items;
    }
}