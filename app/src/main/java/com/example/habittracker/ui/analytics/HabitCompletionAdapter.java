package com.example.habittracker.ui.analytics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habittracker.R;

public class HabitCompletionAdapter extends ListAdapter<HabitCompletion, HabitCompletionAdapter.ViewHolder> {

    public interface OnHabitClickListener {
        void onHabitClick(@NonNull HabitCompletion habit);
    }

    public interface OnHabitCheckInClickListener {
        void onHabitCheckInClick(@NonNull HabitCompletion habit);
    }

    @Nullable
    private final OnHabitClickListener onHabitClickListener;

    @Nullable
    private final OnHabitCheckInClickListener onHabitCheckInClickListener;

    public HabitCompletionAdapter() {
        this(null, null);
    }

    public HabitCompletionAdapter(@Nullable OnHabitClickListener onHabitClickListener) {
        this(onHabitClickListener, null);
    }

    public HabitCompletionAdapter(@Nullable OnHabitClickListener onHabitClickListener,
                                 @Nullable OnHabitCheckInClickListener onHabitCheckInClickListener) {
        super(DIFF);
        this.onHabitClickListener = onHabitClickListener;
        this.onHabitCheckInClickListener = onHabitCheckInClickListener;
    }

    private static final DiffUtil.ItemCallback<HabitCompletion> DIFF = new DiffUtil.ItemCallback<HabitCompletion>() {
        @Override
        public boolean areItemsTheSame(@NonNull HabitCompletion oldItem, @NonNull HabitCompletion newItem) {
            // Prefer stable habitId when available.
            if (oldItem.getHabitId() != null && newItem.getHabitId() != null) {
                return oldItem.getHabitId().equals(newItem.getHabitId());
            }
            // Fallback to name+status.
            return oldItem.getName().equals(newItem.getName()) && oldItem.getStatus().equals(newItem.getStatus());
        }

        @Override
        public boolean areContentsTheSame(@NonNull HabitCompletion oldItem, @NonNull HabitCompletion newItem) {
            if (oldItem.getHabitId() != null && newItem.getHabitId() != null) {
                return oldItem.getHabitId().equals(newItem.getHabitId())
                        && oldItem.getName().equals(newItem.getName())
                        && oldItem.getStatus().equals(newItem.getStatus());
            }
            return oldItem.getName().equals(newItem.getName()) && oldItem.getStatus().equals(newItem.getStatus());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit_completion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HabitCompletion habit = getItem(position);
        holder.name.setText(habit.getName());
        holder.statusText.setText(habit.getStatus().name());
        bindStatusIcon(habit.getStatus(), holder.statusIcon, holder.pendingMarker);

        holder.itemView.setOnClickListener(v -> {
            if (onHabitClickListener != null) {
                onHabitClickListener.onHabitClick(habit);
            }
        });

        holder.actionButton.setOnClickListener(v -> {
            if (onHabitCheckInClickListener != null) {
                onHabitCheckInClickListener.onHabitCheckInClick(habit);
            }
        });
    }

    private void bindStatusIcon(HabitCompletion.Status status, ImageView icon, TextView pendingMarker) {
        switch (status) {
            case COMPLETED:
                pendingMarker.setVisibility(View.GONE);
                icon.setVisibility(View.VISIBLE);
                icon.setImageResource(R.drawable.ic_circle_check);
                break;
            case MISSED:
                pendingMarker.setVisibility(View.GONE);
                icon.setVisibility(View.VISIBLE);
                icon.setImageResource(R.drawable.ic_close_circle);
                break;
            case PENDING:
            default:
                icon.setVisibility(View.GONE);
                pendingMarker.setVisibility(View.VISIBLE);
                break;
        }
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView statusText;
        final ImageView statusIcon;
        final TextView pendingMarker;
        final ImageView actionButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_habit_name);
            statusText = itemView.findViewById(R.id.tv_habit_status);
            statusIcon = itemView.findViewById(R.id.img_status);
            pendingMarker = itemView.findViewById(R.id.tv_pending_marker);
            actionButton = itemView.findViewById(R.id.img_check);
        }
    }
}
