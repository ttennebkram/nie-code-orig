
package nie.filters.pdf.font;

import java.util.*;

/**
 * Describes the character mapping in the MacRoman encoding scheme
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 */
public class PDFEncodingMacRoman extends PDFFontEncoding {
    
    public PDFEncodingMacRoman() {
        encodingName = "MacRomanEncoding";
        
        encoding = new Hashtable();

        encoding.put(new Integer(101), getCharMapping("A"));
        encoding.put(new Integer(256), getCharMapping("AE"));
        encoding.put(new Integer(347), getCharMapping("Aacute"));
        encoding.put(new Integer(345), getCharMapping("Acircumflex"));
        encoding.put(new Integer(200), getCharMapping("Adieresis"));
        encoding.put(new Integer(313), getCharMapping("Agrave"));
        encoding.put(new Integer(201), getCharMapping("Aring"));
        encoding.put(new Integer(314), getCharMapping("Atilde"));
        encoding.put(new Integer(102), getCharMapping("B"));
        encoding.put(new Integer(103), getCharMapping("C"));
        encoding.put(new Integer(202), getCharMapping("Ccedilla"));
        encoding.put(new Integer(104), getCharMapping("D"));
        encoding.put(new Integer(105), getCharMapping("E"));
        encoding.put(new Integer(203), getCharMapping("Eacute"));
        encoding.put(new Integer(346), getCharMapping("Ecircumflex"));
        encoding.put(new Integer(350), getCharMapping("Edieresis"));
        encoding.put(new Integer(351), getCharMapping("Egrave"));
        encoding.put(new Integer(106), getCharMapping("F"));
        encoding.put(new Integer(107), getCharMapping("G"));
        encoding.put(new Integer(110), getCharMapping("H"));
        encoding.put(new Integer(111), getCharMapping("I"));
        encoding.put(new Integer(352), getCharMapping("Iacute"));
        encoding.put(new Integer(353), getCharMapping("Icircumflex"));
        encoding.put(new Integer(354), getCharMapping("Idieresis"));
        encoding.put(new Integer(355), getCharMapping("Igrave"));
        encoding.put(new Integer(112), getCharMapping("J"));
        encoding.put(new Integer(113), getCharMapping("K"));
        encoding.put(new Integer(114), getCharMapping("L"));
        encoding.put(new Integer(115), getCharMapping("M"));
        encoding.put(new Integer(116), getCharMapping("N"));
        encoding.put(new Integer(204), getCharMapping("Ntilde"));
        encoding.put(new Integer(117), getCharMapping("O"));
        
        encoding.put(new Integer(316), getCharMapping("OE"));
        encoding.put(new Integer(356), getCharMapping("Oacute"));
        encoding.put(new Integer(357), getCharMapping("Ocircumflex"));
        encoding.put(new Integer(205), getCharMapping("Odieresis"));
        encoding.put(new Integer(361), getCharMapping("Ograve"));
        encoding.put(new Integer(257), getCharMapping("Oslash"));
        encoding.put(new Integer(315), getCharMapping("Otilde"));
        encoding.put(new Integer(120), getCharMapping("P"));
        encoding.put(new Integer(121), getCharMapping("Q"));
        encoding.put(new Integer(122), getCharMapping("R"));
        encoding.put(new Integer(123), getCharMapping("S"));
        encoding.put(new Integer(124), getCharMapping("T"));
        encoding.put(new Integer(125), getCharMapping("U"));
        encoding.put(new Integer(362), getCharMapping("Uacute"));
        encoding.put(new Integer(363), getCharMapping("Ucircumflex"));
        encoding.put(new Integer(206), getCharMapping("Udieresis"));
        encoding.put(new Integer(364), getCharMapping("Ugrave"));
        encoding.put(new Integer(126), getCharMapping("V"));
        encoding.put(new Integer(127), getCharMapping("W"));
        encoding.put(new Integer(130), getCharMapping("X"));
        encoding.put(new Integer(131), getCharMapping("Y"));
        encoding.put(new Integer(331), getCharMapping("Ydieresis"));
        encoding.put(new Integer(132), getCharMapping("Z"));
        encoding.put(new Integer(141), getCharMapping("a"));
        encoding.put(new Integer(207), getCharMapping("aacute"));
        encoding.put(new Integer(211), getCharMapping("acircumflex"));
        encoding.put(new Integer(253), getCharMapping("acute"));
        encoding.put(new Integer(212), getCharMapping("adieresis"));
        encoding.put(new Integer(276), getCharMapping("ae"));
        encoding.put(new Integer(210), getCharMapping("agrave"));
        encoding.put(new Integer(46),  getCharMapping("ampersand"));
        
        encoding.put(new Integer(214), getCharMapping("aring"));
        encoding.put(new Integer(136), getCharMapping("asciicircum"));
        encoding.put(new Integer(176), getCharMapping("asciitilde"));
        encoding.put(new Integer(52),  getCharMapping("asterisk"));
        encoding.put(new Integer(100), getCharMapping("at"));
        encoding.put(new Integer(213), getCharMapping("atilde"));
        encoding.put(new Integer(142), getCharMapping("b"));
        encoding.put(new Integer(134), getCharMapping("backslash"));
        encoding.put(new Integer(174), getCharMapping("bar"));
        encoding.put(new Integer(173), getCharMapping("braceleft"));
        encoding.put(new Integer(175), getCharMapping("braceright"));
        encoding.put(new Integer(133), getCharMapping("bracketleft"));
        encoding.put(new Integer(135), getCharMapping("bracketright"));
        encoding.put(new Integer(371), getCharMapping("breve"));
        encoding.put(new Integer(245), getCharMapping("bullet"));
        encoding.put(new Integer(143), getCharMapping("c"));
        encoding.put(new Integer(377), getCharMapping("caron"));
        encoding.put(new Integer(215), getCharMapping("ccedilla"));
        encoding.put(new Integer(374), getCharMapping("cedilla"));
        encoding.put(new Integer(242), getCharMapping("cent"));
        encoding.put(new Integer(366), getCharMapping("circumflex"));
        encoding.put(new Integer(72),  getCharMapping("colon"));
        encoding.put(new Integer(54),  getCharMapping("comma"));
        encoding.put(new Integer(251), getCharMapping("copyright"));
        encoding.put(new Integer(333), getCharMapping("currency"));
        encoding.put(new Integer(144), getCharMapping("d"));
        encoding.put(new Integer(240), getCharMapping("dagger"));
        encoding.put(new Integer(340), getCharMapping("daggerdbl"));
        encoding.put(new Integer(241), getCharMapping("degree"));
        encoding.put(new Integer(254), getCharMapping("dieresis"));
        encoding.put(new Integer(326), getCharMapping("divide"));
        encoding.put(new Integer(44),  getCharMapping("dollar"));
        encoding.put(new Integer(372), getCharMapping("dotaccent"));
        encoding.put(new Integer(365), getCharMapping("dotlessi"));
        encoding.put(new Integer(145), getCharMapping("e"));
        encoding.put(new Integer(216), getCharMapping("eacute"));
        
        encoding.put(new Integer(220), getCharMapping("ecircumflex"));
        encoding.put(new Integer(221), getCharMapping("edieresis"));
        encoding.put(new Integer(217), getCharMapping("egrave"));
        encoding.put(new Integer(70),  getCharMapping("eight"));
        encoding.put(new Integer(311), getCharMapping("ellipsis"));
        encoding.put(new Integer(321), getCharMapping("emdash"));
        encoding.put(new Integer(320), getCharMapping("endash"));
        encoding.put(new Integer(75),  getCharMapping("equal"));
        encoding.put(new Integer(41),  getCharMapping("exclam"));
        encoding.put(new Integer(301), getCharMapping("exclamdown"));
        encoding.put(new Integer(146), getCharMapping("f"));
        encoding.put(new Integer(336), getCharMapping("fi"));
        encoding.put(new Integer(65),  getCharMapping("five"));
        encoding.put(new Integer(337), getCharMapping("fl"));
        encoding.put(new Integer(304), getCharMapping("florin"));
        encoding.put(new Integer(64),  getCharMapping("four"));
        encoding.put(new Integer(332), getCharMapping("fraction"));
        encoding.put(new Integer(147), getCharMapping("g"));
        encoding.put(new Integer(247), getCharMapping("germandbls"));
        encoding.put(new Integer(140), getCharMapping("grave"));
        encoding.put(new Integer(76),  getCharMapping("greater"));
        encoding.put(new Integer(307), getCharMapping("guillemotleft"));
        encoding.put(new Integer(310), getCharMapping("guillemotright"));
        encoding.put(new Integer(334), getCharMapping("guilsinglleft"));
        encoding.put(new Integer(335), getCharMapping("guilsinglright"));
        encoding.put(new Integer(150), getCharMapping("h"));
        encoding.put(new Integer(375), getCharMapping("hungarumlaut"));
        encoding.put(new Integer(55),  getCharMapping("hyphen"));
        encoding.put(new Integer(151), getCharMapping("i"));
        encoding.put(new Integer(222), getCharMapping("iacute"));
        encoding.put(new Integer(224), getCharMapping("icircumflex"));
        encoding.put(new Integer(225), getCharMapping("idieresis"));
        encoding.put(new Integer(223), getCharMapping("igrave"));
        encoding.put(new Integer(152), getCharMapping("j"));
        encoding.put(new Integer(153), getCharMapping("k"));
        encoding.put(new Integer(154), getCharMapping("l"));
        
        encoding.put(new Integer(74),  getCharMapping("less"));
        encoding.put(new Integer(302), getCharMapping("logicalnot"));
        encoding.put(new Integer(155), getCharMapping("m"));
        encoding.put(new Integer(370), getCharMapping("macron"));
        encoding.put(new Integer(265), getCharMapping("mu"));
        encoding.put(new Integer(156), getCharMapping("n"));
        encoding.put(new Integer(71),  getCharMapping("nine"));
        encoding.put(new Integer(226), getCharMapping("ntilde"));
        encoding.put(new Integer(43),  getCharMapping("numbersign"));
        encoding.put(new Integer(157), getCharMapping("o"));
        encoding.put(new Integer(227), getCharMapping("oacute"));
        encoding.put(new Integer(231), getCharMapping("ocircumflex"));
        encoding.put(new Integer(232), getCharMapping("odieresis"));
        encoding.put(new Integer(317), getCharMapping("oe"));
        encoding.put(new Integer(376), getCharMapping("ogonek"));
        encoding.put(new Integer(230), getCharMapping("ograve"));
        encoding.put(new Integer(61),  getCharMapping("one"));
        encoding.put(new Integer(273), getCharMapping("ordfeminine"));
        encoding.put(new Integer(274), getCharMapping("ordmasculine"));
        encoding.put(new Integer(277), getCharMapping("oslash"));
        encoding.put(new Integer(233), getCharMapping("otilde"));
        encoding.put(new Integer(160), getCharMapping("p"));
        encoding.put(new Integer(246), getCharMapping("paragraph"));
        encoding.put(new Integer(50),  getCharMapping("parenleft"));
        encoding.put(new Integer(51),  getCharMapping("parenright"));
        encoding.put(new Integer(45),  getCharMapping("percent"));
        encoding.put(new Integer(56),  getCharMapping("period"));
        encoding.put(new Integer(341), getCharMapping("periodcentered"));
        encoding.put(new Integer(344), getCharMapping("perthousand"));
        encoding.put(new Integer(53),  getCharMapping("plus"));
        encoding.put(new Integer(261), getCharMapping("plusminus"));
        
        encoding.put(new Integer(161), getCharMapping("q"));
        encoding.put(new Integer(77),  getCharMapping("question"));
        encoding.put(new Integer(300), getCharMapping("questiondown"));
        encoding.put(new Integer(42),  getCharMapping("quotedbl"));
        encoding.put(new Integer(343), getCharMapping("quotedblbase"));
        encoding.put(new Integer(322), getCharMapping("quotedblleft"));
        encoding.put(new Integer(323), getCharMapping("quotedblright"));
        encoding.put(new Integer(324), getCharMapping("quoteleft"));
        encoding.put(new Integer(325), getCharMapping("quoteright"));
        encoding.put(new Integer(342), getCharMapping("quotesinglbase"));
        encoding.put(new Integer(47),  getCharMapping("quotesingle"));
        encoding.put(new Integer(162), getCharMapping("r"));
        encoding.put(new Integer(250), getCharMapping("registered"));
        encoding.put(new Integer(373), getCharMapping("ring"));
        encoding.put(new Integer(163), getCharMapping("s"));
        encoding.put(new Integer(244), getCharMapping("section"));
        encoding.put(new Integer(73),  getCharMapping("semicolon"));
        encoding.put(new Integer(67),  getCharMapping("seven"));
        encoding.put(new Integer(66),  getCharMapping("six"));
        encoding.put(new Integer(57),  getCharMapping("slash"));
        encoding.put(new Integer(40),  getCharMapping("space"));
        encoding.put(new Integer(243), getCharMapping("sterling"));
        encoding.put(new Integer(164), getCharMapping("t"));
        encoding.put(new Integer(63),  getCharMapping("three"));
        encoding.put(new Integer(367), getCharMapping("tilde"));
        encoding.put(new Integer(252), getCharMapping("trademark"));
        encoding.put(new Integer(62),  getCharMapping("two"));
        encoding.put(new Integer(165), getCharMapping("u"));
        encoding.put(new Integer(234), getCharMapping("uacute"));
        encoding.put(new Integer(236), getCharMapping("ucircumflex"));
        encoding.put(new Integer(237), getCharMapping("udieresis"));
        encoding.put(new Integer(235), getCharMapping("ugrave"));
        
        encoding.put(new Integer(137), getCharMapping("underscore"));
        encoding.put(new Integer(166), getCharMapping("v"));
        encoding.put(new Integer(167), getCharMapping("w"));
        encoding.put(new Integer(170), getCharMapping("x"));
        encoding.put(new Integer(171), getCharMapping("y"));
        
        encoding.put(new Integer(330), getCharMapping("ydieresis"));
        encoding.put(new Integer(264), getCharMapping("yen"));
        encoding.put(new Integer(172), getCharMapping("z"));
        encoding.put(new Integer(60),  getCharMapping("zero"));
    }
}