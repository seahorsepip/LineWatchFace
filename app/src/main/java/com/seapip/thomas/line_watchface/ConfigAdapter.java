package com.seapip.thomas.line_watchface;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ConfigAdapter extends WearableRecyclerView.Adapter<ConfigAdapter.ViewHolder> {

    private ArrayList<ConfigItem> data;
    private Context context;
    private ItemSelectedListener itemSelectedListener;

    public ConfigAdapter(Context context, ArrayList<ConfigItem> data) {
        this.context = context;
        this.data = data;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;
        private ImageView imageView;
        private TextView valueView;

        ViewHolder(View view) {
            super(view);
            textView = (TextView) view.findViewById(R.id.text_item);
            imageView = (ImageView) view.findViewById(R.id.image_item);
            valueView = (TextView) view.findViewById(R.id.value_item);

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

    public void update(ArrayList<ConfigItem> modelList){
        data.clear();
        for (ConfigItem model: modelList) {
            data.add(model);
        }
        notifyDataSetChanged();
    }

    @Override
    public ConfigAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.config_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ConfigAdapter.ViewHolder holder, final int position) {
        if (data != null && !data.isEmpty()) {
            float radius = 20 * context.getResources().getDisplayMetrics().density;
            Drawable drawable = new ConfigDrawable(radius, data.get(position).getImage());
            holder.textView.setText(data.get(position).getTitle());
            holder.imageView.setImageDrawable(drawable);
            String val = data.get(position).getValue();
            if (val != null) {
                holder.valueView.setText(val);
                holder.valueView.setVisibility(View.VISIBLE);
            } else {
                holder.valueView.setVisibility(View.GONE);
            }
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