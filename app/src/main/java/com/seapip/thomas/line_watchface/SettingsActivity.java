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
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Executor;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_background_settings, false);
        SettingsPreferenceFragment settingsPreferenceFragment = new SettingsPreferenceFragment();
        Bundle bundle = getIntent().getExtras();
        settingsPreferenceFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsPreferenceFragment).commit();
    }

    public static class SettingsPreferenceFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        final private int COLOR_REQUEST = 10;
        final private int BACKGROUND_COLOR_REQUEST = 11;

        int resource;
        ProviderInfoRetriever providerInfoRetriever;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            resource = getArguments().getInt("resource");
            resource = resource == 0 ? R.xml.pref_settings : resource;
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(resource);
            updateAll(getPreferenceScreen());
        }

        @Override
        public void addPreferencesFromResource(@XmlRes int preferencesResId) {
            super.addPreferencesFromResource(preferencesResId);

            Executor executor = new Executor() {
                @Override
                public void execute(@NonNull Runnable r) {
                    new Thread(r).start();
                }
            };

            ProviderInfoRetriever.OnProviderInfoReceivedCallback callback = new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                @Override
                public void onProviderInfoReceived(int i, @Nullable ComplicationProviderInfo complicationProviderInfo) {
                    setComplicationSummary(i, complicationProviderInfo);
                }
            };


            providerInfoRetriever = new ProviderInfoRetriever(getContext(), executor);

            providerInfoRetriever.init();
            providerInfoRetriever.retrieveProviderInfo(callback,
                    new ComponentName(
                            getContext(),
                            WatchFaceService.class),
                    WatchFaceService.COMPLICATION_IDS);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            providerInfoRetriever.release();
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
                case "settings_bottom_complication":
                case "settings_background_complication":
                    int id = extras.getInt("id");
                    startActivityForResult(
                            ComplicationHelperActivity.createProviderChooserHelperIntent(
                                    getContext(),
                                    new ComponentName(getContext().getApplicationContext(),
                                            WatchFaceService.class),
                                    id,
                                    WatchFaceService.COMPLICATION_SUPPORTED_TYPES[id]), id);
                    break;
                case "settings_color_name":
                    intent = new Intent(getContext(), ColorActivity.class);
                    intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_color_value", Color.parseColor("#18FFFF")));
                    intent.putExtra("color_names_id", R.array.color_names);
                    intent.putExtra("color_values_id", R.array.color_values);
                    startActivityForResult(intent, COLOR_REQUEST);
                    break;
                case "complication_settings_screen":
                    intent = new Intent(getContext(), SettingsActivity.class);
                    intent.putExtra("resource", R.xml.pref_complication_settings);
                    startActivity(intent);
                    break;
                case "background_settings_screen":
                    intent = new Intent(getContext(), SettingsActivity.class);
                    intent.putExtra("resource", R.xml.pref_background_settings);
                    startActivity(intent);
                    break;
                case "settings_background_color_name":
                    intent = new Intent(getContext(), ColorActivity.class);
                    intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_background_color_value", Color.BLACK));
                    intent.putExtra("color_names_id", R.array.background_color_names);
                    intent.putExtra("color_values_id", R.array.background_color_values);
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
            SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        setComplicationSummary(requestCode, (ComplicationProviderInfo) data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO));
                        break;
                    case COLOR_REQUEST:
                        editor.putString("settings_color_name", data.getStringExtra("color_name"));
                        editor.putInt("settings_color_value", data.getIntExtra("color_value", 0));
                        editor.apply();
                        setSummary("settings_color_name");
                        break;
                    case BACKGROUND_COLOR_REQUEST:
                        editor.putString("settings_background_color_name", data.getStringExtra("color_name"));
                        editor.putInt("settings_background_color_value", data.getIntExtra("color_value", 0));
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
            if (preference != null) {
                PreferenceManager.setDefaultValues(getContext(), resource, false);
                String value = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(key, null);
                preference.setSummary(value);
            }
        }

        private void setComplicationSummary(int id, ComplicationProviderInfo providerInfo) {
            String key;
            switch (id) {
                case 0:
                    key = "settings_top_complication";
                    break;
                case 1:
                    key = "settings_left_complication";
                    break;
                case 2:
                    key = "settings_right_complication";
                    break;
                case 3:
                    key = "settings_bottom_complication";
                    break;
                case 4:
                    key = "settings_background_complication";
                    break;
                default:
                    return;
            }
            Preference preference = findPreference(key);
            if (preference != null) {
                String providerName = providerInfo != null ? providerInfo.providerName : "Empty";
                preference.setSummary(providerName);
            }
        }
    }
}
