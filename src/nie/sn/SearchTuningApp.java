package nie.sn;

import java.io.*;
import java.util.*;
import java.net.*;

import nie.core.*;
// import nie.sn.*;
import org.jdom.Element;
import org.jdom.Document;


// TODO: ??? * Add file globbing to includes, to allow for entire directory scanning
// TODO: * Recheck detection/warnings for duplicate search term entries

public class SearchTuningApp implements Runnable
{

	private static final String kClassName = "SearchTuningApp";

	private static final String MODULE_DISPLAY_NAME =
		"NIE SearchTrack(tm)";
		// "NIE SearchTrack(tm) - Search Tuning and Reporting Server";
	private static final String MODULE_DISPLAY_VERSION = "2.9-e";
	public static String getModuleBanner()
	{
		return MODULE_DISPLAY_NAME
			+ ", Version "
			+ MODULE_DISPLAY_VERSION
			+ NIEUtil.NL
			;
	}

	public String getDetailedVersionBanner()
	{
		return getDetailedVersionBanner( null );
	}
	public String getDetailedVersionBanner( SearchTuningConfig inConfig )
	{
		String banner =
			getModuleBanner()
			+ "With core libraries: " + NIEUtil.getModuleBanner()
			;

		SearchTuningConfig lConfig = null;
		if( null != inConfig )
			lConfig = inConfig;
		else
			lConfig = getSearchTuningConfig();
		if( lConfig != null ) {
			banner += lConfig.getConfigurationBanner();
			banner += "Starting on: " + fCreationTimestamp + NIEUtil.NL;
		}
		else {
			banner += "Version Check / No configuration file given." + NIEUtil.NL;
			banner += "System date/time: " + fCreationTimestamp + NIEUtil.NL;
		}

		banner = banner + NIEUtil.getCopyrightBanner();
		return banner;
	}




//	// private static boolean debug = true;
//	// Don't specifically set it if you want FALSE, it'll default to that
//	private static boolean debug;
//	public static void setDebug( boolean flag )
//	{
//		debug = flag;
//	}

	// This is where the system kicks off
	// It parses the input arguments, then parses the
	// configuration table, then launches the server.
	//////////////////////////////////////////
	public static void main( String inArgs[] )
	{
		final String kFName = "main";

		SearchTuningApp lSearchNames = new SearchTuningApp( inArgs );

		// if(debug) System.err.println( "*** SearchTuningApp:main: calling .run() on search names object." );
		getRunLogObject().debugMsg( kClassName, kFName,
			"Have setup the SearchNames application, will now call .run()."
			);

		lSearchNames.run();

		// Do some checking for run log configuration
		// 1: Path must be specified
		// 2: Path must be absolute
		// NOT CHECKED at this time (checked later):
		// * Whether or not path exists and is writable
		// * Whether it's a directory or a file
		if( lSearchNames.fCheckRunlog )
		{
			getRunLogObject().statusMsg( kClassName, kFName,
				"Checking for proper run log entry in config file."
				+ " Location attribute must be present, and must be an"
				+ " absolute path name."
				);
			if( ! getRunLogImplObject().hasNonDefaultOutput() )
			{
				getRunLogObject().fatalErrorMsg( kClassName, kFName,
					"Told to check run log settings"
					+ ", but a specific path was not set (or had an error)."
					+ " Please set the " + RunLogBasicImpl.LOCATION_ATTR
					+ " attribute of the " + SearchTuningConfig.RUN_LOG_CONFIG_PATH
					+ " tag."
					+ " Exiting with error code 2."
					);
				System.exit( 2 );
			}
			else if( ! getRunLogImplObject().hasAbsolutePathOutput() )
			{
				getRunLogObject().fatalErrorMsg( kClassName, kFName,
					"Told to check run log settings"
					+ ", but a specified path is NOT absolute."
					+ " Found path \"" + getRunLogImplObject().getLastLocationURI() + "\"."
					+ " Exiting with error code 3."
					);
				System.exit( 3 );
			}
		}   // End of if checking run log path

		getRunLogObject().debugMsg( kClassName, kFName,
			"Back from calling .run(), now at end of .main()."
			);


	}

	// Trying this for Wrapper.exe, we'll see if it helps
	// Rather useless, main will call 2nd constructor with real args
	// but hey, if it makes them happy...
	public SearchTuningApp()
	{
		fCreationTimestamp = NIEUtil.getTimestamp();
		final String kFName = "constructor(1)";
		getRunLogObject().statusMsg( kClassName, kFName,
			"Zero argument Constructor."
			);
	}

	// This is where the work actually gets done
	// to start up the server.
	///////////////////////////////////////////
	public SearchTuningApp( String inArgs[] )
	{
		fCreationTimestamp = NIEUtil.getTimestamp();

		final String kFName = "constructor(2)";

		getRunLogObject().debugMsg( kClassName, kFName,
			"Starting, will parse command line."
			);

		// parse command line
		parseCommandLine( inArgs );

//		// Propogate any debugging flags
//		propogateVerbosityAndDebug();

		// If a config URI was given
		if( fConfigFileURI != null ) {
			getRunLogObject().statusMsg( kClassName, kFName,
				"Loading configuration data from \"" + fConfigFileURI + "\"."
				);

			// Try to configure the main engine
			try
			{
				fSnConfig = new SearchTuningConfig( fConfigFileURI, this );
			}
			// About the only thing that will cause this is if we were
			// not given a URL to the native search engine so we can
			// do passthroughs
			// catch( SearchTuningConfigFatalException e1 )
			// catch( SearchTuningConfigException e2 )
			catch( Exception e1 )
			{
				fSnConfig = null;
				fLastDitchSearchEngineURL = SearchEngineConfig.tryFetchingSearchEngineURLFromConfig( fConfigFileURI );
				fLastDitchPort = SearchTuningConfig.tryFetchingPortFromConfig( fConfigFileURI );
				fLastDitchPort = (fLastDitchPort > 0) ? fLastDitchPort : SearchTuningConfig.DEFAULT_PORT;

// stackTrace( kFName,
//	e1, "Config problem"
//	);

				if( null==fLastDitchSearchEngineURL )
					bailOnSetupError(
						"Unable to startup the SearchTrack server (1):"
						+ " Reason: " + e1
						);
				else {
					errorMsg( kFName,
						"Unable to configure the SearchTrack server, serious startup/configuration errors (1): " + e1
						+ " However, process does appear capable of processing pass-through searches to Host search engine."
						);
					stackTrace( kFName, e1, null );
					
				}

			}
	
			// Save the initial port
			// For now we don't allow you to change this
			if( fSnConfig != null )
			{
				fOriginalPortNumber = fSnConfig.getPort();
				getRunLogObject().statusMsg( kClassName, kFName,
					"Configured to listen on port " + fOriginalPortNumber + "."
					);
			}
			else {
				fOriginalPortNumber = fLastDitchPort;
			}

		}




//		// Read the config file in
//		loadConfigFile();
//		// Parse "global" options in the config file
//		readGlobalOptions();
//		// Setup the "mappings"
//		readAndSetupMapping();

		// start something???
	};


	private void propogateVerbosityAndDebug()
	{
		// Setup debugging

//		// Traditional NIE Classes
//		NIEUtil.setVerbosity( fVerbosity );
//		JDOMHelper.setVerbosity( fVerbosity );
//		AuxIOInfo.setVerbosity( fVerbosity );
//
//		// NIE Web Server Framework classes
//		// These will use this App's static methods
//		SearchTuningConfig.setVerbosity( fVerbosity );
//		SnHTTPServer.setVerbosity( fVerbosity );
//		SnRequestHandler.setVerbosity( fVerbosity );
//		SnRedirectRecord.setVerbosity( fVerbosity );
//		SearchEngineConfig.setVerbosity( fVerbosity );

//		if( debug )
//		{
//			// No need to do ouselves
//			// We also skip the trivial exception classes
//
//			SearchTuningConfig.setDebug( debug );
//			NIEUtil.setDebug( debug );
//			JDOMHelper.setDebug( debug );
//			AuxIOInfo.setDebug( debug );
//			SnHTTPServer.setDebug( debug );
//			SnRequestHandler.setDebug( debug );
//			SnRedirectRecord.setDebug( debug );
//			SearchEngineConfig.setDebug( debug );
//			// Others
//		}

	}



