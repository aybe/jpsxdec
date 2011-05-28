/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

package jpsxdec.psxvideo.bitstreams;


import java.io.EOFException;
import java.util.Random;
import jpsxdec.util.Misc;

/** A (hopefully) very fast bit reader. It can be initialized to read the bits
 * in big-endian order, or in 16-bit little-endian order. */
public class ArrayBitReader {

    /** Data to be read as a binary stream. */
    private byte[] _abData;
    /** If 16-bit words should be read in big or little endian order. */
    private boolean _blnLittleEndian;
    /** Offset of first byte in the current word being read from the source buffer. */
    private int _iByteOffset;
    /** The current 16-bit word value from the source data. */
    private short _siCurrentWord;
    /** Bits remaining to be read from the current word. */
    private int _iBitsLeft;

    /** Quick lookup table to mask remaining bits. */
    private static int BIT_MASK[] = new int[] {
        0x00000000,
        0x00000001, 0x00000003, 0x00000007, 0x0000000F,
        0x0000001F, 0x0000003F, 0x0000007F, 0x000000FF,
        0x000001FF, 0x000003FF, 0x000007FF, 0x00000FFF,
        0x00001FFF, 0x00003FFF, 0x00007FFF, 0x0000FFFF,
        0x0001FFFF, 0x0003FFFF, 0x0007FFFF, 0x000FFFFF,
        0x001FFFFF, 0x003FFFFF, 0x007FFFFF, 0x00FFFFFF,
        0x01FFFFFF, 0x03FFFFFF, 0x07FFFFFF, 0x0FFFFFFF,
        0x1FFFFFFF, 0x3FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF,
    };

    /** Performs no initialization. {@link #reset(byte[], boolean, int)}
     * needs to be called before using this class. */
    public ArrayBitReader() {
    }

    /** Start reading from the start of the array with the requested
     * endian-ness. */
    public ArrayBitReader(byte[] abData, boolean blnLittleEndian) {
        this(abData, blnLittleEndian, 0);
    }

    /** Start reading from a requested point in the array with the requested
     *  endian-ness. 
     *  @param iReadStart  Position in array to start reading. Must be an even number. */
    public ArrayBitReader(byte[] abDemux, boolean blnLittleEndian, int iReadStart) {
        reset(abDemux, blnLittleEndian, iReadStart);
    }
    
    /** Re-constructs this ArrayBitReader. Allows for re-using the object
     *  so there is no need to create a new one.
     *  @param iReadStart  Position in array to start reading. Must be an even number. */
    public void reset(byte[] abDemux, boolean blnLittleEndian, int iReadStart) {
        if ((iReadStart & 1) != 0)
            throw new IllegalArgumentException("Data start must be on word boundary.");
        _iByteOffset = iReadStart;
        _abData = abDemux;
        _iBitsLeft = 16;
        _blnLittleEndian = blnLittleEndian;
        _siCurrentWord = readWord(_iByteOffset);
    }

    /** Reads 16-bits at the requested offset in the proper endian order. */
    private short readWord(int i) {
        if (_blnLittleEndian) {
            int b1 = _abData[i  ] & 0xFF;
            int b2 = _abData[i+1] & 0xFF;
            return (short)((b2 << 8) + (b1 << 0));
        } else {
            int b1 = _abData[i+1] & 0xFF;
            int b2 = _abData[i  ] & 0xFF;
            return (short)((b2 << 8) + (b1 << 0));
        }
    }

    /** Returns the offset to the current word that the bit reader is reading. */
    public int getPosition() {
        return _iByteOffset;
    }
    
