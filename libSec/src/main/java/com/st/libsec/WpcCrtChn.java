package com.st.libsec;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * WPC Certificate Chain class
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
public class WpcCrtChn {

    /** Extensions for WPC Certificate chain files */
    public static final String EXT_CHN = "_chn" + WpcFil.EXT_TXT;

    private static final int    MIN_CRT = 2;                                                        // Minimum number of WPC Certificates inside a Certificate Chain
    private static final int    OFS_LEN = 0;                                                        // Offset of the length field in the Certificate Chain
    private ByteBuffer          mChn;                                                               // Certificate Chain

    /**
     * Create a new WPC Certificate Chain with a given Manufacturer Certificate
     *
     * @param   man The Manufacturer certificate
     */
    WpcCrtChn (@NonNull byte[] dig, @NonNull WpcCrt man) {
        mChn = ByteBuffer.wrap(new byte[AppLib.SHT_SIZ + WpcKey.DIG_SIZ + MIN_CRT*WpcCrt.LEN_CRT]); // Create the Certificate Chain
        mChn.putShort((short)0);                                                                    // Set place holder for Certificate Chain length
        mChn.put(dig);                                                                              // Add the Digest of the root Certificate
        mChn.put(man.getEncoded());                                                                 // Add the Manufacturer Certificate
    }

    /**
     * Create a new WPC Certificate Chain with a given Manufacturer Certificate
     *
     * @param   man The Manufacturer certificate
     */
    public WpcCrtChn (@NonNull WpcCrt man) {
        this(WpcCrt.DIG_CA, man);
    }

    /**
     * Create a new WPC Certificate Chain with a given Manufacturer Certificate
     *
     * @param   man The Manufacturer certificate
     * @param   sec The Secondary certificate
     */
    public WpcCrtChn (@NonNull WpcCrt man, @NonNull WpcCrt sec) {
        mChn = ByteBuffer.wrap(new byte[AppLib.SHT_SIZ + WpcKey.DIG_SIZ + 3 * WpcCrt.LEN_CRT]);     // Create the Certificate Chain
        mChn.putShort((short)0);                                                                    // Set place holder for Certificate Chain length
        mChn.put(WpcCrt.DIG_CA);                                                                    // Add the Digest of the root Certificate
        mChn.put(man.getEncoded());                                                                 // Add the Manufacturer Certificate
        mChn.put(sec.getEncoded());                                                                 // Add the Secondary Certificate
    }

    /**
     * Create a WPC Certificate Chain with the byte array of a given WPC Certificate chain
     *
     * @param   chn The byte array of the WPC Certificate Chain
     */
    public WpcCrtChn (@NonNull byte[] chn) {
        mChn = ByteBuffer.wrap(new byte[chn.length]);                                               // Create Certification chain
        mChn.put(chn);                                                                              // Copy certification chain
    }

    /**
     * Logs the WPC Certificate Chain
     *
     * @param req   The WPC Certificate Chain
     * @param log   The log array
     */
    static void logCrtChn(final byte[] req, final @NonNull ArrayList<String> log) {
        final @NonNull BytBuf buf = new BytBuf(req);                                                // Convert response into byte buffer
        final @NonNull byte[] len = buf.getArray(AppLib.SHT_SIZ);                                   // Get Certificate Chain length
        final int siz = new BytBuf(len).getUsShort();                                               // Calculate the Certificate Chain length
        log.add(WpcLog.TAB + Dbg.logStr(len, "Certificate Chain length: " + siz + " bytes"));       // Log Certificate Chain length
        log.add(WpcLog.TAB + Dbg.hexStr("Root Certificate Hash", buf.getArray(WpcKey.DIG_SIZ)));    // Log Root Certificate Hash
        int pos = 1;                                                                                // Certificate position
        while(buf.rest() > 0) {                                                                     // Repeat for all following certificates
            WpcCrt.logCrt(buf, pos, log);                                                           // Log the certificate
            pos++;                                                                                  // Count certificate position
        }
    }

    /**
     * Add one WPC Certificate to the Certificate chain
     *
     * @param   crt The WPC Certificate to be added
     */
    public void addCrt (@NonNull WpcCrt crt) {
        mChn.put(crt.getEncoded());                                                                 // Add the WPC Certificate to the Certificate Chain
        if ((crt.getTyp() & WpcCrt.TYP_RPU) == WpcCrt.TYP_RPU) {                                    // Product Unit Certificate added?
            mChn.putShort(OFS_LEN, (short)mChn.array().length);                                     // Set the length of the Certificate Chain
        }
    }

    /**
     * Return the WPC Certificate Chain as byte array
     *
     * @return  The WPC Certificate Chain as byte array
     */
    public @NonNull byte[] getChn() {
        return mChn.array();                                                                        // Return the WPC Certificate Chain as byte array
    }

    /**
     * Returns the digest of the Certificate Chain
     *
     * @return  The digest of the Certificate Chain
     */
    public @NonNull byte[] getDig() {
        return WpcKey.getDig(mChn.array());                                                         // Return the Digest of the Certificate Chain
    }

