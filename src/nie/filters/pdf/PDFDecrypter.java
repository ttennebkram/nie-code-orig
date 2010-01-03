
package nie.filters.pdf;

import java.io.*;
import java.util.*;
import nie.filters.*;

public class PDFDecrypter implements PDFConst {
    protected char[] passwordKeyPadding = {
        0x28, 0xBF, 0x4E, 0x5E, 0x4E, 0x75, 0x8A, 0x41,
        0x64, 0x00, 0x4E, 0x56, 0xFF, 0xFA, 0x01, 0x08,
        0x2E, 0x2E, 0x00, 0xB6, 0xD0, 0x68, 0x3E, 0x80,
        0x2F, 0x0C, 0xA9, 0xFE, 0x64, 0x53, 0x69, 0x7A,
    };
    
    // Start off assuming the passwords are ok (in case there is no encryption)
    boolean ownerPassOK = true;
    boolean userPassOK = true;
    
    // Default to 5 bit key length
    int keyLength = 5;
    
    // The computed file key
    char[] fileKey = null;
    
    // Some values that get pulled out of the attrs array for easier reference
    int encRevision = 2;
    int encVersion = 1;
    char[] ownerKey;
    char[] userKey;
    
    // Whether or not this file actually contains encrypted data
    boolean isEncrypted = false;
    
    // Cache some stuff for performance reasons...
    int lastObjNum, lastObjGen;
    char[] lastObjKey;
    
    // The encryption dictionary
    Hashtable attrs;
    
    public PDFDecrypter(Vector fileId, Hashtable attrs, String ownerPass, String userPass) 
      throws PDFException {
        this.attrs = attrs;
    
        // If the encryption dictionary is empty, it's not encrypted
        // and this class should just be a pass through
        if (attrs.size() == 0) {
            isEncrypted = false;
            return;
        }
    
        this.encVersion = ((Float)attrs.get("V")).intValue();
        this.encRevision = ((Float)attrs.get("R")).intValue();

        /*
        char[] OEntry = {
            0x20, 0x55, 0xc7, 0x56, 0xc7, 0x2e, 0x1a, 0xd7,
            0x02, 0x60, 0x8e, 0x81, 0x96, 0xac, 0xad, 0x44, 
            0x7a, 0xd3, 0x2d, 0x17, 0xcf, 0xf5, 0x83, 0x23, 
            0x5f, 0x6d, 0xd1, 0x5f, 0xed, 0x7d, 0xab, 0x67
        };

        char[] UEntry = {
            0x24, 0xd4, 0x99, 0xd5, 0x9f, 0x8b, 0xe4, 0x94,
            0xf1, 0x84, 0x42, 0xcb, 0x29, 0xb7, 0x33, 0xb9, 
            0xd2, 0xc7, 0x6e, 0x79, 0x75, 0x23, 0x11, 0x84, 
            0x3c, 0x54, 0xe6, 0x32, 0xfb, 0x3f, 0x88, 0x28
        };
        */
            
        String oKey = (String)attrs.get("O");
        this.ownerKey = new char[oKey.length()];
        oKey.getChars(0, oKey.length(), this.ownerKey, 0);
        String uKey = (String)attrs.get("U");
        this.userKey = new char[uKey.length()];
        uKey.getChars(0, uKey.length(), this.userKey, 0);
        //this.ownerKey = OEntry;
        //this.userKey = UEntry;

        // Do some checks on the data
        String filterName = (String)attrs.get("Filter");
        if (!(filterName.equals("Standard") || filterName.equals("/Standard")))
            throw new PDFException("Unknown encryption scheme");
        if (ownerKey.length != 32 || userKey.length != 32)
            throw new PDFException("Invalid user/owner keys");
        
        if (attrs.containsKey("Length")) {
            int len = ((Float)attrs.get("Length")).intValue();
            this.keyLength = len / 8;
        }
        if (encVersion < 1 || encVersion > 2 || encRevision < 2 || encRevision > 3)
            throw new PDFException("Unsupported version of the security handler");
        
        String fileID = (String)fileId.elementAt(0);
        if (!makeFileKey(ownerPass, userPass, fileID)) {
            throw new PDFException("Incorrect password");
        }
        
        // Everything looks good, signal that the file is encrypted
        this.isEncrypted = true;

    }

