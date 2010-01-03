<% /*@ page
	* language="java"
	* contentType="text/html"
	* errorPage="mainErrorPage.jsp"
	*/
%><%@ page import="nie.sr.*, java.io.*, java.util.*"
%><%
        /* =============================================================
         * Copyright 2002 by New Idea Engineering.
         * Written by Kevin-Neil Klop
         *
         * main display page for the reporting module.
         *
         **************************************
         *
         * $id:$
         *
         * $Log: admin.jsp,v $
         * Revision 1.1  2002/10/15 00:32:53  kklop
         * Initial revision
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
/><jsp:useBean id="lPageInfo" scope="page" class="nie.sr.adminMain"
/><jsp:setProperty name="lPageInfo" property="*"
/><%
	final String kLoginPageURL = "login.jsp";

	if( gConfig == null )
	{
		nie.sr.SRConfig.doErrorMsg( "admin.jsp", "main", "gConfig is null" );
		SRView.internalError( new PrintWriter( out ) );
		return;
	}

	if( ! gConfig.isLoaded() )
	{
		nie.sr.SRConfig.doErrorMsg( "admin.jsp", "main", "gConfig was not loaded." );
		response.sendRedirect( kLoginPageURL );
	}

	if( ! gUserInfo.isLoaded() )
	{
		nie.sr.SRConfig.doErrorMsg( "admin.jsp", "main", "gUserInfo no loaded." );
		response.sendRedirect( kLoginPageURL );
	}

	if( gUserInfo.getSecurityLevel() < nie.sr.SRUserInfo.kAdministratorSecurityLevel )
	{
		response.sendRedirect( kLoingPageURL );
	}

	if( !lPageInfo.isLoaded() )
	{
		SRView.displayTemplateFile( new PrintWriter( out ),
			kAdminFormName,
			null );
	} else {
		try
		{
			controllerAdmin lController = new controllerAdmin( lPageInfo );
			viewAdmin lView = new viewAdmin( lPageInfo );
			lPageInfo.setView( lView );
			lPageInfo.setController( lController );
			lPageInfo.setUserInfo( gUserInfo );

			lView.setWriter( new PrintWriter( out ) );
			lView.setResponse( response );

			lController.process();
		} catch( SRException se ) {
			SRView.internalError( new PrintWriter( out ));
			nie.sr.SRConfig.doErrorMsg( "main.jsp", "main", "" + se + "" );
		}
	}
%>
