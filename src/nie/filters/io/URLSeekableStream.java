
package nie.filters.io;

import java.io.*;
import java.net.*;
import java.util.*;

public class URLSeekableStream extends ByteArraySeekableStream {
    ByteArraySeekableStream stream;
    
    public URLSeekableStream(String url) throws IOException {
        this(new URL(url));
    }
    
    public URLSeekableStream(URL url) throws IOException {
        // Need to fake out the super class
        super(new byte[1]);
        
        URLConnection conn = url.openConnection();
        
        // Store some info about the connection
        setInfo("location", url.toString());        

		// mbennett: seem to get nulls sometimes
		String tmpType = conn.getContentType();
		if( null==tmpType )
			tmpType = "text/html";
        // setInfo("type", conn.getContentType());
		setInfo( "type", tmpType );

		/***
		Object obj = conn.getContentType();
		System.err.println(
			"Debug: nie.filters.ioURLSeekableStream: constructor:"
			+ " connection type: "
			+ (( null!=obj ) ? " obj type=" + obj.getClass().getName() : "")
			+ " = " + obj
			);

        // ^^^ mbennett: is sometimes null
		if( null==conn.getContentType() )
			System.err.println(
				"Warning: nie.filters.ioURLSeekableStream: constructor:"
				+ " null content type for URL \"" + url.toString() + "\""
				);
		***/

        setInfo("expires", String.valueOf(conn.getExpiration()));
        setInfo("last-modified", String.valueOf(conn.getLastModified()));
        
        InputStream in = conn.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] newData = new byte[4096];
        int len;
        
        while ((len=in.read(newData, 0, newData.length)) != -1) {
            out.write(newData, 0, len);
        } 
        out.close();
        in.close();
        
        // And now actually set everything (break some OO rules here)
        this.src = out.toByteArray();
        this.offset = 0;
        this.length = this.src.length;
        
        setInfo("length", String.valueOf(this.length));
    }
    
}