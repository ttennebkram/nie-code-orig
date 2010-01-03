/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Mark Bennett
 *
 * 2001/10/10 mbennett Rev 2, shortened parameter names, added prefix filter
 *
 * Read in work unit files and shove them into the system
 * In the <parameters> area you should have:
 * <file>test1.xml</file>
 * <file>test2.xml</file>
 * <file>http://whatever/test3.xml</file>
 *
 * Or you can do:
 * <dir>source_dir_1</dir>
 * <dir>source_dir_2</dir>
 * Only works on file system directories.
 *
 * When feeding directories you can filter which files are sent:
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

import org.jdom.Element;

public class InjectWorkUnitFilesV2 extends Processor
{
	public String kClassName() { return "InjectWorkUnitFilesV2"; }


	// private static final boolean debug = false;

	public InjectWorkUnitFilesV2( Application inApplication,
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
		
		// System.err.println( "InjectWorkUnitFiles successfully instantiated." );
		infoMsg( kFName, "InjectWorkUnitFiles successfully instantiated." );


		if( inWriteToQueue != null )
		{
			// System.err.println( "# Write Queues: " + inWriteToQueue.length );
			statusMsg( kFName, "# Write Queues: " + inWriteToQueue.length );
			fWriteQueue = inWriteToQueue[0];
		}
		else
		{
			// System.err.println( "InWriteToQueue == null." );
			fatalErrorMsg( kFName, "InWriteToQueue == null." );
			// System.err.println( "Error: No queue to write to, nothing to do" );
			fatalErrorMsg( kFName, "No queue to write to, nothing to do" );
			System.exit(1);
		}


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

		// See if there was a file name prefix as well?
		fFileNamePrefix = jh.getTextByPath( FILE_NAME_PREFIX_PARAM );

		// Normalize the file name prefix, and if it's empty
		// force it to a true null
		if( fFileNamePrefix != null )
		{
			fFileNamePrefix = fFileNamePrefix.trim();
			if( fFileNamePrefix.trim().equals("") )
				fFileNamePrefix = null;
		}
		// Else it was already null

		// Should we pause for ms between each injected file?
		fPauseMS = jh.getIntFromPathText( DELAY_PARAM, -1 );

		// Some debug info
		if( inReadFromQueue != null )
		{
			// System.err.println( "# Read Queues: " + inReadFromQueue.length );
			infoMsg( kFName, "# Read Queues: " + inReadFromQueue.length );
			// System.err.println( "Warning: we don't take inputs." );
			errorMsg( kFName, "we don't take inputs." );
		}
		if( inUsesQueue != null )
		{
			// System.err.println( "# Uses Queues: " + inUsesQueue.length );
			infoMsg( kFName, "# Uses Queues: " + inUsesQueue.length );
			// System.err.println( "Warning: we don't use 'uses' queues." );
			errorMsg( kFName, "we don't use 'uses' queues." );
		}

	}



	File dirToAbsFile( String inDirName )
	{
		
		final String kFName = "constructor";
		if( inDirName == null || inDirName.trim().equals("") )
		{
			// System.err.println( "Warning: Inject:dirToAbs: null dir passed in" );
			errorMsg( kFName, "null dir passed in" );
			return null;
		}

		String lDirName = inDirName.trim();

		File theDir = new File( lDirName );
		if( ! theDir.isAbsolute() )
			theDir = theDir.getAbsoluteFile();
		String thePath = null;
		try
		{
			thePath = theDir.getCanonicalPath();
		}
		catch (Exception e)
		{
				// System.err.println(
				//	"Inject: Unable to handle '" +
				//	lDirName + "'" +
				//	" Exception from getCanonicalPath: " + e
				//	);
			fatalErrorMsg( kFName,
						"Unable to handle '" +
						lDirName + "'" +
						" Exception from getCanonicalPath: " + e
						);
				System.exit(1);
		}

		if( ! theDir.exists() )
		{
			// System.err.println(
			//	"Inject: Unable to find directory '" +
			//	thePath + "'"
			//	);
			fatalErrorMsg( kFName,
					"Unable to find directory '" +
					thePath + "'"
					);
			System.exit(1);
		}

		// if(debug)
			// System.err.println(
			//	"Inject: setDirName dir='" +
			//	thePath + "'"
			//	);
			debugMsg( kFName,
				"setDirName dir='" +
				thePath + "'"
				);

		return theDir;
	}


