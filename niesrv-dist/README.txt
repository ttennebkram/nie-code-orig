NIE Server Package README File

Copyrights
----------
Copyright New Idea Engeering, Inc., 2002 - 2008
Contact us at:
http://ideaeng.com or support@ideaeng.com

This product also includes software libraries developed by the
Apache Software Foundation - http://www.apache.org -
who hold the copyrights on those libraries and
various software libraries released under the "BSD" style license.


README Contents
---------------
Changes in this Version
Known Bugs
Introduction
Included Files
Setting Up Java
Configuration
Running (including output levels and how to stop the server)
Testing
Administration Commands
Modifying
Troubleshooting


Requirements in the 2.9x timeframe
Java 142+
MySQL 5.1+

pending
-------
	done? memory for Java, NIE_MAX_MEMORY, Unix versions as well?
	? fix to nuke_db, Unix versions as well?
	create_db / test_query bug
	nt service name
	register service, if dir has space?
	Swing forms: type = password
	/ Web forms: type = pasword
	configurator help

2.9xd March... '09
------------------
We will now require at least Java 1.4.2; previously we supported Java 1.2
This was done to improve character encoding issues.

2.9xc January 2009
------------------
New internal batch files
	_roll_dates.bat
		Roll dates forward for all search activity
	_import_fast_logs.bat
		Import FAST ESP search log data
	_generator.bat
		Generate simulated search activity from a rules file
		and populates the database

2.9xb July 2008
---------------
Added addendum to 2.9x readme (items that were changed but not documented)
Added escalation of invalid HTTP headers to IOException
Added <search_url use_careful_redirects="TRUE">

2.9x July 2008
-----------------
(addundum to 2.9x readme: these features were in this release
but left out of the readme file)
	Added support for different BASE URL
		Goes directly after <search_url> tag
		<base_url>http://your.other.site.com/</base_url>
	Added default name for SearchTrack log file if a directory is given
	Display admin URL in startup log
	changes to config screens
(end of readme addendum)
Added support for MySQL
fix: minor issue with PostgreSQL dates
fix: password display bug in IE caused by white space
fix: import/export with dates, across databases
Config (Swing)
	replace tabs with tree
_ Internal
	created Backfill, including Google counts
	created Populator
	changes to DNS and Backfill

2.8x 8/1/07
-----------
Revisit Lucene index serving.
Still using old config tabs setup, though in new UI.


pending 27xc (6/20/07)
-------------------------------
New Configurator UI, config tree card layout vs. config tabs
More debugging code when saving Maps, commit, change

pending 27xb (5/25/07) changes
------------------------------
Done
	Handling of null searches
	Option to not log feature, via IP address...
	Revisited startup logic

Null Searches and Test Searches
* You can choose to redirect folks on a null search
* Treat certain key phrases as if they were nulls (will be PROMOTED to nulls)
* Add "(null)" as a map-able search term
* Whether or not to LOG null searches (default is to NOT log)
* Whether or not to LOG searches from certain IP addresses
<nie_config>
  <search_tracking>
    <data_logging site_id="10" ignore_null_searches="TRUE">
      <ignore_address>localhost</ignore_address>
    ...
  <null_searches>
    <redirect_on_null_search enabled="TRUE">
      <!-- "return to sender" -->
      <url>(referrer)</url>
      <!-- or a specific URL
      <url>http://ideaeng.com</url>
      -->
    </redirect_on_null_search>
    <!-- You can map certain phrases to be treated the same as a null search -->
    <null_search_equiv_phrase>enter search here</null_search_equiv_phrase>
  </null_searches>
  ...
</nie_config>



Changes in this Version (pending .... 2.7x - 11/26/06)
------------------------------------------------------
Include Lucene JAR

Handle case of no Lucene index specified (which is actually quite common)

Support POST
	search_url method="..."

API / Snippet support: API support and logging
http://polaris:9001/?nie_context=snippet&id=36902671&pageid=r&mode=ALL&query=nie&collection=&doctype=&language=
No matches gives:
	<!-- Info: NIE SearchTrack: No Suggestions -->
Logs by default
	nie_context=proxy
	- - - -
	nie_context=snippet
	- - - -
	nie_context=log_event
	- - - -
	nie_trans_type
		search = 1
		doc click = 2
		user feedback = 3
		ad exposure = 4
		ad click through = 5
	nie_num_found (and NOT nie_num_results)
	nie_num_searched
	nie_original_query
	nie_normalized_query
	nie_form_name
	nie_dest_url
	nie_ref_trans_id


Changes in this Version (2.62 - 6/27/05)
----------------------------------------
Upgrade Postgresql JDBC driver to 8.03

Clarification: as of v2.60 the dns_resolver script is no longer needed.
	This functionality is now automatically run via the
	"cron lite" internal thread.
	dns_resolver.bat and .sh have been removed.

configurator.bat and .sh have the underscore "_" prefix removed, indicating
	that the Configurator has moved from Alpha to Beta status


Changes in Version (2.61xb - 6/23/05)
-------------------------------------
Internal release, to provide more debugging information.


Changes in Version (2.61x - 6/23/05)
------------------------------------
Internal jar-only release
Fix to link generating code for report drill downs

Alpha of Lucene index search:

In main.xml you would do:
	<include location="lucene.xml" />

Then in lucene.xml you would have:
<lucene_search>
	<index_directory>
		c:\path\to\lucene\collection
	</index_directory>
</lucene_search>

The form would look like:
<form action="http://myserver:9000/lucene" >
	Search for:
	<input name="query">
	<input type="submit" value="Search">
</form>



