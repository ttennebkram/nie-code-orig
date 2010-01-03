//package nie.processors;
package nie.pump.processors;
import java.util.*;
import org.jdom.Element;
import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

/***
	Load a master list
	Then lookup values
	Keep the values that are in the master list
	Or dump the values that match anything in the mast list with invert

	If doing simple matching we use a fast hash.
	If doing otherwise, it actually uses different data structures and
	may be slow.

	If you ask for words, you will get trimming.

	in V2:
	invert logic option
	na: handle words
	na: handle wildcards
	handle substring
	na: handle regex
	na: set word break characters, currently just using Java default
***/

/**
 * Title:	MasterList
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author	Mark
 * @version 1.0
 *
 */


public class MasterLookup2 extends Processor
{
	public String kClassName() { return "MasterLookup2"; }


	// public static final boolean debug = false;

	private void __sep__Constructors__() {};
	////////////////////////////////////////////////////////////
	public MasterLookup2( Application inApplication,
		Queue[] inReadQueue,
		Queue[] inWriteQueue,
		Queue[] inUsesQueue,
		Element inParameter,
		String inID )
	{
		super( inApplication, inReadQueue, inWriteQueue, inUsesQueue, inParameter, inID );
		final String kFName = "constructor";
		if( (inReadQueue == null) || (inReadQueue.length < 2) ||
			(inReadQueue[0] == null) || (inReadQueue[1] == null)
			)
			{	
			
			fatalErrorMsg( kFName, "MasterLookup2 requires two input"
				+ "queues: data to check and master list." );
			System.exit( -1 );
			}

		fReadQueue = inReadQueue[0];
		fMasterValuesQueue = inReadQueue[1];

		if( (inWriteQueue == null) || (inWriteQueue[0] == null) )
		{
			
			// System.err.println( "No output queue specified." );
			fatalErrorMsg( kFName, "No output queue specified." );
			System.exit( -1 );
		}

		fWriteQueue = inWriteQueue[0];

		if( (inUsesQueue == null) || (inUsesQueue[0] == null) )
		{
			// System.err.println( "No uses/trigger queue specified." );
			fatalErrorMsg( kFName, "No uses/trigger queue specified." );
			System.exit( -1 );
		}

		fTriggerQueue = inUsesQueue[0];


		// If parameters were sent in, save them
		if( inParameter != null )
		{
			//System.err.println( "inParameters = " + inParameter );
			infoMsg( kFName, "inParameters = " + inParameter );
			try
			{
				fJdh = new JDOMHelper( inParameter );
			}
			catch (Exception e)
			{
				fJdh = null;
				// System.err.println(
				//	"Error creating jdom helper for parameter\n" + e
				//	);
				fatalErrorMsg( kFName,
						"Error creating jdom helper for parameter\n" + e
						);
				System.exit(1);
			}
		}
		else
		{
			// System.err.println( "Error: inParameter == null." );
			fatalErrorMsg( kFName, "inParameter == null." );
			// System.err.println( "see doc" );
			// fatalErrorMsg( kFName, "see doc" );
			System.exit(1);
		}

//		List tmpList = fJdh.findElementsByPath( TABULATE_TAG_NAME );
//
//		if( tmpList.size() != 1 )
//		{
//			System.err.println( "Error: must have exactly one tabulate tag." );
//			System.err.println( "see doc" );
//			System.exit(1);
//		}
//
//		// Store our tag
//		try
//		{
//			fTabTag = new JDOMHelper( (Element)tmpList.get(0) );
//		}
//		catch (Exception e)
//		{
//			System.err.println( "Can't create jdom helper for tabulate tag" );
//			System.err.println(e);
//			System.exit(1);
//		}

		// Get the main tables ready
		// fMainHashTableCounts = new Hashtable();
		fMainHashTableWorkUnits = new Hashtable();
		fMainExpressionsFlatList = new Vector();
		// fMainExpressionsListOfLists = new Vector();

		// Now init and check all the tag attributes

		// Make sure they get the valueus from the tag, vs the cache
		fHaveDoneInit = false;

		// Call the get methods so they will fill their cache
		// they will do an exit if they are not happy
		getMasterKeyFieldName();
		getCheckKeyFieldName();
		getAuditFieldName();
		getIsCaseSensitive();
		getDoIgnoreEmptyStrings();
		getDoTrimStrings();
		getDoInvertLogic();
		getDoMatchSubstrings();
		// allow wild cards
		// word matching
		// regex matching

		// switch over to using the cached values
		fHaveDoneInit = true;

		// We need to know when we're done reading in init values
		subscribeDoneTrigger();
	}

	// We want to be notified when folks are done
	void subscribeDoneTrigger()
	{
		subscribeTrigger( TriggerQueue.DONE_TRIGGER_TEXT,
			fTriggerQueue
			);
	}



	/*************************************************
	*
	*	Simple Get/Set Logic
	*
	*************************************************/
	private void __sep__Simple_Get_and_Set__() {};
	////////////////////////////////////////////////////////////

	String getMasterKeyFieldName()
	{
		final String kFName = "constructor";
		if( fHaveDoneInit )
			return fMasterKeyFieldName;

		if( fJdh == null )
		{
			// System.err.println( "Error:MasterLookup2: no parameters tag?" );
			fatalErrorMsg( kFName, "no parameters tag?" );
			System.exit(1);
		}

		//String tmpString = fTabTag.getStringFromAttribute(
		String tmpString = fJdh.getTextByPath(
			MASTER_FIELD
			);
		if( tmpString == null || tmpString.trim().equals("") )
		{
			tmpString = fJdh.getTextByPath(
				MASTER_FIELD_2
				);
		}
		if( tmpString == null || tmpString.trim().equals("") )
		{
			tmpString = fJdh.getTextByPath(
				MASTER_FIELD_3
				);
		}

		if( tmpString == null || tmpString.trim().equals("") )
		{
			// System.err.println( "Error:"
			//	+ " not told which key field to key master list on"
			//	+ ", should specify tag " + MASTER_FIELD
			//	+ " or shorthand forms " + MASTER_FIELD_2
			//	+ " or " + MASTER_FIELD_3
			//	);
			fatalErrorMsg( kFName, 
					"not told which key field to key master list on"
					+ ", should specify tag " + MASTER_FIELD
					+ " or shorthand forms " + MASTER_FIELD_2
					+ " or " + MASTER_FIELD_3
					);
			System.exit(1);
		}

		fMasterKeyFieldName = tmpString.trim();
		return fMasterKeyFieldName;
	}

