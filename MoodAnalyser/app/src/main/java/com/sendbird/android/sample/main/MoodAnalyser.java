package com.sendbird.android.sample.main;

import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import com.sendbird.android.sample.R;

public class MoodAnalyser extends TabActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_analyser);

        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);

        tabHost.setup();

        //creating tab menu

        TabSpec Tab1 = tabHost.newTabSpec("tab1");
        TabSpec Tab2 = tabHost.newTabSpec("tab2");



        //setting tab2 name
        Tab1.setIndicator("Homepage");
        //set activity

        Tab1.setContent(new Intent(this, HomePage.class));


        //setting tab2 name
        Tab2.setIndicator("Chatting");
        //set activity
        Tab2.setContent(new Intent(this, MainActivity.class));

        TabHost tabHost1 = getTabHost();
        for(int i=0;i<tabHost1.getTabWidget().getChildCount();i++)
        {
            TextView tv = (TextView) tabHost1.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextColor(Color.WHITE);
        }

        //adding tabs
        tabHost.addTab(Tab1);
        tabHost.addTab(Tab2);
    }
}