package nie.sn;

import nie.core.*;
import org.jdom.Element;
import java.util.*;
import java.lang.reflect.*;

//
// WARNING:
// For performance reasons we must use caching
// It is generally NOT SAFE to add or edit this record and then use the GET
// methods - these caches will likely provide invalid data
//
// The idea is that, for now, to change something, you create a whole new map
// This should be OK since updating the map is rather infrequent
//
// Todo: allow editing with fancy cache control...
//


abstract public class BaseMapRecord implements MapRecordInterface
{

	abstract public String kClassName();
	static final String kStaticClassName = "BaseMapRecord(+)";

	public BaseMapRecord( SearchTuningConfig inMainConfig, Element inMainElement, int inID )
		throws MapRecordException
	{

		final String kFName = "(BaseMapRecord)constructor";
		final String kExTag = kFName + ": ";

		cID = inID;

		// Sanity checks

		if( null == inMainConfig )
			throw new MapRecordException(
				kExTag + "Constructor was passed in a NULL application configuration."
				);
		fMainConfig = inMainConfig;

		if( null == inMainElement )
			throw new MapRecordException(
				kExTag + "Constructor was passed in a NULL configuration element."
				);
		// Instantiate and store the main JDOMHelper a
		fMainElement = null;
		try {
			fMainElement = new JDOMHelper( inMainElement );
		}
		catch (JDOMHelperException e) {
			throw new MapRecordException( kExTag +
				"Error processing configuration element: " + e
				);
		}
		if( fMainElement == null )
			throw new MapRecordException( kExTag +
				"Got back a NULL xml tree when trying to create a map record."
				);

		// We need to initialize and cache a bunch of properties
		initWmsBoxProperties();
		initWmsIconProperties();
		initWmsDocTitleProperties();
		initWmsDocUrlProperties();
		initWmsDocSummaryProperties();
		initWmsSloganProperties();
		// debugMsg( kFName, "Calling initAltSloganProperties **************************" );
		initAltSloganProperties();

		// Cache and check some stuff
		if( null==generateWmsSloganElement() && getHasWmsURLs() )
			throw new MapRecordException( kExTag +
				"Must have valid Webmaster Suggests header if suggesting urls."
				);
		if( null==generateAltSloganElement() && getHasAlternateTerms() )
			throw new MapRecordException( kExTag +
				"Must have valid Alternate Suggestions header if presenting related terms."
				);

		if( shouldDoDebugMsg(kFName) ) {
			String msg = "Map stats: "
				+ " urls=" + getURLObjectsCount()
				+ " wms=" + getWmsURLObjectsCount()
				+ " redirs=" + getRedirectURLObjectsCount()
				+ " alt terms=" + getAlternateTermsCount()
				+ " udi=" + getUserDataItemsCount()
				;
			debugMsg( kFName, msg );
		}
		// Derived classes should now finish intialization
	}





	public int getID()
	{
		return cID;
	}

	// We give back an array
	// The first item is the top level tag
	// the second is where to hang additional content
	// If this creates a single level table then they will in fact match,
	// but for colored border tables it's actually a nested pair of tables
	public Element [] generateWmsBoxElements() {

//		/ { "width", DEFAULT_WMS_BOX_BGCOLOR },	// Usually something like "95%"
//		/ { "border", null },
//		/ { BORDER_COLOR_ATTR, null },
//		{ "cellpadding", null },		// IE defaults to 1
//		{ "cellspacing", null },		// IE defaults to 1
//		{ "bgcolor", DEFAULT_WMS_BOX_BGCOLOR },
//		/ { OUTER_CLASS_ATTR, CSSClassNames.WMS_BOX1 },
//		{ "class", CSSClassNames.WMS_BOX2 }		// probably they want inner class

		// Cache the answer the first time through
		// if( null==cWmsBoxElements ) {
		// ^^^ NO!, long story, but not we can't easily cache this because
		// we can't easily clone it
		// TODO: Revist, basically just clone the [0]'th element
		// and for the second element, try to navitage to tr/td/table, and if that
		// comes back null, just set it also the newly cloned top

			final String kFName = "generateWmsBoxElements";

			String border = getWmsBoxPropertyOrNull( "border" );
			String bgcolor = getWmsBoxPropertyOrNull( "bgcolor" );
			String borderColor = getWmsBoxPropertyOrNull( BORDER_COLOR_ATTR );

			// We do a nested table if we need to do a colored border
			boolean doNested = (null!=borderColor)
				// &&
				||
				( null==border || ! border.equals("0") )
				;

			// Now create the two points
			Element outElem = new Element( "table" );
			// Unlilke most of the properties, with width, if set, always
			// applies to the OUTER table
			String width = getWmsBoxPropertyOrNull( "width" );
			if( null!=width )
				outElem.setAttribute( "width", width );
			// And set or create the content table
			Element contentHanger = null;
			if( doNested ) {
				// Make the inner table and add it to the outer
				contentHanger = JDOMHelper.findOrCreateElementByPath(
					outElem,						// Starting at
					"tr/td/table/@width=100%",	// path
					true							// Yes, tell us about errors
					);
				// Set the optional outer table class
				String outerClass = getWmsBoxPropertyOrNull( OUTER_CLASS_ATTR );
				if( null!=outerClass )
					outElem.setAttribute("class", outerClass );
			}
			// Else not nested
			else {
				contentHanger = outElem;
			}

			// Add the border, as needed
			if( null==borderColor ) {
				// We only add a border if asked for one
				if( null!=border )
					outElem.setAttribute( "border", border );
				// If we're nesting tables and NOT using the outer table for
				// a colored border, then make it's background match the inner
				// tables background to avoid a 1 or 2 pixel gap
				if( null!=bgcolor )
					outElem.setAttribute( "bgcolor", bgcolor );
			}
			// Else there IS a border color
			else {
				// The background color of the outer table becomes the border
				outElem.setAttribute( "bgcolor", borderColor );
				// The "border" number becomes the padding number of the outer table
				border = (null!=border) ? border : "1";
				outElem.setAttribute( "cellpadding", border );
				// Set these others to zero
				outElem.setAttribute( "cellspacing", "0" );
				outElem.setAttribute( "border", "0" );
			}

			// Now we'll copy over any generic property that was set
			// and they will go to the "inner" table (if nested)
			// We first make a list of ones that we DON'T want because
			// we've already dealt with them
			HashSet exludeProps = new HashSet();
			exludeProps.add( "width" );
			exludeProps.add( "border" );
			exludeProps.add( BORDER_COLOR_ATTR );
			exludeProps.add( OUTER_CLASS_ATTR );

			// OK, copy over most of the properies
			for( int i=0; i<kWmsBoxPropertyNamesAndDefaults.length ; i++ ) {
				String attrName = kWmsBoxPropertyNamesAndDefaults[i][0];
				// If it's not one of the ones we're excluding then copy it over
				if( ! exludeProps.contains( attrName ) ) {
					String attrValue = getWmsBoxPropertyOrNull( attrName );
					if( null!=attrValue )
						contentHanger.setAttribute( attrName, attrValue );
				}
			}

			// Create the two element array
			// cWmsBoxElements = new Element [] { outElem, contentHanger };
			return new Element [] { outElem, contentHanger };

		// }	// Done filling cache

		// return cWmsBoxElements;

		// cloning this would be veryu weird... maybe not bother, should be read-only
		// Element outElem = (Element)cWmsBoxElements.clone()0 1;
		// return outElem;
	}

