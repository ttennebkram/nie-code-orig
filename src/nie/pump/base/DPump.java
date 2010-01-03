/*
 *  Copyright 2001 by New Idea Engineering, Inc.
 *  Written by Kevin-Neil Klop
 *
 *  To Do:
 *	Should probably convert this over to using the JDOMHelper class
 *
 *  $Id: DPump.java,v 1.1 2004/03/03 20:00:51 mbennett Exp $
 *
 *  $Log: DPump.java,v $
 *  Revision 1.1  2004/03/03 20:00:51  mbennett
 *  Source tree from niesrv 226, from Feb 2004
 *
 *  Revision 1.1  2001/10/18 23:06:40  kevin
 *  Initial revision
 *
 *  Revision 1.13  2001/09/03 15:31:45  kevin
 *  Was passing in the wrong number of arguments.
 *
 *  Revision 1.12  2001/09/03 00:11:03  kevin
 *  Created the infrastructure needed for the ExitProcessor.
 *
 *  Revision 1.11  2001/08/31 16:43:39  kevin
 *  Had to add some import statements to get compilations to work cleanly.
 *
 *  Revision 1.10  2001/08/31 14:37:27  kevin
 *  Added WorkUnit processing
 *
 *  Revision 1.9  2001/08/27 22:13:40  kevin
 *  Worked on the new configuration stuff.  It now instantiates all the processors before starting any of them.
 *  This allows the constructor of one processor to do some configuration altering things before any subsequent
 *  processor is executed.
 *
 *  Revision 1.8  2001/08/21 03:46:05  kevin
 *  Reindented to meet current NIE style guidelines.
 *  No code changes.
 *
 *  Revision 1.7  2001/08/15 18:01:20  kevin
 *  First working version of "expanded" DPump specification language
 *
 *  Revision 1.6  2001/08/09 14:35:25  kevin
 *  Last working DPump before changing to the more expanded DPump
 *  specification language.
 *
 *  Revision 1.5  2001/08/07 01:14:56  kevin
 *  Slimmed the system down, changed the processors to a less-specific, more generic
 *  format.
 *
 *  Revision 1.4  2001/08/06 07:41:45  kevin
 *  changed the queue lists in the <thread statement to use whitespace instead of commas.
 *
 *  Revision 1.3  2001/08/03 20:50:12  kevin
 *  First working version
 *
 *  Revision 1.2  2001/08/01 14:59:42  kevin
 *  Daily checkin
 *
 *  Revision 1.1  2001/07/31 21:44:20  kevin
 *  Initial revision
 *
 *
 */

//package nie.core;
package nie.pump.base;

import org.jdom.*;
import org.jdom.input.*;
import java.util.*;
import java.lang.Integer;
import java.lang.reflect.*;

import nie.core.JDOMHelper;
import nie.core.JDOMHelperException;
import nie.core.RunLogBasicImpl;
import nie.core.RunLogInterface;
//import nie.core.Application;
//import nie.core.Queue;
//import nie.core.Processor;
import nie.pump.base.Application;
import nie.pump.base.Queue;
import nie.pump.base.Processor;

public class DPump extends Application
{
    static String kClassName() { return "DPump"; }

    ///////////////////////////////////
	//
	// This is the class that maps threads and processor ids.
	//
	//////////////////////////////////

	public class ProcessorEntry
	{
		public ProcessorEntry( Thread inThread, Processor inProcessor )
		{
			fThread = inThread;
			fProcessor = inProcessor;
		}

		public Thread getThread()
		{
			return fThread;
		}

		public Processor getProcessor()
		{
			return fProcessor;
		}

		public String getName()
		{
			return getProcessor().getID();
		}

		Thread fThread = null;
		Processor fProcessor = null;
	}

	/////////////////////////////////
	//
	// This is the exception that is
	// thrown if there's a problem with
	// the spider.
	//
	/////////////////////////////////

	public class DPumpException extends Exception
	{
		public DPumpException( String inMessage )
		{
			super( inMessage );
		}
	}

	////////////////////////////////
	//
	// Constructors for the DPump.
	//
	// There are two constructors.  In one, you
	// give it the URI of the XML feed to read.
	// That's the easiest way to read a config file.
	//
	// However, if the spider settings are part of a
	// already existing and read configuration file that
	// has been put into a JDOM tree, pass it the element
	// that is the root of the spider configuration
	// instead of the URI
	//
	////////////////////////////////

	public DPump( String inURI ) throws DPumpException
	{
		SAXBuilder lSAXBuilder = new SAXBuilder();

		try
		{
			Document lDocument = lSAXBuilder.build(inURI);
			if( lDocument != null )
				common_init( lDocument.getRootElement() );
		}
		// catch( JDOMException je )
		catch( Exception je )
		{
			System.err.println( "Received JDOM error building document." );
			System.err.println( je );
			throw new DPumpException( "jdom:Building document for \"" + inURI + '"' );
		}
	}

