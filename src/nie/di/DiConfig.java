package nie.di;

import java.util.*;
import java.io.*;
import java.sql.*;
import org.jdom.Element;
import nie.core.*;

public class DiConfig
{
	private final static String kClassName = "DiConfig";

	/**********************************************************
	 * Constructor
	 *********************************************************/

	public DiConfig( String inConfigURI )
		throws DiConfigException
	{
		final String kFName = "consctructor";
		final String kExTag = kClassName + ':' + kFName + ": ";

		if( inConfigURI == null )
		{
			throw new DiConfigException( kExTag
				+ "Null input URI given; please supply a file name or URL."
				);
		}

		// Load and store the main XML configuration data
		try
		{
			fConfigTree = new JDOMHelper( inConfigURI );
		}
		catch(JDOMHelperException e)
		{
			throw new DiConfigException( kExTag
				+ "Unable to instantiate XML config file."
				+ " Reason: \"" + e + "\""
				);
		}

		// Initialize logging, warn if no logging set since this is JSP
		// This will throw an exception if it's unhappy
		instantiateLogger();

		// Initialize the database connection
		// connectToDatabase();
		initializeDatabase();

		// Read in information about reports
		initializeCollectionHash();

	};

	/**********************************************************
	 * Create the logger for doing system logging work...
	 *********************************************************/

	 // Initialize the logger, including passing it in configuration
	 // options set in the XML config file
	 static void instantiateLogger()
		throws DiConfigException
	 {
		final String kFName = "instantiateLogger";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// Looks up the logger config data from the main config
		// tree and returns an object
		// Since the Run Log is a shared, singleton instance we
		// don't really need to save it here.
		try
		{
			RunLogInterface tmpLogger =
				(RunLogInterface) fConfigTree.makeObjectFromConfigPath(
					"nie.core.RunLogBasicImpl", RUN_LOG_CONFIG_PATH
					);
		}
		catch(JDOMHelperException e1)
		{
			throw new DiConfigException( kExTag
				+ "Unable to instantiate logger, which is required for JSP apps."
				+ " Reason: \"" + e1 + "\""
				);
		}

	 }

	/**********************************************************
	 * Connect to the database
	 *********************************************************/

	// Use this once initializeDatabase has been run
	// initializeDatabase is called by the constructor
	public DBConfig getDBConfig()
	{
		return gDBConfig;
	}

	// public void connectToDatabase()
	private void initializeDatabase()
		throws DiConfigException
	 {
		final String kFName = "connectToDatabase";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// Set our global database configuration
		// This also initializes the database connection
		try
		{
			gDBConfig =
				(DBConfig) fConfigTree.makeObjectFromConfigPath(
					"nie.core.DBConfig", DB_CONFIG_PATH
					);
		}
		catch(JDOMHelperException e1)
		{
			throw new DiConfigException( kExTag
				+ "Unable to connect to database."
				+ " Reason: \"" + e1 + "\""
				);
		}

	}

