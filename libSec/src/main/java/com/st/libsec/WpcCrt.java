package com.st.libsec;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * WPC Certificate class
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
public class WpcCrt extends Certificate {

    /** Extensions for WPC Certificate files */
    public static final String EXT_CRT = "_crt" + WpcFil.EXT_TXT;

    /** Extensions for private key files */
    public static final String EXT_PRV = "_prv" + WpcFil.EXT_TXT;

    /** File name for WPC root certificate */
    private static final String FILE_ROOT = "WPC_Root" + EXT_CRT;

    /** Digest of the plugfest Root Certificate */
    static byte[] DIG_CA;

    /** Public key for plugfest Root certificate */
    static PublicKey PUB_CA;

   /** Length of EC P-256 signature */
    static final int LEN_SIG = 2 * WpcKey.KEY_SIZ;

    /** Qi Authentication specification version for this WPC Certificate implementation */
    static final String SPEC_VER = "Qi Specification Authentication Protocol Version 1.3 draft 6";

    /** Qi Authentication Certificate type name */
    static final String TYP_CRT = "WPC";

    /** Root Certificate type */
    public static final byte TYP_ROOT = 1;

    /** Manucaturer or Secondary Certificate type */
    public static final byte TYP_INT = 2;

    /** Product Unit Certificate type */
    public static final byte TYP_RPU = 3;

    /** PTx Product Unit Certificate type */
    public static final byte TYP_TPU = 7;

    /** The app resources */
    static Resources sRes;

    /** The app data directory */
    public static File sDir;

    private static final int    LEN_SNR = 9;                                                        // Length of serial number
    private static final int    LEN_ID  = 6;                                                        // Length of issuer/subject ID
    private static final int    LEN_PUB = AppLib.BYT_SIZ + WpcKey.KEY_SIZ;                          // Length of public key
    private static final int    LEN_QIID= 3;                                                        // Length of Qi-ID field
    private static final String MAN_ID  = "M:%04X";                                                 // Format of identifier containing a Manufacturer Code (PTMC)
    private static final int    MAN_OFS = 2;                                                        // Offset of the Manufacturer Code inside the identifier
    private static final int    OFS_VER = 0;                                                        // Offset for Qi Authentication Certificate Structure version
    private static final int    OFS_TYP = OFS_VER + AppLib.BYT_SIZ;                                 // Offset for Certificate Type
    private static final int    OFS_OFS = OFS_TYP + AppLib.BYT_SIZ;                                 // Offset for Signature Offset
    private static final int    OFS_SNR = OFS_OFS + AppLib.BYT_SIZ;                                 // Offset for serial number
    private static final int    OFS_IID = OFS_SNR + LEN_SNR;                                        // Offset for issuer identifier
    private static final int    OFS_SID = OFS_IID + LEN_ID;                                         // Offset for subject identifier
    private static final int    OFS_PUB = OFS_SID + LEN_ID;                                         // Offset for public key
    private static final int    OFS_SIG = OFS_PUB + LEN_PUB;                                        // Offset for signature
    private static final byte   PFX_CRT = 'C';                                                      // Prefix for TBSCertificate
    private static final String PFX_MAN = MAN_ID.substring(0, MAN_OFS);                             // Prefix for Manufacturer code
    private static final BigInteger VAL_K = new BigInteger("1111111111111111111111111111111111111111111111111111111111111111", AppLib.BAS_HEX);
    private static final byte   VER_CRT = 0x01;                                                     // Qi Authentication Certificate Structure Version

    /** Length of WPC Certificate */
    static final int LEN_CRT = OFS_SIG + LEN_SIG;

    private byte[]      mCrt;                                                                       // WPC certificate
    private int         mMan;                                                                       // Manufacturer code responsible for this certificate

