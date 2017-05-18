package com.sendbird.android.sample.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.util.IOUtils;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.sample.R;
import com.sendbird.android.sample.groupchannel.GroupChannelActivity;
import com.sendbird.android.sample.groupchannel.GroupChatFragment;
import com.sendbird.android.sample.model.SentimentInfo;
import com.sendbird.android.sample.utils.PreferenceUtils;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.sendbird.android.sample.main.HomePage.lastAnalyzedTime;
import static com.sendbird.android.sample.main.HomePage.movieNames;
import static com.sendbird.android.sample.main.HomePage.sentimentPolarity;
import static com.sendbird.android.sample.main.HomePage.positiveCount;
import static com.sendbird.android.sample.main.HomePage.negativeCount;

public class MainActivity extends AppCompatActivity implements ApiFragment.Callback{

    private Toolbar mToolbar;
    private NavigationView mNavView;

    private static final int LOADER_ACCESS_TOKEN = 1;
    private static final int API_SENTIMENT = 0;

    private static final String FRAGMENT_API = "api";
    private static final String STATE_SHOWING_RESULTS = "showing_results";
    private ResultPagerAdapter mAdapter;
    private ViewPager mViewPager;
    private EditText mInput;
    LayoutInflater factory ;
    ApiFragment mFragment;
    static public float sentimentMagnitude =0L;

    static int maxPolarityCount=0;
    static int messageListSize =0;
    static int messageListAnalyzedCount =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(mToolbar);
        factory = getLayoutInflater();
        maxPolarityCount=0;

        final FragmentManager fm = getSupportFragmentManager();
        mAdapter = new ResultPagerAdapter(fm, this);
        System.out.println("oncreate in main activity:");

        mNavView = (NavigationView) findViewById(R.id.nav_view_main);
        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_item_group_channels) {
                    Intent intent = new Intent(MainActivity.this, GroupChannelActivity.class);
                    startActivity(intent);
                    return true;

                } else if (id == R.id.nav_item_disconnect) {
                    // Unregister push tokens and disconnect
                    disconnect();
                    return true;
                }

                return false;
            }
        });


        if (savedInstanceState == null) {
            // The app has just launched; handle share intent if it is necessary
            handleShareIntent();
        }
        if (getApiFragment() == null) {
            fm.beginTransaction().add(new ApiFragment(), FRAGMENT_API).commit();
        }
        prepareApi();

