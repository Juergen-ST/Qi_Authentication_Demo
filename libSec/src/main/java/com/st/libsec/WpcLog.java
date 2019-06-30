package com.st.libsec;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.File;
import java.nio.BufferUnderflowException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * WPC Qi Authentication Protocol log
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
public class WpcLog {

    /** Tabulator string */
    static final String TAB = "    ";

    private static final String         EXT_LOG = "-Log" + WpcFil.EXT_TXT;                          // Extensions for log files
    private static ArrayList<TimEvt>    sLst;                                                       // List of time events
    private static String               sApp;                                                       // App description
    private static String               sBld;                                                       // Build information

    /** Event types */
    public enum EvtTyp {
        CMT,                                                                                        // Comment
        CHN,                                                                                        // Certificate Chain
        CRT,                                                                                        // Certificate
        ERR,                                                                                        // Error
        REQ,                                                                                        // NFC command Request
        RES,                                                                                        // NFC Command Response
    }

    private enum TimTyp{                                                                            // Time types
        TOT,                                                                                        // Total time
        EVT,                                                                                        // Event time
        MSG,                                                                                        // Message event time
    }

    /**
     * Listener for long click events
     */
    private static class LngLst implements NameDlg.NameDlgLst, ListView.OnItemLongClickListener {

        private final @NonNull ListFragment mFrg;                                                   // Hosting list fragment
        private final @NonNull File         mDir;                                                   // Proposed file
        private final @NonNull String       mName;                                                  // Log file name

        /**
         * Create the listener for long clicked log list
         *
         * @param frg   The hosting list fragment
         * @param log   The proposed log file
         */
        private LngLst(final @NonNull ListFragment frg, final @NonNull File log) {
            mFrg = frg;                                                                             // Remember the hosting fragment
            mDir = log.getParentFile();                                                             // Remember the proposed log file
            mName = log.getName();                                                                  // Remember the log file name
        }

        /**
         * Called when the name dialog is closed
         *
         * @param name  The entered name
         */
        @Override public void onCls(final @NonNull String name) {
            new LogFile(new File(mDir, name), mFrg).start();                                        // Save the log file
        }

        /**
         * Called when a new log file name was entered
         *
         * @param name  The entered log file name
         * @return      true if the log file name name exists already otherwise false
         */
        @Override public boolean hasName(final @NonNull String name) {
            return new File(mDir, name).exists();                                                   // Inform if log file name exists
        }

