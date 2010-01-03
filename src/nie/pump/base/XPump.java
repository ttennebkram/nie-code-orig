//package nie.core;
package nie.pump.base;

/**
 * Title:        XPump
 * Description:
 * Copyright:    Copyright (c) 2001, 2002
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop and Mark Bennett
 * @version 1.0
 */

import org.jdom.*;
import org.jdom.input.*;
import java.io.*;
import java.net.*;
import java.util.*;

// import nie.core.JDOMHelper;
// import nie.core.JDOMHelperException;
// import nie.core.AuxIOInfo;
import nie.core.*;



public class XPump
{

	private final boolean debug = false;

	private static String kClassName() { return "XPump"; }


	private void __sep__XPumpException_Subclass__() {}
	///////////////////////////////////////////////////////////////////////

	/////////////////////////////////
	//
	// This is the exception that is
	// thrown if there's a problem with
	// something
	//
	/////////////////////////////////

	public class XPumpException extends Exception
	{
		public XPumpException( String inMessage )
		{
			super( inMessage );
		}
	}

	private void __sep__Constructor__() {}
	///////////////////////////////////////////////////////////////////////

	public XPump( String uri ) throws XPumpException
	{
		final String kFName = "(constructor)";
		final String kExTag = kClassName() + "." + kFName + ": ";

		if( uri == null || uri.trim().equals("") )
		{
			throw new XPumpException( kExTag
				+ "XPump must be passed a string that represents a file name or URL."
				);
		}

		/***
		try
		{
			fJdh = new JDOMHelper( uri );
		}
		catch (Exception e)
		{
			throw new XPumpException(
				"XPump constructor got an exception from JDOMHelper constructor" +
				e
				);
		}
		***/

		// Use the version that supports <include's>
		fJdh = null;
		try
		{

			// We'd like to know the final URI
			AuxIOInfo auxInfo = new AuxIOInfo();

			// Load and contstruct
			fJdh = new JDOMHelper(
				uri, null, 0, auxInfo
				);

			// Store the final, absolute URI we wound up with
			fFullConfigFileURI = auxInfo.getFinalURI();

		}
		catch (JDOMHelperException e)
		{
			throw new XPumpException( kExTag
				+ "XPump constructor got an exception from JDOMHelper constructor, error: "
				+ e
				);
		}
		if( fJdh == null )
			throw new XPumpException( kExTag
				+ "Got back a NULL xml tree"
				+ " from file \"" + uri + "\""
				);

		// debugMsg( kFName, "Have now read config file \"" + fConfigFileURI + "\"." );
		debugMsg( kFName,
			"Have now read config file \"" + getConfigFileURI() + "\"."
			);


		String mainElementName = fJdh.getElementName();

		// If it's not xpump, pass it through "raw" to dpump
		if( ! mainElementName.equals( XPUMP_MAIN_TAG_NAME ) )
		{
			fDPumpTree = fJdh.getJdomElement();
		}
		// Else it is an xpump
		// We need to COMPILE IT
		else
		{
			setWorkingDir( uri );
			fDPumpTree = compileXPump();
			String dumpLoc = fJdh.getStringFromAttribute(
				WRITE_COMPILED_DPUMP_LOCATION );
			if( dumpLoc != null && ! dumpLoc.trim().equals("") )
			{
				dumpLoc = dumpLoc.trim();
				statusMsg( kFName, "writing compiled dpump to '" +
					dumpLoc + "'"
					);
				JDOMHelper.writeToFile( fDPumpTree, dumpLoc );
			}
		}

		if( fDPumpTree == null )
			throw new XPumpException( kExTag
				+ "internal dpump tree is null for XML tree \""
				+ mainElementName + "\""
				);

		// Create the dpump
		try
		{
			fMainDPumpInstance = new DPump( fDPumpTree );
		}
		catch (Exception de)
		{
			throw new XPumpException(
				"XPump got an exception when calling DPump constructor: " +
				de
				);
		}

		// Run is left for the run method
	}

	// If the constructor was passed a file, set us to it relatively
	void setWorkingDir( String uri )
	{
		final String kFName = "setWorkingDir";

		final boolean debug = true;

		File tmpFile = null;
		try
		{
			tmpFile = new File( uri );
		}
		catch (Exception e)
		{
			warningMsg( kFName, "uri '" +
				uri + "' doesn't seem to be a file, not changing cwd."
				);
			return;
		}

		if( tmpFile.exists() && tmpFile.isFile() )
		{

			debugMsg( kFName, "setWorkingDir: will set directory for '" +
					uri + "'"
					);
			try
			{
				// statusMsg( kFName, "tmpFile=" + tmpFile );
				// statusMsg( kFName, "tmpFile.getParentFile()=" + tmpFile.getParentFile() );
				if( null != tmpFile.getParentFile() )
					System.setProperty( "user.dir", tmpFile.getParentFile().getCanonicalPath() );
				else
					debugMsg( kFName, "no parent directory" );
			}
			catch (Exception e)
			{
				errorMsg( kFName, "Error setting path: " + e );
			}
			debugMsg( kFName, "cwd now = '" +
					System.getProperty( "user.dir" ) + "'"
					);
		}
		else    // Didn't seem to be a file
			debugMsg( kFName,
				"uri '" +
					uri + "' doesn't seem to be a file." +
					" Leaving cwd as is '" +
					System.getProperty( "user.dir" ) + "'"
					);
	}

	private void __sep__High_Level_Logic__() {}
	///////////////////////////////////////////////////////////////////////

	public void run()
	{
		fMainDPumpInstance.run();
	}

