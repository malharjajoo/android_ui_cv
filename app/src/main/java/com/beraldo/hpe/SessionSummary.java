package com.beraldo.hpe;


import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

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
            // =============================================

            // Find the piechart from xml layout file.
            PieChart pieChart = (PieChart) findViewById(R.id.chart);
            setChartData(summary, pieChart);



        }

        // Move to navigation menu.
        this.navigationButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                openNavigationDrawerActivity();
            }
        });


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