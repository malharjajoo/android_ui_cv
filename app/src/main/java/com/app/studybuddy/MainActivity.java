package com.app.studybuddy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.widget.Button;

import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.widget.ImageView;

import com.app.studybuddy.R;


public class MainActivity extends AppCompatActivity {

    private Button loginButton;
    private String debugTag ;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        this.debugTag = getResources().getString(R.string.debugTag);

        loginButton = (Button) findViewById(R.id.loginbutton);
        loginButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                openCountDownActivity();
            }
        });

        // convert image to rounded
        //makeCircularImage((ImageView) findViewById(R.id.image_studybuddylogo));
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI();

    public void openCountDownActivity()
    {
        Intent intent = new Intent(MainActivity.this, SessionCountDown.class );
        Log.d(debugTag, "Opening StudySession activity ...");
        startActivity(intent);
    }

    public void makeCircularImage(ImageView imageView)
    {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.studybuddy);
        RoundedBitmapDrawable rd = RoundedBitmapDrawableFactory.create(getResources(),bitmap);
        rd.setCircular(true);
        imageView.setImageDrawable(rd);
    }

}
