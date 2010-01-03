package nie.sr;

import java.sql.*;
import java.io.*;
//import nie.sr.util.*;

public class controllerAdmin extends SRController
{
    public controllerAdmin( modelAdmin inAdmin ) throws SRException
    {
	super( inAdmin );
    }

    private modelAdmin getAdminModel() throws SRException
    {
	modelAdmin retModel = (modelAdmin)getModel();
	if( retModel == null )
	    throw new SRException( "the model was null" );
	return retModel;
    }

    public void process()
    {
	try
	{
	    modelAdmin lModel = getAdminModel();
	    SRConfig lConfig = SRConfig.getSoleInstance();
	    fConnection = lConfig.getConnection();
	    if( fConnection != null )
		fStatement = fConnection.createStatement();

	    String lFunctionCode = lModel.getFunctionCode();
	    if( lFunctionCode == null )
	    {
		SRView.internalError( lModel.getView().getWriter() );
		return;
	    }

	    lFunctionCode = lFunctionCode.toLowerCase();
	    if( lFunctionCode.compareTo("add_user") == 0 ) { addUser( lModel ); }
	    else if( lFunctionCode.compareTo( "delete_user") == 0 ) { deleteUser( lModel ); }
	    //	    else if( lFunctionCode.compareTo( "edit_user" ) == 0 ) { editUser( lModel ); }
	    //	    else if( lFunctionCode.compareTo( "generate_trend_reports" ) == 0 ) { generate_trend_reports( lModel ); }
	    else
		((viewAdmin)(getAdminModel().getView())).showPage();

	} catch( SRException se ) {
	} catch( SQLException sqle ) {
	}
    }

    void addUser( modelAdmin inModel )
    {
	try
	{
	    String lUserName = inModel.getUserName();
	    String lPassword = inModel.getPassword();
	    String lFullName = inModel.getFullName();
	    String lEMail    = inModel.getEMail();
	    String lPhone1   = inModel.getPhone1();
	    String lPhone2   = inModel.getPhone2();
	    String lPhone3   = inModel.getPhone3();
	    int lSecurity    = inModel.getSecurityLevel();

	    String lSQL = "SELECT max(user_id) FROM userinfo";
	
	    ResultSet lResultSet = fStatement.executeQuery( lSQL );
	    lResultSet.next();
	    int lUserID = lResultSet.getInt( "user_id" );

	    if( (lUserName == null) || (lUserName == "") ||
		(lPassword == null) || (lPassword == "") ||
		(lFullName == null) || (lFullName == "") )
		return;

	    if( lEMail == null ) lEMail = "";
	    if( lPhone1 == null ) lPhone1 = "";
	    if( lPhone2 == null ) lPhone2 = "";
	    if( lPhone3 == null ) lPhone3 = "";

	    lSQL = "INSERT INTO userinfo (user_id, " +
		"user_name, " +
		"password, " +
		"full_name, " +
		"email, " +
		"phone_1, " +
		"phone_2, " +
		"phone_3, " +
		"security_level)" +
		"VALUES ( " + (lUserID + 1) + ", " +
		lUserName + ", " +
		lPassword + ", " +
		lFullName + ", " +
		lEMail + ", " +
		lPhone1 + ", " +
		lPhone2 + ", " +
		lPhone3 + ", " +
		lSecurity + ")";
	    fStatement.executeUpdate( lSQL );
	    inModel.getView().doRedirect( "admin.jsp" );
	} catch( SRException sre ) {
	} catch( SQLException sqle ) {
	}
    }

    void deleteUser( modelAdmin inModel ) throws SRException
    {
	int lUserID = inModel.getUserID();
	String lUserName = inModel.getUserName();
	try
	{
	    if( lUserID != 0 )
		fStatement.executeUpdate( "DELETE FROM userinfo WHERE user_id = " + lUserID );
	    else if( (lUserName != null) && (lUserName != "") )
		fStatement.executeUpdate( "DELETE FROM userinfo WHERE user_name='" + lUserName + "'" );

	    inModel.getView().doRedirect( "admin.jsp" );
	} catch( SQLException sqle ) {
	} catch( SRException sre ) {
	}

	SRView.internalError( inModel.getView().getWriter() );
    }