	// This is a bit complicated, we're dealing with 3 trees here:
	// # 1: This JDOMHelper instance, which is XPump source code,
	//  it's what we're compiling.
	//  It's the member variable fJdh.
	// # 2: The new JDOM tree (Element vs JDOMHelper) that we're building
	//  as a result of the compilation.
	//  It's a JDOMHelper here as lDPumpTree but the top element is
	//  as a plain Element at the end.
	// Tree # 3: The xpump to dpump mapping tree
	//  This is stored as a system file and brought in here
	//  as a JDOMHelper x2dTree.
	//  Todo: At some point maybe make this static or move it elsewhere.
	Element compileXPump()
		throws XPumpException
	{
		final String kFName = "compileXPump";

		final boolean debug = false;

		// Get the main symantic xpump to dpump mapping file
		JDOMHelper x2dTree = getXPump2DPumpDef();
		// Init the hashes
		initHashes( x2dTree );

		JDOMHelper lDPumpTree = null;
		// Get a fresh, base dpump tree started
		try
		{
			lDPumpTree = new JDOMHelper( DPUMP_SKELETON, null );
		}
		catch (Exception e)
		{
			throw new XPumpException(
				"Error parsing skeleton dpump tree, DPUMP_SKELETON='" +
				DPUMP_SKELETON +
				"', error: " + e
				);
		}

		// Get the main children of the xpump tag
		List xpChildren = fJdh.getJdomChildren();
		if( xpChildren.size() < 1 )
			warningMsg( kFName, "this xpump proram/tree has no children" );
		else
			infoMsg( kFName, "This xpump proram/tree has " +
				xpChildren.size() + " child(ren)"
				);


		xpChildren = collapseGroupTags( xpChildren );

		// For each of the Xpump's first level children
		// Note that we do NOT support recursion, nesting at this time
		int childCount = 0;
		String lastQueue = null;
		boolean lastRequiredAPrimaryOutputQueue = false;
		Iterator it = xpChildren.iterator();
		while( it.hasNext() )
		{
			// Get the child and it's name
			Element child = (Element)it.next();
			childCount++;
			String childName = child.getName();
			childName = childName.toLowerCase().trim();
			if( childName.equals("") )
			{
				throw new XPumpException(
					"xpump child # " + childCount + " has a null name"
					);
			}

			// We want a name can help us talk to the user
			// so we'll let them know which occurrance of the tag this is
			// String friendlyChildName = calculateFriendlyChildName( childName );
			String friendlyChildName = JDOMHelper.getStringFromAttributeTrimOrNull( child, "name" );
			if( null==friendlyChildName )
				friendlyChildName = calculateFriendlyChildName( childName );

			// Handle sepcial tags that don't map to xpump processes
			// ================================================================

			// Is this a global queue setting?
			if( childName.equals("set_default_queue_class") )
			{
				setDefaultQuueClass( child, friendlyChildName );
				continue;
			}

			// Is this a queue declaration?
			if( childName.equals("queue") )
			{
				processXPumpQueueDeclaration( child, friendlyChildName );

				continue;
			}

			// Now find the dpump entry for it
			JDOMHelper dpumpDef = null;
			try
			{
				dpumpDef = (JDOMHelper)fXNameToDDefHash.get( childName );
			}
			catch (Exception e)
			{
				throw new XPumpException(
					"Could not find xpump in dpump def map" +
					", xpump element = '" + friendlyChildName +
					"', xpump child # " + childCount
					);
			}
			if( null == dpumpDef )
			{
				throw new XPumpException(
					"Could not find dpump def in hash map" +
					", xpump element nane = '" + childName +
					"/" +  friendlyChildName +
					"', xpump child # " + childCount
					);
			}

			// Now get the dpump class name
			String dpumpClassName = dpumpDef.getStringFromAttribute( "class" );
			if( dpumpClassName == null || dpumpClassName.trim().equals("") )
			{
				throw new XPumpException(
					"null/no dpump class name in dpump def map" +
					", xpump element = '" + friendlyChildName +
					"', dpump def='" + dpumpDef.JDOMToString() + "'"
					);
			}
			dpumpClassName = dpumpClassName.trim();

			debugMsg( kFName, "for xpump tag '" +
					friendlyChildName + "' found xpump class '" +
					dpumpClassName + "'"
					);

			// So, by now we have an xpump node child and name childName and
			// the corresponding dpump definition dpumpDef,
			// and we have the dpump class name, dpumpClassName

			// now we're ready to generate a dpump declaration

			// First, make sure it is a one to one replacement
			if( dpumpDef.getBooleanFromSinglePathAttr(
					"xpump_info", "direct_xpump_replacement", false
					)
				)
			{
				// Create a process entry
				JDOMHelper procEntry = null;
				try
				{
					// Call the constructor that takes a literal string
					procEntry = new JDOMHelper(
						DPUMP_PROCESS_ENTRY_SKELETON, null );
				}
				catch (Exception e)
				{
					throw new XPumpException(
						"JDOMHelper constructor threw an en exception" +
						" for the dpump process entry literal skeleton '" +
						DPUMP_PROCESS_ENTRY_SKELETON +
						"', the exception was: " + e
						);
				}
				// Add the dpump class name
				procEntry.getJdomElement().setAttribute(
					"class", dpumpClassName
					);

				// And give it a friendly name
				procEntry.getJdomElement().setAttribute(
					"name", friendlyChildName
					);


				// Instances checking
				// ====================================

				// Check for instances
				int numInstances = JDOMHelper.getIntFromAttribute( child,
					"instances", 1
					);

				// Can it accept multiple instances
				boolean allowsMultipleSubInstances =
					dpumpDef.getBooleanFromAttribute(
						"multiple_sub_instances_ok", false
						);

				if( numInstances != 1 )
				{
					if( ! allowsMultipleSubInstances )
						throw new XPumpException( friendlyChildName +
							" specified multiple instances=" + numInstances +
							" but this process class doesn't allow that"
							);
					if( numInstances <= 0 )
						throw new XPumpException( friendlyChildName +
							" specified multiple instances=" + numInstances +
							" but number must be a postive integer"
							);

					// Add the dpump class name
					procEntry.getJdomElement().setAttribute(
						"instances", "" + numInstances
						);
				}

				//////////////////////////////////////////////////////
				//
				//  Preliminary checking for what queues we will
				//  and won't accept, and how many were specified
				//
				//////////////////////////////////////////////////////

				// Does it need/accept input and output queues
				// Booleans for input
				boolean hasPrimaryInputQueue =
					dpumpDef.getBooleanFromSinglePathAttr(
						"reads_from/queue_connection", "primary_input", false
						);
				boolean hasPrimaryOutputQueue =
					dpumpDef.getBooleanFromSinglePathAttr(
						"writes_to/queue_connection", "primary_output", false
						);
				boolean hasPrimaryErrorQueue =
					dpumpDef.getBooleanFromSinglePathAttr(
						"writes_to/queue_connection[-1]", "primary_exception", false
						);
				boolean hasPrimaryUsesQueue =
					dpumpDef.getBooleanFromSinglePathAttr(
						"uses/queue_connection", "primary_uses", false
						);

				// Ints for outputs (intermediate step)

				// Required
				int numRequiredPrimaryInputQueues =
					hasPrimaryInputQueue ?
					dpumpDef.getIntFromSinglePathAttr(
						"reads_from/queue_connection", "how_many_required", 0
						)
					: 0 ;
				int numRequiredPrimaryOutputQueues =
					hasPrimaryOutputQueue ?
					dpumpDef.getIntFromSinglePathAttr(
						"writes_to/queue_connection", "how_many_required", 0
						)
					: 0 ;
				// Get the error, but only if we know we allow errors
				// Assume it's the last queue listing
				int numRequiredPrimaryErrorQueues =
					hasPrimaryErrorQueue ?
					dpumpDef.getIntFromSinglePathAttr(
						"writes_to/queue_connection[-1]", "how_many_required", 0
						)
					: 0 ;
				int numRequiredPrimaryUsesQueues =
					hasPrimaryUsesQueue ?
					dpumpDef.getIntFromSinglePathAttr(
						"uses/queue_connection", "how_many_required", 0
						)
					: 0 ;

				// How many allowed
				int numAllowedPrimaryInputQueues =
					hasPrimaryInputQueue ?
					dpumpDef.getIntFromSinglePathAttr(
						"reads_from/queue_connection", "how_many_allowed", 0
						)
					: 0 ;
				int numAllowedPrimaryOutputQueues =
					hasPrimaryOutputQueue ?
					dpumpDef.getIntFromSinglePathAttr(
						"writes_to/queue_connection", "how_many_allowed", 0
						)
					: 0 ;
				// We only really allow 1
				int numAllowedPrimaryErrorQueues =
					hasPrimaryErrorQueue ?
					dpumpDef.getIntFromSinglePathAttr(
						"writes_to/queue_connection[-1]", "how_many_allowed", 0
						)
					: 0 ;
				int numAllowedPrimaryUsesQueues =
					hasPrimaryUsesQueue ?
					dpumpDef.getIntFromSinglePathAttr(
						"uses/queue_connection", "how_many_allowed", 0
						)
					: 0 ;

				// Booleans for outputs
				// Todo: horrible variable naming, should clean up later
				boolean requiresPrimaryInputQueue =
					numRequiredPrimaryInputQueues >= 1 ? true : false;
				boolean requiresPrimaryOutputQueue =
					numRequiredPrimaryOutputQueues >= 1 ? true : false;
				boolean requiresPrimaryErrorQueue =
					numRequiredPrimaryErrorQueues >= 1 ? true : false;
				boolean requiresPrimaryUsesQueue =
					numRequiredPrimaryUsesQueues >= 1 ? true : false;

				// Now start looking at the queue lists
				// This will get each type of queue's list as a simple
				// list of strings, with no nulls and already trimmed.
				// And it does a LOT of error checking
				List inQs = getAndCheckQs( child, "from", friendlyChildName,
					numRequiredPrimaryInputQueues,
					numAllowedPrimaryInputQueues
					);
				List outQs = getAndCheckQs( child, "to", friendlyChildName,
					numRequiredPrimaryOutputQueues,
					numAllowedPrimaryOutputQueues
					);
				List errorQs = getAndCheckQs( child, "error", friendlyChildName,
					numRequiredPrimaryErrorQueues,
					numAllowedPrimaryErrorQueues
					);
				List usesQs = getAndCheckQs( child, "uses", friendlyChildName,
					numRequiredPrimaryUsesQueues,
					numAllowedPrimaryUsesQueues
					);

				// How many of each were set?
				int numInputQs = inQs.size();
				int numOutputQs = outQs.size();
				int numErrorQs = errorQs.size();
				int numUsesQs = usesQs.size();

				// Determine whether or not a queue set (attr or list)
				// has specified "-" as a queue
				boolean doesInputHaveDash = checkForDash(
					inQs, friendlyChildName
					);
				boolean doesOutputHaveDash = checkForDash(
					outQs, friendlyChildName
					);
				boolean doesErrorHaveDash = checkForDash(
					errorQs, friendlyChildName
					);
				if( doesErrorHaveDash )
					throw new XPumpException( friendlyChildName
						+ " has \"-\" specified as an error queue"
						+ " (via error_q= or <error q=...>)"
						+ ", this is NOT valid for error or uses queues"
						);
				boolean doesUsesHaveDash = checkForDash(
					usesQs, friendlyChildName
					);
				if( doesUsesHaveDash )
					throw new XPumpException( friendlyChildName
						+ " has \"-\" specified as a uses queue"
						+ " (via uses_q= or <uses q=...>)"
						+ ", this is NOT valid for error or uses queues"
						);


				//////////////////////////////////////////////
				//
				//  Done initial checking
				//  Now some process logic
				//
				//////////////////////////////////////////////

				// Now some logic on the queues
				if( hasPrimaryInputQueue )
				{

					// Was a specific queue mentioned?
					if( numInputQs > 0 )
					{

						// Do we have a queue that we're already working with?
						if( lastQueue != null )
						{

							// This may or may not be OK
							if( lastRequiredAPrimaryOutputQueue &&
								! doesInputHaveDash
								)
							{
								throw new XPumpException(
									friendlyChildName +
									" specified particular queue(s) to read from" +
									" but there was also a pending queue left over" +
									" from the previous tag."
									);
							}
						}

						// Else read from the list
						// For each write to queue
						Iterator inQsItr = inQs.iterator();
						while( inQsItr.hasNext() )
						{
							String tmpQName = (String)inQsItr.next();
							// Was normalized above

							// Is it referring to our sort of "stdin" ?
							if( tmpQName.equals("-") )
							{
								if( lastQueue != null )
								{
									addQueueReferenceToProcess(
										procEntry, "reads_from",
										lastQueue, numInstances
										);
								}
								// Else there was no previous queue
								else
								{
									throw new XPumpException( "Error:compile:"
										+ friendlyChildName
										+ " referenced input queue \"-\""
										+ " but none was avialable from"
										+ "previous tag"
										);
								}
							}
							else // Else regular named queue
							{
								// Register a specific user queue
								String newQ = registerSpecificQueueName( tmpQName );
								// Add the queue reference
								addQueueReferenceToProcess(
									procEntry, "reads_from",
									newQ, numInstances
									);
							}
						}   // End for each write to queue
					}
					else    // Else no specific queue was mentioned
					{
						// Do we have a queue that we're working with?
						if( lastQueue != null )
						{
							//updateQueueName( lastQueue, friendlyChildName );
							addQueueReferenceToProcess(
								procEntry, "reads_from", lastQueue, numInstances
								);
						}
						// Else there was no previous queue
						else
						{
							// If we required one then that's a big problem
							if( requiresPrimaryInputQueue )
								throw new XPumpException(
									friendlyChildName +
									" requires an input queue but none was avialable"
									);
						}
					}   // End else no specific queue was mentioned
				}   // End if it has a primary input queue
				// Else does not have a primary input queue
				else
				{
					// Do we have a queue that we're working with?
					if( lastRequiredAPrimaryOutputQueue && lastQueue != null )
					{
						// If we won't accept it then that's a big problem
						throw new XPumpException(
							friendlyChildName +
							" does not accept a primary input queue" +
							" but one was pending from the previous tag."
							);
					}
				}

				// Currently we always clear the last queue,
				// you're not allowed to insert a non-read-write
				// between a reader and writer
				lastQueue = null;


				// Handle outputs
				if( hasPrimaryOutputQueue )
				{
					// Was a specific queue mentioned?
					if( numOutputQs > 0 )
					{
						// For each write to queue
						Iterator outQsItr = outQs.iterator();
						while( outQsItr.hasNext() )
						{
							String tmpQName = (String)outQsItr.next();
							// Was normalized above

							// Is it referring to our sort of "stdin" ?
							if( tmpQName.equals("-") )
							{
								if( it.hasNext() )
								{
									// Declare an anonymous queue
									lastQueue = getNextAutomaticQueueName();
									// Add the reference
									addQueueReferenceToProcess(
										procEntry, "writes_to",
										lastQueue, numInstances
										);
								}
								// Else we're last
								else
								{
									// Double check that it didn't require one
									if( requiresPrimaryOutputQueue )
										throw new XPumpException(
											friendlyChildName +
											" (1) requires an output queue but" +
											" it didn't have any place to write." +
											" No <to> child tags nor to_q= attribute were found." +
											" And it's the last tag in the chain."
											);
								}
							}
							else // Else regular named queue
							{
								// Register a specific user queue
								String newQ = registerSpecificQueueName(
									tmpQName
									);
								// Add the queue reference
								addQueueReferenceToProcess(
									procEntry, "writes_to",
									newQ, numInstances
									);
							}
						}   // End for each write to queue
					}
					else    // Else no specific queue was mentioned
					{

						// If we're not the last tag, then go ahead and
						// create a default output that perhaps the next
						// tag will want
						// We should not create dangling output queues
						// that are not needed
						if( it.hasNext() )
						{
							// Declare an anonymous queue
							lastQueue = getNextAutomaticQueueName();
							// Add the reference
							addQueueReferenceToProcess(
								procEntry, "writes_to", lastQueue, numInstances
								);
						}
						// Else we're last
						else
						{
							// Double check that it didn't require one
							if( requiresPrimaryOutputQueue )
								throw new XPumpException(
									friendlyChildName +
									" requires an output queue but" +
									" it didn't have any place to write." +
									" No <to> child tags nor to_q= attribute were found." +
									" And it's the last tag in the chain."
									);
						}
					}

					// Remember the state for processing the next tag
					lastRequiredAPrimaryOutputQueue = requiresPrimaryOutputQueue;

				}   // End if it has a primary output queue

				// Handle Error queue declaraions
				if( hasPrimaryErrorQueue )
				{
					// Was a specific queue mentioned?
					if( numErrorQs > 0 )
					{
						// For each write to queue
						Iterator errorQsItr = errorQs.iterator();
						while( errorQsItr.hasNext() )
						{
							String tmpQName = (String)errorQsItr.next();
							// Was normalized above

							// Register a specific user queue
							String newQ = registerSpecificQueueName( tmpQName );
							// Add the queue reference
							addQueueReferenceToProcess(
								procEntry, "writes_to", newQ, numInstances
								);
						}   // End for each error queue
					}
					else    // Else no specific queue was mentioned
					{
						// Does it require an error queue?
						// This should have been caught earlier
						if( requiresPrimaryErrorQueue )
							throw new XPumpException(
								friendlyChildName +
								" requires an output error queue but none was specified"
								);
					}
				}   // End if it has a primary error queue

				// Handle Uses queue declaraions
				if( hasPrimaryUsesQueue )
				{
					// Was a specific queue mentioned?
					if( numUsesQs > 0 )
					{
						// For each write to queue
						Iterator usesQsItr = usesQs.iterator();
						while( usesQsItr.hasNext() )
						{
							String tmpQName = (String)usesQsItr.next();
							// Was normalized above

							// Register a specific user queue
							// And we specifically tell the system that
							// this is a "uses" queue, so it can take
							// any special actions needed.
							String newQ = registerSpecificQueueName(
								tmpQName, "uses"
								);
							// Add the queue reference
							addQueueReferenceToProcess(
								procEntry, "uses", newQ, numInstances
								);
						}   // End for each uses queue
					}
					else    // Else no specific queue was mentioned
					{
						// Does it require an error queue?
						// This should have been caught earlier
						if( requiresPrimaryErrorQueue )
							throw new XPumpException(
								friendlyChildName +
								" requires an output error queue but none was specified"
								);
					}
				}   // End if it has a primary error queue

				// Copy over parameters from xpump to dpump
				copyParameters( child, procEntry, dpumpDef );

				/// **********************************************************
				// Todo: add other stuff
				// add exit processor
				// Todo: see if it's not there but maybe special handling
				// Todo: generate queues
				// Todo: chaining!

				if( debug )
				{
					// TODO: integrate into new logging system
					System.out.println( "Will add dpump proc node:" );
					procEntry.print();
					System.out.println();
				}

				// Later these other steps will be easier if we
				// keep the process entry as a JDOMHelper object

				// Now add this new process entry to the DPump
				// tree that we're building
				lDPumpTree.addElementToPath( "processors",
					procEntry.getJdomElement()
					);

			}
			else
				throw new XPumpException(
					"Not Implemented: direct_xpump_replacement must be true" +
					", dpump def='" + dpumpDef + "'"
					);




			// Do the error stuff




		}   // End for each of the Xpump's first level children

		// Todo: final check for pending queue

		// Add the queue definitions to the tree
		addQueueDefinitions( lDPumpTree );

		if( debug )
		{
			// TODO: integrate into new logging system
			System.out.println( "Resulting DPump tree:" );
			lDPumpTree.print();
			System.out.println();
		}
		return lDPumpTree.getJdomElement();
	}

