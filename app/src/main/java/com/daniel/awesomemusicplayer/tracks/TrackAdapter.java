package com.daniel.awesomemusicplayer.tracks;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.daniel.awesomemusicplayer.R;
import com.daniel.awesomemusicplayer.util.Utils;

import java.util.List;

/**
 * Adapter for the Tracks ListView in MainActivity - lstTracks
 */
public class TrackAdapter extends ArrayAdapter<Track> {

    public TrackAdapter(@NonNull Context context, List<Track> items) {
        super(context, R.layout.row_track, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {
            // Initialize the view and the ViewHolder
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.row_track, parent, false);

            viewHolder = new ViewHolder();

            viewHolder.background = convertView.findViewById(R.id.background);
            viewHolder.lblTitle = convertView.findViewById(R.id.lblTitle);
            viewHolder.lblArtist = convertView.findViewById(R.id.lblArtist);
            viewHolder.lblDuration = convertView.findViewById(R.id.lblDuration);
            viewHolder.imgEqualizer = convertView.findViewById(R.id.imgEqualizer);

            convertView.setTag(viewHolder);

        } else {
            // Get ViewHolder from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Get the item
        Track track = getItem(position);

        if (track != null) {

            // Set the background - tiles of grey for all rows, accent color for the selected row
            viewHolder.background.setBackgroundColor(getContext().getColor(
                    track.isSelected()
                            ? R.color.colorAccent
                            : position % 2 == 0
                            ? R.color.colorTrackListBackgroundOne
                            : R.color.colorTrackListBackgroundTwo));

            // Set values for the UI components
            viewHolder.lblTitle.setText(track.getTitle());
            viewHolder.lblArtist.setText(track.getArtist());
            viewHolder.lblDuration.setText(Utils.formatMillis(track.getDuration()));

            if (track.isSelected()) {
                viewHolder.imgEqualizer.setVisibility(View.VISIBLE);
                if (track.isPlaying()) {
                    // The track is playing - show the animated equalizer
                    Glide.with(getContext())
                            .load(R.drawable.equalizer)
                            .asGif()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(viewHolder.imgEqualizer);
                } else {
                    // The track is selected but isn't playing - show the equalizer bitmap
                    Glide.with(getContext())
                            .load(R.drawable.equalizer)
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(viewHolder.imgEqualizer);
                }
            } else {
                // The track isn't selected - remove the image from the ImageView and hide it
                viewHolder.imgEqualizer.setImageDrawable(null);
                viewHolder.imgEqualizer.setVisibility(View.GONE);
            }

        }

        return convertView;
    }

    private static final class ViewHolder {
        LinearLayout background;
        TextView lblTitle, lblArtist, lblDuration;
        ImageView imgEqualizer;
    }

}