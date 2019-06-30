package com.st.libsec;

import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class to manage WPC data files
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
public class WpcFil extends BinFil {

    /** Extension of text files */
    static final String EXT_TXT = ".txt";

    private byte[]  mDat;                                                                           // Data of NFC file

    /**
     * Creates a WPC File object
     * @param fil   The WPC File header data
     */
    public WpcFil(File fil) {
        super(fil);                                                                                 // Creates a WPC file object
    }

    /**
     * Creates a WPC File object
     * @param dir   The directory where the WPC file is located
     * @param nam   The name of the WPC file
     */
    public WpcFil(File dir, String nam) {
        super(new File(dir, nam.endsWith(EXT_TXT)? nam : nam + EXT_TXT));                           // Creates a WPC file object
    }

    /**
     * Creates a WPC File object
     * @param fil   The WPC File header data
     * @param dat   The WPC file data
     */
    public WpcFil(File fil, byte[] dat) {
        super(fil);                                                                                 // Creates a NFC file object
        mDat = dat;                                                                                 // Remember NFC file data
    }

    /**
     * Reads the WPC file
     * This methods is blocking and should not be called from the UI thread
     *
     * @return The WPC file data
     * @throws IOException in case the WPC file cannot be read
     */
    @Override public byte[] read() throws IOException {
        mDat = StrToByt(new String(super.read(), AppLib.CHR_ASC));                                  // Read the WPC file
        return mDat;                                                                                // Returns the WPC file data
    }

    /**
     * Saves the WPC file
     * This methods is blocking and should not be called from the UI thread
     *
     * @param   dat The WPC file data
     * @throws  IOException in case the WPC file cannot be saved
     */
    public void save(final @NonNull byte[] dat) throws IOException {
        FileOutputStream fos = null;                                                                // Initialize the file output stream
        //noinspection TryFinallyCanBeTryWithResources                                              // Not usable below Android 19
        try {
            fos = new FileOutputStream(mFil);                                                       // Create file
            fos.write(dat);                                                                         // Add WPC file data
        } finally {                                                                                 // Execute in any case
            if (fos != null) {                                                                      // NFC file created?
                fos.close();                                                                        // Close NFC file
            }
        }
    }

    /**
     * Converts a string into byte array
     *
     * @param   str The string to be converted
     * @return  The byte array of the string
     */
    private byte[] StrToByt(String str) {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();                                    // Byte array stream
        int old = 0;                                                                                // Origin offest
        int ofs = str.indexOf(':');                                                                 // Actual offset
        if (ofs == 0) {                                                                             // String starts with colon?
            old = 1;                                                                                // Start after the colon
            ofs = str.indexOf(':', old);                                                            // Get new offset
        }
        while (ofs >= 0) {                                                                          // Repeat until no colons found
            try {
                int byt = Integer.parseInt(str.substring(old, ofs).trim(), AppLib.BAS_HEX);         // Get one byte
                bas.write(byt & AppLib.BYT_UNS);                                                    // Add byte
            } catch (NumberFormatException ignored) {}                                              // Ignore errors
            old = ofs + 1;                                                                          // Update origin offset
            ofs = str.indexOf(':', old);                                                            // Set new offset
        }
        if (old < str.length() - 1) {                                                               // Remaining characters available
            try {
                int byt = Integer.parseInt(str.substring(old).trim(), AppLib.BAS_HEX);              // Get one byte
                bas.write(byt & AppLib.BYT_UNS);                                                    // Add byte
            } catch (NumberFormatException ignored) {}                                              // Ignore errors
        }
        return bas.toByteArray();                                                                   // return byte array of the string
    }
}
