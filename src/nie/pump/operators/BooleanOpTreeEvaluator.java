// Traverse a conditional tree and return a boolean

// Todo: check number of children for not operaor

//package nie.core.operators;
package nie.pump.operators;
import nie.core.*;

import org.jdom.Element;
import java.util.*;

import nie.pump.base.WorkUnit;

//class BoolTag extends BaseTag
public class BooleanOpTreeEvaluator
	extends JDOMHelper
	implements OpTreeInterface
{

	private static final boolean debug = false;

	private void __sep__Constructors_and_Init__ () {}
	////////////////////////////////////////////////////////////



	// You must define a constructor, even if it's just
	// calling super().  Otherwise Java wants default
	// constructors all the way up the derived food chain,
	// which we don't have.
	public BooleanOpTreeEvaluator( Element element )
		throws OpTreeException, JDOMHelperException
	{
		super( element );

		if( ! getIsABoolOp( getElementName() ) )
		{
			System.err.println( "BooleanOpTreeEvaluator:"
				+ " Not a boolean operator: " + getElementName()
				);
			System.exit(1);
		}

		// Set some flags
		initSettings();

		// we'll just let setupChildren's exception pass up the food chain
		setupChildren();

		if(debug) System.out.println( "Debug: new BoolTree tag constructed." );
	}


	void setupChildren()
		throws OpTreeException
	{

		if(debug)
		{
			System.out.println(
			"Debug: BaseTag: setupChildren() for '" +
			getElementName() + "'." );
			System.out.println(
				"\tWill consider " +
				getJdomChildrenCount() +
				" jdom children." );
		}

		if( fChildTagList != null )
			throw new OpTreeException(
				"childTagList already initialized!" +
				"  You can only run a tag's .setupChildren() method onces per instance."
				);

		fChildTagList = new Vector();

		// Each jdom child is a potential tag style child
		// For each jdom element
		for( int i = 0; i < getJdomChildrenCount(); i++ )
		{
			Element jdomElement = getJdomChildByOffset(i);
			String elemName = jdomElement.getName();

			OpTreeInterface newChild = null;

			try
			{
				if( getIsABoolOp( elemName ) )
				{
					newChild = new BooleanOpTreeEvaluator( jdomElement );
				}
				else
				{
					newChild = new LeafOpTreeEvaluator( jdomElement );
				}
			}
			catch( Exception e )
			{
				throw new OpTreeException(
					"Error constructing child op tag:"
					+ " element: '" + elemName + "'"
					+ " Exception: " + e
					);
			}

			if( newChild != null )
				addChildTag( newChild );
			else
				throw new OpTreeException(
					"Got back a null from constructing child op tag:"
					+ " element: '" + elemName + "'"
					);

//			if( shouldInstantiateThisJdomElement( jdomElement ) )
//		    {
//
//				// Call the main application factory
//				BaseTag newTag = getMainApp().makeTag( jdomElement );
//
//				if( newTag != null )
//					// Save it
//					addChildTag( newTag );
//				else
//					throw new Exception(
//						"Warning: Got back null node from Main Application factory for JDom element '" +
//						jdomElement.getName() + "'" );
//
//			}  // End if should instantiate


		}  // End for each jdom element

	}


	/***************************************************
	*
	*	Higher Level Process Logic
	*	--------------------------
	*	* Good candidates to be overridden in derived
	*	  classes if you want to change the behavior.
	*
	****************************************************/
	private void __sep__Process_Logic__ () {}
	/////////////////////////////////////////////////////////////////

	// Will be called by the parent tag node or master app
	// ----------------------------------------------------
	public boolean evaluate( WorkUnit inWU )
		throws OpTreeException
	{

		// We don't really need the work unit, but many of the
		// leaf node operators do, so we need it to pass down to them

		// Push us on the run stack
		// and REMEMBER TO TAKE US OFF at the end or
		// before throwing/passing on an exception
		//getMainApp().pushTag( this );

		if(debug)
			System.out.println(
				"Debug: BaseTag: run() for '" +
				getElementName() + "'."
				);

		// This is the ongoing results of our efforts
		// In this tree traversal we actually use it in a slightly
		// different way than a normal tree traversal would
		boolean keepGoing = false;

		// Run my setup processing
		keepGoing = runMyselfBeforeChildren();

		// Process my children
		// By default we DO process the children
		// even if the runMyself returned false.
		// But we DO pass that in, so you can override it
		// if you need to and use that.
		keepGoing = runTagChildren( keepGoing, inWU );

		// In this case we run the cleanup no matter what
		// It returns back the final tally
		keepGoing = runMyselfAfterChildren( keepGoing );

		// Take ourselves off the stack
		//getMainApp().popTag();

		// Signal to the parent whether I think they
		// should continue on with my subsequent siblings
		return keepGoing;

	}

	// Initialize some state variables
	boolean runMyselfBeforeChildren()
	{

		// Get the variables off to a good start
		fHaveSeenAnyChildren = false;
		fDidAllChildrenMatch = false;
		fDidAnyChildrenMatch = false;

		return true;
	}


	// This function will be run after the children are
	// ==============================================================
	boolean runMyselfAfterChildren( boolean inputKeepGoing )
		throws OpTreeException
	{

		if(debug)
			System.out.println(
				"Debug: BoolTag:runMyselfAfterChildren:" +
				" Starting with input keep going of '" + inputKeepGoing + "'"
				);

		// A nice default action
		boolean keepGoing = inputKeepGoing;

		if(debug)
		{
			System.out.println(
				"Debug: BoolTag: runMyselfAfterChildren() for '" +
				getElementName() + "'." );
			System.out.println( "\tgetIsAndOp() =" + getIsAndOp());
			System.out.println( "\tgetIsOrOp() =" + getIsOrOp());
			System.out.println( "\tgetIsTheNotOp() =" + getIsTheNotOp());
			System.out.println( "\tfHaveSeenAnyChildren =" +
				fHaveSeenAnyChildren);
			System.out.println( "\tfDidAllChildrenMatch =" +
				fDidAllChildrenMatch);
			System.out.println( "\tfDidAnyChildrenMatch =" +
				fDidAnyChildrenMatch);
		}

		// We start by assuming False
		boolean areWeHappy = false;
		// Then we check for some cases where it might be true
		if( fHaveSeenAnyChildren )
		{
			// If it was AND, and all the children were TRUE,
			// then we'll return True
			if( getIsAndOp() && fDidAllChildrenMatch )
				areWeHappy = true;
			// If it was OR, and any of the children were TRUE,
			// then we'll return true
			else if( getIsOrOp() && fDidAnyChildrenMatch )
				areWeHappy = true;
			// If it was NOT, and it's child was FALSE,
			// then we'll return true
			else if( getIsTheNotOp() && ! fDidAnyChildrenMatch )
				areWeHappy = true;

			// Else the condition was not met for the operator
			// we are trying for, so leave it to the default of false
		}
		// Else no children were run
		// This should never happen as this would be caught earlier
		else
			throw new OpTreeException(
				"runMyselfAfterChildren: report that no children were run for " +
				getElementName()
				);


//		if( ! areWeHappy && isAMatchRequired() )
//		{
//			//String tmpMsg;
//			String tmpMsg = getFailureMessageWithDefault();
//			if( ! fHaveSeenAnyChildren )
//				tmpMsg += ": boolean operators require at least one child";
//			else if( getIsAndOp() )
//				tmpMsg += ": the AND operator requires all of it's children to match";
//			else if( getIsOrOp() )
//				tmpMsg += ": the OR operator requires at least one of it's children to match";
//			else
//				tmpMsg += ": this new boolean operator's conditions were not satisfied";
//
//			// Add a little more information to the front
//			//tmpMsg = "'" + getElementName() +
//			//	"' tag failed assertion because " + tmpMsg;
//
//			getMainApp().sendMessageUpStack( "invalidRecord", tmpMsg );
//
//		}

		return areWeHappy;
	}


	// The main loop for processing the children
	// ------------------------------------------
	boolean runTagChildren( boolean inputKeepGoing, WorkUnit inWU )
		throws OpTreeException
	{

		if(debug)
		{
			System.out.println(
				"Debug: BoolTag: runTagChildren() for '" +
				getElementName() + "'."
				);
			System.out.println( "\tAbout to run " +
				getNumTagChildren() + " childen."
				);
		}

		if( getNumTagChildren() < 1 )
		{
			throw new OpTreeException(
				"Error: boolean tag '" + getElementName() +
				"' must have at least one child."
				);
			// just return true, the post processing will figure it out
			// return false;
		}

		// Todo: if <not>, check for one child

		// The childrens' continueing advice on whether to
		// keep processing them.
		boolean keepGoing = true;

		// The main loop, will run each child
		// indirectly via a casting caller
		for( int i=0; i<getNumTagChildren(); i++ )
		{

			OpTreeInterface child = getTagChildByOffset( i );

			//keepGoing = runIndividualChildTag( child );
			keepGoing = child.evaluate( inWU );

			// Special case on first child
			if( ! fHaveSeenAnyChildren )
			{
				fHaveSeenAnyChildren = true;
				// Seed the all children flag
				fDidAllChildrenMatch = keepGoing;
			}
			// Else not the first child
			else
			{
				// once the flag is FALSE, it should stay false
				if( fDidAllChildrenMatch && ! keepGoing )
					fDidAllChildrenMatch = false;
			}

			// Once any children have matched, the flag should stay on
			if( ! fDidAnyChildrenMatch && keepGoing )
				fDidAnyChildrenMatch = true;


			// Boolean short circuit logic

			// Examine the instances where we might like to
			// break out of the loop
			// This will implement the "short circuiting" for
			// children of boolean operators, which is a good thing

			// Once we've done any children, we can just stop
			// if it's the not operator
			if( getIsTheNotOp() )
			{
				break;
			}
			// If we're AND'ing and any are false we don't
			// need to go any further, we're hosed!
			else if( getIsAndOp() && ! keepGoing )
			{
				break;
			}
			// If we're OR'ing and had a match, we've had success,
			// no point in going further
			else if( getIsOrOp() && keepGoing )
			{
				break;
			}
			// Else we're in a mode that requires us to keep going
			// We have an OR, with no true's so far
			// or an AND, with no false's so far

			// We have already factored in the child's input
			// In the case of OR we SHOULD keep going
			// Bail if the last child said so
			//if( ! keepGoing )
			//	break;

		}

		return keepGoing;

	}


	/**
	  * This runs one of the children
	  * It's important to get the casting right, so that the
	  * proper method is invoked.
	  * This logic is highly dependant on what types of nodes
	  * this node expects to have as children.
	 **/
//	boolean runIndividualChildTag( JDOMHelper tag )
//		throws Exception
//	{
//
//		if(debug) System.out.println(
//			"Debug: BaseTag: runIndividualChildTag() for '" +
//			getElementName() + "'." );
//
//		// A signal from our child to us _advising_ us on
//		// whether they think we should continue processing
//		// their subsequent siblings
//		boolean keepGoing = true;
//
//		keepGoing = tag.run();
//
//
//		/*
//		 * Another strategy would be to have a handleXyz for
//		 * each type you'd like to handle in the classes
//		 * you derive and override this function.  That way you
//		 * could intercept the results.
//		 * I wouldn't bother unless you really need to.
//		 * See the example below.
//		 */
//		//if( classType.equals("XyzTagType") ) {
//		//	keepGoing = handleXyzTag( tag );
//		//}
//		// *** See template of handleXyzTag method below ***
//
//		// Return the advice about whether to keep going or not
//		return keepGoing;
//
//	}

	// Template for a more advanced handleXyzTag method
	//boolean handleXyzType( BaseTag tag ) {
	//	// child's advice on whether we should keep going or not
	//	boolean keepGoing = true;
	//
	//	... your advanced logic here ...
	//
	//	// Probably still run the child at some point
	//	keepGoing = ((propper cast to correct type)tag).run();
	//
	//	... possibly more of your advanced logic here ...
	//
	//	// return child's advice on whether we should keep going or not
	//	return keepGoing;
	//}


	/***************************************************
	*
	*	Simple Get/Set
	*
	****************************************************/
	private void __sep__Simple_Get_and_Set__ () {}
	////////////////////////////////////////////////////////////

//	getElementName()
//	{
//	}
//	getJdomChildrenCount()
//	{
//	}
//	getJdomChildByOffset( int i )
//	{
//	}

	private void initSettings()
	{
		fHaveDoneInit = false;
		fIsAndOp = getIsAndOp();
		fIsOrOp = getIsOrOp();
		fIsTheNotOp = getIsTheNotOp();
		fHaveDoneInit = true;
	}

	boolean getIsAndOp()
	{
		if( ! fHaveDoneInit )
			return getElementName().trim().toLowerCase().equals(AND_TAG_NAME);
		else
			return fIsAndOp;
	}
	boolean getIsOrOp()
	{
		if( ! fHaveDoneInit )
			return getElementName().trim().toLowerCase().equals(OR_TAG_NAME);
		else
			return fIsOrOp;
	}
	boolean getIsTheNotOp()
	{
		if( ! fHaveDoneInit )
			return getElementName().trim().toLowerCase().equals(NOT_TAG_NAME);
		else
			return fIsTheNotOp;
	}

	public static boolean getIsABoolOp( String inName )
		throws OpTreeException
	{
		if( inName == null || inName.trim().equals("") )
			throw new OpTreeException(
				"isBoolOp was passed null/empty opname/string to check."
				);
		inName = inName.trim().toLowerCase();
		if( inName.equals( AND_TAG_NAME ) || inName.equals( OR_TAG_NAME ) ||
			inName.equals( NOT_TAG_NAME )
			)
		{
			return true;
		}
		else
			return false;
	}

	List getChildTagList() {
		return fChildTagList;
	}

	int getNumTagChildren() {
		return getChildTagList().size();
	}

	void addChildTag( OpTreeInterface newTag )
	{
		getChildTagList().add( newTag );
	}

	OpTreeInterface getTagChildByOffset( int offset )
	{
		return (OpTreeInterface)getChildTagList().get( offset );
	}

	//boolean isAMatchRequired()
	//{
	//	return getBooleanFromAttribute( "require_match" );
	//}

	private void __sep__Constants_and_Fields__ () {}

	// The list of instantiated Op children
	private List fChildTagList;

	// Names of the operators
	// WARNING: If you add to this list you should probably also
	// update getIsBoolOp( String )
	private static final String AND_TAG_NAME = "and";
	private static final String OR_TAG_NAME = "or";
	private static final String NOT_TAG_NAME = "not";

	// Some handy state variables
	boolean fHaveSeenAnyChildren;
	boolean fDidAllChildrenMatch;
	boolean fDidAnyChildrenMatch;

	// Cache some the answers to frequent questions
	boolean fHaveDoneInit;
	boolean fIsTheNotOp;
	boolean fIsAndOp;
	boolean fIsOrOp;

	// The current work unit, if any
	//WorkUnit fCurrentWorkUnit;

}
