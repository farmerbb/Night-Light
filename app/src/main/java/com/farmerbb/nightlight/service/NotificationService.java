/* Copyright 2016 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.nightlight.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.farmerbb.nightlight.R;
import com.farmerbb.nightlight.activity.MainActivity;
import com.farmerbb.nightlight.util.Blacklist;
import com.farmerbb.nightlight.util.U;

public class NotificationService extends Service {

    private Thread thread;

    private boolean shouldRefreshRecents = true;
    private boolean isRefreshingRecents = false;

    private int refreshInterval = 2000;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent receiverIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.farmerbb.nightlight.SNOOZE"), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_colorize_black_24dp)
                .setContentIntent(contentIntent)
                .setContentTitle(getString(R.string.night_light_is_active))
                .setContentText(getString(R.string.click_to_open_settings))
                .addAction(0, getString(R.string.pause_30_min), receiverIntent)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setPriority(Notification.PRIORITY_MIN)
                .setShowWhen(false)
                .setOngoing(true);

        startForeground(8675309, mBuilder.build());
        startRefreshingRecents();
    }

    private void startRefreshingRecents() {
        if(thread != null) thread.interrupt();

        thread = new Thread() {
            @Override
            public void run() {
                if(!isRefreshingRecents) {
                    isRefreshingRecents = true;

                    while(shouldRefreshRecents) {
                        updateRecentApps();
                        SystemClock.sleep(refreshInterval);
                    }

                    isRefreshingRecents = false;
                }
            }
        };

        thread.start();
    }

    private void updateRecentApps() {
        Blacklist blacklist = Blacklist.getInstance(this);
        String currentForegroundApp = null;

        if(!U.isNightModeOn(this))
            stopSelf();
        else if(Settings.Secure.getInt(getContentResolver(), "accessibility_display_inversion_enabled", 0) == 1)
            U.setTint(this, false);
        else {
            if(blacklist.getBlockedApps().size() > 0) {
                UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                UsageEvents events = mUsageStatsManager.queryEvents(
                        System.currentTimeMillis() - AlarmManager.INTERVAL_DAY,
                        System.currentTimeMillis());

                UsageEvents.Event eventCache = new UsageEvents.Event();

                while(events.hasNextEvent()) {
                    events.getNextEvent(eventCache);

                    if(eventCache.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND)
                        currentForegroundApp = eventCache.getPackageName();
                }
            }


            U.setTint(this, !blacklist.isBlocked(currentForegroundApp));
        }
    }

    @Override
    public void onDestroy() {
        shouldRefreshRecents = false;

        super.onDestroy();
    }
}