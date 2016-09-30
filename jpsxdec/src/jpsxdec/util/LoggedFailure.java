/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2017  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.util;

import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.ILocalizedMessage;

/** General purpose failure that includes a message for the user. */
public class LoggedFailure extends Exception {

    @Nonnull
    private final ILocalizedMessage _msg;
    private final Level _level;
    private final boolean _blnLogged;

    public LoggedFailure(@Nonnull Level level, @Nonnull ILocalizedMessage msg) {
        this(level, msg, null);
    }
    public LoggedFailure(@Nonnull Level level, @Nonnull ILocalizedMessage msg, 
                         @CheckForNull Throwable cause)
    {
        super(msg.getEnglishMessage(), cause);
        _msg = msg;
        _level = level;
        _blnLogged = false;
    }

    /** Automatically logs the message while creating this exception. */
    public LoggedFailure(@Nonnull ILocalizedLogger log, @Nonnull Level level,
                         @Nonnull ILocalizedMessage msg)
    {
        this(log, level, msg, null);
    }

    /** Automatically logs the message while creating this exception. */
    public LoggedFailure(@Nonnull ILocalizedLogger log, @Nonnull Level level,
                         @Nonnull ILocalizedMessage msg, @CheckForNull Throwable cause)
    {
        super(msg.getEnglishMessage());
        _msg = msg;
        _level = level;
        log.log(level, msg, cause);
        _blnLogged = true;
    }

    /** If the message was already logged during construction. */
    public boolean wasLogged() {
        return _blnLogged;
    }

    public void log(@Nonnull ILocalizedLogger log) {
        log.log(_level, _msg, getCause());
    }

    public @Nonnull ILocalizedMessage getSourceMessage() {
        return _msg;
    }

    @Override
    public @Nonnull String getLocalizedMessage() {
        return _msg.getLocalizedMessage();
    }

    @Override
    public @CheckForNull String getMessage() {
        return _msg.getEnglishMessage();
    }
    
}