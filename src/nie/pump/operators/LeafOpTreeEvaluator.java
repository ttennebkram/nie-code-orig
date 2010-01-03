//package nie.core.operators;
package nie.pump.operators;
import nie.core.*;

import org.jdom.Element;
import java.util.*;

import nie.pump.base.WorkUnit;

/**
 * Title:        LeafOpTreeEvaluator
 * Description:  Evaluate tags representing leaf operations
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop and Mark Benett
 * @version 1.0
 * Todo:
	use true comparators
	support dates as well as numbers and strings
	options for white space handling
	handling of nulls
	options for siltent warnings
	option for handle non-singular values
	support for <any> and <all> parents
 */

public class LeafOpTreeEvaluator
	extends JDOMHelper
	implements OpTreeInterface
{

	private static final boolean debug = false;

	private void __sep__Constructors_and_Init__ () {}
	////////////////////////////////////////////////////////////

	public LeafOpTreeEvaluator( Element element )
		throws OpTreeException, JDOMHelperException
	{
		super( element );

		// Set some flags
		initSettings();

		if(debug) System.out.println( "Debug: new BoolTree tag constructed." );
	}


	////////////////////////////////////////////////////////////////////
	//
	//
	//      IMPORTANT:
	//
	//      SEE THE static {} SECTION AT THE BOTTOM OF THE CLASS
	//      FOR MORE INITIALIATION!!!
	//
	//      Can't put it here due to forward reference restrictions
	//
	//
	////////////////////////////////////////////////////////////////////



	private void initSettings()
		throws OpTreeException
	{

		// Setup the cache fields for get/set methods
		fHaveDoneInit = false;

		// Run the uncached methods and cache the results
		fRealOpName = getRealOpName();
		fIsNormalOp = getIsNormalComparisonOperator();
		fBitmask = getBitmaskForOperator();
		fPrimaryFieldName = getPrimaryFieldName();
		fSecondaryFieldName = getSecondaryFieldName();
		fFixedValueString = getFixedValueString();
		fType = getType();
		fIsCasen = getIsCaseSensitive();

		// We're done, enable the caching
		fHaveDoneInit = true;

		// we'll just let setupChildren's exception pass up the food chain
		verifyNoChildren();
	}

	void verifyNoChildren()
		throws OpTreeException
	{
		if( getJdomChildrenCount() > 0 )
			throw new OpTreeException(
				"Leaf operator '" + getElementName() +
				"' must not have children"
				);
	}

	private void __sep__Process_Logic__ () {}
	/////////////////////////////////////////////////////////////////


	public boolean evaluate( WorkUnit inWU )
		throws OpTreeException
	{
		boolean result = false;
		if( getIsNormalComparisonOperator() )
			result = evalNormalOp( inWU );
		else
			result = evalSpecialOp( inWU );

		return result;
	}



	// Evaluate all the regular options like
	// equals, less than, gt, lte, etc.
	private boolean evalNormalOp( WorkUnit inWU )
		throws OpTreeException
	{

		//final boolean debug = false;

		if( inWU == null )
			throw new OpTreeException(
				"evalNormalOp was passed a null work unit"
				);

		// Get the two strings to compare
		// These functions will throw exceptions if there
		// is any problem
		String primaryString = getPrimaryString( inWU );
		String secondaryString = getSecondaryString( inWU );
		String type = getType();

		if( debug )
		{
			System.err.println( "evalNormalOp: Start." );
			System.err.println( "\tElement:" +
				JDOMToString()
				);
			System.err.println( "\treal op name=" + getRealOpName() );
			System.err.println( "\tprimaryString='" + primaryString + "'" );
			System.err.println( "\tsecondaryString='" + secondaryString + "'" );
			System.err.println( "\ttype='" + type + "'" );
		}

		// What's the numerical comparison?
		//int compareResult = primaryString.compareTo( secondaryString );
		//int compareResult = primaryString.compareToIgnoreCase(
		//	secondaryString
		//	);

		int compareResult = 0;
		if( type.equals(TYPE_TEXT) )
			compareResult = compareText( primaryString, secondaryString );
		else if( type.equals(TYPE_NUMBER) )
			compareResult = compareNumericStrings(
				primaryString, secondaryString
				);
		else
			throw new OpTreeException(
				"evalNormalOp: unknown data type: " + type
				+ ", we currently support " + TYPE_TEXT
				+ " and " + TYPE_NUMBER
				);

		if(debug) System.err.println( "\tcompareResult=" + compareResult );

		// Force it to a unit value
		if( compareResult != 0 )
			compareResult = compareResult < 0 ? -1 : 1 ;
		if(debug) System.err.println( "\tnow compareResult=" + compareResult );

		// What's the "Happy Zone" bitmask?
		int happyZoneBitMask = getBitmaskForOperator();
		if(debug) System.err.println( "\thappyZoneBitMask=" + happyZoneBitMask );

		// Create a center bitmask and then shift it
		int resultBitMask = 2;  // "010"
		if(debug) System.err.println( "\tstart resultBitMask=" + resultBitMask );
		// We use the right shift operator with the uniary comparison result
		// If it's negative it'll shift it left
		// resultBitMask = resultBitMask >> compareResult;
		// ^^^ This doesn't work, java gives 0 for a negative shift]
		if( compareResult > 0 )
			resultBitMask = resultBitMask >> 1;
		else if( compareResult < 0 )
			resultBitMask = resultBitMask << 1;

		if(debug) System.err.println( "\tnow resultBitMask=" + resultBitMask );
		//if(debug) System.err.println( "\t2 >> 1=" + ( 2 >> 1 ) );
		//if(debug) System.err.println( "\t2 >> -1=" + ( 2 >> -1 ) );

		// Now AND together the two bitmasks
		int finalBitMask = happyZoneBitMask & resultBitMask;
		if(debug) System.err.println( "\tfinalBitMask=" + finalBitMask );

		// If there's any bits left, we have a winner!
		if( finalBitMask != 0 )
			return true;
		else
			return false;

	}

	private int compareText( String str1, String str2 )
		throws OpTreeException
	{

		if( str1 == null || str2 == null )
			throw new OpTreeException(
				"LeafOpTreeEval:compareText:"
				+ " was passed one or more null arguments"
				);

		boolean isCasen = getIsCaseSensitive();

		int compareResult = 0;

		if( isCasen )
			compareResult = str1.compareTo( str2 );
		else
			compareResult = str1.compareToIgnoreCase( str2 );

		return compareResult;
	}

	private int compareNumericStrings( String str1, String str2 )
		throws OpTreeException
	{

		if( str1 == null || str2 == null )
			throw new OpTreeException(
				"LeafOpTreeEval:compareNumericStrings:"
				+ " was passed one or more null arguments"
				);

		double d1 = 0.0;
		double d2 = 0.0;

		// Convert the first number
		try
		{
			d1 = Double.parseDouble( str1 );
		}
		catch( NumberFormatException e1 )
		{
			throw new OpTreeException(
				"LeafOpTreeEval:compareNumericStrings:"
				+ " error converting primary string to number"
				+ ", string=" + str1
				+ ", error: " + e1
				);
		}
		// Convert the second number
		try
		{
			d2 = Double.parseDouble( str2 );
		}
		catch( NumberFormatException e2 )
		{
			throw new OpTreeException(
				"LeafOpTreeEval:compareNumericStrings:"
				+ " error converting secondary string to number"
				+ ", string=" + str2
				+ ", error: " + e2
				);
		}

		// Do the comparisons and return the result
		if( d1 < d2 )
			return -1;
		else if( d1 > d2 )
			return 1;
		else
			return 0;
	}


	// Evaluate all the unusual options like
	// exists, empty, etc.
	//
	// There is at least one field
	// EXISTS_TAG
	// NOT_EXISTS_TAG
	//
	// It does exist, and there is exactly one of them
	// SINGULAR_TAG
	// It DOES exist, and there's more than one of them
	// NOT_SINGULAR_TAG
	//
	// The one field that DOES exist has non white space characters in it
	// EMPTY_TAG
	// The one field that DOES exist has ONLY white space characters in it
	// or is an empty string
	// NOT_EMPTY_TAG
	// String stuff
	// STARTS_TAG NOT_STARTS_TAG ENDS_TAG NOT_ENDS_TAG
	// CONTAINS_TAG NOT_CONTAINS_TAG
	private boolean evalSpecialOp( WorkUnit inWU )
		throws OpTreeException
	{

		//final boolean debug = true;

		if( inWU == null )
			throw new OpTreeException(
				"evalSpecialOp was passed a null work unit"
				);

		// Get the function name and normalize
		String funcName = getRealOpName();
		// This should never happen, shouldn't be possible
		if( funcName == null || funcName.trim().equals("") )
			throw new OpTreeException(
				"evalSpecialOp found a null function name"
				);
		funcName = funcName.trim().toLowerCase();
		// Illegal function names will be caught at the very end

		// Find the primary field
		String fieldName = getPrimaryFieldName();

		// get the list of matching fields
		// We're hoping for EXACTLY ONE
		List fields = inWU.getUserFieldsText( fieldName );

		// There shouldn't be any true nulls in the list, since lists don't
		// allow such things.  There may be 0 length strings, but maybe that's
		// what they want, so we let that pass through.

		// At this point we will start whittling off different types
		// of functions as we come to a point where we can answer their
		// questions

		// None found, well, OK, we can answer all of them here
		if( fields == null || fields.size() < 1 )
		{

			// There is at least one field
			if( funcName.equals( EXISTS_TAG ) )
				return false;
			else if( funcName.equals( NOT_EXISTS_TAG ) )
				return true;

			// It does exist, and there is exactly one of them
			else if( funcName.equals( SINGULAR_TAG ) )
				return false;
			// It DOES exist, and there's more than one of them
			else if( funcName.equals( NOT_SINGULAR_TAG ) )
				return false;

			// The one field that DOES exist either has (or doesn't have)
			// white space characters in it
			// Moot point here as we didn't find any fields, so both
			// operators should fail
			else if( funcName.equals( EMPTY_TAG )
				|| funcName.equals( NOT_EMPTY_TAG )
				)
			{
				throw new OpTreeException(
					"evalSpecialOp: no primary field value found for singular"
					+ " operator."
					+ "  In element=" + getElementName()
					+ ", operator='" + funcName + "'"
					+ ", fieldName='" + fieldName + "'"
					);
			}

			// We'll catch illegal function names at the end

		}   // End if no field values were found

		// We CURRENTLY only handle singleton values
		// Todo: support more values later on

		// More than one found
		if( fields.size() > 1 )
		{

			// There is at least one field
			if( funcName.equals( EXISTS_TAG ) )
				return true;
			else if( funcName.equals( NOT_EXISTS_TAG ) )
				return false;

			// It does exist, and there is exactly one of them
			else if( funcName.equals( SINGULAR_TAG ) )
				return false;
			// It DOES exist, and there's more than one of them
			else if( funcName.equals( NOT_SINGULAR_TAG ) )
				return true;

			// The one field that DOES exist either has (or doesn't have)
			// white space characters in it
			// Moot point here as we didn't find any fields, so both
			// operators should fail
			else if( funcName.equals( EMPTY_TAG )
				|| funcName.equals( NOT_EMPTY_TAG )
				)
			{
				throw new OpTreeException(
					"evalSpecialOp: more than one primary field value found for"
					+ " singular operator."
					+ "  In element=" + getElementName()
					+ ", operator='" + funcName + "'"
					+ ", fieldName='" + fieldName + "'"
					+ ", number of values found=" + fields.size()
					);
			}
		}   // End if more than one field value found


		// If there's exactly one field
		if( fields.size() == 1 )
		{

			// There is at least one field
			if( funcName.equals( EXISTS_TAG ) )
				return true;
			else if( funcName.equals( NOT_EXISTS_TAG ) )
				return false;

			// It does exist, and there is exactly one of them
			else if( funcName.equals( SINGULAR_TAG ) )
				return true;
			// It DOES exist, and there's more than one of them
			else if( funcName.equals( NOT_SINGULAR_TAG ) )
				return false;

			// The one field that DOES exist either has (or doesn't have)
			// white space characters in it
			// Moot point here as we didn't find any fields, so both
			// operators should fail
			else if( funcName.equals( EMPTY_TAG )
				|| funcName.equals( NOT_EMPTY_TAG )
				)
			{

				// Grab the first field and return it
				String fieldValue = (String)fields.get(0);

				// Is it empty?
				if( fieldValue == null || fieldValue.trim().equals("") )
				{
					if( funcName.equals( EMPTY_TAG ) )
						return true;
					else if( funcName.equals( NOT_EMPTY_TAG ) )
						return false;
					// We'll catch straglers below
				}
				// Else it's not empty
				else
				{
					if( funcName.equals( EMPTY_TAG ) )
						return false;
					else if( funcName.equals( NOT_EMPTY_TAG ) )
						return true;
				}

			}   // End else it's one of the EMPTY operators

			// Else is it one of the string comparison tags
			else if( funcName.equals( STARTS_TAG )
					|| funcName.equals( NOT_STARTS_TAG )
					|| funcName.equals( ENDS_TAG )
					|| funcName.equals( NOT_ENDS_TAG )
					|| funcName.equals( CONTAINS_TAG )
					|| funcName.equals( NOT_CONTAINS_TAG )
				)
			{
				// Grab the first field and return it
				String fieldValue = (String)fields.get(0);

				// Is it empty?
				if( fieldValue == null || fieldValue.length() < 1 )
					throw new OpTreeException(
						"evalSpecialOp: null/zero length primary string."
						+ "  In element=" + getElementName()
						+ ", operator='" + funcName + "'"
						+ ", fieldName='" + fieldName + "'"
						);

				// Only perform on text fields
				if( ! getType().equals( TYPE_TEXT ) )
					throw new OpTreeException(
						"evalSpecialOp: text comparison requested"
						+ " on non-text field."
						+ "  In element=" + getElementName()
						+ ", operator='" + funcName + "'"
						+ ", fieldName='" + fieldName + "'"
						+ ", requested/defaulted type='" + getType() + "'"
						+ ", system text type='" + TYPE_TEXT + "'"
						+ ", system default type='" + DEFAULT_TYPE + "'"
						);

				// Will we be "flipping" the answer
				boolean negationLogic = false;
				if( funcName.equals( NOT_STARTS_TAG )
					|| funcName.equals( NOT_ENDS_TAG )
					|| funcName.equals( NOT_CONTAINS_TAG )
					)
				{
					negationLogic = true;
				}

				// Is case important?
				boolean isCasen = getIsCaseSensitive();

				// Get the second string to compare
				// This functions will throw exceptions if there
				// is any problem
				String secondaryString = getSecondaryString( inWU );
				// The two additional checks we need to do
				// Is it zero length?
				if( secondaryString == null || secondaryString.length() < 1 )
					throw new OpTreeException(
						"evalSpecialOp: zero length secondary string."
						+ "  In element=" + getElementName()
						+ ", operator='" + funcName + "'"
						);

				// Is it longer than the string we're comparing to?
				if( secondaryString.length() > fieldValue.length() )
					throw new OpTreeException(
						"evalSpecialOp: secondary string longer"
						+ " than primary string."
						+ "  In element=" + getElementName()
						+ ", operator='" + funcName + "'"
						+ ", primary='" + fieldValue + "'"
						+ " at " + fieldValue.length() + " chars long"
						+ ", secondary='" + secondaryString + "'"
						+ " at " + secondaryString.length() + " chars long"
						);

				// Since the functions don't all have casen logic,
				// we'll normalize to lower case
				if( ! isCasen )
				{
					fieldValue = fieldValue.toLowerCase();
					secondaryString = secondaryString.toLowerCase();
				}

				// Now we start the actual tests
				boolean answer = false;
				if( funcName.equals( STARTS_TAG )
					|| funcName.equals( NOT_STARTS_TAG )
					)
				{
					answer = fieldValue.startsWith( secondaryString );
				}
				else if( funcName.equals( ENDS_TAG )
					|| funcName.equals( NOT_ENDS_TAG )
					)
				{
					answer = fieldValue.endsWith( secondaryString );
				}
				else if( funcName.equals( CONTAINS_TAG )
					|| funcName.equals( NOT_CONTAINS_TAG )
					)
				{
					int tmpResult = fieldValue.indexOf( secondaryString );
					answer = tmpResult >= 0 ? true : false;
				}
				else
					throw new OpTreeException(
						"evalSpecialOp: can't process special string operator"
						+ "  In element=" + getElementName()
						+ ", operator='" + funcName + "'"
						+ ", fieldName='" + fieldName + "'"
						);

				// Do we flip the answer for the NOT_ version?
				if( negationLogic )
					answer = ! answer;

				// Now return the answer
				return answer;

			}   // End else is it one of the string comparison tags

		}   // End if there were exactly one field

		// By now we should have returned a true or false value
		// The code should have encountered one of the logical branches
		// and gone home
		// Put one final catch all sanity check here

		throw new OpTreeException(
			"evalSpecialOp: can't process special operator"
			+ ", no processing rules matched."
			+ "  In element=" + getElementName()
			+ ", operator='" + funcName + "'"
			+ ", fieldName='" + fieldName + "'"
			+ ", number of values found=" + fields.size()
			);

	}


	private void __sep__Simple_Get_and_Set__ () {}
	////////////////////////////////////////////////////////////


	// Map their alias to their real name
	// If it doesn't resolve we have a big problem
	// If you've tried to add new tags and are getting an exception
	// from here it's probably because you forgot to update
	// fCompOpAliasToNameMap in the static init section (in constr section)
	// The non static one caches
	// Todo: maybe combine these
	private String getRealOpName()
		throws OpTreeException
	{
		if( fHaveDoneInit )
			return fRealOpName;
		else
			return getRealNameForAlias( getElementName() );
	}
	private static String getRealNameForAlias( String alias )
		throws OpTreeException
	{
		if( alias == null || alias.trim().equals("") )
			throw new OpTreeException(
				"mapAliasToRealName was passed a null alias"
				);
		alias = alias.trim().toLowerCase();

		String realName = null;
		try
		{
			realName = (String)fCompOpAliasToNameMap.get( alias );
		}
		catch(Exception e)
		{
			throw new OpTreeException(
				"mapAliasToRealName was passed unresolvable alias(1): "
				+ alias
				);
		}
		if( realName == null )
			throw new OpTreeException(
				"mapAliasToRealName was passed unresolvable alias(2): "
				+ alias
				);

		return realName;
	}

	// Given a function name, get me the integer "happy zone" bitmask
	// Bitmask is always >= 0
	// If it doesn't find it, it'll return -1
	// This is also handy for checking whether an operator is "normal"
	// or not; normal ops have a bitmask, special ones don't
	private int getBitmaskForOperator()
		throws OpTreeException
	{
		if( fHaveDoneInit )
			return fBitmask;
		else
			return getBitmaskForOperator( getRealOpName() );
	}
	private static int getBitmaskForOperator( String opName )
		throws OpTreeException
	{
		//final boolean debug = true;

		if( opName == null || opName.trim().equals("") )
			throw new OpTreeException(
				"getBitmaskForOperator was passed a null op name"
				);
		opName = opName.trim().toLowerCase();

		// Init return value to failure code
		int returnValue = -1;

		if( debug )
		{
			System.err.println( "fCompOpNameToBitmaskMap=" );
			for( Iterator it = fCompOpNameToBitmaskMap.keySet().iterator();
				it.hasNext() ;
				)
			{
				String key = (String)it.next();
				String value = (String)fCompOpNameToBitmaskMap.get( key );
				System.err.println( key + "=" + value );
			}
		}

		// Find the bit string
		String bitString = null;
		try
		{
			bitString = (String)fCompOpNameToBitmaskMap.get( opName );
			if(debug) System.err.println( "bitString=" + bitString );
		}
		catch(Exception e)
		{
			// It's OK if we didn't find it, it's probably
			// a special operator
			return returnValue;
		}
		// Odd, not catching bad key exceptions?
		// OK, check for it here
		if( bitString == null )
			return returnValue;

		bitString = bitString.trim();

		// Now convert the bitmap string into an integer
		try
		{
			returnValue = Integer.parseInt( bitString, 2 );
		}
		catch(Exception e2)
		{
			throw new OpTreeException(
				"getBitmaskForOperator uable to convert bitmask to int"
				+ ", opName='" + opName + "'"
				+ ", bitmask='" + bitString + "'"
				);
		}

		// Double check the results
		// %000 and %111 are disallowed as they make no sense
		if( returnValue < 1 || returnValue > 6 )
		{
			throw new OpTreeException(
				"getBitmaskForOperator bitmask out of range"
				+ ", must be between 0 and 7 (decimal, inclusive)"
				+ ", opName='" + opName + "'"
				+ ", bitmask='" + bitString + "'"
				+ ", resulting int='" + returnValue + "'"
				);
		}

		// Finally, the answer
		return returnValue;
	}

	private boolean getIsNormalComparisonOperator()
		throws OpTreeException
	{
		if( fHaveDoneInit )
			return fIsNormalOp;
		else
			return getIsNormalComparisonOperator( getRealOpName() );
	}
	private static boolean getIsNormalComparisonOperator( String opName )
		throws OpTreeException
	{
		if( opName == null || opName.trim().equals("") )
			throw new OpTreeException(
				"getIsNormalComparisonOperator was passed a null op name"
				);
		int tmpInt = getBitmaskForOperator( opName );
		if( tmpInt > 0 )
			return true;
		else
			return false;
	}


	// Some functions to get the operands for the comparison

	private String getPrimaryString( WorkUnit inWU )
		throws OpTreeException
	{
		//getPrimaryFieldName
		//getType


		// We can't cache this, as it comes from the work unit

		if( inWU == null )
			throw new OpTreeException(
				"getPrimaryString: was passed a null work unit!"
				);

		// The name of the field
		// The get method will throw an exception if it doesn't exist
		String fieldName = getPrimaryFieldName();

		// get the list of matching fields
		// We're hoping for EXACTLY ONE
		List fields = inWU.getUserFieldsText( fieldName );

		// There shouldn't be any true nulls in the list, since lists don't
		// allow such things.  There may be 0 length strings, but maybe that's
		// what they want, so we let that pass through.

		// We CURRENTLY only handle singleton values
		// Todo: support more values later on

		// None found
		if( fields == null || fields.size() < 1 )
			throw new OpTreeException(
				"getPrimaryString: can't find primary field in work unit"
				+ ", in element=" + getElementName()
				+ ", fieldName='" + fieldName + "'"
				);

		// More than one found
		if( fields.size() > 1 )
			throw new OpTreeException(
				"getPrimaryString: found more than one primary field value"
				+ " in work unit, must be singleton (in current implementation)."
				+ " Suggestion: try using fieldname[n] syntax if you know"
				+ " which one you want."
				+ "  In element=" + getElementName()
				+ ", fieldName='" + fieldName + "'"
				+ ", num found=" + fields.size()
				);

		// Grab the first field and return it
		String returnValue = (String)fields.get(0);
		return returnValue;

	}

	private String getSecondaryString( WorkUnit inWU )
		throws OpTreeException
	{
		// We can't cache this, as it may come from the work unit

		// Pull in both possible braches
		String fieldName = getSecondaryFieldName();
		String fixedValue = getFixedValueString();

		// Double check, we must not have both, but we must have one
		// Todo: it would be nice to do this check during the constructor
		// If we have neither
		if( fieldName == null && fixedValue == null )
			throw new OpTreeException(
				"getSecondaryString: can't find secondary field"
				+ " or a fixed value; must set one or the other"
				+ ", in element=" + getElementName()
				);

		// If we have both
		if( fieldName != null && fixedValue != null )
			throw new OpTreeException(
				"getSecondaryString: can't specify both a secondary field"
				+ " and a fixed value; must set one or the other"
				+ ", in element=" + getElementName()
				+ ", fieldName='" + fieldName + "'"
				+ ", fixedValue='" + fixedValue + "'"
				);

		if( fixedValue != null )
			return fixedValue;

		// OK, by now we know we're specifying a live secondary field
		// from the work unit, we should get it

		// get the list of matching fields
		// We're hoping for EXACTLY ONE
		List fields = inWU.getUserFieldsText( fieldName );

		// There shouldn't be any true nulls in the list, since lists don't
		// allow such things.  There may be 0 length strings, but maybe that's
		// what they want, so we let that pass through.

		// We CURRENTLY only handle singleton values
		// Todo: support more values later on

		// None found
		if( fields == null || fields.size() < 1 )
			throw new OpTreeException(
				"getSecondaryString: can't find secondary field in work unit"
				+ ", in element=" + getElementName()
				+ ", fieldName='" + fieldName + "'"
				);

		// More than one found
		if( fields.size() > 1 )
			throw new OpTreeException(
				"getSecondaryString: found more than one secondary field value"
				+ " in work unit, must be singleton (in current implementation)."
				+ " Suggestion: try using fieldname[n] syntax if you know"
				+ " which one you want."
				+ "  In element=" + getElementName()
				+ ", fieldName='" + fieldName + "'"
				+ ", num found=" + fields.size()
				);

		// Grab the first field and return it
		String returnValue = (String)fields.get(0);
		return returnValue;
	}


	private String getPrimaryFieldName()
		throws OpTreeException
	{
		// Returned the cached value?
		if( fHaveDoneInit )
			return fPrimaryFieldName;

		// Look up the attribute
		String tmpStr = getStringFromAttribute( PRIMARY_FIELD_ATTR );
		// A string of white spaces, even zero length, is OK at this point
		if( tmpStr == null || tmpStr.trim().equals("") )
			tmpStr = getStringFromAttribute( PRIMARY_FIELD_ATTR_2 );

		if( tmpStr == null || tmpStr.trim().equals("") )
			throw new OpTreeException(
				"getPrimaryFieldName: no primary field specified"
				+ ", in element=" + getElementName()
				+ ", must specify either '" + PRIMARY_FIELD_ATTR + "'"
				+ " or '" + PRIMARY_FIELD_ATTR_2 + "' attribute"
				);

		// Normalize and return
		tmpStr = tmpStr.trim();
		return tmpStr;
	}

	private String getSecondaryFieldName()
	{
		// Returned the cached value?
		if( fHaveDoneInit )
			return fSecondaryFieldName;

		// Look up the attribute
		String tmpStr = getStringFromAttribute( SECONDARY_FIELD_ATTR );

		// It's fine if we don't find it, just return the default
		if( tmpStr == null || tmpStr.trim().equals("") )
			return null;

		// Normalize
		tmpStr = tmpStr.trim();

		return tmpStr;
	}

	private String getFixedValueString()
	{
		// Returned the cached value?
		if( fHaveDoneInit )
			return fFixedValueString;

		// Look up the attribute
		String tmpStr = getStringFromAttribute( FIXED_VALUE_ATTR );
		// A string of white spaces, even zero length, is OK at this point
		if( tmpStr == null )
			tmpStr = getStringFromAttribute( FIXED_VALUE_ATTR_2 );

		// Return whatever we got
		return tmpStr;
	}

	private String getType()
		throws OpTreeException
	{
		// Returned the cached value?
		if( fHaveDoneInit )
			return fType;

		// Look up the attribute
		String tmpStr = getStringFromAttribute( TYPE_ATTR );

		// It's fine if we don't find it, just return the default
		if( tmpStr == null || tmpStr.trim().equals("") )
			return DEFAULT_TYPE;

		// Normalize and check
		tmpStr = tmpStr.trim().toLowerCase();

		// If it's OK, return it
		if( tmpStr.equals( TYPE_TEXT ) || tmpStr.equals( TYPE_NUMBER ) )
			return tmpStr;
		//Todo: add support for TYPE_DATE = "date";

		// Else complain loudly!
		throw new OpTreeException(
			"getType: invalid type declaration"
			+ ", element=" + getElementName()
			+ ", invalid type string=" + tmpStr
			);
	}

	boolean getIsCaseSensitive()
	{
		// Returned the cached value?
		if( fHaveDoneInit )
			return fIsCasen;

		boolean tmpAns = getBooleanFromAttribute(
			CASE_SENSITIVE_ATTR_NAME,
			DEFAULT_IS_CASE_SENSITIVE
			);
		if( tmpAns == DEFAULT_IS_CASE_SENSITIVE )
			tmpAns = getBooleanFromAttribute(
				CASE_SENSITIVE_ATTR_SHORT_NAME,
				DEFAULT_IS_CASE_SENSITIVE
				);
		return tmpAns;
	}



	private void __sep__Constants_and_Fields__ () {}
	//////////////////////////////////////////////////////////////

	// Caching member fields for get/set functions
	// See initSettings()
	private boolean fHaveDoneInit;
	private String fRealOpName;
	private boolean fIsNormalOp;
	private int fBitmask;
	private String fPrimaryFieldName;
	private String fSecondaryFieldName;
	private String fFixedValueString;
	private String fType;
	private boolean fIsCasen;

	// WARNING!!!!!!
	// If you add aliases here, make sure to update the
	// aliases mapping hash table fCompOpAliasToNameMap
	// This is in the static {} section right after the constructor

	// Normal comparison tags
	// BTW, if you mod this class of operator, you might want
	// to ALSO double check the fCompOpNameToBitmaskMap table
	private static final String EQUALS_TAG = "equals";
	private static final String EQUALS_TAG_2 = "equal";
	private static final String EQUALS_TAG_3 = "eq";
	private static final String NOT_EQUALS_TAG = "not_equal";
	private static final String NOT_EQUALS_TAG_2 = "not_equals";
	private static final String NOT_EQUALS_TAG_3 = "ne";
	private static final String LESS_THAN_TAG = "less_than";
	private static final String LESS_THAN_TAG_2 = "lt";
	private static final String GREATER_THAN = "greater_than";
	private static final String GREATER_THAN_2 = "gt";
	private static final String LT_OR_EQ_TAG = "less_than_or_equal_to";
	private static final String LT_OR_EQ_TAG_2 = "lte";
	private static final String GT_OR_EQ_TAG = "greater_than_or_equal_to";
	private static final String GT_OR_EQ_TAG_2 = "gte";

	// Special tags
	private static final String EMPTY_TAG = "is_empty";
	private static final String EMPTY_TAG_2 = "empty";
	private static final String NOT_EMPTY_TAG = "not_empty";
	private static final String NOT_EMPTY_TAG_2 = "is_not_empty";
	private static final String EXISTS_TAG = "exists";
	private static final String NOT_EXISTS_TAG = "not_exists";
	private static final String NOT_EXISTS_TAG_2 = "does_not_exist";
	private static final String NOT_EXISTS_TAG_3 = "missing";

	// It does exist, and there is exactly one of them
	private static final String SINGULAR_TAG = "has_single_value";
	private static final String SINGULAR_TAG_2 = "singular";
	private static final String SINGULAR_TAG_3 = "singleton";
	// It DOES exist, and there's more than one of them
	private static final String NOT_SINGULAR_TAG = "has_multiple_values";
	private static final String NOT_SINGULAR_TAG_2 = "multiple";

	// Some String Functions
	private static final String STARTS_TAG = "starts_with";
	private static final String STARTS_TAG_2 = "starts";
	private static final String NOT_STARTS_TAG = "not_starts_with";
	private static final String NOT_STARTS_TAG_2 = "not_start_with";
	private static final String NOT_STARTS_TAG_3 = "not_starts";
	private static final String NOT_STARTS_TAG_4 = "not_start";

	private static final String ENDS_TAG = "ends_with";
	private static final String ENDS_TAG_2 = "ends";
	private static final String NOT_ENDS_TAG = "not_ends_with";
	private static final String NOT_ENDS_TAG_2 = "not_end_with";
	private static final String NOT_ENDS_TAG_3 = "not_ends";
	private static final String NOT_ENDS_TAG_4 = "not_end";

	private static final String CONTAINS_TAG = "contains";
	private static final String CONTAINS_TAG_2 = "contain";
	private static final String NOT_CONTAINS_TAG = "not_contains";
	private static final String NOT_CONTAINS_TAG_2 = "not_contain";


	// Some names for attribute names
	private static final String PRIMARY_FIELD_ATTR = "field_1";
	private static final String PRIMARY_FIELD_ATTR_2 = "field";
	private static final String SECONDARY_FIELD_ATTR = "field_2";
	private static final String FIXED_VALUE_ATTR = "literal_value";
	private static final String FIXED_VALUE_ATTR_2 = "value";
	private static final String TYPE_ATTR = "type";
	private static final String TYPE_TEXT = "text";
	private static final String TYPE_NUMBER = "number";
	//private static final String TYPE_DATE = "date";
	private static final String DEFAULT_TYPE = TYPE_TEXT;

	// tag attribute: Whether to do case sensitive matching or not
	// before doing string calculations
	private static final String CASE_SENSITIVE_ATTR_NAME =
		"case_sensitive";
	private static final String CASE_SENSITIVE_ATTR_SHORT_NAME =
		"case";
	private static final boolean DEFAULT_IS_CASE_SENSITIVE = false;


	// Important mappings

	private static Hashtable fCompOpAliasToNameMap;
	private static Hashtable fCompOpNameToBitmaskMap;


	private void __sep__STATIC_Init__ () {}
	////////////////////////////////////////////////////////////

	static
	{

		// We have many names for the tags
		// Map each to it's real name, INCLUDING the real name version

		fCompOpAliasToNameMap = new Hashtable();

		fCompOpAliasToNameMap.put( EQUALS_TAG, EQUALS_TAG );
		fCompOpAliasToNameMap.put( EQUALS_TAG_2, EQUALS_TAG );
		fCompOpAliasToNameMap.put( EQUALS_TAG_3, EQUALS_TAG );

		fCompOpAliasToNameMap.put( NOT_EQUALS_TAG, NOT_EQUALS_TAG );
		fCompOpAliasToNameMap.put( NOT_EQUALS_TAG_2, NOT_EQUALS_TAG );
		fCompOpAliasToNameMap.put( NOT_EQUALS_TAG_3, NOT_EQUALS_TAG );

		fCompOpAliasToNameMap.put( LESS_THAN_TAG, LESS_THAN_TAG );
		fCompOpAliasToNameMap.put( LESS_THAN_TAG_2, LESS_THAN_TAG );

		fCompOpAliasToNameMap.put( GREATER_THAN, GREATER_THAN );
		fCompOpAliasToNameMap.put( GREATER_THAN_2, GREATER_THAN );

		fCompOpAliasToNameMap.put( LT_OR_EQ_TAG, LT_OR_EQ_TAG );
		fCompOpAliasToNameMap.put( LT_OR_EQ_TAG_2, LT_OR_EQ_TAG );

		fCompOpAliasToNameMap.put( GT_OR_EQ_TAG, GT_OR_EQ_TAG );
		fCompOpAliasToNameMap.put( GT_OR_EQ_TAG_2, GT_OR_EQ_TAG );


		fCompOpAliasToNameMap.put( EMPTY_TAG, EMPTY_TAG );
		fCompOpAliasToNameMap.put( EMPTY_TAG_2, EMPTY_TAG );

		fCompOpAliasToNameMap.put( NOT_EMPTY_TAG, NOT_EMPTY_TAG );
		fCompOpAliasToNameMap.put( NOT_EMPTY_TAG_2, NOT_EMPTY_TAG );

		fCompOpAliasToNameMap.put( EXISTS_TAG, EXISTS_TAG );

		fCompOpAliasToNameMap.put( NOT_EXISTS_TAG, NOT_EXISTS_TAG );
		fCompOpAliasToNameMap.put( NOT_EXISTS_TAG_2, NOT_EXISTS_TAG );
		fCompOpAliasToNameMap.put( NOT_EXISTS_TAG_3, NOT_EXISTS_TAG );

		fCompOpAliasToNameMap.put( SINGULAR_TAG, SINGULAR_TAG );
		fCompOpAliasToNameMap.put( SINGULAR_TAG_2, SINGULAR_TAG );
		fCompOpAliasToNameMap.put( SINGULAR_TAG_3, SINGULAR_TAG );

		fCompOpAliasToNameMap.put( NOT_SINGULAR_TAG, NOT_SINGULAR_TAG );
		fCompOpAliasToNameMap.put( NOT_SINGULAR_TAG_2, NOT_SINGULAR_TAG );

		fCompOpAliasToNameMap.put( STARTS_TAG, STARTS_TAG );
		fCompOpAliasToNameMap.put( STARTS_TAG_2, STARTS_TAG );

		fCompOpAliasToNameMap.put( NOT_STARTS_TAG, NOT_STARTS_TAG );
		fCompOpAliasToNameMap.put( NOT_STARTS_TAG_2, NOT_STARTS_TAG );
		fCompOpAliasToNameMap.put( NOT_STARTS_TAG_3, NOT_STARTS_TAG );
		fCompOpAliasToNameMap.put( NOT_STARTS_TAG_4, NOT_STARTS_TAG );

		fCompOpAliasToNameMap.put( ENDS_TAG, ENDS_TAG );
		fCompOpAliasToNameMap.put( ENDS_TAG_2, ENDS_TAG );

		fCompOpAliasToNameMap.put( NOT_ENDS_TAG, NOT_ENDS_TAG );
		fCompOpAliasToNameMap.put( NOT_ENDS_TAG_2, NOT_ENDS_TAG );
		fCompOpAliasToNameMap.put( NOT_ENDS_TAG_3, NOT_ENDS_TAG );
		fCompOpAliasToNameMap.put( NOT_ENDS_TAG_4, NOT_ENDS_TAG );

		fCompOpAliasToNameMap.put( CONTAINS_TAG, CONTAINS_TAG );
		fCompOpAliasToNameMap.put( CONTAINS_TAG_2, CONTAINS_TAG );

		fCompOpAliasToNameMap.put( NOT_CONTAINS_TAG, NOT_CONTAINS_TAG );
		fCompOpAliasToNameMap.put( NOT_CONTAINS_TAG_2, NOT_CONTAINS_TAG );



		// Initialize the mapping of a normal comparison
		// operator to it's "happy zone" bitmask
		fCompOpNameToBitmaskMap = new Hashtable();

		// We will later decode these bitmaks with
		// Integer.parseInt("101", 2) returns 5
		fCompOpNameToBitmaskMap.put( EQUALS_TAG, "010" );
		fCompOpNameToBitmaskMap.put( NOT_EQUALS_TAG, "101" );
		fCompOpNameToBitmaskMap.put( LESS_THAN_TAG, "100" );
		fCompOpNameToBitmaskMap.put( GREATER_THAN, "001" );
		fCompOpNameToBitmaskMap.put( LT_OR_EQ_TAG, "110" );
		fCompOpNameToBitmaskMap.put( GT_OR_EQ_TAG, "011" );

		// The list of base operators that are not "normal"
		//EMPTY_TAG
		//NOT_EMPTY_TAG
		//EXISTS_TAG
		//NOT_EXISTS_TAG

	}


	private void __sep__Notes_and_Todo__ () {}
	////////////////////////////////////////////////////////////

/**************************************

// Later
in_range
not_in_range
regex_match_field
match_field

// Other names
field_empty or
field_missing


contains
starts"
exists and is not empty (contains non-white space)
op="not_empty
op="min_count" value="5" >
op="range" modifier="float" min="100" max="250"

modifier="float"
modifier="nocase" >
nowarn no_warning

primary_field first_field field left_field src source_field
second_field right_field
fixed_value value

> 	Sal > 30,000
> 	<if  field="salary" op=">=" value="30000" modifier="float" >
>
> 	There is a field called phone number
> 	<if  field="phone_number" op="exists" >
>
> 	The first name contains the substring Kevin
> 	<if field="first_name" op="contains" value="Kevin" modifier="nocase" >
>
> 	The state starts with "ca"
> 	<if field="state" op="starts" value="Ca" modifier="nocase" >
>
> 	The phone number exists and is not empty (contains non-white space)
> 	<if field="phone_number" op="not_empty" >
>
> 	We have at least 5 part numbers
> 	<if field="part_number" op="min_count" value="5" >
>
> 	Number in range
> 	<if field="weight" op="range" modifier="float" min="100" max="250">

	<regex_match_field field_name="..." value="..." />
	<regex_match_field field_name="..." field_name="..." />

	<match_field field_name="..." value="..." />
	<match_field field_name="..." field_name="..." />

	<less_than field_name="..." value="..." />
	<less_than field_name="..." field_name="..." />

	<greater_than field_name="..." value="..."/>
	<greater_than field_name="..." field_name="..."/>

	<less_than_or_equal_to field_name="..." value="..." />
	<less_than_or_equal_to field_name="..." field_name="..." />

	<greater_than_or_equal_to field_name="..." value="..." />
	<greater_than_or_equal_to field_name="..." field_name="..." />

	<not>
	<and>
	<or>

********************************************/


}
