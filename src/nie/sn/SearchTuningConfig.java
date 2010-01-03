package nie.sn;

import java.io.*;
import java.util.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.lang.reflect.*;
import nie.webui.UILink;
import nie.core.*;
import nie.lucene.LuceneConfig;
import nie.lucene.LuceneRequestDispatcher;
// import nie.sn.*;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Attribute;
import org.jdom.JDOMException;
import org.jdom.xpath.*;

// A class to encapsulate all global config data.
// This will allow the main app to swap out configs easily
final public class SearchTuningConfig
{



	private final static String kClassName = "SearchTuningConfig";


	public String getConfigurationBanner()
	{
		final String nl = NIEUtil.NL;

		String banner = "Listening on port: " + getPort() + nl;
		banner += "NIE Server URL: " + getSearchNamesURL() + nl;
		banner += "Search engine: " + getSearchEngineURL();// + nl;
		banner += " via " + getSearchEngineMethod() + nl;
		banner += "# of Mappings: " + getHashMapCount() + nl;
		banner += "# of Declared User Markup Classes: " + getUserClassesHashMapCount() + nl;
		if( getSearchLogger() != null )
			banner += "Enabled option: Logging Searches" + nl;
		else
			banner += "Unconfigured option: NOT configured for Logging Searches" + nl;
		banner += "Configuration loaded at: " + fCreationTimestamp;
		return banner;
	}


	// private static boolean debug = true;
	// Don't specifically set it if you want FALSE, it'll default to that
//	private static boolean debug;
//	public static void setDebug( boolean flag )
//	{
//		debug = flag;
//	}

	public SearchTuningConfig(
		String inConfigFileURI,
		SearchTuningApp optApp
		)
		throws SearchTuningConfigException,
			SearchTuningConfigFatalException
	{
		final String kFName = "constructor(1)";
		fCreationTimestamp = NIEUtil.getTimestamp();

		// if( null == optApp )
		//	throw new SearchTuningConfigException(
		warningMsg( kFName,
				"Null application passed in."
				);
		// Store a reference to the main application
		fApp = optApp;

		fConfigFileURI = NIEUtil.trimmedStringOrNull( inConfigFileURI );
//		if( fConfigFileURI == null )
//		    if(debug) System.err.println(
//				"Will use default config file name " + DEFAULT_CONFIG_FILE_URI
//				);
//			throw new SearchTuningConfigException(
//				"Constructor was passed a null config URI."
//				);

		// Read the config file in
		loadConfigFile();

		finishInit();

		/***
		// Parse "global" options in the config file
		readGlobalOptions();

		// Setup any user defined data / markup definitions
		readAndSetupMarkupDeclarations();

		// Setup the "mappings"
		readAndSetupMapping();

		// Add temp aux status info to main XML tree
		storeStatusInfo();
		***/

	}


	public SearchTuningConfig(
		JDOMHelper inConfigTree,
		String optConfigFileURI,
		SearchTuningApp optApp
		)
		throws SearchTuningConfigException,
			SearchTuningConfigFatalException
	{
		final String kFName = "constructor(2)";
		fCreationTimestamp = NIEUtil.getTimestamp();

		// if( null == optApp )
		//	throw new SearchTuningConfigException(
		warningMsg( kFName,
				"Null application passed in."
				);
		// Store a reference to the main application
		fApp = optApp;

		if( null!=optConfigFileURI ) {
			fConfigFileURI = NIEUtil.trimmedStringOrNull( optConfigFileURI );
			File tmpFile = new File( optConfigFileURI );
			if( null!=tmpFile )
				fFullConfigFileURI = tmpFile.getAbsolutePath();
		}

		fOverallMasterTree = inConfigTree;


//		if( fConfigFileURI == null )
//			if(debug) System.err.println(
//				"Will use default config file name " + DEFAULT_CONFIG_FILE_URI
//				);
//			throw new SearchTuningConfigException(
//				"Constructor was passed a null config URI."
//				);

		finishInit();

		/***
		// Parse "global" options in the config file
		readGlobalOptions();

		// Setup any user defined data / markup definitions
		readAndSetupMarkupDeclarations();

		// Setup the "mappings"
		readAndSetupMapping();

		// Add temp aux status info to main XML tree
		storeStatusInfo();
		***/

	}

	private void finishInit()
		throws SearchTuningConfigException,
			SearchTuningConfigFatalException
	{
		// Look at the overall JDOM tree, find the major parts, etc.
		digestOverallConfigTree();

		// Parse "global" options in the config file
		readGlobalOptions();

		// Setup any user defined data / markup definitions
		readAndSetupMarkupDeclarations();

		// Setup the "mappings"
		readAndSetupMapping();

		// Add temp aux status info to main XML tree
		storeStatusInfoIntoConfigTree();
	}


	// We go through a little hog wash to set the subtree as the main tree
	// Otherwise we'd have to keep carrying around a prefix.
	private void digestOverallConfigTree()
		throws SearchTuningConfigException
	{
		final String kFName = "digestOverallConfigTree";

		if( fOverallMasterTree == null )
			throw new SearchTuningConfigException( "No overall config tree, nothing to do." );

		// Setup System logging, if any
		// Todo: this will override the command line, probably should be
		// the other way around
		RunLogInterface tmpLogger =
			(RunLogInterface) fOverallMasterTree.makeObjectFromConfigPathOrNull(
				"nie.core.RunLogBasicImpl", RUN_LOG_CONFIG_PATH
				);
		if( tmpLogger == null )
			statusMsg( kFName,
				NIEUtil.NL
				+ "FYI: No Run Logging specified in this configuration"
				+ "; process status information will continue to be sent to this log."
				);

//		try
//		{
//			RunLogInterface tmpLogger =
//			(RunLogInterface) fOverallMasterTree.makeObjectFromConfigPath(
//				"nie.core.RunLogBasicImpl", RUN_LOG_CONFIG_PATH
//				);
//		}
//		catch(JDOMHelperException e1)
//		{
//			String tmpMsg =
//				"No run logging defined in the config file."
//				+ " (Details: \"" + e1 + "\")"
//				;
//			statusMsg( kFName, tmpMsg );
//			gErrorMessage =
//				"Unable to instantiate logger, which is required for JSP apps."
//				+ " Reason: \"" + e1 + "\""
//				;
//			throw new SRConfigException( kExTag
//				 + gErrorMessage
//				 );
//		}


		Element tmpSNElem = fOverallMasterTree.findElementByPath(
			MAIN_SN_CONFIG_PATH
			);
		// Fallback for older syntax
		if( null == tmpSNElem )
		{
			tmpSNElem = fOverallMasterTree.findElementByPath(
				OLD_MAIN_SN_CONFIG_PATH
				);
			// If using the old syntax, warn them
			if( null != tmpSNElem )
				warningMsg( kFName,
					"You are still using the old/deprecated config element \""
					+ OLD_MAIN_SN_CONFIG_PATH
					+ "\"; you should be using \""
					+ MAIN_SN_CONFIG_PATH
					+ "\" element, which is thew new, supported syntax."
					);
		}
		if( tmpSNElem == null )
			throw new SearchTuningConfigException(
				"Unable to find Search Names specific config info"
				+ " in the main configuration file."
				+ " Expected node path = \"" + MAIN_SN_CONFIG_PATH + "\""
				+ " From config file \"" + getConfigFileURI() + "\""
				);
		fConfigTree = null;
		try
		{
			fConfigTree = new JDOMHelper( tmpSNElem );
		}
		catch (JDOMHelperException e)
		{
			throw new SearchTuningConfigException(
				"Error loading config file (2)."
				+ " JDOMHelperException: " + e
				);
		}
		if( fConfigTree == null )
			throw new SearchTuningConfigException(
				"Got back a NULL xml tree from Search Names node."
				+ " Expected node path = \"" + MAIN_SN_CONFIG_PATH + "\""
				+ " From config file \"" + getConfigFileURI() + "\""
				);

	}






	// We go through a little hog wash to set the subtree as the main tree
	// Otherwise we'd have to keep carrying around a prefix.
	private void loadConfigFile()
		throws SearchTuningConfigException
	{

		final String kFName = "loadConfigFile";

		if( fConfigFileURI == null )
		{
			fConfigFileURI = DEFAULT_CONFIG_FILE_URI;
			statusMsg( kFName,
				"No specific configuration file was set."
				+ " Will assume default config file name " + fConfigFileURI
				);
		}


		fOverallMasterTree = null;
		try
		{
			// fOverallMasterTree = new JDOMHelper( fConfigFileURI );

			// Instead, call the JDOMHelper constructor that allows for <include>
			// tags.  We're the top level caller, level=0, and there was no parent
			// of the config file to search against
			// fOverallMasterTree = new JDOMHelper( fConfigFileURI, null, 0 );

			// We'd like to know the final URI
			AuxIOInfo auxInfo = new AuxIOInfo();
			fOverallMasterTree = new JDOMHelper(
				fConfigFileURI, null, 0, auxInfo
				);
			// Store the final, absolute URI we wound up with
			fFullConfigFileURI = auxInfo.getFinalURI();

		}
		catch (JDOMHelperException e)
		{
			throw new SearchTuningConfigException(
				"Error loading config file (1)."
				+ " JDOMHelperException: " + e
				);
		}
		if( fOverallMasterTree == null )
			throw new SearchTuningConfigException(
				"Got back a NULL xml tree"
				+ " from file \"" + fConfigFileURI + "\""
				);

		// debugMsg( kFName, "Have now read config file \"" + fConfigFileURI + "\"." );
		debugMsg( kFName, "Have now read config file \"" + getConfigFileURI() + "\"." );


		/***
		// Setup System logging, if any
		// Todo: this will override the command line, probably should be
		// the other way around
		RunLogInterface tmpLogger =
			(RunLogInterface) fOverallMasterTree.makeObjectFromConfigPathOrNull(
				"nie.core.RunLogBasicImpl", RUN_LOG_CONFIG_PATH
				);
		if( tmpLogger == null )
			statusMsg( kFName,
				NIEUtil.NL
				+ "FYI: No Run Logging specified in this configuration"
				+ "; process status information will continue to be sent to this log."
				);

//		try
//		{
//			RunLogInterface tmpLogger =
//			(RunLogInterface) fOverallMasterTree.makeObjectFromConfigPath(
//				"nie.core.RunLogBasicImpl", RUN_LOG_CONFIG_PATH
//				);
//		}
//		catch(JDOMHelperException e1)
//		{
//			String tmpMsg =
//				"No run logging defined in the config file."
//				+ " (Details: \"" + e1 + "\")"
//				;
//			statusMsg( kFName, tmpMsg );
//			gErrorMessage =
//				"Unable to instantiate logger, which is required for JSP apps."
//				+ " Reason: \"" + e1 + "\""
//				;
//			throw new SRConfigException( kExTag
//				 + gErrorMessage
//				 );
//		}


		Element tmpSNElem = fOverallMasterTree.findElementByPath(
			MAIN_SN_CONFIG_PATH
			);
		// Fallback for older syntax
		if( null == tmpSNElem )
		{
			tmpSNElem = fOverallMasterTree.findElementByPath(
				OLD_MAIN_SN_CONFIG_PATH
				);
			// If using the old syntax, warn them
			if( null != tmpSNElem )
				warningMsg( kFName,
					"You are still using the old/deprecated config element \""
					+ OLD_MAIN_SN_CONFIG_PATH
					+ "\"; you should be using \""
					+ MAIN_SN_CONFIG_PATH
					+ "\" element, which is thew new, supported syntax."
					);
		}
		if( tmpSNElem == null )
			throw new SearchTuningConfigException(
				"Unable to find Search Names specific config info"
				+ " in the main configuration file."
				+ " Expected node path = \"" + MAIN_SN_CONFIG_PATH + "\""
				+ " From config file \"" + getConfigFileURI() + "\""
				);
		fConfigTree = null;
		try
		{
			fConfigTree = new JDOMHelper( tmpSNElem );
		}
		catch (JDOMHelperException e)
		{
			throw new SearchTuningConfigException(
				"Error loading config file (2)."
				+ " JDOMHelperException: " + e
				);
		}
		if( fConfigTree == null )
			throw new SearchTuningConfigException(
				"Got back a NULL xml tree from Search Names node."
				+ " Expected node path = \"" + MAIN_SN_CONFIG_PATH + "\""
				+ " From config file \"" + getConfigFileURI() + "\""
				);

		***/

	}



	private void initDB()
	{
		final String kFName = "initDB";
		final String kExTag = kClassName + '.' + kFName + ": ";

		Element lDBConfElem =
			fOverallMasterTree.findElementByPath(
				DB_CONFIG_PATH
				);
		if( null==lDBConfElem ) {
			// statusMsg( kFName, "Looking for old database config..." );

			lDBConfElem = fOverallMasterTree.findElementByPath(
				OLD_DB_CONFIG_PATH
				);
			if( null!=lDBConfElem )
				warningMsg( kFName,
					"You are using a deprecated XML config path for database configuration."
					+ " Correct path=/(rootelement)/" + DB_CONFIG_PATH
					+ " You're using the older path=/(rootelement)/" + OLD_DB_CONFIG_PATH
					);
		}

		// It's OK if no DB, just let them know
		if( null==lDBConfElem ) {
			// Make sure we don't see an old one
			fDBConfig = null;
			statusMsg( kFName,
				"No Database configuration was found."
				+ " Checked /(rootelement)/" + DB_CONFIG_PATH
				+ " and legacy /(rootelement)/" + OLD_DB_CONFIG_PATH
				// + " Config ="
				// + JDOMHelper.JDOMToString(fOverallMasterTree.getJdomElement(), true )
				);
			return;
		}

		// Now check for, instantiate, and store the dbconfig section
		// This uses the new JDOMHelper object-by-path factory
		try
		{
			// fDBConfig = (DBConfig)fConfigTree.makeObjectFromConfigPath(
			//	"nie.core.DBConfig", DB_CONFIG_PATH
			//	);
			fDBConfig = new DBConfig( lDBConfElem, this );
		}
		catch(Exception dbe)
		{
			String tmpMsg = "Unable to instantiate database configuration."
				+ " This may be caused by a simple database configuration error."
				+ " Reason/Exception = \"" + dbe + "\""
				;
	
			errorMsg( kFName, tmpMsg );
			errorMsg( kFName,
				"*** Even though the database is not reachable"
				+ ", we will still allow the Search Track Server"
				+ " to process searches, so the site will REMAIN UP."
				+ " However searches will likely NOT be logged"
				+ " and other operations requiring the database will not available. ***"
				);
		}
	}




