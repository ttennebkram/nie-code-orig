package nie.sr2.util;

/*
	DNS Updater for the SearchTrack system.
	Written by Kevin-Neil Klop
	Copyright 2002 by  New Idea Engineering, Inc., All Rights Reserved.
	
	If you're reading this without NIE's permission, then please put this
	back where you found it - you're not being nice.
	
*/

import java.sql.*;
import java.util.*;
import java.net.*;
import nie.core.*;

public class DNSLookup implements Runnable
{

	private final static String kClassName = "DNSLookup";

    ////////////////////////////////////////////////
    //
    // In case we're run as a stand-alone program
    // instead of being incorporated in another program
    //
    /////////////////////////////////////////////////
	
    static public void main( String[] inArgs )
	{
		final String kFName = "main";

		DNSLookup lLookup = new DNSLookup();
		// lLookup.parseArgs( inArgs );
		lLookup.parseCommandLine( inArgs );
		try
		{
			lLookup.commonInit();
		}
		catch( DNSException e )
		{
			fatalErrorMsg( kFName,
				"Error initializing, exiting."
				+ " Error: " + e
				);
			System.exit( 2 );
		}
		lLookup.run();
    }

    
    ////////////////////////////////////////////////
    //
    // Constructors.
    //
    // All constructors should call commonInit()
    //
    ////////////////////////////////////////////////
    
    public DNSLookup()
    //	throws DNSException
    {

		// commonInit();
		// ^^^ moved to AFTER we parsed the command line, so
		// we have a config file to use

    }
    
    public void commonInit()
    	throws DNSException
    {
		final String kFName = "commonInit";
		final String kExTag = kClassName + '.' + kFName + ": ";


		// fOperatingMode = kMinimumMode;
		// fDriverName = kDefaultDBURL;

		// Configure the database and cache a statement
		try
		{
			fDBConf = new DBConfig( fConfigFileURI );
			// cStatement = getDBConfig().createStatement();
			Object [] objs = getDBConfig().createStatement();
			cStatement = (Statement) objs[0];
			cConnection = (Connection) objs[1];

		}
		catch( Exception e )
		{
			throw new DNSException( kExTag
				+ " Error caching statement: " + e
				);
		}
	

    }
    
    ////////////////////////////////////////////////
    //
    // Parse an incomming array of strings that
    // are considered command line switches.
    // The following command line switches are
    // handled:
    //
    //		-m
    //		--minimum (default)
    //			Process only IP numbers that have not
    //			been seen before on any run.
    //
    //		-i
    //		--intermediate
    //			process only IP numbers that have not been
    //			seen before OR that have not been successfully
    //			resolved before.
    //
    //		-r
    //		--refresh
    //			process all IP numbers, regardless of whether
    //			they've been seen before or not.  Note that this
    //			will NOT change a resolved host to an unresolved
    //			one.
    //
    //		-f
    //		--full
    //			process all IP Numbers, regardless of whether
    //			they've been seen before or not.  Note that this
    //			WILL change a resolved host to an unresolved one.
    //
    //		These flags are mutually exclusive - the one encountered
    //		LAST on the command  line will be the one used.
    //
    //		The following switches may be used in conjunction with any
    //		of the above switches.
    //
    //		-d
    //		--driver
    //			Specify the JDBC driver to use.  Please refer to the
    //			JDBC API descriptions for information on this URL.
    //			There's a whole chapter on it there that I don't want
    //			to include as a comment.
    //
    //		-a
    //		--account
    //			Specify the account name to be used in connecting with
    //			the database.
    //
    //		-p
    //		--password
    //			Specify the password to be used in connecting  with the
    //			database.
    //
    //		-g
    //		--debug
    //			Specify debug mode
    //
    ////////////////////////////////////////////////////

