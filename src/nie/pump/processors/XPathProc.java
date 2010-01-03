//package nie.processors;
package nie.pump.processors;

//import java.io.*;
import java.util.*;

// import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;
// import org.jdom.CDATA;
// import org.jdom.Text;
// import org.jdom.Attribute;
// import org.jdom.input.SAXBuilder;
// import org.jdom.output.XMLOutputter;
import org.jdom.JDOMException;
import org.jdom.xpath.*;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

public class XPathProc extends Processor
{
	private static final String kStaticClassName = "XPathProc";
	public String kClassName() { return kStaticClassName; }

	// public static final boolean debug = false;

	private void __sep__Constructors__() {};
	////////////////////////////////////////////////////////////
	public XPathProc( Application inApplication,
		Queue[] inReadQueue,
		Queue[] inWriteQueue,
		Queue[] inUsesQueue,
		Element inParameter,
		String inID )
	{
		super( inApplication, inReadQueue, inWriteQueue, inUsesQueue, inParameter, inID );
		final String kFName = "constructor";
		if( (inReadQueue == null) || (inReadQueue[0] == null) )
		{
			fatalErrorMsg( kFName,
				"No input queue specified for CiscoContentExtractor." );
			System.exit( -1 );
		}

		fReadQueue = inReadQueue[0];

		if( (inWriteQueue == null) || (inWriteQueue[0] == null) )
		{
			fatalErrorMsg( kFName,
				"No output queue specified for CiscoContentExtractor." );
			System.exit( -1 );
		}

		fWriteQueue = inWriteQueue[0];

		// If parameters were sent in, save them
		if( inParameter != null )
		{
			//System.err.println( "inParameters = " + inParameter );
			try
			{
				fJdh = new JDOMHelper( inParameter );
			}
			catch (Exception e)
			{
				fJdh = null;
				stackTrace( kFName, e,
						"Error creating jdom helper for parameter"
						);
				fatalErrorMsg( kFName,
					"Exiting due to exception: " + e
					);
				System.exit(1);
			}
		}
		else
		{
			fatalErrorMsg( kFName,
				"inParameter == null." );
			System.exit(1);
		}

		int howMany = acquireXPathElements();
		if( howMany < 1 )
		{
			fatalErrorMsg( kFName,
				"must have at least 1 xpath tag." );
			System.exit(1);
		}

		// Sanity checks on the tags
		// they will halt the system if they don't like what they find
		checkTags();

		// setup the patterns
		try
		{
			setupPaths();
		}
		catch (Exception e)
		{
			stackTrace( kFName, e,
				"Exception during setupPaths" );
			fatalErrorMsg( kFName,
				"Exiting due to exception: " + e );
			System.exit(1);
		}
	}


	private void __sep__Setup_and_Simple_Logic__() {}
	///////////////////////////////////////////////////////////////////////

	// Acquire and store the regex tag
	// Todo: convert to supporting N tags
	int acquireXPathElements()
	{
		final String kFName = "acquireXPathElements";
		List tmpTags = fJdh.findElementsByPath( XPATH_TAG_NAME );
		if( tmpTags == null || tmpTags.size() < 1 )
			return 0;

		int retCount = 0;

		fXPathTags = new JDOMHelper [ tmpTags.size() ];

		for( int i=0; i<tmpTags.size(); i++ )
		{
			Element tmpElem = (Element)tmpTags.get(i);
			try
			{
				fXPathTags[i] = new JDOMHelper( tmpElem );
				retCount++;
			}
			catch(Exception e)
			{
				stackTrace( kFName, e,
					"Error creating jdom helper from xpath element"
					);
				fatalErrorMsg( kFName, "Exiting due to exception: " + e );
				fXPathTags[i] = null;
				System.exit(1);
			}
		}
		return retCount;
	}




	void checkTags()
	{
		for( int i=0; i<fXPathTags.length; i++ )
		{
			getSourceFieldName( i );
			getDestinationFieldName( i );
			getXPathExpression( i );
		}
	}


