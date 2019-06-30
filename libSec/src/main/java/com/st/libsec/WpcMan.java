package com.st.libsec;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * WPC Manufacturer code class
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
public class WpcMan {

    /** Example manufacturer code */
    static final int ACME_MAN = 0xCACA;

    /** Error manufacturer code */
    static final int ERR_MAN = -1;

    /** Manufacturer code for Root Certificate */
    public static final int ROOT_MAN = 0xFFFF;

    private static ArrayList<WpcMan> sMan;                                                          // PTMC table

    private final short     mCod;                                                                   // Manufacturer code
    private final String    mNam;                                                                   // Manufacturer name

    /**
     * Initialize a WpC Manufacturere entry
     *
     * @param cod   The Manufacturer Code
     * @param nam   The manufacturer name
     */
    private WpcMan (final int cod, final @NonNull String nam) {
        mCod = (short)cod;                                                                          // Set the manufacturer code
        mNam = nam;                                                                                 // Set the manufacturer name
    }

    /**
     * Returns the Manufacturer name for a given manufacturer code
     *
     * @param   cod The given Manufacturer code
     * @return  The manufacturer name or null
     */
    static @NonNull String getMan(final int cod) {
        if (sMan == null) {                                                                         // No PTMC table loaded yet?
            sMan = new ArrayList<>();                                                               // Create the PTMC table
            final @NonNull File fil = new File (WpcCrt.sDir, WpcPtx.PATH_EMU + "ptmc.csv");         // Get the PTMC file
            String line;                                                                            // One file text line
            try {
                final @NonNull FileInputStream fis = new FileInputStream(fil);                      // Get the PTMC file
                final @NonNull BufferedReader br = new BufferedReader(new InputStreamReader(fis));  // Create the file reader
                while ((line = br.readLine()) != null) {                                            // Repeat for all file text lines
                    int len = line.indexOf(AppLib.TAB);                                             // Get the position of first tabulator
                    String man = line.substring(0, len);                                            // Get the manufacturer name
                    int ptmc = Integer.parseInt(line.substring(len + 3), AppLib.BAS_HEX);           // Get the PTMC
                    sMan.add(new WpcMan(ptmc, man));                                                // Add the manufacturer descriptor
                }
            } catch (IOException | IndexOutOfBoundsException err) {                                 // An error occurred (should not happen)
                Dbg.log("Cannot load the PTMC table", err);                                         // Log the error
            }
        }
        for (WpcMan man : sMan) {                                                                   // Repeat for all WPC manufacturer code entries
            int mc = man.mCod & AppLib.SHT_UNS;                                                     // Get the manufacturer code
            if (mc >= cod) {                                                                        // Manufacturer code search finished?
                if (mc == cod) {                                                                    // Manufacturer code found?
                    return man.mNam;                                                                // Return the manufacturer code
                }                                                                                   // No Manufacturer code found
                break;                                                                              // Terminate the manufacturer search
            }
        }                                                                                           // No Manufacturer code found
        return WpcCrt.sRes.getString(R.string.qi_ukn_man);                                          // Return no manufacturer code
    }
}