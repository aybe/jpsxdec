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

package jpsxdec.indexing.psxvideofps;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

/** Detects whole-number (integer) sectors/frame rate of
 * STR movies. This can also handle variable rates.
 *<p>
 * It uses this basic idea: find what sector/frame interval will
 * be sure to land on every point between frames. As each frame is examined,
 * the number of possibilities are whittled down until there should be very
 * few. There needs to be at least 3 frames in the video before an
 * initial guess can be made.
 *</p>
 * This isn't usable for movies with &lt; 3 frames,
 * but with that few of frames, the frame rate isn't a big deal.
 */
public class WholeNumberSectorsPerFrame {

    // TODO: how can we detect if the video has a variable frame rate?
    // maybe detect when rates change by some factor?
    
    /** The current frame number. */
    private int _iCurrentFrame;
    /** The number of the previous sector that was reported to be a video sector. */
    private int _iPreviousVideoSect = -1;

    /** 3 states: prior to the 2nd frame == null,
     * still possibilities .size() > 0,
     * no more possibilities .size() == 0
     */
    private TreeSet<SectorsPerFrameFromStart> _startingPoints;

    public WholeNumberSectorsPerFrame(int iSector, int iFrame) {
        if (iSector < 0 || iFrame < 0)
            throw new IllegalArgumentException();
        _iCurrentFrame = iFrame;
        _iPreviousVideoSect = iSector;
    }

    /** Should be called for every video sector.
     * 
     * @param iSector  Number of sectors since the start of the video (starts at 0).
     * @param iFrame   Frame number of the sector.
     * @return         If any sector/frame possibilities exist other than 1.
     */
    public boolean matchesNextVideo(int iSector, int iFrame) {
        if (iSector <= _iPreviousVideoSect || iFrame < _iCurrentFrame)
            throw new IllegalArgumentException();

        // TODO: If the frame number increases by more than 1, make sure the
        // test intervals occur at least twice within the space between frames?

        boolean blnRet;

        if (_startingPoints == null) {
            // haven't reached 2nd frame before now

            if (_iCurrentFrame != iFrame) {
                // we've reached 2nd frame

                // create a possible sectors/frame for each sector that the
                // 2nd frame could actually start from
                _startingPoints = new TreeSet<SectorsPerFrameFromStart>();
                for (int iFrame2StartSector = _iPreviousVideoSect+1;
                    iFrame2StartSector <= iSector;
                    iFrame2StartSector++)
                {
                    _startingPoints.add(new SectorsPerFrameFromStart(iFrame2StartSector));
                }
            }
            blnRet = true;
        } else if (_startingPoints.isEmpty()) {
            // no more possibilities :(
            blnRet = false;
        } else {
            // can add more
            
            if (_iCurrentFrame == iFrame) {
                blnRet = true;
            } else {
                // if new frame

                blnRet = false;
                for (Iterator<SectorsPerFrameFromStart> it = _startingPoints.iterator(); it.hasNext();) {
                    SectorsPerFrameFromStart possibleStart = it.next();
                    if (possibleStart.update(_iPreviousVideoSect, iSector)) {
                        // at least 1 possible starting sector reports still having
                        // some valid sectors/second
                        blnRet = true;
                    } else {
                        // if that starting sector assumption leads to no results,
                        // remove it from the list
                        it.remove();
                    }
                }

            }

        }

        _iCurrentFrame = iFrame;
        _iPreviousVideoSect = iSector;

        return blnRet;
    }

    /** Mostly for debugging. Returns all possible sectors/frame, including
     * redundant factors. */
    int[] getAllPossibleSectorsPerFrame() {
        if (_startingPoints == null)
            return null;
        else if (_startingPoints.isEmpty())
            return new int[] {1};
        return integerCollectionToIntArray(getCombined());
    }

    /** Returns an array of the best possible sectors/frame for this video.
     * Hopefully it will only return 1 number. In the case of multiple
     * numbers, the one with the highest value is probably the best.
     */
    public int[] getPossibleSectorsPerFrame() {
        if (_startingPoints == null)
            return null;
        else if (_startingPoints.isEmpty())
            return new int[] {1};
        return integerCollectionToIntArray(removeFactors(getCombined()));
    }