	/*************************************************
	*
	*	Simple Get/Set Logic
	*
	*************************************************/
	private void __sep__Simple_Get_and_Set__() {};
	////////////////////////////////////////////////////////////

	// see also calculateCompilerOptionsMask() below

	String getXPathExpression( int i )
	{
		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag == null )
			return null;

		// Call the JDOM accessor
		return lRegexTag.getTextTrim();
	}


	String getSourceFieldName( int i )
	{
		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag == null )
			return null;
		String tmpString = lRegexTag.getStringFromAttributeTrimOrNull(
			SOURCE_FIELD_ATTR_NAME
			);
		if( null == tmpString )
			tmpString = lRegexTag.getStringFromAttributeTrimOrNull(
				SOURCE_FIELD_ATTR_SHORT_NAME
				);

		/***
		if( null == tmpString )
		{
			System.err.println( "Error: source_field is a required attribute for the xpath tag" );
			System.exit(1);
		}
		***/
		return tmpString;
	}

	String getDestinationFieldName( int i )
	{
		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag == null )
			return null;
		String tmpString = lRegexTag.getStringFromAttributeTrimOrNull(
			DESTINATION_FIELD_ATTR_NAME
			);
		if( null == tmpString )
		{
			tmpString = lRegexTag.getStringFromAttributeTrimOrNull(
				DESTINATION_FIELD_ATTR_SHORT_NAME
				);
		}
		/***
		if( null == tmpString )
		{
			System.err.println( "Error: destination_field is a required attribute for the xpath tag" );
			System.exit(1);
		}
		***/
		return tmpString;
	}

	/***
	String getSourceText( WorkUnit inWU )
	{
		String srcFieldName = getSourceFieldName();
		return inWU.getUserFieldTextSafeTrim( srcFieldName );
	}
	***/


