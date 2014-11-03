package com.example.RaceGoferApp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class LoginActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
    }

    public void loginButtonClick(View view) {
        //Temporarily goes to race lists
        Intent intent = new Intent(this, RaceListsActivity.class);
        startActivity(intent);
    }
}
