//package nie.core;
package nie.pump.base;

import org.jdom.*;
import java.io.*;
import java.util.*;

import nie.core.JDOMHelper;
import nie.core.JDOMHelperException;

/**
 * Title:        DPump
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop and Mark Benett
 * @version 1.0
 */

public class DiskQueue extends Queue
{
	public DiskQueue( String inQueueName, DPump inDPump, Element inSpecification )
	{
		super( inQueueName, inSpecification );
		fDPump = inDPump;

		if( inSpecification == null )
			fDirectoryName = "";

		Element lDirectoryElement = JDOMHelper.findElementByPath( inSpecification, DIRECTORY_NAME_TAG );
		if( lDirectoryElement == null )
			fDirectoryName = "";
		else
			fDirectoryName = lDirectoryElement.getTextTrim();

		File lQueueDirectory = new File( fDirectoryName );
		if( !lQueueDirectory.exists() )
			lQueueDirectory.mkdirs();

		lQueueDirectory = null;

		fShouldDelete = true;
		Element lShouldNotDeleteElement = JDOMHelper.findElementByPath( inSpecification, SHOULD_NOT_DELETE_TAG );
		if( lShouldNotDeleteElement != null )
			fShouldDelete = false;
	}

	void bad_initialization()
	{
		System.out.println( "You must specify a disk directory for queue " + fName );
		System.exit( -1 );
	}

	public String getProcessorName()
	{
		return fDPump.getProcessorName();
	}

	public Application getApplication()
	{
		return fDPump;
	}


	public synchronized void putNextWorkUnit( WorkUnit inWorkUnit )
	{
		String lFileName = new String( fDirectoryName + "/" + TEMP_FILE_PREFIX + getName() + '.' + getNumberOfEnqueueOperations() + TEMP_FILE_SUFFIX );

		long lNumEnqueues = getNumberOfEnqueueOperations();
		long lNumDequeues = getNumberOfDequeueOperations();

		if( getNumberOfEnqueueOperations() == getNumberOfDequeueOperations() )
		{
			System.err.println( "Queue " + this.getName() + ": Overflow.  System shutting down." );
			System.exit( -1 );
		}

		inWorkUnit.writeToFile( lFileName );
	}

	public synchronized WorkUnit getNextWorkUnit()
	{
		try
		{
			if( !isEmpty() )
			{
				long lNumDequeues = getNumberOfDequeueOperations();
				if( ++lNumDequeues == Long.MAX_VALUE )
					lNumDequeues = 0;

				String lFileName = new String( fDirectoryName + "/" + TEMP_FILE_PREFIX + getName() + '.' + lNumDequeues + TEMP_FILE_SUFFIX );
				File lFile = new File( lFileName );
				if( lFile.exists() )
				{
					WorkUnit lWorkUnit = new WorkUnit( lFile );
					if( fShouldDelete )
						lFile.delete();
					return lWorkUnit;
				}
			}
		}
		catch( Exception e )
		{
		}

		return null;
	}

	static final String TEMP_FILE_PREFIX = "DPUMP_";
	static final String TEMP_FILE_SUFFIX = ".xml";
	static final String DIRECTORY_NAME_TAG = "directory_name";
	static final String SHOULD_NOT_DELETE_TAG = "should_not_delete";

	DPump fDPump;
	String fDirectoryName;
	boolean fShouldDelete;
}
