package com.unipi.p15173.musicplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

//Custom recycle view for displaying the audio tracks (files) from the media folder.
public class RecyclerView_Adapter extends RecyclerView.Adapter<ViewHolder> implements Filterable {

    List<Audio> list, filteredList;
    Context context;

    public RecyclerView_Adapter(List<Audio> list, Context context) {
        this.list = list;
        this.filteredList = list;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflate the layout, initialize the View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        holder.title.setText(filteredList.get(position).getTitle());
    }

    @Override
    public int getItemCount() {
        //returns the number of elements the RecyclerView will display
        return filteredList.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String query = charSequence.toString();

                List<Audio> filteredList = new ArrayList<>();

                if (query.isEmpty()) {
                    filteredList = list;
                } else {
                    for (Audio audio : list) {
                        if (audio.getTitle().toLowerCase().contains(query.toLowerCase())) {
                            filteredList.add(audio);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.count = filteredList.size();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults results) {
                filteredList = (ArrayList<Audio>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}

class ViewHolder extends RecyclerView.ViewHolder {

    TextView title;

    ViewHolder(View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
    }
}