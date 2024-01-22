package com.example.medifiles;

import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoViewHolder> {

    private List<String> videoList;
    private List<String> videoTitleList;
    private OnVideoClickListener onVideoClickListener;

    public interface OnVideoClickListener {
        void onVideoClick(String videoUrl, String videoTitle);
    }

    public VideoListAdapter(List<String> videoList, OnVideoClickListener onVideoClickListener) {
        this.videoList = videoList;
        this.videoTitleList = new ArrayList<>();
        this.onVideoClickListener = onVideoClickListener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        String videoUrl = videoList.get(position);

        // 고정된 play 모양 이미지 설정
        holder.videoThumbnail.setImageResource(R.drawable.play);

        String videoTitle = videoTitleList.get(position);
        holder.videoTitle.setText(videoTitle);

        holder.cardView.setOnClickListener(v -> {
            if (onVideoClickListener != null) {
                onVideoClickListener.onVideoClick(videoUrl, videoTitle);
            }
        });

        // CardView의 상태에 따라 배경색 변경
        holder.cardView.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 손가락이 눌린 상태
                    holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.purple_200));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // 손가락이 떼진 상태
                    holder.cardView.setCardBackgroundColor(Color.WHITE);
                    break;
            }
            return false;
        });
    }


    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public void addVideoTitle(String videoTitle) {
        videoTitleList.add(videoTitle);
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView videoThumbnail;
        TextView videoTitle;
        CardView cardView;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);

            videoThumbnail = itemView.findViewById(R.id.videoThumbnail);
            videoTitle = itemView.findViewById(R.id.videoTitle);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
