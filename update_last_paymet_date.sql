/* Formatted on 3/7/2018 4:35:34 PM (QP5 v5.287) */
UPDATE BILL_METERED
   SET LAST_PAY_DATE_WO_SC = TO_DATE ('3/25/2018', 'MM/DD/YYYY')
 WHERE CUSTOMER_ID LIKE '0302%' AND BILL_MONTH = 02 AND BILL_YEAR = 2018