	void parseCommandLine( String inArgs[] )
	{

		final String kFName = "parseCommandLine";

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

				    if( lFlag.equals( "minimum" ) )
						fOperatingMode = kMinimumMode;
				    else if( lFlag.equals( "intermediate") )
						fOperatingMode = kIntermediateMode;
				    else if( lFlag.equals( "refresh" ) ) 
						fOperatingMode = kRefreshMode;
				    else if( lFlag.equals( "full" ) ) 
						fOperatingMode = kFullMode;
					else
						// We don't know what it is
						bailOnBadSyntax( inArgs[i] );
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
				// Else we've already seen the config!
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

		msg += "Required Parameter:" + NIEUtil.NL
			+ "\tdatabase_config_file_name_or_url.xml" + NIEUtil.NL
			+ NIEUtil.NL
			;

	msg += "Primary Options (choose only one):" + NIEUtil.NL
	+ NIEUtil.NL
	+ "-minimum (default)" + NIEUtil.NL
	+ "	Process only IP numbers that have not" + NIEUtil.NL
	+ "	been seen before on any run." + NIEUtil.NL
	+ NIEUtil.NL
	+ "-intermediate" + NIEUtil.NL
	+ "	process only IP numbers that have not been" + NIEUtil.NL
	+ "	seen before OR that have not been successfully" + NIEUtil.NL
	+ "	resolved before." + NIEUtil.NL
	+ NIEUtil.NL
	+ "-refresh" + NIEUtil.NL
	+ "	process all IP numbers, regardless of whether" + NIEUtil.NL
	+ "	they've been seen before or not.  Note that this" + NIEUtil.NL
	+ "	will NOT change a resolved host to an unresolved" + NIEUtil.NL
	+ "	one." + NIEUtil.NL
	+ NIEUtil.NL
	+ "-full" + NIEUtil.NL
	+ "	process all IP Numbers, regardless of whether" + NIEUtil.NL
	+ "	they've been seen before or not.  Note that this" + NIEUtil.NL
	+ "	WILL change a resolved host to an unresolved one." + NIEUtil.NL
	+ NIEUtil.NL
	+ "These flags are mutually exclusive - the one encountered" + NIEUtil.NL
	+ "LAST on the command  line will be the one used." + NIEUtil.NL
	+ NIEUtil.NL
	;


		msg = msg + RunLogBasicImpl.getVerbosityLevelDescriptions(
			true, true, true, true
			);

		getRunLogObject().fatalErrorMsg( kClassName, kFName,
			msg
			);


		System.exit( 1 );
	}





    public void _OBS_parseArgs( String[] inArgs )
    {
    	final String kFName = "parseArgs";

		// Put inside try block to catch index out of bounds
		// errors and other issues
    	try
    	{

			for( int i = 0; i < inArgs.length; i++ )
			{
			    if( (inArgs[i].compareTo( "-m" ) == 0) ||
					(inArgs[i].compareTo( "--minimum") == 0)
					)
				{
					fOperatingMode = kMinimumMode;
				}
			    else if( (inArgs[i].compareTo( "-i" ) == 0)
				     || (inArgs[i].compareTo( "--intermediate") == 0)
				     )
				{
					fOperatingMode = kIntermediateMode;
				}
			    else if( (inArgs[i].compareTo( "-r" ) == 0)
				     || (inArgs[i].compareTo( "--refresh" ) == 0) 
				     )
				{
					fOperatingMode = kRefreshMode;
				}
			    else if( (inArgs[i].compareTo( "-f" ) == 0)
				     || (inArgs[i].compareTo( "--full" ) == 0) 
				     )
				{
					fOperatingMode = kFullMode;
				}	    
//			    else if( (inArgs[i].compareTo( "-d" ) == 0)
//				     || (inArgs[i].compareTo( "--driver" ) == 0)
//				     )
//				{
//					fDriverName = inArgs[++i];
//				}
//			    else if( (inArgs[i].compareTo( "-p" ) == 0)
//				     || (inArgs[i].compareTo( "--password" ) == 0)
//				     )
//				{
//					fPassword = inArgs[++i];
//				}
//			    else if( (inArgs[i].compareTo( "-a" ) == 0)
//				     || (inArgs[i].compareTo( "--account" ) == 0)
//				     )
//				{
//					fAccount = inArgs[++i];
//				}
			    else if( (inArgs[i].compareTo( "-g" ) == 0)
				     || (inArgs[i].compareTo("--debug" ) == 0)
				     )
			    {
					statusMsg( kFName,
						"Enabling debug messages"
						);
					gDebug = true;
			    }
			    else
				{
					fOperatingMode = kErrorMode;
				}
			}	// End for each command line argument
    	}
    	catch( Exception e )
    	{
  			fOperatingMode = kErrorMode;
  		}
    }
    
    ////////////////////////////////////////////////////
    //
    // And here's where we actually run.  What this
    // does is set up the SQL connections, then build three
    // lists of IP numbers:
    //
    //	List 1: All IP Numbers that are in the "source"
    //			table.
    //	List 2: All IP Numbers that shouldnot be resolved.
    //	List 3:	All IP Numbers that should not be modified IF
    //			they could not be resolved on this run.
    //
    ////////////////////////////////////////////////////
    
