package com.st.libsec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Library functions for NFC operations
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
public class NfcLib {

    private static final String     ACTION_ADAPTER_STATE_CHANGED= "android.nfc.action.ADAPTER_STATE_CHANGED";
    private static final String     ACTION_NFC_SETTINGS         = "android.settings.NFC_SETTINGS";  // Also defined by Settings since API16
    private static final String     EXTRA_ADAPTER_STATE         = "android.nfc.extra.ADAPTER_STATE";// Also defined by NfcAdapter since API18
    private static final int        FLAG_MSK = Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;
    private static final int        STATE_OFF                   = 1;                                // Also defined by NFCAdapter since API18
    private static final int        STATE_ON                    = 3;                                // Also defined by NFCAdapter since API18
    private static final int        STATE_TURNING_ON            = 2;                                // Also defined by NFCAdapter since API18

    private static boolean          mRcv                        = false;                            // Reception flag

    /**
     * Broadcast receiver to listen when the NFC settings have been changed
     */
    private static class NfcChRcv extends BroadcastReceiver {

        private Activity    mAct;                                                                   // Activity enabling NFCC
        private Toast       mTst;                                                                   // Toast message to enable NFCC

        /* Creates a broadcast receiver to detect changed NFC settings
         * @param act   The activity enabling the NFCC
         * @param tst   The toast message asking to enable the NFCC
         */
        private NfcChRcv(Activity act, Toast tst) {
            mAct = act;                                                                             // Remember the activity enabling the NFCC
            mTst = tst;                                                                             // Remember the toast message asking to enable NFCC
        }

        /**
         * Called when the NFC state has been changed.
         * Removes the actual shown short user message and closes the NFC setting activity.
         * @param context    The context in which the receiver is running (not used here)
         * @param intent     The intent being received
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(EXTRA_ADAPTER_STATE, STATE_OFF);                         // Get new state of NFC Adapter
            if (state == STATE_ON || state == STATE_TURNING_ON) {                                   // NFC Adapter is switched on or is turning on
                context.unregisterReceiver(this);                                                   // Unregister this Broadcast receiver
                mTst.cancel();                                                                      // Dismiss toast message
                intent = new Intent(mAct, mAct.getClass());                                         // Get intent for activity enabling NFCC
                intent.addFlags(FLAG_MSK);                                                          // Set flags to restart existing activity
                mAct.startActivity(intent);                                                         // Restart activity enabling the NFCC
                mRcv = true;                                                                        // Remember that activity is restarted
            }
        }
    }

    /**
     * Release all foreground NFC events for the NFC activity
     * @param act   The activity where all NFC foreground events will be released
     */
    public static void disableNFCFgd(Activity act) {
        NfcAdapter nfcc = NfcAdapter.getDefaultAdapter(act);                                        // Get NFC Controller
        if (nfcc != null) {                                                                         // NFC Controller available?
            nfcc.disableForegroundDispatch(act);                                                    // Release all foreground NFC events for the NFC activity
        }
    }

    /**
     * Tries to enable the NFC adapter if not already enabled
     */
    public static void enableNFC(Activity act) {
        NfcAdapter nfcc = NfcAdapter.getDefaultAdapter(act);                                        // Get NFC Controller
        if (nfcc != null) {                                                                         // NFC Controller available?
            if (!nfcc.isEnabled() && !mRcv) {                                                       // Need the state of the NFC adapter been changed?
                Intent intent = new Intent(ACTION_NFC_SETTINGS);                                    // Define intent to start NFC setting activity
                try {
                    act.startActivity(intent);                                                      // Activate the setting menu to enable NFC
                    CharSequence nam = act.getApplicationInfo().loadLabel(act.getPackageManager()); // Get app name
                    String des = String.format(act.getString(R.string.lib_needed), nam);            // Get Description of toast
                    Toast tst = showTst(act, R.string.nfc_enable_nfc, des);                         // Show toast message
                    IntentFilter fil = new IntentFilter(ACTION_ADAPTER_STATE_CHANGED);              // Configure intent filter to listen for changed NFC Adapter state
                    act.registerReceiver(new NfcChRcv(act, tst), fil);                              // Register Broadcast receiver to listen for changed NFC Adapter state
                } catch (ActivityNotFoundException err){                                            // System menu to enable NFC was not found
                    Dbg.showFat(act, R.string.nfc_not_enabled, err);                                // Finish app with error message
                }
            }
            mRcv = false;                                                                           // Remember that NFCC need to be enabled
        }
    }

