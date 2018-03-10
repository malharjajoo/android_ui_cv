package com.beraldo.hpe;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SessionCountDown extends AppCompatActivity {

    private String debugTag ;

    private Timer timer;
    private Button resetButton;

    public class Timer
    {
        private long countDownInterval = 1000;
        public TextView countdownTextview;
        private CountDownTimer countDownTimer;
        private long timeLeft;
        private long countdown;
        private boolean isRunning;

        public Timer(long countdown)
        {
            this.isRunning = false;
            this.countdown = countdown;
            this.timeLeft = countdown;
            this.countdownTextview = (TextView) findViewById(R.id.text_view_countdown);

            //textview is usually set to 00:00, hence update with countdown value.
            this.updateTextView();
        }

        public void startTimer()
        {
            this.countDownTimer = new CountDownTimer(timeLeft,countDownInterval) {
                @Override
                public void onTick(long millisUntilFinished) {

                    timeLeft = millisUntilFinished ;
                    updateTextView();
                }

                @Override
                public void onFinish()
                {
                    isRunning = false;
                    Log.d(debugTag,"Finished countdown timer !!...");
                    openPermissionsActivity();
                }

            }.start();

            this.isRunning = true;

        }

        public void updateTextView()
        {
            int minutes = (int) (timeLeft / 1000) / 60;
            int seconds = (int) (timeLeft / 1000) % 60;

            String timeLeftFormatted = String.format( "%02d:%02d", minutes, seconds);

            this.countdownTextview.setText(timeLeftFormatted);
        }

        public void cancelTimer()
        {
            this.countDownTimer.cancel();
        }
        // on reset button click
        public void resetTimer()
        {
            this.countDownTimer.cancel();
            this.timeLeft = countdown;
            updateTextView();
            this.countDownTimer.start();
        }


        /*
        public void saveInstanceState(Bundle outstate)
        {
            //outstate.putLong("timeLeft",)
        }
        */
    }


    //===============================

    public void initializeButtons()
    {
        this.resetButton.setOnClickListener(new View.OnClickListener(){

            public void onClick(View view){
                timer.resetTimer();
            }

        });
    }


    // Handle Restoring state when orientation is changed.
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_count_down);
        this.debugTag = getResources().getString(R.string.debugTag);

        // Create countdown timer
        long countdown_ms = 5000;
        this.timer = new Timer(countdown_ms);

        // cache XML layout elements..
        this.resetButton = (Button)findViewById(R.id.button_reset);

        initializeButtons();
        this.timer.startTimer();
        /*
        // Used when mobile changes orientation (from vertical to landscape)
        if (savedInstanceState != null) {
            this.timer.restoreInstanceState(savedInstanceState);
        }
        */

    }


    // make sure to do this otherwise next activity will be triggered.
    @Override
    public void onPause()
    {
        super.onPause();
        this.timer.cancelTimer();
    }




    /*
    // handle Saving state when orientation is changed.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        this.timer.saveInstanceState(outState);
    }
    */

    public void openPermissionsActivity()
    {
        Intent intent = new Intent(SessionCountDown.this, Permissions_.class );
        Log.d(debugTag, "Opening Permissions activity ...");
        startActivity(intent);
    }


}