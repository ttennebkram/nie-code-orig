package nie.filters.test;

import java.util.*;

public class CommandLineParser
{
	Vector positional;
	Hashtable keyword;
	String[] args;

	public CommandLineParser(Vector positional, Hashtable keyword,
		String[] args
		)
	{
		this.positional = positional;
		this.keyword = keyword;
		this.args = args;
	}

	public void printUsage()
	{
		this.printUsage("not implemented");
	}

	public void printUsage(String blurb)
	{
		if (blurb.length() > 0)
		{
			System.out.println(blurb);
		}
		System.out.println("================ Usage ================\n");
		if (this.positional.size() > 0) {
			System.out.println("Required positional parameters:");

			Iterator iter = this.positional.iterator();
			while (iter.hasNext()) {
				String stuff[] = (String [])iter.next();
				System.out.println("\t" + stuff[0] + "\t:\t" + stuff[1]);
			}
			System.out.println("");
		}

		if (this.keyword.size() > 0) {
			System.out.println("Optional keyword parameters:");
			Enumeration keys = this.keyword.keys();
			while (keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				Object[] stuff = (Object [])this.keyword.get(key);
				System.out.println("\t" + key + "\t:\targs: " + (Integer)stuff[0] + "\t:\t" + (String)stuff[1]);
			}
		}
	}

	public Hashtable parse()
		throws Exception
	{
		Hashtable ret = new Hashtable();
		int pos = 0;

		// Make sure there are enough arguments
		if (this.args.length < this.positional.size()) {
			throw new Exception("Not enough arguments.   Expected " + this.positional.size() + ", got " + this.args.length);
		}

		// First get all the positional arguments
		Iterator iter = this.positional.iterator();
		for (pos=0; iter.hasNext(); pos++) {
			// Get the key and the value
			String stuff[] = (String [])iter.next();
			String key = stuff[0];
			String value = this.args[pos];

			// Make sure it's not a keyword argument
			if (value.charAt(0) == '-') {
				// Uh-oh
				throw new Exception("Not enough positional parameters.   Missing " + key);
			}

			// Add it
			ret.put(key, value);
		}

		// Now get any keyword arguments that might have been passed
		for ( ; pos < this.args.length; pos++)
		{
			String key = this.args[pos];
			if (key.charAt(0) != '-')
			{
				throw new Exception("Unexpected positional paramenter: " + key);
			}
			key = key.substring(1);
			// See if we know about this argument
			if (!this.keyword.containsKey(key))
			{
				throw new Exception("Unknown keyword argument: " + key);
			}
			Object[] attrs = (Object[])this.keyword.get(key);
			// See how many arguments it has
			int numArgs = ((Integer)attrs[0]).intValue();
			if (numArgs == 0)
			{
				// Done!
				ret.put(key, new Integer(1));
			}
			else if (numArgs == 1)
			{
				if (pos + 1 >= this.args.length)
				{
					throw new Exception("Need a value for keyword argument: " + key);
				}
				String parg = this.args[++pos];
				ret.put(key, parg);
			}
			else
			{
				Vector pargs = new Vector();
				pos++;
				for (int x=0; x<numArgs; x++, pos++) {
					if (pos >= this.args.length) {
						throw new Exception("Not enough values for keyword argument: " + key);
					}
					String parg = this.args[pos];
					pargs.addElement(parg);
				}
				ret.put(key, pargs);
			}
		}

		return ret;
	}

}
