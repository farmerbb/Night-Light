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

package com.farmerbb.nightlight.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.farmerbb.nightlight.R;
import com.farmerbb.nightlight.activity.UninstallNotificationActivity;
import com.farmerbb.nightlight.util.U;

import java.util.Random;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        U.resyncNightLight(context, null, 0);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            SharedPreferences pref = U.getSharedPreferences(context);
            if(!pref.getBoolean("uninstall_dialog_shown", false)) {
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, UninstallNotificationActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_colorize_black_24dp)
                        .setContentIntent(contentIntent)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.getString(R.string.uninstall_notification_text))
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setShowWhen(false)
                        .setOngoing(true)
                        .setAutoCancel(true);

                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(new Random().nextInt(), mBuilder.build());
            }
        }
    }
}