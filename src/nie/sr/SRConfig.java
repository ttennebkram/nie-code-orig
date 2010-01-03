package nie.sr;

import java.util.*;
import java.io.*;
import java.sql.*;
import org.jdom.Element;
import nie.core.*;

public class SRConfig
{
    private final static String kClassName = "SRConfig";

    /**********************************************************
     * Constructor
     *********************************************************/

    public SRConfig()
    {
	// There should be only one instance of the SRConfig structure

	if( gSoleInstance == null )
	    gSoleInstance = this;

	gIsLoaded = false;
    }

    public void SRConfigInit( String inConfigURI )
		throws SRException
    {

		final String kFName = "SRConfigInit";
		final String kExTag = kClassName + ':' + kFName + ": ";

System.err.println( "hello sr1" );

		if( inConfigURI == null )
		{
	    	throw new SRException( kExTag
				   + "Null input URI given; please supply a file name or URL."
				   );
		}
System.err.println( "hello sr2" );

		// Load and store the main XML configuration data
		try
		{
System.err.println( "hello sr3" );
		    fConfigTree = new JDOMHelper( inConfigURI );
System.err.println( "hello sr4" );
		}
		catch(JDOMHelperException e)
		{
System.err.println( "hello sr5" );
		    throw new SRException( kExTag
					   + "Unable to instantiate XML config file."
					   + " Reason: \"" + e + "\""
					   );
		}
System.err.println( "hello sr6" );

	// Initialize logging, warn if no logging set since this is JSP
	// This will throw an exception if it's unhappy
	instantiateLogger();
System.err.println( "hello sr7" );

	// Initialize the database connection
	// connectToDatabase();
	initializeDatabase();
System.err.println( "hello sr8" );

	// Read in information about reports
	initializeReportIDToClassNameHash();
System.err.println( "hello sr9" );

	// We're all set
	gIsLoaded = true;
System.err.println( "hello sr10" );
    };

    /**********************************************************
     * Status checks
     *********************************************************/

    public boolean isLoaded()
    {
	return gIsLoaded;
    };


    public String getBaseDirectory()
    {
	// It's stored as an attribute of the main search track node
	String tmpStr = fConfigTree.getTextFromSinglePathAttr(
							      BASE_DIR_PATH, BASE_DIR_ATTR
							      );
	// Trim, and force to null if empty
	return NIEUtil.trimmedStringOrNull( tmpStr );
    }


    /**********************************************************
     * Error information
     *********************************************************/

    public static String getErrorMessage() { return gErrorMessage; };
    // ^^^ Shouldn't this be getErrorMessage()
    //  or getLastErrorMessage()  ???

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

    /**********************************************************
     * Getters/Setters
     *********************************************************/

    static public SRConfig	getSoleInstance() { return gSoleInstance; };


    public String getClassNameForReportID( String inReportID )
    {
	return getClassNameFromHash( gReportClassNameHash, inReportID, "ReportClass_" );
    }

    public String getClassNameForFilterID( String inFilterID )
    {
	return getClassNameFromHash( gFilterClassNameHash, inFilterID, "FilterClass_" );
    }

