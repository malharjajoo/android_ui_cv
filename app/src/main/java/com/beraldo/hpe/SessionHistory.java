package com.beraldo.hpe;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class SessionHistory extends AppCompatActivity {

    class SessionDisplay {
        StudySession.Summary summary;
        int iconId;

        SessionDisplay(StudySession.Summary summary, int iconId) {
            this.summary = summary;
            this.iconId = iconId;
        }
    }

    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.SessionViewHolder>{

        public class SessionViewHolder extends RecyclerView.ViewHolder {
            CardView cv;
            TextView sessionDuration;
            TextView sessionValues;
            ImageView sessionIcon;

            SessionViewHolder(View itemView) {
                super(itemView);
                cv = (CardView)itemView.findViewById(R.id.cv);
                sessionDuration = (TextView)itemView.findViewById(R.id.session_duration);
                sessionValues = (TextView)itemView.findViewById(R.id.session_values);
                sessionIcon = (ImageView)itemView.findViewById(R.id.session_icon);
            }
        }
        List<SessionDisplay> sessions;

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
            SessionViewHolder.sessionDuration.setText(sessions.get(i).summary.sessionDuration);
            SessionViewHolder.sessionValues.setText("Paying attention: " + sessions.get(i).summary.statsSummary.focussed_percent + "%\nNot paying attention: "+ sessions.get(i).summary.statsSummary.distracted_percent +"%\nNot present: "+sessions.get(i).summary.statsSummary.notpresent_percent);
            SessionViewHolder.sessionIcon.setImageResource(sessions.get(i).iconId);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

    }

    private List<SessionDisplay> displayData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_history);

        //TODO:Function to fetch data from Firebase

        //Function dummy values for now and assign icons
        addIcons(getDummyData());

        //Display the data

        RecyclerView rv = (RecyclerView)findViewById(R.id.rv);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);

        RVAdapter adapter = new RVAdapter(displayData);
        rv.setAdapter(adapter);
    }

    private List<StudySession.Summary> getDummyData(){

        List<StudySession.Summary> fetchedData = new ArrayList<StudySession.Summary>();

        for (int i=0;i<10;i++){
            fetchedData.add(new StudySession.Summary(new StatsEngine.StatsSummary(74,20,6,65), "1h:30m:32s"));
        }

        return fetchedData;

    }

    private void addIcons(List<StudySession.Summary> fetchedData){

        displayData = new ArrayList<SessionDisplay>();

        for (int i=0; i<fetchedData.size();i++){
            StudySession.Summary curSession = fetchedData.get(i);
            int icon = R.drawable.studybuddy;
            //TODO:Create the "if" logic based on the duration to add the icons
            //if (curSession.)
            displayData.add(new SessionDisplay(curSession, icon));
        }

    }
}
