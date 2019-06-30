package com.st.wpcath;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.spongycastle.jce.interfaces.ECPrivateKey;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;

import com.st.libsec.AppLib;
import com.st.libsec.Dbg;
import com.st.libsec.SafFkt;
import com.st.libsec.WpcAthIni;
import com.st.libsec.WpcAthRsp;
import com.st.libsec.WpcCrt;
import com.st.libsec.WpcCrtChn;
import com.st.libsec.WpcFil;
import com.st.libsec.WpcKey;
import com.st.libsec.WpcLog;
import com.st.libsec.WpcPtx;
import com.st.libsec.WpcQiId;


/**
 * Virtual plugfest fragment
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
public class PlgFst extends ListFragment {

    /** Directory name for Qi-Authentication virtual plugfest */
    static final String DIR_PF  = "pf2";

    private static final String NAM_RT  = "Root";                                                   // Directory for Qi-Authentication virtual plugfest

    /** Root directory for virtual plugfest */
    static final String DIR_RT  = DIR_PF + File.separator + NAM_RT;

    private static final String AUTH    = "_CHALLENGE_AUTH";                                        // File name indicator for CHALLENGE AUTH files
    private WpcCrtChn   mChn;                                                                       // Actual WPC Certificate Chain
    private File        mDir;                                                                       // Actual subdirectory for virtual plugfest
    private String      mLog;                                                                       // Actual proposed log file name

    /**
     * Listener for check button
     */
    private class BtnLst implements View.OnClickListener {

        /**
         * Called when the check button was clicked
         * Checks the selected CHALLENGE request and its CHALLENGE_AUTH response
         *
         * @param v The check button (not used here)
         */
        @Override public void onClick(View v) {
            if (mDir.getName().equals(NAM_RT)) {                                                    // Root folder selected?
                //noinspection ConstantConditions                                                   // The layout of the virtual plugfest fragment will be always available
                Spinner sel = getView().findViewById(R.id.spn_req);                                 // Get the spinner for the WPC Certificate file
                String name = (String)((TextView)sel.getSelectedView()).getText();                  // Get the name of the WPC Certificate file
                WpcCrt.verifyCrt(new File(mDir, name));                                             // Verify the WPC Certificate file
                mLog = name.substring(0, name.indexOf(WpcCrt.EXT_CRT));                             // Propose Logfile name
            } else {                                                                                // Another folder selected
                WpcLog.begLog("Verify Qi Authentication");                                          // Start logging of this check
                WpcLog.logCmt("Certificate: " + mChn.toString());                                   // Log success
                try {
                    final byte[] req = getMsg(R.id.spn_req, 0x1B);                                  // Read the CHALLENGE Request
                    final byte[] rnd = Arrays.copyOfRange(req, WpcAthIni.OFS_RND, WpcAthIni.OFS_RND + WpcAthIni.RND_SIZ);
                    WpcLog.logCmt(Dbg.hexStr("Nonce", rnd));                                        // Show nonce of CHALLENGE Request
                    final byte[] res = getMsg(R.id.spn_res, 0x13);                                  // Read the CHALLENGE_AUTH Response
                    if (res[1] != 0x11) {                                                           // Wrong version or message type
                        WpcLog.logErr("Wrong maximum version or Slots Populated Mask");             // Log error
                        throw new IOException();                                                    // Generate error
                    }
                    WpcLog.logCmt("Correct maximum version and Slots Populated Mask");              // Log correct message type
                    final byte[] dig = mChn.getDig();                                               // Get Certificate Chain Hash
                    if (res[2] != dig[WpcKey.DIG_SIZ - 1]) {                                        // Wrong Certificate Chain Hash LSB
                        WpcLog.logErr("Wrong Certificate Chain Hash LSB");                          // Log error
                        throw new IOException();                                                    // Generate error
                    }
                    WpcLog.logCmt("Correct Certificate Chain Hash LSB");                            // Log correct WPC Device type
                    final byte[] ath = Arrays.copyOfRange(res, 0, WpcAthRsp.LEN_ATH);               // Get the CHALLENGE_AUTH Response header
                    final byte[] sig = Arrays.copyOfRange(res, WpcAthRsp.LEN_ATH, res.length);      // Get the signature from the CHALLENGE_AUTH Response
                    SafFkt.verSig(WpcAthRsp.getSigDig(dig, req, ath), sig, mChn.getPu().getPublicKey());// Verification of the signature
                    WpcLog.logCmt("Correct signature");                                             // Log success
                } catch(IOException err) {                                                          // An error occurred
                    WpcLog.logErr("Wrong Qi Authentication message");                               // Log the unsuccessful Qi Authentication
                } catch(GeneralSecurityException err) {                                             // An error occurred
                    WpcLog.logErr("Wrong signature");                                               // Log wrong signature
                }
            }
            WpcLog.endLog(PlgFst.this, new File(mDir, mLog));                                       // Log Successful Qi Authentication
        }

        /**
         * Reads an Qi Authentication message from a WPC authentication message text file
         *
         * @param   spn The Resource identifier of the spinner containing the name of the WPC authentication message text file
         * @return  The Qi Authentication message
         * @throws  IOException in case the Qi Authentication message cannot be read from the text file
         */
        private @NonNull byte[] getMsg(@IdRes int spn, int typ) throws IOException{
            //noinspection ConstantConditions                                                       // The layout of the virtual plugfest fragment will be always available
            Spinner sel = getView().findViewById(spn);                                              // Get the spinner for the message
            String nam = (String)((TextView)sel.getSelectedView()).getText();                       // Get the name of the message file
            final byte[] msg;                                                                       // Qi Authentication message
            if (spn == R.id.spn_res) {                                                              // Authentication Response message?
                mLog = nam.substring(0,nam.toUpperCase().indexOf(AUTH));                            // Propose log file name
            }
            try {
                msg = new WpcFil(new File(mDir, nam)).read();                                       // Read the Qi Authentication message
            } catch(IOException err) {                                                              // An error occurred during reading a file
                WpcLog.logErr("Cannot read " + nam);                                                // Log error
                throw err;                                                                          // Generate error
            }
            WpcLog.logCmt(nam + " is read.");                                                       // Log time to read file
            if (msg[0] != typ) {                                                                    // Wrong version or message type
                WpcLog.logErr("Wrong version or Message Type");                                     // Log error
                throw new IOException();                                                            // Generate error
            }
            WpcLog.logCmt("Correct version and Message Type");                                      // Log success
            return msg;                                                                             // Return the Qi Authentication message
        }
    }

    /**
     * Listener for long click on check button
     */
    private class LngChkLst implements View.OnLongClickListener {

        /**
         * Called when the check button was clicked for a long time
         * Saves a new CHALLENGE Request text file or a new CHALLENGE_AUTH Response text file in case ST directory is selected
         *
         * @param   v   The check button (not used here)
         * @return  true to indicate that this event was processed
         */
        @Override public boolean onLongClick(View v) {
            String dir = mDir.getName();                                                            // Get the Qi Authentication subdirectory name
            byte[] msg = null;                                                                      // Qi Authentication message
            String nam;                                                                             // Text file name to be stored
            if(mChn == null) {                                                                      // No WPC Certificate chain available?
                return false;                                                                       // Save no file
            }
            if (dir.equals("ST")) {                                                                 // ST directory selected?
                //noinspection ConstantConditions                                                   // Layout of the plugfest fragment will be always available
                final Spinner spn = getView().findViewById(R.id.spn_req);                           // Get the spinner for the CHALLENGE Request text file
                if (spn.getAdapter().getCount() == 0) {
                    nam = "I_ST_to_R_ST_001_CHALLENGE_001.txt";                                     // Set file name
                    msg = WpcAthIni.getAth().array();                                               // Get CHALLENGE Request;
                } else {
                    nam = (String)((TextView)spn.getSelectedView()).getText();                      // Get the CHALLENGE Request text file name
                    final String cpy = nam.substring(2, nam.indexOf('_', 2));                       // Extract the company requesting the challenge
                    try {
                        final byte[] req = new WpcFil(new File(mDir, nam)).read();                  // Read the CHALLENGE Request
                        final ByteBuffer res = WpcAthRsp.getChAth(mChn);                            // Create the CHALLENGE_AUTH Response
                        final byte[] dig = WpcAthRsp.getSigDig(mChn.getDig(), req, res.array());    // Get Digest of TBSAuth
                        res.put(SafFkt.genSig(dig, WpcPtx.sPrv));                                   // Calculate the signature
                        nam = "R_ST_to_I_" + cpy + "_001_CHALLENGE_AUTH_001.txt";                   // Set the file name for the CHALLENGE_AUTH message
                        msg = res.array();                                                          // Get CHALLENGE_AUTH Response
                    } catch(GeneralSecurityException | IOException err) {                           // An error occurred
                        Toast.makeText(getActivity(), "Cannot save CHALLENGE_AUTH Response", Toast.LENGTH_LONG).show();
                        Dbg.log("Cannot save CHALLENGE_AUTH Response!", err);                       // Log the error
                    }
                }
            } else {                                                                                // Another directory is selected
                nam = "I_ST_to_R_" + dir + "_001_CHALLENGE_001.txt";                                // Set file name
                msg = WpcAthIni.getAth().array();                                                   // Get CHALLENGE Request;
            }
            if (msg != null) {                                                                      // Qi Authentication message generated?
                WpcFil fil = new WpcFil(new File(mDir, nam));                                       // Generate file header of WPC message text file
                try {
                    fil.save(Dbg.hexStr(msg).getBytes(AppLib.CHR_ASC));                             // Save Qi Authentication message
                    Toast.makeText(getActivity(), nam + " saved", Toast.LENGTH_LONG).show();        // Show successful storage of Qi Authentication message
                } catch (IOException err) {                                                         // An error occurred
                    Toast.makeText(getActivity(), "Cannot save " + nam, Toast.LENGTH_LONG).show();  // Show unsuccessful storage of Qi Authentication message
                }
            }
            return true;                                                                            // Indicate that this event was processed
        }
    }

    /**
     * Listener for selected spinner item
     */
    private class DirLst implements AdapterView.OnItemSelectedListener {

        /**
         * Called when an item of the spinner is selected
         * Sets the PRx device
         *
         * @param parent    The spinner view (not used here)
         * @param view      The item view
         * @param position  The position of the item in the spinner list
         * @param id        The item identifier (not used here)
         */
        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final @NonNull String dir = (String)((TextView)view).getText();                         // Get the actual selected subdirectory name
            final @NonNull String sub =  DIR_PF + File.separator + dir;                             // Subdirectory name for Qi Authentication
            //noinspection ConstantConditions                                                       // Main activity is here always available
            mDir = new File(getContext().getExternalFilesDir(null), sub);                           // Get the subdirectory for Qi Authentication
            final @NonNull File[] lst = mDir.listFiles();                                           // get all files of the subdirectory
            mChn = null;                                                                            // Clear last WPC Certificate Chain
            ArrayList<String> req = new ArrayList<>();                                              // CHALLENGE Request file list
            ArrayList<String> res = new ArrayList<>();                                              // CHALLENGE_AUTH Response file list
            if (dir.equals(NAM_RT)) {                                                               // WPC Root Certificate selected?
                String[] all = mDir.list();                                                         // Get all file names of subdirectory
                for (String str : all) {                                                            // Repeat for all files in the subdirectory
                    if (str.endsWith(WpcCrt.EXT_CRT)) {                                             // Certificate file found?
                        req.add(str);                                                               // Add file to the CHALLENGE_AUTH Response file list
                    }
                }
                WpcCrt.chkSpec();                                                                   // Calculate example Certificate
                mLog = "Example";                                                                   // Set proposed log file name for example calculation
            } else {                                                                                // Another WPC Certificate selected
                String[] all = mDir.list();                                                         // Get all file names of subdirectory
                for (String str : all) {                                                            // Repeat for all files in the subdirectory
                    String upc = str.toUpperCase();                                                 // Convert file name to uppercase
                    if (upc.contains(AUTH)) {                                                       // CHALLENGE_AUTH Response file found?
                        res.add(str);                                                               // Add file to the CHALLENGE_AUTH Response file list
                    } else if (upc.contains("_CHALLENGE")) {                                        // CHALLENGE Request file found?
                        req.add(str);                                                               // Add File to the CHALLENGE Request file list
                    }
                }
                verCrt(lst);                                                                        // Verify the WPC Certificate chain
                mLog = dir;                                                                         // Set proposed log file name for manufacturer certificate chain log
            }
            WpcLog.endLog(PlgFst.this, new File(mDir, mLog));                                       // Stop logging
            setSpn(R.id.spn_req, req);                                                              // Set the spinner for CHALLENGE Request
            setSpn(R.id.spn_res, res);                                                              // Set the spinner for CHALLENGE_AUTH Response
        }

        /**
         * Verify the WPC Certificate
         *
         * @param   lst File list of the WPC Certificate
         */
        private void verCrt(File[] lst) {
            mLog = "Certificate_Error";                                                             // Set initial log file name
            WpcLog.begLog("Verify WPC certificate Chain: ");                                        // Log Certificate Chain log
            try {
                for (File fil : lst) {                                                              // repeat for all files of the subdirectory
                    String nam = fil.getName();                                                     // Get file name
                    int ind = nam.toUpperCase().indexOf("_GENERIC_CERTIFICATE");
                    if (ind > 0) {                                                                  // Certificate file found?
                        mLog = nam.substring(0, ind);
                        byte[] ba = new WpcFil(fil).read();                                         // Get content of Certificate file
                        WpcLog.logCmt(nam + " is read.");                                           // Log time to read file
                        if (ba[0] != 0x12) {                                                        // Wrong version or message type?
                            WpcLog.logErr("Wrong version or Message Type");                         // Log error
                            throw new IOException();                                                // Generate error
                        }
                        WpcLog.logCmt("Correct version and Message Type");                          // Log success
                        final byte[] chn = Arrays.copyOfRange(ba, WpcAthRsp.OFS_CHN, ba.length);    // Get WPC Certificate chain
                        WpcLog.log(WpcLog.EvtTyp.CHN, chn);                                         // Log Certificate chain
                        mChn = new WpcCrtChn(chn);                                                  // Convert the WPC Certificate chain
                        int typ = mChn.getPu().getTyp();                                            // Get the type of the Product Unit certificate
                        mChn.verify();                                                              // Verify the WPC Certificate Chain
                        WpcLog.logCmt("WPC Certificate Chain is correct!");                         // Log success
                        if (typ == WpcCrt.TYP_TPU) {                                                // PTx Certificate?
                            WpcLog.logCmt("PTx: " + mChn.toString());                               // Log PTx Certificate
                        } else {                                                                    // Another Certificate
                            WpcLog.logCmt("PRx: " + mChn.toString());                               // Log PRx Certificate
                        }
                    }
                }
                if (mChn == null) {                                                                 // No Certificate file found?
                    throw new IOException();                                                        // Generate error
                }
                for (File fil : lst) {                                                              // Check all files of subdirectory
                    String nam = fil.getName();                                                     // Get file name
                    if (nam.toUpperCase().contains("_GENERIC_DIGEST")) {                            // Digests file found?
                        byte[] ba = new WpcFil(fil).read();                                         // read Digests file
                        WpcLog.logCmt(nam + " is read.");                                           // Log time to read file
                        if (ba[0] != 0x11) {                                                        // Wrong version or message type?
                            WpcLog.logErr("Wrong version or Message Type");                         // Log error
                            throw new IOException();                                                // Generate error
                        }
                        WpcLog.logCmt("Correct version and Message Type");                          // Log success
                        if (ba[1] != 0x11) {                                                        // Wrong slot masks?
                            WpcLog.logErr("Wrong slot masks");                                      // Log error
                            throw new IOException();                                                // Generate error
                        }
                        WpcLog.logCmt("Correct slot masks");                                        // Log success
                        byte[] dig = Arrays.copyOfRange(ba, WpcAthRsp.OFS_DIG, ba.length);          // Extract Digest
                        if (!Arrays.equals(mChn.getDig(), dig)) {                                   // Digest not correct?
                            WpcLog.logErr("Digest of WPC Certificate Chain is incorrect!");         // Log error
                            throw new SignatureException();                                         // Generate error
                        }                                                                           // Digest is correct
                        WpcLog.logCmt("Digest of WPC Certificate Chain is correct!");               // Log success
                    }
                }
                WpcLog.logCmt("Successful WPC Certificate Chain verification");                     // Log success
            } catch (GeneralSecurityException | IOException err) {                                  // An error occurred
                WpcLog.logErr("Unsuccessful WPC Certificate Chain verification");                   // Stop logging
            }
        }

        /**
         * Called when no item was selected
         * Does nothing
         *
         * @param parent    The spinner view
         */
        @Override public void onNothingSelected(AdapterView<?> parent) {}

        /**
         * Initilize the spinner to set the Qi Authentication message file list
         *
         * @param   spn Resource identifier of the spinner
         * @param   lst File list nameS for Qi Authentication message files
         */
        private void setSpn(@IdRes int spn, @NonNull ArrayList<String> lst) {
            //noinspection ConstantConditions                                                       // The layout of the plugfest fragment is always available
            Spinner sel = getView().findViewById(spn);                                              // Get the spinner for the Qi Authentication message text file
            ArrayAdapter<String> apt;                                                               // Adapter for the Qi Authentication message text files
            apt = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, lst);     // Create adapter the Qi Authentication message text files
            apt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);             // Set layout for the Qi Authentication message text files
            sel.setAdapter(apt);                                                                    // Register the menu for the Qi Authentication message text files
        }
    }

    /**
     * Called when the layout of the fragment shall be created
     *
     * @param inflater              The layout inflater of the app
     * @param container             The view group containing the fragment
     * @param savedInstanceState    Previous saved state of the fragment (not used here)
     * @return                      The layout of the fragment
     */
    @Override public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.plg_fst, container, false);                                // Create fragment view
    }

    /**
     * Called when the layout of the web link fragment was created
     *
     * @param view                  The web link fragment layout
     * @param savedInstanceState    The web link fragment state
     */
    @Override public void onViewCreated (@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);                                              // Initialize the fragment view
        //noinspection ConstantConditions                                                           // Main activity is here always available
        ((WpcAthAct)getActivity()).setTit(R.string.plg_fst);                                        // Set title of fragment
        final @NonNull Spinner spn = view.findViewById(R.id.spn_dir);                               // Get spinner for PRx device
        final @NonNull File dir = new File(WpcCrt.sDir, DIR_PF);                                    // Get directory for Qi Authentication plugfest
        final @NonNull ArrayAdapter<String> apt;                                                    // Adapter for subdirectories
        apt = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, dir.list());  // Create adapter for subdirectories
        apt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);                 // Set layout for the PRx device spinner drop down menu
        spn.setAdapter(apt);                                                                        // Register the menu for the PRx device spinner
        spn.setSelection(0);                                                                        // Set the spinner to the actual set PRx
        spn.setOnItemSelectedListener(new DirLst());                                                // Register Listener for new selected item
        final @NonNull Button btn = view.findViewById(R.id.btn_ath);                                // Get check button
        btn.setOnClickListener(new BtnLst());                                                       // Register Listener for pressed check button
