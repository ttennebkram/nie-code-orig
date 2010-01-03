<%@ page
	 language="java"
	 contentType="text/html"
%>
	 // errorPage="ErrorPage.jsp"
<%@ page import="java.io.*, nie.sr.*, java.util.*"
%>
<%
        /* =============================================================
         * Copyright 2002 by New Idea Engineering.
         * Written by Kevin-Neil Klop
         *
         * Log in to the reporting module.
         *
         **************************************
         *
         * $id:$
         *
         * $Log: login.jsp,v $
         * Revision 1.3  2002/11/13 00:23:15  kklop
         * Fixed the "next link" bug.
         *
         * Revision 1.2  2002/10/08 16:20:27  kklop
         * Tuesday morning check in
         *
         * Revision 1.1  2002/10/07 03:25:52  kklop
         * Initial revision
         *
         *
         *
         * =============================================================
         */
%>
<jsp:useBean id="gConfig" scope="application" class="nie.sr.SRConfig"
/>
<jsp:useBean id="gUserInfo" scope="session" class="nie.sr.SRUserInfo"
/>
<jsp:useBean id="lPageInfo" scope="page" class="nie.sr.modelLogin"
/>
<jsp:setProperty name="lPageInfo" property="*"
/>
<%
	final String lLoginFormName = "html" + File.separator + "login.html";
	// final String kDefaultConfigFileName = "./sr_config.xml";
	final String kDefaultConfigFileName = "D:/apps/tomcat/webapps/SearchTrackReporting/sr_config.xml";


	if( gConfig == null )
	{
		nie.sr.SRConfig.doErrorMsg( "main.jsp", "main", "gConfig is null" );
		SRView.internalError( new PrintWriter(out) );
		return;
	}

	if( ! gConfig.isLoaded() )
	{
		try
		{
			gConfig.SRConfigInit( kDefaultConfigFileName );
		}
		catch( nie.sr.SRException sce )
		{
			gConfig.doErrorMsg( "login.jsp", "main", "Exception loading configuration file '" +
								kDefaultConfigFileName + "'\n" +
								"Error string is: \n\n" +
								sce +"\n" );
			SRView.displayErrorFile( new PrintWriter(out),
							"html" + File.separator + "unavailable.html",
							"" + sce + ""
							);
			return;
		}
	}
		
	/*********************************************************
	 * Okay, the config is loaded - now see about the model...
	 ********************************************************/	

	if( lPageInfo.isLoaded() )
	{
		gConfig.doDebugMsg( "login.jsp", "main", "lPageInfo.isLoaded() returned true." );

		/*****************************************************
		 * Theoretically we have all the information from the
		 * page that we need.  Go ahead and process the page.
		 ****************************************************/
		try
		{
			controllerLogin lController = new controllerLogin( lPageInfo );
			viewLogin lView = new viewLogin( lPageInfo );
			lPageInfo.setView( lView );
			lPageInfo.setController( lController );
			lPageInfo.setUserInfo( gUserInfo );
			 
			lView.setWriter( new PrintWriter(out) );
			lView.setResponse( response );

			//// ************* HERE ****************************
			lController.process();
		}
		catch( SRException se )
		{
			nie.sr.SRConfig.doErrorMsg( "login.jsp", "main", "" + se );
			SRView.internalError( new PrintWriter( out ) );
		}
	}
	else
	{
		gConfig.doDebugMsg( "login.jsp", "main", "lPageInfo.isLoaded() returned false." );

		/*****************************************************
		 * We're still missing information from the page that
		 * we need.  Redisplay the form and ask for all the
		 * information again.
		 ****************************************************/
		try
		{
			Hashtable lHashtable = new Hashtable();
			String lUserName = lPageInfo.getUserName();
			if( lUserName != null )
			{
				lHashtable.put( viewLogin.kUserNameMarker, lUserName );
				SRView.displayTemplateFile( new PrintWriter(out),
					lLoginFormName,
					 lHashtable);
			}
			else
			{
				SRView.displayTemplateFile( new PrintWriter(out),
					lLoginFormName,
					null );
			}
		}
		catch( SRException se )
		{
			nie.sr.SRConfig.doErrorMsg( "login.jsp", "main", "" + se );
			SRView.internalError( new PrintWriter(out) );
		}
	}
%>
