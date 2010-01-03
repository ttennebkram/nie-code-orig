//package nie.processors;
package nie.pump.processors;

import java.util.*;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;
//import nie.core.operators.*;
import nie.pump.operators.*;

import org.jdom.*;

/**
 * Title:        ConditionalRouter.java
 * Description:  Send to a queue when a matching condition is found
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop and Mark Benett
 * @version 2.0
 *
 */

public class ConditionalRouter extends Processor
{
	public String kClassName() { return "ConditionalRouter"; }

	public ConditionalRouter ( Application inApplication,
							nie.pump.base.Queue[] inReadQueueList,
							nie.pump.base.Queue[] inWriteQueueList,
							nie.pump.base.Queue[] inUsesQueueList,
							Element inParameter,
							String inID )
	{
		super( inApplication, inReadQueueList, inWriteQueueList, inUsesQueueList, inParameter, inID );
		final String kFName = "constructor";
		if( inParameter == null )
		{
			// System.err.println( "ConditionalRouter: no parameters passed in!" );
			fatalErrorMsg( kFName, "no parameters passed in!" );
			System.exit(1);
		}

		// Capture the parameters
		try
		{
			fJdh = new JDOMHelper( inParameter );
		}
		catch (Exception e)
		{
			fJdh = null;
			// System.err.println( 
			//	"ConditionalRouter: Error creating jdom helper for parameter\n"
			//	+ e
			//	);
			fatalErrorMsg( kFName,
					"Error creating jdom helper for parameter\n"
					+ e
					);
			System.exit(1);
		}

		// Check for and setup the conditional tags
		recordConditionalInstructions();
		// also sets int fNumInstructions

		//fQueueList = inWriteQueueList;
		if( inReadQueueList == null
			|| inReadQueueList.length != 1
			|| inReadQueueList[0] == null
			)
		{
			// System.out.println( "ConditionalRouter:"
			//	+ " You must specify exactly one input queue to " + inID
			//	);
			fatalErrorMsg( kFName,
					"You must specify exactly one input queue to " + inID
					);
			System.exit( -1 );
		}
		fReadQueue = inReadQueueList[0];

		if( inWriteQueueList == null || inWriteQueueList[0] == null )
		{
			// System.out.println( "ConditionalRouter:"
			//	+ "You must specify an output queue to " + inID
			//	);
			fatalErrorMsg( kFName,
					"You must specify an output queue to " + inID
					);
			System.exit( -1 );
		}
		if( inWriteQueueList.length < fNumInstructions + 1
			|| inWriteQueueList.length > fNumInstructions + 2
			)
		{
			//System.out.println( "ConditionalRouter:"
			//		+ " Wrong number of output queues for " + inID
			//		+ " Each conditional element must have exactly one output queue"
			//		+ " and there must also be one additional ''else'' queue."
			//		+ " Optionally there can be a final error queue."
			//		+ " Numver of conditional tags found = " + fNumInstructions
			//		+ ", therefore expected range of input queues is"
			//		+ " either " + (fNumInstructions + 1)
			//		+ " or " + (fNumInstructions + 2) + " queues"
			//		+ ", actual number passed in = " + (inWriteQueueList.length)
			//		);
			errorMsg( kFName,
				"Wrong number of output queues for " + inID
				+ " Each conditional element must have exactly one output queue"
				+ " and there must also be one additional ''else'' queue."
				+ " Optionally there can be a final error queue."
				+ " Numver of conditional tags found = " + fNumInstructions
				+ ", therefore expected range of input queues is"
				+ " either " + (fNumInstructions + 1)
				+ " or " + (fNumInstructions + 2) + " queues"
				+ ", actual number passed in = " + (inWriteQueueList.length)
				);
		}

		// Copy of the write queues
		fWriteQueues = new Vector();
		for( int i = 0; i < fNumInstructions; i++ )
			fWriteQueues.add( inWriteQueueList[i] );

		// Set the else queue
		// It's # instr + 1 then -1 to convert from ordinal to offset
		fElseQueue = inWriteQueueList[ fNumInstructions ];

		// Set the error queue, if any
		// In this case it's # instr + 2 - 1 = + 1
		if( inWriteQueueList.length == fNumInstructions + 2 )
			fErrorQueue = inWriteQueueList[ fNumInstructions + 1 ];
		else
			fErrorQueue = null;
			// Not really necessary, should be init'd to null
	}

