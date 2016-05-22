package com.alxst1.android.redditeqviewer;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.ApplicationTestCase;
import android.util.Log;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    public static final String LOG_TAG = ApplicationTest.class.getSimpleName();

    public ApplicationTest() {
        super(Application.class);
    }

    public void testDb() throws Throwable {
        RedditDbHelper helper = new RedditDbHelper(mContext);
        SQLiteDatabase db = helper.getReadableDatabase();
        String query;
        //query = "select _id, id, subreddit, score from links as lo where id = (select id from links as li where li.subreddit = lo.subreddit order by score desc limit 1 offset (case when (select position from curr_links as cl where cl.subreddit = subreddit) is null then 0 else (select position from curr_links as cl where cl.subreddit = subreddit) end)) order by score desc";
        query = "select _id, ifnull((select position from curr_links where curr_links.subreddit = links_main.subreddit), 0) as saved_position, (select count(*) from links as links_for_position where links_for_position.subreddit=links_main.subreddit and (links_for_position.score > links_main.score or (links_for_position.score = links_main.score and links_for_position._id < links_main._id))) as actual_position, (select count(*) from links as links_for_count where links_for_count.subreddit=links_main.subreddit) as num_links, id, subreddit, score from links as links_main where (actual_position = saved_position or actual_position = num_links - 1 and actual_position < saved_position ) order by subreddit asc, score desc";
        //query = "select _id, (select position from curr_links where curr_links.subreddit = lo.subreddit) as pos, (select count(*) from links as lc where lc.subreddit=lo.subreddit), id, subreddit, score from links as lo where id = (select id from links as li where li.subreddit = lo.subreddit order by score desc limit 1 offset (case when lo.pos is null then 0 else lo.pos end)) order by score desc";
        //query = "select _id, id, subreddit, score from links as lo where id = (select id from links as li where li.subreddit = lo.subreddit order by score desc limit 1 offset (select (case when position is null then 0 else position end) from curr_links as cl where cl.subreddit = subreddit)) order by score desc";
        //query = "select _id, id, subreddit, score from links order by subreddit asc, score desc";
        query = "select _id, (select count(*) from links as lp where lp.subreddit = lm.subreddit and (lp.score > lm.score or (lp.score = lm.score and lp._id < lm._id))) as pos, id, score, subreddit, title from links as lm where lm.subreddit = 'worldnews' order by pos asc";
        Cursor c = db.rawQuery(query, null);
        printCursorToLog(c);
    }

    public void testDb2() throws Throwable {
        Util.updateSubredditPositionInDb(mContext, "NewsOfTheStupid", 1);
        Util.updateSubredditPositionInDb(mContext, "androidapps", 2);
        Util.updateSubredditPositionInDb(mContext, "doctorwho", 3);
        Util.updateSubredditPositionInDb(mContext, "gadgets", 1);
        Util.updateSubredditPositionInDb(mContext, "newsokunomoral", 2);
        Util.updateSubredditPositionInDb(mContext, "pics", 3);
        Util.updateSubredditPositionInDb(mContext, "politics", 1);
        Util.updateSubredditPositionInDb(mContext, "sports", 5);
        Cursor c = mContext.getContentResolver().query(RedditContract.CurrLinkEntry.CONTENT_URI,
                null, null, null, null);
        printCursorToLog(c);
    }

    void printCursorToLog(Cursor c)
    {
        if (c != null && c.moveToFirst()) {
            do {
                String s = "";
                for (int i = 0; i < c.getColumnCount(); i++) {
                    s += c.getString(i) + "\t\t";
                }
                Log.d(LOG_TAG, s);
            }while (c.moveToNext());
        }
    }
}