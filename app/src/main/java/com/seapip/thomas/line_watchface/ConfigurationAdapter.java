package com.seapip.thomas.line_watchface;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ConfigurationAdapter extends WearableRecyclerView.Adapter<ConfigurationAdapter.ViewHolder> {

    private ArrayList<ConfigurationItemModel> data;
    private Context context;
    private ItemSelectedListener itemSelectedListener;

    public ConfigurationAdapter(Context context, ArrayList<ConfigurationItemModel> data) {
        this.context = context;
        this.data = data;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;
        private ImageView imageView;

        ViewHolder(View view) {
            super(view);
            textView = (TextView) view.findViewById(R.id.text_item);
            imageView = (ImageView) view.findViewById(R.id.item_image);
        }

        void bind(final int position, final ItemSelectedListener listener) {

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemSelected(position);
                    }
                }
            });
        }
    }

    public void setListener(ItemSelectedListener itemSelectedListener) {
        this.itemSelectedListener = itemSelectedListener;
    }

    @Override
    public ConfigurationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_curved_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(ConfigurationAdapter.ViewHolder holder, final int position) {
        if (data != null && !data.isEmpty()) {
            holder.textView.setText(data.get(position).getTitle());
            holder.imageView.setImageDrawable(data.get(position).getImage());
            holder.bind(position, itemSelectedListener);
        }
    }

    @Override
    public int getItemCount() {
        if (data != null && !data.isEmpty()) {
            return data.size();
        }
        return 0;
    }

    public interface ItemSelectedListener {
        void onItemSelected(int position);
    }
}