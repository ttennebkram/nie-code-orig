package nie.sn;

import java.net.*;
import java.util.*;
import java.io.*;

// import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Comment;

import nie.core.*;
import nie.lucene.LuceneException;
import nie.lucene.LuceneRequestDispatcher;
import nie.sr2.ReportConfigException;
import nie.sr2.ReportException;
import nie.sn.LoggingLink;


// import nie.sn.SnRedirectRecord;
// import NIEUtil.AuxIOInfo;

public class SnRequestHandler implements Runnable
{

	// Useful for logging functions
	private static final String kClassName = "SnRequestHandler";

	// private static boolean debug = true;
	// Don't specifically set it if you want FALSE, it'll default to that
//	private static boolean debug;
//	public static void setDebug( boolean flag )
//	{
//		debug = flag;
//	}

	///////
	//
	// Constructors
	//
	///////

	// Request handling does need access YES TO THE MAIN APP
	// unlike most of the other classes that only need the config
	public SnRequestHandler(
		Socket inHandlerSocket,
		SearchTuningApp inApp,
		SearchTuningConfig inConfig,
		long inSocketAcceptTime
		)
//		Hashtable inSearchNamesHashMap,
//		SearchEngineConfig inSearchEngine
//		)
	{

		final String kFName = "constructor";
		
		long consStart = System.currentTimeMillis();
		mTiming = new TimingVector( "socket", inSocketAcceptTime );
		mTiming.addEvent( "SnReq.cstr.start", consStart );
		

		fHandlerSocket = inHandlerSocket;

		if( inApp == null )
		{
			fatalErrorMsg( kFName,
				"No main application passed in to constructor, it was null."
				+ " Can not continue."
				);
			System.exit(2);
		}
		if( inConfig == null )
		{
//			fatalErrorMsg( kFName,
//				"No main application configuration passed in to constructor, it was null."
//				+ " Can not continue."
//				);
//			System.exit(2);
			errorMsg( kFName,
				"No main application configuration passed in to constructor, it was null."
				+ " Will attempt to at least pass-through searches to host search engine."
				);
		}

		// The main application
		fMainApp = inApp;
		fMainConfig = inConfig;

		// Setup stuff from the main app
		// moved to gets
//		fHashMap = inSearchNamesHashMap;
//		fEngine = inSearchEngine;

		// Enable logging
		// setLogMessageOutput( System.err );

//		fHeaders = new HashMap();
//		fReadCR = false;
	}


	private static void ___High_Level_Logic___() {}
	////////////////////////////////////////////////////////////

	///////
	//
	// This is where the handler starts running as well as fulfilling the "Runnable" promise
	//
	///////

	public void run()
	{
		final String kFName = "run";

		mTiming.addEvent( "SnReq.run.start" );

		debugMsg( kFName, "Starting." );

		// Initialize our respoonse
		// We may want to setup errors here so do this right away
		fResponseInfo = new AuxIOInfo();
		// consistent handling of fields
		if( null!=getSearchEngine() )
			fResponseInfo.setCGIFieldsCaseSensitive( getSearchEngine().isCGIFeildsCaseSensitive() );

		// Read the request
		// This sets fRequestInfo
		try {
			debugMsg( kFName, "Will call readCompleteRequest" );
			readCompleteRequest();
			// TODO: some bogus requests never seem to come back, neither PATH 1 nor PATH 2
			debugMsg( kFName, "PATH 1: Back from call to readCompleteRequest" );
		}
		// Don't waste time on bogus requests
		catch( SpuriousHTTPRequestException z ) {
			debugMsg( kFName, "PATH 2: Back from call to readCompleteRequest" );
			infoMsg( kFName, "Ingoring zero byte request." );
			try {
				if( null!=fHandlerSocket )
					fHandlerSocket.close();
				fHandlerSocket = null;
			}
			catch( IOException e ) {
				debugMsg( kFName, "Caught exception closing zero byte request socket:" + e );
			}
			return;
		}
		debugMsg( kFName, "Past read request block." );

		// Init some items, set to "unknown" state
		fSNActionCode = SearchTuningConfig.SN_ACTION_CODE_UNDEFINED;
		fSNStatusCode = SearchTuningConfig.SN_STATUS_UNDEFINED;
		fSNStatusMsg = null;
		fSNActionCount = -1;
		fSNActionItemCount = -1;

		// Sanity check, did we get anything?
		if( null == fRequestInfo )
		{
			errorMsg( kFName,
				"Got back NULL request from readCompleteRequest."
				+ " Will produce error page for this reuqest and exit this thread."
				);
			setupTextErrorResponse( "Unable to properly read request."
				+ " Please contact the Systems Administrator or Webmaster."
				+ " Administrators: please check SearchTrack log file for details."
				);
		}
		// Else we were able to read the request
		else
		{
			// Actually handle the processing
			debugMsg( kFName,
				"Doing the processing."
				);

			try {
				doProcessing();
				// Or close input stream here???
			}
			// Don't waste time on bogus requests
			catch( SpuriousHTTPRequestException z ) {
				infoMsg( kFName, "Ingoring invalid CGI request: " + z );
				try {
					if( null!=fHandlerSocket )
						fHandlerSocket.close();
					fHandlerSocket = null;
				}
				catch( IOException e ) {
					debugMsg( kFName, "Caught exception closing CGI request socket:" + e );
				}
				return;
			}
		}

		// Send back the request
		debugMsg( kFName, "Transmitting a response." );
		mTiming.addEvent( "SnReq.run.response" );

		String errMsg = null;
		try {
			transmitResponse();
		}
		catch( UnsupportedEncodingException enc1 )
		{
			errorMsg( kFName,
				"Encoding Exception while sending primary response."
				+ " IF this was a proxy search, will try failover user redirect to host search engine."
				+ " mContextCode='" + mContextCode + "'"
				+ " Exception=\"" + enc1 + "\""
				);				

			// If a regular proxy seach failed, we should still try
			// send them a redirect to the host engine
			if( null!=mContextCode && mContextCode.equals(SN_CONTEXT_PROXY) )
			{
				// Setup to send them to the search engine via redir
				if( null!=fResponseInfo )
				{
					fResponseInfo.setContent( null );
					fResponseInfo.setBinContent( null );
				}
				setupPassthroughRedirect();
				// Now try sending that
				try {
					transmitResponse();
				}
				catch( UnsupportedEncodingException enc2 )
				{
					errorMsg( kFName,
						"Encoding Exception while sending recovery-redirect response."
						+ " Exception=\"" + enc2 + "\""
						);				
				}
				catch( IOException eIO )
				{
					errorMsg( kFName,
							"IO Exception while sending recovery-redirect response."
							+ " Exception=\"" + eIO + "\""
							);
					
				}
			}

			// Not much else to do
			
			// setupTextErrorResponse( msg );
			// ^^^ We could try to setup an error message
			// response at this time...
			// But if it's IO, then they wouldn't see it
			// And if the redir has encoding problems then that is
			// just the headers, so unlikely anything will work
			
		}
		catch( IOException eIO )
		{
			errorMsg( kFName,
				"IO Exception while sending initial response."
				+ " Exception=\"" + eIO + "\""
				);
			// Nothing else to do here, the client is not connected
		}
		// General exceptions
		// If proxy search, try to do something useful
		catch( Exception e )
		{
			String msg = "General Exception while sending primary response."
				+ " IF this was a proxy search, will try failover user redirect to host search engine."
				+ " mContextCode='" + mContextCode + "'"
				+ " Exception=\"" + e + "\""
				;
			stackTrace( kFName, e, msg );
			
			// If a regular proxy seach failed, we should still try
			// send them a redirect to the host engine
			if( null!=mContextCode && mContextCode.equals(SN_CONTEXT_PROXY) )
			{
				// Setup to send them to the search engine via redir
				if( null!=fResponseInfo )
				{
					fResponseInfo.setContent( null );
					fResponseInfo.setBinContent( null );
				}
				setupPassthroughRedirect();
				// Now try sending that
				try {
					transmitResponse();
				}
				catch( UnsupportedEncodingException enc2 )
				{
					errorMsg( kFName,
						"Encoding Exception while sending recovery-redirect response."
						+ " Exception=\"" + enc2 + "\""
						);				
				}
				catch( IOException eIO )
				{
					errorMsg( kFName,
						"IO Exception while sending recovery-redirect response."
						+ " Exception=\"" + eIO + "\""
						);						
				}
				catch( Exception e2 )
				{
					String msg2 = "General Exception while sending recovery-redirect response."
						+ " Exception=\"" + e2 + "\""
						;
					stackTrace( kFName, e2, msg2 );					
				}
			}	// End if doing proxy
			
		}

		// Cleanup the socket
		try {
			if( null!=fHandlerSocket )
				fHandlerSocket.close();
			fHandlerSocket = null;
		}
		catch( IOException e ) {
			warningMsg( kFName, "Caught exception closing request socket:" + e );
		}

		mTiming.addEvent( "SnReq.run.postLogging" );

		// After doing a response, we may have some data to log
		// This method will worry about whether logging is actually active
		doAnyPostProcessingLogging();

		if( getShouldStopNow() )
		{
			statusMsg( kFName,
				"Have sent response and have now detected that the shutdown flag is set."
				+ " Will call stop now application method."
				);
			forceStopNow();
			debugMsg( kFName,
				"Back from call to stop now."
				);
		}
		else
		{
			debugMsg( kFName, "Have sent response, no shutdown flag detected." );
		}



//		// Shut down
//		try
//		{
//			fHandlerSocket.shutdownOutput();
//			fHandlerSocket.shutdownInput();
//			fHandlerSocket.close();
//		}
//		catch( IOException eIO )
//		{
//			errorMsg( kFName, "Got exception when shutting down sockets"
//				+ ", exception=\"" + eIO + "\"."
//				+ " Main server will continue to attempting to process subsequent requests."
//				);
//		}

		debugMsg( kFName, "Done!" );

		mTiming.addEvent( "SnReq.run.end" );

		boolean showTiming = getRunLogObject().shouldDoInfoMsg(
			TimingVector.REPORT_CLASS_NAME,
			TimingVector.REPORT_METHOD_NAME
			);
		if(showTiming) {
			getRunLogObject().infoMsg(
				TimingVector.REPORT_CLASS_NAME,
				TimingVector.REPORT_METHOD_NAME,
				mTiming.reportStr()
				);
		}
	}






	///////
	//
	// Actually DO something with all that data
	//
	///////

	private void doProcessing()
		throws SpuriousHTTPRequestException
	{

		final String kFName = "doProcessing";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// We need to figure out what to do and respond
		// Since there is a default context, this should not
		// be null, and they normalize it for us
		String lContextCode = determineContext();
		// We also want to know this later!
		mContextCode = lContextCode;

		// Take care of fail-safe conditions
		if( null==fMainConfig ) {
			// Force some types to a common redir
			if( lContextCode.equals( SN_CONTEXT_SEARCH_REDIR )
				|| lContextCode.equals( SN_CONTEXT_PASSTHRU_REDIR )
				|| lContextCode.equals( SN_CONTEXT_PROXY )
				)
			{
				lContextCode = SN_CONTEXT_PASSTHRU_REDIR;
			}
			else if( lContextCode.equals( SN_CONTEXT_DEBUG_REQUEST_ECHO )
				|| lContextCode.equals( SN_CONTEXT_ADMIN )
				|| lContextCode.equals( SN_CONTEXT_UI )
				|| lContextCode.equals( SN_CONTEXT_LUCENE_SEARCH )
				// TODO: maybe allow FILE context and fixed HTML
				// but they may require some elements from a real config?
				)
			{
				// do nothing, let them through
				// This will be caught below
			}
			// Punt on everything else
			else {
				throw new SpuriousHTTPRequestException( kExTag +
					"Fail-safe pass-through mode does not handle \"" + lContextCode + "\" reauests."
					);
			}

		}


		// ??? A typical search names redir search

		// Proxy contents
		if( lContextCode.equals( SN_CONTEXT_PROXY ) )
		{
			infoMsg( kFName,
				"Context: Proxy search on match (" + lContextCode + ")"
				);
			setupProxySearch( );
		}
		// Snippet suggestion
		else if( lContextCode.equals( SN_CONTEXT_SNIPPET_SUGGEST ) )
		{
			infoMsg( kFName,
				"Context: Snippet suggestions (" + lContextCode + ")"
				);
			setupWebmasterSuggestsSnippetOnlyResponse( );
		}
		// Google snippet
		else if( lContextCode.equals( SN_CONTEXT_GOOGLE_ONEBOX_SNIPPET ) )
		{
			infoMsg( kFName,
				"Context: Google OneBox callout snippet request (" + lContextCode + ")"
				);
			processGoogleOneBoxSnippetRequest( );
		}
		// Google snippet config
		else if( lContextCode.equals( SN_CONTEXT_GOOGLE_ONEBOX_CONFIG ) )
		{
			infoMsg( kFName,
				"Context: Google OneBox CONFIGURATION XML request (" + lContextCode + ")"
				);
			processGoogleOneBoxConfigRequest( );
		}
		// Lucene search
		else if( lContextCode.equals( SN_CONTEXT_LUCENE_SEARCH ) )
		{
			infoMsg( kFName,
				"Context: Lucene Search (" + lContextCode + ")"
				);
			processLuceneSearchRequest( );
		}
		// Are we doing direct logging?
		else if( lContextCode.equals( SN_CONTEXT_DIRECT_LOG_TRANSACTION ) )
		{
			infoMsg( kFName,
				"Context: Direct logging of transaction (" + lContextCode + ")"
				);
			setupDirectLogging( );
		}
		// ADMINISTRATION (or UI)
		else if( lContextCode.equals( SN_CONTEXT_ADMIN ) )
		{
			// Main Admin routine
			// Note that we STILL return here to finish up
			infoMsg( kFName,
				"Context: Administration/non-UI (" + lContextCode + ")"
				);
			doAdmin( );
		}
		// UI (usually admin)
		else if( lContextCode.equals( SN_CONTEXT_UI ) )
		{
			// Main Admin routine
			// Note that we STILL return here to finish up
			infoMsg( kFName,
				"Context: UI (" + lContextCode + ")"
				);
			processUIRequest( );
		}
		// Fixed file
		else if( lContextCode.equals( SN_CONTEXT_FILE ) )
		{
			infoMsg( kFName,
				"Context: Static file (" + lContextCode + ")"
				);
			setupStaticFileRequest( );
		}
		else if( lContextCode.equals( SN_CONTEXT_SEARCH_REDIR ) )
		{
			infoMsg( kFName,
				"Context: Search redirect (" + lContextCode + ")"
				);
			setupSearchTermToSpecificURLRedirect( );
		}
		// Or just pass it through
		else if( lContextCode.equals( SN_CONTEXT_PASSTHRU_REDIR ) )
		{
			infoMsg( kFName,
				"Context: Passthrough redirect (" + lContextCode + ")"
				);
			setupPassthroughRedirect( );
		}
		// Or echo back the request
		else if( lContextCode.equals( SN_CONTEXT_DEBUG_REQUEST_ECHO ) )
		{
			infoMsg( kFName,
				"Context: Debug - echo request (" + lContextCode + ")"
				);
			setupRequestEcho( );
		}
		// Or, for debugging, echo back some fixed HTML text
		else if( lContextCode.equals( SN_CONTEXT_DEBUG_FIXED_HTML_ECHO ) )
		{
			infoMsg( kFName,
				"Context: Fixed HTML echo (" + lContextCode + ")"
				);
			setupFixedHTMLEcho( );
		}
		// Is it another protocol that we recognize?
		else if(
			lContextCode.equals( SN_CONTEXT_LOOKUP )
			)
		{
			String msg = // "SnRequestHandler:doProcessing:" + ' ' +
				"You have requested a currently unimplemented context."
				+ " Context=\"" + lContextCode + "\""
				+ " Unable to process your request."
				;
			errorMsg( kFName, msg );
			setupTextErrorResponse( msg );
		}
		// Else we don't recognize it at all
		else
		{
			String msg = "SnRequestHandler:doProcessing:"
				+ " You have requested an unrecognized context."
				+ " Context=\"" + lContextCode + "\""
				+ " Unable to process your request."
				;
			errorMsg( kFName, msg );
			setupTextErrorResponse( msg );
		}

	}

	// When we do logging, we don't like to make the user wait for it,
	// so we send them on their way and then handle this.
	private void doAnyPostProcessingLogging()
	{
	    // xyz
		final String kFName = "doAnyPostProcessingLogging";

		if( null==getMainConfig() )
			return;

		if( ! doSearchLogging() )
		{
			debugMsg( kFName,
				"Search Logging is not active."
				);
			return;
		}

		// We also have the option to not log null searches
		// but that is handled in the logging class

		if( mDoDirectLogAferResponse )
		{
			carryOutDirectLoggingAfterResponse();
		}
		// else if( _mDoProxyLogAferResponse )
		else if( mDoSearchLoggingAferResponse )
		{
			// carryOutProxyLoggingAfterResponse();
		    // Now also includes snippet serving
			carryOutPostSearchLogging();
		}
		else    // Else we're not doing any
		{
			debugMsg( kFName,
				"Did not find flags set for either type of post logging."
				);
		}
	}

	// What we will call to indicate that we have got a shutdown command
	private void setShouldStopNow()
	{
		getMainApplication().setShouldStopNow();
	}
	private boolean getShouldStopNow()
	{
		return getMainApplication().getShouldStopNow();
	}
	private void forceStopNow()
	{
		getMainApplication().forceStopNow();
	}


	private static void ___Handle_Normal_Commands___(){}
	/////////////////////////////////////////////////////////////////

	// This specific method has to be careful as it may be called
	// even if we're running in fail-safe pass-through mode
	private String determineContext()
		throws SpuriousHTTPRequestException
	{
		final String kFName = "determineContext";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( fRequestInfo == null )
		{
			errorMsg( kFName,
				"No request to check, fRequestInfo was NULL."
				+ " Will return NULL context."
				);
			return null;
		}

		// New feature, check for /file URL prefix
		String path = fRequestInfo.getLocalURLPath();
		debugMsg( kFName, "path='" + path + "'" );
		if( null!=path && path.startsWith( FILE_CONTEXT_CGI_PATH_PREFIX ) )
			return SN_CONTEXT_FILE;

		// Allow for /ui/ or /admin/, /administration/
		if( null!=path && path.startsWith( UI_CONTEXT_CGI_PATH_PREFIX ) )
			return SN_CONTEXT_UI;

		// Allow for /login/ etc.
		if( null!=path && startsWithLoginPath( path ) )
			return SN_CONTEXT_ADMIN;

		// check for /lucene URL prefix
		if( null!=path && path.startsWith( LUCENE_SEARCH_CONTEXT_CGI_PATH_PREFIX ) )
			return SN_CONTEXT_LUCENE_SEARCH;

		// Google OneBox
		// Get the config
		if( null!=path && path.toLowerCase().startsWith( GOOGLE_ONEBOX_GET_CONFIG_CONTEXT_CGI_PATH_PREFIX ) )
			return SN_CONTEXT_GOOGLE_ONEBOX_CONFIG;
		// Get actual results
		if( null!=path && path.toLowerCase().startsWith( GOOGLE_ONEBOX_SNIPPET_CONTEXT_CGI_PATH_PREFIX ) )
		{
			if( null!=fRequestInfo )
				fRequestInfo.setIsGoogleOneBoxSnippetRequest();
			return SN_CONTEXT_GOOGLE_ONEBOX_SNIPPET;
		}		
		
		
		
		debugMsg( kFName, "path=" + path );

		// is there an indicator field we should look for?
		// Did that indicator field exist?
		// SN_CONTEXT_FIXED_REDIR
		// And where should this go?

		String outContext = fRequestInfo.getScalarCGIFieldTrimOrNull(
			NIE_CONTEXT_REQUEST_FIELD
			);
		if( null == outContext )
		{
			outContext = fRequestInfo.getScalarCGIFieldTrimOrNull(
				SN_OLD_CONTEXT_REQUEST_FIELD
				);
			if( null != outContext )
			{
				warningMsg( kFName,
					"You are still using the old, deprecated context field."
					+ " You used " + SN_OLD_CONTEXT_REQUEST_FIELD
					+ ", please change your links and HTML forms to use "
					+ NIE_CONTEXT_REQUEST_FIELD
					);
			}
			// No context at all, double check that a query field was passed
			else {
				// we can only reliably check this if we have a real config
				if( null!=fMainConfig ) {
					String queryFieldContents = fRequestInfo.getScalarCGIField_UnnormalizedValue(
						getUserQueryField()
						);
					if( null==queryFieldContents )
						throw new SpuriousHTTPRequestException( kExTag +
							"No context nor search field; ignoring spurious HTTP request."
							);
				}
				// Else we let it pass through
			}
		}

		if( outContext != null )
			outContext = outContext.toLowerCase();
		else
			outContext = getDefaultContext();

		// Return the answer
		return outContext;
	}


	private void setupStaticFileRequest()
	{

		final String kFName = "setupStaticFileRequest";

		FileDispatcher disp =
			// getMainApplication().getSearchTuningConfig().getFileDispatcher();
			getMainConfig().getFileDispatcher();

		if( null != disp )
		{
			String outBuffer = null;
			try
			{
				disp.getFile( fRequestInfo, fResponseInfo );
				// Also sets mime type and bin content in response object
			}
			// catch( ReportException re )
			catch( Exception re )
			{
				String msg = "File Error: " + re;
				errorMsg( kFName, msg );
				setupTextErrorResponse( msg );
			}
		}
		else
		{
			setupTextErrorResponse(
				"Static file module is unavalable."
				+ " Please check configuration."
				);
		}

		// And we're done
	}

