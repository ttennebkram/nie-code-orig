package nie.core;

import java.io.*;
import java.util.*;

import org.jdom.*;

// An interface for writting loggers for our application

public class RunLogBasicImpl implements RunLogInterface
{

	// static final String kClassName = "RunLogBasicImpl";
	// A friendlier name
	static final String kClassName = "RunLog";

	// The singleton instance
	private static RunLogInterface mRunLogObject;

	// Default constructor
	public RunLogBasicImpl()
	{
		// How many messages to keep, and their cache
		// put in each constructor
		// initKeepMessageCacheAndCount();


		// Take over the singleton instance
		// System.err.println( "in cons1, saving " + this );
		mRunLogObject = this;
	}

	public RunLogBasicImpl( String inURI )
		throws Exception
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// How many messages to keep, and their cache
		// put in each constructor
		// initKeepMessageCacheAndCount();


		// create jdom element and store info
		// Sanity checks
		if( inURI == null )
			throw new DBConfigException( kExTag,
				"Constructor was passed in a NULL URI (file name, url, etc)."
				);

		// Instantiate and store the main JDOMHelper a
		fConfigTree = null;
		try
		{
			fConfigTree = new JDOMHelper( inURI );
		}
		catch (JDOMHelperException e)
		{
			throw new Exception( kExTag
				+ "Got JDOMHelper Exception: "
				+ e );
		}

