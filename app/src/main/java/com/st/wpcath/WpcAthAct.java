package com.st.wpcath;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.st.libsec.AppLib;
import com.st.libsec.Dbg;
import com.st.libsec.NfcLib;
import com.st.libsec.WpcCrt;
import com.st.libsec.WpcLog;
import com.st.libsec.WpcPrx;
import com.st.libsec.WpcPtx;

import java.io.File;


/**
 * Qi Authentication demo
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
public class WpcAthAct extends AppCompatActivity {

    /** Transition mode for fragments */
    static final int TRANS_MOD  = FragmentTransaction.TRANSIT_FRAGMENT_FADE;

    private ListFragment    mFrg;                                                                   // Actual shown fragment
    private boolean         mPtx;                                                                   // Emulation mode

    /**
     * The listener class for detected tags
     */
    private class TagLst implements Handler.Callback {

        /**
         * Called when a tag is detected
         *
         * @param   msg The message containing the detected tag
         * @return  true to indicate that this event was processed
         */
        @Override public boolean handleMessage(Message msg) {
            readTag((Tag)msg.obj);                                                                  // Read the detected tag
            return true;                                                                            // Indicate that this event was processed
        }
    }

    /**
     * The Handler class for detected cards
     */
    @SuppressLint({"NewApi", "HandlerLeak"})
    private class TagHnd extends Handler implements NfcAdapter.ReaderCallback {

        /**
         * Initialize Handler for detected tags
         */
        private TagHnd() {
            super(new TagLst());                                                                    // Initialize Handler for detected tags
        }

        /**
         * Called when a tag is detected
         *
         * @param   tag The tag
         */
        @Override public void onTagDiscovered(Tag tag) {
            sendMessage(obtainMessage(0, tag));                                                     // Read the detected tag
        }
    }

    /**
     * Called when the emulation mode button was clicked
     * Switches to the other emulation mode
     *
     * @param   chg Fragment change mode: True Fragment must be exchanged, otherwise added
     * @param   ptx Emulation mode: True PTx emulation, otherwise PRx emulation
     */
    @SuppressLint("NewApi")
    public void chgFrg(boolean chg, boolean ptx) {
        mPtx = ptx;                                                                                 // Change emulation mode
        if (mPtx) {                                                                                 // WTX emulation mode used?
            mFrg = new PtxFrg();                                                                    // Emulate WTx at default
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)) {                            // At least kitkat device?
                int flg = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;    // Set flags for reader mode
                NfcAdapter.getDefaultAdapter(this).enableReaderMode(this, new TagHnd(), flg, null); // Enable only reader mode
            }
        } else {                                                                                    // WPx emulation
            mFrg = new PrxFrg();                                                                    // Emulate WPx at default
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)) {                            // At least kitkat device?
                NfcAdapter.getDefaultAdapter(this).disableReaderMode(this);                         // Disable reader only mode
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {                                  // Need the WPx emulation been switched??
            AppLib.enableComponent(this, WpcPrx.class, !mPtx);                                      // Enable/disable WPx emulation
        }
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();              // Create Fragment transaction
        if (chg) {                                                                                  // Exchange the fragment?
            ft.replace(R.id.lib_frg, mFrg, null);                                                   // Exchange fragment
        } else {                                                                                    // Set new fragment
            ft.add(R.id.lib_frg, mFrg, null);                                                       // Set the new fragment
        }
        ft.setTransition(TRANS_MOD);                                                                // Set transition animation
        ft.commit();                                                                                // Show new fragment
    }

    /**
     * Copy asset files into external app directory
     *
     * @param name  The asset folder name
     */
    private void copyAssets(String name) {
        final @NonNull File dir = new File(getExternalFilesDir(null), name);                        // Get external app directory for asset files
        if (!dir.exists()) {                                                                        // Does the external app directory not exist?
            AppLib.copyAssets(name, name, getAssets(), dir);                                        // Copy all asset files into the external app directory
        }

    }

    /**
     * Called when the activity is created
     * Initialize the Qi Authentication demo app
     *
     * @param savedInstanceState    The state of previous creations
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Wpc);                                                                // Set theme for WPC
        setContentView(R.layout.lib_frg_act);                                                       // Set the layout for activities with frames
        setSupportActionBar((Toolbar)findViewById(R.id.lib_tool_bar));                              // Register action bar
        super.onCreate(savedInstanceState);                                                         // Initialize the activity (mandatory initialization)
        Toolbar bar = findViewById(R.id.lib_tool_bar);                                              // Get the action bar
        ImageView log = bar.findViewById(R.id.lib_img_log);                                         // Get fragment logo
        log.setImageResource(R.drawable.qi_ath);                                                    // Show the app logo
        log.setVisibility(View.VISIBLE);                                                            // Make fragment logo visible
        //noinspection ConstantConditions                                                           // Action bar will be here always available
        getSupportActionBar().setDisplayOptions(AppLib.OPT_STD);                                    // Disable back home button
        if (NfcAdapter.getDefaultAdapter(this) == null) {                                           // No NFC controller available?
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();                // Create Fragment transaction
            ft.add(R.id.lib_frg, new PlgFst(), null);                                               // Show plugfest
            ft.setTransition(WpcAthAct.TRANS_MOD);                                                  // Set transition animation
            ft.commit();                                                                            // Show new fragment
        } else {                                                                                    // NFC controller is available
            chgFrg(false, Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT);                      // Set default emulation mode
        }
        final String app = Dbg.getApp(this, BuildConfig.VERSION_NAME, false);                       // Get app description
        final String bld = Dbg.getBld(BuildConfig.VERSION_CODE, BuildConfig.TIM);                   // Get build information
        WpcLog.init(app, bld);                                                                      // Initialize WPC logger
        copyAssets(WpcPtx.DIR_EMU);                                                                 // Copy all emulation assets files
        copyAssets(PlgFst.DIR_PF);                                                                  // Copy all plugfest assets files
        WpcCrt.init(this, PlgFst.DIR_RT + File.separator);                                          // Initialize WPC Root Certificate
    }

    /**
     * Called when a NFC card is detected
     * Dispatch the received Tag data to the corresponding fragment
     *
     * @param   intent  The intent containing information about the detected NFC card
     */
    @Override protected void onNewIntent(Intent intent) {
        if (mPtx) {                                                                                 // WTx emulation active?
            final String act = intent.getAction();                                                  // Get the action type for the activity creation
            if ((act != null) && (act.equals(NfcAdapter.ACTION_TAG_DISCOVERED))) {                  // Tag without NDEF Message detected?
                readTag((Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));                      // Read the detected tag
            }
        }
    }

    /**
     * Called when this NFC Discover activity is removed from the foreground
     * Releases NFC events for this NFC Discover activity
     */
    @Override protected void onPause(){
        super.onPause();                                                                            // Mandatory call of the parent method
        NfcLib.disableNFCFgd(this);                                                                 // Disable all NFC events for this Discover NFC activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {                                  // Kitkat or newer phone?
            AppLib.enableComponent(this, WpcPrx.class, true);                                       // Enable WPx emulation
        }
    }

    /**
     * Called when this Discover NFC activity is put into the foreground
     * Enables NFC if necessary and register all NFC events for this NFC activity
     */
    @Override protected void onResume(){
        super.onResume();                                                                           // Mandatory call of the parent method
        NfcLib.enableNFCFgd(this);                                                                  // Enable all NFC events for this activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {                                  // Kitkat or newer phone?
            AppLib.enableComponent(this, WpcPrx.class, !mPtx);                                      // Enable/disable WPx emulation
        }
    }

    /**
     * Reads the detected tag
     *
     * @param   tag The detected tag
     */
    private void readTag(final @Nullable Tag tag) {
        if (tag == null) {                                                                          // No tag detected?
            Dbg.log("No tag detected!");                                                            // Log error event
        } else {                                                                                    // Tag was detected
            if (NfcLib.hasTech(tag, IsoDep.class)) {                                                // Tag support ISO-DEP?
                final @NonNull File dir = new File(getExternalFilesDir(null), WpcPtx.DIR_EMU);      // Get the emulation directory
                new WpcPtx(tag, mFrg, new File(dir, PtxFrg.sName)).start();                         // Start the communication to authenticate PTx device
            } else {                                                                                // Another NFC protocol is used
                Dbg.log("Wrong NFC protocol detected!");                                            // Log error
            }
        }
    }

    /**
     * Sets the title in the title bar
     *
     * @param tit   The new title in the title bare
     */
    public void setTit(int tit) {
        TextView tv = findViewById(R.id.lib_txt_tit);                                               // Get title view
        tv.setTextColor(getResources().getColor(R.color.matWht));                                   // Set Title color
        tv.setText(tit);                                                                            // Show the fragment title
    }
}