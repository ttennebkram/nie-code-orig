/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Mark Bennett
 *
 * Write the work units to a directory.
 * Also, if an output queue is given, forward them on
 *
 * Required:
 * <dir>source_dir_1</dir>
 * Optioanl: a prefix for all the files we create, default is wu_
 * <prefix>myprefix_</prefix>
 */

//package nie.processors;
package nie.pump.processors;

import java.io.*;
import java.net.*;
import java.util.*;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

//import Processor;
//import Application;
//import Queue;

import org.jdom.Element;

public class  SiphonWorkUnitFilesV2 extends Processor
{

	public String kClassName() {
		return "SiphonWorkUnitFilesV2";
	}


	// private static final boolean debug = false;

	public  SiphonWorkUnitFilesV2( Application inApplication,
		  Queue[] inReadFromQueue,
		  Queue[] inWriteToQueue,
		  Queue[] inUsesQueue,
		  Element inParameters,
		  String inProcID
		  )
	{
		super( inApplication, inReadFromQueue, inWriteToQueue,
			inUsesQueue, inParameters, inProcID
			);

		final String kFName = "constructor";
		// If parameters were sent in, save them
		if( inParameters != null )
		{
			//System.err.println( "inParameters = " + inParameters );
			try
			{
				jh = new JDOMHelper( inParameters );
			}
			catch (Exception e)
			{
				jh = null;
				// System.err.println(
				//	"Error creating jdom helper for parameters\n" + e
				//	);
				fatalErrorMsg( kFName,
						"Error creating jdom helper for parameters\n" + e
						);
				System.exit(1);
			}
		}
		else
		{
			// System.err.println( "Error: inParameters == null." );
			fatalErrorMsg( kFName, "inParameters == null." );
			// System.err.println( "see doc" );
			// fatalErrorMsg( kFName, "see doc" );
			System.exit(1);
		}

		// Where to read from
		if( inReadFromQueue != null )
		{
			//System.err.println( "# Read Queues: " + inReadFromQueue.length );
			fReadQueue = inReadFromQueue[0];
		}
		else
		{
			// System.err.println( "Error: we require an input queue." );
			fatalErrorMsg( kFName, "we require an input queue." );
			System.exit(1);
		}

		// What queue to write to, if any
		if( inWriteToQueue != null )
		{
			//System.err.println( "# Write Queues: " + inWriteToQueue.length );
			fWriteQueue = inWriteToQueue[0];
		}
		else
		{
			// System.err.println( "InWriteToQueue == null." );
			errorMsg( kFName, "InWriteToQueue == null." );
			// System.err.println( "No queue to write to, will only save files to disk. ");
			errorMsg( kFName, "No queue to write to, will only save files to disk. ");
		}

		// Now get the name of the directory we will write to
		setDirName();
		//fOutDirName = jh.getTextByPath( "wu_dir" );
		//if( fOutDirName == null || fOutDirName.trim().equals("") )
		//{
		//	System.err.println( "Error: no <wu_dir> to write to" );
		//	System.exit(1);
		//}
		//fOutDirName = fOutDirName.trim();

		// See if there was a file name prefix as well?
		fFileNamePrefix = jh.getTextByPath( FILE_NAME_PREFIX_PARAM );
		if( fFileNamePrefix == null || fFileNamePrefix.trim().equals("") )
			fFileNamePrefix = DEFAULT_FILE_NAME_PREFIX;
		fFileNamePrefix = fFileNamePrefix.trim();

		// Some debug info
		if( inUsesQueue != null )
		{
			// System.err.println( "# Uses Queues: " + inUsesQueue.length );
			infoMsg( kFName, "# Uses Queues: " + inUsesQueue.length );
			// System.err.println( "Warning: we don't use 'uses' queues." );
			warningMsg( kFName, "Warning: we don't use 'uses' queues." );
		}

		//System.err.println( "SiphonWorkUnitFiles successfully instantiated." );

		initParams();

	}

