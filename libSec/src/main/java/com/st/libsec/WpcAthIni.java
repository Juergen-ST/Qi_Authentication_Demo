package com.st.libsec;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * WPC Authentication Initiator class
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
public class WpcAthIni extends Thread {

    /** Qi Authentication Protocol Version */
    static final int ATH_VER = 1;

    /** No error value */
    public static final int NO_ERR  = 0;

    /** Offset for Nonce in CHALLENGE Request */
    public static final int OFS_RND = 2;

    /** Message type for CHALLENGE request */
    static final int REQ_ATH = 0x0B;

    /** Message type for GET_CERTIFICATE request */
    static final int REQ_CRT = 0x0A;

    /** Message type for GET_DIGEST request */
    static final int REQ_DIG = 0x09;

    /** Size for Nonce in CHALLENGE request message */
    public static final int RND_SIZ = 16;

    /** Number for Slot 0 */
    static final byte SLOT_0 = 0;

    /** Mask for Slot 0 */
    static final byte SLOT_MSK = 1;

    /** Protocol flows according section 7 of Qi Authentication Protocol */
    public enum FlwTyp {
        SMPL,                                                                                       // Simple flow according section 7.1 of Qi Authentication Protocol
        CACH,                                                                                       // Flow with caching according section 7.2 of Qi Authentication Protocol
        ATH1                                                                                        // Challenge first flow according section 7.4 of Qi Authentication Protocol
    }

    private static final int        MAX_CRT = 242;                                                  // Maximum length for GET_CERTIFICATE Request to avoid segmentation of RF frames
    private final CachBuf   mCach;                                                                  // WPC Certificate Chain cache
    private final WpcCom    mCom;                                                                   // WPC communication interface
    private final FlwTyp    mFlw;                                                                   // Chosen Qi Authentication flow

    /**
     * Create Qi Authentication Initiator thread
     *
     * @param   com The Qi communication interface
     * @param   flw The used protocol flow
     * @param   buf The cache buffer
     */
    WpcAthIni(@NonNull WpcCom com, FlwTyp flw, @NonNull CachBuf buf) {
        mCom = com;                                                                                 // Register the Qi communication interface
        mFlw = flw;                                                                                 // Register the protocol flow
        mCach= buf;                                                                                 // Register cache buffer
    }

    /**
     * Starts the Qi Authentication Initiator thread
     */
    @Override public void run() {
        setPriority(Thread.MAX_PRIORITY);                                                           // Set this thread to maximum priority
        WpcLog.begLog("PRx starts Qi Authentication");                                              // Log start of Qi Authentication
        try {
            switch (mFlw) {                                                                         // Select the protocol flow
                case SMPL: runSmpl(); break;                                                        // Run the simple flow
                case CACH: runCach(); break;                                                        // Run the flow with caching
                case ATH1: runAth1(); break;                                                        // Run the challenge first flow
            }
            WpcLog.logCmt("Correct signature");                                                     // Log correct signature
            WpcLog.logCmt("Successful Qi Authentication");                                          // Log termination of Qi Authentication
            mCom.endAuth(NO_ERR, NO_ERR);                                                           // Terminate the Qi Authentication
        } catch (GeneralSecurityException err) {                                                    // Communication error occurred
            WpcLog.logErr("Unsuccessful Qi Authentication");                                        // Log termination of the Qi Authentication
            mCom.endAuth(R.string.qi_fak_ptx, R.string.qi_buy);                                     // Terminate the Qi Authentication
        } catch (IOException err) {                                                                 // Communication error occurred
            WpcLog.logErr("Communication error");                                                   // Log communication error
            WpcLog.logErr("Abort Qi Authentication");                                               // Log abort of the Qi Authentication
            mCom.endAuth(R.string.lib_err_com, R.string.lib_try);                                   // Terminate the Qi Authentication
        }
    }

    /**
     * Sends a CHALLENGE request with a given Nonce
     *
     * @param   req The given Nonce
     * @return  The returned Response
     */
    private @NonNull byte[] sndAth(@NonNull ByteBuffer req) throws IOException {
        return sndMsg(req, WpcAthRsp.RES_ATH).array();                                              // Return the received Response
    }

