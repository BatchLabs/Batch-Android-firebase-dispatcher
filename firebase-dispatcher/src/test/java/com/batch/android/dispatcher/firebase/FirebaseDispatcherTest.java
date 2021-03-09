package com.batch.android.dispatcher.firebase;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.batch.android.Batch;
import com.batch.android.BatchMessage;
import com.batch.android.BatchPushPayload;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

/**
 * Test the Firebase Event Dispatcher implementation
 * The dispatcher should respect the UTM protocol from Google tools
 * See : https://ga-dev-tools.appspot.com/campaign-url-builder/
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(FirebaseAnalytics.class)
public class FirebaseDispatcherTest
{
    @Rule
    public PowerMockRule rule = new PowerMockRule();
    private FirebaseAnalytics firebase;
    private FirebaseDispatcher firebaseDispatcher;

    @Before
    public void setUp() {
        Context context = PowerMockito.mock(Context.class);
        firebase = PowerMockito.mock(FirebaseAnalytics.class);

        PowerMockito.mockStatic(FirebaseAnalytics.class);
        Mockito.when(FirebaseAnalytics.getInstance(context)).thenReturn(firebase);

        firebaseDispatcher = new FirebaseDispatcher(context);
    }

    @Test
    public void testNotificationNoData() {

        TestEventPayload payload = new TestEventPayload(null,
                null,
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "push");
        expected.putString("source", "batch");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_DISPLAY, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_notification_display"), bundleEq(expected));
    }

    @Test
    public void testNotificationDeeplinkQueryVars() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_source=batchsdk&utm_medium=push-batch&utm_campaign=yoloswag&utm_content=button1",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "push-batch");
        expected.putString("source", "batchsdk");
        expected.putString("campaign", "yoloswag");
        expected.putString("content", "button1");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_DISPLAY, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_notification_display"), bundleEq(expected));
    }

    @Test
    public void testNotificationDeeplinkQueryVarsEncode() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_source=%5Bbatchsdk%5D&utm_medium=push-batch&utm_campaign=yoloswag&utm_content=button1",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "push-batch");
        expected.putString("source", "[batchsdk]");
        expected.putString("campaign", "yoloswag");
        expected.putString("content", "button1");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_DISPLAY, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_notification_display"), bundleEq(expected));
    }

    @Test
    public void testNotificationDeeplinkFragmentVars() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com#utm_source=batch-sdk&utm_medium=pushbatch01&utm_campaign=154879548754&utm_content=notif001",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "pushbatch01");
        expected.putString("source", "batch-sdk");
        expected.putString("campaign", "154879548754");
        expected.putString("content", "notif001");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_OPEN, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_notification_open"), bundleEq(expected));
    }

    @Test
    public void testNotificationDeeplinkFragmentVarsEncode() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com/test#utm_source=%5Bbatch-sdk%5D&utm_medium=pushbatch01&utm_campaign=154879548754&utm_content=notif001",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "pushbatch01");
        expected.putString("source", "[batch-sdk]");
        expected.putString("campaign", "154879548754");
        expected.putString("content", "notif001");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_OPEN, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_notification_open"), bundleEq(expected));
    }

    @Test
    public void testNotificationCustomPayload() {

        Bundle customPayload = new Bundle();
        customPayload.putString("utm_medium", "654987");
        customPayload.putString("utm_source", "jesuisuntest");
        customPayload.putString("utm_campaign", "heinhein");
        customPayload.putString("utm_content", "allo118218");
        TestEventPayload payload = new TestEventPayload(null,
                null,
                customPayload);

        Bundle expected = new Bundle();
        expected.putString("medium", "654987");
        expected.putString("source", "jesuisuntest");
        expected.putString("campaign", "heinhein");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_DISPLAY, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_notification_display"), bundleEq(expected));
    }

    @Test
    public void testNotificationDeeplinkPriority() {
        // priority: Custom Payload > Query vars > Fragment vars
        Bundle customPayload = new Bundle();
        customPayload.putString("utm_medium", "654987");
        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_source=batchsdk&utm_campaign=yoloswag#utm_source=batch-sdk&utm_medium=pushbatch01&utm_campaign=154879548754&utm_content=notif001",
                customPayload);

        Bundle expected = new Bundle();
        expected.putString("medium", "654987");
        expected.putString("source", "batchsdk");
        expected.putString("campaign", "yoloswag");
        expected.putString("content", "notif001");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_OPEN, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_notification_open"), bundleEq(expected));
    }

    @Test
    public void testNotificationDeeplinkNonTrimmed() {
        Bundle customPayload = new Bundle();
        TestEventPayload payload = new TestEventPayload(null,
                "   \n     https://batch.com?utm_source=batchsdk&utm_campaign=yoloswag     \n ",
                customPayload);

        Bundle expected = new Bundle();
        expected.putString("medium", "push");
        expected.putString("source", "batchsdk");
        expected.putString("campaign", "yoloswag");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_OPEN, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_notification_open"), bundleEq(expected));
    }

    @Test
    public void testNotificationDismissCampaign() {

        Bundle customPayload = new Bundle();
        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_campaign=yoloswag",
                customPayload);

        Bundle expected = new Bundle();
        expected.putString("medium", "push");
        expected.putString("source", "batch");
        expected.putString("campaign", "yoloswag");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_DISMISS, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_notification_dismiss"), bundleEq(expected));
    }

    @Test
    public void testInAppNoData() {

        TestEventPayload payload = new TestEventPayload(null,
                null,
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", null);
        expected.putString("batch_tracking_id", null);

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.MESSAGING_SHOW, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_in_app_show"), bundleEq(expected));
    }

    @Test
    public void testInAppShowUppercaseQueryVars() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?uTm_ConTENT=jesuisuncontent",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("batch_tracking_id", null);
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", null);
        expected.putString("content", "jesuisuncontent");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.MESSAGING_SHOW, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_in_app_show"), bundleEq(expected));
    }

    @Test
    public void testInAppShowUppercaseFragmentVars() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com#UtM_CoNtEnT=jesuisuncontent",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("batch_tracking_id", null);
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", null);
        expected.putString("content", "jesuisuncontent");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.MESSAGING_SHOW, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_in_app_show"), bundleEq(expected));
    }

    @Test
    public void testInAppTrackingId() {

        TestEventPayload payload = new TestEventPayload("jesuisunid",
                null,
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", "jesuisunid");
        expected.putString("batch_tracking_id", "jesuisunid");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.MESSAGING_CLICK, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_in_app_click"), bundleEq(expected));
    }

    @Test
    public void testInAppDeeplinkContentQueryVars() {

        TestEventPayload payload = new TestEventPayload("jesuisunid",
                "https://batch.com?utm_content=jesuisuncontent",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", "jesuisunid");
        expected.putString("batch_tracking_id", "jesuisunid");
        expected.putString("content", "jesuisuncontent");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.MESSAGING_CLOSE_ERROR, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_in_app_close_error"), bundleEq(expected));
    }

    @Test
    public void testInAppDeeplinkFragmentQueryVars() {

        TestEventPayload payload = new TestEventPayload("jesuisunid",
                "https://batch.com#utm_content=jesuisuncontent00587",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", "jesuisunid");
        expected.putString("batch_tracking_id", "jesuisunid");
        expected.putString("content", "jesuisuncontent00587");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.MESSAGING_SHOW, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_in_app_show"), bundleEq(expected));
    }

    @Test
    public void testInAppDeeplinkContentPriority() {

        TestEventPayload payload = new TestEventPayload("jesuisunid",
                "https://batch.com?utm_content=jesuisuncontent002#utm_content=jesuisuncontent015",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", "jesuisunid");
        expected.putString("batch_tracking_id", "jesuisunid");
        expected.putString("content", "jesuisuncontent002");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.MESSAGING_AUTO_CLOSE, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_in_app_auto_close"), bundleEq(expected));
    }

    @Test
    public void testInAppWebView() {

        TestEventPayload payload = new TestEventPayload(null,
                "jesuisunbouton",
                null,
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", null);
        expected.putString("batch_tracking_id", null);
        expected.putString("batch_webview_analytics_id", "jesuisunbouton");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.MESSAGING_WEBVIEW_CLICK, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_in_app_webview_click"), bundleEq(expected));
    }

    @Test
    public void testInAppDeeplinkContentNoId() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_content=jesuisuncontent",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", null);
        expected.putString("batch_tracking_id", null);
        expected.putString("content", "jesuisuncontent");

        firebaseDispatcher.dispatchEvent(Batch.EventDispatcher.Type.MESSAGING_CLICK, payload);
        Mockito.verify(firebase).logEvent(Mockito.eq("batch_in_app_click"), bundleEq(expected));
    }

    private static class TestEventPayload implements Batch.EventDispatcher.Payload {

        private String trackingId;
        private String deeplink;
        private String webViewAnalyticsID;
        private Bundle customPayload;

        TestEventPayload(String trackingId,
                         String deeplink,
                         Bundle customPayload)
        {
            this(trackingId, null, deeplink, customPayload);
        }

        TestEventPayload(String trackingId,
                         String webViewAnalyticsID,
                         String deeplink,
                         Bundle customPayload)
        {
            this.trackingId = trackingId;
            this.webViewAnalyticsID = webViewAnalyticsID;
            this.deeplink = deeplink;
            this.customPayload = customPayload;
        }

        @Nullable
        @Override
        public String getTrackingId()
        {
            return trackingId;
        }

        @Nullable
        @Override
        public String getWebViewAnalyticsID() {
            return webViewAnalyticsID;
        }

        @Nullable
        @Override
        public String getDeeplink()
        {
            return deeplink;
        }

        @Nullable
        @Override
        public String getCustomValue(@NonNull String key)
        {
            if (customPayload == null) {
                return null;
            }
            return customPayload.getString(key);
        }

        @Override
        public boolean isPositiveAction() {
            return false;
        }

        @Nullable
        @Override
        public BatchMessage getMessagingPayload()
        {
            return null;
        }

        @Nullable
        @Override
        public BatchPushPayload getPushPayload()
        {
            return null;
        }
    }

    public static Bundle bundleEq(Bundle expected) {
        return Mockito.argThat(new BundleObjectMatcher(expected));
    }

    private static class BundleObjectMatcher implements ArgumentMatcher<Bundle>
    {
        Bundle expected;

        private BundleObjectMatcher(Bundle expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Bundle bundle) {
            return equalBundles(bundle, expected);
        }

        private boolean equalBundles(Bundle one, Bundle two) {
            if (one.size() != two.size()) {
                return false;
            }

            Set<String> setOne = new HashSet<>(one.keySet());
            setOne.addAll(two.keySet());
            Object valueOne;
            Object valueTwo;

            for (String key : setOne) {
                if (!one.containsKey(key) || !two.containsKey(key)) {
                    return false;
                }

                valueOne = one.get(key);
                valueTwo = two.get(key);
                if (valueOne instanceof Bundle && valueTwo instanceof Bundle &&
                        !equalBundles((Bundle) valueOne, (Bundle) valueTwo)) {
                    return false;
                } else if (valueOne == null) {
                    if (valueTwo != null) {
                        return false;
                    }
                } else if (!valueOne.equals(valueTwo)) {
                    return false;
                }
            }
            return true;
        }
    }
}