	// This specific method has to be careful as it may be called
	// even if we're running in fail-safe pass-through mode
	private void setupPassthroughRedirect()
	{
		final String kFName = "setupPassthroughRedirect";
		// Call the main search engine because the redirect
		// would have taken us back to the page they searched
		// from.

		// String lNewURL = lSearchURL + "?" + lQueryParamName + lQueryTerms + lOtherParams;
//		String lNewURL = getSearchEngine().getSearchEngineURL();
		String lNewURL = getSearchEngineURL();

		// Bail if no URL
		if( null==lNewURL ) {
			errorMsg( kFName, "No search engine URL!? Reporting error to user." );
			setupTextErrorResponse( "Unable to process search; please see log files." );
			return;
		}

		// Add in all the CGI variables
		fResponseInfo.copyInCGIFields( fRequestInfo );

		// We sometimes need to add search track logging fields
		// In theory we could also add this info if proxy searching
		// fails, as it will fall back to this method, however it's
		// unlikely those search engines would know what to do with
		// all the extra fields
		// Skip this check if we're running in pass-through mode
		if( null!=fMainConfig ) {
			// if( doSearchLogging() && ! doesSearchLoggingRequireProxy() )
			if( doSearchLogging() && ! doesSearchLoggingRequireScraping() )
			{
				// This is one of two places we call this
				// The other place is if we do a proxy search
				// redirect
				addSearchTrackDirectLoggingCGIFields( fResponseInfo );
			}
		}

		// Now get all the variables as an encoded string
		String lCGIBuffer = fResponseInfo.getCGIFieldsAsEncodedBuffer();

		debugMsg( kFName, "getCGIFieldsAsEncodedBuffer = " + lCGIBuffer );

		// If we got anything back, add it on
		if( lCGIBuffer != null && lCGIBuffer.length() > 0 )
		{
			// Join the two strings together, usually with a "?"
			lNewURL = NIEUtil.concatURLAndEncodedCGIBuffer(
				lNewURL, lCGIBuffer
				);
		}

		// Set the location
		fResponseInfo.setDesiredRedirectURL( lNewURL );

		// Clear out the cgi variables hash, so there is no confusion -
		// we've already encoded them into the location field, so they
		// don't need to be "sent" in any other way
		fResponseInfo.deleteAllCGIFields();

		// Set the error code to redirect
		fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_REDIRECT_CODE );

	}

	private void setupSpecificURLRedicect( String inDestinationURL )
	{
		static_setupSpecificURLRedicect( inDestinationURL, fResponseInfo );

		/***
		fResponseInfo.setRedirectURL( inDestinationURL );
		// Be nice and also add in a message
		String content =
			"<html><head><title>Redirecting Request</title></head><body>"
			+ "<h3>Redirecting Request</h3>"
			+ "Your web request is being redirected to another URL."
			+ " Ideally this will happen automatically and you'll never see this message."
			+ "<p> However, if you do see this message, go head and click the link below;"
			+ " it will take you to the correct place."
			+ "<p> Go go "
			+ "<a href=\"" + inDestinationURL + "\">"
			+ inDestinationURL + "</a>"
			+ "</body></html>"
			;
		fResponseInfo.setContent( content );
		// Set the error code to redirect
		fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_REDIRECT_CODE );
		***/
	}

	public static void static_setupSpecificURLRedicect(
		String inDestinationURL,
		AuxIOInfo ioResponseInfo
		)
	{
		final String kFName = "static_setupSpecificURLRedicect";
		if( null==inDestinationURL ) {
			staticErrorMsg( kFName, "Null URL passed in, nothing to do." );
			return;
		}
		if( null==ioResponseInfo ) {
			staticErrorMsg( kFName, "No response object passed in to add redirect to." );
			return;
		}

		ioResponseInfo.setDesiredRedirectURL( inDestinationURL );
		// Be nice and also add in a message
		String content =
			"<html><head><title>Redirecting Request</title></head><body>"
			+ "<h3>Redirecting Request</h3>"
			+ "Your web request is being redirected to another URL."
			+ " Ideally this will happen automatically and you'll never see this message."
			+ "<p> However, if you do see this message, go head and click the link below;"
			+ " it will take you to the correct place."
			+ "<p> Go go "
			+ "<a href=\""
			+ NIEUtil.htmlEscapeString( inDestinationURL, true )
			+ "\">"
			+ NIEUtil.htmlEscapeString( inDestinationURL, true )
			+ "</a>"
			+ "</body></html>"
			;

		ioResponseInfo.setContent( content );

		// Set the error code to redirect
		ioResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_REDIRECT_CODE );
	}





	// Return whether or not it did it
	// If it DID do it, then the response object will be all set
	// unless we tell it otherwise
	private boolean setupSearchTermToSpecificURLRedirect()
	{
		return setupSearchTermToSpecificURLRedirect( true );
	}
	// return whether or not we did anything
	private boolean setupSearchTermToSpecificURLRedirect(
		boolean inDoPassthruSetupIfNoMatch
		)
	{
		final String lFName = "setupSearchRedirect";


		// Find out what the user entered
		String lQueryTerms = getUserQueryToLower();
		SnURLRecord lSnURL = getMainConfig().getValidRedirectRecord( lQueryTerms, fRequestInfo );
		String lDestinationURLStr = null;
		if( null!=lSnURL )
			lDestinationURLStr = lSnURL.getURL();

		/*** old code to find redir
		// Check for no query, or no matching query
		if( ! getHasAnyMatchingTerms() )
		{
			if( inDoPassthruSetupIfNoMatch )
				setupPassthroughRedirect();
			return false;
		}

		// Find out what the user entered
		String lQueryTerms = getUserQueryToLower();

		// Go ahead and look it up
		List redirRecords = (List)getHashMap().get(
			lQueryTerms
			);
		// And we'll loop through, looking for the first redir record
		SnURLRecord lSnURL = null;
		String lDestinationURLStr = null;
		// Loop until we find one that we like or we're out of them
		// For each redir record
		for( Iterator it = redirRecords.iterator(); it.hasNext() ; )
		{
			// Get the next one from the list
			MapRecordInterface mapRecord = (MapRecordInterface)it.next();

			// Get the list of redirect URL records
			List urlsRecords = mapRecord.getRedirectURLObjects();

			// IF this had none, try the next map
			if( urlsRecords == null || urlsRecords.size() < 1 )
				continue;

			// We are assured that, if a reidrect URL is returned, it is valid

			// Get the URL we're supposed to redirect to
			lSnURL = (SnURLRecord) urlsRecords.get(0);
			lDestinationURLStr = lSnURL.getURL();

//			// Sanity check, if we didn't find a URL, complain
//			if( lDestinationURL == null )
//			{
//				errorMsg( lFName,
//					"Found a matching search redirect record for term"
//					+ "\"" + lQueryTerms + "\""
//					+ " but there was no URL listed to redirect it to."
//					+ " Will continue looking at subsequent candidate records, if any."
//					);
//				continue;
//			}

			// OK, so we found one we like, and it has a URL
			// lSnURL = tmpRecord;
			break;

		}   // End for each matching redir record
		***/

		// At this point we either have a record that we like
		// or we don't.  It's perfectly fine if we don't.
		if( null == lDestinationURLStr )
		{
			// Go ahead and rediect to the main search engine
			if( inDoPassthruSetupIfNoMatch )
				setupPassthroughRedirect();
			return false;
		}
		// Else we did find a good URL
		else
		{

			// To be safe, check the URL that the request came from
			String lRefererURL =
				fRequestInfo.getScalarHTTPFieldTrimOrNull(
					 "HTTP-Referer"
					);

			// If they're the same, just redirecto the standard search engine
			if( lRefererURL != null &&
				lDestinationURLStr.equals(lRefererURL)
				)
			{
				if( inDoPassthruSetupIfNoMatch )
					setupPassthroughRedirect();
				return false;
			}
			// Else they are NOT the same, so go ahead and do it!
			else
			{

				// Make a note of what we did
				if( fSNActionCode == SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
					fSNActionCode = SearchTuningConfig.SN_ACTION_CODE_SEARCH_REDIR ;
				// Increment the counters
				// We need to handle if they are as-yet undefined
				fSNActionCount = fSNActionCount <= 0 ? 1 : fSNActionCount+1;
				fSNActionItemCount = fSNActionItemCount <= 0 ? 1 : fSNActionItemCount+1;

				// Setup an internal search names status if one has not been set
				if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
					fSNStatusCode = SearchTuningConfig.SN_STATUS_OK;

				// Redirect to lDestinationURL because the search terms
				// matched and it's a redirect.
				setupSpecificURLRedicect( lDestinationURLStr );
				// Some state fiddling so we don't whine too much
				fDidASearchTermToSpecificURLRedirect = true;
				return true;
			}
		}   // End else we DID find a good URL

// too many varaibles out of scope at this point
// Todo: revisit
//		// Maybe log a debug message
//		String logMsg =
//			"Query Terms = '" + lQueryTerms + "'"
//			+ "Query Terms URL Encoded = '" + urlEncode( lQueryTerms ) + "'
//			+ "Search Submission URL = '" + lSearchURL + "'"
//			+ "Search Parameter name = '" + lQueryParamName + "'"
//			+ "Suggestion Param Name = '" + lSuggestionParamName + "'"
//			+ "Other Params = '" + lOtherParams + "'"
//			;
//		debugMsg( lFName, logMsg );

	}


	// This is for when we JUST want the snippet
	private void _setupWebmasterSuggestsSnippetOnlyResponse_v1()
	{
		final String kFName = "_setupWebmasterSuggestsSnippet_v1";

		// The overall wrappers for the message
		//final String nl = "\r\n";
		final String kPageHeader = ""; // "<html><body>" + NL ;
		final String kPageFooter = ""; // "</body></html>" + NL ;


		// Even if we find nothing, we will always emit HTML
		fResponseInfo.setContentType(
			AuxIOInfo.MIME_TYPE_HTML
			);

		// The results
		StringBuffer buff = new StringBuffer();
		buff.append( kPageHeader );

		// calling this always marks for post processing
		// And the true tells it this was just a snippet served up (no parsing)
		markPostSearchLogging( true );

		// First off, see if there's no matching terms
		if( ! getHasAnyMatchingTerms() )
		{
		    debugMsg( kFName, "getHasAnyMatchingTerms() was false" );
			buff.append( "<!-- Info: NIE SearchTrack: No Suggestions -->" );
		}
		// Else we do have a shot at it
		else
		{
		    debugMsg( kFName, "getHasAnyMatchingTerms() was true" );

			// Get the snippet
			String snippet = null;
			if( getMainConfig().getLicIsAllGood() ) {
				snippet = _generateSnippet_v1();
				debugMsg( kFName, "calling generateSnippet()" );
			}
			else
				warningMsg( kFName, "N"
					+ "ot pro" + "vid" + "i" + "ng " + 's' + "u" + "gges" + "tio" + "ns; L"
					+ "IC" + "ENS"
					+ 'E' + ' '
					+ 'E' + 'X' + "PI" + 'R' + "ED" + '!'
					+ '!' + '!'
					+ ' ' + "con" + "tact su"
					+ "por" + "t@id"
					+ "eaen"
					+ "g." + 'c' + "om"
					);
			// Carefully check the results
			// If we didn't get anything, just give a quick notice
			if( snippet == null || ! fSnippetToSend )
			{
				buff.append( "<!-- Info: NIE SearchTrack: No Suggestions (empty) -->" );
				debugMsg( kFName, "NULL or no fSnippet Snippet" );
				debugMsg( kFName, "snippet" + ( null==snippet ? " is null" : " has "+snippet.length()+" chars (1)") );
				debugMsg( kFName, "fSnippetToSend="+fSnippetToSend );
			}
			else
			{
				// Go ahead and add it
				buff.append( snippet );
				debugMsg( kFName, "got snippet with " + snippet.length() + " chars (2)" );
			}

		}   // End else we did have a shot, we did have at least some matches

		// And cleanup
		buff.append( kPageFooter );

		// save the buffer in Contents
		fResponseInfo.setContent( new String(buff) );

		// Make a note that we did have success
		fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE );

	}

	// This is for when we JUST want the snippet
	private void setupWebmasterSuggestsSnippetOnlyResponse()
	{
		final String kFName = "setupWebmasterSuggestsSnippet";

		// The overall wrappers for the message
		//final String nl = "\r\n";
		final String kPageHeader = ""; // "<html><body>" + NL ;
		final String kPageFooter = ""; // "</body></html>" + NL ;

		// Even if we find nothing, we will always emit HTML
		fResponseInfo.setContentType(
			AuxIOInfo.MIME_TYPE_HTML
			);

		// The results
		StringBuffer buff = new StringBuffer();
		buff.append( kPageHeader );

		// calling this always marks for post processing
		// And the true tells it this was just a snippet served up (no parsing)
		markPostSearchLogging( true );

		// First off, see if there's no matching terms
		if( ! getHasAnyMatchingTerms() )
		{
		    debugMsg( kFName, "getHasAnyMatchingTerms() was false" );
			buff.append( "<!-- Info: NIE SearchTrack: No Suggestions -->" );
		}
		// Else we do have a shot at it
		else
		{
		    debugMsg( kFName, "getHasAnyMatchingTerms() was true" );

			// Get the snippet
			/*String*/ Element snippetElem = null;
			if( getMainConfig().getLicIsAllGood() ) {
				snippetElem = generateSnippet();
				debugMsg( kFName, "calling generateSnippet()" );
			}
			else
				warningMsg( kFName, "N"
					+ "ot pro" + "vid" + "i" + "ng " + 's' + "u" + "gges" + "tio" + "ns; L"
					+ "IC" + "ENS"
					+ 'E' + ' '
					+ 'E' + 'X' + "PI" + 'R' + "ED" + '!'
					+ '!' + '!'
					+ ' ' + "con" + "tact su"
					+ "por" + "t@id"
					+ "eaen"
					+ "g." + 'c' + "om"
					);
			// Carefully check the results
			// If we didn't get anything, just give a quick notice
			String snippetStr = null;
			if( null!=snippetElem )
				snippetStr = JDOMHelper.JDOMToString( snippetElem, true );

			if( snippetStr == null || ! fSnippetToSend )
			{
				buff.append( "<!-- Info: NIE SearchTrack: No Suggestions (empty) -->" );
				debugMsg( kFName, "NULL or no fSnippet Snippet" );
				debugMsg( kFName, "snippet" + ( null==snippetStr ? " is null" : " has "+snippetStr.length()+" chars (1)") );
				debugMsg( kFName, "fSnippetToSend="+fSnippetToSend );
			}
			else
			{
				// Go ahead and add it
				// buff.append( snippet );
				buff.append( snippetStr );
				debugMsg( kFName, "got snippet with " + snippetStr.length() + " chars (2)" );
			}

		}   // End else we did have a shot, we did have at least some matches

		// And cleanup
		buff.append( kPageFooter );

		// save the buffer in Contents
		fResponseInfo.setContent( new String(buff) );

		// Make a note that we did have success
		fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE );

	}

	public String _getGoogleOneBoxDefinitionUrl()
	{
		return getSearchNamesURL() + GOOGLE_ONEBOX_GET_CONFIG_CONTEXT_CGI_PATH_PREFIX;
	}
	public String getGoogleOneBoxSnippetUrl()
	{
		final String kFName = "getGoogleOneBoxSnippetUrl";
		// return getSearchNamesURL() + GOOGLE_ONEBOX_SNIPPET_CONTEXT_CGI_PATH_PREFIX;
		URL outUrl = null;
		try {
			outUrl = new URL( new URL(getSearchNamesURL()), GOOGLE_ONEBOX_SNIPPET_CONTEXT_CGI_PATH_PREFIX );
		}
		catch( MalformedURLException e ) {
			errorMsg( kFName, "Error combining baseUrl + path"
			 + ". base='" + getSearchNamesURL()
			 + "', path='" + GOOGLE_ONEBOX_SNIPPET_CONTEXT_CGI_PATH_PREFIX
			 + "'. Error: " + e
			 );
		}
		return outUrl.toExternalForm();
	}
	private void processGoogleOneBoxConfigRequest()
	{
		final String kFName = "processGoogleOneBoxConfigRequest";
		final String kExTag = kClassName + '.' + kFName + ": ";
		fResponseInfo.setContentType(
				AuxIOInfo.MIME_TYPE_XML
				);
		JDOMHelper outElem = null;
		try {
			debugMsg( kFName, "Will open XML '" + GOOGLE_ONEBOX_DEFINITION_SKELETON_URI + "'" );
			// We call the version that is recursive and understands system:
			// and will set it relative to this class
			// We also need JDOMHelper to NOT record the _attributes about the location inclusions
			outElem = new JDOMHelper( this.getClass(), GOOGLE_ONEBOX_DEFINITION_SKELETON_URI, false );
		}
		catch( /*JDOMHelper*/ Exception e ) {
			String msg = "Got exception from JDOMHelper: " + e;
			errorMsg( kFName, msg );
			setupTextErrorResponse( kExTag + msg );
			return;
		}
		debugMsg( kFName,
			"Will attempt to set '" + GOOGLE_ONEBOX_DEFINITION_ST_URL_PATH
			+ "' to '" + getGoogleOneBoxSnippetUrl() + "'"
			);
		// outElem.addSimpleTextToNewPath( GOOGLE_ONEBOX_DEFINITION_ST_URL_PATH, getGoogleOneBoxSnippetUrl() ); // NOT getGoogleOneBoxDefinitionUrl() );
		outElem.setTextByPath( GOOGLE_ONEBOX_DEFINITION_ST_URL_PATH, getGoogleOneBoxSnippetUrl() ); // NOT getGoogleOneBoxDefinitionUrl() );
		fResponseInfo.setContent( outElem.JDOMToString(true) );
	}

	// Although this is conceptually similar to a servicing a regular snippet
	// request, we're implementing it here in the newer style where we handoff
	// to a separate class for easier modularity / maintainability / etc.
	// SEE
	// http://code.google.com/apis/searchappliance/documentation/50/oneboxguide.html
	private void processGoogleOneBoxSnippetRequest()
	{
		final String kFName = "processGoogleOneBoxSnippetRequest";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// The IP address is passed to us by Google, vs the IP
		// address of the actual Google appliance
		String oneBoxIp = fRequestInfo.getScalarCGIFieldTrimOrNull( GOOGLE_ONEBOX_CGI_CLIENT_IP_FIELD );
		debugMsg( kFName, "Google IP address: " + oneBoxIp );
		
		// Special handling of failure mode
		String errMsg = null;

		// Setup the output
		JDOMHelper outElem = null;
		try {
			debugMsg( kFName, "Will open XML '" + GOOGLE_ONEBOX_RESULTS_SKELETON_URI + "'" );
			// We call the version that is recursive and understands system:
			// and will set it relative to this class
			// We also need JDOMHelper to NOT record the _attributes about the location inclusions
			outElem = new JDOMHelper( this.getClass(), GOOGLE_ONEBOX_RESULTS_SKELETON_URI, false );
		}
		catch( /*JDOMHelper*/ Exception e ) {
			errMsg = "Got exception from JDOMHelper: " + e;
			// errorMsg( kFName, msg );
			// setupTextErrorResponse( kExTag + msg );
			// return;
		}

		// We still want to do this, even if error state
		if( null!=outElem && null!=getGoogleOneBoxSnippetUrl() ) {
			debugMsg( kFName,
					"Will attempt to set '" + GOOGLE_ONEBOX_RESULTS_ST_URL_PATH
					+ "' to '" + getGoogleOneBoxSnippetUrl() + "'"
					);
			outElem.setTextByPath( GOOGLE_ONEBOX_RESULTS_ST_URL_PATH, getGoogleOneBoxSnippetUrl() );
		}			

		if( null==getMainConfig() ) {
			errMsg = (null==errMsg ? "" : errMsg + " AND " )
				+ "Null SearchTrack Config: Google OneBox snippets are not available in fail-safe pass-through mode.";
		}
		else if( ! getMainConfig().getLicIsAllGood() ) {
			errMsg = (null==errMsg ? "" : errMsg + " AND " )
				+ "Not providing OneBox suggestions"
				+ "LICENSE EXPIRED!!!"
				+ " contact support@ideaeng.com"
				;
		}

		// Bail on all of the failurs we know about
		if( null!=errMsg )
		{
			errorMsg( kFName, errMsg );
			if( null!=outElem ) {
				fResponseInfo.setContentType(
						AuxIOInfo.MIME_TYPE_XML
						);
				// Set "/OneBoxResults/resultCode" to "lookupFailure"
				outElem.setTextByPath( GOOGLE_ONEBOX_RESULTS_CODE_PATH, GOOGLE_ONEBOX_RESULTS_ERROR_CODE );
				// Set "/OneBoxResults/Diagnostics" to errMsg
				outElem.setTextByPath( GOOGLE_ONEBOX_RESULTS_MSG_PATH, errMsg );
				
				// save the buffer in Contents
				fResponseInfo.setContent( outElem.JDOMToString(true) );
				// Make a note that we did have success
				fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE );
				errorMsg( kFName, errMsg + " - Sending XML-mode failure to client." );
			}
			else {
				setupTextErrorResponse( errMsg );
				errorMsg( kFName, errMsg + " - Sending text-mode failure to client." );
			}
			return;
		}

		fResponseInfo.setContentType(
				AuxIOInfo.MIME_TYPE_XML
				);

		// calling this always marks for post processing
		// And the true tells it this was just a snippet served up (no parsing)
		markPostSearchLogging( true );

		// TODO: Revisit the nesting of these branches, seems like it
		// could be simplified

		// First off, see if there's no matching terms
		if( getHasAnyMatchingTerms() )
		{
		    debugMsg( kFName, "getHasAnyMatchingTerms() was true" );


		
			// Even though the later routines check for the query,
			// we like to check for it now, to give better debug messages
			String query = getUserQuery();
			
			// TODO: null query handling, matches

			// If no query, just return
			// No warning needed, be we should emit a comment at least
			if( query == null )
			{
				outElem.setTextByPath( GOOGLE_ONEBOX_RESULTS_MSG_PATH, "No Search Terms" );
			}
			// Check for no query, or no matching query
			// This also checks the hash
			else if( ! getHasAnyMatchingTerms() )
			{
				outElem.setTextByPath( GOOGLE_ONEBOX_RESULTS_MSG_PATH,
					"No search terms match the SearchTrack suggestions list."
					);
			}
			else
			{

				// The list of valid, matching wm suggests records, IF any found
				// turns list of SnURLRecords
				List suggestions = getValidWebmasterSuggestsRecords();
				// And also alternative spellings
				// Returns list of strings
				List alternatives = getValidAlternateSuggestionRecords();
	
				// If there aren't any, still just do a passthrough
				// Usually there should be by this point in the code
				if( ( suggestions == null || suggestions.size() < 1 )
					&& ( alternatives == null || alternatives.size() < 1 )
					)
				{
					outElem.setTextByPath( GOOGLE_ONEBOX_RESULTS_MSG_PATH,
						"Search term(s) not mapped"
						+ " for Webmaster Suggests nor Alternatives terms suggestions."
						+ " Terms may have mapped for other uses, not supported in OneBox mode."
						+ " Terms = '" + query + "'"
						);
				}
				// OK we have at least one supported type
				else {

					// Google will only handle up to 3 suggestions
					int maxOneboxItems = 3;
					int maxUrlSuggestionns = maxOneboxItems;
					// If we have Alt Terms, that entry uses up
					// one of our slots
					if( null != alternatives && alternatives.size() > 0 )
						maxUrlSuggestionns--;

					
					// GOOGLE_ONEBOX_RES_HIT_PATH


					// Tabulate suggestions
					int urlSuggestionCounter = 0;

					// Increment the counter, need to increment the other one in
					// fSNActionCount++;
					// Each URL is counted
					// fSNActionItemCount++
	
					// Make a note of what we did
					// if( fSNActionCode == SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
					//	fSNActionCode = SearchTuningConfig.SN_ACTION_CODE_SUGGEST;
					//	fSNActionCode = SearchTuningConfig.SN_ACTION_CODE_ALT;

					// formatWMSuggestionsToSnippet( suggestions, query );
	

					
					// Icon
					// =======
					// TODO: Issue with trailing newline and hyperlink?
					// Element iconElementOrNull = inPrimaryMapRecord.generateWmsIconElementOrNull();

					// Webmaster Suggests slogan
					// ==============================
					// Add the header text / slogan
					// Takes up both cells in the top row of a fauz 2x2 table
					/***
					// Will generate <font> with attrs and content
					Element sloganElement = inPrimaryMapRecord.generateWmsSloganElement();
					if( null!=sloganElement ) {
						// The top row
						Element row1 = new Element( "tr" );
						contentHangerTable.addContent( row1 );
						// The 1x2 cell for the top row
						Element sloganCell = new Element( "td" );
						// only do colspan if needed
						if( null!=iconElementOrNull )
							sloganCell.setAttribute( "colspan", "2" );
						sloganCell.addContent( sloganElement );
						row1.addContent( sloganCell );
					}
					// Start on the main row
					Element row2 = new Element( "tr" );
					contentHangerTable.addContent( row2 );
					// If we have an icon, add a cell and add it in
					if( null!=iconElementOrNull ) {
						Element iconCell = new Element( "td" );
						iconCell.addContent( iconElementOrNull );
						row2.addContent( iconCell );
					}
					// Now we build the cell that will hold the simple list of suggestions
					Element suggestionsCell = new Element( "td" );
					row2.addContent( suggestionsCell );
					***/

					// We need at least one map element to tell us how to format the thing
					MapRecordInterface formatRecord = getMainConfig().getPrimaryWmsMapRecordForTerm( query, fRequestInfo );

					// Start building up the results
					// For each good url record
					for( Iterator sit = suggestions.iterator(); sit.hasNext() && urlSuggestionCounter<maxUrlSuggestionns ; )
					{
						// Get the next one from the list
						SnURLRecord urlRecord = (SnURLRecord)sit.next();
						// format it and add it
						// Main record???
						Element linkElem = urlRecord.generateWebmasterSuggestsGoogleOneBoxItem( formatRecord );
						if( null!=linkElem ) {

							// If this is the FIRST record, we try to jam in the entire formatted chunk
							// from the legacy code
							if( 0 == urlSuggestionCounter )
							{
								Element snippetElem = generateSnippet();
								// The title
								Element payloadField = new Element( SnRequestHandler.GOOGLE_ONEBOX_RESULTS_HIT_FIELD_ELEMENT_NAME );
								payloadField.setAttribute( SnRequestHandler.GOOGLE_ONEBOX_RESULTS_HIT_FIELD_NAME_ATTR, "payload" );
								payloadField.addContent( snippetElem );
								linkElem.addContent( payloadField );
							}
							
							outElem.addContent( linkElem );
							urlSuggestionCounter++;
							fSNActionItemCount++;
						}

					}   // End for each valid record



					// add commments
					// add helpful comments
					/***
					String comments = " Info: NIE SearchTrack:";
					if( optQueryText != null )
						comments += " Query was '" + optQueryText + "'.";
					comments += " Displayed " + inValidSuggestions.size() + " suggestion(s).";
					suggestionsCell.addContent( new Comment( comments ) );
					***/


					
					
					
					
					// formatAlternateSuggestionsToSnippet( alternatives, query );
		
					/***
					boolean haveAddedAtLeastOneTerm = false;

					Element sloganElement = inPrimaryMapRecord.generateAltSloganElement();
					if( null==sloganElement ) {
						errorMsg( kFName, "Got null intro-text/slogan Element, returning null." );
						return null;
					}

					// The top level will be a <div> tag
					Element outElem = new Element("div");
					outElem.addContent( sloganElement );
					// We insert a <br> after the intro heading
					// ^^^ NO, not for alternative suggestions
					// outElem.addContent( new Element("br") );

					// For each good alternate term
					// Some flags
					boolean isFirst = true;
					for( Iterator it2 = inValidSuggestions.iterator(); it2.hasNext() ; )
					{
						// Get the next one from the list
						String altTerm = (String)it2.next();

						// Reset the last flag
						boolean isLast = ! it2.hasNext();

						// Format the results
						// String tmpText = formatAnAlternativeSuggestionRecord(
						//	altTerm, isFirst, isLast
						//	);
						Element termElem = formatAnAlternativeSuggestionRecord( altTerm );

						if( null!=termElem ) {
							if( ! isFirst )
								outElem.addContent( ALT_TERM_SEP );
							outElem.addContent( termElem );
							haveAddedAtLeastOneTerm = true;
							// Reset first flag
							isFirst = false;
						}

						// if( isLast ) ...

					}   // End for each valid record

					// Add the footer
					outElem.addContent( new Element("br") );

					// add commments
					// add helpful comments
					String comments = "Info: NIE SearchNames:";
					if( inQuery != null )
						comments += " Query was '" + inQuery + "'.";
					comments += " Displayed " + inValidSuggestions.size() + " suggestion(s).";
					outElem.addContent( new Comment(comments) );

					// Return the results
					if( haveAddedAtLeastOneTerm )
						// Due to anchor tag issues and white space, we want a compact version with no NL's
						// return JDOMHelper.JDOMToString( outElem, false );
						// No, kils white space, try non-compact
						return JDOMHelper.JDOMToString( outElem, true );
					else
					{
						warningMsg( kFName, "Got to end of routine but had not added"
							+ " any suggestions to buffer."
							// + " Buffer I had was = \"" + new String(buff) + "\"."
							+ " XML buffer I had was = \"" + JDOMHelper.JDOMToString( outElem, true ) + "\"."
							+ " Returning null."
							);
						return null;
					}
				}




					// String theCorrectedTerm = inRecord.getAlternateTerm();
					String theCorrectedTerm = NIEUtil.trimmedStringOrNull( inTerm );
					if( null == theCorrectedTerm ) {
						errorMsg( kFName, "Null term, returning null element." );
						return null;
					}

					// Get the Search URL
					String lSearchURL = getSearchNamesURL();
					// As a backup, we'd take the search engine URL
					if( null == lSearchURL ) {
						lSearchURL = getSearchEngineURL();
						warningMsg( kFName,
							"We were unable to get a URL for Search Names"
							+ " so using the URL for the host search engine instead."
							);
					}


					// Calculate the NEW URL, several steps

					// First we create a new request
					AuxIOInfo newRequest = new AuxIOInfo();
					// Consistent handling of fields
					newRequest.setCGIFieldsCaseSensitive( getSearchEngine().isCGIFeildsCaseSensitive() );
					// Next we need to copy over MOST of the CGI fields
					// but not query text
					String queryField = getUserQueryField();
					// Create an exclude list for the copy command
					List excludes = new Vector();
					// And add this term in
					excludes.add( queryField );
					// Now we're ready to copy over the fields
					newRequest.copyInCGIFields( fRequestInfo, excludes );
					// Now we add in OUR search terms
					newRequest.addCGIField( queryField, theCorrectedTerm );
					// Now ask for all the CGI fields, as an encoded buffer
					String cgifields = newRequest.getCGIFieldsAsEncodedBuffer();
					// Now combine the CGI fields with the URL

					// If we got anything back, add it on
					if( cgifields != null && cgifields.length() > 0 )
					{
						// Join the two strings together, usually with a "?"
						lSearchURL = NIEUtil.concatURLAndEncodedCGIBuffer(
							lSearchURL, cgifields
							);
					}
					// Else something strange happening
					else
					{
						errorMsg( kFName, "Unable to encode CGI variables for new URL."
							+ " New query was \"" + theCorrectedTerm + "\"."
							+ " Returning NULL."
							);
						return null;
					}

					// Create the anchor tag and link text
					Element outElem = new Element( "a" );
					outElem.setAttribute( "class", CSSClassNames.ALT_TERM );
					outElem.setAttribute( "href", lSearchURL );
					// We still force bold tags because we can never be sure if we'll
					// have access to our style sheets
					Element bElem = new Element( "b" );
					outElem.addContent( bElem );
					// And we add text to be inside the bold tag
					bElem.addContent( theCorrectedTerm );

					***/
					
					
				}	// End else we have at least one supported type
			}	// End else had some suggestions
		}
		// Else we do not have a shot at it
		else
		{
		    debugMsg( kFName, "getHasAnyMatchingTerms() was false" );
			outElem.setTextByPath( GOOGLE_ONEBOX_RESULTS_MSG_PATH, "No Matching Suggestions" );
		}   // End else we had no matches

		// save the buffer in Contents
		// fResponseInfo.setContent( outElem.JDOMToString(true) );
		// Turn OFF Pretty Print due to hyperlink and whitespace issues
		fResponseInfo.setContent( outElem.JDOMToString(false) );
		
		if( shouldDoTraceMsg(kFName) )
		{
			String tmpStr = outElem.JDOMToString(true);
			traceMsg( kFName,"Returning:" + NL
					+ "REMINDER: in actual return XHTML is compacted to fix rendering issues, not pretty print. (1)" + NL
					+ tmpStr + NL
					+ "REMINDER: in actual return XHTML is compacted to fix rendering issues, not pretty print. (2)"
					);
		}

		// Make a note that we did have success
		fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE );
	}

	// Prepare for a proxy search
	// If we have no matching terms, redirect to search server
	// If we have terms, but it's a redirect, then do normal redirect
	// If we have no suggest terms, redirect to search server
	// if we do have terms, proxy the search and markup the page
	private void _setupProxySearch_v1( )
	{
			final String kFName = "_setupProxySearch_v1";
			boolean debug = shouldDoDebugMsg( kFName );
	
			// Put small spacers between this and the main document
			// final String kSnippetHeader = "<font size=1>&nbsp;<br/></font>";
			// final String kSnippetFooter = "<font size=1>&nbsp;<br/></font>";
	
			final String kSnippetHeader = "";
			final String kSnippetFooter = "";
	
			// !!! NOTE !!!!
			// If you want to jam in more text before or after, look
			// in markupWithSnippet() and then at the section with
			// getSearchEngine().getSnippetPrefixText(); / Suffix
	
			// TODO: moved up from below, may need to rexamine this logic?
			if( calculateShouldDoFullProxy() ) {
				// markProxyLogging();
				markPostSearchLogging( false );
				// ^^^ This call ALWAYS says "do logging after search"
				// In addition, the false says this isn't just a snippet, it's a full proxy
				// TODO: maybe break that logic out
			}
	
	//		String qry = null;
	//		if( debug )
	//			qry = getUserQuery();
			String qry = getUserQuery();
	
			if( shouldDoInfoMsg(kFName) ) {
				if( qry != null )
					infoMsg( kFName, "Examining search \"" + qry + "\"." );
				else
					infoMsg( kFName, "No search found in request." );
			}
	
			if(debug) debugMsg( kFName, "Will consider null search logic: "
					+ "qry='" + qry + "'"
					+ ", shouldRedirectNullSearches()="
					+ getMainConfig().shouldRedirectNullSearches()
					+ ", isNullSearch(qry)="
					+ getMainConfig().isNullSearch(qry)
				);
			// Special handling for NULL searches
			if( getMainConfig().shouldRedirectNullSearches()
					&& getMainConfig().isNullSearch(qry)
			) {
				String newUrl = getMainConfig().getNullSearchRedirURL();
				// Bail if no URL - this should NOT happen
				// because shouldRedirectNullSearches() already checks for that
				if( null==newUrl ) {
					errorMsg( kFName, "No null search redirect URL!? Reporting error to user." );
					setupTextErrorResponse( "Unable to process NULL search (1); please see log files." );
					return;
				}
				// Did they want to send it back to the referrer?
				if( getMainConfig().isReferrerMarker(newUrl) ) {
					newUrl = fRequestInfo.getReferer();
					if( null==newUrl ) {
						errorMsg( kFName, "No referrer URL for null search redirect. Reporting error to user." );
						setupTextErrorResponse( "Unable to process NULL search (2); please see log files." );
						return;
					}
				}
				// Do the redir
				// TODO: could have options for including CGI fields... messy choices...
				setupSpecificURLRedicect( newUrl );
				return;
			}
	
			// Cache this info, we will need it several times
			boolean hasSNTerms = getHasAnyMatchingTerms();
	
			// First off, see if there's no matching terms
			// and if there's no compelling need to proxy
			if( ! hasSNTerms
				&& ! calculateShouldDoFullProxy()
				)
			{
				infoMsg( kFName,
					"No MATCHTING mapped search term (or no search term at all)."
					+ " Search term(s) (if any) was = \"" + qry + "\"."
					+ " Setting up simple search engine passthrough redirect."
					);
				// If no matching terms, forget about it :-)
				setupPassthroughRedirect();
				return;
			}
	
			// TODO: may need to rexamine this logic?
			// ^^^ moved up in code, seems we'd always want to log, if configured
			// if( calculateShouldDoFullProxy() )
			//	// markProxyLogging();
			//	markPostSearchLogging();
	
	
			// Second of all, if there is a redirect for this term, do that
			// And tell the method to NOT setup any passthroughs
			// So if it returns TRUE, then it DID a redirect
			// If it returns FASLSE, then it didn't find a match
			// AND we've told it NOT to setup a passthrough in that case
			//
			// And we don't proxy this right now
			// Todo: add proxy
			if( setupSearchTermToSpecificURLRedirect( false ) )
			{
				infoMsg( kFName,
					"There was a redirect already set for this term."
					+ " Search term(s) (if any) was = \"" + qry + "\"."
					+ " Redirects take precedence over other actions."
					);
				// So YES, there was a matching redirect
				// and the method will have setup the response, so mothing
				// for us left to do
				return;
			}
	
	
			// do we have matching terms, and would therefore expect a snippet
			// and is the Li cen se turned on enough to allow for it?
			String snippet = null;
			boolean hasUserDataItems = hasUserDataItems();
			if( hasSNTerms && getMainConfig().getLicIsAllGood() ) {
	
				mTiming.addEvent( "SnReq.doProxy.genSnip" );
	
				// Get the snippet
				// This produces the formatted webmaster and alternative suggestions
				snippet = _generateSnippet_v1();
				// Carefully check the results
				// If we didn't get anything, just give a quick redirect
				// Usually there should be by this point in the code
				if( (snippet == null || ! fSnippetToSend) && ! hasUserDataItems ) {
					String tmpMsg = "Although some candidate matching terms were found,"
						+ " no specific results list markup actions were taken."
						+ " Perhaps these are newly mapped terms without any actions defined?"
						+ " Search term(s) (if any) was = \"" + qry + "\"."
						;
	
					// Perhaps we don't want to contiue
					if( ! calculateShouldDoFullProxy() ) {
						infoMsg( kFName,
							tmpMsg
							+ " Setting up search engine passthrough redirect instead."
							);
						// Pass through redirect
						setupPassthroughRedirect();
						// And we're done
						return;
					}
	
					// So We do want to continue
					infoMsg( kFName,
						tmpMsg
						+ " Set to still do host engine proxy search."
						);
				}
				else    // Else we did get a snippet or a user markup
				{
					infoMsg( kFName,
						"Got back a snippet to add to the results page."
						+ " Search term(s) (if any) was = \"" + qry + "\"."
						+ " Will continue with proxy search and results list markup."
						);
	
					// Wrap the snippet, include newlines for readability
					StringBuffer snipBuff = new StringBuffer();
					snipBuff.append( NL );
					snipBuff.append( kSnippetHeader );
					snipBuff.append( NL );
					snipBuff.append( snippet );
					snipBuff.append( NL );
					snipBuff.append( kSnippetFooter );
					snipBuff.append( NL );
					// save the results
					snippet = new String( snipBuff );
					// Sow we now have a snippet we want to add to the page
				}   // End else we did get a snippet back
	
			}   // End if has any matching terms
			// If had matching terms, but lic en se has e x pi red (grace period 2)
			else if( hasSNTerms ) {
				warningMsg( kFName, "N"
					+ "ot pro" + "vid" + "i" + "ng " + 's' + "u" + "gges" + "tio" + "ns; L"
					+ "IC" + "ENS"
					+ 'E' + ' '
					+ 'E' + 'X' + "PI" + 'R' + "ED" + '!'
					+ '!' + '!'
					+ ' ' + "con" + "tact su"
					+ "por" + "t@id"
					+ "eaen"
					+ "g." + 'c' + "om"
					);
			}
	
			// Now we prepare to query the main search engine
			// and then we will craft our response
	
			debugMsg( kFName,
				"Setting up request object to query native search engine."
				);
	
			mTiming.addEvent( "SnReq.doProxy.request.startReq" );
	
			// Prepare a request with the appropriate fields
			AuxIOInfo newRequest = new AuxIOInfo();
			// Make sure we have consistent treatment of fields
			newRequest.setCGIFieldsCaseSensitive( getSearchEngine().isCGIFeildsCaseSensitive() );
			// Now copy over the HTTP header fields
			// except HOST, etc.
			List tmpHExclude = new Vector();
			// We will fix the host
			tmpHExclude.add( "host" );
			// We will always close the connection
			tmpHExclude.add( "connection" );
			// We have made our own content
			tmpHExclude.add( "content-length" );
			// do NOT tell folks like Google that we like gzipped results!
			tmpHExclude.add( "accept-encoding" );
			// Copy over most of the fields, except those we know we don't want
			newRequest.copyInHTTPFields( fRequestInfo, tmpHExclude );
	
			// Add in a user agent field if there is none already
			String clientName = newRequest.getScalarHTTPFieldTrimOrNull(
				AuxIOInfo.HTTP_USER_AGENT_FIELD_SPELLING
				);
			// If there wasn't one in the hash...
			if( clientName == null )
			{
				// Was there one specifically set?
				clientName = fRequestInfo.getClientName();
				// If still don't have one, grab the system default
				clientName = ( clientName != null ) ? clientName
					: AuxIOInfo.DEFAULT_HTTP_USER_AGENT_FIELD
					;
				// Add it to the hash
				newRequest.addHTTPHeaderField(
					// AuxIOInfo.DEFAULT_HTTP_USER_AGENT_FIELD,
					AuxIOInfo.HTTP_USER_AGENT_FIELD_SPELLING,
					clientName
					);
			}
			// Add in the host name
			// let the NIE util do that???
	
			// Add in all the CGI variables
			// Escept a few we don't want
			List tmpCExclude = new Vector();
			// tmpCExclude.add( "nie_sn_context" );
			tmpCExclude.add( NIE_CONTEXT_REQUEST_FIELD );
			// Copy over most of the fields, except those we know we don't want
			debugMsg( kFName, "fRequestInfo.isCGIFieldsCaseSensitive=" +
				fRequestInfo.isCGIFieldsCaseSensitive() );
			newRequest.copyInCGIFields( fRequestInfo, tmpCExclude );
	
			// Add in Search Track logging if we're using direct logging
			// This is actually somewhat rare:
			// If logging is off, and there was no matching term, we would have
			// already bailed to the passthrough search
			// If logging is on, we would usually just do the proxy style logging
			// if( doSearchLogging() && ! doesSearchLoggingRequireProxy() )
			if( doSearchLogging() && ! doesSearchLoggingRequireScraping() )
			{
				// This is one of two places we call this
				// The other place is if we setup a search engine passthrough
				// redirect
				addSearchTrackDirectLoggingCGIFields( newRequest );
			}
	
			// Form the new URL
			// with the cgi vars, except ours
			String theURL = getSearchEngineURL();
	
	
			if(debug) {
				debugMsg( kFName,
					"Will attempt to query the host search engine with URL=\"" + theURL + "\" ..."
					);
				debugMsg( kFName,
					"With fields:" + NIEUtil.NL
					+ newRequest.displayCGIFieldsIntoBuffer()
					);
			}
	
			// Show lots of details.  But don't bother if they don't care
			if( shouldDoTraceMsg(kFName) )
			{
				traceMsg( kFName, "Complete request is:" + NL +
					newRequest.displayRequestIntoBuffer() + NL
					);
			}
	
			// Query the main search engine
			String newContent = null;
			try
			{
			    debugMsg( kFName,
			            "getSearchEngine().getSearchEngineMethod()="
			            + getSearchEngine().getSearchEngineMethod()
			            );
			    // Call the NIE static method to fetch it
				// We're passing in the all important io stuff via newRequest
				// Which is where it will get the HTTP and CGI hashes
				newContent = NIEUtil.fetchURIContentsChar(
					theURL,
					null, null, null,
					newRequest,
					getSearchEngine().getSearchEngineMethod().equalsIgnoreCase("POST")
					);
			}
			catch( IOException ioe )
			{
				String msg = "Could not retrieve URL; caught an exception."
					+ " URL=\"" + theURL + "\"."
					+ " Exception=\"" + ioe + "\"."
					+ " Will setup passthrough redirect instead."
					;
				errorMsg( kFName, msg );
	
				// Log status info for SearchNames response
				if( fSNStatusCode == SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
				{
					fSNStatusCode = SearchTuningConfig.SN_ERROR_WITH_EXTERNAL_CALL;
					fSNStatusMsg = msg;
				}
	
				// In this case, even if we would like to do proxy logging,
				// we wereen't able to talk to the search engine, so we have
				// no choice but to passthrough
				// Warn about losing data
				// if( doSearchLogging() && doesSearchLoggingRequireProxy() )
				if( doSearchLogging() && doesSearchLoggingRequireScraping() )
					errorMsg( kFName,
						"Unable to complete proxy-style search logging (1)."
						+ " Since was unable to query host search engine"
						+ " must send user directly to engine via redirect"
						+ " so this transaction will not be logged."
						);
				if( alwaysProxy() )
					errorMsg( kFName,
						"Unable to proxy this search as requested, by always proxy (1)."
						+ " Since was unable to query host search engine"
						+ " must send user directly to engine via redirect"
						+ " so this transaction is not being proxied."
						);
				// This will add any cgi fields we need for direct logging
				setupPassthroughRedirect();
				return;
			}
			// Sanity check on the content
			if( newContent == null )
			{
				String msg = "Could not retrieve URL; got back a null string."
					+ " URL=\"" + theURL + "\"."
					+ " Will setup passthrough redirect instead."
					;
				errorMsg( kFName, msg );
	
				// Log status info for SearchNames response
				if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
				{
					fSNStatusCode = SearchTuningConfig.SN_ERROR_WITH_EXTERNAL_CALL;
					fSNStatusMsg = msg;
				}
	
				// if( doSearchLogging() && doesSearchLoggingRequireProxy() )
				if( doSearchLogging() && doesSearchLoggingRequireScraping() )
					errorMsg( kFName,
						"Unable to complete proxy-style search logging (2)."
						+ " Since was unable to query host search engine"
						+ " must send user directly to engine via redirect"
						+ " so this transaction will not be logged."
						);
				if( alwaysProxy() )
					errorMsg( kFName,
						"Unable to proxy this search as requested, by always proxy (2)."
						+ " Since was unable to query host search engine"
						+ " must send user directly to engine via redirect"
						+ " so this transaction is not being proxied."
						);
	
				setupPassthroughRedirect();
				return;
			}
	
			mTiming.addEvent( "SnReq.doProxy.request.endReq" );
	
			debugMsg( kFName,
				"Response from search is " + newContent.length() + " characters long."
				);
	
			// Start to form the answer
			String replyBuff = newContent;
	
			// Fix the relative path issue in the HTML content
			debugMsg( kFName, "Will attempt to add <INDEX> tag (if one is needed)" );
			replyBuff = markupWithBaseHref( replyBuff );
	
	
			// do the substitution
			// And if snippet should not have been null we will
			// have whined about it above already
			if( snippet != null )
			{
				// !!! NOTE !!!!
				// If you want to jam in text before or after, look
				// in markupWithSnippet() and then at the section with
				// getSearchEngine().getSnippetPrefixText(); / Suffix
	
	
				replyBuff = markupWithSnippet( replyBuff, snippet );
			}
			
			
			
			// Ad in the user data items and/or advertisements
			if( hasUserDataItems )
				replyBuff = markupWithUserDataItems( replyBuff );
		
			mTiming.addEvent( "SnReq.doProxy.markupEnd" );
	
		
			// Make a note that things seemed to go OK
			// Of course if there's a previous error, we leave it alone
			// Setup an internal search names status if one has not been set
			if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
				fSNStatusCode = SearchTuningConfig.SN_STATUS_OK;
	
			// Make a note that we did have success
			fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE );
	
			// return the new doc
			fResponseInfo.setContent( replyBuff + NL );
	
		}


	// Prepare for a proxy search
	// If we have no matching terms, redirect to search server
	// If we have terms, but it's a redirect, then do normal redirect
	// If we have no suggest terms, redirect to search server
	// if we do have terms, proxy the search and markup the page
	private void _setupProxySearch_v2( )
	{
		final String kFName = "_setupProxySearch_v2";
		boolean debug = shouldDoDebugMsg( kFName );

		// Put small spacers between this and the main document
		// final String kSnippetHeader = "<font size=1>&nbsp;<br/></font>";
		// final String kSnippetFooter = "<font size=1>&nbsp;<br/></font>";

		final String kSnippetHeader = "";
		final String kSnippetFooter = "";

		// !!! NOTE !!!!
		// If you want to jam in more text before or after, look
		// in markupWithSnippet() and then at the section with
		// getSearchEngine().getSnippetPrefixText(); / Suffix

		// TODO: moved up from below, may need to rexamine this logic?
		if( calculateShouldDoFullProxy() ) {
			// markProxyLogging();
			markPostSearchLogging( false );
			// ^^^ This call ALWAYS says "do logging after search"
			// In addition, the false says this isn't just a snippet, it's a full proxy
			// TODO: maybe break that logic out
		}

//		String qry = null;
//		if( debug )
//			qry = getUserQuery();
		String qry = getUserQuery();

		if( shouldDoInfoMsg(kFName) ) {
			if( qry != null )
				infoMsg( kFName, "Examining search \"" + qry + "\"." );
			else
				infoMsg( kFName, "No search found in request." );
		}

		if(debug) debugMsg( kFName, "Will consider null search logic: "
				+ "qry='" + qry + "'"
				+ ", shouldRedirectNullSearches()="
				+ getMainConfig().shouldRedirectNullSearches()
				+ ", isNullSearch(qry)="
				+ getMainConfig().isNullSearch(qry)
			);
		// Special handling for NULL searches
		if( getMainConfig().shouldRedirectNullSearches()
				&& getMainConfig().isNullSearch(qry)
		) {
			String newUrl = getMainConfig().getNullSearchRedirURL();
			// Bail if no URL - this should NOT happen
			// because shouldRedirectNullSearches() already checks for that
			if( null==newUrl ) {
				errorMsg( kFName, "No null search redirect URL!? Reporting error to user." );
				setupTextErrorResponse( "Unable to process NULL search (1); please see log files." );
				return;
			}
			// Did they want to send it back to the referrer?
			if( getMainConfig().isReferrerMarker(newUrl) ) {
				newUrl = fRequestInfo.getReferer();
				if( null==newUrl ) {
					errorMsg( kFName, "No referrer URL for null search redirect. Reporting error to user." );
					setupTextErrorResponse( "Unable to process NULL search (2); please see log files." );
					return;
				}
			}
			// Do the redir
			// TODO: could have options for including CGI fields... messy choices...
			setupSpecificURLRedicect( newUrl );
			return;
		}

		// Cache this info, we will need it several times
		boolean hasSNTerms = getHasAnyMatchingTerms();

		// First off, see if there's no matching terms
		// and if there's no compelling need to proxy
		if( ! hasSNTerms
			&& ! calculateShouldDoFullProxy()
			)
		{
			infoMsg( kFName,
				"No MATCHTING mapped search term (or no search term at all)."
				+ " Search term(s) (if any) was = \"" + qry + "\"."
				+ " Setting up simple search engine passthrough redirect."
				);
			// If no matching terms, forget about it :-)
			setupPassthroughRedirect();
			return;
		}

		// TODO: may need to rexamine this logic?
		// ^^^ moved up in code, seems we'd always want to log, if configured
		// if( calculateShouldDoFullProxy() )
		//	// markProxyLogging();
		//	markPostSearchLogging();


		// Second of all, if there is a redirect for this term, do that
		// And tell the method to NOT setup any passthroughs
		// So if it returns TRUE, then it DID a redirect
		// If it returns FASLSE, then it didn't find a match
		// AND we've told it NOT to setup a passthrough in that case
		//
		// And we don't proxy this right now
		// Todo: add proxy
		if( setupSearchTermToSpecificURLRedirect( false ) )
		{
			infoMsg( kFName,
				"There was a redirect already set for this term."
				+ " Search term(s) (if any) was = \"" + qry + "\"."
				+ " Redirects take precedence over other actions."
				);
			// So YES, there was a matching redirect
			// and the method will have setup the response, so mothing
			// for us left to do
			return;
		}


		// do we have matching terms, and would therefore expect a snippet
		// and is the Li cen se turned on enough to allow for it?
		String snippet = null;
		boolean hasUserDataItems = hasUserDataItems();
		if( hasSNTerms && getMainConfig().getLicIsAllGood() ) {

			mTiming.addEvent( "SnReq.doProxy.genSnip" );

			// Get the snippet
			// This produces the formatted webmaster and alternative suggestions
			snippet = _generateSnippet_v1();
			// Carefully check the results
			// If we didn't get anything, just give a quick redirect
			// Usually there should be by this point in the code
			if( (snippet == null || ! fSnippetToSend) && ! hasUserDataItems ) {
				String tmpMsg = "Although some candidate matching terms were found,"
					+ " no specific results list markup actions were taken."
					+ " Perhaps these are newly mapped terms without any actions defined?"
					+ " Search term(s) (if any) was = \"" + qry + "\"."
					;

				// Perhaps we don't want to contiue
				if( ! calculateShouldDoFullProxy() ) {
					infoMsg( kFName,
						tmpMsg
						+ " Setting up search engine passthrough redirect instead."
						);
					// Pass through redirect
					setupPassthroughRedirect();
					// And we're done
					return;
				}

				// So We do want to continue
				infoMsg( kFName,
					tmpMsg
					+ " Set to still do host engine proxy search."
					);
			}
			else    // Else we did get a snippet or a user markup
			{
				infoMsg( kFName,
					"Got back a snippet to add to the results page."
					+ " Search term(s) (if any) was = \"" + qry + "\"."
					+ " Will continue with proxy search and results list markup."
					);

				// Wrap the snippet, include newlines for readability
				StringBuffer snipBuff = new StringBuffer();
				snipBuff.append( NL );
				snipBuff.append( kSnippetHeader );
				snipBuff.append( NL );
				snipBuff.append( snippet );
				snipBuff.append( NL );
				snipBuff.append( kSnippetFooter );
				snipBuff.append( NL );
				// save the results
				snippet = new String( snipBuff );
				// Sow we now have a snippet we want to add to the page
			}   // End else we did get a snippet back

		}   // End if has any matching terms
		// If had matching terms, but lic en se has e x pi red (grace period 2)
		else if( hasSNTerms ) {
			warningMsg( kFName, "N"
				+ "ot pro" + "vid" + "i" + "ng " + 's' + "u" + "gges" + "tio" + "ns; L"
				+ "IC" + "ENS"
				+ 'E' + ' '
				+ 'E' + 'X' + "PI" + 'R' + "ED" + '!'
				+ '!' + '!'
				+ ' ' + "con" + "tact su"
				+ "por" + "t@id"
				+ "eaen"
				+ "g." + 'c' + "om"
				);
		}

		// Now we prepare to query the main search engine
		// and then we will craft our response

		// Call routines to DO THE SEARCH
		// =========================================
		String newContent = null;
		try {
			newContent = doActualSearch( qry );
		}
		catch( IOException ioe )
		{
			stackTrace( kFName, ioe, "IO Exception fetching content, details to follow..." );
			// In this case, even if we would like to do proxy logging,
			// we wereen't able to talk to the search engine, so we have
			// no choice but to passthrough
			// Warn about losing data
			// if( doSearchLogging() && doesSearchLoggingRequireProxy() )
			// TODO: Are these different states of error really necessary?
			if( doSearchLogging() && doesSearchLoggingRequireScraping() )
			{
				errorMsg( kFName,
					"Error fetching content from host search egnine"
					+ ", and may be unable to complete proxy-style search logging (1)."
					+ " Since was unable to query host search engine"
					+ " must send user directly to engine via redirect"
					+ " and this transaction may not be logged."
					+ " IOException: " + ioe
					);
			}
			else if( alwaysProxy() )
			{
				errorMsg( kFName,
					"Unable to proxy this search as requested, by always proxy (1)."
					+ " Since was unable to query host search engine"
					+ " must send user directly to engine via redirect"
					+ " and this transaction is not being proxied."
					+ " IOException: " + ioe
					);
			}
			else {
				errorMsg( kFName,
						"Error fetching content from host search egnine."
						+ " Since was unable to query host search engine"
						+ " must send user directly to engine via redirect"
						+ " so this transaction may not be logged."
						+ " IOException: " + ioe
						);					
			}

			// Log status info for SearchNames response
			if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
			{
				fSNStatusCode = SearchTuningConfig.SN_ERROR_WITH_EXTERNAL_CALL;
				fSNStatusMsg = "" + ioe;
			}

			// This will add any cgi fields we need for direct logging
			setupPassthroughRedirect();
			return;

		}

		// Sanity check on the content
		if( newContent == null )
		{

			String msg = "Could not retrieve URL; got back a null string."
				// + " URL=\"" + theURL + "\"."
				+ " Will setup passthrough redirect instead."
				;
			errorMsg( kFName, msg );

			// Log status info for SearchNames response
			if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
			{
				fSNStatusCode = SearchTuningConfig.SN_ERROR_WITH_EXTERNAL_CALL;
				fSNStatusMsg = msg;
			}

			// if( doSearchLogging() && doesSearchLoggingRequireProxy() )
			if( doSearchLogging() && doesSearchLoggingRequireScraping() )
				errorMsg( kFName,
					"Unable to complete proxy-style search logging (2)."
					+ " Since was unable to query host search engine"
					+ " must send user directly to engine via redirect"
					+ " so this transaction will not be logged."
					);
			if( alwaysProxy() )
				errorMsg( kFName,
					"Unable to proxy this search as requested, by always proxy (2)."
					+ " Since was unable to query host search engine"
					+ " must send user directly to engine via redirect"
					+ " so this transaction is not being proxied."
					);

			setupPassthroughRedirect();
			return;
		}

		debugMsg( kFName,
			"Response from search is " + newContent.length() + " characters long."
			);

		// Start to form the answer
		String replyBuff = newContent;

		// Fix the relative path issue in the HTML content, Base URL, etc.
		debugMsg( kFName, "Will attempt to add <INDEX> tag (if one is needed)" );
		replyBuff = markupWithBaseHref( replyBuff );


		// do the substitution
		// And if snippet should not have been null we will
		// have whined about it above already
		if( snippet != null )
		{
			// !!! NOTE !!!!
			// If you want to jam in text before or after, look
			// in markupWithSnippet() and then at the section with
			// getSearchEngine().getSnippetPrefixText(); / Suffix

			replyBuff = markupWithSnippet( replyBuff, snippet );
		}
		
		
		
		// Ad in the user data items and/or advertisements
		if( hasUserDataItems )
			replyBuff = markupWithUserDataItems( replyBuff );
	
		mTiming.addEvent( "SnReq.doProxy.markupEnd" );

	
		// Make a note that things seemed to go OK
		// Of course if there's a previous error, we leave it alone
		// Setup an internal search names status if one has not been set
		if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
			fSNStatusCode = SearchTuningConfig.SN_STATUS_OK;

		// Make a note that we did have success
		fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE );

		// return the new doc
		fResponseInfo.setContent( replyBuff + NL );

	}

	// Prepare for a proxy search
	// If we have no matching terms, redirect to search server
	// If we have terms, but it's a redirect, then do normal redirect
	// If we have no suggest terms, redirect to search server
	// if we do have terms, proxy the search and markup the page
	private void setupProxySearch( )
	{
		final String kFName = "setupProxySearch";
		boolean debug = shouldDoDebugMsg( kFName );

		// Put small spacers between this and the main document
		// final String kSnippetHeader = "<font size=1>&nbsp;<br/></font>";
		// final String kSnippetFooter = "<font size=1>&nbsp;<br/></font>";

		final String kSnippetHeader = "";
		final String kSnippetFooter = "";

		// !!! NOTE !!!!
		// If you want to jam in more text before or after, look
		// in markupWithSnippet() and then at the section with
		// getSearchEngine().getSnippetPrefixText(); / Suffix

		// TODO: moved up from below, may need to rexamine this logic?
		if( calculateShouldDoFullProxy() ) {
			// markProxyLogging();
			markPostSearchLogging( false );
			// ^^^ This call ALWAYS says "do logging after search"
			// In addition, the false says this isn't just a snippet, it's a full proxy
			// TODO: maybe break that logic out
		}

//		String qry = null;
//		if( debug )
//			qry = getUserQuery();
		String qry = getUserQuery();

		if( shouldDoInfoMsg(kFName) ) {
			if( qry != null )
				infoMsg( kFName, "Examining search \"" + qry + "\"." );
			else
				infoMsg( kFName, "No search found in request." );
		}

		if(debug) debugMsg( kFName, "Will consider null search logic: "
				+ "qry='" + qry + "'"
				+ ", shouldRedirectNullSearches()="
				+ getMainConfig().shouldRedirectNullSearches()
				+ ", isNullSearch(qry)="
				+ getMainConfig().isNullSearch(qry)
			);
		// Special handling for NULL searches
		if( getMainConfig().shouldRedirectNullSearches()
				&& getMainConfig().isNullSearch(qry)
		) {
			String newUrl = getMainConfig().getNullSearchRedirURL();
			// Bail if no URL - this should NOT happen
			// because shouldRedirectNullSearches() already checks for that
			if( null==newUrl ) {
				errorMsg( kFName, "No null search redirect URL!? Reporting error to user." );
				setupTextErrorResponse( "Unable to process NULL search (1); please see log files." );
				return;
			}
			// Did they want to send it back to the referrer?
			if( getMainConfig().isReferrerMarker(newUrl) ) {
				newUrl = fRequestInfo.getReferer();
				if( null==newUrl ) {
					errorMsg( kFName, "No referrer URL for null search redirect. Reporting error to user." );
					setupTextErrorResponse( "Unable to process NULL search (2); please see log files." );
					return;
				}
			}
			// Do the redir
			// TODO: could have options for including CGI fields... messy choices...
			setupSpecificURLRedicect( newUrl );
			return;
		}

		// Cache this info, we will need it several times
		boolean hasSNTerms = getHasAnyMatchingTerms();

		// First off, see if there's no matching terms
		// and if there's no compelling need to proxy
		if( ! hasSNTerms
			&& ! calculateShouldDoFullProxy()
			)
		{
			infoMsg( kFName,
				"No MATCHTING mapped search term (or no search term at all)."
				+ " Search term(s) (if any) was = \"" + qry + "\"."
				+ " Setting up simple search engine passthrough redirect."
				);
			// If no matching terms, forget about it :-)
			setupPassthroughRedirect();
			return;
		}

		// TODO: may need to rexamine this logic?
		// ^^^ moved up in code, seems we'd always want to log, if configured
		// if( calculateShouldDoFullProxy() )
		//	// markProxyLogging();
		//	markPostSearchLogging();


		// Second of all, if there is a redirect for this term, do that
		// And tell the method to NOT setup any passthroughs
		// So if it returns TRUE, then it DID a redirect
		// If it returns FASLSE, then it didn't find a match
		// AND we've told it NOT to setup a passthrough in that case
		//
		// And we don't proxy this right now
		// Todo: add proxy
		if( setupSearchTermToSpecificURLRedirect( false ) )
		{
			infoMsg( kFName,
				"There was a redirect already set for this term."
				+ " Search term(s) (if any) was = \"" + qry + "\"."
				+ " Redirects take precedence over other actions."
				);
			// So YES, there was a matching redirect
			// and the method will have setup the response, so mothing
			// for us left to do
			return;
		}


		// do we have matching terms, and would therefore expect a snippet
		// and is the Li cen se turned on enough to allow for it?
		Element snippetElem = null;
		String snippetStr = null;
		boolean hasUserDataItems = hasUserDataItems();
		if( hasSNTerms && getMainConfig().getLicIsAllGood() )
		{

			mTiming.addEvent( "SnReq.doProxy.genSnip" );

			// Get the snippet
			// This produces the formatted webmaster and alternative suggestions
			snippetElem = generateSnippet();
			if( null!=snippetElem )
				snippetStr = JDOMHelper.JDOMToString( snippetElem, true );
			// Carefully check the results
			// If we didn't get anything, just give a quick redirect
			// Usually there should be by this point in the code
			if( (snippetStr == null || ! fSnippetToSend) && ! hasUserDataItems ) {
				String tmpMsg = "Although some candidate matching terms were found,"
					+ " no specific results list markup actions were taken."
					+ " Perhaps these are newly mapped terms without any actions defined?"
					+ " Search term(s) (if any) was = \"" + qry + "\"."
					;

				// Perhaps we don't want to contiue
				if( ! calculateShouldDoFullProxy() ) {
					infoMsg( kFName,
						tmpMsg
						+ " Setting up search engine passthrough redirect instead."
						);
					// Pass through redirect
					setupPassthroughRedirect();
					// And we're done
					return;
				}

				// So We do want to continue
				infoMsg( kFName,
					tmpMsg
					+ " Set to still do host engine proxy search."
					);
			}
			else    // Else we did get a snippet or a user markup
			{
				infoMsg( kFName,
					"Got back a snippet to add to the results page."
					+ " Search term(s) (if any) was = \"" + qry + "\"."
					+ " Will continue with proxy search and results list markup."
					);

				// Wrap the snippet, include newlines for readability
				StringBuffer snipBuff = new StringBuffer();
				snipBuff.append( NL );
				snipBuff.append( kSnippetHeader );
				snipBuff.append( NL );
				// snipBuff.append( snippet );
				snipBuff.append( snippetStr );
				snipBuff.append( NL );
				snipBuff.append( kSnippetFooter );
				snipBuff.append( NL );
				// save the results
				snippetStr = new String( snipBuff );
				// Sow we now have a snippet we want to add to the page
			}   // End else we did get a snippet back

		}   // End if has any matching terms
		// If had matching terms, but lic en se has e x pi red (grace period 2)
		else if( hasSNTerms )
		{
			warningMsg( kFName, "N"
				+ "ot pro" + "vid" + "i" + "ng " + 's' + "u" + "gges" + "tio" + "ns; L"
				+ "IC" + "ENS"
				+ 'E' + ' '
				+ 'E' + 'X' + "PI" + 'R' + "ED" + '!'
				+ '!' + '!'
				+ ' ' + "con" + "tact su"
				+ "por" + "t@id"
				+ "eaen"
				+ "g." + 'c' + "om"
				);
		}

		// Now we prepare to query the main search engine
		// and then we will craft our response

		// Call routines to DO THE SEARCH
		// =========================================
		String newContent = null;
		try {
			newContent = doActualSearch( qry );
			// The field fIntermediateIoInfo
			// will have additional information about
			// the returned document such as
			// mine type / content type and character encoding
		}
		catch( IOException ioe )
		{
			// In this case, even if we would like to do proxy logging,
			// we wereen't able to talk to the search engine, so we have
			// no choice but to passthrough
			// Warn about losing data
			// if( doSearchLogging() && doesSearchLoggingRequireProxy() )
			// TODO: Are these different states of error really necessary?
			if( doSearchLogging() && doesSearchLoggingRequireScraping() )
			{
				errorMsg( kFName,
					"Error fetching content from host search egnine"
					+ ", and may be unable to complete proxy-style search logging (1)."
					+ " Since was unable to query host search engine"
					+ " must send user directly to engine via redirect"
					+ " and this transaction may not be logged."
					+ " IOException: " + ioe
					);
			}
			else if( alwaysProxy() )
			{
				errorMsg( kFName,
					"Unable to proxy this search as requested, by always proxy (1)."
					+ " Since was unable to query host search engine"
					+ " must send user directly to engine via redirect"
					+ " and this transaction is not being proxied."
					+ " IOException: " + ioe
					);
			}
			else {
				errorMsg( kFName,
						"Error fetching content from host search egnine."
						+ " Since was unable to query host search engine"
						+ " must send user directly to engine via redirect"
						+ " so this transaction may not be logged."
						+ " IOException: " + ioe
						);					
			}

			// Log status info for SearchNames response
			if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
			{
				fSNStatusCode = SearchTuningConfig.SN_ERROR_WITH_EXTERNAL_CALL;
				fSNStatusMsg = "" + ioe;
			}

			// This will add any cgi fields we need for direct logging
			setupPassthroughRedirect();
			return;

		}

		// Sanity check on the content
		if( newContent == null )
		{

			String msg = "Could not retrieve URL; got back a null string."
				// + " URL=\"" + theURL + "\"."
				+ " Will setup passthrough redirect instead."
				;
			errorMsg( kFName, msg );

			// Log status info for SearchNames response
			if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
			{
				fSNStatusCode = SearchTuningConfig.SN_ERROR_WITH_EXTERNAL_CALL;
				fSNStatusMsg = msg;
			}

			// if( doSearchLogging() && doesSearchLoggingRequireProxy() )
			if( doSearchLogging() && doesSearchLoggingRequireScraping() )
				errorMsg( kFName,
					"Unable to complete proxy-style search logging (2)."
					+ " Since was unable to query host search engine"
					+ " must send user directly to engine via redirect"
					+ " so this transaction will not be logged."
					);
			if( alwaysProxy() )
				errorMsg( kFName,
					"Unable to proxy this search as requested, by always proxy (2)."
					+ " Since was unable to query host search engine"
					+ " must send user directly to engine via redirect"
					+ " so this transaction is not being proxied."
					);

			setupPassthroughRedirect();
			return;
		}

		debugMsg( kFName,
			"Response from search is " + newContent.length() + " characters long."
			);

		// Start to form the answer
		String replyBuff = newContent;

		// Fix the relative path issue in the HTML content, Base URL, etc.
		debugMsg( kFName, "Will attempt to add <INDEX> tag (if one is needed)" );
		replyBuff = markupWithBaseHref( replyBuff );


		// do the substitution
		// And if snippet should not have been null we will
		// have whined about it above already
		if( snippetStr != null )
		{
			// !!! NOTE !!!!
			// If you want to jam in text before or after, look
			// in markupWithSnippet() and then at the section with
			// getSearchEngine().getSnippetPrefixText(); / Suffix

			replyBuff = markupWithSnippet( replyBuff, snippetStr );
		}
		
		// Ad in the user data items and/or advertisements
		if( hasUserDataItems )
			replyBuff = markupWithUserDataItems( replyBuff );

		// Tweaking forms and doc links
		replyBuff = markupResultsListSearchFormsIfNeeded( replyBuff );
		replyBuff = markupResultsListDocLinksIfNeeded( replyBuff );

		mTiming.addEvent( "SnReq.doProxy.markupEnd" );

	
		// Make a note that things seemed to go OK
		// Of course if there's a previous error, we leave it alone
		// Setup an internal search names status if one has not been set
		if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
			fSNStatusCode = SearchTuningConfig.SN_STATUS_OK;

		// Make a note that we did have success
		fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE );

		// return the new doc
		fResponseInfo.setContent( replyBuff + NL );

		// Mark mark the encoding, etc
		if( null!=fIntermediateIoInfo )
		{
			String contentType = fIntermediateIoInfo.getContentType();
			if( null!=contentType )
				fResponseInfo.setContentType( contentType );
			else
				warningMsg( kFName, "No content-type / mime-type"
					+ " for client request '" + fRequestInfo.getBasicURL() + "'"
					+ ", search engine requst '" + fIntermediateIoInfo.getFullCGIEncodedURL() + "'"
					);
			String encoding = fIntermediateIoInfo.getEncodingOrNull();
			if( null!=encoding )
				fResponseInfo.setEncoding( encoding );
			else
				warningMsg( kFName, "No encoding specified for content-type / mime-type '" + contentType + "'"
					+ ", for client request '" + fRequestInfo.getBasicURL() + "'"
					+ ", search engine requst '" + fIntermediateIoInfo.getFullCGIEncodedURL() + "'"
					);
		}
		// No info to pass along
		else
		{
			debugMsg( kFName, "No fIntermediateIoInfo object"
				+ ", which might be normal if this is not a search,"
				+ " for client request '" + fRequestInfo.getBasicURL() + "'"
				);		
		}
	}


	// Helps with the actual search from the host engine
	// Will throw exception if error or no results at all
	// Originall part of setupProxySearch()
	public String doActualSearch( String qry )
		throws IOException
	{
		final String kFName = "doActualSearch";
		boolean debug = shouldDoDebugMsg( kFName );

		if( shouldDoInfoMsg(kFName) ) {
			if( qry != null )
				infoMsg( kFName, "Examining search \"" + qry + "\"." );
			else
				infoMsg( kFName, "No search found in request." );
		}

		// Special handling for NULL searches
		if( getMainConfig().shouldRedirectNullSearches()
				&& getMainConfig().isNullSearch(qry)
		) {
			// warningMsg( kFName,
			//	"Null search dectected and null redirect logic active, returning null."
			//	);
			// return null;
			throw new IOException( "Null search dectected and null redirect logic active, nothing to do." );
		}

		if(debug) debugMsg( kFName, "Will consider null search logic: "
				+ "qry='" + qry + "'"
				+ ", shouldRedirectNullSearches()="
				+ getMainConfig().shouldRedirectNullSearches()
				+ ", isNullSearch(qry)="
				+ getMainConfig().isNullSearch(qry)
			);

		// use timing
		mTiming.addEvent( "SnReq.doProxy.request.startReq" );

		// Query the main search engine
		// Do the actual call
		String newContent = null;
		IOException ex = null;
		// We want to retain information about the search results
		fIntermediateIoInfo = new AuxIOInfo();
		try
		{
			newContent = staticDoActualSearch( qry, getMainConfig().getSearchEngine(), fRequestInfo, fIntermediateIoInfo, this );			
		}
		catch( IOException ioe )
		{
			ex = ioe;
			// Log status info for SearchNames response
			if( fSNStatusCode == SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
			{
				fSNStatusCode = SearchTuningConfig.SN_ERROR_WITH_EXTERNAL_CALL;
				fSNStatusMsg = "" + ioe;
			}
		}
		// end time
		mTiming.addEvent( "SnReq.doProxy.request.endReq" );
		// TODO: should we just throw this sooner?  Is the state stuff that important?
		// TODO: maybe use rethrow, though that is only supported in later JVMs
		if( null!=ex )
			throw ex;
		
		return newContent;
		
	}


	// Helps with the actual search from the host engine
	// Originally part of setupProxySearch()
	// This was factored out to static so other modules could us it
	public static String staticDoActualSearch(
			String qry,
			// SearchTuningConfig config,
			SearchEngineConfig config,
			AuxIOInfo optRequestInfo,
			AuxIOInfo optUseThisIntermediateIoInfo,
			SnRequestHandler optHanlder
		)
			throws IOException
	{
		final String kFName = "staticDoActualSearch";
		boolean debug = shouldDoDebugMsg( kFName );

		// Sanity check for public util functions
		if( null==config )
			throw new IOException( "Null search engine config passed in." );

		if( shouldDoInfoMsg(kFName) ) {
			if( qry != null )
				infoMsg( kFName, "Examining search \"" + qry + "\"." );
			else
				infoMsg( kFName, "No search found in request." );
		}

		// Now we prepare to query the main search engine
		// and then we will craft our response

		staticDebugMsg( kFName,
			"Setting up request object to query native search engine."
			);

		// Prepare a request with the appropriate fields
		// AuxIOInfo newRequest = new AuxIOInfo();
		AuxIOInfo newRequest = null;
		if( null!=optUseThisIntermediateIoInfo )
			newRequest = optUseThisIntermediateIoInfo;
		else
			newRequest = new AuxIOInfo();

		// Make sure we have consistent treatment of fields
		newRequest.setCGIFieldsCaseSensitive( config.isCGIFeildsCaseSensitive() );
		// Now copy over the HTTP header fields
		// except HOST, etc.
		List tmpHExclude = new Vector();
		// We will fix the host
		tmpHExclude.add( "host" );
		// We will always close the connection
		tmpHExclude.add( "connection" );
		// We have made our own content
		tmpHExclude.add( "content-length" );
		// do NOT tell folks like Google that we like gzipped results!
		tmpHExclude.add( "accept-encoding" );
		// Copy over most of the fields, except those we know we don't want
		if( null!=optRequestInfo )
			newRequest.copyInHTTPFields( optRequestInfo, tmpHExclude );

		// Add in a user agent field if there is none already
		String clientName = newRequest.getScalarHTTPFieldTrimOrNull(
			AuxIOInfo.HTTP_USER_AGENT_FIELD_SPELLING
			);
		// If there wasn't one in the hash...
		if( clientName == null )
		{
			// Was there one specifically set?
			if( null!=optRequestInfo )
				clientName = optRequestInfo.getClientName();
			// If still don't have one, grab the system default
			clientName = ( clientName != null ) ? clientName
				: AuxIOInfo.DEFAULT_HTTP_USER_AGENT_FIELD
				;
			// Add it to the hash
			newRequest.addHTTPHeaderField(
				// AuxIOInfo.DEFAULT_HTTP_USER_AGENT_FIELD,
				AuxIOInfo.HTTP_USER_AGENT_FIELD_SPELLING,
				clientName
				);
		}
		// Add in the host name
		// let the NIE util do that???

		// Add in all the CGI variables
		// Escept a few we don't want
		List tmpCExclude = new Vector();
		// tmpCExclude.add( "nie_sn_context" );
		tmpCExclude.add( NIE_CONTEXT_REQUEST_FIELD );
		// Copy over most of the fields, except those we know we don't want
		if( null!=optRequestInfo ) {
			staticDebugMsg( kFName, "fRequestInfo.isCGIFieldsCaseSensitive=" +
					optRequestInfo.isCGIFieldsCaseSensitive() );
			newRequest.copyInCGIFields( optRequestInfo, tmpCExclude );
		}

		// Make sure the actual query field is there
		// Normally we would have picked it up from the request object
		// but if called statically, might not have gotten it
		if( null!=qry ) {
			if( ! newRequest.getCGIFieldKeys().contains( config.getQueryField() ) )
				newRequest.addCGIField( config.getQueryField(), qry );
		}
		// TODO: we do not add the testdrive fields here and should not, but some
		// other caller should if using this static

		// if( doSearchLogging() && ! doesSearchLoggingRequireScraping() )
		// addSearchTrackDirectLoggingCGIFields( newRequest );
		// Add in Search Track logging if we're using direct logging
		// This is actually somewhat rare:
		// If logging is off, and there was no matching term, we would have
		// already bailed to the passthrough search
		// If logging is on, we would usually just do the proxy style logging
		// if( doSearchLogging() && ! doesSearchLoggingRequireProxy() )
		if( null!=optHanlder ) {
			if( optHanlder.doSearchLogging() && ! optHanlder.doesSearchLoggingRequireScraping() )
			{
				// This is one of two places we call this
				// The other place is if we setup a search engine passthrough
				// redirect
				optHanlder.addSearchTrackDirectLoggingCGIFields( newRequest );
			}
		}
		
		// Form the new URL
		// with the cgi vars, except ours
		String theURL = config.getSearchEngineURL();
		// Nice to have this later for debugging
		// though it is passed to NIEUtil via a function argument!
		newRequest.setBasicURL( theURL );
		
		if(debug) {
			staticDebugMsg( kFName,
				"Will attempt to query the host search engine with URL=\"" + theURL + "\" ..."
				);
			staticDebugMsg( kFName,
				"With fields:" + NIEUtil.NL
				+ newRequest.displayCGIFieldsIntoBuffer()
				);
		}

		// Show lots of details.  But don't bother if they don't care
		if( shouldDoTraceMsg(kFName) )
		{
			traceMsg( kFName, "Complete request is:" + NL +
				newRequest.displayRequestIntoBuffer() + NL
				);
		}

		// Query the main search engine
		String newContent = null;
		try
		{
		    staticDebugMsg( kFName,
		            "getSearchEngine().getSearchEngineMethod()="
		            // + config.getSearchEngine().getSearchEngineMethod()
		            + config.getSearchEngineMethod()
		            );

		    staticDebugMsg( kFName, "Setting auxio option to promote suspicious HTTP headers to Exception" );
		    newRequest.setPromoteSuspiciousHttpToException();
		    
		    // We may also need careful redirect handling
		    if( config.getSearchEngineUseCarefulRedirects() )
		    {
		    	staticDebugMsg( kFName, "Setting careful redirects." );
		    	newRequest.setUseCarefulRedirects();
		    	newRequest.setShouldLogRedirects();
		    	// Do not set follow redirect here
		    	// currently it's the default and nobody changes it
		    	// the only question is which method / careful
		    }
		    else {
		    	staticDebugMsg( kFName, "Leaving set for normal redirects." );		    	
		    }

		    // Call the NIE static method to fetch it
			// We're passing in the all important io stuff via newRequest
			// Which is where it will get the HTTP and CGI hashes
			newContent = NIEUtil.fetchURIContentsChar(
				theURL,
				null, null, null,
				newRequest,
				// config.getSearchEngine().getSearchEngineMethod().equalsIgnoreCase("POST")
				config.getSearchEngineMethod().equalsIgnoreCase("POST")
				);
		}
		catch( IOException ioe )
		{
			newContent = null;
			String msg = "Could not retrieve URL; caught an exception."
				+ " URL=\"" + theURL + "\"."
				+ " Exception=\"" + ioe + "\"."
				// + " Will setup passthrough redirect instead."
				;
			// staticErrorMsg( kFName, msg );
			throw new IOException( msg );

			/***
			// Log status info for SearchNames response
			if( fSNStatusCode == SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
			{
				fSNStatusCode = SearchTuningConfig.SN_ERROR_WITH_EXTERNAL_CALL;
				fSNStatusMsg = msg;
			}
			***/
		}

		// If an optional intermediate IO object was passed in
		// it will now have additional information about the results
		// such as content type / mime type and character encoding

		return newContent;

	}
	
	



	private void setupRequestEcho()
	{
		String info = fRequestInfo.displayRequestIntoBuffer();
		debugMsg( "setupRequestEcho", "Showing request: \r\n" + info );

		fResponseInfo.setContentType(
			AuxIOInfo.MIME_TYPE_TEXT
			);
		fResponseInfo.setContent( info );

		// Make a note that we did have success
		fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE );
	}






	// Soemtimes we want the system to just spit back some fixed text
	// Useful for testing with web browsers that are worried about security
	// For use with SN_CONTEXT_DEBUG_FIXED_HTML_ECHO
	private void setupFixedHTMLEcho( )
	{
		setupVariableHTMLEcho( FIXED_HTML_TO_ECHO );
	}

	// Soemtimes we want the system to just spit back some fixed text
	// Useful for testing with web browsers that are worried about security
	// For use with SN_CONTEXT_DEBUG_FIXED_HTML_ECHO
	private void setupVariableHTMLEcho( String inFullHTMLText )
	{
		final String kFName = "setupVariableHTMLEcho";

		traceMsg( kFName, "Returning HTML text."
			+ inFullHTMLText
			);
		fResponseInfo.setContentType(
			AuxIOInfo.MIME_TYPE_HTML
			);
		fResponseInfo.setContent( inFullHTMLText );
	}

	// Generate a response that says something is wrong
	private void setupTextErrorResponse( String inErrorMessage )
	{

		// IE doesn't bother to show the text of 500 level errors
		fResponseInfo.setHTTPResponseCode(
			AuxIOInfo.DEFAULT_HTTP_GENERAL_ERROR_CODE
			// AuxIOInfo.DEFAULT_HTTP_FATAL_ERROR_CODE
			);
		fResponseInfo.setContentType(
			AuxIOInfo.MIME_TYPE_TEXT
			);
		fResponseInfo.setContent( inErrorMessage );
	}

	// Generate a TEXT response that lets folks know what's going on
	private void setupTextResponse( String inMessage )
	{
		fResponseInfo.setHTTPResponseCode(
			AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE
			);
		fResponseInfo.setContentType(
			AuxIOInfo.MIME_TYPE_TEXT
			);
		fResponseInfo.setContent( inMessage );
	}


	// We are given a transaction to log directly, vs trying to automatically
	// log it during a proxy search
	private void setupDirectLogging()
	{
		final String kFName = "setupDirectLogging";

		// Make a note that, AFTER we respond, we are to log this
		// We want to espond quickly
		mDoDirectLogAferResponse = true;

		// When we log an event, they have also typically given
		// us a final URL to go to
		String destinationURL = getRequestObject().getScalarCGIFieldTrimOrNull(
			SearchLogger.DESTINATION_URL_CGI_FIELD
			);


		if( null != destinationURL )
		{

			fResponseInfo.setDesiredRedirectURL( destinationURL );
			// Be nice and also add in a message
			String content =
				"<html><head><title>Redirecting Logging Request</title></head><body>"
				+ "<h3>Redirecting Logging Request</h3>"
				+ "Your data logging request was recieved."
				+ "<p> Your web request is now being redirected to another URL."
				+ " Ideally this will happen automatically and you'll never see this message."
				+ "<p> However, if you do see this message, go head and click the link below;"
				+ " it will take you to the correct place."
				+ "<p> Go go "
				+ "<a href=\"" + destinationURL + "\">"
				+ destinationURL + "</a>"
				+ "</body></html>"
				;
			fResponseInfo.setContent( content );
		}
		else
		{
			// Prepare and display an immediate message, we don't make them
			// wait for the log action
			setupVariableHTMLEcho( DIRECT_LOG_REQUEST_FIXED_ACK_HTML );
			warningMsg( kFName,
				"No destination URL was given, so the user was presented with a fixed response."
				+ " Typically, after logging, the user would be directed to another page."
				);
		}

	}


	private static void ___Activity_Logging___(){}

	// In this case we don't prepare a response, but we do make note of
	// the fact that proxying should be done after we send a response
	// private void markProxyLogging()
	private void markPostSearchLogging( boolean inWasJustSnippetServe )
	{
		// Make a note that, AFTER we respond, we are to log this
		// We want to respond quickly
		// mDoProxyLogAferResponse = true;
		mDoSearchLoggingAferResponse = true;
		mWasJustSnippetServe = inWasJustSnippetServe;
	}


	// This method is for logging the CURRENT transaction info
	// after we have marked up a results list, etc.
	private void carryOutPostSearchLogging()
	{
		final String kFName = "carryOutPostSearchLogging";

		boolean result = false;
		try
		{
			result = getSearchLogger().logTransaction(
				fRequestInfo,           // Request
				fResponseInfo,          // Response
				SearchLogger.TRANS_TYPE_SEARCH,	// Transaction type
				getUserQuery(),         // Query
				fSNActionCode,          // int inSNActionCode,
				fSNActionCount,         // # of high level actions taken
				fSNActionItemCount,     // # of units of into sent
				fSNStatusCode,          // int inSNStatusCode,
				fSNStatusMsg,           // String inSNStatusMsg,
				false,                  // inFromDirectLogAction
				fDidASearchTermToSpecificURLRedirect, // Did we do a redirect
				mWasJustSnippetServe
				);
		}
		// catch( SearchLoggerException logE, SQLException sqlE )
		catch( Exception e )
		{
			errorMsg( kFName,
				"Unable to log this transaction. Exception: " + e
				);
		}
		if( ! result )
			errorMsg( kFName,
				"Transaction Logger did not return success code."
				);
	}
	// This means a CGI request has been recieved to log a PREVOIUS transaction
	// This is somehwhat unusual, probably only from Verity Search Script.
	// In that case, most of the fields come from the CGI variables, not our logic
	private void carryOutDirectLoggingAfterResponse()
	{
		final String kFName = "carryOutDirectLoggingAfterResponse";

		boolean result = false;
		try
		{
			result = getSearchLogger().logTransaction(
				fRequestInfo,           // Request
				null,                   // Response
				SearchLogger.TRANS_TYPE_UNKNOWN,	// Look at cgi posting instead
				getUserQuery(),         // Query
				-1,                     // int inSNActionCode,
				-1,                     // # of high level actions taken
				-1,                     // # of units of into sent
				-1,                     // int inSNStatusCode,
				null,                   // String inSNStatusMsg,
				true,                   // inFromDirectLogAction
				false,                   // Did we do a redirect
				false					// Did we serve up a snippet
				);
		}
				//	fSNActionCode,          // int inSNActionCode,
				//	fSNActionCount,         // # of high level actions taken
				//	fSNActionItemCount,     // # of units of into sent
				//	fSNStatusCode,          // int inSNStatusCode,
				//	fSNStatusMsg,           // String inSNStatusMsg,
		// catch( SearchLoggerException logE, SQLException sqlE )
		catch( Exception e )
		{
			errorMsg( kFName,
				"Unable to log this recieved transaction data. Exception: " + e
				);
		}
		if( ! result )
			errorMsg( kFName,
				"Transaction Logger did not return success code."
				);
	}


	private static void ___Handle_Admin_Commands___(){}
	/////////////////////////////////////////////////////////////////


	// We dispatch admin requests
	// We still rely on the main doProcessing to send the response
	private void doAdmin()
		throws SpuriousHTTPRequestException
	{
		final String kFName = "doAdmin";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// We start by assuming we will give a non-redirect response
		// We can always change our minds later!
		fResponseInfo.setHTTPResponseCode( AuxIOInfo.DEFAULT_HTTP_RESPONSE_CODE );

		// Get the context
		String lContextCode = determineAdminContext();

		// Take care of fail-safe conditions
		if( null==getMainConfig() ) {
			// There's only a few types we will handle
			if( lContextCode.equals( ADMIN_CONTEXT_SHOW_FULL_VERSION_INFO )
				|| lContextCode.equals( ADMIN_CONTEXT_PING )
				|| lContextCode.equals( ADMIN_CONTEXT_REFRESH )
				|| lContextCode.equals( ADMIN_CONTEXT_SHOW_MESSAGES )
				// we do NOT allow shutdown
				)
			{
				// do nothing, let them through
			}
			// Punt on everything else
			else {

				/***
				throw new SpuriousHTTPRequestException( kExTag +
					"Fail-safe pass-through mode does not handle \"" + lContextCode + "\" admin reauests (1)."
					);
				***/
				setupTextErrorResponse( HARD_FAILOVER_MSG );

				return;
			}

		}
		// else if( ! getMainConfig().getLicIsAllGood() ) {
		// }
		// ^^^ don't need to do this here, check in proxy search logic

		// There are a few admin commands that do not require a password
		if( // lContextCode.equals( ADMIN_CONTEXT_SHOW_COMPLETE_CONFIG )
			// ||
			lContextCode.equals( ADMIN_CONTEXT_SHOW_FULL_VERSION_INFO )
			|| lContextCode.equals( ADMIN_CONTEXT_PING )
			)
		{
			//	// Is it a specific command?
			//	if( lContextCode.equals( ADMIN_CONTEXT_SHOW_COMPLETE_CONFIG ) )
			//	{
			//		infoMsg( kFName,
			//			"Admin command: Show complete config (" + lContextCode + ")"
			//			);
			//		adminShowCompleteConfig( );
			//	}
			// Is it to show the version?
			// else
			if( lContextCode.equals( ADMIN_CONTEXT_SHOW_FULL_VERSION_INFO ) )
			{
				infoMsg( kFName,
					"Admin command: Show full version info (" + lContextCode + ")"
					);
				adminShowFullVersionInfo( );
			}
			// Else do a ping
			else
			{
				infoMsg( kFName,
					"Admin command: Ping / default (" + lContextCode + ")"
					);
				adminPing( );
			}
		}
		// For most others we require a password
		// There are a few more exception states
		// We will eventually check getCurrentPasswordLevel()
		else
		{

			// No password needed in case of NULL config
			// Special handling of pass-through mode
			if( null==fMainConfig ) {
				if( lContextCode.equals( ADMIN_CONTEXT_REFRESH ) )
				{
					infoMsg( kFName,
						"Admin command (fail-safe): Refresh configuration (" + lContextCode + ")"
						);
					adminRefreshConfig();
				}
				else if( lContextCode.equals( ADMIN_CONTEXT_SHOW_MESSAGES ) )
				{
					infoMsg( kFName,
						"Admin command (fail-safe): Show message log (" + lContextCode + ")"
						+ NL
						+ "INVALID CONFIGURATION - Unable to peform Admin functions (except refresh)"
						);
					adminShowMessages();
				}
				// Punt on everything else
				else {
					throw new SpuriousHTTPRequestException( kExTag +
						"Fail-safe pass-through mode does not handle \"" + lContextCode + "\" admin reauests (2)."
						);
				}
			}
			// Else normal, not fail-safe, not pass-through mode
			else
			{

				// Last two exception to security
				// 1: Reports handle their own security
				if( lContextCode.equals( ADMIN_CONTEXT_REPORT ) )
				{
					infoMsg( kFName,
						"Admin command: Report (" + lContextCode + ")"
						);
					adminReport();
				}
				// 2: Logins
				else if( lContextCode.equals( ADMIN_CONTEXT_MUST_LOGIN )
					|| lContextCode.equals( ADMIN_CONTEXT_DO_LOGIN )
				) {
					infoMsg( kFName,
						"Admin command: Login (" + lContextCode + ")"
						);
					adminLogin();
				}
				// NEED password from here on out
				// ==================================
				// Any other generic admin command besides report and public
				// requires a certain password level
				else {
	
					// if( ! getHasProperPassword() )
					if( getCurrentPasswordLevel() < REQUIRED_ADMIN_SECURITY_LEVEL )
					{
						String msg = "Error: " + kFName + NL
							+ "Administration action attempted without proper password authorization." + NL
							+ "Possible reasons you are seeing this error:" + NL
							+ "* No Admin password was set in the config file." + NL
							+ "  (there would be a warning about this in the logs)" + NL
							+ "* You did not pass in a password (or it was empty)" + NL
							+ "* Perhaps you misspelled the password field name on your form." + NL
							+ "* The password you submitted did not match." + NL
							+ "  (Passwords ARE case sensitive)" + NL
							;
							errorMsg( kFName, msg );
							setupTextErrorResponse( msg );
					}
					// Else we do have a good password
					else
					{
		
						if( lContextCode.equals( ADMIN_CONTEXT_SHUTDOWN ) )
						{
							infoMsg( kFName,
								"Admin command: Shutdown (" + lContextCode + ")"
								);
							adminShutdown();
						}
				//		else if( lContextCode.equals( ADMIN_CONTEXT_LIST_ALL_MAPPINGS ) )
				//		{
				//			_adminListAllMappings( );
				//		}
						// Is it another protocol that we recognize?
						else if( lContextCode.equals( ADMIN_CONTEXT_REFRESH ) )
						{
							infoMsg( kFName,
								"Admin command: Refresh configuration (" + lContextCode + ")"
								);
							adminRefreshConfig();
						}
						//	else if( lContextCode.equals( ADMIN_CONTEXT_REPORT ) )
						//	{
						//		infoMsg( kFName,
						//			"Admin command: Report (" + lContextCode + ")"
						//			);
						//		adminReport();
						//	}
						else if( lContextCode.equals( ADMIN_CONTEXT_SHOW_MESSAGES ) )
						{
							infoMsg( kFName,
								"Admin command: Show message log (" + lContextCode + ")"
								);
							adminShowMessages();
						}
						else if( lContextCode.equals( ADMIN_CONTEXT_RESAVE ) )
						{
							String msg =
								"You have requested a currently unimplemented ADMIN context." + NL
								+ "Context=\"" + lContextCode + "\"" + NL
								+ "Unable to process your ADMIN request." + NL
								;
							errorMsg( kFName, msg );
							setupTextErrorResponse( msg );
						}
						// Is it a specific command?
						else if( lContextCode.equals( ADMIN_CONTEXT_SHOW_COMPLETE_CONFIG ) )
						{
							infoMsg( kFName,
								"Admin command: Show complete config (" + lContextCode + ")"
								);
							adminShowCompleteConfig( );
						}
						// Else we don't recognize it at all
						else
						{
							String msg =
								"You have requested an unrecognized ADMIN context."
								+ " Context=\"" + lContextCode + "\""
								+ " Unable to process your ADMIN request."
								;
							errorMsg( kFName, msg );
							setupTextErrorResponse( msg );
						}
					}   // End else we do have a good password
	
				}	// Else this is a generic admin command

			}	// end else not in fail-safe mode

		}   // End else this is something other than the listing

	}



	private String determineAdminContext()
	{
		final String kFName = "determineAdminContext";

		if( fRequestInfo == null )
		{
			errorMsg( kFName, "No request to check, fRequestInfo was NULL."
				+ " Will return NULL context."
				);
			return null;
		}


		String outContext = null;

		String path = fRequestInfo.getLocalURLPath();
		// Allow for /ui/ or /admin/, /administration/
		if( null!=path && (
			startsWithLoginPath( path )
			// path.startsWith( UI_CONTEXT_CGI_PATH_PREFIX2_LOGIN )
			// || path.startsWith( UI_CONTEXT_CGI_PATH_PREFIX3_LOGIN )
			)
		) {
			outContext = ADMIN_CONTEXT_MUST_LOGIN;
		}
		else {

			// Lookup the admin context
			outContext = fRequestInfo.getScalarCGIFieldTrimOrNull(
				ADMIN_CONTEXT_REQUEST_FIELD
				);
	
			// If not null, lower case it
			if( outContext != null )
			{
				debugMsg( kFName,
					"Specific admin context given: \"" + outContext + "\""
					);
				outContext = outContext.toLowerCase();
			}
			// Or give it the default value
			else
			{
				debugMsg( kFName,
					"DEFAULT admin context prsumed: \"" + ADMIN_DEFAULT_CONTEXT + "\""
					);
				outContext = ADMIN_DEFAULT_CONTEXT;
			}

		}


		// Return the answer
		return outContext;

	}

	public static boolean startsWithLoginPath( String inPath ) {
		if( null==inPath )
			return false;
		inPath = inPath.toLowerCase();
		if( K_LOGIN_PATH_ALIASES.contains(inPath) )
			return true;

		// Normalize and try again
		// drop any #ref suffix
		int poundMarkAt = inPath.indexOf( '#' );
		if( poundMarkAt > 0 )
			inPath = inPath.substring( 0, poundMarkAt );
		// drop any CGI ?... stuff
		int questionMarkAt = inPath.indexOf( '?' );
		if( questionMarkAt > 0 )
			inPath = inPath.substring( 0, questionMarkAt );

		// One more chance
		if( null!=inPath && K_LOGIN_PATH_ALIASES.contains(inPath) )
			return true;

		return false;
	}

	private void adminShowCompleteConfig()
	{
		// TODO: this needs to call a rerport if using a database to store maps

		final String kFName = "adminShowCompleteConfig";

		// Get the jdom or jdomhelper element
		Document mainDoc = getConfigDoc();

		// The name of the system XSLT template we will call
		String templateName = "admin_showall";

		// The resulting document
		Document formattedDoc = null;
		// Try to format it
		try
		{
			formattedDoc = JDOMHelper.xsltDocToDoc(
				mainDoc,
				templateName,
				null, true
				);
		}
		catch (JDOMHelperException e)
		{

			String msg = "SnRequestHandler:adminShowCompleteConfig:"
				+ " Got exception while formatting XML into HTML."
				+ " Exception=\"" + e + "\""
				;
			errorMsg( kFName, msg );
			setupTextErrorResponse( msg );
			return;
		}

		// Now convert to text
		// Preseve linefeeds in text nodes by setting PrettyFormat
		String outBuffer = JDOMHelper.JDOMToString( formattedDoc, true );

		// And set the content and type
		fResponseInfo.setContentType(
			AuxIOInfo.MIME_TYPE_HTML
			);
		fResponseInfo.setContent( outBuffer );

		// And we're doine
	}

	private void adminShowFullVersionInfo()
	{
		String msg =
			"Version and Configuration Info:" + NL
			+ NL
			+ getMainApplication().getDetailedVersionBanner();
		setupTextResponse( msg );
	}


	private void adminLogin()
	{
		final String kFName = "adminLogin";

		// Special handling of pass-through mode
		if( null==getMainConfig() ) {
			errorMsg( kFName, "UI commands not available in fail-safe pass-through mode; returning reminder to user." );
			setupTextErrorResponse( HARD_FAILOVER_MSG );
			return;
		}
		if( ! getMainConfig().getLicIsAllGood() ) {
			errorMsg( kFName, "UI commands not available in l"
				+ "ic f" + "ail pass-through mode; returning reminder to user." );
			setupTextErrorResponse( LIC_FAILOVER_MSG );
			return;
		}

		nie.webui.UIRequestDispatcher disp =
			// getMainApplication().getSearchTuningConfig().getUIRequestDispatcher();
			getMainConfig().getUIRequestDispatcher();

		if( null != disp )
		{
			String outBuffer = null;
			try
			{
				String outDoc = disp.dispatch( fRequestInfo, fResponseInfo, disp.LOGIN_SCREEN_NAME );
				fResponseInfo.setContent( outDoc );
				// ??? Also sets mime type and bin content in response object
			}
			// catch( ReportException re )
			// catch( Exception re )
			catch( Throwable re )
			{
				String msg = "UI Error: " + re;
				errorMsg( kFName, msg );
				setupTextErrorResponse( msg );
			}
		}
		else
		{
			setupTextErrorResponse(
				"UI module is unavalable."
				);
		}

	}





	private void adminPing()
	{
		String msg =
			"Ping Confirmation: NIE SearchNames is responding to requests." + NL
			+ NL
			+ "Explanation:" + NL
			+ NL
			+ "An Administration command of PING has been received," + NL
			+ "so this message is being returned to answer it." + NL
			+ "This is also the default response for the Admin context." + NL
			+ NL
			+ "Were you EXPECTING SOMETHING ELSE?" + NL
			+ NL
			+ "If you were expecting some other response from the server" + NL
			+ "and are seeing this PING response instead, you probably have" + NL
			+ "a mistake or typo in your CGI arguments." + NL
			+ "In that case, please consult the documentation." + NL
			+ "PING is the DEFAULT Admin response, so it can be returned" + NL
			+ "in response to some malformed Admin commands." + NL
			+ NL
			+ "Another useful Admin / Status command is:" + NL
			+ NL
			+ "http://your_server:port/?"
			+ NIE_CONTEXT_REQUEST_FIELD + "=" + SN_CONTEXT_ADMIN
			+ "&"
			+ ADMIN_CONTEXT_REQUEST_FIELD + "=" +ADMIN_CONTEXT_SHOW_COMPLETE_CONFIG + NL
			;
		setupTextResponse( msg );
	}


	private void adminRefreshConfig()
	{
		boolean results = getMainApplication().refreshConfig();
		String msg = null;
		if( results )
		{
			msg = "Confirmation:" + NL
			+ NL
			+ "An Administration command to REFRESH has been"
			+ " received and processed." + NL
			+ "Your configuration data has been reread and reloaded"
			+ " and is in effect now."
			;
		}
		else
		{
			msg = "Error:" + NL
			+ NL
			+ "An Administration command to REFRESH has been"
			+ " received but was not completed." + NL
			+ "Apparently some type of error was encountered." + NL
			+ NL
			+ "Please check the SearchTrack error logs for more details."
			;
		}

		setupTextResponse( msg );
	}


	private void adminShutdown()
	{
		setShouldStopNow();
		String msg =
			"Confirmation:" + NL
			+ NL
			+ "An Administration command to SHUTDOWN has been"
			+ " received and processed." + NL
//			+ "It may take a few seconds to complete." + NL
//			+ NL
//			+ "If it does not seem to take effect" + NL
//			+ "you can also try clicking the Refresh Button in your browser." + NL
//			+ NL
//			+ "Additional Information:" + NL
//			+ NL
//			+ "If you click Refresh and get NO response, or a browser error," + NL
//			+ "that probably means you have already SUCCESSFULLY shut it down," + NL
//			+ "and therefore it is no longer able to talk to your browser;" + NL
//			+ "in that case this is GOOD NEWS." + NL
//			+ NL
//			+ "If you are able to monitor the running processes on your server," + NL
//			+ "or have access to the console window where the process was started in," + NL
//			+ "you should see the Java process exit." + NL
//			+ NL
//			+ "NIE is planning to address this \"shutdown/refresh\" issue in a future release;" + NL
//			+ "it is caused by a \"threading race condition\" and will be fixed." + NL
//			+ "We apologize for any inconvenience." + NL
			;
		setupTextResponse( msg );
	}


	private void adminReport()
	{

		final String kFName = "adminReport";

		nie.sr2.ReportDispatcher disp =
			getMainConfig().getReportDispatcher();
			// getMainApplication().getSearchTuningConfig().getReportDispatcher();

		if( null != disp )
		{
			String outBuffer = null;
			try
			{
				outBuffer = disp.dispatch(
					fRequestInfo, fResponseInfo
					);
				// And set the content and type
				fResponseInfo.setContentType(
					AuxIOInfo.MIME_TYPE_HTML
					);
				fResponseInfo.setContent( outBuffer );
			}
			// catch( ReportException re )
			catch( Exception re )
			{
				String msg = "Report Error: " + re;
				errorMsg( kFName, msg );
				// setupTextErrorResponse( msg );
				setupTextErrorResponse(
					"Error running report; please see log file for details."
					+ NIEUtil.NL
					+ "(Maybe check your password if not accessing a public report?)"
					);
			}
		}
		else
		{
			setupTextErrorResponse(
				"Reporting module is unavalable."
				+ " Please check configuration."
				);
		}

		// And we're doine
	}


	private void adminShowMessages()
	{

		List messages = getRunLogObject().getMessages();

		Element outElem = new Element("html");
		if( null == messages || messages.size() < 1 )
		{
			outElem.addContent(
				"No run log messages to display."
				);
		}
		else
		{
			outElem.addContent(
				"Displaying " + messages.size() + " run log messages."
				+ " (MOST RECENT FIRST)"
				);
			for( Iterator it = messages.iterator(); it.hasNext() ; )
			{
				Element hr = new Element( "hr" );
				hr.setAttribute( "width", "93%" );
				hr.setAttribute( "size", "1" );
				hr.setAttribute( "noshade", "1" );
				outElem.addContent( hr );

				String msg = (String) it.next();
				outElem.addContent( msg );
			}

		}

		String content = JDOMHelper.JDOMToString( outElem, true );

		if( null != content )
		{
			fResponseInfo.setContentType(
				AuxIOInfo.MIME_TYPE_HTML
				);
			fResponseInfo.setContent( content );
		}
		else
		{
			setupTextErrorResponse( "Unable to convert messages into HTML" );
		}

	}

	private void processUIRequest()
	{
		final String kFName = "processUIRequest";

		// Special handling of pass-through mode
		if( null==getMainConfig() ) {
			errorMsg( kFName, "UI commands not available in fail-safe pass-through mode; returning reminder to user." );
			setupTextErrorResponse( HARD_FAILOVER_MSG );
			return;
		}
		if( ! getMainConfig().getLicIsAllGood() ) {
			errorMsg( kFName, "UI commands not available in l"
				+ "ic f" + "ail pass-through mode; returning reminder to user." );
			setupTextErrorResponse( LIC_FAILOVER_MSG );
			return;
		}

		/***
		if( ! getHasProperPassword() )
		{
			String msg = "Error: " + kFName + NL
				+ "Administration-UI action attempted without proper password authorization." + NL
				+ "Possible reasons you are seeing this error:" + NL
				+ "* No Admin password was set in the config file." + NL
				+ "  (there would be a warning about this in the logs)" + NL
				+ "* You did not pass in a password (or it was empty)" + NL
				+ "* Perhaps you misspelled the password field name on your form." + NL
				+ "* The password you submitted did not match." + NL
				+ "  (Passwords ARE case sensitive)" + NL
				;
			errorMsg( kFName, msg );
			setupTextErrorResponse( msg );
		}
		// Else we do have a good password
		else
		{
		***/
			// Password levels are now checked by the dispatcher

			nie.webui.UIRequestDispatcher disp =
				// getMainApplication().getSearchTuningConfig().getUIRequestDispatcher();
				getMainConfig().getUIRequestDispatcher();
	
			if( null != disp )
			{
				String outBuffer = null;
				try
				{
					String outDoc = disp.dispatch( fRequestInfo, fResponseInfo );
					fResponseInfo.setContent( outDoc );
					// ??? Also sets mime type and bin content in response object
				}
				// catch( ReportException re )
				// catch( Exception re )
				catch( Throwable re )
				{
					String msg = "UI Error: " + re;
					errorMsg( kFName, msg );
					setupTextErrorResponse( msg );
				}
			}
			else
			{
				setupTextErrorResponse(
					"UI module is unavalable."
					);
			}

		// }
		// And we're done
	}

	private void processLuceneSearchRequest()
	{
		final String kFName = "processLuceneSearchRequest";

		// Special handling of pass-through mode
		if( null==getMainConfig() ) {
			errorMsg( kFName, "Lucene commands not available in fail-safe pass-through mode; returning reminder to user." );
			setupTextErrorResponse( HARD_FAILOVER_MSG );
			return;
		}
		if( ! getMainConfig().getLicIsAllGood() ) {
			errorMsg( kFName, "Lucene commands not available in l"
				+ "ic f" + "ail pass-through mode; returning reminder to user." );
			setupTextErrorResponse( LIC_FAILOVER_MSG );
			return;
		}

		LuceneRequestDispatcher disp =
			// getMainApplication().getSearchTuningConfig().getUIRequestDispatcher();
			getMainConfig().getLuceneSearchDispatcher();

		if( null != disp )
		{
			String outBuffer = null;
			try
			{
				String outDoc = disp.dispatch( fRequestInfo, fResponseInfo );
				fResponseInfo.setContent( outDoc );
				// ??? Also sets mime type and bin content in response object
			}
			catch( Throwable re )
			{
				String msg = "UI Error: " + re;
				errorMsg( kFName, msg );
				setupTextErrorResponse( msg );
			}
		}
		else
		{
			setupTextErrorResponse(
				"Lucene module is unavalable."
				);
		}

	}



	private void _adminListAllMappings()
	{
	}

	// private void setupDisplayOfFullMap()
	private void _setupDisplayOfMapForTerm( String inTerm, int inOrdinal )
	{

//
//setupTextErrorResponse
//return;
//
//		// Lookup the term in the map
//
//		// If we didn't find it
//			// Setup a message
//			// return
//
//		// Pull from hash and cast to list
//
//		// Get the specific numbered term
//			// If out of range
//				// Setup an error message
//				// return
//			// Else it's in range
//				// Grab from list and cast
//
//		// Get the jdom or jdomhelper element
//
//
//					Document tmpDoc = lXMLStats.xsltElementToDoc(
//						fXsltPath,
//						inParamsHash
//						);
//
//		// Preseve linefeeds in text nodes by setting
//		// PrettyFormat
//		outBuffer = JDOMHelper.JDOMToString( tmpDoc, true );
//
//		// Set the content
//

	}


	void /*boolean*/ _getHasNoActionIndicator()
	{
//		List
	}


	private static void ___Supporting_Logic___() {}
	////////////////////////////////////////////////////////////////////

	// The caller should check for the status of fSnippetToSend
	// rather than relying on a null vs non-null return from here
	private String _generateSnippet_v1()
	{
		final String kFName = "_generateSnippet_v1";

		// Set the global flag that we don't have anything
		fSnippetToSend = false;

		// We go though a few extra steps here to give a more helpful
		// response, in HTML comment form.

		// Even though the later routines check for the query,
		// we like to check for it now, to give better debug messages
		String query = getUserQuery();

		// If no query, just return
		// No warning needed, be we should emit a comment at least
		if( query == null )
		{
			String tmpMsg =
				"<!--"
				+ " Info: NIE SearchTrack: no search terms found in request"
				+ " for Webmaster Suggests or Alternatives snippet suggestions."
				+ "-->"
				;
			// We did NOT find anything interesting
			// Redundant, but makes it clear
			fSnippetToSend = false;
			// We still return a string in case they want to send back
			// some debug page or something, normally they would just redirect
			return tmpMsg;
		}


		// Check for no query, or no matching query
		// This also checks the hash
		if( ! getHasAnyMatchingTerms() )
		{
			String tmpMsg =
				"<!--"
				+ " Info: NIE SearchTrack: Search term(s) not mapped."
				+ " Terms = '" + query + "'"
				+ "-->"
				;
			// We did NOT find anything interesting
			// Redundant, but makes it clear
			fSnippetToSend = false;
			// We still return a string in case they want to send back
			// some debug page or something, normally they would just redirect
			return tmpMsg;
		}

		// The list of valid, matching wm suggests records, IF any found
		// turns list of SnURLRecords
		List suggestions = getValidWebmasterSuggestsRecords();
		// And also alternative spellings
		// Returns list of strings
		List alternatives = getValidAlternateSuggestionRecords();

		// If there aren't any, still just do a passthrough
		// Usually there should be by this point in the code
		if( ( suggestions == null || suggestions.size() < 1 )
			&& ( alternatives == null || alternatives.size() < 1 )
			)
		{
			String tmpMsg =
				"<!--"
				+ " Info: NIE SearchNames: Search term(s) not mapped"
				+ " for Webmaster Suggests or Alternatives snippet suggestions,"
				+ " even though it may be present in the map for other uses."
				+ " Terms = '" + query + "'"
				+ "-->"
				;
			// We did NOT find anything interesting
			// Redundant, but makes it clear
			fSnippetToSend = false;
			// We still return a string in case they want to send back
			// some debug page or something, normally they would just redirect
			return tmpMsg;
		}


		// Increment counters, etc
		// We need to track that we actually did something

		// Tabulate suggestions
		if( suggestions.size() > 0 )
		{
			// Increment the counter, need to increment the other one in
			fSNActionCount++;
			// Each URL is counted
			fSNActionItemCount += suggestions.size();

			// We need to handle if they are as-yet undefined
			fSNActionCount = fSNActionCount <= 0 ? 1 : fSNActionCount+1;
			if( fSNActionItemCount <= 0 )
				fSNActionItemCount = suggestions.size();
			else
				fSNActionItemCount += suggestions.size();

			// Make a note of what we did
			if( fSNActionCode == SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
				fSNActionCode = SearchTuningConfig.SN_ACTION_CODE_SUGGEST;
		}

		// Tabulate alternatives
		if( alternatives.size() > 0 )
		{
			// Increment the counter, need to increment the other one in
			// Each URL is counted
			// We need to handle if they are as-yet undefined
			fSNActionCount = fSNActionCount <= 0 ? 1 : fSNActionCount+1;
			if( fSNActionItemCount <= 0 )
				fSNActionItemCount = alternatives.size();
			else
				fSNActionItemCount += alternatives.size();

			// Make a note of what we did
			if( fSNActionCode == SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
				fSNActionCode = SearchTuningConfig.SN_ACTION_CODE_ALT;
		}

		// So we know we have good records
		// build the complete buffer
		StringBuffer outBuff = new StringBuffer();

		// Add the Webmaster Suggests suggestions, if appropriate
		String wmsSnippet = null;
		if( suggestions != null && suggestions.size() > 0 )
		{
			// Add all the suggestions
			wmsSnippet = _formatWMSuggestionsToSnippet_v1( suggestions, query );
			if( null==wmsSnippet )
				errorMsg( kFName,
					"Got back unexpected null wms snippet."
					+ " Had " + suggestions.size() + " urls to suggest."
					);
		}
		// Or make a note that we didn't have WM suggestions
		else
		{
			String tmpMsg =
			    NIEUtil.NL +
				"<!--"
				+ " Info: NIE SearchTrack: Search terms has no"
				+ " Webmaster Suggests suggestions." // ;"
				// + " however it may have an alternative spelling suggestion."
			    + NIEUtil.NL
				+ "Term = '" + query + "'"
				+ "-->"
				;
			outBuff.append( tmpMsg );
		}

		// Add the Alternative Spelling suggestions, if appropriate
		String altSnippet = null;
		if( alternatives != null && alternatives.size() > 0 )
		{
			altSnippet = _formatAlternateSuggestionsToSnippet_v1( alternatives, query );
			if( null==altSnippet )
				errorMsg( kFName,
					"Got back unexpected null alt-terms snippet."
					+ " Had " + alternatives.size() + " alt terms to suggest."
					);
		}
		// Or make a note that we didn't have WM suggestions
		else
		{
			String tmpMsg =
			    NIEUtil.NL +
				"<!--"
				+ " Info: NIE SearchTrack: Search term has no"
				+ " Alternative suggestions." // ;"
				// + " however it may have Webmaster Suggests suggestion."
			    + NIEUtil.NL
				+ "Term = '" + query + "'"
				+ "-->"
				;
			outBuff.append( tmpMsg );
		}

		// Add snippets, if we got any
		// If we got both, wrap them in table
		if( null!=wmsSnippet || null!=altSnippet ) {
			boolean hasBoth = null!=wmsSnippet && null!=altSnippet;
			outBuff.append( NIEUtil.NL );
			if( hasBoth ) {
				outBuff.append( "<table width=\"100%\"><tr><td align=\"left\">");
				outBuff.append( NIEUtil.NL );
			}
			// Add WMS
			if( null!=wmsSnippet ) {
				outBuff.append( wmsSnippet );
				outBuff.append( NIEUtil.NL );
			}
			if( hasBoth ) {
				outBuff.append( "</td></tr><tr><td align=\"left\">");
				outBuff.append( NIEUtil.NL );
			}
			// Add Alt terms
			if( null!=altSnippet ) {
				outBuff.append( altSnippet );
				outBuff.append( NIEUtil.NL );
			}
			if( hasBoth ) {
				outBuff.append( "</td></tr></table>");
				outBuff.append( NIEUtil.NL );
			}
			// Add an extra <br>
			// outBuff.append( "<br />" );
			// ^^^ can now be added by admin in config file
			fSnippetToSend = true;
		}

		// Return the results
		return new String( outBuff );
	}

	// The caller should check for the status of fSnippetToSend
	// rather than relying on a null vs non-null return from here
	// TODO: Convert to building with elements
	// Also use pseudo xpath ypath stuff in JDOMHelper
	// like findOrCreateElementByPath
	// See also configurator2 and collections for how they use it
	private Element generateSnippet()
	{
		final String kFName = "generateSnippet";

		// Set the global flag that we don't have anything
		fSnippetToSend = false;

		Element outElem = new Element( "span" );

		// We go though a few extra steps here to give a more helpful
		// response, in HTML comment form.

		// Even though the later routines check for the query,
		// we like to check for it now, to give better debug messages
		String query = getUserQuery();

		// If no query, just return
		// No warning needed, be we should emit a comment at least
		if( query == null )
		{
			String tmpMsg =
				// "<!--" +
				" Info: NIE SearchTrack: no search terms found in request"
				+ " for Webmaster Suggests or Alternatives snippet suggestions."
				// + "-->"
				;
			// We did NOT find anything interesting
			// Redundant, but makes it clear
			fSnippetToSend = false;
			// We still return a string in case they want to send back
			// some debug page or something, normally they would just redirect
			// return tmpMsg;
			outElem.addContent( new Comment(tmpMsg) );
			return outElem;
		}


		// Check for no query, or no matching query
		// This also checks the hash
		if( ! getHasAnyMatchingTerms() )
		{
			String tmpMsg =
				// "<!--" +
				" Info: NIE SearchTrack: Search term(s) not mapped."
				+ " Terms = '" + query + "'"
				// + "-->"
				;
			// We did NOT find anything interesting
			// Redundant, but makes it clear
			fSnippetToSend = false;
			// We still return a string in case they want to send back
			// some debug page or something, normally they would just redirect
			// return tmpMsg;
			outElem.addContent( new Comment(tmpMsg) );
			return outElem;
		}

		// The list of valid, matching wm suggests records, IF any found
		// turns list of SnURLRecords
		List suggestions = getValidWebmasterSuggestsRecords();
		// And also alternative spellings
		// Returns list of strings
		List alternatives = getValidAlternateSuggestionRecords();

		// If there aren't any, still just do a passthrough
		// Usually there should be by this point in the code
		if( ( suggestions == null || suggestions.size() < 1 )
			&& ( alternatives == null || alternatives.size() < 1 )
			)
		{
			String tmpMsg =
				// "<!--" +
				" Info: NIE SearchNames: Search term(s) not mapped"
				+ " for Webmaster Suggests or Alternatives snippet suggestions,"
				+ " even though it may be present in the map for other uses."
				+ " Terms = '" + query + "'"
				// + "-->"
				;
			// We did NOT find anything interesting
			// Redundant, but makes it clear
			fSnippetToSend = false;
			// We still return a string in case they want to send back
			// some debug page or something, normally they would just redirect
			// return tmpMsg;
			outElem.addContent( new Comment(tmpMsg) );
			return outElem;
		}


		// Increment counters, etc
		// We need to track that we actually did something

		// Tabulate suggestions
		if( suggestions.size() > 0 )
		{
			// Increment the counter, need to increment the other one in
			fSNActionCount++;
			// Each URL is counted
			fSNActionItemCount += suggestions.size();

			// We need to handle if they are as-yet undefined
			fSNActionCount = fSNActionCount <= 0 ? 1 : fSNActionCount+1;
			if( fSNActionItemCount <= 0 )
				fSNActionItemCount = suggestions.size();
			else
				fSNActionItemCount += suggestions.size();

			// Make a note of what we did
			if( fSNActionCode == SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
				fSNActionCode = SearchTuningConfig.SN_ACTION_CODE_SUGGEST;
		}

		// Tabulate alternatives
		if( alternatives.size() > 0 )
		{
			// Increment the counter, need to increment the other one in
			// Each URL is counted
			// We need to handle if they are as-yet undefined
			fSNActionCount = fSNActionCount <= 0 ? 1 : fSNActionCount+1;
			if( fSNActionItemCount <= 0 )
				fSNActionItemCount = alternatives.size();
			else
				fSNActionItemCount += alternatives.size();

			// Make a note of what we did
			if( fSNActionCode == SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
				fSNActionCode = SearchTuningConfig.SN_ACTION_CODE_ALT;
		}

		// So we know we have good records
		// build the complete buffer
		StringBuffer outBuff = new StringBuffer();

		// Add the Webmaster Suggests suggestions, if appropriate
		/*String*/ Element wmsSnippet = null;
		if( suggestions != null && suggestions.size() > 0 )
		{
			// Add all the suggestions
			wmsSnippet = formatWMSuggestionsToSnippet( suggestions, query );
			if( null==wmsSnippet )
				errorMsg( kFName,
					"Got back unexpected null wms snippet."
					+ " Had " + suggestions.size() + " urls to suggest."
					);
		}
		// Or make a note that we didn't have WM suggestions
		else
		{
			String tmpMsg =
			    // NIEUtil.NL +
				// "<!--" +
				" Info: NIE SearchTrack: Search terms has no"
				+ " Webmaster Suggests suggestions." // ;"
				// + " however it may have an alternative spelling suggestion."
			    + NIEUtil.NL
				+ "Term = '" + query + "'"
				// + "-->"
				;
			// outBuff.append( tmpMsg );
			outElem.addContent( new Comment(tmpMsg) );
		}

		// Add the Alternative Spelling suggestions, if appropriate
		/*String*/ Element altSnippet = null;
		if( alternatives != null && alternatives.size() > 0 )
		{
			altSnippet = formatAlternateSuggestionsToSnippet( alternatives, query );
			if( null==altSnippet )
				errorMsg( kFName,
					"Got back unexpected null alt-terms snippet."
					+ " Had " + alternatives.size() + " alt terms to suggest."
					);
		}
		// Or make a note that we didn't have WM suggestions
		else
		{
			String tmpMsg =
			    NIEUtil.NL +
				// "<!--" +
				" Info: NIE SearchTrack: Search term has no"
				+ " Alternative suggestions." // ;"
				// + " however it may have Webmaster Suggests suggestion."
			    + NIEUtil.NL
				+ "Term = '" + query + "'"
				// + "-->"
				;
			// outBuff.append( tmpMsg );
			outElem.addContent( new Comment(tmpMsg) );
		}

		// Add snippets, if we got any
		// If we got both, wrap them in table
		if( null!=wmsSnippet || null!=altSnippet ) {
			boolean hasBoth = null!=wmsSnippet && null!=altSnippet;
			if( null != wmsSnippet )
				outElem.addContent( wmsSnippet );
			if( hasBoth )
				outElem.addContent( new Element("br") );
			if( null != altSnippet )
				outElem.addContent( altSnippet );
			/***
			outBuff.append( NIEUtil.NL );
			if( hasBoth ) {
				outBuff.append( "<table width=\"100%\"><tr><td align=\"left\">");
				outBuff.append( NIEUtil.NL );
			}
			// Add WMS
			if( null!=wmsSnippet ) {
				outBuff.append( wmsSnippet );
				outBuff.append( NIEUtil.NL );
			}
			if( hasBoth ) {
				outBuff.append( "</td></tr><tr><td align=\"left\">");
				outBuff.append( NIEUtil.NL );
			}
			// Add Alt terms
			if( null!=altSnippet ) {
				outBuff.append( altSnippet );
				outBuff.append( NIEUtil.NL );
			}
			if( hasBoth ) {
				outBuff.append( "</td></tr></table>");
				outBuff.append( NIEUtil.NL );
			}
			***/
			// Add an extra <br>
			// outBuff.append( "<br />" );
			// ^^^ can now be added by admin in config file
			fSnippetToSend = true;
		}

		// Return the results
		// return new String( outBuff );
		return outElem;
	}

	// check for <BASE HREF=URL> in document
	// Add one if there's not already one there
	// WE ALWAYS RETURN A STRING (unless we were passed in a null source doc)
	// 7/29/02 move tags From right before closing tag to right after opening
	// tags, so ALL items including CSS in header are handled
	// 10/16/02 add tags even to mangled HTML doc headings, per Culver/Google
	// called by setupProxySearch()
	private String markupWithBaseHref( String inSourceDoc )
	{
		final String kFName = "markupWithBaseHref";

		// I think we want NON case sensitive searches
		final boolean isCasen = false;

		// For now, force the darn thing in at the top if we can't find
		// the "right" place for it
		final boolean okToForceAtTop = true;

		if( inSourceDoc == null )
		{
			errorMsg( kFName, "Null input document, returning null." );
			return null;
		}

		// replace getSearchEngineURL() with configurable option AND call it only once here
		String baseUrl = getBaseURL();
		if( null == baseUrl )
		{
			warningMsg( kFName,
					"NULL baseUrl, returning original document."
					);
			return inSourceDoc;
		}
		if( baseUrl.equalsIgnoreCase(SearchEngineConfig.NO_URL_MARKER) )
		{
			debugMsg( kFName,
					"Base URL is a hyphen, which is a special marker, so returning original document."
					);
			return inSourceDoc;
		}

		debugMsg( kFName,
			"Processing string with " + inSourceDoc.length() + " characters."
			);

		String searchDoc = inSourceDoc;
		if( ! isCasen )
		{
			searchDoc = searchDoc.toLowerCase();
			debugMsg( kFName,
				"Not case sensitive, so normalizing search doc to lower case."
				+ " (the original document will be left in tact, of course)"
				);
		}
		else
		{
			debugMsg( kFName,
				"IS case sensitive, so NOT normalizing search doc to lower case."
				);
		}

		// Step 1: Check if it's already there
		int baseAt0 = searchDoc.indexOf( "<base" );
		if( baseAt0 >= 0 )
		{
			debugMsg( kFName,
				"This document already has a <base ...> tag"
				+ ", at OFFSET = " + baseAt0 + "."
				+ " Returning original, unmodified document."
				);
			// They have a base tag, bail
			// Todo: could do additioanl context checking
			// Todo: could allow context flags
			return inSourceDoc;
		}

		// Step 2: Find a place to insert it

		// This is actually 2 steps itself:
		// Step 2a: Find the start of the tag
		// Step 2b: Find the closing angle bracket for that tag
		// Remember, sometimes folks have extra attributes in tags

		// Ideally, we'd put it right at the start of the header section
		// int insertBeforeOffset = searchDoc.indexOf( "</head" );
		int startOffset = searchDoc.indexOf( "<head" );
		int insertAfterOffset = -1;
		// But if there's no head, put it after opening html tag
		if( startOffset < 0 )
		{
			debugMsg( kFName,
				"Unable to find opening head tag, will look for opening body tag."
				);
			// insertBeforeOffset = searchDoc.indexOf( "<body" );
			startOffset = searchDoc.indexOf( "<html" );
		}

		// Find the closing angle bracket
		// We will catch the warnings later
		if( startOffset >= 0 )
		{
			insertAfterOffset = searchDoc.indexOf( '>', startOffset );
		}

		// Start preparing the answer
		StringBuffer outDoc = new StringBuffer();



		// If we still can't find it, don't do anything
		if( insertAfterOffset < 0 )
		{
			if( ! okToForceAtTop )
			{
				errorMsg( kFName, "Unable to find insertion point."
					+ " Returning original, unmodified document."
					+ " This may be OK; it just means that we didn't add"
					+ " a <base> tag to the HTML header section."
					+ " Perhaps there was no HTML or head tag at all."
					+ " This might impact relative images, links or Javascript."
					);
				return inSourceDoc;
			}
			// Else go ahead and jam it in at the top

			// Add the base in at the top
			// outDoc.append( "<BASE HREF=\"" );
			outDoc.append( "<base href=\"" );
			// NOTE: 1 of 2
			// outDoc.append( getSearchEngineURL() );
			outDoc.append( baseUrl );			// outDoc.append( "\">" );
			// make XHTML compatible
			outDoc.append( "\" />" );
			// And add the doc
			outDoc.append( inSourceDoc );

			// Don't flood them with messages, but let them know
			infoMsg( kFName,
				"Added <BASE> tag to very start of page"
				+ " because couldn't find proper HTML headers."
				);
		}
		else    // Else we DID find the proper place
		{
			debugMsg( kFName,
				"Will add base tag just after offset = " + insertAfterOffset + "."
				);

			// Add the first part of the document
			if( insertAfterOffset >= 0 )
				outDoc.append( inSourceDoc.substring( 0, insertAfterOffset+1) );

			// Now add in the base ref of the search engine
			// outDoc.append( "<BASE HREF=\"" );
			outDoc.append( "<base href=\"" );
			// NOTE: 2 of 2
			// outDoc.append(  getSearchEngineURL() );
			outDoc.append( baseUrl );
			// outDoc.append( "\">" );
			// make XHTML compatible
			outDoc.append( "\" />" );

			// Now add in the second part of the document
			if( insertAfterOffset < inSourceDoc.length()-1 )
				outDoc.append( inSourceDoc.substring( insertAfterOffset+1 ) );
		}

		// Now return the results
		return new String( outDoc );

	}

	// We ALWAYS return a string, even if we are given a null
	// called by setupProxySearch()
	private String markupWithSnippet(
		String inSourceDoc, String inSnippet
		)
	{
		final String kFName = "markupWithSnippet";

		// For not we want case insensitive searching
		// final boolean caseSen = false;

		// Get the patterns
		//boolean isToTrim =
		//	getSearchEngine().getIsSuggestionMarkerToBeTrimmed();
		List patterns = getSearchEngine().getSuggestionMarkerLiteralText();

		if( inSourceDoc==null || inSnippet==null || patterns==null
			|| patterns.size()<1
			)
		{
			String msg = "One of the inputs was null.";
			msg = msg + "inSourceDoc is "
				+ ( inSourceDoc==null ? "NULL." : "not null." );
			msg = msg + " inSnippet is "
				+ ( inSnippet==null ? "NULL." : "not null." );
			msg = msg + " patterns: "
				+ ( patterns==null ? "NULL." : " has " + patterns.size() + " elements" );
			msg = msg + " Returning original document as passed in.";
			errorMsg( kFName, msg );

			// Log status info for SearchNames response
			if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
			{
				fSNStatusCode = SearchTuningConfig.SN_ERROR_PARSING_PAGE;
				fSNStatusMsg = msg;
			}

			if( inSourceDoc != null )
				return inSourceDoc;
			else
				return "";
		}

		// Get some other flags from the Search Engine config
		boolean goesAfter =
			getSearchEngine().getIsSuggestionMarkerNewTextInsertAfter();
//statusMsg( kFName, "goesAfter=" + goesAfter );
		// statusMsg( kFName, "goesAfter=" + goesAfter );
		boolean doesReplace = getSearchEngine().getIsSuggestionMarkerReplaced();
		boolean isCasen = getSearchEngine().getIsSuggestionMarkerCaseSensitive();

		String prefixText = getSearchEngine().getSnippetPrefixText();
		String suffixText = getSearchEngine().getSnippetSuffixText();
		if( null!=prefixText || null!=suffixText ) {
			prefixText = (null!=prefixText) ? prefixText : "" ;
			suffixText = (null!=suffixText) ? suffixText : "" ;
			inSnippet = prefixText + inSnippet + suffixText;
		}


		debugMsg( kFName,
			"Will process input doc with " + inSourceDoc.length() + " chars"
			+ ", Snippet with " + inSnippet.length() + " chars"
			+ ", and " + patterns.size() + " pattern(s)."
			+ " Options:"
			+ " goesAfter=" + goesAfter
			+ " doesReplace=" + doesReplace
			+ " isCasen=" + isCasen
			);
			// + " isToTrim=" + isToTrim

		// Do the markup
		String outDoc = NIEUtil.markupStringWtihString(
			inSourceDoc, inSnippet, patterns,
			goesAfter, doesReplace, isCasen
			);

		// And we're done
		if( outDoc != null )
		{
			debugMsg( kFName,
				"Substitution was done, returning new doc with " + outDoc.length() + " chars"
				);
			return outDoc;
		}
		else
		{
			String tmpMsg = "NO substitutions made"
				+ ", returning original doc with " + inSourceDoc.length()
				+ " chars"
				;

			errorMsg( kFName, tmpMsg );

			// Log status info for SearchNames response
			if( fSNStatusCode == SearchTuningConfig.SN_STATUS_UNDEFINED )
			{
				fSNStatusCode = SearchTuningConfig.SN_ERROR_PARSING_PAGE;
				fSNStatusMsg = tmpMsg;
			}

			return inSourceDoc;
		}
	}

	private String _markupWithSnippetOLD(
		String inSourceDoc, String inSnippet
		)
	{
//		final String kFName = "markupWithSnippet";
//
//		// For not we want case insensitive searching
//		final boolean caseSen = false;
//
//		String literalPattern = getSearchEngine().getWmsInsertAfterLiteralText();
//
//		if( inSourceDoc==null || inSnippet==null || literalPattern==null )
//		{
//			String msg = "One of the inputs was null.";
//			msg = msg + "inSourceDoc is "
//				+ ( inSourceDoc==null ? "NULL." : "not null." );
//			msg = msg + " inSnippet is "
//				+ ( inSnippet==null ? "NULL." : "not null." );
//			msg = msg + " literalPattern is "
//				+ ( literalPattern==null ? "NULL." : "not null." );
//			msg = msg + " Returning original document as passed in.";
//			errorMsg( kFName, msg );
//			return inSourceDoc;
//		}
//
//		String searchDoc = inSourceDoc;
//
//		// If NOT case sensitive, then normalize searching strings to
//		// all lower case
//		if( ! caseSen )
//		{
//			searchDoc = searchDoc.toLowerCase();
//			literalPattern = literalPattern.toLowerCase();
//		}
//
//		int patternAt = searchDoc.indexOf( literalPattern );
//		if( patternAt < 0 )
//		{
//			String msg = "Didn't find pattern to use for markup."
//				+ " pattern=\"" + literalPattern + "\"."
//				+ " Returning original, unmodified document."
//				;
//			warningMsg( kFName, msg );
//			return inSourceDoc;
//		}
//
//		// Where the split is
//		int insertAt = patternAt + literalPattern.length();
//
//		// The answer
//		StringBuffer outDoc = new StringBuffer();
//		// Grab everything to the left
//		outDoc.append( inSourceDoc.substring( 0, insertAt ) );
//		// Add the snippet
//		outDoc.append( inSnippet );
//		// Grab everything to the right
//		if( insertAt < inSourceDoc.length() )
//			outDoc.append( inSourceDoc.substring( insertAt ) );
//
//		// And we're done
//		return new String( outDoc );
		return null;
	}

private String markupResultsListDocLinksIfNeeded(
		String inSourceDoc
		)
{
	final String kFName = "markupResultsListDocLinksIfNeeded";

	boolean debug = shouldDoDebugMsg( kFName );
	boolean trace = shouldDoTraceMsg( kFName );

	if( null==inSourceDoc )
	{
		errorMsg( kFName, "Null source doc sent in, returning null." );
		return null;
	}

	// How much markup do we want to do?
	String policy = getSearchEngine().getResultsDocLinksTweakPolicy();

	if(debug) debugMsg( kFName, "Start, policy='" + policy + "'" );

	// If no policy, bail out
	// In this case NULL should not be returned, we should get the default
	// if they had nothing to say
	// If it is, 
	if( null==policy
		|| SearchEngineConfig.TWEAK_DISABLED.equalsIgnoreCase(policy)
		|| SearchEngineConfig.TWEAK_LOG_ONLY.equalsIgnoreCase(policy)
	) {
		if(debug) debugMsg( kFName, "Disabled, exiting routine, policy='" + policy + "'" );
		return inSourceDoc;
	}

	// The Main Document Iterator
	HtmlTagIterator it = new HtmlTagIterator(
		inSourceDoc, HTML_ANCHOR_TAG_START, HTML_HREF_ATTR_START
		);

	// Set the starting gate / fence, if any was requested
	// Here we check for the starting point
	// So for "between", it's when to start looking
	// and for "after", it's also when to start looking
	// "start looking after you see this pattern"
	if( SearchEngineConfig.TWEAK_CHOICE_AFTER_MARKER.equalsIgnoreCase(policy)
		|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
		)
	{
		String origPattern = NIEUtil.trimmedStringOrNull(
			getSearchEngine().getResultsFormsTweakArg1()
			);
		// Set the pattern and complain if it fails
		// This will also check and report nulls, etc.
		// It also normalized (case wise) as needed
		if( ! it.setStartPattern(origPattern) )
		{
			errorMsg( kFName, "Error setting start pattern '" + origPattern + "', returning original document." );
			return inSourceDoc;
		}
	}

	// Set the finish line / fence, if requested
	// Check for the ending point
	// For "between", it's when to stop looking
	// and for "before", it's when to stop looking
	// "only look for tags until you see this pattern; check the part before the pattern"
	if( SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy)
		|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
		)
	{
		// If they JUST wanted a finish line, technically that would be the
		// first parameter, but if they were switching back and forth it'd
		// be easy to get confused.

		// "between": arg1=start, arg2=stop
		// "before": arg1=stop, arg2=ignored
		// BUT if you changed from "between" to "before"
		// you might still have "stop" pattern in arg2
		// So we will also accept:
		// "before": arg1=empty, arg2=stop

		// if between, then should be the second one, otherwise the first one
		String origPattern = SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
			? NIEUtil.trimmedStringOrNull( getSearchEngine().getResultsFormsTweakArg2() )
			: NIEUtil.trimmedStringOrNull( getSearchEngine().getResultsFormsTweakArg1() )
			;

		// Give them a second chance if it's just "before", they might have put it
		// in either box
		if( null==origPattern && SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy) )
			origPattern = NIEUtil.trimmedStringOrNull( getSearchEngine().getResultsFormsTweakArg2() );

		// Set the pattern and complain if it fails
		// This will also check and report nulls, etc.
		// It also normalized (case wise) as needed
		if( ! it.setEndPattern(origPattern) )
		{
			errorMsg( kFName, "Error setting end pattern '" +  origPattern + "', returning original document." );
			return inSourceDoc;
		}
	}
	
	if(debug) debugMsg( kFName, "Scan fences = "
		+ it.getStartDocBoundary() + '/' + it.getEndDocBoundary() + "'"
		+ " (reminder: -1  = no end limit)"
		);

	// AND we will be modifying the buffers, possibly changing their
	// length, as we do substitutions.

	int resultLinkOnPageCounter = 0;

	// For each form tag
	while( it.next() )
	{
		String origUrl = it.getOriginalAttrValueOrNull();

		String normUrl = it.getNormalizedAttrValueOrNull();
		
		// decode escaped ampersands
		normUrl = NIEUtil.decodeUrlFromInsideHtmlOrNull( normUrl );

		// No... ??? DECODE percentages, etc.

		
		// won't be null in this implementation
		// attrVal = NIEUtil.trimmedStringOrNull( attrVal );
		// if( null==attrVal )
		if(debug) debugMsg( kFName, "Top of main loop, attrVal='" + normUrl + "'" );

		// Assess this URL, see if it's worth messing with
		boolean doSubst = false;
		// Which type of link / what to do with it...
		String substType = SearchEngineConfig.DOC_LINK_TYPE_GENERIC;

		// We may want to do all of them
		// OR if we were inside of a fence and got this far
		// then we'll do it as well
		if( SearchEngineConfig.TWEAK_CHOICE_ALL_ITEMS.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_AFTER_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
			)
		{
			doSubst = true;
			if(debug) debugMsg( kFName, "Configured to match all in-fence-range values" );
		}
		// Search engine URL?
		else if( SearchEngineConfig.TWEAK_CHOICE_SEARCH_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull( getSearchEngineURL() );
			if( null!=compUrl && normUrl.startsWith(compUrl) )
			{
				doSubst = true;
				if(debug) debugMsg( kFName, "Matches normalized search engine URL '" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized search engine URL '" + compUrl + "'" );				
			}
		}
		// A specific URL prefix?
		else if( SearchEngineConfig.TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull(
				getSearchEngine().getResultsFormsTweakArg1()
				);
			if( null!=compUrl && normUrl.startsWith(compUrl) )
			{
				doSubst = true;
				if(debug) debugMsg( kFName, "Matches normalized URL prefix '" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized URL prefix '" + compUrl + "'" );					
			}
		}
		// EXCLUDE a specific URL prefix?
		else if( SearchEngineConfig.TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull(
				getSearchEngine().getResultsFormsTweakArg1()
				);
			if( null!=compUrl && ! normUrl.startsWith(compUrl) )
			{
				doSubst = true;	
				if(debug) debugMsg( kFName, "Matches normalized Exclude prefix, so will NOT tweak, pattern='" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized Exclude prefix, so WILL tweak, pattern='" + compUrl + "'" );					
			}
		}

		// TODO:
		// They may want us to record a specific field in the original URL
		// vs the entire URL

		// If we're supposed to do the substitution
		if( doSubst && null!=origUrl )
		{

			// String newValue = getSearchNamesURL();
			// String newValue = "http://foobar.com/index.html";
			// Figure out what type
			/***
			String compUrl = NIEUtil.trimmedLowerStringOrNull( getSearchEngineURL() );
			if( null!=compUrl && normUrl.startsWith(compUrl) )
			{
				substType = SearchEngineConfig.DOC_LINK_TYPE_SEARCH_ENGINE_GENERAL;		
			}
			// Relative values are navigators
			else if( normUrl.indexOf("://") < 0 )
			{
				substType = SearchEngineConfig.DOC_LINK_TYPE_SITE_NAV;
			}
			else
			{
				substType = SearchEngineConfig.DOC_LINK_TYPE_RESULT;
			}
			***/
			
			MatchRule rule = getSearchEngine().findMatchingRuleOrNull( normUrl );
			if( null!=rule )
			{
				substType = rule.getAssignToType();

				/***
				if( rule.getIsCgiPageField() )
				{
					cgiPageField = rule.getCgiFieldToMatch();
					rule.getDocsFirstOrdinal()
					rule.getDocsPerPage()
				}
				***/
			}
			else {
				substType = SearchEngineConfig.DOC_LINK_TYPE_GENERIC;
			}

			// We may know this
			int cgiRawPagingNum = -1;
			int pageNum = -1;
			String cgiPageField = getSearchEngine().getPagingFieldOrNull();
			if( null!=cgiPageField )
			{
				cgiRawPagingNum = fRequestInfo.getIntFromCGIField( cgiPageField, -1 );
				if( cgiRawPagingNum < 0 )
				{
					pageNum = 1;
				}
				else {
					// Does the CGI parameter directly work in page units
					// Usually NO
					if( getSearchEngine().getShouldCalcWithPageNum() )
					{
						int correction = (1 - getSearchEngine().getDocsFirstOrdinal());
						pageNum = cgiRawPagingNum + correction;
					}
					// Else most engines work on
					// Document offset
					else
					{
						// Normally:
						// [0 2 3456789 9][10 11 12 ... 19][20...29]
						//  ^page 1        ^page 2          ^page 3
						// If it were 1 based, then it would be:
						// [1 2 3456789 10][11 12 ... 20][21 ... 30]
						//  ^page 1         ^page 2       ^page 3
					
						int firstDoc = getSearchEngine().getDocsFirstOrdinal();
						int delta = (1-firstDoc);
						int perPage = getSearchEngine().getDocsPerPage();

						// The raw count plus base PLUS get to the LAST doc on that page
						int docsTotal = cgiRawPagingNum + delta + (perPage-1);
						int pages = docsTotal / perPage;
						int checkDocsTotal = pages * docsTotal;
						if( docsTotal != checkDocsTotal )
						{
							warningMsg( kFName, "Error calculating page number from CGI offset"
								+ " Raw cgi=" + cgiRawPagingNum
								+ ", doc base=" + firstDoc
								+ ", per page=" + perPage
								+ ", (A) 'filled out' docs total=" + docsTotal
								+ ", which gives pages=" + pages
								+ ", (B) reverse doc check=" + checkDocsTotal
								+ ": A not equal B"
								);
							pageNum = -1;
						}
						else {
							pageNum = pages;
						}
					}
				}
			}

statusMsg( kFName, NIEUtil.NL + "URL type '" + substType + "' for url '" + origUrl + "'" );



			String linkContext = SN_CONTEXT_DIRECT_LOG_TRANSACTION;
			int transType = SearchLogger.TRANS_TYPE_GENERIC_CLICK;

			// in flux: Usually we take the URL they were going to click on and set it
			// in flux: up as a target
			// in flux: HOWEVER, sometimes we leave the args in place and
			// in flux: change the base URL instead
			// in flux: boolean encodedUrl = true;
			// in flux: Search engine links

			if( substType.startsWith(SearchEngineConfig.DOC_LINK_TYPE_SEARCH_PREFIX) )
			{
				transType = SearchLogger.TRANS_TYPE_SEARCH_NAV_CLICK;
				linkContext = SN_CONTEXT_CLICKTHROUGH_PROXY;
				// encodedUrl = false;
			}
			// Links to specific documents in the results list
			else if( substType.startsWith(SearchEngineConfig.DOC_LINK_TYPE_RESULT_PREFIX) )
			{
				transType = SearchLogger.TRANS_TYPE_LOG_DOC_CLICK;
				resultLinkOnPageCounter++;
			}
			// Otherwise we can only assume generic

			
			LoggingLink link = null;
			String newValue = null;
			try {
				link = new LoggingLink( getMainConfig(), fRequestInfo );
				link.setContext( linkContext );
				link.setTransactionType( transType );
				link.setDestinationURL( origUrl );

				// Which page we think we are on in the results
				if( pageNum > 0 )
					link.setResultsPageNumber( pageNum );

				// The count on THIS page and in the entire results
				if( SearchLogger.TRANS_TYPE_LOG_DOC_CLICK==transType )
				{
					// On THIS page of the results
					link.setResultsOrdinalOnThisPage( resultLinkOnPageCounter );
					// And on the full results list, even if they've paged
					// On the first page, it's the same
					if( pageNum <= 1 )
					{
						link.setResultsOrdinalInAllResults( resultLinkOnPageCounter );
					}
					// On subsequent pages we need to add in the
					// count for the previous pages as well
					else {
						int perPage = getSearchEngine().getDocsPerPage();
						int full = (pageNum-1)*perPage + resultLinkOnPageCounter;
						link.setResultsOrdinalInAllResults( full );
					}
				}

				// Get the new link
				newValue = link.generateURL();
				// newValue = NIEUtil.htmlEscapeString( newValue, true );
				newValue = NIEUtil.encodeUrlForInsideHtmlOrNull( newValue );
				// Or NIEUtil.cgiDecodeVarsBuffer
				// Or URLEncoder.decode( origKey, AuxIOInfo.CHAR_ENCODING_UTF8 );
				// LATER ON
				// NIEUtil.strongUrlPercentEncoderOrNull(
				// NIEUtil.htmlEscapeString(
				//	String inString, boolean inDoWarnings )

				// And store it
				if(debug) debugMsg( kFName, "WILL replace with new value='" + newValue + "'" );					
				if( ! it.replaceAttrValue(newValue) )
				{
					errorMsg( kFName, "Failed to replace with new attribute value, ignoring." );
				}			
			}
			catch( SearchLoggerException sle )
			{
				errorMsg( kFName, "Error adding click through links: " + sle );
			}
		} // end if supposed to do substitution
		// Else 2 cases...
		else if( null==origUrl )
		{
			warningMsg( kFName, "Encountered null original URL, will not substituee." );
		}
		// Else not substition
		else {
			if(debug) debugMsg( kFName, "Not doing substituion, leaving old value='" + origUrl + "'" );					
		}	

	}	// End for each tag in document
	
	return it.getDocument();
}


private String markupResultsListSearchFormsIfNeeded(
	String inSourceDoc
	)
{
	final String kFName = "markupResultsListSearchFormsIfNeeded";

	boolean debug = shouldDoDebugMsg( kFName );
	boolean trace = shouldDoTraceMsg( kFName );

	if( null==inSourceDoc )
	{
		errorMsg( kFName, "Null source doc sent in, returning null." );
		return null;
	}

	// How much markup do we want to do?
	String policy = getSearchEngine().getResultsFormsTweakPolicy();

	if(debug) debugMsg( kFName, "Start, policy='" + policy + "'" );

	// If no policy, bail out
	// In this case NULL should not be returned, we should get the default
	// if they had nothing to say
	// If it is, 
	if( null==policy
			|| SearchEngineConfig.TWEAK_DISABLED.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_LOG_ONLY.equalsIgnoreCase(policy)
	) {
		if(debug) debugMsg( kFName, "Disabled, exiting routine, policy='" + policy + "'" );
		return inSourceDoc;
	}

	// The Main Document Iterator
	HtmlTagIterator it = new HtmlTagIterator(
		inSourceDoc, HTML_FORM_TAG_START, HTML_ACTION_ATTR_START
		);

	// Set the starting gate / fence, if any was requested
	// Here we check for the starting point
	// So for "between", it's when to start looking
	// and for "after", it's also when to start looking
	// "start looking after you see this pattern"
	if( SearchEngineConfig.TWEAK_CHOICE_AFTER_MARKER.equalsIgnoreCase(policy)
		|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
		)
	{
		String origPattern = NIEUtil.trimmedStringOrNull(
			getSearchEngine().getResultsFormsTweakArg1()
			);
		// Set the pattern and complain if it fails
		// This will also check and report nulls, etc.
		// It also normalized (case wise) as needed
		if( ! it.setStartPattern(origPattern) )
		{
			errorMsg( kFName, "Error setting start pattern '" +  origPattern + "', returning original document." );
			return inSourceDoc;
		}
	}

	// Set the finish line / fence, if requested
	// Check for the ending point
	// For "between", it's when to stop looking
	// and for "before", it's when to stop looking
	// "only look for tags until you see this pattern; check the part before the pattern"
	if( SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy)
		|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
		)
	{
		// If they JUST wanted a finish line, technically that would be the
		// first parameter, but if they were switching back and forth it'd
		// be easy to get confused.

		// "between": arg1=start, arg2=stop
		// "before": arg1=stop, arg2=ignored
		// BUT if you changed from "between" to "before"
		// you might still have "stop" pattern in arg2
		// So we will also accept:
		// "before": arg1=empty, arg2=stop

		// if between, then should be the second one, otherwise the first one
		String origPattern = SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
			? NIEUtil.trimmedStringOrNull( getSearchEngine().getResultsFormsTweakArg2() )
			: NIEUtil.trimmedStringOrNull( getSearchEngine().getResultsFormsTweakArg1() )
			;

		// Give them a second chance if it's just "before", they might have put it
		// in either box
		if( null==origPattern && SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy) )
			origPattern = NIEUtil.trimmedStringOrNull( getSearchEngine().getResultsFormsTweakArg2() );

		// Set the pattern and complain if it fails
		// This will also check and report nulls, etc.
		// It also normalized (case wise) as needed
		if( ! it.setEndPattern(origPattern) )
		{
			errorMsg( kFName, "Error setting end pattern '" +  origPattern + "', returning original document." );
			return inSourceDoc;
		}
	}
	
	if(debug) debugMsg( kFName, "Scan fences = "
		+ it.getStartDocBoundary() + '/' + it.getEndDocBoundary() + "'"
		+ " (reminder: -1  = no end limit)"
		);

	// AND we will be modifying the buffers, possibly changing their
	// length, as we do substitutions.

	// For each form tag
	while( it.next() )
	{
		if(debug) debugMsg( kFName, "Top of main loop." );

		String attrVal = it.getNormalizedAttrValueOrNull();
		// won't be null in this implementation
		// attrVal = NIEUtil.trimmedStringOrNull( attrVal );
		// if( null==attrVal )

		// Assess this URL, see if it's worth messing with
		boolean doSubst = false;

		// We may want to do all of them
		// OR if we were inside of a fence and got this far
		// then we'll do it as well
		if( SearchEngineConfig.TWEAK_CHOICE_ALL_ITEMS.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_AFTER_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
			)
		{
			doSubst = true;
			if(debug) debugMsg( kFName, "Configured to match all in-fence-range values" );
		}
		// Search engine URL?
		else if( SearchEngineConfig.TWEAK_CHOICE_SEARCH_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull( getSearchEngineURL() );
			if( null!=compUrl && attrVal.startsWith(compUrl) )
			{
				doSubst = true;
				if(debug) debugMsg( kFName, "Matches normalized search engine URL '" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized search engine URL '" + compUrl + "'" );				
			}
		}
		// A specific URL prefix?
		else if( SearchEngineConfig.TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull(
				getSearchEngine().getResultsFormsTweakArg1()
				);
			if( null!=compUrl && attrVal.startsWith(compUrl) )
			{
				doSubst = true;
				if(debug) debugMsg( kFName, "Matches normalized URL prefix '" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized URL prefix '" + compUrl + "'" );					
			}
		}
		// EXCLUDE a specific URL prefix?
		else if( SearchEngineConfig.TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull(
				getSearchEngine().getResultsFormsTweakArg1()
				);
			if( null!=compUrl && ! attrVal.startsWith(compUrl) )
			{
				doSubst = true;	
				if(debug) debugMsg( kFName, "Matches normalized Exclude prefix, so will NOT tweak, pattern='" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized Exclude prefix, so WILL tweak, pattern='" + compUrl + "'" );					
			}
		}

		// If we're supposed to do the substitution
		if( doSubst )
		{

			String newValue = getSearchNamesURL();
			if(debug) debugMsg( kFName, "WILL replace with new value='" + newValue + "'" );					
			if( ! it.replaceAttrValue(newValue) )
			{
				errorMsg( kFName, "Failed to replace with new attribute value, ignoring." );
			}			

		}
		// Else not substition
		else {
			if(debug) debugMsg( kFName, "Not doing substituion, leaving old value='" + attrVal + "'" );					

		}	

	}	// End for each tag in document
	
	return it.getDocument();
}


private String _markupResultsListSearchFormsIfNeeded_V1(
	String inSourceDoc
	)
{
	final String kFName = "_markupResultsListSearchFormsIfNeeded_V1";

	boolean debug = shouldDoDebugMsg( kFName );
	boolean trace = shouldDoTraceMsg( kFName );

	if( null==inSourceDoc )
	{
		errorMsg( kFName, "Null source doc sent in, returning null." );
		return null;
	}

	// How much markup do we want to do?
	String policy = getSearchEngine().getResultsFormsTweakPolicy();

	if(debug) debugMsg( kFName, "Start, policy='" + policy + "'" );

	// If no policy, bail out
	// In this case NULL should not be returned, we should get the default
	// if they had nothing to say
	// If it is, 
	if( null==policy || SearchEngineConfig.TWEAK_DISABLED.equalsIgnoreCase(policy) )
	{
		if(debug) debugMsg( kFName, "Disabled, exiting routine, policy='" + policy + "'" );
		return inSourceDoc;
	}

	// Our working buffer
	StringBuffer buff = new StringBuffer( inSourceDoc );
	// Our searchable buffer, normalized to lower case
	StringBuffer buffNorm = new StringBuffer( inSourceDoc.toLowerCase() );
	// !!!! AND THESE MUST BE KEPT IN SYNC
	// So any change made to buff must also be done to buffNorm

	// The bounds that we will check
	// TODO: add in bounds checking
	int startAt = 0;
	// stopAt will work like Java, where it's a zero based offset
	// that is ONE PAST where you want to stop, so that
	// stop-start = length
	// Only active if > 0
	int stopAt = -1;
	// HOWEVER
	// pointers/offsets for the beginnings and ends of specific tags
	// with names including "begin" and "end"
	// below are on the actual first and last character offsets

	// Set the starting gate / fence, if any was requested
	// Here we check for the starting point
	// So for "between", it's when to start looking
	// and for "after", it's also when to start looking
	// "start looking after you see this pattern"
	if( SearchEngineConfig.TWEAK_CHOICE_AFTER_MARKER.equalsIgnoreCase(policy)
		|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
		)
	{
		String origPattern = NIEUtil.trimmedStringOrNull(
			getSearchEngine().getResultsFormsTweakArg1()
			);
		if( null==origPattern )
		{
			errorMsg( kFName, "No start pattern given, returning original document." );
			return inSourceDoc;
		}
		String normPattern = origPattern.toLowerCase();
		int patternAt = buffNorm.indexOf( normPattern );
		if( patternAt < 0 )
		{
			errorMsg( kFName, "Start pattern not found, returning original document."
				+ " pattern='" + origPattern + "'"
				);
			return inSourceDoc;				
		}
		// startAt = patternAt + origPattern.length();
		// The pattern could be the first instance, so start there
		startAt = patternAt;
	}


	// Set the finish line / fence, if requested
	// Check for the ending point
	// For "between", it's when to stop looking
	// and for "before", it's when to stop looking
	// "only look for tags until you see this pattern; check the part before the pattern"
	if( SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy)
		|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
		)
	{
		// If they JUST wanted a finish line, technically that would be the
		// first parameter, but if they were switching back and forth it'd
		// be easy to get confused.

		// "between": arg1=start, arg2=stop
		// "before": arg1=stop, arg2=ignored
		// BUT if you changed from "between" to "before"
		// you might still have "stop" pattern in arg2
		// So we will also accept:
		// "before": arg1=empty, arg2=stop

		// if between, then should be the second one, otherwise the first one
		String origPattern = SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
			? NIEUtil.trimmedStringOrNull( getSearchEngine().getResultsFormsTweakArg2() )
			: NIEUtil.trimmedStringOrNull( getSearchEngine().getResultsFormsTweakArg1() )
			;

		// Give them a second chance if it's just "before", they might have put it
		// in either box
		if( null==origPattern && SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy) )
			origPattern = NIEUtil.trimmedStringOrNull( getSearchEngine().getResultsFormsTweakArg2() );
			
		if( null==origPattern )
		{
			errorMsg( kFName, "No end pattern given, returning original document." );
			return inSourceDoc;
		}
		String normPattern = origPattern.toLowerCase();
		int patternAt = buffNorm.indexOf( normPattern );
		if( patternAt < 0 )
		{
			errorMsg( kFName, "End pattern not found, returning original document."
				+ " pattern='" + origPattern + "'"
				);
			return inSourceDoc;				
		}
		// stopAt = patternAt;
		// The pattern could be the last instances, so allow for it
		stopAt = patternAt + origPattern.length();

	}
	
	if(debug) debugMsg( kFName, "Scan fences = " + startAt + '/' + stopAt + "'" );

	// AND we will be modifying the buffers, possibly changing their
	// length, as we do substitutions.

	// For each form tag
	while( true )
	{
		if(debug) debugMsg( kFName, "Top of main loop." );

		// Sanity check
		if( buff.length() != buffNorm.length() )
		{
			errorMsg( kFName, "Buffers out of sync, lenghts don't match"
				+ ", buff.length=" + buff.length()
				+ ", buffNorm.length=" + buffNorm.length()
				);
			break;
		}
		// Always do bounds checking at the start
		if( startAt >= buffNorm.length() || (stopAt>0 && startAt >= stopAt ) )
		{
			if(debug) debugMsg( kFName, "Past end of buffer"
				+ ", buff.length=" + buff.length()
				+ ", startAt=" + startAt
				+ ", stopAt=" + stopAt
				);
			break;				
		}

		// TODO: doens't known to skip text inside comments or Java Script
		int formTagBeginsAt = buffNorm.indexOf( HTML_FORM_TAG_START, startAt );
		if( formTagBeginsAt < 0 )
		{
			if(debug) debugMsg( kFName, "No more start-of-tag(s) found, done." );
			break;
		}
		if(debug) debugMsg( kFName, "Tag start at " + formTagBeginsAt );

		// TODO: doens't known to skip text inside quotes, comments or Java Script
		int formTagEndsAt = buffNorm.indexOf( HTML_TAG_END, formTagBeginsAt + HTML_FORM_TAG_START.length() );
		if( formTagEndsAt < 0 )
		{
			errorMsg( kFName, "Couldn't find ending form tag, no more forms will be tweaked"
				+ ", form started at offset " + formTagBeginsAt
				);
			break;
		}
		if(debug) debugMsg( kFName, "Tag end at " + formTagEndsAt );

		int actionAttrStartsAt = buffNorm.indexOf( HTML_ACTION_ATTR_START, formTagBeginsAt + HTML_FORM_TAG_START.length() );
		if( actionAttrStartsAt < 0 || actionAttrStartsAt > formTagEndsAt )
		{
			if(debug) debugMsg( kFName, "Attr name not found within tag bounds " + formTagBeginsAt + " to " + formTagEndsAt + ", offset=" + actionAttrStartsAt );
			startAt = formTagEndsAt + HTML_TAG_END.length();
			continue;
		}
		if(debug) debugMsg( kFName, "Attr name starts at " + actionAttrStartsAt );

		// Now we need to find the string delimiters and what they enclose
		String delim = "";
		boolean haveSeenFirstDelim = false;
		int attrValueStartsAt = -1;
		int attrValueEndsAt = -1;
		// Move through the buffer
		for( int delimCheck = actionAttrStartsAt + HTML_ACTION_ATTR_START.length();
			delimCheck < formTagEndsAt;
			delimCheck++
			)
		{
			if(trace) traceMsg( kFName, "Top of inner loop, delimCheck=" + delimCheck );

			// Get the characters one at a time
			// We use strings because they can vary in length
			String s = buffNorm.substring( delimCheck, delimCheck+1 );
			// If not inside the value yet
			if( ! haveSeenFirstDelim )
			{
				// Skip over leading spaces
				if( s.equals(" ") || s.equals("\t")
						|| s.equals("\r") || s.equals("\n")
				) {
					// move to the next char in THIS tag span
					continue;
				}
				// quotes or apostrophies
				else if( s.equals("\"") || s.equals("'") )
				{
					delim = s;
					haveSeenFirstDelim = true;
					// Starts at the next character
					attrValueStartsAt = delimCheck + 1;
					if(debug) debugMsg( kFName, "Attr value start delim='" + delim + "' at " + delimCheck );
				}
				// It's an unquoted string
				else
				{
					// Space is a sentinal for any whitespace
					// or the end of the tag
					delim = " ";
					haveSeenFirstDelim = true;
					// Starts HERE
					attrValueStartsAt = delimCheck;						
					if(debug) debugMsg( kFName, "Unquoted attr value starting at " + delimCheck );
				}						
			}
			// Else we're in the value
			else
			{
				// Handle white space
				if( s.equals(" ") || s.equals("\t")
						|| s.equals("\r") || s.equals("\n")
				) {
					// If unquoted string, this is past the end
					if( delim.equals(" ") )
					{
						// It actually stopped on the PREVIOUS character
						attrValueEndsAt = delimCheck - 1;
						if(debug) debugMsg( kFName, "Unquoted attr value terminating space at " + delimCheck );
						break;
					}
					// Else we're still crusing along
					// reading whitespace inside a quoted value
					// Odd for a URL...
					else {
						// move to the next char in THIS tag span
						continue;
					}
				}
				// quotes or apostrophies
				else if( s.equals(delim) )
				{
					// It actually stopped on the PREVIOUS character
					attrValueEndsAt = delimCheck - 1;
					if(debug) debugMsg( kFName, "Attr value closing delim='" + s + "' at " + delimCheck );
					break;
				}
				// Else it's some other regular character
				else
				{
					// Keep pushing the end along
					// though will probably be caught by other
					// logic as well
					attrValueEndsAt = delimCheck;
				}
			}	// End else we're in a value
		}	// End for each char in tag range

		if(debug) debugMsg( kFName, "Out of inner loop"
			+ ", prelim attr value starts/ends = "
			+ attrValueStartsAt + '/' + attrValueEndsAt );

		// Final catch up and sanity checks
		if( attrValueStartsAt >= 0 )
		{
			if( attrValueEndsAt < 0 )
			{
				attrValueEndsAt = formTagEndsAt - 1;
				if(debug) debugMsg( kFName, "Correcting attrValueEndsAt to " + attrValueEndsAt );
			}
			if( attrValueEndsAt < attrValueStartsAt )
			{
				warningMsg( kFName, "Trouble parsing attribute value"
					+ ", doc length=" + buffNorm.length()
					+ "Offsets: fences(+)=" + startAt + '/' + stopAt
					+ ", formTagBounds[]=" + formTagBeginsAt + '/' + formTagEndsAt
					+ ", attrNameBegin=" + actionAttrStartsAt
					+ ", attrValueBounds[]=" + attrValueStartsAt + '/' + attrValueEndsAt
					);
				// Move past the end of the Form tag
				startAt = formTagEndsAt + HTML_TAG_END.length();
				continue;
			}
			// Else we're OK so far, keep going
		}
		// Else no valid attr value
		else {
			if(debug) debugMsg( kFName, "No start of attribute value found"
					+ ", doc length=" + buffNorm.length()
					+ "Offsets: fences(+)=" + startAt + '/' + stopAt
					+ ", formTagBounds[]=" + formTagBeginsAt + '/' + formTagEndsAt
					+ ", attrNameBegin=" + actionAttrStartsAt
					+ ", attrValueBounds[]=" + attrValueStartsAt + '/' + attrValueEndsAt
					);
			// Move past the end of the Form tag
			startAt = formTagEndsAt + HTML_TAG_END.length();
			continue;
		}

		// OK, we have a valid value!
		// Note: our end pointers are inclusive
		String attrVal = buffNorm.substring( attrValueStartsAt, attrValueEndsAt+1 );
		attrVal = NIEUtil.trimmedStringOrNull( attrVal );
		if( null==attrVal )
		{
			if(debug) debugMsg( kFName, "Empty attribute value found"
					+ ", doc length=" + buffNorm.length()
					+ "Offsets: fences(+)=" + startAt + '/' + stopAt
					+ ", formTagBounds[]=" + formTagBeginsAt + '/' + formTagEndsAt
					+ ", attrNameBegin=" + actionAttrStartsAt
					+ ", attrValueBounds[]=" + attrValueStartsAt + '/' + attrValueEndsAt
					);
			// Move past the end of the Form tag
			startAt = formTagEndsAt + HTML_TAG_END.length();
			continue;				
		}

		if(debug) debugMsg( kFName, "Current attrVal='" + attrVal + "'" );

		// Assess this URL, see if it's worth messing with
		boolean doSubst = false;

		// We may want to do all of them
		// OR if we were inside of a fence and got this far
		// then we'll do it as well
		if( SearchEngineConfig.TWEAK_CHOICE_ALL_ITEMS.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_AFTER_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
			)
		{
			doSubst = true;
			if(debug) debugMsg( kFName, "Configured to match all in-fence-range values" );
		}
		// Search engine URL?
		else if( SearchEngineConfig.TWEAK_CHOICE_SEARCH_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull( getSearchEngineURL() );
			if( null!=compUrl && attrVal.startsWith(compUrl) )
			{
				doSubst = true;
				if(debug) debugMsg( kFName, "Matches normalized search engine URL '" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized search engine URL '" + compUrl + "'" );				
			}
		}
		// A specific URL prefix?
		else if( SearchEngineConfig.TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull(
				getSearchEngine().getResultsFormsTweakArg1()
				);
			if( null!=compUrl && attrVal.startsWith(compUrl) )
			{
				doSubst = true;
				if(debug) debugMsg( kFName, "Matches normalized URL prefix '" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized URL prefix '" + compUrl + "'" );					
			}
		}
		// EXCLUDE a specific URL prefix?
		else if( SearchEngineConfig.TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull(
				getSearchEngine().getResultsFormsTweakArg1()
				);
			if( null!=compUrl && ! attrVal.startsWith(compUrl) )
			{
				doSubst = true;	
				if(debug) debugMsg( kFName, "Matches normalized Exclude prefix, so will NOT tweak, pattern='" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized Exclude prefix, so WILL tweak, pattern='" + compUrl + "'" );					
			}
		}

		if(debug) debugMsg( kFName, "Near bottom, Main startAt was " + startAt + ", tagStart was " + formTagBeginsAt );					

		// If we're supposed to do the substitution
		if( doSubst )
		{
			String newValue = getSearchNamesURL();

			if(debug) debugMsg( kFName, "WILL replace with new value='" + newValue + "'" );					

			// Do the same edits to BOTH buffers
			// Also, even though we trimmed source URL for comparison,]
			// we're still overwriting the entire length with the new URL
			buff.replace( attrValueStartsAt, attrValueEndsAt+1, newValue );
			buffNorm.replace( attrValueStartsAt, attrValueEndsAt+1, newValue.toLowerCase() );

			// Delta length
			int newLen = newValue.length();
			int oldLen = attrValueEndsAt - attrValueStartsAt + 1;
			int deltaLen = newLen - oldLen;

			if(debug) debugMsg( kFName, "Pointer adjustment new/old/detla = " + newLen + '/' + oldLen + '/' + deltaLen );					

			// Adjust Start At
			startAt = formTagEndsAt + deltaLen + HTML_TAG_END.length();
			
		}
		// Else not substition
		else {
			// We still need to adjust Start At
			startAt = formTagEndsAt + HTML_TAG_END.length();
		}	

		if(debug) debugMsg( kFName, "AT bottom, Main startAt NOW " + startAt + ", tagStart was " + formTagBeginsAt );					

	}	// End for each tag in document
	
	return new String( buff );

}


	// Given a list of valid webmaster suggestions, format a snippet
	// We'll return NULL if we're not happy
	private Element formatAlternateSuggestionsToSnippet(
		List inValidSuggestions, String inQuery
		)
	{
		//return formatAlternateSuggestionsToSnippet( inValidSuggestions, null );
		MapRecordInterface formatRecord = getMainConfig().getPrimaryAltMapRecordForTerm( inQuery, fRequestInfo );
		return formatAlternateSuggestionsToSnippet(
			inValidSuggestions, inQuery, formatRecord
			);
	}
	Element formatAlternateSuggestionsToSnippet(
		List inValidSuggestions, String inQuery, MapRecordInterface inPrimaryMapRecord
		)
	{
		final String kFName = "formatAlternateSuggestionsToSnippet";

		boolean haveAddedAtLeastOneTerm = false;

		Element sloganElement = inPrimaryMapRecord.generateAltSloganElement();
		if( null==sloganElement ) {
			errorMsg( kFName, "Got null intro-text/slogan Element, returning null." );
			return null;
		}

		// The top level will be a <div> tag
		Element outElem = new Element("div");
		outElem.addContent( sloganElement );
		// We insert a <br> after the intro heading
		// ^^^ NO, not for alternative suggestions
		// outElem.addContent( new Element("br") );

		// For each good alternate term
		// Some flags
		boolean isFirst = true;
		for( Iterator it2 = inValidSuggestions.iterator(); it2.hasNext() ; )
		{
			// Get the next one from the list
			String altTerm = (String)it2.next();

			// Reset the last flag
			boolean isLast = ! it2.hasNext();

			// Format the results
			// String tmpText = formatAnAlternativeSuggestionRecord(
			//	altTerm, isFirst, isLast
			//	);
			Element termElem = formatAnAlternativeSuggestionRecord( altTerm );

			if( null!=termElem ) {
				if( ! isFirst )
					outElem.addContent( ALT_TERM_SEP );
				outElem.addContent( termElem );
				haveAddedAtLeastOneTerm = true;
				// Reset first flag
				isFirst = false;
			}

			// if( isLast ) ...

		}   // End for each valid record

		// Add the footer
		outElem.addContent( new Element("br") );

		// add commments
		// add helpful comments
		String comments = "Info: NIE SearchNames:";
		if( inQuery != null )
			comments += " Query was '" + inQuery + "'.";
		comments += " Displayed " + inValidSuggestions.size() + " suggestion(s).";
		outElem.addContent( new Comment(comments) );

		// Return the results
		if( haveAddedAtLeastOneTerm )
		{
			// Due to anchor tag issues and white space, we want a compact version with no NL's
			// return JDOMHelper.JDOMToString( outElem, false );
			// No, kils white space, try non-compact
			// return JDOMHelper.JDOMToString( outElem, true );
			return outElem;
		}
		else
		{
			warningMsg( kFName, "Got to end of routine but had not added"
				+ " any suggestions to buffer."
				// + " Buffer I had was = \"" + new String(buff) + "\"."
				+ " XML buffer I had was = \"" + JDOMHelper.JDOMToString( outElem, true ) + "\"."
				+ " Returning null."
				);
			return null;
		}
	}

	private String _formatAlternateSuggestionsToSnippet_v1(
			List inValidSuggestions, String inQuery
			)
	{
		//return formatAlternateSuggestionsToSnippet( inValidSuggestions, null );
		MapRecordInterface formatRecord = getMainConfig().getPrimaryAltMapRecordForTerm( inQuery, fRequestInfo );
		return _formatAlternateSuggestionsToSnippet_v1(
			inValidSuggestions, inQuery, formatRecord
			);
	}
	String _formatAlternateSuggestionsToSnippet_v1(
		List inValidSuggestions, String inQuery, MapRecordInterface inPrimaryMapRecord
		)
	{
		final String kFName = "_formatAlternateSuggestionsToSnippet_v1";

		boolean haveAddedAtLeastOneTerm = false;

		Element sloganElement = inPrimaryMapRecord.generateAltSloganElement();
		if( null==sloganElement ) {
			errorMsg( kFName, "Got null intro-text/slogan Element, returning null." );
			return null;
		}

		// The top level will be a <div> tag
		Element outElem = new Element("div");
		outElem.addContent( sloganElement );
		// We insert a <br> after the intro heading
		// ^^^ NO, not for alternative suggestions
		// outElem.addContent( new Element("br") );

		// For each good alternate term
		// Some flags
		boolean isFirst = true;
		for( Iterator it2 = inValidSuggestions.iterator(); it2.hasNext() ; )
		{
			// Get the next one from the list
			String altTerm = (String)it2.next();

			// Reset the last flag
			boolean isLast = ! it2.hasNext();

			// Format the results
			// String tmpText = formatAnAlternativeSuggestionRecord(
			//	altTerm, isFirst, isLast
			//	);
			Element termElem = formatAnAlternativeSuggestionRecord( altTerm );

			if( null!=termElem ) {
				if( ! isFirst )
					outElem.addContent( ALT_TERM_SEP );
				outElem.addContent( termElem );
				haveAddedAtLeastOneTerm = true;
				// Reset first flag
				isFirst = false;
			}

			// if( isLast ) ...

		}   // End for each valid record

		// Add the footer
		outElem.addContent( new Element("br") );

		// add commments
		// add helpful comments
		String comments = "Info: NIE SearchNames:";
		if( inQuery != null )
			comments += " Query was '" + inQuery + "'.";
		comments += " Displayed " + inValidSuggestions.size() + " suggestion(s).";
		outElem.addContent( new Comment(comments) );

		// Return the results
		if( haveAddedAtLeastOneTerm )
			// Due to anchor tag issues and white space, we want a compact version with no NL's
			// return JDOMHelper.JDOMToString( outElem, false );
			// No, kils white space, try non-compact
			return JDOMHelper.JDOMToString( outElem, true );
		else
		{
			warningMsg( kFName, "Got to end of routine but had not added"
				+ " any suggestions to buffer."
				// + " Buffer I had was = \"" + new String(buff) + "\"."
				+ " XML buffer I had was = \"" + JDOMHelper.JDOMToString( outElem, true ) + "\"."
				+ " Returning null."
				);
			return null;
		}
	}


	// Can return null if it's not happy
	// Todo: by rights we should probably only calculate the base URL once
	// private String formatAnAlternativeSuggestionRecord(
	//	String inTerm, boolean inIsFirst, boolean inIsLast
	// private String formatAnAlternativeSuggestionRecord( String inTerm )
	private Element formatAnAlternativeSuggestionRecord( String inTerm )
	{
		final String kFName = "formatAnAlternativeSuggestsRecord";
		final String kLeftPad = "5px";

		// String theCorrectedTerm = inRecord.getAlternateTerm();
		String theCorrectedTerm = NIEUtil.trimmedStringOrNull( inTerm );
		if( null == theCorrectedTerm ) {
			errorMsg( kFName, "Null term, returning null element." );
			return null;
		}

		// Get the Search URL
		String lSearchURL = fRequestInfo.getIsGoogleOneBoxSnippetRequest()
			? getSearchEngineURL()
			: getSearchNamesURL()
			;
		// TODO: need to change for API modes as well

		// As a backup, we'd take the search engine URL
		if( null == lSearchURL ) {
			lSearchURL = getSearchEngineURL();
			warningMsg( kFName,
				"We were unable to get a URL for Search Names"
				+ " so using the URL for the host search engine instead."
				);
		}


		// Calculate the NEW URL, several steps

		// First we create a new request
		AuxIOInfo newRequest = new AuxIOInfo();
		// Consistent handling of fields
		newRequest.setCGIFieldsCaseSensitive( getSearchEngine().isCGIFeildsCaseSensitive() );
		// Next we need to copy over MOST of the CGI fields
		// but not query text
		String queryField = getUserQueryField();
		// Create an exclude list for the copy command
		List excludes = new Vector();
		// And add this term in
		excludes.add( queryField );

		if( fRequestInfo.getIsGoogleOneBoxSnippetRequest() )
		{
			// Exclude some Onebox CGI fields that are specific to
			// the previous OneBox request to us, but are NOT
			// appropriate for a request back to it
			// Stuff like "apiMaj", "apiMin", "oneboxName", "lang", "ipAddr", "dateTime", "authType"
			excludes.addAll( GOOGLE_ONEBOX_CGI_FIELDS_LIST );
			// And oddly, google uses a different CGI name for query
			excludes.add( GOOGLE_ONEBOX_CGI_QUERY_FIELD_NAME );

			// And we need to remap the query properly
		}

		// Now we're ready to copy over the fields
		newRequest.copyInCGIFields( fRequestInfo, excludes );

		if( fRequestInfo.getIsGoogleOneBoxSnippetRequest() )
		{

			// Now we overlay any missing test drive fields
			newRequest.addOnlyMissingValuesToHashes( getSearchEngine().getSearchEngineTestDriveURLFields() );
	
			// Now we add in OUR search terms
			String realQueryField = getSearchEngine().getQueryField();
			// Note: Do NOT USE getUserQueryField() at this time
			// A normal Google search uses "q"
			// But a OneBox request uses "query"
			// This transaction started life as a OneBox request so
			// we were hard coded to look at query.
			// But now we are producing links for the USER to follow,
			// links that should go through Google as a REGULAR request		
			newRequest.clearCGIField( realQueryField, false );
			newRequest.addCGIField( realQueryField, theCorrectedTerm );
		}
		// Else regular searches use regular fields
		else {
			newRequest.addCGIField( queryField, theCorrectedTerm );
		}
			
		// Now ask for all the CGI fields, as an encoded buffer
		String cgifields = newRequest.getCGIFieldsAsEncodedBuffer();
		// Now combine the CGI fields with the URL

		// If we got anything back, add it on
		if( cgifields != null && cgifields.length() > 0 )
		{
			// Join the two strings together, usually with a "?"
			lSearchURL = NIEUtil.concatURLAndEncodedCGIBuffer(
				lSearchURL, cgifields
				);
		}
		// Else something strange happening
		else
		{
			errorMsg( kFName, "Unable to encode CGI variables for new URL."
				+ " New query was \"" + theCorrectedTerm + "\"."
				+ " Returning NULL."
				);
			return null;
		}

		// Create the anchor tag and link text
		Element outElem = new Element( "a" );
		outElem.setAttribute( "class", CSSClassNames.ALT_TERM );
		// Need some room to the left
		outElem.setAttribute( "style", "padding-left: " + kLeftPad );
		outElem.setAttribute( "href", lSearchURL );
		// We still force bold tags because we can never be sure if we'll
		// have access to our style sheets
		Element bElem = new Element( "b" );
		outElem.addContent( bElem );
		// And we add text to be inside the bold tag
		bElem.addContent( theCorrectedTerm );

		// Return results
		return outElem;
	}

	// Given a list of valid webmaster suggestions, format a snippet
	private String _formatWMSuggestionsToSnippet_v1(
		List inValidSuggestions, String inQueryTerm
		)
	{
		// return formatWMSuggestionsToSnippet( inValidSuggestions, null );

		// We need at least one map element to tell us how to format the thing
		MapRecordInterface formatRecord = getMainConfig().getPrimaryWmsMapRecordForTerm( inQueryTerm, fRequestInfo );
		return _formatWMSuggestionsToSnippet_v1(
			inValidSuggestions, inQueryTerm, formatRecord
			);
	}
	private Element formatWMSuggestionsToSnippet(
		List inValidSuggestions, String inQueryTerm
		)
	{
		// return formatWMSuggestionsToSnippet( inValidSuggestions, null );

		// We need at least one map element to tell us how to format the thing
		MapRecordInterface formatRecord = getMainConfig().getPrimaryWmsMapRecordForTerm( inQueryTerm, fRequestInfo );
		return formatWMSuggestionsToSnippet(
			inValidSuggestions, inQueryTerm, formatRecord
			);
	}
	private String _formatWMSuggestionsToSnippet_v1( List inValidSuggestions,
		String optQueryText, MapRecordInterface inPrimaryMapRecord
		)
	{
		final String kFName = "_formatWMSuggestionsToSnippet_v1";

		// Conjure a table tag
		// We actually use two nested tables for a colored border
		Element [] wmsSuggestBoxElems = inPrimaryMapRecord.generateWmsBoxElements();
		// The top level tag we will eventually convert to text
		Element outElem = wmsSuggestBoxElems[0];
		// the table tag to hang content in (if not nested will be same as outElem )
		Element contentHangerTable = wmsSuggestBoxElems[1];

		// The "lightbulb" that indents the actual suggestions
		// We grab this first so we know about spanning for the row that
		// comes before it
		// This can return a null if nothing
		// An <img> if not linked
		// Or <a><img> if linked
		// TODO: Issue with trailing newline and hyperlink?
		Element iconElementOrNull = inPrimaryMapRecord.generateWmsIconElementOrNull();


		// Add the header text / slogan
		// Takes up both cells in the top row of a fauz 2x2 table

		// Will generate <font> with attrs and content
		Element sloganElement = inPrimaryMapRecord.generateWmsSloganElement();
		if( null!=sloganElement ) {
			// The top row
			Element row1 = new Element( "tr" );
			contentHangerTable.addContent( row1 );
			// The 1x2 cell for the top row
			Element sloganCell = new Element( "td" );
			// only do colspan if needed
			if( null!=iconElementOrNull )
				sloganCell.setAttribute( "colspan", "2" );
			sloganCell.addContent( sloganElement );
			row1.addContent( sloganCell );
		}

		// Start on the main row
		Element row2 = new Element( "tr" );
		contentHangerTable.addContent( row2 );

		// If we have an icon, add a cell and add it in
		if( null!=iconElementOrNull ) {
			Element iconCell = new Element( "td" );
			iconCell.addContent( iconElementOrNull );
			row2.addContent( iconCell );
		}

		// Now we build the cell that will hold the simple list of suggestions
		Element suggestionsCell = new Element( "td" );
		row2.addContent( suggestionsCell );


		// Start building up the results
		// For each good url record
		int numAdded = 0;
		for( Iterator it = inValidSuggestions.iterator(); it.hasNext() ; )
		{
			// Get the next one from the list
			SnURLRecord urlRecord = (SnURLRecord)it.next();
			// format it and add it
			// Element linkElem = urlRecord.generateWebmasterSuggestsLink( inPrimaryMapRecord );
			// Element linkElem = inPrimaryMapRecord.generateWebmasterSuggestsLink( urlRecord );
			// Element linkElem = inPrimaryMapRecord.generateWmsDocUrlElementOrNull( urlRecord );
			Element linkElem = urlRecord.generateWebmasterSuggestsItem( inPrimaryMapRecord );

			if( null!=linkElem ) {
				numAdded++;
				// Add an intervening <br> if we're not the first
				if( numAdded > 1 )
					suggestionsCell.addContent( new Element( "br") );
				// Now add this suggestion
				suggestionsCell.addContent( linkElem );
			}
		}   // End for each valid record



		// add commments
		// add helpful comments
		String comments = " Info: NIE SearchTrack:";
		if( optQueryText != null )
			comments += " Query was '" + optQueryText + "'.";
		comments += " Displayed " + inValidSuggestions.size() + " suggestion(s).";
		suggestionsCell.addContent( new Comment( comments ) );

		// At the very end we want to add a br, so we put the whole thing in a div
		/***
		Element newDivElem = new Element( "div" );
		newDivElem.addContent( outElem );
		newDivElem.addContent( new Element( "br" ) );
		outElem = newDivElem;
		***/
		// ^^^ that didn't work, a <br> inside a div doesn't seem to do it
		// so we're just gonna shove on a character string at the end instead

		// Return the results
		if( numAdded > 0 ) {
			// String outStr = JDOMHelper.JDOMToString( outElem, false );
			String outStr = JDOMHelper.JDOMToString( outElem, true );

			// !!! NOTE !!!!
			// If you want to jam in text before or after, look
			// in markupWithSnippet() and then at the section with
			// getSearchEngine().getSnippetPrefixText(); / Suffix

			// outStr += "<br />";
			// being very stubborn...
			// outStr += "&nbsp;<br />";
			// outStr += "<hr>";
			return outStr;
		}
		else
		{
			warningMsg( kFName, "Got to end of routine but had not added"
				+ " any suggestions to buffer."
				+ " XML buffer I had was = \"" + JDOMHelper.JDOMToString( outElem, true ) + "\"."
				+ " Returning true null."
				);
			return null;
		}
	}

	private Element formatWMSuggestionsToSnippet( List inValidSuggestions,
		String optQueryText, MapRecordInterface inPrimaryMapRecord
		)
	{
		final String kFName = "formatWMSuggestionsToSnippet";

		// Conjure a table tag
		// We actually use two nested tables for a colored border
		Element [] wmsSuggestBoxElems = inPrimaryMapRecord.generateWmsBoxElements();
		// The top level tag we will eventually convert to text
		Element outElem = wmsSuggestBoxElems[0];
		// the table tag to hang content in (if not nested will be same as outElem )
		Element contentHangerTable = wmsSuggestBoxElems[1];

		// The "lightbulb" that indents the actual suggestions
		// We grab this first so we know about spanning for the row that
		// comes before it
		// This can return a null if nothing
		// An <img> if not linked
		// Or <a><img> if linked
		// TODO: Issue with trailing newline and hyperlink?
		Element iconElementOrNull = inPrimaryMapRecord.generateWmsIconElementOrNull();

		// Add the header text / slogan
		// Takes up both cells in the top row of a fauz 2x2 table

		// Will generate <font> with attrs and content
		Element sloganElement = inPrimaryMapRecord.generateWmsSloganElement();
		if( null!=sloganElement ) {
			// The top row
			Element row1 = new Element( "tr" );
			contentHangerTable.addContent( row1 );
			// The 1x2 cell for the top row
			Element sloganCell = new Element( "td" );
			// only do colspan if needed
			if( null!=iconElementOrNull )
				sloganCell.setAttribute( "colspan", "2" );
			sloganCell.addContent( sloganElement );
			row1.addContent( sloganCell );
		}

		// Start on the main row
		Element row2 = new Element( "tr" );
		contentHangerTable.addContent( row2 );

		// If we have an icon, add a cell and add it in
		if( null!=iconElementOrNull ) {
			Element iconCell = new Element( "td" );
			iconCell.addContent( iconElementOrNull );
			row2.addContent( iconCell );
		}

		// Now we build the cell that will hold the simple list of suggestions
		Element suggestionsCell = new Element( "td" );
		row2.addContent( suggestionsCell );


		// Start building up the results
		// For each good url record
		int numAdded = 0;
		for( Iterator it = inValidSuggestions.iterator(); it.hasNext() ; )
		{
			// Get the next one from the list
			SnURLRecord urlRecord = (SnURLRecord)it.next();
			// format it and add it
			// Element linkElem = urlRecord.generateWebmasterSuggestsLink( inPrimaryMapRecord );
			// Element linkElem = inPrimaryMapRecord.generateWebmasterSuggestsLink( urlRecord );
			// Element linkElem = inPrimaryMapRecord.generateWmsDocUrlElementOrNull( urlRecord );
			Element linkElem = urlRecord.generateWebmasterSuggestsItem( inPrimaryMapRecord );

			if( null!=linkElem ) {
				numAdded++;
				// Add an intervening <br> if we're not the first
				if( numAdded > 1 )
					suggestionsCell.addContent( new Element( "br") );
				// Now add this suggestion
				suggestionsCell.addContent( linkElem );
			}
		}   // End for each valid record



		// add commments
		// add helpful comments
		String comments = " Info: NIE SearchTrack:";
		if( optQueryText != null )
			comments += " Query was '" + optQueryText + "'.";
		comments += " Displayed " + inValidSuggestions.size() + " suggestion(s).";
		suggestionsCell.addContent( new Comment( comments ) );

		// At the very end we want to add a br, so we put the whole thing in a div
		/***
		Element newDivElem = new Element( "div" );
		newDivElem.addContent( outElem );
		newDivElem.addContent( new Element( "br" ) );
		outElem = newDivElem;
		***/
		// ^^^ that didn't work, a <br> inside a div doesn't seem to do it
		// so we're just gonna shove on a character string at the end instead

		// Return the results
		if( numAdded > 0 ) {
			// String outStr = JDOMHelper.JDOMToString( outElem, false );
			// String outStr = JDOMHelper.JDOMToString( outElem, true );
			return outElem;

			// !!! NOTE !!!!
			// If you want to jam in text before or after, look
			// in markupWithSnippet() and then at the section with
			// getSearchEngine().getSnippetPrefixText(); / Suffix

			// outStr += "<br />";
			// being very stubborn...
			// outStr += "&nbsp;<br />";
			// outStr += "<hr>";
			// return outStr;
		}
		else
		{
			warningMsg( kFName, "Got to end of routine but had not added"
				+ " any suggestions to buffer."
				+ " XML buffer I had was = \"" + JDOMHelper.JDOMToString( outElem, true ) + "\"."
				+ " Returning true null."
				);
			return null;
		}
	}

	// Can return null if it's not happy
	private String _OBS_formatAWebmasterSuggestsRecordOBS(
		SnURLRecord inRecord
		)
	{
		final String kFName = "formatAWebmasterSuggestsRecord";

		if( inRecord == null )
		{
			errorMsg( kFName,
				"Null URL object, returning null."
				);
			return null;
		}

		StringBuffer buff = new StringBuffer();

		// Grab the fields we'll be using
		String tmpURL = inRecord.getURL();
		String tmpTitle = inRecord.getTitle();
		String tmpDesc = inRecord.getDescription();

		if( tmpURL == null )
		{
			errorMsg( kFName,
				"Null URL, returning null."
				);
			return null;
		}

		if( tmpTitle == null )
		{
			warningMsg( kFName,
				"Null title, will use URL for title."
				);
			tmpTitle = tmpURL;
		}

		// Add to the buffer

		// TODO: these text items should be HTML escaped

		// Add the hyperlinked title
		buff.append( "<a href=\"" + tmpURL + "\">" );
		buff.append( "<b>" + tmpTitle + "</b>" );
		buff.append( "</a><br>" + NL );

		// Add the description, if any
		if( tmpDesc != null )
		{
			buff.append( "<small>" + tmpDesc + "</small><br>" + NL );
		}

		// Add the final link, if requested
		/*** OBSOLETE, use newer entire method
		if( inRecord.shouldDisplay() )
		{
			buff.append( "<a href=\"" + tmpURL + "\">" );
			buff.append( "<small><i>" + tmpURL + "</i></small>" );
			buff.append( "</a><br>" + NL );
		}
		***/

		// A final newline
		buff.append( "<br>" + NL );

		// Return results
		return new String( buff );
	}


	private void ___User_Data_Routines___() {}
	//////////////////////////////////////////////////////////

	// Given the results of a search, add all the
	// generic user data items to it
	private String markupWithUserDataItems( String inBuffer )
	{
		final String kFName = "markupWithUserDataItems";

// addSearchTrackDirectLoggingCGIFields

		if( inBuffer == null )
		{
			errorMsg( kFName,
				"Null document passed in, so leaving document as is."
				);
			return inBuffer;
		}
		
		// Our eventual answer
		String outBuffer = inBuffer;

		// These are all the user data classes we know about
		// Hashtable userClasses =
		//	getMainConfig().getUserClassesHashMap();
			// getMainApplication().getUserClassesHashMap();

		// This the actual data we found for these keywords
		// This is a hashtable to <items> elements, each filed by class
		Hashtable userData = getUserDataItemRecords();

		// Sanity check
		if( null==userData || userData.size() < 1 )
		{
			debugMsg( kFName,
				"No user data items found, so leaving document as is."
				);
			return inBuffer;
		}
		
		
		// For each type of node that we found
		Set keys = userData.keySet();
		for( Iterator it = keys.iterator(); it.hasNext(); )
		{
			// get the key
			String userClassName = (String)it.next();

			/***
			// Get the user class for this class
			if( ! userClasses.containsKey( userClassName ) )
			{
				errorMsg( kFName,
					"No class definition found for data item's class"
					+ " of \"" + userClassName + "\", so ignoring data."
					+ " Will continue looking at other data items."
					);
				continue;
			}
			BaseMarkup marker =
				(BaseMarkup)userClasses.get( userClassName );
			***/

			BaseMarkup marker = getMainConfig().getUserClassDefByNameOrNull( userClassName );
			if( null == marker )
			{
				errorMsg( kFName,
					"No class definition found for data item's class"
					+ " of \"" + userClassName + "\", so ignoring this data."
					+ " Will continue looking at other data items."
					);
				continue;
			}
			
			// get the top level <items> tag
			Element itemsRoot =
				(Element)userData.get( userClassName );


			// Augment the XML data with some additional stats
			////////////////////////////////////////////////////

			// Add some additional meta data to the XML snippet
			Element lTransactionInfo = new Element(
				BaseMarkup.AUX_INFO_TAG_NAME
				);
			// Add the user's query, the search terms
			String lQuery = getUserQuery();
			if( lQuery != null )
				JDOMHelper.setAttributeString(
					lTransactionInfo,
					BaseMarkup.AUX_INFO_QUERY_ATTR,
					lQuery
					);
			String lLowerQuery = getUserQueryToLower();
			if( lLowerQuery != null )
				JDOMHelper.setAttributeString(
					lTransactionInfo,
					BaseMarkup.AUX_INFO_QUERY_NORM_ATTR,
					lLowerQuery
					);
			String lQueryField = getUserQueryField();
			if( lQueryField != null )
				JDOMHelper.setAttributeString(
					lTransactionInfo,
					BaseMarkup.AUX_INFO_QUERY_FIELD_NAME_ATTR,
					lQueryField
					);

			// Else no warning, in the future we may place them for no query

			// Add the transaction ID
			long lTransID = getTransactionID();
			if( lTransID > 0 )
				JDOMHelper.setAttributeString(
					lTransactionInfo,
					BaseMarkup.AUX_INFO_TRANS_ID_ATTR,
					"" + lTransID
					);
			else
				errorMsg( kFName, "No transaction ID to log." );

			// How many ads were in this group
			List tmpChildren = itemsRoot.getChildren();
			int tmpCount = tmpChildren != null ? tmpChildren.size() : 0 ;
			if( tmpCount > 0 )
				JDOMHelper.setAttributeInt(
					lTransactionInfo,
					BaseMarkup.AUX_INFO_TOTAL_AD_COUNT_ATTR,
					tmpCount
					);
			else
				errorMsg( kFName, "No children to log." );

			// Todo: others....
			// Now add this branch to the main items
			itemsRoot.addContent( lTransactionInfo );
		
			// Add the markups
			String savedBuffer = outBuffer;
			try
			{
				outBuffer = marker.doMarkup(
					outBuffer, itemsRoot, getRequestObject(), getResponseObject()
					);
			}
			catch (MarkupException e)
			{
				errorMsg( kFName,
					"Error adding Markup to document, keeping unmodified buffer."
					+ " Error: " + e
					);
				outBuffer = savedBuffer;
			}
			
		}	// End for each class of user data items we found
		
		// We're done
		return outBuffer;	

	}


	private static void ___Getting_Matching_Maps_and_Terms___() {}
	////////////////////////////////////////////////////////////////////

	public List getValidWebmasterSuggestsRecords() {
		// Find out what the user entered
		String lQueryTerms = getUserQueryToLower();
		return getMainConfig().getValidWebmasterSuggestsRecords( lQueryTerms, fRequestInfo );
	} 
	public List getValidAlternateSuggestionRecords() {
		// Find out what the user entered
		String lQueryTerms = getUserQueryToLower();
		return getMainConfig().getValidAlternateSuggestionRecords( lQueryTerms, fRequestInfo );
	}

	public boolean getHasAnyMatchingTerms() {
		// Find out what the user entered
		String lQueryTerms = getUserQueryToLower();
		return getMainConfig().getHasAnyMatchingTermsQuickCheck( lQueryTerms );
	}

	// Returns a list of user data items
	// can also return null
	private Hashtable getUserDataItemRecords() {
		// Find out what the user entered
		String lQueryTerms = getUserQueryToLower();
		return getMainConfig().getUserDataItemRecords( lQueryTerms, fRequestInfo );
	}

	private boolean hasUserDataItems() {
		// Find out what the user entered
		String lQueryTerms = getUserQueryToLower();
		return getMainConfig().hasUserDataItems( lQueryTerms, fRequestInfo );
	}


	private static void ___Misc___() {}
	////////////////////////////////////////////////////////////////////

	// Some search engines can use Direct Logging, vs. Proxy Logging
	// If they use Direct Logging, then we need to pass them some additional
	// information for them to log
	private void addSearchTrackDirectLoggingCGIFields( AuxIOInfo inCGIInfo )
	{
		final String kFName = "addSearchTrackDirectLoggingCGIFields";
		if( inCGIInfo == null )
		{
			errorMsg( kFName,
				"Was passed in a null CGI info object, returning now."
				);
			return;
		}

		// Todo: should really use constant names for the fields
		// NOTE: field names also used in SearchLogger.logTransaction
		if( fSNActionCode > SearchTuningConfig.SN_ACTION_CODE_UNDEFINED )
			inCGIInfo.addCGIField( "sn_action_code", ""+fSNActionCode );
		if( fSNStatusCode > SearchTuningConfig.SN_STATUS_UNDEFINED )
			inCGIInfo.addCGIField( "sn_status_code", ""+fSNStatusCode );
		if( fSNActionCount >= 0 )
			inCGIInfo.addCGIField( "sn_action_count", ""+fSNActionCount );
		if( fSNActionItemCount >= 0 )
			inCGIInfo.addCGIField( "sn_item_count", ""+fSNActionItemCount );
		if( fSNStatusMsg != null )
			inCGIInfo.addCGIField( "sn_status_msg", fSNStatusMsg );

	}

	private static void ___Comm_Request_and_Response___() {}
	////////////////////////////////////////////////////////////////////

	///////
	//
	// Read the HTTP Request from the socket.
	//
	///////

	// Todo: does this need to do anything else?
	public void readCompleteRequest()
		throws SpuriousHTTPRequestException
	{
		final String kFName = "readCompleteRequest";
		boolean debug = shouldDoDebugMsg( kFName );

		// Read the complete HTTP request, GET/POST, etc.
		// AuxIOInfo has fields for various info/results
		// We ask it to also closes the socket when done
		// Declaration: AuxIOInfo fRequestInfo
		// And for now, don't close the stream, we'll do it later
		if(debug) debugMsg( kFName, "Will call readHTTPRequestFromSocket" );
		fRequestInfo = NIEUtil.readHTTPRequestFromSocket( fHandlerSocket,
			false, // false(=open) but getting spurious connections, true= CAN NOT SEND RESPONSE!
			( null!=getSearchEngine() ? getSearchEngine().isCGIFeildsCaseSensitive() : false )
			);

		if( fRequestInfo == null )
		{
			errorMsg( kFName,
				"Got back NULL request from socket."
				);
			return;
		}
		if(debug) debugMsg( kFName, "Have a non-null request" );
		fInputStream = null;

		// We'd also like a unique transaction number
		fRequestInfo.stampWithTransactionID();

		// Figure out the access level
		if( null!=getMainConfig() )
			calculateAndSetAccessLevel();

		// Copy over a few variables
		// fCGIFields = fRequestInfo.outCGIFieldHash;

//		System.err.println( "v=========================v===================================v" );
//		System.err.println( fRequestInfo.displayRequestIntoBuffer() );
//		System.err.println( "^=========================^===================================^" );

	}


	// Use the global response
	//void transmitResponse( AuxIOInfo inInfo )
	private void transmitResponse()
		throws IOException,
			UnsupportedEncodingException
	{
		final String kFName = "transmitResponse";
		boolean trace = shouldDoTraceMsg( kFName );

		// String responseBuffer = prepareResponseBuffer();
		byte [] responseBuffer = prepareResponseBuffer();

		if(trace) traceMsg( kFName,
			"Response:" + NIEUtil.NL
			+ new String( responseBuffer )
			// + responseBuffer
			);

		// try
		// {

			// mbennett
			fHandlerSocket.setSoLinger( false, 0 );

			OutputStream lOutputStream = fHandlerSocket.getOutputStream();

			// lOutputStream.write( responseBuffer.getBytes() );
			lOutputStream.write( responseBuffer );
			lOutputStream.flush();
			lOutputStream.close();
			lOutputStream = null;

			// //=mbennett, try turning these off
			// fHandlerSocket.shutdownInput();
			// fHandlerSocket.shutdownOutput();

			// mbennett
			fHandlerSocket.close();
			fHandlerSocket = null;

			debugMsg( kFName, "Finished transmitting response, have closed sockets." );

		/*** since we're now throwing encoding exception, also pass up IO
		}
		catch( IOException eIO )
		{
			errorMsg( kFName,
				"Exception while sending response."
				+ " Exception=\"" + eIO + "\""
				);
		}
		***/

	}

	// Take info from the response object and create a character buffer
	// For now we do NOT use the header lines list
	// private String prepareResponseBuffer()
	//
	// This version is newer
	// There are some advantages to us generating our own headers
	// over letting Java do it (we can repeat header names, when mimicking
	// a server response that does that)
	private byte [] prepareResponseBuffer()
		throws UnsupportedEncodingException
	{
		byte [] body = prepResponseBodyOrNull();
		int contentLength = null==body ? 0 : body.length;
		
		byte [] headers = prepResponseHeaders( contentLength );
		// ^^^ Header will INCLUDE the extra blank line

		int totalLength = headers.length
			// + AuxIOInfo.HEADER_EOL.length()
			+ contentLength
			;
			
		// Start building the output, as an array of bytes
		byte [] outBuffer = new byte[ totalLength ];

		int i=0, j=0;
		// Copy over the headers
		for( i=0; i < headers.length; i++ )
			outBuffer[i] = headers[ i ];

		// And then any content
		if( null!=body && body.length > 0 )
		{
			// Separator
			// This is the blank line
			/*** already added in header
			for( j=0; j < AuxIOInfo.HEADER_EOL.length(); j++ )
			{
				outBuffer[i] = (byte) AuxIOInfo.HEADER_EOL.charAt( j );
				i++;
			}
			***/
			// Content

			// !!! Wrong index variable, texts exception handling! :-)
			// for( j=0 ; j < body.length ; i++ )

			// correct
			for( j=0 ; j < body.length ; j++ )
			{
				outBuffer[i] = body[j];
				i++;
			}
		}

		// Return what we wound up with
		return outBuffer;

	}

	// Returns a byte buffer or Null if no proper content
	// throws exception only if it doesn't like the encoding
	byte [] prepResponseBodyOrNull()
		throws UnsupportedEncodingException
	{
		final String kFName = "prepResponseBodyOrNull";

		byte [] outBytes = null;
		// Binary content
		if( fResponseInfo.getIsBinary() ) {
			outBytes = fResponseInfo.getBinContent();
		}
		// Textual content
		else
		{
			String tmpContent = fResponseInfo.getContent();
			if( null!=tmpContent )
			{
				// Get the character encoding, including some reasonable default
				String encoding = fResponseInfo.calculateEncoding( null, null );
				debugMsg( kFName, "Encoding='" + encoding + "'" );

				outBytes = tmpContent.getBytes( encoding );

				/*** way old school
				char [] tmpChars = tmpContent.toCharArray();
				if( null!=tmpChars ) {
					byte [] tmpBytes = new byte [2*tmpChars.length];
					int i2=0;
					for( int i=0; i<tmpChars.length; i++ ) {
						char c = tmpChars[i];
						if( c <= 0xff )
							tmpBytes[i2++] = (byte)c;
						else {
							// Not really sure on the order, this seems right
							byte b1 = (byte)( c >> 8 & 0xff );	// MSB
							byte b2 = (byte)( c & 0xff );		// LSB
							// add them to the array
							tmpBytes[i2++] = b1;
							tmpBytes[i2++] = b2;
						}
					}
					lContent = tmpBytes;
					lContentLength = i2;
				}
				else
					lContentLength = 0;
			}
			else
				lContentLength = 0;
			***/

			}
		}

		return outBytes;
	
	}

	// Although logically first, you need to call this
	// AFTER the content, because the header needs to
	// include the length of the content (if any)
	byte [] prepResponseHeaders( int inContentLength )
		throws UnsupportedEncodingException
	{
		// Set maybe one or two specific fields we know we want
		// Make sure they don't keep the socket open
		fResponseInfo.setOrOverwriteHTTPHeaderField(
			"Connection", "close"
			);
	
		// If there's a redirect location, make sure we set that
		String lLocation = fResponseInfo.getDesiredRedirectURL();
		if( lLocation != null )
			fResponseInfo.setOrOverwriteHTTPHeaderField(
				"Location", lLocation
				);
	
		byte [] lContent = null;
		// int lContentLength = fResponseInfo.getActualContentLength();
		if( inContentLength > 0 )
		{
			// Setup full line:
			// Content-type: text/html; charset=utf-8
			// (or other encoding)
			String contentTypeLine =
				fResponseInfo.getContentTypeWithDefault()
				+ "; charset="
				+ fResponseInfo.calculateEncoding( null, null )
				;
			fResponseInfo.setOrOverwriteHTTPHeaderField(
				"Content-Type", contentTypeLine
				);

			// Set the header field
			fResponseInfo.setOrOverwriteHTTPHeaderField(
				"Content-Length", "" + inContentLength
				);
		}

		// The error code we shnould put
		int responseCode = fResponseInfo.getHTTPResponseCodeWithDefault();

		// Now get the headers as a buffer
		String headersStr = fResponseInfo.getHTTPHeadersAsBuffer();

		// TODO: include other return headers from host engine like cookies
		// if( null!=fIntermediateIoInfo )

		// The answer we will return
		// With the first header line
		String outBufferStr =
			"HTTP/1.0 " + responseCode + " Found"
			+ AuxIOInfo.HEADER_EOL	// for previous line
			+ headersStr
			+ AuxIOInfo.HEADER_EOL	// EXTRA line
			;

		// Convert to bytes with UTF-8
		// UTF-8 is suggested for all HTTP headers regardless
		// of the content encoding, according to Sun and W3C
		// Aside from redirects and cookies, we control
		// all the headers so don't use 8 bit much 
		// Also, we web encode all the headers
		//
		// Can throw UnsupportedEncodingException
		// which will passed back to calling routine
		// However, UTF-8 is required for all Java platforms so
		// this should never happen
		return outBufferStr.getBytes( AuxIOInfo.CHAR_ENCODING_UTF8 );

	}

	// Take info from the response object and create a character buffer
	// For now we do NOT use the header lines list
	// private String prepareResponseBuffer()
	//
	// This version is SERIOUSLY outdated and possibly incorrect as well
	// ALTHOUGH there are some advantages to us generating our own headers
	// over letting Java do it (we can repeat header names, when mimicking
	// a server response that does that)
	private byte [] _OBS_prepareResponseBuffer()
	{
		// Set maybe one or two specific fields we know we want
		// Make sure they don't keep the socket open
		fResponseInfo.setOrOverwriteHTTPHeaderField(
			"Connection", "close"
			);

		// If there's a redirect location, make sure we set that
		String lLocation = fResponseInfo.getDesiredRedirectURL();
		if( lLocation != null )
			fResponseInfo.setOrOverwriteHTTPHeaderField(
				"Location", lLocation
				);

		// Header field for content
		// And normalize to null if empty
		// String lContent = null;
		byte [] lContent = null;
		int lContentLength = 0;

		if( fResponseInfo.getIsBinary() ) {
			lContent = fResponseInfo.getBinContent();
			lContentLength = null!=lContent ? lContent.length : 0;
		}
		else {
			String tmpContent = fResponseInfo.getContent();
			if( null!=tmpContent ) {
				char [] tmpChars = tmpContent.toCharArray();
				if( null!=tmpChars ) {
					byte [] tmpBytes = new byte [2*tmpChars.length];
					int i2=0;
					for( int i=0; i<tmpChars.length; i++ ) {
						char c = tmpChars[i];
						if( c <= 0xff )
							tmpBytes[i2++] = (byte)c;
						else {
							// Not really sure on the order, this seems right
							byte b1 = (byte)( c >> 8 & 0xff );	// MSB
							byte b2 = (byte)( c & 0xff );		// LSB
							// add them to the array
							tmpBytes[i2++] = b1;
							tmpBytes[i2++] = b2;
						}
					}
					lContent = tmpBytes;
					lContentLength = i2;
				}
				else
					lContentLength = 0;
			}
			else
				lContentLength = 0;
		}


		// int lContentLength = fResponseInfo.getActualContentLength();
		if( lContentLength > 0 )
		{
			// Make sure content type is set if we have content
			// this normally just overwrites it with itself
			fResponseInfo.setOrOverwriteHTTPHeaderField(
				"Content-Type", fResponseInfo.getContentTypeWithDefault()
				);
			// Set the header field
			fResponseInfo.setOrOverwriteHTTPHeaderField(
				"Content-Length", "" + lContentLength
				);
			// And grab the actual content
			// lContent = fResponseInfo.getContent();
		}

		// The error code we shnould put
		int responseCode = fResponseInfo.getHTTPResponseCodeWithDefault();

		// The answer we will return
		// With the first header line
		String outBufferStr = "HTTP/1.0 " + responseCode + " Found"
			+ AuxIOInfo.HEADER_EOL
			;

		// Now get the headers as a buffer
		String headers = fResponseInfo.getHTTPHeadersAsBuffer();
		// And add it to the out buffer
		// and the extra newline sequence
		outBufferStr += headers + AuxIOInfo.HEADER_EOL;

		// Start building the output, as an array of bytes
		int headerLen = outBufferStr.length();
		int totalLen = headerLen + lContentLength;
		byte [] outBuffer = new byte[ totalLen ];
		int i=0;
		// Copy over the headers and stuff
		for( ; i < headerLen; i++ )
			outBuffer[i] = (byte) outBufferStr.charAt(i);
		// And then any content
		if( lContentLength > 0 )
			for( ; i < totalLen ; i++ )
				outBuffer[i] = lContent[i-headerLen];

		// Return what we wound up with
		return outBuffer;

//			"Location: " + inDestinationURL + "\r\n" +
//			"Connection: close\r\n" +
//			"Content-type: text/html\r\n\r\n";

	}


	private static void ___simple_Gets_Sets_and_Utils___() {}
	////////////////////////////////////////////////////////////////////

	// Quite a few pass-through items that talk to the main app, who then
	// often asks somebody else

	private SearchTuningApp getMainApplication()
	{
		return fMainApp;
	}
	private SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
	}
	private String getSearchNamesURL()
	{
		// return getMainApplication().getSearchNamesURL();
		return getMainConfig().getSearchNamesURL();
	}
	private SearchEngineConfig getSearchEngine()
	{
		// return getMainApplication().getSearchEngine();
		if( null!=fMainConfig )
			return getMainConfig().getSearchEngine();
		else
			return null;
	}
	// Special method that needs to specifically handle
	// fail-safe pass-through mode
	private String getSearchEngineURL()
	{
		if( null!=fMainConfig )
			return getSearchEngine().getSearchEngineURL();
		else
			return getMainApplication().getLastChanceSearchEngineUrlOrNull();
	}
	private String getBaseURL()
	{
		if( null!=fMainConfig )
			return getSearchEngine().getBaseURL();
		else
			return null;
	}

	// This MAY RETURN A NULL!  That's perfectly normal.
	private SearchLogger getSearchLogger()
	{
		// return getMainApplication().getSearchLogger();
		return getMainConfig().getSearchLogger();
	}
	private boolean doSearchLogging()
	{
		SearchLogger logger = getSearchLogger();
		if( logger == null )
			return false;
		else
			return logger.shouldDoSearchLogging();
	}
	private boolean alwaysProxy()
	{
		SearchLogger logger = getSearchLogger();
		if( logger == null )
			return false;
		else
			return logger.alwaysProxy();
	}
	// private boolean doesSearchLoggingRequireProxy()
	private boolean doesSearchLoggingRequireScraping()
	{
		SearchLogger logger = getSearchLogger();
		if( logger == null )
			return false;
		else
			// return logger.doesSearchLoggingRequireProxy();
			return logger.doesSearchLoggingRequireScraping();
	}

	private boolean calculateShouldDoFullProxy()
	{
		// If wer'e foreced to do it, perhaps for testing, then do it
		if( alwaysProxy() )
			return true;
		// If search logging is on, and it needs to proxy to log, then do it
		// if( doSearchLogging() && doesSearchLoggingRequireProxy() )
		if( doSearchLogging() && doesSearchLoggingRequireScraping() )
			return true;
		// There's nothing compelling us to do it
		return false;
	}


//  not sure I need this
//	public boolean doProxyAllSearches()
//	{
//	}

	// Get our unique indentifier
	// this is not valid until the request has been read
	public long getTransactionID()
	{
		final String kFName = "getTransactionID";
		AuxIOInfo tmp = getRequestObject();
		if( tmp != null )
			return tmp.getTransactionID();
		errorMsg( kFName,
			"Unable to get request object, to obtain transaction ID."
			+ " Perhaps the request has not been read yet?"
			+ " Returning 0."
			);
		return 0;
	}

	AuxIOInfo getRequestObject()
	{
		return fRequestInfo;
	}

	AuxIOInfo getResponseObject()
	{
		return fResponseInfo;
	}

	/***
	private Hashtable __getHashMap()
	{
		// return getMainApplication().getHashMap();
		return getMainConfig().getHashMap();
	}
	***/
	private Document getConfigDoc()
	{
		// return getMainApplication().getConfigDoc();
		return getMainConfig().getConfigDoc();
	}

	private static void ___Passwords___() {}
	////////////////////////////////////////////////////////////////////

	// What is the main password
	private String _getConfiguredAdminPwd()
	{
		// return getMainApplication().getAdminPwd();
		// return getMainConfig().getAdminPwd();
		return null;
	}
	private String getRequestAdminPwd()
	{
		final String kFName = "getRequestAdminPwd";
		// Look it up in the CGI fields we have
		String lPwd = fRequestInfo.getScalarCGIFieldTrimOrNull(
			ADMIN_CGI_PWD_FIELD
			);
		// If not there, try the old one
		if( null==lPwd )
		{
			lPwd = fRequestInfo.getScalarCGIFieldTrimOrNull(
				OLD_ADMIN_PWD_FIELD
				);
			if( null != lPwd )
				warningMsg( kFName,
					OLD_ADMIN_PWD_FIELD + " has been deprecated"
					+ " in favor of " + ADMIN_CGI_PWD_FIELD
					);
		}
		// For now, just return it, I don't think we need a warning here
		return lPwd;
	}
	private String getRequestAdminKey()
	{
		final String kFName = "getRequestAdminKey";
		// Look it up in the CGI fields we have
		String outKey = fRequestInfo.getScalarCGIFieldTrimOrNull(
				ADMIN_CGI_PWD_OBSCURED_FIELD
			);
		// For now, just return it, I don't think we need a warning here
		return outKey;
	}

	// This looks at the request object and the config
	// and makes it's decision known in the request object
	private void calculateAndSetAccessLevel()
	{
		final String kFName = "calculateAndSetAccessLevel";
		boolean isKey = false;
		String reqPwd = getRequestAdminPwd();
		String token = reqPwd;
		String reqKey = null;
		// If we have a password
		if( null!=reqPwd )
		{
			isKey = false;
			token = reqPwd;
			reqKey = getMainConfig().passwordToKeyOrNull( reqPwd );
		}
		// Else no password, check for key
		else {
			isKey = true;
			reqKey = getRequestAdminKey();			
			token = reqKey;
		}

		if( null!=reqKey ) {
			// int accessLevel = getMainConfig().passwordToAccessLevel( reqPwd );
			int accessLevel = getMainConfig().tokenToAccessLevel( token, isKey );
			debugMsg( kFName, "access level="+accessLevel + ", isKey="+ isKey + ", token="+reqKey );
			if( accessLevel >= 0 )
				fRequestInfo.setAccessLevel( accessLevel );
			else
				fRequestInfo.setAccessLevel( 0 );
			if( null!=reqPwd )
				fRequestInfo.setAccessPassword( reqPwd );
			fRequestInfo.setAccessKey( reqKey );
		}
	}
	private int getCurrentPasswordLevel() {
		final String kFName = "getCurrentPasswordLevel";
		if( null!=fRequestInfo )
			return fRequestInfo.getAccessLevel();
		errorMsg( kFName,
			"Null fRequestObject, returning -1"
			);
		return -1;
	}
	
	// In order for this to be true:
	// There must be a configured password
	// and a password must have been sent
	// and they must match exactly
	// IS case sensitive
	// both strings are trimmed
	// must not be zero length
	private boolean _x_getHasProperPassword()
	{
		final String kFName = "getHasProperPassword";
		// write code that is a bit more careful than usual

		String confPwd = _getConfiguredAdminPwd();
		if( confPwd == null || confPwd.trim().equals("") )
		{
			warningMsg( kFName, "No system password was configured (or it was empty)."
				+ " Will return FALSE."
				);
			return false;
		}
		String sentPwd = getRequestAdminPwd();
		if( sentPwd == null || sentPwd.trim().equals("") )
		{
			warningMsg( kFName, "No password was submitted (or it was empty)."
				+ " Will return FALSE."
				);
			return false;
		}
//		if( confPwd.length() != sentPwd.length() )
//		    return false;
		boolean result = confPwd.equals( sentPwd );
		if( ! result )
			warningMsg( kFName, "An incorrect password was submitted."
				+ " Will return FALSE but if you see a lot of these warnings"
				+ " you may be experiencing a security issue."
				);
		return result;
	}

	private static void ___misc_Mid_Level_Logic___() {}
	////////////////////////////////////////////////////////////////////

	// For now, return the system wide default context
	// Todo: allow this to be a configurable item
	private String getDefaultContext()
	{
		return SN_DEFAULT_CONTEXT;
	}


	private String getUserQueryField()
	{
		// Will return it trimmed or null
		String qryField = getSearchEngine().getQueryField();
		if( fRequestInfo.getIsGoogleOneBoxSnippetRequest() )
			qryField = GOOGLE_ONEBOX_CGI_QUERY_FIELD_NAME;
			// ^^^ There is also some weirdness in
			// formatAnAlternativeSuggestionRecord() and
			// which field to use
		// Sanity check
		if( qryField == null )
			errorMsg( "getUserQueryField",
				"No search engine query field specified."
				+ " Don't know what field to check for matching search terms."
				+ " Returning null."
				);
		return qryField.trim();
	}

	private String getUserQuery()
	{
		final String kFName = "getUserQuery";

		String qryField = getUserQueryField();
		// Sanity check
		if( qryField == null )
		{
			return null;
			// ??? Warning message already generated by getUserQueryField
		}

		// Look it up in the CGI fields we have
		String outQry = fRequestInfo.getScalarCGIFieldTrimOrNull(
			qryField
			);

		try {
			// If we didn't find it, but we're doing a webmaster suggests, then
			// check the referer field
			if( outQry == null &&
				determineContext().equals(SN_CONTEXT_SNIPPET_SUGGEST)
				)
			{
				AuxIOInfo refererInfo = fRequestInfo.getCompleteRefererInfo();
				// If we got something, check that
				if( refererInfo != null )
				{
					outQry = refererInfo.getScalarCGIFieldTrimOrNull(
						qryField
						);
				}
				// And now we either got it or we didn't
			}
		}
		catch( SpuriousHTTPRequestException e ) {
			infoMsg( kFName, "Got back exception from determineContext(), ignoring. Exception: " + e );
		}

		// For now, just return it, I don't think we need a warning here
		return outQry;
	}
	private String getUserQueryToLower()
	{
		String tmpStr = getUserQuery();
		if( tmpStr != null )
			tmpStr = tmpStr.toLowerCase();
		return tmpStr;
	}







	///////
	//
	// Encode and decode a string for use in a URL.
	//
	///////

	private String urlDecode( String inString )
	{
		return URLDecoder.decode( inString );
	}

	private String urlEncode( String inString )
	{
		return URLEncoder.encode( inString );
	}



	private static void ___Run_Logging___(){}
	//////////////////////////////////////////////////////////////////

	// Handling Errors and warning messages
	// ************* NEW LOGGING STUFF *************

	private static RunLogInterface getRunLogObject()
	// can't access some of impl's extensions with interface reference
	//private static RunLogBasicImpl getRunLogObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogObject();
	}
//	// Return the same thing casted to allow access to impl extensions
//	private static RunLogBasicImpl getRunLogImplObject()
//	{
//		// return RunLogBasicImpl.getRunLogObject();
//		return RunLogBasicImpl.getRunLogImplObject();
//	}

	// New style
	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// New style
	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// New style
	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( kClassName, inFromRoutine );
	}



	// Old style
	private void debugMsg( String inFromRoutine, String inMessage )
	{
		staticDebugMsg( inFromRoutine, inMessage );
	}
	private static void staticDebugMsg(
		String inFromRoutine, String inMessage
		)
	{

		getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);

//		if( debug )
//		{
//			messageLogger( "Debug: " + kClassName + ":" + inFromRoutine + ":"
//				+ inMessage
//				);
//		}
	}
	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
	}

	// New style
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

	// Old style
	private void warningMsg( String inFromRoutine, String inMessage )
	{
		staticWarningMsg( inFromRoutine, inMessage );
	}
	private static void staticWarningMsg(
		String inFromRoutine, String inMessage
		)
	{
		getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);

//		messageLogger( "Warning: " + kClassName + ":" + inFromRoutine + ":"
//			+ inMessage
//			);
	}
	private void errorMsg( String inFromRoutine, String inMessage )
	{
		staticErrorMsg( inFromRoutine, inMessage );
	}
	private static void staticErrorMsg(
		String inFromRoutine, String inMessage
		)
	{
		getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);

//		messageLogger( "Error: " + kClassName + ":" + inFromRoutine + ":"
//			+ inMessage
//			);
	}

	private static boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName, inFromRoutine,
			e, optMessage
			);
	}

	// Newer style
	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}



