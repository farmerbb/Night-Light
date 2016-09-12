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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.farmerbb.nightlight.BuildConfig;
import com.farmerbb.nightlight.R;
import com.farmerbb.nightlight.util.Blacklist;
import com.farmerbb.nightlight.util.ListEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SelectAppActivity extends AppCompatActivity {

    private AppListGenerator appListGenerator;
    private ProgressBar progressBar;
    private ListView appList;

    static { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_app);
        setFinishOnTouchOutside(false);
        setTitle(getString(R.string.edit_blacklist));

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        appList = (ListView) findViewById(R.id.list);

        appListGenerator = new AppListGenerator();
        appListGenerator.execute();
    }

    @Override
    public void finish() {
        if(appListGenerator != null && appListGenerator.getStatus() == AsyncTask.Status.RUNNING)
            appListGenerator.cancel(true);

        super.finish();
    }

    private class AppListAdapter extends ArrayAdapter<ListEntry> {
        AppListAdapter(Context context, int layout, List<ListEntry> list) {
            super(context, layout, list);
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            // Check if an existing view is being reused, otherwise inflate the view
            if(convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row, parent, false);

            final ListEntry entry = getItem(position);
            final Blacklist blacklist = Blacklist.getInstance(getContext());

            TextView textView = (TextView) convertView.findViewById(R.id.name);
            textView.setText(entry.getLabel());

            final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
            checkBox.setChecked(blacklist.isBlocked(entry.getPackageName()));

            LinearLayout layout = (LinearLayout) convertView.findViewById(R.id.entry);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(blacklist.isBlocked(entry.getPackageName())) {
                        blacklist.removeBlockedApp(getContext(), entry.getPackageName());
                        checkBox.setChecked(false);
                    } else {
                        blacklist.addBlockedApp(getContext(), entry);
                        checkBox.setChecked(true);
                    }
                }
            });

            return convertView;
        }
    }

    private final class AppListGenerator extends AsyncTask<Void, Void, AppListAdapter> {
        @Override
        protected AppListAdapter doInBackground(Void... params) {
            final PackageManager pm = getPackageManager();

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> info = pm.queryIntentActivities(intent, 0);

            Collections.sort(info, new Comparator<ResolveInfo>() {
                @Override
                public int compare(ResolveInfo ai1, ResolveInfo ai2) {
                    return ai1.activityInfo.loadLabel(pm).toString().compareTo(ai2.activityInfo.loadLabel(pm).toString());
                }
            });

            final List<ListEntry> entries = new ArrayList<>();
            for(ResolveInfo appInfo : info) {
                if(!appInfo.activityInfo.applicationInfo.packageName.equals(BuildConfig.APPLICATION_ID))
                    entries.add(new ListEntry(
                            appInfo.activityInfo.applicationInfo.packageName,
                            appInfo.loadLabel(pm).toString()));
            }

            return new AppListAdapter(SelectAppActivity.this, R.layout.row, entries);
        }

        @Override
        protected void onPostExecute(AppListAdapter adapter) {
            progressBar.setVisibility(View.GONE);
            appList.setAdapter(adapter);
        }
    }
}