Changes in Version (2.60 - 12/9/04)
-----------------------------------
NEW easy-to-remember Admin / login URL:
	http://yourserver:9000/login
	(will also work with http://yourserver:9000/admin)

Fix to Web Form rendering so that, if no sub-site fields are defined, that
section of the form is not displayed.

Additional Admin menu items in main reports menu (if logged in as Admin)

Changes to Configurator:
	Moved and changed wording for some fields and buttons
	Menu
	Save and Refresh - to update a running server
	Hourglass cursor for long operations

"Cron jobs" added:
	Keeps DNS entries up to date
	Caches 3 trend reports

Processing timing added.  Set pseudo class "timing" to verbosity "info"


Changes in Versions (2.51xd, xe, xf - 12/9/04)
----------------------------------------------
(internal test releases)

Changes in Version (2.51xc - 10/25/04)
--------------------------------------
(internal release)

Fixed buffer append issue in NIEUtil to use older append(String), believed
to be an issue with the JVM that Oracle ships

Made lookup_date an optional field in nie_domain_names to make domain names
easier to import from a flat file.


Changes in Version (2.51xb - 10/6/04)
-------------------------------------
Additional cosmetic options for Look and Feel, including CSS class names.
Also impacted the config UI app.

2.51xa
------
(internal release, small fixes)

Changes in this Version (2.50 - 5/27/04)
----------------------------------------
Internal release featuring Alpha of several new NIE tools and features:

UI: Create Map screen now has a "Check URL" button

Command line tools:
(_tools and these notes to be removed if shipping to customer)

_configurator.bat/.sh
	Graphical config file editor
	Run with no arguments for syntax.

_vcheck.bat/.sh
	Verity checkup tool
	Run with no arguments for syntax.

_spyder.bat (PC only for now, should work on unix with .sh)
	Looks for search forms on Web sites
	Very early PRE-alpha
	manually edit the batch file for command line arguments ARGS
	Notes:
	some are possibly obsolete now

	Very handy option # 1:
	-list urls.lst  will accept a list of URLs in a file to go after
	Where URLs can be:
	http://www.speific.url
		OR
	domain.com (with no http, etc) and I will GUESS the URL
	^^^ (handy for stripped email addresses, but YOU remove name@)

	Very handy option # 2:
	-cache cache_dir  will cache web content to make re-running faster!
	when using -cache, any failed web page retrieval will have
		detailed errors in its cache file, so you can see why


Changes in Version (2.50 - 5/27/04)
-----------------------------------
First June release candidate.

Changes to startup and failover logic.  Server should start at least
in pass-through mode with *almost* any config error, and respect the
refresh command.  This means that end users should always be able
to run searches, regardless of most misconfiguration or licensing issues.

A sample license file.  Should be embedded under main nie_config node
or included via the <include ...> tag.
Licensing dates are in GMT.

lic.xml
-------
<license
    rev="1"
    company="Your Company"
    start_date="1-may-2004"
    end_date="1-oct-2004"
    server="server1"
    key="1111-2222-3333-4444-5555"
/>

Note:
Our stock demo will no longer run since it does not have a license key;
a license key would need to be generated and included.


Changes in Version (2.33xd - 5/19/04)
-------------------------------------
Quick fixes to Create Map form wording and linked help.

Fix problem with lower casing suggested URLs.

Fix problem with changing a list of multiple meta item check boxes.



Changes in Version (2.33xc - 5/19/04)
--------------------------------------
First internal release of license keys.

Redesign of navigation menu.

Changes in Version (2.33xb - 5/13/04)
-------------------------------------
First internal release of moded field REPORTS.

Code to fix spurious null-search logging caused by spuriuos
web browser HTTP requests


Changes in Version (2.33xa - 4/20/04)
-------------------------------------
First internal release of moded field matching.

Interim Internal releases:
2.32xc 4/16/04
2.32xb
2.32xa

Changes in this Version (2.32 - 4/8/04)
---------------------------------------
Tweaks to text ad class.


Changes in Version (2.31 - 4/1/04)
----------------------------------

Fixed iso-vs-utf8 codepage CGI decode issue.

Removed warning for empty search engine test drive field values.

UI and Reports:

If no user classes are defined, we still present the older hyperlink-style
action links; if user classes are involved, then drop down action
lists are used.


Changes in Version (2.30 - 3/24/04)
-----------------------------------

General
-------

First candidate for April release.

Ability to edit Advertisements or other custom promotions.


UI Changes
----------

Change from edit hyperlinks to edit action mini-forms, to allow for many
more types of objects to be managed.

Pressing Enter in the Query Maps form will now run a lookup query.
Pressing Enter on other forms is equivalent to having clicked Cancel.



Reporting
---------

Fix to chained reports when a user had advanced to page 2.

Change to sorting:
All reports now use the most recent search time as a secondary sort criteria
to act as a "tie breaker" and prevent random sorting.
For example, if many searches each occurred 5 times, the most recent search
will always be listed first within that group.

Null values:
If the server is unable to parse out the number of documents that matched
a search, it will now correctly show a null value in the pertinent reports,
vs. showing a false zero.  This will only happen if the patterns in the
search engine info section are incorrect; a properly configured system
will not have nulls in this field.



Configuration / System
----------------------

New shorter format for the default transaction message, now less than 80 chars.

Default Docs found pattern matching character maximums increased by 5 chars.

Change in Markups:
To support tighter embedding of NIE suggestions into customer tables, we
have removed some of the white space that was added to our markups.
To simulate the old behavior you can use something like:
<nie_config>
	...
	<search_engine_info>
		<vendor>...</vendor>
		<search_term_field_name>...</search_term_field_name>
		<search_url>... </search_url>
		...
		<markup_after>
		<![CDATA[
			&nbsp;<br>
		]]>
		</markup_after>
	... etc ...


Distrubution Files
------------------

Alpha test of our first Windows installer.
Issue: Installer does not yet help you configure the system or register
the service.
Issue: Uninstaller does not yet completely clean up all of our files.

Cleanup and reorganization of the samples directory.

Cleanup of various spurious messages in the log file.

Added Unix scripts init_db.sh and dns_resolver.sh
Note: Due to packaging issues, sometimes .sh files need to have the "execute"
flag set.
Note: nuke_db.bat has not yet been ported to nuke_db.sh



Changes in Version (2.26a - 3/3/04)
------------------------------------
Fix to table creation / field indicies with Oracle and MS Sql

Fix to trend report subtitle (now says N, vs N x 2)


Changes in Version (2.26 - 2/18/04)
-----------------------------------
Fixes to dns_resolver batch file and class files.

Updated system Jar files to JDOM b9 (was JDOM b7)

Bug fix: admin Refresh command now works properly

Known issue: spurious warning message on refresh:
	WARNING: 2/18/04 11:06:40 AM PST SearchLogger.initSqlFieldInfo:
	Tables have already been initialized, returning.
This is a harmless warning message and will be fixed in a subsequent release.

Known issue: Product may not run if installed in a very deep directory tree
Cause: class path is getting very long; length depends on install directory
Workaround: Install the product in a directory with a shorter overall path


Changes in Version (2.25b - 2/9/04)
-----------------------------------
Fix to Postgresql specific date range bug; change applied to all reports.

Change in startup banner and product name.

Changes to formatting of reports:
- new message in records area if no matching records were presented
- more white space between the bottom of the report and the bottom menu bar

Some cleanup of the distribution directory.


Changes in Version (2.25 - 2/4/04)
----------------------------------
Different passwords for different levels of access.

In main.xml
-----------
<nie_config)
	...
	<search_tuning
		admin_password="wheel"
		read_only_password="sales"
	>
		...
	</search_tuning>
	...
</nie_config>

Previously the attribute to set a password was simply "password" - to maintain
config syntax compatibility this is now an alias for admin_password.

Therefore:
	<search_tuning
		admin_password="wheel"
		...
And:
	<search_tuning
		password="wheel"
		...

are equivalent.

Also, in terms of CGI parameters passed into the NIE Server, that is still
simply "password"

In the above example, these two URL's would provide different levels of access:

http://your_server:port/?nie_context=admin&command=report&password=wheel

http://your_server:port/?nie_context=admin&command=report&password=sales