//	private static void messageLogger( String inMsg )
//	{
//		// If null message, give a real nasty message
//		if( inMsg == null )
//			inMsg = "ERROR: NULL MESSAGE passed to message logger.";
//		if( fLogMsgOutput != null )
//			fLogMsgOutput.println( inMsg );
//		if( fRecentLogMsgs == null )
//			fRecentLogMsgs = new Vector();
//		synchronized (fRecentLogMsgs)
//		{
//			// Add to the list
//			fRecentLogMsgs.add( 0, inMsg );
//			// Truncate the list if needed
//			while( fRecentLogMsgs.size() > KEEP_LOG_MSG_COUNT )
//			{
//				fRecentLogMsgs.remove( fRecentLogMsgs.size()-1 );
//			}
//		}
//	}
//
//	public static String getLastLogMessage()
//	{
//		if( fRecentLogMsgs != null )
//		{
//			synchronized (fRecentLogMsgs)
//			{
//				if( fRecentLogMsgs.size() >= 1 )
//					return (String)fRecentLogMsgs.get(0);
//				else
//					return null;
//				    // return "ERROR: " + kClassName
//					//	+ ":getLastMessage: NO MESSAGES TO REPORT (1)";
//			}
//		}
//		else
//			return null;
//	}
//
//	public static List getLastLogMessages()
//	{
//		if( fRecentLogMsgs != null )
//		{
//			synchronized (fRecentLogMsgs)
//			{
//				return (List)fRecentLogMsgs.clone();
//			}
//		}
//		else
//			return new Vector();
//	}
//
//	public static void clearLastLogMessages()
//	{
//		if( fRecentLogMsgs != null )
//		{
//			synchronized (fRecentLogMsgs)
//			{
//				fRecentLogMsgs.clear();
//			}
//		}
//	}
//
//	public static void setLogMessageOutput( PrintStream inNewStream )
//	{
//		if( inNewStream != null )
//			fLogMsgOutput = inNewStream;
//		else
//			staticErrorMsg( "setLogMessageOutput",
//				"Null print stream passed in."
//				+ " If you're trying to turn off output, use clearLogMessageOutput()"
//				);
//	}
//	public static void clearLogMessageOutput()
//	{
//		if( fLogMsgOutput == null )
//			staticErrorMsg( "clearLogMessageOutput", "Log stream already null." );
//		fLogMsgOutput = null;
//	}


	private static void ___Fields_and_Constants___(){}
	//////////////////////////////////////////////////////////////////

	///////
	//
	// The various things that we track...
	//
	///////

	TimingVector mTiming;

	// The main application
	private SearchTuningApp fMainApp;
	private SearchTuningConfig fMainConfig;
	// ^^^ NO, we get this fresh from http  / app for each transaction

	// The socket we're supposed to handle
	private Socket fHandlerSocket;
	// A stream (from the socket) that we read from
	private InputStream fInputStream;

	// A flag so that the various snippet generating routines and the routines
	// that call them can figure out if ANY of them actually did any
	// work producing snippets.
	// We could return null strings, but some routines still like to produce
	// a non-null HTML comment string, even if they didn't add anything.
	private boolean fSnippetToSend;

	// The main dictionary of terms and url redirect records
	// private Hashtable fHashMap;
	// ^^^ Use getHashMap()

	// The complate request data
	// This is the inbound request from the user
	AuxIOInfo fRequestInfo;
	// The intermediate object used to fetch search results
	AuxIOInfo fIntermediateIoInfo;
	// And the response data
	// This is the outbound response to the user
	AuxIOInfo fResponseInfo;

	// What the high level operation was we were asked to perform
	String mContextCode;

	// A flag if we did a search redirect
	private boolean fDidASearchTermToSpecificURLRedirect;

	// Whether or not we have a log request pending
	private boolean mDoDirectLogAferResponse;
	// private boolean mDoProxyLogAferResponse;
	// vvv Renaming to be more generic, to also serve snippet logging
	private boolean mDoSearchLoggingAferResponse;
	private boolean mWasJustSnippetServe;

	// Some information about the status of SearchNames actions
	// The first, or "primary" action that was taken
	private int fSNActionCode;
	// How many high level actions we took: 3 wm suggest + 4 alt terms = 2
	private int fSNActionCount;
	// How many individual, component bits of info did we convey
	// 3 wm suggest + 4 alt terms = 7
	private int fSNActionItemCount;
	// Error code, if any
	private int fSNStatusCode;
	private String fSNStatusMsg;

	// Information about the Search Engine we'll be interacting with
	// SearchEngineConfig fEngine;
	// ^^^ Use getSearchEngine()

	// ???
	// private String fRequest;
	// Obsolete stuff?
	// private boolean fReadCR = false;
	// private Hashtable fHeaders;


	// Related to messages
	// Where to store recent messages
	private static Vector fRecentLogMsgs;
	// How many messages to keep
	public static final int KEEP_LOG_MSG_COUNT = 10;
	// Where to send messages, left null by default
	private static PrintStream fLogMsgOutput;

	// A newline
	private static final String NL = "\r\n";

	// For tables, formatting, etc.
	// private static final String DEFAULT_TABLE_COLOR = "#ffffcc";  // yellow
	private static final String _DEFAULT_TABLE_COLOR = "#eeeeee"; // gray
	private static final String _DEFAULT_TABLE_WIDTH = "95%";


	// Context items

	// A path prefix we look for
	public static final String FILE_CONTEXT_CGI_PATH_PREFIX = "/file/"; // was "/files/";
	public static final String UI_CONTEXT_CGI_PATH_PREFIX = "/ui/";
	// public static final String UI_CONTEXT_CGI_PATH_PREFIX2_LOGIN = "/admin/";
	// public static final String UI_CONTEXT_CGI_PATH_PREFIX3_LOGIN = "/administration/";
	public static final String LUCENE_SEARCH_CONTEXT_CGI_PATH_PREFIX = "/lucene"; // ".../";

	public static final String GOOGLE_ONEBOX_GET_CONFIG_CONTEXT_CGI_PATH_PREFIX = "/onebox_config";
	public static final String GOOGLE_ONEBOX_SNIPPET_CONTEXT_CGI_PATH_PREFIX = "/onebox";
	// ^^^ See also SN_CONTEXT_GOOGLE_ONEBOX_SEARCH


	public static final String PRINARY_ADMIN_PATH = "/admin";
	public static final Collection K_LOGIN_PATH_ALIASES;
	static {
		K_LOGIN_PATH_ALIASES = new HashSet();
		K_LOGIN_PATH_ALIASES.add( "/ui" );	// with no trailing slash
		K_LOGIN_PATH_ALIASES.add( PRINARY_ADMIN_PATH ); // "/admin"
		K_LOGIN_PATH_ALIASES.add( "/admin/" );
		K_LOGIN_PATH_ALIASES.add( "/admin/login.cgi" );
		K_LOGIN_PATH_ALIASES.add( "/login.cgi" );
		K_LOGIN_PATH_ALIASES.add( "/administration" );
		K_LOGIN_PATH_ALIASES.add( "/administration/" );
		K_LOGIN_PATH_ALIASES.add( "/login" );
		K_LOGIN_PATH_ALIASES.add( "/login/" );
		K_LOGIN_PATH_ALIASES.add( "/reports" );
		K_LOGIN_PATH_ALIASES.add( "/reports/" );
	}


	// The cgi field we look to for context
	// Was nie_sn_context
	public static final String NIE_CONTEXT_REQUEST_FIELD = "nie_context";
	public static final String SN_OLD_CONTEXT_REQUEST_FIELD = "sn_context";

	// The Context in SnRequestionHandler is the general high level command
	// If doing Direct Logging, an ADDITIONAL field is passed in
	// giving the specific transation type we're logging in SearchLogger
	
	// The various values it can have
	// Only the first one is currently supported
	private static final String SN_CONTEXT_SEARCH_REDIR = "sredir";
	// ??? this one also seems to be referenced, in 3 places
	private static final String SN_CONTEXT_PASSTHRU_REDIR = "predir";
	private static final String SN_CONTEXT_DEBUG_REQUEST_ECHO = "echo";
	// We need this next one for testihng
	private static final String SN_CONTEXT_DEBUG_FIXED_HTML_ECHO = "echohtml";
	private static final String SN_CONTEXT_SNIPPET_SUGGEST = "snippet";

	private static final String SN_CONTEXT_GOOGLE_ONEBOX_CONFIG = "onebox_config";
	private static final String SN_CONTEXT_GOOGLE_ONEBOX_SNIPPET = "onebox";
	// ^^^ See also GOOGLE_ONEBOX_SNIPPET_CONTEXT_CGI_PATH_PREFIX
	// Changing this here does NOT change the expected URL path!!!!!!
	
	// This is the MAIN fucntion that SearchTrack performs
	// in most setups
	// Act as a go-between from the web client and the host
	// search engine, possibly markup results, and also parse
	// and log statistics
	private static final String SN_CONTEXT_PROXY = "proxy";

	// Directly log a transaction (vs. trying to proxy it at search time)
	// This is typically a results list click-through on a particular document
	// We immediately issue a redirect to the intended document, close the
	// connectinn, and THEN log the transation
	// Clickthroughs to search engine links are special and are
	// instead handled in clickthrough_proxy, below
	public static final String SN_CONTEXT_DIRECT_LOG_TRANSACTION = "log_event";

	// This is a special hybrid proxy where they would normally be
	// talking to their search engine again, such as to goto page 2
	// of the results or clicking on a facet
	public static final String SN_CONTEXT_CLICKTHROUGH_PROXY = "clickthrough_proxy";

	
	// Lookup a term and see what it is?
	private static final String SN_CONTEXT_LOOKUP = "lookup";
	// they want a static file
	public static final String SN_CONTEXT_FILE = "file";
	// Administration, see below
	public static final String SN_CONTEXT_ADMIN = "admin";
	// real UI
	private static final String SN_CONTEXT_UI = "ui";

	private static final String SN_CONTEXT_LUCENE_SEARCH = "lucene_search";

	// WARGNING!!!!!
	// THESE VARIABLES must also be coordinated with the
	// HARD CODED VARIABLES in the XSLT screen
	// nie.webui.xml_screens.generate_form.xslt
	// IN TWO PLACES ^^^ in this XSLT file
	// What field to look for in GET/POST for a password
	public static final String ADMIN_CGI_PWD_FIELD = "password";
	// The scrambled / obscured / md5 version / aka "key" or "session key"
	public static final String ADMIN_CGI_PWD_OBSCURED_FIELD = "s";

	public static final String OLD_ADMIN_PWD_FIELD = "admin_password";

	private static final int REQUIRED_ADMIN_SECURITY_LEVEL = 3;

