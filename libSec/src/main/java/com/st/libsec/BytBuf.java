package com.st.libsec;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Byte Buffer optimized to manage unsigned values
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
public class BytBuf {

    private final @NonNull ByteBuffer   mBuf;                                                       // Own byte buffer

    /**
     * Returns the first signed integer value of an byte array
     *
     * @param buf   The byte array
     * @return      The first signed integer value of the byte array
     */
    public static int getInt(final @NonNull byte[] buf) {
        return ByteBuffer.wrap(buf).getInt();                                                       // Returns the first signed integer value
    }

    /**
     * Returns the first unsigned short value of an byte array
     *
     * @param buf   The byte array
     * @return      The first unsigned short value of the byte array
     */
    public static int getUsShort(final @NonNull byte[] buf) {
        return ByteBuffer.wrap(buf).getShort() & AppLib.SHT_UNS;                                    // Returns the first unsigned short value
    }

    /**
     * Initialises the byte buffer
     *
     * @param buf   The byte buffer
     */
    public BytBuf(final @NonNull byte[] buf) {
        mBuf = ByteBuffer.wrap(buf);                                                                // Create the byte buffer
    }

    /**
     * Initialises the byte buffer
     *
     * @param buf   The byte buffer
     */
    public BytBuf(final @NonNull byte[] buf, final @NonNull ByteOrder ord) {
        this(buf);                                                                                  // Create the byte buffer
        mBuf.order(ord);                                                                            // Set the endian order
    }

    /**
     * Get the byte array of the byte buffer
     *
     * @return  The byte array of the byte buffer
     */
    public @NonNull byte[] getArray() {
        return mBuf.array();                                                                        // Return the byte array
    }

    /**
     * Get the next byte array
     *
     * @param len   The length of the byte buffer
     * @return  The next byte array
     */
    public @NonNull byte[] getArray(int len) {
        final @NonNull byte[] dat = new byte[len];                                                  // Create the byte array
        mBuf.get(dat);                                                                              // Get the next byte array
        return dat;                                                                                 // Return the byte array
    }

    /**
     * Get the next signed byte value
     *
     * @return  The next signed byte value
     */
    public byte getByte() {
        return mBuf.get();                                                                          // Return the next signed byte value
    }

    /**
     * Get the next signed integer value
     *
     * @return  The next signed integer value
     */
    public int getInt() {
        return mBuf.getInt();                                                                       // Return the next signed integer value
    }

    /**
     * Get the next signed short value
     *
     * @return  The next signed short value
     */
    public short getSht() {
        return mBuf.getShort();                                                                     // Return the next signed short value
    }

    /**
     * Get the next unsigned byte value
     *
     * @return  The next unsigned byte value
     */
    public int getUsByte() {
        return mBuf.get() & AppLib.BYT_UNS;                                                         // Return the next unsigned byte value
    }

    /**
     * Get the next unsigned short value
     *
     * @return  The next unsigned short value
     */
    public int getUsShort() {
        return mBuf.getShort() & AppLib.SHT_UNS;                                                    // Return the next unsigned short value
    }

    /**
     * Returns the number of remaining bytes
     *
     * @return  The number of remaining bytes
     */
    public int rest() {
        return mBuf.remaining();                                                                    // Returns the number of remaining bytes
    }

}