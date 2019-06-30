package com.st.libsec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Generic library class for apps
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
public class AppLib {

    /** Base number for hex integer */
    public static final int BAS_HEX = 16;

    /** Number of bits per byte */
    public static final int BIT_BYT = 8;

    /** Size of a byte value in number of bytes */
    public static final int BYT_SIZ = 1;

    /** Mask to convert a byte into an unsigned integer */
    public  static final int BYT_UNS = 0xFF;

    /** US-ASCII charset (7-bit: 128 chars: 33 not printable + 95 printable) */
    public static final Charset CHR_ASC = Charset.forName("US-ASCII");

    /** ISO-8859-1 charset (8-bit: 256 chars: US-ASCII + 96 additional printable chars) */
    public static final Charset CHR_ISO = Charset.forName("ISO-8859-1");

    /** Empty byte array */
    public static final byte[] NO_BA = {};

    /** Empty String */
    public static final String NO_STR = "";

    /** Display option for action bar for standard configuration */
    public static final int OPT_STD = 0;

    /** Size of a short value in number of bytes */
    public static final int SHT_SIZ = 2;

    /** Mask to convert a short into an unsigned integer */
    public static final int SHT_UNS = 0xFFFF;

    /** Tabulator character */
    public static final char TAB = '\t';

    private static final byte[] TABLE   = {                                                         // Character conversion table
            0x2E, 0x51, 0x51, 0x56, 0x7F, 0x26, 0x41, 0x6F, 0x3C, (byte)0xBB, (byte)0xAC, 0x36, (byte)0xA4, (byte)0xB6, 0x7C, 0x2D,
            0x3E, 0x3C, 0x7C, 0x7C, (byte)0xB6, (byte)0xA7, 0x7F, 0x7C, 0x7C, 0x7C, 0x3E, 0x3C, (byte)0xAC, 0x2D, 0x5E, 0x76,
            0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
            0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
            0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
            0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F,
            0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F,
            (byte)0xC7, (byte)0xFC, (byte)0xE9, (byte)0xE2, (byte)0xE4, (byte)0xE0, (byte)0xE5, (byte)0xE7, (byte)0xEA, (byte)0xEB, (byte)0xE8, (byte)0xEF, (byte)0xEE, (byte)0xEC, (byte)0xC4, (byte)0xC5,
            (byte)0xC9, (byte)0xE6, (byte)0xC6, (byte)0xF4, (byte)0xF6, (byte)0xF2, (byte)0xFB, (byte)0xF9, (byte)0xFF, (byte)0xD6, (byte)0xDC, (byte)0xA2, (byte)0xA3, (byte)0xA5, 0x50, 0x66,
            (byte)0xA0, (byte)0xA1, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5, (byte)0xA6, (byte)0xA7, (byte)0xA8, (byte)0xA9, (byte)0xAA, (byte)0xAB, (byte)0xAC, (byte)0xAD, (byte)0xAE, (byte)0xAF,
            (byte)0xB0, (byte)0xB1, (byte)0xB2, (byte)0xB3, (byte)0xB4, (byte)0xB5, (byte)0xB6, (byte)0xB7, (byte)0xB8, (byte)0xB9, (byte)0xBA, (byte)0xBB, (byte)0xBC, (byte)0xBD, (byte)0xBE, (byte)0xBF,
            (byte)0xC0, (byte)0xC1, (byte)0xC2, (byte)0xC3, (byte)0xC4, (byte)0xC5, (byte)0xC6, (byte)0xC7, (byte)0xC8, (byte)0xC9, (byte)0xCA, (byte)0xCB, (byte)0xCC, (byte)0xCD, (byte)0xCE, (byte)0xCF,
            (byte)0xD0, (byte)0xD1, (byte)0xD2, (byte)0xD3, (byte)0xD4, (byte)0xD5, (byte)0xD6, (byte)0xD7, (byte)0xD8, (byte)0xD9, (byte)0xDA, (byte)0xDB, (byte)0xDC, (byte)0xDD, (byte)0xDE, (byte)0xDF,
            (byte)0xE0, (byte)0xE1, (byte)0xE2, (byte)0xE3, (byte)0xE4, (byte)0xE5, (byte)0xE6, (byte)0xE7, (byte)0xE8, (byte)0xE9, (byte)0xEA, (byte)0xEB, (byte)0xEC, (byte)0xED, (byte)0xEE, (byte)0xEF,
            (byte)0xF0, (byte)0xF1, (byte)0xF2, (byte)0xF3, (byte)0xF4, (byte)0xF5, (byte)0xF6, (byte)0xF7, (byte)0xF8, (byte)0xF9, (byte)0xFA, (byte)0xFB, (byte)0xFC, (byte)0xFD, (byte)0xFE, (byte)0xFF
    };

    private static final int    BUF_SIZ = 1024;                                                     // File buffer size [byte]

