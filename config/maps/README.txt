This directory has the individual keyword mappings to various URLs and
advertisements.

Each map is in it's own file.


Note about adding map files
---------------------------
If you add a new map file, you MUST also make a REFERENCE to the new
file in the main keyword_mappings.xml file in the main directory.

The new files will NOT show up automatically.  If you add a map, and it
doesn't seem to work, double-check that you added a reference.
(this feature may be added in a future version)


Note about links to advertisements
----------------------------------
Notice that the references to advertisements have "../ads/" prefix.
This is because those files are relative to these files, so must go up
one level then down to the ads directory.

If this is confusing you could put the ads directory under this maps directory
ionstead.
