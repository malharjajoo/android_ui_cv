package com.app.studybuddy;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;

import java.util.Calendar;
import java.text.SimpleDateFormat;


import com.app.studybuddy.GazeTracker.GazeState;
import com.app.studybuddy.NoiseTracker.NoiseData;
import com.app.studybuddy.StatsEngine.StatsSummary;

import java.util.*;

public class StudySession extends AppCompatActivity {

    private String debugTag;
    public enum SessionState {CREATED, STARTED, PAUSED, STOPPED;}

    // Data obtained from CV module.
    public class TimestampedGazeState
    {
        public GazeState state;
        public String time;

        public TimestampedGazeState(GazeState state, String time)
        {
            this.state = state;
            this.time = time;
        }
    }

    // Data obtained from Noise Tracker.
    public class TimestampedNoiseData
    {
        public NoiseData noiseData;
        public String time;

        public TimestampedNoiseData(NoiseData noiseData, String time)
        {
            this.noiseData = noiseData;
            this.time = time;
        }

        public NoiseData getNoiseData()
        {
            return noiseData;
        }
    }


    // Abstract away details of time calculations.
    // Uses a chronometer variable to handle the calculations.
    // Can also handle pausing and re-starting the session.
    public class Timer
    {
        private Chronometer chrono;

        // used to enable pausing functionality
        public long lastPause;

        public long duration;
       // private String startTime;
        //private String endTime;

        public Timer(Chronometer chrono)
        {
            this.duration = 0;
            this.chrono = chrono;
        }

        // returns time duration in string format.
        public String getDuration()
        {
            String duration="";

            try {

                long diff = this.duration;

                long diffSeconds = diff / 1000 % 60;
                long diffMinutes = diff / (60 * 1000) % 60;
                long diffHours = diff / (60 * 60 * 1000) % 24;
                //long diffDays = diff / (24 * 60 * 60 * 1000);

                 duration = Long.toString(diffHours) +":"
                                + Long.toString(diffMinutes)+":"
                                + Long.toString(diffSeconds);

            }catch (Exception e) {
                e.printStackTrace();
            }

            return duration;
        }


        // Helper functions
        // Time related functions, currently return as string
        // but can be changed to a numeric format if required.
        // Simply combination of getDate() and getCurrentTime().
        public String getDateAndTime()
        {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
        }

        public String getDate()
        {
            // can change format if required by simply altering string.
            return new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
        }
        public String getCurrentTime()
        {
            return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
        }


    }


    // Android Parcelable needs to be implemented to pass data between activities.
    // It is an (optimized) alternative to Java Serializable. Check link below.
    // https://stackoverflow.com/questions/3323074/android-difference-between-parcelable-and-serializable
    public static class Summary implements Parcelable
    {
        //TODO: add other sensors later.
        public StatsSummary statsSummary;
        public String sessionDuration;

        //TODO: add other sensors later.
        public Summary(StatsSummary statsSummary, String sessionDuration)
        {
            this.statsSummary = statsSummary;
            this.sessionDuration = sessionDuration;
        }


        protected Summary(Parcel in) {
            statsSummary = in.readParcelable(StatsSummary.class.getClassLoader());
            sessionDuration = in.readString();
        }

        public static Creator<Summary> CREATOR = new Creator<Summary>() {
            @Override
            public Summary createFromParcel(Parcel in) {
                return new Summary(in);
            }

            @Override
            public Summary[] newArray(int size) {
                return new Summary[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(statsSummary, i);
            parcel.writeString(sessionDuration);
        }
    }

    // This class encapsulates a timer, associated buttons
    // and interaction with the sensors.
    public class MySession
    {
        // Can place this in an inner class later.
        private Button startButton;
        private Button pauseButton;
        private Button stopButton;
        private Timer timer;

        private SessionState sessionState;


        private GazeTracker gazeTracker; // reference to CV module
        private NoiseTracker noiseTracker; //reference to Mic module.

        //private StatsEngine statsEngine;
        private List<TimestampedGazeState> cv_data;
        private List<TimestampedNoiseData> noise_data;



        public void initializeSession()
        {
            this.sessionState = SessionState.CREATED;
            this.timer = new Timer( (Chronometer) findViewById(R.id.chronometer) );
            this.initializeButtons();
        }

        public void initializeButtons()
        {
            this.startButton = (Button) findViewById(R.id.start_button);
            this.startButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view)
                {

                    // check if paused state, and start timer accordingly.
                    if(timer.lastPause != 0)
                    {
                        timer.chrono.setBase(timer.chrono.getBase() + SystemClock.elapsedRealtime() - timer.lastPause);
                    }else
                    {
                        // start from 00:00
                        //timer.chrono.getBase() +
                        timer.chrono.setBase(SystemClock.elapsedRealtime());
                    }

                    timer.chrono.start();

                    // ensure user doesn't break anything by re-pressing unnecessary buttons.
                    startButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    startSession();
                }
            });


