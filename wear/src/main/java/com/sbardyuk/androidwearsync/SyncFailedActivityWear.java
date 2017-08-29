package com.sbardyuk.androidwearsync;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.widget.TextView;

public class SyncFailedActivityWear extends WearableActivity {

    private TextView syncFailedLabel;
    private BoxInsetLayout containerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_failed);
        setAmbientEnabled();
        containerView = (BoxInsetLayout) findViewById(R.id.containerView);
        syncFailedLabel = (TextView) findViewById(R.id.sync_failed_label);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateAmbientDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateAmbientDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateAmbientDisplay();
        super.onExitAmbient();
    }

    private void updateAmbientDisplay() {
        if (isAmbient()) {
            containerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            syncFailedLabel.setTextColor(getResources().getColor(android.R.color.white));
            syncFailedLabel.getPaint().setAntiAlias(false);
        } else {
            containerView.setBackground(null);
            syncFailedLabel.setTextColor(getResources().getColor(android.R.color.black));
            syncFailedLabel.getPaint().setAntiAlias(true);
        }
    }
}
