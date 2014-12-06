package com.example.RaceGoferApp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Brian on 11/18/2014.
 */
public class JoinRaceActivity extends Activity {
    String raceId;
    JSONObject raceInfo;
    TextView raceText;
    TextView typeText;
    TextView locationText;
    TextView passwordText;
    EditText passwordField;
    RadioButton participant;
    RadioButton spectator;
    RadioButton manager;
    CheckBox hideBox;
    Boolean hasPass;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joinrace);
        Intent i = getIntent();
        raceId = i.getStringExtra("race_id");

        raceText = (TextView)findViewById(R.id.raceNameText);
        typeText = (TextView)findViewById(R.id.typeText);
        locationText = (TextView)findViewById(R.id.locationText);
        passwordText = (TextView)findViewById(R.id.passwordText);
        passwordField = (EditText)findViewById(R.id.passwordField);
        participant = (RadioButton)findViewById(R.id.participantButton);
        spectator = (RadioButton)findViewById(R.id.spectatorButton);
        manager = (RadioButton)findViewById(R.id.managerButton);
        hideBox = (CheckBox)findViewById(R.id.hideCheckBox);

        HttpConc http = new HttpConc(getApplicationContext());
        URLParamEncoder encoder = new URLParamEncoder();
        try{
            String response = http.sendGet("http://racegofer.com/api/GetRaceInfo?raceId=" + encoder.encode(raceId));
            Log.v("Response", response);
            raceInfo = new JSONObject(response);
            raceText.setText(raceInfo.getString("raceName"));
            typeText.setText(raceInfo.getString("raceType"));
            locationText.setText(raceInfo.getString("location"));
            hasPass = raceInfo.getBoolean("raceHasPassword");
        }
        catch (Exception e){
            String err = (e.getMessage()==null)?"JoinRaces Error":e.getMessage();
            Log.e("JoinRaces Error", err);
            Context context = getApplicationContext();
            CharSequence text = "Error in communicating with server.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            this.finish();
        }

        if(!hasPass){
            passwordText.setVisibility(View.GONE);
            passwordField.setVisibility(View.GONE);
        }

        if(participant.isChecked() && hasPass){
            passwordText.setVisibility(View.VISIBLE);
            passwordField.setVisibility(View.VISIBLE);
            hideBox.setVisibility(View.VISIBLE);
        }
        else if(participant.isChecked() && !hasPass){
            passwordText.setVisibility(View.GONE);
            passwordField.setVisibility(View.GONE);
            hideBox.setVisibility(View.VISIBLE);
        }
        if(spectator.isChecked() && hasPass){
            passwordText.setVisibility(View.VISIBLE);
            passwordField.setVisibility(View.VISIBLE);
            hideBox.setVisibility(View.GONE);
        }
        else if(spectator.isChecked() && !hasPass){
            passwordText.setVisibility(View.GONE);
            passwordField.setVisibility(View.GONE);
            hideBox.setVisibility(View.GONE);
        }
        else if(manager.isChecked()){
            passwordText.setVisibility(View.VISIBLE);
            passwordField.setVisibility(View.VISIBLE);
            hideBox.setVisibility(View.GONE);
        }
    }

    public void radioGroupClick(View view){
        if(participant.isChecked() && hasPass){
            passwordText.setVisibility(View.VISIBLE);
            passwordField.setVisibility(View.VISIBLE);
            hideBox.setVisibility(View.VISIBLE);
        }
        else if(participant.isChecked() && !hasPass){
            passwordText.setVisibility(View.GONE);
            passwordField.setVisibility(View.GONE);
            hideBox.setVisibility(View.VISIBLE);
        }
        if(spectator.isChecked() && hasPass){
            passwordText.setVisibility(View.VISIBLE);
            passwordField.setVisibility(View.VISIBLE);
            hideBox.setVisibility(View.GONE);
        }
        else if(spectator.isChecked() && !hasPass){
            passwordText.setVisibility(View.GONE);
            passwordField.setVisibility(View.GONE);
            hideBox.setVisibility(View.GONE);
        }
        else if(manager.isChecked()){
            passwordText.setVisibility(View.VISIBLE);
            passwordField.setVisibility(View.VISIBLE);
            hideBox.setVisibility(View.GONE);
        }
    }

    public void raceClick(View view){
        if(passwordField.getText().toString().equals("") && hasPass){
            Context context = getApplicationContext();
            CharSequence text = "Please provide a password.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        HttpConc http = new HttpConc(getApplicationContext());
        URLParamEncoder encoder = new URLParamEncoder();
        try{
            Intent i = new Intent(getApplication(), RacerViewActivity.class);
            String response = "0";
            if(participant.isChecked()) {
                response = http.sendGet("http://racegofer.com/api/JoinRace?raceId=" + encoder.encode(raceId) + "&password=" + encoder.encode(passwordField.getText().toString()) + "&userType=Participant&hidden=" + hideBox.isChecked());
                i.putExtra("user_type", "Participant");
            }
            else if(spectator.isChecked()){
                response = http.sendGet("http://racegofer.com/api/JoinRace?raceId=" + encoder.encode(raceId) + "&password=" + encoder.encode(passwordField.getText().toString()) + "&userType=Spectator&hidden=" + hideBox.isChecked());
                i.putExtra("user_type", "Spectator");
            }
            else if(manager.isChecked()){
                response = http.sendGet("http://racegofer.com/api/JoinRace?raceId=" + encoder.encode(raceId) + "&password=" + encoder.encode(passwordField.getText().toString()) + "&userType=Manager&hidden=" + hideBox.isChecked());
                i.putExtra("user_type", "Manager");
            }

            i.putExtra("race", raceInfo.getString("raceName"));
            i.putExtra("race_id", raceId);
            i.putExtra("hidden", hideBox.isChecked());
            if(response.equals("0")){
                Context context = getApplicationContext();
                CharSequence text = "Incorrect password.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
            else if(response.equals("1")){
                startActivity(i);
            }
            else{
                Context context = getApplicationContext();
                CharSequence text = "There was an error joining the race.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }
        catch (Exception e){
            String err = (e.getMessage()==null)?"JoinRaces Error":e.getMessage();
            Log.e("JoinRaces Error", err);
            Context context = getApplicationContext();
            CharSequence text = "Error in communicating with server.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

    }

    public void backClick(View view){
        this.finish();
    }
}