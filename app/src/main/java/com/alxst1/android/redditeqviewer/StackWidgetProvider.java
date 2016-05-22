package com.alxst1.android.redditeqviewer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * Created by AlexSt on 10/13/2015.
 */
public class StackWidgetProvider extends AppWidgetProvider {
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        // update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            setUpRemoteCollectionView(context, rv, appWidgetIds[i]);
            setCollectionItemOnClickIntent(context, rv, appWidgetIds[i]);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds[i], R.id.stack_view);
            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void setUpRemoteCollectionView(Context context, RemoteViews widgetLayoutRemoteView,
                                           int widgetId) {
        // Here we setup the intent which points to the StackViewService
        // which will provide the views for this collection.
        Intent intent = new Intent(context, StackWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        // When intents are compared, the extras are ignored, so we need to
        // embed the extras into the data so that the extras will not be ignored.
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        widgetLayoutRemoteView.setRemoteAdapter(widgetId, R.id.stack_view, intent);
        // The empty view is displayed when the collection has no items. It
        // should be a sibling of the collection view.
        widgetLayoutRemoteView.setEmptyView(R.id.stack_view, R.id.empty_view);
    }

    private void setCollectionItemOnClickIntent(Context context, RemoteViews widgetLayoutRemoteView,
                                                int widgetId) {
        // Here we setup the a pending intent template. Individuals items of
        // a collection cannot setup their own pending intents, instead, the collection
        // as a whole can setup a pending intent template, and the individual items can set
        // a fillInIntent to create unique before on an item to item basis.
        Intent intent = new Intent(context, LinkWithCommentsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(Constants.ACTION_SHOW_LINKS_FOR_SUBREDDIT);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                widgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        widgetLayoutRemoteView.setPendingIntentTemplate(R.id.stack_view, pendingIntent);
    }
}