	String getCheckKeyFieldName()
	{
		
		final String kFName = "constructor";
		if( fHaveDoneInit )
			return fCheckKeyFieldName;

		if( fJdh == null )
		{
			// System.err.println( "Error:MasterLookup2: no parameters tag?" );
			fatalErrorMsg( kFName, "No parameters tag?" );
			System.exit(1);
		}

		//String tmpString = fTabTag.getStringFromAttribute(
		String tmpString = fJdh.getTextByPath(
			CHECK_FIELD
			);
		// We default to the same as the master field
		if( tmpString == null || tmpString.trim().equals("") )
		{
			tmpString = getMasterKeyFieldName();
		}

		if( tmpString == null || tmpString.trim().equals("") )
		{
			// System.err.println( "Error:"
			//	+ " not told which key field check against master list"
			//	+ ", should specify tag " + CHECK_FIELD
			//	+ " or set the master key field"
			//	);
			fatalErrorMsg( kFName, 
					"not told which key field check against master list"
					+ ", should specify tag " + CHECK_FIELD
					+ " or set the master key field"
					);
			System.exit(1);
		}

		fCheckKeyFieldName = tmpString.trim();
		return fCheckKeyFieldName;
	}

	String getAuditFieldName()
	{
		if( fHaveDoneInit )
			return fAuditFieldName;

		if( fJdh == null )
		{
			return null;
		}

		//String tmpString = fTabTag.getStringFromAttribute(
		String tmpString = fJdh.getTextByPath(
			AUDIT_FIELD
			);
		// We default to the same as the master field
		if( tmpString == null || tmpString.trim().equals("") )
		{
			tmpString = getMasterKeyFieldName();
		}

		if( tmpString == null || tmpString.trim().equals("") )
		{
			return null;
		}

		fAuditFieldName = tmpString.trim();
		return fAuditFieldName;
	}



//	String getSourceFieldName()
//	{
//		if( fHaveDoneInit )
//			return fSourceFieldName;
//		else
//		{
//			if( fJdh == null )
//			{
//				System.err.println( "Error: no parameters tag?" );
//				System.exit(1);
//			}
//			else
//			{
//				//String tmpString = fTabTag.getStringFromAttribute(
//				String tmpString = fJdh.getTextByPath(
//					SOURCE_FIELD_ATTR_NAME
//					);
//				if( tmpString == null || tmpString.trim().equals("") )
//				{
//					tmpString = fJdh.getTextByPath(
//						SOURCE_FIELD_ATTR_SHORT_NAME
//						);
//					if( tmpString == null || tmpString.trim().equals("") )
//					{
//						System.err.println( "Error:" +
//							" not told which key field to tabulate on" +
//							", should specify tag " + SOURCE_FIELD_ATTR_NAME +
//							" or shorthand form " + SOURCE_FIELD_ATTR_SHORT_NAME
//							);
//						System.exit(1);
//					}
//				}
//				fSourceFieldName = tmpString.trim();
//			}
//			return fSourceFieldName;
//		}
//	}


//	String getDestinationCountFieldName()
//	{
//		if( fHaveDoneInit )
//			return fDestinationCountFieldName;
//		else
//		{
//			if( fJdh == null )
//			{
//				System.err.println( "Error: no params tag" );
//				System.exit(1);
//				//fDestinationFieldName = null;
//			}
//			else
//			{
//				String tmpString = fJdh.getTextByPath(
//					DESTINATION_COUNT_FIELD_ATTR_NAME
//					);
//				if( tmpString == null || tmpString.trim().equals("") )
//				{
//
//					tmpString = fJdh.getTextByPath(
//						DESTINATION_COUNT_FIELD_ATTR_SHORT_NAME
//						);
//					if( tmpString == null || tmpString.trim().equals("") )
//					{
//						System.err.println( "Error:" +
//							" not told which key field to put the total count in" +
//							", should specify tag " + DESTINATION_COUNT_FIELD_ATTR_NAME +
//							" or shorthand form " + DESTINATION_COUNT_FIELD_ATTR_SHORT_NAME
//							);
//						System.exit(1);
//					}
//				}
//				fDestinationCountFieldName = tmpString.trim();
//			}
//			return fDestinationCountFieldName;
//		}
//	}

//	String getDestinationKeyFieldName()
//	{
//		if( fHaveDoneInit )
//			return fDestinationKeyFieldName;
//		else
//		{
//			if( fJdh == null )
//			{
//				System.err.println( "Error: no params tag" );
//				System.exit(1);
//				//fDestinationFieldName = null;
//			}
//			else
//			{
//				String tmpString = fJdh.getTextByPath(
//					DESTINATION_KEY_FIELD_ATTR_NAME
//					);
//				if( tmpString == null || tmpString.trim().equals("") )
//				{
//					// Make it the same as the source's
//					tmpString = getSourceFieldName();
//					if( tmpString == null || tmpString.trim().equals("") )
//					{
//						System.err.println( "Error:"
//							+ " Unable to calculate destination key field name"
//							);
//						System.exit(1);
//					}
//				}
//				fDestinationKeyFieldName = tmpString.trim();
//			}
//			return fDestinationKeyFieldName;
//		}
//	}

//	String getCopySourceFieldName( Element inCopyInstructionElement )
//	{
//		// Look for the long name
//		String tmpString = inCopyInstructionElement.getAttributeValue(
//				COPY_SOURCE_FIELD_ATTR_NAME
//				);
//		// If not found, look for the short name
//		if( tmpString == null )
//			tmpString = inCopyInstructionElement.getAttributeValue(
//					COPY_SOURCE_FIELD_ATTR_SHORT_NAME
//					);
//		if( tmpString == null || tmpString.trim().equals("") )
//		{
//			System.out.println(
//				"Tabulate:Copy: Must specify a source field" +
//				" with attr of " + COPY_SOURCE_FIELD_ATTR_NAME + " or " +
//				COPY_SOURCE_FIELD_ATTR_SHORT_NAME
//				);
//			System.exit(1);
//		}
//		return tmpString.trim();
//	}

//	String getCopyDestinationFieldName( Element inCopyInstructionElement )
//	{
//		// Look for the long name
//		String tmpString = inCopyInstructionElement.getAttributeValue(
//				COPY_DESTINATION_FIELD_ATTR_NAME
//				);
//		// If not found, try the short name
//		if( tmpString == null )
//			tmpString = inCopyInstructionElement.getAttributeValue(
//					COPY_DESTINATION_FIELD_ATTR_SHORT_NAME
//					);
//		// Else just make it the same as the source field,
//		// after all it's between two different work units
//		if( tmpString == null || tmpString.trim().equals("") )
//		{
//			tmpString = getCopySourceFieldName( inCopyInstructionElement );
//		}
//		return tmpString.trim();
//	}

//	// Will return a trimmed string or a null (if null or empty)
//	String getCopyDestinationBranchName( Element inBranchInstructionElement )
//	{
//		// Look for the long name
//		String tmpString = inBranchInstructionElement.getAttributeValue(
//				BRANCH_DESTINATION_FIELD_ATTR_NAME
//				);
//		// If not found, try the short name
//		if( tmpString == null )
//			tmpString = inBranchInstructionElement.getAttributeValue(
//					BRANCH_DESTINATION_FIELD_ATTR_SHORT_NAME
//					);
//
//		// Normalize, and set to a true null if it's empty
//		if( tmpString.trim().equals("") )
//			tmpString = null;
//		else
//			tmpString = tmpString.trim();
//
//		return tmpString;
//	}


