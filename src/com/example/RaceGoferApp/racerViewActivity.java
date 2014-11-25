package com.example.RaceGoferApp;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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


        String URL1 = new String();  //Call function GetRaceInfo
        String URL2 = new String();  //Call function GetUserCoordinates

        String RaceCoordinatesValue= new String(); // Get result of function GetRaceInfo
        String UserCoordinates = new String();    //Get result of function GetUserCoordinates
        JSONObject RaceCoordinate=  new JSONObject();

        //TODO - call the get race info api function
        URL1 =  "http://" + "www.racegofer.com" + "/api/GetRaceInfo?" + "raceId=" + race_id;

        HttpConc http = new HttpConc(getApplicationContext());
        try
        {
            RaceCoordinatesValue= http.sendGet(URL1);

        }

        catch(Exception e)
        {
            String err = (e.getMessage()==null)?"HTTP Fail":e.getMessage();
            Log.e("HTTP Error", err);
            Log.v("HTTP Attempt", RaceCoordinatesValue);
            DialogFragment alert = new HttpErrorAlert();
            alert.show(getFragmentManager(), "http error alert");
        }
        try {
            RaceCoordinate = new JSONObject(RaceCoordinatesValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray CP = new JSONArray();
        try {
            CP = new JSONArray(RaceCoordinate.getJSONArray("checkPoints"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (int j = 0; j < CP.length(); j++) {
            JSONObject cp = new JSONObject();
            Double lon = 0.0;
            Double lat = 0.0;
            try {
                cp = CP.getJSONObject(j);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                lon = cp.getDouble("longitude");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                lat = cp.getDouble("latitude");
            } catch (JSONException e) {
                e.printStackTrace();
            }


            System.out.println(lon + ", " + lat);
        }

        //GetRaceInfo(race_id);
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

        drawRacers();
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