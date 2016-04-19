package com.slickup.android.googleCloudMessaging;

/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.LauncherActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.slickup.android.R;
import com.slickup.android.managers.UserDataManager;
import com.slickup.android.notifications.ChatEvent;
import com.slickup.android.ui.ActivityLifeCycleAdapter;
import com.slickup.android.ui.NotificationsActivity;
import com.slickup.android.ui.event.EventActivity;
import com.slickup.android.ui.mainview.MainActivity;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class GcmNotificationListenerService extends GcmListenerService {

    private static final String TAG = "GCM";
    public static final String NOTIFICATION_CODE = "notification_code";
    public static final String FRIEND_ID = "friend_id";
    public static final String FRIEND_USERNAME = "friend_username";
    public static final String EVENT_ID = "event_id";
    public static final String PROFILE_ID = "profile_id";

    public static final int FRIEND_REQ = 340;
    public static final int FRIEND_REQ_ACCEPTED = 341;
    public static final int SUGGESTION = 342;


    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.i("NOTIFICATION:",data+" from :"+from);
        if (from.startsWith("/topics/")) {
            String channelId = from.substring(8);
            handleTopicNotifications(data, channelId);
        } else {
            handleNormalNotifications(data);
        }
    }

    private void handleTopicNotifications(Bundle data, String channelId) {
        if (data.getString("username").equals(UserDataManager.getInstance().getUsername()))
            return;
        if (ActivityLifeCycleAdapter.isApplicationInForeground()) {
            ChatEvent chatEvent = new ChatEvent();
            chatEvent.setData(data, channelId);
            EventBus.getDefault().post(chatEvent);
        } else {
            String message = data.getString("username") + "has sent a message";
            Intent intent = new Intent(this, LauncherActivity.class);
            intent.putExtra(EventActivity.EVENTIDKEY, Integer.parseInt(channelId));
            sendNotification(intent,message);
        }
    }

    private void handleNormalNotifications(Bundle data) {
        if (ActivityLifeCycleAdapter.isApplicationInForeground()) {
            //TODO: IN APP NOTIFICATION
        } else {
            sendMobileNotification(data);
        }
    }


    private void sendMobileNotification(Bundle data) {
        String message = "";
        int notificationCode = data.getInt(NOTIFICATION_CODE);
        int friendId = data.getInt(FRIEND_ID);
        String friendUsername = data.getString(FRIEND_USERNAME);
        int eventId;
        Intent intent = new Intent();
        switch (notificationCode) {
            case FRIEND_REQ:
                message = friendUsername + " has sent you a friend request";
                intent = new Intent(this, LauncherActivity.class);
                break;
            case FRIEND_REQ_ACCEPTED:
                message = friendUsername + " has accepted your friend request";
                intent = new Intent(this, MainActivity.class);
                break;
            case SUGGESTION:
                eventId = data.getInt(EVENT_ID);
                message = friendUsername + " has made a suggestion for you";
                intent = new Intent(this, LauncherActivity.class);
                intent.putExtra(EventActivity.EVENTIDKEY, eventId);
                break;
        }

        sendNotification(intent, message);
    }

    private void sendNotification(Intent intent, String message) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, 0);

        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_logo)
                        .setContentTitle("SlickUp")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentText(message);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(100, mBuilder.build());
    }
}
