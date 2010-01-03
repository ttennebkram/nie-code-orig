/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Kevin-Neil Klop
 *
 *  $Id: NoRepeatURLFilterV2.java,v 1.1 2004/03/03 20:00:49 mbennett Exp $
 *
 *  $Log: NoRepeatURLFilterV2.java,v $
 *  Revision 1.1  2004/03/03 20:00:49  mbennett
 *  Source tree from niesrv 226, from Feb 2004
 *
 *  Revision 1.7  2001/08/31 16:29:50  kevin
 *  Implemented new WorkUnits stuff into these processors.
 *
 *  Revision 1.6  2001/08/27 22:15:09  kevin
 *  Altered to match new constructor for processors.
 *
 *  Revision 1.5  2001/08/24 17:11:53  kevin
 *  Added new constructor parameter - String inID
 *
 *  Revision 1.4  2001/08/21 03:56:59  kevin
 *  re-indented to match current NIE, Inc. style giude.
 *  No code changes.
 *
 *  Revision 1.3  2001/08/15 20:17:57  kevin
 *  Minor changes in preparation for the Major language/specification.
 *
 *  Revision 1.2  2001/08/03 20:51:03  kevin
 *  First working version.
 *
 *  Revision 1.1  2001/07/31 21:46:35  kevin
 *  Initial revision
 *
 *
 */

//package nie.processors;
package nie.pump.processors;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

import java.util.*;
import org.jdom.Element;
import java.net.*;

public class NoRepeatURLFilterV2 extends Processor
{
	public String kClassName() { return "NoRepeatURLFilterV2"; }


	/////////////////////
	//
	// Private comparator class used in the TreeSet
	// for comparing the URLs
	//
	/////////////////////

	private class URLComparatorV2 implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{
			return ( (String)o1 ).compareTo( (String)o2 );
		}