In both cases a menu and report will be presented, but the menu given the
the "sales" user will not allow changes to the mappings.


Other changes in this release:

Cleanup of extra variables in the URL.


Internal preview of the "Hot Links" report.
The URL would be:
http://your_server:port/?nie_context=admin&command=report&report=HotLinks

Notice that this URL does not require a password because HotLinks is a
public report.


Known Issues with this version:

"(null search)" does not appear in the "Searches with No Hits" report.

Two reports of the admin refresh command not working properly; some
configuration operations don't seem to take effect.  The workaround
for now is to simply restart the server.  This is not a problem for
seeing new mappings when using the database as a repository.


Changes in Version (2.20b - 1/17/04)
------------------------------------
Fix to navigation/return-url between maps listing screen and add/edit screens

Simple validation (syntax only) of URLs that are entered on the form.

Added http:// to URL field of blank form

Allow editing of maps that have no URLs but that DO have Alterate Terms;
previously we complained that there was no URL whether there were alt terms
or not.

Remove "* indicates required field" message from forms that don't need it

Preserve Redirect flag when editing redirect records; previously they were
incorrectly changed to WMS records during commit.

Allow for blank titles when editing a redirect URL.


Changes in Version (2.20 - 1/15/04)
------------------------------------
New compact, horizontal Reports Menu

New default Reports screen - shows most popular searches for the past week

New Administration menu.  Provides access to all the defined search maps,
allowing you to search by term or URL; it also supports wildcard searches
with asterisks (*)

Fixes to Trend Reports

Faster commit time.

New CSV data exporter (via the hello_db script)
Run the script with no arguments for full syntax.

Changes to report fonts for more consistency; now features mostly sans-serif
fonts in the body of reports.

Various internal fixes and changes.

Changes in Version (2.12 - 12/22/03)
-------------------------------------

Search Engine configuration changes:

Change to search engine config to specify extra fields that need to be
sent to your search engine.  Normally customers have these as hidden fields
on their search form, but when we have a "show results" link in our reports,
we don't have access to those hidden fields; this is a way to provide us with
that information.

Typicially in search_engine.xml
-------------------------------
<search_engine_info>
	...
	<search_url>
	<![CDATA[
		http://search.freefind.com/find.html
	]]>
	</search_url>
	<!-- NEW Option -->
	<search_url_test_drive_fields>
		<field name="id" value="5555555" />
		<field name="pageid" value="r" />
		<field name="mode" value="ALL" />
	</search_url_test_drive_fields>
	...
	... rest of search engine options ...
	...
</search_engine_info>


Change to search engine configuration to work around nested HTML table issues:
In some web pages, when we add our markups, some existing content may be
"squished" or otherwise improperly displaced.  This is usually caused
by nested HTML tables and the browser trying to accommodate all content.

The fix is usually to insert additional table rows in the HTML table.
This is typically done in the search_engine.xml config file.
An example of this would be:

<search_engine_info>
	...
	... search engine vendor, url, etc ...
	...
	<suggestion_marker_text>
	<![CDATA[
		... your existing text telling us where to place markups ...
	]]>
	</suggestion_marker_text>
	<!-- NEW OPTION -->
	<!-- Causes an additional table row to be inserted after
		our markup, but before the content that was already there.
		The content that was already there is then put on its own
		row of the table and has plenty of room to display properly.
	-->
	<markup_after>
	<![CDATA[
		</td></tr> <tr><td>
	]]>
	</markup_after>
	<!-- You can also do markup_before -->
	...
	... rest of search engine options ...
	...
</search_engine_info>


Trend Reports:

Trend report added to main menu screen.

Trend reports cached, by day, to memory and disk.
Each interval (1 day, 7 days, etc) is cached for that day if it's run.

To force caching for the 3 trend reports, fetch these 3 URL's:
(Each URL should be on ONE line, broken here for readability)

http://server.com:9000/?
	nie_context=admin&command=report&password=foo&report=ActivityTrend&days=1
http://server.com:9000/?
	nie_context=admin&command=report&password=foo&report=ActivityTrend&days=7
http://server.com:9000/?
	nie_context=admin&command=report&password=foo&report=ActivityTrend&days=30

You don't need to do anything with the output; the mere fact that the URL was
fetched will cause that day's report to be cached.
A report_cache directory will automatically be created under your existing
config directory.


Database Changes:

Now have FULL support for Postgres (aka: Postgresql)

"oracle" now means Oracle 8i and 9i; we fall back to the older 8i syntax.

Fix to Oracle "too many cursors open" issue.


UI Changes:

When a term is to be edited, and that term exists in more than one mapping,
a list of matching maps is presented.  The Admin can then choose which map
to edit.

Fix to Create Directed Results form to show the currently configured system
default value in drop down lists.

Support for editing multiple URLs in a single Map.
(currently no way to ADD additional urls via the UI, but you can edit all
 the urls that are already there, presumably from a legacy XML conversion)


Misc:

Bug fixes for various reports, including click-through on null searches.

Suppression of some IE related spurious (and harmless) socket errors.
There still may be some others.

Advertisement related report links are no longer shown if no Ads or
User Markups are defined.  User Markup classes and style sheets are
still defined via the global XML config file.

Advertisements can now be stored in the database; they are not currently
editable in the UI.

Some support for meta data items stored in database; limited, and not editable
via the UI.



Changes in Version (2.11 - 12/9/03)
-----------------------------------
Version numbering adjusted from 1.x to 2.x (prev v 1.26 should have been 2.1)

New intermediate Map selection screen when you try to edit a term
that has more than one associated map.

Misc. adjustments to the Create Map form.

* System database schemas were changed in this version.
  You will need to reinitialize the database with nuke_db.bat, see below.
  Also see warnings section.

Changes to database scripts:

hello_db and init_db will now accept either a standalone database
configuruation file, or will look for the embedded configuration inside
a full Search Track config file.

Added script nuke_db.bat, which will completely wipe out and recreate
tables.  This was needed to force schema upgrades.
WARNING:
This is a DESTRUCTIVE operation and will destroy existing data.
If you need to presever earlier data, please contact New Idea Engieering
for assistance.

New option to fix nested tables:

There is a new option for placing our content in web pages that have nested
tables and where NIE content is incorrectly jammed together with other content.
Inside of the search engine configuration you can now have:
...
<search_engine_info>
	... <search_url> ... <suggestion_marker_text> ... etc ...
	<markup_after>
	<![CDATA[
		</td></tr> <tr><td>
	]]>
	</markup_after>
</search_engine_info>
...
In the above example, the Webmaster Suggests box will 

Misc. changes to DBConfig command line options. Not normally accessed by
end users or admins.


Changes in Version (1.26 - 12/4/03)
-----------------------------------
Debut of UI.  Requires DB setup.

Alpha of Postgresql support.

Start reports as usual:
http://your-server:port/?nie_context=admin&command=report&password=your-pswd