        /** Called when the log list was clicked for a long time
         * Store the log file
         *
         * @param parent    Log list view of the log list
         * @param view      Selected item view (not used here)
         * @param position  Position of selected item (not used here)
         * @param id        Identifier of selected item (not used here)
         * @return          True to indicate that this event was processed
         */
        @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            //noinspection ConstantConditions                                                       // Fragmentmanager will be here always available
            final @NonNull FragmentManager fm = mFrg.getFragmentManager();                          // Get the Fragment manager
            final @NonNull String nam = mName + EXT_LOG;                                            // Set full log file name
            final int pos = mName.length();                                                         // Get the cursor position
            NameDlg.showDlg(R.string.lib_sav_log, R.string.lib_log_nam, nam, pos, R.string.lib_ovr_log, fm, this);
            return true;
        }
    }

    /**
     * Thread to generate the WPC log list
     */
    private static class LogGen extends Thread implements Handler.Callback {

        private final @NonNull  ListFragment    mFrg;                                               // The list fragment hosting the log list
        private final @NonNull  Handler         mHnd;                                               // Handler for finished log list
        private final @NonNull  File            mLog;                                               // Proposed file name

        /**
         * Initialise thread to generate WPC log list
         *
         * @param frg   The list fragment hosting the WPC log list
         * @param log   The proposed log file
         */
        private LogGen(final @NonNull ListFragment frg, final @NonNull File log) {
            mFrg = frg;                                                                             // Set the list fragment
            mHnd = new Handler(this);                                                               // Generate the handler for finished log list
            mLog = log;                                                                             // Set the proposed log file
        }

        /**
         * Called when the communication log is received
         *
         * @param   msg Handler message with the communication log
         * @return  true to indicate that this event was processed
         */
        @Override public boolean handleMessage(Message msg) {
            mFrg.setListAdapter((ListAdapter)msg.obj);                                              // Show the communication log
            mFrg.getListView().setOnItemLongClickListener(new LngLst(mFrg, mLog));
            return true;                                                                            // Inform that this event was processed
        }

        /**
         * Generates the WPC log list
         */
        public @Override void run() {
            final @NonNull ArrayList<String> log = new ArrayList<>();                               // Create logLen event list
            log.add(sApp);                                                                          // Get app description
            log.add(sBld);                                                                          // Add build information
            log.add("Device: " + Build.MANUFACTURER + " " + Build.MODEL);                           // Log device name
            log.add("Android " + Build.VERSION.RELEASE);                                            // Log Android version
            log.add(WpcCrt.SPEC_VER);                                                               // Log Qi Authentication specification version
            @SuppressWarnings("ConstantConditions") final @NonNull Context ctx = mFrg.getContext(); // Get fragment context
            final ArrayAdapter apt = new ArrayAdapter<>(ctx, R.layout.lib_lst_itm, getlog(log));    // Set list
            mHnd.sendMessage(mHnd.obtainMessage(0, apt));                                           // Send adapter to the handler
            sLst = null;                                                                            // Delete actual list
        }
    }

    /**
     * Time event
     */
    private static class TimEvt {
        private final           long      mTim;                                                     // Time stamp of the time event
        private final           EvtTyp    mTyp;                                                     // Time event type
        private final @NonNull  byte[]    mDat;                                                     // Data of the time event

        /**
         * Initialize a new time event
         *
         * @param tim   The time stamp of the time event
         * @param typ   The time event type
         * @param dat   The data of the time event
         */
        private TimEvt(final long tim, final EvtTyp typ, final @NonNull byte[] dat) {
            mTim = tim;                                                                             // Set time stamp of the time event
            mTyp = typ;                                                                             // Set time event type
            mDat = dat;                                                                             // Set the data of the time event
        }
    }

    /**
     * Disable default constructor as this class contains only static methods
     */
    private WpcLog() {}

    /**
     * Starts logging WPC communication
     *
      * @param msg   The message to start the WPC vommunication log
     */
    public static void begLog(final @NonNull String msg) {
        final long tim = System.currentTimeMillis();                                                // Get the actual time stamp
        sLst = new ArrayList<>();                                                                   // Create a new logging list
        sLst.add(new TimEvt(tim, EvtTyp.CMT, msg.getBytes(AppLib.CHR_ISO)));                        // Add the first time event
    }

    /**
     * Initialize the communication logLen
     *
     * @param   app App description
     * @param   bld Build information
     */
    public static void init(final @NonNull String app, final @NonNull String bld) {
        sApp = app;                                                                                 // Set app description
        sBld = bld;                                                                                 // Set build information
    }

    /**
     * Log an event
     *
     * @param typ   The time event type
     * @param dat   The data of the time event
     */
    public static void log(final EvtTyp typ, final @NonNull byte[] dat) {
        if (sLst != null) {                                                                         // Log started?
            sLst.add(new TimEvt(System.currentTimeMillis(), typ, dat));                             // Add a new time event
        }
    }

    /**
     * Log a comment
     *
     * @param msg   The comment message
     */
    public static void logCmt(final @NonNull String msg) {
        log(EvtTyp.CMT, msg.getBytes(AppLib.CHR_ISO));                                              // Log the comment message
    }

    /**
     * Log an error
     *
     * @param msg   The error message
     */
    public static void logErr(final @NonNull String msg) {
        log(EvtTyp.ERR, msg.getBytes(AppLib.CHR_ISO));                                              // Log the error message
    }

    /**
     * Stops the communication log and shows the log at a list fragment
     *
     * @param   frg The List Fragment
     * @param   log The proposed log file name
     */
    public static void endLog(final @NonNull ListFragment frg, final @NonNull File log) {
        new LogGen(frg, log).start();                                                               // Shows the WPC communication log
    }

    /**
     * Returns the WPC communication log list
     *
     * @return  The WPC communication log list
     */
    private static @NonNull ArrayList<String> getlog(final @NonNull ArrayList<String> log) {
        final long[] tim = new long[TimTyp.values().length];                                        // Create timers
        final int cnt = sLst.size();                                                                // Get number of log entries
        int ind = 0;                                                                                // Log event number
        while(ind < cnt) {                                                                          // Repeat for all log entries
            TimEvt evt = sLst.get(ind);                                                             // Get time event
            String msg;                                                                             // Time message
            if (ind == 0) {                                                                         // First time event
                for (int lev = 0; lev < tim.length; lev++) {                                        // Repeat for all timers
                    tim[lev] = evt.mTim;                                                            // Initialise the timer
                }
                SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy  HH:mm:ss  ", Locale.UK);   // Define actual date format
                msg = sdf.format(new Date(evt.mTim));                                               // Set Time stamp
            } else {                                                                                // Not the first event
                int lev;                                                                            // Timer level
                if (ind == cnt - 1) {                                                               // Last event?
                    lev = TimTyp.TOT.ordinal();                                                     // Measure total time
                } else {                                                                            // Not the last event
                    switch (evt.mTyp) {                                                             // Analyze event type
                        case CMT:                                                                   // Comment
                        case ERR:                                                                   // Error
                        case REQ:                                                                   // Command Request
                        case RES:                                                                   // Command Request
                            lev = TimTyp.EVT.ordinal(); break;                                      // User event timer
                        default:                                                                    // NFC Commend events
                            lev = TimTyp.MSG.ordinal();                                             // NFC command timer
                    }
                }
                msg = getTab(lev) + (evt.mTim - tim[lev]) + " ms  ";                                // Set difference time
                tim[lev] = evt.mTim;                                                                // Remember last time stamp
            }
            ind++;                                                                                  // Next log event
            try {
                switch (evt.mTyp) {                                                                 // Analyze event type
                    case CMT:                                                                       // Comment
                        log.add(msg + new String(evt.mDat, AppLib.CHR_ISO));                        // Log the comment
                        break;
                    case CHN:                                                                       // WPC Certificate chain
                        log.add("Certificate Chain");                                               // Show the WPC Certificate Chain log
                        WpcCrtChn.logCrtChn(evt.mDat, log);                                         // Log the WPC Certificate Chain
                        break;
                    case CRT:                                                                       // WPC Certificate
                        log.add("Certificate");                                                     // Show the WPC Certificate Chain log
                        WpcCrt.logCrt(new BytBuf(evt.mDat), 1, log);                                // Log the WPC Certificate Chain
                        break;
                    case ERR:                                                                       // Error
                        log.add(msg + "Error: " + new String(evt.mDat, AppLib.CHR_ISO));            // Log the error
                        break;
                    case REQ:                                                                       // Qi Authentication request message
                        switch (evt.mDat[0] & 0x0F) {                                               // Analyse the message type
                            case 0x09:                                                              // GET_DIGESTS REQUEST
                                log.add(msg + "GET_DIGESTS request");                               // Show the GET_DIGESTS request
                                logGetDig(evt.mDat, log);                                           // Log the GET_DIGESTS request
                                break;
                            case 0x0A:                                                              // GET_CERTIFICATE REQUEST
                                log.add(msg + "GET_CERTIFICATE request");                           // sHOW THE GET_CERTIFICATE request
                                logGetCrt(evt.mDat, log);                                           // Log GET_CERTIFICATE request
                                break;
                            case 0x0B:                                                              // CHALLENGE request
                                log.add(msg + "CHALLENGE request");                                 // Show the CHALLENGE request
                                logChallenge(evt.mDat, log);                                        // Log the CHALLENGE request
                                break;
                            default:                                                                // Unknown request
                                log.add(msg + "Unknown request");                                   // Show the unknown request
                                log.add(Dbg.hexStr(evt.mDat));                                      // Log the unknown request
                        }
                        break;
                    case RES:                                                                       // Qi Authentication response message
                        switch (evt.mDat[0] & 0x0F) {                                               // Analyse the response message type
                            case 0x01:                                                              // DIGESTS response
                                log.add(msg + "DIGESTS response");                                  // Show the DIGESTS response
                                logDig(evt.mDat, log);                                              // Log the DIGESTS response
                                break;
                            case 0x02:                                                              // CERTIFICATE response
                                log.add(msg + "CERTIFICATE response");                              // Show the CERTIFICATE response
                                logCrt(evt.mDat, log);                                              // Log the CERTIFICATE response
                                break;
                            case 0x03:                                                              // CHALLENGE_AUTH response
                                log.add(msg + "CHALLENGE_AUTH response");                           // Show the CHALLENGE_AUTH response
                                logAuth(evt.mDat, log);                                             // Log the CHALLENGE_AUTH response
                                break;
                            case 0x07:                                                              // Error response
                                log.add(msg + "ERROR response");                                    // Show the Error response
                                logErr(evt.mDat, log);                                              // Log the Error response
                                break;
                            default:                                                                // Unknown response
                                log.add(msg + "Unknown response");                                  // Show the unknown response
                                log.add(Dbg.hexStr(evt.mDat));                                      // Log the unknown response
                        }
                        break;
                }
            }
            catch (BufferUnderflowException err) {                                                  // Not enough data available?
                Dbg.log("Not enough data", err);                                                    // Log error
            }
        }
        return log;                                                                                 // Return log list
    }

    private static @NonNull String addHead(final byte head, final @NonNull ArrayList<String> log) {
        final @NonNull String tab = getTab(TimTyp.MSG.ordinal());
        log.add(tab + Dbg.logStr(head, "Authentication Protocol Version | Message Type"));
        return tab;
    }

    /**
     * Logs the GET_DIGESTS request
     *
     * @param req   The GET_DIGESTS request
     * @param log   The log array
     */
    private static void logGetDig(final byte[] req, final @NonNull ArrayList<String> log) {
        final @NonNull BytBuf buf = new BytBuf(req);                                                // Convert request into byte buffer
        final @NonNull String tab = addHead(buf.getByte(), log);                                    // Log header
        log.add(tab + Dbg.logStr(buf.getByte(), "Slots"));                                          // Log slots
    }

    /**
     * Logs the GET_CERTIFICATE request
     *
     * @param req   The GET_CERTIFICATE request
     * @param log   The log array
     */
    private static void logGetCrt(final byte[] req, final @NonNull ArrayList<String> log) {
        final @NonNull BytBuf buf = new BytBuf(req);                                                // Convert request into byte buffer
        final @NonNull String tab = addHead(buf.getByte(), log);                                    // Log header
        final byte hi = buf.getByte();                                                              // Get high bits plus slot number
        log.add(tab + Dbg.logStr(hi, "Ofs/len | Slot " + (hi & 0x0F)));                             // Log slot number
        byte byt = buf.getByte();                                                                   // Get offset
        int val = (hi & 0xC0) >> 6 | (byt & AppLib.BYT_UNS);                                        // Calculate offset
        log.add(tab + Dbg.logStr(byt, "Offset: " + val +" bytes"));                                 // Log offset
        byt = buf.getByte();                                                                        // Get Length
        val = (hi & 0x30) >> 4 | (byt & AppLib.BYT_UNS);                                            // Calculate length
        log.add(tab + Dbg.logStr(byt, "Length: " + val + " bytes"));                                // Log length
    }

    /**
     * Logs the CHALLENGE request
     *
     * @param req   The CHALLENGE request
     * @param log   The log array
     */
    private static void logChallenge(final byte[] req, final @NonNull ArrayList<String> log) {
        final @NonNull BytBuf buf = new BytBuf(req);                                                // Convert request into byte buffer
        final @NonNull String tab = addHead(buf.getByte(), log);                                    // Log the message header
        final byte slt = buf.getByte();                                                             // Get the slot number
        log.add(tab + Dbg.logStr(slt, "Slot " + (slt & 0x0F)));                                     // Log the slot number
        log.add(tab + Dbg.hexStr("Nonce", buf.getArray(16)));                                       // Log the nonce
    }

    /**
     * Logs the DIGESTS response
     *
     * @param req   The DIGESTS response
     * @param log   The log array
     */
    private static void logDig(final byte[] req, final @NonNull ArrayList<String> log) {
        final @NonNull BytBuf buf = new BytBuf(req);                                                // Convert response into byte buffer
        final @NonNull String tab = addHead(buf.getByte(), log);                                    // Log header
        byte slt = buf.getByte();                                                                   // Get the slots
        log.add(tab + Dbg.logStr(slt, "Populated | Returned slots"));                               // Log slots
        slt = (byte)(slt & 0x0F);                                                                   // Get the returned slots
        int msk = 0x01;                                                                             // Slot mask
        for (byte cnt = 0; cnt < 4; cnt++) {                                                        // Repeat for all slots
            if ((msk & slt) != 0) {                                                                 // Slot returned?
                log.add(tab + Dbg.hexStr("Nonce for Slot " + cnt, buf.getArray(16)));               // Log nonce
            }
            msk = msk << 1;                                                                         // Set next slot mask
        }
    }

    /**
     * Logs the Qi Authentication message header
     *
     * @param msg   The Qi Authentication message
     * @param log   The log array
     */
    private static void logCrt(final byte[] msg, final @NonNull ArrayList<String> log) {
        final @NonNull BytBuf buf = new BytBuf(msg);                                                // Convert response into byte buffer
        final @NonNull String tab = addHead(buf.getByte(), log);                                    // Log header
        log.add(tab + Dbg.hexStr("Certificate Chain Segment", buf.getArray(msg.length - 1)));       // Log Certificate Chain Segment
    }

    /**
     * Logs the signature
     *
     * @param crt   The Signature
     * @param tab   The tabulator string
     * @param log   The log array
     */
    static void logSig(final @NonNull BytBuf crt, final @NonNull String tab, final @NonNull ArrayList<String> log) {
        log.add(tab + Dbg.hexStr("Signature r", crt.getArray(WpcKey.KEY_SIZ)));                     // Log signature R
        log.add(tab + Dbg.hexStr("Signature s", crt.getArray(WpcKey.KEY_SIZ)));                     // Log signature S
    }


    /**
     * Logs the CHALLENGE_AUTH response
     *
     * @param req   The CHALLENGE_AUTH response
     * @param log   The log array
     */
    private static void logAuth(final byte[] req, final @NonNull ArrayList<String> log) {
        final @NonNull BytBuf buf = new BytBuf(req);                                                // Convert response into byte buffer
        final @NonNull String tab = addHead(buf.getByte(), log);                                    // Log header
        log.add(tab + Dbg.logStr(buf.getByte(), "Maximum Authentication Protocol version | Slots"));// Log slot
        log.add(tab + Dbg.logStr(buf.getByte(), "Certificate Chain Hasch LSB"));                    // Log Certificate Chain Segment
        logSig(buf, tab, log);                                                                      // Log the signature
    }

    /**
     * Logs the ERROR response
     *
     * @param req   The ERROR response
     * @param log   The log array
     */
    private static void logErr(final byte[] req, final @NonNull ArrayList<String> log) {
        BytBuf buf = new BytBuf(req);                                                               // Convert response into byte buffer
        final @NonNull String tab = addHead(buf.getByte(), log);                                    // Log header
        final int code = buf.getUsByte();                                                           // Get Error Code
        final @NonNull String des;                                                                  // Error description
        switch(code) {                                                                              // Analyse the error code
            case 0x01: des = "Invalid request"; break;                                              // Invalid request error code
            case 0x02: des = "Unsupported protocol"; break;                                         // Unsupported protocol error code
            case 0x03: des = "Busy"; break;                                                         // Busy error code
            case 0x04: des = "Unspecified"; break;                                                  // Unspecified error code
            case 0x08: des = "Excessive retries"; break;                                            // Excessive retries error code
            default:                                                                                // Remaining error codes
                if (code >= 0xF0) {                                                                 // Manufacturer defined error code?
                    des = "Manufacturer defined";                                                   // Show manufacturer defined error code
                } else {                                                                            // Reserved error code
                    des = "Reserved";                                                               // Show reserved error code
                }
        }
        log.add(tab + Dbg.logStr((byte)code, "Error Code: " + des));                                // Log Error Code
        log.add(tab + Dbg.logStr(buf.getByte(), "Error Data"));                                     // Log Error Data
    }

    /**
     * Returns the intent string for a given intent level
     *
     * @param lev   The intent level
     * @return      The intent string
     */
    private static @NonNull String getTab(final int lev) {
        final @NonNull StringBuilder sb = new StringBuilder();                                      // String builder for the intent string
        for (int ind = 0; ind < lev; ind++) {                                                       // repeat for all levels
            sb.append(TAB);                                                                         // Add intent
        }
        return sb.toString();                                                                       // Return the intent string
    }
}