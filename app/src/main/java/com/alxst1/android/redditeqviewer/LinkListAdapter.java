package com.alxst1.android.redditeqviewer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.GestureDetector;

import com.squareup.picasso.Picasso;

/**
 * Created by Alexander on 5/15/2016.
 */
public class LinkListAdapter extends CursorAdapter {
    MainActivity mActivity;
    private final GestureDetectorCompat mGestureDetector;
    private int mViewWidth;
    private ListView mListView;
    private float mViewWidthRatioIgnoreFromLeft;

    public LinkListAdapter(MainActivity activity, ListView lv, Cursor c, int flags) {
        super(activity, c, flags);
        mActivity = activity;
        mListView = lv;
        mGestureDetector = new GestureDetectorCompat(mActivity, new SubredditListGestureListener());
        mViewWidthRatioIgnoreFromLeft = Util.getFloatFromResources(mActivity.getResources(),
                R.dimen.view_width_ratio_left_ignore_horiz_swipe);
    }

    public GestureDetectorCompat getGestureDetector() {
        return mGestureDetector;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        return super.getView(pos, convertView, parent);
    }

    @Override
    public void bindView(final View view, Context context, Cursor c) {
        mViewWidth = view.getWidth();
        final ViewHolder vh = (ViewHolder)view.getTag();
        String thumbUrl = c.getString(MainActivity.COL_THUMBNAIL);
        ViewGroup.LayoutParams pars = vh.thumb.getLayoutParams();
        if (thumbUrl != null && !thumbUrl.isEmpty() && thumbUrl.startsWith(Constants.HTTP_PREFIX)) {
            pars.height = vh.imgHeight;
            pars.width = vh.imgWidth;
            Picasso.with(mActivity).load(thumbUrl)
                    //.resize(vh.thumb.getLayoutParams().width, vh.thumb.getLayoutParams().height)
                    //.centerCrop()
                    .into(vh.thumb);
        } else {
            vh.thumb.setImageDrawable(ContextCompat.getDrawable(mActivity,
                    R.drawable.photo_placeholder));
            pars.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            pars.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        vh.thumb.setLayoutParams(pars);
        vh.subreddit.setText(Util.getSubredditNameWithR(mActivity,
                c.getString(MainActivity.COL_SUBREDDIT)));
        vh.when.setText(Util.getRelativeLocalTimeFromUTCtime((long)c.getInt(MainActivity
                .COL_CREATED_UTC)));
        vh.domain.setText(c.getString(MainActivity.COL_DOMAIN));
        vh.title.setText(c.getString(MainActivity.COL_TITLE));
        vh.score.setText(Util.bold(c.getString(MainActivity.COL_SCORE)));
        vh.numComments.setText(String.format(mActivity.getString(R.string.num_commments),
                c.getInt(MainActivity.COL_NUM_COMMENTS)));
        vh.author.setText(Util.bold(c.getString(MainActivity.COL_AUTHOR)));
        int pos = c.getInt(MainActivity.COL_POSITION);
        int count = c.getInt(MainActivity.COL_COUNT);
        vh.position.setText(Util.getLinkPositionString(mActivity, pos, count));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        float ratio = Util.getFloatFromResources(mActivity.getResources(),
                R.dimen.image_height_to_view_height_ratio);
        // Image will have a square shape
        // 'image_height_to_view_height_ratio' is used for image to have some padding from top and
        // bottom of view
        int listItemHeight = (int)(Util.getListPreferredItemHeight(mActivity) *
                Util.getFloatFromResources(mActivity.getResources(),
                R.dimen.links_list_item_height_multiplier));
        int imgHeight = (int)(listItemHeight * ratio);
        int imgWidth = imgHeight;
        View view = LayoutInflater.from(context).inflate(R.layout.link_list_item, parent, false);
        ViewHolder holder = new ViewHolder(view, listItemHeight, imgWidth, imgHeight);
        view.setTag(holder);
        return view;
    }

    static class ViewHolder {
        ImageView thumb;
        FrameLayout thumbHolder;
        TextView subreddit;
        TextView when;
        TextView domain;
        TextView title;
        TextView score;
        TextView numComments;
        TextView author;
        TextView position;
        RelativeLayout topRow;
        RelativeLayout bottomRow;
        int imgWidth;
        int imgHeight;
        public ViewHolder(View view, int layoutHeight, int imgWidth, int imgHeight) {
            thumb = (ImageView) view.findViewById(R.id.link_thumb);
            thumbHolder = (FrameLayout) view.findViewById(R.id.link_thum_holder);
            view.getLayoutParams().height = layoutHeight;
            ViewGroup.LayoutParams pars = thumbHolder.getLayoutParams();
            pars.height = imgHeight;
            pars.width = imgWidth;
            this.imgWidth = imgWidth;
            this.imgHeight = imgHeight;
            subreddit = (TextView) view.findViewById(R.id.link_subreddit);
            when = (TextView) view.findViewById(R.id.link_when);
            domain = (TextView) view.findViewById(R.id.link_domain);
            title = (TextView) view.findViewById(R.id.link_title);
            score = (TextView) view.findViewById(R.id.link_score);
            numComments = (TextView) view.findViewById(R.id.link_num_comments);
            author = (TextView) view.findViewById(R.id.link_author);
            position = (TextView) view.findViewById(R.id.link_position);
            topRow = (RelativeLayout) view.findViewById(R.id.link_top_row);
            bottomRow = (RelativeLayout) view.findViewById(R.id.link_bottom_row);
        }
    }

    class SubredditListGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(DEBUG_TAG,"onDown: " + event.toString());
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            int curPos = mListView.pointToPosition((int)e.getX(), (int)e.getY());
            Cursor c = (Cursor)getItem(curPos);
            if (c != null) {
                Intent intent = new Intent(mActivity, LinkWithCommentsActivity.class);
                intent.putExtra(Constants.EXTRA_SUBREDDIT_NAME,
                        c.getString(MainActivity.COL_SUBREDDIT));
                intent.putExtra(Constants.EXTRA_LINK_COUNT, c.getInt(MainActivity.COL_COUNT));
                intent.putExtra(Constants.EXTRA_LINK_POSITION, c.getInt(MainActivity.COL_POSITION));
                intent.setAction(Constants.ACTION_SHOW_LINKS_FOR_SUBREDDIT);
                mActivity.startActivity(intent);
            }
            return true;
        }

