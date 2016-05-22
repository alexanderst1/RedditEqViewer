package com.alxst1.android.redditeqviewer;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.alxst1.android.redditeqviewer.RedditContract.AnonymSubscrEntry;
import com.alxst1.android.redditeqviewer.RedditContract.CurrLinkEntry;
import com.alxst1.android.redditeqviewer.RedditContract.LinkEntry;
import com.alxst1.android.redditeqviewer.RedditContract.SubredditEntry;
import com.alxst1.android.redditeqviewer.RedditContract.SubredditSearchEntry;
import com.alxst1.android.redditeqviewer.RedditContract.UserEntry;

/**
 * Created by Alexander on 5/3/2016.
 */
public class RedditProvider extends ContentProvider {

    public static final String LOG_TAG = RedditProvider.class.getSimpleName();

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private RedditDbHelper mHelper;

    static final int USER = 100;
    static final int USER_WITH_ID = 101;
    static final int SUBREDDIT = 200;
    static final int SUBREDDIT_WITH_ID = 201;
    static final int ANONYM_SUBSCRIPTION = 300;
    static final int ANONYM_SUBSCRIPTION_WITH_ID = 301;
    static final int SUBREDDIT_SEARCH = 400;
    static final int SUBREDDIT_SEARCH_WITH_ID = 401;
    static final int LINK = 500;
    static final int LINK_WITH_ID = 501;
    static final int CURR_LINK = 600;
    static final int CURR_LINK_WITH_ID = 601;

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = RedditContract.CONTENT_AUTHORITY;
        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, RedditContract.PATH_USERS, USER); //in for 'query' and for 'insert'
        matcher.addURI(authority, RedditContract.PATH_USERS + "/*", USER); //in for 'query'
        matcher.addURI(authority, RedditContract.PATH_USERS + "/#", USER_WITH_ID); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_SUBREDDITS, SUBREDDIT); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_SUBREDDITS + "/*", SUBREDDIT); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_SUBREDDITS + "/#", SUBREDDIT_WITH_ID); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_ANONYM_SUBSCR, ANONYM_SUBSCRIPTION); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_ANONYM_SUBSCR + "/*", ANONYM_SUBSCRIPTION); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_ANONYM_SUBSCR + "/#", ANONYM_SUBSCRIPTION_WITH_ID); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_SUBREDDIT_SEARCH, SUBREDDIT_SEARCH); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_SUBREDDIT_SEARCH + "/*", SUBREDDIT_SEARCH); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_SUBREDDIT_SEARCH + "/#", SUBREDDIT_SEARCH_WITH_ID); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_LINKS, LINK); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_LINKS + "/*", LINK); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_LINKS + "/#", LINK_WITH_ID); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_CURRENT_LINKS, CURR_LINK); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_CURRENT_LINKS + "/*", CURR_LINK); //out from 'insert'
        matcher.addURI(authority, RedditContract.PATH_CURRENT_LINKS + "/#", CURR_LINK_WITH_ID); //out from 'insert'
        return matcher;
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case USER:
                return UserEntry.CONTENT_TYPE;
            case USER_WITH_ID:
                return UserEntry.CONTENT_ITEM_TYPE;
            case SUBREDDIT:
                return SubredditEntry.CONTENT_TYPE;
            case SUBREDDIT_WITH_ID:
                return SubredditEntry.CONTENT_ITEM_TYPE;
            case ANONYM_SUBSCRIPTION:
                return AnonymSubscrEntry.CONTENT_TYPE;
            case ANONYM_SUBSCRIPTION_WITH_ID:
                return AnonymSubscrEntry.CONTENT_ITEM_TYPE;
            case SUBREDDIT_SEARCH:
                return SubredditSearchEntry.CONTENT_TYPE;
            case SUBREDDIT_SEARCH_WITH_ID:
                return SubredditSearchEntry.CONTENT_ITEM_TYPE;
            case LINK:
                return SubredditSearchEntry.CONTENT_TYPE;
            case LINK_WITH_ID:
                return SubredditSearchEntry.CONTENT_ITEM_TYPE;
            case CURR_LINK:
                return SubredditSearchEntry.CONTENT_TYPE;
            case CURR_LINK_WITH_ID:
                return SubredditSearchEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case USER: {
                retCursor = db.query( UserEntry.TABLE_NAME, projection,
                        selection, selectionArgs, /*groupBy*/ null, /*having*/ null, sortOrder);
                break;
            }
            case SUBREDDIT: {
                if (uri.getLastPathSegment().equals(RedditContract.SUBPATH_ANONYMOUS)) {
                    /*
                    * *** Anonymous user case only ***
                    * combine subreddit names from tables 'subreddits and 'subscription' and
                    * set user_is_subscribed=1 only for those names available in table
                    * 'subscription'. Table 'subscription' may have names not available in
                    * 'subreddits', that's why combining (union) tables is necessary.
                    * ----------------------------------------------------------------------
                    * select 0 as _id, name, (select count(*) from subscr where
                    * subscr.name = all_names.name) as user_is_subscriber from
                    * (select name from subreddits UNION select name from subscr) as
                    * all_names order by user_is_subscriber desc
                    * */
                    String query = "select 0 as _id," + SubredditEntry.COLUMN_NAME +
                            ", (select count(*) from " + AnonymSubscrEntry.TABLE_NAME + " where " +
                            AnonymSubscrEntry.TABLE_NAME + "." + AnonymSubscrEntry.COLUMN_NAME +
                            "=all_names." + SubredditEntry.COLUMN_NAME + ") as " +
                            SubredditEntry.COLUMN_USER_IS_SUBSCRIBER + " from (select " +
                            SubredditEntry.COLUMN_NAME + " from " + SubredditEntry.TABLE_NAME +
                            " union select " + AnonymSubscrEntry.COLUMN_NAME + " from " +
                            AnonymSubscrEntry.TABLE_NAME + ") as all_names order by " +
                            SubredditEntry.COLUMN_USER_IS_SUBSCRIBER + " desc";
                    retCursor = db.rawQuery(query, null);
                } else {
                    retCursor = db.query(SubredditEntry.TABLE_NAME, projection,
                            selection, selectionArgs, /*groupBy*/ null, /*having*/ null, sortOrder);
                }
                break;
            }
            case ANONYM_SUBSCRIPTION: {
                retCursor = db.query( AnonymSubscrEntry.TABLE_NAME, projection,
                        selection, selectionArgs, /*groupBy*/ null, /*having*/ null, sortOrder);
                break;
            }
            case SUBREDDIT_SEARCH: {
                retCursor = db.query(SubredditSearchEntry.TABLE_NAME, projection,
                        selection, selectionArgs, /*groupBy*/ null, /*having*/ null, sortOrder);
                break;
            }
            case LINK: {
                String lastPathSegment = uri.getLastPathSegment();
                if (lastPathSegment.equals(RedditContract.SUB_PATH_LINKS_TOP_N)
                        || lastPathSegment.equals(RedditContract.SUB_PATH_LINKS_TOP_N_WIDGET)) {
                    /**
                     select _id, (select count(*) from links as links_for_position where
                     links_for_position.subreddit = links_main.subreddit and
                     (links_for_position.score > links_main.score or (links_for_position.score =
                     links_main.score and links_for_position._id < links_main._id))) as
                     actual_position, (select count(*) from links as links_for_count where
                     links_for_count.subreddit = links_main.subreddit) as num_links, id, title,
                     domain, author, score, num_comment, subreddit, created_utc, thumbnail,
                     ifnull((select position from curr_links where curr_links.subreddit =
                     links_main.subreddit), 0) as saved_position, (select max(score) from links as
                     links_for_score where links_for_score.subreddit = links_main.subreddit) as
                     max_score  from links as links_main where (actual_position = saved_position or
                     actual_position = num_links-1 and actual_position < saved_position) order by
                     max_score desc, links_main._id asc
                     */
                    String query = "select " +
                            LinkEntry._ID + ", " +
                            //POSITION (actual position, is equal to saved and is used as a variable
                            // to participate several times in comparison expression below)
                            "(select count(*) from " + LinkEntry.TABLE_NAME +
                            " as links_for_position where links_for_position." +
                            LinkEntry.COLUMN_SUBREDDIT + " = links_main." +
                            LinkEntry.COLUMN_SUBREDDIT + " and (links_for_position." +
                            LinkEntry.COLUMN_SCORE + " > links_main." + LinkEntry.COLUMN_SCORE +
                            " or (links_for_position." + LinkEntry.COLUMN_SCORE + " = links_main." +
                            LinkEntry.COLUMN_SCORE + " and links_for_position." + LinkEntry._ID +
                            " < links_main." + LinkEntry._ID +
                            "))) as actual_position, " +
                            //NUMBER OF LINKS OF THE SAME SUBREDDIT
                            "(select count(*) from " +
                            LinkEntry.TABLE_NAME + " as links_for_count where links_for_count." +
                            LinkEntry.COLUMN_SUBREDDIT + " = links_main." +
                            LinkEntry.COLUMN_SUBREDDIT + ") as num_links, ";
                    if (lastPathSegment.equals(RedditContract.SUB_PATH_LINKS_TOP_N_WIDGET)) {
                        //Regular columns
                        query += LinkEntry.COLUMN_TITLE + ", " +
                                LinkEntry.COLUMN_SCORE + ", " +
                                LinkEntry.COLUMN_NUM_COMMENTS + ", " +
                                LinkEntry.COLUMN_SUBREDDIT + ", ";
                    } else if (lastPathSegment.equals(RedditContract.SUB_PATH_LINKS_TOP_N)) {
                        //Regular columns
                        query += LinkEntry.COLUMN_ID + ", " +
                                LinkEntry.COLUMN_TITLE + ", " +
                                LinkEntry.COLUMN_DOMAIN + ", " +
                                LinkEntry.COLUMN_AUTHOR + ", " +
                                LinkEntry.COLUMN_SCORE + ", " +
                                LinkEntry.COLUMN_NUM_COMMENTS + ", " +
                                LinkEntry.COLUMN_SUBREDDIT + ", " +
                                LinkEntry.COLUMN_CREATED_UTC + ", " +
                                LinkEntry.COLUMN_THUMBNAIL + ", ";
                    }
                    //SAVED_POSITION (saved in separate table, remembers user's choice,
                    //used as a variable to participate several times in comparison
                    //expression below))
                    query += "ifnull((select " + CurrLinkEntry.COLUMN_POSITION + " from " +
                            CurrLinkEntry.TABLE_NAME + " where " + CurrLinkEntry.TABLE_NAME + "." +
                            CurrLinkEntry.COLUMN_SUBREDDIT + " = links_main." +
                            LinkEntry.COLUMN_SUBREDDIT + "), 0) as saved_position, " +
                            //MAX score for sorting
                            "(select max(" + LinkEntry.COLUMN_SCORE + ") from " +
                            LinkEntry.TABLE_NAME + " as links_for_score where links_for_score." +
                            LinkEntry.COLUMN_SUBREDDIT + " = links_main." +
                            LinkEntry.COLUMN_SUBREDDIT + ") as max_score " +
                            //from expression
                            " from " + LinkEntry.TABLE_NAME + " as links_main where " +
                            "(actual_position = saved_position or actual_position = num_links-1 " +
                            "and actual_position < saved_position) " +
                            "order by max_score desc, links_main." + LinkEntry._ID + " asc";
                    retCursor = db.rawQuery(query, null);
                } else if (!lastPathSegment.isEmpty() &&
                        !lastPathSegment.equals(RedditContract.PATH_LINKS)) {
                    String query = "select " + LinkEntry._ID + ", " +
                            "(select count(*) from links as lp where lp.subreddit = lm.subreddit " +
                            "and (lp.score > lm.score or " +
                            "(lp.score = lm.score and lp._id < lm._id))) as pos, " +
                            LinkEntry.COLUMN_ID + ", " +
                            LinkEntry.COLUMN_TITLE + ", " +
                            LinkEntry.COLUMN_DOMAIN + ", " +
                            LinkEntry.COLUMN_AUTHOR + ", " +
                            LinkEntry.COLUMN_SCORE + ", " +
                            LinkEntry.COLUMN_NUM_COMMENTS + ", " +
                            LinkEntry.COLUMN_POST_HINT + ", " +
                            LinkEntry.COLUMN_STICKIED + ", " +
                            LinkEntry.COLUMN_OVER_18 + ", " +
                            LinkEntry.COLUMN_AUTHOR_FL_TEXT + ", " +
                            LinkEntry.COLUMN_SLFTXT_HTML + ", " +
                            LinkEntry.COLUMN_SLFTXT + ", " +
                            LinkEntry.COLUMN_CREATED_UTC + ", " +
                            LinkEntry.COLUMN_URL + ", " +
                            LinkEntry.COLUMN_IMG_PORT + ", " +
                            LinkEntry.COLUMN_IMG_PORT_WIDTH + ", " +
                            LinkEntry.COLUMN_IMG_PORT_HEIGHT + ", " +
                            LinkEntry.COLUMN_IMG_LAND + ", " +
                            LinkEntry.COLUMN_IMG_LAND_WIDTH + ", " +
                            LinkEntry.COLUMN_IMG_LAND_HEIGH + " " +
                            "from links as lm where lm.subreddit = '" + lastPathSegment +
                            "' order by pos asc";
                    retCursor = db.rawQuery(query, null);
                }
                else {
                    retCursor = db.query(LinkEntry.TABLE_NAME, projection,
                            selection, selectionArgs, /*groupBy*/ null, /*having*/ null, sortOrder);
                }
                break;
            }
            case CURR_LINK: {
                retCursor = db.query(CurrLinkEntry.TABLE_NAME, projection,
                        selection, selectionArgs, /*groupBy*/ null, /*having*/ null, sortOrder);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
        //printCursorToLog(retCursor);
        Context c = getContext();
        if (c != null) retCursor.setNotificationUri(c.getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        Uri returnUri;
        boolean notifySubredditEntry = false;
        boolean notifySubredditSearchEntry = false;
        boolean notifyLinkEntry = false;
        switch (sUriMatcher.match(uri)) {
            case USER: {
                long _id = db.insert(UserEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = UserEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case SUBREDDIT: {
                long _id = db.insert(SubredditEntry.TABLE_NAME, null, values);
                notifySubredditSearchEntry = true;
                if (_id > 0)
                    returnUri = SubredditEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case ANONYM_SUBSCRIPTION: {
                long _id = db.insert(AnonymSubscrEntry.TABLE_NAME, null, values);
                notifySubredditEntry = true;
                notifySubredditSearchEntry = true;
                if (_id > 0)
                    returnUri = AnonymSubscrEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case SUBREDDIT_SEARCH: {
                long _id = db.insert(SubredditSearchEntry.TABLE_NAME, null, values);
                notifySubredditEntry = true;
                if (_id > 0)
                    returnUri = SubredditSearchEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case LINK: {
                long _id = db.insert(LinkEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = LinkEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case CURR_LINK: {
                long _id = db.insert(CurrLinkEntry.TABLE_NAME, null, values);
                notifyLinkEntry = true;
                if (_id > 0)
                    returnUri = CurrLinkEntry.buildUriWithRowId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
        if (returnUri != null) {
            Context c = getContext();
            if (c != null) {
                ContentResolver cr = c.getContentResolver();
                cr.notifyChange(uri, null);
                if (notifySubredditEntry)
                    cr.notifyChange(SubredditEntry.CONTENT_URI, null);
                if (notifySubredditSearchEntry)
                    cr.notifyChange(SubredditSearchEntry.CONTENT_URI, null);
                if (notifyLinkEntry)
                    cr.notifyChange(LinkEntry.CONTENT_URI, null);
            }
        }
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        int rowsDeleted;
        boolean notifySubredditEntry = false;
        boolean notifySubredditSearchEntry = false;
        boolean notifyLinkEntry = false;
        // have DB engine to delete all rows and to return number of rows deleted
        if ( null == selection ) selection = "1";
        switch (sUriMatcher.match(uri)) {
            case USER: {
                rowsDeleted = db.delete(UserEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            }
            case SUBREDDIT: {
                rowsDeleted = db.delete(SubredditEntry.TABLE_NAME,
                        selection, selectionArgs);
                notifySubredditSearchEntry = true;
                break;
            }
            case ANONYM_SUBSCRIPTION: {
                rowsDeleted = db.delete(AnonymSubscrEntry.TABLE_NAME,
                        selection, selectionArgs);
                notifySubredditEntry = true;
                notifySubredditSearchEntry = true;
                break;
            }
            case SUBREDDIT_SEARCH: {
                rowsDeleted = db.delete(SubredditSearchEntry.TABLE_NAME,
                        selection, selectionArgs);
                notifySubredditEntry = true;
                break;
            }
            case LINK: {
                rowsDeleted = db.delete(LinkEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            }
            case CURR_LINK: {
                rowsDeleted = db.delete(CurrLinkEntry.TABLE_NAME,
                        selection, selectionArgs);
                notifyLinkEntry = true;
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
        if (rowsDeleted != 0) {
            Context c = getContext();
            if (c != null) {
                ContentResolver cr = c.getContentResolver();
                cr.notifyChange(uri, null);
                if (notifySubredditEntry)
                    cr.notifyChange(SubredditEntry.CONTENT_URI, null);
                if (notifySubredditSearchEntry)
                    cr.notifyChange(SubredditSearchEntry.CONTENT_URI, null);
                if (notifyLinkEntry)
                    cr.notifyChange(LinkEntry.CONTENT_URI, null);
            }
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        boolean notifySubredditEntry = false;
        boolean notifySubredditSearchEntry = false;
        boolean notifyLinkEntry = false;
        int rowsUpdated;
        switch (sUriMatcher.match(uri)) {
            case USER: {
                rowsUpdated = db.update(UserEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            }
            case SUBREDDIT: {
                rowsUpdated = db.update(SubredditEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                notifySubredditSearchEntry = true;
                break;
            }
            case ANONYM_SUBSCRIPTION: {
                rowsUpdated = db.update(AnonymSubscrEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                notifySubredditEntry = true;
                notifySubredditSearchEntry = true;
                break;
            }
            case SUBREDDIT_SEARCH: {
                rowsUpdated = db.update(SubredditSearchEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                notifySubredditEntry = true;
                break;
            }
            case LINK: {
                rowsUpdated = db.update(LinkEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            }
            case CURR_LINK: {
                rowsUpdated = db.update(CurrLinkEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                notifyLinkEntry = true;
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
        if (rowsUpdated != 0) {
            Context c = getContext();
            if (c != null) {
                ContentResolver cr = c.getContentResolver();
                cr.notifyChange(uri, null);
                if (notifySubredditEntry)
                    cr.notifyChange(SubredditEntry.CONTENT_URI, null);
                if (notifySubredditSearchEntry)
                    cr.notifyChange(SubredditSearchEntry.CONTENT_URI, null);
                if (notifyLinkEntry)
                    cr.notifyChange(LinkEntry.CONTENT_URI, null);
            }
        }
        return rowsUpdated;
    }

    @Override
    public boolean onCreate() {
        mHelper = new RedditDbHelper(getContext());
        return true;
    }

    @Override
    public void shutdown() {
        mHelper.close();
        super.shutdown();
    }
}
