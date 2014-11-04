package com.example.RaceGoferApp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian on 11/3/2014.
 */
public class CheckpointChooser extends Activity implements GoogleMap.OnMapClickListener {
    private GoogleMap map;
    GPSTracker gps;
    private double latitude;
    private double longitude;
    List<LatLng> checkpointlist = new ArrayList<LatLng>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkpointchooser);

        ActionBar ab = getActionBar();
        ab.setTitle("Choose Checkpoints");

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
        map.setOnMapClickListener(CheckpointChooser.this);

        gps = new GPSTracker(CheckpointChooser.this);

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

    @Override
    public void onMapClick(LatLng point) {
        checkpointlist.add(point);
        drawPoints();
    }

    private void drawPoints() {
        map.clear();
        PolylineOptions checkpointline = new PolylineOptions();
        for(int i = 0; i < checkpointlist.size(); i++){
            map.addMarker(new MarkerOptions().position(checkpointlist.get(i)).icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint)));
            checkpointline.add(checkpointlist.get(i));
        }
        Polyline polyline = map.addPolyline(checkpointline);
    }

    public void undoButton(View view) {
        checkpointlist.remove(checkpointlist.size()-1);
        drawPoints();
    }

    public void createButton(View view) {
        Intent i = new Intent(this, RaceListsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);

    }
}