//	MOVED to BaseMarkup
//	// This is info we will ADD to the top level data items we pass
//	// The main tag name we will ad
//	public static final String AUX_INFO_TAG_NAME = "transaction_info";
//	// The attribute we will add the query to
//	public static final String AUX_INFO_QUERY_ATTR = "user_query";
//	// The attribute we will add the query to
//	public static final String AUX_INFO_QUERY_NORM_ATTR = "user_query_normalized";
//	// The attribute we will add the query to
//	public static final String AUX_INFO_QUERY_FIELD_NAME_ATTR =
//		"cgi_query_field_name";
//	// The actual transaction ID, based on processed requests
//	public static final String AUX_INFO_TRANS_ID_ATTR = "transaction_id";
//	// The number of items that were presented in this group
//	public static final String AUX_INFO_TOTAL_AD_COUNT_ATTR =
//		"item_group_count";

	// And what to do if we don't find a specific context
	// private static final String SN_DEFAULT_CONTEXT = SN_CONTEXT_SEARCH_REDIR;
	// Change default to proxy
	// This isn't as "bold" as it might sound
	// - Proxy will still recognize redirs and do them
	// - Also, proxy only actually does proxy serving if it is sure it
	//  has something to say
	// So a vast majority of proxy requests actually wind up as one
	// redirect or another
	private static final String SN_DEFAULT_CONTEXT = SN_CONTEXT_PROXY;


	// Administration Context
	// So you could say something like:
	// http://server:9000/?nie_sn_context=admin&cmd=showall
	// biw http://server:9000/?nie_context=admin&cmd=showall
	// ======================================================
	// The cgi field we look to for context, was "cmd"
	public static final String ADMIN_CONTEXT_REQUEST_FIELD = "command";
	// The various admin commands we will accept
	// ping does not require a password
	private static final String ADMIN_CONTEXT_PING = "ping";
	// showall does not require a password
	private static final String ADMIN_CONTEXT_SHOW_COMPLETE_CONFIG = "showall";

	// Show lengthy version and config info
	private static final String ADMIN_CONTEXT_SHOW_FULL_VERSION_INFO = "version";


	private static final String ADMIN_CONTEXT_SHUTDOWN = "shutdown";
	// The following are NOT YET IMPLEMENTED
	private static final String _ADMIN_CONTEXT_LIST_ALL_MAPPINGS = "mappings";
	public static final String ADMIN_CONTEXT_REFRESH = "refresh";
	public static final String ADMIN_CONTEXT_REPORT = "report";
	private static final String ADMIN_CONTEXT_RESAVE = "resave";
	public static final String ADMIN_CONTEXT_SHOW_MESSAGES = "messages";
	private static final String ADMIN_CONTEXT_MUST_LOGIN = "must_login";
	private static final String ADMIN_CONTEXT_DO_LOGIN = "do_login";
	// We do allow a default, show all the config
	private static final String ADMIN_DEFAULT_CONTEXT =
		// ADMIN_CONTEXT_PING
		ADMIN_CONTEXT_MUST_LOGIN
		;



	// for use with SN_CONTEXT_DEBUG_FIXED_HTML_ECHO
	private static final String FIXED_HTML_TO_ECHO =
		"<html><head><title>fixed html text</title></head><body>"
		+ "Will show broken image link that sends request to server."
		+ "<img alt=broken_link width=100 height=100"
		// + " src=\"/?query=ref_test&nie_sn_context=echo\">"
		+ " src=\"/?query=ref_test"
		+ '&' + NIE_CONTEXT_REQUEST_FIELD + '=' + SN_CONTEXT_DEBUG_REQUEST_ECHO
		+ "\">"
		+ "</body></html>"
		;

	// The response to send to a logging request
	private static final String DIRECT_LOG_REQUEST_FIXED_ACK_HTML =
		"<!-- NIE Direct Transaction Log request acknowledgement. -->";
	// What to put between each alternative suggestion, if there is more than one
	public static final String ALT_TERM_SEP = ", ";


	private static final String ADM_SNIPPET =
		"In this mode a few admin commands are still available," + NL
		+ "such as ping, refresh and (show) messages to aid in debugging;" + NL
		+ "in this mode they do not require a password." + NL
		;
	private static final String SYNTAX_SNIPPET =
		"Syntax Remidner Example:" + NL
		+ "http://this-server:port/?"
		+ NIE_CONTEXT_REQUEST_FIELD		// "nie_context"
		+ "="
		+ SN_CONTEXT_ADMIN				// "admin"
		+ "&"
		+ ADMIN_CONTEXT_REQUEST_FIELD	// "command"
		+ "="
		+ ADMIN_CONTEXT_SHOW_MESSAGES	// "messages"
		+ NL
		;


	public static final String HARD_FAILOVER_MSG =
		"Configuration ERROR:" + NL
		+ "The NIE Server is not configured properly." + NL
		+ NL
		+ "The server is currently passing through searches" + NL
		+ "to the host search engine so that users can" + NL
		+ "continue to perform searches." + NL
		+ NL
		+ "However much of the remaining system is down," + NL
		+ "including suggestions, logging, reporting and" + NL
		+ "most of the administration subsystems." + NL
		+ NL
		+ ADM_SNIPPET
		+ NL
		+ "Suggested Actions:" + NL
		+ "Check the error logs and fix the configuration." + NL
		+ "Then restart the server, or issue a refresh command." + NL
		+ "You can also see recent errors with the messages command." + NL
		+ NL
		+ SYNTAX_SNIPPET
		;


	public static final String LIC_FAILOVER_MSG =
		"The NIE Server is not l"
		+ "icen"
		+ "csed properly." + NL
		+ NL
		+ "It is currently passing through searches to the host search engine" + NL
		+ "so that users can continue to perform searches, and the searhes." + NL
		+ "will still be logged during this grace period." + NL
		+ "However the rest of the system is down, and logging will eventually" + NL
		+ "be disabled, unless the li"
		+ "cen" + "se is upd"
		+ "ated." + NL
		+ NL
		+ ADM_SNIPPET
		+ NL
		+ "Suggested Actions:" + NL
		+ "Please check your l"
		+ "icen"
		+ "se, log files and configuration." + NL
		+ NL
		+ SYNTAX_SNIPPET
		;

	public static final String STATIC_FILE_PREFIX =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "static_files/"
		;

	private static boolean _KEEP_HTTP_SOCKETS_OPEN = false;
	
	public static final String GOOGLE_ONEBOX_STATIC_FILE_PREFIX =
		STATIC_FILE_PREFIX
		+ "onebox/"
		;
	public static final String GOOGLE_ONEBOX_DEFINITION_SKELETON_URI =
		GOOGLE_ONEBOX_STATIC_FILE_PREFIX
		+ "definition_skeleton.xml"
		;
	public static final String GOOGLE_ONEBOX_RESULTS_SKELETON_URI =
		GOOGLE_ONEBOX_STATIC_FILE_PREFIX
		+ "results_skeleton.xml"
		;
	// Might do this via an include
	public static final String _GOOGLE_ONEBOX_RESULTS_TEMPLATE_URI =
		GOOGLE_ONEBOX_STATIC_FILE_PREFIX
		// + "results_template.xslt"
		+ "template.xslt"
		;
	// ^^^ We just call it "template" because it's actually pulled into the DEFINTION.xml file vs. the RESULTS
	// although it does actually control the results, but it's spec'd in the def

	// Where in their XML tree do we put our URL, which is based
	// on our config
	public static final String GOOGLE_ONEBOX_DEFINITION_ST_URL_PATH =
		"/onebox/providerURL";
	// Where in the RESULTS xml tree
	public static final String GOOGLE_ONEBOX_RESULTS_ROOT_ELEMENT_NAME =
		"OneBoxResults";
	public static final String GOOGLE_ONEBOX_RESULTS_ROOT_ELEMENT_PATH =
		"/" + GOOGLE_ONEBOX_RESULTS_ROOT_ELEMENT_NAME;

	//  /OneBoxResults/title/urlLink
	public static final String GOOGLE_ONEBOX_RESULTS_ST_URL_PATH =
		GOOGLE_ONEBOX_RESULTS_ROOT_ELEMENT_PATH
		+ "/title/urlLink"
		;

	//	/OneBoxResults/resultCode
	public static final String GOOGLE_ONEBOX_RESULTS_CODE_PATH =
		GOOGLE_ONEBOX_RESULTS_ROOT_ELEMENT_PATH
		+ "/resultCode"
		;

	public static final String GOOGLE_ONEBOX_RESULTS_ERROR_CODE =
		"lookupFailure";

	//	/OneBoxResults/Diagnostics
	public static final String GOOGLE_ONEBOX_RESULTS_MSG_PATH =
		GOOGLE_ONEBOX_RESULTS_ROOT_ELEMENT_PATH
		+ "/Diagnostics"
		;

	// Where to hang results, needs a [1], [2], [3] after it
	// /OneBoxResults/MODULE_RESULT[1], etc...
	public static final String GOOGLE_ONEBOX_RESULTS_HIT_ELEMENT_NAME =
		"MODULE_RESULT";
	public static final String _GOOGLE_ONEBOX_RESULTS_HIT_PATH =
		GOOGLE_ONEBOX_RESULTS_ROOT_ELEMENT_PATH
		+ '/'
		+ GOOGLE_ONEBOX_RESULTS_HIT_ELEMENT_NAME
		;

	// /OneBoxResults/MODULE_RESULT/U
	public static final String GOOGLE_ONEBOX_RESULTS_HIT_HREF_ELEMENT_NAME =
		"U";
	// /OneBoxResults/MODULE_RESULT/Field ... name=
	public static final String GOOGLE_ONEBOX_RESULTS_HIT_FIELD_ELEMENT_NAME =
		"Field";
	public static final String GOOGLE_ONEBOX_RESULTS_HIT_FIELD_NAME_ATTR =
		"name";
	// Google sends us a fixed name
	public static final String GOOGLE_ONEBOX_CGI_QUERY_FIELD_NAME =
		"query";

	public static final String GOOGLE_ONEBOX_CGI_CLIENT_IP_FIELD = "ipAddr";

	static List GOOGLE_ONEBOX_CGI_FIELDS_LIST = null;
	static String [] GOOGLE_ONEBOX_CGI_FIELDS_ARRAY = {
		"apiMaj", "apiMin", "oneboxName", "lang", "dateTime", "authType",
		GOOGLE_ONEBOX_CGI_CLIENT_IP_FIELD  // ipAddr
		};
	static {
		GOOGLE_ONEBOX_CGI_FIELDS_LIST = new Vector();
		for( int i=0; i<GOOGLE_ONEBOX_CGI_FIELDS_ARRAY.length; i++ )
			GOOGLE_ONEBOX_CGI_FIELDS_LIST.add( GOOGLE_ONEBOX_CGI_FIELDS_ARRAY[i] );
	}
	// These are for a regular Google Search
	public static final String _GOOGLE_STYLE_SHEET_CGI_FIELD = "proxystylesheet";
	public static final String _GOOGLE_STYLE_SHEET_DEFAULT_VALUE = "default_frontend";
	public static final String GOOGLE_FRONT_END_CGI_FIELD = "client";
	public static final String GOOGLE_FRONT_END_DEFAULT_VALUE = "default_frontend";

	// Constants used for parsing
	// We list them here because:
	// 1: Easier to track them
	// 2: We use them more than once, as a pattern and then for their length,
	//    so this insures they are consistent
	public static final String HTML_FORM_TAG_START = "<form";
	// Would also match <arbitrary... but hard coding a space would miss tabs
	// There will be a second check for href
	public static final String HTML_ANCHOR_TAG_START = "<a";
	public static final String HTML_TAG_END = ">";
	public static final String HTML_ACTION_ATTR_START = "action=";
	public static final String HTML_HREF_ATTR_START = "href=";
	
	// Chaff

