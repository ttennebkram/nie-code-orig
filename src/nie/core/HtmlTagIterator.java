package nie.core;

import java.net.URLEncoder;

// HtmlTagIterator
// A class for scanning through HTML text and making changes
// to targeted attribute values
//
// For example, scan through a web page and find all of the
// hyperlinks (a tags), and then tweak the link (the href attribute)

// Similar to Java Iterator EXCEPT you do not get a separate object back
// So in some respects, more like a database cursor

// FAQ:
//
// Why do we need this class?
// To iterate through messy HTML pages and tweak certain attribute values.
//
// Why not just use a utility method for this?  Why a whole class?
// We need to maintain a complex state; we'd need to pass too many
// variables back and forth.
//
// Why not just use a standard parser?
// Standard parsers that we've tested so far make subtle changes to HTML
// that ARE noticeable.  You wouldn't think they would be, but they are.
//
// Why not use XHTML so that you could use XPath or XSLT?
// Many pages are poorly formed and not XHTML complaint.
// AND these would also affect the display of the page (see previous question)
//
// Why not use JTidy to fixup HTML into XHTML?
// There are some pages JTidy can't handle, and it's hard to tell success from failure.
// AND it would affect the page's appearance, as in previous two items.
//
// Why not just require properly formed HTML?
// The real world has poorly formed pages which we must handle.
// We often do not control the source content.
//
// Since this acts like an Iterator, why not just implement that interface?
// Several reasons actually:
// 1: Implementer returns a separate object; we want them to just use this object.
//    And returning ourselves would be confusing.
// 2: It's hard to do hasNext without actually trying, which moves the cursor forward.
//    And maintaining 2 sets of points would complicate things.
// 3: We need additional methods, so a user would need to keep casting back
//    to this class anyway - not much savings.
// 4: Showing a return type of void on the next() method is jarring enough to
//    make a user pay attention and actually read this doc.
//
// Why does the class have an isDone method instead of a hasNext() ?
// We actually need to attempt to get another one to know if one is there,
// and this changes all the state variables.  And maintaining multiple
// states or doing rollbacks would be more complicated.
//
// How does this handle tags that inside of comment blocks or JavaScript?
// It will still try to change them.  It doesn't know about that higher level structure.
// A possible workaround, if needed, is to bound the replacements to a certain part
// of the page with the start and end patterns.

