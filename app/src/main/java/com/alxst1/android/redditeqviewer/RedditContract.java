package com.alxst1.android.redditeqviewer;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Alexander on 5/3/2016.
 */
public class RedditContract {

    public static final String CONTENT_AUTHORITY = "com.alxst1.android.redditeqviewer";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_USERS = "users";
    public static final String PATH_SUBREDDITS = "subreddits";
    public static final String PATH_ANONYM_SUBSCR = "subscr_subredits";
    public static final String PATH_SUBREDDIT_SEARCH = "search_subreddit_names";
    public static final String SUBPATH_ANONYMOUS = "anonym";
    public static final String PATH_LINKS = "links";
    public static final String SUB_PATH_LINKS_TOP_N = "topn";
    public static final String PATH_CURRENT_LINKS = "curr_links";

    public static final class UserEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_USERS).build();
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_USERS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_USERS;
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_ACCESS_TOKEN = "access_token";
        public static final String COLUMN_REFRESH_TOKEN = "refresh_token";

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
    }

    public static final class SubredditEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SUBREDDITS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_SUBREDDITS;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_SUBREDDITS;
        public static final String TABLE_NAME = "subreddits";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_OVER18 = "over18";
        public static final String COLUMN_LANG = "lang";
        public static final String COLUMN_REDDIT_URL = "url";
        public static final String COLUMN_SUBREDDDIT_TYPE = "subreddit_type";
        public static final String COLUMN_SUBMISSION_TYPE = "submission_type";
        public static final String COLUMN_USER_IS_SUBSCRIBER = "user_is_subscriber";

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
        public static Uri buildUriWithSubpath(String subPath) {
            return CONTENT_URI.buildUpon().appendPath(subPath).build();
        }
    }

    public static final class AnonymSubscrEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ANONYM_SUBSCR).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_ANONYM_SUBSCR;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_ANONYM_SUBSCR;
        public static final String TABLE_NAME = "subscr";
        public static final String COLUMN_NAME = "name";

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
    }

    public static final class SubredditSearchEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SUBREDDIT_SEARCH).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_SUBREDDIT_SEARCH;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_SUBREDDIT_SEARCH;
        public static final String TABLE_NAME = "search";
        public static final String COLUMN_NAME = "name";

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
    }

    public static final class LinkEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LINKS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_LINKS;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_LINKS;
        public static final String TABLE_NAME = "links";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_DOMAIN = "domain";
        public static final String COLUMN_AUTHOR = "author";
        public static final String COLUMN_SCORE = "score";
        public static final String COLUMN_NUM_COMMENTS = "num_comment";
        public static final String COLUMN_POST_HINT = "post_hint";
        public static final String COLUMN_STICKIED = "stickied";
        public static final String COLUMN_OVER_18 = "over_18";
        public static final String COLUMN_SUBREDDIT = "subreddit";
        public static final String COLUMN_SUBREDDIT_ID = "subreddit_id";
        public static final String COLUMN_AUTHOR_FL_TEXT = "author_flair_text";
        public static final String COLUMN_SLFTXT_HTML = "selftext_html";
        public static final String COLUMN_SLFTXT = "selftext";
        public static final String COLUMN_CREATED = "created";
        public static final String COLUMN_CREATED_UTC = "created_utc";
        public static final String COLUMN_PERMALINK = "permalink";
        public static final String COLUMN_URL = "url";
        public static final String COLUMN_THUMBNAIL = "thumbnail";
        public static final String COLUMN_IMG_PORT = "img_p";
        public static final String COLUMN_IMG_PORT_WIDTH = "img_port_width";
        public static final String COLUMN_IMG_PORT_HEIGHT = "img_port_height";
        public static final String COLUMN_IMG_LAND = "img_land";
        public static final String COLUMN_IMG_LAND_WIDTH = "img_land_width";
        public static final String COLUMN_IMG_LAND_HEIGH = "img_land_height";

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
        public static Uri buildUriWithSubpath(String subPath) {
            return CONTENT_URI.buildUpon().appendPath(subPath).build();
        }
    }

    public static final class CurrLinkEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CURRENT_LINKS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_CURRENT_LINKS;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_CURRENT_LINKS;
        public static final String TABLE_NAME = "curr_links";
        public static final String COLUMN_SUBREDDIT = "subreddit";
        public static final String COLUMN_POSITION = "position";
        public static final String COLUMN_COUNT = "count"; //virtual column (in query only)

        public static final Uri buildUriWithRowId (long rowId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(rowId)).build();
        }
    }
}
