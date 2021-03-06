create or replace view v_invoice_adjustments_per_day as
select
  currency as pivot
, date_format(created_date, '%Y-%m-%d') as day
, sum(amount) as count
from bia
where report_group = 'default'
group by 1, 2
order by 1, 2 asc
;
