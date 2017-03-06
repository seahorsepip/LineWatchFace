package com.seapip.thomas.line_watchface;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.view.DefaultOffsettingHelper;
import android.support.wearable.view.WearableRecyclerView;

import java.util.ArrayList;

/**
 * The watch-side config activity for {@link LineWatchFaceService}, which
 * allows for setting complications on the left and right of watch face.
 */
public class LineWatchFaceConfigActivity extends WearableActivity implements ConfigurationAdapter.ItemSelectedListener {

    private static final int PROVIDER_CHOOSER_REQUEST_CODE = 1;

    private ConfigurationAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_watch_face_config);

        mAdapter = new ConfigurationAdapter(getApplicationContext(), getConfigurationItems());
        WearableRecyclerView wearableRecyclerView = (WearableRecyclerView) findViewById(R.id.recycler_launcher_view);
        wearableRecyclerView.setHasFixedSize(true);

        DefaultOffsettingHelper offsettingHelper = new DefaultOffsettingHelper();

        wearableRecyclerView.setCenterEdgeItems(true);
        wearableRecyclerView.setOffsettingHelper(offsettingHelper);
        mAdapter.setListener(this);
        wearableRecyclerView.setAdapter(mAdapter);
        //mWearableConfigListView.setClickListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PROVIDER_CHOOSER_REQUEST_CODE
                && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);

            finish();
        }
    }

    @Override
    public void onItemSelected(int position) {
        startActivity(getConfigurationItems().get(position).getActivity());
    }

    /*
    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        Integer tag = (Integer) viewHolder.itemView.getTag();
        ComplicationItem complicationItem = mAdapter.getItem(tag);

        // Note: If you were previously using ProviderChooserIntent.createProviderChooserIntent()
        // (now deprecated), you will want to switch to
        // ComplicationHelperActivity.createProviderChooserHelperIntent()
        startActivityForResult(
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        getApplicationContext(),
                        complicationItem.watchFace,
                        complicationItem.complicationId,
                        complicationItem.supportedTypes),
                PROVIDER_CHOOSER_REQUEST_CODE);
    }
    */

    private ArrayList<ConfigurationItemModel> getConfigurationItems() {
        ComponentName watchFace = new ComponentName(
                getApplicationContext(), LineWatchFaceService.class);

        String[] complicationNames =
                getResources().getStringArray(R.array.line_watch_face_names);

        int[] complicationIds = LineWatchFaceService.COMPLICATION_IDS;

        TypedArray icons = getResources().obtainTypedArray(R.array.line_watch_face_icons);

        ArrayList<ConfigurationItemModel> items = new ArrayList<>();
        for (int i = 0; i < complicationIds.length; i++) {
            Intent intent = ComplicationHelperActivity.createProviderChooserHelperIntent(
                    getApplicationContext(),
                    watchFace,
                    complicationIds[i],
                    LineWatchFaceService.COMPLICATION_SUPPORTED_TYPES[i]);
            items.add(new ConfigurationItemModel(complicationNames[i],
                    icons.getDrawable(i),
                    intent));
        }
        return items;
    }

    /*
     * Inner class representing items of the ConfigurationAdapter (WearableListView.Adapter) class.
     */
    private final class ComplicationItem {
        ComponentName watchFace;
        int complicationId;
        int[] supportedTypes;
        Drawable icon;
        String title;

        public ComplicationItem(ComponentName watchFace, int complicationId, int[] supportedTypes,
                                Drawable icon, String title) {
            this.watchFace = watchFace;
            this.complicationId = complicationId;
            this.supportedTypes = supportedTypes;
            this.icon = icon;
            this.title = title;
        }
    }

    /*
    private static class ConfigurationAdapter extends WearableListView.Adapter {

        private Context mContext;
        private final LayoutInflater mInflater;
        private List<ComplicationItem> mItems;


        public ConfigurationAdapter (Context context, List<ComplicationItem> items) {
            mContext = context;
            mInflater = LayoutInflater.from(mContext);
            mItems = items;
        }

        // Provides a reference to the type of views you're using
        public static class ItemViewHolder extends WearableListView.ViewHolder {
            private ImageView iconImageView;
            private TextView textView;
            public ItemViewHolder(View itemView) {
                super(itemView);
                iconImageView = (ImageView) itemView.findViewById(R.id.icon);
                textView = (TextView) itemView.findViewById(R.id.name);
            }
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            // Inflate custom layout for list items.
            return new ItemViewHolder(
                    mInflater.inflate(R.layout.activity_line_watch_face_list_item, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {

            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            ImageView imageView = itemHolder.iconImageView;
            imageView.setImageDrawable(mItems.get(position).icon);

            TextView textView = itemHolder.textView;
            textView.setText(mItems.get(position).title);

            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public ComplicationItem getItem(int position) {
            return mItems.get(position);
        }
    }
    */
}