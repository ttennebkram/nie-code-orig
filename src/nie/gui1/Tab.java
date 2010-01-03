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
import java.awt.Container;

/**
 * @author mbennett
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class Tab extends javax.swing.JComponent
// public class Tab extends java.awt.Container
{
	String mLabel;
	Component mParent;
	
	public Tab( Component inParent )
	{
		mParent = inParent;
	}
	public void setLabel( String inLabel )
	{
		mLabel = inLabel;
	}
	public String getLabel()
	{
		return mLabel;
	}
	public Component add( Component inComp )
	{
//System.err.println( "a" );
//		JTabbedPane dad = (JTabbedPane)this.getParent();
//System.err.println( "b" );
//		dad.addTab(getLabel(),inComp);
//System.err.println( "c" );
//		return inComp;
System.err.println( "a" );
		JTabbedPane dad = (JTabbedPane)mParent;
System.err.println( "b" );
		dad.addTab(getLabel(),inComp);
System.err.println( "c" );
		return inComp;
	}
}
