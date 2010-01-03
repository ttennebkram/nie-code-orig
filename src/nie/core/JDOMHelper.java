package nie.core;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.CDATA;
import org.jdom.Text;
import org.jdom.Attribute;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.JDOMException;
import org.jdom.xpath.*;

import org.jdom.transform.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import nie.filters.*;
import nie.filters.io.*;

/**
 * <p><code>JDomUtil</code></p> encapsulates some shortcuts
 * for creating objects around JDom.
 * </p>
 * <p>Originally classes were derived from this class, so the functions
 * were implemented that way.  Now you can also do a "has a" vs "is a"
 * mode, see Constructor # 4.
 * </p>
 * Copyright 2001 New Idea Engineering, Inc.
 * {@link www.ideaeng.com}
 * @author Mark Bennett (mbennett@ideaeng.com)
 * @version 0.1
 **/


public class JDOMHelper implements Cloneable
{

	private final static String kClassName = "JDOMHelper";

//  Moved to it's own file
//	public class JDOMHelperException extends Exception
//	{
//		public JDOMHelperException( String inMessage )
//		{
//			super( inMessage );
//		}
//	}

	// The name of the top level element
	// OVERRIDE THIS if you want to have us check.
	// Of course you can only override this if you are inheriting
	// from it.
	// Have it return null if you don't want this check performed.
	// return a white space separated list if you will accept multiple ones

	public String _getDesiredTopLevelElementName()
	{
		// Will normally be NULL, which will casue an error,
		// so you should really override.

		return null;
	}

	// private static boolean debug = true;
	// Don't specifically set it if you want FALSE, it'll default to that
//	private static boolean debug;
//	public static void setDebug( boolean flag )
//	{
//		debug = flag;
//	}


	private static void ___Sep__Construct_Clone_and_Factory__(){}
	///////////////////////////////////////////////////////////////////

	/**********************************************
	 *
	 *		Constructors
	 *
	 ***********************************************/

	/**
	 * Constructor # 1: Create from an existing element.
	 * <p>
	 * This will create an instance of a <code>JDOMHelper</code>
	 * </p>
	 *
	 * @param jdom Element <code>element</code>
	 * An element describing the Stream to create.
	 **/

	public JDOMHelper( Element inputElement )
		throws JDOMHelperException
	{
		_jdomInit( inputElement );
		// Can't use this() because other constructors
	}

	// This version includes recursion AND will resolve
	// system: paths relative to the class you supply
	// Put the String SEFCOND on purpose to avoid ambiguity
	// with the other constructor in the case that the second
	// optional argument is null
	public JDOMHelper( Class inClass, String inURI, boolean inRecordInternalIncludeAttrs )
		throws Exception
	{
		this( inURI, null, 0, new AuxIOInfo(inClass,inRecordInternalIncludeAttrs) );
	}

	/**
	 * Constructor # 2: Create from an XML file, given a filename or url
	 * <p>
	 * This will create an instance of a <code>JDOMHelper</code>
	 * It does NOT handle includes nor system: paths, so use
	 * one of the fanicer versions if you need that.
	 * </p>
	 *
	 * @param String <code>uri</code>
	 * Where to get the source document from
	 **/
	public JDOMHelper( String uri )
		throws JDOMHelperException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// Note:
		// Do NOT just use the (String, String, int) version
		// It doesn't handle 
		
		// Normalize and sanity check
		uri = NIEUtil.trimmedStringOrNull( uri );
		if( uri == null )
			throw new JDOMHelperException( kExTag
				+ "Null/empty URI passed into constructor."
				);

		// Save it for later
		fSourceURI = uri;

		// Lower case for sanity checking
		String checkName = uri.toLowerCase();

		// We have special handling for PDF and HTML files
		boolean isHTML = false;
		boolean isPDF = false;
		if( checkName.endsWith( ".pdf" ) )
			isPDF = true;
		else if( checkName.endsWith(".html")
			|| checkName.endsWith(".htm")
			|| checkName.endsWith(".shtml")
			)
			isHTML = true;

		//synchronized( jdomBuilder )
		//{
		//  if( jdomBuilder == null )
		//		jdomBuilder = new SAXBuilder();
		//	jdomDocument = jdomBuilder.build(uri);
		//}

		Exception lastException = null;
		try
		{
			if( isHTML )
				jdomDocument = readHTML(uri);
			else if( isPDF )
				jdomDocument = readPDF(uri);
			else
				jdomDocument = readXML(uri);
		}
		catch(Exception e)
		{
			lastException = e;
			jdomDocument = null;
			// We usually bail at this point
			// We'll give a second chance if the file has backslashes
			// which can indicate a Windows style path, which we may need
			// to tewak
			// If the URI is null or has NO backslashes, we're done
			if( uri == null || uri.indexOf('\\') < 0 )
				throw new JDOMHelperException( kExTag
					+ "Got exception for uri '" + uri + "'"
					+ " error:" + e
					);
			// Else we'll give it a second chance
		}

		// Retry if we had a problem on the first attempt
		// This means we got an exception AND we decided NOT to bail
		// and instead opted for a second chance
		if( lastException != null )
		{
			// Make an attempt to convert it to a file type
			String newURI = NIEUtil.convertWindoesPathToFileURI( uri );

			// If we got nothing back, or it was unchanged, just throw the last exception
			if( newURI == null || newURI.equals(uri) )
					throw new JDOMHelperException( kExTag
						+ "Got exception for uri '" + uri + "'"
						+ " error:" + lastException
						);

			statusMsg( kFName,
				"Will retry with file URL \"" + newURI + "\"."
				);

			// Try it again
			try
			{
				if( isHTML )
					jdomDocument = readHTML(uri);
				else if( isPDF )
					jdomDocument = readPDF(uri);
				else
					jdomDocument = readXML(uri);
			}
			catch(Exception e3)
			{
				jdomDocument = null;
				throw new JDOMHelperException( kExTag
					+ "Got exception for uri '" + uri + "'"
					+ " error:" + e3
					);
			}
		}   // End if there was a problem the first time

		_jdomInit( jdomDocument.getRootElement() );
		