	public Element generateWmsIconElementOrNull() {
		// Cache the answer the first time through
		if( null==cWmsIconElement ) {
			final String kFName = "generateWmsIconElementOrNull";
			String srcURL = getWmsIconPropertyOrNull( "src" );
			if( null==srcURL ) {
				debugMsg( kFName, "No image specified, returning null.");
				return null;
			}
			// We'll need these IF we're creating a link
			String href = getWmsIconPropertyOrNull( "href" );
			String target = getWmsIconPropertyOrNull( "target" );
			// Mostly we're building an image element
			Element imgElement = new Element("img");
			for( int i=0; i<kWmsIconPropertyNamesAndDefaults.length ; i++ ) {
				String attrName = kWmsIconPropertyNamesAndDefaults[i][0];
				// If it's not one of the link properties then copy it over
				if( ! attrName.equals("href") && ! attrName.equals("target") ) {
					String attrValue = getWmsIconPropertyOrNull( attrName );
					if( null!=attrValue )
						imgElement.setAttribute( attrName, attrValue );
				}
			}
			// Do we want a linked image?
			if( null!=href ) {
				cWmsIconElement = new Element( "a" );
				cWmsIconElement.setAttribute( "href", href );
				if( null!=target )
					cWmsIconElement.setAttribute( "target", target );
				// And now add the image
				cWmsIconElement.addContent( imgElement );
			}
			else {
				if( null!=target )
					errorMsg( kFName,
						"Target attribute is only valid when creating a linked image (by setting href)."
						+ " Ignroring target.  Img=" + srcURL
						);
				cWmsIconElement = imgElement;
			}
		}
		// return cWmsSloganElement;
		Element outElem = (Element)cWmsIconElement.clone();
		return outElem;
	}

	public Element generateWmsDocTitleElementOrNull( SnURLRecord inURL ) {
		final String kFName = "generateWmsDocTitleElementOrNull";
		if( null==inURL ) {
			errorMsg( kFName, "No element passed in, returning null.");
			return null;
		}

		String shouldDisplayStr = getWmsDocTitlePropertyOrNull( WMS_DOC_TITLE_SHOULD_DISPLAY_ATTR );
		boolean shouldDisplay = NIEUtil.decodeBooleanString( shouldDisplayStr, DEFAULT_WMS_DOC_TITLE_SHOULD_DISPLAY );
		if( ! shouldDisplay ) {
			debugMsg( kFName, "Configured to not display, returning null.");
			return null;
		}

		// We'll need these IF we're creating a link
		String href = inURL.getURL();
		if( null==href ) {
			errorMsg( kFName, "Null href/url returned from URL object");
			return null;
		}
		// String displayURL = inURL.getDisplayURL();
		String displayTitle = inURL.getTitle();
		if( null==displayTitle )
			displayTitle = inURL.getDisplayURL();

		String target = getWmsDocUrlPropertyOrNull( "target" );
		String classStr = getWmsDocUrlPropertyOrNull( "class" );

		// Mostly we're building an image element
		// Element outElement = new Element("div");
		// Element midElem = new Element( "b" );
		Element outElement = new Element( "b" );
		// outElement.addContent( midElem );
		Element innerElem = new Element( "font" );
		// midElem.addContent( innerElem );
		outElement.addContent( innerElem );
		for( int i=0; i<kWmsDocTitlePropertyNamesAndDefaults.length ; i++ ) {
			String attrName = kWmsDocTitlePropertyNamesAndDefaults[i][0];
			// If it's not one of the link properties then copy it over
			if( ! attrName.equals(WMS_DOC_TITLE_SHOULD_DISPLAY_ATTR)
				&& ! attrName.equals("target")
				&& ! attrName.equals("class")
			) {
				String attrValue = getWmsDocTitlePropertyOrNull( attrName );
				if( null!=attrValue )
					innerElem.setAttribute( attrName, attrValue );
			}
		}
		Element linkElem = new Element( "a" );
		linkElem.setAttribute( "href", href );
		if( null!=target )
			linkElem.setAttribute( "target", target );
		if( null!=classStr )
			linkElem.setAttribute( "class", classStr );
		linkElem.addContent( displayTitle );
		innerElem.addContent( linkElem );

		// statusMsg( kFName, "Title element=" + NIEUtil.NL + JDOMHelper.JDOMToString(outElement, true));

		return outElement;
	}