	boolean getIsCaseSensitive()
	{
		if( fHaveDoneInit )
			return fDoIsCaseSensitive;
		else
		{
			if( fJdh == null )
				fDoIsCaseSensitive = DEFAULT_IS_CASE_SENSITIVE;
			else
			{   // getTextByPath
				fDoIsCaseSensitive = fJdh.getBooleanFromPathText(
					CASE_SENSITIVE_ATTR,
					DEFAULT_IS_CASE_SENSITIVE
					);
			}
			return fDoIsCaseSensitive;
		}
	}

	boolean getDoMatchSubstrings()
	{
		if( fHaveDoneInit )
			return fDoMatchSubstrings;
		else
		{
			if( fJdh == null )
				fDoMatchSubstrings = DEFAULT_MATCH_SUBSTRINGS;
			else
			{
				fDoMatchSubstrings = fJdh.getBooleanFromPathText(
					MATCH_SUBSTRINGS_ATTR,
					DEFAULT_MATCH_SUBSTRINGS
					);
			}
			return fDoMatchSubstrings;
		}
	}

//	boolean getDoMatchWords()
//	{
//		if( fHaveDoneInit )
//			return fDoMatchWords;
//		else
//		{
//			if( fJdh == null )
//				fDoWordsStrings = DEFAULT_DO_MATCH_WORDS;
//			else
//			{
//				fDoMatchWords = fJdh.getBooleanFromPathText(
//					MATCH_WORDS_ATTR,
//					DEFAULT_MATCH_WORDS
//					);
//			}
//			return fDoMatchWords;
//		}
//	}

//	boolean getDoAllowWildcards()
//	{
//		if( fHaveDoneInit )
//			return fDoAllowWildCards;
//		else
//		{
//			if( fJdh == null )
//				fDoAllowWildCards = DEFAULT_ALLOW_WILD_CARDS;
//			else
//			{
//				fDoAllowWildCards = fJdh.getBooleanFromPathText(
//					ALLOW_WILD_CARDS_ATTR,
//					DEFAULT_ALLOW_WILD_CARDS
//					);
//			}
//			return fDoAllowWildCards;
//		}
//	}

	boolean getDoTrimStrings()
	{
		if( fHaveDoneInit )
			return fDoTrimStrings;
		else
		{
			if( fJdh == null )
				fDoTrimStrings = DEFAULT_TRIM_STRINGS;
			else
			{
				fDoTrimStrings = fJdh.getBooleanFromPathText(
					TRIM_STRINGS_ATTR,
					DEFAULT_TRIM_STRINGS
					);
			}
			return fDoTrimStrings;
		}
	}

	boolean getDoIgnoreEmptyStrings()
	{
		if( fHaveDoneInit )
			return fDoIgnoreEmptyStrings;
		else
		{
			if( fJdh == null )
				fDoIgnoreEmptyStrings = DEFAULT_IGNORE_EMPTY_STRINGS;
			else
			{
				fDoIgnoreEmptyStrings = fJdh.getBooleanFromPathText(
					IGNORE_EMPTY_STRINGS_ATTR,
					DEFAULT_IGNORE_EMPTY_STRINGS
					);
			}
			return fDoIgnoreEmptyStrings;
		}
	}