	public DPump( Element inElement ) throws DPumpException
	{
		common_init( inElement );
	}

	///////
	//
	// This is the initialization that is common no matter which
	// constructor is used.
	//
	///////

	private void common_init( Element inElement ) throws DPumpException
	{
		if( inElement == null )
			throw new DPumpException( "Null element passed to DPump initialization." );

		fProcessorsThreadGroup = new ThreadGroup( "Processors" );
		fElement = inElement;
		fThreads = new Vector();

		// mbennett add queue/process mapping
		fProcToQMap = new Hashtable();
		fQToProcMap = new Hashtable();
		fProcToQMapByRole = new Hashtable();
		fQToProcMapByRole = new Hashtable();
	}

	///////////////////////////////
	//
	// Find the name of the current processor.  Note that this must be
	// issued from either the processor's thread OR the processor's
	// thread must be passed in.
	//
	///////////////////////////////

	public String getProcessorName()
	{
		return getProcessorName( Thread.currentThread() );
	}

	public String getProcessorName( Thread inThread )
	{
		for( Iterator lIterator = fThreads.iterator(); lIterator.hasNext(); )
		{
			ProcessorEntry lEntry = (ProcessorEntry)lIterator.next();
			if( lEntry.getThread() == inThread )
				return lEntry.getName();
		}
		return null;
	}
	public void showThreads()
	{
	    final String kFName = "showThreads";
	    statusMsg( kFName, "fThreads.size()="+fThreads.size() );
	    statusMsg( kFName, "\nThread.currentThread()=" + Thread.currentThread() );
	    
		for( Iterator lIterator = fThreads.iterator(); lIterator.hasNext(); )
		{
			ProcessorEntry lEntry = (ProcessorEntry)lIterator.next();
		    // statusMsg( kFName, "process entry = " + lEntry );
		    statusMsg( kFName, "\nentry getThread() = " + lEntry.getThread() );
			
		}
	}
	
	public Thread getProcessorThread( String inName )
	{
		for( Iterator lIterator = fThreads.iterator(); lIterator.hasNext(); )
		{
			ProcessorEntry lEntry = (ProcessorEntry)lIterator.next();
			if( lEntry.getName().equals( inName ) )
				return lEntry.getThread();
		}

		return null;
	}

	public ThreadGroup getThreadGroup()
	{
		return fProcessorsThreadGroup;
	}

	///////////////////////////////
	//
	// Shutdown the system by sending terminate messages
	// to all the processors.
	//
	///////////////////////////////

	public void shutdown()
	{
		if( getThreadGroup() != null )
			getThreadGroup().interrupt();
	}

	///////////////////////////////
	//
	// This is where all the instantiation happens
	// each thread instruction instantiates another
	// thread running the specified class.  NOTE:
	// the class MUST be a descendant of the Processor
	// class!!!
	//
	///////////////////////////////

	public void run()
	{
		try
		{
			instantiateQueues( fElement.getChildren( QUEUES_TAG_NAME ));
			instantiateProcessors(fElement.getChild( PROCESSORS_TAG_NAME ));
		}
		catch( DPumpException se )
		{
			System.err.println( "DPumpException occurred..." );
			System.err.println( se.getMessage() );
			se.printStackTrace();
			System.exit( -1 );
		}
	}

	///////////////////////////////
	//
	// Creates the queues specified in the configuration file.
	//
	///////////////////////////////

	private void instantiateQueues( List inQueueElementsList ) throws DPumpException
	{
		if( fQueueList == null )
			fQueueList = new Hashtable();

		if( inQueueElementsList != null )
			for( Iterator i = inQueueElementsList.iterator(); i.hasNext(); )
			{
				Element lQueueSpec = (Element)i.next();
				instantiateQueue( lQueueSpec );
			}
	}


	///////
	//
	// Given a single queue specification element, instantiate that
	// queue.
	//
	///////

