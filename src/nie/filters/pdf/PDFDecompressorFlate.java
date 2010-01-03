
package nie.filters.pdf;

import java.io.*;
import java.util.*;
import java.util.zip.*;


/**
 * PDF Flate decompression exception
 * 
 * @author Steve Halliburton for NIE
 * @version 1.0
 * @see PDFException
 */
class FlateDecompressException extends PDFException {
    String excName = "FlateDecompressException";
    FlateDecompressException(String msg) {
        super(msg);
    }
}

/**
 * Decompresses Flate encoded data.   Flate compression is the
 * same compression used in zip files, so all this class really
 * does is run the input string through the zip decompression
 * stream.
 * 
 * @author Steve Halliburton for NIE
 * @version 1.0
 */
public class PDFDecompressorFlate {
    
    private InflaterInputStream in;
    private ByteArrayOutputStream out;
    private Hashtable attrs;
    private byte[] data;
    
    /**
     * Given a byte array of data (and some possible attributes
     * for the way the data should be decoded), setup all the 
     * IO streams for the decoding
     * 
     * @param attrs Hashtable of attributes describing how to decode the data.
     * Currently this is unused.
     * @param data The byte array of data to decompress
     */
    public PDFDecompressorFlate(Hashtable attrs, byte[] data) {
        this.attrs = attrs;
        this.data = data;
        this.in = new InflaterInputStream(new ByteArrayInputStream(data));
        this.out = new ByteArrayOutputStream();
    }
    
    
    /**
     * Actually decompress the data.   Runs the data through
     * the InflaterInputStream into a ByteArayOutputStream.  When
     * the decompression is done, a new byte array containing
     * the decompressed data is available.
     * 
     * @return A byte array containing the decompressed data
     * @exception com.nie.parser.pdf.FlateDecompressException
     */
    public byte[] decompress() throws FlateDecompressException {
        //byte[] newData = new byte[16384];
        byte[] newData = new byte[1024*512];
        int len;
        try {
            while ((len=in.read(newData, 0, newData.length)) != -1) {
                out.write(newData, 0, len);
            }
            out.close();
            in.close();
        } catch (java.util.zip.ZipException ze) {
            ze.printStackTrace();
            //throw new FlateDecompressException("ZipException during decompression");
            byte[] r = {0x0D, 0x0A};
            return r;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            throw new FlateDecompressException("IO Exception during decompression");
        }
        return out.toByteArray();    
    }    
}