	boolean getDoInvertLogic()
	{
		if( fHaveDoneInit )
			return fDoInvertLogic;
		else
		{
			if( fJdh == null )
				fDoInvertLogic = DEFAULT_INVERT_LOGIC;
			else
			{
				fDoInvertLogic = fJdh.getBooleanFromPathText(
					INVERT_LOGIC_ATTR,
					DEFAULT_INVERT_LOGIC
					);
			}
			return fDoInvertLogic;
		}
	}

	boolean getIsNormalHashLogic()
	{
	    final String kFName = "getIsNormalHashLogic";
		// If we're doing anything unusual, the it's not normal
		// this is in terms of the central lookup logic and data structures
		if( getDoMatchSubstrings()
			/***
			|| getDoMatchWords()
			|| getDoMatchRegexes()
			|| getDoAllowWildcards()
			***/
			)
		{
			// if(debug) System.err.println( "ML2: getIsNormalHashLogic: false" );
			debugMsg( kFName, "returning false" );

			return false;
		}
		else
		{
			// if(debug) System.err.println( "ML2: getIsNormalHashLogic: true" );
		    debugMsg( kFName, "returning true" );
			return true;
		}
	}

	private void __sep__Central_Logic__() {};
	////////////////////////////////////////////////////////////
	public void run()
	{
		
		final String kFName = "run";
		//boolean debug = false;

		try
		{
			// if(debug) System.err.println( "Debug:MasterLookup2:run: Start." );
			// if(debug) System.err.println( "Debug:ML:run: Phase 1: read master values." );
			debugMsg( kFName, "Start." );

			debugMsg( kFName, "Phase 1: read master values." );

			fMasterCount = 0L;
			fTriggerCount = 0L;
			fMainCount = 0L;
			boolean gotTrigger = false;
			while( ! gotTrigger )
			{
				// System.err.println( "\tML:Ph1: top of loop." );
				debugMsg( kFName, "top of loop." );

				while( ! fMasterValuesQueue.isEmpty() )
				{
					// if(debug) System.err.println( "\tML:Ph1:"
					//	+ " appear to have master data."
					//	);
					debugMsg( kFName, "appear to have master data." );
					WorkUnit lMasterWU = null;
					lMasterWU = dequeue( fMasterValuesQueue );
					if( lMasterWU != null )
					{
						fMasterCount++;
						debugMsg( kFName,
							"processing master unit # " + fMasterCount
							);
						saveMasterUnit( lMasterWU );
						lMasterWU = null;
					}
					else
						// System.err.println( "Error:MasterLookup2:run:"
						//	+ " got back a null master data unit"
						//	+ " after " + fMasterCount + " non null units"
						//	);
						errorMsg( kFName,
								"got back a null master data unit"
							+ " after " + fMasterCount + " non null units"
							);
				}
				debugMsg( kFName,
					"appears to be no more master data, at least for now."
					);
				if( ! fTriggerQueue.isEmpty() )
				//if( ((TriggerQueue)fTriggerQueue).getSize(getID()) != 0L )
				{
					debugMsg( kFName,
						"appears to have a trigger waiting."
						);
					WorkUnit lTriggerWU = dequeue( fTriggerQueue );
					if( lTriggerWU != null )
					{
						gotTrigger = true;
						fTriggerCount++;
						debugMsg( kFName,
							"got a non-null trigger record."
							+ " # of triggers seen: " + fTriggerCount
							);
						lTriggerWU = null;
					}
					else
						// System.err.println( "Warning:ML:Ph1:"
						//	+ " got a null trigger record"
						//	+ " but queue reported that there WAS a trigger waiting."
						//	+ " Will keep trying."
						//	);
					errorMsg( kFName, "got a null trigger record"
							+ " but queue reported that there WAS a trigger waiting."
							+ " Will keep trying."
							);
				}
				else
					debugMsg( kFName,
						"appears to NOT have a trigger waiting."
						);

				debugMsg( kFName,
					"gotTrigger=" + gotTrigger
					);

			}   // Botton of phase 1 while loop

			//debug = true;

			debugMsg( kFName,
				"process regular work units and compare to master list."
				);

			// Now process the normal, input work units
			//while( ! fReadQueue.isEmpty() )
			while( true )
			{
			    mWorkUnit = null;

			    // if(debug)
					// System.err.println( "Debug:ML:run: Phase 2:"
					//		+ " Top of loop"
					//		);
				debugMsg( kFName, "Phase 2:"
					+ " Top of loop"
					);
					
				// WorkUnit lWorkUnit = dequeue( fReadQueue );
				mWorkUnit = dequeue( fReadQueue );
				fMainCount++;
				debugMsg( kFName,
					"\tHave dequeued # " + fMainCount
					// + ": " + lWorkUnit
					);
				try
				{
					debugMsg( kFName,
						"\tperforming lookup"
						);
					//lookupUnit( lWorkUnit );
					lookupUnit( mWorkUnit, true, getDoInvertLogic() );
					debugMsg( kFName,
						"\tback from lookup"
						);
				}
				catch(Exception e)
				{
					//lWorkUnit = null;
					// System.err.println( "MasterLookup2: unable to process work unit"
					//	+ ", marking invalid"
					//	+ ", error was: " + e
					//	);
					mWorkUnit.stackTrace( this, kFName, e, "unable to process work unit"
							+ ", marking invalid"
							);
					//continue;
					mWorkUnit.setIsValidRecord( false );
				}

				if( /*debug &&*/ fMainCount % kReportInterval == 0 )
				{
					// System.err.println( "MasterList: Processed '" + fMainCount +
					//	"' input records." );
					infoMsg( kFName, "Processed '" + fMainCount +
						"' input records." );
				}

				// Now enqueue the result
				debugMsg( kFName,
					"about to enqueue"
					+ ", queue=" + fWriteQueue
					// + ", lWorkUnit=" + lWorkUnit
					);
				enqueue( fWriteQueue, mWorkUnit );
				debugMsg( kFName,
					"back from enqueue"
					);

				mWorkUnit = null;
			}
//			if(debug) System.err.println(
//				"Debug:ML:run: done phase 2 and done run"
//				);

			//if(debug) System.err.println( "MasterList: ..." );

		}
		catch( InterruptedException ie )
		{
		    // This is normal
		}
	}

