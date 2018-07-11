
select * from bill_non_metered where CUSTOMER_ID in(
select CUSTOMER_ID from customer_connection
where PARENT_CONNECTION='020300093') and bill_month=1 and bill_year='2018'
and TOTAL_CONSUMPTION= 87.91