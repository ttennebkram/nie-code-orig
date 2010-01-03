package nie.filters.test;

import nie.filters.*;
import nie.filters.io.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.jdom.*;
import org.jdom.output.*;

// A class to exercise the readers
class Test
{
	public static void main(String args[])
	{

		// Get ready to parse the command line
		Vector positional = new Vector();
		String a[] = new String[2];
		a[0] = "input"; a[1] = "The file to parse";
		positional.addElement(a.clone());
		a[0] = "output"; a[1] = "The output file";
		positional.addElement(a.clone());

		Object o[] = new Object[2];
		Hashtable keyword = new Hashtable();
		o[0] = new Integer(1); o[1] = "PDF: The maximum number of pages to parse";
		keyword.put("maxpages", o.clone());
		o[0] = new Integer(0); o[1] = "PDF: Include stream contents";
		keyword.put("stream", o.clone());
		o[0] = new Integer(1); o[1] = "PDF: Layout format to use: 1=Absolute 2=Friendly";
		keyword.put("layout", o.clone());
		o[0] = new Integer(0); o[1] = "PDF: Don't include path information";
		keyword.put("nopath", o.clone());
		o[0] = new Integer(1); o[1] = "PDF: Use mapping file for font information";
		keyword.put("mapping", o.clone());
		o[0] = new Integer(0); o[1] = "HTML: Exclude style elements";
		keyword.put("nostyle", o.clone());
		o[0] = new Integer(0); o[1] = "HTML: Exclude script elements";
		keyword.put("noscript", o.clone());
		o[0] = new Integer(0); o[1] = "HTML: Set Tidy to clean source";
		keyword.put("clean", o.clone());
		o[0] = new Integer(0); o[1] = "HTML: Don't display warnings";
		keyword.put("nowarn", o.clone());
		o[0] = new Integer(0); o[1] = "PDF: Include character code values in output";
		keyword.put("inccharcodes", o.clone());

		// Parse the command line into a hash table
		Hashtable values;
		CommandLineParser cmdLine = new CommandLineParser(positional, keyword, args);
		try
		{
			values = cmdLine.parse();
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
			cmdLine.printUsage();
			return;
		}

		// Get and open the file name or URL, which we call the URI
		String fileName = (String)values.get("input");
		SeekableStream rd;
		// Open either a file or a URL
		try
		{
			// Is it a URL?
			if (fileName.startsWith("http://"))
			{
				rd = new URLSeekableStream(new URL(fileName));
			}
			// Else assume it's a file
			else
			{
				rd = new FileSeekableStream(fileName);
			}
		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
			return;
		}

		// The in-memory repository for the tree we will build
		Element doc = new Element("document");
		Document pdfDoc = new Document(doc);

		// Read the input file
		try
		{
			// If it's PDF
			if (fileName.endsWith(".pdf"))
			{
				PDFParser p = new PDFParser(rd, doc);

				// Set whatever options were passed on the command line
				if (values.size() > 0)
				{
					p.setSettings(values);
				}

				p.getContents();
			}
			// Else assume it's HTML
			else
			{
				HTMLParser p = new HTMLParser(rd, doc);

				// Set whatever options were passed on the command line
				if (values.size() > 0)
				{
					p.setSettings(values);
				}

				p.getContents();
			}
		}
		catch (Exception e)
		{
			// Catch everything... and just print out whatever we have so far
			e.printStackTrace();
		}

		// Write to a file
		try
		{
			String outputFile = (String)values.get("output");
			FileWriter writer = new FileWriter(outputFile);
			XMLOutputter outputter = new XMLOutputter("  ", true);
			outputter.output(pdfDoc, writer);
		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
		}

		// Free the file
		try
		{
			rd.close();
		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
		}
	}
}
