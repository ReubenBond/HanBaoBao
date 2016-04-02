package com.tallogre.hanbaobao;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tallogre.hanbaobao.Utilities.CharacterUtil;
import com.tallogre.hanbaobao.Utilities.Globals;
import com.tallogre.hanbaobao.Utilities.UserPreferences;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class ScreenReaderService extends AccessibilityService {
    private static final String TAG = "ScreenReaderService";
    private Boolean isServiceBound = false;
    private ServiceConnection serviceConnection;
    private BackgroundService backgroundService;
    private EditTextTranslator editTextTranslator = new EditTextTranslator();
    private final CharSequence thisPackageName;

    public ScreenReaderService() {
        thisPackageName = Globals.getApplication().getPackageName();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackgroundService.ACTION_STOP);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopSelf();
            }
        }, intentFilter);
        return result;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            info = getServiceInfo();
        } else {
            info = new AccessibilityServiceInfo();
        }

        info.flags |= AccessibilityServiceInfo.DEFAULT;
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED
                | AccessibilityEvent.TYPE_VIEW_FOCUSED
                | AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
                | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                | AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        }*/

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        }*/

        info.notificationTimeout = 500;
        setServiceInfo(info);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                backgroundService = ((BackgroundService.ServiceBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                backgroundService = null;
            }
        };
        if (backgroundService == null) connectToBackgroundService();
    }

    private void connectToBackgroundService() {
        if (backgroundService == null) {
            Intent serviceIntent = new Intent(this, BackgroundService.class);
            serviceIntent.setAction(BackgroundService.ACTION_BACKGROUND);
            startService(serviceIntent);
            getApplicationContext().bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
            isServiceBound = true;
        }
    }

    private CharSequence getEventText(AccessibilityEvent event) {
        if (event.getText().size() == 1) {
            return event.getText().get(0);
        }

        StringBuilder sb = new StringBuilder();
        for (CharSequence s : event.getText()) {
            sb.append(s);
        }
        return sb;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (backgroundService != null) return backgroundService.onKeyEvent(event);
        return super.onKeyEvent(event);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //printEvent(event);
        // Ignore events from this package.
        if (event.getClassName() == null || CharacterUtil.equals(thisPackageName, event.getPackageName())) return;
        CharSequence className = event.getClassName();
        CharSequence eventText = null;
        boolean isEditText = event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        if (isEditText || CharacterUtil.endsWith(className, "TextView") || isFacebookMessengerMessage(event, className)) {
            eventText = getEventText(event);
        }

        if (eventText == null || eventText.length() == 0) return;

        if (backgroundService == null) connectToBackgroundService();
        if (backgroundService != null) {
            if (isEditText) {
                editTextTranslator.setEvent(event, eventText);
            } else {
                backgroundService.onAccessibilityEventText(eventText);
            }
        }
    }

    private StringBuilder b = new StringBuilder();

    private void printEvent(AccessibilityEvent event) {
        b.setLength(0);
        b.append("onAccessibilityEvent: Type: ")
                .append(AccessibilityEvent.eventTypeToString(event.getEventType()))
                .append(" Class: ").append(event.getClassName())
                .append(" Package: ").append(event.getPackageName())
                .append(" Time: ").append(event.getEventTime())
                .append("\nText: ").append(getEventText(event))
                .append("\nFrom: ").append(event.getFromIndex())
                .append("\tTo: ").append(event.getToIndex())
                .append("\nAction: ").append(getActionSymbolicName(event.getAction()))
                //.append("\n\tContent Change Type: ").append(getContentChangeTypeString(event.getContentChangeTypes()));
                .append("\nBefore Text: ").append(event.getBeforeText())
                .append("\nRecords: ").append(event.getRecordCount())
                .append("\nAdded: ").append(event.getAddedCount())
                .append("\tRemoved: ").append(event.getRemovedCount())
                .append("\nContent Description: ").append(event.getContentDescription())
                .append("\nCurrent Item: ").append(event.getCurrentItemIndex()).append("/").append(event.getItemCount());
        Parcelable p = event.getParcelableData();

        if (p != null) {
            b.append("\nHas parcel");
        }

        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Bundle extras = source.getExtras();
                if (extras != null) {
                    for (String key : extras.keySet()) {
                        b.append("\n\tExtra [").append(key).append("] = ").append(extras.get(key));
                    }
                }
            }
        }

        Log.v(TAG, b.toString());
    }

    private boolean isFacebookMessengerMessage(AccessibilityEvent event, CharSequence className) {
        return CharacterUtil.endsWith(className, ".LinearLayout") && CharacterUtil.equals("com.facebook.orca", event.getPackageName());
    }

    @Override
    public void onInterrupt() {
        ServiceConnection connection = serviceConnection;
        if (connection == null || !isServiceBound) return;
        getApplicationContext().unbindService(connection);
        isServiceBound = false;
    }

    static String getContentChangeTypeString(int contentChangeType) {
        switch (contentChangeType) {
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION:
                return "CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION";
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE:
                return "CONTENT_CHANGE_TYPE_SUBTREE";
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT:
                return "CONTENT_CHANGE_TYPE_TEXT";
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED:
                return "CONTENT_CHANGE_TYPE_UNDEFINED";
            default:
                return "CONTENT_CHANGE_TYPE_UNKNOWN";
        }
    }

    static String getActionSymbolicName(int action) {
        switch (action) {
            case AccessibilityNodeInfo.ACTION_FOCUS:
                return "ACTION_FOCUS";
            case AccessibilityNodeInfo.ACTION_CLEAR_FOCUS:
                return "ACTION_CLEAR_FOCUS";
            case AccessibilityNodeInfo.ACTION_SELECT:
                return "ACTION_SELECT";
            case AccessibilityNodeInfo.ACTION_CLEAR_SELECTION:
                return "ACTION_CLEAR_SELECTION";
            case AccessibilityNodeInfo.ACTION_CLICK:
                return "ACTION_CLICK";
            case AccessibilityNodeInfo.ACTION_LONG_CLICK:
                return "ACTION_LONG_CLICK";
            case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS:
                return "ACTION_ACCESSIBILITY_FOCUS";
            case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                return "ACTION_CLEAR_ACCESSIBILITY_FOCUS";
            case AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY:
                return "ACTION_NEXT_AT_MOVEMENT_GRANULARITY";
            case AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY:
                return "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY";
            case AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT:
                return "ACTION_NEXT_HTML_ELEMENT";
            case AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT:
                return "ACTION_PREVIOUS_HTML_ELEMENT";
            case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
                return "ACTION_SCROLL_FORWARD";
            case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
                return "ACTION_SCROLL_BACKWARD";
            case AccessibilityNodeInfo.ACTION_CUT:
                return "ACTION_CUT";
            case AccessibilityNodeInfo.ACTION_COPY:
                return "ACTION_COPY";
            case AccessibilityNodeInfo.ACTION_PASTE:
                return "ACTION_PASTE";
            case AccessibilityNodeInfo.ACTION_SET_SELECTION:
                return "ACTION_SET_SELECTION";
            default:
                return "ACTION_UNKNOWN";
        }
    }

    private class EditTextTranslator implements Observer<CharSequence>,UserPreferences.OnChangeListener {
        private static final String TAG = "EditTextTranslator";

        private AccessibilityNodeInfo source;
        private static final String QUERY_PATTERN_SUFFIX = "[^\\d][^#]+";
        private String queryPatternPrefix;
        private Pattern queryPattern;
        private CharSequence inputText;
        private int start;
        private int end;
        private Subscription subscription;
        private Rect sourceBounds = new Rect();

        public EditTextTranslator() {
            Globals.getUserPreferences().addOnChangeListener(this);
            onPreferencesChanged();
        }

        public void setEvent(AccessibilityEvent event, CharSequence eventText) {
            source = event.getSource();
            if (source == null) return;
            if (eventText == null) return;
            this.inputText = eventText;

            // Find the translation mark
            Matcher matcher = queryPattern.matcher(eventText);
            if (!matcher.find()) {

                // If the user was previously looking up a term and has now deleted it, collapse the
                // search window.
                if (backgroundService != null && end != 0) {
                    backgroundService.widget.collapseIntoIcon();
                }

                start = end = 0;
                return;
            }

            start = matcher.start();
            end = matcher.end();
            //Log.i(TAG, "FOUND from " + start + " to " + end + ": query = [" + inputText.subSequence(start,end) + "]");

            // Extract the query
            // save the query location for later.
            if (backgroundService != null) {
                if (subscription != null) subscription.unsubscribe();
                source.getBoundsInScreen(sourceBounds);
                int aboveYCoordinate = Math.max(280, sourceBounds.top - 48);
                CharSequence query = inputText.subSequence(start + queryPatternPrefix.length(), end);
                subscription = backgroundService.widget.onSearchDictionary(query.toString(), aboveYCoordinate).observeOn(AndroidSchedulers.mainThread()).subscribe(editTextTranslator);
            }
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            Log.e(TAG, "Error looking up text \"" + inputText + "\"", e);
        }

        @Override
        public void onNext(CharSequence replacement) {
            if (replacement == null) return;
            setText(source, replacement);
        }

        private void setText(AccessibilityNodeInfo source, CharSequence text) {
            if (source == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                source.refresh();
            }

            if (inputText.length() < end || inputText.length() < start) return;
            if (!queryPattern.matcher(inputText).find()) return;

            Bundle arguments = new Bundle();
            StringBuilder result = new StringBuilder();
            result.append(inputText.subSequence(0, start)).append(text);
            arguments.putCharSequence(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, result);
            source.performAction(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, arguments);
        }

        @Override
        public void onPreferencesChanged() {
            queryPatternPrefix = Globals.getUserPreferences().getLookupSymbol();
            queryPattern = Pattern.compile(Pattern.quote(queryPatternPrefix) + QUERY_PATTERN_SUFFIX);
        }
    }
}
