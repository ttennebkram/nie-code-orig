package nie.checkup.verity;

import java.util.*;
import java.io.*;

import nie.core.*;
import nie.checkup.*;

public class Partition {

	public static final String kClassName = "Partition";

	// public Partition( File inPddDir, String inPartPath )
	public Partition( Collection inParentCollection, String inPartPath,
	        File optVerityBinDirectory, String optTitleField )
		throws DataException, IOException
	{
		final String kFName = "constructor";
		// if( null==inPddDir )
		//		throw new DataException( "Null collection directory passed in." );
		if( null==inParentCollection )
			throw new DataException( "Null parent collection passed in." );
		mParentCollection = inParentCollection;
		mVBin = optVerityBinDirectory;
		optTitleField = NIEUtil.trimmedStringOrNull( optTitleField );
		if( null!=optTitleField )
		    mTitleField = optTitleField;

		File pddDir = mParentCollection.getPddDirectory();

		if( null==inPartPath )
			throw new DataException( "Null partition path passed in." );

		mShortName = new File( inPartPath ).getName();

		mDddFile = new File( pddDir, inPartPath + ".ddd" );
		// Reduce out the "../" stuff
		mDddFile = mDddFile.getCanonicalFile();

		cacheIDStr();

		infoMsg( kFName, "Opening partition " + mDddFile );

		cacheBaseFieldInfo();
	}

	Collection getCollection()
		throws DataException
	{
		if( null==mParentCollection )
			throw new DataException( "Null parent collection" );
		return mParentCollection;
	}

	void cacheBaseFieldInfo()
		throws DataException, IOException
	{
		if( null==mDddFile )
			throw new DataException( "Null partition file" );

		cFieldNameList = new ArrayList();
		cFieldNameSet = new HashSet();
		cFieldNameRawCounts = new Hashtable();

		// Browse the vdb
		VBrowseSession browser = new VBrowseSession( mDddFile, mVBin );
		// browser.getFieldValue( 0, "_pdd_partpath" );
		browser.cacheFieldsInfo( cFieldNameList, cFieldNameSet, cFieldNameRawCounts );
		browser.closeSession();

		if( null==cFieldNameList || cFieldNameList.isEmpty() )
			throw new DataException( "Null/empty field list" );

		// System.out.println( "Fields = " + cFieldNameList );
		// System.out.println( "Field/values = " + cFieldNameRawCounts );

		cacheCommonFieldStats();


		// Also cache the raw doc count
		cacheRawDocCount();


		// printFieldsSummary();
	}

	public void printFieldsSummary()
	{
		final String kFName = "printFieldsSummary";
		if( null==cDocFieldNamesCommon || null==cPartFieldNamesCommon || null==cFieldNamesSystem ) {
			warningMsg( kFName, "categorized field lists not initialized: please run cacheBaseFieldInfo()");
			return;
		}

		statusMsg( kFName, "\nCommon document fields = \n"
			+ cDocFieldNamesCommon
			);
		statusMsg( kFName, "\nCommon partition fields = \n"
			+ cPartFieldNamesCommon
			);
		statusMsg( kFName, "\nSystem fields = \n"
			+ cFieldNamesSystem
			);


	}

	private static final void __sep__Fast_Items__(){}
	////////////////////////////////////////////////////////////////


	public String getShortName()
	{
		return mShortName;
	}

	void cacheIDStr()
		throws IOException, DataException
	{
		String collName = getCollection().getShortName();
		String partName = getShortName();
		if( null==partName )
			throw new DataException( "Null partition name" );
		cIDStr = collName + '.' + partName;
	}

	public String getIDStr()
	{
		return cIDStr;
	}

	public int getRawDocCount()
	{
		return cRawDocCount;
	}


	private static final void __sep__Slow_Items__(){}
	////////////////////////////////////////////////////////////////

