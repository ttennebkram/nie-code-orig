package nie.di;
/*
 DataIndexer.java - 1.02 1/08/2003 mbk

 Changes in version 1.02
     supporting classes named Di* rather than Bif*
     added commented change history
     removed old VERSION constant, superceded by
          APPNAME and APPVERSION

 Changes in version 1.01
     retrieving SQL data results by column position
          rather than column name - allows use on any table
     displaying program name and version as first line
          of output

*/

import java.util.*;
import java.io.*;
import java.sql.*;
import org.jdom.Element;
import nie.core.*;
import java.lang.*;



public class DataIndexer
 {

  private final static String kClassName = "DataIndexer";
  static boolean first=true;
  static final String APPNAME = "DataIndexer";
  static final String APPVERSION = "1.02 1/08/2003";



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
  // This gets us to the logging object
  private static RunLogInterface getRunLogObject()
  {
    return RunLogBasicImpl.getRunLogObject();
  }





/// main //////////////////////////////////  main /////////////

public static void main(String[] args)
{

  final String kFName = "main";



  int i=0;
  boolean targetCollSpecified = false;  // is a colleciton specified in args
  int numberOfColls = 0;                // number of cols in config file
  int currentColl=0;                    // loop variable
  int lineCount=0;                      // debug var
  String targetColl="";                 // user speciried collection
  String configFile="";                 // user specified config file REQ
  String keyValue="";                   // article id for link

  boolean success=false;                // did we output any linkss
  // this is here so if a colleciton is specified but never found
  // we know we did nothign so we can warn the user
  DiConfig myBC = null;
  ResultSet rs = null;
  PrintStream outPS = null;


  System.out.print(APPNAME + " version " + APPVERSION + "\n" );
  System.out.flush();

  //  Check run-time parms

  if( args.length < 1 )
  {
    errorMsg( kFName,
              "Syntax error, missing arg1 (config file name)."
              + " Syntax is: java " + kClassName + " config_uri.xml"
              + " Exiting program (error code 1)."
              );
    System.exit( 1 );
  }
  configFile = args[0];

  if( args.length == 2)
  {
    targetCollSpecified = true;
    targetColl = args[1];
  }

// Get the DiConfig data
  try
  {
    myBC = new DiConfig( configFile );
  }  // try
  catch(Exception e)
  {
    System.out.println( "exception: " + e );
  }  // catch new DiConfig

// Assign variables from config file
List collectionIDs = myBC.getCollectionIDList();

// begin iterating collections
for( Iterator it = collectionIDs.iterator(); it.hasNext() ; )
{
  // Get the report ID from the list
  String collID = (String)it.next();

  // get data for this collIDS from the config file
  String diName = myBC.getDiFileNameForCollection( collID );
  String sql = myBC.getSQLForCollection( collID );
  String leadURL = myBC.getCGIAccessURLForCollection(collID);

  // Display information
  statusMsg( kFName,
             "Coll \"" + collID + "\" di file \"" + diName + "\"."
             + " SQL=\"" + sql + "\""
             );
  //
  // Process this collection if no command line collection name was
  // given, or if it was given and the current collection is the
  // one specified
  //

  if( ( ( targetCollSpecified & collID.equals(targetColl) ) ) |
      ( !targetCollSpecified ) )
  {
    statusMsg( kFName, "Processing collection " + collID );
    DBConfig myDB = myBC.getDBConfig();
	Connection con = null;
	Statement st = null;
    try
    {
      // rs = myDB.runQuery(sql);
	  Object [] objs = myDB.runQuery( sql, true );
	  rs = (ResultSet) objs[0];
	  st = (Statement) objs[1];
	  con = (Connection) objs[2];

     while(rs.next())
    {
      if(first)
      {
        try{
          outPS = new PrintStream(
              new FileOutputStream( diName ));
        }
        catch(Exception e){
          System.err.print("Exception " + e );
        }

        DiWriteSeed.Header_Rec(outPS);
        first = false;
      }
      keyValue=rs.getString(1);
      DiWriteSeed.Data_Rec( outPS, leadURL , keyValue );
      lineCount = lineCount+1;
      // WriteSeed.Eod_Rec( outPS );
    //   rs.next();
    }  // end while loop for more records
    DiWriteSeed.Trailer_Rec( outPS );

    }  // end try block
    catch(Exception e){    // any errors since dbget
      errorMsg(kFName, "Error in database loop" + e);
    }
	finally {
		rs = DBConfig.closeResults( rs, kClassName, kFName, false );
		st = DBConfig.closeStatement( st, kClassName, kFName, false );
		con = DBConfig.closeConnection( con, kClassName, kFName, false );
	}


  }  // end of if test for target collection specified

  // done with this collection; flush the di file buffer
  // if first is false, we've been thru the loop once so we wrote
  // data for a previous colleciton; reset it to first, flush
  // and close the output device
  if(!first){
    first = true;
    try{
      outPS.flush();
      outPS.close();
    }
    catch(Exception e){
      statusMsg(kFName, "Error closing output file" +
               diName + "reports " + e);
    }

  }

  statusMsg( kFName, "loop count is " + lineCount );
  lineCount = 0; // reset lc between collections
}  // end iteration over collections in config file
statusMsg( kFName, "out if if loop ");
}   // End iteration over collections



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

// Where to find info about bulk file names, and the defaults
// These are RELATIVE TO EACH COLLECTION
private static final String BIF_FIXED_NAME_PATH = "bif_file_fixed_name";
private static final String BIF_PREFIX_PATH = "bif_file_prefix";
private static final String DEFAULT_BIF_PREFIX = "bulk_file_";
private static final String BIF_SUFFIX_PATH = "bif_file_suffix";
private static final String DEFAULT_BIF_SUFFIX = ".html";


}