		// return this;
	}

	public JDOMHelper JDOMHelperV1( String uri )
		throws JDOMHelperException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// Normalize and sanity check
		uri = NIEUtil.trimmedStringOrNull( uri );
		if( uri == null )
			throw new JDOMHelperException( kExTag
				+ "Null/empty URI passed into constructor."
				);

		//synchronized( jdomBuilder )
		//{
		//  if( jdomBuilder == null )
		//		jdomBuilder = new SAXBuilder();
		//	jdomDocument = jdomBuilder.build(uri);
		//}

		// JDOMException lastException = null;
		Exception lastException = null;
		try
		{
			jdomDocument = jdomBuilder.build(uri);
		}
		// catch(JDOMException e)
		catch(Exception e)
		{
			lastException = e;
			jdomDocument = null;
			// We usually bail at this point
			// We'll give a second chance if the file has backslashes
			// which can indicate a Windows style path, which we may need
			// to tewak
			// If the URI is null or has NO backslashes, we're done
			if( uri == null || uri.indexOf('\\') < 0 )
				throw new JDOMHelperException(
					"jdh(uri)-1: exception for uri '" + uri + "'"
					+ " error:" + e
					);
			// Else we'll give it a second chance
		}

		// Retry if we had a problem on the first attempt
		// This means we got an exception AND we decided NOT to bail
		// and instead opted for a second chance
		if( lastException != null )
		{
			// Make an attempt to convert it to a file type
			String newURI = NIEUtil.convertWindoesPathToFileURI( uri );

			// If we got nothing back, or it was unchanged, just throw the last exception
			if( newURI == null || newURI.equals(uri) )
					throw new JDOMHelperException(
						"jdh(uri)-2: exception for uri '" + uri + "'"
						+ " error:" + lastException
						);

			statusMsg( kFName,
				"Will retry with file URL \"" + newURI + "\"."
				);

			// Try it again
			try
			{
				jdomDocument = jdomBuilder.build(newURI);
			}
			// catch(JDOMException e3)
			catch( Exception e3 )
			{
				jdomDocument = null;
				throw new JDOMHelperException(
					"jdh(uri)-3: exception for uri '" + uri + "'"
					+ " error:" + e3
					);
			}
		}   // End if there was a problem the first time

		_jdomInit( jdomDocument.getRootElement() );

		return this;
	}

	private Document readXML( String uri )
		throws JDOMException, IOException
	{
		return jdomBuilder.build(uri);
	}

	// private Document readHTML( String uri )
	public static Document readHTML( String uri )
		throws java.io.IOException
		// Exception
	{

		/***
		TODO: Use v|v|v this v|v|v code to make this method act like
		the other constructors, in particluar, we'd like to know the
		final URL that was used
		// Open a stream
		InputStream fin = null;
		// We'd like the underlying routines to tell us a bit
		// about the path they eventually wound up opening
		AuxIOInfo auxPathInfo = inAuxInfo;
		if( null == auxPathInfo )
			auxPathInfo = new AuxIOInfo();
		try
		{
			// open it relative to whatever we might have opened before
			// we have no username, password nor AuxIORecord
			fin = NIEUtil.openURIReadBin(
				inURI, optBaseURI,
				null, null, auxPathInfo
				);
		}
		catch( Exception e )
		{
			throw new JDOMHelperException( kExTag
				+ "Unable to open input stream."
				+ " URI=\"" + inURI + "\""
				+ " Reason: " + e
				);
		}

		// Get the final URL
		String finalURI = auxPathInfo.getFinalURI();
		debugMsg( kFName, "Final URI was \"" + finalURI + "\"" );
		// Also save it as a field
		fFinalURI = finalURI;

		rd.wrapInputStream( InputStrem is, boolean canSeekBackwards );
		***/

		SeekableStream rd = null;

		// Open either a file or a URL
		// Is it a URL?
		if( NIEUtil.isStringAURL( uri ) )
		{
			rd = new URLSeekableStream(new URL(uri));
		}
		// Else assume it's a file
		else
		{
			rd = new FileSeekableStream(uri);
		}



		// The in-memory repository for the tree we will build
		Element root = new Element("document");
		Document outDoc = new Document(root);

		HTMLParser p = new HTMLParser(rd, root);

		// Set whatever options were passed on the command line
//		if (values.size() > 0)
//		{
//			p.setSettings(values);
//		}

		// Read the file
		p.getContents();

		// cleanup and return the answer
		rd.close();
		return outDoc;
	}


	// private Document readHTML( String uri )
	public static Document readHTML( byte [] inPageBuffer )
		throws java.io.IOException
	{
		final String kFName = "readHTML(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==inPageBuffer )
			throw new IOException( kExTag + "Null byte buffer passed in." );

		SeekableStream rd = new ByteArraySeekableStream( inPageBuffer );

		// The in-memory repository for the tree we will build
		Element root = new Element("document");
		Document outDoc = new Document(root);

		HTMLParser p = new HTMLParser(rd, root);

		// Read the buffer
		p.getContents();

		// cleanup and return the answer
		rd.close();
		return outDoc;
	}





	private Document readPDF( String uri )
		throws java.io.IOException, Exception
	{

		SeekableStream rd = null;

		// Open either a file or a URL
		// Is it a URL?
		if( NIEUtil.isStringAURL( uri ) )
		{
			rd = new URLSeekableStream(new URL(uri));
		}
		// Else assume it's a file
		else
		{
			rd = new FileSeekableStream(uri);
		}

		// The in-memory repository for the tree we will build
		Element root = new Element("document");
		Document outDoc = new Document(root);

		PDFParser p = new PDFParser(rd, root);

		// Set whatever options were passed on the command line
//		if (values.size() > 0)
//		{
//			p.setSettings(values);
//		}

		// Read the file
		p.getContents();

		// cleanup and return the answer
		rd.close();
		return outDoc;
	}


	/*
	 * Constructor # 3: Create from an XML blob of text
	 * <p>
	 * Create an object from a string representation of an element.
	 * </p>
	 *
	 * @param String <code>XML text</code>, Builder <code>builder</code>
	 * null is OK for builder
	 */
	public JDOMHelper( String sourceString, SAXBuilder builder )
		throws JDOMHelperException
	{

		/* A little convoluted logic, but it makes sense.
		   If I pass you a builder, use it.
		   If I don't pass you a builder, see if there's already
		   one assigned to this object.
		   If none passed and none in object, create one.
		   And whatever you do, if you find the object's was
		   null, set it with whatever you come up with.
		*/
		//synchronized( jdomBuilder )
		//{
			if( builder == null )
			{
				if( jdomBuilder == null )
				{
					builder = new SAXBuilder();
					jdomBuilder = builder;
				}
				else
					builder = jdomBuilder;
			}
			else
				if( jdomBuilder == null )
					jdomBuilder = builder;
		//}   // End Sync block


		/* By this point, builder is pointed to something reasonable */

		// Create a character reader from the string

		StringReader sr = new StringReader( sourceString );
		try
		{
			jdomDocument = jdomBuilder.build(sr);
		}
		// catch(JDOMException e)
		catch( Exception e )
		{
			jdomDocument = null;
			try { sr.close(); } catch (Exception e2) { }
			throw new JDOMHelperException(
				"jdh(from String): exception"
				+ " error:" + e
				);

		}

		try { sr.close(); } catch (Exception e2) { }
		sr = null;
		_jdomInit( jdomDocument.getRootElement() );
	}

	/*
	 * Constructor # 4: Create from a Java File object
	 * <p>
	 * Create an object from a Java File representation of an element.
	 * </p>
	 *
	 * @param File <code>XML file</code>
	 */
	public JDOMHelper( File inFile )
		throws JDOMHelperException
	{
		final String kFName = "constructor4";

		// final boolean debug = false;

		debugMsg( kFName, "Start." );

//		synchronized( jdomBuilder )
//		{
//			//if( jdomBuilder == null )
//				jdomBuilder = new SAXBuilder();
//			if(debug)
//			{
//				System.err.p rintln( "JDH const(file) B" );
//				System.err.flush();
//			}
//			jdomDocument = jdomBuilder.build( inFile );
//		}

		try
		{
			jdomDocument = jdomBuilder.build(inFile);
			fSourceURI = inFile.getAbsolutePath();
		}
		// catch(JDOMException e)
		catch( Exception e )
		{
			jdomDocument = null;
			throw new JDOMHelperException(
				"jdh(File): exception"
				+ " error:" + e
				);
		}

//		//jdomDocument = jdomBuilder.build( inFile.getCanonicalPath() );
//		String tmpName = inFile.getCanonicalPath();
//		System.err.p rintln( "before: " + tmpName );
//		tmpName = tmpName.replace( '\\', '/' );
//		System.err.p rintln( "after: " + tmpName );
//		//tmpName = "/test/cisco_cache_small/wu_10007.xml";
//		tmpName = tmpName.substring( 2 );
//		System.err.p rintln( "and then: " + tmpName );
//		jdomDocument = jdomBuilder.build( tmpName );
		// traceMsg( kFName, "point C." );

		if( jdomDocument == null )
			throw new JDOMHelperException(
				"jdh(inFile): null document from File '" + inFile + "'"
				);
		traceMsg( kFName, "Point D." );
		_jdomInit( jdomDocument.getRootElement() );

		debugMsg( kFName, "Done." );
	}

	/**
	 * Constructor # 5: Create from an InputStream
	 * <p>
	 * This will create an instance of a <code>JDOMHelper</code>
	 * </p>
	 *
	 * @param InputStream <code>inStream</code>
	 * Where to get the source document from
	 * REMEMBER TO CLOSE YOUR STREAM!!!!!
	 **/
	public JDOMHelper( InputStream inStream )
		throws JDOMHelperException
	{
//		synchronized( jdomBuilder )
//		{
//			//if( jdomBuilder == null )
//				jdomBuilder = new SAXBuilder();
//			jdomDocument = jdomBuilder.build( inStream );
//		}

		try
		{
			jdomDocument = jdomBuilder.build(inStream);
		}
		// catch(JDOMException e)
		catch( Exception e )
		{
			jdomDocument = null;
			throw new JDOMHelperException(
				"jdh(Stream): exception"
				+ " error:" + e
				);
		}

		if( jdomDocument == null )
			throw new JDOMHelperException(
				"jdh(inStream): null document from stream '" +
				inStream + "'"
				);

		_jdomInit( jdomDocument.getRootElement() );

		//jdomDocument.getRootElement().detach();
	}


	/*
	 * Constructor # 6: Construct recursively, expanding include tags
	 */
	public JDOMHelper( String inURI, String optBaseURI,
		int inLevelCounter
		)
			throws JDOMHelperException
	{
		this( inURI, optBaseURI, inLevelCounter, null );
	}
	public JDOMHelper( String inURI, String optBaseURI,
		int inLevelCounter, AuxIOInfo inAuxInfo
		)
			throws JDOMHelperException
	{
		final String kFName = "constructor(6)";
		final String kExTag = kClassName + ':' + kFName + ": ";
		// We will only recurse up to 10 levels
		final int kMaxLevels = 10;

		if( inLevelCounter < 0 )
			throw new JDOMHelperException( kExTag
				+ "Invalid recursion level passed in, must be >= 0"
				+ "and <= to max=" + kMaxLevels + ", was called with " + inLevelCounter
				);
		if( inLevelCounter > kMaxLevels )
			throw new JDOMHelperException( kExTag
				+ "Have exceeded maximum recursion levels of include"
				+ ", max=" + kMaxLevels + ", called with " + inLevelCounter
				);
		

		// Normalize and sanity check
		inURI = NIEUtil.trimmedStringOrNull( inURI );
		if( inURI == null )
			throw new JDOMHelperException( kExTag
				+ "Null/empty URI passed into constructor."
				);

		infoMsg( kFName,
			"Processing URI \"" + inURI + "\""
			+ " with optional base URI \"" + optBaseURI + "\""
			+ ", at include level " + inLevelCounter
			);

		// Lower case for sanity checking
		String checkName = inURI.toLowerCase();

		// We have special handling for PDF and HTML files
		boolean isHTML = false;
		boolean isPDF = false;
		if( checkName.endsWith( ".pdf" ) )
			isPDF = true;
		else if( checkName.endsWith(".html")
			|| checkName.endsWith(".htm")
			|| checkName.endsWith(".shtml")
			)
			isHTML = true;
		if( isPDF || isHTML )
			throw new JDOMHelperException( kExTag
				+ "Can not use this constructor (which expands <include> tags)"
				+ " on PDF or HTML files; it's only valid for XML."
				);

		// Open a stream
		InputStream fin = null;
		// We'd like the underlying routines to tell us a bit
		// about the path they eventually wound up opening
		AuxIOInfo auxPathInfo = inAuxInfo;
		if( null == auxPathInfo )
			auxPathInfo = new AuxIOInfo();

		try
		{
			// open it relative to whatever we might have opened before
			// we have no username, password nor AuxIORecord
			fin = NIEUtil.openURIReadBin(
				inURI, optBaseURI,
				null, null, auxPathInfo, false
				);
		}
		catch( Exception e )
		{
			throw new JDOMHelperException( kExTag
				+ "Unable to open input stream."
				+ " URI=\"" + inURI + "\""
				+ " Reason: " + e
				);
		}

		// Get the final URL
		String finalURI = auxPathInfo.getFinalURI();
		debugMsg( kFName, "Final URI was \"" + finalURI + "\"" );
		// Also save it as a field
		fFinalURI = finalURI;

		// Now construct the JDOM object
		try
		{
			jdomDocument = jdomBuilder.build( fin );
		}
		catch( Exception e )
		{
			throw new JDOMHelperException( kExTag
				+ "Unable to create JDOM document."
				+ " URI=\"" + finalURI + "\""
				+ " Reason: " + e
				);
		}

		try
		{
			fin.close();
		}
		catch( Exception e )
		{
			warningMsg( kFName,
				"Unable to close stream."
				+ " Will continue with initialization."
				+ " URI=\"" + finalURI + "\""
				+ " Exception: " + e
				);
		}

		// Now traverse the tree and look for include children
		Element currElem = jdomDocument.getRootElement();
		// This recursive method can also throw an exception
		// If it's happy, it keeps adding to currElem
		// We pass in the DIRECTORY of our parent XML file
		// If parent XML file was in the current directory, parent will
		// be null, so tell the Dir method that's OK
		recursiveIncludes(
			currElem,
			NIEUtil.calculateDirOfURI( finalURI, false ),
			inLevelCounter,
			auxPathInfo
			);

		// We still do this at the very end
		_jdomInit( jdomDocument.getRootElement() );


		// And one final item, IF we're the TOP level
		// We'd like to make sure we know what the final path was
		// in case anybody cares
		// (since we may share an auxioinfo object, important
		// to reset it after recursion)
		if( 0 == inLevelCounter
			&& null != fFinalURI
			&& null != auxPathInfo
			)
		{
			auxPathInfo.setFinalURI( fFinalURI );
		}


	}

	// This is a helper for the recursive constructor
	private void recursiveIncludes( Element inCurrElem,
		String inBaseURI, int inLevelCounter,
		AuxIOInfo inAuxInfo
		)
		throws JDOMHelperException
	{
		final String kFName = "recursiveIncludes";
		final String kExTag = kClassName + '.' + kFName + ": ";

		boolean trace = shouldDoTraceMsg( kFName );

		if(trace)
			traceMsg( kFName,
				"Starting: "
				+ " inBaseURI=\"" + inBaseURI + "\""
				+ " inLevelCounter=\"" + inLevelCounter + "\""
				);

		String currName = inCurrElem.getName();
		if( currName.equals( INCLUDE_TAG ) )
			throw new JDOMHelperException( kExTag
				+ "Can not expand <include> tag as top level element."
				+ " base URI = \"" + inBaseURI + "\""
				);
		List children = inCurrElem.getChildren();
		// The easy case, no children, nothing to do!  And normal, so no error/warning
		if( children == null && children.size() < 1 )
			return;

		// We need to get a COPY of the children list because we will
		// be modifying it
		Object [] childrenAry = children.toArray();

		// For each child
		// for( Iterator it = children.iterator(); it.hasNext(); )
		// {
		// 	Element child = (Element)it.next();
		for( int i = 0; i < childrenAry.length ; i++ )
		{
			Element child = (Element)childrenAry[i];
			String childName = child.getName();
			// If this is not an include tag, just search down it
			if( ! childName.equals( INCLUDE_TAG ) )
			{
				recursiveIncludes( child, inBaseURI,
					inLevelCounter, inAuxInfo
					);
			}
			// Else this IS an include node
			else
			{

				// A sanity check
				List GrandChildren = child.getChildren();
				if( GrandChildren != null && GrandChildren.size() > 0 )
					throw new JDOMHelperException( kExTag
						+ "<include> tags must not have any children"
						+ "; the entire tree of the new file REPLACES the include tag"
						+ ", so the existing children would be lost."
						+ " This offending include tag is the child of \"" + currName + "\"."
						+ ", base URI = \"" + inBaseURI + "\""
						);
				// Get the URI of the included file
				String location = getStringFromAttributeTrimOrNull(
					child, INCLUDE_LOCATION_ATTR, false
					);
				// Sanity
				if( location == null )
					throw new JDOMHelperException( kExTag
						+ "Include tags must have a location attribute."
						+ " This offending include tag is the child of \"" + currName + "\"."
						+ ", base URI = \"" + inBaseURI + "\""
						);
				infoMsg( kFName,
					"Will read in included XML file \"" + location + "\""
					+ " relative to \"" + inBaseURI + "\""
					+ "  at level " + inLevelCounter
					);

				// Now call that constructor
				// If there's a problem, it will have thrown an error
				JDOMHelper newDoc = new JDOMHelper(
					location, inBaseURI, inLevelCounter+1, inAuxInfo
					);
				Element newChild = newDoc.getJdomElement();
				newChild.detach();

				// Add some system meta data to the new child
				if( (null==inAuxInfo && AuxIOInfo.DEFAULT_RECORD_INCLUDES)
					|| inAuxInfo.getRecordInternalIncludeAttrs()
				) {
					newChild.setAttribute( SYSTEM_ATTR_INCLUDE_LOCATION, location );
					if( null!=inBaseURI )
						newChild.setAttribute( SYSTEM_ATTR_BASE_URI, inBaseURI );
					newChild.setAttribute( SYSTEM_ATTR_LEVEL, "" + (inLevelCounter+1) );
				}

				//traceMsg( kFName,
				//	"new child=" + NIEUtil.NL
				//	+ JDOMHelper.JDOMToString( newChild, true )
				//	);


				// We now have a new child!

				// We need to replace the old child with the new one
				// Find out where the old one was
				int oldChildAt = children.indexOf( child );
				// Shove the new one in its place
				children.add( oldChildAt, newChild );
				// And remove the old one
				// These next two may be redundant, but harmless
				child.detach();
				children.remove( child );


			}	// End else this IS an include node

		}	// End for each child


	}



	/*
	 * Constructor # 7: default constructor
	 */

	public JDOMHelper()
		throws JDOMHelperException
	{
		//synchronized( jdomBuilder )
		//{
			//if( jdomBuilder == null )
			//	jdomBuilder = new SAXBuilder();
			jdomDocument = new Document( new Element("root") );
		//}
		_jdomInit( jdomDocument.getRootElement() );
	}

	private void _jdomInit( Element inputElement )
		throws JDOMHelperException
	{
		if( inputElement == null )
			throw new JDOMHelperException( "Null element passed in to init." );

		// Store the reference to our jdom element
		myElement = inputElement;

		String desiredElementName = _getDesiredTopLevelElementName();
		if( desiredElementName != null )
		{
			boolean haveSeenValidName = false;
			String buffer = desiredElementName;
			StringTokenizer st = new StringTokenizer( buffer );
			while( st.hasMoreTokens() )
			{
				String item = st.nextToken();

				// todo: revisit case sensitivity, probably bad, would also
				// have to change logic in dpapp
				//item = item.trim().toLowerCase();

				if( item != null && ! item.equals("") )
				{
					if( getElementName().equals(item) )
					{
						haveSeenValidName = true;
						break;
					}
				}
			}
			if( ! haveSeenValidName )
				throw new JDOMHelperException(
					"Expected the element name to match in '" +
					desiredElementName + "' but got a '" +
					inputElement.getName() + "' element instead."
					);
		}  // end if have a desired name

	}

	// Do a "deep" clone
	public synchronized Object clone()
	{
		final String kFName = "clone";

		// Cloning should take place in a try block
		try
		{
			// Get a default clone of myself
			JDOMHelper me2 = (JDOMHelper)(super.clone());

			// Duplicate memeber variables

			// If we have a document, we need to reproduce both
			// the document and the my element variable
			// in the proper order
			if( jdomDocument != null )
			{
				// Clone the document
				me2.jdomDocument = (Document)jdomDocument.clone();
				// Just adjust the reference to the new document
				// in the clone
				if( myElement != null )
					me2.myElement = (Element)me2.jdomDocument.getRootElement();
			}
			// Else we didn't have a document
			// but we probably still have an element
			else
			{
				if( myElement != null )
					me2.myElement = (Element)myElement.clone();
			}

			if( attrListCache != null )
				me2.attrListCache = (Hashtable)attrListCache.clone();

			// sourceURI is an immutable string
			// SAXBuilder is static
			// debug is static

			return me2;

		}
		catch (CloneNotSupportedException e)
		{
			errorMsg( kFName, "Clone failed, reason: " + e );
			return this;
		}

	}


	// Simplified construction of nested sub objects
	// ///////////////////////////////////////////////////////////////////
	// Per Kevin
	// I start with a main config tree of some sort that I have constructed
	// I wouuld like to have some sub branches represent sub-objects
	// My sub objects have a constructor that takes a JDOM Elemen or helper
	// as an argument
	//
	// How to use:
	// Create your component/branch class - you will need to know the name
	// Create your main config
	// Decide on a path to the sub object
	// Call one of these routines
	// Cast the result back to your object type
	// You can have exceptions, or you can just get back a null

	// Make it from this instance


	public Object makeObjectFromConfigPath( String inClassName, String inPath )
		throws JDOMHelperException
	{
		return makeObjectFromConfigPath(
			getJdomElement(), inClassName, inPath
			);
	}
	// Make it from this instance, no exceptions thrown
	public Object makeObjectFromConfigPathOrNull(
		String inClassName, String inPath
		)
	{
		return makeObjectFromConfigPathOrNull(
			getJdomElement(), inClassName, inPath
			);
	}
	// Static version with no exceptions
	public static Object makeObjectFromConfigPathOrNull(
		Element inElem, String inClassName, String inPath
		)
	{
		final String kFName = "makeObjectFromConfigPathOrNull(2)";
		Object obj = null;
		try
		{
			obj = makeObjectFromConfigPath( inElem, inClassName, inPath );
		}
		catch( JDOMHelperException e )
		{
			debugMsg( kFName,
				"Got exception instantiating class " + inClassName
				+ " so will return null; this may be perfectly normal for"
				+ " this application."
				+ " Exception was \"" + e + "\""
				);
			return null;
		}
		return obj;
	}
	// Static version, with exceptions
	// This is the actual implementation
	public static Object makeObjectFromConfigPath( Element inElem,
		String inClassName, String inPath
		)
		throws JDOMHelperException
	{
		final String kFName = "makeObjectFromConfigPath(2)";
		final String kExTag = kClassName + ':' + kFName + ": ";
		if( inElem==null || inClassName==null || inPath==null )
		{
			throw new JDOMHelperException( kExTag
				+ "Was passed null input(s):"
				+ " inElem=" + inElem
				+ "inClassName=" + inClassName
				+ "inPath=" + inPath
				);
		}

		// First, let's see if we even have this branch
		Element targetElement = JDOMHelper.findElementByPath(
			inElem, inPath
			);
		// Bail if we didn't find it
		if( targetElement == null )
		{
			throw new JDOMHelperException( kExTag
				+ "Didn't find requested path in XML tree."
				+ " Path = \"" + inPath + "\"."
				+ " Class name = \"" + inClassName + "\"."
				);
		}

		// Now get the class
		Class theClass = null;
		try
		{
			theClass = Class.forName( inClassName );
		}
		catch(Exception e1)
		{
			throw new JDOMHelperException( kExTag
				+ "Unable to locate class \"" + inClassName + "\"."
				+ "You might want to check your spelling or your class path?"
				+ "Exception was \"" + e1 + "\"."
				);
		}

		// A long try/catch block for the many things that can go wrong
		try
		{

			// Look for a constructor that takes an Element
			Class argTypes1[] = { Class.forName("org.jdom.Element") };
			boolean foundIt = false;
			Constructor cons1 = null;
			// We want to catch this ONE exception here
			try
			{
				cons1 = theClass.getConstructor( argTypes1 );
				foundIt = true;
			}
			catch(Exception e3)
			{
				// This is perfectly OK, we'll try something else later
				foundIt = false;
			}

			// If we found it, go ahead and use it!
			// And we will return the object in this block
			Object answer = null;
			if( foundIt )
			{
				// Prepare the arguments for the constructor
				Object args1[] = { targetElement };
				// Call the constructor
				answer = cons1.newInstance( args1 );
				// Return the answer
				return answer;
			}

			// OK, so we didn't find a constructor using an Element
			// That's fine, we'll look for one using JDOMHelper
			// From this point on, any failure means we really are stuck
			// and we will let it fall through

			// Look for a constructor that takes an Element
			Class argTypes2[] = { Class.forName("nie.core.JDOMHelper") };
			Constructor cons2 = theClass.getConstructor( argTypes2 );

			// If we're still here, great, it means we found a matching
			// constructor, so now we have to promote the element into
			// a full jdom helper for them, and again any exceptions from
			// here will just fall through

			// Promote it
			// This is calling the JDOMHelper constructor, of course,
			// vs the eventual constructor for what we're trying to create (below)
			JDOMHelper jdhNode = new JDOMHelper( targetElement );

			// Prepare the arguments for the user's class constructor
			Object args2[] = { jdhNode };
			// Call the constructor
			answer = cons2.newInstance( args2 );

			// Return the answer
			return answer;

		}
		catch(Exception e2)
		{
			throw new JDOMHelperException( kExTag
				+ "Unable to instantiate class \"" + inClassName + "\"."
				+ " Perhaps there is no constructor that takes either a"
				+ " JDOMHelper node or an Element?"
				+ " Exception was \"" + e2 + "\"."
				);
		}

		// Answers returned above, if an exception was not thrown
	}


	/********************************************
	 *
	 *		Simple Get / Set Methods
	 *
	 *********************************************/

	private void __sep__Simple_Get_Set_Methods__() {}

	public Element getJdomElement()
	{
		return myElement;
	}

	public String getElementName()
	{
		return getJdomElement().getName();
	}

	public String getTextTrim()
	{
		return getTextTrim( getJdomElement() );
	}
	public static String getTextTrim( Element inElem )
	{
		return inElem.getTextTrim();
	}

	public String getTextTrimOrNull()
	{
		return getTextTrimOrNull( getJdomElement() );
	}
	public static String getTextTrimOrNull( Element inElem )
	{
		String tmpStr = inElem.getText();
		tmpStr = NIEUtil.trimmedStringOrNull( tmpStr );
		return tmpStr;
	}

	public String getTextSuperTrimOrNull()
	{
		return getTextSuperTrimOrNull( getJdomElement() );
	}
	public static String getTextSuperTrimOrNull( Element inElem )
	{
		String tmpStr = inElem.getTextNormalize();
		tmpStr = NIEUtil.trimmedStringOrNull( tmpStr );
		return tmpStr;
	}


	public String getTreeText()
	{
		return getTreeText( getJdomElement() );
	}


	public static String getTreeText( Element inElem )
	{
		return getTreeText( inElem, false );
	}

	public static String getTreeText( Element inElem, boolean inDoHTMLHelper )
	{
		return getTreeText( inElem, inDoHTMLHelper, true );
	}


	public static String getTreeText(
		Element inElem,
		boolean inDoHTMLHelper,
		boolean inDoFinalTextCleanup
		)
	{
		final String kFName = "getTreeText";
		if( null==inElem ) {
			errorMsg( kFName, "Null element passed in, returning null." );
			return null;
		}

		final boolean kShouldCacheText = false;

		/***
		final String [] kSpaceTagsAry = {
			"html", "head", "body", "meta", "table", "tr", "td", "th", "br", "p",
			"ul", "ol", "dl", "li", "dt", "dd",
			"form", "select", "option"
		};
		final HashSet kSpaceTags = new HashSet( kSpaceTagsAry );
		***/

		StringBuffer outBuff = new StringBuffer();

		if( inDoHTMLHelper ) {
			String tagName = inElem.getName();
			// Ignore junk
			if( tagName.equals("script") || tagName.equals("style") ) {
				return null;
			}
			// Include the alt text of images
			else if( tagName.equals("img") || tagName.equals("input") ) {
				String altText = inElem.getAttributeValue( "alt" );
				altText = NIEUtil.trimmedStringOrNull( altText );
				if( null!=altText )
					outBuff.append( "[ " ).append( altText ).append( " ]" );
			}
			// Include the meta fields description and keywords
			else if( tagName.equals("meta") ) {
				tagName = inElem.getAttributeValue( "name" );
				tagName = NIEUtil.trimmedLowerStringOrNull( tagName );
				if( null!=tagName && (tagName.equals("keywords") || tagName.equals("description")) ) {
					String content = inElem.getAttributeValue( "content" );
					content = NIEUtil.trimmedStringOrNull( content );
					if( null!=content )
						outBuff.append( content );
				}
			}
		}


		// For each part of the content
		for( Iterator it = inElem.getContent().iterator(); it.hasNext() ; ) {
			Object obj = it.next();
			String theText = null;
			// Figure out which text to get
			// Is it simple text?
			if( obj instanceof Text ) {
				Text textElem = (Text) obj;
				theText = textElem.getTextNormalize();
			}
			// or simple CDATA
			if( obj instanceof CDATA ) {
				CDATA cElem = (CDATA) obj;
				theText = cElem.getTextNormalize();
			}
			// Is it an element so we have to traverse?
			else if( obj instanceof Element ) {
				Element currChild = (Element) obj;
				theText = getTreeText( currChild, inDoHTMLHelper, false );
			}
			// Add the text, if we got any
			if( null!=theText && theText.length() > 0 ) {
				if( outBuff.length() > 0 )
					outBuff.append( ' ' );
				outBuff.append( theText );
			}
		}

		// We're done!
		if( outBuff.length() > 0 ) {
			// Cleanup nbsp stuff
			String out = new String( outBuff );

			if( inDoFinalTextCleanup || kShouldCacheText ) {
				/***
				statusMsg( kFName, "Chars " );
				for( int i=0; i<outBuff.length() ; i++ )
					System.out.print( "" + outBuff.charAt(i) + "(" + (int)outBuff.charAt(i) + ") " );
				System.out.println();
				out = NIEUtil.zapChars( out, '\240' );
				***/
				// Nuke the bogus nbsp stuff
				out = NIEUtil.replaceChars( out, '\240', ' ' );

				if( inDoHTMLHelper && null!=out ) {
					out = NIEUtil.cleanupFancyHTML8BitChars( out );
				}

				// return NIEUtil.trimmedStringOrNull( out );
				// We want to normalize the string's spaces, so we piggy back
				// on JDOM's method for doing that
				Element tmpElem = new Element( "tmp" );
				tmpElem.addContent( out );
				out = tmpElem.getTextNormalize();
				out = NIEUtil.trimmedStringOrNull( out );
			}

			// Should we cache the text at this level?
			if( kShouldCacheText && null!=out ) {
				inElem.setAttribute( "_text", out );
			}

			return out;
		}
		else
			return null;
	}



	public List getJdomChildren()
	{
		return getJdomElement().getChildren();
	}

	public int getJdomChildrenCount()
	{
		return getJdomChildren().size();
	}

	public Element getJdomChildByOffset( int i )
	{
		return (Element)getJdomChildren().get(i);
	}

	public String getClassName()
	{
		return this.getClass().getName();
	}


	// Return the URI we read this XML from
	// We'd prefer the more specific / fully qualified version
	// Warning: may be null, not always created from a file/url
	public String getURI()
	{
		String tmpStr = NIEUtil.trimmedStringOrNull( fFinalURI );
		if( tmpStr != null )
			return tmpStr;
		else
			return NIEUtil.trimmedStringOrNull( fSourceURI );
	}


	// void setDesiredTopLevelElementName( String target )
	// {
	//		 acceptableName = target;
	// }
	// no way to call this before constructor, don't feel like
	// complicating constructors right now.


	private void __sep__Simple_JDOM_Wrappers_() {}
	public Element addContent( JDOMHelper newNode )
	{
		return addContent( this, newNode );
	}
	public static Element addContent( JDOMHelper inParent,
		JDOMHelper inNewChild
		)
	{
		final String kFName = "addContent(j,j)";

		if( inParent==null || inNewChild==null )
		{
			errorMsg( kFName,
				"Null inputs."
				+ " inParent=" + inParent
				+ " inNewChild=" + inNewChild
				+ " Returning null."
				);
			return null;
		}
		Element parent = inParent.getJdomElement();
		Element child = inNewChild.getJdomElement();
		child.detach();
		return parent.addContent( child );
	}
	public Element addContent( Element newNode )
	{
		return addContent( this, newNode );
	}
	public static Element addContent( JDOMHelper inParent, Element inNewChild )
	{
		final String kFName = "addContent(j,e)";

		if( inParent==null || inNewChild==null )
		{
			errorMsg( kFName,
				"Null inputs."
				+ " inParent=" + inParent
				+ " inNewChild=" + inNewChild
				+ " Returning null."
				);
			return null;
		}
		Element parent = inParent.getJdomElement();
		inNewChild.detach();
		return parent.addContent( inNewChild );
	}


	public static Element addContent( Element inParent, JDOMHelper inNewChild )
	{
		final String kFName = "addContent(e,j)";
		if( inParent==null || inNewChild==null )
		{
			errorMsg( kFName,
				"Null inputs."
				+ " inParent=" + inParent
				+ " inNewChild=" + inNewChild
				+ " Returning null."
				);
			return null;
		}
		Element child = inNewChild.getJdomElement();
		child.detach();
		return inParent.addContent( child );
	}

	/********************************************************
	 *
	 *		Help with Reading and Writing Attributes
	 *
	 *********************************************************/
	private void __sep__Reading_and_Writing_Attributes() {}

	// When requesting a string, do NOT look for plural by default

	public String getStringFromAttribute( String target )
	{
		return getStringFromAttribute( myElement, target, false );
	}
	public static String getStringFromAttribute( Element elem, String target )
	{
		return getStringFromAttribute( elem, target, false );
	}

	// These versions trim strings and force empty strings to nulls
	public String getStringFromAttributeTrimOrNull( String target )
	{
		return getStringFromAttributeTrimOrNull(
			myElement, target, false
			);
	}
	public static String getStringFromAttributeTrimOrNull(
		Element elem, String target
		)
	{
		return getStringFromAttributeTrimOrNull( elem, target, false );
	}

	public static String getStringFromAttributeOrDefaultValue(
		Element inElem, String inTarget, String inDefaultValue
		)
	{
		String tmpVal = getStringFromAttributeTrimOrNull(
			inElem, inTarget, false
			);
		if( null != tmpVal )
			return tmpVal;
		else
			return inDefaultValue;
	}

	// Get a string, optionally check for plural form
	// We always prefer the singular form

	public String getStringFromAttribute( String target,
		boolean allowPlural )
	{
		return getStringFromAttribute( myElement, target, allowPlural );
	}
	public String getStringFromAttributeTrimOrNull( String target,
		boolean allowPlural )
	{
		return getStringFromAttributeTrimOrNull( myElement, target, allowPlural );
	}
	public static String getStringFromAttribute( Element elem, String target,
		boolean allowPlural )
	{
		final String kFName = "getStringFromAttribute";
		String tmp = null;
		tmp = elem.getAttributeValue(target);
//		statusMsg( kFName,
//			"Element \"" + elem.getName() + "\""
//			+ ", attribute \"" + target + "\""
//			+ ", value \"" + tmp + "\""
//			);
		if( tmp == null && allowPlural )
			tmp = elem.getAttributeValue( target+"s" );
		return tmp;
	}
	public static String getStringFromAttributeTrimOrNull(
		Element elem,	String inTarget, boolean inAllowPlural
		)
	{
		String tmpStr = null;
		tmpStr = elem.getAttributeValue( inTarget );
		tmpStr = NIEUtil.trimmedStringOrNull( tmpStr );
		if( tmpStr == null && inAllowPlural )
		{
			tmpStr = elem.getAttributeValue( inTarget + "s" );
			tmpStr = NIEUtil.trimmedStringOrNull( tmpStr );
		}
		return tmpStr;
	}

	public boolean setAttributeString( String attrName, String attrValue )
	{
		return setAttributeString( getJdomElement(), attrName, attrValue );
	}
	public static boolean setAttributeString( Element elem, String attrName,
		String attrValue
		)
	{
		final String kFName = "setAttributeString";

		if( elem==null || attrName==null || attrName.trim().equals("")
			|| attrValue==null
			)
		{
			errorMsg( kFName,
			// throw new Error(
				"Null/empty values input."
				+ " elem=" + elem
				+ " attrName=" + attrName
				+ " attrValue=" + attrValue
				+ " (attrValue can be an empty string, but not a true NULL)"
				+ " Returning false (failure)."
				);
			return false;
		}
		attrName = attrName.trim();
		elem.setAttribute( attrName, attrValue );
		return true;
	}

	public boolean setAttributeInt( String attrName, int attrValue )
	{
		return setAttributeInt( getJdomElement(), attrName, attrValue );
	}
	public static boolean setAttributeInt( Element elem, String attrName,
		int attrValue
		)
	{
		final String kFName = "setAttributeInt";

		if( elem==null || attrName==null || attrName.trim().equals("") )
		{
			errorMsg( kFName,
				"Null/empty values input."
				+ " elem=\"" + elem + "\""
				+ " attrName=\"" + attrName + "\""
				+ " attrValue=\"" + attrValue + "\""
				+ " Returning false (failur)."
				);
			return false;
		}
		attrName = attrName.trim();
		elem.setAttribute( attrName, "" + attrValue );
		return true;
	}

	public boolean setAttributeStringForPath(
		String inPath, String inAttrName, String inAttrValue
		)
	{
		return setAttributeStringForPath(
			getJdomElement(),
			inPath, inAttrName, inAttrValue
			);
	}
	public static boolean setAttributeStringForPath(
		Element inStartElem,
		String inPath, String inAttrName, String inAttrValue
		)
	{
		final String kFName = "setAttributeStringForPath";

		if( inStartElem==null || inPath==null
			|| inAttrName==null || inAttrValue==null
			)
		{
			errorMsg( kFName,
				"Was passed NULL argument(s)."
				+ " inStartElem=" + inStartElem
				+ " inPath=" + inPath
				+ " inAttrName=" + inAttrName
				+ " inAttrValue=" + inAttrValue
				+ " Returning false (failure)."
				);
			return false;
		}

		Element destElem = findElementByPath( inStartElem, inPath );
		if( destElem == null )
			return false;

		destElem.setAttribute( inAttrName, inAttrValue );

		return true;
	}

	// No warnings for nulls
	public static boolean decodeBooleanStringOBS( String inData, boolean defaultValue )
	{
		final String kFName = "decodeBooleanString";

		if( inData == null || inData.trim().equals("") )
			return defaultValue;

		inData = inData.trim().toLowerCase();

		// Todo: other ideas: good, pass, passed
		if( inData.equals("1") ||
			inData.equals("yes") || inData.equals("y") ||
			inData.equals("true") || inData.equals("t") ||
			inData.equals("affirmative") ||
			inData.equals("positive") || inData.equals("+")
			)
		{
			return true;
		}
		// Todo: other ideas: bad, fail, failed
		else if( inData.equals("0") ||
			inData.equals("no") || inData.equals("n") ||
			inData.equals("false") || inData.equals("f") ||
			inData.equals("negative") || inData.equals("-")
			)
		{
			return false;
		}
		else
		{
			errorMsg( kFName,
				"Unknown string \"" + inData + "\""
				+ ", returning default value \"" + defaultValue + "\""
				);
			return defaultValue;
		}
	}

	public boolean getBooleanFromAttribute( String target )
	{
		return getBooleanFromAttribute( myElement, target, false );
	}
	public static boolean getBooleanFromAttribute( Element elem, String target )
	{
		return getBooleanFromAttribute( elem, target, false );
	}

	public boolean getBooleanFromAttribute( String target,
		boolean defaultValue
		)
	{
		return getBooleanFromAttribute( myElement, target, defaultValue );
	}
	public static boolean getBooleanFromAttribute( Element elem,
		String target, boolean defaultValue
		)
	{
		final String kFName = "getBooleanFromAttribute(2)";

		debugMsg( kFName, "Start. target='" + target + "', default value='" + defaultValue + "'" );
		String tmpFlag = getStringFromAttribute( elem, target );
		debugMsg( kFName, "tmpFlag='" + tmpFlag + "'" );
		if( tmpFlag == null || tmpFlag.trim().equals("") )
		{
			debugMsg( kFName, "It was null, returning default " + defaultValue );
			return defaultValue;
		}
		boolean answer = NIEUtil.decodeBooleanString( tmpFlag, defaultValue );
		debugMsg( kFName, "String was decoded to " + answer );
		return answer;
	}

	public int getIntFromAttribute( String target,
		int defaultValue )
	{
		return getIntFromAttribute( myElement, target, defaultValue );
	}
	public static int getIntFromAttribute( Element elem,
		String target, int defaultValue
		)
	{
		final String kFName = "getIntFromAttribute(2)";

		// debugMsg( kFName, "Start. target='" + target + "', default value='" + defaultValue + "'" );
		String tmpFlag = getStringFromAttribute( elem, target );
		// debugMsg( kFName, "tmpFlag='" + tmpFlag + "'" );
		if( tmpFlag == null || tmpFlag.trim().equals("") )
		{
			debugMsg( kFName, "It was null, returning default." );
			return defaultValue;
		}
		tmpFlag = tmpFlag.trim();
		int retValue = defaultValue;
		try
		{
			retValue = Integer.parseInt( tmpFlag );
			// debugMsg( kFName, "Value string decoded to " + retValue );
		}
		catch(Exception e)
		{
			retValue = defaultValue;
			errorMsg( kFName,
				"Failed to decode \"" + tmpFlag + "\" to an integer."
				+ " Exception was: " + e
				+ " Returning default value of " + defaultValue
				);
		}

		return retValue;

	}

	public long getLongFromAttribute( String target,
		long defaultValue )
	{
		return getLongFromAttribute( myElement, target, defaultValue );
	}
	public static long getLongFromAttribute( Element elem,
		String target, long defaultValue
		)
	{
		final String kFName = "getLongFromAttribute(2)";

		// debugMsg( kFName, "Start. target='" + target + "', default value='" + defaultValue + "'" );
		String tmpFlag = getStringFromAttribute( elem, target );
		// debugMsg( kFName, "tmpFlag='" + tmpFlag + "'" );
		if( tmpFlag == null || tmpFlag.trim().equals("") )
		{
			debugMsg( kFName, "It was null, returning default." );
			return defaultValue;
		}
		tmpFlag = tmpFlag.trim();
		long retValue = defaultValue;
		try
		{
			retValue = Long.parseLong( tmpFlag );
			// debugMsg( kFName, "Value string decoded to " + retValue );
		}
		catch(Exception e)
		{
			retValue = defaultValue;
			errorMsg( kFName,
				"Failed to decode \"" + tmpFlag + "\" to an long integer."
				+ " Exception was: " + e
				+ " Returning default value of " + defaultValue
				);
		}

		return retValue;

	}


	// XML allows for a tag attribute to contain a LIST of
	// values, separated by white space.
	// These methods help manage that.

	// Will always return a list (assuming valid cache)

	public List getListFromAttribute( String target )
	{
		return getListFromAttribute( target, false );
	}

	public static List getListFromAttribute( Element elem, String target )
	{
		return getListFromAttribute( elem, target, false );
	}


	public List getListFromAttribute( String target,
		boolean allowPlural
		)
	{
		// Create the global cache table if it's not there

		if( attrListCache == null )
			attrListCache = new Hashtable();

		// Do we already know the answer?
		// No, calculate it and cache it

		if( ! attrListCache.containsKey(target) )
		{

			// Call the static version
			List retList = getListFromAttribute(
				myElement, target, allowPlural
				);

			// Cache the results
			attrListCache.put(target, retList);
		}

		// Return the answer

		return (List)attrListCache.get(target);
	}

	// This is the static version
	// Of course it does no caching
	// This also does the real work for the non-static version,
	// but the non-static version implements caching and wraps
	// that logic on top of this
	public static List getListFromAttribute( Element elem,
		String target, boolean allowPlural
		)
	{

		List retList = new Vector();
		String tmpStr = getStringFromAttribute( elem, target, allowPlural );
		if( tmpStr != null )
		{
			StringTokenizer st = new StringTokenizer( tmpStr );
			while( st.hasMoreTokens() )
			{
				String item = st.nextToken();
				item = item.trim().toLowerCase();
				if( ! item.equals("") )
				{
					if( ! retList.contains(item) )
						retList.add(item);
				}
			}
		}

		return retList;
	}


	// Given the name of an attribute that nornally contains one or
	// more values, separated by white space, does that list contain
	// a particular value?

	public boolean hasItemInAttrList( String listName,
		String target
		)
	{
		return hasItemInAttrList( listName, target, false );
	}

	public static boolean hasItemInAttrList( Element elem, String listName,
		String target
		)
	{
		return hasItemInAttrList( elem, listName, target, false );
	}

	public boolean hasItemInAttrList( String listAttrName,
		String targetAttrValue, boolean allowPlural
		)
	{
		String tmpTarget = targetAttrValue.trim();
		return (getListFromAttribute(listAttrName,allowPlural)).contains(tmpTarget);
	}

	public static boolean hasItemInAttrList( Element elem, String listName,
		String target, boolean allowPlural
		)
	{
		String tmpTarget = target.trim();
		return (getListFromAttribute(elem,listName,allowPlural)).contains(tmpTarget);
	}

	public int getAttrListCount( String listName )
	{
		return getAttrListCount( listName, false );
	}

	public static int getAttrListCount( Element elem, String listName )
	{
		return getAttrListCount( elem, listName, false );
	}

	public int getAttrListCount( String listName,
		boolean allowPlural
		)
	{
		return (getListFromAttribute(listName,allowPlural)).size();
	}

	public static int getAttrListCount( Element elem, String listName,
		boolean allowPlural
		)
	{
		return (getListFromAttribute(elem,listName,allowPlural)).size();
	}

	// Add a string to the attribute list
	// It returns true or false depending on whether it really
	// added it or not; it won't add it if it's already there.

	public boolean addStringToAttrList( String listAttrName,
		String newValue
		)
	{
		return addStringToAttrList( listAttrName, newValue, false );
	}

	public static boolean addStringToAttrList( Element elem,
		String listAttrName, String newValue
		)
	{
		return addStringToAttrList( elem, listAttrName, newValue, false );
	}

	public boolean addStringToAttrList( String listAttrName,
		String newValue, boolean allowPlural
		)
	{
		boolean success = addStringToAttrList( myElement,
			listAttrName, newValue, allowPlural
			);

		// The non-static list attribute list methods employ caching
		// so we need to make sure and update caches in all non-static
		// attr list methods

		// If the static version was successful, then we need to clean
		// up the old cache
		if( success )
		{
			// Null out any cache
			if( attrListCache != null )
			{
				if( attrListCache.containsKey(listAttrName) )
					attrListCache.remove(listAttrName);
				// Check the plural form, if applicable
				if( allowPlural )
				{
					String pluralListName = listAttrName + 's';
					if( attrListCache.containsKey(pluralListName) )
						attrListCache.remove(pluralListName);
				}
			}
		}

		return success;
	}

	public static boolean addStringToAttrList( Element elem,
		String listAttrName, String newValue, boolean allowPlural
		)
	{

		if( newValue == null )
			return false;

		String tmpValue = newValue.trim();

		// bail if no value sent, this is not allowed in xml
		if( tmpValue.equals("") )
			return false;

		// bail if this is ALREADY in the list
		if( hasItemInAttrList( elem, listAttrName, tmpValue, allowPlural) )
			return false;

		// Get the string from the attribute
		String tmpAttrStr1 = null;
		String pluralListName = listAttrName + "s";
		tmpAttrStr1 = elem.getAttributeValue( listAttrName );

		// Do we already have a string/list there?
		if( tmpAttrStr1 != null )
		{
			// Replace the old list with a new one, with this
			// value added to the end
			elem.removeAttribute( listAttrName );
			elem.setAttribute( listAttrName,
				tmpAttrStr1 + " " + tmpValue
			);
		}
		// Else we don't already have attribute with this name
		else
		{
			// If we're allowing plural, check that string
			if( allowPlural )
			{
				String tmpAttrStr2 = null;
				tmpAttrStr2 = elem.getAttributeValue( pluralListName );
				// Did we find a plural form?
				if( tmpAttrStr2 != null )
				{
					elem.removeAttribute( pluralListName );
					elem.setAttribute( pluralListName,
						tmpAttrStr2 + " " + tmpValue
						);
				}
				// Else no plural, so add in proper singular form
				else
				{
					elem.setAttribute( listAttrName, tmpValue );
				}
			}
			// Else not allowing plural
			else
			{
				// So just create new singular form
				elem.setAttribute( listAttrName, tmpValue );
			}
		}

		return true;

	}

	public static void copyAttributes( Element inSourceElem, Element inDestElem )
	{
		final String kFName = "copyAttributes";
		if( null==inSourceElem ) {
			errorMsg( kFName, "Null source element." );
			return;
		}
		if( null==inDestElem ) {
			errorMsg( kFName, "Null destination element." );
			return;
		}

		try {
			// Copy (and clone) each of the attributes
			for( Iterator it = inSourceElem.getAttributes().iterator() ; it.hasNext() ; ) {
				Attribute srcAttr = (Attribute)it.next();
				Attribute dstAttr = (Attribute)srcAttr.clone();
				inDestElem.setAttribute( dstAttr );
			}
		}
		catch( Throwable t ) {
			errorMsg( kFName, "Error copying attributes: " + t );
		}
	}

	/******************************************************************
	 *
	 *		Support for psuedo xpath syntax and simple operations
	 *
	 *******************************************************************/
	private void __sep__Pseudo_XPath_Syntax_Support_ () {}