    public void run()
    {
    	final String kFName = "run";

		// TreeSet lCandidateIPNumbersTreeSet = null;
		// TreeSet lShouldNotResolveTreeSet = null;
		// TreeSet lDoNotChangeToUnknownTreeSet = null;
		Hashtable lCandidateIPNumbersHash = null;
		Hashtable lShouldNotResolveHash = null;
		Hashtable lDoNotChangeToUnknownHash = null;

		ResultSet lCandidateIPNumbersResultSet = null;
		try
		{
		    ///////
		    //
		    // Connect to the database
		    //
		    ///////
				
//		    statusMsg( kFName,
//		    	"Connecting using URL: " + fDriverName
//		    	);
//		    Connection lConnection =
//			DriverManager.getConnection( fDriverName,
//						     fAccount,
//						     fPassword );
//		    Statement lStatement = lConnection.createStatement();
//			^^^ all replaced, and moved to constructor


		    ///////
		    //
		    // Get the list of candidate IP numbers
		    //
		    ///////
		    
			debugMsg( kFName,
				"Executing SQL: " + kGetCandidateIPNumbers
				);

		    lCandidateIPNumbersResultSet =
				cStatement.executeQuery( kGetCandidateIPNumbers );

			debugMsg( kFName,
				"Result Set returned."
				);

		    // lCandidateIPNumbersTreeSet =
			//	buildIPTreeSet( lCandidateIPNumbersResultSet );
		    lCandidateIPNumbersHash =
				buildIPTreeSet( lCandidateIPNumbersResultSet );

		    lCandidateIPNumbersResultSet = null;
	    
		    ///////
		    //
		    // For the various operating  modes, get the lists of:
		    //		IP Numbers that should not be resolved
		    //		IP Numbers that should not be set to unknown
		    //
		    ///////
		    
		    //
		    // Strings to hold the SQL statement to get the above lists
		    //
		    
		    String lShouldNotResolveSQLText = null;
		    String lDoNotChangeToUnknownSQLText = null;
	    
		    //
		    // Set the strings to the right values for different
		    // operating modes.
		    //
		    
		    switch( fOperatingMode )
		    {

			case kMinimumMode:
				debugMsg( kFName,
					"Running in minimum mode"
					+ " (processing only IP numbers in log that do not"
					+ " apepar in domainnames)"
					);

			    lShouldNotResolveSQLText =
					kBasicModeShouldNotResolveSQLText;

			    lDoNotChangeToUnknownSQLText =
					kBasicDoNotChangeToUnknownSQLText;

			    break;

		    
	        case kIntermediateMode:
				debugMsg( kFName,
					"Running in intermediate mode"
					+ " (IP numbers that we've not seen before OR that"
					+ " have not been previously successfully resolved)"
					);
	
			    lShouldNotResolveSQLText =
					kIntermediateModeShouldNotResolveSQLText;

			    lDoNotChangeToUnknownSQLText =
					kIntermediateDoNotChangeToUnknownSQLText;

			    break;

			
			case kRefreshMode:
				debugMsg( kFName,
					"Running in refresh mode"
					+ " (process all IP numbers in log."
					+ " This will NOT change an already identified IP"
					+ " number to unidentified but will update it if"
					+ " we can resolve it during this run)"
					);
	
			    lShouldNotResolveSQLText =
					kRefreshModeShouldNotResolveSQLText;

			    lDoNotChangeToUnknownSQLText =
					kRefreshDoNotChangeToUnknownSQLText;

			    break;

				
	        case kFullMode:
				debugMsg( kFName,
					"Running in full mode (process all IP numbers"
					+ " in log.  This WILL change an already"
					+ " identified IP number to unidentified if we"
					+ " can't update it during this run)"
					);
	
			    lShouldNotResolveSQLText =
					kFullModeShouldNotResolveSQLText;

			    lDoNotChangeToUnknownSQLText =
					kFullDoNotChangeToUnknownSQLText;

			    break;

				
	        default:
	
			    fatalErrorMsg( kFName,
			    	"Invalid operating mode \"" + fOperatingMode + "\""
			    	);
			    System.exit(-1);

			    break;


	    	}	// End switch on fOperatingMode

			
		    //
		    // Set up the list of IP numbers that should be ignored.
		    // This MAY be an empty tree set.
		    
			debugMsg( kFName,
				"Executing Query: "
				+ lShouldNotResolveSQLText
				);
		    
		    if( null != lShouldNotResolveSQLText )
				lCandidateIPNumbersResultSet =
			    	cStatement.executeQuery( lShouldNotResolveSQLText );
	
		    // lShouldNotResolveTreeSet =
			//	buildIPTreeSet( lCandidateIPNumbersResultSet );
		    lShouldNotResolveHash =
				buildIPTreeSet( lCandidateIPNumbersResultSet );
		    
		    // Set up the list of IP numbers whose server should not be
		    // changed to unknown if they already exist.
		    
			debugMsg( kFName,
				"Executing Query: "
				+ lDoNotChangeToUnknownSQLText
				);
	
		    if( lDoNotChangeToUnknownSQLText != null )
				lCandidateIPNumbersResultSet =
			    	cStatement.executeQuery( lDoNotChangeToUnknownSQLText );
	
		    // lDoNotChangeToUnknownTreeSet =
			//	buildIPTreeSet( lCandidateIPNumbersResultSet );
		    lDoNotChangeToUnknownHash =
				buildIPTreeSet( lCandidateIPNumbersResultSet );
		    
		    // Now that the lists are set up, do the actual work.
		    // We'll use a method for this as it's possible that
		    // someone else might want to call this externally after
		    // setting up their own lists.
		    
		    // doProcessIPNumbers( lConnection,
			//		lCandidateIPNumbersTreeSet,
			//		lShouldNotResolveTreeSet,
			//		lDoNotChangeToUnknownTreeSet 
			//		);
		    doProcessIPNumbers(
					lCandidateIPNumbersHash,
					lShouldNotResolveHash,
					lDoNotChangeToUnknownHash 
					);
	    
		}
		catch( SQLException se )
		{
		    errorMsg( kFName,
		    	"SQL Exception caught: " + se
		    	);
		    se.printStackTrace();
		}
		catch( Exception e )
		{
		    fatalErrorMsg( kFName,
		    	"General exception caught: " + e
		    	);
		    e.printStackTrace();
		    System.exit(-1);
		}	
		finally {
			// quickie cleanup!
			lCandidateIPNumbersResultSet = DBConfig.closeResults( lCandidateIPNumbersResultSet, kClassName, kFName, false );
			cStatement = DBConfig.closeStatement( cStatement, kClassName, kFName, false );
			cConnection = DBConfig.closeConnection( cConnection, kClassName, kFName, false );
		}
	}

