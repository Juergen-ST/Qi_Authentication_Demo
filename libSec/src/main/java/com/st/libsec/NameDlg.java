package com.st.libsec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

/**
 * Dialog to enter a name
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

@SuppressWarnings("ConstantConditions")
public class NameDlg extends DialogFragment implements View.OnClickListener, TextView.OnEditorActionListener, TextWatcher, View.OnFocusChangeListener {

    private static final String TAG     = "NameDlg";                                                // Class tag
    private static final String KEY_EDIT= TAG + "Edit";                                             // Bundle key string to provide the edit name hint
    private static final String KEY_ERR = TAG + "Err";                                              // Bundle key string to provide the error
    private static final String KEY_NAME= TAG + "Name";                                             // Bundle key string to provide the name
    private static final String KEY_POS = TAG + "Pos";                                              // Bundle key string to provide the cursor position
    private static final String KEY_TIT = TAG + "Tit";                                              // Bundle key string to provide the dialog title

    private boolean         mCancel;                                                                // Flag for shown cancel button
    private NameDlgLst      mLst;                                                                   // The listener for name dialog
    private TextInputLayout mLay;                                                                   // Editor layout
    private EditTxt         mNam;                                                                   // Entered name
    private String          mErr;                                                                   // Error text

    /**
     * Called before a text is changed
     **
     * @param s
     * @param start
     * @param count
     * @param after
     */
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    /**
     * Called when a text is changed.
     *
     * @param s
     * @param start
     * @param before
     * @param count
     */
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

    /**
     * Called after a text is changed
     *
     * @param s     The text after the change
     */
    @Override
    public void afterTextChanged(Editable s) {
        final @NonNull String name = s.toString().trim();                                           // Get the actual entered name
        if (!name.isEmpty()) {                                                                      // Was a name entered?
            final boolean ovr = mLst.hasName(name);                                                 // Check if file exists already
            final boolean err = (mLay.getError() != null);                                          // Check if overwrite message is shown
            if (ovr && !err) {                                                                      // Name exists and no overwrite active?
                mLay.setError(mErr);
                Button btn = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);    // Get the OK button
                btn.setText(R.string.lib_ovr);                                                      // Show overwrite action
            } else if ((!ovr && err) || mCancel) {                                                  // Name exists and no overwrite active?
                mLay.setError(null);                                                        // Hide the warning message
                Button btn = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);    // Get the OK button
                btn.setText(R.string.lib_ok);                                                       // Show OK action
                mCancel = false;
            }
        } else {
            mLay.setError(null);                                                            // Hide the warning message
            Button btn = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);        // Get the OK button
            btn.setText(R.string.lib_cancel);                                                       // Show cancel action
            mCancel = true;
        }
    }

    /**
     * Listener interface for closing the name dialog
     */
    public interface NameDlgLst {

        /**
         * Called when the name dialog is closed
         *
         * @param name  The entered name
         */
        void onCls(final @NonNull String name);

        /**
         * Called when a new name was entered
         *
         * @param name  The entered name
         * @return      true if the name exists already otherwise false
         */
        boolean hasName(final @NonNull String name);
    }

    /**
     * Create a name dialog
     *
     * @param nam  The proposed name
     * @param des   The name edit field name
     * @param err   The error message
     * @param fm    The Fragment manager
     * @param lst   Listener for closing the name dialog
     */
    public static void showDlg(final @StringRes int tit, final @StringRes int des, final @NonNull String nam, final int pos, final @StringRes int err, final @NonNull FragmentManager fm, final @NonNull NameDlgLst lst) {
        final @NonNull Bundle arg = new Bundle();                                                   // Image size dialog parameter
        arg.putInt(KEY_TIT, tit);                                                                   // Add the resource identifier for name type
        arg.putInt(KEY_EDIT, des);                                                                  // Add the resource identifier for name type
        arg.putString(KEY_NAME, nam);                                                               // Add proposed name
        arg.putInt(KEY_POS, pos);                                                                   // Add the cursor position
        arg.putInt(KEY_ERR, err);                                                                   // Add the resource identifier for error message
        final @NonNull NameDlg dlg = new NameDlg();                                                 // Create the image size dialog
        dlg.setArguments(arg);                                                                      // Set the image size dialog parameter
        dlg.mLst = lst;                                                                             // Remember listener for closing the image size dialog
        dlg.show(fm, TAG);                                                                          // Show the name dialog
    }

    /**
     * Called when the OK button of the name dialog is pressed
     *
     * @param v     The OK button (not used here)
     */
    @Override public void onClick(View v) {
        onEditorAction(mNam, 0, null);                                                              // Handle the pressed OK button
    }

    /**
     * Called when the dialog fragment is created
     *
     * @param savedInstanceState    The saved data (not used here)
     * @return                      The name dialog
     */
    @SuppressLint("InflateParams")                                                                  // The alert dialog view must not be derived from a parent view
    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        final @NonNull Activity act = getActivity();                                                // Get the activity showing this dialog
        final @NonNull View lay = act.getLayoutInflater().inflate(R.layout.lib_nam_dlg, null);      // Get the name dialog layout
        final @NonNull AlertDialog.Builder bld = new AlertDialog.Builder(act);                      // Create the alert dialog builder
        final @NonNull Bundle arg = getArguments();                                                 // Get the name dialog parameter
        bld.setTitle(arg.getInt(KEY_TIT));                                                          // Set dialog title
        bld.setView(lay);                                                                           // Set the view in the name dialog
        mCancel = false;
        mNam = lay.findViewById(R.id.lib_edit_nam);                                                 // Get the name edit field
        mLay = lay.findViewById(R.id.lib_lay_nam);                                                  // Get the name editor layout
        final @NonNull String name = arg.getString(KEY_NAME);                                       // Get proposed file name
        if (savedInstanceState == null) {                                                           // Dialog newly created?
            mNam.setText(name);                                                                     // Set the proposed name
            @StringRes int txt = arg.getInt(KEY_EDIT);                                              // Get the name edit field name
            if (txt != 0) {                                                                         // Name edit field name available?
                final @NonNull String str = getString(txt);                                         // Get the name edit field name
                ((TextInputLayout)lay.findViewById(R.id.lib_lay_nam)).setHint(str);                 // Set the name edit field name
                mNam.setHint(str);                                                                  // Set the name edit field name
            }
            mErr = getString(arg.getInt(KEY_ERR));                                                  // Get the error message
            mLay.setErrorEnabled(true);                                                             // Dis-/Enable error text
            if (mLst.hasName(name)) {                                                               // Error massage available?
                mLay.setError(mErr);                                                                // Set the error message
                bld.setPositiveButton(R.string.lib_ovr, null);                                      // Register OK button
            } else {
                bld.setPositiveButton(R.string.lib_ok, null);                                       // Register OK button
            }
        }
        mNam.requestFocus();                                                                        // Set the focus to the name edit field
        mNam.setSelection(arg.getInt(KEY_POS));                                                     // Set the cursor position
        mNam.setOnEditorActionListener(this);                                                       // Register listener for entered name
        mNam.setOnFocusChangeListener(this);                                                        // Register listener for closed keyboard
        mNam.addTextChangedListener(this);                                                          // Register listener for changed text
        return bld.create();                                                                        // Return the name dialog
    }

    /**
     * Called when the Do key or OK button was pressed.
     *
     * @param v         The view that was clicked.
     * @param actionId  Identifier of the action.
     * @param event     If triggered by an enter key, this is the event; otherwise, this is null.
     * @return          Returns true to indicate that this event is processed
     */
    @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        final @NonNull String name = ((EditTxt)v).getTxt();                                         // Get the actual entered name
        if (!name.isEmpty()) {                                                                      // Was a name entered?
            mLst.onCls(name);                                                                       // Inform about the chosen name
        }
        dismiss();                                                                                  // Close the name dialog box
        return true;                                                                                // Inform that this event is processed
    }

    /**
     * Called when the keyboard has closed.
     *
     * @param v        The view whose state has changed.
     * @param hasFocus The new focus state of v.
     */
    @Override public void onFocusChange(View v, boolean hasFocus) {
        dismiss();                                                                                  // Close the name dialog
    }

    /**
     * Called after dialog creation
     */
    @Override public void onStart() {
        final @NonNull AlertDialog dlg = (AlertDialog)getDialog();                                  // Get the name dialog
        dlg.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);                    // Show the soft keyboard
        super.onStart();                                                                            // Show the dialog
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this);                        // Register the listener for the OK button
    }
}