/* ###################### new stuff ################## */

	// Regex tag attribute: Set the destination to a fixed value
	//		on a match, vs using matchtext
	//      Can return null
	private String getConstMatchTag( int i )
	{
		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag == null )
			return null;
		String tmpString = lRegexTag.getStringFromAttribute(
			USE_CONST_MATCH_TAG_ATTR_NAME
			);
		// we won't accept just ""
		if( tmpString != null && tmpString.equals("") )
			tmpString = null;
		return tmpString;
	}

	// Regex tag attribute: default/nomatch value
	//      Can return null
	private String getConstDefaultNoMatchTag( int i )
	{
		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag == null )
			return null;
		String tmpString = lRegexTag.getStringFromAttribute(
			USE_DEFAULT_NOMATCH_TAG_ATTR_NAME
			);
		// We won't accept just ""
		if( tmpString != null && tmpString.equals("") )
			tmpString = null;
		return tmpString;
	}

	// Regex tag attribute: Which one to keep:
	//	Values: first(default) | last | all|* | (+/- number)
	// Positive integer: nth from start of list, base 1, 1 = first
	// Negative integer: nth from back of list, -1 = last
	// Integer zero(0): ALL matches
	// ========================================================
	private int getKeepWhichMatch( int i )
	{
		JDOMHelper lRegexTag = fXPathTags[i];

		// For all the "null" cases we return the default
		if( null == lRegexTag )
			return DEFAULT_KEEP_WHICH;
		String tmpString = lRegexTag.getStringFromAttributeTrimOrNull(
			KEEP_WHICH_ATTR_NAME
			);
		if( null == tmpString )
			tmpString = lRegexTag.getStringFromAttributeTrimOrNull(
				KEEP_WHICH_ATTR_SHORT_NAME
				);
		if( null == tmpString )
			tmpString = lRegexTag.getStringFromAttributeTrimOrNull(
				KEEP_WHICH_ATTR_SHORT2_NAME
				);
		if( null == tmpString )
			tmpString = lRegexTag.getStringFromAttributeTrimOrNull(
				KEEP_WHICH_ATTR_SHORT3_NAME
				);

		if( null == tmpString )
			return DEFAULT_KEEP_WHICH;
		tmpString = tmpString.trim().toLowerCase();
		if( tmpString.equals("") )
			return DEFAULT_KEEP_WHICH;

		// first = 1
		if( tmpString.equals("first") )
			return 1;
		// last = -1
		if( tmpString.equals("last") )
			return -1;
		// all amd * = 0
		if( tmpString.equals("all") || tmpString.equals("*") )
			return 0;

		// All special cases have been handled
		// Now it's time to convert the string
		// to an integer

		int retValue = DEFAULT_KEEP_WHICH;
		try
		{
			retValue = Integer.parseInt( tmpString );
		}
		catch(Exception e)
		{
			retValue = DEFAULT_KEEP_WHICH;
		}

		return retValue;

	}

	// Should we delete the source field when a REGEX has
	// matched
	boolean getDoDeleteSourceOnSuccess( int i )
	{
		final String kFName = "getDoDeleteSourceOnSuccess";
		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag == null )
			return DEFAULT_DEL_SRC_ON_SUCECSS;
		boolean tmpAns = lRegexTag.getBooleanFromAttribute(
			DEL_SRC_ON_SUCCESS_ATTR_NAME,
			DEFAULT_DEL_SRC_ON_SUCECSS
			);
		String tmpField = getSourceFieldName( i );
		if( tmpAns && null==tmpField ) {
			mWorkUnit.errorMsg( this, kFName,
				"can't remove source field on success because no source field given" );
			tmpAns = false;
		}
		return tmpAns;
	}


	// Should we delete the source field when an XPath has
	// not matched
	// Note that if there's an error somewhere and the regex
	// fails due to that, the source may not get set even if this
	// is enabled.  So if it's not deleteing the source, check
	// that there aren't other errors like a bad field name happening.
	boolean getDoDeleteSourceOnNoMatch( int i )
	{
		final String kFName = "getDoDeleteSourceOnNoMatch";
		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag == null )
			return DEFAULT_DEL_SRC_ON_NO_MATCH;
		boolean tmpAns = lRegexTag.getBooleanFromAttribute(
			DEL_SRC_ON_NO_MATCH_ATTR_NAME,
			DEFAULT_DEL_SRC_ON_NO_MATCH
			);
		String tmpField = getSourceFieldName( i );
		if( tmpAns && null==tmpField ) {
			mWorkUnit.errorMsg( this, kFName,
				"can't remove source field on no_match because no source field given" );
			tmpAns = false;
		}
		return tmpAns;
	}


	boolean getDoMoveMatches( int i )
	{
		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag == null )
			return DEFAULT_MOVE_MATCHES;
		return lRegexTag.getBooleanFromAttribute(
			MOVE_MATCHES_ATTR_NAME,
			DEFAULT_MOVE_MATCHES
			);
	}




	private boolean getIsJustOneMatch( int i )
	{
		if( getKeepWhichMatch(i) == 1 )
			return true;
		else
			return false;
	}

	// Regex tag attribute: How to store multiple matches
	//		Valid values: multiple(default) or concatenate
	// This is a little odd:
	// store_multiple_as="multiple" = TRUE
	// store_multiple_as="concatenate" = FALSE
	// MAKE SURE THAT DEFAULT_STORE_MULTI_AS equals
	// "multiple" or "concatenate"
	// If you screw that up we will return true
	// Any other value will current return the default
	private boolean getStoreAsSeparateFields( int i )
	{
		String tmpFlag = DEFAULT_STORE_MULTI_AS;

		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag != null )
		{
			tmpFlag = lRegexTag.getStringFromAttribute(
				STORE_MULTI_AS_ATTR_NAME
				);
			if( tmpFlag == null )
			{
				tmpFlag = DEFAULT_STORE_MULTI_AS;
			}
			else
			{
				tmpFlag = tmpFlag.trim().toLowerCase();
				if( tmpFlag.equals("") )
				{
					tmpFlag = DEFAULT_STORE_MULTI_AS;
				}
				// We have something, make sure it's a value
				// we can handle
				else
				{
					if( ! tmpFlag.equals("multiple") &&
						! tmpFlag.equals("concatenate")
						)
					{
						tmpFlag = DEFAULT_STORE_MULTI_AS;
					}
				}
			}
		}

		// OK by now we have a tmpFlag set to one
		// of two normalized strings
		// Decode to true/false

		if( tmpFlag.equals("multiple") )
			return true;
		else if( tmpFlag.equals("concatenate") )
			return false;
		else    // This should never happen
			return true;

	}


	// Look for older tags with the same dest field
	// So basically when we're looking for part_num's, we
	// will count in our logic (in terms of dup/dedup)
	// any old ones that were already in the work unit user data
	private List acquireExistingDestinationFields( int i, WorkUnit inWorkUnit )
	{
		String destFieldName = getDestinationFieldName(i);
		JDOMHelper lRegexTag = fXPathTags[i];
		if( lRegexTag == null )
			return new Vector();

		// Get the nice clean list of existing values, if any
		List outList = inWorkUnit.getUserFieldsTextNotNullSafeTrim(
			destFieldName
			);

		return outList;

	}


	// some other ideas
	// get existing tags
	// do we need to keep a list
	// for "first" when we have 5 other tags...



	private void __sep__Central_Logic__() {};
	////////////////////////////////////////////////////////////
	public void run()
	{

		try
		{
			while( true )
			{
			    mWorkUnit = null;
				// WorkUnit lWorkUnit = dequeue( fReadQueue );
				mWorkUnit = dequeue( fReadQueue );
				// cWorkUnit = lWorkUnit;
				runXPaths( mWorkUnit );

				enqueue( fWriteQueue, mWorkUnit );
				// cWorkUnit = lWorkUnit = null;
				mWorkUnit = null;
			}
		}
		catch( InterruptedException ie )
		{
		}
	}

	// For each XPath (usually only one) run the matches
	void runXPaths( WorkUnit inWorkUnit )
	{
		final String kFName = "runXPaths";

		// For each pattern
		for( int i=0; i<fCompiledPaths.length; i++ )
		{

			// Step 1: Get the pattern and check matches


			// Get the pattern and source text
			XPath lPath = fCompiledPaths[i];
			if( null==lPath ) {
				mWorkUnit.errorMsg( this, kFName,
					"null compiled path, skipping" );
				continue;
			}
			// Get the name of the field, will halt if invalid name
			String lSourceFieldName = getSourceFieldName(i);
			Element lSourceElem = null;
			// If they gave a name, start with that path
			if( null!=lSourceFieldName ) {
				// Get the Element
				lSourceElem = inWorkUnit.getUserField(
					lSourceFieldName
					);
				if( null == lSourceElem ) {
					mWorkUnit.errorMsg( this, kFName,
						"No source element found" );
					return;
				}
			}
			// Else just start with "root"
			else {
				lSourceElem = inWorkUnit.getUserData();
			}

			// States
			// 0: nodes copied over, source removed
			// 1: nodes copied over, source left
			// 2: nodes moved over, source removed
			// 3: nodes moved over, source left
			// 4: fixed nodes for match, copy mode, source removed
			// 5: fixed nodes for match, copy mode, source left
			// 6: fixed nodes for match, move mode, source removed
			// 7: fixed nodes for match, move mode, source left
			// #	fixed match	copy0/move1	keep source	=mv	CLONE	Unlink?	Detach
			// 0:	0				0				0				1		0		1			1
			// 1:	0				0				1				0		1		1			0
			// 2:	0				1				0				1		0		1?			1
			// 3:	0				1				1				1		0		1			1
			// 4:	1				0				0				0		0		x			0
			// 5:	1				0				1				0		0		1			0
			// 6:	1				1				0				0		0		x			0
			// 7:	1				1				1				0		0		1			1

			int doFixedText = null != getConstMatchTag(i) ? 1 : 0 ;
			int doMoveMatches = getDoMoveMatches( i ) ? 1 : 0 ;
			int doKeepSource = ! getDoDeleteSourceOnSuccess(i) ? 1 : 0 ;

			int state = doFixedText * 4 + doMoveMatches * 2 + doKeepSource;

			boolean metaDetach = state==0 || state==2 || state==3 || state==7;
			boolean metaClone = state==1;
			boolean metaMv = state==0 || state==2 || state==3;


			// What is our target for matches?
			int whichOffset = getKeepWhichMatch(i);

			// Remember how many we've currently matched
			int numWeHaveAdded = 0;

			// Remember whether we've had a match or not
			// Slightly different then numWeHaveAdded >0 because
			// we can add a default value on no match sometimes,
			// so track this flag separately
			boolean haveWeHadAMatch = false;

			// Run the XPath search
			List matches = null;
			// System.err.println( "lPath="+lPath + ", lSourceElem=" + lSourceElem );
			try {
				matches = lPath.selectNodes( lSourceElem );
			}
			catch( JDOMException e )
			{
				mWorkUnit.stackTrace( this, kFName, e,
					"XPath search error" );
				return;
			}


			// Filter the results over to a copied list
			// Avoid concurrency, and also pull over only valid Elements
			List nodesToAdd = new Vector();
			// For each match
			if( null!=matches ) {
				for( Iterator it = matches.iterator(); it.hasNext(); ) {
					Object nodeObj = it.next();

					// System.out.println( "Match type is " + nodeObj.getClass().getName() );

					// We only want Elements
					if( ! (nodeObj instanceof Element)
						&& ! (nodeObj instanceof Attribute)
					)
						continue;

					// Cast and add
					Element nodeElem = null;
					if( nodeObj instanceof Element ) {
						nodeElem = (Element) nodeObj;
					}
					else if( nodeObj instanceof Attribute ) {
						Attribute nodeAttr = (Attribute) nodeObj;
						String name = nodeAttr.getName();
						String value = nodeAttr.getValue();
						nodeElem = new Element( name );
						nodeElem.addContent( value );
					}

					// Trust that the meta settings told us what to do
					if( metaDetach )
						nodeElem.detach();

					// Get a value for the list of stuff to add
					// Move it over
					if( metaMv ) {
						nodesToAdd.add( nodeElem );
					}
					// Or copy it
					else if( metaClone ) {
						Element newElem = (Element) nodeElem.clone();
						nodesToAdd.add( newElem );
					}
					// Or do a fixed value
					else if( doFixedText != 0 ) {
						Element newElem = tagTextToXML( getConstMatchTag(i) );
						if( null==newElem )
							newElem = new Element( "error" );
						nodesToAdd.add( newElem );
					}

					haveWeHadAMatch = true;
					// Remember how many we've currently matched
					numWeHaveAdded++;
					// Early escape if we don't need any more
					if( whichOffset>0 && numWeHaveAdded>=whichOffset )
						break;
				}
			}
			// we might add a fixed text node if nothing has matched
			if( ! haveWeHadAMatch ) {
				String optionalFixedText = getConstDefaultNoMatchTag(i);
				// Did we have any default text?
				if( null != optionalFixedText ) {
					Element newElem = tagTextToXML( optionalFixedText );
					nodesToAdd.add( newElem );
					// Remember how many we've currently matched
					numWeHaveAdded++;
				}
			}



			// Step 2: actually save any desired matches (or no-match)
			// into the work unit



			// The grand total
			int finalSize = nodesToAdd.size();
			if( finalSize < 1 )
				return;

			// Get the destination field name and the matching text
			// We'll need this below
			String lDestFieldName = getDestinationFieldName(i);
			Element newField = null;
			if( null!=lDestFieldName ) {
				newField = new Element( lDestFieldName );
				inWorkUnit.addUserDataElement( newField );
			}

			// Add all, or just some?
			if( whichOffset == 0 ) {
				for( Iterator it = nodesToAdd.iterator() ; it.hasNext() ; ) {
					Element newElem = (Element) it.next();
					if( null!=newField )
						newField.addContent( newElem );
					else
						inWorkUnit.addUserDataElement( newElem );				
				}
			}
			// Only wanted one specific node
			else {
				int listOffset = -1;
				// Positive 1-based offset
				if( whichOffset>0 ) {
					if( whichOffset>finalSize ) {
						mWorkUnit.errorMsg( this, kFName,
						"offset " + whichOffset + " greater than list len " + finalSize );
						return;
					}
					listOffset = whichOffset-1;
				}
				// Else it's less than zero, so start at back of list
				// Negative offset
				else {
					int whichFromBack = whichOffset * -1;
					if( whichFromBack>finalSize ) {
						mWorkUnit.errorMsg( this, kFName,
							"negative offset "
							+ whichOffset + " greater than list len " + finalSize );
						return;
					}
					listOffset = finalSize - whichFromBack;
					// actually +1 for "from back" and then -1 for real zero offset cancels
				}
				// Grab this single node
				Element newElem = (Element) nodesToAdd.get( listOffset );
				// And add it
				if( null!=newField )
					newField.addContent( newElem );
				else
					inWorkUnit.addUserDataElement( newElem );				
			}


			// Potential deletion of source node
			// TODO: not if it's the root!
			boolean removeSource = ( haveWeHadAMatch && getDoDeleteSourceOnSuccess(i) )
				|| ( ! haveWeHadAMatch && getDoDeleteSourceOnNoMatch(i) )
				;
			if( removeSource ) {
				// inWorkUnit.deleteUserDataFieldMulti( lSourceFieldName );
				lSourceElem.detach();
			}

		} //  End for each path
	}



	/*static*/ Element tagTextToXML( String inText ) {
		final String kFName = "tagTextToXML";
		if( null==inText ) {
			mWorkUnit.errorMsg( this, kFName, "null tag text passed in" );
			return null;
		}

		Element outElem = null;
		// Did they give us rich XML text?
		try {
			if( inText.indexOf('<') >= 0 ) {
				JDOMHelper tmp = new JDOMHelper( inText, null );
				outElem = tmp.getJdomElement();
			}
			// Or assume it's just a simple tag name
			else {
				outElem = new Element( inText );
			}
		}
		catch( Exception e ) {
			mWorkUnit.stackTrace( this, kFName, e,
				"unable to convert tag text into XML"
				+ " text=\"" + inText + "\""
				);
			outElem = null;
		}
		return outElem;
	}



	// Get a hash back from a list
	private Map hashFromList( List inList )
	{

		// Todo: someday make this comparison case
		// specific per tag, which would mean we'd need i

		Hashtable outHash = new Hashtable();
		if( inList == null || inList.size() < 1 )
			return outHash;
		Iterator it = inList.iterator();
		while( it.hasNext() )
		{
			String newContent = (String)it.next();
			// Todo: see if need to normalize

			if( newContent.equals("") )
				continue;
			if( ! outHash.containsKey( newContent ) )
				outHash.put( newContent, newContent );
		}
		return outHash;
	}

	/*************************************************
	*
	*	Dealing with Regular Expressions
	*
	*************************************************/
	private void __sep__Regex_Methods__() {};
	////////////////////////////////////////////////////////////

	void setupPaths()
		//throws Exception
	{
		final String kFName = "setupPaths";
		// Initial the two other arrays we need
		fCompiledPaths = new XPath [ fXPathTags.length ];

		// Once for each xpath tag
		for( int i=0; i<fXPathTags.length; i++ )
		{
			String xText = getXPathExpression( i );
			xText = NIEUtil.trimmedStringOrNull( xText );
			if( null == xText )
			{
				//throw new Exception( "Error: RegexTag:init: null regular expression text." );
				fatalErrorMsg( kFName,
					"null xpath expression text." );
				// continue;
				System.exit(1);
			}

//			if(debug)
//			{
//				System.out.println( "=========== regex text ==============" );
//				System.out.println( xText );
//				System.out.println( "=====================================" );
//			}


			// Compile the regular expression
			// one per regex
 			try
			{
				fCompiledPaths[i] = XPath.newInstance( xText );
			}
			catch( JDOMException e )
			{
				stackTrace( kFName, e,
					"XPath compile error" );
				fCompiledPaths[i] = null;
				fatalErrorMsg( kFName,
					"Exiting becasue could not compile regular expression, exception"
				     + e );
				System.exit(1);
			}

		}   // End for each regex tag

	}


	private void __sep__Constants__() {};
	//////////////////////////////////////////////////////
	// Defaults for the regex attributes
	// See below for doc in the attr name section
	private static final boolean DEFAULT_DEL_SRC_ON_SUCECSS = false;
	private static final boolean DEFAULT_DEL_SRC_ON_NO_MATCH = false;
	private static final boolean DEFAULT_MOVE_MATCHES = false;
	private static final int DEFAULT_KEEP_WHICH = 1;

	// Set to "multiple" or "concatenate"
	// MUST match a value understood by getStoreAsSeparateFields()
	private static final String DEFAULT_STORE_MULTI_AS = "multiple";

	// Some constant strings
	private static final String XPATH_TAG_NAME =
		"xpath";
	// Todo: implement and explain later
	// "global" setting for all regexes, no sense in
	// trying to mix
	private static final String FORCE_TREE_TAG_NAME =
		"force_freeform_fields";
	// Regex tag attribute: What field to look in
	private static final String SOURCE_FIELD_ATTR_NAME =
		"source_field";
	private static final String SOURCE_FIELD_ATTR_SHORT_NAME =
		"src";
	// Regex tag attribute: What feild to put any matches in
	private static final String DESTINATION_FIELD_ATTR_NAME =
		"destination_field";
	private static final String DESTINATION_FIELD_ATTR_SHORT_NAME =
		"dst";
	// Regex tag attribute: whether to delete the source field
	//		after a good match
	private static final String DEL_SRC_ON_SUCCESS_ATTR_NAME =
		"delete_source_on_success";
	private static final String MOVE_MATCHES_ATTR_NAME =
		"move_matches";
	// Regex tag attribute: whether to delete the source field
	//		after no match
	private static final String DEL_SRC_ON_NO_MATCH_ATTR_NAME =
		"delete_source_on_no_match";
	// Regex tag attribute: Set the destination to a fixed value
	//		on a match, vs using matchtext
	private static final String USE_CONST_MATCH_TAG_ATTR_NAME =
			"constant_match_tag";
	// Regex tag attribute: default/nomatch value
	private static final String USE_DEFAULT_NOMATCH_TAG_ATTR_NAME =
			"default_no_match_tag";
	// Regex tag attribute: Which one to keep:
	//		Values: first(default) | last | all|* | (+/- number)
	private static final String KEEP_WHICH_ATTR_NAME =
			"keep_which_match";
	private static final String KEEP_WHICH_ATTR_SHORT_NAME =
			"keep";
	private static final String KEEP_WHICH_ATTR_SHORT2_NAME =
			"keep_which";
	private static final String KEEP_WHICH_ATTR_SHORT3_NAME =
			"which";
	// Regex tag attribute: How to store multiple matches
	//		Valid values: multiple(default) or concatenate
	private static final String STORE_MULTI_AS_ATTR_NAME =
			"store_multiple_as";
	// Regex tag attribute: will we accept duplicates when
	//		doing more than one

	private void __sep__Member_Fields1__() {};
	//////////////////////////////////////////////////////

	// The main JDOM node from the parameters section
	JDOMHelper fJdh;
	JDOMHelper fXPathTags [];

	// The queues
	Queue fReadQueue;
	Queue fWriteQueue;

	// The compiled pattern
	// one per xpath
	XPath fCompiledPaths [];

	WorkUnit _cWorkUnit;
	WorkUnit mWorkUnit;

	// The last result we calculated
	// one per instance
	Element _fLastResults [];

	// The optional mapping between named regex groups
	// and regex group offsets
	//Vector mappingVector;

}
