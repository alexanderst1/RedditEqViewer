package com.alxst1.android.redditeqviewer;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Alexander on 5/6/2016.
 */
public class SubredditListAdapter extends CursorAdapter {
    MainActivity mActivity;
    private Toast mToast;
    Handler mHandler = new Handler();
    Runnable mShowHintToast;
    String mUnsubscrHint;
    long mLastTapTime = 0;
    int mLastTapViewHash = 0;

    public SubredditListAdapter(MainActivity activity, Cursor c, int flags) {
        super(activity, c, flags);
        this.mActivity = activity;
        mUnsubscrHint = mActivity.getString(R.string.unsubscr_hint);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        return super.getView(pos, convertView, parent);
    }

    private void highlightTextInTextView(TextView v, String text, String highlightString) {
        int length = highlightString != null ? highlightString.length() : 0;
        if (length > 0) {
            Spannable spanText = Spannable.Factory.getInstance().newSpannable(text);
            Resources r = mActivity.getResources();
            int color = ContextCompat.getColor(mActivity, R.color.colorSecondary);
            spanText.setSpan(new BackgroundColorSpan(color), 0, length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            v.setText(spanText);
        } else {
            v.setText(text);
        }
    }

    @Override
    public void bindView(final View view, Context context, Cursor cursor) {
        final boolean isSubscribed = cursor.getInt(MainActivity.COL_IS_SUBSCRIBER) == 1;
        final String subrdtName = cursor.getString(MainActivity.COL_SUBREDDIT_NAME);
        String highlightString = mActivity.getSearchString();
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.icon.setImageDrawable(ContextCompat.getDrawable(mActivity,
                isSubscribed ? R.drawable.ic_check_black_24dp : R.drawable.ic_add_black_24dp));
        highlightTextInTextView(holder.text, subrdtName, highlightString);
        holder.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                handleSubsriptionClick(v, !isSubscribed, subrdtName);
            }
        });
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.subreddit_list_item, parent, false);

        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);

        return view;
    }

    /**
     * Function name is not accurately reflects what it is doing (for brevity).
     * Returns 'true' if user tapped 2 times within 1 sec on the same view (with check-mark icon).
     * Otherwise shows toast with a hint to double-tap and returns false.
     *
     * @param v - view tapped on
     * @return - see description
     */
    private boolean isDoubleTap(final View v) {
        boolean result = false;
        //Check for time between taps -
        //allow 1000 msec between taps to consider them as a double-tap
        long currTime = System.currentTimeMillis();
        boolean doubleTap = currTime - mLastTapTime < 1000;
        mLastTapTime = currTime;
        //Check of both taps were on the same view
        int viewHash = v.hashCode();
        boolean sameView = mLastTapViewHash == viewHash;
        mLastTapViewHash = viewHash;
        //Cancel showing toast with a hint to double tap
        mHandler.removeCallbacks(mShowHintToast);
        if (doubleTap && sameView) {
            result = true;
        } else {
            //Show toast with hint to double tap in 500 msec
            mShowHintToast = new Runnable() {
                @Override
                public void run() {
                    makeToast(v, mUnsubscrHint);
                }
            };
            mHandler.postDelayed(mShowHintToast, 500);
        }
        return result;
    }

    private void handleSubsriptionClick(final View v, boolean subscribe /*false - unsubscribe*/,
                                        final String subrdtName) {
        //Initialize message for success - common for logged on and anonymous users
        final String successMsg = mActivity.getString(subscribe ?
                R.string.subscr_success_message : R.string.unsubsc_success_message);
        final String failureMsg;
        if (Util.isLoggedIn(mActivity)) { //Logged in user
            failureMsg = mActivity.getString(subscribe ?
                    R.string.subscr_failure_message : R.string.unsubsc_failure_message);
            //Prepare toasts for success and failure
            RedditRestClient.ResultHandler resHandler = new RedditRestClient.ResultHandler() {
                @Override
                public void onSuccess() {
                    makeToast(null, String.format(successMsg, subrdtName));
                }
                @Override
                public void onFailure(int errorCode) {
                    makeToast(v, String.format(failureMsg, errorCode));
                }
            };
            //subscribe by single tap and unsubscribe by double tap
            if (subscribe || (!subscribe && isDoubleTap(v))) {
                new RedditRestClient(mActivity).subscribeSubreddit(subscribe, subrdtName, resHandler);
            }
        } else { //Anonymous user
            if (subscribe || (!subscribe && isDoubleTap(v))) {
                if (Util.subcrAnonymSubredditInDb(mActivity, subscribe, subrdtName)) {
                    makeToast(null, String.format(successMsg, subrdtName));
                } else {
                    failureMsg = mActivity.getString(subscribe ? R.string.subscr_failure_message_anonym :
                            R.string.unsubsc_failure_message_anonym);
                    makeToast(v, failureMsg);
                }
            }
        }
    }

    /**
     * Shows toast below the view if the view is not null, otherwise shows in default location
     * @param view
     * @param text
     */
    public void makeToast(View view, String text){
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(mActivity, text, Toast.LENGTH_SHORT);
        if (view != null) {
            int xOffset = 20;
            int yOffset = 10;
            int loc[] = new int[2];
            view.getLocationOnScreen(loc);
            int x = loc[0] + xOffset;
            int y = loc[1] + yOffset;
            mToast.setGravity(Gravity.TOP | Gravity.LEFT, x, y);
        }
        mToast.show();
    }

    static class ViewHolder {
        ImageView icon;
        TextView text;
        public ViewHolder(View view) {
            icon = (ImageView) view.findViewById(R.id.toggle);
            text = (TextView) view.findViewById(R.id.subredditName);
        }
    }
}