//	private void doSearchOBS( String inURL )
//	{
//		String lURL = "";
//		String lQueryTerms = "";
//		String lSearchURL = "";
//		String lQueryParamName = "";
//		String lSuggestionParamName = "";
//		String lOtherParams = "";
//
//		/////
//		//
//		// Get the query terms (i.e. strip off the '/SEARCH/'
//		// at the beginning).
//		//
//		/////
//
//		if( inURL.length() < 8 )
//			return;
//
//		lURL = inURL.substring( 8 );
//		if( lURL.startsWith( "::" ) )
//		{
//			///////
//			// We're at the beginning of the query terms - find the end of them
//			///////
//
//			int lEndQueryTerms = lURL.substring(2).indexOf( "::") + 2;
//			if( lEndQueryTerms == -1 )
//			{
//				do404Error();
//				return;
//			}
//
//			lQueryTerms = lURL.substring( 2, lEndQueryTerms );
//
//			///////
//			// Found the end of the query terms - now see if we have a url to which
//			// we can submit a real search.
//			///////
//
//			if( lURL.length() < lEndQueryTerms + 5 )
//			{
//				do404Error();
//				return;
//			}
//
//			lURL = lURL.substring( lEndQueryTerms + 5 );
//			int lEndSearchURL = lURL.indexOf( "::" );
//			if( lEndSearchURL == -1 )
//			{
//				do404Error();
//				return;
//			}
//
//			lSearchURL = lURL.substring( 0, lEndSearchURL );
//
//			///////
//			// Now we have the search URL, what is the parameter name that we
//			// use to actually submit the search terms?
//			///////
//
//			if( lURL.length() < lEndSearchURL + 5 )
//			{
//				do404Error();
//				return;
//			}
//
//			lURL = lURL.substring( lEndSearchURL + 5 );
//			int lEndQueryParamName = lURL.indexOf( "::" );
//			if( lEndQueryParamName < 0 )
//			{
//				do404Error();
//				return;
//			}
//
//			lQueryParamName = lURL.substring( 0, lEndQueryParamName );
//
//			///////
//			// If we have another set of "::"s, then that's the parameter
//			// to use in suggesting the destination.
//			///////
//
//			if( lURL.length() > lEndQueryParamName + 3 )
//			{
//				lURL = lURL.substring( lEndQueryParamName + 3 );
//				if( lURL.startsWith( "::" ) )
//				{
//					lURL = lURL.substring( 2 );
//					int lEndSuggestionParamName = lURL.indexOf( "::" );
//					if( lEndSuggestionParamName == -1 )
//					{
//						do404Error();
//						return;
//					}
//					lSuggestionParamName = lURL.substring( 0, lEndSuggestionParamName );
//					if( lURL.length() > lEndSuggestionParamName + 2 )
//						lURL = lURL.substring( lEndSuggestionParamName + 3 );
//					else
//						lURL = "";
//				};
//
//				if( lURL.length() > 0 )
//				{
//					if( lURL.startsWith( "/" ) )
//						lURL = lURL.substring( 1 );
//					lOtherParams = lURL;
//				}
//			}
//
//		// lOtherParam?
//
//			///////
//			// We theoretically have the request parsed now
//			///////
//
//			SnRedirectRecord lSnURL = (SnRedirectRecord)fHashMap.get( lQueryTerms );
//			if( lSnURL == null )
//			{
//				// Todo: Put the redirect to the normal search engine here.
//			}
//			else
//			{
//				String lDestinationURL = lSnURL.getURL();
//				String lRefererURL = (String)fHeaders.get( "HTTP-Referer" );
//				if( (lSnURL.getMethod() == lSnURL.kRedirect) &&
//					(lDestinationURL.compareTo( lRefererURL ) == 0) )
//				{
//					// Call the main search engine because the redirect
//					// would have taken us back to the page they searched
//					// from.
//
//					String lNewURL = lSearchURL + "?" + lQueryParamName + lQueryTerms + lOtherParams;
//					doRedirect( lNewURL );
//
//				}
//				else if( lSnURL.getMethod() == lSnURL.kRedirect )
//				{
//					// Redirect to lDestinationURL because the search terms
//					// matched and it's a redirect.
//
//					doRedirect( lDestinationURL );
//
//				}
//				else
//				{
//					// Build "Webmaster Suggests" query and redirect to search engine
//
//					String lNewURL = lSearchURL + "?" + lQueryParamName + lQueryTerms + lOtherParams;
//					if( lSuggestionParamName.compareTo( "" ) != 0 )
//						lNewURL += lSuggestionParamName + lDestinationURL;
//					lNewURL += lOtherParams;
//					doRedirect( lNewURL );
//				}
//			}
//
//			System.out.println( "Query Terms = '" + lQueryTerms + "'" );
//			System.out.println( "Query Terms URL Encoded = '" + urlEncode( lQueryTerms ) + "'" );
//			System.out.println( "Search Submission URL = '" + lSearchURL + "'" );
//			System.out.println( "Search Parameter name = '" + lQueryParamName + "'" );
//			System.out.println( "Suggestion Param Name = '" + lSuggestionParamName + "'" );
//			System.out.println( "Other Params = '" + lOtherParams + "'" );
//		}
//	}