		// Do comoon init stuff, it will throw an exception if it
		// isn't happy
		finishInit();

	}

	// A constructor that takes a jdom node tree as an argument
	// This will OVERWRITE any singlton instance that might be in place
	public RunLogBasicImpl( JDOMHelper inNode )
		throws Exception
	{
		// How many messages to keep, and their cache
		// put in each constructor
		// initKeepMessageCacheAndCount();


		final String kFName = "constructor(3)";
		final String kExTag = kClassName + ':' + kFName + ": ";
		if( inNode == null )
		{
			String tmpMsg = "Was passed a null JDOMHelper node.";
			errorMsg( kClassName, kFName, tmpMsg );
			throw new Exception( kExTag + tmpMsg );
		}
		// Store our config data
		fConfigTree = inNode;
		// Finish the init process
		finishInit();
	}

	// NOT called by default constructor, though it does set the global
	// instance
	// Note: It's a little bit confusing:
	// If I set verbosity on the command line and in the config file,
	// the command line items will be set first, then the config file will
	// for a 2nd config.
	// But command line should OVERRIDE config items.
	// So in this routine, if we're changing from an old to a new config, we
	// carefully preserve some of the older values, allowing them to "override"
	// some of the newer values.
	private void finishInit()
		throws Exception
	{
		final String kFName = "finishInit";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// How many messages to keep, and their cache
		initKeepMessageCount();

		// If there's already an instance, let them know what's going on
		boolean hadExistingObject = false;
		if( mRunLogObject != null )
		{
			hadExistingObject = true;
			mRunLogObject.debugMsg( kClassName, kFName,
				"Constructing new run log object, will replace this object."
				);
			// Copy over old messages
			List msgs = mRunLogObject.getMessages();
			if( null != msgs )
			{
				// Walk backards, adding the oldest first
				for( int i=msgs.size()-1; i>=0; i-- )
				{
					String msg = (String)msgs.get(i);
					pushMsg( msg );
				}
			}
			// Copy over old Verbosity settings
			try {
				RunLogBasicImpl tmp = (RunLogBasicImpl)mRunLogObject;
				this.mVerbosityByName = tmp.mVerbosityByName;
			}
			catch( Exception e ) {
				// Actually don't worry about it too much
				mRunLogObject.debugMsg( kClassName, kFName,
					"Exception copying over old verbosity hash: " + e
					);
			}
		}


		// Propogate any append or overwrite policies
		initOverwritePolicyFromConfig();

		// Set any location
		String loc = getLogLocationFromConfig();
		if( loc != null )
		{
//			if( mRunLogObject != null )
//			{
//				mRunLogObject.statusMsg( kClassName, kFName,
//					"Output moving to \"" + loc + "\"."
//					);
//			}
			// Set it in the new instance
			setOutputURI( loc );
		}

		// Set any verbosity

		// We may retain the old verbosity, if any
		boolean keepOld = false;
		if( hadExistingObject ) {
			int oldVerb = 0;
			// Copy over old Verbosity settings
			try {
				RunLogBasicImpl tmp = (RunLogBasicImpl)mRunLogObject;
				oldVerb = tmp.mVerbosity;
			}
			catch( Exception e ) {
				// Actually don't worry about it too much
				mRunLogObject.debugMsg( kClassName, kFName,
					"Exception copying over old verbosity int: " + e
					);
			}
			if( 0!=oldVerb && RunLogInterface.DEFAULT_VERBOSITY != oldVerb ) {
				mVerbosity = oldVerb;
				keepOld = true;
			}
		}
		// OK, no old, see if there's a new one to use
		if( ! keepOld ) {
			String verbosityStr = getVerosityStringFromConfig();
			if( verbosityStr != null )
				// the method we're calling will handle all failure cases
				setVerbosityByString( verbosityStr );
		}



		// Get any other Verbosity items
		// We don't generate errors or warnings here as that would be
		// a bit messy
		List verbosityTags = getVerosityElementsFromConfig();
		if( null!=verbosityTags && ! verbosityTags.isEmpty() ) {
			for( Iterator it = verbosityTags.iterator() ; it.hasNext() ; ) {
				Element vElem = (Element) it.next();
				String className = JDOMHelper.getStringFromAttributeTrimOrNull( vElem, VERBOSITY_CLASS_ATTR );
				if( null==className )
					continue;
				String methodName = JDOMHelper.getStringFromAttributeTrimOrNull( vElem, VERBOSITY_METHOD_ATTR );
				String levelStr = JDOMHelper.getStringFromAttributeTrimOrNull( vElem, VERBOSITY_LEVEL_ATTR );
				int level = decodeStringToLevelInt( levelStr, false );
				// Use default if something is wrong
				if( 	0==level
						|| RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED==level
						|| RunLogInterface.VERBOSITY_USE_DEFAULT==level
					)
					level = RunLogInterface.DEFAULT_VERBOSITY;
				setVerbosityByClassAndMethodName( className, methodName, level, ! hadExistingObject, false );
			}
		}

		// Todo: look for sub elements to control verbosity for
		// specific classes and methods

		// System.err.println( "in finish init, saving " + this );
		// Take over the singleton instance
		mRunLogObject = this;

	}


	// How to get a hold of our singleton instance
	// Folks can always call our constructor if they want to be fancy
	// To get at this, you'll need to call OUR class, not the interface's
	public static RunLogInterface getRunLogObject()
	{
		// Is this the first call?
		if( mRunLogObject == null )
			new RunLogBasicImpl();  // Sets mRunLogObject
			// mRunLogObject = new RunLogBasicImpl();
		// Return our singleton instance
		return mRunLogObject;
	}

	// Sometimes we need this specific Class instance
	public static RunLogBasicImpl getRunLogImplObject()
	{
		return (RunLogBasicImpl) getRunLogObject();
	}

	private void initOverwritePolicyFromConfig()
	{
		final String kFName = "initOverwritePolicy";
		if( fConfigTree == null )
		{
			errorMsg( kClassName, kFName,
				"Asked to init settings from config, but no config data found."
				+ " Nothing to do."
				);
			return;
		}

		// Read the append attribute from config and pass it to the setter
		setShouldAppend(
			fConfigTree.getBooleanFromAttribute(
				APPEND_ATTR, DEFAULT_SHOULD_APPEND
				)
			);

		// Read the overwrite attribute from config and pass it to the setter
		setShouldOverwrite(
			fConfigTree.getBooleanFromAttribute(
				OVERWRITE_ATTR, DEFAULT_SHOULD_OVERWRITE
				)
			);
	}

	private boolean shouldAppend()
	{
		return fShouldAppend;
	}
	public void setShouldAppend( boolean inFlag )
	{
		fShouldAppend = inFlag;
	}
	private boolean shouldOverwrite()
	{
		return fShouldOverwrite;
	}
	public void setShouldOverwrite( boolean inFlag )
	{
		fShouldOverwrite = inFlag;
	}


	private String getVerosityStringFromConfig()
	{
		if( fConfigTree == null )
			return null;
		return fConfigTree.getStringFromAttributeTrimOrNull( VERBOSITY_ATTR );
	}


	private String getLogLocationFromConfig()
	{
		if( fConfigTree == null )
			return null;
		return fConfigTree.getStringFromAttributeTrimOrNull( LOCATION_ATTR );
	}

	// Extra overloads NOT in Interface
	// Todo: tie these into some type of hash for easy maintenance
	// By default we do issue an error message for unregnized strings
	public boolean setVerbosityByString( String inLevel )
	{
		return setVerbosityByString( inLevel, true );
	}

	public boolean hasNonDefaultOutput()
	{
		final String kFName = "hasNonDefaultOutput";
		debugMsg( kFName, kFName,
			"Returning " + fHasFileOutput
			);
		return fHasFileOutput;
	}
	public boolean hasAbsolutePathOutput()
	{
		final String kFName = "hasAbsolutePathOutput";
		debugMsg( kFName, kFName, "Start." );
		// Sanity check, no path counts if it wasn't activated
		if( ! hasNonDefaultOutput() )
		{
			debugMsg( kFName, kFName, "Has default output, returning false." );
			return false;
		}
		// If no URI set, it's also false
		if( getLastLocationURI() == null )
		{
			debugMsg( kFName, kFName, "Last location is null, returning false." );
			return false;
		}

		// Create a file and check it
		String fName = getLastLocationURI();
		File theFile = new File( fName );

		// Return the answer
		boolean answer = theFile.isAbsolute();

		debugMsg( kFName, kFName,
			"Returning isAbsolute for " + fName + "=" + answer
			);

		return answer;
	}
	public String getLastLocationURI()
	{
		return fOutURI;
	}


	public boolean setVerbosityByString( String inLevel,
		boolean inOutputErrorMessages
		)
	{
		final String kFName = "setVerbosityByString";

		// See if we got back anything reasonable
		if( inLevel == null )
		{
			if( inOutputErrorMessages )
			{
				errorMsg( kClassName, kFName,
					"The new level string that was passed in was null."
					+ " Not changing verbosity level."
					+ " Returning false (failure)."
					);
			}
			return false;
		}

		// The second half is the possible class and method name qualifier
		String lCmName = "";
		String lLevelStr = inLevel;

		int colonAt = inLevel.indexOf( ':' );
		if( colonAt >= 0 )
		{
			if( colonAt > 0 )
				lLevelStr = inLevel.substring( 0, colonAt );
			else
				lLevelStr = null;
			if( colonAt < inLevel.length()-1 )
				lCmName = inLevel.substring( colonAt+1 );
			else
				lCmName = null;
		}

		// Normalized down to nulls if empty
		lLevelStr = NIEUtil.trimmedStringOrNull( lLevelStr );
		lCmName = NIEUtil.trimmedStringOrNull( lCmName );

		// Double check that, if there was a colon, that something
		// resonable was after it
		if( colonAt >= 0 && lCmName == null )
		{
			errorMsg( kClassName, kFName,
				"There was a colon in the verbosity tag, indicating"
				+ " a class or class.method qualifier was intended."
				+ " However, the string after the colon was empty or null."
				+ " So the verobosity tag is invalid."
				+ " The full verbosity level tag was \"" + inLevel + "\"."
				+ " Not changing verbosity."
				+ " Returning false (failure)."
				);
			return false;
		}

		// Convert to an int, let decode produce any errors about null/empty
		int levelInt = decodeStringToLevelInt( lLevelStr,
			inOutputErrorMessages
			);

		if( 0==levelInt )
			levelInt = RunLogInterface.DEFAULT_VERBOSITY;

		// See if we got back anything reasonable
		if( levelInt == RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED )
		{
			if( inOutputErrorMessages )
			{
				errorMsg( kClassName, kFName,
					"The new level string that was passed in was not recognized."
					+ " It may have been null or empty, or perhaps misspelled."
					+ " Not changing verbosity level."
					+ " Returning false (failure)."
					);
			}
			return false;
		}

		// REMINDER:
		// Coordinate changes here with
		// changes to getVerbosityLevelDescriptions()

		// If no qualifier was added, then we just call the main level
		if( lCmName == null )
			return setVerbosity( levelInt );

		// Else we'll add this to the qualifier hash

		// First, convert the int to a full object for storage in a hash
		Integer intObj = new Integer( levelInt );

		// We do not check the validity of the qualifier
		// Also we do not normalize case
		// We normalize white space here, but don't when checking

		// Next, double check the hashtable, if we're the first to store
		// anything, we need to create it
		// it is quite common for this to not be set yet
		if( mVerbosityByName == null )
			mVerbosityByName = new Hashtable();

		// Now store the results
		mVerbosityByName.put( lCmName, intObj );

		// And we're done!
		// It's up to the other methods to make use of this hash
		return true;

	}


	public boolean setVerbosityByClassAndMethodName(
		String inClassName, String optMethodName, int inLevel,
		boolean inIsClobberOK, boolean inOutputErrorMessages
		)
	{
		final String kFName = "setVerbosityByByClassAndMethodName";

		inClassName = NIEUtil.trimmedStringOrNull( inClassName );
		if( null == inClassName )
		{
			if( inOutputErrorMessages )
			{
				errorMsg( kClassName, kFName,
					"Null/empty class name passed in."
					+ " Not changing verbosity level."
					+ " Returning false (failure)."
					);
			}
			return false;
		}
		optMethodName = NIEUtil.trimmedStringOrNull( optMethodName );

		// See if we got back anything reasonable
		if( inLevel == RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED )
		{
			if( inOutputErrorMessages )
			{
				errorMsg( kClassName, kFName,
					"The new level that was passed in was not recognized: " + inLevel
					+ " Not changing verbosity level."
					+ " Returning false (failure)."
					);
			}
			return false;
		}
		if( 0==inLevel )
			inLevel = RunLogInterface.DEFAULT_VERBOSITY;

		// First, convert the int to a full object for storage in a hash
		Integer intObj = new Integer( inLevel );

		// We do not check the validity of the qualifier
		// Also we do not normalize case
		// We normalize white space here, but don't when checking

		// Next, double check the hashtable, if we're the first to store
		// anything, we need to create it
		// it is quite common for this to not be set yet
		if( mVerbosityByName == null )
			mVerbosityByName = new Hashtable();

		// form the key
		String hashKey = inClassName;
		if( null!=optMethodName )
			hashKey += "." + optMethodName;

		// Now store the results
		if( inIsClobberOK || ! mVerbosityByName.containsKey(hashKey) )
			mVerbosityByName.put( hashKey, intObj );

		// And we're done!
		// It's up to the other methods to make use of this hash
		return true;

	}




	// Returns RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED if not happy
	public int decodeStringToLevelInt( String inLevel,
		boolean inOutputErrorMessages )
	{
		final String kFName = "decodeStringToLevelInt";

		// Normalize and check the string they gave us
		String newLevelStr = NIEUtil.trimmedLowerStringOrNull( inLevel );

		if( newLevelStr == null || newLevelStr.equals("-") )
		{
			if( inOutputErrorMessages )
			{
				errorMsg( kClassName, kFName,
					"The new level string that was passed in was null or empty."
					+ " Can't set a level if we're not told what it is."
					+ " Returning special code for unrecognized verbosity."
					);
			}
			return RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED;
		}

		// Remove leading slash, if any
		if( newLevelStr.startsWith("-") )
		{
			newLevelStr = newLevelStr.substring( 1 );
		}

		// REMINDER:
		// Coordinate changes here with
		// changes to getVerbosityLevelDescriptions()

		if( newLevelStr.equals( USE_DEFAULT_INDICATOR ) ||	// "default"
			newLevelStr.equals( USE_DEFAULT_INDICATOR2 )		// "default_verbosity"
			)
			return RunLogInterface.DEFAULT_VERBOSITY;

		if( newLevelStr.equals( SUPER_QUIET_INDICATOR ) ||	// "super_quiet"
			newLevelStr.equals( SUPER_QUIET_INDICATOR2 )		// "fatal_only"
			)
			return RunLogInterface.VERBOSITY_FATAL_ONLY;

		if( newLevelStr.equals( "q" )
			|| newLevelStr.equals( QUIET_INDICATOR )			// "quiet"
			)
			return RunLogInterface.VERBOSITY_QUIET;

		if( newLevelStr.equals( "s" ) || newLevelStr.equalsIgnoreCase( PREFIX_STATUS ) )
			return RunLogInterface.VERBOSITY_STATUS_PROCESS;

		if( newLevelStr.equals( "t" )
			|| newLevelStr.equalsIgnoreCase( PREFIX_TRANSACTION )
			|| newLevelStr.equals( TRANSACTION_PLURAL_INDICATOR )	// "transactions"
			)
			return RunLogInterface.VERBOSITY_STATUS_TRANSACTIONS;

		if( newLevelStr.equals( "i" ) || newLevelStr.equalsIgnoreCase( PREFIX_INFO ) )
			return RunLogInterface.VERBOSITY_DETAILED_INFO;

		if( newLevelStr.equals( "d" ) || newLevelStr.equalsIgnoreCase( PREFIX_DEBUG ) )
			return RunLogInterface.VERBOSITY_DEBUG;

		if( newLevelStr.equals( "t" ) || newLevelStr.equalsIgnoreCase( PREFIX_TRACE ) )
			return RunLogInterface.VERBOSITY_TRACE;

		// So by now we haven't recognized what they gave us

		// Should we complain?
		// In some cases the parent program will want to handle that

		if( inOutputErrorMessages )
		{
			errorMsg( kClassName, kFName,
				"The new level string that was passed in was not recognized."
				+ " Attempted new unrecognized verbosity level was \"" + inLevel + "\"."
				+ " The method .getVerbosityLevelDescriptions() may be helpful."
				// + " Or try the same command with info (or -info) for a more detailed message."
				+ " Returning special code for unrecognized verbosity."
				);
			// Get a message and use the defaults
			String tmpMsg = getVerbosityLevelDescriptions();
			warningMsg( kClassName, kFName, tmpMsg );
		}

		// return the special unknown code
		return RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED;

	}












	public boolean setVerbosityByStringOLD( String inLevel,
		boolean inOutputErrorMessages
		)
	{
		final String kFName = "setVerbosityByString";

		// Normalize and check the string they gave us
		String newLevelStr = NIEUtil.trimmedLowerStringOrNull( inLevel );

		if( newLevelStr == null )
		{
			if( inOutputErrorMessages )
			{
				errorMsg( kClassName, kFName,
					"The new level string that was passed in was null or empty."
					+ " Can't set a level if we're not told what it is."
					+ " Leaving Verbosity level unchanged."
					);
			}
			return false;
		}

		// REMINDER:
		// Coordinate changes here with
		// changes to getVerbosityLevelDescriptions()

		if( newLevelStr.equals( "fatal_only" ) ||
			newLevelStr.equals( "super_quiet" )
			)
			return setVerbosity( RunLogInterface.VERBOSITY_FATAL_ONLY );

		if( newLevelStr.equals( "q" ) || newLevelStr.equals( "quiet" ) )
			return setVerbosity( RunLogInterface.VERBOSITY_QUIET );

		if( newLevelStr.equals( "s" ) || newLevelStr.equals( "status" ) )
			return setVerbosity( RunLogInterface.VERBOSITY_STATUS_PROCESS );

		if( newLevelStr.equals( "t" ) || newLevelStr.equals( "transaction" ) ||
			newLevelStr.equals( "transactions" )
			)
			return setVerbosity( RunLogInterface.VERBOSITY_STATUS_TRANSACTIONS );

		if( newLevelStr.equals( "i" ) || newLevelStr.equals( "info" ) )
			return setVerbosity( RunLogInterface.VERBOSITY_DETAILED_INFO );

		if( newLevelStr.equals( "d" ) || newLevelStr.equals( "debug" ) )
			return setVerbosity( RunLogInterface.VERBOSITY_DEBUG );

		if( newLevelStr.equals( "t" ) || newLevelStr.equals( "trace" ) )
			return setVerbosity( RunLogInterface.VERBOSITY_TRACE );

		// So by now we haven't recognized what they gave us

		// Should we complain?
		// In some cases the parent program will want to handle that

		if( inOutputErrorMessages )
		{
			errorMsg( kClassName, kFName,
				"The new level string that was passed in was not recognized."
				+ " Attempted new unrecognized verbosity level was \"" + inLevel + "\"."
				+ " The method .getVerbosityLevelDescriptions() may be helpful."
				// + " Or try the same command with info (or -info) for a more detailed message."
				+ " Leaving Verbosity level unchanged."
				);
			// Perhaps give a more detailed reminder message
//		    if( shouldDoInfoMsg( kClassName, kFName ) )
//		    {
//				String tmpMsg = getVerbosityLevelDescriptions(
//					true, false, true, true
//				    );
				// Get a message and use the defaults
				String tmpMsg = getVerbosityLevelDescriptions();
				warningMsg( kClassName, kFName, tmpMsg );
//				infoMsg( kClassName, kFName, tmpMsg );
//		    }
		}

		return false;
	}

	// Get syntax help, make easy to include in command line options syntax msgs
	// Todo: tie into some type of hash for easier maintenance
	public static String getVerbosityLevelDescriptions()
	{
		return getVerbosityLevelDescriptions(
			true, false, true, true
			);
	}
	// More control over the output
	public static String getVerbosityLevelDescriptions(
		boolean inShowHeader, boolean inUseHyphens,
		boolean inUseTabs, boolean inUseNewlines
		)
	{

		StringBuffer buff = new StringBuffer();

		// some seni-constants
		String tab = inUseTabs ? "\t" : " ";
		String hyphen = inUseHyphens ? "-" : "";
		String nl = inUseNewlines ? RunLogInterface.NL : "";

		String spacer1 = " | ";
		String spacer2 = ": ";

		// Start the message
		if( inShowHeader )
			buff.append( "Recognized Verbosity Levels include:" ).append(NL);

		// REMINDER:
		// Coordinate changes here with
		// changes to setVerbosityByString()

		// Fatal only
		buff.append( tab );
		buff.append( hyphen ).append( "fatal_only" );
		buff.append( spacer1 );
		buff.append( hyphen ).append( "super_quiet" );
		buff.append( spacer2 );
		buff.append( "Suppress ALL messages except FATAL Errors - use at your own risk!" );
		buff.append( nl );

		// Quiet
		buff.append( tab );
		buff.append( hyphen ).append( "q" );
		buff.append( spacer1 );
		buff.append( hyphen ).append( "quiet" );
		buff.append( spacer2 );
		buff.append( "Suppress most messages except warnings and errors." );
		buff.append( nl );

		// Overall Program Status
		buff.append( tab );
		buff.append( hyphen ).append( "s" );
		buff.append( spacer1 );
		buff.append( hyphen ).append( "status" );
		buff.append( spacer2 );
		buff.append( "Overall progress of program; major functions starting and stopping, etc." );
		buff.append( nl );

		// More Detailed / Transaction Status
		buff.append( tab );
		buff.append( hyphen ).append( "transaction" );
		buff.append( spacer1 );
		buff.append( hyphen ).append( "transactions" );
		buff.append( spacer2 );
		buff.append( "More detailed status progress of program" );
		buff.append( " including individual requests and transactions." );
		buff.append( nl );

		// Info
		buff.append( tab );
		buff.append( hyphen ).append( "i" );
		buff.append( spacer1 );
		buff.append( hyphen ).append( "info" );
		buff.append( spacer2 );
		buff.append( "More detailed descriptions of the actions taken." );
		buff.append( nl );

		// Debug
		buff.append( tab );
		buff.append( hyphen ).append( "d" );
		buff.append( spacer1 );
		buff.append( hyphen ).append( "debug" );
		buff.append( spacer2 );
		buff.append( "Includes internal logic of program." );
		buff.append( nl );

		// Trace
		buff.append( tab );
		buff.append( hyphen ).append( "trace" );
		buff.append( spacer1 );
		buff.append( hyphen ).append( "debug_trace" );
		buff.append( spacer2 );
		buff.append( "Much more verbose debug mode" );
		buff.append( "; may create HUGE log files and slow down the program." );
		buff.append( nl );

		// Return the results
		return new String( buff );

	}

	private List getVerosityElementsFromConfig()
	{
		if( fConfigTree == null )
			return null;
		return fConfigTree.findElementsByPath( VERBOSITY_ELEMENT );
	}


	// Covered by the Interface

	// Controlling verbosity
	public boolean setVerbosity( int inLevel )
	{
		final String kFName = "setVerbosity";

		// Take a little care about if either the old or the new setting
		// would have justified it
		int oldVerbosity = mVerbosity;

		// Are they the same?
		if( oldVerbosity == inLevel )
		{
			// If it's not the default
			if( inLevel != RunLogInterface.DEFAULT_VERBOSITY )
				infoMsg( kClassName, kFName,
					"Verbosity level is being set to " + inLevel
					+ " but it was already at that level."
					+ " This is OK but a perhaps a bit unusual"
					+ " so thought you should know."
					+ " Will still carry out any code logic for this level."
					);
			else // Else don't bug us about default to default
				infoMsg( kClassName, kFName,
					"Verbosity level is being set to the default value"
					+ " and it was already at that level."
					+ " This is quite normal, especially during program init."
					+ " Will still carry out any code logic for this level."
					);
		}

		// Are they setting it?
		if( inLevel != RunLogInterface.VERBOSITY_USE_DEFAULT )
		{
			mVerbosity = inLevel;
			// We should let them know if EITHER the old or new levels
			// would have shown process status messages
			if( shouldLog( oldVerbosity, kClassName, kFName ) ||
				shouldLog( mVerbosity, kClassName, kFName )
				)
			{
				// We handle changes different than requests to set it to
				// what it already is.
				// And we tend to not obsess about default verbosity as much.

				if( oldVerbosity != mVerbosity )
				{
					// Force a log event if either verbosity would have allowed
					statusMsg( kClassName, kFName,
						"Changing run log verbosity"
						+ " from " + oldVerbosity
						+ " to " + mVerbosity
						, true
						);
				}
				// Have already handled info msg for when they are the
				// same above.
			}

		}
		// Or telling us to use the default
		else
		{
			mVerbosity = RunLogInterface.DEFAULT_VERBOSITY;
			// We should let them know if EITHER the old or new levels
			// would have shown process status messages
			if( shouldLog( oldVerbosity, kClassName, kFName ) ||
				shouldLog( mVerbosity, kClassName, kFName )
				)
			{
				// Force a log event if either verbosity would have allowed
				statusMsg( kClassName, kFName,
					"Changing run log verbosity"
					+ " from " + oldVerbosity
					+ " to DEFAULT value " + mVerbosity
					, true
					);
			}
		}

		// Always return success for now
		return true;
	}

	public boolean setVerbosityForClass( String inClassName, int inLevel )
	{
		return setVerbosityByClassAndMethodName( inClassName, null, inLevel, true, true );
	}
	public boolean setVerbosityForMethod(
		String inClassName, String inMethodName, int inLevel
		)
	{
		return setVerbosityByClassAndMethodName( inClassName, inMethodName, inLevel, true, true );
	}

	// Where the data goes
	// False if it doesn't work
	public boolean setOutput( OutputStream inNewDestination )
	{
		final String kFName = "setOutput(1)";
		if( inNewDestination == null )
		{
			errorMsg( kClassName, kFName,
				"New destination output stream was null; new destination is required."
				+ " Will not change output."
				);
			return false;
		}

		PrintStream tmpOut = new PrintStream( inNewDestination );
		if( tmpOut == null )
		{
			errorMsg( kClassName, kFName,
				"Unable to create print writer, got null value."
				+ " Will not change output."
				);
			return false;
		}

		if( tmpOut.checkError() )
		{
			errorMsg( kClassName, kFName,
				"Unable to create print writer; new writer reported an error."
				+ " Will not change output."
				);
			return false;
		}

		// Tell folks what's going on
		infoMsg( kClassName, kFName,
			"Have been told to change to a new run log output."
			+ " This is the last message to the OLD output."
			);

		// Save the old output
		PrintStream oldOut = mOut;

		// Change over
		mOut = tmpOut;

		// Tell folks what's going on
		infoMsg( kClassName, kFName,
			"Have been told to change to a new run log output."
			+ " This is the first message to the NEW output."
			);

		// We want to know whether or not a non-default output has
		// been setup
		if( mOut != null && mOut != System.out && mOut != System.err )
			fHasFileOutput = true;
		else
			fHasFileOutput = false;
		debugMsg( kClassName, kFName, "Have set fHasFileOutput=" + fHasFileOutput );

		return true;

	}

	// This is the real method that does the actual work
	// Will close old print writer if not one of the standard system ones
	public boolean setOutput( PrintStream inNewDestination )
	{
		final String kFName = "setOutput(2)";
		if( inNewDestination == null )
		{
			errorMsg( kClassName, kFName,
				"New destination print writer was null; new destination is required."
				+ " Will not change output."
				);
			return false;
			// And we leave the status as it was from the last time
		}
		if( inNewDestination.checkError() )
		{
			errorMsg( kClassName, kFName,
				"New destination reports an error."
				+ " Will not change output."
				);
			return false;
			// And we leave the status as it was from the last time
		}

		// Save the old value so we can close it
		PrintStream oldOut = mOut;

		// Warn if it's the same value
		if( oldOut == inNewDestination )
		{
			warningMsg( kClassName, kFName,
				"New destination is the same as the old one."
				+ " Will continue with setting it but this seems very strange."
				);
		}



		// Switch to the new value
		mOut = inNewDestination;

		// We want to know whether or not a non-default output has
		// been setup
		if( mOut != null && mOut != System.out && mOut != System.err )
			fHasFileOutput = true;
		else
			fHasFileOutput = false;
		debugMsg( kClassName, kFName, "Have set fHasFileOutput=" + fHasFileOutput );


		// Was the old value worth trying to close?
		if( oldOut != null && oldOut != System.out && oldOut != System.err &&
			oldOut != inNewDestination
			)
		{
//			boolean success = false;
//			try
//			{
				oldOut.close();
				// ^^^ Odd, doesn't seem to thrown an exception

//				success = true;
//			}
//			catch (IOException e)
//			{
//				warningMsg( kClassName, kFName,
//					"Was unable to close old output."
//					+ " Exception was \"" + e + "\""
//					+ " Since the new output is in place and appears to be working"
//					+ " this is just a warning message to possibly check the old output."
//					);
//			}
//          // Todo: perhaps do debug with success flag

		}
		// Give a good reason why we didn't
		else if( oldOut == null )
		{
			debugMsg( kClassName, kFName,
				"Did not attempt to close the old output"
				+ " because it was null."
				);
		}
		else if( oldOut == System.out || oldOut == System.err )
		{
			debugMsg( kClassName, kFName,
				"Did not attempt to close the old output"
				+ " because it appeared to be one of the standard System streams."
				);
		}
		else if( oldOut == inNewDestination )
		{
			debugMsg( kClassName, kFName,
				"Did not attempt to close the old output"
				+ " because it appeared to ALSO be the NEW output."
				);
		}
		// Catch all, this shouldn't ever happen
		else
		{
			warningMsg( kClassName, kFName,
				"Did not attempt to close the old output"
				+ " (generic)."
				);
		}

		return true;
	}

	// Should at least accept file names
	// this overloaded version is actually a wrapper around the underloaded
	// version, which actually does the work.
	// this version also lets you set the append and overwrite policies
	public boolean setOutputURI( String inNewDestinationURI,
		boolean inShouldAppend, boolean inShouldOverwrite
		)
	{
		final String kFName = "setOutputURI(1)";

		if( inShouldAppend && inShouldOverwrite )
			warningMsg( kClassName, kFName,
				"inShouldAppend and inShouldOverwrite were both set to true."
				+ " Append takes precedence over Overwrite;"
				+ " if we're appending to a file then there is no need to overwrite it."
				+ " So your overwrite flag will have no effect."
				+ " Will still call both set methods and continue, but thought you shuld know."
				);

		// Call the setters
		setShouldAppend( inShouldAppend );
		setShouldOverwrite( inShouldOverwrite );

		// And now call the actual method
		return setOutputURI( inNewDestinationURI );
	}

	// Should at least accept file names
	public boolean setOutputURI( String inNewDestinationURI )
	{
		final String kFName = "setOutputURI(2)";
		String tmpURI = NIEUtil.trimmedStringOrNull( inNewDestinationURI );
		if( tmpURI == null )
		{
			errorMsg( kClassName, kFName,
				"URI was null or empty; URI is required."
				+ " Will not change output."
				);
			return false;
		}

		// The file we will deal with
		File theFile = new File( tmpURI );

		// We now allow for adding a default filename
		// to a directory path
		if( theFile.exists() && theFile.isDirectory() )
		{
			theFile = new File( theFile, DEFAULT_LOG_FILE_NAME );
			statusMsg( kClassName, kFName,
				"A directory name was given instead of an actuual file name"
				+ ", adding default file name '" + DEFAULT_LOG_FILE_NAME + "' to it."
				+ " Log file is now '" + theFile.toString() + "'"
				);
		}

		// Some logic flags
		boolean exists = theFile.exists();
		boolean append = shouldAppend();
		boolean overwrite = shouldOverwrite();

		// There is at least one scenario we know we can't handle
		if( exists && ! append && ! overwrite )
		{
			errorMsg( kClassName, kFName,
				"Asked to create new log file that already exists"
				+ " AND told to not append to it AND told to not overwrite it."
				+ " Please edit your configuration to allow append or overwrite mode"
				+ ", or remove the existing file, or log to a different file."
				+ " Requested file was \"" + tmpURI + "\"."
				+ " Can't create file, leaving existing logging as is."
				);
			return false;
		}

		// Prepare an action phrase for our status messages
		String actionPhrase = "creating a new";
		if( exists )
			if( append )
				actionPhrase = "appending to the existing";
			else
				actionPhrase = "overwriting the existing";

		// Go ahead and attempt the operation
		OutputStream lOut = null;
		try
		{
			lOut = new FileOutputStream( tmpURI, append );
		}
		catch (Exception e)
		{
			errorMsg( kClassName, kFName,
				"Unable to create Output Stream"
				+ " for URI \"" + tmpURI + "\"."
				+ " Currently only file system FILE NAMES are supported."
				+ " Exception was \"" + e + "\""
				+ " Will not change output."
				);
			return false;
		}

		// Let the old folks know what's going on
		statusMsg( kClassName, kFName,
			"Setting output to different location/file"
			+ " of \"" + inNewDestinationURI + "\"."
			+ " We will be " + actionPhrase + " log file."
			);

		// We'll now pass on the new output stream
		// Since we can give a more detailed error message, which would
		// include the URI in question, we will add to any failures

		boolean result = setOutput( lOut );

		if( ! result )
		{
			errorMsg( kClassName, kFName,
				"Unable to create Output Stream"
				+ " for URI \"" + tmpURI + "\"."
				+ " The call to setOutput failed."
				);
			return false;
		}

		// Say hello to the new folks
		statusMsg( kClassName, kFName,
			"Have set output to new location/file"
			+ " of \"" + inNewDestinationURI + "\"."
			+ " We are " + actionPhrase + " log file."
			);

		// And store the path we used
		fOutURI = tmpURI;

		debugMsg( kClassName, kFName, "Saving URI " + fOutURI );

		return true;
	}

	// A hook for folks wanting to extend this, they can cast the object
	// to whatever they want
	public boolean setOutputURI( String inNewDestinationURI,
		Object inAuxOutputOptions
		)
	{
		final String kFName = "setOutputURI";
		errorMsg( kClassName, kFName,
			"2nd version of this method was called"
			+ "; support for Aux Output Options not yet implemented."
			+ " Not changing output."
			);
		return false;
	}




	private static void __sep__Message_Caching__() {}
	////////////////////////////////////////////////////////

	// private void initKeepMessageCacheAndCount()
	private void initKeepMessageCount()
	{
		// NO, moved to decl init fMessageCache = new Vector();
		if( fConfigTree != null )
			fKeepMessages = fConfigTree.getIntFromAttribute(
				KEEP_N_MSGS_ATTR,
				DEFAULT_KEEP_MESSAGES
				);
	}

	public List getMessages()
	{
		// return fMessageCache;
		if( null == fMessageCache )
			return null;

		List outList = null;
		synchronized( fMessageCache )
		{
			outList = (List)((Vector)fMessageCache).clone();
		}

		return outList;
	}
	public int getKeepMessageCount()
	{
		if( fKeepMessages > 0 )
			return fKeepMessages;
		else
			return 0;
	}
	public void setKeepMessageCount( int inNewValue )
	{
		if( inNewValue >= 0 )
			fKeepMessages = inNewValue;
		else
			errorMsg( kClassName, "setKeepMessageCount",
				"Negative value " + inNewValue + " not allowed, ignoreing."
				);
	}


	void pushMsg( String inMsg )
	{
		int keep = getKeepMessageCount();
		if( keep < 1 )
			return;

		final String kFName = "pushMsg";
		if( null == inMsg || null == fMessageCache )
		{
			System.err.println( kFName
				+ " Null input(s), message will not be cached:"
				+ " inMsg=" + inMsg
				+ " fMessageCache=" + fMessageCache
				);
			return;
		}

		synchronized( fMessageCache )
		{

			// Add the message at the front
			fMessageCache.add( 0, inMsg );
	
			// Done if we don't need to remove any
			if( fMessageCache.size() <= keep )
				return;

			try
			{
				// Blow some away, starting at the end
				for( int i=fMessageCache.size()-1; i > (keep-1) ; i-- )
					fMessageCache.remove( i );
			}
			catch( Exception e )
			{
				System.err.println( kFName
					+ " Caught exception trimming message stack."
					+ " Error: " + e
					);
				e.printStackTrace( System.err );
				return;
			}

		}

	}

	/////////////////////////////////////////////////////////////////
	//
	//  Actual Message Output
	//  Will be thrown away if logging doesn't warrent it
	//
	//////////////////////////////////////////////////////////////////
	private static void __sep__General_Messages__() {}
	////////////////////////////////////////////////////////

	// Actual message output
	public boolean statusMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		)
	{
		return statusMsg(
			inFromClass, inFromMethod, inMessage,
			false
			);
	}
	public boolean statusMsg(
		String inFromClass, String inFromMethod,
		String inMessage, boolean inForceMessageOutput
		)
	{
		if( inForceMessageOutput ||
			shouldDoStatusMsg( inFromClass, inFromMethod )
			)
		{
			return genericMsg(
				PREFIX_STATUS // "Status"
				// + "(" + mRunLogObject + ")"
				,
				inFromClass, inFromMethod, inMessage,
				true
				);
		}
		else
		{
			return false;
		}
	}

	public boolean transactionStatusMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		)
	{
		if( shouldDoTransactionStatusMsg( inFromClass, inFromMethod ) )
		{
			return genericMsg(
				PREFIX_TRANSACTION, //  "Transaction",
				inFromClass, inFromMethod, inMessage,
				true
				);
		}
		else
		{
			return false;
		}
	}

	public boolean infoMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		)
	{
		if( shouldDoInfoMsg( inFromClass, inFromMethod ) )
		{
			return genericMsg(
				PREFIX_INFO, // "Info",
				inFromClass, inFromMethod, inMessage,
				false
				);
		}
		else
		{
			return false;
		}
	}

	public boolean debugMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		)
	{
		if( shouldDoDebugMsg( inFromClass, inFromMethod ) )
		{
			return genericMsg(
				PREFIX_DEBUG, // "Debug",
				inFromClass, inFromMethod, inMessage,
				false
				);
		}
		else
		{
			return false;
		}
	}

	public boolean traceMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		)
	{
		if( shouldDoTraceMsg( inFromClass, inFromMethod ) )
		{
			return genericMsg(
				PREFIX_TRACE, // "Trace",
				inFromClass, inFromMethod, inMessage,
				false
				);
		}
		else
		{
			return false;
		}
	}


	public boolean warningMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		)
	{
		if( shouldDoWarningMsg( inFromClass, inFromMethod ) )
		{
			return genericMsg(
				PREFIX_WARNING, // "WARNING",	// Make warning messages more visible with CAPS
				inFromClass, inFromMethod, inMessage,
				true	// We do want timestamps with Warnings
				// false
				);
		}
		else
		{
			return false;
		}
	}

	public boolean errorMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		)
	{
		if( shouldDoErrorMsg( inFromClass, inFromMethod ) )
		{
			return genericMsg(
				PREFIX_ERROR, // "ERROR",
				inFromClass, inFromMethod, inMessage,
				true
				);
		}
		else
		{
			return false;
		}
	}

	public boolean stackTrace(
		String inFromClass, String inFromMethod,
		Exception e, String optMessage
		)
	{
		if( shouldDoErrorMsg( inFromClass, inFromMethod ) )
		{

			String myTrace = null;
			if( null!=e ) {
				// Get a stream to string buffer
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter( sw );
				e.printStackTrace( pw );
	
				// Run the output
				String e2msg = null;
				try
				{
					pw.close();
					sw.close();
				}
				catch (Exception e2)
				{
					e2msg = "Error doing strack trace: " + e2;
				}
				// Return the string
				myTrace = sw.toString();
				if( null!=e2msg )
					myTrace += " " + e2msg;
			}
			else
				myTrace = "(NULL Strack Trace passed in)";

			if( null==optMessage )
				optMessage = "Generic:";

			return genericMsg(
				"STACK_TRACE",
				inFromClass, inFromMethod, optMessage + NIEUtil.NL + myTrace,
				true
				);
		}
		else
		{
			return false;
		}
	}


	public boolean fatalErrorMsg(
		String inFromClass, String inFromMethod,
		String inMessage
		)
	{
		return genericMsg(
			PREFIX_FATAL_ERROR, // "FATAL ERROR",
			inFromClass, inFromMethod, inMessage,
			true
			);
	}


	public boolean genericMsg(
		String inMsgHeader,
		String inFromClass, String inFromMethod,
		String inMessage,
		boolean inDoTimeStamp
		)
	{
		final String kFName = "genericMsg";

		if( inMessage == null )
		{
			errorMsg( kClassName, kFName,
				"Was passed in a null message to send to the run log"
				+ "; null messages are not allowed."
				+ " Generating this error message instead."
				+ " Message header was \"" + inMsgHeader + "\""
				+ ", original class was \"" + inFromClass + "\""
				+ ", original method was \"" + inFromMethod + "\""
				);
			return false;
		}

		inMsgHeader = inMsgHeader != null ? inMsgHeader : "Message";
		inFromClass = inFromClass != null ? inFromClass : "(class?)";
		inFromMethod = inFromMethod != null ? inFromMethod : "(method?)";

		String lTimeStamp = "";
		if( inDoTimeStamp )
			lTimeStamp = NIEUtil.getTimestamp() + " ";

		// Build our message
		StringBuffer msgBuff = new StringBuffer();
		msgBuff.append( inMsgHeader ).append( ": " );
		msgBuff.append( lTimeStamp );
		msgBuff.append( inFromClass ).append( '.' );
		msgBuff.append( inFromMethod ).append( ": " );
		msgBuff.append( inMessage );
		// Convert to string
		String tmpMsg = new String( msgBuff );

		// Save it on the stack
		pushMsg( tmpMsg );

		if( mOut != null )
		{
			mOut.println( tmpMsg );
			return mOut.checkError();
		}
		else
		{
			System.err.println( tmpMsg );
			return System.err.checkError();
		}

	}


	/////////////////////////////////////////////////////////////////
	//
	//  Check if a message would even be logged.
	//  Useful if preparing messages would be an expensive operation
	//
	//////////////////////////////////////////////////////////////////


	// Actual message output
	public boolean shouldDoStatusMsg(
		String inFromClass, String inFromMethod
		)
	{
		return shouldLog( RunLogInterface.VERBOSITY_STATUS_PROCESS,
			inFromClass, inFromMethod
			);
	}
	public boolean shouldDoTransactionStatusMsg(
		String inFromClass, String inFromMethod
		)
	{
		return shouldLog( RunLogInterface.VERBOSITY_STATUS_TRANSACTIONS,
			inFromClass, inFromMethod
			);
	}
	public boolean shouldDoInfoMsg(
		String inFromClass, String inFromMethod
		)
	{
		return shouldLog( RunLogInterface.VERBOSITY_DETAILED_INFO,
			inFromClass, inFromMethod
			);
	}
	public boolean shouldDoDebugMsg(
		String inFromClass, String inFromMethod
		)
	{
		return shouldLog( RunLogInterface.VERBOSITY_DEBUG,
			inFromClass, inFromMethod
			);
	}
	public boolean shouldDoTraceMsg(
		String inFromClass, String inFromMethod
		)
	{
		return shouldLog( RunLogInterface.VERBOSITY_TRACE,
			inFromClass, inFromMethod
			);
	}

	public boolean shouldDoWarningMsg(
		String inFromClass, String inFromMethod
		)
	{
		return shouldLog( RunLogInterface.VERBOSITY_QUIET,
			inFromClass, inFromMethod
			);
	}
	public boolean shouldDoErrorMsg(
		String inFromClass, String inFromMethod
		)
	{
		return shouldLog( RunLogInterface.VERBOSITY_QUIET,
			inFromClass, inFromMethod
			);
	}
