package com.example.RaceGoferApp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by Brian on 11/20/2014.
 */
public class SignupActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        ActionBar ab = getActionBar();
        ab.setTitle("Create your account");
    }

    public void createAccount(View view){
        EditText username = (EditText)findViewById(R.id.usernameField);
        EditText firstname = (EditText)findViewById(R.id.firstnameField);
        EditText lastname = (EditText)findViewById(R.id.lastnameField);
        EditText email = (EditText)findViewById(R.id.emailField);
        EditText phone = (EditText)findViewById(R.id.phoneField);
        EditText password = (EditText)findViewById(R.id.passwordField);

        if(username.getText().toString().equals("") || firstname.getText().toString().equals("") || lastname.getText().toString().equals("") || email.getText().toString().equals("") || phone.getText().toString().equals("") || password.getText().toString().equals("")){
            Context context = getApplicationContext();
            CharSequence text = "Please fill in all fields.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        URLParamEncoder encoder = new URLParamEncoder();
        String url = "http://racegofer.com/api/RegisterUser?" +
                "userName=" + encoder.encode(username.getText().toString()) +
                "&password=" + encoder.encode(password.getText().toString()) +
                "&firstName=" + encoder.encode(firstname.getText().toString()) +
                "&lastName=" + encoder.encode(lastname.getText().toString()) +
                "&phoneNumber=" + encoder.encode(phone.getText().toString()) +
                "&email=" + encoder.encode(email.getText().toString());
        HttpConc http = new HttpConc(getApplicationContext());
        try {
            http.sendGetNoAuth(url);
            SharedPreferences settings = this.getSharedPreferences("com.example.RaceGoferApp", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("username", username.getText().toString());
            editor.putString("password", password.getText().toString());
            editor.putString("username_rem", "__failure__");
            editor.putString("password_rem", "__failure__");
            editor.putBoolean("remChecked", false);
            editor.commit();
            Intent intent = new Intent(this, RaceListsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        catch (Exception e){
            String err = (e.getMessage()==null)?"Signup Error":e.getMessage();
            Log.e("Signup", err);
            Context context = getApplicationContext();
            CharSequence text = "Error in communicating with server.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
    }
}