The most popular searches and popular searches with no hits are
now hyperlinked.

Known issues:
* No install notes for dns resolver; not tested with Postgresql
* Some reports are broken with postgres
* Startup with no maps in a new database will give warnings



Changes in Version (1.25b - 10/27/03)
-------------------------------------
Small change to totals in "searches per day" report, per jessica

Changes in Version (1.25 - 10/24/03)
------------------------------------
Improvements to Database reconnect code

The <database> tag has been promoted to a top level configuration item.
The old path still works but will give a warning.

Internally:
Start of framework for new trend reports
Coding towards universal DB support in reporting
Start of UI framework and schemas

Changes in Version (1.24 - 5/23/03)
-----------------------------------
Expanded status information displayed for "show all" command:
	* Added startup info about time, config file, version, etc
	* Added advertisement listings to the maps
	Command is still:
	http://server:port/?nie_context=admin&command=showall

Added Online web browser viewing of last N messages:
	Command is:
	http://server:port/?nie_context=admin&command=messages&password=xyz

Improvements to reports:
	* translated DNS addresses now shown in reports
	* Advertisement related reports now show percentage clickthrough

More logging added to DNS lookup utility to see what it's doing.


Changes in Version (1.23-beta - 5/21/03)
----------------------------------------
First deployment of newer DNS resolver process.

Changes in Version (1.22-beta - 5/16/03)
----------------------------------------
First customer deployment of new, XML defined reports.

Changes in this Version (1.21 - 5/5/03)
---------------------------------------

Fix: Fix to XSLT style sheet for "show all"

More visible banner in run log when the server first starts up.

More visible banner in run log when the server is "refreshed" and has
reread its configuration.

More visible instantiation of User Data types, such as Ads; they are now
reported as full Status-level messages during startup.  If ads to not
display, this is a good thing to double check in the log file - whether
or not that Ad class was reported as being loaded at startup - if it's not
reported, then the ads will certainly NOT work.


Changes in Version (1.20-dev - 4/25/03)
---------------------------------------

*** RENAMED PRODUCT ***
	Old Name: SearchNames
	NEW Names:
		NIE Server: Main HTTP server process
		Search Tuning: Marking up the results list
		Search Tracking and Reporting:
			Logging and reporting of search activity


*** Configuration Changes due to Renaming ***

When a product is renamed, the support files and configuration options
also need to change, to reflect the new name.


Configuration Parameter and Script File Changes:


Change to Directory Name:

The main distribution tar and zip files, and the directory that they create,
has been changed.

	Old Names: (where Vvv stands for the version string)
	snVvv.tar
	snVvv.zip
	snVvv (directory)

	NEW NAMES:
	niesrvVvv.tar
	niesrvVvv.zip
	niesrvVvv (directory)


Change to Script / Batch Files:

	Old Names:
	searchnames.bat
	searchnames.sh

	NEW NAMES:
	niesrv.bat
	niesrv.sh


Change to XML for Search Tuning (aka: SearchNames) configuration:

	Old Syntax:
		<search_names>
			... config data ...
		</search_names>

	NEW Syntax:
		<search_tuning>
			... config data ...
		</search_tuning>

	* <search_names> will still work, but will generate a reminder warning


Two other changes to the <search_tuning> tag:

* The attribute port="nnn" has been DEPRECATED.
	The port is now taken from the <nie_server_url>
	(formerly the <search_names_url> tag, discussed later)

* And admin_password="xyz" has been changed to just password="xyz".

	Example of complete old syntax:

		<search_names admin_password="demo" port="9000">
			... etc ...

	Example of complete NEW Syntax:

		<search_tuning password="demo">
		<!-- ^^ renamed tag, ^^ renamed password, NO port= -->
			... etc ...


* admin_password="xyz" will still work, but will generate a reminder warning

* port="xyz" will give a warning if used, assuming it agrees with the URL.
  port="xyz" will give an ERROR if used, if it does NOT agree with the URL.


Change to NIE server process URL parameter:


	Old Syntax:
		<search_names_url>
			http://nie_server_machine:port
		</search_names_url>

	NEW Syntax:
		<nie_server_url>
			http://nie_server_machine:port
		</nie_server_url>

* <search_names_url> will still work, but will generate a reminder warning


Change to XML for Search Tracking (aka: SearchTrack) configuration:

	Old Syntax:
		<search_track>
			... config data ...
		</search_track>

	NEW Syntax:
		<search_tracking>
			... config data ...
		</search_tracking>

	* <search_track> will still work, but will generate a reminder warning


CGI Parameter Changes:

	Controlling the context/action of the server
	Old: sn_context
	NEW: nie_context
	(sn_context will still work, but will generate a reminder warning)

	Entering the Administrator Password:
	Old: admin_password
	NEW: password
	(admin_password will still work, but will generate a reminder warning)





Changes in Version (1.11-dev)
------------------------------------
Internal release, new framework for Search Activity Reports.

Changes in Version (1.10b - 3/18/03)
------------------------------------
Fixed issue with logging search statistics fields, the "n of m logging"

Changes in Version (1.10 - 3/11/03)
------------------------------------
Added support for logging Advertisement click-throughs

Heavier testing of modular configuration.

Deprecated <search_names ... port="nnn"> attribute.
	Port number is now take from the Search Names URL


Changes in Version (1.05dev - 2/1/03)
--------------------------------------
Much better handling of relative paths and files that reference other files.

Changes in Version (1.04dev - 1/30/03)
--------------------------------------
Added support for XML based user data and XSLT based formatting
	First use will be for the "Ad Server"

Added support for detailed Version and configuration banner
	-v or -version flag on command line
	?sn_context=admin&command=version  with a web browser

Added support for <include> tag in XML config files
	<include location="my_other_file.xml" />
	This should make management of large configurations easier

Known Limitation to File References:
	Search Names XML configuration files can reference other files,
	such as XSLT style sheets or included XML config files.
	These secondary files are presumed to be relative to the original file.
	HOWEVER, if the original file referenced contains a "../" or "..\"
	in it's path, the other files will not be correctly found and you
	will get various I/O errors.

Changes in Version (1.00 - 12/3/02)
-----------------------------------
No bugs reported in SN 099b release.

Completion of performance testing:
	- Sustained 14 markup proxies per second on a single low end Linux box
		Proxy operations are the most expensive we perform.
		Rate is equal to 1.2 million full proxy queries per day.
		Assuming 10% of queries are full proxy, est possibly 12 M / day

No code changes made in this release.


Changes in Version (099b - 11/26/02)
------------------------------------
Changes and fixes to register_service.bat
	- fixed prompting problem with xcopy command
	- now checks that run logging is set to a file
	- also checks that run logging file is absolute
	- detailed error and sample syntax if it's not right
	- service name can have spaces, though still not suggested

