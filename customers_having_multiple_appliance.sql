select customer_id, count(*) from BURNER_QNT_CHANGE
group by customer_id
having count(*)>1
order by count(*) desc