	///////
	//
	// Browse through the command line arguments
	//
	///////
	void parseCommandLine( String inArgs[] )
	{

		final String kFName = "parseCommandLine";

		// Set the defaults
		// fVerbosity = DEFAULT_VERBOSITY;
		// fConfigFileURI = DEFAULT_CONFIG_FILE_URI;
		// ^^^ let SearchTuningConfig handle defaults

		boolean haveSeenConfigURI = false;

		// For each argument on the command line
		for( int i = 0; i < inArgs.length; i++ )
		{
			// If the argument starts with a dash then it's a switch
			///////
			if( inArgs[i].startsWith( "-" ) )
			{
				String lFlag = inArgs[i].substring( 1 ).toLowerCase();

				// See if it's a verbosity flag
				// boolean result = getRunLogObject().setVerbosityByString( lFlag, false );
				boolean result = getRunLogImplObject().setVerbosityByString(
					inArgs[i], false
					);
//                  // ^^^ Must preserve original case when controlling logging
//                  //  and setVerbosityByString can handle optional leading hyphen
//					lFlag, false
//					);

				// If it's not verbosity, keep ckecking
				if( ! result )
				{
					if( lFlag.equals( "c" )
						|| lFlag.equals( JUST_CHECK_CONFIG_CMD_LINE_OPT )
						|| lFlag.equals( JUST_CHECK_CONFIG_CMD_LINE_OPT2 )
						)
					{
						fJustCheckConfig = true;
						getRunLogObject().debugMsg( kClassName, kFName,
							"Command line option " + (i+1)
							+ " is signal to just check syntax."
							);
					}
					else if( lFlag.equals( "v" )
						|| lFlag.equals( DISPLAY_VERSION_INFO )
						)
					{
						fJustShowVersion = true;
						getRunLogObject().debugMsg( kClassName, kFName,
							"Command line option " + (i+1)
							+ " is signal to just show version information."
							);
					}
					else if( lFlag.equals( CHECK_RUNLOG_CONFIG )
						)
					{
						fJustCheckConfig = true;
						fCheckRunlog = true;
						getRunLogObject().debugMsg( kClassName, kFName,
							"Command line option " + (i+1)
							+ " is signal to just test run log setting."
							);
					}
					else
					{
						// We don't know what it is
						bailOnBadSyntax( inArgs[i] );
					}
				}
			}
			else
			{
				// If it's not a switch then the only other thing it
				// can legally be is the name of an alternate config
				// file.
				///////
				if( ! haveSeenConfigURI )
				{
					fConfigFileURI = inArgs[i];
					haveSeenConfigURI = true;
					getRunLogObject().debugMsg( kClassName, kFName,
						"Command line option " + (i+1)
						+ " is config file name \"" + fConfigFileURI + "\"."
						);
				}
				else
				{
					getRunLogObject().fatalErrorMsg( kClassName, kFName,
						"Can only specify one config file"
						+ " on the command line."
						);
					bailOnBadSyntax( inArgs[i] );
				}
			}
		}
	}


//	void parseCommandLineOBS( String inArgs[] )
//	{
//		///////
//		//
//		// Browse through the command line arguments
//		//
//		///////
//
//		// Set the defaults
//		fVerbosity = DEFAULT_VERBOSITY;
//		// fConfigFileURI = DEFAULT_CONFIG_FILE_URI;
//
//		// For each argument on the command line
//		for( int i = 0; i < inArgs.length; i++ )
//		{
//			// If the argument starts with a dash then it's a switch
//			///////
//			if( inArgs[i].startsWith( "-" ) )
//			{
//				String lFlag = inArgs[i].substring( 1 ).toLowerCase();
//				if( lFlag.equals( "q" ) || lFlag.equals( "quiet" ) )
//					fVerbosity = VERBOSITY_QUIET;
//				else if( lFlag.equals( "s" ) || lFlag.equals( "status" ) )
//					fVerbosity = VERBOSITY_MINIMUM;
//				else if( lFlag.equals( "v" ) || lFlag.equals( "verbose" ) )
//					fVerbosity = VERBOSITY_CHATTY;
//				else if( lFlag.equals( "d" ) || lFlag.equals( "debug" ) )
//					fVerbosity = VERBOSITY_DEBUG;
//				else
//					// We don't know what it is
//					bailOnBadSyntax( inArgs[i] );
//			}
//			else
//			{
//				// If it's not a switch then the only other thing it
//				// can legally be is the name of an alternate config
//				// file.
//				///////
//				fConfigFileURI = inArgs[i];
//			}
//		}
//	}

//	// We go through a little hog wash to set the subtree as the main tree
//	// Otherwise we'd have to keep carrying around a prefix.
//	private void loadConfigFile()
//	{
//		fOverallMasterTree = null;
//		try
//		{
//			fOverallMasterTree = new JDOMHelper( fConfigFileURI );
//		}
//		catch (JDOMHelperException e)
//		{
//			bailOnSetupError( "JDOMHelperException 1: " + e );
//		}
//		if( fOverallMasterTree == null )
//			bailOnSetupError( "Got back a NULL xml tree"
//				+ " from file \"" + fConfigFileURI + "\""
//				);
//		Element tmpSNElem = fOverallMasterTree.findElementByPath(
//			MAIN_SN_CONFIG_PATH
//			);
//		if( tmpSNElem == null )
//			bailOnSetupError( "Unable to find Search Names specific config info"
//				+ " in the main configuration file."
//				+ " Expected node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//				+ " From config file \"" + fConfigFileURI + "\""
//				);
//		fConfigTree = null;
//		try
//		{
//			fConfigTree = new JDOMHelper( tmpSNElem );
//		}
//		catch (JDOMHelperException e)
//		{
//			bailOnSetupError( "JDOMHelperException 2: " + e );
//		}
//		if( fConfigTree == null )
//			bailOnSetupError( "Got back a NULL xml tree from Search Names node."
//				+ " Expected node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//				+ " From config file \"" + fConfigFileURI + "\""
//				);
//	}

//	private void readGlobalOptions()
//	{
//		fPortNumber = fConfigTree.getIntFromAttribute(
//			SN_PORT_ATTR, DEFAULT_PORT
//			);
//
//		if( getAdminPwd() == null )
//		{
//			System.err.println( "Warning: SearchNames init:"
//				+ " No administration password was set."
//				+ " You will not be able to perform administration with"
//				+ " a web browser or issue a shutdown command;"
//				+ " in this mode you will have to control-C or kill the process."
//				+ " Reminder: You can set a password in the "
//				+ ADMIN_PWD_ATTR + " attribute of the "
//				+ MAIN_SN_CONFIG_PATH + " tag."
//				+ " Will continue with initialization."
//				);
//		}
//
//		// Find the search engine info and instantiate it
//		Element lSEConfElem = fConfigTree.findElementByPath( SEI_CONFIG_PATH );
//		// No need to check for null, the constructor we're about
//		// to call will throw an exception for that
//		// Instantiate it
//		try
//		{
//			fSearchEngineConfig = new SearchEngineConfig( lSEConfElem );
//		}
//		catch (SearchEngineConfigException e)
//		{
//			bailOnSetupError(
//				"Unable to find/create host Search Engine's config info."
//				+ " Reason/exception: \"" + e + "\""
//				+ " Configuration reminder: it's \"" + SEI_CONFIG_PATH + "\""
//				+ " under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//				+ " Reaading from config file \"" + fConfigFileURI + "\""
//				);
//		}
//
//
//	}

//	// Look for mapped statements and create redirect objects for each
//	private void readAndSetupMapping()
//	{
//		// Init the hash, no matter what
//		fTermHashMap = new Hashtable();
//
//		// We currently only support FIXED maps
//
//		List maps = fConfigTree.findElementsByPath( FIXED_MAP_PATH );
//		if( maps == null || maps.size() < 1 )
//		{
//			System.err.println( "WARNING: SearchNames:readAndSetupMapping:"
//				+ " No redirect mappings found in config file."
//				+ " Maybe you're planning to add some interactively?"
//				+ " (Todo: Not yet implemented.)"
//				+ " Will still listen on port until shut down."
//				+ " Config path should be \"" + FIXED_MAP_PATH + "\""
//				+ " under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//				+ " Reaading from config file \"" + fConfigFileURI + "\""
//				);
//			return;
//		}
//
//		// Currently, every term and URL combo is turned into
//		// a unique record
//
//		// For each mapping statement
//		int lMapCount = 0;
//		for( Iterator it1 = maps.iterator(); it1.hasNext(); )
//		{
//			// Get this map
//			Element mapElem = (Element)it1.next();
//			lMapCount++;
//
//			// For each term in the mapping statement
//			// Get the terms for this map
//			List terms = JDOMHelper.getTextListByPathNotNullTrim(
//				mapElem, SEARCH_TERM_PATH
//				);
//			// Sanity check
//			if( terms == null || terms.size() < 1 )
//			{
//				System.err.println( "Warning: SearchNames:readAndSetupMapping:"
//					+ " No search terms found in map # " + lMapCount + "."
//					+ " Maybe you're planning to add some interactively?"
//					+ " (Todo: Not yet implemented.)"
//					+ " Will continue to read in the remainder of the redirect maps."
//					+ " There should be at least one \"" + SEARCH_TERM_PATH + "\" tag"
//					+ " under each mapping tag \"" + FIXED_MAP_PATH + "\""
//					+ " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//					+ " Reaading from config file \"" + fConfigFileURI + "\""
//					);
//				continue;
//			}
//
//
//			// Get the urls for this map
//			List urlElems = JDOMHelper.findElementsByPath(
//				mapElem, URL_PATH
//				);
//			// Get the alternative term elements for this map
//			List altElems = JDOMHelper.findElementsByPath(
//				mapElem, ALTTERM_PATH
//				);
//
//			// Now combine these two lists
//			List combinedElemList = new Vector();
//			if( urlElems != null )
//				combinedElemList.addAll( urlElems );
//			if( altElems != null )
//				combinedElemList.addAll( altElems );
//
//			// Sanity check
//			if( combinedElemList == null || combinedElemList.size() < 1 )
//			{
//				System.err.println( "Warning: SearchNames:readAndSetupMapping:"
//					+ " No URLs or alternative terms found in map # " + lMapCount + "."
//					+ " Maybe you're planning to add some interactively?"
//					+ " (Todo: Not yet implemented.)"
//					+ " Will skip all the terms in this mapped keyword set,"
//					+ " but will continue to read in the remainder of the redirect maps."
//					+ " There should be at least one \"" + URL_PATH + "\" tag"
//					+ " and/or one \"" + ALTTERM_PATH + "\" tag"
//					+ " under each mapping tag \"" + FIXED_MAP_PATH + "\""
//					+ " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//					+ " Reaading from config file \"" + fConfigFileURI + "\""
//					);
//				continue;
//			}
//
//			// Loop through terms
//			for( Iterator it2 = terms.iterator(); it2.hasNext(); )
//			{
//				// Grab the term and normalize it
//				String term = (String)it2.next();
//				term = term.toLowerCase();
//
//				// Loop through the URL and alternative terms elements
//				int recordCounter=0;
//				for( Iterator it3 = combinedElemList.iterator(); it3.hasNext(); )
//				{
//					// Grab the URL JDOM tree
//					Element urlElem = (Element)it3.next();
//					recordCounter++;
//
//					// Create a Redirect record
//					SnRedirectRecord urlRecord;
//					try
//					{
//						urlRecord = new SnRedirectRecord(
//							term, urlElem
//							);
//					}
//					catch (Exception e)
//					{
//						System.err.println(
//							"Warning: SearchNames:readAndSetupMapping:"
//							+ " Unable to construct valid redirect record"
//							+ " for url/term # " + recordCounter
//							+ " in map # " + lMapCount + "."
//							+ " Exception was \"" + e + "\"."
//							+ " Maybe you're planning to add some interactively?"
//							+ " (Todo: Not yet implemented.)"
//							+ " Will continue to process any other URLs in this map set."
//							+ " URLs are declared in \"" + URL_PATH + "\" tags"
//							+ " under each mapping tag \"" + FIXED_MAP_PATH + "\""
//							+ " which are under SN node path = \"" + MAIN_SN_CONFIG_PATH + "\""
//							+ " Reaading from config file \"" + fConfigFileURI + "\""
//							);
//						continue;
//					}
//
//					// Now add it to the big map!
//					List termURLVector = null;
//					// If we have an existing vector for this term, grab it
//					if( fTermHashMap.containsKey( term ) )
//						termURLVector = (List)fTermHashMap.get( term );
//					// Otherwise, create a fresh new vector for it
//					else
//						termURLVector = new Vector();
//
//					// Now add this new record to the end of the vector
//					termURLVector.add( urlRecord );
//
//					// And update the hash with the new/revised vector
//					fTermHashMap.put( term, termURLVector );
//
//				}   // End for each URL
//
//			}   // End for each Term
//
//		}   // End for each mapping statement
//		// fTermHashMap
//	}


