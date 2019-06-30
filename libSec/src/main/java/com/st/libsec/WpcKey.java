package com.st.libsec;

import android.support.annotation.NonNull;

import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.signers.DSAKCalculator;
import org.spongycastle.crypto.signers.ECDSASigner;
import org.spongycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.ECPointUtil;
import org.spongycastle.jce.interfaces.ECPrivateKey;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPrivateKeySpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.BigIntegers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;

import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;

/**
 * WPC key handling class
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
public class WpcKey {

    /** Algorithm provider */
    static final BouncyCastleProvider ALG_PRV = new BouncyCastleProvider();

    /** Size of SHA-256 result */
    public static final int DIG_SIZ = 256 / AppLib.BIT_BYT;

    /** Size of EC P-256 keys */
    public static final int KEY_SIZ = 256 / AppLib.BIT_BYT;

    /** Elliptic curve */
    static final String EC_CRV = "secp256r1";

    /** Elliptic curve digital signature */
    static final String EC_DSA = "ECDSA";

    private static final KeyFactory       KEY_GEN;                                                  // Key genaerator for ECDSA keys
    private static final MessageDigest    MSG_DIG;                                                  // Message Digest for SHA-256

    static {
        try {
            KEY_GEN = KeyFactory.getInstance(EC_DSA, ALG_PRV);                                      // Get key generator
            MSG_DIG = MessageDigest.getInstance("SHA-256", ALG_PRV);                                // Return message digest algorithm
        } catch (GeneralSecurityException err) {                                                    // Algorithm are not available (should never happen)
            throw new ProviderException("Algorithm is not available!", err);                        // Raise error
        }
    }

    /**
     * Returns the private key for a NIST P-256 curve
     *
     * @param   key The private key
     * @return  The private key for a NIST P-256 curve
     */
    static @NonNull PrivateKey getPrvKey(final @NonNull BigInteger key) {
        final @NonNull ECParameterSpec crv = ECNamedCurveTable.getParameterSpec(EC_CRV);            // Get NIST P-256 curve parameter
        try {
            return KEY_GEN.generatePrivate(new ECPrivateKeySpec(key, crv));                         // Return private key
        } catch(InvalidKeySpecException err) {                                                      // Error occured (should never happen)
            throw new ProviderException("Invalid Key specification", err);                          // Raise the error
        }
    }

    /**
     * Returns the private key for a NIST P-256 curve
     *
     * @param   prv The byte array of the private key
     * @return  The private key for a NIST P-256 curve
     */
    public static @NonNull PrivateKey getPrvKey(@NonNull byte[] prv) {
        if (prv[0] < 0) {                                                                           // Negative private key?
            final @NonNull ByteBuffer buf = ByteBuffer.wrap(new byte[prv.length + AppLib.BYT_SIZ]); // Generate buffer for unsigned private key
            buf.put((byte)0x00);                                                                    // Add leading zero byte
            buf.put(prv);                                                                           // Add private key
            prv = buf.array();                                                                      // Get unsigned private key
        }
        return getPrvKey(new BigInteger(prv));                                                      // Return the private key
    }

    /**
     * Generates the compressed public EC key
     *
     * @param   key The EC public key
     * @return  The compressed public key
     */
    static @NonNull byte[] getComKey(final @NonNull PublicKey key) {
        return ((ECPublicKey)key).getQ().getEncoded(true);                                          // Returns the compressed public key
    }

    /**
     * Returns the byte array of a big integer without the leading 0 for negative values
     *
     * @param   bi  The big integer
     * @param   siz The targeted size of the byte array
     * @return  The byte array of the big integer
     */
    public static @NonNull byte[] getInt(final @NonNull BigInteger bi, final int siz) {
        return BigIntegers.asUnsignedByteArray(siz, bi);                                            // returns the unsigned byte array of the big integer
    }

    /**
     * Calculates the public key from a NIST P-256 private key
     *
     * @param   prv The private key
     * @return  The public key for a NIST P-256 curve
     */
    public static @NonNull PublicKey getPubKey(final @NonNull PrivateKey prv) {
        final @NonNull ECParameterSpec par = ECNamedCurveTable.getParameterSpec(EC_CRV);            // Get NIST P-256 curve parameter
        final @NonNull ECPoint q = par.getG().multiply(((ECPrivateKey)prv).getD());                 // Get Q point
        return getPubKey(par, par.getCurve().decodePoint(q.getEncoded(false)));                     // Return the public key
    }

    /**
     * Calculates the public key for a compressed NIST P-256 public key
     *
     * @param   key The compressed public key
     * @return  The public key for a NIST P-256 curve
     */
    static @NonNull PublicKey getPubKey(final @NonNull byte[] key) {
        final @NonNull ECParameterSpec par = ECNamedCurveTable.getParameterSpec(EC_CRV);            // Get NIST P-256 curve parameter
        final @NonNull ECCurve ec = par.getCurve();                                                 // Get elliptic curve
        final @NonNull EllipticCurve crv = EC5Util.convertCurve(ec, par.getSeed());                 // Convert elliptic curve
        return getPubKey(par, EC5Util.convertPoint(ec, ECPointUtil.decodePoint(crv, key), true));   // Return the public key
    }

    /**
     * Returns the public key for a NIST P-256 curve
     *
     * @param   par The NIST P-256 curve parameter
     * @param   pnt The elliptic curve point
     * @return  The public key for a NIST P-256 curve
     */
    private static @NonNull PublicKey getPubKey(final @NonNull ECParameterSpec par, final @NonNull ECPoint pnt) {
        try {
            return KEY_GEN.generatePublic(new ECPublicKeySpec(pnt, par));                           // Return the public key
        } catch (InvalidKeySpecException err) {                                                     // Error occurred (should never happen)
            throw new ProviderException("Invalid key specification", err);                          // Raise the error
        }
    }

    /**
     * Returns the byte array of an uncompressed public key
     *
     * @param   pub The public key
     * @return  The byte array of the uncompressed public key
     */
    static @NonNull byte[] getKeyDat(final @NonNull PublicKey pub) {
        final @NonNull ByteArrayOutputStream bas = new ByteArrayOutputStream();                     // Byte stream for uncompressed public key
        final @NonNull ECPoint pnt = ((ECPublicKey)pub).getQ();                                     // Get the Q point of the public key
        try {
            bas.write(0x04);                                                                        // Write header for uncompressed public key
            bas.write(pnt.getAffineXCoord().getEncoded());                                          // Add the X coordinate
            bas.write(pnt.getAffineYCoord().getEncoded());                                          // Add the Y coordinate
        } catch (IOException err) {                                                                 // Error should never occur
            Dbg.log("Cannot get byte array of public key", err);                                    // Log error
        }
        return bas.toByteArray();                                                                   // return byte array of uncompressed public key
    }

    /**
     * Calculate the Digest of a given message with SHA-256
     *
     * @param   msg The given message
     * @return      The Digest of the given message
     */
    static @NonNull byte[] getDig(final byte[] msg) {
        return MSG_DIG.digest(msg);                                                                 // Return the digest of message
    }

    /**
     * Helper class to define a signature with given K value
     */
    private static class Kval implements DSAKCalculator {

        private BigInteger mK;                                                                      // The given K value

        /**
         * Initialize helper class to set the K value for the signature
         *
         * @param   k   The given K value for the signature
         */
        private Kval(BigInteger k) {
            mK = k;                                                                                 // Register the K value
        }

        /**
         * Informs if the signature is deterministic
         *
         * @return  true as the signature is deterministic with a fixed K value
         */
        @Override public boolean isDeterministic() {
            return true;                                                                            // Inform that the signature is deterministic
        }

        /**
         * Initialize the K value with a random value
         * Does nothing
         *
         * @param   n       The N value
         * @param   random  The random value
         */
        @Override public void init(BigInteger n, SecureRandom random) {}

        @Override public void init(BigInteger n, BigInteger d, byte[] message) {}

        /**
         * Returns the K value
         *
         * @return The K value
         */
        @Override public BigInteger nextK() {
            return mK;                                                                              // Returns the K value
        }
    }

    /**
     * Generate a deterministic signature with a fixed K value
     *
     * @param   prv     The private key
     * @param   hash    The Hash value for the signature
     * @param   k       The used K value
     * @return  The signature
     */
    static byte[] genSig(@NonNull PrivateKey prv, @NonNull byte[] hash, @SuppressWarnings("SameParameterValue") @NonNull BigInteger k) {
        WpcLog.logCmt(Dbg.hexStr("Sig. hash", hash));                                               // Log hash value
        final ECParameterSpec crv = ECNamedCurveTable.getParameterSpec(EC_CRV);                     // Get NIST P-256 curve parameter
        ECDomainParameters par = new ECDomainParameters(crv.getCurve(), crv.getG(), crv.getN());    // Convert Curve parameter
        ECDSASigner sig = new ECDSASigner(new Kval(k));                                             // Create signature algorithm
        sig.init(true, new ECPrivateKeyParameters(((ECPrivateKey)prv).getD(), par));                // Initialize signature algorithm
        BigInteger[] res = sig.generateSignature(hash);                                             // Get the signature
        byte[] r = getInt(res[0], KEY_SIZ);                                                         // Extract R value
        byte[] s = getInt(res[1], KEY_SIZ);                                                         // Extract S value
        WpcLog.logCmt(Dbg.hexStr("Sig. r", r));                                                     // Log R value
        WpcLog.logCmt(Dbg.hexStr("Sig. s", s));                                                     // Log S value
        ByteBuffer buf = ByteBuffer.wrap(new byte[WpcCrt.LEN_SIG]);                                 // Create signature buffer
        buf.put(r);                                                                                 // Add R value
        buf.put(s);                                                                                 // Add S value
        return buf.array();                                                                         // Return the signature
    }

    /**
     * Logs the content of a private key
     *
     * @param   prv The private key
     */
    static void logPrvKey(PrivateKey prv) {
        Dbg.log(Dbg.seqHex("Private key: ", getInt(((ECPrivateKey)prv).getD(), KEY_SIZ)));
    }
}