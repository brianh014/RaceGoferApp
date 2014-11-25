package com.example.RaceGoferApp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Brian on 10/30/2014.
 */
public class RacerViewActivity extends Activity{
    private GoogleMap map;
    GPSTracker gps;
    private double latitude;
    private double longitude;
    String race_id;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.racerview);

        Intent i = getIntent();
        String race = i.getStringExtra("race");
        ActionBar ab = getActionBar();
        ab.setTitle(race);

        //Get Race Id from intent
        race_id = i.getStringExtra("race_id");

        //Map Setup
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);

        setupInfo(race_id);

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

        //drawRacers();
    }

    private void setupInfo(String raceid){
        HttpConc http = new HttpConc(getApplicationContext());
        URLParamEncoder encoder = new URLParamEncoder();
        String response;
        try
        {
            response = http.sendGet("http://racegofer.com/api/GetRaceInfo?raceId=" + encoder.encode(raceid));
        }
        catch(Exception e)
        {
            String err = (e.getMessage()==null)?"HTTP Error":e.getMessage();
            Log.e("HTTP Error", err);
            Context context = getApplicationContext();
            CharSequence text = "Error in communicating with server.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        JSONObject raceInfo;
        try {
             raceInfo = new JSONObject(response);
        }
        catch (JSONException e){
            String err = (e.getMessage()==null)?"JSON Error":e.getMessage();
            Log.e("JSON Error", err);
            return;
        }

        JSONArray checkpoints;
        try {
            checkpoints = raceInfo.getJSONArray("checkPoints");
        } catch (JSONException e) {
            String err = (e.getMessage()==null)?"JSON Array Error":e.getMessage();
            Log.e("JSON Array Error", err);
            return;
        }

        PolylineOptions checkpointLine = new PolylineOptions();
        for (int i = 0; i < checkpoints.length(); i++) {
            JSONObject checkpoint;
            Double lon = 0.0;
            Double lat = 0.0;
            try {
                checkpoint = checkpoints.getJSONObject(i);
                lon = checkpoint.getDouble("longitude");
                lat = checkpoint.getDouble("latitude");
            } catch (JSONException e) {
                String err = (e.getMessage()==null)?"JSON Error":e.getMessage();
                Log.e("JSON Error", err);
                return;
            }
            map.addMarker(new MarkerOptions().position(new LatLng(lat,lon)).icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint)));
            checkpointLine.add(new LatLng(lat,lon));
        }
        map.addPolyline(checkpointLine);
    }

    private void drawRacers() {
        //Draw dummy racers
        map.addMarker(new MarkerOptions().position(new LatLng(30.6,-96.3)).title("Racer's Name").icon(BitmapDescriptorFactory.fromResource(R.drawable.race_marker_green)));
        map.addMarker(new MarkerOptions().position(new LatLng(30.62,-96.31)).title("Racer's Name").icon(BitmapDescriptorFactory.fromResource(R.drawable.race_marker_green)));

        //Draw dummy checkpoints
        map.addMarker(new MarkerOptions().position(new LatLng(30.6,-96.4)).icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint)));
        map.addMarker(new MarkerOptions().position(new LatLng(30.7,-96.5)).icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint)));
        map.addMarker(new MarkerOptions().position(new LatLng(30.8,-96.4)).icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint)));
        map.addMarker(new MarkerOptions().position(new LatLng(30.3,-96.2)).icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint)));
        PolylineOptions checkpointline = new PolylineOptions()
                .add(new LatLng(30.6,-96.4))
                .add(new LatLng(30.7,-96.5))
                .add(new LatLng(30.8,-96.4))
                .add(new LatLng(30.3,-96.2));
        Polyline polyline = map.addPolyline(checkpointline);
    }

}
