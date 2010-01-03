package nie.config_ui;

import java.util.*;
import java.io.*;
import java.text.*;

import java.lang.reflect.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import javax.swing.text.JTextComponent;
// import javax.swing.text.*;
import javax.swing.event.*;
// import javax.swing.event.DocumentListener;

import org.jdom.*;
import org.jdom.xpath.*;

import nie.core.*;
import nie.sn.SearchEngineConfig;
import nie.spider.*;

public class Configurator2 {

	public static final String kClassName = "Configurator2";
	public static final String kFullClassName = "nie.config_ui." + kClassName;

	public static void main(String[] args) {
		final String kFName = "main";

		// Event_VerifyDBConn obj = new Event_VerifyDBConn();
		// statusMsg( kFName,
		//	"full class is " + obj.getClass().getName()
		//	);

		if( args.length < 1 || args.length > 2 ) {
			errorMsg( kFName,
				NIEUtil.NL +
				"Syntax (1): script config_file.xml [-create_ok]"
				);
			System.exit( 1 );
		}

		String fileName = args[0];
		boolean createOK = false;
		if( args.length > 1 ) {
			String tmpStr = args[1];
			// allow for -create_ok, -createok, -create, etc
			if( tmpStr.startsWith("-create") )
				createOK = true;
			else {
				errorMsg( kFName,
					NIEUtil.NL
					+ "Syntax (2): script config_file.xml [-create_ok]" + NIEUtil.NL
					+ "Unknown option \"" + tmpStr + "\""
					);
				System.exit( 1 );
			}
		}

		try {
			new Configurator2( fileName, createOK );
		}
		catch( ConfiguratorException e ) {
			errorMsg( kFName, "Caught exception: " + e );
		}
	}

	private static void __sep__Initialization__() {}
	/////////////////////////////////////////////////////////////

