//package nie.processors;
package nie.pump.processors;

import java.net.*;
import org.jdom.*;
import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

/**
 * Title:        DPump
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop and Mark Benett
 * @version 1.0
 */

public class URLDirExtractor extends Processor
{
	public String kClassName() { return "URLDirExtractor"; }


	public URLDirExtractor( Application inApplication,
						Queue[] inReadQueueList,
						Queue[] inWriteQueueList,
						Queue[] inUsesQueueList,
						Element inParameter,
						String inID )
	{
		super( inApplication, inReadQueueList, inWriteQueueList,
			inUsesQueueList, inParameter, inID
			);
		final String kFName = "constructor";
		if( inReadQueueList == null || inReadQueueList[0] == null )
		{
			// System.err.println( inID + ": requires an input queue." );
			fatalErrorMsg( kFName, inID + ": requires an input queue." );
			System.exit( -1 );
		}

		if( inWriteQueueList == null || inWriteQueueList[0] == null )
		{
			// System.err.println( inID + ": requires an output queue." );
			fatalErrorMsg( kFName, inID + ": requires an output queue." );
			System.exit( -1 );
		}

		fReadQueue = inReadQueueList[0];
		fWriteQueue = inWriteQueueList[0];

		Element lBase = inParameter.getChild( BUILD_TAG_NAME );
		if( lBase == null )
		{
			// System.out.println( "Error: Must have an extract_directory tag." );
			fatalErrorMsg( kFName, "Error: Must have an extract_directory tag." );
			System.exit(1);
		}
		fSourceFieldName = lBase.getAttributeValue( SOURCE_FIELD_NAME );
		fDestinationFieldName = lBase.getAttributeValue( DESTINATION_FIELD_NAME );
		if( fSourceFieldName == null || fDestinationFieldName == null ||
			fSourceFieldName.trim().equals("") || fDestinationFieldName.trim().equals("")
			)
		{
			// System.out.println( "Error: extract_directory tag must have source and destination field name attributes." );
			fatalErrorMsg( kFName, "Error: extract_directory tag must have source and destination field name attributes." );
			System.exit(1);
		}
	}

	public void run()
	{
		final String kFName = "run";	
		try
		{
			// Main while loop
			while( true )
			{
			    mWorkUnit = null;
				// Get a work unit
				// WorkUnit lWorkUnit = dequeue( fReadQueue );
				mWorkUnit = dequeue( fReadQueue );

				// Get the source URL
				String lSourceURLStr = mWorkUnit.getUserFieldText(
					fSourceFieldName
					);

				// If we found it, process it
				if( lSourceURLStr != null )
				{
					// Call our local extract function
					String lResultDirStr = extractDirFromURL( lSourceURLStr );
					// Did we get something back?
					if( lResultDirStr != null &&
						! lResultDirStr.trim().equals("")
						)
					{
						// Add it to the work unit as a text field
					    mWorkUnit.addNamedField( fDestinationFieldName,
							lResultDirStr.trim()
							);
					}
				}   // End if we found a source URL

				// We always re-queue the work unit
				enqueue( fWriteQueue, mWorkUnit );

				fCounter ++;
				if( fCounter % kReportInterval == 0 )
				{
					// System.out.println( "URLDirExtractor: Processed '" + fCounter +
					//	"' records." );
					infoMsg( kFName, "Processed '" + fCounter +
					"' records." );
				}


			}   // End main while loop
		}
		catch( InterruptedException ie )
		{
		}
	}

	String extractDirFromURL( String inURL )
	{
		// bypass some of the nullish edge cases
		if( inURL == null )
			return null;
		inURL = inURL.trim();
		if( inURL.equals("") )
			return inURL;

		// Setup the return value in advance
		String outURL = inURL;

		// look for the opening double slash
		int doubleSlashAt = inURL.indexOf( "//" );
		int firstSingleSlashAt;
		if( doubleSlashAt >= 0 )
			firstSingleSlashAt = inURL.indexOf( '/', doubleSlashAt+2 );
		else
			firstSingleSlashAt = inURL.indexOf( '/' );

		// At this point we know where the first
		// single slash is
		// If there's NO first single slash then we know that:
		// 1: it's a server only url
		// 2: that does not have a slash
		// So in that case we should add it and return
		// the results

		// If there is no ending slash
		if( firstSingleSlashAt < 0 )
		{
			// Just return it
			return outURL;
		}

		// Now find the last slash in the URL
		int lastSingleSlashAt = inURL.lastIndexOf( '/' );
		// Sanity check that we did find one
		if( lastSingleSlashAt < 0 )
			return outURL;

		// If it's at the end, we're OK, just drop
		// the final slash and return it
		if( lastSingleSlashAt == inURL.length()-1 )
		{
			outURL = outURL.substring( 0, lastSingleSlashAt );
			return outURL;
		}

		// OK we have a path section of the URL
		// and it doesn't end in a slash
		// Lets look at it further

		// Grab the final portion of the path
		String finalPath = inURL.substring( lastSingleSlashAt+1 );

		// If it has a dot OR ? OR # in it we probably have
		// a file name or cgi call that should be dropped
		if( finalPath.indexOf('.') >= 0 ||
			finalPath.indexOf('?') >= 0 ||
			finalPath.indexOf('#') >= 0
			)
		{
			outURL = outURL.substring( 0, lastSingleSlashAt );
			return outURL;
		}

		// OK at this point we know there is a
		// final bit of path, and it doesn't seem
		// to be a a file name or CGI call so it
		// really looks like a plain old DIR reference
		// with no slash, so it's already bare, so just
		// return it
		return outURL;

	}

	private void __sep__Member_vars_and_Constants__() {}
	///////////////////////////////////////////////////////////////////

	private static long fCounter;
	private static final long kReportInterval = 1000;


	Queue fReadQueue;
	Queue fWriteQueue;

	String fSourceFieldName;
	//String fFragmentFieldName;
	String fDestinationFieldName;

	public static final String BUILD_TAG_NAME = "extract_directory";
	public static final String SOURCE_FIELD_NAME = "source_field";
	public static final String DESTINATION_FIELD_NAME = "destination_field";
	WorkUnit mWorkUnit;
}
