package com.example.RaceGoferApp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Brian on 11/18/2014.
 */
public class JoinRaceActivity extends Activity {
    TextView raceText;
    TextView typeText;
    TextView locationText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joinrace);
        Intent i = getIntent();
        JSONObject raceInfo;

        raceText = (TextView)findViewById(R.id.raceNameText);
        typeText = (TextView)findViewById(R.id.typeText);
        locationText = (TextView)findViewById(R.id.locationText);

        HttpConc http = new HttpConc(getApplicationContext());
        URLParamEncoder encoder = new URLParamEncoder();
        try{
            String response = http.sendGet("http://racegofer.com/api/GetRaceInfo?raceId=" + encoder.encode(i.getStringExtra("race_id")));
            raceInfo = new JSONObject(response);
            raceText.setText(raceInfo.getString("raceName"));
            typeText.setText(raceInfo.getString("raceType"));
            locationText.setText(raceInfo.getString("location"));
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
    }

    public void radioGroupClick(View view){

    }

    public void raceClick(View view){

    }

    public void backClick(View view){
        this.finish();
    }
}