    /**
     * Creates a CHALLENGE request with a Nonce
     *
     * @return  The CHALLENGE request
     */
    public static @NonNull ByteBuffer getAth() {
        ByteBuffer req = getMsg(REQ_ATH, OFS_RND + RND_SIZ);                                        // Create CHALLENGE request
        req.put(SLOT_0);                                                                            // Add Slot Number
        req.put(SafFkt.getRnd(RND_SIZ));                                                            // Add Nonce
        return req;                                                                                 // Return the CHALLENGE request
    }

    /**
     * Returns the WPC Certificate Chain for a given Digest
     *
     * @param   dig The given Digest
     * @return  Returns the WPC Certificate Chain from the cache when the Digest matches, otherwise null
     */
    private @Nullable WpcCrtChn getChn(@NonNull byte[] dig) {
        for (WpcCrtChn chn: mCach) {                                                                // Repeat for all WPC Certificate Chains in the cache
            if (Arrays.equals(chn.getDig(), dig)) {                                                 // WPC Certificate Chain found?
                return chn;                                                                         // return the WPC Certificate Chain
            }
        }                                                                                           // No WPC Certificate Chain found in the cache
        return null;                                                                                // Return no WPC Certificate Chain
    }

    /**
     * Read the Certificate Chain from the remote device
     *
     * @return  The Certificate Chain from the remote device
     * @throws  IOException in case an error occurred
     */
    private @NonNull WpcCrtChn getChn() throws IOException, GeneralSecurityException {
        int siz = MAX_CRT;                                                                          // Initialize the requested length for the GET_CERTIFICATE Request
        int ofs = 0;                                                                                // Initialize the offset for the GET_CERTIFICATE Request
        int len = 0;                                                                                // Initialize length of Certificate Chain
        ByteArrayOutputStream bas = new ByteArrayOutputStream(MAX_CRT);                             // Create stream for Certificate Chain
        do {
            ByteBuffer req = getMsg(REQ_CRT, 4);                                                    // Create GET_CERTIFICATE request message
            req.position(1);                                                                        // Set buffer pointer to offset
            req.put((byte) (((ofs & 0x0300) >> 2) | SLOT_0));                                       // Add slot byte
            req.put((byte) ofs);                                                                    // Add offset
            req.put((byte) siz);                                                                    // Add Length
            ByteBuffer res = sndMsg(req, WpcAthRsp.RES_CRT);                                        // Send GET_CERTIFICATE Request
            if (ofs == 0) {                                                                         // First GET_CERTIFICATE Request?
                len = res.getShort();                                                               // Get the total length of the Certificate Chain
                if (len < siz) {                                                                    // Certificate Chain too small?
                    WpcLog.logErr("Wrong WPC Certificate Chain length");                            // Log error
                    throw new IOException();                                                        // Abort authentication
                }
            }
            byte[] buf = res.array();                                                               // Get CERTIFICATE Response
            if ((siz != (buf.length - 1))) {                                                        // Incorrect Certificate Chain fragment size?
                WpcLog.logErr("Invalid WPC Certificate Chain length");                              // Log error
                throw new IOException();                                                            // Abort authentication
            }
            bas.write(buf, 1, siz);                                                                 // Add Certificate fragment to the Certificate Chain
            len = len - siz;                                                                        // Calculate remaining bytes in the Certificate Chain
            ofs = ofs + siz;                                                                        // Calculate offset for the next GET_CERTIFICATE Request
            if (len > MAX_CRT) {                                                                    // Remaining Certificate Chain does not fit into one GET_CERTIFICATE Request?
                siz = MAX_CRT;                                                                      // Request the maximum fragment for the next GET_CERTIFICATE Request
            } else {                                                                                // Remaining Certificate Chain fits into one GET_CERTIFICATE Request
                siz = len;                                                                          // Request the remaining bytes of the Certificate Chain
            }
        } while (len > 0);                                                                          // Repeat until whole Certificate Chain is received
        byte[] ba = bas.toByteArray();                                                              // Convert WPC Certificate Chain into a byte array
        WpcCrtChn chn = new WpcCrtChn(ba);                                                          // Create the Certificate Chain
        chn.verify();                                                                               // Verify the Certificate Chain
        mCom.setChn(chn);                                                                           // Announce used WPC Certificate Chain
        WpcLog.log(WpcLog.EvtTyp.CHN, ba);                                                          // Log the received WPC Certificate Chain
        return chn;                                                                                 // Return the Certificate Chain
    }