	private void bailOnBadSyntax( String inOpt )
	{
		bailOnBadSyntax( inOpt, null );
	}
	private void bailOnBadSyntax( String inOpt, String optMsg )
	{
		final String kFName = "bailOnBadSyntax";


		String msg = "Bad Command Line Syntax: ";
		if( optMsg != null )
			msg += optMsg;
		else
			msg += "Unknown option \"" + inOpt + "\"";
		msg += NIEUtil.NL;

		msg = msg + "Major Options:"
			+ NIEUtil.NL
			+ "\t[-c | -check[_config]]"
			+ NIEUtil.NL
			+ "\t[-check_runlog_path]"
			+ NIEUtil.NL		
			+ "\t[-v | -version]"
			+ NIEUtil.NL
			+ "\tconfig_file_name_or_url"
			+ NIEUtil.NL
			;

		// Add the verbosity synax, with hyphens
//		msg = msg + getRunLogObject().getVerbosityLevelDescriptions(
//			true, true, true, true
//			);
		msg = msg + RunLogBasicImpl.getVerbosityLevelDescriptions(
			true, true, true, true
			);

		getRunLogObject().fatalErrorMsg( kClassName, kFName,
			msg
			);


		System.exit( 1 );
	}


	private void bailOnSetupError( String inMsg )
	{
		final String kFName = "bailOnSetupError";

		String msg = "Failed to Initialize:"
			+ " Reason: \"" + inMsg + "\""
			;

		getRunLogObject().fatalErrorMsg( kClassName, kFName,
			msg
			);

		if( fConfigFileURI == null )
			bailOnBadSyntax( null, "Did you forget to specify a config file?" );

		System.exit( 2 );
	}

	//////////////////////////////////////////
	//
	// Start the searnames server
	//
	//////////////////////////////////////////

