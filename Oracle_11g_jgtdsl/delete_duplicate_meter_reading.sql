/* Formatted on 4/15/2018 12:47:33 PM (QP5 v5.287) */
DELETE FROM METER_READING
      WHERE     ROWID NOT IN
                   (  SELECT MIN (ROWID)
                        FROM METER_READING
                       WHERE     CUSTOMER_ID LIKE '2410%'
                             AND BILLING_YEAR = 2018
                             AND BILLING_MONTH = 3
                    GROUP BY CUSTOMER_ID,
                             BILLING_YEAR,
                             BILLING_MONTH,
                             PREV_READING,
                             CURR_READING)
            AND CUSTOMER_ID LIKE '2410%'
            AND BILLING_YEAR = 2018
            AND BILLING_MONTH = 3