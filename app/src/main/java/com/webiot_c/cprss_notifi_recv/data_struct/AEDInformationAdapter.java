package com.webiot_c.cprss_notifi_recv.data_struct;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.webiot_c.cprss_notifi_recv.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AEDInformationAdapter extends RecyclerView.Adapter<AEDInformationAdapter.ViewHolder> {

    ArrayList<AEDInformation> dataset;
    Context context;

    View.OnClickListener listener;

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView aedid;
        TextView location;
        LinearLayout root;

        ViewHolder(View v){
            super(v);
            root = v.findViewById(R.id.aedinfo_root);
            aedid = v.findViewById(R.id.adeid);
            location = v.findViewById(R.id.map_loc);
        }

    }

    public AEDInformationAdapter(Context context, ArrayList<AEDInformation> dataset, View.OnClickListener listener){
        this.context = context;
        this.dataset = new ArrayList<>(dataset);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewtype) {
        View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.aedinfo_listview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        AEDInformation aed = dataset.get(position);

        String location = String.format(Locale.getDefault(),
                context.getString(R.string.latitude) + " ／  " + context.getString(R.string.longitude),
                aed.getLatitude(),
                aed.getLongitude()
        );

        holder.aedid.setText(aed.getAed_id());
        holder.location.setText(location);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClick(view);
            }
        });

        if(aed.isReceivedDateInDuration(new Date(), 7)){
            holder.root.setBackgroundColor(context.getResources().getColor(R.color.oldAEDInfo));
        }

    }

    public void updateList(ArrayList<AEDInformation> dataset){
        this.dataset = dataset;
    }

    public void add(int position, AEDInformation aedinfo){
        this.dataset.add(position, aedinfo);
    }


    public AEDInformation remove(int position){
        return this.dataset.remove(position);
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

}
