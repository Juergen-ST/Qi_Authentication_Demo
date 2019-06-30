package com.st.libsec;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Class to debug code
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
public class Dbg {

    /** No error */
    static final int NO_ERR = 0;

    private static final int    CALL_TSK    = 3;                                                    // Index to find the calling method inside the task stack
    private static final String KEY_MSG     = "KEY_MSG";                                            // Parameter key string for dialog message
    private static final String TAG         = "Dbg";                                                // Tag for dialogs
    private static final String VAL_SEP     = " - ";                                                // Separator string for values
    private static final char[] HEX         = "0123456789ABCDEF".toCharArray();                     // Char array to convert values into hexadecimal strings

    /**
     * Hide the default constructor to prevent instantiation of this class
     */
    private Dbg() {}

    /**
     * Convert a given byte array into a string with hexadecimal values
     * @param   val Byte array which shall be shown as a string with hexadecimal values
     * @return  String with hexadecimal values
     */
    public static @NonNull String seqHex(@NonNull String msg, byte[] val) {
        int len = val.length;
        char[] str = new char[len * 2];
        int pos;
        int byt;
        for (int ind = 0; ind < len; ind ++) {                                                      // Repeat for all bytes of the byte array
            pos = ind * 2;
            byt = val[ind] & AppLib.BYT_UNS;
            str[pos] = HEX[(byt & 0xF0) >> 4];
            str[pos + 1] = HEX[byt & 0x0F];
        }
        return msg + new String(str);                                                               // Return string with hexadecimal values
    }

    /**
     * Fatal error dialog fragment
     */
    public static class FatDlg extends DialogFragment {

