
package nie.filters.io;

import java.io.*;

public class RandomAccessData extends RandomAccessFile {

    public RandomAccessData(String fileName) 
      throws java.io.FileNotFoundException {
        super(fileName, "r");
    }
    
    public RandomAccessData(File file) 
      throws java.io.IOException {
        super(file, "r");
    }
    
    //public RandomAccessData(byte[] data) {
    //    
    //}

    
}