	private void instantiateQueue( Element inElement ) throws DPumpException
	{
		final String kFName = "instantiateQueue";
		boolean lSingleValue = false;
		String lQueueName = null;

		//
		// Get the queue's class name.  If it isn't specified, use the
		// standard "Queue" class.
		//

		String lQueueClassName = inElement.getAttributeValue( CLASSNAME_ATTRIBUTE_NAME );
		if( lQueueClassName == null )
			lQueueClassName = "BasicQueue";

		debugMsg( kFName, "Q class='" + lQueueClassName + "'" );
		
		//
		// Try and find the class for the queue.  A Queue constructor
		// is allowed to have either of the following constructors:
		//
		//   class MyQueue( String inName, DPump inDPump, Element inSpecification );
		//   class MyQueue( String inName, DPump inDPump );
		//

		try
		{
			//
			// First try finding a constructor with two parameters.
			//

			Class lQueueClass;
			lQueueClass = null;

			try
			{
				lQueueClass = Class.forName( lQueueClassName );
			}
			catch( ClassNotFoundException cnfe )
			{
				try
				{
					lQueueClass = Class.forName( CORE_PACKAGE_PATH_NAME + lQueueClassName );
				}
				catch( ClassNotFoundException cnfe2 )
				{
					try
					{
						lQueueClass = Class.forName( PROCESSORS_PACKAGE_PATH_NAME + lQueueClassName );
					}
					catch( ClassNotFoundException cnfe3 )
					{
						System.err.println( "Could not find specified queue class \"" + lQueueClassName + '"' );
						System.exit( -1 );
					}
				}
			}

			Class[] lQueueSignature = new Class[3];
			lQueueSignature[0] = lQueueClassName.getClass();
			lQueueSignature[1] = this.getClass();
			lQueueSignature[2] = inElement.getClass();
			Constructor lQueueConstructor = null;

			try
			{
				lQueueConstructor = lQueueClass.getConstructor( lQueueSignature );
			}
			catch( NoSuchMethodException nsme )
			{
				//
				// If that didn't work, try finding a
				// constructor with only one parameter.
				//

				lQueueSignature = new Class[2];
				lQueueSignature[0] = lQueueClassName.getClass();
				lQueueSignature[1] = this.getClass();

				try
				{
					lQueueConstructor = lQueueClass.getConstructor( lQueueSignature );
				}
				catch( NoSuchMethodException nsme2 )
				{
					//
					// Neither constructor exists.  Can
					// not instantiate the queue and,
					// therefore, can not instantiate the
					// entire DPump since it's likely that
					// SOME processors will be depending
					// on this queue.
					//

					System.err.println( "Could not find appropriate constructor for Queue class \"" +
							lQueueClassName +
							"\".\nConstructor that accepts either (String, Element) " +
							"or (String) must be present." );
					System.exit( -1 );
				}

				lSingleValue = true;
			}

			//
			// For all the queues specified using this class,
			// instantiate each queue.
			//

			Object[] lParams = null;
			if( lSingleValue )
				lParams = new Object[2];
			else
				lParams = new Object[3];

			List lQueueDOMList = inElement.getChildren( QUEUE_TAGS_NAME );
			for( Iterator lQueueDOMListIterator = lQueueDOMList.iterator(); lQueueDOMListIterator.hasNext(); )
			{
				Element lElement = (Element)lQueueDOMListIterator.next();
				lQueueName = lElement.getAttributeValue( NAME_ATTRIBUTE_NAME );
				if( lQueueName == null )
				{
					System.err.println( "Queue must have a name!" );
					System.exit( -1 );
				}

				Queue lQueue = null;
				lParams[0] = lQueueName;
				lParams[1] = this;
				if( !lSingleValue )
				{
					lParams[2] = lElement;
				}

				lQueue = (Queue)lQueueConstructor.newInstance( lParams );
				if( lQueue != null )
					fQueueList.put( lQueueName, lQueue );
				else
					throw new DPumpException("Could not create queue " + lElement.getTextTrim() );
			}
		}
		catch( InstantiationException ie )
		{
			System.err.println( "Could not instantiate queue \"" + lQueueName + "\"" );
			System.exit( -1 );
		}
		catch( IllegalAccessException iae )
		{
			System.err.println( "Queue \"" + lQueueName + "\" had an access violation." );
			System.exit( -1 );
		}
		catch( InvocationTargetException ite )
		{
			System.err.println( "Queue \"" + lQueueName + "\" received an \"InvocationTargetException\", whatever that means." );
			System.err.println( ite.getMessage() );
			ite.printStackTrace();
			System.exit(-1);
		}

		return;
	}

	///////////////////////////////
	//
	// Creates the processors specified in the configuration file.
	//
	///////////////////////////////