	public int getActiveDocCount()
		throws IOException, DataException
	{
		if( cActiveDocCount < 0 )
			cacheActiveDocCountByTitle();
		return cActiveDocCount;
	}

	public String calcStorageEfficiencyStr()
		throws IOException, DataException
	{
		return NIEUtil.formatPercentage(
			getActiveDocCount(),
			getRawDocCount(),
			Collection.PERCENT_DECIMAL_PLACES
			);
	}


	public int getNonBlankTitleCount( )
		throws IOException, DataException
	{
		if( null == cSequentialTitles || null == cSequentialUniqueTitles
			|| null == cUniqueTitleCounts
		) {
			cacheTitles( );
		}

		// The total number
		int total = cSequentialTitles.size();

		// The number of nulls (should always be zero)
		int nullCount = 0;
		if( cUniqueTitleCounts.containsKey(NULL_FIELD_MARKER) ) {
			Integer blankCountObj = (Integer) cUniqueTitleCounts.get(NULL_FIELD_MARKER);
			nullCount = blankCountObj.intValue();
		}

		// The number of blanks (often > 0)
		int blankCount = 0;
		if( cUniqueTitleCounts.containsKey("") ) {
			Integer blankCountObj = (Integer) cUniqueTitleCounts.get("");
			blankCount = blankCountObj.intValue();
		}


		return total - blankCount - nullCount;
	}

