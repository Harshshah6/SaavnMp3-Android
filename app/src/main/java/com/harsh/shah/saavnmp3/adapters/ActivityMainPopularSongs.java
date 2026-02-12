package com.harsh.shah.saavnmp3.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity;
import com.harsh.shah.saavnmp3.model.AlbumItem;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ActivityMainPopularSongs extends RecyclerView.Adapter<ActivityMainPopularSongs.ViewHolder> {

    private final List<AlbumItem> data;

    public ActivityMainPopularSongs(List<AlbumItem> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View _v = View.inflate(parent.getContext(),
                viewType == 0 ? R.layout.activity_main_songs_item : R.layout.songs_item_shimmer, null);
        _v.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(_v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == 1) {
            ((ShimmerFrameLayout) holder.itemView.findViewById(R.id.shimmer)).startShimmer();
            return;
        }

        ((TextView) holder.itemView.findViewById(R.id.albumTitle)).setText(data.get(position).albumTitle());
        ((TextView) holder.itemView.findViewById(R.id.albumSubTitle)).setText(data.get(position).albumSubTitle());

        holder.itemView.findViewById(R.id.albumTitle).setSelected(true);
        holder.itemView.findViewById(R.id.albumSubTitle).setSelected(true);

        ImageView coverImage = holder.itemView.findViewById(R.id.coverImage);
        Picasso.get().load(Uri.parse(data.get(position).albumCover())).into(coverImage);

        holder.itemView.setOnClickListener(v -> {
            com.harsh.shah.saavnmp3.BaseApplicationClass.trackQueue.clear();
            android.util.Log.d("AdapterDebug",
                    "Click at pos: " + position + ". Populating queue with " + data.size() + " items.");
            for (int i = 0; i < data.size(); i++) {
                String id = data.get(i).id();
                android.util.Log.d("AdapterDebug", "Queue[" + i + "]: " + id + " - " + data.get(i).albumTitle());
                com.harsh.shah.saavnmp3.BaseApplicationClass.trackQueue.add(id);
            }
            com.harsh.shah.saavnmp3.BaseApplicationClass.track_position = position;

            v.getContext().startActivity(
                    new Intent(v.getContext(), MusicOverviewActivity.class).putExtra("id", data.get(position).id()));
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (data.get(position).albumTitle().equals("<shimmer>"))
            return 1;
        else
            return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
