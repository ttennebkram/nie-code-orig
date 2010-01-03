package nie.sr2.util;

import java.sql.*;
import java.util.*;
import java.net.*;
import nie.core.*;
import nie.sn.SearchTuningConfig;

abstract public class ReportCacherBase implements nie.sn.CronLiteJob // Runnable
{

	private final static String kClassName = "ReportCacherBase";

	static final long _MY_INTERVAL = nie.sn.CronLite.MINUTE; // nie.sn.CronLite.THREE_HOURS;

	// How often to run
	public long _getRunIntervalInMS() {
		return _MY_INTERVAL;
	}
	abstract public long getRunIntervalInMS();
	abstract public String getReportName();

	// Example: nie.sr2.ReportConstants.DAYS_OLD_CGI_FIELD_NAME
	abstract public String getFilterName();

	abstract public String getFilterValue();

	void runReport()
		throws Exception
	{
		final String kFName = "runReport";
		final String kExTag = kClassName + '.' + kFName + ": ";
		nie.sr2.ReportDispatcher disp =
			getMainConfig().getReportDispatcher();
		if( null==disp )
			throw new Exception( kExTag + "Null Report Dispatcer" );
		// Pass variables to report dispatcher via a simulated request
		// A sumulated input request
		AuxIOInfo simInRequest = new AuxIOInfo();
		// The name of the report we want
		simInRequest.setOrOverwriteCGIField(
			nie.sr2.ReportConstants.REPORT_NAME_CGI_FIELD,
			getReportName()
			);

		// Add the password, and update the request object
		String password = getMainConfig().getAdminPwd();
		// simInRequest.setOrOverwriteCGIField( nie.sn.SnRequestHandler.ADMIN_CGI_PWD_FIELD, password );
		// int accessLevel = getMainConfig().passwordToAccessLevel( password );
		// July 2008: we work with tokens now
		String token = getMainConfig().passwordToKeyOrNull( password );
		simInRequest.setOrOverwriteCGIField( nie.sn.SnRequestHandler.ADMIN_CGI_PWD_OBSCURED_FIELD, token );
		int accessLevel = getMainConfig().tokenToAccessLevel( token, true );
		simInRequest.setAccessLevel( accessLevel );

		// Add parameters, if we have any
		if( null!=getFilterName() && null!=getFilterValue() ) {
			simInRequest.setOrOverwriteCGIField(
				nie.sr2.ReportConstants.FILTER_NAME_CGI_FIELD_NAME,
				getFilterName()
				);
	
			simInRequest.setOrOverwriteCGIField(
				getFilterName(),
				getFilterValue()
				);
		}

		// Simulated output, not really used
		AuxIOInfo simOutRequest = new AuxIOInfo();

		// Get the report
		String tmpReport = getMainConfig().getReportDispatcher().dispatch(
			simInRequest, simOutRequest
			);

		// statusMsg( kFName, "Report=\n" + tmpReport );


	}

    ////////////////////////////////////////////////
    //
    // Constructors.
    //
    // All constructors should call commonInit()
    //
    ////////////////////////////////////////////////
    
    public ReportCacherBase()
    //	throws DNSException
    {
		// commonInit();
		// ^^^ moved to AFTER we parsed the command line, so
		// we have a config file to use

    }

	public ReportCacherBase( SearchTuningConfig inMainConfig )
	{
		this();
		setMainConfig( inMainConfig );
	}

	public boolean hadError() {
		return mHadError;
	}


    public void run()
    {
    	final String kFName = "run";
		mHadError = false;

		try
		{
			runReport();
		}
		catch( Exception e )
		{
			stackTrace( kFName, e, "General Exception Caught" );
			mHadError = true;
		}
    }
    

	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	// This gets us essentially the same thing, but casted
	// to let us to implementation specific things like parse
	// command line options
	private static RunLogBasicImpl getRunLogImplObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogImplObject();
	}

	protected boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName, inFromRoutine,
			e, optMessage
			);
	}

	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName, inFromRoutine );
	}

	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
	}

	private static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private static boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName, inFromRoutine );
	}

	private static boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( kClassName, inFromRoutine );
	}

	private static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}



	public void setMainConfig( SearchTuningConfig inMainConfig ) {
		mMainConfig = inMainConfig;
	}
	SearchTuningConfig getMainConfig() {
		return mMainConfig;
	}

    
    ///////////////////////////////////////////////////////////
    //
    // Private members...
    //
    ///////////////////////////////////////////////////////////

	SearchTuningConfig mMainConfig;

	boolean mHadError;
	String _mErrorMsg;

    
}