	private void initializeCollectionHash()
	{
		final String kFName = "initializeCollectionHash";

		gCollectionHash = new Hashtable();
		gCollectionIDList = new Vector();

		// Find all the matching nodes
		List nodes = fConfigTree.findElementsByPath( COLLECTION_ELEMENTS_PATH );
		// It's perfectly fine if there are none
		if( nodes == null || nodes.size() < 1 )
		{
			infoMsg( kFName,
				"No specific collections defined.  Will use auto-naming rules."
				);
			return;
		}

		infoMsg( kFName,
			"Will examine " + nodes.size() + " collection nodes."
			);

		// Loop through the elements we found
		int counter = 0;
		for( Iterator it = nodes.iterator() ; it.hasNext() ; )
		{
			// Get the report
			Element node = (Element)it.next();
			counter++;
			// Get the values that we want
			String id = JDOMHelper.getStringFromAttributeTrimOrNull(
				node, COLLECTION_ID_ATTR
				);
			if( id == null )
			{
				errorMsg( kFName,
					"Collection # " + counter + " has no ID, skipping."
					);
				continue;
			}
			// Sanity check, they must have a SQL statement
			String sql = getSQLForCollection( node );
			if( sql == null )
			{
				errorMsg( kFName,
					"Collection # " + counter + " has no SQL defined"
					+ "; the element name to set is \"" + SQL_PATH + "\"."
					+ "Skipping this collection."
					);
				continue;
			}
			// Sanity check, they must have a CGI access URL for doc_fn
			String url = getCGIAccessURLForCollection( node );
			if( url == null )
			{
				warningMsg( kFName,
					"Collection # " + counter + " has no CGI access URL defined, skipping."
					+ "; the element name to set is \"" + CGI_ACCESS_URL_PATH + "\"."
					+ " The URL will be null for this collection"
					+ "; perhaps you're planning to use the key as the URL?"
					// + "Skipping this collection."
					);
				// continue;
			}

			// Make sure the hash doesn't already have it!
			if( gCollectionHash.containsKey(id) )
			{
				errorMsg( kFName,
					"Collection ID = \"" + id + "\" is already defined."
					+ " ( Duplicate is at collection element # " + counter + ")."
					+ " Skipping."
					);
				continue;
			}

			traceMsg( kFName,
				"Adding Collection ID = \"" + id + "\""
				);

			// Now add it
			gCollectionHash.put( id, node );
			gCollectionIDList.add( id );

		}   // End of for each element report

		debugMsg( kFName,
			"Hash finished with " + gCollectionHash.keySet().size() + " items from config."
			);
	}

	/**********************************************************
	 * Database Operations
	 *********************************************************/

	// Returns null on failure
	public Connection getConnection()
	{
		return getDBConfig().getConnectionOrNull();
		// If you prefer exceptions, use
		// .getConnection()
	}


	public List getCollectionIDList()
	{
		// return new Vector( gCollectionHash.keySet() );

		// We'd like them in order of appearance
		return gCollectionIDList;
	}

	/**********************************************************
	 * Getters/Setters
	 *********************************************************/

	private Element findCollectionElement( String inID )
	{
		final String kFName = "findCollectionElement";
		// Normalize to remove spaces
		inID = NIEUtil.trimmedStringOrNull( inID );
		// Sanity check
		if( inID == null )
		{
			errorMsg( kFName,
				"Null ID passed in, returning null class name."
				);
			return null;
		}
		// This shouldn't happen, handled in the constructor
		if( gCollectionHash == null )
		{
			errorMsg( kFName,
				"Collections not initialized, returning null class name."
				);
			return null;
		}
		// If it's in the hash, return it
		if( gCollectionHash.containsKey(inID) )
			return (Element)gCollectionHash.get(inID);
		else
		{
			errorMsg( kFName,
				"Collection ID not found, returning null collection."
				);
			return null;
		}
	}

	public String getDiFileNameForCollection( String inID )
	{
		final String kFName = "getBifFileNameForCollection";
		// Normalize to remove spaces
		inID = NIEUtil.trimmedStringOrNull( inID );
		// Sanity check
		if( inID == null )
		{
			errorMsg( kFName,
				"Null ID passed in, returning null."
				);
			return null;
		}
		// Get the collection
		Element coll = findCollectionElement( inID );
		if( coll == null )
		{
			errorMsg( kFName,
				"Unknown ID passed in, \"" + inID + "\", returning null."
				);
			return null;
		}

		// First, try to get a fixed name
		String answer = getFixedBiffNameForCollection( coll );
		if( answer != null )
			return answer;

		// Else autogenerate it
		return autoGenerateBifFileNameForCollection( inID );
	}

	private String autoGenerateBifFileNameForCollection( String inCollectionID )
	{
		final String kFName = "autoGenerateBifFileNameForCollection";
		inCollectionID = NIEUtil.trimmedStringOrNull( inCollectionID );
		if( inCollectionID == null )
		{
			errorMsg( kFName,
				"Null collection ID passed in, returning null class name."
				);
			return null;
		}
		StringBuffer buff1 = new StringBuffer();
		// Add the prefix
		buff1.append( getDiPrefixForCollection( inCollectionID ) );
		// Add the report ID itself
		buff1.append( inCollectionID );
		// Todo: maybe add date stuff
		// buff1.append( some date or sequence number );
		// Add the suffix
		buff1.append( getDiSuffixForCollection( inCollectionID ) );
		// We're done!
		return new String( buff1 );
	}