	private void initParams()
	{
		fHaveDoneInit = false;
		fFieldNameAsPrefix = getFieldNameForPrefix();
		fKeepRunpathOnOutput = getKeepRunpathOnOutput();
		fHaveDoneInit = true;
	}

	/////////////////////////////////////////////
	//
	// And this is where the work gets done.
	//
	/////////////////////////////////////////////

	public void run()
	{

		final String kFname = "run";
		while( true )
		{
			try
			{
				// Grab a work unit
				WorkUnit lWorkUnit = dequeue( fReadQueue );
				// Send it to a file
				writeWorkUnit( lWorkUnit );
				// If we have an output queue, pass it on
				if( fWriteQueue != null )
				{
					enqueue( fWriteQueue, lWorkUnit );
					lWorkUnit = null;
				}
			}
			catch( InterruptedException ie )
			{
				return;
			}
		}
	}



	void setDirName()
	{
		final String kFName = "setDirName";
		// final boolean debug = false;

		String lOutDirName = jh.getTextByPath( DIR_NAME_PARAM );
		if( lOutDirName == null || lOutDirName.trim().equals("") )
		{
			// System.err.println( "Siphon: Error: no <dir> to write to" );
			fatalErrorMsg( kFName, "no <dir> to write to" );
			System.exit(1);
		}
		lOutDirName = lOutDirName.trim();

		debugMsg( kFName, "passed in '" +
			lOutDirName + "'"
			);

		File theDir = new File( lOutDirName );
		if( ! theDir.isAbsolute() )
			theDir = theDir.getAbsoluteFile();
		String thePath = null;
		try
		{
			thePath = theDir.getCanonicalPath();
		}
		catch (Exception e)
		{
				//System.err.println(
				//	"Siphon: Unable to handle '" +
				//	lOutDirName + "'" +
				//	" Exception from getCanonicalPath: " + e
				//	);
				stackTrace( kFName, e, "Exception from getCanonicalPath for " + lOutDirName );
				fatalErrorMsg( kFName, "Exiting due to exception: " + e );
				System.exit(1);
		}

		if( ! theDir.exists() )
		{
			// System.err.println(
			//	"Siphon: Will create directory '" +
			//	thePath + "'"
			//	);
			statusMsg( kFName, 
					"Will create directory '" +
					thePath + "'"
					);
			if( ! theDir.mkdirs() )
			{
				// System.err.println(
				//	"Siphon: Unable to create directory '" +
				//	thePath + "'"
				//	);
				fatalErrorMsg( kFName, 
						"Unable to create directory '" +
						thePath + "'"
						);
				System.exit(1);
			}
		}

		debugMsg( kFName, "setDirName dir='" +
				thePath + "'"
				);

		fOutDirName = thePath;

	}

	void writeWorkUnit( WorkUnit inWorkUnit )
	{
		String fileName = getNextFileName( inWorkUnit );
		WorkUnit lWorkUnit = inWorkUnit;

		// Were we asked to delete the runpath on output?
		if( ! getKeepRunpathOnOutput() )
		{
			// If we're also going to pass the work unit on
			// we don't want it modified, so we need to make
			// a clone to mess with
			if( fWriteQueue != null )
				lWorkUnit = (WorkUnit)inWorkUnit.clone();
			// Remove the run path as requested
			lWorkUnit.nukeRunpath();
		}

		// Now output the work unit
		lWorkUnit.writeToFile( fileName );

	}

