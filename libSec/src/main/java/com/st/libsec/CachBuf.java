package com.st.libsec;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.ListPopupWindow;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import java.util.ArrayList;

/**
 * Cache buffer class
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
public class CachBuf extends ArrayList<WpcCrtChn> {

    private static final int    MSG_ENA = 1;                                                        // Message to enable the Clear cache button
    private Handler mHnd;                                                                           // Handler to enable Clear cache button
    private int mSiz;                                                                               // The maximum size of the cache buffer

    /**
     * The listener class for a short pressed Clear cache button
     */
    private class ClrCachLst implements View.OnClickListener {

        /**
         * Called when the clear cache button is pressed.
         * Clears the WPC Certificate Chain cache
         *
         * @param   v   The Clear cache button
         */
        @Override public void onClick(View v) {
            clear();                                                                                // Clear the WPC Certificate cache
            v.setEnabled(false);                                                                    // Disable the Clear cache button
        }
    }

    /**
     * The listener class for a long pressed Clear cache button
     */
    private class ShwCachLst implements View.OnLongClickListener {

        /**
         * Called when the clear cache button is pressed for a long time.
         * Shows the WPC Certificate Chain cache
         *
         * @param   v   The Clear cache button
         * @return  true to indicate that this long click event was processed
         */
        @Override public boolean onLongClick(View v) {
            Context ctx = v.getContext();                                                           // Get the app context
            ListPopupWindow lst = new ListPopupWindow(ctx);                                         // Create list popup window
            lst.setAnchorView(v);                                                                   // Connect list popup window with the Clear cache button
            lst.setAdapter(new ArrayAdapter<>(ctx, R.layout.cach_itm, CachBuf.this));               // Initialize list adapter for list popup window
            lst.show();                                                                             // Show the list popup window
            return true;                                                                            // Indicate that this long click event was processed
        }
    }

    /**
     * The listener class to enable the Clear cache button
     */
    private class ShwCachBtn implements Handler.Callback {

        private Button mBtn;                                                                        // The Clear cache button

        /**
         * Initialize the listener to show the WPC Certificate Chain
         *
         * @param   btn The Clear Cache button
         */
        private ShwCachBtn(@NonNull Button btn) {
            mBtn = btn;                                                                             // Register the Clear cache button
        }

        /**
         * Called when the Clear cache button shall be enabled
         *
         * @param   msg The message
         * @return  true to indicate that this event was processed
         */
        @Override public boolean handleMessage(Message msg) {
            mBtn.setEnabled(msg.what == MSG_ENA);                                                   // Enable the Clear cache button
            return true;                                                                            // Indicate that that this event was processed
        }
    }

    /**
     * Initialize the cache buffer
     *
     * @param   siz The maximum size of the cache buffer
     */
    public CachBuf(int siz) {
        super(siz);                                                                                 // Initialize the cache buffer
        mSiz = siz;                                                                                 // Set the maximum size of the cache buffer
    }

    /**
     * Add a WPC Certificate Chain in the cache buffer
     *
     * @param   e   The new WPC Certificate Chain to be added to the cache buffer
     * @return  true to indicate successful addition of WPC Certificate Chain
     */
    @Override public boolean add(WpcCrtChn e) {
        int siz = size();                                                                           // Get the actual size of the cache buffer
        if (mSiz == siz) {                                                                          // Cache buffer has reached its maximum size?
            remove(mSiz - 1);                                                                       // Remove the oldest WPC Certificate chain
        } else if ((siz == 0) && (mHnd != null)) {                                                  // First WPC Certificate added an Clear cache button is shown?
            mHnd.sendEmptyMessage(MSG_ENA);                                                         // Enable the Clear cache button
        }
        add(0, e);                                                                                  // Add new WPC Certificate Chain
        return true;                                                                                // Return indication that addition was successful
    }

    /**
     * Register actual shown Clear cache button
     *
     * @param   btn The actual shown Clear cache button or null
     */
    public void regCachBtn(@Nullable Button btn) {
        if (btn == null) {                                                                          // No clear cache button shown?
            mHnd = null;                                                                            // Register no handler to enable the Clear cache button
        } else {                                                                                    // A Clear cache button is shown
            mHnd = new Handler(new ShwCachBtn(btn));                                                // Create handler to enable the Clear cache button
            btn.setEnabled(size() != 0);                                                            // Show the actual button state
        }
    }

    /**
     * Sets the listener for the Clear Cache button
     */
    public void setCachBtnLst(@NonNull Button btn) {
        btn.setOnClickListener(new ClrCachLst());                                                   // Set a listener for click on Clear cache button
        btn.setOnLongClickListener(new ShwCachLst());                                               // Set listener for long click on Clear cache button
    }
}