    ///////////////////////////////////////////////////////////
    //
    // Process the three lists, updating the domainnames table.
    //
    ///////////////////////////////////////////////////////////
   
    // void doProcessIPNumbers( Connection inDBConnection,
	//		     TreeSet inCandidates,
	//		     TreeSet inDoNotResolve,
	//		     TreeSet inDoNotUpdateToUnknown
	//	)
    void doProcessIPNumbers(
			     Hashtable inCandidates,
			     Hashtable inDoNotResolve,
			     Hashtable inDoNotUpdateToUnknown
		)
    {
    	final String kFName = "doProcessIPNumbers";
		final int kReportInterval = 1000;

		// Note:
		// To turn a String into an IP address OBJECT, we start
		// with the getByName, even though in this case we are starting
		// with an IP address.  Then from that object, we can call
		// getName()
		
		if( null == inCandidates || null == inDoNotResolve
			|| null == inDoNotUpdateToUnknown
			)
		{
			errorMsg( kFName,
				"Null input(s), exiting method."
				+ " inCandidates=" + inCandidates
				+ " inDoNotResolve=" + inDoNotResolve
				+ " inDoNotUpdateToUnknown=" + inDoNotUpdateToUnknown
				);
			return;
		}

		// Iterator lCandidateIPNumbersIterator = inCandidates.iterator();
		Iterator lCandidateIPNumbersIterator =
			inCandidates.keySet().iterator();
		Object lCandidateIPObject = null;
		// try
		// {
		    // Statement lStatement = inDBConnection.createStatement();
		    // For each candidate IP number

			int lExpectedTotal = inCandidates.size();
			int lCurrScannedCount = 0;
			int lSkippedCount = 0;
			int lDNSCheckedCount = 0;
			int lResolvedCount = 0;
			int lDidNotResolveCount = 0;
			statusMsg( kFName, "Will check " + lExpectedTotal + " records" );

		    while( lCandidateIPNumbersIterator.hasNext() )
		    {

				// lCandidateIPObject = lCandidateIPNumbersIterator.next();
				// DotNotation lCandidateIPNumber =
				//	(DotNotation)lCandidateIPObject;

				String lCandidateIPNumber =
					(String)lCandidateIPNumbersIterator.next();

				lCurrScannedCount++;
				if( lCurrScannedCount % kReportInterval == 0 )
					statusMsg( kFName,
						"Checking record " + lCurrScannedCount
						+ " Previously:"
						+ " skipped=" + lSkippedCount
						+ ", DNS-checked=" + lDNSCheckedCount
						+ ", resolved=" + lResolvedCount
						+ ", not resolved=" + lDidNotResolveCount
						+ ". (stats do NOT include the current record)"
						);

				// Skip if we were told not to resolve it
				if( inDoNotResolve.contains( lCandidateIPNumber ) )
				{
					lSkippedCount++;
					continue;
				}
				// Or count it as checked
				// Errors will show up in unresolved total
				// and error/warning messages
				lDNSCheckedCount++;

				// If NOT in do not resolve list
				// if( !inDoNotResolve.contains( lCandidateIPNumber ) )
				// {
				    //	
				    // Resolve the IP number in lCandidateIPNumber
				    //	
							
				    // InetAddress lAddress =
					//	lCandidateIPNumber.asInetAddress();

				    InetAddress lAddress = null;
				    try
				    {
				    	lAddress = InetAddress.getByName(
				    		lCandidateIPNumber
				    		);
					}
					catch( UnknownHostException uhe )
					{
						errorMsg( kFName,
							"Unable to create IP address object from string"
							+ " \"" + lCandidateIPNumber + "\", setting as unknown."
							+ " Will continue looking at any subsequent addresses."
							+ " Error was: " + uhe
							);
					    doSetUnknown(
					    	// lStatement,
					    	lCandidateIPNumber,
					    	inDoNotUpdateToUnknown
					    	);
					    lDidNotResolveCount++;
						continue;
					}
					if( null == lAddress )
					{
						errorMsg( kFName,
							"Got back NULL IP address object from string"
							+ " \"" + lCandidateIPNumber + "\", setting as unknown."
							+ " Will continue looking at any subsequent addresses."
							);
					    doSetUnknown(
					    	// lStatement,
					    	lCandidateIPNumber,
					    	inDoNotUpdateToUnknown
					    	);
					    lDidNotResolveCount++;
						continue;						
					}


				    // if( lAddress != null )
				    // {
						String lHostName = lAddress.getHostName();
						// if( lHostName.compareTo(
						//			lCandidateIPNumber.asString()
						//			) == 0
						//	)
						// {
						// If it doesn't resolve, I guess it comes back
						// as the same string
						if( lHostName.equalsIgnoreCase( lCandidateIPNumber ) )
						{
							infoMsg( kFName,
								"Got back same string from getHostName for string"
								+ " \"" + lCandidateIPNumber + "\", setting as unknown."
								+ " Will continue looking at any subsequent addresses."
								);
					    	doSetUnknown(
					    		// lStatement,
					    		lCandidateIPNumber,
					    		inDoNotUpdateToUnknown
					    		);
					    	lDidNotResolveCount++;
						}
						// else if( lHostName.compareTo(
						//			lCandidateIPNumber.asString()
						//			) != 0
						//	)
						else
						{
					    	// doSetHost( lStatement, lAddress );
					    	doSetWasResolved(
					    		// lStatement,
					    		lCandidateIPNumber,
					    		lHostName
					    		);
					    	lResolvedCount++;
						}
				    // }
				    // else
					// {
					//	doSetUnknown( lStatement,
					//		lCandidateIPNumber, inDoNotUpdateToUnknown
					//		);
					//}
				// }	// End if NOT in do not resolve list
	    	}	// End for each candidate IP number
		// }
		// catch (SQLException se)
		// {
		//    fatalErrorMsg( kFName,
		//    	"Caught SQL Exception: " + se
		//    	);
		//    se.printStackTrace();
		//    System.exit( -1 );
		// }
		// return;

		statusMsg( kFName,
			"Done."
			+ " Final statistics:"
    		+ " # candidates=" + lCurrScannedCount
			+ ", skipped=" + lSkippedCount
			+ ", DNS-checked=" + lDNSCheckedCount
			+ ", resolved=" + lResolvedCount
			+ ", not resolved=" + lDidNotResolveCount
			);

		if( lExpectedTotal != lCurrScannedCount )
			errorMsg( kFName,
				"# scanned records not equal number expected:"
				+ " expected=" + lExpectedTotal
				+ ", actual candidates scanned=" + lCurrScannedCount
				);

		if( lSkippedCount + lDNSCheckedCount != lCurrScannedCount )
			errorMsg( kFName,
				"# records checked+skipped != # scanned candidate records:"
				+ " scanned candidate=" + lCurrScannedCount
				+ ", DNS-checked=" + lDNSCheckedCount
				+ "+ skipped=" + lSkippedCount
				);

		if( lResolvedCount + lDidNotResolveCount != lDNSCheckedCount )
			errorMsg( kFName,
				"# records resolved + not resolved != # DNS-checked records:"
				+ " DNS checks=" + lDNSCheckedCount
				+ ", resolved=" + lResolvedCount
				+ "+ not resolved=" + lDidNotResolveCount
				);

    }
    
