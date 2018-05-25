package com.daniel.awesomemusicplayer.tracks;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daniel.awesomemusicplayer.R;
import com.daniel.awesomemusicplayer.util.Utils;

import java.util.List;

public class TrackAdapter extends ArrayAdapter<Track> {

    public TrackAdapter(@NonNull Context context, List<Track> items) {
        super(context, R.layout.row_track, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.row_track, parent, false);

            viewHolder = new ViewHolder();

            viewHolder.background = convertView.findViewById(R.id.background);
            viewHolder.lblTitle = convertView.findViewById(R.id.lblTitle);
            viewHolder.lblArtist = convertView.findViewById(R.id.lblArtist);
            viewHolder.lblDuration = convertView.findViewById(R.id.lblDuration);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Track track = getItem(position);

        if (track != null) {

            viewHolder.background.setBackgroundColor(getContext().getResources().getColor(
                    track.isSelected()
                            ? R.color.colorAccent
                            : position % 2 == 0
                            ? R.color.colorTrackListBackgroundOne
                            : R.color.colorTrackListBackgroundTwo));

            viewHolder.lblTitle.setText(track.getTitle());
            viewHolder.lblArtist.setText(track.getArtist());
            viewHolder.lblDuration.setText(Utils.formatMillis(track.getDuration()));

        }

        return convertView;
    }

    private static final class ViewHolder {
        LinearLayout background;
        TextView lblTitle, lblArtist, lblDuration;
    }

}