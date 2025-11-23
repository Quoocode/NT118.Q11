package com.example.habittracker.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// Thay com.example.crubfirebase bằng package chứa file R của bạn
import com.example.habittracker.R;

import java.util.List;

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {

    private Context context;
    private List<String> iconNames; // Danh sách tên file (VD: "ic_book")
    private String selectedIconName; // Icon đang được chọn
    private OnIconSelectedListener listener;

    public interface OnIconSelectedListener {
        void onIconSelected(String iconName);
    }

    public IconAdapter(Context context, List<String> iconNames, String currentIcon, OnIconSelectedListener listener) {
        this.context = context;
        this.iconNames = iconNames;
        this.selectedIconName = currentIcon;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_icon_selection.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_icon_selection, parent, false);
        return new IconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        String iconName = iconNames.get(position);

        // 1. Load hình ảnh từ tên file drawable
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        if (resId != 0) {
            holder.imgIcon.setImageResource(resId);
        } else {
            holder.imgIcon.setImageResource(android.R.drawable.ic_menu_help);
        }

        // 2. Xử lý hiệu ứng Chọn (TỐI ƯU HÓA)
        // Không cần setBackgroundResource thủ công nữa.
        // Chỉ cần set trạng thái Selected = true/false.
        // File XML (bg_icon_selector) sẽ tự động đổi viền dựa trên trạng thái này.
        if (iconName.equals(selectedIconName)) {
            holder.container.setSelected(true);
        } else {
            holder.container.setSelected(false);
        }

        // 3. Sự kiện Click
        holder.itemView.setOnClickListener(v -> {
            // Cập nhật biến selected
            selectedIconName = iconName;
            // Báo cho Activity biết
            if (listener != null) {
                listener.onIconSelected(iconName);
            }
            // Refresh lại giao diện để cập nhật viền xanh
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return iconNames.size();
    }

    // Hàm để set icon ban đầu khi mở màn hình Edit
    public void setSelectedIcon(String iconName) {
        this.selectedIconName = iconName;
        notifyDataSetChanged();
    }

    public static class IconViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        FrameLayout container;

        public IconViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_icon_item);
            container = itemView.findViewById(R.id.container_icon);
        }
    }
}