    public boolean isOwnerPassOK() {
        return ownerPassOK;
    }
    
    public boolean isUserPassOK() {
        return userPassOK;
    }

    public String decryptString(String txt, int objNum, int objGen) {
        return new String(decryptBytes(txt.getBytes(), objNum, objGen));
    }
    
    public byte[] decryptBytes(byte[] txt, int objNum, int objGen) {
        // If the document isn't encrypted, just return the txt
        if (!isEncrypted) { return txt; }
        
        char[] state = new char[256];
        char[] objKey;
        if (objNum != lastObjNum || objGen != lastObjGen) {
            objKey = makeObjectKey(objNum, objGen);
            lastObjNum = objNum;
            lastObjGen = objGen;
            lastObjKey = objKey;
        } else {
            objKey = lastObjKey;
        }
    
        int objKeyLength = keyLength + 5;
        if (objKeyLength > 16) { objKeyLength = 16; }

        // Setup for decryption
        rc4InitKey(objKey, objKeyLength, state);
        
        // Do the decryption
        byte[] rtxt = new byte[txt.length];
        int[] xy = {0, 0};
        for (int i=0; i<txt.length; i++) {
            rtxt[i] = (byte)rc4DecryptByte(state, xy, (char)txt[i]);
        }
        return rtxt;
    }

    protected char[] makeObjectKey(int objNum, int objGen) {
        char[] objKey = new char[21];
  
        // build the key
        for (int i=0; i<keyLength; i++) {
            objKey[i] = fileKey[i];
        }
        objKey[keyLength] = (char)(objNum & 0xFF);
        objKey[keyLength + 1] = (char)((objNum >> 8) & 0xFF);
        objKey[keyLength + 2] = (char)((objNum >> 16) & 0xFF);
        objKey[keyLength + 3] = (char)(objGen & 0xFF);
        objKey[keyLength + 4] = (char)((objGen >> 8) & 0xFF);
        
        objKey = md5(objKey, keyLength+5);
        return objKey;
    }

    protected boolean makeFileKey(String ownerPass, String userPass, String fileID) {
        String buf = "";

        if (!ownerPass.equals("")) {
            int len = ownerPass.length();
            if (len < 32) {
                buf += ownerPass;
                buf += new String(passwordKeyPadding, 0, 32 - len);
            } else {
                buf += ownerPass.substring(0, 32);
            }
        } else {
            buf = new String(passwordKeyPadding);
        }
        
        char[] fState = new char[256];
        char[] test = md5(buf, 32);
        if (encRevision == 3) {
            for (int i=0; i<50; ++i) {
                test = md5(test, 16);
            }
        }
        rc4InitKey(test, keyLength, fState);
        
        int[] fxy = {0, 0};
        char[] test2 = new char[32];
        for (int i=0; i<32; i++) {
            test2[i] = rc4DecryptByte(fState, fxy, ownerKey[i]);
        }
        
        String userPassword2 = new String(test2);
        if (makeFileKey2(userPassword2, fileID)) {
            ownerPassOK = true;
            userPassOK = true;
            return true;
        }

        ownerPassOK = false;
        return makeFileKey2(userPass, fileID);
    }
        
