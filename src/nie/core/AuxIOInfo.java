package nie.core;

import java.io.*;
import java.util.*;
import java.net.*;
import org.jdom.Element;

// Helper class used for additional communications between
// users of our file and URL IO functions
// Use this if you:
// - get/set detailed information about CGI requests
// - (future) Would like set addtional, infrequently used options
// - Would like us to provide you with additonal information about
//      our efforts
// HASH WARNING:
// Currently you are responsible for your own key normalization
public class AuxIOInfo
{

	private static final String kClassName = "AuxIOInfo";

//	// private static boolean debug = true;
//	// Don't specifically set it if you want FALSE, it'll default to that
//	private static boolean debug;
//	public static void setDebug( boolean flag )
//	{
//		debug = flag;
//	}

	public AuxIOInfo()
	{

		// Init some of the containers
		// For reading HTTP requests
		fIoHTTPHeaders = new Vector();

		fIoHTTPHeaderFieldHash = new Hashtable();
		fHTTPHeaderNamesOrigCaseOrder = new Vector();

		fIoCGIFieldHash = new Hashtable();
		fCGINamesOrigCaseOrder = new Vector();

		fIoCGIFieldHashUnnormalizedValues = new Hashtable();
		fCGINamesOrigCaseOrderUnnormalizedValues = new Vector();
	}

	// Convenience constructor that also sets the base class
	public AuxIOInfo( Class inClass )
		throws Exception
	{
		this();
		final String kFName = "constructor(Class)";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==inClass )
			throw new Exception( kExTag + "Input class was null; it is required." );
		setSystemRelativeBaseClass( inClass );
	}
	public AuxIOInfo( Class inClass, boolean inRecordInternalIncludeAttrs )
		throws Exception
	{
		this( inClass );
		setRecordInternalIncludeAttrs( inRecordInternalIncludeAttrs );
	}
	

	public void setBasicURL( String inNewURL )
	{
		final String kFName = "setURL";
		inNewURL = NIEUtil.trimmedStringOrNull( inNewURL );
		if( null == inNewURL )
			errorMsg( kFName,
				"Null/empty base URL sent in, will set field to null."
				);
		fIoBasicURL = inNewURL;
	}
	public String getBasicURL()
	{
		return fIoBasicURL;
	}
	public String getFullCGIEncodedURL()
	{
		final String kFName = "getFullCGIEncodedURL";

		String outURLString = getBasicURL();
		if( null == outURLString )
		{
			errorMsg( kFName,
				"No basic URL to add CGI varaibles to. Returning null."
				);
			return null;
		}

		// Now get all the variables as an encoded string
		String lCGIBuffer = getCGIFieldsAsEncodedBuffer();
		// traceMsg( kFName, "CGI Buffer = \"" + lCGIBuffer + "\"" );

		// If we got anything back, add it on
		if( lCGIBuffer != null && lCGIBuffer.length() > 0 )
		{
			// Join the two strings together, usually with a "?"
			outURLString = NIEUtil.concatURLAndEncodedCGIBuffer(
				outURLString, lCGIBuffer
				);
		}

		// Return whatever we're left with
		return outURLString;

	}

	public void setIPAddressFromSocket( Socket inSocket )
	{
		final String kFName = "setIPAddressFromSocket";

		if( inSocket == null )
		{
			errorMsg( kFName,
				"Was passed in a null socket, nothing to do."
				);
			return;
		}
		setIPAddress( getIPAddressStringFromSocket(inSocket) );
	}

	// Sometimes we want a unique, sequential marker for transactions
	public long stampWithTransactionID()
	{
		fTransID = fTransPrefix + (++fTransCounter);
		return fTransID;
	}
	// public String getTransactionIDStr()
	// {
	// 	return fTransID;
	//}
	public long getTransactionID()
	{
		return fTransID;
	}
	public void setIPAddress( String inAddr )
	{
		final String kFName = "setIPAddress";
		inAddr = NIEUtil.trimmedStringOrNull( inAddr );
		if( inAddr == null )
		{
			errorMsg( kFName,
				"Was passed in a null/empty IP address string, nothing to store."
				);
			return;
		}
		fHostAddress = inAddr;
	}
	public String getIPAddress()
	{
		return fHostAddress;
	}

	// A light wrapper, and protecting us from null pointer exceptions
	public static String getIPAddressStringFromSocket( Socket inSocket )
	{
		final String kFName = "getIPAddressStringFromSocket";

		if( inSocket == null )
		{
			errorMsg( kFName,
				"Was passed in a null socket, returning null."
				);
			return null;
		}
		InetAddress lAddress = inSocket.getInetAddress();
		if( lAddress == null )
		{
			errorMsg( kFName,
				"Unable to get address for socket, returning null."
				);
			return null;
		}
		return lAddress.getHostAddress();

//		byte[] lOctets = lAddress.getAddress();
//		lAddress.
//		StringBuffer buff = new StringBuffer();
//		for( int i=0 ; i<lOctets.length ; i++ )
//		{
//		    int num = lOctets[i];
//		    num = normalizeOctet( num );
//		    if( i>0 )
//				buff.append( '.' );
//		    buff.append( num );
//		}
//		return new String( buff );
	}
