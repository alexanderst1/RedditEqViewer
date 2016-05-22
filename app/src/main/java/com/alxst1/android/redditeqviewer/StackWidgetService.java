package com.alxst1.android.redditeqviewer;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.alxst1.android.redditeqviewer.RedditContract.LinkEntry;

/**
 * Created by AlexSt on 10/13/2015.
 */
public class StackWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    public static final int COL_ROWID = 0;
    public static final int COL_POSITION = 1;
    public static final int COL_NUM_LINKS = 2;
    public static final int COL_TITLE = 3;
    public static final int COL_SCORE = 4;
    public static final int COL_NUM_CMNTS = 5;
    public static final int COL_SUBREDDIT = 6;

    private Context mContext;
    private int mAppWidgetId;
    Cursor mCursor;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
    }

    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        if (mCursor != null)
            mCursor.close();
    }

    public int getCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    public RemoteViews getViewAt(int position) {
        // position will always range from 0 to getCount() - 1.

        RemoteViews rv = new RemoteViews(mContext.getPackageName(),
                R.layout.widget_list_item);

        if (mCursor != null && mCursor.getCount() > 0 && mCursor.moveToPosition(position)){
            mCursor.moveToPosition(position);
            String pageNum = Integer.toString(position + 1);
            rv.setTextViewText(R.id.wid_list_order_num, pageNum);
            rv.setContentDescription(R.id.wid_list_order_num, "Card number " + pageNum);
            rv.setTextViewText(R.id.wid_subreddit_name, mCursor.getString(COL_SUBREDDIT));
            rv.setContentDescription(R.id.wid_subreddit_name, mCursor.getString(COL_SUBREDDIT));
            rv.setTextViewText(R.id.wid_title, mCursor.getString(COL_TITLE));
            rv.setContentDescription(R.id.wid_title, mCursor.getString(COL_TITLE));
            rv.setTextViewText(R.id.wid_num_cmnts, "" + mCursor.getInt(COL_NUM_CMNTS));
            rv.setContentDescription(R.id.wid_num_cmnts, "" + mCursor.getInt(COL_NUM_CMNTS));
            rv.setTextViewText(R.id.wid_score, "" + mCursor.getInt(COL_SCORE));
            rv.setContentDescription(R.id.wid_score, mCursor.getString(COL_SCORE));
            rv.setTextViewText(R.id.wid_num_links, "" + mCursor.getInt(COL_NUM_LINKS));
            rv.setContentDescription(R.id.wid_num_links, "" + mCursor.getInt(COL_NUM_LINKS));

            // Next, we set a fill-intent which will be used to fill-in the pending
            // intent template which is set on the collection view in StackWidgetProvider.
            Intent intent = new Intent();
            intent.putExtra(Constants.EXTRA_SUBREDDIT_NAME, mCursor.getString(COL_SUBREDDIT));
            intent.putExtra(Constants.EXTRA_LINK_COUNT, mCursor.getInt(COL_NUM_LINKS));
            intent.putExtra(Constants.EXTRA_LINK_POSITION, mCursor.getInt(COL_POSITION));
            rv.setOnClickFillInIntent(R.id.widget_list_item, intent);
        }
        // You can do heaving lifting in here, synchronously. For example, if you need to process
        // an image, fetch something from the network, etc., it is ok to do it here, synchronously.
        // A loading view will show up in lieu of the actual contents in the interim.

        // Return the remote views object.
        return rv;
    }

    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.)
        // If you return null here, you will get the default loading view.
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // This is triggered when you call AppWidgetManager
        // notifyAppWidgetViewDataChanged on the collection view corresponding to this factory.
        // You can do heaving lifting in here, synchronously. For example, if you need to process
        // an image, fetch something from the network, etc., it is ok to do it here, synchronously.
        // The widget will remain in its current state while work is being done here, so you don't
        // need to worry about locking up the widget.
        if (mCursor != null)
            mCursor.close();
        Uri uri = LinkEntry.buildUriWithSubpath(RedditContract.SUB_PATH_LINKS_TOP_N_WIDGET);
        mCursor = mContext.getContentResolver().query(uri, null, null, null, null);
    }
}