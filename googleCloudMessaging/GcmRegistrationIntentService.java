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

package com.slickup.android.googleCloudMessaging;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.slickup.android.R;
import com.slickup.android.dataProviders.SubscribedChannelProvider;
import com.slickup.android.managers.NetworkManager;
import com.slickup.android.managers.UserDataManager;
import com.slickup.android.model.newApiModels.Event;
import com.slickup.android.model.newApiModels.request.GcmServerRegistrationRequest;
import com.slickup.android.model.newApiModels.response.GeneralResponse;
import com.slickup.android.model.newApiModels.response.UserLoginResponse;
import com.slickup.android.ui.mainview.MainActivity;

import java.io.IOException;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class GcmRegistrationIntentService extends IntentService implements SubscribedChannelProvider.Listener {

    private static final String TAG = "RegIntentService";
    private String token;
    private int counter = 0;
    private final int FINAL_COUNT = 2;

    public GcmRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            String savedToken = UserDataManager.getInstance().getGcmToken();
            if (!token.equals(savedToken)) {
                UserDataManager.getInstance().setGcmToken(token);
                registerGcmTokenWithServer();
            } else checkIfComplete();
            SubscribedChannelProvider.getInstance().getChannelsToSubscribeTo(this);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean(GcmPreferenceKeys.SENT_TOKEN_TO_SERVER, true).apply();
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(GcmPreferenceKeys.SENT_TOKEN_TO_SERVER, false).apply();
        }
    }

    private void subscribeTopicsToGcm(String gcmToken, Integer channelId) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        pubSub.subscribe(gcmToken, "/topics/" + channelId, null);
    }

    @Override
    public void channelsToSubscribeTo(List<Integer> channelIds) {
        for (Integer channelId : channelIds)
            try {
                subscribeTopicsToGcm(token, channelId);
            } catch (IOException e) {
            }
        checkIfComplete();
    }

    private void registerGcmTokenWithServer() {
        GcmServerRegistrationRequest registrationRequest = new GcmServerRegistrationRequest();
        registrationRequest.setAndroid_device(UserDataManager.getInstance().getGcmToken());
        registrationRequest.setToken(UserDataManager.getInstance().getUserAuthToken());
        Call<GeneralResponse> gcmRegistrationCall = NetworkManager.getApiInterface().registerDeviceForGCM(registrationRequest);
        gcmRegistrationCall.enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Response<GeneralResponse> response, Retrofit retrofit) {
                checkIfComplete();
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }

    private void checkIfComplete() {
        counter++;
        if (counter == FINAL_COUNT) {
            Intent registrationComplete = new Intent(GcmPreferenceKeys.REGISTRATION_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        }
    }
}