	public Element generateWmsDocUrlElementOrNull( SnURLRecord inURL ) {
		final String kFName = "generateWmsDocUrlElementOrNull";
		if( null==inURL ) {
			errorMsg( kFName, "No element passed in, returning null.");
			return null;
		}

		String shouldDisplayStr = getWmsDocUrlPropertyOrNull( WMS_DOC_URL_SHOULD_DISPLAY_ATTR );
		boolean shouldDisplay = NIEUtil.decodeBooleanString( shouldDisplayStr, DEFAULT_WMS_DOC_URL_SHOULD_DISPLAY );
		if( ! shouldDisplay ) {
			debugMsg( kFName, "Configured to not display, returning null.");
			return null;
		}

		// We'll need these IF we're creating a link
		String href = inURL.getURL();
		if( null==href ) {
			errorMsg( kFName, "Null href/url returned from URL object");
			return null;
		}
		String displayURL = inURL.getDisplayURL();

		String target = getWmsDocUrlPropertyOrNull( "target" );
		String classStr = getWmsDocUrlPropertyOrNull( "class" );

		// Mostly we're building an image element
		Element outElement = new Element("small");
		Element midElem = new Element( "i" );
		outElement.addContent( midElem );
		Element innerElem = new Element( "font" );
		midElem.addContent( innerElem );
		for( int i=0; i<kWmsDocUrlPropertyNamesAndDefaults.length ; i++ ) {
			String attrName = kWmsDocUrlPropertyNamesAndDefaults[i][0];
			// If it's not one of the link properties then copy it over
			if( ! attrName.equals(WMS_DOC_URL_SHOULD_DISPLAY_ATTR)
				&& ! attrName.equals("target")
				&& ! attrName.equals("class")
			) {
				String attrValue = getWmsDocUrlPropertyOrNull( attrName );
				if( null!=attrValue )
					innerElem.setAttribute( attrName, attrValue );
			}
		}
		Element linkElem = new Element( "a" );
		linkElem.setAttribute( "href", href );
		if( null!=target )
			linkElem.setAttribute( "target", target );
		if( null!=classStr )
			linkElem.setAttribute( "class", classStr );
		linkElem.addContent( displayURL );
		innerElem.addContent( linkElem );

		// statusMsg( kFName, "URL element=" + NIEUtil.NL + JDOMHelper.JDOMToString(outElement, true));

		return outElement;
	}

	public Element generateWmsDocSummaryElementOrNull( SnURLRecord inURL ) {
		final String kFName = "generateWmsDocSummaryElementOrNull";
		if( null==inURL ) {
			errorMsg( kFName, "No element passed in, returning null.");
			return null;
		}

		String shouldDisplayStr = getWmsDocSummaryPropertyOrNull( WMS_DOC_SUMMARY_SHOULD_DISPLAY_ATTR );
		boolean shouldDisplay = NIEUtil.decodeBooleanString( shouldDisplayStr, DEFAULT_WMS_DOC_SUMMARY_SHOULD_DISPLAY );
		if( ! shouldDisplay ) {
			debugMsg( kFName, "Configured to not display, returning null.");
			return null;
		}

		// We'll need these IF we're creating a link
		String summary = inURL.getDescription();
		if( null==summary ) {
			errorMsg( kFName, "Null summary/description returned from URL object");
			return null;
		}

		// Mostly we're building an image element
		// Element outElement = new Element("div");
		// Element midElem = new Element( "small" );
		Element outElement = new Element( "small" );
		// Element outElement = new Element( "b" );
		// outElement.addContent( midElem );
		Element innerElem = new Element( "font" );
		// midElem.addContent( innerElem );
		outElement.addContent( innerElem );
		// outElement.addContent( innerElem );
		for( int i=0; i<kWmsDocSummaryPropertyNamesAndDefaults.length ; i++ ) {
			String attrName = kWmsDocSummaryPropertyNamesAndDefaults[i][0];
			// If it's not one of the link properties then copy it over
			if( ! attrName.equals(WMS_DOC_SUMMARY_SHOULD_DISPLAY_ATTR) ) {
				String attrValue = getWmsDocSummaryPropertyOrNull( attrName );
				if( null!=attrValue )
					innerElem.setAttribute( attrName, attrValue );
			}
		}
		innerElem.addContent( summary );

		// statusMsg( kFName, "Summary element=" + NIEUtil.NL + JDOMHelper.JDOMToString(outElement, true));

		return outElement;
	}



	// Will generate a <font> tag, or null if no slogan text
	public Element generateWmsSloganElement() {
		// Cache the answer the first time through
		if( null==cWmsSloganElement ) {
			final String kFName = "generateWmsSloganElement";
			String sloganText = getWmsSloganPropertyOrNull( SLOGAN_TEXT_ATTR );
			if( null==sloganText ) {
				errorMsg( kFName, "No slogan text, returning null.");
				return null;
			}
			cWmsSloganElement = new Element("font");
			for( int i=0; i<kWmsSloganPropertyNamesAndDefaults.length ; i++ ) {
				String attrName = kWmsSloganPropertyNamesAndDefaults[i][0];
				if( ! attrName.equals(SLOGAN_TEXT_ATTR)
					&& ! attrName.equals(PREFIX_TEXT_ATTR)
					&& ! attrName.equals(SUFFIX_TEXT_ATTR)
				) {
					String attrValue = getWmsSloganPropertyOrNull( attrName );
					if( null!=attrValue )
						cWmsSloganElement.setAttribute( attrName, attrValue );
				}
				else if( attrName.equals(PREFIX_TEXT_ATTR)
					|| attrName.equals(SUFFIX_TEXT_ATTR)
				) {
					debugMsg( kFName,
						"Ignorining currently unsupported attribute " + attrName
						);
				}
			}
			cWmsSloganElement.addContent( sloganText );
		}
		// return cWmsSloganElement;
		Element outElem = (Element)cWmsSloganElement.clone();
		return outElem;
	}
	public Element generateAltSloganElement() {
		// Cache the answer the first time through
		if( null==cAltSloganElement ) {
			final String kFName = "generateAltSloganElement";
			String sloganText = getAltSloganPropertyOrNull( SLOGAN_TEXT_ATTR );
			if( null==sloganText ) {
				errorMsg( kFName, "No slogan text, returning null.");
				return null;
			}
			cAltSloganElement = new Element("font");
			for( int i=0; i<kAltSloganPropertyNamesAndDefaults.length ; i++ ) {
				String attrName = kAltSloganPropertyNamesAndDefaults[i][0];
				if( ! attrName.equals(SLOGAN_TEXT_ATTR)
					&& ! attrName.equals(PREFIX_TEXT_ATTR)
					&& ! attrName.equals(SUFFIX_TEXT_ATTR)
				) {
					String attrValue = getAltSloganPropertyOrNull( attrName );
					if( null!=attrValue )
						cAltSloganElement.setAttribute( attrName, attrValue );
				}
				else if( attrName.equals(PREFIX_TEXT_ATTR)
					|| attrName.equals(SUFFIX_TEXT_ATTR)
				) {
					debugMsg( kFName,
						"Ignorining currently unsupported attribute " + attrName
						);
				}
			}
			// cAltSloganElement.addContent( sloganText );
			// Wrap the slogan in a bold tag
			Element boldElem = new Element( "b" );
			boldElem.addContent( sloganText );
			cAltSloganElement.addContent( boldElem );
		}
		// return cAltSloganElement;
		Element outElem = (Element)cAltSloganElement.clone();
		return outElem;
	}


