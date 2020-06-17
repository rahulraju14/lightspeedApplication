SET foreign_key_checks = on;
/* Used to generate the Gross totals table to match the AICP spreadsheet.

	It creates a "pivot table" (or cross-tab?) with the W/E dates across the top and 
	employee names (= roles) down the left. 

		The "qaNum" field, taken from the User.middle_name, is used to match the order
	of the employees in the Test spreadsheet, as it is not exactly alphabetic by name.
	** NOTE ** YOU MUST update the User.Middle_Name fields (via sql) before this will work **

		There are two steps - the first generates a temporary table, and the second creates
	the pivot table query results (XLS data) from the temporary table.
*/
/* *
set @prod = 'videotape - non dramatic';
set @first_date = '2015-07-11'; -- videotape
/* */
set @prod = 'aicp 2';
set @first_date = '2014-11-08'; -- aicp 2
/* *
set @prod = 'aicp 1';
set @first_date = '2014-11-08'; -- aicp 1
/* */

/* */
DROP TABLE if exists temp_totals;
create table temp_totals (
select 
		u.account_number acctNum, u.middle_name qaNum,
		round(grand_total-e.total,2) total, 
		concat(w.last_name, ', ', w.first_name) name, end_date,
		(datediff(end_date, @first_date)/7)+1 weeknum
	from pay_expense e, weekly_time_card w, user u 
	where e.weekly_id = w.id and u.account_number = w.user_account
		and e.category = 'bonus'
		and w.prod_name = @prod
	order by end_date, w.last_name, w.first_name
	);
/* */
select name, qaNum,
sum(total*(1-abs(sign(weeknum-  1 )))) as 2014_11_08,
sum(total*(1-abs(sign(weeknum-  2 )))) as 2014_11_15,
sum(total*(1-abs(sign(weeknum-  3 )))) as 2014_11_22,
sum(total*(1-abs(sign(weeknum-  4 )))) as 2014_11_29,
sum(total*(1-abs(sign(weeknum-  5 )))) as 2014_12_06,
sum(total*(1-abs(sign(weeknum-  6 )))) as 2014_12_13,
sum(total*(1-abs(sign(weeknum-  7  )))) as 2014_12_20,
sum(total*(1-abs(sign(weeknum-  8  )))) as 2014_12_27,
sum(total*(1-abs(sign(weeknum-  9  )))) as 2015_01_03,
sum(total*(1-abs(sign(weeknum-  10  )))) as 2015_01_10,
sum(total*(1-abs(sign(weeknum-  11  )))) as 2015_01_17,
sum(total*(1-abs(sign(weeknum-  12  )))) as 2015_01_24,
sum(total*(1-abs(sign(weeknum-  13 )))) as 2015_01_31,
sum(total*(1-abs(sign(weeknum-  14  )))) as 2015_02_07,
sum(total*(1-abs(sign(weeknum-  15  )))) as 2015_02_14,
sum(total*(1-abs(sign(weeknum-  16  )))) as 2015_02_21,
sum(total*(1-abs(sign(weeknum-  17  )))) as 2015_02_28,
sum(total*(1-abs(sign(weeknum-  18  )))) as 2015_03_07,
sum(total*(1-abs(sign(weeknum-  19  )))) as 2015_03_14,
sum(total*(1-abs(sign(weeknum-  20  )))) as 2015_03_21,
sum(total*(1-abs(sign(weeknum-  21  )))) as 2015_03_28,
sum(total*(1-abs(sign(weeknum-  22  )))) as 2015_04_04,
sum(total*(1-abs(sign(weeknum-  23  )))) as 2015_04_11,
sum(total*(1-abs(sign(weeknum-  24  )))) as 2015_04_18,
sum(total*(1-abs(sign(weeknum-  25  )))) as 2015_04_25,
sum(total*(1-abs(sign(weeknum-  26 )))) as 2015_05_02,
sum(total*(1-abs(sign(weeknum-  27 )))) as 2015_05_09,
sum(total*(1-abs(sign(weeknum-  28  )))) as 2015_05_16,
sum(total*(1-abs(sign(weeknum-  29  )))) as 2015_05_23,
sum(total*(1-abs(sign(weeknum-  30  )))) as 2015_05_30,
sum(total*(1-abs(sign(weeknum-  31  )))) as 2015_06_06,
sum(total*(1-abs(sign(weeknum-  32  )))) as 2015_06_13,
sum(total*(1-abs(sign(weeknum-  33  )))) as 2015_06_20,
sum(total*(1-abs(sign(weeknum-  34  )))) as 2015_06_27,
sum(total*(1-abs(sign(weeknum-  35  )))) as 2015_07_04,
sum(total*(1-abs(sign(weeknum-  36  )))) as 2015_07_11,
sum(total*(1-abs(sign(weeknum-  37  )))) as 2015_07_18,
sum(total*(1-abs(sign(weeknum-  38  )))) as 2015_07_25,
sum(total*(1-abs(sign(weeknum-  39  )))) as 2015_08_01,
sum(total*(1-abs(sign(weeknum-  40  )))) as 2015_08_08,
sum(total*(1-abs(sign(weeknum-  41  )))) as 2015_08_15,
sum(total*(1-abs(sign(weeknum-  42  )))) as 2015_08_22,
sum(total*(1-abs(sign(weeknum-  43  )))) as 2015_08_29,
sum(total*(1-abs(sign(weeknum-  44  )))) as 2015_09_05,
sum(total*(1-abs(sign(weeknum-  45  )))) as 2015_09_12,
sum(total*(1-abs(sign(weeknum-  46  )))) as 2015_09_19,
sum(total*(1-abs(sign(weeknum-  47  )))) as 2015_09_26,
sum(total*(1-abs(sign(weeknum-  48  )))) as 2015_10_03,
sum(total*(1-abs(sign(weeknum-  49  )))) as 2015_10_10,
sum(total*(1-abs(sign(weeknum-  50  )))) as 2015_10_17
from temp_totals group by name order by qaNum;

/* */
/* */
	