	private void recordConditionalInstructions()
	{
		final String kFName = "constructor";
		fInstructions = new Vector();
		List condTags = fJdh.findElementsByPath( CONDITIONAL_TAG );
		if( condTags == null || condTags.size() < 0 )
		{
			//System.out.println( "ConditionalRouter: No conditional tags found"
			//	+ " Should have at least one " + CONDITIONAL_TAG + " element."
			//	);
			fatalErrorMsg( kFName, "No conditional tags found"
					+ " Should have at least one " + CONDITIONAL_TAG + " element."
					);
			System.exit( -1 );
		}

		fNumInstructions = condTags.size();
		int condTagCounter = 0;
		for( Iterator it = condTags.iterator(); it.hasNext(); )
		{
			// get the high level conditional from the parameters
			Element condTag = (Element)it.next();
			condTagCounter++;
			// Now get it's only child
			// Todo: someday we should check for other children and complain
			Element opTag = JDOMHelper.findElementByPath( condTag, "[1]" );
			// Sanity check
			if( opTag == null || opTag.getName() == null
				|| opTag.getName().trim().equals("")
				)
			{
				//System.out.println( "ConditionalRouter:"
				//	+ " null/no conditional operator for conditional tag # "
				//	+ condTagCounter
				//	);
				fatalErrorMsg( kFName,
						"null/no conditional operator for conditional tag # "
						+ condTagCounter
						);
				System.exit( -1 );
			}

			// The name of the operator is the name of the tag
			String opName = opTag.getName();

			// Now we instantiate one of the classes that has
			// the .evalute( work unit ) method
			OpTreeInterface newOp = null;
			// Either constructor can throw an exception
			try
			{
				// If it looks like a boolean, try to instantiate it
				if( BooleanOpTreeEvaluator.getIsABoolOp( opName ) )
				{
					newOp = new BooleanOpTreeEvaluator( opTag );
				}
				// Else if it's not a boolean, it must be a leaf
				else
				{
					newOp = new LeafOpTreeEvaluator( opTag );
				}
			}
			catch(Exception e)
			{
				// System.err.println( "ConditionalRouter:"
				//	+ " error instanting operator for conditional tag # "
				//	+ condTagCounter
				//	+ ", opname=" + opName
				//	+ ", error was: " + e
				//	);
				fatalErrorMsg( kFName,
						"error instanting operator (1) for conditional tag # "
						+ condTagCounter
						+ ", opname=" + opName
						+ ", error was: " + e
						);
				e.printStackTrace( System.err );
				System.exit( -1 );
			}
			// And triple check that we got soemthing
			// This should never happen
			if( newOp == null )
			{
				// System.err.println( "ConditionalRouter:"
				//	+ " error instanting operator for conditional tag # "
				//	+ condTagCounter
				//	+ ", opname=" + opName
				//	+ ", constructor returned a null"
				//	);
				fatalErrorMsg( kFName,
						"error instanting operator (2) for conditional tag # "
						+ condTagCounter
						+ ", opname=" + opName
						+ ", constructor returned a null"
						);
				System.exit( -1 );
			}

			// Now, finally, add this compiled operator to the list
			fInstructions.add( newOp );
		}

	}


	public void run()
	{
		try
		{
		    mWorkUnit = null;
			while( true )
			{
				int lWhichQueue = -1;
				// WorkUnit lWorkUnit = dequeue( fReadQueue );
				mWorkUnit = dequeue( fReadQueue );

				// Result will be a zero based offset of what queue to use
				// -1 will mean none matched
				lWhichQueue = processWorkUnit( mWorkUnit );

				/***
				// First we check for errors
				if( ! lWorkUnit.getIsValidRecord() )
				{
					if( fErrorQueue != null )
						enqueue( fErrorQueue, lWorkUnit );
					else
						enqueue( fElseQueue, lWorkUnit );
				}
				***/
				// Else did it match any of the conditions?
				/*else*/ if( lWhichQueue >= 0 )
				{
					nie.pump.base.Queue targetQueue = (nie.pump.base.Queue)fWriteQueues.get( lWhichQueue );
					enqueue( targetQueue, mWorkUnit );
				}
				// Else it simply didn't match anything
				else
				{
					// Send it to the else queue
					enqueue( fElseQueue, mWorkUnit );
				}
				mWorkUnit = null;
			}
		}
		catch( InterruptedException ie )
		{
		}
	}

	int processWorkUnit( WorkUnit inWU )
		// throws OpTreeException
	{
	    final String kFName = "processWorkUnit";

		int whichMatch = -1;

		/***
		if( ! inWU.getIsValidRecord() )
		{
			System.err.println( "Warning: ConditionalRouter:"
				+ " Recieved a work unit that was already invalid;"
				+ " it will not be run against these conditionals."
				);
			return whichMatch;
		}
		***/

		int opCounter = 0;
		for( Iterator it = fInstructions.iterator(); it.hasNext(); )
		{
			OpTreeInterface opObj = (OpTreeInterface)it.next();
			opCounter++;
			boolean result = false;
			boolean shouldBreak = false;
			try
			{
				result = opObj.evaluate( inWU );
			}
			catch(OpTreeException e)
			{
				mWorkUnit.stackTrace( this, kFName, e,
					"Got exception when evaluating condition # " + opCounter
					+ ", will route to else queue (or error queue if specified)"
					+ ", Exception was: " + e
					);
				inWU.setIsValidRecord( false );
				result = false;
				shouldBreak = true;
			}

			// System.out.println( "result=" + result );

			// OK to have broken within catch block?
			if( shouldBreak )
				break;
			// Record the match and break, if we found one
			if( result )
			{
				whichMatch = opCounter - 1;
				break;
			}
		}

		// System.out.println( "whichMatch=" + whichMatch );

		// Return the answer
		return whichMatch;
	}


	static final String CONDITIONAL_TAG = "test";

	//static Comparator fElementComparator;
	//static Comparator fAttributeComparator;

	JDOMHelper fJdh;
	WorkUnit mWorkUnit;
	List fInstructions;
	int fNumInstructions;
	nie.pump.base.Queue fReadQueue;
	List fWriteQueues;
	nie.pump.base.Queue fElseQueue;
	nie.pump.base.Queue fErrorQueue;
	List fConditions;
}
