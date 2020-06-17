

select * from Film_stock fs, Material where (fs.date, fs.material_id) IN 
			(Select max(f.date),f.material_id from Film_Stock f, material m 
			 where f.date <= '2011-04-06'
			 and f.material_id = m.id
			 group by f.material_id)
			 and fs.material_id = material.id
--			 group by material.id
			 order by material.type, fs.id desc;

Select max(f.date),f.material_id from Film_Stock f, material m 
			 where f.date <= '2011-04-06'
			 and f.material_id = m.id
			 group by f.material_id;


/*dprfilmused2subrpt.jrxml  *
select m.type as type,
	case when (fs.date='2011-03-29') then fm.print else 0 end as print,
	case when (fs.date='2011-03-29') then fm.no_good else 0 end as no_good,
	case when (fs.date='2011-03-29') then fm.waste else 0 end as waste,
	case when (fs.date='2011-03-29') then (fm.print+fm.no_good+fm.waste) else 0 end as total
from film_stock fs, material m, film_measure fm where
	fs.material_id = m.id and
  fs.used_today_id = fm.id and
	(fs.date, fs.material_id) IN 
		(Select max(f2.date),f2.material_id from Film_Stock f2, Material 
		where f2.date <= '2011-03-29'
		and f2.material_id = material.id 
		group by f2.material_id)
group by type
order by type
limit 3;

/* dprfilmused1subrpt.jrxml *
select material.type, film_measure.id,
	SUM(film_measure.print) as Print, 
	SUM(film_measure.no_good) as No_Good, 
	SUM(film_measure.waste) as Waste, 
	SUM(film_measure.print)+SUM(film_measure.no_good)+SUM(film_measure.waste) as Total
from material, film_stock, film_measure
where film_stock.Date < '2011-03-29'
	and film_stock.material_id = material.id
	and film_stock.used_today_id = film_measure.id
group by material.type
limit 3;

/* dprfilmused3subrpt.jrxml *
select material.type, film_measure.id,
SUM(film_measure.print) as Print, 
SUM(film_measure.no_good) as No_Good, 
SUM(film_measure.waste) as Waste, 
SUM(film_measure.print)+SUM(film_measure.no_good)+SUM(film_measure.waste) as Total
from material, film_stock, film_measure
where film_stock.Date <= '2011-03-29'
and film_stock.material_id = material.id
and film_stock.used_today_id = film_measure.id
group by material.type
limit 3;


/* dprfilmtypesubrpt.jrxml - film type headings (left labels) for "inventory" table
select material.type, film_measure.id, film_measure.gross, 
SUM(film_measure.print) as Print, 
SUM(film_measure.no_good) as No_Good, 
SUM(film_measure.waste) as Waste,
(film_measure.print+film_measure.no_good+film_measure.waste) as Total
from material, film_stock, film_measure
where film_stock.material_id = material.id
and film_stock.used_prior_id = film_measure.id
group by material.type
limit 6;
/*** Works - with date, not dpr
select m.type as type,
	case when (fs.date='2011-04-07') then fs.inventory_prior
		else (fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) end as inventory_prior,
	case when (fs.date='2011-04-07') then fs.inventory_received   else 0 end as inventory_received,
	case when (fs.date='2011-04-07') then fs.inventory_used_today else 0 end as inventory_used_today,
	(fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) as inventory_total
from film_stock fs, material m where
	fs.material_id = m.id and
	(fs.date, fs.material_id) IN 
		(Select max(f2.date),f2.material_id from Film_Stock f2, Material 
		where f2.date <= '2011-04-07'
		and f2.material_id = material.id 
		group by f2.material_id)
order by type
limit 6;



/** WORKS! uses dpr
select m.type as type,
case when (fs.date=dpr.date) then fs.inventory_prior
		else (fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) end as inventory_prior,
case when (fs.date=dpr.date) then fs.inventory_received   else 0 end as inventory_received,
case when (fs.date=dpr.date) then fs.inventory_used_today else 0 end as inventory_used_today,
(fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) as inventory_total

from film_stock fs, material m, dpr where
	dpr.date = '2011-04-05' and 
	fs.material_id = m.id and
	(fs.date, fs.material_id) IN 
					(Select max(f2.date),f2.material_id from Film_Stock f2, Material 
					 where f2.date <= dpr.date
					 and f2.material_id = material.id 
					 group by f2.material_id)
order by type;


/** perfect for matching date
select m.type, inventory_prior, inventory_received, inventory_used_today,
	(fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) as inventory_total

from film_stock fs, material m where 
	fs.material_id = m.id and
	(fs.date, fs.material_id) IN 
					(Select max(f2.date),f2.material_id from Film_Stock f2, Material 
					 where f2.date = '2011-04-05'
					 and f2.material_id = material.id 
					 group by f2.material_id)
	order by m.type;

/** Perfect when missing current date record:		*	 
select m.type,
	(fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) as inventory_prior,
	 0 as inventory_received,
	 0 as inventory_used_today,
	(fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) as inventory_total
from film_stock fs, material m where 
	fs.material_id = m.id and
	(fs.date, fs.material_id) IN 
					(Select max(f2.date),f2.material_id from Film_Stock f2, Material 
					 where f2.date < '2011-04-07'
					 and f2.material_id = material.id 
					 group by f2.material_id)
	order by m.type;
	
	
/*

select material.type, SUM(film_stock.inventory_prior) as inventory_prior , 
SUM(film_stock.inventory_received) as inventory_received,
SUM(film_stock.inventory_used_today) as inventory_used_today, 
SUM(film_stock.inventory_total) inventory_total
from material,film_stock
where film_stock.material_id = material.id
group by material.type
limit 6;

/* */