//	public static Element findElementByPathOLD( Element sourceElem,
//		String target, boolean inDoNavigationWarnings
//		)
//	{
//
//		// We'll save an unmodified copy for error/debug messages
//		String saveInitPath = target;
//
//		if(debug) System.err.println( "Debug: JDOMHelper.path: Start: given '"+
//			target + "'"
//			);
//
//		if( sourceElem == null )
//		{
//			System.err.println( "Error: JDOMHelper:findElementByPath:"
//				+ " Was called with a null or empty starting element."
//				+ " Returning null."
//				);
//			return null;
//		}
//
//		target = NIEUtil.trimmedStringOrNull( target );
//		if( target == null )
//		{
//			System.err.println( "Error: JDOMHelper:findElementByPath:"
//				+ " Was called with a null or empty path string."
//				+ " Returning null."
//				);
//			return null;
//		}
//
//		// Init the return element, this will be
//		// "walked" down the tree as we traverse the path
//		Element retElem = null;
//
//		if( ! target.substring(0,1).equals("/") )
//		{
//			// If it does NOT start with a /, then it's
//			// relative to the top level node; ie the
//			// first part/part2 is a child of root.
//			retElem = sourceElem;
//		}
//		else
//		{
//			// Else we're starting at "root"
//			// Kill the initial slash AND
//			// we leave the retELem set to null from
//			// above, so it will be handled correctly below.
//			target = target.substring(1);
//		}
//
//		if(debug) System.err.println( "Debug: JDOMHelper.path: target now ='"+
//			target + "'"
//			);
//
//		// Keep walking the path until we're done or run out of path
//		while( true )
//		{
//			// sanity check if we're out of path
//			if( target.length() < 1 )
//				break;
//
//			// Find the next slash
//			int nextSlash = target.indexOf('/');
//
//			// Init the current path section
//			String pathSection = "";
//
//			// If no slash, just grab the rest
//			if( nextSlash < 0 )
//			{
//				pathSection = target;
//				target = "";
//			}
//			else // else it was >= 0
//			{
//				// If a slash, then grab the next section and
//				// move forward
//				if( nextSlash == 0 || nextSlash == target.length()-1 )
//				{
//					System.err.println( "Error: JDOMHelper:findElementByPath:"
//						+ " Found misplaced forward slash when walking through the path."
//						+ " This might have happened if, for example, there were accidently 2 slashes"
//						+ " next to each other, or if it ended in a slash."
//						+ " Initial path passed in was \"" + saveInitPath + "\"."
//						+ " The path currently being checked was \"" + target + "\"."
//						+ " Returning null."
//						);
//					return null;
//				}
//				// Grab the next section, and move the target along
//				pathSection = target.substring( 0, nextSlash );
//				target = target.substring( nextSlash+1 );
//			}
//
//			pathSection = pathSection.trim();
//
//			// Sanity check, we don't allow /(null)/
//			// This shouldn't really be possible...
//			if( pathSection.equals("") )
//			{
//				System.err.println( "Error: JDOMHelper:findElementByPath:"
//					+ " Found empty path segment when walking through the path."
//					+ " This might have happened if, for example, there were accidently 2 slashes"
//					+ " next to each other or were separated by just whitespace."
//					+ " Initial path passed in was \"" + saveInitPath + "\"."
//					+ " Returning null."
//					);
//				return null;
//			}
//
//			// Look for offset stuff
//			// Offsets are ONE based, not zero based, to comply with xpath
//			int listIndex = 0;
//			int openBracketAt = pathSection.indexOf( '[' );
//
//			// If we found an opening bracket
//			if( openBracketAt >= 0 )
//			{
//				// Look for ending bracket
//				int closeBracketAt = pathSection.indexOf( ']',
//					openBracketAt
//					);
//				// Open bracket with no close is bad
//				if( closeBracketAt < 0 )
//				{
//					System.err.println( "Error: JDOMHelper:findElementByPath:"
//						+ " Found unbalanced square brackets."
//						+ " Initial path passed in was \"" + saveInitPath + "\"."
//						+ " Section of the path being checked was \"" + pathSection + "\"."
//						+ " Returning null."
//						);
//					return null;
//				}
//
//				// If there's no characters between the
//				// brackets that's bad as well
//				if( closeBracketAt == openBracketAt-1 )
//				{
//					System.err.println( "Error: JDOMHelper:findElementByPath:"
//						+ " Found adjacent set of square brackets."
//						+ " Square brackets must have something between them."
//						+ " Initial path passed in was \"" + saveInitPath + "\"."
//						+ " Section of the path being checked was \"" + pathSection + "\"."
//						+ " Returning null."
//						);
//					return null;
//				}
//
//				String indexString = pathSection.substring(
//					openBracketAt+1, closeBracketAt
//					);
//				indexString = indexString.trim();
//
//				pathSection = pathSection.substring( 0, openBracketAt );
//				pathSection = pathSection.trim();
//
//				// Sanity check, can't have []
//				if( indexString.equals("") )
//				{
//					System.err.println( "Error: JDOMHelper:findElementByPath:"
//						+ " Found empty set of square brackets."
//						+ " Square brackets must have something between them."
//						+ " Initial path passed in was \"" + saveInitPath + "\"."
//						+ " Section of the path being checked was \"" + pathSection + "\"."
//						+ " Returning null."
//						);
//					return null;
//				}
//				// Convert the index to an integer
//				try
//				{
//					listIndex = new Integer(indexString).intValue();
//				}
//				catch( Exception e )
//				{
//					// Bail on bad integers
//					System.err.println( "Error: JDOMHelper:findElementByPath:"
//						+ " Found bad string inside a set of square brackets."
//						+ " Unable to convert the string to an integer."
//						+ " Initial path passed in was \"" + saveInitPath + "\"."
//						+ " Section of the path being checked was \"" + pathSection + "\"."
//						+ " Extracted string we were trying to convert was \"" + indexString + "\"."
//						+ " The Java exception was \"" + e + "\"."
//						+ " Returning null."
//						);
//					return null;
//				}
//
//				// Sanity check, IF there was an index specified, it
//				// must not be zero.  Later on a value of zero will
//				// then mean that none was specified.
//				if( listIndex == 0 )
//				{
//					System.err.println( "Error: JDOMHelper:findElementByPath:"
//						+ " Invalid integer inside a set of square brackets."
//						+ " The value must not be zero."
//						+ " List indicies are ONE-based, not zero based."
//						+ " So the first element is addressed with [1], not [0]."
//						+ " Negative integers can also be used to index from the end of the list."
//						+ " For example the last element is addressed with [-1]."
//						+ " Initial path passed in was \"" + saveInitPath + "\"."
//						+ " Section of the path being checked was \"" + pathSection + "\"."
//						+ " Extracted string we were trying to convert was \"" + indexString + "\"."
//						+ " Returning null."
//						);
//					return null;
//				}
//			}
//
//			if(debug) System.err.println( "Debug: JDOMHelper.path: decoded '"+
//				pathSection + "' index=" + listIndex
//				);
//
//			// Another Sanity check
//			if( pathSection.equals("") && listIndex == 0 )
//			{
//				System.err.println( "Error: JDOMHelper:findElementByPath:"
//					+ " Each section of a path must have a name or a bracketed index integer, or both."
//					+ " In this case we found both a null name and a null, empty or zero index."
//					+ " Initial path passed in was \"" + saveInitPath + "\"."
//					+ " Returning null."
//					);
//				return null;
//			}
//
//			// By now we have a path section and/or index
//
//			// If this is not the first node (or first and not rooted)
//			if( retElem != null )
//			{
//				// If no index, just use the getChild method
//				if( listIndex == 0 )
//				{
//					// Grab the element by name
//					retElem = retElem.getChild( pathSection );
//					if( retElem == null )
//					{
//						if( inDoNavigationWarnings )
//						{
//							System.err.println(
//								"Warning: JDOMHelper:findElementByPath:"
//								+ " No children found while walking path (1)."
//								+ " Looking for named children \"" + pathSection + "\"."
//								+ " Initial path passed in was \"" + saveInitPath + "\"."
//								+ " Returning null."
//								);
//						}
//						return null;
//					}
//				}
//				// Else we do have an index
//				else
//				{
//					// Else we have an index, get the entire list and
//					// do the math
//					List children = null;
//					// If no path section, get all children
//					if( pathSection.equals("") )
//						children = retElem.getChildren();
//					// Else get all named children
//					else
//						children = retElem.getChildren( pathSection );
//
//					// Sanity check on results
//					if( children == null || children.size() < 1 )
//					{
//						if( inDoNavigationWarnings )
//						{
//							System.err.println(
//								"Warning: JDOMHelper:findElementByPath:"
//								+ " No children found while walking path (2)."
//								+ " Children with \"" + pathSection + "\" name (null is OK)."
//								+ " Initial path passed in was \"" + saveInitPath + "\"."
//								+ " Returning null."
//								);
//						}
//						return null;
//					}
//
//					int childCount = children.size();
//					if( listIndex > 0 )
//					{
//						// Bounds sanity check
//						if( listIndex > childCount )
//						{
//							if( inDoNavigationWarnings )
//							{
//								System.err.println(
//									"Warning: JDOMHelper:findElementByPath:"
//									+ " Positive index out of bounds."
//									+ " Children with \"" + pathSection + "\" name (null is OK)."
//									+ " Requested index was " + listIndex + "."
//									+ " Number of matching children was " + childCount + "."
//									+ " Initial path passed in was \"" + saveInitPath + "\"."
//									+ " Returning null."
//									);
//							}
//							return null;
//						}
//
//						// grab the element, convert from one based to
//						// zero based
//						retElem = (Element)children.get( listIndex - 1 );
//					}
//					// Else we're dealing with a negative index
//					else
//					{
//						// Else it's negative, count from the end
//						// Bounds sanity check
//						if( childCount + listIndex < 0 )
//						{
//							if( inDoNavigationWarnings )
//							{
//								System.err.println(
//									"Warning: JDOMHelper:findElementByPath:"
//									+ " Negative index out of bounds."
//									+ " Children with \"" + pathSection + "\" name (null is OK)."
//									+ " Requested index was " + listIndex + "."
//									+ " Number of matching children was " + childCount + "."
//									+ " Initial path passed in was \"" + saveInitPath + "\"."
//									+ " Returning null."
//									);
//							}
//							return null;
//						}
//
//						// Grab the child from the end of the list
//						// Since the offset is negative, we ADD it to the
//						// # of children to get the index.
//						// As a coincidence, this also converts from
//						// one based to zero based for us.
//						// Example:
//						//		3 node list
//						//		index of -1, meaning the last/3rd element, offset=2
//						//		3 + -1 = 2
//						//		index of -3, meaning first element, offset=0
//						//		3 + -3 = 0
//						retElem = (Element)children.get(
//							childCount + listIndex
//							);
//					}
//
//				}		// end else we do have an index
//
//				// Final sanity check, if we were working with
//				// a valid node and now it's null then something is
//				// wrong, we don't want to accidently start over
//				// back at the root.
//				if( retElem == null )
//				{
//					if( inDoNavigationWarnings )
//					{
//						System.err.println(
//							"Warning: JDOMHelper:findElementByPath:"
//							+ " Encountered a null node while walking the path."
//							+ " Children with \"" + pathSection + "\" name (null is OK)."
//							+ " Requested index was " + listIndex + "."
//							+ " Initial path passed in was \"" + saveInitPath + "\"."
//							+ " Returning null."
//							);
//					}
//					return null;
//				}
//			}
//			// Else this is the first node AND it is rooted
//			else
//			{
//
//				// If retElem is null then this is the first time through
//				// and it's rooted
//				// We are not allowed to specify an index offset
//				// for the root
//				if( listIndex != 0  )
//				{
//					System.err.println(
//						"Error: JDOMHelper:findElementByPath:"
//						+ " Rooted paths can not have index offsets"
//						+ " in the first part of their path."
//						+ " If you're trying to get to the Nth child of the root"
//						+ " then you probably want to drop the initial slash."
//						+ " Examing path segment \"" + pathSection + "\"."
//						+ " Requested index was " + listIndex + "."
//						+ " Initial path passed in was \"" + saveInitPath + "\"."
//						+ " Returning null."
//						);
//					return null;
//				}
//
//				// Verify that the names match
//				if( sourceElem.getName().equals(pathSection) )
//					retElem = sourceElem;
//				else		// Error, should match top node
//				{
//					System.err.println(
//						"Error: JDOMHelper:findElementByPath:"
//						+ " The first section of a ROOTED path must match"
//						+ " the top level element we're starting from."
//						+ " If you're trying to get to a named CHILD of the root"
//						+ " then you should drop the leading slash."
//						+ " Reminder: XML paths ARE case sensitive."
//						+ " Expected to match segment \"" + pathSection + "\""
//						+ ", but actual root node name is \"" + sourceElem.getName() + "\"."
//						+ " Initial path passed in was \"" + saveInitPath + "\"."
//						+ " Returning null."
//						);
//					return null;
//				}
//			}
//
//		}		// End of while loop
//
//		// Return whatever's left!
//		// One final check, and this is an error since I can't figure out
//		// how this could happen
//		if( retElem == null )
//		{
//			System.err.println(
//				"Error: JDOMHelper:findElementByPath:"
//				+ " At end of method retElem is still null."
//				+ " Initial path passed in was \"" + saveInitPath + "\"."
//				+ " Returning null."
//				);
//		}
//		// return it
//		return retElem;
//	}


	// Return multiple elements from a path query
	// We do NOT allow for "/foo" queries here.
	// And we do not allow for offsets in the final part
	// of the path.
	public List findElementsByPath( String target )
	{
		return findElementsByPath( getJdomElement(), target );
	}
	public static List findElementsByPath( Element sourceElem,
		String target
		)
	{
		final String kFName = "findElementsByPath";

		List retList = new Vector();
		if( sourceElem == null || target == null )
		{
			errorMsg( kFName,
				"Was passed in at least one null argument."
				+ "sourceElem='" + sourceElem + "'"
				+ "target='" + target + "'"
				);
			return retList;
		}

		// Chop up the string into the lead and last part
		int lastSlashAt = target.lastIndexOf( '/' );

		// Sanity check, and we do NOT allow "/foo" in this method
		if( lastSlashAt == 0 || lastSlashAt == target.length()-1 )
		{
			errorMsg( kFName,
				"We don't accept rooted paths, so no /path."
				+ " target='" + target + "'"
				+ " (Todo: possibly implement later)"
				+ " Returning list with zero elements."
				);
			return retList;
		}

		// The two parts of the path
		String partA = null;
		String partB = null;

		// Do we have a leading part?
		if( lastSlashAt > 0 )
		{
			partA = target.substring( 0, lastSlashAt );
			partB = target.substring( lastSlashAt+1 );
		}
		else		// Else just the second half of the path
		{
			partA = "";
			partB = target;
		}

		// Sanity check
		if( partB.indexOf('[') >= 0 || partB.indexOf(']') >= 0 )
			return retList;
		partB = partB.trim();
		if( partB.equals("") )
			return retList;

		// Figure out what the parent is
		Element parent = null;

		// If there's any partA path, look it up
		if( ! partA.trim().equals("") )
			parent = findElementByPath( sourceElem, partA );
		else
			parent = sourceElem;

		// Sanity check
		if( parent == null )
			return retList;

		// Have jdom do the rest
		return parent.getChildren( partB );

	}


	public static String getPathToElement( Element inElem ) {
		final String kFName = "getPathToElement";

		/***
		Element elem1 = new Element( "test" );
		Element elem2 = new Element( "test" );

		statusMsg( kFName, "elem1==elem2 = " + (elem1==elem2) );
		statusMsg( kFName, "elem1.hashCode = " + elem1.hashCode() );
		statusMsg( kFName, "elem1.toString = \"" + elem1.toString() + "\"" );
		statusMsg( kFName, "elem1.isRootElement = \"" + elem1.isRootElement() + "\"" );
		statusMsg( kFName, "elem1.getParent = \"" + elem1.getParent() + "\"" );

		statusMsg( kFName, "elem2.hashCode = " + elem2.hashCode() );
		statusMsg( kFName, "elem2.toString = \"" + elem2.toString() + "\"" );
		statusMsg( kFName, "elem2.isRootElement = \"" + elem2.isRootElement() + "\"" );
		statusMsg( kFName, "elem2.getParent = \"" + elem2.getParent() + "\"" );
		***/

		if( null==inElem ) {
			errorMsg( kFName, "Null element passed in." );
			return null;
		}

		StringBuffer buff = new StringBuffer();
		List pathBits = new Vector();
		int tmpVal = traverseOneLevelGoingUp( inElem, buff, pathBits );
		if( tmpVal < 0 ) {
			errorMsg( kFName, "Got back error code " + tmpVal + ", returning null." );
			return null;
		}
		if( buff.length() < 1 || pathBits.isEmpty() ) {
			errorMsg( kFName, "Got back empty path, returning null." );
			return null;
		}

		return new String( buff );
	}

	private static int traverseOneLevelGoingUp(
		Element inCurrElem,
		StringBuffer inBuff,
		List inPathBits
	) {
		final String kFName = "traverseOneLevelGoingUp";
		if( null==inCurrElem ) {
			errorMsg( kFName, "Null element passed in." );
			return -1;
		}
		if( null==inBuff ) {
			errorMsg( kFName, "Null buffer passed in." );
			return -1;
		}
		if( null==inPathBits ) {
			errorMsg( kFName, "Null path bits list passed in." );
			return -1;
		}

		String myName = inCurrElem.getName();
		String myFullName = null;
		Element myParent = ! inCurrElem.isRootElement() ? inCurrElem.getParent() : null;
		int outValue = 0;
		boolean isRoot = false;
		// If no parent
		if( null==myParent ) {
			myFullName = myName;
			isRoot = true;
		}
		// Else has a parent
		else {
			List mySiblings = myParent.getChildren( myName );
			if( null==mySiblings || mySiblings.isEmpty() ) {
				errorMsg( kFName,
					"Null/empty children by name \"" + myName + "\""
					+ " in parent node " + myParent
					+ "; returning -1"
					);
				return -1;
			}
			int myOffset = 0;
			if( mySiblings.size() > 1 ) {
				for( int i=0; i<mySiblings.size() ; i++ ) {
					Element sibling = (Element) mySiblings.get( i );
					if( sibling == inCurrElem ) {
						myOffset = i+1;
						break;
					}
				}
				if( myOffset < 1 ) {
					errorMsg( kFName,
						"Unable to find myself by name \"" + myName + "\""
						+ " in parent node " + myParent
						+ " after looking through " + mySiblings.size() + " siblings."
						+ "; returning -1"
						);
					return -1;
				}
			}
			if( myOffset > 0 )
				myFullName = myName + '[' + myOffset + ']';
			else
				myFullName = myName;
		}

		// Now we add ourself to the buffer and list
		if( inBuff.length() > 0 )
			inBuff.insert( 0, '/' );
		inBuff.insert( 0, myFullName );
		inPathBits.add( 0, myFullName );
		// Add the leading slash if this is the root
		if( isRoot ) {
			inBuff.insert( 0, '/' );
			inPathBits.add( 0, "/" );
		}

		// Record our level of traversal
		outValue++ ;

		// If there was a parent, traverse upwards
		int tmpValue = 0;
		if( null!=myParent ) {
			tmpValue = traverseOneLevelGoingUp( myParent, inBuff, inPathBits );
			// Sanity check
			if( tmpValue < 0 ) {
				// No sense in giving multiple errors, will already have seen one
				infoMsg( kFName, "Got error code traversing parent, returning error code" );
				return tmpValue;
			}
			outValue += tmpValue;
		}

		// We're done!
		return outValue;
	}



	public static String getPathToElementNumericStr( Element inElem ) {
		final String kFName = "getPathToElementNumericStr";

		if( null==inElem ) {
			errorMsg( kFName, "Null element passed in." );
			return null;
		}

		StringBuffer buff = new StringBuffer();
		List pathBits = new Vector();
		int tmpVal = traverseOneLevelGoingUpNumeric( inElem, buff, pathBits );
		if( tmpVal < 0 ) {
			errorMsg( kFName, "Got back error code " + tmpVal + ", returning null." );
			return null;
		}
		if( buff.length() < 1 || pathBits.isEmpty() ) {
			errorMsg( kFName, "Got back empty path, returning null." );
			return null;
		}

		return new String( buff );
		// return pathBits;
	}

	public static List getPathToElementNumericList( Element inElem ) {
		final String kFName = "getPathToElementNumericList";

		if( null==inElem ) {
			errorMsg( kFName, "Null element passed in." );
			return null;
		}

		StringBuffer buff = new StringBuffer();
		List pathBits = new Vector();
		int tmpVal = traverseOneLevelGoingUpNumeric( inElem, buff, pathBits );
		if( tmpVal < 0 ) {
			errorMsg( kFName, "Got back error code " + tmpVal + ", returning null." );
			return null;
		}
		if( buff.length() < 1 || pathBits.isEmpty() ) {
			errorMsg( kFName, "Got back empty path, returning null." );
			return null;
		}

		// return new String( buff );
		return pathBits;
	}


	private static int traverseOneLevelGoingUpNumeric(
		Element inCurrElem,
		StringBuffer inBuff,
		List inPathBits
	) {
		final String kFName = "traverseOneLevelGoingUpNumeric";
		if( null==inCurrElem ) {
			errorMsg( kFName, "Null element passed in." );
			return -1;
		}
		if( null==inBuff ) {
			errorMsg( kFName, "Null buffer passed in." );
			return -1;
		}
		if( null==inPathBits ) {
			errorMsg( kFName, "Null path bits list passed in." );
			return -1;
		}

		// String myName = inCurrElem.getName();
		String myFullName = null;
		int myOffset = -1;
		Element myParent = ! inCurrElem.isRootElement() ? inCurrElem.getParent() : null;
		int outValue = 0;
		boolean isRoot = false;
		// If no parent
		if( null==myParent ) {
			// myFullName = myName;
			isRoot = true;
			myOffset = 1;
		}
		// Else has a parent
		else {
			// List mySiblings = myParent.getChildren( myName );
			List mySiblings = myParent.getChildren();
			if( null==mySiblings || mySiblings.isEmpty() ) {
				errorMsg( kFName,
					"Null/empty children"
					+ " in parent node " + myParent
					+ "; returning -1"
					);
				return -1;
			}
			for( int i=0; i<mySiblings.size() ; i++ ) {
				Element sibling = (Element) mySiblings.get( i );
				if( sibling == inCurrElem ) {
					myOffset = i+1;
					break;
				}
			}
			if( myOffset < 1 ) {
				errorMsg( kFName,
					"Unable to find myself"
					+ " in parent node " + myParent
					+ " after looking through " + mySiblings.size() + " siblings."
					+ "; returning -1"
					);
				return -1;
			}
			// myFullName = "[" + myOffset + ']';
		}
		myFullName = "[" + myOffset + ']';

		// Now we add ourself to the buffer and list
		if( inBuff.length() > 0 )
			inBuff.insert( 0, '/' );
		inBuff.insert( 0, myFullName );
		// inPathBits.add( 0, myFullName );
		inPathBits.add( 0, new Integer(myOffset) );
		// Add the leading slash if this is the root
		if( isRoot ) {
			inBuff.insert( 0, '/' );
			// inPathBits.add( 0, "/" );
		}

		// Record our level of traversal
		outValue++ ;

		// If there was a parent, traverse upwards
		int tmpValue = 0;
		if( null!=myParent ) {
			tmpValue = traverseOneLevelGoingUpNumeric( myParent, inBuff, inPathBits );
			// Sanity check
			if( tmpValue < 0 ) {
				// No sense in giving multiple errors, will already have seen one
				infoMsg( kFName, "Got error code traversing parent, returning error code" );
				return tmpValue;
			}
			outValue += tmpValue;
		}

		// We're done!
		return outValue;
	}





	// Below are some of the new style basic building block routines
	// we're using
	private void __sep__XPath_Building_Block_Routines_ () {}


	// Walk a tree looking for elements
	// By default we do NOT warn you about why/when we stopped walking
	// the tree, because it's quite common to ask for things that you know
	// may not be there.
	// However, path syntax problems are ERRORS and are reported, no matter
	// what warning is set to
	// The instance specific versions
	// Also an exists() wrapper around two of them
	public boolean exists( String target )
	{
		Element tmp = findElementByPath( getJdomElement(), target, false );
		return tmp != null;
	}
	public Element findElementByPath( String target )
	{
		return findElementByPath( getJdomElement(), target, false );
	}
	public Element findElementByPath(
		String target, boolean inDoNavigationWarnings
		)
	{
		return findElementByPath(
			getJdomElement(), target, inDoNavigationWarnings
			);
	}
	// The static versions that do all the actual work
	public static Element findElementByPath( Element findElementByPath,
		String target
		)
	{
		return findElementByPath( findElementByPath, target, false );
	}

	public static boolean exists( Element inStartElem, String inTargetPath )
	{
		Element tmp = findElementByPath( inStartElem, inTargetPath, false );
		return tmp != null;
	}

	// A revised method header redirecting to the new find Element routines
	public static Element findElementByPath( Element inStartElem,
		String inTargetPath, boolean inDoNavigationWarnings
		)
	{
		final String kFName = "findElementByPath";

		inTargetPath = NIEUtil.trimmedStringOrNull( inTargetPath );

		if( inStartElem == null || inTargetPath == null )
		{
			errorMsg( kFName,
				"Null or empty input parameter(s)."
				+ ", inStartElem=" + inStartElem
				+ ", inPath=" + inTargetPath
				);
			return null;
		}

		List choppedPath = fullPathChopper( inTargetPath );

		return findElementByPath( inStartElem,
			choppedPath, false, null,
			inDoNavigationWarnings, 0,
			false
			);
	}
	// Same as above, but OK to create path as we go
	public static Element findOrCreateElementByPath( Element inStartElem,
		String inTargetPath, boolean inDoNavigationWarnings
		)
	{
		final String kFName = "findOrCreateElementByPath";

		inTargetPath = NIEUtil.trimmedStringOrNull( inTargetPath );

		if( inStartElem == null || inTargetPath == null )
		{
			errorMsg( kFName,
				"Null or empty input parameter(s)."
				+ ", inStartElem=" + inStartElem
				+ ", inPath=" + inTargetPath
				+ " Returning null."
				);
			return null;
		}

		List choppedPath = fullPathChopper( inTargetPath );

		// Find element, and tell them it's OK to create
		return findElementByPath( inStartElem,
			choppedPath, true, null,
			inDoNavigationWarnings, 0,
			false
			);
	}

	// This version of findElement lets you give it a list of elements
	// as it's traversal path
	// You can also specify an alternate node to use if we encounter root
	// Or you can have us automatically calculate the root
	// Or, by default, we'll just use the node you passed in as root
	// By default we traverse the entire list, unless stopshort >0
	// And by default we do NOT warn about unfound elements
	private static Element findElementByPath( Element inStartElem,
		List inPathBits, boolean inCreateOK, Element inRootElem
		)
	{
		return findElementByPath( inStartElem,
			inPathBits, inCreateOK, inRootElem,
			false, 0, false
			);
	}
	private static Element findElementByPath( Element inStartElem,
		List inPathBits, boolean inCreateOK, Element inRootElem,
		boolean inDoNavigationWarnings, int inStopShortCount,
		boolean inAutoCalculateDocumentRoot
		)
	{
		// if(debug) System.err.p rintln( "Debug: JDOMHelper.path:" );
		final String kFName = "findElementByPath";

		boolean trace = shouldDoTraceMsg( kFName );

		if( inStartElem == null || inPathBits == null )
		{
			errorMsg( kFName,
				"Null arguments passed in."
				+ " inStartElem=" + inStartElem
				+ " inPathBits=" + inPathBits
				+ " Returning null."
				);
			return null;
		}

		// Are we "out of path" ?
		if( inPathBits.size() < 1 ||
				( inStopShortCount > 0 && inStopShortCount > inPathBits.size() )
			)
		{
			if( inDoNavigationWarnings )
				errorMsg( kFName,
					"Out of path."
					+ " Number of elements passed in: " + inPathBits.size()
					+ " and stop-short count: " + inStopShortCount
					+ " Returning null."
					+ " If this is normal behavior for your app then"
					+ " consider using inDoNavigationWarnings=false."
					);
			// return inStartElem;
			return null;
		}

		// Handle the special "/" case
		boolean isRooted = false;
		String firstBit = (String)inPathBits.get(0);
		Element currElem = inStartElem;
		if( firstBit.equals("/") )
		{
			if(trace) traceMsg( kFName, "Fist part is /" );
			// Note that we're rooted and update the path bits list
			isRooted = true;
			inPathBits.remove( 0 );

			// Figure out what to use for root
			// Did they specifically tell us what to use?
			// If they told us, then just use it
			if( inRootElem != null )
			{
				currElem = inRootElem;
			}
			// Else we'll figure it out for ourselves
			else
			{

				// If we've NOt been asked to auto-calculate root
				// then just use the element that was passed in as root
				// this is like how we did it before
				if( ! inAutoCalculateDocumentRoot )
				{
					currElem = inStartElem;
				}
				// Else we ARE supposed to auto-calculate,
				// based on document root
				else
				{
					// If start element is already a root, use it!
					if( inStartElem.isRootElement() )
					{
						currElem = inStartElem;
					}
					// Else find the main doc, and then take it's root element
					else
					{
						// Get the doc and check it
						Document parentDoc = inStartElem.getDocument();
						if( parentDoc != null )
						{
							Element rootElem = parentDoc.getRootElement();
							if( rootElem != null )
							{
								currElem = rootElem;
							}
							// Else the root element of the doc was null
							else
							{
								if( inDoNavigationWarnings )
									errorMsg( kFName,
										"Unable to get main doc's root element for root directive."
										+ " You may want to pass in a root element."
										+ " Will use start element as root element."
										+ " inStartElem=" + inStartElem
										+ " inPathBits=" + inPathBits
										+ " Returning null."
										+ " If this is normal behavior for your app then"
										+ " consider using inDoNavigationWarnings=false."
										);
//								currElem = inStartElem;
								return null;
							}
						}
						// Else the main doc we found was null
						else
						{
							if( inDoNavigationWarnings )
								errorMsg( kFName,
									"Unable to get main doc for root directive."
									+ " You may want to pass in a root element."
									+ " Will use start element as root element."
									+ " inStartElem=" + inStartElem
									+ " inPathBits=" + inPathBits
									+ " Returning null."
									+ " If this is normal behavior for your app then"
									+ " consider using inDoNavigationWarnings=false."
									);
//							currElem = inStartElem;
							return null;
						}
					}  // End Else find the main doc, and then take it's root element

				}   // End else we've been asked to calculate auto root

			}   // End else we were not given root will figure out where root is
		}   // End if first element is root

		// We may have cleaned out the path bits list, check again
		// And, if true, there's no need for a warning
		if( inPathBits.size() < 1 ||
				( inStopShortCount > 0 && inStopShortCount > inPathBits.size() )
			)
		{
			return currElem;
		}

		// Keep walking the path until we're done or run out of path
		for( int pathBitOffset = 0; pathBitOffset < inPathBits.size();
			pathBitOffset++
			)
		{
			// Bail if we've gone far enough
			// Do we even need to check
			if( inStopShortCount > 0 ) {
				// OK, check it
				if( pathBitOffset >= (inPathBits.size() - inStopShortCount) ) {
					break;
				}
				// Note the >= vs >, to correct for one-based counts
				// vs. zero-based offsets
			}

			// Extract the path bit
			String pathBit = (String)inPathBits.get( pathBitOffset );

			if(trace) traceMsg( kFName, "path bit =\"" + pathBit + "\"" );

			// This is probably extremely rare.
			// The first / is taken care of.
			// The path chopper should only create it at the start or end
			// And most callers will have skimmed off any ending /
			if( pathBit.equals("/") ) {
//				if( it.hasNext() )
				if( pathBitOffset < inPathBits.size()-1 ) {
					errorMsg( kFName,
						"A bare '/' can only be the first or last part of a path."
						+ " Returning null."
						);
					return null;
				}
				// Else it's OK, we're done
				break;
			}

			// Get the next element in the chain
			// If it doesn't like something it will complain and we'll
			// just get back a null
			// We won't bother to warn them if:
			// a: they told us not to
			//      or
			// b: they asked us to, but also said it was OK to create things
			//      as we go, so no warnings just because we didn't find
			//      something already existing
			// We'll need this if we try to create something
			Element lastCurrElem = currElem;
			// ******* This is it!!! ***************************
			currElem = traverseOneLevelByName( currElem, pathBit,
				isRooted, inDoNavigationWarnings && ! inCreateOK
				);

			if(trace) traceMsg( kFName, "after traverse one level, currElem=" + currElem );


			// No matter what, turn off the is rooted variable
			isRooted = false;

			// If we didn't get something, we still might be OK
			if( currElem == null ) {
				// Is it OK to try and create it?
				if( inCreateOK ) {
					currElem = createOneLevelByName( lastCurrElem, pathBit );
					// If this didn't work, then we have a real error
					if( currElem == null ) {
						errorMsg( kFName,
							"Unable to create a child node"
							+ " while traversing path in createOK mode."
							+ " Attempted child path \"" + pathBit + "\""
							+ " Returning null."
							);
						return null;
					}

					if(trace) traceMsg( kFName,
						"now after CREATE one level, currElem=" + currElem
						);


				}
				// Else not OK to create
				else
				{
					if( inDoNavigationWarnings )
					{
						errorMsg( kFName,
							"Unable to create a child node"
							+ " while traversing path in createOK mode."
							+ " Attempted child path \"" + pathBit + "\""
							+ " Returning null."
							);
					}
					return null;
				}
			}
			// Else continue going through the segments
		}   // End for each element in path

		// One final check
		// Since I don't see how this could happen, we'll call it an
		// error vs a warning
		if( currElem == null )
		{
			errorMsg( kFName,
				"Wound up with null return node at end of method."
				+ " Returning null."
				);
		}

		// Return the answer
		return currElem;

	}


	// Follow a path one level
	// We do NOT handle / or . or .. of any kind
	// ???? changing this...
	// AND we do NOT check for such nonsense
	// This is usually called by a method that's already checking that