    /**
     * Sends a GET_DIGESTS request
     *
     * @return  The DIGESTS response
     * @throws  IOException in case no valid DIGESTS Response was received
     */
    private @NonNull byte[] getDig() throws IOException {
        ByteBuffer req = getMsg(REQ_DIG, 2);                                                        // Create GET_DIGESTS Request
        req.put(SLOT_MSK);                                                                          // Add Slot mask
        byte[] res = sndMsg(req, WpcAthRsp.RES_DIG).array();                                        // Send GET_DIGESTS Request
        if ((res[1] != ((SLOT_MSK << 4) | SLOT_MSK)) || (res.length != 2 + WpcKey.DIG_SIZ)) {       // Wrong formated DIGESTS Response
            WpcLog.logErr("Wrong formatted DIGESTS Response!");                                     // Log error
            throw new IOException();                                                                // Throw exception
        }
        return Arrays.copyOfRange(res, 2, 2 + WpcKey.DIG_SIZ);                                      // Return the Digest
    }

    /**
     * Returns a Qi Authentication Request template
     *
     * @param   typ The Qi Authentication Request Type
     * @param   len The Length of the Qi Authentication Request
     * @return  The Qi Authentication Request template
     */
    static @NonNull ByteBuffer getMsg(int typ, int len) {
        ByteBuffer res = ByteBuffer.wrap(new byte[len]);                                            // Create Authentication Request template
        res.put((byte)((ATH_VER << 4) | typ));                                                      // Set the Qi-Authentication version and the Request type
        return res;                                                                                 // Return the Authentication Request template
    }

    /**
     * Challenge first Qi Authentication Initiator flow
     *
     * @throws GeneralSecurityException In case the remote device cannot be authenticated
     * @throws IOException              In case a communication error occurred
     */
    private void runAth1() throws GeneralSecurityException, IOException {
        final ByteBuffer msg = getAth();                                                            // Create CHALLENGE request
        final byte[] res = sndAth(msg);                                                             // Send the CHALLENGE Request message
        final byte[] sig = Arrays.copyOfRange(res, WpcAthRsp.LEN_ATH, res.length);                  // Get the signature from the CHALLENGE_AUTH Response
        if (!verify1(msg.array(), res, sig)) {                                                      // Verification of the signature failed?
            final WpcCrtChn chn = getChn();                                                         // Request the WPC Certificate Chain
            final byte[] dig = WpcAthRsp.getSigDig(chn.getDig(), msg.array(), res);                 // Get the Digest for the challenge
            verify(dig, sig, chn.getPu());                                                          // Verification of the signature failed?
            mCach.add(chn);                                                                         // Add Certificate Chain to Certificate cache
        }
    }

    /**
     * Qi Authentication Initiator flow with caching
     *
     * @throws GeneralSecurityException In case the remote device cannot be authenticated
     * @throws IOException              In case a communication error occurred
     */
    private void runCach() throws GeneralSecurityException, IOException {
        byte[] dig = getDig();                                                                      // Get the digest of the remote WPC Certificate Chain
        WpcCrtChn chn = getChn(dig);                                                                // Search WPC Certificate Chain in the cache
        if (chn == null) {                                                                          // No WPC Certificate Chain found in the cache?
            chn = getChn();                                                                         // Request the WPC Certificate Chain
            mCach.add(chn);                                                                         // Add Certificate Chain to Certificate cache
        }
        mCom.setChn(chn);                                                                           // Register used WPC Cartificate chain
        ByteBuffer msg = getAth();                                                                  // Create CHALLENGE request
        byte[] res = sndAth(msg);                                                                   // Send the CHALLENGE Request message
        dig = WpcAthRsp.getSigDig(chn.getDig(), msg.array(), res);                                  // Get the Digest for the challenge
        byte[] sig = Arrays.copyOfRange(res, WpcAthRsp.LEN_ATH, res.length);                        // Get the signature from the CHALLENGE_AUTH Response
        verify(dig, sig, chn.getPu());                                                              // Verify the signature
    }

