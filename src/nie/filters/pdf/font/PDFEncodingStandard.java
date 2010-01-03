
package nie.filters.pdf.font;

import java.util.*;

/**
 * Describes the base character encoding in pdf documents
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 */
public class PDFEncodingStandard extends PDFFontEncoding {
    
    public PDFEncodingStandard() {
        encodingName = "StandardEncoding";
        
        encoding = new Hashtable();

        encoding.put(new Integer(101), getCharMapping("A"));
        encoding.put(new Integer(341), getCharMapping("AE"));
        encoding.put(new Integer(102), getCharMapping("B"));
        encoding.put(new Integer(103), getCharMapping("C"));
        encoding.put(new Integer(104), getCharMapping("D"));
        encoding.put(new Integer(105), getCharMapping("E"));
        encoding.put(new Integer(106), getCharMapping("F"));
        encoding.put(new Integer(107), getCharMapping("G"));
        encoding.put(new Integer(110), getCharMapping("H"));
        encoding.put(new Integer(111), getCharMapping("I"));
        encoding.put(new Integer(112), getCharMapping("J"));
        encoding.put(new Integer(113), getCharMapping("K"));
        encoding.put(new Integer(114), getCharMapping("L"));
        encoding.put(new Integer(350), getCharMapping("Lslash"));
        encoding.put(new Integer(115), getCharMapping("M"));
        encoding.put(new Integer(116), getCharMapping("N"));
        encoding.put(new Integer(117), getCharMapping("O"));
        
        encoding.put(new Integer(352), getCharMapping("OE"));
        encoding.put(new Integer(351), getCharMapping("Oslash"));
        encoding.put(new Integer(120), getCharMapping("P"));
        encoding.put(new Integer(121), getCharMapping("Q"));
        encoding.put(new Integer(122), getCharMapping("R"));
        encoding.put(new Integer(123), getCharMapping("S"));
        encoding.put(new Integer(124), getCharMapping("T"));
        encoding.put(new Integer(125), getCharMapping("U"));
        encoding.put(new Integer(126), getCharMapping("V"));
        encoding.put(new Integer(127), getCharMapping("W"));
        encoding.put(new Integer(130), getCharMapping("X"));
        encoding.put(new Integer(131), getCharMapping("Y"));
        encoding.put(new Integer(132), getCharMapping("Z"));
        encoding.put(new Integer(141), getCharMapping("a"));
        encoding.put(new Integer(302), getCharMapping("acute"));
        encoding.put(new Integer(361), getCharMapping("ae"));
        encoding.put(new Integer(46),  getCharMapping("ampersand"));
        
        encoding.put(new Integer(136), getCharMapping("asciicircum"));
        encoding.put(new Integer(176), getCharMapping("asciitilde"));
        encoding.put(new Integer(52),  getCharMapping("asterisk"));
        encoding.put(new Integer(100), getCharMapping("at"));
        encoding.put(new Integer(142), getCharMapping("b"));
        encoding.put(new Integer(134), getCharMapping("backslash"));
        encoding.put(new Integer(174), getCharMapping("bar"));
        encoding.put(new Integer(173), getCharMapping("braceleft"));
        encoding.put(new Integer(175), getCharMapping("braceright"));
        encoding.put(new Integer(133), getCharMapping("bracketleft"));
        encoding.put(new Integer(135), getCharMapping("bracketright"));
        encoding.put(new Integer(306), getCharMapping("breve"));
        encoding.put(new Integer(267), getCharMapping("bullet"));
        encoding.put(new Integer(143), getCharMapping("c"));
        encoding.put(new Integer(317), getCharMapping("caron"));
        encoding.put(new Integer(313), getCharMapping("cedilla"));
        encoding.put(new Integer(242), getCharMapping("cent"));
        encoding.put(new Integer(303), getCharMapping("circumflex"));
        encoding.put(new Integer(72),  getCharMapping("colon"));
        encoding.put(new Integer(54),  getCharMapping("comma"));
        encoding.put(new Integer(250), getCharMapping("currency"));
        encoding.put(new Integer(144), getCharMapping("d"));
        encoding.put(new Integer(262), getCharMapping("dagger"));
        encoding.put(new Integer(263), getCharMapping("daggerdbl"));
        encoding.put(new Integer(310), getCharMapping("dieresis"));
        encoding.put(new Integer(44),  getCharMapping("dollar"));
        encoding.put(new Integer(307), getCharMapping("dotaccent"));
        encoding.put(new Integer(365), getCharMapping("dotlessi"));
        encoding.put(new Integer(145), getCharMapping("e"));
        
        encoding.put(new Integer(70),  getCharMapping("eight"));
        encoding.put(new Integer(274), getCharMapping("ellipsis"));
        encoding.put(new Integer(320), getCharMapping("emdash"));
        encoding.put(new Integer(261), getCharMapping("endash"));
        encoding.put(new Integer(75),  getCharMapping("equal"));
        encoding.put(new Integer(41),  getCharMapping("exclam"));
        encoding.put(new Integer(241), getCharMapping("exclamdown"));
        encoding.put(new Integer(146), getCharMapping("f"));
        encoding.put(new Integer(256), getCharMapping("fi"));
        encoding.put(new Integer(65),  getCharMapping("five"));
        encoding.put(new Integer(257), getCharMapping("fl"));
        encoding.put(new Integer(246), getCharMapping("florin"));
        encoding.put(new Integer(64),  getCharMapping("four"));
        encoding.put(new Integer(244), getCharMapping("fraction"));
        encoding.put(new Integer(147), getCharMapping("g"));
        encoding.put(new Integer(373), getCharMapping("germandbls"));
        encoding.put(new Integer(301), getCharMapping("grave"));
        encoding.put(new Integer(76),  getCharMapping("greater"));
        encoding.put(new Integer(253), getCharMapping("guillemotleft"));
        encoding.put(new Integer(273), getCharMapping("guillemotright"));
        encoding.put(new Integer(254), getCharMapping("guilsinglleft"));
        encoding.put(new Integer(255), getCharMapping("guilsinglright"));
        encoding.put(new Integer(150), getCharMapping("h"));
        encoding.put(new Integer(315), getCharMapping("hungarumlaut"));
        encoding.put(new Integer(55),  getCharMapping("hyphen"));
        encoding.put(new Integer(151), getCharMapping("i"));
        encoding.put(new Integer(152), getCharMapping("j"));
        encoding.put(new Integer(153), getCharMapping("k"));
        encoding.put(new Integer(154), getCharMapping("l"));
        
        encoding.put(new Integer(74),  getCharMapping("less"));
        encoding.put(new Integer(370), getCharMapping("lslash"));
        encoding.put(new Integer(155), getCharMapping("m"));
        encoding.put(new Integer(305), getCharMapping("macron"));
        encoding.put(new Integer(156), getCharMapping("n"));
        encoding.put(new Integer(71),  getCharMapping("nine"));
        encoding.put(new Integer(43),  getCharMapping("numbersign"));
        encoding.put(new Integer(157), getCharMapping("o"));
        encoding.put(new Integer(372), getCharMapping("oe"));
        encoding.put(new Integer(316), getCharMapping("ogonek"));
        encoding.put(new Integer(61),  getCharMapping("one"));
        encoding.put(new Integer(343), getCharMapping("ordfeminine"));
        encoding.put(new Integer(353), getCharMapping("ordmasculine"));
        encoding.put(new Integer(371), getCharMapping("oslash"));
        encoding.put(new Integer(160), getCharMapping("p"));
        encoding.put(new Integer(266), getCharMapping("paragraph"));
        encoding.put(new Integer(50),  getCharMapping("parenleft"));
        encoding.put(new Integer(51),  getCharMapping("parenright"));
        encoding.put(new Integer(45),  getCharMapping("percent"));
        encoding.put(new Integer(56),  getCharMapping("period"));
        encoding.put(new Integer(264), getCharMapping("periodcentered"));
        encoding.put(new Integer(275), getCharMapping("perthousand"));
        encoding.put(new Integer(53),  getCharMapping("plus"));
        
        encoding.put(new Integer(161), getCharMapping("q"));
        encoding.put(new Integer(77),  getCharMapping("question"));
        encoding.put(new Integer(277), getCharMapping("questiondown"));
        encoding.put(new Integer(42),  getCharMapping("quotedbl"));
        encoding.put(new Integer(271), getCharMapping("quotedblbase"));
        encoding.put(new Integer(252), getCharMapping("quotedblleft"));
        encoding.put(new Integer(272), getCharMapping("quotedblright"));
        encoding.put(new Integer(140), getCharMapping("quoteleft"));
        encoding.put(new Integer(47),  getCharMapping("quoteright"));
        encoding.put(new Integer(270), getCharMapping("quotesinglbase"));
        encoding.put(new Integer(251), getCharMapping("quotesingle"));
        encoding.put(new Integer(162), getCharMapping("r"));
        encoding.put(new Integer(312), getCharMapping("ring"));
        encoding.put(new Integer(163), getCharMapping("s"));
        encoding.put(new Integer(247), getCharMapping("section"));
        encoding.put(new Integer(73),  getCharMapping("semicolon"));
        encoding.put(new Integer(67),  getCharMapping("seven"));
        encoding.put(new Integer(66),  getCharMapping("six"));
        encoding.put(new Integer(57),  getCharMapping("slash"));
        encoding.put(new Integer(40),  getCharMapping("space"));
        encoding.put(new Integer(243), getCharMapping("sterling"));
        encoding.put(new Integer(164), getCharMapping("t"));
        encoding.put(new Integer(63),  getCharMapping("three"));
        encoding.put(new Integer(304), getCharMapping("tilde"));
        encoding.put(new Integer(62),  getCharMapping("two"));
        encoding.put(new Integer(165), getCharMapping("u"));
        
        encoding.put(new Integer(137), getCharMapping("underscore"));
        encoding.put(new Integer(166), getCharMapping("v"));
        encoding.put(new Integer(167), getCharMapping("w"));
        encoding.put(new Integer(170), getCharMapping("x"));
        encoding.put(new Integer(171), getCharMapping("y"));
        
        encoding.put(new Integer(245), getCharMapping("yen"));
        encoding.put(new Integer(172), getCharMapping("z"));
        encoding.put(new Integer(60),  getCharMapping("zero"));
        
    }
}
