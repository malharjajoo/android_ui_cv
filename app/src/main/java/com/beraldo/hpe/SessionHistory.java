package com.beraldo.hpe;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.beraldo.hpe.StatsEngine.StatsSummary;

import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;



//==================================================================



public class SessionHistory extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private String debugTag;
    private List<SessionDisplay> displayData;
    private List<StatsSummary> summaryList;
    private RecyclerView rv;

    public class SessionDisplay
    {
        StatsSummary summary;
        int iconId;

        SessionDisplay(StatsSummary summary, int iconId) {
            this.summary = summary;
            this.iconId = iconId;
        }
    }


    private void addIcons(List<StatsSummary> fetchedData){

        Log.d(debugTag, "isniide add icons entries = is: " + summaryList.size());


        int len = fetchedData.size();
        for (int i=0; i<len;i++){

            StatsSummary curSession = fetchedData.get(i);
            //int icon = R.drawable.studybuddy;
            String duration_str = curSession.sessionDuration;
            String[] tokens = duration_str.split(":");
            int hours = Integer.parseInt(tokens[0]);
            int minutes = Integer.parseInt(tokens[1]);
            int seconds = Integer.parseInt(tokens[2]);
            int duration = 3600 * hours + 60 * minutes + seconds;
            //Adding icons based on duration
            if (duration < (30*60)) {
                this.displayData.add(new SessionDisplay(curSession, R.drawable.notebook));
            }else if (duration < (90*60)){
                this.displayData.add(new SessionDisplay(curSession, R.drawable.agenda));
            }else{
                this.displayData.add(new SessionDisplay(curSession, R.drawable.bookshelf));
            }
        }

    }

    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.SessionViewHolder>
    {
        List<SessionDisplay> sessions;

        public class SessionViewHolder extends RecyclerView.ViewHolder {
            CardView cv;
            TextView sessionDescription;
            TextView sessionValues;
            TextView sessionDates;
            ImageView sessionIcon;

            SessionViewHolder(View itemView) {
                super(itemView);
                cv = (CardView)itemView.findViewById(R.id.cv);
                sessionDescription = (TextView)itemView.findViewById(R.id.session_description);
                sessionValues = (TextView)itemView.findViewById(R.id.session_levels);
                sessionDates = (TextView)itemView.findViewById(R.id.session_times);
                sessionIcon = (ImageView)itemView.findViewById(R.id.session_icon);
            }
        }


        RVAdapter(List<SessionDisplay> sessions){
            this.sessions = sessions;
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        @Override
        public SessionViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.session_history_item, viewGroup, false);
            SessionViewHolder pvh = new SessionViewHolder(v);
            return pvh;
        }

        @Override
        public void onBindViewHolder(SessionViewHolder SessionViewHolder, int i) {
            float hrv = sessions.get(i).summary.avg_hr;
            String hrv_str;
            if (hrv == -1){
                hrv_str = "Watch disconnected";
            }else{
                hrv_str = String.format(java.util.Locale.US,"%.0f", sessions.get(i).summary.avg_hr) + "bpm";
            }
            String date = sessions.get(i).summary.startTime;
            String[] tokens = date.split(" ");

            String date_vals = "\nDate: " + tokens[0] + "\nStart time: " + tokens[1] + "\nDuration: " + sessions.get(i).summary.sessionDuration;

            String sensory_vals = "Focused: " + String.format(java.util.Locale.US,"%.2f", sessions.get(i).summary.focussed_percent) +
                    "%\nDistracted: "+ String.format(java.util.Locale.US,"%.2f", sessions.get(i).summary.distracted_percent) +
                    "%\nNot present: "+ String.format(java.util.Locale.US,"%.2f", sessions.get(i).summary.notpresent_percent) +
                    "%\nNoise level: " + sessions.get(i).summary.avg_noise_description +
                    "\nAverage heart rate: " + hrv_str;

            SessionViewHolder.sessionDescription.setText(sessions.get(i).summary.description);
            SessionViewHolder.sessionValues.setText(sensory_vals);
            SessionViewHolder.sessionDates.setText(date_vals);
            SessionViewHolder.sessionIcon.setImageResource(sessions.get(i).iconId);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d(debugTag,"In Session history Activity");
        this.summaryList = new ArrayList<StatsSummary>();
        this.displayData = new ArrayList<SessionDisplay>();

        String firebasename = getResources().getString(R.string.Firebasename);
        this.database = FirebaseDatabase.getInstance();
        this.myRef = database.getReference(firebasename);
        this.debugTag = getResources().getString(R.string.debugTag);

        //Function to fetch data from Firebase
        readFromDatabase();

        setContentView(R.layout.activity_session_history);

        //Display the data
        this.rv = (RecyclerView)findViewById(R.id.rv);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        this.rv.setLayoutManager(llm);


    }


    public void readFromDatabase()
    {


        // Read from the database
        myRef.addListenerForSingleValueEvent(new ValueEventListener(){

            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                summaryList.clear();

                //Map<String, StatsSummary> map = (HashMap<String, StatsSummary>)dataSnapshot.getValue();
                for(DataSnapshot ds : dataSnapshot.getChildren())
                {
                    StatsSummary summary = ds.getValue(StatsSummary.class);
                    Log.d(debugTag, "description is: " + summary.description);
                    Log.d(debugTag, "percentage= is: " + summary.avg_noise);

                    summaryList.add(summary);
                }

                Collections.reverse(summaryList);
                Log.d(debugTag, "entries = is: " + summaryList.size());
                //Function dummy values for now and assign icons
                addIcons(summaryList);

                Log.d(debugTag, "displaydata entries = is: " + displayData.size());

                RVAdapter adapter = new RVAdapter(displayData);
                rv.setAdapter(adapter);

            }

            @Override
            public void onCancelled(DatabaseError error)
            {
                // Failed to read value
                Log.w(debugTag, "Failed to read value.", error.toException());
            }
        });

    }




}