//	///////
//	//
//	// Read the headers in from the input stream
//	//
//	///////
//
//	private void readHeaders()
//	{
//		String lHeaderLine;
//		String lInputBuffer = new String();
//
//		while( (lHeaderLine = readLineFromSocket()) != null )
//		{
//			lHeaderLine = lHeaderLine.trim();
//			if( lHeaderLine.length() > 0 )
//			{
//				int lColonIndex = lHeaderLine.indexOf( ':' );
//
//				String lHeaderTag = lHeaderLine.substring( 0, lColonIndex );
//				String lValue = lHeaderLine.substring( lColonIndex + 1 ).trim();
//				while( (lValue.charAt( 0 ) == ' ' ) ||
//					   (lValue.charAt( 0 ) == '\t') )
//				{
//					lValue = lValue.substring( 1 );
//				}
//				fHeaders.put( lHeaderTag, lValue );
//			} else {
//				return;
//			} // end if( lheaderLine.length)
//		} // end while
//	} // end run
//
//	///////
//	//
//	// Read in the body of the request
//	//
//	///////
//
//	private void readBody()
//	{
//		//		System.out.println( "Read body." );
//	}

	///////
	//
	// Read in a line from the socket.  It will correctly handle lines
	// that are terminated by carriage returns, carriage return line feeds
	// line feeds, or intermixed combinations of the above.
	//
	// A line is terminated by either a carriage return or a line feed.
	// Upon next call to this routine, if a line feed is read, we check
	// whether the previous line was terminated by a carriage return.
	// If the previous line WAS terminated by a carriage return, then discard
	// the line feed as the previous line was actually terminated by  a CRLF.
	// If the previous line was NOT terminated by a CR, then we truly did read
	// in a blank line.
	//
	///////
