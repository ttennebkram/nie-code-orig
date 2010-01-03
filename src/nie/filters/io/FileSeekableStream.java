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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A subclass of <code>SeekableStream</code> that takes its input
 * from a <code>File</code> or <code>RandomAccessFile</code>.
 * Backwards seeking is supported.  The <code>mark()</code> and
 * <code>resest()</code> methods are supported.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 * <p>This class was obtained from the decomissioned part of the JAI
 */
public class FileSeekableStream extends SeekableStream {
    
    private RandomAccessFile file;
    private long markPos = -1;

    // Base 2 logarithm of the cache page size
    private static final int PAGE_SHIFT = 9;

    // The page size, derived from PAGE_SHIFT
    private static final int PAGE_SIZE = 1 << PAGE_SHIFT;

    // Binary mask to find the offset of a pointer within a cache page
    private static final int PAGE_MASK = PAGE_SIZE - 1;

    // Number of pages to cache
    private static final int NUM_PAGES = 32;

    // Reads longer than this bypass the cache
    private static final int READ_CACHE_LIMIT = PAGE_SIZE;

    // The page cache
    private byte[][] pageBuf = new byte[PAGE_SIZE][NUM_PAGES];
    
    // The index of the file page held in a given cache entry,
    // -1 = invalid.
    private int[] currentPage = new int[NUM_PAGES];

    private long length = 0L;

    private long pointer = 0L;

    /**
     * Constructs a <code>FileSeekableStream</code> from a 
     * <code>RandomAccessFile</code>.
     */
    public FileSeekableStream(RandomAccessFile file) throws IOException {
        this.file = file;
        file.seek(0L);
        this.length = file.length();

        // Set some info
        setInfo("length", String.valueOf(this.length));

        // Allocate the cache pages and mark them as invalid
        for (int i = 0; i < NUM_PAGES; i++) {
            pageBuf[i] = new byte[PAGE_SIZE];
            currentPage[i] = -1;
        }
    }

    /**
     * Constructs a <code>FileSeekableStream</code> from a 
     * <code>File</code>.
     */
    public FileSeekableStream(File file) throws IOException {
        this(new RandomAccessFile(file, "r"));
    }

    /**
     * Constructs a <code>FileSeekableStream</code> from a 
     * <code>String</code> path name.
     */
    public FileSeekableStream(String name) throws IOException {
        this(new RandomAccessFile(name, "r"));
    }

    /** Returns true since seeking backwards is supported. */
    public final boolean canSeekBackwards() {
        return true;
    }

    /**
     * Returns the length of the stream
     *
     * @return     the length of the stream
     */
    public long length() {
        return this.length;
    }

    /**
     * Returns the current offset in this stream.
     *
     * @return     the offset from the beginning of the stream, in bytes,
     *             at which the next read occurs.
     * @exception  IOException  if an I/O error occurs.
     */
    public final long getFilePointer() throws IOException {
        return pointer;
    }

    public final void seek(long pos) throws IOException {
        if (pos < 0) {
            throw new IOException("FileSeekableStream");
        }
        pointer = pos;
    }

    public final int skip(int n) throws IOException {
        pointer += n;
        return n;
    }

    private byte[] readPage(long pointer) throws IOException {
        int page = (int)(pointer >> PAGE_SHIFT);

        for (int i = 0; i < NUM_PAGES; i++) {
            if (currentPage[i] == page) {
                return pageBuf[i];
            }
        }

        // Use random replacement for now
        int index = (int)(Math.random()*NUM_PAGES);
        currentPage[index] = page;

        long pos = ((long)page) << PAGE_SHIFT;
        long remaining = length - pos;
        int len = PAGE_SIZE < remaining ? PAGE_SIZE : (int)remaining;
        file.seek(pos);
        file.readFully(pageBuf[index], 0, len);

        return pageBuf[index];
    }

    /** Forwards the request to the real <code>File</code>. */
    public final int read() throws IOException {
        if (pointer >= length) {
            return -1;
        }

        byte[] buf = readPage(pointer);
        return buf[(int)(pointer++ & PAGE_MASK)] & 0xff;
    }

    /** Forwards the request to the real <code>File</code>. */
    public final int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if ((off < 0) || (len < 0) || (off + len > b.length)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        len = (int)Math.min((long)len, length - pointer);
        if (len <= 0) {
            return -1;
        }

        // If the read is large, don't bother to cache it.
        if (len > READ_CACHE_LIMIT) {
            file.seek(pointer);
            int nbytes = file.read(b, off, len);
            pointer += nbytes;
            return nbytes;
        } else {
            byte[] buf = readPage(pointer);
        
            // Compute length to end of page
            int remaining = PAGE_SIZE - (int)(pointer & PAGE_MASK);
            int newLen = len < remaining ? len : remaining;
            System.arraycopy(buf, (int)(pointer & PAGE_MASK), b, off, newLen);
            
            pointer += newLen;
            return newLen;
        }
    }

    /** Forwards the request to the real <code>File</code>. */
    public final void close() throws IOException {
        file.close();
    }

    /**
     * Marks the current file position for later return using
     * the <code>reset()</code> method.
     */
    public synchronized final void mark(int readLimit) {
        markPos = pointer;
    }

    /**
     * Returns the file position to its position at the time of
     * the immediately previous call to the <code>mark()</code>
     * method.
     */
    public synchronized final void reset() throws IOException {
        if (markPos != -1) {
            pointer = markPos;
        }
    }

    /** Returns <code>true</code> since marking is supported. */
    public boolean markSupported() {
        return true;
    }
}