//        // Displays the SDK version in a TextView
//        String sdkVersion = String.format(getResources().getString(R.string.all_app_version),
//                BaseApplication.VERSION, SendBird.getSDKVersion());
//        ((TextView) findViewById(R.id.text_main_versions)).setText(sdkVersion);
    }

    /**
     * Unregisters all push tokens for the current user so that they do not receive any notifications,
     * then disconnects from SendBird.
     */
    private void disconnect() {
        SendBird.unregisterPushTokenAllForCurrentUser(new SendBird.UnregisterPushTokenHandler() {
            @Override
            public void onUnregistered(SendBirdException e) {
                if (e != null) {
                    // Error!
                    e.printStackTrace();
                    return;
                }

                Toast.makeText(MainActivity.this, "All push tokens unregistered.", Toast.LENGTH_SHORT)
                        .show();

                SendBird.disconnect(new SendBird.DisconnectHandler() {
                    @Override
                    public void onDisconnected() {
                        PreferenceUtils.setConnected(MainActivity.this, false);
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mFragment= getApiFragment();
        outState.putString(STATE_SHOWING_RESULTS,mFragment.toString());
    }

    private void handleShareIntent() {
        final Intent intent = getIntent();
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_SEND)
                && TextUtils.equals(intent.getType(), "text/plain")) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        }
    }
    private void prepareApi() {

        getSupportLoaderManager().initLoader(LOADER_ACCESS_TOKEN, null,
                new LoaderManager.LoaderCallbacks<String>() {
                    @Override
                    public Loader<String> onCreateLoader(int id, Bundle args) {
                        return new AccessTokenLoader(MainActivity.this);
                    }

                    @Override
                    public void onLoadFinished(Loader<String> loader, String token) {
                        getApiFragment().setAccessToken(token);
                    }

                    @Override
                    public void onLoaderReset(Loader<String> loader) {
                    }
                });
    }

    @Override
    public void onSentimentReady(SentimentInfo sentiment) {
//        if (mViewPager.getCurrentItem() == API_SENTIMENT) {
////            showResults();
//        }
        mAdapter.setSentiment(sentiment);
    }

    public void startAnalyze(String userInput) {

        // Call the API
        System.out.println("mInput here:"+userInput);
        final String text ="Some Text";
//        final String text = mInput.getText().toString();
        System.out.println("getApiFragment here:"+getApiFragment());

        getApiFragment().analyzeSentiment(userInput);
    }

    private ApiFragment getApiFragment() {
        return (ApiFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_API);
    }

    @Override
    public void onResume() {
        Log.d("LIFECYCLE", "MainActivity onResume()");

        GroupChatFragment gF = new GroupChatFragment();
        List<HashMap<Long,String>> messages   = gF.messages();

        System.out.println("messages here in main activity resume:"+messages);
        maxPolarityCount=0;
        positiveCount =0;
        negativeCount =0;
        for(HashMap<Long,String> message:messages){
            for(long key : message.keySet()){
                long timeDiff= (System.currentTimeMillis()-key)/(60000 *60);
                lastAnalyzedTime = System.currentTimeMillis();
                if(timeDiff<3){
                    System.out.println("before start analyze");
                    if(message.get(key).length()!=0){
                        startAnalyze(message.get(key));
                        messageListSize++;
                    }

                }
            }
        }


        super.onResume();
    }


    public static class ResultPagerAdapter extends FragmentPagerAdapter {

        private final String[] mApiNames;

        private final Fragment[] mFragments = new Fragment[1];

        public ResultPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mApiNames = context.getResources().getStringArray(R.array.api_names);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            System.out.println("position:"+position);
            Thread.dumpStack();
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mFragments[position] = fragment;
            return fragment;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case API_SENTIMENT:
                    return SentimentFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mApiNames[position];
        }

        public void setSentiment(SentimentInfo sentiment) {
            messageListAnalyzedCount ++;
            float magnitude=0f;
            if(sentiment.polarity < 0 && sentiment.magnitude < 0.5){
                magnitude = -0.5f;
                negativeCount ++;
            }else  if(sentiment.polarity <0 && sentiment.magnitude >= 0.5) {
                magnitude= -1f;
                negativeCount ++;
            }else  if(sentiment.polarity >0 && sentiment.magnitude < 0.5) {
                positiveCount ++;
                magnitude = 0.5f;
            }else  if(sentiment.polarity >0 && sentiment.magnitude >= 0.5) {
                positiveCount++;
                magnitude = 1f;
            }
            if(maxPolarityCount == 0){
                sentimentPolarity = sentiment.polarity;
                sentimentMagnitude = magnitude;
                maxPolarityCount ++;
            }else if(sentimentPolarity == sentiment.polarity && sentimentMagnitude == magnitude){
                maxPolarityCount ++;
            }else{
                maxPolarityCount --;
            }
            if(messageListAnalyzedCount == messageListSize){
                System.out.println("sentiment polarity final:"+sentimentPolarity);

                String targetURL ="http://ec2-13-58-84-119.us-east-2.compute.amazonaws.com:8080/5/ratings/top/10/";
                int finalSentiment= 0;
                System.out.println("positiveCount:"+positiveCount);
                System.out.println("negativeCount:"+negativeCount);
                if(sentimentPolarity > 0  && sentimentMagnitude ==1f )finalSentiment = 10;
                else if(sentimentPolarity > 0&& sentimentMagnitude == 0.5f)finalSentiment = 7;
                else if(sentimentPolarity == 0)finalSentiment = 5;
                else if(sentimentPolarity < 0  && sentimentMagnitude == -0.5f)finalSentiment = 3;
                else if(sentimentPolarity < 0  &&  sentimentMagnitude == -1f)finalSentiment = 1;

                if((positiveCount > negativeCount) && finalSentiment < 5){
                    finalSentiment = 10;
                    sentimentPolarity =1;
                }else if ((positiveCount < negativeCount) && finalSentiment > 5){
                    finalSentiment = 1;
                    sentimentPolarity =-1;
                }
                System.out.println("sentient Polarity:"+sentimentPolarity);
                System.out.println("finalSentiment:"+finalSentiment);
                System.out.println("sentimentMagnitude:"+sentimentMagnitude);
                new GetMediaLists().execute(targetURL,String.valueOf(finalSentiment));
            }

            System.out.println("sentiment magnitude here:"+sentiment.magnitude);
            System.out.println("sentiment polarity here:"+sentiment.polarity);
            System.out.println("0  here:"+maxPolarityCount);
            System.out.println("sentimentPolarityset here here:"+sentimentPolarity);

            final SentimentFragment fragment = (SentimentFragment) mFragments[API_SENTIMENT];
            if (fragment != null) {
                fragment.setSentiment(sentiment);
            }
        }

    }

    public static class GetMediaLists extends AsyncTask<String, String, String> {

        public GetMediaLists() {
            //set context variables if required
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected String doInBackground(String... params) {

            String urlString = params[0]; // URL to call

            int sentiment = Integer.valueOf(params[1]);
            urlString +=sentiment;
            System.out.println("urlString:"+urlString);

            String resultToDisplay = "";
            HttpURLConnection urlConnection=null;
            String jsonString = new String();
            StringBuilder sb = new StringBuilder();
            try {

                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setChunkedStreamingMode(0);
                urlConnection.setDoOutput(false);
                urlConnection.connect();
                System.out.println("getResponseCode:"+urlConnection.getResponseCode());
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    System.out.println("in this line");
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    jsonString = sb.toString();
                    JSONArray responseArray = new JSONArray(jsonString);
                    movieNames = new String[0];
                    movieNames = new String[responseArray.length()];
                    for(int i=0;i<responseArray.length();i++){
                        String newJsonString = responseArray.get(i).toString();
                        JSONArray subresponse = new JSONArray(newJsonString);
                        movieNames[i]= String.valueOf(subresponse.get(0));
                        System.out.println("movie:"+subresponse.get(0));
                    }
                }

            } catch (Exception e) {

                System.out.println(e.getMessage());

                return e.getMessage();

            }finally{
                urlConnection.disconnect();
            }

//            try {
////                resultToDisplay = IOUtils.toString(in, "UTF-8");
//                //to [convert][1] byte stream to a string
////                resultToDisplay = ;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            return resultToDisplay;
        }

        @Override
        protected void onPostExecute(String result) {

        }
    }
}