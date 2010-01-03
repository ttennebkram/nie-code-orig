/*
 * Created on Jun 24, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nie.spider;

import java.io.*;
import nie.core.*;

/**
 * @author Mark Bennett
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CachingRetriever {

	public CachingRetriever( String inCacheDirName, boolean inIsCreateOK )
		throws IOException
	{
		if( null==inCacheDirName )
			throw new IOException( "Null cache dir name passed in" );

		mCacheDir = new File( inCacheDirName );
		if( ! mCacheDir.exists() ) {
			if( ! inIsCreateOK )
				throw new IOException( "No such directory \"" + inCacheDirName + "\"" );
			else
				if( ! mCacheDir.mkdirs() )
					throw new IOException( "Unable to create cache directory \"" + inCacheDirName + "\"" );
		}

	}

	public byte [] fetchContents( String inURL )
		throws IOException
	{
		// If it's not a URL, just return the bytes with no caching
		if( ! NIEUtil.isStringAURL(inURL, true) )
			return NIEUtil.fetchURIContentsBin( inURL );
		String tmpName = urlToCacheName( inURL );
		File cacheFile = new File( mCacheDir, tmpName + ".good" );
		File badCacheFile = new File( mCacheDir, tmpName + ".bad" );
		if( badCacheFile.exists() )
			throw new IOException( "Previous attempt to retrieve this page failed." );
		byte [] outContent = null;
		if( cacheFile.exists() ) {
			outContent = NIEUtil.fetchURIContentsBin( cacheFile.toString() );
		}
		else {
			try {
				outContent = NIEUtil.fetchURIContentsBin( inURL );
				NIEUtil.writeBinaryFile( cacheFile, outContent );
			}
			catch( IOException e ) {
				// NIEUtil.writeBinaryFile( badCacheFile, e.toString().getBytes() );

				ByteArrayOutputStream buff = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream( buff );
				e.printStackTrace( ps );
				ps.close();
				buff.close();
				NIEUtil.writeBinaryFile( badCacheFile, buff.toByteArray() );

				// NIEUtil.writeBinaryFile( badCacheFile, ( ""+e ).getBytes() );
				if( cacheFile.exists() )
					cacheFile.delete();
				throw new IOException( "Retrieve failed: " + e );
			}
		}
		return outContent;
	}

	public static String urlToCacheName( String ioURL )
		throws IOException
	{
		ioURL = NIEUtil.trimmedLowerStringOrNull( ioURL );
		if( null==ioURL )
			throw new IOException( "Null url passed in" );
		ioURL = NIEUtil.zapChars( ioURL, '_' );
		return ioURL;
	}

	File mCacheDir;

}
