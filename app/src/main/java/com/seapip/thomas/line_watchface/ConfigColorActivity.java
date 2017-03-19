package com.seapip.thomas.line_watchface;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableRecyclerView;

import org.jraf.android.androidwearcolorpicker.app.ColorPickActivity;

import java.util.ArrayList;

/**
 * The watch-side config activity for {@link WatchFaceService}, which
 * allows for setting complications on the left and right of watch face.
 */
public class ConfigColorActivity extends WearableActivity implements ConfigAdapter.ItemSelectedListener {

    private static final int PROVIDER_CHOOSER_REQUEST_CODE = 1;

    private ConfigAdapter mAdapter;
    private SharedPreferences mPrefs;
    private WearableRecyclerView mWearableRecyclerView;

    private String[] color_names;
    private int[] color_values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        color_names = getResources().getStringArray(R.array.line_watch_face_color_names);
        TypedArray color_values_typed = getResources().obtainTypedArray(R.array.line_watch_face_color_values);
        color_values = new int[color_names.length];
        for (int i = 0; i < color_names.length; i++) {
            color_values[i] = color_values_typed.getColor(i, 0);
        }
        color_values_typed.recycle();

        mAdapter = new ConfigAdapter(getApplicationContext(), getConfigurationItems());
        mAdapter.setListener(this);
        mWearableRecyclerView = (WearableRecyclerView) findViewById(R.id.recycler_launcher_view);
        mWearableRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public void onItemSelected(int position) {
        if (position > 0) {
            SharedPreferences.Editor mEditor = mPrefs.edit();
            mEditor.putString("setting_color_name", color_names[position - 1]);
            mEditor.putInt("setting_color_value", color_values[position - 1]);
            mEditor.commit();
            setResult(Activity.RESULT_OK, new Intent());
            finish();
        } else {
            Intent intent = new ColorPickActivity.IntentBuilder().oldColor(mPrefs.getInt("setting_color_value", Color.parseColor("#18FFFF"))).build(this);
            startActivityForResult(intent, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            SharedPreferences.Editor mEditor = mPrefs.edit();
            mEditor.putString("setting_color_name", "Custom");
            mEditor.putInt("setting_color_value", ColorPickActivity.getPickedColor(data));
            mEditor.commit();
            setResult(Activity.RESULT_OK, new Intent());
            finish();
        }
    }

    private ArrayList<ConfigItem> getConfigurationItems() {
        float radius = 20 * getResources().getDisplayMetrics().density;
        ArrayList<ConfigItem> items = new ArrayList<>();
        items.add(new ConfigItem(
                "Custom",
                null,
                new ConfigDrawable(radius, getDrawable(R.drawable.ic_colorize_black_24dp)),
                null
        ));
        for (int i = 0; i < color_names.length; i++) {
            items.add(new ConfigItem(
                    color_names[i],
                    null,
                    new ConfigDrawable(radius, color_values[i]),
                    null
            ));
        }

        return items;
    }
}