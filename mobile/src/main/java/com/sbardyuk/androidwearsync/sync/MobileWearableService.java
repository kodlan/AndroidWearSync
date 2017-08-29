package com.sbardyuk.androidwearsync.sync;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class MobileWearableService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = MobileWearableService.class.getSimpleName() + "MOBILE";

    private static final String REQUEST_NUMBER_MESSAGE_PATH = "/android_wear_sync_mobile_request_number";
    private static final String RESPONSE_NUMBER_MESSAGE_PATH = "/android_wear_sync_mobile_response_number";

    private GoogleApiClient googleApiClient;
    private boolean nodeConnected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        nodeConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        nodeConnected = false;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        nodeConnected = false;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(REQUEST_NUMBER_MESSAGE_PATH)) {

            int number = 12;

            sendNumberToWear(messageEvent.getSourceNodeId(), number);
        }
    }

    private void sendNumberToWear(String responseNodeId, int number) {
        byte message [] = new byte[1];
        message[0] = (byte) number;
        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleApiClient, responseNodeId, RESPONSE_NUMBER_MESSAGE_PATH, message).await();
        if (!result.getStatus().isSuccess()) {
            Log.d(TAG, "failed to send message");
        }
    }
}