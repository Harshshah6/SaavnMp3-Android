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
import com.harsh.shah.saavnmp3.records.SongResponse;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ActivityArtistProfileTopSongsAdapter extends RecyclerView.Adapter<ActivityArtistProfileTopSongsAdapter.ViewHolder> {

    private final List<SongResponse.Song> data;

    public ActivityArtistProfileTopSongsAdapter(List<SongResponse.Song> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View _v = View.inflate(parent.getContext(), viewType == 1 ? R.layout.activity_artist_profile_view_top_songs_item : R.layout.artist_profile_view_top_songs_shimmer, null);
        _v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(_v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            ((ShimmerFrameLayout) holder.itemView.findViewById(R.id.shimmer)).startShimmer();
            return;
        }

        ImageView coverImage = holder.itemView.findViewById(R.id.coverImage);
        TextView coverTitle = holder.itemView.findViewById(R.id.coverTitle);
        TextView coverPlayed = holder.itemView.findViewById(R.id.coverPlayed);
        TextView positionTextView = holder.itemView.findViewById(R.id.position);
        ImageView moreImage = holder.itemView.findViewById(R.id.more);

        positionTextView.setText(String.valueOf(position + 1));
        coverTitle.setText(data.get(position).name());
        coverPlayed.setText(
                String.format("%s | %s", data.get(position).year(), data.get(position).label())
        );
        Picasso.get().load(Uri.parse(data.get(position).image().get(data.get(position).image().size() - 1).url())).into(coverImage);

        holder.itemView.setOnClickListener(view -> {
            view.getContext().startActivity(new Intent(view.getContext(), MusicOverviewActivity.class).putExtra("id", data.get(position).id()));
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (data.get(position).id().equals("<shimmer>")) return 0;
        else return 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