	// List getURLObjects() to be defined in the derived classes
	public int getURLObjectsCount()
	{
		List objs = getURLObjects();
		if( objs != null )
			return objs.size();
		else
			return 0;
	}
	public boolean getHasURLObjects()
	{
		return getURLObjectsCount() > 0;
	}
	// Get a list of ONLY the Webmaster Suggests URL Objects
	// List getWmsURLObjects() to be defined in the derived classes
	// Get the list and ADD it to an existing list, do not add dupes
	// YOU supply the master list and master lookup hash
	// Returns the number added, -1 = hard failure
	// public int getWmsURLObjects( List ioMasterList, Hashtable ioMasterHash )
	public int getWmsURLObjectsCount()
	{
		if( fWmsURLCount < 0 ) {
			List tmpList = getWmsURLObjects();
			if( null!=tmpList )
				fWmsURLCount = tmpList.size();
			else
				fWmsURLCount = 0;
		}
		return fWmsURLCount;
	}
	public boolean getHasWmsURLs()
	{
		return getWmsURLObjectsCount() > 0;
	}


	// Get a list of ONLY the REDIRECT URL Objects
	// public List getRedirectURLObjects() in derived class
	public int getRedirectURLObjectsCount()
	{
		if( fRedirURLCount < 0 ) {
			List tmpList = getRedirectURLObjects();
			if( null!=tmpList )
				fRedirURLCount = tmpList.size();
			else
				fRedirURLCount = 0;
		}
		return fRedirURLCount;
	}
	public boolean getHasRedirURLs()
	{
		return getRedirectURLObjectsCount() > 0;
	}


	// Return strings, not objects
	// public List getAlternateTerms() in derived class
	// Get the list and ADD it to an existing list, do not add dupes
	// YOU supply the master list and master lookup hash
	// Returns the number added, -1 = hard failure
	// public int getAlternateTerms( List ioMasterList, Hashtable ioMasterHash )
	public int getAlternateTermsCount()
	{
		if( fAltTermsCount < 0 ) {
			List tmpList = getAlternateTerms();
			if( null!=tmpList )
				fAltTermsCount = tmpList.size();
			else
				fAltTermsCount = 0;
		}
		return fAltTermsCount;
	}
	public boolean getHasAlternateTerms()
	{
		return getAlternateTermsCount() > 0;
	}


	// Return a list of fully populated User Data Records
	// public List getUserDataItems()
	public int getUserDataItemsCount()
	{
		if( fUserDataItemsCount < 0 ) {
			List tmpList = getUserDataItems();
			if( null!=tmpList )
				fUserDataItemsCount = tmpList.size();
			else
				fUserDataItemsCount = 0;
		}
		return fUserDataItemsCount;
	}
	public boolean getHasUserDataItems()
	{
		return getUserDataItemsCount() > 0;
	}




	// TODO: this is ugly, we should not be traversing the main config's
	// jdom tree directly, we should have it do that, but OK for now I guess...
	public String getSystemLookFeelDefaultOrNull(
		String inMainPath, String inSubPath,
		String optDefaultValue
		)
	{
		final String kFName = "getSystemLookFeelDefaultOrNull";
		boolean trace = shouldDoTraceMsg(kFName);

		String outStr = null;

		if( null==inMainPath || null==inSubPath ) {
			errorMsg( kFName, "Null input(s), returning null. Values: inMainPath=" + inMainPath + ", inSubPath=" + inSubPath );
			return outStr;
		}

		// Fixup the path
		inMainPath =
			SearchTuningConfig.MAIN_SN_CONFIG_PATH
			+ "/default_" + inMainPath
			;

		// JDOMHelper mainElem = getMainElement();
		JDOMHelper mainElem = getMainSystemElement();
		if( null!=mainElem ) {
			if(trace) traceMsg( kFName,
				"Looking for System property \"" + inMainPath + " /@= " + inSubPath + "\""
				);
			outStr = mainElem.getTextFromSinglePathAttrTrimOrNull( inMainPath, inSubPath );
			if( null==outStr && null!=optDefaultValue )
				outStr = optDefaultValue;
		}
		else
			errorMsg( kFName, "No app config JDOM tree; returning null." );

		return outStr;
	}




	/***
	abandoned, this got really ugly and needed reflection or interfaces
	to pass a method as a parameter...
	void genericInitAPropertiesCache(
		String inSystemConfigPath,
		String [][] inPropertyNamesAndDefaults,
		Method inGetUserMethod,
		Hashtable inValuesCache
		)
	{
		for( int i=0 ; i<inPropertyNamesAndDefaults.length ; i++ ) {
			String [] propAndDef = inPropertyNamesAndDefaults[i];
			String propName = propAndDef[0];
			String possibleDefaultValue = propAndDef[1];

			// Get the value for this map
			String value = getWmsIconPropertyOrNull( propName );

			// If not, get the configured default value or,
			// if none, take the hard coded application value
			if( null==value )
				value = getSystemLookFeelDefaultOrNull(
					inSystemConfigPath,
					propName,
					possibleDefaultValue
					);

			// Cache the answer if we got one
			if( null!=value )
				inValuesCache.put( propName, value );
		}
	}
	***/