//	private static Element findChildByPathBit( Element inStartElem,
//		String inPathBit, boolean inCreateOK
	private static Element traverseOneLevelByName( Element inStartElem,
		String inPathBit, boolean inIsRooted, boolean inDoNavigationWarnings
		)
	{
		final String kFName = "traverseOneLevelByName";

		// final boolean debug = false;

		String pathBit = NIEUtil.trimmedStringOrNull( inPathBit );
		if( inStartElem == null || pathBit == null ) {
			errorMsg( kFName,
				"NULL or empty input(s)."
				+ " Returning NULL."
				+ " start element=" + inStartElem
				+ " path=" + inPathBit
				+ " Returning null."
				);
			return null;
		}

		// Todo: support this later on
		if( pathBit.equals("..") ) {
			errorMsg( kFName,
				"Dot-dot syntax not yet supported."
				+ " Returning null."
				);
			return null;
		}

		if( pathBit.equals(".") )
			return inStartElem;

		// If we're rooted, then the current node should match
		// this element
		if( inIsRooted ) {
			String elemName = inStartElem.getName();
			// If they are the same, then we're fine, we'll stay on this element
			if( pathBit.equals(elemName) ) {
				return inStartElem;
			}
			// Else it does NOT match, this is bad
			else {
				errorMsg( kFName,
					"A root path of the form /name did not match the name of the root element."
					+ ", / name=" + pathBit
					+ ", root/current elment name = " + elemName
					+ " Depending on what you're trying to do, you may just want to drop the leading /;"
					+ " we would then look for the name in the CHILDREN of the root node."
					+ " You can also use /./name to refer to a CHILD of the ROOT."
					+ " '/' by itself returns the designated root."
					+ " Returning null."
					);
				return null;
			}
		}

		// Get the components of the path bit
		String childName = extractChildNameFromPathBit( inPathBit );
		// int rawOrdinal = extractRawOffsetFromPathBit( inPathBit );
		String rawOrdinalStr = extractRawOffsetFromPathBit( inPathBit );
		int rawOrdinal = 0;
		// Is it a plus sign
		if( null!=rawOrdinalStr && rawOrdinalStr.equals("+") ) {
			if( inDoNavigationWarnings )
				errorMsg( kFName,
					"Unable to find named child."
					+ " attempted element='" + childName + "'"
					+ " child path='" + inPathBit + "'"
					+ " Returning NULL."
					+ " If this is normal for your application"
					+ " you can set inDoNavigationWarnings=false."
					);
			return null;
		}
		// if we have a valid string, convert it to an int
		if( null!=rawOrdinalStr && ! rawOrdinalStr.equals("+") )
			rawOrdinal = NIEUtil.stringToIntOrDefaultValue(rawOrdinalStr, 0, true, true );


		// New!!!  Attribute References
		String attrName = extractAttrNameFromPathBit( inPathBit );
		String attrValue = extractAttrValueFromPathBit( inPathBit );

		// Sanity check, can't have attr value with no name
		if( attrValue != null && attrName == null )
		{
			errorMsg( kFName,
				"Path section \"" + inPathBit + "\" seems to have"
				+ " an attribute value, but no attribute name;"
				+ " this is not allowed."
				+ " Perhaps there is a misplaced equals sign?"
				+ " Returning null."
				);
			return null;
		}

		// logical Flags
		boolean isElement = childName != null || rawOrdinal != 0;
		boolean isAttr = attrName != null || attrValue != null;

		// Sanity check, can't mix regular path bits
		if( isElement && isAttr )
		{
			errorMsg( kFName,
				"Path section \"" + inPathBit + "\" seems to have"
				+ " BOTH element name / offset AND attribute information;"
				+ " this is not allowed - must be element or attribute reference."
				+ " Returning null."
				);
			return null;
		}

		// We're also not allowed to have neither be true
		if( ! isElement && ! isAttr )
		{
			errorMsg( kFName,
				"Path section \"" + inPathBit + "\" seems to have"
				+ " NO element name / offset NOR attribute information;"
				+ " this is not allowed - must be element or attribute reference."
				+ " Returning null."
				);
			return null;
		}


		// This will be our answer
		Element outElem = null;

		// Is it an Element (or ordinal) reference
		if( isElement ) {
			// Sanity check
			if( childName == null && rawOrdinal == 0 ) {
				errorMsg( kFName,
					"Input path segment has no name and no/zero offset."
					+ " Returning NULL."
					+ " path='" + inPathBit + "'"
					);
				return null;
			}

			// If no index, just use the getChild method
			if( rawOrdinal == 0 ) {
				if( shouldDoTraceMsg(kFName) ) {
					traceMsg( kFName, "Find child name: \"" + childName + "\"");

					traceMsg( kFName, "Node name: \"" + inStartElem.getName() + "\"");
					List tmpChildren = inStartElem.getChildren();
					if( tmpChildren != null ) {
						traceMsg( kFName, "" + tmpChildren.size() + " children named:" );
						for( Iterator tmpIt = tmpChildren.iterator(); tmpIt.hasNext() ; ) {
							Element tmpElem = (Element) tmpIt.next();
							String tmpName = tmpElem.getName();
							String tmpStr = tmpName.equals( childName ) ? "*" : "";
							traceMsg( kFName, "\"" + tmpName + "\"" + tmpStr );
						}
						// System.err.println();
					}
					else
						traceMsg( kFName, "NULL CHILDREN" );
				}

				outElem = inStartElem.getChild( childName );

				if( shouldDoDebugMsg(kFName) ) {
					String tmpMsg = (outElem==null) ? "NULL" : "not null" ;
					debugMsg( kFName, "outElem is " + tmpMsg );
				}

				// If we didn't find anything and we're told it's OK to
				// create, then go ahead and create it

				if( outElem == null ) {
					if( inDoNavigationWarnings ) {
						errorMsg( kFName,
							"Unable to find named child."
							+ " attempted element='" + childName + "'"
							+ " child path='" + inPathBit + "'"
							+ " Returning NULL."
							+ " If this is normal for your application"
							+ " you can set inDoNavigationWarnings=false."
							);
					}
					return null;
				}
				// Else it wasn't null, and it's already set to the correct output
			}
			// Else we have an index, get the entire list and
			// do the math
			else
			{
				List children = null;
				// Get the full list
				if( childName == null )
				{
					children = inStartElem.getChildren();
				}
				// Or just the children with that name
				else
				{
					children = inStartElem.getChildren( childName );
				}

				// Sanity check on list
				if( children == null || children.size() < 1 )
				{
					if( inDoNavigationWarnings )
					{
						errorMsg( kFName,
							"No children found while walking path name/index path."
							+ " Children with \"" + childName + "\" name (null is OK)."
							+ " Returning null."
							+ " If this is normal for your application"
							+ " you can set inDoNavigationWarnings=false."
							);
					}
					return null;
				}

				int childCount = children.size();

				// If it's a positive index
				if( rawOrdinal > 0 )
				{
					// Bounds sanity check
					if( rawOrdinal > childCount )
					{
						if( inDoNavigationWarnings )
						{
							errorMsg( kFName,
								"Positive Index out of range."
								+ " requested index='" + rawOrdinal
								+ " Number of matching children='" + childCount
								+ " Child name='" + childName + "' (null is OK)"
								+ " Returning NULL."
								+ " If this is normal for your application"
								+ " you can set inDoNavigationWarnings=false."
								);
						}
						return null;
					}
					// Else it's a valid ordinal

					// grab the element, convert from one based to
					// zero based
					outElem = (Element)children.get( rawOrdinal - 1 );

				}
				// Else it's negative, count from the end
				// Bounds sanity check
				else
				{
					// Check that it's not too large in absolute value terms
					if( childCount + rawOrdinal < 0 )
					{
						if( inDoNavigationWarnings )
						{
							errorMsg( kFName,
								"Negative Index out of range."
								+ " requested index='" + rawOrdinal
								+ " Number of matching children='" + childCount
								+ " Max negative offset must not be greater than number children (abs)."
								+ " Most negative allowable offset would be"
								+ ( -1 * childCount )
								+ " Child name='" + childName + "' (null is OK)"
								+ " Returning NULL."
								+ " If this is normal for your application"
								+ " you can set inDoNavigationWarnings=false."
								);
						}
						return null;
					}

					// Grab the child from the end of the list
					// Since the offset is negative, we ADD it to the
					// # of children to get the index.
					// As a coincidence, this also converts from
					// one based to zero based for us.
					// Example:
					//		3 node list
					//		index of -1, meaning the last/3rd element, offset=2
					//		3 + -1 = 2
					//		index of -3, meaning first element, offset=0
					//		3 + -3 = 0
					outElem = (Element)children.get(
						childCount + rawOrdinal
						);
				}   // End else it's a negative index/offset

			}		// end else we do have an index

		}   // End if it's an Element / ordinal reference
		// Else is it an attribute reference?
		else if( isAttr )
		{
			// Get the full attribute, by name
			// Attribute attrObj = inStartElem.getAttribute( attrName );
			String thisAttrValue = inStartElem.getAttributeValue( attrName );

			// If we didn't find it, we're done
			// if( attrObj == null )
			if( thisAttrValue == null )
			{
				if( inDoNavigationWarnings )
					warningMsg( kFName,
						"Did not find the named attribute \"" + attrName + "\"."
						+ " Returning null."
						);
				return null;
			}

			// If there's a value to check, check it
			if( attrValue != null )
			{
				// If it's not equal, we have a problem
				// if( ! attrObj.equals(attrValue) )
				if( ! thisAttrValue.equals(attrValue) )
				{
					if( inDoNavigationWarnings )
						warningMsg( kFName,
							"Attribute value did not match."
							+ " Attribute name = \"" + attrName + "\""
							+ ", desired value = \"" + attrValue + "\""
							// + ", actual value = \"" + attrObj.getValue() + "\""
							+ ", actual value = \"" + thisAttrValue + "\""
							+ " Returning null."
							);
					// regardless of warnings, we're done, return null
					return null;
				}
			}

			// Else at this point we're good, we have attr, and if
			// a target value was set, it also matches
			// Of course, the element stays the same!
			outElem = inStartElem;

		}   // End else if it's an attribute reference
		// Else we don't know what it is, should never get to this point
		else
		{
			errorMsg( kFName,
				"Niether element nor attribute reference."
				+ " Returning null."
				);
			return null;
		}

		// All set, return the answer!
		return outElem;
	}

	private static Element createOneLevelByName( Element inStartElem,
		String inPathBit
		)
	{
		final String kFName = "createOneLevelByName";

		if( inStartElem == null || inPathBit == null )
		{
			errorMsg( kFName,
				"NULL input(s)."
				+ " start element=" + inStartElem
				+ " path=" + inPathBit
				+ " Returning NULL."
				);
			return null;
		}

		// Get the components of the path bit
		String childName = extractChildNameFromPathBit( inPathBit );
		// int rawOrdinal = extractRawOffsetFromPathBit( inPathBit );
		String rawOrdinalStr = extractRawOffsetFromPathBit( inPathBit );
		int rawOrdinal = 0;
		if( null!=rawOrdinalStr && ! rawOrdinalStr.equals("+") )
			rawOrdinal = NIEUtil.stringToIntOrDefaultValue(rawOrdinalStr, 0, true, true );



		// New!!!  Attribute References
		String attrName = extractAttrNameFromPathBit( inPathBit );
		String attrValue = extractAttrValueFromPathBit( inPathBit );

		// Sanity check, can't have attr value with no name
		if( attrValue != null && attrName == null )
		{
			errorMsg( kFName,
				"Path section \"" + inPathBit + "\" seems to have"
				+ " an attribute value, but no attribute name;"
				+ " this is not allowed."
				+ " Perhaps there is a misplaced equals sign?"
				+ " Returning null."
				);
			return null;
		}

		// logical Flags
		boolean isElement = childName != null
			|| rawOrdinal != 0
			|| ( null!=rawOrdinalStr && rawOrdinalStr.equals("+") )
			;
		boolean isAttr = attrName != null || attrValue != null;

		// Sanity check, can't mix regular path bits
		if( isElement && isAttr )
		{
			errorMsg( kFName,
				"Path section \"" + inPathBit + "\" seems to have"
				+ " BOTH element name / offset AND attribute information;"
				+ " this is not allowed - must be element or attribute reference."
				+ " Orig path bit = \"" + inPathBit + "\""
				+ ", childName=\"" + childName + "\""
				+ ", rawOrdinal=\"" + rawOrdinal + "\""
				+ ", attrName=\"" + attrName + "\""
				+ ", attrValue=\"" + attrValue + "\"."
				+ " Returning null."
				);
			return null;
		}

		// We're also not allowed to have neither be true
		if( ! isElement && ! isAttr )
		{
			errorMsg( kFName,
				"Path section \"" + inPathBit + "\" seems to have"
				+ " NO element name / offset NOR attribute information;"
				+ " this is not allowed - must be element or attribute reference."
				+ " Returning null."
				);
			return null;
		}

		// This will be our answer
		Element outElem = null;



		// If it's an Element
		if( isElement ) {
			// Sanity check, must have a name
			// Todo: revisit some type of auto-naming, maybe based on existing
			// nodes, or let them pass in something, or have a system default
			if( childName == null ) {
				errorMsg( kFName,
					"Can't create a child node with no name?"
					+ " path='" + inPathBit + "'"
					+ " Returning NULL."
					);
				return null;
			}

			// Figure out how many to create
			// Start by assuming none
			int numToCreate = 0;

			// If we're dealing with regular offsets
			if( null==rawOrdinalStr || ! rawOrdinalStr.equals("+") ) {

				// Find out how many siblings we currently have
				// by this name
				List tmpSiblings = inStartElem.getChildren( childName );
				int currSiblingCount = tmpSiblings.size();

				// Sanity check, must NOT have an index offset
				// TODO: revisit the logic of allowing them to specifiy an offset
				if( rawOrdinal < -1 ) {
					errorMsg( kFName,
						"Can't create a child Element with a negative index, except if = -1."
						+ " path='" + inPathBit + "'"
						+ " Returning NULL."
						);
					return null;
				}

				// Normally we just create one
				numToCreate = 1;

				// If the index is > 0 then we need to do some checking
				if( rawOrdinal > 0 )
				{
					// If the ordinal is within the range of EXISTING
					// siblings, then we can't create it, this must be
					// a mistake
					if( rawOrdinal <= currSiblingCount )
					{
						errorMsg( kFName,
							"Asked to CREATE a specific numbered element"
							+ " whose slot is already taken."
							+ " Original path spec = \"" + inPathBit + "\"."
							+ " Requested element name = \"" + childName + "\""
							+ ", requested ordinal slot = " + rawOrdinal
							+ ", number of existing siblings with this name = "
							+ currSiblingCount
							+ " Returning null."
							);
						return null;
					}

					// Now update the correct count
					numToCreate = rawOrdinal - currSiblingCount;
				}

				// If creating more than 1, that's a bit unusual
				if( numToCreate > 1 ) {
					infoMsg( kFName,
						"Asked to CREATE a SPECIFIC numbered element"
						+ " but will need to PAD the list in ordre to do it."
						+ " Original path spec = \"" + inPathBit + "\"."
						+ " Requested element name = \"" + childName + "\""
						+ ", requested ordinal slot = " + rawOrdinal
						+ ", number of existing siblings with this name = "
						+ currSiblingCount
						+ " Will create a total of " + numToCreate
						+ " elements (1 would be the usual)."
						+ " This may be perfectly normal for your application."
						);
				}
			}
			// Else it was a plus sign, so just create it
			else {
				numToCreate = 1;
			}

			// Loop to create the desired number
			for( int i = 0; i < numToCreate; i++ ) {
				// Create the element
				Element tmpElem = new Element( childName );
				// If we were able to create it, go ahead and attach it
				if( tmpElem != null ) {
					tmpElem.detach();
					inStartElem.addContent( tmpElem );
					// We keep setting it to the last one we created
					outElem = tmpElem;
				}
				else {
					errorMsg( kFName,
						"Unable to create requested named child node."
						+ " attempted element='" + childName + "'"
						+ " Returning NULL."
						);
					return null;
				}

			}	// End for loop to create the desired number

		}   // End if it's an element
		// Else if is attribute
		else if( isAttr )
		{
			if( attrValue == null )
				attrValue = DEFAULT_NEW_ATTR_VALUE;
			// Create tte attribute
			inStartElem.setAttribute( attrName, attrValue );

			// Return the answer
			outElem = inStartElem;
		}
		// Else we're confused
		else
		{
			errorMsg( kFName,
				"Niether element nor attribute.  Returning null."
				);
			return null;
		}

		// Return our answer
		return outElem;
	}



	// Will return NULL if empty string
	// Given a pathlette like foo[5], give back foo
	private static String extractChildNameFromPathBit( String inPathBit )
	{
		final String kFName = "extractChildNameFromPathBit";

		String lPathBit = NIEUtil.trimmedStringOrNull( inPathBit );
		if( lPathBit == null )
		{
			errorMsg( kFName,
				"NULL or empty path passed in, returning null."
				+ " Returning null."
				);
			return null;
		}
		String outPathBit = lPathBit;
		int leftBracketAt = outPathBit.indexOf( '[' );
		int atSignAt = outPathBit.indexOf( '@' );
//		int delimAt = atSignAt >= 0 && atSignAt < leftBracketAt
//			? atSignAt : leftBracketAt;

		int delimAt = -1;
		// If we found both, take the LOWER of the two
		if( leftBracketAt >= 0 && atSignAt >= 0 )
		{
			if( leftBracketAt < atSignAt )
				delimAt = leftBracketAt;
			else
				delimAt = atSignAt;
		}
		// If we only found one or the other, use that
		else if( leftBracketAt >= 0 )
		{
			delimAt = leftBracketAt;
		}
		else if( atSignAt >= 0 )
		{
			delimAt = atSignAt;
		}
		// Else leave it at -1

		// If we have a reasonable delimiter, extract it
		if( delimAt > 0 )
		{
			outPathBit = outPathBit.substring( 0, delimAt );
			// May reduce to null, which is odd
			outPathBit = NIEUtil.trimmedStringOrNull( outPathBit );
		}
		// This is perfectly normal
		else if( delimAt == 0 )
		{
			outPathBit = null;
		}
		// Else leave it alone, leave it set to the entire
		// string, from above

		return outPathBit;
	}
	// Given a pathlette like foo[5], give back 5
	// Returns 0 if it finds none
	// private static int extractRawOffsetFromPathBit( String inPathBit )
	private static String extractRawOffsetFromPathBit( String inPathBit )
	{
		final String kFName = "extractRawOffsetFromPathBit";

		inPathBit = NIEUtil.trimmedStringOrNull( inPathBit );
		if( inPathBit == null )
		{
			errorMsg( kFName,
				"NULL path passed in, returning null."
				);
			// return 0;
			return null;
		}
		// Look for offset stuff
		// Offsets are ONE based, not zero based, to comply with xpath
		// A return of 0 often means we just didn't find anything, which is OK!
		int outIndex = 0;
		int openBracketAt = inPathBit.indexOf( '[' );
		String outIindexString = null;

		// If we found an opening bracket
		if( openBracketAt >= 0 )
		{
			// Look for ending bracket
			int closeBracketAt = inPathBit.indexOf( ']',
				openBracketAt
				);
			// Open bracket with no close is bad
			if( closeBracketAt < 0 )
			{
				errorMsg( kFName,
					"Unbalanced square brackets, returning zero."
					+ " string='" + inPathBit + "'"
					);
				// return 0;
				return null;
			}

			// If there's no characters between the
			// brackets that's bad as well
			if( closeBracketAt == openBracketAt-1 )
			{
				errorMsg( kFName,
					"Nothing in between square brackets, returning zero."
					+ " string='" + inPathBit + "'"
					);
				return null;
			}

			outIindexString = inPathBit.substring(
				openBracketAt+1, closeBracketAt
				);
			outIindexString = outIindexString.trim();

			// Sanity check, can't have []
			if( outIindexString.equals("") )
			{
				errorMsg( kFName,
					"Only whitespace in between square brackets, returning zero."
					+ " string='" + inPathBit + "'"
					);
				return null;
			}

			// A stand alone + means ADD
			if( outIindexString.equals("+") )
				return outIindexString;

			// Convert the index to an integer
			try
			{
				outIndex = Integer.parseInt( outIindexString );
			}
			catch( Exception e )
			{
				// Bail on bad integers
				errorMsg( kFName,
					"Error parsing integer in square brackets, returning zero."
					+ " int string='" + outIindexString + "'"
					+ " path segment it was in='" + inPathBit + "'"
					);
				return null;
			}

		}   // End if there was a left bracket

		// return outIndex;
		return outIindexString;
	}


	private static String extractAttrNameFromPathBit( String inPathBit )
	{
		final String kFName = "extractAttrNameFromPathBit";
		if( inPathBit == null )
		{
			errorMsg( kFName, "In path bit was null, returning null" );
			return null;
		}
		// Look for @ sign
		int atSignAt = inPathBit.indexOf( '@' );
		if( atSignAt < 0 )
			return null;
		// Extract the attribution portion, if any
		String attrName = null;
		// If it's not at the end, grab the rest
		if( atSignAt < inPathBit.length() - 1 )
			attrName = inPathBit.substring( atSignAt+1 );
		// Sanity check
		if( attrName == null )
		{
			errorMsg( kFName,
				"A null/empty attribute name was found"
				+ " in path section \"" + inPathBit + "\" (1)."
				+ " Returning null."
				);
			return null;
		}

		int equalsAt = attrName.indexOf( '=' );
		if( equalsAt >= 0 )
		{
			if( equalsAt > 0 )
				attrName = attrName.substring( 0, equalsAt );
			// If the = was right at the start, then we have an empty attr name
			else
			{
				errorMsg( kFName,
					"A null/empty attribute name was found"
					+ " in path section \"" + inPathBit + "\" (2)."
					+ " Returning null."
					);
				return null;
			}
		}
		attrName = NIEUtil.trimmedStringOrNull( attrName );
		// One final sanity check
		if( attrName == null )
		{
			errorMsg( kFName,
				"A null/empty attribute name was found"
				+ " in path section \"" + inPathBit + "\" (3)."
				+ " Returning null."
				);
		}
		return attrName;
	}

	// To be safe, you should also check for the attr value
	private static String extractAttrValueFromPathBit( String inPathBit )
	{
		final String kFName = "extractAttrValueFromPathBit";
		if( inPathBit == null )
		{
			errorMsg( kFName, "In path bit was null, returning null" );
			return null;
		}
		int equalsAt = inPathBit.indexOf( '=' );
		if( equalsAt < 0 )
			return null;
		String attrValue = null;
		if( equalsAt < inPathBit.length() - 1 )
		{
			attrValue = inPathBit.substring( equalsAt+1 );
		}
		// Euqals sign is right at the end of the string
		else
		{
			errorMsg( kFName,
				"A null/empty attribute value was found"
				+ " in path section \"" + inPathBit + "\" (1)."
				+ " Returning null."
				);
			return null;
		}

		// Final normalize
		attrValue = NIEUtil.trimmedStringOrNull( attrValue );

		if( attrValue == null )
			errorMsg( kFName,
				"A null/empty attribute value was found"
				+ " in path section \"" + inPathBit + "\" (2)."
				+ " Returning null."
				);
		return attrValue;
	}


	// This will always return a list
	// nulls/empty are OK
	public static List fullPathChopper( String inPath )
	{

		final String kFName = "fullPathChopper";

		List outList = new Vector();
		if( inPath == null || inPath.trim().equals("") )
		{
			errorMsg( kFName,
					"Was passed in a NULL path."
					+ " Returning a list with zero elements."
					);
			return outList;
		}

		inPath = inPath.trim();

		// Whine about // query directive, unsupported
		if( inPath.length()>=2 && inPath.startsWith( "//" ) )
		{
			errorMsg( kFName,
				"Path starts with '//' query directive"
				+ " which is not supported at this time."
				+ " path='" + inPath + "'"
				);
			return outList;
		}

		// Handle "root" directive
		if( inPath.startsWith( "/" ) )
		{
			outList.add( "/" );
			if( inPath.length() > 1 )
				inPath = inPath.substring( 1 );
			else
				inPath = "";
		}

		if( inPath.equals("") )
			return outList;

		StringTokenizer st = new StringTokenizer( inPath, "/" );
		while( st.hasMoreTokens() )
		{
			String item = st.nextToken();
			item = item.trim();
			// If we got a valid sub path, add it!
			if( ! item.equals("") )
				outList.add( item );
			// Else it was null
			else
			{
				// Paths are allowed to have a trailing slash
				// If so, add it
				if( ! st.hasMoreTokens() )
					outList.add( "/" );
				// Otherwise this is clearly invalid
				else
				{
					errorMsg( kFName,
						"Path has null subpath."
						+ " path='" + inPath + "'"
						+ " Returning list with zero elements."
						);
					// Null out the list
					outList = new Vector();
					break;
				}
			}
		}

		return outList;
	}

	// See if we have a normal looking, plane old path element
	// So it's not null and does NOT have [], / @ or spaces in it
	public static boolean getIsPathBitAPlainElementName( String pathBit )
	{
		if( pathBit == null )
			return false;
		pathBit = pathBit.trim();
		if( pathBit.equals("") )
			return false;
		if(
				pathBit.indexOf('[') >= 0
				|| pathBit.indexOf(']') >= 0
				|| pathBit.indexOf('/') >= 0
				|| pathBit.indexOf('@') >= 0
				|| pathBit.indexOf('.') >= 0
				|| pathBit.indexOf(' ') >= 0
				|| pathBit.indexOf('\t') >= 0
				|| pathBit.indexOf(',') >= 0
			)
		{
			return false;
		}

		return true;
	}