	private void readGlobalOptions()
		throws SearchTuningConfigException,
			SearchTuningConfigFatalException
	{
		final String kFName = "readGlobalOptions";
		final String kExTag = kClassName + '.' + kFName + ": ";

		final String msg = "B"
			+ "ad" + ' ' + "l"
			+ "ic " + "fi"
			+ 'e' + "ld" + ':' + ' ';
		if( null==fOverallMasterTree )
			throw new SearchTuningConfigException( kExTag +
				"Missing/invalid Config."
				);
		Element myElem = fOverallMasterTree.findElementByPath( LNODE );
		if( null==myElem ) throw new SearchTuningConfigException( kExTag + kExTag + msg + LNODE );
		StringBuffer buff = new StringBuffer();
		String r = myElem.getAttributeValue( LRV );
		r = NIEUtil.trimmedStringOrNull( r );
		if( null==r || ! r.equals("1") ) throw new SearchTuningConfigException( kExTag + msg + LRV );
		buff.append(r).append('-');
		fCo = myElem.getAttributeValue( LCO );
		fCo = NIEUtil.trimmedStringOrNull( fCo );
		if( null==fCo ) throw new SearchTuningConfigException( kExTag + msg + '(' + 1 + ") " + LCO );
		int ccnt = 0;
		for( int i=0; i<fCo.length(); i++ ) {
			char c = fCo.charAt( i );
			if( (c>='a' && c<='z') || (c>='0' && c<='9') ) {
				buff.append(c);
				ccnt++;
			}
			else if( c>='A' && c<='Z' ) {
				buff.append( Character.toLowerCase(c) );
				ccnt++;
			}
		}
		if( ccnt<3 ) throw new SearchTuningConfigException( kExTag + msg + '(' + 2 + ") " + LCO );
		fStStr = myElem.getAttributeValue( LST );
		fStStr = NIEUtil.trimmedStringOrNull( fStStr );
		// if( null==fStStr ) throw new SearchTuningConfigException( kExTag + msg + '(' + 1 + ") " + LST );
		if( null!=fStStr ) {
			try {
				DateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy z");
				if( ! fStStr.toLowerCase().endsWith(" gmt") )
					fStStr += " GMT";
				fSt = fmt.parse(fStStr);
			} catch (ParseException e1) {
				throw new SearchTuningConfigException( kExTag + msg + '(' + 2 + ") " + LST + ": \"" + fStStr + "\" err: " + e1 );
			}
			buff.append('-').append( fSt.getTime() );
		}
		else {
			buff.append("-nst");
		}
		fNdStr = myElem.getAttributeValue( LND );
		fNdStr = NIEUtil.trimmedStringOrNull( fNdStr );
		if( null==fNdStr ) throw new SearchTuningConfigException( kExTag + msg + '(' + 1 + ") " + LND );
		try {
			DateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy z");
			if( ! fNdStr.toLowerCase().endsWith(" gmt") )
				fNdStr += " GMT";
			fNd = fmt.parse(fNdStr);
		} catch (ParseException e2) {
			throw new SearchTuningConfigException( kExTag + msg + '(' + 2 + ") " + LND + ": \"" + fNdStr + "\" err: " + e2 );
		}
		buff.append('-').append( fNd.getTime() );
		fSrv = myElem.getAttributeValue( LSRV );
		fSrv = NIEUtil.trimmedStringOrNull( fSrv );
		buff.append('-');
		if( null!=fSrv ) {
			ccnt = 0;
			for( int i=0; i<fSrv.length(); i++ ) {
				char c = fSrv.charAt( i );
				if( (c>='a' && c<='z') || (c>='0' && c<='9') ) {
					buff.append(c);
					ccnt++;
				}
				else if( c>='A' && c<='Z' ) {
					buff.append( Character.toLowerCase(c) );
					ccnt++;
				}
			}
			if( ccnt<1 ) throw new SearchTuningConfigException( kExTag + msg + '(' + 2 + ") " + LSRV );
		}
		else {
			buff.append( "ns" );
		}
		buff.append('-');
		int sce1 = 1; int sce2 = 1;
		for( int i=0; i<20; i++ ) {
			buff.append(sce1);
			int tmp = sce2; sce2 += sce1; sce1 = tmp;
		}

		// statusMsg( kFName, "lstr=\"" + buff + "\"" );

		final int kyln = 4 * 5;
		String xky = myElem.getAttributeValue( LKY );
		xky = NIEUtil.trimmedStringOrNull( xky );
		if( null==xky ) throw new SearchTuningConfigException( kExTag + msg + '(' + 1 + "): " + LKY );
		StringBuffer buff2 = new StringBuffer();
		for( int i=0; i<xky.length(); i++ ) {
			char c = xky.charAt( i );
			if( (c>='a' && c<='f') || (c>='0' && c<='9') ) {
				buff2.append(c);
			}
			else if( c>='A' && c<='F' ) {
				buff2.append( Character.toLowerCase(c) );
			}
		}
		if( buff2.length() != kyln ) throw new SearchTuningConfigException( kExTag + msg + '(' + 2 + "): " + LKY );
		String cky2 = null;
		try {
			Chap cp = new Chap();
			int [] cky1 = cp.sign( new ByteArrayInputStream( (new String(buff)).getBytes()) );
			cky2 = cp.md5string(cky1);
		}
		catch( IOException e ) {
			throw new SearchTuningConfigException( kExTag + msg + '(' + 3 + "): " + LKY + ": " + e );
		}
		final int st = 7;
		if( null==cky2 || cky2.length() < (st + kyln) )
			throw new SearchTuningConfigException( kExTag + msg + '(' + 4 + "): " + LKY + ": " + st + '/' + kyln + '/' + ( null==cky2 ? -1 : cky2.length() ) );
		cky2 = cky2.substring( st-1, st+kyln-1 );

		// statusMsg( kFName, "cky2=\"" + cky2 + "\" (" + cky2.length() + ')' );

		if( ! cky2.equals( new String(buff2) ) )
			throw new SearchTuningConfigException( kExTag + msg + '(' + 5 + "): " + LKY + ": " + (null!=fNd ? fNd.getTime() : -1) );
		if( fNd.getTime() + LOGGRC < (new java.util.Date()).getTime() ) {
			fLicFull = false;
			throw new SearchTuningConfigException( kExTag + "L"
				+ "IC" + "ENS"
				+ "E HA" + 'S' + " EX" + "PIR" + "ED"
				+ '!' + '!' + '!'
				+ " conta" + "ct sal" + "es@id" + "eaen" + "g.co" + 'm'
				);
		}
		if( fNd.getTime() + SUGGRC < (new java.util.Date()).getTime() ) {
			fLicFull = false;
			errorMsg( kFName, "L"
				+ "IC" + "ENS"
				+ "E HA" + 'S' + " EX" + "PIR" + "ED"
				+ '!' + '!' + '!'
				+ " Su" + "ges" + "tio" + "ns an" + "d Rep" + "orts DI" + "SABL" + "ED"
				+ "; wil" + "l sti" + 'l' + "l lo" + "g se" + "arch" + "es fo" + "r a wh" + "ile lon" + "ger."
				+ " conta" + "ct sal" + "es@id" + "eaen" + "g.co" + 'm'
				);
		}
		else {
			fLicFull = true;
		}



		// Turn off Caching
		fUseCache = false;

		// DB has to come first, in case config data comes from there
		initDB();

		String lSnURL = getSearchNamesURL();
		if( null == lSnURL )
			throw new SearchTuningConfigException(
				"Missing/invalid Search Names URL."
				+ " You must provide a valid URL to this process."
				);
		// cache this as well
		// getSearchNamesTestDriveURL();

		int lPortNumber = getPort();
		if( lPortNumber < 1 )
		{
			throw new SearchTuningConfigException(
				"Invalid port number " + lPortNumber
				+ " Port number must be a positive integer."
				+ " Some operating systems also place restrictions on access"
				+ " to lower port numbers, or may have other port number restrictions."
				);
		}
		else
		{
			debugMsg( kFName, "Does have a postive port " + lPortNumber );
		}

		// See if they have a legacy port specified by the deprecated attr
		int lLegacyPortNumber = getLegacyPort();
		// Yes they do
		// We need to either warn them, or if it does not match, give an error!
		if( lLegacyPortNumber > 0 )
		{
			if( lLegacyPortNumber != lPortNumber )
				throw new SearchTuningConfigException(
					"Conflicting port numbers specified."
					+ " Port number from Search Names URL = " + lPortNumber
					+ " Port number from Legacy attribute = " + lLegacyPortNumber
					+ " The legacy port attribute \"" + SN_PORT_ATTR + "\""
					+ " has been deprecated; but if it is specified, it must agree"
					+ " with the port number found in main URL \"" + lSnURL + "\""
					+ " (or the default for that protocol, for example http=80)."
					);
			// Else they do agree, but still warn
			warningMsg( kFName,
					"Configuration is still using the deprecated port attribute \""
					+ SN_PORT_ATTR + "\""
					+ ", though it does match port number found in"
					+ " in the main URL \"" + lSnURL + "\""
					+ " (or the default for that protocol, for example http=80)."
					+ " The application will run as configured on port " + lPortNumber
					+ " but this deprecated/legacy attribute should be removed."
					);
		}
		else
		{
			debugMsg( kFName, "No Legacy port specified." );
		}

		// Setup passwords, etc.
		initPasswordTable();
		// TODO: also check md5 login key
		if( null == getAdminPwd() )
		{
			warningMsg( kFName,
				"No administration password was set."
				+ " You will not be able to perform administration with"
				+ " a web browser or issue a shutdown command;"
				+ " in this mode you will have to control-C or kill the process."
				+ " Reminder: You can set a password in the "
				+ ADMIN_PWD_ATTR + " attribute of the "
				+ MAIN_SN_CONFIG_PATH + " tag."
				+ " Will continue with initialization."
				);
		}
		else
			debugMsg( kFName, "Does have an admin password." );

		String cssText = getDefaultCssStyleTextOrNull();
		if( null==cssText )
			errorMsg( kFName,
				"Unable to get system-wide CSS style sheet."
				+ " The application will continue to initialize, but may look very odd."
				);

		// Settings for null searches
		getNullSearchMarker();  // currently a const but could change
		getNullSearchRedirURL();  // may return NULL_REDIR_TO_REFERRER_MARKER
		// vvv also calls ^^^ but harmless to leave it here
		shouldRedirectNullSearches();
		getNullPhraseEquivList();
		
		// Turn ON field/member Caching
		fUseCache = true;


		// Find the search engine info and instantiate it
		Element lSEConfElem = fConfigTree.findElementByPath( SEI_CONFIG_PATH );
		// No need to check for null, the constructor we're about
		// to call will throw an exception for that
		// Instantiate it
		try
		{
			fSearchEngineConfig = new SearchEngineConfig( lSEConfElem );
		}
		catch (SearchEngineConfigException e)
		{
			throw new SearchTuningConfigException(
				"Unable to find/create host Search Engine's config info."
				+ " Reason/exception: \"" + e + "\""
				+ " Configuration reminder: it's \"" + SEI_CONFIG_PATH + "\""
				+ " under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
				+ " Reaading from config file \"" + getConfigFileURI() + "\""
				);
		}


		// See if there's any search logging to be done
		// Note that this is relative to the OVERALL CONFIG ROOT
		// It's OK if there isn't
		Element lSearchTrackingConfElem =
			fOverallMasterTree.findElementByPath(
				SEARCH_TRACKING_CONFIG_PATH
				);
		// Fallback for older syntax
		if( null == lSearchTrackingConfElem )
		{
			lSearchTrackingConfElem = fOverallMasterTree.findElementByPath(
				OLD_SEARCH_TRACKING_CONFIG_PATH
				);
			// If using the old syntax, warn them
			if( null != lSearchTrackingConfElem )
				warningMsg( kFName,
					"You are still using the old/deprecated config element \""
					+ OLD_SEARCH_TRACKING_CONFIG_PATH
					+ "\"; you should be using \""
					+ SEARCH_TRACKING_CONFIG_PATH
					+ "\" element, which is the new, supported syntax."
					);
		}

		// If there is some search tracking (logging and/or reporting)
		if( null != lSearchTrackingConfElem )
		{

			// See if there's any search logging to be done
			// Note that this is relative to the OVERALL CONFIG ROOT
			// It's OK if there isn't
			Element lSearchLogConfElem = JDOMHelper.findElementByPath(
				lSearchTrackingConfElem, SEARCH_LOGGER_CONFIG_PATH
				);
			// If there is, set things up
			if( lSearchLogConfElem != null )
			{
				// No need to check for null, the constructor we're about
				// to call will throw an exception for that
				// Instantiate it
				try
				{
					fSearchLogger = new SearchLogger(
						lSearchLogConfElem,
						this
						);
				}
				catch (SearchLoggerException e)
				{
					throw new SearchTuningConfigException(
						"Unable to create configured Search Logger."
						+ " Reason/exception: \"" + e + "\""
						+ " Configuration reminder:"
						+ " it's \"" + SEARCH_LOGGER_CONFIG_PATH + "\""
						+ " under \"" + SEARCH_TRACKING_CONFIG_PATH + "\""
						+ ", which is under the main node."
						+ " Reaading from config file \"" + getConfigFileURI() + "\""
						);
				}

				// We also check for OVERRIDE config to the default
				// search reporting.
				//
				// If you have logging, then you AUTOMATICALLY have reports
				//
				// If you put report settings, you're just overriding
				// the defaults for reports
				//
				// You can NOT have reports without logging, because
				// the logging defines the database connection.


				// OK, so we are doing search logging, let's also
				// allow reports
				// Todo: Revisit, not happy with search logger and
				// reporting hanging off of the same config tree but
				// in their own little worlds
				// and should handle exceptions at some point

				// See if there's reporting
				// but either way we DO create a dispatcher

				Element lReportsConfElem = JDOMHelper.findElementByPath(
					lSearchTrackingConfElem, SEARCH_REPORTS_CONFIG_PATH
					);

				// NULL is OK
				try
				{
					// Get a report config option, null will give default
					fReportDispatcher = new nie.sr2.ReportDispatcher(
						lReportsConfElem,
						this
						// getMainApplication()
						);
	
				}
				catch (nie.sr2.ReportConfigException e)
				{
					throw new SearchTuningConfigException(
						"Unable to create configured Search Reporting."
						+ " Reason/exception: \"" + e + "\""
						+ " Configuration reminder:"
						+ " it's \"" + SEARCH_REPORTS_CONFIG_PATH + "\""
						+ " under \"" + SEARCH_TRACKING_CONFIG_PATH + "\""
						+ ", which is under the main node."
						+ " Reaading from config file \"" + getConfigFileURI() + "\""
						);
				}

			}	// End if search logging config was not null
			else
			{
				statusMsg( kFName,
					NIEUtil.NL
					+ "FYI: No Search / query logging or reporting"
					+ " was specified in this configuration"
					+ "(1); user searches will not be logged for Search Tracking."
					+ " Configuration reminder:"
					+ " it's \"" + SEARCH_LOGGER_CONFIG_PATH + "\""
					+ " under \"" + SEARCH_TRACKING_CONFIG_PATH + "\""
					+ ", which is under the main node."
					+ " Reaading from config file \"" + getConfigFileURI() + "\""
					);
			}

		}
		// Else no search tracking logging or reporting specified
		else
		{
			statusMsg( kFName,
				NIEUtil.NL
				+ "FYI: No Search / query logging or reporting"
				+ " was specified in this configuration"
				+ "(2); user searches will not be logged for Search Tracking."
				+ " Configuration reminder: it's \"" + SEARCH_TRACKING_CONFIG_PATH + "\""
				+ " under the main node."
				+ " Reaading from config file \"" + getConfigFileURI() + "\""
				);
		}


		// Setup file dispatcher, for now no config
		// Todo: at some point make this configurable, etc.
		// Where the files are, mime types, etc.
		fFileDispatcher = new FileDispatcher(
			null,	// The jdom helper config snippet
			this
			// getMainApplication()
			);

		// Setup UI Request dispatcher, for now no config
		// Todo: at some point make this configurable, etc.
		try {
			fUIDispatcher = new nie.webui.UIRequestDispatcher(
				null,	// The jdom helper config snippet
				this
				);
		}
		catch (nie.webui.UIException e3) {
			throw new SearchTuningConfigException(
				"Unable to initiate UI config info."
				+ " Reason/exception: \"" + e3 + "\""
				);
		}

		// Setup Lucene search
		Element lLuceneConfElem = fOverallMasterTree.findElementByPath(
			LUCENE_SEARCH_CONFIG_PATH
			);
		if( null!=lLuceneConfElem ) {
		    // Need to double check

		    String tmpPath = JDOMHelper.getTextByPathTrimOrNull(
		            lLuceneConfElem, LuceneConfig.INDEX_LOCATION_FILE_PATH
					);
		    if( null!=tmpPath ) {
			    // statusMsg( kFName, "lLuceneConfElem="+lLuceneConfElem);
				try {
					fLuceneDispatcher = new nie.lucene.LuceneRequestDispatcher(
						lLuceneConfElem,	// The jdom helper config snippet
						this
						);
					statusMsg( kFName, "FYI: Lucene Searching is enabled." );
				}
				catch (nie.lucene.LuceneConfigException e4) {
					throw new SearchTuningConfigException(
						"Unable to initiate built-in Search engine config info."
						+ " Reason/exception: \"" + e4 + "\""
						);
				}
		    }
		    else {
		        statusMsg( kFName, "FYI: No Lucene Searching configured. (1)" );
		    }
		}
		else {
			statusMsg( kFName, "FYI: No Lucene Searching configured. (2)" );
		}


		// statusMsg( kFName, "machine" );

		// Keep these for the exception
		String lName = null;
		String lDesc = null;
		try {
			String keyMachine = fSrv;

			String stURL = getSearchNamesURL();
			URL tmpURL = new URL( stURL );
			String stMachine = tmpURL.getHost();
			// statusMsg( kFName, NIEUtil.NL +
			//	"stMachine=" + stMachine );

			// What we pull from variuos sources here
			HashSet candidateIPs = new HashSet();
			HashSet candidateIPNames = new HashSet();
			// what the licensed server resolves to, if any
			HashSet licIPs = null;

			// For each machine the Search Engine URL resolves to
			debugMsg( kFName, "Checking SearchTrack URL Server IPs for \"" + stMachine + "\"" );
			lName = stMachine;
			lDesc = "Sea" + "rchT" + "rack U" + "RL ma" + "chin" + "e na" + "me";
			InetAddress[] myAddrs = InetAddress.getAllByName( lName );
			candidateIPNames.add( stMachine );
			for( int i=0; i<myAddrs.length; i++ ) {
				InetAddress ip = myAddrs[i];
				debugMsg( kFName, NIEUtil.NL + "\t" + (i+1) + ": " + ip.getHostName() + " / " + ip.getHostAddress() );
				candidateIPs.add( ip.getHostAddress() );
			}

			// For each machine found with local host
			InetAddress lip = InetAddress.getLocalHost();
			String lhMachine = lip.getHostName();
			candidateIPNames.add( lhMachine );
			debugMsg( kFName, "Checking Local Host Server IPs for \"" + lhMachine + "\"" );
			lName = lhMachine;
			lDesc = "Loc" + "al " + "H" + "ost ma" + "chin" + "es";
			myAddrs = InetAddress.getAllByName( lName );
			for( int i=0; i<myAddrs.length; i++ ) {
				InetAddress ip = myAddrs[i];
				debugMsg( kFName, NIEUtil.NL + "\t" + (i+1) + ": " + ip.getHostName() + " / " + ip.getHostAddress() );
				candidateIPs.add( ip.getHostAddress() );
			}
			// TODO: Java 1.4 has java.net.NetworkInterface

			statusMsg( kFName, /*NIEUtil.NL +*/ "Locally found IP(s): " + candidateIPNames + "=" + candidateIPs );

			// If there is a licensed machine
			if( null!=keyMachine ) {
				debugMsg( kFName, "Checking Licensed server IPs for \"" + keyMachine + "\"" );
				licIPs = new HashSet();
				lName = keyMachine;
				lDesc = "K" + "e" + "y" + " ma" + "chin" + "es";
				myAddrs = InetAddress.getAllByName( lName );
				for( int i=0; i<myAddrs.length; i++ ) {
					InetAddress ip = myAddrs[i];
					debugMsg( kFName, NIEUtil.NL + "\t" + (i+1) + ": " + ip.getHostName() + " / " + ip.getHostAddress() );
					licIPs.add( ip.getHostAddress() );
				}
				statusMsg( kFName, /*NIEUtil.NL +*/ "License IP(s) for \"" + keyMachine + "\": " + licIPs );

				// Here we perform a soft check
				HashSet intersect = new HashSet();
				intersect.addAll( candidateIPs );
				intersect.retainAll( licIPs );
				// if( true ) {
				if( licIPs.isEmpty() ) {
					errorMsg( kFName,
						"Invalid License: "
						+ "License's machine \"" + keyMachine + "\" does not have a matching IP address for this system."
						+ NIEUtil.NL
						+ "Licenced server=\"" + keyMachine + " with IP(s) " + licIPs + "."
						+ NIEUtil.NL
						+ "Local address(es) =\"" + candidateIPNames + " with IP(s) " + candidateIPs + "."
						+ NIEUtil.NL
						+ " Will still start server, but please contact sales@ideaeng.com"
						);
				}
			}

			//	String sURL = getSearchEngineURL();
			//	URL tmpURL = new URL( sURL );
			//	String searchMachine = tmpURL.getHost();
			//	statusMsg( kFName, NIEUtil.NL +
			//		"searchMachine=" + searchMachine );
			//	DBConfig db = getDBConfig();
			//	String dbMachine = db.getServerString();
			//	statusMsg( kFName, NIEUtil.NL +
			//		"dbMachine=" + dbMachine );

			// InetAddress ip = InetAddress.getLocalHost();
			// String ipMachine = ip.getHostName();
			// String ipMachine2 = ip.getHostAddress();

			//	myAddrs = InetAddress.getAllByName( "localhost" );
			//	for( int i=0; i<myAddrs.length; i++ ) {
			//		InetAddress ip2 = myAddrs[i];
			//		System.err.println(
			//			"\t" + (i+1) + ": " + ip2.getHostName() + " / " + ip2.getHostAddress()
			//			);
			//	}


		}
		catch( Exception e ) {
			// e.printStackTrace( System.err );
			warningMsg( kFName,
				"Problem with IP for " + lDesc + " = \"" + lName + "\""
				+ " This may be related to a typo in the config, or a DNS server, or license key field."
				+ " Exception: " + e
				);
		}



		debugMsg( kFName, "Have now read Config." );

	}


