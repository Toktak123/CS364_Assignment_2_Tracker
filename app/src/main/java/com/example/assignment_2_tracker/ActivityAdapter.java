package com.example.assignment_2_tracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);

    public interface OnItemClickListener { void onItemClick(ActivityItem item); }

    private final List<ActivityItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public ActivityAdapter(OnItemClickListener listener) { this.listener = listener; }

    public void setData(List<ActivityItem> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }

            @Override public boolean areItemsTheSame(int o, int n) {
                return items.get(o).getId() == newItems.get(n).getId();
            }

            @Override public boolean areContentsTheSame(int o, int n) {
                ActivityItem a = items.get(o), b = newItems.get(n);
                return a.getSteps() == b.getSteps()
                        && a.getShakes() == b.getShakes()
                        && a.getDurationSec() == b.getDurationSec()
                        && a.getStepRecord() == b.getStepRecord()
                        && a.getShakeRecord() == b.getShakeRecord();
            }
        });

        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
        notifyItemRangeChanged(0, items.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textIndex, title, info, summary, textStepRecord, textShakeRecord;

        final View cardShakeRecord, cardStepRecord;
        ViewHolder(View itemView) {
            super(itemView);
            textIndex = itemView.findViewById(R.id.text_item_activity_index);
            title = itemView.findViewById(R.id.text_item_activity_title);
            info = itemView.findViewById(R.id.text_item_activity_info);
            summary = itemView.findViewById(R.id.text_item_activity_summary);
            cardStepRecord = itemView.findViewById(R.id.card_item_activity_new_step_record);
            textStepRecord = itemView.findViewById(R.id.text_item_activity_new_step_record);
            cardShakeRecord = itemView.findViewById(R.id.card_item_activity_new_shake_record);
            textShakeRecord = itemView.findViewById(R.id.text_item_activity_new_shake_record);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(v) { };    // คลาสนิรนาม ไม่ต้องตั้งชื่อ
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ActivityItem item = items.get(position);
        Context ctx = h.itemView.getContext();

        h.textIndex.setText(String.valueOf( position + 1));
        h.title.setText(dateFormat.format(new Date(item.getStartTime())));
        h.info.setText(ctx.getString(R.string.item_info_format,
                item.getSteps(), ctx.getString(R.string.steps),
                item.getShakes(), ctx.getString(R.string.shakes)));

        int sec = item.getDurationSec();
        String duration = String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60);
        h.summary.setText(ctx.getString(R.string.item_summary_format,
                duration,
                item.getStepsPerMin(),  ctx.getString(R.string.steps_per_min),
                item.getShakesPerMin(), ctx.getString(R.string.shakes_per_min)));


        h.cardStepRecord.setVisibility(item.getStepRecord() ? View.VISIBLE : View.GONE);
        h.cardShakeRecord.setVisibility(item.getShakeRecord() ? View.VISIBLE : View.GONE);

        h.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }



    @Override public int getItemCount() { return items.size(); }


}