//        btn.setOnLongClickListener(new LngChkLst());                                                // Register listener for long pressed check button
//        setPuCrt();                                                                                 // Create Product Unit Certificates
//        savManCrt(0xCACA, "Test");                                                                  // Create the WPC Manufacturer Test Certificate for
    }

    /**
     * Saves a new WPC manufacturer certificate for signature
     * @param   sid The Manufacturer code
     * @param   nam The name of the manufacturer
     */
    private void savTbsCrt(int sid, @NonNull String nam) {
        File dir = new File(WpcCrt.sDir, DIR_RT);                                                   // Get directory for the root certificate
        WpcFil fil = new WpcFil(dir, "Informal_plugfest_" + nam + "_TBS_cert");                     // Create TBS certificate file header
        KeyPair pair = SafFkt.getPair();                                                            // Generate key pair
        WpcCrt crt = new WpcCrt(sid, pair.getPublic());                                             // Generate WPC Manufacturer certificate for signature
        String str = Dbg.hexStr(crt.getEncoded());                                                  // Get WPC Manufacturer Certificate string
        Dbg.log("Manufacturer Certificate: " + str);                                                // Log WPC Manufacturer Certificate string
        try {
            fil.save(str.getBytes(AppLib.CHR_ASC));                                                 // Save the WPC manufacturer Certificate string
            fil = new WpcFil(dir, "Private_key_" + nam);                                            // Create file header for private key
            str = Dbg.hexStr(((ECPrivateKey)pair.getPrivate()).getD().toByteArray());               // Get private key string
            Dbg.log("Private key: " + str);                                                         // Log private key
            fil.save(str.getBytes(AppLib.CHR_ASC));                                                 // Save private key
            WpcKey.getPubKey(pair.getPrivate());                                                    // Verify private key
        } catch (IOException err) {                                                                 // Error occurs
            Dbg.log("Cannot generate TBS certificate file", err);                                   // Log error
        }
    }

    /**
     * Saves a new WPC manufacturer certificate for signature
     * @param   sid The Manufacturer code
     * @param   nam The name of the manufacturer
     */
    private void savManCrt(int sid, @NonNull String nam) {
        final @NonNull File dir = new File(WpcCrt.sDir, NAM_RT);                                    // Get directory for the root certificate
        final @NonNull WpcFil key = new WpcFil(dir, "Informal_plugfest_2_CA_root_private_key");     // Get the private key of the root certificate
        try {
            final @NonNull PrivateKey prv = WpcKey.getPrvKey(key.read());                           // Read the private key of the root certificate
            KeyPair pair = SafFkt.getPair();                                                        // Generate key pair
            final byte[] snr = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08};
            WpcCrt crt = new WpcCrt(snr, sid, pair.getPublic(), prv);                               // Generate WPC Manufacturer certificate for signature
            String str = Dbg.hexStr(crt.getEncoded());                                              // Get WPC Manufacturer Certificate string
            Dbg.log("Manufacturer Certificate: " + str);                                            // Log WPC Manufacturer Certificate string
            WpcFil fil = new WpcFil(dir, "Informal_plugfest_2_CA_" + nam + "_cert");                // Create manufacturer certificate file header
            fil.save(str.getBytes(AppLib.CHR_ASC));                                                 // Save the WPC manufacturer Certificate string
            fil = new WpcFil(dir, "Informal_plugfest_2_CA_" + nam + "_private_key");                // Create file header for private key
            str = Dbg.hexStr(WpcKey.getInt(((ECPrivateKey)pair.getPrivate()).getD(), WpcKey.KEY_SIZ)); // Get private key string
            Dbg.log("Private key: " + str);                                                         // Log private key
            fil.save(str.getBytes(AppLib.CHR_ASC));                                                 // Save private key
            WpcKey.getPubKey(pair.getPrivate());                                                    // Verify private key
        } catch (GeneralSecurityException | IOException err) {                                      // Error occurs
            Dbg.log("Cannot generate Manufacturer certificate", err);                               // Log error
        }
    }

    /**
     * Reads the Manufacturer certificate from file
     *
     * @param   nam The Manufacturer name
     * @return  The Manufacturer certificate
     */
    private @NonNull WpcCrt getMan(@NonNull String nam) {
        final File dir = new File(WpcCrt.sDir, NAM_RT);                                             // Get directory for the root certificate
        final WpcFil fil = new WpcFil(dir, "Informal_plugfest_2_CA_" + nam + "_cert");              // Create TBS certificate file header
        try {
            return new WpcCrt(fil.read(), 0);                                                       // Return the manufacturer certificate
        } catch (IOException err) {                                                                 // Error during reading the manufacturer certificate
            Dbg.log("Cannot read manufacturer certificate", err);                                   // Log error
            throw new NullPointerException();                                                       // Abort app
        }
    }

    /**
     * Reads the private key for a manufacturer certificate from a file
     *
     * @param   nam The manufacturer name
     * @return  The private key of the manufacturer certificate
     */
    private PrivateKey getPrv(@NonNull String nam) {
        File dir = new File(WpcCrt.sDir, NAM_RT);                                                   // Get directory for the root certificate
        final WpcFil fil = new WpcFil(dir, "Informal_plugfest_2_CA_" + nam + "_private_key");       // Create TBS certificate file header
        try {
            byte[] ba = fil.read();                                                                 // Read the private key
            Dbg.log(Dbg.seqHex("Private key: ", ba));                                               // Log the private key
            return WpcKey.getPrvKey(ba);                                                            // Return the private key
        } catch (IOException err) {                                                                 // Error during reading the private key
            Dbg.log("Cannot read private key", err);                                                // Log error
            throw new NullPointerException();                                                       // Abort app
        }

    }

    /**
     * Generates Product Unit certificates
     */
    private void setPuCrt() {
        WpcCrt man = getMan("ST");                                                                  // Get the test manufacturer certificate
        Dbg.log(Dbg.seqHex("Man-Cert.: ", man.getEncoded()));                                       // Log loaded certificate
        int mc = man.getMan();                                                                      // Get the manufacturer code
        PrivateKey prv = getPrv("ST");                                                              // Get the private key of the manufacturer certificate
        WpcKey.getPubKey(prv);                                                                      // Calculate the public key
        try {
            new WpcCrtChn(setPuCrt(WpcCrt.TYP_TPU, mc, 24, 5634, man, prv)).verify();               // Create Certificate Chain for STWBC
            final KeyPair pair = SafFkt.getPair();                                                  // Create key pair
            WpcCrt sec = new WpcCrt(WpcCrt.TYP_INT, 31, man.getSid(), mc, "SecCrt".getBytes(AppLib.CHR_ISO), pair.getPublic(), prv);
            PrivateKey key = pair.getPrivate();
            new WpcCrtChn(setPuCrt(WpcCrt.TYP_TPU, mc, 45, 2249, man, sec, key)).verify();          // Create Certificate Chain for STWBC2
            man = getMan("Test");                                                                   // Get the Cheap manufacturer certificate
            mc = man.getMan();                                                                      // Get the manufacturer code
            prv = getPrv("Test");                                                                   // Get the private key of the manufacturer code
            WpcKey.getPubKey(prv);                                                                  // Calculate the public key
            new WpcCrtChn(setPuCrt(WpcCrt.TYP_TPU, mc, 25, 6386, man, prv)).verify();               // Verify the Certificate Chain for PTx-1
        } catch (Exception err) {                                                                   // Error occurred
            Dbg.log("Error err", err);                                                              // Log error
        }
    }

    /**
     * Create Certificate Chain
     * @param   typ Product Unit Certificate Type
     * @param   mc  Manufacturer code
     * @param   snr Serial number of the Product Unit Certificate
     * @param   qiid Qi-ID of the Product Unit Certificate
     * @param   man Manufacturer Certificate
     * @param   prv Private key of the Manufacturer key
     * @return  The Certificate Chain
     */
    private @NonNull byte[] setPuCrt(byte typ, int mc, int snr, final int qiid, @NonNull WpcCrt man, @NonNull PrivateKey prv) {
        return savPuCrt(typ, mc,snr, man.getSid(), qiid, new WpcCrtChn(man), prv);
    }
    /**
     * Create Certificate Chain
     * @param   typ Product Unit Certificate Type
     * @param   mc  Manufacturer code
     * @param   snr Serial number of the Product Unit Certificate
     * @param   qiid Qi-ID of the Product Unit Certificate
     * @param   man Manufacturer Certificate
     * @param   sec Secondary Certificate
     * @param   prv Private key of the Manufacturer key
     */
    private @NonNull byte[] setPuCrt(byte typ, int mc, int snr, final int qiid, @NonNull WpcCrt man, WpcCrt sec, @NonNull PrivateKey prv) {
        return savPuCrt(typ, mc,snr, sec.getSid(), qiid, new WpcCrtChn(man, sec), prv);
    }

    /**
     * Create Certificate Chain
     * @param   typ Product Unit Certificate Type
     * @param   mc  Manufacturer code
     * @param   snr Serial number of the Product Unit Certificate
     * @param   iid Issuer ID of the Product Unit Certificate
     * @param   qiid Qi-ID of the Product Unit Certificate
     * @param   chn WPC certificate chain
     * @param   prv Private key of the Manufacturer key
     * @return  The Certificate Chain
     */
    private @NonNull byte[] savPuCrt(byte typ, int mc, int snr, final @NonNull byte[] iid, final int qiid, @NonNull WpcCrtChn chn, @NonNull PrivateKey prv) {
        final KeyPair pair = SafFkt.getPair();                                                      // Create key pair
        final @NonNull ByteBuffer sid = ByteBuffer.wrap(new byte[6]);
        sid.put(AppLib.intToBcd(qiid, 3));
        chn.addCrt(new WpcCrt(typ, snr, iid, mc, sid.array(), pair.getPublic(), prv));              // Add product unit certificate
        byte[] ba = chn.getChn();                                                                   // Get the byte array of the WPC Certificate Chain
        Dbg.log(Dbg.seqHex("Chain: ", ba));                                                         // Log chain
        final @NonNull File dir = new File(WpcCrt.sDir, NAM_RT);                                    // Get directory for the root certificate
        final String name = WpcQiId.getName(qiid);
        WpcFil fil = new WpcFil(dir, name + WpcCrtChn.EXT_CHN);                                     // Create manufacturer certificate file header
        try {
            fil.save(Dbg.hexStr(ba).getBytes(AppLib.CHR_ASC));
            String key = Dbg.hexStr(((ECPrivateKey)pair.getPrivate()).getD().toByteArray());
            Dbg.log("Key: " + key);
            fil = new WpcFil(dir, name + WpcCrt.EXT_PRV);                                           // Create manufacturer certificate file header
            fil.save(key.getBytes(AppLib.CHR_ASC));
            String dig = Dbg.hexStr(chn.getDig());
            Dbg.log("Digest: " + dig);
            fil = new WpcFil(dir, name + "_dig");                                                   // Create manufacturer certificate file header
            fil.save(key.getBytes(AppLib.CHR_ASC));
        } catch(IOException err) {
            Dbg.log("Cannot save certificate", err);
        }
        return ba;
    }
}