	private void readAndSetupMarkupDeclarations()
	{
		final String kFName = "readAndSetupMarkupDeclarations";

		// Init the hash, no matter what
		fUserClassHashMap = new Hashtable();

		List decls = fConfigTree.findElementsByPath( USER_DATA_DECLARATION_PATH );
		if( decls == null || decls.size() < 1 )
		{
			statusMsg( kFName,
				NIEUtil.NL
				+ "FYI: No User Data markup declarations or Ad declarations found in config file."
				);
			return;
		}

		infoMsg( kFName,
			"Loading " + decls.size() + " user markup declaration nodes."
			+ " Reminders:"
			+ " You can use debug or trace mode to see more details"
			+ " about what's being loaded."
			+ " Or, once the server is up and running,  you can"
			+ " use your web browser to do the admin showall command;"
			+ " that may be easier than reading through this log."
			);

		// For each mapping / declaration statement
		int lDeclCount = 0;
		for( Iterator it1 = decls.iterator(); it1.hasNext(); )
		{
			// Get this map
			Element declElem = (Element)it1.next();
			lDeclCount++;

			// Get some basic info about this element
			// How the user will refer to this
			String userClassName = BaseMarkup.getUserAliasClass(
				declElem
				);
			// Sanity check
			if( null == userClassName )
			{
				errorMsg( kFName,
					"User Marktup # " + lDeclCount
					+ " does not have a class name assigned."
					+ " Will continue trying to load other markup definitions."
					);
				continue;
			}
			// See if this is a duplicate
			if( fUserClassHashMap.containsKey( userClassName ) )
			{
				errorMsg( kFName,
					"User Marktup # " + lDeclCount
					+ " is a duplicate definition for class name"
					+ " \"" + userClassName + "\", ignoring this duplication."
					+ " Will continue trying to load other markup definitions."
					);
				continue;
			}

			// The real Java Class
			String className = BaseMarkup.getRequestedJavaClass(
				declElem
				);
			// Sanity check
			if( null == className )
			{
				errorMsg( kFName,
					"User Marktup # " + lDeclCount
					+ " does not have a Java class assigned."
					+ " Will continue trying to load other markup definitions."
					);
				continue;
			}


			// Lots of things can go wrong when we try to instantiate
			// these classes and call the constructor
			try
			{

				// Get the class they're asking for
				Class theClass = Class.forName( className );
			
				// Look for a constructor that takes an Element
				// and an application pointer (by which can also get config)

				// When you ask for a constructor, you need an array
				// of object types, in this case a two item array
				Class argTypes[] = {
					Class.forName( "org.jdom.Element" ),
					// Class.forName( "nie.sn.SearchTuningApp" ),
					Class.forName( "nie.sn.SearchTuningConfig" )
					};
				// Lookup the constructor
				Constructor cons = theClass.getConstructor( argTypes );
				
				// Now we will call the constructor

				// Prepare the arguments for the constructor
				// Object args[] = { declElem, this };
				// Object args[] = { declElem, getMainApplication(), this };
				Object args[] = { declElem, this };
				// Call the constructor
				BaseMarkup answer = (BaseMarkup)cons.newInstance( args );

				// OK, we did it!, go ahead and store it
				// This is the object instance we will call when we
				// have data items of this user class
				fUserClassHashMap.put( userClassName, answer );

			}
			catch( Exception e )
			{
				errorMsg( kFName,
					"User Marktup # " + lDeclCount
					+ ", user class \"" + userClassName + "\""
					+ " referencing Java Class \"" + className + "\""
					+ ", could not be instantiated."
					+ " Error: " + e
					+ " Will continue trying to load other markup definitions."
					);
				continue;
			}			

			statusMsg( kFName,
				"Loaded Markup/Ad declaration for user class \""
				+ userClassName
				+ "\", Java implementing class \""
				+ className + "\""
				);
			
		}	// End for each declaration
			
	}





	public void readAndSetupMapping()
		throws SearchTuningConfigException
	{
		final String kFName = "readAndSetupMapping";
		final String kExTag = kClassName + '.' + kFName + ": ";

		Element tmpDBMap = fConfigTree.findElementByPath( DB_MAP_TOP_PATH );
		boolean hasDBMaps = null!=tmpDBMap;
		DBConfig tmpDB = getDBConfig();
		boolean hasDB = null!=tmpDB;

		if( hasDBMaps && ! hasDB )
			throw new SearchTuningConfigException( kExTag
				+ "Database repository indicated, but DB is unreachable (or not configured)."
				);


		Element tmpElem = fConfigTree.findElementByPath( FIXED_MAP_HEAD );
		List tmpMaps = (null!=tmpElem) ? fConfigTree.findElementsByPath( FIXED_MAP_PATH ) : null;
		// boolean hasStaticMaps = null!=tmpMaps && tmpMaps.size() > 0;
		boolean hasStaticMaps = null!=tmpMaps; // && tmpMaps.size() > 0;

		if( hasDBMaps && hasStaticMaps )
			throw new SearchTuningConfigException( kExTag
				+ "Can't have both XML/static maps and Database repository maps."
				);

		statusMsg( kFName,
			"" + NIEUtil.NL + "Loading Directed Search Results Maps, *** PLEASE WAIT *** ..."
			);

		// Hashtable newTermHashMap = null;
		Hashtable [] newHashes;
		if( hasDBMaps ) {
			cIsDbBased = true;
			// newTermHashMap = readAndSetupMappingFromDatabase();
			newHashes = readAndSetupMappingFromDatabase();
		}
		else if( hasStaticMaps ) {
			// newTermHashMap = readAndSetupMappingFromXML();
			newHashes = readAndSetupMappingFromXML();
		}
		else
			throw new SearchTuningConfigException( kExTag
				+ "Must have XML/static maps or Database repository maps."
				+ " (If starting with an empty XML map, add at least one map.)"
				);

		// installNewMap( newTermHashMap );
		installNewMaps( newHashes );
	}

	// Look for mapped statements and create redirect objects for each
	// private Hashtable readAndSetupMappingFromDatabase()
	// Change to ID based records
	private Hashtable [] readAndSetupMappingFromDatabase()
		throws SearchTuningConfigException
	{
		final String kFName = "readAndSetupMappingFromDatabase";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==getDBConfig() )
			throw new SearchTuningConfigException( kExTag
				+ "No database configured to load maps from."
				);

		boolean trace = shouldDoTraceMsg( kFName );

		// Init the hash, no matter what
		// Given a term, what map ID's have it
		Hashtable newTermToMapIDs = new Hashtable();
		// Given a map ID, give me the real map object
		Hashtable newMapIDToMapObject = new Hashtable();
		// Given a map ID, tell me which terms it has
		Hashtable newMapIDToMapTerms = new Hashtable();

		// tracking ID's
		// fMapsByIDHash = new Hashtable();


		// where does site ID come from????

		// Get the maps from the DB server
		String qry = "SELECT id FROM nie_map";
		// TODO: Join with site table and also check for active status from there

		//ResultSet mapRecords = getDBConfig().runQueryOrNull( qry );
		Object [] objs = getDBConfig().runQueryOrNull( qry, true );

