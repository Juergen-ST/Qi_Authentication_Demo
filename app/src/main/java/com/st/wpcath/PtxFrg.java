package com.st.wpcath;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.st.libsec.Dbg;
import com.st.libsec.WpcCrt;
import com.st.libsec.WpcCrtChn;
import com.st.libsec.WpcFil;
import com.st.libsec.WpcKey;
import com.st.libsec.WpcPtx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * PTx emulation
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
public class PtxFrg extends ListFragment implements AdapterView.OnItemSelectedListener, View.OnClickListener  {

    static String sName;                                                                            // Actual adjusted device name

    /**
     * Listener for the plugfest button
     */
    static class TestBtnLst implements View.OnClickListener {

        private final @NonNull FragmentActivity mAct;                                               // Main activity hosting the plugfest button

        /**
         * Initialses the listener for the plugfest button
         *
         * @param act   The main activity hosting this button
         */
        TestBtnLst(final @NonNull FragmentActivity act) {
            mAct = act;                                                                             // Remember the main activity hosting the plugfest button
        }

        /**
         * Called when the Plugfest button was pressed
         *
         * @param v The pressed Plugfest button (not used here)
         */
        @Override
        public void onClick(View v) {
            FragmentTransaction ft = mAct.getSupportFragmentManager().beginTransaction();           // Create Fragment transaction
            ft.replace(R.id.lib_frg, new PlgFst(), null);                                           // Exchange fragment
            ft.addToBackStack(null);                                                                // Add fragment to back stack
            ft.setTransition(WpcAthAct.TRANS_MOD);                                                  // Set transition animation
            ft.commit();                                                                            // Show new fragment
        }
    }

    /**
     * Called when the mode button was clicked
     * Changes to WPx emulation
     *
     * @param v The mode button (not used here)
     */
    @Override public void onClick(View v) {
        //noinspection ConstantConditions                                                           // Activity will be at this method always available
        ((WpcAthAct)getActivity()).chgFrg(true, false);                                             // Change to PRx emulation
    }

    /**
     * Called when the layout of the fragment shall be created
     *
     * @param inflater              The layout inflater of the app
     * @param container             The view group containing the fragment
     * @param savedInstanceState    Previous saved state of the fragment (not used here)
     * @return                      The layout of the fragment
     */
    @Override public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ptx_frg, container, false);                                // Create fragment view
    }

    /**
     * Called when an item of the spinner is selected
     * Sets the PTx device
     *
     * @param parent    The spinner view (not used here)
     * @param view      The item view (not used here)
     * @param position  The position of the item in the spinner list
     * @param id        The item identifier (not used here)
     */
    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        sName = (String)parent.getAdapter().getItem(position);                                      // Get the emulated PTx device name
        //noinspection ConstantConditions                                                           // Activity will be at this method always available
        final @NonNull File dir = new File(getContext().getExternalFilesDir(null), WpcPtx.DIR_EMU); // Get directory for Qi Authentication plugfest
        try {
            WpcPtx.sChn = new WpcCrtChn(new WpcFil(dir, sName + WpcCrtChn.EXT_CHN).read());         // Load WPC certificate chain of the device
            WpcPtx.sPrv = WpcKey.getPrvKey(new WpcFil(dir, sName + WpcCrt.EXT_PRV).read());         // Load the private key of the device
        } catch(IOException err) {                                                                  // Error occurred
            Dbg.log("Cannot load device", err);                                                     // Log error
        }

    }

    /**
     * Called when no item was selected
     * Does nothing
     *
     * @param parent    The spinner view
     */
    @Override public void onNothingSelected(AdapterView<?> parent) {}

     /**
     * Called when the layout of the web link fragment was created
     *
     * @param view                  The web link fragment layout
     * @param savedInstanceState    The web link fragment state
     */
    @Override public void onViewCreated (@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);                                              // Initialize the fragment view
        //noinspection ConstantConditions                                                           // Activity will be at this method always available
        final @NonNull WpcAthAct act = (WpcAthAct)getActivity();                                    // Get the main activity
        act.setTit(R.string.emu_ptx);                                                               // Set title of fragment
        final @NonNull File dir = new File(act.getExternalFilesDir(null), WpcPtx.DIR_EMU);          // Get directory for Qi Authentication plugfest
        final @NonNull ArrayList<String> list = new ArrayList<>();                                  // Create device list
        for (File file : dir.listFiles()) {                                                         // Repeat for all files
            String name = file.getName();                                                           // Get the file name
            int ind = name.indexOf(WpcCrtChn.EXT_CHN);                                              // Search for WPC chain files
            if (ind > 0) {                                                                          // WPC chain file found?
                String dev = name.substring(0, ind);                                                // Get device name
                if (new File(dir, dev + WpcCrt.EXT_PRV).exists()) {                                 // Exists the corresponding private key file?
                    list.add(dev);                                                                  // Add the device to the emulated Ptx spinner list
                }
            }
        }
        final @NonNull ArrayAdapter<String> apt;                                                    // Adapter for subdirectories
        apt = new ArrayAdapter<>(act, android.R.layout.simple_spinner_item, list);                  // Create adapter for subdirectories
        apt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);                 // Set layout for the PRx device spinner drop down menu
        final @NonNull Spinner spn = view.findViewById(R.id.spn_dev);                               // Get spinner for emulated PTx device
        spn.setAdapter(apt);                                                                        // Register the menu for the PRx device spinner
        spn.setOnItemSelectedListener(this);                                                        // Register Listener for new selected item
        spn.setSelection(0);                                                                        // Set the spinner to the actual set PRx
        final @NonNull Button btn = view.findViewById(R.id.btn_mod);                                // Get the mode button
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)) {                                // At least Kitkat phone?
            btn.setOnClickListener(this);                                                           // Register listener for mode button
        } else {                                                                                    // Older phone model
            btn.setEnabled(false);                                                                  // Disable mode button
        }
        view.findViewById(R.id.btn_test).setOnClickListener(new TestBtnLst(act));                   // Register listener for the plugfest button
    }
}