	public int getBlankTitleCount( )
		throws IOException, DataException
	{
		if( null == cSequentialTitles || null == cSequentialUniqueTitles
			|| null == cUniqueTitleCounts
		) {
			cacheTitles( );
		}

		// The number of nulls (should always be zero)
		int nullCount = 0;
		if( cUniqueTitleCounts.containsKey(NULL_FIELD_MARKER) ) {
			Integer blankCountObj = (Integer) cUniqueTitleCounts.get(NULL_FIELD_MARKER);
			nullCount = blankCountObj.intValue();
		}

		// The number of blanks (often > 0)
		int blankCount = 0;
		if( cUniqueTitleCounts.containsKey("") ) {
			Integer blankCountObj = (Integer) cUniqueTitleCounts.get("");
			blankCount = blankCountObj.intValue();
		}

		return blankCount + nullCount;
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
				if( ! title.equals("") && ! title.equals(NULL_FIELD_MARKER) )
					answer += count;
			}
		}

		cDuplicateTitleCount = answer;
		return answer;
	}

	public int getUniqueTitleCount( )
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
				if( ! title.equals("") && ! title.equals(NULL_FIELD_MARKER) )
					answer += count;
			}
		}

		cUniqueTitleCount = answer;
		return answer;
	}


	public double calcStorageEfficiency()
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getActiveDocCount(),
			getRawDocCount(),
			Collection.PERCENT_DECIMAL_PLACES
			);
	}


	public double calcPopulatedTitleRatio( )
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getNonBlankTitleCount( ),
			( getNonBlankTitleCount( ) + getBlankTitleCount( ) ),
			Collection.PERCENT_DECIMAL_PLACES
			);
	}


	public double calcUniqueTitleRatio( )
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getUniqueTitleCount( ),
			getNonBlankTitleCount( ),
			Collection.PERCENT_DECIMAL_PLACES
			);
	}



	public String calcDuplicateTitleRatioStr( )
		throws IOException, DataException
	{
		return NIEUtil.formatPercentage(
			getDuplicateTitleCount( ),
			getNonBlankTitleCount( ),
			Collection.PERCENT_DECIMAL_PLACES
			);
	}

	public double calcDuplicateTitleRatio( )
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getDuplicateTitleCount( ),
			getNonBlankTitleCount( ),
			Collection.PERCENT_DECIMAL_PLACES
			);
	}




	public String calcBlankTitleRatioStr()
		throws IOException, DataException
	{
		return NIEUtil.formatPercentage(
			getBlankTitleCount(),
			( getNonBlankTitleCount() + getBlankTitleCount() ),
			Collection.PERCENT_DECIMAL_PLACES
			);
	}

	public double calcBlankTitleRatio()
		throws IOException, DataException
	{
		return NIEUtil.calcPercentage(
			getBlankTitleCount(),
			( getNonBlankTitleCount() + getBlankTitleCount() ),
			Collection.PERCENT_DECIMAL_PLACES
			);
	}







	public int _getTitleCount()
		throws IOException, DataException
	{
		if( null == cSequentialTitles || null == cSequentialUniqueTitles
			|| null == cUniqueTitleCounts
		) {
			cacheTitles();
		}

		// The total number
		int total = cSequentialTitles.size();

		/***
		// The number of nulls (should always be zero)
		int nullCount = 0;
		if( cUnqieTitleCounts.containsKey(NULL_FIELD_MARKER) ) {
			Integer blankCountObj = (Integer) cUnqieTitleCounts.get(NULL_FIELD_MARKER);
			nullCount = blankCountObj.intValue();
		}
		***/

		// The number of blanks (often > 0)
		int blankCount = 0;
		if( cUniqueTitleCounts.containsKey("") ) {
			Integer blankCountObj = (Integer) cUniqueTitleCounts.get("");
			blankCount = blankCountObj.intValue();
		}

		return total - blankCount; // - nullCount;
	}

	public void addMyTitlesToMasterCache(
		List ioSequential, List ioUniqueSequential,
		Hashtable ioUniqueCounts, Hashtable ioUniqueKeys
		)
			throws IOException, DataException
	{
		// Sanity
		if( null == ioSequential || null == ioUniqueSequential
			|| null == ioUniqueCounts || null == ioUniqueKeys
		) {
			throw new DataException( "Null cache(s) passed in" );
		}

		// make sure we have our stuff ready
		if( null == cSequentialTitles || null == cSequentialUniqueTitles
			|| null == cUniqueTitleCounts || null == cUniqueTitleKeys
		) {
			cacheTitles();
		}

		// let the generic static method do the rest
		genericAddMyFieldToMainCache(
			ioSequential, ioUniqueSequential,
			ioUniqueCounts, ioUniqueKeys,
			cSequentialTitles, cSequentialUniqueTitles,
			cUniqueTitleCounts, cUniqueTitleKeys
			);

	}


	public static void genericAddMyFieldToMainCache(
		List ioSequentialMaster, List ioUniqueSequentialMaster,
		Hashtable ioUniqueCountsMaster, Hashtable ioUniqueKeysMaster,
		List ioSequentialMine, List ioUniqueSequentialMine,
		Hashtable ioUniqueCountsMine, Hashtable ioUniqueKeysMine
		)
			throws IOException, DataException
	{
		// Sanity
		if( null == ioSequentialMaster || null == ioUniqueSequentialMaster
			|| null == ioUniqueCountsMaster || null == ioUniqueKeysMaster
		) {
			throw new DataException( "Null master cache(s) passed in" );
		}

		// make sure we have our stuff ready
		if( null == ioSequentialMine || null == ioUniqueSequentialMine
			|| null == ioUniqueCountsMine || null == ioUniqueKeysMine
		) {
			throw new DataException( "Null local cache(s) passed in" );
		}

		// A quickie escape hatch for our stuff
		if( ioSequentialMine.isEmpty() && ioUniqueSequentialMine.isEmpty()
			&& ioUniqueCountsMine.isEmpty() && ioUniqueKeysMine.isEmpty()
		) {
			return;
		}

		// All the titles, in order
		// ========================================================
		// for this list we always just add our list to theirs
		ioSequentialMaster.addAll( ioSequentialMine );

		// The list of UNIQUE titles, IN-ORDER of appearance
		// ========================================================
		// If first partition to be added, just add all our stuff
		if( ioUniqueSequentialMaster.isEmpty() ) {
			ioUniqueSequentialMaster.addAll( ioUniqueSequentialMine );
		}
		// Else need to carefully merge with other parts' data
		else {
			// use set stuff and hash sets to calc unique key set
			HashSet newKeys = new HashSet();
			// our keys
			newKeys.addAll( ioUniqueSequentialMine );
			// their keys
			newKeys.removeAll( ioUniqueSequentialMaster );
			// we are left with just the keys of ours that are new
			if( ! newKeys.isEmpty() ) {
				// Look at each of my keys
				for( Iterator kit = ioUniqueSequentialMine.iterator() ; kit.hasNext() ; ) {
					String key = (String) kit.next();
					// If it's in the NEW set, then add it to the end of the master
					if( newKeys.contains(key) )
						ioUniqueSequentialMaster.add( key );
				}
			}
		}

		// the COUNT for each unqieue title
		// ==========================================================
		// If first partition to be added, just add all our stuff
		if( ioUniqueCountsMaster.isEmpty() ) {
			ioUniqueCountsMaster.putAll( ioUniqueCountsMine );
		}
		// Else need to carefully merge with other parts' data
		else {
			// Note that you can't short circuit here even if no new keys
			// even if we have titles that are already in the master hash
			// OUR counts of those titles need to be added in

			// Now combine the counts using the UNIQUE keys we just calculated
			// in the previous step
			for( Iterator cit = ioUniqueSequentialMaster.iterator() ; cit.hasNext() ; ) {
				String key = (String) cit.next();
				int count = 0;
				// Add the old count, if present
				if( ioUniqueCountsMaster.containsKey(key) ) {
					Integer tmpObj = (Integer) ioUniqueCountsMaster.get(key);
					count += tmpObj.intValue();
				}
				// Add the new count, if present
				if( ioUniqueCountsMine.containsKey(key) ) {
					Integer tmpObj = (Integer) ioUniqueCountsMine.get(key);
					count += tmpObj.intValue();
				}
				// Whatever we get back, we store
				ioUniqueCountsMaster.put( key, new Integer(count) );
			}
		}

		// the KEYS for each unqieue title
		// ==========================================================
		// If first partition to be added, just add all our stuff
		if( ioUniqueKeysMaster.isEmpty() ) {
			ioUniqueKeysMaster.putAll( ioUniqueKeysMine );
		}
		// Else need to carefully merge with other parts' data
		else {
			// Note that you can't short circuit here even if no new keys
			// even if we have titles that are already in the master hash
			// OUR counts of those titles need to be added in

			// Now combine the counts using the UNIQUE keys we just calculated
			// in the previous step
			for( Iterator kit = ioUniqueSequentialMaster.iterator() ; kit.hasNext() ; ) {
				String key = (String) kit.next();
				// Don't be fancy with this, just use .addAll, so that the orig lists
				// won't be messed with
				List allKeys = new ArrayList();
				// Add the old count, if present
				if( ioUniqueKeysMaster.containsKey(key) ) {
					List masterKeys = (List) ioUniqueKeysMaster.get(key);
					allKeys.addAll( masterKeys );
				}
				// Add the new count, if present
				if( ioUniqueKeysMine.containsKey(key) ) {
					List myKeys = (List) ioUniqueKeysMine.get(key);
					allKeys.addAll( myKeys );
				}

				// Whatever we get back, we store
				ioUniqueKeysMaster.put( key, allKeys );
			}
		}



	}





	void cacheActiveDocCountByTitle()
		throws IOException, DataException
	{
		if( null == cSequentialTitles || null == cSequentialUniqueTitles
			|| null == cUniqueTitleCounts
		) {
			cacheTitles();
		}
		cActiveDocCount = cSequentialTitles.size();
	}


	void _cacheActiveDocCountByVdkKey()
		throws IOException, DataException
	{
		if( null == cSequentialKeys || null == cSequentialUniqueKeys
			|| null == cUnqieKeyCounts
		) {
			_cacheVdkVgwKeys();
		}
		cActiveDocCount = cSequentialKeys.size();
	}

	void _cacheVdkVgwKeys()
		throws IOException, DataException
	{
		cSequentialKeys = new ArrayList();
		cSequentialUniqueKeys = new ArrayList();
		cUnqieKeyCounts = new Hashtable();
		cacheEntireField(
			"vdkvgwkey",
			cSequentialKeys,
			cSequentialUniqueKeys,
			cUnqieKeyCounts,
			null,
			false,	// boolean inDoTrim,
			false,	// boolean inDoNormalizeToLowerCase,
			false,	// boolean inForceEmptyToNull,
			true,	// boolean inKeepNullWithMarker,
			true	// boolean inDoRespectSecurityDeletedBit
			);
	}

	void cacheTitles()
		throws IOException, DataException
	{
		cSequentialTitles = new ArrayList();
		cSequentialUniqueTitles = new ArrayList();
		cUniqueTitleCounts = new Hashtable();
		cUniqueTitleKeys = new Hashtable();
		cacheEntireField(
		    mTitleField,
			cSequentialTitles,
			cSequentialUniqueTitles,
			cUniqueTitleCounts,
			cUniqueTitleKeys,
			true,	// boolean inDoTrim,
			false,	// boolean inDoNormalizeToLowerCase,
			false,	// boolean inForceEmptyToNull,
			true,	// boolean inKeepNullWithMarker,
			true	// boolean inDoRespectSecurityDeletedBit
			);
	}


	public void cacheEntireField(
		String inFieldName,
		List ioOptSequentialValuesCache,
		List ioOptSequentialUniqueValuesCache,
		Hashtable ioOptUnqieValueCountsCache,
		Hashtable ioOptUnqieValueToKeyListCache,
		boolean inDoTrim,
		boolean inDoNormalizeToLowerCase,
		boolean inForceEmptyToNull,
		boolean inKeepNullWithMarker,
		boolean inDoRespectSecurityDeletedBit
		)
			throws IOException, DataException
	{
		final String kFName = "cacheEntireField";
		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null == inFieldName )
			throw new IOException( "Null/empty field name" );

		if( null==ioOptSequentialValuesCache
			&& null==ioOptSequentialUniqueValuesCache
			&& null==ioOptUnqieValueCountsCache
		)
			throw new IOException( "All 3 passed in cache objects are null" );


		int recCount = getRawDocCount();

		if( recCount < 0 )
			throw new IOException( "Invalid raw record count found: " + recCount );

		long startTime = 0;
		long endTime = 0;
		if( recCount > SCAN_RECORDS_REPORT_INTERVAL ) {
System.err.println( "Partition: " + mDddFile + ": caching active field \"" + inFieldName + "\"");
System.err.print( "scanning " + recCount + " records so this may take a momment" );
			startTime = NIEUtil.getCurrTimeMillis();
		}

		// Browse the vdb
		VBrowseSession browser = null;
		try {

			browser = new VBrowseSession( mDddFile, mVBin );

			// For each expected record
			for( int i=0; i<recCount; i++ ) {

				if( recCount > SCAN_RECORDS_REPORT_INTERVAL && i>0 && (i+1) % SCAN_RECORDS_REPORT_INTERVAL == 0 ) {
System.err.print( "." );
				}


				String thisValue = null;
				String securityValue = null;
				String thisKey = null;

				browser.sendCmd( ""+i );
	
				boolean haveEatenCmdEcho = false;
				boolean haveSeenValueField = false;
				boolean haveSeenSecurityField = false;
				boolean haveSeenKeyField = false;
				// For each field in this record
				while( true ) {
					String line = browser.readLine();
	//	   System.out.println( "Line=" + line );
					if( null==line )
						continue;
					if( ! haveEatenCmdEcho ) {
						haveEatenCmdEcho = true;
						continue;
					}
					// If it's the command prompt, we're done
					if( line.length()==VBrowseSession.PROMPT_LEN && line.equals(VBrowseSession.READY_PROMPT) )
						break;

					// Process line
					String fieldName = VBrowseSession.extractMormalizedFieldNameFromRecordOrNull( line );

					// The value itself
					if( ! haveSeenValueField
							&& null!=fieldName && fieldName.equals(inFieldName)
					) {
						thisValue = VBrowseSession. extractValueFromRecordOrNull( line );
						haveSeenValueField = true;
					}
					// The security field
					if( ! haveSeenSecurityField && inDoRespectSecurityDeletedBit
							&& null!=fieldName
							&& fieldName.equals(SECURITY_FIELD)
					) {
						securityValue = VBrowseSession.extractValueFromRecordOrNull( line );
						haveSeenSecurityField = true;
					}
					// The key field (VdkVgwKey)
					if( null!=ioOptUnqieValueToKeyListCache
							&& ! haveSeenKeyField
							&& null!=fieldName
							&& fieldName.equals(KEY_FIELD)
					) {
						thisKey = VBrowseSession.extractValueFromRecordOrNull( line );
						thisKey = NIEUtil.trimmedStringOrNull( thisKey );
						if( null==thisKey )
							throw new DataException( "Null / empty key field." );
						haveSeenKeyField = true;
					}

					// If we've got the value and security field (or don't care about
					// security), then bail early
					if( haveSeenValueField
							&& (haveSeenKeyField || null==ioOptUnqieValueToKeyListCache)
							&& (haveSeenSecurityField || !inDoRespectSecurityDeletedBit)
					) {
						browser.consumeLinesThroughPrompt();
						break;
					}
	
				}	// end for each field in record

				// Sanity check
				if( ! haveSeenValueField )
					throw new DataException( "Did not find field \"" + inFieldName + "\"" );
				if( null!=ioOptUnqieValueToKeyListCache && ! haveSeenKeyField )
					throw new DataException( "Did not find key field \"" + KEY_FIELD + "\"" );

				// Security check
				if( inDoRespectSecurityDeletedBit ) {
					securityValue = NIEUtil.trimmedStringOrNull( securityValue );
					if( null!=securityValue ) {
						long secureBits = NIEUtil.stringToLongOrDefaultValue( securityValue, -1L, true, true );
						if( secureBits < 0L )
							throw new DataException( "Invalid security field value \"" + securityValue + "\"" );
						if( (secureBits & DELETED_BIT) != 0L )
							continue;
					}
				}

				// Start looking at the value
				if( null!=thisValue ) {
					if( thisValue.startsWith(VBrowseSession.RECORD_COUNT_PREFIX_PATTERN)
						&& thisValue.startsWith(VBrowseSession.RECORD_COUNT_SUFFIX_PATTERN)
					) {
						throw new DataException(
							"Read past end of document records for field \"" + inFieldName + "\""
							+ " At offset " + i + " with expected max offset of " + recCount
							+ " but Verity says \"" + thisValue + "\""
							);
						
					}

					// Some other normalization
					if( inDoTrim )
						thisValue = thisValue.trim();

					if( inDoNormalizeToLowerCase )
						thisValue = thisValue.toLowerCase();

					if( inForceEmptyToNull && thisValue.length() < 1 )
						thisValue = null;

				}

				// Now check if null
				if( null==thisValue ) {
					// Either normalize to our string
					if( inKeepNullWithMarker )
						thisValue = NULL_FIELD_MARKER;
					// Or throw it away
					else
						continue;
				}



				// OK, we have a value we'd like to store

				// The list of all the values
				if( null != ioOptSequentialValuesCache ) {
					ioOptSequentialValuesCache.add( thisValue );
				}

				// The list of unique values
				// Ideally we do the lookup in the hashtable's keys which
				// should be faster, but we can use the list if the hash
				// is not available
				if( null != ioOptSequentialUniqueValuesCache
					&& (
						( null!=ioOptUnqieValueCountsCache
							&& !ioOptUnqieValueCountsCache.containsKey(thisValue)
							)
						|| ! ioOptSequentialUniqueValuesCache.contains( thisValue )
					)
				) {
					ioOptSequentialUniqueValuesCache.add( thisValue );
					// TODO: might be faster to just always add it, not sure
				}

				// Tabulate counts
				if( null != ioOptUnqieValueCountsCache ) {
					int oldCount = 0;
					if( ioOptUnqieValueCountsCache.containsKey(thisValue) ) {
						Integer oldCountObj = (Integer) ioOptUnqieValueCountsCache.get( thisValue );
						oldCount = oldCountObj.intValue();
					}
					ioOptUnqieValueCountsCache.put( thisValue, new Integer(oldCount+1) );
				}

				// Store the key
				if( null != ioOptUnqieValueToKeyListCache ) {
					List keyList = null;
					if( ioOptUnqieValueToKeyListCache.containsKey(thisValue) )
						keyList = (List) ioOptUnqieValueToKeyListCache.get( thisValue );
					else
						keyList = new ArrayList();
					keyList.add( thisKey );
					ioOptUnqieValueToKeyListCache.put( thisValue, keyList );
				}

			}	// end for each record

			if( recCount > SCAN_RECORDS_REPORT_INTERVAL ) {
				endTime = NIEUtil.getCurrTimeMillis();
				long duration = endTime - startTime;
				if( duration >= 0L )
					System.err.print( " (completed in " + duration + " ms)");

				System.err.println();
			}


		}
		catch( Exception e ) {
			throw new DataException( "Error caching field: " + e );
		}
		finally {
			if( null!=browser )
				browser.closeSession();
		}

	}




	int cacheRawDocCount()
		throws DataException
	{
		if( null==cDocFieldNamesCommon || cDocFieldNamesCommon.isEmpty() || null==cFieldNameRawCounts )
			throw new DataException( "Null/empty field list" );
		int rawDocCount = -1;
		for( Iterator it = cDocFieldNamesCommon.iterator() ; it.hasNext() ; ) {
			String fieldName = (String) it.next();
			if( ! cFieldNameRawCounts.containsKey(fieldName) )
				throw new DataException( "Unknown field name \"" + fieldName + "\"" );
			Integer rawCount = (Integer) cFieldNameRawCounts.get( fieldName );
			int newVal = rawCount.intValue();
			if( newVal > rawDocCount )
				rawDocCount = newVal;
		}
		if( rawDocCount < 0 )
			throw new DataException( "Unable to get raw document count" );
		cRawDocCount = rawDocCount;
		return cRawDocCount;
	}


	private List cacheCommonFieldStats()
	{
		cDocFieldNamesCommon = new ArrayList();
		cPartFieldNamesCommon = new ArrayList();
		cFieldNamesSystem = new ArrayList();

		for( Iterator it = cFieldNameList.iterator(); it.hasNext() ; ) {
			String fieldName = (String) it.next();

			// System fields ending in _of, _sz, _mi, _mx, _ix
			if( fieldName.endsWith("_of") || fieldName.endsWith("_sz")
				|| fieldName.endsWith("_mi") || fieldName.endsWith("_mx")
				|| fieldName.endsWith("_ix")
			) {
				cFieldNamesSystem.add( fieldName );
			}
			else if( kObscureVerityPartitionFields.contains(fieldName) ) {
				cFieldNamesSystem.add( fieldName );
			}
			else if( kVerityPartitionOnlyFields.contains(fieldName) ) {
				cPartFieldNamesCommon.add( fieldName );
			}
			// Else default to a field they want to see
			else {
				cDocFieldNamesCommon.add( fieldName );
			}
			

		}

		return cDocFieldNamesCommon;
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
			getIDStr() + ": " + inMessage
			);
	}

	private boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			// inMessage
			getIDStr() + ": " + inMessage
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
			getIDStr() + ": " + inMessage
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
			getIDStr() + ": " + inMessage
			);
		// return getRunLogObject().statusMsg( kClassName, inFromRoutine,
		// 	"DEBUG: " + inMessage
		// 	);
	}

	private boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName, inFromRoutine,
			// inMessage
			getIDStr() + ": " + inMessage
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
			getIDStr() + ": " + inMessage
			);
	}

	private boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			// inMessage
			getIDStr() + ": " + inMessage
			);
	}

	private boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			// inMessage
			getIDStr() + ": " + inMessage
			);
	}


	private static void __sep__Member_Fields_and_Constants__() {}
	//////////////////////////////////////////////////



	Collection mParentCollection;

	File mDddFile;

	String mShortName;

	// Optional directory where Verity binaries are stored
	File mVBin;
	String mTitleField = DEFAULT_TITLE_FIELD;
	
	String cIDStr; // coll name . part name (short)

	int cRawDocCount;
	int cActiveDocCount = -1;
	int cDuplicateTitleCount = -1;
	int cUniqueTitleCount = -1;

	// We really need this info several different ways
	List cFieldNameList;
	List cDocFieldNamesCommon;
	List cPartFieldNamesCommon;
	List cFieldNamesSystem;
	HashSet cFieldNameSet;
	Hashtable cFieldNameRawCounts;

	List cSequentialKeys;
	List cSequentialUniqueKeys;
	Hashtable cUnqieKeyCounts;

	List cSequentialTitles;
	List cSequentialUniqueTitles;
	Hashtable cUniqueTitleCounts;
	Hashtable cUniqueTitleKeys;



	public static HashSet kObscureVerityPartitionFields;
	static {
		kObscureVerityPartitionFields = new HashSet();

		// Not just 1 record, but less than total
		kObscureVerityPartitionFields.add( "_ddflag" );
		kObscureVerityPartitionFields.add( "_ddvalue" );

		// Just 1 record
		kObscureVerityPartitionFields.add( "_docidx" );
		kObscureVerityPartitionFields.add( "_ftrcfg" );
		kObscureVerityPartitionFields.add( "_sumcfg" );
		kObscureVerityPartitionFields.add( "_spare1" );
		kObscureVerityPartitionFields.add( "_spare2" );

		// 1 for EACH doc record
		kObscureVerityPartitionFields.add( "_cache_delete" );
		kObscureVerityPartitionFields.add( "_dirid" );
		kObscureVerityPartitionFields.add( "isachunk" );
		kObscureVerityPartitionFields.add( "_parentid" );


	}

	public static HashSet kVerityPartitionOnlyFields;
	static {
		kVerityPartitionOnlyFields = new HashSet();

		// Fields with only 1 record
		kVerityPartitionOnlyFields.add( "_dbversion" );
		kVerityPartitionOnlyFields.add( "_dddstamp" );
		kVerityPartitionOnlyFields.add( "_docidx" );
		kVerityPartitionOnlyFields.add( "_partdesc" );
		kVerityPartitionOnlyFields.add( "_ftrcfg" );
		kVerityPartitionOnlyFields.add( "_sumcfg" );
		kVerityPartitionOnlyFields.add( "_spare1" );
		kVerityPartitionOnlyFields.add( "_spare2" );

	}


	public static final String NULL_FIELD_MARKER = "(__null_value_marker__)";
	public static final long DELETED_BIT = 0x80000000L;
	// ^^^ fyi, this = 2,147,483,648 in base 10
	public static final String SECURITY_FIELD = "_security";
	public static final String DEFAULT_TITLE_FIELD = "title";
	public static final String KEY_FIELD = "vdkvgwkey";

	public static final int SCAN_RECORDS_REPORT_INTERVAL = 1000;

}