//	// Help us display an octet as 0-255 vs -128 to 127
//	public static int normalizeOctet( int inOctet )
//	{
//		return (inOctet < 0) ? (256 + inOctet) : inOctet;
//	}

	public String getLocalURLPath() {
		return fOutRequestedPath;
	}


	public String displayRequestIntoBuffer()
	{
		String str1 = displayRawHTTPHeaderLinesIntoBuffer();
		String str2 = displayHTTPHeaderFieldsIntoBuffer();
		String str3 = displayCGIFieldsIntoBuffer();
		return str1 + str2 + str3;
	}


	// Get the named field, force to scalar
	// Note that we do NOT normalize the output since it is
	// valid to have field= with no value, which in some applications
	// may be different then not having the value at all
	public String getScalarCGIField( String inFieldName )
	{
		final String kFName = "getScalarCGIField";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return null;
		}

		// Go ahead and return the results
		// return getScalarFromHash(
		//	fIoCGIFieldHash, inFieldName, ! isCGIFieldsCaseSensitive()
		//	);

		String outStr = getScalarFromHash(
			fIoCGIFieldHash, inFieldName, ! isCGIFieldsCaseSensitive(), true
			);

		return outStr;
	}

	public String getScalarCGIField_UnnormalizedValue( String inFieldName )
	{
		final String kFName = "getScalarCGIField_UnnormalizedValue";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return null;
		}

		// Go ahead and return the results
		// return getScalarFromHash(
		//	fIoCGIFieldHash, inFieldName, ! isCGIFieldsCaseSensitive()
		//	);

		String outStr = getScalarFromHash(
			fIoCGIFieldHashUnnormalizedValues, inFieldName,
			! isCGIFieldsCaseSensitive(), false	// true
			);

		return outStr;
	}

	// get a mutable list
	public List getCGIFieldKeys()
	{
		List keys = new Vector();
		// if( fIoCGIFieldHash != null )
		if( null!=fCGINamesOrigCaseOrder )
		{
			// Set tmpSet = fIoCGIFieldHash.keySet();
			// keys.addAll( tmpSet );
			keys.addAll( fCGINamesOrigCaseOrder );
		}
		return keys;
	}
	public List getCGIFieldKeys_UnnormalizedValue()
	{
		List keys = new Vector();
		// if( fIoCGIFieldHashUnnormalizedValues != null )
		if( fCGINamesOrigCaseOrderUnnormalizedValues != null )
		{
			// Set tmpSet = fIoCGIFieldHashUnnormalizedValues.keySet();
			// keys.addAll( tmpSet );
			keys.addAll( fCGINamesOrigCaseOrderUnnormalizedValues );
		}
		return keys;
	}

	
	public String getScalarCGIFieldTrimOrNull( String inFieldName )
	{
		String tmpStr = getScalarCGIField( inFieldName );
		tmpStr = NIEUtil.trimmedStringOrNull( tmpStr );
		return tmpStr;
	}

	public int getIntFromCGIField( String inFieldName,
		int inDefaultValue
		)
	{
		String tmpStr = getScalarCGIFieldTrimOrNull( inFieldName );
		// Warnings will be generated, if needed, from underlying routines
		return NIEUtil.stringToIntOrDefaultValue(
			tmpStr, inDefaultValue
			);
	}


	public List getMultivalueCGIField( String inFieldName )
	{
		final String kFName = "getMultivalueCGIField";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return new Vector();
		}

		// Go ahead and return the results
		return getMultivalueFromHash(
			fIoCGIFieldHash, inFieldName, ! isCGIFieldsCaseSensitive(), true
			);
	}

	public List getMultivalueCGIField_UnnormalizedValues( String inFieldName )
	{
		final String kFName = "getMultivalueCGIField_UnnormalizedValues";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return new Vector();
		}

		// Go ahead and return the results
		return getMultivalueFromHash(
			fIoCGIFieldHashUnnormalizedValues, inFieldName, ! isCGIFieldsCaseSensitive(), true
			);
	}



	// Get the fields as color=red&size=large&name=john+doe
	public String getCGIFieldsAsEncodedBuffer()
	{

		final String kFName = "getCGIFieldsAsEncodedBuffer";
		boolean debug = shouldDoDebugMsg( kFName );

		if( null==fIoCGIFieldHash || null==fCGINamesOrigCaseOrder )
		{
			errorMsg( kFName,
				"No cgi fields to encode; null field hash."
				+ " Returning empty string."
				);
			return "";
		}

		// The output buffer
		StringBuffer outBuff = new StringBuffer();

		// Loop through the hash keys
		// Set keys = fIoCGIFieldHash.keySet();
		Collection keys = ( null!=fCGINamesOrigCaseOrder )
							? (Collection)fCGINamesOrigCaseOrder
							: (Collection)(fIoCGIFieldHash.keySet())
							;
		if(debug) {
			debugMsg( kFName, "orig keys = " + keys );
			debugMsg( kFName, "Will encode " + keys.size() + " keys." );
		}

		for( Iterator it = keys.iterator(); it.hasNext() ; )
		{
			// Get the key
			String newKey = (String) it.next();

			// Save the original version
			String origKey = newKey;
			// Normalize for the hash if needed
			newKey = isCGIFieldsCaseSensitive() ? newKey : newKey.toLowerCase();

			// We need to encode the key into x-www-form-urlencoded format
			// String encodedKey = URLEncoder.encode( newKey );
			// String encodedKey = URLEncoder.encode( origKey );
			String encodedKey = null;
			try {
				// Sun says W3C says to use UTF-8, see
				// http://java.sun.com/j2se/1.4.2/docs/api/java/net/URLDecoder.html#decode(java.lang.String,%20java.lang.String)
				// http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
				encodedKey = URLEncoder.encode( origKey, AuxIOInfo.CHAR_ENCODING_UTF8 );
			}
			catch( UnsupportedEncodingException e )
			{
				encodedKey = URLEncoder.encode( origKey );
				warningMsg( kFName, "URLEncoder.encode threw exception for standard encoding, resorting to older encode, for key '" + origKey + "', exception: " + e );
			}

			// Now get the object
			Object obj = fIoCGIFieldHash.get( newKey );

			// If it's a simple string, just add it in
			if( obj instanceof String ) {
				// Convert to String
				String strValue = (String)obj;

				// We need to encode the value into x-www-form-urlencoded format
				// String encodedValue = URLEncoder.encode( strValue );

				String encodedValue = null;
				try {
					// Sun says W3C says to use UTF-8, see
					// http://java.sun.com/j2se/1.4.2/docs/api/java/net/URLDecoder.html#decode(java.lang.String,%20java.lang.String)
					// http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
					encodedValue = URLEncoder.encode( strValue, AuxIOInfo.CHAR_ENCODING_UTF8 );
				}
				catch( UnsupportedEncodingException e )
				{
					encodedValue = URLEncoder.encode( strValue );
					warningMsg( kFName, "URLEncoder.encode threw exception for standard encoding, resorting to older encode, for value '" + strValue + "' (1), exception: " + e );
				}
				
				
				// Add delimiter, if not first
				if( outBuff.length() > 0 )
					outBuff.append( '&' );

				// And add it
				// The header field name
				outBuff.append( encodedKey );
				// The separator
				outBuff.append( '=' );
				// The actual value
				outBuff.append( encodedValue );

			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector ) {
				// Convert to String
				List listOfValues = (List)obj;

				// And loop through the list
				for( Iterator it2 = listOfValues.iterator(); it2.hasNext() ;)
				{
					// Grab the value
					String tmpValue = (String) it2.next();
					// We need to encode the value into x-www-form-urlencoded format
					// String encodedValue = URLEncoder.encode( tmpValue );
					String encodedValue = null;
					try {
						// Sun says W3C says to use UTF-8, see
						// http://java.sun.com/j2se/1.4.2/docs/api/java/net/URLDecoder.html#decode(java.lang.String,%20java.lang.String)
						// http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
						encodedValue = URLEncoder.encode( tmpValue, AuxIOInfo.CHAR_ENCODING_UTF8 );
					}
					catch( UnsupportedEncodingException e )
					{
						encodedValue = URLEncoder.encode( tmpValue );
						warningMsg( kFName, "URLEncoder.encode threw exception for standard encoding, resorting to older encode, for value '" + tmpValue + "' (2), exception: " + e );
					}

					// Add delimiter, if not first
					if( outBuff.length() > 0 )
						outBuff.append( '&' );

					// And add it
					// The header field name
					outBuff.append( encodedKey );
					// The separator
					outBuff.append( '=' );
					// The actual value
					outBuff.append( encodedValue );

				}
			}
			else {
				// Else we don't know what to do
				errorMsg( kFName,
					"Unable to convert requested Hash object into buffer."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Ingoring this item."
					);
			}

		}   // End of for each key in hash

		// Done
		return new String( outBuff );

	}


	// Get the fields as hidden fields underneath a presumed form tag
	public boolean addCGIFieldsToFormElemAsHiddenFields( Element inFormElem )
	{

		final String kFName = "addCGIFieldsToFormElemAsHiddenFields";

		if( null==fIoCGIFieldHash || null==fCGINamesOrigCaseOrder ) {
			errorMsg( kFName,
				"No cgi fields to encode; null field hash."
				+ " Returning failure."
				);
			return false;
		}

		boolean success = true;

		// Loop through the hash keys
		// Set keys = fIoCGIFieldHash.keySet();
		Collection keys = fCGINamesOrigCaseOrder;
		debugMsg( kFName, "Will add " + keys.size() + " keys." );
		for( Iterator it = keys.iterator(); it.hasNext() ; )
		{
			// Get the key
			String newKey = (String) it.next();

			// Normalize if needed
			newKey = isCGIFieldsCaseSensitive() ? newKey : newKey.toLowerCase();

			// Now get the object
			Object obj = fIoCGIFieldHash.get( newKey );

			// Normalize and check after we use it as a hash key
			newKey = NIEUtil.trimmedStringOrNull( newKey );
			if( null==newKey ) {
				errorMsg( kFName, "Empty field name, skipping." );
				success = false;
				continue;
			}

			// If it's a simple string, just add it in
			if( obj instanceof String )
			{
				// Convert to String
				String strValue = (String)obj;
				// Note that we will add empty strings
				strValue = strValue.trim();

				Element field = new Element( "input" );
				field.setAttribute( "type", "hidden" );
				field.setAttribute( "name", newKey );
				field.setAttribute( "value", strValue );

				inFormElem.addContent( field );

			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector )
			{
				// Convert to String
				List listOfValues = (List)obj;

				// And loop through the list
				for( Iterator it2 = listOfValues.iterator(); it2.hasNext() ;)
				{
					// Grab the value
					String tmpValue = (String) it2.next();
					// Note that we will add empty strings
					tmpValue = tmpValue.trim();

					Element field = new Element( "input" );
					field.setAttribute( "type", "hidden" );
					field.setAttribute( "name", newKey );
					field.setAttribute( "value", tmpValue );

					inFormElem.addContent( field );

				}
			}
			else {
				// Else we don't know what to do
				errorMsg( kFName,
					"Unable to convert requested Hash object into hidden form fields."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Ingoring this item."
					);
				success = false;
			}

		}   // End of for each key in hash

		// Done
		return success;
	}


	public int processMetaField( String inMetaFieldName /*, boolean inDoForceScalar*/ )
	{
		final String kFName = "processMetaField";
		if( null == inMetaFieldName ) {
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return -1;
		}
		int metaCount = 0;
		// Will always return a list, even if empty
		List metas = getMultivalueCGIField( inMetaFieldName );
		for( Iterator it = metas.iterator() ; it.hasNext() ; ) {
			String buffer = (String) it.next();
			int tmpResult = NIEUtil.cgiDecodeVarsBuffer( this, buffer );
			// metaCount++;
			if( tmpResult < 0 )
				metaCount = -1;
			else if( metaCount>= 0 )
				metaCount += tmpResult;
		}
		clearCGIField( inMetaFieldName, false );
		return metaCount;
	}


	public String getScalarHTTPFieldTrimOrNull( String inFieldName )
	{
		String tmpStr = getScalarHTTPField( inFieldName );
		tmpStr = NIEUtil.trimmedStringOrNull( tmpStr );
		return tmpStr;
	}
	public String getScalarHTTPField( String inFieldName )
	{
		final String kFName = "getScalarHTTPField";
		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return null;
		}

		// Go ahead and return the results
		return getScalarFromHash(
			fIoHTTPHeaderFieldHash, inFieldName, DEFAULT_HASH_KEY_NORMALIZE_CASE, false
			);
	}

	public List getHTTPFieldFieldKeys()
	{
		List keys = new Vector();
		// if( fIoHTTPHeaderFieldHash != null )
		if( null != fHTTPHeaderNamesOrigCaseOrder )
		{
			// Set tmpSet = fIoHTTPHeaderFieldHash.keySet();
			// keys.addAll( tmpSet );
			keys.addAll( fHTTPHeaderNamesOrigCaseOrder );
		}
		return keys;
	}


	public void addHTTPHeaderField( String inKey, String inValue )
	{
		addFieldToMultivalueHash(
			fIoHTTPHeaderFieldHash, fHTTPHeaderNamesOrigCaseOrder,
			inKey, inValue, true
			);
	}
	public void setOrOverwriteHTTPHeaderField( String inKey, String inValue )
	{
		setOrOverwriteHashValue(
			fIoHTTPHeaderFieldHash, fHTTPHeaderNamesOrigCaseOrder,
			inKey, inValue, true
			);
	}



	// Copy in the CGI fields from a second (donor) Aux Info object
	// By default, copy all fields
	public void copyInHTTPFields( AuxIOInfo inDonorInfo )
	{
		copyInHTTPFields( inDonorInfo, null );
	}
	public void copyInHTTPFields(
		AuxIOInfo inDonorInfo, List inExcludeFields
		)
	{
		final String kFName = "copyInHTTPFields";
		if( inDonorInfo == null )
		{
			errorMsg( kFName,
				"Was passed in a null donor object."
				+ " Nothing to copy in."
				);
			return;
		}

		// Go ahead and call the static hash copy method, with
		// the appropriate member fields
		combineMultivalueHashes(
						fIoHTTPHeaderFieldHash,
						fHTTPHeaderNamesOrigCaseOrder,
			inDonorInfo.fIoHTTPHeaderFieldHash,
			inDonorInfo.fHTTPHeaderNamesOrigCaseOrder,
			inExcludeFields,
			! isCGIFieldsCaseSensitive()
			);
	}





	public String getHTTPHeadersAsBuffer()
	{
		final String kFName = "getHTTPHeadersAsBuffer";

		if( fIoHTTPHeaderFieldHash == null )
		{
			errorMsg( kFName,
				"No header fields to encode."
				+ " returning empty string."
				);
			return "";
		}

		// The output buffer
		StringBuffer outBuff = new StringBuffer();

		// Loop through the hash keys
		// Set keys = fIoHTTPHeaderFieldHash.keySet();
		Collection keys = fHTTPHeaderNamesOrigCaseOrder;
		for( Iterator it = keys.iterator(); it.hasNext() ; )
		{
			String newKey = (String) it.next();

			String origKey = newKey;
			newKey = ! DEFAULT_HASH_KEY_NORMALIZE_CASE ? newKey : newKey.toLowerCase();

			// Now get the object
			Object obj = fIoHTTPHeaderFieldHash.get( newKey );

			// If it's a simple string, just add it in
			if( obj instanceof String )
			{
				// Convert to String
				String strValue = (String)obj;
				// And add it
				// The header field name
				// outBuff.append( newKey );
				outBuff.append( origKey );
				// The separator
				outBuff.append( ": " );
				// The actual value
				outBuff.append( strValue );
				// And the newline sequence
				outBuff.append( HEADER_EOL );

			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector )
			{
				// Convert to String
				List listOfValues = (List)obj;

				// And loop through the list
				for( Iterator it2 = listOfValues.iterator(); it2.hasNext() ;)
				{
					// Grab the value
					String tmpValue = (String) it2.next();
					// And add it
					// The header field name
					// outBuff.append( newKey );
					outBuff.append( origKey );
					// The separator
					outBuff.append( ": " );
					// The actual value
					outBuff.append( tmpValue );
					// And the newline sequence
					outBuff.append( HEADER_EOL );
				}
			}
			else
			{
				// Else we don't know what to do
				errorMsg( kFName,
					"Unable to convert requested Hash object into buffer."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Returning null."
					);
			}


		}   // End of for each key in hash

		// Done
		return new String( outBuff );
	}


	public String displayHTTPHeaderFieldsIntoBuffer()
	{
		return displayHashIntoBuffer(
			fIoHTTPHeaderFieldHash, fHTTPHeaderNamesOrigCaseOrder, "Parsed HTTP Fields"
			);
	}

	public String displayRawHTTPHeaderLinesIntoBuffer()
	{
		return NIEUtil.printStringListToBuffer(
			fIoHTTPHeaders, "Raw HTTP Header Lines"
			);
	}

	public void addCGIField( String inKey, long inValue )
	{
		addCGIField( inKey, ""+inValue );
	}
	public void addCGIField( String inKey, int inValue )
	{
		addCGIField( inKey, ""+inValue );
	}
	public void addCGIField( String inKey, String inValue )
	{
		addFieldToMultivalueHash(
			fIoCGIFieldHash, fCGINamesOrigCaseOrder,
			inKey, inValue, ! isCGIFieldsCaseSensitive()
			);
	}
	public void addCGIField_UnnormalizedValue( String inKey, String inValue )
	{
		addFieldToMultivalueHash(
			fIoCGIFieldHashUnnormalizedValues, fCGINamesOrigCaseOrderUnnormalizedValues,
			inKey, inValue, ! isCGIFieldsCaseSensitive()
			);
	}
	public void setOrOverwriteCGIField( String inKey, String inValue )
	{
		setOrOverwriteHashValue(
			fIoCGIFieldHash, fCGINamesOrigCaseOrder,
			inKey, inValue, ! isCGIFieldsCaseSensitive()
			);
	}
	public void setOrOverwriteCGIField_UnnormalizedValue( String inKey, String inValue )
	{
		setOrOverwriteHashValue(
			fIoCGIFieldHashUnnormalizedValues, fCGINamesOrigCaseOrderUnnormalizedValues,
			inKey, inValue, ! isCGIFieldsCaseSensitive()
			);
	}
	public void clearCGIField( String inKey )
	{
		clearHashValue(
			fIoCGIFieldHash, fCGINamesOrigCaseOrder,
			inKey, ! isCGIFieldsCaseSensitive()
			);
	}
	public void clearCGIField(
		String inKey,
		boolean inWarnIfNotPresent
		)
	{
		clearHashValue(
			fIoCGIFieldHash, fCGINamesOrigCaseOrder,
			inKey, ! isCGIFieldsCaseSensitive(), inWarnIfNotPresent
			);
	}




	// Copy in the CGI fields from a second (donor) Aux Info object
	// By default, copy all fields
	public void copyInCGIFields( AuxIOInfo inDonorInfo )
	{
		copyInCGIFields( inDonorInfo, null );
	}
	public void copyInCGIFields(
		AuxIOInfo inDonorInfo, Collection inExcludeFields
		)
	{
		final String kFName = "copyInCGIFields";
		if( inDonorInfo == null )
		{
			errorMsg( kFName,
				"Was passed in a null donor object."
				+ " Nothing to copy in."
				);
			return;
		}

		// Go ahead and call the static hash copy method, with
		// the appropriate member fields
		combineMultivalueHashes(
						fIoCGIFieldHash,
						fCGINamesOrigCaseOrder,
			inDonorInfo.fIoCGIFieldHash,
			inDonorInfo.fCGINamesOrigCaseOrder,
			inExcludeFields,
			! isCGIFieldsCaseSensitive()
			);

	}

	// Like copyIn, but will NOT replace add to existing
	// fields.  This will only add fields that are
	// not already in the request
	public void addOnlyMissingValuesToHashes(
		Hashtable inDonorInfo
		)
	{
		final String kFName = "addOnlyMissingValuesToHashes";
		if( inDonorInfo == null )
		{
			errorMsg( kFName,
				"Was passed in a null donor object."
				+ " Nothing to copy in."
				);
			return;
		}

		// Go ahead and call the static hash copy method, with
		// the appropriate member fields
		addOnlyMissingValuesToHashes(
			fIoCGIFieldHash,
			fCGINamesOrigCaseOrder,
			inDonorInfo,
			null,
			null, // inExcludeFields,
			! isCGIFieldsCaseSensitive()
			);
	}

	// Clear out all CGI fields
	public void deleteAllCGIFields()
	{
		fIoCGIFieldHash = new Hashtable();
		fCGINamesOrigCaseOrder = new Vector();
	}

	public String displayCGIFieldsIntoBuffer()
	{
		return displayHashIntoBuffer(
			fIoCGIFieldHash, fCGINamesOrigCaseOrder, "Parsed CGI Fields"
			);
	}

	static final void ____Generic_Hash_Routines____(){}
	////////////////////////////////////////////////////////////

	private static String getScalarFromHash(
		// Hashtable inHash, String inKey
		Hashtable inHash, String inKey, boolean inNormalizeCase, boolean inDoKillNbsp
		)
	{
		final String kFName = "getScalarFromHash";

		// lNormalizeCase = isCGIFieldsCaseSensitive();

		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Returning null."
				);
			return null;
		}

		inKey = NIEUtil.trimmedStringOrNull( inKey );
		if( inKey == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return null;
		}

		// Normalize if asked to do so
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// Return null if we don't have it
		// No need for a warning, this is a normal event
		if( ! inHash.containsKey( inKey ) )
			return null;

		// Get the object and see what it is
		Object obj = inHash.get( inKey );

		// If it's a simple string, just return it
		if( obj instanceof String ) {
			String tmpStr = (String) obj;
			if( inDoKillNbsp )
				tmpStr = NIEUtil.replaceChars( tmpStr, NIEUtil.NBSP, K_REPL_CHAR );
			return tmpStr;
		}

		if( obj instanceof List || obj instanceof Vector )
		{
			List tmpList = (List)obj;
			StringBuffer strbuff = new StringBuffer();
			for( Iterator it = tmpList.iterator(); it.hasNext(); )
			{
				String nextStr = (String)it.next();
				if( inDoKillNbsp )
					nextStr = NIEUtil.replaceChars( nextStr, NIEUtil.NBSP, K_REPL_CHAR );

				// If it's the first to be added, just append it
				if( strbuff.length() < 1 )
				{
					strbuff.append( nextStr );
				}
				// It's not the first, so add delimiter and then string
				else
				{
					strbuff.append( DEFAULT_MULTI_VALUE_FIELD_DELIMITER );
					strbuff.append( nextStr );
				}
			}
			// Convert string to buffer and return
			return new String( strbuff );
		}

		// Else we don't know what to do
		errorMsg( kFName,
			"Unable to convert requested Hash object to scalar String."
			+ " Requested hash key = \"" + inKey + "\""
			+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
			+ " Returning null."
			);
		return null;
	}

	// We will ALWAYS return a list, even if it's zero items long
	// Return values are NOT normalized
	private static List getMultivalueFromHash(
		Hashtable inHash, String inKey, boolean inNormalizeCase, boolean inDoKillNbsp
		)
	{
		final String kFName = "getMultivalueFromHash";

		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Returning null."
				);
			return new Vector();
		}

		inKey = NIEUtil.trimmedStringOrNull( inKey );
		if( inKey == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return new Vector();
		}

		// Normalize if asked to do so
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// Return null if we don't have it
		// No need for a warning, this is a normal event
		if( ! inHash.containsKey( inKey ) )
			return new Vector();

		// Start working on the return value
		List outVect = new Vector();

		// Get the object and see what it is
		Object obj = inHash.get( inKey );

		// If it's a simple string, just add it in
		if( obj instanceof String )
		{
			String tmpStr = (String)obj;
			// Accented lower case letter a, hex=A0, dec=160, oct=240
			if( inDoKillNbsp )
				tmpStr = NIEUtil.replaceChars( tmpStr, NIEUtil.NBSP, K_REPL_CHAR );
				// ^^^ he does a quick escape if char is not there at all

			outVect.add( tmpStr );
		}
		// If it's a list, add the list's elements
		// Todo: could scan list and do error checking
		// Todo: could have option to normalize values, skip nulls, etc.
		else if( obj instanceof List || obj instanceof Vector )
		{
			List tmpList = (List)obj;
			if( ! inDoKillNbsp )
				outVect.addAll( tmpList );
			else {
				for( Iterator it = tmpList.iterator(); it.hasNext() ; ) {
					String tmpStr = (String) it.next();
					// Accented lower case letter a, hex=A0, dec=160, oct=240
					tmpStr = NIEUtil.replaceChars( tmpStr, NIEUtil.NBSP, K_REPL_CHAR );
					// ^^^ he does a quick escape if char is not there at all
					outVect.add( tmpStr );
				}
			}
		}
		else
		{
			// Else we don't know what to do
			errorMsg( kFName,
				"Unable to convert requested Hash object into multivalue list."
				+ " Requested hash key = \"" + inKey + "\""
				+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
				+ " Returning null."
				);
		}
		// Return the answer, whatever it is
		return outVect;
	}




	// Add a field to a hash.  If there is already a value (or values),
	// make a list and add this to the end
	// This version adds in an additional scalar field value
	private static void setMultivalueIntoHash(
		Hashtable inHash, Collection optKeepOrigCaseFieldNames,
		String inKey, String inValue,
		boolean inNormalizeCase
		)
	{
		final String kFName = "getMultivalueIntoHash";
		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nowhere to store values, will discard values."
				);
			return;
		}

		// Normalize inputs
		inKey = NIEUtil.trimmedStringOrNull( inKey );
		// Note that we don't normalize the value

		// Sanity check
		if( inKey == null || inValue == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name or null value."
				+ ", key=\"" + inKey + "\""
				+ ", value=\"" + inValue + "\""
				+ " Nothing to store."
				);
			return;
		}

		// Normalize if asked to do so
		String origKey = inKey;
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// If we don't have it, which is usually the case, just add it
		if( ! inHash.containsKey( inKey ) )
		{
			inHash.put( inKey, inValue );
			if( null!=optKeepOrigCaseFieldNames )
				optKeepOrigCaseFieldNames.add( origKey );
			return;
		}

		// OK, so it DOES already have this key

		// The list we will create / update and restore into the hash
		List lVect = null;

		// Get the object and see what it is
		Object obj = inHash.get( inKey );

		// If it's a simple string, just add it in
		if( obj instanceof String )
		{
			lVect = new Vector();
			// Add the old value
			lVect.add( (String)obj );
			// And then add the new value
			lVect.add( inValue );
			// Store the new vector
			inHash.put( inKey, lVect );
		}
		// If it's a list, add the list's elements
		// Todo: could scan list and do error checking
		// Todo: could have option to normalize values, skip nulls, etc.
		else if( obj instanceof List || obj instanceof Vector )
		{
			lVect = (List)obj;
			lVect.add( inValue );
			// No need to store in hash, it's already there, we were
			// just using a reference
		}
		else
		{
			// Else we don't know what to do
			errorMsg( kFName,
				"Unable to convert requested Hash object into multivalue list."
				+ " Requested hash key = \"" + inKey + "\""
				+ ", value = \"" + inValue + "\""
				+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
				+ " Returning null."
				);
			// And we leave the obj that we found "as is"
		}

		// Done
	}


	private static void addFieldToMultivalueHash(
		Hashtable inHash, Collection optKeepOrigCaseFieldNames,
		String inKey, String inValue,
		boolean inNormalizeCase
		)
	{
		final String kFName = "addFieldToMultivalueHash";

		if( null==inKey || null==inValue ) {
			errorMsg( kFName, "Null input(s):"
				+ "inHash=" + inHash + ", inKey=" + inKey
				+ "inValue=" + inValue + "."
				+ " Can not add to list with null values."
				);
			return;
		}
		List tmpList = new Vector();
		tmpList.add( inValue );

		// the main method with a one element list
		addFieldsToMultivalueHash(
			inHash, optKeepOrigCaseFieldNames, inKey, tmpList, inNormalizeCase
			);

	}


	// Add a field to a hash.  If there is already a value (or values),
	// make a list and add this to the end
	// This version adds in a list of additional field values
	private static void addFieldsToMultivalueHash(
		Hashtable inHash, Collection optKeepOrigCaseFieldNames,
		String inKey, List inValues,
		boolean inNormalizeCase
		)
	{
		final String kFName = "addFieldToMultivalueHash(2)";
		if( null==inHash || null==inValues ) {
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nowhere to store values, will discard values."
				);
			return;
		}

		// Normalize inputs
		inKey = NIEUtil.trimmedStringOrNull( inKey );

		// Sanity check
		if( inKey == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ ", key=\"" + inKey + "\""
				+ " Nothing to store."
				);
			return;
		}

		String origKey = inKey;
		// Normalize if asked to do so
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// If we don't have it, which is usually the case, just add it
		if( ! inHash.containsKey( inKey ) )
		{
			inHash.put( inKey, inValues );
			if( null != optKeepOrigCaseFieldNames )
				optKeepOrigCaseFieldNames.add( origKey );
			return;
		}

		// Else we have seen it before

		// The list we will create / update and restore into the hash
		List lVect = null;

		// Get the object and see what it is
		Object obj = inHash.get( inKey );

		// If it's a simple string, just add it in
		if( obj instanceof String )
		{
			lVect = new Vector();
			// Add the old value
			lVect.add( (String)obj );
			// And then add the new value
			lVect.addAll( inValues );
			// Store the new vector
			inHash.put( inKey, lVect );
		}
		// If it's a list, add the list's elements
		// Todo: could scan list and do error checking
		// Todo: could have option to normalize values, skip nulls, etc.
		else if( obj instanceof List || obj instanceof Vector )
		{
			lVect = (List)obj;
			lVect.addAll( inValues );
			// No need to store in hash, it's already there, we were
			// just using a reference
		}
		else
		{
			// Else we don't know what to do
			errorMsg( kFName,
				"Unable to convert requested Hash object into multivalue list."
				+ " Requested hash key = \"" + inKey + "\""
				+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
				+ " Returning null."
				);
			// And we leave the obj that we found "as is"
		}

		// Done
	}






	// Join the contents of a second multivalue hash to the original
	// TODO: could be slightly more efficient
	private static void combineMultivalueHashes(
		Hashtable inCombinedHash,
		Collection optCombinedOrigKeys,
		Hashtable inAdditionalValuesHash,
		Collection optAdditionalOrigKeys,
		Collection inExcludeKeys,
		boolean inNormalizeCase
		)
	{
		final String kFName = "combineMultivalueHashes";
		boolean debug = shouldDoDebugMsg(kFName);

		if( null==inCombinedHash || null==inAdditionalValuesHash ) {
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nothing to do."
				);
			return;
		}

		if( debug ) {
			debugMsg( kFName,
				"Asked to combine two hashes."
				+ " Hash 1 (destination) already has "
					+ inCombinedHash
				+ " Hash 2 (donar) has "
					+ inAdditionalValuesHash
				);

			if( null!=optCombinedOrigKeys || null!=optAdditionalOrigKeys )
				debugMsg( kFName,
					"Recieving (dest) has orig keys " + optCombinedOrigKeys
					+ " Donor has orig keys " + optAdditionalOrigKeys
					);
			else
				debugMsg( kFName, "No orig case key lists." );

			if( null!=inExcludeKeys )
				debugMsg( kFName,
					"Exclude list has "
					+ inExcludeKeys.size() + " keys."
					);
			else
				debugMsg( kFName, "No exclude list." );
		}



		// The hash we will use for quick lookup of excluded values
		// Hashtable lExcludeHash = new Hashtable();
		HashSet excludeKeys = new HashSet();
		// Were there any keys to exclude?
		if( inExcludeKeys != null )
		{
			// For each key
			for( Iterator it = inExcludeKeys.iterator(); it.hasNext() ; )
			{
				String tmpKey = (String) it.next();
				tmpKey = NIEUtil.trimmedStringOrNull( tmpKey );
				if( tmpKey == null )
					continue;
					// Todo: could add warning
				// Normalize if needed
				if( inNormalizeCase )
					tmpKey = tmpKey.toLowerCase();
				// Only store if not in the hash
				// if( ! lExcludeHash.containsKey( tmpKey ) )
				if( ! excludeKeys.contains( tmpKey ) )
				{
					// lExcludeHash.put( tmpKey, tmpKey );
					excludeKeys.add( tmpKey );
				}
				// else it already had this key
				//  Todo: I suppose we could have an option to warn them
			}
		}   // End if we have some keys to exclude


		// Loop through the second hash
		// Set newKeys = inAdditionalValuesHash.keySet();
		Collection newKeys = (null!=optAdditionalOrigKeys)
			? optAdditionalOrigKeys
			: inAdditionalValuesHash.keySet()
			;
		for( Iterator it2 = newKeys.iterator(); it2.hasNext() ; )
		{
			String newKey = (String) it2.next();
			newKey = NIEUtil.trimmedStringOrNull( newKey );
			if( newKey == null ) {
				warningMsg( kFName,
					"Empty key in donor hash " + inAdditionalValuesHash
					+ " Ignoring."
					);
				continue;
			}
			// Normalize if asked to do so
			String lCheckKey = newKey;
			if( inNormalizeCase )
				lCheckKey = lCheckKey.toLowerCase();
			// If it's in the exclude hash, ignore it
			// if( excludeKeys.containsKey( lCheckKey ) )
			if( excludeKeys.contains( lCheckKey ) )
				continue;
				// No warning necessary, this is an expected condition

			// Now get the object
			Object obj = null;
			if( inAdditionalValuesHash.containsKey(lCheckKey) )
				obj = inAdditionalValuesHash.get( lCheckKey );
			else if( inAdditionalValuesHash.containsKey(newKey) )
				obj = inAdditionalValuesHash.get( newKey );
			else {
				warningMsg( kFName,
					"Expected values not found in hash for key " + newKey+'/'+lCheckKey
					+ " Ignoring."
					);
				continue;
			}

			// If it's a simple string, just add it in
			if( obj instanceof String ) {
				// Convert to String
				String strValue = (String)obj;
				// Call static method to add it in
				addFieldToMultivalueHash(
					inCombinedHash, optCombinedOrigKeys,
					newKey, strValue,
					inNormalizeCase
					);
			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector ) {
				// Convert reference to List
				List listOfValues = (List)obj;
				// Call static method to add it in
				addFieldsToMultivalueHash(
					inCombinedHash, optCombinedOrigKeys,
					newKey, listOfValues,
					inNormalizeCase
					);
			} else {
				// Else we don't know what to do
				errorMsg( kFName,
					"Unable to convert requested Hash object into multivalue list."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Returning null."
					);
			}
		}

		// Done
		if( shouldDoDebugMsg( kFName ) ) {
			debugMsg( kFName,
				"At end."
				+ " Hash 1 (destination) now has "
				+ inCombinedHash.keySet().size() + " keys."
				);
		}
	}

	// Only add in items that are not already there at all
	// TODO: could be slightly more efficient
	private static void addOnlyMissingValuesToHashes(
		Hashtable inCombinedHash,
		Collection optCombinedOrigKeys,
		Hashtable inAdditionalValuesHash,
		Collection optAdditionalOrigKeys,
		Collection inExcludeKeys,
		boolean inNormalizeCase
		)
	{
		final String kFName = "addOnlyMissingValuesToHashes";
		boolean debug = shouldDoDebugMsg(kFName);

		if( null==inCombinedHash || null==inAdditionalValuesHash ) {
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nothing to do."
				);
			return;
		}

		if( debug ) {
			debugMsg( kFName,
				"Asked to combine two hashes."
				+ " Hash 1 (destination) already has "
					+ inCombinedHash
				+ " Hash 2 (donar) has "
					+ inAdditionalValuesHash
				);

			if( null!=optCombinedOrigKeys || null!=optAdditionalOrigKeys )
				debugMsg( kFName,
					"Recieving (dest) has orig keys " + optCombinedOrigKeys
					+ " Donor has orig keys " + optAdditionalOrigKeys
					);
			else
				debugMsg( kFName, "No orig case key lists." );

			if( null!=inExcludeKeys )
				debugMsg( kFName,
					"Exclude list has "
					+ inExcludeKeys.size() + " keys."
					);
			else
				debugMsg( kFName, "No exclude list." );
		}



		// The hash we will use for quick lookup of excluded values
		// Hashtable lExcludeHash = new Hashtable();
		HashSet excludeKeys = new HashSet();
		// Were there any keys to exclude?
		if( inExcludeKeys != null )
		{
			// For each key
			for( Iterator it = inExcludeKeys.iterator(); it.hasNext() ; )
			{
				String tmpKey = (String) it.next();
				tmpKey = NIEUtil.trimmedStringOrNull( tmpKey );
				if( tmpKey == null )
					continue;
					// Todo: could add warning
				// Normalize if needed
				if( inNormalizeCase )
					tmpKey = tmpKey.toLowerCase();
				// Only store if not in the hash
				// if( ! lExcludeHash.containsKey( tmpKey ) )
				if( ! excludeKeys.contains( tmpKey ) )
				{
					// lExcludeHash.put( tmpKey, tmpKey );
					excludeKeys.add( tmpKey );
				}
				// else it already had this key
				//  Todo: I suppose we could have an option to warn them
			}
		}   // End if we have some keys to exclude

		// We also exclude all the keys that are already there
		for( Iterator dit = inCombinedHash.keySet().iterator(); dit.hasNext(); )
		{
			String tmpKey = (String) dit.next();
			tmpKey = NIEUtil.trimmedStringOrNull( tmpKey );
			if( tmpKey == null )
				continue;
				// Todo: could add warning
			// Normalize if needed
			if( inNormalizeCase )
				tmpKey = tmpKey.toLowerCase();
			// Only store if not in the hash
			if( ! excludeKeys.contains( tmpKey ) )
			{
				excludeKeys.add( tmpKey );
			}			
		}

		// Loop through the second hash
		// Set newKeys = inAdditionalValuesHash.keySet();
		Collection newKeys = (null!=optAdditionalOrigKeys)
			? optAdditionalOrigKeys
			: inAdditionalValuesHash.keySet()
			;
		for( Iterator it2 = newKeys.iterator(); it2.hasNext() ; )
		{
			String newKey = (String) it2.next();
			newKey = NIEUtil.trimmedStringOrNull( newKey );
			if( newKey == null ) {
				warningMsg( kFName,
					"Empty key in donor hash " + inAdditionalValuesHash
					+ " Ignoring."
					);
				continue;
			}
			// Normalize if asked to do so
			String lCheckKey = newKey;
			if( inNormalizeCase )
				lCheckKey = lCheckKey.toLowerCase();
			// If it's in the exclude hash, ignore it
			// if( excludeKeys.containsKey( lCheckKey ) )
			if( excludeKeys.contains( lCheckKey ) )
				continue;
				// No warning necessary, this is an expected condition

			// Now get the object
			Object obj = null;
			if( inAdditionalValuesHash.containsKey(lCheckKey) )
				obj = inAdditionalValuesHash.get( lCheckKey );
			else if( inAdditionalValuesHash.containsKey(newKey) )
				obj = inAdditionalValuesHash.get( newKey );
			else {
				warningMsg( kFName,
					"Expected values not found in hash for key " + newKey+'/'+lCheckKey
					+ " Ignoring."
					);
				continue;
			}

			// If it's a simple string, just add it in
			if( obj instanceof String ) {
				// Convert to String
				String strValue = (String)obj;
				// Call static method to add it in
				addFieldToMultivalueHash(
					inCombinedHash, optCombinedOrigKeys,
					newKey, strValue,
					inNormalizeCase
					);
			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector ) {
				// Convert reference to List
				List listOfValues = (List)obj;
				// Call static method to add it in
				addFieldsToMultivalueHash(
					inCombinedHash, optCombinedOrigKeys,
					newKey, listOfValues,
					inNormalizeCase
					);
			} else {
				// Else we don't know what to do
				errorMsg( kFName,
					"Unable to convert requested Hash object into multivalue list."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Returning null."
					);
			}
		}

		// Done
		if( shouldDoDebugMsg( kFName ) ) {
			debugMsg( kFName,
				"At end."
				+ " Hash 1 (destination) now has "
				+ inCombinedHash.keySet().size() + " keys."
				);
		}
	}


	private void setOrOverwriteHashValue(
		Hashtable inHash, Collection optOrigKeys,
		String inKey, String inValue,
		boolean inNormalizeCase
		)
	{
		final String kFName = "setOrOverwriteHashValue";
		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nowhere to store values, will discard values."
				);
			return;
		}

		// Normalize inputs
		inKey = NIEUtil.trimmedStringOrNull( inKey );
		// Note that we don't normalize the value

		// Sanity check
		if( null==inKey || null==inValue ) {
			errorMsg( kFName,
				"Was passed in null/empty field name or null value."
				+ ", key=\"" + inKey + "\""
				+ ", value=\"" + inValue + "\""
				+ " Nothing to store."
				);
			return;
		}

		// Normalize if asked to do so
		String origKey = inKey;
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// Store original key if not stored before
		if( null!=optOrigKeys && ! inHash.containsKey(inKey) )
			optOrigKeys.add( origKey );
		// Store the actual value
		inHash.put( inKey, inValue );
	}


	private void clearHashValue(
		Hashtable inHash, Collection optOrigKeys,
		String inKey,
		boolean inNormalizeCase
		)
	{
		clearHashValue( inHash, optOrigKeys, inKey, inNormalizeCase, true );
	}

	private void clearHashValue(
		Hashtable inHash, Collection optOrigKeys,
		String inKey,
		boolean inNormalizeCase,
		boolean inWarnIfNotPresent
		)
	{
		final String kFName = "clearHashValue";
		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nowhere to store values, will discard values."
				);
			return;
		}

		// Normalize inputs
		inKey = NIEUtil.trimmedStringOrNull( inKey );
		// Note that we don't normalize the value

		// Sanity check
		if( inKey == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ ", key=\"" + inKey + "\""
				+ " Nothing to delete."
				);
			return;
		}

		// Normalize if asked to do so
		String origKey = inKey;
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// Hashes generally have the correct behavior we want
		// Still nice to have a method for it, for consistency
		if( inHash.containsKey( inKey ) ) {
			inHash.remove( inKey );
		}
		else {
			if( inWarnIfNotPresent ) {
				warningMsg( kFName,
					"Key not found in hash"
					+ ", key=\"" + inKey + "\""
					+ " Nothing to delete."
					);
			}
			return;
		}

		// The list of original keys is in random casing
		if( null!=optOrigKeys ) {
			// If case sensitive, look for the original key
			if( ! inNormalizeCase )
				optOrigKeys.remove( origKey );
			// Else case-insenstive, we'll have to do a full scan
			else {
				String foundKey = null;
				for( Iterator it=optOrigKeys.iterator() ; it.hasNext() ; ) {
					String candidateKey = (String) it.next();
					if( inKey.equalsIgnoreCase( candidateKey ) ) {
						foundKey = candidateKey;
						break;
					}
				}
				if( null!=foundKey )
					optOrigKeys.remove( foundKey );		
				else
					errorMsg( kFName,
						"Error removing original key from list/hash pair."
						+ " key=" + origKey + '/' + inKey
						+ " paired key list = " + optOrigKeys
						);
			}
		}

	}




	// list the entire hash into a text buffer
	private String displayHashIntoBuffer(
		Hashtable inHash, Collection optOrigKeys,
		String optHashName
		)
	{
		final String kFName = "displayHashIntoBuffer";
		final String nl = "\r\n";

		if( inHash == null )
		{
			String msg =
				"Error: AuxIOInfo: displayHashIntoBuffer:"
				+ " No hash to display."
				+ " Returning this error message."
				;
			errorMsg( kFName, msg );
			return msg + nl;
		}

		// The output buffer
		StringBuffer outBuff = new StringBuffer();
		if( optHashName != null )
			outBuff.append( "Displaying hash \"" + optHashName + "\"" + nl );

		// Loop through the hash keys
		// Set keys = inHash.keySet();
		Collection keys = (null!=optOrigKeys)
			? optOrigKeys
			: inHash.keySet()
			;
		for( Iterator it = keys.iterator(); it.hasNext() ; )
		{
			// Get the key
			String newKey = (String) it.next();
			String normKey = newKey.toLowerCase();

			// Now get the object
			// Object obj = inHash.get( newKey );
			Object obj = null;
			if( inHash.containsKey( newKey ) )
				obj = inHash.get( newKey );
			else if( inHash.containsKey( normKey ) )
				obj = inHash.get( normKey );
			else {
				errorMsg( kFName,
					"Can't find values for key " + newKey+'/'+normKey + ", ignoring."
					);
				continue;
			}

			// If it's a simple string, just add it in
			if( obj instanceof String )
			{
				// Convert to String
				String strValue = (String)obj;

				outBuff.append( "\tkey \"" + newKey + "\"");
				outBuff.append( " (scalar value) \"" + strValue + "\"" + nl );

			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector )
			{
				// Convert to String
				List listOfValues = (List)obj;

				outBuff.append( "\tkey \"" + newKey + "\"" );
				outBuff.append( " multivalue with " + listOfValues.size()
					+ " entries:" + nl
					);

				// And loop through the list
				for( Iterator it2 = listOfValues.iterator(); it2.hasNext() ;)
				{
					// Grab the value
					String tmpValue = (String) it2.next();
					outBuff.append( "\t\t\"" + tmpValue + "\"" + nl );
				}
			}
			else
			{
				// Else we don't know what to do
				outBuff.append(
					"Error: AuxIOInfo: getCGIFieldsAsEncodedBuffer:"
					+ " Unable to convert requested Hash object into buffer."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Ingoring this item."
					+ nl
					);
			}

		}   // End of for each key in hash

		// Done
		return new String( outBuff );

	}





	private static final void ____Lower_Level____(){}
	////////////////////////////////////////////////////////////

	public int getHTTPResponseCode()
	{
		return fIoHTTPResponseCode;
	}
	// You can ask for code and give us a default to use
	// Or ask for a for the code and ask US to pick a default for you
	public int getHTTPResponseCodeWithDefault()
	{
		// When choosing a default value, we should check for a redirect
		// field first
		String tmpLoc = getDesiredRedirectURL();

		// If no redirect URL, default to regular default code
		if( tmpLoc == null || tmpLoc.trim().equals("") )
		{
			return getHTTPResponseCodeWithDefault(
				DEFAULT_HTTP_RESPONSE_CODE
				);
		}
		// Else there is a redirect field, so the DEFAULT would be for that
		else
		{
			return getHTTPResponseCodeWithDefault(
				DEFAULT_HTTP_REDIRECT_CODE
				);
		}
	}
	public int getHTTPResponseCodeWithDefault( int inDefaultCode )
	{
		int tmpCode = getHTTPResponseCode();
		if( tmpCode < 1 )
			tmpCode = inDefaultCode;
		return tmpCode;
	}
	public void setHTTPResponseCode( int inCode )
	{
		fIoHTTPResponseCode = inCode;
	}

	// NOTE: Content Type is the same thing as Mime Type
	// Make this VERY VISIBLE
	public static void ___Content_Type_same_as_Mime_Type___() {}
	//////////////////////////////////////////////////////////
	
	public String getContentTypeWithDefault()
	{
		return getContentTypeWithDefault( DEFAULT_MIME_TYPE );
	}
	// SAME AS getContentTypeWithDefault
	public String getMimeTypeWithDefault()
	{
		return getContentTypeWithDefault();
	}
	public String getContentTypeWithDefault( String inDefaultMimeType )
	{
		String tmpStr = getContentType();
		if( tmpStr != null )
			return tmpStr;
		else
			return inDefaultMimeType;
	}
	// SAME AS getContentTypeWithDefault
	public String getMimeTypeWithDefault( String inDefaultMimeType )
	{
		return getContentTypeWithDefault( inDefaultMimeType );
	}

	public String getContentType()
	{
		// If it's already set, return it
		if( fIoContentType != null )
			return fIoContentType;
		// Else if we have headers, look in there
		String outType = null;
		if( fIoHTTPHeaderFieldHash != null )
		{
			outType = getScalarHTTPFieldTrimOrNull( "content-type" );
		}
		if( outType != null )
			outType = outType.toLowerCase();
		return outType;
	}
	// SAME AS getContentType
	public String getMimeType()
	{
		return getContentType();
	}
		
	public void setContentType( String inType )
	{
		fIoContentType = inType;
	}
	// SAME AS setContentType
	public void setMimeType( String inType )
	{
		setContentType( inType );
	}

	// This is the EXPLICITLY set or detected value
	public String getEncodingOrNull()
	{
		return fIoEncoding;
	}
	public void setEncoding( String inType )
	{
		fIoEncoding = inType;
	}

	// This is the IMPLICIT version based on all the
	// information we have and our defaults
	public String calculateEncoding(
			String inBaseName,
			String optRelativeRoot
	) {
		final String kFName = "calculateEncoding";
		boolean debug = shouldDoDebugMsg( kFName );
		String encoding = getEncodingOrNull();
		if( null!=encoding )
		{
			if(debug) debugMsg( kFName, "Got explicit encoding from getEncodingOrNull()" );
			return encoding;
		}
		if(debug) debugMsg( kFName, "No explicit encoding, will guess from Content-Type" );
		String contentType = getContentTypeWithDefault();
		if(debug) debugMsg( kFName, "Explicit contentType = '" + contentType + "'" );

		if( null==contentType )
		{
			contentType = guessContentTypeFromNameOrNull( inBaseName, optRelativeRoot );
			if(debug) debugMsg( kFName, "Forced to also guess contentType = '" + contentType + "'" );
		}
		encoding = getDefaultEncodingForMimeType( contentType );
		if(debug) debugMsg( kFName, "Final answer encoding = '" + encoding + "' for contentType = '" + contentType + "'" );
		return encoding;
	}
	public static String staticCalculateEncoding(
			String inBaseName,
			String optRelativeRoot
	) {
		String contentType = guessContentTypeFromNameOrNull( inBaseName, optRelativeRoot );
		return getDefaultEncodingForMimeType( contentType );
	}

	public static String guessContentTypeFromNameOrNull( String inBaseName, String optRelativeRoot )
	{
		final String kFName = "guessContentTypeFromNameOrNull";
		debugMsg( kFName, "Resorting to guessing content type / mime type for '" + inBaseName + "' / '" + optRelativeRoot + "'" );
		if( null==inBaseName )
			return null;
		String tmpName = inBaseName.toLowerCase();
		// Will also pickup .html, also assume asp and aspx
		if( tmpName.indexOf(".htm") >= 0 || tmpName.indexOf(".asp") >= 0 )
			return MIME_TYPE_HTML;
		// xml, xsl and xslt
		if( tmpName.indexOf(".xml") >= 0 || tmpName.indexOf(".xsl") >= 0 )
			return MIME_TYPE_HTML;
		// text, css and js
		if( tmpName.indexOf(".txt") >= 0 || tmpName.indexOf(".css") >= 0 || tmpName.indexOf(".js") >= 0 )
			return MIME_TYPE_HTML;
		// TODO: come up with other guesses, or see what other Java guys do
		// TODO: couuld default to HTML if it looks like a URL... not sure if that's a good idea...
		// TODO: There's also a bunch we would know as binary..., not sure what to do about that... doc pdf gif jpg etc
		// We have no strong evidence for any other guess at this time
		return null;
	}

	public static String getDefaultEncodingForMimeType( String contentType )
	{
		if( null==contentType )
			return DEFAULT_CHAR_ENCODING;
		else if( MIME_TYPE_HTML.equalsIgnoreCase(contentType) )
			return DEFAULT_CHAR_ENCODING_HTML;
		else if( MIME_TYPE_XML.equalsIgnoreCase(contentType) )
			return DEFAULT_CHAR_ENCODING_XML;
		else if( MIME_TYPE_TEXT.equalsIgnoreCase(contentType) )
			return DEFAULT_CHAR_ENCODING_TEXT;
		else
			return DEFAULT_CHAR_ENCODING;
	}
	
	public boolean getIsBinary() {
		return ! getContentTypeWithDefault().toLowerCase().startsWith( "text/" );
	}

	public String getDesiredRedirectURL()
	{
		// If it's already set, return it
		if( fRedirectURL != null )
			return fRedirectURL;
		// Else if we have headers, look in there
		String outURL = null;
		if( fIoHTTPHeaderFieldHash != null )
		{
			outURL = getScalarHTTPFieldTrimOrNull( "Location" );
		}
		return outURL;
	}
	public void setDesiredRedirectURL( String inURL )
	{
		fRedirectURL = inURL;
		// Todo: do we need to do this?  Will be set by default if no other
		// code is set
		// setHTTPResponseCode( DEFAULT_HTTP_REDIRECT_CODE );
	}


	public boolean isCGIFieldsCaseSensitive()
	{
		return fIoIsCGIFieldsCasen;
	}
	public void setCGIFieldsCaseSensitive( boolean inFlag )
	{
		fIoIsCGIFieldsCasen = inFlag;
	}

	public String getReferer()
	{
		// If it's already set, return it
		if( fIoRefererURL != null )
			return fIoRefererURL;
		// Else if we have headers, look in there
		String outURL = null;
		if( fIoHTTPHeaderFieldHash != null )
		{
			outURL = getScalarHTTPFieldTrimOrNull( "Referer" );
		}
		return outURL;
	}
	public AuxIOInfo getCompleteRefererInfo()
	{
		// If it's already set, return it
		if( fIoRefererInfo != null )
			return fIoRefererInfo;

		// Get the string, if it's there
		String refURL = getReferer();

		// If we don't have one, return null
		// No need for warning, since this is rather common
		if( refURL == null )
			return null;

		// Intialize a new record
		fIoRefererInfo = new AuxIOInfo();

		// See if there's a question mark
		int questionMarkAt = refURL.indexOf( '?' );
		if( questionMarkAt >= 0
			&& questionMarkAt < refURL.length()-1
			)
		{
			// Grab it
			String encodedBuffer = refURL.substring( questionMarkAt+1 );
			// And now decode it

			// parse and store the variables
			NIEUtil.cgiDecodeVarsBuffer( fIoRefererInfo, encodedBuffer );
		}

		// Return what we got
		return fIoRefererInfo;

	}

	public String getClientName()
	{
		// If it's already set, return it
		if( fIoHTTPUserAgentString != null )
			return fIoHTTPUserAgentString;
		// Else if we have headers, look in there
		String tmpStr = null;
		if( fIoHTTPHeaderFieldHash != null )
		{
			tmpStr = getScalarHTTPFieldTrimOrNull( HTTP_USER_AGENT_FIELD_SPELLING );
		}
		return tmpStr;
	}

	public String getContent()
	{
		return fIoContent;
	}
	public void setContent( String inContent )
	{
		fIoContent = inContent;
	}
	public byte [] getBinContent()
	{
		return fIoBinContent;
	}
	public void setBinContent( byte [] inContent )
	{
		fIoBinContent = inContent;
	}

	// Return the content length as recorded in the http header HASH
	// We do NOT look at the raw lines
	// Return -1 if don't have one
	public int getExpectedContentLength()
	{
		final String kFName = "getExpectedContentLength";

		// Just return -1 if no header fields
		if( fIoHTTPHeaderFieldHash == null )
			return -1;

		// Look it up
		String tmpStr = getScalarHTTPFieldTrimOrNull(
			"Content-Length"
			);

		// If not found, return -1
		if( tmpStr == null )
			return -1;

		// Now convert it
		int outInt = NIEUtil.stringToIntOrDefaultValue( tmpStr, -1 );

		// A sanity check warning for malformed header strings
		// Missing is one thing, but present and can't convert is
		// bad
		if( outInt < 0 )
			errorMsg( kFName,
				"Could not convert string to valid content length"
				+ " from http header fields."
				+ " String=\"" + tmpStr + "\""
				+ " Returning -1."
				);

		// Return the answer
		return outInt;
	}