	private void instantiateProcessors( Element inElement ) throws DPumpException
	{
		List lProcessorDOMList = inElement.getChildren( PROCESSOR_TAGS_NAME );

		//
		// Handle all <processor ...> tags within this <processors> tag.
		//

		int lProcessorStatements = 0;
		int lInstances = 1;
		int lProcessorCounter = 0;

		// For each process that was defined in the dpump config
		for( Iterator lProcessorsIterator = lProcessorDOMList.iterator(); lProcessorsIterator.hasNext(); )
		{
			lInstances = 1; // Default number of instances

			/////
			//
			// Get the next specified processor specification
			//
			/////

			Element lElement = (Element)lProcessorsIterator.next();

			/////
			//
			// The class definition is required - make sure it's there.
			//
			/////

			String lClassName = lElement.getAttributeValue( CLASSNAME_ATTRIBUTE_NAME );
			if( lClassName == null )
				throw new DPumpException( "Class Name not specified for thread." );

			/////
			//
			// Get the (optional) number of threads of this sort to instantiate
			//
			/////

			String lInstancesString = lElement.getAttributeValue( INSTANCES_PARAMETER );
			if( lInstancesString != null )
				lInstances = Integer.parseInt( lInstancesString );

			String lID = lElement.getAttributeValue( NAME_ATTRIBUTE_NAME );
			if( lID == null )
				lID = lClassName;

			///////
			//
			// Figure out the queue lists.
			//
			///////

			Queue[] lReadQueues = getQueueList(
				lElement.getChild( READ_FROM_QUEUES_TAG_NAME )
				);
			Queue[] lWriteQueues = getQueueList(
				lElement.getChild( WRITES_TO_QUEUES_TAG_NAME )
				);
			Queue[] lUsesQueues = getQueueList(
				lElement.getChild( USES_QUEUES_TAG_NAME )
				);

			///////
			//
			// Find the constructor for the processor.
			//
			///////

			if( fConstructorSignature == null )
				buildConstructorSignature( lElement ); // Need to pass in an Element so that buildConstructor can get the class of it

			Class lNewProcessor;
			lNewProcessor = null;
			Constructor lProcessorConstructor;
			lProcessorConstructor = null;

			try
			{
				lNewProcessor = Class.forName( lClassName );
			}
			catch( ClassNotFoundException e )
			{
				try
				{
					lNewProcessor = Class.forName( CORE_PACKAGE_PATH_NAME + lClassName );
				}
				catch( ClassNotFoundException cnfe2 )
				{
					try
					{
						lNewProcessor = Class.forName( PROCESSORS_PACKAGE_PATH_NAME + lClassName );
					}
					catch( ClassNotFoundException cnfe3 )
					{
						System.err.println( e.getMessage() );
						e.printStackTrace(System.err);
						System.exit( -1 );
					}
				}
			}

			try
			{
				lProcessorConstructor = lNewProcessor.getConstructor( fConstructorSignature );
			}
			catch( NoSuchMethodException nsme )
			{
				System.err.println( nsme.getMessage() );
				nsme.printStackTrace(System.err);
				System.exit( -1 );
			}

			///////
			//
			// Instantiate all instances of the processor.
			//
			///////

			Element lSpecificParameter = null;
			Element lDefaultParameter = null;

			for( int i = 0; i < lInstances; i++ )
			{
				lSpecificParameter = null;
				lDefaultParameter = null;

				List lParametersList = lElement.getChildren( PARAMETERS_TAGS_NAME );

				// Try to find parameters for this instance...

				if( lParametersList != null )
				{
					for( Iterator lParametersIterator = lParametersList.iterator(); lParametersIterator.hasNext(); )
					{
						Element lParameterElement = (Element)lParametersIterator.next();
						String lInstanceAttributeString = lParameterElement.getAttributeValue( INSTANCE_ATTRIBUTE_NAME );
						if( lInstanceAttributeString == null )
							lDefaultParameter = lParameterElement;
						else
						{
							int lInstanceNumber = Integer.parseInt( lInstanceAttributeString );
							if( lInstanceNumber == i )
							{
								if( lSpecificParameter != null )
									lSpecificParameter = lParameterElement;
								else
								{
									System.out.println( "Parameters specification for instance " + lInstanceNumber + " specified more than once." );
									System.exit( -1 );
								}
							}
						}

					}
				}
				else
					lSpecificParameter = lDefaultParameter = null;

				if( (lSpecificParameter == null) &&
					(lDefaultParameter != null) )
					lSpecificParameter = lDefaultParameter;

				String lSpecificID;

				// Form the process name
				lSpecificID = lID + "." + lProcessorCounter;
				if( lInstances > 1 )
					lSpecificID = lSpecificID + "." + i;

				try
				{
					Processor lProcessor =
						(Processor) lProcessorConstructor.newInstance(
							buildConstructorParameters( lReadQueues,
								lWriteQueues, lUsesQueues, lSpecificParameter,
								lSpecificID
								)
							);
					if( lProcessor != null )
					{
						Thread lThread = new Thread( getThreadGroup(), lProcessor );
						if( lThread == null )
						{
							System.err.println( "Could not instantiate a thread for Processor \"" + lClassName + "\"." );
							System.exit( -1 );
						}
						fThreads.add( new ProcessorEntry( lThread, lProcessor ) );

						// mbennett preserve the mapping
						recordProcessorQueueMappings( lSpecificID,
							lReadQueues, lWriteQueues, lUsesQueues
							);
					}
					else
					{
						System.err.println( "Processor \"" + lClassName + "\" failed to start." );
						System.exit( -1 );
					}
				}
				catch( InstantiationException ie )
				{
					throw new DPumpException( ie.getMessage() );
				}
				catch( IllegalAccessException iae )
				{
					throw new DPumpException( iae.getMessage() );
				}
				catch( InvocationTargetException ite )
				{
					System.err.println( "Error instantiating class:" );
					System.err.println( ite );
					System.err.println( ite.getMessage() );
					ite.printStackTrace();

					throw new DPumpException( "Received InvocationTargetException for \"" + lClassName + '"' );
				}
			}

			lProcessorCounter++;
		}   // End for each process that was defined in the dpump config


		// Start the processes
		for( Iterator lIterator = fThreads.iterator(); lIterator.hasNext(); )
		{
			ProcessorEntry lThreadToStart;
			lThreadToStart = (ProcessorEntry)lIterator.next();
			lThreadToStart.getThread().start();
		}
	}