            this.pauseButton = (Button) findViewById(R.id.pause_button);
            this.pauseButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {

                    timer.lastPause = SystemClock.elapsedRealtime();
                    timer.duration = timer.duration + SystemClock.elapsedRealtime() - timer.chrono.getBase();

                    timer.chrono.stop();

                    // ensure user doesn't break anything by re-pressing unnecessary buttons.
                    pauseButton.setEnabled(false);
                    startButton.setEnabled(true);
                    pauseSession();
                }
            });


            this.stopButton = (Button) findViewById(R.id.stop_button);
            this.stopButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {

                    timer.lastPause = 0; // distinguishes it from a pause.
                    timer.duration = timer.duration + SystemClock.elapsedRealtime() - timer.chrono.getBase();
                    timer.chrono.stop();
                    timer.chrono.setBase(SystemClock.elapsedRealtime());

                    // ensure user doesn't break anything by re-pressing unnecessary buttons.
                    startButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    stopSession();
                }
            });


        }

        // starts a countdown timer of 15s before session is started
        public void startCountdownTimer()
        {


        }

        // On start button click, initialize session
        // and sensors
        public void startSession()
        {

            // Extra layer of safety. Already ensured by disabling appropriate button.
            // Ensure state change is appropriate.
            if(sessionState == SessionState.CREATED)
            {
                this.sessionState = SessionState.STARTED;
                Log.d(debugTag, "Session started ...");

                // Place inside constructor ?
                this.gazeTracker = new GazeTracker();
                this.noiseTracker = new NoiseTracker();


                // TODO: If using background threads, signal sensors to START.
                // For now assuming a Polling model to get data from sensors.
                pollSensors();

            }

        }

        // On pause button click.
        public void pauseSession()
        {
            // TODO: If using background threads, signal sensors to PAUSE.
            Log.d(debugTag, "Session Paused ! ...");
        }

        // On stop button click.
        public void stopSession()
        {

            if(this.sessionState == SessionState.STARTED)
            {
                String sessionDuration = this.timer.getDuration();

                // TODO: If using background threads, signal sensors to STOP.

                this.sessionState = SessionState.STOPPED;

                // Pass on data to Summary Activity.
                // need to serialize the Summary Object and pass.
                StatsEngine statsEngine = new StatsEngine();
                StatsSummary statsSummary = statsEngine.getSummary(this.cv_data, this.noise_data);

                //For now only filling Stats Data. Add other sensors summary later.
                Summary summary = new Summary(statsSummary,sessionDuration);

                sendSummaryForDisplay(summary);

                Log.d(debugTag, "Session Stopped ! ...");
            }


         }


        // Summary is sent for display (after the session is stopped)
         public void sendSummaryForDisplay(Summary summary)
         {
             Intent intent = new Intent(StudySession.this, SessionSummary.class );
             intent.putExtra("Summary",summary);
             startActivity(intent);
         }

        // This method keeps polling the sensors at a certain rate.
        // It appends the timer values to the .
        public void pollSensors()
        {
            List cv_data = new ArrayList<TimestampedGazeState>();
            // TODO: separate noise sensor data from CV sensors if different frequency required ?
            List noise_data = new ArrayList<TimestampedNoiseData>();


            // keep polling CV and sensor module at a certain frequency
            // TODO: What should be the frequency? and how to enforce it ? will depend on each sensor ...
            for(int i=0; i < 100 ; ++i)
            {
                String time = this.timer.getCurrentTime();
                cv_data.add(new TimestampedGazeState(gazeTracker.getData(i),time));
                noise_data.add(new TimestampedNoiseData(noiseTracker.getData(),time));

            }


            Log.d(debugTag, "Finished collecting CV data ...");
            Log.d(debugTag,"Finished collecting Noise Data");
            this.cv_data = cv_data;
            this.noise_data = noise_data;
        }


    }




    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        debugTag = getResources().getString(R.string.debugTag);
        Log.d(debugTag, "Inside StudySession activity ...");

        // use corresponding XML resource to create layout
        setContentView(R.layout.activity_studysession);

        MySession mySession = new MySession();
        // intializes session state and button event handlers.
        mySession.initializeSession();
    }




}