    /** Reads the requested number of bits. */
    public long readUnsignedBits(int iCount) throws EOFException {
        assert iCount >= 0 && iCount <= 31;
        
        try {
            // want to read the next 16-bit word only when it is needed
            // so we don't try to buffer data beyond the array
            if (_iBitsLeft == 0) {
                _iByteOffset += 2;
                _siCurrentWord = readWord(_iByteOffset);
                _iBitsLeft = 16;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            _iByteOffset -= 2;
            throw new EOFException();
        }

        long lngRet = 0;
        if (iCount <= _iBitsLeft) { // iCount <= _iBitsLeft <= 16
            lngRet = (_siCurrentWord >>> (_iBitsLeft - iCount)) & BIT_MASK[iCount];
            _iBitsLeft -= iCount;
        } else {
            lngRet = _siCurrentWord & BIT_MASK[_iBitsLeft];
            iCount -= _iBitsLeft;
            _iBitsLeft = 0;

            try {
                while (iCount >= 16) {
                    _iByteOffset += 2;
                    lngRet = (lngRet << 16) | (readWord(_iByteOffset) & 0xFFFF);
                    iCount -= 16;
                }

                if (iCount > 0) { // iCount < 16
                    _iByteOffset += 2;
                    _siCurrentWord = readWord(_iByteOffset);
                    _iBitsLeft = 16 - iCount;
                    lngRet = (lngRet << iCount) | ((_siCurrentWord & 0xFFFF) >>> _iBitsLeft);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                // _iBitsLeft will == 0
                _iByteOffset -= 2;
                return lngRet << iCount;
            }
        }

        return lngRet;
    }
    
    /** Reads the requested number of bits then sets the sign 
     *  according to the highest bit. */
    public long readSignedBits(int iCount) throws EOFException {
        return (readUnsignedBits(iCount) << (64 - iCount)) >> (64 - iCount); // extend sign bit
    }    
    
    public long peekUnsignedBits(int iCount) throws EOFException {
        int iSaveOffs = _iByteOffset;
        int iSaveBitsLeft = _iBitsLeft;
        short siSaveCurrentWord = _siCurrentWord;
        try {
            return readUnsignedBits(iCount);
        } finally {
            _iByteOffset = iSaveOffs;
            _iBitsLeft = iSaveBitsLeft;
            _siCurrentWord = siSaveCurrentWord;
        }
    }
    
    public long peekSignedBits(int iCount) throws EOFException {
        return (peekUnsignedBits(iCount) << (64 - iCount)) >> (64 - iCount); // extend sign bit
    }    
    
    public void skipBits(int iCount) throws EOFException {

        _iBitsLeft -= iCount;
        if (_iBitsLeft < 0) {
            while (_iBitsLeft < 0) {
                _iByteOffset += 2;
                _iBitsLeft += 16;
            }
            if (_iBitsLeft > 0) {
                try {
                    _siCurrentWord = readWord(_iByteOffset);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    throw new EOFException();
                }
            }
        }
        
    }

    /** Returns a String of 1 and 0 unless at the end of the stream, then
     * returns only the remaining bits. */
    public String peekBitsToString(int iCount) throws EOFException {
        int iBitsRemaining = bitsRemaining();
        if (iBitsRemaining < iCount)
            return Misc.bitsToString(peekUnsignedBits(iBitsRemaining), iBitsRemaining);
        else
            return Misc.bitsToString(peekUnsignedBits(iCount), iCount);
    }

    /** Returns a String of 1 and 0 unless at the end of the stream, then
     * returns only the remaining bits. */
    public String readBitsToString(int iCount) throws EOFException {
        int iBitsRemaining = bitsRemaining();
        if (iBitsRemaining < iCount)
            return Misc.bitsToString(readUnsignedBits(iBitsRemaining), iBitsRemaining);
        else
            return Misc.bitsToString(readUnsignedBits(iCount), iCount);
    }

    public int bitsRemaining() {
        return (_abData.length - _iByteOffset) * 8 - (16 - _iBitsLeft);
    }

    /** Test this class. */
    public static void main(String[] args) throws EOFException {
        final Random rand = new Random();

        byte[] abTest = new byte[6];
        rand.nextBytes(abTest);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : abTest) {
            sb.append(Misc.bitsToString(b, 8));
        }
        final String BIT_STRING = sb.toString();
        System.out.println(BIT_STRING);
        sb.setLength(0);

        ArrayBitReader abr = new ArrayBitReader(abTest, false);

        System.out.println("Reading " + 16);
        sb.append(abr.peekBitsToString(16));
        abr.skipBits(16);
        System.out.println("Reading " + 16);
        sb.append(abr.peekBitsToString(16));
        abr.skipBits(16);
        System.out.println("Reading " + 16);
        sb.append(abr.peekBitsToString(16));
        abr.skipBits(16);
        System.out.println(sb);

        sb.setLength(0);
        abr = new ArrayBitReader(abTest, false);

        int i;
        for (i = rand.nextInt(31)+1; i < abr.bitsRemaining(); i = rand.nextInt(31)+1) {
            String s = abr.peekBitsToString(i);
            System.out.println("Reading " + i + " " + s);
            sb.append(s);
            abr.skipBits(i);
        }
        i = abr.bitsRemaining();
        if (i > 0) {
            String s = abr.peekBitsToString(i);
            System.out.println("Reading " + i + " " + s);
            sb.append(s);
            abr.skipBits(i);
        }

        final String READ_BITS = sb.toString();
        System.out.println(READ_BITS);
        if (READ_BITS.equals(BIT_STRING)) {
            System.out.println("Success!");
        } else {
            System.out.println("FAILURE!");
        }


        
        long lngPeek, lngRead;
        String sPeek;

        abr.reset(abTest, true, 0);
        
        System.out.println(abr.bitsRemaining());
        sPeek = abr.peekBitsToString(31);
        lngPeek = abr.peekUnsignedBits(31);
        lngRead = abr.readUnsignedBits(31);
        System.out.println(lngPeek+" == "+lngRead+" (" + sPeek + "): " + (lngPeek == lngRead));
        System.out.println(abr.bitsRemaining());
        lngPeek = abr.peekUnsignedBits(3);
        lngRead = abr.readUnsignedBits(3);
        System.out.println(lngPeek+" == "+lngRead+" : " + (lngPeek == lngRead));
        System.out.println(abr.bitsRemaining());
        lngPeek = abr.peekUnsignedBits(3);
        lngRead = abr.readUnsignedBits(3);
        System.out.println(lngPeek+" == "+lngRead+" : " + (lngPeek == lngRead));
        System.out.println(abr.bitsRemaining());
        lngPeek = abr.peekUnsignedBits(11);
        lngRead = abr.readUnsignedBits(11);
        System.out.println(lngPeek+" == "+lngRead+" : " + (lngPeek == lngRead));
        System.out.println(abr.bitsRemaining());

        abr.reset(abTest, true, 0);
        abr.skipBits(5);
        System.out.println(abr.bitsRemaining());
        abr.skipBits(30);
        System.out.println(abr.bitsRemaining());
        lngPeek = abr.peekUnsignedBits(13);
        lngRead = abr.readUnsignedBits(13);
        System.out.println(lngPeek+" == "+lngRead+" : " + (lngPeek == lngRead));
        System.out.println(abr.bitsRemaining());
    }

}