public class HtmlTagIterator
{

// Useful for logging functions
private static final String kClassName = "HtmlTagIterator";

public HtmlTagIterator( String inDoc, String inTagName, String inAttrName )
	throws IllegalArgumentException // NOT InvalidArgumentException
{
	final String kFName = "Constructor";
	final String kExTag = kClassName + '.' + kFName + ": ";

	if( null==inDoc )
		throw new IllegalArgumentException( kExTag + "Null input document" );
	mOrigBuff = new StringBuffer( inDoc );

	String tmpTag = NIEUtil.trimmedStringOrNull( inTagName );
	if( null==tmpTag )
		throw new IllegalArgumentException( kExTag + "Null/empty tag name" );
	setTagName( tmpTag );

	String tmpAttr = NIEUtil.trimmedStringOrNull( inAttrName );
	if( null==tmpAttr )
		throw new IllegalArgumentException( kExTag + "Null/empty attribute name" );
	setAttrName( tmpAttr );

	setIsCasen( DEFAULT_IS_CASEN );
	// ^^^ This will also handle the normalized fields as needed
}

// Attempts to find a match, moving along in the buffer
// Returns true if it found another valid match, false otherwise
// False also implies that it's done
public boolean next()
{
	final String kFName = "next";
	boolean debug = shouldDoDebugMsg( kFName );
	boolean trace = shouldDoTraceMsg( kFName );

	if( ! getHasStarted() )
	{
		if(debug) debugMsg( kFName, "Start"
			+ ", caseSensitive=" + getIsCasen()
			+ ", hasNormBuff=" + (null!=mNormBuff)
			+ ", hasSearchBuff=" + (null!=mSearchBuff)
			+ ", Scan fences = " + mStartAt + '/' + mStopAt + "'"
			+ " (Reminder: -1 = no limit at end)"
			);

		setHasStarted();
	}

	if( getIsDone() )
	{
		errorMsg( kFName, "Have already finished searching through buffer; no more iterations." );
		return false;
	}
	if( getHasFailedBoundarySetting() )
	{
		warningMsg( kFName, "Attempt to set start/end pattern for boundary of document scan had previously failed, so results are suspect." );
	}

	// Set the starting gate / fence, if any was requested
	// and ending gate should be taken care of with the
	// setXxx methods before calling this
	
	// AND we will be modifying the buffers, possibly changing their
	// length, as we do substitutions.

	// Keep scanning until we find a form tag
	// or run out of buffer
	while( true )
	{
		if(debug) debugMsg( kFName, "Top of main loop." );

		// Sanity check
		if( mOrigBuff.length() != mSearchBuff.length() )
		{
			errorMsg( kFName, "Buffers out of sync, lenghts don't match"
				+ ", buff.length=" + mOrigBuff.length()
				+ ", buffNorm.length=" + mSearchBuff.length()
				);
			// break;
			setIsDone();
			return false;
		}
		// Always do bounds checking at the start
		if( mCurrDocPointer >= mSearchBuff.length() || (mStopAt>0 && mCurrDocPointer >= mStopAt ) )
		{
			if(debug) debugMsg( kFName, "Past end of buffer"
				+ ", buff.length=" + mSearchBuff.length()
				+ ", startAt=" + mStartAt
				+ ", stopAt=" + mStopAt
				);
			// break;
			setIsDone();
			return false;
		}

		// Search for the start of the tag
		// TODO: doens't known to skip text inside comments or Java Script
		mCurrTagBeginsAt = mSearchBuff.indexOf( getSearchTagPattern(), mCurrDocPointer );
		if( mCurrTagBeginsAt < 0 )
		{
			if(debug) debugMsg( kFName, "No more start-of-tag(s) found, done."
				+ " literal start pattern = '" + getSearchTagPattern() + "'"
				);
			// break;
			setIsDone();
			return false;
		}
		if(debug) debugMsg( kFName, "Tag start at " + mCurrTagBeginsAt );

		// End of the Tag
		// TODO: doens't known to skip text inside quotes, comments or Java Script
		mCurrTagEndsAt = mSearchBuff.indexOf( HTML_TAG_END, mCurrTagBeginsAt + getSearchTagPattern().length() );
		if( mCurrTagEndsAt < 0 )
		{
			errorMsg( kFName, "Couldn't find ending form tag, no more forms will be tweaked"
				+ ", form started at offset " + mCurrTagBeginsAt
				+ ", literal end pattern = '" + HTML_TAG_END + "'"
				);
			// break;
			setIsDone();
			return false;
		}
		if(debug) debugMsg( kFName, "Tag end at " + mCurrTagEndsAt );

		mCurrAttrNameStartsAt = mSearchBuff.indexOf( getSearchAttrPattern(), mCurrTagBeginsAt + getSearchTagPattern().length() );
		if( mCurrAttrNameStartsAt < 0 || mCurrAttrNameStartsAt > mCurrTagEndsAt )
		{
			if(debug) debugMsg( kFName, "Attr name not found within tag bounds "
				+ mCurrTagBeginsAt + " to " + mCurrTagEndsAt + ", offset=" + mCurrAttrNameStartsAt
				+ ", literal attr pattern = '" + getSearchAttrPattern() + "'"
				);
			mCurrDocPointer = mCurrTagEndsAt + HTML_TAG_END.length();
			continue;
		}
		if(debug) debugMsg( kFName, "Attr name starts at " + mCurrAttrNameStartsAt );

		// Now we need to find the string delimiters and what they enclose
		mCurrDelim = "";
		boolean haveSeenFirstDelim = false;
		mCurrAttrValueStartsAt = -1;
		mCurrAttrValueEndsAt = -1;
		// Move through the buffer
		for( int delimCheck = mCurrAttrNameStartsAt + getSearchAttrPattern().length();
			delimCheck < mCurrTagEndsAt;
			delimCheck++
			)
		{
			if(trace) traceMsg( kFName, "Top of inner loop, delimCheck=" + delimCheck );

			// Get the characters one at a time
			// We use strings because they can vary in length
			String s = mSearchBuff.substring( delimCheck, delimCheck+1 );
			// If not inside the value yet
			if( ! haveSeenFirstDelim )
			{
				// Skip over leading spaces
				if( s.equals(" ") || s.equals("\t")
						|| s.equals("\r") || s.equals("\n")
				) {
					// move to the next char in THIS tag span
					continue;
				}
				// quotes or apostrophies
				else if( s.equals("\"") || s.equals("'") )
				{
					mCurrDelim = s;
					haveSeenFirstDelim = true;
					// Starts at the next character
					mCurrAttrValueStartsAt = delimCheck + 1;
					if(debug) debugMsg( kFName, "Attr value start delim='" + mCurrDelim + "' at " + delimCheck );
				}
				// It's an unquoted string
				else
				{
					// Space is a sentinal for any whitespace
					// or the end of the tag
					mCurrDelim = " ";
					haveSeenFirstDelim = true;
					// Starts HERE
					mCurrAttrValueStartsAt = delimCheck;						
					if(debug) debugMsg( kFName, "Unquoted attr value starting at " + delimCheck );
				}						
			}
			// Else we're in the value
			else
			{
				// Handle white space
				if( s.equals(" ") || s.equals("\t")
						|| s.equals("\r") || s.equals("\n")
				) {
					// If unquoted string, this is past the end
					if( mCurrDelim.equals(" ") )
					{
						// It actually stopped on the PREVIOUS character
						mCurrAttrValueEndsAt = delimCheck - 1;
						if(debug) debugMsg( kFName, "Unquoted attr value terminating space at " + delimCheck );
						break;
					}
					// Else we're still crusing along
					// reading whitespace inside a quoted value
					// Odd for a URL...
					else {
						// move to the next char in THIS tag span
						continue;
					}
				}
				// quotes or apostrophies
				else if( s.equals(mCurrDelim) )
				{
					// It actually stopped on the PREVIOUS character
					mCurrAttrValueEndsAt = delimCheck - 1;
					if(debug) debugMsg( kFName, "Attr value closing delim='" + s + "' at " + delimCheck );
					break;
				}
				// Else it's some other regular character
				else
				{
					// Keep pushing the end along
					// though will probably be caught by other
					// logic as well
					mCurrAttrValueEndsAt = delimCheck;
				}
			}	// End else we're in a value
		}	// End for each char in tag range

		if(debug) debugMsg( kFName, "Out of inner loop"
			+ ", prelim attr value starts/ends = "
			+ mCurrAttrValueStartsAt + '/' + mCurrAttrValueEndsAt );

		// Final catch up and sanity checks
		if( mCurrAttrValueStartsAt >= 0 )
		{
			if( mCurrAttrValueEndsAt < 0 )
			{
				mCurrAttrValueEndsAt = mCurrTagEndsAt - 1;
				if(debug) debugMsg( kFName, "Correcting attrValueEndsAt to " + mCurrAttrValueEndsAt );
			}
			if( mCurrAttrValueEndsAt < mCurrAttrValueStartsAt )
			{
				warningMsg( kFName, "Trouble parsing attribute value"
					+ ", doc length=" + mSearchBuff.length()
					+ "Offsets: fences(+)=" + mStartAt + '/' + mStopAt
					+ ", tagBounds[]=" + mCurrTagBeginsAt + '/' + mCurrTagEndsAt
					+ ", attrNameBegin=" + mCurrAttrNameStartsAt
					+ ", attrValueBounds[]=" + mCurrAttrValueStartsAt + '/' + mCurrAttrValueEndsAt
					);
				// Move past the end of the Form tag
				mCurrDocPointer = mCurrTagEndsAt + HTML_TAG_END.length();
				continue;
			}
			// Else we're OK so far, keep going
		}
		// Else no valid attr value
		else {
			if(debug) debugMsg( kFName, "No start of attribute value found"
					+ ", doc length=" + mSearchBuff.length()
					+ "Offsets: fences(+)=" + mStartAt + '/' + mStopAt
					+ ", tagBounds[]=" + mCurrTagBeginsAt + '/' + mCurrTagBeginsAt
					+ ", attrNameBegin=" + mCurrAttrNameStartsAt
					+ ", attrValueBounds[]=" + mCurrAttrValueStartsAt + '/' + mCurrAttrValueEndsAt
					);
			// Move past the end of the Form tag
			mCurrDocPointer = mCurrTagEndsAt + HTML_TAG_END.length();
			continue;
		}

		// OK, we appear to have a valid value...
		// Note: our end pointers are inclusive
		// String attrVal = mSearchBuff.substring( mCurrAttrValueStartsAt, mCurrAttrValueEndsAt+1 );
		// attrVal = NIEUtil.trimmedStringOrNull( attrVal );
		String attrVal = getOriginalAttrValueOrNull();
		// Skip over empty values and try again
		if( null==attrVal )
		{
			if(debug) debugMsg( kFName, "Empty attribute value found"
					+ ", doc length=" + mSearchBuff.length()
					+ "Offsets: fences(+)=" + mStartAt + '/' + mStopAt
					+ ", tagBounds[]=" + mCurrTagBeginsAt + '/' + mCurrTagEndsAt
					+ ", attrNameBegin=" + mCurrAttrNameStartsAt
					+ ", attrValueBounds[]=" + mCurrAttrValueStartsAt + '/' + mCurrAttrValueEndsAt
					);
			// Move past the end of the Form tag
			mCurrDocPointer = mCurrTagEndsAt + HTML_TAG_END.length();
			continue;				
		}
		// Else we have a good one
		else {
			if(debug) debugMsg( kFName, "Found attribute value '" + attrVal + "'"
					// + ", doc length=" + mSearchBuff.length()
					// + "Offsets: fences(+)=" + mStartAt + '/' + mStopAt
					+ ", tagBounds[]=" + mCurrTagBeginsAt + '/' + mCurrTagEndsAt
					+ ", attrNameBegin=" + mCurrAttrNameStartsAt
					+ ", attrValueBounds[]=" + mCurrAttrValueStartsAt + '/' + mCurrAttrValueEndsAt
					);
			// Adjust the point forward
			mCurrDocPointer = mCurrTagEndsAt + HTML_TAG_END.length();
			// Pointer is always PAST the current stuff
			// but the attribute names, etc are current

			return true;
		}

		/***
		if(debug) debugMsg( kFName, "Current attrVal='" + attrVal + "'" );

		// Assess this URL, see if it's worth messing with
		boolean doSubst = false;

		// We may want to do all of them
		// OR if we were inside of a fence and got this far
		// then we'll do it as well
		if( SearchEngineConfig._TWEAK_CHOICE_ALL_FORMS.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_AFTER_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
			)
		{
			doSubst = true;
			if(debug) debugMsg( kFName, "Configured to match all in-fence-range values" );
		}
		// Search engine URL?
		else if( SearchEngineConfig._TWEAK_CHOICE_SAME_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull( getSearchEngineURL() );
			if( null!=compUrl && attrVal.startsWith(compUrl) )
			{
				doSubst = true;
				if(debug) debugMsg( kFName, "Matches normalized search engine URL '" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized search engine URL '" + compUrl + "'" );				
			}
		}
		// A specific URL prefix?
		else if( SearchEngineConfig._TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull(
				getSearchEngine().getResultsFormsTweakArg1()
				);
			if( null!=compUrl && attrVal.startsWith(compUrl) )
			{
				doSubst = true;
				if(debug) debugMsg( kFName, "Matches normalized URL prefix '" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized URL prefix '" + compUrl + "'" );					
			}
		}
		// EXCLUDE a specific URL prefix?
		else if( SearchEngineConfig._TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
		{
			String compUrl = NIEUtil.trimmedLowerStringOrNull(
				getSearchEngine().getResultsFormsTweakArg1()
				);
			if( null!=compUrl && ! attrVal.startsWith(compUrl) )
			{
				doSubst = true;	
				if(debug) debugMsg( kFName, "Matches normalized Exclude prefix, so will NOT tweak, pattern='" + compUrl + "'" );
			}
			else {
				if(debug) debugMsg( kFName, "Does NOT match normalized Exclude prefix, so WILL tweak, pattern='" + compUrl + "'" );					
			}
		}

		if(debug) debugMsg( kFName, "Near bottom, Main startAt was " + startAt + ", tagStart was " + formTagBeginsAt );					

		// If we're supposed to do the substitution
		if( doSubst )
		{
			String newValue = getSearchNamesURL();

			if(debug) debugMsg( kFName, "WILL replace with new value='" + newValue + "'" );					

			// Do the same edits to BOTH buffers
			// Also, even though we trimmed source URL for comparison,]
			// we're still overwriting the entire length with the new URL
			buff.replace( mCurrAttrValueStartsAt, mCurrAttrValueEndsAt+1, newValue );
			buffNorm.replace( mCurrAttrValueStartsAt, mCurrAttrValueEndsAt+1, newValue.toLowerCase() );

			// Delta length
			int newLen = newValue.length();
			int oldLen = mCurrAttrValueEndsAt - mCurrAttrValueStartsAt + 1;
			int deltaLen = newLen - oldLen;

			if(debug) debugMsg( kFName, "Pointer adjustment new/old/delta = " + newLen + '/' + oldLen + '/' + deltaLen );					

			// Adjust Start At
			startAt = mCurrTagEndsAt + deltaLen + HTML_TAG_END.length();
			
		}
		// Else not substitution
		else {
			// We still need to adjust Start At
			startAt = mCurrTagEndsAt + HTML_TAG_END.length();
		}	

		if(debug) debugMsg( kFName, "AT bottom, Main startAt NOW " + mStartAt + ", tagStart was " + mCurrTagBeginsAt );
		return true;
		***/

	}	// End for each tag in document
	
	// If we get here, we did NOT find anything
	// But all these cases are handled above
	// if(debug) debugMsg( kFName, "Ran out of buffer, no more found." );
	// setIsDone();
	// return false;
	
}

// WARNING: This is giving UN-decoded sequences
// If you're trying to get URLs, you might want to run the output through
// URLEncoder.decode( origKey, AuxIOInfo.CHAR_ENCODING_UTF8 );
public String getOriginalAttrValueOrNull()
{
	final String kFName = "getOriginalAttrValueOrNull";
	if( getIsDone() )
	{
		errorMsg( kFName, "No more tags / attribute values, returning null." );
		return null;
	}
	if( ! getHasStarted() )
	{
		errorMsg( kFName, "Must call next() to find first tag/attribute before calling this, returning null." );
		return null;
	}

	// OK, we have a valid value!
	// Note: our end pointers are inclusive
	String attrVal = mOrigBuff.substring( mCurrAttrValueStartsAt, mCurrAttrValueEndsAt+1 );
	attrVal = NIEUtil.trimmedStringOrNull( attrVal );
	return attrVal;
}


// WARNING: This is giving UN-decoded sequences
// If you're trying to get URLs, you might want to run the output through
// URLEncoder.decode( origKey, AuxIOInfo.CHAR_ENCODING_UTF8 );
public String getNormalizedAttrValueOrNull()
{
	final String kFName = "getNormalizedAttrValueOrNull";
	if( getIsDone() )
	{
		errorMsg( kFName, "No more tags / attribute values, returning null." );
		return null;
	}
	if( ! getHasStarted() )
	{
		errorMsg( kFName, "Must call next() to find first tag/attribute before calling this, returning null." );
		return null;
	}

	// OK, we have a valid value!
	// Note: our end pointers are inclusive
	String attrVal = mSearchBuff.substring( mCurrAttrValueStartsAt, mCurrAttrValueEndsAt+1 );
	attrVal = NIEUtil.trimmedStringOrNull( attrVal );
	return attrVal;
}

// Carefully replace the attribute value
// which includes possibly updating 2 buffers
// and adjusting positional pointers
public boolean replaceAttrValue( String newValue )
{
	final String kFName = "replaceAttrValue";
	boolean debug = shouldDoDebugMsg( kFName );
	if( null==newValue )
	{
		errorMsg( kFName, "Null passed in, ignoring.  If you want to clear the value, use a zero length string." );
		return false;
	}
	if( getIsDone() )
	{
		errorMsg( kFName, "No more tags / attribute values, returning null." );
		return false;
	}
	if( ! getHasStarted() )
	{
		errorMsg( kFName, "Must call next() to find first tag/attribute before calling this, returning null." );
		return false;
	}

	// Delta length
	// Our start/end bounds are INCLUSIVE, unlike Java's default
	int oldLen = mCurrAttrValueEndsAt - mCurrAttrValueStartsAt + 1;
	int newLen = newValue.length();
	int deltaLen = newLen - oldLen;

	if(debug) debugMsg( kFName, "WILL replace with new value='" + newValue + "'" );					

	// Some sanity bounds checking
	if( mCurrAttrValueStartsAt<0 || mCurrAttrValueEndsAt<0 || oldLen<0
		|| mCurrAttrValueEndsAt >= mOrigBuff.length()
		)
	{
		errorMsg( kFName, "Existing attribute value pointers are invalid"
			+ ", doc length=" + mSearchBuff.length()
			+ "Offsets: fences(+)=" + mStartAt + '/' + mStopAt
			+ ", tagBounds[]=" + mCurrTagBeginsAt + '/' + mCurrTagEndsAt
			+ ", attrNameBegin=" + mCurrAttrNameStartsAt
			+ ", attrValueBounds[]=" + mCurrAttrValueStartsAt + '/' + mCurrAttrValueEndsAt
			);
		return false;
	}


	// Sanity Check / Double check the buffers
	if( null!=mNormBuff && mOrigBuff.length() != mNormBuff.length() )
	{
		errorMsg( kFName, "Buffers out of sync / different length"
				+ ", doc length=" + mSearchBuff.length()
				+ ", sourceBuff=" + mOrigBuff.length() + " chars"
				+ ", normalizedBuff=" + mNormBuff.length() + "chars"
				+ ". Offsets: fences(+)=" + mStartAt + '/' + mStopAt
				+ ", tagBounds[]=" + mCurrTagBeginsAt + '/' + mCurrTagEndsAt
				+ ", attrNameBegin=" + mCurrAttrNameStartsAt
				+ ", attrValueBounds[]=" + mCurrAttrValueStartsAt + '/' + mCurrAttrValueEndsAt
				);
		return false;
	}

	// Do the same edits to BOTH buffers
	// Also, even though we trimmed source URL for comparison,]
	// we're still overwriting the entire length with the new URL
	mOrigBuff.replace( mCurrAttrValueStartsAt, mCurrAttrValueEndsAt+1, newValue );
	if( null!=mNormBuff )
	{
		mNormBuff.replace( mCurrAttrValueStartsAt, mCurrAttrValueEndsAt+1, newValue.toLowerCase() );
	}

	if(debug) debugMsg( kFName, "Pointer adjustment old/new/delta = " + oldLen + '/' + newLen + '/' + deltaLen );					

	// Adjust Start At
	// mCurrDocPointer = mCurrTagEndsAt + deltaLen + HTML_TAG_END.length();
	// The other stuff is already handled in next()
	mCurrDocPointer += deltaLen;

	// Also adjust the attribute value stuff, in case they try again
	mCurrAttrValueEndsAt += deltaLen;
	
	return true;
}

private static void ___Setters_and_Getters___(){}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

public String getDocument()
{
	return new String( mOrigBuff );
}

public String getOrigTagName() { return mOrigTagName; }
public String getSearchTagPattern() { return mNormTagName; }

public String getOrigAttrName() { return mOrigAttrName; }
public String getSearchAttrPattern() { return mNormAttrName; }

// Private: only we get to set this
private void setTagName( String inName )
{
	mOrigTagName = inName;
	mNormTagName = inName.startsWith("<") ? inName : "<" + inName;
	if( getIsCasen() )
		mNormTagName = mNormTagName.toLowerCase();
}
// used when case sensitivity is changed
private void resetTagName() { setTagName(getOrigTagName()); }

private void setAttrName( String inName )
{
	mOrigAttrName = inName;
	mNormAttrName = inName.endsWith("=") ? inName : inName + "=";
	if( getIsCasen() )
		mNormAttrName = mNormAttrName.toLowerCase();
}
//used when case sensitivity is changed
private void resetAttrName() { setAttrName(getOrigAttrName()); }

private boolean getHasStarted()
{
	return mHasStarted;
}
private void setHasStarted()
{
	mHasStarted = true;
}
public boolean getIsDone()
{
	return mIsDone;
}
public void setIsDone()
{
	mIsDone = true;
}

public boolean getIsCasen() { return mIsCasen; }
public void setIsCasen( boolean flag )
{
	final String kFName = "setIsCasen";
	if( getHasStarted() )
	{
		errorMsg( kFName, "Too late to change this; must be called before first iteration.  Ignoring." );
		return;
	}
	mIsCasen = flag;
	// If not case sensitive, then we actually
	// need to setup lower case buffer
	// caseINsensitive
	if( ! flag )
	{
		mNormBuff = new StringBuffer( new String(mOrigBuff).toLowerCase() );
		mSearchBuff = mNormBuff;
	}
	// Else IS case sensitive, so we use the original stuff
	else {
		mNormBuff = null;
		mSearchBuff = mOrigBuff;
	}
	resetTagName();
	resetAttrName();
}

// success = true
public boolean setStartPattern( String inPattern )
{
	final String kFName = "setStartPattern";
	if( getHasStarted() )
	{
		errorMsg( kFName, "Too late to change this; must be called before first iteration.  Setting error code." );
		mHasFailedBoundarySetting = true;
		return false;
	}
	// If we have a pattern
	mOrigStartPattern = inPattern;
	if( null!=inPattern )
	{
		mNormStartPattern = getIsCasen() ? inPattern : inPattern.toLowerCase();
		// do lookup
		mStartAt = mSearchBuff.indexOf( mNormStartPattern );
		if( mStartAt < 0 )
		{
			errorMsg( kFName, "Start pattern not found, will search from start of document."
				+ " pattern='" + mNormStartPattern + "'"
				);
			mStartAt = 0;
			mHasFailedBoundarySetting = true;
			return false;
		}
		else {
			return true;
		}
	}
	// Else no pattern
	else {
		mNormStartPattern = null;
		mStartAt = 0;
		mHasFailedBoundarySetting = true;
		errorMsg( kFName, "Null pattern passed in.  Setting error code." );
		return false;
	}
}

public int getStartDocBoundary()
{
	return mStartAt;
}

// success = true
public boolean setEndPattern( String inPattern )
{
	final String kFName = "setEndPattern";
	if( getHasStarted() )
	{
		errorMsg( kFName, "Too late to change this; must be called before first iteration.  Setting error code." );
		mHasFailedBoundarySetting = true;
		return false;
	}
	// If we have a pattern
	mOrigStopPattern = inPattern;
	if( null!=inPattern )
	{
		mNormStopPattern = getIsCasen() ? inPattern : inPattern.toLowerCase();
		// do lookup
		mStopAt = mSearchBuff.indexOf( mNormStopPattern );
		if( mStopAt < 0 )
		{
			errorMsg( kFName, "Start pattern not found, will search from start of document."
				+ " pattern='" + mNormStartPattern + "'"
				);
			mStopAt = -1;
			mHasFailedBoundarySetting = true;
			return false;
		}
		else {
			// The pattern could be the last instances, so allow for it
			// TODO: this could be argued either way...
			mStopAt += inPattern.length() ;
			return true;
		}
	}
	// Else no pattern
	else {
		mNormStopPattern = null;
		mStopAt = -1;
		mHasFailedBoundarySetting = true;
		errorMsg( kFName, "Null pattern passed in.  Setting error code." );
		return false;
	}
}

public int getEndDocBoundary()
{
	return mStopAt;
}


public boolean getHasFailedBoundarySetting()
{
	return mHasFailedBoundarySetting;
}

private static void ___Run_Logging___(){}
//////////////////////////////////////////////////////////////////

// Handling Errors and warning messages
// ************* NEW LOGGING STUFF *************

private static RunLogInterface getRunLogObject()
// can't access some of impl's extensions with interface reference
//private static RunLogBasicImpl getRunLogObject()
{
	// return RunLogBasicImpl.getRunLogObject();
	return RunLogBasicImpl.getRunLogObject();
}
//	// Return the same thing casted to allow access to impl extensions
//	private static RunLogBasicImpl getRunLogImplObject()
//	{
//		// return RunLogBasicImpl.getRunLogObject();
//		return RunLogBasicImpl.getRunLogImplObject();
//	}

// New style
private static boolean statusMsg( String inFromRoutine, String inMessage )
{
	return getRunLogObject().statusMsg( kClassName, inFromRoutine,
		inMessage
		);
}

// New style
private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
{
	return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
		inMessage
		);
}