    /**
     * Initialize the WPC certificate class
     *
     * @param ctx   The app context
     */
    public static void init(final @NonNull Context ctx, final @NonNull String dir) {
        sRes = ctx.getResources();                                                                  // Sets the app resources
        try {
            sDir = ctx.getExternalFilesDir(null);                                                   // Get File the base app data directory
            final byte[] ba = new WpcFil(new File(sDir, dir + FILE_ROOT)).read();                   // Get content of Certificate file
            final @NonNull WpcCrt crt = new WpcCrt(ba, WpcMan.ROOT_MAN);                            // Create Root WPC Certificate
            if (crt.getTyp() != TYP_ROOT) {                                                         // No Root Certificate?
                throw new CertificateEncodingException("No Root Certificate type!");                // Generate error
            }
            final @NonNull byte[] id = getId(TYP_CRT);                                              // Get expected ID
            if (!Arrays.equals(id, crt.getIid())) {                                                 // Wrong Issuer ID?
                throw new CertificateEncodingException("No Root Issuer ID!");                       // Generate error
            }
            if (!Arrays.equals(id, crt.getSid())) {                                                 // Wrong Subject ID?
                throw new CertificateEncodingException("No Root Subject ID!");                      // Generate error
            }
            crt.verify(crt.getPublicKey());                                                         // Verify WPC Root Certificate
            PUB_CA = crt.getPublicKey();                                                            // Set the public key of the WPC Root Certificate
            DIG_CA = crt.getDatDig();                                                               // Set the WPC Root Certificate Digest
        } catch (Exception err) {                                                                   // Error occurred during loading WPC root certificate
            Dbg.log("Cannot load Root certificate!", err);                                          // Log error
            @NonNull BigInteger bi;                                                                 // Large integer for constant initialization
            bi = new BigInteger("A61F3A7981F1D0B664F8935CD998F4F64D573DA4CC5846F6AF012FEBDA1C8AE1", AppLib.BAS_HEX);
            DIG_CA = WpcKey.getInt(bi, WpcKey.DIG_SIZ);                                             // Set the WPC Root Certificate Digest for the plugfest
            bi = new BigInteger("03299CBB09C006946B050957B78C57BE4EF82356D7B18CBFC72FFAEC1C43E58E54", AppLib.BAS_HEX);
            PUB_CA =  WpcKey.getPubKey(bi.toByteArray());                                           // Set the public key of the WPC Root Certificate for the plugfest
        }
    }

    /**
     * Creates a new WPC Certificate using a fixed K value for signature
     * only used by the chkSpec method to verify the example calculation in the Qi Authentication spacification
     *
     * @param   typ The WPC Certificate Type
     * @param   snr The serial number of the WPC Certificate
     * @param   iid The Issuer Identifier of the WPC Certificate
     * @param   man The Manufacturer code responsible for this certificate
     * @param   sid The Subject Identifier of the WPC Certificate
     * @param   pub The public key of the WPC Certificate
     * @param   key The private key signing this WPC Certificate
     */
    private WpcCrt(byte typ, @NonNull byte[] snr, @NonNull byte[] iid, int man, @NonNull byte[] sid, @NonNull PublicKey pub, @NonNull PrivateKey key) {
        super(TYP_CRT);                                                                             // Initialize WPC Certificate
        mMan = man;                                                                                 // Register manufacturer code responsible for this Certificate
        WpcKey.logPrvKey(key);                                                                      // Log private key of certificate
        ByteBuffer crt = iniCrt(typ, snr, iid, sid, pub);                                           // Create byte buffer for WPC Certificate
        final @NonNull byte[] tbs = getTbs();                                                       // Get the TBSCertificate
        WpcLog.logCmt(Dbg.hexStr("TBSCertificate", tbs));                                           // Log the TBSCertificate
        crt.put(WpcKey.genSig(key, WpcKey.getDig(tbs), VAL_K));                                     // Add signature
    }


    /**
     * Creates a new WPC Certificate
     *
     * @param   typ The WPC Certificate Type
     * @param   snr The serial number of the WPC Certificate
     * @param   iid The Issuer Identifier of the WPC Certificate
     * @param   man The Manufacturer code responsible for this certificate
     * @param   sid The Subject Identifier of the WPC Certificate
     * @param   pub The public key of the WPC Certificate
     * @param   prv The private key of this WPC Certificate
     */
    public WpcCrt(final byte typ, final int snr, final @NonNull byte[] iid, final int man, final @NonNull byte[] sid, final @NonNull PublicKey pub, final @NonNull PrivateKey prv) {
        super(TYP_CRT);                                                                             // Initialize WPC Certificate
        mMan = man;                                                                                 // Register manufacturer code responsible for this Certificate
        final @NonNull byte[] nr = new byte[LEN_SNR];                                               // Create serial number
        nr[LEN_SNR-1] = (byte)snr;                                                                  // Set serial number
        final @NonNull ByteBuffer crt = iniCrt(typ, nr, iid, sid, pub);                             // Create byte buffer for WPC Certificate
        try {
            crt.put(SafFkt.genSig(getDig(), prv));                                                  // Add signature
        } catch (GeneralSecurityException err) {                                                    // Error occrred (should never happen)
            throw new ProviderException("Cannot sign certificate", err);                            // Raise the error
        }
    }