//	public boolean shouldDoFatalErrorMsg(
//		String inFromClass, String inFromRoutine,
//		String inMessage
//		);


	// Useful if the calling class would have to do a lot of
	// work to prepare a message that would not even be displayed
	//
	// NOTE:
	// For performance reasons we DO NOT NORMALIZE for CASE OR WHITESPACE
	// And there is no check that the class/method matches a known pair
	// Being fancy at this point would slow EVERY call down
	// If somebody gets it wrong it just may not given them fine tooned
	// run logs, but they can always crank up or down the whole thing
	//
	// It is fine for class name and method name to be null
	// Todo: we aren't using many debugMsg() calls here
	// Worried about recursive calling
	// It may be best to debug this with an IDE or print statements
	public boolean shouldLog(
		int inMessageLevel, String inFromClass, String inFromMethod
		)
	{

		// Wrok on figuring out the current set point
		int currentSetpoint = mVerbosity;

		// If there's a hash, do some lookups
		// NOTE:
		// As soon as you have even ONE specific by-name rule,
		// it forces us to take this logic path EVERY time ANY
		// logging is done, so having even one little named rule may slow
		// down the system.
		if( mVerbosityByName != null && inFromClass != null )
		{
			// Our marker for what, if any, key we found
			String key = null;
			// First, try classname.methodname
			if( inFromMethod != null )
			{
				String fullName = inFromClass + "." + inFromMethod;
				if( mVerbosityByName.containsKey(fullName) )
					key = fullName;
			}
			// If no match, try just classname
			if( key == null && mVerbosityByName.containsKey(inFromClass) )
			{
				key = inFromClass;
			}

			// OK, so we've checked the possible keys, in order
			// If key is set, then we will have a specific level
			// It's perfectly fine for there to have been no match
			if( key != null )
			{
				// Fetch the Integer object
				Integer intObj = (Integer)mVerbosityByName.get(key);
				// Now retrieve it's value and save it
				currentSetpoint = intObj.intValue();
			}
		}   // End of logic for if we had a hash to check

		// Todo: could do sanity check on setpoint for "unknown" value

		// Normalize the setpoint to account for the default
		if( currentSetpoint == RunLogInterface.VERBOSITY_USE_DEFAULT )
			currentSetpoint = RunLogInterface.DEFAULT_VERBOSITY;
		// Todo: is this convoluted logic really necessary?
		// Can't every just use the DEFAULT_VERBOSITY constant?

		// Now do the actual checking
		if( inMessageLevel <= currentSetpoint )
			return true;
		else
			return false;

	}




	// Useful if the calling class would have to do a lot of
	// work to prepare a message that would not even be displayed
	public boolean shouldLogOLD(
		int inMessageLevel, String inFromClass, String inFromMethod
		)
	{
		// Figure out which verbosity to use for the test
		int tmpInt = inMessageLevel != RunLogInterface.VERBOSITY_USE_DEFAULT ?
			inMessageLevel :
			RunLogInterface.DEFAULT_VERBOSITY
			;

		if( tmpInt <= mVerbosity )
			return true;
		else
			return false;
	}


	////// Main ////////////////////////////////////////////////////

	public static void main(String[] args)
	{

		final String kFName = "main";

		// statusMsg( kFName, "Starting" );

		if( args.length < 1 )
		{
			getRunLogObject().errorMsg( kClassName, kFName,
				"Syntax error, missing arg1 (config file name)."
				+ " Syntax is: java " + kClassName + " run_log_config_uri.xml"
				+ " Exiting program (error code 1)."
				);
			System.exit( 1 );
		}
		String configFile = args[0];

		getRunLogObject().statusMsg( kClassName, kFName,
			"Will read config URI \"" + configFile + "\""
			);

		RunLogBasicImpl myLog = null;
		try
		{
			myLog = new RunLogBasicImpl( configFile );
		}
		catch (Exception e)
		{
			getRunLogObject().errorMsg( kClassName, kFName,
				"Unable to construct Run Log Config object"
				+ " Exception = " + e
				+ " Exiting program (error code 2)."
				);
			System.exit( 2 );
		}

		getRunLogObject().statusMsg( kClassName, kFName,
			"Was able to read config."
			);
	}


	// The main JDOM configuration tree
	private JDOMHelper fConfigTree;

	// List of cached messages, if any
	volatile List fMessageCache = new Vector();


	// How many messages to keep
	private static final String KEEP_N_MSGS_ATTR = "keep";
	int fKeepMessages = DEFAULT_KEEP_MESSAGES;


	// When looking at an element, what is the attr name for Verobosity
	public static final String VERBOSITY_ATTR = "verbosity";
	// Where to write the log file to
	public static final String LOCATION_ATTR = "location";


	// Our Verbosity
	private int mVerbosity = RunLogInterface.DEFAULT_VERBOSITY;

	// Where to send data to
	private PrintStream mOut;

	// Master table for overriding defaults
	// LEAVE THIS NULL until someobody actually wants ot use it.
	// For apps that don't use it, we can skip a lot of processing
	// when we know this is null
	private Hashtable mVerbosityByName; // = null by default

	// Whether or not they have actually set a non-default output
	private boolean fHasFileOutput;
	// the last URI that was successfully set
	private String fOutURI;

	// Policies concerning log files
	private static final String APPEND_ATTR = "append";
	private static final String OVERWRITE_ATTR = "overwrite";

	public static final String VERBOSITY_ELEMENT = "verbosity";
	public static final String VERBOSITY_CLASS_ATTR = "class";
	public static final String VERBOSITY_METHOD_ATTR = "method";
	public static final String VERBOSITY_LEVEL_ATTR = "level";


	// See also RunLogInterface int VERBOSITY_xxx values
	public static final String PREFIX_STATUS = "Status";
	public static final String PREFIX_TRANSACTION = "Trasnaction";
	public static final String PREFIX_INFO = "Info";
	public static final String PREFIX_DEBUG = "Debug";
	public static final String PREFIX_TRACE = "Trace";
	public static final String PREFIX_WARNING = "WARNING";
	public static final String PREFIX_ERROR = "ERROR";
	public static final String PREFIX_FATAL_ERROR = "FATAL_ERROR";
	public static final String PREFIX_GENERIC = "Message";

	public static final String USE_DEFAULT_INDICATOR = "default";
	public static final String USE_DEFAULT_INDICATOR2 = "default_verbosity";

	public static final String TRANSACTION_PLURAL_INDICATOR = "transactions";

	public static final String QUIET_INDICATOR = "quiet";

	public static final String SUPER_QUIET_INDICATOR = "super_quiet";
	public static final String SUPER_QUIET_INDICATOR2 = "fatal_only";


	public static final Map INT_TO_LEVEL_PREFIX;
	static {
		INT_TO_LEVEL_PREFIX = new Hashtable();
		INT_TO_LEVEL_PREFIX.put(
			new Integer( RunLogInterface.VERBOSITY_STATUS_PROCESS ),
				PREFIX_STATUS );
		INT_TO_LEVEL_PREFIX.put(
			new Integer( RunLogInterface.VERBOSITY_STATUS_TRANSACTIONS ),
				PREFIX_TRANSACTION );
		INT_TO_LEVEL_PREFIX.put(
			new Integer( RunLogInterface.VERBOSITY_DETAILED_INFO ),
				PREFIX_INFO );
		INT_TO_LEVEL_PREFIX.put(
			new Integer( RunLogInterface.VERBOSITY_DEBUG ),
				PREFIX_DEBUG );
		INT_TO_LEVEL_PREFIX.put(
			new Integer( RunLogInterface.VERBOSITY_TRACE ),
				PREFIX_TRACE );
		INT_TO_LEVEL_PREFIX.put(
			new Integer( RunLogInterface.LEVEL_WARNING ),
				PREFIX_WARNING );
		INT_TO_LEVEL_PREFIX.put(
			new Integer( RunLogInterface.LEVEL_ERROR ),
				PREFIX_ERROR );
		INT_TO_LEVEL_PREFIX.put(
			new Integer( RunLogInterface.LEVEL_FATAL_ERROR ),
				PREFIX_FATAL_ERROR );
		INT_TO_LEVEL_PREFIX.put(
			new Integer( RunLogInterface.LEVEL_GENERIC ),
				PREFIX_GENERIC );
	}

	// Append SUPERCEDES Overwrite
	private boolean fShouldAppend = DEFAULT_SHOULD_APPEND;
	private boolean fShouldOverwrite = DEFAULT_SHOULD_OVERWRITE;
	
	// default for when they just specify a directory
	private static final String DEFAULT_LOG_FILE_NAME = "niesrv.out";

	// We default both to yes, but append supercedes overwrite
	private final static boolean DEFAULT_SHOULD_APPEND = true;
	private final static boolean DEFAULT_SHOULD_OVERWRITE = true;
	final static int DEFAULT_KEEP_MESSAGES = 100; // 50;

}