Changes in Version (099 - 11/18/02)
-----------------------------------
Changed: Added option to support case sensitivity in CGI fields
	for search engines that are case sensitive, such as Fulcrum
	or JSP oriented systems.  The new option is part of the
	Search Engine configuration.  The default is still to be
	case insensitive.

	For example:
	Enabling this feature for Fulcrum:

	<search_engine_info>
		<case_sensitive_cgi_fields>true</case_sensitive_cgi_fields>
		... other options such as ...
		<search_term_field_name>searchText</search_term_field_name>
		...
	</search_engine_info>

Fixed: Spaces are now handled properly for our install directory on Windows.
	For example, you can now install us under C:\Program Files

Changed: Support for user defined JVM
	By default we use the default Java executable in your
	environment's search path.  This is not always correct.

	When running searchnames.bat on Windows, you can now define the
	environment variable JVM to be a full path to your Java executable.
	This is particularly handy if you have more than one JVM installed.

	For examples of why you might change it:
	The default Internet Explorer JVM from old versions does not work.
	The JVM shipped with Oracle does not work as a Windows Service.

	On Unix, the default jvm on your path is used.


Changes in Version (098 d - 11/15/02)
-------------------------------------

Added: SearchNames can now run as a full Windows service.
	See register_service.bat
	WARNING: running multiple servers on the same machine,
	as windows services, is technically unsupported.
	It is possible, in theory, but a little tricky to setup.
	Simply running register_service twice, with two different
	configurations, will NOT WORK correctly.

Added: Database reconnection logic added.
	When logging queries and an error occurs, the system will
	try to reestablish a connection later.
	Includes user selectable retry waiting interval.
	And can be disabled entirely.
	Default is 2 minutes.
	Example:
	<database ... retry_wait_ms="15000" />
	would mean a 15 second delay.
	Does not try to reconnect if database is down at SearchNames startup.

Added: Ability to hyperlink the Webmaster Suggest Icon
	The webmaster_suggests_icon and default_webmaster_suggests_icon
	tags both recognize two new attributes:
		href="some url to click on"
		target="..."

	href is the most useful one.  You give it the URL you want
	to goto when the user clicks on the image.

	target is for sending folks to a new window, for example:
		target="_blank"
	will open a NEW browser window when they click.

	Syntax:
	To add a default to all maps:
	<default_webmaster_suggests_icon
		src="http://ideaeng.com/sn/images/tower.gif"
		href="http://clickable-url"
		... other options ...
	/>

	To add to a specific map:
	<map>
		... <terms> ...
		... <urls> ...
		<webmaster_suggests_icon
			src="http://ideaeng.com/sn/images/tower.gif"
			href="http://clickable-url"
			target="_blank"
			... other options ...
		/>
	</map>
	This would also open the new web page in a NEW WINDOW.


Added: Ability to suppress the display of the URL
	in the Webmaster Suggests box.

	Syntax:
	<url display="false">
		... rest of url data ...
	</url>
	This would be inside of a map.

	You can make this the DEFAULT with:
	<default_url_settings display="false" />
	This would be directly under the MAIN search_names tag,
	as a sibling to the other default tags.

Added: Ability to use a COLORED border around the Webmaster Suggests box
	Syntax:
	<default_webmaster_suggests_box
		border="1"
		border_color="#000088"
		...other settings...
	/>
	The above would create a blue "pinstripe" border.
	Note that, if you use a border color, the border is not shaded.

	Border can also be a number larger than 1.
	border="3" border_color="#000088" would make a heavier
	blue border.

Fixed: Absolute path names to XML config files using Windows syntax now works.
	They will be automtically converted to file: url syntax for use
	by the JDOM libraries.


Changes in Version (098 b and c - 10/28/02)
-------------------------------------------
More sample database configuration files in samples directory
	See hello_db_*.xml
Option to show first row of the table in addition to count
	when first connecting, this is OFF by default
Support for extra_parameters in database configuration
	this allows for extra parameters to be passed to ODBC sources
Have tested with:
	Oracle, Postgres, Excel
	also appears to correctly attempt to connect to SQL Server

* Warning about Excel *
If you will be writing to an Excel file, please make sure to:
	- Have a backup copy saved somewhere
	- Do not have the file open in Excel while the database is writing

Changes in Version (098a - 10/23/02)
------------------------------------
First internal release of multiple database support:
	Oracle (native jdbc, and odbc if driver installed)
	SQL Server (native jdbc and odbc)
	PostgreSQL (native jdbc, and odbc if installed)
	MySQL (native jdbc)
	Excel (via ODBC)
	Access (via ODBC)
	CSV
	Any Predefined DSN for ODBC on Windows
	Others

Needs doc and testing.

SEE samples\hello_db_XXXX.xml


Changes in Version (097b - 10/22/02)
-------------------------------------
Tweak to JDBC prepared statement usage to allow for better throughput

Changes in Version (097)
-------------------------
Fix to -check_config port bug
Fix to make search logging errors not stop search names by default
Workaround to support adding <BASE> tag to top of invalid HTML pages
New "config" directory for customers' XML config files
New sample Google configuration in samples directory

Changes in Version (096)
-------------------------
Added Search Track Logging Option
Cleanup of Samples directory

WARNING / NOTE:
If you get an error about the JVM not running, or so a VERY long
error message in the log file, it's probably just that we couldn't
talk to the database in the search_track logging database section,
so just double check your settings.

This will be fixed in the next version.


Changes in Version (095)
-------------------------
Packaging of hello_db for customer testing.

Changes in Versions 093 and 094
-------------------------------
Internal releases.

Added <run_log> tag.
Added support for Oracle.

Changes in Version (092)
------------------------
This is an internal engineering release only.
It's main purpose is as a package to transmission.
Other internal code changes are in progress.

For this internal release only, there is a source_code.tar
file in the main directory.


Changes in Version (091)
-------------------------

Run Log output overhauled.  Much more control over verbosity.
	SEE the "Running" section for more details about Output Levels.

The "big green exclamation mark" has been removed.

	In previous versions, for Webmaster Suggests, if no icon is specified
	as a decoration for the left side of the shaded box, then a
	large green exclamation point was put in the box instead.
	The workaround to get rid of it was to have an icon of a single
	transparent pixel.

	This has been fixed.
	If no icon is specified, then no decoration is put in the shaded box.

There were also substantial internal code changes made in preparation
for CGI access to maps; this *shouldn't* cause any noticeable change
for users.

Changes in Version (009)
-------------------------
NEW: "refresh" command:
	http://your_machine:9000/?sn_context=admin&command=refesh
	&admin_password=your_password
	(all on one line of course)

Fixed: Shutdown command is much more reliable, should not need refresh.

Changes in Version (008)
------------------------

NEW: "Ping" command:
	http://your_machine:9000/?sn_context=admin
		or
	http://your_machine:9000/?sn_context=admin&command=ping

	Since ping is the NEW DEFAULT admin comtext, you don't have
	list it.  No password is required.

NEW: Command line option -debug (or -d)
	Warning: gives LOTS of output.
	Currently there is no granularity on the amount of output
	or for which classes debug is enabled.