    /**
     * Create WPC Manufacturer certificate for signature
     *
     * @param   sid The Manufacturer code
     * @param   pub The public key of the WPC Manufacturer Certificate
     */
    public WpcCrt(int sid, PublicKey pub) {
        super(TYP_CRT);                                                                             // Initialize WPC Certificate
        byte[] id = getId(String.format(MAN_ID, sid));                                              // Create subject ID for manufacturer certificate
        iniCrt(TYP_INT, new byte[LEN_SNR], getId(TYP_CRT), id, pub);                                // Initialize WPC Manufacturer certificate for signature
    }

    /**
     * Create WPC Manufacturer certificate for signature
     *
     * @param   snr The serial number of the Manufacturer Certificate
     * @param   man The Manufacturer Code (PTMC)
     * @param   pub The public key of the Manufacturer Certificate
     * @param   prv The root private key
     * @throws  GeneralSecurityException in case the Manufacturer Certificate cannot be signed
     */
    public WpcCrt(final @NonNull byte[] snr, final int man, final @NonNull PublicKey pub, final @NonNull PrivateKey prv) throws GeneralSecurityException {
        super(TYP_CRT);                                                                             // Initialize WPC Certificate
        mMan = man;                                                                                 // Register manufacturer code responsible for this Certificate
        byte[] id = getId(String.format(MAN_ID, man));                                              // Create subject ID for manufacturer certificate
        ByteBuffer crt = iniCrt(TYP_INT, snr, getId(TYP_CRT), id, pub);                             // Initialize WPC Manufacturer certificate for signature
        final byte[] dig = getDig();                                                                // Get hash for signature
        Dbg.log(Dbg.seqHex("Hash of TBSCertificate: ", dig));                                       // Log hash for signature
        crt.put(SafFkt.genSig(dig, prv));                                                           // Add signature
        Dbg.log(Dbg.seqHex("Certificate data: ", crt.array()));                                     // Log Certificate
    }

    /**
     * Create WPC certificate with given byte array of WPC certificate
     *
     * @param   crt Byte array of the WPC certificate
     * @param   man The Manufacturer code responsible for this certificate
     */
    public WpcCrt(@NonNull byte[] crt, int man) {
        super(TYP_CRT);                                                                             // Initialize WPC certificate
        int len = crt.length;                                                                       // Get length of WPC certificate
        mCrt = new byte[len];                                                                       // Create WPC certificate byte array
        System.arraycopy(crt, 0, mCrt, 0, len);                                                     // Copy WPC certificate byte array
        mMan = man;                                                                                 // Register manufacturer code responsible for this Certificate
    }

