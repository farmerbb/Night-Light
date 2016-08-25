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

package com.farmerbb.nightlight.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;

import com.farmerbb.nightlight.BuildConfig;
import com.farmerbb.nightlight.R;
import com.farmerbb.nightlight.activity.SelectAppActivity;
import com.farmerbb.nightlight.util.TimePreference;
import com.farmerbb.nightlight.util.U;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener {

    private boolean finishedLoadingPrefs;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        // Set values
        setRetainInstance(true);
        setHasOptionsMenu(true);

        // Add preferences
        addPreferencesFromResource(R.xml.pref_general);
        addPreferencesFromResource(R.xml.pref_about);

        // Set OnClickListeners for certain preferences
        findPreference("start_time").setOnPreferenceClickListener(this);
        findPreference("end_time").setOnPreferenceClickListener(this);
        findPreference("blacklist").setOnPreferenceClickListener(this);
        findPreference("about").setOnPreferenceClickListener(this);
        findPreference("about").setSummary(getString(R.string.pref_about_description, new String(Character.toChars(0x1F601))));

        bindPreferenceSummaryToValue(findPreference("turn_on_automatically"));
        bindPreferenceSummaryToValue(findPreference("start_time"));
        bindPreferenceSummaryToValue(findPreference("end_time"));

        finishedLoadingPrefs = true;
    }

    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if(preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference
                        .setSummary(index >= 0 ? listPreference.getEntries()[index]
                                : null);
                if(preference.getKey().equals("turn_on_automatically")) {
                    boolean shouldEnableTimePrefs = stringValue.equals("custom_schedule");
                    findPreference("start_time").setEnabled(shouldEnableTimePrefs);
                    findPreference("end_time").setEnabled(shouldEnableTimePrefs);

                    if(stringValue.equals("sunset_to_sunrise")) {
                        U.refreshSunriseSunsetTime(getActivity());

                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                bindPreferenceSummaryToValue(findPreference("start_time"));
                                bindPreferenceSummaryToValue(findPreference("end_time"));
                            }
                        });
                    }
                }
            } else if(preference instanceof TimePreference) {
                if(DateFormat.is24HourFormat(getActivity())) {
                    preference.setSummary(stringValue);
                } else {
                    String divider = ":";
                    String[] pieces = stringValue.split(divider);
                    int hour = Integer.parseInt(pieces[0]);
                    String amPm = hour < 12 ? " AM" : " PM";

                    if(hour == 0)
                        hour = 12;
                    else if(hour > 12)
                        hour = hour - 12;

                    preference.setSummary(String.valueOf(hour) + divider + pieces[1] + amPm);
                }
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }

            SharedPreferences pref = U.getSharedPreferences(getActivity());
            if(finishedLoadingPrefs && !pref.getBoolean("is_snoozed", false)) {
                U.registerNextReceiver(getActivity(), preference.getKey(), stringValue);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("com.farmerbb.nightlight.SHOW_DIALOG"));
            }

            return true;
        }
    };

    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference
                .setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager.getDefaultSharedPreferences(
                        preference.getContext()).getString(preference.getKey(),
                        ""));
    }

    @Override
    public boolean onPreferenceClick(Preference p) {
        switch(p.getKey()) {
            case "blacklist":
                if(U.hasUsageStatsPermission(getActivity()))
                    startActivity(new Intent(getActivity(), SelectAppActivity.class));
                else
                    U.showPermissionDialog(getActivity());
                break;
            case "about":
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                break;
        }

        return true;
    }
}