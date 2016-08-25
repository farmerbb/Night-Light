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

package com.farmerbb.nightlight.service;

import android.Manifest;
import android.content.pm.PackageManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.farmerbb.nightlight.util.U;

public class QuickSettingsTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();
    }

    @Override
    public void onClick() {
        super.onClick();
        U.setNightMode(this, !U.isNightModeOn(this));
        updateState();
    }

    private void updateState() {
        Tile tile = getQsTile();
        if(tile != null) {
            if(checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED)
                tile.setState(Tile.STATE_UNAVAILABLE);
            else
                tile.setState(U.isNightModeOn(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);

            tile.updateTile();
        }
    }
}