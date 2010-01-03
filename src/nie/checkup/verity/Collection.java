/*
 * Created on Jul 2, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nie.checkup.verity;

import java.util.*;
import java.io.*;

import org.jdom.*;
import javax.xml.transform.*;

import nie.core.*;
import nie.checkup.*;

/**
 * @author Mark Bennett
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Collection {

	public static final String kClassName = "Collection";

	public static void main(String[] args) {
		final String kFName = "main";

		List roots = new ArrayList();
		// OPTIONAL path to Verity directory
		String vbinStr = null;
		String titleFieldStr = null;
		File vbin = null;
		boolean doDetailed = false;
		boolean forceSummary = false;
		boolean doPartitions = false;
		boolean forceNoParts = false;
		boolean doListOnly = false;
		boolean askedForHelp = false;

		String outFile = null;

		// For each arg
		for( int i=0; i<args.length; i++ ) {
			String arg = args[i];
			if( ! arg.startsWith("-") )
				roots.add( arg );
			else {
				arg = arg.toLowerCase().trim();
				if( arg.equals("-h") || arg.equals("-?") || arg.startsWith("-help") ) {
					askedForHelp = true;
					break;
				}
				else if( arg.startsWith("-detail") || arg.startsWith("-thorough") )
					doDetailed = true;
				else if( arg.startsWith("-quick") || arg.startsWith("-summary") )
					forceSummary = true;
				else if( arg.startsWith("-partition") )
					doPartitions = true;
				else if( arg.startsWith("-no_part") || arg.startsWith("-nopart") )
					forceNoParts = true;
				else if( arg.startsWith("-list") )
					doListOnly = true;
				// Se an output file
				else if( arg.startsWith("-output") ) {
					if( null!=outFile )
						bailOnSyntax( "-output file already set to \"" + outFile + "\"" );
					i++;
					if( i>=args.length )
						bailOnSyntax( "must include a file name after -output option" );
					outFile = args[i];
					if( outFile.startsWith("-") )
						bailOnSyntax( "-output file name must not start with a dash (-)" );
				}
				else if( arg.startsWith("-vbin") ) {
					if( null!=vbin )
						bailOnSyntax( "-vbin file already set to \"" + vbin + "\"" );
					i++;
					if( i>=args.length )
						bailOnSyntax( "must include a path after -vbin option" );
					vbinStr = args[i];
					if( vbinStr.startsWith("-") )
						bailOnSyntax( "-vbin path must not start with a dash (-)" );
					vbin = new File( vbinStr );
					if( ! vbin.exists() || ! vbin.isDirectory() )
						bailOnSyntax( "No directory \"" + vbinStr + "\"" );
				}
				else if( arg.startsWith("-title") ) {
					if( null!=titleFieldStr )
						bailOnSyntax( "-title_field already set to \"" + titleFieldStr + "\"" );
					i++;
					if( i>=args.length )
						bailOnSyntax( "must include a field name after -title option" );
					titleFieldStr = args[i];
					if( titleFieldStr.startsWith("-") )
						bailOnSyntax( "-title_field must not start with a dash (-)" );
				}
				else
					staticErrorMsg( kFName, "ignoring unknown option \"" + arg + "\"" );
			}
		}

		if( askedForHelp )
			bailOnSyntax( "Help was requested: ignoring all other options" );

		if( doListOnly &&
			( doDetailed || forceSummary || doPartitions || forceNoParts || null!=outFile )
			)
		{
			bailOnSyntax( "Error: Can't -list_only with any other options" );
		}

		if( forceSummary && doDetailed )
			bailOnSyntax( "Error: Can't set both -thorough and -quick" );

		if( doPartitions && forceNoParts )
			bailOnSyntax( "Error: Can't set both -partitions and -no_partitions" );

		if( roots.isEmpty() )
			bailOnSyntax(
				"Error: no starting directories given.  Reminder: use . for current directory."
				);

		try {
			// Find the collections
			List colls = findCollectionsFactory( roots, vbin, titleFieldStr );

			// If just listing
			if( doListOnly ) {

				if( colls.isEmpty() ) {
					System.out.println( "Did not find any Verity collections." );
				}
				else {
					System.out.println( "Found " + colls.size() + " Verity collections." );
					System.out.println();
					System.out.println( "Name\tDirectory" );
					System.out.println( "----\t---------" );

					for( Iterator it = colls.iterator() ; it.hasNext() ; ) {
						Collection coll = (Collection) it.next();
						System.out.println(
							coll.getShortName() + '\t' + coll.getDirectory()
							);
					}
				}

			}
			// Else do the full report!
			else {
				if( null==outFile )
					outFile = DEFAULT_OUT_FILE;

				// Do detailed if asked for it, or if only one coll
				doDetailed = !forceSummary && (doDetailed || colls.size()==1);
				doPartitions = !forceNoParts && (doPartitions || colls.size()==1);
	
				Element reportElem = null;
				for( Iterator it = colls.iterator() ; it.hasNext() ; ) {
					Collection coll = (Collection) it.next();
	
					/***
					if( doDetailed )
						staticStatusMsg( kFName, "\n" + coll.getSlowCollectionSummary() + "\n" );
					else
						staticStatusMsg( kFName, "\n" + coll.getFastCollectionSummary() + "\n" );
					***/
	
					reportElem = coll.generateCollectionStatistics(
						reportElem, doDetailed, doPartitions, doDetailed
						);
	
	// System.out.println( "^^^^^^^^^^^" );
	// System.err.println();
	
				}
	
	
				// staticStatusMsg( kFName, "report=" + NIEUtil.NL
				//	+ JDOMHelper.JDOMToString( reportElem, true )
				//	);
	
				if( null!=reportElem ) {
					Element html = formatCollectionStatistics( reportElem );
	
					if( null!=html ) {
						JDOMHelper.writeToFile( html, outFile, false );
						System.out.println( "Generated report to " + outFile );
					}
				}

			}	// end else do the full report

		}
		catch( Throwable t ) {
			staticErrorMsg( kFName, "Error running:" );
			t.printStackTrace( System.err );
			System.exit(2);
		}

	}

	static void bailOnSyntax( String inMsg )
	{
		final String nl = NIEUtil.NL;
		final char t = '\t';
		final String script = "vcheck";
		final String syntax =
			nl + script + " Syntax:" + nl
			+ nl
			+ "For syntax help:" + nl
			+ script + " [-h|-help|-?]" + nl
			+ nl
			+ t + "or" + nl
			+ nl
			+ "To search for collections and list them out:" + nl
			+ script + " -list dir1 [dir2 ...] [-vbin verity_binary_directory]" + nl
			+ nl
			+ t + "or" + nl
			+ nl
			+ "To generate an HTML report of collection statistics:" + nl
			+ script + " [-quick|-thorough] [-parts|-no_parts]"
			+ nl + t + "coll1 [coll2 ...]"
			+ " [-output report_name.html] [-vbin verity_binary_directory]" + nl
			+ " [-title|-title_field collection_title_field]" + nl
			+ nl
			+ "Where colls are either specific collections or a directories to search." + nl
			+ "If only one collection is found, defaults to turning on -thorough and -parts." + nl
			+ "If the Verity binary directory is not in your path, set it with -vbin." + nl
			+ "The default output report name is \"" + DEFAULT_OUT_FILE + "\"" + nl
			+ "Reminder: If a dir or coll name contains spaces, enclose it in quotes." + nl
			+ nl
			;

		System.out.println(
			nl + inMsg + nl
			+ syntax
			);

		System.exit(1);
	}


	// Factory
	public static List findCollectionsFactory( File dirRoot,
	    File optVerityBinaryDirectory, String optTitleField
	)
		throws IOException
	{
		List tmpList = new ArrayList();
		tmpList.add( dirRoot );
		return findCollectionsFactory( tmpList, optVerityBinaryDirectory, optTitleField );
	}
	public static List findCollectionsFactory( String dirRoot,
	    File optVerityBinaryDirectory, String optTitleField
	)
		throws IOException
	{
		List tmpList = new ArrayList();
		tmpList.add( dirRoot );
		return findCollectionsFactory( tmpList, optVerityBinaryDirectory, optTitleField );
	}

	public static List findCollectionsFactory( List dirRoots,
	    File optVerityBinaryDirectory, String optTitleField
	)
		throws IOException
	{

		final String kFName = "findCollectionsFactory";

		if( null==dirRoots || dirRoots.isEmpty() ) {
			dirRoots = new ArrayList();
			dirRoots.add( "." );
		}
		List outColls = new ArrayList();
		Set foundCollDirs = new HashSet();
		Set skippedCollDirs = new HashSet();

		// For each root to search
		for( Iterator rit = dirRoots.iterator() ; rit.hasNext() ; ) {
			File dirRoot = null;
			Object dirObj = rit.next();
			if( dirObj instanceof File )
				dirRoot = (File) dirObj;
			else if( dirObj instanceof String )
				dirRoot = new File( (String)dirObj );
			else {
				throw new IOException(
					"Don't know how to scan root dir object "
					+ "\"" + dirObj + "\""
					+ " of type \"" + dirObj.getClass().getName() + "\""
					);
			}

			// Now search that root
			try {
				List candidateFiles = NIEUtil.findFiles( dirRoot, "pdd" );
	
				staticInfoMsg( kFName, "Found " + candidateFiles.size() + " candiate pdd files" );
				for( Iterator cit = candidateFiles.iterator() ; cit.hasNext() ; ) {
					File pdd = (File) cit.next();
					// System.out.println( '\t' + pdd.toString() );
					String tmpName = pdd.getName();
					if( tmpName.length() < 1 )
						continue;
					// First character should be a digit
					char firstChar = tmpName.charAt(0);
					if( firstChar < '0' || firstChar > '9' )
						continue;
					// Parent should be a directory called pdd
					File parent = pdd.getParentFile();
					if( null==parent )
						continue;
					String parentName = parent.getName();
					if( ! parentName.equals("pdd") )
						continue;
					// Ok, get the grandparent
					File gramps = parent.getParentFile();
					if( null==gramps )
						gramps = new File( "." );
					else
						if( gramps.getName().endsWith(".vdx") ) {
							if( ! skippedCollDirs.contains(gramps) ) {
								staticInfoMsg( kFName, "Skipping Verity Knowledge Tree " + gramps );
								skippedCollDirs.add( gramps );
							}
							continue;
						}
					if( ! foundCollDirs.contains(gramps) ) {
						foundCollDirs.add( gramps );
						try {
							Collection coll = new Collection( gramps,
							        optVerityBinaryDirectory, optTitleField );
							outColls.add( coll );
						}
						// We let IO Exceptions flow up
						catch( DataException de ) {
							staticErrorMsg( kFName,
								"Error opening collection \"" + gramps.getAbsolutePath() + "\" :" + de
								);
						}
					}
				}
	
			}
			catch( Exception e ) {
				staticErrorMsg( kFName, "" + e );
				e.printStackTrace( System.err );
			}

		}	// end for each root to search


		/***
		System.out.println( "Found " + colls.size() + " collections" );
		for( Iterator it = colls.iterator() ; it.hasNext() ; ) {
			File coll = (File) it.next();
			System.out.println( '\t' + coll.toString() );
		}
		***/

		return outColls;
	}



	public Collection( File inCollDir,
	    File optVerityBinaryDiretory, String optTitleField
	)
		throws DataException, IOException
	{
		if( null==inCollDir )
			throw new DataException( "Null collection directory passed in." );

		mCollDir = inCollDir;
		mVBin = optVerityBinaryDiretory;
		// If it's null, the partition contructor will handle it
		optTitleField = NIEUtil.trimmedStringOrNull( optTitleField );
		if( null!=optTitleField )
		    mTitleField = optTitleField;
		
		// Open the PDD and all the partitions
		openEntireCollection();
	}

	void openEntireCollection()
		throws DataException, IOException
	{
		final String kFName = "openEntireCollection";

		if( null==mCollDir )
			throw new DataException( "Null collection directory" );

		staticInfoMsg( kFName, NIEUtil.NL + "Opening collection " + mCollDir );

		// The file for the main table of contents
		cachePddFile();

		// Some basic info
		cacheVdbVersion();

		// the name of all the listed partitions
		cacheActivePartitionNames();

		// Let's get them all
		openAndCacheAllParitions();
	}

	private static void __sep__Fast_Items__(){}
	///////////////////////////////////////////////////////////

	String getFastCollectionSummary()
		throws IOException, DataException
	{
		StringBuffer outSum = new StringBuffer();

		outSum.append("Collection: ").append( getShortName() ).append( " at " );
		outSum.append( getDirectory() );
		outSum.append( NIEUtil.NL );

		outSum.append( "\tupdated: " ).append( getMostRecentCollectionDateAsFormattedString() );
		outSum.append( " (" ).append( getCollectionAgeAsFormattedString() ).append( ')' );
		outSum.append( NIEUtil.NL );

		outSum.append( "\tstats: paritions=" ).append( getParitionCount() );

		// outSum.append( " active_docs=" ).append( getActiveDocCount() );

		outSum.append( " raw_docs=" ).append( getRawDocCount() );

		return new String( outSum );
	}

	public File getDirectory()
	{
		return mCollDir;
	}

	public String getShortName()
	{
		if( null!=mCollDir )
			return mCollDir.getName();
		else
			return null;
	}


	public File getPddDirectory()
		throws DataException //, IOException
	{
		return getCachedPddFile().getParentFile();
	}


	public int getParitionCount()
		throws DataException
	{
		if( null==cPartitions )
			throw new DataException( "Null paritions; not initialized" );
		return cPartitions.size();
	}

	public int getRawDocCount()
		throws DataException
	{
		if( cRawDocCount >= 0 )
			return cRawDocCount;
		int outAnswer = 0;
		List parts = getCachedParitionObjects();
		// ^^^ will complain if none
		for( Iterator it = parts.iterator(); it.hasNext() ; ) {
			Partition part = (Partition) it.next();
			outAnswer += part.getRawDocCount();
		}
		cRawDocCount = outAnswer;
		return cRawDocCount;
	}

	public String getVdbVersion()
	{
		return cVdbVersion;
	}

	String cacheVdbVersion()
		throws IOException, DataException
	{
		final String kFName = "cacheVdbVersion";

		File pdd = getCachedPddFile();

		VBrowseSession browser = null;
		String answer = null;
		try {
			// Browse the vdb
			browser = new VBrowseSession( pdd, mVBin );
			// browser.getFieldValue( 0, "_pdd_partpath" );
			answer = browser.getFieldValue( 0, "_dbversion" );
		}
		finally {
			if( null!=browser )
				browser.closeSession();
		}

		if( null==answer )
			throw new DataException(
				"Null/empty pdd version field for pdd=\"" + pdd + "\""
				);

		infoMsg( kFName, "pdd version = " + answer );

		cVdbVersion = answer;

		return answer;
	}

	private static void __sep__Dates__also_Fast__() {}
	/////////////////////////////////////////////////////////

	public long getMostRecentCollectionDateInMS()
		throws IOException, DataException
	{
		File pddFile = getCachedPddFile();
		// ^^^ also checks for missing file
		long outDate = pddFile.lastModified();
		if( outDate < 1L )
			throw new IOException( "Error getting PDD date of " + pddFile );

		return outDate;
		// could also ask partitions
	}

	public String getMostRecentCollectionDateAsFormattedString()
		throws IOException, DataException
	{
		long lastMod = getMostRecentCollectionDateInMS();
		return NIEUtil.formatDateToString( lastMod );
	}
	

	public long getCollectionAgeInMS()
		throws IOException, DataException
	{
		final String kFName = "getCollectionAgeInMS";

		long now = NIEUtil.getCurrTimeMillis();
		long then = getMostRecentCollectionDateInMS();
		if( then > now )
			warningMsg( kFName, "Warning: collection file date seems newer than system clock; server clocks out of sync?");
		return now - then;
	}

	public double getCollectionAgeInDaysUnrounded()
		throws IOException, DataException
	{
		long age = getCollectionAgeInMS();
		return (double) age / (double) NIEUtil.MS_PER_DAY; 
	}
	public double getCollectionAgeInDaysRounded()
		throws IOException, DataException
	{
		long age = getCollectionAgeInMS();
		double outAnswer = (double) age / (double) NIEUtil.MS_PER_DAY;
		outAnswer = NIEUtil.formatDoubleToDisplayPrecision( outAnswer );
		return outAnswer;
	}
	public String getCollectionAgeAsFormattedString()
		throws IOException, DataException
	{
		long age = getCollectionAgeInMS();
		return NIEUtil.formatTimeIntervalToNDaysFromMS( age, true );
	}


	private static void __sep__Slow_Items__(){}
	///////////////////////////////////////////////////////////

	String getSlowCollectionSummary( )
		throws IOException, DataException
	{
		StringBuffer outSum = new StringBuffer();

		outSum.append("Collection: ").append( getShortName() ).append( " at " );
		outSum.append( getDirectory() );
		outSum.append( NIEUtil.NL );

		outSum.append( "\tupdated: " ).append( getMostRecentCollectionDateAsFormattedString() );
		outSum.append( " (" ).append( getCollectionAgeAsFormattedString() ).append( ')' );
		outSum.append( NIEUtil.NL );

		outSum.append( "\tstats: paritions=" ).append( getParitionCount() );
		outSum.append( " active_docs=" ).append( getActiveDocCount() );
		outSum.append( " raw_docs=" ).append( getRawDocCount() );
		outSum.append( " efficiency_score=").append( calcStorageEfficiencyStr() );
		outSum.append( NIEUtil.NL );

		outSum.append("\ttitles:");

		outSum.append( " BLANK=").append( getBlankTitleCount( ) );
		outSum.append( " populated=").append( getNonBlankTitleCount( ) );
		outSum.append( " populated_score=").append( calcPopulatedTitleRatioStr( ) );
		outSum.append( NIEUtil.NL );


		outSum.append( "\t\tunique=").append( getUniqueTitleCount( ) );
		outSum.append( " duplicate=").append( getDuplicateTitleCount( ) );
		outSum.append( " uniqueness_score=").append( calcUniqueTitleRatioStr( ) );

		return new String( outSum );
	}


	public int getActiveDocCount()
		throws DataException, IOException
	{
		if( cActiveDocCount >= 0 )
			return cActiveDocCount;
		int outAnswer = 0;
		List parts = getCachedParitionObjects();
		// ^^^ will complain if none
		for( Iterator it = parts.iterator(); it.hasNext() ; ) {
			Partition part = (Partition) it.next();
			outAnswer += part.getActiveDocCount();
		}
		cActiveDocCount = outAnswer;
		return cActiveDocCount;
	}

	public int getNonBlankTitleCount( )
		throws DataException, IOException
	{
		if( cNonBlankTitleCount >= 0 )
			return cNonBlankTitleCount;
		int outAnswer = 0;
		List parts = getCachedParitionObjects();
		// ^^^ will complain if none
		for( Iterator it = parts.iterator(); it.hasNext() ; ) {
			Partition part = (Partition) it.next();
			outAnswer += part.getNonBlankTitleCount( );
		}
		cNonBlankTitleCount = outAnswer;
		return outAnswer;
	}

	public int getBlankTitleCount( )
		throws DataException, IOException
	{
		if( cBlankTitleCount >= 0 )
			return cBlankTitleCount;
		int outAnswer = 0;
		List parts = getCachedParitionObjects();
		// ^^^ will complain if none
		for( Iterator it = parts.iterator(); it.hasNext() ; ) {
			Partition part = (Partition) it.next();
			outAnswer += part.getBlankTitleCount( );
		}
		cBlankTitleCount = outAnswer;
		return outAnswer;
	}


	public int getDuplicateTitleCount( )
		throws IOException, DataException
	{
		if( cDuplicateTitleCount >= 0 )
			return cDuplicateTitleCount;

		if( null == cSequentialTitles || null == cSequentialUniqueTitles
			|| null == cUniqueTitleCounts
		) {
			cacheTitles( );
		}

		int answer = 0;
		for( Iterator it = cUniqueTitleCounts.keySet().iterator() ; it.hasNext() ; ) {
			String title = (String) it.next();
			Integer countObj = (Integer) cUniqueTitleCounts.get(title);
			int count = countObj.intValue();
			if( count > 1 ) {
				if( ! title.equals("") && ! title.equals(Partition.NULL_FIELD_MARKER) )
					answer += count;
			}
		}

		cDuplicateTitleCount = answer;
		return answer;
	}

	public Map getDuplicateTitlesByCount( )
		throws IOException, DataException
	{
		return getTitlesByCount(
			false,	// boolean inIncludeBlanks
			true	// boolean inDupesOnly
			);
	}

	public Map getTitlesByCount( boolean inIncludeBlanks, boolean inDupesOnly )
		throws IOException, DataException
	{

		if( null == cSequentialTitles || null == cSequentialUniqueTitles
			|| null == cUniqueTitleCounts
		) {
			cacheTitles( );
		}

		Hashtable outHash = new Hashtable();

		int answer = 0;
		for( Iterator it = cUniqueTitleCounts.keySet().iterator() ; it.hasNext() ; ) {
			String title = (String) it.next();

			// Skip blanks if asked to do so
			if( ! inIncludeBlanks
				&& ( title.equals("") || title.equals(Partition.NULL_FIELD_MARKER) )
			) {
				continue;
			}

			Integer countObj = (Integer) cUniqueTitleCounts.get(title);
			int count = countObj.intValue();

			// Skip unique / non-dupe titles if asked to do so
			if( inDupesOnly && count < 2 )
				continue;

			List titlesForCount = null;
			if( outHash.containsKey(countObj) )
				titlesForCount = (List) outHash.get(countObj);
			else
				titlesForCount = new ArrayList();
			titlesForCount.add( title );
			outHash.put( countObj, titlesForCount );

		}

		return outHash;
	}


	public int getUniqueTitleCount()
		throws IOException, DataException
	{
		if( cUniqueTitleCount >= 0 )
			return cUniqueTitleCount;

		if( null == cSequentialTitles || null == cSequentialUniqueTitles
			|| null == cUniqueTitleCounts
		) {
			cacheTitles( );
		}

		int answer = 0;
		for( Iterator it = cUniqueTitleCounts.keySet().iterator() ; it.hasNext() ; ) {
			String title = (String) it.next();
			Integer countObj = (Integer) cUniqueTitleCounts.get(title);
			int count = countObj.intValue();
			if( count == 1 ) {
				if( ! title.equals("") && ! title.equals(Partition.NULL_FIELD_MARKER) )
					answer += count;
			}
		}

		cUniqueTitleCount = answer;
		return answer;
	}

	public Map getTitlesByLength( )
		throws IOException, DataException
	{

		if( null == cSequentialTitles || null == cSequentialUniqueTitles
			|| null == cUniqueTitleCounts
		) {
			cacheTitles( );
		}

		Hashtable outHash = new Hashtable();

		int answer = 0;
		for( Iterator it = cUniqueTitleCounts.keySet().iterator() ; it.hasNext() ; ) {
			String title = (String) it.next();


			Integer lenObj = null;
			if( null==title || title.equals("") || title.equals(Partition.NULL_FIELD_MARKER) )
				lenObj = new Integer( 0 );
			else
				lenObj = new Integer( title.length() );

			List titlesForLen = null;
			if( outHash.containsKey( lenObj ) )
				titlesForLen = (List) outHash.get( lenObj );
			else
				titlesForLen = new ArrayList();
			titlesForLen.add( title );
			outHash.put( lenObj, titlesForLen );

		}

		return outHash;
	}






	void cacheTitles( )
		throws IOException, DataException
	{
		cSequentialTitles = new ArrayList();
		cSequentialUniqueTitles = new ArrayList();
		cUniqueTitleCounts = new Hashtable();
		cUniqueTitleKeys = new Hashtable();

		List parts = getCachedParitionObjects();
		// ^^^ will complain if none
		for( Iterator it = parts.iterator(); it.hasNext() ; ) {
			Partition part = (Partition) it.next();
			part.addMyTitlesToMasterCache( cSequentialTitles,
				cSequentialUniqueTitles, cUniqueTitleCounts, cUniqueTitleKeys
				);
		}
	}








	private static void __sep__Slow_Formatted_Percentages__() {}
	//////////////////////////////////////////////////////////////

	public String calcStorageEfficiencyStr()
		throws IOException, DataException
	{
		return NIEUtil.formatPercentage(
			getActiveDocCount(),
			getRawDocCount(),
			PERCENT_DECIMAL_PLACES
			);
	}
	public double calcStorageEfficiency()
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getActiveDocCount(),
			getRawDocCount(),
			PERCENT_DECIMAL_PLACES
			);
	}

	public String calcPopulatedTitleRatioStr( )
		throws IOException, DataException
	{
		return NIEUtil.formatPercentage(
			getNonBlankTitleCount( ),
			( getNonBlankTitleCount( ) + getBlankTitleCount( ) ),
			PERCENT_DECIMAL_PLACES
			);
	}
	public double calcPopulatedTitleRatio( )
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getNonBlankTitleCount( ),
			( getNonBlankTitleCount( ) + getBlankTitleCount( ) ),
			PERCENT_DECIMAL_PLACES
			);
	}

	public String calcBlankTitleRatioStr( )
		throws IOException, DataException
	{
		return NIEUtil.formatPercentage(
			getBlankTitleCount( ),
			( getNonBlankTitleCount( ) + getBlankTitleCount( ) ),
			PERCENT_DECIMAL_PLACES
			);
	}
	public double calcBlankTitleRatio( )
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getBlankTitleCount( ),
			( getNonBlankTitleCount( ) + getBlankTitleCount( ) ),
			PERCENT_DECIMAL_PLACES
			);
	}


	public String calcUniqueTitleRatioStr( )
		throws IOException, DataException
	{
		return NIEUtil.formatPercentage(
			getUniqueTitleCount( ),
			getNonBlankTitleCount( ),
			PERCENT_DECIMAL_PLACES
			);
	}
	public double calcUniqueTitleRatio( )
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getUniqueTitleCount( ),
			getNonBlankTitleCount( ),
			PERCENT_DECIMAL_PLACES
			);
	}


	public String calcDuplicateTitleRatioStr( )
		throws IOException, DataException
	{
		return NIEUtil.formatPercentage(
			getDuplicateTitleCount( ),
			getNonBlankTitleCount( ),
			PERCENT_DECIMAL_PLACES
			);
	}
	public double calcDuplicateTitleRatio( )
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getDuplicateTitleCount( ),
			getNonBlankTitleCount( ),
			PERCENT_DECIMAL_PLACES
			);
	}



	private static void __sep__XML_Reporting__() {}
	///////////////////////////////////////////////////////////


	Element generateCollectionStatistics(
			Element optIoAppendToReport,
			boolean inDoDetailedReport,
			boolean inIncludeIndividualPartitionStats,
			boolean inIncludeSpecificTitles
		)
		throws IOException, DataException
	{
		final String kFName = "generateCollectionStatistics";

		int whichColumn = 1;
		// Create a new report
		if( null==optIoAppendToReport ) {
			optIoAppendToReport = new Element( RPT_ELEM );	// report
			optIoAppendToReport.setAttribute( RPT_RUN_DATE_ATTR,
				NIEUtil.getTimestamp()
				);
			optIoAppendToReport.setAttribute( RPT_COL_COUNTER_ATTR, "1" );
		}
		// Add another column to an existing report
		else {
			whichColumn = JDOMHelper.getIntFromAttribute(
				optIoAppendToReport, RPT_COL_COUNTER_ATTR, -1
				);
			if( whichColumn < 1 )
				throw new DataException( "Missing/invalid column counter in report element" );
			whichColumn++;
			optIoAppendToReport.setAttribute( RPT_COL_COUNTER_ATTR, ""+whichColumn );
		}

		// Basic

		addStatisticToReport( optIoAppendToReport, whichColumn,
			"Collection",
			getShortName()
			);
		addStatisticToReport( optIoAppendToReport, whichColumn,
			"Directory",
			""+getDirectory()
			// , 1,
			// "small"
			);
		addStatisticToReport( optIoAppendToReport, whichColumn,
			"Level of Details",
			( inDoDetailedReport ? "thorough" : "quick" )
			+ ( inIncludeIndividualPartitionStats ? " w/parts" : "" )
			);

		// age
		double daysOut = getCollectionAgeInDaysRounded();
		String daysOutLevel = null;
		if( daysOut > 90 )
			daysOutLevel = "red";
		else if( daysOut > 7 )
			daysOutLevel = "yellow";

		addStatisticToReport( optIoAppendToReport, whichColumn,
			"Last Update",
			getMostRecentCollectionDateAsFormattedString(),
			1,
			daysOutLevel
			);

		addStatisticToReport( optIoAppendToReport, whichColumn,
			"How Long",
			getCollectionAgeAsFormattedString(),
			1,
			daysOutLevel
			);

		// Stats

		addStatisticToReport( optIoAppendToReport, whichColumn,
			"Collection Version",
			getVdbVersion()
			);

		// Titles
		if( inDoDetailedReport ) {

		    addStatisticToReport( optIoAppendToReport, whichColumn,
					"Title field",
					mTitleField
					);

		    addStatisticToReport( optIoAppendToReport, whichColumn,
				"Populated Titles",
				""+getNonBlankTitleCount( )
				);

			double blankRate = calcBlankTitleRatio();
			String blankRateLevel = null;
			if( blankRate >= 20.0 )
				blankRateLevel = "red";
			else if( blankRate >= 5.0 )
				blankRateLevel = "yellow";

			addStatisticToReport( optIoAppendToReport, whichColumn,
				"BLANK Titles",
				""+getBlankTitleCount( ),
				1,
				blankRateLevel
				);

			addStatisticToReport( optIoAppendToReport, whichColumn,
				"Populated Title Score",
				calcPopulatedTitleRatioStr( ),
				1,
				blankRateLevel
				);


			addStatisticToReport( optIoAppendToReport, whichColumn,
				"Unique Titles",
				""+getUniqueTitleCount( )
				);

			double dupeRate = calcDuplicateTitleRatio( );
			String dupeRateLevel = null;
			if( dupeRate >= 25.0 )
				dupeRateLevel = "red";
			else if( dupeRate >= 10.0 )
				dupeRateLevel = "yellow";

			addStatisticToReport( optIoAppendToReport, whichColumn,
				"DUPLICATE Titles",
				""+getDuplicateTitleCount( ),
				1,
				dupeRateLevel
				);

			addStatisticToReport( optIoAppendToReport, whichColumn,
				"Title Uniqueness Score",
				calcUniqueTitleRatioStr( ),
				1,
				dupeRateLevel
				);

		}

		// Partitions - high level
		addStatisticToReport( optIoAppendToReport, whichColumn,
			"Number of Partitions",
			""+getParitionCount()
			);

		addStatisticToReport( optIoAppendToReport, whichColumn,
			"Raw Document Records",
			""+getRawDocCount()
			);

		if( inDoDetailedReport ) {
			addStatisticToReport( optIoAppendToReport, whichColumn,
				"Active Document Records",
				""+getActiveDocCount()
				);

			double eff = calcStorageEfficiency();
			String effLevel = null;
			if( eff < 50.0 )
				effLevel = "red";
			else if( eff < 80.0 )
				effLevel = "yellow";

			addStatisticToReport( optIoAppendToReport, whichColumn,
				"Storage Efficiency Score",
				""+calcStorageEfficiencyStr(),
				1,
				effLevel
				);
		}


		// Each partition, if requested
		if( inIncludeIndividualPartitionStats ) {
			List parts = getCachedParitionObjects();
			for( int i = 0; i<parts.size() ; i++ ) {
				Partition part = (Partition) parts.get( i );
				String labelPrefix = "part " + (i+1) + ": ";

				addStatisticToReport( optIoAppendToReport, whichColumn,
					labelPrefix + "name",
					part.getShortName(),
					2,
					null
					);

				addStatisticToReport( optIoAppendToReport, whichColumn,
					labelPrefix + "raw docs",
					""+part.getRawDocCount(),
					2,
					null
					);

				// If we want details
				if( inDoDetailedReport ) {
					addStatisticToReport( optIoAppendToReport, whichColumn,
						labelPrefix + "active docs",
						""+part.getActiveDocCount(),
						2,
						null
						);

					double eff = part.calcStorageEfficiency();
					String effLevel = null;
					if( eff < 50.0 )
						effLevel = "red";
					else if( eff < 80.0 )
						effLevel = "yellow";


					// double score = part.cal
					addStatisticToReport( optIoAppendToReport, whichColumn,
						labelPrefix + "efficiency",
						""+part.calcStorageEfficiencyStr(),
						2,
						effLevel
						);
				}


			}
		}


// help me!
// statusMsg( kFName, "inIncludeSpecificTitles=" + inIncludeSpecificTitles );
		if( inIncludeSpecificTitles ) {

			// Add in specific empty title keys

			// truly null titles
			String title = Partition.NULL_FIELD_MARKER;
			if( cUniqueTitleKeys.containsKey( title ) ) {
				List keys = (List) cUniqueTitleKeys.get( title );
				Element statsElem = listKeysIntoElement( keys );
				if( null != statsElem )
					addStatisticToReport( optIoAppendToReport, whichColumn,
						"Docs with Null Titles",
						statsElem
						);

			}
			else {
				debugMsg( kFName, "no nulls" );
			}
			// blank / empty titles
			title = "";
			if( cUniqueTitleKeys.containsKey( title ) ) {
				List keys = (List) cUniqueTitleKeys.get( title );
				Element statsElem = listKeysIntoElement( keys );
				if( null != statsElem )
					addStatisticToReport( optIoAppendToReport, whichColumn,
						"Docs with Empty Titles",
						statsElem
						);
			}
			else {
				debugMsg( kFName, "no empties" );
			}

			// List duplicates
			Map dupeHashByCount = getDuplicateTitlesByCount();
			// If have some duplicates
			if( ! dupeHashByCount.isEmpty() ) {
				Element allDupesElem = new Element( "div" ); // "small" );
				List bigDupes = NIEUtil.sortHashKeysDesc( dupeHashByCount );
				// For each COUNT
				for( Iterator cit = bigDupes.iterator(); cit.hasNext() ; ) {
					Integer countObj = (Integer) cit.next();
					// Get the titles
					List dupeTitles = (List) dupeHashByCount.get( countObj );
					dupeTitles = NIEUtil.sortAsc( dupeTitles );
					// For each title
					for( Iterator tit = dupeTitles.iterator(); tit.hasNext() ; ) {
						String dupeTitle = (String) tit.next();
						// allDupesElem.addContent( "" + countObj + " instances of \"" + dupeTitle + "\"" );
						allDupesElem.addContent( "\"" + dupeTitle + "\" (found " + countObj + ")" );
						allDupesElem.addContent( new Element("br") );
						List dupeKeys = (List) cUniqueTitleKeys.get( dupeTitle );
						for( Iterator kit = dupeKeys.iterator() ; kit.hasNext() ; ) {
							String dupeKey = (String) kit.next();
							allDupesElem.addContent( dupeKey );
							allDupesElem.addContent( new Element("br") );
						}	// end for each key
						allDupesElem.addContent( new Element("br") );
					}	// end for each title
					// allDupesElem.addContent( new Element("br") );
				}	// end for each count

				addStatisticToReport( optIoAppendToReport, whichColumn,
					"Docs with Duplicate Titles",
					allDupesElem
					);


			}	// End if have some duplicates



			// Now look at the lengths of titles
			// DEFAULT_MIN_TITLE_LENGTH DEFAULT_MAX_TITLE_LENGTH
			Map lenHashByCount = getTitlesByLength();
			Element shortsElem = null;
			List lengths = NIEUtil.sortHashKeysAsc( lenHashByCount );
			// For each short Length going up
			for( Iterator lit1 = lengths.iterator(); lit1.hasNext() ; ) {
				Integer lenObj = (Integer) lit1.next();
				int len = lenObj.intValue();
				if( len < 1 )
					continue;
				if( len >= DEFAULT_MIN_TITLE_LENGTH )
					break;
				// OK, we have one
				if( null==shortsElem )
					shortsElem = new Element( "div" );

				// Get the titles
				List theTitles = (List) lenHashByCount.get( lenObj );
				theTitles = NIEUtil.sortAsc( theTitles );
				// For each title
				for( Iterator tit1 = theTitles.iterator(); tit1.hasNext() ; ) {
					String dupeTitle = (String) tit1.next();
					// allDupesElem.addContent( "" + countObj + " instances of \"" + dupeTitle + "\"" );
					shortsElem.addContent( "\"" + dupeTitle + "\" (length=" + len + ")" );
					shortsElem.addContent( new Element("br") );
					List dupeKeys = (List) cUniqueTitleKeys.get( dupeTitle );
					for( Iterator kit = dupeKeys.iterator() ; kit.hasNext() ; ) {
						String dupeKey = (String) kit.next();
						shortsElem.addContent( dupeKey );
						shortsElem.addContent( new Element("br") );
					}	// end for each key
					shortsElem.addContent( new Element("br") );
				}	// end for each title
				// allDupesElem.addContent( new Element("br") );
			}	// end for each short length

			if( null!=shortsElem )
				addStatisticToReport( optIoAppendToReport, whichColumn,
					"Short Titles (< " + DEFAULT_MIN_TITLE_LENGTH + ")",
					shortsElem
					);


			Element longsElem = null;
			lengths = NIEUtil.sortHashKeysDesc( lenHashByCount );
			// For each short Length going up
			for( Iterator lit2 = lengths.iterator(); lit2.hasNext() ; ) {
				Integer lenObj = (Integer) lit2.next();
				int len = lenObj.intValue();
				if( len <= DEFAULT_MAX_TITLE_LENGTH )
					break;
				// OK, we have one
				if( null==longsElem )
					longsElem = new Element( "div" );

				// Get the titles
				List theTitles = (List) lenHashByCount.get( lenObj );
				theTitles = NIEUtil.sortAsc( theTitles );
				// For each title
				for( Iterator tit2 = theTitles.iterator(); tit2.hasNext() ; ) {
					String dupeTitle = (String) tit2.next();
					// allDupesElem.addContent( "" + countObj + " instances of \"" + dupeTitle + "\"" );
					longsElem.addContent( "\"" + dupeTitle + "\" (length=" + len + ")" );
					longsElem.addContent( new Element("br") );
					List dupeKeys = (List) cUniqueTitleKeys.get( dupeTitle );
					for( Iterator kit = dupeKeys.iterator() ; kit.hasNext() ; ) {
						String dupeKey = (String) kit.next();
						longsElem.addContent( dupeKey );
						longsElem.addContent( new Element("br") );
					}	// end for each key
					longsElem.addContent( new Element("br") );
				}	// end for each title
				// allDupesElem.addContent( new Element("br") );
			}	// end for each short length

			if( null!=longsElem )
				addStatisticToReport( optIoAppendToReport, whichColumn,
					"Long Titles (> " + DEFAULT_MAX_TITLE_LENGTH + ")",
					longsElem
					);




		}	// End if doing title checking



		return optIoAppendToReport;
	}


	Element listKeysIntoElement( List inKeys )
	{
		Element statElem = null;
		if( null!=inKeys && ! inKeys.isEmpty() ) {
			statElem = new Element( "div" ); // "small" );
			int howMany = inKeys.size();
			statElem.addContent( "Listing " + howMany + " key" + (howMany!=1?"s":"") + ":" );
			statElem.addContent( new Element("br") );
			for( Iterator kit = inKeys.iterator(); kit.hasNext(); ) {
				String key = (String) kit.next();
				statElem.addContent( key );
				statElem.addContent( new Element("br") );
			}
		}
		return statElem;
	}


	// Add a string value to the report
	private void addStatisticToReport( Element inReportElem, int inTargetColumn,
		String inStatName, String inStatValue
		)
			throws DataException
	{
		addStatisticToReport( inReportElem, inTargetColumn,
				inStatName, inStatValue,
				0, null
				);
	}

	// Add an arbitrary element to the report
	private void addStatisticToReport( Element inReportElem, int inTargetColumn,
		String inStatName, Element inStatValue
		)
			throws DataException
	{
		addStatisticToReport( inReportElem, inTargetColumn,
				inStatName, inStatValue,
				0, null
				);
	}


	// Add a string
	private void addStatisticToReport( Element inReportElem, int inTargetColumn,
		String inStatName, String inStatValue,
		int inSubLevel, String optSeverity
		)
			throws DataException
	{
		final String kFName = "addStatisticToReport";

		if( null==inReportElem || null==inStatName || null==inStatValue )
			throw new DataException( "Null input(s)" );
		if( inTargetColumn < 1 )
			throw new DataException( "Invalid target column " + inTargetColumn + " (reminder: 1-based)" );

		/*** take 1
		if( inStatName.indexOf('/')>=0 || inStatName.indexOf('\\')>=0
			|| inStatName.indexOf('"')>=0 || inStatName.indexOf('=')>=0
			|| inStatName.indexOf('@')>=0
			|| inStatName.indexOf('[')>=0 || inStatName.indexOf(']')>=0
		) {
			throw new DataException(
				"Invalid character in statistic name \"" + inStatName + "\""
				+ " ( must not contain /, \\, @, =, \", [ or ] )"
				);
		}
		***/

		// Find the statistic row for this named statistic
		/***
		String yPath1 = RPT_STATISTIC_ELEM
			+ "/@" + RPT_STATISTIC_LABEL_ATTR + "=" + inStatName
			// + '/' + RPT_DATA_ELEM + '[' + inTargetColumn + ']'
			;
		statusMsg( kFName, "ypath1=" + yPath1 );
		Element statElem = JDOMHelper.findElementByPath( inReportElem, yPath1, true );
		***/

		/*** take 2
		Element statElem = null;
		List children = inReportElem.getChildren( RPT_STATISTIC_ELEM );
		for( Iterator it = children.iterator() ; it.hasNext() ; ) {
			Element child = (Element) it.next();
			String label = child.getAttributeValue( RPT_STATISTIC_LABEL_ATTR );
			if( null!=label && label.equals(inStatName) ) {
				statElem = child;
				break;
			}
		}
		if( null==statElem ) {
			statElem = new Element( RPT_STATISTIC_ELEM );
			statElem.setAttribute( RPT_STATISTIC_LABEL_ATTR, inStatName );
			inReportElem.addContent( statElem );

			// nie_stat_label_cell + _level_2
			String cssClass = CSSClassNames.LABEL_CELL;
			if( inSubLevel > 0 )
				cssClass += CSSClassNames.LABEL_LEVEL_SUFFIX + inSubLevel;
			statElem.setAttribute( "class", cssClass );
		}


		String yPath2 = RPT_DATA_ELEM + '[' + inTargetColumn + ']' ;

		// statusMsg( kFName, "ypath2=" + yPath2 );

		Element targetElem = JDOMHelper.findOrCreateElementByPath(
			statElem, yPath2, true
			);

		if( null==targetElem )
			throw new DataException(
				"Unable to add reprot statistic to XML tree, ypath=" + yPath2
				);

		targetElem.addContent( inStatValue );
		// nie_stat_value_cell + _severity_ yellow
		String cssClass = CSSClassNames.DATA_CELL;
		if( null!=optSeverity )
			cssClass += CSSClassNames.DATA_SEVERITY_SUFFIX + optSeverity;
		targetElem.setAttribute( "class", cssClass );
		***/

		// take 3
		Element statElem = new Element( "div" );
		statElem.addContent( inStatValue );
		addStatisticToReport( inReportElem, inTargetColumn,
			   inStatName, statElem,
			   inSubLevel, optSeverity
			   );

	}


	// Add an element
	private void addStatisticToReport( Element inReportElem, int inTargetColumn,
		String inStatName, Element inStatValue,
		int inSubLevel, String optSeverity
		)
			throws DataException
	{
		final String kFName = "addStatisticToReport";

		if( null==inReportElem || null==inStatName || null==inStatValue )
			throw new DataException( "Null input(s)" );
		if( inTargetColumn < 1 )
			throw new DataException( "Invalid target column " + inTargetColumn + " (reminder: 1-based)" );

		Element statElem = null;
		List children = inReportElem.getChildren( RPT_STATISTIC_ELEM );
		for( Iterator it = children.iterator() ; it.hasNext() ; ) {
			Element child = (Element) it.next();
			String label = child.getAttributeValue( RPT_STATISTIC_LABEL_ATTR );
			if( null!=label && label.equals(inStatName) ) {
				statElem = child;
				break;
			}
		}
		if( null==statElem ) {
			statElem = new Element( RPT_STATISTIC_ELEM );
			statElem.setAttribute( RPT_STATISTIC_LABEL_ATTR, inStatName );
			inReportElem.addContent( statElem );

			// nie_stat_label_cell + _level_2
			String cssClass = CSSClassNames.LABEL_CELL;
			if( inSubLevel > 0 )
				cssClass += CSSClassNames.LABEL_LEVEL_SUFFIX + inSubLevel;
			statElem.setAttribute( "class", cssClass );
		}


		String yPath2 = RPT_DATA_ELEM + '[' + inTargetColumn + ']' ;

		// statusMsg( kFName, "ypath2=" + yPath2 );

		Element targetElem = JDOMHelper.findOrCreateElementByPath(
			statElem, yPath2, true
			);

		if( null==targetElem )
			throw new DataException(
				"Unable to add reprot statistic to XML tree, ypath=" + yPath2
				);

		targetElem.addContent( inStatValue );
		// nie_stat_value_cell + _severity_ yellow
		String cssClass = CSSClassNames.DATA_CELL;
		if( null!=optSeverity )
			cssClass += CSSClassNames.DATA_SEVERITY_SUFFIX + optSeverity;
		targetElem.setAttribute( "class", cssClass );


	}



	static Element formatCollectionStatistics( Element inXMLReport )
		throws DataException, IOException
	{
		final String kFName = "formatCollectionStatistics";

		// Get compiled and cached XSLT
		Transformer formatter = getCompiledXSLTDoc();
		if( formatter == null )
			throw new IOException( "Could not obtain XSLT formatting rules" );

		// debugMsg( kFName, "Formatting XML ..." );	

		Hashtable values = new Hashtable();
		values.put( "css_text", getCssStyleText() );
		// ^^^ returns "" if none

		// Now Transform it!
		// We have no parameters hash, and since we're already
		// working with CLONED data, it's OK to not have them reclone
		Document newJDoc = null;
		try {
			newJDoc = JDOMHelper.xsltElementToDoc(
				inXMLReport,
				formatter,
				values,
				false	// do cloning, not needed here
				);
		}
		catch( JDOMHelperException e ) {
			throw new DataException( "JDOM Exception: " + e );
		}

		if( null == newJDoc )
			throw new DataException( "Got back null document from XSLT formatter." );

		return newJDoc.getRootElement();
	}

	static Transformer getCompiledXSLTDoc()
		throws DataException, IOException
	{
		if( null == cTransformer )
		{
			final String kFName = "getCompiledXSLTDoc";
			try
			{
				byte [] contents = getXSLTTextAsBytes();
				cTransformer = JDOMHelper.compileXSLTString( contents );
			}
			catch(Exception e)
			{
				String msg = "Error getting / compiling XSLT: " + e;
				staticErrorMsg( kFName, msg );
				throw new DataException( msg );
			}
		}
		return cTransformer;
	}

	// Get the XSLT text from either the literal text
	// or from a file
	// results cached by calling routine
	static byte [] getXSLTTextAsBytes()
		throws IOException
	{
		final String kFName = "getXSLTTextAsBytes";

		AuxIOInfo tmpAuxInfo = new AuxIOInfo();
		// Resolve system URLs relative to the main application config
		tmpAuxInfo.setSystemRelativeBaseClassName( CSS_BASE_CLASS_NAME );

		byte [] contents = NIEUtil.fetchURIContentsBin(
			XSLT_URI,
			null,			// optRelativeRoot,
			null, null,	// optUsername, optPassword,
			tmpAuxInfo,	// inoutAuxIOInfo
			false
			);

		if( contents.length < 1 )
			throw new IOException(
				"No XSLT formatting specified."
				+ " Must set literal text or location URI attribute."
				);

		return contents;
	}
	




	// Not getting a style sheet should not be a fatal error
	// though we will log error messages
	// Will return "" if has problem, won't keep retrying
	static String getCssStyleText()
	{
		if( null == cCssText ) {
			final String kFName = "getCssStyleText";

			AuxIOInfo tmpAuxInfo = new AuxIOInfo();
			// Resolve system URLs relative to the main application config
			tmpAuxInfo.setSystemRelativeBaseClassName( CSS_BASE_CLASS_NAME );
			try
			{
				cCssText = NIEUtil.fetchURIContentsChar(
					CSS_URI,
					null,			// baseURI
					null, null,	// optUsername, optPassword,
					tmpAuxInfo,
					false			// Use POST
					);
			}
			catch( Exception e )
			{
				staticErrorMsg( kFName,
					"Error opening CSS URI \"" + CSS_URI + "\"."
					+ " Returning null."
					+ " Error: " + e
					);
				cCssText = null;
			}

			// Normalize and check
			cCssText = NIEUtil.trimmedStringOrNull( cCssText );
			if( null==cCssText ) {
				staticErrorMsg( kFName,
					"Null/empty default CSS style sheet contents read"
					+ " from URI \"" + CSS_URI + "\", returning null."
					);
				cCssText = "";
			}

			// debugMsg( kFName, "CSS=" + cCssText );

		}

		return cCssText;


		// Good resource on tables and CSS, from Nick Sayer
		// http://www.w3.org/TR/REC-CSS2/tables.html
		// And overall CSS info
		// http://www.w3.org/TR/REC-CSS2/cover.html#minitoc
		// Selectors / pattern matching
		// http://www.w3.org/TR/REC-CSS2/selector.html

//		Inside HTML:
//		<head>
//			...
//			<STYLE type="text/css">
//				H1 { color: blue }
//			</STYLE>
//		</head>

	}





	private static void __sep__Partitions__() {}
	/////////////////////////////////////////////////////////


	List getCachedParitionObjects()
		throws DataException
	{
		if( null==cPartitions || cPartitions.isEmpty() )
			throw new DataException( "Null/empty parition object list" );
		return cPartitions;
	}


	List openAndCacheAllParitions()
		throws DataException, IOException
	{
		// File pddDir = getPddDirectory();
		List partNames = getCachedActiveParitionNames();
		cPartitions = new ArrayList();
		// For each partition
		for( Iterator it = partNames.iterator() ; it.hasNext() ; ) {
			String partName = (String) it.next();
			Partition part = new Partition( this, partName, mVBin, mTitleField );
			cPartitions.add( part );
		}
		if( null==cPartitions || cPartitions.isEmpty() )
			throw new DataException( "Null/empty parition object list" );
		return cPartitions;
	}

	List getCachedActiveParitionNames()
		throws DataException
	{
		if( null==cPartitionPaths || cPartitionPaths.isEmpty() )
			throw new DataException( "Null/empty parition path list" );
		return cPartitionPaths;
	}

	List cacheActivePartitionNames()
		throws DataException, IOException
	{
		final String kFName = "cacheActivePartitionNames";

		File pdd = getCachedPddFile();

		VBrowseSession browser = null;
		try {
			// Browse the vdb
			browser = new VBrowseSession( pdd, mVBin );
			// browser.getFieldValue( 0, "_pdd_partpath" );
			cPartitionPaths = browser.getUnfilteredFieldValues( "_pdd_partpath", true );
		}
		finally {
			browser.closeSession();
		}

		if( null==cPartitionPaths || cPartitionPaths.isEmpty() )
			throw new DataException( "Null/empty parition list" );

		infoMsg( kFName, "Partition paths = " + cPartitionPaths );

		return cPartitionPaths;
	}

	File getCachedPddFile()
		throws DataException
	{
		if( null==cPddFile )
			throw new DataException( "Null PDD file" );
		return cPddFile;
	}

	File cachePddFile()
		throws DataException
	{
		if( null==mCollDir )
			throw new DataException( "Null collection directory." );

		File pddDir = new File( mCollDir, "pdd" );
		if( null==pddDir || ! pddDir.exists() || ! pddDir.isDirectory() )
			throw new DataException(
				"Invalid / missing pdd directory in collection \""
				+ mCollDir.getAbsolutePath() + "\""
				);

		List pddFiles = new ArrayList();
		File [] candidateFiles = pddDir.listFiles();
		for( int i=0; i<candidateFiles.length; i++ ) {
			File candidateFile = candidateFiles[i];
			String tmpName = candidateFile.getName();
			if( null==tmpName || tmpName.length() < 1 )
				continue;
			// First character should be a digit
			char firstChar = tmpName.charAt(0);
			if( firstChar < '0' || firstChar > '9' )
				continue;
			// and end in .pdd
			tmpName = tmpName.toLowerCase();
			if( ! tmpName.endsWith(".pdd") )
				continue;
			// ok, add it
			pddFiles.add( candidateFile );
		}

		if( pddFiles.isEmpty() )
			throw new DataException(
				"No active pdd files pdd directory \""
				+ pddDir.getAbsolutePath() + "\""
				);

		cPddFile = (File) pddFiles.get( pddFiles.size()-1 );

		return cPddFile;
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


	private boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			// inMessage
			getShortName() + ": " + inMessage
			);
	}
	private static boolean staticStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	private boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			// inMessage
			getShortName() + ": " + inMessage
			);
	}


	private boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName, inFromRoutine );
	}


	private boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			// inMessage
			getShortName() + ": " + inMessage
			);
	}
	private static boolean staticInfoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	private boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
	}


	private boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			// inMessage
			getShortName() + ": " + inMessage
			);
		// return getRunLogObject().statusMsg( kClassName, inFromRoutine,
		// 	"DEBUG: " + inMessage
		// 	);
	}


	private boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName, inFromRoutine,
			// inMessage
			getShortName() + ": " + inMessage
			);
	}


	private boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName, inFromRoutine );
	}


	private boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName, inFromRoutine,
			// inMessage
			getShortName() + ": " + inMessage
			);
	}


	private boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			// inMessage
			getShortName() + ": " + inMessage
			);
	}
	private static boolean staticErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	private boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			// inMessage
			getShortName() + ": " + inMessage
			);
	}



	private static void __sep__Member_Fields_and_Constants__() {}



	File mCollDir;
	File cPddFile;
	List cPartitionPaths;
	List cPartitions;

	static String cCssText;
	static final String CSS_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "nie_checkup.css"
		;
	static final String XSLT_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "format_report.xslt"
		;
	static final String CSS_BASE_CLASS_NAME = "nie.checkup.CSSClassNames";

	// Optional path to Verity binaries
	File mVBin;
	String mTitleField = Partition.DEFAULT_TITLE_FIELD;

	static Transformer cTransformer;

	String cVdbVersion;

	int cRawDocCount = -1;
	int cActiveDocCount = -1;
	int cBlankTitleCount = -1;
	int cNonBlankTitleCount = -1;
	int cUniqueTitleCount = -1;
	int cDuplicateTitleCount = -1;

	List cSequentialTitles;
	List cSequentialUniqueTitles;
	Hashtable cUniqueTitleCounts;
	Hashtable cUniqueTitleKeys;

	public static final String DEFAULT_OUT_FILE = "checkup.html";

	public static final int PERCENT_DECIMAL_PLACES = 2;

	public static final String RPT_ELEM = "report";
	public static final String RPT_RUN_DATE_ATTR = "run_date";
	public static final String RPT_COL_COUNTER_ATTR = "last_column_added";
	public static final String RPT_STATISTIC_ELEM = "statistic";
	public static final String RPT_STATISTIC_LABEL_ATTR = "label";
	public static final String RPT_DATA_ELEM = "data_element";

	public static final int DEFAULT_MIN_TITLE_LENGTH = 10; //5;
	public static final int DEFAULT_MAX_TITLE_LENGTH = 80; //40;//100;

}
