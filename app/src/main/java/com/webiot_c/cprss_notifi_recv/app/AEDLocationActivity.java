package com.webiot_c.cprss_notifi_recv.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.webiot_c.cprss_notifi_recv.R;
import com.webiot_c.cprss_notifi_recv.app.service.CPRSS_BackgroundAccessService;

import java.util.Locale;

public class AEDLocationActivity extends FragmentActivity implements OnMapReadyCallback {

    public class MapBroadcastReceier extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            String received_aedid = intent.getStringExtra("aed-id");

            if(!received_aedid.equals(aed_id)) {
                return;
            }

            if(intent.getAction().equals(CPRSS_BackgroundAccessService.BroadcastConstant.AED_FINISHED)){
                isEnabled = false;
                updateUI();
                return;
            }

            lat = intent.getDoubleExtra("lat", 0);
            lon = intent.getDoubleExtra("lon", 0);

            if(marker != null){
                marker.setPosition(new LatLng(lat, lon));

                zoom = mMap.getCameraPosition().zoom;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), zoom));
            }

            isEnabled = true;

            updateUI();

        }
    }

    private GoogleMap mMap;
    private Marker marker;

    private String aed_id;
    private double lat;
    private double lon;

    private float zoom = 17;

    private MapBroadcastReceier mapBroadcastReceier;

    private boolean isEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mapBroadcastReceier = new MapBroadcastReceier();
        IntentFilter mbr_filter = new IntentFilter();

        mbr_filter.addAction(CPRSS_BackgroundAccessService.BroadcastConstant.AED_STARTED);
        mbr_filter.addAction(CPRSS_BackgroundAccessService.BroadcastConstant.AED_LOCATION_UPDATED);
        mbr_filter.addAction(CPRSS_BackgroundAccessService.BroadcastConstant.AED_FINISHED);
        registerReceiver(mapBroadcastReceier, mbr_filter);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aedlocation);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        updateUI();

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Intent intent = getIntent();
        aed_id = getIntent().getStringExtra("aed-id");

        mMap = googleMap;

        lat = intent.getDoubleExtra("lat", 0);
        lon = intent.getDoubleExtra("lon", 0);

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(lat, lon);
        marker = mMap.addMarker(new MarkerOptions().position(sydney).title(intent.getStringExtra("aed-id")));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, zoom));

        updateUI();

    }

    public void updateUI(){
        String location = String.format(Locale.getDefault(),
                getString(R.string.latitude) + " ／  " + getString(R.string.longitude),
                lat, lon
        );

        ((TextView) findViewById(R.id.map_aed_id)).setText(aed_id);
        ((TextView) findViewById(R.id.map_loc)).setText(location);

        int forecol, backcol;

        if(isEnabled){
            forecol = getResources().getColor(R.color.aed_enabled_fore);
            backcol = getResources().getColor(R.color.aed_enabled);
        } else {
            forecol = getResources().getColor(R.color.noaedinfo_fore);
            backcol = getResources().getColor(R.color.noaedinfo);
        }

        findViewById(R.id.map_title_bar).setBackgroundColor(backcol);
        ((TextView)findViewById(R.id.map_aed_id)).setTextColor(forecol);

        findViewById(R.id.map_open_area).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String uri = String.format("geo:%f,%f?q=%f,%f&z=%f", lat, lon, lat, lon, zoom);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
                return true;
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mapBroadcastReceier);
    }
}