        int lastScrollEntrancePosition = ListView.INVALID_POSITION;
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null)
                return false;
            Log.d(DEBUG_TAG, "onScroll: " + e1.toString() + e2.toString());
            if (Math.abs(distanceX) >  Math.abs(distanceY) &&
                    e1.getX() > (mViewWidth * mViewWidthRatioIgnoreFromLeft)) {
                int curPos = mListView.pointToPosition((int)e1.getX(), (int)e1.getY());
                Cursor c = (Cursor)getItem(curPos);
                if (c != null) {
                    String subreddit = c.getString(MainActivity.COL_SUBREDDIT);
                    int subrdtPos = c.getInt(MainActivity.COL_POSITION);
                    if (subrdtPos != lastScrollEntrancePosition) {
                        lastScrollEntrancePosition = subrdtPos;
                        int subrdtCnt = c.getInt(MainActivity.COL_COUNT);
                        if (distanceX > 0) { //scroll to left
                            subrdtPos++;
                        } else { //scroll to right
                            subrdtPos--;
                        }
                        if (subrdtPos >= subrdtCnt) subrdtPos = subrdtCnt - 1;
                        if (subrdtPos < 0) subrdtPos = 0;
                        if (subrdtPos >= 0 && subrdtPos < subrdtCnt) {
                            Util.updateSubredditPositionInDb(mActivity, subreddit, subrdtPos);
                        }
                    }
                }
                return true;
            }
        return false;
        }
    }
}
