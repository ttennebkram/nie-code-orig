package nie.core;

import java.io.*;
import java.util.*;

// An interface for writting loggers for our application

public interface RunLogInterface
{

	// *** !!! IMPORTANT Implementor Note !!! ***
	// =====================================================
	// There is one more method that an implementation
	// should create, with a suggested signature of:
	//
	//      public static RunLogInterface getRunLogObject();
	//
	// You implemenation should return an a valid INSTNACE of your
	// logger class when this static method is called.
	// Presumably you would store that singleton instance as a
	// STATIC member variable and only instantiate once when you first
	// notice that it is null.
	//
	//
	// I can't do it here because static methods are not allowed in
	// interfaces; since static methods can be overridden, they would
	// odd to allow in interface files.

	// Controlling verbosity
	public boolean setVerbosity( int inLevel );
	public boolean setVerbosityForClass( String inClassName, int inLevel );
	public boolean setVerbosityForMethod(
		String inClassName, String inMethodName, int inLevel
		);

	// Where the data goes
	// False if it doesn't work
	public boolean setOutput( OutputStream inNewDestination );

	// Get any messages that have been cached
	// returns null if the class doesn't implement it
	public List getMessages();

	public boolean setOutput( PrintStream inNewDestination );
	// Should at least accept file names
	public boolean setOutputURI( String inNewDestinationURI );
	// A hook for folks wanting to extend this, they can cast the object
	// to whatever they want
	public boolean setOutputURI( String inNewDestinationURI,
		Object inAuxOutputOptions
		);


	/////////////////////////////////////////////////////////////////
	//
	//  Actual Message Output
	//  Will be thrown away if logging doesn't warrent it
	//
	//////////////////////////////////////////////////////////////////

	// Actual message output
	public boolean statusMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		);
	public boolean transactionStatusMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		);
	public boolean infoMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		);
	public boolean debugMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		);
	public boolean traceMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		);

	public boolean warningMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		);
	public boolean errorMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		);
	public boolean fatalErrorMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		);

	public boolean stackTrace(
		String inFromClass, String inFromMethod,
		Exception e, String optMessage
		);

	public boolean genericMsg(
		String inMessageHeader,
		String inFromClass, String inFromMethod,
		String inMessage,
		boolean inDoTimeStamp
		);


	/////////////////////////////////////////////////////////////////
	//
	//  Check if a message would even be logged.
	//  Useful if preparing messages would be an expensive operation
	//
	//////////////////////////////////////////////////////////////////


	// Actual message output
	public boolean shouldDoStatusMsg(
		String inFromClass, String inFromMethod
		);
	public boolean shouldDoTransactionStatusMsg(
		String inFromClass, String inFromMethod
		);
	public boolean shouldDoInfoMsg( String inFromClass, String inFromMethod );
	public boolean shouldDoDebugMsg( String inFromClass, String inFromMethod );
	public boolean shouldDoTraceMsg( String inFromClass, String inFromMethod );

	public boolean shouldDoWarningMsg( String inFromClass, String inFromMethod );
	public boolean shouldDoErrorMsg( String inFromClass, String inFromMethod );
//	public boolean doFatalErrorMsg(
//		String inFromClass, String inFromRoutine,
//		String inMessage
//		);




	// Useful if the calling class would have to do a lot of
	// work to prepare a message that would not even be displayed
	public boolean shouldLog(
		int inMessageLevel, String inFromClass, String inFromMethod
		);




	// Verbosity levels

	// A return code for when we don't recognize something
	// It's use is not strictly dictated by this interface
	public static final int VERBOSITY_VALUE_NOT_RECOGNIZED = -10;

	// Let us decide
	public static final int VERBOSITY_USE_DEFAULT = -2;
	// Be quiet except for fatal errors, which we don't suggest
	public static final int VERBOSITY_FATAL_ONLY = -1;
	// !!! We don't use zero intentionally
	// Virtually: VERBOSITY_UNASSIGNED = 0;
	// If we accidently get a zero then it defaults to Default
	// Be quiet (except for warnings and errors)
	public static final int VERBOSITY_QUIET = 1;
	// Tell user about overall starting and stopping of processes and
	// major threads.
	// Don't pester them about threads for individual transactions
	public static final int VERBOSITY_STATUS_PROCESS = 2;
	// private static final String VNAME_MIN = "Status";
	public static final int VERBOSITY_STATUS_TRANSACTIONS = 3;
	// private static final String VNAME_MIN = "Status";
	public static final int VERBOSITY_DETAILED_INFO = 4;
	// private static final String VNAME_CHAT = "Info";
	public static final int VERBOSITY_DEBUG = 5;
	// private static final String VNAME_DEBUG = "Debug";
	// Really detailed info about internal machinery
	public static final int VERBOSITY_TRACE = 6;
	// You wouldn't really use the rest as Verbosity settings, but
	// a message could be at that level
	public static final int LEVEL_WARNING = 7;
	public static final int LEVEL_ERROR = 8;
	public static final int LEVEL_FATAL_ERROR = 9;
	public static final int LEVEL_GENERIC = 50;
	// private static final String VNAME_DEBUG = "Trace";
	public static final int DEFAULT_VERBOSITY = VERBOSITY_STATUS_TRANSACTIONS;

	// What everbody should use for a newline, if they want to ask us
	public static final String NL = "\r\n";

}
