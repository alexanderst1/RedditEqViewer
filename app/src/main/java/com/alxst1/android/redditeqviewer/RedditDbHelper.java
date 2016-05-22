package com.alxst1.android.redditeqviewer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alxst1.android.redditeqviewer.RedditContract.AnonymSubscrEntry;
import com.alxst1.android.redditeqviewer.RedditContract.CurrLinkEntry;
import com.alxst1.android.redditeqviewer.RedditContract.LinkEntry;
import com.alxst1.android.redditeqviewer.RedditContract.SubredditEntry;
import com.alxst1.android.redditeqviewer.RedditContract.SubredditSearchEntry;
import com.alxst1.android.redditeqviewer.RedditContract.UserEntry;
/**
 * Created by Alexander on 5/3/2016.
 */
public class RedditDbHelper  extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    static final String DATABASE_NAME = "reddit.db";
    public RedditDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_USERS_TABLE = "CREATE TABLE " + UserEntry.TABLE_NAME + " (" +
                UserEntry._ID + " INTEGER PRIMARY KEY, " +
                UserEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                UserEntry.COLUMN_ID + " TEXT, " +
                UserEntry.COLUMN_ACCESS_TOKEN + " TEXT, " +
                UserEntry.COLUMN_REFRESH_TOKEN + " TEXT, " +
                " UNIQUE (" + UserEntry.COLUMN_NAME + ") ON CONFLICT REPLACE);";
        final String SQL_CREATE_SUBREDDITS_TABLE = "CREATE TABLE " + SubredditEntry.TABLE_NAME
                + " (" +
                SubredditEntry._ID + " INTEGER PRIMARY KEY, " +
                SubredditEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                SubredditEntry.COLUMN_ID + " TEXT, " +
                SubredditEntry.COLUMN_OVER18 + " INTEGER, " +
                SubredditEntry.COLUMN_LANG + " TEXT, " +
                SubredditEntry.COLUMN_REDDIT_URL + " TEXT, " +
                SubredditEntry.COLUMN_SUBREDDDIT_TYPE + " TEXT, " +
                SubredditEntry.COLUMN_SUBMISSION_TYPE + " TEXT, " +
                SubredditEntry.COLUMN_USER_IS_SUBSCRIBER + " INTEGER, " +
                " UNIQUE (" + SubredditEntry.COLUMN_ID + ") ON CONFLICT REPLACE);";
        final String SQL_CREATE_ANONYM_SUBSCR_TABLE = "CREATE TABLE " +
                AnonymSubscrEntry.TABLE_NAME + " (" +
                AnonymSubscrEntry._ID + " INTEGER PRIMARY KEY, " +
                AnonymSubscrEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                " UNIQUE (" + AnonymSubscrEntry.COLUMN_NAME +
                ") ON CONFLICT REPLACE);";
        final String SQL_CREATE_SUBREDDIT_SEARCH_TABLE = "CREATE TABLE " +
                SubredditSearchEntry.TABLE_NAME + " (" +
                SubredditSearchEntry._ID + " INTEGER PRIMARY KEY," +
                SubredditSearchEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                " UNIQUE (" + SubredditSearchEntry.COLUMN_NAME +
                ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_LINKS_TABLE = "CREATE TABLE " + LinkEntry.TABLE_NAME + " (" +
                LinkEntry._ID + " INTEGER PRIMARY KEY," +
                LinkEntry.COLUMN_ID + " TEXT, " + //"4jh09c"
                LinkEntry.COLUMN_TITLE + " TEXT, " +
                LinkEntry.COLUMN_DOMAIN + " TEXT, " +
                LinkEntry.COLUMN_AUTHOR + " TEXT, " +
                LinkEntry.COLUMN_SCORE + " INTEGER, " +
                LinkEntry.COLUMN_NUM_COMMENTS + " INTEGER, " +
                LinkEntry.COLUMN_POST_HINT + " TEXT, " +
                LinkEntry.COLUMN_STICKIED + " INTEGER, " +
                LinkEntry.COLUMN_OVER_18 + " INTEGER, " +
                LinkEntry.COLUMN_SUBREDDIT + " TEXT, " + //"Android"
                LinkEntry.COLUMN_SUBREDDIT_ID + " TEXT, " + //"t5_2qlqh"
                LinkEntry.COLUMN_AUTHOR_FL_TEXT + " TEXT, " +
                LinkEntry.COLUMN_SLFTXT_HTML + " TEXT, " +
                LinkEntry.COLUMN_SLFTXT + " TEXT, " +
                LinkEntry.COLUMN_CREATED + " INTEGER, " +
                LinkEntry.COLUMN_CREATED_UTC + " INTEGER, " +
                LinkEntry.COLUMN_PERMALINK + " TEXT, " +
                LinkEntry.COLUMN_URL + " TEXT, " +
                LinkEntry.COLUMN_THUMBNAIL + " TEXT, " +
                LinkEntry.COLUMN_IMG_PORT + " TEXT, " +
                LinkEntry.COLUMN_IMG_PORT_WIDTH + " INTEGER, " +
                LinkEntry.COLUMN_IMG_PORT_HEIGHT + " INTEGER, " +
                LinkEntry.COLUMN_IMG_LAND + " TEXT, " +
                LinkEntry.COLUMN_IMG_LAND_WIDTH + " INTEGER, " +
                LinkEntry.COLUMN_IMG_LAND_HEIGH + " INTEGER, " +
                " UNIQUE (" + LinkEntry.COLUMN_ID + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_CURR_LINKS_TABLE = "CREATE TABLE " + CurrLinkEntry.TABLE_NAME + " (" +
                CurrLinkEntry._ID + " INTEGER PRIMARY KEY," +
                CurrLinkEntry.COLUMN_SUBREDDIT + " TEXT, " +
                CurrLinkEntry.COLUMN_POSITION + " INTEGER, " +
                " UNIQUE (" + LinkEntry.COLUMN_SUBREDDIT + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_USERS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_SUBREDDITS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_ANONYM_SUBSCR_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_SUBREDDIT_SEARCH_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_LINKS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_CURR_LINKS_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SubredditEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + AnonymSubscrEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SubredditSearchEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LinkEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CurrLinkEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
