/*
 * Copyright (c) 2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * -Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduct the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed,licensed or intended for use in 
 * the design, construction, operation or maintenance of any nuclear facility.
 */
package nie.filters.io;

import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * A subclass of <code>SeekableStream</code> that may be used to wrap
 * a regular <code>InputStream</code>.  Seeking backwards is supported
 * by means of an in-memory cache.  For greater efficiency,
 * <code>FileCacheSeekableStream</code> should be used in
 * circumstances that allow the creation of a temporary file.
 *
 * <p> The <code>mark()</code> and <code>reset()</code> methods are
 * supported.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 * <p>This class was obtained from the decomissioned part of the JAI
 */
public final class MemoryCacheSeekableStream extends SeekableStream {

    /** The source input stream. */
    private InputStream src;

    /** Position of first unread byte. */
    private long pointer = 0;

    /** Log_2 of the sector size. */
    private static final int SECTOR_SHIFT = 9;

    /** The sector size. */
    private static final int SECTOR_SIZE = 1 << SECTOR_SHIFT;

    /** A mask to determine the offset within a sector. */
    private static final int SECTOR_MASK = SECTOR_SIZE - 1;

    /** A Vector of source sectors. */
    private Vector data = new Vector();

    /** Number of sectors stored. */
    int sectors = 0;

    /** Number of bytes read. */
    int length = 0;

    /** True if we've previously reached the end of the source stream */
    boolean foundEOS = false;

    /**
     * Constructs a <code>MemoryCacheSeekableStream</code> that takes
     * its source data from a regular <code>InputStream</code>.
     * Seeking backwards is supported by means of an in-memory cache.
     */
    public MemoryCacheSeekableStream(InputStream src) {
        this.src = src;
    }

    /**
     * Ensures that at least <code>pos</code> bytes are cached,
     * or the end of the source is reached.  The return value
     * is equal to the smaller of <code>pos</code> and the
     * length of the source stream.
     */
    private long readUntil(long pos) throws IOException {
        // We've already got enough data cached
        if (pos < length) {
            return pos;
        }
        // pos >= length but length isn't getting any bigger, so return it
        if (foundEOS) {
            return length;
        }

        int sector = (int)(pos >> SECTOR_SHIFT);

        // First unread sector
        int startSector = length >> SECTOR_SHIFT;

        // Read sectors until the desired sector
        for (int i = startSector; i <= sector; i++) {
            byte[] buf = new byte[SECTOR_SIZE];
            data.addElement(buf);
            
            // Read up to SECTOR_SIZE bytes
            int len = SECTOR_SIZE;
            int off = 0;
            while (len > 0) {
                int nbytes = src.read(buf, off, len);
                // Found the end-of-stream
                if (nbytes == -1) {
                    foundEOS = true;
                    return length;
                }
                off += nbytes;
                len -= nbytes;
                
                // Record new data length
                length += nbytes;
            }
        }

        return length;
    }

    /**
     * Returns <code>true</code> since all
     * <code>MemoryCacheSeekableStream</code> instances support seeking
     * backwards.
     */
    public boolean canSeekBackwards() {
        return true;
    }

    /**
     * Returns the current offset in this file. 
     *
     * @return     the offset from the beginning of the file, in bytes,
     *             at which the next read occurs.
     */
    public long getFilePointer() {
        return pointer;
    }

    /**
     * Sets the file-pointer offset, measured from the beginning of this 
     * file, at which the next read occurs.
     *
     * @param      pos   the offset position, measured in bytes from the 
     *                   beginning of the file, at which to set the file 
     *                   pointer.
     * @exception  IOException  if <code>pos</code> is less than 
     *                          <code>0</code> or if an I/O error occurs.
     */
    public void seek(long pos) throws IOException {
        if (pos < 0) {
            throw new IOException("MemoryCacheSeekableStream");
        }
        pointer = pos;
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     */
    public int read() throws IOException {
        long next = pointer + 1;
        long pos = readUntil(next);
        if (pos >= next) {
            byte[] buf =
                (byte[])data.elementAt((int)(pointer >> SECTOR_SHIFT));
            return buf[(int)(pointer++ & SECTOR_MASK)] & 0xff;
        } else {
            return -1;
        }
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read, possibly
     * zero. The number of bytes actually read is returned as an integer.
     *
     * <p> This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     *
     * <p> If <code>b</code> is <code>null</code>, a
     * <code>NullPointerException</code> is thrown.
     *
     * <p> If <code>off</code> is negative, or <code>len</code> is negative, or
     * <code>off+len</code> is greater than the length of the array
     * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
     * thrown.
     *
     * <p> If <code>len</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value <code>-1</code> is returned; otherwise, at least one
     * byte is read and stored into <code>b</code>.
     *
     * <p> The first byte read is stored into element <code>b[off]</code>, the
     * next one into <code>b[off+1]</code>, and so on. The number of bytes read
     * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
     * bytes actually read; these bytes will be stored in elements
     * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     *
     * <p> In every case, elements <code>b[0]</code> through
     * <code>b[off]</code> and elements <code>b[off+len]</code> through
     * <code>b[b.length-1]</code> are unaffected.
     *
     * <p> If the first byte cannot be read for any reason other than end of
     * file, then an <code>IOException</code> is thrown. In particular, an
     * <code>IOException</code> is thrown if the input stream has been closed.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      len   the maximum number of bytes to read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
     */
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if ((off < 0) || (len < 0) || (off + len > b.length)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        long pos = readUntil(pointer + len);
        // End-of-stream
        if (pos <= pointer) {
            return -1;
        }

        byte[] buf = (byte[])data.elementAt((int)(pointer >> SECTOR_SHIFT));
        int nbytes = Math.min(len, SECTOR_SIZE - (int)(pointer & SECTOR_MASK));
        System.arraycopy(buf, (int)(pointer & SECTOR_MASK),
                         b, off, nbytes);
        pointer += nbytes;
        return nbytes;
    }
}
