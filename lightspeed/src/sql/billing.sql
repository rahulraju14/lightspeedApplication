SET foreign_key_checks = on;
-- update weekly_time_card set prod_co ='Best Studios, Inc.' where prod_name = 'Best TV Series Ever';
-- select * from weekly_time_card where updated is null;
/* */
 set @prod_id = 'pt25';
 set @service_id = 3; -- 1: file; 2: IP; 6: showbiz
 set @month = '201611';
/*
select w.prod_id, prod_co, prod_name, date_format(w.end_date,'%m/%d/%Y') we_date, sum(w.grand_total),
		w.id,  b.name Batch,
		user_account, first_name, last_name, social_security, w.occupation, w.loan_out_corp, w.fed_corp_id, w.state_corp_id, grand_total, w.status
	from production p, payroll_preference pp, weekly_time_card w left join weekly_batch b on w.weekly_batch_id = b.id
	where updated is not null -- omit Global preference records
	and p.payroll_preference_id = pp.id
	-- and pp.payroll_service_id = @service_id
	and w.prod_id = p.prod_id
	-- and w.weekly_batch_id = b.id
	-- and p.prod_id = @prod_id
	-- and p.studio like 'pilgrim%'
	-- and (w.status = 'submitted' or w.status = 'approved')
	and w.grand_total is not null and w.grand_total > 0
	and left(date_format(w.end_date,'%Y%m%d'),6)=@month
	group by prod_name
	order by prod_co, prod_id, w.end_date, last_name, first_name;
/* *
-- List of all productions assigned to a particular Payroll Service id

select distinct prod_id, studio, title from production p, payroll_preference pp where 
	p.payroll_preference_id = pp.id
	and pp.payroll_service_id = @service_id
	order by studio, title;

-- select prod_id, studio, title from production where studio like 'pilgrim%';

/* */
	