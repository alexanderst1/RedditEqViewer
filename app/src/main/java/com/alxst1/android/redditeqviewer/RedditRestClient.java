package com.alxst1.android.redditeqviewer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.message.BasicHeader;

/**
 * Created by Alexander on 5/1/2016.
 */

public class RedditRestClient {
    private static final String LOG_TAG = RedditRestClient.class.getSimpleName();

    //These 3 constants are JSON response parameter names (defined in Reddit API) and also
    //HTTP PUT parameter names and also they are used as keys to write into shared preferences
    public static final String API_ACCESS_TOKEN = "access_token";
    public static final String API_REFRESH_TOKEN = "refresh_token";
    public static final String API_USER_NAME = "name";

    public static final String API_INSTALLED_CLIENT = "https://oauth.reddit.com/grants/installed_client";
    public static final String ANONYM_ACCESS_TOKEN = "anonym_access_token";

    private static final int STATUS_CODE_AUTHORIZATION_FAILED = 401;

    private Context mContext;
    private static AsyncHttpClient mHttp = new AsyncHttpClient();

    private static final String BASE_URL = "https://www.reddit.com";
    private static final String BASE_URL_OAUTH = "https://oauth.reddit.com";

    private enum RequestType {
        Get,
        Post
    }

    public enum SubrdtDisplayOrder {
        POPULAR,
        NEW,
        GOLD,
        DEFAULT
    }

    public enum LinkDisplayOrder {
        TOP,
        CONTROVERSIAL,
        HOT,
        NEW,
        RANDOM
    }

    public RedditRestClient(Context c){
        mContext = c;
    }

    public void get(boolean isOauthUrl, String url, Header[] headers, RequestParams params,
                    AsyncHttpResponseHandler responseHandler) {
        mHttp.get(mContext, getAbsoluteUrl(isOauthUrl, url) + ".json", headers, params,
                responseHandler);
    }
    public void post(boolean isOauthUrl, String url, Header[] headers, RequestParams params,
                     AsyncHttpResponseHandler responseHandler) {
        mHttp.post(mContext, getAbsoluteUrl(isOauthUrl, url), headers, params, null,
                responseHandler);

    }

    private String getAbsoluteUrl(boolean isOauth, String relativeUrl) {
        if (isOauth)
            return BASE_URL_OAUTH + relativeUrl;
        else
            return BASE_URL + relativeUrl;
    }

    interface JsonResultHandler {
        void onSuccess(JSONObject response);
        void onSuccess(JSONArray response);
        void onFailure(int errorCode);
    }

    public interface ResultHandler {
        void onSuccess();
        void onFailure(int errorCode);
    }

    interface ResultSuccessHandler {
        void onSuccess();
    }

    /**
     * @return Array of 1 or 2 headers.
     * First element is USER-AGENT constant string.
     * Second element exists only for logged on user and is ACCESS TOKEN taken from shared
     * preferences.
     */
    private Header[] getHeaders() {
        Header[] headers = new Header[2];
        headers[0] = new BasicHeader("User-Agent", Constants.USER_AGENT);
        headers[1] = new BasicHeader("Authorization", "bearer " +
                Util.getSharedString(mContext, API_ACCESS_TOKEN));
        return headers;
    }

