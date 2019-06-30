package com.st.libsec;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class to manage communication with binary files
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
public class BinFil {

    public File mFil;                                                                              // File header of binary file

    /**
     * Interface to receive file data
     */
    public interface FilRcv {

        /**
         * Called when a binary file is completely read
         * @param buf   The read file data
         */
        void onLoad(byte[] buf);
    }

    /**
     * Interface to save file data
     */
    public interface FilSav {

        /**
         * Called when a binary file is completely saved
         * @param err  The exception during saving a file
         */
        void onSave(Exception err);
    }

    /* Handler to for reach end of file when reading it
     */
    private static class RcvHnd extends Handler {

        private Activity    mAct;                                                                   // Activity showing the actual user interface
        private FilRcv      mRcv;                                                                   // Receiver of the file data

        /* Create a handler to handle the end of a read binary file
         * @param rcv   The receiver of the file data
         */
        private RcvHnd(Activity act, FilRcv rcv) {
            mAct = act;
            mRcv = rcv;                                                                             // Remember the receiver of the file data
        }

        /**
         * Called when a binary file id completely read
         * @param msg   The message containing the read file data
         */
        @Override
        public void handleMessage (Message msg) {
            if (msg.what == Dbg.NO_ERR) {                                                        // No error occurred?
                mRcv.onLoad((byte[]) msg.obj);                                                      // Send the received file data to the file data receiver
            } else {                                                                                // A read error occurred
                AppLib.showErrTst(mAct, R.string.lib_err_rd, msg.what);                             // Show error message
            }
        }
    }

    /* Handler for completely saved file
     */
    private static class SavHnd extends Handler {
        private FilSav mRcv;                                                                        // Receiver of the file data

        /* Create a handler to handle the end of a read binary file
         * @param rcv   The receiver of the file data
         */
        private SavHnd(FilSav rcv) {
            super(Looper.getMainLooper());
            mRcv = rcv;                                                                             // Remember the receiver of the file data
        }

        /**
         * Called when a binary file id completely saved
         * @param msg   The message containing the error message
         */
        @Override
        public void handleMessage (Message msg) {
            mRcv.onSave((Exception)msg.obj);                                                         // Send the received exception to the file data receiver
        }
    }

    /**
     * Class to read a binary file within a background task
     */
    protected class RdFil implements Runnable {

        private Handler mHnd;                                                                        // Handler for end of file reading

        /**
         * Creates the background thread to read a binary file
         * @param   rcv The file data receiver
         */
        protected RdFil(Activity act, FilRcv rcv) {
            mHnd = new RcvHnd(act, rcv);                                                                 // Create the handler for end of file reading
        }

        /**
         * Creates the background thread to read a binary file
         * @param   hnd The handler managing the received file data
         */
        protected RdFil(Handler hnd) {
            mHnd = hnd;                                                                             // Remember the handler to manage the received file data
        }

        /**
         * Background thread to read the data of a binary file
         */
        @Override
        public void run() {
            Message msg = new Message();                                                            // Create message for handler
            try {
                msg.what = Dbg.NO_ERR;                                                           // Assume no reading error
                msg.obj = readall();                                                                // Read the file data
            } catch (FileNotFoundException err) {                                                   // Error during opening the file
                Dbg.log("File cannot be opened", err);                                           // Log error during file opening
                msg.what = R.string.lib_err_op;                                                     // Report error during file opening
            } catch (EOFException err) {                                                            // File is too short
                Dbg.log("File is too short", err);                                               // Log error during reading file
                msg.what = R.string.lib_err_sht;                                                    // Report that file is too short
            } catch (IOException err) {                                                             // IO Error during reading file
                Dbg.log("Error during reading file", err);                                       // Log error during reading file
                msg.what = R.string.lib_err_com;                                                    // Report communication problem
            }
            mHnd.sendMessage(msg);                                                                  // Send the message to the end of file handler
        }
    }

    /*
     * Class to write a binary file inside of a background thread
     */
    private class WrtFil implements Runnable {

        private byte[] mBuf;                                                                        // Binary data to be written
        private SavHnd mHnd;                                                                        // Handler for saved file

        /* Creates the background thread to write data into the file
         * @param buf   The file data to be written
         */
        private WrtFil(byte[] buf, FilSav rcv) {
            mBuf = buf;                                                                             // Remember the file data
            mHnd = new SavHnd(rcv);                                                                 // Create the handler for end of file reading
        }

        /**
         * Execute the background thread
         */
        @Override
        public void run() {
            FileOutputStream fos = null;                                                            // Initialite the file output stream
            Message msg = new Message();
            msg.obj = null;
            try {
                mFil.createNewFile();                                                               // Create the file if not existing
                fos = new FileOutputStream(mFil);                                                   // Create file
                fos.write(mBuf);                                                                    // Write file data
            } catch (FileNotFoundException err) {                                                   // Error during creation of file
                Dbg.log("Creation of binary file failed", err);                                  // Log error during file creation
                msg.obj = err;
            } catch (IOException err) {                                                             // Error during writing file
                Dbg.log("Error during writing binary file", err);                                // Log error during writing file
                msg.obj = err;
            } finally {                                                                             // Execute in any case
                try {
                    if (fos != null) {                                                              // file created?
                        fos.close();                                                                // Close file
                    }
                } catch (IOException err) {                                                         // Error during closing file
                    Dbg.log("Cannot close binary file", err);                                    // Log error during closing file
                    msg.obj = err;
                }
            }
            mHnd.sendMessage(msg);
        }
    }

