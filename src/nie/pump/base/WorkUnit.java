//package nie.core;
package nie.pump.base;

import java.io.*;
import java.util.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.CDATA;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import nie.core.*;


public class WorkUnit implements Cloneable
{
	static final String kStaticClassName = "WorkUnit";

//	/////////////////////////////////
//	//
//	// This is the exception that is
//	// thrown if there's a problem with
//	// something
//	//
//	/////////////////////////////////
//
//	public class WorkUnitException extends Exception
//	{
//		public WorkUnitException( String inMessage )
//		{
//			super( inMessage );
//		}
//	}


	// public String kClassName() { return "WorkUnit" };

	// Constructors
	// ============
	private void __sep__Constructors__() {}

	// Constructor # 1: default
	public WorkUnit()
		throws WorkUnitException
	{
		final String kFName = "constructor(1)";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";

		// init our member variable with a JDOM instance
		try
		{
			jh = new JDOMHelper( XML_TEMPLATE, null );
		}
		catch(JDOMHelperException e)
		{
			throw new WorkUnitException( kExTag +
				"got JDOMHelperException: " + e
				);
		}

	}

	// Constructor # 2 used more for testing
	// Read the work unit in from an external file or URL
	public WorkUnit( String uri )
		throws WorkUnitException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";

		try
		{
			jh = new JDOMHelper( uri );
		}
		catch(JDOMHelperException e)
		{
			throw new WorkUnitException( kExTag +
				"got JDOMHelperException: " + e
				);
		}
	}

	// Constructor # 3 used more for testing
	// Read the work unit in from an external file or URL
	public WorkUnit( File inFile )
		throws WorkUnitException
	{
		final String kFName = "constructor(3)";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";

		try
		{
			jh = new JDOMHelper( inFile );
		}
		catch(JDOMHelperException e)
		{
			throw new WorkUnitException( kExTag +
				"got JDOMHelperException: " + e
				);
		}
	}

	// Do a "deep" clone
	public synchronized Object clone()
	{
		final String kFName = "clone";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";

		// Cloning should take place in a try block
		try
		{
			// Get a default clone of myself
			WorkUnit me2 = (WorkUnit)(super.clone());

			// Duplicate mutable member variables
			if( jh != null )
				synchronized( jh )
				{
					me2.jh = (JDOMHelper)(jh.clone());
				}

			return me2;

		}
		catch (CloneNotSupportedException e)
		{
			staticErrorMsg( kFName, "clone failed: " + e );
			// return this;
			return null;
		}

	}





	/************************************************
	 *
	 *		Simple get/set items
	 *
	 ************************************************/
	private void __sep__Simple_Get_Set_Methods__() {}

	public boolean getIsValidRecord()
	{
		// Look for a tag we hope is NOT there.
		Element tmp = jh.findElementByPath(
			INVALID_MARKER_PATH
			// "processing_information/invalid_record"
			);
		// It's GOOD if we did NOT find the tag.
		return tmp == null;
	}

	Element getProcessInfoElem()
	{
		if( null!=cProcInfoElem )
			return cProcInfoElem;
		cProcInfoElem = jh.getJdomElement().getChild( PROCESS_INFO_TAG );
		return cProcInfoElem;
	}

	public void setIsValidRecord( boolean validFlag )
	{
		// If already set correctly, nothing needs to be done
		if( getIsValidRecord() == validFlag )
			return;

		// The case where we want to mark it as invalid
		// we want to add the singleton tag
		if( ! validFlag )
		{
			JDOMHelper.findOrCreateElementByPath( jh.getJdomElement(), INVALID_MARKER_PATH, true );

			// Element tmpNode = jh.addElementToPath(
				// PROCESS_INFO_TAG,
				// new Element(INVALID_MARKER_TAG)
				// );
			// Element tmpNode = jh.addElementToPath(
				// "processing_information",
				// new Element("invalid_record")
				// );
		}
		// Else it is valid, and we have a tag saying that it isn't
		// so we need to remove it
		else
		{
			// jh.removeElementByPath( "processing_information/invalid_record" );
			jh.removeElementByPath( INVALID_MARKER_PATH );
		}
	}

	public boolean getIsBackwash()
	{
		// Look for a tag we hope is NOT there.
		Element tmp = jh.findElementByPath(
			"processing_information/backwash"
			);
		// It's being backwashed if we DID find the tag.
		return tmp != null;
	}

	public void setIsBackwash( boolean validFlag )
	{
		// If already set correctly, nothing needs to be done
		if( getIsBackwash() == validFlag )
			return;

		// The case where we want to mark it as invalid
		// we want to add the singleton tag
		if( ! validFlag )
		{
			Element tmpNode = jh.addElementToPath(
				"processing_information",
				new Element("backwash")
				);
		}
		// Else it is valid, and we have a tag saying that it isn't
		// so we need to remove it
		else
		{
			jh.removeElementByPath( "processing_information/backwash" );
		}
	}

	/***************************************************
	 *
	 *		Runpath related
	 *
	 ***************************************************/
	private void __sep__Runpath_Related__() {}

	// Given a process's ID, find it on the stack

	public int getRunpathCount()
	{
		// RUN_PATH_TAG, INDIVIDUAL_RUN_PATH_ENTRY_TAG, PROCESSOR_ID_ATTR

		// Element runPath = jh.findElementByPath( "runpath" );
		Element runPath = jh.findElementByPath( RUN_PATH_TAG );
		if( runPath == null )
			return 0;
		// List entries = runPath.getChildren( "runpath_entry" );
		List entries = runPath.getChildren( INDIVIDUAL_RUN_PATH_ENTRY_TAG );
		return entries.size();
	}

	public Element findRunpathElement( Processor inProc )
	{
		String processID = inProc.getID();
		return findRunpathElement( processID );
	}

	public Element findRunpathElement( String processID )
	{
		// Look for the LAST runpath_entry in the runpath
		// stack who's procid attribute matches the target

		// RUN_PATH_TAG, INDIVIDUAL_RUN_PATH_ENTRY_TAG, PROCESSOR_ID_ATTR

		// Final true arg tells it to search BACKWARDS, which is important
		// return jh.mixedQuery( "runpath", "runpath_entry",
		//	"procid", processID, true
		//	);
		return jh.mixedQuery( RUN_PATH_TAG, INDIVIDUAL_RUN_PATH_ENTRY_TAG,
			PROCESSOR_ID_ATTR, processID, true
			);
	}

	public Element getRunpathElementState( String processID )
	{
		Element entry = findRunpathElement( processID );
		if( entry == null )
			return null;
		return entry.getChild( "state_info" );
	}

	// Set (reset) the state_info element of a processes entry
	// in runpath.
	// Return the new state on success, null on failure

	public Element setRunpathElementState( String processID, Element newState )
	{
		Element oldEntry = getRunpathElementState( processID );
		// Get rid of any old entry
		if( oldEntry != null )
		{
			// Remove the old one, bail if it fails
			if( ! oldEntry.getParent().removeContent( oldEntry ) )
				return null;
		}
		Element parent = findRunpathElement( processID );
		if( parent == null )
			return null;

		try
		{
			parent.addContent( newState );
		}
		catch (Exception e)
		{
			return null;
		}
		return newState;

	}

	public Element createRunpathEntry( String inProcessorName )
	{
		// RUN_PATH_TAG, INDIVIDUAL_RUN_PATH_ENTRY_TAG, PROCESSOR_ID_ATTR

		// Element lTmpNode = new Element( "runpath_entry" );
		Element lTmpNode = new Element( INDIVIDUAL_RUN_PATH_ENTRY_TAG );

		if( null != lTmpNode )
		{
			// lTmpNode.setAttribute("procid", inProcessorName );
			lTmpNode.setAttribute( PROCESSOR_ID_ATTR, inProcessorName );
		}
		// jh.addElementToPath( "runpath", lTmpNode );
		jh.addElementToPath( RUN_PATH_TAG, lTmpNode );

		return lTmpNode;
	}

	// Return all the messages for a given id
	public List getMessagesForProcess( String processID )
	{
		// Find that process
		Element srcProc = findRunpathElement( processID );
		if( srcProc == null )
			return null;

		// PROCESSOR_MESSAGES_SUBTAG, INDIVIDUAL_MESSAGE_TAG

		// Get it's messages subelement
		// Element msgs = srcProc.getChild( "messages" );
		Element msgs = srcProc.getChild( PROCESSOR_MESSAGES_SUBTAG );
		if( msgs == null )
			// return new Vector();
			return null;
		// Have JDom do the final lookup
		// return msgs.getChildren( "message" );
		return msgs.getChildren( INDIVIDUAL_MESSAGE_TAG );
	}

	// Return all the messages for a given id for a level
	public List _getMessagesForProcess( String processID, String level )
	{
		List retList = new Vector();
		Element srcProc = findRunpathElement( processID );
		if( srcProc == null )
			return retList;

		// PROCESSOR_MESSAGES_SUBTAG, INDIVIDUAL_MESSAGE_TAG

		// Query for the matching messages
		// retList = JDOMHelper.mixedListQuery( srcProc,
		//	"messages",  "message",
		//	"level", level, false
		//	);
		retList = JDOMHelper.mixedListQuery( srcProc,
			PROCESSOR_MESSAGES_SUBTAG, INDIVIDUAL_MESSAGE_TAG,
			"level", level, false
			);

		return retList;
	}

	// Return all the messages for ALL the processes
	// TODO: rewrite with XPath //
	public List _getAllMessages()
	{
		List retList = new Vector();
		List tmpList1 = jh.findElementsByPath(
			"runpath/runpath_entry"
			);

		// Sanity check
		if( tmpList1==null || tmpList1.size()<1 )
			return retList;

		// For each runpath entry
		for( int i=0; i<tmpList1.size(); i++ )
		{
			Element entry = (Element)tmpList1.get(i);
			// Find all the messages for this runpath entry
			List tmpList2 = JDOMHelper.findElementsByPath( entry,
				"messages/message"
				);
			if( tmpList2==null || tmpList2.size()<1 )
				continue;
			// Now add these to the master list
			for( int j=0; j<tmpList2.size(); j++ )
				retList.add( (Element)tmpList2.get(j) );
		}

		return retList;

	}


	// Return all the messages for ALL the processes
	// for a given level
	public List _getAllMessages( String level )
	{
		List retList = new Vector();
		List tmpList1 = jh.findElementsByPath(
			"runpath/runpath_entry"
			);

		// Sanity check
		if( tmpList1==null || tmpList1.size()<1 )
			return retList;

		// For each runpath entry
		for( int i=0; i<tmpList1.size(); i++ )
		{
			Element entry = (Element)tmpList1.get(i);
			// Find all the main message tag for this runpath entry

			// Query for the matching messages
			List tmpList2 = JDOMHelper.mixedListQuery( entry,
				"messages",  "message",
				"level", level, false
				);

			if( tmpList2==null || tmpList2.size()<1 )
				continue;

			// Now add these to the master list
			for( int j=0; j<tmpList2.size(); j++ )
				retList.add( (Element)tmpList2.get(j) );
		}

		return retList;

	}

	// BE CAREFUL WITH THIS ONE!!!
	// Maybe useful if you're storing or sending work units and
	// couldn't backwash anyways
	public int nukeRunpath()
	{
		// Element runPath = jh.findElementByPath( "runpath" );
		Element runPath = jh.findElementByPath( RUN_PATH_TAG );
		if( runPath == null )
			return 0;
		// List entries = runPath.getChildren( "runpath_entry" );
		List entries = runPath.getChildren( INDIVIDUAL_RUN_PATH_ENTRY_TAG );
		int numEntries = entries.size();
		// runPath.removeChildren();
		runPath.getChildren().clear();
		return numEntries;
	}

	// Todo:
	//public Element addMessage( String processID,
	//int level, String Message,
	//Element optionalFields );
	// Also scratchpad stuff

	/********************************************************
	 *
	 *		Some convenience functions per Kevin's request
	 *
	 ********************************************************/
	private void __sep__Convenience_Functions_per_Kevin__ () {}

	public Element addElement( String inElementPath, Element inElement )
	{
		return jh.addElementToPath( inElementPath, inElement  );
	}
	public Element addElement( String inElementPath,
		String inElementAsXMLString
		)
	{
		return jh.addXMLTextToPath( inElementPath, inElementAsXMLString );
	}
	public Element deleteElement( String inElementPath )
	{
		return jh.removeElementByPath( inElementPath );
	}

	public Element findElement( String inElementPath )
	{
		return jh.findElementByPath( inElementPath );
	}

	public WorkUnit Clone() throws CloneNotSupportedException
	{
		return (WorkUnit)this.clone();
	}

	/***************************************************
	 *
	 *		User Data / Record Section
	 *
	 ***************************************************/
	private void __sep__User_Data_Section__() {}

	// Get the base user data section
	// keeps folks from needing to use USER_DATA_PATH
	// not public
	public Element getUserData()
	{
		return jh.findElementByPath( USER_DATA_PATH );
		// Todo: should we create it if it doesn't exist?
	}

	// do we recognize this as a normalized field centric record
	// Similar to getIsUserDataFreeForm, but they both
	// return false if there is no data
	public boolean getIsUserDataFlat()
	{

		// Quick test, if we're SURE it's freeform then it sure isn't flat
		if( getIsUserDataFreeForm() )
			return false;

		// Look for <field>'s
		// List fieldChildren = getUserData().getChildren( "field" );
		List fieldChildren = getUserData().getChildren( FIELD_TAG );

		// If there are no field children, then no
		if( fieldChildren.size() < 1 )
			return false;

		// Look at all the children
		List allChildren = getUserData().getChildren();

		// If we have children that are not fields, then
		// this is NOT a flat record
		if( allChildren.size() != fieldChildren.size() )
			return false;
		// Else they are the same, assume it's one of ours
		else
			return true;
	}

	// Do we have free form (non <field> tags) data?
	// If NO data, return false
	// Similar to getIsUserDataFlat, but they both
	// return false if there is no data
	public boolean getIsUserDataFreeForm()
	{

		// First, check if we've specifically marked it as freeform
		if( JDOMHelper.getBooleanFromAttribute( getUserData(),
			FREE_FORM_ATTR_NAME, false ) )
		{
			return true;
		}

		// Look at all the children
		List allChildren = getUserData().getChildren();
		// If there are no field children, then no
		if( allChildren.size() < 1 )
			return false;
		// Look for <field>'s
		List fieldChildren = getUserData().getChildren( FIELD_TAG );
		// If we have children that are not fields, then
		// this is NOT a flat record
		if( allChildren.size() == fieldChildren.size() )
			return false;
		// Else they are the same, assume it's one of ours
		else
			return true;

	}


	// This causes conversion and marks it for sure
	public boolean forceUserDataToFreeForm()
	{
		boolean wasAlready = getIsUserDataFreeForm();
		boolean retValue = true;
		// If it wasn't already, we need to convert it
		if( ! wasAlready )
		{
			// autoconvert should return the new node create
			// but if it returns null there was a problem
			if( autoConvertFlatRecordToTree() == null )
				retValue = false;
			else
			{
				// Else OK, go ahead and set it
				getUserData().setAttribute( FREE_FORM_ATTR_NAME, "1" );
			}
		}
		else
		{
			// set it just to be on the safe side
			getUserData().setAttribute( FREE_FORM_ATTR_NAME, "1" );
		}
		return retValue;
	}

	// find out how many <field> fields we already have
	// Will return a 1 if it's a user data tree
	public int getUserFlatFieldCount()
	{
		// First, get the list of fields
		List fields = jh.findElementsByPath( USER_DATA_PATH + '/' + FIELD_TAG );
		// then return the count
		return fields.size();
	}

	public boolean getIsUserDataEmpty()
	{
		// Look for <field>'s
		List fieldChildren = getUserData().getChildren();

		// If there are no field children, then no
		if( fieldChildren.size() < 1 )
			return true;
		else
			return false;
	}

	// Get the list of user fields
	public List getAllFlatFields()
	{
		List retList = new Vector();

		if( ! getIsUserDataFlat() )
			return retList;

		retList = jh.findElementsByPath( USER_DATA_PATH + '/' + FIELD_TAG );

		return retList;
	}

	private static boolean getIsAFlatFieldName( String inName )
	{
		if( inName == null || inName.trim().equals("") )
		{
			System.err.println( "Error: WorkUnit:getIsAFlatFieldName:"
				+ " Null/empty argument passed in"
				+ ", returning FALSE"
				);
			return false;
		}
		int slashAt = inName.indexOf( '/' );
		int leftBracketAt = inName.indexOf( '[' );
		int rightBracketAt = inName.indexOf( ']' );

		if( slashAt >= 0 || leftBracketAt >= 0 || rightBracketAt >= 0 )
			return false;
		else
			return true;

	}

	// Given an individual field, tell us if it's "flat style" or not
	private static boolean getIsAFlatElement( Element inElem )
	{
		if( inElem == null )
		{
			System.err.println( "Error: WorkUnit:getIsAFlatElement:"
				+ " Null argument passed in"
				+ ", returning FALSE"
				);
			return false;
		}

		// Figure out if we need to autoconvert
		String tmpElemName = inElem.getName();
		if( tmpElemName == null )
		{
			System.err.println( "Error: WorkUnit:getIsAFlatElement:"
				+ " Null element name"
				+ ", returning FALSE"
				);
			return false;
		}
		String tmpNameAttr = inElem.getAttributeValue( FIELD_NAME_ATTR );

		if( tmpElemName.trim().toLowerCase().equals(
					DEFAULT_USER_DATA_TREE_ELEMENT_NAME
				)
			&& tmpNameAttr != null
			&& ! tmpNameAttr.trim().equals("")
			)
		{
			return true;
		}
		else
			return false;
	}

	// Given the name of a field, look it up by name
	public Element getUserField( String inFieldName )
	{
		// Sanity check
		if( inFieldName==null )
			return null;

		// For flat records, we look for matching <field> tags
		// with a matching name="xyz" attribute
		if( getIsUserDataFlat() )
		{
			// Normalize the name
			inFieldName = inFieldName.trim().toLowerCase();
			// Sanity check
			if( inFieldName.equals("") )
				return null;

			// Look for the first matching field with that name
			return jh.mixedQuery( USER_DATA_PATH,
				FIELD_TAG, FIELD_NAME_ATTR, inFieldName, false
				);

		}
		// Else it's NOT flat, look it up in their tree
		else
		{

			return JDOMHelper.findElementByPath( getUserData(), inFieldName );

		}
	}



	// Given the name of a field, look it up by name
	// Return all matching elements
	// Will always return a list, perhaps with zero elements
	// Presumably if flat you'll get at one most, but you never know!
	public List getUserFields( String inFieldName )
	{
		final boolean debug = false;

		// Sanity check
		if( inFieldName == null || inFieldName.trim().equals("") )
		{
			System.err.println( "ERROR: WorkUnit:getUserFields:"
				+ " Null/empty field name passed in"
				);
			return new Vector();
		}

		if(debug) System.err.println( "Debug: WorkUnit:getUserFields:"
			+ " field name='" + inFieldName + "'"
			);

		// For flat records, we look for matching <field> tags
		// with a matching name="xyz" attribute
		if( getIsUserDataFlat() )
		{
			if(debug) System.err.println( "Debug: WorkUnit:getUserFields:"
				+ " looking for flat fields."
				);

			// Normalize the name
			inFieldName = inFieldName.trim().toLowerCase();
			// Sanity check
			// taken care of above
			// if( inFieldName.equals("") )
			//	return new Vector();

			// Look for the first matching field with that name
			return jh.mixedListQuery( USER_DATA_PATH,
				FIELD_TAG, FIELD_NAME_ATTR, inFieldName, false
				);

		}
		// Else it's NOT flat, look it up in their tree
		else
		{
			if(debug) System.err.println( "Debug: WorkUnit:getUserFields:"
				+ " looking for free form fields."
				);

			return JDOMHelper.findElementsByPath( getUserData(), inFieldName );

		}
	}

	// Get the text of a user field, by name
	// Null if not found
	// NOT trimmed
	public String getUserFieldText( String inFieldName )
	{
		Element field = getUserField( inFieldName );
		if( field == null )
			return null;
		return field.getText();
	}

	// Get the text of a user field
	// Always return a valid string, even if field not found
	// Always trim string before returning it
	public String getUserFieldTextSafeTrim( String inFieldName )
	{
		String data = getUserFieldText( inFieldName );
		if( data == null )
			data = "";
		else
			data = data.trim();
		return data;
	}



	// Get back a list of all matching field values
	// Will always return a list
	// Will NOT values
	// WILL include empty strings if that's what we got back
	// WARNING: If you use free form text and the matching
	// nodes are not simple text nodes, this will just bring
	// back the top level text, perhaps not what you want
	public List getUserFieldsText( String inFieldName )
	{

		List retList = new Vector();

		// Sanity check
		if( inFieldName==null )
			return retList;

		// For flat records, we look for matching <field> tags
		// with a matching name="xyz" attribute
		if( getIsUserDataFlat() )
		{
			// Normalize the name
			inFieldName = inFieldName.trim().toLowerCase();
			// Sanity check
			if( inFieldName.equals("") )
				return retList;

			// Look for the first matching field with that name
			List fields = jh.mixedListQuery( USER_DATA_PATH,
				FIELD_TAG, FIELD_NAME_ATTR, inFieldName, false
				);

			if( fields == null || fields.size()<1 )
				return retList;

			Iterator it = fields.iterator();
			while( it.hasNext() )
			{
				Element field = (Element)it.next();
				if( field==null )
					continue;
				String content = field.getText();
				if( content == null )
					content = "";
				retList.add(content);
			}

			return retList;

		}
		// Else it's NOT flat, look it up in their tree
		else
		{

			// Let JDOMHelper do the rest
			return JDOMHelper.getTextListByPath(
				getUserData(), inFieldName
				);

		}

	}


	// Get back a list of all matching field values
	// Will always return a list
	// Will have trimmed all values
	// Will not include null strings
	// WARNING: If you use free form text and the matching
	// nodes are not simple text nodes, this will just bring
	// back the top level text, perhaps not what you want
	public List getUserFieldsTextNotNullSafeTrim( String inFieldName )
	{

		List retList = new Vector();

		// Sanity check
		if( inFieldName==null )
			return retList;

		// For flat records, we look for matching <field> tags
		// with a matching name="xyz" attribute
		if( getIsUserDataFlat() )
		{
			// Normalize the name
			inFieldName = inFieldName.trim().toLowerCase();
			// Sanity check
			if( inFieldName.equals("") )
				return retList;

			// Look for the first matching field with that name
			List fields = jh.mixedListQuery( USER_DATA_PATH,
				FIELD_TAG, FIELD_NAME_ATTR, inFieldName, false
				);

			if( fields == null || fields.size()<1 )
				return retList;

			Iterator it = fields.iterator();
			while( it.hasNext() )
			{
				Element field = (Element)it.next();
				if( field==null )
					continue;
				String content = field.getTextTrim();
				if( content == null || content.equals("") )
					continue;
				retList.add(content);
			}

			return retList;

		}
		// Else it's NOT flat, look it up in their tree
		else
		{

			// Let JDOMHelper do the rest
			return JDOMHelper.getTextListByPathNotNullTrim(
				getUserData(), inFieldName
				);

		}
	}

	// Add a generic field, of the form field_1, field_2, etc.
	// Basically we conjure up a name and then add it as a named field.
	public Element addAnonymousField( String inFieldValue )
	{

		if( inFieldValue == null )
			inFieldValue = "";

		// This is actually simpler for free form data
		if( getIsUserDataFreeForm() )
		{
			return addNamedField( DEFAULT_USER_DATA_TREE_ELEMENT_NAME,
				inFieldValue
				);
		}

		// Make up one or more names, use the first name
		// that is not already in use

		// First, find out how many fields we already have
		List fields = jh.findElementsByPath( USER_DATA_PATH + "/field" );
		// Start our naming one after, so if there are 2 fields, we will
		// start at field_3 as the first candidate name
		int fieldSuffix = getUserFlatFieldCount()+1;

		Element retElem = null;

		// Keep trying names until we're done
		while( true )
		{
			// Conjure a candidate name
			String candidateName = AUTO_FIELDNAME_PREFIX + fieldSuffix;
			// See if it's in use by looking it up
			// If the lookup returns null then it's NOT already in use
			if( getUserField( candidateName ) == null )
			{
				// Not in use, let's go ahead and add it
				retElem = addNamedField( candidateName, inFieldValue );
				// And now we're all done
				break;
			}
			// Keep trying, increment the numerical suffix _n
			fieldSuffix++;
		}

		// Return the new element that was added
		return retElem;
	}

	// Given a field name and value, add the field
	public Element addNamedField( String inFieldName, String inFieldValue )
	{
		return addNamedField( inFieldName, inFieldValue,
			DEFAULT_AUTO_CDATA_THRESHOLD
			);
	}

	// Given a field name and value, add the field
	// If auto cdata >= 0, use that as the threshold for when to
	// use cdata
	// default is currently 1024
	// -1 means never
	// 0 means always
	public Element addNamedField( String inFieldName, String inFieldValue,
			int inAutoCDataThreshold
			)
	{

		final boolean debug = false;

		if( inFieldName==null || inFieldValue==null )
		{
			System.err.println( "Error: WorkUnit:addNamedField2:"
				+ " null value(s) passed in"
				+ ", fieldname=" + inFieldName
				+ ", value=" + inFieldValue
				);
			return null;
		}

		boolean tmpIsFlat = getIsUserDataFlat();
		boolean tmpIsFreeForm = getIsUserDataFreeForm();
		boolean tmpIsEmpty = getIsUserDataEmpty();
		boolean tmpIsFlatName = getIsAFlatFieldName( inFieldName );
		if(debug) System.out.println(
			"Debug: WorkUnit:addNamedField2:"
			+ " inFieldName=" + inFieldName
			+ ", inFieldValue=" + inFieldValue
			+ ", tmpIsFlat=" + tmpIsFlat
			+ ", tmpIsFreeForm=" + tmpIsFreeForm
			+ ", tmpIsEmpty=" + tmpIsEmpty
			+ ", tmpIsFlatName=" + tmpIsFlatName
			);

		// We can only do flat if we're already flat (or empty)
		// and the new name is also flat.
		boolean doAsFlat = ( tmpIsFlat || ( ! tmpIsFreeForm && tmpIsEmpty ) )
			&& tmpIsFlatName;
		// We need to upconvert if we've decided to go free form
		// but we have existing flat data
		boolean needToConvert = ! doAsFlat && (tmpIsFlat && ! tmpIsEmpty);

		if(debug) System.out.println(
			"Debug: WorkUnit:addNamedField2:"
			+ " doAsFlat=" + doAsFlat
			+ ", needToConvert=" + needToConvert
			);

		if( needToConvert )
		{
			autoConvertFlatRecordToTree();
		}

		int valLen = inFieldValue.length();

		// Should we add normal fields?
		if( doAsFlat )
		{

			// Normalize the name
			inFieldName = inFieldName.trim().toLowerCase();

			// Sanity check
			if( inFieldName.equals("") )
			{
				System.err.println( "Error: WorkUnit:addNamedField2:"
					+ " empty flat field name passed in"
					);
				return null;
			}

			// Does the field already exist?
			Element oldField = getUserField( inFieldName );
			if( oldField != null )
			{
				System.err.println( "Error: WorkUnit:addNamedField2:"
					+ " Flat field with same name already esists."
					+ " Ignoring new value."
					+ " (You may want to consider using free-form style data.)"
					+ ", new fieldname=" + inFieldName
					+ ", new value=" + inFieldValue
					+ ", previously existing field tag="
					+ JDOMHelper.JDOMToString( oldField )
					);
				return null;
			}

			// Actually add the field

			// First we create an element <field />
			Element newFieldElem = new Element(
				DEFAULT_USER_DATA_TREE_ELEMENT_NAME
				);
			newFieldElem.detach();

			// Set the name
			newFieldElem.setAttribute( FIELD_NAME_ATTR, inFieldName );

			// Add the content
			// If no cdata limit was set, or it's set and we're below
			// it, go ahead and add as regular content
			if( inAutoCDataThreshold < 0 ||
				(valLen < inAutoCDataThreshold )
				)
			{
				newFieldElem.addContent( inFieldValue );
			}
			// Otherwise we do need to use CDATA
			else
			{
				newFieldElem.addContent( new CDATA(inFieldValue) );
			}

			// Add it to the record
			// return jh.addElementToPath( USER_DATA_PATH, newFieldElem );
			return addUserDataElement( newFieldElem );

		}
		// Else add it as a free form item
		else if( getIsUserDataFreeForm() )
		{

			// Normalize the name
			inFieldName = inFieldName.trim();

			// We need to split the name up

			// Chop up the string into the lead and last part
			int lastSlashAt = inFieldName.lastIndexOf( '/' );

			// Sanity check, and we do NOT allow "/foo" in this method
			if( lastSlashAt == 0 || lastSlashAt == inFieldName.length()-1 )
			{
				System.err.println( "Error: WorkUnit:addNamedField2:"
					+ " Invalid slash placement."
					+ " Can't be at start or end."
					+ " Ignoring new value."
					+ ", fieldname=" + inFieldName
					+ ", value=" + inFieldValue
					);
				return null;
			}

			// The two parts of the path
			String partA = null;
			String partB = null;

			// Do we have a leading part?
			if( lastSlashAt > 0 )
			{
				partA = inFieldName.substring( 0, lastSlashAt );
				partB = inFieldName.substring( lastSlashAt+1 );
			}
			else		// Else just the second half of the path
			{
				partA = "";
				partB = inFieldName;
			}

			// Sanity check
			if( partB.indexOf('[') >= 0 || partB.indexOf(']') >= 0 )
			{
				System.err.println( "Error: WorkUnit:addNamedField2:"
					+ " Invalid bracket characters."
					+ " Can't use brackets in last section of path."
					+ " Ignoring new value."
					+ ", fieldname=" + inFieldName
					+ ", field value=" + inFieldValue
					);
				return null;
			}
			partB = partB.trim();
			// We really need part B, if there's none, we're hosed
			if( partB.equals("") )
			{
				System.err.println( "Error: WorkUnit:addNamedField2:"
					+ " Invalid ending path?"
					+ " Can't use brackets in last section of path."
					+ " Ignoring new value."
					+ ", fieldname=" + inFieldName
					+ ", trimmed endPath=\"" + partB + "\""
					+ ", field value=" + inFieldValue
					);
				return null;
			}

			// Figure out what the parent is
			Element parent = null;

			// If there's any partA path, look it up
			if( ! partA.trim().equals("") )
			{
				parent = JDOMHelper.findElementByPath( getUserData(), partA );
				if( parent == null )
					System.err.println( "Warning: WorkUnit:addNamedField2:"
						+ " Didn't find parent path '" + partA + "'"
						+ " so this node will be added at the root"
						);
			}
			// Else it's off the "root" of the user data
			else
				parent = getUserData();

			// Sanity check
			if( parent == null )
			{
				System.err.println( "Error: WorkUnit:addNamedField2:"
					+ " Wound up with null parent?"
					+ " Ignoring new value."
					+ ", fieldname=" + inFieldName
					+ ", start Path=\"" + partA + "\""
					+ ", field value=" + inFieldValue
					);
				return null;
			}

			// Now create the new element
			Element newChild = new Element( partB );
			newChild.detach();

			// add the content

			// If no cdata limit was set, or it's set and we're below
			// it, go ahead and add as regular content
			if( inAutoCDataThreshold < 0 ||
				(valLen < inAutoCDataThreshold )
				)
			{
				newChild.addContent( inFieldValue );
			}
			// Otherwise we do need to use CDATA
			else
			{
				newChild.addContent( new CDATA(inFieldValue) );
			}

			// Now attach it to it's parent
			parent.addContent( newChild );

			// Return what we created
			return newChild;

		}
		// Else we're confused, this should never happen
		else
		{
			System.err.println( "Error: WorkUnit:addNamedField2:"
				+ " Unknown fall through condition reached?"
				);
			return null;
		}
	}


	// Given a field name and value, add the field
	// Todo: Support full path
	// By default, the destination work unit is the same
	// as the source, but you can send fields to another
	// work unit if you specify a desitnation in the overloaded
	// version
	public void copyNamedFields( String inSrcFieldName,
		String inDstFieldName )
	{
		copyNamedFields( inSrcFieldName,inDstFieldName, null );
	}

	public void copyNamedFields( String inSrcFieldName,
		String inDstFieldName, WorkUnit optDestWorkUnit )
	{
	    final String kFName = "copyNamedFields";
		if( null==optDestWorkUnit)
		    optDestWorkUnit = this;

		if( inSrcFieldName==null || inSrcFieldName.trim().equals("") ||
			inDstFieldName==null || inDstFieldName.trim().equals("")
			)
		{
		    optDestWorkUnit.errorMsg( kStaticClassName, kFName,
				"null field(s) names passed in" +
				" source='" + inSrcFieldName + "'" +
				" destination='" + inDstFieldName + "'" +
				", cacelling operation."
				);
			return;
		}

		if( inSrcFieldName.indexOf('/')>=0 || inSrcFieldName.indexOf('[')>=0 ||
			inDstFieldName.indexOf('/')>=0 || inDstFieldName.indexOf('[')>=0
			)
		{
			System.err.println( "Warning: WorkUnit:copyNamedField:" +
				" paths not yet supported, found '/' or '[' in field name(s)," +
				" source='" + inSrcFieldName + "'" +
				" destination='" + inDstFieldName + "'" +
				", cacelling operation."
				);
			return;
		}

		String lDstFieldName = inDstFieldName.trim();

		// Look up the source field(s)
		List fields = getUserFields( inSrcFieldName );

		// For each source field
		List tmpChildren = new Vector();
		tmpChildren.addAll( fields );
		// for( Iterator it=fields.iterator() ; it.hasNext() ; )
		// ^^^ throwing java.util.ConcurrentModificationException
		for( Iterator it=tmpChildren.iterator() ; it.hasNext() ; )
		{
			Element srcField=(Element)it.next();
			// Clone and detach it
			Element destField = (Element)srcField.clone();
			destField.detach();
			// Then rename it
			// Is it a "flat" field?
			if( getIsAFlatElement(destField) )
			{
				destField.setAttribute( FIELD_NAME_ATTR, lDstFieldName );
			}
			// Else it's free form
			else
			{
				destField.setName( lDstFieldName );
			}

			// Add it to the root
			// Todo: someday, when we support paths, add it there
			optDestWorkUnit.addUserDataElement( destField );
		}   // End for each source field
	}

	public boolean renameField( String inSrcFieldName, String inDstFieldName ) {
		// return renameField( inSrcFieldName, inDstFieldName, 1 );
		return renameField( inSrcFieldName, inDstFieldName, DEFAULT_HOW_MANY );
	}
	// TODO: allow for offsets, negative offsets, etc
	// TODO: REDO ERRORs and Warnings here...
	public boolean renameField( String inSrcFieldName, String inDstFieldName, int inHowMany )
	{
		final String kFName = "renameField";
		
		inSrcFieldName = NIEUtil.trimmedStringOrNull( inSrcFieldName );
		inDstFieldName = NIEUtil.trimmedStringOrNull( inDstFieldName );

		// staticStatusMsg( kFName, "inHowMany=" + inHowMany );

		if( null==inSrcFieldName || null==inDstFieldName ) {
			System.err.println( "Warning: WorkUnit:renameField:" +
				" null field(s) names passed in" +
				" source='" + inSrcFieldName + "'" +
				" destination='" + inDstFieldName + "'" +
				", cacelling operation."
				);
			return false;
		}

		if( inSrcFieldName.indexOf('/')>=0 || inSrcFieldName.indexOf('[')>=0 ||
			inDstFieldName.indexOf('/')>=0 || inDstFieldName.indexOf('[')>=0
			)
		{
			System.err.println( "Warning: WorkUnit:renameField:" +
				" paths not yet supported, found '/' or '[' in field name(s)," +
				" source='" + inSrcFieldName + "'" +
				" destination='" + inDstFieldName + "'" +
				", cacelling operation."
				);
			return false;
		}

		if( inSrcFieldName.equals( inDstFieldName ) ) {
			System.err.println( "Warning: WorkUnit: renameField: can't rename field to itself:" +
				" null field(s) names passed in" +
				" source='" + inSrcFieldName + "'" +
				" destination='" + inDstFieldName + "'" +
				", cacelling operation."
				);
			return false;
		}

		List matches = getUserData().getChildren( inSrcFieldName );
		if( null==matches || matches.isEmpty() ) {
			System.err.println( "Warning: WorkUnit:renameField: did not find field named '"
				+ inSrcFieldName + "'"
				+ ", cacelling operation."
				);
			return false;
		}
		int counter = 0;
		for( Iterator it = matches.iterator(); it.hasNext() ; ) {
			Element targetElem = (Element) it.next();
			targetElem.setName( inDstFieldName );
			counter++;
			if( inHowMany>0 && counter>=inHowMany )
				break;
		}
		return true;
	}

	public Element addUserDataElement( JDOMHelper inElem )
	{
		if( inElem == null )
		{
			System.err.println( "Warning: WorkUnit:addUserDataElement1:" +
				" was passed a null in Element"
				);
			return null;
			// Todo: complain
		}
		return addUserDataElement( inElem.getJdomElement() );
	}

	public Element addUserDataElement( String inXMLText )
	{
		if( inXMLText == null )
		{
			System.err.println( "Warning: WorkUnit:addUserDataElement2:" +
				" was passed a null in Element"
				);
			return null;
			// Todo: complain
		}

		JDOMHelper lJdh = null;
		try
		{
			lJdh = new JDOMHelper( inXMLText, null );
		}
		catch( Exception e )
		{
			System.err.println( "Warning: WorkUnit:addUserDataElement3:"
				+ " got exception from JDOMHelper constructor."
				+ " XML text was: " + inXMLText
				+ " Exception: " + e
				);
			return null;
		}
		return addUserDataElement( lJdh.getJdomElement() );
	}

	// Given a new element, add it to the user data section
	// It so happens that if we're using flat fields and this came
	// from a flat field then it'll stay flat, so no special
	// checking or coersion is necessary.
	public Element addUserDataElement( Element inElem )
	{
		if( inElem == null )
		{
			System.err.println( "Warning: WorkUnit:addUserDataElement3:" +
				" was passed a null in Element"
				);
			return null;
			// Todo: complain
		}

		// Figure out if we need to autoconvert
		// We're conservative about applying this rule
		// - The node must be specifically non-flat
		// - There is already existing user data
		// - The existing user data is NOT freeform
		if( ! getIsAFlatElement(inElem) )
		{
			if( ! getIsUserDataEmpty() && ! getIsUserDataFreeForm() )
				forceUserDataToFreeForm();
		}

		inElem.detach();
		return getUserData().addContent( inElem );
	}



	/***
	// If the user record contains a full XML tree, get it
	public Element getUserDataTree()
	{
		// We look for the first user data field of type tree
		Element tmpElem = jh.mixedQuery( USER_DATA_PATH,
			"field", "type", "tree", false
			);

		// If we found something, return the first child, that
		// will be their tree
		if( tmpElem != null )
		{
			// Get the list of children
			List tmpList = tmpElem.getChildren();
			// If there is at least one, return the first child
			if( tmpList.size() > 0 )
				return (Element)tmpList.get(0);
			// Else no children, return null
			else
				return null;
		}
		// Else we did not find any tree fields
		else
			return null;

	}
	***/


	// Return the child we are deleting, or null if it fails
	// This will only delete the "first" matching child
	// This method handles both the "flat" and "free form" trees
	// automatically by using the getUserField function vs
	// just mucking with the main user data element.
	public Element deleteUserDataField( String inPath )
	{
		if( inPath == null )
			return null;

		// Find the child
		Element theField = getUserField( inPath );

		if( theField == null )
			return null;

		// Find it's parent
		Element theParent = theField.getParent();
		if( theParent == null )
			return null;

		// Now remove the child from the parent
		if( theParent.removeContent( theField ) )
			// success, return the field we found and deleted
			return theField;
		else
			// return failure
			return null;

	}

	// Sometimes there is more then one matching field in a user
	// data section, especially with free form trees
	// Returns how many children it deleted
	public int deleteUserDataFieldMulti( String inPath )
	{
		int counter = 0;
		while( deleteUserDataField( inPath ) != null )
			counter++;
		return counter;
	}

	// Sometimes there is more then one matching field in a user
	// data section, especially with free form trees
	// Returns how many children it deleted
	// TODO: Revisit the calling order, seems backwards
	public int deleteUserDataFieldMulti( String inPath, int inKeepWhich )
	{
		// Passthrough on default
		if( inKeepWhich == 0 )
			return deleteUserDataFieldMulti( inPath );
		// Sanity checks
		if( inPath == null || inPath.trim().equals("") )
			return 0;
		List children = getUserFields( inPath );
		if( children == null || children.size() < 1 )
			return 0;

		// We need to convert the input offset to an array-style offset
		// Input is +/- and one-based, Output is >= 0 and zero-based
		int numChil = children.size();
		int lKeepOffset = inKeepWhich > 0 ? inKeepWhich - 1 :
			numChil + inKeepWhich;

		// check bounds
		if( lKeepOffset < 0 || lKeepOffset > numChil-1 )
		{
			System.err.println( "Warning: WorkUnit: deleteUserDataFieldMulti:" +
				" Badd offset '" + inKeepWhich + "' with only '" +
				numChil + "' children."
				);
		}

		// An edge case
		// If they've asked us to "keep" a child but there's only
		// one, then just retain it
		if( children.size() == 1 )
			return 0;

		// A bit of a fancy backwards walk through the list
		// JDOM doc swears this is OK to do
		int deleteCounter = 0;
		for( int i=numChil-1; i>=0; i-- )
		{
			// Don't remove the one we want to keep
			if( i==lKeepOffset )
				continue;
			// If we're "above" the one we will keep, then
			// just use the offset
			if( i > lKeepOffset )
				children.remove( i );
			// If we're "below" / past the one we deleted, the
			// index i is invalid, but we just need to do it N
			// times in no particular order, so just keep
			// toasting the first one on the list
			else
				children.remove( 0 );

			deleteCounter++;
		}

		return deleteCounter;

	}



	// Deletes ALL of the user data section
	// public boolean deleteAllUserData()
	public void deleteAllUserData()
	{
		Element mainUserData = jh.findElementByPath(
			USER_DATA_PATH
			);
		if( mainUserData == null )
			// return false;
			return;

		// return mainUserData.removeChildren();
		mainUserData.getChildren().clear();
	}

	Element autoConvertFlatRecordToTree()
	{
		// Don't bother to convert if there's nothing there
		if( getIsUserDataEmpty() )
			return getUserData();

		// Don't bother to convert if we've already done it
		if( ! getIsUserDataFlat() )
			return getUserData();

		// Get the list of existing fields, and delete them
		// from their parent
		List oldFields = getAllFlatFields();

		// Now add the main new field
		// if( ! deleteAllUserData() )
		//	return null;
		deleteAllUserData();

//		// Create the default top level element
//		// First create the actual new element
//		Element tree = new Element( DEFAULT_USER_DATA_TREE_ROOT_NAME );
//		// Now create the one user field
//		Element newField = new Element( "field" );
//		// Note that it's a tree field
//		newField.setAttribute( "type", "tree" );
//		// Now add the user data field
//		newField.addContent( tree );
//		// Finally, connect the new tree field to the user data branch
//		jh.addElementToPath( USER_DATA_PATH,
//			newField
//			);

		// Loop through the fields and add them
		for( int i=0; i<oldFields.size(); i++ )
		{
			// get the old field element
			Element oldField = (Element)oldFields.get(i);
			if( oldField == null )
				continue;
				// Todo: yell loudly

			// Get the old name
			String oldName = oldField.getAttributeValue( FIELD_NAME_ATTR );
			if( oldName == null )
				continue;
				// Todo: complain

			oldName = oldName.trim();
			if( oldName.equals("") )
				continue;
				// Todo: complain!

			// If we autonamed it, don't use the _n suffix version,
			// trees can have duplicate names
			//if( oldName.startsWith( AUTO_FIELDNAME_PREFIX ) )
			//{
			//	// Use our default name for field elements
			//	oldName =  DEFAULT_USER_DATA_TREE_ELEMENT_NAME;
			//}
			// Disabled for now, leave it field_n
			// Todo: revsit when all would support field[n] syntax

			// Get the old content
			String oldContent = oldField.getText();

			// Create an element
			Element newElem = new Element( oldName );
			// Add the content
			newElem.addContent( oldContent );

			// now add it
			//tree.addContent( newElem );
			getUserData().addContent( newElem );
		}

		//return tree;
		return getUserData();

	}

	/***************************************************
	 *
	 *		Print / Debug
	 *
	 ***************************************************/
	private void __sep__Print_Debug__() {}

	public void print()
	{
		jh.print();
	}

	public static void printElementList( List elements )
	{
		if( elements==null )
		{
			System.out.println( "printElementList: nothing to print." );
			return;
		}
		System.out.println( "Printing " + elements.size() + " elements." );
		for( int i=0; i<elements.size(); i++ )
		{
			System.out.println( "\t" +
				JDOMHelper.JDOMToString( (Element)elements.get(i) )
				);
		}
	}

	public String toString()
	{
		return jh.JDOMToString();
	}

	public void writeToFile( String fileName )
	{
		jh.writeToFile( fileName );
	}

	private static void ___Sep__Run_Logging__(){}





	//////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////////////
	//
	//  Logging
	//
	////////////////////////////////////////////////////////////

	// Returns RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED if not happy
	public static int decodeStringToLevelInt( String inLevel,
		boolean inOutputErrorMessages )
	{
		final String kFName = "decodeStringToLevelInt";

		// Normalize and check the string they gave us
		String newLevelStr = NIEUtil.trimmedLowerStringOrNull( inLevel );

		if( newLevelStr == null || newLevelStr.equals("-") )
		{
			if( inOutputErrorMessages )
			{
				staticErrorMsg( kFName,
					"The new level string that was passed in was null or empty."
					+ " Can't set a level if we're not told what it is."
					+ " Returning special code for unrecognized verbosity."
					);
			}
			return RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED;
		}

		// Remove leading slash, if any
		if( newLevelStr.startsWith("-") )
		{
			newLevelStr = newLevelStr.substring( 1 );
		}

		// REMINDER:
		// Coordinate changes here with
		// changes to getVerbosityLevelDescriptions()


		if( newLevelStr.equals( RunLogBasicImpl.USE_DEFAULT_INDICATOR ) ||	// "default"
			newLevelStr.equals( RunLogBasicImpl.USE_DEFAULT_INDICATOR2 )		// "default_verbosity"
			)
			// return RunLogInterface.DEFAULT_VERBOSITY;
			return DEFAULT_VERBOSITY;

		if( newLevelStr.equals( RunLogBasicImpl.SUPER_QUIET_INDICATOR ) ||	// "super_quiet"
			newLevelStr.equals( RunLogBasicImpl.SUPER_QUIET_INDICATOR2 )		// "fatal_only"
			)
			return RunLogInterface.VERBOSITY_FATAL_ONLY;

		if( newLevelStr.equals( "q" )
			|| newLevelStr.equals( RunLogBasicImpl.QUIET_INDICATOR )			// "quiet"
			)
			return RunLogInterface.VERBOSITY_QUIET;

		if( newLevelStr.equals( "s" )
			|| newLevelStr.equalsIgnoreCase( RunLogBasicImpl.PREFIX_STATUS )
			)
			return RunLogInterface.VERBOSITY_STATUS_PROCESS;

		if( newLevelStr.equals( "t" )
			|| newLevelStr.equalsIgnoreCase( RunLogBasicImpl.PREFIX_TRANSACTION )
			|| newLevelStr.equals( RunLogBasicImpl.TRANSACTION_PLURAL_INDICATOR )	// "transactions"
			)
			return RunLogInterface.VERBOSITY_STATUS_TRANSACTIONS;

		if( newLevelStr.equals( "i" )
			|| newLevelStr.equalsIgnoreCase( RunLogBasicImpl.PREFIX_INFO )
			)
			return RunLogInterface.VERBOSITY_DETAILED_INFO;

		if( newLevelStr.equals( "d" )
			|| newLevelStr.equalsIgnoreCase( RunLogBasicImpl.PREFIX_DEBUG )
			)
			return RunLogInterface.VERBOSITY_DEBUG;

		if( newLevelStr.equals( "t" )
			|| newLevelStr.equalsIgnoreCase( RunLogBasicImpl.PREFIX_TRACE )
			)
			return RunLogInterface.VERBOSITY_TRACE;

		// So by now we haven't recognized what they gave us

		// Should we complain?
		// In some cases the parent program will want to handle that

		if( inOutputErrorMessages )
		{
			staticErrorMsg( kFName,
				"The level string that was passed in was not recognized."
				+ " Attempted new unrecognized verbosity level was \"" + inLevel + "\"."
				// + " The method .getVerbosityLevelDescriptions() may be helpful."
				// + " Or try the same command with info (or -info) for a more detailed message."
				+ " Returning special code for unrecognized verbosity."
				);
			// Get a message and use the defaults
			// String tmpMsg = getVerbosityLevelDescriptions();
			// warningMsg( kFName, tmpMsg );
		}

		// return the special unknown code
		return RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED;

	}
















	int getVerbosity()
	{
		if( cOverallVerbosity != 0 )
			return cOverallVerbosity;

		// Get processor status info
		Element info = getProcessInfoElem();
		if( null==info ) {
			cOverallVerbosity = DEFAULT_VERBOSITY;
		}
		// Get the verobosity sub element
		else {
			Element verb = info.getChild( RunLogBasicImpl.VERBOSITY_ELEMENT );
			if( null==verb ) {
				cOverallVerbosity = DEFAULT_VERBOSITY;
			}
			// Get the level attribute
			else {
				String levelStr = JDOMHelper.getStringFromAttributeTrimOrNull(
					verb, RunLogBasicImpl.VERBOSITY_LEVEL_ATTR, false
					);
				if( null==levelStr ) {
					cOverallVerbosity = DEFAULT_VERBOSITY;
				}
				// Decode it
				else {
					cOverallVerbosity = decodeStringToLevelInt( levelStr, true );
					// and double check
					if( RunLogInterface.VERBOSITY_VALUE_NOT_RECOGNIZED == cOverallVerbosity )
						cOverallVerbosity = DEFAULT_VERBOSITY;
				}
			}
		}

		return cOverallVerbosity;
	}

	public boolean shouldLog(
			int inMessageLevel, Processor inFromProcess, String inFromMethod
			)
	{
	    return shouldLog( inMessageLevel, inFromProcess.kClassName(), inFromMethod );
	}

	public boolean shouldLog(
		// int inMessageLevel, Processor inFromProcess, String inFromMethod
		int inMessageLevel, String inFromProcess, String inFromMethod
		)
	{

		// Wrok on figuring out the current set point
		int currentSetpoint = getVerbosity();

		// TODO: See if the processor has overridden that
		// by looking at inFromProcess

		// Normalize the setpoint to account for the default
		if( currentSetpoint == RunLogInterface.VERBOSITY_USE_DEFAULT )
			// currentSetpoint = RunLogInterface.DEFAULT_VERBOSITY;
			currentSetpoint = DEFAULT_VERBOSITY;

		// Todo: is this convoluted logic really necessary?
		// Can't every just use the DEFAULT_VERBOSITY constant?

		// Now do the actual checking
		if( inMessageLevel <= currentSetpoint )
			return true;
		else
			return false;

	}

	public boolean message( int inMsgLevel, Processor inFromProcessor, String inFromRoutine, String inMessage )
	{
	    return message( inMsgLevel, inFromProcessor.kClassName(), inFromRoutine, inMessage );
	}
	public boolean message( int inMsgLevel, String inFromProcessor, String inFromRoutine, String inMessage )
	{
		final String kFName = "message";

		if( ! shouldLog( inMsgLevel, inFromProcessor, inFromRoutine ) )
			return false;


		Element procElem = findRunpathElement( inFromProcessor );
		Element messages = JDOMHelper.findOrCreateElementByPath(
			procElem, PROCESSOR_MESSAGES_SUBTAG, true
			);	

		Element msgElem = new Element( INDIVIDUAL_MESSAGE_TAG );

		// Severity Info
		Integer key = new Integer( inMsgLevel );
		String levelStr = RunLogBasicImpl.PREFIX_GENERIC;
		if( RunLogBasicImpl.INT_TO_LEVEL_PREFIX.containsKey(key) )
			levelStr = (String) RunLogBasicImpl.INT_TO_LEVEL_PREFIX.get(key);
		levelStr = levelStr.toLowerCase();
		msgElem.setAttribute( RunLogBasicImpl.VERBOSITY_LEVEL_ATTR, levelStr );
		msgElem.setAttribute( RunLogBasicImpl.VERBOSITY_LEVEL_ATTR + "_int", ""+inMsgLevel );

		// msgElem.setAttribute( RunLogBasicImpl.VERBOSITY_CLASS_ATTR, inFromProcessor.kClassName() );
		msgElem.setAttribute( RunLogBasicImpl.VERBOSITY_CLASS_ATTR, inFromProcessor );

		if( null!=inFromRoutine )
			msgElem.setAttribute( RunLogBasicImpl.VERBOSITY_METHOD_ATTR, inFromRoutine );

		if( null == msgElem.addContent( inMessage ) )
			staticErrorMsg( kFName, "Not able to add message to map element: " + inMessage );

		if( null == procElem.addContent( msgElem ) )
			staticErrorMsg( kFName, "Not able to add map element to proc element for error: " + inMessage );

		return true;
	}


	public boolean statusMsg( Processor inFromProcessor, String inFromRoutine, String inMessage )
	{
		return message( RunLogInterface.VERBOSITY_STATUS_PROCESS,
			inFromProcessor, inFromRoutine,
			inMessage
			);
	}

	public boolean shouldDoStatusMsg( Processor inFromProcessor, String inFromRoutine )
	{
		return shouldLog( RunLogInterface.VERBOSITY_STATUS_PROCESS,
				inFromProcessor, inFromRoutine
				);
	}

	public boolean transactionStatusMsg( Processor inFromProcessor, String inFromRoutine, String inMessage )
	{
		return message( RunLogInterface.VERBOSITY_STATUS_TRANSACTIONS,
			inFromProcessor, inFromRoutine,
			inMessage
			);
	}

	public boolean shouldDoTransactionStatusMsg( Processor inFromProcessor, String inFromRoutine )
	{
		return shouldLog( RunLogInterface.VERBOSITY_STATUS_TRANSACTIONS,
				inFromProcessor, inFromRoutine
				);
	}

	public boolean infoMsg( Processor inFromProcessor, String inFromRoutine, String inMessage )
	{
		return message( RunLogInterface.VERBOSITY_DETAILED_INFO,
			inFromProcessor, inFromRoutine,
			inMessage
			);
	}

	public boolean shouldDoInfoMsg( Processor inFromProcessor, String inFromRoutine )
	{
		return shouldLog( RunLogInterface.VERBOSITY_DETAILED_INFO,
				inFromProcessor, inFromRoutine
				);
	}

	public boolean debugMsg( Processor inFromProcessor, String inFromRoutine, String inMessage )
	{
		return message( RunLogInterface.VERBOSITY_DEBUG,
			inFromProcessor, inFromRoutine,
			inMessage
			);
	}

	public boolean shouldDoDebugMsg( Processor inFromProcessor, String inFromRoutine )
	{
		return shouldLog( RunLogInterface.VERBOSITY_DEBUG,
				inFromProcessor, inFromRoutine
				);
	}

	public boolean traceMsg( Processor inFromProcessor, String inFromRoutine, String inMessage )
	{
		return message( RunLogInterface.VERBOSITY_TRACE,
			inFromProcessor, inFromRoutine,
			inMessage
			);
	}

	public boolean shouldDoTraceMsg( Processor inFromProcessor, String inFromRoutine )
	{
		return shouldLog( RunLogInterface.VERBOSITY_TRACE, inFromProcessor, inFromRoutine );
	}

	public boolean warningMsg( Processor inFromProcessor, String inFromRoutine, String inMessage )
	{
		return message( RunLogInterface.LEVEL_WARNING,
			inFromProcessor, inFromRoutine,
			inMessage
			);
	}

	public boolean shouldDoWarningMsg( Processor inFromProcessor, String inFromRoutine )
	{
		return shouldLog( RunLogInterface.LEVEL_WARNING, inFromProcessor, inFromRoutine );
	}

	public boolean errorMsg( Processor inFromProcessor, String inFromRoutine, String inMessage )
	{
		return message( RunLogInterface.LEVEL_ERROR,
			inFromProcessor, inFromRoutine,
			inMessage
			);
	}
	public boolean errorMsg( String inFromProcessor, String inFromRoutine, String inMessage )
	{
		return message( RunLogInterface.LEVEL_ERROR,
			inFromProcessor, inFromRoutine,
			inMessage
			);
	}

	public boolean shouldDoErrorMsg( Processor inFromProcessor, String inFromRoutine )
	{
		return shouldLog( RunLogInterface.LEVEL_ERROR, inFromProcessor, inFromRoutine );
	}

	// 	public boolean message( int inMsgLevel, Processor inFromProcessor, String inFromRoutine, String inMessage )
	//	if( ! shouldLog( inMsgLevel, inFromProcessor, inFromRoutine ) )
	//		return false;

	public boolean stackTrace(
        Processor inFromProcessor, String inFromMethod,
		Exception e, String optMessage
		)
	{
		if( shouldDoErrorMsg( inFromProcessor, inFromMethod ) )
		{

			String myTrace = null;
			if( null!=e ) {
				// Get a stream to string buffer
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter( sw );
				e.printStackTrace( pw );
	
				// Run the output
				String e2msg = null;
				try
				{
					pw.close();
					sw.close();
				}
				catch (Exception e2)
				{
					e2msg = "Error doing strack trace: " + e2;
				}
				// Return the string
				myTrace = sw.toString();
				if( null!=e2msg )
					myTrace += " " + e2msg;
			}
			else
				myTrace = "(NULL Strack Trace passed in)";

			if( null==optMessage )
				optMessage = "Generic:";

			return message( RunLogInterface.LEVEL_ERROR,
					inFromProcessor, inFromMethod,
					"STACK_TRACE: " + optMessage + NIEUtil.NL + myTrace
					);

		}
		else
		{
			return false;
		}
	}


	// This is only for errors that can't be stuffed into a specific
	// work unit
	private static void ___Sep__STATIC_Run_Logging__(){}

	// This gets us to the logging object
	public static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	private static boolean staticStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean staticShouldDoStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoStatusMsg( kStaticClassName, inFromRoutine );
	}


	private static boolean staticTransactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean staticShouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kStaticClassName, inFromRoutine );
	}


	private static boolean staticInfoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean staticShouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( kStaticClassName, inFromRoutine );
	}


	private static boolean staticDebugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean staticShouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kStaticClassName, inFromRoutine );
	}


	private static boolean staticTraceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	private static boolean staticShouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kStaticClassName, inFromRoutine );
	}


	private static boolean staticWarningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}

	public static boolean staticWarningMsg( String inFromClass, String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( inFromClass, inFromRoutine,
			inMessage
			);
	}


	private static boolean staticShouldDoWarningMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoWarningMsg( kStaticClassName, inFromRoutine );
	}

	// Processors should use their own, not this
	/*private*/ static boolean staticErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}

	public static boolean staticErrorMsg( String inFromClass, String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( inFromClass, inFromRoutine,
			inMessage
			);
	}

	private static boolean staticFatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}

	public static boolean staticFatalErrorMsg( String inFromClass, String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( inFromClass, inFromRoutine,
			inMessage
			);
	}

	// Member Variables
	// ================================================
	private void __separator__Member_Variables__() {}

	// WARNING!!!!!!
	// We implement cloneable, so remember to update the clone
	// method if you add non-static, mutable member variables

	// This is the main jdom tree, wrapped in a jdom helper instance
	JDOMHelper jh;


	private void __sep__Cached_Fields__() {}
	///////////////////////////////////////////////
	Element cProcInfoElem;
	int cOverallVerbosity;



	// Constants
	// =========
	private void __sep__Constants__() {}

	// UNLIKE normal processes, we don't want our node cluttered up with
	// lots of status messages, etc.  If they want it, they can turn it on.
	public static final int DEFAULT_VERBOSITY = RunLogInterface.VERBOSITY_QUIET;

	// At what point should we automatically add user fields as CDATA
	static final int DEFAULT_AUTO_CDATA_THRESHOLD = 1024;

	// XML related stuff

	public static final String MAIN_TAG = "work_unit";
	public static final String PROCESS_INFO_TAG = "processing_information";
	// Where the user data is stored
	public static final String USER_DATA_TAG = "user_data";
	public static final String USER_DATA_PATH = USER_DATA_TAG;
	public static final String RUN_PATH_TAG = "runpath";
	public static final String SYSTEM_DATA_TAG = "system_data";
	public static final String EXTENDED_DATA_TAG = "extended_data";


	public static final String FIELD_TAG = "field";
	public static final String FIELD_NAME_ATTR = "name";
	// The default prefix for auto-named fields
	// for field_1, field_2, etc.
	static final String AUTO_FIELDNAME_PREFIX = FIELD_TAG + '_'; // "field_";

	// The attribute name we use to force freeform
	static final String FREE_FORM_ATTR_NAME = "free_form";
	// The default name we give when creating a user data tree
	// (non-flat record), typically from an auto conversion
	// of flat to tree
	static final String DEFAULT_USER_DATA_TREE_ROOT_NAME = "record";
	// The name we give to elements that we add in those circumstances
	static final String DEFAULT_USER_DATA_TREE_ELEMENT_NAME = FIELD_TAG;


	public static final String INVALID_MARKER_TAG = "invalid_record";

	public static final String INVALID_MARKER_PATH =
		// "processing_information/invalid_record"
		PROCESS_INFO_TAG + '/' + INVALID_MARKER_TAG;

	public static final String INDIVIDUAL_RUN_PATH_ENTRY_TAG = "runpath_entry";
	public static final String PROCESSOR_ID_ATTR = "procid";

	public static final String PROCESSOR_MESSAGES_SUBTAG = "messages";
	public static final String INDIVIDUAL_MESSAGE_TAG = "message";

	private static int DEFAULT_HOW_MANY = nie.pump.base.PumpConstants.DEFAULT_HOW_MANY;

	//	// template structure
	//	static final String XML_TEMPLATE =
	//		"<work_unit>" +
	//		"	<processing_information />" +
	//		"	<user_data />" +
	//		"	<runpath />" +
	//		"	<system_data />" +
	//		"	<extended_data />" +
	//		"</work_unit>" ;

	// template structure
	static final String XML_TEMPLATE =
		"<" + MAIN_TAG + ">" +
		"	<" + PROCESS_INFO_TAG + " />" +
		"	<" + USER_DATA_TAG + " />" +
		"	<" + RUN_PATH_TAG + " />" +
		"	<" + SYSTEM_DATA_TAG + " />" +
		"	<" + EXTENDED_DATA_TAG + " />" +
		"</" + MAIN_TAG + ">" ;


	/************************************************
	 *
	 *		Main
	 *
	 ************************************************/
	private void __sep__Main__() {}

	public static void main(String[] args)
	{

		if( args.length < 1 )
		{
			System.out.println( "Error: syntax:\nclass wu-test1.xml wu-test2.xml ..." );
			System.exit(1);
		}

		for( int i = 0; i<args.length; i++ )
		{
			String uri = args[i];

			WorkUnit wu = null;
			try
			{
				wu = new WorkUnit( uri );
			}
			catch (Exception e)
			{
				System.out.println( "Error creating work unit from uri '" +
									uri + "'" );
				System.out.println( e );
				continue;
			}

			WorkUnit wu2 = (WorkUnit)(wu.clone());
			System.out.println( "Cloned it too" );

			System.out.println( "===== Work unit created from '" +
				uri + "' ======="
				);
			//wu.print();

			// Tests
			System.out.println( "isValid=" + wu.getIsValidRecord() );

			System.out.println( "_ Showing messages:" );
			printElementList( wu._getAllMessages() );

		}
	}

}