    ///////////////////////////////////////////////////////////
    //
    // Set a given IP number to "unknown"
    //
    ///////////////////////////////////////////////////////////

    // public void doSetUnknown( Statement inStatement,  DotNotation inIPNumber, TreeSet inDoNotUpdateToUnknown )
    // public void doSetUnknown( Statement inStatement,  String inIPNumber, TreeSet inDoNotUpdateToUnknown )
    public void doSetUnknown(
    	// Statement inStatement,
    	String inIPNumber,
    	Hashtable inDoNotUpdateToUnknown
    	)
    {
		final String kFName = "doSetUnknown";

    	// String lIPNumber = inIPNumber.asString();
    	String lIPNumber = inIPNumber;
    	
    	if( gDebug )
	    	debugMsg( kFName,
	    		"Unknown IP: " + lIPNumber
	    		);

    	// String lNewHostName = "Unknown " + inIPNumber.asString();
    	String lNewHostName = "Unknown " + lIPNumber;
    	String lSQLInsertText =
    		replaceAll( kInsertDomainNameSQL, kHOSTNAME, lNewHostName );
    	lSQLInsertText =
    		replaceAll( lSQLInsertText, kIPNUMBER, lIPNumber );
    	lSQLInsertText =
    		replaceAll( lSQLInsertText, kRESOLVED, kWAS_NOT_RESOLVED );
    	
    	if( gDebug )
	    {
			debugMsg( kFName,
				"Trying to insert using SQL statement: \""
				+ lSQLInsertText + "\""
				);
	    }
	
    	try
		{
	    	// inStatement.execute( lSQLInsertText );
	    	cStatement.execute( lSQLInsertText );
		}
		catch( SQLException se )
		{
		    // Perhaps the record is already in there and we need to update it.
		    // Check against the DoNotUpdateToUnknown list before doing that!!
		    
		    if( ! inDoNotUpdateToUnknown.contains( inIPNumber ) )
		    {
				String lSQLUpdateText = replaceAll( kUpdateDomainNameSQL, kHOSTNAME, lNewHostName );
				lSQLUpdateText = replaceAll( lSQLUpdateText, kIPNUMBER, lIPNumber );
				lSQLUpdateText = replaceAll( lSQLUpdateText, kRESOLVED, kWAS_NOT_RESOLVED );
		
				if( gDebug )
				{
				   	debugMsg( kFName,
				   		"Trying to update using SQL statement: \""
				   		+ lSQLUpdateText + "\""
				   		);
				}
		
				try
				{
				    // inStatement.execute( lSQLUpdateText );
				    cStatement.execute( lSQLUpdateText );
				}
				catch( SQLException se1 )
				{
					String seStr = NIEUtil.exceptionToString( se );
					String se1Str = NIEUtil.exceptionToString( se1 );

					String msg =
				    	"Could not update database"
				    	+ " - got two SQL exceptions in a row."
				    	+ " First SQL Statement was: \""
				    	+ lSQLInsertText + "\""
				    	+ " First exception was: \"" + seStr + "\""
				    	+ " Second SQL Statement was: \""
				    	+ lSQLUpdateText + "\""
				    	+ " Second exception was: " + se1Str
				    	;
					fatalErrorMsg( kFName, msg );
				    System.exit( -1 );
				}
	    	}
		}
    }
    