//	some false starts at path navigation, will rework the older routines above
//	instead
//	public static Element newFindElement( Element inSourceElem, String inPath,
//		boolean inDoCreate, boolean inDoNavigationWarnings,
//		int inStopShortCount
//		)
//	{
//
//		List choppedPath = fullPathChopper( inPath );
//		int pathLen = choppedPath.size();
//
//		if( pathLen < 1 )
//		{
//			System.err.p rintln( "Error: JDOMHelper:findElement:"
//				+ " Path too short; must have at least one element."
//				+ ", inPath=" + inPath
//				+ " Returning null."
//				);
//			return null;
//		}
//	}
//	private static Element newTraverseOneLevel()
//	{
//	}
//	private static Element newCreateOneLevel()
//	{
//	}





	// routines setting and getting data
	private void __sep__XPath_Data_Get_And_Set_ () {}



	// Given a path, return text
	// We will return a NULL if we don't find the element
	// you asked for.
	// We do not trim it for you
	// WARNING: Does NOT HANDLE attribute oriented paths
	public String getTextByPath( String path )
	{
		return getTextByPath( getJdomElement(), path );
	}
	public static String getTextByPath( Element startElem,
		String path
		)
	{
		Element tmpElem = findElementByPath( startElem, path );
		if( tmpElem == null )
			return null;
		return tmpElem.getText();
	}
	public boolean setTextByPath( String path, String newText )
	{
		return setTextByPath( getJdomElement(), path, newText );
	}
	// WARNING: Does NOT HANDLE attribute oriented paths
	public static boolean setTextByPath( Element startElem,
		String path, String newText
		)
	{
		Element tmpElem = findElementByPath( startElem, path );
		if( tmpElem == null )
			return false;
		tmpElem.setText( newText );
		return true;
	}

	public String getTextByPathTrimOrNull( String path )
	{
		return getTextByPathTrimOrNull( getJdomElement(), path );
	}
	public static String getTextByPathTrimOrNull( Element startElem,
		String path
		)
	{
	    final String kFName = "getTextByPathTrimOrNull";
	    if(null!=path && path.indexOf('@') >= 0) {
	        errorMsg( kFName,
	                "Path includes at sign (@), this method does not support attributes."
	                + " Try using getTextFromSinglePathAttrTrimOrNull() instead."
	                + " path=\""+path+'"');
	        return null;
	    }
		Element tmpElem = findElementByPath( startElem, path );
		if( tmpElem == null )
			return null;
		String tmpStr = tmpElem.getText();
		tmpStr = NIEUtil.trimmedStringOrNull( tmpStr );
		return tmpStr;
	}
	public String getTextByPathTrimOrDefault( String path, String defaultVal )
	{
		String outStr = getTextByPathTrimOrNull( getJdomElement(), path );
		return null!=outStr ? outStr : defaultVal;
	}

	public List getListFromCsvTextByPathTrim( String path )
	{
		return getListFromCsvTextByPathTrim( getJdomElement(), path );
	}
	public List getListFromCsvTextByPathTrim(
			String path, boolean inIgnoreEmpties
		)
	{
		return getListFromCsvTextByPathTrim( getJdomElement(), path,
				inIgnoreEmpties
				);
	}
	public static List getListFromCsvTextByPathTrim( Element startElem,
			String path
		)
	{
		return getListFromCsvTextByPathTrim( startElem, path, true );
	}
	public static List getListFromCsvTextByPathTrim( Element startElem,
			String path, boolean inIgnoreEmpties
		)
	{
		final String kFName = "getListFromCsvTextByPathTrim";
		String data = getTextByPathTrimOrNull( startElem, path );
		List answer = new Vector();
		if( null==data ) {
			return answer;
		}
		List tmpAnswer = NIEUtil.parseCSVLine( data );
		for( Iterator it = tmpAnswer.iterator(); it.hasNext(); ) {
			String val = (String) it.next();
			val = val.trim();
			if( val.length()>0 || ! inIgnoreEmpties ) {
				answer.add( val );
			}
		}
		return answer;
	}

	// These also call Element's "normalize" to clean up
	// bounded white space within the string, changing that to a single space
	public String getTextByPathSuperTrimOrNull( String path )
	{
		return getTextByPathSuperTrimOrNull( getJdomElement(), path );
	}
	public static String getTextByPathSuperTrimOrNull( Element startElem,
		String path
		)
	{
		Element tmpElem = findElementByPath( startElem, path );
		if( tmpElem == null )
			return null;
		String tmpStr = tmpElem.getTextNormalize();
		tmpStr = NIEUtil.trimmedStringOrNull( tmpStr );
		return tmpStr;
	}




	// Convenience function
	// Find all the elements that match a path, grab the text of
	// those elements and shove it into a vector
	// WILL ALWAYES RETURN A LIST, but maybe with 0 strings
	// If we get back a NULL element we will not include that element
	// However, if the element's getText returns null we WILL put in a ""
	// string, so all filled slots will have strings from nodes that were
	// actually found.
	// We do NOT trim the strings, so if you care, you should do it
	// or use the other method
	public List getTextListByPath( String inPath )
	{
		return getTextListByPath( getJdomElement(), inPath );
	}
	public static List getTextListByPath(
		Element inStartingElem, String inPath
		)
	{
		// Prime the return value
		List retList = new Vector();

		// Find the matching children
		List elements = findElementsByPath( inStartingElem, inPath );
		// Sanity check, should never happen
		if( elements == null )
			return retList;

		// Loop through all the matches
		Iterator it = elements.iterator();
		// For each field in the record
		while( it.hasNext() )
		{
			// pull the next element
			Element tmpElem = (Element)it.next();
			if( tmpElem == null )
				continue;       // This will likely never happen
			// String tmpContent = tmpElem.getText();
			String tmpContent = tmpElem.getTextTrim();
			if( tmpContent == null )
				tmpContent = "";       // This should never happen
			retList.add( tmpContent );
		}

		return retList;
	}

	// Similar to above, but will DO trim and do NOT include null or empty
	// items
	public List getTextListByPathNotNullTrim( String inPath )
	{
		return getTextListByPathNotNullTrim( getJdomElement(), inPath );
	}
	// ALWAYS RETURN A LIST, even if zero elements
	public static List getTextListByPathNotNullTrim(
		Element inStartingElem, String inPath
		)
	{
		// Prime the return value
		List retList = new Vector();

		// Find the matching children
		List elements = findElementsByPath( inStartingElem, inPath );
		// Sanity check, should never happen
		if( elements == null )
			return retList;

		// Loop through all the matches
		Iterator it = elements.iterator();
		// For each field in the record
		while( it.hasNext() )
		{
			// pull the next element
			Element tmpElem = (Element)it.next();
			if( tmpElem == null )
				continue;       // This will likely never happen
			String tmpContent = tmpElem.getText();
			if( tmpContent == null )
				continue;       // This should never happen
			// Trim and skip if null
			tmpContent = tmpContent.trim();
			if( tmpContent.equals("") )
				continue;
			retList.add( tmpContent );
		}

		return retList;
	}

	// Similar to above, but we also normalize internal spaces
	public List getTextListByPathNotNullSuperTrim( String inPath )
	{
		return getTextListByPathNotNullSuperTrim( getJdomElement(), inPath );
	}
	// ALWAYS RETURN A LIST, even if zero elements
	public static List getTextListByPathNotNullSuperTrim(
		Element inStartingElem, String inPath
		)
	{
		// Prime the return value
		List retList = new Vector();

		// Find the matching children
		List elements = findElementsByPath( inStartingElem, inPath );
		// Sanity check, should never happen
		if( elements == null )
			return retList;

		// Loop through all the matches
		Iterator it = elements.iterator();
		// For each field in the record
		while( it.hasNext() )
		{
			// pull the next element
			Element tmpElem = (Element)it.next();
			if( tmpElem == null )
				continue;       // This will likely never happen
			String tmpContent = tmpElem.getTextNormalize();
			if( tmpContent == null )
				continue;       // This should never happen
			// Trim and skip if null
			tmpContent = tmpContent.trim();
			if( tmpContent.equals("") )
				continue;
			retList.add( tmpContent );
		}

		return retList;
	}

	// Similar to above but will return text of an attribute AND
	// of matching elements
	public List getTextListByPathAndSingleAttrNotNullTrim( String inPath )
	{
		return getTextListByPathAndSingleAttrNotNullTrim( getJdomElement(), inPath );
	}
	// ALWAYS RETURN A LIST, even if zero elements
	public static List getTextListByPathAndSingleAttrNotNullTrim(
		Element inStartingElem, String inPath
		)
	{
		// Get the list of matching elements
		List outList = getTextListByPathNotNullTrim( inStartingElem, inPath );
		// And also any attributes
		String lAttrValue = getStringFromAttributeTrimOrNull(
			inStartingElem, inPath
			);
		// If there was an attribute
		if( lAttrValue != null )
			outList.add( 0, lAttrValue );

		// Return the results
		return outList;
	}



	// Complex Query
	// Variant 1: Given a starting path, an element name,
	// an attribute name and value, and a direction to search,
	// find the matching node
	// If the starting path is null that's OK, we will scan
	// from the initial element
	// If the elementName is null that's OK, we will scan
	// all the children at that point.
	// Todo: make element value be optional
	// Todo: options for trimming and normalizing attr values
	public Element mixedQuery( String startPath,
		String optionalElementName, String attributeName,
		String attributeValue, boolean searchBackwardsFlag
		)
	{
		return mixedQuery( getJdomElement(), startPath,
			optionalElementName, attributeName, attributeValue,
			searchBackwardsFlag
			);
	}

	public Element mixedQuery( String startPath,
		String optionalElementName, String attributeName,
		String attributeValue
		)
	{
		return mixedQuery( getJdomElement(), startPath,
			optionalElementName, attributeName, attributeValue,
			false
			);
	}

	public static Element mixedQuery( Element sourceElem,
		String startPath, String optionalElementName,
		String attributeName, String attributeValue
		)
	{
		return mixedQuery( sourceElem, startPath,
			optionalElementName, attributeName, attributeValue,
			false
			);
	}

	public static Element mixedQuery( Element sourceElem, String startPath,
		String optionalElementName, String attributeName, String attributeValue,
		boolean searchBackwardsFlag
		)
	{

		// First find the parent
		Element parent = null;
		if( startPath != null )
		{
			parent = findElementByPath( sourceElem, startPath );
			if( parent == null )
				return null;
		}
		else
			parent = sourceElem;

		// Now get the matching children
		List children = null;

		if( optionalElementName != null )
		{
			// Get named children
			children = parent.getChildren( optionalElementName );
		}
		else
		{
			// Get all children
			children = parent.getChildren();
		}

		// The eventual answer
		Element returnElem = null;

		// If we're searching forward
		if( ! searchBackwardsFlag )
		{
			for( int i=0; i<children.size(); i++ )
			{
				Element tmpElem = (Element)children.get(i);
				String tmpAttrValue = tmpElem.getAttributeValue( attributeName );
				if( tmpAttrValue == null )
					continue;

				if( tmpAttrValue.equals(attributeValue) )
				{
					returnElem = tmpElem;
					break;
				}
			}
		}
		else		// Yes we are searching backwards
		{
			for( int i = children.size()-1; i >= 0; i-- )
			{
				Element tmpElem = (Element)children.get(i);
				String tmpAttrValue = tmpElem.getAttributeValue( attributeName );
				if( tmpAttrValue == null )
					continue;
				if( tmpAttrValue.equals(attributeValue) )
				{
					returnElem = tmpElem;
					break;
				}
			}
		}

		// We've either found it we haven't
		return returnElem;

	}



	// Complex Query
	// Variant 2: Given a starting path, an element name,
	// an attribute name and value, and a direction to search,
	// find ALL matching Elements.
	// If the elementName is null that's OK, we will scan
	// all the children at that point.
	// Todo: make element value be optional
	// Todo: options for trimming and normalizing attr values
	public List mixedListQuery( String startPath,
		String optionalElementName, String attributeName, String attributeValue,
		boolean searchBackwardsFlag
		)
	{
		return mixedListQuery( getJdomElement(), startPath,
			optionalElementName, attributeName, attributeValue,
			searchBackwardsFlag
			);
	}

	public static List mixedListQuery( Element sourceElem, String startPath,
		String optionalElementName, String attributeName, String attributeValue,
		boolean searchBackwardsFlag
		)
	{
		List retList = new Vector();

		// First find the parent
		Element parent = findElementByPath( sourceElem, startPath );
		if( parent == null )
			return retList;

		// Now get the matching children
		List children = null;

		if( optionalElementName != null )
			// Get named children
			children = parent.getChildren( optionalElementName );
		else
			// Get all children
			children = parent.getChildren();

		// If we're searching forward
		if( ! searchBackwardsFlag )
		{
			for( int i=0; i<children.size(); i++ )
			{
				Element tmpElem = (Element)children.get(i);
				String tmpAttrValue = tmpElem.getAttributeValue( attributeName );
				if( tmpAttrValue == null )
					continue;
				if( tmpAttrValue.equals(attributeValue) )
					retList.add( tmpElem );
			}
		}
		else		// Yes we are searching backwards
		{
			for( int i = children.size()-1; i >= 0; i-- )
			{
				Element tmpElem = (Element)children.get(i);
				String tmpAttrValue = tmpElem.getAttributeValue( attributeName );
				if( tmpAttrValue == null )
					continue;

				if( tmpAttrValue.equals(attributeValue) )
					retList.add( tmpElem );
			}
		}

		// we always return a valid list, maybe with 0 elements
		return retList;

	}

	public boolean existsPathAttr( String inPath, String inAttrName )
	{
		return existsPathAttr( getJdomElement(), inPath, inAttrName, true );
	}
	public boolean existsPathAttr( String inPath, String inAttrName,
		boolean inDoNullArgWarnings
		)
	{
		return existsPathAttr( getJdomElement(), inPath, inAttrName,
			inDoNullArgWarnings
			);
	}
	public static boolean existsPathAttr( Element inStartElem,
		String inPath, String inAttrName
		)
	{
		return existsPathAttr( inStartElem, inPath, inAttrName, true );
	}
	public static boolean existsPathAttr( Element inStartElem, String inPath,
		String inAttrName, boolean inDoNullArgWarnings
		)
	{
		final String kFName = "existsPathAttr";
		inPath = NIEUtil.trimmedStringOrNull( inPath );
		inAttrName = NIEUtil.trimmedStringOrNull( inAttrName );
		if( inStartElem == null || inPath == null || inAttrName == null )
		{
			if( inDoNullArgWarnings )
				errorMsg( kFName, "One or more null/empty inputs: "
					+ "inStartElem=" + inStartElem
					+ ", inPath=" + inPath
					+ ", inAttrName=" + inAttrName
					);
			return false;
		}

		// First find the parent
		Element theElem = findElementByPath( inStartElem, inPath );
		if( theElem == null )
			return false;

		// See if we have the attribute
		Attribute theAttr = theElem.getAttribute( inAttrName );
		if( theAttr == null )
			return false;
		else
			return true;
	}

	// Similar to getAttributeValue and getString functions, will
	// return the text of a named attribute given a path and
	// an attribute name.
	// Returns NULL if there is no such attribute
	public String getTextFromSinglePathAttr( String inPath, String inAttrName )
	{
		return getTextFromSinglePathAttr( getJdomElement(), inPath, inAttrName );
	}
	public static String getTextFromSinglePathAttr( Element inStartElem, String inPath,
		String inAttrName
		)
	{
		if( inStartElem == null || inAttrName == null ||
			inAttrName.trim().equals("")
			)
			return null;
			// Todo: complain loudly?

		// First find the parent
		Element theElem = findElementByPath( inStartElem, inPath );
		if( theElem == null )
			return null;

		// Have jdom do the rest
		return theElem.getAttributeValue( inAttrName );
	}

	public String getTextFromSinglePathAttrTrimOrNull(
		String inPath, String inAttrName
		)
	{
		return getTextFromSinglePathAttrTrimOrNull(
			getJdomElement(), inPath, inAttrName
			);
	}
	public static String getTextFromSinglePathAttrTrimOrNull(
		Element inStartElem, String inPath,
		String inAttrName
		)
	{
		if( inStartElem == null || inAttrName == null ||
			inAttrName.trim().equals("")
			)
			return null;
			// Todo: complain loudly?
		// First find the parent
		Element theElem = findElementByPath( inStartElem, inPath );
		// Element theElem = findElementByPath( inStartElem, inPath, true );
		if( theElem == null )
			return null;

		// Use jdom
		String tmpStr = theElem.getAttributeValue( inAttrName );
		// Then return using our string normalizer
		return NIEUtil.trimmedStringOrNull( tmpStr );
	}


	// Set the text of an attribute
	// By default we will create it
	// Todo: revisit default creation vs. warning flags
	// 4 entry points, static vs instance x simple vs extra parameters
	public Element setTextOfSinglePathAttr(
		String inPath, String inAttrName, String inAttrValue
		)
	{
		return setTextOfSinglePathAttr(
			getJdomElement(), inPath,
			inAttrName, inAttrValue
			);
	}
	public static Element setTextOfSinglePathAttr(
		Element inStartElem, String inPath,
		String inAttrName, String inAttrValue
		)
	{
		return setTextOfSinglePathAttr(
			inStartElem, inPath,
			inAttrName, inAttrValue,
			true, false
			);
	}
	public Element setTextOfSinglePathAttr(
		String inPath,
		String inAttrName, String inAttrValue,
		boolean inDoCreate, boolean inDoNavigationWarnings
		)
	{
		return setTextOfSinglePathAttr(
			getJdomElement(), inPath,
			inAttrName, inAttrValue,
			inDoCreate, inDoNavigationWarnings
			);
	}
	public static Element setTextOfSinglePathAttr(
		Element inStartElem, String inPath,
		String inAttrName, String inAttrValue,
		boolean inDoCreate, boolean inDoNavigationWarnings
		)
	{
		final String kFName = "setTextOfSinglePathAttr";

		inPath = NIEUtil.trimmedStringOrNull( inPath );
		inAttrName = NIEUtil.trimmedStringOrNull( inAttrName );
		inAttrValue = NIEUtil.trimmedStringOrNull( inAttrValue );

		if( inStartElem == null || inPath == null ||
			inAttrName == null || inAttrValue == null
			)
		{
			errorMsg( kFName,
				"Null or empty input parameter(s)."
				+ ", inStartElem=" + inStartElem
				+ ", inPath=" + inPath
				+ ", inAttrName=" + inAttrName
				+ ", inAttrValue=" + inAttrValue
				+ " Returning null."
				);
			return null;
		}

		// Find or create the element
		Element currElem = findOrCreateElementByPath(
			inStartElem, inPath, inDoNavigationWarnings
			);

		if( currElem == null )
		{
			errorMsg( kFName,
				"Unable to find or create target tag."
				+ " (findOrCreateElementByPath returned null)"
				+ ", inStartElem=" + inStartElem
				+ ", inPath=" + inPath
				+ ", inAttrName=" + inAttrName
				+ ", inAttrValue=" + inAttrValue
				+ " Returning null."
				);
			return null;
		}

		boolean success = setAttributeString( currElem, inAttrName, inAttrValue );
		if( success )
			return currElem;
		else
		{
			errorMsg( kFName,
				"Unable to set attributte."
				+ " (setAttributeString returned false)"
				+ ", inStartElem=" + inStartElem
				+ ", inPath=" + inPath
				+ ", inAttrName=" + inAttrName
				+ ", inAttrValue=" + inAttrValue
				+ " Returning null."
				);
			return null;
		}

	}


	// Get an int, default of zero
	public int getIntFromPathText(
		String inPath
		)
	{
		return getIntFromPathText(
			getJdomElement(), inPath, 0
			);
	}
	public static int getIntFromPathText(
		Element inStartElem, String inPath
		)
	{
		return getIntFromPathText(
			inStartElem, inPath, 0
			);
	}
	// Return a default value if not found
	public int getIntFromPathText(
		String inPath, int defaultValue
		)
	{
		return getIntFromPathText(
			getJdomElement(), inPath, defaultValue
			);
	}
	public static int getIntFromPathText(
		Element inStartElem, String inPath,
		int defaultValue
		)
	{
		final String kFName = "getIntFromPathText";

		// First find the parent
		String pathText = getTextByPath( inStartElem, inPath );
		if( pathText == null || pathText.trim().equals("") )
			return defaultValue;
		pathText = pathText.trim();

		int theValue = defaultValue;
		try
		{
			theValue = Integer.parseInt( pathText );
			debugMsg( kFName,
				"Parsed Int string \"" +
				pathText + "\" to value " + theValue
				);
		}
		catch(Exception e)
		{
			errorMsg( kFName,
				"Error parsing Int string \"" +
				pathText + "\", returning default value " + defaultValue
				);
			theValue = defaultValue;
		}

		return theValue;
	}

	// Get a long, default of zero
	public long getLongFromPathText(
		String inPath
		)
	{
		return getLongFromPathText(
			getJdomElement(), inPath, 0L
			);
	}
	public static long getLongFromPathText(
		Element inStartElem, String inPath
		)
	{
		return getLongFromPathText(
			inStartElem, inPath, 0L
			);
	}
	// Return a default value if not found
	public long getIntFromPathText(
		String inPath, long defaultValue
		)
	{
		return getLongFromPathText(
			getJdomElement(), inPath, defaultValue
			);
	}
	public static long getLongFromPathText(
		Element inStartElem, String inPath,
		long defaultValue
		)
	{
		final String kFName = "getLongFromPathText";

		// First find the parent
		String pathText = getTextByPath( inStartElem, inPath );
		if( pathText == null || pathText.trim().equals("") )
			return defaultValue;
		pathText = pathText.trim();

		long theValue = defaultValue;
		try
		{
			theValue = Long.parseLong( pathText );
			debugMsg( kFName,
				"Parsed Long Int string \"" +
				pathText + "\" to value " + theValue
				);
		}
		catch(Exception e)
		{
			errorMsg( kFName,
				"Error parsing Long Int string \"" +
				pathText + "\", returning default value " + defaultValue
				);
			theValue = defaultValue;
		}

		return theValue;
	}



	// Get a Boolean, default of false
	public boolean getBooleanFromPathText(
		String inPath
		)
	{
		return getBooleanFromPathText(
			getJdomElement(), inPath, false
			);
	}
	public static boolean getBooleanFromPathText(
		Element inStartElem, String inPath
		)
	{
		return getBooleanFromPathText(
			inStartElem, inPath, false
			);
	}
	// Return a default value if not found
	public boolean getBooleanFromPathText(
		String inPath, boolean defaultValue
		)
	{
		return getBooleanFromPathText(
			getJdomElement(), inPath, defaultValue
			);
	}
	public static boolean getBooleanFromPathText(
		Element inStartElem, String inPath,
		boolean defaultValue
		)
	{
		// First find the parent
		String pathText = getTextByPath( inStartElem, inPath );

		// Have jdom helper do the rest
		return NIEUtil.decodeBooleanString( pathText, defaultValue );
	}

	// Similar to getBooleanFromAttribute, except that
	// this accpepts a path

	// Return false if not found
	public boolean getBooleanFromSinglePathAttr(
		String inPath, String inAttrName
		)
	{
		return getBooleanFromSinglePathAttr(
			getJdomElement(), inPath, inAttrName, false
			);
	}
	public static boolean getBooleanFromSinglePathAttr(
		Element inStartElem, String inPath, String inAttrName
		)
	{
		return getBooleanFromSinglePathAttr(
			inStartElem, inPath, inAttrName, false
			);
	}
	// Return a default value if not found
	public boolean getBooleanFromSinglePathAttr(
		String inPath, String inAttrName, boolean defaultValue
		)
	{
		return getBooleanFromSinglePathAttr(
			getJdomElement(), inPath, inAttrName, defaultValue
			);
	}
	public static boolean getBooleanFromSinglePathAttr(
		Element inStartElem, String inPath,
		String inAttrName, boolean defaultValue
		)
	{
		// First find the parent
		Element theElem = findElementByPath( inStartElem, inPath );
		if( theElem == null )
			return defaultValue;

		// Have jdom helper do the rest
		return getBooleanFromAttribute( theElem, inAttrName, defaultValue );
	}

	// Return a default value if not found
	public int getIntFromSinglePathAttr(
		String inPath, String inAttrName, int defaultValue
		)
	{
		return getIntFromSinglePathAttr(
			getJdomElement(), inPath, inAttrName, defaultValue
			);
	}
	public static int getIntFromSinglePathAttr(
		Element inStartElem, String inPath,
		String inAttrName, int defaultValue
		)
	{
		// First find the parent
		Element theElem = findElementByPath( inStartElem, inPath );
		if( theElem == null )
			return defaultValue;

		// Have jdom helper do the rest
		return getIntFromAttribute( theElem, inAttrName, defaultValue );
	}

	// Return a default value if not found
	public long getLongFromSinglePathAttr(
		String inPath, String inAttrName, long defaultValue
		)
	{
		return getLongFromSinglePathAttr(
			getJdomElement(), inPath, inAttrName, defaultValue
			);
	}
	public static long getLongFromSinglePathAttr(
		Element inStartElem, String inPath,
		String inAttrName, long defaultValue
		)
	{
		// First find the parent
		Element theElem = findElementByPath( inStartElem, inPath );
		if( theElem == null )
			return defaultValue;

		// Have jdom helper do the rest
		return getLongFromAttribute( theElem, inAttrName, defaultValue );
	}



	// Given a path and the name of an attribute, return the attribute's
	// contents as a list (white space separated lists are stored in attrs)
	// If you're doing this on the TOP level element
	// just use getListFromAttribute
	// Currently no plural form
	// Currently no form that sweeps for all elements
	public List getAttrValuesListFromSingularPath( String path,
		String listAttrName
		)
	{
		Element attrElem = findElementByPath( path );
		// Bail if we didn't find anything
		if( attrElem == null )
			return new Vector();
		return getListFromAttribute( attrElem, listAttrName );
	}

	// The static version, works on any element
	public static List getAttrValuesListFromSingularPath( Element startingElem,
		String path, String listAttrName
		)
	{
		if( startingElem == null )
			return new Vector();
		Element attrElem = findElementByPath( startingElem, path );
		// Bail if we didn't find anything
		if( attrElem == null )
			return new Vector();
		return getListFromAttribute( attrElem, listAttrName );
	}


	// Similar to getListFromAttribute
	// Given a jdom path and an attribute name, give back
	// a list of strings, from the content of that attribute
	// Will always return a list, even if no elements found,
	// so just need to check .size()
	// In this version we do NOT normalize to lower case and
	// we DO allow duplicates (vs. our getListFromAttrribute method)
	public List getListFromSinglePathAttr( String inPath, String inAttrName )
	{
		return getListFromSinglePathAttr( getJdomElement(), inPath, inAttrName );
	}
	public static List getListFromSinglePathAttr( Element inStartElem, String inPath,
		String inAttrName
		)
	{

		// The return list, we always return something
		List retList = new Vector();

		// Get the text, given the path and attribute name
		String attrStr = getTextFromSinglePathAttr( inStartElem,
			inPath, inAttrName
			);
		// Bail if failed or null string
		if( attrStr == null || attrStr.trim().equals("") )
			return retList;

		// Now prepare to loop through the string
		StringTokenizer st = new StringTokenizer( attrStr );
		while( st.hasMoreTokens() )
		{
			String item = st.nextToken();
			// Trim and make sure there's something really there
			item = item.trim();     // no .toLowerCase()
			if( ! item.equals("") )
			{
				// if( ! retList.contains(item) )
				retList.add(item);
			}
		}

		return retList;

	}

	// For this version we will look UP the tree for default values
	// Starting out with very few signatures for this one
	public String getTextFromPathOrInheritTrimOrNull(
		String inPath,
		String optAttrName,
		String optDefaultsBranchPath,
		int optMaxLevels
		)
	{
		return getTextFromPathOrInheritTrimOrNull(
			getJdomElement(),
			inPath, optAttrName, optDefaultsBranchPath,
			optMaxLevels
			);
	}
	public static String getTextFromPathOrInheritTrimOrNull(
		Element inStartElem,
		String optPath,
		String optAttrName,
		String optDefaultsBranchPath,
		int optMaxLevels
		)
	{
		final String kFName = "getTextFromPathOrInheritTrimOrNull";
		boolean debug = shouldDoDebugMsg( kFName );
		boolean trace = shouldDoTraceMsg( kFName );

		if( inStartElem == null ) {
			errorMsg( kFName,
				"Null starting element, returning NULL."
				);
			return null;
		}

		// Normalize all the items
		optPath = NIEUtil.trimmedStringOrNull( optPath );
		optAttrName = NIEUtil.trimmedStringOrNull( optAttrName );
		optDefaultsBranchPath = NIEUtil.trimmedStringOrNull( optDefaultsBranchPath );
		// a debug variable
		String startNodeName = inStartElem.getName();

		// Must have a path or attr name
		if( null==optPath && null==optAttrName ) {
			errorMsg( kFName,
				"Both inputs inPath and optAttrName are null."
				+ " Must supply at least one of them."
				+ " Returning NULL."
				);
			return null;
		}

		if(debug) debugMsg( kFName,
			"optPath=" + optPath
			+ ", optAttrName=" + optAttrName
			+ ", optDefaultsBranchPath=" + optDefaultsBranchPath
			+ ", inStartElem=" + NIEUtil.NL
			+ JDOMToString( inStartElem, true )
			);

		// Setup some state variables
		int traversedLevels = -1;   // The first time is incremented to ZERO
		Element currBaseElem = inStartElem;
		String answer = null;
		boolean isAtPrimaryLevel = true;

		// Do until done
		// Basically we go until
		// 1: We found an answer, or
		// 2: We've gone up the entire tree
		// 2: We've gone past the max limit, if any set
		while( true )
		{

			// There is sometimes a difference between the base element
			// and the content element
			Element contentElement = currBaseElem;

			// Debug variable
			String baseElemName = currBaseElem.getName();
			String contentElemName = contentElement.getName();

			// Calculate any incremental path, if any
			String incrementalPath = null;
			// If we have either path
			if( optPath != null || optDefaultsBranchPath != null )
			{
				// If we have an input path
				if( optPath != null )
				{
					// If we ALSO have a defaults path
					if( optDefaultsBranchPath != null )
					{
						// If the first time through, we want the main path
						if( isAtPrimaryLevel )
							incrementalPath = optPath;
						// Else not the first time though, we want to secondary
						else
							incrementalPath = optDefaultsBranchPath;
					}
					// Else no optional path, so always the main path
					else
					{
						incrementalPath = optPath;
					}
				}
				// Else inPath is null, so always use the default path
				else
				{
					// If it's the first time through, let them check
					// the current node so leave it null
					// If it's not the first time though, then look for incr
					if( ! isAtPrimaryLevel )
						incrementalPath = optDefaultsBranchPath;
				}
			}   // End if we have either path
			// Else if neither path it remains null

			// If inc != null, traverse
			if( incrementalPath != null )
			{
				// Traverse one level
				contentElement = traverseOneLevelByName(
					currBaseElem, incrementalPath,
					false, false
					);
				// update debug variable
				if( contentElement != null )
					contentElemName = contentElement.getName();
				// If that's null, we USUALLY done
				// but can try one more time if this was the first
				if( contentElement == null && isAtPrimaryLevel )
				{
					// Note that this is no longer the first time
					isAtPrimaryLevel = false;
					// And try again
					continue;
				}
			}

			// Now get the value
			String tmpValue = null;
			if( contentElement != null )
			{
				// If there's an attribute name, use that
				if( optAttrName != null )
					tmpValue = contentElement.getAttributeValue( optAttrName );
				// Else use the text of the node itself
				else
					tmpValue = contentElement.getText();
				// And normalize anything we may have gotten back
				tmpValue = NIEUtil.trimmedStringOrNull( tmpValue );
			}

			// If we have a value, we're done!
			if( tmpValue != null )
			{
				answer = tmpValue;
				break;
			}

			// Prepare for next try

			// First update the counter
			// Notes:
			// We increment here vs, above so that
			// two stabs at the root doesn't accidently count.
			// Also the counter starts at -1 so that the root
			// counts as zero
			traversedLevels++;
			// IF we HAVE a limit, and we're at it, then break
			if( optMaxLevels > 0 )
				if( traversedLevels >= optMaxLevels )
					break;

			// The moving up and down is based on the "current" node,
			// not the "content" node
			// If it's not the root, then move up!
			if( ! currBaseElem.isRootElement() )
			{
				currBaseElem = currBaseElem.getParent();
				// A sanity check
				if( currBaseElem == null )
				{
					errorMsg( kFName,
						"Unable to get parent of non-root Element?"
						+ " Returning NULL."
						);
					return null;
				}
			}
			// Else we're at the root, can't go any further
			else
			{
				break;
			}

			// Remember that we ARE now past the primary level
			isAtPrimaryLevel = false;

		}   // End do until done

		// All done, return whatever we got
		return answer;
	}

	// Similar for booleans
	public boolean getBooleanFromPathOrInherit(
		String optPath,
		String optAttrName,
		String optDefaultsBranchPath,
		boolean inDefaultValue,
		int optMaxLevels
		)
	{
		return getBooleanFromPathOrInherit(
			getJdomElement(),
			optPath, optAttrName, optDefaultsBranchPath,
			inDefaultValue, optMaxLevels
			);
	}
	public static boolean getBooleanFromPathOrInherit(
		Element inStartElem,
		String optPath,
		String optAttrName,
		String optDefaultsBranchPath,
		boolean inDefaultValue,
		int optMaxLevels
		)
	{

		// First, cheat and use the string inherited lookup
		String tmpStr = getTextFromPathOrInheritTrimOrNull(
			inStartElem,
			optPath,	optAttrName, optDefaultsBranchPath,
			optMaxLevels
			);

		// Have jdom helper do the rest
		return NIEUtil.decodeBooleanString( tmpStr, inDefaultValue );

	}


	// Add an element as the new last child of a node
	// Return the child that was added, or null if it failed
	public Element addElementToPath( String target, Element newChild )
	{
			return addElementToPath( getJdomElement(), target, newChild );
	}
	public static Element addElementToPath( Element sourceElem,
		String target, Element newChild
		)
	{
		final String kFName = "addElementToPath";

		// If the path is null, just add it to the starting node
		// Not useful as a direct call here, but helps support the
		// addXMLTextToPath which in turn calls this method
		Element parent = null;
		if( target == null || target.trim().equals("") )
			parent = sourceElem;
		else
		{
			// Find the node we're to remove
			parent = findElementByPath( sourceElem, target );
			if( parent == null )
				return null;
		}

		// To be safe, try and remove the child from any possible
		// old parent
		newChild.detach();

		// Tell the parent to add the child
		try
		{
			parent.addContent( newChild );
		}
		catch (Exception e)
		{
			errorMsg( kFName,
				"Got an exception: " + e
				+ " Returning null."
				);
			return null;
		}

		return newChild;
	}
	public Element addElementToPath( String target, JDOMHelper newChild )
	{
			return addElementToPath( getJdomElement(), target, newChild );
	}
	public static Element addElementToPath( Element sourceElem,
		String target, JDOMHelper newChild
		)
	{
		final String kFName = "addElementToPath";

		// If the path is null, just add it to the starting node
		// Not useful as a direct call here, but helps support the
		// addXMLTextToPath which in turn calls this method
		Element parent = null;
		if( target == null || target.trim().equals("") )
			parent = sourceElem;
		else
		{
			// Find the node we're to remove
			parent = findElementByPath( sourceElem, target );
			if( parent == null )
				return null;
		}

		// To be safe, try and remove the child from any possible
		// old parent
		Element lNewChild = newChild.getJdomElement();
		lNewChild.detach();

		// Tell the parent to add the child
		try
		{
			parent.addContent( lNewChild );
		}
		catch (Exception e)
		{
			errorMsg( kFName,
				"Got an exception: " + e
				+ " Returning null."
				);
			return null;
		}

		return lNewChild;
	}

	public Element addXMLTextToPath( String path, String xmlText )
	{
		return addXMLTextToPath( getJdomElement(), path, xmlText );
	}
	public static Element addXMLTextToPath( Element sourceElem,
		String path, String xmlText
		)
	{

		// Turn the XML Text into an element
		JDOMHelper newJH = null;
		try
		{
			newJH = new JDOMHelper( xmlText, null );
		}
		catch (Exception e)
		{
			return null;
		}

		// Grab the top element from it
		Element newElem = newJH.getJdomElement();

		// Now just call the normal, static add element method
		return addElementToPath( sourceElem, path, newElem );

	}


	// Given a path, create a NEW element with that path and add the
	// text as content
	public Element addSimpleTextToNewPath( String path, String simpleText )
	{
		return addSimpleTextToNewPath( getJdomElement(), path, simpleText );
	}
	public static Element addSimpleTextToNewPath( Element inSourceElem,
		String inPath, String inNewText
		)
	{

		final String kFName = "addSimpleTextToNewPath";

		if( inSourceElem == null || inPath==null || inNewText==null )
		{
			errorMsg( kFName,
				"Null value(s) passed in"
				+ ", inSourceElem=" + inSourceElem
				+ ", inPath=" + inPath
				+ ", inNewText=" + inNewText
				+ " Returning null."
				);
			return null;
		}

		int textLen = inNewText.length();

		List choppedPath = fullPathChopper( inPath );
		int pathLen = choppedPath.size();

		if( pathLen < 1 )
		{
			errorMsg( kFName,
				"Path too short; must have at least one element."
				+ ", inPath=" + inPath
				+ " Returning null."
				);
			return null;
		}

		// The last part of the path is the element we will create
		String lastPathBit = (String)choppedPath.get( pathLen - 1 );
		// And the rest is where we'll go
		choppedPath.remove( pathLen - 1 );

		// Some checking
		if( lastPathBit.equals("/") )
		{
			errorMsg( kFName,
				"Path must not end in '/'; not supported in this method."
				+ ", inPath=" + inPath
				+ " Returning null."
				);
			return null;
		}
		if( ! getIsPathBitAPlainElementName( lastPathBit ) )
		{
			errorMsg( kFName,
				"Last element in path must be simple element name."
				+ ", inPath=" + inPath
				+ ", lastPathBit=" + lastPathBit
				+ " Returning null."
				);
			return null;
		}

		// Lookup direct parent with list based lookup
		// Creating is OK
		// Currently we're not passing in an alternative root
		// Element directParent = findElementByPath( inSourceElem,
		//	choppedPath, true, null
		//	);
		// Allow for path where only the final node is given
		// Traverse intermediate path, IF ANY
		Element directParent = null;
		if( ! choppedPath.isEmpty() )
			directParent = findElementByPath( inSourceElem,
				choppedPath, true, null
				);
		else
			directParent = inSourceElem;

		// If null, complain, because we said creating was OK
		if( directParent == null )
		{
			errorMsg( kFName,
				"Unable to create intermediate path."
				+ " inSourceElem=" + inSourceElem
				+ ", inPath=" + inPath
				+ ", choppedPath=" + choppedPath
				+ " Returning null."
				);
			return null;
		}

		// Create the element itself
		Element newChild = new Element( lastPathBit );
		newChild.detach();

		// add the content

		//newChild.addContent( inNewText );
		// ^^^ need to handle long strings as cdata

		// WARNING: We're setting this here
		// Todo: make this an input option
		// Later we'll make it an argument you pass in
		// Todo: turn into a parameter
		int inAutoCDataThreshold = 1000;

		// Safely add the textual content
		setPotentialCDataTextOfElement(
			newChild, inNewText, inAutoCDataThreshold
			);

		// Add it to it's parent

		// Now attach it to it's parent
		directParent.addContent( newChild );

//		System.err.p rintln( "JDOMHelper:addSimpleTextToNewPath:"
//			+ " inSourceElem=" + inSourceElem.getName()
//			+ " inPath=" + inPath
//			+ " inNewText=" + inNewText
//			+ " newChild=" + newChild.getName()
//			+ " direct parent=" + directParent.getName()
//			);
		// Return what we created
		return newChild;

	}

	// Given a path, find or create an element with that path and
	// replace (or add if new) with this text
	public Element updateSimpleTextToExistingOrNewPath(
		String path, String simpleText
		)
	{
		return updateSimpleTextToExistingOrNewPath( getJdomElement(), path, simpleText );
	}
	// Find an element by it's path, and set a text attribute
	// This method CREATES a path to the node if it's not present
	// Todo: revisit, maybe add auto-create flag
	// Todo: remember to update call with inAutoCDataThreshold
	// when that's implemented as a passable variable
	// and in call to addSimpleTextToNewPath
	public static Element updateSimpleTextToExistingOrNewPath(
		Element inSourceElem, String optPath, String inNewText
		)
	{
		final String kFName = "updateSimpleTextToExistingOrNewPath";

		if( inSourceElem == null /* || inPath==null */|| inNewText==null )
		{
			errorMsg( kFName,
				"Null value(s) passed in"
				+ ", inSourceElem=" + inSourceElem
				+ ", inPath=" + optPath
				+ ", inNewText=" + inNewText
				+ " Returning null."
				);
			return null;
		}

		// This will be the element we add/replace content with
		Element targetElement = null;

		// Find the element if we already have it
		if( null!=optPath )
			targetElement = findOrCreateElementByPath( inSourceElem, optPath, false );
		else
			targetElement = inSourceElem;

		// If we didn't find it, go ahead and create it by just calling
		// the create method
		if( targetElement == null && null!=optPath )
		{
			return addSimpleTextToNewPath( inSourceElem, optPath, inNewText );
			// Todo: remember to update call with inAutoCDataThreshold
			// when that's implemented as a passable variable
		}

		// Clear out text that was already there
		// Todo: make this an option
		killText( targetElement );

		// Todo: make this an input option
		int inAutoCDataThreshold = 1000;

		// Safely add the textual content
		setPotentialCDataTextOfElement(
			targetElement, inNewText, inAutoCDataThreshold
			);

		// Return the results
		return targetElement;

	}

	// Remove any existing text or cdata from a node
	public void killText()
	{
		killText( getJdomElement() );
	}
	public static void killText( Element inElem )
	{
		final String kFName = "killText";
		final boolean trace = shouldDoTraceMsg( kFName );

		// final boolean debug = false;

		if( inElem == null )
		{
			errorMsg( kFName,
				"Null node passed in, nothing to do."
				);
			return;
		}

		// To be safe, we'll do this in 2 passes
		List children = inElem.getContent();
		debugMsg( kFName,
				"Will look for text nodes to kill."
				+ " Will examine " + children.size() + " nodes."
				+ " Use trace mode to see details."
				);

		int initCount = children.size();
		// Scan the list, go BACKWARDS since we're removing elements as we go
		for( int i = initCount-1; i>=0; i-- )
		{
			Object obj = children.get(i);
			if( trace )
				traceMsg( kFName,
						"Object offset " + i
						+ " is type \"" + obj.getClass().getName() + "\""
						);

			// Check the type
			if( obj instanceof CDATA || obj instanceof Text ||
				obj instanceof java.lang.String )
			{
				// do a nice debug message
				if( trace )
				{
					String tmpValue = null;
					if( obj instanceof CDATA )
					{
						tmpValue = ((CDATA)obj).getText();
					}
					else if( obj instanceof Text )
					{
						// tmpValue = ((Text)obj).getValue();
						tmpValue = ((Text)obj).getText();
					}
					else if( obj instanceof java.lang.String )
					{
						tmpValue = (String)obj;
					}

					traceMsg( kFName,
						"Removing obj offset " + i
						+ ", value was \"" + tmpValue + "\""
						);
				}
				// Actually remove it
				children.remove( i );
			}
			else
			{
				if(trace) traceMsg( kFName, "NOT removing object offset " + i );
			}
		}

		debugMsg( kFName,
				"At end, node now has "
				+ inElem.getContent().size()
				+ " content nodes."
				);

	}

