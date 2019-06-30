package com.st.libsec;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Provides the name of a certified Qi charger with extended power profile
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
public class WpcQiId {

    private static ArrayList<WpcQiId> sQiIdTable;                                                   // Table of certified Qi charger with extended power profile

    private final           int     mQiid;                                                          // Qi ID of the certified Qi charger
    private final @NonNull  String  mMan;                                                           // Manufacturer name of the certified Qi charger
    private final @NonNull  String  mName;                                                          // Product name of the certified Qi charger

    /**
     * Initialises a new certified  Qi charger descriptor
     *
     * @param qiid  The Qi-ID of the certified Qi charger
     * @param man   The manufacturer name of the certified Qi charger
     * @param name  The product name of the certified Qi charger
     */
    private WpcQiId(final int qiid, final @NonNull String man, final @NonNull String name) {
        mQiid = qiid;                                                                               // Set the Qi-ID of the certified Qi charger
        mMan  = man;                                                                                // Set the manufacturer name of the certified Qi charger
        mName = name;                                                                               // Set the product name of the certified Qi charger
    }

    /**
     * Returns the name of the certified Qi charger
     *
     * @param qiid  The Qi-ID of the certified Qi charger
     * @return      The name of the certified Qi charger
     */
    public static @NonNull String getName(final int qiid) {
        loadQiId();                                                                                 // Load the Qi-ID table
        for(WpcQiId entry: sQiIdTable) {                                                            // Repeat for all Qi-ID table entries
            if (entry.mQiid <= qiid) {                                                              // Certified Qi-charger may be found?
                if (entry.mQiid == qiid) {                                                          // Certified Qi-charger found?
                    return entry.mName;                                                             // Return the product name of the certified Qi charger
                }
                break;                                                                              // Terminate the search
            }
        }
        return "Qi ID-" + qiid;                                                                     // Return Qi-ID number
    }

    /**
     * Returns the name of the certified Qi charger
     *
     * @param qiid  The Qi-ID of the certified Qi charger
     * @param ptmc  The Power Transmitter Manufacturer Code
     * @return      The name of the certified Qi charger
     */
    static @NonNull String getName(final int qiid, final int ptmc) {
        loadQiId();                                                                                 // Load the Qi-ID table
        for(WpcQiId entry: sQiIdTable) {                                                            // Repeat for all Qi-ID table entries
            if (entry.mQiid <= qiid) {                                                              // Certified Qi-charger may be found?
                if (entry.mQiid == qiid) {                                                          // Certified Qi-charger found?
                    return entry.toString();                                                        // Return the name of the certified Qi charger
                }
                break;                                                                              // Terminate the search
            }
        }
        final @NonNull String str = WpcCrt.sRes.getString(R.string.qi_chg_qiid) + qiid;             // Get Qi-Id number name
        return str + WpcCrt.sRes.getString(R.string.lib_from) + WpcMan.getMan(ptmc);                // Return the name of the unknwon certified Qi charger
    }

    /**
     * Loads the Qi ID table
     */
    private static void loadQiId() {
        if (sQiIdTable == null) {                                                                   // Qi ID table not already loaded?
            sQiIdTable = new ArrayList<>();                                                         // Create the Qi ID table
            final @NonNull File fil = new File (WpcCrt.sDir, WpcPtx.PATH_EMU + "ptx_epp.csv");      // Get the PTMC file
            String line;                                                                            // One file text line
            try {
                final @NonNull FileInputStream fis = new FileInputStream(fil);                      // Get the Qi ID file
                final @NonNull BufferedReader br = new BufferedReader(new InputStreamReader(fis));  // Create the file reader
                while ((line = br.readLine()) != null) {                                            // Repeat for all file text lines
                    int len = line.indexOf(AppLib.TAB);                                             // Get the position of first tabulator
                    int qiid = Integer.parseInt(line.substring(0, len));                            // Get the Qi ID of the certified Qi charger
                    int ofs = len + 1;                                                              // Continue with manufacturer name
                    len = line.indexOf(AppLib.TAB, ofs);                                            // Get the position of second tabulator
                    String man = line.substring(ofs, len);                                          // Get the manufacturer name of the certified Qi charger
                    String type = line.substring(len + 1);                                          // Get the type name of the certified Qi charger
                    sQiIdTable.add(new WpcQiId(qiid, man, type));                                   // Add the certified Qi charger descriptor
                }
            } catch (IOException | IndexOutOfBoundsException err) {                                 // An error occurred (should never happen)
                Dbg.log("Cannot load the Qi ID table", err);                                        // Log the error
            }
        }
    }

    /**
     * Returns the name of the certified Qi charger
     *
     * @return  The name of the certified Qi charger
     */
    @Override public @NonNull String toString() {
        return mName + WpcCrt.sRes.getString(R.string.lib_from) + mMan;                             // Return the name of the certified Qi charger
    }
}