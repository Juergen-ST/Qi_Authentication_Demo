package com.st.wpcath;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import com.st.libsec.WpcAthIni;
import com.st.libsec.WpcPrx;

/**
 * PRx emulation
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
public class PrxFrg extends ListFragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private Button mBtn;                                                                            // Clear cache button

    /**
     * Called when the mode button was clicked
     * Changes to WTx emulation
     *
     * @param v The mode button (not used here)
     */
    @Override public void onClick(View v) {
        //noinspection ConstantConditions                                                           // Activity will be at this method always available
        ((WpcAthAct)getActivity()).chgFrg(true, true);                                              // Change to WTx emulation
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
        return inflater.inflate(R.layout.prx_frg, container, false);                                // Create fragment view
    }

    /**
     * Called when an item of the spinner is selected
     * Sets the Qi Authentication Initiator flow for the PRx
     *
     * @param parent    The spinner view (not used here)
     * @param view      The item view (not used here)
     * @param position  The position of the item in the spinner list
     * @param id        The item identifier (not used here)
     */
    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        WpcPrx.sFlw = WpcAthIni.FlwTyp.values()[position];                                          // Set the selected Qi Authentication Initiator flow
    }

    /**
     * Called when no item was selected
     * Does nothing
     *
     * @param parent    The spinner view
     */
    @Override public void onNothingSelected(AdapterView<?> parent) {}

    /**
     * Called before this fragment is shown
     */
    @Override public void onResume() {
        super.onResume();                                                                           // Mandatory call of standard routine
        WpcPrx.sFrg = this;                                                                         // Register this fragment at the Qi Authentication application
        WpcPrx.sCach.regCachBtn(mBtn);                                                              // Register the Clear cache button
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);                                    // Set UI thread set to maximum priority

    }

    /**
     * Called when this fragment is hidden
     */
    @Override public void onPause() {
        super.onPause();                                                                            // Mandatory call of standard routine
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);                                    // Set Background set to minimum priority
        WpcPrx.sFrg = null;                                                                         // De-register this fragment at the Qi Authentication application
        WpcPrx.sCach.regCachBtn(null);                                                              // Deregister the Clear cache button
    }

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
        act.setTit(R.string.emu_prx);                                                               // Set title of fragment
        final @NonNull Spinner spn = view.findViewById(R.id.spn_flw);                               // Get spinner for Initiator flow
        spn.setSelection(WpcPrx.sFlw.ordinal());                                                    // Set the spinner to the actual set Initiator flow
        spn.setOnItemSelectedListener(this);                                                        // Register Listener for new selected initiator flow
        view.findViewById(R.id.btn_mod).setOnClickListener(this);                                   // Register listener for mode button
        view.findViewById(R.id.btn_test).setOnClickListener(new PtxFrg.TestBtnLst(act));            // Register listener for the plugfest button
        mBtn = view.findViewById(R.id.btn_cach);                                                    // Get the Clear cache button
        WpcPrx.sCach.setCachBtnLst(mBtn);                                                           // Configure the Clear cache button
    }
}