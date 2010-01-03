//package nie.processors;
package nie.pump.processors;

//import java.io.*;
import java.util.*;

import org.apache.oro.text.regex.*;
import org.jdom.Element;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

// mbennett 3/28/05 get all source fields with matching name

/**
 * Title:	Regex V4
 * Description:
 * Copyright:    Copyright (c) 2001 - 2005
 * Company:
 * @author	Mark and Kevin
 * @version 1.0
 *
 * <regex ...attrs...>your pattern</regex>
 *
 * Required attrs:
 * source_field=
 * destination_field=
 *
 * REMINDER:
 * In an XML (Dpump control) file you are NOT allowed to use
 * unescaped angle brackets in your regexes.  When parsing HTML
 * folks are often tempted to use them.
 * There are two fixes for this:
 * - Use &lt; and &gt;
 * - Enclose your regex text in a CDATA directive: (untested at this time)
 *   <![CDATA[ my html regex with <title> tags, etc ]]>
 *
 * NEW in V3
 * ======================
 * Set a varaible to a fixed value on a match, vs using matchtext
 * Good for setting logic variables
 * "constant_match_text"
 * default/nomatch value
 * "default_no_match_text"
 * Which one to keep:
 * keep_which_match=" first | last | all|* | (+/- number)"
 *		default is "first"
 * How to store multiple matches
 * store_multiple_as = separate or concatenate
 * allow_duplicates=0 1 (default is 0, NO dupes)
 * NOT IMPLEMENTED YET
 * concatenation_separator (=", " by default)
 *
 *
 * Other options:
 * group_number="n"	Which regex match group, default=0 = entire buffer
 *
 * Binary choices:
 * All of the form:
 *      attr_name="0" or attr_name="1"
 * See the constants section at the bottom for update defaults and names
 * Choices:
 * Whether to do case sensitive matching or not
 *      case_sensitive, default is NO, case does not matter by default
 *                      so turn this on if you care about case
 * Whether to allow extended syntax including white space
 *      extended_syntax, default is YES
 *                      If you use this, you must use \s for space
 * Whether to match on multiple lines as a single blob
 *      multiline_match, default is YES
 * Whether to delete the source field after a good match
 *      delete_source_on_success, default is NO
 * Whether to delete the source field after no match
 *      delete_source_on_no_match, default is NO
 *
 * Other PLANNED options:
 * Whether to keep null string matches
 *		keep_null_strings default 0
 * required=0/1  (0 is default, if 1 record marked invalid if not found)
 * continue_matching="1" (if doing more than one)
 * debug?
 * always delete
 * dup check is case sensative
 * trim data before storing
 * cleanup data before storing
 * don't bother storing null strings, kind of have that in v3
 * don't do if already have this field, kind of have that in v3 now
 *
 */


public class RegexV4 extends Processor
{
	public String kClassName() { return "RegexV4"; }


	// public static final boolean debug = false;

	private void __sep__Constructors__() {};
	////////////////////////////////////////////////////////////
	public RegexV4( Application inApplication,
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
			// System.err.println( "No input queue specified for Regex." );
			fatalErrorMsg( kFName, "No input queue specified, id=" + inID );
			System.exit( -1 );
		}

		fReadQueue = inReadQueue[0];

