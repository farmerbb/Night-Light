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

package com.farmerbb.nightlight.util;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;

import com.farmerbb.nightlight.BuildConfig;
import com.farmerbb.nightlight.activity.TaskerConditionActivity;
import com.farmerbb.nightlight.receiver.NightLightReceiver;
import com.farmerbb.nightlight.service.NotificationService;
import com.farmerbb.nightlight.R;
import com.farmerbb.nightlight.service.QuickSettingsTileService;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.Calendar;
import java.util.TimeZone;

public class U {

    private U() {}

    private static SharedPreferences pref;

    public static SharedPreferences getSharedPreferences(Context context) {
        if(pref == null) pref = context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE);
        return pref;
    }

    public static void setNightMode(Context context, boolean nightModeOn) {
        setNightMode(context, nightModeOn ? 1 : 0);
    }

    public static void setTint(Context context, boolean tintOn) {
        try {
            if(tintOn) {
                if(Settings.Secure.getInt(context.getContentResolver(), "tuner_night_mode_adjust_tint", 1) == 0)
                    Settings.Secure.putInt(context.getContentResolver(), "tuner_night_mode_adjust_tint", 1);
            } else {
                if(Settings.Secure.getInt(context.getContentResolver(), "tuner_night_mode_adjust_tint", 1) == 1)
                    Settings.Secure.putInt(context.getContentResolver(), "tuner_night_mode_adjust_tint", 0);
            }
        } catch (SecurityException e) { /* Gracefully fail */ }
    }

    public static void setNightMode(Context context, int nightModeValue) {
        SharedPreferences pref = getSharedPreferences(context);
        pref.edit().remove("is_snoozed").apply();

        try {
            Settings.Secure.putInt(context.getContentResolver(), "tuner_night_mode_adjust_tint", 1);
            Settings.Secure.putInt(context.getContentResolver(), "twilight_mode", nightModeValue);

            Intent intent = new Intent(context, NotificationService.class);
            if(nightModeValue == 1)
                context.startService(intent);
            else
                context.stopService(intent);

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.nightlight.UPDATE_SWITCH"));

            TileService.requestListeningState(context, new ComponentName(BuildConfig.APPLICATION_ID, QuickSettingsTileService.class.getName()));

            Intent query = new Intent(com.twofortyfouram.locale.api.Intent.ACTION_REQUEST_QUERY)
                    .putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_ACTIVITY_CLASS_NAME, TaskerConditionActivity.class.getName());
            context.sendBroadcast(query);
        } catch (SecurityException e) { /* Gracefully fail */ }
    }

    public static boolean isNightModeOn(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), "twilight_mode") == 1;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    public static void refreshSunriseSunsetTime(Context context) {
        String[] latLong = getLatLong(context);

        if(latLong != null) {
            SharedPreferences pref = getSharedPreferences(context);
            com.luckycatlabs.sunrisesunset.dto.Location location = new com.luckycatlabs.sunrisesunset.dto.Location(latLong[0], latLong[1]);
            SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, pref.getString("time_zone", TimeZone.getDefault().getID()));

            SharedPreferences.Editor editor = pref.edit();
            editor.putString("start_time", calculator.getOfficialSunsetForDate(Calendar.getInstance()));
            editor.putString("end_time", calculator.getOfficialSunriseForDate(Calendar.getInstance()));

            editor.apply();
        }
    }

    private static String[] getLatLong(Context context) {
        // Try and grab the last known location
        final Location location = getLastKnownLocation(context);
        if(location != null)
            return new String[] { String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()) };
        else return null;
    }

    private static Location getLastKnownLocation(Context context) {
        int permission = PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permission == PermissionChecker.PERMISSION_GRANTED)
            return getLastKnownLocationForProvider(context, LocationManager.NETWORK_PROVIDER);
        else return null;
    }

    private static Location getLastKnownLocationForProvider(Context context, String provider) {
        LocationManager mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if(mLocationManager != null) {
            try {
                if(mLocationManager.isProviderEnabled(provider))
                    return mLocationManager.getLastKnownLocation(provider);
            } catch (SecurityException e) { /* Gracefully fail */ }
        }

        return null;
    }

    public static void registerNextReceiver(Context context) {
        registerNextReceiver(context, DateTime.now());
    }

    private static void registerNextReceiver(Context context, DateTime now) {
        SharedPreferences pref = getSharedPreferences(context);
        if(pref.getString("turn_on_automatically", "never").equals("sunset_to_sunrise"))
            refreshSunriseSunsetTime(context);

        registerNextReceiver(context, null, null, now);
    }

    public static void registerNextReceiver(Context context, String key, String value) {
        registerNextReceiver(context, key, value, DateTime.now());
    }

    private static void registerNextReceiver(Context context, String key, String value, DateTime now) {
        Intent broadcastIntent = new Intent(context, NightLightReceiver.class);
        long nextStartTime = -1;
        long nextEndTime = -1;

        SharedPreferences pref = getSharedPreferences(context);
        String autoMode;

        if(key != null && key.equals("turn_on_automatically"))
            autoMode = value;
        else
            autoMode = pref.getString("turn_on_automatically", "never");

        if(!autoMode.equals("never") || pref.getBoolean("is_snoozed", false)) {
            String[] startTime = null;
            String[] endTime = null;
            String divider = ":";

            if(key != null) {
                if(key.equals("start_time"))
                    startTime = value.split(divider);

                if(key.equals("end_time"))
                    endTime = value.split(divider);
            }

            if(startTime == null) startTime = pref.getString("start_time", "20:00").split(divider);
            if(endTime == null) endTime = pref.getString("end_time", "06:00").split(divider);

            DateTime startTimeCalendar =
                    now.withHourOfDay(Integer.parseInt(startTime[0]))
                    .withMinuteOfHour(Integer.parseInt(startTime[1]))
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);

            nextStartTime = startTimeCalendar.isBefore(now)
                    ? startTimeCalendar.plusDays(1).getMillis()
                    : startTimeCalendar.getMillis();

            DateTime endTimeCalendar =
                    now.withHourOfDay(Integer.parseInt(endTime[0]))
                    .withMinuteOfHour(Integer.parseInt(endTime[1]))
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);

            if(autoMode.equals("never"))
                nextEndTime = Long.MAX_VALUE;
            else
                nextEndTime = endTimeCalendar.isBefore(now)
                        ? endTimeCalendar.plusDays(1).getMillis()
                        : endTimeCalendar.getMillis();

            broadcastIntent.putExtra("action", nextStartTime < nextEndTime ? "start" : "end");
        }

        // Re-register NightLightReceiver
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 123456, broadcastIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences.Editor editor = pref.edit();

        if(nextStartTime != -1 && nextEndTime != -1) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextStartTime < nextEndTime ? nextStartTime : nextEndTime, pendingIntent);
            editor.putLong("next_start_time", nextStartTime);
            editor.putLong("next_end_time", nextEndTime);
            editor.putString("time_zone", TimeZone.getDefault().getID());
        } else {
            manager.cancel(pendingIntent);
            editor.remove("next_start_time");
            editor.remove("next_end_time");
            editor.remove("time_zone");
        }

        editor.apply();

        // Check if we need to restart the NotificationService
        if(isNightModeOn(context) && !isServiceRunning(context)) {
            context.startService(new Intent(context, NotificationService.class));
        }

        TileService.requestListeningState(context, new ComponentName(BuildConfig.APPLICATION_ID, QuickSettingsTileService.class.getName()));
    }

    private static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(NotificationService.class.getName().equals(service.service.getClassName()))
                return true;
        }

        return false;
    }

    public static void showPermissionDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message_alt)
                .setPositiveButton(R.string.action_grant_permission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            context.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        } catch (ActivityNotFoundException e) {
                            showErrorDialog(context, "GET_USAGE_STATS");
                        }
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static void showErrorDialog(final Context context, String appopCmd) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.error_dialog_title)
                .setMessage(context.getString(R.string.error_dialog_message, BuildConfig.APPLICATION_ID, appopCmd))
                .setPositiveButton(R.string.action_ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static AlertDialog getApplyDialog(final Context context, final boolean value) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.apply_changes_title)
                .setMessage(context.getString(R.string.apply_changes_message, context.getString(value ? R.string.on : R.string.off).toLowerCase()))
                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setNightMode(context, value);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null);

        return builder.create();
    }

    public static boolean hasUsageStatsPermission(Context context) {
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
        } catch (PackageManager.NameNotFoundException e) { /* Gracefully fail */ }

        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        return applicationInfo != null
                && appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
                == AppOpsManager.MODE_ALLOWED;
    }
    
    public static void resyncNightLight(Context context, LocalDateTime localDateTime, int offset) {
        DateTime now = localDateTime == null ? DateTime.now() : localDateTime.toDateTime();

        SharedPreferences pref = getSharedPreferences(context);
        if(pref.contains("next_start_time") && pref.contains("next_end_time")) {
            DateTime startTimeCalendar = new DateTime(pref.getLong("next_start_time", -1)).minus(offset);
            DateTime endTimeCalendar = new DateTime(pref.getLong("next_end_time", -1)).minus(offset);

            if(startTimeCalendar.isBefore(now) && endTimeCalendar.isBefore(now)) {
                while(startTimeCalendar.isBefore(now) && endTimeCalendar.isBefore(now)) {
                    startTimeCalendar = startTimeCalendar.plusDays(1);
                    endTimeCalendar = endTimeCalendar.plusDays(1);
                }
            }

            if(startTimeCalendar.isBefore(now) && endTimeCalendar.isAfter(now)) {
                setNightMode(context, true);
            } else if(startTimeCalendar.isAfter(now) && endTimeCalendar.isBefore(now)) {
                setNightMode(context, false);
            }
        }

        registerNextReceiver(context, now);
    }
}