    /**
     * Thread to enable/disable an application component
     */
    private static class SetCmp extends Thread {
        private final @NonNull Class<?> mCmp;                                                       // The application component
        private final @NonNull Context  mCtx;                                                       // The app context
        private final          boolean  mMod;                                                       // The setting mode (true = enable, false = disable)

        /**
         * Initialise the thread to enable/disable an application component
         *
         * @param ctx   The app context
         * @param cmp   The application component
         * @param mod   The setting mode (true = enable, false = disable)
         */
        private SetCmp(final @NonNull Context ctx, final @NonNull Class<?> cmp, final boolean mod) {
            mCtx = ctx;                                                                             // Set the app context
            mCmp = cmp;                                                                             // Set the application component
            mMod = mod;                                                                             // Set the Setting mode
        }

        /**
         * Enable/disable the application component
         */
        @Override public void run() {
            final @NonNull PackageManager pm = mCtx.getPackageManager();                            // Get the package manager
            final @NonNull ComponentName cn = new ComponentName(mCtx, mCmp);                        // Get the component name of the application component
            final int sts;                                                                          // The new state of the application component
            if (mMod) {                                                                             // Application component shall be enabled
                sts = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;                               // Set the new state to enable the application component
            } else {                                                                                // Application component shall be disabled
                sts = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;                              // Set the new state to disable the application component
            }
            pm.setComponentEnabledSetting(cn, sts, PackageManager.DONT_KILL_APP);                   // Dis-/enable the application component
        }
    }

    /**
     * Copy all asset files into external files
     *
     * @param path  The asset file paths
     * @param name  The asset file name
     * @param am    The asset manager
     * @param file  The file header of the external file
     */
    public static void copyAssets(final @NonNull String path, final @NonNull String name, final @NonNull AssetManager am, final @NonNull File file) {
        InputStream     in  = null;                                                                 // Asset file data
        OutputStream    out = null;                                                                 // External file data
        try {
            final @Nullable String[] list = am.list(path);                                          // Get asset file
            if (list != null) {                                                                     // Exists the asset file?
                if (list.length == 0) {                                                             // No directory?
                    in = am.open(path);                                                             // Open asset file
                    out = new FileOutputStream(file);                                               // Open external file
                    byte[] buffer = new byte[BUF_SIZ];                                              // Creater buffer
                    int read;                                                                       // Read flag
                    while ((read = in.read(buffer)) !=-1) {                                         // Repeat for whole asset file
                        out.write(buffer, 0, read);                                                 // Copy the asset file
                    }
                } else {                                                                            // Asset directory
                    if (!file.mkdir()) {                                                            // Create external directory
                        Dbg.log("Cannot create directory");                                         // Log error
                    }
                    for (String sub : list) {                                                       // Repeat for all files of asset directory
                        copyAssets(path + File.separator + sub, sub, am, new File(file, sub));      // Copy the asset file
                    }
                }
            }
        } catch(IOException err) {                                                                  // Communication error occurred
            Dbg.log("Cannot copy asset file", err);                                                 // Log error
        } finally {
            if (in != null) {                                                                       // Asset file opened?
                try { in.close(); }                                                                 // Close the asset file
                catch (IOException err) {                                                           // Error occurred (should never happen)
                    Dbg.log("Cannot close asset file", err);                                        // Log error
                }
            }
            if (out != null) {                                                                      // External file opened?
                try {
                    out.flush();                                                                    // Store remeining data
                    out.close();                                                                    // Close external file
                } catch (IOException err) {                                                         // Error occurred
                    Dbg.log("Cannot close file", err);                                              // Log error
                }
            }
        }
    }

    /**
     * Convert a given BCD byte array into an integer
     * @param   bcd The BCD byte array
     * @return  The value of the byte array
     */
    public static int bcdToInt(@NonNull byte[] bcd) {
        int val = 0;                                                                                // Value of the byte array
        for (byte byt: bcd) {                                                                       // Repeat for all bytes of the byte array
            val = ((byt & 0xF0) >> 4) + 10 * val;                                                   // Add digit of high nibble
            val = val * 10 + (byt & 0x0F);                                                          // Add digit of low nibble
        }
        return val;                                                                                 // Return the value of the byte array
    }

    public static @NonNull byte[] intToBcd(int val, int siz) {
        final byte[] bcd = new byte[siz];
        for(int ind = siz - 1; ind >= 0; ind--) {
            int ten = val / 10;
            bcd[ind] = (byte)((val % 10) | (ten % 10) << 4);
            val = ten / 10;
        }
        return bcd;
    }

    /**
     * Enables or disables an application component.
     * @param ctx   The context of the application
     * @param cmp   The application component class which shall be enabled or disabled
     * @param mod   true: the application component will be enabled, otherwise disabled
     */
    static public void enableComponent(final @NonNull Context ctx, final @NonNull Class<?> cmp, boolean mod) {
        new SetCmp(ctx, cmp, mod).start();                                                          // Enable/Disable application component
    }

