// Traverse a String tree and return a String

//package nie.core.operators;
package nie.pump.operators;
import nie.core.*;

import org.jdom.Element;
import java.util.*;

import nie.pump.base.WorkUnit;

//class BoolTag extends BaseTag
public class StrTreeEvaluator
	extends JDOMHelper
	implements StrTreeInterface
{

	private static final boolean debug = false;

	private void __sep__Constructors_and_Init__ () {}
	////////////////////////////////////////////////////////////


	public StrTreeEvaluator( Element element )
		throws OpTreeException, JDOMHelperException
	{
		this( element, true );
	}

	// You must define a constructor, even if it's just
	// calling super().  Otherwise Java wants default
	// constructors all the way up the derived food chain,
	// which we don't have.
	public StrTreeEvaluator( Element element, boolean checkTagName )
		throws OpTreeException, JDOMHelperException
	{
		super( element );

		if(debug) System.err.println( "StrTree: const 2" );

		if( checkTagName )
		{
			if(debug) System.err.println( "StrTree: checking "
				+ getElementName()
				);
			if( ! getIsASpanOp( getElementName() ) )
			{
				System.err.println( "BooleanOpTreeEvaluator:"
					+ " Not a boolean operator: " + getElementName()
					);
				System.exit(1);
			}
		}
		else
		{
			if(debug) System.err.println( "StrTree: NOT checking "
				+ getElementName()
				);
			setIsASpanOp( false );
		}

		// Set some flags
		initSettings();

		// we'll just let setupChildren's exception pass up the food chain
		setupChildren();

		if(debug) System.err.println( "Debug: new BoolTree tag constructed." );
	}


	void setupChildren()
		throws OpTreeException
	{

		if(debug) System.err.println( "StrTree:SetupChildren: Start." );

		if(debug)
		{
			System.err.println(
			"Debug: BaseTag: setupChildren() for '" +
			getElementName() + "'." );
			System.err.println(
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

			StrTreeInterface newChild = null;

			try
			{
				if( getIsASpanOp( elemName ) )
				{
					newChild = new StrTreeEvaluator( jdomElement );
				}
				else if( LeafStrTreeEvaluator.getIsAStrLeafOp( elemName ) )
				{
					newChild = new LeafStrTreeEvaluator( jdomElement );
				}
				// Else it's cruft, just ignore it
				// We can easily have cruft left over from the xpump
				// source and the compiler, so don't complain
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
//			else
//				throw new OpTreeException(
//					"Got back a null from constructing child op tag:"
//					+ " element: '" + elemName + "'"
//					);

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

		if(debug) System.err.println( "StrTree:SetupChildren: End." );

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
	public String evaluate( WorkUnit inWU )
		throws OpTreeException
	{
		if(debug) System.err.println( "StrTree:evaluate: Start." );

		// We don't really need the work unit, but many of the
		// leaf node operators do, so we need it to pass down to them

		// Run my setup processing
		if(debug) System.err.println( "StrTree:evaluate: running pre" );
		runMyselfBeforeChildren();

		// Process my children
		if(debug) System.err.println( "StrTree:evaluate: running children" );
		runTagChildren( inWU );

		// run the cleanup
		// It returns back the final tally
		if(debug) System.err.println( "StrTree:evaluate: running post" );
		String answer = runMyselfAfterChildren();

		if(debug) System.err.println( "StrTree:evaluate: done"
			+ " answer=\"" + answer + "\""
			);

		return answer;

	}

	// Initialize some state variables
	void runMyselfBeforeChildren()
	{
		fStringBuffer = new StringBuffer();
	}


	// This function will be run after the children are
	// ==============================================================
	String runMyselfAfterChildren()
		throws OpTreeException
	{
		if(debug) System.err.println( "StrTree:runMyselfAfterChildren: start" );

		if( fStringBuffer == null )
			throw new OpTreeException(
				"StrTree: runMyselfAfterChildren: buffer was null for element " +
				getElementName()
				);

		if( fStringBuffer.length() < 1 )
			System.err.println( "StrTree: runMyselfAfterChildren:"
				+ " Zero length result from children for element "
				+ getElementName()
				);

		String tagName = getElementName().trim().toLowerCase();

		int repeatCount = 1;
		String joinText = null;
		if( tagName.equals( REPEAT_TAG_NAME ) )
		{
			repeatCount = getRepeatCount();
			joinText = getJoinText();
		}

		if( repeatCount < 1 )
		{
			System.err.println( "Warning: StrTreeEval:"
				+ " Repeat count was less than 1 for element "
				+ getElementName()
				+ ", returning an empty string."
				);
			return "";
		}
		String tmpStr = new String( fStringBuffer );
		// This is usually where we bail
		if( repeatCount == 1 )
		{
			if(debug) System.err.println( "StrTree:runMyselfAfterChildren: end2" );
			return tmpStr;
		}

		// They want more than one copy
		StringBuffer buff = new StringBuffer();
		// Give them the number of copies they asked for
		for( int i = 1; i <= repeatCount; i++ )
		{
			buff.append( tmpStr );
			// If tehy asked for join text and we're not on the last
			// element, add that as well
			if( joinText != null && i < repeatCount )
				buff.append( joinText );
		}

		if(debug) System.err.println( "StrTree:evaluate: End3." );

		return new String( buff );
	}


	// The main loop for processing the children
	// Adds text to fStringBuffer
	// ------------------------------------------
	void runTagChildren( WorkUnit inWU )
		throws OpTreeException
	{

		if(debug) System.err.println( "StrTree:runTagChildren: Start." );

		if(debug)
		{
			System.err.println(
				"Debug: BoolTag: runTagChildren() for '" +
				getElementName() + "'."
				);
			System.err.println( "\tAbout to run " +
				getNumTagChildren() + " childen."
				);
		}

		if( getNumTagChildren() < 1 )
		{
			throw new OpTreeException(
				"Error: spanning tree tag '" + getElementName() +
				"' must have at least one child."
				);
			// just return true, the post processing will figure it out
			// return false;
		}

		// The main loop, will run each child
		// indirectly via a casting caller
		for( int i=0; i<getNumTagChildren(); i++ )
		{

			StrTreeInterface child = getTagChildByOffset( i );

			String childResult = child.evaluate( inWU );

			if( childResult != null )
				fStringBuffer.append( childResult );
			else
				System.err.println( "Warning:StrTreeEval:runTagChildren:"
					+ " Got a null return string"
					);
		}

		if(debug) System.err.println( "StrTree:runTagChildren: End." );

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
		throws OpTreeException
	{
		if(debug) System.err.println( "StrTree:initSettings: Start." );

		fHaveDoneInit = false;
		fRepeatCount = getRepeatCount();
		fJoinText = getJoinText();
		fIsASpanOp = getIsASpanOp();
		fHaveDoneInit = true;

		if(debug) System.err.println( "StrTree:initSettings: End." );
	}

	public void setIsASpanOp( boolean flag )
	{
		if(debug) System.err.println( "StrTree:setIsASpanOp: flag=" + flag );
		fForcedIsASpanOp = true;
		fIsASpanOp = flag;
	}

	boolean getIsASpanOp()
		throws OpTreeException
	{
		if(debug) System.err.println( "StrTree:getIsASpanOp: Start." );
		if( fHaveDoneInit )
		{
			if(debug) System.err.println( "\thaveDoneInit so returning "
				+ " fIsASpanOp=" + fIsASpanOp
				);
			return fIsASpanOp;
		}
		if( fForcedIsASpanOp )
		{
			if(debug) System.err.println( "\tfIsASpanOp forced"
				+ " fIsASpanOp=" + fIsASpanOp
				);
			return fIsASpanOp;
		}
		if(debug) System.err.println( "\tcalling static method" );
		return getIsASpanOp( getElementName() );
	}

	public static boolean getIsASpanOp( String inName )
		throws OpTreeException
	{
		if( inName == null || inName.trim().equals("") )
			throw new OpTreeException(
				"isASpanOp was passed null/empty opname/string to check."
				);
		inName = inName.trim().toLowerCase();
		if(debug) System.err.println( "StrTree:static getIsASpanOp:"
				+ "inName=" + inName
				);
		if( inName.equals( GROUP_TAG_NAME ) ||
			inName.equals( REPEAT_TAG_NAME )
			)
		{
			if(debug) System.err.println( "tFound match so TRUE" );
			return true;
		}
		else
		{
			if(debug) System.err.println( "tNo match so FALSE" );
			return false;
		}
	}

	List getChildTagList() {
		return fChildTagList;
	}

	int getNumTagChildren()
	{
		return getChildTagList().size();
	}

	void addChildTag( StrTreeInterface newTag )
	{
		getChildTagList().add( newTag );
	}

	StrTreeInterface getTagChildByOffset( int offset )
	{
		return (StrTreeInterface)getChildTagList().get( offset );
	}

	private String getJoinText()
	{
		// Returned the cached value?
		if( fHaveDoneInit )
			return fJoinText;
		// Look up the attribute
		String tmpStr = getStringFromAttribute( JOINER_FIXED_VALUE_ATTR );
		// Lookup secndary name
		if( tmpStr == null )
			tmpStr = getStringFromAttribute( JOINER_FIXED_VALUE_ATTR_2 );

		if( tmpStr != null && tmpStr.equals("") )
			tmpStr = null;

		// Return whatever we got
		return tmpStr;

	}

	private int getRepeatCount()
	{
		// Returned the cached value?
		if( fHaveDoneInit )
			return fRepeatCount;
		// Look up the attribute
		int tmpInt = getIntFromAttribute( REPEAT_COUNT_ATTR, -1 );
		// Lookup secndary name
		if( tmpInt < 0 )
			tmpInt = getIntFromAttribute( REPEAT_COUNT_ATTR_2, -1 );
		if( tmpInt < 0 )
			tmpInt = getIntFromAttribute( REPEAT_COUNT_ATTR_3, -1 );

		if( tmpInt < 0 )
			tmpInt = 1;

		// Return whatever we got
		return tmpInt;

	}


	private void __sep__Constants_and_Fields__ () {}


	private static final String GROUP_TAG_NAME = "concatenate";
	private static final String REPEAT_TAG_NAME = "repeat";
	private static final String REPEAT_COUNT_ATTR = "repeat_count";
	private static final String REPEAT_COUNT_ATTR_2 = "count";
	private static final String REPEAT_COUNT_ATTR_3 = "times";
	private static final String JOINER_FIXED_VALUE_ATTR = "join_text";
	private static final String JOINER_FIXED_VALUE_ATTR_2 = "join_with";

	// The list of instantiated Op children
	private List fChildTagList;

	// Names of the operators
	// WARNING: If you add to this list you should probably also
	// update getIsBoolOp( String )
	//private static final String REPEAT_TAG_NAME = "repeat";

	// The buffer we're building
	StringBuffer fStringBuffer;

	// Some handy state variables
	//boolean fHaveSeenAnyChildren;
	//boolean fDidAllChildrenMatch;
	//boolean fDidAnyChildrenMatch;

	// Cache some the answers to frequent questions
	private boolean fHaveDoneInit;
	private boolean fIsASpanOp;
	private boolean fForcedIsASpanOp;
	private int fRepeatCount;
	private String fJoinText;
	//boolean fIsTheNotOp;
	//boolean fIsAndOp;
	//boolean fIsOrOp;

}
