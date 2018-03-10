package com.beraldo.hpe;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import android.support.v7.app.AppCompatActivity;

import android.view.View;


public class MainActivity extends AppCompatActivity {

    private Button loginButton;
    private String debugTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        this.debugTag = getResources().getString(R.string.debugTag);
        this.loginButton = (Button) findViewById(R.id.loginbutton);
        this.loginButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                openNavigationDrawerActivity();
            }
        });

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI();

    public void openNavigationDrawerActivity()
    {
        Intent intent = new Intent(MainActivity.this, NavigationDrawer.class );
        Log.d(debugTag, "Opening NvaigationDrawer activity ...");
        startActivity(intent);
    }

}