	private void saveMasterUnit( WorkUnit inWU )
	{
		final String kFName = "saveMasterUnit";
		List keys = inWU.getUserFieldsText( getMasterKeyFieldName() );
		Iterator it = keys.iterator();
		while( it.hasNext() )
		{
			String key = (String)it.next();
			// We must not have truely a NULL key
			if( key == null )
			{
				// System.err.println( "Warning: MasterLookup2:saveMasterUnit:"
				//	+ " found a truly null field"
				//	+ " for key " + getMasterKeyFieldName()
				//	+ ", skipping this key."
				//	);
				mWorkUnit.errorMsg( this, kFName, "found a truly null field"
						+ " for key " + getMasterKeyFieldName()
						+ ", skipping this key."
						);
				continue;
			}

			// If it's not case sensitive, then normalize it
			if( ! getIsCaseSensitive() )
				key = key.toLowerCase();
			// Do trimming if requested
			if( getDoTrimStrings() )
			{
				key = key.trim();
			}
			// Ignore empty strings if requested to do so
			if( getDoIgnoreEmptyStrings() )
				if( key.equals("") )
					continue;

			// At this point we have a clean and normalized key
			// that we know we'd like to store.
			// But the type of matching we've been asked to peform impacts
			// which data structure we'll use to save the key into.

			// If doing a normal hash-style check, use the main hash
			if( getIsNormalHashLogic() )
			{
				// See if there's already a work unit for it
				if( fMainHashTableWorkUnits.containsKey( key ) )
				{
					// System.err.println( "Warning: MasterLookup2:saveMasterUnit:"
					//	+ " Have already seen this master key: \"" + key + "\""
					//	+ ", for key field " + getMasterKeyFieldName()
					//	+ ", not including this later work unit in master list."
					//	);
					mWorkUnit.errorMsg( this, kFName, "Have already seen this master key: \"" + key + "\""
							+ ", for key field " + getMasterKeyFieldName()
							+ ", not including this later work unit in master list."
							);
					continue;
				}
				else
				{
					// Save this in the master list
					fMainHashTableWorkUnits.put( key, inWU );
				}
			}
			// Else we're doing some other type of work
			else
			{
				if( getDoMatchSubstrings() )
				{
					if( ! fMainExpressionsFlatList.contains( key ) )
						fMainExpressionsFlatList.add( key );
				}
				// Other structures, etc.
			}

			//// Process any copy instructions
			//doCopies( inWU, destinationWorkUnit );

		}
	}

