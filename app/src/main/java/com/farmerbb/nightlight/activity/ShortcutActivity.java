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

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import com.farmerbb.nightlight.R;
import com.farmerbb.nightlight.util.U;

public class ShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().hasExtra("is_launching_shortcut"))
            U.setNightMode(this, !U.isNightModeOn(this));
        else {
            Intent shortcutIntent = new Intent(this, ShortcutActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.putExtra("is_launching_shortcut", true);

            BitmapDrawable drawable = (BitmapDrawable) getDrawable(R.mipmap.ic_launcher);

            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            if(drawable != null) intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, drawable.getBitmap());
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcut_label));

            setResult(RESULT_OK, intent);
        }

        finish();
    }
}