	public void run_old()
	{

		final String kFName = "run_old";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// getShouldStartServer() moved to later in code

		// Start out assuming we do want to run
		fShouldStopNow = false;

		// If we just want the SHORT one
		if( ! getShouldShowFullVersionInfo() )
		{
			getRunLogObject().debugMsg( kClassName, kFName,
				"Showing short version banner instead of long banner"
				);
			getRunLogObject().statusMsg( kClassName, kFName,
				getModuleBanner().trim()
				);
		}


		// In the case of a version check, we may have no server
		// so don't to run it
		// USUALLY we DO do this section of code
		boolean isBadServer = false;
		if( null!=getSearchTuningConfig() )
		{
			getRunLogObject().debugMsg( kClassName, kFName,
					"Non-Null Search Track Config, so will create SnHTTPServer"
					);

			// construct the HTTP server
			fPrimarySnServer = new SnHTTPServer(
				getPort(),
				this,
				getSearchTuningConfig(),
				getShouldStartServer()
				);

			// Was there a problem, was it null?	
			if( null == fPrimarySnServer )
			{
				getRunLogObject().debugMsg( kClassName, kFName,
						"Got back a Null SnHTTPServer"
						);

				// We normally bail at this point
				// Either there was a config and it was bad
				// Or no config was found or given, which is also bad
				// We do catch the one case of no config and version checking
				// and let the logic below complain about that

				if( ! getShouldShowFullVersionInfo() )
				{			
					getRunLogObject().debugMsg( kClassName, kFName,
							"Bailing immediately because of Null SnHTTPServer"
							);
					String tmpMsg = kExTag
						+ "Unable to instantiate SnHTTPServer server."
						+ " Fatal error, exiting."
						;
					bailOnSetupError( tmpMsg );
				}
				// Else caught below
			}
			// Else the primary server is OK, and is NOT null
			else
			{
				getRunLogObject().debugMsg( kClassName, kFName,
						"Good, now have Non-Null SnHTTPServer"
						);
				// isGoodServer = true;
				if( ! isBadServer && getShouldCheckConfig() ) {
					getRunLogObject().statusMsg( kClassName, kFName,
						"In version / config-check mode only (1a),"
						+ " so will not actually start server."
						+ " If you're seeing this message then the system"
						+ " was able to initialize properly."
						);
				}
				else {
					getRunLogObject().debugMsg( kClassName, kFName,
							"isBadServer=" + isBadServer
							+ ", getShouldCheckConfig()="
							+ getShouldCheckConfig()
							);
				}
			}
		}
		// Else there was no configuration tree at all
		// either because one wasn't given, or something failed
		else
		{
			getRunLogObject().debugMsg( kClassName, kFName,
					"Have Null Search Track Config, but might still create SnHTTPServer"
					);

			// We're supposed to start the server, but have no valid config
			if( getShouldStartServer() ) {
				getRunLogObject().debugMsg( kClassName, kFName,
					"Even with Null Search Track Config, we would still like to create an SnHTTPServer just to allow passthroughs, if possible."
					+ " fLastDitchPort=" + fLastDitchPort
					+ ", fLastDitchSearchEngineURL=" + fLastDitchSearchEngineURL
					);
				if( fLastDitchPort > 0 && null!=fLastDitchSearchEngineURL ) {
					// construct a passthrough HTTP server
					fPrimarySnServer = new SnHTTPServer(
						fLastDitchPort,
						this,
						null,
						true
						);
					isBadServer = true;
				}
				else {
					getRunLogObject().debugMsg( kClassName, kFName,
							"Not even enough info for passthrough."
							+ " fLastDitchPort=" + fLastDitchPort
							+ ", fLastDitchSearchEngineURL=" + fLastDitchSearchEngineURL
							);
				}
			}
			// else NOT getShouldStartServer()
			else {
				getRunLogObject().debugMsg( kClassName, kFName,
						"Null Search Track Config and was not asked to start server."
						);
			}


			// Was there an attempt to look at a config?
			// If so, then report a problem
			if( fConfigFileURI != null && ! isBadServer )
			{
				String tmpMsg = kExTag
					+ "In version / config-check mode only,"
					+ " but there was a problem setting up the server."
					;
				bailOnSetupError( tmpMsg );
			}

			// So no tree, and no config file given

			// If somebody was doing a check config, then this is WRONG
			if( getShouldCheckConfig() )
			{
				String tmpMsg = kExTag
					+ "In config-check mode, but no configration given"
					+ " and was not able to load a default config file."
					;
				bailOnBadSyntax( null, tmpMsg );
			}
			// Todo: seems like we should have caught this before now, in parseCmdLine?

			// OK, we didn't start up, and no config file was given
			getRunLogObject().statusMsg( kClassName, kFName,
				"In version / config-check mode only??? (2b)"
				// + ", so will not actually start server."
				// + " If you're seeing this message then the system"
				// + " was able to initialize properly."
				);
		}
		// Now print the full version info if requested
		if( getShouldShowFullVersionInfo() )
		{
			System.err.println( getDetailedVersionBanner().trim() );
		}
		// And exit out if we're not really going to start
		if( ! getShouldStartServer() )
			return;

//		getRunLogObject().debugMsg( kClassName, kFName,
//			"*** SearchTuningApp:run: Calling .run() on http server object."
//			);

		// Prepare a nice version banner
		List kBannerStrings = new Vector();
		if( ! isBadServer ) {

			kBannerStrings.add(
				"Initialization Complete, the NIE Server is Starting"
				);
	
			kBannerStrings.add( MODULE_DISPLAY_NAME + " version " + MODULE_DISPLAY_VERSION );

			kBannerStrings.add( "" );


			double days = getSearchTuningConfig().getLicExpDays();
			if( days < 0 ) {
				kBannerStrings.add( "ER" + "ROR: " + "YO" + "UR LI" + "CENSE HAS EX" + "PIRE" + "D !" + '!' + '!' );
				kBannerStrings.add( "ple" + "ase con" + "tact sale" + "s" + '@' + "ide" + "aeng.com" );
			}

			String co = getSearchTuningConfig().getLicCoStr();
			if( null!=co )
				kBannerStrings.add( "Li" + "ce" + "nsed to \"" + co + "\"" );

			String msg = "";
			String startDt = getSearchTuningConfig().getLicStartDate();
			if( null!=startDt )
				msg += "fr" + "om " + startDt + " ";
			String endDt = getSearchTuningConfig().getLicEndDate();
			if( null!=endDt )
				msg += "thr" + "ough " + endDt + " ";
			if( days < 0 )
				msg += "(li" + "cen" + "se has E" + 'X' + "PIR" + "ED" + '!' + ")";
			else {
				String daysStr = ""+days;
				if( daysStr.endsWith(".0") && daysStr.length()>2 )
					daysStr = daysStr.substring( 0, daysStr.length()-2 );
				msg += "(ex" + "pir" + "es in " + daysStr + " da" + "ys)";
			}
			kBannerStrings.add( msg );

			String server = getSearchTuningConfig().getLicSrvStr();
			if( null!=server )
				kBannerStrings.add( "to r" + "un on se" + "rv" + "er \"" + server + "\"" );


			kBannerStrings.add( "" );

			kBannerStrings.add( "Upp at " + NIEUtil.getTimestamp() + " on port " + getPort() );
			kBannerStrings.add( "using " + getConfigFileURI() );
			kBannerStrings.add( "at url " + getSearchTuningConfig().getSearchNamesURL() );
	
			// kBannerStrings.add( MODULE_DISPLAY_NAME );
			// kBannerStrings.add( "version " + MODULE_DISPLAY_VERSION );
	
			// kBannerStrings.add( "Up at " + NIEUtil.getTimestamp() );
			// kBannerStrings.add( "on port " + getPort() );

		}
		// We're doing passthrough only
		else {

			kBannerStrings.add( "" );
			kBannerStrings.add(
				"ERROR: Initialization FAILED !!!"
				);
			kBannerStrings.add(
				"---------------------========---"
				);
			kBannerStrings.add( "" );
			kBannerStrings.add(
				"NIE Server is starting in FAIL-OVER pass-through mode."
				);
			kBannerStrings.add(
				"Users' searches will be passed through to the host search engine,"
				);
			kBannerStrings.add(
				"but other parts of the system are DOWN (suggestions, reports, etc)"
				);
			kBannerStrings.add(
				"Please check error log and configuration!"
				);
			kBannerStrings.add( "" );
	
			kBannerStrings.add( MODULE_DISPLAY_NAME + " version " + MODULE_DISPLAY_VERSION );
			kBannerStrings.add( "Up at " + NIEUtil.getTimestamp() + " on tentative port " + fLastDitchPort );
			kBannerStrings.add( "with BAD config " + getConfigFileURI() );
			kBannerStrings.add( "passing through to Search Engine URL" );
			kBannerStrings.add( fLastDitchSearchEngineURL );
			kBannerStrings.add( "" );

		}

		String bannerStr = isBadServer ? "!" : null;

		String kBigMsg =
			NIEUtil.NL
			+ NIEUtil.NL
			+ NIEUtil.starBanner( kBannerStrings, bannerStr )
			;

		getRunLogObject().statusMsg( kClassName, kFName,
			"Init Complete."
			+ kBigMsg
			);


		// We need to know SnServer's thread later so we can send it
		// an interrupt
		// fSnServerThread = Thread.currentThread();
		// fPrimarySnServer.run();
		// ^^^ Huh???  I think that's totally wrong
		fSnServerThread = new Thread( fPrimarySnServer );
		fSnServerThread.start();

		// if good config
		if( ! isBadServer ) {
			try {
				CronLite cron = new CronLite( this );
				mCronLiteThread = new Thread( cron );
				mCronLiteThread.start();
			}
			catch( Exception e ) {
				errorMsg( kFName, "Unable to start CronLite thread. Error: " + e );
			}
		}

		// getRunLogObject().debugMsg( kClassName, kFName,
		//	"Back from .run() on http server object."
		//	);

		// Let them know we had some type of problem on startup
		if( isBadServer )
			System.exit( 10 );

		// Todo: should we call .start() instead? with a thread?
		// So later we would create a new thread, register that
		// with snserver, and then store that
	}

