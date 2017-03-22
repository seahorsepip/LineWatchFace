package com.seapip.thomas.line_watchface;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_backgrounds_settings, false);
        SettingsPreferenceFragment settingsPreferenceFragment = new SettingsPreferenceFragment();
        Bundle bundle = getIntent().getExtras();
        settingsPreferenceFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsPreferenceFragment).commit();
    }

    public static class SettingsPreferenceFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        final private int BACKGROUND_COLOR_REQUEST = 10;
        int resource;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            resource = getArguments().getInt("resource");
            resource = resource == 0 ? R.xml.pref_settings : resource;
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(resource);
            updateAll(getPreferenceScreen());
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            PreferenceManager.setDefaultValues(getActivity(), resource, false);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            Bundle extras = preference.getExtras();
            Intent intent;
            switch (preference.getKey()) {
                case "settings_top_complication":
                case "settings_left_complication":
                case "settings_right_complication":
                case "settings_background_complication":
                    int id = extras.getInt("id");
                    Log.d("LINE", String.valueOf(id));
                    startActivityForResult(
                            ComplicationHelperActivity.createProviderChooserHelperIntent(
                                    getContext(),
                                    new ComponentName(getContext().getApplicationContext(),
                                            WatchFaceService.class),
                                    id,
                                    WatchFaceService.COMPLICATION_SUPPORTED_TYPES[id]), id);
                    break;
                case "background_settings_screen":
                    intent = new Intent(getContext(), SettingsActivity.class);
                    intent.putExtra("resource", R.xml.pref_backgrounds_settings);
                    startActivity(intent);
                    break;
                case "settings_background_color_name":
                    intent = new Intent(getContext(), ColorActivity.class);
                    intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_background_color_value", Color.BLACK));
                    startActivityForResult(intent, BACKGROUND_COLOR_REQUEST);
                    break;
                case "time_format":
                    startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
                    break;
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Log.d("LINE", String.valueOf(requestCode));
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        ComplicationProviderInfo complicationProviderInfo = data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
                        break;
                    case BACKGROUND_COLOR_REQUEST:
                        Log.d("LINE", "oops!");
                        SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
                        String colorName = data.getStringExtra("color_name");
                        int colorValue = data.getIntExtra("color_value", 0);
                        editor.putString("settings_background_color_name", colorName);
                        editor.putInt("settings_background_color_value", colorValue);
                        editor.apply();
                        setSummary("settings_background_color_name");
                        break;
                }

            }
        }

        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        private void updateAll(PreferenceGroup preferenceGroup) {
            for (int x = 0; x < preferenceGroup.getPreferenceCount(); x++) {
                Preference preference = preferenceGroup.getPreference(x);
                Drawable icon = preference.getIcon();
                if (icon != null) {
                    setStyleIcon(preference, icon);
                }
                onSharedPreferenceChanged(getPreferenceScreen().getSharedPreferences(), preference.getKey());
            }
        }

        private void setStyleIcon(Preference preference, Drawable icon) {
            LayerDrawable layerDrawable = (LayerDrawable) getContext().getDrawable(R.drawable.config_icon);
            icon.setTint(Color.WHITE);
            if (layerDrawable.setDrawableByLayerId(R.id.nested_icon, icon)) {
                preference.setIcon(layerDrawable);
            }
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference preference = findPreference(key);
            Bundle extras = preference.getExtras();
            if (preference instanceof MultiSelectListPreference) {
                Set<String> values = sharedPreferences.getStringSet(key, null);
                ArrayList<CharSequence> items = new ArrayList<>();
                CharSequence[] entries = ((MultiSelectListPreference) preference).getEntries();
                CharSequence[] entryValues = ((MultiSelectListPreference) preference).getEntryValues();
                for (int x = 0; x < entries.length; x++) {
                    if (values.contains(entryValues[x].toString())) {
                        items.add(entries[x]);
                    }
                }
                String delimiter = items.size() == 2 ? " & " : ", ";
                String summary = items.size() > 0 ? TextUtils.join(delimiter, items) : "None";
                preference.setSummary(summary);
            } else if (preference instanceof ListPreference) {
                String name = extras.getString("icons");
                if (name != null) {
                    String value = sharedPreferences.getString(key, null);
                    int id = getResources().getIdentifier(name, "array", getActivity().getPackageName());
                    TypedArray icons = getResources().obtainTypedArray(id);
                    CharSequence[] entryValues = ((ListPreference) preference).getEntryValues();
                    for (int x = 0; x < entryValues.length; x++) {
                        if (value.equals(entryValues[x])) {
                            setStyleIcon(preference, getResources().getDrawable(icons.getResourceId(x, 0)));
                        }
                    }
                    icons.recycle();
                }
            } else if (preference.getSummary() != null && preference.getSummary().equals("%s")) {
                setSummary(key);
            }
        }

        private void setSummary(String key) {
            Preference preference = findPreference(key);
            PreferenceManager.setDefaultValues(getActivity(), resource, false);
            String value = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(key, "uhh?");
            preference.setSummary(value);
        }
    }
}