	////////////
	//
	// Build the list of queues that will be passed to this processor.
	//
	////////////

	private Queue[] getQueueList( Element inTopElement ) throws DPumpException
	{
		if( inTopElement != null )
		{
			Vector lQueueVector = new Vector();

			List lQueueList = inTopElement.getChildren( QUEUE_CONNECTION_NAME );
			for( Iterator lQueueListIterator = lQueueList.iterator(); lQueueListIterator.hasNext(); )
			{
				Element lQueueElement = (Element)lQueueListIterator.next();
				String lQueueElementName = lQueueElement.getAttributeValue( NAME_ATTRIBUTE_NAME );

				if( lQueueElementName == null )
				{
					System.out.println( "queue_connection without required \"name\" attribute." );
					System.exit( -1 );
				}

				Queue lQueue = Queue.findQueueByName( lQueueElementName );
				if( lQueue != null )
					lQueueVector.add( lQueue );
				else
				{
					System.err.println( "Invalid queue specified, \"" + lQueueElementName + '"' );
					System.exit( -1 );
				}
			}

			if( lQueueVector.size() > 0 )
			{
				Queue[] lReturnedQueueList = new Queue[ lQueueVector.size() ];
				if( lReturnedQueueList != null )
				{
					for( int i = 0; i < lQueueVector.size(); i++ )
					lReturnedQueueList[i] = (Queue)lQueueVector.elementAt( i );
				}

				return lReturnedQueueList;
			}
		}
		return null;
	}

	////////////
	//
	// Build the constructor signature list for a processor.
	//
	////////////

	private void buildConstructorSignature( Element inElement ) throws DPumpException
	{
		try
		{
			Queue[] lExampleQueueList = new Queue[1];
			String lExampleString = new String("");

			fConstructorSignature = new Class[6];
			fConstructorSignature[0] = Class.forName( CORE_PACKAGE_PATH_NAME + "Application" );
			fConstructorSignature[1] = lExampleQueueList.getClass();
			fConstructorSignature[2] = fConstructorSignature[1];
			fConstructorSignature[3] = fConstructorSignature[2];
			fConstructorSignature[4] = inElement.getClass();
			fConstructorSignature[5] = lExampleString.getClass();
		}
		catch( Exception e )
		{
			throw new DPumpException( e.getMessage() );
		}
	}

	////////////
	//
	// Build the actual parameter list that will be passed to a processor constructor.
	//
	////////////

	private Object[] buildConstructorParameters( Queue[] inReadQueue, Queue[] inWriteQueue, Queue[] inUsesQueue, Element inParamElement, String inID )
	{
		Object[] lConstParams = new Object[6];
		lConstParams[0] = this;
		lConstParams[1] = inReadQueue;
		lConstParams[2] = inWriteQueue;
		lConstParams[3] = inUsesQueue;
		lConstParams[4] = inParamElement;
		lConstParams[5] = inID;

		return lConstParams;
	}

	////////////
	//
	// Get the Queue Collection used mainly by the ExitProcessor.
	//
	////////////

	public Hashtable getQueues()
	{
		return fQueueList;
	}

	////////////
	//
	// Get the list of processors
	//
	////////////

	public Hashtable getProcessors()
	{
		Vector lProcessorVector = getProcessorList();
		int lVectorSize = lProcessorVector.size();
		Hashtable lHashtable = new Hashtable();

		for( int i = 0; i < lVectorSize; i++ )
		{
			ProcessorEntry lProcessorEntry = (ProcessorEntry)lProcessorVector.elementAt( i );
			Processor lProcessor = lProcessorEntry.getProcessor();
			String lProcessorID = lProcessor.getID();
			lHashtable.put( lProcessorID, lProcessor );
		}

		return lHashtable;
	}