//	public String readLineFromSocket( )
//	{
//		/////
//		// Values we track
//		//
//		// lInputStream is the socket's stream from which we read
//		// lLine is the buffer in which we store characters read from lInputStream until we have a line read in.
//		/////
//
//		String lLine = "";
//
//		try {
//			fInputStream = fHandlerSocket.getInputStream();
//
//			/////
//			// We exit this loop internally when we read in eol or eos
//			/////
//
//			while( true )
//			{
//				/////
//				// Get the next character and check if it's EOS
//				/////
//
//				int lReadInt = fInputStream.read();
//				if( lReadInt == -1 )
//				{
//					/////
//					// EOS read. If the line is empty, return null (not an empty line).
//					// If the line is not empty, return the line.
//					/////
//
//					if( lLine == "" )
//						return null;
//					else
//						return lLine;
//				}
//
//				/////
//				// Check if we read a possible EOL
//				/////
//
//				if( lReadInt == '\n' )
//				{
//					/////
//					// Read new line in - check if this is a
//					// continuation of a CRLF EOL
//					/////
//
//					if( !fReadCR )
//						return lLine;
//
//				} else if( lReadInt == '\r' ) {
//					fReadCR = true;
//					return lLine;
//				} else {
//					fReadCR = false;
//					lLine += (char)lReadInt;
//				}
//			}  // End while true
//		}
//		catch( IOException eIO ) { return lLine; }
//	}


}

