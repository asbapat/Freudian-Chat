package com.sendbird.android.sample.main;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.annotation.RestrictTo;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import com.sendbird.android.sample.R;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;



public class HomePage extends AppCompatActivity {
    static public float sentimentPolarity =-4L;
    static public float sentimentMagnitude =-4f;
    static public int  positiveCount =0;
    static public int  negativeCount =0;
    private static YouTube youtube;
    private static long NUMBER_OF_VIDEOS_RETURNED = 1;
    static public String[] movieNames;
    static public long lastAnalyzedTime =System.currentTimeMillis();
    LinearLayout lView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        System.out.println("sentiment here:"+sentimentPolarity);
//        try {
//            String[] queryTerms = getInputQuery();
//            new GetMediaLinks().execute(queryTerms);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    public class Wrapper
    {
        public String[] resultLinks;
        public String [] titles;
    }

    private static String[] getInputQuery() throws IOException {

        long timeDiff= (System.currentTimeMillis()-lastAnalyzedTime)/(60000 *60);

        if(timeDiff > 3){
            movieNames = new String[0];
        }

        if(movieNames==null || movieNames.length <=1){
            movieNames = new String[1];
            movieNames[0] ="Top 10 English Movies";
            System.out.println("it comes inside");
        }else{
            for(int i=0;i<movieNames.length;i++){
                System.out.println("it comes inside 2");
                movieNames[i] += " scenes";
            }
        }

        return movieNames;
    }

    @Override
    public void onResume() {

        try {
            String[] queryTerms = getInputQuery();
            System.out.println("queryTerms length here:"+queryTerms.length);
            new GetMediaLinks().execute(queryTerms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onResume();

    }

    public class GetMediaLinks extends AsyncTask<String[], String, Wrapper> {


        public GetMediaLinks() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected Wrapper doInBackground(String[]... params) {
            String[] links;
            Wrapper w = new Wrapper();
            if(params[0].length!=1) {
                links = new String[params[0].length];
                w.titles = new String[params[0].length];
                w.resultLinks = new String[params[0].length];
            }else{
                links= new String[10];
                w.titles = new String[10];
                w.resultLinks = new String[10];
            }
            int count =0;


            try{
                youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
                    public void initialize(HttpRequest request) throws IOException {
                    }
                }).setApplicationName("MoodAnalyser").build();

                YouTube.Search.List search = youtube.search().list("id,snippet");
                String apiKey ="AIzaSyCvWIJTQid5LFSn2yfHZhWLasqR4D2ba84";
                search.setKey(apiKey);
                String [] movieScenes = params[0];
                if(movieScenes.length == 1)NUMBER_OF_VIDEOS_RETURNED=10;
                else NUMBER_OF_VIDEOS_RETURNED =1;

                for(int i=0;i<movieScenes.length;i++) {
                    System.out.println("movieScenes [i]"+movieScenes[i]);
                    search.setQ(movieScenes[i]);

                    search.setType("video");
                    search.setFields("items(id/kind,id/videoId,snippet/title)");
                    search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                    SearchListResponse searchResponse = search.execute();
                    List<SearchResult> searchResultList = searchResponse.getItems();
                    System.out.println("searchResultList:" + searchResultList);
                    if (searchResultList != null) {
                        Iterator<SearchResult> iteratorSearchResults = searchResultList.iterator();
                        while (iteratorSearchResults.hasNext()) {
                            SearchResult singleVideo = iteratorSearchResults.next();
                            ResourceId rId = singleVideo.getId();
                            String videoID = rId.getVideoId();
                            w.titles[count]= singleVideo.getSnippet().getTitle();
                            links[count] = "https://www.youtube.com/watch?v="+videoID;
                            w.resultLinks[count] = links[count];
                            count++;
                        }
                    }
                }
            }catch (GoogleJsonResponseException e) {
                System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                        + e.getDetails().getMessage());
            } catch (IOException e) {
                System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
            } catch (Throwable t) {
                System.out.println("in throwable error");
                t.printStackTrace();
            }
            System.out.println("w here:"+w);
            System.out.println("w resultlinks:"+w.resultLinks.length);
            System.out.println("w titles:"+w.titles.length);
            return w;

        }

        @Override
        protected void onPostExecute(Wrapper result) {
            ScrollView sv = new ScrollView(HomePage.this);
            LinearLayout lView = new LinearLayout(HomePage.this);
            lView.setOrientation(LinearLayout.VERTICAL);
            sv.addView(lView);
            TextView tViewHead = new TextView(HomePage.this);
            tViewHead.setTypeface(null, Typeface.BOLD);
            tViewHead.setTextSize(18);
            tViewHead.setTextColor(Color.RED);
            if(sentimentPolarity ==-4){
                tViewHead.setText("Here are some list of video clips for you to view\n");
            }else if(sentimentPolarity ==-1){
                tViewHead.setText("Here are some list of video clips for you to cheer up\n");

            }else if(sentimentPolarity ==1){
                tViewHead.setText("Here are some list of video clips for you to enjoy\n");
            }else {
                tViewHead.setText("Here are some list of video clips for you\n");
            }
            lView.addView(tViewHead);

            for( int i = 0; i < result.resultLinks.length; i++) {
                TextView tView1 = new TextView(HomePage.this);
                tView1.setId(i);
                tView1.setTypeface(null, Typeface.BOLD);
                tView1.setText(result.titles[i] + "\n");
                System.out.println("link here:"+result.resultLinks[i]);
                TextView tView2 = new TextView(HomePage.this);
                tView2.setText(result.resultLinks[i] + "\n");
                Linkify.addLinks(tView2, Linkify.WEB_URLS);

                lView.addView(tView1);
                lView.addView(tView2);
            }
            setContentView(sv);


        }
    }


}