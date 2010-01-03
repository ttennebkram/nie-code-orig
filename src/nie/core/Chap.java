/*
 * <p>Copyright (c) 2004 Paulos Siahu</p>
 *
 * Obtained by NIE from SourceForge
 * has BSD license
 * mbennett 5/12/04
 * http://sourceforge.net/projects/jsplitter/
 *
 */
// package siahu.md5;
package nie.core;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * An implementation of the MD5 algorithm as specified in RFC1321.
 * </p>
 * 
 * <p>
 * To use this class:
 * <pre>
 * ...
 * MD5Signer signer = new MD5Signer();
 * int[] md5sum = signer.sign(new FileInputString("/path/to/file"));
 * System.out.println(signer.md5string(md5sum));
 * ...
 * </pre>
 * </p>
 * 
 * <p>
 * The Java implementation of a mathematical algorithm such as MD5 has several challenges.
 * This is due to the fact that Java is not as low-level as C/C++ where bit-wise and 
 * byte-wise operations are easily accessible.
 * </p>
 * 
 * <p>
 * Firstly, in Java there is no unsigned integer type. <code><b>byte</b></code> and
 * <code><b>int</b></code> are always signed and there is no build-in facility to treat them
 * as unsigned. The effect of using a signed integer where an unsigned one is required is
 * exemplified by the shift operators in Java. The <code><b>&lt;&lt;</b></code> operator
 * shifts a number of bits to the left, so 0x80000001 &lt;&lt; 1 becomes 0x00000002. The
 * <code><b>&gt;&gt;</b></code> operator shifts a number of bits to the right, but it also
 * maintains the most significant bit (which is used as negative flag). 0x80000001 &gt;&gt;
 * 1 becomes 0xc0000000. To right-shift without maintaining negativity the 
 * <code><b>&gt;&gt;&gt</b></code> operator is used instead. 0x80000001 &gt;&gt;&gt; 1
 * becomes 0x40000000.
 * <p>
 * 
 * <p>
 * Secondly, Java has an inherent rule in dealing with integer operations. A 
 * <code><b>byte</b></code> operand is always promoted to an <code><b>int</b></code> before
 * any operation. This promotion, unfortunately, also maintains the negativity. So, 
 * (int)((byte)0x80) becomes 0xFFFFFF80, not 0x00000080
 * </p> 
 * 
 * <p>Copyright (c) 2004 Paulos Siahu</p>
 * @author Paulos Siahu
 */