	List collapseGroupTags( List inChildren ) {
		List outChildren = new Vector();
		outChildren.addAll( inChildren );
		while( true ) {

			boolean hasGroups = false;
			for( Iterator ckit = outChildren.iterator() ; ckit.hasNext() ; ) {
				Object node = ckit.next();
				if( ! (node instanceof Element) )
					continue;
				Element elem = (Element) node;
				String name = elem.getName();
				if( name.equals(GROUP_TAG) ) {
					hasGroups = true;
					break;
				}
			}
			if( ! hasGroups )
				break;

			List newChildren = new Vector();
			for( Iterator cpit = outChildren.iterator() ; cpit.hasNext() ; ) {
				Object node = cpit.next();
				// If it's not even an element, just copy it over
				if( ! (node instanceof Element) ) {
					newChildren.add( node );
					continue;
				}
				Element elem = (Element) node;
				String name = elem.getName();

				// If not a group tag, just copy it over
				if( ! name.equals(GROUP_TAG) ) {
					newChildren.add( elem );
					continue;
				}

				for( Iterator childIt = elem.getChildren().iterator() ; childIt.hasNext() ; ) {
					Object newNode = childIt.next();
					newChildren.add( newNode );
				}

			}
			outChildren = newChildren;			

		}
		return outChildren;
	}

