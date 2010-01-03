package nie.sr2;

import java.util.*;

import org.jdom.Element;
import org.jdom.Attribute;
import nie.core.*;
import nie.sn.CSSClassNames;

/**
 * @author mbennett
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class XMLDefinedField
	implements XMLReportFilterInterface
{

	private final static String kClassName = "XMLDefinedField";

	public XMLDefinedField(
		Element inFieldDefinitionElement,
		XMLDefinedReport inReport,
		// nie.sn.SearchTuningApp inMainApp
		nie.sn.SearchTuningConfig inMainConfig
		)
			throws ReportConfigException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==inFieldDefinitionElement )
			throw new ReportConfigException( kExTag
				+ "Null field definition passed in."
				);
		if( null==inReport )
			throw new ReportConfigException( kExTag
				+ "Null report object passed in."
				);
		if( null==inMainConfig )
			throw new ReportConfigException( kExTag
				+ "Null application configuration object passed in."
				);

		fMainElem = inFieldDefinitionElement;
		fReport = inReport;
		// fMainApp = inMainApp;
		fMainConfig = inMainConfig;

		initCachedFields();

		// Some final sanity checking
		if( getIsExternalLink() && getIsReportLink() )
			throw new ReportConfigException( kExTag
				+ "Field is set to be BOTH an external link AND"
				+ " a report link; can not do both."
				);

		if( ! getShouldDisplay() && getShouldDoStatistics() )
			throw new ReportConfigException( kExTag
				+ "Field is set overall to NOT be displayed, but the display of statistics was requested"
				+ "; can't display statistics for a non-visible field."
				);

		if( ! getShouldDoStatistics() && null!=getNullStatsValueOrNull() )
			throw new ReportConfigException( kExTag
				+ "A null-value stats replacement was set for use in statistics, but this field doesn't have statistics enabled."
				);
	}

	private void initCachedFields()
		throws ReportConfigException
	{
		fUseCache = false;

		// This first one kicks in some of the others as well
		// generateSQLFieldString();
		// getSqlFieldExpression();
		getSqlFieldAsSelectString();

		getShouldDisplay();
		getHeading();
		getHeadingColSpan();
		getNullValueDisplay();

		// Statistics
		getShouldDoStatistics();
		// ^^^ this also caches for getShouldCalculateTotal(), getShouldCalculateMin()
		// getShouldCalculateMax(), getShouldCalculateAverage(), getShouldCalculateCount()
		getHasMultipleStatistics();
		getNullStatsValue();

		getCssHeadingClass();
		getCssDataClassOrNull();

		getIsSortField();
		getIsFilterField();
		getIsDeclaredDefaultSortField();
		getIsSortReversible();
		getInitialSortDirection();

		getTrueLinkElem();
		getIsReportLink();
		getDoLinkOnNull();

		getFilterOperator();
		getDoQuoteFilterValues();
		getDoFilterOnNull();

		getLinkTarget();
		getLinkTitle();
		getLinkedReportName();
		getLinkedReportFilterField();
		getIsExternalLink();

		// Numeric formatting
		getExplicitGeneralFormat();
		getExplicitMinDecimalPlaces();
		getExplicitMaxDecimalPlaces();
		getIsExplcitlyNumeric();
		getIsExplcitlyDate();
		getExplicitPrefixString();
		getExplicitSuffixString();

		// Date formatting
		getDateFormat();	// Calls default as well

		// Get this one towards the end
		// this sets cFieldID AND cIsExplicitFieldID
		getFieldID();

		initAnyDecodeInfo();

		fUseCache = true;

	}



	void initAnyDecodeInfo()
		throws ReportConfigException
	{
		final String kFName = "initAnyDecodeInfo";
		final String kExTag = kClassName + '.' + kFName + ": ";

		cHasDecode = false;

		// Look for optional decide tree
		Element mainDecodeElement = JDOMHelper.findElementByPath(
			fMainElem, DECODE_PATH, false
			);
		if( null==mainDecodeElement )
			return;

		// The default (do not trim)
		cDecodeDefaultValue = JDOMHelper.getTextByPath(mainDecodeElement, DECODE_DEFAULT_PATH);

		// cIsDecodeNumeric
		// cIsDecodeRanged

		List itemsList = JDOMHelper.findElementsByPath(mainDecodeElement, DECODE_VALUE_PATH);
		if( null==itemsList || itemsList.isEmpty() ) {
			if( null==cDecodeDefaultValue )
				throw new ReportConfigException( kExTag +
					"Decode needs one or more items or at least a default."
					);
		}

		cDecodeHash = new Hashtable();
		for( Iterator it=itemsList.iterator(); it.hasNext() ; ) {
			Element item = (Element)it.next();
			String key = JDOMHelper.getStringFromAttributeTrimOrNull( item, DECODE_VALUE_MATCH_ATTR );
			if( null==key )
				throw new ReportConfigException( kExTag +
					"Decode items must have a key value to match against."
					);
			if( cDecodeHash.containsKey(key) )
				throw new ReportConfigException( kExTag +
					"Duplicate decode items key \"" + key + "\""
					);
			String value = item.getText();
			if( null==value )
				value = "";
			cDecodeHash.put( key, value );
		}
		cHasDecode = true;
	}

	public Element generateHeaderElement()
	{
		final String kFName = "generateHeaderElement";

		// Get the value for this header
		String heading = getHeadingOrNull();

		if( ! getShouldDisplay() )
		{
			errorMsg( kFName,
				"This field is not supposed to be displayed."
				+ " Field: \""
				+ getFieldIDOrNull()
				+ "\" / \""
				+ heading
				+ "\" in report \"" + getReportName() + "\""
				+ " Returning null element."
				);
			return null;
		}

		// Starting building the return element
		Element outElem = new Element( "th" );
		outElem.setAttribute(
			"class",
			getCssHeadingClass()
			);
		// Headings can now have colspans
		if( getHeadingColSpan() > 1 )
			outElem.setAttribute(
				"colspan",
				"" + getHeadingColSpan()
				);
		// If there's text, add it
		// Note that we ALWAYS create a cell, otherwise
		// columns would be messed up
		if( null!=heading )
		{
			outElem.addContent( heading );
		}
		else
		{
			warningMsg( kFName,
				"No heading for field "
				+ " in report \"" + getReportName() + "\""
				// + " Returning null."
				);
		}

		// We're done
		return outElem;

	}


	// It's perfectly normally to pass in a null!
	// this is NOT an error condition
	public Element generateDataElement(
		Object inObjValue,
		AuxIOInfo inRequest,
		Hashtable optFieldValueStrings
		)
	{
		final String kFName = "generateDataElement";

		boolean debug = shouldDoDebugMsg( kFName );

		String report = getReportName();
		String fieldID = getFieldIDOrNull();

		String heading = getHeadingOrNull();

		String fieldDesc =
				"Report: \"" + report
				+ "\", Field: \"" + fieldID
				+ "\" / \"" + heading + "\""
				+ " = \"" + inObjValue + "\""
				;


		// Complain if we're not supposed to display this
		if( ! getShouldDisplay() )
		{
			errorMsg( kFName,
				"This field is not supposed to be displayed. "
				+ fieldDesc
				+ " Returning null element."
				);
			return null;
		}

		if(debug)
			debugMsg( kFName,	fieldDesc );


		// Get the value for that field
		String strValue = null;
		boolean wasNull = true;

		boolean isDecode = cHasDecode;
		boolean isNumeric = getIsExplcitlyNumeric()
			|| ( NIEUtil.isNumericType( inObjValue ) && ! getIsExplcitlyDate() );
		boolean isPercentage = getExplicitGeneralFormat() != null &&
			getExplicitGeneralFormat().equals(FORMAT_AS_PERCENT);
		boolean isBoolean = getExplicitGeneralFormat() != null && getExplicitGeneralFormat().equals(FORMAT_AS_BOOLEAN);
		boolean isDate = getIsExplcitlyDate() || NIEUtil.isDateType( inObjValue );
		if(debug)
			debugMsg( kFName,
				"format=" + getExplicitGeneralFormat()
				+ ", isNumeric=" + isNumeric
				+ ", isBoolean=" + isBoolean
				);

		if( null != inObjValue )
		{
		    // wasNull = false;

		    // Check decode first
			if( isDecode )
				strValue = doDecode( inObjValue );

			// Check boolean before number
			else if( isBoolean )
				strValue = formatBooleanTypeToString( inObjValue, true );

			else if( isNumeric )
				strValue = formatNumericTypeToString( inObjValue, true );

			else if( isDate )
				strValue = formatDateTypeToString( inObjValue, true );

			else {
				strValue = inObjValue.toString();
				if(debug)
					debugMsg( kFName,
						"Is a " + inObjValue.getClass().getName()
						);
			}

		}
		else {
			debugMsg( kFName, "Was null" );
		}

		// If it's still null, see if there was something assigned
		// It may still be null after this of course
		if( null == strValue )
		{
			// wasNull = true;

			// which may still be null
			strValue = getNullValueDisplay();

			// We can sometimes have a second chance at formatting
			// the null value, however, we don't if decode was used
			if( ! isDecode ) {
				// See if we want to format this null value?
				// check for boolean first
				if( null!=strValue && isBoolean )
				{
					String newStrValue = formatBooleanTypeToString( strValue, false );
					if( null!=newStrValue ) {
						strValue = newStrValue;
						// We need to know whether or not to add the prefix / suffix
						wasNull = false;
					}
				}
				// We might have set some type of numeric null value
				// which we would like to have diplayed
				else if( null!=strValue && isNumeric )
				{
					String newStrValue = formatNumericTypeToString( strValue, false );
					if( null!=newStrValue ) {
						strValue = newStrValue;
						// We need to know whether or not to add the prefix / suffix
						wasNull = false;
					}
				}
				else if( null!=strValue && isDate )
				{
					String newStrValue = formatDateTypeToString( strValue, false );
					if( null!=newStrValue ) {
						strValue = newStrValue;
						// We need to know whether or not to add the prefix / suffix
						wasNull = false;
					}
				}
			}

			// We DON'T change the wasNull flag here,
			// if it was ORIGINALLY null, then that's the
			// logic we will follow, even if we decide to
			// display something
		}
		else {
		    wasNull = false;
		}

		// Add any prefix or suffix text that might have been requested
		// We only do this if we had a reasonable chance of a non-standlone null token
		// In other words, don't add $ or % to just a null marker of -
		if( ! wasNull ) {
			String prefix = getPrefix();
			String suffix = getSuffix();
			if( null!=prefix || null!=suffix ) {
				StringBuffer tmpBuff = new StringBuffer();
				if( null!=prefix )
					tmpBuff.append(prefix);
				if( null!=strValue )
					tmpBuff.append(strValue);
				if( null!=suffix )
					tmpBuff.append(suffix);
				strValue = new String( tmpBuff );
			}
		}

		// Get a CSS class
		String cssClass = getCssDataClassOrNull();
		if( null==cssClass ) {
			// if( isBoolean )
			if( isPercentage )
				cssClass = CSSClassNames.PERCENTAGE_CELL;
			else if( isNumeric )
				cssClass = CSSClassNames.NUMERIC_CELL;
			else if( isDate )
				cssClass = CSSClassNames.DATETIME_CELL;
			else
				cssClass = CSSClassNames.DATA_CELL;
			// TODO: Add support for boolean, etc.
		}

		// Start building the output element
		Element outElem = new Element( "td" );
		outElem.setAttribute( "class", cssClass );
		// If there's text, add it
		// Note that we ALWAYS create a cell, otherwise
		// columns would be messed up
		if( null != strValue )
		{
			/***
			statusMsg( kFName,
				"field=" + fieldID
				+ " getIsExternalLink()=" + getIsExternalLink()
				);
			***/

			// The name of the current filter that has been
			// invoked, if any
			String currFilterName = inRequest.getScalarCGIFieldTrimOrNull(
				ReportConstants.FILTER_NAME_CGI_FIELD_NAME
				);
			if(debug)
				debugMsg( kFName,
					"fieldDesc=" + fieldDesc
					+ " currFilterName=" + currFilterName
					);

			// If it's an external link
			if( getIsExternalLink()
					&&
					( 	! wasNull
						||
						getDoLinkOnNull()
					)
				)
			{
				Element a = new Element( "a" );
				a.setAttribute( "href", strValue );
				a.setAttribute( "target", getLinkTarget() );
				a.setAttribute(
					"class", CSSClassNames.ACTIVE_RPT_LINK
					);
				a.addContent( strValue );
				outElem.addContent( a );
			}
			// Else if it's a filter field
			// AND it's not null, or is null but they say that's OK
			else if(
					getIsReportLink()	// It is a link
					&&
					( 	! wasNull			// And this value warrents it
						||
						getDoLinkOnNull()
					)
					&&
					(	null == currFilterName	// And not already the filter
						||
						! currFilterName.equalsIgnoreCase( fieldID )
					)
				)
			{

			    makeReportLink(
			            outElem,
			    		inRequest,
			            inObjValue,
			            strValue,
			            wasNull,
			    		optFieldValueStrings
			    	);
			    

			}
			// Else it's just plain text
			else
			{
				outElem.addContent( strValue );
			}
		}
		// Else it is null, consider some warnings if appropriate
		else
		{
			if( getIsExternalLink() )
				warningMsg( kFName,
					"Unable to create external hyperlink with null text."
					);
					// Todo: add report name, etc.
			else if( getIsReportLink() && getDoLinkOnNull() )
				warningMsg( kFName,
					"Unable to create filter hyperlink with null text."
					);
					// Todo: add report name, etc.
		}

		// We're done
		return outElem;

	}

	// Generating a report link requires two steps:
	// 1: construct a general report link object
	// 2: ask it to generate a link for a specific value
	// When constructing a link, you need to pass that construtor
	// a config element.  This may be directly available, or you
	// may need to generate a temporary element from parameters.
	// There are several ways you can get the element or parameters:
	// 1: A field may have a <report_link> sub element
	// 2: The field def itself may have parameters that can be copied over
	// 3: We can mix in some of the parameters we were given (the field value)
	// 4: String values can have dollar signs, which indicate variable expansion
	void makeReportLink(
            Element inContentHanger,
    		AuxIOInfo inRequest,
    		Object inObjValue,
    		String inDisplayValue,
    		boolean inWasOrigNull,
    		Hashtable optFieldValueStrings
			)
	{
	    final String kFName = "makeReportLink";
	    			    
		boolean debug = shouldDoDebugMsg( kFName );

		String fieldID = getFieldIDOrNull();
		String report = getReportName();
		String heading = getHeadingOrNull();
		String fieldDesc =
				"Report=\"" + report
				+ "\", Field=\"" + fieldID
				+ "\"(\"" + heading + "\")"
				+ " value=\"" + inObjValue + "\""
				;


		if(debug) {
		    debugMsg( kFName, "================vvvvvv===========");
			debugMsg( kFName,
			        fieldDesc
			        + " inObjValue=" + inObjValue
			        + " inDisplayValue=" + inDisplayValue
			        + " inWasOrigNull=" + inWasOrigNull
			        + " optFieldValueStrings=" + optFieldValueStrings
			        );

		
		
		}

		// If no cached report link object
	    if( null==cReportLinkObj ) {

	        // See if a formal <report_link> element exists
	        // May or may not be null
	    	Element linkElem = getTrueLinkElem();
	    	// statusMsg( kFName, "linkElem=" + linkElem );

			// If it's just a quickie link generated by method args
			// so we need to generate a temp element for the constructor
			if( null==linkElem ) {
			
				// The report name to use
				// String lDestReport = null != getLinkedReportName()
				//	? getLinkedReportName() : report
				//	;
				String lDestReport = getLinkedReportName();
				// Figure out which parameter name to use
				// If not null, use it, otherwise, system null
				String parmName = null != getLinkedReportFilterField()
					? getLinkedReportFilterField() : fieldID
					;
			
				
				// Create a report link XML Element to pass
				// to it's contructor
				linkElem = new Element(
					ReportLink.MAIN_ELEM_NAME
					);
			
				// Set some attributes that we need
				// linkElem.setAttribute(
				//	ReportLink.LINK_TEXT_PATH, strValue
				//	);
				// ^^^ No, this is different for each row
			
				// Add the actual TEXT for this field
				// Element linkTextElem =
				//	JDOMHelper.findOrCreateElementByPath(
				//	    linkElem,
				//		ReportLink.LINK_TEXT_PATH,
				//		true
				//		);
				// linkTextElem.addContent( strValue );
			
				// Add a title, if there was one
				if( null != getLinkTitle() )
				{
					Element linkTitleElem =
						JDOMHelper.findOrCreateElementByPath(
						    linkElem,
							ReportLink.TITLE_PATH,
							true
							);
					linkTitleElem.addContent( getLinkTitle() );
				}
			
			
				// Create a parameter node and add it
				Element parmElem = new Element( ReportLink.PARM_PATH );
				linkElem.addContent( parmElem );
			
				parmElem.setAttribute(
					ReportLink.PARM_NAME_ATTR, parmName
					);

				// Also set the filter
				// linkElem.setAttribute(
				//        linkElem.PARM_NAME_ATTR, parmName
				//		);
			
				// Do NOT set parm value in the constructor since
				// we will reuse this link for many rows (different values)
				// parmElem.setAttribute(
				//	ReportLink.PARM_VALUE_ATTR, parmValue
				//	);
				
			}	// end If no link element
			
			// Now we copy over any additional attributes from the
			// field definition
			
			// Copy over attributes from the main field
			List srcAttrs = fMainElem.getAttributes();
			for( Iterator it = srcAttrs.iterator() ; it.hasNext() ; ) {
				Attribute newAttr = (Attribute) it.next();
				String attrName = newAttr.getName();
				Attribute oldAttr = linkElem.getAttribute( attrName );
				if( null==oldAttr )
					linkElem.setAttribute( attrName, newAttr.getValue() );
				else
					errorMsg( kFName,
						"Conflicting attribute names for main field and link element."
						+ " attr name =\"" + attrName + "\""
						+ ", link attr value =\"" + oldAttr.getValue() + "\""
						+ ", field attr value =\"" + newAttr.getValue() + "\""
						+ " Keeping more specific link attribute and IGNORING field attribute."
						);
			}
	
			
			// Construct the link
			try
			{
				cReportLinkObj = new ReportLink(
					linkElem, getMainConfig(), false
					);
			}
			catch( Exception e )
			{
				errorMsg( kFName,
					"Error preparing report link for "
					+ fieldDesc
					+ " (1). Will add UNlinked text instead."
					+ " Error: " + e
					);
				inContentHanger.addContent( inDisplayValue );
				return;
			}
	
		    
	    }	// End if no cached link

		// Figure out which value to use
		String parmValue = ! inWasOrigNull
			// ? strValue : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
			? ""+inObjValue : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
			;

	    Hashtable varHash = null;
		// Add variables, if needed
		if( cReportLinkObj.getAppearsToHaveDynamicVariables() ) {
			errorMsg( kFName,
					"Dynamic variables not currently supported for report field links."
					);
			inContentHanger.addContent( inDisplayValue );
			return;
		    /***
		    if( null==optFieldValueStrings ) {
				errorMsg( kFName,
					"Link settings indicated field variables, but none were passed in."
					);
			}
			else {
				varHash = new Hashtable();
				Hashtable rawStrings = new Hashtable();
				rawStrings.putAll( optFieldValueStrings );
				varHash.put( ReportLink.RAW_FIELDS_HASH_NAME, rawStrings );
			}
			***/
		}

		// generate the link
		try
		{
			// Request the anchor element
			Element anchor = cReportLinkObj.generateRichLink(
			        inRequest,
			        null,	// String optReportName,
			        null,	// String optFilterName,
			        false,	// boolean inIsMenuLink,
			        parmValue,	// String optNewParmValue,
			        optFieldValueStrings,
			        inDisplayValue		// String optNewLinkTextValue
			        );
				
			// Add it to the answer
			inContentHanger.addContent( anchor );
		}
		catch( Exception e )
		{
			errorMsg( kFName,
				"Error preparing report link for "
				+ fieldDesc
				+ " (1). Will add UNlinked text instead."
				+ " Error: " + e
				);
			inContentHanger.addContent( inDisplayValue );
		}
	    					
		// statusMsg( kFName, "================^^^^^^===========");

	}

	
	
	
	// It's perfectly normally to pass in a null!
	// this is NOT an error condition
	public Element _generateDataElement_v0(
		Object inObjValue,
		AuxIOInfo inRequest,
		Hashtable optFieldValueStrings
		)
	{
		final String kFName = "generateDataElement";

		boolean debug = shouldDoDebugMsg( kFName );

		String report = getReportName();
		String fieldID = getFieldIDOrNull();

		String heading = getHeadingOrNull();

		String fieldDesc =
				"Report: \"" + report
				+ "\", Field: \"" + fieldID
				+ "\" / \"" + heading + "\""
				+ " = \"" + inObjValue + "\""
				;


		// Complain if we're not supposed to display this
		if( ! getShouldDisplay() )
		{
			errorMsg( kFName,
				"This field is not supposed to be displayed. "
				+ fieldDesc
				+ " Returning null element."
				);
			return null;
		}

		if(debug)
			debugMsg( kFName,	fieldDesc );


		// Get the value for that field
		String strValue = null;
		boolean wasNull = true;
		boolean isDecode = cHasDecode;
		boolean isNumeric = getIsExplcitlyNumeric()
			|| ( NIEUtil.isNumericType( inObjValue ) && ! getIsExplcitlyDate() );
		boolean isPercentage = getExplicitGeneralFormat() != null &&
			getExplicitGeneralFormat().equals(FORMAT_AS_PERCENT);
		boolean isBoolean = getExplicitGeneralFormat() != null && getExplicitGeneralFormat().equals(FORMAT_AS_BOOLEAN);
		boolean isDate = getIsExplcitlyDate() || NIEUtil.isDateType( inObjValue );
		if(debug)
			debugMsg( kFName,
				"format=" + getExplicitGeneralFormat()
				+ ", isNumeric=" + isNumeric
				+ ", isBoolean=" + isBoolean
				);

		if( null != inObjValue )
		{
			// Check decode first
			if( isDecode ) {
				if( null!=inObjValue )
					strValue = doDecode( inObjValue );
			}
			// Check boolean before number
			else if( isBoolean )
			{
				if( null!=inObjValue )
					strValue = formatBooleanTypeToString( inObjValue, true );
			}
			else if( isNumeric )
			{
				if( null!=inObjValue )
					strValue = formatNumericTypeToString( inObjValue, true );
			}
			else if( isDate )
			{
				if( null!=inObjValue )
					strValue = formatDateTypeToString( inObjValue, true );
			}
			else {
				strValue = inObjValue.toString();
				if(debug)
					debugMsg( kFName,
						"Is a " + inObjValue.getClass().getName()
						);
			}

			// call me paranoid
			if( null!=strValue )
				wasNull = false;
		}
		else {
			debugMsg( kFName, "Was null" );
		}

		// If it's still null, see if there was something assigned
		// It may still be null after this of course
		if( null == strValue )
		{
			wasNull = true;

			// which may still be null
			strValue = getNullValueDisplay();

			// We can sometimes have a second chance at formatting
			// the null value, however, we don't if decode was used
			if( ! isDecode ) {
				// See if we want to format this null value?
				// check for boolean first
				if( null!=strValue && isBoolean )
				{
					String newStrValue = formatBooleanTypeToString( strValue, false );
					if( null!=newStrValue ) {
						strValue = newStrValue;
						// We need to know whether or not to add the prefix / suffix
						wasNull = false;
					}
				}
				// We might have set some type of numeric null value
				// which we would like to have diplayed
				else if( null!=strValue && isNumeric )
				{
					String newStrValue = formatNumericTypeToString( strValue, false );
					if( null!=newStrValue ) {
						strValue = newStrValue;
						// We need to know whether or not to add the prefix / suffix
						wasNull = false;
					}
				}
				else if( null!=strValue && isDate )
				{
					String newStrValue = formatDateTypeToString( strValue, false );
					if( null!=newStrValue ) {
						strValue = newStrValue;
						// We need to know whether or not to add the prefix / suffix
						wasNull = false;
					}
				}
			}

			// We DON'T change the wasNull flag here,
			// if it was ORIGINALLY null, then that's the
			// logic we will follow, even if we decide to
			// display something
		}

		// Add any prefix or suffix text that might have been requested
		// We only do this if we had a reasonable chance of a non-standlone null token
		// In other words, don't add $ or % to just a null marker of -
		if( ! wasNull ) {
			String prefix = getPrefix();
			String suffix = getSuffix();
			if( null!=prefix || null!=suffix ) {
				StringBuffer tmpBuff = new StringBuffer();
				if( null!=prefix )
					tmpBuff.append(prefix);
				if( null!=strValue )
					tmpBuff.append(strValue);
				if( null!=suffix )
					tmpBuff.append(suffix);
				strValue = new String( tmpBuff );
			}
		}

		// Get a CSS class
		String cssClass = getCssDataClassOrNull();
		if( null==cssClass ) {
			// if( isBoolean )
			if( isPercentage )
				cssClass = CSSClassNames.PERCENTAGE_CELL;
			else if( isNumeric )
				cssClass = CSSClassNames.NUMERIC_CELL;
			else if( isDate )
				cssClass = CSSClassNames.DATETIME_CELL;
			else
				cssClass = CSSClassNames.DATA_CELL;
			// TODO: Add support for boolean, etc.
		}

		// Start building the output element
		Element outElem = new Element( "td" );
		outElem.setAttribute( "class", cssClass );
		// If there's text, add it
		// Note that we ALWAYS create a cell, otherwise
		// columns would be messed up
		if( null != strValue )
		{
			/***
			statusMsg( kFName,
				"field=" + fieldID
				+ " getIsExternalLink()=" + getIsExternalLink()
				);
			***/

			// The name of the current filter that has been
			// invoked, if any
			String currFilterName = inRequest.getScalarCGIFieldTrimOrNull(
				ReportConstants.FILTER_NAME_CGI_FIELD_NAME
				);
			if(debug)
				debugMsg( kFName,
					"fieldDesc=" + fieldDesc
					+ " currFilterName=" + currFilterName
					);

			// If it's an external link
			if( getIsExternalLink()
					&&
					( 	! wasNull
						||
						getDoLinkOnNull()
					)
				)
			{
				Element a = new Element( "a" );
				a.setAttribute( "href", strValue );
				a.setAttribute( "target", getLinkTarget() );
				a.setAttribute(
					"class", CSSClassNames.ACTIVE_RPT_LINK
					);
				a.addContent( strValue );
				outElem.addContent( a );
			}
			// Else if it's a filter field
			// AND it's not null, or is null but they say that's OK
			else if(
					getIsReportLink()	// It is a link
					&&
					( 	! wasNull			// And this value warrents it
						||
						getDoLinkOnNull()
					)
					&&
					(	null == currFilterName	// And not already the filter
						||
						! currFilterName.equalsIgnoreCase( fieldID )
					)
				)
			{

				// optFieldValueStrings

				// May or may not be null
				Element linkElem = getTrueLinkElem();

				// If it's just a quickie link
				if( null==linkElem /*&& (null==strValue || strValue.indexOf('$') < 0)*/ ) {

					Element constructorElem = null;
					// The report name to use
					String lDestReport = null != getLinkedReportName()
						? getLinkedReportName() : report
						;
					// Figure out which parameter name to use
					// If not null, use it, otherwise, system null
					String parmName = null != getLinkedReportFilterField()
						? getLinkedReportFilterField() : fieldID
						;
					// Figure out which value to use
					String parmValue = ! wasNull
						? strValue : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
						;
	
					// If no cached report link object
					if( null==cReportLinkObj ) {

						// Create a report link XML Element to pass
						// to it's contructor
						constructorElem = new Element(
							ReportLink.MAIN_ELEM_NAME
							);
		
						// Set some attributes that we need
						constructorElem.setAttribute(
							ReportLink.LINK_TEXT_PATH, strValue
							);
		
						// Add the actual TEXT for this field
						Element linkTextElem =
							JDOMHelper.findOrCreateElementByPath(
								constructorElem,
								ReportLink.LINK_TEXT_PATH,
								true
								);
						linkTextElem.addContent( strValue );
		
						// Add a title, if there was one
						if( null != getLinkTitle() )
						{
							Element linkTitleElem =
								JDOMHelper.findOrCreateElementByPath(
									constructorElem,
									ReportLink.TITLE_PATH,
									true
									);
							linkTitleElem.addContent( getLinkTitle() );
						}
		
		
						// Create a parameter node and add it
						Element parmElem = new Element( ReportLink.PARM_PATH );
						constructorElem.addContent( parmElem );
				
						parmElem.setAttribute(
							ReportLink.PARM_NAME_ATTR, parmName
							);
	
						parmElem.setAttribute(
							ReportLink.PARM_VALUE_ATTR, parmValue
							);
		
					}	// end If no cached report link object
	
					// Construct the link
					try
					{
						// Todo: ??? pass in values to clear
	
						// Create the object
						if( null==cReportLinkObj )
							cReportLinkObj = new ReportLink(
								// constructorElem, getMainApp()
								constructorElem, getMainConfig()
								);
						// Request the anchor element
						Element anchor = cReportLinkObj.generateRichLink(
							inRequest, lDestReport, parmName, false, parmValue, null, strValue
							);
						// Add it to the answer
						outElem.addContent( anchor );
					}
					catch( Exception e )
					{
						errorMsg( kFName,
							"Error preparing report link for "
							+ fieldDesc
							+ " (1). Will add UNlinked text instead."
							+ " Error: " + e
							);
						outElem.addContent( strValue );
					}

				}
				// Else it's a fancy link!
				else {

					//	optFieldValueStrings
					//	linkElem
					//	ReportLink.PARM_PATH
					//	// Figure out which value to use
					//	String parmValue = ! wasNull
					//		? strValue : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
					//
					//	ReportLink.PARM_VALUE_ATTR


					// If no cached report link object
					if( null==cReportLinkObj ) {
						// Copy over attributes from the main field
						List srcAttrs = fMainElem.getAttributes();
						for( Iterator it = srcAttrs.iterator() ; it.hasNext() ; ) {
							Attribute newAttr = (Attribute) it.next();
							String attrName = newAttr.getName();
							Attribute oldAttr = linkElem.getAttribute( attrName );
							if( null==oldAttr )
								linkElem.setAttribute( attrName, newAttr.getValue() );
							else
								errorMsg( kFName,
									"Conflicting attribute names for main field and link element."
									+ " attr name =\"" + attrName + "\""
									+ ", link attr value =\"" + oldAttr.getValue() + "\""
									+ ", field attr value =\"" + newAttr.getValue() + "\""
									+ " Keeping more specific link attribute and IGNORING field attribute."
									);
						}
					}

					// Construct the link
					try
					{
						// Todo: ??? pass in values to clear
	
						// Create the object
						// If no cached report link object
						if( null==cReportLinkObj )
							cReportLinkObj = new ReportLink(
								linkElem, getMainConfig(), false
								);

						Hashtable varHash = null;
						// Add variables, if needed
						if( cReportLinkObj.getAppearsToHaveDynamicVariables() ) {
							if( null==optFieldValueStrings ) {
								errorMsg( kFName,
									"Link settings indicated field variables, but none were passed in."
									);
							}
							else {
								varHash = new Hashtable();
								Hashtable rawStrings = new Hashtable();
								rawStrings.putAll( optFieldValueStrings );
								varHash.put( ReportLink.RAW_FIELDS_HASH_NAME, rawStrings );
							}
						}


						// Request the anchor element
						Element anchor = cReportLinkObj.generateRichLink(
							inRequest, null, null, false, null, varHash, strValue
							);
						// Add it to the answer
						outElem.addContent( anchor );
					}
					catch( Exception e )
					{
						errorMsg( kFName,
							"Error preparing report link for "
							+ fieldDesc
							+ "(2). Will add UNlinked text instead."
							+ " Error: " + e
							);
						outElem.addContent( strValue );
					}


				}	// End else it's a fancy link

			}
			// Else it's just plain text
			else
			{
				outElem.addContent( strValue );
			}
		}
		// Else it is null, consider some warnings if appropriate
		else
		{
			if( getIsExternalLink() )
				warningMsg( kFName,
					"Unable to create external hyperlink with null text."
					);
					// Todo: add report name, etc.
			else if( getIsReportLink() && getDoLinkOnNull() )
				warningMsg( kFName,
					"Unable to create filter hyperlink with null text."
					);
					// Todo: add report name, etc.
		}

		// We're done
		return outElem;

	}


	// It's perfectly normally to pass in a null!
	// this is NOT an error condition
	public Element _generateDataElement_v1x(
		Object inObjValue,
		AuxIOInfo inRequest,
		Hashtable optFieldValueStrings
		)
	{
		final String kFName = "generateDataElement";

		boolean debug = shouldDoDebugMsg( kFName );

		String report = getReportName();
		String fieldID = getFieldIDOrNull();

		String heading = getHeadingOrNull();

		String fieldDesc =
				"Report: \"" + report
				+ "\", Field: \"" + fieldID
				+ "\" / \"" + heading + "\""
				+ " = \"" + inObjValue + "\""
				;


		// Complain if we're not supposed to display this
		if( ! getShouldDisplay() )
		{
			errorMsg( kFName,
				"This field is not supposed to be displayed. "
				+ fieldDesc
				+ " Returning null element."
				);
			return null;
		}

		if(debug)
			debugMsg( kFName,	fieldDesc );


		// Get the value for that field
		String strValue = null;
		boolean wasNull = true;
		boolean isDecode = cHasDecode;
		boolean isNumeric = getIsExplcitlyNumeric()
			|| ( NIEUtil.isNumericType( inObjValue ) && ! getIsExplcitlyDate() );
		boolean isPercentage = getExplicitGeneralFormat() != null &&
			getExplicitGeneralFormat().equals(FORMAT_AS_PERCENT);
		boolean isBoolean = getExplicitGeneralFormat() != null && getExplicitGeneralFormat().equals(FORMAT_AS_BOOLEAN);
		boolean isDate = getIsExplcitlyDate() || NIEUtil.isDateType( inObjValue );
		if(debug)
			debugMsg( kFName,
				"format=" + getExplicitGeneralFormat()
				+ ", isNumeric=" + isNumeric
				+ ", isBoolean=" + isBoolean
				);

		if( null != inObjValue )
		{
			// Check decode first
			if( isDecode ) {
				if( null!=inObjValue )
					strValue = doDecode( inObjValue );
			}
			// Check boolean before number
			else if( isBoolean )
			{
				if( null!=inObjValue )
					strValue = formatBooleanTypeToString( inObjValue, true );
			}
			else if( isNumeric )
			{
				if( null!=inObjValue )
					strValue = formatNumericTypeToString( inObjValue, true );
			}
			else if( isDate )
			{
				if( null!=inObjValue )
					strValue = formatDateTypeToString( inObjValue, true );
			}
			else {
				strValue = inObjValue.toString();
				if(debug)
					debugMsg( kFName,
						"Is a " + inObjValue.getClass().getName()
						);
			}

			// call me paranoid
			if( null!=strValue )
				wasNull = false;
		}
		else {
			debugMsg( kFName, "Was null" );
		}

		// If it's still null, see if there was something assigned
		// It may still be null after this of course
		if( null == strValue )
		{
			wasNull = true;

			// which may still be null
			strValue = getNullValueDisplay();

			// We can sometimes have a second chance at formatting
			// the null value, however, we don't if decode was used
			if( ! isDecode ) {
				// See if we want to format this null value?
				// check for boolean first
				if( null!=strValue && isBoolean )
				{
					String newStrValue = formatBooleanTypeToString( strValue, false );
					if( null!=newStrValue ) {
						strValue = newStrValue;
						// We need to know whether or not to add the prefix / suffix
						wasNull = false;
					}
				}
				// We might have set some type of numeric null value
				// which we would like to have diplayed
				else if( null!=strValue && isNumeric )
				{
					String newStrValue = formatNumericTypeToString( strValue, false );
					if( null!=newStrValue ) {
						strValue = newStrValue;
						// We need to know whether or not to add the prefix / suffix
						wasNull = false;
					}
				}
				else if( null!=strValue && isDate )
				{
					String newStrValue = formatDateTypeToString( strValue, false );
					if( null!=newStrValue ) {
						strValue = newStrValue;
						// We need to know whether or not to add the prefix / suffix
						wasNull = false;
					}
				}
			}

			// We DON'T change the wasNull flag here,
			// if it was ORIGINALLY null, then that's the
			// logic we will follow, even if we decide to
			// display something
		}

		// Add any prefix or suffix text that might have been requested
		// We only do this if we had a reasonable chance of a non-standlone null token
		// In other words, don't add $ or % to just a null marker of -
		if( ! wasNull ) {
			String prefix = getPrefix();
			String suffix = getSuffix();
			if( null!=prefix || null!=suffix ) {
				StringBuffer tmpBuff = new StringBuffer();
				if( null!=prefix )
					tmpBuff.append(prefix);
				if( null!=strValue )
					tmpBuff.append(strValue);
				if( null!=suffix )
					tmpBuff.append(suffix);
				strValue = new String( tmpBuff );
			}
		}

		// Get a CSS class
		String cssClass = getCssDataClassOrNull();
		if( null==cssClass ) {
			// if( isBoolean )
			if( isPercentage )
				cssClass = CSSClassNames.PERCENTAGE_CELL;
			else if( isNumeric )
				cssClass = CSSClassNames.NUMERIC_CELL;
			else if( isDate )
				cssClass = CSSClassNames.DATETIME_CELL;
			else
				cssClass = CSSClassNames.DATA_CELL;
			// TODO: Add support for boolean, etc.
		}

		// Start building the output element
		Element outElem = new Element( "td" );
		outElem.setAttribute( "class", cssClass );
		// If there's text, add it
		// Note that we ALWAYS create a cell, otherwise
		// columns would be messed up
		if( null != strValue )
		{
			/***
			statusMsg( kFName,
				"field=" + fieldID
				+ " getIsExternalLink()=" + getIsExternalLink()
				);
			***/

			// The name of the current filter that has been
			// invoked, if any
			String currFilterName = inRequest.getScalarCGIFieldTrimOrNull(
				ReportConstants.FILTER_NAME_CGI_FIELD_NAME
				);
			if(debug)
				debugMsg( kFName,
					"fieldDesc=" + fieldDesc
					+ " currFilterName=" + currFilterName
					);

			// If it's an external link
			if( getIsExternalLink()
					&&
					( 	! wasNull
						||
						getDoLinkOnNull()
					)
				)
			{
				Element a = new Element( "a" );
				a.setAttribute( "href", strValue );
				a.setAttribute( "target", getLinkTarget() );
				a.setAttribute(
					"class", CSSClassNames.ACTIVE_RPT_LINK
					);
				a.addContent( strValue );
				outElem.addContent( a );
			}
			// Else if it's a filter field
			// AND it's not null, or is null but they say that's OK
			else if(
					getIsReportLink()	// It is a link
					&&
					( 	! wasNull			// And this value warrents it
						||
						getDoLinkOnNull()
					)
					&&
					(	null == currFilterName	// And not already the filter
						||
						! currFilterName.equalsIgnoreCase( fieldID )
					)
				)
			{

			    // statusMsg( kFName, "================vvvvvv===========");
			    
			    // optFieldValueStrings

				// May or may not be null
				Element linkElem = getTrueLinkElem();
				// statusMsg( kFName, "linkElem=" + linkElem );

				// If it's just a quickie link
				if( null==linkElem /*&& (null==strValue || strValue.indexOf('$') < 0)*/
				) {
				    // xyz

					Element constructorElem = null;
					// The report name to use
					String lDestReport = null != getLinkedReportName()
						? getLinkedReportName() : report
						;
					// Figure out which parameter name to use
					// If not null, use it, otherwise, system null
					String parmName = null != getLinkedReportFilterField()
						? getLinkedReportFilterField() : fieldID
						;
					// Figure out which value to use
					String parmValue = ! wasNull
						? strValue : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
						;
	
					// // If no cached report link object
					if( null==cReportLinkObj ) {
					// ^^^^ BROKEN???
					// uses the same link parameter for all links
					// Bug found by Miles
					
						// Create a report link XML Element to pass
						// to it's contructor
						constructorElem = new Element(
							ReportLink.MAIN_ELEM_NAME
							);
		
						// Set some attributes that we need
						constructorElem.setAttribute(
							ReportLink.LINK_TEXT_PATH, strValue
							);
		
						// Add the actual TEXT for this field
						Element linkTextElem =
							JDOMHelper.findOrCreateElementByPath(
								constructorElem,
								ReportLink.LINK_TEXT_PATH,
								true
								);
						linkTextElem.addContent( strValue );
		
						// Add a title, if there was one
						if( null != getLinkTitle() )
						{
							Element linkTitleElem =
								JDOMHelper.findOrCreateElementByPath(
									constructorElem,
									ReportLink.TITLE_PATH,
									true
									);
							linkTitleElem.addContent( getLinkTitle() );
						}
		
		
						// Create a parameter node and add it
						Element parmElem = new Element( ReportLink.PARM_PATH );
						constructorElem.addContent( parmElem );
				
						parmElem.setAttribute(
							ReportLink.PARM_NAME_ATTR, parmName
							);
	
						// parmElem.setAttribute(
						//	ReportLink.PARM_VALUE_ATTR, parmValue
						//	);
		
					}	// end If no cached report link object
	
					// Construct the link
					try
					{
						// Todo: ??? pass in values to clear
	
						// Create the object
						// if( null==cReportLinkObj )
							cReportLinkObj = new ReportLink(
								// constructorElem, getMainApp()
								constructorElem, getMainConfig()
								);
						// Request the anchor element
						Element anchor = cReportLinkObj.generateRichLink(
							inRequest, lDestReport, parmName, false,
							parmValue, null, strValue
							);
						// Add it to the answer
						outElem.addContent( anchor );
					}
					catch( Exception e )
					{
						errorMsg( kFName,
							"Error preparing report link for "
							+ fieldDesc
							+ " (1). Will add UNlinked text instead."
							+ " Error: " + e
							);
						outElem.addContent( strValue );
					}

				}
				// Else it's a fancy link!
				else {

					//	optFieldValueStrings
					//	linkElem
					//	ReportLink.PARM_PATH
					//	// Figure out which value to use
					//	String parmValue = ! wasNull
					//		? strValue : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
					//
					//	ReportLink.PARM_VALUE_ATTR


					// If no cached report link object
					if( null==cReportLinkObj ) {
						// Copy over attributes from the main field
						List srcAttrs = fMainElem.getAttributes();
						for( Iterator it = srcAttrs.iterator() ; it.hasNext() ; ) {
							Attribute newAttr = (Attribute) it.next();
							String attrName = newAttr.getName();
							Attribute oldAttr = linkElem.getAttribute( attrName );
							if( null==oldAttr )
								linkElem.setAttribute( attrName, newAttr.getValue() );
							else
								errorMsg( kFName,
									"Conflicting attribute names for main field and link element."
									+ " attr name =\"" + attrName + "\""
									+ ", link attr value =\"" + oldAttr.getValue() + "\""
									+ ", field attr value =\"" + newAttr.getValue() + "\""
									+ " Keeping more specific link attribute and IGNORING field attribute."
									);
						}
					}

					// Construct the link
					try
					{
						// Todo: ??? pass in values to clear
	
						// Create the object
						// If no cached report link object
						if( null==cReportLinkObj )
							cReportLinkObj = new ReportLink(
								linkElem, getMainConfig(), false
								);

						Hashtable varHash = null;
						// Add variables, if needed
						if( cReportLinkObj.getAppearsToHaveDynamicVariables() ) {
							if( null==optFieldValueStrings ) {
								errorMsg( kFName,
									"Link settings indicated field variables, but none were passed in."
									);
							}
							else {
								varHash = new Hashtable();
								Hashtable rawStrings = new Hashtable();
								rawStrings.putAll( optFieldValueStrings );
								varHash.put( ReportLink.RAW_FIELDS_HASH_NAME, rawStrings );
							}
						}


						// Request the anchor element
						Element anchor = cReportLinkObj.generateRichLink(
							inRequest, null, null, false, null, varHash, strValue
							);
						// Add it to the answer
						outElem.addContent( anchor );
					}
					catch( Exception e )
					{
						errorMsg( kFName,
							"Error preparing report link for "
							+ fieldDesc
							+ "(2). Will add UNlinked text instead."
							+ " Error: " + e
							);
						outElem.addContent( strValue );
					}


				}	// End else it's a fancy link

				// statusMsg( kFName, "================^^^^^^===========");

			}
			// Else it's just plain text
			else
			{
				outElem.addContent( strValue );
			}
		}
		// Else it is null, consider some warnings if appropriate
		else
		{
			if( getIsExternalLink() )
				warningMsg( kFName,
					"Unable to create external hyperlink with null text."
					);
					// Todo: add report name, etc.
			else if( getIsReportLink() && getDoLinkOnNull() )
				warningMsg( kFName,
					"Unable to create filter hyperlink with null text."
					);
					// Todo: add report name, etc.
		}

		// We're done
		return outElem;

	}



	String formatNumericTypeToString( Object inObject, boolean inComplainOnError )
	{
		final String kFName = "formatNumericTypeToString";

		Double dObj = NIEUtil.objectToDoubleOrNull(inObject, false, inComplainOnError );
		if( null==dObj ) {
			if( inComplainOnError )
				errorMsg( kFName, "Unable to convert to number, object=\"" + inObject + "\"");
			return null;
		}
		double d = dObj.doubleValue();
		// If percentage, multiply by 100.0
		String tmpFormat = getExplicitGeneralFormat();
		if( null!=tmpFormat && tmpFormat.equals(FORMAT_AS_PERCENT) )
			d = d * 100.0;
		int maxDec = getMaxDecPlaces( d );
		if( maxDec >= 0 ) {
			d = NIEUtil.round( d, maxDec );
		}
		String outStr = ""+d;
		int minDec = getMinDecPlaces();
		// Take our default action?
		if( minDec < 0 ) {
			// By default we drop trailing .0
			if( outStr.endsWith(".0") )
				outStr = outStr.substring( 0, outStr.length()-2 );
		}
		// oddball case if you set it to exactly 0, means you want . at the end
		else if( 0 == minDec ) {
			if( outStr.endsWith(".0") )
				outStr = outStr.substring( 0, outStr.length()-1 );
			else if( outStr.lastIndexOf('.') < 0 )
				outStr += '.';
		}
		// we do want trailing zeros
		else {
			int dotAt = outStr.lastIndexOf('.');
			if( dotAt < 0 ) {
				outStr += '.';
				dotAt = outStr.length()-1;
			}
			int padCount = minDec - ( outStr.length() - dotAt - 1 );
			for( int i=0; i<padCount ; i++ )
				outStr += '0';
		}

		// Check for comma formatting, per miles
		if( K_ADD_COMMAS ) {
			// TODO: Java has this as well, but we have additional options...
			String mainNumber = null;
			String prefix = null;
			String suffix = null;
			int dotAt = outStr.indexOf('.');
			if( dotAt > 0 ) {
				mainNumber = outStr.substring( 0, dotAt );
				suffix = outStr.substring( dotAt );
			}
			else {
				mainNumber = outStr;
			}
			if( mainNumber.startsWith("-") ) {
				mainNumber = mainNumber.substring( 1 );
				prefix = "-";
			}
			if( mainNumber.length() > 3 ) {
				char [] digits = mainNumber.toCharArray();
				StringBuffer buff = new StringBuffer();
				int origLen = digits.length;
				for( int i=0 ; i<origLen ; i++ ) {
					buff.append( digits[i] );
					if( i<origLen-1 && (origLen-i-1) % 3 == 0 )
						buff.append( ',' );
				}
				if( null!=prefix )
					buff.insert( 0, prefix );
				if( null!=suffix )
					buff.append( suffix );
				outStr = new String( buff );
			}
		}

		return outStr;
	}

	public static String formatNumericTypeToString_static(
		Object inObject, boolean inComplainOnError,
		String optExplicitFormat,
		int optMinDec, int optMaxDec
		)
	{
		final String kFName = "formatNumericTypeToString_static";

		Double dObj = NIEUtil.objectToDoubleOrNull(inObject, false, inComplainOnError );
		if( null==dObj ) {
			if( inComplainOnError )
				errorMsg( kFName, "Unable to convert to number, object=\"" + inObject + "\"");
			return null;
		}
		double d = dObj.doubleValue();
		// If percentage, multiply by 100.0
		if( null!=optExplicitFormat && optExplicitFormat.equals(FORMAT_AS_PERCENT) )
			d = d * 100.0;
		int maxDec = optMaxDec;
		if( maxDec < 0 )
			maxDec = getDefaultMaxDecPlaces( optExplicitFormat, d );
		if( maxDec >= 0 ) {
			d = NIEUtil.round( d, maxDec );
		}
		String outStr = ""+d;
		int minDec = optMinDec;
		// By default it is -1
		if( minDec < 0 )
			minDec = getDefaultMinDecPlaces( optExplicitFormat );
		// Take our default action?
		if( minDec < 0 ) {
			// By default we drop trailing .0
			if( outStr.endsWith(".0") )
				outStr = outStr.substring( 0, outStr.length()-2 );
		}
		// oddball case if you set it to exactly 0, means you want . at the end
		else if( 0 == minDec ) {
			if( outStr.endsWith(".0") )
				outStr = outStr.substring( 0, outStr.length()-1 );
			else if( outStr.lastIndexOf('.') < 0 )
				outStr += '.';
		}
		// we do want trailing zeros
		else {
			int dotAt = outStr.lastIndexOf('.');
			if( dotAt < 0 ) {
				outStr += '.';
				dotAt = outStr.length()-1;
			}
			int padCount = minDec - ( outStr.length() - dotAt - 1 );
			for( int i=0; i<padCount ; i++ )
				outStr += '0';
		}
		String suffix = getDefaultSuffix( optExplicitFormat );
		if( null!=suffix )
			outStr += suffix;


		// Check for comma formatting, per miles
		if( K_ADD_COMMAS ) {
			// TODO: Java has this as well, but we have additional options...
			// traceMsg( kFName, "outStr = " + outStr );

			String mainNumber = null;
			String prefix = null;
			String suffix2 = null;
			int dotAt = outStr.indexOf('.');
			if( dotAt > 0 ) {
				mainNumber = outStr.substring( 0, dotAt );
				suffix2 = outStr.substring( dotAt );
			}
			else {
				mainNumber = outStr;
			}
			if( mainNumber.startsWith("-") ) {
				mainNumber = mainNumber.substring( 1 );
				prefix = "-";
			}
			// traceMsg( kFName, "Number = " + mainNumber );
			if( mainNumber.length() > 3 ) {
				char [] digits = mainNumber.toCharArray();
				StringBuffer buff = new StringBuffer();
				int origLen = digits.length;
				for( int i=0 ; i<origLen ; i++ ) {
					buff.append( digits[i] );
					if( i<origLen-1 && (origLen-i-1) % 3 == 0 )
						buff.append( ',' );
				}
				if( null!=prefix )
					buff.insert( 0, prefix );
				if( null!=suffix2 )
					buff.append( suffix2 );
				outStr = new String( buff );
			}
		}


		return outStr;
	}

	String formatBooleanTypeToString( Object inObject, boolean inComplainOnError  )
	{
		final String kFName = "formatBooleanTypeToString";
		Double dObj = NIEUtil.objectToDoubleOrNull(inObject, false, false );
		if( null==dObj ) {
			if( inComplainOnError )
				errorMsg( kFName, "Unable to convert to Boolean, object=\"" + inObject + "\"");
			return null;
		}
		double d = dObj.doubleValue();
		String outStr = d!=0.0 ? DEFAULT_BOOLEAN_TRUE_STRING : DEFAULT_BOOLEAN_FALSE_STRING;
		return outStr;
	}

	String formatDateTypeToString( Object inObject, boolean inComplainOnError )
	{
		final String kFName = "formatDateTypeToString";
		Date dObj = NIEUtil.objectToDateOrNull(
			inObject, false, inComplainOnError,
			getDBConfig().getVendorEpochCorrectionInMS()
			);
		if( null==dObj )
		if( null==dObj ) {
			if( inComplainOnError )
				errorMsg( kFName, "Unable to convert to date/time, object=\"" + inObject + "\"");
			return null;
		}

		String formatStr = getDateFormat();
		if( null==formatStr ) {
			if( inComplainOnError )
				errorMsg( kFName, "No format string to use to convert to date/time, object=\"" + inObject + "\"");
			return null;
		}

		String outStr = NIEUtil.formatDateToString( dObj, formatStr, inComplainOnError );

		return outStr;
	}


	String doDecode( Object inObject )
	{
		String key = inObject.toString();
		if( cDecodeHash.containsKey(key) )
			return (String)cDecodeHash.get(key);
		else
			return cDecodeDefaultValue;
	}


	// [ sum, count, min, max ]
	public static double [] initStats() {
		double [] answer = new double[4];
		answer[2] = Double.POSITIVE_INFINITY;
		answer[3] = Double.NEGATIVE_INFINITY;
		return answer;
	}

	public double [] tabulateStats(
		Object inObjValue,
		AuxIOInfo inRequest,
		double [] optExistingStats
		)
	{
		final String kFName = "tabulateStats";

		boolean debug = shouldDoDebugMsg( kFName );

		String report = getReportName();
		String fieldID = getFieldIDOrNull();
		String heading = getHeadingOrNull();

		String fieldDesc =
			"Report: \"" + report
			+ "\", Field: \"" + fieldID
			+ "\" / \"" + heading + "\""
			;

		// Our stats array
		double [] answer = optExistingStats;
		if( null==answer )
			answer = initStats();

		// Convert the object to a number, complain about errors (but not nulls)
		Double newValue = NIEUtil.objectToDoubleOrNull( inObjValue, false, true );
		// Give it a resonable default if null (which may also be null!)
		if( null==inObjValue )
			newValue = getNullStatsValueOrNull();

		// At this point we either have a legit double, or a reasonable default, or null

		// Tabulate if not null
		if( null!=newValue ) {
			double dValue = newValue.doubleValue();
			// sum
			answer[0] += dValue;
			// count
			answer[1] += 1.0;
			// min
			if( dValue < answer[2] )
				answer[2] = dValue;
			// max
			if( dValue > answer[3] )
				answer[3] = dValue;

			debugMsg( kFName, "Tabulated " + dValue + " into " + answer );
		}
		else
			debugMsg( kFName, "No value for " + inObjValue );

		return answer;
	}







	// We generate an element whether or not this field has statistics
	// If it doesn't have statistics, we still generate a placeholder cell
	// Order of display (for any that are present):
	// sum / avg / min / max / count
	// Stats array is in:
	// sum / count / min / max
	public Element generateStatsElement(
		AuxIOInfo inRequest,
		double [] inExistingStats
		)
	{

		final String kFName = "generateStatsElement";

		boolean debug = shouldDoDebugMsg( kFName );

		String report = getReportName();
		String fieldID = getFieldIDOrNull();
		String heading = getHeadingOrNull();

		String fieldDesc =
				"Report: \""
				+ report
				+ "\", Field: \""
				+ fieldID
				+ "\" / \""
				+ heading
				+ "\""
				;

		// Complain if we're not supposed to display this
		if( ! getShouldDisplay() )
		{
			errorMsg( kFName,
				"This field is not supposed to be displayed. "
				+ fieldDesc
				+ " Returning null element."
				);
			return null;
		}



		// Start building the output element
		Element outElem = new Element( "td" );
		// Do the statistics, if asked to do so
		if( getShouldDoStatistics() ) {

			outElem.setAttribute(
				"class",
				CSSClassNames.NUMERIC_CELL
				// getCssDataClass()
				);


			boolean alreadyHasContent = false;


			if( getShouldCalculateTotal() ) {
				String tmpStr = ""+inExistingStats[0];
				if( tmpStr.endsWith(".0") )
					tmpStr = tmpStr.substring(0, tmpStr.length()-2 );
				outElem.addContent( tmpStr );
				alreadyHasContent = true;
			}


			// Average
			// Be VERY careful about /0 divide by zero, exceptions and overflows
			if( getShouldCalculateAverage() ) {
				double numerator = inExistingStats[0];
				double denominator = inExistingStats[1];
				double quotient = Double.POSITIVE_INFINITY;
				if( 0.0 != denominator ) {
					try {
						quotient = numerator / denominator;
					}
					catch( Throwable e ) {
						quotient = Double.POSITIVE_INFINITY;
					}
				}
				String tmpStr = null;
				if( quotient == Double.POSITIVE_INFINITY || quotient == Double.NEGATIVE_INFINITY )
					tmpStr = getNullValueDisplay();
				else {
					tmpStr = ""+quotient;
					if( tmpStr.endsWith(".0") )
						tmpStr = tmpStr.substring(0, tmpStr.length()-2 );
				}
				if( null!=tmpStr ) {
					// if( outElem.hasChildren() )
					if( alreadyHasContent )
						outElem.addContent( new Element("br") );
					outElem.addContent( tmpStr );
					alreadyHasContent = true;
				}
			}

			if( getShouldCalculateMin() ) {
				double tmpD = inExistingStats[2];
				String tmpStr = null;
				if( tmpD == Double.POSITIVE_INFINITY )
					tmpStr = getNullValueDisplay();
				else {
					tmpStr = ""+tmpD;
					if( tmpStr.endsWith(".0") )
						tmpStr = tmpStr.substring(0, tmpStr.length()-2 );
				}
				if( null!=tmpStr ) {
					// if( outElem.hasChildren() )
					if( alreadyHasContent )
						outElem.addContent( new Element("br") );
					outElem.addContent( tmpStr );
					alreadyHasContent = true;
				}
			}

			if( getShouldCalculateMax() ) {
				double tmpD = inExistingStats[3];
				String tmpStr = null;
				if( tmpD == Double.NEGATIVE_INFINITY )
					tmpStr = getNullValueDisplay();
				else {
					tmpStr = ""+tmpD;
					if( tmpStr.endsWith(".0") )
						tmpStr = tmpStr.substring(0, tmpStr.length()-2 );
				}
				if( null!=tmpStr ) {
					// if( outElem.hasChildren() )
					if( alreadyHasContent )
						outElem.addContent( new Element("br") );
					outElem.addContent( tmpStr );
					alreadyHasContent = true;
				}
			}

			if( getShouldCalculateCount() ) {
				String tmpStr = ""+(long)inExistingStats[1];
				// if( outElem.hasChildren() )
				if( alreadyHasContent )
					outElem.addContent( new Element("br") );
				outElem.addContent( tmpStr );
				alreadyHasContent = true;
			}



		}




		return outElem;
	}


	public String calculateFilterExpression( AuxIOInfo inRequest )
	{
		final String kFName = "calculateFilterExpression";

		if( null == inRequest )
		{
			errorMsg( kFName,
				"Null request object passed in, returning null."
				);
			return null;
		}

		// Get some variables, some can throw exceptions
		String fieldExpr = null;
		String cgiField = null;
		try
		{
			// Get the field expression
			// fieldExpr = getSqlAlias();
			// if( null == fieldExpr )
				fieldExpr = getSqlFieldExpression();
			if( null == fieldExpr )
			{
				errorMsg( kFName,
					"Unable to get field expression, returning null."
					);
				return null;
			}
	
			// Get the value
			cgiField = getFieldID();
			if( null == cgiField )
			{
				errorMsg( kFName,
					"Unable to get cgi field name, returning null."
					);
				return null;
			}
		}
		catch( Exception e )
		{
			errorMsg( kFName,
				"Error producing filter, returning null."
				+ " Error: " + e
				);
			return null;
		}

		// String cgiValue = inRequest.getScalarCGIFieldTrimOrNull(
		// No!  Very important that we get un-normalized values
		String cgiValue = inRequest.getScalarCGIField_UnnormalizedValue(
			cgiField
			);
		// If they wanted us to actually do an IS NULL query they should
		// have passed in the internal null marker in the CGI
		// so if it's REALLY null then that's a mistake
		if( null == cgiValue ) {
			errorMsg( kFName,
				"Unable to get cgi field value, returning null."
				);
			return null;
		}

		// Paradoxically, we NOW normalize the value to null
		// if it's our system null marker
		if( cgiValue.equalsIgnoreCase( ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE ) )
			cgiValue = null;

		// Sanity check if we allow nulls
		if( null == cgiValue && ! getDoFilterOnNull() )
		{
			errorMsg( kFName,
				"Null marker detected, but null filter not allowed."
				+ " Returning null."
				);
			return null;
		}

		StringBuffer outBuff = new StringBuffer();
		outBuff.append( fieldExpr );
		// If it's not a null
		if( null != cgiValue )
		{
			// First, add the operator
			outBuff.append( getFilterOperator() );

			// Then add the operand

			// Is this a quoted field?
			if( getDoQuoteFilterValues() )
			{
				// opening quote
				outBuff.append( '\'' );

				// Add the encoded value
				String tmpStr = NIEUtil.sqlEscapeString(
					cgiValue, true
					);
				if( null == tmpStr )
				{
					errorMsg( kFName,
						"Error encoding quote value \"" + cgiValue + "\""
						+ " Returning null."
						);
					return null;
				}
				outBuff.append( tmpStr );

				// Closing quote
				outBuff.append( '\'' );
			}
			// Else it's not quoted, just add it
			else
				outBuff.append( cgiValue );
		}
		// Else it is an allowed null
		else
		{
			outBuff.append( " IS NULL" );
		}

		// We're done!
		return new String( outBuff );

	}



	String _generateSQLFieldString()
		throws ReportConfigException
	{
		if( ! fUseCache )
		{
			final String kFName = "generateSQLFieldString";
			final String kExTag = kClassName + '.' + kFName + ": ";


		}
		return cSqlText;
	}

	String getSqlFieldExpression()
		throws ReportConfigException
	{
		if( ! fUseCache && null==cSqlFieldStatement )
		{
			final String kFName = "getSqlFieldExpression";
			final String kExTag = kClassName + '.' + kFName + ": ";

			cSqlFieldStatement = JDOMHelper.getTextTrimOrNull( fMainElem );
			if( null==cSqlFieldStatement )
				throw new ReportConfigException( kExTag
					+ "Null/empty field name."
					);
		}
		return cSqlFieldStatement;
	}



	String getFieldIDOrNull()
	{
		String tmpStr = null;
		try
		{
			tmpStr = getFieldID();
		}
		catch( Exception e )
		{
			tmpStr = null;
		}
		return tmpStr;	
	}

	// This should be called AFTER other caching getters
	// and it ALSO sets the cache value for cIsExplicitID
	// will normalize based on kIsCasen
	String getFieldID()
		throws ReportConfigException
	{
		if( ! fUseCache && null==cFieldID )
		{
			final String kFName = "getFieldID";
			final String kExTag = kClassName + '.' + kFName + ": ";

			final boolean kIsCasen = false;

			// Did we have a specifically set ID?
			cFieldID = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, FIELD_ID_ATTR
				);

			// If yes, we're done, and make sure
			// we remember that it was specifically set
			if( null != cFieldID )
			{
				cIsExplicitID = true;
			}
			// Else we didn't find one, look for a sql alias
			else
			{
				// OK, admit that we're guessing, so that later
				// the report class can resolve conflicts easier
				cIsExplicitID = false;

				// Try the SQL Alias
				cFieldID = getSqlAlias();

				// And if that's null, then go for the SQL expression
				if( null == cFieldID )
					cFieldID = getSqlFieldExpression();
			}

			// final normalization
			if( kIsCasen )
				cFieldID = NIEUtil.trimmedStringOrNull( cFieldID );
			else
				cFieldID = NIEUtil.trimmedLowerStringOrNull( cFieldID );

			if( null==cFieldID )
				throw new ReportConfigException( kExTag
					+ "Null/empty field ID."
					);

		}
		return cFieldID;
	}



	// CACHED by getFieldID()
	public boolean getIsExplicitID()
	{
		return cIsExplicitID;
	}

	String getSqlAlias()
	{
		if( ! fUseCache && null == cSqlFieldAlias )
		{
			cSqlFieldAlias = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, SQL_ALIAS_ATTR
				);
		}
		return cSqlFieldAlias;
	}

	String getDateFormat()
	{
		if( ! fUseCache && null == cDateFormat )
		{
			cDateFormat = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, FORMAT_STRING_ATTR
				);
			if( null==cDateFormat )
				cDateFormat = getDefaultDateFormat();
		}
		return cDateFormat;
	}

	boolean getShouldDisplay()
	{
		if( ! fUseCache )
		{
			cShouldDisplay = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				SHOULD_DISPLAY_ATTR, DEFAULT_SHOULD_DISPLAY_FIELD
				);
		}
		return cShouldDisplay;
	}
	boolean getShouldCalculateTotal()
	{
		if( ! fUseCache )
		{
			cShouldCalculateTotal = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				SHOULD_CALC_TOTAL_ATTR, DEFAULT_SHOULD_CALC_TOTAL
				);
		}
		return cShouldCalculateTotal;
	}
	boolean getShouldCalculateMin()
	{
		if( ! fUseCache )
		{
			cShouldCalculateMin = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				SHOULD_CALC_MIN_ATTR, DEFAULT_SHOULD_CALC_MIN
				);
		}
		return cShouldCalculateMin;
	}
	boolean getShouldCalculateMax()
	{
		if( ! fUseCache )
		{
			cShouldCalculateMax = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				SHOULD_CALC_MAX_ATTR, DEFAULT_SHOULD_CALC_MAX
				);
		}
		return cShouldCalculateMax;
	}
	boolean getShouldCalculateAverage()
	{
		if( ! fUseCache )
		{
			cShouldCalculateAverage = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				SHOULD_CALC_AVG_ATTR, DEFAULT_SHOULD_CALC_AVERAGE
				);
		}
		return cShouldCalculateAverage;
	}
	boolean getShouldCalculateCount()
	{
		if( ! fUseCache )
		{
			cShouldCalculateCount = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				SHOULD_CALC_COUNT_ATTR, DEFAULT_SHOULD_CALC_COUNT
				);
		}
		return cShouldCalculateCount;
	}
	boolean getShouldDoStatistics()
	{
		if( ! fUseCache ) {
			// Force all four methods to be called, to force caching, even if earlier one is true
			boolean a = getShouldCalculateTotal();
			boolean b = getShouldCalculateMin();
			boolean c = getShouldCalculateMax();
			boolean d = getShouldCalculateAverage();
			boolean e = getShouldCalculateCount();
			// Our answer
			cShouldDoStats = a || b || c || d || e;
		}
		return cShouldDoStats;
	}
	boolean getHasMultipleStatistics()
	{
		if( ! fUseCache ) {
			int i = 0;
			i += getShouldCalculateTotal() ? 1 : 0;
			i += getShouldCalculateMin() ? 1 : 0;
			i += getShouldCalculateMax() ? 1 : 0;
			i += getShouldCalculateAverage() ? 1 : 0;
			i += getShouldCalculateCount() ? 1 : 0;
			// Our answer
			cHasMultipleStats = i > 1;
		}
		return cHasMultipleStats;
	}



	Double getNullStatsValue() throws ReportConfigException {
		if( ! fUseCache )
		{
			final String kFName = "getNullStatsValue";
			final String kExTag = kClassName + '.' + kFName + ": ";
			String tmpStr = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, NULL_STATS_VALUE_ATTR
				);
			boolean isExplicitValue = true;

			// We use what's displayed for null by default, because that's what
			// they see, BUT it's often not a legit number, so don't complain
			if( null == tmpStr ) {
				tmpStr = getNullValueDisplay();
				isExplicitValue = false;	// We won't complain if it doesn't parse
			}

			if( null != tmpStr ) {
				try {
					cNullStatsValue = new Double( tmpStr );
				}
				catch( NumberFormatException e ) {
					cNullStatsValue = null;
					// If it's a value that should have converted, complain loudly
					if( isExplicitValue )
						throw new ReportConfigException( kExTag +
							"Invalid null value stats placeholder given: \"" + tmpStr + "\"."
							+ " Error: " + e
							);
				}
			}
		}
		return cNullStatsValue;
	}
	Double getNullStatsValueOrNull() {
		Double retVal = null;
		try {
			retVal = getNullStatsValue();
		}
		catch( Exception e ) {
			retVal = null;
		}
		return retVal;
	}


	String getHeadingOrNull()
	{
		String tmpStr = null;
		try
		{
			tmpStr =  getHeading();
		}
		catch( Exception e )
		{
			tmpStr = null;
		}
		return tmpStr;	
	}

	// Should always return a value
	String getHeading()
		throws ReportConfigException
	{
		if( ! fUseCache && null==cFieldHeading )
		{
			// Look for a proper heading
			cFieldHeading = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, HEADING_ATTR
				);
			// Fall back to the alias they gave us
			if( null == cFieldHeading )
				cFieldHeading = getSqlAlias();
			// OK, fall back to the original field
			// And this will throw an exception if no name
			if( null == cFieldHeading )
				cFieldHeading = getSqlFieldExpression();
		}
		return cFieldHeading;
	}

	int getHeadingColSpan() {
		if( ! fUseCache )
		{
			cFieldHeadingColSpan = JDOMHelper.getIntFromAttribute(
				fMainElem, HEADING_COLSPAN_ATTR, 1
				);
		}
		return cFieldHeadingColSpan;
	}

	String getSqlFieldAsSelectString()
		throws ReportConfigException
	{
		if( ! fUseCache )
		{
			final String kFName = "getSqlFieldsAsSelectString";
			final String kExTag = kClassName + '.' + kFName + ": ";

			cSqlText = getSqlFieldExpression();
			String tmpStr = getSqlAlias();
			if( null != tmpStr )
				// cSqlText += ' ' + tmpStr;
				// add it, possibly with " AS " syntax
				cSqlText += getDBConfig().getVendorAliasString() + tmpStr;
		}
		return cSqlText;
	}



	String getCssHeadingClass()
	{
		if( ! fUseCache && null == cCssHeadingClass )
		{
			cCssHeadingClass =
				JDOMHelper.getStringFromAttributeOrDefaultValue(
					fMainElem,
					HEADING_CSS_CLASS_ATTR, CSSClassNames.HEADER_CELL
					);
		}
		return cCssHeadingClass;
	}
	String getCssDataClassOrNull()
	{
		if( ! fUseCache && null == cCssDataClass )
		{
			// String lDefault =
			//	DEFAULT_EXTERNAL_LINK_CSS_CLASS

			cCssDataClass =
				JDOMHelper.getStringFromAttribute(
					fMainElem,
					DATA_CSS_CLASS_ATTR
					);

			/***
			cCssDataClass =
				JDOMHelper.getStringFromAttributeOrDefaultValue(
					fMainElem,
					DATA_CSS_CLASS_ATTR,
					CSSClassNames.DATA_CELL
					);
			***/
		}
		return cCssDataClass;

	}

	String getFilterOperator()
	{
		if( ! fUseCache && null == cFilterOperator )
		{
			cFilterOperator =
				JDOMHelper.getStringFromAttributeOrDefaultValue(
					fMainElem,
					FILTER_OPERATOR_ATTR,
					DEFAULT_FILTER_OPERATOR
					);
		}
		return cFilterOperator;
	}

	String getInitialSortDirection()
	{
		if( ! fUseCache && null == cInitialSortDirection )
		{
			cInitialSortDirection =
				JDOMHelper.getStringFromAttributeOrDefaultValue(
					fMainElem,
					INIT_SORT_DIR_ATTR,
					DEFAULT_SORT_DIRECTION
					);
		}
		return cInitialSortDirection;
	}

	String getLinkTarget()
	{
		if( ! fUseCache && null == cLinkTarget )
		{
			cLinkTarget = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, LINK_TARGET_ATTR
				);
			// The default depends on whether this is an
			// internal or external link
			if( null == cLinkTarget )
				if( ! getIsExternalLink() )
					cLinkTarget = DEFAULT_INTERNAL_LINK_TARGET;
				else
					cLinkTarget = DEFAULT_EXTERNAL_LINK_TARGET;
		}
		return cLinkTarget;
	}

	String getLinkTitle()
	{
		if( ! fUseCache && null==cLinkTitle )
		{
			cLinkTitle = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, LINK_TITLE_ATTR
				);
		}
		return cLinkTitle;
	}
	String getLinkedReportName()
	{
		if( ! fUseCache && null==cLinkedReport )
		{
			cLinkedReport = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, LINKED_REPORT_ATTR
				);
		}
		return cLinkedReport;
	}
	String getLinkedReportFilterField()
	{
		if( ! fUseCache && null==cLinkedReportField )
		{
			cLinkedReportField = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, LINKED_REPORT_FIELD_ATTR
				);
		}
		return cLinkedReportField;
	}



	// Should always return a value
	String getNullValueDisplay()
	{
		if( ! fUseCache && null==cOnNullValueDisplay )
		{
			// Look for a holder value
			// we do NOT trim this
			cOnNullValueDisplay = JDOMHelper.getStringFromAttribute(
				fMainElem, NULL_VALUE_CONSTANT_ATTR
				);
			if( null == cOnNullValueDisplay )
				// cOnNullValueDisplay = DEFAULT_NULL_VALUE_CONSTANT;
				cOnNullValueDisplay = ReportConstants.DEFAULT_DISPLAY_NULL_VALUE_CONSTANT;
		}
		return cOnNullValueDisplay;
	}



	boolean getIsSortField()
	{
		if( ! fUseCache )
		{
			cIsSortable = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				IS_SORTABLE_ATTR, DEFAULT_IS_SORTABLE
				);
		}
		return cIsSortable;
	}

	boolean getIsFilterField()
	{
		if( ! fUseCache )
		{
			cIsFilterable = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				IS_FILTERABLE_ATTR, DEFAULT_IS_FILTERABLE
				);
		}
		return cIsFilterable;
	}


	// Calculate the min decimal places
	// the answer can't be cached because it can depend on the specific
	// value and the type of formatting desired
	// By the time you call this we kind of assume you know you have
	// a number of some sort
	// -1 means "no", don't do anything
	int getMinDecPlaces() {
		int tmpInt = getExplicitMinDecimalPlaces();
		if( tmpInt >= 0 )
			return tmpInt;
		String tmpFormat = getExplicitGeneralFormat();
		int defaultVal = getDefaultMinDecPlaces( tmpFormat );
		// We need a safetey override when choosing a default
		// we should not set min to be greater than max!
		int specifcMaxVal = getExplicitMaxDecimalPlaces();
		if( specifcMaxVal < 0 )
			return defaultVal;
		else {
			if( defaultVal > specifcMaxVal )
				return specifcMaxVal;
			else
				return defaultVal;
		}
	}
	static int getDefaultMinDecPlaces( String optExplicitFormat ) {
		int answer = DEFAULT_MIN_DEC_PLACES;
		if( null!=optExplicitFormat ) {
			if( optExplicitFormat.equals(FORMAT_AS_CURRENCY) )
				answer = DEFAULT_MIN_DEC_PLACES_CURRENCY;
			else if( optExplicitFormat.equals(FORMAT_AS_PERCENT) )
				answer = DEFAULT_MIN_DEC_PLACES_PERCENT;
			else
				answer = DEFAULT_MIN_DEC_PLACES;
			// else if( tmpFormat.equals(FORMAT_AS_PERCENT) )
		}
		return answer;
	}
	// IF it's a percentage, this is AFTER scaling
	int getMaxDecPlaces( double inValue ) {
		int tmpInt = getExplicitMaxDecimalPlaces();
		if( tmpInt >= 0 )
			return tmpInt;
		String tmpFormat = getExplicitGeneralFormat();

		// Get a reasonable default
		int defaultValue = getDefaultMaxDecPlaces( tmpFormat, inValue );

		// We need a safetey override when choosing a default
		// we should not default max to be less than the min!
		int specifcMinVal = getExplicitMinDecimalPlaces();
		if( specifcMinVal < 0 )
			return defaultValue;
		else {
			if( defaultValue < specifcMinVal )
				return specifcMinVal;
			else
				return defaultValue;
		}
	}
	static int getDefaultMaxDecPlaces( String optExplicitFormat, double inValue ) {
		int answer = DEFAULT_MAX_DEC_PLACES;
		if( null!=optExplicitFormat ) {
			if( optExplicitFormat.equals(FORMAT_AS_CURRENCY) )
				answer = DEFAULT_MAX_DEC_PLACES_CURRENCY;
			else if( optExplicitFormat.equals(FORMAT_AS_PERCENT) )
				if( inValue >= 1.0 || inValue <= -1.0 )
					answer = DEFAULT_MAX_DEC_PLACES_PERCENT_GTE_ONE;
				else
					answer = DEFAULT_MAX_DEC_PLACES;
			else
				if( inValue >= 1.0 || inValue <= -1.0 )
					answer = DEFAULT_MAX_DEC_PLACES_GTE_ONE;
				else
					answer = DEFAULT_MAX_DEC_PLACES;
		}
		return answer;
	}


	String getPrefix() {
		String tmpStr = getExplicitPrefixString();
		// If one was set, return it
		if( null!=tmpStr )
			return tmpStr;	
		// maybe a default prefix?
		String tmpFormat = getExplicitGeneralFormat();
		if( null!=tmpFormat ) {
			if( tmpFormat.equals(FORMAT_AS_CURRENCY) )
				return DEFAULT_PREFIX_CURRENCY;
			else
				return null;
		}
		// Else no default
		else {
			return null;
		}
	}

	String getSuffix() {
		String tmpStr = getExplicitSuffixString();
		// If one was set, return it
		if( null!=tmpStr )
			return tmpStr;	
		// maybe a default prefix?
		String tmpFormat = getExplicitGeneralFormat();
		return getDefaultSuffix( tmpFormat );
	}
	static String getDefaultSuffix( String optFormat ) {
		String answer = null;
		if( null!=optFormat ) {
			if( optFormat.equals(FORMAT_AS_PERCENT) )
				answer = DEFAULT_SUFFIX_PERCENTAGE;
			else
				answer = null;
		}
		return answer;
	}


	String getExplicitGeneralFormat()
	{
		if( ! fUseCache )
		{
			cGeneralFormat = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem,
				GENERAL_FORMAT_ATTR
				);
			if( null!=cGeneralFormat )
				cGeneralFormat = cGeneralFormat.toLowerCase();
		}
		return cGeneralFormat;
	}

	String getExplicitPrefixString()
	{
		if( ! fUseCache )
		{
			// we do NOT trim or null, an empty string is acceptable to override a default
			cPrefixStr = JDOMHelper.getStringFromAttribute(
				fMainElem,
				FIXED_PREFIX_ATTR
				);
		}
		return cPrefixStr;
	}
	String getExplicitSuffixString()
	{
		if( ! fUseCache )
		{
			// we do NOT trim or null, an empty string is acceptable to override a default
			cSuffixStr = JDOMHelper.getStringFromAttribute(
				fMainElem,
				FIXED_SUFFIX_ATTR
				);
		}
		return cSuffixStr;
	}



	boolean getIsExplcitlyNumeric()
	{
		if( ! fUseCache )
		{
			String tmpStr = getExplicitGeneralFormat();
			if( null!=tmpStr ) {
				if( tmpStr.equals(FORMAT_AS_PERCENT)
					|| tmpStr.equals(FORMAT_AS_NUMBER)
					|| tmpStr.equals(FORMAT_AS_CURRENCY)
					)
					cIsExplcitNumber = true;
			}
			else if( getExplicitMinDecimalPlaces() >= 0 || getExplicitMinDecimalPlaces() >=0 )
				cIsExplcitNumber = true;
		}
		return cIsExplcitNumber;
	}

	boolean getIsExplcitlyDate()
	{
		if( ! fUseCache )
		{
			String tmpStr = getExplicitGeneralFormat();
			if( null!=tmpStr ) {
				if( tmpStr.equals(FORMAT_AS_DATE)
					|| tmpStr.equals(FORMAT_AS_TIME)
					|| tmpStr.equals(FORMAT_AS_DATETIME)
					)
					cIsExplcitDate = true;
			}
			else if( getExplicitMinDecimalPlaces() >= 0 || getExplicitMinDecimalPlaces() >=0 )
				cIsExplcitDate = true;
		}
		return cIsExplcitDate;
	}

	String getDefaultDateFormat()
	{
		if( ! fUseCache && null==cDefaultDateFormat )
		{
			final String kFName = "getDefaultDateFormat";
			String tmpStr = getExplicitGeneralFormat();
			if( null!=tmpStr ) {
				if( tmpStr.equals(FORMAT_AS_DATE) )
					cDefaultDateFormat = ReportConstants.DEFAULT_DISPLAY_DATE_FORMAT;
				else if( tmpStr.equals(FORMAT_AS_TIME) )
					cDefaultDateFormat = ReportConstants.DEFAULT_DISPLAY_TIME_FORMAT;
				else if( tmpStr.equals(FORMAT_AS_DATETIME) )
					cDefaultDateFormat = ReportConstants.DEFAULT_DISPLAY_DATETIME_FORMAT;
				else {
					cDefaultDateFormat = ReportConstants.DEFAULT_DISPLAY_DATETIME_FORMAT;
					/*** this is ok, many other formats not related to dates
					errorMsg( kFName,
						"Don't know default date/time format for \"" + tmpStr + "\""
						+ " Using default format for date+time."
						);
					***/
				}
			}
			else
				cDefaultDateFormat = ReportConstants.DEFAULT_DISPLAY_DATETIME_FORMAT;
		}
		return cDefaultDateFormat;
	}

	// The _default_ value changes, so just tell us if they set anything,
	// the call will handle the rest
	int getExplicitMinDecimalPlaces()
	{
		if( ! fUseCache )
		{
			cMinDec = JDOMHelper.getIntFromAttribute(
				fMainElem,
				MIN_DECIMAL_ATTR, -1	// NOT DEFAULT_MIN_DEC_PLACES
				);
		}
		return cMinDec;
	}
	int getExplicitMaxDecimalPlaces()
	{
		if( ! fUseCache )
		{
			cMaxDec = JDOMHelper.getIntFromAttribute(
				fMainElem,
				MAX_DECIMAL_ATTR, -1	// NOT DEFAULT_MAX_DEC_PLACES
				);
		}
		return cMaxDec;
	}

	Element getTrueLinkElem()
	{
		if( ! fUseCache && null==cLinkElem )
		{
			cLinkElem = JDOMHelper.findElementByPath( fMainElem, ReportLink.MAIN_ELEM_NAME, false );
		}
		return cLinkElem;
	}

	boolean getIsReportLink()
	{
		if( ! fUseCache )
		{
			final String kFName = "getIsReportLink";

			List tmpChildren = fMainElem.getChildren( ReportLink.MAIN_ELEM_NAME );

			boolean hasReportStuff =
				null!=getTrueLinkElem()
				|| null != getLinkedReportName()
				|| null != getLinkedReportFilterField()
				;
			boolean lDefault = hasReportStuff
				? true : DEFAULT_IS_REPORT_LINK
				;

			cIsReportLink = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				IS_REPORT_LINK_ATTR,
				lDefault
				);

			if( hasReportStuff && ! cIsReportLink )
				warningMsg( kFName,
					"Report link name and/or filter has been set"
					+ " but this field is NOT set to be a link."
					+ " Maybe link logic has been turned off temporarily?"
					+ " Please check configuration."
					);

		}
		return cIsReportLink;
	}

	boolean getDoLinkOnNull()
	{
		if( ! fUseCache )
		{
			cDoLinkOnNull = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				LINK_ON_NULL_VALUES_ATTR, DEFAULT_DO_LINK_ON_NULL_FIELD
				);
		}
		return cDoLinkOnNull;
	}
	boolean getDoFilterOnNull()
	{
		if( ! fUseCache )
		{
			cDoFilterOnNull = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				FILTER_ON_NULL_VALUES_ATTR, DEFAULT_FILTER_ON_NULL_FIELD
				);
		}
		return cDoFilterOnNull;
	}

	boolean getIsSortReversible()
	{
		if( ! fUseCache )
		{
			cIsSortReversible = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				IS_SORT_REVERSIBLE_ATTR, DEFAULT_IS_SORT_REVERSIBLE
				);
		}
		return cIsSortReversible;
	}

	boolean getIsDeclaredDefaultSortField()
	{
		if( ! fUseCache )
		{
			cIsDeclaredDefaultSortField = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				IS_DEFAULT_SORT_ATTR, false	// No sense to ever def true
				);
		}
		return cIsDeclaredDefaultSortField;
	}

	boolean getIsExternalLink()
	{
		if( ! fUseCache )
		{
			cIsExternalLink = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				IS_EXTERNAL_HREF_ATTR, DEFAULT_IS_EXTERNAL_LINK
				);
		}
		return cIsExternalLink;
	}

	boolean getDoQuoteFilterValues()
	{
		if( ! fUseCache )
		{
			cDoQuoteFilterStrings = JDOMHelper.getBooleanFromAttribute(
				fMainElem,
				DO_QUOTE_FILTER_VALUES_ATTR, DEFAULT_QUOTE_FILTER_VALUES
				);
		}
		return cDoQuoteFilterStrings;
	}






	nie.sn.SearchTuningApp _getMainApp()
	{
		return _fMainApp;
	}
	nie.sn.SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
	}

	DBConfig getDBConfig()
	{
		// return getMainApp().getDBConfig();
		return getMainConfig().getDBConfig();
	}

	XMLDefinedReport getParentReport()
	{
		return fReport;
	}
	String getReportName()
	{
		final String kFName = "getReportName";
		if( null != getParentReport() )
			return getParentReport().getReportName();
		else
		{
			errorMsg( kFName, "No associated report, returning null." );
			return null;
		}
	}



	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}


	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName, inFromRoutine );
	}


	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
	}


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


	private static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}




	private static void __Main_Members_and_Constants__() {}

	Element fMainElem;
	XMLDefinedReport fReport;
	nie.sn.SearchTuningApp _fMainApp;
	nie.sn.SearchTuningConfig fMainConfig;

	boolean fUseCache;
	// Field tag related strings
	// private static final String FIELD_ELEM_PATH = "field";
	private static final String HEADING_ATTR = "heading";
	private static final String HEADING_COLSPAN_ATTR = "heading_colspan";

	static final String NULL_VALUE_CONSTANT_ATTR = "null_value_display";
	static final String FIELD_ID_ATTR = "id";

	static final String SHOULD_DISPLAY_ATTR = "should_display";


	static final String _TH_PATH = "th";

	private static void __SQL_Members_and_Constants__() {}
	String cFieldID;
	String cSqlFieldStatement;
	String cSqlFieldAlias;
	boolean cIsExplicitID;

	String cSqlText;

	ReportLink cReportLinkObj;

	String cFieldHeading;
	int cFieldHeadingColSpan;
	boolean cShouldDisplay;
	static final String SQL_ALIAS_ATTR = "sql_alias";



	private static void __Sorting_Members_and_Constants__() {}

	boolean cIsDeclaredDefaultSortField;
	String cInitialSortDirection;



	boolean cIsSortable;
	boolean cIsSortReversible;
	static final String IS_SORTABLE_ATTR = "is_sort_field";
	static final String IS_DEFAULT_SORT_ATTR = "is_default_sort_field";
	static final String INIT_SORT_DIR_ATTR = "sort_direction";
	static final String IS_SORT_REVERSIBLE_ATTR = "is_reversible";
	static final boolean DEFAULT_IS_SORTABLE = false;
	static final boolean DEFAULT_IS_SORT_REVERSIBLE = true;
	static final String DEFAULT_SORT_DIRECTION = "asc";

	private static void __Filtering_Members_and_Constants__() {}





	boolean cIsFilterable;
	boolean cDoFilterOnNull;
	String cFilterOperator;
	boolean cDoQuoteFilterStrings;
	
	
	






	static final String IS_FILTERABLE_ATTR = "is_filter_field";
	static final String FILTER_OPERATOR_ATTR = "filter_operator";
	static final String FILTER_ON_NULL_VALUES_ATTR =
		"do_filter_if_null";
	static final String DO_QUOTE_FILTER_VALUES_ATTR = "quote_values";
	static final boolean DEFAULT_IS_FILTERABLE = true; // false;
	static final boolean DEFAULT_FILTER_ON_NULL_FIELD = false;
	static final boolean DEFAULT_QUOTE_FILTER_VALUES = true;
	static final String DEFAULT_FILTER_OPERATOR = "=";

	private static void __Hyperlinking_Members_and_Constants__() {}



	String cLinkTarget;

	String cLinkTitle;

	String cLinkedReport;

	String cLinkedReportField;

	boolean cDoLinkOnNull;

	boolean cIsReportLink;

	boolean cIsExternalLink;






	static final String IS_REPORT_LINK_ATTR = "is_report_link";
	public static final String LINKED_REPORT_ATTR =
		"link_report_name";
	static final String LINKED_REPORT_FIELD_ATTR =
		"link_report_filter_field";
	static final String LINK_ON_NULL_VALUES_ATTR =
		"do_link_if_null";
	static final String IS_EXTERNAL_HREF_ATTR = "is_external_link";
	static final String LINK_TARGET_ATTR = "link_target";
	static final String LINK_TITLE_ATTR = "link_title";





	public static final String _DEFAULT_EXTERNAL_LINK_CSS_CLASS =
		CSSClassNames.ACTIVE_RPT_LINK;
	static final boolean DEFAULT_IS_REPORT_LINK = false;
	static final boolean DEFAULT_DO_LINK_ON_NULL_FIELD = false;
	static final boolean DEFAULT_IS_EXTERNAL_LINK = false;
	static final String DEFAULT_INTERNAL_LINK_TARGET = null;
	static final String DEFAULT_EXTERNAL_LINK_TARGET = "_blank";

	private static void __Misc_Formatting_Members_and_Constants__() {}

	String cGeneralFormat;
	String cPrefixStr;
	String cSuffixStr;
	String cOnNullValueDisplay;

	boolean cHasDecode;
	String cDecodeDefaultValue;
	Hashtable cDecodeHash;
	static final String GENERAL_FORMAT_ATTR = "format_as";
	static final String FORMAT_STRING_ATTR = "format_template";

	Element cLinkElem;




	static final String FIXED_PREFIX_ATTR = "prefix"; // TODO: FIXED_PREFIX_ATTR
	static final String FIXED_SUFFIX_ATTR = "suffix"; // TODO: FIXED_SUFFIX_ATTR


	static final String NULL_STATS_VALUE_ATTR = "null_stats_value";

	public static final String _DEFAULT_NULL_VALUE_CONSTANT = " - "; // Per Miles


	// Having to do with decode
	private static final String DECODE_PATH = "decode";

	private static final String _DECODE_DO_RANGE_ATTR = "_use_range";

	private static final String DECODE_VALUE_PATH = "item";

	private static final String DECODE_VALUE_MATCH_ATTR = "key";

	private static final String DECODE_DEFAULT_PATH = "default_" + DECODE_VALUE_PATH;
	;


	// We show fields by default
	private static final boolean DEFAULT_SHOULD_DISPLAY_FIELD = true;

	// The internal tag WE USE to indicate a null value when creating
	// CGI filter links
	// Moved to ReportConstants
	// public static final String _INTERNAL_NULL_MARKER_SEQUENCE = "(null)";


	// general formats we understand
	public static final String FORMAT_AS_NUMBER = "number";

	public static final String FORMAT_AS_PERCENT = "percentage";

	public static final String FORMAT_AS_CURRENCY = "currency";

	public static final String FORMAT_AS_BOOLEAN = "boolean";

	public static final String FORMAT_AS_DATETIME = "datetime";	// TODO, epoch vs float, just time or date
	
	public static final String FORMAT_AS_DATE = "date";

	public static final String FORMAT_AS_TIME = "time";




	public static final String DEFAULT_BOOLEAN_TRUE_STRING = "Y";
	public static final String DEFAULT_BOOLEAN_FALSE_STRING = "N";

	private static void __Numeric_Formatting_Members_and_Constants__() {}


	boolean cIsExplcitNumber;
	int cMinDec;
	int cMaxDec;
	Double cNullStatsValue;

	boolean cShouldCalculateTotal;
	boolean cShouldCalculateMin;
	boolean cShouldCalculateMax;
	boolean cShouldCalculateAverage;
	boolean cShouldCalculateCount;
	boolean cShouldDoStats;
	boolean cHasMultipleStats;
	static final String MIN_DECIMAL_ATTR = "min_decimal_places";
	static final String MAX_DECIMAL_ATTR = "max_decimal_places";
	static final String _ROUNDING_TEN_POWER_ATTR = "_tens_rounding"; // TODO: _ROUNDING_TEN_POWER_ATTR 
	static final String _SCALING_FACTOR_ATTR = "_scale"; // TODO: _SCALING_FACTOR_ATTR

	static final String SHOULD_CALC_TOTAL_ATTR = "display_sum";
	static final String SHOULD_CALC_MIN_ATTR = "display_min";
	static final String SHOULD_CALC_MAX_ATTR = "display_max";
	static final String SHOULD_CALC_AVG_ATTR = "display_avg";
	static final String SHOULD_CALC_COUNT_ATTR = "display_count";

	// Some defaults for formatting
	// we don't force decimals by default
	public static final int DEFAULT_MIN_DEC_PLACES = -1;

	public static final int DEFAULT_MAX_DEC_PLACES = -1;

	// round to 3 decimal places by default for |val|>=1.0, no limit if < 1
	public static final int DEFAULT_MAX_DEC_PLACES_GTE_ONE = 3;

	// for perecentages, round to 1 decimal places by default for |val|>=1.0%, no limit if < 1
	public static final int DEFAULT_MAX_DEC_PLACES_PERCENT_GTE_ONE = 1;

	public static final int DEFAULT_MIN_DEC_PLACES_PERCENT = 1;


	public static final int DEFAULT_MIN_DEC_PLACES_CURRENCY = 2;

	public static final int DEFAULT_MAX_DEC_PLACES_CURRENCY = 2;


	public static final String DEFAULT_SUFFIX_PERCENTAGE = "%";

	public static final String DEFAULT_PREFIX_CURRENCY = "$";



	// Whether to do total on this field for thie values on this page
	private static final boolean DEFAULT_SHOULD_CALC_TOTAL = false;


	private static final boolean DEFAULT_SHOULD_CALC_MIN = false;


	private static final boolean DEFAULT_SHOULD_CALC_MAX = false;


	private static final boolean DEFAULT_SHOULD_CALC_AVERAGE = false;


	private static final boolean DEFAULT_SHOULD_CALC_COUNT = false;







	private static void __Date_Time_Formatting_Members_and_Constants__() {}

	boolean cIsExplcitDate;
	String cDefaultDateFormat;
	String cDateFormat;




	private static void __CSS_Members_and_Constants__() {}


	String cCssHeadingClass;
	String cCssDataClass;
	public static final String DATA_CSS_CLASS_ATTR = "css_class";
	static final String HEADING_CSS_CLASS_ATTR = "css_heading_class";


	public static final boolean K_ADD_COMMAS = true;

	public static final String _DEFAULT_TD_CSS_CLASS = "nie_data_cell";
	public static final String _DEFAULT_TH_CSS_CLASS = "nie_header_cell";

	public static final String _DEFAULT_SPECIAL_TD_CSS_CLASS = "nie_special_data_cell";
	public static final String _DEFAULT_NUMERIC_TD_CSS_CLASS = "nie_numeric_cell";
	public static final String _DEFAULT_DATETIME_TD_CSS_CLASS = "nie_datetime_cell";
	public static final String _DEFAULT_PERCENTAGE_TD_CSS_CLASS = "nie_percentage_cell";

	

	


}
