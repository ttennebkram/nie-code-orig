package nie.gui1;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.beans.*;
import java.lang.reflect.*;

// Do NOT import all of java.util.*, there are name collisions
// such as "List"
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;

import org.jdom.*;

import nie.core.*;

class XMLToGui
{

	public XMLToGui( String uri )
		throws JDOMHelperException, Exception
	{
		// Load and read in the XML definition
		fTree = new JDOMHelper( uri );

		// Traverse the XML tree and build the thing
		fComponents = build( fTree.getJdomElement() );

		// Run it or something
		// fComponents.show();
		((JFrame)fComponents).pack();
		fComponents.setVisible( true );
		
	}

	// A recursive builder!
	static Component build( Element root )
		throws Exception
	{
		return build( root, null );
	}
	static Component build( Element root,
		Component inParent
		)
		throws Exception
	{
		if( root == null )
		{
			System.err.println( "Null root node, returning null." );
			return null;
		}

		// The class name starts out life as the name of the node
		String theClassName = root.getName();
		// We allow for shortcut alaises, such as button for javax.swing.JButton
		if( fClassAliases.containsKey( theClassName ) )
			theClassName = (String) fClassAliases.get( theClassName );
		// We will also add a default prefix to unqualified class names that
		// are not aliased (if they have no dots, then unqualified)
		else if( theClassName.indexOf( '.' ) < 1 )
			theClassName = DEFAULT_CLASS_PREFIX + theClassName;

		System.err.println( "debug: class " + theClassName );
		Component component = null;
		// Now we get the class and an instance of it
		Class theClass = Class.forName( theClassName );
		if( theClassName.equals( "nie.gui1.Tab" ) )
			component = new Tab( inParent );
		else
			component = (Component)theClass.newInstance();

		// Now get the list of valid properties
		BeanInfo info = Introspector.getBeanInfo( theClass );
		PropertyDescriptor[] validProperties = info.getPropertyDescriptors();
		Hashtable propertyHash = new Hashtable();
		// transfer the array to a hash
		System.err.print( "debug: has properties: " );
		for( int i=0; i<validProperties.length; i++ )
		{
			PropertyDescriptor prop = validProperties[i];
			String propName = prop.getName();
			propertyHash.put( propName, prop );
			System.err.print( propName + '('
				// + prop.getPropertyType().getName()
				+ ") "
				);
		}
		System.err.println();


		// An array to hold a single arg for the setter
		Object[] methodArgs = new Object[1];

		// Loop through the XML element's properties
		java.util.List rootAttrs = root.getAttributes();
		if( rootAttrs != null )
		{
			for( Iterator it = rootAttrs.iterator(); it.hasNext() ; )
			{
				Attribute attr = (Attribute)it.next();
				String attrName = attr.getName();
				System.err.println( "attr " + attrName );
				// Find the property
				PropertyDescriptor prop = (PropertyDescriptor)propertyHash.get( attrName );
				if( prop == null )
				{
					System.err.println( "Warning: no matching property for attr " + attrName );
					continue;
				}
				// Get more info
				// type of argument
				Class propClass = prop.getPropertyType();
				// What method to call to set that
				Method setter = prop.getWriteMethod();
				// Sanity check
				if( setter == null )
					throw new Exception( "Setter for " + theClassName
						+ "." + attrName + " is read-only."
						);
				System.err.println( "Setter = " + setter.getName() );
	
				// Now copy the XML attr value into properties array
	
				// Is it a String?
				if( propClass == String.class )
					methodArgs[0] = attr.getValue();
				// Or maybe an integer?
				else if( propClass == int.class )
					methodArgs[0] = new Integer( attr.getIntValue() );
				// Or a boolean?
				else if( propClass == boolean.class )
					methodArgs[0] = new Boolean( attr.getBooleanValue() );
				// Color?
				else if( propClass == Color.class )
					methodArgs[0] = Color.decode( attr.getValue() );
				// Font?
				else if( propClass == Font.class )
					methodArgs[0] = Font.decode( attr.getValue() );
				else
					throw new Exception( "Don't have conversion rule for " + propClass.getName()
						+ " in " + theClassName	+ "." + attrName
						);
	
				// Now actually write the value
				setter.invoke( component, methodArgs );
	
			}	// End for each attr
		}	// end if attrs not null
		else
			System.err.println( "null attrs" );


		// Now traverse and construct the children
		java.util.List childElements = root.getChildren();
		Container componentAsContainer = null;
		for( Iterator it2 = childElements.iterator(); it2.hasNext() ; )
		{
			// Upconvert if first time through
			if( componentAsContainer == null )
				componentAsContainer = (Container)component;
			// Get the child and construct it
			Element childElem = (Element)it2.next();
			Component childComp = build( childElem, component );
			// Now add the child to this container
			if( componentAsContainer.getClass().getName().equals("javax.swing.JFrame") )
			{
				javax.swing.JFrame tmpContainer = (javax.swing.JFrame)componentAsContainer;
				tmpContainer.getContentPane().add(childComp);
			}
			else if( componentAsContainer.getClass().getName().equals("javax.swing.JTabbedPane") )
			{
				// Don't do anything, handled by tab tags underneath:w
				
//				javax.swing.JTabbedPane tmpContainer = (javax.swing.JTabbedPane)componentAsContainer;
//				tmpContainer.addTab( "Tab", childComp );
			}
			else
			{
				componentAsContainer.add( childComp );
			}		
		}

		// Return the top of our tree
		return component;
	}


	public static void main( String args[] )
	{
		if( args.length != 1 )
		{
			System.err.println( "Must specify XML file on command line." );
			System.exit( 1 );
		}


		XMLToGui stuff = null;
		try
		{
			stuff = new XMLToGui( args[0] );
		}
		catch(Exception e)
		{
			System.err.println( "Error loading XML: " + e );
			System.exit( 2 );
		}

	}



	// The main XML file
	private JDOMHelper fTree;
	// A hashtable of convenient aliases for classes
	private static Hashtable fClassAliases;
	// The top of the tree of built components
	private Component fComponents;

	// What to prepend to unaliased classes if they have no prefix
	public static final String DEFAULT_CLASS_PREFIX = "javax.swing.";


	static
	{

		// We have many names for the tags
		// Map each to it's real name, INCLUDING the real name version

		fClassAliases = new Hashtable();

		fClassAliases.put( "button", "javax.swing.JButton" );
		fClassAliases.put( "radio_button", "javax.swing.JRadioButton" );
		fClassAliases.put( "slider", "javax.swing.JSlider" );
		fClassAliases.put( "frame", "javax.swing.JFrame" );

		fClassAliases.put( "tabSet", "javax.swing.JTabbedPane" );
		fClassAliases.put( "label", "javax.swing.JLabel" );

		fClassAliases.put( "tab", "nie.gui1.Tab" );


//		fClassAliases.put( "frame", "javax.swing.JFrame" );
//		fClassAliases.put( "frame", "javax.swing.JFrame" );
//		fClassAliases.put( "frame", "javax.swing.JFrame" );


	}



}
