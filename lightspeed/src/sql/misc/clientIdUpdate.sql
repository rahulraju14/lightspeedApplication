SET foreign_key_checks = on;

-- Updating TEAM Client IDs into Payroll Preferences
 
/*
update payroll_preference pp set pp.payroll_prod_id='15073' where pp.id in 
(
select ppid from
	(select pp.id as ppid
	from production p, project pj, payroll_preference pp
	where pj.production_id = p.id and
	(pj.payroll_preference_id = pp.id or p.payroll_preference_id = pp.id) and
	pp.payroll_service_id = 4
	and studio like 'dick%') as tab1
)
limit 10
;
/*
select p.id, p.studio, p.title, pj.id, pj.title, pp.* from production p, project pj, payroll_preference pp
where pj.production_id = p.id and
(pj.payroll_preference_id = pp.id or p.payroll_preference_id = pp.id) and
pp.payroll_service_id in (4,11)
-- and studio like 'dick%'
;
/* *
select distinct p.studio, pp.payroll_prod_id, pj.code, pj.title, pj.payroll_preference_id, pp.payroll_service_id 
	from production p, project pj, payroll_preference pp 
	where pj.production_id = p.id
	and pj.payroll_preference_id = pp.id
	and p.id 
	in (
	select id from production p where p.payroll_preference_id in ( 
		select id from payroll_preference where payroll_service_id in (4,11)
		)  
	) order by studio, code;

/* */
/* */
	