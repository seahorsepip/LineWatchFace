package com.seapip.thomas.line_watchface;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ConfigurationAdapter extends WearableListView.Adapter {
    private ArrayList<ConfigurationItemModel> mItems;
    private final LayoutInflater mInflater;

    public ConfigurationAdapter(Context context, ArrayList<ConfigurationItemModel> items) {
        mInflater = LayoutInflater.from(context);
        mItems = items;
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(
            ViewGroup viewGroup, int i) {
        return new ItemViewHolder(mInflater.inflate(R.layout.activity_line_watch_face_list_item, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder viewHolder,
                                 int position) {
        ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
        CircledImageView circledView = itemViewHolder.mCircledImageView;
        circledView.setImageDrawable(mItems.get(position).getImage());
        TextView textView = itemViewHolder.mItemTextView;
        textView.setText(mItems.get(position).getTitle());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private static class ItemViewHolder extends WearableListView.ViewHolder {
        private CircledImageView mCircledImageView;
        private TextView mItemTextView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mCircledImageView = (CircledImageView)
                    itemView.findViewById(R.id.circle);
            mItemTextView = (TextView) itemView.findViewById(R.id.name);
        }
    }
}