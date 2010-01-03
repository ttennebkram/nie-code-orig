This directory contains the raw XML data for advertisements.

These ads are references by map files in the maps directory.

Important Note
--------------
If you just add a new file here, and do nothing else, it will NOT show up.
You must associate each advertisement with keywords in one of the map files.


Re-Using Ad Files
-----------------

It is OK to include one of these files more than once, in more than one
map file.

This might be useful if you have a customer that has bought more than one
keyword (that is in more than one map), but the advertisement is the same
for both.

In that case, it is perfectly OK to reuse the same file here over and over.
Just repeat the same <include> tag in each of the related map files.

Formatting Ads
--------------
The formatting of these ads is NOT controlled in these XML files.
Instead, it is controlled by XSLT files elsewhere.
