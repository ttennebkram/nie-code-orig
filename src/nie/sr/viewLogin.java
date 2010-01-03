package nie.sr;

import java.util.*;
import java.io.File;
import javax.servlet.jsp.*;
import javax.servlet.http.*;

public class viewLogin extends SRView
{
	/*************************************************
	 * Constructor
	 ************************************************/
	 
	public viewLogin( modelLogin inModel ) throws SRException
	{
		super( inModel );
	}
	
	/*************************************************
	 * Setters and Getters
	 ************************************************/
	 
	// This one is for convenience so that I don't have
	// to keep casting the base class' getModel() call
	
	modelLogin getModelLogin()
	{
		return (modelLogin)getModel();
	}
	
	/**************************************************
	 **************************************************
	 **
	 ** Output various forms/pages to the user
	 **
	 **************************************************
	 *************************************************/
	 
	 // Get the user name and password
	 
	public void requestUserNameAndPassword() throws SRException
	{
		SRUserInfo lUserInfo = getModelLogin().getUserInfo();
		
		// Create the hash table that has the fields we can fill in
		Hashtable lHashtable = new Hashtable();
		String lUserName = lUserInfo.getUserName();
		if( lUserName == null )
			lUserName = "";
		lHashtable.put( kUserNameMarker, lUserName );
		
		// Display the form to the user.
		displayTemplateFile( getWriter(), kUserNamePasswordFormFile, lHashtable );
	};
	
	// the user has performed too many failed attempts...
	
	public void tooManyAttempts() throws SRException
	{
		displayTemplateFile( kTooManyAttemptsFile, null );
	}
	
	// Display the main screen - they logged on successfully!
	
	public void goMainScreen()
	{
		try
		{
			HttpServletResponse lResponse = getResponse();
			lResponse.sendRedirect( kMainPageURL );
		} catch( Exception ioe ) {
			SRConfig.doErrorMsg( "viewLogin", "goMainScreen", "Exception thrown when transferring to the main screen.  Exception is: '" + ioe.toString() + "'" );
		}
	}
	
	/****************************************************
	 * Constants that we've used
	 ***************************************************/
	
	public static final String kUserNameMarker = "<user_name>";
	
	// static final String kFileNamePrefix = "html" + File.separator;
	static final String kFileNamePrefix = "html/";
	static final String kFileNameSuffix =  ".html";
	
	static final String kMainPageURL = kFileNamePrefix +
	    "main" +
	    kFileNameSuffix;
										
	static final String kUserNamePasswordFormFile = kFileNamePrefix +
	    "login" +
	    kFileNameSuffix;
													
	static final String kTooManyAttemptsFile  = kFileNamePrefix +
	    "badlogin" +
	    kFileNameSuffix;
}