	// Normalize children and attribute queues to a single
	// list of normalized strings
	// and do LOTS of error checking
	private List getAndCheckQs( Element sourceTag, String qType,
		String nodeName, int numRequired, int numAllowed
		)
		throws XPumpException
	{
		final String kFName = "getAndCheckQs";

		final boolean debug = false;

		debugMsg( kFName, "Start."
			+ " qType=" + qType
			+ ", nodeName=" + nodeName
			+ ", numAllowed=" + numAllowed
			+ ", numRequired=" + numRequired
			);
		debugMsg( kFName, "sourceElem is: "
			+ JDOMHelper.JDOMToString( sourceTag )
			);

		// Whether to remove the stuff we find from the node
		// as we go along
		//final boolean doCleanup = false;
		// Todo: implement

		// Exception checking
		if( sourceTag == null || qType == null || qType.trim().equals("")
			|| nodeName == null
			)
		{
			new XPumpException( "Error: xpump:getAndCheckQs: null input(s)" );
		}

		// Normalize q class
		qType = qType.trim().toLowerCase();

		// Setup the return
		List outList = new Vector();


		// Check for specific read from and write to attrs
		// We will allow for xxx_queue="..." or xxx_q="..."
		// The names of the two attibutes
		String longAttrName = qType + "_queue";
		String shortAttrName = qType + "_q";
		// The two potential values
		String attrValue1 = sourceTag.getAttributeValue( longAttrName );
		String attrValue2 = sourceTag.getAttributeValue( shortAttrName );
		// They should not set both
		if( attrValue1 != null && attrValue2 != null )
			new XPumpException( "Error: Node " + nodeName
				+ " has specified conflicting attributes"
				+ ", has set both " + longAttrName
				+ " and " + shortAttrName
				+ ", must not use both"
				);
		// Grab the one that's been set
		String attrValue = attrValue1 != null ? attrValue1 : attrValue2;
		// It should not be an empty string
		if( attrValue != null && attrValue.trim().equals("") )
			new XPumpException(
				"Node " + nodeName + " has specified a null queue attribute"
				+ " for queue class " + qType
				);
		// Normalize it, if it exists
		if( attrValue != null )
			attrValue = attrValue.trim();
		debugMsg( kFName, "attrValue='" + attrValue + "'" );

		// Now we want to get the list of children
		List qChildren = sourceTag.getChildren( qType );
		debugMsg( kFName, "# children =" + qChildren.size() );

		// Don't specify both forms
		if( attrValue != null && qChildren.size() > 0 )
			throw new XPumpException( "Error: Node " + nodeName
				+ " has both a queue attribute and queue children"
				+ " for queue type " + qType
				+ ", must not use both"
				);

		// Now start copying over the queue names as Strings
		// BTW, for now it's OK to have neither, we'll check
		// that later
		// If there's an attr, then that's it
		if( attrValue != null )
		{
			outList.add( attrValue );
			debugMsg( kFName, "just using attr" );
		}
		// if there's any children, scan them
		else if( qChildren.size() > 0 )
		{
			debugMsg( kFName, "will use children" );
			int childCounter = 0;
			// For each child in the list
			for( Iterator it = qChildren.iterator() ; it.hasNext() ; )
			{
				// Next element
				Element entry = (Element)it.next();
				childCounter++;

				// Look for the two variations in name
				String tmpQName1 = entry.getAttributeValue( "queue" );
				String tmpQName2 = entry.getAttributeValue( "q" );

				// Bad if both set
				if( tmpQName1 != null && tmpQName2 != null )
					throw new XPumpException( "Error: Node " + nodeName
						+ " has a queue child with both queue and q attributes"
						+ ", for child # " + childCounter
						+ ", queue type " + qType
						+ ", must use one or the other"
						);

				// Bad if neither set
				if( tmpQName1 == null && tmpQName2 == null )
					throw new XPumpException( "Error: Node " + nodeName
						+ " has a queue child without a queue or q attribute"
						+ ", for child # " + childCounter
						+ ", queue type " + qType
						+ ", must have one or the other"
						);
				// Keep one and normalize
				String tmpQName = tmpQName1 != null ? tmpQName1 : tmpQName2;
				if( tmpQName != null )	// Shouldn't need to check
					tmpQName = tmpQName.trim();

				// Bad if null or empty string
				if( tmpQName == null || tmpQName.equals("") )
					throw new XPumpException( "Error: Node " + nodeName
						+ " has a queue child without an empty string as a queue attribute"
						+ ", attribute queue= or q="
						+ ", for child # " + childCounter
						+ ", queue type " + qType
						+ ", must have one or the other and not be empty"
						);

				debugMsg( kFName,
					"adding child # " + childCounter
					+ ", qname=" + tmpQName
					);

				// Now add it to the list!
				outList.add( tmpQName );

			}	// End for each child
		}	// End else look at the list

		// We'll be using the size of the queue a lot
		int numFound = outList.size();
		debugMsg( kFName, "final list length=" + numFound );

		// Now some sanity checks on what we found
		if( numAllowed >= 0 &&
			numFound > numAllowed
			)
		{
			throw new XPumpException( "Error: Node " + nodeName
				+ " has too many queues specified"
				+ " (via attributes or children elements)"
				+ ", queue type " + qType
				+ ", max=" + numAllowed
				+ ", actual=" + numFound
				);
		}

		// Now some sanity checks on what we found
		// We need to make allowances for from and to, because they
		// may be doing the normal "standard" flow, so might not specify
		// a queue
		boolean stdAllowance = ( qType.equals("to") || qType.equals("from") );
		if( numRequired > 0 &&
			(
				( ! stdAllowance && numFound < numRequired ) ||
				( stdAllowance && numRequired > 1 && numFound < numRequired )
			)
			)
		{
			throw new XPumpException( "Error: Node " + nodeName
				+ " does not have enough queues specified"
				+ " (via attributes or children elements)"
				+ ", queue type " + qType
				+ ", min required=" + numRequired
				+ ", stdAllowance=" + stdAllowance
				+ ", actual=" + numFound
				);
		}

		// We're done!
		return outList;
	}

	// Check for a "-" entry
	// Error if more than one
	private boolean checkForDash( List qList, String nodeName )
		throws XPumpException
	{
		if( qList == null || nodeName == null )
			new XPumpException( "XPump:checkForDash: both inputs are null" );

		// Short circuit if none
		if( qList.size() < 1 )
			return false;

		// Must check through the list
		boolean haveSeenDash = false;
		for( Iterator it = qList.iterator() ; it.hasNext() ; )
		{
			String tmpQName = (String)it.next();
			// Should already be normalized

			// Is it a dash?
			if( tmpQName.equals("-") )
			{
				// If we've already seen one this is bad
				if( haveSeenDash )
					throw new XPumpException( "xpump:checkForDash: " + nodeName
						+ " has more than one queue \"-\" specified"
						+ " (via <child q=...>)"
						);
				haveSeenDash = true;

				// Note that we don't break on true, we keep checking
				// all of them
			}
		}

		return haveSeenDash;
	}


