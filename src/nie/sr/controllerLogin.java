package nie.sr;

import java.sql.*;
import java.util.*;

public class controllerLogin extends SRController
{
    /************************************************************
     * Constructor
     ************************************************************/
    
    public controllerLogin( modelLogin inModel ) throws SRException
    {
		super( inModel );
    }
    
    /************************************************************
     * Utility routine so that I don't have to keep casting the getModel()
     * call from SRController
     ************************************************************/
    
    modelLogin getLoginModel()
    {
    	return (modelLogin)getModel();
    }
    
    /************************************************************
     * Main processing entry point for the controller.
     ************************************************************/
    
    public void process() throws SRException
    {
		final String kFName = "process";

		/*************************************
		 * Process the request - theoretically
		 * we have all the information that we
		 * need.
		 ************************************/
		
		// Get the configuration object from the system so that we can get to the database connection
		
		final String kErrorMsg = "No or erroneous configuration found.  System could not be configured.";
		
		// Get a pointer to the information from the login page
		modelLogin lModel = getLoginModel();
		if( lModel == null )
		{
		    doErrorMsg( kFName, "SRConfig.getLoginModel() returned a null!!!!???" );
		    return;
		}
	
		// Get the view object for easy access
		viewLogin lView = lModel.getLoginView();
		if( lView == null )
		{
		    doErrorMsg( kFName, "SRConfig.getLoginModel() returned a null!!!???" );
		    return;
		}

		// Get our main configuration data	    
		SRConfig lConfig = SRConfig.getSoleInstance();
		if( lConfig == null )
		{
		    doErrorMsg( kFName, "SRConfig.getSoleInstance() returned a null!!!!???" );
		    lView.displayErrorFile( SRView.kUnavailableFileName, kErrorMsg );
		    return;
		}

		// Get a connection to the database.
		Connection lConnection = lConfig.getConnection();
		if( lConnection == null )
		{
		    doErrorMsg( kFName, "SRConfig.getConnection() returned a null!!!!???" );
		    lView.displayErrorFile( SRView.kUnavailableFileName, kErrorMsg );
		    return;
		}
	
		// Create the mapping from marker to replacement information
		
		Hashtable lHashtable = new Hashtable();
		lHashtable.put( kUserNameMarker,  lModel.getUserName() );
		lHashtable.put( kUserPasswordMarker, lModel.getUserPassword() );
		
		// Create the SQL statement to execute
		
		String lSQL = SRView.createOutputStringFromTemplate(
			kLoginSQL, lHashtable
			);
		doDebugMsg( kFName, "SQL to check user is: " + lSQL );
		// doErrorMsg( kFName, "(not error) SQL to check user is: " + lSQL );

	
		ResultSet lResultSet;

		// Do all SQL processing needed to validate this user/password combo	
		try
		{		
		    // Create a statement object that we can use to execute SQL statements
		    Statement lStatement = lConnection.createStatement();
		    
		    // And execute it.
		    lResultSet = lStatement.executeQuery( lSQL );
		    
		    // Position the result set to the first row returned OR
		    // determine that the result set has no rows.

			// If there is at least one row		    
		    if( lResultSet.next() )
		    {
	
				// There is at least one row if we get to here
		
				doDebugMsg( kFName, "user_id from database is: "
					+ lResultSet.getString( "user_id" )
					);

				// Copy this record's fields into the User Info object
				lModel.getUserInfo().fillFromResultSet( lResultSet );

				// Was the user intentionally locked out?
				if( lModel.getUserInfo().getSecurityLevel() < 0 )
				{
		
				    // User has been intentionally locked out of the system.
				    // Stonewall'em by providing a blank log in screen again
		
				    doDebugMsg( kFName,
				    	"getSecurityLevel() is "
				    	+ lModel.getUserInfo().getSecurityLevel()
				    	);
				    lView.requestUserNameAndPassword();
				    return;
		
				}
				// Have they tried too many times to login?
				else if( lModel.getUserInfo().getNumberOfTries() > kMaxTries )
				{
				    doErrorMsg( kFName,
				    	"Too many login attempts by the same session."
				    	);
				    lView.tooManyAttempts();
				    return;
				}
				// Else good login!!!!
				else
				{
		
				    // Send'em to the main screen!!
				    SRUserInfo lUserInfo = lModel.getUserInfo();
				    doDebugMsg( kFName,
				    	"Logging in User ID: "
				    	+ lUserInfo.getUserID()
				    	+ " user name: "
				    	+ lUserInfo.getUserName()
				    	);
				    lView.doRedirect( "main.jsp" );
				}
		    }
		    // Else no rows, user not found
		    else
		    {
				SRConfig.doErrorMsg( "controllerLogin", kFName,
					"Bad attempted login - user name of '"
					+ lModel.getUserName()
					+ "' password of '"
					+ lModel.getUserPassword() + "'"
					);
				lView.requestUserNameAndPassword();
		    }
		}
		// Catch exceptions from user validation SQL stuff
		catch( SQLException se )
		{
			// Todo: Security issue with sql in message?
			String msg =
		    	"tried to execute \"" + lSQL + "\""
		    	+ " which generated exception " + se.toString()
		    	;

		    // doErrorMsg( kFName, msg );
		    doErrorMsg( kFName, "SQL Exception: " + se );



		    //// ***** HERE ******************
		    lView.displayErrorFile( SRView.kUnavailableFileName, msg );


		    return;
		}
    }
    
    /***********************************************************
     * Log an error message to the system log
     **********************************************************/
    
    static void doErrorMsg( String inMethodName, String inMessage )
    {
		SRConfig.doErrorMsg( "controllerLogin", inMethodName, inMessage );
    }

    static void doDebugMsg( String inMethodName, String inMessage )
    {
		SRConfig.doDebugMsg( "controllerLogin", inMethodName, inMessage );
    }

    static final String kUserNameMarker = "<user_name>";
    static final String kUserPasswordMarker = "<user_password>";
	public static String getLoginTableName()
	{
		// return kLoginTable;
		return nie.core.DBConfig.USER_INFO_TABLE;
	}

	// The name of the login table
	// static final String kLoginTable = "nie_user_info";
	// moved to db config
    static final String kLoginSQL =
    	// "SELECT * FROM userinfo "
    	"SELECT * FROM " + getLoginTableName()
    	+ " WHERE user_name='" + kUserNameMarker + "'"
    	+ " AND password='" + kUserPasswordMarker + "'";
    static final int kMaxTries = 3;
}