	// For each matching key field in the work unit we're checking,
	// look it up against the master list.  Delete it if it's not in the
	// master list (unless we're inverting the logic!)
	void lookupUnit( WorkUnit inWU )
	{
		lookupUnit( inWU, true, false );
	}
	void lookupUnit( WorkUnit inWU, boolean doDeletes, boolean invertLogic )
	{
		final String kFName = "lookupUnit";
		// Sanity check
		if( inWU == null )
		{
			// System.err.println( "Error: MasterLookup2:lookupUnit:"
			//	+ " Was passed a null work unit to process!"
			//	);
			errorMsg( kFName, "Was passed a null work unit to process!"
					);
			return;
		}

		// Get the matching key fields
		List candidateFields = inWU.getUserFields( getCheckKeyFieldName() );
		if( candidateFields == null || candidateFields.size() < 1 )
		{
			mWorkUnit.debugMsg( this, kFName,
				"no matching fields to check"
				+ " for key " + getCheckKeyFieldName()
				);
			return;
		}

		// Create a queue of fields to delete
		// (we don't want to delete them here because it might affect
		//  the list iterator)
		List deleteQueue = null;
		if( doDeletes )
		{
			mWorkUnit.debugMsg( this, kFName,
				"Asked to actually perform deletes"
				);
			deleteQueue = new Vector();
		}

		// String for the audit field text
		String auditMatchText = null;

		// Now loop through each of the fields
		for( Iterator it = candidateFields.iterator() ; it.hasNext() ; )
		{
			// Variable to record which pattern matched
			auditMatchText = null;

			// Grab the field
			Element field = (Element)it.next();
			// Grab the field's contents
			String key = field.getText();
			// We must not have truely a NULL key
			if( key == null )
			{
				// System.err.println( "Warning: MasterLookup:saveMasterUnit:"
				//	+ " found a truly null field"
				//	+ " for key " + getCheckKeyFieldName()
				//	+ ", skipping this key."
				//	);
				mWorkUnit.errorMsg( this, kFName, "found a truly null field"
						+ " for key " + getCheckKeyFieldName()
						+ ", skipping this key."
						);
				continue;
			}

			// If it's not case sensitive, then normalize it
			if( ! getIsCaseSensitive() )
				key = key.toLowerCase();
			// Do trimming if requested
			if( getDoTrimStrings() )
			{
				key = key.trim();
			}
			// Ignore empty strings if requested to do so
			if( ! getDoIgnoreEmptyStrings() )
				if( key.equals("") )
					continue;


			// Logic change:
			// TODO: If simple match, then use hash
			// Otherwise, need to loop through each pattern as well

			boolean foundKey = false;
			if( getIsNormalHashLogic() )
			{

				// Whether we actually have the key in our list
				foundKey = fMainHashTableWorkUnits.containsKey( key );

				mWorkUnit.debugMsg( this, kFName,
					" key: \"" + key + "\""
					+ " foundKey: " + foundKey
					+ ", for key field " + getCheckKeyFieldName()
					);

				if( getAuditFieldName() != null )
				{
					auditMatchText = key + " (hash)";
				}
			}
			// Else we're doing a complex lookup
			// TODO: complex lookup, revisit?
			else
			{

				// If we're doing substring matches
				if( getDoMatchSubstrings() )
				{

					// For each pattern we're to check
					for( Iterator it2=fMainExpressionsFlatList.iterator(); it2.hasNext(); )
					{

						String pattern = (String)it2.next();

						mWorkUnit.debugMsg( this, kFName,
							" key: \"" + key + "\""
							+ " substring check: " + pattern
							);

						if( key.indexOf( pattern ) >= 0 )
						{
							foundKey = true;
							if( getAuditFieldName() != null )
							{
								auditMatchText = pattern + " (substring)";
							}
							mWorkUnit.debugMsg( this, kFName,
								"it matched."
								);
							break;
						}
						else
						{
							mWorkUnit.debugMsg( this, kFName,
								"it didn't match."
								);
						}
					}  // End for each substring to check
				}
				// Else we don't know what to do
				else
				{
					// System.err.println(
					//	"Error: MasterLookup2:lookupUnit:"
					//	+ " Set to use special logic, but substring logic"
					//	+ " is not set; substring is currently the only"
					//	+ " special logic available (though others are planned)"
					//	);
					mWorkUnit.errorMsg( this, kFName, " Set to use special logic, but substring logic"
							+ " is not set; substring is currently the only"
							+ " special logic available (though others are planned)"
							);
				}

			}




			// Record audit info if we were asked to do so
			if( getAuditFieldName() != null )
			{
				if( auditMatchText == null )
					auditMatchText = "(nomatch)";
				inWU.addNamedField( getAuditFieldName(), auditMatchText	);
			}

			// The flag for whether we've found a "match" or not
			// "good match" mean's we're generally "happy" and won't
			// be inclined to delete the values
			boolean goodMatch = foundKey;

			// Do we flip the logic?
			if( invertLogic )
			{

				mWorkUnit.debugMsg( this, kFName,
					"told to invert logic"
					);
				goodMatch = ! goodMatch;
			}

			mWorkUnit.debugMsg( this, kFName,
				"Found matching key: \"" + key + "\""
				+ ", for key field " + getCheckKeyFieldName()
				+ ", leaving it as is."
				);

			// See if there's a matching work unit for it
			if( goodMatch )
			{

				mWorkUnit.debugMsg( this, kFName,
					"In good-match block"
					+ ", currently just leaving it as is."
					);
				continue;
			}
			// Else not a good match!
			else
			{
				mWorkUnit.debugMsg( this, kFName,
				    "In bad-match block"
					);
				if( doDeletes )
				{
					mWorkUnit.debugMsg( this, kFName,
						"queueing for deletion"
						);
					deleteQueue.add( field );
				}
			}
		}   // End of for each matching field

		if( doDeletes )
		{
			int numDeletes = deleteQueue.size();

			if( numDeletes > 0 )
			{
				mWorkUnit.debugMsg( this, kFName,
					"performing " + numDeletes + " queued deletes"
					);

				// Now loop through each of the fields to delete
				for( Iterator it3 = deleteQueue.iterator() ; it3.hasNext() ; )
				{
					// Grab the field
					Element badField = (Element)it3.next();
					// "delete" it
					badField.detach();
				}
			}
			else    // Else there were no queued deletes
			{
				mWorkUnit.debugMsg( this, kFName,
					"no queued deletes to peform"
					);

			}
		}  // End if doing deletes

	}

//	void tabulateUnit( WorkUnit inWU )
//		throws Exception
//	{
//		List keys = inWU.getUserFieldsText( getSourceFieldName() );
//		Iterator it = keys.iterator();
//		while( it.hasNext() )
//		{
//			String key = (String)it.next();
//			// We must not have truely a NULL key
//			if( key == null )
//			{
//				System.err.println( "Warning: TabulatorV2:tabulateWorkUnit:"
//					+ " found a truly null field"
//					+ " for key " + getSourceFieldName()
//					+ ", skipping this key."
//					);
//				continue;
//			}
//			// Keep the original copy, we may want it later
//			String origKey = key;
//			// If it's not case sensitive, then normalize it
//			if( ! getIsCaseSensitive() )
//				key = key.toLowerCase();
//				// We don't lowercase the original key
//			// Do trimming if requested
//			if( getDoTrimStrings() )
//			{
//				// For this option we do trim both
//				// Todo: maybe have more options
//				origKey = origKey.trim();
//				key = key.trim();
//			}
//			// Ignore empty strings if requested to do so
//			if( ! getDoCountNullStrings() )
//				if( key.equals("") )
//					continue;
//
//			// Find the existing count
//			int oldValue;
//			// Is there key already there?
//			if( fMainHashTableCounts.containsKey( key ) )
//			{
//				Integer tmpInt = (Integer)fMainHashTableCounts.get( key );
//				oldValue = tmpInt.intValue();
//			}
//			else
//			{
//				oldValue = 0;
//			}
//
//			// Now create the new value
//			Integer newValue = new Integer( oldValue + 1 );
//
//			// And now store the integer
//			fMainHashTableCounts.put( key, newValue );
//
//			// See if there's already a work unit for it
//			WorkUnit destinationWorkUnit = null;
//			if( fMainHashTableWorkUnits.containsKey( key ) )
//			{
//				destinationWorkUnit = (WorkUnit)fMainHashTableWorkUnits.get(
//					key
//					);
//			}
//			else
//			{
//				// Create a new work unit for this key
//				// destinationWorkUnit = newWorkUnit( key );
//				// We'd like to preserve the original key in their data
//				destinationWorkUnit = newWorkUnit( origKey );
//				fMainHashTableWorkUnits.put( key, destinationWorkUnit );
//			}
//
//			// Process any copy instructions
//			doCopies( inWU, destinationWorkUnit );
//
//		}
//	}

//	// Run through the hash table and create the output
//	void outputTabulations()
//	{
//
//			// reset the counter
//			fCounter = 0;
//
//			// Loop through all the keys
//			Set keys = fMainHashTableWorkUnits.keySet();
//			Iterator it = keys.iterator();
//			while( it.hasNext() )
//			{
//				// Grab they key and value
//				String theKey = (String)it.next();
//				// The work unit
//				WorkUnit theUnit = (WorkUnit)fMainHashTableWorkUnits.get(
//					theKey
//					);
//
//				// Just to be safe, get rid of the hash's reference
//				// to this work unit
//				//fMainHashTableWorkUnits.remove( theKey );
//
//				// The count
//				Integer theValueObj = (Integer)fMainHashTableCounts.get(
//					theKey
//					);
//				String theValueStr = theValueObj.toString();
//
//				// Add the count field
//				theUnit.addNamedField( getDestinationCountFieldName(), theValueStr );
//
//				// Output this work unit
//				enqueue( fWriteQueue, theUnit );
//				theUnit = null;
//
//				fCounter ++;
//				if( fCounter % kReportInterval == 0 )
//				{
//					System.out.println( "Tabulator: Generated '" + fCounter +
//						"' output records." );
//				}
//
//			}
//			// Free up the references to work units
//			fMainHashTableWorkUnits = null;
//	}


//	WorkUnit newWorkUnit( String inKeyValue )
//		throws Exception
//	{
//
//		WorkUnit returnRecord = new WorkUnit();
//		addRunpathEntry( returnRecord );
//
//		// If we need freeform
//		if( getForceFreeForm() )
//			returnRecord.forceUserDataToFreeForm();
//
//		// Add the main field
//
//		// Get the key name
//		String dstFieldName = getDestinationKeyFieldName();
//
//		// Add in the value
//		returnRecord.addNamedField( dstFieldName, inKeyValue );
//
//		// Return it
//		return returnRecord;
//	}