    ///////////////////////////////////////////////////////////
    //
    // Set a given IP number to resolved... We did it!
    //
    ///////////////////////////////////////////////////////////
    
    // public void doSetHost( Statement inStatement, InetAddress inIPNumber )
    public void doSetWasResolved(
    	String inIPNumber, String inHostName
    	)
    {
		final String kFName = "doSetHost";

    	// String lHostName = inIPNumber.getHostName();
    	// String lIPNumber = inIPNumber.getHostAddress();
    	String lHostName = inHostName;
    	String lIPNumber = inIPNumber;
  	
    	String lSQLInsertText = replaceAll( kInsertDomainNameSQL, kHOSTNAME, lHostName );
    	lSQLInsertText = replaceAll( lSQLInsertText, kIPNUMBER, lIPNumber );
    	lSQLInsertText = replaceAll( lSQLInsertText, kRESOLVED, kWAS_RESOLVED );
    	
    	if( gDebug )
    	{
		    debugMsg( kFName,
		    	"Trying to insert using SQL statement: \""
		    	+ lSQLInsertText + "\""
		    	);
		}
		
    	try
    	{
	    	// inStatement.execute( lSQLInsertText );
	    	cStatement.execute( lSQLInsertText );
    	}
    	catch( SQLException se1 )
    	{
		    String lSQLUpdateText =
		    	replaceAll( kUpdateDomainNameSQL, kHOSTNAME, lHostName );
		    lSQLUpdateText =
		    	replaceAll( lSQLUpdateText, kIPNUMBER, lIPNumber );
		    lSQLUpdateText =
		    	replaceAll( lSQLUpdateText, kRESOLVED, kWAS_RESOLVED );
	    
		    if( gDebug )
		    {
				debugMsg( kFName,
					"Trying to update using SQL statement: \""
					+ lSQLUpdateText + "\""
					);
		    }

		    try
    	    {
				// inStatement.execute( lSQLUpdateText );
				cStatement.execute( lSQLUpdateText );
	    	}
	    	catch( SQLException se2 )
	    	{
				String se1Str = NIEUtil.exceptionToString( se1 );
				String se2Str = NIEUtil.exceptionToString( se2 );

				String msg =
					"Could not update database"
					+ " - got two SQL exceptions in a row."
					+ " First SQL Statement was: \""
					+ lSQLInsertText + "\""
					+ " Exception: \"" + se1Str + "\""
					+ " Second SQL Statement was: \""
					+ lSQLUpdateText + "\""
					+ " Exception: \"" + se2Str + "\""
					;
				fatalErrorMsg( kFName, msg );

				System.exit( -1 );
	    	}
		}
		
		if( gDebug )
		    debugMsg( kFName,
		    	"Resolved " + lIPNumber
		    	+ " to " + lHostName
		    	);
    }

