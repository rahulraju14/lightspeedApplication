SET foreign_key_checks = off;

/* Contract list */
-- delete from production_contract;
delete from contract;

insert into contract values(3,1,"12LA16","Local 16 - IATSE San Francisco","AG",2015,"2012-08-01","2015-07-31","16","SF-16");
insert into contract values(4,1,"12TM40","Local 40 - International Brotherhood of Electrical Workers, IBEW","AG",2015,"2012-07-29","2015-07-31","40","TM-40");
insert into contract values(5,1,"12LA44","Local 44 - IATSE LA Property Craftspersons","AG",2015,"2012-07-29","2015-07-31","44","LA-44");
insert into contract values(6,1,"12TM78","Local 78 - Plumbers and Pipe Fitters","AG",2015,"2012-07-29","2015-07-31","78","TM-78");
insert into contract values(7,1,"12LA80","Local 80 - IATSE LA Motion Picture Studio Grips & Crafts Service","AG",2015,"2012-07-29","2015-07-31","80","LA-80");
insert into contract values(8,1,"12TM399L","Local 399 - Location Managers","AG",2015,"2012-07-29","2015-07-31","399L","TM-399L");
insert into contract values(9,1,"12TM399C","Local 399 - Casting Directors","AG",2015,"2012-09-30","2015-09-30","399C","TM-399C");
insert into contract values(10,1,"12TM399T","Local 399 - Teamsters","AG",2015,"2012-07-29","2015-07-31","399T","TM-399T");
insert into contract values(11,1,"12TM399TM","Local 399 - Teamsters - Movie of the Week (MOW)","AG",2015,"2012-07-29","2015-07-31","399T","TM-MW");
insert into contract values(12,1,"12LA600C","Local 600 - IATSE International Photographers","AG",2015,"2012-07-29","2015-07-31","600C","LA-600C");
insert into contract values(13,1,"12LA600P","Local 600 - IATSE Publicists","AG",2015,"2012-07-29","2015-07-31","600P","LA-600P");
insert into contract values(14,1,"12LA695P","Local 695 - IATSE LA Projectionists","AG",2015,"2012-07-29","2015-07-31","695P","LA-695P");
insert into contract values(15,1,"12LA695S","Local 695 - IATSE LA International Sound Technicians","AG",2015,"2012-07-29","2015-07-31","695S","LA-695S");
insert into contract values(16,1,"12LA700E","Local 700 - IATSE Motion Picture Film Editors","AG",2015,"2012-07-29","2015-07-31","700E","LA-700E");
insert into contract values(17,1,"12LA700A","Local 700 - IATSE LA Screen Story Analysts","AG",2015,"2012-07-29","2015-07-31","700A","LA-700A");
insert into contract values(18,1,"12LA700S","Local 700 - IATSE LA Sound Technicians","AG",2015,"2012-07-29","2015-07-31","700S","LA-700S");
insert into contract values(19,1,"12LA705","Local 705 - IATSE LA Motion Picture Costumers (Wardrobe)","AG",2015,"2012-07-29","2015-07-31","705","LA-705");
insert into contract values(20,1,"12LA706","Local 706 - IATSE LA Make-Up Artists & Hair Stylists","AG",2015,"2012-07-29","2015-07-31","706","LA-706");
insert into contract values(21,1,"12TM724","Local 724 - Studio Utility Employees","AG",2015,"2012-07-29","2015-07-31","724","TM-724");
insert into contract values(22,1,"12LA728","Local 728 - IATSE LA Electrical Lighting Technicians","AG",2015,"2012-07-29","2015-07-31","728","LA-728");
insert into contract values(23,1,"12LA729","Local 729 - IATSE LA Motion Picture Set Painters","AG",2015,"2012-07-29","2015-07-31","729","LA-729");
insert into contract values(24,1,"12TM755","Local 755 - Ornamental Plasterers & Cement Masons","AG",2015,"2012-07-29","2015-07-31","755","TM-755");
insert into contract values(25,1,"12LA767","Local 767 - IATSE LA Motion Picture Studio First Aid","AG",2015,"2012-07-29","2015-07-31","767","LA-767");
insert into contract values(26,1,"12LA800M","Local 800 - IATSE LA Illustrators and Matte Artists","AG",2015,"2012-07-29","2015-07-31","800M","LA-800M");
insert into contract values(27,1,"12LA800S","Local 800 - IATSE LA Scenic/Title/Graphic Artists","AG",2015,"2012-07-29","2015-07-31","800S","LA-800S");
insert into contract values(28,1,"12LA800D","Local 800 - IATSE LA Set Designers/Model Builders","AG",2015,"2012-07-29","2015-07-31","800D","LA-800D");
insert into contract values(29,1,"12LA800A","Local 800 - IATSE LA Society of Motion Picture Art Directors","AG",2015,"2012-07-29","2015-07-31","800A","LA-800A");
insert into contract values(30,1,"12LA839","Local 839 - IATSE LA Animation","AG",2015,"2012-07-29","2015-07-31","839","LA-839");
insert into contract values(31,1,"12LA871","Local 871 - IATSE LA Script Supervisors, Coordinators, & Accountants","AG",2015,"2012-07-29","2015-07-31","871","LA-871");
insert into contract values(32,1,"12LA884","Local 884 - IATSE LA Motion Picture Studio Teachers & Welfare Workers","AG",2015,"2012-07-29","2015-07-31","884","LA-884");
insert into contract values(33,1,"12LA892","Local 892 - IATSE LA Costume Designers","AG",2015,"2012-07-29","2015-07-31","892","LA-892");
insert into contract values(34,1,"12LAVT","IATSE Videotape Agreement","AG",2015,"2013-01-01","2015-12-31","VT","LA-VT");
insert into contract values(35,1,"12IALB","IATSE Low Budget Theatrical","AG",2015,"2013-01-01","2015-12-31","IALB","IALB");
insert into contract values(36,1,"12NY52","Local 52 - IATSE NY Studio Mechanics","AG",2015,"2012-05-16","2015-05-15","52","NY-52");
insert into contract values(37,1,"12NY161","Local 161 - IATSE NY Script Supervisors, Coordinators, & Accountants","AG",2015,"2012-03-04","2015-02-28","161","NY-161");
insert into contract values(38,1,"12NY764","Local 764 - IATSE NY Theatrical Wardrobe","AG",2015,"2012-03-04","2015-02-28","764","NY-764");
insert into contract values(39,1,"12NY798","Local 798 - IATSE NY Make-Up Artists & Hair Stylists","AG",2015,"2012-03-04","2015-02-28","798","NY-798");
insert into contract values(40,1,"12TM817","Local 817 - Theatrical Teamsters","AG",2015,"2012-10-28","2015-10-31","817","NY-817");
insert into contract values(41,1,"12NY829","Local 829 - IATSE NY Scenic Artists","AG",2015,"2012-09-30","2015-09-27","829","NY-829");
insert into contract values(42,1,"12A38S","ASA Local 38 Michigan (Southeastern)","AG",2015,"2012-07-29","2015-07-31","38-SE","ASA-M");
insert into contract values(43,1,"12A38NS","ASA Local 38 Michigan (outside Southeastern)","AG",2015,"2012-07-29","2015-07-31","38","ASA-NM");
insert into contract values(44,1,"12A209","ASA Local 209 Ohio","AG",2015,"2012-07-29","2015-07-31","209","ASA-NM");
insert into contract values(45,1,"12A476","ASA Local 476 Illinois","AG",2015,"2012-09-01","2015-08-31","476","CH-476");
insert into contract values(46,1,"12A477","ASA Local 477 Florida","AG",2015,"2012-07-29","2015-07-31","477","ASA-M");
insert into contract values(47,1,"12A478","ASA Local 478 Louisiana and Southern Mississippi","AG",2015,"2012-07-29","2015-07-31","478","ASA-NM");
insert into contract values(48,1,"12A479","ASA Local 479 Georgia","AG",2015,"2012-07-29","2015-07-31","479","ASA-NM");
insert into contract values(49,1,"12A480","ASA Local 480 New Mexico","AG",2015,"2012-07-29","2015-07-31","480","ASA-NM");
insert into contract values(50,1,"12A481","ASA Local 481 ME, MA, NH, RI, VT","AG",2015,"2012-07-29","2015-07-31","481","ASA-M");
insert into contract values(51,1,"12A484","ASA Local 484 Texas","AG",2015,"2012-07-29","2015-07-31","484","ASA-NM");
insert into contract values(52,1,"12A485","ASA Local 485 Arizona","AG",2015,"2012-07-29","2015-07-31","485","ASA-NM");
insert into contract values(53,1,"12A487M","ASA Local 487 Maryland","AG",2015,"2012-07-29","2015-07-31","487-M","ASA-M");
insert into contract values(54,1,"12A487W","ASA Local 487 Washington, DC","AG",2015,"2012-07-29","2015-07-31","487-DC","ASA-DC");
insert into contract values(55,1,"12A487V","ASA Local 487 Virginia","AG",2015,"2012-07-29","2015-07-31","487-V","ASA-NM");
insert into contract values(56,1,"12A488","ASA Local 488 Oregon, Washington","AG",2015,"2012-07-29","2015-07-31","488","ASA-NM");
insert into contract values(57,1,"12A489","ASA Local 489 Pittsburgh, PA","AG",2015,"2012-07-29","2015-07-31","489","ASA-NM");
insert into contract values(58,1,"12A490","ASA Local 490 Minnesota","AG",2015,"2012-07-29","2015-07-31","490","ASA-NM");
insert into contract values(59,1,"12A491","ASA Local 491 North & South Carolina,  & Savannah, GA","AG",2015,"2012-07-29","2015-07-31","491","ASA-NM");
insert into contract values(60,1,"12A492","ASA Local 492 Tennessee, N. Mississippi","AG",2015,"2012-07-29","2015-07-31","492","ASA-NM");
insert into contract values(61,1,"12A493","ASA Local 493 St Louis, MO","AG",2015,"2012-07-29","2015-07-31","493","ASA-NM");
insert into contract values(62,1,"12A494","ASA Local 494 Puerto Rico, US Virgin Islands","AG",2015,"2012-07-29","2015-07-31","494","ASA-NM");
insert into contract values(63,1,"12A495","ASA Local 495 San Diego, CA","AG",2015,"2012-07-29","2015-07-31","495","ASA-NM");
insert into contract values(64,1,"12A665","ASA Local 665 Hawaii","AG",2015,"2012-07-29","2015-07-31","665","ASA-HW-LV");
insert into contract values(65,1,"12A720","ASA Local 720 Las Vegas, NV","AG",2015,"2012-07-29","2015-07-31","720","ASA-HW-LV");
Insert into contract values(66,1,"13C16","Local 16 San Francisco - Bay Area Commercial","AG",2015,"2013-10-01","2016-09-30","CPA-16","C-16");
Insert into contract values(67,1,"13CPA","IATSE CPA/NEC","AG",2015,"2013-10-01","2016-09-30","CPA-*","CPA");
Insert into contract values(68,1,"13C399L","Teamsters 399 Location Mgrs/Scouts - AICP","AG",2015,"2011-02-01","2014-01-31","CPA-399L","C-399L");
Insert into contract values(69,1,"13C399T","Teamsters 399 Drivers (Transportation) - AICP","AG",2015,"2011-02-01","2014-01-31","CPA-399T","C-399T");
Insert into contract values(70,1,"13CI399L","Teamsters 399 Location Mgrs/Scouts - Comm. Indie","AG",2015,"2011-02-01","2014-01-31","CPA-399L","CI-399L");
Insert into contract values(71,1,"13CI399T","Teamsters 399 Drivers (Transportation) - Comm. Indie","AG",2015,"2011-02-01","2014-01-31","CPA-399T","CI-399T");
Insert into contract values(72,1,"13C476","Local 476 (Chicago) Commercial","AG",2015,"2013-10-01","2016-09-30","CPA-476","C-476");
Insert into contract values(73,1,"13C817L","Teamsters 817 (NY) Location Mgrs/Scouts - AICP","AG",2015,"2013-10-01","2016-09-30","CPA-817L","C-817L");
Insert into contract values(74,1,"13C817T","Teamsters 817 (NY) Drivers (Transportation) - AICP","AG",2015,"2013-10-01","2016-09-30","CPA-817T","C-817T");
Insert into contract values(75,1,"13CI817L","Teamsters 817 (NY) Mgrs/Scouts - Comm. Indie","AG",2015,"2013-10-01","2016-09-30","CPA-817L","CI-817L");
Insert into contract values(76,1,"13CI817T","Teamsters 817 (NY) Drivers (Transportation) - Comm. Indie","AG",2015,"2013-10-01","2016-09-30","CPA-817T","CI-817T");
insert into contract values(77,1,"13C399O","Teamsters CPA Other","AG",2015,"2013-10-01","2016-09-30","CPA-TEAM","C-TEAM");
Insert into contract values(78,1,"13C38","Local 38 Detroit (Comm. Indie)","AG",2015,"2013-10-01","2016-09-30","CPA-38-I","CI-38");
Insert into contract values(79,1,"13C52","Local 52 Studio Mechanics - AICP","AG",2015,"2013-10-01","2016-09-30","CPA-52","C-52");
Insert into contract values(80,1,"13CI52I","Local 52 ICC Studio Mechanics","AG",2015,"2013-10-01","2016-09-30","CPA-52","CI-52");
Insert into contract values(81,1,"13C829","Local 829 - Scenic Artists - AICP","AG",2015,"2013-10-01","2016-09-30","CPA-829","C-829");
insert into contract values(82,1,"13CI829","Local 829 - Scenic Artists - Commercial Independent","AG",2015,"2013-10-01","2016-09-30","CPA-829","CI-829");
insert into contract values(83,1,"13DGA-C","DGA NCA","AG",2015,"2013-10-01","2016-09-30","CPA-DGA","C-DGA");
insert into contract values(84,1,"12DGA","DGA - Directors Guild of America","AG",2015,"2012-07-01","2015-06-30","DGA","DGA");
insert into contract values(85,1,"12PGA","PGA - Producers Guild of America","AG",2015,"2013-01-01","2015-12-31","PGA","PGA");
insert into contract values(86,1,"12WGA","WGA - Writers Guild of America","AG",2015,"2012-07-29","2015-07-31","WGA","WGA");
insert into contract values(87,1,"12SAG","SAG - Screen Actors Guild","AG",2015,"2012-07-29","2015-07-31","SAG","SAG");
insert into contract values(88,1,"12AFTRA","AFTRA - American Federation of Television and Radio Artists","AG",2015,"2012-07-29","2015-07-31","AFTRA","AFTRA");

/* */
