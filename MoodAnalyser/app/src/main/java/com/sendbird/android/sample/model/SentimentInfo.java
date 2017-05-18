package com.sendbird.android.sample.model;

/**
 * Created by user on 09-04-2017.
 */

import android.os.Parcel;
import android.os.Parcelable;

import com.google.api.services.language.v1beta1.model.Sentiment;


/**
 * A {@link Parcelable} variant of {@link Sentiment}.
 */
public class SentimentInfo implements Parcelable {

    public static final Creator<SentimentInfo> CREATOR = new Creator<SentimentInfo>() {
        @Override
        public SentimentInfo createFromParcel(Parcel in) {
            return new SentimentInfo(in);
        }

        @Override
        public SentimentInfo[] newArray(int size) {
            return new SentimentInfo[size];
        }
    };

    /**
     * Polarity of the sentiment in the [-1.0, 1.0] range.
     */
    public final float polarity;

    /**
     * The absolute magnitude of sentiment in the [0, +inf) range.
     */
    public final float magnitude;

    public SentimentInfo(Sentiment sentiment) {
        polarity = sentiment.getPolarity();
        magnitude = sentiment.getMagnitude();
    }

    protected SentimentInfo(Parcel in) {
        polarity = in.readFloat();
        magnitude = in.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(polarity);
        out.writeFloat(magnitude);
    }

}