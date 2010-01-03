
package nie.filters.pdf;

import java.io.*;
import java.util.*;
import nie.filters.codec.*;

/**
 * LZW Flate decompression exception
 * 
 * @author Steve Halliburton for NIE
 * @version 1.0
 * @see PDFException
 */
class LZWDecompressException extends PDFException {
    String excName = "LZWDecompressException";
    LZWDecompressException(String msg) {
        super(msg);
    }
}

/**
 * Decompresses LZW encoded data.   This class is basically just a wrapper
 * around the com.nie.sun.codec.TIFFLZWDecoder class.
 * 
 * @author Steve Halliburton for NIE
 * @version 1.0
 * @see nie.filters.codec.TIFFLZWDecoder
 */
public class PDFDecompressorLZW {
    private Hashtable attrs;
    private byte[] data;
    private TIFFLZWDecoder decoder;

    /**
     * Given a byte array of data (and some possible attributes
     * for the way the data should be decoded), setup all the 
     * IO streams for the decoding
     * 
     * @param attrs Hashtable of attributes describing how to decode the data.
     * Currently this is unused.
     * @param data The byte array of data to decompress
     */
    public PDFDecompressorLZW(Hashtable attrs, byte[] data) {
        this.attrs = attrs;
        this.data = data;

        this.decoder = new TIFFLZWDecoder(1, 1, 1);
    }
    
    /**
     * Actually decompress the data.   Runs the data through
     * the decoder codec into a new byte array.
     * 
     * @return A byte array containing the decompressed data
     * @exception com.nie.parser.pdf.LZWDecompressException
     */
    public byte[] decompress() throws LZWDecompressException {
        byte[] newData = new byte[this.data.length];
        this.decoder.decode(this.data, newData, 1);
        return newData;
    }    
}