	// Paths:
	// (we do want to run == getShouldStartServer()
	// 1 Server start and config good
	// 2 Server start and config bad
	// 3 Server start but startup fails
	// - - - - -
	// 4 Version check
	// 5 Config check and good
	// 6 Config check and bad
	//
	// General steps IF we are starting up and everything is OK:
	// 0: Declare that we do want to run
	// 1: Print short version banner
	// 2: Instantiate SnHTTPServer with SnConfig (AND flag that we are serious?)
	// 3: Create banner
	// 4: Create thread
	// 5: start thread
	// 6: Create cron
	// 7: System.exit(error) if bad
	// 
	public void run()
	{
		
		final String kFName = "run";
		final String kExTag = kClassName + '.' + kFName + ": ";

		boolean isBadServer = false;

		// Paths 1, 2 and 3, assuming we DO want to startup
		// ===============================================================
		// getShouldStartServer() = ! getShouldCheckConfig() && ! getShouldShowFullVersionInfo();
		if( getShouldStartServer() ) {
			// Start out assuming we do want to run
			fShouldStopNow = false;
			getRunLogObject().debugMsg( kClassName, kFName,
					"Showing short version banner instead of long banner (1)"
					);
			getRunLogObject().statusMsg( kClassName, kFName,
				getModuleBanner().trim()
				);

			// If we DO have a Config object, try to create a server
			// paths 1 and 3 ok so far
			if( null!=getSearchTuningConfig() )
			{
				getRunLogObject().debugMsg( kClassName, kFName,
						"Non-Null Search Track Config, so will create SnHTTPServer"
						);

				// construct the HTTP server
				fPrimarySnServer = new SnHTTPServer(
					getPort(),
					this,
					getSearchTuningConfig(),
					getShouldStartServer()
					);

				// Was there a problem, was it null?	
				if( null == fPrimarySnServer )
				{
					getRunLogObject().debugMsg( kClassName, kFName,
							"Got back a Null SnHTTPServer"
							);
					// We normally bail at this point
					// Either there was a config and it was bad
					// Or no config was found or given, which is also bad
					// We do catch the one case of no config and version checking
					// and let the logic below complain about that
					getRunLogObject().debugMsg( kClassName, kFName,
							"Bailing immediately because of Null SnHTTPServer"
							);
					String tmpMsg = kExTag
						+ "Unable to instantiate SnHTTPServer server."
						+ " Fatal error, exiting."
						;
					bailOnSetupError( tmpMsg );
				}
				// OK so far!
				getRunLogObject().debugMsg( kClassName, kFName,
						"Good, now have Non-Null SnHTTPServer with config"
						);
			}
			// Else NULL Config, but try passthrough
			// path 2, bad config
			else {
				isBadServer = true;
				getRunLogObject().debugMsg( kClassName, kFName,
						"Have Null Search Track Config, but might still create SnHTTPServer"
						);

				// We're supposed to start the server, but have no valid config
				if( getShouldStartServer() ) {
					getRunLogObject().debugMsg( kClassName, kFName,
						"Even with Null Search Track Config, we would still like to create an SnHTTPServer just to allow passthroughs, if possible."
						+ " fLastDitchPort=" + fLastDitchPort
						+ ", fLastDitchSearchEngineURL=" + fLastDitchSearchEngineURL
						);
					if( fLastDitchPort > 0 && null!=fLastDitchSearchEngineURL ) {
						// construct a passthrough HTTP server
						fPrimarySnServer = new SnHTTPServer(
							fLastDitchPort,
							this,
							null,
							true
							);
					}
					// Else we don't have enough info to even try
					else {
						bailOnSetupError( "Unable to create passthrough SnHTTPServer server. (1)" );
					}
					if( null==fPrimarySnServer )
						bailOnSetupError( "Unable to create passthrough SnHTTPServer server. (2)" );
				}
			}	// End else NULL config

			// At this point, if we're still here at all, we have
			// SnHTTPServer instance of some sort

			// Prepare a nice version banner
			List kBannerStrings = new Vector();
			if( ! isBadServer ) {
				kBannerStrings.add(
					"Initialization Complete, the NIE Server is Starting"
					);
				kBannerStrings.add( MODULE_DISPLAY_NAME + " version " + MODULE_DISPLAY_VERSION );
				kBannerStrings.add( "" );

				double days = getSearchTuningConfig().getLicExpDays();
				if( days < 0 ) {
					kBannerStrings.add( "ER" + "ROR: " + "YO" + "UR LI" + "CENSE HAS EX" + "PIRE" + "D !" + '!' + '!' );
					kBannerStrings.add( "ple" + "ase con" + "tact sale" + "s" + '@' + "ide" + "aeng.com" );
				}

				String co = getSearchTuningConfig().getLicCoStr();
				if( null!=co )
					kBannerStrings.add( "Li" + "ce" + "nsed to \"" + co + "\"" );

				String msg = "";
				String startDt = getSearchTuningConfig().getLicStartDate();
				if( null!=startDt )
					msg += "fr" + "om " + startDt + " ";
				String endDt = getSearchTuningConfig().getLicEndDate();
				if( null!=endDt )
					msg += "thr" + "ough " + endDt + " ";
				if( days < 0 )
					msg += "(li" + "cen" + "se has E" + 'X' + "PIR" + "ED" + '!' + ")";
				else {
					String daysStr = ""+days;
					if( daysStr.endsWith(".0") && daysStr.length()>2 )
						daysStr = daysStr.substring( 0, daysStr.length()-2 );
					msg += "(ex" + "pir" + "es in " + daysStr + " da" + "ys)";
				}
				kBannerStrings.add( msg );

				String server = getSearchTuningConfig().getLicSrvStr();
				if( null!=server )
					kBannerStrings.add( "to r" + "un on se" + "rv" + "er \"" + server + "\"" );

				kBannerStrings.add( "" );

				kBannerStrings.add( "Up at " + NIEUtil.getTimestamp() + " on port " + getPort() );
				kBannerStrings.add( "using config " + getConfigFileURI() );
				// moved later: kBannerStrings.add( "on URL " + getSearchTuningConfig().getSearchNamesURL() );

				try {
					URL adminUrl = new URL( new URL(getSearchTuningConfig().getSearchNamesURL()), SnRequestHandler.PRINARY_ADMIN_PATH );
					kBannerStrings.add( "admin URL " + adminUrl.toExternalForm() );
				}
				catch( MalformedURLException e ) {
					errorMsg( kFName, "Unable to form Admin URL for display from '"
						+ getSearchTuningConfig().getSearchNamesURL()
						+ "' and '" + SnRequestHandler.PRINARY_ADMIN_PATH + "'"
						+ " Error: " + e
						);
					// Backup plan, at lest give them the main URL
					kBannerStrings.add( "main URL " + getSearchTuningConfig().getSearchNamesURL() );			
				}
				
				// kBannerStrings.add( MODULE_DISPLAY_NAME );
				// kBannerStrings.add( "version " + MODULE_DISPLAY_VERSION );
		
				// kBannerStrings.add( "Up at " + NIEUtil.getTimestamp() );
				// kBannerStrings.add( "on port " + getPort() );
			}
			// We're doing passthrough only
			else {
				kBannerStrings.add( "" );
				kBannerStrings.add(
					"ERROR: Configuration FAILED !!!"
					);
				kBannerStrings.add(
					"--------------------========---"
					);
				kBannerStrings.add( "" );
				kBannerStrings.add(
					"NIE Server will attempt to start in FAIL-OVER pass-through mode."
					);
				kBannerStrings.add(
					"Users' searches will be passed through to the host search engine,"
					);
				kBannerStrings.add(
					"but other parts of the system are DOWN (suggestions, reports, etc)"
					);
				kBannerStrings.add(
					"Please check error log and configuration!"
					);
				kBannerStrings.add( "" );
		
				kBannerStrings.add( MODULE_DISPLAY_NAME + " version " + MODULE_DISPLAY_VERSION );
				kBannerStrings.add( "Up at " + NIEUtil.getTimestamp() + " on tentative port " + fLastDitchPort );
				kBannerStrings.add( "with BAD config " + getConfigFileURI() );
				kBannerStrings.add( "passing through to Search Engine URL" );
				kBannerStrings.add( fLastDitchSearchEngineURL );
				kBannerStrings.add( "" );
			}

			String bannerStr = isBadServer ? "!" : null;
			String kBigMsg = NIEUtil.NL + NIEUtil.NL
				+ NIEUtil.starBanner( kBannerStrings, bannerStr )
				;

			getRunLogObject().statusMsg( kClassName, kFName,
				"Init Complete."
				+ kBigMsg
				);

			// We need to know SnServer's thread later so we can send it
			// an interrupt
			// fSnServerThread = Thread.currentThread();
			// fPrimarySnServer.run();
			// ^^^ Huh???  I think that's totally wrong
			fSnServerThread = new Thread( fPrimarySnServer );
			// Run it!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			fSnServerThread.start();

			// if good config
			if( ! isBadServer ) {
				try {
					CronLite cron = new CronLite( this );
					mCronLiteThread = new Thread( cron );
					mCronLiteThread.start();
				}
				catch( Exception e ) {
					errorMsg( kFName, "Unable to start CronLite thread. Error: " + e );
				}
			}

			// Let them know we had some type of problem on startup
			// if( isBadServer )
			//	System.exit( 10 );

		}
		//
		// Else paths 4, 5 and 6, we do not intend to start the server
		// ===============================================================
		// 4 Version check
		// 5 Config check and good
		// 6 Config check and bad
		else {
			// Path 4, version and exit
			// Now print the full version info if requested
			if( getShouldShowFullVersionInfo() )
			{
				System.err.println( getDetailedVersionBanner().trim() );
				System.exit(0);
			}

			// Short banner
			getRunLogObject().statusMsg( kClassName, kFName,
				getModuleBanner().trim()
				);

			// Only have paths 5 and 6 left
			// Both are to check config
			// If not null, then it was OK
			// If null, not good
			// Path 5, check and it's good
			if( null!=getSearchTuningConfig() ) {
				getRunLogObject().statusMsg( kClassName, kFName,
					"In version / config-check mode only."
					+ " Got back a valid configuration."
					+ " Please review logs for any warning messages."
					);
				// System.exit(0);
				// return;
				// Just let it fall through
			}
			// Else path 6, not OK
			else {
				getRunLogObject().errorMsg( kClassName, kFName,
					"In version / config-check mode only."
					+ " Did NOT get back a valid configuration."
					+ " See logs for error and warning messages."
					);
				System.exit(5);
			}
		}

	}

	
	public boolean refreshConfig()
	{
		final String kFName = "refreshConfig";

		getRunLogObject().statusMsg( kClassName, kFName,
			"Will refresh configuration from file \"" + fConfigFileURI + "\"."
			);

		if( fIsDoingRefresh )
		{
			getRunLogObject().errorMsg( kClassName, kFName,
				"Asked to refresh / reload config data"
				+ " but a refresh is already in progress (1)."
				+ " Perhaps two requests were submitted in rapid succession?"
				+ " Ignoring this request."
				);
			return false;
		}

		// We will be very careful!!!
		synchronized( fRefreshLock )
		{

			// This really shouldn't happen, but doesn't hurt to check again
			if( fIsDoingRefresh )
			{
				getRunLogObject().errorMsg( kClassName, kFName,
					"Asked to refresh / reload config data"
					+ " but a refresh is already in progress (2)."
					+ " Perhaps two requests were submitted in rapid succession?"
					+ " Ignoring this request."
					);
				return false;
			}

			// Now we make sure to grab it
			fIsDoingRefresh = true;

			// Now we attempt to load the new config
			SearchTuningConfig newSnConfig = null;
			// Try to configure the main engine
			try
			{
				newSnConfig = new SearchTuningConfig( fConfigFileURI, this );
			}
			// catch( SearchTuningConfigException e )
			// catch both SearchTuningConfigException and SearchTuningConfigFatalException
			catch( Exception e )
			{
				getRunLogObject().errorMsg( kClassName, kFName,
					"Unable to load new config info."
					+ " Retaining old config info."
					+ " Reason: " + e
					);

				// If there was no valid config from before either, they
				// may have at least updated the search engine URL, so
				// look for that
				// If NEVER had a valid config
				if( null==fSnConfig ) {
					String tmpURL = SearchEngineConfig.tryFetchingSearchEngineURLFromConfig( fConfigFileURI );
					if( null!=tmpURL ) {
						statusMsg( kFName,
							"Was able to at least find a search engine URL for pass-through searches: "
							+ " \"" + tmpURL + "\""
							+ ( null!=fLastDitchSearchEngineURL ? ", which replaces previous \"" + fLastDitchSearchEngineURL + "\"" : "" )
							);
						// We always take the newest one, if we have one
						fLastDitchSearchEngineURL = tmpURL;
					}
				}

				fIsDoingRefresh = false;
				return false;
			}

			getRunLogObject().debugMsg( kClassName, kFName,
				"Have read new config, will now check ports."
				);

			// So we seem to have a new config that we like

			// The one last thing is that it should be on the same port
			int newPort = newSnConfig.getPort();
			// Check that the ports match
			if( fOriginalPortNumber != newPort )
			{
				getRunLogObject().errorMsg( kClassName, kFName,
					"The SearchNames port that is set in the new configuration"
					+ " does not match up with what the previous configuration"
					+ " had set it to."
					+ " You can not dynamically change ports; this is one"
					+ " of the few changes that actually requires a true"
					+ " shutdown of the server and restart."
					+ " Requested new port = " + newPort
					+ " Original port = " + fOriginalPortNumber
					+ " Ignoring this command."
					);
				fIsDoingRefresh = false;
				return false;
			}

			getRunLogObject().debugMsg( kClassName, kFName,
				"Port settings are OK, about to switch over to new config."
				);

			// OK, drum roll please, we will put this in place!
			fSnConfig = newSnConfig;

			// We're done
			fIsDoingRefresh = false;

			List kBannerStrings = new Vector();
			kBannerStrings.add(
				"Re-initialization Complete, new configuration is active."
				);
			
			kBannerStrings.add( MODULE_DISPLAY_NAME + " version " + MODULE_DISPLAY_VERSION );
			kBannerStrings.add( "Refreshed at " + NIEUtil.getTimestamp() + " on port " + getPort() );
			kBannerStrings.add( "using " + getConfigFileURI() );
			
			// kBannerStrings.add( MODULE_DISPLAY_NAME );
			// kBannerStrings.add( "version " + MODULE_DISPLAY_VERSION );
			
			// kBannerStrings.add( "Up at " + NIEUtil.getTimestamp() );
			// kBannerStrings.add( "on port " + getPort() );
			
			String kBigMsg =
				NIEUtil.NL
				+ NIEUtil.NL
				+ NIEUtil.starBanner( kBannerStrings )
				;
			
			getRunLogObject().statusMsg( kClassName, kFName,
				"RE-Init Complete."
				+ kBigMsg
				);







		}   // End of synchronized block

		getRunLogObject().debugMsg( kClassName, kFName,
			"Done."
			);

		// Tell them that we did do it
		return true;

	}


