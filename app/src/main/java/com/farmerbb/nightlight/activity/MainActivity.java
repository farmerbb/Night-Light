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

package com.farmerbb.nightlight.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;

import com.farmerbb.nightlight.BuildConfig;
import com.farmerbb.nightlight.R;
import com.farmerbb.nightlight.fragment.SettingsFragment;
import com.farmerbb.nightlight.util.U;

import org.joda.time.DateTime;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat theSwitch;
    private CheckForRoot checkForRoot;
    private ProgressDialog dialog;
    private AlertDialog applyDialog;

    private boolean shouldFinish = false;

    static { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO); }

    private BroadcastReceiver switchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSwitch();
        }
    };

    private BroadcastReceiver showDialogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences pref = U.getSharedPreferences(MainActivity.this);
                    if(!pref.getString("turn_on_automatically", "never").equals("never")) {
                        String divider = ":";
                        String[] startTime = pref.getString("start_time", "20:00").split(divider);
                        String[] endTime = pref.getString("end_time", "06:00").split(divider);

                        DateTime startTimeCalendar = DateTime.now()
                                .withHourOfDay(Integer.parseInt(startTime[0]))
                                .withMinuteOfHour(Integer.parseInt(startTime[1]))
                                .withSecondOfMinute(0)
                                .withMillisOfSecond(0);

                        DateTime endTimeCalendar = DateTime.now()
                                .withHourOfDay(Integer.parseInt(endTime[0]))
                                .withMinuteOfHour(Integer.parseInt(endTime[1]))
                                .withSecondOfMinute(0)
                                .withMillisOfSecond(0);

                        if(endTimeCalendar.isBeforeNow())
                            endTimeCalendar = endTimeCalendar.plusDays(1);

                        if(startTimeCalendar.isBeforeNow() && endTimeCalendar.isBeforeNow()) {
                            while(startTimeCalendar.isBeforeNow() && endTimeCalendar.isBeforeNow()) {
                                startTimeCalendar = startTimeCalendar.plusDays(1);
                                endTimeCalendar = endTimeCalendar.plusDays(1);
                            }
                        }

                        if(startTimeCalendar.isBeforeNow() && endTimeCalendar.isAfterNow()) {
                            if(!U.isNightModeOn(MainActivity.this)) showApplyDialog(true);
                        } else {
                            if(U.isNightModeOn(MainActivity.this)) showApplyDialog(false);
                        }
                    }
                }
            }, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(switchReceiver, new IntentFilter("com.farmerbb.nightlight.UPDATE_SWITCH"));

        checkForPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateSwitch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(switchReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(showDialogReceiver);
    }

    @Override
    public void finish() {
        if(checkForRoot != null
                && checkForRoot.getStatus() == AsyncTask.Status.RUNNING)
            checkForRoot.cancel(true);

        super.finish();
    }

    private void checkForPermission() {
        if(checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            checkForRoot = new CheckForRoot();
            checkForRoot.execute();
        } else proceedWithAppLaunch();
    }

    private void proceedWithAppLaunch() {
        setContentView(R.layout.main);

        SharedPreferences pref = getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", MODE_PRIVATE);
        if(!pref.getBoolean("is_snoozed", false))
            U.registerNextReceiver(this);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setCustomView(R.layout.switch_layout);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        theSwitch = (SwitchCompat) findViewById(R.id.the_switch);
        if(theSwitch != null) {
            theSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(U.isNightModeOn(MainActivity.this) != b)
                        U.setNightMode(MainActivity.this, b);
                }
            });
        }

        if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 42);
        else if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !pref.getBoolean("dont_show_uninstall_dialog", false))
            startActivity(new Intent(this, UninstallNotificationActivity.class));

        if(pref.getString("start_time", "99:99").equals("99:99"))
            pref.edit().putString("start_time", "20:00").apply();

        if(pref.getString("end_time", "99:99").equals("99:99"))
            pref.edit().putString("end_time", "06:00").apply();

        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new SettingsFragment(), "SettingsFragment").commit();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(showDialogReceiver, new IntentFilter("com.farmerbb.nightlight.SHOW_DIALOG"));
            }
        }, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            SharedPreferences pref = U.getSharedPreferences(this);
            if(!pref.getBoolean("dont_show_uninstall_dialog", false))
                startActivity(new Intent(this, UninstallNotificationActivity.class));
        }
    }

    private void updateSwitch() {
        if(theSwitch != null)
            theSwitch.setChecked(U.isNightModeOn(this));
    }

    private void showApplyDialog(boolean value) {
        if(applyDialog == null || !applyDialog.isShowing()) {
            applyDialog = U.getApplyDialog(this, value);
            applyDialog.show();
        }
    }

    private final class CheckForRoot extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage(getResources().getString(R.string.checking_for_superuser));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Shell.SU.run(new String[] {
                    "pm grant " + BuildConfig.APPLICATION_ID + " " + Manifest.permission.WRITE_SECURE_SETTINGS,
                    "appops set " + BuildConfig.APPLICATION_ID + " GET_USAGE_STATS allow"
            });
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                dialog.dismiss();

                if(checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    if(shouldFinish)
                        finish();
                    else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.permission_dialog_title)
                                .setMessage(getString(R.string.permission_dialog_message, BuildConfig.APPLICATION_ID, Manifest.permission.WRITE_SECURE_SETTINGS))
                                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        shouldFinish = true;
                                        checkForPermission();
                                    }
                                });

                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                        alertDialog.setCancelable(false);
                    }
                } else proceedWithAppLaunch();
            } catch (IllegalStateException e) {
                finish();
            } catch (IllegalArgumentException e) { /* Gracefully fail */ }
        }
    }
}