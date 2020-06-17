$P{date} is DPR date

-- dprfilminvtrysubrpt:

select m.type as type,
	case when (fs.date=$P{date}) then fs.inventory_prior
		else (fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) end as inventory_prior,
	case when (fs.date=$P{date}) then fs.inventory_received   else 0 end as inventory_received,
	case when (fs.date=$P{date}) then fs.inventory_used_today else 0 end as inventory_used_today,
	(fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) as inventory_total
from film_stock fs, material m 
where fs.material_id = m.id and
	(fs.date, fs.material_id) IN
		(Select max(f2.date),f2.material_id from Film_Stock f2, Material
		where f2.date <= $P{date}
		and f2.material_id = material.id
		group by f2.material_id)
order by type
limit 6;


-- dprfilmused1subrpt: total usage prior to today

select material.type, film_measure.id, film_measure.gross, 
	SUM(film_measure.print) as Print, 
	SUM(film_measure.no_good) as No_Good, 
	SUM(film_measure.waste) as Waste, 
	SUM(film_measure.print)+SUM(film_measure.no_good)+SUM(film_measure.waste) as Total
from material, film_stock, film_measure
where film_stock.Date < $P{date}
	and film_stock.material_id = material.id
	and film_stock.used_today_id = film_measure.id
group by material.type
limit 3;


-- dprfilmused2subrpt: today's usage (0 if no record)

select m.type as type,
	case when (fs.date=$P{date}) then fm.print else 0 end as print,
	case when (fs.date=$P{date}) then fm.no_good else 0 end as no_good,
	case when (fs.date=$P{date}) then fm.waste else 0 end as waste,
	case when (fs.date=$P{date}) then (fm.print+fm.no_good+fm.waste) else 0 end as total
from film_stock fs, material m, film_measure fm where
	fs.material_id = m.id and
	fs.used_today_id = fm.id and
	(fs.date, fs.material_id) IN 
		(Select max(f2.date),f2.material_id from Film_Stock f2, Material 
		where f2.date <= $P{date}
		and f2.material_id = material.id 
		group by f2.material_id)
group by type
order by type
limit 3;


-- dprfilmused3subrpt: total usage including today

select material.type, film_measure.id, film_measure.gross, 
	SUM(film_measure.print) as Print, 
	SUM(film_measure.no_good) as No_Good, 
	SUM(film_measure.waste) as Waste, 
	SUM(film_measure.print)+SUM(film_measure.no_good)+SUM(film_measure.waste) as Total
from material, film_stock, film_measure
where film_stock.Date <= $P{date}
	and film_stock.material_id = material.id
	and film_stock.used_today_id = film_measure.id
group by material.type
limit 3;

-- the filmused4/5/6 queries are the same as 1/2/3 except for changing
-- the limit to "limit 3, 3"