	private String getNextFileName( WorkUnit inWU )
	{
		final String kFName = "getNextFileName";
		fCounter++;

		// The prefix we will use
		String thePrefix = null;

		// Do they want to use a field's contents for the name?
		String prefixField = getFieldNameForPrefix();
		if( prefixField != null )
		{
			thePrefix = inWU.getUserFieldText( prefixField );
			if( thePrefix == null || thePrefix.trim().equals("") )
			{
				thePrefix = null;
				// System.err.println( "Warning: Siphon: found null field value"
				//	+ " for file name prefix, will use default naming rules"
				//	+ ", field name=" + prefixField
				//	);
				errorMsg( kFName, "found null field value"
						+ " for file name prefix, will use default naming rules"
						+ ", field name=" + prefixField
						);
			}

			// Todo: I suppose we could offer to still add a numeric suffix
			// that might help avoid duplicates, if the fields have dupes
		}

		// If still null, auto generate something
		if( thePrefix == null )
		{
			String numStr = NIEUtil.leftPadInt( fCounter,
				NUMERIC_FILE_NAME_PAD_LENGTH
				);
			thePrefix = fFileNamePrefix + numStr;
		}

		// Now we have a prefix, add the extension
		String theShortName = thePrefix + ".xml";

		// Now create a file with the proper directory
		File theFile = new File( fOutDirName, theShortName );

		String thePath = null;
		try
		{
			thePath = theFile.getCanonicalPath();
		}
		catch (Exception e)
		{
				// System.err.println(
				//	"Siphon:getNextFileName: Unable to combine dir '" +
				//	fOutDirName + "' with new file name '" +
				//	theShortName + "'" +
				//	" Exception from getCanonicalPath: " + e
				//	);
		    	stackTrace( kFName, e, "Exception from getCanonicalPath: " );
				fatalErrorMsg( kFName, 
						"Unable to combine dir '" +
						fOutDirName + "' with new file name '" +
						theShortName + "'" +
						" Exception from getCanonicalPath: " + e
						);
				System.exit(1);
		}

		return thePath;

	}

	String getFieldNameForPrefix()
	{
		if( fHaveDoneInit )
			return fFieldNameAsPrefix;
		String tmpStr = jh.getTextByPath( FIELD_AS_FILE_NAME_PREFIX_PARAM );
		if( tmpStr == null || tmpStr.trim().equals("") )
			tmpStr = null;
		return tmpStr;
	}

	boolean getKeepRunpathOnOutput()
	{
		if( fHaveDoneInit )
			return fKeepRunpathOnOutput;
		boolean result = jh.getBooleanFromPathText(
			KEEP_RUNPATH_PARAM, DEFAULT_KEEP_RUNPATH
			);
		return result;
	}


	boolean fHaveDoneInit;
	Queue fReadQueue;
	Queue fWriteQueue;
	String fOutDirName;
	String fFileNamePrefix;
	String fFieldNameAsPrefix;
	boolean fKeepRunpathOnOutput;
	long fCounter;

	JDOMHelper jh;

	private static final String DEFAULT_FILE_NAME_PREFIX = "wu_";
	private static final boolean DEFAULT_KEEP_RUNPATH = true;
	private static final int NUMERIC_FILE_NAME_PAD_LENGTH = 5;
	private static final String DIR_NAME_PARAM = "dir";
	private static final String FILE_NAME_PREFIX_PARAM = "prefix";
	private static final String FIELD_AS_FILE_NAME_PREFIX_PARAM =
		"file_prefix_field";
	private static final String KEEP_RUNPATH_PARAM = "keep_runpath";


	private static void ___Sep__Run_Logging__(){}
	
	// None of these errors will effect the output work unit, so no logging to it
	WorkUnit _mWorkUnit;

	//////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////////////
	//
	//  Logging
	//
	////////////////////////////////////////////////////////////

	/***
	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}



	private boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}


	private boolean shouldDoStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoStatusMsg( this.kClassName(), inFromRoutine );
	}



	private boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}


	private boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( this.kClassName(), inFromRoutine );
	}



	private boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}


	private boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( this.kClassName(), inFromRoutine );
	}



	private boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}


	private boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( this.kClassName(), inFromRoutine );
	}



	private boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}


	private boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( this.kClassName(), inFromRoutine );
	}



	private boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}


	private boolean shouldDoWarningMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoWarningMsg( this.kClassName(), inFromRoutine );
	}



	private boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}



	private boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}

	***/









}