// New style
private static boolean infoMsg( String inFromRoutine, String inMessage )
{
	return getRunLogObject().infoMsg( kClassName, inFromRoutine,
		inMessage
		);
}
private static boolean shouldDoInfoMsg( String inFromRoutine )
{
	return getRunLogObject().shouldDoInfoMsg( kClassName, inFromRoutine );
}



// Old style
private void debugMsg( String inFromRoutine, String inMessage )
{
	staticDebugMsg( inFromRoutine, inMessage );
}
private static void staticDebugMsg(
	String inFromRoutine, String inMessage
	)
{

	getRunLogObject().debugMsg( kClassName, inFromRoutine,
		inMessage
		);

//		if( debug )
//		{
//			messageLogger( "Debug: " + kClassName + ":" + inFromRoutine + ":"
//				+ inMessage
//				);
//		}
}
private static boolean shouldDoDebugMsg( String inFromRoutine )
{
	return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
}

// New style
private static boolean traceMsg( String inFromRoutine, String inMessage )
{
	return getRunLogObject().traceMsg( kClassName, inFromRoutine,
		inMessage
		);
}
private static boolean shouldDoTraceMsg( String inFromRoutine )
{
	return getRunLogObject().shouldDoTraceMsg( kClassName, inFromRoutine );
}