NEW: New Troubleshooting section in README

Fixed: corrected a few typos in the README doc to reflect
	minor syntax changes.

Internal fix: Corrected bug where we would likely drop some header
	fields from the host search engine during a proxy search.
	Currently this would have had no inpact on customers.


Changes in Version (007)
-------------------------

NEW: admin_password added to XML Config file (for admin authentication)
	See Configuration section for details

NEW: Shutdown command added (requires refresh)
	See new Administration Commands section for details

Fixed: Admin command to list mapped terms and URLs
	See new Administration Commands section for details

NEW: Check validity of config file without starting new server process
	See Configuration section for details

NEW: searchnames.sh for Unix with indentical same usage as searchnames.bat
	Includes support for command line parameters
	See Running section for details

NEW: first draft of Search Names Syntax Reference Guide
	See:
	doc/searchnanes-reference.doc
		OR
	doc/searchnanes-reference.html (for non Windows users)

Changed: CGI field nie_sn_context changed to sn_context (used for admin)

Changed: CGI field cmd changed to command (used for admin, easier to remember)

Changed: Less debugging output


shutdown command, may need refresh
	syntax for shutdown
list all config mappings now working
	syntax for command
.sh added
ref doc added!
less debugging


Changes in Version (006)
------------------------
(see also 005)

Fixed: Moved <BASE> tag up in HTML Header section to be first item.
	This fixes problems with references to cascading style sheets
	and early-defined Java Script routines.
	This was the Miles / Morgan Stanly / "font size" bug
Documented: URL for displaying all mappings
	http://your_machine:9000/?sn_context=admin&command=showall
Enhanced: added a few more things to "show all" page, still needs more work
Documented: Mile's selected text console issue

Changes in Version (005)
------------------------

Fixed: POST bug!!!  Please test.
Changed: This version has lots of debugging turned on
Slight change to packaged files:
	Changed: Base name changed from searchnames_packageNNN to just snNNN
	Added: Creation of new "tools" directory, only 2 files
	Added: system\dev-doc, some geeky notes, may be of interest
		includes "todo" list, in rather terse format
Internal changes to searchnames batch file.
	Better checking of environment variables
	Much better error suggestions
ADDED: Some additional suggestions in Confugration and Running sections
	of this README.txt file

Changes in Version 0.04
-----------------------

Now Supported: can specify a config file on the command line
Fixed: bug that sometimes prevented <base> tag from correctly being inserted
Fixed: Google bug with gzip'd contents
Fixed: proxy-mode is now the DEFAULT, so no need to include a hidden field
ADDED: Support for many more formatting options.  See Culver xml file.
	Specific items that can be formatted:
	many options for the icon
	many options for the Webmaster Suggests and Alternate Suggestion text
	many options for the Webmaster Suggests box
ADDED: Timestamps in connection message.
CHANGED: option for background color of Webmaster Suggests BOX is now
	bgcolor (was color)
	This was done to maintain consistency with HTML <table> parameters
	which in turn makes it easier to document and remember.
	All FONT items are JUST "color", to be consistent with HTML <font>
Cleaned up directory that we distributed files in.
Internal enhancements to searchnames.bat


Known Bugs in this Version
--------------------------
Cursors issue with Oracle when editing records.
Edit form text area controls have a single space in it when empty.
Some ancially socket messages in log files, still ivestigating, does not
	seem to prevent application from running.
Need to retool Google sample configuration to new modular format
Need to document modular configuration in this file
SearchTrack logging does not correctly record errors
	If there is a problem, an entry is made in the run log but
	not the database
the user supplied JVM environment variable is ignored and overwritten
	in the Unix searchnames.sh script
the database reconnect logic will not work if the database is down
	when searchnames is first started
needs doc for toubleshooting various database connections
Admin password sent as cleartext
Limited doc for the many options; see xml files.


Introduction
------------
This should setup a small, working test SearchNames server under
Windows 2000 or Windows XP, and on various Unix platforms.

It is configured by editing an XML file, and then passsing that file
name to the searchnames.bat batch file.

Note:
You do NOT need to set the Java class path.  This should be done
automatically by the batch file.

It will run on it's own port (settable in the XML config).
It is run from the COMMAND LINE interface, from the main directory
that you unzip to.

YOU WILL NEED TO MODIFY AT LEAST TWO FILES TO MAKE THIS RUN CORRECTLY.

And you will need Java, see below.

For Admin functions, please set an administration password.


Included Files
---------------

Main files:

searchnames.bat	The main Batch File for STARTING SearchNames
		Use Control-C to stop it, or Admin shutdown command.
searchnames.sh	The Unix equivalent to searchnames.bat
hello_db.bat	A utility to test connectivity to your database.
hello_db.sh	The Unix equivalent to hello_db.bat
register_service.bat
		A utility to have Search Names run as a full
		Windows Service,
		Note: there is no register_service.sh, because these services
		are specific to Windows.
