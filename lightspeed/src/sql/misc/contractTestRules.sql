SET foreign_key_checks = on;

-- ===========  Includes ALL 2.9 (State) Rules and 3.0 TEST rules from DH's "rough" analysis  ============

Delete from contract_rule;


insert into contract_rule values(58,1,'58',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-STD-40',0,'','');
insert into contract_rule values(59,1,'59',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-NONE',0,'','no callback pay');
insert into contract_rule values(60,1,'60',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',0,'','no Gold');
insert into contract_rule values(61,1,'61',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-NONE',0,'','no guarantee');
insert into contract_rule values(62,1,'62',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"HO",'HO-NONE',0,'','no holiday pay');
insert into contract_rule values(63,1,'63',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"HL",'HL-NONE',0,'','no holidays');
insert into contract_rule values(64,1,'64',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',0,'','');
insert into contract_rule values(65,1,'65',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","TR","N_A",null,"MP",'MP-NONE',9999,'','');
insert into contract_rule values(66,1,'66',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"NP",'NP-NONE',0,'','no Night Premium');
insert into contract_rule values(67,1,'67',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-NONE',0,'','no overtime');
insert into contract_rule values(68,1,'68',"N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"RS",'RST-NONE',0,'','no rest invasion');





insert into contract_rule values(74,1,'74',"N_A","NonU","N_A","EX","N_A","N_A","N_A","N_A","N_A",null,"OC",'OC-57-S',60,'','');
insert into contract_rule values(75,1,'75',"N_A","NonU","N_A","EX","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',60,'','');
insert into contract_rule values(76,1,'76',"N_A","NonU","N_A","EX","N_A","N_A","N_A","N_A","N_A",null,"SP",'SP_NO_6TH_7TH_DAY',60,'"Pay 6th/7th" = FALSE','chk SF for no 6th/7th day pay');
insert into contract_rule values(77,1,'77',"N_A","NonU","N_A","EX","N_A","N_A","N_A","HA","N_A",null,"OC",'OC-NO-PAY',2060,'','');
insert into contract_rule values(78,1,'78',"N_A","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-STD-40',20,'','OT after 40');
insert into contract_rule values(79,1,'79',"N_A","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-USE-START',4999,'','use Start guar');
insert into contract_rule values(80,1,'80',"N_A","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-NONE',4999,'','no daily OT');
insert into contract_rule values(81,1,'81',"N_A","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',4999,'','no gold');
insert into contract_rule values(82,1,'82',"N_A","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',4999,'','no MPV');
insert into contract_rule values(83,1,'83',"N_A","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"HL",'HL-STD',4999,'','Std holidays');
insert into contract_rule values(84,1,'84',"N_A","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-NONE',4999,'','no callback pay');
insert into contract_rule values(85,1,'85',"N_A","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"RS",'RST-NONE',4999,'','no rest invasion');


insert into contract_rule values(88,1,'88',"AK","NonU","N_A","HR","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',5099,'','Daily OT after 8');
insert into contract_rule values(89,1,'89',"AK","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',5099,'','Daily OT after 8');


insert into contract_rule values(92,1,'92',"CA","NonU","N_A","HR","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',5099,'','Daily OT after 8');
insert into contract_rule values(93,1,'93',"CA","NonU","N_A","HR","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-WK-12-2X',5099,'','2x after 12');
insert into contract_rule values(94,1,'94',"CA","NonU","N_A","HR","N_A","N_A","N_A","N_A","7",null,"OT",'OT-0-15X',5099,'7th day paid at 1.5x','Start at 1.5x');
insert into contract_rule values(95,1,'95',"CA","NonU","N_A","HR","N_A","N_A","N_A","N_A","7",null,"GL",'GL-WK-8-2X',5099,'','2x after 8 hr');
insert into contract_rule values(96,1,'96',"CA","NonU","N_A","HR","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-CA',5099,'CA MPV rule','1 MPV if > 6 hr');
insert into contract_rule values(97,1,'97',"CA","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',5099,'','Daily OT after 8');
insert into contract_rule values(98,1,'98',"CA","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-WK-12-2X',5099,'','2x after 12');
insert into contract_rule values(99,1,'99',"CA","N_A","N_A","N_A","N_A","N_A","N_A","N_A","7",null,"OT",'OT-0-15X',5099,'7th day paid at 1.5x','Start at 1.5x');
insert into contract_rule values(100,1,'100',"CA","N_A","N_A","N_A","N_A","N_A","N_A","N_A","7",null,"GL",'GL-WK-8-2X',5099,'','2x after 8 hr');
insert into contract_rule values(101,1,'101',"CO","NonU","N_A","HR","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-12-15X',5099,'','Daily OT after 12');
insert into contract_rule values(102,1,'102',"CO","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-12-15X',5099,'','Daily OT after 12');











insert into contract_rule values(114,1,'114',"KY","NonU","N_A","HR","N_A","N_A","N_A","N_A","7",null,"OT",'OT-0-15X',5099,'7th day paid at 1.5x','Start at 1.5x');
insert into contract_rule values(115,1,'115',"KY","N_A","N_A","N_A","N_A","N_A","N_A","N_A","7",null,"OT",'OT-0-15X',5099,'7th day paid at 1.5x','Start at 1.5x');







































insert into contract_rule values(155,1,'155',"PR","NonU","N_A","HR","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',5099,'','Daily OT after 8');
insert into contract_rule values(156,1,'156',"PR","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',5099,'','Daily OT after 8');
insert into contract_rule values(157,1,'157',"VI","NonU","N_A","HR","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',5099,'','Daily OT after 8');
insert into contract_rule values(158,1,'158',"VI","NonU","N_A","HR","N_A","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',5099,'6th day paid at 1.5x','Start at 1.5x');
insert into contract_rule values(159,1,'159',"VI","NonU","N_A","HR","N_A","N_A","N_A","N_A","7",null,"OT",'OT-0-15X',5099,'7th day paid at 1.5x','Start at 1.5x');
insert into contract_rule values(160,1,'160',"VI","N_A","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',5099,'','Daily OT after 8');
insert into contract_rule values(161,1,'161',"VI","N_A","N_A","N_A","N_A","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',5099,'6th day paid at 1.5x','Start at 1.5x');
insert into contract_rule values(162,1,'162',"VI","N_A","N_A","N_A","N_A","N_A","N_A","N_A","7",null,"OT",'OT-0-15X',5099,'7th day paid at 1.5x','Start at 1.5x');

insert into contract_rule values(164,1,'164',"Z1","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',5099,'','Daily OT after 8');
insert into contract_rule values(165,1,'165',"Z1","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-WK-14-2X',5099,'','2x after 14');
insert into contract_rule values(166,1,'166',"Z1","NonU","N_A","N_A","N_A","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',5199,'','Start at 1.5x');
insert into contract_rule values(167,1,'167',"Z1","NonU","N_A","N_A","N_A","N_A","N_A","N_A","6",null,"GL",'GL-WK-14-2X-16-3X',5199,'','2x after 14, 3x after 16');
insert into contract_rule values(168,1,'168',"Z1","NonU","N_A","N_A","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X-WK-12-4X',5199,'','2x to start, 4x after 12');
insert into contract_rule values(169,1,'169',"Z1","NonU","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-Z1',5099,'','');

insert into contract_rule values(171,1,'171',"TM-40","40","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(172,1,'172',"TM-40","40","N_A","A","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',170,'','p78');
insert into contract_rule values(173,1,'173',"TM-40","40","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','p87, 8(b)');
insert into contract_rule values(174,1,'174',"TM-40","40","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','p87, 8(b)');
insert into contract_rule values(175,1,'175',"TM-40","40","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','p87, 8(b)');
insert into contract_rule values(176,1,'176',"TM-40","40","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','p87, 8(b)');
insert into contract_rule values(177,1,'177',"TM-40","40","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','p88, 8(c)');
insert into contract_rule values(178,1,'178',"TM-40","40","N_A","A","DL","N_A","N_A","N_A","16",null,"GT",'GT-9.5-1X',1170,'','');
insert into contract_rule values(179,1,'179',"TM-40","40","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','p110, P31(a)');
insert into contract_rule values(180,1,'180',"TM-40","40","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','p114, P32(e)');
insert into contract_rule values(181,1,'181',"TM-40","40","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(182,1,'182',"TM-40","40","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','p84, P6(e)');
insert into contract_rule values(183,1,'183',"TM-40","40","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(184,1,'184',"TM-40","40","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(185,1,'185',"TM-40","40","N_A","A","ZB","ON","FT","N_A","15",null,"GL",'GL-EL-14-2X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(186,1,'186',"TM-40","40","N_A","A","ZB","ON","FT","N_A","6",null,"GL",'GL-EL-14-375X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(187,1,'187',"TM-40","40","N_A","A","ZB","ON","FT","N_A","7",null,"GL",'GL-EL-14-5X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(188,1,'188',"TM-40","40","N_A","A","ZB","ON","TV","N_A","15",null,"GL",'GL-WK-14-2X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(189,1,'189',"TM-40","40","N_A","A","ZB","ON","TV","N_A","6",null,"GL",'GL-WK-14-375X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(190,1,'190',"TM-40","40","N_A","A","ZB","ON","TV","N_A","7",null,"GL",'GL-WK-14-5X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(191,1,'191',"TM-40","40","N_A","A","ZB","OP","N_A","N_A","15",null,"GL",'GL-EL-14-2X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(192,1,'192',"TM-40","40","N_A","A","ZB","OP","N_A","N_A","6",null,"GL",'GL-EL-14-375X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(193,1,'193',"TM-40","40","N_A","A","ZB","OP","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(194,1,'194',"TM-40","40","N_A","A","ST","ON","FT","N_A","15",null,"GL",'GL-EL-14-2X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(195,1,'195',"TM-40","40","N_A","A","ST","ON","FT","N_A","6",null,"GL",'GL-EL-14-3X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(196,1,'196',"TM-40","40","N_A","A","ST","ON","FT","N_A","7",null,"GL",'GL-EL-14-4X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(197,1,'197',"TM-40","40","N_A","A","ST","ON","TV","N_A","15",null,"GL",'GL-WK-14-2X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(198,1,'198',"TM-40","40","N_A","A","ST","ON","TV","N_A","6",null,"GL",'GL-WK-14-3X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(199,1,'199',"TM-40","40","N_A","A","ST","ON","TV","N_A","7",null,"GL",'GL-WK-14-4X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(200,1,'200',"TM-40","40","N_A","A","ST","OP","N_A","N_A","15",null,"GL",'GL-EL-12-2X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(201,1,'201',"TM-40","40","N_A","A","ST","OP","N_A","N_A","6",null,"GL",'GL-EL-12-3X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(202,1,'202',"TM-40","40","N_A","A","ST","OP","N_A","N_A","7",null,"GL",'GL-EL-12-4X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(203,1,'203',"TM-40","40","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-BC',30,'','p102, P20; p132, P45');
insert into contract_rule values(204,1,'204',"TM-40","40","N_A","A","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','p116, P32(b)');
insert into contract_rule values(205,1,'205',"TM-40","40","N_A","N_A","SL","OP","N_A","N_A","N_A",null,"NP",'NP-LA-STD',330,'','p83, P5');
insert into contract_rule values(206,1,'206',"TM-40","40","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',170,'','p78');
insert into contract_rule values(207,1,'207',"TM-40","40","N_A","B-1","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','p78');
insert into contract_rule values(208,1,'208',"TM-40","40","N_A","B-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','p83, 1(d)');
insert into contract_rule values(209,1,'209',"TM-40","40","N_A","B-1","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(210,1,'210',"TM-40","40","N_A","B-1","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','p87, 8(b)');

insert into contract_rule values(212,1,'212',"TM-40","40","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'','p110, P31(a)');
insert into contract_rule values(213,1,'213',"TM-40","40","N_A","B-1","DL","N_A","N_A","N_A","16",null,"GT",'GT-7-1X',1170,'','p110, P31(a)');
insert into contract_rule values(214,1,'214',"TM-40","40","N_A","B-1","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','p113, P32(e)');
insert into contract_rule values(215,1,'215',"TM-40","40","N_A","B-1","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',4999,'','p113, P32(e)');
insert into contract_rule values(216,1,'216',"TM-40","40","N_A","N_A","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3030,'','p116, P32(b)');
insert into contract_rule values(217,1,'217',"TM-40","40","N_A","N_A","DL","N_A","N_A","N_A","N_A",null,"GL",'GL-EL-14-25X',130,'','p121, P44(b)');
insert into contract_rule values(218,1,'218',"TM-40","40","N_A","N_A","DL","N_A","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1130,'','p121, P44(b)');

insert into contract_rule values(220,1,'220',"TM-40","40","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(221,1,'221',"TM-40","40","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','p82, 1( c)');
insert into contract_rule values(222,1,'222',"TM-40","40","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','p114, 32(f)(iv)');
insert into contract_rule values(223,1,'223',"TM-40","40","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','p114, 32(f)(ii),(iii)');
insert into contract_rule values(224,1,'224',"TM-40","40","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','p84, P6(e)');
insert into contract_rule values(225,1,'225',"TM-40","40","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(226,1,'226',"TM-40","40","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(227,1,'227',"TM-40","40","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(228,1,'228',"TM-40","40","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(229,1,'229',"TM-40","40","N_A","C","ZB","N_A","N_A","N_A","N_A",null,"HO",'HO-P-5X',170,'','gold pays 5x');
insert into contract_rule values(230,1,'230',"TM-40","40","N_A","C","ST","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',170,'','gold pays 4x');

insert into contract_rule values(99232,1,'1008',"LA-44","44","N_A","A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-LA',170,'','test');
insert into contract_rule values(232,1,'232',"LA-44","44","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(233,1,'233',"LA-44","44","N_A","A","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',170,'','');
insert into contract_rule values(234,1,'234',"LA-44","44","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(235,1,'235',"LA-44","44","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(236,1,'236',"LA-44","44","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(237,1,'237',"LA-44","44","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(238,1,'238',"LA-44","44","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(239,1,'239',"LA-44","44","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(240,1,'240',"LA-44","44","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(241,1,'241',"LA-44","44","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(242,1,'242',"LA-44","44","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(243,1,'243',"LA-44","44","N_A","A","DL","N_A","N_A","N_A","7",null,"OT",'OT-NONE',1170,'','');
insert into contract_rule values(244,1,'244',"LA-44","44","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(245,1,'245',"LA-44","44","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(246,1,'246',"LA-44","44","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(247,1,'247',"LA-44","44","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(248,1,'248',"LA-44","44","N_A","A","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(249,1,'249',"LA-44","44","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54',170,'','');
insert into contract_rule values(250,1,'250',"LA-44","44","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-9-1X',1170,'','');
insert into contract_rule values(251,1,'251',"LA-44","44","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(252,1,'252',"LA-44","44","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(253,1,'253',"LA-44","44","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-60',170,'','');
insert into contract_rule values(254,1,'254',"LA-44","44","N_A","B","DL","N_A","N_A","N_A","15",null,"GT",'GT-9.5-1X',1170,'','');
insert into contract_rule values(255,1,'255',"LA-44","44","N_A","B","DL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(256,1,'256',"LA-44","44","N_A","B","DL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(257,1,'257',"LA-44","44","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(258,1,'258',"LA-44","44","N_A","B","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(259,1,'259',"LA-44","44","N_A","A","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');
insert into contract_rule values(260,1,'260',"LA-44","44","N_A","B","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');
insert into contract_rule values(261,1,'261',"LA-44","44","N_A","B","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(262,1,'262',"LA-44","44","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(263,1,'263',"LA-44","44","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(264,1,'264',"LA-44","44","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(265,1,'265',"LA-44","44","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(266,1,'266',"LA-44","44","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(267,1,'267',"LA-44","44","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(268,1,'268',"LA-44","44","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(269,1,'269',"LA-44","44","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',70,'','(Daily OnCall)');
insert into contract_rule values(270,1,'270',"LA-44","44","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(271,1,'271',"LA-44","44","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'no MPV for on-call','');
insert into contract_rule values(272,1,'272',"LA-44","44","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'no gold for on-call','');

insert into contract_rule values(274,1,'274',"TM-78","78","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(275,1,'275',"TM-78","78","N_A","A","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',170,'','');
insert into contract_rule values(276,1,'276',"TM-78","78","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(277,1,'277',"TM-78","78","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(278,1,'278',"TM-78","78","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(279,1,'279',"TM-78","78","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(280,1,'280',"TM-78","78","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(281,1,'281',"TM-78","78","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(282,1,'282',"TM-78","78","N_A","A","DL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(283,1,'283',"TM-78","78","N_A","A","DL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(284,1,'284',"TM-78","78","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(285,1,'285',"TM-78","78","N_A","A","DL","N_A","N_A","N_A","7",null,"OT",'OT-NONE',1170,'','');
insert into contract_rule values(286,1,'286',"TM-78","78","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(287,1,'287',"TM-78","78","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(288,1,'288',"TM-78","78","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(289,1,'289',"TM-78","78","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','local 80 Pg 26 P8 (c) 1');
insert into contract_rule values(290,1,'290',"TM-78","78","N_A","A","ZB","ON","FT","N_A","15",null,"GL",'GL-EL-14-2X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(291,1,'291',"TM-78","78","N_A","A","ZB","ON","FT","N_A","6",null,"GL",'GL-EL-14-375X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(292,1,'292',"TM-78","78","N_A","A","ZB","ON","FT","N_A","7",null,"GL",'GL-EL-14-5X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(293,1,'293',"TM-78","78","N_A","A","ZB","ON","TV","N_A","15",null,"GL",'GL-WK-14-2X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(294,1,'294',"TM-78","78","N_A","A","ZB","ON","TV","N_A","6",null,"GL",'GL-WK-14-375X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(295,1,'295',"TM-78","78","N_A","A","ZB","ON","TV","N_A","7",null,"GL",'GL-WK-14-5X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(296,1,'296',"TM-78","78","N_A","A","ZB","OP","N_A","N_A","15",null,"GL",'GL-EL-14-2X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(297,1,'297',"TM-78","78","N_A","A","ZB","OP","N_A","N_A","6",null,"GL",'GL-EL-14-375X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(298,1,'298',"TM-78","78","N_A","A","ZB","OP","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(299,1,'299',"TM-78","78","N_A","A","ST","ON","FT","N_A","15",null,"GL",'GL-EL-14-2X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(300,1,'300',"TM-78","78","N_A","A","ST","ON","FT","N_A","6",null,"GL",'GL-EL-14-3X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(301,1,'301',"TM-78","78","N_A","A","ST","ON","FT","N_A","7",null,"GL",'GL-EL-14-4X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(302,1,'302',"TM-78","78","N_A","A","ST","ON","TV","N_A","15",null,"GL",'GL-WK-14-2X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(303,1,'303',"TM-78","78","N_A","A","ST","ON","TV","N_A","6",null,"GL",'GL-WK-14-3X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(304,1,'304',"TM-78","78","N_A","A","ST","ON","TV","N_A","7",null,"GL",'GL-WK-14-4X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(305,1,'305',"TM-78","78","N_A","A","ST","OP","N_A","N_A","15",null,"GL",'GL-EL-12-2X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(306,1,'306',"TM-78","78","N_A","A","ST","OP","N_A","N_A","6",null,"GL",'GL-EL-12-3X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(307,1,'307',"TM-78","78","N_A","A","ST","OP","N_A","N_A","7",null,"GL",'GL-EL-12-4X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(308,1,'308',"TM-78","78","N_A","N_A","DL","N_A","N_A","N_A","N_A",null,"GL",'GL-EL-14-25X',130,'','p121, P44(b)');
insert into contract_rule values(309,1,'309',"TM-78","78","N_A","N_A","DL","N_A","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1130,'','p121, P44(b)');
insert into contract_rule values(310,1,'310',"TM-78","78","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-BC',30,'',' P20;  P45');
insert into contract_rule values(311,1,'311',"TM-78","78","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(312,1,'312',"TM-78","78","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(313,1,'313',"TM-78","78","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(314,1,'314',"TM-78","78","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(315,1,'315',"TM-78","78","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(316,1,'316',"TM-78","78","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(317,1,'317',"TM-78","78","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(318,1,'318',"TM-78","78","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(319,1,'319',"TM-78","78","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(320,1,'320',"TM-78","78","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(321,1,'321',"TM-78","78","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(322,1,'322',"TM-78","78","N_A","C","ZB","N_A","N_A","N_A","N_A",null,"HO",'HO-P-5X',170,'','gold pays 5x');
insert into contract_rule values(323,1,'323',"TM-78","78","N_A","C","ST","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',170,'','gold pays 4x');

insert into contract_rule values(325,1,'325',"LA-80","80","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(326,1,'326',"LA-80","80","N_A","A","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',170,'','');
insert into contract_rule values(327,1,'327',"LA-80","80","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(328,1,'328',"LA-80","80","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(329,1,'329',"LA-80","80","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(330,1,'330',"LA-80","80","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(331,1,'331',"LA-80","80","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(332,1,'332',"LA-80","80","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(333,1,'333',"LA-80","80","N_A","A","DL","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',170,'','');
insert into contract_rule values(334,1,'334',"LA-80","80","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(335,1,'335',"LA-80","80","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(336,1,'336',"LA-80","80","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(337,1,'337',"LA-80","80","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(338,1,'338',"LA-80","80","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(339,1,'339',"LA-80","80","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(340,1,'340',"LA-80","80","N_A","N_A","N_A","N_A","N_A","TR","N_A",null,"GT",'GT-4-8-1X',2030,'Travel pays 4-8 hrs','');
insert into contract_rule values(341,1,'341',"LA-80","80","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54',170,'','');
insert into contract_rule values(342,1,'342',"LA-80","80","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-9-1X',1170,'','');
insert into contract_rule values(343,1,'343',"LA-80","80","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(344,1,'344',"LA-80","80","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(345,1,'345',"LA-80","80","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-60',170,'','');
insert into contract_rule values(346,1,'346',"LA-80","80","N_A","B","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(347,1,'347',"LA-80","80","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(348,1,'348',"LA-80","80","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(350,1,'350',"LA-80","80","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-43',170,'','');
insert into contract_rule values(351,1,'351',"LA-80","80","N_A","B-1","SL","N_A","N_A","N_A","15",null,"GT",'GT-7-1X',1170,'','');
insert into contract_rule values(352,1,'352',"LA-80","80","N_A","B-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(353,1,'353',"LA-80","80","N_A","B-1","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(354,1,'354',"LA-80","80","N_A","B-1","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(355,1,'355',"LA-80","80","N_A","B-1","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');

insert into contract_rule values(357,1,'357',"LA-80","80","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'','');
insert into contract_rule values(358,1,'358',"LA-80","80","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(359,1,'359',"LA-80","80","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(360,1,'360',"LA-80","80","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(361,1,'361',"LA-80","80","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(362,1,'362',"LA-80","80","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(363,1,'363',"LA-80","80","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');

insert into contract_rule values(365,1,'365',"TM-399L","399L","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'','');
insert into contract_rule values(366,1,'366',"TM-399L","399L","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(367,1,'367',"TM-399L","399L","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(368,1,'368',"TM-399L","399L","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(369,1,'369',"TM-399L","399L","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(370,1,'370',"TM-399L","399L","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(371,1,'371',"TM-399L","399L","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(372,1,'372',"TM-399L","399L","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(373,1,'373',"TM-399L","399L","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(374,1,'374',"TM-399L","399L","N_A","C","SL","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',170,'','');
insert into contract_rule values(375,1,'375',"TM-399L","399L","N_A","C","DL","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P-D',170,'','');

insert into contract_rule values(377,1,'377',"TM-399T","399T","N_A","A-10","N_A","OP","N_A","N_A","15",null,"BA",'WK-CUME-40-4',1270,'','p84, footnote 1');
insert into contract_rule values(378,1,'378',"TM-399T","399T","N_A","A-10","N_A","OP","N_A","N_A","15",null,"GT",'GT-10-1X',1270,'','p84, footnote 1');
insert into contract_rule values(379,1,'379',"TM-399T","399T","N_A","A-10","N_A","OP","N_A","N_A","15",null,"OT",'OT-10-15X',1270,'','p84, footnote 1');
insert into contract_rule values(380,1,'380',"TM-399T","399T","N_A","A-10","N_A","OP","N_A","N_A","5",null,"GT",'GT-8-1X',1270,'','p84, footnote 1?');
insert into contract_rule values(381,1,'381',"TM-399T","399T","N_A","A-10","N_A","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1070,'','');
insert into contract_rule values(382,1,'382',"TM-399T","399T","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(383,1,'383',"TM-399T","399T","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(384,1,'384',"TM-399T","399T","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(385,1,'385',"TM-399T","399T","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(386,1,'386',"TM-399T","399T","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(387,1,'387',"TM-399T","399T","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');

insert into contract_rule values(389,1,'389',"TM-399T","399T","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'','p91, sec 7c');
insert into contract_rule values(390,1,'390',"TM-399T","399T","3500","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',250,'','??');
insert into contract_rule values(391,1,'391',"TM-399T","399T","3500","C","SL","N_A","N_A","HA","N_A",null,"OC",'OC-NO-PAY',2250,'Employee leaves work on own accord, relinquishes 1/5W.','p91, sec 7d');
insert into contract_rule values(392,1,'392',"TM-399T","399T","3500","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',150,'On-call does not get MPVs','');
insert into contract_rule values(393,1,'393',"TM-399T","399T","3500","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',150,'On-call does not get gold time','');
insert into contract_rule values(394,1,'394',"TM-399T","399T","N_A","A","SL","N_A","N_A","N_A","N_A",null,"HO",'HO-P-5X',170,'','p93, sec 8');
insert into contract_rule values(395,1,'395',"TM-399T","399T","N_A","A","SL","N_A","N_A","N_A","15",null,"CB",'CB-4-15X',1170,'','p97, sec 9');
insert into contract_rule values(396,1,'396',"TM-399T","399T","N_A","A","SL","N_A","N_A","N_A","67",null,"CB",'CB-3-2X',1170,'','p97, sec 9');

insert into contract_rule values(398,1,'398',"TM-399T","399T","N_A","A","N_A","N_A","N_A","N_A","15",null,"GL",'GL-EL-14-25X',1070,'','p97. sec 10');
insert into contract_rule values(399,1,'399',"TM-399T","399T","N_A","A","N_A","N_A","N_A","N_A","6",null,"GL",'GL-EL-14-375X',1070,'','p97. sec 10');
insert into contract_rule values(400,1,'400',"TM-399T","399T","N_A","A","N_A","N_A","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1070,'','p97. sec 10');
insert into contract_rule values(401,1,'401',"TM-399T","399T","N_A","A","N_A","N_A","N_A","N_A","7",null,"RS",'RST-8-PR',1070,'','p98. sec 10b');
insert into contract_rule values(402,1,'402',"TM-399T","399T","N_A","A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-BC',70,'','p102, sec 18');
insert into contract_rule values(403,1,'403',"TM-399T","399T","N_A","A","SL","OP","N_A","N_A","N_A",null,"NP",'NP-LA-STD',370,'','p88, sec 4');
insert into contract_rule values(404,1,'404',"TM-399T","399T","N_A","A","SL","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2170,'','p89, sec 5f');
insert into contract_rule values(405,1,'405',"TM-399T","399T","3500","C","SL","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2250,'','p89, sec 5f');
insert into contract_rule values(406,1,'406',"TM-399T","399T","N_A","A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-BC',70,'','p102 18a,b; p104, 18g, p128 40a');
insert into contract_rule values(407,1,'407',"TM-399T","399T","N_A","A","N_A","N_A","N_A","N_A","N_A",'FR=Y',"MP",'MP-NONE',70,'French Hours','p104, sec 18 2j; p129, sec 40e');
insert into contract_rule values(408,1,'408',"TM-399T","399T","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(409,1,'409',"TM-399T","399T","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');

insert into contract_rule values(411,1,'411',"TM-399T","399T","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(412,1,'412',"TM-399T","399T","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(413,1,'413',"TM-399T","399T","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','p123, sec 33e');
insert into contract_rule values(414,1,'414',"TM-399T","399T","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(415,1,'415',"TM-399T","399T","N_A","A","DL","N_A","N_A","N_A","16",null,"BA",'WK-CUME-40-6',1170,' ','p122, sec 33b par 2');
insert into contract_rule values(416,1,'416',"TM-399T","399T","N_A","A","DL","N_A","N_A","N_A","16",null,"GL",'GL-EL-14-25X',1170,'','p128, sec 39b');
insert into contract_rule values(417,1,'417',"TM-399T","399T","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1170,'7th day or Holiday worked','p128, sec 39b');
insert into contract_rule values(418,1,'418',"TM-399T","399T","N_A","A","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'Idle 6th Day','p122, sec 33b par 3');
insert into contract_rule values(419,1,'419',"TM-399T","399T","N_A","A","DL","N_A","N_A","TR","N_A",null,"GT",'GT-4-8-1X',2170,'Travel pays 4-8 hrs','p 125, sec 36a');
insert into contract_rule values(420,1,'420',"TM-399L","399T","3500","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',250,'','??');
insert into contract_rule values(421,1,'421',"TM-399T","399T","3500","C","DL","OP","N_A","N_A","N_A",null,"OC",'OC-NO-PAY',450,'','p117, footnote 1');
insert into contract_rule values(422,1,'422',"TM-399T","399T","3500","C","DL","N_A","N_A","N_A","67",null,"OC",'OC-SC-5',1250,'On Call Distant 6 & 7 Days','p123, sec 33 f');
insert into contract_rule values(423,1,'423',"TM-399T","399T","3500","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3150,'Idle 6th & 7th Day','p123, sec 33 f');
insert into contract_rule values(424,1,'424',"TM-399T","399T","3500","C","DL","N_A","N_A","TR","N_A",null,"OC",'OC-1/6',2250,'On Call Travel','p 126, sec 36a');

insert into contract_rule values(426,1,'426',"TM-399T","399T","N_A","A","DL","N_A","N_A","N_A","N_A",null,"HO",'HO-P-5X-D',170,'','p94, 8b');

insert into contract_rule values(428,1,'428',"TM-MW","399T","N_A","X-8","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(429,1,'429',"TM-MW","399T","N_A","X-8","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',170,'','');





insert into contract_rule values(435,1,'435',"TM-MW","399T","N_A","X-8","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'','p91, sec 7c');
insert into contract_rule values(436,1,'436',"TM-MW","399T","4622","X-OC","SL","N_A","N_A","HA","N_A",null,"OC",'OC-NO-PAY',2250,'Employee leaves work on own accord, relinquishes 1/5W.','p91, sec 7d');
insert into contract_rule values(437,1,'437',"TM-MW","399T","4622","X-OC","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',150,'On-call does not get MPVs','');
insert into contract_rule values(438,1,'438',"TM-MW","399T","4622","X-OC","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',150,'On-call does not get gold time','');
insert into contract_rule values(439,1,'439',"TM-MW","399T","N_A","X-8","SL","N_A","N_A","N_A","N_A",null,"HO",'HO-P-5X',170,'','p93, sec 8');
insert into contract_rule values(440,1,'440',"TM-MW","399T","N_A","X-8","SL","N_A","N_A","N_A","15",null,"CB",'CB-4-15X',1170,'','p97, sec 9');
insert into contract_rule values(441,1,'441',"TM-MW","399T","N_A","X-8","SL","N_A","N_A","N_A","67",null,"CB",'CB-3-2X',1170,'','p97, sec 9');

insert into contract_rule values(443,1,'443',"TM-MW","399T","N_A","X-8","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-EL-14-2X',70,'','p97. sec 10');



insert into contract_rule values(447,1,'447',"TM-MW","399T","N_A","X-8","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-BC',70,'','p102, sec 18');
insert into contract_rule values(448,1,'448',"TM-MW","399T","N_A","X-8","SL","OP","N_A","N_A","N_A",null,"NP",'NP-NONE',370,'','p88, sec 4');
insert into contract_rule values(449,1,'449',"TM-MW","399T","N_A","X-8","SL","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2170,'','p89, sec 5f');
insert into contract_rule values(450,1,'450',"TM-MW","399T","4622","X-OC","SL","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2250,'','p89, sec 5f');
insert into contract_rule values(451,1,'451',"TM-MW","399T","N_A","X-8","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-BC',70,'','p102 18a,b; p104, 18g, p128 40a');
insert into contract_rule values(452,1,'452',"TM-MW","399T","N_A","X-8","N_A","N_A","N_A","N_A","N_A",'FR=Y',"MP",'MP-NONE',70,'French Hours','p104, sec 18 2j; p129, sec 40e');
insert into contract_rule values(453,1,'453',"TM-MW","399T","N_A","X-8","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(454,1,'454',"TM-MW","399T","N_A","X-8","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');

insert into contract_rule values(456,1,'456',"TM-MW","399T","N_A","X-8","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(457,1,'457',"TM-MW","399T","N_A","X-8","DL","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',170,'','');


insert into contract_rule values(460,1,'460',"TM-MW","399T","N_A","X-8","DL","N_A","N_A","N_A","16",null,"BA",'WK-CUME-40-6',1170,' ','p122, sec 33b par 2');
insert into contract_rule values(461,1,'461',"TM-MW","399T","N_A","X-8","DL","N_A","N_A","N_A","N_A",null,"GL",'GL-EL-14-2X',170,'','p128, sec 39b');

insert into contract_rule values(463,1,'463',"TM-MW","399T","N_A","X-8","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'Idle 6th Day','p122, sec 33b par 3');
insert into contract_rule values(464,1,'464',"TM-MW","399T","N_A","X-8","N_A","N_A","N_A","TR","N_A",null,"GT",'GT-4-8-1X',2070,'Travel pays 4-8 hrs','p 125, sec 36a');
insert into contract_rule values(465,1,'465',"TM-MW","399T","4622","X-OC","DL","OP","N_A","N_A","N_A",null,"OC",'OC-NO-PAY',450,'','p117, footnote 1');
insert into contract_rule values(466,1,'466',"TM-MW","399T","4622","X-OC","DL","N_A","N_A","N_A","67",null,"OC",'OC-SC-5',1250,'On Call Distant 6 & 7 Days','p123, sec 33 f');
insert into contract_rule values(467,1,'467',"TM-MW","399T","4622","X-OC","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3150,'Idle 6th & 7th Day','p123, sec 33 f');
insert into contract_rule values(468,1,'468',"TM-MW","399T","4622","X-OC","DL","N_A","N_A","TR","N_A",null,"OC",'OC-1/6',2250,'On Call Travel','p 126, sec 36a');



insert into contract_rule values(472,1,'472',"LA-600C","600C","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(473,1,'473',"LA-600C","600C","N_A","A","DL","N_A","N_A","TR","N_A",null,"GT",'GT-4-8-1X',2170,'Travel pays 4-8 hrs','');
insert into contract_rule values(474,1,'474',"LA-600C","600C","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(475,1,'475',"LA-600C","600C","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(476,1,'476',"LA-600C","600C","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(477,1,'477',"LA-600C","600C","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(478,1,'478',"LA-600C","600C","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(479,1,'479',"LA-600C","600C","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(480,1,'480',"LA-600C","600C","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(481,1,'481',"LA-600C","600C","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(482,1,'482',"LA-600C","600C","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(483,1,'483',"LA-600C","600C","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(484,1,'484',"LA-600C","600C","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(485,1,'485',"LA-600C","600C","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(486,1,'486',"LA-600C","600C","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','local 80 Pg 26 P8 (c) 1');
insert into contract_rule values(487,1,'487',"LA-600C","600C","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(488,1,'488',"LA-600C","600C","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(489,1,'489',"LA-600C","600C","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GL",'GL-WK-12-2X',170,'','????');
insert into contract_rule values(490,1,'490',"LA-600C","600C","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GL",'GL-WK-14-2X',170,'','????');
insert into contract_rule values(491,1,'491',"LA-600C","600C","N_A","A-1","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-STD-40',70,'','');
insert into contract_rule values(492,1,'492',"LA-600C","600C","N_A","A-1","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',70,'','');
insert into contract_rule values(493,1,'493',"LA-600C","600C","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-43',170,'','');
insert into contract_rule values(494,1,'494',"LA-600C","600C","N_A","B","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8.6-1X',170,'','');
insert into contract_rule values(495,1,'495',"LA-600C","600C","N_A","B","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-9.3-15X',170,'','');
insert into contract_rule values(496,1,'496',"LA-600C","600C","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(497,1,'497',"LA-600C","600C","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(498,1,'498',"LA-600C","600C","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(499,1,'499',"LA-600C","600C","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(501,1,'501',"LA-600C","600C","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-43',170,'','');
insert into contract_rule values(502,1,'502',"LA-600C","600C","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-7-1X',170,'','');
insert into contract_rule values(503,1,'503',"LA-600C","600C","N_A","B-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','6th day min call?');
insert into contract_rule values(504,1,'504',"LA-600C","600C","N_A","B-1","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(505,1,'505',"LA-600C","600C","N_A","B-1","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(506,1,'506',"LA-600C","600C","N_A","B-1","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(508,1,'508',"LA-600C","600C","N_A","C","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',170,'','');
insert into contract_rule values(509,1,'509',"LA-600C","600C","N_A","C","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(510,1,'510',"LA-600C","600C","N_A","C","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','6th day min call?');
insert into contract_rule values(511,1,'511',"LA-600C","600C","N_A","C","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(512,1,'512',"LA-600C","600C","N_A","C","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(513,1,'513',"LA-600C","600C","N_A","C","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');
insert into contract_rule values(514,1,'514',"LA-600C","600C","N_A","C","N_A","N_A","N_A","N_A","7",null,"OT",'OT-NONE',1070,'','');
insert into contract_rule values(515,1,'515',"LA-600C","600C","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-LA',70,'','');

insert into contract_rule values(517,1,'517',"LA-600C","600C","N_A","C-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',170,'','');
insert into contract_rule values(518,1,'518',"LA-600C","600C","N_A","C-1","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(519,1,'519',"LA-600C","600C","N_A","C-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','6th day min call?');
insert into contract_rule values(520,1,'520',"LA-600C","600C","N_A","C-1","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(521,1,'521',"LA-600C","600C","N_A","C-1","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(522,1,'522',"LA-600C","600C","N_A","C-1","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');
insert into contract_rule values(523,1,'523',"LA-600C","600C","N_A","C-1","N_A","N_A","N_A","N_A","7",null,"OT",'OT-NONE',1070,'','');
insert into contract_rule values(524,1,'524',"LA-600C","600C","N_A","C-1","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-LA',70,'','');

insert into contract_rule values(526,1,'526',"LA-600C","600C","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-48-6',170,'','');
insert into contract_rule values(527,1,'527',"LA-600C","600C","N_A","B","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(528,1,'528',"LA-600C","600C","N_A","B","DL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',170,'','');
insert into contract_rule values(529,1,'529',"LA-600C","600C","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-48-6',170,'','');
insert into contract_rule values(530,1,'530',"LA-600C","600C","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(531,1,'531',"LA-600C","600C","N_A","C","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54-6',170,'','');
insert into contract_rule values(532,1,'532',"LA-600C","600C","N_A","C","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(533,1,'533',"LA-600C","600C","N_A","C-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54-6',170,'','');
insert into contract_rule values(534,1,'534',"LA-600C","600C","N_A","C-1","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');

insert into contract_rule values(536,1,'536',"LA-600P","600P","N_A","A","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',70,'','');
insert into contract_rule values(537,1,'537',"LA-600P","600P","N_A","A","N_A","N_A","N_A","N_A","16",null,"GT",'GT-7-1X',1070,'','');
insert into contract_rule values(538,1,'538',"LA-600P","600P","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(539,1,'539',"LA-600P","600P","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(540,1,'540',"LA-600P","600P","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','local 80 Pg 26 P8 (c) 1');
insert into contract_rule values(541,1,'541',"LA-600P","600P","N_A","A","DL","N_A","N_A","N_A","6",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(542,1,'542',"LA-600P","600P","N_A","A","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');
insert into contract_rule values(543,1,'543',"LA-600P","600P","N_A","A","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(544,1,'544',"LA-600P","600P","N_A","A","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');
insert into contract_rule values(545,1,'545',"LA-600P","600P","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(546,1,'546',"LA-600P","600P","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(547,1,'547',"LA-600P","600P","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(548,1,'548',"LA-600P","600P","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(549,1,'549',"LA-600P","600P","N_A","B","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',70,'','');
insert into contract_rule values(550,1,'550',"LA-600P","600P","N_A","B","N_A","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(551,1,'551',"LA-600P","600P","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(552,1,'552',"LA-600P","600P","N_A","B","DL","N_A","N_A","N_A","6",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(553,1,'553',"LA-600P","600P","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(554,1,'554',"LA-600P","600P","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(556,1,'556',"LA-600P","600P","N_A","B","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');
insert into contract_rule values(557,1,'557',"LA-600P","600P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(558,1,'558',"LA-600P","600P","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(559,1,'559',"LA-600P","600P","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(560,1,'560',"LA-600P","600P","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(561,1,'561',"LA-600P","600P","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(562,1,'562',"LA-600P","600P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(563,1,'563',"LA-600P","600P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(564,1,'564',"LA-600P","600P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(565,1,'565',"LA-600P","600P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');

insert into contract_rule values(567,1,'567',"LA-695P","695P","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(568,1,'568',"LA-695P","695P","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(569,1,'569',"LA-695P","695P","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(570,1,'570',"LA-695P","695P","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(571,1,'571',"LA-695P","695P","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(572,1,'572',"LA-695P","695P","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(573,1,'573',"LA-695P","695P","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(574,1,'574',"LA-695P","695P","N_A","A","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(575,1,'575',"LA-695P","695P","N_A","A","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');
insert into contract_rule values(576,1,'576',"LA-695P","695P","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(577,1,'577',"LA-695P","695P","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(578,1,'578',"LA-695P","695P","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(579,1,'579',"LA-695P","695P","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(580,1,'580',"LA-695P","695P","N_A","N_A","N_A","N_A","N_A","TR","N_A",null,"GT",'GT-4-8-1X',2030,'Travel pays 4-8 hrs','??');
insert into contract_rule values(581,1,'581',"LA-695P","695P","N_A","A","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');
insert into contract_rule values(582,1,'582',"LA-695P","695P","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-43',170,'','');
insert into contract_rule values(583,1,'583',"LA-695P","695P","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-7-1X',1170,'','');
insert into contract_rule values(584,1,'584',"LA-695P","695P","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(585,1,'585',"LA-695P","695P","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(586,1,'586',"LA-695P","695P","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'','');
insert into contract_rule values(587,1,'587',"LA-695P","695P","N_A","B","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(588,1,'588',"LA-695P","695P","N_A","B","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');
insert into contract_rule values(589,1,'589',"LA-695P","695P","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(590,1,'590',"LA-695P","695P","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(592,1,'592',"LA-695P","695P","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'','');
insert into contract_rule values(593,1,'593',"LA-695P","695P","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(594,1,'594',"LA-695P","695P","N_A","B-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(595,1,'595',"LA-695P","695P","N_A","B-1","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(596,1,'596',"LA-695P","695P","N_A","B-1","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(597,1,'597',"LA-695P","695P","N_A","B-1","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');
insert into contract_rule values(598,1,'598',"LA-695P","695P","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-43-6',170,'','');
insert into contract_rule values(599,1,'599',"LA-695P","695P","N_A","B-1","DL","N_A","N_A","N_A","16",null,"GT",'GT-4-1X',1170,'','');
insert into contract_rule values(600,1,'600',"LA-695P","695P","N_A","B-1","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','8 hrs?');
insert into contract_rule values(601,1,'601',"LA-695P","695P","N_A","B-1","DL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(602,1,'602',"LA-695P","695P","N_A","B-1","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');

insert into contract_rule values(604,1,'604',"LA-695P","695P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(605,1,'605',"LA-695P","695P","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(606,1,'606',"LA-695P","695P","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(607,1,'607',"LA-695P","695P","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(608,1,'608',"LA-695P","695P","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(609,1,'609',"LA-695P","695P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(610,1,'610',"LA-695P","695P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(611,1,'611',"LA-695P","695P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(612,1,'612',"LA-695P","695P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(613,1,'613',"LA-695P","695P","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',30,'','assume std for testing');

insert into contract_rule values(615,1,'615',"LA-695S","695S","N_A","A-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-STD-40',170,'','for y4-y16: 1.5 after 40');
insert into contract_rule values(616,1,'616',"LA-695S","695S","N_A","A-1","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-9-1X',170,'','');
insert into contract_rule values(617,1,'617',"LA-695S","695S","N_A","N_A","N_A","N_A","N_A","TR","N_A",null,"GT",'GT-4-8-1X',2030,'Travel pays 4-8 hrs','??');
insert into contract_rule values(618,1,'618',"LA-695S","695S","N_A","A-1","SL","N_A","N_A","N_A","N_A",'WRKD>4',"OT",'OT-8-15X',170,'worked 5 days or more','1.5x after 8');
insert into contract_rule values(619,1,'619',"LA-695S","695S","N_A","A-1","SL","N_A","N_A","N_A","N_A",'WRKD<5',"OT",'OT-9-15X',170,'worked less than 5 days','1.5x after 9');
insert into contract_rule values(620,1,'620',"LA-695S","695S","8105","A-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',250,'','y1-y3 no wkly OT');
insert into contract_rule values(621,1,'621',"LA-695S","695S","8171","A-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',250,'','y1-y3 no wkly OT');
insert into contract_rule values(622,1,'622',"LA-695S","695S","8109","A-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',250,'','y1-y3 no wkly OT');
insert into contract_rule values(623,1,'623',"LA-695S","695S","8173","A-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',250,'','y1-y3 no wkly OT');
insert into contract_rule values(624,1,'624',"LA-695S","695S","8105","A-1","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',250,'','1.5x after 9');
insert into contract_rule values(625,1,'625',"LA-695S","695S","8171","A-1","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',250,'','1.5x after 9');
insert into contract_rule values(626,1,'626',"LA-695S","695S","8109","A-1","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',250,'','1.5x after 9');
insert into contract_rule values(627,1,'627',"LA-695S","695S","8173","A-1","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',250,'','1.5x after 9');
insert into contract_rule values(628,1,'628',"LA-695S","695S","N_A","A-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-STD-40',170,'','for y4-y16: 1.5 after 40');
insert into contract_rule values(629,1,'629',"LA-695S","695S","N_A","A-1","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(630,1,'630',"LA-695S","695S","N_A","A-1","DL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',170,'','1.5x after 9');
insert into contract_rule values(631,1,'631',"LA-695S","695S","8105","A-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',250,'','y1-y3 no wkly OT');
insert into contract_rule values(632,1,'632',"LA-695S","695S","8171","A-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',250,'','y1-y3 no wkly OT');
insert into contract_rule values(633,1,'633',"LA-695S","695S","8109","A-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',250,'','y1-y3 no wkly OT');
insert into contract_rule values(634,1,'634',"LA-695S","695S","8173","A-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',250,'','y1-y3 no wkly OT');
insert into contract_rule values(635,1,'635',"LA-695S","695S","N_A","N_A","N_A","N_A","N_A","ID","N_A",null,"GT",'GT-4-4-1X',2030,'','??');
insert into contract_rule values(636,1,'636',"LA-695S","695S","N_A","A-1","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(637,1,'637',"LA-695S","695S","N_A","A-1","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(638,1,'638',"LA-695S","695S","N_A","A-1","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','local 80 Pg 26 P8 (c) 1');
insert into contract_rule values(639,1,'639',"LA-695S","695S","N_A","A-1","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(640,1,'640',"LA-695S","695S","N_A","A-1","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(641,1,'641',"LA-695S","695S","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',170,'','1.5x after 40');
insert into contract_rule values(642,1,'642',"LA-695S","695S","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(643,1,'643',"LA-695S","695S","8105","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',250,'','1.5x after 48.6');
insert into contract_rule values(644,1,'644',"LA-695S","695S","8171","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',250,'','1.5x after 48.6');
insert into contract_rule values(645,1,'645',"LA-695S","695S","8109","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',250,'','1.5x after 48.6');
insert into contract_rule values(646,1,'646',"LA-695S","695S","8173","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',250,'','1.5x after 48.6');
insert into contract_rule values(647,1,'647',"LA-695S","695S","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54-6-A',170,'','1.5x after 54');
insert into contract_rule values(648,1,'648',"LA-695S","695S","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(649,1,'649',"LA-695S","695S","N_A","B-2","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'','1.5x after 40?');
insert into contract_rule values(650,1,'650',"LA-695S","695S","N_A","B-2","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(651,1,'651',"LA-695S","695S","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',30,'','assume std for testing');

insert into contract_rule values(653,1,'653',"LA-700E","700E","N_A","N_A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1130,'','');
insert into contract_rule values(654,1,'654',"LA-700E","700E","N_A","N_A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1130,'','');
insert into contract_rule values(655,1,'655',"LA-700E","700E","N_A","N_A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1130,'','');
insert into contract_rule values(656,1,'656',"LA-700E","700E","N_A","N_A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1130,'','');
insert into contract_rule values(657,1,'657',"LA-700E","700E","N_A","N_A","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1030,'','');
insert into contract_rule values(658,1,'658',"LA-700E","700E","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(659,1,'659',"LA-700E","700E","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(660,1,'660',"LA-700E","700E","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(661,1,'661',"LA-700E","700E","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(662,1,'662',"LA-700E","700E","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(663,1,'663',"LA-700E","700E","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(664,1,'664',"LA-700E","700E","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(665,1,'665',"LA-700E","700E","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(666,1,'666',"LA-700E","700E","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(667,1,'667',"LA-700E","700E","N_A","A","SL","OP","N_A","N_A","N_A",null,"NP",'NP-A-8-1-6',370,'','????');
insert into contract_rule values(668,1,'668',"LA-700E","700E","N_A","A-2","SL","OP","N_A","N_A","N_A",null,"NP",'NP-A-8-1-6',370,'','????');
insert into contract_rule values(669,1,'669',"LA-700E","700E","N_A","A-4","SL","OP","N_A","N_A","N_A",null,"NP",'NP-A-8-1-6',370,'','????');
insert into contract_rule values(670,1,'670',"LA-700E","700E","N_A","A-5","SL","OP","N_A","N_A","N_A",null,"NP",'NP-A-8-1-6',370,'','????');
insert into contract_rule values(671,1,'671',"LA-700E","700E","N_A","A-2","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(672,1,'672',"LA-700E","700E","N_A","A-2","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(673,1,'673',"LA-700E","700E","N_A","A-2","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(674,1,'674',"LA-700E","700E","N_A","A-2","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(675,1,'675',"LA-700E","700E","N_A","A-2","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(676,1,'676',"LA-700E","700E","N_A","A-2","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(677,1,'677',"LA-700E","700E","N_A","A-2","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(678,1,'678',"LA-700E","700E","N_A","A-2","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(679,1,'679',"LA-700E","700E","N_A","A-2","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(680,1,'680',"LA-700E","700E","N_A","A-4","SL","N_A","N_A","N_A","15",null,"GT",'GT-9-1X',1170,'','');
insert into contract_rule values(681,1,'681',"LA-700E","700E","N_A","A-4","SL","N_A","N_A","N_A","15",null,"OT",'OT-9-15X',1170,'','');
insert into contract_rule values(682,1,'682',"LA-700E","700E","N_A","A-5","SL","N_A","N_A","N_A","15",null,"GT",'GT-9-1X',1170,'','');
insert into contract_rule values(683,1,'683',"LA-700E","700E","N_A","A-5","SL","N_A","N_A","N_A","15",null,"OT",'OT-9-15X',1170,'','');
insert into contract_rule values(684,1,'684',"LA-700E","700E","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-43',170,'PP','');
insert into contract_rule values(685,1,'685',"LA-700E","700E","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-7-1X',170,'PP','');
insert into contract_rule values(686,1,'686',"LA-700E","700E","N_A","B-3","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',170,'PP','');
insert into contract_rule values(687,1,'687',"LA-700E","700E","N_A","B-3","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(688,1,'688',"LA-700E","700E","N_A","B-4","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',170,'PP','');
insert into contract_rule values(689,1,'689',"LA-700E","700E","N_A","B-4","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(690,1,'690',"LA-700E","700E","N_A","C-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',170,'Major','');
insert into contract_rule values(691,1,'691',"LA-700E","700E","N_A","C-1","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'Major','');
insert into contract_rule values(692,1,'692',"LA-700E","700E","N_A","D-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',170,'Major','');
insert into contract_rule values(693,1,'693',"LA-700E","700E","N_A","D-1","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'Major','');
insert into contract_rule values(694,1,'694',"LA-700E","700E","N_A","D-1","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(695,1,'695',"LA-700E","700E","N_A","D-1","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(696,1,'696',"LA-700E","700E","N_A","D-1","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(697,1,'697',"LA-700E","700E","N_A","D-1","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(698,1,'698',"LA-700E","700E","N_A","D-1","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(699,1,'699',"LA-700E","700E","N_A","D-1","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(700,1,'700',"LA-700E","700E","N_A","E-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'Major','');
insert into contract_rule values(701,1,'701',"LA-700E","700E","N_A","E-1","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-7-1X',170,'Major','');
insert into contract_rule values(702,1,'702',"LA-700E","700E","N_A","C-2","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-43',170,'PP','');
insert into contract_rule values(703,1,'703',"LA-700E","700E","N_A","C-2","SL","N_A","N_A","N_A","15",null,"GT",'GT-7-1X',1170,'PP','');
insert into contract_rule values(704,1,'704',"LA-700E","700E","N_A","F","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',170,'PP','');
insert into contract_rule values(705,1,'705',"LA-700E","700E","N_A","F","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(706,1,'706',"LA-700E","700E","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'PP','');
insert into contract_rule values(707,1,'707',"LA-700E","700E","N_A","A","DL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',170,'PP','');
insert into contract_rule values(708,1,'708',"LA-700E","700E","N_A","A-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-STD-40',170,'PP','');
insert into contract_rule values(709,1,'709',"LA-700E","700E","N_A","A-1","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'PP','');
insert into contract_rule values(710,1,'710',"LA-700E","700E","N_A","A-1","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'PP','');
insert into contract_rule values(711,1,'711',"LA-700E","700E","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'PP','');
insert into contract_rule values(712,1,'712',"LA-700E","700E","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(713,1,'713',"LA-700E","700E","N_A","B-2","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54-6-A',170,'PP','1.5 after 54');
insert into contract_rule values(714,1,'714',"LA-700E","700E","N_A","B-2","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(715,1,'715',"LA-700E","700E","N_A","B-3","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'PP','');
insert into contract_rule values(716,1,'716',"LA-700E","700E","N_A","B-3","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(717,1,'717',"LA-700E","700E","N_A","C-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'PP','');
insert into contract_rule values(718,1,'718',"LA-700E","700E","N_A","C-1","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(719,1,'719',"LA-700E","700E","N_A","D-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',170,'PP','');
insert into contract_rule values(720,1,'720',"LA-700E","700E","N_A","D-1","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'PP','');
insert into contract_rule values(721,1,'721',"LA-700E","700E","N_A","F-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6-54',170,'PP','');
insert into contract_rule values(722,1,'722',"LA-700E","700E","N_A","F-1","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(723,1,'723',"LA-700E-I","700E","N_A","A","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',170,'Ind','');
insert into contract_rule values(724,1,'724',"LA-700E-I","700E","N_A","A-3","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',170,'Ind','');
insert into contract_rule values(725,1,'725',"LA-700E-I","700E","N_A","B-2","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',170,'Ind','');
insert into contract_rule values(726,1,'726',"LA-700E-I","700E","N_A","B-2","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'Ind','');
insert into contract_rule values(727,1,'727',"LA-700E-I","700E","N_A","B-3","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',170,'Ind','');
insert into contract_rule values(728,1,'728',"LA-700E-I","700E","N_A","B-3","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'Ind','');
insert into contract_rule values(729,1,'729',"LA-700E-I","700E","N_A","C-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-43',170,'Ind','');
insert into contract_rule values(730,1,'730',"LA-700E-I","700E","N_A","C-1","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-7-1X',170,'Ind','');
insert into contract_rule values(731,1,'731',"LA-700E-I","700E","N_A","C-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'Ind','');
insert into contract_rule values(732,1,'732',"LA-700E-I","700E","N_A","D","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',170,'Ind','');
insert into contract_rule values(733,1,'733',"LA-700E-I","700E","N_A","D","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'Ind','');
insert into contract_rule values(734,1,'734',"LA-700E-I","700E","N_A","E","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',170,'Ind','');
insert into contract_rule values(735,1,'735',"LA-700E-I","700E","N_A","E","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'Ind','');
insert into contract_rule values(736,1,'736',"LA-700E-I","700E","N_A","A-2","DL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',170,'Ind','');
insert into contract_rule values(737,1,'737',"LA-700E-I","700E","N_A","A-3","DL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',170,'Ind','');
insert into contract_rule values(738,1,'738',"LA-700E-I","700E","N_A","B-2","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54-6-A',170,'Ind','');
insert into contract_rule values(739,1,'739',"LA-700E-I","700E","N_A","B-2","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'Ind','');
insert into contract_rule values(740,1,'740',"LA-700E-I","700E","N_A","B-3","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'Ind','');
insert into contract_rule values(741,1,'741',"LA-700E-I","700E","N_A","B-3","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'Ind','');
insert into contract_rule values(742,1,'742',"LA-700E-I","700E","N_A","C","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'Ind','');
insert into contract_rule values(743,1,'743',"LA-700E-I","700E","N_A","C","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'Ind','');
insert into contract_rule values(744,1,'744',"LA-700E-I","700E","N_A","D","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6-54',170,'Ind','');
insert into contract_rule values(745,1,'745',"LA-700E-I","700E","N_A","D","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'Ind','');
insert into contract_rule values(746,1,'746',"LA-700E-I","700E","N_A","E","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',170,'Ind','');
insert into contract_rule values(747,1,'747',"LA-700E-I","700E","N_A","E","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'Ind','');

insert into contract_rule values(749,1,'749',"LA-700S","700S","N_A","N_A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1130,'PP','');
insert into contract_rule values(750,1,'750',"LA-700S","700S","N_A","N_A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1130,'','');
insert into contract_rule values(751,1,'751',"LA-700S","700S","N_A","N_A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1130,'PP','');
insert into contract_rule values(752,1,'752',"LA-700S","700S","N_A","N_A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1130,'PP','');
insert into contract_rule values(753,1,'753',"LA-700S","700S","N_A","N_A","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1030,'','');
insert into contract_rule values(754,1,'754',"LA-700S","700S","N_A","B-3","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',170,'PP','1.5x after 48.6');
insert into contract_rule values(755,1,'755',"LA-700S","700S","N_A","B-3","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(756,1,'756',"LA-700S","700S","N_A","B-4","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',170,'PP','1.5x after 40');
insert into contract_rule values(757,1,'757',"LA-700S","700S","N_A","B-4","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(758,1,'758',"LA-700S","700S","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'PP','');
insert into contract_rule values(759,1,'759',"LA-700S","700S","N_A","A","DL","N_A","N_A","N_A","N_A",null,"OT",'OT-9-15X',170,'PP','');
insert into contract_rule values(760,1,'760',"LA-700S","700S","N_A","B-2","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54-6',170,'PP','');
insert into contract_rule values(761,1,'761',"LA-700S","700S","N_A","B-2","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');
insert into contract_rule values(762,1,'762',"LA-700S","700S","N_A","B-3","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'PP','');
insert into contract_rule values(763,1,'763',"LA-700S","700S","N_A","B-3","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'PP','');



insert into contract_rule values(767,1,'767',"LA-700A","700A","N_A","B","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',70,'','');
insert into contract_rule values(768,1,'768',"LA-700A","700A","N_A","B","N_A","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(769,1,'769',"LA-700A","700A","N_A","B","N_A","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(770,1,'770',"LA-700A","700A","N_A","B","N_A","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1070,'','');
insert into contract_rule values(771,1,'771',"LA-700A","700A","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(772,1,'772',"LA-700A","700A","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(774,1,'774',"LA-705","705","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(775,1,'775',"LA-705","705","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(776,1,'776',"LA-705","705","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(777,1,'777',"LA-705","705","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(778,1,'778',"LA-705","705","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(779,1,'779',"LA-705","705","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(780,1,'780',"LA-705","705","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(781,1,'781',"LA-705","705","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(782,1,'782',"LA-705","705","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(783,1,'783',"LA-705","705","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(784,1,'784',"LA-705","705","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(785,1,'785',"LA-705","705","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(786,1,'786',"LA-705","705","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(787,1,'787',"LA-705","705","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(788,1,'788',"LA-705","705","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(789,1,'789',"LA-705","705","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(790,1,'790',"LA-705","705","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54',170,'','');
insert into contract_rule values(791,1,'791',"LA-705","705","N_A","B-1","SL","N_A","N_A","N_A","15",null,"GT",'GT-9-1X',1170,'','');
insert into contract_rule values(792,1,'792',"LA-705","705","N_A","B-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(793,1,'793',"LA-705","705","N_A","B-1","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(794,1,'794',"LA-705","705","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-60',170,'','');
insert into contract_rule values(795,1,'795',"LA-705","705","N_A","B-1","DL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(796,1,'796',"LA-705","705","N_A","B-1","DL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(797,1,'797',"LA-705","705","N_A","B-1","DL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(798,1,'798',"LA-705","705","N_A","B-1","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(799,1,'799',"LA-705","705","N_A","B-1","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(801,1,'801',"LA-705","705","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54',170,'','');
insert into contract_rule values(802,1,'802',"LA-705","705","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-60',170,'','');
insert into contract_rule values(803,1,'803',"LA-705","705","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(804,1,'804',"LA-705","705","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(805,1,'805',"LA-705","705","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(806,1,'806',"LA-705","705","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(807,1,'807',"LA-705","705","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(808,1,'808',"LA-705","705","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(809,1,'809',"LA-705","705","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(810,1,'810',"LA-705","705","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');

insert into contract_rule values(812,1,'812',"LA-706","706","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(813,1,'813',"LA-706","706","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(814,1,'814',"LA-706","706","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(815,1,'815',"LA-706","706","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(816,1,'816',"LA-706","706","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(817,1,'817',"LA-706","706","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(818,1,'818',"LA-706","706","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(819,1,'819',"LA-706","706","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(820,1,'820',"LA-706","706","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(821,1,'821',"LA-706","706","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(822,1,'822',"LA-706","706","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(823,1,'823',"LA-706","706","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(824,1,'824',"LA-706","706","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(825,1,'825',"LA-706","706","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(826,1,'826',"LA-706","706","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(827,1,'827',"LA-706","706","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(828,1,'828',"LA-706","706","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49-A',170,'','');
insert into contract_rule values(829,1,'829',"LA-706","706","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(830,1,'830',"LA-706","706","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(831,1,'831',"LA-706","706","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(832,1,'832',"LA-706","706","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-60-A',170,'','');
insert into contract_rule values(833,1,'833',"LA-706","706","N_A","B","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(834,1,'834',"LA-706","706","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(835,1,'835',"LA-706","706","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(837,1,'837',"LA-706","706","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-49',170,'','');
insert into contract_rule values(838,1,'838',"LA-706","706","N_A","B-1","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(839,1,'839',"LA-706","706","N_A","B-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(840,1,'840',"LA-706","706","N_A","B-1","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(841,1,'841',"LA-706","706","N_A","B-1","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(842,1,'842',"LA-706","706","N_A","B-1","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');

insert into contract_rule values(844,1,'844',"LA-706","706","N_A","B-2","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'','');
insert into contract_rule values(845,1,'845',"LA-706","706","N_A","B-2","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-7-1X',170,'','');
insert into contract_rule values(846,1,'846',"LA-706","706","N_A","B-2","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(847,1,'847',"LA-706","706","N_A","B-2","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(848,1,'848',"LA-706","706","N_A","B-2","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(849,1,'849',"LA-706","706","N_A","B-2","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(850,1,'850',"LA-706","706","N_A","C","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54-A',170,'','');
insert into contract_rule values(851,1,'851',"LA-706","706","N_A","C","SL","N_A","N_A","N_A","15",null,"GT",'GT-9-1X',1170,'','');
insert into contract_rule values(852,1,'852',"LA-706","706","N_A","C","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(853,1,'853',"LA-706","706","N_A","C","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(854,1,'854',"LA-706","706","N_A","C","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-60-A',170,'','');
insert into contract_rule values(855,1,'855',"LA-706","706","N_A","C","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(856,1,'856',"LA-706","706","N_A","C","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(857,1,'857',"LA-706","706","N_A","C","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');

insert into contract_rule values(859,1,'859',"LA-706","706","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-LA',70,'','');
insert into contract_rule values(860,1,'860',"LA-706","706","N_A","D","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'','');
insert into contract_rule values(861,1,'861',"LA-706","706","N_A","D","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(862,1,'862',"LA-706","706","N_A","D","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(863,1,'863',"LA-706","706","N_A","D","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(864,1,'864',"LA-706","706","N_A","D","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(865,1,'865',"LA-706","706","N_A","D","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(866,1,'866',"LA-706","706","N_A","D","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(867,1,'867',"LA-706","706","N_A","D","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(868,1,'868',"LA-706","706","N_A","D","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');

insert into contract_rule values(870,1,'870',"TM-724","724","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(871,1,'871',"TM-724","724","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(872,1,'872',"TM-724","724","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(873,1,'873',"TM-724","724","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(874,1,'874',"TM-724","724","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(875,1,'875',"TM-724","724","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(876,1,'876',"TM-724","724","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(877,1,'877',"TM-724","724","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(878,1,'878',"TM-724","724","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(879,1,'879',"TM-724","724","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(880,1,'880',"TM-724","724","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(881,1,'881',"TM-724","724","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(882,1,'882',"TM-724","724","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(883,1,'883',"TM-724","724","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','local 80 Pg 26 P8 (c) 1');
insert into contract_rule values(884,1,'884',"TM-724","724","N_A","A","ZB","ON","FT","N_A","15",null,"GL",'GL-EL-14-2X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(885,1,'885',"TM-724","724","N_A","A","ZB","ON","FT","N_A","6",null,"GL",'GL-EL-14-375X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(886,1,'886',"TM-724","724","N_A","A","ZB","ON","FT","N_A","7",null,"GL",'GL-EL-14-5X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(887,1,'887',"TM-724","724","N_A","A","ZB","ON","TV","N_A","15",null,"GL",'GL-WK-14-2X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(888,1,'888',"TM-724","724","N_A","A","ZB","ON","TV","N_A","6",null,"GL",'GL-WK-14-375X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(889,1,'889',"TM-724","724","N_A","A","ZB","ON","TV","N_A","7",null,"GL",'GL-WK-14-5X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(890,1,'890',"TM-724","724","N_A","A","ZB","OP","N_A","N_A","15",null,"GL",'GL-EL-14-2X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(891,1,'891',"TM-724","724","N_A","A","ZB","OP","N_A","N_A","6",null,"GL",'GL-EL-14-375X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(892,1,'892',"TM-724","724","N_A","A","ZB","OP","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(893,1,'893',"TM-724","724","N_A","A","ST","ON","FT","N_A","15",null,"GL",'GL-EL-14-2X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(894,1,'894',"TM-724","724","N_A","A","ST","ON","FT","N_A","6",null,"GL",'GL-EL-14-3X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(895,1,'895',"TM-724","724","N_A","A","ST","ON","FT","N_A","7",null,"GL",'GL-EL-14-4X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(896,1,'896',"TM-724","724","N_A","A","ST","ON","TV","N_A","15",null,"GL",'GL-WK-14-2X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(897,1,'897',"TM-724","724","N_A","A","ST","ON","TV","N_A","6",null,"GL",'GL-WK-14-3X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(898,1,'898',"TM-724","724","N_A","A","ST","ON","TV","N_A","7",null,"GL",'GL-WK-14-4X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(899,1,'899',"TM-724","724","N_A","A","ST","OP","N_A","N_A","15",null,"GL",'GL-EL-12-2X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(900,1,'900',"TM-724","724","N_A","A","ST","OP","N_A","N_A","6",null,"GL",'GL-EL-12-3X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(901,1,'901',"TM-724","724","N_A","A","ST","OP","N_A","N_A","7",null,"GL",'GL-EL-12-4X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(902,1,'902',"TM-724","724","N_A","N_A","DL","N_A","N_A","N_A","N_A",null,"GL",'GL-EL-14-25X',130,'','');
insert into contract_rule values(903,1,'903',"TM-724","724","N_A","N_A","DL","N_A","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1130,'','');
insert into contract_rule values(904,1,'904',"TM-724","724","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-BC',30,'',' P20;  P45');
insert into contract_rule values(905,1,'905',"TM-724","724","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(906,1,'906',"TM-724","724","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(907,1,'907',"TM-724","724","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'','');
insert into contract_rule values(908,1,'908',"TM-724","724","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(909,1,'909',"TM-724","724","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(910,1,'910',"TM-724","724","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(911,1,'911',"TM-724","724","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(912,1,'912',"TM-724","724","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(913,1,'913',"TM-724","724","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(914,1,'914',"TM-724","724","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(915,1,'915',"TM-724","724","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(916,1,'916',"TM-724","724","N_A","C","ZB","N_A","N_A","N_A","N_A",null,"HO",'HO-P-5X',170,'','gold pays 5x');
insert into contract_rule values(917,1,'917',"TM-724","724","N_A","C","ST","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',170,'','gold pays 4x');

insert into contract_rule values(919,1,'919',"LA-728","728","N_A","A","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(920,1,'920',"LA-728","728","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(921,1,'921',"LA-728","728","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(922,1,'922',"LA-728","728","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(923,1,'923',"LA-728","728","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(924,1,'924',"LA-728","728","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(925,1,'925',"LA-728","728","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(926,1,'926',"LA-728","728","N_A","A","DL","N_A","N_A","N_A","16",null,"GT",'GT-9.5-1X',1170,'','');
insert into contract_rule values(927,1,'927',"LA-728","728","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(928,1,'928',"LA-728","728","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(929,1,'929',"LA-728","728","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(930,1,'930',"LA-728","728","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(931,1,'931',"LA-728","728","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(932,1,'932',"LA-728","728","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(933,1,'933',"LA-728","728","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(934,1,'934',"LA-728","728","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(935,1,'935',"LA-728","728","N_A","A","N_A","N_A","N_A","ID","N_A",null,"GT",'GT-4-4-1X',2070,'','??');
insert into contract_rule values(936,1,'936',"LA-728","728","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54',170,'','');
insert into contract_rule values(937,1,'937',"LA-728","728","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-9-1X',1170,'','');
insert into contract_rule values(938,1,'938',"LA-728","728","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(939,1,'939',"LA-728","728","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(940,1,'940',"LA-728","728","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-60',170,'','');
insert into contract_rule values(941,1,'941',"LA-728","728","N_A","B","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(942,1,'942',"LA-728","728","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(943,1,'943',"LA-728","728","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(945,1,'945',"LA-728","728","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(946,1,'946',"LA-728","728","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(947,1,'947',"LA-728","728","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(948,1,'948',"LA-728","728","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(949,1,'949',"LA-728","728","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(950,1,'950',"LA-728","728","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(951,1,'951',"LA-728","728","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(952,1,'952',"LA-728","728","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(953,1,'953',"LA-728","728","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');


insert into contract_rule values(956,1,'956',"LA-729","729","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',30,'','');
insert into contract_rule values(957,1,'957',"LA-729","729","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',30,'','');
insert into contract_rule values(958,1,'958',"LA-729","729","N_A","N_A","SB","OP","N_A","N_A","N_A",null,"RS",'RST-8-PR',330,'Off Prod, on stage or outside of zone','Pg 31, P10, top para');
insert into contract_rule values(959,1,'959',"LA-729","729","N_A","N_A","SL","N_A","N_A","N_A","N_A",null,"RS",'RST-10-PR',130,'Studio Zone, off stage','Pg 31, P10, top para');
insert into contract_rule values(960,1,'960',"LA-729","729","N_A","N_A","SB","ON","N_A","N_A","N_A",null,"RS",'RST-9-PR',330,'On Prod, on stage or outside of zone','Pg 31, P10, top para');
insert into contract_rule values(961,1,'961',"LA-729","729","N_A","N_A","SL","N_A","N_A","N_A","7",null,"RS",'RST-8-PR',1130,'Callback in 7th day gold after midn.','Pg 33, P11, b (2nd para)');


insert into contract_rule values(964,1,'964',"LA-729","729","N_A","N_A","DL","N_A","N_A","N_A","N_A",null,"NP",'NP-NONE',130,'No Night Premiums on Distant','Pg 55, P46');

insert into contract_rule values(966,1,'966',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-STD-40',170,'','Pg 14. P1 (a) (1) Schedule A chart');
insert into contract_rule values(967,1,'967',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(968,1,'968',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',170,'','');
insert into contract_rule values(969,1,'969',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(970,1,'970',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(971,1,'971',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(972,1,'972',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(973,1,'973',"LA-729","729","N_A","A","SL","OP","N_A","N_A","N_A",null,"NP",'NP-LA-STD',370,'','Pg 22, P5 a-c');
insert into contract_rule values(974,1,'974',"LA-729","729","N_A","A","SL","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2170,'Safety training, 4 hr Guar','Pg 23, P6 (e)');

insert into contract_rule values(976,1,'976',"LA-729","729","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20.0',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','Pg 24 P8 (c) 1');
insert into contract_rule values(977,1,'977',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',170,'If employee works past midnight into a Holiday.  This is handled by the standard Holiday rule.','Pg 24 P8 (c) 2');
insert into contract_rule values(978,1,'978',"LA-729","729","N_A","A","SL","OP","N_A","N_A","6",'SH=Y',"GT",'GT-8-1X',1370,'Sched shift intrudes on days off. 1.5x is paid.','Pg 25 P8 e2(i) B, C or A');
insert into contract_rule values(979,1,'979',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(980,1,'980',"LA-729","729","N_A","A","SL","ON","N_A","N_A","7",'SH=Y;WD=Mon',"GT",'GT-8-1X',1370,'Sched shift, but intervening Sunday results in regular OT on first workday of new sched.  MAY BE CONTRADICTED BY A FUTURE RULE.','Pg 25, P8 e2(ii) Note: MM says this rule is not used in real life.');
insert into contract_rule values(981,1,'981',"LA-729","729","N_A","A","SL","N_A","N_A","T4","N_A",null,"GT",'GT-4-4-1X',2170,'Sched shift and travel intrudes upon the days off.  No premium is paid.','Pg 25, P8 e2(iii)');
insert into contract_rule values(982,1,'982',"LA-729","729","N_A","A","SL","N_A","N_A","T8","N_A",null,"GT",'GT-8-1X',2170,'Sched shift and travel intrudes upon the days off.  No premium is paid.','Pg 25, P8 e2(iii)');



insert into contract_rule values(986,1,'986',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',170,'Check gold.','Pg. 27, (d1)');
insert into contract_rule values(987,1,'987',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","15",null,"CB",'CB-4-15X',1170,'Callback on days 1-5, non-holiday','Pg 31, P10, 2nd para & table');
insert into contract_rule values(988,1,'988',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","67",null,"CB",'CB-3-2X',1170,'Callback on days 6-7','Pg 31, P10, 2nd para & table');
insert into contract_rule values(989,1,'989',"LA-729","729","N_A","A","SL","N_A","N_A","HW","N_A",null,"CB",'CB-3-2X',2170,'Callback on Holidays','Pg 31, P10, 2nd para & table');
insert into contract_rule values(990,1,'990',"LA-729","729","N_A","A","ZB","OP","N_A","N_A","15",null,"GL",'GL-EL-14-2X',1370,'Non-holiday','Pg 32, P11, a1');
insert into contract_rule values(991,1,'991',"LA-729","729","N_A","A","ZB","OP","N_A","N_A","6",null,"GL",'GL-EL-14-3X',1370,'','Pg 32, P11, a1');
insert into contract_rule values(992,1,'992',"LA-729","729","N_A","A","ZB","OP","N_A","N_A","7",null,"GL",'GL-EL-14-4X',1370,'','Pg 32, P11, a1');
insert into contract_rule values(993,1,'993',"LA-729","729","N_A","A","ST","OP","N_A","N_A","15",null,"GL",'GL-EL-12-2X',1370,'Non-holiday','Pg 32, P11, a2');
insert into contract_rule values(994,1,'994',"LA-729","729","N_A","A","ST","OP","N_A","N_A","6",null,"GL",'GL-EL-12-3X',1370,'','Pg 32, P11, a2');
insert into contract_rule values(995,1,'995',"LA-729","729","N_A","A","ST","OP","N_A","N_A","7",null,"GL",'GL-EL-12-4X',1370,'','Pg 32, P11, a2');
insert into contract_rule values(996,1,'996',"LA-729","729","N_A","A","ZB","ON","FT","N_A","15",null,"GL",'GL-EL-14-2X',1570,'Non-holiday','Pg 32, P11, a1-3');
insert into contract_rule values(997,1,'997',"LA-729","729","N_A","A","ZB","ON","FT","N_A","6",null,"GL",'GL-EL-14-3X',1570,'','Pg 32, P11, a1-3');
insert into contract_rule values(998,1,'998',"LA-729","729","N_A","A","ZB","ON","FT","N_A","7",null,"GL",'GL-EL-14-4X',1570,'','Pg 32, P11, a1-3');
insert into contract_rule values(999,1,'999',"LA-729","729","N_A","A","ST","ON","FT","N_A","15",null,"GL",'GL-EL-12-2X',1570,'Non-holiday','Pg 32, P11, a1-3');
insert into contract_rule values(1000,1,'1000',"LA-729","729","N_A","A","ST","ON","FT","N_A","6",null,"GL",'GL-EL-12-3X',1570,'','Pg 32, P11, a1-3');
insert into contract_rule values(1001,1,'1001',"LA-729","729","N_A","A","ST","ON","FT","N_A","7",null,"GL",'GL-EL-12-4X',1570,'','Pg 32, P11, a1-3');
insert into contract_rule values(1002,1,'1002',"LA-729","729","N_A","A","ZB","ON","TV","N_A","15",null,"GL",'GL-WK-14-2X',1570,'Non-holiday','Pg 32, P11, a1-3');
insert into contract_rule values(1003,1,'1003',"LA-729","729","N_A","A","ZB","ON","TV","N_A","6",null,"GL",'GL-WK-14-3X',1570,'','Pg 32, P11, a1-3');
insert into contract_rule values(1004,1,'1004',"LA-729","729","N_A","A","ZB","ON","TV","N_A","7",null,"GL",'GL-WK-14-4X',1570,'','Pg 32, P11, a1-3');
insert into contract_rule values(1005,1,'1005',"LA-729","729","N_A","A","ST","ON","TV","N_A","15",null,"GL",'GL-WK-12-2X',1570,'Non-holiday','Pg 32, P11, a1-3');
insert into contract_rule values(1006,1,'1006',"LA-729","729","N_A","A","ST","ON","TV","N_A","6",null,"GL",'GL-WK-12-3X',1570,'','Pg 32, P11, a1-3');
insert into contract_rule values(1007,1,'1007',"LA-729","729","N_A","A","ST","ON","TV","N_A","7",null,"GL",'GL-WK-12-4X',1570,'','Pg 32, P11, a1-3');
insert into contract_rule values(1008,1,'1008',"LA-729","729","N_A","A","SL","N_A","N_A","N_A","N_A",null,"MP",'MP-LA',170,'','Pg. 36, P20 a');
insert into contract_rule values(1009,1,'1009',"LA-729","729","N_A","A","ST","N_A","TV","N_A","N_A",null,"MP",'MP-LA-STG-MOW',370,'','Pg. 38, P20 h2');
insert into contract_rule values(1010,1,'1010',"LA-729","729","N_A","A","SL","ON","N_A","N_A","N_A",'FR=Y',"MP",'MP-NONE',370,'French Hours.  No rule required.','Pg. 39, P20 i');
insert into contract_rule values(1011,1,'1011',"LA-729","729","N_A","A","ZN","OP","N_A","N_A","15",null,"GL",'GL-EL-12-2X',1370,'Golden hour Zone rule-supersedes pg 32','Pg. 40, P23 b2');
insert into contract_rule values(1012,1,'1012',"LA-729","729","N_A","A","ZN","OP","N_A","N_A","6",null,"GL",'GL-EL-12-3X',1370,'Golden hour Zone rule-supersedes pg 32','Pg. 40, P23 b2');
insert into contract_rule values(1013,1,'1013',"LA-729","729","N_A","A","ZN","OP","N_A","N_A","7",null,"GL",'GL-EL-12-4X',1370,'Golden hour Zone rule-supersedes pg 32','Pg. 40, P23 b2');
insert into contract_rule values(1014,1,'1014',"LA-729","729","N_A","A","ZN","ON","FT","N_A","15",null,"GL",'GL-EL-12-2X',1570,'Golden hour Zone rule-supersedes pg 32','Pg. 40, P23 b2');
insert into contract_rule values(1015,1,'1015',"LA-729","729","N_A","A","ZN","ON","FT","N_A","6",null,"GL",'GL-EL-12-3X',1570,'Golden hour Zone rule-supersedes pg 32','Pg. 40, P23 b2');
insert into contract_rule values(1016,1,'1016',"LA-729","729","N_A","A","ZN","ON","FT","N_A","7",null,"GL",'GL-EL-12-4X',1570,'Golden hour Zone rule-supersedes pg 32','Pg. 40, P23 b2');
insert into contract_rule values(1017,1,'1017',"LA-729","729","N_A","A","ZN","ON","TV","N_A","15",null,"GL",'GL-WK-12-2X',1570,'Golden hour Zone rule-supersedes pg 32','Pg. 40, P23 b2');
insert into contract_rule values(1018,1,'1018',"LA-729","729","N_A","A","ZN","ON","TV","N_A","6",null,"GL",'GL-WK-12-3X',1570,'Golden hour Zone rule-supersedes pg 32','Pg. 40, P23 b2');
insert into contract_rule values(1019,1,'1019',"LA-729","729","N_A","A","ZN","ON","TV","N_A","7",null,"GL",'GL-WK-12-4X',1570,'Golden hour Zone rule-supersedes pg 32','Pg. 40, P23 b2');

insert into contract_rule values(1021,1,'1021',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','Pg 43, P31 a1 Sched 01 chart');
insert into contract_rule values(1022,1,'1022',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","N_A",null,"OT",'OT-8-15X',170,'','Pg 43, P31 a1 Sched 01 chart');
insert into contract_rule values(1023,1,'1023',"LA-729","729","N_A","A","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'6th or 7th day IDLE','Pg 47, P31, e (see table) & Pg49 P32b');
insert into contract_rule values(1024,1,'1024',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'7th day worked, Sched 01. (8 hr guar)','Pg 47, P31, e (see table)');
insert into contract_rule values(1025,1,'1025',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1026,1,'1026',"LA-729","729","N_A","A","N_A","N_A","N_A","TR","N_A",null,"GT",'GT-4-8-1X',2070,'Travel Day','Pg 51, P39 a');
insert into contract_rule values(1027,1,'1027',"LA-729","729","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','Pg 51 P39 c');
insert into contract_rule values(1028,1,'1028',"LA-729","729","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','Pg 51 P39 c');
insert into contract_rule values(1029,1,'1029',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","N_A",null,"RS",'RST-8-PR',170,'Invaded Rest: 1.5x on callback','Pg 53 P41');
insert into contract_rule values(1030,1,'1030',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","16",null,"CB",'CB-4-15X',1170,'Note: TBD: If, after callback, the employee is stuck in 1.5x or if golden hours is possible.','Pg 53 P41 table');
insert into contract_rule values(1031,1,'1031',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","7",null,"CB",'CB-3-2X',1170,'','Pg 53 P41 table');
insert into contract_rule values(1032,1,'1032',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","16",null,"GL",'GL-EL-14-2X',1170,'','Pg 54 P44 b');
insert into contract_rule values(1033,1,'1033',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-EL-14-4X',1170,'','Pg 54 P44 b');
insert into contract_rule values(1034,1,'1034',"LA-729","729","N_A","A","DL","N_A","N_A","HW","N_A",null,"GL",'GL-EL-14-4X',2170,'','Pg 54 P44 b');
insert into contract_rule values(1035,1,'1035',"LA-729","729","N_A","A","DL","N_A","N_A","N_A","N_A",null,"MP",'MP-LA-D',170,'MPV Distant','Pg 54 P45 a, b');
insert into contract_rule values(1036,1,'1036',"LA-729","729","N_A","A","DL","ON","N_A","N_A","N_A",'FR=Y',"MP",'MP-NONE',370,'French Hours.  No MPV allowed.','Pg 55, P45 d');

insert into contract_rule values(1038,1,'1038',"LA-729","729","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(1039,1,'1039',"LA-729","729","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(1040,1,'1040',"LA-729","729","N_A","D","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(1041,1,'1041',"LA-729","729","N_A","D","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(1042,1,'1042',"LA-729","729","N_A","C","SL","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',170,'','Page 20, P1 b1');
insert into contract_rule values(1043,1,'1043',"LA-729","729","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','Page 21, P1 (b2)');
insert into contract_rule values(1044,1,'1044',"LA-729","729","N_A","C","SL","N_A","N_A","N_A","56",'EH>=15;WR>25',"SP",'SP_PAY_NXT_WRK',1170,'6th & 7th days begin at 1am the morning of the 6th or 7th day if the employee has worked 15 hours elapsed as of 1am.','Page 21, P1, b 2, 2nd para');
insert into contract_rule values(1045,1,'1045',"LA-729","729","N_A","C","SL","N_A","N_A","N_A","N_A",'NXH=Y;EH>=15;WR>25',"SP",'SP_PAY_NXT_WRK',170,'','Page 21, P1, b 2, 2nd para');
insert into contract_rule values(1046,1,'1046',"LA-729","729","N_A","D","SL","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',170,'','Page 20, P1 b1');
insert into contract_rule values(1047,1,'1047',"LA-729","729","N_A","D","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-57-S',170,'Overtime','Page 21, P1 (b2)');
insert into contract_rule values(1048,1,'1048',"LA-729","729","N_A","D","SL","N_A","N_A","N_A","56",'EH>=15;WR>25',"SP",'SP_PAY_NXT_WRK',1170,'6th & 7th days begin at 1am the morning of the 6th or 7th day if the employee has worked 15 hours elapsed as of 1am.','Page 21, P1, b 2, 2nd para');
insert into contract_rule values(1049,1,'1049',"LA-729","729","N_A","D","SL","N_A","N_A","N_A","N_A",'NXH=Y;EH>=15;WR>25',"SP",'SP_PAY_NXT_WRK',170,'','Page 21, P1, b 2, 2nd para');

insert into contract_rule values(1051,1,'1051',"LA-729","729","N_A","C","SL","N_A","N_A","HA","N_A",null,"OC",'OC-NO-PAY',2170,'Employee leaves work on own accord, relinquishes 1/5W.','Pg 24, P8 (d)');

insert into contract_rule values(1053,1,'1053',"LA-729","729","N_A","C","DL","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P-D',170,'','Pg 28, P9 e2');
insert into contract_rule values(1054,1,'1054',"LA-729","729","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');

insert into contract_rule values(1056,1,'1056',"LA-729","729","N_A","C","DL","N_A","N_A","N_A","6",null,"OC",'OC-56-D',1170,'6/7 day WORKED','Pg 47, P31, f 1 (i, iv)');
insert into contract_rule values(1057,1,'1057',"LA-729","729","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'6/7 day IDLE','Pg 47, P31, f 1 (ii, iii)');
insert into contract_rule values(1058,1,'1058',"LA-729","729","N_A","C","N_A","N_A","N_A","N_A","N_A",'WRKD<6;MXW=Y',"SP",'SP_PART_WK_GUAR',70,'Partial Workweek pro-rate/guar studio','Pg 48, P31 f2');
insert into contract_rule values(1059,1,'1059',"LA-729","729","N_A","C","DL","N_A","N_A","TR","N_A",null,"OC",'OC-1/6',2170,'Travel Day','Pg 51, P39 a');
insert into contract_rule values(1060,1,'1060',"LA-729","729","N_A","D","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');

insert into contract_rule values(1062,1,'1062',"LA-729","729","N_A","D","DL","N_A","N_A","N_A","6",null,"OC",'OC-56-D',1170,'6/7 day WORKED','Pg 47, P31, f 1 (i, iv)');
insert into contract_rule values(1063,1,'1063',"LA-729","729","N_A","D","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'6/7 day IDLE','Pg 47, P31, f 1 (ii, iii)');
insert into contract_rule values(1064,1,'1064',"LA-729","729","N_A","D","N_A","N_A","N_A","N_A","N_A",'WRKD<6;MXW=Y',"SP",'SP_PART_WK_GUAR',70,'Partial Workweek pro-rate/guar studio','Pg 48, P31 f2');
insert into contract_rule values(1065,1,'1065',"LA-729","729","N_A","D","DL","N_A","N_A","TR","N_A",null,"OC",'OC-1/6',2170,'Travel Day','Pg 51, P39 a');
insert into contract_rule values(1066,1,'1066',"LA-729","729","N_A","A? C?","DL","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',170,'Is this for "on-call" people? Callback for WEEKLY employees.  Is the callback min guar 1/12W?','Pg 53 P41 table');























insert into contract_rule values(1090,1,'1090',"TM-755","755","N_A","A","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1091,1,'1091',"TM-755","755","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1092,1,'1092',"TM-755","755","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1093,1,'1093',"TM-755","755","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1094,1,'1094',"TM-755","755","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1095,1,'1095',"TM-755","755","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1096,1,'1096',"TM-755","755","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','local 80 Pg 26 P8 (c) 1');
insert into contract_rule values(1097,1,'1097',"TM-755","755","N_A","A","DL","N_A","N_A","N_A","16",null,"GT",'GT-9.5-1X',1170,'','');
insert into contract_rule values(1098,1,'1098',"TM-755","755","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1099,1,'1099',"TM-755","755","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1100,1,'1100',"TM-755","755","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1101,1,'1101',"TM-755","755","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1102,1,'1102',"TM-755","755","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1103,1,'1103',"TM-755","755","N_A","A","ZB","ON","FT","N_A","15",null,"GL",'GL-EL-14-2X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(1104,1,'1104',"TM-755","755","N_A","A","ZB","ON","FT","N_A","6",null,"GL",'GL-EL-14-375X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(1105,1,'1105',"TM-755","755","N_A","A","ZB","ON","FT","N_A","7",null,"GL",'GL-EL-14-5X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(1106,1,'1106',"TM-755","755","N_A","A","ZB","ON","TV","N_A","15",null,"GL",'GL-WK-14-2X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(1107,1,'1107',"TM-755","755","N_A","A","ZB","ON","TV","N_A","6",null,"GL",'GL-WK-14-375X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(1108,1,'1108',"TM-755","755","N_A","A","ZB","ON","TV","N_A","7",null,"GL",'GL-WK-14-5X',1570,'','p96, P11(a)(1),(3)');
insert into contract_rule values(1109,1,'1109',"TM-755","755","N_A","A","ZB","OP","N_A","N_A","15",null,"GL",'GL-EL-14-2X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(1110,1,'1110',"TM-755","755","N_A","A","ZB","OP","N_A","N_A","6",null,"GL",'GL-EL-14-375X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(1111,1,'1111',"TM-755","755","N_A","A","ZB","OP","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1370,'','p96, P11(a)(1)');
insert into contract_rule values(1112,1,'1112',"TM-755","755","N_A","A","ST","ON","FT","N_A","15",null,"GL",'GL-EL-14-2X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(1113,1,'1113',"TM-755","755","N_A","A","ST","ON","FT","N_A","6",null,"GL",'GL-EL-14-3X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(1114,1,'1114',"TM-755","755","N_A","A","ST","ON","FT","N_A","7",null,"GL",'GL-EL-14-4X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(1115,1,'1115',"TM-755","755","N_A","A","ST","ON","TV","N_A","15",null,"GL",'GL-WK-14-2X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(1116,1,'1116',"TM-755","755","N_A","A","ST","ON","TV","N_A","6",null,"GL",'GL-WK-14-3X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(1117,1,'1117',"TM-755","755","N_A","A","ST","ON","TV","N_A","7",null,"GL",'GL-WK-14-4X',1570,'','p96, P11(a)(2),(3)');
insert into contract_rule values(1118,1,'1118',"TM-755","755","N_A","A","ST","OP","N_A","N_A","15",null,"GL",'GL-EL-12-2X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(1119,1,'1119',"TM-755","755","N_A","A","ST","OP","N_A","N_A","6",null,"GL",'GL-EL-12-3X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(1120,1,'1120',"TM-755","755","N_A","A","ST","OP","N_A","N_A","7",null,"GL",'GL-EL-12-4X',1370,'','p96, P11(a)(2)');
insert into contract_rule values(1121,1,'1121',"TM-755","755","N_A","N_A","DL","N_A","N_A","N_A","N_A",null,"GL",'GL-EL-14-25X',130,'','');
insert into contract_rule values(1122,1,'1122',"TM-755","755","N_A","N_A","DL","N_A","N_A","N_A","7",null,"GL",'GL-EL-14-5X',1130,'','');
insert into contract_rule values(1123,1,'1123',"TM-755","755","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-BC',30,'',' P20;  P45');
insert into contract_rule values(1124,1,'1124',"TM-755","755","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(1125,1,'1125',"TM-755","755","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(1126,1,'1126',"TM-755","755","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(1127,1,'1127',"TM-755","755","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1128,1,'1128',"TM-755","755","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(1129,1,'1129',"TM-755","755","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(1130,1,'1130',"TM-755","755","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(1131,1,'1131',"TM-755","755","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(1132,1,'1132',"TM-755","755","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(1133,1,'1133',"TM-755","755","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'no MPV for on-call','');
insert into contract_rule values(1134,1,'1134',"TM-755","755","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'no gold for on-call','');

insert into contract_rule values(1136,1,'1136',"LA-767","767","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(1137,1,'1137',"LA-767","767","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1138,1,'1138',"LA-767","767","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1139,1,'1139',"LA-767","767","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1140,1,'1140',"LA-767","767","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1141,1,'1141',"LA-767","767","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1142,1,'1142',"LA-767","767","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1143,1,'1143',"LA-767","767","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(1144,1,'1144',"LA-767","767","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1145,1,'1145',"LA-767","767","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1146,1,'1146',"LA-767","767","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1147,1,'1147',"LA-767","767","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1148,1,'1148',"LA-767","767","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1149,1,'1149',"LA-767","767","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1150,1,'1150',"LA-767","767","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1151,1,'1151',"LA-767","767","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1152,1,'1152',"LA-767","767","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-43',170,'','');
insert into contract_rule values(1153,1,'1153',"LA-767","767","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-7-1X',1170,'','');
insert into contract_rule values(1154,1,'1154',"LA-767","767","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1155,1,'1155',"LA-767","767","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1156,1,'1156',"LA-767","767","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-57-6',170,'','');
insert into contract_rule values(1157,1,'1157',"LA-767","767","N_A","B","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1158,1,'1158',"LA-767","767","N_A","B","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');
insert into contract_rule values(1159,1,'1159',"LA-767","767","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(1160,1,'1160',"LA-767","767","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');


insert into contract_rule values(1163,1,'1163',"LA-800A","800A","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(1164,1,'1164',"LA-800A","800A","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1165,1,'1165',"LA-800A","800A","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(1166,1,'1166',"LA-800A","800A","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(1167,1,'1167',"LA-800A","800A","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(1168,1,'1168',"LA-800A","800A","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(1169,1,'1169',"LA-800A","800A","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(1170,1,'1170',"LA-800A","800A","N_A","C","N_A","N_A","N_A","TR","N_A",null,"GT",'GT-4-8-1X',2070,'','??');
insert into contract_rule values(1171,1,'1171',"LA-800A","800A","N_A","C","N_A","N_A","N_A","ID","N_A",null,"GT",'GT-4-4-1X',2070,'','??');
insert into contract_rule values(1172,1,'1172',"LA-800A","800A","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'no MPV for on-call','');
insert into contract_rule values(1173,1,'1173',"LA-800A","800A","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'no gold for on-call','');

insert into contract_rule values(1175,1,'1175',"LA-800D","800D","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(1176,1,'1176',"LA-800D","800D","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1177,1,'1177',"LA-800D","800D","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1178,1,'1178',"LA-800D","800D","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1179,1,'1179',"LA-800D","800D","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1180,1,'1180',"LA-800D","800D","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1181,1,'1181',"LA-800D","800D","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1182,1,'1182',"LA-800D","800D","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(1183,1,'1183',"LA-800D","800D","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1184,1,'1184',"LA-800D","800D","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1185,1,'1185',"LA-800D","800D","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1186,1,'1186',"LA-800D","800D","N_A","A","DL","N_A","N_A","N_A","7",null,"OT",'OT-NONE',1170,'','');
insert into contract_rule values(1187,1,'1187',"LA-800D","800D","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1188,1,'1188',"LA-800D","800D","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1189,1,'1189',"LA-800D","800D","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1190,1,'1190',"LA-800D","800D","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1191,1,'1191',"LA-800D","800D","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1192,1,'1192',"LA-800D","800D","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'','');
insert into contract_rule values(1193,1,'1193',"LA-800D","800D","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1194,1,'1194',"LA-800D","800D","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1195,1,'1195',"LA-800D","800D","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1196,1,'1196',"LA-800D","800D","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'','');
insert into contract_rule values(1197,1,'1197',"LA-800D","800D","N_A","B","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1198,1,'1198',"LA-800D","800D","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(1199,1,'1199',"LA-800D","800D","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(1201,1,'1201',"LA-800D","800D","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'','');

insert into contract_rule values(1203,1,'1203',"LA-800M","800M","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',170,'','');
insert into contract_rule values(1204,1,'1204',"LA-800M","800M","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1205,1,'1205',"LA-800M","800M","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1206,1,'1206',"LA-800M","800M","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1207,1,'1207',"LA-800M","800M","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1208,1,'1208',"LA-800M","800M","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1209,1,'1209',"LA-800M","800M","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-9.5-1X',170,'','');
insert into contract_rule values(1210,1,'1210',"LA-800M","800M","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1211,1,'1211',"LA-800M","800M","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1212,1,'1212',"LA-800M","800M","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1213,1,'1213',"LA-800M","800M","N_A","A","DL","N_A","N_A","N_A","7",null,"OT",'OT-NONE',1170,'','');
insert into contract_rule values(1214,1,'1214',"LA-800M","800M","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1215,1,'1215',"LA-800M","800M","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1216,1,'1216',"LA-800M","800M","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','local 80 Pg 26 P8 (c) 1');
insert into contract_rule values(1217,1,'1217',"LA-800M","800M","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1218,1,'1218',"LA-800M","800M","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1219,1,'1219',"LA-800M","800M","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'','');
insert into contract_rule values(1220,1,'1220',"LA-800M","800M","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1221,1,'1221',"LA-800M","800M","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1222,1,'1222',"LA-800M","800M","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1223,1,'1223',"LA-800M","800M","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40-6',170,'','');
insert into contract_rule values(1224,1,'1224',"LA-800M","800M","N_A","B","DL","N_A","N_A","N_A","16",null,"GT",'GT-6-1X',1170,'','');
insert into contract_rule values(1225,1,'1225',"LA-800M","800M","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(1226,1,'1226',"LA-800M","800M","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(1228,1,'1228',"LA-800M","800M","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'','');
insert into contract_rule values(1229,1,'1229',"LA-800M","800M","N_A","B-1","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1230,1,'1230',"LA-800M","800M","N_A","B-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1231,1,'1231',"LA-800M","800M","N_A","B-1","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1232,1,'1232',"LA-800M","800M","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-48-6',170,'','');
insert into contract_rule values(1233,1,'1233',"LA-800M","800M","N_A","B-1","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1234,1,'1234',"LA-800M","800M","N_A","B-1","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(1235,1,'1235',"LA-800M","800M","N_A","B-1","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');

insert into contract_rule values(1237,1,'1237',"LA-800M","800M","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(1238,1,'1238',"LA-800M","800M","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1239,1,'1239',"LA-800M","800M","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(1240,1,'1240',"LA-800M","800M","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(1241,1,'1241',"LA-800M","800M","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(1242,1,'1242',"LA-800M","800M","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(1243,1,'1243',"LA-800M","800M","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(1244,1,'1244',"LA-800M","800M","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'no MPV for on-call','');
insert into contract_rule values(1245,1,'1245',"LA-800M","800M","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'no gold for on-call','');
insert into contract_rule values(1246,1,'1246',"LA-800M","800M","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'','');
insert into contract_rule values(1247,1,'1247',"LA-800M","800M","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',70,'','');
insert into contract_rule values(1248,1,'1248',"LA-800M","800M","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');

insert into contract_rule values(1250,1,'1250',"LA-800S","800S","N_A","A","SL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1251,1,'1251',"LA-800S","800S","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1252,1,'1252',"LA-800S","800S","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1253,1,'1253',"LA-800S","800S","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1254,1,'1254',"LA-800S","800S","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1255,1,'1255',"LA-800S","800S","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1256,1,'1256',"LA-800S","800S","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1257,1,'1257',"LA-800S","800S","N_A","A","DL","N_A","N_A","N_A","16",null,"GT",'GT-9.5-1X',1170,'','');
insert into contract_rule values(1258,1,'1258',"LA-800S","800S","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1259,1,'1259',"LA-800S","800S","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1260,1,'1260',"LA-800S","800S","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1261,1,'1261',"LA-800S","800S","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1262,1,'1262',"LA-800S","800S","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1263,1,'1263',"LA-800S","800S","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1264,1,'1264',"LA-800S","800S","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1265,1,'1265',"LA-800S","800S","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1266,1,'1266',"LA-800S","800S","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'','');
insert into contract_rule values(1267,1,'1267',"LA-800S","800S","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-6-1X',1170,'','');
insert into contract_rule values(1268,1,'1268',"LA-800S","800S","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1269,1,'1269',"LA-800S","800S","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1270,1,'1270',"LA-800S","800S","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',170,'','');
insert into contract_rule values(1271,1,'1271',"LA-800S","800S","N_A","B","DL","N_A","N_A","N_A","16",null,"GT",'GT-6-1X',1170,'','');
insert into contract_rule values(1272,1,'1272',"LA-800S","800S","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');

insert into contract_rule values(1274,1,'1274',"LA-800S","800S","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');
insert into contract_rule values(1275,1,'1275',"LA-800S","800S","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1276,1,'1276',"LA-800S","800S","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(1277,1,'1277',"LA-800S","800S","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(1278,1,'1278',"LA-800S","800S","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(1279,1,'1279',"LA-800S","800S","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(1280,1,'1280',"LA-800S","800S","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'no MPV for on-call','');
insert into contract_rule values(1281,1,'1281',"LA-800S","800S","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'no gold for on-call','');

insert into contract_rule values(1283,1,'1283',"LA-839","839","N_A","X-40","N_A","N_A","N_A","N_A","15",null,"BA",'WK-CUME-40',1070,'','p17 5(A)(1)');
insert into contract_rule values(1284,1,'1284',"LA-839","839","N_A","X-40","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-4-1X',70,'','p17 5(A)(1)');
insert into contract_rule values(1285,1,'1285',"LA-839","839","N_A","X-40","N_A","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1070,'','p17 5(A)(1)');
insert into contract_rule values(1286,1,'1286',"LA-839","839","N_A","X-40","N_A","N_A","N_A","N_A","6",null,"GT",'GT-4-1X',1070,'','p18 5(A)(2)');
insert into contract_rule values(1287,1,'1287',"LA-839","839","N_A","X-40","N_A","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1070,'','');
insert into contract_rule values(1288,1,'1288',"LA-839","839","N_A","X-40","N_A","N_A","N_A","N_A","7",null,"GT",'GT-4-1X',1070,'','p18 5(A)(2)');
insert into contract_rule values(1289,1,'1289',"LA-839","839","N_A","X-40","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');
insert into contract_rule values(1290,1,'1290',"LA-839","839","N_A","N_A","N_A","N_A","N_A","N_A","15",null,"GL",'GL-EL-14-2X',1999,'','');
insert into contract_rule values(1291,1,'1291',"LA-839","839","N_A","X-4","N_A","N_A","N_A","N_A","15",null,"BA",'WK-STD-40',1070,'','Federal');
insert into contract_rule values(1292,1,'1292',"LA-839","839","N_A","X-4","N_A","N_A","N_A","N_A","15",null,"GT",'GT-4-1X',1070,'','p19 5(B)(1)');
insert into contract_rule values(1293,1,'1293',"LA-839","839","N_A","X-4","N_A","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1070,'','p19 5(B)(1)');
insert into contract_rule values(1294,1,'1294',"LA-839","839","N_A","X-4","N_A","N_A","N_A","N_A","6",null,"GT",'GT-4-1X',1070,'','p18 5(A)(2)?');
insert into contract_rule values(1295,1,'1295',"LA-839","839","N_A","X-4","N_A","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1070,'','');
insert into contract_rule values(1296,1,'1296',"LA-839","839","N_A","X-4","N_A","N_A","N_A","N_A","7",null,"GT",'GT-4-1X',1070,'','p18 5(A)(2)?');
insert into contract_rule values(1297,1,'1297',"LA-839","839","N_A","X-4","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');
insert into contract_rule values(1298,1,'1298',"LA-839","839","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-EL-14-2X',30,'','p20 5(D)');
insert into contract_rule values(1299,1,'1299',"LA-839","839","N_A","N_A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',30,'','not in contract');
insert into contract_rule values(1300,1,'1300',"LA-839","839","N_A","N_A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2030,'','');
insert into contract_rule values(1301,1,'1301',"LA-839","839","N_A","N_A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2030,'','');
insert into contract_rule values(1302,1,'1302',"LA-839","839","N_A","N_A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2030,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1303,1,'1303',"LA-839","839","N_A","N_A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2030,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1304,1,'1304',"LA-839","839","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'','');
insert into contract_rule values(1305,1,'1305',"LA-839","839","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',70,'','');
insert into contract_rule values(1306,1,'1306',"LA-839","839","N_A","X-OC","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');

insert into contract_rule values(1308,1,'1308',"LA-871","871A","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'','anything to get C to work');
insert into contract_rule values(1309,1,'1309',"LA-871","871A","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1310,1,'1310',"LA-871","871A","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');

insert into contract_rule values(1312,1,'1312',"LA-871","871P","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'','anything to get C to work');
insert into contract_rule values(1313,1,'1313',"LA-871","871P","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1314,1,'1314',"LA-871","871P","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');

insert into contract_rule values(1316,1,'1316',"LA-871","871S","N_A","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-10.5-1X',170,'','');
insert into contract_rule values(1317,1,'1317',"LA-871","871S","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-6-15X',1170,'','');
insert into contract_rule values(1318,1,'1318',"LA-871","871S","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1319,1,'1319',"LA-871","871S","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1320,1,'1320',"LA-871","871S","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1321,1,'1321',"LA-871","871S","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1322,1,'1322',"LA-871","871S","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1323,1,'1323',"LA-871","871S","7704","A","SL","N_A","N_A","N_A","N_A",null,"GT",'GT-8-1X',250,'','');
insert into contract_rule values(1324,1,'1324',"LA-871","871S","7704","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1250,'','');
insert into contract_rule values(1325,1,'1325',"LA-871","871S","N_A","A","DL","N_A","N_A","N_A","N_A",null,"GT",'GT-10.5-1X',170,'','');
insert into contract_rule values(1326,1,'1326',"LA-871","871S","N_A","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-6-15X',1170,'','');
insert into contract_rule values(1327,1,'1327',"LA-871","871S","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1328,1,'1328',"LA-871","871S","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1329,1,'1329',"LA-871","871S","7704","A","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1250,'','');
insert into contract_rule values(1330,1,'1330',"LA-871","871S","7704","A","DL","N_A","N_A","N_A","16",null,"OT",'OT-8-15X',1250,'','');
insert into contract_rule values(1331,1,'1331',"LA-871","871S","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1332,1,'1332',"LA-871","871S","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1333,1,'1333',"LA-871","871S","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1334,1,'1334',"LA-871","871S","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1335,1,'1335',"LA-871","871S","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1336,1,'1336',"LA-871","871S","N_A","B","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-54',170,'','');
insert into contract_rule values(1337,1,'1337',"LA-871","871S","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-9-1X',1170,'','');
insert into contract_rule values(1338,1,'1338',"LA-871","871S","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1339,1,'1339',"LA-871","871S","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1340,1,'1340',"LA-871","871S","N_A","B","DL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-60',170,'','');
insert into contract_rule values(1341,1,'1341',"LA-871","871S","N_A","B","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1342,1,'1342',"LA-871","871S","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(1343,1,'1343',"LA-871","871S","N_A","B","N_A","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1070,'','');
insert into contract_rule values(1344,1,'1344',"LA-871","871S","N_A","B","N_A","N_A","N_A","N_A","N_A",null,"HO",'HO-STD-P',70,'','');


insert into contract_rule values(1347,1,'1347',"LA-884","884","N_A","A","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1348,1,'1348',"LA-884","884","N_A","A","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1349,1,'1349',"LA-884","884","N_A","A","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1350,1,'1350',"LA-884","884","N_A","A","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1351,1,'1351',"LA-884","884","N_A","A","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1352,1,'1352',"LA-884","884","N_A","A","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1353,1,'1353',"LA-884","884","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1354,1,'1354',"LA-884","884","N_A","A","DL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1355,1,'1355',"LA-884","884","N_A","A","DL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1356,1,'1356',"LA-884","884","N_A","A","DL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1357,1,'1357',"LA-884","884","N_A","A","DL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1358,1,'1358',"LA-884","884","N_A","A","DL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1359,1,'1359',"LA-884","884","N_A","A","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1360,1,'1360',"LA-884","884","N_A","A","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1361,1,'1361',"LA-884","884","N_A","A","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1362,1,'1362',"LA-884","884","N_A","A","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','local 80 Pg 26 P8 (c) 1');
insert into contract_rule values(1363,1,'1363',"LA-884","884","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(1364,1,'1364',"LA-884","884","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(1365,1,'1365',"LA-884","884","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(1366,1,'1366',"LA-884","884","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1367,1,'1367',"LA-884","884","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(1368,1,'1368',"LA-884","884","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(1369,1,'1369',"LA-884","884","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(1370,1,'1370',"LA-884","884","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(1371,1,'1371',"LA-884","884","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(1372,1,'1372',"LA-884","884","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'no MPV for on-call','');
insert into contract_rule values(1373,1,'1373',"LA-884","884","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'no gold for on-call','');

insert into contract_rule values(1375,1,'1375',"LA-892","892","N_A","A","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','');
insert into contract_rule values(1376,1,'1376',"LA-892","892","N_A","A","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1377,1,'1377',"LA-892","892","N_A","A","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(1378,1,'1378',"LA-892","892","N_A","A","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(1379,1,'1379',"LA-892","892","N_A","A","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(1380,1,'1380',"LA-892","892","N_A","A","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(1381,1,'1381',"LA-892","892","N_A","A","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','LA/IA P39( c )');
insert into contract_rule values(1382,1,'1382',"LA-892","892","N_A","A","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(1383,1,'1383',"LA-892","892","N_A","A","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(1384,1,'1384',"LA-892","892","N_A","A-1","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1385,1,'1385',"LA-892","892","N_A","A-1","SL","N_A","N_A","N_A","15",null,"OT",'OT-8-15X',1170,'','');
insert into contract_rule values(1386,1,'1386',"LA-892","892","N_A","A-1","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1387,1,'1387',"LA-892","892","N_A","A-1","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1388,1,'1388',"LA-892","892","N_A","A-1","SL","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1389,1,'1389',"LA-892","892","N_A","A-1","SL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1390,1,'1390',"LA-892","892","N_A","A-1","N_A","N_A","N_A","SF","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1391,1,'1391',"LA-892","892","N_A","A-1","N_A","N_A","N_A","DT","N_A",null,"GT",'GT-4-4-1X',2070,'','');
insert into contract_rule values(1392,1,'1392',"LA-892","892","N_A","A-1","SL","ON","N_A","N_A","5",'WD=Fri;CL>20',"OT",'OT-8-15X-24',1370,'Friday 5th day, late runs into Sat.  Employee starts work after 8pm on Friday and works past midnight.','');
insert into contract_rule values(1393,1,'1393',"LA-892","892","N_A","A-1","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');
insert into contract_rule values(1394,1,'1394',"LA-892","892","N_A","A-1","N_A","N_A","N_A","WT","N_A",null,"SP",'SP_WRK_TRAV_1',2070,'Work & Travel day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1395,1,'1395',"LA-892","892","N_A","A-1","N_A","N_A","N_A","TW","N_A",null,"SP",'SP_TRAV_WRK_1',2070,'Travel & Work day.  Travel time AFTER work, beyond the min call, does not count towards GOLD.','');
insert into contract_rule values(1396,1,'1396',"LA-892","892","N_A","B","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-40',70,'','');
insert into contract_rule values(1397,1,'1397',"LA-892","892","N_A","B","SL","N_A","N_A","N_A","15",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1398,1,'1398',"LA-892","892","N_A","B","SL","N_A","N_A","N_A","6",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1399,1,'1399',"LA-892","892","N_A","B","SL","N_A","N_A","N_A","6",null,"OT",'OT-0-15X',1170,'','');
insert into contract_rule values(1400,1,'1400',"LA-892","892","N_A","B","DL","N_A","N_A","N_A","16",null,"GT",'GT-8-1X',1170,'','');
insert into contract_rule values(1401,1,'1401',"LA-892","892","N_A","B","DL","N_A","N_A","N_A","7",null,"GL",'GL-0-2X',1170,'','');
insert into contract_rule values(1402,1,'1402',"LA-892","892","N_A","B","N_A","N_A","N_A","N_A","7",null,"GT",'GT-8-1X',1070,'','');
insert into contract_rule values(1403,1,'1403',"LA-892","892","N_A","B","N_A","N_A","N_A","ID","67",null,"GT",'GT-4-4-1X',3070,'','');

insert into contract_rule values(1405,1,'1405',"LA-892","892","N_A","B-1","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'','');
insert into contract_rule values(1406,1,'1406',"LA-892","892","N_A","B-1","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1407,1,'1407',"LA-892","892","N_A","B-1","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(1408,1,'1408',"LA-892","892","N_A","B-1","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(1409,1,'1409',"LA-892","892","N_A","B-1","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(1410,1,'1410',"LA-892","892","N_A","B-1","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(1411,1,'1411',"LA-892","892","N_A","B-1","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(1412,1,'1412',"LA-892","892","N_A","B-1","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(1413,1,'1413',"LA-892","892","N_A","B-1","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');
insert into contract_rule values(1414,1,'1414',"LA-892","892","N_A","C","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1415,1,'1415',"LA-892","892","N_A","C","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(1416,1,'1416',"LA-892","892","N_A","C","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(1417,1,'1417',"LA-892","892","N_A","C","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(1418,1,'1418',"LA-892","892","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(1419,1,'1419',"LA-892","892","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(1420,1,'1420',"LA-892","892","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'no MPV for on-call','');
insert into contract_rule values(1421,1,'1421',"LA-892","892","N_A","C","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'no gold for on-call','');
insert into contract_rule values(1422,1,'1422',"LA-892","892","N_A","C-D","N_A","N_A","N_A","N_A","N_A",null,"BA",'WK-OC',70,'Override default weekly','Daily on-call');
insert into contract_rule values(1423,1,'1423',"LA-892","892","N_A","C-D","SL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-S',170,'','');
insert into contract_rule values(1424,1,'1424',"LA-892","892","N_A","C-D","DL","N_A","N_A","N_A","N_A",null,"OC",'OC-56-D',170,'','');
insert into contract_rule values(1425,1,'1425',"LA-892","892","N_A","C-D","N_A","N_A","N_A","ID","67",null,"OC",'OC-1/12',3070,'','');
insert into contract_rule values(1426,1,'1426',"LA-892","892","N_A","C-D","N_A","N_A","N_A","SF","N_A",null,"OC",'OC-1/10',2070,'','');
insert into contract_rule values(1427,1,'1427',"LA-892","892","N_A","C-D","N_A","N_A","N_A","N_A","N_A",null,"CB",'CB-05-1X',70,'','');
insert into contract_rule values(1428,1,'1428',"LA-892","892","N_A","C-D","N_A","N_A","N_A","N_A","N_A",null,"GT",'GT-12-1X',70,'','');
insert into contract_rule values(1429,1,'1429',"LA-892","892","N_A","C-D","N_A","N_A","N_A","N_A","N_A",null,"MP",'MP-NONE',70,'On-call does not get MPVs','');
insert into contract_rule values(1430,1,'1430',"LA-892","892","N_A","C-D","N_A","N_A","N_A","N_A","N_A",null,"GL",'GL-NONE',70,'On-call does not get gold time','');

insert into contract_rule values(1432,1,'1432',"DGA","DGA","N_A","X-W","SL","N_A","N_A","N_A","N_A",null,"BA",'WK-CUME-60',170,'','');

-- ===============================  ====================================

/* */
