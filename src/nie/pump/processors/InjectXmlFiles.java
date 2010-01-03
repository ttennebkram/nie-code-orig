/*
 *  Copyright 2005 New Idea Engineering, Inc.
 *  Written by Mark Bennett
 *
 * Inject MULTIPLE XML files, very similar to inject work unit files
 * TODO: will eventually create xml_in for single web or xml named docs
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

public class InjectXmlFiles extends Processor
{
	static final String kClassName = "InjectXmlFiles";
	public String kClassName() { return kClassName; }


	// private static final boolean debug = false;

	public InjectXmlFiles( Application inApplication,
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
		
		infoMsg( kFName, "InjectXmlFiles successfully instantiated." );


		if( inWriteToQueue != null )
		{
			// System.err.println( "# Write Queues: " + inWriteToQueue.length );
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
				stackTrace( kFName, e, "JDOM Error from parameters" );
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
				// 	"Inject: Unable to handle '" +
				// 	lDirName + "'" +
				// 	" Exception from getCanonicalPath: " + e
				// 	);
		    	stackTrace( kFName, e, "Error with getCanonicalPath" );
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
			//		"Inject: setDirName dir='" +
			//		thePath + "'"
			//		);
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
		feedAllDirs( DEFAULT_RECURSIVE );

		setStateFinished();

		// System.out.println( "Exiting InjectXmlFiles.run()" );
		infoMsg( kFName, "Exiting InjectXmlFiles.run()" );
	}

	// Given a potential work unit file name, check it,
	// instantiate it, and then feed it to the ouptut queue
	boolean feedAFile( String wuFileName )
	{
		return feedAFile( new File(wuFileName) );
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
			wu = new WorkUnit();
		}
		catch (Exception e)
		{
			// System.err.println( "Error creating WorkUnit: " + e);
			errorMsg( kFName, "Error creating WorkUnit: " + e );
			return false;
		}

		addRunpathEntry( wu );

		/*if(debug)*/
			// System.err.println( "Feeding XML file from '" +
			//		inFile + "' by File"
			//		);
			infoMsg( kFName, "Feeding XML file from '" +
					inFile + "' by File"
					);
		//System.err.flush();


		try
		{
			Element treeContent = (new JDOMHelper( inFile )).getJdomElement();
	
			Element baseElem = treeContent;
			String baseElemName = getDestinationFieldName();
			if( null!=baseElemName ) {
				baseElem = new Element( baseElemName );
				baseElem.addContent( treeContent );
			}
	
			// Store the content
			wu.addUserDataElement( baseElem );

			// Send it to the queue
			enqueue( fWriteQueue, wu );
			wu = null;
		}
		catch (JDOMHelperException je)
		{
			// System.err.println( "Error adding XML from '" +
			//	inFile + "', skipping: " + je );
			stackTrace( kFName, je, "Error adding XML from '" +
				inFile + "', skipping: " + je );
			return false;
		}

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



	void feedAllDirs( boolean isRecursive )
	{
		final String kFName = "feedAllDirs";
		List wuDirNames = jh.getTextListByPath( DIR_NAME_PARAM );
		if( wuDirNames == null )
		{
			// System.err.println( "No work unit directory parameters found" );
			warningMsg( kFName, "No work unit directory parameters found" );
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
			feedADir( wuDirName, isRecursive );
		}

	}

	void feedADir( String dirName, boolean isRucursive )
	{
		if( dirName == null )
			return;
		dirName = dirName.trim();
		if( dirName.equals("") )
			return;

		//File din = new File( dirName );
		File din = dirToAbsFile( dirName );

		feedADir( din, isRucursive );
	}

	void feedADir( File inDir, boolean isRucursive )
	{
		
		final String kFName = "feedADir";
		if( null == inDir )
			return;

		if( ! inDir.isDirectory() )
		{
			// System.err.println( "Error: wu_dir '" + inDir +
			//	"' is not a valid directory, skipping"
			//	);
			errorMsg( kFName, "wu_dir '" + inDir +
				"' is not a valid directory, skipping"
				);
			return;
		}
		else
			// System.out.println( "Scanning dir '" + inDir +
			//	"' for work unit xml files."
			//	);
			infoMsg( kFName, "Scanning dir '" + inDir +
				"' for work unit xml files."
				);

		// Get the list of files
		String [] fileNames = inDir.list();

		// For each file name in this directory
		for( int i=0; i < fileNames.length; i++ )
		{
			String fileName = fileNames[i];
			// skip it if it's a directory
			//File theFile = new File( dirName, fileName );
			File theFile = new File( inDir, fileName );

			// Maybe scan directories
			if( isRucursive && theFile.isDirectory() ) {
				feedADir( theFile, isRucursive );
				continue;
			}

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

	String getDestinationFieldName()
	{
		String tmpString = jh.getStringFromAttributeTrimOrNull(
			DESTINATION_FIELD_ATTR_NAME
			);
		if( null == tmpString )
		{
			tmpString = jh.getStringFromAttributeTrimOrNull(
				DESTINATION_FIELD_ATTR_SHORT_NAME
				);
		}
		if( null == tmpString )
		{
			tmpString = jh.getStringFromAttributeTrimOrNull(
				TREE_FIELD_ATTR_NAME
				);
		}
		return tmpString;
	}


	Queue fWriteQueue;
	JDOMHelper jh;
	String fFileNamePrefix;
	int fPauseMS;

	private static final String DIR_NAME_PARAM = "dir";
	private static final String FILE_NAME_PARAM = "file";
	private static final String FILE_NAME_PREFIX_PARAM = "prefix";
	private static final String DELAY_PARAM = "pause";

	private static final String DESTINATION_FIELD_ATTR_NAME =
		"destination_field";
	private static final String DESTINATION_FIELD_ATTR_SHORT_NAME =
		"dst";
	// Same as destination, for compatibility with http_download
	private static final String TREE_FIELD_ATTR_NAME =
		"tree_field";

	public static final boolean DEFAULT_RECURSIVE = true;
	// Not used in this processor, if there's trouble loading a file
	// then there's nothing to attach an error to
	WorkUnit _mWorkUnit;
}

