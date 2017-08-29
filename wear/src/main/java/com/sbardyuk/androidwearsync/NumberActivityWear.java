package com.sbardyuk.androidwearsync;


import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.widget.TextView;

public class NumberActivityWear extends WearableActivity {

    public static final String NUMBER_PARAM = "number_param";

    private TextView numberTextView;
    private BoxInsetLayout containerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_failed);
        setAmbientEnabled();
        containerView = (BoxInsetLayout) findViewById(R.id.containerView);
        numberTextView = (TextView) findViewById(R.id.number_text_views);

        int number = getIntent().getIntExtra(NUMBER_PARAM, 0);
        numberTextView.setText(Integer.toString(number));
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
            numberTextView.setTextColor(getResources().getColor(android.R.color.white));
            numberTextView.getPaint().setAntiAlias(false);
        } else {
            containerView.setBackground(null);
            numberTextView.setTextColor(getResources().getColor(android.R.color.black));
            numberTextView.getPaint().setAntiAlias(true);
        }
    }
}