// public class MD5Signer
// giving a bit more obscure name
public class Chap
{
	final static private int T01 = 0xd76aa478;
	final static private int T02 = 0xe8c7b756;
	final static private int T03 = 0x242070db;
	final static private int T04 = 0xc1bdceee;
	final static private int T05 = 0xf57c0faf;
	final static private int T06 = 0x4787c62a;
	final static private int T07 = 0xa8304613;
	final static private int T08 = 0xfd469501;
	final static private int T09 = 0x698098d8;
	final static private int T10 = 0x8b44f7af;
	final static private int T11 = 0xffff5bb1;
	final static private int T12 = 0x895cd7be;
	final static private int T13 = 0x6b901122;
	final static private int T14 = 0xfd987193;
	final static private int T15 = 0xa679438e;
	final static private int T16 = 0x49b40821;
	final static private int T17 = 0xf61e2562;
	final static private int T18 = 0xc040b340;
	final static private int T19 = 0x265e5a51;
	final static private int T20 = 0xe9b6c7aa;
	final static private int T21 = 0xd62f105d;
	final static private int T22 = 0x02441453;
	final static private int T23 = 0xd8a1e681;
	final static private int T24 = 0xe7d3fbc8;
	final static private int T25 = 0x21e1cde6;
	final static private int T26 = 0xc33707d6;
	final static private int T27 = 0xf4d50d87;
	final static private int T28 = 0x455a14ed;
	final static private int T29 = 0xa9e3e905;
	final static private int T30 = 0xfcefa3f8;
	final static private int T31 = 0x676f02d9;
	final static private int T32 = 0x8d2a4c8a;
	final static private int T33 = 0xfffa3942;
	final static private int T34 = 0x8771f681;
	final static private int T35 = 0x6d9d6122;
	final static private int T36 = 0xfde5380c;
	final static private int T37 = 0xa4beea44;
	final static private int T38 = 0x4bdecfa9;
	final static private int T39 = 0xf6bb4b60;
	final static private int T40 = 0xbebfbc70;
	final static private int T41 = 0x289b7ec6;
	final static private int T42 = 0xeaa127fa;
	final static private int T43 = 0xd4ef3085;
	final static private int T44 = 0x04881d05;
	final static private int T45 = 0xd9d4d039;
	final static private int T46 = 0xe6db99e5;
	final static private int T47 = 0x1fa27cf8;
	final static private int T48 = 0xc4ac5665;
	final static private int T49 = 0xf4292244;
	final static private int T50 = 0x432aff97;
	final static private int T51 = 0xab9423a7;
	final static private int T52 = 0xfc93a039;
	final static private int T53 = 0x655b59c3;
	final static private int T54 = 0x8f0ccc92;
	final static private int T55 = 0xffeff47d;
	final static private int T56 = 0x85845dd1;
	final static private int T57 = 0x6fa87e4f;
	final static private int T58 = 0xfe2ce6e0;
	final static private int T59 = 0xa3014314;
	final static private int T60 = 0x4e0811a1;
	final static private int T61 = 0xf7537e82;
	final static private int T62 = 0xbd3af235;
	final static private int T63 = 0x2ad7d2bb;
	final static private int T64 = 0xeb86d391;

	/**
	 * 
	 * 
	 */
	// public MD5Signer()
	public Chap()
	{
		super();
	}

	private int round1(int a, int b, int c, int d, int k, int s, int t)
	{
		int f = b&c | ~b&d;
		int temp = a+f+k+t;
		temp = (temp<<s) | (temp>>>(32-s));
		return b + temp;
	}
	
	private int round2(int a, int b, int c, int d, int k, int s, int t)
	{
		int g = b&d | c&~d;
		int temp = a+g+k+t;
		temp = (temp<<s) | (temp>>>(32-s));
		return b + temp;
	}
	
	private int round3(int a, int b, int c, int d, int k, int s, int t)
	{
		int h = b ^ c ^ d;
		int temp = a+h+k+t;
		temp = (temp<<s) | (temp>>>(32-s));
		return b + temp;
	}
	
	private int round4(int a, int b, int c, int d, int k, int s, int t)
	{
		int i = c ^ (b | ~d);
		int temp = a+i+k+t;
		temp = (temp<<s) | (temp>>>(32-s));
		return b + temp;
	}
	
