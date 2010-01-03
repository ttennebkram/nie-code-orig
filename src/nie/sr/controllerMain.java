package nie.sr;

import java.sql.*;
import java.util.*;
import java.lang.*;
import java.lang.reflect.*;

public class controllerMain extends SRController
{
    /************************************************************
     * Constructor
     ************************************************************/
    
    public controllerMain( modelMain inModel ) throws SRException
    {
	super( inModel );
    }
    
    /************************************************************
     * Utility routine so that I don't have to keep casting the getModel()
     * call from SRController
     ************************************************************/
    
    modelMain getMainModel() throws SRException
    {
	modelMain lModel = (modelMain)getModel();
	if( lModel == null )
	    throw new SRException( "getModel() returned null in controllerMain.process()" );
	return lModel;
    }
    
    /************************************************************
     * Main processing entry point for the controller.
     ************************************************************/
    
    public void process() throws SRException
    {
	/*************************************
	 * Process the request - theoretically
	 * we have all the information that we
	 * need.
	 ************************************/

	modelMain lModel = getMainModel();
	viewMain lView = lModel.getMainView();

	if( lModel.isLoaded() )
	{
	    String lFunctionCode = lModel.getFunctionCode();
	    if( lFunctionCode != null )
	    {
		lFunctionCode = lFunctionCode.toLowerCase();
		if( lFunctionCode.compareTo( "logout") == 0 ) {
		    doLogout();
		} else if( lFunctionCode.compareTo( "report" ) == 0 ) {
		    doReport();
		} else if( lFunctionCode.compareTo( "shutdown" ) == 0 ) {
		    doShutdown();
		} else if( lFunctionCode.compareTo( "user_admin" ) == 0 ) {
		    doUserAdmin();
		} else {
		    doUnknownFunction();
		}
	    } else if( (lModel.getReportCode() != null) &&
		       (lModel.getReportCode().compareTo("") != 0 ) ) {
		doReport();
	    } else {
		SRView.internalError( lView.getWriter() );
		doErrorMsg( "process",
			    "Neither Function Code nor Report Code passed in but modelMain.isLoaded() returned true." );
		return;
	    }
	}
	return;

    };

    /***********************************************************
     ***********************************************************
     **
     ** Log the user off the system
     **
     ***********************************************************
     **********************************************************/

    void doLogout() throws SRException, SRException, SRException
    {
	modelMain lModel = getMainModel();
	SRUserInfo lUserInfo = lModel.getUserInfo();
	lUserInfo.setUserID(0);
	lUserInfo.setUserName( null );
	lUserInfo.setFullName( null );
	lUserInfo.setSecurityLevel( 0 );

	// We should also destroy the session here as well, but that can wait for another day.

	viewMain lView = (viewMain)lModel.getView();
	lView.doRedirect( kLoginPage );
    }

    /***********************************************************
     ***********************************************************
     **
     ** Perform a report
     **
     ***********************************************************
     **********************************************************/

    void doReport() throws SRException
    {
	modelMain lModel = getMainModel();
	SRReport lReport = instantiateReport( lModel.getReportCode() );
	SRFilter lFilter = instantiateFilter( lModel.getFilterCode() );

	lReport.process( lFilter );
    }

    SRReport instantiateReport( String inReportID ) throws SRException
    {
	String lReportClassName = SRConfig.getConfigInstance().getClassNameForReportID( inReportID );
	doDebugMsg( "instantiateReport", "received report class name '" + lReportClassName + "' for report id '" + inReportID + "'" );
	return (SRReport)dynamicInstantiation( lReportClassName );
    }

    SRFilter instantiateFilter( String inFilterID ) throws SRException
    {
	String lFilterClassName = SRConfig.getConfigInstance().getClassNameForFilterID( inFilterID );
	doDebugMsg( "instantiateFilter", "received filter class name '" + lFilterClassName + "' for filter id '" + inFilterID + "'" );
	return (SRFilter)dynamicInstantiation( lFilterClassName );
    }

    Object dynamicInstantiation( String inClassName ) throws SRException
    {
	if( inClassName == null )
	{
	    doErrorMsg( "dynamicInstantiation", "class name passed in is null." );
	    throw new SRException("Class name passed to dynamicInstantiation is null." );
	}

	modelMain lModel = getMainModel();
	viewMain lView = lModel.getMainView();

	try {
	    Class lClass = Class.forName( inClassName );
	    Class[] lConstructorSignature = new Class[1];
	    lConstructorSignature[0] = Class.forName( "nie.sr.modelMain" );
	    
	    Constructor lConstructor = lClass.getConstructor( lConstructorSignature );
	    Object[] lConstructorArguments = new Object[1];
	    lConstructorArguments[0] = lModel;
	    
	    Object lObject = lConstructor.newInstance( lConstructorArguments );
	    return lObject;
	} catch( ClassNotFoundException cnfe ) {
	    String lErrorMessage = "Could not find the specified class \"" + inClassName + "\" message was: " + cnfe;
	    doErrorMsg( "process",
			lErrorMessage );
	    SRView.internalError( lView.getWriter() );
	    throw new SRException( cnfe );
	} catch( NoSuchMethodException nsme ) {
	    doErrorMsg( "process",
			"Could not find the proper constructor for class \"" + inClassName + "\" message was: " + nsme );
	    SRView.internalError( lView.getWriter() );
	    throw new SRException( nsme );
	} catch( InstantiationException ie ) {
	    doErrorMsg( "process",
			"Could not instantiate class \"" + inClassName + "\" message was: " + ie );
	    SRView.internalError( lView.getWriter() );
	    throw new SRException( ie );
	} catch( IllegalAccessException iae ) {
	    doErrorMsg( "process",
			"Could not access class \"" + inClassName + "\" message was: " + iae );
	    SRView.internalError( lView.getWriter() );
	    throw new SRException( iae );
	} catch( InvocationTargetException ite ) {
	    doErrorMsg( "process",
			"Could not invoke new on  class \"" + inClassName + "\" message was: " + ite );
	    SRView.internalError( lView.getWriter() );
	    throw new SRException( ite );
	}
    }

    /***********************************************************
     ***********************************************************
     **
     ** Shutdown the system - currently doesn't actually do
     ** anything.
     **
     ***********************************************************
     **********************************************************/

    void doShutdown()
    {
    }

    /***********************************************************
    ************************************************************
    **
    ** doUserAdmin - administer users (i.e. add, delete, etc.)
    **
    ************************************************************
    ***********************************************************/

    void doUserAdmin()
    {
    }

    /***********************************************************
     ***********************************************************
     **
     ** doUnknownFunction - we had a function code come in
     ** without an assigned class - even the default algorithm
     ** didn't come up with a viable class name.
     **
     ***********************************************************
     **********************************************************/

    void doUnknownFunction()
    {
    }

    /***********************************************************
     * Log an error message to the system log
     **********************************************************/
    
    static void doErrorMsg( String inMethodName, String inMessage )
    {
		SRConfig.doErrorMsg( "controllerMain", inMethodName, inMessage );
    }
    
    static void doDebugMsg( String inMethodName, String inMessage )
    {
		SRConfig.doDebugMsg( "controllerMain", inMethodName, inMessage );
    }
    
    /***********************************************************
     ***********************************************************
     **
     ** Instance variables
     **
     ***********************************************************
     **********************************************************/

    //    static final String kLoginPage = "html" + java.io.File.separator + "login.html";
    static final String kLoginPage = "login.jsp";
}