	// mbennett
	// Get connection information for various processes
	public List getQueuesForProcessor( String inProcName )
	{
		return getMapValues( fProcToQMap, inProcName );
	}
	public List getQueuesForProcessor( String inProcName, String inRole )
	{
		return getMapValues( fProcToQMapByRole,
			inProcName + PQ_KEY_DELIM + inRole
			);
	}
	public List getProcessorsForQueue( String inQueueName )
	{
		return getMapValues( fQToProcMap, inQueueName );
	}
	public List getProcessorsForQueue( String inQueueName, String inRole )
	{
		return getMapValues( fQToProcMapByRole,
			inQueueName + PQ_KEY_DELIM + inRole
			);
	}

	// Get the info in the form an XML snippet
	// Todo: rewrite to share code with getProcessesForQueueAsXML
	public JDOMHelper getQueuesForProcessorAsXML( String inProcName )
	{

		final boolean debug = false;

		// The main opening tag
		JDOMHelper outElem = null;
		try
		{
			outElem = new JDOMHelper( "<connections/>", null );
		}
		catch (JDOMHelperException e)
		{
			System.err.println( "ERROR: DPump:getQueuesForProcessorAsXML:"
				+ " Unable to create JDOM Helper."
				+ " Exception: " + e
				);
			return null;
		}

		String [] lRoles = new String[]
		{
			"reads_from", "writes_to", "uses"
		};

		// For each role
		for( int i=0; i<lRoles.length; i++ )
		{
			String role = lRoles[i];
			// Get a list of ajoining objects
			List others = getQueuesForProcessor( inProcName, role );
			if( others.size() <= 0 )
				continue;
			// Create the new branch
			Element newBranch = new Element( role );
			// For each related object
			for( Iterator it = others.iterator(); it.hasNext(); )
			{
				// Get the object's name
				String otherName = (String)it.next();

				if(debug) System.err.println( "Debug: DPump:getQueuesForProcessorAsXML:"
					+ " Adding info to tree:"
					+ " inProcName=" + inProcName
					+ " role=" + role
					+ " otherName=" + otherName
					);

				// create a leaf for it and then add the name to the branch
				Element newLeaf = new Element( "object" );
				newLeaf.addContent( otherName );
				newBranch.addContent( newLeaf );
			}
			// Add the branch to the tree
			outElem.addContent( newBranch );
		}

		return outElem;
	}

	// Get the info in the form an XML snippet
	// Todo: rewrite to share code with getQueuesForProcessAsXML
	public JDOMHelper getProcessorsForQueueAsXML( String inQName )
	{

		JDOMHelper outElem = null;
		try
		{
			outElem = new JDOMHelper( "<connections/>", null );
		}
		catch (JDOMHelperException e)
		{
			System.err.println( "ERROR: DPump:getProcessorsForQueueAsXML:"
				+ " Unable to create JDOM Helper."
				+ " Exception: " + e
				);
			return null;
		}

		String [] lRoles = new String[]
		{
			"reads_from", "writes_to", "uses"
		};

		// For each role
		for( int i=0; i<lRoles.length; i++ )
		{
			String role = lRoles[i];
			// Get a list of ajoining objects
			List others = getProcessorsForQueue( inQName, role );
			if( others.size() <= 0 )
				continue;
			// Create the new branch
			Element newBranch = new Element( role );
			// For each related object
			for( Iterator it = others.iterator(); it.hasNext(); )
			{
				// Get the object's name
				String otherName = (String)it.next();
				// create a leaf for it and then add the name to the branch
				Element newLeaf = new Element( "object" );
				newLeaf.addContent( otherName );
				newBranch.addContent( newLeaf );
			}
			// Add the branch to the tree
			outElem.addContent( newBranch );
		}

		return outElem;
	}

