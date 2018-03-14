package com.beraldo.hpe;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by malhar on 2/15/18.
 */

public class StatsEngine
{

    private String debugTag ;
    //private List<TimestampedGazeState> cv_data;
    //private List<TimestampedNoiseData> noise_data;

    public StatsEngine()
    {
        this.debugTag = "StudyBuddy";
        //this.cv_data = new ArrayList<TimestampedGazeState>();
        //this.noise_data = new ArrayList<TimestampedNoiseData>();
    }

/*
    // Data obtained from CV module.
    public class TimestampedGazeState
    {
        public OnGetImageListener.State state;
        public String time;

        public TimestampedGazeState(OnGetImageListener.State state, String time)
        {
            this.state = state;
            this.time = time;
        }
    }

    // Data obtained from Noise Tracker.
    public class TimestampedNoiseData
    {
        public int noiseData;
        public String time;

        public TimestampedNoiseData(int noiseData, String time)
        {
            this.noiseData = noiseData;
            this.time = time;
        }
    }
*/


    // This method makes inference from the sensor data.
    // It returns a description of the session.
    public String makeInference(List<OnGetImageListener.State> cv_data_raw,
                              ArrayList<Integer> noise_data_raw,
                               String dayDateTime,String sessionDuration)
    {
        // TODO: split session duration equally among sensor data and create description.
        String day = dayDateTime.substring(0,3);
        String month = dayDateTime.substring(4, 4+3);
        int hour = Integer.parseInt(dayDateTime.substring(11,11+2));
        String time_of_day; //depends on ^hour
        String dd = dayDateTime.substring(8,8+2);
        String year = dayDateTime.substring(30,30+4);


        Log.d(debugTag,"curren date and time=" + dayDateTime);
        // Morning, afternoon , evening, night
        if( hour < 12)
        {
            time_of_day = "Morning";
        }else if(hour < 16)
        {
            time_of_day = "Afternoon";
        }else if(hour < 20)
        {
            time_of_day = "Evening";
        }
        else if(hour < 4)
        {
            time_of_day = "Night";
        }else
        {
            time_of_day = "Early morning";
        }

        String description = "Your " + day + " " + time_of_day + " session ("+dd + " " + month +"," +year+ ")" ;

        return description;
    }



    // TODO: add heart rate sensors.
    // For now just handles CV and noise sensors.
    // Implementing Android Parcelable class - this is required to pass custom data between activities.
    public static class StatsSummary implements Parcelable
    {
        // session data
        public String startTime;
        public String sessionDuration;
        public String description;

        // outputs from CV module for pie chart
        public float focussed_percent ;
        public float distracted_percent ;
        public float notpresent_percent ;

        // output from noise tracker
        public float avg_noise;
        public String avg_noise_description;

        // output from HR watch
        public float avg_hr;


        public StatsSummary()
        {

        }

        public StatsSummary(float focussed_percent,float distracted_percent,
                            float notPresent , float avgNoise, float avg_hr, String avg_noise_description, String description,
                            String startTime,String sessionDuration)
        {
            this.startTime = startTime;
            this.sessionDuration = sessionDuration;
            this.description = description;

            this.focussed_percent = focussed_percent;
            this.distracted_percent = distracted_percent;
            this.notpresent_percent = notPresent;

            this.avg_noise = avgNoise;
            this.avg_hr = avg_hr;
            this.avg_noise_description = avg_noise_description;
        }


        protected StatsSummary(Parcel in) {
            startTime = in.readString();
            sessionDuration = in.readString();
            description = in.readString();
            focussed_percent = in.readFloat();
            distracted_percent = in.readFloat();
            notpresent_percent = in.readFloat();
            avg_noise = in.readFloat();
            avg_hr = in.readFloat();
            avg_noise_description = in.readString();

        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(startTime);
            dest.writeString(sessionDuration);
            dest.writeString(description);
            dest.writeFloat(focussed_percent);
            dest.writeFloat(distracted_percent);
            dest.writeFloat(notpresent_percent);
            dest.writeFloat(avg_noise);
            dest.writeFloat(avg_hr);
            dest.writeString(avg_noise_description);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<StatsSummary> CREATOR = new Creator<StatsSummary>() {
            @Override
            public StatsSummary createFromParcel(Parcel in) {
                return new StatsSummary(in);
            }

            @Override
            public StatsSummary[] newArray(int size) {
                return new StatsSummary[size];
            }
        };
    }


    public StatsSummary getSummary(List<OnGetImageListener.State> cv_data_raw,
                                   ArrayList<Integer> noise_data_raw, ArrayList<Integer> heart_data_raw,
                                   String sessionStartTime,String sessionDuration, String dayDateTime)


    {
        float focussed_percent, distracted_percent, notpresent_percent;
        float avg_noise;
        float avg_hr;
        String avg_noise_description;
        String description;


        // Divide session duration equally for sensor data.
        description = makeInference(cv_data_raw, noise_data_raw,dayDateTime, sessionDuration);


        // =========== For CV data ====================
        // For now just finds percentage of total entries...
        int len1 = cv_data_raw.size();
        //Log.d(debugTag,"Len of cv_dat="+len);
        int sum1  = 0;
        int sum2  = 0;
        for(int i=0; i < len1 ; i++)
        {
            if(cv_data_raw.get(i) == OnGetImageListener.State.PAYING_ATTENTION)
            {
                sum1 = sum1 + 1;
            }
            else if(cv_data_raw.get(i) == OnGetImageListener.State.NOT_PAYING_ATTENTION)
            {
                sum2 = sum2 + 1;
            }

        }

        // ======== For noise data =============
        int len2 = noise_data_raw.size();
        int sum3 = 0 ;
        for(int i=0; i < len2 ; i++)
        {
            sum3 = sum3 + noise_data_raw.get(i);
        }

        avg_noise = (float)sum3/(float)len2;

        // ======= For noise description =======

        if(avg_noise < 50){
            avg_noise_description = "Quiet";
        }
        else if(avg_noise >= 50 && avg_noise <= 400){
            avg_noise_description = "Conversation Level";
        }
        else{
            avg_noise_description = "Too Loud";
        }

        //========== For HR description =====
        int len_hr = heart_data_raw.size();
        if (len_hr == 0){
            avg_hr = -1;
        }else {
            int sum_hr = 0;
            for (int i = 0; i < len_hr; i++) {
                sum_hr = sum_hr + heart_data_raw.get(i);
            }
            avg_hr = (float) sum_hr / (float) len_hr;
        }
        // Summarize the data.
        focussed_percent = ((float) sum1 / (float) len1) * 100;
        distracted_percent = ((float) sum2 / (float) len1) * 100;
        notpresent_percent = 100 - (focussed_percent + distracted_percent);

        return new StatsSummary(focussed_percent,distracted_percent,notpresent_percent,avg_noise,
                avg_hr, avg_noise_description, description,sessionStartTime,sessionDuration);

    }

}