		if( (inWriteQueue == null) || (inWriteQueue[0] == null) )
		{
			// System.err.println( "No output queue specified for Regex." );
			fatalErrorMsg( kFName, "No output queue specified, id=" + inID );
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
				// System.err.println(
				//	"Error creating jdom helper for parameter\n" + e
				//	);
				fatalErrorMsg( kFName,
						"Error creating jdom helper for parameter\n" + e
						);
				System.exit(1);
			}
		}
		else
		{
			// System.err.println( "Error: inParameter == null." );
			fatalErrorMsg( kFName, "Error: inParameter == null." );
			//System.out.println( "see doc" );
			// fatalErrorMsg( kFName, "see doc" );
			System.exit(1);
		}

		int howMany = acquireRegexElements();
		if( howMany < 1 )
		{
			// System.err.println( "Error: must have at least 1 regex tag." );
			fatalErrorMsg( kFName, "Error: must have at least 1 regex tag." );
			// System.err.println( "see doc" );
			// fatalErrorMsg( kFName, "see doc" );
			System.exit(1);
		}

		// Sanity checks on the tags
		// they will halt the system if they don't like what they find
		checkTags();

		// setup the patterns
		try
		{
			setupPatterns();
		}
		catch (Exception e)
		{
			// System.err.println( "Error: inParameter == null." );
			stackTrace( kFName, e, null );
			//System.out.println( "see doc" ;)
			fatalErrorMsg( kFName, "Exiting due to exception: " + e );
			System.exit(1);
		}
	}


	private void __sep__Setup_and_Simple_Logic__() {}
	///////////////////////////////////////////////////////////////////////

	// Acquire and store the regex tag
	// Todo: convert to supporting N tags
	int acquireRegexElements()
	{
		
		final String kFName = "constructor";
		List tmpTags = fJdh.findElementsByPath( REGEX_TAG_NAME );
		if( tmpTags == null || tmpTags.size() < 1 )
			return 0;

		int retCount = 0;

		fRegexTags = new JDOMHelper [ tmpTags.size() ];

		for( int i=0; i<tmpTags.size(); i++ )
		{
			Element tmpElem = (Element)tmpTags.get(i);
			try
			{
				fRegexTags[i] = new JDOMHelper( tmpElem );
				retCount++;
			}
			catch(Exception e)
			{
				// System.err.println(
				//	"Error creating jdom helper from regex element" +
				//	"\n" + e
				//	);
				stackTrace( kFName, e,
						"Error creating jdom helper from regex element"
						);
				fatalErrorMsg( kFName, "Exiting due to exception: " + e );
				// fRegexTags[i] = null;
			}
		}
		return retCount;
	}




	void checkTags()
	{
		for( int i=0; i<fRegexTags.length; i++ )
		{
			getSourceFieldName( i );
			getDestinationFieldName( i );
			getRegexText( i );
		}
	}

	// When we compile a pattern we need to tell it which options
	// we want.  See the above 4 functions and the default
	// constants at the top.
	int calculateCompilerOptionsMask( int i )
	{
		int result = Perl5Compiler.DEFAULT_MASK;
		if( ! getIsCaseSensitive(i) )
			result |= Perl5Compiler.CASE_INSENSITIVE_MASK;
		if( getIsExtendedSyntax(i) )
			result |= Perl5Compiler.EXTENDED_MASK;
		if( getIsMultiline(i) )
			result |= Perl5Compiler.MULTILINE_MASK;
		else
			result |= Perl5Compiler.SINGLELINE_MASK;
		if( getIsThreadSafeCompiler(i) )
			result |= Perl5Compiler.READ_ONLY_MASK;
		return result;
	}


	/*************************************************
	*
	*	Simple Get/Set Logic
	*
	*************************************************/
	private void __sep__Simple_Get_and_Set__() {};
	////////////////////////////////////////////////////////////

	// see also calculateCompilerOptionsMask() below

	boolean getIsCaseSensitive( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return DEFAULT_IS_CASE_SENSITIVE;
		boolean tmpAns = lRegexTag.getBooleanFromAttribute(
			CASE_SENSITIVE_ATTR_NAME,
			DEFAULT_IS_CASE_SENSITIVE
			);
		if( tmpAns == DEFAULT_IS_CASE_SENSITIVE )
			tmpAns = lRegexTag.getBooleanFromAttribute(
				CASE_SENSITIVE_ATTR_SHORT_NAME,
				DEFAULT_IS_CASE_SENSITIVE
				);
		return tmpAns;
	}
	boolean getIsExtendedSyntax( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return DEFAULT_IS_EXTENDED_SYNTAX;
		boolean tmpAns = lRegexTag.getBooleanFromAttribute(
			EXTENDED_SYNTAX_ATTR_NAME,
			DEFAULT_IS_EXTENDED_SYNTAX
			);
		if( tmpAns == DEFAULT_IS_EXTENDED_SYNTAX )
			tmpAns = lRegexTag.getBooleanFromAttribute(
				EXTENDED_SYNTAX_ATTR_SHORT_NAME,
				DEFAULT_IS_EXTENDED_SYNTAX
				);
		return tmpAns;
	}
	boolean getIsMultiline( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return DEFAULT_IS_MULTILINE;
		boolean tmpAns = lRegexTag.getBooleanFromAttribute(
			MULTILINE_ATTR_NAME,
			DEFAULT_IS_MULTILINE
			);
		if( tmpAns == DEFAULT_IS_MULTILINE )
			tmpAns = lRegexTag.getBooleanFromAttribute(
				MULTILINE_ATTR_SHORT_NAME,
				DEFAULT_IS_MULTILINE
				);
		//System.err.println( "Debug: getIsMultiline: returning " + tmpAns );
		return tmpAns;
	}
	boolean getIsThreadSafeCompiler( int i )
	{
		return DEFAULT_IS_THREAD_SAFE_COMPILER;
	}

	String getRegexText( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return null;

		// Call the JDOM accessor
		return lRegexTag.getTextTrim();
	}


	// Should we delete the source field when a REGEX has
	// matched
	boolean getDoDeleteSourceOnSuccess( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return DEFAULT_DEL_SRC_ON_SUCECSS;
		return lRegexTag.getBooleanFromAttribute(
			DEL_SRC_ON_SUCCESS_ATTR_NAME,
			DEFAULT_DEL_SRC_ON_SUCECSS
			);
	}
	// Should we delete the source field when a REGEX has
	// not matched
	// Note that if there's an error somewhere and the regex
	// fails due to that, the source may not get set even if this
	// is enabled.  So if it's not deleteing the source, check
	// that there aren't other errors like a bad field name happening.
	boolean getDoDeleteSourceOnNoMatch( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return DEFAULT_DEL_SRC_ON_NO_MATCH;
		return lRegexTag.getBooleanFromAttribute(
			DEL_SRC_ON_NO_MATCH_ATTR_NAME,
			DEFAULT_DEL_SRC_ON_NO_MATCH
			);
	}

	String getSourceFieldName( int i )
	{
		final String kFName = "constructor";
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return null;
		String tmpString = lRegexTag.getStringFromAttribute(
			SOURCE_FIELD_ATTR_NAME
			);
		if( tmpString == null || tmpString.trim().equals("") )
		{
			tmpString = lRegexTag.getStringFromAttribute(
				SOURCE_FIELD_ATTR_SHORT_NAME
				);
		}
		if( tmpString == null || tmpString.trim().equals("") )
		{
			// System.err.println( "Error: source_field is a required attribute for the regex tag" );
			fatalErrorMsg( kFName, "source_field is a required attribute for the regex tag" );
			System.exit(1);
		}
		return tmpString.trim();
	}

	String getDestinationFieldName( int i )
	{
		final String kFName = "constructor";
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return null;
		String tmpString = lRegexTag.getStringFromAttribute(
			DESTINATION_FIELD_ATTR_NAME
			);
		if( tmpString == null || tmpString.trim().equals("") )
		{
			tmpString = lRegexTag.getStringFromAttribute(
				DESTINATION_FIELD_ATTR_SHORT_NAME
				);
		}
		if( tmpString == null || tmpString.trim().equals("") )
		{
			// System.err.println( "Error: destination_field is a required attribute for the regex tag" );
			fatalErrorMsg( kFName, "destination_field is a required attribute for the regex tag" );
			System.exit(1);
		}
		return tmpString.trim();
	}

	/***
	String getSourceText( WorkUnit inWU )
	{
		String srcFieldName = getSourceFieldName();
		return inWU.getUserFieldTextSafeTrim( srcFieldName );
	}
	***/

	int getMatchGroupNumber( int i )
	{
		final String kFName = "getMatchGroupNumber";
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
		{
			// System.err.println( "Error: Regex: getMatchGroupNumber:"
			//	+ " no regex tag record found for tag # " + i
			//	);
			mWorkUnit.errorMsg( this, kFName,
					"no regex tag record found for tag # " + i
					);
			return 0;
		}
		int tmpAns = lRegexTag.getIntFromAttribute(
			REGEX_GROUP_NUM_ATTR_NAME, -1
			);
		if( tmpAns < 0 )
			tmpAns = lRegexTag.getIntFromAttribute(
				REGEX_GROUP_NUM_ATTR_SHORT_NAME, -1
				);

		if( tmpAns >= 0 )
			return tmpAns;
		else
			return 0;
	}

	// Oddly we like user functions to be 1 based, but
	// as it happens the 0th group tends to be the entire
	// match anyway, so the offset of 1 will typically be
	// the first paren group they specified
	public String getMatchGroupText( int i )
	{
		final String kFName = "constructor";
		if( fLastResults[i] == null ) {
			//System.err.println( "Warning: Regex:getMatchGroupText: " +
			//	" the last result was null?"
			//	);
			return null;
		}

		int offset = getMatchGroupNumber( i );

		if( offset < 0 || offset >= fLastResults[i].groups() )
		{
			// System.err.println(
			//	"Warning: Regex:getMatchGroupByNumber: " +
			//	" requested offset/group-number '" + offset +
			//	"' is out of bounds, must be between 0 and " +
			//	(fLastResults[i].groups()-1) + " inclusive."
			//	);
			mWorkUnit.warningMsg( this, kFName,
					"Warning: Regex:getMatchGroupByNumber: " +
					" requested offset/group-number '" + offset +
					"' is out of bounds, must be between 0 and " +
					(fLastResults[i].groups()-1) + " inclusive."
					);
			return null;
		}

		return fLastResults[i].group( offset );
	}

