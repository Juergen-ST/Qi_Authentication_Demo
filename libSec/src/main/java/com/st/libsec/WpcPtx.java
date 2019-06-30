package com.st.libsec;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ListFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Arrays;

/**
 * WTx Thread to handle the NFC communication with the remote PRx
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
public class WpcPtx extends Thread implements Handler.Callback {

    /** Directory to emulate Ptx */
    public static final String DIR_EMU = "emu";

    /** Path name for the Directory to emulate Ptx */
    static final String PATH_EMU = DIR_EMU + File.separator;

    private static final byte   CLA     = 0x00;                                                     // Class byte for Qi Authentication APDUs

    /** Command APDU header for GET_DATA */
    static final byte[] GET_HD = {CLA, (byte)0xCA, 0x00, 0x00};

    /** Command APDU header for PUT_DATA */
    static final byte[] PUT_HD = {CLA, (byte)0xDA, 0x00, 0x00};

    /** Command APDU header for SELECT AID */
    static final byte[] SEL_HD = {0x00, (byte)0xA4, 0x04, 0x00};

    /** Length of status word */
    static final int LEN_SW = AppLib.SHT_SIZ;

    private static final byte[] AID     = {(byte)0xA0, 0x00, 0x00, 0x00, (byte)0x96, (byte)0xF0, (byte)0xFF, 0x7F, 0x01};
    private static final int    MSG_ERR = 1;                                                        // Error message identifier
    private static final int    MSG_OK  = 0;                                                        // Message identifier
    private static final int    MSK_SW1 = 0xFF00;                                                   // Mask for SW1
    private static final int    MSK_SW2 = AppLib.BYT_UNS;                                           // Mask for SW2
    private static final int    NO_SW   = 0;                                                        // Value for no status word

    /** Certificate Chain of actual emulated PTx device */
    public static WpcCrtChn sChn;

    /** Private key of actual emulated PTx device */
    public static PrivateKey sPrv;

    private final @NonNull IsoDep       mCom;                                                       // ISO-DEP communication interface
    private final @NonNull ListFragment mFrg;                                                       // App context
    private final @NonNull Handler      mHnd;                                                       // Listener to show the communication log
    private final @NonNull File         mLog;                                                       // Proposed log file name
    private final @NonNull ShwHnd       mShw;                                                       // Listener to show the results

    /**
     * Generates the thread to perform the Qi Authentication via NFC
     *
     * @param   tag The detected NFC card
     * @param   frg The list fragment where the communication log shall be shown
     */
    public WpcPtx(final @NonNull Tag tag, final @NonNull ListFragment frg, final @NonNull File log) {
        mCom = IsoDep.get(tag);                                                                     // Get the ISO-DEP communication interface
        mHnd = new Handler(this);                                                                   // Register the List fragment showing the communication log
        mFrg = frg;                                                                                 // Register list fragment
        mLog = log;                                                                                 // Set proposed log file name
        //noinspection ConstantConditions                                                           // Main activity is here always available
        mShw = new ShwHnd(frg.getActivity());                                                       // Register handler to show Qi Authentication results
    }

    /**
     * The handler class to show the result of the Qi Authentication
     */
    private static class ShwHnd extends Handler {

        /**
         * Initialize the handler to show the result of the Qi Authentication
         *
         * @param   ctx The app context
         */
        private ShwHnd (@NonNull Context ctx) {
            super(new ShwRes(ctx));                                                                 // Initialize the handler to show the result of the Qi Authentication
        }

        /**
         * Shows an error message
         *
         * @param   tit The resource identifier of the error title
         * @param   des The resource identifier of the error description
         */
        private void shwErr(@StringRes int tit, @StringRes int des) {
            Message msg = obtainMessage(MSG_ERR);                                                   // Obtain the error message
            msg.arg1 = tit;                                                                         // Set the error title
            msg.arg2 = des;                                                                         // Set the error description
            sendMessage(msg);                                                                       // Show the error message
        }

        /**
         * Shows the successful Qi Authentication
         *
         * @param   chn The WPC Certificate chain of the remote device
         */
        private void shwRes(@Nullable WpcCrtChn chn) {
            Message msg = obtainMessage(MSG_OK);                                                    // Obtain the information message
            msg.obj = chn;                                                                          // Set the WPC Certificate chain
            sendMessage(msg);                                                                       // Show the result of the Qi Authentication
        }

    }

    /**
     *  The listener class to show the result of the Qi Authentication
     */
    private static class ShwRes implements Handler.Callback {

        private final Context mCtx;                                                                 // The app context

        /**
         * Initialize the listener to show the result of the Qi Authentication
         *
         * @param   ctx The app context
         */
        private ShwRes(Context ctx) {
            mCtx = ctx;                                                                             // Register the app context
        }

        /**
         * Called when a new message shall be shown.
         * Shows the given message
         *
         * @param   msg The given message
         * @return  true to indicate that this event was processed
         */
        @Override public boolean handleMessage(Message msg) {
            if (msg.what == MSG_OK) {                                                               // Successful Qi Authentication?
                WpcCrtChn chn = (WpcCrtChn)msg.obj;                                                 // Get the WPC Certificate Chain
                String txt;                                                                         // Text message
                if (chn != null) {                                                                  // WPC Certificate chain received?
                    txt = chn.toString() + mCtx.getString(R.string.qi_chg_prx);                     // Set text message with device name
                } else {                                                                            // No WPC Certificate Chain
                    txt = mCtx.getString(R.string.qi_chg_dev);                                      // Set standard text message
                }
                AppLib.shwTst(mCtx, R.layout.tst_ok, txt);                                          // Show text message
            } else {                                                                                // An error occurred
                AppLib.shwTst(mCtx, R.layout.tst_err, msg.arg1, msg.arg2);                          // Show error message
            }
            return true;                                                                            // Return information that this event was processed
        }
    }

    /**
     * Exception for received wrong status word
     */
    private class SwExp extends IOException {

        /**
         * Initialize the exception for received wrong status word
         */
        private SwExp() {
            super();                                                                                // Initialize exception for received wrong status word
        }
    }

    /**
     * Create a case 3 Command APDU
     *
     * @param head  The Command APDU header
     * @param dat   The command data
     * @return      The Command APDU
     * @throws IOException when an error occurred during Command APDU creation (should never occur)
     */
    private @NonNull ByteArrayOutputStream getCmd(@NonNull byte[] head, @NonNull byte[] dat) throws IOException{
        ByteArrayOutputStream cmd=new ByteArrayOutputStream(head.length+AppLib.BYT_SIZ+dat.length); // Create the Command APDU
        cmd.write(head);                                                                            // Set Command header
        cmd.write(dat.length);                                                                      // Set Lc
        cmd.write(dat);                                                                             // Set command data
        return cmd;                                                                                 // Return the Command APDU
    }

    /**
     * Send the GET DATA command to receive a Qi Authentication message
     *
     * @param   le  The length of the expected Qi Authentication message
     * @return  The Qi Authentication message
     * @throws  IOException when an NFC communication error occurred
     */
    private @NonNull byte[] getDat(int le) throws IOException {
        ByteArrayOutputStream cmd = new ByteArrayOutputStream(GET_HD.length + AppLib.BYT_SIZ);      // Create the Command APDU
        cmd.write(GET_HD);                                                                          // Set Command header
        cmd.write(le);                                                                              // Set Le
        final @NonNull byte[] apdu = mCom.transceive(cmd.toByteArray());                            // Send the GET DATA command to the PRx
         final int len = apdu.length - LEN_SW;                                                      // Get the length of the Qi Authentication message
        if ((len <= 0) || (len > le) || (getSw(apdu) != WpcPrx.SW_OK)) {                            // Incorrect length of Response APDU or unexpected status word?
            throw new SwExp();                                                                      // Generate communication error
        }
        return Arrays.copyOf(apdu, apdu.length - LEN_SW);                                           // Return the Qi Authentication message
    }

    /**
     * Returns the status word of the Response APDU
     *
     * @param   res The Response APDU
     * @return  The status word of the Response APDU
     */
    private int getSw(@NonNull byte[] res) {
        final int len = res.length;                                                                 // Get the length of the Response APDU
        if (len < LEN_SW) {                                                                         // Response APDU too small?
            return NO_SW;                                                                           // Return no status word
        }                                                                                           // Response APDU is large enough
        return ((res[len-LEN_SW]&AppLib.BYT_UNS)<< 8) | (res[len-AppLib.BYT_SIZ] & AppLib.BYT_UNS); // Return the status word of the Response APDU
    }

    /**
     * Called when the WPC communication log is finished
     *
     * @param   msg Handler message (not used here)
     * @return  true to indicate that this event was processed
     */
    @Override public boolean handleMessage(Message msg) {
        WpcLog.endLog(mFrg, mLog);                                                                  // Log abortion of Qi Authentication
        return true;                                                                                // Inform that this event was processed
    }

    /**
     * Sends a PUT DATA command with a Qi Authentication message
     *
     * @param   res The Qi authentication message
     * @return  The length of the responded Qi authentication message
     * @throws  IOException when an NFC communication error occurs
     */
    private int putDat(@Nullable byte[] res) throws IOException {
        if (res == null) {                                                                          // No Authentication message received?
            return 0;                                                                               // Return no Qi authentication message
        }
        ByteArrayOutputStream cmd = getCmd(PUT_HD, res);                                            // Create PUT DATA command
        byte[] apdu = cmd.toByteArray();                                                            // Convert Command APDU
        apdu = mCom.transceive(apdu);                                                               // Send the PUT DATA command
        int sw = getSw(apdu);                                                                       // Get returned status word
        if (sw == WpcPrx.SW_OK) {                                                                   // Normal ending status word?
            return 0;                                                                               // Terminate NFC communication
        }
        if ((sw & MSK_SW1) != WpcPrx.SW_DAT) {                                                      // Unexpected status word?
            throw new IOException();                                                                // Generate NFC communication error
        }                                                                                           // Expected status word
        return sw & MSK_SW2;                                                                        // Returns the length of the responded Qi Authentication message
    }

    /**
     * Thread to manage the NFC communication with the remote PRx
     */
    public void run() {
        setPriority(Thread.MAX_PRIORITY);                                                           // Set this thread to maximum priority
        try {
            WpcLog.begLog(sChn.toString() + " starts Qi Authentication");                           // Start the WPC communication log
            mCom.connect();                                                                         // Connect NFC communication
            ByteArrayOutputStream cmd = getCmd(SEL_HD, AID);                                        // Create the SELECT AID Command APDU
            cmd.write((byte)0);                                                                     // Set Le
            byte[] apdu = cmd.toByteArray();                                                        // Convert Command APDU
            apdu = mCom.transceive(apdu);                                                           // Send the SELECT AID command
            int sw = getSw(apdu);                                                                   // Get the status word
            if ((sw & MSK_SW1) == WpcPrx.SW_DAT) {                                                  // WPC PRx wants to start a WPC authentication?
                WpcAthRsp rsp = new WpcAthRsp(sChn, sPrv, true);                                    // Initialize WPC Authentication Responder
                int len = sw & AppLib.BYT_UNS;                                                      // Get length of first WPC Authentication request message
                while (len > 0) {                                                                   // Repeat until no more WPC Authentication request messages are available
                    len = putDat(rsp.athReq(getDat(len)));                                          // Execute the WPC Authentication request message
                }
            } else {                                                                                // Another status word received
                throw new SwExp();                                                                  // Throw exception
            }
            WpcLog.log(WpcLog.EvtTyp.CMT, "Qi Authentication finished".getBytes(AppLib.CHR_ISO));   // Log finished Qi Authentication
            mShw.shwRes(null);                                                                      // Show end of NFC operation
        } catch (SwExp err) {                                                                       // An error occurred during the authentication of PRx
            mShw.shwErr(R.string.qi_fak_prx, R.string.qi_buy);                                      // Inform about fake device
        } catch (IOException err) {                                                                 // A communication error occurred during the authentication of PRx
            WpcLog.logErr("Communication error");                                                   // Log abortion of Qi Authentication
            mShw.shwErr(R.string.lib_err_com, R.string.lib_try);                                    // Inform about communication loss
        }
        try {
            mCom.close();                                                                           // Close the communication with the remote PRx
        } catch (IOException err) {                                                                 // Error occurred during closing the communication
            Dbg.log("Cannot close the communication with the remote PRx", err);                     // Log error
        }
        mHnd.sendEmptyMessage(0);                                                                   // Inform about finished WPC communication
    }
}