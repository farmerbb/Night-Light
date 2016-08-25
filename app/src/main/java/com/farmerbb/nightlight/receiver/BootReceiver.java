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

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = U.getSharedPreferences(context);
        if(pref.contains("next_start_time") && pref.contains("next_end_time")) {
            DateTime startTimeCalendar = new DateTime(pref.getLong("next_start_time", -1));
            DateTime endTimeCalendar = new DateTime(pref.getLong("next_end_time", -1));

            if(startTimeCalendar.isBeforeNow() && endTimeCalendar.isBeforeNow()) {
                while(startTimeCalendar.isBeforeNow() && endTimeCalendar.isBeforeNow()) {
                    startTimeCalendar = startTimeCalendar.plusDays(1);
                    endTimeCalendar = endTimeCalendar.plusDays(1);
                }
            }

            if(startTimeCalendar.isBeforeNow() && endTimeCalendar.isAfterNow()) {
                U.setNightMode(context, true);
            } else if(startTimeCalendar.isAfterNow() && endTimeCalendar.isBeforeNow()) {
                U.setNightMode(context, false);
            }
        }

        U.registerNextReceiver(context);
    }
}