    protected boolean makeFileKey2(String userPassword, String fileID) {
        String buf = "";
        
        if (!userPassword.equals("")) {
            int len = userPassword.length();
            if (len < 32) {
                buf += userPassword;
                buf += new String(passwordKeyPadding, 0, 32 - len);
            } else {
                buf += userPassword.substring(0, 32);
            }
        } else {
            buf = new String(passwordKeyPadding);
        }
        
        buf += new String(ownerKey);
        
        int pval = ((Float)attrs.get("P")).intValue();
        char[] cpval = new char[4];
        cpval[0] = (char)(pval & 0xff);
        cpval[1] = (char)((pval >> 8) & 0xff);
        cpval[2] = (char)((pval >> 16) & 0xff);
        cpval[3] = (char)((pval >> 24) & 0xff);
        buf += new String(cpval);

        buf += fileID;
        
        // Set the fileKey
        this.fileKey = md5(buf, buf.length());
        if (encRevision == 3) {
            for (int i=0; i<50; ++i) {
                this.fileKey = md5(this.fileKey, 16);
            }
        }
        
        // test the user password
        boolean ok = true;
        char[] fState = new char[256];
        int[] fxy = {0, 0};
        if (encRevision == 2) {
            rc4InitKey(fileKey, keyLength, fState);
        
            char[] test = new char[32];
            for (int i=0; i<32; i++) {
                test[i] = rc4DecryptByte(fState, fxy, userKey[i]);
            }
            for (int i=0; i<32; i++) {
                if ((int)test[i] != passwordKeyPadding[i]) {
                    ok = false;
                    break;
                }
            }
        } else if (encRevision == 3) {
            String test = new String(userKey);
            char[] test2 = new char[32];
            char[] tmpKey = new char[16];
            for (int i=19; i>=0; --i) {
                for (int j=0; j<keyLength; ++j) {
                    tmpKey[j] = (char)(fileKey[j] ^ i);
                }
                rc4InitKey(tmpKey, keyLength, fState);
                for (i=0; i<32; ++i) {
                    test2[i] = rc4DecryptByte(fState, fxy, test.charAt(i));
                }
            }
            buf = new String(passwordKeyPadding);
            buf += fileID;
            char[] buf2 = md5(buf, buf.length());
            for (int i=0; i<32; i++) {
                if ((int)buf2[i] != test2[i]) {
                    ok = false;
                    break;
                }
            }
        } else {
            ok = false;
        }

        return ok;
    }
    

//------------------------------------------------------------------------
// RC4 Decryption
//------------------------------------------------------------------------

    public void rc4InitKey(char[] key, int keyLen, char[] state) {
        char t;
        int i, index1, index2;
        
        for (i=0; i<256; ++i) {
            state[i] = (char)i;
        }
        index1 = index2 = 0;
        for (i=0; i<256; ++i) {
            index2 = (key[index1] + state[i] + index2) % 256;
            t = state[i];
            state[i] = state[index2];
            state[index2] = t;
            index1 = (index1 + 1) % keyLen;
        }
    }
  
    public char rc4DecryptByte(char[] state, int[] xy, char c) {
        char tx, ty;
        int x1, y1;

        x1 = xy[0] = (xy[0] + 1) % 256;
        y1 = xy[1] = (state[xy[0]] + xy[1]) % 256;
  
        tx = state[x1];
        ty = state[y1];
        state[x1] = ty;
        state[y1] = tx;

        int t;
        t = (int)c;
        t = (tx + ty);
        t = (tx + ty) % 256;
        t = state[(tx + ty) % 256];
        t = (c ^ state[(tx + ty) % 256]);
        
        return (char)(c ^ state[(tx + ty) % 256]);
    }

//------------------------------------------------------------------------
// MD5 message digest
//------------------------------------------------------------------------
    
    protected int md5Round1(int a,int b,int c,int d,int k,int s,int t)
    {
        a += k + t + (d ^ (b & (c ^ d)));
        a = (a << s | a >>> -s);
        return a + b;
    }

    protected int md5Round2(int a,int b,int c,int d,int k,int s,int t)
    {
        a += k + t + (c ^ (d & (b ^ c)));
        a = (a << s | a >>> -s);
        return a + b;
    }

    protected int md5Round3(int a,int b,int c,int d,int k,int s,int t)
    {
        a += k + t + (b ^ c ^ d);
        a = (a << s | a >>> -s);
        return a + b;
    }

    protected int md5Round4(int a,int b,int c,int d,int k,int s,int t)
    {
        a += k + t + (c ^ (b | ~d));
        a = (a << s | a >>> -s);
        return a + b;
    }

    public char[] md5(String smsg, int msgLen) {
        char[] msg = new char[smsg.length()];
        for (int i=0; i<smsg.length(); i++) {
            msg[i] = smsg.charAt(i);
        }
        return md5(msg, msgLen);
    }
    
