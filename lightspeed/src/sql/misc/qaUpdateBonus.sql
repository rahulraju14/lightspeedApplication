SET foreign_key_checks = on;

/* Update the Bonus record to have a value 1 less than the timecard's grand total. */

set @cat = 'Bonus';  -- normal
set @diff = 1.0; -- set to 1.00 for normal bonus calculation

-- set @cat = 'Sal Advance - Txb'; -- if setting to 0 for unused weeks
-- set @diff = -1.0; -- set to 0.00 for unused weeks

-- set @prod = 'aicp 1';
-- set @prod = 'aicp 2';
-- set @prod = '700 Generic';
-- set @prod = 'videotape - non dramatic';
set @prod = 'NF TV';
set @date = '2015-11-21'; -- if doing just one w/e date


/* *
-- UPDATE all Bonus values in the EXPENSE table:
update pay_expense p, weekly_time_card w set p.rate = (w.grand_total - @diff + p.rate), p.total = (@diff - w.grand_total + p.rate) 
	where p.weekly_id in
		(
		select id from weekly_time_card where 
		prod_name= @prod 
		and end_date = @date
		)
	and p.weekly_id = w.id and p.category = @cat
	and w.grand_total <> @diff;

/* *
-- UPDATE the Bonus line in the PAY BREAKDOWN to match Expense:
update pay_breakdown pb, pay_expense pe, weekly_time_card w set pb.rate = pe.rate, pb.total = pe.total, w.grand_total = @diff 
	where pb.weekly_id in
		(select id from (
		select id from weekly_time_card where 
		prod_name= @prod 
		and end_date = @date
		) table1 )
	and pb.weekly_id = w.id and pb.category = @cat
	and pe.weekly_id = w.id and pe.category = @cat
	and w.grand_total <> @diff;

/* *
-- CLEAR out all bonus totals:

update pay_expense p, weekly_time_card w set p.rate = 0, p.total = 0
	where p.weekly_id in
		( select id from weekly_time_card where prod_name = @prod 
--		and end_date = @date
		)
	and p.weekly_id = w.id and p.category = @cat;

/* *
select prod_name, count(*), count(*)/45 from weekly_time_card
	where prod_name = @prod and grand_total = 1.0 group by prod_name;
select prod_name, count(*), count(*)/45 from weekly_time_card
	where prod_name = @prod and (grand_total is null or grand_total <> 1.0) group by prod_name;
/* */

/* *
select count(*) from pay_expense where weekly_id in 
(
select id from weekly_time_card where prod_name = @prod 
	and end_date = @date
)
and category = @cat;


/* Generate "Bonus" Pay Expense records for all timecards in a production *
insert into pay_expense (
	weekly_Id, line_number, Category, Quantity, Rate, Total )
Select 
	Id, 0, @cat, -1.00, 0.00, 0.00
	from 
	(
	select id from weekly_time_card where prod_name = @prod
	and (CASE WHEN @date <> '' THEN end_date = @date ELSE true END) 
	) tab1;


/* DELETE *
delete from pay_expense where weekly_id in
		(
		select id from weekly_time_card where 
		prod_name= @prod 
		and end_date = @date
		)
	and category = @cat;

/* */
/* */