	// Set Native look and feel
	static {
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			// SwingUtilities.updateComponentTreeUI(mMainFrame);
			// mMainFrame.pack();
	 	}
	 	catch (Exception exc) {
	 		System.err.println( "Could not load native look and feel. Exception: " + exc );
		}
	}


	// JTextField firstname, lastname, address1, address2, city, zip;
	// JComboBox state;
	
	public Configurator2( String inConfigFileName, boolean inCreateOK )
		throws ConfiguratorException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Get the form definition	
		// try {
			// mXmlForm = nie.webui.XMLDefinedScreen.getXML( kClassName );
			mXmlForm = getXMLFormDef();
		// } catch( nie.webui.UIException e ) {
		// 	System.err.println( "Unable to open XML form: " + e );
		// 	System.exit( 2 );
		// }

		if( null==inConfigFileName )
			throw new ConfiguratorException( kExTag +
				"Null config file name passed in."
				);
		mSaveFileName = inConfigFileName;
		File fin = new File(inConfigFileName);
		// boolean existed = fin.canRead();
		boolean existed = fin.exists();
		mIsNew = ! existed;

		if( ! existed && ! inCreateOK ) {
			int answer = showYesNoCancelDialog(
					"SearchTrack Config Confirmation",
					"File \"" + inConfigFileName + "\" doesn't exist."
					+ "\n\nCreate it as a new configuration ?"
					);
			// Yes, create
			if( 0==answer ) {
				// Nothing to do, will be handled later
			}
			// Cancel (and exit)
			else if( 2==answer ) {
				System.exit(0);
			}
			else {
				throw new ConfiguratorException( kExTag +
					"Unable to access file \"" + inConfigFileName + "\""
					);
			}
		}

		if( ! inConfigFileName.endsWith( ".xml" ) )
			throw new ConfiguratorException( kExTag +
				"Invalid file name \"" + inConfigFileName + "\"; must end in .xml"
				);

		try {
			if( existed ) {
				infoMsg( kFName, "Reading existing file \"" + inConfigFileName + "\"" );
				mConfigTree = new JDOMHelper( inConfigFileName, null, 0, null );
				mIsSaved = true;
			}
			else {
				// Element tmpElem = new Element( "nie_config" );
				// mConfigTree = new JDOMHelper( tmpElem );
				// Get a template that includes our preferred <include> structure
				infoMsg( kFName, "Creating a new config file" );
				mConfigTree = getXMLTemplateConfig();
				mIsModified = true;
			}
		} catch( JDOMHelperException je ) {
			System.err.println( "Unable to create/load XML config data: " + je );
			System.exit( 3 );
		}
	
		registerFormFields();

		/***
		try {
			JDOMHelper.writeToFile(
				mConfigTree.getJdomElement(), new File("tmpdir/cons.xml"), false
				);
			JDOMHelper.writeToFileWithIncludes(
				mConfigTree.getJdomElement(), new File("tmpdir/main.xml"), false
				);
		}
		catch( JDOMHelperException e ) {
			errorMsg( kFName, "Got exception writing file: " + e );
		}
		***/

		// statusMsg( kFName, "In test mode, exiting now." );
		// System.exit(0);

		renderForm( ! existed );
	
	}

	public JDOMHelper getXMLFormDef()
		throws ConfiguratorException
	{
		final String kFName = "getXMLFormDef";
		final String kExTag = kClassName + '.' + kFName + ": ";

		JDOMHelper outXML = null;
		debugMsg( kFName, "Looking for xml " + FORM_DEF_URI );

		// Force system: references relative to this class
		AuxIOInfo tmpAuxInfo = new AuxIOInfo();
		tmpAuxInfo.setSystemRelativeBaseClassName( kFullClassName );

		try {
			outXML = new JDOMHelper(
				FORM_DEF_URI,
				null, // optRelativeRoot
				0,
				tmpAuxInfo
				);
		}
		catch( Exception e )
		{
			errorMsg( kFName, "Stacktrace of JDOMHelper constructor:");
			e.printStackTrace( System.err );
			throw new ConfiguratorException( kExTag + "Error loading XML: " + e );
		}
		return outXML;
	}

	public JDOMHelper getXMLTemplateConfig()
		throws ConfiguratorException
	{
		final String kFName = "getXMLTemplateConfig";
		final String kExTag = kClassName + '.' + kFName + ": ";

		JDOMHelper outXML = null;
		debugMsg( kFName, "Looking for xml " + TEMPLATE_CONFIG_URI );

		// Force system: references relative to this class
		AuxIOInfo tmpAuxInfo = new AuxIOInfo();
		tmpAuxInfo.setSystemRelativeBaseClassName( kFullClassName );

		try {
			outXML = new JDOMHelper(
				TEMPLATE_CONFIG_URI,
				null, // optRelativeRoot
				0,
				tmpAuxInfo
				);
		}
		catch( Exception e )
		{
			errorMsg( kFName, "Stacktrace of JDOMHelper constructor:");
			e.printStackTrace( System.err );
			throw new ConfiguratorException( kExTag + "Error loading XML: " + e );
		}
		return outXML;
	}






	private static void __sep__High_Level_Operations_AND_Actions__() {}
	//////////////////////////////////////////////////////////



	public void renderForm( boolean inIsNew ) {
		final String kFName = "renderForm";
		// boolean debug = shouldDoDebugMsg( kFName );
		final boolean debug = true;
		// boolean debug = true;

		mMainFrame = new JFrame();	// Title set below
		mMainFrame.getContentPane().setLayout( new BorderLayout() );
		// mMainFrame.getContentPane().setLayout( new GridLayout(1,2) );
		mMainFrame.setResizable( false );

		// renderMenu();
		debugMsg( kFName, "SKIPPING renderMenu()" );

		// Basic window setup
		// ===============================================================
		// mMainFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		// or .setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
		// The WindowListener Interface
		// public void windowClosing(WindowEvent e) {
		// public void windowClosed(WindowEvent e) {
		mMainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		mMainFrame.addWindowListener(new WindowListener() { 
			public void windowClosed(WindowEvent e) {
				// Configurator2.this.verifyAll();
				// quit();
			}
			public void windowClosing(WindowEvent e) {
				quit();
			}
			public void windowIconified(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowOpened(WindowEvent e) {}
			public void windowActivated(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
		});
		// mMainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);	

		// Title for Window
		String mainTitle = mXmlForm.getStringFromAttributeTrimOrNull( "title" );
		mainTitle = (null==mainTitle) ? "Config Editor" : mainTitle;
		// Add the operation and file name
		mainTitle += " v2: " + (inIsNew ? "creating " : "editing ") + mSaveFileName;
		mMainFrame.setTitle( mainTitle );

		// LEFT half of screen
		// ============================================================
		JPanel leftPanel = new JPanel();
		int w = 5;
		leftPanel.setBorder( new EmptyBorder(w,w,w,w) );
		leftPanel.setLayout( new BorderLayout() );	
		mMainFrame.getContentPane().add( leftPanel, BorderLayout.WEST );
		JLabel catsLabel = new JLabel( "<html><b>Configuration Categories</b></html>" );
		leftPanel.add( catsLabel, BorderLayout.NORTH );
		
		// TREE
		// ================================================
		// Tabs are navigated in a TREE now
		DefaultMutableTreeNode treeRoot =
			new DefaultMutableTreeNode("Settings");
		mNavTree = new JTree( treeRoot );

		// Tweak the style
		// mNavTree.setRootVisible( false );  <<== Do this LATER ON
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon( null );
		renderer.setOpenIcon( null );
		renderer.setClosedIcon( null );
		mNavTree.setCellRenderer(renderer);
		mNavTree.setShowsRootHandles( true );
		// mNavTree.putClientProperty( "JTree.lineStyle", "None" );
		// Expand the tree AFTER nodes have been added

		// Wire up the Tree events
		mNavTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse){
		        // if(((TreeNode)tse.getPath().getLastPathComponent()).isLeaf())
		        // {
		        	String keyText = ( (TreeNode)tse.getPath().getLastPathComponent() ).toString();
		        	if(debug) debugMsg( kFName, "Tree click on: '" + keyText + "'" );
		        	mTabChanger.show( mTabsPanel, keyText );
		        // }
			}
		});

		// Add the tree as cell # 1 (of 2)
		// mMainFrame.getContentPane().add( new JScrollPane(mNavTree) );
		// mMainFrame.getContentPane().add( mNavTree );
		// mMainFrame.getContentPane().add( mNavTree, BorderLayout.WEST );
		// Try to inset it a bit, make it look nicer
		// mMainFrame.getContentPane().add( new JScrollPane(mNavTree), BorderLayout.WEST );
		leftPanel.add( new JScrollPane(mNavTree), BorderLayout.CENTER );

		// The main Submit buttons
		// =====================================
		JPanel buttonPanel = renderBottomButtons();
		leftPanel.add( buttonPanel, BorderLayout.SOUTH );
		// debugMsg( kFName, "SKIPPING renderBottomButtons()" );

		

		
		// RIGHT half
		// ===========================================================

		// Changing from Tabbed to Cards
		// JTabbedPane tframe = new JTabbedPane();
		// mMainFrame.getContentPane().add( tframe, BorderLayout.NORTH );
		mTabChanger = new CardLayout();
		mTabsPanel = new JPanel( mTabChanger );
		// mTabsPanel.setLayout( new BorderLayout() );	
		// Add the tabs/cards as cell # 2 (of 2)
		// mMainFrame.getContentPane().add( mTabsPanel );
		// Use border layout
		mMainFrame.getContentPane().add( mTabsPanel, BorderLayout.EAST );
	
		GridBagConstraints gbc = new GridBagConstraints();
		final int n = 12;
		// gbc.insets = new Insets( 2, 0, 2, n );
		gbc.insets = new Insets( 2, 0, 2, 6 );
		// gbc.insets = new Insets( 0, 0, 0, 0 );

		if(debug) debugMsg( kFName, "Start" );	

		// For each section
		java.util.List sections = mXmlForm.findElementsByPath( "section" );

		if(debug) debugMsg( kFName, "# sections = " + sections.size() );	

		// Where new nodes get attached in the tree
		int MAX_LEVEL=5;
		DefaultMutableTreeNode [] branches = new DefaultMutableTreeNode[ MAX_LEVEL+1 ];
		branches[ 0 ] = treeRoot;

		int sectionCounter = 0;
		int prevLevel = 0;
		// For each Section
		for( Iterator sit = sections.iterator() ; sit.hasNext() ; ) {
			Element section = (Element) sit.next();
			sectionCounter++;

			// The panel we will place widgets on
			JPanel p = new JPanel();
			gbc = new GridBagConstraints();
			p.setLayout( new GridBagLayout() );
			// p.setBackground( Color.RED );
	
			String title = section.getAttributeValue( "title" );
			title = nie.core.NIEUtil.trimmedStringOrNull( title );
			title = (null==title) ? "Section " + sectionCounter : title;
			
			int thisLevel = nie.core.JDOMHelper.getIntFromAttribute( section, "level", 1 );
			if(debug) debugMsg( kFName, "'" + title + "' = level " + thisLevel );
			if( thisLevel > prevLevel+1 ) {
				errorMsg( kFName, "Level indent too big (can only indent 1 level at a time), prevLevel=" + prevLevel + ", reqested level=" + thisLevel );
				thisLevel = prevLevel + 1;
			}
			if( thisLevel < 1 ) {
				errorMsg( kFName, "Level < 1, setting to 1: level was " + thisLevel );
				thisLevel = 1;
			}
			if( thisLevel > MAX_LEVEL ) {
				errorMsg( kFName, "Level > MAX_LELVEL (" + MAX_LEVEL + "), capping at max: level was " + thisLevel );
				thisLevel = MAX_LEVEL;
			}
			DefaultMutableTreeNode parent = branches[ thisLevel - 1 ];
			if( null==parent ) {
				errorMsg( kFName, "parent was null, attaching to root" );
			}
			// Null out prev children, to be safe for future cycles
			for( int j=thisLevel+1; j<=MAX_LEVEL; j++ )
				branches[j] = null;
			prevLevel = thisLevel;
			// And add it to the Navigation Tree
			// mNavTree...
			DefaultMutableTreeNode newLeaf = new DefaultMutableTreeNode( title );
			parent.add( newLeaf );
			branches[ thisLevel ] = newLeaf;
		
			// A parent panel that we can show our content panel North on
			JPanel pp = new JPanel();
			// pp.setBorder( new EmptyBorder(0,0,0,0) );
			// add our widget panel to this panel
			pp.add( p, BorderLayout.NORTH );
			// Add the parent panel to the tab
			mTabsPanel.add( title, pp );
			// mTabsPanel.add( title, p );
			// mTabsPanel.add( title, p, BorderLayout.NORTH );
			// tframe.addTab( title, null, p, tip );
			// GridBagConstraints gbc = new GridBagConstraints();
	
			// For each field in this section
			java.util.List fields = nie.core.JDOMHelper.findElementsByPath( section, "field" );
			if(debug) debugMsg( kFName, "# sections = " + fields.size() );	
			int rowCounter = 0;

			// For each field
			for( Iterator fit = fields.iterator() ; fit.hasNext() ; ) {
				Element field = (Element) fit.next();
				rowCounter++;

				String type = field.getAttributeValue( "type" );
				type = NIEUtil.trimmedLowerStringOrNull( type );
	
				String shortName = field.getAttributeValue( "name" );
				shortName = nie.core.NIEUtil.trimmedStringOrNull( shortName );
				if( null==shortName
					&& (null==type || ! type.equals("inline_button"))
					&& (null==type || ! type.equals("vertical_spacer"))
				) {
					System.err.println(
						"Field with no name attribute"
						+ ": Section " + sectionCounter
						+ " field " + rowCounter
						+ ".  Skipping."
						);
					continue;
				}

				// Display the label, if any
				// ==============================================
				String displayName = renderFieldLabelIfAny( p, field, type, rowCounter-1, gbc );


				// Figure out which value to display, if any
				// ==============================================
				String populateValue = getValueToUse(
					field, shortName, displayName, inIsNew, type
					);


				// Render the actual Input Widget
				// ==============================================

				// Place the widget
				gbc.gridx = 1;	// 2nd column
				gbc.gridy = rowCounter-1;	// current row
				gbc.gridwidth = 2;
				gbc.fill = GridBagConstraints.HORIZONTAL;

				// Vertical spacer / centered sub label
				if( null!=type && type.equals("vertical_spacer") ) {
					renderVerticalSpacer( p, field, rowCounter-1, gbc );
				}
				// Drop down list
				else if( null!=type && (type.equals("select") || type.equals("dropdown")) ) {
					renderDropDownWidget( p, field, shortName, populateValue, gbc );
				}
				// Check box
				else if( null!=type && type.startsWith("check") ) {
					renderCheckBoxWidget( p, field, shortName, populateValue, gbc );
				}
				// Text Area (VS normal one line text input)
				else if( null!=type && type.equals("textarea") ) {
					renderTextAreaWidget( p, field, shortName, populateValue, gbc );
				}
				// a READ-ONLY Text Area
				else if( null!=type && type.equals("read_only") ) {
					renderReadOnlyWidget( p, field, shortName, populateValue, gbc );
				}
				// A button
				else if( null!=type && type.equals("inline_button") ) {
					renderInlineButton( p, field, shortName, populateValue, gbc );
				}
				// Specifically text
				else if( null!=type && (type.equals("text") || type.equals("input")) ) {
					renderTextBoxWidget( p, field, shortName, populateValue, gbc );
				}
				// Defaults to text input box
				else {
					renderTextBoxWidget( p, field, shortName, populateValue, gbc );
				}
	
			}	// End for each field
		}	// End for each Section

		// Expand the tree
		// mNavTree.setRootVisible( false );
		// mNavTree.setSelectionRow( 1 );
		// mNavTree.expandRow( 2 );
		// NOTE: the value of getRowCount() can CHANGE while the loop is
		// running, as hidden subtrees are exposed, so
		// do NOT cache the value getRowCount()
		for( int row = 0; row < mNavTree.getRowCount(); row++ ) {
			mNavTree.expandRow( row );
		}
		mNavTree.setRootVisible( false );


		mMainFrame.pack();
		mMainFrame.show();
	}

	/////////////////////////////////////////////



	boolean verifyAll() {
		return verifyAll( false, true );
	}


	boolean verifyAll( boolean hasAlreadyCopiedSwing, boolean inShowIfOK )
	{
		final String kFName = "verifyAll";

		mMainFrame.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );

		if( ! hasAlreadyCopiedSwing )
			copySwingValuesToJdomTree();

		nie.sn.SearchTuningConfig tmpConfig = null;
		try {
			tmpConfig = new nie.sn.SearchTuningConfig(
				mConfigTree,
				mSaveFileName,
				new nie.sn.SearchTuningApp()
				);

			// mMainFrame.setCursor( Cursor.DEFAULT_CURSOR );

		}
		// Really serious problem
		catch( Exception e ) {
			errorMsg( kFName, "Error checking config: " + e );
			e.printStackTrace( System.out );

			mMainFrame.setCursor( Cursor.getDefaultCursor() );

			showLastRunlogErrorAsDialogBox(
				"Configuration Problem",
				"There is a problem with this configuration."
				);

			mIsVerified = false;

			return false;
		}

		SearchEngineConfig search = tmpConfig.getSearchEngine();
		java.util.List tmpMarkers = null;
		if( null!=search )
			tmpMarkers = search.getSuggestionMarkerLiteralText();

		// Search config, this should have thrown an exception already
		if( null==search ) {
			mIsVerified = false;
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
			showErrorDialogBox(
				"Missing Search Engine",
				"The host search engine has not been configued."
				);
		}
		// Missing pattern
		else if( null==tmpMarkers || tmpMarkers.isEmpty() ) {
			mIsVerified = false;
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
			showErrorDialogBox(
				"Missing HTML Pattern",
				"There is no HTML pattern for the placement of Suggestions\n"
				+ "on the Patterns tab."
				);
		}
		// no search field
		else if( null==search.getQueryField() ) {
			mIsVerified = false;
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
			showErrorDialogBox(
				"Missing Search Field",
				"There is no CGI search field configured for the\n"
				+ "existing host search engine."
				);
		}
		// License issue
		else if( ! tmpConfig.getLicIsAllGood() ) {
			mIsVerified = false;
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
			showErrorDialogBox(
				"License is Expiring",
				"The configured license is expiring.\n"
				+ "Some functionality will be disabled.\n\n"
				+ "Please contact sales@ideaeng.com"
				);
		}
		// Logging issue
		else if( null==tmpConfig.getSearchLogger() ) {
			mIsVerified = false;
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
			showErrorDialogBox(
				"Logging Not Enabled",
				"Althought the SearchTrack Server can run without logging\n"
				+ "search activity to a database, it is not advised."
				);
		}
		// Database issue
		else if( null==tmpConfig.getDBConfig() ) {
			mIsVerified = false;
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
			showErrorDialogBox(
				"No Database",
				"Althought the SearchTrack Server can run without a database\n"				+ "it's functionality is severly limited.\n\n"
				+ "Features such as search logging and reporting and the\n"
				+ "Administration UI will not be available."
				);
		}
		else {
			mIsVerified = true;
			mMainFrame.setCursor( Cursor.getDefaultCursor() );

			if( inShowIfOK )
				JOptionPane.showMessageDialog( mMainFrame,
					"This configuration appears to be correct.",
					"Configuration Confirmed",
					JOptionPane.INFORMATION_MESSAGE);
		}
		return mIsVerified;

	}


	void save() {
		final String kFName = "save";

		copySwingValuesToJdomTree();

		if( ! mIsVerified ) {
			int answer = showYesNoCancelDialog(
					"Confirm Verify Before Save",
					"This configuration has not been checked."
					+ ( ! mIsModified ? "\n(although it has not been modified either)" : "" )
					+ "\n\n"
					+ "Verify before saving ?"
					);

			// Yes
			if( 0 == answer ) {
				// OK, check it
				if( ! verifyAll(true, false) ) {
					if( !
						showYesNoDialog(
							"Confirm Save Anyway",
							"The configuraiton check failed."
							+ "\n\nDo you still want to save it to disk ?"
							)
					) {
						return;
					}
				}
			}
			// Cancel
			else if( 2 == answer )
				return;
			// else No, don't verify, just fall on through
		}

		File saveFile = new File(mSaveFileName);
		// If the parent dir(s) doesn't exist, offer to create it
		File parentDir = saveFile.getParentFile();
		if( null!=parentDir && ! parentDir.exists() ) {
			int answer = showYesNoCancelDialog(
					"Confirmation",
					"The destination direcotry \"" + parentDir.getAbsolutePath().toString()
					+ "\" doesn't exist.\n\nCreate this directory ?"
					);
			// Yes
			if( 0==answer )
			{
				try {
					if( ! parentDir.mkdirs() ) {
						showErrorDialogBox(
							"Create Directory Failed",
							"Failed to create directory \"" + parentDir.getAbsolutePath().toString() + "\""
							+ "\n\nConfiguration not saved."
							);
						return;
					}
				}
				catch( Exception e ) {
					showErrorDialogBox(
						"Create Directory Failed",
						"Failed to create directory \"" + parentDir.getAbsolutePath().toString() + "\""
						+ "\n\nError:\n"
						+ NIEUtil.longStringChopper( ""+e, 80, "\n" )
						+ "\n\nConfiguration not saved."
						);
					return;
				}
			}
			// Cancel
			else if( 2==answer )
				return;
			// else No, so just keep on going through here
		}

		// Before we write it out, clean up a couple things added to the
		// tree by the SnConfig consctructor; they are useful status things to
		// have at runtine, but not appropriate for permanent storage
		// See nie.sn.SearchTuningConfig.storeStatusInfoIntoConfigTree()
		String [] badTagsAry = new String [] {
			"_start_time",
			"_version", "_version_and_config",
			"_config_uri", "_full_config_uri"
			};
		for( int i=0; i<badTagsAry.length; i++ ) {
			mConfigTree.getJdomElement().removeAttribute( badTagsAry[i] );
		}


		// Since writing out is a destructive operation, clone the tree

		// JDOMHelper.writeToFile( mConfigTree.getJdomElement(), mSaveFileName, false );
		try {
			JDOMHelper.writeToFileWithIncludes(
				mConfigTree.getJdomElement(), saveFile, false
				);
		}
		catch( JDOMHelperException e ) {
			errorMsg( kFName, "Error writing file \"" + mSaveFileName + "\" Error: " + e );

			showLastRunlogErrorAsDialogBox(
				"Problem Saving File",
				"Was NOT able to save the configuration."
				);

			return;
		}

		mIsModified = false;

		System.exit(0);
	}

	void save2() {
		final String kFName = "save2";

		mMainFrame.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );
		copySwingValuesToJdomTree();
		mMainFrame.setCursor( Cursor.getDefaultCursor() );

		if( ! mIsVerified ) {
			int answer = showYesNoCancelDialog(
					"Confirm Verify Before Save",
					"This configuration has not been checked."
					+ ( ! mIsModified ? "\n(although it has not been modified either)" : "" )
					+ "\n\n"
					+ "Verify before saving ?"
					);

			// Yes
			if( 0 == answer ) {
				// OK, check it
				if( ! verifyAll(true, false) ) {
					if( !
						showYesNoDialog(
							"Confirm Save Anyway",
							"The configuraiton check failed."
							+ "\n\nDo you still want to save it to disk ?"
							)
					) {
						return;
					}
				}
			}
			// Cancel
			else if( 2 == answer )
				return;
			// else No, don't verify, just fall on through
		}

		File saveFile = new File(mSaveFileName);
		// If the parent dir(s) doesn't exist, offer to create it
		File parentDir = saveFile.getParentFile();
		if( null!=parentDir && ! parentDir.exists() ) {
			int answer = showYesNoCancelDialog(
					"Confirmation",
					"The destination direcotry \"" + parentDir.getAbsolutePath().toString()
					+ "\" doesn't exist.\n\nCreate this directory ?"
					);
			// Yes
			if( 0==answer )
			{
				try {
					if( ! parentDir.mkdirs() ) {
						showErrorDialogBox(
							"Create Directory Failed",
							"Failed to create directory \"" + parentDir.getAbsolutePath().toString() + "\""
							+ "\n\nConfiguration not saved."
							);
						return;
					}
				}
				catch( Exception e ) {
					showErrorDialogBox(
						"Create Directory Failed",
						"Failed to create directory \"" + parentDir.getAbsolutePath().toString() + "\""
						+ "\n\nError:\n"
						+ NIEUtil.longStringChopper( ""+e, 80, "\n" )
						+ "\n\nConfiguration not saved."
						);
					return;
				}
			}
			// Cancel
			else if( 2==answer )
				return;
			// else No, so just keep on going through here
		}

		mMainFrame.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );

		// Before we write it out, clean up a couple things added to the
		// tree by the SnConfig consctructor; they are useful status things to
		// have at runtine, but not appropriate for permanent storage
		// See nie.sn.SearchTuningConfig.storeStatusInfoIntoConfigTree()
		String [] badTagsAry = new String [] {
			"_start_time",
			"_version", "_version_and_config",
			"_config_uri", "_full_config_uri"
			};
		for( int i=0; i<badTagsAry.length; i++ ) {
			mConfigTree.getJdomElement().removeAttribute( badTagsAry[i] );
		}

		//	statusMsg( kFName,
		//		mConfigTree.JDOMToString( true )
		//		);

		boolean wasSaved = false;
		// JDOMHelper.writeToFile( mConfigTree.getJdomElement(), mSaveFileName, false );
		try {
			JDOMHelper.writeToFileWithIncludes(
				mConfigTree.getJdomElement(), saveFile, false
				);
			wasSaved = true;
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
		}
		catch( JDOMHelperException e ) {
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
			errorMsg( kFName, "Error writing file \"" + mSaveFileName + "\" Error: " + e );

			showLastRunlogErrorAsDialogBox(
				"Problem Saving File",
				"Was NOT able to save the configuration."
				);

			e.printStackTrace( System.err );

			return;
		}
		mMainFrame.setCursor( Cursor.getDefaultCursor() );

		if( wasSaved ) {
			mIsModified = false;

			int answer = showYesNoCancelDialog(
					"Refresh Server",
					"Refresh Server:"
					+ "\n\n"
					+ "The Server Configuration has been saved to disk."
					+ "\n\n"
					+ "IF the server is running, would you like to send it a"
					+ "\nRefresh command, so it will reflect this new configuration?"
					+ "\n\n"
					+ "Refresh Server ?"
					);

			// Yes
			if( 0 == answer )
			{
				doRefreshWithDialogs();
//				// OK, check it
//				String errorMsg = doRefresh();
//				mMainFrame.setCursor( Cursor.getDefaultCursor() );
//				if( null!=errorMsg ) {
//					showErrorDialogBox(
//						"Problem with Refresh",
//						"The Refresh command didn't seem to work.\n"
//						+ "\n"
//						+ "Possible Reasons:\n"
//						+ "* Perhaps the server isn't running?\n"
//						+ "   If this is a brand new installation it probably hasn't been started yet.\n"
//						+ "* Did you change the machine name or port number?\n"
//						+ "   The process will be running with the OLD settings.\n"
//						+ "   Please KILL and then RESTART the server; refresh will not work.\n"
//						+ "* Did you change the Administration Password?\n"
//						+ "   The process will be running with the OLD password.\n"
//						+ "   Please do a manual refresh with the old password from the Admin UI.\n"
//						+ "\n"
//						+ "Error Was:\n"
//						+ NIEUtil.longStringChopper( errorMsg, 80, "\n" )
//						);
//				}
//				// It worked
//				else {
//					JOptionPane.showMessageDialog( mMainFrame,
//						"The Refresh command seemed to complete successfully.",
//						"Refresh Confirmed",
//						JOptionPane.INFORMATION_MESSAGE);
//				}
			}
			// Cancel
			else if( 2 == answer )
				return;
			// else No, don't verify, just fall on through

		}

		// System.exit(0);
	}




	void quit()
	{
		if( mIsModified ) {
			if( showYesNoDialog(
					"Confirm Exit",
					"Exit without Saving ?"
					)
			) {
				System.exit(0);
			}
		}
		// Don't pester them if nothing has changed
		else {
			System.exit(0);
		}

	}

	void doRefreshWithDialogs()
	{
		String errorMsg = doRefresh();
		mMainFrame.setCursor( Cursor.getDefaultCursor() );
		if( null!=errorMsg ) {
			showErrorDialogBox(
				"Problem with Refresh",
				"The Refresh command didn't seem to work.\n"
				+ "\n"
				+ "Possible Reasons:\n"
				+ "* Perhaps the server isn't running?\n"
				+ "   If this is a brand new installation it probably hasn't been started yet.\n"
				+ "* Did you change the machine name or port number?\n"
				+ "   The process will be running with the OLD settings.\n"
				+ "   Please KILL and then RESTART the server; refresh will not work.\n"
				+ "* Did you change the Administration Password?\n"
				+ "   The process will be running with the OLD password.\n"
				+ "   Please do a manual refresh with the old password from the Admin UI.\n"
				+ "\n"
				+ "Error Was:\n"
				+ NIEUtil.longStringChopper( errorMsg, 80, "\n" )
				);
		}
		// It worked
		else {
			JOptionPane.showMessageDialog( mMainFrame,
				"The Refresh command seemed to complete successfully.",
				"Refresh Confirmed",
				JOptionPane.INFORMATION_MESSAGE);
		}
	}
		
	String doRefresh() {
		mMainFrame.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );

		String outMsg = null;

		String serverURL = getSwingFormValueTrimOrNull( "nie_server" );
		if( null==serverURL ) {
			return "SearchTrack Server URL and port not set";
		}
		String adminPassword = getSwingFormValueTrimOrNull( "admin_password" );
		if( null==adminPassword ) {
			return "Administration password not set";
		}

		String refreshURL = serverURL;
		if( ! refreshURL.endsWith("/") )
			refreshURL += "/";
		// July 2008 stop passing around plaintext passwords
		String token = nie.sn.SearchTuningConfig.passwordToKeyOrNull( adminPassword );
		refreshURL +=
			  "?" + nie.sn.SnRequestHandler.NIE_CONTEXT_REQUEST_FIELD
			+ "=" + nie.sn.SnRequestHandler.SN_CONTEXT_ADMIN
			+ "&" + nie.sn.SnRequestHandler.ADMIN_CONTEXT_REQUEST_FIELD
			+ "=" + nie.sn.SnRequestHandler.ADMIN_CONTEXT_REFRESH
			// + "&" + nie.sn.SnRequestHandler.ADMIN_CGI_PWD_FIELD
			// + "=" + adminPassword
			// July 2008 stop passing around plaintext passwords
			+ "&" + nie.sn.SnRequestHandler.ADMIN_CGI_PWD_OBSCURED_FIELD
			+ "=" + token
			;

		// return refreshURL;

		String contents = null;
		try {
			contents = NIEUtil.fetchURIContentsChar( refreshURL );
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
			// return contents;
			return null;
		}
		catch( Exception e ) {
			mMainFrame.setCursor( Cursor.getDefaultCursor() );
			return "Error: " + e;
		}

		/***
		try {
			nie.sn.AdminLink refreshLink = new nie.sn.AdminLink(
				getMainConfig(),
				nie.sn.SnRequestHandler.ADMIN_CONTEXT_REFRESH,
				"Refresh",
				"Refresh/Re-Read Server Config (confirmation in new window)",
				null,
				null,
				null
				);
			Element refreshLinkElem = refreshLink.generateRichLink(
				inRequestInfo,
				nie.sn.SnRequestHandler.ADMIN_CONTEXT_REFRESH,
				null, null, null
				);
		}
		catch( Exception e ) {
			outMsg = "Error linking to Server: " + e
		}
		***/



		// mMainFrame.setCursor( Cursor.DEFAULT_CURSOR );
		// return outMsg;
	}

	/***
	long vlb()
		throws Exception
	{
		final String kFName = "vlb";
		final String kExTag = kClassName + '.' + kFName + ": ";
		try {
			return vl();
		}
		catch( Exception e ) {
			errorMsg( kFName )
		}
	}
	***/

	long vl()
		throws Exception
	{
		final String kFName = "vl";
		// final String kExTag = kClassName + '.' + kFName + ": ";
		final String kExTag = "";

		final String msg = "Bad license field: ";
		StringBuffer buff = new StringBuffer();
		String r = getSwingFormValueTrimOrNull( "nie_lic_rev" );
		if( null==r || ! r.equals("1") ) throw new Exception( kExTag + msg + "revision" + ": must be \"1\"" );
		buff.append(r).append('-');
		String lCo = getSwingFormValueTrimOrNull( "nie_lic_company" );
		if( null==lCo ) throw new Exception( kExTag + msg + '(' + 1 + ") " + "company" + ": missing" );
		int ccnt = 0;
		for( int i=0; i<lCo.length(); i++ ) {
			char c = lCo.charAt( i );
			if( (c>='a' && c<='z') || (c>='0' && c<='9') ) {
				buff.append(c);
				ccnt++;
			}
			else if( c>='A' && c<='Z' ) {
				buff.append( Character.toLowerCase(c) );
				ccnt++;
			}
		}
		if( ccnt<3 ) throw new Exception( kExTag + msg + '(' + 2 + ") " + "company" + ": name too short" );
		String lStStr = getSwingFormValueTrimOrNull( "nie_lic_start_date" );
		Date lSt = null;
		// if( null==fStStr ) throw new SearchTuningConfigException( kExTag + msg + '(' + 1 + ") " + LST );
		if( null!=lStStr ) {
			try {
				DateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy z");
				if( ! lStStr.toLowerCase().endsWith(" gmt") )
					lStStr += " GMT";
				lSt = fmt.parse(lStStr);
			} catch (ParseException e1) {
				throw new Exception( kExTag + msg + '(' + 2 + ") " + "start_date" + ": \"" + lStStr + "\" Format Erorr: " + e1 );
			}
			buff.append('-').append( lSt.getTime() );
		}
		else {
			buff.append("-nst");
		}
		String lNdStr = getSwingFormValueTrimOrNull( "nie_lic_end_date" );
		if( null==lNdStr ) throw new Exception( kExTag + msg + '(' + 1 + ") " + "end_date" + ": missing" );
		Date lNd = null;
		try {
			DateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy z");
			if( ! lNdStr.toLowerCase().endsWith(" gmt") )
				lNdStr += " GMT";
			lNd = fmt.parse(lNdStr);
		} catch (ParseException e2) {
			throw new Exception( kExTag + msg + '(' + 2 + ") " + "end_date" + ": \"" + lNdStr + "\" Format Erorr: " + e2 );
		}
		buff.append('-').append( lNd.getTime() );

		// Some sanity checks on the dates they've given us
		if( null!=lSt && lSt.getTime() >= lNd.getTime() )
			throw new Exception( kExTag + msg + '(' + 3 + ") Dates: " + "start_date" + ">=" + "end_date" );
		// Some warnings
		final long MSID = 1000 * 3600 * 24;
		if( lNd.getTime() < (new Date()).getTime() )
			throw new Exception( kExTag + msg + '(' + 3 + ") " + "end_date" + ": \"" + lNdStr + "\" has passed." );
			// System.err.println( "ERROR: The end date has already passed!" );
		else if( lNd.getTime() < (new Date()).getTime() + MSID * 32 )
			System.err.println( "Warning: The end date will expire in less than a month!" );
		else if( lNd.getTime() > (new Date()).getTime() + MSID * 455 )
			System.err.println( "Warning: The end date expires in way more than one year!" );




		String lSrv = getSwingFormValueTrimOrNull( "nie_lic_server" );
		buff.append('-');
		if( null!=lSrv ) {
			ccnt = 0;
			for( int i=0; i<lSrv.length(); i++ ) {
				char c = lSrv.charAt( i );
				if( (c>='a' && c<='z') || (c>='0' && c<='9') ) {
					buff.append(c);
					ccnt++;
				}
				else if( c>='A' && c<='Z' ) {
					buff.append( Character.toLowerCase(c) );
					ccnt++;
				}
			}
			if( ccnt<1 ) throw new Exception( kExTag + msg + '(' + 2 + ") " + "server" + ": invalid name" );
		}
		else {
			buff.append( "ns" );
		}
		buff.append('-');
		int sce1 = 1; int sce2 = 1;
		for( int i=0; i<20; i++ ) {
			buff.append(sce1);
			int tmp = sce2; sce2 += sce1; sce1 = tmp;
		}

		final int kyln = 4 * 5;
		String xky = getSwingFormValueTrimOrNull( "nie_lic_key" );
		xky = NIEUtil.trimmedStringOrNull( xky );
		if( null==xky ) throw new Exception( kExTag + msg + '(' + 1 + "): " + "key" + ": missing" );
		StringBuffer buff2 = new StringBuffer();
		for( int i=0; i<xky.length(); i++ ) {
			char c = xky.charAt( i );
			if( (c>='a' && c<='f') || (c>='0' && c<='9') ) {
				buff2.append(c);
			}
			else if( c>='A' && c<='F' ) {
				buff2.append( Character.toLowerCase(c) );
			}
		}
		if( buff2.length() != kyln ) throw new Exception( kExTag + msg + '(' + 2 + "): " + "key" + ": wrong length" );
		String cky2 = null;
		try {
			Chap cp = new Chap();
			int [] cky1 = cp.sign( new ByteArrayInputStream( (new String(buff)).getBytes()) );
			cky2 = cp.md5string(cky1);
		}
		catch( IOException e ) {
			throw new Exception( kExTag + msg + '(' + 3 + "): " + "key" + ": " + e );
		}
		final int st = 7;
		if( null==cky2 || cky2.length() < (st + kyln) )
			throw new Exception( kExTag + msg + '(' + 4 + "): " + "key" + ": " + st + '/' + kyln + '/' + ( null==cky2 ? -1 : cky2.length() ) );
		cky2 = cky2.substring( st-1, st+kyln-1 );

		// statusMsg( kFName, "cky2=\"" + cky2 + "\" (" + cky2.length() + ')' );

		if( ! cky2.equals( new String(buff2) ) )
			throw new Exception( kExTag + msg + '(' + 5 + "): " + "key" + ": invalid: " + (null!=lNd ? lNd.getTime() : -1) );

		return null!=lNd ? lNd.getTime() : -1L ;

	}

	private static void __sep__Buttons_and_Actions__() {}
	//////////////////////////////////////////////////////////

	// DetectSearchSettings
	// x ClearAndDetectSearchSettings
	// RunTestSearches
	// x ClearAndRetrySearches
	void renderInlineButton(
			JPanel inPanel, Element inFieldElem, String inFieldName, String optValue,
			GridBagConstraints inGbc
		) {
			final String kFName = "renderInlineButton";

			String buttonLabel = inFieldElem.getTextTrim();
			buttonLabel = NIEUtil.trimmedStringOrNull( buttonLabel );
			buttonLabel = (null!=buttonLabel) ? buttonLabel : "Button";
			JButton myButton = new JButton( buttonLabel );

			// Add an event handler, if specified
			String clickClassName = JDOMHelper.getStringFromAttributeTrimOrNull(
					inFieldElem, EVENT_CLASS_ATTR ); // "click_event_class"
			if( null!=clickClassName ) {

				// Class clickClass = Class.forName( clickClassName );
				final String kPrefix = "nie.config_ui." + kClassName + "$Event_";
				String fullName = kPrefix + clickClassName;
				try {

					debugMsg( kFName, "Looking for '" + fullName + "'" );
					Class clickClass = Class.forName( fullName );
					// ActionListener listener = (ActionListener) clickClass.newInstance();
					// Object obj = clickClass.newInstance();
					// Non static member classes do not really use
					// const(), actually it's const( ParentClass that )
					Class [] constrSig = new Class [] {
						nie.config_ui.Configurator2.class
						// Object.class
					};

					if( shouldDoTraceMsg(kFName) ) {
						// Constructor [] constrs = clickClass.getConstructors();
						Constructor [] constrs = clickClass.getDeclaredConstructors();
						traceMsg( kFName, "Examining " + constrs.length + " constructors" );
						for( int i=0; i<constrs.length; i++ ) {
							Constructor tmpConstr = constrs[i];
							Class [] types = tmpConstr.getParameterTypes();
							traceMsg( kFName,
								"Const # " + (i+1) + " has " + types.length + " args."
								);
							for( int j=0; j<types.length; j++ ) {
								Class currType = types[j];
								statusMsg( kFName,
									"\tArg # " + (j+1) + " is " + currType.getName()
									);
							}
						}
					}

					// throws NoSuchMethodException
					// Constructor constr = clickClass.getConstructor( constrSig );
					Constructor constr = clickClass.getDeclaredConstructor( constrSig );
					Object [] parms = new Object[] { this };
					Object obj = constr.newInstance( parms );

					ActionListener listener = (ActionListener) obj;
					myButton.addActionListener(
						listener
						);

					// Event_VerifyDBConn obj = new Event_VerifyDBConn();
					// ActionListener obj = new Event_VerifyDBConn();
					// 	statusMsg( kFName,
					// 	"full class is " + obj.getClass().getName()
					// 	);
					// Gives
					// "nie.config_ui.Configurator2$Event_VerifyDBConn"

					//	jb.addActionListener(new ActionListener() {
					//		public void actionPerformed(ActionEvent e) {
					//			MainWindow.this.toggleCompactView();
					//		}
					//	});

					//this.favoriteMenu.addActionListener(new ActionListener() {
					//			public void actionPerformed(ActionEvent e) {
					//				//if (e.getActionCommand() != "comboBoxChanged")
					//				//	 return;
					//				if (MainWindow.this.ignoreFavoriteMenu)
					//					return;

				}
				catch( Throwable t ) {
					errorMsg( kFName,
						"Error wiring up button '" + buttonLabel + "'"
						+ " which had attribute " + EVENT_CLASS_ATTR + "=\"" + clickClassName + '"'
						+ " (" + fullName + ')'
						+ " Error: " + t
						);
				}


			}
			// Else no class name given
			else {
				warningMsg( kFName,
					"No event class associated with button \"" + buttonLabel + "\""
					+ ", missing attribute '" + EVENT_CLASS_ATTR + "'"
					+ "; nothing will happen when the button is clicked."
					+ " Button def = " + JDOMHelper.JDOMToString( inFieldElem, true )
					);
			}

			inPanel.add( myButton, inGbc );
			// mNameToSwingWidget.put( shortName, myButton );
		}

	
	JPanel renderBottomButtons()
	{
		// The main Submit button
		JPanel buttonPanel = new JPanel();
		// buttonPanel.setLayout( new GridLayout(3,0) );
		buttonPanel.setLayout( new GridLayout(4,0) );

		// mMainFrame.getContentPane().add( buttonPanel, BorderLayout.SOUTH);

		JButton jbv = new JButton("Verify All");
		jbv.setPreferredSize( new Dimension(BOTTOM_BUTTON_WIDTH,20) );
		jbv.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator2.this.verifyAll();
			}
		});
		// mMainFrame.getContentPane().add( jb, BorderLayout.SOUTH);
		buttonPanel.add( jbv ); //, BorderLayout.CENTER);

		/***
		JButton jbs = new JButton("Save and Exit");
		jbs.setPreferredSize( new Dimension(BOTTOM_BUTTON_WIDTH,20) );
		jbs.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator.this.save();
			}
		});
		// mMainFrame.getContentPane().add( jb, BorderLayout.SOUTH);
		buttonPanel.add( jbs, BorderLayout.CENTER);
		***/

		// JButton jbs = new JButton("Save and Refresh");
		JButton jbs = new JButton("Save ...");
		jbs.setPreferredSize( new Dimension(BOTTOM_BUTTON_WIDTH,20) );
		jbs.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator2.this.save2();
			}
		});
		// mMainFrame.getContentPane().add( jb, BorderLayout.SOUTH);
		buttonPanel.add( jbs ); // , BorderLayout.CENTER);

		JButton jbr = new JButton("Refresh Server (if running)");
		jbr.setPreferredSize( new Dimension(BOTTOM_BUTTON_WIDTH,20) );
		jbr.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator2.this.doRefreshWithDialogs();
			}
		});
		// mMainFrame.getContentPane().add( jb, BorderLayout.SOUTH);
		buttonPanel.add( jbr ); // , BorderLayout.CENTER);

		/***
		// JButton jbc = new JButton("Cancel");
		JButton jbc = new JButton("Exit without Saving");
		jbc.setPreferredSize( new Dimension(BOTTOM_BUTTON_WIDTH,20) );
		jbc.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator.this.quit();
			}
		});
		// mMainFrame.getContentPane().add( jb, BorderLayout.SOUTH);
		buttonPanel.add( jbc, BorderLayout.EAST);
		***/

		// JButton jbc = new JButton("Cancel");
		JButton jbc = new JButton("Exit");
		jbc.setPreferredSize( new Dimension(BOTTOM_BUTTON_WIDTH,20) );
		jbc.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator2.this.quit();
			}
		});
		// mMainFrame.getContentPane().add( jb, BorderLayout.SOUTH);
		buttonPanel.add( jbc ); // , BorderLayout.EAST);

		return buttonPanel;
	}


	public void detectSearchSettings( boolean inClearFieldsFirst ) {
		final String kFName = "detectSearchSettings";
		if( inClearFieldsFirst )
			clearSearchEngineSwingFields();
		String searchFormURL = getSwingFormValueTrimOrNull( "search_form_url" );
		if( null==searchFormURL || searchFormURL.equals("http://") ) {

			showErrorDialogBox(
				"Missing URL",
				"No URL was given in the \"Search Form URL\" field; nothing to do."
				);

		}
		// Else they did give us a URL
		else {
			// Add a url prefix if they forgot
			if( searchFormURL.indexOf("//") < 0 )
				searchFormURL = "http://" + searchFormURL;
			debugMsg( kFName, "Will check URL " + searchFormURL );
			PageInfo searchPage = null;
			try {
				searchPage = new PageInfo( searchFormURL );

// Element pageElem = searchPage.getPageElem();
// JDOMHelper.writeToFile( pageElem, "tmp.xhtml", false );

				// find any forms on the page
				java.util.List forms = searchPage.findForms();
				int whichForm = -1;
				// If no forms, complain
				if( null==forms || forms.isEmpty() ) {
					showErrorDialogBox(
						"No Forms Detected",
						"No HTML forms were found on this page; nothing to do."
						);
				}
				// Else if one form, just use it
				else if( 1 == forms.size() ) {
					whichForm = 1;
				}
				// Else if more than one form
				// so we need to ask them
				else {
					StringBuffer buff = new StringBuffer();
					buff.append( "Multiple forms found on this page.\n" );
					buff.append( "Please choose the correct Search Form:\n\n" );

					String[] buttonLables = new String[ forms.size() + 1 ];
					buttonLables[0] = "Cancel";
					for( int i=0; i<forms.size() ; i++ ) {
						buttonLables[i+1] = "Form # " + (i+1);
						FormInfo form = (FormInfo) forms.get(i);
						buff.append( "Form # " ).append( i+1 ).append('\n');
						String formText = form.getFormTreeTextOrNull();
						if( null!=formText ) {
						// if( false ) {
							buff.append('"').append(
								NIEUtil.longStringChopper(formText, 80, "\n")
								).append('"').append('\n');
						}
						else {
							Element formElem = form.getFormElem();
							if( null!=formElem ) {
								String tmpStr = JDOMHelper.JDOMToString( formElem, true );
								// Remove line wraps after 300 chars
								if( null!=tmpStr && tmpStr.length() > 300 )
									tmpStr = JDOMHelper.JDOMToString( formElem, false );
								// if( null!=tmpStr )
								//	statusMsg( "tmp", "legnth=" + tmpStr.length() );
								// Force truncation over 1000 chars
								if( null!=tmpStr && tmpStr.length() > 1024 )
									tmpStr = tmpStr.substring( 0, 1024 ) + "...";
								// cleanup \r chars left by jdom pretty formatter
								tmpStr = NIEUtil.replaceChars( tmpStr, '\r', '\000' );
								// Force to 100 chars wide wrapping
								tmpStr = NIEUtil.longStringChopper( tmpStr, 100, "\n" );
								if( null!=tmpStr )
									buff.append( tmpStr ).append('\n');
							}
						}
						buff.append( '\n' );
					}

					int n = JOptionPane.showOptionDialog( mMainFrame,
						new String( buff ),
						"Choose Form",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						buttonLables,
						buttonLables[0]
						);

					if( n>0 )
						whichForm = n;

				}	// End else more than one form

				debugMsg( kFName, "Picked form # " + whichForm );

				// If we do have a form
				if( whichForm > 0 ) {
					// get the form (zero-based list vs. one-based choice )
					FormInfo form = (FormInfo) forms.get( whichForm - 1 );
					// Element newSearchElem = form.generateSearchEngineConfigTree();
					if( null!=form )
						copyOverSearchConfigFields( form );
					else
						errorMsg( kFName, "Null form # " + whichForm );		
				}

			}
			catch( Throwable t ) {

				showErrorDialogBox(
					"Problem Detecting Settings",
					"Was not able to auto-detect search engine settings"
					+ "\nfrom the URL that was given.\n\nError:\n" + t.getMessage()
					);

			}
		}
	}



	void clearSearchEngineSwingFields() {
		clearSwingFormValue( SEARCH_URL_FIELD );
		clearSwingFormValue( SEARCH_VENDOR_FIELD );
		clearSwingFormValue( SEARCH_QUERY_FIELD );
		for( int i=1; i<=MAX_HIDDEN_FIELDS ; i++ ) {
			clearSwingFormValue( HIDDEN_FIELD_NAME_TEMPLATE + i );
			clearSwingFormValue( HIDDEN_VALUE_NAME_TEMPLATE + i );
		}
		for( int j=1; j<=MAX_OPTION_FIELDS ; j++ ) {
			// search_form_option_field_name_1
			clearSwingFormValue( OPTION_FIELD_NAME_TEMPLATE + "name_" + j );
			// search_form_option_field_desc_1
			clearSwingFormValue( OPTION_FIELD_NAME_TEMPLATE + "desc_" + j );
			for( int k=1; k<=MAX_OPTION_VALUES ; k++ ) {
				// search_form_option_field_1_value_1
				clearSwingFormValue( OPTION_FIELD_NAME_TEMPLATE + j + "_value_" + k );
				// search_form_option_field_1_value_1_desc
				clearSwingFormValue( OPTION_FIELD_NAME_TEMPLATE + j + "_value_" + k + "_desc" );
			}
		}
	}

	DBConfig checkDB( boolean inDoSuccess ) {
		DBConfig outConfig = Configurator2.this.formDbConfigFromSwingDataOrNull();
		//	System.err.println(
		//		"Connecting to database "
		//		+ (null!=tmpConfig ? "worked" : "did NOT work" )
		//		);

		if( null!=outConfig ) {
			if( inDoSuccess )
				JOptionPane.showMessageDialog( mMainFrame,
					"The database configuration information appears to be correct.",
					"Connection Confirmed",
					JOptionPane.INFORMATION_MESSAGE);
		}
		else {

			showLastRunlogErrorAsDialogBox(
				"Problem Connecting to Database",
				"Was NOT able to connect to the database."
				);

		}

		return outConfig;

	}


	// Look at the form and try to conjure up a database
	// config object
	DBConfig formDbConfigFromSwingDataOrNull() {
		final String kFName = "formDbConfigFromSwingDataOrNull";

		DBConfig outDB = null;
		try {
			outDB = new DBConfig(
				getSwingFormValueTrimOrNull( "db_vendor_tag" ),
				getSwingFormValueTrimOrNull( "db_server_name" ),
				getSwingFormValueTrimOrNull( "db_port" ),
				getSwingFormValueTrimOrNull( "db_database" ),
				getSwingFormValueTrimOrNull( "db_username" ),
				getSwingFormValueTrimOrNull( "db_password" )
				);
		}
		catch( DBConfigException e ) {
			outDB = null;
			errorMsg( kFName, "Error connecting to database: " + e );
		}

		return outDB;
	}

	private static void __sep__Event_Classes__() {}
	///////////////////////////////////////////////


	class Event_DetectSearchSettings implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			detectSearchSettings( false );
		}
	}
	class Event_ClearAndDetectSearchSettings implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			detectSearchSettings( true );
		}
	}


	class Event_VerifyDBConn implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// MainWindow.this.toggleCompactView();
			// System.err.println( "In Event_VerifyDBConn.actionPerformed" );
			checkDB( true );
		}
	}

	class Event_InitDB implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			DBConfig tmpDB = checkDB( false );
			if( null!=tmpDB ) {
				boolean isAllOK = tmpDB.verifyAllDBTables( false, true );

				if( isAllOK ) {
					JOptionPane.showMessageDialog( mMainFrame,
						"The database configuration information appears to be correct,  "
						+ "\nand all system tables have been verified and/or created.",
						"SearchTrack Tables Confirmed",
						JOptionPane.INFORMATION_MESSAGE);
				}
				// Else not OK
				else {

					showLastRunlogErrorAsDialogBox(
						"Problem Verifying SearchTrack Tables",
						"Was NOT able to verify and/or create all of our system tables."
						);

				}

			}
			// Else it failed and we already complained about it
		}
	}



	class Event_ClearLogs implements ActionListener {
		public void actionPerformed(ActionEvent e) {

			// If they said Yes
			if(
				showYesNoDialog(
					"Confirmation",
					"Permanently delete all existing search activity"
					+ " data from the log tables?"
					)
			) {
				DBConfig tmpDB = checkDB( false );
				if( null!=tmpDB ) {
					// boolean isAllOK = tmpDB.verifyAllDBTables( false, true );
					// tmpDB.nukeTables ...e;
					boolean ok2 = tmpDB.deleteAllRecordsFromTable( DBConfig.LOG_META_TABLE );
					boolean ok1 = tmpDB.deleteAllRecordsFromTable( DBConfig.LOG_TABLE );
	
	
					if( ok1 && ok2 ) {
						JOptionPane.showMessageDialog( mMainFrame,
							"Search activity tables have been emptied.",
							"Completed",
							JOptionPane.INFORMATION_MESSAGE);
					}
					// Else not OK
					else {
	
						showLastRunlogErrorAsDialogBox(
							"Problem Verifying SearchTrack Tables",
							"Was NOT able to search activity logs."
							+ " isOK1=" + ok1
							+ " isOK2=" + ok2
							);

					}
	
				}
				// Else it failed and we already complained about it
			}	// End if they said yes

		}
	}

	// verify license
	class Event_VL implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				long endTime = vl();

				// Display a nice message
				long now = (new java.util.Date()).getTime();
				long timeLeft = endTime - now;
				double dTimeLeft = (timeLeft / (double) (1000 * 3600 * 24) );
				// It's good THROUGH the entire ending day
				dTimeLeft += 1.0;
				double days = NIEUtil.formatDoubleToDisplayPrecision( dTimeLeft );
				String daysStr = "" + days;
				if( daysStr.endsWith(".0") && daysStr.length()>2 )
					daysStr = daysStr.substring( 0, daysStr.length()-2 );

				JOptionPane.showMessageDialog( mMainFrame,
					"The license information appears to be correct."
					+ "\n\nExpires in " + daysStr + " day" + (daysStr.equals("1") ? "" : "s" )
					+ " (midnight GMT)\n "
					,
					"License Confirmed",
					JOptionPane.INFORMATION_MESSAGE);
			}
			catch( Exception ex ) {
				// ex.printStackTrace( System.err );
				showErrorDialogBox(
					"License Error",
					"The license information is not valid:\n\n" + ex.getMessage()
					);
			}
		}
	}



	private static void __sep__Mid_Level_Operations__() {}
	//////////////////////////////////////////////////////////

	void copySwingValuesToJdomTree() {
		final String kFName = "copySwingValuesToJdomTree";
		boolean trace = shouldDoTraceMsg( kFName );
	
		infoMsg( kFName, "Will examine " + mFieldNameList.size() + " fields" );
	
		debugMsg( kFName, "mNameToConfigTreePath has " + mNameToConfigTreePath.size() + " entries" );

		// For each registered field
		for( Iterator it = mFieldNameList.iterator() ; it.hasNext() ; ) {
			String fieldName = (String) it.next();
			// If we have a path for it
			if( mNameToConfigTreePath.containsKey(fieldName) ) {
				String yPath = (String) mNameToConfigTreePath.get( fieldName );
				boolean isAttr = yPath.lastIndexOf( '@' ) > yPath.lastIndexOf( '/' );
				Element configElem = (Element) mNameToConfigTreeElem.get( fieldName );
				// JTextComponent widget = (JTextComponent) mNameToSwingWidget.get( fieldName );
				Object widget = mNameToSwingWidget.get( fieldName );
				String newValue = null;
				// ALL METHODS to think about when adding a type
				// * copySwingValuesToJdomTree
				// * clearSwingFormValue
				// * getSwingFormValue
				// * getSwingFormValueTrimOrNull
				if( widget instanceof JComboBox ) {
					JComboBox comboWidget = (JComboBox) widget;
					newValue = comboWidget.getSelectedItem().toString();
				}
				else if( widget instanceof JCheckBox ) {
					JCheckBox myBox = (JCheckBox) widget;
					boolean state = myBox.isSelected();
					newValue = state ? "TRUE" : "FALSE";
				}
				else if( widget instanceof JLabel ) {
					JLabel myLabel = (JLabel) widget;
					newValue = myLabel.getText();
				}
				else {
					JTextComponent textWidget = (JTextComponent) widget;
					newValue = textWidget.getText();
				}
				newValue = NIEUtil.trimmedStringOrNull( newValue );
				newValue = (null!=newValue) ? newValue : "";

				if(trace)
					traceMsg( kFName, NIEUtil.NL + '\t' + fieldName + " = \"" + newValue + "\"" );

				// statusMsg( kFName, fieldName + "=" + newValue );
				if( isAttr ) {
					// configElem.setAttribute( fieldName, newValue );
					int atAt = yPath.lastIndexOf( '@' );
					if( atAt < 0 || atAt == yPath.length()-1 )
						errorMsg( kFName, "Invalid attribute path (1) \"" + yPath + "\"" );
					else {
						String attrName = yPath.substring( atAt+1 );
						attrName = NIEUtil.trimmedStringOrNull( attrName );
						if( null==attrName )
							errorMsg( kFName, "Invalid attribute path (2) \"" + yPath + "\"" );
						else
							configElem.setAttribute( attrName, newValue );
					}
				}
				else
					JDOMHelper.updateSimpleTextToExistingOrNewPath(
						configElem, null, newValue
						);
	
			}
			// Else no path
			else {
				// If it's not special, it should have had a path
				if( ! isSpecialField(fieldName) )
					warningMsg( kFName, fieldName + " not in path hash" );
				else
					infoMsg( kFName, fieldName + " not in path hash" );
			}
	
		}
	}







	void copyOverSearchConfigFields( FormInfo inForm ) {
		final String kFName = "copyOverSearchConfigFields";
		if( null==inForm ) {
			errorMsg( kFName, "Null form info passed in, nothing to do." );
			return;
		}

		java.util.List checkItems = new Vector();

		// Search URL
		String fieldName = SEARCH_URL_FIELD;
		String tmpURL = getSwingFormValueTrimOrNull(fieldName);
		if( null==tmpURL || tmpURL.equals("http://") ) {
			String newSearchURL = inForm.getAbsoluteActionURL();
			if( null!=newSearchURL )
				setSwingFormValue( fieldName, newSearchURL );
			else
				checkItems.add( "Search Engine CGI URL not found (is required)" );
		}
		else
			statusMsg( kFName, "search_url: Not overwriting existing value." );

		// Search Method
		fieldName = SEARCH_METHOD_FIELD;
		if( null==getSwingFormValueTrimOrNull(fieldName) ) {
			String newSearchMethod = inForm.getMethod();
			if( null!=newSearchMethod )
				setSwingFormValue( fieldName, newSearchMethod );
		}
		else
			statusMsg( kFName, "search_method: Not overwriting existing value." );



		// Search vendor
		fieldName = SEARCH_VENDOR_FIELD;
		if( null==getSwingFormValueTrimOrNull(fieldName) ) {
			String newSearchVendor = inForm.guessSearchVendor();
			if( null!=newSearchVendor )
				setSwingFormValue( fieldName, newSearchVendor );
		}
		else
			statusMsg( kFName, "search_vendor: Not overwriting existing value." );



		// The primary search field
		// Check the value that might already be there
		fieldName = SEARCH_QUERY_FIELD;
		String tmpSearchField = getSwingFormValueTrimOrNull(fieldName);
		// If no value, go ahead and find it
		if( null==tmpSearchField || tmpSearchField.equals("query") ) {
			java.util.List textFields = inForm.getAllTextFieldNames();
			String textFieldName = null;
			// If only one option, use it
			if( null!=textFields && textFields.size()==1 ) {
				textFieldName = (String) textFields.get( 0 );
			}
			// If multiple options, let them pick
			else if( null!=textFields && textFields.size()>1 ) {
				String [] possibleValues = new String[ textFields.size()+1 ];
				java.util.List possibleValuesList = new Vector();
				final String kNoneValue = "(none)";
				possibleValues[0] = kNoneValue;
				possibleValuesList.add( possibleValues[0] );
				int optionCounter = 0;
				for( Iterator it=textFields.iterator(); it.hasNext() ; ) {
					String tmpName = (String) it.next();
					possibleValues[ optionCounter ] = tmpName;
				}
	
				Object selectedValue = JOptionPane.showInputDialog( mMainFrame,
					"Multiple text input fields were found.\n\n"
					+ "A search form needs to have a field where a user can type in their search.\n"
					+ "This form seems to have more than one such input box;\n"
					+ "please tell us which one is the PRIMARY search box.\n\n"
					,
					"Please Choose One",
					JOptionPane.QUESTION_MESSAGE,
					null,
					possibleValues,
					possibleValues[1]
					);
	
				statusMsg( kFName, "Selected " + selectedValue );
				String selectedValue2 = (null!=selectedValue) ? (String) selectedValue : null;
				if( null!=selectedValue2 && ! selectedValue2.equals(kNoneValue) )
					textFieldName = selectedValue2;
			}

			// Did we get something?
			if( null!=textFieldName ) {
				if( null!=textFieldName )
					setSwingFormValue( fieldName, textFieldName );
				else
					checkItems.add( "Search Terms Query Field not found (is required)" );
			}
		}
		/***
		if( null==tmpSearchField || tmpSearchField.equals("query") ) {
			String newSearchField = inForm.getPrimaryTextFieldName();
			// List textFields = getAllTextFieldNames()
			// TODO: Ask which one, if there's more than one
			if( null!=newSearchField )
				setSwingFormValue( fieldName, newSearchField );
			else
				checkItems.add( "Search Terms Query Field not found (is required)" );
		}
		else
			statusMsg( kFName, "search_field: Not overwriting existing value." );
		***/

		// The Hidden fields
		java.util.List hiddenFields = inForm.getAllHiddenFieldNames();
		int kMaxFieldCount = MAX_HIDDEN_FIELDS;
		String kNameTemplate = HIDDEN_FIELD_NAME_TEMPLATE;
		String kValueTemplate = HIDDEN_VALUE_NAME_TEMPLATE;
		// If there are any hidden fields
		if( null!=hiddenFields && ! hiddenFields.isEmpty() ) {
			int which = 0;
			Iterator hit = null;
			String hiddenName = null;
			// For each hidden field name
			for( hit = hiddenFields.iterator() ; hit.hasNext() ; ) {
				hiddenName = (String) hit.next();
				// String hiddenValue = inForm.getScalarCGIField( hiddenName );
				java.util.List hiddenValues = inForm.getMultivalueCGIField( hiddenName );
				// If there are any hidden values for this hidden field
				if( null!=hiddenValues && ! hiddenValues.isEmpty() ) {
					// For each value
					Iterator vit = null;
					String hiddenValue = null;
					for( vit=hiddenValues.iterator(); vit.hasNext() ; ) {
						which++;
						hiddenValue = (String) vit.next();
						// This shouldn't be possible
						if( null==hiddenValue )
							hiddenValue = "";
						// Keep trying to add values to an available slot
						while( true ) {
							String tmpFieldName = kNameTemplate + which;
							String tmpValueName = kValueTemplate + which;
							String tmp1 = getSwingFormValueTrimOrNull( tmpFieldName );
							String tmp2 = getSwingFormValueTrimOrNull( tmpValueName );
							// If both blank, go ahead and add these values
							if( null==tmp1 && null==tmp2 ) {
								setSwingFormValue( tmpFieldName, hiddenName );
								setSwingFormValue( tmpValueName, hiddenValue );
								hiddenValue = null;
								break;
							}
							// Else wasn't blank, try the next slot
							else {
								which++;
								// Bail if past end
								if( which > kMaxFieldCount )
									break;
							}
							// Back to top to try again if haven't broke out
						}
						// Escape if we're out of room
						if( which > kMaxFieldCount )
							break;
					}	// End for each hidden value for this field
					if( vit.hasNext() || null!=hiddenValue )
						checkItems.add( "May be missing values for hidden field \"" + hiddenName + "\"" );

				}	// End if there are hidden values for this field
				// Escape if we're out of room
				if( which > kMaxFieldCount )
					break;

				hiddenName = null;
			}	// End for each hidden field name

			if( hit.hasNext() || null!=hiddenName )
				checkItems.add( "Some hidden fields may not have been recorded: " + hiddenFields );
		}
		// Else no hidden fields
		else
			statusMsg( kFName, "No hidden fields." );

		// The Option fields
		java.util.List optionFields = inForm.getAllOptionFieldNames();
		String optionName = null;
		// If only one option, use it
		if( null!=optionFields && optionFields.size()==1 ) {
			optionName = (String) optionFields.get( 0 );
		}
		// If multiple options, let them pick
		else if( null!=optionFields && optionFields.size()>1 ) {
			String [] possibleValues = new String[ optionFields.size()+1 ];
			java.util.List possibleValuesList = new Vector();
			possibleValues[0] = "(none)";
			possibleValuesList.add( possibleValues[0] );
			int optionCounter = 0;
			for( Iterator it=optionFields.iterator(); it.hasNext() ; ) {
				String tmpName = (String) it.next();
				String tmpDesc = inForm.getScalarFieldDescription( tmpName );
				optionCounter++;
				// Will usually just have a name
				String tmpDesc2 = null;
				if( null==tmpDesc )
					tmpDesc2 = tmpName;
				else
					tmpDesc2 = tmpDesc + " (" + tmpName + ')';

				possibleValues[ optionCounter ] = tmpDesc2;
				possibleValuesList.add( tmpDesc2 );
			}
			// List possibleValuesList = new Vector( possibleValues );
			// ^^^ not sure this is ok before 1.4

			Object selectedValue = JOptionPane.showInputDialog( mMainFrame,
				"Multiple eligible Sub-Site fields were found.\nPlease choose one.\n\n"
				+ "If you want more than one, choose the primary one here and then\n"
				+ "edit the configuration manually to add the others.\n\n"
				,
				"Please Choose One",
				// JOptionPane.INFORMATION_MESSAGE,
				JOptionPane.QUESTION_MESSAGE,
				null,
				possibleValues,
				possibleValues[1]
				);

			statusMsg( kFName, "Selected " + selectedValue );
			if( null!=selectedValue && possibleValuesList.contains(selectedValue) ) {
				int atWhere = possibleValuesList.indexOf( selectedValue );
				if( atWhere > 0 ) {
					optionName = (String) optionFields.get( atWhere-1 );
				}
			}

		}

		// If there are any hidden fields
		// if( null!=optionFields && ! optionFields.isEmpty() ) {
		if( null!=optionName ) {
			int hiddenFieldCount = 0;
			// Iterator oit = null;
			// String optionName = null;
			String optionDesc = null;
			// For each hidden field name
			// for( oit = optionFields.iterator() ; oit.hasNext() ; ) {
			// oit = optionFields.iterator();
				hiddenFieldCount++;
				// optionName = (String) oit.next();
				optionDesc = inForm.getScalarFieldDescription( optionName );

				// search_form_option_field_name_1
				setSwingFormValue(
					OPTION_FIELD_NAME_TEMPLATE + "name_" + hiddenFieldCount,
					optionName
					);
				// search_form_option_field_desc_1
				if( null!=optionDesc )
					setSwingFormValue(
						OPTION_FIELD_NAME_TEMPLATE + "desc_" + hiddenFieldCount,
						optionDesc
						);


				// String hiddenValue = inForm.getScalarCGIField( hiddenName );
				java.util.List optionValues = inForm.getMultivalueCGIField( optionName );
				// If there are any hidden values for this hidden field
				if( null!=optionValues && ! optionValues.isEmpty() ) {
					int hiddenValueCount = 0;
					// For each value
					Iterator vit = null;
					String optionValue = null;
					String optionValueDesc = null;
					for( vit=optionValues.iterator(), hiddenValueCount=1 ;
							vit.hasNext() && hiddenValueCount<=MAX_OPTION_VALUES ;
							hiddenValueCount++
					) {
						optionValue = (String) vit.next();
						// This shouldn't be possible
						if( null==optionValue )
							optionValue = "";
						optionValueDesc = inForm.getScalarOptionDescription( optionName, optionValue );
	
						// search_form_option_field_1_value_1
						setSwingFormValue(
							OPTION_FIELD_NAME_TEMPLATE + hiddenFieldCount + "_value_"
							+ hiddenValueCount,
							optionValue
							);
						// search_form_option_field_1_value_1_desc
						if( null!=optionValueDesc )
							setSwingFormValue(
								OPTION_FIELD_NAME_TEMPLATE + hiddenFieldCount + "_value_"
								+ hiddenValueCount + "_desc",
								optionValueDesc
								);

					}	// End for each hidden value for this field
					if( vit.hasNext() )
						checkItems.add( "May be missing values for option field \"" + optionName + "\"" );

				}	// End if there are hidden values for this field

			// }	// End for each hidden field name

			// if( oit.hasNext() )
			//	checkItems.add( "Some option fields may not have been recorded: " + optionFields );
		}
		// Else no option
		else
			statusMsg( kFName, "No opiton fields." );

		statusMsg( kFName, "Check items: " + checkItems );

		if( ! checkItems.isEmpty() ) {
			StringBuffer buff = new StringBuffer();
			buff.append( "Some search engine fields may not be configured properly.\n" );
			buff.append( "You may need to manually edit the config file.\n\n");
			buff.append( "Specific Issue(s):\n\n");
			for( Iterator mit=checkItems.iterator(); mit.hasNext() ; ) {
				String msg = (String) mit.next();
				buff.append("* ").append( msg ).append('\n').append('\n');
			}
			buff.append(' ');
			showErrorDialogBox(
				"Search Engine Config Issues",
				new String( buff )
				);
		}

	}









	private static void __sep__Specific_Form_Widgets__() {}
	/////////////////////////////////////////////

	void _renderMenu()
	{
		JMenu fileMenu = new JMenu("File");
		// JMenuItem fileMenu = new JMenuItem("File");
		// fileMenu.setAccelerator(KeyStroke.getKeyStroke(
		//	KeyEvent.VK_F, ActionEvent.ALT_MASK)
		//	);
		fileMenu.setMnemonic(KeyEvent.VK_F);

		JMenuItem verifyMenu = new JMenuItem("Verify All");
		verifyMenu.setMnemonic(KeyEvent.VK_V);
		verifyMenu.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator2.this.verifyAll();
			}
		});
		fileMenu.add( verifyMenu );

		JMenuItem saveMenu = new JMenuItem("Save and Refresh");
		saveMenu.setMnemonic(KeyEvent.VK_S);
		saveMenu.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator2.this.save2();
			}
		});
		fileMenu.add( saveMenu );

		JMenuItem exitMenu = new JMenuItem("Exit");
		exitMenu.setMnemonic(KeyEvent.VK_X);
		exitMenu.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator2.this.quit();
			}
		});
		fileMenu.add( exitMenu );

		JMenuBar menuBar = new JMenuBar();
		menuBar.add( fileMenu );
		mMainFrame.setJMenuBar( menuBar );
	}


	// Display the label, if any
	String renderFieldLabelIfAny(
		JPanel inPanel, Element inFieldElem, String inType,
		int inRowNumber, GridBagConstraints inGbc
	) {
		final String kFName = "renderFieldLabelIfAny";
		String displayName = inFieldElem.getAttributeValue( "label" );
		displayName = nie.core.NIEUtil.trimmedStringOrNull( displayName );
		if( null!=displayName ) {
			if( null!=inType && inType.equalsIgnoreCase("vertical_spacer")) {
				errorMsg( kFName, "Ignoring label for vertical spacer widget, label=\"" + displayName + "\"");
			}
			else {
				JLabel jl = new JLabel( displayName + ": " );
				jl.setHorizontalAlignment( SwingConstants.RIGHT );
				jl.setPreferredSize( new Dimension( LABEL_WIDTH, 20 ) );
				// jl.setHorizontalAlignment(SwingConstants.EAST);
				// jl.setAlignmentX( 25 );
				inGbc.gridx = 0;
				inGbc.gridy = inRowNumber;
				inGbc.gridwidth = 1;
				inGbc.fill = GridBagConstraints.NONE;
				// frame.getContentPane().add(jl, gbc);
				inPanel.add( jl, inGbc );
			}
		}
		return displayName;
	}

	void renderVerticalSpacer(
		JPanel inPanel, Element inFieldElem, int inRowNumber, GridBagConstraints inGbc
	) {
		final String kFName = "renderVerticalSpacer";
		// inPanel.setBackground( Color.GREEN );
		
		// Display the label, if any
		String displayText = inFieldElem.getTextNormalize();
		displayText = NIEUtil.trimmedStringOrNull( displayText );
		int fontSize = JDOMHelper.getIntFromAttribute( inFieldElem, "font_size_increment", 1 );
		boolean isBold = JDOMHelper.getBooleanFromAttribute( inFieldElem, "is_bold", false );

		StringBuffer labelBuff = null;
		if( null!=displayText ) {
			boolean hasFont = fontSize != 0;
			labelBuff = new StringBuffer();
			labelBuff.append("<html>");

			if( hasFont ) {
				labelBuff.append( "<font " );
				if( fontSize != 0 ) {
					labelBuff.append( "size=\"" );
					if( fontSize > 0 )
						labelBuff.append( '+' );
					else if( fontSize < 0 )
						labelBuff.append( '-' );
					labelBuff.append( fontSize );
					labelBuff.append( '"' );
				}
				labelBuff.append( '>' );
			}

			if( isBold )
				labelBuff.append( "<b>" );

			labelBuff.append( NIEUtil.htmlEscapeString( displayText, true ) );

			if( isBold )
				labelBuff.append( "</b>" );
			if( hasFont )
				labelBuff.append( "</font>" );

			labelBuff.append("</html>");
		}

		JLabel jl = null;
		// If there is label text
		if( null!=labelBuff && labelBuff.length() > 0 ) {
			jl = new JLabel( new String(labelBuff) );

			jl.setOpaque(true);
			// jl.setBackground( new Color( 64, 64, 64 ) );
			int kGray = 184; // 172; //196; // 128;
			jl.setBackground( new Color( kGray, kGray, kGray ) );

			// jl.setBackround(Color.WHITE);
			// jl.setFont(new Font("Serif", Font.BOLD, 48));
			// LookAndFeel.installColors(selected, "TextField.selectionBackground", "TextField.selectionForeground");

			// statusMsg( kFName, "Adding " + new String(labelBuff) );
		}
		// Else no label text
		else {
			jl = new JLabel();
		}

		// jl.setBackground( Color.GREEN );
		
		jl.setHorizontalAlignment( SwingConstants.CENTER );
		jl.setPreferredSize( new Dimension(
				LABEL_WIDTH + INPUT_WIDTH,
				20 + 4*fontSize
			) );
		// jl.setHorizontalAlignment(SwingConstants.EAST);
		// jl.setAlignmentX( 25 );
		inGbc.gridx = 0;
		inGbc.gridy = inRowNumber;
		inGbc.gridwidth = 3; // 1;
		inGbc.fill = GridBagConstraints.NONE;
		// frame.getContentPane().add(jl, gbc);
		inPanel.add( jl, inGbc );

	}

	// Figure out which value to display, if any
	String getValueToUse( Element inFieldElem, String inFieldName, String inDisplayName,
		boolean inIsNew, String inType
	) {
		final String kFName = "getValueToUse";
		boolean debug = shouldDoDebugMsg( kFName );

		String defValue = inFieldElem.getAttributeValue( DEFAULT_ATTR );
		boolean isSpecial = JDOMHelper.getBooleanFromAttribute( inFieldElem, "is_special", false );
		String existingValue = null;
	
		// If we're editing an existing tree, lookup the value
		boolean isAttr = false;
		String yPath = null;
		Element existingElem = null;
		String elemAttrName = null;
		// Get the DATA form the ST config tree
		if( ! inIsNew
			&& (null==inType || ! (inType.equals("inline_button") || inType.equals("vertical_spacer")))
		) {		
			if( mNameToConfigTreePath.containsKey(inFieldName) ) {
				yPath = (String) mNameToConfigTreePath.get( inFieldName );
				isAttr = yPath.lastIndexOf( '@' ) > yPath.lastIndexOf( '/' );
				existingElem = (Element) mNameToConfigTreeElem.get( inFieldName );
	
				if( isAttr ) {
					int lastAt = yPath.lastIndexOf( '@' );
					if( lastAt < 0 || lastAt >= (yPath.length()-1) ) {
						errorMsg( kFName, "Invalid placement of @ at offset " + lastAt + " in yPath \"" + yPath + "\"; no value retrieved." );
					}
					else {
						elemAttrName = yPath.substring( lastAt+1 );
						existingValue = existingElem.getAttributeValue( elemAttrName );
						// statusMsg( kFName, elemAttrName + "=\"" + existingValue + "\"" );
					}
				}
				else
					existingValue = existingElem.getTextTrim();
	
				// statusMsg( kFName, NIEUtil.NL + '\t' + yPath + "=\"" + existingValue + "\"" );
		
			}
			else {
				if( ! isSpecial )
					warningMsg( kFName, "No existing element for \"" + inFieldName + "\"" );
			}
		}

		// The value we will use, if any
		String outValue = inIsNew ? defValue : existingValue;
		
		if(debug) debugMsg( kFName,
			"inIsNew=" + inIsNew
			+ ", isAttr=" + isAttr + NIEUtil.NL
			+ "inFieldName=\"" + inFieldName + "\""
			+ ", displayName=\"" + inDisplayName + "\"" + NIEUtil.NL
			+ "defValue=\"" + defValue + "\""
			+ ", existingValue=\"" + existingValue + "\""
			+ ", outValue=\"" + outValue + "\"" + NIEUtil.NL
			+ "yPath = \"" + yPath + "\""
			+ ", elemAttrName = \"" + elemAttrName + "\"" + NIEUtil.NL
			+ "Existing element = " + ( null!=existingElem ? JDOMHelper.JDOMToString( existingElem, true ) : "NULL" )
			);

		return outValue;
	}

	void renderDropDownWidget(
		JPanel inPanel, Element inFieldElem, String inFieldName, String optValue,
		GridBagConstraints inGbc
	) {
		final String kFName = "renderDropDownWidget";
		boolean trace = shouldDoTraceMsg( kFName );

		if(trace) traceMsg( kFName, "Start: opt value=\"" + optValue + "\"" );
		if( null==NIEUtil.trimmedStringOrNull(optValue) ) {
			optValue = inFieldElem.getAttributeValue( DEFAULT_ATTR );
			if(trace) traceMsg( kFName, "Using default value=\"" + optValue + "\"" );
		}

		// For each selection option
		java.util.List options = nie.core.JDOMHelper.findElementsByPath(
			inFieldElem, "option"
			);
		int optionCounter = 0;
		java.util.Vector tmpVect = new java.util.Vector();
		int valueAt = -1;
		// Add the values we know about
		for( Iterator oit = options.iterator() ; oit.hasNext() ; ) {
			Element optionElem = (Element) oit.next();
			optionCounter++;
			String option = optionElem.getTextNormalize();
			option = nie.core.NIEUtil.trimmedStringOrNull( option );
			// If there's an option, add it
			if( null!=option ) {
				tmpVect.add( option );
				// If it's our preferred value, make a note of it
				if( valueAt < 0 && null!=optValue && optValue.equalsIgnoreCase(option) ) {
					valueAt = tmpVect.size()-1;
					if(trace) traceMsg( kFName, "Found value in list at " + valueAt );
				}
			}
		}
		// Sanity check to make sure the value that was there is in the list
		// If there is a value we're we really have our heart set on and
		// it hasn't been found so far, add it!
		if( valueAt < 0 && null!=NIEUtil.trimmedStringOrNull(optValue) ) {
			tmpVect.add( optValue );
			valueAt = tmpVect.size()-1;
			if(trace) traceMsg( kFName, "Forced value at end, now at " + valueAt );
		}
		// need array, not list
		String [] optionAry = (String []) tmpVect.toArray( new String [1] );
		// The widget
		// JComboBox comboBox = new JComboBox(new String[] {"red", "green"});
		JComboBox comboBox = new JComboBox( optionAry );
		
		// And set the current item, if there was one
		if( optionAry.length > 0 )
			if( valueAt >= 0 )
				comboBox.setSelectedIndex( valueAt );
			else
				comboBox.setSelectedIndex( 0 );

		comboBox.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e) {
// statusMsg( "combo action:", "Event: " + e );
// statusMsg( "combo action", "event action = " + e.getActionCommand() );
// statusMsg( "combo action", "event param str = " + e.paramString() );
// statusMsg( "combo action", "event type = " + e.getID() );
				markAsModified();
			}
		});

		inPanel.add( comboBox, inGbc );
		mNameToSwingWidget.put( inFieldName, comboBox );
	}



	void _renderDropDownWidgetOBS(
		JPanel inPanel, Element inFieldElem, String inFieldName, String optValue,
		GridBagConstraints inGbc
	) {
		// For each selection option
		java.util.List options = nie.core.JDOMHelper.findElementsByPath(
			inFieldElem, "option"
			);
		int optionCounter = 0;
		java.util.Vector tmpVect = new java.util.Vector();
		for( Iterator oit = options.iterator() ; oit.hasNext() ; ) {
			Element optionElem = (Element) oit.next();
			optionCounter++;
			String option = optionElem.getTextNormalize();
			option = nie.core.NIEUtil.trimmedStringOrNull( option );
			if( null!=option )
				tmpVect.add( option );
		}
		String [] optionAry = (String []) tmpVect.toArray( new String [1] );
		int valueAt = -1;
		if( null!=optionAry && null!=optValue ) {
			for( int i=0; i<optionAry.length; i++ ) {
				if( optionAry[i] == optValue ) {
					valueAt = i;
					break;
				}
			}
		}
		// JComboBox comboBox = new JComboBox(new String[] {"red", "green"});
		JComboBox comboBox = new JComboBox( optionAry );
		if( optionAry.length > 0 )
			if( valueAt >= 0 )
				comboBox.setSelectedIndex( valueAt );
			else
				comboBox.setSelectedIndex( 0 );
		inPanel.add( comboBox, inGbc );
		mNameToSwingWidget.put( inFieldName, comboBox );
	}
	void renderTextAreaWidget(
		JPanel inPanel, Element inFieldElem, String inFieldName, String optValue,
		GridBagConstraints inGbc
	) {
		int numRows = nie.core.JDOMHelper.getIntFromAttribute( inFieldElem, "rows", TEXT_AREA_DEFAULT_ROWS );
		numRows++;	// Add one for when we get horiz scroll bar
		JTextArea bigBox = null;
		// if( inIsNew && null!=defValue ) {
		if( null!=optValue ) {
			bigBox = new JTextArea( optValue, numRows, TEXT_AREA_DEFAULT_CHAR_WIDTH );
		}
		else {
			bigBox = new JTextArea( numRows, TEXT_AREA_DEFAULT_CHAR_WIDTH );
		}
		/***
		bigBox.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e) {
				markModified();
			}
		});
		***/
		bigBox.getDocument().addDocumentListener( new DocumentListener() {
			public void changedUpdate( DocumentEvent e ) {
				// statusMsg( "input box action", "doc change Event: " + e );
				markAsModified();
			}
			public void removeUpdate( DocumentEvent e ) {
				// statusMsg( "input box action", "doc remove Event: " + e );
				markAsModified();
			}
			public void insertUpdate( DocumentEvent e ) {
				// statusMsg( "input box action", "doc insert Event: " + e );
				markAsModified();
			}
		});

		// bigBox.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5) );
		// p.add( bigBox, gbc );
		JScrollPane scroll = new JScrollPane( bigBox );
		inPanel.add( scroll, inGbc );
	
		mNameToSwingWidget.put( inFieldName, bigBox );
	}
	void renderCheckBoxWidget(
			JPanel inPanel, Element inFieldElem, String inFieldName, String optValue,
			GridBagConstraints inGbc
	) {
		final String kFName = "renderCheckBoxWidget";

		boolean val = NIEUtil.decodeBooleanString( optValue, false );
		JCheckBox littleBox = new JCheckBox();
		littleBox.setSelected( val );
		littleBox.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				// statusMsg( "check box action", "doc change Event: " + e );
				markAsModified();
			}
		});
		inPanel.add( littleBox, inGbc );
		mNameToSwingWidget.put( inFieldName, littleBox );
	}
	void renderTextBoxWidget(
		JPanel inPanel, Element inFieldElem, String inFieldName, String optValue,
		GridBagConstraints inGbc
	) {
		final String kFName = "renderTextBoxWidget";
		// statusMsg( kFName, inFieldName + "=\"" + optValue + "\"" );

		JTextField inputBox = null;
		if( null!=optValue ) {
			inputBox = new JTextField( optValue );
		}
		else {
			inputBox = new JTextField();
			// find element and add value
		}
		inputBox.setPreferredSize( new Dimension( INPUT_WIDTH,20 ) );
		// javax.swing.text.Document doc = inputBox.getDocument();
		inputBox.getDocument().addDocumentListener( new DocumentListener() {
			public void changedUpdate( DocumentEvent e ) {
				// statusMsg( "input box action", "doc change Event: " + e );
				markAsModified();
			}
			public void removeUpdate( DocumentEvent e ) {
				// statusMsg( "input box action", "doc remove Event: " + e );
				markAsModified();
			}
			public void insertUpdate( DocumentEvent e ) {
				// statusMsg( "input box action", "doc insert Event: " + e );
				markAsModified();
			}
		});
		/***
		inputBox.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e) {
statusMsg( "input box action:", "Event: " + e );

				markAsModified();
			}
		});
		***/
		inPanel.add( inputBox, inGbc );
		mNameToSwingWidget.put( inFieldName, inputBox );
	}

	void renderReadOnlyWidget(
		JPanel inPanel, Element inFieldElem, String inFieldName, String optValue,
		GridBagConstraints inGbc
	) {
		// JTextField inputBox = null;
		JLabel inputBox = null;

		if( null!=optValue ) {
			// inputBox = new JTextField( optValue );
			inputBox = new JLabel( optValue );
		}
		else {
			// inputBox = new JTextField();
			inputBox = new JLabel();
		}
		inputBox.setPreferredSize( new Dimension( INPUT_WIDTH,20 ) );
		inputBox.setHorizontalAlignment( SwingConstants.LEFT );

		inPanel.add( inputBox, inGbc );
		mNameToSwingWidget.put( inFieldName, inputBox );
	}





	public void _renderFormOBS( boolean inIsNew ) {
		final String kFName = "_renderFormOBS";
		boolean debug = shouldDoDebugMsg( kFName );
		// boolean debug = true;
	
		/*JFrame*/ mMainFrame = new JFrame();
	    mMainFrame.getContentPane().setLayout( new BorderLayout() );
		mMainFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		String mainTitle = mXmlForm.getStringFromAttributeTrimOrNull( "title" );
		mainTitle = (null==mainTitle) ? "Config Editor" : mainTitle;
		mMainFrame.setTitle( mainTitle );
	
		JTabbedPane tframe = new JTabbedPane();
		mMainFrame.getContentPane().add( tframe, BorderLayout.NORTH );
	
		GridBagConstraints gbc = new GridBagConstraints();
		final int n = 12;
		gbc.insets = new Insets( 2, 0, 2, n );

		if(debug) debugMsg( kFName, "Start" );	

		// For each section
		java.util.List sections = mXmlForm.findElementsByPath( "section" );

		if(debug) debugMsg( kFName, "# sections = " + sections.size() );	

		int sectionCounter = 0;
		// For each section
		for( Iterator sit = sections.iterator() ; sit.hasNext() ; ) {
			Element section = (Element) sit.next();
			sectionCounter++;
	
			JPanel p = new JPanel();
			JPanel pp = new JPanel();
			gbc = new GridBagConstraints();
			p.setLayout( new GridBagLayout() );
			// gbc.anchor = GridBagConstraints.NORTH;
			// p.setLayout( gbc );
	
			String title = section.getAttributeValue( "title" );
			title = nie.core.NIEUtil.trimmedStringOrNull( title );
			title = (null==title) ? "Section " + sectionCounter : title;

			pp.add( p, BorderLayout.NORTH );
	
			// tframe.addTab( title, p );
			tframe.addTab( title, pp );


			// tframe.addTab( title, null, p, tip );
	
			// GridBagConstraints gbc = new GridBagConstraints();
	
			// JLabel jl = new JLabel("Fill me out");
	
			// For each field in this section
			java.util.List fields = nie.core.JDOMHelper.findElementsByPath( section, "field" );
			if(debug) debugMsg( kFName, "# sections = " + fields.size() );	
			int fieldCounter = 0;
			for( Iterator fit = fields.iterator() ; fit.hasNext() ; ) {
				Element field = (Element) fit.next();
				fieldCounter++;

				String type = field.getAttributeValue( "type" );
				type = NIEUtil.trimmedLowerStringOrNull( type );
	
				String shortName = field.getAttributeValue( "name" );
				shortName = nie.core.NIEUtil.trimmedStringOrNull( shortName );
				if( null==shortName
					&& (null==type || ! type.equals("inline_button"))
				) {
					System.err.println(
						"Field with no name attribute"
						+ ": Section " + sectionCounter
						+ " field " + fieldCounter
						+ ".  Skipping."
						);
					continue;
				}
	
				// Display the label, if any
				// final int kLabelWidth = 250; // 150;
				String displayName = field.getAttributeValue( "label" );
				displayName = nie.core.NIEUtil.trimmedStringOrNull( displayName );
				if( null!=displayName ) {
					JLabel jl = new JLabel( displayName + ": " );
					jl.setHorizontalAlignment( SwingConstants.RIGHT );
					jl.setPreferredSize( new Dimension( LABEL_WIDTH, 20 ) );
					// jl.setHorizontalAlignment(SwingConstants.EAST);
					// jl.setAlignmentX( 25 );
					gbc.gridx = 0;
					gbc.gridy = fieldCounter;
					gbc.gridwidth = 1;
					gbc.fill = GridBagConstraints.NONE;
					// frame.getContentPane().add(jl, gbc);
					p.add(jl, gbc);
				}


				// Display the input box
				// final int kInputWidth = 150;
				// final int kInputWidth = 200;
				final int kTextAreaCharWidth = INPUT_WIDTH / 12;
				final int kTextAreaRowsDefault = 3;
	
				gbc.gridx = 1;
				gbc.gridy = fieldCounter;
				gbc.gridwidth = 2;
				gbc.fill = GridBagConstraints.HORIZONTAL;
	
				String defValue = field.getAttributeValue( "default" );
				String existingValue = null;

				// If we're editing an existing tree, lookup the value
				boolean isAttr = false;
				String yPath = null;
				Element existingElem = null;
				String elemAttrName = null;
				// Get the DATA form the ST config tree
				if( ! inIsNew
					&& (null==type || ! type.equals("inline_button"))
				) {		
					if( mNameToConfigTreePath.containsKey(shortName) ) {
						yPath = (String) mNameToConfigTreePath.get( shortName );
						isAttr = yPath.lastIndexOf( '@' ) > yPath.lastIndexOf( '/' );
						existingElem = (Element) mNameToConfigTreeElem.get( shortName );

						if( isAttr ) {
							int lastAt = yPath.lastIndexOf( '@' );
							if( lastAt < 0 || lastAt >= (yPath.length()-1) ) {
								errorMsg( kFName, "Invalid placement of @ at offset " + lastAt + " in yPath \"" + yPath + "\"; no value retrieved." );
							}
							else {
								elemAttrName = yPath.substring( lastAt+1 );
								existingValue = existingElem.getAttributeValue( elemAttrName );
							}
						}
						else
							existingValue = existingElem.getTextTrim();

						// statusMsg( kFName, NIEUtil.NL + '\t' + yPath + "=\"" + existingValue + "\"" );
	
					}
					else {
						warningMsg( kFName, "No existing element for \"" + shortName + "\"" );
					}
				}

				// The value we will use, if any
				String populateValue = inIsNew ? defValue : existingValue;
	
				if(debug) debugMsg( kFName,
					"inIsNew=" + inIsNew
					+ ", isAttr=" + isAttr
					+ NIEUtil.NL
					+ "shortName=\"" + shortName + "\""
					+ ", displayName=\"" + displayName + "\""
					+ NIEUtil.NL
					+ "defValue=\"" + defValue + "\""
					+ ", existingValue=\"" + existingValue + "\""
					+ ", populateValue=\"" + populateValue + "\""
					+ NIEUtil.NL
					+ "yPath = \"" + yPath + "\""
					+ ", elemAttrName = \"" + elemAttrName + "\""
					+ NIEUtil.NL
					+ "Existing element = " + ( null!=existingElem ? JDOMHelper.JDOMToString( existingElem, true ) : "NULL" )
					);




				// Drop down list
				if( null!=type && (type.equals("select") || type.equals("dropdown")) ) {
	
					// For each selection option
					java.util.List options = nie.core.JDOMHelper.findElementsByPath(
						field, "option"
						);
					int optionCounter = 0;
					java.util.Vector tmpVect = new java.util.Vector();
					for( Iterator oit = options.iterator() ; oit.hasNext() ; ) {
						Element optionElem = (Element) oit.next();
						optionCounter++;
						String option = optionElem.getTextNormalize();
						option = nie.core.NIEUtil.trimmedStringOrNull( option );
						if( null!=option )
							tmpVect.add( option );
					}
					// String [] optionAry = (String []) tmpVect.toArray();
					String [] optionAry = (String []) tmpVect.toArray( new String [1] );
					int valueAt = -1;
					if( null!=optionAry && null!=populateValue ) {
						for( int i=0; i<optionAry.length; i++ ) {
							if( optionAry[i] == populateValue ) {
								valueAt = i;
								break;
							}
						}
					}
					// JComboBox comboBox = new JComboBox(new String[] {"red", "green"});
					JComboBox comboBox = new JComboBox( optionAry );
					if( optionAry.length > 0 )
						if( valueAt >= 0 )
							comboBox.setSelectedIndex( valueAt );
						else
							comboBox.setSelectedIndex( 0 );
					p.add( comboBox, gbc );
					mNameToSwingWidget.put( shortName, comboBox );
	
				}
				// Text Area
				else if( null!=type && type.equals("textarea") ) {
					int numRows = nie.core.JDOMHelper.getIntFromAttribute( field, "rows", kTextAreaRowsDefault );
					numRows++;	// Add one for when we get horiz scroll bar
					JTextArea bigBox = null;
					// if( inIsNew && null!=defValue ) {
					if( null!=populateValue ) {
						bigBox = new JTextArea( populateValue, numRows, kTextAreaCharWidth );
					}
					else {
						bigBox = new JTextArea( numRows, kTextAreaCharWidth );
					}
					// bigBox.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5) );
					// p.add( bigBox, gbc );
					JScrollPane scroll = new JScrollPane( bigBox );
					p.add( scroll, gbc );
	
					mNameToSwingWidget.put( shortName, bigBox );
	
				}
				// A button
				else if( null!=type && type.equals("inline_button") ) {
					String buttonLabel = field.getTextTrim();
					buttonLabel = NIEUtil.trimmedStringOrNull( buttonLabel );
					buttonLabel = (null!=buttonLabel) ? buttonLabel : "Button";
					JButton myButton = new JButton( buttonLabel );

					// Add an event handler, if specified
					String clickClassName = JDOMHelper.getStringFromAttributeTrimOrNull( field, "click_event_class" );
					if( null!=clickClassName ) {

						try {

							// Class clickClass = Class.forName( clickClassName );
							final String kPrefix = "nie.config_ui." + kClassName + "$Event_";
							Class clickClass = Class.forName( kPrefix + clickClassName );
							// ActionListener listener = (ActionListener) clickClass.newInstance();
							// Object obj = clickClass.newInstance();
							// Non static member classes do not really use
							// const(), actually it's const( ParentClass that )
							Class [] constrSig = new Class [] {
								nie.config_ui.Configurator2.class
								// Object.class
							};

							if( shouldDoTraceMsg(kFName) ) {
								// Constructor [] constrs = clickClass.getConstructors();
								Constructor [] constrs = clickClass.getDeclaredConstructors();
								traceMsg( kFName, "Examining " + constrs.length + " constructors" );
								for( int i=0; i<constrs.length; i++ ) {
									Constructor tmpConstr = constrs[i];
									Class [] types = tmpConstr.getParameterTypes();
									traceMsg( kFName,
										"Const # " + (i+1) + " has " + types.length + " args."
										);
									for( int j=0; j<types.length; j++ ) {
										Class currType = types[j];
										statusMsg( kFName,
											"\tArg # " + (j+1) + " is " + currType.getName()
											);
									}
								}
							}

							// throws NoSuchMethodException
							// Constructor constr = clickClass.getConstructor( constrSig );
							Constructor constr = clickClass.getDeclaredConstructor( constrSig );
							Object [] parms = new Object[] { this };
							Object obj = constr.newInstance( parms );

							ActionListener listener = (ActionListener) obj;
							myButton.addActionListener(
								listener
								);

							// Event_VerifyDBConn obj = new Event_VerifyDBConn();
							/***
							ActionListener obj = new Event_VerifyDBConn();
								statusMsg( kFName,
								"full class is " + obj.getClass().getName()
								);
							***/
							// Gives
							// "nie.config_ui.Configurator2$Event_VerifyDBConn"

							//	jb.addActionListener(new ActionListener() {
							//		public void actionPerformed(ActionEvent e) {
							//			MainWindow.this.toggleCompactView();
							//		}
							//	});

							//this.favoriteMenu.addActionListener(new ActionListener() {
							//			public void actionPerformed(ActionEvent e) {
							//				//if (e.getActionCommand() != "comboBoxChanged")
							//				//	 return;
							//				if (MainWindow.this.ignoreFavoriteMenu)
							//					return;

						}
						catch( Throwable t ) {
							errorMsg( kFName,
								"Error wiring up button " + buttonLabel
								+ ": " + t
								);
						}


					}

					p.add( myButton, gbc );
					// mNameToSwingWidget.put( shortName, myButton );
				}
				// Defaults to text input box
				else {
					JTextField inputBox = null;
					if( null!=populateValue ) {
						inputBox = new JTextField( populateValue );
					}
					else {
						inputBox = new JTextField();
						// find element and add value
					}
					inputBox.setPreferredSize( new Dimension( INPUT_WIDTH,20 ) );
					p.add( inputBox, gbc );
					mNameToSwingWidget.put( shortName, inputBox );
				}
	
	
			}	// End for each field
	
		}	// End for each Section
	
		/***
	
		JPanel p2 = new JPanel();
		p2.setLayout(new GridBagLayout() );
		// tframe.add( p2 );
		tframe.addTab( "Where", p2 );
	
	
	    GridBagConstraints gbc = new GridBagConstraints();
	
	    JLabel jl = new JLabel("Fill me out, beyatch!");
	    jl.setHorizontalAlignment(SwingConstants.CENTER);
		// jl.setHorizontalAlignment(SwingConstants.EAST);
	    // gbc.gridwidth = 4;
	
	
	    jl = new JLabel("Where dat at:");
		jl.setHorizontalAlignment( SwingConstants.RIGHT );
		jl.setPreferredSize( new Dimension(labelWidth,20) );
	    gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.gridwidth = 1;
	    gbc.fill = GridBagConstraints.NONE;
	    // frame.getContentPane().add(jl, gbc);
		p2.add(jl, gbc);
	    this.city = new JTextField();
	    gbc.gridx = 1;
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    // frame.getContentPane().add(this.city, gbc);
		p2.add(this.city, gbc);
	
	
		jl = new JLabel("State:");
		jl.setHorizontalAlignment( SwingConstants.RIGHT );
		jl.setPreferredSize( new Dimension(labelWidth,20) );
		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		// frame.getContentPane().add(jl, gbc);
		p2.add(jl, gbc);
	    this.state = new JComboBox(new String[] {"Eas' Coas'", "Wes' Coas'"});
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		// gbc.gridy = 6;
	    // frame.getContentPane().add(this.state, gbc);
		p2.add(this.state, gbc);
	
		***/
	
	
		// The main Submit button
		JButton jb = new JButton("Save");
		jb.setPreferredSize( new Dimension(100,20) );
		jb.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				Configurator2.this.save();
			}
		});
		/***
		gbc.gridx = 1;
		gbc.gridy = 9;
		gbc.weightx = 1;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		frame.getContentPane().add(jb, gbc);
		***/
		mMainFrame.getContentPane().add( jb, BorderLayout.SOUTH );
		// mMainFrame.add( jb );
	    
		// frame.pack();
		// frame.show();
		// tframe.pack();
		// tframe.show();
	
		// p1.pack();
		// tframe.show();
		mMainFrame.pack();
		mMainFrame.show();
	
	}

	private static void __sep__Form_Fields__() {}
	/////////////////////////////////////////////

	public void registerFormFields()
		throws ConfiguratorException
	{
		final String kFName = "registerFormFields";
		final String kExTag = kClassName + '.' + kFName + ": ";
	
		final String path = "//field";
	
		if( null==mXmlForm )
			throw new ConfiguratorException( kExTag + "Null/missing form definition data." );
		if( null==mConfigTree )
			throw new ConfiguratorException( kExTag + "Null/missing system configuration data/tree." );
	
		try {
			XPath xpath = XPath.newInstance( path );
		
			// List results = xpath.selectNodes( inDoc );
			java.util.List results = xpath.selectNodes( mXmlForm.getJdomElement() );
		
			infoMsg( kFName,
				"Looking for \"" + path + "\" from node \"" + mXmlForm.getElementName() + "\""
				// "Looking for \"" + inPath + "\" from node \"" + inDoc.getRootElement().getName() + "\""
				+ " Found " + results.size() + " fields."
				);

			int fieldCounter = 0;
			for( Iterator it = results.iterator() ; it.hasNext() ; ) {
				Object currObj = it.next();
				if( currObj instanceof org.jdom.Element ) {
					fieldCounter++;
					Element currElem = (Element) currObj;
					// Obsess about the name
					String name = JDOMHelper.getStringFromAttributeTrimOrNull(currElem, "name" );
					String type = JDOMHelper.getStringFromAttributeTrimOrNull(currElem, "type" );
					type = NIEUtil.trimmedLowerStringOrNull( type );
					if( null==name ) {
						if( null==type || ! (type.equals("inline_button") || type.equals("vertical_spacer")) )
							warningMsg( kFName, "Field # " + fieldCounter + " has no name, skipping." );
						continue;
					}
					if( mNameToFormDefElem.containsKey( name ) )
						throw new ConfiguratorException( kExTag +
							"Duplicate name \"" + name + "\" in field # " + fieldCounter
							);
					mFieldNameList.add( name );
					mNameToFormDefElem.put( name, currElem );
					// Obsess about the path
					// Since NIE paths are not exactly xpath compliant, we call
					// ours ypath in the code here
					String yPath = JDOMHelper.getStringFromAttributeTrimOrNull(currElem, "xpath" );
					boolean isSpecial = JDOMHelper.getBooleanFromAttribute( currElem, "is_special", false );
					if( null==yPath && ! isSpecial )
						throw new ConfiguratorException( kExTag +
							"No confitg xpath given for field \"" + name + "\", field # " + fieldCounter
							);
					if( null!=yPath ) {
						// store the path for later
						mNameToConfigTreePath.put( name, yPath );
	
						// statusMsg( kFName, "Storing " + name + "=" + yPath );
	
						// Now locate/create the node in the in-memory config tree
						Element configElem = JDOMHelper.findOrCreateElementByPath(
							mConfigTree.getJdomElement(), yPath, true
							);
						if( null==configElem )
							throw new ConfiguratorException( kExTag +
								"Unable to find/create config tree node for field \"" + name + "\", field # " + fieldCounter
								+ " xpath=" + yPath
								);
						mNameToConfigTreeElem.put( name, configElem );
					}
					if( isSpecial )
						mIsSpecialFieldNames.add( name );
	
				}
				else {
					errorMsg( kFName, "Don't know how to handle object " + currObj );
				}
			}
	
		} catch( Exception e ) {
			errorMsg( kFName,  "Exception Strack:" );
			e.printStackTrace( System.err );
			throw new ConfiguratorException( kExTag + "Caught exception: " + e );
		}
	
		infoMsg( kFName, "mNameToConfigTreePath has " + mNameToConfigTreePath.size() + " entries" );
	
	}



	String getSwingFormValueTrimOrNull( String inFieldName ) {
		final String kFName = "getSwingFormValueTrimOrNull";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( null==inFieldName ) {
			errorMsg( kFName, "Null field name passed in; returning null." );
			return null;
		}
		if( null==mNameToSwingWidget ) {
			errorMsg( kFName, "Null widget cache; returning null." );
			return null;
		}
		if( ! mNameToSwingWidget.containsKey(inFieldName) ) {
			errorMsg( kFName, "Field \"" + inFieldName + "\" not found in widget cache; returning null." );
			return null;
		}

		Object widget = mNameToSwingWidget.get( inFieldName );
		String outValue = null;
		traceMsg( kFName, "Widget is a " + widget.getClass().getName() );
		// ALL METHODS to think about when adding a type
		// * copySwingValuesToJdomTree
		// * clearSwingFormValue
		// * getSwingFormValue
		// * getSwingFormValueTrimOrNull
		if( widget instanceof JComboBox ) {
			JComboBox comboWidget = (JComboBox) widget;
			outValue = comboWidget.getSelectedItem().toString();
			traceMsg( kFName, "Combo value = \"" + outValue + "\"" );
		}
		else if( widget instanceof JCheckBox ) {
			JCheckBox myBox = (JCheckBox) widget;
			boolean state = myBox.isSelected();
			outValue = state ? "TRUE" : "FALSE";
			traceMsg( kFName, "Checkbox value = \"" + outValue + "\"" );
		}
		else if( widget instanceof JLabel ) {
			JLabel myLabel = (JLabel) widget;
			outValue = myLabel.getText();
			traceMsg( kFName, "Lavel value = \"" + outValue + "\"" );
		}
		else {
			JTextComponent textWidget = (JTextComponent) widget;
			outValue = textWidget.getText();
			traceMsg( kFName, "Text value = \"" + outValue + "\"" );
		}
		outValue = NIEUtil.trimmedStringOrNull( outValue );
		return outValue;
	}

	boolean setSwingFormValue( String inFieldName, String inNewValue ) {
		final String kFName = "setSwingFormValue";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( null==inFieldName ) {
			errorMsg( kFName, "Null field name passed in; returning null." );
			return false;
		}
		if( null==mNameToSwingWidget ) {
			errorMsg( kFName, "Null widget cache; returning null." );
			return false;
		}
		if( ! mNameToSwingWidget.containsKey(inFieldName) ) {
			errorMsg( kFName, "Field \"" + inFieldName + "\" not found in widget cache; returning null." );
			return false;
		}

		if( null==inNewValue )
			inNewValue = "";

		Component widget = (Component) mNameToSwingWidget.get( inFieldName );
		String outValue = null;
		traceMsg( kFName, "Widget is a " + widget.getClass().getName() );
		// ALL METHODS to think about when adding a type
		// * copySwingValuesToJdomTree
		// * clearSwingFormValue
		// * getSwingFormValue
		// * getSwingFormValueTrimOrNull
		if( widget instanceof JComboBox ) {
			JComboBox comboWidget = (JComboBox) widget;
			// Try to set it
			comboWidget.setSelectedItem( inNewValue );
			// Double check it, maybe value wasn't in list?
			String checkValue = comboWidget.getSelectedItem().toString();
			if( null==checkValue || ! checkValue.equals(inNewValue) ) {
				// Add it
				comboWidget.addItem( inNewValue );
				// And select it
				int howMany = comboWidget.getItemCount();
				comboWidget.setSelectedIndex( howMany - 1 );
			}

			// comboWidget.addItem( foo )
			// comboWidget.getItemAt()
			// comboWidget.getItemCount()
			// comboWidget.setSelectedItem(anObject)
			// comboWidget.setSelectedIndex(anIndex)
			// comboWidget.
			traceMsg( kFName, "Combo value = \"" + inNewValue + "\"" );
		}
		else if( widget instanceof JCheckBox ) {
			JCheckBox myBox = (JCheckBox) widget;
			boolean val = NIEUtil.decodeBooleanString( inNewValue, false );
			myBox.setSelected( val );
			traceMsg( kFName, "Checkbox value = \"" + inNewValue + "\"(=" + val + "\"" );
		}
		else if( widget instanceof JLabel ) {
			JLabel myLabel = (JLabel) widget;
			myLabel.setText( inNewValue );
			traceMsg( kFName, "Label value = \"" + inNewValue + "\"" );
		}
		else {
			JTextComponent textWidget = (JTextComponent) widget;
			textWidget.setText( inNewValue );
			traceMsg( kFName, "Text value = \"" + inNewValue + "\"" );
		}
		widget.invalidate();
		return true;
	}

	boolean clearSwingFormValue( String inFieldName ) {
		final String kFName = "clearSwingFormValue";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( null==inFieldName ) {
			errorMsg( kFName, "Null field name passed in; returning null." );
			return false;
		}
		if( null==mNameToSwingWidget ) {
			errorMsg( kFName, "Null widget cache; returning null." );
			return false;
		}
		if( ! mNameToSwingWidget.containsKey(inFieldName) ) {
			errorMsg( kFName, "Field \"" + inFieldName + "\" not found in widget cache; returning null." );
			return false;
		}

		final String newValue = "";

		Component widget = (Component) mNameToSwingWidget.get( inFieldName );
		String outValue = null;
		traceMsg( kFName, "Widget is a " + widget.getClass().getName() );
		// ALL METHODS to think about when adding a type
		// * copySwingValuesToJdomTree
		// * clearSwingFormValue
		// * getSwingFormValue
		// * getSwingFormValueTrimOrNull
		if( widget instanceof JComboBox ) {
			JComboBox comboWidget = (JComboBox) widget;
			// Try to set it
			comboWidget.setSelectedItem( newValue );
			// Double check it, maybe value wasn't in list?
			String checkValue = comboWidget.getSelectedItem().toString();
			if( null==checkValue || ! checkValue.equals(newValue) ) {
				// Add it
				comboWidget.addItem( newValue );
				// And select it
				int howMany = comboWidget.getItemCount();
				comboWidget.setSelectedIndex( howMany - 1 );
			}

			// comboWidget.addItem( foo )
			// comboWidget.getItemAt()
			// comboWidget.getItemCount()
			// comboWidget.setSelectedItem(anObject)
			// comboWidget.setSelectedIndex(anIndex)
			// comboWidget.
			traceMsg( kFName, "Set Combo value = \"" + newValue + "\"" );
		}
		else if( widget instanceof JCheckBox ) {
			JCheckBox myBox = (JCheckBox) widget;
			boolean val = NIEUtil.decodeBooleanString( newValue, false );
			myBox.setSelected( val );
			traceMsg( kFName, "Set Checkbox value = \"" + val + "\"" );
		}
		else if( widget instanceof JLabel ) {
			JLabel myLabel = (JLabel) widget;
			myLabel.setText( newValue );
			traceMsg( kFName, "Set Label value = \"" + newValue + "\"" );
		}
		else {
			JTextComponent textWidget = (JTextComponent) widget;
			textWidget.setText( newValue );
			traceMsg( kFName, "Set Text value = \"" + newValue + "\"" );
		}
		widget.invalidate();
		return true;
	}


	private static void __sep__Low_Level_Logic__() {}
	/////////////////////////////////////////////

	void markAsModified() {
		mIsModified = true;
	}



	boolean isSpecialField( String inFieldName ) {
		final String kFName = "isSpecialField";
		if( null==inFieldName ) {
			errorMsg( kFName, "Null field name passed in; returning false;" );
			return false;
		}
		return mIsSpecialFieldNames.contains( inFieldName );
	}


	private static void __sep__Dialog_Boxes__() {}
	/////////////////////////////////////////////

	void showLastRunlogErrorAsDialogBox(
			String inTitle, String inMsgIntro
	) {

		java.util.List messages = getRunLogObject().getMessages();
		String errText = null;
		if( null!=messages && messages.size() > 0 )
		{
			for( Iterator it = messages.iterator(); it.hasNext() ; )
			{
				errText = (String) it.next();
				break;
			}
			// TODO: could give more info
		}
		if( null == errText )
			errText = "Please check the log files.";
		else {
			errText = NIEUtil.longStringChopper( errText, 80, "\n" );
		}

		JOptionPane.showMessageDialog( mMainFrame,
			inMsgIntro
			+ "\n\n" + errText + '\n'
			,
			inTitle,
			JOptionPane.ERROR_MESSAGE
			);

	}


	void showErrorDialogBox(
			String inTitle, String inMessage
	) {
		JOptionPane.showMessageDialog( mMainFrame,
			inMessage + '\n' + '\n',
			inTitle,
			JOptionPane.ERROR_MESSAGE
			);
	}


	boolean showYesNoDialog( String inTitle, String inMessage ) {
		int n = JOptionPane.showOptionDialog(
			mMainFrame,	// It's OK if mMainFrame is null
			inMessage + '\n' + '\n',
			inTitle,
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			null,
			null
			);

		return n==0;
	}

	int showYesNoCancelDialog( String inTitle, String inMessage ) {
		int n = JOptionPane.showOptionDialog(
			mMainFrame,	// It's OK if mMainFrame is null
			inMessage + '\n' + '\n',
			inTitle,
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			null,
			null
			);

		// Convert [x] box to Cancel
		if( n<0 )
			n = 2;

		return n;
	}


	int showYesNoCancelDialogV0( String inTitle, String inMessage ) {
		final String kFName = "showYesNoCancelDialog";
		final String cancelStr = "Cancel";
		// final String [] choices = new String [] { "Yes", "No", "Cancel" };
		final String [] choices = new String [] { "Yes", "No", cancelStr };
		int n = JOptionPane.showOptionDialog(
			mMainFrame,	// It's OK if mMainFrame is null
			inMessage + '\n' + '\n',
			inTitle,
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			choices,
			cancelStr // "Cancel"
			);

		statusMsg( kFName, "Selected " + n );
		if( n>=0 )
			statusMsg( kFName, "= \"" + choices[n] + "\"" );

		return n;
	}

	// From http://www.informit.com/articles/article.asp?p=18716
	/***
	The getImage() method starts by getting an instance of the current class,
	and then asking that class to locate the resource toolbarButtonGraphics/imagefile.
	For example, if we wanted to load the image for the Cut icon, we would specify
	toolbarButtonGraphics/general/Cut16.gif.
	
	The image name, including the General folder, is all specific to the JAR file
	we are using: jlfgr-1_0.jar (Java Look and Feel Graphics Repository).
	Refer back to http://developer.java.sun.com/developer/techDocs/hi/repository/
	and the example at the end of this article for more information.
	***/
	public ImageIcon getImage( String strFilename )
	{
		  // Get an instance of our class
		  Class thisClass = getClass();
		  // Locate the desired image file and create a URL to it
		  java.net.URL url = thisClass.getResource( "toolbarButtonGraphics/" +
													strFilename );

		  // See if we successfully found the image
		  if( url == null )
		  {
			 System.out.println( "Unable to load the following image: " +
								 strFilename );
			 return null;
		  }

		  // Get a Toolkit object
		  Toolkit toolkit = Toolkit.getDefaultToolkit();
      
		  // Create a new image from the image URL
		  Image image = toolkit.getImage( url );

		  // Build a new ImageIcon from this and return it to the caller
		  return new ImageIcon( image );
	}



	private static void __sep__Runtime_Logging__() {}
	//////////////////////////////////////////////////


	private static RunLogInterface getRunLogObject()
	// can't access some of impl's extensions with interface reference
	//private static RunLogBasicImpl getRunLogObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogObject();
	}
	
	
	// Return the same thing casted to allow access to impl extensions
	private static RunLogBasicImpl getRunLogImplObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogImplObject();
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
	
	
	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
	}
	
	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
		// return getRunLogObject().statusMsg( kClassName, inFromRoutine,
		// 	"DEBUG: " + inMessage
		// 	);
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


	private static void __sep__Member_Fields__() {}
	///////////////////////////////////////////////
	
	
	JFrame mMainFrame;

	JTree mNavTree;
	JPanel mTabsPanel;
	CardLayout mTabChanger;  // Card changer, whatever
	
	JDOMHelper mXmlForm;
	JDOMHelper mConfigTree;
	String mSaveFileName;
	
	java.util.List mFieldNameList = new Vector();
	
	Hashtable mNameToFormDefElem = new Hashtable();
	Hashtable mNameToConfigTreePath = new Hashtable();
	Hashtable mNameToConfigTreeElem = new Hashtable();
	Hashtable mNameToSwingWidget = new Hashtable();
	HashSet mIsSpecialFieldNames = new HashSet();

	boolean mIsNew;
	boolean mIsModified;
	boolean mIsVerified;
	boolean mIsSaved;

	public static final String FORM_DEF_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX + kClassName + ".xml";
	public static final String TEMPLATE_CONFIG_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "static_files/default_config_include_structure/main.xml";

	public static final String DEFAULT_ATTR = "default";

	public static final int LABEL_WIDTH = 230;// 200; // 250; // 150;
	public static final int INPUT_WIDTH = 230;// 200;	// 150
	public static final int BOTTOM_BUTTON_WIDTH = 150; // 100

	// These are the names of fields ON THE FORM
	public static final String SEARCH_URL_FIELD = "search_url";
	public static final String SEARCH_METHOD_FIELD = "search_method";
	public static final String SEARCH_VENDOR_FIELD = "search_vendor";
	public static final String SEARCH_QUERY_FIELD = "search_field";

	public static final int MAX_HIDDEN_FIELDS = 10;
	public static final String HIDDEN_FIELD_NAME_TEMPLATE = "search_hidden_field_name_";
	public static final String HIDDEN_VALUE_NAME_TEMPLATE = "search_hidden_field_value_";

	public static final int MAX_OPTION_FIELDS = 1;
	public static final int MAX_OPTION_VALUES = 20;
	public static final String OPTION_FIELD_NAME_TEMPLATE = "search_form_option_field_";

	public static final int TEXT_AREA_DEFAULT_ROWS = 3;
	public static final int TEXT_AREA_DEFAULT_CHAR_WIDTH = INPUT_WIDTH / 12;

	public static String EVENT_CLASS_ATTR = "click_event_class";

//		search_form_option_field_name_1
//		search_form_option_field_desc_1
//		search_form_option_field_1_value_1
//		search_form_option_field_1_value_1_desc
}
