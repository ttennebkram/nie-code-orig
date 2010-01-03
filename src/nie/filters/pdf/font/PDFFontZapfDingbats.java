
package nie.filters.pdf.font;

import java.util.*;

/**
 * Description of the standard font Zapf Dingbats
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 */
public class PDFFontZapfDingbats extends PDFFontDesc {

    public PDFFontZapfDingbats() {
        fontName = "ZapfDingbats";
        baseEncoding = "WinAnsiEncoding";
        isStandardFont = true;
        
        fontDescriptor = new Hashtable();
        fontDescriptor.put("Ascent", new Float(0));
        fontDescriptor.put("CapHeight", new Float(0));
        fontDescriptor.put("Descent", new Float(0));
        fontDescriptor.put("ItalicAngle", new Float(0));
        fontDescriptor.put("AvgWidth", new Float(28));
        fontDescriptor.put("MissingWidth", new Float(28));
        fontDescriptor.put("Weight", new Float(1));

        fontWidths = new Hashtable();
        
        fontWidths.put(new Integer(32), new Float(278));		// space
        fontWidths.put(new Integer(33), new Float(974));		// a1
        fontWidths.put(new Integer(34), new Float(961));		// a2
        fontWidths.put(new Integer(35), new Float(974));		// a202
        fontWidths.put(new Integer(36), new Float(980));		// a3
        fontWidths.put(new Integer(37), new Float(719));		// a4
        fontWidths.put(new Integer(38), new Float(789));		// a5
        fontWidths.put(new Integer(39), new Float(790));		// a119
        fontWidths.put(new Integer(40), new Float(791));		// a118
        fontWidths.put(new Integer(41), new Float(690));		// a117
        fontWidths.put(new Integer(42), new Float(960));		// a11
        fontWidths.put(new Integer(43), new Float(939));		// a12
        fontWidths.put(new Integer(44), new Float(549));		// a13
        fontWidths.put(new Integer(45), new Float(855));		// a14
        fontWidths.put(new Integer(46), new Float(911));		// a15
        fontWidths.put(new Integer(47), new Float(933));		// a16
        fontWidths.put(new Integer(48), new Float(911));		// a105
        fontWidths.put(new Integer(49), new Float(945));		// a17
        fontWidths.put(new Integer(50), new Float(974));		// a18
        fontWidths.put(new Integer(51), new Float(755));		// a19
        fontWidths.put(new Integer(52), new Float(846));		// a20
        fontWidths.put(new Integer(53), new Float(762));		// a21
        fontWidths.put(new Integer(54), new Float(761));		// a22
        fontWidths.put(new Integer(55), new Float(571));		// a23
        fontWidths.put(new Integer(56), new Float(677));		// a24
        fontWidths.put(new Integer(57), new Float(763));		// a25
        fontWidths.put(new Integer(58), new Float(760));		// a26
        fontWidths.put(new Integer(59), new Float(759));		// a27
        fontWidths.put(new Integer(60), new Float(754));		// a28
        fontWidths.put(new Integer(61), new Float(494));		// a6
        fontWidths.put(new Integer(62), new Float(552));		// a7
        fontWidths.put(new Integer(63), new Float(537));		// a8
        fontWidths.put(new Integer(64), new Float(577));		// a9
        fontWidths.put(new Integer(65), new Float(692));		// a10
        fontWidths.put(new Integer(66), new Float(786));		// a29
        fontWidths.put(new Integer(67), new Float(788));		// a30
        fontWidths.put(new Integer(68), new Float(788));		// a31
        fontWidths.put(new Integer(69), new Float(790));		// a32
        fontWidths.put(new Integer(70), new Float(793));		// a33
        fontWidths.put(new Integer(71), new Float(794));		// a34
        fontWidths.put(new Integer(72), new Float(816));		// a35
        fontWidths.put(new Integer(73), new Float(823));		// a36
        fontWidths.put(new Integer(74), new Float(789));		// a37
        fontWidths.put(new Integer(75), new Float(841));		// a38
        fontWidths.put(new Integer(76), new Float(823));		// a39
        fontWidths.put(new Integer(77), new Float(833));		// a40
        fontWidths.put(new Integer(78), new Float(816));		// a41
        fontWidths.put(new Integer(79), new Float(831));		// a42
        fontWidths.put(new Integer(80), new Float(923));		// a43
        fontWidths.put(new Integer(81), new Float(744));		// a44
        fontWidths.put(new Integer(82), new Float(723));		// a45
        fontWidths.put(new Integer(83), new Float(749));		// a46
        fontWidths.put(new Integer(84), new Float(790));		// a47
        fontWidths.put(new Integer(85), new Float(792));		// a48
        fontWidths.put(new Integer(86), new Float(695));		// a49
        fontWidths.put(new Integer(87), new Float(776));		// a50
        fontWidths.put(new Integer(88), new Float(768));		// a51
        fontWidths.put(new Integer(89), new Float(792));		// a52
        fontWidths.put(new Integer(90), new Float(759));		// a53
        fontWidths.put(new Integer(91), new Float(707));		// a54
        fontWidths.put(new Integer(92), new Float(708));		// a55
        fontWidths.put(new Integer(93), new Float(682));		// a56
        fontWidths.put(new Integer(94), new Float(701));		// a57
        fontWidths.put(new Integer(95), new Float(826));		// a58
        fontWidths.put(new Integer(96), new Float(815));		// a59
        fontWidths.put(new Integer(97), new Float(789));		// a60
        fontWidths.put(new Integer(98), new Float(789));		// a61
        fontWidths.put(new Integer(99), new Float(707));		// a62
        fontWidths.put(new Integer(100), new Float(687));		// a63
        fontWidths.put(new Integer(101), new Float(696));		// a64
        fontWidths.put(new Integer(102), new Float(689));		// a65
        fontWidths.put(new Integer(103), new Float(786));		// a66
        fontWidths.put(new Integer(104), new Float(787));		// a67
        fontWidths.put(new Integer(105), new Float(713));		// a68
        fontWidths.put(new Integer(106), new Float(791));		// a69
        fontWidths.put(new Integer(107), new Float(785));		// a70
        fontWidths.put(new Integer(108), new Float(791));		// a71
        fontWidths.put(new Integer(109), new Float(873));		// a72
        fontWidths.put(new Integer(110), new Float(761));		// a73
        fontWidths.put(new Integer(111), new Float(762));		// a74
        fontWidths.put(new Integer(112), new Float(762));		// a203
        fontWidths.put(new Integer(113), new Float(759));		// a75
        fontWidths.put(new Integer(114), new Float(759));		// a204
        fontWidths.put(new Integer(115), new Float(892));		// a76
        fontWidths.put(new Integer(116), new Float(892));		// a77
        fontWidths.put(new Integer(117), new Float(788));		// a78
        fontWidths.put(new Integer(118), new Float(784));		// a79
        fontWidths.put(new Integer(119), new Float(438));		// a81
        fontWidths.put(new Integer(120), new Float(138));		// a82
        fontWidths.put(new Integer(121), new Float(277));		// a83
        fontWidths.put(new Integer(122), new Float(415));		// a84
        fontWidths.put(new Integer(123), new Float(392));		// a97
        fontWidths.put(new Integer(124), new Float(392));		// a98
        fontWidths.put(new Integer(125), new Float(668));		// a99
        fontWidths.put(new Integer(126), new Float(668));		// a100
        fontWidths.put(new Integer(128), new Float(390));		// a89
        fontWidths.put(new Integer(129), new Float(390));		// a90
        fontWidths.put(new Integer(130), new Float(317));		// a93
        fontWidths.put(new Integer(131), new Float(317));		// a94
        fontWidths.put(new Integer(132), new Float(276));		// a91
        fontWidths.put(new Integer(133), new Float(276));		// a92
        fontWidths.put(new Integer(134), new Float(509));		// a205
        fontWidths.put(new Integer(135), new Float(509));		// a85
        fontWidths.put(new Integer(136), new Float(410));		// a206
        fontWidths.put(new Integer(137), new Float(410));		// a86
        fontWidths.put(new Integer(138), new Float(234));		// a87
        fontWidths.put(new Integer(139), new Float(234));		// a88
        fontWidths.put(new Integer(140), new Float(334));		// a95
        fontWidths.put(new Integer(141), new Float(334));		// a96
        fontWidths.put(new Integer(161), new Float(732));		// a101
        fontWidths.put(new Integer(162), new Float(544));		// a102
        fontWidths.put(new Integer(163), new Float(544));		// a103
        fontWidths.put(new Integer(164), new Float(910));		// a104
        fontWidths.put(new Integer(165), new Float(667));		// a106
        fontWidths.put(new Integer(166), new Float(760));		// a107
        fontWidths.put(new Integer(167), new Float(760));		// a108
        fontWidths.put(new Integer(168), new Float(776));		// a112
        fontWidths.put(new Integer(169), new Float(595));		// a111
        fontWidths.put(new Integer(170), new Float(694));		// a110
        fontWidths.put(new Integer(171), new Float(626));		// a109
        fontWidths.put(new Integer(172), new Float(788));		// a120
        fontWidths.put(new Integer(173), new Float(788));		// a121
        fontWidths.put(new Integer(174), new Float(788));		// a122
        fontWidths.put(new Integer(175), new Float(788));		// a123
        fontWidths.put(new Integer(176), new Float(788));		// a124
        fontWidths.put(new Integer(177), new Float(788));		// a125
        fontWidths.put(new Integer(178), new Float(788));		// a126
        fontWidths.put(new Integer(179), new Float(788));		// a127
        fontWidths.put(new Integer(180), new Float(788));		// a128
        fontWidths.put(new Integer(181), new Float(788));		// a129
        fontWidths.put(new Integer(182), new Float(788));		// a130
        fontWidths.put(new Integer(183), new Float(788));		// a131
        fontWidths.put(new Integer(184), new Float(788));		// a132
        fontWidths.put(new Integer(185), new Float(788));		// a133
        fontWidths.put(new Integer(186), new Float(788));		// a134
        fontWidths.put(new Integer(187), new Float(788));		// a135
        fontWidths.put(new Integer(188), new Float(788));		// a136
        fontWidths.put(new Integer(189), new Float(788));		// a137
        fontWidths.put(new Integer(190), new Float(788));		// a138
        fontWidths.put(new Integer(191), new Float(788));		// a139
        fontWidths.put(new Integer(192), new Float(788));		// a140
        fontWidths.put(new Integer(193), new Float(788));		// a141
        fontWidths.put(new Integer(194), new Float(788));		// a142
        fontWidths.put(new Integer(195), new Float(788));		// a143
        fontWidths.put(new Integer(196), new Float(788));		// a144
        fontWidths.put(new Integer(197), new Float(788));		// a145
        fontWidths.put(new Integer(198), new Float(788));		// a146
        fontWidths.put(new Integer(199), new Float(788));		// a147
        fontWidths.put(new Integer(200), new Float(788));		// a148
        fontWidths.put(new Integer(201), new Float(788));		// a149
        fontWidths.put(new Integer(202), new Float(788));		// a150
        fontWidths.put(new Integer(203), new Float(788));		// a151
        fontWidths.put(new Integer(204), new Float(788));		// a152
        fontWidths.put(new Integer(205), new Float(788));		// a153
        fontWidths.put(new Integer(206), new Float(788));		// a154
        fontWidths.put(new Integer(207), new Float(788));		// a155
        fontWidths.put(new Integer(208), new Float(788));		// a156
        fontWidths.put(new Integer(209), new Float(788));		// a157
        fontWidths.put(new Integer(210), new Float(788));		// a158
        fontWidths.put(new Integer(211), new Float(788));		// a159
        fontWidths.put(new Integer(212), new Float(894));		// a160
        fontWidths.put(new Integer(213), new Float(838));		// a161
        fontWidths.put(new Integer(214), new Float(1016));		// a163
        fontWidths.put(new Integer(215), new Float(458));		// a164
        fontWidths.put(new Integer(216), new Float(748));		// a196
        fontWidths.put(new Integer(217), new Float(924));		// a165
        fontWidths.put(new Integer(218), new Float(748));		// a192
        fontWidths.put(new Integer(219), new Float(918));		// a166
        fontWidths.put(new Integer(220), new Float(927));		// a167
        fontWidths.put(new Integer(221), new Float(928));		// a168
        fontWidths.put(new Integer(222), new Float(928));		// a169
        fontWidths.put(new Integer(223), new Float(834));		// a170
        fontWidths.put(new Integer(224), new Float(873));		// a171
        fontWidths.put(new Integer(225), new Float(828));		// a172
        fontWidths.put(new Integer(226), new Float(924));		// a173
        fontWidths.put(new Integer(227), new Float(924));		// a162
        fontWidths.put(new Integer(228), new Float(917));		// a174
        fontWidths.put(new Integer(229), new Float(930));		// a175
        fontWidths.put(new Integer(230), new Float(931));		// a176
        fontWidths.put(new Integer(231), new Float(463));		// a177
        fontWidths.put(new Integer(232), new Float(883));		// a178
        fontWidths.put(new Integer(233), new Float(836));		// a179
        fontWidths.put(new Integer(234), new Float(836));		// a193
        fontWidths.put(new Integer(235), new Float(867));		// a180
        fontWidths.put(new Integer(236), new Float(867));		// a199
        fontWidths.put(new Integer(237), new Float(696));		// a181
        fontWidths.put(new Integer(238), new Float(696));		// a200
        fontWidths.put(new Integer(239), new Float(874));		// a182
        fontWidths.put(new Integer(241), new Float(874));		// a201
        fontWidths.put(new Integer(242), new Float(760));		// a183
        fontWidths.put(new Integer(243), new Float(946));		// a184
        fontWidths.put(new Integer(244), new Float(771));		// a197
        fontWidths.put(new Integer(245), new Float(865));		// a185
        fontWidths.put(new Integer(246), new Float(771));		// a194
        fontWidths.put(new Integer(247), new Float(888));		// a198
        fontWidths.put(new Integer(248), new Float(967));		// a186
        fontWidths.put(new Integer(249), new Float(888));		// a195
        fontWidths.put(new Integer(250), new Float(831));		// a187
        fontWidths.put(new Integer(251), new Float(873));		// a188
        fontWidths.put(new Integer(252), new Float(927));		// a189
        fontWidths.put(new Integer(253), new Float(970));		// a190
        fontWidths.put(new Integer(254), new Float(918));		// a191
    }
  
}
