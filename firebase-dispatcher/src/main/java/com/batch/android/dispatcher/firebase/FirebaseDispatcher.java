package com.batch.android.dispatcher.firebase;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.batch.android.Batch;
import com.batch.android.BatchEventDispatcher;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Firebase Event Dispatcher
 * The dispatcher should generate UTM tag from a Batch payload and send them to the Firebase SDK
 * See : https://ga-dev-tools.appspot.com/campaign-url-builder/
 */
public class FirebaseDispatcher implements BatchEventDispatcher
{
    /**
     * Firebase UTM tag keys
     */
    private static final String CAMPAIGN = "campaign";
    private static final String SOURCE = "source";
    private static final String MEDIUM = "medium";
    private static final String CONTENT = "content";

    /**
     * UTM tag keys
     */
    private static final String UTM_CAMPAIGN = "utm_campaign";
    private static final String UTM_SOURCE = "utm_source";
    private static final String UTM_MEDIUM = "utm_medium";
    private static final String UTM_CONTENT = "utm_content";

    /**
     * Key used to dispatch the Batch tracking Id on Firebase
     */
    private static final String BATCH_TRACKING_ID = "batch_tracking_id";

    /**
     * Event name used when logging on Firebase
     */
    private static final String NOTIFICATION_DISPLAY_NAME = "batch_notification_display";
    private static final String NOTIFICATION_OPEN_NAME = "batch_notification_open";
    private static final String NOTIFICATION_DISMISS_NAME = "batch_notification_dismiss";
    private static final String MESSAGING_SHOW_NAME = "batch_in_app_show";
    private static final String MESSAGING_CLOSE_NAME = "batch_in_app_close";
    private static final String MESSAGING_AUTO_CLOSE_NAME = "batch_in_app_auto_close";
    private static final String MESSAGING_CLICK_NAME = "batch_in_app_click";
    private static final String UNKNOWN_EVENT_NAME = "batch_unknown";

    private FirebaseAnalytics firebaseAnalytics;