    public char[] md5(char[] msg, int msgLen) {
        int x[] = new int[16];
        int a, b, c, d, aa, bb, cc, dd;
        int n64;
        int i, j, k;
        char[] digest = new char[16];

        // compute number of 64-byte blocks
        // (length + pad byte (0x80) + 8 bytes for length)
        n64 = (msgLen + 1 + 8 + 63) / 64;

        // initialize a, b, c, d
        a = 0x67452301;
        b = 0xefcdab89;
        c = 0x98badcfe;
        d = 0x10325476;

        // loop through blocks
        k = 0;
        for (i = 0; i < n64; ++i) {
            // grab a 64-byte block
            for (j = 0; j < 16 && k < msgLen - 3; ++j, k += 4) {
                x[j] = ((((((msg[k+3] << 8) + msg[k+2]) << 8) + msg[k+1]) << 8) + msg[k]);
            }
            if (i == n64 - 1) {
                if (k == msgLen - 3)
	                x[j] = 0x80000000 + (((msg[k+2] << 8) + msg[k+1]) << 8) + msg[k];
                else if (k == msgLen - 2)
	                x[j] = 0x800000 + (msg[k+1] << 8) + msg[k];
                else if (k == msgLen - 1)
	                x[j] = 0x8000 + msg[k];
                else
	                x[j] = 0x80;
                ++j;
                while (j < 16)
	                x[j++] = 0;
                x[14] = msgLen << 3;
            }

            // save a, b, c, d
            aa = a;
            bb = b;
            cc = c;
            dd = d;

            // round 1
            a = md5Round1(a, b, c, d, x[0],   7, 0xd76aa478);
            d = md5Round1(d, a, b, c, x[1],  12, 0xe8c7b756);
            c = md5Round1(c, d, a, b, x[2],  17, 0x242070db);
            b = md5Round1(b, c, d, a, x[3],  22, 0xc1bdceee);
            a = md5Round1(a, b, c, d, x[4],   7, 0xf57c0faf);
            d = md5Round1(d, a, b, c, x[5],  12, 0x4787c62a);
            c = md5Round1(c, d, a, b, x[6],  17, 0xa8304613);
            b = md5Round1(b, c, d, a, x[7],  22, 0xfd469501);
            a = md5Round1(a, b, c, d, x[8],   7, 0x698098d8);
            d = md5Round1(d, a, b, c, x[9],  12, 0x8b44f7af);
            c = md5Round1(c, d, a, b, x[10], 17, 0xffff5bb1);
            b = md5Round1(b, c, d, a, x[11], 22, 0x895cd7be);
            a = md5Round1(a, b, c, d, x[12],  7, 0x6b901122);
            d = md5Round1(d, a, b, c, x[13], 12, 0xfd987193);
            c = md5Round1(c, d, a, b, x[14], 17, 0xa679438e);
            b = md5Round1(b, c, d, a, x[15], 22, 0x49b40821);

            // round 2
            a = md5Round2(a, b, c, d, x[1],   5, 0xf61e2562);
            d = md5Round2(d, a, b, c, x[6],   9, 0xc040b340);
            c = md5Round2(c, d, a, b, x[11], 14, 0x265e5a51);
            b = md5Round2(b, c, d, a, x[0],  20, 0xe9b6c7aa);
            a = md5Round2(a, b, c, d, x[5],   5, 0xd62f105d);
            d = md5Round2(d, a, b, c, x[10],  9, 0x02441453);
            c = md5Round2(c, d, a, b, x[15], 14, 0xd8a1e681);
            b = md5Round2(b, c, d, a, x[4],  20, 0xe7d3fbc8);
            a = md5Round2(a, b, c, d, x[9],   5, 0x21e1cde6);
            d = md5Round2(d, a, b, c, x[14],  9, 0xc33707d6);
            c = md5Round2(c, d, a, b, x[3],  14, 0xf4d50d87);
            b = md5Round2(b, c, d, a, x[8],  20, 0x455a14ed);
            a = md5Round2(a, b, c, d, x[13],  5, 0xa9e3e905);
            d = md5Round2(d, a, b, c, x[2],   9, 0xfcefa3f8);
            c = md5Round2(c, d, a, b, x[7],  14, 0x676f02d9);
            b = md5Round2(b, c, d, a, x[12], 20, 0x8d2a4c8a);

            // round 3
            a = md5Round3(a, b, c, d, x[5],   4, 0xfffa3942);
            d = md5Round3(d, a, b, c, x[8],  11, 0x8771f681);
            c = md5Round3(c, d, a, b, x[11], 16, 0x6d9d6122);
            b = md5Round3(b, c, d, a, x[14], 23, 0xfde5380c);
            a = md5Round3(a, b, c, d, x[1],   4, 0xa4beea44);
            d = md5Round3(d, a, b, c, x[4],  11, 0x4bdecfa9);
            c = md5Round3(c, d, a, b, x[7],  16, 0xf6bb4b60);
            b = md5Round3(b, c, d, a, x[10], 23, 0xbebfbc70);
            a = md5Round3(a, b, c, d, x[13],  4, 0x289b7ec6);
            d = md5Round3(d, a, b, c, x[0],  11, 0xeaa127fa);
            c = md5Round3(c, d, a, b, x[3],  16, 0xd4ef3085);
            b = md5Round3(b, c, d, a, x[6],  23, 0x04881d05);
            a = md5Round3(a, b, c, d, x[9],   4, 0xd9d4d039);
            d = md5Round3(d, a, b, c, x[12], 11, 0xe6db99e5);
            c = md5Round3(c, d, a, b, x[15], 16, 0x1fa27cf8);
            b = md5Round3(b, c, d, a, x[2],  23, 0xc4ac5665);

            // round 4
            a = md5Round4(a, b, c, d, x[0],   6, 0xf4292244);
            d = md5Round4(d, a, b, c, x[7],  10, 0x432aff97);
            c = md5Round4(c, d, a, b, x[14], 15, 0xab9423a7);
            b = md5Round4(b, c, d, a, x[5],  21, 0xfc93a039);
            a = md5Round4(a, b, c, d, x[12],  6, 0x655b59c3);
            d = md5Round4(d, a, b, c, x[3],  10, 0x8f0ccc92);
            c = md5Round4(c, d, a, b, x[10], 15, 0xffeff47d);
            b = md5Round4(b, c, d, a, x[1],  21, 0x85845dd1);
            a = md5Round4(a, b, c, d, x[8],   6, 0x6fa87e4f);
            d = md5Round4(d, a, b, c, x[15], 10, 0xfe2ce6e0);
            c = md5Round4(c, d, a, b, x[6],  15, 0xa3014314);
            b = md5Round4(b, c, d, a, x[13], 21, 0x4e0811a1);
            a = md5Round4(a, b, c, d, x[4],   6, 0xf7537e82);
            d = md5Round4(d, a, b, c, x[11], 10, 0xbd3af235);
            c = md5Round4(c, d, a, b, x[2],  15, 0x2ad7d2bb);
            b = md5Round4(b, c, d, a, x[9],  21, 0xeb86d391);

            // increment a, b, c, d
            a += aa;
            b += bb;
            c += cc;
            d += dd;
        }

        // break digest into bytes
        digest[0] = (char)(a & 0xff);
        digest[1] = (char)((a >>= 8) & 0xff);
        digest[2] = (char)((a >>= 8) & 0xff);
        digest[3] = (char)((a >>= 8) & 0xff);
        digest[4] = (char)(b & 0xff);
        digest[5] = (char)((b >>= 8) & 0xff);
        digest[6] = (char)((b >>= 8) & 0xff);
        digest[7] = (char)((b >>= 8) & 0xff);
        digest[8] = (char)(c & 0xff);
        digest[9] = (char)((c >>= 8) & 0xff);
        digest[10] = (char)((c >>= 8) & 0xff);
        digest[11] = (char)((c >>= 8) & 0xff);
        digest[12] = (char)(d & 0xff);
        digest[13] = (char)((d >>= 8) & 0xff);
        digest[14] = (char)((d >>= 8) & 0xff);
        digest[15] = (char)((d >>= 8) & 0xff);
        
        return digest;
    }
    
}