    /**
     * Checks the Qi Authentication Specification
     */
    public static void chkSpec() {
        WpcLog.begLog("Cryptographic examples of the Qi Authentication Specification");             // Log Cryptographic examples of the Qi Authentication Specification
        WpcLog.logCmt(Dbg.hexStr("k", VAL_K.toByteArray()));                                        // Log K value for sigantures
        BigInteger bi;                                                                              // Private keys
        bi = new BigInteger("d326969a5c2bc25dc48c6cb3ded7cdcc8063f8f0a7a1e3b68f62c622650cfda2", AppLib.BAS_HEX);
        WpcLog.logCmt(Dbg.hexStr("WPC CA private key", WpcKey.getInt(bi, WpcKey.KEY_SIZ)));         // Log WPC CA private key
        @NonNull PrivateKey prv = WpcKey.getPrvKey(bi);                                             // Set WPC CA example private key
        @NonNull PublicKey pub = WpcKey.getPubKey(prv);                                             // Set WPC CA example public key
        WpcLog.logCmt(Dbg.hexStr("WPC CA public key", WpcKey.getKeyDat(pub)));                      // Log WPC CA public key
        byte[] id = getId(TYP_CRT);                                                                 // Issuer ID for Root certificate
        final @NonNull byte[] snrrt = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};       // Set serial number of Root Certificate
        WpcCrt rt = new WpcCrt((byte)1, snrrt, id, WpcMan.ROOT_MAN, id, pub, prv);                  // Create example root certificate
        @NonNull byte[] ba = rt.getEncoded();                                                       // Get byte array of Root Certificate
        WpcLog.logCmt(Dbg.hexStr("WPC CA Root Certificate", ba));                                   // Log the Root Certificate
        WpcLog.log(WpcLog.EvtTyp.CRT, ba);                                                          // Analyse the Root Certificate
        final @NonNull byte[] digca = rt.getDatDig();                                               // Calculate example root certificate
        WpcLog.logCmt(Dbg.hexStr("WPC CA hash", digca));                                            // Log WPC CA Root certificate hash
        bi = new BigInteger("5baae6070a108d646da605d976a6e407e2d43197119dab994d8eea98c0c768e1", AppLib.BAS_HEX);
        WpcLog.logCmt(Dbg.hexStr("Acme private key", WpcKey.getInt(bi, WpcKey.KEY_SIZ)));           // Log Acme private key
        PrivateKey key = WpcKey.getPrvKey(bi);                                                      // Set Acme private key
        pub = WpcKey.getPubKey(key);                                                                // Set Acme public key
        WpcLog.logCmt(Dbg.hexStr("Acme public key", WpcKey.getKeyDat(pub)));                        // Log WPC CA public key
        byte[] sid = getId(String.format(MAN_ID, WpcMan.ACME_MAN));                                 // Get subject identifier of Acme certificate
        final @NonNull WpcCrt man;                                                                  // Acme Manufacturer Certificate
        final @NonNull byte[] snrman = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};      // Set serial number of Acme Manufacturer Certificate
        man = new WpcCrt(TYP_INT, snrman, id, WpcMan.ACME_MAN, sid, pub, prv);                      // Create Acme Manufacturer Certificate
        ba = man.getEncoded();                                                                      // Get byte array of Acme Certificate
        WpcLog.logCmt(Dbg.hexStr("Acme Manufacturer Certificate", ba));                             // Log the Acne Certificate
        WpcLog.log(WpcLog.EvtTyp.CRT, ba);                                                          // Analyse the Acme Certificate
        bi = new BigInteger("e45588d311be5888ad3627ccccfd69d6e5b77fb2760a65e43b3f04d63e9d52e1", AppLib.BAS_HEX);
        WpcLog.logCmt(Dbg.hexStr("PU private key", WpcKey.getInt(bi, WpcKey.KEY_SIZ)));             // Log Acme Lampstand private key
        prv = key;                                                                                  // Use Acme private key for signing the Acme Lampstand Product Unit certificate
        id = sid;                                                                                   // Issuer ID of Acme Lampstand Product Unit certificate is Acme
        key = WpcKey.getPrvKey(bi);                                                                 // Set Acme Lampstand Product Unit example private key
        pub = WpcKey.getPubKey(key);                                                                // Set Acme public key
        WpcLog.logCmt(Dbg.hexStr("PU public key", WpcKey.getKeyDat(pub)));                          // Log Acme Lampstand public key
        final @NonNull byte[] qid = {0x00, 0x63, (byte)0x86, 0x00, 0x00, 0x00};                     // Get subject identifier of Acme Lampstand Product Unit certificate
        final @NonNull byte[] snr = {0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x04, (byte)0xd2};   // Serial number of Acme Lampstand Product Unit certificate
        WpcCrt pu = new WpcCrt(TYP_TPU, snr, id, WpcMan.ACME_MAN, qid, WpcKey.getPubKey(key), prv); // Create Acme Lampstand Product Unit certificate
        ba = pu.getEncoded();                                                                       // Get byte array of PU Certificate
        WpcLog.logCmt(Dbg.hexStr("Product Unit Certificate", ba));                                  // Log the PU Certificate
        WpcLog.log(WpcLog.EvtTyp.CRT, ba);                                                          // Analyse the PU Certificate
        WpcCrtChn chn = new WpcCrtChn(digca, man);                                                  // Create WPC example certificate chain
        chn.addCrt(pu);                                                                             // Add product unit certificate
        ba = chn.getChn();                                                                          // Get byte array of Certificate Chain
        WpcLog.logCmt(Dbg.hexStr("Certificate Chain", ba));                                         // Log the Certificate Chain
        WpcLog.log(WpcLog.EvtTyp.CHN, ba);                                                          // Analyse the Certificate Chain
        byte[] dig = chn.getDig();                                                                  // Get digest of WPC example Certificate chain
        WpcLog.logCmt(Dbg.hexStr("Certificate Chain hash", dig));                                   // Log example Certificate Chain hash
        bi = new BigInteger("1B00A5023B84B46CD7102E6F519AF7993752", AppLib.BAS_HEX);                // Get CHALLENGE Request
        byte[] req = bi.toByteArray();                                                              // Set GET CHALLENGE Request
        WpcLog.log(WpcLog.EvtTyp.REQ, req);                                                         // Log the GET CHALLENGE request
        byte[] res = new BigInteger("131100", AppLib.BAS_HEX).toByteArray();                        // Set CHALLENGE_AUTH Response
        res[2] = dig[31];                                                                           // Set LSB of Certificate Chain Hash
        WpcLog.log(WpcLog.EvtTyp.RES, res);                                                         // Log the GET CHALLENGE AUTH response
        dig = WpcAthRsp.getTbs(dig, req, res);                                                      // Get TBSAuth
        WpcLog.logCmt(Dbg.hexStr("TBSAuth", dig));                                                  // Log TBSAuth
        WpcKey.genSig(key, WpcKey.getDig(dig), VAL_K);                                              // Calculate Signature
        WpcLog.logCmt("End of Cryptographic examples of Qi Authentication Specification");          // Log end of specification check
    }

    /**
     * Returns the byte array of an identifier field
     *
     * @param   id  Identifier string
     * @return  The byte array of an identifier field
     */
    static byte[] getId(String id) {
        ByteBuffer buf = ByteBuffer.wrap(new byte[LEN_ID]);                                         // Create byte array for identifier field
        buf.put(id.getBytes(AppLib.CHR_ASC));                                                       // Insert identifier field string
        return buf.array();                                                                         // Return the byte array of the identifier field
    }

    /**
     * Logs the WPC certificate
     *
     * @param crt   The WPC Certificate Chain
     * @param pos   The position of the Certificate in the Certificate chain
     * @param log   The log array
     */
    static void logCrt(final BytBuf crt, int pos, final @NonNull ArrayList<String> log) {
        log.add(WpcLog.TAB + Dbg.logStr(crt.getByte(), "Certificate Structure Version"));           // Log Certificate Structure Version
        final byte byt = crt.getByte();                                                             // Get the Certificate type
        final int typ = byt & 0x03;                                                                 // Calculate the Certificate type
        final String des;                                                                           // Certificate description
        switch(typ) {                                                                               // Analyse the Certificate type
            case TYP_ROOT: des = "Root Certificate"; break;                                         // Root Certificate
            case TYP_INT:                                                                           // Intermediate Certificate
                if (pos == 1) {                                                                     // Manufacturer Certificate?
                    des = "Manufacturer Certificate";                                               // Show Manufacturer Certificate
                } else {                                                                            // Secondary Certificate
                    des = "Secondary Certificate";                                                  // Show Secondary Certificate
                }
                break;
            case TYP_RPU: des = "Product Unit Certificate"; break;                                  // Product Unit Certificate
            default: des = "Reserved";                                                              // Reserved Certificate type
        }
        log.add(WpcLog.TAB + Dbg.logStr(byt, "Certificate Type: " + des));                          // Log Certificate Type
        int ofs = crt.getUsByte();                                                                  // Get the Signature offset
        log.add(WpcLog.TAB + Dbg.logStr((byte)ofs, "Signature offset: " + ofs + " bytes"));         // Log Signature offset
        log.add(WpcLog.TAB + Dbg.logStr(crt.getArray(LEN_SNR), "Serial Number"));                   // Log Serial number
        logId(crt, false, "Issuer", log);                                                           // Log the Issuer ID
        logId(crt, typ == TYP_RPU, "Subject", log);                                                 // Log the Subject ID
        log.add(WpcLog.TAB + Dbg.hexStr("Public Key", crt.getArray(33)));                           // Log public key
        ofs = ofs - 57;                                                                             // Calculate number of reserved bytes
        if (ofs > 0) {                                                                              // Reserved bytes available?
            log.add(WpcLog.TAB + Dbg.hexStr("Reserved bytes", crt.getArray(ofs)));                  // Log the reserved bytes
        }
        WpcLog.logSig(crt, WpcLog.TAB, log);                                                        // Log the signature of the certificate
    }

    /**
     * Logs the ID
     *
     * @param crt   The WPC Certificate chain
     * @param pu    Flag for product unit Certificate
     * @param nam   The ID name
     * @param log   The log array
     */
    private static void logId(final @NonNull BytBuf crt, final boolean pu, final @NonNull String nam, final @NonNull ArrayList<String> log) {
        final byte[] id = crt.getArray(LEN_ID);                                                     // Get the ID
        final String str = new String(id, AppLib.CHR_ISO).trim();                                   // Get the ID string
        @NonNull String des;                                                                        // ID description
        if (str.equals(TYP_CRT)) {                                                                  // Root certificate?
            des = "Root Certificate";                                                               // Show root certificate
        } else if (str.startsWith(PFX_MAN)) {                                                       // Manufacturer Certificate?
            try {
                int ptmc = Integer.parseInt(str.substring(MAN_OFS), AppLib.BAS_HEX);                // Calculate PTMC
                des = String.format("PTMC = %04X - %s", ptmc, WpcMan.getMan(ptmc));                 // Show the manufacturer
            } catch (NumberFormatException err) {                                                   // Invalid PTMC
                des = "invalid PTMC";                                                               // Show invalid PTMC
            }
        } else if(pu) {                                                                             // Product Unit Certificate
            final int qiid = AppLib.bcdToInt(Arrays.copyOf(id, 3));                                 // Get the Qi ID
            des = "Qi ID = " + qiid + " - " + WpcQiId.getName(qiid, WpcMan.ERR_MAN);                // Show the Qi ID
        } else {                                                                                    // Secondary Certificate
            des = "Secondary Certificate: " + str;                                                  // Show the Secondary Certificate
        }
        log.add(WpcLog.TAB + Dbg.logStr(id, nam + " ID: " + des));                                  // Log ID
    }

    /**
     * Return the Digest of the Certificate content
     *
     * @return  The Digest of the Certificate content
     */
    private @NonNull byte[] getDatDig() {
        return WpcKey.getDig(mCrt);                                                                 // Return the Certificate digest
    }

    /**
     * Return the WPC Certificate Digests
     *
     * @return  The WPC Certificate Digest
     */
    private byte[] getDig() {
        return WpcKey.getDig(getTbs());                                                             // Return the TBSCertificate digest
    }

    /**
     * Returns the WPC certificate byte array
     *
     * @return  The WPC certificate byte array
     */
    @Override public byte[] getEncoded() {
        return mCrt;                                                                                // Return the WPC Certificate byte array
    }

    /**
     * Returns the issuer identifier of the WPC certificate
     *
     * @return  The issuer identifier of the WPC certificate
     */
    @NonNull byte[] getIid() {
        return Arrays.copyOfRange(mCrt, OFS_IID, OFS_SID);                                          // Returns the issuer identifier
    }

    /**
     * Returns the manufaacturer code of a Manufacturer Certificate
     *
     * @return  The manufacturer code
     */
    public int getMan() {
        return getMan(getSid());                                                                    // Return the manufacturer code
    }


    /**
     * Returns the manufaacturer code of a Manufacturer Certificate
     *
     * @return  The manufacturer code
     */
    private int getMan(final @NonNull byte[] id) {
        final String str = new String(id, AppLib.CHR_ASC);                                          // Get the subject identifier as a string
        if ((str.length() != LEN_ID) || !str.startsWith(PFX_MAN)) {                                 // No valid subject ID?
            return WpcMan.ERR_MAN;                                                                  // Return error manufacturer code
        }
        mMan = Integer.valueOf(str.substring(MAN_OFS), AppLib.BAS_HEX);                             // Get the manufacturer code
        return mMan;                                                                                // Return the manufacturer code
    }

    /**
     * Returns the Qi-ID of the Product Unit certificate
     *
     * @return  The Qi-ID of the Product Unit certificate
     */
    int getQiId() {
        return AppLib.bcdToInt(Arrays.copyOfRange(mCrt, OFS_SID, OFS_SID + LEN_QIID));              // Returns the Qi-ID of the Product Unit Certificate
    }

    /**
     * Returns the subject identifier of the WPC certificate
     *
     * @return  The subject identifier of the WPC certificate
     */
    public @NonNull byte[] getSid() {
        return Arrays.copyOfRange(mCrt, OFS_SID, OFS_PUB);                                          // Returns the subject identifier
    }

    /**
     * Returns the serial number of the WPC certificate
     *
     * @return  The serial number of the WPC certificate
     */
    public @NonNull byte[] getSnr() {
        return Arrays.copyOfRange(mCrt, OFS_SNR, OFS_IID);                                          // Returns the serial numberr
    }

    /**
     * Returns the TBS Certificate
     *
     * @return  The TBS Certificate
     */
    private byte[] getTbs() {
        ByteBuffer val = ByteBuffer.wrap(new byte[3 + OFS_SIG]);                                    // Create the byte array for the signature computation
        val.put(PFX_CRT);                                                                           // Add preceding byte array
        val.putShort((short)mMan);                                                                  // Add manufacturer code responsible for this Certificate
        val.put(mCrt, 0, OFS_SIG);                                                                  // Add byte array of certificate (without the signature)
        return val.array();                                                                         // Return the TBSCertificate
    }

    /**
     * Initialize the WPC Certificate
     *
     * @param   typ The WPC Certificate Type
     * @param   snr The serial number of the WPC Certificate
     * @param   iid The Issuer Identifier
     * @param   sid The Subject Identifier
     * @param   pub The public key of the WPC Certificate
     * @return  The byte buffer of the WPC Certificate
     */
    private @NonNull ByteBuffer iniCrt(byte typ, @NonNull byte[] snr, @NonNull byte[] iid, @NonNull byte[] sid, @NonNull PublicKey pub) {
        mCrt = new byte[LEN_CRT];                                                                   // Create byte array for WPC Certificate
        ByteBuffer crt = ByteBuffer.wrap(mCrt);                                                     // Create byte buffer for WPC Certificate
        crt.put(VER_CRT);                                                                           // Add WPC Certificate version
        crt.put(typ);                                                                               // Add WPC Certificate type
        crt.put((byte)(OFS_SIG));                                                                   // Add offset for Certificate signature
        crt.put(snr);                                                                               // Add serial number
        crt.put(iid);                                                                               // Add issuer identifier
        crt.put(sid);                                                                               // Add subject identifier
        crt.put(WpcKey.getComKey(pub));                                                             // Add the public key
        return crt;                                                                                 // return the initialize Certificate
    }

    /**
     * Verifies the signature of the WPC certificate
     *
     * @param   key Public key to verify the signature in the WPC Certificate
     * @throws CertificateException In case the Certificate is wrong formatted
     * @throws InvalidKeyException  In case the algorithm is not available (should never occur)
     * @throws SignatureException   In case the Signature is incorrect
     */
    @Override public void verify(PublicKey key) throws CertificateException, InvalidKeyException, SignatureException {
        if (mCrt[OFS_VER] != VER_CRT) {                                                             // Wrong WPC Certificate version?
            throw new CertificateEncodingException("Wrong WPC Certificate version");                // Throw certification error
        }
        int ofs = mCrt[OFS_OFS];                                                                    // Get offset
        byte[] sig = new byte[LEN_SIG];                                                             // Create Signature byte array
        System.arraycopy(mCrt, ofs, sig, 0, LEN_SIG);                                               // Get signature stored in the certificate
        SafFkt.verSig(getDig(), sig, key);                                                          // Verify signature
    }

    /**
     * Verifies the signature of the WPC certificate
     *
     * @param   key         Public key to verify the signature in the WPV Vertificate
     * @param   sigProvider The signature algorithm provider (not used here)
     * @throws CertificateException In case the Certificate is wrong formatted
     * @throws InvalidKeyException  In case the algorithm is not available (should never occur)
     * @throws SignatureException   In case the Signature is incorrect
     */
    @Override public void verify(PublicKey key, String sigProvider) throws CertificateException, InvalidKeyException, SignatureException {
        verify(key);                                                                                // Verifies the signature of the WPC Certificate
    }

    /**
     * Returns the Certificate name
     *
     * @return  WPC certificate name
     */
    @Override public @NonNull String toString() {
        return "WPC certificate";                                                                   // Return WPC certificate name
    }

    /**
     * Return the public key of the WPC certificate
     *
     * @return  The public key of the WPC certificate
     */
    @Override public PublicKey getPublicKey() {
        return WpcKey.getPubKey(Arrays.copyOfRange(mCrt, OFS_PUB, OFS_SIG));                        // Return the public key of the WPC certificate
    }

    /**
     * Returns the WPC Certificate type
     *
     * @return  The WPC Certificate type
     */
    public int getTyp() {
        return mCrt[OFS_TYP] & TYP_TPU;                                                             // Returns the WPC Certificate type
    }

    /**
     * Verify the Certificate file
     *
     * @param   file    The WPC Certificate file
     */
    public static void verifyCrt(@NonNull File file) {
        WpcLog.begLog("Verify WPC Certificate: ");                                                  // Log Certificate Chain log
        try {
            @NonNull String name = file.getName();                                                  // Get name of WPC Certificate file
            @NonNull byte[] ba = new WpcFil(file).read();                                           // Get content of WPC Certificate file
            WpcLog.logCmt(name + " is read.");                                                      // Log time to read fWPC Certificate ile
            WpcLog.log(WpcLog.EvtTyp.CRT, ba);                                                      // Show the WPC Certificate
            final @NonNull WpcCrt crt = new WpcCrt(ba, WpcMan.ROOT_MAN);                            // Create WPC Certificate
            final int type = crt.getTyp();                                                          // Get the Certificate type
            switch (type) {                                                                         // Analyze the Certificate type
                case TYP_ROOT:                                                                      // Root Certificate
                    WpcLog.logCmt("Root Certificate Type");                                         // Log Root Certificate type
                    byte[] id = WpcCrt.getId(TYP_CRT);                                              // Get expected ID
                    if (!Arrays.equals(id, crt.getIid())) {                                         // Wrong Issuer ID?
                        WpcLog.logErr("Wrong Issuer ID!");                                          // Log Error
                        throw new IOException();                                                    // Generate error
                    }
                    WpcLog.logCmt("Issuer ID is correct!");                                         // Log Correct Issuer ID
                    if (!Arrays.equals(id, crt.getSid())) {                                         // Wrong Subject ID?
                        WpcLog.logErr("Wrong Subject ID!");                                         // Log Error
                        throw new IOException();                                                    // Generate error
                    }
                    WpcLog.logCmt("Subject ID is correct!");                                        // Log Correct Subject ID
                    crt.verify(crt.getPublicKey());                                                 // Verify Root Certificate
                    WpcLog.logCmt("Signature of WPC Root Certificate is correct!");                 // Log success
                    break;
                case TYP_INT:                                                                       // Intermediate Certificate type
                    id = WpcCrt.getId(TYP_CRT);                                                     // Get expected Issuer ID for Manufacturer Certificate
                    final @NonNull byte[] iid = crt.getIid();                                       // Get the Issuer ID
                    if (Arrays.equals(id, iid)) {                                                   // Issuer ID from WPC Root?
                        WpcLog.logCmt("Manufacturer Certificate Type");                             // Log Manufacturer Certificate type
                        WpcLog.logCmt("Issuer ID is correct!");                                     // Log Correct Issuer ID
                        if (crt.getMan(crt.getSid()) == WpcMan.ERR_MAN) {                           // In Correct Subject ID?
                            WpcLog.logErr("Wrong Subject ID!");                                     // Log Error
                            throw new IOException();                                                // Generate error
                        }
                        WpcLog.logCmt("Subject ID is correct!");                                    // Log correct Subject ID
                        crt.verify(PUB_CA);                                                         // Verify Manufacturer Certificate
                        WpcLog.logCmt("Signature of WPC Manufacturer Certificate is correct!");     // Log success
                    } else {                                                                        // Secondary Certificate Type
                        WpcLog.logCmt("Secondary Certificate Type");                                // Log Secondary Certificate type
                        if (crt.getMan(iid) == WpcMan.ERR_MAN) {                                    // Incorrect Issuer ID?
                            WpcLog.logErr("Wrong Issuer ID!");                                      // Log Error
                            throw new IOException();                                                // Generate error
                        }
                        WpcLog.logCmt("Issuer ID is correct!");                                     // Log Correct Issuer ID
                    }
                    break;
                case TYP_TPU:                                                                       // Product Unit Certificate Type
                    WpcLog.logCmt("Product Unit Certificate type");                                 // Log Product Unit Certificate type
                    break;
                default:                                                                            // Unknown certificate type
                    WpcLog.logErr("Unknown Certificate type!");                                     // Log Error
                    throw new IOException();                                                        // Generate error
            }
            WpcLog.logCmt(Dbg.hexStr("Digest", crt.getDatDig()));                                   // Log WPC Root certificate digest
            name = name.substring(0, name.indexOf(EXT_CRT)) + EXT_PRV;                              // Create file name for corresponding private key
            file = new File(file.getParentFile(), name);                                            // Create file header for private key file
            if (file.exists()) {                                                                    // Exists the private key file?
                name = file.getName();                                                              // Get name of private key file
                ba = new WpcFil(file).read();                                                       // Get content of WPC Certificate file
                WpcLog.logCmt(name + " is read.");                                                  // Log time to read private key file
                WpcLog.logCmt(Dbg.hexStr("Private Key", ba));                                       // Show the read private key
                PublicKey pub = WpcKey.getPubKey(WpcKey.getPrvKey(ba));                             // Calculate the corresponding public key
                WpcLog.logCmt(Dbg.hexStr("Public key", WpcKey.getKeyDat(pub)));                     // Show the public key
                if(!pub.equals(crt.getPublicKey())) {                                               // Wrong public key?
                    WpcLog.logErr("Private key does not match with poblic key of the Certificate!");// Log Error
                    throw new IOException();                                                        // Generate error
                }
                WpcLog.logCmt("Private key matches with public key of the Certificate");            // Log coorect public key
            }
            WpcLog.logCmt("Successful Certificate verification");                                   // Log successful verification
        } catch (GeneralSecurityException | IOException err) {                                      // An error occurred
            WpcLog.logErr("Unsuccessful Root Certificate verification");                            // Log error
        }
    }
}