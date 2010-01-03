package nie.core;

// Mini class to hold redirect history

public class AuxIORedirEntry
{
	public static final String kClassName = "AuxIORedirEntry";

	public AuxIORedirEntry( String fromUrl, String toUrl,
			int statusCode, boolean willFollow, String optComment
	) {
		final String kFName = "constructor";
		mFromUrl = fromUrl;
		mToUrl = toUrl;
		mStatusCode = statusCode;
		mWillFollow = willFollow;
		mComment = optComment;
		if( null==mFromUrl )
			warningMsg( kFName, "fromURL is null" );
		if( null==mToUrl )
			warningMsg( kFName, "toURL is null" );
		if( ! isValidCode() )
			warningMsg( kFName,
				"Invalid redirect status code: " + mStatusCode
				+ " Should be " + PERMANENT + "(=" + PERMANENT_NAME + ")"
				+ " or " + TEMPORARY + "(=" + TEMPORARY_NAME + ")"
				+ " or at least >= 300 and < 400 (=" + NONSTANDARD_300_RANGE_NAME + ")"
				);
	}
	public String toString()
	{
		final String kFName = "toString";
		// statusMsg( kFName, "mStatusCode="+mStatusCode + " PERMANENT="+PERMANENT + " TEMPORARY="+TEMPORARY + " mWillFollow="+mWillFollow	);
		return
			( ! getWillFollow() ? "REJECTED " : "" )
			+ "Redirect "
			+ getStatusCodeAsText()
			+ ' ' + getFromUrl()
			+ " to " + getToUrl()
			+ ( null!=getCommentOrNull() ? " (" + getCommentOrNull() + ')' : "" )
			;
	}
	public String getFromUrl()
	{
		return mFromUrl;
	}
	public String getToUrl()
	{
		return mToUrl;
	}
	public int getStatusCode()
	{
		return mStatusCode;
	}
	public boolean getWillFollow()
	{
		return mWillFollow;
	}
	public String getCommentOrNull()
	{
		return mComment;
	}
	public String getStatusCodeAsText()
	{
		String name = UNKNOWN_NAME;
		if( isPermanent() )
			name = PERMANENT_NAME;
		else if( isTemporary() )
			name = TEMPORARY_NAME;
		else if( isNonStandard300() )
			name = NONSTANDARD_300_RANGE_NAME;
		return "" + mStatusCode + '=' + name;
	}

	public boolean isPermanent()
	{
		return PERMANENT == mStatusCode;
	}
	public boolean isTemporary()
	{
		return TEMPORARY == mStatusCode;
	}
	public boolean isNonStandard300()
	{
		return PERMANENT != mStatusCode
			&& TEMPORARY != mStatusCode
			&& mStatusCode >= 300
			&& mStatusCode < 400
			;
	}
	
	public boolean isValidCode()
	{
		return
			mStatusCode >= 300
			&& mStatusCode < 400
			;
			// PERMANENT == mStatusCode
			// || TEMPORARY == mStatusCode
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


	String mFromUrl;
	String mToUrl;
	int mStatusCode;
	boolean mWillFollow;
	String mComment;

	// Wikipedia lists quite a few:
	// http://en.wikipedia.org/wiki/URL_redirection#HTTP_status_codes_3xx
	// Java names java.net.HttpURLConnection
	// http://java.sun.com/j2se/1.4.2/docs/api/java/net/HttpURLConnection.html#field_summary
    // * 300 multiple choices (e.g. offer different languages) HTTP_MULT_CHOICE 
    // * 301 moved permanently HTTP_MOVED_PERM
    // * 302 found (e.g. temporary redirect) HTTP_MOVED_PERM
    // * 303 see other (e.g. for results of cgi-scripts) HTTP_SEE_OTHER 
    // * 304 not modified HTTP_NOT_MODIFIED 
    // * 305 use proxy HTTP_USE_PROXY 
    // * 307 temporary redirect (not listed in Java)

	public static final int PERMANENT = java.net.HttpURLConnection.HTTP_MOVED_PERM;
	public static final String PERMANENT_NAME = "permanent";
	public static final int TEMPORARY = java.net.HttpURLConnection.HTTP_MOVED_TEMP;
	public static final String TEMPORARY_NAME = "temporary";
	public static final String NONSTANDARD_300_RANGE_NAME = "other_300_range";
	public static final String UNKNOWN_NAME = "unknown";
}