    void editUser( modelAdmin inModel )
    {
	boolean lClause = false;
	String lSQL = "UPDATE user_info SET ";

	if( inModel.getSecurityLevel() >= -1 )
	{
	    int lSecurity = inModel.getSecurityLevel();
	    if( lSecurity >= inModel.getUserInfo().getSecurityLevel() )
		lSecurity = inModel.getUserInfo().getSecurityLevel() - 1;
	    lSQL +="security_level=" + lSecurity;
	    lClause = true;
	}

	String lString = inModel.getUserName();
	if( (lString != null) && (lString != "") )
	{
	    if( lClause )
		lSQL += ", ";
	    lSQL += "user_name='" + lString + "'";
	    lClause = true;
	}

	lString = inModel.getFullName();
	if( (lString != null) && (lString != "") )
	{
	    if( lClause )
		lSQL += ", ";
	    lSQL += "full_name='" + lString + "'";
	    lClause = true;
	}

	lString = inModel.getEMail();
	if( (lString != null) && (lString != "") )
	{
	    if( lClause )
		lSQL += ", ";
	    lSQL += "email='" + lString + "'";
	    lClause = true;
	}

	lString = inModel.getPhone1();
	if( (lString != null) && (lString != "") )
	{
	    if( lClause )
		lSQL += ", ";
	    lSQL += "phone1='" + lString + "'";
	    lClause = true;
	}

	lString = inModel.getPhone2();
	if( (lString != null) && (lString != "") )
	{
	    if( lClause )
		lSQL += ", ";
	    lSQL += "phone2='" + lString + "'";
	    lClause = true;
	}

	lString = inModel.getPhone3();
	if( (lString != null) && (lString != "") )
	{
	    if( lClause )
		lSQL += ", ";
	    lSQL += "phone3='" + lString + "'";
	    lClause = true;
	}

	lString = inModel.getPhone1();
	if( (lString != null) && (lString != "") )
	{
	    if( lClause )
		lSQL += ", ";
	    lSQL += "phone1='" + lString + "'";
	    lClause = true;
	}

    }

    void generate_trend_reports( modelAdmin inModel )
    {
	/*	try
	{
	    File lFile = new File( "html/dailyTrendReport.html" );
	    lFile.delete();
	    lFile.createNewFile();
	    PrintStream lPrintStream = new PrintStream( new FileOutputStream( lFile ) );

	    trendReport lTrendReport = new trendReport();
	    lTrendReport.setConfig( SRConfig.getSoleInstance() );
	    lTrendReport.setInterval( 1 );
	    lTrendReport.process( lPrintStream );

	    lTrendReport.setInterval( 7 );
	    lPrintStream.close();

	    lFile = new File( "html/weeklyTrendReport.xml" );
	    lFile.delete();
	    lFile.createNewFile();
	    lPrintStream = new PrintStream( new FileOutputStream( lFile ) );

	    lTrendReport.setInterval( 7 );
	    lTrendReport.process( lPrintStream );
	    lPrintStream.close();

	    lFile = new File( "html/weeklyTrendReport.xml" );
	    lFile.delete();
	    lFile.createNewFile();
	    lPrintStream = new PrintStream( new FileOutputStream( lFile ) );
	    lTrendReport.setInterval( 30 );
	    lTrendReport.process( lPrintStream );
	    lPrintStream.close();
	} catch( IOException ioe ) {
	}

	try {
	    getAdminModel().getView().doRedirect( "main.jsp" );
	} catch( Exception e ) {
	}*/
    }

    Connection fConnection;
    Statement fStatement;
}