		// if( mapRecords == null )
		if( null==objs )
			throw new SearchTuningConfigException( kExTag
				+ "Got back Null results set when querying database."
				);
		ResultSet mapRecords = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];


		int lRowCount = 0;
		int lMapCount = 0;

		try {
	
			// For each mapping record
			while( mapRecords.next() )
			{
				lRowCount++;

				/***
				Object tmpObj = mapRecords.getObject(1);
				if( null==tmpObj ) {
					warningMsg( kFName, "Null map ID for record " + lRowCount + ", ignoring." );
					continue;
				}
	
				// Instantiate an object
				// We do this via an Element to maintain consistency with other map types
				String key = tmpObj.toString();
				Element mapElem = new Element( "map" );
				mapElem.setAttribute( "id", key );
				***/

				int id = mapRecords.getInt(1);

				debugMsg( kFName, "Processing map ID " + id );

				Element mapElem = new Element( "map" );
				mapElem.setAttribute( "id", ""+id );

	
				MapRecordInterface map = null;
				try
				{
					map = new DbMapRecord( this, mapElem, id );
				}
				catch (MapRecordException e)
				{
					errorMsg( kFName,
						"Unable to create map # " + lMapCount + "."
						+ " Reason: " + e
						+ " Will continue trying to load other map entries."
						);
					continue;
				}

				lMapCount++;

				// Get the map's ID, if any
				int mapID = map.getID();
				if( mapID < 1 )
				{
					errorMsg( kFName,
						"Invalid map ID " + mapID + " for map # " + lMapCount + "."
						+ " Will continue trying to load other map entries."
						);
					continue;
				}

				// Store the object
				newMapIDToMapObject.put( new Integer(mapID), map );
	
				// Now store it in the global hash
				// fMapsByIDHash.put( new Integer(mapID), mapElem );
				// fMapsByIDHash.put( new Integer(mapID), map );
	
				// For each term in the mapping statement
				// Get the terms for this map
				List terms = map.getTerms();

				// Sanity check
				if( terms == null || terms.size() < 1 )
				{
					warningMsg( kFName,
						"No search terms found in map # " + lMapCount + "."
						+ " Maybe you're planning to add some interactively?"
						+ " (Todo: Not yet implemented.)"
						+ " Will continue to read in the remainder of the redirect maps."
						// + " There should be at least one \"" + SEARCH_TERM_PATH + "\" tag"
						// + " under each mapping tag \"" + FIXED_MAP_PATH + "\""
						// + " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
						// + " Reaading from config file \"" + fConfigFileURI + "\""
						);
					newMapIDToMapObject.remove( new Integer(mapID) );
					continue;
				}

				// Store a copy of the list of terms
				// I prefer to copy over, since the orig list is mutable
				List tmpList = new Vector();
				tmpList.addAll( terms );
				newMapIDToMapTerms.put( new Integer(mapID), tmpList );

	
				debugMsg( kFName,
					"Map # " + lMapCount
					+ " has " + terms.size() + " terms."
					);
	
	
				//////////////////////////////////////////////
				//
				// Associate the various "data items"
				// with terms
				//
				//////////////////////////////////////////////
	

				traceMsg( kFName, "Will get conbimed count." );
				// int combinedCount = map.getURLObjectsCount()
				//	+ map.getAlternateTermsCount() + map.getUserDataItemsCount()
				//	;
				traceMsg( kFName, "Will get url count." );
				int urlsCount = map.getURLObjectsCount();
				traceMsg( kFName, "Back, url count = " + urlsCount );

				traceMsg( kFName, "Will get alt terms count." );
				int relatedTermsCount = map.getAlternateTermsCount();
				traceMsg( kFName, "Back, alt terms count = " + relatedTermsCount );

				traceMsg( kFName, "Will get user data count." );
				int userItemsCount = map.getUserDataItemsCount();
				traceMsg( kFName, "Back, user data count = " + userItemsCount );

				int combinedCount = urlsCount + relatedTermsCount + userItemsCount;

				traceMsg( kFName, "Back, conbimed count = " + combinedCount );
	
				// Sanity check
				if( combinedCount <= 0 )
				{
					warningMsg( kFName,
						"No URLs, alternative terms or data items found in map # "
						+ lMapCount + "."
						+ " Maybe you're planning to add some interactively?"
						+ " (Todo: Not yet implemented.)"
						+ " Will skip all the terms in this mapped keyword set,"
						+ " but will continue to read in the remainder of the redirect maps."
						);
					// Skip to the next map element
					continue;
				}
	
	
	
				// We will now associate this map element to every
				// term that it contains
				
	
				// Loop through terms
				for( Iterator it2 = terms.iterator(); it2.hasNext(); )
				{
					// Grab the term and normalize it
					String term = (String)it2.next();
					term = term.toLowerCase();
	
					if(trace) traceMsg( kFName,
						"Mapping individual search term \"" + term + "\""
						+ " to " + combinedCount
						+ " URLs/Alt terms"
						);
	
	
					// Now add it to the big map!
					// We associate a term to the map that it was found in
					// At some point we may allow a term to appear in more
					// than one map
	
					// pointer to placeholder list
					List mapIDVector = null;
	
					// If we have an existing vector for this term, grab it
					if( newTermToMapIDs.containsKey( term ) )
					{
						mapIDVector = (List)newTermToMapIDs.get( term );
						if(trace) traceMsg( kFName,
							"Term \"" + term + "\" already had " + mapIDVector.size()
							+ " vector entries; these new ones will be added."
							);
					}
					// Otherwise, create a fresh new vector for it
					else
					{
						mapIDVector = new Vector();
						if(trace) traceMsg( kFName,
							"New term \"" + term + "\" vector."
							);
					}
	
					// Now add this new record to the end of the vector
					mapIDVector.add( new Integer(mapID) );
	
					// And update the hash with the new/revised vector
					newTermToMapIDs.put( term, mapIDVector );
	
				}   // End for each Term
	
			}   // End for each mapping statement
			// fTermHashMap

		}
		catch( SQLException sqle ) {
			throw new SearchTuningConfigException( kExTag +
				"Error getting map data from database: " + sqle
				);
		}
		finally {
			mapRecords = DBConfig.closeResults( mapRecords, kClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
		}

		if( lMapCount < 1 )
			warningMsg( kFName, "No mappings were loaded; perhaps this is a new configuration?" );

		infoMsg( kFName,
			"Finished, now have " + newTermToMapIDs.size() + " individual mapped search terms."
			);

		return new Hashtable [] {
			newTermToMapIDs, newMapIDToMapObject, newMapIDToMapTerms
			};
	}
	
	// Look for mapped statements and create redirect objects for each
	private Hashtable readAndSetupMappingFromDatabaseOBS()
		throws SearchTuningConfigException
	{
		final String kFName = "readAndSetupMappingFromDatabaseOBS";
		final String kExTag = kClassName + '.' + kFName + ": ";

		boolean trace = shouldDoTraceMsg( kFName );

		// Init the hash, no matter what
		Hashtable newTermHashMap = new Hashtable();

		// tracking ID's
		// fMapsByIDHash = new Hashtable();


		// where does site ID come from????

		// Get the maps from the DB server
		String qry = "SELECT id FROM nie_map";
		// TODO: Join with site table and also check for active status from there

		//ResultSet mapRecords = getDBConfig().runQueryOrNull( qry );
		Object [] objs = getDBConfig().runQueryOrNull( qry, true );

		// if( mapRecords == null )
		if( null==objs )
			throw new SearchTuningConfigException( kExTag
				+ "Got back Null results set when querying database."
				);
		ResultSet mapRecords = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];


		int lRowCount = 0;
		int lMapCount = 0;

		try {
	
			// For each mapping record
			while( mapRecords.next() )
			{
				lRowCount++;

				/***
				Object tmpObj = mapRecords.getObject(1);
				if( null==tmpObj ) {
					warningMsg( kFName, "Null map ID for record " + lRowCount + ", ignoring." );
					continue;
				}
	
				// Instantiate an object
				// We do this via an Element to maintain consistency with other map types
				String key = tmpObj.toString();
				Element mapElem = new Element( "map" );
				mapElem.setAttribute( "id", key );
				***/

				int id = mapRecords.getInt(1);

				debugMsg( kFName, "Processing map ID " + id );

				Element mapElem = new Element( "map" );
				mapElem.setAttribute( "id", ""+id );

	
				MapRecordInterface map = null;
				try
				{
					map = new DbMapRecord( this, mapElem, id );
				}
				catch (MapRecordException e)
				{
					errorMsg( kFName,
						"Unable to create map # " + lMapCount + "."
						+ " Reason: " + e
						+ " Will continue trying to load other map entries."
						);
					continue;
				}
				lMapCount++;
	
	
				// Get the map's ID, if any
				int mapID = map.getID();
	
				// Now store it in the global hash
				// fMapsByIDHash.put( new Integer(mapID), mapElem );
				// fMapsByIDHash.put( new Integer(mapID), map );
	
				// For each term in the mapping statement
				// Get the terms for this map
				List terms = map.getTerms();
	
				// Sanity check
				if( terms == null || terms.size() < 1 )
				{
					warningMsg( kFName,
						"No search terms found in map # " + lMapCount + "."
						+ " Maybe you're planning to add some interactively?"
						+ " (Todo: Not yet implemented.)"
						+ " Will continue to read in the remainder of the redirect maps."
						// + " There should be at least one \"" + SEARCH_TERM_PATH + "\" tag"
						// + " under each mapping tag \"" + FIXED_MAP_PATH + "\""
						// + " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
						// + " Reaading from config file \"" + fConfigFileURI + "\""
						);
					continue;
				}
	
				debugMsg( kFName,
					"Map # " + lMapCount
					+ " has " + terms.size() + " terms."
					);
	
	
				//////////////////////////////////////////////
				//
				// Associate the various "data items"
				// with terms
				//
				//////////////////////////////////////////////
	

				traceMsg( kFName, "Will get conbimed count." );
				// int combinedCount = map.getURLObjectsCount()
				//	+ map.getAlternateTermsCount() + map.getUserDataItemsCount()
				//	;
				traceMsg( kFName, "Will get url count." );
				int urlsCount = map.getURLObjectsCount();
				traceMsg( kFName, "Back, url count = " + urlsCount );

				traceMsg( kFName, "Will get alt terms count." );
				int relatedTermsCount = map.getAlternateTermsCount();
				traceMsg( kFName, "Back, alt terms count = " + relatedTermsCount );

				traceMsg( kFName, "Will get user data count." );
				int userItemsCount = map.getUserDataItemsCount();
				traceMsg( kFName, "Back, user data count = " + userItemsCount );

				int combinedCount = urlsCount + relatedTermsCount + userItemsCount;

				traceMsg( kFName, "Back, conbimed count = " + combinedCount );
	
				// Sanity check
				if( combinedCount <= 0 )
				{
					warningMsg( kFName,
						"No URLs, alternative terms or data items found in map # "
						+ lMapCount + "."
						+ " Maybe you're planning to add some interactively?"
						+ " (Todo: Not yet implemented.)"
						+ " Will skip all the terms in this mapped keyword set,"
						+ " but will continue to read in the remainder of the redirect maps."
						);
					// Skip to the next map element
					continue;
				}
	
	
	
				// We will now associate this map element to every
				// term that it contains
				
	
				// Loop through terms
				for( Iterator it2 = terms.iterator(); it2.hasNext(); )
				{
					// Grab the term and normalize it
					String term = (String)it2.next();
					term = term.toLowerCase();
	
					if(trace) traceMsg( kFName,
						"Mapping individual search term \"" + term + "\""
						+ " to " + combinedCount
						+ " URLs/Alt terms"
						);
	
	
					// Now add it to the big map!
					// We associate a term to the map that it was found in
					// At some point we may allow a term to appear in more
					// than one map
	
					// pointer to placeholder list
					List termURLVector = null;
	
					// If we have an existing vector for this term, grab it
					if( newTermHashMap.containsKey( term ) )
					{
						termURLVector = (List)newTermHashMap.get( term );
						if(trace) traceMsg( kFName,
							"Term \"" + term + "\" already had " + termURLVector.size()
							+ " vector entries; these new ones will be added."
							);
					}
					// Otherwise, create a fresh new vector for it
					else
					{
						termURLVector = new Vector();
						if(trace) traceMsg( kFName,
							"New term \"" + term + "\" vector."
							);
					}
	
					// Now add this new record to the end of the vector
					termURLVector.add( map );
	
					// And update the hash with the new/revised vector
					newTermHashMap.put( term, termURLVector );
	
				}   // End for each Term
	
			}   // End for each mapping statement
			// fTermHashMap

		}
		catch( SQLException sqle ) {
			throw new SearchTuningConfigException( kExTag +
				"Error getting map data from database: " + sqle
				);
		}
		finally {
			mapRecords = DBConfig.closeResults( mapRecords, kClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
		}

		if( lMapCount < 1 )
			warningMsg( kFName, "No mappings were loaded; perhaps this is a new configuration?" );

		infoMsg( kFName,
			"Finished, now have " + newTermHashMap.size() + " individual mapped search terms."
			);

		return newTermHashMap;
	}
	



	// Look for mapped statements and create redirect objects for each
	private Hashtable [] readAndSetupMappingFromXML()
	{
		final String kFName = "readAndSetupMappingFromXML";

		// Init the hash, no matter what
		Hashtable newTermHashMap = new Hashtable();

		// The max counter
		fMaxIDSeen = 0;

		// tracking ID's
		// fMapsByIDHash = new Hashtable();

		// We currently only support FIXED maps

		List maps = fConfigTree.findElementsByPath( FIXED_MAP_PATH );
		if( maps == null || maps.size() < 1 )
		{
			warningMsg( kFName,
				"No redirect mappings found in config file."
				+ " Maybe you're planning to add some interactively?"
				+ " (Todo: Not yet implemented.)"
				+ " Will still listen on port until shut down."
				+ " Config path should be \"" + FIXED_MAP_PATH + "\""
				+ " under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
				+ " Reaading from config file \"" + getConfigFileURI() + "\""
				);
			return null;
		}

		infoMsg( kFName,
			"Loading " + maps.size() + " map nodes."
			+ " Reminders:"
			+ " You can use debug or trace mode to see more details"
			+ " about what's being loaded."
			+ " Or, once the server is up and running,  you can"
			+ " use your web browser to do the admin showall command;"
			+ " that may be easier than reading through this log."
			);

		// Currently, every term and URL combo is turned into
		// a unique record

		// For each mapping statement
		int lMapCount = 0;
		for( Iterator it1 = maps.iterator(); it1.hasNext(); )
		{
			// Get this map
			Element mapElem = (Element)it1.next();
			lMapCount++;

			// SnMapRecord map = null;
			MapRecordInterface map = null;
			try
			{
				// map = new SnMapRecord( mapElem );
				map = new XmlMapRecord( this, mapElem, -1 );
			}
			catch (MapRecordException e)
			{
				errorMsg( kFName,
					"Unable to create map # " + lMapCount + "."
					+ " Reason: " + e
					+ " Will continue trying to load other map entries."
					);
				continue;
			}


			// Get the map's ID, if any
			int mapID = map.getID();

			// some sanity checking
			if( mapID > 0 && mapID <= fMaxIDSeen )
				warningMsg( kFName,
					"Map # " + lMapCount
					+ " has a pre-assigned ID of " + mapID
					+ " which may already be in use."
					+ " Will asign new map ID to it and continue."
					);

			// Assign a new ID if needed
			// No need to store it for now
			if( mapID < 0 || mapID <= fMaxIDSeen )
			{
				mapID = ++fMaxIDSeen;
				// The old records had to have their id set manually
				((XmlMapRecord) map).setID( mapID );
			}
			else
			{
				fMaxIDSeen = mapID;
			}

			// Now store it in the global hash
			// fMapsByIDHash.put( new Integer(mapID), mapElem );

			// For each term in the mapping statement
			// Get the terms for this map
			List terms = map.getTerms();

			// Sanity check
			if( terms == null || terms.size() < 1 )
			{
				warningMsg( kFName,
					"No search terms found in map # " + lMapCount + "."
					+ " Maybe you're planning to add some interactively?"
					+ " (Todo: Not yet implemented.)"
					+ " Will continue to read in the remainder of the redirect maps."
					// + " There should be at least one \"" + SEARCH_TERM_PATH + "\" tag"
					// + " under each mapping tag \"" + FIXED_MAP_PATH + "\""
					// + " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
					// + " Reaading from config file \"" + fConfigFileURI + "\""
					);
				continue;
			}

			debugMsg( kFName,
				"Map # " + lMapCount
				+ " has " + terms.size() + " terms."
				);


			//////////////////////////////////////////////
			//
			// Associate the various "data items"
			// with terms
			//
			//////////////////////////////////////////////


//			// Get the urls for this map
//			List urlElems = map.getURLObjects();
//			debugMsg( kFName, "It has " + urlElems.size() + " URLs." );
//
//			// Get the alternative term elements for this map
//			List altTerms = map.getAlternateTerms();
//			debugMsg( kFName, "It has " + altTerms.size() + " alternate terms." );
//
//			// Get the user defined data elements for this map
//			List userDataItems = map.getUserDataItems();
//			debugMsg( kFName, "It has " + userDataItems.size() + " user data items." );
//
//			// The combined count
//			int combinedCount = urlElems.size() + altTerms.size()
//				+ userDataItems.size();

			int combinedCount = map.getURLObjectsCount()
				+ map.getAlternateTermsCount() + map.getUserDataItemsCount()
				;

			// Sanity check
			if( combinedCount <= 0 )
			{
				warningMsg( kFName,
					"No URLs, alternative terms or data items found in map # "
					+ lMapCount + "."
					+ " Maybe you're planning to add some interactively?"
					+ " (Todo: Not yet implemented.)"
					+ " Will skip all the terms in this mapped keyword set,"
					+ " but will continue to read in the remainder of the redirect maps."
//					+ " There should be at least one \"" + URL_PATH + "\" tag"
//					+ " and/or one \"" + ALTTERM_PATH + "\" tag"
//					+ " under each mapping tag \"" + FIXED_MAP_PATH + "\""
//					+ " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//					+ " Reaading from config file \"" + fConfigFileURI + "\""
					);
				// Skip to the next map element
				continue;
			}



			// We will now associate this map element to every
			// term that it contains
			

			// Loop through terms
			for( Iterator it2 = terms.iterator(); it2.hasNext(); )
			{
				// Grab the term and normalize it
				String term = (String)it2.next();
				term = term.toLowerCase();

				traceMsg( kFName,
					"Mapping individual search term \"" + term + "\""
					+ " to " + combinedCount
					+ " URLs/Alt terms"
					);


				// Now add it to the big map!
				// We associate a term to the map that it was found in
				// At some point we may allow a term to appear in more
				// than one map

				// pointer to placeholder list
				List termURLVector = null;

				// If we have an existing vector for this term, grab it
				if( newTermHashMap.containsKey( term ) )
				{
					termURLVector = (List)newTermHashMap.get( term );
					traceMsg( kFName,
						"This term already had " + termURLVector.size()
						+ " vector entries; these new ones will be added."
						);
				}
				// Otherwise, create a fresh new vector for it
				else
				{
					termURLVector = new Vector();
					traceMsg( kFName,
						"This is a new term vector."
						);
				}

				// Now add this new record to the end of the vector
				termURLVector.add( map );

				// And update the hash with the new/revised vector
				newTermHashMap.put( term, termURLVector );

			}   // End for each Term

		}   // End for each mapping statement
		// fTermHashMap

		infoMsg( kFName,
			"Finished, now have " + newTermHashMap.size() + " individual mapped search terms."
			);

		// return newTermHashMap;
		return new Hashtable [] { newTermHashMap, null, null };

	}
	



	// Look for mapped statements and create redirect objects for each
	private Hashtable _readAndSetupMappingV2()
	{
		final String kFName = "readAndSetupMapping";

		// Init the hash, no matter what
		Hashtable newTermHashMap = new Hashtable();

		// The max counter
		fMaxIDSeen = 0;

		// tracking ID's
		// fMapsByIDHash = new Hashtable();

		// We currently only support FIXED maps

		List maps = fConfigTree.findElementsByPath( FIXED_MAP_PATH );
		if( maps == null || maps.size() < 1 )
		{
			warningMsg( kFName,
				"No redirect mappings found in config file."
				+ " Maybe you're planning to add some interactively?"
				+ " (Todo: Not yet implemented.)"
				+ " Will still listen on port until shut down."
				+ " Config path should be \"" + FIXED_MAP_PATH + "\""
				+ " under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
				+ " Reaading from config file \"" + getConfigFileURI() + "\""
				);
			return null;
		}

		infoMsg( kFName,
			"Loading " + maps.size() + " map nodes."
			+ " Reminders:"
			+ " You can use debug or trace mode to see more details"
			+ " about what's being loaded."
			+ " Or, once the server is up and running,  you can"
			+ " use your web browser to do the admin showall command;"
			+ " that may be easier than reading through this log."
			);

		// Currently, every term and URL combo is turned into
		// a unique record

		// For each mapping statement
		int lMapCount = 0;
		for( Iterator it1 = maps.iterator(); it1.hasNext(); )
		{
			// Get this map
			Element mapElem = (Element)it1.next();
			lMapCount++;

			MapRecordInterface map = null;
			try
			{
				map = new XmlMapRecord( this, mapElem, -1 );
			}
			catch (MapRecordException e)
			{
				errorMsg( kFName,
					"Unable to create map # " + lMapCount + "."
					+ " Reason: " + e
					+ " Will continue trying to load other map entries."
					);
				continue;
			}


			// Get the map's ID, if any
			int mapID = map.getID();

			// some sanity checking
			if( mapID > 0 && mapID <= fMaxIDSeen )
				warningMsg( kFName,
					"Map # " + lMapCount
					+ " has a pre-assigned ID of " + mapID
					+ " which may already be in use."
					+ " Will asign new map ID to it and continue."
					);

			// Assign a new ID if needed
			// No need to store it for now
			if( mapID < 0 || mapID <= fMaxIDSeen )
			{
				mapID = ++fMaxIDSeen;
				((XmlMapRecord)map).setID( mapID );
			}
			else
			{
				fMaxIDSeen = mapID;
			}

			// Now store it in the global hash
			// fMapsByIDHash.put( new Integer(mapID), mapElem );

			// For each term in the mapping statement
			// Get the terms for this map
			List terms = map.getTerms();

			// Sanity check
			if( terms == null || terms.size() < 1 )
			{
				warningMsg( kFName,
					"No search terms found in map # " + lMapCount + "."
					+ " Maybe you're planning to add some interactively?"
					+ " (Todo: Not yet implemented.)"
					+ " Will continue to read in the remainder of the redirect maps."
					// + " There should be at least one \"" + SEARCH_TERM_PATH + "\" tag"
					// + " under each mapping tag \"" + FIXED_MAP_PATH + "\""
					// + " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
					// + " Reaading from config file \"" + fConfigFileURI + "\""
					);
				continue;
			}

			debugMsg( kFName,
				"Map # " + lMapCount
				+ " has " + terms.size() + " terms."
				);


			//////////////////////////////////////////////
			//
			// Associate the various "data items"
			// with terms
			//
			//////////////////////////////////////////////


//			// Get the urls for this map
//			List urlElems = map.getURLObjects();
//			debugMsg( kFName, "It has " + urlElems.size() + " URLs." );
//
//			// Get the alternative term elements for this map
//			List altTerms = map.getAlternateTerms();
//			debugMsg( kFName, "It has " + altTerms.size() + " alternate terms." );
//
//			// Get the user defined data elements for this map
//			List userDataItems = map.getUserDataItems();
//			debugMsg( kFName, "It has " + userDataItems.size() + " user data items." );
//
//			// The combined count
//			int combinedCount = urlElems.size() + altTerms.size()
//				+ userDataItems.size();

			int combinedCount = map.getURLObjectsCount()
				+ map.getAlternateTermsCount() + map.getUserDataItemsCount()
				;

			// Sanity check
			if( combinedCount < 0 )
			{
				warningMsg( kFName,
					"No URLs, alternative terms or data items found in map # "
					+ lMapCount + "."
					+ " Maybe you're planning to add some interactively?"
					+ " (Todo: Not yet implemented.)"
					+ " Will skip all the terms in this mapped keyword set,"
					+ " but will continue to read in the remainder of the redirect maps."
//					+ " There should be at least one \"" + URL_PATH + "\" tag"
//					+ " and/or one \"" + ALTTERM_PATH + "\" tag"
//					+ " under each mapping tag \"" + FIXED_MAP_PATH + "\""
//					+ " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//					+ " Reaading from config file \"" + fConfigFileURI + "\""
					);
				// Skip to the next map element
				continue;
			}



			// We will now associate this map element to every
			// term that it contains
			

			// Loop through terms
			for( Iterator it2 = terms.iterator(); it2.hasNext(); )
			{
				// Grab the term and normalize it
				String term = (String)it2.next();
				term = term.toLowerCase();

				traceMsg( kFName,
					"Mapping individual search term \"" + term + "\""
					+ " to " + combinedCount
					+ " URLs/Alt terms"
					);


				// Now add it to the big map!
				// We associate a term to the map that it was found in
				// At some point we may allow a term to appear in more
				// than one map

				// pointer to placeholder list
				List termURLVector = null;

				// If we have an existing vector for this term, grab it
				if( newTermHashMap.containsKey( term ) )
				{
					termURLVector = (List)newTermHashMap.get( term );
					traceMsg( kFName,
						"This term already had " + termURLVector.size()
						+ " vector entries; these new ones will be added."
						);
				}
				// Otherwise, create a fresh new vector for it
				else
				{
					termURLVector = new Vector();
					traceMsg( kFName,
						"This is a new term vector."
						);
				}

				// Now add this new record to the end of the vector
				termURLVector.add( map );

				// And update the hash with the new/revised vector
				newTermHashMap.put( term, termURLVector );

			}   // End for each Term

		}   // End for each mapping statement
		// fTermHashMap

		infoMsg( kFName,
			"Finished, now have " + newTermHashMap.size() + " individual mapped search terms."
			);

		return newTermHashMap;
	}
	
		// Look for mapped statements and create redirect objects for each
	private Hashtable _readAndSetupMappingV1()
	{
		final String kFName = "readAndSetupMapping";

		// Init the hash, no matter what
		Hashtable newTermHashMap = new Hashtable();

		// The max counter
		fMaxIDSeen = 0;

		// tracking ID's
		// fMapsByIDHash = new Hashtable();

		// We currently only support FIXED maps

		List maps = fConfigTree.findElementsByPath( FIXED_MAP_PATH );
		if( maps == null || maps.size() < 1 )
		{
			warningMsg( kFName,
				"No redirect mappings found in config file."
				+ " Maybe you're planning to add some interactively?"
				+ " (Todo: Not yet implemented.)"
				+ " Will still listen on port until shut down."
				+ " Config path should be \"" + FIXED_MAP_PATH + "\""
				+ " under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
				+ " Reaading from config file \"" + getConfigFileURI() + "\""
				);
			return null;
		}

		infoMsg( kFName,
			"Loading " + maps.size() + " map nodes."
			+ " Reminders:"
			+ " You can use debug or trace mode to see more details"
			+ " about what's being loaded."
			+ " Or, once the server is up and running,  you can"
			+ " use your web browser to do the admin showall command;"
			+ " that may be easier than reading through this log."
			);

		// Currently, every term and URL combo is turned into
		// a unique record

		// For each mapping statement
		int lMapCount = 0;
		for( Iterator it1 = maps.iterator(); it1.hasNext(); )
		{
			// Get this map
			Element mapElem = (Element)it1.next();
			lMapCount++;

			MapRecordInterface map = null;
			try
			{
				map = new XmlMapRecord( this, mapElem, -1 );
			}
			catch (MapRecordException e)
			{
				errorMsg( kFName,
					"Unable to create map # " + lMapCount + "."
					+ " Reason: " + e
					+ " Will continue trying to load other map entries."
					);
				continue;
			}


			// Get the map's ID, if any
			int mapID = map.getID();

			// some sanity checking
			if( mapID > 0 && mapID <= fMaxIDSeen )
				warningMsg( kFName,
					"Map # " + lMapCount
					+ " has a pre-assigned ID of " + mapID
					+ " which may already be in use."
					+ " Will asign new map ID to it and continue."
					);

			// Assign a new ID if needed
			// No need to store it for now
			if( mapID < 0 || mapID <= fMaxIDSeen )
			{
				mapID = ++fMaxIDSeen;
				((XmlMapRecord)map).setID( mapID );
			}
			else
			{
				fMaxIDSeen = mapID;
			}

			// Now store it in the global hash
			// fMapsByIDHash.put( new Integer(mapID), mapElem );

			// For each term in the mapping statement
			// Get the terms for this map
			List terms = map.getTerms();

			// Sanity check
			if( terms == null || terms.size() < 1 )
			{
				warningMsg( kFName,
					"No search terms found in map # " + lMapCount + "."
					+ " Maybe you're planning to add some interactively?"
					+ " (Todo: Not yet implemented.)"
					+ " Will continue to read in the remainder of the redirect maps."
					// + " There should be at least one \"" + SEARCH_TERM_PATH + "\" tag"
					// + " under each mapping tag \"" + FIXED_MAP_PATH + "\""
					// + " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
					// + " Reaading from config file \"" + fConfigFileURI + "\""
					);
				continue;
			}

			debugMsg( kFName,
				"Map # " + lMapCount
				+ " has " + terms.size() + " terms."
				);

			// Get the urls for this map
			List urlElems = map.getURLObjects();

			debugMsg( kFName, "It has " + urlElems.size() + " URLs." );

			// Get the alternative term elements for this map
			List altTerms = map.getAlternateTerms();

			debugMsg( kFName, "It has " + altTerms.size() + " alternate terms." );

			int combinedCount = urlElems.size() + altTerms.size();

			// Sanity check
			if( combinedCount < 0 )
			{
				warningMsg( kFName,
					"No URLs or alternative terms found in map # " + lMapCount + "."
					+ " Maybe you're planning to add some interactively?"
					+ " (Todo: Not yet implemented.)"
					+ " Will skip all the terms in this mapped keyword set,"
					+ " but will continue to read in the remainder of the redirect maps."
//					+ " There should be at least one \"" + URL_PATH + "\" tag"
//					+ " and/or one \"" + ALTTERM_PATH + "\" tag"
//					+ " under each mapping tag \"" + FIXED_MAP_PATH + "\""
//					+ " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//					+ " Reaading from config file \"" + fConfigFileURI + "\""
					);
				continue;
			}

			// Loop through terms
			for( Iterator it2 = terms.iterator(); it2.hasNext(); )
			{
				// Grab the term and normalize it
				String term = (String)it2.next();
				term = term.toLowerCase();

				traceMsg( kFName,
					"Mapping individual search term \"" + term + "\""
					+ " to " + combinedCount
					+ " URLs/Alt terms"
					);


				// Now add it to the big map!

				// pointer to placeholder list
				List termURLVector = null;

				// If we have an existing vector for this term, grab it
				if( newTermHashMap.containsKey( term ) )
				{
					termURLVector = (List)newTermHashMap.get( term );
					traceMsg( kFName,
						"This term already had " + termURLVector.size()
						+ " vector entries; these new ones will be added."
						);
				}
				// Otherwise, create a fresh new vector for it
				else
				{
					termURLVector = new Vector();
					traceMsg( kFName,
						"This is a new term vector."
						);
				}

				// Now add this new record to the end of the vector
				termURLVector.add( map );

				// And update the hash with the new/revised vector
				newTermHashMap.put( term, termURLVector );

			}   // End for each Term

		}   // End for each mapping statement
		// fTermHashMap

		infoMsg( kFName,
			"Finished, now have " + newTermHashMap.size() + " individual mapped search terms."
			);

		return newTermHashMap;
	}

	void storeStatusInfoIntoConfigTree()
	{
		final String kFName = "storeStatusInfoIntoConfigTree";
		JDOMHelper tree = fOverallMasterTree;
		if( null == tree )
		{
			errorMsg( kFName, "Null config tree, nothing to augment." );
			return;
		}

		// NOTE!!!!
		// See also nie.config_ui.Configurator.save() which REMOVES
		// all this stuff before writing the config out

		// When we started
		tree.setAttributeString(
			"_start_time", NIEUtil.getTimestamp()
			);

		SearchTuningApp app = getMainApplication();
		if( null == app )
		{
			warningMsg( kFName, "Null application, nothing to add." );
			return;
		}

		// Version info
		tree.setAttributeString(
			"_version", SearchTuningApp.getModuleBanner()
			);
		tree.setAttributeString(
			"_version_and_config", app.getDetailedVersionBanner( this )
			);

		// The config file we used
		tree.setAttributeString(
			// "_config_uri", app.getConfigFileURI()
			"_config_uri", getConfigFileURI()
			);
		tree.setAttributeString(
			"_full_config_uri", getFullConfigFileURI()
			);



	}

	private static void __sep__Getting_Matching_Maps_and_Terms__() {}
	////////////////////////////////////////////////////////////////////



	// When we display Webmaster or Alternative Suggestions there
	// are options that control how they will be displayed.
	// These options can be global, or set in that particular map
	// These methods return the main map element for that term.
	// In the case where a term appears in more than one map, if we allow
	// that, this will return the primary one (currently the FIRST)

	public boolean updateMapping( String inOperation, int inMapID, List optIntendedTerms ) {
		final String kFName = "updateMapping";

		boolean isNewMap = inOperation.equals(UILink.UI_OPERATION_ADD);
		boolean debug = shouldDoDebugMsg( kFName );
		boolean trace = shouldDoTraceMsg( kFName );

		if( null==inOperation ) {
			errorMsg( kFName, "Null operation passed in." );
			return false;
		}
		if( inMapID < 1 ) {
			errorMsg( kFName, "Invalid map ID " + inMapID + " passed in." );
			return false;
		}
		if( ! cIsDbBased ) {
			errorMsg( kFName, "Method not valid when not using database maps." );
			return false;
		}

		if(debug) debugMsg( kFName, "Start: inOperation=" + inOperation + ", inMapID=" + inMapID + ", isNew=" + isNewMap );

		if( null==fTermHashMap
			|| null==fMapIDToMapObjHash
			|| null==fMapIDToTermsHash
			) {
			errorMsg( kFName,
				"System hashes not properly initialized; at least one of them is null:"
				+ " fTermHashMap=" + fTermHashMap
				+ ", fMapIDToMapObjHash=" + fMapIDToMapObjHash
				+ ", fMapIDToTermsHash=" + fMapIDToTermsHash
				);
			return false;
		}

		// We need a full object to use as a key
		Integer mapKey = new Integer( inMapID );

		// Get the new map object, if available
		MapRecordInterface newMapObj = null;
		MapRecordInterface oldMapObj = null;

		// Get the old map, which may not be there if new or maybe race condition
		if( ! isNewMap ) {
			try {
				oldMapObj = (MapRecordInterface) fMapIDToMapObjHash.get( mapKey );
			}
			catch( Throwable t ) {
				oldMapObj = null;
				if( isNewMap ) {
					errorMsg( kFName,
						"Error locating map existing map = " + inMapID
						+ " for operation '" + inOperation + "'"
						+ " NIE Server restart is suggested."
						+ " Error: " + t );
				}
			}
	
			if(trace) traceMsg( kFName, "oldMap=" + oldMapObj );
		}

		// Get the new map
		if( ! inOperation.equals( UILink.UI_OPERATION_DELETE ) ) {
			Element mapElem = new Element( "map" );
			mapElem.setAttribute( "id", ""+inMapID );
			try
			{
				newMapObj = new DbMapRecord( this, mapElem, inMapID );
			}
			catch( MapRecordException e )
			{
				errorMsg( kFName,
					"Unable to create map object for map ID " + inMapID + "."
					+ " Reason: " + e
					);
				return false;
			}

			if(trace) traceMsg( kFName, "newMap=" + newMapObj + " as key \"" + mapKey + "\"" );

			// Store it in the object hash
			fMapIDToMapObjHash.put( mapKey, newMapObj );
		}
		// We hold off deleting until the end

		boolean wasThereAnIssue = false;

		List oldTerms = new Vector();
		// if( ! inOperation.equals( UILink.UI_OPERATION_ADD ) ) {
		// ^^^ to heck with it, double check anyway
			try {
				oldTerms = (List) fMapIDToTermsHash.get( mapKey );
			}
			catch( Throwable t ) {
				oldTerms = new Vector();
			}
		// }
		if(debug) debugMsg( kFName, "oldTerms=" + oldTerms );


		List newTerms = new Vector();
		if( ! inOperation.equals( UILink.UI_OPERATION_DELETE ) ) {
			newTerms = newMapObj.getTerms();
			// ^^^ Currently already forced to lower case

			if( debug ) {
				debugMsg( kFName, "Terms from DB=" + newTerms );
			}
			// Some debug checking
			if( null!=optIntendedTerms ) {
				Collection [] lists = NIEUtil.setsCompare( newTerms, optIntendedTerms );
				Collection union = lists[0];
				Collection intersection = lists[1];
				Collection dbOnly = lists[2];
				Collection callerOnly = lists[3];
				boolean isTheSame = dbOnly.size()==0 && callerOnly.size()==0
					&& newTerms.size() == optIntendedTerms.size()
					&& union.size() == intersection.size()
					&& union.size() == newTerms.size()
					;
				if( ! isTheSame ) {
					String msg =
						"Map read from DB doesn't have the same set of expected terms"
						+ "; check database commit/connectors."
						+ " BB=" + newTerms
						+ " expected=" + optIntendedTerms
						+ " DB_missing=" + callerOnly
						+ " DB_extra=" + dbOnly
						;
					errorMsg( kFName, msg );
				}
				else {
					if(debug) debugMsg( kFName, "Terms from caller=" + optIntendedTerms );
				}
				// TODO: Consider automatically fixing BUT....
				// 1: This will mask the problem
				// 2: What ELSE were we supposed to get from the DB, maybe retry?
			}
			else {
				if(debug) debugMsg( kFName, "No terms from caller." );
			}
		}
		if(debug) debugMsg( kFName, "newTerms=" + newTerms );

		// Terms that need removal
		Collection termsToRemove = new HashSet();
		if( null!=oldTerms )
			termsToRemove.addAll( oldTerms );
		if( null!=newTerms )
			termsToRemove.removeAll( newTerms );
		if(debug) debugMsg( kFName, "Need to remove terms=" + termsToRemove );

		// Terms that are new
		Collection termsToAdd = new HashSet();
		if( null!=newTerms )
			termsToAdd.addAll( newTerms );
		if( null!=oldTerms )
			termsToAdd.removeAll( oldTerms );
		if(debug) debugMsg( kFName, "Need to add terms=" + termsToAdd );

		// Terms that were in both sets don't need anything done to them
		// For debugging
		Collection termsInCommon = new HashSet();
		if( null!=newTerms && null!=oldTerms ) {
			termsInCommon.addAll( newTerms );
			termsInCommon.retainAll( oldTerms );
		}
		if(debug) debugMsg( kFName, "Unchanged terms (no need to update)=" + termsInCommon );
			
		// Given priority to removing the old stuff
		// For each removal term
		if(debug) debugMsg( kFName, "Removing " + termsToRemove.size() + " terms." );
		for( Iterator it1 = termsToRemove.iterator() ; it1.hasNext() ; ) {
			String term = (String) it1.next();
			if(trace) traceMsg( kFName, "Remove map-key term '" + term + "' for map '" + mapKey + "'" );
			// Get the vector of map ID's
			try {
				List mapIDs = (List) fTermHashMap.get( term );
				if( null!=mapIDs && mapIDs.contains( mapKey ) ) {
					mapIDs.remove( mapKey );
				}
				else {
					errorMsg( kFName, "Unable to remove linkage between now-unused term '"
							+ term + "' and map '" + mapKey + "'" );
				}
			}
			catch( Throwable t ) {
				errorMsg( kFName,
					"Error removing map-id " + inMapID + " reference for term \""
					+ term + "\""
					+ " Error: " + t
					);
				wasThereAnIssue = true;
			}
		}
		// And add the new stuff, if any
		// For each removal term
		if(debug) debugMsg( kFName, "Adding " + termsToAdd.size() + " terms." );
		for( Iterator it2 = termsToAdd.iterator() ; it2.hasNext() ; ) {
			String term = (String) it2.next();
			if(trace) traceMsg( kFName, "Adding map-key term '" + term + "' for map '" + mapKey + "'" );
			// Terms to Map IDs
			// Get the vector of map ID's for this term
			List mapIDs = null;
			try {
				mapIDs = (List) fTermHashMap.get( term );
				if(trace) traceMsg( kFName, "Map IDs for term '" + term + "' =" + mapIDs );
			}
			catch( Throwable t ) {
				mapIDs = null;
			}
			// If not list yet, create a new list
			if( null==mapIDs ) {
				mapIDs = new Vector();
				// Associate this new list with the map ID
				fTermHashMap.put( term, mapIDs );
				if(trace) traceMsg( kFName, "Crated new Map IDs list for term '" + term + "'" );
			}
			else {
				if(trace) traceMsg( kFName, "Already had Map IDs list for term '" + term + "'" );
			}
			// Map IDs to map Objects
			// Careful not to add it twice
			if( ! mapIDs.contains( mapKey ) ) {
				mapIDs.add( mapKey );
			}
			else {
				warningMsg( kFName, "Term already associated with map: term='"
					+ term + "', map='" + mapKey + "'");
			}
		}

		// Then finally do the delete, if needed
		if( inOperation.equals( UILink.UI_OPERATION_DELETE ) ) {
			if(debug) debugMsg( kFName, "Deletee: will remove mapKey '" + mapKey + "' from fMapIDToMapObjHash and fMapIDToTermsHash" );
			try {
				fMapIDToMapObjHash.remove( mapKey );
			}
			catch( Throwable t ) {
				errorMsg( kFName,
					"Unable to remove object for id " + inMapID 
					);
				wasThereAnIssue = true;
			}
			try {
				fMapIDToTermsHash.remove( mapKey );
			}
			catch( Throwable t ) {
				errorMsg( kFName,
					"Unable to remove terms list for id " + inMapID 
					);
				wasThereAnIssue = true;
			}
		}
		// Else add or update, so make sure we store the new terms
		else {
			if(debug) debugMsg( kFName, "Setting will fMapIDToTermsHash mapKey '" + mapKey + "' terms=" + newTerms );
			fMapIDToTermsHash.put( mapKey, newTerms );
		}

		// And we're done!
		if(debug) debugMsg( kFName, "End: wasThereAnIssue=" + wasThereAnIssue + " (returning opposite of that as success code)" );
		return ! wasThereAnIssue;
	}


	public MapRecordInterface getPrimaryWmsMapRecordForTerm( String inQuery, AuxIOInfo inRequestObject )
	{
		final String kFName = "getPrimaryWmsMapRecordForTerm";
		// Normalize and check
		inQuery = NIEUtil.trimmedLowerStringOrNull( inQuery );
		if( inQuery == null ) {
			errorMsg( kFName,
				"Empty or null search term passed in, returning null."
				);
			return null;
		}
		// Get the hash and then the list, with some checking
		// Hashtable theHash = getHashMap();
		// if( theHash == null ) {
		if( null==fTermHashMap ) {
			errorMsg( kFName,
				"Null search term hash, returning null."
				);
			return null;
		}
		if( ! fTermHashMap.containsKey(inQuery) ) {
			warningMsg( kFName,
				"Search term \"" +inQuery + "\" not in master list, returning null."
				);
			return null;
		}
		List mapRecords = (List)fTermHashMap.get( inQuery );
		debugMsg( kFName, "Query '" + inQuery + "' gives reocrds=" + mapRecords );

		// Some sanity checking and a short cut
		if( mapRecords == null || mapRecords.size() < 1 )
		{
			warningMsg( kFName,
				"Search term \"" + inQuery + "\" not in master list, returning null (2)."
				);
			return null;
		}
		// Short circuit if only one
		if( mapRecords.size() == 1 )
		{
			// MapRecordInterface answer = (MapRecordInterface) mapRecords.get(0);
			MapRecordInterface answer = null;

			// Get the next one from the list
			if( ! cIsDbBased ) {
				answer = (MapRecordInterface) mapRecords.get(0);
			}
			else {
				try {
					Integer key = (Integer) mapRecords.get(0);
					answer = (MapRecordInterface)fMapIDToMapObjHash.get( key );
// getPrimaryWmsMapRecordForTerm (1)
// statusMsg( kFName, "Got map " + answer + " (1) with key \"" + key + "\"" );
					if( null!=answer ) {
						// Check for field mode matching
						boolean matches = ( (DbMapRecord) answer ).checkModeMatch( inRequestObject );
						if( ! matches ) {
							warningMsg( kFName, "The only non-null record did not match field search mode." );
							return null;
					   }
					}



				}
				catch( Throwable t ) {
					warningMsg( kFName, "Error converting from int to obj (1): " + t );
					return null;
				}
				if( null==answer ) {
					warningMsg( kFName, "Got null converting from int to obj (1)" );
					return null;
				}
			}

			// TODO: could do a check and a wraning
			if( ! answer.getHasWmsURLs() )
			{
				warningMsg( kFName,
					"No primary Webmaster Suggests map record found"
					+ " for term \"" + inQuery + "\"."
					+ " Returning null."
					);
				return null;
			}
			return answer;
		}

		// Loop through each one and find the match
		for( Iterator it = mapRecords.iterator(); it.hasNext(); )
		{
			// MapRecordInterface map = (MapRecordInterface) it.next();
			MapRecordInterface map = null;
			if( ! cIsDbBased ) {
				map = (MapRecordInterface) it.next();
			}
			else {
				try {
					Integer key = (Integer) it.next();
					map = (MapRecordInterface)fMapIDToMapObjHash.get( key );
// getPrimaryWmsMapRecordForTerm (2)
// statusMsg( kFName, "Got map " + map + " (2) with key \"" + key + "\"" );

					if( null!=map ) {
					   // Check for field mode matching
					   boolean matches = ( (DbMapRecord) map ).checkModeMatch( inRequestObject );
					   if( ! matches )
						   continue;
					}

				}
				catch( Throwable t ) {
					warningMsg( kFName, "Error converting from int to obj (2): " + t );
					continue;
				}
				if( null==map ) {
					warningMsg( kFName, "Got null converting from int to obj (2)" );
					continue;
				}
			}

			if( map.getHasWmsURLs() )
				return map;
		}

		warningMsg( kFName,
			"No primary Webmaster Suggests map record found"
			+ " for term \"" + inQuery + "\" (2)."
			+ " Returning null."
			);

		return null;
	}

	// We use this to get the settings for alt term maps
	public MapRecordInterface getPrimaryAltMapRecordForTerm( String inQuery, AuxIOInfo inRequestObject )
	{
		final String kFName = "getPrimaryAltMapRecordForTerm";
		// Normalize and check
		inQuery = NIEUtil.trimmedLowerStringOrNull( inQuery );
		if( inQuery == null ) {
			errorMsg( kFName,
				"Empty or null search term passed in, returning null."
				);
			return null;
		}
		// Get the hash and then the list, with some checking
		// Hashtable theHash = getHashMap();
		// if( theHash == null )
		if( null==fTermHashMap ) {
			errorMsg( kFName,
				"Null search term hash, returning null."
				);
			return null;
		}
		// if( ! theHash.containsKey(inQuery) )
		if( ! fTermHashMap.containsKey(inQuery) ) {
			warningMsg( kFName,
				"Search term \"" + inQuery + "\" not in master list, returning null."
				);
			return null;
		}
		// List mapRecords = (List)theHash.get( inQuery );
		List mapRecords = (List)fTermHashMap.get( inQuery );

		// Some sanity checking and a short cut
		if( mapRecords == null || mapRecords.size() < 1 ) {
			warningMsg( kFName,
				"Search term \"" + inQuery + "\" not in master list, returning null (2)."
				);
			return null;
		}
		// Short circuit if only one
		if( mapRecords.size() == 1 ) {
			// MapRecordInterface answer = (MapRecordInterface) mapRecords.get(0);
			MapRecordInterface answer = null;

			// Get the next one from the list
			if( ! cIsDbBased ) {
				answer = (MapRecordInterface) mapRecords.get(0);
			}
			else {
				try {
					Integer key = (Integer) mapRecords.get(0);
					answer = (MapRecordInterface)fMapIDToMapObjHash.get( key );
// MapRecordInterface (3)
// statusMsg( kFName, "Got map " + answer + " (3) with key \"" + key + "\"" );

					if( null!=answer ) {
						boolean matches = ( (DbMapRecord) answer ).checkModeMatch( inRequestObject );
						if( ! matches ) {
							warningMsg( kFName, "The only non-null record did not match field search mode." );
							return null;
						}
					}

				}
				catch( Throwable t ) {
					warningMsg( kFName, "Error converting from int to obj (1): " + t );
					return null;
				}
				if( null==answer ) {
					warningMsg( kFName, "Got null converting from int to obj (1)" );
					return null;
				}
			}

			// Todo: could do a check and a wraning
			if( ! answer.getHasAlternateTerms() )
			{
				warningMsg( kFName,
					"No primary Alternate Terms map record found"
					+ " for term \"" + inQuery + "\"."
					+ " Returning null."
					);
				return null;
			}
			return answer;
		}

		// Else there is more than one

		// Loop through each one and find the match
		for( Iterator it = mapRecords.iterator(); it.hasNext(); )
		{
			// MapRecordInterface map = (MapRecordInterface) it.next();
			MapRecordInterface map = null;
			if( ! cIsDbBased ) {
				map = (MapRecordInterface) it.next();
			}
			else {
				try {
					Integer key = (Integer) it.next();
					map = (MapRecordInterface)fMapIDToMapObjHash.get( key );
// MapRecordInterface (4)
// statusMsg( kFName, "Got map " + map + " (4) with key \"" + key + "\"" );
					if( null!=map ) {
						// Check for field mode matching
						boolean matches = ( (DbMapRecord) map ).checkModeMatch( inRequestObject );
						if( ! matches )
							continue;
					}

				}
				catch( Throwable t ) {
					warningMsg( kFName, "Error converting from int to obj (2): " + t );
					continue;
				}
				if( null==map ) {
					warningMsg( kFName, "Got null converting from int to obj (2)" );
					continue;
				}
			}

			if( map.getHasAlternateTerms() )
				return map;
		}

		warningMsg( kFName,
			"No primary Alternate Terms map record found"
			+ " for term \"" + inQuery + "\" (2)."
			+ " Returning null."
			);

		return null;

	}


	public List getValidAlternateSuggestionRecords( String query, AuxIOInfo inRequestObject )
	{
		final String kFName = "getValidAlternateSuggestionRecords";

		// Our answer
		List outList = new Vector();

		// Check for no query, or no matching query
		// This also checks the hash
		if( ! getHasAnyMatchingTermsQuickCheck( query ) )
		{
			debugMsg( kFName, "No matching terms, returning empty list." );
			return outList;
		}

		// String query = getUserQueryToLower();

		// Go ahead and look it up
		// List candidateList = (List)getHashMap().get( query );
		List candidateList = (List)fTermHashMap.get( query );

		debugMsg( kFName,
			"Will check " + candidateList.size() + " candidate maps."
			+ " Use trace mode to see more details."
			);

		// Master lists so we don't get duplicates
		Hashtable masterListHash = new Hashtable();

		// And we'll loop through, looking for good records
		// For each redir record
		for( Iterator it = candidateList.iterator(); it.hasNext() ; )
		{
			// Get the next one from the list
			// MapRecordInterface map = (MapRecordInterface) it.next();
			MapRecordInterface map = null;
			if( ! cIsDbBased ) {
				map = (MapRecordInterface) it.next();
			}
			else {
				try {
					Integer key = (Integer) it.next();
					map = (MapRecordInterface)fMapIDToMapObjHash.get( key );
// getValidAlternateSuggestionRecords (5)
// statusMsg( kFName, "Got map " + map + " (5) with key \"" + key + "\"" );
				}
				catch( Throwable t ) {
					warningMsg( kFName, "Error converting from int to obj (2): " + t );
					continue;
				}
				if( null==map ) {
					warningMsg( kFName, "Got null converting from int to obj (2)" );
					continue;
				}
			}

			// Check for field mode matching
			if( cIsDbBased ) {
				boolean matches = ( (DbMapRecord) map ).checkModeMatch( inRequestObject );
				if( ! matches )
					continue;
			}


			// Add in any additional, unique terms
			map.getAlternateTerms( outList, masterListHash );

		}   // End for each candiate record

		infoMsg( kFName,
			"Returning list with " + outList.size() + " suggestions."
			);

		// Return the results
		return outList;
	}


	// Returns a list of URL records
	public List getValidWebmasterSuggestsRecords( String query, AuxIOInfo inRequestObject )
	{
		final String kFName = "getValidWebmasterSuggestsRecords";

		// The master output list
		List outList = new Vector();
		// A hash to prevent duplicates
		Hashtable masterURLHash = new Hashtable();

		// Check for no query, or no matching query
		// This also checks the hash
		if( ! getHasAnyMatchingTermsQuickCheck( query ) )
		{
			debugMsg( kFName, "No matching terms, returning empty list." );
			return outList;
		}

		// Go ahead and look it up
		// List candidateList = (List)getHashMap().get( query );
		List candidateList = (List)fTermHashMap.get( query );

		// debugMsg( kFName,
		//	"Will check " + candidateList.size() + " candidates."
		//	+ " Use trace mode for details on each candidate."
		//	);
		debugMsg( kFName, "Query '" + query + "' gives candidate reocrds=" + candidateList );

		// And we'll loop through, looking for good records
		// For each redir record
		for( Iterator it = candidateList.iterator(); it.hasNext() ; )
		{
			// Get the next one from the list
			// MapRecordInterface map = (MapRecordInterface) it.next();
			MapRecordInterface map = null;
			if( ! cIsDbBased ) {
				traceMsg( kFName, "Not DB Based" );
				map = (MapRecordInterface) it.next();
			}
			else {
				try {
					Integer key = (Integer) it.next();
					map = (MapRecordInterface)fMapIDToMapObjHash.get( key );
// getValidAlternateSuggestionRecords (6)
// statusMsg( kFName, "Got map " + map + " (6) with key \"" + key + "\"" );
					traceMsg( kFName, "DB Based, key=(I)" + key );
				}
				catch( Throwable t ) {
					warningMsg( kFName, "Error converting from int to obj (2): " + t );
					continue;
				}
				if( null==map ) {
					warningMsg( kFName, "Got null converting from int to obj (2)" );
					continue;
				}
			}

			// Check for field mode matching
			if( cIsDbBased ) {
				boolean matches = ( (DbMapRecord) map ).checkModeMatch( inRequestObject );
				if( ! matches ) {
					traceMsg( kFName, "DB mode mismatch, skipping this one" );
					continue;
				}
			}


			// Add this record's urls to the main list
			map.getWmsURLObjects( outList, masterURLHash );

//			// If it's not a suggestion record, then skip it
//			if( ! tmpRecord.getIsASuggestion() )
//			{
//				traceMsg( kFName,
//					"Candidate is not a suggestion, skipping."
//					);
//				continue;
//			}
//
//			// To be safe, grab it's URL and title and make sure we're OK
//			// Get the URL we're supposed to redirect to
//			String tmpDestinationURL = tmpRecord.getURL();
//			String tmpTitle = tmpRecord.getTitle();
//
//			// Sanity check, if we didn't find a URL, complain
//			if( tmpDestinationURL == null || tmpTitle == null )
//			{
//				warningMsg( kFName,
//					"Found a matching webmaster suggests redirect record for term"
//					+ "\"" + query + "\""
//					+ " but there was no URL and/or title listed to to display."
//					+ " Will continue looking at subsequent candidate records, if any."
//					);
//				continue;
//			}
//
//			// Else it's good, keep it and add it to the verified list
//			traceMsg( kFName,
//				"Candidate IS a suggestion, adding it."
//				);
//			outList.add( tmpRecord );

		}   // End for each candiate record

		infoMsg( kFName,
			"Returning list with " + outList.size() + " suggestions."
			);

		// Return the results
		return outList;

	}
	
	
	








	// Returns a list of URL records
	public SnURLRecord getValidRedirectRecord( String query, AuxIOInfo inRequestObject )
	{
		final String kFName = "getValidRedirectRecord";

		SnURLRecord outRecord = null;

		// Check for no query, or no matching query
		// This also checks the hash
		if( ! getHasAnyMatchingTermsQuickCheck( query ) )
		{
			debugMsg( kFName, "No matching terms, returning empty list." );
			return outRecord;
		}

		// Go ahead and look it up
		// List candidateList = (List)getHashMap().get( query );
		List candidateList = (List)fTermHashMap.get( query );

		debugMsg( kFName,
			"Will check " + candidateList.size() + " candidates."
			+ " Use trace mode for details on each candidate."
			);

		// And we'll loop through, looking for good records
		// For each redir record
		for( Iterator it = candidateList.iterator(); it.hasNext() ; )
		{
			// Get the next one from the list
			// MapRecordInterface map = (MapRecordInterface) it.next();
			MapRecordInterface map = null;
			if( ! cIsDbBased ) {
				map = (MapRecordInterface) it.next();
			}
			else {
				try {
					Integer key = (Integer) it.next();
					map = (MapRecordInterface)fMapIDToMapObjHash.get( key );
// getValidRedirectRecord (7)
// statusMsg( kFName, "Got map " + map + " (7) with key \"" + key + "\"" );

				   // Check for field mode matching
				   if( null!=map ) {
					   boolean matches = ( (DbMapRecord) map ).checkModeMatch( inRequestObject );
					   if( ! matches )
						   continue;
				   }


				}
				catch( Throwable t ) {
					warningMsg( kFName, "Error converting from int to obj (2): " + t );
					continue;
				}
				if( null==map ) {
					warningMsg( kFName, "Got null converting from int to obj (2)" );
					continue;
				}
			}

			// Get the list of redirect URL records
			List urlsRecords = map.getRedirectURLObjects();

			// IF this had none, try the next map
			if( urlsRecords == null || urlsRecords.size() < 1 )
				continue;

			// We are assured that, if a reidrect URL is returned, it is valid

			// Get the URL we're supposed to redirect to
			outRecord = (SnURLRecord) urlsRecords.get(0);

			// OK, so we found one we like, and it has a URL
			break;

		}   // End for each candiate record

		infoMsg( kFName,
			"Returning " + outRecord + " redir suggestions."
			);

		// Return the results
		return outRecord;
	}
	
	
	











	// Returns a list of user data items
	// can also return null
	public Hashtable getUserDataItemRecords( String inTerms, AuxIOInfo inRequestObject )
	{
		final String kFName = "getUserDataItemRecords";

		// Escape if we're not configured for that
		if( ! hasUserClasses() )
			return null;

		// A hash to hold each alias
		// this will be a hash to <item> nodes
		// which will, in turn, be composed of cloned
		// user data nodes that match that class
		Hashtable outItemsByClassHash = new Hashtable();

		// get the search terms
		// Check for no query, or no matching query
		// This also checks the hash
		if( ! getHasAnyMatchingTermsQuickCheck( inTerms ) )
		{
			debugMsg( kFName, "No matching terms, returning empty Hash." );
			return outItemsByClassHash;
		}
		// String query = getUserQueryToLower();

		// Find the matching map (later maps)
		
		// Go ahead and look it up
		// Find all the map elements for this query
		List candidateList = (List)getHashMap().get( inTerms );

		debugMsg( kFName,
			"Will check " + candidateList.size() + " candidate."
			+ " Use trace mode for details on each candidate."
			);
		

		// And we'll loop through each map record
		for( Iterator it = candidateList.iterator(); it.hasNext() ; )
		{
			// Get the next one from the list
			MapRecordInterface mapRecord = null;
			if( ! cIsDbBased ) {
				mapRecord = (MapRecordInterface)it.next();
			}
			else {
				try {
					Integer key = (Integer)it.next();
					mapRecord = (MapRecordInterface)fMapIDToMapObjHash.get( key );
// getUserDataItemRecords (8)
// statusMsg( kFName, "Got map " + mapRecord + " (8) with key \"" + key + "\"" );

   					// Check for field mode matching
					if( null!=mapRecord ) {
					   boolean matches = ( (DbMapRecord) mapRecord ).checkModeMatch( inRequestObject );
					   if( ! matches )
						   continue;
					}

				}
				catch( Throwable t ) {
					warningMsg( kFName, "Error converting from int to obj: " + t );
					continue;
				}
				if( null==mapRecord ) {
					warningMsg( kFName, "Got null converting from int to obj" );
					continue;
				}
			}

			// search for items
			// sn config, map record, getUserDataItems()
			List items = mapRecord.getUserDataItems();
			
			// for each item
			for( Iterator it2 = items.iterator(); it2.hasNext() ; )
			{
				// Get the next one from the list
				UserDataItem item = (UserDataItem)it2.next();
				
				// get it's "class"
				// the item class should have normalized it
				String className = item.getUserClassName();

				// The root element we will attach this item to
				Element rootClassElem = null;
				
				// If we haven't seen one of these classes before
				// then we need to create it
				if( ! outItemsByClassHash.containsKey( className ) )
				{
					// rootClassElem = new Element("items");
					rootClassElem = new Element(
						UserDataItem.TOP_LEVEL_XSLT_CONTAINER_NAME
						);
					outItemsByClassHash.put( className, rootClassElem );
				}
				// Else get it from the hash
				else
				{
					rootClassElem = (Element)outItemsByClassHash.get( className );
				}

				// Clone the child's XML data and add it
				Element clone = item.getXMLDataAsCopy();
				rootClassElem.addContent( clone );

			}	// End for each data item

		}   // End for each candiate map

		infoMsg( kFName,
			"Returning hash with " + outItemsByClassHash.size() + " classes."
			);

		// Return the results
		return outItemsByClassHash;

	}



	// Whether or not we think we have any user data
	// markup items or advertisements, etc, for the current query
	public boolean hasUserDataItems( String query, AuxIOInfo inRequestObject )
	{
		final String kFName = "hasUserDataItems";

		// Escape if we're not configured for that
		if( ! hasUserClasses() )
			return false;

		// Currently we can only attach user data items
		// to a key word
		if( ! getHasAnyMatchingTermsQuickCheck( query ) )
			return false;
		// Sanity check, should have been caught above
		if( query == null )
			return false;

		// Find the matching map (later maps)
		List candidateList = (List)getHashMap().get( query );

		// Quick escape
		if( candidateList == null || candidateList.size() < 1 )
			return false;

		// And we'll loop through each map record
		for( Iterator it = candidateList.iterator(); it.hasNext() ; )
		{
			// Get the next one from the list
			MapRecordInterface mapRecord = null;
			if( ! cIsDbBased ) {
				mapRecord = (MapRecordInterface)it.next();
			}
			else {
				try {
					Integer key = (Integer)it.next();
					mapRecord = (MapRecordInterface)fMapIDToMapObjHash.get( key );
// hasUserDataItems (9)
// statusMsg( kFName, "Got " + mapRecord + " (9) with key \"" + key + "\"" );

					if( null!=mapRecord ) {
						// Check for field mode matching
						boolean matches = ( (DbMapRecord) mapRecord ).checkModeMatch( inRequestObject );
						if( ! matches )
							continue;
					}

				}
				catch( Throwable t ) {
					warningMsg( kFName, "Error converting from int to obj: " + t );
					continue;
				}
				if( null==mapRecord ) {
					warningMsg( kFName, "Got null converting from int to obj" );
					continue;
				}
			}


			// search for items
			// sn config, map record, getUserDataItems()
			List items = mapRecord.getUserDataItems();
			
			// If we found ANY, then we're done
			if( items != null && items.size() > 0 )
				return true;
				
			// Else keep looking
		}

		// We've scanned all maps for data items and found none
		// so the answer is NO
		return false;
		
	}

	// Moved here from SNRueqestHandler
	// This is a quick first pass test
	// it may be that, later on, no records will qualify after additional filtering
	public boolean getHasAnyMatchingTermsQuickCheck( String inTerm )
	{
		final String kFName = "getHasAnyMatchingTermsQuickCheck";

		// Find out what the user entered
		// String lQueryTerms = getUserQueryToLower();
		String lQueryTerms = inTerm;

		// If no query, that's fine
		if( lQueryTerms == null )
		{
			debugMsg( kFName, "No query terms found, returning false." );
			return false;
		}
		// Or if the hash doesn't have that term listed
		// Lookup the record
		else if( getHashMap() == null || ! getHashMap().containsKey( lQueryTerms ) )
		{
			debugMsg( kFName,
				"Query terms \"" + lQueryTerms + "\" not found in map"
				+ " (or no map in use), returning false."
				);
			return false;
		}

		debugMsg( kFName,
			"Query terms \"" + lQueryTerms + "\" was found in the map."
			+ "Returning true."
			);

		return true;
	}


	private static void __sep__Misc__() {}
	////////////////////////////////////////////////////////////////////



	public SearchTuningApp getMainApplication()
	{
		return fApp;
	}

	public String getConfigFileURI()
	{
		final String kFName = "getConfigFileURI";
		// return fConfigFileURI;
		// We'd like to get a more specific path from the JDOMHelper class
		if( null!=fOverallMasterTree )
			return fOverallMasterTree.getURI();
		errorMsg( kFName,
			"No master JDOM tree, returning null."
			);
		return null;
	}

	public String getFullConfigFileURI()
	{
		return fFullConfigFileURI;
	}

	public String getSearchNamesURL()
	{
		if( ! fUseCache )
		{
			final String kFName = "getSearchNamesURL";
			cSearchNamesURL = fConfigTree.getTextByPathTrimOrNull(
				SERVER_URL_PATH
				);
			if( null == cSearchNamesURL )
			{
				cSearchNamesURL = fConfigTree.getTextByPathTrimOrNull(
					OLD_SEARCH_NAMES_URL_PATH
					);
				if( null != cSearchNamesURL )
					warningMsg( kFName,
						"You are still using the old/deprecated setting"
						+ " to specify the main application URL."
						+ " You are currently using "
						+ OLD_SEARCH_NAMES_URL_PATH
						+ ", so you should update to using "
						+ SERVER_URL_PATH
						);
			}

			if( null == cSearchNamesURL )
			{
				errorMsg( kFName,
					"No Search Engine URL specified."
					);
			}
			// Else there is a string, make sure it's a valid URL
			else
			{
				URL tmpURL  = null;
				try
				{
					tmpURL = new URL( cSearchNamesURL );
				}
				catch(Exception e)
				{
					errorMsg( kFName,
						"Invalid/malformed Search Engine URL specified."
						+ " URL=\"" + cSearchNamesURL + "\""
						+ ", Error: " + e
						);
					tmpURL = null;
				}
				if( null != tmpURL )
				{
					String lProto = tmpURL.getProtocol();
					// Invalid protocol, only handle http
					if( null == lProto || ! lProto.toLowerCase().equals( "http" ) )
					{
						errorMsg( kFName,
							"Missing/Invalid protocol " + lProto  + " from Search Engine URL."
							+ " URL=\"" + cSearchNamesURL + "\""
							+ " We only support http at this time."
							);
					}
					else
					{
						int lPort = tmpURL.getPort();
						if( lPort <= 0 )
							lPort = 80;	// not our default, the http protocol default
						cPortNumber = lPort;
					}
				}
			}
		}
		return cSearchNamesURL;
	}

	public static String _getSearchNamesURL_static( JDOMHelper inConfigTree )
	{
		final String kFName = "getSearchNamesURL_static";
		if( null == inConfigTree ) {
			errorMsg( kFName,
				"Null config tree passed in, returning null."
				);
			return null;
		}

		String outURL = inConfigTree.getTextByPathTrimOrNull(
			SERVER_URL_PATH
			);
		if( null == outURL )
		{
			outURL = inConfigTree.getTextByPathTrimOrNull(
				OLD_SEARCH_NAMES_URL_PATH
				);
			if( null != outURL )
				warningMsg( kFName,
					"You are still using the old/deprecated setting"
					+ " to specify the main application URL."
					+ " You are currently using "
					+ OLD_SEARCH_NAMES_URL_PATH
					+ ", so you should update to using "
					+ SERVER_URL_PATH
					);
		}

		if( null == outURL )  {
			errorMsg( kFName,
				"No SearchTrack URL specified, returning null."
				);
			return null;
		}

		// Check for valid URL syntax and http protocol
		URL tmpURL = null;
		try
		{
			tmpURL = new URL( outURL );
		}
		catch(Exception e)
		{
			errorMsg( kFName,
				"Invalid/malformed Search Engine URL specified."
				+ " URL=\"" + outURL + "\""
				+ ", Error: " + e
				);
			tmpURL = null;
		}
		if( null != tmpURL )
		{
			String lProto = tmpURL.getProtocol();
			// Invalid protocol, only handle http
			if( null == lProto || ! lProto.toLowerCase().equals( "http" ) )
			{
				errorMsg( kFName,
					"Missing/Invalid protocol " + lProto  + " from Search Engine URL."
					+ " URL=\"" + outURL + "\""
					+ " We only support http at this time."
					);
				return null;
			}
		}

		return outURL;
	}




	// MUST CALL getSearchEngineURL() before calling this routine
	public int getPort()
	{
		// This member field is set only once in readGlobalOptions
		// And it is validated there and the appropriate exception is thrown
		// at constructor time

		// IMPORATANT NOTE:
		// This is CACHED by getSearchEngineURL()
		// and should be called AFTER that routine

		return cPortNumber;
	}

	public int getLegacyPort()
	{
		if( ! fUseCache )
			cLegacyPortNumber = fConfigTree.getIntFromAttribute(
				SN_PORT_ATTR, -1
				);
			// cLegacyPortNumber = fConfigTree.getIntFromAttribute(
			//	SN_PORT_ATTR, DEFAULT_PORT
			//	);

		// This member field is set only once in readGlobalOptions
		// And it is validated there and the appropriate exception is thrown
		// at constructor time

		return cLegacyPortNumber;
	}

	public static int tryFetchingPortFromConfig( String inConfigFileURI ) {
		final String kFName = "tryFetchingPortFromConfig";
		if( null==inConfigFileURI ) {
			warningMsg( kFName, "Null config file URI passed in; will try default." );
			// return null;
			inConfigFileURI = SearchTuningConfig.DEFAULT_CONFIG_FILE_URI;
		}

		JDOMHelper tree = null;
		// Try fancy jdom
		try
		{
			tree = new JDOMHelper(
				inConfigFileURI, null, 0, null
				);
		}
		catch (JDOMHelperException e1)
		{
			errorMsg( kFName, 
				"Error loading config file with includes."
				+ " JDOMHelperException: " + e1
				+ " Will try without includes."
				);
			// OK, try it the old fashioned way, with no includes
			try
			{
				tree = new JDOMHelper( inConfigFileURI );
			}
			catch (JDOMHelperException e2)
			{
				errorMsg( kFName, 
					"Error loading config file wihthout includes."
					+ " JDOMHelperException: " + e2
					// + " Will try as plain text."
					+ " Returning null."
					);
				tree = null;
			}
		}

		// If we got a tree back, use xpath to search it
		if( null!=tree ) {

			// Paths we'll try
			String [] tryPaths = {
				"//" + SERVER_URL_PATH,
				"//" + OLD_SEARCH_NAMES_URL_PATH,
				"/" + MAIN_SN_CONFIG_PATH + "/@port",
				"/" + OLD_MAIN_SN_CONFIG_PATH + "/@port"
				};

			// For each path we'd like to try
			for( int i=0; i<tryPaths.length ; i++ ) {
				String tryPath = tryPaths[i];
				try {
					XPath xpath = XPath.newInstance( tryPath );
					List results = xpath.selectNodes( tree.getJdomElement() );
		
					// For each XML node
					for( Iterator it = results.iterator() ; it.hasNext() ; ) {
						Object currObj = it.next();

						// If it's an element, assume the text
						// is a URL and grab the port from that
						if( currObj instanceof org.jdom.Element ) {
							Element currElem = (Element) currObj;
							String tmpUrlStr = currElem.getTextNormalize();
							tmpUrlStr = NIEUtil.trimmedStringOrNull( tmpUrlStr );
							if( null==tmpUrlStr ) {
								errorMsg( kFName,
									"Ignoring empty URL element "
									+ JDOMHelper.JDOMToString( currElem, false )
									);
								continue;
							}
							// Try making it into a real URL
							URL tmpURL  = null;
							try {
								tmpURL = new URL( tmpUrlStr );
							}
							catch(Exception e) {
								errorMsg( kFName,
									"Ignoring invalid URL (1): \"" + tmpUrlStr + "\""
									+ " Error: " + e
									);
								continue;
							}
							if( null != tmpURL )
							{
								int lPort = tmpURL.getPort();
								if( lPort <= 0 )
									lPort = 80;	// not our default, the http protocol default
								return lPort;
							}
							// this should never happen
							else {
								errorMsg( kFName,
									"Ignoring invalid URL (2): \"" + tmpUrlStr + "\""
									);
								continue;
							}

						}	// End if it was an element
						// Else was it an attribute?  The port attribute?
						else if( currObj instanceof org.jdom.Attribute ) {
							Attribute currAttr = (Attribute) currObj;
							String portStr = currAttr.getValue();
							int lPort = NIEUtil.stringToIntOrDefaultValue(
								portStr, -1, true, true
								);
							if( lPort > 0 )
								return lPort;
							else {
								errorMsg( kFName,
									"Ignoring invalid port \"" + portStr + "\""
									+ " in attribute \"" + currAttr + "\""
									);
								continue;
							}

						}
						// Else we don't know what it was
						else {
							errorMsg( kFName,
								"Ignoring unhandled XML node of type \"" + currObj.getClass().getName() + "\""
								+ " = \"" + currObj + "\""
								);
							continue;
						}

					}	// End for each XML node
				}
				catch( JDOMException e ) {
					errorMsg( kFName, 
						"Error searching config file with xpath for \"" + tryPath + "\""
						+ " Exception: " + e
						+ " Will keep checking paths."
						);
					continue;
				}

			}	// end or each path we'd like to try

		}	// End if tree is not null

		/***
		No, can't retrieve as text, could be cdata, etc, too weird
		// Else no jdom, try loading as a string
		try {
			String fileContents = NIEUtil.fetchURIContentsChar( inConfigFileURI );
			if( null!=fileContents && fileContents.length() > 0 ) {

				String pattern1 = SEARCH_ENGINE_URL_PATH + "=\"";
				int pattern1At = fileContents.indexOf( pattern1 );
				if( pattern1At>=0 ) {
					int urlStart = pattern1At + pattern1.length();


				}

			}
			else {
				errorMsg( kFName,
					"Got back null/empty string when loading config file \"" + inConfigFileURI + "\" as text."
					+ " We give up, returning null."
					);
			}

		}
		catch( IOException e4 ) {
			errorMsg( kFName, 
				"Error loading config file as text."
				+ " Exception: " + e4
				+ " We give up, returning null."
				);
		}
		***/

		return -1;
	}








	public boolean getHasDbBasedMaps() {
		return cIsDbBased;
	}

	// Methods that spawned processes can use to get back
	// to us and ask for global type data
	private Hashtable getHashMap()
	{
		return fTermHashMap;
	}
	// private boolean installNewMap( Hashtable inNewHash )
	private boolean installNewMaps( Hashtable [] inNewHashes )
	{
		final String kFName = "installNewMap";
		// if( null==inNewHash ) {
		if( null==inNewHashes ) {
			errorMsg( kFName, "Null replacement hash map passed in, keep old hash." );
			return false;
		}
		if( inNewHashes.length != 3 ) {
			errorMsg( kFName, "Incorrect number of replacement hashes passed in, keeping old hashes." );
			return false;
		}
		if( null==inNewHashes[0] ) {
			errorMsg( kFName, "Null first element of replacement hashes passed in, keeping old hashes." );
			return false;
		}
		cIsInTransition = true;
		fTermHashMap = inNewHashes[0]; // inNewHash;
		fMapIDToMapObjHash = inNewHashes[1];
		fMapIDToTermsHash = inNewHashes[2];
		cLastTransition = (new java.util.Date()).getTime();
		cIsInTransition = false;
		return true;
	}

	public int getHashMapCount()
	{
		Hashtable tmpHash = getHashMap();
		if( tmpHash == null )
			return 0;
		else
			return tmpHash.size();
	}
	
	public BaseMarkup getUserClassDefByNameOrNull( String inUserClassAlias )
	{
		final String kFName = "getUserClassDefByNameOrNull";
		inUserClassAlias = NIEUtil.trimmedStringOrNull( inUserClassAlias );
		if( null==inUserClassAlias ) {
			errorMsg( kFName, "Null/empty user data class alias passed in, returning null." );
			return null;
		}
		if( getUserClassesHashMap().containsKey( inUserClassAlias ) )
			return (BaseMarkup) getUserClassesHashMap().get( inUserClassAlias );
		else
			return null;
	}

	private Hashtable getUserClassesHashMap()
	{
		return fUserClassHashMap;
	}
	public Set getAllUserClassNames()
	{
		Hashtable tmpHash = getUserClassesHashMap();
		if( null!=tmpHash )
			return tmpHash.keySet();
		else
			return null;
	}
	public int getUserClassesHashMapCount()
	{
		Hashtable tmpHash = getUserClassesHashMap();
		if( tmpHash == null )
			return 0;
		else
			return tmpHash.size();
	}
	public boolean hasUserClasses()
	{
		return null!=fUserClassHashMap && ! fUserClassHashMap.isEmpty();
	}
	public boolean hasSearchModes()
	{
		SearchEngineConfig src = getSearchEngine();
		if( null==src )
			return false;
		Collection modes = src.getSearchFormOptionFieldNames();
		return null!=modes && ! modes.isEmpty();
	}


	public Document getConfigDoc()
	{
		return fOverallMasterTree.getJdomElement().getDocument();
	}

	public JDOMHelper getOverallMasterConfigTree() {
		return fOverallMasterTree;
	}
	// Returns null if there is none
	public DBConfig getDBConfig()
	{
		/***
		if( null != getSearchLogger() )
			return getSearchLogger().getDBConfig();
		else
			return null;
		***/
		return fDBConfig;
	}

	public SearchEngineConfig getSearchEngine()
	{
		return fSearchEngineConfig;
	}
	public String getSearchEngineURL()
	{
		return getSearchEngine().getSearchEngineURL();
	}
	public String getSearchEngineMethod()
	{
		return getSearchEngine().getSearchEngineMethod();
	}

	public SearchLogger getSearchLogger()
	{
		return fSearchLogger;
	}
	public boolean hasSearchLogger()
	{
		return getSearchLogger() != null;
	}



	public nie.sr2.ReportDispatcher getReportDispatcher()
	{
		return fReportDispatcher;
	}
	public boolean hasReportDispatcher()
	{
		return (null != getReportDispatcher() );
	}

	public LuceneRequestDispatcher getLuceneSearchDispatcher()
	{
		return fLuceneDispatcher;
	}
	public boolean hasLuceneSearchDispatcher()
	{
		return (null != getLuceneSearchDispatcher() );
	}

	public nie.sn.FileDispatcher getFileDispatcher()
	{
		return fFileDispatcher;
	}
	public nie.webui.UIRequestDispatcher getUIRequestDispatcher()
	{
		return fUIDispatcher;
	}

	private static final void __Passwords__(){}
	/////////////////////////////////////////////////////////////////////////

	public /*private*/ String getAdminPwd()
	{
		if( ! fUseCache )
		{
			final String kFName = "getAdminPwd";
			cAdminPwd = fConfigTree.getStringFromAttributeTrimOrNull(
				ADMIN_PWD_ATTR
				);
			String otherAdminPwd = fConfigTree.getStringFromAttributeTrimOrNull(
				OLD_ADMIN_PWD_ATTR
				);
			if( null!=cAdminPwd && null!=otherAdminPwd ) {
				cAdminPwd = null;
				errorMsg( kFName,
					"Have incorrectly set BOTH admin password attributes: "
					+ ADMIN_PWD_ATTR + " is an alias for "
					+ OLD_ADMIN_PWD_ATTR + "."
					+ " They can not both be set."
					+ " No admin password has been set."
					);

			}
			if( null == cAdminPwd )
			{
				cAdminPwd = otherAdminPwd;
			}
				/***
				if( null != cAdminPwd )
					warningMsg( kFName,
						OLD_ADMIN_PWD_ATTR + " has been deprecated"
						+ " in favor of " + ADMIN_PWD_ATTR
						);
				***/
		}
		return cAdminPwd;
	}




	public int _old_passwordToAccessLevel( String inRequestedPassword ) {
		final String kFName = "passwordToAccessLevel";
		inRequestedPassword = NIEUtil.trimmedStringOrNull( inRequestedPassword );
		if( null==inRequestedPassword ) {
			errorMsg( kFName, "Null password passed in, returning -1." );
			return -1;
		}
		if( null==cPasswordLevelTable ) {
			errorMsg( kFName, "Null password table (not initialized), returning -1." );
			return -1;
		}
		if( ! cPasswordLevelTable.containsKey(inRequestedPassword) ) {
			warningMsg( kFName,
				"Password \"" +  inRequestedPassword
				+ "\" not found in password table, returning -1"
				);
			return -1;
		}
		Integer obj = (Integer) cPasswordLevelTable.get( inRequestedPassword );
		return obj.intValue();
	}
	public int tokenToAccessLevel( String inToken, boolean inIsKey ) {
		final String kFName = "tokenToAccessLevel";
		inToken = NIEUtil.trimmedStringOrNull( inToken );
		if( null==inToken ) {
			errorMsg( kFName, "Null token passed in, returning -1." );
			return -1;
		}
		if( ! inIsKey ) {
			inToken = passwordToKeyOrNull( inToken );
			if( null==inToken ) {
				errorMsg( kFName, "Null key from password, returning -1." );
				return -1;
			}
			inIsKey = true;
		}
		if( null==cPasswordLevelTable ) {
			errorMsg( kFName, "Null password table (not initialized), returning -1." );
			return -1;
		}
		if( ! cPasswordLevelTable.containsKey(inToken) ) {
			warningMsg( kFName,
				"Password \"" +  inToken
				+ "\" not found in password table, returning -1"
				);
			return -1;
		}
		Integer obj = (Integer) cPasswordLevelTable.get( inToken );
		return obj.intValue();
	}
	// July '08 actually this now stores the scrambled md5 tokens
	void initPasswordTable() {
		if( null==cPasswordLevelTable ) {
			cPasswordLevelTable = new Hashtable();
			// Check for the admin password
			// warning about there being none is done elsewhere
			String tmpStr = getAdminPwd();
			tmpStr = passwordToKeyOrNull( tmpStr );
			if( null!=tmpStr )
				cPasswordLevelTable.put( tmpStr, new Integer(ADMIN_PWD_SECURITY_LEVEL) );
			// TODO: also look for login_key

			// Check for read-only
			tmpStr = fConfigTree.getStringFromAttributeTrimOrNull(
				BROWSE_PWD_ATTR
				);
			tmpStr = passwordToKeyOrNull( tmpStr );
			if( null!=tmpStr )
				cPasswordLevelTable.put( tmpStr, new Integer(BROWSE_PWD_SECURITY_LEVEL) );
			// TODO: also look for read_only_login_key

			// TODO: What about the older login field?
		}
	}
	// Needs to be static (for now) because it's referenced
	// by the Configurator
	public static String passwordToKeyOrNull( String inPassword )
	{
		final String kFName = "passwordToKeyOrNull";
		final String kExTag = kClassName + '.' + kFName + ": ";
		String outKey = null;
		inPassword = NIEUtil.trimmedStringOrNull( inPassword );
		if( null!=inPassword )
		{
			try {
				Chap cp = new Chap();
				int [] cky1 = cp.sign( new ByteArrayInputStream( inPassword.getBytes()) );
				outKey = cp.md5string(cky1);
			}
			catch( IOException e ) {
				errorMsg( kFName, "chap error: " + e );
				outKey = null;
			}
		}
		else {
			errorMsg( kFName, "null/empty password input" );
		}
		return outKey;
	}

	private static final void __Misc_Getters_and_Setters__(){}
	////////////////////////////////////////////////////////
	
	// Not getting a style sheet should not be a fatal error
	// though we will log error messages
	// TODO: let them load their own from elsewhere
	public String getDefaultCssStyleTextOrNull()
	{
		if( ! fUseCache && cCssText==null ) {
			final String kFName = "getCssStyleText";
	
			// String uri = getCssStyleSheetURI();
			String uri = DEFAULT_CSS_URI;
			if( null==uri )
			{
				infoMsg( kFName,
					"No CSS URI defined, returning null."
					);
				cCssText = null;
			}
			else {
				AuxIOInfo tmpAuxInfo = new AuxIOInfo();
				// tmpAuxInfo.setSystemRelativeBaseClassName( kFullClassName() );
				// Resolve system URLs relative to the main application config
				tmpAuxInfo.setSystemRelativeBaseClassName(
					this.getClass().getName()
					);
				try
				{
					cCssText = NIEUtil.fetchURIContentsChar(
						uri,
						getConfigFileURI(),
						null, null,	// optUsername, optPassword,
						tmpAuxInfo, false
						);
				}
				catch( Exception e )
				{
					errorMsg( kFName,
						"Error opening CSS URI \"" + uri + "\"."
						+ " Returning null."
						+ " Error: " + e
						);
					cCssText = null;
				}
				// Normalize and check
				cCssText = NIEUtil.trimmedStringOrNull( cCssText );
				if( null==cCssText )
					errorMsg( kFName,
						"Null/empty default CSS style sheet contents read"
						+ " from URI \"" + uri + "\", returning null."
						);
				// debugMsg( kFName, "CSS=" + cCssText );
			}

		}

		return cCssText;


		// Good resource on tables and CSS, from Nick Sayer
		// http://www.w3.org/TR/REC-CSS2/tables.html
		// And overall CSS info
		// http://www.w3.org/TR/REC-CSS2/cover.html#minitoc
		// Selectors / pattern matching
		// http://www.w3.org/TR/REC-CSS2/selector.html

//		Inside HTML:
//		<head>
//			...
//			<STYLE type="text/css">
//				H1 { color: blue }
//			</STYLE>
//		</head>

	}


	// TODO: For now, not settable
	// NOTE: This group of methods could be argued to be
	// more at home in the search engine config
	// BUT some of the factors are SITE SPECIFIC, not search
	// engine specific, so not really a good fit there either
	public static String getNullSearchMarker() {
		return DEFAULT_NULL_SEARCH_MARKER;
	}
	// Technically this goes in the logic section, vs simple get/set
	public boolean isNullSearch( String inQuery ) {
		inQuery = NIEUtil.trimmedLowerStringOrNull( inQuery );
		// Null or empty
		if( null==inQuery )
			return true;
		// Or our special sentinal marker
		if( inQuery.equalsIgnoreCase( getNullSearchMarker() ) )
			return true;
		// Or one of the equiv phrases
		if( null!=getNullPhraseEquivList() ) {
			inQuery = NIEUtil.trimmedLowerStringOrNull( inQuery );
			if( getNullPhraseEquivList().contains(inQuery) )
				return true;
		}
		// else it is NOT null
		return false;
	}
	// Technically this goes in the logic section, vs simple get/set
	public boolean isReferrerMarker( String inUrl ) {
		inUrl = NIEUtil.trimmedStringOrNull( inUrl );
		if( null!=inUrl ) {
			if( inUrl.equalsIgnoreCase(NULL_REDIR_TO_REFERRER_MARKER)
				|| inUrl.equalsIgnoreCase(NULL_REDIR_TO_REFERRER_MARKER2)
			) {
				return true;
			}
		}
		return false;
	}
	
	// Phrases that we should treat as null searches
	public Set getNullPhraseEquivList()
	{
		if( null == fNullPhraseEquiv )
		{
			List tmpList = fConfigTree.getTextListByPathNotNullTrim(
				NULL_EQUIV_PHRASE_PATH
				);
			// Normalize them all to lower case, OK if empty
			fNullPhraseEquiv = NIEUtil.trimmedLowerStringsAsSet( tmpList, false );
		}
		return fNullPhraseEquiv;
	}

	public boolean shouldRedirectNullSearches()
	{
		final String kFName = "shouldRedirectNullSearches";
		if( ! fUseCache ) {
			debugMsg( kFName, "Checking config path: " + NULL_REDIRECT_PATH );
			fNullSearchRedirEnabled = fConfigTree.getBooleanFromSinglePathAttr(
					NULL_REDIRECT_PATH, ACTIVATE_NULL_REDIR_ATTR,
					DEFAULT_ACTIVATE_NULL_REDIR
					);
			debugMsg( kFName, "Set to: " + fNullSearchRedirEnabled );
			debugMsg( kFName, "getNullSearchRedirURL()=" + getNullSearchRedirURL() );
			fNullSearchRedirEnabled &= ( null!=getNullSearchRedirURL() );
			debugMsg( kFName, "Finally = " + fNullSearchRedirEnabled );
		}
		else {
			debugMsg( kFName, "Returning cached value: " + fNullSearchRedirEnabled );
		}
		return fNullSearchRedirEnabled;
	}

	public String getNullSearchRedirURL()
	{
		if( ! fUseCache )
		{
			// The URL to redirect to
			fNullSearchRedirUrl = fConfigTree.getTextByPathTrimOrNull(
					NULL_REDIR_URL_PATH
				);
			// may be equal to NULL_REDIR_TO_REFERRER_MARKER
		}
		return fNullSearchRedirUrl;
	}


	public String getLicCoStr() {
		return fCo;
	}
	public String getLicSrvStr() {
		return fSrv;
	}
	public double getLicExpDays() {
		if( null==fNd )
			return -1.0;
		long endTime = fNd.getTime();
		long now = (new java.util.Date()).getTime();

		long timeLeft = endTime - now;
		double outTimeLeft = ( (double) timeLeft / (double) NIEUtil.MS_PER_DAY );
		// It's good THROUGH the entire ending day
		outTimeLeft += 1.0;
		if( timeLeft < 0.0 )
			return -1.0;

		return NIEUtil.formatDoubleToDisplayPrecision( outTimeLeft );
	}
	public String getLicEndDate() {
		if( null==fNd )
			return null;
		// return NIEUtil.formatDateToString( fNd, "_d-Mon-yyyy" );
		// We need to display this in GMT, regardless of the local timezone
		TimeZone tz = TimeZone.getTimeZone("GMT");
		// Create a GMT based calendar
		Calendar cal = new GregorianCalendar( tz );
		// Now the simple date formatter
		SimpleDateFormat df = new SimpleDateFormat( "M/d/yy z" );
		// But tell it to use this calendar (and time zone)
		df.setCalendar( cal );
		// Do the formatting and return the results
		String reformattedDate = df.format( fNd );
		return reformattedDate;
	}
	public String getLicStartDate() {
		if( null==fSt )
			return null;
		// return NIEUtil.formatDateToString( fSt, "_d-Mon-yyyy" );
		// return NIEUtil.formatDateToString( fSt, "_m/_d/yy" );
		// We need to display this in GMT, regardless of the local timezone
		TimeZone tz = TimeZone.getTimeZone("GMT");
		// Create a GMT based calendar
		Calendar cal = new GregorianCalendar( tz );
		// Now the simple date formatter with timzone
		// SimpleDateFormat df = new SimpleDateFormat( "M/d/yy z" );
		// for the start date, we don't actually show the timezone
		// but it is still factoried in wrt day month, etc, and the
		// end date will have it
		SimpleDateFormat df = new SimpleDateFormat( "M/d/yy" );
		// But tell it to use this calendar (and time zone)
		df.setCalendar( cal );
		// Do the formatting and return the results
		String reformattedDate = df.format( fSt );
		return reformattedDate;
	}
	public boolean getLicIsAllGood() {
		return fLicFull;
	}


	public void printCompleteInfo()
	{
		final String kFName = "printCompleteInfo";

		errorMsg( kFName, "Error: SearchNamesAppConfig:printCompleteInfo:"
				+ " Not yet implemented.  (Todo)"
				);
//			if( gVerbose )
//			{
//				Set lKeySet = fTermHashMap.keySet();
//				Iterator lTempIterator = lKeySet.iterator();
//
//				System.out.println( "Server will listen on port " + fPortNumber );
//
//				while( lTempIterator.hasNext() )
//				{
//					String lKey = (String)lTempIterator.next();
//					SnRedirectRecord lURL = (SnRedirectRecord)fTermHashMap.get( lKey );
//					System.out.println( lKey + " will " + (lURL.getMethod() == SnRedirectRecord.kRedirect ? "redirect" : "suggest") + " url " + lURL.getURL() );
//				}

	}



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


	// The main application
	private SearchTuningApp fApp;


	// Store the instance we get back
	private DBConfig fDBConfig;



	// The hash map maps requested search terms to urls
	// and whether that's supposed to be a suggestion or
	// a redirect.
	// Each term links to a LIST of SnRedirectRecords OR map IDs (Integer objects)
	// ^^^ depends on whether XML or DB based
	private volatile Hashtable fTermHashMap;
	// Given a term, find the map
	// If this is the only hash and not using map ids, it's the actual map
	// If using map ID's, such as with database repository, then get back
	// Integer key, which then look in next hash	
	// If using map ID's, use this hash to go from Integer key to map object
	// See cIsDbBased
	// TODO: Validate All Strings or INTS
	private Hashtable fMapIDToMapObjHash;
	// Given a map ID Integer, give a list of terms that point to it
	private Hashtable fMapIDToTermsHash;
	// Controls whether uses all 3 hashes and map IDs, or simple one hash and objects
	boolean cIsDbBased;
	// Instead of syncs, we have this var to tell us that, if something is weird,
	// if we're in a transition don't make such a fuss about it
	volatile boolean cIsInTransition;
	// also, in case we have issues, this tells when the last time was
	long cLastTransition;

	// Declarations of User Data Markup Classes
	private Hashtable fUserClassHashMap;


	// A shortcut to map records, by id
	private Hashtable _fMapsByIDHash;
	// Id's are numeric
	private int fMaxIDSeen;
	
	// When we were created
	private String fCreationTimestamp;

	// Cached variable flag
	// Off by default
	private boolean fUseCache;

	// What port to listen on
	private int cPortNumber;
	private int cLegacyPortNumber;
	private String cSearchNamesURL;
	private String cSearchNamesTestDriveURL;
	private String cAdminPwd;
	private String cCssText;
	// Should we be verbose or not
	public int fVerbosity;

	// Where we store the config data
	JDOMHelper fConfigTree;
	// This is the overall tree we were inside of
	// We may this for storing the data back out to disk
	JDOMHelper fOverallMasterTree;
	// Where to read paramters from
	private String fConfigFileURI;
	private String fFullConfigFileURI;
	// PLEASE use the method getConfigFileURI() instead, we now get it from JDOMHelper



	// The configuration information for the Host Search Engine
	private SearchEngineConfig fSearchEngineConfig;

	// The OPTIONAL object for allowing user queries to be logged
	private SearchLogger fSearchLogger;
	// The report request dispatcher, if we have search logging
	private nie.sr2.ReportDispatcher fReportDispatcher;
	// the object in charge of getting static files
	private FileDispatcher fFileDispatcher;
	// process UI requests
	private nie.webui.UIRequestDispatcher fUIDispatcher;
	// Process Search requests (to our built in search engine, probably Lucene)
	private nie.lucene.LuceneRequestDispatcher fLuceneDispatcher;

	
	private Set fNullPhraseEquiv;
	private boolean fNullSearchRedirEnabled;
	private String fNullSearchRedirUrl;

	private String fCo;
	private String fStStr;
	private java.util.Date fSt;
	private String fNdStr;
	private java.util.Date fNd;
	private String fSrv;
	private boolean fLicFull;

	
