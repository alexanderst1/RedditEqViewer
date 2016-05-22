package com.alxst1.android.redditeqviewer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.DrawableMarginSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.alxst1.android.redditeqviewer.RedditContract.UserEntry;
import com.alxst1.android.redditeqviewer.RedditContract.SubredditEntry;
import com.alxst1.android.redditeqviewer.RedditContract.AnonymSubscrEntry;
import com.alxst1.android.redditeqviewer.RedditContract.SubredditSearchEntry;
import com.alxst1.android.redditeqviewer.RedditContract.LinkEntry;
import com.alxst1.android.redditeqviewer.RedditContract.CurrLinkEntry;

/**
 * Created by Alexander on 4/29/2016.
 */
public class Util {
    public static final String LOG_TAG = Util.class.getSimpleName();
    public static SharedPreferences getSharedPreferences(Context c) {
        return c.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);
    }
    public static String getSharedString(Context c, String key) {
        return getSharedPreferences(c).getString(key, null);
    }
    public static void setSharedString(Context c, String key, String val) {
        getSharedPreferences(c).edit().putString(key, val).commit();
    }
    public static void removeSharedString(Context c, String key) {
        getSharedPreferences(c).edit().remove(key).commit();
    }
    public static String getUserName(Context c) {
        return getSharedString(c, RedditRestClient.API_USER_NAME);
    }

    public static boolean FilterNsfw(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c)
                .getBoolean(c.getString(R.string.pref_nsfw_key), false);
    }

    public static boolean isLoggedIn(Context c) {
        return getUserName(c) != null;
    }
    public static void setUser(Context c, String userName, String accessTkn, String refreshTkn) {
        setSharedString(c, RedditRestClient.API_USER_NAME, userName);
        setSharedString(c, RedditRestClient.API_ACCESS_TOKEN, accessTkn);
        setSharedString(c, RedditRestClient.API_REFRESH_TOKEN, refreshTkn);
    }
    public static void resetUser(Context c) {
        setUser(c, null, null, null);
    }

    public static void writeUserDataIntoDb(Context c, JSONObject response) {
        ContentValues cv = new ContentValues();
        try {
            //Taking these data from JSON
            cv.put(UserEntry.COLUMN_NAME,
                    response.getString("name"));
            cv.put(UserEntry.COLUMN_ID,
                    response.getString("id"));
            //Taking these data from shared preferences.
            //They were written there by getJsonHttpResponseHandler()
            cv.put(UserEntry.COLUMN_ACCESS_TOKEN,
                    Util.getSharedString(c, RedditRestClient.API_ACCESS_TOKEN));
            cv.put(UserEntry.COLUMN_REFRESH_TOKEN,
                    Util.getSharedString(c, RedditRestClient.API_REFRESH_TOKEN));
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        c.getContentResolver()
                .insert(UserEntry.CONTENT_URI, cv);
    }

    public static void saveUserAccessToken(Context c) {
        ContentResolver cr = c.getContentResolver();
        ContentValues cv = new ContentValues();
        if (isLoggedIn(c)) {
            cv.put(UserEntry.COLUMN_ACCESS_TOKEN,
                    Util.getSharedString(c, RedditRestClient.API_ACCESS_TOKEN));
            cr.update(UserEntry.CONTENT_URI, cv, UserEntry.COLUMN_NAME + "=?",
                    new String[]{Util.getUserName(c)});

        } else {
            setSharedString(c, RedditRestClient.ANONYM_ACCESS_TOKEN,
                    getSharedString(c, RedditRestClient.API_ACCESS_TOKEN));
        }
    }

    public static void restoreAnonymousUserAccessToken(Context c) {
        setSharedString(c, RedditRestClient.API_ACCESS_TOKEN,
                getSharedString(c, RedditRestClient.ANONYM_ACCESS_TOKEN));
    }

    public static boolean subscribeSubredditInDb(Context c, boolean subscribe,
                                              String name) {
        boolean res = false;
        ContentResolver cr = c.getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(SubredditEntry.COLUMN_USER_IS_SUBSCRIBER, (subscribe ? 1 : 0));
        res = 0 < cr.update(SubredditEntry.CONTENT_URI, cv, SubredditEntry.COLUMN_NAME + "=?",
                new String[] {name});
        if (!res) {
            cv.put(SubredditEntry.COLUMN_NAME, name);
            res = null != cr.insert(SubredditEntry.CONTENT_URI, cv);
        }
        return res;
    }

    public static boolean subcrAnonymSubredditInDb(Context c, boolean subscribe,
                                                   String name) {
        ContentResolver cr = c.getContentResolver();
        if (subscribe) {
            ContentValues cv = new ContentValues();
            cv.put(AnonymSubscrEntry.COLUMN_NAME, name);
            return null != cr.insert(AnonymSubscrEntry.CONTENT_URI, cv);
        }else {
            return 1 == cr.delete(AnonymSubscrEntry.CONTENT_URI,
                    AnonymSubscrEntry.COLUMN_NAME + "=?",
                    new String[]{name});
        }
    }

    public static void writeSubredditsDataIntoDb(Context c, JSONObject response) {
        Vector<ContentValues> vec = null;
        try {
            JSONArray children = response.getJSONObject("data").getJSONArray("children");
            vec = new Vector<>(children.length());
            for(int i = 0; i < children.length(); i++) {
                JSONObject child = children.getJSONObject(i).getJSONObject("data");
                ContentValues cv = new ContentValues();
                cv.put(SubredditEntry.COLUMN_ID,
                        child.getString("id"));
                cv.put(SubredditEntry.COLUMN_NAME,
                        child.getString("display_name"));
                cv.put(SubredditEntry.COLUMN_OVER18,
                        child.getBoolean("over18") ? 1 : 0);
                cv.put(SubredditEntry.COLUMN_LANG,
                        child.getString("lang"));
                cv.put(SubredditEntry.COLUMN_REDDIT_URL,
                        child.getString("url"));
                cv.put(SubredditEntry.COLUMN_SUBREDDDIT_TYPE,
                        child.getString("subreddit_type"));
                cv.put(SubredditEntry.COLUMN_SUBMISSION_TYPE,
                        child.getString("submission_type"));
                cv.put(SubredditEntry.COLUMN_USER_IS_SUBSCRIBER,
                        getBoolean(child, "user_is_subscriber") ? 1 : 0);
                vec.add(cv);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        if ( vec != null && vec.size() > 0 ) {
            ContentValues[] cvs = new ContentValues[vec.size()];
            vec.toArray(cvs);
            c.getContentResolver()
                    .bulkInsert(SubredditEntry.CONTENT_URI, cvs);
        }
    }

    private static boolean getBoolean(JSONObject o, String name) throws JSONException {
        return !o.isNull(name) && o.getBoolean(name);
    }

    private static String getString(JSONObject o, String name) throws JSONException {
        return o.isNull(name) ? null : o.getString(name);
    }

    public static void deleteSubredditsFromDb(Context c) {
        c.getContentResolver().delete(SubredditEntry.CONTENT_URI, null, null);
    }

    public static void deleteLinksFromDb(Context c) {
        c.getContentResolver().delete(LinkEntry.CONTENT_URI, null, null);
    }

    public static void deleteCurrLinksFromDb(Context c) {
        c.getContentResolver().delete(CurrLinkEntry.CONTENT_URI, null, null);
    }

    public static void writeSubredditSearchIntoDb(Context c, JSONObject response) {
        ContentResolver cr = c.getContentResolver();
        Vector<ContentValues> vec = null;
        try {
            JSONArray names = response.getJSONArray("names");
            vec = new Vector<>(names.length());
            for (int i = 0; i < names.length(); i++) {
                ContentValues cv = new ContentValues();
                cv.put(SubredditSearchEntry.COLUMN_NAME, names.getString(i));
                vec.add(cv);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        if ( vec != null && vec.size() > 0 ) {
            ContentValues[] cvs = new ContentValues[vec.size()];
            vec.toArray(cvs);
            cr.bulkInsert(SubredditSearchEntry.CONTENT_URI, cvs);
        }
    }

    public static List<String> getSubscription(Context c) {
        ArrayList<String> res = new ArrayList<>();
        ContentResolver cr = c.getContentResolver();
        Cursor cur = null;
        if (isLoggedIn(c)) {
            cur = cr.query(SubredditEntry.CONTENT_URI, MainActivity.LOGGEDON_SUBREDDIT_LIST_COLUMNS,
                    null, null, MainActivity.SUBREDDITS_DEFAULT_SORT_ORDER);
        } else {
            cur = cr.query(SubredditEntry.buildUriWithSubpath(RedditContract.SUBPATH_ANONYMOUS),
                    null, null, null, null);
        }
        if (cur == null) {
            return null;
        }
        if (cur.getCount() == 0) {
            cur.close();
            return res;
        } else {
            if (cur.moveToFirst()) {
                do {

                } while (cur.moveToNext());
            }
            cur.close();
        }
        return res;
    }

    public static void writeLinkDataIntoDb(Context c, JSONObject response) {
        Point dispSize = getDisplaySize(c);
        int dispWidthPort = Math.min(dispSize.x, dispSize.y);
        int dispWidthLand = Math.max(dispSize.x, dispSize.y);
        ContentResolver cr = c.getContentResolver();
        Vector<ContentValues> vec = null;
        try {
            JSONArray children = response.getJSONObject("data").getJSONArray("children");
            vec = new Vector<>(children.length());
            for(int i = 0; i < children.length(); i++) {
                JSONObject child = children.getJSONObject(i).getJSONObject("data");
                ContentValues cv = new ContentValues();
                cv.put(LinkEntry.COLUMN_ID,
                        child.getString("id"));
                cv.put(LinkEntry.COLUMN_TITLE,
                        child.getString("title"));
                cv.put(LinkEntry.COLUMN_DOMAIN,
                        child.getString("domain"));
                cv.put(LinkEntry.COLUMN_AUTHOR,
                        child.getString("author"));
                cv.put(LinkEntry.COLUMN_SCORE,
                        child.getInt("score"));
                cv.put(LinkEntry.COLUMN_NUM_COMMENTS,
                        child.getInt("num_comments"));
                cv.put(LinkEntry.COLUMN_POST_HINT,
                        getString(child, "post_hint"));
                cv.put(LinkEntry.COLUMN_STICKIED,
                        child.getBoolean("stickied") ? 1 : 0);
                cv.put(LinkEntry.COLUMN_OVER_18,
                        child.getBoolean("over_18") ? 1 : 0);
                cv.put(LinkEntry.COLUMN_SUBREDDIT,
                        child.getString("subreddit"));
                cv.put(LinkEntry.COLUMN_SUBREDDIT_ID,
                        child.getString("subreddit_id"));
                cv.put(LinkEntry.COLUMN_AUTHOR_FL_TEXT,
                        child.getString("author_flair_text"));
                cv.put(LinkEntry.COLUMN_SLFTXT_HTML,
                        child.getString("selftext_html"));
                cv.put(LinkEntry.COLUMN_SLFTXT,
                        child.getString("selftext"));
                cv.put(LinkEntry.COLUMN_CREATED,
                        child.getInt("created"));
                cv.put(LinkEntry.COLUMN_CREATED_UTC,
                        child.getInt("created_utc"));
                cv.put(LinkEntry.COLUMN_PERMALINK,
                        child.getString("permalink"));
                cv.put(LinkEntry.COLUMN_URL,
                        child.getString("url"));
                cv.put(LinkEntry.COLUMN_THUMBNAIL,
                        child.getString("thumbnail"));

                if (!child.isNull("preview") && !child.getJSONObject("preview").isNull("images")) {
                    JSONArray images = child.getJSONObject("preview").getJSONArray("images");
                    JSONObject imgSrc = images.getJSONObject(0).getJSONObject("source");
                    String urlPort = imgSrc.getString("url");
                    String urlLand = urlPort;
                    int widthPort = imgSrc.getInt("width");
                    int widthLand = widthPort;
                    int heightPort = imgSrc.getInt("height");
                    int heightLand = heightPort;
                    boolean foundPort = false;
                    boolean foundLand = false;
                    JSONArray imgRslns = images.getJSONObject(0).getJSONArray("resolutions");
                    //Assuming image sizes are in ascending order
                    for (int j = 0; j < imgRslns.length(); j++) {
                        JSONObject imgData = imgRslns.getJSONObject(j);
                        String url = imgData.getString("url");
                        int width = imgData.getInt("width");
                        int height = imgData.getInt("height");
                        if (!foundPort && width >= dispWidthPort) {
                            //take first image that is wider than screen width in portrait
                            foundPort = true;
                            urlPort = url;
                            widthPort = width;
                            heightPort = height;
                        }
                        if (!foundLand && width >= dispWidthLand) {
                            //take first image that is wider than screen width in landscape
                            foundLand = true;
                            urlLand = url;
                            widthLand = width;
                            heightLand = height;
                        }
                        if (foundLand && foundPort)
                            break;
                    }
                    cv.put(LinkEntry.COLUMN_IMG_PORT, urlPort);
                    cv.put(LinkEntry.COLUMN_IMG_PORT_WIDTH, widthPort);
                    cv.put(LinkEntry.COLUMN_IMG_PORT_HEIGHT, heightPort);
                    cv.put(LinkEntry.COLUMN_IMG_LAND, urlLand);
                    cv.put(LinkEntry.COLUMN_IMG_LAND_WIDTH, widthLand);
                    cv.put(LinkEntry.COLUMN_IMG_LAND_HEIGH, heightLand);
                }
                vec.add(cv);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        if ( vec != null && vec.size() > 0 ) {
            ContentValues[] cvs = new ContentValues[vec.size()];
            vec.toArray(cvs);
            cr.bulkInsert(LinkEntry.CONTENT_URI, cvs);
        }
    }

    public static void updateSubredditPositionInDb(Context c, String subreddit, int pos) {
        ContentValues cv = new ContentValues();
        cv.put(CurrLinkEntry.COLUMN_SUBREDDIT, subreddit);
        cv.put(CurrLinkEntry.COLUMN_POSITION, pos);
        c.getContentResolver().insert(CurrLinkEntry.CONTENT_URI, cv);
    }

    public static int getCount(Context c, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = c.getContentResolver().query(uri, new String[] {"count(*)"},
                selection, selectionArgs, null);
        if (cursor == null) {
            return -1;
        }
        if (cursor.getCount() == 0) {
            cursor.close();
            return 0;
        } else {
            cursor.moveToFirst();
            int result = cursor.getInt(0);
            cursor.close();
            return result;
        }
    }

    public static String getLinkPositionString(Context c, int pos, int count) {
        pos++; //switch from 0-based index to 1-based index
        return String.format(c.getString(R.string.position_format_str),
                (pos < count ? "<" : ""), pos, count, (pos > 1 ? ">" : ""));
    }

    public static String getRelativeTime(long timeSec) {
        return DateUtils.getRelativeTimeSpanString(1000 * (long)timeSec).toString();
    }

    public static String getRelativeLocalTimeFromUTCtime(long timeSec) {
        long timeOffsetSec = TimeZone.getDefault().getRawOffset() / 1000;
        return getRelativeTime(timeSec + timeOffsetSec);
    }

    public static Point getDisplaySize(Context c) {
        WindowManager wm = (WindowManager)c.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static float getFloatFromResources(Resources r, int resourceID) {
        TypedValue typedValue = new TypedValue();
        r.getValue(resourceID, typedValue, true);
        return typedValue.getFloat();
    }

    public static int getListPreferredItemHeight(Activity activity)
    {
        TypedValue value = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, value, true);
        TypedValue.coerceToString(value.type, value.data);
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (int)value.getDimension(metrics);
    }

    public static void printCursorToLog(String logTag, Cursor c) {
        if (c != null && c.moveToFirst()) {
            do {
                String s = "";
                for (int i = 0; i < c.getColumnCount(); i++) {
                    s += c.getString(i) + "\t\t";
                }
                Log.d(logTag, s);
            }while (c.moveToNext());
        }
    }

    /**
     * Returns a CharSequence that concatenates the specified array of CharSequence
     * objects and then applies a list of zero or more tags to the entire range.
     *
     * @param content an array of character sequences to apply a style to
     * @param tags the styled span objects to apply to the content
     *        such as android.text.style.StyleSpan
     *
     */
    private static CharSequence apply(CharSequence[] content, Object... tags) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        openTags(text, tags);
        for (CharSequence item : content) {
            text.append(item);
        }
        closeTags(text, tags);
        return text;
    }

    /**
     * Iterates over an array of tags and applies them to the beginning of the specified
     * Spannable object so that future text appended to the text will have the styling
     * applied to it. Do not call this method directly.
     */
    private static void openTags(Spannable text, Object[] tags) {
        for (Object tag : tags) {
            text.setSpan(tag, 0, 0, Spannable.SPAN_MARK_MARK);
        }
    }

    /**
     * "Closes" the specified tags on a Spannable by updating the spans to be
     * endpoint-exclusive so that future text appended to the end will not take
     * on the same styling. Do not call this method directly.
     */
    private static void closeTags(Spannable text, Object[] tags) {
        int len = text.length();
        for (Object tag : tags) {
            if (len > 0) {
                text.setSpan(tag, 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                text.removeSpan(tag);
            }
        }
    }

    /**
     * Returns a CharSequence that applies boldface to the concatenation
     * of the specified CharSequence objects.
     */
    public static CharSequence bold(CharSequence... content) {
        return apply(content, new StyleSpan(Typeface.BOLD));
    }

    /**
     * Returns a CharSequence that applies italics to the concatenation
     * of the specified CharSequence objects.
     */
    public static CharSequence italic(CharSequence... content) {
        return apply(content, new StyleSpan(Typeface.ITALIC));
    }

    /**
     * Returns a CharSequence that applies a foreground color to the
     * concatenation of the specified CharSequence objects.
     */
    public static CharSequence color(int color, CharSequence... content) {
        return apply(content, new ForegroundColorSpan(color));
    }

    public static CharSequence underline(CharSequence... content) {
        return apply(content, new UnderlineSpan());
    }

    public static CharSequence indentDrawable(Context c, int indentation,
                                              CharSequence... content) {
//        return apply(content, new DrawableMarginSpan(ContextCompat.getDrawable(c,
//                R.drawable.dotted_border), indentation));
        return apply(content, new LeadingMarginSpan.Standard(indentation));
    }

    public static CharSequence indent(int indentation, CharSequence... content) {
        return apply(content, new LeadingMarginSpan.Standard(indentation));
    }

    public static CharSequence getSubredditNameWithR(Context c, String subredditname) {
        SpannableStringBuilder res = new SpannableStringBuilder(
                Util.color(ContextCompat.getColor(c, R.color.colorForR),
                        c.getString(R.string.subreddit_prefix)));
        res.append(subredditname);
        return res;
    }

    /**
     * Test method for testing 'New User' dialog with different amount of users.
     */
    public static void createUserSelectionDialogTestData(Context c) {
        ContentResolver cr = c.getContentResolver();
        cr.delete(UserEntry.CONTENT_URI, null, null);
        final boolean ADD_USERS = false;
        if (ADD_USERS) {
            final int NUM_USERS = 3;
            ContentValues[] cvs = new ContentValues[NUM_USERS];
            for (int i = 0; i < NUM_USERS; i++) {
                cvs[i] = new ContentValues();
                cvs[i].put(UserEntry.COLUMN_NAME, "user " + i);
                cvs[i].put(UserEntry.COLUMN_ID, "REDDIT_ID_" + i);
                cvs[i].put(UserEntry.COLUMN_ACCESS_TOKEN, "ACCESS_TOKEN_" + i);
                cvs[i].put(UserEntry.COLUMN_REFRESH_TOKEN, "REFRESH_TOKEN_" + i);
            }
            cr.bulkInsert(UserEntry.CONTENT_URI, cvs);
        }
        final int CURRENT_USER_INDEX = 1;
        final boolean SET_CURRENT_USER = false;
        if (SET_CURRENT_USER) {
            setUser(c, "user " + CURRENT_USER_INDEX,
                    "ACCESS_TOKEN_" + CURRENT_USER_INDEX, "REFRESH_TOKEN_" + CURRENT_USER_INDEX);
        } else {
            resetUser(c);
        }
    }
}
