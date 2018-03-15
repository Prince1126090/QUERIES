CREATE OR REPLACE PROCEDURE GenerateBill_Metered(
   iBillFor                   IN   VARCHAR2,
   iCustomerId                IN   VARCHAR2,
   iCategoryId                IN   VARCHAR2,  
   iAreaId                    IN   VARCHAR2,   
   iBillMonth                 IN   NUMBER,  
   iBillYear                  IN   NUMBER,
   iIssueDate                 IN   VARCHAR2, 
   iUserId                    IN   VARCHAR2, 
   iRemarks                   IN   VARCHAR2,  
   iReprocess                 IN   VARCHAR2, 
    iBillType                  IN   NUMBER,
   oResponse                  OUT  NUMBER,
   oRespMsg                   OUT  VARCHAR2
)

IS

  reading_cursor  sys_refcursor;
  where_clause varchar2(400);
  actual_consumption number;
  actual_consumption_rebate number;
  hhv_nhv_consumption number;
  other_consumption number;
  billed_consumption number;
  billId   varchar2(30);
  marginId number;
  tRate number;
  tCnt number;
  tVatAmount number;
  tSdAmount number;
  tOthersAmount number;
  tGovtAmount number;  
  tPbGasBillAmount number;
  tPbAmount    number;
  tMeterId     number;
  tMeterRent   number;
  tBillAmount  number;
  tTotalPayableAmount number;
  tCount number;
  tBillId4Reprocess varchar2(100);
  
  tcustomerName varchar2(100);
  tProprietorName varchar2(100); 
  tCustomerCategory varchar2(2);
  tCustomerCategoryName  varchar2(50);
  tCategoryType  varchar2(10);
  tAreaId  varchar2(2);
  tAreaName varchar2(50);
  tAddress  varchar2(500);
  tPhone    varchar2(200);
  tMobile   varchar2(200);
  
  tProportionalMinLoad number;
  tProportionalMaxLoad number;
  tActualMinLoad number;
  tBillForMinLoad number;
  tHhvNhvAmount number;
  
  tVatRebate number;
  tVatRebateAmount number;
  
  tPrice  number;
  tPbRate number;
  tVatRate number;
  tSdRate number;
  tOtherRate number;
  tReadingId number;
  tLockStatus number;
  tTotalReadingRecord number;
  tAmountInWord varchar2(400);
   isMinBill boolean:=true;
  l_start number;
  l_end number;
  l_diff number;
  
  holiday_counter NUMBER; 
  initialpaydate  DATE; 
  pay_date_holder Date;
  payDate_within_wo_sc Date;
  payDate_within_wo_sc_view Date; 
  payDate_within_w_sc Date;
  payDate_within_w_sc_view Date;
  
  oSubResponse   NUMBER; -- For Sub Connection of Mixed Consumption
  oSubRespMsg    varchar2(400); -- For Sub Connection of Mixed Consumption
  
  
 
  
  TYPE reading_ids_t IS TABLE OF METER_READING.READING_ID%TYPE
     INDEX BY PLS_INTEGER; 
  TYPE actual_consumption_t IS TABLE OF METER_READING.ACTUAL_CONSUMPTION%TYPE
     INDEX BY PLS_INTEGER;
  TYPE tariff_id_t IS TABLE OF METER_READING.TARIFF_ID%TYPE
     INDEX BY PLS_INTEGER;     
  TYPE rate_t IS TABLE OF METER_READING.RATE%TYPE
     INDEX BY PLS_INTEGER;
  TYPE hhv_nhv_adjustment_t IS TABLE OF VIEW_METER_READING.HHV_NHV_ADJUSTMENT%TYPE
     INDEX BY PLS_INTEGER; 
     
  TYPE customer_id_t IS TABLE OF CUSTOMER.CUSTOMER_ID%TYPE
     INDEX BY PLS_INTEGER;      
  TYPE customer_category_t IS TABLE OF CUSTOMER.CUSTOMER_CATEGORY%TYPE
     INDEX BY PLS_INTEGER; 
  TYPE pay_within_wo_sc_t IS TABLE OF CUSTOMER_CONNECTION.PAY_WITHIN_WO_SC%TYPE
     INDEX BY PLS_INTEGER; 
  TYPE pay_within_w_sc_t IS TABLE OF CUSTOMER_CONNECTION.PAY_WITHIN_W_SC%TYPE
     INDEX BY PLS_INTEGER;  
  TYPE max_load_t IS TABLE OF CUSTOMER_CONNECTION.MAX_LOAD%TYPE
     INDEX BY PLS_INTEGER; 
  TYPE min_load_t IS TABLE OF CUSTOMER_CONNECTION.MIN_LOAD%TYPE
     INDEX BY PLS_INTEGER; 
  TYPE has_sub_conn_t IS TABLE OF CUSTOMER_CONNECTION.HAS_SUB_CONNECTION%TYPE
     INDEX BY PLS_INTEGER; 
  TYPE parent_conn_t IS TABLE OF CUSTOMER_CONNECTION.PARENT_CONNECTION%TYPE
     INDEX BY PLS_INTEGER;    
  TYPE isMetered_t IS TABLE OF CUSTOMER_CONNECTION.ISMETERED %TYPE
     INDEX BY PLS_INTEGER;
  TYPE cunnection_date_t IS TABLE OF CUSTOMER_CONNECTION.CONNECTION_DATE %TYPE
     INDEX BY PLS_INTEGER;  
       
     
  
  TYPE holiday_t IS TABLE OF HOLIDAYS.HOLIDAY_DATE%TYPE
     INDEX BY PLS_INTEGER; 
  TYPE cachePayDate_t IS TABLE OF Date 
      INDEX BY VARCHAR2(64); 
  TYPE payDateHolder_t IS VARRAY(2) OF VARCHAR2(64);
  TYPE payDateRangeHolder_t IS VARRAY(2) OF INTEGER;    
       
  
                           
  l_reading_ids_t   reading_ids_t;
  l_actual_consumption_t   actual_consumption_t;
  l_tariff_id_t   tariff_id_t;
  l_rate_t   rate_t;
  l_hhv_nhv_adjustment_t   hhv_nhv_adjustment_t;
  
  l_customer_id_t   customer_id_t;
  l_sub_customer_id_t   customer_id_t;
  l_isMetered_t   isMetered_t;
  
  Vcategory customer_id_t;
  l_customer_category_t   customer_category_t;
  l_pay_within_wo_sc_t   pay_within_wo_sc_t;
  l_pay_within_w_sc_t   pay_within_w_sc_t;
  l_max_load_t   max_load_t;
  l_min_load_t   min_load_t;
  l_has_sub_conn_t   has_sub_conn_t;
  l_parent_conn_t   parent_conn_t;
  l_cunnection_date_t cunnection_date_t;
  
  l_holiday_t holiday_t;
  l_cachePayDate_t cachePayDate_t;
  l_payDateHolder_t payDateHolder_t;
  l_payDateRangeHolder_t payDateRangeHolder_t;
     
  tSubBillAmount number;
  tMainMeterBillRate number;
  tMixedConsumption number;
  
  tBillStatus number;
  
  tDisconnId number;
  tReconnCount number;
  VcategoryId varchar2(4);
  Vprice number;

