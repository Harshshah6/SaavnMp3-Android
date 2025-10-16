package com.harsh.shah.saavnmp3.adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity;
import com.harsh.shah.saavnmp3.databinding.DownloadManagerMoreViewBinding;
import com.harsh.shah.saavnmp3.utils.TrackDownloader;

import java.util.List;

public class ActivityDownloadManagerListAdapter extends RecyclerView.Adapter<ActivityDownloadManagerListAdapter.ViewHolder> {

    private final List<TrackDownloader.DownloadedTrack> data;

    public ActivityDownloadManagerListAdapter(List<TrackDownloader.DownloadedTrack> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ActivityDownloadManagerListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View _v = View.inflate(parent.getContext(), viewType == 0 ? R.layout.download_manager_list_item : R.layout.activity_list_shimmer, null);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        _v.setLayoutParams(layoutParams);
        return new ActivityDownloadManagerListAdapter.ViewHolder(_v);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityDownloadManagerListAdapter.ViewHolder holder, int position) {
        if (getItemViewType(position) == 1) {
            ((ShimmerFrameLayout) holder.itemView.findViewById(R.id.shimmer)).startShimmer();
            return;
        }

        holder.itemView.findViewById(R.id.title).setSelected(true);
        holder.itemView.findViewById(R.id.artist).setSelected(true);

        TrackDownloader.DownloadedTrack item = data.get(position);

        ((TextView) holder.itemView.findViewById(R.id.title)).setText(item.title());
        ((TextView) holder.itemView.findViewById(R.id.artist)).setText(item.artist());

        if(item.coverImage()!=null){
            ((ImageView) holder.itemView.findViewById(R.id.coverImage)).setImageBitmap(item.coverImage());
        }

        holder.itemView.setOnClickListener(view -> {
            showDialog(item, view);
        });
    }

    private void showDialog(TrackDownloader.DownloadedTrack track, View view){
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(view.getContext(), R.style.MyBottomSheetDialogTheme);
        final DownloadManagerMoreViewBinding _binding = DownloadManagerMoreViewBinding.inflate(((Activity)view.getContext()).getLayoutInflater());
        _binding.songTitle.setText(track.title());
        _binding.songSubTitle.setText(track.artist());
        _binding.coverImage.setImageBitmap(track.coverImage());
        _binding.albumTitle.setText(track.album());
        _binding.songYear.setText(track.year());
        _binding.bitrate.setText(track.bitrate() + " kbps");
        _binding.duration.setText(track.trackLength() + " Seconds");
        if(track.trackUID() == null || track.trackUID().isEmpty()) _binding.button.setVisibility(View.GONE);
        _binding.button.setOnClickListener(v->{
            v.getContext().startActivity(new Intent(v.getContext(), MusicOverviewActivity.class).putExtra("type", "clear").putExtra("id", track.trackUID()));
            bottomSheetDialog.dismiss();
        });
        bottomSheetDialog.setContentView(_binding.getRoot());
        bottomSheetDialog.show();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (data.get(position).title().equals("<shimmer>")) return 1;
        else return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
