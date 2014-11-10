package com.example.RaceGoferApp;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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
        EditText racename = (EditText)findViewById(R.id.racename);
        EditText type = (EditText)findViewById(R.id.type);
        EditText location = (EditText)findViewById(R.id.location);
        EditText password = (EditText)findViewById(R.id.password);
        EditText managerpassword = (EditText)findViewById(R.id.managerpassword);

        if(racename.getText().toString().equals("") || type.getText().toString().equals("") || location.getText().toString().equals("") || managerpassword.getText().toString().equals("")) {
            Context context = getApplicationContext();
            CharSequence text = "Please fill out all required fields.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
        else {
            Intent intent = new Intent(this, CheckpointChooser.class);
            intent.putExtra("race_name", racename.getText().toString());
            intent.putExtra("type", type.getText().toString());
            intent.putExtra("location", location.getText().toString());
            intent.putExtra("password", password.getText().toString());
            intent.putExtra("manager_password", managerpassword.getText().toString());
            startActivity(intent);
        }
    }
}