	/////////////////////////////////////////////
	//
	// And this is where the work gets done.
	//
	/////////////////////////////////////////////

	public void run()
	{
		
		final String kFName = "run";
		setStateProcessing();

		// Look for and feed through all the <wu_file>xxx</wu_file> entries
		feedAllFiles();
		// Look for and feed through all the <wu_dir>xxx</wu_dir> entries
		feedAllDirs();

		setStateFinished();

		// System.out.println( "Exiting InjectWorkUnitFiles.run()" );
		infoMsg( kFName, "Exiting InjectWorkUnitFiles.run()" );
	}

	// Given a potential work unit file name, check it,
	// instantiate it, and then feed it to the ouptut queue
	boolean feedAFile( String wuFileName )
	{
		final String kFName = "feedAFile";
		WorkUnit wu = null;

		if( wuFileName == null )
			return false;

		wuFileName = wuFileName.trim();
		if( wuFileName.equals("") )
			return false;

		try
		{
			wu = new WorkUnit( wuFileName );
		}
		catch (Exception e)
		{
			// System.err.println( "Error creating WorkUnit from '" +
			//	wuFileName + "', skipping." );
			stackTrace( kFName, e, "Error creating WorkUnit from '" +
				wuFileName + "', skipping." );
			// System.err.println( e );
			// mWorkUnit.errorMsg( this, kFName, "Got Exception:" + e );
			return false;
		}

		addRunpathEntry( wu );

		// if(debug)
		// System.out.println( "Feeding work unit from '" +
		//		wuFileName + "' by name"
		//		);
		debugMsg( kFName, "Feeding work unit from '" +
				wuFileName + "' by name"
				);
		//
		// Send it to the queue
		enqueue( fWriteQueue, wu );
		wu = null;

		// Impelement pausing
		if( fPauseMS > 0 )
			try {
				//this.wait( (long)fPauseMS );
				Thread.sleep(fPauseMS);
			} catch(Exception e) {
									// System.err.println( "sleep1 exc: " + e );
									debugMsg( kFName, "sleep1 exc: " + e );
								  }

		return true;
	}

	// Given a potential work unit file name, check it,
	// instantiate it, and then feed it to the ouptut queue
	boolean feedAFile( File inFile )
	{
		final String kFName = "feedAFile";
		// final boolean debug = false;

		WorkUnit wu = null;

		if( inFile == null )
			return false;

		try
		{
			wu = new WorkUnit( inFile );
		}
		catch (Exception e)
		{
			// System.err.println( "Error creating WorkUnit from '" +
			//	inFile + "', skipping." );
			stackTrace( kFName, e, "Error creating WorkUnit from '" +
				inFile + "', skipping." );
			// System.err.println( e );
			// mWorkUnit.errorMsg( this, kFName, "Got Exception:" + e );
			return false;
		}

		addRunpathEntry( wu );

		// if(debug)
			// System.err.println( "Feeding work unit from '" +
			//		inFile + "' by File"
			//		);
			debugMsg( kFName, "Feeding work unit from '" +
					inFile + "' by File"
					);
		//System.err.flush();

		// Send it to the queue
		enqueue( fWriteQueue, wu );
		wu = null;

		// Impelement pausing
		if( fPauseMS > 0 )
			try {
				//this.wait( (long)fPauseMS );
				Thread.sleep(fPauseMS);
			} catch(Exception e) {
									// System.err.println( "sleep exc: " + e );
									debugMsg( kFName, "sleep exc: " + e );
								  }

		return true;
	}

