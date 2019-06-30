package com.st.libsec;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.Arrays;

/**
 * WPC Authentication Responder class
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
public class WpcAthRsp {

    /** Offset for Qi Authentication version and message type */
    public static final int OFS_CHN = 1;

    /** Offset for digest in DIGESTS Response */
    public static final int OFS_DIG = 2;

    /** Message type for CHALLENGE_AUTH response */
    static final int RES_ATH = 0x03;

    /** Message type for CERTIFICATE response */
    static final int RES_CRT = 0x02;

    /** Message type for DIGESTS response */
    static final int RES_DIG = 0x01;

    private static final int    ERR_INV = 0x01;                                                     // INVALID  REQUEST error code
    private static final int    ERR_UNS = 0x04;                                                     // UNSPECIFIED error code
    private static final int    ERR_VER = 0x02;                                                     // UNSUPPORTED_PROTOCOL error code
    private static final int    RES_ERR = 0x07;                                                     // Message type for ERROR response
    private static final int    MSK_SLT = 0x03;                                                     // Mask to extract slot number
    private static final int    MSK_TYP = 0x0F;                                                     // Mask for Offset for message type
    private static final int    MSK_VER = 0xF0;                                                     // Mask for Qi Authentication version
    private static final int    OFS_TYP = 0;                                                        // Offset for Qi Authentication version and message type
    private static final int    OFS_SLT = 1;                                                        // Offset for slot number in Request
    private static final byte   PFX_ATH = 'A';                                                      // Prefix for TBSAuth

    /** Size for CHALLENGE_AUTH response header */
    public static final int LEN_ATH = 3;

    private final WpcCrtChn     mChn;                                                               // WPC Certificate Chain
    private final PrivateKey    mPrv;                                                               // Private key for Product Unit Certificate in mChn
    private final boolean       mPtx;                                                               // WPC device type

    /**
     * Initialize the WPC Authentication Responder
     *
     * @param   chn The WPC Certificate Chain of the WPC Authentication Responder
     * @param   prv The private key for the Product Unit certificate inside of chn
     */
    WpcAthRsp(@NonNull WpcCrtChn chn, @NonNull PrivateKey prv, boolean ptx) {
        mChn = chn;                                                                                 // Set the WPC Certificate Chain
        mPrv = prv;                                                                                 // Set the private key
        mPtx = ptx;                                                                                 // Get WPC device type
    }

    /**
     * Calculate the Digest for the signature of CHALLENGE_AUTH Response
     *
     * @param   dig The Certificate Chain Hash
     * @param   req The Challenge Request
     * @param   res The Challenge_AUTH Response
     * @return  The Digest for the signature
     */
    public static @NonNull byte[] getSigDig(@NonNull byte[] dig, @NonNull byte[] req, @NonNull byte[] res) {
        return WpcKey.getDig(getTbs(dig, req, res));                                                // Return Digest for the signature
    }

    /**
     * Calculate the TBSAuth of CHALLENGE_AUTH Response
     *
     * @param   dig The Certificate Chain Hash
     * @param   req The Challenge Request
     * @param   res The Challenge_AUTH Response
     * @return  The TBSAuth of CHALLENGE_AUTH Response
     */
    static @NonNull byte[] getTbs(@NonNull byte[] dig, @NonNull byte[] req, @NonNull byte[] res) {
        final @NonNull ByteBuffer tbs;                                                              // TBSAuth buffer
        tbs = ByteBuffer.wrap(new byte[AppLib.BYT_SIZ + WpcKey.DIG_SIZ + req.length + LEN_ATH]);    // Create TBSAuth buffer
        tbs.put(PFX_ATH);                                                                           // Add Prefix
        tbs.put(dig);                                                                               // Add Certificate Chain Hash
        tbs.put(req);                                                                               // Add CHALLENGE Request
        tbs.put(Arrays.copyOf(res, LEN_ATH));                                                       // Add header of CHALLENGE_AUTH Response
        return tbs.array();                                                                         // Return TBSAuth of CHALLENGE_AUTH Response
    }

    /**
     * Performs an Authentication Request
     *
     * @param   req The Authentication Request
     * @return  The Authentication Response
     */
    @Nullable byte[] athReq(@NonNull byte[] req) {
        WpcLog.log(WpcLog.EvtTyp.REQ, req);                                                         // Log request
        if (req.length == 0) {                                                                      // No authentication Request data available?
            WpcLog.logErr("No authentication request data available");                              // Log error
            return error(ERR_INV, 0);                                                               // Return ERROR message
        }
        if (((req[OFS_TYP] & MSK_VER) >> 4) !=  WpcAthIni.ATH_VER) {                                // Qi Authentication verion not supported?
            WpcLog.logErr("Qi authentication version is not supported");                            // Log error
            return error(ERR_VER, WpcAthIni.ATH_VER);                                               // Return ERROR response
        }
        final @NonNull byte[] res;
        int msk = req[OFS_TYP] & MSK_TYP;                                                           // Get Message type
        switch (msk) {                                                                              // Analyze message type
            case WpcAthIni.REQ_DIG: res = getDig(req); break;                                       // GET_DIGESTS request
            case WpcAthIni.REQ_CRT: res = getCrt(req); break;                                       // GET_CERTIFICATE request
            case WpcAthIni.REQ_ATH: res = challenge(req); break;                                    // CHALLENGE request
            default:
                WpcLog.logErr("Unknown Qi authentication request message type received");           // Log error
                res = error(ERR_INV, 0);                                                            // Return ERROR response

        }                                                                                           // No valid Message type
        WpcLog.log(WpcLog.EvtTyp.RES, res);                                                         // Log response
        return res;                                                                                 // Return the authentication response
    }

    /**
     * Executes a CHALLENGE request message
     *
     * @param   req The CHALLENGE request message
     * @return  The Authentication repsonse
     */
    private @NonNull byte[] challenge(@NonNull byte[] req) {
        if (req.length < WpcAthIni.OFS_RND + WpcAthIni.RND_SIZ) {                                   // Not enough data available?
            WpcLog.logErr("Not enough data in CHALLENGE Request");                                  // Log error
            return error(ERR_INV, 0);                                                               // Return ERROR message
        }
        if (chkSlt(req)) {                                                                          // Wrong slot number or WPC Device?
            return error(ERR_INV, 0);                                                               // Return ERROR message
        }
        ByteBuffer res = getChAth(mChn);                                                            // Create CHALLENGE_AUTH Response message
        try {
            res.put(SafFkt.genSig(getSigDig(mChn.getDig(), req, res.array()), mPrv));               // Calculate the signature
            return res.array();                                                                     // Return the CHALLENGE_AUTH Response
        } catch (Exception err) {                                                                   // Error occurred during signature calculation
            return error(ERR_UNS, 0);                                                               // Return UNSPECIFIED ERROR message
        }
    }

    /**
     * Checks the slot number and WPC device type
     *
     * @param   req The Qi Authentication request where slot number and WPC device type shall be checked
     * @return  true if error identified, otherwise false
     */
    private boolean chkSlt(byte[] req) {
        if ((req[OFS_SLT] & MSK_SLT) != WpcAthIni.SLOT_0) {                                         // Wrong slot number?
            WpcLog.logErr("Slot is not available");                                                 // Log error
            return true;                                                                            // Return error
        }
        if ((req[OFS_SLT] & 0x04) == ((mPtx?1:0) << 2)) {                                           // Wrong requesting WPC device?
            WpcLog.logErr("Request from wrong WPC device type");                                    // Log error
            return true;                                                                            // Return error
        }
        return false;                                                                               // Return no error
    }

    /**
     * Creates the header of a CHALLENGE_AUTH Response
     * @param   chn The WPC Certificate Chain for this challenge
     * @return  The header of the CHALLENGE AUTH Response
     */
    public static ByteBuffer getChAth(WpcCrtChn chn) {
        ByteBuffer res = WpcAthIni.getMsg(RES_ATH, LEN_ATH + WpcCrt.LEN_SIG);                       // Create CHALLENGE_AUTH Response message
        res.put((byte)((WpcAthIni.ATH_VER << 4) | WpcAthIni.SLOT_MSK));                             // Maximum Qi Authentication Protocol version and Slots Populated Mask
        res.put(chn.getDig()[WpcKey.DIG_SIZ - 1]);                                                  // Add LSB of Certificate Chain Digest
        return res;                                                                                 // Return the header of the CHALLENGE_AUTH Response
    }

    /**
     * Executes the GET_CERTIFICATE Request message
     * @param   req The GET_CERTIFICATE Request message
     * @return  The Authentication Response message
     */
    private @NonNull byte[] getCrt(@NonNull byte[] req) {
        if (req.length < 4) {                                                                       // Not enough data available or not Slot 0 requested?
            WpcLog.logErr("Not enough data in GET_CERTIFICATE Request");                            // Log error
            return error(ERR_INV, 0);                                                               // Return ERROR message
        }
        if (chkSlt(req)) {                                                                          // Wrong slot number or WPC Device?
            return error(ERR_INV, 0);                                                               // Return ERROR message
        }
        ByteBuffer buf = ByteBuffer.wrap(req);                                                      // Get byte buffer of request
        buf.position(2);                                                                            // Set the offest for the offset parameter
        int ofs = buf.get() & AppLib.BYT_UNS | ((req[OFS_SLT] & 0xC0) << 2);                        // Read offset
        int len = buf.get() & AppLib.BYT_UNS | ((req[OFS_SLT] & 0x30) << 4);                        // Read length
        byte[] chn = mChn.getChn();                                                                 // Get WPC Certificate Chain
        if (ofs + len > chn.length) {                                                               // Too many bytes requested?
            WpcLog.logErr("Too many data requested");                                               // Log error
            return error(ERR_INV, 0);                                                               // Return ERROR message
        }
        buf = WpcAthIni.getMsg(RES_CRT, len + OFS_CHN);                                             // Create CERTIFICATE Response
        buf.put(chn, ofs, len);                                                                     // Add Certificate Chain Segment
        return buf.array();                                                                         // Return the CERTIFICATE Response
    }

    /**
     * Executes the GET_DIGEST request message
     *
     * @param   req The GET_DIGEST Request
     * @return  The Authentication Response
     */
    private @NonNull byte[] getDig(@NonNull byte[] req) {
        if (req.length < 2) {                                                                       // Not enough data available?
            WpcLog.logErr("Not enough data in GET_DIGESTS Request");                                // Log error
            return error(ERR_INV, 0);                                                               // Return ERROR message
        }
        ByteBuffer res;                                                                             // Authentication Response
        if ((req[OFS_SLT] & WpcAthIni.SLOT_MSK) == WpcAthIni.SLOT_MSK) {                            // Slot 0 requested?
            res = WpcAthIni.getMsg(RES_DIG, OFS_DIG + WpcKey.DIG_SIZ);                              // Create DIGESTS Response with one Digest
            res.put((byte)((WpcAthIni.SLOT_MSK << 4) | WpcAthIni.SLOT_MSK));                        // Set Slots Returned Mask
            res.put(mChn.getDig());                                                                 // Adds Digest
        } else {                                                                                    // No Slot 0 requested
            res = WpcAthIni.getMsg(RES_DIG, OFS_DIG);                                               // Create DIGESTS Response with no Digest
            res.put((byte)(WpcAthIni.SLOT_MSK << 4));                                               // Set Slots Returned Mask
            WpcLog.logErr("Requested slot is not available");                                       // Log warning
        }
        return res.array();                                                                         // Return the DIGESTS Response
    }

    /**
     * Returns an ERROR Response
     * @param   cod The Error Code
     * @param   dat The Error Data
     * @return  The ERROR Response
     */
    private @NonNull byte[] error(int cod, int dat) {
        ByteBuffer res = WpcAthIni.getMsg(RES_ERR, 3);                                              // Create the ERROR Response
        res.put((byte)cod);                                                                         // Set the Error Code
        res.put((byte)dat);                                                                         // Set the Error Data
        return res.array();                                                                         // Return the ERROR Response
    }
}