	void initWmsBoxProperties() {
		cWmsBoxPropertiesCache = new Hashtable();
		/***
		genericInitAPropertiesCache(
			WMS_BOX_DEFAULTS_PATH,
			kWmsBoxPropertyNamesAndDefaults,
			cWmsBoxPropertiesCache
			);
		***/
		cWmsBoxValidProperties = new HashSet();
		for( int i=0 ; i<kWmsBoxPropertyNamesAndDefaults.length ; i++ ) {
			String [] propAndDef = kWmsBoxPropertyNamesAndDefaults[i];
			String propName = propAndDef[0];
			String possibleDefaultValue = propAndDef[1];

			// Get the value for this map
			String value = getWmsBoxUserPropertyOrNull( propName );

			// If not, get the configured default value or,
			// if none, take the hard coded application value
			if( null==value )
				value = getSystemLookFeelDefaultOrNull(
					WMS_BOX_PATH,
					propName,
					possibleDefaultValue
					);

			// Cache the answer if we got one
			if( null!=value )
				cWmsBoxPropertiesCache.put( propName, value );
			// And record this as a valid property
			cWmsBoxValidProperties.add( propName );
		}

	}
	void initWmsIconProperties() {
		cWmsIconPropertiesCache = new Hashtable();
		/***
		genericInitAPropertiesCache(
			WMS_ICON_DEFAULTS_PATH,
			kWmsIconPropertyNamesAndDefaults,
			getWmsIconUserPropertyOrNull,
			cWmsIconPropertiesCache
			);
		***/
		cWmsIconValidProperties = new HashSet();
		for( int i=0 ; i<kWmsIconPropertyNamesAndDefaults.length ; i++ ) {
			String [] propAndDef = kWmsIconPropertyNamesAndDefaults[i];
			String propName = propAndDef[0];
			String possibleDefaultValue = propAndDef[1];

			// Get the value for this map
			String value = getWmsIconUserPropertyOrNull( propName );

			// If not, get the configured default value or,
			// if none, take the hard coded application value
			if( null==value )
				value = getSystemLookFeelDefaultOrNull(
					WMS_ICON_PATH,
					propName,
					possibleDefaultValue
					);

			// Cache the answer if we got one
			if( null!=value )
				cWmsIconPropertiesCache.put( propName, value );
			// And record this as a valid property
			cWmsIconValidProperties.add( propName );
		}
	}

	void initWmsDocTitleProperties() {
		cWmsDocTitlePropertiesCache = new Hashtable();
		cWmsDocTitleValidProperties = new HashSet();
		for( int i=0 ; i<kWmsDocTitlePropertyNamesAndDefaults.length ; i++ ) {
			String [] propAndDef = kWmsDocTitlePropertyNamesAndDefaults[i];
			String propName = propAndDef[0];
			String possibleDefaultValue = propAndDef[1];

			// Get the value for this map
			String value = getWmsDocTitleUserPropertyOrNull( propName );

			// If not, get the configured default value or,
			// if none, take the hard coded application value
			if( null==value )
				value = getSystemLookFeelDefaultOrNull(
					WMS_DOC_TITLE_PATH,
					propName,
					possibleDefaultValue
					);

			// Cache the answer if we got one
			if( null!=value )
				cWmsDocTitlePropertiesCache.put( propName, value );
			// And record this as a valid property
			cWmsDocTitleValidProperties.add( propName );
		}
	}

	void initWmsDocUrlProperties() {
		cWmsDocUrlPropertiesCache = new Hashtable();
		cWmsDocUrlValidProperties = new HashSet();
		for( int i=0 ; i<kWmsDocUrlPropertyNamesAndDefaults.length ; i++ ) {
			String [] propAndDef = kWmsDocUrlPropertyNamesAndDefaults[i];
			String propName = propAndDef[0];
			String possibleDefaultValue = propAndDef[1];

			// Get the value for this map
			String value = getWmsDocUrlUserPropertyOrNull( propName );

			// If not, get the configured default value or,
			// if none, take the hard coded application value
			if( null==value )
				value = getSystemLookFeelDefaultOrNull(
					WMS_DOC_URL_PATH,
					propName,
					possibleDefaultValue
					);

			// Cache the answer if we got one
			if( null!=value )
				cWmsDocUrlPropertiesCache.put( propName, value );
			// And record this as a valid property
			cWmsDocUrlValidProperties.add( propName );
		}
	}

	void initWmsDocSummaryProperties() {
		cWmsDocSummaryPropertiesCache = new Hashtable();
		cWmsDocSummaryValidProperties = new HashSet();
		for( int i=0 ; i<kWmsDocSummaryPropertyNamesAndDefaults.length ; i++ ) {
			String [] propAndDef = kWmsDocSummaryPropertyNamesAndDefaults[i];
			String propName = propAndDef[0];
			String possibleDefaultValue = propAndDef[1];

			// Get the value for this map
			String value = getWmsDocSummaryUserPropertyOrNull( propName );

			// If not, get the configured default value or,
			// if none, take the hard coded application value
			if( null==value )
				value = getSystemLookFeelDefaultOrNull(
					WMS_DOC_SUMMARY_PATH,
					propName,
					possibleDefaultValue
					);

			// Cache the answer if we got one
			if( null!=value )
				cWmsDocSummaryPropertiesCache.put( propName, value );
			// And record this as a valid property
			cWmsDocSummaryValidProperties.add( propName );
		}
	}