    private String getClassNameFromHash( Hashtable inHash, String inID, String inPrefix )
    {
	final String kFName = "getClassNameFromHash";
	// Normalize to remove spaces
	inID = NIEUtil.trimmedStringOrNull( inID );
	// Sanity check
	if( inID == null ) {
	    errorMsg( kFName,
		      "Null report ID passed in, returning null class name."
		      );
	    return null;
	}
	// This shouldn't happen, handled in the constructor
	if( inHash == null ) {
	    return autoGenerateClassName( inID, inPrefix );
	}
	// If it's in the hash, return it
	if( inHash.containsKey(inID) )
	    return (String)inHash.get(inID);
	// Else auto generate the name and return that
	return autoGenerateClassName( inID, inPrefix );
    }
    private static String autoGenerateClassName( String inReportID, String inPrefix )
    {
	final String kFName = "autoGenerateClassName";
	if( inReportID == null )
	    {
		errorMsg( kFName,
			  "Null report ID passed in, returning null class name."
			  );
		return null;
	    }
	return GENERIC_CLASS_NAME_PREFIX + inPrefix + inReportID;
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

    private static RunLogInterface getRunLogObject()
    {
	return RunLogBasicImpl.getRunLogObject();
    }


    static public void doErrorMsg( String inClassName, String inMethodName, String inErrorMessage )
    {
	getRunLogObject().errorMsg(
				   inClassName, inMethodName,
				   inErrorMessage
				   );
    }

    static public void doDebugMsg( String inClassName, String inMethodName, String inErrorMessage )
    {
	getRunLogObject().debugMsg( inClassName, inMethodName, inErrorMessage );
    }

    static public void doFatalMsg( String inClassName, String inMethodName, String inErrorMessage )
    {
	getRunLogObject().fatalErrorMsg( inClassName, inMethodName, inErrorMessage );
    }

    /**********************************************************
     * Create the logger for doing system logging work...
     *********************************************************/

    // Initialize the logger, including passing it in configuration
    // options set in the XML config file

    static void instantiateLogger()
	throws SRException
    {
	final String kFName = "instantiateLogger";
	final String kExTag = kClassName + ':' + kFName + ": ";

	// Looks up the logger config data from the main config
	// tree and returns an object
	// Since the Run Log is a shared, singleton instance we
	// don't really need to save it here.

	try {
	    RunLogInterface tmpLogger =
		(RunLogInterface) fConfigTree.makeObjectFromConfigPath(
								       "nie.core.RunLogBasicImpl", RUN_LOG_CONFIG_PATH
								       );
	}
	catch(JDOMHelperException e1)
	    {
		gErrorMessage =
		    "Unable to instantiate logger, which is required for JSP apps."
		    + " Reason: \"" + e1 + "\""
		    ;
		throw new SRException( kExTag
				       + gErrorMessage
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
	throws SRException
    {
	final String kFName = "connectToDatabase";
	final String kExTag = kClassName + ':' + kFName + ": ";

	// Set our global database configuration
	// This also initializes the database connection

	try {
	    gDBConfig =
		(DBConfig) fConfigTree.makeObjectFromConfigPath(
								"nie.core.DBConfig", DB_CONFIG_PATH
								);
	} catch(JDOMHelperException e1) {
	    throw new SRException( kExTag
				   + "Unable to connect to database."
				   + " Reason: \"" + e1 + "\""
				   );
	}

    }


    private void initializeReportIDToClassNameHash()
    {
	gReportClassNameHash = genericInitIDToClassNameHash( REPORT_ELEMENTS_PATH, REPORT_ID_ATTR, REPORT_CLASS_ATTR );
    }

    private void initializeFilterIDToClassNameHash()
    {
	gFilterClassNameHash = genericInitIDToClassNameHash( FILTER_ELEMENTS_PATH, REPORT_ID_ATTR, REPORT_CLASS_ATTR );
    }

    private Hashtable genericInitIDToClassNameHash( String inElementPath, String inIDAttr, String inClassAttr )
    {
	final String kFName = "genericInitIDToClassnameHash";

	Hashtable retHash = new Hashtable();

	// Find all the matching nodes
	List lElements = fConfigTree.findElementsByPath( inElementPath );

	// It's perfectly fine if there are none
	if( lElements == null || lElements.size() < 1 )
	    {
		infoMsg( kFName,
			 "No specific items defined.  Will use auto-naming rules."
			 );
		return null;
	    }

	infoMsg( kFName,
		 "Will examine " + lElements.size() + " nodes."
		 );

	// Loop through the elements we found
	int lElementCounter = 0;
	for( Iterator it = lElements.iterator() ; it.hasNext() ; ) {
	    // Get the report

	    Element lElement = (Element)it.next();
	    lElementCounter++;

	    // Get the values that we want
	    String lID = JDOMHelper.getStringFromAttributeTrimOrNull(
								     lElement, inIDAttr
								     );
	    if( lID == null ) {
		errorMsg( kFName,
			  "Element # " + lElementCounter + " has no ID, skipping."
			  );
		continue;
	    }

	    String lClass = JDOMHelper.getStringFromAttributeTrimOrNull(
									lElement, inClassAttr
									);
	    if( lClass == null ) {
		// Could argue about whether this is info or warning
		// Almost seems a warning now, but would be normal when reports
		// were extended for other purposes.

		infoMsg( kFName,
			 "Element # " + lElementCounter + " has no java class specified."
			 + " Element ID = \"" + lID + "\""
			 + " Skipping."
			 );
		continue;
	    }

	    // Make sure the hash doesn't already have it!

	    if( retHash.containsKey(lID) ) {
		errorMsg( kFName,
			  "Element ID = \"" + lID + "\" is already defined."
			  + " ( Duplicate is at element # " + lElementCounter + ")."
			  + " Skipping."
			  );
		continue;
	    }

	    traceMsg( kFName,
		      "Adding Element ID = \"" + lID + "\""
		      + " Java Class = \"" + lClass + "\" to class."
		      );

	    // Now add it
	    retHash.put( lID, lClass );

	}   // End of for each element report

	debugMsg( kFName,
		  "Hash finished with " + retHash.keySet().size() + " items from config."
		  );

	return retHash;
    }

    ////// Get the configuration instance and throw an exception if it doesn't exist

    public static SRConfig getConfigInstance() throws SRException
    {
	if( getSoleInstance() == null )
	    throw new SRException( "getSoleInstance() returned a null." );
	else
	    return getSoleInstance();
    }

    ////// Main ////////////////////////////////////////////////////

    public static void main(String[] args)
    {
	final String kFName = "main";

	// statusMsg( kFName, "Starting" );

	if( args.length < 1 ) {
	    errorMsg( kFName,
		      "Syntax error, missing arg1 (config file name)."
		      + " Syntax is: java " + kClassName + " sr_config_uri.xml"
		      + " Exiting program (error code 1)."
		      );
	    System.exit( 1 );
	}
	String configFile = args[0];

	statusMsg( kFName,
		   "Will read config URI \"" + configFile + "\""
		   );

	SRConfig mySR = null;

	try {
	    mySR = new SRConfig();
	    mySR.SRConfigInit( configFile );
	} catch (SRException e) {
	    errorMsg( kFName,
		      "Unable to construct SR Config object"
		      + " Exception = " + e
		      + " Exiting program (error code 2)."
		      );
	    System.exit( 2 );
	}

	statusMsg( kFName,
		   "Was able to read config and intialize."
		   );

	statusMsg( kFName,
		   "report1 class name is \""
		   + mySR.getClassNameForReportID("report1") + "\""
		   );
	statusMsg( kFName,
		   "report2 class name is \""
		   + mySR.getClassNameForReportID("report2") + "\""
		   );
	statusMsg( kFName,
		   "Base dir is \""
		   + mySR.getBaseDirectory() + "\""
		   );



	// Todo: try running query

    }



    /**********************************************************
     * Instance variables
     *********************************************************/

    // Whether we are ready or not

    private static boolean gIsLoaded = false;

    // This represents the in-memory copy of the entire XML configuration tree
    // It is used by various getters and setters
    // It is initialized in the constructor
    // Static, like other fields, per Kevin

    private static JDOMHelper fConfigTree;

    // The database configuration

    private static DBConfig gDBConfig;

    // The hash the maps report ID's to java class names
    // most reuprt class names are just auto generated

    private static Hashtable gFilterClassNameHash;
    private static Hashtable gReportClassNameHash;

    private static SRConfig gSoleInstance = null;

    private static String			gErrorMessage	= null;

    // The package name and name prefix we will prepend to AUTO generated
    // class names.  This can be overrode in the XML config file to allow
    // for specific mappings

    private static final String GENERIC_CLASS_NAME_PREFIX =
	"nie.sr.reports.";

    // Where to find the optional base directory
    private static final String BASE_DIR_PATH = "search_track";
    private static final String BASE_DIR_ATTR = "base_directory";


    // The path to find individual report elements

    private static final String REPORT_ELEMENTS_PATH =
	"search_track/reports/report";
    private static final String FILTER_ELEMENTS_PATH =
	"search_track/reports/filter";

    // The attribute of a report that is the report's ID

    private static final String REPORT_ID_ATTR = "id";

    // The attribute that lists the java class implementation

    private static final String REPORT_CLASS_ATTR = "report_java_class";

    // Some paths to subobjects within the main config tree
    // This is relative to the main node (so does not include the root elem name)
    // Run Log info (verbosity and log file)

    private static final String RUN_LOG_CONFIG_PATH = "run_logging";

    // Database config info

    private static final String DB_CONFIG_PATH = "search_track/database";
}