//		public void setContentLength()
//		{
//          setOrOverwriteHTTPHeaderField
//		}
	public int getActualContentLength()
	{
		String tmpStr = getContent();
		if( tmpStr == null )
			return 0;
		else
			return tmpStr.length();
	}



	public boolean getWasURL()
	{
		return fOutWasAURL;
	}
	public void setWasURL( boolean inFlag )
	{
		fOutWasAURL = inFlag;
		if( inFlag )
		{
			setWasFile( false );
			setWasSystemResource( false );
		}
	}

	public boolean getWasFile()
	{
		return fOutWasAFile;
	}
	public void setWasFile( boolean inFlag )
	{
		fOutWasAFile = inFlag;
		if( inFlag )
		{
			setWasURL( false );
			setWasSystemResource( false );
		}
	}

	public boolean getWasSystemResource()
	{
		return fOutWasASystemResource;
	}
	public void setWasSystemResource( boolean inFlag )
	{
		fOutWasASystemResource = inFlag;
		if( inFlag )
		{
			setWasFile( false );
			setWasURL( false );
		}
	}

	public String getFinalURI()
	{
		return fOutFinalURI;
	}


	public void setFinalURI( String inURI )
	{
		fOutFinalURI = NIEUtil.trimmedStringOrNull( inURI );
	}


	public String getSystemRelativeBaseClassName()
	{
		return fIoBaseClassName;
	}


	public void setSystemRelativeBaseClass(
		Class inBaseClass
		)
	{
		setSystemRelativeBaseClassName( inBaseClass.getName() );
	}

	public void setSystemRelativeBaseClassName(
		String inBaseClassName
		)
	{
		final String kFName = "setSystemRelativeBaseClassName";
		inBaseClassName = NIEUtil.trimmedStringOrNull(
			inBaseClassName
			);
		if( null==inBaseClassName )
			errorMsg( kFName,
				"Null/empty class name given, this will CLEAR any"
				+ " previously set value."
				);
		fIoBaseClassName = inBaseClassName;
	}


	public Map getCgiVarsHash() {
		return fIoCGIFieldHash;
	}

	private static void ___Sep__Passwords__() {}
	////////////////////////////////////////////////////////////////////

	public int getAccessLevel() {
		return fIoAccessLevel;
	}


	public void setAccessLevel( int inNewLevel ) {
		fIoAccessLevel = inNewLevel;
	}


	// Was used is ONE class that needed it, XMLDefinedScreen
	// but changed that to use getAccessKey()
	/*public*/ private String getAccessPassword() {
		return fIoAccessPassword;
	}
	public String getAccessKey() {
		return fIoAccessKey;
	}


	public void setAccessPassword( String inNewPassword ) {
		final String kFName = "setAccessPassword";
		inNewPassword = NIEUtil.trimmedStringOrNull( inNewPassword );
		if( null==inNewPassword )
			warningMsg( kFName, "Was passed in a null/empty new password; any existing password will be cleared." );
		fIoAccessPassword = inNewPassword;
	}
	public void setAccessKey( String inNewKey ) {
		final String kFName = "setAccessKey";
		inNewKey = NIEUtil.trimmedStringOrNull( inNewKey );
		if( null==inNewKey )
			warningMsg( kFName, "Was passed in a null/empty new key; any existing key will be cleared." );
		fIoAccessKey = inNewKey;
	}



	private static void ___Issues_with_Suspicious_Odd_HTTP_Connections___ (){}
	// Some web sites respond with errors with NO http headers
	// they just start blasting out HTML, Inquira server at Fidelity

	public boolean getSawSuspiciousHttpResponse()
	{
		if( fInIgnoreSuspiciousHttpResponse )
			return false;
		return fInOutSawSuspiciousHttpResponse
			|| fInAssumeSuspiciousHttpResponse
			;
	}
	public String getSuspiciousHttpReason()
	{
		if( ! getSawSuspiciousHttpResponse() ) {
			return "Not suspicious: "
				+ "sawSuspiciousHttpResponse=" + fInOutSawSuspiciousHttpResponse
				+ ", told to ignoreSuspiciousHttpResponse=" + fInIgnoreSuspiciousHttpResponse
				+ ", told to assumeSuspiciousHttpResponse=" + fInAssumeSuspiciousHttpResponse
				+ " (aand ignore trumps assume)"
				;
		}
		if( null!=fInOutSuspiciousHttpReason )
			return fInOutSuspiciousHttpReason;
		if( fInAssumeSuspiciousHttpResponse )
			return "Flagged as suspicious - no reason given";
		if( fInAssumeSuspiciousHttpResponse )
			return "Set to assume suspicion";
		return "Unknown reason";
	}

	public void setSawSuspiciousHttpResponse()
	{
		setSawSuspiciousHttpResponse( true, null );
	}
	public void setSawSuspiciousHttpResponse( String optMessage )
	{
		setSawSuspiciousHttpResponse( true, optMessage );	
	}
	public void setSawSuspiciousHttpResponse( boolean inFlag )
	{
		setSawSuspiciousHttpResponse( inFlag, null );
	}
	public void setSawSuspiciousHttpResponse( boolean inFlag, String optMessage )
	{
		final String kFName = "setSawSuspiciousHttpResponse";
		fInOutSawSuspiciousHttpResponse = inFlag;
		// Carefully store the message, if there is one
		if( null != optMessage )
		{
			if( inFlag )
			{
				if( null != fInOutSuspiciousHttpReason ) {
					fInOutSuspiciousHttpReason += ", AND " + optMessage;
				}
				else {
					fInOutSuspiciousHttpReason = optMessage;
				}
			}
			else {
				warningMsg( kFName, "Can't set reason when clearning flag, nulling message" );
				fInOutSuspiciousHttpReason = null;
			}
		}
		else {
			fInOutSuspiciousHttpReason = null;
		}
	}
	public void setAssumeSuspiciousHttpResponse()
	{
		setAssumeSuspiciousHttpResponse( true );
	}
	public void setAssumeSuspiciousHttpResponse( boolean inFlag )
	{
		fInAssumeSuspiciousHttpResponse = true;
	}
	public void setIgnoreSuspiciousHttpResponse()
	{
		setIgnoreSuspiciousHttpResponse( true );
	}
	public void setIgnoreSuspiciousHttpResponse( boolean inFlag )
	{
		final String kFName = "setIgnoreSuspiciousHttpResponse";
		if( inFlag && fInPromoteSuspiciousHttpToException )
		{
			debugMsg( kFName, "Now told to ignore suspicious HTTP headers, so clearing previously set promoteSuspiciousToException" );
			fInPromoteSuspiciousHttpToException = false;
		}
		fInIgnoreSuspiciousHttpResponse = true;
	}
	public boolean getIgnoreSuspiciousHttpResponse()
	{
		return fInIgnoreSuspiciousHttpResponse;
	}
	public void setPromoteSuspiciousHttpToException()
	{
		setPromoteSuspiciousHttpToException( true );
	}
	public void setPromoteSuspiciousHttpToException( boolean inFlag )
	{
		final String kFName = "setPromoteSuspiciousHttpToException";
		if( inFlag && fInIgnoreSuspiciousHttpResponse )
		{
			debugMsg( kFName, "Now being told to promote suspicious HTTP headers to Exception, so clearing previously set ignoreSuspicious" );
			fInIgnoreSuspiciousHttpResponse = false;
		}
		if( inFlag && fInAssumeSuspiciousHttpResponse )
		{
			warningMsg( kFName, "Now being told to promote suspicious HTTP headers to Exception, and was previously told assumeSuspiciousHttp, so exceptions are extremely likely unless the assume flag is cleared.");
		}
		fInPromoteSuspiciousHttpToException = true;
	}
	public boolean getPromoteSuspiciousHttpToException()
	{
		return fInPromoteSuspiciousHttpToException;
	}

	// ============================================================
	private static final void ___Redirects___(){}
	private static final void ___REDIRECTS_WARNING___()
	{
		// July 2008
		// These redirect policies are currently only used
		// by the advanced methods in NIEUtil.openURLReadBin_WithManualRedirects()
		// and are ignored by others!
	}

	// Usually called by a method that is doing it's own manual redirect handling
	public void recordRedirect( String inFronUrl, String inToUrl,
			int inCode, boolean inWillFollow, String optComment
	) {
		final String kFName = "recordRedirect";
		if( null==mIoRedirectHistory )
		{
			mIoRedirectHistory = new Vector();
			if( ! getShouldLogRedirects() )
				warningMsg( kFName,
					"Had requested to not recieve redirect log requests, but getting them anyway."
					+ " Will record it, and not issue this warning again for this auxio instance."
					);
		}
		mIoRedirectHistory.add( new AuxIORedirEntry(inFronUrl,inToUrl,inCode,inWillFollow,optComment) );
		if( getMaxDesiredRedirectLevels()>0
			&& mIoRedirectHistory.size() == getMaxDesiredRedirectLevels()+1
		) {
			warningMsg( kFName,
					"Have just exceeded desired number of redirects."
					+ " Will not issue this warning again for this auxio instance."
					);		
		}
	}


	public List getRedirectsSoFarOrNull()
	{
		return mIoRedirectHistory;
	}


	public int getNumRedirectsSoFar()
	{
		if( null==getRedirectsSoFarOrNull() )
			return 0;
		else
			return getRedirectsSoFarOrNull().size();
	}


	public String getRedirectsSoFarAsStringOrNull()
	{
		if( getNumRedirectsSoFar() < 1 )
			return null;
		StringBuffer buff = new StringBuffer();
		buff.append( "Showing " + getNumRedirectsSoFar() + " redirect(s):" );
		int i = 0;
		Iterator it = getRedirectsSoFarOrNull().iterator();
		while( it.hasNext() )
		{
			if( i>0 )
				buff.append( NIEUtil.NL );
			AuxIORedirEntry entry = (AuxIORedirEntry) it.next();
			buff.append( ++i ).append( ' ' );
			buff.append( entry.toString() );
		}
		return new String( buff );
	}


	public void setMaxDesiredRedirectLevels( int inMax )
	{
		mMaxDesiredRedirectLevels = inMax;
	}


	public int getMaxDesiredRedirectLevels()
	{
		return mMaxDesiredRedirectLevels;
	}


	public void setFollowRedirects()
	{
		setFollowRedirects( true );
	}
	public void setFollowRedirects( boolean inFlag )
	{
		mFollowRedirects = inFlag;
	}
	public boolean getFollowRedirects()
	{
		return mFollowRedirects;
	}


	public void setShouldLogRedirects()
	{
		setShouldLogRedirects( true );
	}
	public void setShouldLogRedirects( boolean inFlag )
	{
		mLogRedirects = inFlag;
	}
	public boolean getShouldLogRedirects()
	{
		return mLogRedirects;
	}

	
	public void setIsGoogleOneBoxSnippetRequest()
	{
		setIsGoogleOneBoxSnippetRequest( true );
	}
	public void setIsGoogleOneBoxSnippetRequest( boolean inFlag )
	{
		fIsGoogleOneBoxSnippetRequest = inFlag;
	}
	public boolean getIsGoogleOneBoxSnippetRequest()
	{
		return fIsGoogleOneBoxSnippetRequest;
	}

	public void setUseCarefulRedirects()
	{
		setUseCarefulRedirects( true );
	}


	public void setUseCarefulRedirects( boolean inFlag )
	{
		mUseCarefulRedirects = inFlag;
	}


	public boolean getUseCarefulRedirects()
	{
		final String kFName = "getUseCarefulRedirects";
		return mUseCarefulRedirects;
		// warningMsg( kFName, "FORCING true" );
		// return true;
	}

	public void setRecordInternalIncludeAttrs()
	{
		setRecordInternalIncludeAttrs( true );
	}
	public void setRecordInternalIncludeAttrs( boolean inFlag )
	{
		mRecordInternalIncludeAttrs = inFlag;
	}
	public boolean getRecordInternalIncludeAttrs()
	{
		final String kFName = "getRecordInternalIncludeAttrs";
		return mRecordInternalIncludeAttrs;
		// warningMsg( kFName, "FORCING true" );
		// return true;
	}
	
	// ============================================================
	private static final void ___Runtine_Logging___(){}
	
	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}
	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName, inFromRoutine );
	}
	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
	}
	private static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName, inFromRoutine );
	}
	private static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// ============================================================
	private static final void ___Member_Fields_and_Constants___(){}


	private int fIoAccessLevel;
	private String fIoAccessPassword;
	private String fIoAccessKey;

	public static final char DEFAULT_MULTI_VALUE_FIELD_DELIMITER = '\t';
	public static final boolean DEFAULT_HASH_KEY_NORMALIZE_CASE = true;

	// Items related to the transaction ID
	// private String fTransPrefixStr = NIEUtil.getCompactTimestamp();
	// Long times 10 million
	// private long fTransPrefix = NIEUtil.getTimestampLong() * 10000000L;
	// multiply by smaller number, even long has it's limits
	private long fTransPrefix = NIEUtil.getTimestampLong() * 10000L;
	// private long fTransPrefix = NIEUtil.getTimestampLong() * 100L;


	// private String fTransIDStr;
	private long fTransID;
	private long fTransCounter;

	// If given a relative parent URI path, should we be forced
	// to use it, or only use it if we think it's reasonable (the default)
	// Note: Currently only implemented in the file logic, not for urls.
	public boolean inForceUseOfRoot;
	// The client name we would like to present to the remote server
	// See the default below
	private String fIoHTTPUserAgentString;
	// We may have been given a relative URI and base root
	// so let them know what we finally conjured up
	// Needs to be public for now, accessed by NIEUtil
	public String outAttemptedURI;
	// In the case of an HTTP redirect, the URL we got might not be
	// what we started with
	private String fOutFinalURI;
	// Contains all the HTTP headers we got back
	// Currently needs to be public, accessed in NIEUtil
	// TODO: this logic should be moved into this class
	public /*private*/ List fIoHTTPHeaders;

	// Newer code, using "m" vs. "f" for Member fields
	private List mIoRedirectHistory;
	private int mMaxDesiredRedirectLevels = DEFAULT_MAX_REDIRECTS;
	// I found in the range from 3 to 10 as defaults for various
	// software libraries online
	public static final int DEFAULT_MAX_REDIRECTS = 10;
	// This is a request, but not implemented here
	private boolean mFollowRedirects = true;
	private boolean mLogRedirects = DEFAULT_LOG_REDIRECTS;
	// private boolean mHaveIssuedForcedRecordedRedirectWarningAlready;
	private boolean mUseCarefulRedirects = DEFAULT_USE_CAREFUL_REDIRECTS;
	public static final boolean DEFAULT_USE_CAREFUL_REDIRECTS = false;
	public static final boolean DEFAULT_LOG_REDIRECTS = true;

	private boolean mRecordInternalIncludeAttrs = DEFAULT_RECORD_INCLUDES;
	public static final boolean DEFAULT_RECORD_INCLUDES = true;
	// See also JDOMHelper SYSTEM_ATTR_INCLUDE_LOCATION, SYSTEM_ATTR_BASE_URI and SYSTEM_ATTR_LEVEL
	
	// States for whether we were dealing with a file or URL
	private boolean fOutWasAURL;
	private boolean fOutWasAFile;
	private boolean fOutWasASystemResource;
	// TODO: Future Use
	// private boolean fOutWasSystemResource;

	// Some web sites respond with errors with NO http headers
	// they just start blasting out HTML, Inquira server at Fidelity
	private String fInOutSuspiciousHttpReason;
	private boolean fInOutSawSuspiciousHttpResponse;
	private boolean fInAssumeSuspiciousHttpResponse;
	private boolean fInIgnoreSuspiciousHttpResponse;
	private boolean fInPromoteSuspiciousHttpToException;
	
	// The IP Address
	private String fHostAddress;

	// For reading HTTP requests
	private int fIoHTTPResponseCode;
	private Hashtable fIoHTTPHeaderFieldHash;
	private List fHTTPHeaderNamesOrigCaseOrder;
	// With normalized values
	private Hashtable fIoCGIFieldHash;
	private List fCGINamesOrigCaseOrder;
	// with UN-trimmed and UN-normalized values
	private Hashtable fIoCGIFieldHashUnnormalizedValues;
	private List fCGINamesOrigCaseOrderUnnormalizedValues;
	public String fIoTransactionType;
	public String fOutRequestedPath;
	public long fOutContentLength;
	private String fIoContent;
	private byte [] fIoBinContent;
	// These are ones that are specifically set or detected
	private String fIoContentType;
	private String fIoEncoding;
	private String fIoBasicURL;
	private String fRedirectURL;

	// Information about possibly referring URL's passed via the HTTP headers
	private String fIoRefererURL;
	private AuxIOInfo fIoRefererInfo;
	private String fIoBaseClassName;

	// Special handling for Google OneBox requests
	private boolean fIsGoogleOneBoxSnippetRequest;

	// If folks ask for a system resource, they will start the path with this
	public static final String SYSTEM_RESOURCE_PREFIX = "system:";

	// Whether or not CGI fields are case sensitive
	private boolean fIoIsCGIFieldsCasen = DEFAULT_CGI_FIELDS_CASEN;
	private static final boolean DEFAULT_CGI_FIELDS_CASEN = false;

	// Some static constants
	public static final String DEFAULT_HTTP_USER_AGENT_FIELD =
	"Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0; NetCaptor 6.5.0)";
	// The exact spelling of the HTTP user agent field
	public static final String HTTP_USER_AGENT_FIELD_SPELLING =
		"User-Agent";

	// What to return
	// See http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
	public static final int DEFAULT_HTTP_RESPONSE_CODE = 200;
	public static final int DEFAULT_HTTP_REDIRECT_CODE = 302;
	// public static final int DEFAULT_HTTP_GENERAL_ERROR_CODE = 400;
	// MS won't display any 400 or 500 errors even though we send text
	// You can disable in Internet Options advanced settings, but a pain
	public static final int DEFAULT_HTTP_GENERAL_ERROR_CODE = 203;
	public static final int DEFAULT_HTTP_FATAL_ERROR_CODE = 500;

	// Some mime types
	public static final String MIME_TYPE_HTML = "text/html";
	public static final String MIME_TYPE_TEXT = "text/plain";
	public static final String MIME_TYPE_XML = "text/xml";
	public static final String DEFAULT_MIME_TYPE = MIME_TYPE_HTML;

	// As it appears in the BROWSER/IIS, but NOT case sensitive in Java
	// TODO: Not tied into xpump HTTPRetriever defaults
	public static final String CHAR_ENCODING_8859 = "iso-8859-1";
	public static final String CHAR_ENCODING_UTF8 = "utf-8";
	public static final String CHAR_ENCODING_WINDOWS = "CP-1252";

	// public static final String DEFAULT_CHAR_ENCODING_HTML = CHAR_ENCODING_8859;
	// As of March 2009 darn near every search is using UTF-8 although in theory
	// W3C had said, many years ago, that 8859 was the default
	public static final String DEFAULT_CHAR_ENCODING_HTML = CHAR_ENCODING_UTF8;

	public static final String DEFAULT_CHAR_ENCODING_TEXT = CHAR_ENCODING_8859;
	public static final String DEFAULT_CHAR_ENCODING_XML = CHAR_ENCODING_UTF8;
	// SearchTrack mostly deals with HTML...
	public static final String DEFAULT_CHAR_ENCODING = DEFAULT_CHAR_ENCODING_HTML;
	// Java standard include UTF-8 and US-ASCII (among others)
	// CP-1252 (Windows), "Latin-1"

	// End of line marker
	public static final String HEADER_EOL = "\r\n";

	// Accented lower case letter a, hex=A0, dec=160, oct=240
	public static final char _K_NBSP = '\240';
	// ^^^ moved to nieutil
	public static final char K_REPL_CHAR = ' ';


}