/* ###################### new stuff ################## */

	// Regex tag attribute: Set the destination to a fixed value
	//		on a match, vs using matchtext
	//      Can return null
	private String getConstMatchText( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return null;
		String tmpString = lRegexTag.getStringFromAttribute(
			USE_CONST_MATCH_TEXT_ATTR_NAME
			);
		// we won't accept just ""
		if( tmpString != null && tmpString.equals("") )
			tmpString = null;
		return tmpString;
	}

	// Regex tag attribute: default/nomatch value
	//      Can return null
	private String getConstDefaultNoMatchText( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return null;
		String tmpString = lRegexTag.getStringFromAttribute(
			USE_DEFAULT_NOMATCH_TEXT_ATTR_NAME
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
		JDOMHelper lRegexTag = fRegexTags[i];

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

		JDOMHelper lRegexTag = fRegexTags[i];
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


	// Regex tag attribute: will we accept duplicates when
	//		doing more than one
	private boolean getAllowMatchDupes( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return DEFAULT_ALLOW_DUPES;
		return lRegexTag.getBooleanFromAttribute(
			ALLOW_DUPES_ATTR_NAME,
			DEFAULT_ALLOW_DUPES
			);
	}

	// Regex tag attribute: If concatenating multiple
	//		matches, what string to put in between
	//		default is ", "
	// In this ruotine an attribute of "" is NOT the same
	// as a null.  If you want no separateor you would
	// say attr="", vs leaving it out to get the default ", "
	private String getConcatenationSeparator( int i )
	{
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return DEFAULT_CONCAT_SEP;
		String tmpString = lRegexTag.getStringFromAttribute(
			CONCAT_SEP_ATTR_NAME
			);
		if( tmpString == null )
			return DEFAULT_CONCAT_SEP;
		else
			return tmpString;
	}

	// Look for older tags with the same dest field
	// So basically when we're looking for part_num's, we
	// will count in our logic (in terms of dup/dedup)
	// any old ones that were already in the work unit user data
	private List acquireExistingDestinationFields( int i, WorkUnit inWorkUnit )
	{
		String destFieldName = getDestinationFieldName(i);
		JDOMHelper lRegexTag = fRegexTags[i];
		if( lRegexTag == null )
			return new Vector();

		// Get the nice clean list of existing values, if any
		List outList = inWorkUnit.getUserFieldsTextNotNullSafeTrim(
			destFieldName
			);

		// Do we need to de-dupe the list?
		if( ! getAllowMatchDupes(i) )
			outList = dedupeList( outList );
			// Todo: someday the de-duping may be case
			// specific for each tag, so it would need to
			// have a reference to i

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

				runRegexes( mWorkUnit );

				enqueue( fWriteQueue, mWorkUnit );
				mWorkUnit = null;
			}
		}
		catch( InterruptedException ie )
		{
		}
	}

	// Run all the regexes in order (usually only one)
	// for each regex, search until we have the number of matches
	// we want
	void runRegexes( WorkUnit inWorkUnit )
	{

		// For each Regex
		for( int i=0; i<fCompiledPatterns.length; i++ )
		{

			final String kFName = "runRegexes";

			// Step 1: Get the pattern and check for an initial match

			// Get the pattern and source text
			Pattern lPattern = fCompiledPatterns[i];
			// Get the name of the field, will halt if invalid name
			String lSourceFieldName = getSourceFieldName(i);
			// Get the text
			// String lSourceText = inWorkUnit.getUserFieldTextSafeTrim(
			//	lSourceFieldName
			//	);
			List sourceTexts = inWorkUnit.getUserFieldsTextNotNullSafeTrim(
				lSourceFieldName
				);

			// System.err.println( "sourceTexts=" + sourceTexts );

			// System.out.println( "source text=" + lSourceText );

			// It can be "", but not null
			if( null == sourceTexts || sourceTexts.isEmpty() )
				return;

			// What is our target for matches?
			int whichOffset = getKeepWhichMatch(i);

			// System.err.println( "whichOffset=" + whichOffset );

			// Do we really need to use a buffer?
			// We care because using the buffer may take more memory
			boolean useBuffer = whichOffset != 1 ? true : false;


			// Get any existing strings
			// This is so that we can not add duplicates, even
			// from different patterns
			List valuesList = acquireExistingDestinationFields(
				i, inWorkUnit
				);

			// Remember the count, how many we had
			// to start with
			int numAlreadyThere = valuesList.size();

			// We'll also be using a hash if not allowing dupes
			Map hashCache = null;
			// If we DON'T allow duplicates then we will
			// have to maintain a cache of what's already
			// there
			if( ! getAllowMatchDupes(i) )
				hashCache = hashFromList( valuesList );

			// Remember how many we've currently matched
			int numWeHaveAdded = 0;

			// Remember whether we've had a match or not
			// Slightly different then numWeHaveAdded >0 because
			// we can add a default value on no match sometimes,
			// so track this flag separately
			boolean haveWeHadAMatch = false;

			// For each source text field
			boolean isDone = false;
			for( Iterator textIt = sourceTexts.iterator() ; textIt.hasNext() ; ) {
				String lSourceText = (String) textIt.next();

				// Get the repeating buffer, if we really need it
				PatternMatcherInput lSourceBuffer = null;
				if( useBuffer )
				{
					// Create a stateful input buffer
					lSourceBuffer = new PatternMatcherInput( lSourceText );
					if( lSourceBuffer == null )
						return;
				}

				// Step 2: evaluate and stockpile matches we may keep

				// Main loop for multipe matches
				// Keep looping through the buffer while
				// we keep finding matches
				while( true )
				{


					// System.err.println( "lSourceText=" + lSourceText );
					// System.err.println( "useBuffer=" + useBuffer );

					// See if the pattern matches
					// If it does the doMatch method will have set fLastResults[i]

					// We have two different versions of the call
					// we can make, depending on whether we use a buffer
					// or not
					boolean tmpDoMatchResult = false;
					// If using the buffer
					if( useBuffer )
					{
						// Call with the buffer
						tmpDoMatchResult = doMatch( i, lSourceBuffer, lPattern );
					}
					// Else not using the buffer
					else
					{
						// Call with just the string
						tmpDoMatchResult = doMatch( i, lSourceText, lPattern );
					}

					// If doMatch matched
					if( tmpDoMatchResult )
					{

						String candidateContent;
						String optionalFixedText = getConstMatchText(i);

						// What is the matched text
						// Normally we want the matched text
						// If the fixed text option is null (usually is)
						// then get the matched text
						if( null == optionalFixedText ) {
							// Grab the actual matched text as the content
							candidateContent = getMatchGroupText(i);
							// Double check that we got something
							if( candidateContent == null )
							{
									// System.err.println( "Error:Regex: got null text"
									// + " for subtag " + i
									// + ", will try another match if using buffer"
									// + ", usingBuffer=" + useBuffer
									// );
								mWorkUnit.errorMsg( this, kFName, "got null text"
										+ " for subtag " + i
										+ ", will try another match if using buffer"
										+ ", usingBuffer=" + useBuffer
										);
								// Free up the memory of the matcher, asap
								fLastResults[i] = null;
								// Try the next itteration
								// We can try again if we're using the buffer
								if( useBuffer )
									continue;
								// Else we'll just have to give up
								else {
									isDone = true;
									break;
								}
							}
							// Todo: maybe do some trimming
							// Todo: complain if it's null, shouldn't be
						}
						// Else we actually want fixed text each time
						else
						{
							// Grab the fixed text as our content
							candidateContent = optionalFixedText;
						}

						// Free up the memory of the last match, asap
						fLastResults[i] = null;

						// From here on we apply the same logic, regardless
						// of where the text came from

						// Now some logic to decide whether or not we want
						// to add the text we've got
						boolean shouldAddThisContent = false;

						// If we don't want dupes, then we'll have to
						// check.

						// If dupes are OK then we will add this one too
						if( getAllowMatchDupes(i) )
						{
							shouldAddThisContent = true;
						}
						// So if we're NOT allowing dupes then that means
						// we must check for dupes
						else
						{

							// Is this value is NOT already in our hash cache
							// Todo: this next section will need updating if
							// we allow for case insensitive dupe checking
							if( ! hashCache.containsKey(candidateContent) )
							{
								// It's not already there so we DO want
								// to add this one
								shouldAddThisContent = true;
								// And we need to now remember that
								// we have seen this value
								hashCache.put( candidateContent,
									candidateContent
									);
								// Note that the content is both the
								// key and value.
								// If anything gets normalized, it would
								// probably be the key version
							}
							// Else it is already in the cache
							else
							{
								// So we don't want this
								// (code is actually redundant, but clear)
								shouldAddThisContent = false;
							}

						}   // End else we needed to check for dupes

						// At this point we've decided for sure
						// whether we want to add this new
						// content

						// If should add it, then do so
						if( shouldAddThisContent )
						{
							// add it
							valuesList.add( candidateContent );

							// Remember how many we've currently matched
							numWeHaveAdded++;

							// Remember that we've had a match that was saved
							// Only saved matches "count" as matches
							// but that makes sense since usually we won't
							// have had other values, and even if we had
							// they would likely not have had the same value
							// and in the rare case where both conditions are
							// true and we don't want dupes then it's silly
							// to not have added it again but still say
							// we had a match, and if they really care then they
							// should turn dupe checking off

							haveWeHadAMatch = true;

						}   // End if we should add it

						// Can we exit early?
						// If we're not using a buffer we certainly
						// can, in fact we can't do a second match!
						if( ! useBuffer ) {
							isDone = true;
							break;
						}

						// If using the buffer there's only even
						// a chance if offset is positive
						if( whichOffset > 0 )
						{
							// have we met it?
							if( numWeHaveAdded >= whichOffset ) {
								isDone = true;
								break;
							}
						}
						// For most other offsets we need to
						// do the whole darn thing until the end

					}   // If we DID have a match
					// Else no match this time through
					// We either had no match or are out of buffer
					else
					{
						// We're done looking at this string
						break;
					}


				}   // End while we have matches

				// Do some cleanup right here to free up
				// some memory, as we've been having some trouble
				lSourceBuffer = null;
				lSourceText = null;

				if( isDone )
					break;

			}	// End for each string


			// Step 3: actually save any desired matches (or no-match)
			// into the work unit



			// If had no match we should fire the nomatch stuff
			if( ! haveWeHadAMatch )
			{
				// We have a couple things to do at this point

				// First, seee if there's default text
				String optionalFixedText = getConstDefaultNoMatchText(i);
				// Did we have any default text?
				if( optionalFixedText != null )
				{
					// Make sure not to skimp on the dedupe rules!

					// Todo: maybe normalize this content too

					// Track our decision
					boolean shouldAddThisContent = false;

					// If dupes are OK then we will add this one too
					if( getAllowMatchDupes(i) )
					{
						shouldAddThisContent = true;
					}
					// So if we're NOT allowing dupes then that means
					// we must check for dupes
					else
					{
						// See similar logic comments above
						if( ! hashCache.containsKey(optionalFixedText) )
						{
							// It's not already there so we DO want
							// to add this one
							shouldAddThisContent = true;
							// We don't really need to remember
							// this since we're about done
							//hashCache.put( optionalFixedText,
							//	optionalFixedText
							//	);
						}
						// Else it is already in the cache
						else
						{
							// So we don't want this
							// (code is actually redundant, but clear)
							shouldAddThisContent = false;
						}

					}   // End else we needed to check for dupes
					// Add if if we want it
					if( shouldAddThisContent )
					{
						// add it
						valuesList.add( optionalFixedText );
						// Remember how many we've currently matched
						numWeHaveAdded++;
					}
				}   // End if default text was not null

				// Also on no match we sometimes want to
				// remove the offending field
				if( getDoDeleteSourceOnNoMatch(i) )
				{
					inWorkUnit.deleteUserDataFieldMulti( lSourceFieldName );
				}
			}
			// Else we DID have a match
			else
			{
				// Sometimes they want to get rid of the source
				// field on success, it's just an option
				if( getDoDeleteSourceOnSuccess(i) )
				{
					inWorkUnit.deleteUserDataFieldMulti( lSourceFieldName );
				}
			}

			// Do some cleanup right here to free up
			// some memory, as we've been having some trouble
			hashCache = null;

			// Now the big loop to process the results and
			// store any matches into the user data area

			// Get the destination field name and the matching text
			// We'll need this below
			String lDestFieldName = getDestinationFieldName(i);

			// Now some sanity checking
			// The total number of elements in the list
			// should be equal to what was there when we started
			// from other fields PLUS the numober of entries
			// we've added.  If not, something is really wrong.

			// The grand total
			int finalSize = valuesList.size();
			// What we expect
			int expectedSize = numAlreadyThere + numWeHaveAdded;
			// A final sanity check!
			if( finalSize != expectedSize )
			{
					// System.err.println(
					// "Error in RegexV4 processor:runRegexes:\n" +
					// "While processing regex # '" + i + "' " +
					// "we had '" + numAlreadyThere +
					// "' existing dest data values in the work unit " +
					// " and added '" + numWeHaveAdded + "' more values;\n" +
					// "the total size should have been '" + expectedSize +
					// "' but the actual final size was '" + finalSize +
					// "', exiting this routine prematurely.\n" +
					// "(note that if dupes were not allowed these counts " +
					// "may differ from the work unit)"
					// );
				mWorkUnit.errorMsg( this, kFName,
						"\n" +
						"While processing regex # '" + i + "' " +
						"we had '" + numAlreadyThere +
						"' existing dest data values in the work unit " +
						" and added '" + numWeHaveAdded + "' more values;\n" +
						"the total size should have been '" + expectedSize +
						"' but the actual final size was '" + finalSize +
						"', exiting this routine prematurely.\n" +
						"(note that if dupes were not allowed these counts " +
						"may differ from the work unit)"
						);
				return;
			}

			// Todo:
			// Before we add, if the total will be more then 1 we
			// need to make sure the data area isn't flat
			//if( finalSize > 1 )
			//  inWorkUnit.forceNotFlat();

			// Now loop through all the values we're supposed to add
			// The start point is actually:
			//  numAlreadyThere+1 then -1 for the 0 based array offset
			//  so the +1-1 cancel out.
			for( int j=numAlreadyThere; j<finalSize; j++ )
			{
				String finalContent = (String)valuesList.get(j);
				if( finalContent == null )
					continue;
					// Todo: complain

				// Add it to the work unit
				inWorkUnit.addNamedField( lDestFieldName, finalContent );
			}

		} //  End for each pattern
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

	// Given a list, return it with all the duplicates removed
	private List dedupeList( List inList )
	{

		// Todo: someday make this comparison case
		// specific per tag, which would mean we'd need i

		Map listHash = hashFromList( inList );
		// Use the build in hash method to get
		// all the values back.
		// We need to promote the return from a Collection
		// to a Vector, so that we're ahove the List interface bar
		return new Vector( listHash.values() );
	}

	/*************************************************
	*
	*	Dealing with Regular Expressions
	*
	*************************************************/
	private void __sep__Regex_Methods__() {};
	////////////////////////////////////////////////////////////

	void setupPatterns()
		//throws Exception
	{
		
		final String kFName = "setupPatterns";

		// get a compiler
		// Only need ONE
		if( fCompilerEngine == null )
			fCompilerEngine = new Perl5Compiler();
		// get a matching engine
		// Only need ONE
		if( fMatcherEngine == null )
			fMatcherEngine = new Perl5Matcher();

		// Initial the two other arrays we need
		fCompiledPatterns = new Pattern [ fRegexTags.length ];
		fLastResults = new MatchResult [ fRegexTags.length ];

		// Once for each Regex tag
		for( int i=0; i<fRegexTags.length; i++ )
		{
			String regexText = getRegexText( i );

			// We generally want to dump the extra white
			// space that might surround the regex
			if( regexText != null )
				if( getIsExtendedSyntax(i) )
					regexText = regexText.trim();
			if( regexText == null || regexText.equals("") )
			{
				//throw new Exception( "Error: RegexTag:init: null regular expression text." );
				// System.err.println( "Error: RegexTag:init: null regular expression text." );
				errorMsg( kFName, "null regular expression text." );
				continue;
			}

				// System.out.println( "regex text" );
				traceMsg( kFName, regexText );
				// System.out.println( "" );


			// Compile the regular expression
			// one per regex
			try
			{
				fCompiledPatterns[i] = fCompilerEngine.compile( regexText,
					calculateCompilerOptionsMask(i) );
			}
			catch(MalformedPatternException e)
			{
				stackTrace( kFName, e,
				        "Could not compile regular expression '" + regexText + "'"
				        );
				// fCompiledPatterns[i] = null;
				fatalErrorMsg( kFName, "Exiting due to regex compiler exception: " + e );
				System.exit(1);
			}

		}   // End for each regex tag

	}

	// Do the actual match
	// We have to have i so we know which match slot to fill
	// WARNING: There are two similar versions of this method
	// If make changes here, change the other one as well
	// See the other version below
	// From a BUFFER
	boolean doMatch( int i, PatternMatcherInput inSourceBuffer,
		Pattern inCompiledPattern
		)
	{
		
		final String kFName = "doMatch";

		if( inSourceBuffer == null )
		{
			// system.out.println( "Warning: Regex: null source text/buffer to run pattern against." );
			mWorkUnit.errorMsg( this, kFName, "null source text/buffer to run pattern against." );
			return false;
		}

		// or boolean matcher.matches(text, exact match pattern))
		if( fMatcherEngine.contains( inSourceBuffer, inCompiledPattern ) )
		{
			// A result object is returned when a matcher matches something
			//MatchResult result = matcher.getMatch();
			fLastResults[i] = fMatcherEngine.getMatch();

			// Tell the world we have a match!
			//registerAMatch();

			// COUNT of how many matching groups

			if(mWorkUnit.shouldDoDebugMsg(this,kFName))
			{
				int groupCount = fLastResults[i].groups();
				// System.out.println(  "\tFound " + groupCount + " groups.");
				mWorkUnit.infoMsg( this, kFName, "Found " + groupCount + " groups.");
				for( int j=0; j<groupCount; j++ )
				{
					// Get a specific matched group by offset
					String text = fLastResults[i].group(j);
						// System.out.println( "\tgroup " + j + ": \"" +
						// text + "\"" );
					mWorkUnit.debugMsg( this, kFName, "group " + j + ": \"" +
							text + "\"" );
				}
			}
			return true;
		}
		else
		{
			// if(debug) System.out.println( "\tNo Regex match." );
			mWorkUnit.debugMsg( this, kFName, "No Regex match." );
			// make sure we don't get confused
			// about old matches
			fLastResults[i] = null;
			// since we didn't get a match, there's no point
			// in continueing
			return false;
		}

	}

	// Do the actual match
	// We have to have i so we know which match slot to fill
	// WARNING: There are two similar versions of this method
	// If make changes here, change the other one as well
	// See the other version above
	// From a STRING
	boolean doMatch( int i, String inSourceText, Pattern inCompiledPattern )
	{

		final String kFName = "doMatch";
		
		if( inSourceText == null )
		{
			// System.out.println( "Warning: Regex: null source text/buffer to run pattern against." );
			mWorkUnit.errorMsg( this, kFName, "null source text/buffer to run pattern against." );
			return false;
		}

		// or boolean matcher.matches(text, exact match pattern))
		if( fMatcherEngine.contains( inSourceText, inCompiledPattern ) )
		{
			// A result object is returned when a matcher matches something
			//MatchResult result = matcher.getMatch();
			fLastResults[i] = fMatcherEngine.getMatch();

			// Tell the world we have a match!
			//registerAMatch();

			// COUNT of how many matching groups

			if(mWorkUnit.shouldDoDebugMsg(this,kFName))
			{
				int groupCount = fLastResults[i].groups();
				// System.out.println(  "\tFound " + groupCount + " groups.");
				mWorkUnit.debugMsg( this, kFName, "Found " + groupCount + " groups.");
				for( int j=0; j<groupCount; j++ )
				{
					// Get a specific matched group by offset
					String text = fLastResults[i].group(j);
						// System.out.println( "\tgroup " + j + ": \"" +
						// text + "\"" );
					mWorkUnit.debugMsg( this, kFName, "group " + j + ": \"" +
							text + "\"" );
				}
			}
			return true;
		}
		else
		{
			// if(debug) System.out.println( "\tNo Regex match." );
			mWorkUnit.debugMsg( this, kFName, "No Regex match." );
			// make sure we don't get confused
			// about old matches
			fLastResults[i] = null;
			// since we didn't get a match, there's no point
			// in continueing
			return false;
		}
	}



	private void __sep__Chaff_ignore__() {};
	//////////////////////////////////////////////////////

	/***
	List getMappingList() {
		return getListFromAttribute( "mapping" );
	}
	***/

	/***
	PatternCompiler getCompiler() {
		return compiler;
	}
	Pattern getPattern() {
		return pattern;
	}
	PatternMatcher getMatcher() {
		//return matcher;
		return ((RegexTag)this).matcher;
	}
	***/

	/***
	named groups are not currently supported in the regex package we use
	public String getMatchGroupByName( String name )
	{
		int offset = mapNameToOffset( name );
		if( offset < 0 ) {
			System.out.println( "Error: RegexTag:getMatchGroupByName: mapNameToOffset could not find a mapping for the group name '" + name + "'" );
			return null;
		}

		// Just call the normal function with the offset
		return getMatchGroupByNumber( offset );

	}

	int mapNameToOffset( String name )
	{
		if( name == null || name.trim().equals("") ) {
			System.out.println( "Error: RegexTag:mapNameToOffset: requested name is null" );
			return -1;
		}
		name = name.trim().toLowerCase();
		// We will accept * to mean the entire string, group 0
		if( name.equals("*") )
			return 0;
		List mappingVector = getMappingList();
		if( mappingVector.size() < 1 ) {
			System.out.println( "Error: RegexTag:mapNameToOffset: this regex tag does not have a group name mapping assigned." );
			return -1;
		}

		// Look it up
		int tmpIndex = mappingVector.indexOf( name );

		// if it didn't find anything, just pass that info along
		if( tmpIndex < 0 )
			return tmpIndex;
		// If it did find something, add 1 to the result
		// to correct for one-offset vs. zero-offset of regex group #'s
		else
			return tmpIndex+1;

	}
	***/



	private void __sep__Constants__() {};
	//////////////////////////////////////////////////////
	// Control of the parameters to the Perl 5 regex compiler
	// see also calculateCompilerOptionsMask()
	private static final boolean DEFAULT_IS_CASE_SENSITIVE = false;
	private static final boolean DEFAULT_IS_EXTENDED_SYNTAX = true;
	private static final boolean DEFAULT_IS_MULTILINE = true;
	private static final boolean DEFAULT_IS_THREAD_SAFE_COMPILER = true;
	// Defaults for the regex attributes
	// See below for doc in the attr name section
	private static final boolean DEFAULT_DEL_SRC_ON_SUCECSS = false;
	private static final boolean DEFAULT_DEL_SRC_ON_NO_MATCH = false;
	// TODO: if this is which overall match to keep, where it matches mutliple times,
	// then make it consistnet with Pump Constants
	// private static final int DEFAULT_KEEP_WHICH = 1;
	private static final int DEFAULT_KEEP_WHICH = nie.pump.base.PumpConstants.DEFAULT_KEEP_WHICH;

	// Set to "multiple" or "concatenate"
	// MUST match a value understood by getStoreAsSeparateFields()
	private static final String DEFAULT_STORE_MULTI_AS = "multiple";

	// private static final boolean DEFAULT_ALLOW_DUPES = false;
	private static final boolean DEFAULT_ALLOW_DUPES = true;

	private static final String DEFAULT_CONCAT_SEP = ", ";


	// Some constant strings
	private static final String REGEX_TAG_NAME =
		"regex";
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
	// Regex tag attribute: Which regex group we want to grab
	private static final String REGEX_GROUP_NUM_ATTR_NAME =
		"group_number";
	private static final String REGEX_GROUP_NUM_ATTR_SHORT_NAME =
		"group";
	// Regex tag attribute: Whether to do case sensitive matching or not
	private static final String CASE_SENSITIVE_ATTR_NAME =
		"case_sensitive";
	private static final String CASE_SENSITIVE_ATTR_SHORT_NAME =
		"case";
	// Regex tag attribute: Whether to allow extended syntax
	//		including white space
	private static final String EXTENDED_SYNTAX_ATTR_NAME =
		"extended_syntax";
	private static final String EXTENDED_SYNTAX_ATTR_SHORT_NAME =
		"extended";
	// Regex tag attribute: whether to match on multiple lines
	//		as a single blob
	private static final String MULTILINE_ATTR_NAME =
		"multiline_match";
	private static final String MULTILINE_ATTR_SHORT_NAME =
		"multiline";
	// Regex tag attribute: whether to delete the source field
	//		after a good match
	private static final String DEL_SRC_ON_SUCCESS_ATTR_NAME =
		"delete_source_on_success";
	// Regex tag attribute: whether to delete the source field
	//		after no match
	private static final String DEL_SRC_ON_NO_MATCH_ATTR_NAME =
		"delete_source_on_no_match";
	// Regex tag attribute: Set the destination to a fixed value
	//		on a match, vs using matchtext
	private static final String USE_CONST_MATCH_TEXT_ATTR_NAME =
			"constant_match_text";
	// Regex tag attribute: default/nomatch value
	private static final String USE_DEFAULT_NOMATCH_TEXT_ATTR_NAME =
			"default_no_match_text";
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
	private static final String ALLOW_DUPES_ATTR_NAME =
			"allow_duplicates";
	// Regex tag attribute: If concatenating multiple
	//		matches, what string to put in between
	//		default is ", "
	private static final String CONCAT_SEP_ATTR_NAME =
			"concatenation_separator";

	WorkUnit mWorkUnit;
	
	private void __sep__Member_Fields1__() {};
	//////////////////////////////////////////////////////

	// The main JDOM node from the parameters section
	JDOMHelper fJdh;
	JDOMHelper fRegexTags [];

	// The queues
	Queue fReadQueue;
	Queue fWriteQueue;

	// The main pattern compiler
	// Only need ONE of these
	PatternCompiler fCompilerEngine;
	// The compiled pattern
	// one per regex
	Pattern fCompiledPatterns [];
	// The actual engine for running patterns against input
	// We only need ONE of these
	PatternMatcher fMatcherEngine;
	// The last result we calculated
	// one per instance
	MatchResult fLastResults [];

	// The optional mapping between named regex groups
	// and regex group offsets
	//Vector mappingVector;

}
