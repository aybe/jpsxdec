/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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

package jpsxdec.discitems;

import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.i18n.LocalizedMessage;
import jpsxdec.sectors.IdentifiedSector;

/** Interface for decoders generated by an IDiscItemAudioStream.
 *  Hides the decoder implementation, but also allows it to be passed around. */
public interface ISectorAudioDecoder {

    public interface ISectorTimedAudioWriter {
        void write(@Nonnull AudioFormat format, @Nonnull byte[] abData, int iStart, int iLen, int iPresentationSector) throws IOException;
    }

    /** Must be set before using this class. */
    void setAudioListener(@Nonnull ISectorTimedAudioWriter audioFeeder);

    /** The format of the audio data that will be fed to the
     * {@link ISectorTimedAudioWriter} listener. */
    @Nonnull AudioFormat getOutputFormat();

    /** If it likes the sector, feeds audio data to the ISectorTimedAudioWriter
     *  supplied by the {@link #setAudioListener(ISectorTimedAudioWriter)} method. 
     *  @return if the sector was accepted.  */
    boolean feedSector(@Nonnull IdentifiedSector sector, @Nonnull Logger log) throws IOException;

    double getVolume();
    void setVolume(double dblVolume);

    /** Resets the decoding context. */
    void reset();

    /** Sector where the audio begins to play. */
    int getPresentationStartSector();

    int getStartSector();
    int getEndSector();

    @Nonnull LocalizedMessage[] getAudioDetails();
    
    int getSamplesPerSecond();

    int getDiscSpeed();
}