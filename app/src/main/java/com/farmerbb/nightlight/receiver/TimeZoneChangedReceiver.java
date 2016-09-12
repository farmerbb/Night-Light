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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.farmerbb.nightlight.util.U;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.util.TimeZone;

public class TimeZoneChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = U.getSharedPreferences(context);
        String oldTimeZone = pref.getString("time_zone", TimeZone.getDefault().getID());
        String newTimeZone = intent.getStringExtra("time-zone");

        if(!oldTimeZone.equals(newTimeZone)) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("time_zone", newTimeZone);

            DateTime utc = new DateTime(DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
            LocalDateTime oldTime = utc.withZone(DateTimeZone.forID(oldTimeZone)).toLocalDateTime();
            LocalDateTime newTime = utc.withZone(DateTimeZone.forID(newTimeZone)).toLocalDateTime();

            long offset = newTime.toDate().getTime() - oldTime.toDate().getTime();
            if(offset != 0 && pref.contains("next_start_time") && pref.contains("next_end_time")) {
                long startTime = pref.getLong("next_start_time", -1);
                long endTime = pref.getLong("next_end_time", -1);

                editor.putLong("next_start_time", startTime + offset);
                editor.putLong("next_end_time", endTime + offset);
            }

            editor.apply();

            U.resyncNightLight(context, newTime, (int) offset);
        }
    }
}