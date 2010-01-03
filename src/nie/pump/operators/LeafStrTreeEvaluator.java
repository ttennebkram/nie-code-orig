//package nie.core.operators;
package nie.pump.operators;
import nie.core.*;

import org.jdom.Element;
import java.util.*;

import nie.pump.base.WorkUnit;

/**
 * Title:        LeafStrTreeEvaluator
 * Description:  Evaluate tags representing String leaf operations
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop and Mark Benett
 * @version 1.0
 */

public class LeafStrTreeEvaluator
	extends JDOMHelper
	implements StrTreeInterface
{

	private static final boolean debug = false;

	private void __sep__Constructors_and_Init__ () {}
	////////////////////////////////////////////////////////////

	public LeafStrTreeEvaluator( Element element )
		throws OpTreeException, JDOMHelperException
	{
		super( element );

		if(debug) System.err.println( "LeafStr constr" );
		// Set some flags
		initSettings();

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
		if(debug) System.err.println( "LeafStr init settings" );

		// Setup the cache fields for get/set methods
		fHaveDoneInit = false;

		// Run the uncached methods and cache the results
		fRealOpName = getRealOpName();
		fPrimaryFieldName = getPrimaryFieldName();
		fFixedValueString = getFixedValueString();
		fJoinText = getJoinText();

		// We're done, enable the caching
		fHaveDoneInit = true;

		// we'll just let setupChildren's exception pass up the food chain
		verifyNoChildren();
	}

	void verifyNoChildren()
		throws OpTreeException
	{
		if(debug) System.err.println( "LeafStr verify no children" );
		if( getJdomChildrenCount() > 0 )
			throw new OpTreeException(
				"Leaf operator '" + getElementName() +
				"' must not have children"
				);
	}

	private void __sep__Process_Logic__ () {}
	/////////////////////////////////////////////////////////////////

	// Evaluate all the unusual options like
	// exists, empty, etc.
	//
	// SPACE_TAG
	// FIXED_TEXT_TAG
	// SINGULAR_FIELD_TAG
	// MULTIPLE_FIELD_TAG
	public String evaluate( WorkUnit inWU )
		throws OpTreeException
	{

		if(debug) System.err.println( "LeafStr evaluate start" );

		if( inWU == null )
			throw new OpTreeException(
				"evaluate was passed a null work unit"
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
		if(debug) System.err.println( "LeafStr eval, funcname=" + funcName );


		// Did they just want a space?
		if( funcName.equals( SPACE_TAG ) )
		{
			if(debug) System.err.println( "\treturning a space" );
			return " ";
		}

		// Did they just want a space?
		if( funcName.equals( FIXED_TEXT_TAG ) )
		{
			String fixedText = getFixedValueString();
			if( fixedText == null )
				throw new OpTreeException(
					"evalOp: no fixed text found"
					+ "  In element=" + getElementName()
					+ ", operator='" + funcName + "'"
					+ ", must specify either '" + FIXED_VALUE_ATTR + "'"
					+ " or '" + FIXED_VALUE_ATTR_2 + "'"
					+ " or '" + FIXED_VALUE_ATTR_3 + "' attribute"
					);
			if(debug) System.err.println( "\treturning fixed text"
				+ "\"" + fixedText + "\""
				);
			return fixedText;
		}

		// Did they just want a space?
		if( funcName.equals( SINGULAR_FIELD_TAG ) )
		{
			// Will throw exception if doesn't exist or more than one
			String valueStr = getPrimaryString( inWU );
			if(debug) System.err.println( "\treturning field value"
				+ "\"" + valueStr + "\""
				);
			return valueStr;
		}

		if( funcName.equals( MULTIPLE_FIELD_TAG ) )
		{
			if(debug) System.err.println( "\tcalc'ing multi field" );

			String fieldName = getPrimaryFieldName();
			// get the list of matching fields
			// We're hoping for EXACTLY ONE
			// There shouldn't be any true nulls in the list, since lists don't
			// allow such things.  There may be 0 length strings, but maybe that's
			// what they want, so we let that pass through.
			List fields = inWU.getUserFieldsText( fieldName );

			// None found, well, OK, we can answer all of them here
			if( fields == null || fields.size() < 1 )
				throw new OpTreeException(
					"getPrimaryString: can't find any primary field values in work unit"
					+ ", in element=" + getElementName()
					+ ", fieldName='" + fieldName + "'"
					);

			String joinText = getJoinText();

			if(debug) System.err.println( "\tfor field " + fieldName
				+ " found " + fields.size() + " values"
				);

			// Loop through the multiple values and concatenate into
			// a String Buffer
			StringBuffer buffer = new StringBuffer();
			for( Iterator it = fields.iterator(); it.hasNext(); )
			{
				String nextValue = (String)it.next();
				buffer.append( nextValue );
				// If we have join text AND this isn't the last of
				// them, add the join text as well
				if( joinText != null && it.hasNext() )
					buffer.append( joinText );
			}

			// Convert the buffer to a string and return it
			String answer = new String( buffer );
			if(debug) System.err.println( "\tanswer is \"" + answer + "\"" );
			return answer;

		}   // End if it's multi value joining

		// By now we should have returned a true or false value
		// The code should have encountered one of the logical branches
		// and gone home
		// Put one final catch all sanity check here

		throw new OpTreeException(
			"evalOp: can't process unknown operator"
			+ ", no processing rules matched."
			+ "  In element=" + getElementName()
			+ ", operator='" + funcName + "'"
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
			realName = (String)fOpAliasToNameMap.get( alias );
		}
		catch(Exception e)
		{
			throw new OpTreeException(
				"mapAliasToRealName was passed unresolvable alias(1): "
				+ alias
				);
		}
		if(realName == null )
			throw new OpTreeException(
				"mapAliasToRealName was passed unresolvable alias(2): "
				+ alias
				);

		return realName;
	}

	public static boolean getIsAStrLeafOp( String candidateName )
	{
		// See if we have an alias, but tell it to not throw an
		// exception if there isn't one
		String tmpResult = null;
		try
		{
			tmpResult = getRealNameForAlias( candidateName );
		}
		catch(Exception e)
		{
			tmpResult = null;
		}

		return tmpResult != null;
	}


	// Some functions to get the operands for the comparison

	private String getPrimaryString( WorkUnit inWU )
		throws OpTreeException
	{

		// We can't cache this, as it comes from the work unit

		if( inWU == null )
			throw new OpTreeException(
				"getPrimaryString: was passed a null work unit!"
				);

		// The name of the field
		// The get method will NOT throw an exception if it doesn't exist
		String fieldName = getPrimaryFieldName();

		if( fieldName == null || fieldName.trim().equals("") )
			throw new OpTreeException(
				"getPrimaryFieldName: no primary field specified"
				+ ", in element=" + getElementName()
				+ ", must specify either '" + PRIMARY_FIELD_ATTR + "'"
				+ " or '" + PRIMARY_FIELD_ATTR_2 + "' attribute"
				+ " or '" + PRIMARY_FIELD_ATTR_3 + "' attribute"
				);

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


	private String getPrimaryFieldName()
		throws OpTreeException
	{
		// Returned the cached value?
		if( fHaveDoneInit )
			return fPrimaryFieldName;

		// Look up the attribute
		String tmpStr = getStringFromAttribute( PRIMARY_FIELD_ATTR );
		// Altername names
		if( tmpStr == null || tmpStr.trim().equals("") )
			tmpStr = getStringFromAttribute( PRIMARY_FIELD_ATTR_2 );
		if( tmpStr == null || tmpStr.trim().equals("") )
			tmpStr = getStringFromAttribute( PRIMARY_FIELD_ATTR_3 );

//		if( tmpStr == null || tmpStr.trim().equals("") )
//			throw new OpTreeException(
//				"getPrimaryFieldName: no primary field specified"
//				+ ", in element=" + getElementName()
//				+ ", must specify either '" + PRIMARY_FIELD_ATTR + "'"
//				+ " or '" + PRIMARY_FIELD_ATTR_2 + "' attribute"
//				+ " or '" + PRIMARY_FIELD_ATTR_3 + "' attribute"
//				);

		// Normalize and return
		if( tmpStr != null )
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
		// Lookup secndary name
		if( tmpStr == null )
			tmpStr = getStringFromAttribute( FIXED_VALUE_ATTR_2 );
		// Lookup third name
		if( tmpStr == null )
			tmpStr = getStringFromAttribute( FIXED_VALUE_ATTR_3 );

		// Return whatever we got
		return tmpStr;
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

	private void __sep__Constants_and_Fields__ () {}
	//////////////////////////////////////////////////////////////

	// Caching member fields for get/set functions
	// See initSettings()
	private boolean fHaveDoneInit;
	private String fRealOpName;
	private String fPrimaryFieldName;
	private String fFixedValueString;
	private String fJoinText;

	// WARNING!!!!!!
	// If you add aliases here, make sure to update the
	// aliases mapping hash table fCompOpAliasToNameMap
	// This is in the static {} section right after the constructor

	// Normal comparison tags
	// BTW, if you mod this class of operator, you might want
	// to ALSO double check the fCompOpNameToBitmaskMap table
	private static final String SPACE_TAG = "space";
	private static final String FIXED_TEXT_TAG = "literal_text";
	private static final String FIXED_TEXT_TAG_2 = "text";
	private static final String SINGULAR_FIELD_TAG = "from_field";
	private static final String SINGULAR_FIELD_TAG_2 = "field";
	private static final String MULTIPLE_FIELD_TAG = "from_list_field";
	private static final String MULTIPLE_FIELD_TAG_2 = "list";

	// Some names for attribute names
	private static final String PRIMARY_FIELD_ATTR = "source_field";
	private static final String PRIMARY_FIELD_ATTR_2 = "field";
	private static final String PRIMARY_FIELD_ATTR_3 = "src";
	private static final String FIXED_VALUE_ATTR = "literal_value";
	private static final String FIXED_VALUE_ATTR_2 = "value";
	private static final String FIXED_VALUE_ATTR_3 = "text";
	// If joining together a multi value field, use this sequence of text
	private static final String JOINER_FIXED_VALUE_ATTR = "join_text";
	private static final String JOINER_FIXED_VALUE_ATTR_2 = "join_with";


	// Important mappings

	private static Hashtable fOpAliasToNameMap;
	// private static Hashtable fOpNameToBitmaskMap;


	private void __sep__STATIC_Init__ () {}
	////////////////////////////////////////////////////////////

	static
	{

		// We have many names for the tags
		// Map each to it's real name, INCLUDING the real name version

		fOpAliasToNameMap = new Hashtable();

		fOpAliasToNameMap.put( SPACE_TAG, SPACE_TAG );

		fOpAliasToNameMap.put( FIXED_TEXT_TAG, FIXED_TEXT_TAG );
		fOpAliasToNameMap.put( FIXED_TEXT_TAG_2, FIXED_TEXT_TAG );

		fOpAliasToNameMap.put( SINGULAR_FIELD_TAG, SINGULAR_FIELD_TAG );
		fOpAliasToNameMap.put( SINGULAR_FIELD_TAG_2, SINGULAR_FIELD_TAG );

		//fOpAliasToNameMap.put( MULTIPLE_FIELD_TAG, MULTIPLE_FIELD_TAG );
		//fOpAliasToNameMap.put( MULTIPLE_FIELD_TAG_2, MULTIPLE_FIELD_TAG );


		// Initialize the mapping of a normal comparison
		// operator to it's "happy zone" bitmask
		// fOpNameToBitmaskMap = new Hashtable();

		// We will later decode these bitmaks with
		// Integer.parseInt("101", 2) returns 5
		// fOpNameToBitmaskMap.put( EQUALS_TAG, "010" );

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
