package com.beraldo.hpe;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.List;
import com.beraldo.hpe.StudySession.TimestampedGazeState;
import com.beraldo.hpe.StudySession.TimestampedNoiseData;
import com.beraldo.hpe.GazeTracker.GazeState;
/**
 * Created by malhar on 2/15/18.
 */

public class StatsEngine
{

    // TODO: add heart rate sensors.
    // For now just handles CV and noise sensors.
    // Implementing Android Parcelable class - this is required to pass custom data between activities.
    public static class StatsSummary implements Parcelable
    {
        // outputs from CV module for pie chart
        public float focussed_percent ;
        public float distracted_percent ;
        public float notpresent_percent ;

        // output from noise tracker
        public float avgNoise;


        public StatsSummary(float focussed_percent,float distracted_percent,
                       float notPresent , float avgNoise)
        {
            this.focussed_percent = focussed_percent;
            this.distracted_percent = distracted_percent;
            this.notpresent_percent = notPresent;

            this.avgNoise = avgNoise;
        }

        protected StatsSummary(Parcel in)
        {
            this.focussed_percent = in.readFloat();
            this.distracted_percent = in.readFloat();
            this.notpresent_percent = in.readFloat();
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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeFloat(focussed_percent);
            parcel.writeFloat(distracted_percent);
            parcel.writeFloat(notpresent_percent);
        }
    }


    public StatsSummary getSummary(List<TimestampedGazeState> cv_data,
                                   List<TimestampedNoiseData> noise_data)
    {
        float focussed_percent,distracted_percent,notpresent_percent;
        float avg_noise;

        // =========== For CV data ====================
        // For now just finds percentage of total entries...
        int len = cv_data.size();
        //Log.d("StudyBuddy","Len of cv_dat="+len);
        int sum1  = 0;
        int sum2  = 0;
        for(int i=0; i < len ; i++)
        {
            if(cv_data.get(i).state == GazeState.PRESENT_FOCUSSED)
            {
                sum1 = sum1 + 1;
            }
            else if(cv_data.get(i).state == GazeState.PRESENT_DISTRACTED)
            {
                sum2 = sum2 + 1;
            }

        }

        // ======== For noise data =============
        int len2 = noise_data.size();
        int sum3 = 0 ;
        for(int i=0; i < len2 ; i++)
        {
            sum3 = sum3 + noise_data.get(i).getNoiseData().getNoiseLevel();
        }


        //Log.d("StudyBuddy","sum is jnow ="+sum1);

        focussed_percent = ((float) sum1/ (float) len) * 100;
        distracted_percent = ((float) sum2/ (float) len) * 100;
        notpresent_percent = 100 - (focussed_percent + distracted_percent);

        avg_noise = (float)sum3/(float)len2;

        return new StatsSummary(focussed_percent,distracted_percent,notpresent_percent,avg_noise);

    }
}
