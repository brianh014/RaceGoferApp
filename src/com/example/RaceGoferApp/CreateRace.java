package com.example.RaceGoferApp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Created by Brian on 11/3/2014.
 */
public class CreateRace extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.createrace);

        ActionBar ab = getActionBar();
        ab.setTitle("Create Race");
    }

    public void nextButtonClick(View view) {
        Intent intent = new Intent(this, CheckpointChooser.class);
        startActivity(intent);
    }
}