//	// flag for whether we should just check the config
//	private boolean fJustCheckConfig;
//	// Other actions we don't currently implement but are planning
//	private boolean fCheckURLs = false;
//	private boolean fCheckImages = false;
//	private boolean fAutoFetchMetaFields = false;
//	private boolean fAutoRefetchAllMetaFields = false;

//	// Hashes to track classes we know about
//	private static Hashtable fClassListDescription;
//	private static Hashtable fClassListVerbosity;

	// XML Paths we use
	// When we read in the XML config file, what is the path to
	// the main node we should look at
	public static final String MAIN_SN_CONFIG_PATH = "/nie_config/search_tuning";
	private static final String OLD_MAIN_SN_CONFIG_PATH = "/nie_config/search_names";


	// Where to look under the main search names tree
	// SEI = Search Engine Info
	// Note that we do not add a /
	// public static final String SEI_CONFIG_PATH = "search_engine_info";
	public static final String SEI_CONFIG_PATH = SearchEngineConfig.MAIN_ELEM_NAME;

	// Information about Search Track data logging
	private static final String SEARCH_TRACKING_CONFIG_PATH =
		"search_tracking";

	// Other stuff moved to SearchEngineConfig class
	private static final String OLD_SEARCH_TRACKING_CONFIG_PATH =
		"search_track";

	// Information about Search Track data logging
	private static final String SEARCH_LOGGER_CONFIG_PATH =
		"data_logging";

	// Information about Search Track reports
	private static final String SEARCH_REPORTS_CONFIG_PATH =
		"report_settings";

	// All about the Configured Database
	// Where to look for it
	public static final String DB_CONFIG_PATH =
		DBConfig.MAIN_ELEMENT_NAME; // "database";
	// Where it used to be

	public static final String LUCENE_SEARCH_CONFIG_PATH =
		"lucene_search";

	public static final String OLD_DB_CONFIG_PATH =
		SEARCH_TRACKING_CONFIG_PATH
		+ '/' + SEARCH_LOGGER_CONFIG_PATH
		+ '/' + SearchLogger.OLD_DB_CONFIG_PATH;

	private static final String _OLD_DB_CONFIG_PATH =
		SEARCH_TRACKING_CONFIG_PATH
		;



	// Some attributes of the main search names tag
	private static final String SN_PORT_ATTR = "port";

	// Security / Access Keys / Logins / Tokens / Session IDs, etc
	private static final String ADMIN_PWD_ATTR = "password";
	// The scrambled md5 version, not yet implemented
	private static final String _ADMIN_KEY_ATTR = "login_key";
	public static final int ADMIN_PWD_SECURITY_LEVEL = 5;
	private static final String OLD_ADMIN_PWD_ATTR = "admin_password";
	// Miles idea for a read-only password
	private static final String BROWSE_PWD_ATTR = "read_only_password";
	// The scrambled md5 version, not yet implemented
	private static final String _BROWSE_KEY_ATTR = "read_only_login_key";
	public static final int BROWSE_PWD_SECURITY_LEVEL = 3;
	// As of July '08 these are SCRAMBLED
	// via passwordToKey()
	private Hashtable cPasswordLevelTable;

	// Where to find map statements
	// Will usually have more than one "/map"
	// Typically relative to main Search names subtree
	private static final String FIXED_MAP_HEAD =
		"fixed_redirection_map";
	private static final String FIXED_MAP_PATH =
		FIXED_MAP_HEAD + "/map";

	private static final String DB_MAP_TOP_PATH =
		"db_redirection_map";



	// Where we specificy OUR own URL
	// Todo: Someday figure this out without being told
	private static final String SERVER_URL_PATH = "nie_server_url";
	// private static final String SERVER_URL_TEST_DRIVE_PATH = "search_names_url_test_drive";

	private static final String OLD_SEARCH_NAMES_URL_PATH = "search_names_url";
	
	// Where to look for user data declarations
	private static final String USER_DATA_DECLARATION_PATH =
		"markup_data_classes/markup";

	public static final String DEFAULT_NULL_SEARCH_MARKER = "(null)";

	public static final String NULL_SEARCHES_BASE_PATH = "null_searches";
	public static final String NULL_REDIRECT_ELEM = "redirect_on_null_search";
	public static final String NULL_REDIRECT_PATH =
		NULL_SEARCHES_BASE_PATH + "/" + NULL_REDIRECT_ELEM;
	public static final String ACTIVATE_NULL_REDIR_ATTR = "enabled";
	public static final boolean DEFAULT_ACTIVATE_NULL_REDIR = false;
	public static final String NULL_REDIR_URL_ELEM = "url";
	public static final String NULL_REDIR_URL_PATH =
		NULL_REDIRECT_PATH + "/" + NULL_REDIR_URL_ELEM;
	public static final String NULL_REDIR_TO_REFERRER_MARKER = "(referer)";
	public static final String NULL_REDIR_TO_REFERRER_MARKER2 = "(referrer)";
	public static final String NULL_EQUIV_PHRASE_ELEM = "null_search_equiv_phrase";
	public static final String NULL_EQUIV_PHRASE_PATH =
		NULL_SEARCHES_BASE_PATH + "/" + NULL_EQUIV_PHRASE_ELEM;
	
	