    /**
     * Returns the Product Unit Certificate of the WPC Certificate Chain
     *
     * @return  The Product Unit Certificate
     */
    public @NonNull WpcCrt getPu() {
        final @NonNull byte[] dat = mChn.array();                                                   // Get the WPC Certificate Chain byte buffer
        return new WpcCrt(Arrays.copyOfRange(dat, dat.length - WpcCrt.LEN_CRT, dat.length), 0);     // return the Product Unit Certificate
    }

    /**
     * Returns the Device name of the WPC Certificate Chain
     *
     * @return  The device name of the WPC Certificate Chain
     */
    @Override public @NonNull String toString() {
        final @NonNull byte[] dat = mChn.array();                                                   // Get the WPC Certificate Chain byte array
        final int beg = AppLib.SHT_SIZ + WpcKey.DIG_SIZ;                                            // Calculate offset for Manufacturer Certificate
        final int mid = new WpcCrt(Arrays.copyOfRange(dat, beg, beg + WpcCrt.LEN_CRT), 0).getMan(); // Get Subject of Manufacturer Certificate
        return WpcQiId.getName(getPu().getQiId(), mid);                                             // Return the certified Qi charger name
    }

    /**
     * Verifies the Certificate Chain
     *
     * @throws CertificateException when the Certificate format is incorrect
     * @throws InvalidKeyException when the public keys are invalid
     * @throws SignatureException when the signature of a WPC Certificate is not correct
     */
    public void verify() throws CertificateException, InvalidKeyException, SignatureException {
        mChn.position(0);
        int len = mChn.getShort() & AppLib.SHT_UNS;                                                 // Get the length of the Certificate chain
        if ((len != mChn.array().length) || (len < AppLib.SHT_SIZ + WpcKey.DIG_SIZ)) {              // Certificate Chain length inconsistent or too small?
            WpcLog.logErr("Certificate Chain length is inconsistent or too small!");                // Log error
            throw new CertificateException();                                                       // Return Certificate error
        }
        len = len - AppLib.SHT_SIZ - WpcKey.DIG_SIZ;                                                // Calculate size of stored Certificates
        if (len % WpcCrt.LEN_CRT != 0) {                                                            // Inconsistent Certificate length
            WpcLog.logErr("Certificate Chain length is inconsistent!");                             // Log error
            throw new CertificateException();                                                       // Return Certificate error
        }
        len = len / WpcCrt.LEN_CRT;                                                                 // Calculate numbers of remaining Certificates
        if (len < MIN_CRT) {                                                                        // Not ebnough Certificates stored?
            WpcLog.logErr("Not enough Certificates in the Certificate Chain!");                     // Log error
            throw new CertificateException();                                                       // Return Certificate error
        }
        byte[] dig = new byte[WpcKey.DIG_SIZ];                                                      // Digest of root Certificate
        mChn.get(dig);                                                                              // Get Digest of root Certificate
        if (!Arrays.equals(dig, WpcCrt.DIG_CA)) {                                                   // Unknown root certificate
            WpcLog.logErr("Unkown Root Certificate!");                                              // Log error
            throw new SignatureException();                                                         // Return Signature error
        }
        PublicKey pub = WpcCrt.PUB_CA;                                                              // Get the Public Key of the Root Certificate
        byte[] ba = new byte[WpcCrt.LEN_CRT];                                                       // WPC Certificate
        byte[] id = WpcCrt.getId(WpcCrt.TYP_CRT);                                                   // Get issuer of root certificate
        int man = WpcMan.ERR_MAN;                                                                   // Manufacturer code
        do {
            mChn.get(ba);                                                                           // Get WPC Certificate
            WpcCrt crt = new WpcCrt(ba, man);                                                       // Create WPC Certificate object
            if (man == WpcMan.ERR_MAN) {                                                            // Manufacturer Certificate?
                man = crt.getMan();                                                                 // Get Manufacturer code
                if (man == WpcMan.ERR_MAN) {                                                        // No valid manufacturer code found?
                    WpcLog.logErr("No Manufacturer code found in the Manufacturer certificate!");   // Log error
                    throw new CertificateException();                                               // Return Certificate error
                }
            }
            if (!Arrays.equals(id, crt.getIid())) {                                                 // Incorrect Issuer identifier implemented?
                WpcLog.logErr("Wrong Issuer identifier!");                                          // Log error
                throw new CertificateException();                                                   // Return Certificate error
            }
            id = crt.getSid();                                                                      // Get Subject identifier for the next certificate
            try {
                crt.verify(pub);                                                                    // Verify the WPC Certificate
            } catch (GeneralSecurityException err) {                                                // Signature error occurred
                WpcLog.logErr("Wrong Certificate signature!");                                      // Log error
                throw err;                                                                          // Forward error
            }
            int typ = crt.getTyp();                                                                 // Get WPC Certificate type))
            if (((len > 1) && (typ != WpcCrt.TYP_INT)) || ((len == 1) && (typ != WpcCrt.TYP_TPU))) {// Wrong Certificate type?
                WpcLog.logErr("Wrong Certificate type!");                                           // Log error
                throw new CertificateException();                                                   // Return Certificate error
            }
            pub = crt.getPublicKey();                                                               // Get the public Key of the WPC Certificate
            len--;                                                                                  // Goto next WPC Certificate
        } while (len > 0);                                                                          // Repeat for all WPC Certifcates
    }
}