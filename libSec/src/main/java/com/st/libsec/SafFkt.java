package com.st.libsec;

import android.support.annotation.NonNull;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.jce.ECNamedCurveTable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;

import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

/**
 * Security function class
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
public class SafFkt {

    private static final Signature ALG_DSA;                                                         // Digital signature algorithm
    private static final KeyPairGenerator KEY_GEN;                                                  // Key pait generator

    static {
        try {
            ALG_DSA = Signature.getInstance("NONEwithECDSA", WpcKey.ALG_PRV);                       // Use ECDSA algorithm without using a Digest as digital signature
            KEY_GEN = KeyPairGenerator.getInstance(WpcKey.EC_DSA, WpcKey.ALG_PRV);                  // Use ECDSA key pair generator
            KEY_GEN.initialize(ECNamedCurveTable.getParameterSpec(WpcKey.EC_CRV));                  // Initialize the ECDSA key pait generator
        } catch (GeneralSecurityException err) {                                                    // Error occurred (should never happen)
            throw new ProviderException(err);                                                       // Raise error
        }
    }

    /**
     * Converts a raw signature into a DER coded signature
     *
     * @param   raw The raw data of the signature
     * @return  The DER coded signature
     */
    private static @NonNull byte[] getDer(@NonNull byte[] raw) {
        ASN1Integer[] sig = new ASN1Integer[2];                                                     // Create two big integers
        @NonNull BigInteger bi = new BigInteger(1, Arrays.copyOf(raw, WpcKey.KEY_SIZ));             // Get R
        sig[0] = new ASN1Integer(bi.toByteArray());                                                 // Create R
        bi = new BigInteger(1, Arrays.copyOfRange(raw, WpcKey.KEY_SIZ, WpcCrt.LEN_SIG));            // Get S
        sig[1] = new ASN1Integer(bi.toByteArray());                                                 // Create S
        DERSequence seq = new DERSequence(sig);                                                     // Create DER sequence
        try {
            return seq.getEncoded();                                                                // Return DER coded signature
        } catch(IOException err) {                                                                  // Error occurred (should never happen)
            throw new ProviderException("Cannot generate DER coded signature!", err);               // Raise error
        }
    }

    /**
     * Generates new key pair for NIST-256 curve
     *
     * @return  The new key pair
     */
    public static @NonNull KeyPair getPair() {
        return KEY_GEN.generateKeyPair();                                                           // Return the new key pair
    }

    /**
     * Converts a DER coded signature into a raw format
     *
     * @param   der The DER coded signature
     * @return  The raw format of the signature
     */
    private static @NonNull byte[] getRaw(@NonNull byte[] der) {
        ByteArrayOutputStream bas = new ByteArrayOutputStream(WpcCrt.LEN_SIG);                      // Create Raw byte stream
        try {
            for (ASN1Encodable prt : ASN1Sequence.getInstance(der).toArray()) {                     // Repeat for both integers
                bas.write(WpcKey.getInt(((ASN1Integer)prt).getValue(), WpcKey.KEY_SIZ));            // Add integer
            }
            return bas.toByteArray();                                                               // Return raw bytes of the signature
        } catch (IOException err) {                                                                 // Error occurred (should never happen)
            throw new ProviderException("Cannot generate raw byte array of signature!", err);       // Raise error
        }
    }

    /**
     * Returns a nonce
     *
     * @param   len The length of the nonce [bytes]
     * @return  The nonce
     */
    public static byte[] getRnd(int len) {
        byte[] rnd = new byte[len];                                                                 // Create the nonce
        new SecureRandom().nextBytes(rnd);                                                          // Fill the nonce
        return rnd;                                                                                 // Return the nonce
    }

    /**
     * Generate a P-256 signature for a message digest
     *
     * @param   dig The message digest
     * @param   key The private key
     * @return  The signature
     * @throws  GeneralSecurityException When an error occurred during signature calculation
     */
    public static byte[] genSig(byte[] dig, PrivateKey key) throws GeneralSecurityException {
        ALG_DSA.initSign(key);                                                                      // Set the private key for the signature
        ALG_DSA.update(dig);                                                                        // Set the data to be signed
        return getRaw(ALG_DSA.sign());                                                              // Return the signature
    }

    /**
     * Verify a P-256 signature for a given message digest
     *
     * @param   dig The message digest
     * @param   sig The signature
     * @param   key The public key to verify the signature
     * @throws  InvalidKeyException when the public key is not valid
     * @throws  SignatureException when an error during signature verification occurred
     */
    public static void verSig(byte[] dig, byte[] sig, PublicKey key) throws InvalidKeyException, SignatureException {
        ALG_DSA.initVerify(key);                                                                    // Set the public key for the signature verification
        ALG_DSA.update(dig);                                                                        // Set the data for the signature verification
        if (!ALG_DSA.verify(getDer(sig))) {                                                         // Wrong signature?
            throw new SignatureException();                                                         // Throw signature exception
        }
    }
}