	public String getDiPrefixForCollection( String inID )
	{
		return getDiPrefixForCollection(
			findCollectionElement( inID )
			);
	}
	public String getDiPrefixForCollection( Element inColl )
	{
		final String kFName = "getBifPrefixForCollection(2)";
		if( inColl != null )
		{
			String tmpStr = JDOMHelper.getTextByPathTrimOrNull(
				inColl, BIF_PREFIX_PATH
				);
			if( tmpStr != null )
				return tmpStr;
			else
				return DEFAULT_BIF_PREFIX;
		}
		else
		{
			errorMsg( kFName,
				"Collection not found, returning default value."
				);
			return DEFAULT_BIF_PREFIX;
		}
	}




	public String getDiSuffixForCollection( String inID )
	{
		return getDiSuffixForCollection(
			findCollectionElement( inID )
			);
	}
	public String getDiSuffixForCollection( Element inColl )
	{
		final String kFName = "getBifSuffixForCollection(2)";
		if( inColl != null )
		{
			String tmpStr = JDOMHelper.getTextByPathTrimOrNull(
				inColl, BIF_SUFFIX_PATH
				);
			if( tmpStr != null )
				return tmpStr;
			else
				return DEFAULT_BIF_SUFFIX;
		}
		else
		{
			errorMsg( kFName,
				"Collection not found, returning default value."
				);
			return DEFAULT_BIF_SUFFIX;
		}
	}

	public String getFixedBiffNameForCollection( String inID )
	{
		return getFixedBiffNameForCollection(
			findCollectionElement( inID )
			);
	}
	public String getFixedBiffNameForCollection( Element inColl )
	{
		final String kFName = "getFixedBiffNameForCollection(2)";
		if( inColl != null )
		{
			String tmpStr = JDOMHelper.getTextByPathTrimOrNull(
				inColl, BIF_FIXED_NAME_PATH
				);
			if( tmpStr != null )
				return tmpStr;
			else
				return null;
		}
		else
		{
			errorMsg( kFName,
				"Collection not found, returning null."
				);
			return null;
		}
	}

	public String getSQLForCollection( String inID )
	{
		return getSQLForCollection(
			findCollectionElement( inID )
			);
	}
	public String getSQLForCollection( Element inColl )
	{
		final String kFName = "getSQLForCollection(2)";
		if( inColl != null )
		{
			String tmpStr = JDOMHelper.getTextByPathTrimOrNull(
				inColl, SQL_PATH
				);
			if( tmpStr != null )
				return tmpStr;
			else
				return null;
		}
		else
		{
			errorMsg( kFName,
				"Collection not found, returning null."
				);
			return null;
		}
	}


	public String getCGIAccessURLForCollection( String inID )
	{
		return getCGIAccessURLForCollection(
			findCollectionElement( inID )
			);
	}
	public String getCGIAccessURLForCollection( Element inColl )
	{
		final String kFName = "getCGIAccessURLForCollection(2)";
		if( inColl != null )
		{
			String tmpStr = JDOMHelper.getTextByPathTrimOrNull(
				inColl, CGI_ACCESS_URL_PATH
				);
			if( tmpStr != null )
				return tmpStr;
			else
				return null;
		}
		else
		{
			errorMsg( kFName,
				"Collection not found, returning null."
				);
			return null;
		}
	}






