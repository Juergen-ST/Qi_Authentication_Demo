package com.st.libsec;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.WindowManager.LayoutParams;

/**
 * A helper activity class to turn on the screen
 *
 * Copyright 2019 STMicroelectronics Application GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jürgen Böhler
 */
public class WakAct extends Activity {

    /**
     * The broadcast receiver listening for a turned on screen
     */
    private class ScrRcv extends BroadcastReceiver {

        /**
         * Called when the screen is turned on
         * Finishes this activity
         *
         * @param context   The app context (not used here)
         * @param intent    The intent for switched on screen (not used here)
         */
        @Override public void onReceive(Context context, Intent intent) {
            finish();                                                                               // Finish this activity
        }
    }

    /**
     * Creates the helper activity to turn on the screen
     *
     * @param savedInstanceState    The saved origin state
     */
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                                                         // Initialize this activity
        getWindow().addFlags(LayoutParams.FLAG_TURN_SCREEN_ON | LayoutParams.FLAG_SHOW_WHEN_LOCKED);// Set the window flags to turn on screen
        registerReceiver(new ScrRcv(), new IntentFilter(Intent.ACTION_SCREEN_ON));                  // Set broadcast receiver for turned on screen
    }
}
