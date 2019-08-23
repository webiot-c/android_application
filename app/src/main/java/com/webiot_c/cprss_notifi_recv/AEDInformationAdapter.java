package com.webiot_c.cprss_notifi_recv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class AEDInformationAdapter extends BaseAdapter {

    Context context;
    LayoutInflater lif = null;
    ArrayList<AEDInformation> aeds;

    public AEDInformationAdapter(Context context) {
        this(context, null);
    }
    public AEDInformationAdapter(Context context, ArrayList<AEDInformation> aeds) {
        this.context = context;
        this.lif = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.aeds = aeds;
    }

    public void setList(ArrayList<AEDInformation> aeds){
        this.aeds = aeds;
    }

    @Override
    public int getCount() {
        return aeds.size();
    }

    @Override
    public Object getItem(int position) {
        return aeds.get(position);
    }

    @Override
    public long getItemId(int position) {
        return aeds.get(position).getUniqueID();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = lif.inflate(R.layout.aedinfo_listview, parent, false);

        AEDInformation aed = aeds.get(position);
        ((TextView)convertView.findViewById(R.id.adeid)).setText(aed.getAed_id());

        String location = String.format(Locale.getDefault(),
                context.getString(R.string.latitude) + " ／  " + context.getString(R.string.longitude),
                aed.getLatitude(),
                aed.getLongitude()
        );

        ((TextView)convertView.findViewById(R.id.loc)).setText(location);

        return convertView;
    }
}
