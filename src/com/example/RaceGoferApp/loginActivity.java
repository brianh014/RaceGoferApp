package com.example.RaceGoferApp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
    EditText usernameText;
    EditText passwordText;
    CheckBox remBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        usernameText = (EditText)findViewById(R.id.usernameText);
        passwordText = (EditText)findViewById(R.id.passwordTextField);
        remBox = (CheckBox)findViewById(R.id.rememberBox);

        SharedPreferences settings = this.getSharedPreferences("com.example.RaceGoferApp", Context.MODE_PRIVATE);
        String username = settings.getString("username", "__failure__");
        String password = settings.getString("password", "__failure__");
        if(username != "__failure__" && password != "__failure__"){
            Intent intent = new Intent(this, RaceListsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        String usernameRem = settings.getString("username_rem", "__failure__");
        String passwordRem = settings.getString("password_rem", "__failure__");
        Boolean remChecked = settings.getBoolean("remChecked", false);

        if(usernameRem != "__failure__" && passwordRem != "__failure__" && remChecked) {
            usernameText.setText(usernameRem);
            passwordText.setText(passwordRem);
            remBox.setChecked(true);
        }

        ActionBar ab = getActionBar();
        ab.hide();
    }

    public void loginButtonClick(View view) {
        //Check login be querying api
        int responseCode;
        HttpConc con = new HttpConc(getApplicationContext());
        try {
            responseCode = con.checkLogin(usernameText.getText().toString(), passwordText.getText().toString());
        }
        catch (Exception e){
            String err = (e.getMessage()==null)?"Login Error":e.getMessage();
            Log.e("Login", err);
            Context context = getApplicationContext();
            CharSequence text = "Error in communicating with server.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        if(responseCode == 401){
            Log.v("Login", "Login failed");
            Context context = getApplicationContext();
            CharSequence text = "Login failed. Wrong username or password.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        else if(responseCode == 200) {
            SharedPreferences settings = this.getSharedPreferences("com.example.RaceGoferApp", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("username", usernameText.getText().toString());
            editor.putString("password", passwordText.getText().toString());
            editor.commit();

            if(remBox.isChecked()){
                editor.putString("username_rem", usernameText.getText().toString());
                editor.putString("password_rem", passwordText.getText().toString());
                editor.putBoolean("remChecked", true);
                editor.commit();
            }
            else{
                editor.putString("username_rem", "__failure__");
                editor.putString("password_rem", "__failure__");
                editor.putBoolean("remChecked", false);
                editor.commit();
            }

            Log.v("Login", "Login success");
            Context context = getApplicationContext();
            CharSequence text = "Login successful.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
        else{
            Log.e("Login", "Error with login");
            Context context = getApplicationContext();
            CharSequence text = "Error in communicating with server.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        Intent intent = new Intent(this, RaceListsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void signupClick(View view){
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }
}
