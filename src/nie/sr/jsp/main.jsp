<%@ page
	language="java"
	contentType="text/html"
	errorPage="ErrorPage.jsp"
%><%@ page import="nie.sr.*, java.io.*, java.util.*"
%><%
        /* =============================================================
         * Copyright 2002, 2003 by New Idea Engineering.
         * Written by Kevin-Neil Klop and Mark Bennett
         *
         * main display page for the reporting module.
         *
         **************************************
         *
         * $id:$
         *
         * $Log: main.jsp,v $
         * Revision 1.6  2002/11/13 00:23:15  kklop
         * Fixed the "next link" bug.
         *
         * Revision 1.5  2002/11/09 17:40:20  kklop
         * Almost ready for general release
         *
         * Revision 1.4  2002/10/15 00:32:53  kklop
         * First release (beta) version.
         *
         * Revision 1.3  2002/10/13 15:31:41  kklop
         * Trend reports now working, utilities added.
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
%><jsp:useBean id="gConfig" scope="application" class="nie.sr.SRConfig"
/><jsp:useBean id="gUserInfo" scope="session" class="nie.sr.SRUserInfo"
/><jsp:useBean id="lPageInfo" scope="page" class="nie.sr.modelMain"
/><jsp:setProperty name="lPageInfo" property="*"
/><%

	System.err.println( "hello b" );

	final String kMainFormName = "html" + File.separator + "main.html";
	final String kUnavailablePageURL = "html" + File.separator + "unavailable.html";
	final String kLoginPageURL = "login.jsp";
	final String kTrendReportMarker = "<trend_report>";
	final String kAdditionalFunctionsMarker = "<additional_function_codes>";
	final String kAdditionalFunctions = "<a href=\"admin.jsp\">Administration</a>";
	final String[] kTrendReportFileNames =
		{
			"html/dailyTrendReport.html",
			"html/weeklyTrendReport.html",
			"html/monthlyTrendReport.html"
		};

	// If no config, internal error
	if( gConfig == null )
	{
		nie.sr.SRConfig.doErrorMsg( "main.jsp", "main", "gConfig is null" );
		SRView.internalError( new PrintWriter(out) );
		return;
	}

	// If config is not finished loading, goto login
	if( ! gConfig.isLoaded() )
	{
		nie.sr.SRConfig.doErrorMsg( "main.jsp", "main", "gConfig was not loaded." );
		response.sendRedirect( kLoginPageURL );
	}

	// If user is not logged in, goto login
	if( !gUserInfo.isLoaded() )
	{
		nie.sr.SRConfig.doErrorMsg( "main.jsp", "main", "gUserInfo not loaded." );
		response.sendRedirect( kLoginPageURL );
	}

	// If the user has no security, goto login
	if( gUserInfo.getSecurityLevel() < 0 )
		response.sendRedirect( kLoginPageURL );

	// Figure out what we're here to do
	String lFunctionCode = lPageInfo.getFunctionCode();

	// If we have a function code
	if( lFunctionCode != null )
	{
		// Trend reports
		if( lFunctionCode.toLowerCase().compareTo( "trend" ) == 0 )
		{
			out.println( "<!-- trend report modification -->" );
			if( lPageInfo.getReportCode() != null )
			{
				out.println( "<!-- getReportCode returned " + lPageInfo.getReportCode() + " -->" );
				gUserInfo.setWhichTrendReport( Integer.parseInt( lPageInfo.getReportCode() ) );
				lPageInfo.setReportCode( null );
				lPageInfo.setFunctionCode( null );
			}
		}
	}	// End if we have a function code

	// If the page is loaded
	// MAIN PAGE LOGIC
	if( lPageInfo.isLoaded() )
	{
		out.println( "<!-- lPageInfo.isLoaded() returned true. -->" );
		/*****************************************************
		 * Theoretically we have all the information from the
		 * page that we need.  Go ahead and process the page.
		 ****************************************************/

		try
		{
			// get a "controller"
			controllerMain lController = new controllerMain( lPageInfo );
			// Request the main page
			viewMain lView = new viewMain( lPageInfo );

			// Wire up the view, controller and user info			
			lPageInfo.setView( lView );
			lPageInfo.setController( lController );
			lPageInfo.setUserInfo( gUserInfo );
				 
			lView.setWriter( new PrintWriter(out) );
			lView.setResponse( response );

			// Do it
			lController.process();

		}
		catch( SRException se )
		{
			SRView.internalError( new PrintWriter(out) );
			nie.sr.SRConfig.doErrorMsg( "main.jsp", "main", "" + se + "" );
		}
	}
	// Else the page is not loaded
	else
	{

		out.println( "<!-- lPageInfo.isLoaded() returned false. -->" );

		/*****************************************************
		 * We're still missing information from the page that
		 * we need.  Redisplay the form and ask for all the
		 * information again.
		 ****************************************************/
		
		Hashtable lHashtable = new Hashtable();
		lHashtable.put( kTrendReportMarker, "Trend Report Goes here" );

		/*****************************************************
		 * Create the field fill values in the hash table
		 ****************************************************/

		// Administrative function - only available if administrator or higher level

		if( gUserInfo.getSecurityLevel() < gUserInfo.kAdministratorSecurityLevel )
			lHashtable.put( kAdditionalFunctionsMarker, "" );
		else
			lHashtable.put( kAdditionalFunctionsMarker, kAdditionalFunctions );

		// Trend report - get which one we should display from the user information

		String lTrendReportFileName;
		int lTrendReportNumber = gUserInfo.getWhichTrendReport();
		if( (lTrendReportNumber >= nie.sr.SRUserInfo.kMinTrendReportNumber) &&
		    (lTrendReportNumber <= nie.sr.SRUserInfo.kMaxTrendReportNumber) )
		{
			lTrendReportFileName = kTrendReportFileNames[ lTrendReportNumber ];
		} else {
			nie.sr.SRConfig.doDebugMsg( "main.jsp", "main", "Trend Report Number " + lTrendReportNumber + " out of range." );
			SRView.internalError( new PrintWriter(out) );
			return;
		}

		// Read in the trend report

		try
		{
			gConfig.doDebugMsg( "main.jsp", "main", "Processing trend report " + lTrendReportFileName );
			File lFile = new File( lTrendReportFileName );
			gConfig.doDebugMsg( "main.jsp", "main", "Path to trend report is '" + lFile.getAbsolutePath() + "'" );

			if( lFile.exists() )
			{
				FileInputStream lInStream = new FileInputStream( lFile );
				byte[] lBytes = new byte[(int)lFile.length()];
				lInStream.read( lBytes );
				String lTrendReport = new String( lBytes );
				lHashtable.put( kTrendReportMarker, lTrendReport );
			} else {
				gConfig.doErrorMsg( "main.jsp", "main", "could not find trend report " + lTrendReportFileName );
				lHashtable.put( kTrendReportMarker, "<strong>Trend Report Not Found</strong>" );
			}

			SRView.displayTemplateFile( new PrintWriter(out),
							kMainFormName,
							lHashtable);
		} catch( SRException se ) {
			SRView.internalError( new PrintWriter(out) );
			nie.sr.SRConfig.doErrorMsg( "main.jsp", "main", "" + se );
		}
	}	// End else page is NOT loaded

%>
