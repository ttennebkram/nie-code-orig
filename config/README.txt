Search Names new "modular" configuration files, using the new <include> tag

Intro
-----
Search Names is configured with XML files, but they can get very large
and hard to edit.

To address this, configuration data can now be broken apart into separate, more
managable files.  A relatively small main configuration file can now "include"
other XML files.  Those files can, in turn, include additional files.

It is still OK to have everything in one big file; breaking the config
files up is OPTIONAL.


The <include> Tag
-----------------
Search Names now understands the <include> tag, which is how you reference
other files.

Example:

	<include location="other-file.xml" />

The location is a path name to the additional file that is to
be included.  The location is relative to the file that <include> tag is in.

The included file must be a VALID XML file by itself - you can not iunclude
fragments.

This means the included file must:
* Also conform to XML syntax
* Have all of its own matching tags
  If you have <foo> then you need </foo>, or you can have <foo/>
* Have a SINGLE top level XML tag

See the samples in this directory.


Files In This Sample Configuration
----------------------------------

XML Files
---------
main.xml			Top Level config file, includes the other files
search_engine.xml		Info about host search engine, ex: Verity
keyword_mappings.xml		Links to actual keywords and assoc urls and Ads
markup_declarations.xml		Declaration of text_ads, look and placement

Directories
-----------
maps/				Each keyword mapping is in a separate file
defaults/			Additional cosmetic settings colors, fonts, etc
ads/				Individual advertisements
style_sheets/			How to format each type of Ad

Misc Files
----------
README.txt			This File
