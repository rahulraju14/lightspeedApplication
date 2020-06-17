SET foreign_key_checks = off;

set @sid=170; /* 19 */

set @proj = ( select project_id from script where id = @sid);
set @lowsceneid = ( select min(id) from scene where script_id = @sid ) ;
set @hisceneid = ( select max(id) from scene where script_id = @sid ) ;
set @stbd = (select current_stripboard_id from project where id = @proj );
set @pg1id = (select id from page where script_id = @sid order by number limit 1);
set @pgLastId = (select id from page where script_id = @sid order by number desc limit 1);

-- select @lowsceneid, @hisceneid;
 
select * from script where id = @sid;
-- 2 all scenes:
select * from scene where script_id = @sid order by sequence;
-- 3 all pages:
select * from page where script_id = @sid order by number;

-- 4 complete text
-- select * from text_element where scene_id >= @lowsceneid and scene_id <= @hisceneid order by sequence;
-- 4 complete text
select * from text_element where page_id >= @pg1id and page_id <= @pgLastId order by sequence;

-- 5 complete list of scene elements
select * from script_element where project_id = @proj order by type, element_id;

-- 6 get breakdown: script elements by scene
select sse.*, s.number, se.type, se.name from scene_script_element sse, script_element se, scene s where scene_id >= @lowsceneid and scene_id <= @hisceneid
  and s.id = scene_id
  and se.id = sse.script_element_id 
  order by s.sequence, se.type, se.name;

-- All Stripboards
select * from stripboard where project_id = @proj;

-- Strips from "current" stripboard
select * from strip where stripboard_id = @stbd
	order by status, orderNumber;

-- All Units for this project
select * from unit where project_id = @proj;

-- All Unit_stripboards for the current stripboard
select * from unit_stripboard where stripboard_id = @stbd;

/*
*/
	