		public boolean equals( Object o1, Object o2 )
		{
			return ( (String)o1 ).equals( (String)o2 );
		}
	}

	/////////////////////
	//
	// Here's the actual constructor for the class.
	//
	/////////////////////

	public NoRepeatURLFilterV2( Application inApplication,
				  Queue[] inReadFromQueue,
				  Queue[] inWriteToQueue,
				  Queue[] inUsesQueue,
				  Element inParameters,
				  String inID )
	{
		super( inApplication, inReadFromQueue, inWriteToQueue, inUsesQueue, inParameters, inID );
		
		final String kFName = "constructor";
		if( (inReadFromQueue == null) ||
			(inReadFromQueue.length == 0) ||
			(inReadFromQueue[0] == null ) )
		{
			// System.err.println( "Invalid input queue specified for NoRepeatURLFilterV2." );
			fatalErrorMsg( kFName, "Invalid input queue specified for NoRepeatURLFilterV2." );
			System.exit( -1 );
		}

		if( (inWriteToQueue == null) ||
			(inWriteToQueue.length == 0) ||
			(inWriteToQueue[0] == null ) )
		{
			// System.err.println( "Invalid output queue specified for NoRepeatURLFilterV2." );
			fatalErrorMsg( kFName, "Invalid output queue specified for NoRepeatURLFilterV2." );
			System.exit( -1 );
		}

		fReadQueue = inReadFromQueue[0];
		fWriteQueue = inWriteToQueue[0];

		if( inParameters != null )
		{
			try
			{
				fJdh = new JDOMHelper( inParameters );
			}
			catch(Exception e)
			{
				// System.err.println( "NoRepeatURLFilterV2: got exc from jdh " +
				//	e );
				fatalErrorMsg( kFName, "NoRepeatURLFilterV2: got exc from jdh " +
						e );
				System.exit(1);
			}
		}
		fHaveDoneInit = false;
		getAllLowerCase();
		getHostToLowerCase();
		getCheckFieldName();
		fHaveDoneInit = true;

		fAlreadySeenURLs = Collections.synchronizedSet(
			new TreeSet( new URLComparatorV2() )
			);

	}

	/////////////////////////////
	//
	// This is where the work gets done.
	//
	/////////////////////////////

	public void run()
	{
		
		final String kFName = "run";
		// WorkUnit lWorkUnit = null;
		//String lURLString = null;
		//Element lElement = null;

		try
		{
		    mWorkUnit = null;
			while( true )
			{
				// Get a work unit
				// lWorkUnit = (WorkUnit)dequeue( fReadQueue );
				mWorkUnit = (WorkUnit)dequeue( fReadQueue );

				// Process it
				// WorkUnit lResultWorkUnit = processWorkUnit(
				//	lWorkUnit
				//	);
				mWorkUnit = processWorkUnit(
				        mWorkUnit
						);

				// If we got a result, enqueue it
				if( mWorkUnit != null )
					enqueue( fWriteQueue, mWorkUnit );

				// Clear our references to objects
				mWorkUnit = null;
			}
		}
		catch( InterruptedException ie )
		{
		}
	}

	WorkUnit processWorkUnit( WorkUnit inWU )
	{
		final String kFName = "processWorkUnit";
		//String lURLString = null;
		//Element lElement = null;
		WorkUnit outWU = null;

		String lURLString = getURL( inWU );

debugMsg( kFName, "Checking (1) URL '" + lURLString + "'" );

		if( lURLString == null || lURLString.trim().equals("") )
		{
			// System.err.println( "NoRepeatURLFilterV2: processWorkUnit:" +
			//	" No URL found, work unit will NOT be passed through."
			//	);
			mWorkUnit.errorMsg( this, kFName, "Was passed a null or empty string, ignoring"
					);
			return outWU;
		}
		lURLString = lURLString.trim();

		if( getAllLowerCase() )
		{
			lURLString = lURLString.toLowerCase();
		}
		else if( getHostToLowerCase() )
		{
			try
			{
				URL lURL = new URL(lURLString);
				String lHost = lURL.getHost();
				String lLowerHost = lHost.toLowerCase();
				if( ! lLowerHost.equals( lHost ) )
				{
					//	System.err.println( "Aha!" );
					// Find out where the host is
					int lPositionOfHost = lURLString.indexOf( lHost );
					// Start forming the new URL
					String newURL = "";
					// Get what's left of the host, if any
					if( lPositionOfHost > 0 )
						newURL = lURLString.substring( 0, lPositionOfHost );
					// Add the new lower case host
					newURL = newURL + lLowerHost;
					// Get what's to the right of the host, if any
					if( (lPositionOfHost + lHost.length()) <
						(lURLString.length() - 1)
						)
					{
						newURL = newURL +
							lURLString.substring(
								lPositionOfHost + lHost.length()
							);
					}
					// Save the results
					lURLString = newURL;
				}
			}   // End of try block
			catch (Exception e)
			{
				// System.err.println(
				//	"Error: NoRepeatURLFilterV2:processWorkUnit:" +
				//	" Got an exception while trying to normalize URL to have" +
				//	" a lower case host name, url='" + lURLString + "'" +
				//	" This work unit will be dropped." +
				//	" Exception was '" + e + "'"
				//	);
				mWorkUnit.errorMsg( this, kFName, "Got an exception while trying to normalize URL to have" +
						" a lower case host name, url='" + lURLString + "'" +
						// " This work unit will be dropped." +
						" Exception was '" + e + "'"
						);
				lURLString = null;
			}
		}

		// By now we have a normalized lURLString

		debugMsg( kFName,
				"fAlreadySeenURLs.size=" + fAlreadySeenURLs.size()
				+ ", Checking (2) URL '" + lURLString + "'"
				);

		// If we haven't seen it before
		if( lURLString != null && ! fAlreadySeenURLs.contains( lURLString ) )
		{
			// Make a note of the new URL
			fAlreadySeenURLs.add( lURLString );
			// and we WILL return this work unit
			outWU = inWU;
		}
		else {
			// mWorkUnit.errorMsg( this, kFName,
			//        "Wound up with null result."
			//		);
			// outWU = mWorkUnit;
			
			// Else we've seen it, just don't emit this record
			outWU = null;
		    
		}

		// Return the resulting work unit
		// Will be NULL if we've seen this URL before OR if we had an error
		return outWU;
	}

	String getURL( WorkUnit inWU )
	{
		if( inWU == null )
			return null;
		String urlField = getCheckFieldName();
		if( urlField == null || urlField.trim().equals("") )
			return null;
		urlField = urlField.trim();

		return inWU.getUserFieldText( urlField );
	}


	String getCheckFieldName()
	{
		if( fHaveDoneInit )
			return fCheckFieldName;
		else
		{
			if( fJdh == null )
				fCheckFieldName = DEFAULT_CHECK_FIELD;
			else
			{
				String tmpString = fJdh.getTextByPath(
					CHECK_FIELD
					);
				if( tmpString == null || tmpString.trim().equals("") )
					fCheckFieldName = DEFAULT_CHECK_FIELD;
				else
					fCheckFieldName = tmpString.trim();
			}
		}
		return fCheckFieldName;
	}


	boolean getAllLowerCase()
	{
		if( fHaveDoneInit )
			return fForceToLowerCase;
		else
		{
			if( fJdh == null )
				fForceToLowerCase = DEFAULT_FORCE_TO_LOWER_CASE;
			else
			{
				Element tmp = fJdh.findElementByPath(
					FORCE_TO_LOWER_CASE
					);
				// If we find it, then it's true
				fForceToLowerCase = tmp != null ? true :
					DEFAULT_FORCE_TO_LOWER_CASE ;
			}
			return fForceToLowerCase;
		}
	}

	boolean getHostToLowerCase()
	{
		if( fHaveDoneInit )
			return fForceHostToLowerCase;
		else
		{
			if( fJdh == null )
				fForceHostToLowerCase = DEFAULT_FORCE_HOST_TO_LOWER_CASE;
			else
			{
				Element tmp = fJdh.findElementByPath(
					FORCE_HOST_TO_LOWER_CASE
					);
				// If we find it, then it's true
				fForceHostToLowerCase = tmp != null ? true :
					DEFAULT_FORCE_HOST_TO_LOWER_CASE ;
			}
			return fForceHostToLowerCase;
		}
	}

	private JDOMHelper fJdh;

	private boolean fHaveDoneInit;

	private String fCheckFieldName;
	private boolean fForceToLowerCase;
	private boolean fForceHostToLowerCase;

	private Queue fReadQueue;
	private Queue fWriteQueue;

	private Set fAlreadySeenURLs;
	
	WorkUnit mWorkUnit;

	static final String CHECK_FIELD = "url_field";
	//static final String CHECK_FIELD_SHORT = "src";
	static final String DEFAULT_CHECK_FIELD = "url";

	static final String FORCE_HOST_TO_LOWER_CASE = "force_host_to_lower_case";
	static final boolean DEFAULT_FORCE_HOST_TO_LOWER_CASE = true;

	static final String FORCE_TO_LOWER_CASE = "force_to_lower_case";
	static final boolean DEFAULT_FORCE_TO_LOWER_CASE = false;

}