    FirebaseDispatcher(Context context)
    {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    /**
     * Callback when a new event just happened in the Batch SDK.
     *
     * @param type The type of the event
     * @param payload The payload associated with the event
     */
    @Override
    public void dispatchEvent(@NonNull Batch.EventDispatcher.Type type,
                              @NonNull Batch.EventDispatcher.Payload payload)
    {
        Bundle firebaseParams = null;
        if (type.isNotificationEvent()) {
            firebaseParams = getNotificationParams(payload);
        } else if (type.isMessagingEvent()) {
            firebaseParams = getInAppParams(payload);
        }

        firebaseAnalytics.logEvent(getFirebaseEventName(type), firebaseParams);
    }

    private static Bundle getInAppParams(Batch.EventDispatcher.Payload payload)
    {
        Bundle firebaseParams = new Bundle();
        firebaseParams.putString(CAMPAIGN, payload.getTrackingId());
        firebaseParams.putString(SOURCE, "batch");
        firebaseParams.putString(MEDIUM, "in-app");
        firebaseParams.putString(BATCH_TRACKING_ID, payload.getTrackingId());

        String deeplink = payload.getDeeplink();
        if (deeplink != null) {
            deeplink = deeplink.trim();
            Uri uri = Uri.parse(deeplink);

            String fragment = uri.getFragment();
            if (fragment != null && !fragment.isEmpty()) {
                Map<String, String> fragments = getFragmentMap(fragment);
                // Copy from fragment part of the deeplink
                copyValueFromMap(fragments, UTM_CONTENT, firebaseParams, CONTENT);
            }
            // Copy from query parameters of the deeplink
            copyValueFromQuery(uri, UTM_CONTENT, firebaseParams, CONTENT);
        }
        // Load from custom payload
        copyValueFromPayload(payload, UTM_CAMPAIGN, firebaseParams, CAMPAIGN);
        copyValueFromPayload(payload, UTM_MEDIUM, firebaseParams, MEDIUM);
        copyValueFromPayload(payload, UTM_SOURCE, firebaseParams, SOURCE);
        return firebaseParams;
    }

    private static Bundle getNotificationParams(Batch.EventDispatcher.Payload payload)
    {
        Bundle firebaseParams = new Bundle();
        firebaseParams.putString(SOURCE, "batch");
        firebaseParams.putString(MEDIUM, "push");

        String deeplink = payload.getDeeplink();
        if (deeplink != null) {
            deeplink = deeplink.trim();
            Uri uri = Uri.parse(deeplink);

            String fragment = uri.getFragment();
            if (fragment != null && !fragment.isEmpty()) {
                Map<String, String> fragments = getFragmentMap(fragment);
                // Copy from fragment part of the deeplink
                copyValueFromMap(fragments, UTM_CAMPAIGN, firebaseParams, CAMPAIGN);
                copyValueFromMap(fragments, UTM_MEDIUM, firebaseParams, MEDIUM);
                copyValueFromMap(fragments, UTM_SOURCE, firebaseParams, SOURCE);
                copyValueFromMap(fragments, UTM_CONTENT, firebaseParams, CONTENT);
            }

            // Copy from query parameters of the deeplink
            copyValueFromQuery(uri, UTM_CAMPAIGN, firebaseParams, CAMPAIGN);
            copyValueFromQuery(uri, UTM_MEDIUM, firebaseParams, MEDIUM);
            copyValueFromQuery(uri, UTM_SOURCE, firebaseParams, SOURCE);
            copyValueFromQuery(uri, UTM_CONTENT, firebaseParams, CONTENT);
        }
        // Load from custom payload
        copyValueFromPayload(payload, UTM_CAMPAIGN, firebaseParams, CAMPAIGN);
        copyValueFromPayload(payload, UTM_MEDIUM, firebaseParams, MEDIUM);
        copyValueFromPayload(payload, UTM_SOURCE, firebaseParams, SOURCE);
        return firebaseParams;
    }

    private static Map<String, String> getFragmentMap(String fragment)
    {
        String[] params = fragment.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String[] parts = param.split("=");
            if (parts.length >= 2) {
                map.put(parts[0].toLowerCase(), parts[1]);
            }
        }
        return map;
    }

    private static void copyValueFromMap(Map<String, String> map,
                                         String keyFrom,
                                         Bundle bundle,
                                         String keyOut)
    {
        String value = map.get(keyFrom);
        if (value != null) {
            bundle.putString(keyOut, value);
        }
    }

    private static void copyValueFromQuery(Uri uri, String keyFrom, Bundle bundle, String keyOut)
    {
        Set<String> keys = uri.getQueryParameterNames();
        for (String key : keys) {
            if (keyFrom.equalsIgnoreCase(key)) {
                String value = uri.getQueryParameter(key);
                if (value != null) {
                    bundle.putString(keyOut, value);
                    return;
                }
            }
        }
    }

    private static void copyValueFromPayload(Batch.EventDispatcher.Payload payload,
                                             String keyFrom,
                                             Bundle bundle,
                                             String keyOut)
    {
        String value = payload.getCustomValue(keyFrom);
        if (value != null) {
            bundle.putString(keyOut, value);
        }
    }

    private static String getFirebaseEventName(Batch.EventDispatcher.Type type) {
        switch (type) {
            case NOTIFICATION_DISPLAY:
                return NOTIFICATION_DISPLAY_NAME;
            case NOTIFICATION_OPEN:
                return NOTIFICATION_OPEN_NAME;
            case NOTIFICATION_DISMISS:
                return NOTIFICATION_DISMISS_NAME;
            case MESSAGING_SHOW:
                return MESSAGING_SHOW_NAME;
            case MESSAGING_CLOSE:
                return MESSAGING_CLOSE_NAME;
            case MESSAGING_AUTO_CLOSE:
                return MESSAGING_AUTO_CLOSE_NAME;
            case MESSAGING_CLICK:
                return MESSAGING_CLICK_NAME;
        }
        return UNKNOWN_EVENT_NAME;
    }
}