    /**
     * Simple Qi Authentication Initiator flow
     *
     * @throws GeneralSecurityException In case the remote device cannot be authenticated
     * @throws IOException              In case a communication error occurred
     */
    private void runSmpl() throws GeneralSecurityException, IOException {
        WpcCrtChn chn = getChn();                                                                   // Request the WPC Certificate Chain
        ByteBuffer msg = getAth();                                                                  // Create CHALLENGE request
        byte[] res = sndAth(msg);                                                                   // Send the CHALLENGE Request message
        byte[] dig = WpcAthRsp.getSigDig(chn.getDig(), msg.array(), res);                           // Get the Digest for the challenge
        byte[] sig = Arrays.copyOfRange(res, WpcAthRsp.LEN_ATH, res.length);                        // Get the signature from the CHALLENGE_AUTH Response
        verify(dig, sig, chn.getPu());                                                              // Verify the signature
    }

    /**
     * Sends a Qi Authentication Request
     *
     * @param   req The Qi Authentication Request
     * @param   typ The expected Qi Authentication Response type
     * @return  The Qi Authentication Response
     * @throws  IOException in case of NFC communication errors
     */
    private @NonNull ByteBuffer sndMsg(final @NonNull ByteBuffer req, int typ) throws IOException {
        final @NonNull byte[] ba = req.array();                                                     // Get request
        WpcLog.log(WpcLog.EvtTyp.REQ, ba);                                                          // Log request
        ByteBuffer res = ByteBuffer.wrap(mCom.sndMsg(ba));                                          // Send the Qi Authentication Request
        WpcLog.log(WpcLog.EvtTyp.RES, res.array());                                                 // Log response
        if ((res.get() & AppLib.BYT_UNS) != ((ATH_VER << 4) | typ)) {                               // Unexpected type
            throw new IOException();                                                                // Raise error
        }
        return res;                                                                                 // Return the Qi Authentication Response
    }

    /**
     * Tries to verifies the signature for a given message digest with WPC Certificate Chains in the cache
     *
     * @param   req The Challenge Request
     * @param   res The Challenge Auth Response
     * @param   sig The Signature
     * @return  true if the verification was successful, otherwise false
     */
    private boolean verify1(@NonNull byte[] req, @NonNull byte[] res, @NonNull byte[] sig) {
        for (WpcCrtChn chn: mCach) {                                                                // Repeat for all WPC Certificate Chains in the cache
            if (chn.getDig()[WpcKey.DIG_SIZ - 1] == res[2]) {                                       // Matches the last hash byte?
                byte[] dig = WpcAthRsp.getSigDig(chn.getDig(), req, res);                           // Get the Digest for the challenge
                try {
                    verify(dig, sig, chn.getPu());                                                  // Successful verification?
                    mCom.setChn(chn);                                                               // Report used WPC Certificate Chain
                    return true;                                                                    // Inform that the verification was successful
                } catch (GeneralSecurityException ignored) {}                                       // Ignore the wrong signature and test the next potential WPC Certificate Chain in the cache
            }
        }                                                                                           // No successful verification
        return false;                                                                               // Inform that the verification was not successful
    }

    /**
     * Verifies the signature for a given message digest
     *
     * @param   dig The Message digest
     * @param   sig The Signature
     * @param   crt The WPC Product Unit Certificate containing the public key
     * @throws  GeneralSecurityException in case the signature is wrong
     */
    private void verify(@NonNull byte[] dig, @NonNull byte[] sig, @NonNull WpcCrt crt) throws GeneralSecurityException {
        try {
            SafFkt.verSig(dig, sig, crt.getPublicKey());                                            // Verify the signature
        } catch (GeneralSecurityException err) {                                                    // An error occurred during the signature verification
            WpcLog.logErr("Wrong signature");                                                       // Log wrong signature
            throw err;                                                                              // Report no successful signature verification
        }
    }
}