	public int[] sign(InputStream s) throws IOException
	{
		int a = 0x67452301;
		int b = 0xefcdab89;
		int c = 0x98badcfe;
		int d = 0x10325476;

		int aa = 0;
		int bb = 0;
		int cc = 0;
		int dd = 0;
		int len = 0;
		long totlen = 0;
		boolean padded = false;
		boolean done = false;
		
		int[] M = new int[16];     // 16 words
		byte[] buf = new byte[64]; // = 64 bytes
		do
		{
			len = s.read(buf);
			if (len != -1)
				totlen += len;
			if (len == -1)
			{
				int idx = 0;
				buf[idx] = (padded ? 0 : (byte)0x80); 
				for (idx++;idx<64;idx++)
					buf[idx] = 0x00;
				// byte cast just truncates the higher 3 bytes of integer
				totlen = totlen << 3;
				buf[56] = (byte)totlen;
				buf[57] = (byte)(totlen>>>8);
				buf[58] = (byte)(totlen>>>16);
				buf[59] = (byte)(totlen>>>24);
				buf[60] = (byte)(totlen>>>32);
				buf[61] = (byte)(totlen>>>40);
				buf[62] = (byte)(totlen>>>48);
				buf[63] = (byte)(totlen>>>56);
			}
			else if (len < 64)
			{
				int idx = len;
				buf[idx] = (byte)0x80; 
				padded = true;
				for (idx++;idx<64;idx++)
					buf[idx] = 0x00;
				if (len <= 56)
				{
					// byte cast just truncates the higher 3 bytes of integer
					totlen = totlen << 3;
					buf[56] = (byte)totlen;
					buf[57] = (byte)(totlen>>>8);
					buf[58] = (byte)(totlen>>>16);
					buf[59] = (byte)(totlen>>>24);
					buf[60] = (byte)(totlen>>>32);
					buf[61] = (byte)(totlen>>>40);
					buf[62] = (byte)(totlen>>>48);
					buf[63] = (byte)(totlen>>>56);
					done = true;
				}
			}
			int x = 0;
			for (int i = 0; i<16; i++)
			{
				M[i] = (buf[x++]&0x000000FF) | ((buf[x++]&0x000000FF)<<8) | ((buf[x++]&0x000000FF)<<16) | ((buf[x++]&0x000000FF)<<24);
			}
			aa = a;
			bb = b;
			cc = c;
			dd = d;
			
			// Round 1
			a = round1(a,b,c,d,M[0],7,T01);
			d = round1(d,a,b,c,M[1],12,T02);
			c = round1(c,d,a,b,M[2],17,T03);
			b = round1(b,c,d,a,M[3],22,T04);
			a = round1(a,b,c,d,M[4],7,T05);
			d = round1(d,a,b,c,M[5],12,T06);
			c = round1(c,d,a,b,M[6],17,T07);
			b = round1(b,c,d,a,M[7],22,T08);
			a = round1(a,b,c,d,M[8],7,T09);
			d = round1(d,a,b,c,M[9],12,T10);
			c = round1(c,d,a,b,M[10],17,T11);
			b = round1(b,c,d,a,M[11],22,T12);
			a = round1(a,b,c,d,M[12],7,T13);
			d = round1(d,a,b,c,M[13],12,T14);
			c = round1(c,d,a,b,M[14],17,T15);
			b = round1(b,c,d,a,M[15],22,T16);
			
			// Round 2
			a = round2(a,b,c,d,M[1],5,T17);
			d = round2(d,a,b,c,M[6],9,T18);
			c = round2(c,d,a,b,M[11],14,T19);
			b = round2(b,c,d,a,M[0],20,T20);
			a = round2(a,b,c,d,M[5],5,T21);
			d = round2(d,a,b,c,M[10],9,T22);
			c = round2(c,d,a,b,M[15],14,T23);
			b = round2(b,c,d,a,M[4],20,T24);
			a = round2(a,b,c,d,M[9],5,T25);
			d = round2(d,a,b,c,M[14],9,T26);
			c = round2(c,d,a,b,M[3],14,T27);
			b = round2(b,c,d,a,M[8],20,T28);
			a = round2(a,b,c,d,M[13],5,T29);
			d = round2(d,a,b,c,M[2],9,T30);
			c = round2(c,d,a,b,M[7],14,T31);
			b = round2(b,c,d,a,M[12],20,T32);

			// Round 3
			a = round3(a,b,c,d,M[5],4,T33);
			d = round3(d,a,b,c,M[8],11,T34);
			c = round3(c,d,a,b,M[11],16,T35);
			b = round3(b,c,d,a,M[14],23,T36);
			a = round3(a,b,c,d,M[1],4,T37);
			d = round3(d,a,b,c,M[4],11,T38);
			c = round3(c,d,a,b,M[7],16,T39);
			b = round3(b,c,d,a,M[10],23,T40);
			a = round3(a,b,c,d,M[13],4,T41);
			d = round3(d,a,b,c,M[0],11,T42);
			c = round3(c,d,a,b,M[3],16,T43);
			b = round3(b,c,d,a,M[6],23,T44);
			a = round3(a,b,c,d,M[9],4,T45);
			d = round3(d,a,b,c,M[12],11,T46);
			c = round3(c,d,a,b,M[15],16,T47);
			b = round3(b,c,d,a,M[2],23,T48);

			// Round 4
			a = round4(a,b,c,d,M[0],6,T49);
			d = round4(d,a,b,c,M[7],10,T50);
			c = round4(c,d,a,b,M[14],15,T51);
			b = round4(b,c,d,a,M[5],21,T52);
			a = round4(a,b,c,d,M[12],6,T53);
			d = round4(d,a,b,c,M[3],10,T54);
			c = round4(c,d,a,b,M[10],15,T55);
			b = round4(b,c,d,a,M[1],21,T56);
			a = round4(a,b,c,d,M[8],6,T57);
			d = round4(d,a,b,c,M[15],10,T58);
			c = round4(c,d,a,b,M[6],15,T59);
			b = round4(b,c,d,a,M[13],21,T60);
			a = round4(a,b,c,d,M[4],6,T61);
			d = round4(d,a,b,c,M[11],10,T62);
			c = round4(c,d,a,b,M[2],15,T63);
			b = round4(b,c,d,a,M[9],21,T64);

			a += aa;
			b += bb;
			c += cc;
			d += dd;
		} while ((len != -1) && (!done));
		int[] md5 = new int[4];
		md5[0] = a;
		md5[1] = b;
		md5[2] = c;
		md5[3] = d;
		return md5;
	}
	