	void addRunpathEntry( WorkUnit inWU )
	{
		inWU.createRunpathEntry( getID() );
	}

//	// Process all (if any) copy instructions we're to perform when
//	// we find a match
//	void doCopies( WorkUnit inSourceWU, WorkUnit inDestinationWU )
//	{
//		List instructions = fJdh.getJdomChildren();
//		Element branch = null;
//		for( Iterator it = instructions.iterator(); it.hasNext(); )
//		{
//			// Get the static copy instruction we're to work on
//			Element instruction = (Element)it.next();
//			// Get the instruction name, check and normalize
//			String insName = instruction.getName();
//			// This should never happen
//			if( insName == null || insName.trim().equals("") )
//			{
//				System.err.println( "Error:TabulatorV2:doCopies: null element name, skipping" );
//				continue;
//			}
//			insName = insName.trim().toLowerCase();
//			// Is it a branch instruction?
//			if( insName.equals( NEW_BRANCH_INSTRUCTION ) )
//			{
//				String branchName = getCopyDestinationBranchName( instruction );
//				// It's OK if the brach name is null, that means they
//				// want to clear it and not use a branch
//				if( branchName == null || branchName.trim().equals("") )
//				{
//					// This would be odd if they already have a null branch
//					// Maybe they're confused?
//					if( branch == null )
//						System.err.println( "Warning: Error:TabulatorV2:doCopies:"
//							+ ", encountered null/clear brach instruction"
//							+ " but branch was already null?"
//							);
//					// So clear it and loop
//					branch = null;
//					continue;
//				}
//				branchName = branchName.trim();
//				// Instantiate the branch
//				branch = new Element( branchName );
//				if( branch != null )
//				{
//					branch.detach();
//					// Attach it to the destination
//					inDestinationWU.addUserDataElement( branch );
//				}
//				else    // Branch was null???
//					System.err.println( "Error: Error:TabulatorV2:doCopies:"
//						+ ", got a null brach element back from jdom???"
//						+ " Subsequent fields will be copied to root of dest."
//						);
//
//				// Now all subsequent fields will be attached to
//				// this branch, until told otherwise
//			}   // End if it's a branch instruction
//			// Else is it a copy instruction?
//			else if( insName.equals( COPY_FIELD_INSTRUCTION ) )
//			{
//				// Run it against the source and destination work units we have
//				// If branch is null that's fine, doACopy will know what to do
//				doACopy( instruction, inSourceWU, inDestinationWU, branch );
//			}   // End else if it was a copy instruction
//			else    // Else we don't know what it is
//				System.err.println( "Error: Error:TabulatorV2:doCopies:"
//					+ " ingoring unknown instruction '" + insName + "'"
//					+ ", tag was: " + JDOMHelper.JDOMToString( instruction )
//					);
//		}
//	}

//	// Process all (if any) copy instructions we're to perform when
//	// we find a match
//	void doCopiesOLD( WorkUnit inSourceWU, WorkUnit inDestinationWU )
//	{
//
//		String branchName = getCopyDestinationBranchName();
//		Element branch = null;
//		if( branchName != null && ! branchName.trim().equals("") )
//		{
//			branchName = branchName.trim();
//			branch = new Element( branchName );
//			branch.detach();
//		}
//
//		List copyInstructions = fJdh.findElementsByPath(
//			COPY_FIELD_INSTRUCTION
//			);
//		for( Iterator it = copyInstructions.iterator(); it.hasNext(); )
//		{
//			// Get the static copy instruction we're to work on
//			Element copyIns = (Element)it.next();
//			// Run it against the source and destination work units we have
//			doACopy( copyIns, inSourceWU, inDestinationWU, branch );
//		}
//
//		if( branch != null )
//		{
//			inDestinationWU.addUserDataElement( branch );
//		}
//	}

//	// Todo: make return type meaningful
//	// would need meaningful return value from work unit method as well
//	private boolean doACopy( Element inCopyInstruction,
//		WorkUnit inSourceWorkUnit, WorkUnit inDestinationWorkUnit,
//		Element branch
//		)
//	{
//		String lSourceFieldName = getCopySourceFieldName( inCopyInstruction );
//		String lDestinationFieldName =
//			getCopyDestinationFieldName( inCopyInstruction
//			);
//
//		if( branch == null )
//		{
//			// work unit copy command
//			inSourceWorkUnit.copyNamedFields( lSourceFieldName,
//				lDestinationFieldName, inDestinationWorkUnit
//				);
//		}
//		else
//		{
//			List sourceData = inSourceWorkUnit.getUserFields(
//				lSourceFieldName
//				);
//			if( sourceData != null && sourceData.size() > 0 )
//			{
//				for( Iterator it = sourceData.iterator(); it.hasNext(); )
//				{
//					Element oldChild = (Element)it.next();
//					Element newChild = (Element)oldChild.clone();
//					newChild.detach();
//					branch.addContent( newChild );
//				}
//			}
//		}
//		return true;
//	}


