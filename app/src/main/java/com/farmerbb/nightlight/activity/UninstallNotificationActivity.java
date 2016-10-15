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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import com.farmerbb.nightlight.BuildConfig;
import com.farmerbb.nightlight.R;
import com.farmerbb.nightlight.util.U;

public class UninstallNotificationActivity extends AppCompatActivity {

    static { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences pref = U.getSharedPreferences(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.uninstall_dialog_title)
                .setMessage(R.string.uninstall_dialog_text)
                .setNegativeButton(pref.getBoolean("uninstall_dialog_shown", false)
                        ? R.string.action_dont_show_again
                        : R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pref.edit().putBoolean(pref.getBoolean("uninstall_dialog_shown", false)
                                ? "dont_show_uninstall_dialog"
                                : "uninstall_dialog_shown", true).apply();
                        finish();
                    }
                })
                .setPositiveButton(R.string.action_uninstall, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + BuildConfig.APPLICATION_ID)));

                        pref.edit().putBoolean("uninstall_dialog_shown", true).apply();
                        U.setNightMode(UninstallNotificationActivity.this, false);
                        finish();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCancelable(false);
    }
}