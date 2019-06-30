package com.st.libsec;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.io.IOException;

/**
 * Qi Authentication interface
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
public interface WpcCom {
    /**
     * Provides the received WPC Certification chain of the remote device
     *
     * @param   chn The WPC Certification Chain
     */
    void setChn(@NonNull WpcCrtChn chn);

    /**
     * Sends a Qi Authentication Request
     *
     * @param   req The Qi Authentication Request
     * @return  The Qi Authentication Response
     * @throws  IOException in case of NFC communication errors
     */
    @NonNull byte[] sndMsg(@NonNull byte[] req) throws IOException;

    /**
     * Terminates the Qi Authentication
     *
     * @param   err The error text
     * @param   des The error description
     */
    void endAuth(final @StringRes int err, final @StringRes int des);
}