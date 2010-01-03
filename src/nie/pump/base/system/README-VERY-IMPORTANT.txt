IMPORTANT NOTE TO DEVELOPERS:

The system directory contains non-java resources that are are
part of DPump/XPump, such as XML files, XSLT, etc.

The system dir must be relative to the main class (usually XPump)
But it's relative to the BINARY classes directory.

So, conceptually, system files go hear in src.nie.pump.base.system
but they are ACTUALLY in classes.nie.base.system

Make sure you remember them and treat them as source,
but leave them in the classes tree for the jar utility's sake.

Mark