    /**
     * Used in 'sendRequest' to provide common handling of 'success' and 'failure' responses.
     * * For 'success':
     *  - Logs up to 300 characters of JSON
     *  - Retrieves parameters of interest from JSON and writes them into shared preferences
     *  - Calls result handler 'resHandler.onSuccess(response)' passing JSON for further processing
     * * For 'failure':
     *  - Logs status code, JSON, header
     *  - Calls result handler 'resHandler.onFailure(statusCode)' passing status code for further
     *  processing
     *
     * @param relUrl
     * @param respParNames
     * @param resHandler
     * @return
     */
    private JsonHttpResponseHandler getJsonHttpResponseHandler(
            final String relUrl, final String[] respParNames,
            final JsonResultHandler resHandler) {
        return new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //super.onSuccess(statusCode, headers, response);
                //Logging
                String respStr = response.toString();
                int maxLength = 300;
                Log.d(LOG_TAG, relUrl + " response: " + (respStr.length() > maxLength ?
                        respStr.substring(0, maxLength - 1) + "..." : respStr));
                try {
                    if (respParNames != null) {
                        for (String respParName : respParNames) {
                            String respParVal = response.getString(respParName);
                            Util.setSharedString(mContext, respParName, respParVal);
                            Log.d(LOG_TAG, relUrl + " " + respParName + ": " + respParVal);
                        }
                    }
                    if (resHandler != null) resHandler.onSuccess(response);
                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                String respStr = response.toString();
                int maxLength = 300;
                Log.d(LOG_TAG, relUrl + " response: " + (respStr.length() > maxLength ?
                        respStr.substring(0, maxLength - 1) + "..." : respStr));
                if (resHandler != null) resHandler.onSuccess(response);
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject errorResponse) {
                //super.onFailure(statusCode, headers, throwable, errorResponse);
                onFailureCommon(statusCode, headers,
                        errorResponse == null ? null : errorResponse.toString());
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString,
                                  Throwable throwable) {
                //super.onFailure(statusCode, headers, responseString, throwable);
                onFailureCommon(statusCode, headers, responseString);
            }
            private void onFailureCommon(int statusCode, Header[] headers, String responseString) {
                String msg = "Failure status code: " + statusCode;
                if (responseString != null)
                    msg += ", response: " + responseString;
                Log.d(LOG_TAG, relUrl + " " + msg);
                if (headers != null) {
                    int i = 0;
                    for (Header h : headers) {
                        //if (h.getName().equals("www-authenticate")) {
                            Log.d(LOG_TAG, "header " + i + ":" + h.toString());
                        //}
                        i++;
                    }
                }
                if (resHandler != null) resHandler.onFailure(statusCode);
            }
        };
    }

    private void sendRequest(final RequestType reqType, final boolean isOauth, final String relUrl,
                             final Header[] headers, final RequestParams params,
                             final String[] respParNames, final boolean tryToRefreshTokenOnFailure,
                            final JsonResultHandler handler) {

        JsonResultHandler jsonResHandler = new JsonResultHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                if (handler != null)
                    handler.onSuccess(response);
            }
            @Override
            public void onSuccess(JSONArray response) {
                if (handler != null)
                    handler.onSuccess(response);
            }
            @Override
            public void onFailure(int statusCode) {
                //Check for 2 conditions:
                //(1) If access token expired, it will be STATUS_CODE_AUTHORIZATION_FAILED.
                //If it is not, then it would be useless to try to refresh token.
                //(2) If we already tried to refresh token and we are here again, then
                //something went wrong and we should not try again to avoid infinite
                //recursion
                if (statusCode != STATUS_CODE_AUTHORIZATION_FAILED ||
                        !tryToRefreshTokenOnFailure/* || !mIsLoggedIn*/) {
                    handler.onFailure(statusCode);
                } else {
                    //Trying to get new access token by providing refresh token
                    refreshAccessToken(new ResultSuccessHandler() {
                        @Override
                        public void onSuccess() {
                            //Recursive call to 'postAction' to try to achieve what
                            //failed first time due to an expired token.
                            //We are not passing original 'headers' but calling
                            //'getHeaders()' again here to get new access token
                            //retrieved during token refresh
                            sendRequest(reqType, isOauth, relUrl, getHeaders(), params,
                                    respParNames, false /*no token refresh*/, handler);
                        }
                    });
                }
            }
        };
        switch (reqType) {
            case Get:
                get(isOauth, relUrl, headers, params,
                        getJsonHttpResponseHandler(relUrl, respParNames, jsonResHandler));
                break;
            case Post:
                post(isOauth, relUrl, headers, params,
                        getJsonHttpResponseHandler(relUrl, respParNames, jsonResHandler));
                break;
            default:
        }
    }

    /**
     * Is called from 'RedditAuthorizationActivity' if the activity successfully obtained
     * AUTHORIZATION CODE from Reddit after user authorized access to its account by this
     * application.
     * Sends POST request with AUTHORIZATION CODE requesting ACCESS and REFRESH TOKENS.
     * On success, writes ACCESS and REFRESH TOKENS into shared preferences and calls
     * 'onUserChangedToLoggedIn'
     *
     * @param authorizationCode
     */
    public void beginRetrievingNewUserAccessTokenAndChangingUser(String authorizationCode) {
        mHttp.setBasicAuth(Constants.CLIENT_ID, "");
        RequestParams par = new RequestParams();
        par.put("code", authorizationCode);
        par.put("grant_type", "authorization_code");
        par.put("redirect_uri", Constants.REDIRECT_URI);
        sendRequest(RequestType.Post, false, "/api/v1/access_token", null, par,
                new String[]{API_ACCESS_TOKEN, API_REFRESH_TOKEN}, false /*tryToRefreshAccessToken*/,
                new JsonResultHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        beginRetrievingUserName(null);
                    }
                    @Override
                    public void onSuccess(JSONArray response) {
                    }
                    @Override
                    public void onFailure(int statusCode) {
                    }
                });
    }

    /**
     * Sends POST request with REFRESH TOKEN requesting new ACCESS TOKEN instead of expired one.
     * On success, writes new ACCESS TOKEN into shared preferences and calls a handler passed as
     * an input parameter. The handler is used in functions 'getAction'/'postAction' so that if
     * GET or POST action failed due to access token expiration, it could call 'refreshAccessToken'
     * passing itself as a handler so that 'getAction'/'postAction' could be recursively called
     * again with a new access token.
     *
     * @param handler
     */
    private void refreshAccessToken(final ResultSuccessHandler handler) {
        mHttp.setBasicAuth(Constants.CLIENT_ID, "");
        RequestParams par = new RequestParams();
        if (Util.isLoggedIn(mContext)) {
            par.put("grant_type", API_REFRESH_TOKEN);
            par.put(API_REFRESH_TOKEN, Util.getSharedString(mContext, API_REFRESH_TOKEN));
        } else {
            par.put("grant_type", API_INSTALLED_CLIENT);
            par.put("device_id", Constants.OAUTH_STATE);
        }
        sendRequest(RequestType.Post, false, "/api/v1/access_token", null, par,
                new String[]{API_ACCESS_TOKEN}, false /*tryToRefreshAccessToken*/,
                new JsonResultHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Util.saveUserAccessToken(mContext);
                        if (handler != null)
                            handler.onSuccess();
                    }
                    @Override
                    public void onSuccess(JSONArray response) {
                    }
                    @Override
                    public void onFailure(int statusCode) {
                    }
                });
    }

    /**
     * According to Reddit API rules, good citizens should call this when they don't need access
     * token anymore. We always need access token, so never call this function.
     * Implemented it just in case...
     */
    public void revokeToken() {
        final String accessToken = Util.getSharedString(mContext, API_ACCESS_TOKEN);
        if (accessToken == null) return;
        mHttp.setBasicAuth(Constants.CLIENT_ID, "");
        RequestParams par = new RequestParams();
        par.put("token", accessToken);
        par.put("token_type_hint","access_token");
        sendRequest(RequestType.Post, false, "/api/v1/revoke_token", null, par, null,
                false /*tryToRefreshAccessToken*/, new JsonResultHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Util.removeSharedString(mContext, API_ACCESS_TOKEN);
                    }
                    @Override
                    public void onSuccess(JSONArray response) {
                    }
                    @Override
                    public void onFailure(int statusCode) {
                    }
                });
    }

    /**
     * Sends OAUTH2 GET request for user information. If ACCESS TOKEN expired, it triggers
     * REFRESHING of ACCESS TOKEN. In the end, retrieved USER NAME and its ACCESS and REFRESH
     * TOKENS are written into shared preferences and database
     *
     * @param successHandler
     */
    public void beginRetrievingUserName(final ResultSuccessHandler successHandler) {
        //Util.removeSharedString(mContext, API_USER_NAME);
        sendRequest(RequestType.Get, true, "/api/v1/me", getHeaders(), null,
            new String[]{API_USER_NAME}, true, new JsonResultHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                Util.writeUserDataIntoDb(mContext, response);
                //notify main_activity action that user changed
                Intent intent = new Intent(Constants.NEW_USER_EVENT);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                if (successHandler != null)
                    successHandler.onSuccess();
            }
            @Override
            public void onSuccess(JSONArray response) {
            }
            @Override
            public void onFailure(int errorCode) {
            }
        });
    }

    public void beginRetrievingMySubreddits() {
        sendRequest(RequestType.Get, true, "/subreddits/mine/subscriber", getHeaders(), null, null,
                true, new JsonResultHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                Util.writeSubredditsDataIntoDb(mContext, response);
                Intent intent = new Intent(Constants.MY_SUBREDDITS_RETRIEVED_EVENT);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
            }
            @Override
            public void onSuccess(JSONArray response) {
            }
            @Override
            public void onFailure(int errorCode) {
            }
        });
    }

    public void beginRetrievingSubreddits(SubrdtDisplayOrder displayOrder) {
        String order;
        switch (displayOrder) {
            case POPULAR:
                order = "popular";
                break;
            case NEW:
                order = "new";
                break;
            case GOLD:
                order = "gold";
                break;
            case DEFAULT:
            default:
                order = "default";
        }
        String relUrl = "/subreddits/" + order;
        RequestParams par = null;
        //par = new RequestParams();
        //par.put("limit", "100"); //TODO: add 'limit=?' to App Settings
        //par.put("show","all"); //TODO: add 'show=all' to App Settings
        sendRequest(RequestType.Get, true, relUrl, getHeaders(), par, null, true,
                new JsonResultHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                Util.writeSubredditsDataIntoDb(mContext, response);
                Intent intent = new Intent(Constants.SUBREDDITS_RETRIEVED_EVENT);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
            }
            @Override
            public void onSuccess(JSONArray response) {
            }
            @Override
            public void onFailure(int errorCode) {
            }
        });
    }

    public void beginRetrievingLinks(String subreddit, LinkDisplayOrder order) {
        String sOrder;
        switch (order) {
            case CONTROVERSIAL:
                sOrder = "controversial";
                break;
            case HOT:
                sOrder = "hot";
                break;
            case NEW:
                sOrder = "new";
                break;
            case RANDOM:
                sOrder = "random";
                break;
            case TOP:
            default:
                sOrder = "top";
        }
        String relUrl = "/r/" + subreddit + "/" + sOrder;
        RequestParams par = null;
        par = new RequestParams();
        //par.put("limit", "10"); //TODO: add 'limit=?' to App Settings
        //par.put("show","all"); //TODO: add 'show=all' to App Settings
        sendRequest(RequestType.Get, true, relUrl, getHeaders(), par, null, true,
                new JsonResultHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Util.writeLinkDataIntoDb(mContext, response);
                        Intent intent = new Intent(Constants.LINKS_RETRIEVED_EVENT);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                    }
                    @Override
                    public void onSuccess(JSONArray response) {
                    }
                    @Override
                    public void onFailure(int errorCode) {
                    }
                });
    }


    public void subscribeSubreddit(final boolean subscribe,
                                            final String fullName, final ResultHandler handler) {
        RequestParams par = new RequestParams();
        par.put("action", subscribe ? "sub" : "unsub");
        par.put("sr_name", fullName);
        sendRequest(RequestType.Post, true, "/api/subscribe", getHeaders(), par, null, true,
                new JsonResultHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Util.subscribeSubredditInDb(mContext, subscribe, fullName);
                        if (handler != null)
                            handler.onSuccess();
                    }
                    @Override
                    public void onSuccess(JSONArray response) {
                    }
                    @Override
                    public void onFailure(int statusCode) {
                        if (handler != null)
                            handler.onFailure(statusCode);
                    }
                });
    }

    public void beginSearchRedditNames(final String query, final ResultHandler handler) {
        RequestParams par = new RequestParams();
        par.put("exact", "false" ); //TODO: add 'exact=?' to App Settins
        par.put("include_over_18", Util.FilterNsfw(mContext) ? "false" : "true");
        par.put("query", query);
        sendRequest(RequestType.Post, true, "/api/search_reddit_names", getHeaders(), par,
                null /*respParNames*/, true, new JsonResultHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Util.writeSubredditSearchIntoDb(mContext, response);
                        if (handler != null)
                            handler.onSuccess();
                    }
                    @Override
                    public void onSuccess(JSONArray response) {
                    }
                    @Override
                    public void onFailure(int statusCode) {
                        if (handler != null)
                            handler.onFailure(statusCode);
                    }
                });
    }

    public void beginRetrievingComments(String subreddit, String linkId, final JsonResultHandler
            handler) {
        String relUrl = "/r/" + subreddit + "/comments/" + linkId;
        RequestParams par = null;
        par = new RequestParams();
        par.put("depth", "5"); //TODO: add 'depth=?' to App Settings
        par.put("limit","50"); //TODO: add 'limit=?' to App Settings
        sendRequest(RequestType.Get, true, relUrl, getHeaders(), par, null, true, handler);
    }
}