BEGIN
     l_start := dbms_utility.get_time ;
     billed_consumption:=0;
     tBillAmount:=0;
     tBillForMinLoad:=0;
     tHhvNhvAmount:=0;
     
     If(iBillFor='individual_customer') Then 
       where_clause := ' and Customer_Id='''||iCustomerId||'''';
       Select Area into tAreaId from Customer Where customer_id= iCustomerId;
     ELSIF(iBillFor='category_wise') Then 
       where_clause := ' and category_id='''||iCategoryId || ''' and area_id= '''|| iAreaId||''''; 
         tAreaId:=iAreaId;     
     ELSIF(iBillFor='area_wise') Then 
       where_clause := ' and  area_id= '''|| iAreaId||''''; 
       tAreaId:=iAreaId;      
     ELSE
       oResponse:=-1; -- invalid billFor value
       oRespMsg:='Invalid iBillFor value';
       DBMS_OUTPUT.PUT_LINE ('Invalid iBillFor value');
       return;
     End If;
     
     
     Select status into tLockStatus From BILLING_SEMAPHORE Where AREA_ID=tAreaId and ISMETERED=1;
     
     If(tLockStatus=1 and iBillFor<>'individual_customer') Then
        oResponse:=2;
        oRespMsg:='Failed to Take the lock control';
        return;
     End If;
     
     MERGE INTO BILLING_SEMAPHORE BS
        USING (SELECT tAreaId Area_id,1 IsMetered FROM dual ) BS1
        ON (BS.AREA_ID = BS1.AREA_ID and BS.ISMETERED = BS1.ISMETERED)
        WHEN MATCHED THEN 
        UPDATE SET BS.STATUS=1
        WHEN NOT MATCHED THEN
             INSERT (AREA_ID,ISMETERED,PROCESSED_BY,STATUS)
             VALUES(tAreaId,1,iUserId,1);
    
    EXECUTE IMMEDIATE
        'SELECT customer_id,category_id,pay_within_wo_sc,pay_within_w_sc,max_load, min_load,HAS_SUB_CONNECTION,CATEGORY_TYPE FROM MVIEW_CUSTOMER_INFO 
          WHERE ismetered = 1 And  customer_id in (select  customer_id  From METER_READING Where reading_purpose in (2,4,5,3) and billing_month='||iBillMonth||' and billing_year='||iBillYear||' ) '|| where_clause 
    BULK COLLECT INTO l_customer_id_t,l_customer_category_t,l_pay_within_wo_sc_t,l_pay_within_w_sc_t,l_max_load_t,l_min_load_t,l_has_sub_conn_t, Vcategory;
    
    EXECUTE IMMEDIATE 
    'select distinct HOLIDAY_DATE from HOLIDAYS where HOLIDAY_DATE BETWEEN to_date('''|| 
    iIssueDate||''',''dd-MM-YYYY'') AND to_date('''||iIssueDate|| 
    ''',''dd-MM-YYYY'')+100 order by HOLIDAY_DATE'bulk collect INTO l_holiday_t; 
   
    DBMS_OUTPUT.Put_line(l_customer_id_t.COUNT);
    
    FOR indx IN 1 .. l_customer_id_t.COUNT
     LOOP
         tMixedConsumption:=0;
         -- Start : MainMeter Billing with Sub Meter/NonMeter Connection
         If(l_has_sub_conn_t(indx)='Y') 
         Then
            DBMS_OUTPUT.Put_line('Mixed Consumption Detected');
            
            Select Customer_Id,IsMetered
             BULK COLLECT INTO l_sub_customer_id_t,l_isMetered_t
             From Customer_Connection Where  Parent_Connection=l_customer_id_t(indx);
             
             tMixedConsumption:=0;
             --- why rate is summed
             Select distinct(RATE) InTo tMainMeterBillRate From VIEW_METER_READING 
             Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear and reading_purpose in (2,4,5,3);
             
             FOR sIndx in 1 .. l_sub_customer_id_t.count LOOP
                
                BEGIN
                     If(l_isMetered_t(sIndx)=1) Then
                            
                            Select count(*) InTo tCnt From BILL_METERED Where Customer_Id=l_sub_customer_id_t(sIndx) and Bill_Month=iBillMonth and Bill_Year=iBillYear;
                            
                            if(tCnt=0) then
                                GenerateBill_Metered('individual_customer',l_sub_customer_id_t(sIndx),'','',iBillMonth,iBillYear,iIssueDate,iUserId,'',iReprocess,1,oSubResponse,oSubRespMsg);
                            end if;
                            
                                --Select BILLED_AMOUNT InTo tSubBillAmount From BILL_METERED Where Customer_Id=l_sub_customer_id_t(sIndx) and Bill_Month=iBillMonth and Bill_Year=iBillYear;
                                --tMixedConsumption:=tMixedConsumption+tSubBillAmount/tMainMeterBillRate;
                                
                                --BILLED_CONSUMPTION
                                
                                Select BILLED_CONSUMPTION InTo tSubBillAmount From BILL_METERED Where Customer_Id=l_sub_customer_id_t(sIndx) and Bill_Month=iBillMonth and Bill_Year=iBillYear;
                                tMixedConsumption:=tMixedConsumption+tSubBillAmount;
                                                            
                             
       
                        Else                                 
                            /*select decode(Vcategory(indx),'PVT','03','10') into VcategoryId  from dual;
                            SELECT PRICE  into Vprice FROM MST_TARIFF WHERE CUSTOMER_CATEGORY_ID=VcategoryId
                            AND TRUNC(EFFECTIVE_FROM) <=TO_DATE(iIssueDate,'DD-MM-YYYY')  
                            AND EFFECTIVE_TO is null;
                            
                           select sum(NEW_APPLIANCE_QNT_BILLCAL*APPLIANCE_RATE) InTo tSubBillAmount from 
                            (select CUSTOMER_ID,APPLIANCE_TYPE_CODE,NEW_APPLIANCE_QNT_BILLCAL from BURNER_QNT_CHANGE where (APPLIANCE_TYPE_CODE,pid) in(
                            select APPLIANCE_TYPE_CODE,max(pid) from BURNER_QNT_CHANGE where CUSTOMER_ID=l_sub_customer_id_t(sIndx)
                            AND TRUNC(EFFECTIVE_DATE) <=TO_DATE(iIssueDate,'DD-MM-YYYY') 
                            group by APPLIANCE_TYPE_CODE)) tm1, 
                            (select * from APPLIANCE_RATE_HISTORY where (AREA_ID,APPLIANCE_ID,SLNO) in(
                                select AREA_ID,APPLIANCE_ID,max(SLNO) 
                                from APPLIANCE_RATE_HISTORY
                                where  Effective_From<=to_date('01-'||to_char(to_date(iIssueDate,'dd-mm-YYYY'), 'mm-YYYY'),'dd-MM-YYYY HH24:MI:SS')
                                And (Effective_To is Null or Effective_To>=to_date('01-'||to_char(to_date(iIssueDate,'dd-mm-YYYY'), 'mm-YYYY'),'dd-MM-YYYY HH24:MI:SS'))
                                and AREA_ID=substr(l_sub_customer_id_t(sIndx),1,2)
                                group by AREA_ID,APPLIANCE_ID)) tm2
                            where tm1.APPLIANCE_TYPE_CODE=tm2.APPLIANCE_ID;
                            
                            tMixedConsumption:=tMixedConsumption+round(tSubBillAmount/Vprice,3); */
                            Select count(*) InTo tCnt From BILL_non_METERED Where Customer_Id=l_sub_customer_id_t(sIndx) and Bill_Month=iBillMonth and Bill_Year=iBillYear;
                            
                            if(tCnt=0) then
                                GenerateBill_NonMetered('individual_customer',l_sub_customer_id_t(sIndx),'','',iBillMonth,iBillYear,iIssueDate,iUserId,'',iReprocess,1,oSubResponse,oSubRespMsg);
                            end if;
                            
                            
                             Select TOTAL_CONSUMPTION InTo tSubBillAmount From BILL_NON_METERED Where Customer_Id=l_sub_customer_id_t(sIndx) and Bill_Month=iBillMonth and Bill_Year=iBillYear;
                             tMixedConsumption:=tMixedConsumption+tSubBillAmount;
                               
                            
                       
                     End If;
                End; 
               
              END LOOP;
                    
                                         
         End If;
         
         -- End : MainMeter Billing with Sub Meter/NonMeter Connection
         
         -- Newly Added : Check for Disconnection - Recononection Information for a customer
         -- It checkes whether the customer is in disconnected stage or during the billing month         
        -- Select Max(PID) InTo tDisconnId From DISCONN_METERED
         --Where Customer_Id=l_customer_id_t(indx) and Disconnect_Date in 
           -- (Select Max(Disconnect_Date) From DISCONN_METERED Where Customer_Id=l_customer_id_t(indx) and Disconnect_Date<to_date('01-'||iBillMonth||'-'||iBillYear,'dd-MM-YYYY'));


         --Select Count(PID) Into tReconnCount  From RECONN_METERED Where Customer_Id=l_customer_id_t(indx) and Disconnect_Id=tDisconnId
         --And RECONNECT_DATE<to_date('01-'||iBillMonth||'-'||iBillYear,'dd-MM-YYYY');
           
         --If(tReconnCount=0) Then
         --   CONTINUE; 
        -- End If;
         -- End of Disconnected Customer Checking ---
         
         
         
         Select count(Reading_Id) Into tTotalReadingRecord from VIEW_METER_READING Where Customer_Id=l_customer_id_t(indx) and Billing_Month=iBillMonth and Billing_Year=iBillYear;
         IF tTotalReadingRecord=0  THEN
          CONTINUE;
         END IF;
  
         Select  Count(Bill_Id) InTo tCount From BILL_METERED Where Customer_Id=l_customer_id_t(indx) and Bill_Month=iBillMonth and Bill_Year=iBillYear;                  
         
         If(tCount>0)
         Then  
              If(iReprocess='Y') Then   
                   Select  status InTo  tBillStatus From BILL_METERED Where Customer_Id=l_customer_id_t(indx) and Bill_Month=iBillMonth and Bill_Year=iBillYear;          
                  If(tBillStatus=0) Then
                       Select  BILL_ID Into tBillId4Reprocess From BILL_METERED Where Customer_Id=l_customer_id_t(indx) and Bill_Month=iBillMonth and Bill_Year=iBillYear;
                       Delete BILLING_READING_MAP Where Bill_Id =tBillId4Reprocess;
                       Delete BILL_METERED Where Customer_Id=l_customer_id_t(indx) and Bill_Month=iBillMonth and Bill_Year=iBillYear;               
                       Delete DTL_MARGIN_GOVT Where Bill_Id =tBillId4Reprocess;
                       Delete DTL_MARGIN_PB Where Bill_Id =tBillId4Reprocess;
                       Delete SUMMARY_MARGIN_GOVT Where Bill_Id =tBillId4Reprocess;
                       Delete SUMMARY_MARGIN_PB Where Bill_Id =tBillId4Reprocess;
                       Delete SALES_REPORT Where Customer_Id=l_customer_id_t(indx) and Bill_Id =tBillId4Reprocess;
                   Else
                       CONTINUE;
                   End If;
               
              ElsIf(iReprocess='N') Then
                   CONTINUE;
              End If;

         End If;
         
         
         --Select SQN_BILL_METERED.nextval into billId from dual;  
         billId:=iBillYear||lpad(iBillMonth,2,0)||l_customer_id_t(indx);
        
         /** 2 -> General Billing; 3 -> Average Billing; 4 -> Disconnect Meter; 5 -> Pressure Change; 6 -> Bill at Max Load; 8 -> Adjustment Billing **/
         
         --Step 1 : Prepare Basic data for Billing  and Insert into BILL_METERED Table;
         
         Select sum(NVL(actual_consumption,0)),(sum(NVL(total_consumption,0))-sum(NVL(actual_consumption,0))) into actual_consumption,hhv_nhv_consumption  
         From VIEW_METER_READING Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear and reading_purpose in (2,3,4,6,5,10);
          
         Select sum(NVL(actual_consumption,0)) InTo other_consumption From VIEW_METER_READING Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear 
         and reading_purpose in (8);
                  
         billed_consumption:=NVL(actual_consumption,0)+NVL(other_consumption,0)-NVL(tMixedConsumption,0);
         If(billed_consumption<0) Then
         billed_consumption:=0;
         End If;
         
         Insert Into SALES_REPORT(CUSTOMER_ID,BILLING_MONTH,BILLING_YEAR,BILL_ID)
         Values(l_customer_id_t(indx),iBillMonth,iBillYear,billId);
         
         
         
         

         Select READING_ID
         BULK COLLECT INTO l_reading_ids_t
         From METER_READING Where customer_id=l_customer_id_t(indx)and billing_month=iBillMonth and billing_year=iBillYear;
         
         FORALL rIndx IN l_reading_ids_t.FIRST .. l_reading_ids_t.LAST
            Insert InTo BILLING_READING_MAP(BILL_ID,READING_ID) Values(billId,l_reading_ids_t(rIndx));  
            
            
                                                                     
         --------------------------------------------------------------------------------------
         --PayDate within without and with surchrarge calculation(Consider holiday)
         --------------------------------------------------------------------------------------
            l_payDateRangeHolder_t:=payDateRangeHolder_t(l_pay_within_wo_sc_t(indx),l_pay_within_w_sc_t(indx));
          
            FOR i in 1 .. l_payDateRangeHolder_t.count LOOP
            
                    BEGIN
                         pay_date_holder:=l_cachePayDate_t(to_char(iIssueDate)||to_char(l_payDateRangeHolder_t(i)));
                      EXCEPTION
                        WHEN no_data_found 
                         THEN
                            initialpaydate := To_date(iIssueDate, 'dd-MM-YYYY') + (l_payDateRangeHolder_t(i)-1); 
                            pay_date_holder:=initialpaydate;
                            IF i=1THEN
                                payDate_within_wo_sc_view:=pay_date_holder;
                            ELSE
                                payDate_within_w_sc_view:=pay_date_holder;
                            END IF;
                            
                                FOR indx IN 1 .. l_holiday_t.COUNT
                                    LOOP
                                            IF pay_date_holder = l_holiday_t(indx) THEN
                                               pay_date_holder:=pay_date_holder+1;                                          
                                            END IF;
                                END LOOP;
                                                    
                                                 
                           l_cachePayDate_t(to_char(iIssueDate)||to_char(l_payDateRangeHolder_t(i))):=pay_date_holder;
                    END;
                      IF i=1THEN
                        payDate_within_wo_sc:=pay_date_holder;
                        ELSE
                        payDate_within_w_sc:=pay_date_holder;
                      END IF;
            END LOOP;
        
            if(payDate_within_w_sc_view is null) then
                    payDate_within_w_sc_view:=payDate_within_w_sc;
            end if;
            
         getMeterRentForBilling(l_customer_id_t(indx),iBillMonth,iBillYear,tMeterRent);
         Insert Into BILL_METERED(BILL_ID,INVOICE_NO,BILL_MONTH,BILL_YEAR,CUSTOMER_ID,ISSUE_DATE,LAST_PAY_DATE_WO_SC,LAST_PAY_DATE_WO_SC_VIEW,LAST_PAY_DATE_W_SC,LAST_PAY_DATE_W_SC_VIEW,LAST_DISCONN_RECONN_DATE,
         MONTHLY_CONTACTUAL_LOAD,MINIMUM_LOAD,ACTUAL_GAS_CONSUMPTION,OTHER_CONSUMPTION,MIXED_CONSUMPTION,BILLED_CONSUMPTION,PREPARED_BY,PREPARED_DATE,METER_RENT) 
         Values(billId,null,iBillMonth,iBillYear,l_customer_id_t(indx),to_date(iIssueDate,'dd-MM-YYYY'),payDate_within_wo_sc,payDate_within_wo_sc,payDate_within_w_sc,payDate_within_w_sc,
         getPreviousDisReConnDate(l_customer_id_t(indx)),l_max_load_t(indx),l_min_load_t(indx),round(actual_consumption,3),round(other_consumption,3),round(NVL(tMixedConsumption,0),3),round(billed_consumption,3),iUserId,SYSDATE,tMeterRent);
         

         Select full_name,proprietor_name,category_id,category_name,Category_Type,area_ID,area_name,address,VAT_REBATE,Phone,Mobile into 
         tcustomerName,tProprietorName,tCustomerCategory,tCustomerCategoryName,tCategoryType,tAreaId,tAreaName,tAddress,tVatRebate,tPhone,tMobile         
         From MVIEW_CUSTOMER_INFO Where customer_id=l_customer_id_t(indx);     
         
         --Step 2 : Fetch current active margin info for Govt. 
         --Step 3 : Fetch current active margin info for Petro Bangla.
         
         l_reading_ids_t.delete();
         If(l_has_sub_conn_t(indx)='Y') Then
                         
         Select Max(READING_ID) InTo l_reading_ids_t(1) from VIEW_METER_READING  Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear;
         l_actual_consumption_t(1) :=billed_consumption;
         Select Max(TARIFF_ID),RATE InTo l_tariff_id_t(1),l_rate_t(1) From VIEW_METER_READING  Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear  group by RATE ;
         Select Avg(HHV_NHV_ADJUSTMENT) into l_hhv_nhv_adjustment_t(1) From VIEW_METER_READING  Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear;
         
         Else
         
         Select READING_ID,ACTUAL_CONSUMPTION,TARIFF_ID,RATE,HHV_NHV_ADJUSTMENT
         BULK COLLECT INTO l_reading_ids_t,l_actual_consumption_t,l_tariff_id_t,l_rate_t,l_hhv_nhv_adjustment_t
         From VIEW_METER_READING Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear;
         
         End If;
         
         FOR mIndx IN 1 .. l_reading_ids_t.COUNT
             LOOP
                Select  PRICE,PB,VAT,SD,OTHER into tPrice,tPbRate,tVatRate,tSdRate,tOtherRate From MST_TARIFF Where Tariff_Id=l_tariff_id_t(mIndx);                        
                INSERT ALL
                  WHEN 1 = 1                
                    THEN
                        INTO DTL_MARGIN_GOVT(BILL_ID,READING_ID,CATEGORY,ACTUAL_CONSUMPTION,TARIFF_ID,RATE,PARTIAL_RATE,AMOUNT) Values(billId,l_reading_ids_t(mIndx),'VAT',l_actual_consumption_t(mIndx),l_tariff_id_t(mIndx),l_rate_t(mIndx),tVatRate,round(tVatRate*l_actual_consumption_t(mIndx),2))
                        INTO DTL_MARGIN_GOVT(BILL_ID,READING_ID,CATEGORY,ACTUAL_CONSUMPTION,TARIFF_ID,RATE,PARTIAL_RATE,AMOUNT) Values(billId,l_reading_ids_t(mIndx),'SD',l_actual_consumption_t(mIndx),l_tariff_id_t(mIndx),l_rate_t(mIndx),tSdRate,round(tSdRate*l_actual_consumption_t(mIndx),2))
                        INTO DTL_MARGIN_GOVT(BILL_ID,READING_ID,CATEGORY,ACTUAL_CONSUMPTION,TARIFF_ID,RATE,PARTIAL_RATE,AMOUNT) Values(billId,l_reading_ids_t(mIndx),'OTHERS',l_actual_consumption_t(mIndx),l_tariff_id_t(mIndx),l_rate_t(mIndx),tOtherRate,round(tOtherRate*l_actual_consumption_t(mIndx),2))
                        INTO DTL_MARGIN_PB(BILL_ID,READING_ID,CATEGORY,CONSUMPTION,TARIFF_ID,RATE,PARTIAL_RATE,AMOUNT)          Values(billId,l_reading_ids_t(mIndx),'GAS_BILL',l_actual_consumption_t(mIndx),l_tariff_id_t(mIndx),l_rate_t(mIndx),tPbRate,round(tPbRate*l_actual_consumption_t(mIndx),2))                             
                  WHEN l_hhv_nhv_adjustment_t(mIndx)!=0 THEN
                        INTO DTL_MARGIN_PB(BILL_ID,READING_ID,CATEGORY,CONSUMPTION,TARIFF_ID,RATE,PARTIAL_RATE,AMOUNT)          Values(billId,l_reading_ids_t(mIndx),'HHV_NHV_BILL',l_hhv_nhv_adjustment_t(mIndx),l_tariff_id_t(mIndx),l_rate_t(mIndx),tPbRate,round(tPrice*l_hhv_nhv_adjustment_t(mIndx),2))                 
                  SELECT *  FROM dual;                                                           
             END LOOP;
         
         
            -- SaleReport rate update--
            Select rate into tPrice from meter_reading where reading_id=
            (Select max(reading_id) from meter_reading Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear)  ;       
         
            Update SALES_REPORT set RATE=tPrice  Where Customer_Id=l_customer_id_t(indx) and Bill_Id=billId;
           ------------------------------------
         
         
            --Step 2(a) : Calcuate and take the summery from Govt. Margin Detail to Govt. Margin Summary
            Select sum(decode(category,'VAT',amount,0)),sum(decode(category,'SD',amount,0)),sum(decode(category,'OTHERS',amount,0))  INTO   tVatAmount,tSdAmount,tOthersAmount
            From   DTL_MARGIN_GOVT     where  Bill_id=billId   and category in ('VAT','SD','OTHERS');

        
            tGovtAmount:=nvl(tVatAmount,0)+nvl(tSdAmount,0)+nvl(tOthersAmount,0);
        
            Insert into SUMMARY_MARGIN_GOVT(BILL_ID,VAT_AMOUNT,SD_AMOUNT,OTHERS_AMOUNT,TOTAL_AMOUNT) 
            Values(billId,round(tVatAmount,0),round(tSdAmount,0),round(tOthersAmount,0),round(tGovtAmount,0));
                                     
                  
           --Step 3(a) : Calcuate and take the summery from PB. Margin Detail to PB. Margin Summary            
            Select sum(decode(category,'GAS_BILL',amount,0)),sum(decode(category,'HHV_NHV_BILL',amount,0)) INTO   tPbGasBillAmount,tHhvNhvAmount
            From   DTL_MARGIN_PB     where  Bill_id=billId   and category in ('GAS_BILL','HHV_NHV_BILL');
            
            tPbAmount:=NVL(tPbGasBillAmount,0)+NVL(tHhvNhvAmount,0);
        
            tMeterRent:=0;
            
             --4(a) : Calcuate and Set Meter Rent Amount
              getMeterRentForBilling(l_customer_id_t(indx),iBillMonth,iBillYear,tMeterRent);
            
            Insert Into SUMMARY_MARGIN_PB(BILL_ID,GAS_BILL,HHV_NHV_BILL,METER_RENT,TOTAL_AMOUNT)
            Values(billId,round(tPbGasBillAmount,0),round(tHhvNhvAmount,0),tMeterRent,round(tPbAmount,0)); 
        
        
        --Step 4: Update various informat about Petro Bangla Margin
        
        
             
              If(tMeterRent is not Null) Then
                Insert into METER_RENT_BILLING(BILL_ID,METER_ID,METER_RENT) Values(billId,tMeterId,tMeterRent);
                --Update SUMMARY_MARGIN_PB Set Meter_Rent=round(tMeterRent,0) Where Bill_Id=billId;
                tPbAmount:=tPbAmount+round(tMeterRent,0);
              End If;
        DBMS_OUTPUT.PUT_LINE (tMeterRent);


            Select Max(PID) InTo tDisconnId From DISCONN_METERED
            Where Customer_Id=l_customer_id_t(indx) and Disconnect_Date in 
            (Select Max(Disconnect_Date) From DISCONN_METERED Where Customer_Id=l_customer_id_t(indx) and Disconnect_Date<to_date('01-'||iBillMonth||'-'||iBillYear,'dd-MM-YYYY'));
           
            Select Count(nvl(PID,0)) Into tReconnCount  From RECONN_METERED Where Customer_Id=l_customer_id_t(indx) and Disconnect_Id=tDisconnId
            And RECONNECT_DATE<to_date('01-'||iBillMonth||'-'||iBillYear,'dd-MM-YYYY');
            
            IF(tReconnCount=0) THEN
            Select sum(NVL(pMIN_LOAD,0)),sum(NVL(pMAX_LOAD,0)),sum(DISTINCT(NVL(pMIN_LOAD,0))) into tProportionalMinLoad,tProportionalMaxLoad,tActualMinLoad From VIEW_METER_READING 
            Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear and reading_purpose in (2,3,4,5);
            END IF;

            if(tReconnCount>0) THEN
            --Step 4(b) : Calcuate and Set Minimum Bill Amount
            Select sum(NVL(PMIN_LOAD,0)),sum(NVL(PMAX_LOAD,0)),sum(DISTINCT(NVL(MIN_LOAD,0))) into tProportionalMinLoad,tProportionalMaxLoad,tActualMinLoad From VIEW_METER_READING 
            Where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear and reading_purpose in (2,3,4,5);
          --tanmay has question on below block when multiple rate
            END IF;
          
         
            If(tProportionalMinLoad>tActualMinLoad) Then
                tProportionalMinLoad:=tActualMinLoad;
            End If;    
          
          
          
            If( billed_consumption<tProportionalMinLoad AND l_customer_category_t(indx)<>9) Then
                tBillForMinLoad:=(tProportionalMinLoad-billed_consumption)*tPrice;
                Update SUMMARY_MARGIN_PB Set MIN_LOAD_BILL=round(nvl(tBillForMinLoad,0),0) Where bill_id=billId;
                tPbAmount:=tPbAmount+nvl(tBillForMinLoad,0);
                Update SALES_REPORT Set 
                    ACTUAL_EXCEPT_MINIMUM=0,
                    ACTUAL_WITH_MINIMUM=round(billed_consumption,3),
                    BILLING_UNIT=round(tProportionalMinLoad,3),
                    DIFFERENCE=round((round(tProportionalMinLoad,3)-round(billed_consumption,3)),3),
                    TOTAL_ACTUAL_CONSUMPTION=round(billed_consumption,3),
                    VALUE_OF_ACTUAL_CONSUMPTION=round((tGovtAmount+NVL(tPbGasBillAmount,0)),0),
                    MINIMUM_CHARGE=round(tBillForMinLoad,0),
                    METER_RENT=tMeterRent,
                    HHV_NHV_AMOUNT=round(NVL(tHhvNhvAmount,0),0)
                    Where Customer_Id=l_customer_id_t(indx) and Bill_Id=billId;
            Else
                    Update SALES_REPORT Set 
                    ACTUAL_EXCEPT_MINIMUM=round(billed_consumption,3),
                    TOTAL_ACTUAL_CONSUMPTION=round(billed_consumption,3),
                    VALUE_OF_ACTUAL_CONSUMPTION=round((tGovtAmount+NVL(tPbGasBillAmount,0)),0),
                    METER_RENT=tMeterRent,
                    HHV_NHV_AMOUNT=round(NVL(tHhvNhvAmount,0),0),
                    IS_METER=1
                    Where Customer_Id=l_customer_id_t(indx) and Bill_Id=billId;        
            End If;
            
            --Setp 4(c) : Consider the Surcharge Amount  
                --No Need to consider it during bill processing.
                --When the bill processing will be done then from the SURCHARGE interface this information will be added.
                
            --Step 4(d) : Adjustment and Other will be consider from Adjustment and Other Interface. So we don't need to think about it in bill processing stage.
            
                
            --Step 4(e) : Calcuate and set Vat Rebate Percentance and Amount
            
            
            select sum(nvl(ACTUAL_CONSUMPTION,0)) into actual_consumption_rebate from view_meter_reading where customer_id=l_customer_id_t(indx) and billing_month=iBillMonth and billing_year=iBillYear AND nvl(vat_rebate,0)>0;
            
            If(nvl(actual_consumption_rebate,0)>0) Then
              ---tVatRebateAmount:=(tVatRebate/100)*tVatAmount;
               tVatRebateAmount := (actual_consumption_rebate*tVatRate)-(actual_consumption_rebate*round(tVatRate*0.2,4)); -- by toraf
            End If;
            Update SUMMARY_MARGIN_PB Set Total_Amount=round(tPbAmount-round(NVL(tVatRebateAmount,0),0),0),VAT_REBATE_AMOUNT=round(tVatRebateAmount,0) Where bill_id=billId;
          
        
        --Step 5 : Calculate Total Payable Amount 
        
        
        
        
        tGovtAmount:=nvl(tVatAmount,0)+nvl(tSdAmount,0)+nvl(tOthersAmount,0);
        tTotalPayableAmount:=round(tGovtAmount+tPbAmount,0);
        tBillAmount:=tTotalPayableAmount-NVL(tVatRebateAmount,0);  --Surcharge Amount pore bosate hobe
        
        tBillAmount:=round(tBillAmount);
        
        Update SALES_REPORT Set VAT_REBATE=round(tVatRebateAmount,0) ,TOTAL_AMOUNT=round(tTotalPayableAmount,0) where Customer_Id=l_customer_id_t(indx) and Bill_Id=billId;
        
        Select NUMBER_SPELLOUT_FUNC(to_char(tBillAmount, 'fm999999999999.9999')) InTo tAmountInWord From Dual;
        Update BILL_METERED Set CUSTOMER_NAME=tcustomerName, PROPRIETOR_NAME=tProprietorName, CUSTOMER_CATEGORY=tCustomerCategory, CUSTOMER_CATEGORY_NAME=tCustomerCategoryName, 
        AREA_ID=tAreaId,AREA_NAME=tAreaName, ADDRESS=tAddress,PHONE=tPhone, MOBILE=tMobile,
        Billed_Amount=tBillAmount,Payable_Amount=to_char(tBillAmount, 'fm999999999999.9999'), Amount_In_Word=tAmountInWord
        Where bill_id=billId;
         
      END LOOP;
     Update BILLING_SEMAPHORE Set Status=0 Where Area_Id=tAreaId and ISMETERED=1;
     oResponse:=1;
     oRespMsg:='Successfully Bill Processed.';
     
     l_end := dbms_utility.get_time ;
     l_diff := (l_end-l_start)/100;
     dbms_output.put_line('Elapsed Time '||l_diff|| ' secs');
        
EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK;
      oResponse:=500;
      oRespMsg:='Exception Occured : '|| 'Error Code : '||SQLCODE|| ', Error Message : ' || SUBSTR(SQLERRM, 1, 400)||DBMS_UTILITY.format_error_backtrace;
      Update BILLING_SEMAPHORE Set Status=0 Where Area_Id=tAreaId and ISMETERED=1;
      
     
END GenerateBill_Metered;
/