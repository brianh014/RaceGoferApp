package com.example.RaceGoferApp;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import android.view.View;
import android.widget.Button;
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
    private Runnable getCoordsRunnable;

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
        findViewById(R.id.callButton).getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);

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
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setAllGesturesEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);

        //Draw race checkpoints
        Queue<Coordinate> checkpointsList = setupInfo();

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
        getCoordsRunnable = new getCoords(checkpointsList);
        getHandler.postDelayed(getCoordsRunnable, 1000);
    }

    public Activity getActivity(){
        return this;
    }

    private Queue<Coordinate> setupInfo(){
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
            return null;
        }

        JSONObject raceInfo;
        try {
             raceInfo = new JSONObject(response);
        }
        catch (JSONException e){
            String err = (e.getMessage()==null)?"JSON Error":e.getMessage();
            Log.e("JSON Error", err);
            return null;
        }

        JSONArray checkpoints;
        try {
            checkpoints = raceInfo.getJSONArray("checkPoints");
        } catch (JSONException e) {
            String err = (e.getMessage()==null)?"JSON Array Error":e.getMessage();
            Log.e("JSON Array Error", err);
            return null;
        }

        //Parse and draw checkpoints and line
        PolylineOptions checkpointLine = new PolylineOptions();
        Queue<Coordinate> checkpointsList = new LinkedList<Coordinate>();
        for (int i = 0; i < checkpoints.length(); i++) {
            JSONObject checkpoint;
            Double lon;
            Double lat;
            try {
                checkpoint = checkpoints.getJSONObject(i);
                lon = checkpoint.getDouble("longitude");
                lat = checkpoint.getDouble("latitude");

                //Setup structure for use in CoordinateFunction
                checkpointsList.add(new Coordinate(lat, lon));

            } catch (JSONException e) {
                String err = (e.getMessage()==null)?"JSON Error":e.getMessage();
                Log.e("JSON Error", err);
                return null;
            }
            if(i == checkpoints.length() - 1) {
                map.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint_end)));
            }
            else{
                map.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint)));
            }
            checkpointLine.add(new LatLng(lat, lon));
        }
        map.addPolyline(checkpointLine);
        return checkpointsList;
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

    public class getCoords implements Runnable {

        private final Queue<Coordinate> checkList = new LinkedList<Coordinate>();

        public getCoords(Queue<Coordinate> checkListArg){
            checkList.addAll(checkListArg);
        }

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

                    //Determine if user too far away from checkpoints
                    CoordinateFunction userCheck = new CoordinateFunction();
                    Boolean inRange = userCheck.RacerInRange(new Coordinate(lat, lon), checkList);
                    Log.v("inRange", inRange.toString());
                    Log.v("checkList", checkList.toString());

                    Marker marker;
                    if(inRange) {
                        marker = map.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title(raceCoord.getString("firstName") + " " + raceCoord.getString("lastName")).icon(BitmapDescriptorFactory.fromResource(R.drawable.race_marker_green)));
                    }
                    else{
                        marker = map.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title(raceCoord.getString("firstName") + " " + raceCoord.getString("lastName") + " - Racer is off track!").icon(BitmapDescriptorFactory.fromResource(R.drawable.race_marker_red)));
                    }
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
    }

    @Override
    public void onDestroy(){
        try {
            getHandler.removeCallbacks(getCoordsRunnable);
            sendHandler.removeCallbacks(sendCoords);
        }
        catch (Exception e){
            String err = (e.getMessage()==null)?"Runnable Error":e.getMessage();
            Log.e("Runnable Error", err);
        }
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
            JSONArray managersArray;
            JSONArray participantArray;
            try{
                String managers = http.sendGet("http://racegofer.com/api/GetNamesAndNumbersForRace?raceId=" + encoder.encode(getArguments().getString("id")) + "&type=Manager");
                String participants = http.sendGet("http://racegofer.com/api/GetNamesAndNumbersForRace?raceId=" + encoder.encode(getArguments().getString("id")) + "&type=Participant");
                managersArray = new JSONArray(managers);
                participantArray = new JSONArray(participants);
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

            List<String> managerList = new LinkedList<String>();
            final List<String> userNumbers = new LinkedList<String>();

            for(int i = 0; i < managersArray.length(); i++){
                JSONObject user;
                try {
                    user = managersArray.getJSONObject(i);
                    managerList.add(user.getString("firstName") + " " + user.getString("lastName") + " - Race Manager");
                    userNumbers.add(user.getString("phoneNumber"));
                }
                catch (JSONException e){
                    String err = (e.getMessage()==null)?"UserList Error":e.getMessage();
                    Log.e("UserList Error", err);
                }
            }

            List<String> participantList = new LinkedList<String>();

            for(int i = 0; i < participantArray.length(); i++){
                JSONObject user;
                try {
                    user = participantArray.getJSONObject(i);
                    participantList.add(user.getString("firstName") + " " + user.getString("lastName") + " - Race Participant");
                    userNumbers.add(user.getString("phoneNumber"));
                }
                catch (JSONException e){
                    String err = (e.getMessage()==null)?"UserList Error":e.getMessage();
                    Log.e("UserList Error", err);
                }
            }

            List<String> combo = new LinkedList<String>();
            combo.addAll(managerList);
            combo.addAll(participantList);
            final CharSequence[] charSequenceUserList = combo.toArray(new CharSequence[combo.size()]);

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

    public void emergencyClick(View view){
        HttpConc http = new HttpConc(getActivity().getApplicationContext());
        URLParamEncoder encoder = new URLParamEncoder();
        JSONArray managersArray;
        try{
            String managers = http.sendGet("http://racegofer.com/api/GetNamesAndNumbersForRace?raceId=" + race_id + "&type=Manager");
            managersArray = new JSONArray(managers);
        }
        catch (Exception e){
            String err = (e.getMessage()==null)?"UserList Error":e.getMessage();
            Log.e("UserList Error", err);
            Context context = getActivity().getApplicationContext();
            CharSequence text = "Error in communicating with server.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        List<String> managerList = new LinkedList<String>();
        final List<String> userNumbers = new LinkedList<String>();

        for(int i = 0; i < managersArray.length(); i++){
            JSONObject user;
            try {
                user = managersArray.getJSONObject(i);
                managerList.add(user.getString("firstName") + " " + user.getString("lastName") + " - Race Manager");
                userNumbers.add(user.getString("phoneNumber"));
            }
            catch (JSONException e){
                String err = (e.getMessage()==null)?"UserList Error":e.getMessage();
                Log.e("UserList Error", err);
            }
        }

        Bundle args = new Bundle();
        args.putString("name", managerList.get(0));
        args.putString("number", userNumbers.get(0));
        CallNumberDialog call = new CallNumberDialog();
        call.setArguments(args);
        call.show(getFragmentManager(), "call");
    }
}