	// Orderly shutdown to the server
	public boolean getShouldStopNow()
	{
		// final String kFName = "getShouldStopNow";

//		getRunLogObject().debugMsg( kClassName, kFName,
//			"Returning " + fShouldStopNow
//			);
		// ^^^ causes null pointer exception at aopa, very strange

		return fShouldStopNow;
	}
	public void setShouldStopNow()
	{
		final String kFName = "setShouldStopNow";
		fShouldStopNow = true;
		getRunLogObject().debugMsg( kClassName, kFName,
			"Have set flag to " + fShouldStopNow
			+ "  Please Note: I do not take any specific action to actually shutdown;"
			+ " that is done by forceStopNow()"
			);

		// Test shutting it down now
		// forceStopNow();
	}
	public void forceStopNow()
	{
		final String kFName = "forceStopNow";

		getRunLogObject().statusMsg( kClassName, kFName,
			"Will now close / shut down main server socket."
			);

		// fPrimarySnServer.closeMainSocket();
		// Tell it which thread to "bonk"
		fPrimarySnServer.closeMainSocket( fSnServerThread );

		getRunLogObject().debugMsg( kClassName, kFName,
			"Back from closing main server socket."
			);


//      maybe sleep before we timeout
//		try {
//		Thread.currentThread().sleep( 200 );
//		} catch (InterruptedException e){
//		}

		getRunLogObject().debugMsg( kClassName, kFName,
			"Calling systems exit method with 0 (success)."
			);
		System.exit( 0 );



		// VVV moved to close socket method in sn http server
		// vvv None of this seems to work
		// socket.accept() doesn't seem to respond to such things
//
//		// Make sure we unblock the server if it's locked in .accept()
//		if( fSnServerThread != null )
//		{
//			if(debug) System.err.println( kFName
//				+ "About to call .interrupt() for SnServer " + fSnServerThread
//				);
//			fSnServerThread.interrupt();
//			if(debug) System.err.println( kFName
//				+ "Back from call to .interrupt() for SnServer"
//				);
//			if(debug) System.err.println( kFName
//				+ "Also calling .interrupt() for current thread " + Thread.currentThread()
//				);
//			Thread.currentThread().interrupt();
//			if(debug) System.err.println( kFName
//				+ "Back from calling .interrupt() for current thread"
//				);
//		}
//		else
//		{
//			if(debug) System.err.println( kFName
//				+ "Not calling .interrupt() for SnServer"
//				+ " because fSnServerThread is null."
//				+ " This may be normal if the application is just starting up."
//				);
//		}
	}
	// Normally you'd think we would ALWAYS want to start the server
	// but it is possible that we just want to check the config
	private boolean getShouldStartServer()
	{
		return ! getShouldCheckConfig() && ! getShouldShowFullVersionInfo();
	}
	private boolean getShouldCheckConfig()
	{
		return fJustCheckConfig;
	}


