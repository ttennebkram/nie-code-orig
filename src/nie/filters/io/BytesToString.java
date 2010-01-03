
package nie.filters.io;

public class BytesToString {

    public static String convert(byte ascii[]) {
        return convert(ascii, 0, 0, ascii.length);
    }

    public static String convert(byte ascii[], int hibyte, int offset, int count) {
	    if (offset < 0) {
	        throw new StringIndexOutOfBoundsException(offset);
	    }
	    if (count < 0) {
	        throw new StringIndexOutOfBoundsException(count);
	    }
	    // Note: offset or count might be near -1>>>1.
	    if (offset > ascii.length - count) {
	        throw new StringIndexOutOfBoundsException(offset + count);
	    }

	    char value[] = new char[count];

	    if (hibyte == 0) {
	        for (int i = count ; i-- > 0 ;) {
		        value[i] = (char) (ascii[i + offset] & 0xff);
	        }
	    } else {
	        hibyte <<= 8;
	        for (int i = count ; i-- > 0 ;) {
		        value[i] = (char) (hibyte | (ascii[i + offset] & 0xff));
	        }
	    }
	    
	    return new String(value);
    }
    
}
