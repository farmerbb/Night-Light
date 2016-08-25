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

import java.util.Locale;

public class SnoozeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        U.setNightMode(context, false);

        SharedPreferences pref = U.getSharedPreferences(context);
        String[] endTime = pref.getString("end_time", "06:00").split(":");

        DateTime snoozeTime = DateTime.now().plusMinutes(30);
        DateTime endTimeCalendar = DateTime.now()
                .withHourOfDay(Integer.parseInt(endTime[0]))
                .withMinuteOfHour(Integer.parseInt(endTime[1]))
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        if(endTimeCalendar.isBeforeNow())
            endTimeCalendar = endTimeCalendar.plusDays(1);

        if(snoozeTime.isBefore(endTimeCalendar)) {
            pref.edit().putBoolean("is_snoozed", true).apply();

            U.registerNextReceiver(context, "start_time",
                    String.format(Locale.US, "%02d", snoozeTime.getHourOfDay())
                            + ":" + String.format(Locale.US, "%02d", snoozeTime.getMinuteOfHour()));
        } else
            U.registerNextReceiver(context);
    }
}