	// This is one of the trickier operations
	// XPump tags have elements and attributes, and so
	// do DPump tags (in their <parameters> tag.
	// This method copies things over.
	// Which method to use is defined in the dpump def
	// Supported modes:
	//  - none
	//      don't do any copying
	//  - children_to_children
	//      a node for node copy of their children
	//  - tag_as_child
	//      a copy of the tag is set as a child of the parameters seciton
	//      Todo: allow for renaming
	//  - attrs_as_children
	//      convert each attribute to a child
	//      This ALSO copies over any other children, like children_to_children
	//      Todo: allow for renaming
	//  - tag_as_params
	//      The tag BECOMES the parameters tag and the element
	//      is renamed.
	//      This is not often used.
	// Todo: add other methods.
	void copyParameters( Element fromXPumpTag,
		JDOMHelper toDPumpTag, JDOMHelper withDPumpDefTag
		)
		throws XPumpException
	{
		final String kFName = "copyParameters";

		if( fromXPumpTag == null || toDPumpTag == null  ||
			withDPumpDefTag == null
			)
		{
			throw new XPumpException(
				"One of the 3 required inputs is NULL, is null=" +
				(fromXPumpTag == null) + "/" +
				(toDPumpTag == null) + "/" +
				(withDPumpDefTag == null)
				);
		}

		// Get the copy policy
		String copyPolicy = withDPumpDefTag.getTextFromSinglePathAttr(
			"parameters_info", "copy_as"
			);
		if( copyPolicy == null )
			copyPolicy = "_null_";
		copyPolicy = copyPolicy.trim().toLowerCase();

		debugMsg( kFName, "copy policy is '" +
				copyPolicy + "'"
				);

		// Bail if we're told there's nothing to do
		if( copyPolicy.equals("none") )
			return;


		// Get the current paramaters node, if there is one
		// Whether it's OK to have, or not have, one is dependant on
		// the copy policy logic in use, see below.
		Element oldParams = toDPumpTag.findElementByPath( "parameters" );

		// Unless we're in a mode that does NOT want a new parameters set,
		// go ahead and set/create the parameters
		Element params = null;
		if( ! copyPolicy.equals("tag_as_params") )
		{
			// To date, all the conditions below would like there to
			// already be a parameters node in place, so for now just
			// create one if it isn't already there, we can always move
			// the logic later if any of the following don't like it
			if( oldParams == null )
			{

				// Create a new queue reference
				JDOMHelper tmpChild = null;
				try
				{
					tmpChild = new JDOMHelper( DPUMP_PARAMETERS_SKELETON, null );
				}
				catch (Exception e)
				{
					throw new XPumpException(
						"Error parsing skeleton dpump parameters entry definition, DPUMP_PARAMETERS_SKELETON='" +
						DPUMP_PARAMETERS_SKELETON +
						"', error: " + e
						);
				}

				// grab the parameters node
				params = tmpChild.getJdomElement();

				// And for now, since they all need it, just add
				// the new params branch to the dpump element
				params.detach();
				toDPumpTag.getJdomElement().addContent( params );
			}
			// Else the old params were NOT null, so they already exist
			else
				params = oldParams;
		}
		else    //  Else it is a mode that does NOT want parameters
			params = oldParams;
			// Later the code will complain if params/oldparams is not null

		// Shall we copy each child of the xpump node as a child
		// of the dpump parameters node?
		if( copyPolicy.equals("children_to_children") )
		{
			List sourceChildren = fromXPumpTag.getChildren();
			Iterator it = sourceChildren.iterator();
			while( it.hasNext() )
			{
				// Get the old child and clone it
				Element oldChild = (Element)it.next();
				Element newChild = (Element)oldChild.clone();
				// Add the new cloned child to the parameters
				newChild.detach();
				params.addContent( newChild );
			}
		}
		// Shall we copy the entire xpump node as a singular child
		// of the dpump parameters node?
		else if( copyPolicy.equals("tag_as_child") )
		{
			// To be safe, clone the xpump tag
			Element newChild = (Element)fromXPumpTag.clone();
			// Todo: allow for renaming it
			// Now add it to the parameters
			newChild.detach();
			params.addContent( newChild );
		}
		// Shall we copy each of the attributes as full tag children
		// Todo: allow for renaming it
		else if( copyPolicy.equals("attrs_as_children") )
		{
			// Get the attributes
			List attrs = fromXPumpTag.getAttributes();
			Iterator it2 = attrs.iterator();
			// For each attribute
			while( it2.hasNext() )
			{
				// Get the attribute
				Attribute attr = (Attribute)it2.next();
				// Get the name and contents
				String attrName = attr.getName();
				String attrValue = attr.getValue();
				if( attrValue == null || attrValue.trim().equals("") )
					continue;
				// Create a new element
				Element newChild = new Element( attrName );
				// Set the value
				newChild.addContent( attrValue );
				// Add it to parameters
				newChild.detach();
				params.addContent( newChild );
			}
			// We will also copy over any other children it had
			List sourceChildren = fromXPumpTag.getChildren();
			Iterator it3 = sourceChildren.iterator();
			while( it3.hasNext() )
			{
				// Get the old child and clone it
				Element oldChild = (Element)it3.next();
				Element newChild = (Element)oldChild.clone();
				// Add the new cloned child to the parameters
				newChild.detach();
				params.addContent( newChild );
			}
		}
		// Else are we to simply rename (after cloning) the
		// xpump tag to be "parameters" under dpump
		else if( copyPolicy.equals("tag_as_params") )
		{
			if( params != null )
				throw new XPumpException(
					"Compiler: Previous params not null with policy set to tag_as_params"
					);

			// To be safe, clone the xpump tag
			Element newParams = (Element)fromXPumpTag.clone();
			newParams.detach();
			// Now set it's new name
			newParams.setName( "parameters" );
			// Now add it to dpump process we're creating
			toDPumpTag.getJdomElement().addContent( newParams );
		}
		// Else is there some funky set of rules for this specific tag?
		else if( copyPolicy.equals("custom") )
		{
			// Todo: add support for custom copying
			throw new XPumpException(
				"\"custom\" copy policy not yet implemented, dpump proc def '" +
				withDPumpDefTag.JDOMToString() + "'"
				);
		}
		else
			throw new XPumpException(
				"Invalid or null copy parameters policy for dpump proc def '" +
				withDPumpDefTag.JDOMToString() + "'"
				);


	}

	void initHashes( JDOMHelper inDefTree )
		throws XPumpException
	{
		final String kFName = "initHashes";

		fXNameToDDefHash = new Hashtable();

		// Get the list of all definitions that map to xpump
		List procElements = inDefTree.findElementsByPath( "processor" );

		//List mappedElements = inTree.mixedListQuery( "processor", "xpump_info",
		//	"direct_xpump_replacement", "1", false
		//	);
		if( procElements.size() < 1 )
		{
			throw new XPumpException(
				"Didn't find any dpump definitions definition"
				);
		}
		else
			debugMsg( kFName, "recording " +
				procElements.size() + " dpump process definitions"
				);
		// Now run through the list
		Iterator it = procElements.iterator();
		// Track a counter so we can give better error messages
		int mapElemCounter = 0;
		while( it.hasNext() )
		{
			// Get the next process definition node
			Element defElem = (Element)it.next();
			// Find out if it claims to be mapping xpump commands
			boolean isXPumpMapped = JDOMHelper.getBooleanFromSinglePathAttr(
				defElem, "xpump_info", "direct_xpump_replacement", false
				);
			if( ! isXPumpMapped )
			{
				debugMsg( kFName,
						"skipping this process def because" +
						" it isn't marked direct_xpump_replacement, def='" +
						JDOMHelper.JDOMToString( defElem ) +
						"'" );
				continue;
			}
			mapElemCounter++;
			JDOMHelper defJhElem;
			try
			{
				defJhElem = new JDOMHelper( defElem );
			}
			catch (Exception e)
			{
				throw new XPumpException(
					"Error creating jdom helper object for dpump def entry '" +
					defElem.getName() + "', error: " + e
					);
			}

			// find all the aliases
			List xpumpNames = defJhElem.getAttrValuesListFromSingularPath(
				"xpump_info", "xpump_name" );
			if( xpumpNames.size() < 1 )
			{
				throw new XPumpException(
					"Didn't find any xpump mappings in dpump definition" +
					", process tag # " + mapElemCounter +
					", def tag = '" + defJhElem.JDOMToString() + "'"
					);
			}

			// Now run through the strings from the attr list
			Iterator it2 = xpumpNames.iterator();
			while( it2.hasNext() )
			{
				String xpumpName = (String)it2.next();
				xpumpName = xpumpName.toLowerCase().trim();
				if( xpumpName.equals("") )
				{
					throw new XPumpException(
						"null xpump name in dpump definition" +
						", process tag # " + mapElemCounter
						);
				}
				// Complain if it's already there
				if( fXNameToDDefHash.containsKey( xpumpName ) )
				{
					throw new XPumpException(
						"duplicate xpump name '" +
						xpumpName + "', in dpump definition" +
						", second one found in process tag # " +
						mapElemCounter
						);
				}
				// Add the reference
				fXNameToDDefHash.put( xpumpName, defJhElem );

				debugMsg( kFName,
						"adding xpump '" + xpumpName + "' entry for dpump " +
						"def class'" + defJhElem.JDOMToString() + "'"
						);

			}   // End for each string in the attr list

		}   // End for each process element we found in the def file

		// Also init some of the other hashes
		fXNameTagCountHash = new Hashtable();

		// Our master list of queues that are to be defined
		fQueueHash = new Hashtable();
		fQueueHadWritesToReference = new Hashtable();
		fQueueHadReadsFromReference = new Hashtable();
		fQueueHadUsesReference = new Hashtable();
		fQueueClassName = new Hashtable();
		fQueueIgnoreExitScan = new Hashtable();
		fQueueExtraParameters = new Hashtable();

	}

	private void __sep__Lower_Level_Logic__() {}
	///////////////////////////////////////////////////////////////////////

	void setDefaultQuueClass(
		Element inQueueElem, String inDisplayTagName
		)
		throws XPumpException
	{
		final String kFName = "setDefaultQuueClass";
		String className = JDOMHelper.getStringFromAttributeTrimOrNull( inQueueElem, "class" );
		if( null != className )
		{
			if( className.equalsIgnoreCase("disk") )
				className = "DiskQueue";
			fDefaultQueueClassName = className;

			// Check for special parameters that must be saved for later
			// based on the class name
			// Todo: revist this mechanism

			if( className.equals( "DiskQueue" ) )
			{
				// Find the directory they should have specified
				String dirName = JDOMHelper.getStringFromAttributeTrimOrNull( inQueueElem, "dir" );
				if( null==dirName )
					dirName = "q";
				fDefaultQueueDirBase = dirName;

				statusMsg( kFName, "Setting default disk queue, base dir ='" + dirName + "'");
			}   // End if it's a disk queue
		}
	}

