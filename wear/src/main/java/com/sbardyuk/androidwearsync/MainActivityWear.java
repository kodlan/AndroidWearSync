package com.sbardyuk.androidwearsync;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import static com.sbardyuk.androidwearsync.NumberActivityWear.NUMBER_PARAM;

public class MainActivityWear extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    private static final String TAG = MainActivityWear.class.getSimpleName();
    private static final String NUMBER_CAPABILITY_NAME = "number_capability";
    private static final String REQUEST_NUMBER_MESSAGE_PATH = "/android_wear_sync_mobile_request_number";
    private static final String RESPONSE_NUMBER_MESSAGE_PATH = "/android_wear_sync_mobile_response_number";

    private BoxInsetLayout containerView;
    private TextView textView;
    private DelayedConfirmationView continueButton;

    private GoogleApiClient googleApiClient;
    private boolean nodeConnected = false;
    private String mobileNodeId = null;

    private Handler handler = new Handler();
    private boolean syncCancelled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wear);
        setAmbientEnabled();

        containerView = (BoxInsetLayout) findViewById(R.id.container);
        textView = (TextView) findViewById(R.id.label);
        continueButton = (DelayedConfirmationView) findViewById(R.id.continue_button);

        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(googleApiClient, this);
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            containerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            textView.setTextColor(getResources().getColor(android.R.color.white));
            textView.getPaint().setAntiAlias(false);
            continueButton.setVisibility(View.INVISIBLE);
        } else {
            containerView.setBackground(null);
            textView.setTextColor(getResources().getColor(android.R.color.black));
            textView.getPaint().setAntiAlias(true);
            continueButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected to Google Api Service");
        Wearable.MessageApi.addListener(googleApiClient, this);
        nodeConnected = true;
        communicateWithMobileBackground();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
        nodeConnected = false;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        nodeConnected = false;
        postSyncFailedMessage();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(RESPONSE_NUMBER_MESSAGE_PATH)) {
            // mobile app responded with number completed. message length should be 1
            if (messageEvent.getData() != null && messageEvent.getData().length == 1) {
                int number = messageEvent.getData()[0];
                Log.d(TAG, "number received = " + number);
                handleNumberReceived(number);
            } else {
                handleSyncFailed();
            }
        }
    }

    private void communicateWithMobileBackground() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (nodeConnected && googleApiClient != null && googleApiClient.isConnected()) {
                    setupCapability();
                    sendRequestForNumber();
                } else {
                    handleSyncFailed();
                }
            }
        }).start();
    }

    private void setupCapability() {
        Log.d(TAG, "setting up capabilities");
        CapabilityApi.GetCapabilityResult result = Wearable.CapabilityApi.getCapability(googleApiClient, NUMBER_CAPABILITY_NAME, CapabilityApi.FILTER_REACHABLE).await();
        updateNumberCapability(result.getCapability());

        CapabilityApi.CapabilityListener capabilityListener = new CapabilityApi.CapabilityListener() {
            @Override
            public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                updateNumberCapability(capabilityInfo);
            }
        };

        Wearable.CapabilityApi.addCapabilityListener(googleApiClient, capabilityListener, NUMBER_CAPABILITY_NAME);
    }

    private void updateNumberCapability(CapabilityInfo capabilityInfo) {
        for (Node node : capabilityInfo.getNodes()) {
            if (node.isNearby()) {
                mobileNodeId = node.getId();
            }
            mobileNodeId = node.getId();
        }
        Log.d(TAG, "node = " + mobileNodeId);
    }

    private void sendRequestForNumber() {
        if (mobileNodeId != null) {
            Log.d(TAG, "sending message " + REQUEST_NUMBER_MESSAGE_PATH);
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleApiClient, mobileNodeId, REQUEST_NUMBER_MESSAGE_PATH, new byte[1]).await();
            if (!result.getStatus().isSuccess()) {
                Log.d(TAG, "Failed to send message");
                postSyncFailedMessage();
            }
        } else {
            Log.d(TAG, "Unable to retrieve node with number capability");
            postSyncFailedMessage();
        }
    }

    private void handleSyncFailed() {
        if (!syncCancelled) {
            continueButton.setListener(null);
            startActivity(new Intent(this, SyncFailedActivityWear.class));
            this.finish();
        }
    }

    private void postSyncFailedMessage() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                handleSyncFailed();
            }
        });
    }

    private void handleNumberReceived(int number) {
        if (!syncCancelled) {
            continueButton.setListener(null);

            Intent intent = new Intent(this, NumberActivityWear.class);
            intent.putExtra(NUMBER_PARAM, number);

            startActivity(intent);
            finish();
        }
    }
}