	// mbennett
	// Called from instantiateProcessors
	// Records, by name, the relationships between processes and queues
	private void recordProcessorQueueMappings( String inProcName,
		Queue[] inReadQs, Queue[] inWriteQs, Queue[] inUsesQs
		)
	{
		int i;
		if( inReadQs != null )
			for( i=0; i<inReadQs.length; i++ )
				recordSinglePQMap( inProcName, "reads_from", inReadQs[i] );
		if( inWriteQs != null )
			for( i=0; i<inWriteQs.length; i++ )
				recordSinglePQMap( inProcName, "writes_to", inWriteQs[i] );
		if( inUsesQs != null )
			for( i=0; i<inUsesQs.length; i++ )
				recordSinglePQMap( inProcName, "uses", inUsesQs[i] );
	}
	private void recordSinglePQMap( String inProcName,
		String inRoleName, Queue inQ
		)
	{

		final boolean debug = false;

		if( inProcName == null || inQ == null || inRoleName == null )
		{
			System.err.println( "ERROR: DPump:recordSinglePQMap"
				+ " null argument passed in."
				+ " inProcName=" + inProcName
				+ " inQ=" + inQ
				+ " inRoleName=" + inRoleName
				);
			return;
		}
		inProcName = inProcName.trim();
		if( inProcName.equals("") )
		{
			System.err.println( "ERROR: DPump:recordSinglePQMap"
				+ " empty proc name passed in."
				);
			return;
		}
		inRoleName = inRoleName.trim();
		if( inRoleName.equals("") )
		{
			System.err.println( "ERROR: DPump:recordSinglePQMap"
				+ " empty role name passed in."
				);
			return;
		}

		String lQName = inQ.getName();

		if( lQName == null || lQName.trim().equals("") )
		{
			System.err.println( "ERROR: DPump:recordSinglePQMap"
				+ " null/empty queue name found."
				);
			return;
		}
		lQName = lQName.trim();


		// The queue sees things differently than a process
		// If a process reads from a queue, then that queue
		// is writing to that process
		// For now it seems minimal confusion would be to change
		// the role name around for the queues' view
		String lQRoleName;
		if( inRoleName.equals("reads_from") )
			lQRoleName = "writes_to";
		else if( inRoleName.equals("writes_to") )
			lQRoleName = "reads_from";
		else if( inRoleName.equals("uses") )
			lQRoleName = "uses";
		else
		{
			lQRoleName = "associated";
			System.err.println( "Warning: DPump:recordSinglePQMap"
				+ " Unknown role name '" + inRoleName + "'"
				+ ", will go ahead and add it anyway."
				);
		}

		if(debug) System.err.println( "Debug: DPump:recordSinglePQMap:"
			+ " inProcName=" + inProcName
			+ " inRoleName=" + inRoleName
			+ " lQName=" + lQName
			+ " lQRoleName=" + lQRoleName
			);



		// Record the process -> queue association
		if(debug) System.err.println( "Debug: DPump:recordSinglePQMap:"
			+ " Calling updateMap with fProcToQMap"
			+ " inProcName=" + inProcName
			+ " lQName=" + lQName
			);
		updateMap( fProcToQMap, inProcName, lQName );
		// Record the queue -> process association
		if(debug) System.err.println( "Debug: DPump:recordSinglePQMap:"
			+ " Calling updateMap with fQToProcMap"
			+ " lQName=" + lQName
			+ " inProcName=" + inProcName
			);
		updateMap( fQToProcMap, lQName, inProcName );


		String tmpStr;

		// Record the process+role -> queue association
		tmpStr = inProcName + PQ_KEY_DELIM + inRoleName;
		if(debug) System.err.println( "Debug: DPump:recordSinglePQMap:"
			+ " Calling updateMap with fProcToQMapByRole"
			+ " tmpStr=" + tmpStr
			+ " lQName=" + lQName
			);
		updateMap( fProcToQMapByRole,
			tmpStr,
			lQName
			);
		// Record the queue+role -> process association
		tmpStr = lQName + PQ_KEY_DELIM + lQRoleName;
		if(debug) System.err.println( "Debug: DPump:recordSinglePQMap:"
			+ " Calling updateMap with fQToProcMapByRole"
			+ " tmpStr=" + tmpStr
			+ " inProcName=" + inProcName
			);
		updateMap( fQToProcMapByRole,
			tmpStr,
			inProcName
			);
	}

