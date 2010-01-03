
package nie.filters.pdf.font;

import java.util.*;

/**
 * Description of the standard font Symbol
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 */
public class PDFFontSymbol extends PDFFontDesc {

    public PDFFontSymbol() {
        fontName = "Symbol";
        baseEncoding = "WinAnsiEncoding";
        isStandardFont = true;
        
        fontDescriptor = new Hashtable();
        fontDescriptor.put("Ascent", new Float(0));
        fontDescriptor.put("CapHeight", new Float(0));
        fontDescriptor.put("Descent", new Float(0));
        fontDescriptor.put("ItalicAngle", new Float(0));
        fontDescriptor.put("AvgWidth", new Float(92));
        fontDescriptor.put("MissingWidth", new Float(92));
        fontDescriptor.put("Weight", new Float(1));

        fontWidths = new Hashtable();
        
        fontWidths.put(new Integer(32), new Float(250));		// space
        fontWidths.put(new Integer(33), new Float(333));		// exclam
        fontWidths.put(new Integer(34), new Float(713));		// universal
        fontWidths.put(new Integer(35), new Float(500));		// numbersign
        fontWidths.put(new Integer(36), new Float(549));		// existential
        fontWidths.put(new Integer(37), new Float(833));		// percent
        fontWidths.put(new Integer(38), new Float(778));		// ampersand
        fontWidths.put(new Integer(39), new Float(439));		// suchthat
        fontWidths.put(new Integer(40), new Float(333));		// parenleft
        fontWidths.put(new Integer(41), new Float(333));		// parenright
        fontWidths.put(new Integer(42), new Float(500));		// asteriskmath
        fontWidths.put(new Integer(43), new Float(549));		// plus
        fontWidths.put(new Integer(44), new Float(250));		// comma
        fontWidths.put(new Integer(45), new Float(549));		// minus
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
        fontWidths.put(new Integer(58), new Float(278));		// colon
        fontWidths.put(new Integer(59), new Float(278));		// semicolon
        fontWidths.put(new Integer(60), new Float(549));		// less
        fontWidths.put(new Integer(61), new Float(549));		// equal
        fontWidths.put(new Integer(62), new Float(549));		// greater
        fontWidths.put(new Integer(63), new Float(444));		// question
        fontWidths.put(new Integer(64), new Float(549));		// congruent
        fontWidths.put(new Integer(65), new Float(722));		// Alpha
        fontWidths.put(new Integer(66), new Float(667));		// Beta
        fontWidths.put(new Integer(67), new Float(722));		// Chi
        fontWidths.put(new Integer(68), new Float(612));		// Delta
        fontWidths.put(new Integer(69), new Float(611));		// Epsilon
        fontWidths.put(new Integer(70), new Float(763));		// Phi
        fontWidths.put(new Integer(71), new Float(603));		// Gamma
        fontWidths.put(new Integer(72), new Float(722));		// Eta
        fontWidths.put(new Integer(73), new Float(333));		// Iota
        fontWidths.put(new Integer(74), new Float(631));		// theta1
        fontWidths.put(new Integer(75), new Float(722));		// Kappa
        fontWidths.put(new Integer(76), new Float(686));		// Lambda
        fontWidths.put(new Integer(77), new Float(889));		// Mu
        fontWidths.put(new Integer(78), new Float(722));		// Nu
        fontWidths.put(new Integer(79), new Float(722));		// Omicron
        fontWidths.put(new Integer(80), new Float(768));		// Pi
        fontWidths.put(new Integer(81), new Float(741));		// Theta
        fontWidths.put(new Integer(82), new Float(556));		// Rho
        fontWidths.put(new Integer(83), new Float(592));		// Sigma
        fontWidths.put(new Integer(84), new Float(611));		// Tau
        fontWidths.put(new Integer(85), new Float(690));		// Upsilon
        fontWidths.put(new Integer(86), new Float(439));		// sigma1
        fontWidths.put(new Integer(87), new Float(768));		// Omega
        fontWidths.put(new Integer(88), new Float(645));		// Xi
        fontWidths.put(new Integer(89), new Float(795));		// Psi
        fontWidths.put(new Integer(90), new Float(611));		// Zeta
        fontWidths.put(new Integer(91), new Float(333));		// bracketleft
        fontWidths.put(new Integer(92), new Float(863));		// therefore
        fontWidths.put(new Integer(93), new Float(333));		// bracketright
        fontWidths.put(new Integer(94), new Float(658));		// perpendicular
        fontWidths.put(new Integer(95), new Float(500));		// underscore
        fontWidths.put(new Integer(96), new Float(500));		// radicalex
        fontWidths.put(new Integer(97), new Float(631));		// alpha
        fontWidths.put(new Integer(98), new Float(549));		// beta
        fontWidths.put(new Integer(99), new Float(549));		// chi
        fontWidths.put(new Integer(100), new Float(494));		// delta
        fontWidths.put(new Integer(101), new Float(439));		// epsilon
        fontWidths.put(new Integer(102), new Float(521));		// phi
        fontWidths.put(new Integer(103), new Float(411));		// gamma
        fontWidths.put(new Integer(104), new Float(603));		// eta
        fontWidths.put(new Integer(105), new Float(329));		// iota
        fontWidths.put(new Integer(106), new Float(603));		// phi1
        fontWidths.put(new Integer(107), new Float(549));		// kappa
        fontWidths.put(new Integer(108), new Float(549));		// lambda
        fontWidths.put(new Integer(109), new Float(576));		// mu
        fontWidths.put(new Integer(110), new Float(521));		// nu
        fontWidths.put(new Integer(111), new Float(549));		// omicron
        fontWidths.put(new Integer(112), new Float(549));		// pi
        fontWidths.put(new Integer(113), new Float(521));		// theta
        fontWidths.put(new Integer(114), new Float(549));		// rho
        fontWidths.put(new Integer(115), new Float(603));		// sigma
        fontWidths.put(new Integer(116), new Float(439));		// tau
        fontWidths.put(new Integer(117), new Float(576));		// upsilon
        fontWidths.put(new Integer(118), new Float(713));		// omega1
        fontWidths.put(new Integer(119), new Float(686));		// omega
        fontWidths.put(new Integer(120), new Float(493));		// xi
        fontWidths.put(new Integer(121), new Float(686));		// psi
        fontWidths.put(new Integer(122), new Float(494));		// zeta
        fontWidths.put(new Integer(123), new Float(480));		// braceleft
        fontWidths.put(new Integer(124), new Float(200));		// bar
        fontWidths.put(new Integer(125), new Float(480));		// braceright
        fontWidths.put(new Integer(126), new Float(549));		// similar
        fontWidths.put(new Integer(160), new Float(750));		// Euro
        fontWidths.put(new Integer(161), new Float(620));		// Upsilon1
        fontWidths.put(new Integer(162), new Float(247));		// minute
        fontWidths.put(new Integer(163), new Float(549));		// lessequal
        fontWidths.put(new Integer(164), new Float(167));		// fraction
        fontWidths.put(new Integer(165), new Float(713));		// infinity
        fontWidths.put(new Integer(166), new Float(500));		// florin
        fontWidths.put(new Integer(167), new Float(753));		// club
        fontWidths.put(new Integer(168), new Float(753));		// diamond
        fontWidths.put(new Integer(169), new Float(753));		// heart
        fontWidths.put(new Integer(170), new Float(753));		// spade
        fontWidths.put(new Integer(171), new Float(1042));		// arrowboth
        fontWidths.put(new Integer(172), new Float(987));		// arrowleft
        fontWidths.put(new Integer(173), new Float(603));		// arrowup
        fontWidths.put(new Integer(174), new Float(987));		// arrowright
        fontWidths.put(new Integer(175), new Float(603));		// arrowdown
        fontWidths.put(new Integer(176), new Float(400));		// degree
        fontWidths.put(new Integer(177), new Float(549));		// plusminus
        fontWidths.put(new Integer(178), new Float(411));		// second
        fontWidths.put(new Integer(179), new Float(549));		// greaterequal
        fontWidths.put(new Integer(180), new Float(549));		// multiply
        fontWidths.put(new Integer(181), new Float(713));		// proportional
        fontWidths.put(new Integer(182), new Float(494));		// partialdiff
        fontWidths.put(new Integer(183), new Float(460));		// bullet
        fontWidths.put(new Integer(184), new Float(549));		// divide
        fontWidths.put(new Integer(185), new Float(549));		// notequal
        fontWidths.put(new Integer(186), new Float(549));		// equivalence
        fontWidths.put(new Integer(187), new Float(549));		// approxequal
        fontWidths.put(new Integer(188), new Float(1000));		// ellipsis
        fontWidths.put(new Integer(189), new Float(603));		// arrowvertex
        fontWidths.put(new Integer(190), new Float(1000));		// arrowhorizex
        fontWidths.put(new Integer(191), new Float(658));		// carriagereturn
        fontWidths.put(new Integer(192), new Float(823));		// aleph
        fontWidths.put(new Integer(193), new Float(686));		// Ifraktur
        fontWidths.put(new Integer(194), new Float(795));		// Rfraktur
        fontWidths.put(new Integer(195), new Float(987));		// weierstrass
        fontWidths.put(new Integer(196), new Float(768));		// circlemultiply
        fontWidths.put(new Integer(197), new Float(768));		// circleplus
        fontWidths.put(new Integer(198), new Float(823));		// emptyset
        fontWidths.put(new Integer(199), new Float(768));		// intersection
        fontWidths.put(new Integer(200), new Float(768));		// union
        fontWidths.put(new Integer(201), new Float(713));		// propersuperset
        fontWidths.put(new Integer(202), new Float(713));		// reflexsuperset
        fontWidths.put(new Integer(203), new Float(713));		// notsubset
        fontWidths.put(new Integer(204), new Float(713));		// propersubset
        fontWidths.put(new Integer(205), new Float(713));		// reflexsubset
        fontWidths.put(new Integer(206), new Float(713));		// element
        fontWidths.put(new Integer(207), new Float(713));		// notelement
        fontWidths.put(new Integer(208), new Float(768));		// angle
        fontWidths.put(new Integer(209), new Float(713));		// gradient
        fontWidths.put(new Integer(210), new Float(790));		// registerserif
        fontWidths.put(new Integer(211), new Float(790));		// copyrightserif
        fontWidths.put(new Integer(212), new Float(890));		// trademarkserif
        fontWidths.put(new Integer(213), new Float(823));		// product
        fontWidths.put(new Integer(214), new Float(549));		// radical
        fontWidths.put(new Integer(215), new Float(250));		// dotmath
        fontWidths.put(new Integer(216), new Float(713));		// logicalnot
        fontWidths.put(new Integer(217), new Float(603));		// logicaland
        fontWidths.put(new Integer(218), new Float(603));		// logicalor
        fontWidths.put(new Integer(219), new Float(1042));		// arrowdblboth
        fontWidths.put(new Integer(220), new Float(987));		// arrowdblleft
        fontWidths.put(new Integer(221), new Float(603));		// arrowdblup
        fontWidths.put(new Integer(222), new Float(987));		// arrowdblright
        fontWidths.put(new Integer(223), new Float(603));		// arrowdbldown
        fontWidths.put(new Integer(224), new Float(494));		// lozenge
        fontWidths.put(new Integer(225), new Float(329));		// angleleft
        fontWidths.put(new Integer(226), new Float(790));		// registersans
        fontWidths.put(new Integer(227), new Float(790));		// copyrightsans
        fontWidths.put(new Integer(228), new Float(786));		// trademarksans
        fontWidths.put(new Integer(229), new Float(713));		// summation
        fontWidths.put(new Integer(230), new Float(384));		// parenlefttp
        fontWidths.put(new Integer(231), new Float(384));		// parenleftex
        fontWidths.put(new Integer(232), new Float(384));		// parenleftbt
        fontWidths.put(new Integer(233), new Float(384));		// bracketlefttp
        fontWidths.put(new Integer(234), new Float(384));		// bracketleftex
        fontWidths.put(new Integer(235), new Float(384));		// bracketleftbt
        fontWidths.put(new Integer(236), new Float(494));		// bracelefttp
        fontWidths.put(new Integer(237), new Float(494));		// braceleftmid
        fontWidths.put(new Integer(238), new Float(494));		// braceleftbt
        fontWidths.put(new Integer(239), new Float(494));		// braceex
        fontWidths.put(new Integer(241), new Float(329));		// angleright
        fontWidths.put(new Integer(242), new Float(274));		// integral
        fontWidths.put(new Integer(243), new Float(686));		// integraltp
        fontWidths.put(new Integer(244), new Float(686));		// integralex
        fontWidths.put(new Integer(245), new Float(686));		// integralbt
        fontWidths.put(new Integer(246), new Float(384));		// parenrighttp
        fontWidths.put(new Integer(247), new Float(384));		// parenrightex
        fontWidths.put(new Integer(248), new Float(384));		// parenrightbt
        fontWidths.put(new Integer(249), new Float(384));		// bracketrighttp
        fontWidths.put(new Integer(250), new Float(384));		// bracketrightex
        fontWidths.put(new Integer(251), new Float(384));		// bracketrightbt
        fontWidths.put(new Integer(252), new Float(494));		// bracerighttp
        fontWidths.put(new Integer(253), new Float(494));		// bracerightmid
        fontWidths.put(new Integer(254), new Float(494));		// bracerightbt

    }
  
}
