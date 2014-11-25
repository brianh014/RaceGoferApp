package com.example.RaceGoferApp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;


/**
 * Created by Brian on 10/30/2014.
 */
public class RacerViewActivity extends Activity{
    private GoogleMap map;
    private GPSTracker gps;
    private String race_id;
    private Handler handler;
    private List userMarkers = new ArrayList();

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

        //Draw race checkpoints
        setupInfo();

        //Create gps tracker and move to users location
        gps = new GPSTracker(RacerViewActivity.this);
        if (gps.canGetLocation()) {
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            CameraPosition.Builder current = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(16);
            map.animateCamera(CameraUpdateFactory.newCameraPosition(current.build()));
        } else {
            gps.showSettingsAlert();
        }

        //Start a task to repeat getting and sending racer coordinates
        handler = new Handler();
        handler.postDelayed(getSendCoords, 1000);
    }


    private void setupInfo(){
        HttpConc http = new HttpConc(getApplicationContext());
        URLParamEncoder encoder = new URLParamEncoder();
        String response;
        try
        {
            response = http.sendGet("http://racegofer.com/api/GetRaceInfo?raceId=" + encoder.encode(race_id));
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

        //Parse and draw checkpoints and line
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
            checkpointLine.add(new LatLng(lat, lon));
        }
        map.addPolyline(checkpointLine);
    }

    private Runnable getSendCoords = new Runnable() {
        @Override
        public void run() {
            HttpConc http = new HttpConc(getApplicationContext());
            URLParamEncoder encoder = new URLParamEncoder();
            String response;
            //Send current coords
            try
            {
                if (gps.canGetLocation()) {
                    response = http.sendGet("http://racegofer.com/api/UpdatePosition?raceId=" + encoder.encode(race_id) + "&latitude=" + encoder.encode(Double.toString(gps.getLatitude())) + "&longitude=" + encoder.encode(Double.toString(gps.getLongitude())));
                }
            }
            catch(Exception e)
            {
                String err = (e.getMessage()==null)?"HTTP Send Coords Error":e.getMessage();
                Log.e("HTTP Send Coords Error", err);
                return;
            }

            //Get other racers coords
            try
            {
                response = http.sendGet("http://racegofer.com/api/GetRacerCoordinates?raceId=" + encoder.encode(race_id));
            }
            catch(Exception e)
            {
                String err = (e.getMessage()==null)?"HTTP Send Coords Error":e.getMessage();
                Log.e("HTTP Send Coords Error", err);
                return;
            }

            JSONArray racerCoords;
            try {
                racerCoords = new JSONArray(response);
            }
            catch (JSONException e){
                String err = (e.getMessage()==null)?"JSON Error":e.getMessage();
                Log.e("JSON Error", err);
                return;
            }

            //clear marker list
            for(int i = 0; i < userMarkers.size(); i++){
                Marker marker = (Marker)userMarkers.get(i);
                marker.remove();
            }

            for(int i = 0; i < racerCoords.length(); i++){
                JSONObject raceCoord;
                try{
                    raceCoord = racerCoords.getJSONObject(i);
                    Double lat = raceCoord.getDouble("latitude");
                    Double lon = raceCoord.getDouble("longitude");
                    String user = raceCoord.getString("userName");
                    Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(lat,lon)).title(user).icon(BitmapDescriptorFactory.fromResource(R.drawable.race_marker_green)));
                    userMarkers.add(marker);
                }
                catch (JSONException e){
                    String err = (e.getMessage()==null)?"JSON Error":e.getMessage();
                    Log.e("JSON Error", err);
                    return;
                }
            }

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onDestroy(){
        handler.removeCallbacks(getSendCoords);
        super.onDestroy();
    }
}