        /**
         * Called when the error dialog fragment is created
         * Creates the error dialog box for app information dialog fragment
         *
         * @param savedInstanceState Parameter bundle for the dialog box (not used here as this dialog box need no parameter)
         * @return Returns the error dialog box
         */
        @Override
        public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
            @SuppressWarnings("ConstantConditions") final @NonNull Activity act = getActivity();    // Get the host activity
            AlertDialog.Builder builder = new AlertDialog.Builder(act);                             // Create dialog builder
            builder.setIcon(R.drawable.lib_fat);                                                    // Set icon of error dialog
            builder.setTitle(R.string.lib_fat_err);                                                 // Set the title of the error dialog
            //noinspection ConstantConditions
            builder.setMessage(getArguments().getString(KEY_MSG));                                  // Set message of the error dialog
            FinDlg fin = new FinDlg(act);                                                           // Create listener for finish and back button
            builder.setPositiveButton(R.string.lib_finish, fin);                                    // Register Finish button of fatal error dialog
            builder.setOnKeyListener(fin);                                                          // Register key event handler
            return builder.create();                                                                // Create alert dialog for app information dialog fragment
        }
    }

    /**
     * Class to close the activity of a dialog box after a certain delay
     */
    private static class FinHnd extends Handler {

        /**
         * Will be called to close the activity of the dialog box
         * @param msg   The message containing the reference to the activity of the dialog box
         */
        @Override
        public void handleMessage(Message msg) {
            ((Activity) msg.obj).finish();                                                          // Close the activity of the dialog box
        }
    }

    /**
     * Class to listen to Finish and back button
     */
    private static class FinDlg implements DialogInterface.OnClickListener, DialogInterface.OnKeyListener {

        private Activity mAct;                                                                      // Activity of the dialog box

        /* Creates the listener for the finish and back button
         * @param act   The activity of the dialog box
         */
        private FinDlg(Activity act) {
            mAct = act;                                                                             // Remember the activity of the dialog box
        }

        /**
         * Called when Finish button was pressed.
         * Closes the activity of the dialog box.
         * @param dialog    The dialog box (not used here)
         * @param which     additional information for pressed button (not used)
         */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mAct.finish();                                                                           // Close the activity of the dialog box
        }

        /**
         * Called when a key is pressed
         * @param dialog    The dialog box (not used here)
         * @param keycode   The pressed key code
         * @param keyEvent  The key event (not used here)
         * @return  true if back key was pressed otherwise false
         */
        @Override
        public boolean onKey(DialogInterface dialog, int keycode, KeyEvent keyEvent) {
            if (keycode == KeyEvent.KEYCODE_BACK) {                                                 // Back key is pressed?
                mAct.finish();                                                                      // Close the activity of the dialog box
                return true;                                                                        // Inform that this event was consumed
            }
            return false;                                                                           // Inform that this event was not consumed
        }
    }

    /**
     * Return the app description
     * @param   ctx The app context
     * @param   ver The version name
     * @param   dbg The debug flag
     * @return  The app description
     */
    public static String getApp(Context ctx, String ver, boolean dbg) {
        if (dbg) {                                                                                  // Debug version?
            ver = ver + "-\u03b2";                                                                  // Add beta version
        }
        return getNam(ctx) + "  " + ver;                                                            // Return app description
    }

    /**
     * Return the app build date
     * @param   cod The version code
     * @param   tim The build time stamp
     * @return  The app build date
     */
    public static String getBld(int cod,  long tim) {
        final @NonNull SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.UK);        // Define date format
        return "Version code: " + cod + " - " + sdf.format(new Date(tim));                          // Return the build state the application
    }

    /**
     * Return the app name
     * @param   ctx The app context
     * @return  The app name
     */
    private static String getNam(Context ctx) {
        return (String)ctx.getPackageManager().getApplicationLabel(ctx.getApplicationInfo());       // Return app name
    }

    /**
     * Convert a given byte into a string with hexadecimal values
     * @param   byt Byte which shall be shown as a string with hexadecimal values
     * @return  String with hexadecimal values
     */
    private static String hexStr(byte byt) {
        char[] str = new char[2];                                                                   // Create char array for hexadecimal values
        str[0] = HEX[(byt & 0xF0)>> 4];                                                             // Set higher nibble
        str[1] = HEX[byt & 0x0F];                                                                   // Set lower nibble
        return new String(str);                                                                     // Return string with hexadecimal values
    }

    /**
     * Convert a given byte array into a string with hexadecimal values
     * @param   val Byte array which shall be shown as a string with hexadecimal values
     * @return  String with hexadecimal values
     */
    public static String hexStr(byte[] val) {
        return hexStr(val, ':');                                                                    // Return string with hexadecimal values
    }

    /**
     * Convert a given byte array into a string with hexadecimal values
     * @param   val Byte array which shall be shown as a string with hexadecimal values
     * @return  String with hexadecimal values
     */
    private static String hexStr(byte[] val, char sep) {
        int len = val.length;
        char[] str = new char[len * 3 - 1];
        int pos;
        int byt;
        for (int ind = 0; ind < len; ind ++) {                                                      // Repeat for all bytes of the byte array
            pos = ind * 3;
            byt = val[ind] & AppLib.BYT_UNS;
            str[pos] = HEX[(byt & 0xF0) >> 4];
            str[pos + 1] = HEX[byt & 0x0F];
            if (ind < len - 1) {
                str[pos + 2] = sep;
            }
        }
        return new String(str);                                                                     // Return string with hexadecimal values
    }

    /**
     * Convert a given byte array into a string with hexadecimal values
     * @param   nam The name of the byte array
     * @param   val Byte array which shall be shown as a string with hexadecimal values
     * @return  String with hexadecimal values
     */
    public static String hexStr(String nam, byte[] val) {
        return nam + ": " + hexStr(val, ' ');                                                         // Return string with hexadecimal values
    }

    /**
     * Log a debug message
     *
     * @param msg   Debug message
     */
    public static void log(@NonNull String msg) {
        Log.d(Thread.currentThread().getStackTrace()[CALL_TSK].toString(), msg + '!');              // Log the debug message
    }

    /**
     * Log an error message
     *
     * @param msg   Error message
     * @param err   The error which shall be logged
     */
    public static void log(String msg, Throwable err) {
        Log.e(Thread.currentThread().getStackTrace()[CALL_TSK].toString(), msg, err);               // Log the error message
    }

    /**
     * Returns a log string for a given value
     *
     * @param val   Logged value
     * @param msg   Log message
     */
    static @NonNull String logStr(byte val, @NonNull String msg) {
        return hexStr(val) + VAL_SEP + msg;                                                         // Return the log string
    }

    /**
     * Returns a log string for a given byte array
     *
     * @param val   Logged value
     * @param msg   Log message
     */
    static @NonNull String logStr(byte[] val, @NonNull String msg) {
        return hexStr(val) + VAL_SEP + msg;                                                         // Return the log string
    }

    /**
     * Shows the dialog box
     * @param act   The activity where the dialog shall be shown
     * @param msg   The resource identifier for the dialog message
     * @param dlg   The dialog fragment which shall be shown
     */
    private static DialogFragment showDlg(Activity act, int msg, DialogFragment dlg) {
        Bundle arg = new Bundle();                                                                  // Create argument list
        arg.putString(KEY_MSG, act.getString(msg));                                                 // Set dialog message
        dlg.setArguments(arg);                                                                      // Set the arguments for the  dialog
        dlg.show(((AppCompatActivity)act).getSupportFragmentManager(), TAG);                        // Show dialog
        return dlg;                                                                                 // Close dialog after a delay
    }

    /**
     * Shows the fatal error dialog and logs the error
     * @param act   The activity where the fatal error dialog shall be shown
     * @param msg   The resource identifier for the fatal error message
     * @param err   The thrown error
     */
    static void showFat(Activity act, int msg, Throwable err) {
        String des = act.getResources().getResourceName(msg);                                       // Get the resource name of the error message
        Log.wtf(Thread.currentThread().getStackTrace()[CALL_TSK].toString(), des, err);             // Log the fatal error message
        showDlg(act, msg, new FatDlg()).setCancelable(false);                                       // Show the fatal error dialog
        Message hm = new Message();                                                                 // Create Message for handler
        hm.obj = act;
        new FinHnd().sendMessageDelayed(hm, ClsHnd.TIM_DEL);                                        // Send a message delayed to close the dialog box
    }
}