	void initWmsSloganProperties() {
		cWmsSloganPropertiesCache = new Hashtable();
		cWmsSloganValidProperties = new HashSet();
		for( int i=0 ; i<kWmsSloganPropertyNamesAndDefaults.length ; i++ ) {
			String [] propAndDef = kWmsSloganPropertyNamesAndDefaults[i];
			String propName = propAndDef[0];
			String possibleDefaultValue = propAndDef[1];

			// Get the value for this map
			String value = getWmsSloganUserPropertyOrNull( propName );

			// If not, get the configured default value or,
			// if none, take the hard coded application value
			if( null==value )
				value = getSystemLookFeelDefaultOrNull(
					WMS_SLOGAN_PATH,
					propName,
					possibleDefaultValue
					);

			// Cache the answer if we got one
			if( null!=value )
				cWmsSloganPropertiesCache.put( propName, value );
			// And record this as a valid property
			cWmsSloganValidProperties.add( propName );
		}
	}

	void initAltSloganProperties() {

		// debugMsg( "initAltSloganProperties", "************************** start" );

		cAltSloganPropertiesCache = new Hashtable();
		boolean cachingDefaults = false;
		if( null==cAltSloganDefaultsCache ) {
			cAltSloganDefaultsCache = new Hashtable();
			cachingDefaults = true;
		}
		cAltSloganValidProperties = new HashSet();
		for( int i=0 ; i<kAltSloganPropertyNamesAndDefaults.length ; i++ ) {
			String [] propAndDef = kAltSloganPropertyNamesAndDefaults[i];
			String propName = propAndDef[0];
			String possibleDefaultValue = propAndDef[1];

			// Get the value for this map
			String value = getAltSloganUserPropertyOrNull( propName );

			String defaultValue = null;
			if( cachingDefaults ) {
				// We also cache the system default
				defaultValue = getSystemLookFeelDefaultOrNull(
						ALT_SLOGAN_PATH,
						propName,
						possibleDefaultValue
						);
				// Cache the answer if we got one
				if( null!=defaultValue )
					cAltSloganDefaultsCache.put( propName, defaultValue );
			}
			else {
				defaultValue = genericGetPropertyFromCacheOrNull(
					cAltSloganDefaultsCache, null,
					propName, "cAltSloganDefaultsCache"
					);
			}


			// If not, get the configured default value or,
			// if none, take the hard coded application value
			if( null==value && null!=defaultValue )
				value = defaultValue;

			// Cache the answer if we got one
			if( null!=value )
				cAltSloganPropertiesCache.put( propName, value );
			// And record this as a valid property
			cAltSloganValidProperties.add( propName );
		}
	}



	abstract String getWmsIconUserPropertyOrNull( String inPropertyName );
	abstract String getWmsBoxUserPropertyOrNull( String inPropertyName );
	abstract String getWmsDocTitleUserPropertyOrNull( String inPropertyName );
	abstract String getWmsDocUrlUserPropertyOrNull( String inPropertyName );
	abstract String getWmsDocSummaryUserPropertyOrNull( String inPropertyName );



	abstract String getWmsSloganUserPropertyOrNull( String inPropertyName );

	abstract String getAltSloganUserPropertyOrNull( String inPropertyName );





	static String genericGetPropertyFromCacheOrNull(
		Hashtable inCache, HashSet optValidPropertyNames,
		String inPropertyName, String optCacheName
		)
	{
		final String kFName = "genericGetPropertyFromCacheOrNull";
		if( null==inCache ) {
			staticErrorMsg( kFName,
				"Null cache passed in for property \"" + inPropertyName + "\""
				+ " For cache '" + optCacheName + "'"
				+ " Returning null."
				);
			// throw new Error( "forcing stack dump." );
			// Runtime.getRuntime()
			// SecurityManager sm = System.getSecurityManager();
			// TODO: weird, this should not be reachable but
			// saw it a couple times around 3.0, maybe just weird state
			// of dev...  I added the name of the cache at least to
			// narrow it down if it happens again
			return null;
		}
		if( null==inPropertyName ) {
			staticErrorMsg( kFName, "Null property name passed in, returning null." );
			return null;
		}
		if( inCache.containsKey( inPropertyName ) )
			return (String)inCache.get( inPropertyName );

		if( null!=optValidPropertyNames && ! optValidPropertyNames.contains(inPropertyName) ) {
			staticErrorMsg( kFName,
				"Invalid property name: " + inPropertyName
				+ " Valid names: " + optValidPropertyNames
				);
			// throw new Error( "check attributes" );
			// Runtime.getRuntime().
			// throw new Error( "Showing where invalid property came from." );
		}

		return null;
	}


	public String getWmsIconPropertyOrNull( String inPropertyName ) {
		final String kFName = "getWmsIconPropertyOrNull";
		traceMsg( kFName, "Getting property \"" + inPropertyName + "\"" );
		return genericGetPropertyFromCacheOrNull(
			cWmsIconPropertiesCache, cWmsIconValidProperties,
			inPropertyName, "cWmsIconPropertiesCache"
			);
	}
	public String getWmsDocTitlePropertyOrNull( String inPropertyName ) {
		final String kFName = "getWmsDocTitlePropertyOrNull";
		traceMsg( kFName, "Getting property \"" + inPropertyName + "\"" );
		return genericGetPropertyFromCacheOrNull(
			cWmsDocTitlePropertiesCache, cWmsDocTitleValidProperties,
			inPropertyName, "cWmsDocTitlePropertiesCache"
			);
	}
	public String getWmsDocUrlPropertyOrNull( String inPropertyName ) {
		final String kFName = "getWmsDocUrlPropertyOrNull";
		traceMsg( kFName, "Getting property \"" + inPropertyName + "\"" );
		return genericGetPropertyFromCacheOrNull(
			cWmsDocUrlPropertiesCache, cWmsDocUrlValidProperties,
			inPropertyName, "cWmsDocUrlPropertiesCache"
			);
	}
	public String getWmsDocSummaryPropertyOrNull( String inPropertyName ) {
		final String kFName = "getWmsDocSummaryPropertyOrNull";
		traceMsg( kFName, "Getting property \"" + inPropertyName + "\"" );
		return genericGetPropertyFromCacheOrNull(
			cWmsDocSummaryPropertiesCache, cWmsDocSummaryValidProperties,
			inPropertyName, "cWmsDocSummaryPropertiesCache"
			);
	}