	// Some queues need special consideration
	/***
	XPump:
	<queue name="my_name" class="classname" ignore_exit_scan="x" />
	// Todo: really shouldn't have them give specific class names
	DPump:
	<queues class="DiskQueue">
		and
	<queue name="to_tabulate">
		<parameters>
			<no_exit_scan />
		</parameters>
	</queue>

	Or how about:
	XPump:
	<queue name="my_name" class="DiskQueue" dir="mydir" />
	DPump:
	<queues class="DiskQueue">
	  <queue name="retriever_queue">
		  <directory_name>retriever_disk_queue</directory_name>
	  </queue>
	</queues>
	***/
	void processXPumpQueueDeclaration(
			Element inQueueElem, String inDisplayTagName
		)
		throws XPumpException
	{
		final String kFName = "processXPumpQueueDeclaration";
		
		String lQName = JDOMHelper.getStringFromAttributeTrimOrNull( inQueueElem, "name" );
		if( null == lQName )
			throw new XPumpException(
				inDisplayTagName + " is trying to declare a queue" +
				" but didn't specify a name for the queue" +
				" or it was null/empty" +
				"; you must set the name=\"xxx\" attribute when" +
				" declaring a queue."
				);
		if( fQueueHash.containsKey(lQName) )
			throw new XPumpException(
				inDisplayTagName +
				" has already been declared or referenced."
				);

		registerSpecificQueueName( lQName );

		if( JDOMHelper.getBooleanFromAttribute(
				inQueueElem, "ignore_exit_scan", false
				)
			)
		{
			fQueueIgnoreExitScan.put( lQName, new Boolean(true) );
		}

		String className = JDOMHelper.getStringFromAttributeTrimOrNull( inQueueElem, "class" );
		if( null != className || null != fDefaultQueueClassName )
		{
			if( null==className )
				className = fDefaultQueueClassName;

			if( className.equalsIgnoreCase("disk") )
				className = "DiskQueue";
			fQueueClassName.put( lQName, className.trim() );

			statusMsg( kFName, "Adding Q/class '" + lQName + "'/'" + className.trim() + "'" );

			// Check for special parameters that must be saved for later
			// based on the class name
			// Todo: revist this mechanism

			if( className.equals( "DiskQueue" ) )
			{
				// Find the directory they should have specified
				String dirName = JDOMHelper.getStringFromAttributeTrimOrNull( inQueueElem, "dir" );

				if( null == dirName )
				{
					if( null!=fDefaultQueueDirBase )
					{
						dirName = fDefaultQueueDirBase + '/' + inDisplayTagName;
						statusMsg( kFName, "Auto-generated Q dir = '" + dirName + "'" );
						// TODO: do it the long with the OS appropirate File join stuff, exceptions, etc. also in addQueueReferenceToProcess
					}
					else
						throw new XPumpException(
							"DiskQueue declarations must have dir attribute set"
							+ ", tag=" + JDOMHelper.JDOMToString( inQueueElem )
							);
				}
					
				// Form a compound key and store the results
				String hashKey = lQName + ":dir";
				fQueueExtraParameters.put( hashKey, dirName );
			}   // End if it's a disk queue

		}
		else {
			statusMsg( kFName, "No custom queue class for Q '" + inDisplayTagName + "'" );
		}
	}
	void processXPumpQueueDeclaration_v1(
			Element inQueueElem, String inDisplayTagName
		)
		throws XPumpException
	{
		String lQName = inQueueElem.getAttributeValue( "name" );
		if( lQName == null || lQName.trim().equals("") )
			throw new XPumpException(
				inDisplayTagName + " is trying to declare a queue" +
				" but didn't specify a name for the queue" +
				" or it was null/empty" +
				"; you must set the name=\"xxx\" attribute when" +
				" declaring a queue."
				);
		lQName = lQName.trim();
		if( fQueueHash.containsKey(lQName) )
			throw new XPumpException(
				inDisplayTagName +
				" has already been declared or referenced."
				);

		registerSpecificQueueName( lQName );

		if( JDOMHelper.getBooleanFromAttribute(
				inQueueElem, "ignore_exit_scan", false
				)
			)
		{
			fQueueIgnoreExitScan.put( lQName, new Boolean(true) );
		}

		String className = inQueueElem.getAttributeValue("class");
		if( className != null && ! className.trim().equals("") )
		{
			fQueueClassName.put( lQName, className.trim() );

			// Check for special parameters that must be saved for later
			// based on the class name
			// Todo: revist this mechanism

			if( className.equals( "DiskQueue" ) )
			{
				// Find the directory they should have specified
				String dirName = JDOMHelper.getStringFromAttribute(
					inQueueElem, "dir"
					);
				if( dirName == null || dirName.trim().equals("") )
					throw new XPumpException(
						"DiskQueue declarations must have dir attribute set"
						+ ", tag=" + JDOMHelper.JDOMToString( inQueueElem )
						);
				dirName = dirName.trim();
				// Form a compound key and store the results
				String hashKey = lQName + ":dir";
				fQueueExtraParameters.put( hashKey, dirName );
			}   // End if it's a disk queue

		}
	}

	// Special processing for "uses" queues
	// Currently this reduces to making sure we change the default
	// declaration of the class to use for any queues involved in this
	void markQueueAsUsesMode( String inQName )
		throws XPumpException
	{
		if( inQName == null || inQName.trim().equals("") )
			throw new XPumpException(
				"XPump:markQueueAsUsesMode: pass in null/empty queue name"
				);
		inQName = inQName.trim();
		// If this queue has not been declared, declare it now
		if( ! fQueueClassName.containsKey( inQName ) )
			fQueueClassName.put( inQName, DEFAULT_USES_QUEUE_CLASS_NAME );
	}


	void addQueueDefinitions( JDOMHelper inDPumpTree )
		throws XPumpException
	{
		final String kFName = "addQueueDefinitions";

		Set keys = new TreeSet( fQueueHash.keySet() );
		Iterator it = keys.iterator();
		if( ! it.hasNext() )
			warningMsg( kFName, "no queues to define" );
		while( it.hasNext() )
		{
			String qKey = (String)it.next();
			// For now we just pull back the same name
			String qValue = (String)fQueueHash.get( qKey );

			JDOMHelper newChild = generateQueueDefinition( qKey );

			if( ! fQueueClassName.containsKey( qKey ) )
			{
				// Now add the definition
				debugMsg( kFName, "Adding generic Q def for '" + qKey + "', key='" + qKey + "'" );
				addQueueDefinition( inDPumpTree, newChild );
			}
			else
			{
				debugMsg( kFName, "Adding Special Q def for '" + qKey + "', key='" + qKey + "'" );
				String className = (String)fQueueClassName.get( qKey );
				addQueueDefinition( inDPumpTree, newChild, className );
			}
		}
	}

//		String className = inQueueElem.getAttributeValue("class");
//		if( className != null || ! className.trim().equals("") )
//		{
//			fQueueClassName.put( lQName, className.trim() );
//		}
//
//		// Add the other options
//		if( fQueueIgnoreExitScan.containsKey( qName ) )
//		{
//			Boolean doItObj = (Boolean)fQueueIgnoreExitScan.get(qName);
//			if( doItObj.booleanValue() )
//				newChild.addXMLTextToPath(
//					null, DPUMP_QUEUE_NOSCAN_SKELETON
//				);
//		}

//		if( JDOMHelper.getBooleanFromAttribute(
//				inQueueElem, "ignore_exit_scan", false
//				)
//			)
//		{
//			fQueueIgnoreExitScan.put( lQName, new Boolean(true) );
//		}
//
//		String className = inQueueElem.getAttributeValue("class");
//		if( className != null || ! className.trim().equals("") )
//		{
//			fQueueClassName.put( lQName, className.trim() );
//		}




	JDOMHelper generateQueueDefinition( String qName )
		throws XPumpException
	{
		final String kFName = "generateQueueDefinition";

		// Create a new queue reference
		JDOMHelper newChild = null;
		try
		{
			newChild = new JDOMHelper( DPUMP_QUEUE_ENTRY_SKELETON, null );
		}
		catch (Exception e)
		{
			throw new XPumpException(
				"Error parsing skeleton dpump queue entry definition, DPUMP_QUEUE_ENTRY_SKELETON='" +
				DPUMP_QUEUE_ENTRY_SKELETON +
				"', error: " + e
				);
		}

		// Add the name of the queue
		newChild.getJdomElement().setAttribute( "name", qName );


		// Add the other options
		if( fQueueIgnoreExitScan.containsKey( qName ) )
		{
			Boolean doItObj = (Boolean)fQueueIgnoreExitScan.get(qName);
			if( doItObj.booleanValue() )
				newChild.addXMLTextToPath(
					null, DPUMP_QUEUE_NOSCAN_SKELETON
				);
		}

		// Get the class name for this queue
		String className = null;
		if( fQueueClassName.containsKey( qName ) )
			className = (String)fQueueClassName.get( qName );
		// It's fine if there isn't any

		// Add the directory setting for disk queues
		if( className != null && className.equals( "DiskQueue" ) )
		{
			// Form a compound key and store the results
			String hashKey = qName + ":dir";
			if( ! fQueueExtraParameters.containsKey( hashKey ) )
				throw new XPumpException(
					"Could not find 'dir' parameter for disk queue "
					+ qName + " in parameters hash"
					);
			String dirName = (String)fQueueExtraParameters.get( hashKey );
			if( dirName == null || dirName.trim().equals("") )
				throw new XPumpException(
					"'dir' parameter for disk queue from params hash was null/empty"
					+ " qName=" + qName
					);
			dirName = dirName.trim();
			// Now form the XML snippet like
			// <directory_name>my_dir</directory_name>
			String XMLText = "<directory_name>" + dirName
				+ "</directory_name>";
			// Now add this to the tree
			Element result = newChild.addXMLTextToPath( null, XMLText );
			if( result == null )
				throw new XPumpException(
					"failed to add disk queue dir setting to dpump queue definition"
					+ " XMLText=" + XMLText
					);

		}   // End if it's a disk queue






		// Do some checking on the references we had
		int readRefs = 0;
		try
		{
			readRefs = ((Integer)fQueueHadReadsFromReference.get(qName)).intValue();
		}
		catch (Exception e)
		{
			readRefs = 0;
		}
		int writeRefs = 0;
		try
		{
			writeRefs = ((Integer)fQueueHadWritesToReference.get(qName)).intValue();
		}
		catch (Exception e)
		{
			writeRefs = 0;
		}
		int usesRefs = 0;
		try
		{
			usesRefs = ((Integer)fQueueHadUsesReference.get(qName)).intValue();
		}
		catch (Exception e)
		{
			usesRefs = 0;
		}

		// Calculate a comment string and add it
		String commentStr = " Processor references:"
			+ " readers=" + readRefs
			+ ", writers=" + writeRefs
			+ ", users=" + usesRefs
			+ " ";

		// Now add the comment the definition
		newChild.getJdomElement().addContent( new Comment( commentStr ) );

		// Some warnings
		if( readRefs <= 0 && usesRefs <= 0 )
			warningMsg( kFName,
				"no readers from queue '" + qName + "'" +
				" (you might want to check your spelling)"
				);
		if( writeRefs <= 0 && usesRefs <= 0 )
			warningMsg( kFName,
				"no writers to queue '" + qName + "'" +
				" (you might want to check your spelling)"
				);

		// This is the individual queue item
		return newChild;
	}

