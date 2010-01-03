package nie.di;
import java.io.*;

class DiWriteSeed
{

  public static void Header_Rec(PrintStream outPS)
  {
    // Write HTML header info
    outPS.println( "<html><br>\n<head><br>\n" );
    outPS.println("<META NAME=\"ROBOTS\" CONTENT=\"NOINDEX,FOLLOW\"><br>\n");
    outPS.println("</head><br>\n<body>");
    outPS.println( "<!--" + DataIndexer.APPNAME + "&nbps;" +
                            DataIndexer.APPVERSION + "-->" );
  }

 public static void Data_Rec( PrintStream outPS, String rootURL, String keyVal)
 {
   // Write the actual link page
   String buffer = "";
   buffer = rootURL + keyVal;
   outPS.println("<a href=" + buffer + ">" );
   outPS.println( buffer + "</a><br>\n" );
  }

  public static void Eod_Rec(PrintStream outPS)
  {
    outPS.println("<<EOD>>");
  }

 public static void Trailer_Rec(PrintStream outPS)
 {
   // Write the close of HTML file
  outPS.println("<!-- last line of seed file" + DataIndexer.APPVERSION + "-->" );
  outPS.println("</body></html>");
  }

}


