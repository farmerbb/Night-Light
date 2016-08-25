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

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.farmerbb.nightlight.R;
import com.farmerbb.nightlight.util.PluginBundleManager;

public class TaskerActivityBase extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(this instanceof TaskerActionActivity)
            setTitle(R.string.tasker_action_title);

        if(this instanceof TaskerConditionActivity)
            setTitle(R.string.tasker_condition_title);

        addPreferencesFromResource(R.xml.pref_tasker);
        findPreference("tasker_on").setOnPreferenceClickListener(this);
        findPreference("tasker_off").setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference p) {
        final Intent resultIntent = new Intent();

        /*
         * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note
         * that anything placed in this Bundle must be available to Locale's class loader. So storing
         * String, int, and other standard objects will work just fine. Parcelable objects are not
         * acceptable, unless they also implement Serializable. Serializable objects must be standard
         * Android platform objects (A Serializable class private to this plug-in's APK cannot be
         * stored in the Bundle, as Locale's classloader will not recognize it).
         */
        final Bundle resultBundle = PluginBundleManager.generateBundle(this, p.getKey());
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, resultBundle);

        /*
         * The blurb is concise status text to be displayed in the host's UI.
         */

        switch(p.getKey()) {
            case "tasker_on":
                resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, getString(R.string.on));
                break;
            case "tasker_off":
                resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, getString(R.string.off));
                break;
        }

        setResult(RESULT_OK, resultIntent);

        finish();
        return true;
    }
}