	private boolean getShouldShowFullVersionInfo()
	{
		return fJustShowVersion;
	}




	public String getConfigFileURI()
	{
		final String kFName = "getConfigFileURI";
		if( null != getSearchTuningConfig() )
			return getSearchTuningConfig().getConfigFileURI();
		else
			return fConfigFileURI;
//		errorMsg( kFName,
//			"No search tuning config, returning null."
//			);
//		return null;
	}

	//////////////////////////////////////////
	//
	// Given a line from the configuration file that started with "listen".
	// get the port number and save it.
	//
	//////////////////////////////////////////

//	private void setPort(String inSearchLine )
//	{
//		inSearchLine = inSearchLine.substring( 7 ).trim();
//		fPortNumber = Integer.parseInt( inSearchLine );
//	}

	// Now mostly pass through wrappers to the currently configured
	// search names server

	public SearchTuningConfig getSearchTuningConfig()
	{
		return fSnConfig;
	}

	// Methods that spawned processes can use to get back
	// to us and ask for global type data
	public int getPort()
	{
		return getSearchTuningConfig().getPort();
	}

	public String getLastChanceSearchEngineUrlOrNull() {
		return fLastDitchSearchEngineURL;
	}
	public int getLastChancePort() {
		return fLastDitchPort;
	}


	/***
	public Hashtable __getHashMap()
	{
		return getSearchTuningConfig().getHashMap();
	}
	***/
	
	/***
	public Hashtable _getUserClassesHashMap()
	{
		return getSearchTuningConfig().getUserClassesHashMap();
	}
	public Document _getConfigDoc()
	{
		return getSearchTuningConfig().getConfigDoc();
	}
	public SearchEngineConfig _getSearchEngine()
	{
		return getSearchTuningConfig().getSearchEngine();
	}
	public SearchLogger _getSearchLogger()
	{
		return getSearchTuningConfig().getSearchLogger();
	}
	public nie.sr2.SearchReportingConfig _getReportingConfig()
	{
		if( null != getSearchTuningConfig().getReportDispatcher() )
			return getSearchTuningConfig().getReportDispatcher().getReportingConfig();
		else
			return null;
	}

	public DBConfig _getDBConfig()
	{
		/// if( null!=getSearchLogger() )
		///	return getSearchLogger().getDBConfig();
		/// else
		///	return null;

		return getSearchTuningConfig().getDBConfig();
	}
	public boolean _hasSearchLogger()
	{
		return getSearchTuningConfig().hasSearchLogger();
	}

	public String _getSearchNamesURL()
	{
		return getSearchTuningConfig().getSearchNamesURL();
	}

	public String _getAdminPwd()
	{
		return getSearchTuningConfig().getAdminPwd();
	}
	***/

	//public List

	///////////////////////////////////////////
	//
	// Given a line from the configuration file that started with
	// the searchterm keyword, parse out the search terms, the
	// destination URL, and whether it's a suggest or redirect,
	// then insert the information into the hash map.
	//
	////////////////////////////////////////////

