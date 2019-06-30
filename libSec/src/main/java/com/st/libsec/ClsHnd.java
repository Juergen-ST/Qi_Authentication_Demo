package com.st.libsec;

import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;

/**
 * Class to close a dialog fragment after a certain delay
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
public class ClsHnd extends Handler {

    public static final int        TIM_DEL = 5000;                                                 // Maximum time to show dialog

    /**
     * Closes a dialog fragment latest after a certain time
     * @param dlg   The dialog fragment which shall be closed after a certain time
     */
    public static void clsDel(DialogFragment dlg) {
        Message msg = new Message();                                                                // Create Message for handler
        msg.obj = dlg;                                                                              // Add dialog fragment as a parameter to the message
        new ClsHnd().sendMessageDelayed(msg, TIM_DEL);                                              // Send a message delayed to close the dialog box
    }

    /**
     * Will be called to close the dialog box
     * @param msg   The message containing the reference to the dialog box
     */
    @Override
    public void handleMessage(Message msg) {
        DialogFragment dlg = ((DialogFragment) msg.obj);                                            // Get dialog fragment
        if (dlg.isResumed()) {                                                                      // Dialog Fragment still visible?
            dlg.dismiss();                                                                          // Close the dialog box
        }
    }
}