//	// Verbosity levels
//	private static final int VERBOSITY_QUIET = 0;
//	private static final int VERBOSITY_MINIMUM = 1;
//	// private static final String VNAME_MIN = "Status";
//	private static final int VERBOSITY_CHATTY = 2;
//	// private static final String VNAME_CHAT = "Info";
//	private static final int VERBOSITY_DEBUG = 3;
//	// private static final String VNAME_DEBUG = "Debug";

	// private static final int DEFAULT_PORT = 8080;
	// NO!  We do not act as a true proxy, so we should not use 8080
	// and besides everyone (at NIE) is used to 9000
	public static final int DEFAULT_PORT = 9000;

	public static final String DEFAULT_CONFIG_FILE_URI =
	// 	"searchnames_config.xml";
		"config/main.xml";
//	private static final int DEFAULT_VERBOSITY = VERBOSITY_MINIMUM;

	private static final String JUST_CHECK_CONFIG_CMD_LINE_OPT = "check_config";


	public static final String RUN_LOG_CONFIG_PATH = "run_logging";


	// Where we get css from
	public static final String DEFAULT_CSS_URI =
		SnRequestHandler.STATIC_FILE_PREFIX
		+ "style_sheets/nie_st.css";
		// was AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		// plus + "static_files/style_sheets/nie_st.css"
		;

	// Codes we will use to communicate about what is going on
	public static final int SN_ACTION_CODE_UNDEFINED = -1;
	// IMPORTANT NOTE:
	// Please make sure these are all >= zero,
	// negative numbers are special elsewhere
	public static final int SN_ACTION_CODE_NONE = 0;
	public static final int SN_ACTION_CODE_SUGGEST = 1;
	public static final int SN_ACTION_CODE_ALT = 2;
	public static final int SN_ACTION_CODE_SEARCH_REDIR = 3;
	public static final int SN_ACTION_CODE_AD = 4;
	public static final int SN_ACTION_CODE_AD_CODE = 5;

	// Codes about error / status conditions
	public static final int SN_STATUS_UNDEFINED = -1;
	// IMPORTANT NOTE:
	// Please make sure these are all >= zero,
	// negative numbers are special elsewhere
	public static final int SN_STATUS_OK = 0;
	public static final int SN_ERROR_READING_REQUEST = 1;
	public static final int SN_ERROR_BAD_MAP = 2;
	public static final int SN_ERROR_SENDING_RESPONSE = 3;
	public static final int SN_ERROR_PARSING_PAGE = 4;
	public static final int SN_ERROR_WITH_EXTERNAL_CALL = 5;

	// a little obscrty
	private static final String LNODE = "l" + "i"
		+ "ce" + "n"
		+ "se";
	private static final String LRV = "r"
		+ "e" + "v";
	private static final String LCO = "co"
		+ "mpa" + "ny";
	private static final String LST = "s" + "t"
		+ "ar" + "t_d"
		+ "ate";
	private static final String LND = "e" + "n"
		+ "d_da" + "te";
	private static final String LSRV = "s"
		+ "er" + "ve" + "r";
	private static final String LKY = "k"
		+ "e"
		+ "y";
	private static final long _MSID = 1000 * 3600 * 24;
	private static final long SUGGRC = 9 * NIEUtil.MS_PER_DAY;
	private static final long LOGGRC = 95 * NIEUtil.MS_PER_DAY;


	private void __sep__STATIC_Init__ () {}
	////////////////////////////////////////////////////////////

//	static
//	{

		// Keep a map of classes we might like to use for
		// propogating verbosity info


//		fClassListDescription = new Hashtable();
//
//		fClassListDescription.put( "SearchTuningApp",
//          "Main application, reads command line, high level logic and setup"
//		    );
//		fClassListDescription.put( "NIEUtil", EQUALS_TAG );
//		fClassListDescription.put( "JDOMHelper", EQUALS_TAG );
//		fClassListDescription.put( "AuxIOInfo", EQUALS_TAG );
//		fClassListDescription.put( "SnHTTPServer", EQUALS_TAG );
//		fClassListDescription.put( "SnRequestHandler", EQUALS_TAG );
//		fClassListDescription.put( "SnRedirectRecord", EQUALS_TAG );
//		fClassListDescription.put( "SearchEngineConfig", EQUALS_TAG );

//	}

};

