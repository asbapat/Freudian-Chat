package com.sendbird.android.sample.main;

/**
 * Created by user on 09-04-2017.
 */


import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sendbird.android.sample.R;
import com.sendbird.android.sample.model.SentimentInfo;


public class SentimentFragment extends Fragment {

    private static final String ARG_SENTIMENT = "sentiment";

    public static SentimentFragment newInstance() {
        final SentimentFragment fragment = new SentimentFragment();
        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private int mColorPositive;
    private int mColorNeutral;
    private int mColorNegative;

    private TextView mPolarity;
    private TextView mMagnitude;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources resources = getResources();
        final Resources.Theme theme = getActivity().getTheme();
//        mColorPositive = ResourcesCompat.getColor(resources, R.color.polarity_positive, theme);
//        mColorNeutral = ResourcesCompat.getColor(resources, R.color.polarity_neutral, theme);
//        mColorNegative = ResourcesCompat.getColor(resources, R.color.polarity_negative, theme);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sentiment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mPolarity = (TextView) view.findViewById(R.id.polarity);
        mMagnitude = (TextView) view.findViewById(R.id.magnitude);
        final Bundle args = getArguments();
        if (args.containsKey(ARG_SENTIMENT)) {
            showSentiment((SentimentInfo) args.getParcelable(ARG_SENTIMENT));
        }
    }

    public void setSentiment(SentimentInfo sentiment) {
        showSentiment(sentiment);
        getArguments().putParcelable(ARG_SENTIMENT, sentiment);
    }

    private void showSentiment(SentimentInfo sentiment) {
        mPolarity.setText(String.valueOf(sentiment.polarity));
        System.out.println("sentiment.polarity:"+sentiment.polarity);
        mMagnitude.setText(String.valueOf(sentiment.magnitude));
    }

}
