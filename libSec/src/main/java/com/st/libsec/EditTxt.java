package com.st.libsec;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * View class to show an input field with a hint text
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
public class EditTxt extends TextInputEditText {

    /**
     * Hides the actually shown keyboard
     *
     * @param   ctx The context for the keyboard
     * @param   vw  The view where the keyboard is shown
     */
    public static void hideKbd(Context ctx, View vw) {
        InputMethodManager imm = (InputMethodManager)ctx.getSystemService(INPUT_METHOD_SERVICE);    // Get keyboard manager
        if (imm != null) {                                                                          // Keyboard manager available?
            imm.hideSoftInputFromWindow(vw.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);   // Hide the soft keyboard
        }
        vw.clearFocus();                                                                            // Clear the focus
    }

    /**
     * Shows the soft keyboard
     *
     * @param   ctx The context for the keyboard
     * @param   et  The input field where the keyboard shall be shown
     */
    public static void showKbd(Context ctx, EditText et) {
        InputMethodManager imm = (InputMethodManager)ctx.getSystemService(INPUT_METHOD_SERVICE);    // Get keyboard manager
        et.requestFocus();                                                                          // Request focus for the input field
        if (imm != null) {                                                                          // Keyboard manager available?
            imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);                                // Show the soft keyboard
        }
    }

    /**
     * Listener for enter key
     */
    private class EndLst implements TextView.OnEditorActionListener {

        /**
         * Calle when the enter key was pressed
         *
         * @param v         The edit field
         * @param actionId  The action identifier
         * @param event     The key event
         * @return          false to continue standard process of this event
         */
        @Override  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            hideKbd(getContext(), v);                                                               // Hide the soft keyboard and clear the focus of the edit field
            return false;                                                                           // Indicate that the standard process shall be continued
        }
    }

    /**
     * Creates an input field with hint text
     *
     * @param context   The context where the input field will be shown
     */
    public EditTxt(Context context) {
        this(context, null);                                                                        // Creates the input field with hint text
    }

    /**
     * Creates an input field with hint text
     *
     * @param context   The context where the input field will be shown
     * @param attrs     The XML attribute set
     */
    public EditTxt(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);                                                 // Creates the input field with hint text
    }
    /**
     * Creates an input field with hint text
     *
     * @param context   The context where the input field will be shown
     * @param attrs     The XML attribute set
     * @param defStyle  The default style resource
     */
    public EditTxt(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);                                                            // Creates the input field
        int act = getImeOptions();                                                                  // Get the keyboard action
        if ((act != EditorInfo.IME_ACTION_NEXT) && (act != EditorInfo.IME_ACTION_UNSPECIFIED)) {    // Action closes the soft keyboard?
            setOnEditorActionListener(new EndLst());                                                // Register listener for editor actions
        }
    }

    /**
     * Returns the actual content of the edit field
     *
     * @return  The actual content of the edit field
     */
    public @NonNull String getTxt() {
        //noinspection ConstantConditions                                                           // A text will be her always available
        return getText().toString().trim();                                                         // Return the actual content of the edit field
    }

    /**
     * Handles the key events before they are handled by the input field
     * De-focus the view when back button was pressed
     *
     * @param keyCode   The pressed button
     * @param event     The key event
     * @return          false to continue with the standard behaviour
     */
    @Override public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getAction() == KeyEvent.ACTION_UP)) {      // Back key pressed?
            clearFocus();                                                                           // Clear the focus of the edit field
        }
        return false;                                                                               // Continue with hiding the keyboard
    }
}