package com.beraldo.hpe;


import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

import com.beraldo.hpe.StatsEngine.StatsSummary;


public class SessionSummary extends AppCompatActivity {


    private String debugTag;
    private Button navigationButton;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.debugTag = getResources().getString(R.string.debugTag);


        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //  activity_navigationsidebar
        setContentView(R.layout.activity_session_summary);
        this.navigationButton = (Button) findViewById(R.id.homebutton);

        // Get data from session
        Intent in = getIntent();
        StatsSummary summary = in.getExtras().getParcelable("Summary");


        if (summary == null)
        {
            Log.d(debugTag, "No summary available");
        }
        else
        {

            Log.d(debugTag,"summary CV percentages="+Float.toString(summary.focussed_percent)+","+Float.toString(summary.distracted_percent));
            Log.d(debugTag,"summary Avg noise="+ Float.toString(summary.avg_noise));
            Log.d(debugTag, "Session duration=" + summary.sessionDuration);
            Log.d(debugTag,"hrv is: " + String.format(java.util.Locale.US,"%.0f",summary.avg_hr) );
            Log.d(debugTag,"avg noise description: " + summary.avg_noise_description);
            Log.d(debugTag,"avg_noise: " + String.format(java.util.Locale.US,"%.0f",summary.avg_noise) );
            // =============================================

            // Find the piechart from xml layout file.
            PieChart pieChart = (PieChart) findViewById(R.id.chart);
            setChartData(summary, pieChart);
            String soundTxt = drawSpeaker(summary);
            prepareText(summary,soundTxt);



        }

        // Move to navigation menu.
        this.navigationButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                openNavigationDrawerActivity();
            }
        });


    }

    public String drawSpeaker(StatsSummary summary)
    {
        String speaker_text;
        ImageView speaker = (ImageView) findViewById(R.id.speaker);
        String avg_noise_val = summary.avg_noise_description;
        //Log.d(debugTag,);



        if (avg_noise_val.equals("Quiet"))
        {
            speaker.setImageResource(R.drawable.quiet);
            speaker_text = "Quiet environment: Quietness helps some people to focus more, good job on finding suitable environment!";
        }else if (avg_noise_val.equals("Conversation Level"))
        {
            speaker.setImageResource(R.drawable.conversation);
            speaker_text = "Conversation level: A reasonable amount of noise, most people are not disturbed by it.";
        }else {
            speaker.setImageResource(R.drawable.loud);
            speaker_text = "Too loud: In addition of being distracting, prolonged periods in loud environment are unhealthy. Try to find more quiet room in the future.";
        }
        return speaker_text;
    }

    public void prepareText(StatsSummary summary, String speakText)
    {
        TextView attention = (TextView) findViewById(R.id.attention_text);
        TextView heart = (TextView) findViewById(R.id.heart_text);
        TextView sound = (TextView) findViewById(R.id.sound_text);

        sound.setText(speakText);

        String attention_comment;
        String hear_comment;

        if (summary.focussed_percent < 30){
            attention_comment = "Very poor attention level, this study session most likely did not help you at all :(";
        }else if (summary.focussed_percent < 60){
            attention_comment = "At least you studied for a while. You should consider paying more attention in the future to better utilize your time.";
        }else
        {
            attention_comment = "Nice! You tried to stay on topic during your session. Keep it up for better study results :)";
        }


        if (summary.avg_hr == -1){
            hear_comment = "Watch was not connected. Consider using wearable heart monitoring so we can provide better information for you.";
        }else if (summary.avg_hr < 60){
            hear_comment = String.format(java.util.Locale.US,"%.0f",summary.avg_hr) + "bpm" +
                    "\nAre you an athlete? If not, you heartbeat was a bit too low. We hope the studying was not that boring!";
        }else if (summary.avg_hr < 100){
            hear_comment = String.format(java.util.Locale.US,"%.0f",summary.avg_hr) + "bpm" +
                    "\nHealthy heartbeat values, everything in norm. Keep it up!";
        }else {
            hear_comment = String.format(java.util.Locale.US,"%.0f",summary.avg_hr) + "bpm" +
                    "\nYou heartbeat is too high! Try to get a bit more relaxed, it is not worth your health.";
        }

        heart.setText(hear_comment);
        attention.setText(attention_comment);


    }

    public void setChartData(StatsSummary summary, PieChart pieChart)
    {

        // set a bunch of properties
        Description desc = new Description();
        desc.setText("");
        pieChart.setDescription(desc);
        pieChart.setHoleRadius(10f);

        // create a list of pie chart entries
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(summary.focussed_percent,"Focused"));
        entries.add(new PieEntry(summary.distracted_percent,"Distracted"));
        entries.add(new PieEntry(summary.notpresent_percent,"Not Present"));

        PieDataSet dataset = new PieDataSet(entries,"StudySession Results");

        // just create a bunch of colours ...
        final int[] COLORS = {
                Color.rgb(0x81,0xc7,0x84),
                Color.rgb(0xff,0x8a,0x65),
                Color.rgb(0x64,0xb5,0xf6),
                //Color.rgb(255,51,153)
        };
        ArrayList<Integer> colors = new ArrayList<Integer>();
        for(int i = 0 ; i < entries.size() ; i++ )
        {
            colors.add(COLORS[i]);
        }

        dataset.setColors(colors);

        PieData data = new PieData(dataset);
        pieChart.setData(data);
        pieChart.invalidate();

    }

    public void openNavigationDrawerActivity()
    {
        Intent intent = new Intent(this, NavigationDrawer.class );
        Log.d(debugTag, "Opening NvaigationDrawer activity ...");
        startActivity(intent);
    }


}