// Old style
private void warningMsg( String inFromRoutine, String inMessage )
{
	staticWarningMsg( inFromRoutine, inMessage );
}
private static void staticWarningMsg(
	String inFromRoutine, String inMessage
	)
{
	getRunLogObject().warningMsg( kClassName, inFromRoutine,
		inMessage
		);

//		messageLogger( "Warning: " + kClassName + ":" + inFromRoutine + ":"
//			+ inMessage
//			);
}
private void errorMsg( String inFromRoutine, String inMessage )
{
	staticErrorMsg( inFromRoutine, inMessage );
}
private static void staticErrorMsg(
	String inFromRoutine, String inMessage
	)
{
	getRunLogObject().errorMsg( kClassName, inFromRoutine,
		inMessage
		);

//		messageLogger( "Error: " + kClassName + ":" + inFromRoutine + ":"
//			+ inMessage
//			);
}

private static boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
{
	return getRunLogObject().stackTrace( kClassName, inFromRoutine,
		e, optMessage
		);
}

// Newer style
private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
{
	return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
		inMessage
		);
}

private static void ___Member_Variables_and_Constants___(){}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

private boolean mHasStarted = false;
private boolean mIsDone = false;

private boolean mHasFailedBoundarySetting = false;

private String mOrigTagName;
private String mNormTagName;