    /** Combines all the possible sectors/frame into one collection.
     *  All duplicates are removed. */
    private TreeSet<Integer> getCombined() {
        TreeSet<Integer> unique = new TreeSet<Integer>();
        for (SectorsPerFrameFromStart startAndInterval : _startingPoints) {
            if (startAndInterval._possibleSectorsPerFrame != null)
                unique.addAll(startAndInterval._possibleSectorsPerFrame);
        }
        return unique;
    }


    /** Removes Integer values that are just factors of other 
     *  Integer values in the Collection and returns the result.
     *  The original Collection is not modified. */
    private static TreeSet<Integer> removeFactors(TreeSet<Integer> sourceValues) {
        TreeSet<Integer> copy = (TreeSet<Integer>)sourceValues.clone();

        for (Iterator<Integer> it = copy.iterator(); it.hasNext();) {
            Integer oi = it.next();
            Integer[] aioValues = copy.toArray(new Integer[copy.size()]);
            for (int i = aioValues.length-1; aioValues[i] != oi; i--) {
                if (aioValues[i].intValue() % oi.intValue() == 0) {
                    it.remove();
                    break;
                }
            }
        }
        return copy;
    }

    /** Converts Collection of Integer classes to int[]. */
    private static int[] integerCollectionToIntArray(Collection<Integer> intIterable) {
        if (intIterable.isEmpty())
            return null;
        int[] ai = new int[intIterable.size()];
        int i = 0;
        for (Integer oi : intIterable) {
            ai[i] = oi.intValue();
            i++;
        }
        return ai;
    }


    /** Tracks possible sectors/frame for a video, assuming the 2nd frame 
     * started at a particular sector. */
    private static class SectorsPerFrameFromStart implements Comparable<SectorsPerFrameFromStart> {
        /** Number of the sector that we are assuming the 2nd frame in a video started at. */
        private final int _iFrame2StartSector;
        /** 3 possible states:
         * no frame yet = null,
         * some possibilities .size() > 0,
         * no possibilities .size() == 0 */
        private TreeSet<Integer> _possibleSectorsPerFrame;

        public SectorsPerFrameFromStart(int iFrame2StartSector) {
            _iFrame2StartSector = iFrame2StartSector;
        }

        /** Updates this sectors/frame possibility with another space between frames.
         *  @return  If any sectors/frame possibilities exist other than 1. */
        private boolean update(int iLastSectorOfPreviousFrame, int iFirstSectorOfNewFrame) {
            final int iLastSectOfPrevFrmFromStart = iLastSectorOfPreviousFrame+1-_iFrame2StartSector;
            final int iFirstSectOfNewFrmFromStart = iFirstSectorOfNewFrame-_iFrame2StartSector;

            if (_possibleSectorsPerFrame == null) {
                // first time in this function
                // fill _possibleSectorsPerFrame with an
                // initial set of possible sectors/frame
                _possibleSectorsPerFrame = new TreeSet<Integer>();
                for (int i=2; i <= iFirstSectOfNewFrmFromStart; i++)
                    _possibleSectorsPerFrame.add(i);
            } else if (_possibleSectorsPerFrame.isEmpty()) {
                // all possibilities have been exhausted in previous calls
                return false;
            }

            // retain only the possible sectors/frame that will
            // land in this space between frames
            TreeSet<Integer> retainedPossibleSPF = new TreeSet<Integer>();
            for (Integer oiSectorsPerFrame : _possibleSectorsPerFrame) {
                for (int iModCheck = iLastSectOfPrevFrmFromStart;
                     iModCheck <= iFirstSectOfNewFrmFromStart;
                     iModCheck++)
                {
                    if (iModCheck % oiSectorsPerFrame.intValue() == 0) {
                        retainedPossibleSPF.add(oiSectorsPerFrame);
                        break;
                    }
                }
            }

            _possibleSectorsPerFrame = retainedPossibleSPF;
            return _possibleSectorsPerFrame.size() > 0;
        }
        
        /** Sort based on starting sector. */
        public int compareTo(SectorsPerFrameFromStart o) {
            if (_iFrame2StartSector < o._iFrame2StartSector)
                return -1;
            else if (_iFrame2StartSector > o._iFrame2StartSector)
                return 1;
            else
                return 0;
        }

    }

}