    /**
     * Returns the attribute color for a given fragment according its actual used theme
     *
     * @param ctx   The context for which the color is searched
     * @param col   The attribute color resource identifier (R.attr.xxx)
     * @return      The color
     */
    public static int getCol(@NonNull Context ctx, @AttrRes int col) {
        TypedValue val = new TypedValue();                                                          // Color
        ctx.getTheme().resolveAttribute(col, val, true);                                            // Get color
        return val.data;                                                                            // Return color
    }

    /**
     * Shows a byte array
     * @param ba    The byte array
     * @return      The string showing the byte array
     */
    public static String showByt(final @NonNull byte[] ba) {
        final @NonNull byte[] txt = new byte[ba.length];                                            // String showing the byte array
        for (int ind = 0; ind < ba.length; ind++) {                                                 // Repeat for all bytes
            txt[ind] = TABLE[ba[ind] & BYT_UNS];                                                    // Get character of this byte
        }
        return new String(txt, CHR_ISO);                                                            // Retun string showing the byte array
    }

    /**
     * Show a toast with warning
     * @param act   The activity showing the toast
     * @param tit   The resource identifier of the error title
     * @param des   The resource identifier of the error description
     * @return      The shown toast
     */
    public static Toast showErrTst(Activity act, int tit, int des) {
        String msg = AppLib.NO_STR;
        if (des != 0) {
            msg = act.getString(des);
        }
        return showTst(act, R.drawable.lib_err, act.getString(tit), msg);                           // Show the error toast
    }

    /** Show a toast
     * @param act   The activity showing the toast
     * @param ico   The resource identifier for the icon
     * @param tit   The title of the toast
     * @param des   The description of the toast
     * @return      The showed toast
     */
    public static Toast showTst(Activity act, int ico, String tit, String des) {
        @SuppressLint("InflateParams")                                                              // The toast loayout must not have a root
                View vw = act.getLayoutInflater().inflate(R.layout.lib_tst, null);                          // Create the layout for the toast
        Drawable icn = act.getResources().getDrawable(ico);                                         // Get icon
        ImageView iv = vw.findViewById(R.id.lib_ico);                                               // Get icon view object
        iv.setImageDrawable(icn);                                                                   // Set icon
        ((TextView)vw.findViewById(R.id.lib_tit)).setText(tit);                                     // Set the title
        if (des != null) {                                                                          // Description available?
            ((TextView) vw.findViewById(R.id.lib_txt_des)).setText(des);                            // Set the description
        } else {                                                                                    // No description available
            vw.findViewById(R.id.lib_txt_des).setVisibility(View.GONE);                             // Hide description
        }
        Toast tst = new Toast(act);                                                                 // Create the taost
        tst.setDuration(Toast.LENGTH_LONG);                                                         // Set the showing time
        tst.setMargin(0f, 0f);                                                                      // Set distance to screen
        tst.setGravity(Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0);                             // Set toast at the bottom of screen
        tst.setView(vw);                                                                            // Set the layout
        tst.show();                                                                                 // Show the toast
        return tst;                                                                                 // Return the toast
    }

    /** Show a toast
     * @param ctx   The activity showing the toast
     * @param lay   The resource identifier for the icon
     * @param tit   The title of the toast
     * @return      The showed toast
     */
    public static Toast shwTst(@NonNull Context ctx, int lay, @NonNull String tit) {
        LayoutInflater li = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  // Get Layoutinflater of device
        //noinspection ConstantConditions                                                           // Layoutinflater will always available
        @SuppressLint("InflateParams")                                                              // The toast loayout must not have a root
                View vw = li.inflate(lay, null);                                                            // Create the layout for the toast
        ((TextView)vw.findViewById(R.id.lib_tit)).setText(tit);                                     // Set the title
        return shwTst(ctx, vw);                                                                     // Return the toast
    }
    /** Show a toast
     * @param ctx   The activity showing the toast
     * @param lay   The resource identifier for the icon
     * @param tit   The title of the toast
     * @return      The showed toast
     */
    public static Toast shwTst(@NonNull Context ctx, int lay, int tit, int des) {
        LayoutInflater li = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  // Get Layoutinflater of device
        @SuppressLint("InflateParams") final @NonNull View vw = li.inflate(lay, null);              // Create the layout for the toast
        ((TextView)vw.findViewById(R.id.lib_txt_tit)).setText(tit);                                 // Set the title
        ((TextView)vw.findViewById(R.id.lib_txt_des)).setText(des);                                 // Set the title
        return shwTst(ctx, vw);                                                                     // Return the toast
    }

    private static Toast shwTst(@NonNull Context ctx, @NonNull View view) {
        Toast tst = new Toast(ctx);                                                                 // Create the toast
        tst.setDuration(Toast.LENGTH_LONG);                                                         // Set the showing time
        tst.setMargin(0f, 0f);                                                                      // Set distance to screen
        tst.setGravity(Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0);                             // Set toast at the bottom of screen
        tst.setView(view);                                                                          // Set the layout
        tst.show();                                                                                 // Show the toast
        return tst;                                                                                 // Return the toast
    }

}
