package com.example.RaceGoferApp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Brian on 10/30/2014.
 */
public class RacerViewActivity extends Activity{
    private GoogleMap map;
    GPSTracker gps;
    private double latitude;
    private double longitude;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.racerview);

        Intent i = getIntent();
        String race = i.getStringExtra("race");
        ActionBar ab = getActionBar();
        ab.setTitle(race);

        //Map Setup
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);

        gps = new GPSTracker(RacerViewActivity.this);


        if (gps.canGetLocation()) {
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();

            CameraPosition.Builder current = new CameraPosition.Builder().target(new LatLng(latitude,longitude)).zoom(16);
            map.animateCamera(CameraUpdateFactory.newCameraPosition(current.build()));
        }
        else {
            gps.showSettingsAlert();
        }
    }

}