private String mOrigAttrName;
private String mNormAttrName;

// The OPTIONAL patterns for which
// sections of the doc to look in
private String mOrigStartPattern;
private String mNormStartPattern;
private String mOrigStopPattern;
private String mNormStopPattern;

private boolean mIsCasen = DEFAULT_IS_CASEN;
public static final boolean DEFAULT_IS_CASEN = false;

private StringBuffer mOrigBuff;
// Normalized buffer, IF used
private StringBuffer mNormBuff;
// Which one to search against
// Will point to either Original or Normalized version
private StringBuffer mSearchBuff;

// The bounds that we will check
private int mStartAt = 0;
// ^^^ This needs to be 0, not -1
// stopAt will work like Java, where it's a zero based offset
// that is ONE PAST where you want to stop, so that
// stop-start = length
// Only active if > 0
// < 0 means no stop margin, go to natural end
private int mStopAt = -1;
// HOWEVER
// pointers/offsets for the beginnings and ends of specific tags
// with names including "begin" and "end"
// below are on the actual first and last character offsets

// Where we'll look NEXT
// needs to start at 0, not -1
private int mCurrDocPointer = 0;

// These other positions are critical to be set
// correctly so are initialized to -1
private int mCurrTagBeginsAt = -1;
private int mCurrTagEndsAt = -1;

private int mCurrAttrNameStartsAt = -1;
private int mCurrAttrNameEndsAt = -1;

private int mCurrAttrValueStartsAt = -1;
private int mCurrAttrValueEndsAt = -1;

private String mCurrDelim;

public static final String HTML_TAG_END = ">";

}
