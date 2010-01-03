
package nie.filters.pdf.font;

import java.util.*;

/**
 * Description of the standard font Courier-Oblique
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 */
public class PDFFontCourierOblique extends PDFFontDesc {

    public PDFFontCourierOblique() {
        fontName = "Courier-Oblique";
        baseEncoding = "MacRomanEncoding";
        isStandardFont = true;
        
        fontDescriptor = new Hashtable();
        fontDescriptor.put("Ascent", new Float(629));
        fontDescriptor.put("CapHeight", new Float(562));
        fontDescriptor.put("Descent", new Float(-157));
        fontDescriptor.put("ItalicAngle", new Float(-12));
        fontDescriptor.put("AvgWidth", new Float(250));
        fontDescriptor.put("MissingWidth", new Float(250));
        fontDescriptor.put("Weight", new Float(1));

        fontWidths = new Hashtable();
        
        fontWidths.put(new Integer(32), new Float(600));		// space
        fontWidths.put(new Integer(33), new Float(600));		// exclam
        fontWidths.put(new Integer(34), new Float(600));		// quotedbl
        fontWidths.put(new Integer(35), new Float(600));		// numbersign
        fontWidths.put(new Integer(36), new Float(600));		// dollar
        fontWidths.put(new Integer(37), new Float(600));		// percent
        fontWidths.put(new Integer(38), new Float(600));		// ampersand
        fontWidths.put(new Integer(39), new Float(600));		// quoteright
        fontWidths.put(new Integer(40), new Float(600));		// parenleft
        fontWidths.put(new Integer(41), new Float(600));		// parenright
        fontWidths.put(new Integer(42), new Float(600));		// asterisk
        fontWidths.put(new Integer(43), new Float(600));		// plus
        fontWidths.put(new Integer(44), new Float(600));		// comma
        fontWidths.put(new Integer(45), new Float(600));		// hyphen
        fontWidths.put(new Integer(46), new Float(600));		// period
        fontWidths.put(new Integer(47), new Float(600));		// slash
        fontWidths.put(new Integer(48), new Float(600));		// zero
        fontWidths.put(new Integer(49), new Float(600));		// one
        fontWidths.put(new Integer(50), new Float(600));		// two
        fontWidths.put(new Integer(51), new Float(600));		// three
        fontWidths.put(new Integer(52), new Float(600));		// four
        fontWidths.put(new Integer(53), new Float(600));		// five
        fontWidths.put(new Integer(54), new Float(600));		// six
        fontWidths.put(new Integer(55), new Float(600));		// seven
        fontWidths.put(new Integer(56), new Float(600));		// eight
        fontWidths.put(new Integer(57), new Float(600));		// nine
        fontWidths.put(new Integer(58), new Float(600));		// colon
        fontWidths.put(new Integer(59), new Float(600));		// semicolon
        fontWidths.put(new Integer(60), new Float(600));		// less
        fontWidths.put(new Integer(61), new Float(600));		// equal
        fontWidths.put(new Integer(62), new Float(600));		// greater
        fontWidths.put(new Integer(63), new Float(600));		// question
        fontWidths.put(new Integer(64), new Float(600));		// at
        fontWidths.put(new Integer(65), new Float(600));		// A
        fontWidths.put(new Integer(66), new Float(600));		// B
        fontWidths.put(new Integer(67), new Float(600));		// C
        fontWidths.put(new Integer(68), new Float(600));		// D
        fontWidths.put(new Integer(69), new Float(600));		// E
        fontWidths.put(new Integer(70), new Float(600));		// F
        fontWidths.put(new Integer(71), new Float(600));		// G
        fontWidths.put(new Integer(72), new Float(600));		// H
        fontWidths.put(new Integer(73), new Float(600));		// I
        fontWidths.put(new Integer(74), new Float(600));		// J
        fontWidths.put(new Integer(75), new Float(600));		// K
        fontWidths.put(new Integer(76), new Float(600));		// L
        fontWidths.put(new Integer(77), new Float(600));		// M
        fontWidths.put(new Integer(78), new Float(600));		// N
        fontWidths.put(new Integer(79), new Float(600));		// O
        fontWidths.put(new Integer(80), new Float(600));		// P
        fontWidths.put(new Integer(81), new Float(600));		// Q
        fontWidths.put(new Integer(82), new Float(600));		// R
        fontWidths.put(new Integer(83), new Float(600));		// S
        fontWidths.put(new Integer(84), new Float(600));		// T
        fontWidths.put(new Integer(85), new Float(600));		// U
        fontWidths.put(new Integer(86), new Float(600));		// V
        fontWidths.put(new Integer(87), new Float(600));		// W
        fontWidths.put(new Integer(88), new Float(600));		// X
        fontWidths.put(new Integer(89), new Float(600));		// Y
        fontWidths.put(new Integer(90), new Float(600));		// Z
        fontWidths.put(new Integer(91), new Float(600));		// bracketleft
        fontWidths.put(new Integer(92), new Float(600));		// backslash
        fontWidths.put(new Integer(93), new Float(600));		// bracketright
        fontWidths.put(new Integer(94), new Float(600));		// asciicircum
        fontWidths.put(new Integer(95), new Float(600));		// underscore
        fontWidths.put(new Integer(96), new Float(600));		// quoteleft
        fontWidths.put(new Integer(97), new Float(600));		// a
        fontWidths.put(new Integer(98), new Float(600));		// b
        fontWidths.put(new Integer(99), new Float(600));		// c
        fontWidths.put(new Integer(100), new Float(600));		// d
        fontWidths.put(new Integer(101), new Float(600));		// e
        fontWidths.put(new Integer(102), new Float(600));		// f
        fontWidths.put(new Integer(103), new Float(600));		// g
        fontWidths.put(new Integer(104), new Float(600));		// h
        fontWidths.put(new Integer(105), new Float(600));		// i
        fontWidths.put(new Integer(106), new Float(600));		// j
        fontWidths.put(new Integer(107), new Float(600));		// k
        fontWidths.put(new Integer(108), new Float(600));		// l
        fontWidths.put(new Integer(109), new Float(600));		// m
        fontWidths.put(new Integer(110), new Float(600));		// n
        fontWidths.put(new Integer(111), new Float(600));		// o
        fontWidths.put(new Integer(112), new Float(600));		// p
        fontWidths.put(new Integer(113), new Float(600));		// q
        fontWidths.put(new Integer(114), new Float(600));		// r
        fontWidths.put(new Integer(115), new Float(600));		// s
        fontWidths.put(new Integer(116), new Float(600));		// t
        fontWidths.put(new Integer(117), new Float(600));		// u
        fontWidths.put(new Integer(118), new Float(600));		// v
        fontWidths.put(new Integer(119), new Float(600));		// w
        fontWidths.put(new Integer(120), new Float(600));		// x
        fontWidths.put(new Integer(121), new Float(600));		// y
        fontWidths.put(new Integer(122), new Float(600));		// z
        fontWidths.put(new Integer(123), new Float(600));		// braceleft
        fontWidths.put(new Integer(124), new Float(600));		// bar
        fontWidths.put(new Integer(125), new Float(600));		// braceright
        fontWidths.put(new Integer(126), new Float(600));		// asciitilde
        fontWidths.put(new Integer(161), new Float(600));		// exclamdown
        fontWidths.put(new Integer(162), new Float(600));		// cent
        fontWidths.put(new Integer(163), new Float(600));		// sterling
        fontWidths.put(new Integer(164), new Float(600));		// fraction
        fontWidths.put(new Integer(165), new Float(600));		// yen
        fontWidths.put(new Integer(166), new Float(600));		// florin
        fontWidths.put(new Integer(167), new Float(600));		// section
        fontWidths.put(new Integer(168), new Float(600));		// currency
        fontWidths.put(new Integer(169), new Float(600));		// quotesingle
        fontWidths.put(new Integer(170), new Float(600));		// quotedblleft
        fontWidths.put(new Integer(171), new Float(600));		// guillemotleft
        fontWidths.put(new Integer(172), new Float(600));		// guilsinglleft
        fontWidths.put(new Integer(173), new Float(600));		// guilsinglright
        fontWidths.put(new Integer(174), new Float(600));		// fi
        fontWidths.put(new Integer(175), new Float(600));		// fl
        fontWidths.put(new Integer(177), new Float(600));		// endash
        fontWidths.put(new Integer(178), new Float(600));		// dagger
        fontWidths.put(new Integer(179), new Float(600));		// daggerdbl
        fontWidths.put(new Integer(180), new Float(600));		// periodcentered
        fontWidths.put(new Integer(182), new Float(600));		// paragraph
        fontWidths.put(new Integer(183), new Float(600));		// bullet
        fontWidths.put(new Integer(184), new Float(600));		// quotesinglbase
        fontWidths.put(new Integer(185), new Float(600));		// quotedblbase
        fontWidths.put(new Integer(186), new Float(600));		// quotedblright
        fontWidths.put(new Integer(187), new Float(600));		// guillemotright
        fontWidths.put(new Integer(188), new Float(600));		// ellipsis
        fontWidths.put(new Integer(189), new Float(600));		// perthousand
        fontWidths.put(new Integer(191), new Float(600));		// questiondown
        fontWidths.put(new Integer(193), new Float(600));		// grave
        fontWidths.put(new Integer(194), new Float(600));		// acute
        fontWidths.put(new Integer(195), new Float(600));		// circumflex
        fontWidths.put(new Integer(196), new Float(600));		// tilde
        fontWidths.put(new Integer(197), new Float(600));		// macron
        fontWidths.put(new Integer(198), new Float(600));		// breve
        fontWidths.put(new Integer(199), new Float(600));		// dotaccent
        fontWidths.put(new Integer(200), new Float(600));		// dieresis
        fontWidths.put(new Integer(202), new Float(600));		// ring
        fontWidths.put(new Integer(203), new Float(600));		// cedilla
        fontWidths.put(new Integer(205), new Float(600));		// hungarumlaut
        fontWidths.put(new Integer(206), new Float(600));		// ogonek
        fontWidths.put(new Integer(207), new Float(600));		// caron
        fontWidths.put(new Integer(208), new Float(600));		// emdash
        fontWidths.put(new Integer(225), new Float(600));		// AE
        fontWidths.put(new Integer(227), new Float(600));		// ordfeminine
        fontWidths.put(new Integer(232), new Float(600));		// Lslash
        fontWidths.put(new Integer(233), new Float(600));		// Oslash
        fontWidths.put(new Integer(234), new Float(600));		// OE
        fontWidths.put(new Integer(235), new Float(600));		// ordmasculine
        fontWidths.put(new Integer(241), new Float(600));		// ae
        fontWidths.put(new Integer(245), new Float(600));		// dotlessi
        fontWidths.put(new Integer(248), new Float(600));		// lslash
        fontWidths.put(new Integer(249), new Float(600));		// oslash
        fontWidths.put(new Integer(250), new Float(600));		// oe
        fontWidths.put(new Integer(251), new Float(600));		// germandbls

    }
  
}