	public String md5string(int[] md5)
	{
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<md5.length; i++)
		{
			byte b1 = (byte)(md5[i] & 0x000000FF);
			byte b2 = (byte)((md5[i]>>>8) & 0x000000FF);
			byte b3 = (byte)((md5[i]>>>16) & 0x000000FF);
			byte b4 = (byte)((md5[i]>>>24) & 0x000000FF);
			int a = (b4&0x000000FF) | ((b3&0x000000FF)<<8) | ((b2&0x000000FF)<<16) | ((b1&0x000000FF)<<24);
			buf.append(Integer.toHexString(a));
		}
		int len = buf.length();
		for (int i=32-len; i>0; i--)
			buf.insert(0, '0');
		return buf.toString();
	}
	
	public static void main(String[] args) throws IOException
	{
		
		// MD5Signer signer = new MD5Signer();
		Chap signer = new Chap();
		// int[] md5 = signer.sign(new ByteArrayInputStream("".getBytes()));
		int[] md5 = null;
		/***
		System.out.println(signer.md5string(md5));
		md5 = signer.sign(new ByteArrayInputStream("a".getBytes()));
		System.out.println(signer.md5string(md5));
		md5 = signer.sign(new ByteArrayInputStream("abc".getBytes()));
		System.out.println(signer.md5string(md5));
		md5 = signer.sign(new ByteArrayInputStream("message digest".getBytes()));
		System.out.println(signer.md5string(md5));
		md5 = signer.sign(new ByteArrayInputStream("abcdefghijklmnopqrstuvwxyz".getBytes()));
		System.out.println(signer.md5string(md5));
		md5 = signer.sign(new ByteArrayInputStream("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".getBytes()));
		System.out.println(signer.md5string(md5));
		md5 = signer.sign(new ByteArrayInputStream("12345678901234567890123456789012345678901234567890123456789012345678901234567890".getBytes()));
		System.out.println(signer.md5string(md5));
		md5 = signer.sign(new FileInputStream("/home/psiahu/Backup.zip"));
		System.out.println(signer.md5string(md5));
		System.out.println(Integer.toHexString((int)(0x80)));
		***/

		for( int i=0; i<args.length; i++ ) {
			md5 = signer.sign(new FileInputStream(args[i]));
			System.out.println(signer.md5string(md5));
			// System.out.println(Integer.toHexString((int)(0x80)));
		}
	}
}