	/**********************************************************
	 * Logging operations
	 *********************************************************/

	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg(
			kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg(
			kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg(
			kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg(
			kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg(
			kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg(
			kClassName, inFromRoutine,
			inMessage
			);
	}
	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}



	////// Main ////////////////////////////////////////////////////

	public static void main(String[] args)
	{
		final String kFName = "main";

		// statusMsg( kFName, "Starting" );

		if( args.length < 1 )
		{
			errorMsg( kFName,
				"Syntax error, missing arg1 (config file name)."
				+ " Syntax is: java " + kClassName + " bif_maker_config_uri.xml"
				+ " Exiting program (error code 1)."
				);
			System.exit( 1 );
		}
		String configFile = args[0];

		statusMsg( kFName,
			"Will read config URI \"" + configFile + "\""
			);

		DiConfig myDiConf = null;
		try
		{
			myDiConf = new DiConfig( configFile );
		}
		catch (DiConfigException e)
		{
			errorMsg( kFName,
				"Unable to construct SR Config object"
				+ " Exception = \"" + e + "\""
				+ " Exiting program (error code 2)."
				);
			System.exit( 2 );
		}

		statusMsg( kFName,
			"Was able to read config and initialize."
			+ " This usually indicates that the database is working as well"
			+ " unless it has been deactivated in the config."
			);

		// Since we handle multiple collections, we must refer to them by ID
		// And if we're referring to them by ID, we need to know what
		// report ID's we have available.
		List collectionIDs = myDiConf.getCollectionIDList();
		statusMsg( kFName,
			"Found " + collectionIDs.size() + " collection(s) defined in config."
			);

		// Show a brief list of report ID's and their data

		// for( int i = 0; i < collectionIDs.size(); i++ )
		// {
		//      String collID = (String) collectionIDs.get(i);

		// A main loop over the ID's
		for( Iterator it = collectionIDs.iterator(); it.hasNext() ; )
		{
			// Get the report ID from the list
			String collID = (String) it.next();
			// Get the calculated name of the Bulk file we will use
			String diName = myDiConf.getDiFileNameForCollection( collID );
			// Get the SQL query
			String sql = myDiConf.getSQLForCollection( collID );
			// Get the URL to access the records with
			String url = myDiConf.getCGIAccessURLForCollection( collID );

			// Display a message
			statusMsg( kFName,
				"Coll \"" + collID + "\" bif file \"" + diName + "\"."
				+ " SQL=\"" + sql + "\""
				+ " CGI URL =\"" + url + "\""
			);
		}

	}



	/**********************************************************
	 * Instance variables
	 *********************************************************/

	// This represents the in-memory copy of the entire XML configuration tree
	// It is used by various getters and setters
	// It is initialized in the constructor
	// Static, like other fields, per Kevin
	private static JDOMHelper fConfigTree;

	// The database configuration
	private DBConfig gDBConfig;

	// The hash the maps colleciton ID's to Element nodes
	private Hashtable gCollectionHash;
	private List gCollectionIDList;



	// Some paths to subobjects within the main config tree

	// This is relative to the main node (so does not include the root elem name)
	// Run Log info (verbosity and log file)
	private static final String RUN_LOG_CONFIG_PATH = "run_logging";

	// The main XML branch path for MOST bulk maker settings
	private static final String MAIN_BIF_PATH = "bif_maker";

	// Database config info
	private static final String DB_CONFIG_PATH =
		MAIN_BIF_PATH + "/database";

	// The path to find individual report elements
	private static final String COLLECTION_ELEMENTS_PATH =
		MAIN_BIF_PATH + "/collection";
	// The attribute of a collection that is the collection's ID
	private static final String COLLECTION_ID_ATTR = "id";

	// This is RELATIVE TO EACH COLLECTION
	private static final String SQL_PATH = "main_sql_query";

	// This is RELATIVE TO EACH COLLECTION
	// The URL to access the item via CGI, minus the actual ID value
	private static final String CGI_ACCESS_URL_PATH = "cgi_access_url_root";

	// Where to find info about bulk file names, and the defaults
	// These are RELATIVE TO EACH COLLECTION
	private static final String BIF_FIXED_NAME_PATH = "bif_file_fixed_name";
	private static final String BIF_PREFIX_PATH = "bif_file_prefix";
	private static final String DEFAULT_BIF_PREFIX = "bulk_file_";
	private static final String BIF_SUFFIX_PATH = "bif_file_suffix";
	private static final String DEFAULT_BIF_SUFFIX = ".bif";



}