	private void addSearchTermOBS( String inSearchLine )
	{
//		///////
//		// Get the search term
//		///////
//
//		int lTermStart = inSearchLine.indexOf( "<<" ) + 2;
//		inSearchLine = inSearchLine.substring( lTermStart );
//		int lTermEnd = inSearchLine.indexOf( ">>" );
//		String lTerm = inSearchLine.substring( 0, lTermEnd );
//
//		///////
//		// Is it a suggest or a redirect?
//		///////
//
//		inSearchLine = inSearchLine.substring( lTermEnd + 2 );
//		inSearchLine = inSearchLine.trim();
//		int lFunction = 0;
//
//		if( inSearchLine.toLowerCase().startsWith( "redirect" ) )
//			lFunction = SnRedirectRecord.kRedirect;
//		else if( inSearchLine.toLowerCase().startsWith( "suggests" ) )
//			lFunction = SnRedirectRecord.kSuggests;
//
//		///////
//		// Get the destination URL
//		///////
//
//		lTermStart = inSearchLine.indexOf( "<<" ) + 2;
//		inSearchLine = inSearchLine.substring( lTermStart );
//		lTermEnd = inSearchLine.indexOf( ">>" );
//		String lURL = inSearchLine.substring( 0, lTermEnd );
//
//		///////
//		// Save it in the list
//		///////
//
//		SnRedirectRecord lsnURL = new SnRedirectRecord( lFunction, lURL, lTerm );
//		fTermHashMap.put( lTerm, lsnURL );
	}

	// Todo: Implement this
	public void printCompleteInfo()
	{
		final String kFName = "printCompleteInfo";

		getRunLogObject().errorMsg( kClassName, kFName,
			"Not yet implemented.  (Todo)"
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


	private static RunLogInterface getRunLogObject()
	// can't access some of impl's extensions with interface reference
	//private static RunLogBasicImpl getRunLogObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogObject();
	}
	// Return the same thing casted to allow access to impl extensions
	private static RunLogBasicImpl getRunLogImplObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogImplObject();
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
	private boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName, inFromRoutine,
			e, optMessage
			);
	}
	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	// When we were created
	private String fCreationTimestamp;

	public static volatile Object globalMapUpdateLock = new Object();

	// The main config data for this process
	// But it can be swapped out for a newer one
	private SearchTuningConfig fSnConfig;
	private String fLastDitchSearchEngineURL;
	private int fLastDitchPort;

	// The main http server for this application
	private SnHTTPServer fPrimarySnServer;

	// The thread for the server process
	// Todo: should this also be declared volatile
	private Thread fSnServerThread;
	private Thread mCronLiteThread;

	// flag for whether we should shut down
	// private boolean fShouldStopNow;
	private volatile boolean fShouldStopNow;


//	// The hash map maps requested search terms to urls
//	// and whether that's supposed to be a suggestion or
//	// a redirect.
//	// Each term links to a LIST of SnRedirectRecords
//	private Hashtable fTermHashMap;

	// What port to listen on
	// private int fPortNumber;
	// When we first started up, where did we listen?
	private int fOriginalPortNumber;

	// Where to read paramters from
	private String fConfigFileURI;

	// Should we be verbose or not
	public int fVerbosity;

//	// Where we store the config data
//	JDOMHelper fConfigTree;
//	// This is the overall tree we were inside of
//	// We may this for storing the data back out to disk
//	JDOMHelper fOverallMasterTree;

	// The configuration information for the Host Search Engine
	private SearchEngineConfig fSearchEngineConfig;

	// flag for whether we should just check the config
	private boolean fJustCheckConfig;

	// Flag to just show version data
	private boolean fJustShowVersion;
	// And a flag for whether we need to check the run log object
	private boolean fCheckRunlog;


	// Other actions we don't currently implement but are planning
	private boolean fCheckURLs = false;
	private boolean fCheckImages = false;
	private boolean fAutoFetchMetaFields = false;
	private boolean fAutoRefetchAllMetaFields = false;

	// Hashes to track classes we know about
	private static Hashtable fClassListDescription;
	private static Hashtable fClassListVerbosity;

//	// XML Paths we use
//	// When we read in the XML config file, what is the path to
//	// the main node we should look at
//	private static final String MAIN_SN_CONFIG_PATH = "/nie_config/search_names";
//	// Some attributes of the main search names tag
//	private static final String SN_PORT_ATTR = "port";
//	private static final String ADMIN_PWD_ATTR = "admin_password";
//
//	// Where to look under the main search names tree
//	// SEI = Search Engine Info
//	// Note that we do not add a /
//	private static final String SEI_CONFIG_PATH = "search_engine_info";
//	// Other stuff moved to SearchEngineConfig class
//
//
//	// Where to find map statements
//	// Will usually have more than one "/map"
//	// Typically relative to main Search names subtree
//	private static final String FIXED_MAP_PATH =
//		"fixed_redirection_map/map";
//	// How we refer to specific fields
//	// Relative to parent map statement
//	private static final String SEARCH_TERM_PATH = "term";
//	// we reference these from SnRedirectRecord
//	public static final String URL_PATH = "url";
//	public static final String ALTTERM_PATH = "alternate_term";
//	// Fields under the URL field, see SnRedirectRecord
//
//	// Where we specificy OUR URL
//	// Todo: Someday figure this out without being told
//	private static final String SEARCH_NAMES_URL_PATH = "search_names_url";

	// An object to use as a semephore when we want to reload the config
	private Object fRefreshLock = new Object();
	private volatile boolean fIsDoingRefresh;

//	// Verbosity levels
//	public static final int VERBOSITY_USE_DEFAULT = -1;
//	public static final int VERBOSITY_QUIET = 0;
//	public static final int VERBOSITY_MINIMUM = 1;
//	// private static final String VNAME_MIN = "Status";
//	// private static final int VERBOSITY_CHATTY = 2;
//	// private static final String VNAME_CHAT = "Info";
//	public static final int VERBOSITY_DEBUG = 3;
//	// private static final String VNAME_DEBUG = "Debug";
//	public static final int VERBOSITY_TRACE = 4;
//	// private static final String VNAME_DEBUG = "Trace";
//	public static final int DEFAULT_VERBOSITY = VERBOSITY_MINIMUM;

//	private static final int DEFAULT_PORT = 8080;
	// NO!  We do not act as a true proxy, so we should not use 8080
	// and besides everyone (at NIE) is used to 9000
	// private static final int DEFAULT_PORT = 9000;
	// ^^^ Moved to SearchTuningConfig and set to public

//	private static final String DEFAULT_CONFIG_FILE_URI =
//		"searchnames_config.xml";

	// We have TWO different options that start with the word "check",
	// but the first one is far more common, so it gets -c and -check
	private static final String JUST_CHECK_CONFIG_CMD_LINE_OPT = "check_config";
	private static final String JUST_CHECK_CONFIG_CMD_LINE_OPT2 = "check";
	private static final String CHECK_RUNLOG_CONFIG = "check_runlog_path";

	private static final String DISPLAY_VERSION_INFO = "version";


	private void __sep__STATIC_Init__ () {}
	////////////////////////////////////////////////////////////

	static
	{

		// Keep a map of classes we might like to use for
		// propogating verbosity info


//		fClassListDescription = new Hashtable();
//
//		fClassListDescription.put( "SearchTuningApp",
//          "Main application, reads command line, high level logic and setup"
//		    );
//		fClassListDescription.put( "SearchTuningConfig", EQUALS_TAG );
//		fClassListDescription.put( "NIEUtil", EQUALS_TAG );
//		fClassListDescription.put( "JDOMHelper", EQUALS_TAG );
//		fClassListDescription.put( "AuxIOInfo", EQUALS_TAG );
//		fClassListDescription.put( "SnHTTPServer", EQUALS_TAG );
//		fClassListDescription.put( "SnRequestHandler", EQUALS_TAG );
//		fClassListDescription.put( "SnRedirectRecord", EQUALS_TAG );
//		fClassListDescription.put( "SearchEngineConfig", EQUALS_TAG );

	}

};

