/*
 * Copyright (C) 2014 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meiste.tempalarm.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.R;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

public class Alarm extends ActionBarActivity {

    /** Play alarm up to 10 minutes before silencing */
    private static final long ALARM_TIMEOUT = 10 * DateUtils.MINUTE_IN_MILLIS;

    private static final long[] sVibratePattern = new long[] { 500, 500 };

    private boolean mPlaying = false;
    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;

    @InjectView(R.id.alert_msg)
    protected TextView mAlertMsg;

    private static final int KILLER = 1000;
    private Handler mHandler = new Handler() {
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case KILLER:
                    Timber.i("Alarm killer triggered");
                    finish();
                    break;
            }
        }
    };

    private BroadcastReceiver mKillReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Timber.d("Stopping alarm due to kill intent");
            finish();
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.alarm);
        ButterKnife.inject(this);

        final Bundle extras = getIntent().getExtras();
        if ((extras != null) && (extras.containsKey(AppConstants.INTENT_EXTRA_ALERT_MSG))) {
            final String msg = getString(extras.getInt(AppConstants.INTENT_EXTRA_ALERT_MSG, 0));
            Timber.i("Showing alarm: %s", msg);
            mAlertMsg.setText(msg);
        }

        getSupportActionBar().setElevation(0);

        LocalBroadcastManager.getInstance(this).registerReceiver(mKillReceiver,
                new IntentFilter(AppConstants.INTENT_ACTION_KILL_ALARM));
    }

    @Override
    protected void onResume() {
        super.onResume();

        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0 (typically because ringer mode is silent).
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            final Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(final MediaPlayer mp, final int what, final int extra) {
                    Timber.e("Error occurred while playing audio.");
                    mp.stop();
                    mp.reset();
                    mp.release();
                    mMediaPlayer = null;
                    return true;
                }
            });
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            try {
                mMediaPlayer.setDataSource(this, alert);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (final IOException e) {
                Timber.e("Failed to play alarm tone: %s", e);
            }
        }

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mVibrator.vibrate(sVibratePattern, 0);

        mPlaying = true;
        mHandler.sendEmptyMessageDelayed(KILLER, ALARM_TIMEOUT);
    }

    @Override
    protected void onPause() {
        mHandler.removeMessages(KILLER);
        if (mPlaying) {
            mPlaying = false;

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            // Stop vibrator
            mVibrator.cancel();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mKillReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(NavUtils.getParentActivityIntent(this))
                        .startActivities();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.alert_button)
    public void dismiss() {
        TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(NavUtils.getParentActivityIntent(this))
                .startActivities();
    }
}