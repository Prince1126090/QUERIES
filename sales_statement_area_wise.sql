/* Formatted on 3/18/2018 3:14:32 PM (QP5 v5.287) */
  SELECT MCC.CATEGORY_ID,
         CATEGORY_NAME,
         MCC.CATEGORY_TYPE,
         SUM (ACTUAL_EXCEPT_MINIMUM) ACTUAL_EXCEPT_MINIMUM,
         SUM (ACTUAL_WITH_MINIMUM) ACTUAL_WITH_MINIMUM,
         SUM (BILLING_UNIT) BILLING_UNIT,
         SUM (DIFFERENCE) DIFFERENCE,
         SUM (TOTAL_ACTUAL_CONSUMPTION) TOTAL_ACTUAL_CONSUMPTION,
         RATE,
         SUM (VALUE_OF_ACTUAL_CONSUMPTION) VALUE_OF_ACTUAL_CONSUMPTION,
         SUM (MINIMUM_CHARGE) MINIMUM_CHARGE,
         SUM (METER_RENT) METER_RENT,
         SUM (SURCHARGE_AMOUNT) SURCHARGE_AMOUNT,
         SUM (HHV_NHV_AMOUNT) HHV_NHV_AMOUNT,
         SUM (TOTAL_AMOUNT) TOTAL_AMOUNT
    FROM SALES_REPORT SR, CUSTOMER_CONNECTION conn, MST_CUSTOMER_CATEGORY mcc
   WHERE     SR.customer_id = conn.customer_id
         AND BILLING_MONTH = '01'
         AND BILLING_YEAR = '2018'
         AND SUBSTR (SR.customer_id, 3, 2) = MCC.CATEGORY_ID
         AND SUBSTR (SR.customer_id, 1, 2) = '01'
GROUP BY MCC.CATEGORY_ID,
         MCC.CATEGORY_NAME,
         MCC.CATEGORY_TYPE,
         RATE
ORDER BY MCC.CATEGORY_TYPE ASC, MCC.CATEGORY_ID ASC