	// Maintain a hash of hashes
	// Each subhash contains a running total of how many of each key it's seen
	private void updateMap( Hashtable inHash, String inKey, String inValue )
	{
		if( inHash == null || inKey == null || inValue == null )
		{
			System.err.println( "ERROR: DPump:updateMap:"
				+ " null argument passed in."
				+ " inHash='" + inHash + "'"
				+ " inKey='" + inKey + "'"
				+ " inValue='" + inValue + "'"
				);
			return;
		}

		final boolean debug = false;

		if(debug) System.err.println( "Debug: DPump:updateMap:"
				+ " inHash='" + inHash + "'"
				+ " inKey='" + inKey + "'"
				+ " inValue='" + inValue + "'"
				);

		// This is a hash table of hash tables

		// First find the main hashtable for this key
		// or create and store it

		Hashtable lValuesHash;
		if( inHash.containsKey( inKey ) )
		{
			if(debug) System.err.println( "\tfound values hash" );
			lValuesHash = (Hashtable)inHash.get( inKey );
		}
		else
		{
			if(debug) System.err.println( "\tcreating values hash" );
			lValuesHash = new Hashtable();
			inHash.put( inKey, lValuesHash );
		}
		if(debug) System.err.println( "Debug: DPump:updateMap:"
			+ " lValuesHash now = " + lValuesHash
			);

		// Find the old count, if any
		int prevCount = 0;
		if( lValuesHash.containsKey(inValue) )
		{
			Integer obj = (Integer)lValuesHash.get( inValue );
			prevCount = obj.intValue();
			if(debug) System.err.println( "Debug: DPump:updateMap:"
				+ " Found existing count " + prevCount
				+ " for tabulating key " + inValue
				);
		}
		else
			if(debug) System.err.println( "Debug: DPump:updateMap:"
				+ " No count found for tabulating key " + inValue
				);

		// Now create the updated count and store it
		Integer newObj = new Integer( prevCount + 1 );
		lValuesHash.put( inValue, newObj );

		if(debug) System.err.println( "Debug: DPump:updateMap:"
			+ " lValuesHash finally = " + lValuesHash
			);

		if(debug) System.err.println( "Debug: DPump:updateMap:"
				+ " inHash finally =" + inHash
				);

	}

	private List getMapValues( Hashtable inMap, String inKey )
	{
		if( inMap.containsKey( inKey ) )
		{
			//return new Vector( inMap.keySet() );
			Hashtable lValuesHash = (Hashtable)inMap.get( inKey );
			if( lValuesHash != null )
			{
				return new Vector( lValuesHash.keySet() );
			}
			else
			{
				System.err.println( "ERROR: DPump:getMapValues:"
					+ " System hash reported to have key but got no hashtable"
					+ " inKey=" + inKey
					);
				return new Vector();
			}
		}
		else
			return new Vector();
	}

	public Vector getProcessorList()
	{
		return fThreads;
	}

	////////////
	//
	// This is the starting routine for DPump
	//
	////////////

	public static void main( String[] inArgs )
	{
		try
		{
			DPump lDPump = new DPump( inArgs[0] );
			lDPump.run();
		}
		catch( DPumpException de )
		{
			System.err.println( de );
			System.err.println( de.getMessage() );
			de.printStackTrace();
		}
	}

	private static void ___Sep__Run_Logging__(){}

	//////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////////////
	//
	//  Logging
	//
	////////////////////////////////////////////////////////////

	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoStatusMsg( kClassName(), inFromRoutine );
	}

	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName(), inFromRoutine );
	}

	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( kClassName(), inFromRoutine );
	}

	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName(), inFromRoutine );
	}

	private static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName(), inFromRoutine );
	}

	private static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoWarningMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoWarningMsg( kClassName(), inFromRoutine );
	}

	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}

	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}


	//
	// Private instance variables.
	//

	private Hashtable fQueueList = null;
	private Class[] fConstructorSignature = null;
	private ThreadGroup fProcessorsThreadGroup = null;
	private Vector fThreads = null;

	// mbennett process/queue mapping
	private Hashtable fProcToQMap;
	private Hashtable fQToProcMap;
	private Hashtable fProcToQMapByRole;
	private Hashtable fQToProcMapByRole;
	private final String PQ_KEY_DELIM = "\t";

	//
	// Private string constants
	//

	private final String READ_QUEUE_PARAMETER = "reads_from";
	private final String WRITE_QUEUE_PARAMETER = "writes_to";
	private final String USES_QUEUE_PARAMETER = "reads_and_writes";
	private final String PARAMETERS_TAGS_NAME = "parameters";
	private final String CLASSNAME_ATTRIBUTE_NAME = "class";
	private final String INSTANCES_PARAMETER = "instances";
	private final String INSTANCE_ATTRIBUTE_NAME = "instance";
	private final String QUEUES_TAG_NAME = "queues";
	private final String QUEUE_TAGS_NAME = "queue";
	private final String PROCESSORS_TAG_NAME = "processors";
	private final String PROCESSOR_TAGS_NAME = "processor";
	private final String NAME_ATTRIBUTE_NAME = "name";
	private final String READ_FROM_QUEUES_TAG_NAME = "reads_from";
	private final String WRITES_TO_QUEUES_TAG_NAME = "writes_to";
	private final String USES_QUEUES_TAG_NAME = "uses";
	private final String QUEUE_CONNECTION_NAME = "queue_connection";
	//private final String CORE_PACKAGE_PATH_NAME = "nie.core.";
	private final String CORE_PACKAGE_PATH_NAME = "nie.pump.base.";
	//private final String PROCESSORS_PACKAGE_PATH_NAME = "nie.processors.";
	private final String PROCESSORS_PACKAGE_PATH_NAME = "nie.pump.processors.";
}