	private void __sep__Constants__() {};
	//////////////////////////////////////////////////////
	// Control of the parameters to the Perl 5 regex compiler
	// see also calculateCompilerOptionsMask()
	private static final boolean DEFAULT_IS_CASE_SENSITIVE = false;
	private static final boolean DEFAULT_TRIM_STRINGS = true;
	private static final boolean DEFAULT_IGNORE_EMPTY_STRINGS = true;

	private static final boolean DEFAULT_INVERT_LOGIC = false;
	// private static final boolean DEFAULT_MATCH_WORDS = false;
	// private static final boolean DEFAULT_ALLOW_WILD_CARDS = false;
	// private static final boolean DEFAULT_MATCH_REGEXES = false;
	private static final boolean DEFAULT_MATCH_SUBSTRINGS = false;


	// What fields to check against
	// The field for the master record
	private static final String MASTER_FIELD = "master_field";
	private static final String MASTER_FIELD_2 = "key_field";
	private static final String MASTER_FIELD_3 = "key";
	// The field in the records we're checking, if different
	private static final String CHECK_FIELD = "check_field";

	// The field to record which string matched
	private static final String AUDIT_FIELD = "audit_field";

	// Normally the filter KEEPS only the fields that match the main master
	// list, and deletes those that do not match.
	// You can invert the logic, and have it discard matching items.
	private static final String INVERT_LOGIC_ATTR = "invert_logic";
	// providing alternative names is a pain for booleans
	//private static final String INVERT_LOGIC_ATTR_2 = "keep_no_match";
	//private static final String INVERT_LOGIC_ATTR_3 = "discard_matches";

	// Tabulate tag attribute: Whether to do case sensitive matching or not
	// When tabulating the input field
	private static final String CASE_SENSITIVE_ATTR =
		"case_sensitive";

	private static final String IGNORE_EMPTY_STRINGS_ATTR =
		"ignore_empty_strings";
	// old name, deprecated
	// private static final String IGNORE_NULL_STRINGS_ATTR_2 =
	//	"count_empty_strings";

	// Whether trim all values before comparison
	private static final String TRIM_STRINGS_ATTR =
		"trim_strings";
//	private static final String TRIM_STRINGS_ATTR =
//		"trim_before_tabulating";
//	private static final String TRIM_STRINGS_ATTR_2 =
//		"trim";


	// make an effort to match on words
	// private static final String MATCH_WORDS_ATTR = "match_words";
	// whether to respect the * in it's "DOS Style" usage
	// private static final String ALLOW_WILD_CARDS_ATTR = "use_wildcards";
//	private static final String MATCH_REGEXES = "match_regexes";
	private static final String MATCH_SUBSTRINGS_ATTR = "match_substrings";
	WorkUnit mWorkUnit;


	private void __sep__Member_Fields1__() {};
	//////////////////////////////////////////////////////

	// some debug variables
	private static long fCounter;
	private static final long kReportInterval = 1000;

	// The main JDOM node from the parameters section
	JDOMHelper fJdh;
	//JDOMHelper fTabTag;

	// The queues
	Queue fReadQueue;
	Queue fMasterValuesQueue;
	Queue fWriteQueue;
	Queue fTriggerQueue;

	// Some counts
	long fMasterCount;
	long fTriggerCount;
	long fMainCount;

	// Have we initialized, set by contstructor
	boolean fHaveDoneInit;

	// cache for name of field to read from
	// String fSourceFieldName;
	// cached name of fields to write to
	// String fDestinationKeyFieldName;
	// String fDestinationCountFieldName;

	// The keys to use for comparisons
	private String fMasterKeyFieldName;
	private String fCheckKeyFieldName;
	private String fAuditFieldName;

	// cached boolean attrs
	boolean fDoIsCaseSensitive;
	boolean fDoTrimStrings;
	//boolean fDoMatchWords;
	//boolean fDoAllowWildcards;
	//boolean fDoMatchRegexes;
	boolean fDoMatchSubstrings;
	boolean fDoIgnoreEmptyStrings;
	boolean fDoInvertLogic;

	// The main hash
	// Hashtable fMainHashTableCounts;
	Hashtable fMainHashTableWorkUnits;
	Vector fMainExpressionsFlatList;
	Vector fMainExpressionsListOfLists;

}