	// This adds content to the compiled dpump tree
	// It adds content to the top section of dpump, where the
	// queues are defined in dpump.
	// There are two versions, depending on whether the class is known
	// or not.
	void addQueueDefinition( JDOMHelper tree, JDOMHelper newChild )
		throws XPumpException
	{
		// Add the new reference to process node's read list
		tree.addElementToPath( "queues", newChild.getJdomElement() );
	}
	void addQueueDefinition( JDOMHelper inDPTree, JDOMHelper inNewChild,
		String className
		)
		throws XPumpException
	{
		final String kFName = "addQueueDefinition";

		if( className == null || className.trim().equals("") )
		{
			addQueueDefinition( inDPTree, inNewChild );
			return;
		}
		className = className.trim();

		// See if we can find a tree
		Element existingQueueClass = inDPTree.mixedQuery(
			null, "queues", "class", className );
		if( existingQueueClass != null ) {
			// existingQueueClass.addContent( inNewChild.getJdomElement() );
			Element newChild = inNewChild.getJdomElement();
			newChild.detach();
			existingQueueClass.addContent( newChild );
		}
		else {
			// Create a new path and add

			// First, find the last existing queues entry
			List existingQsList = inDPTree.findElementsByPath( "queues" );

			// Now create a new queues entry, with the class name

			// Create a new queue reference
			JDOMHelper lNewQs = null;
			try
			{
				lNewQs = new JDOMHelper( DPUMP_QUEUES_SKELETON, null );
			}
			catch (Exception e)
			{
				throw new XPumpException(
					"Error parsing skeleton dpump queues definition" +
					", DPUMP_QUEUES_SKELETON='" +
					DPUMP_QUEUES_SKELETON +
					"', error: " + e
					);
			}
			// Add the class name to it
			lNewQs.getJdomElement().setAttribute( "class", className );

			// Disconnect from any document association
			lNewQs.getJdomElement().detach();
			// Add it to the END of the search we did for other queues elements
			// This has the affect of adding it to the main dpump tree
			//existingQsList.add( lNewQs );
			// This puts it at the end, but at least it works
			inDPTree.getJdomElement().addContent( lNewQs.getJdomElement() );

			// A little reminder of our variables
			// inNewChild   queue definition
			// lNewQs   the new queues section with class name
			// inDPTree the main tree
			// existingQsList   previously found "queues" entries

			// Add the queue definition to this new queues branch
			inNewChild.getJdomElement().detach();
			lNewQs.getJdomElement().addContent( inNewChild.getJdomElement() );
			// inDPTree.getJdomElement().addContent( inNewChild.getJdomElement() );

			debugMsg( kFName, "lNewQs="
				+ lNewQs.JDOMToString()
				);
		}
	}

	// "Create" a new queue with a specific name
	// Currently we return a String.  Later we might instantiate
	// an object that encapsulates more information.
	String registerSpecificQueueName( String inQueueName )
		throws XPumpException
	{
		return registerSpecificQueueName( inQueueName, null );
	}
	String registerSpecificQueueName( String inQueueName, String usageMode )
		throws XPumpException
	{
		if( inQueueName == null || inQueueName.trim().equals("") )
		{
			throw new XPumpException(
				"Error: XPump:registerSpecificQueueName2:"
				+ " null or empty queue name passed in"
				);
		}

		// Store it in the hash
		fQueueHash.put( inQueueName, inQueueName );

		// If it's a "uses" queue we may take special steps
		if( usageMode != null &&
			usageMode.trim().toLowerCase().equals("uses")
			)
		{
			markQueueAsUsesMode( inQueueName );
		}

		// For now, just return it
		return inQueueName;
	}

	// "Create" a new queue with an automatic name
	// Winds up calling registerSpecificQueueName above.
	// Currently we return a String.  Later we might instantiate
	// an object that encapsulates more information.
	String getNextAutomaticQueueName()
		throws XPumpException
	{
		return getNextAutomaticQueueName( null );
	}
	String getNextAutomaticQueueName( String usageMode )
		throws XPumpException
	{
		// We use a hash for this
		// It may be a little overkill now, but I do expect
		// that we'll enhance this in the future
		int currQCount = fQueueHash.size();

		// Form the new name
		String newQName = AUTO_QUEUE_NAME_PREFIX + currQCount;

		// Store it in the hash
		registerSpecificQueueName( newQName, usageMode );

		// Return the results
		return newQName;
	}

	void addQueueReferenceToProcess( JDOMHelper proc,
			String qBranch, String qName, int numInstances
			)
			throws XPumpException
	{
		final String kFName = "addQueueReferenceToProcess";

		if( qBranch == null || qBranch.trim().equals("") )
			throw new XPumpException(
				"qBranch was null"
				);
		qBranch = qBranch.trim();

		// Create a new queue reference
		JDOMHelper newChild = null;
		try
		{
			newChild = new JDOMHelper( DPUMP_QUEUE_REFERENCE_SKELETON, null );
		}
		catch (Exception e)
		{
			throw new XPumpException(
				"Error parsing skeleton dpump queue entry, DPUMP_QUEUE_REFERENCE_SKELETON='" +
				DPUMP_QUEUE_REFERENCE_SKELETON +
				"', error: " + e
				);
		}

		// Double check non-default queue, such ask disk based
		// Such as set_default_queue_class ... = disk
		// AND remember the que can be references multiple times but we only
		// need to do something the first time through
		debugMsg( kFName, "Checking class for Q '" + qName + "'" );
		if( null != fDefaultQueueClassName && ! fQueueClassName.containsKey(qName) )
		{
			fQueueClassName.put( qName, fDefaultQueueClassName );
			if( fDefaultQueueClassName.equals("DiskQueue") )
			{
				String dirName = fDefaultQueueDirBase + '/' + qName;
				// TODO: do it the long with the OS appropirate File join stuff, exceptions, etc. also in processXPumpQueueDeclaration
				debugMsg( kFName, "Auto-generated Q dir = '" + dirName + "'" );
				// Form a compound key and store the results
				String hashKey = qName + ":dir";
				fQueueExtraParameters.put( hashKey, dirName );
			}
		}
		else {
			debugMsg( kFName, "No class addition needed"
				+ ", fDefaultQueueClassName = '" + fDefaultQueueClassName + "'"
				+ ", fQueueHash.containsKey(qName) = " + fQueueHash.containsKey(qName)
				);
		}
		
		// Add the name of the queue
		newChild.getJdomElement().setAttribute( "name", qName );

		// Add the new reference to process node's read list
		proc.addElementToPath( qBranch, newChild.getJdomElement() );

		// Tabulate the hookups we've seen
		int oldCount = 0;
		// places the queue reads from
		if( qBranch.equals("reads_from") )
		{
			try
			{
				oldCount = ((Integer)fQueueHadReadsFromReference.get(qName)).intValue();
			}
			catch (Exception e)
			{
				oldCount = 0;
			}
			fQueueHadReadsFromReference.put( qName,
				new Integer(oldCount+numInstances)
				);
		}
		// places the queue writes to
		else if( qBranch.equals("writes_to") )
		{
			try
			{
				oldCount = ((Integer)fQueueHadWritesToReference.get(qName)).intValue();
			}
			catch (Exception e)
			{
				oldCount = 0;
			}
			fQueueHadWritesToReference.put( qName,
				new Integer(oldCount+numInstances)
				);
		}
		// places the queue writes to
		else if( qBranch.equals("uses") )
		{
			try
			{
				oldCount = ((Integer)fQueueHadUsesReference.get(qName)).intValue();
			}
			catch (Exception e)
			{
				oldCount = 0;
			}
			fQueueHadUsesReference.put( qName,
				new Integer(oldCount+numInstances)
				);
		}
		else
			throw new XPumpException(
				"Unknown queue connection type '" + qBranch + "'" +
				", should be reads_from or writes_to"
				);

	}

