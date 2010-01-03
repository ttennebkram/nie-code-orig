
package nie.filters.pdf.font;

import java.util.*;

/**
 * Description of the standard font Times-Bold
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 */
public class PDFFontTimesBold extends PDFFontDesc {

    public PDFFontTimesBold() {
        fontName = "Times-Bold";
        baseEncoding = "MacRomanEncoding";
        isStandardFont = true;
        
        fontDescriptor = new Hashtable();
        fontDescriptor.put("Ascent", new Float(683));
        fontDescriptor.put("CapHeight", new Float(676));
        fontDescriptor.put("Descent", new Float(-217));
        fontDescriptor.put("ItalicAngle", new Float(0));
        fontDescriptor.put("AvgWidth", new Float(250));
        fontDescriptor.put("MissingWidth", new Float(250));
        fontDescriptor.put("Weight", new Float(2));

        fontWidths = new Hashtable();
        
        fontWidths.put(new Integer(32), new Float(250));		// space
        fontWidths.put(new Integer(33), new Float(333));		// exclam
        fontWidths.put(new Integer(34), new Float(555));		// quotedbl
        fontWidths.put(new Integer(35), new Float(500));		// numbersign
        fontWidths.put(new Integer(36), new Float(500));		// dollar
        fontWidths.put(new Integer(37), new Float(1000));		// percent
        fontWidths.put(new Integer(38), new Float(833));		// ampersand
        fontWidths.put(new Integer(39), new Float(333));		// quoteright
        fontWidths.put(new Integer(40), new Float(333));		// parenleft
        fontWidths.put(new Integer(41), new Float(333));		// parenright
        fontWidths.put(new Integer(42), new Float(500));		// asterisk
        fontWidths.put(new Integer(43), new Float(570));		// plus
        fontWidths.put(new Integer(44), new Float(250));		// comma
        fontWidths.put(new Integer(45), new Float(333));		// hyphen
        fontWidths.put(new Integer(46), new Float(250));		// period
        fontWidths.put(new Integer(47), new Float(278));		// slash
        fontWidths.put(new Integer(48), new Float(500));		// zero
        fontWidths.put(new Integer(49), new Float(500));		// one
        fontWidths.put(new Integer(50), new Float(500));		// two
        fontWidths.put(new Integer(51), new Float(500));		// three
        fontWidths.put(new Integer(52), new Float(500));		// four
        fontWidths.put(new Integer(53), new Float(500));		// five
        fontWidths.put(new Integer(54), new Float(500));		// six
        fontWidths.put(new Integer(55), new Float(500));		// seven
        fontWidths.put(new Integer(56), new Float(500));		// eight
        fontWidths.put(new Integer(57), new Float(500));		// nine
        fontWidths.put(new Integer(58), new Float(333));		// colon
        fontWidths.put(new Integer(59), new Float(333));		// semicolon
        fontWidths.put(new Integer(60), new Float(570));		// less
        fontWidths.put(new Integer(61), new Float(570));		// equal
        fontWidths.put(new Integer(62), new Float(570));		// greater
        fontWidths.put(new Integer(63), new Float(500));		// question
        fontWidths.put(new Integer(64), new Float(930));		// at
        fontWidths.put(new Integer(65), new Float(722));		// A
        fontWidths.put(new Integer(66), new Float(667));		// B
        fontWidths.put(new Integer(67), new Float(722));		// C
        fontWidths.put(new Integer(68), new Float(722));		// D
        fontWidths.put(new Integer(69), new Float(667));		// E
        fontWidths.put(new Integer(70), new Float(611));		// F
        fontWidths.put(new Integer(71), new Float(778));		// G
        fontWidths.put(new Integer(72), new Float(778));		// H
        fontWidths.put(new Integer(73), new Float(389));		// I
        fontWidths.put(new Integer(74), new Float(500));		// J
        fontWidths.put(new Integer(75), new Float(778));		// K
        fontWidths.put(new Integer(76), new Float(667));		// L
        fontWidths.put(new Integer(77), new Float(944));		// M
        fontWidths.put(new Integer(78), new Float(722));		// N
        fontWidths.put(new Integer(79), new Float(778));		// O
        fontWidths.put(new Integer(80), new Float(611));		// P
        fontWidths.put(new Integer(81), new Float(778));		// Q
        fontWidths.put(new Integer(82), new Float(722));		// R
        fontWidths.put(new Integer(83), new Float(556));		// S
        fontWidths.put(new Integer(84), new Float(667));		// T
        fontWidths.put(new Integer(85), new Float(722));		// U
        fontWidths.put(new Integer(86), new Float(722));		// V
        fontWidths.put(new Integer(87), new Float(1000));		// W
        fontWidths.put(new Integer(88), new Float(722));		// X
        fontWidths.put(new Integer(89), new Float(722));		// Y
        fontWidths.put(new Integer(90), new Float(667));		// Z
        fontWidths.put(new Integer(91), new Float(333));		// bracketleft
        fontWidths.put(new Integer(92), new Float(278));		// backslash
        fontWidths.put(new Integer(93), new Float(333));		// bracketright
        fontWidths.put(new Integer(94), new Float(581));		// asciicircum
        fontWidths.put(new Integer(95), new Float(500));		// underscore
        fontWidths.put(new Integer(96), new Float(333));		// quoteleft
        fontWidths.put(new Integer(97), new Float(500));		// a
        fontWidths.put(new Integer(98), new Float(556));		// b
        fontWidths.put(new Integer(99), new Float(444));		// c
        fontWidths.put(new Integer(100), new Float(556));		// d
        fontWidths.put(new Integer(101), new Float(444));		// e
        fontWidths.put(new Integer(102), new Float(333));		// f
        fontWidths.put(new Integer(103), new Float(500));		// g
        fontWidths.put(new Integer(104), new Float(556));		// h
        fontWidths.put(new Integer(105), new Float(278));		// i
        fontWidths.put(new Integer(106), new Float(333));		// j
        fontWidths.put(new Integer(107), new Float(556));		// k
        fontWidths.put(new Integer(108), new Float(278));		// l
        fontWidths.put(new Integer(109), new Float(833));		// m
        fontWidths.put(new Integer(110), new Float(556));		// n
        fontWidths.put(new Integer(111), new Float(500));		// o
        fontWidths.put(new Integer(112), new Float(556));		// p
        fontWidths.put(new Integer(113), new Float(556));		// q
        fontWidths.put(new Integer(114), new Float(444));		// r
        fontWidths.put(new Integer(115), new Float(389));		// s
        fontWidths.put(new Integer(116), new Float(333));		// t
        fontWidths.put(new Integer(117), new Float(556));		// u
        fontWidths.put(new Integer(118), new Float(500));		// v
        fontWidths.put(new Integer(119), new Float(722));		// w
        fontWidths.put(new Integer(120), new Float(500));		// x
        fontWidths.put(new Integer(121), new Float(500));		// y
        fontWidths.put(new Integer(122), new Float(444));		// z
        fontWidths.put(new Integer(123), new Float(394));		// braceleft
        fontWidths.put(new Integer(124), new Float(220));		// bar
        fontWidths.put(new Integer(125), new Float(394));		// braceright
        fontWidths.put(new Integer(126), new Float(520));		// asciitilde
        fontWidths.put(new Integer(161), new Float(333));		// exclamdown
        fontWidths.put(new Integer(162), new Float(500));		// cent
        fontWidths.put(new Integer(163), new Float(500));		// sterling
        fontWidths.put(new Integer(164), new Float(167));		// fraction
        fontWidths.put(new Integer(165), new Float(500));		// yen
        fontWidths.put(new Integer(166), new Float(500));		// florin
        fontWidths.put(new Integer(167), new Float(500));		// section
        fontWidths.put(new Integer(168), new Float(500));		// currency
        fontWidths.put(new Integer(169), new Float(278));		// quotesingle
        fontWidths.put(new Integer(170), new Float(500));		// quotedblleft
        fontWidths.put(new Integer(171), new Float(500));		// guillemotleft
        fontWidths.put(new Integer(172), new Float(333));		// guilsinglleft
        fontWidths.put(new Integer(173), new Float(333));		// guilsinglright
        fontWidths.put(new Integer(174), new Float(556));		// fi
        fontWidths.put(new Integer(175), new Float(556));		// fl
        fontWidths.put(new Integer(177), new Float(500));		// endash
        fontWidths.put(new Integer(178), new Float(500));		// dagger
        fontWidths.put(new Integer(179), new Float(500));		// daggerdbl
        fontWidths.put(new Integer(180), new Float(250));		// periodcentered
        fontWidths.put(new Integer(182), new Float(540));		// paragraph
        fontWidths.put(new Integer(183), new Float(350));		// bullet
        fontWidths.put(new Integer(184), new Float(333));		// quotesinglbase
        fontWidths.put(new Integer(185), new Float(500));		// quotedblbase
        fontWidths.put(new Integer(186), new Float(500));		// quotedblright
        fontWidths.put(new Integer(187), new Float(500));		// guillemotright
        fontWidths.put(new Integer(188), new Float(1000));		// ellipsis
        fontWidths.put(new Integer(189), new Float(1000));		// perthousand
        fontWidths.put(new Integer(191), new Float(500));		// questiondown
        fontWidths.put(new Integer(193), new Float(333));		// grave
        fontWidths.put(new Integer(194), new Float(333));		// acute
        fontWidths.put(new Integer(195), new Float(333));		// circumflex
        fontWidths.put(new Integer(196), new Float(333));		// tilde
        fontWidths.put(new Integer(197), new Float(333));		// macron
        fontWidths.put(new Integer(198), new Float(333));		// breve
        fontWidths.put(new Integer(199), new Float(333));		// dotaccent
        fontWidths.put(new Integer(200), new Float(333));		// dieresis
        fontWidths.put(new Integer(202), new Float(333));		// ring
        fontWidths.put(new Integer(203), new Float(333));		// cedilla
        fontWidths.put(new Integer(205), new Float(333));		// hungarumlaut
        fontWidths.put(new Integer(206), new Float(333));		// ogonek
        fontWidths.put(new Integer(207), new Float(333));		// caron
        fontWidths.put(new Integer(208), new Float(1000));		// emdash
        fontWidths.put(new Integer(225), new Float(1000));		// AE
        fontWidths.put(new Integer(227), new Float(300));		// ordfeminine
        fontWidths.put(new Integer(232), new Float(667));		// Lslash
        fontWidths.put(new Integer(233), new Float(778));		// Oslash
        fontWidths.put(new Integer(234), new Float(1000));		// OE
        fontWidths.put(new Integer(235), new Float(330));		// ordmasculine
        fontWidths.put(new Integer(241), new Float(722));		// ae
        fontWidths.put(new Integer(245), new Float(278));		// dotlessi
        fontWidths.put(new Integer(248), new Float(278));		// lslash
        fontWidths.put(new Integer(249), new Float(500));		// oslash
        fontWidths.put(new Integer(250), new Float(722));		// oe
        fontWidths.put(new Integer(251), new Float(556));		// germandbls

    }
  
}