//	public static void killText( Element inElem )
//	{
//		final boolean debug = true;
//
//		if( inElem == null )
//		{
//			System.err.p rintln( "Error: JDOMHelper:killText:"
//				+ " Null node passed in, nothing to do."
//				);
//			return;
//		}
//		// To be safe, we'll do this in 2 passes
//		List killList = new Vector();
//		List children = inElem.getContent();
//		if(debug) System.err.p rintln( "Debug: JDOMHelper:killText:"
//				+ " Will look for text nodes to kill."
//				+ " Will examine " + children.size() + " nodes."
//				);
//
//		// Scan the node for Text and CDATA nodes
//		for( Iterator it = children.iterator(); it.hasNext(); )
//		{
//			Object o = it.next();
//			if(debug) System.err.p rintln( "Object type is "
//					+ "\"" + o.getClass().getName() + "\""
//					);
//
//			if( o instanceof CDATA || o instanceof Text ||
//				o instanceof java.lang.String )
//			{
//				killList.add( o );
//				if(debug) System.err.p rintln( "found a text or cdata node" );
//			}
//			else
//			{
//				if(debug) System.err.p rintln( "not text or cdata" );
//			}
//		}
//		// Remove the Text and CDATA nodes
//		// start from the end, because I feel paranoid
//		if(debug) System.err.p rintln( "Debug: JDOMHelper:killText:"
//				+ " Will now remove nominated text nodes to kill."
//				+ " Will process " + killList.size() + " nodes."
//				);
//
//		for( int i = killList.size()-1; i >= 0; i-- )
//		{
//			Object o2 = killList.get(i);
//
//			if( o2 instanceof CDATA )
//			{
//				CDATA tmpObj = (CDATA)o2;
//				if(debug) System.err.p rintln( "removing cdata node "
//					+ "\"" + tmpObj.getText() + "\""
//					);
//				inElem.removeContent( tmpObj );
//			}
//			else if( o2 instanceof Text )
//			{
//				// Todo: Update JDom library, as it does show
//				// these remove and detach methods
//				// inElem.removeContent( (Text)o2 );  // won't compile
//				Text ot = (Text)o2;
//				if(debug) System.err.p rintln( "removing text node "
//					+ "\"" + ot.getValue() + "\""
//					);
//				// ot.detach(); // won't compile
//				// ot.setValue( "" ); // doesn't work
//				children.remove( o2 );
//			}
//			else if( o2 instanceof java.lang.String )
//			{
//				String tmpStr = (String)o2;
//				if(debug) System.err.p rintln( "removing string node "
//					+ "\"" + tmpStr + "\""
//					);
//				// inElem.removeContent( tmpStr );
//				inElem.removeContent( o2 );
//			}
//			else
//				System.err.p rintln( "Error: JDOMHelper:killText:"
//					+ " Trying to remove node that is neither CDATA nor Text."
//					+ " Will continue trying to remove any subsequent nodes."
//					);
//		}
//
//
//		if(debug) System.err.p rintln( "Debug: JDOMHelper:killText:"
//				+ " At end, node now has "
//				+ inElem.getContent().size()
//				+ " content nodes."
//				);
//
//	}

	// Long strings really should be stored as CDATA
	// I believe one problem I found was performance when reading
	// back in XML if long strings aren't CDATA, the parser wastes time
	// obsession about it
	private static void setPotentialCDataTextOfElement(
			Element inElem, String inNewText, int inAutoCDataThreshold
			)
	{

		//newChild.addContent( inNewText );
		// ^^^ need to handle long strings as cdata

		boolean tooBig = false;
		boolean hasSpecialChars = false;
		boolean forceCData = false;
		if( null!=inNewText ) {
			if( inAutoCDataThreshold >= 0 && inNewText.length() >= inAutoCDataThreshold )
				tooBig = true;
			// Check for special characters
			else {
				int ltAt = inNewText.indexOf( '<' );
				if( ltAt >= 0 ) {
					hasSpecialChars = true;
				}
				else {
					int gtAt = inNewText.indexOf( '>' );
					if( gtAt >= 0 ) {
						hasSpecialChars = true;
					}
					else {
						int ampAt = inNewText.indexOf( '&' );
						if( ampAt >= 0 ) {
							hasSpecialChars = true;
						}
						else {
							int qAt = inNewText.indexOf( '"' );
							if( qAt >= 0 ) {
								hasSpecialChars = true;
							}
							else {
								int aAt = inNewText.indexOf( '\'' );
								if( aAt >= 0 ) {
									hasSpecialChars = true;
								}
								else {
									int nAt = inNewText.indexOf( '\n' );
									if( nAt >= 0 ) {
										hasSpecialChars = true;
									}
									else {
										int rAt = inNewText.indexOf( '\r' );
										if( rAt >= 0 ) {
											hasSpecialChars = true;
										}
										else {
											int tAt = inNewText.indexOf( '\t' );
											if( tAt >= 0 ) {
												hasSpecialChars = true;
											}
											else {
												int ssAt = inNewText.indexOf( "  " );
												if( ssAt >= 0 ) {
													hasSpecialChars = true;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			forceCData = tooBig || hasSpecialChars;
		}

		// If no cdata limit was set, or it's set and we're below
		// it, go ahead and add as regular content
		// if( inAutoCDataThreshold < 0 ||
		// 	(inNewText.length() < inAutoCDataThreshold )
		// 	)
		if( ! forceCData )
		{
			inElem.addContent( inNewText );
		}
		// Otherwise we do need to use CDATA
		else
		{
			inElem.addContent( new CDATA(inNewText) );
		}

	}


	// Delete an element
	// Return the deleted element on success,
	// null on failure

	public Element removeElementByPath( String target )
	{
		return removeElementByPath( getJdomElement(), target );
	}

	public static Element removeElementByPath( Element sourceElem,
		String target
		)
	{
		// Find the node we're to remove
		Element endChild = findElementByPath( sourceElem, target );
		if( endChild == null )
			return null;

		// Find it's parent
		Element directParent = endChild.getParent();
		if( directParent == null )
			return null;

		// Tell the parent to delete this child, by reference
		boolean success = directParent.removeContent( endChild );

		if( success )
			return endChild;
		else
			return null;
	}



	public static Element flatListToXML( List inNestedList,
		String inTopNodeName
		)
	{
		final String kFName = "flatListToXML";
		inTopNodeName = NIEUtil.trimmedStringOrNull( inTopNodeName );
		if( inNestedList == null || inTopNodeName == null )
		{
			errorMsg( kFName,
				"Null/empty inputs."
				+ "inNestedList=" + inNestedList
				+ ", topNodeName=" + inTopNodeName
				+ " Returning null."
				);
			return null;
		}
		if( inNestedList.size() < 1 )
		{
			warningMsg( kFName,
				"Zero element list."
				+ " Will be returning just the top level node."
				);
		}

		// Todo: add sort routine

		// Create the top level
		Element topElem = new Element( inTopNodeName );
		if( topElem == null )
		{
			errorMsg( kFName,
				"Was unable to create top level element \"" + inTopNodeName + "\""
				+ ", got a null back."
				+ " Returning null."
				);
			return null;
		}

		// Itterate through the list of keys/values
		int counter = 0;
		for( Iterator it = inNestedList.iterator() ; it.hasNext() ; )
		{
			// Get the key/value double list
			List tinyList = (List) it.next();
			counter++;
			// Sanity check
			if( tinyList.size() != 2 )
			{
				errorMsg( kFName,
					"Invalid sub list, must have exactly 2 elements."
					+ " This sublist has " + tinyList.size() + " elements."
					+ " Key/value # " + counter + " for new node " + inTopNodeName
					+ " Returning null."
					);
				return null;
			}

			// get the key
			String key = (String) tinyList.get(0);
			// We only handle keys starting with ./ for now
			if( ! key.startsWith( "./" ) )
				continue;
			if( key.length() < 3 )
			{
				errorMsg( kFName,
					"./ is not a valid key name."
					+ " Key/value # " + counter + " for new node " + inTopNodeName
					+ " Returning null."
					);
				return null;
			}
			// Get the rest of the key, after the ./
			key = key.substring( 2 );
			if( key == null )
			{
				errorMsg( kFName,
					"./ is not a valid key name (2)."
					+ " Key/value # " + counter + " for new node " + inTopNodeName
					+ " Returning null."
					);
				return null;
			}
			// So now we have a key that we like

			// Get the value
			String value = (String)tinyList.get(1);

			// Now we split the path up and check its parts
			List pathParts = fullPathChopper( key );
			int origListLen = pathParts.size();
			if( origListLen < 1 )
			{
				errorMsg( kFName,
					"No path sections found (bad syntax)."
					+ " Key/value # " + counter + " \"" + key + "\""
					+ " for new node " + inTopNodeName
					+ " Returning null."
					);
				return null;
			}
			// We treat the last section different than the rest,
			// we will force it to be added later, and set it's content
			String lastPart = (String) pathParts.get( origListLen-1 );
			// And remove the last portion from the main path
			pathParts.remove( origListLen-1 );

			// Setup the node that we'll force an add to
			// By default, it's what we had before
			Element currElem = topElem;
			// Traverse, if there's anything to traverse
			if( pathParts.size() > 0 )
			{

				// Use the master traverse routine to traverse this path
				currElem = findElementByPath(
					currElem,   // Start with the current element
					pathParts,  // list of parsed bits
					true,       // YES, create nodes as needed
					currElem,   // For "root", just use what we started with
					false,      // Do NOT issue warnings
					0,          // Do not stop short
					false       // Don't bother to auto calculate root
					);

				// This really should have returned a node, since we
				// told it it's OK to create them, so if it didn't
				// something was very wrong

				errorMsg( kFName,
					"Unable to traverse/construct parent node."
					+ " Key/value # " + counter + " \"" + key + "\""
					+ " (not including the very last section of the path)"
					+ ", for new node " + inTopNodeName
					+ " Returning null."
					);
				return null;

			}
			// So by now we have a "current' node, for which we need
			// to force an add for the final bit (either element or attribute)


			// If it's an element, then we need to have the element
			// created and add the content after
			// If it's an attribute, we need to have the attr added, but
			// then add the content, maybe overwrite what's there

			// First add it, we know we always want to do that
			currElem = createOneLevelByName( currElem, lastPart );

			// Sanity check, did we have success
			if( currElem == null )
			{
				errorMsg( kFName,
					"Unable to construct leaf node."
					+ " Key/value # " + counter + " \"" + key + "\""
					+ " (look at the very last section of the path)"
					+ ", for new node " + inTopNodeName
					+ " Returning null."
					);
				return null;
			}

			// Get the components of the path bit
			String childName = extractChildNameFromPathBit( lastPart );
			String attrName = extractAttrNameFromPathBit( lastPart );

			// If it's a node, add the text
			if( childName != null )
			{
				currElem.addContent( value );
			}
			// Else if it's an attr, add this value
			else if( attrName != null )
			{
				currElem.setAttribute( attrName, value );
			}
			// Else we don't know what to do
			else
			{
				errorMsg( kFName,
					"Unable to add content to leaf node."
					+ " Key/value # " + counter + " \"" + key + "\""
					+ " (look at the very last section of the path)"
					+ ", for new node " + inTopNodeName
					+ " Returning null."
					);
				return null;
			}

		}   // End for each key/value pair

		// Return something
		return topElem;
	}



	/***************************************************
	 *
	 *      XSLT and Transforms
	 *
	 ****************************************************/
	private void __separator__XSLT_and_Transforms_ () {}

//	public static String getStyleSheetPath( String inSheetPath )
//	{
//		return "";
//	}

	// Giving us JUST a base file name, find it in the system
	// directory hierarchy
	// Typically: proj/classes/nie/core/system/style_sheets/foo.xslt
	public static InputStream getSystemStyleSheetStream(
		String inSheetName, boolean inDoWarnings
		)
	{
		final String kFName = "getSystemStyleSheetStream";

		inSheetName = NIEUtil.trimmedStringOrNull( inSheetName );
		if( inSheetName == null )
		{
			if( inDoWarnings )
				errorMsg( kFName,
					"Error: JDOMHelper:getSystemStyleSheetStream:"
					+ " Was passed in a null/empty style sheet name."
					+ " Nothing to open; returning a null stream."
					+ " If this is normal for your app you may want to"
					+ " call this method with inDoWarnings=false."
					);
			return null;
		}

		// Now calculate the full file name
		// Start with the base name
		String lFullFileName = inSheetName;
		// Add an extension if it doesn't have one already
		if( lFullFileName.indexOf( '.' ) < 0 )
			lFullFileName = lFullFileName + "." + SYSTEM_STYLE_SHEET_EXTENSION;
		// Now prepend the directory name
		// and here we're just using /'s, as they seem to work all around
		lFullFileName = SYSTEM_STYLE_SHEET_PATH + "/" + lFullFileName;

		// Now we need to create a bogus instance of a jdomelement so
		// we can call the .getClass() method
		JDOMHelper tmpJd = null;
		try
		{
			tmpJd = new JDOMHelper();
		}
		catch (Exception e)
		{
			errorMsg( kFName,
				"Unable to create tmp JDOMHelper node."
				+ " Exception was \"" + e + "\""
				+ " This prevents access to system resources for this class"
				+ " so returning a null stream."
				);
			return null;
		}

		// Try to open it
		InputStream input = tmpJd.getClass().getResourceAsStream(
			lFullFileName
			);

		// Sanity check
		if( input == null && inDoWarnings )
			errorMsg( kFName,
				"Unable to open requested system style sheet."
				+ " Requested name = \"" + inSheetName + "\""
				+ ", full system path attempted = \"" + lFullFileName + "\""
				+ ", which is usually relative to proj/classes/nie/core"
				+ " Returning a null stream."
				);

		// Now return whatever we were left with
		return input;

	}


	// Given an ELEMENT and style sheet, return a document

	// Todo: Revisit the many signatures
	// Gosh I wish Java had optional, named arguments

	// Given a jdom helper node and a style sheet, transform it
	// into a new document
	// This is just a wrapper
	// The non-static versions
	public Document xsltElementToDoc( String inStyleSheetName )
		throws JDOMHelperException
	{
		return xsltElementToDoc( inStyleSheetName, null );
	}
	public Document xsltElementToDoc( String inStyleSheetName,
		Hashtable inParamsHash
		)
		throws JDOMHelperException
	{
		return xsltElementToDoc( inStyleSheetName,
			inParamsHash, false
			);
	}
	public Document xsltElementToDoc( String inStyleSheetName,
		Hashtable inParamsHash, boolean inIsSystemStyleSheet
		)
		throws JDOMHelperException
	{
		Element lElem = getJdomElement();
		return xsltElementToDoc( lElem, inStyleSheetName,
			inParamsHash, inIsSystemStyleSheet
			);
	}

	// Given a jdom element and a style sheet, transform it
	// into a new document

	// These just convert jdom helper to a true jdom element
	public static Document xsltElementToDoc( JDOMHelper inElem,
			String inStyleSheetName
		)
		throws JDOMHelperException
	{
		Element lElem = inElem.getJdomElement();
		return xsltElementToDoc( lElem, inStyleSheetName );
	}

	public static Document xsltElementToDoc( Element inElem,
			String inStyleSheetName
		)
		throws JDOMHelperException
	{
		return xsltElementToDoc( inElem, inStyleSheetName, null );
	}

	// Given a jdom element and a style sheet, transform it
	// Pass in the parameters if any are set
	// into a new document

	public static Document xsltElementToDoc( Element inElem,
			String inStyleSheetName,
			Hashtable inParamsHash
		)
		throws JDOMHelperException
	{
		return xsltElementToDoc(
			inElem, inStyleSheetName,
			inParamsHash, false
			);
	}
	public static Document xsltElementToDoc( Element inElem,
			String inStyleSheetName,
			Hashtable inParamsHash,
			boolean inIsSystemStyleSheet
		)
		throws JDOMHelperException
	{
		Document srcDoc = null;
		if( inElem.isRootElement() )
			srcDoc = inElem.getDocument();
		else
		{
			Element newElem = (Element)inElem.clone();
			newElem.detach();
			srcDoc = new Document( newElem );
		}

		return xsltDocToDoc( srcDoc, inStyleSheetName,
			inParamsHash, inIsSystemStyleSheet
			);
	}

	// Versions that accept a compiled XSLT doc and
	// allow for control over cloning policy

	public static Document xsltElementToDoc( Element inElem,
		Transformer inTransformer,
		Hashtable inParamsHash
		)
		throws JDOMHelperException
	{
		return xsltElementToDoc( inElem,
			inTransformer, inParamsHash,
			true
		);
	}

	// I STRONGLY suggest you set Do Cloning to TRUE
	// This is a DESTRUCTIVE OPERATION on the XML DATA TREE
	// because it DETACHES it from it's tree
	// If you are SURE that this is OK, or this is already
	// cloned data, then you can turn it off.
	public static Document xsltElementToDoc( Element inElem,
		Transformer inTransformer,
		Hashtable inParamsHash,
		boolean inDoCloning
		)
		throws JDOMHelperException
	{
		Document srcDoc = null;
		if( inElem.isRootElement() )
			srcDoc = inElem.getDocument();
		else
		{
			Element newElem = null;
			if( inDoCloning )
				newElem = (Element)inElem.clone();
			else
				newElem = inElem;
			newElem.detach();
			srcDoc = new Document( newElem );
		}
		
		return xsltDocToDoc( srcDoc,
			inTransformer, inParamsHash
			);

	}

	// Given a DOCUMENT and style sheet, return a document


	// The static versions
	// Defaults:
	// - Assume a file name, vs a named system style sheet
	// - Assume we are NOT passing in a hash of parameters
	public static Document xsltDocToDoc(Document inDoc, String inStyleSheet)
		throws JDOMHelperException
	{
		return xsltDocToDoc( inDoc, inStyleSheet, null );
	}
	// Here we default to assuming the style sheet is a file name
	public static Document xsltDocToDoc(Document inDoc,
			String inStyleSheet,
			Hashtable inParamsHash
		)
		throws JDOMHelperException
	{
		return xsltDocToDoc(
			inDoc,
			inStyleSheet,
			inParamsHash,
			false
			);
	}
	
	
	
	// The main method
	// Todo: Allow for setting a base URI for stylesheets
	// to be relative to.
	// Truth is we usually just use system: anyway
	public static Document xsltDocToDoc(Document inDoc,
			String inStyleSheet,
			Hashtable inParamsHash,
			boolean inIsASystemStyleSheet
		)
		throws JDOMHelperException
	{
		Document outDoc = null;
		try
		{

			// The transformer engine we will use
			Transformer transformer = null;
			// The stream we may have open
			InputStream lStyleStream = null;

			// Acquire and compile the style sheet

			// If it's NOT a named system sheet, then it's a plain file name
			if( ! inIsASystemStyleSheet )
			{
				// File lStyleFile = NIEUtil.findInputFile( inStyleSheet );
				InputStream lStream = NIEUtil.openURIReadBin(
					inStyleSheet,
					null, // optRelativeRoot,
					null, null, // optUsername, optPassword,
					null, // inoutAuxIOInfo
					false	// usePost
					);
				// Create the transformer with the File object
				// transformer = TransformerFactory.newInstance()
				//	.newTransformer(new StreamSource(lStyleFile));
				transformer = TransformerFactory.newInstance()
					.newTransformer(new StreamSource(lStream));
			}
			// Else it IS a named system style sheet
			else
			{
				// Locate and open a stream for this system resource
				// And ask it to give us warnings if it fails
				lStyleStream = getSystemStyleSheetStream(
					inStyleSheet, true
					);
				// Create the transformer with the input stream
				transformer = TransformerFactory.newInstance()
					.newTransformer(new StreamSource(lStyleStream));
			}


			// Used the compiled transformer to process
			// the data
			outDoc = xsltDocToDoc( inDoc, transformer, inParamsHash );

			// Close the stream if we opened it
			if( lStyleStream != null )
			{
				try { lStyleStream.close(); }
					catch (Exception eClose) { }
			}
		}
		catch( Exception e )
		{
			throw new JDOMHelperException(
				"Error processing data with style sheet: " + e
				);
		}

		return outDoc;
	
	}
	
	

	// The main method
	// This assumes you have a compiled style sheet
	public static Document xsltDocToDoc( Document inDoc,
		Transformer inTransformer,
		Hashtable inParamsHash
		)
		throws JDOMHelperException
	{
		try
		{

			// Now start the work
			JDOMResult lOutResult = new JDOMResult();

			// Add the parameters, if we were given any
			if( inParamsHash != null )
			{
				Set keys = inParamsHash.keySet();
				for( Iterator it = keys.iterator() ; it.hasNext() ; )
				{
					String key = (String) it.next();
					String value = (String) inParamsHash.get( key );
					inTransformer.setParameter( key, value );
				}
			}

			// Do the actual work and return the answer
			inTransformer.transform(new JDOMSource(inDoc), lOutResult);

			// Return the results
			return lOutResult.getDocument();
		}
		catch (TransformerException e)
		{
			throw new JDOMHelperException("XSLT Transformation failed: " + e);
		}
		catch (Exception e2)
		{
			throw new JDOMHelperException("XSLT Transformation, setup problem"
				+ e2
				);
		}
	}
	
	
	
	
	
	// The main method
	public static Document xsltDocToDocV0(Document inDoc,
			String inStyleSheet,
			Hashtable inParamsHash,
			boolean inIsASystemStyleSheet
		)
		throws JDOMHelperException
	{
		try
		{

			// The transformer engine we will use
			Transformer transformer = null;
			// The stream we may have open
			InputStream lStyleStream = null;

			// If it's NOT a named system sheet, then it's a plain file name
			if( ! inIsASystemStyleSheet )
			{
				File lStyleFile = NIEUtil.findInputFile( inStyleSheet );
				// Create the transformer with the File object
				transformer = TransformerFactory.newInstance()
					.newTransformer(new StreamSource(lStyleFile));
			}
			// Else it IS a named system style sheet
			else
			{
				// Locate and open a stream for this system resource
				// And ask it to give us warnings if it fails
				lStyleStream = getSystemStyleSheetStream(
					inStyleSheet, true
					);
				// Create the transformer with the input stream
				transformer = TransformerFactory.newInstance()
					.newTransformer(new StreamSource(lStyleStream));
			}

			// Now start the work
			JDOMResult lOutResult = new JDOMResult();

			// Add the parameters, if we were given any
			if( inParamsHash != null )
			{
				Set keys = inParamsHash.keySet();
				for( Iterator it = keys.iterator() ; it.hasNext() ; )
				{
					String key = (String) it.next();
					String value = (String) inParamsHash.get( key );
					transformer.setParameter( key, value );
				}
			}

			// Do the actual work and return the answer
			transformer.transform(new JDOMSource(inDoc), lOutResult);

			// Close the stream if we opened it
			if( lStyleStream != null )
			{
				try { lStyleStream.close(); }
					catch (Exception eClose) { }
			}

			// Return the results
			return lOutResult.getDocument();
		}
		catch (TransformerException e)
		{
			throw new JDOMHelperException("XSLT Transformation failed: " + e);
		}
		catch (Exception e2)
		{
			throw new JDOMHelperException("XSLT Transformation, setup problem"
				+ e2
				);
		}
	}

	// Given a string containing a complete XSLT style sheet,
	// go ahead and compile it
	// Bytes are preferred to characters, so that the XML parser
	// has a chance to get the encoding right, so says StreamSource doc
	// Notice we do NOT provide one that accepts a String
	public static Transformer compileXSLTString( byte [] inContents )
		throws JDOMHelperException
	{
		final String kFName = "compileXSLTString";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( inContents.length < 1 )
			throw new JDOMHelperException( kExTag
				+ "Null/empty style sheet contents, nothing to compile."
				);
		
		try
		{
			// From bytes to a stream to a Stream Source		
			ByteArrayInputStream lByteStream = new ByteArrayInputStream( inContents );
			StreamSource lSource = new StreamSource( lByteStream );
	
			// Now compile
			Transformer outTransformer = TransformerFactory.newInstance()
						.newTransformer( lSource );
	
			// And return the results
			return outTransformer;
		}
		catch(Exception e)
		{
			throw new JDOMHelperException(
				"Error compiling XSLT style sheet: " + e
				);
		}
	}
		


	/***************************************************
	 *
	 *      Printing and Debugging
	 *
	 ****************************************************/
	private void __separator__Printing_and_Debugging_ () {}

	public void printAttributeList( String target )
	{
		final String kFName = "printAttributeList";

		List attrs = getListFromAttribute( target );
		debugMsg( kFName,
			getElementName() +
			" attr list '" + target + "':"
			);
		Iterator it = attrs.iterator();
		while( it.hasNext() )
		{
			debugMsg( kFName, "\t'" + (String)it.next() + "'" );
		}
	}

	public void listKids( )
	{
		listKids( getJdomElement() );
	}

	public static void listKids( Element elem )
	{
		final String kFName = "listKids";

		List kids = elem.getChildren();
		debugMsg( kFName, "Listing " + kids.size() + " children." );
		for( int i=0; i<kids.size(); i++ )
		{
			debugMsg( kFName, "\t" + (i+1) + ": " +
				((Element)kids.get(i)).getName()
				);
		}
	}

	public void print()
	{
		print( myElement );
	}
	public static void print( Element elem )
	{
		final String kFName = "print";

		// Get compact outputter
		XMLOutputter xo = new XMLOutputter( "    ", true );

		//xo.setTrimText(true);
		xo.setTextNormalize(true);

		try
		{
			xo.output( elem, System.err );
		}
		catch (Exception e)
		{
			errorMsg( kFName,
				"Error outputting jdom element."
				+ " Output of full tree unlikeley."
				+ " Exception: " + e
				);
		}
	}

	public String JDOMToString()
	{
		return JDOMToString( this.getJdomElement(), false );
	}
	public String JDOMToString( boolean inPrettyFormat )
	{
		return JDOMToString( this.getJdomElement(), inPrettyFormat );
	}
	public static String JDOMToString( Document inDoc )
	{
		return JDOMToString( inDoc.getRootElement(), false );
	}
	public static String JDOMToString( Document inDoc, boolean inPrettyFormat )
	{
		return JDOMToString( inDoc.getRootElement(), inPrettyFormat );
	}
	public static String JDOMToString( Element elem )
	{
		return JDOMToString( elem, false );
	}
	public static String JDOMToString( Element elem, boolean inPrettyFormat )
	{
		final String kFName = "JDOMToString";

		if( elem == null )
		{
			errorMsg( kFName,
				"Was passed a NULL element."
				+ " Nothing to convert to string."
				+ " Returning a text node with <null/>"
				);
			return "<null/>";
		}

		XMLOutputter xo = null;
		if( ! inPrettyFormat )
		{
			// Get compact outputter
			xo = new XMLOutputter();
			//xo.setTrimText(true);
			xo.setTextNormalize(true);
		}
		else
		{
			// Get indented outputter
			xo = new XMLOutputter( "  ", true );
		}

		// Get a stream to string buffer
		StringWriter sw = new StringWriter();

		// Run the output
		try
		{
			xo.output( elem, sw );
			sw.close();
		}
		catch (Exception e)
		{
			return "<error_converting_to_string/>";
		}

		// Return the string
		return sw.toString();

	}

	public boolean writeToFile( String fileName )
	{
		return writeToFile( getJdomElement(), fileName );
	}
	public boolean writeToFile( File inFile )
	{
		return writeToFile( getJdomElement(), inFile );
	}
	public boolean writeToFile( String fileName, boolean inDoCompactOutput )
	{
		return writeToFile( getJdomElement(), fileName, inDoCompactOutput );
	}
	public boolean writeToFile( File inFile, boolean inDoCompactOutput )
	{
		return writeToFile( getJdomElement(), inFile, inDoCompactOutput );
	}


	public static boolean writeToFile( Element inElem, String inFileName )
	{
		return writeToFile( inElem, inFileName, false );
	}
	public static boolean writeToFile( Element inElem, String inFileName,
		boolean inDoCompactOutput )
	{
		final String kFName = "writeToFile(3)";
		if( inFileName == null )
		{
			errorMsg( kFName,
				"Null output file name passed in."
				+ " Unable to write file."
				+ " Returning false (failure)."
				);
			return false;
		}
		return writeToFile( inElem, new File( inFileName ), inDoCompactOutput );
	}

	public static boolean writeToFile( Element inElem, File inFile )
	{
		return writeToFile( inElem, inFile, false );
	}


	public static boolean writeToFile( Element inElem, File inFile,
		boolean inDoCompactOutput )
	{
		final String kFName = "writeToFile(4)";
		return writeToFile( inElem, inFile, inDoCompactOutput, false );
	}


	public static boolean writeToFile( Element inElem, File inFile,
		boolean inDoCompactOutput, boolean inCreateDirsOK )
	{
		final String kFName = "writeToFile(5)";

		if( inElem == null || inFile == null )
		{
			errorMsg( kFName,
				"At least one of the required inputs was null."
				+ " inElem=\"" + inElem + "\""
				+ " inFile=\"" + inFile + "\""
				+ " Unable to write file."
				+ " Returning false (failure)."
				);
			return false;
		}

		// Should we check parent directories?
		if( inCreateDirsOK ) {
			// Is there even a parent path?
			File parent = inFile.getParentFile();
			if( null!=parent )
				// If it doesn't exist, go ahead and create it/them
				if( ! parent.exists() )
					parent.mkdirs();
					// ^^ if this fails we'll find out below soon enough
		}

		OutputStream outStream = null;
		try
		{
			File theFile = new File( System.getProperty( "user.dir" ), inFile.getCanonicalPath() );
			outStream = new FileOutputStream( theFile );
		}
		catch (Exception e)
		{
			try
			{
				outStream = new FileOutputStream( inFile );
			}
			catch (Exception e2)
			{
				errorMsg( kFName,
					"Error creating output stream to write xml to file."
					+ " Exception=\"" + e2 + "\""
					+ " inFileName=\"" + inFile + "\""
					+ " Unable to write file."
					+ " Returning false (failure)."
					);
				return false;
			}
		}

		// OutputStream tmpOut = new BufferedOutputStream( outStream );
		// Writer writer = new OutputStreamWriter( tmpOut );

		// Get compact outputter
		// XMLOutputter xo = new XMLOutputter( "    ", true );
		//XMLOutputter xo = new XMLOutputter( "    ", true, "ISO-8859-1" );
		

		//xo.setTrimText(true);
		// xo.setTextNormalize(true);

		XMLOutputter xo = null;
		if( inDoCompactOutput ) {
			// Get compact outputter
			xo = new XMLOutputter();
			//xo.setTrimText(true);
			xo.setTextNormalize(true);
		}
		else {
			// Get indented outputter
			xo = new XMLOutputter( "  ", true );
			// xo.setTextNormalize(true);
			xo.setTextTrim( true );
		}

		try {
			//xo.output( inElem, writer );
			xo.output( inElem, outStream );
		}
		catch (Exception e) {
			errorMsg( kFName,
				"Error outputting jdom element."
				+ " Exception=\"" + e + "\""
				+ " inFileName=\"" + inFile + "\""
				+ " Unable to write file."
				);
			return false;
		}

		//try { writer.close(); } catch (Exception e) { }
		//try { tmpOut.close(); } catch (Exception e) { }
		try { outStream.close(); }
		catch (Exception e)
		{
			errorMsg( kFName,
				"Error closing output file."
				+ " Exception=\"" + e + "\""
				+ " inFileName=\"" + inFile + "\""
				+ " Not sure that file was properly saved."
				);
			return false;
		}
		// Guess we're OK
		return true;
	}

	public static void writeToFileWithIncludes( Element inElem, File inFile,
			boolean inDoCompactOutput
		)
			throws JDOMHelperException
	{
		writeToFileWithIncludes( inElem, inFile, inDoCompactOutput, false );
	}
	public static void writeToFileWithIncludes( Element inElem, File inFile,
			boolean inDoCompactOutput, boolean inOkToCorrupt
		)
			throws JDOMHelperException
	{
		final String kFName = "writeToFileWithIncludes";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( inElem == null || inFile == null )
			throw new JDOMHelperException( kExTag + 
				"At least one of the required inputs was null."
				+ " inElem=\"" + inElem + "\""
				+ " inFile=\"" + inFile + "\""
				+ " Unable to write file."
				+ " Returning false (failure)."
				);

		// This is a destructive operation
		if( ! inOkToCorrupt )
		{
			try {
				inElem = (Element) inElem.clone();
			}
			catch( Exception c ) {
				throw new JDOMHelperException( kExTag +
					"Error cloning JDOM tree before saving to file with includes."
					+ " (this operation is destructive, so is usually performed on a clone)"
					+ " Error: " + c
					);
			}
		}

		// find all the nodes that started life as an include
		Hashtable inlineSubtrees = new Hashtable();
		Hashtable subtreeLevels = new Hashtable();
		findIncludedSubtrees( inElem, inlineSubtrees, subtreeLevels );

		// convert the tree bits into separate jdoms and
		// repair the referencing node to include
		// Hashtable miniSubtrees = new Hashtable();
		// Gnerally speaking we will be leaving the nodes IN PLACE, but we will
		// be removing their reference from their PARENT's view and raplacing that
		// reference to a new include node
		List reverseKeys = NIEUtil.sortIntVectorsDesc( inlineSubtrees.keySet() );
		for( Iterator it1 = reverseKeys.iterator() ; it1.hasNext() ; ) {
			// the int vector key
			List pathBitsKey = (List) it1.next();

			debugMsg( kFName, NIEUtil.NL + "path = " + pathBitsKey );

			// The node it points to
			Element inTreeReference = (Element) inlineSubtrees.get( pathBitsKey );
			// ^^^ do NOT detatch() yet!
			Element parent = inTreeReference.getParent();
			if( null==parent )
				throw new JDOMHelperException( kExTag +
					"No parent for element node " + JDOMToString( inTreeReference, false )
					);

			traceMsg( kFName, NIEUtil.NL + "Me = " + inTreeReference.getName() );
			traceMsg( kFName, NIEUtil.NL + "Parent = " + parent.getName() );


			// The original include reference
			String location = getStringFromAttributeTrimOrNull( inTreeReference, SYSTEM_ATTR_INCLUDE_LOCATION );
			if( null==location )
				throw new JDOMHelperException( kExTag +
					"No include element for node " + JDOMToString( inTreeReference, false )
					);
			// Remove the include stuff from there if it exists
			// inTreeReference.removeAttribute( SYSTEM_ATTR_INCLUDE_LOCATION );
			// inTreeReference.removeAttribute( SYSTEM_ATTR_BASE_URI );
			// inTreeReference.removeAttribute( SYSTEM_ATTR_LEVEL );
			// ^^^ NO, we need these later!

			// Create a replacment node
			Element replacementElem = new Element( INCLUDE_TAG );
			replacementElem.detach();
			replacementElem.setAttribute( INCLUDE_LOCATION_ATTR, location );

			// We need to replace the old child with the new one
			// Find out where the old one was
			// Get all siblings
			List siblings = parent.getChildren();
			// This should never be able to happen
			if( null == siblings || siblings.size() < 1 )
				throw new JDOMHelperException( kExTag +
					"No siblings for element node " + JDOMToString( inTreeReference, false )
					+ ", parent element " + JDOMToString( parent, false )
					);

			traceMsg( kFName, NIEUtil.NL + "Parent " + parent.getName() + " has " + siblings.size() + " children" );

			traceMsg( kFName, NIEUtil.NL + "Siblings = " + siblings );


			// Look up the current node
			int currentSlot = siblings.indexOf( inTreeReference );

			// traceMsg( kFName, NIEUtil.NL + "I'm at " + currentSlot );

			if( currentSlot < 0 ) {
				warningMsg( kFName,
					"Didn't find myself among all siblings."
					+ " Me = " + inTreeReference.getName()
					+ ", parent = " + parent.getName()
					+ ", siblings = " + siblings
					);
				/***
				throw new JDOMHelperException( kExTag +
					"Unable to find element node " // + JDOMToString( inTreeReference, false )
					+ " in parent element " // + JDOMToString( parent, false )
					);
				***/
				parent.addContent( replacementElem );
			}
			else {
				// Shove the new one in its place
				siblings.add( currentSlot, replacementElem );
				// And remove the old one
				// These next two may be redundant, but harmless
				siblings.remove( inTreeReference );
			}
			inTreeReference.detach();
		}

		// Now we will write out the trees, but in foward order, using a stack
		// to hold the bases
		Collection tmpLevels = subtreeLevels.values();

		int minLevel = -1;
		int maxLevel = -1;
		if( ! tmpLevels.isEmpty() ) {
			minLevel = NIEUtil.minIntFromCollection( tmpLevels );
			maxLevel = NIEUtil.maxIntFromCollection( tmpLevels );
	
			if( minLevel < 1 || minLevel > maxLevel )
				throw new JDOMHelperException( kExTag +
					"Invalid nesting:"
					+ " Levels = " + tmpLevels
					+ ", min=" + minLevel
					+ ", max=" + maxLevel
					);
		}
		else {
			minLevel = maxLevel = 0;
		}

		traceMsg( kFName,
			"Levels = " + tmpLevels
			+ ", min=" + minLevel
			+ ", max=" + maxLevel
			);

		// Create the path stack
		String [] pathStack = new String [maxLevel+1];
		writeToFile( inElem, inFile, inDoCompactOutput );
		// this may indeed be null, that's OK
		pathStack[0] = inFile.getParent();
		// We now traverse the nodes in forward document order
		// and write them out
		List forwardKeys = NIEUtil.sortIntVectorsAsc( inlineSubtrees.keySet() );
		for( Iterator it2 = forwardKeys.iterator() ; it2.hasNext() ; ) {
			// the int vector key
			List pathBitsKey = (List) it2.next();

			debugMsg( kFName, NIEUtil.NL + "Writing path = " + pathBitsKey );
			// The node it points to
			Element inTreeReference = (Element) inlineSubtrees.get( pathBitsKey );

			// Where it should go
			String location = JDOMHelper.getStringFromAttributeTrimOrNull( inTreeReference, SYSTEM_ATTR_INCLUDE_LOCATION );
			if( null==location )
				throw new JDOMHelperException( kExTag +
					"No location attribute: "
					+ " path=" + pathBitsKey
					+ ", Element=" + JDOMHelper.JDOMToString( inTreeReference, true )
					);
			// Don't really need these but maybe interesting
			String oldBase = JDOMHelper.getStringFromAttributeTrimOrNull( inTreeReference, SYSTEM_ATTR_BASE_URI );
			int oldLevel = JDOMHelper.getIntFromAttribute( inTreeReference, SYSTEM_ATTR_LEVEL, -1 );
			// Now Remove the include stuff from there if it exists
			inTreeReference.removeAttribute( SYSTEM_ATTR_INCLUDE_LOCATION );
			inTreeReference.removeAttribute( SYSTEM_ATTR_BASE_URI );
			inTreeReference.removeAttribute( SYSTEM_ATTR_LEVEL );

			// Get our current level
			Integer levelObj = (Integer) subtreeLevels.get( pathBitsKey );
			if( null==levelObj )
				throw new JDOMHelperException( kExTag +
					"No level in cache: "
					+ " path=" + pathBitsKey
					+ ", Element=" + JDOMHelper.JDOMToString( inTreeReference, true )
					);
			int level = levelObj.intValue();
			if( level < 1 || level > maxLevel )
				throw new JDOMHelperException( kExTag +
					"Invalid cached level = " + level
					+ " path=" + pathBitsKey
					+ ", Element=" + JDOMHelper.JDOMToString( inTreeReference, true )
					);
			// Calculate the file to use
			String baseURI = pathStack[ level-1 ];
			File thisFile = null;
			if( null!=baseURI )
				thisFile = new File( baseURI, location );
			else
				thisFile = new File( location );
			// Write out the file
			writeToFile( inTreeReference, thisFile, inDoCompactOutput, true );
			// And record our directory in the stack
			// which may in fact be null
			pathStack[ level ] = thisFile.getParent();

		}
	}





	private static void findIncludedSubtrees(
			Element inRootElement,
			Hashtable outSubtrees, Hashtable outSubtreeLevels
		)
			throws JDOMHelperException
	{
		final String kFName = "findIncludedSubtrees";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );
		//boolean trace = shouldDoTraceMsg( kFName );
		boolean trace=true;
		if( null==inRootElement )
			throw new JDOMHelperException( kExTag + "Null starting element, nothing to do." );
		if( null==outSubtrees || null==outSubtreeLevels )
			throw new JDOMHelperException( kExTag + 
				"Null output hash(es), can't returnm answers so nothing to do."
				+ " outSubtrees=" + outSubtrees
				+ ", outSubtreeLevels=" + outSubtreeLevels
				);

		try {
			final String kPath = ".//.[@" + SYSTEM_ATTR_INCLUDE_LOCATION + ']';
			XPath xpath = XPath.newInstance( kPath );

			debugMsg( kFName, "path=" + xpath.getXPath() );
	
			// List results = xpath.selectNodes( inDoc );
			List results = xpath.selectNodes( inRootElement );
	
			debugMsg( kFName,
				"Looking for \"" + kPath + "\" from node \"" + inRootElement.getName() + "\""
				// "Looking for \"" + inPath + "\" from node \"" + inDoc.getRootElement().getName() + "\""
				+ " Found " + results.size() + " forms."
				);
	
			int formCounter = 0;
			// For each form
			for( Iterator it = results.iterator() ; it.hasNext() ; ) {
				Object currObj = it.next();
				if( currObj instanceof org.jdom.Element ) {
					formCounter++;
					Element currElem = (Element) currObj;
					List currNPathBits = JDOMHelper.getPathToElementNumericList( currElem );
					if( trace ) {
						String currPath = JDOMHelper.getPathToElement( currElem );
						traceMsg( kFName, "our path=" + currPath );
						traceMsg( kFName, "npath bits=" + currNPathBits );
					}

					outSubtrees.put( currNPathBits, currElem );
					// pathHash.put( currNPathBits, currPath );

					// Calculate the include level
					int includeLevelCounter = 0;
					for( Element walkElem = currElem; null!=walkElem; ) {
						Attribute tmpAttr = walkElem.getAttribute( SYSTEM_ATTR_INCLUDE_LOCATION );
						// If it has an include element, count it
						if( null != tmpAttr )
							includeLevelCounter++;
						// Now go up one level
						// If we're aleady at the top, we're done
						if( walkElem == inRootElement || walkElem.isRootElement() )
							walkElem = null;
						// Else not at the top, so go up one level
						else
							walkElem = walkElem.getParent();
					}
					// Now store the answer
					outSubtreeLevels.put( currNPathBits, new Integer(includeLevelCounter) );

				}
				else {
					// Attribute currAttr = (Attribute) currObj;
					errorMsg( kFName,
						"Don't know how to handle non-element " 
						+ NIEUtil.NL + "  " // '\t'
						// + currAttr.toString()
						+ currObj
						+ ", skipping."
						);
				}
			}	// End for each matching node


		}
		catch( JDOMException e ) {
			throw new JDOMHelperException( kExTag + "Got JDOM/XML exception: " + e );
		}
		catch( Throwable t ) {
			t.printStackTrace( System.err );
			throw new JDOMHelperException( kExTag + "General exception: " + t );
		}



	}








	private static void ___Sep__Run_Logging__(){}
	///////////////////////////////////////////////////////////////////

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
	private static boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName, inFromRoutine,
			e, optMessage
			);
	}
	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	/***************************************************
	 *
	 *		Main
	 *
	 ***************************************************/
	private void __separator__main_ () {}

	public static void main(String[] args)
	{
		final String kFName = "main";

		if( args.length < 1 )
		{
			fatalErrorMsg( kFName, "syntax:\nscript input_test.xml [output.xml]" );
			System.exit(1);
		}

		try {
			String inFile = args[0];
			JDOMHelper jdh = new JDOMHelper( inFile, null, 0 );

			if( args.length > 1 ) {
			    String outFile = args[1];
			    jdh.writeToFile( outFile );
			}

		}
		catch( Exception e ) {
		    stackTrace( kFName, e, "Touble in Main" );
		}
		
		/***
		statusMsg( kFName, "testing cloneable" );
		String sampleDoc =
			"<food>" +
			"	<healthy>" +
			"		<fruit>apple</fruit>" +
			"		<fruit>orange</fruit>" +
			"		<fruit>bannana</fruit>" +
			"		<fruit>pear</fruit>" +
			"		<fruit>blueberry</fruit>" +
			"		<fruit>rasberry</fruit>" +
			"	</healthy>" +
			"	<healthy>" +
			"		<fruit>kiwi</fruit>" +
			"		<fruit>mango</fruit>" +
			"		<fruit>pineapple</fruit>" +
			"	</healthy>" +
			"</food>";

		JDOMHelper jh = null;
		try
		{
			jh = new JDOMHelper( sampleDoc, null );
		}
		catch (Exception e)
		{
			errorMsg( kFName, "Error creating test document: " + e );
			System.exit(1);
		}

		statusMsg( kFName, "Have original" );

		JDOMHelper jh2 = (JDOMHelper)jh.clone();

		statusMsg( kFName, "Have a clone" );

		//System.err.p rintln( "======== Sample Doc ===========" );
		//jh.p rint();
		***/


		/***
		for( int i = 0; i<args.length; i++ )
		{
			String query = args[i];
			System.err.p rintln( "==============================" );
			System.err.p rintln( "Trying '" + query + "'" );
			Element elem = jh.findElementByPath( query );
			if( elem == null )
				System.err.p rintln( "NONE FOUND" );
			else
				System.err.p rintln( JDOMHelper.JDOMToString( elem ) );
		}
		***/
	}

	private void __separator__Member_Fields_and_CONSTANTS_ () {}
	////////////////////////////////////////////////////////

	/**********************************************
	 *
	 *		Member Variables
	 *
	 ***********************************************/

	// WARNING!!!!!!
	// We implement cloneable, so remember to update the clone
	// method if you add non-static, mutable member variables

	// The main element that defines this object
	private Element myElement = null;
	// Optional: if read from a file/url, where was it?
	// This is what was handed to us
	private String fSourceURI;
	// Optional: if we used some of the file and URL locator
	// methods, they may have told us what the final URI was resolved to
	private String fFinalURI;
	// Optional: if a parser or document was constructed, save it
	private Document jdomDocument = null;
	// Our factory
	// private static SAXBuilder jdomBuilder = new SAXBuilder();
	// I'm not sure this is thread safe, making it non-static
	private SAXBuilder jdomBuilder = new SAXBuilder();
	// A cache for attribute lists
	// Assumes READ ONLY
	private Hashtable attrListCache = null;


	// Where to look for XSLT style sheets
	// This is relative to the directory where the .CLASS file
	// for JDOMHelper is located
	private static final String SYSTEM_STYLE_SHEET_PATH =
		"system/style_sheets";
	private static final String SYSTEM_STYLE_SHEET_EXTENSION = "xslt";

	private static final String INCLUDE_TAG = "include";
	private static final String INCLUDE_LOCATION_ATTR = "location";


	// If we have to create an attribute, and have no immediate value,
	// what should we put in it?
	// public static final String DEFAULT_NEW_ATTR_VALUE = "x";
	public static final String DEFAULT_NEW_ATTR_VALUE = "";

	// special system attributes for keeping track of includes
	// See also
	// AuxIOInfo.DEFAULT_RECORD_INCLUDES)
	// AuxIOInfo.getRecordInternalIncludeAttrs()
	public static final String SYSTEM_ATTR_INCLUDE_LOCATION = "_" + INCLUDE_TAG;
	public static final String SYSTEM_ATTR_BASE_URI = SYSTEM_ATTR_INCLUDE_LOCATION + "_base_uri";
	public static final String SYSTEM_ATTR_LEVEL = SYSTEM_ATTR_INCLUDE_LOCATION + "_level";

}