	// Feed all the individual files that were referenced
	void feedAllFiles()
	{
		final String kFName = "feedAllFiles";
		List wuFileNames = jh.getTextListByPath( FILE_NAME_PARAM );

		if( wuFileNames == null )
		{
			// System.err.println( "No individual work unit files" );
			warningMsg( kFName, "No individual work unit files" );
			return;
		}
		else
		{
			// System.err.println( "Found " + wuFileNames.size() + " candidate uri(s)/files to inject" );
			infoMsg( kFName, "Found " + wuFileNames.size() + " candidate uri(s)/files to inject" );
		}

		Iterator it = wuFileNames.iterator();
		while( it.hasNext() )
		{
			String wuFileName = (String)it.next();
			feedAFile( wuFileName );
		}
	}



	void feedAllDirs()
	{
		final String kFName = "feedAllDirs";
		List wuDirNames = jh.getTextListByPath( DIR_NAME_PARAM );
		if( wuDirNames == null )
		{
			// System.err.println( "Found " + wuFileNames.size() + " candidate uri(s)/files to inject" );
			// mWorkUnit.errorMsg( this, kFName, "Found " + wuFileNames.size() + " candidate uri(s)/files to inject" );
			return;
		}
		else
		{
			// System.err.println( "Found " + wuDirNames.size() + " candidate dirs to inject from" );
			infoMsg( kFName, "Found " + wuDirNames.size() + " candidate dirs to inject from" );
		}

		Iterator it = wuDirNames.iterator();
		while( it.hasNext() )
		{
			String wuDirName = (String)it.next();
			feedADir( wuDirName );
		}

	}

	void feedADir( String dirName )
	{
		final String kFName = "feedADir";
		if( dirName == null )
			return;
		dirName = dirName.trim();
		if( dirName.equals("") )
			return;

		//File din = new File( dirName );
		File din = dirToAbsFile( dirName );
		if( ! din.isDirectory() )
		{
			// System.err.println( "Error: wu_dir '" + dirName +
			//	"' is not a valid directory, skipping"
			//	);
			errorMsg( kFName, "wu_dir '" + dirName +
				"' is not a valid directory, skipping"
				);
			return;
		}
		else
			// System.out.println( "Scanning dir '" + dirName +
			//	"' for work unit xml files."
			//	);
			infoMsg( kFName, "Scanning dir '" + dirName +
				"' for work unit xml files."
				);

		// Get the list of files
		String [] fileNames = din.list();

		// For each file name in this directory
		for( int i=0; i < fileNames.length; i++ )
		{
			String fileName = fileNames[i];
			// skip it if it's a directory
			//File theFile = new File( dirName, fileName );
			File theFile = new File( din, fileName );

			// Skip if it's not a file
			if( ! theFile.isFile() )
				continue;

			// Skip if it doesn't end in .xml
			if( ! fileName.toLowerCase().endsWith( ".xml" ) )
				continue;

			// Skip if we have a prefix set and this does NOT match
			// the prefix
			if( fFileNamePrefix != null &&
				! fileName.startsWith( fFileNamePrefix )
				)
				continue;

			// Go ahead and send it!
			feedAFile( theFile );

		}
	}

	void addRunpathEntry( WorkUnit inWU )
	{
		inWU.createRunpathEntry( getID() );
	}

	Queue fWriteQueue;
	JDOMHelper jh;
	String fFileNamePrefix;
	int fPauseMS;

	private static final String DIR_NAME_PARAM = "dir";
	private static final String FILE_NAME_PARAM = "file";
	private static final String FILE_NAME_PREFIX_PARAM = "prefix";
	private static final String DELAY_PARAM = "pause";
	// Not useful for this processor
	// If we can't create it, then there's nothing to tag with an error
	WorkUnit _mWorkUnit;

}