    /**
     * Registers all NFC event for the NFC activity
     * @param act   The activity where all NFC foreground events will be registered
     */
    public static void enableNFCForegound(Activity act) {
        NfcAdapter nfcc = NfcAdapter.getDefaultAdapter(act);                                        // Get NFC Controller
        if (nfcc != null) {                                                                         // NFC Controller available?
            Intent intent = new Intent(act, act.getClass());                                        // Create intent for pending intent
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);                                       // Specify intent for pending intent
            PendingIntent pi = PendingIntent.getActivity(act, 0, intent, 0);                        // Set pending intent for NFC foreground dispatch
            nfcc.enableForegroundDispatch(act, pi, null, null);                                     // Register the NFC activity for all NFC technologies
        }
    }

    /**
     * Enable NFC if not already enabled and registers all NFC cards for the given NFC activity
     *
     * @param act   The activity where all NFC foreground events will be registered
     */
    public static void enableNFCFgd(Activity act) {
        enableNFC(act);                                                                             // Enable NFC if not already enabled
        enableNFCForegound(act);                                                                    // Register all NFC cards for the given activity
    }

    /**
     * Checks if a given tag supports a given NFC technology
     * @param tag    The tag which shall be checked
     * @param cla    The the searched NFC technology class 
     * @return true if the searched NFC technology is supported by the tag, otherwise false
     */
    public static boolean hasTech(Tag tag, Class<?> cla) {
        String[] list = tag.getTechList();                                                          // Get all supported NFC technologies
        String tech = cla.getName();                                                                // Get the class name of the NFC technology class
        for (String entry: list) {                                                                  // Check for all reported NFC technologies
            if (entry.equals(tech)) {                                                               // Is the searched NFC technology found?
                return true;                                                                        // Inform that the searched NFC technology was found
            }
        }
        return false;                                                                               // Inform that the searched NFC technology was not found
    }

    /** Shows a toast
     *
     * @param ctx   The activity showing the toast
     * @param tit   The title of the toast
     * @param des   The description of the toast
     * @return      The showed toast
     */
    private static Toast showTst(@NonNull Context ctx, @StringRes int tit, @Nullable String des) {
        LayoutInflater li = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  // Get Layoutinflater of device
        @SuppressLint("InflateParams")                                                              // The toast loayout must not have a root
        View vw = li.inflate(R.layout.nfc_tst_nfc, null);                                           // Create the layout for the toast
        ((TextView)vw.findViewById(R.id.lib_tit)).setText(tit);                                     // Set the title
        if (des != null) {                                                                          // Description available?
            ((TextView) vw.findViewById(R.id.lib_txt_des)).setText(des);                            // Set the description
        }
        Toast tst = new Toast(ctx);                                                                 // Create the taost
        tst.setDuration(Toast.LENGTH_LONG);                                                         // Set the showing time
        tst.setMargin(0f, 0f);                                                                      // Set distance to screen
        tst.setGravity(Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0);                             // Set toast at the bottom of screen
        tst.setView(vw);                                                                            // Set the layout
        tst.show();                                                                                 // Show the toast
        ImageView iv = vw.findViewById(R.id.lib_ico);                                               // Get icon view object
        ((AnimationDrawable)iv.getDrawable()).start();                                              // Start animation of switch
        return tst;                                                                                 // Return the toast
    }
}