	void addQueueReferenceToProcess_v1( JDOMHelper proc,
		String qBranch, String qName, int numInstances
		)
		throws XPumpException
	{

		if( qBranch == null || qBranch.trim().equals("") )
			throw new XPumpException(
				"qBranch was null"
				);
		qBranch = qBranch.trim();

		// Create a new queue reference
		JDOMHelper newChild = null;
		try
		{
			newChild = new JDOMHelper( DPUMP_QUEUE_REFERENCE_SKELETON, null );
		}
		catch (Exception e)
		{
			throw new XPumpException(
				"Error parsing skeleton dpump queue entry, DPUMP_QUEUE_REFERENCE_SKELETON='" +
				DPUMP_QUEUE_REFERENCE_SKELETON +
				"', error: " + e
				);
		}

		// Add the name of the queue
		newChild.getJdomElement().setAttribute( "name", qName );

		// Add the new reference to process node's read list
		proc.addElementToPath( qBranch, newChild.getJdomElement() );

		// Tabulate the hookups we've seen
		int oldCount = 0;
		// places the queue reads from
		if( qBranch.equals("reads_from") )
		{
			try
			{
				oldCount = ((Integer)fQueueHadReadsFromReference.get(qName)).intValue();
			}
			catch (Exception e)
			{
				oldCount = 0;
			}
			fQueueHadReadsFromReference.put( qName,
				new Integer(oldCount+numInstances)
				);
		}
		// places the queue writes to
		else if( qBranch.equals("writes_to") )
		{
			try
			{
				oldCount = ((Integer)fQueueHadWritesToReference.get(qName)).intValue();
			}
			catch (Exception e)
			{
				oldCount = 0;
			}
			fQueueHadWritesToReference.put( qName,
				new Integer(oldCount+numInstances)
				);
		}
		// places the queue writes to
		else if( qBranch.equals("uses") )
		{
			try
			{
				oldCount = ((Integer)fQueueHadUsesReference.get(qName)).intValue();
			}
			catch (Exception e)
			{
				oldCount = 0;
			}
			fQueueHadUsesReference.put( qName,
				new Integer(oldCount+numInstances)
				);
		}
		else
			throw new XPumpException(
				"Unknown queue connection type '" + qBranch + "'" +
				", should be reads_from or writes_to"
				);

	}


	// In order to tell programmers about mistakes, we need to tell
	// them WHERE in the xpump the problem is.
	// We don't have line numbers, but we do have the tag name.
	// The only problem is, in a large file, which instance of the tag
	// is it?
	// This method keeps track of tag names and their occurance counts.
	// It maintains the hash of counts as well.
	// And, given a tag name, will return a name with _n on the end.
	String calculateFriendlyChildName( String inChildName )
	{
		int oldCount = 0;
		if( fXNameTagCountHash.containsKey( inChildName ) )
		{
			Integer intObj = (Integer)fXNameTagCountHash.get( inChildName );
			oldCount = intObj.intValue();
		}
		int newCount = oldCount + 1;
		fXNameTagCountHash.put( inChildName, new Integer(newCount) );
		return inChildName + "(" + newCount + ")";
	}

	JDOMHelper getXPump2DPumpDef()
		throws XPumpException
	{
		InputStream input = this.getClass().getResourceAsStream(
			XPUMP_TO_DPUMP_DEF_RES_NAME
			);

		if( input == null )
			throw new XPumpException(
				"Could locate system xpump2dpump definition resource '" +
				XPUMP_TO_DPUMP_DEF_RES_NAME +
				"'" );

		JDOMHelper map = null;
		try
		{
			map = new JDOMHelper( input );
			input.close();
		}
		catch (Exception e)
		{
			try { input.close(); } catch (Exception e2) {}
			throw new XPumpException(
				"Error parsing system xpump2dpump definition: " +
				e );
		}

		return map;

		//String name = "nie.core.system.test.xml";
		//String name = "system/test.xml";
		//String name = "test.xml";
		//String name = "system/test.xml";

		//URL uri = this.getClass().getResource( name );
		//if( uri != null )
		//	System.out.println( "getRes for '" + name + "' is '" +
		//		uri.toExternalForm() +
		//		"'" );
		//else
		//	System.err.println( "getRes for '" + name + "' was null." );
		//InputStream input = this.getClass().getResourceAsStream( name );

		// Class.getClassLoader()
		//ClassLoader cl = this.getClass().getClassLoader();
		//InputStream in = cl.getResourceAsStream( name );

		//ClassLoader cl = new ClassLoader();
		//InputStream in = cl.getResourceAsStream( name );

		// ClassLoader.getSystemResourceAsStream(
	}



	private static void __sep__Simple_Sets_and_Gets__() {}

	public String getConfigFileURI()
	{
		return fFullConfigFileURI;
	}


	private void __sep__Main__() {}
	////////////////////////////////////////////////////////////

	public static void main( String[] inArgs )
	{
		final String kFName = "main";

		if( inArgs.length < 1 ) {
			fatalErrorMsg( kFName, "Must specify XPump source code file." );
			System.exit( 1 );
		}

		try
		{
			XPump lXPump = new XPump( inArgs[0] );
			statusMsg( kFName, "About to run " + inArgs[0] );
			lXPump.run();
			statusMsg( kFName, "Finished running " + inArgs[0] );
		}
		catch( XPumpException xe )
		{
			// TODO: bring into modern logging
			System.err.println( xe );
			System.err.println( xe.getMessage() );
			xe.printStackTrace();
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






	private void __sep__Member_Fields_and_Constants__() {}
	//////////////////////////////////////////////////////////////////

	// The root of the file we read in
	JDOMHelper fJdh;
	// Where we read the original config from
	String fFullConfigFileURI;
	// Our compiled dpump tree
	Element fDPumpTree;
	// the main dpump layer
	DPump fMainDPumpInstance;

	// The main hash for converting from xpump function names
	// to dpump names
	Hashtable fXNameToDDefHash;

	// The count of how many times we've seen each tag name
	Hashtable fXNameTagCountHash;

	// The queues that we have generated
	Hashtable fQueueHash;
	// Hashes to make sure that both ends of queues were
	// attached to something
	Hashtable fQueueHadWritesToReference;
	Hashtable fQueueHadReadsFromReference;
	Hashtable fQueueHadUsesReference;
	// Hashes to hold special qualifiers
	Hashtable fQueueClassName;
	Hashtable fQueueIgnoreExitScan;
	// An ugly hack to hold addtional parameters for a few classes
	// of queues.
	// Example: DiskQueue needs a directory specified
	// If you had <queue class="DiskQueue" name="foo" dir="mydir" />
	// We would store:
	// "foo:dir" -> "mydir"
	// Todo: revisit this mess when we revisit queue syntax in dpump
	Hashtable fQueueExtraParameters;

	// Single variables
	String fDefaultQueueClassName;
	String fDefaultQueueDirBase;

	// Some constants
	private static final String XPUMP_MAIN_TAG_NAME = "xpump";
	// If it's xpump, and we want to save the compiled dpump, we
	// can add an attribute to the main xpump tag
	private static final String WRITE_COMPILED_DPUMP_LOCATION =
		"save_dpump_loc";

	// The name of the "resource" that defines where our
	// XML mapping data comes from
	private static final String XPUMP_TO_DPUMP_DEF_RES_NAME =
		"system/dpump_proc_def.xml";

	// What class of queue to reference by default
	// private static final String DEFAULT_QUEUE_CLASS_NAME = "BasicQueue";
	private static final String DEFAULT_QUEUE_CLASS_NAME =
		"PrioritizingBasicQueue";
	// Warning: should probably override if code has cycles
	private static final String DEFAULT_USES_QUEUE_CLASS_NAME =
		"TriggerQueue";

	public static final String GROUP_TAG = "tag_group";

	// The base DPump document to create
	private static final String DPUMP_SKELETON =
		"<dpump>" +
		"  <queues class=\"" + DEFAULT_QUEUE_CLASS_NAME + "\" />" +
		"  <processors />" +
		"</dpump>";

	// The basic XML for process entry
	private static final String DPUMP_PROCESS_ENTRY_SKELETON =
		"<processor>" +
		"  <reads_from />" +
		"  <writes_to />" +
		"  <uses />" +
		"</processor>";

	// The basic XML for a queue entry
	private static final String DPUMP_QUEUE_ENTRY_SKELETON =
		"<queue />";
	// The basic XML for a queues branch
	private static final String DPUMP_QUEUES_SKELETON =
		"<queues />";


	// The basic XML for a queue reference
	private static final String DPUMP_QUEUE_REFERENCE_SKELETON =
		"<queue_connection />";

	private static final String DPUMP_QUEUE_NOSCAN_SKELETON =
		"<parameters>" +
		"  <no_exit_scan />" +
		"</parameters>";

	private static final String DPUMP_PARAMETERS_SKELETON =
		"<parameters />";

	// The prefix to add to queue names that we automatically generate
	private static final String AUTO_QUEUE_NAME_PREFIX =
		"_q_";

}
