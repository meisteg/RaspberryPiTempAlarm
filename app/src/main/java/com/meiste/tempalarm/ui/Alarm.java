/*
 * Copyright (C) 2014-2017 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.WindowManager;

import com.meiste.tempalarm.R;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class Alarm extends AppCompatActivity {

    /** Play alarm up to 10 minutes before silencing */
    private static final long ALARM_TIMEOUT = 10 * DateUtils.MINUTE_IN_MILLIS;

    private static final long[] sVibratePattern = new long[] { 500, 500 };

    private boolean mPlaying = false;
    private Vibrator mVibrator;

    MediaPlayer mMediaPlayer;

    private static final int KILLER = 1000;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(final Message msg) {
            switch (msg.what) {
                case KILLER:
                    Timber.i("Alarm killer triggered");
                    finish();
                    return true;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.i("Showing alarm");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.alarm);
        ButterKnife.bind(this);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0 (typically because ringer mode is silent).
        if ((am != null) && (am.getStreamVolume(AudioManager.STREAM_ALARM) != 0)) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final AudioAttributes attr = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();
                mMediaPlayer.setAudioAttributes(attr);
            } else {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }
            try {
                mMediaPlayer.setDataSource(this, alert);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (final IOException e) {
                Timber.e("Failed to play alarm tone: %s", e.toString());
            }
        }

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(VibrationEffect.createWaveform(sVibratePattern, 0));
            } else {
                mVibrator.vibrate(sVibratePattern, 0);
            }
        }

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
            if (mVibrator != null) {
                mVibrator.cancel();
            }
        }
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                dismiss();
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