    ///////////////////////////////////////////////////////////
    //
    // Early Java VMs (like the one that is distributed with 
    // Oracle 9i server) is 1.2.x, which is missing numerous
    // functions.  These are replacements.
    //
    ///////////////////////////////////////////////////////////

    static String replaceAll( String inSourceString,
    	String inSearchString, String inReplacementString
    	)
    {
		int lPosition = inSourceString.indexOf( inSearchString );
		if( lPosition >= 0 )
	    {
			String lPrefixString = inSourceString.substring( 0, lPosition );
			String lPostString = replaceAll( inSourceString.substring( lPosition + inSearchString.length() ), inSearchString, inReplacementString );
			return lPrefixString + inReplacementString + lPostString;
	    }
		else
		    return inSourceString;
    }


	public static String getLogTableName()
	{
		return DBConfig.LOG_TABLE;
	}
	public static String getDomainTableName()
	{
		return DBConfig.DOMAIN_TABLE;
	}





	private String fConfigFileURI;

    ///////////////////////////////////////////////////////////
    //
    // Build a tree set from a result set.  If the input result
    // set is null, then we return an initialized, but empty,
    // tree set.
    //
    ///////////////////////////////////////////////////////////
    
    // private TreeSet buildIPTreeSet( ResultSet inResultSet )
    private Hashtable buildIPTreeSet( ResultSet inResultSet )
    {
		final String kFName = "buildIPTreeSet";

		// if( gComparator == null )
		//	gComparator = new DotNotationComparator();
	
		// TreeSet lTreeSet = new TreeSet( gComparator );
		Hashtable outHash = new Hashtable();

		if( inResultSet != null )
		{
		    try
		    {
				while( inResultSet.next() )
				{
				    String lIPNumberString;
				    
				    lIPNumberString = inResultSet.getString( 1 );
				    if( gDebug )
				    {
						debugMsg( kFName,
							"Converting '" + lIPNumberString + "'"
							);
				    }
				    // DotNotation lDotNotation =
				    //	new DotNotation( lIPNumberString );
				    // lTreeSet.add( lDotNotation );
				    // lTreeSet.add( lIPNumberString );
				    outHash.put( lIPNumberString, lIPNumberString );
				}
		    }
		    catch( SQLException se )
		    {
				fatalErrorMsg( kFName,
					"SQL Exception caught: " + se
					);
				se.printStackTrace();
				System.exit( -1 );
		    }
		}
		// return lTreeSet;
		return outHash;
	}


	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	// This gets us essentially the same thing, but casted
	// to let us to implementation specific things like parse
	// command line options
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



