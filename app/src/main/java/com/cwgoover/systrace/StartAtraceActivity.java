
package com.cwgoover.systrace;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.cwgoover.systrace.toolbox.FileUtil;

public class StartAtraceActivity extends Activity
            implements OnClickListener, AtraceFloatView.Callback {

    public static final String TAG = "jrdSystrace";
    public static final String SYSTRACE_SERVICE = "com.cwgoover.systrace.AtraceFloatView";

    public static final String TIME_INTERVAL = "time";
    public static final String ICON_SHOW = "iconShow";
    public static final String MENU_SHOW_DIALOG = "showDialog";

    Button mStartBtn;
    Button mStopBtn;
    EditText mTimeInterval;

    FileUtil mUtil;
    AtraceFloatView.UIBinder mBinder;
    AtraceFloatView.Callback mCallback;

    boolean mIsBindService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_systrace);

        mCallback = this;
        mUtil = FileUtil.getInstance();

        mStartBtn = (Button) findViewById(R.id.start);
        mStopBtn = (Button) findViewById(R.id.stop);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mTimeInterval = (EditText) findViewById(R.id.interval);

        String savedTime = mUtil.getTimeInterval(this, TIME_INTERVAL);
        if (savedTime.isEmpty() || savedTime.equals("0")) {
            mUtil.setTimeInterval(this, TIME_INTERVAL, mTimeInterval.getText().toString());
            FileUtil.myLogger(TAG, "default setTimeInterval " + mTimeInterval.getText().toString());
        }
        else {
            mTimeInterval.setText(savedTime);
        }
        // place cursor at the end of text in EditText
        mTimeInterval.setSelection(mTimeInterval.getText().length());

        mTimeInterval.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String time = mTimeInterval.getText().toString();
                FileUtil.myLogger(TAG, "EditText change, the time=" + time);
                if (time.isEmpty() || time.equals("")) {
                    // Instead of try/catch for parseInt method.
                    mUtil.setTimeInterval(getApplicationContext(), TIME_INTERVAL, time);
                } else if(Integer.parseInt(time) <= 30) {
                    // The maximum time is 30 seconds
                    mUtil.setTimeInterval(getApplicationContext(), TIME_INTERVAL, time);
                } else {
                    // shake EditText & clear numbers & show toast
                    mTimeInterval.setText("");
                    shakeEditText();
                    Toast.makeText(getApplicationContext(), "Wrong number!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, AtraceFloatView.class);
        // flag : BIND_AUTO_CREATE (it will bind the service and start the service)
        // flag : 0 (method will return true and will not start service until a call
        //        like startService(Intent) is made to start the service)
        if (!mIsBindService) {
            mIsBindService = true;
            bindService(intent, connection, 0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mIsBindService) {
            mIsBindService = false;
            unbindService(connection);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // config show dialog menu
        menu.findItem(R.id.action_show_dialog).setChecked(mUtil.getBooleanState(this, MENU_SHOW_DIALOG, false));

        // config Spinner menu
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                        this, R.array.icon_actions, android.R.layout.simple_list_item_1);
        MenuItem spinnerItem = menu.findItem(R.id.menu_spinner);
        View view = spinnerItem.getActionView();
        if (view instanceof Spinner) {
            final Spinner spinner = (Spinner) view;
            spinner.setAdapter(adapter);
            // change spinner's default value
            if (!mUtil.getBooleanState(this, ICON_SHOW, true)) {
                spinner.setSelection(1);
            }
            spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                        int position, long id) {
                    switch (parent.getId()) {
                        case R.id.menu_spinner:
                            switch (position) {
                                case 0:
                                    mUtil.setBooleanState(getApplicationContext(), ICON_SHOW, true);
                                    break;
                                case 1:
                                    mUtil.setBooleanState(getApplicationContext(),ICON_SHOW, false);
                                    break;
                            }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_dialog:
                mUtil.setBooleanState(this, MENU_SHOW_DIALOG, !item.isChecked());
                item.setChecked(!item.isChecked());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mStartBtn) {
            Intent intent = new Intent(this, AtraceFloatView.class);
            startService(intent);
        }
        else if (v == mStopBtn) {
            Intent intent = new Intent(this, AtraceFloatView.class);
            if (mIsBindService) {
                FileUtil.myLogger(TAG, "click stop button, and unbindService");
                mIsBindService = false;
                unbindService(connection);
            }
            stopService(intent);
        }
    }

    @Override
    public void notifyChange(boolean changed) {
        FileUtil.myLogger(TAG, "notifyChange: changed=" + changed);
        updateUI(changed);
    }


    private void updateUI (boolean enable) {
        mStartBtn.setEnabled(enable);
        mStopBtn.setEnabled(enable);
        mTimeInterval.setEnabled(enable);
    }

    private void shakeEditText() {
        Animation animationShake = AnimationUtils.loadAnimation(this, R.anim.shake);
        mTimeInterval.startAnimation(animationShake);
    }

    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (AtraceFloatView.UIBinder) service;
            FileUtil.myLogger(TAG, "onServiceConnected: create mBinder");
            mBinder.setCallback(mCallback);
            updateUI(mBinder.getUIState());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };
}