doc/*.doc,*.html Draft of syuntax reference manual
samples		A directory of sample XML config files and HTML search forms.
		You would normally copy one of the XML files to the main
		directory and modify it.
config		The suggested location for YOUR site's XML configuration files.
logs		A suggested place for your log files.


Other supporting files:

README.txt	This file
hello_db.bat	A batch file to help test connectivity with your database.
		Useful when logging User searches to a database.
hello_db.sh	The Unix equivalent to hello_db.bat
system		The main system directory.
		You usually don't need to go in here.
system\jar_files	Our compiled code and some 3rd party libraries.
			You do NOT need to worry about these; they will
			be added to your path automatically.
			Some files may have .zip extensions instead of .jar.
			Please do not move or modify these files in any way.


Setting Up Java
----------------

Note:
You do NOT need to set the Java class path.  This should be done
automatically by the batch file.

You can probably use an existing Java Virtual Machine on
your system.  If you had a really old version it might not work.

Look for java.exe on your machine.

Check the version:
	java -version
If it's 1.3x or above you're probably OK.

If it's on your machine, ADD IT TO THE SEARCH PATH.

If you don't have Java, get it from Sun:
http://java.sun.com/j2se/1.4/download.html
Notes:
* "Java 2" is for Java v1.2, 1.3 and 1.4, etc.
* You want "J2SE" - Java 2 Standard Edition (vs. EE or ME)
* You would then look for "JRE" (vs JDK)
	You just need the JVM which is part of JRE (Java Runtime Environment)
	JDK is for developers
* They are currently pushing v1.4.  That should be fine.


Configuration
-------------

Java Note:
You do NOT need to configure the Java class path.  This should be done
automatically by the batch and script files.

Overview of configuration:
* Create your site's configuration file in the config directory.
  The samples directory shows some template XML files which may be helpful.
* Create a web form that will call the running process.
* Have your system startup Search Names when it starts.

You should read and MODIFY a searchnames XML config file.
There are some samples in the samples directory.
Put yours in the config directory.

Note:
If you run multiple servers, make sure to SET THE PORTS so that they do
not collide.  For example, run one on port 9000 and another on 9001.

SUGGESTION:
It is HELPFUL to include the PORT NUMBER when you NAME your config file.
For example, you might put site1 on port 9001 and site2 on 9002;
in that case you might consider naming the respective files
site1-config-9001.xml and site2-config-9002.xml


Some highlights of the XML config file:

You should set an ADMINISTRATION PASSWORD in the search_tuning tag.

Example showing both options in the search_tuning tag:

	<search_tuning password="pizza">

* Look for <nie_server_url> and CHANGE IT.
It should be YOUR-MACHINE-NAME:PORT

This is also how the Search Names application knows which port to run on.
The default for http is port 80.

* Look for <search_engine_info>
CHANGE THIS to the EXISTING search CGI you'd like to proxy between.

* If you want to log User searches to a database, you'll need to
  also configure a search_track section, complete with information
  about the database you would like to connect to.

  See the samples with "search logging" in their names.

* Optionally you can set runtime process status logging in the
  configuration file.

* You MIGHT want to change some of these:
	<default_webmaster_suggests_header>
	<default_webmaster_suggests_box>
	<default_webmaster_suggests_icon>
	^^^ This is where the search tower comes from!
	So if you don't like it, you can change it
	OR it is SAFE to REMOVE THIS or comment it out.
	<default_alternate_suggestions_heading>

* The actual maappings, see <fixed_redirection_map>
Each <map>...</map> entry associates a group of words and phrases
with a group of URLs.  I'm sorry there's not better doc.


Then create a web form.  There are a few in the samples directory.

You should not use "localhost" as this will only work on your machine
or when you are actually logged in to the machine running SearchNames.

Look at the <form> tag:
<form action="http://localhost:9000/" method="get">
This points to the SEARCH NAMES process.
Change the localhost to your machine name, and port if you changed
it in the config.


Checking Config File Changes:

You can check your config file syntax without actually starting the
persistent server process.  This is handy if you have made changes
to your config file and would like to double check it before stopping
the already-running server.

On Windows:
	searchnames.bat -check_config my_config_file.xml

On Unix:
	./searchnames.sh -check_config my_config_file.xml

You can substitute -c for -check_config to save a little typing.
Also, it can go before or after the config file name.

So these commands would also be valid:
	searchnames.bat -c my_config_file.xml
	searchnames.bat my_config_file.xml -check_config
	searchnames.bat my_config_file.xml -c

On Unix searchnames.bat would be replaced by searchnames.sh


When you make configuration changes you can isseue a refresh command
to the server.  Please see the Administration Commands section.


If you will be logging user searches, or using the web based interface,
Search Names will need to talk to your database.  Information about your
database is required, such as what machine it is running on, what is the
name of the actual database, and login information.

This information is usually configured under the search_tuning section of
the configuration file.  However, it is possible to test connectivity
with the hello_db utility.

The samples directory contains a template hello_db.xml file.  This file
is UNLIKELY to work as-is, given the differences in each site's database
configuration.  The file should be copied to the config directory
and then modified.

Assuming the file had been copied to config and then modified, the
command to run would be:

On Windows:

	hello_db.bat config\hello_db.xml

On Unix:

	./hello_db.sh config/hello_db.xml


It will print out a message if it encounters errors.

Once this is working, use the configuration settings from that
XML file in your larger configuration file.


To run as a Windows Server, aka: NT Service:

You can run ONE instance of SearchNames as a service under
Windows NT, Windows 2000 and Windows XP.

First, get ALL OF YOUR CONFIGURATION WORKING first with the regular tools.

Second, make sure your XML file has a LOGGING ENTRY.
If you do not make a logging entry, there will be no way to monitor
SearchNames.  And the log file should have an ABSOLUTE PATH.

Example:
<nie_config>

	<run_logging
		location="D:\programs\SearchNames-098d\logs\site-9001.log"
	/>

	... rest of cofiguration file ...

</nie_config>

Then run register_service.bat.  It will give you the syntax to use.

You can also test your configuration, before installing, by using
the -console option.

WARNING: Do NOT try to register more than ONE Search Names server
as a service on a server.  If you need to do this, contact New Idea
Engineering.

(Note: this has to do with the 2nd MONITOR_PORT which defaults to 1777)


Running
-------
Note:
If you have registered your process as a Windows Service, use the
Control Panel / Services Manager to start and stop it.
This is only applicable if you have used register_service.bat
Otherwise, continue reading this section.

Start from main directory you unzipped or untarred.

WARNING for Windows Users:
If you are running on Windows in cmd.exe windows, be careful NOT to
select text with your mouse.  The system will pause and not process requests
while text is selected.
This problem can happen, for example, if you have ENABLED "Quick Edit"
in the properties (DISABLED by default) and accidently click inside the
text area of the window.

Fixes:
* Right click to unselect the text; the process should resume.
* Click the title bar or task bar to bring the window to the foreground
* Disable Quick Edit in the properties of that command window

SUGGESTION for Windows Users:
It is helpful to rename the command window that you will start
Search Names in to reflect the site and port it is servicing,
especially if you are running more than one.

In the above examples running on port 9001, you might enter the command:

	title my_website_name:9001

in the command window, prior to starting searchnames.

SUGGESTION:
You may want to change the properties of your command or terminal window
so that it has a scroll bar and maintains a history of the output.


Windows Startup Syntax:

The syntax for SearchNames is simply:
	searchnames.bat config\your_config_file.xml

You can leave off the .bat extension.


Unix Startup Syntax:

The syntax for SearchNames is simply:
	./searchnames.sh config/your_config_file.xml

You can replace the "./" to wherever you have installed the SearchNames
package.  Or you can put that directory in your path.


By default SearchNames looks for searchnames_config.xml in the main
directory.  This is typically NOT where your configuration files will be;
they will usually be in ./config


Additional command line options:

On both Windows and Unix, you can add additional command line options
to the command you issue to start Search Names.

These options can go before or after the name of your configuration file.

Option		Shorthand	Comment
------		---------	-------

Major Options:

-check_config	-c		Check config but don't actually start up,
				see Configuration section

-version	-v		Display version and configuration data and
				then exit.  If a configuration file is also
				given, will summarize the configuration as well

Run Log Output Verbosity Options:

Options to control the Runtime message logging or "Verbosity",
such as status and error messages:

Each level in the list gives more and more output.
The default verbosity is -transaction, which will give an overall picture
of the prorgrams operation, and also briefly announce each transaction.

Option		Shorthand	Comment
------		---------	-------

		(least output here with -fatal_only)

-fatal_only	(none)		Suppress ALL messages except FATAL Errors -
				use at your own risk!
-quiet		-q		Suppress most messages except warnings and
				errors.
-status		-s		Overall progress of program; major functions
				starting and stopping, etc.

-transaction	(none)		This is the DEFAULT OUTPUT LEVEL
  or				More detailed status progress of program
-transactions			including individual requests and transactions.

-info		-i		More detailed descriptions of the actions taken

-debug		-d		Includes internal logic of program.

-trace		(none)		Much more verbose debug mode;
  or				may create HUGE log files
-debug_trace			and slow down the program.

		(most output here with -trace)

WARNING about -debug and -trace:

These options turn on a LOT of output.  They should be used only briefly.

If you log process output to a log file, it may cause it to grow
very quickly.

Also, formatting and outputting the MANY debug messages can actually
slow the server down.



Stopping the Server:

There are several ways to stop the SearchNames server:
* The best way to stop the server is to issue an administration command
  via a Web Browser.  See the Administration Commands section for details.
* If started in a command or terminal window, you can use Control-C.
* You can also find the running process and "kill" it using your either
  the Windows Task Manager or Unix "ps" and "kill" commands.
  Look for a Java process.


Testing
--------
You can verifiy that the server is running by using some of the
Admin commands "ping" and "showall".  See the Administration Commands
section below for details.

Of course you can also test by bringing up your form and submitting
sone searches.

You'll often want to test by setting up a new HTML form that submits
searches to the SerachNames server.
It may be quicker to make a copy of your existing search form and
modify the form tag.

See the samples directory for some ideas.

Once you've got it running and MODIFIED the search form,
try running a search.

When you make configuration changes, which includes changes to seach
terms and URLs, you can isseue a refresh command
to the server.  Please see the Administration Commands section.


Administration Commands
-----------------------

"Pinging" the SearchNames Server:

This is the simplest Admin command you can issue.
It simply verifies that the server is up and is responding
to HTTP requests.

Syntax:

	http://your_machine:port/?sn_context=admin
		or
	http://your_machine:port/?sn_context=admin&command=ping

Since ping is the DEFAULT admin command, you don't need to specifically
add it to the URL, although doing so makes your URLs clearer.

No password is required for this command.


Getting Version and Configuration Details:

Displays version of Search Names and the NIE core library.
Also shows which major options and configurations are active in Search Names.
Also shows NIE copyright and web site URL.

Syntax:

	http://your_machine:port/?sn_context=admin&command=version

No password is required for this command.


Listing all Terms and URLs:

You can view your ENTIRE LISTING of mapped terms by calling up
the admin screen from the running Search Names process:

Syntax:

	http://nie_server_machine:port/?nie_context=admin&command=showall

This currently does NOT show all of your settings, but it at least gives
an overview of terms and URLs.  Suggestions welcome.

In a pinch, this report is technically user modifiable by changing the XSLT
code that generates it.  The jar file would need to be extracted, the class
path modified, and then the file located and modified.  This is a rather
involved process and may be too complex for casuaul administrators.

This is the only other Admin command that does not require a password.


Reloading the Config File, Terms and URLs:

You can now edit your file on disk and reload it, without the need
to restart your server.  This is handy if you make frequent changes
to your terms and URLs.

The syntax is:

	http://nie_server_machine:port/?nie_context=admin&command=refresh
	&password=your-password

	^^^^ THIS IS A SINGLE URL ^^^^
	It should be entered into your browser as a single command.

You should probably mnake changes to a config file with a new name
and use the -check_config option on that.  Later, when you know it's
correct, then copy it over the existing config file and a server refresh
command.

Some Notes:

* Please do not confuse the SearchNames "refresh" command with your
  Web browser's "Refresh" button.  The similar name is only a
  coincidence; pressing the refresh button in your browser will
  NOT refresh the SearchNames search terms and URL mappings.
  Please use the URL above instead.

* There are several configuration options which can NOT be changed
  with this dynamic refresh:
	- The port that SearchNames runs on can not be changed
	- The location of the configuration file can not be changed;
	  it will try to reread the same file or URL it read when it started.

* Since you can not change the name of the file or URL SearchNames
  reads from, you must put any changes into a file with the same name,
  or overrite the file with a new copy, again with the same name.

* If an error occurs during initializatio of the new configuration
  the server should not crash.  Instead, it will keep the old configuration
  information it has in memory from the last successful initialization.

* If you do get an error, make sure to CORRECT IT.  Even though the current
  instance of the server should remain running, it is likely you will not
  be able to restart your SearchNames service the next time you need to.
  So even though a bad file won't crash the server now, it probably will
  prevent it from running in the future.

* Even though the feature of keeping the old config data and continuing
  to run in the event of a problem with the new config is a nice safeguard,
  we still strongly advise that administrators continue the habbit of using
  -check_config before issuing a refresh.

You may want to use the "show all" command to look at the updated
map that the SearchNames server has loaded.


Stopping the Server:

In order to STOP the server, you will need to have setup a password
in the search_tuning tag in your config file.  See the Confiuguration section
for details.

	http://nie_server_machine:port/?nie_context=admin&command=shutdown
	&password=your-password

	^^^^ THIS IS A SINGLE URL ^^^^
	It should be entered into your browser as a single command.

	If your password has spaces, replace each space with a plus sign (+)
	If your password has an punctuation, you should "escape it".

You maight want to save this URL in an HTML page for easy access.
Or you could create a web form to issue various administration commands.

See the samples directory for ideas.


Modifying
----------
If you modify the Search Form, you can just Reload in your browser.

If you modify the Config file, you must RESTART the SearchNames process.

This will be addressed in a future version.


Troubleshooting
---------------

The error messages, though sometimes verbose, may provide valuable
information.

You can run the process with -debug, although this produces a LOT of
output, see the Command Line Options area of the Running section above.

On Windows, make sure your console window does not have any text selected.


Is your firewall causing a problem?

If you are testing on an internal network before deploying to your
public website, or are behind a corporate firewall for some other reason,
your test machime may not be able to see the host search engine machine.

Ironically, some corporations' firewalls prevent their internal machines
from accessing their own public machines, even though other computers on the
Internet are able to access them.

Advanced users:

Telnet can be used to test access to your host search engine.
If these instructions are confusing, you may wish to speak to your
system's administrator.

The *general* procedure is:

Login to the TEST machine.

telnet machine_name port_number

Where port_number is often port 80.

If you get a telnet prompt you can issue a request.

A simple request would be:

	GET / HTTP/1.0  (return)
	(2nd return)

Where (return) represents your keyboard's return or enter key.
There are NO spaces at the end of the line before the return key;
they are shown here simply for clarity.

If you see "reasonable" looking output then the firewall would seem
to not be a factor.

A fancier request can include addtitional HTTP header fields and
CGI varaiables.  Some web servers require additional fields to
function properly.

	telnet your_host 80

	GET /your_seach_cgi?query=test HTTP/1.0 (return)
	Accept: */* (return)
	Host: your_host (return)
	User-Agent: Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0; NetCaptor 6.5.0) (return)
	Connection: Close (return)
	(a final return)

