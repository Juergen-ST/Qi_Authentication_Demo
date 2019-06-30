package com.st.libsec;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class to store a log file
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
public class LogFile extends Thread implements Handler.Callback {

    private final @NonNull File         mFile;                                                      // Log file header
    private final @NonNull ListFragment mFrg;                                                       // List fragment which sahll be logged
    private final @NonNull Handler      mHnd;                                                       // Handler for saved log file

    /**
     * Initialise thread to store a log file
     *
     * @param file  The log file header
     * @param frg   The listfragment containing the String array
     */
    LogFile(final @NonNull File file, final @NonNull ListFragment frg) {
        mFile = file;                                                                               // Store the file header
        mFrg = frg;                                                                                 // Store the list fragemnt
        mHnd = new Handler(this);                                                                   // Set handler for saved log file
    }

    /**
     * Thread to store a log file
     */
    @Override public void run() {
        FileOutputStream fos = null;                                                                // File output stream
        //noinspection unchecked
        ArrayAdapter<String> apt = (ArrayAdapter<String>)mFrg.getListAdapter();                     // Get list adapter of fragment
        try {
            final int cnt = apt.getCount();                                                         // Get number of list entries
            if (cnt > 0) {                                                                          // Log messages available
                fos = new FileOutputStream(mFile);                                                  // Open file output stream
                for (int ind = 0; ind < cnt; ind++) {                                               // Repeat for all list entries
                    fos.write((apt.getItem(ind) + "\r\n").getBytes(AppLib.CHR_ISO));                // Write list entry
                }
                mHnd.sendMessage(mHnd.obtainMessage(0, mFile.getName()));                           // Inform about successful storage of log file
            }
        } catch (IOException err) {                                                                 // An error occurred
            mHnd.sendEmptyMessage(1);                                                               // Inform about error
            Dbg.log("Cannot write log file", err);                                                  // Log error
        } finally {
            if (fos != null) {                                                                      // File output stream opened?
                try {
                    fos.close();                                                                    // Close file output stream
                } catch (IOException err) {                                                         // Error occurred (should never occur)
                    Dbg.log("Cannot close log file", err);                                          // Log error
                }
            }
        }

    }

    /**
     * Called whemn the log file is saved
     *
     * @param   msg The result of the saved log file
     * @return  true to indicate that this event was processed
     */
    @Override public boolean handleMessage(Message msg) {
        //noinspection ConstantConditions                                                           // Main activity will be always available
        final @NonNull Activity act = mFrg.getActivity();                                           // Get the main activity
        if (msg.what == 0) {                                                                        // No error occurred?
            String txt = String.format(act.getString(R.string.lib_sav), (String)msg.obj);           // Get Toast message
            AppLib.showTst(act, R.drawable.lib_sav, txt, null);                                     // Inform that the log file was saved
        } else {                                                                                    // Error occurred
            AppLib.showErrTst(act, R.string.lib_err_log, 0);                                        // Inform that the log file was not saved
        }
        return true;                                                                                // Indicate that this event was processed
    }
}
