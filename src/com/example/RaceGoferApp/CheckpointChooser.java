package com.example.RaceGoferApp;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;
import org.apache.http.client.utils.URIUtils;

import java.net.URI;
import java.net.URLEncoder;
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
        if(checkpointlist.size() > 0) {
            checkpointlist.remove(checkpointlist.size() - 1);
        }
        drawPoints();
    }

    public void createButton(View view) {
        if(checkpointlist.size() > 0) {
            Intent thisIntent = getIntent();
            String racename = thisIntent.getStringExtra("race_name");
            String type = thisIntent.getStringExtra("type");
            String location = thisIntent.getStringExtra("location");
            String password = thisIntent.getStringExtra("password");
            String managerpassword = thisIntent.getStringExtra("manager_password");

            URLParamEncoder encoder = new URLParamEncoder();

            String params = "raceName=" + encoder.encode(racename) + "&raceType=" + encoder.encode(type) + "&raceLocation=" + encoder.encode(location) + "&";
            if (!password.equals("")) {
                params = params + "racePassword=" + encoder.encode(password) + "&";
            }
            params = params + "managerPassword=" + encoder.encode(managerpassword) + "&checkPoints=[";

            for(int i = 0; i < checkpointlist.size(); i++){
                if(i == checkpointlist.size()-1){
                    params = params + "{%22latitude%22:" + String.valueOf(checkpointlist.get(i).latitude) + ",%22longitude%22:" + String.valueOf(checkpointlist.get(i).longitude) + "}]";
                }
                else {
                    params = params + "{%22latitude%22:" + String.valueOf(checkpointlist.get(i).latitude) + ",%22longitude%22:" + String.valueOf(checkpointlist.get(i).longitude) + "},";
                }
            }


            String url = "http://" + getString(R.string.site) + getString(R.string.api_create_race) + params;

            HttpConc http = new HttpConc(getApplicationContext());
            try {
                String response = http.sendGet(url);
                String joinurl = "http://racegofer.com/api/JoinRace?raceId=" + encoder.encode(response) + "&password=" + encoder.encode(managerpassword) + "&userType=Manager";
                http.sendGet(joinurl);

                Intent intent = new Intent(this, RaceListsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            catch (Exception e){
                String err = (e.getMessage()==null)?"HTTP Fail":e.getMessage();
                Log.e("HTTP Error", err);
                Log.v("HTTP Attempt", url);
                DialogFragment alert = new HttpErrorAlert();
                alert.show(getFragmentManager(), "http error alert");
            }
        }
        else {
            Context context = getApplicationContext();
            CharSequence text = "Please create checkpoints for your racers by tapping on the map.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

}