	// private JDOMHelper fMainElem;


	DBConfig getDBConfig()
	{
		return fDBConf;
	}

    
    ///////////////////////////////////////////////////////////
    //
    // Private members...
    //
    ///////////////////////////////////////////////////////////

	// The primary database configuration
	DBConfig fDBConf;
  
    int fOperatingMode = kMinimumMode;

	Statement cStatement;
	Connection cConnection;

    // String fDriverName;
    // String fPassword = "kklop";
    // String fAccount = "kklop";
    // String fPassword;
    // String fAccount;
    
    // private static Comparator gComparator = null;
    
    static final int kErrorMode = 0;
    static final int kMinimumMode = 1;
    static final int kIntermediateMode = 2;
    static final int kRefreshMode = 3;
    static final int kFullMode = 4;
    
    // static final String kDefaultDBURL =
    //	"jdbc:oracle:thin:@bigmomma:1521:stack"
    //	;
    
    ////////////////////////////////////////////////////////////
    //
    // SQL Statements...
    //
    ////////////////////////////////////////////////////////////
    
    static final String kGetCandidateIPNumbers =
		"SELECT DISTINCT client_host"
		+ " FROM " + getLogTableName()
		+ " WHERE client_host IS NOT NULL"
		+ " ORDER BY client_host"
		;
    
    static final String kBasicModeShouldNotResolveSQLText =
		"SELECT DISTINCT client_host"
		+ " FROM " + getDomainTableName()
		+ " WHERE client_host IS NOT NULL"
		+ " ORDER BY client_host"
		;
    
    static final String kBasicDoNotChangeToUnknownSQLText = null;
    
    static final String kIntermediateModeShouldNotResolveSQLText =
		"SELECT DISTINCT client_host"
		+ " FROM " + getDomainTableName()
		+ " WHERE resolved = 1"
		+ "  AND client_host IS NOT NULL"
		+ " ORDER BY dn_client_host"
		;
    
    static final String kIntermediateDoNotChangeToUnknownSQLText = null;
    
    static final String kRefreshModeShouldNotResolveSQLText = null;
    
    static final String kRefreshDoNotChangeToUnknownSQLText =
		"SELECT DISTINCT client_host"
		+ " FROM " + getDomainTableName()
		+ " WHERE resolved = 1"
		+ "  AND client_host IS NOT NULL"
		+ " ORDER BY client_host"
		;
    
    static final String kFullModeShouldNotResolveSQLText = null;
    
    static final String kFullDoNotChangeToUnknownSQLText = null;
    
    static final String kHOSTNAME = "VAR_HOSTNAME";
    static final String kIPNUMBER = "VAR_IPNUMBER";
    static final String kRESOLVED = "VAR_RESOLVED";
    static final String kWAS_RESOLVED = "1";
    static final String kWAS_NOT_RESOLVED = "0";
    
    static final String kInsertDomainNameSQL =
    	"INSERT INTO " + getDomainTableName()
    	+ " VALUES ('" + kIPNUMBER + "'"
    	+ ",'" + kHOSTNAME + "'"
    	+ ", SYSDATE"
    	+ ", " + kRESOLVED + " ) "
    	;
    
    static final String kUpdateDomainNameSQL =
    	"UPDATE " + getDomainTableName()
    	+ " SET dns_name = '" + kHOSTNAME + "'"
    	+ ", lookup_date = SYSDATE"
    	+ ", resolved = " + kRESOLVED
    	+ " WHERE client_host = '" + kIPNUMBER + "'"
    	;
    	
    public static boolean gDebug = false;
}