    /**
     * Standard constructor allowing sub classes
     */
    protected BinFil() {
    }


    /**
     * Create a binary file
     * @param   fil The file header data
     */
    public BinFil(File fil) {
        mFil = fil;                                                                                 // Remember the file header
    }

    /**
     * Deletes a binary file
     */
    public void delete() {
        if(!mFil.delete()) {                                                                        // Deletion was not successful?
            Dbg.log("Cannot delete file");                                                       // Log that file deletion was not successful
        }
    }

    /**
     * Removes the file extension from a given file name
     * @param   nam The file name
     * @return  The file name without an extension
     */
    protected String delExt(String nam) {
        int len = nam.lastIndexOf('.');                                                             // Index of extension
        if (len > 1) {                                                                              // Extension found?
            return nam.substring(0, len);                                                           // Return file name without extension
        }                                                                                           // No extension was found
        return nam;                                                                                 // return unchanged file name
    }

    /**
     * Return the header of the binary file
     * @return  The file header
     */
    public File getHead() {
        return mFil;                                                                                // returns the file header
    }

    /**
     * Reads a binary file inside of a background thread
     * @param rcv   The file data receiver
     */
    public void read(Activity act, FilRcv rcv) {
        new Thread(new RdFil(act, rcv)).start();                                                         // Read the file data inside of a background thread
    }

    /**
     * Reads a binary file inside of a background thread
     * @param hnd   The handler managing the read file data
     */
    public void read(Handler hnd) {
        new Thread(new RdFil(hnd)).start();                                                         // Read the file data inside of a background thread
    }

    /**
     * Reads a binary file.
     * This method is blocking and should not be called at the UI thread
     * @return  The read file data
     */
    public byte[] read() throws IOException {
        FileInputStream fis = null;                                                                 // Initialize file input stream
        int len = (int)mFil.length();                                                               // Get file length
        int ofs = 0;                                                                                // Initialize file offset
        byte[] buf = new byte[len];                                                                 // Create data area for the binary file
        try {
            fis = new FileInputStream(mFil);                                                        // Open binary file
            while (len > 0) {                                                                       // Repeat until complete file is read
                ofs = fis.read(buf, ofs, len);                                                      // Read file
                if (ofs < 0) {                                                                      // File is shorter than expected?
                    throw new EOFException();                                                       // Throw end of file exception
                }
                len = len - ofs;                                                                    // Calculate remaining length to read
            }
        } catch (FileNotFoundException err) {                                                       // Error during opening the file
            Dbg.log("File cannot be opened", err);                                                  // Log error during file opening
            throw err;                                                                              // Return no file data
        } catch (IOException err) {                                                                 // IO Error during reading file
            Dbg.log("Error during reading file", err);                                              // Log error during reading file
            delete();                                                                               // Delete the erronous file
            throw err;                                                                              // Return no data
        } finally {
            if (fis != null) {                                                                      // NFC file opened?
                fis.close();                                                                        // Close NFC file
            }
        }
        return buf;                                                                                 // Return file data
    }

    /**
     * Reads a binary file.
     * This method is blocking and should not be called at the UI thread
     * @return  The read file data
     */
    public byte[] readall() throws IOException {
        int len = (int)mFil.length();                                                               // Get file length
        byte[] buf = new byte[len];                                                                 // Create data area for the binary file
        read(new FileInputStream(mFil), buf, 0, len);                                               // Read binary file
        return buf;                                                                                 // Return file data
    }

    /**
     * Reads a binary file
     * This method is blocking and should not be called at the UI thread
     * @param   is  The input stream of the binary file
     * @param   buf The buffer for the read file data
     * @param   ofs The offset where the file starts to be read
     * @param   len The number of bytes to be read
     * @throws  IOException  In case of errors while reading the binary file
     */
    public static void read(InputStream is, byte[] buf, int ofs, int len) throws IOException {
        while (len > 0) {                                                                           // Repeat until complete file is read
            ofs = is.read(buf, ofs, len);                                                           // Read file
            if (ofs < 0) {                                                                          // File is shorter than expected?
                throw new EOFException();                                                           // Throw end of file exception
            }
            len = len - ofs;                                                                        // Calculate remaining length to read
        }
        is.close();                                                                                 // Close file
    }

    /**
     * Write data into a file by using a background thread
     * @param buf   The file data to be written
     */
    public void write(byte[] buf, FilSav rcv)
    {
        new Thread(new WrtFil(buf, rcv)).start();                                                   // Write the data into the file by using a background thread
    }
}
