package com.st.libsec;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ListFragment;
import android.view.Display;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

import static com.st.libsec.WpcAthIni.FlwTyp.CACH;

/**
 * Service to emulate a WPC PRx
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
@SuppressLint("NewApi")                                                                             // To allow also compilation for devices prior Android Kitkat
public class WpcPrx extends HostApduService implements Handler.Callback, WpcCom {

    /** Status word indicating available data */
    public static final int SW_DAT = 0x6200;

    /** Status word for normal ending */
    public static final int SW_OK = 0x9000;

    /** WPC PTx cach buffer */
    public static final CachBuf sCach = new CachBuf(4);

    /** Actual Qi Authentication protocol flow */
    public static WpcAthIni.FlwTyp sFlw = CACH;

    /** Handler to show the communication log */
    public static @Nullable ListFragment sFrg;

    private static final int    MIN_LEN = 5;                                                        // Minimum Command APDU length
    private static final int    OFS_CLA = 0;                                                        // Offset for class byte inside of Command APDU
    private static final int    OFS_INS = 1;                                                        // Offset for instruction byte inside of Command APDU
    private static final int    OFS_P1  = 2;                                                        // Offset for parameter 1 byte inside of Command APDU
    private static final int    OFS_P2  = 3;                                                        // Offset for parameter 2 byte inside of Command APDU
    private static final int    OFS_P3  = 4;                                                        // Offset for parameter 3 byte inside of Command APDU
    private static final int    SW_LE   = 0x6400;                                                   // Status word indicating error with available data
    private static final int    SW_PAR  = 0x6B00;                                                   // Status word indicating wrong P1/P2 parameter
    private static final int    SW_LC   = 0x6C00;                                                   // Status word indicating wrong LC parameter
    private static final int    SW_INS  = 0x6D00;                                                   // Status word indicating wrong instruction code
    private static final int    SW_CLA  = 0x6E00;                                                   // Status word indicating wrong class byte
    private static final int    SW_ERR  = 0x6F00;                                                   // Status word indicating generic error
    private         Handler     mHnd;                                                               // Handler to show errors
    private         Semaphore   mLck;                                                               // Lock flag to wait for the next PUT DATA command
    private         byte[]      mMsg;                                                               // Response message for the next GET DATA command
    public static   String      sName;                                                              // Identified PTx name
    private         WpcCrtChn   mPtx;                                                               // The Certification chain received from remote PTx

    /**
     * Exception class for unexpected errors in received Command APDUs to generate error status words
     */
    private class SwExp extends IOException {

        private int mSw;                                                                            // Error status word

        /**
         * Creates an exception due to an identified command APDU error
         *
         * @param   sw  The status word describing the identified command error
         */
        private SwExp(int sw) {
            mSw = sw;                                                                               // Remember the error status word
        }

        /**
         * Returns the error status word
         *
         * @return  The error status word
         */
        private int getSw() {
            return mSw;                                                                             // Return the error status word
        }
    }

    /**
     * Checks the CLA and P1 of the Command APDU
     *
     * @param head  The expected header bytes
     * @param apdu  The received Command APDU
     * @throws SwExp In case the Command header does not match
     */
    private void chkSel(@NonNull byte[] head, @NonNull byte[] apdu) throws SwExp {
        if (apdu.length < MIN_LEN) {                                                                // Command APDU is too small?
            throw new SwExp(SW_ERR);                                                                // Generate error status word
        }
        if (head[OFS_CLA] != apdu[OFS_CLA]) {                                                       // Incorrect class byte?
            throw new SwExp(SW_CLA);                                                                // Generate error status word
        }
        if (head[OFS_P1] != apdu[OFS_P1]) {                                                         // Incorrect parameter 1?
            throw new SwExp(SW_PAR);                                                                // Generate error status word
        }
    }

    /**
     * Checks the header of the Command APDU
     *
     * @param head  The expected header bytes
     * @param apdu  The received Command APDU
     * @throws SwExp In case the Command header does not match
     */
    private void chkCmd(@NonNull byte[] head, @NonNull byte[] apdu) throws SwExp {
        chkSel(head, apdu);                                                                         // Check class byte and parameter P1
        if (head[OFS_P2] != apdu[OFS_P2]) {                                                         // Incorrect parameter P2
            throw new SwExp(SW_PAR);                                                                // Generate error status word
        }
    }

    /**
     * Called when a new message shall be shown.
     * Shows the given message
     *
     * @param   msg The given message
     * @return  true to indicate that this event was processed
     */
    @Override public boolean handleMessage(Message msg) {
        if (msg.what == WpcAthIni.NO_ERR) {                                                         // No error detected?
            swScrOn();                                                                              // Switches the screen on
            if (mPtx != null) {                                                                     // Certification chain received from remote PTx?
                final @NonNull String dev =  mPtx.toString() + getString(R.string.qi_chg_ptx);      // Get PTx device name
                AppLib.shwTst(this, R.layout.tst_ok, dev);                                          // Shows the successful Qi authentication
                sName = WpcQiId.getName(mPtx.getPu().getQiId());                                    // Set log file name
            } else {
                AppLib.shwTst(this, R.layout.tst_ok, getString(R.string.qi_chg_dev));               // Shows the successful Qi authentication
                sName = getString(R.string.lib_suc);                                                // Set log file name
            }
        } else {                                                                                    // Error was detected
            shwErr(msg.what, msg.arg1);                                                             // Show erroneous product error
        }
        if (sFrg != null) {                                                                         // Prx emulation fragment shown?
            final @NonNull File dir = new File(getExternalFilesDir(null), WpcPtx.DIR_EMU);          // Get emulation directory
            WpcLog.endLog(sFrg, new File(dir, "R_" + sName));                                       // Stops communication log
        }
        return true;
    }

    /**
     * Called when the PTx emulation is finished
     *
     * @param reason    Reason for finishing the Qi Authentication application
     */
    @Override public void onDeactivated(int reason) {
        mMsg = null;                                                                                // Clear the message buffer for the GET DATA command
        mLck.release();                                                                             // Unlock the Qi Authentication Initiator on PRx
    }

    /**
     * Called when a new Command APDU for the Qi Authentication application was received
     *
     * @param apdu      The Command APDU
     * @param extras    Additional information (not used here)
     * @return          The Response APDU
     */
    @Override public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        ByteBuffer rsp;                                                                             // Response APDU
        byte ins = apdu[OFS_INS];                                                                   // Get instruction byte
        try {
            if (ins == WpcPtx.SEL_HD[OFS_INS]) {                                                    // SELECT command received?
                WpcLog.begLog("PRx starts Qi Authentication");                                      // Log start of Qi Authentication
                chkSel(WpcPtx.SEL_HD, apdu);                                                        // Check command header
                mHnd = new Handler(this);                                                           // Start handler to show result message
                new WpcAthIni(this, sFlw, sCach).start();                                           // Start Qi Authentication Initiator on PRx
                mLck = new Semaphore(0);                                                            // Initialize lock flag
                return null;                                                                        // Wait for the first Authentication Request message from the Qi Authentication Initiator
            } else if (ins == WpcPtx.GET_HD[OFS_INS]) {                                             // GET DATA command received?
                chkCmd(WpcPtx.GET_HD, apdu);                                                        // Check command header
                if ((mMsg == null) || (apdu.length != MIN_LEN)) {                                   // No message available or illegal command length?
                    throw new SwExp(SW_ERR);                                                        // Return error status word
                }
                int le = apdu[OFS_P3] & AppLib.BYT_UNS;                                             // Get length of expected response data
                if ((le < mMsg.length) && (le != 0)) {                                              // Requested response data are too low?
                    throw new SwExp(SW_LE | mMsg.length);                                           // Return error status word informing about the available message bytes
                }
                rsp = ByteBuffer.wrap(new byte[mMsg.length + WpcPtx.LEN_SW]);                       // Create Response APDU
                rsp.put(mMsg);                                                                      // Add response data
                rsp.putShort((short) SW_OK);                                                        // Add status word for normal ending
                mMsg = null;
            } else if (ins == WpcPtx.PUT_HD[OFS_INS]) {                                             // PUT DATA command received?
                chkCmd(WpcPtx.GET_HD, apdu);                                                        // Check command header
                if (mMsg != null) {                                                                 // An old message is still pending?
                    throw new SwExp(SW_LE | mMsg.length);                                           // Return error status word informing about the available message bytes
                }
                int lc = apdu[OFS_P3] & AppLib.BYT_UNS;                                             // Get length of command data
                if (lc == 0) {                                                                      // Communication aborted by PTx?
                    WpcLog.logErr("Qi Authentication aborted by PTx");                              // Stops communication log
                    shwErr(R.string.qi_fak_ptx, R.string.qi_buy);                                   // Show erroneous product error
                    throw new SwExp(SW_OK);                                                         // Respond with normal ending status word
                }
                if (lc != apdu.length - MIN_LEN) {                                                  // Illegal length of command data?
                    throw new SwExp(SW_LC);                                                         // Generate error status word
                }
                mMsg = Arrays.copyOfRange(apdu, MIN_LEN, apdu.length);                              // Get authentication message
                mLck.release();                                                                     // Unlock the command execution at the Qi Authentication Initiator on PRx
                return null;                                                                        // Wait for the next Authentication Request message from the Qi Authentication Initiator
            } else {                                                                                // Unknown command received
                rsp = statword(SW_INS);                                                             // Return the error status word
            }
        } catch (SwExp err) {                                                                       // Command error identified
            rsp = statword(err.getSw());                                                            // Return the error status word
        }
        return rsp.array();                                                                         // Return the Response APDU
    }

    /**
     * Returns the status word
     *
     * @param sw    The status word to be returned
     * @return      The Response APDU with the status word
     */
    private ByteBuffer statword(int sw) {
        ByteBuffer res = ByteBuffer.wrap(new byte[AppLib.SHT_SIZ]);                                 // Create Response APDU for status word
        res.putShort((short)sw);                                                                    // Add status word
        return res;                                                                                 // Return Response APDU
    }

    /**
     * Turns on the screen when the screen is switched off
     */
    private void swScrOn() {
        Context ctx = getBaseContext();                                                             // Get the app context
        DisplayManager dm = (DisplayManager)ctx.getSystemService(Context.DISPLAY_SERVICE);          // Get the display manager
        if (dm.getDisplay(Display.DEFAULT_DISPLAY).getState() == Display.STATE_OFF) {               // Screen is switched off?
            ctx.startActivity(new Intent(ctx, WakAct.class));                                       // Turn on the screen
        }
    }

    /**
     * Provides the received WPC Certification chain of the remote device
     *
     * @param   chn The WPC Certification Chain
     */
    public void setChn(@NonNull WpcCrtChn chn) {
        mPtx = chn;                                                                                 // Remember the WPC Certification Chain
    }

    /**
     * Shows an error
     *
     * @param   tit Error title
     * @param   des Error description
     */
    private void shwErr(int tit, int des) {
        swScrOn();                                                                                  // Switches the screen on
        if (tit == R.string.qi_fak_ptx) {                                                           // Fake device identified?
            sName = "Fake";                                                                         // Set device name
        } else {                                                                                    // Other error identified
            sName = "Error";                                                                        // Set device name
        }
        AppLib.shwTst(getBaseContext(), R.layout.tst_err, tit, des);                                // Shows the error
    }

    /**
     * Sends the Qi Authentication message request to the remote TRx
     *
     * @param   req The Qi Authentication Request message
     * @return  The Qi Authentication Response message
     * @throws  IOException when no Qi Authentication Response message
     */
    @Override public @NonNull byte[] sndMsg(@NonNull byte[] req) throws IOException {
        byte[] sw = statword(SW_DAT | req.length).array();                                          // Set the status word
        sendResponseApdu(sw);                                                                       // Send status word back indicating the length of available message
        mMsg = req;                                                                                 // Set the message buffer
        try {
            mLck.acquire();                                                                         // Wait for PUT DATA command
        } catch (InterruptedException err) {                                                        // Qi Authentication Initiator of PRx was interrupted
            WpcLog.logErr("Authentication Initiator at PRx was interrupted!");                      // Log error reason
            mMsg = null;                                                                            // Clear the message buffer
        }
        if (mMsg == null) {                                                                         // No Qi Authentication message received?
            WpcLog.logErr("No Qi Authentication Response message received!");                       // Log error reason
            throw new IOException();                                                                // Throw error to indicate that no Qi Authentication Response message was received
        }
        return mMsg;                                                                                // Return the Qi Authentication Response message
    }

    /**
     * Terminates the Qi Authentication
     *
     * @param   err The error text
     * @param   des The error description
     */
    @Override public void endAuth(final @StringRes int err, final @StringRes int des) {
        sendResponseApdu(statword(SW_OK).array());                                                  // Send status word back
        mMsg = AppLib.NO_BA;                                                                        // Set message for the following GET_DATA command
        mHnd.sendMessage(mHnd.obtainMessage(err, des, 0));                                          // Terminate the Qi Authentication
    }
}