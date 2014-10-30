package com.example.RaceGoferApp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class loginActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
    }

    public void loginButtonClick(View view) {
        //Temporarily goes to racer view
        Intent intent = new Intent(this, racerViewActivity.class);
        startActivity(intent);
    }
}