	public String getWmsBoxPropertyOrNull( String inPropertyName ) {
		final String kFName = "getWmsBoxPropertyOrNull";
		traceMsg( kFName, "Getting property \"" + inPropertyName + "\"" );
		return genericGetPropertyFromCacheOrNull(
			cWmsBoxPropertiesCache, cWmsBoxValidProperties,
			inPropertyName, "cWmsBoxPropertiesCache"
			);
	}


	public String getWmsSloganPropertyOrNull( String inPropertyName ) {
		final String kFName = "getWmsSloganPropertyOrNull";
		traceMsg( kFName, "Getting property \"" + inPropertyName + "\"" );
		return genericGetPropertyFromCacheOrNull(
			cWmsSloganPropertiesCache, cWmsSloganValidProperties,
			inPropertyName, "cWmsSloganPropertiesCache"
			);
	}


	public String getAltSloganPropertyOrNull( String inPropertyName ) {
		final String kFName = "getAltSloganPropertyOrNull";
		traceMsg( kFName, "Getting property \"" + inPropertyName + "\"" );
		return genericGetPropertyFromCacheOrNull(
			cAltSloganPropertiesCache, cAltSloganValidProperties,
			inPropertyName, "cAltSloganPropertiesCache"
			);
	}

	public static String getAltSloganDefaultPropertyOrNull( String inPropertyName ) {
		final String kFName = "getAltSloganDefaultPropertyOrNull";
		staticTraceMsg( kFName, "Getting default property \"" + inPropertyName + "\"" );
		return genericGetPropertyFromCacheOrNull(
			cAltSloganDefaultsCache, null,
			inPropertyName, "cAltSloganDefaultsCache"
			);
	}


