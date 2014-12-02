package com.example.RaceGoferApp;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
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
public class RacerViewActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    private GoogleMap map;
    private GPSTracker gps;
    private String race;
    private String race_id;
    private String userType;
    private Handler getHandler;
    private Handler sendHandler;
    private List userMarkers = new ArrayList();

    //GPS variables
    private LocationClient locationClient;
    private LocationRequest locationRequest;
    private Boolean locationEnabled;
    private  Location curLocation;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        race = i.getStringExtra("race");
        ActionBar ab = getActionBar();
        ab.setTitle(race);

        //Get extras from intent
        race_id = i.getStringExtra("race_id");
        userType = i.getStringExtra("user_type");

        setContentView(R.layout.racerview);

        //gps setup
        locationClient = new LocationClient(this, this, this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setSmallestDisplacement(4);

        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationEnabled = false;
            Toast.makeText(this, "Enable location services for accurate data", Toast.LENGTH_SHORT).show();
        }
        else if(!userType.equals("Spectator")){
            locationEnabled = true;
            locationClient.connect();
            Log.v("Location Services", "Location Enabled");
        }

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

        //Start a task to repeat getting racer coordinates
        getHandler = new Handler();
        getHandler.postDelayed(getCoords, 1000);
    }

    public Activity getActivity(){
        return this;
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

    private Runnable sendCoords = new Runnable() {
        @Override
        public void run() {
            HttpConc http = new HttpConc(getApplicationContext());
            URLParamEncoder encoder = new URLParamEncoder();
            try
            {
                http.sendGet("http://racegofer.com/api/UpdatePosition?raceId=" + encoder.encode(race_id) + "&latitude=" + encoder.encode(Double.toString(curLocation.getLatitude())) + "&longitude=" + encoder.encode(Double.toString(curLocation.getLongitude())));
            }
            catch(Exception e)
            {
                String err = (e.getMessage()==null)?"HTTP Send Coords Error":e.getMessage();
                Log.e("HTTP Send Coords Error", err);
            }
        }
    };

    private Runnable getCoords = new Runnable() {
        @Override
        public void run() {
            HttpConc http = new HttpConc(getApplicationContext());
            URLParamEncoder encoder = new URLParamEncoder();
            String response;
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

            getHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onDestroy(){
        getHandler.removeCallbacks(getCoords);
        sendHandler.removeCallbacks(sendCoords);
        locationClient.removeLocationUpdates(this);
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("Location Services", "Connected");
        Location location = locationClient.getLastLocation();
        locationClient.requestLocationUpdates(locationRequest, this);
        if (location != null) {
            Log.v("Location Services", "Location: " + location.getLatitude() + ", " + location.getLongitude());
        }
        else if (location == null && locationEnabled) {
            locationClient.requestLocationUpdates(locationRequest, this);
        }
    }

    @Override
    public void onDisconnected() {
        locationClient.removeLocationUpdates(this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location) {
        curLocation = location;
        sendHandler = new Handler();
        sendHandler.post(sendCoords);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        MenuItem raceUsers = menu.findItem(R.id.action_users);
        if(userType.equals("Spectator")){
            raceUsers.setVisible(false);
        }

        MenuItem deleteRace = menu.findItem(R.id.action_delete);
        if(userType.equals("Spectator") || userType.equals("Participant")){
            deleteRace.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.racerview_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_users:
                DialogFragment raceUsers = new UserListDialog();
                Bundle argsUser = new Bundle();
                argsUser.putString("id", race_id);
                argsUser.putString("type", userType);
                raceUsers.setArguments(argsUser);
                raceUsers.show(getFragmentManager(), "raceUsers");
                return true;
            case R.id.action_info:
                DialogFragment raceInfo = new RaceInfoDialog();
                Bundle args = new Bundle();
                args.putString("id", race_id);
                args.putString("type", userType);
                raceInfo.setArguments(args);
                raceInfo.show(getFragmentManager(), "raceInfo");
                return true;
            case R.id.action_leave:
                DialogFragment leave = new LeaveDialog();
                Bundle argsLeave = new Bundle();
                argsLeave.putString("id", race_id);
                leave.setArguments(argsLeave);
                leave.show(getFragmentManager(), "leaveRace");
                return true;
            case R.id.action_delete:
                DialogFragment delete = new DeleteDialog();
                Bundle argsDelete = new Bundle();
                argsDelete.putString("id", race_id);
                delete.setArguments(argsDelete);
                delete.show(getFragmentManager(), "leaveRace");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class RaceInfoDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            HttpConc http = new HttpConc(getActivity().getApplicationContext());
            URLParamEncoder encoder = new URLParamEncoder();
            JSONObject raceInfo;
            try{
                String response = http.sendGet("http://racegofer.com/api/GetRaceInfo?raceId=" + encoder.encode(getArguments().getString("id")));
                raceInfo = new JSONObject(response);
            }
            catch (Exception e){
                String err = (e.getMessage()==null)?"JoinRaces Error":e.getMessage();
                Log.e("JoinRaces Error", err);
                Context context = getActivity().getApplicationContext();
                CharSequence text = "Error in communicating with server.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                return null;
            }

            String message = "Failed to get race information. Please try again later.";
            try {
                message = "Race Name: " + raceInfo.getString("raceName") + "\nRace Type: " + raceInfo.getString("raceType") + "\nLocation: " + raceInfo.getString("location") + "\nYour role: " + getArguments().getString("type");
            }
            catch (Exception e){}

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(message)
                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            return builder.create();
        }
    }

    public static class UserListDialog extends DialogFragment{
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //Get users in race
            HttpConc http = new HttpConc(getActivity().getApplicationContext());
            URLParamEncoder encoder = new URLParamEncoder();
            JSONArray users;
            try{
                String response = http.sendGet("http://racegofer.com/api/GetNamesAndNumbersForRace?raceId=" + encoder.encode(getArguments().getString("id")) + "&type=" + encoder.encode(getArguments().getString("type")));
                users = new JSONArray(response);
            }
            catch (Exception e){
                String err = (e.getMessage()==null)?"UserList Error":e.getMessage();
                Log.e("UserList Error", err);
                Context context = getActivity().getApplicationContext();
                CharSequence text = "Error in communicating with server.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                return null;
            }

            List<String> userList = new LinkedList<String>();
            final List<String> userNumbers = new LinkedList<String>();

            for(int i = 0; i < users.length(); i++){
                JSONObject user;
                try {
                    user = users.getJSONObject(i);
                    userList.add(user.getString("firstName") + " " + user.getString("lastName"));
                    userNumbers.add(user.getString("phoneNumber"));
                }
                catch (JSONException e){
                    String err = (e.getMessage()==null)?"UserList Error":e.getMessage();
                    Log.e("UserList Error", err);
                }
            }

            final CharSequence[] charSequenceUserList = userList.toArray(new CharSequence[userList.size()]);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Participants in race")
                    .setItems(charSequenceUserList, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            CallNumberDialog callDialog = new CallNumberDialog();
                            Bundle args = new Bundle();
                            args.putString("name", charSequenceUserList[which].toString());
                            args.putString("number", userNumbers.get(which));
                            callDialog.setArguments(args);
                            callDialog.show(getFragmentManager(), "callNumber");
                        }
                    });
            return builder.create();
        }
    }

    public static class CallNumberDialog extends DialogFragment{
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Would you like to call " + getArguments().getString("name") + " at " + getArguments().getString("number") + "?")
                    .setPositiveButton("Call", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:" + getArguments().getString("number")));
                            startActivity(callIntent);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
            return builder.create();
        }
    }

    public static class LeaveDialog extends DialogFragment{
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure you want to leave the race?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            HttpConc http = new HttpConc(getActivity().getApplicationContext());
                            URLParamEncoder encoder = new URLParamEncoder();
                            try {
                                String response = http.sendGet("http://racegofer.com/api/LeaveRace?raceId=" + encoder.encode(getArguments().getString("id")));
                                if(response.equals("0")){
                                    Context context = getActivity().getApplicationContext();
                                    CharSequence text = "Error in leaving race. Try again later.";
                                    int duration = Toast.LENGTH_SHORT;
                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                    return;
                                }
                                getActivity().finish();

                            } catch (Exception e) {
                                String err = (e.getMessage() == null) ? "Leave Error" : e.getMessage();
                                Log.e("Leave Error", err);
                                Context context = getActivity().getApplicationContext();
                                CharSequence text = "Error in communicating with server.";
                                int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();
                            }
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
            return builder.create();
        }
    }

    public static class DeleteDialog extends DialogFragment{
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure you want to delete the race?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            HttpConc http = new HttpConc(getActivity().getApplicationContext());
                            URLParamEncoder encoder = new URLParamEncoder();
                            try {
                                String response = http.sendGet("http://racegofer.com/api/DeleteRace?raceId=" + encoder.encode(getArguments().getString("id")));
                                if(response.equals("0")){
                                    Context context = getActivity().getApplicationContext();
                                    CharSequence text = "Error in leaving race. Try again later.";
                                    int duration = Toast.LENGTH_SHORT;
                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                    return;
                                }
                                getActivity().finish();

                            } catch (Exception e) {
                                String err = (e.getMessage() == null) ? "Delete Error" : e.getMessage();
                                Log.e("Delete Error", err);
                                Context context = getActivity().getApplicationContext();
                                CharSequence text = "Error in communicating with server.";
                                int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();
                            }
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
            return builder.create();
        }
    }
}