	public SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
	}

	public JDOMHelper getMainElement()
	{
		return fMainElement;
	}

	public Element getJDomElement()
	{
		return fMainElement.getJdomElement();
	}

	public JDOMHelper getMainSystemElement()
	{
		return getMainConfig().getOverallMasterConfigTree();
	}
		


	// This gets us to the logging object
	static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}
	boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName(), inFromRoutine );
	}
	boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName(), inFromRoutine );
	}
	boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	static boolean staticTraceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName(), inFromRoutine );
	}
	boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	static boolean staticErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}


	private SearchTuningConfig fMainConfig;
	private JDOMHelper fMainElement;
	private int cID;

	// some cached counts
	private int fWmsURLCount = -1;
	private int fAltTermsCount = -1;
	private int fRedirURLCount = -1;
	private int fUserDataItemsCount = -1;

	Element cWmsSloganElement;
	Element cAltSloganElement;
	Element cWmsIconElement;
	Element cWmsDocUrlElement;
	Element [] cWmsBoxElements;


	public final static String SLOGAN_TEXT_ATTR = "text";

	// Text that goes before and after things
	public final static String PREFIX_TEXT_ATTR = "markup_before";
	public final static String SUFFIX_TEXT_ATTR = "markup_after";
	public final static String OUTER_CLASS_ATTR = "outer_class";
	public final static String BORDER_COLOR_ATTR = "border_color";





	public static final String DEFAULT_WMS_BOX_WIDTH = "95%";
	public static final String DEFAULT_WMS_BOX_BGCOLOR = "#eeeeee";
	public static final String DEFAULT_WMS_BOX_ALIGN = "left";
	public static final String DEFAULT_WMS_ICON_VSPACE = "10";
	public static final String DEFAULT_WMS_ICON_HSPACE = "10";

	public static final String DEFAULT_WMS_SLOGAN = "The Webmaster Suggests:";
	public static final String DEFAULT_WMS_SLOGAN_OPEN_TEXT = null;
	public static final String DEFAULT_WMS_SLOGAN_CLOSE_TEXT = null;
	public static final String DEFAULT_WMS_SLOGAN_FONT_COLOR = "#000088";
	public static final String DEFAULT_WMS_SLOGAN_FONT_FACE = null;
	public static final String DEFAULT_WMS_SLOGAN_FONT_SIZE = "+1";
	public static final String DEFAULT_WMS_SLOGAN_CSS_CLASS = CSSClassNames.WMS_SLOGAN_FONT;







	public static final String WMS_SLOGAN_PATH = "webmaster_suggests_header";

	public static String [][] kWmsSloganPropertyNamesAndDefaults = {
		{ SLOGAN_TEXT_ATTR, DEFAULT_WMS_SLOGAN },
		{ "face", DEFAULT_WMS_SLOGAN_FONT_FACE },
		{ "color", DEFAULT_WMS_SLOGAN_FONT_COLOR },
		{ "size", DEFAULT_WMS_SLOGAN_FONT_SIZE },
		{ PREFIX_TEXT_ATTR, DEFAULT_WMS_SLOGAN_OPEN_TEXT },
		{ SUFFIX_TEXT_ATTR, DEFAULT_WMS_SLOGAN_CLOSE_TEXT },
		{ "class", CSSClassNames.WMS_SLOGAN_FONT }
	};
	HashSet cWmsSloganValidProperties;
	Hashtable cWmsSloganPropertiesCache;

	public static final String DEFAULT_ALT_SLOGAN = "See also:";
	public static final String DEFAULT_ALT_SLOGAN_OPEN_TEXT = null;
	public static final String DEFAULT_ALT_SLOGAN_CLOSE_TEXT = null;
	public static final String DEFAULT_ALT_SLOGAN_FONT_COLOR = "#880000";
	public static final String DEFAULT_ALT_SLOGAN_FONT_FACE = null;
	public static final String DEFAULT_ALT_SLOGAN_FONT_SIZE = null;
	public static final String DEFAULT_ALT_SLOGAN_CSS_CLASS = CSSClassNames.ALT_SLOGAN_FONT;

	// TODO: Make this consistent with WMS headER vs headING
	// and suggests vs suggestions (maybe change other on that)
	public static final String ALT_SLOGAN_PATH = "alternate_suggestions_heading";


	public static String [][] kAltSloganPropertyNamesAndDefaults = {
		{ SLOGAN_TEXT_ATTR, DEFAULT_ALT_SLOGAN },
		{ "face", DEFAULT_ALT_SLOGAN_FONT_FACE },
		{ "color", DEFAULT_ALT_SLOGAN_FONT_COLOR },
		{ "size", DEFAULT_ALT_SLOGAN_FONT_SIZE },
		{ PREFIX_TEXT_ATTR, DEFAULT_ALT_SLOGAN_OPEN_TEXT },
		{ SUFFIX_TEXT_ATTR, DEFAULT_ALT_SLOGAN_CLOSE_TEXT },
		{ "class", CSSClassNames.ALT_SLOGAN_FONT }
	};
	Hashtable cAltSloganPropertiesCache;
	static Hashtable cAltSloganDefaultsCache;
	HashSet cAltSloganValidProperties;
	// TODO: ^^^ this should be static too



	public final static String WMS_BOX_PATH = "webmaster_suggests_box";

	public static String [][] kWmsBoxPropertyNamesAndDefaults = {
		{ "width", DEFAULT_WMS_BOX_WIDTH },	// Usually something like "95%"
		{ "border", null },
		{ BORDER_COLOR_ATTR, null },
		{ "cellpadding", null },		// IE defaults to 1
		{ "cellspacing", null },		// IE defaults to 1
		{ "bgcolor", DEFAULT_WMS_BOX_BGCOLOR },
		{ "align", DEFAULT_WMS_BOX_ALIGN },
		{ "background", null },
		{ OUTER_CLASS_ATTR, CSSClassNames.WMS_BOX1 },
		{ "class", CSSClassNames.WMS_BOX2 }		// probably they want inner class
	};

	Hashtable cWmsBoxPropertiesCache;
	HashSet cWmsBoxValidProperties;

	public final static String WMS_ICON_PATH = "webmaster_suggests_icon";

	public static String [][] kWmsIconPropertyNamesAndDefaults = {
		{ "src", null },
		{ "alt", null },
		{ "href", null },
		{ "target", null },
		{ "width", null },
		{ "height", null },
		{ "border", "0" },	// I can't imagine ever changing this
		{ "vspace", DEFAULT_WMS_ICON_VSPACE },	// Usually something like "10"
		{ "hspace", DEFAULT_WMS_ICON_HSPACE },	// Usually something like "10"
		{ "class", CSSClassNames.WMS_ICON }
	};

	Hashtable cWmsIconPropertiesCache;
	HashSet cWmsIconValidProperties;

	// Title
	public static final String WMS_DOC_TITLE_PATH = "webmaster_suggests_doc_title";
	public static final String WMS_DOC_TITLE_SHOULD_DISPLAY_ATTR = "should_display";
	// ^^^ not yet implemented
	// public static final boolean DEFAULT_WMS_DOC_URL_SHOULD_DISPLAY = true;
	public static final String DEFAULT_WMS_DOC_TITLE_SHOULD_DISPLAY_STR = "true";
	public static final boolean DEFAULT_WMS_DOC_TITLE_SHOULD_DISPLAY = true;
	public static String [][] kWmsDocTitlePropertyNamesAndDefaults = {
		{ WMS_DOC_TITLE_SHOULD_DISPLAY_ATTR, DEFAULT_WMS_DOC_TITLE_SHOULD_DISPLAY_STR },
		{ "target", null },
		{ "face", null },
		{ "color", null },
		{ "size", null },
		{ PREFIX_TEXT_ATTR, null },	// TODO: maybe use these for <small><i>
		{ SUFFIX_TEXT_ATTR, null },
		{ "class", null }
		// Todo: could add lots more, like font, css, etc
	};
	Hashtable cWmsDocTitlePropertiesCache;
	HashSet cWmsDocTitleValidProperties;

	// URL
	public static final String WMS_DOC_URL_PATH = "webmaster_suggests_doc_url";
	public static final String WMS_DOC_URL_SHOULD_DISPLAY_ATTR = "should_display";
	// public static final boolean DEFAULT_WMS_DOC_URL_SHOULD_DISPLAY = true;
	public static final String DEFAULT_WMS_DOC_URL_SHOULD_DISPLAY_STR = "true";
	public static final boolean DEFAULT_WMS_DOC_URL_SHOULD_DISPLAY = true;
	public static String [][] kWmsDocUrlPropertyNamesAndDefaults = {
		{ WMS_DOC_URL_SHOULD_DISPLAY_ATTR, DEFAULT_WMS_DOC_URL_SHOULD_DISPLAY_STR },
		{ "target", null },
		{ "face", null },
		{ "color", null },
		{ "size", null },
		{ PREFIX_TEXT_ATTR, null },	// TODO: maybe use these for <small><i>
		{ SUFFIX_TEXT_ATTR, null },
		{ "class", null }
		// Todo: could add lots more, like font, css, etc
	};
	Hashtable cWmsDocUrlPropertiesCache;
	HashSet cWmsDocUrlValidProperties;


	// Summary
	public static final String WMS_DOC_SUMMARY_PATH = "webmaster_suggests_doc_summary";
	public static final String WMS_DOC_SUMMARY_SHOULD_DISPLAY_ATTR = "should_display";
	public static final String DEFAULT_WMS_DOC_SUMMARY_SHOULD_DISPLAY_STR = "true";
	public static final boolean DEFAULT_WMS_DOC_SUMMARY_SHOULD_DISPLAY = true;
	public static String [][] kWmsDocSummaryPropertyNamesAndDefaults = {
		{ WMS_DOC_SUMMARY_SHOULD_DISPLAY_ATTR, DEFAULT_WMS_DOC_SUMMARY_SHOULD_DISPLAY_STR },
		// { "target", null },
		{ "face", null },
		{ "color", null },
		{ "size", null },
		{ PREFIX_TEXT_ATTR, null },	// TODO: maybe use these for <small><i>
		{ SUFFIX_TEXT_ATTR, null },
		{ "class", null }
		// Todo: could add lots more, like font, css, etc
	};
	Hashtable cWmsDocSummaryPropertiesCache;
	HashSet cWmsDocSummaryValidProperties;

};

