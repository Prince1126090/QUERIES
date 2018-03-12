CREATE OR REPLACE FUNCTION JALALABAD.GET_NON_BILL_AMOUNT(
   iCustomerId           VARCHAR2,
   iFrom_month           VARCHAR2,
   iFrom_year            VARCHAR2,
   iTo_month             VARCHAR2,
   iTo_year              VARCHAR2,
   iCollectionDate       varchar2)
return  VARCHAR2
as iBalance VARCHAR2(500);
iFrom_month_year number;
iTo_month_year  number;
tBillAmount   number;
tAsurcharge   number;
tCAmount    number;
tCsurcharge  number;
tRemainAmount number;
tRemainSur    number;

BEGIN
    
    iFrom_month_year :=TO_NUMBER(iFrom_year||LPAD (iFrom_month, 2, 0));
    iTo_month_year :=TO_NUMBER(iTo_year||LPAD (iTo_month, 2, 0));
    
    
    select sum(BILLED_AMOUNT), sum(ACTUAL_SURCHARGE), sum(COLLECTED_BILLED_AMOUNT), sum(COLLECTED_SURCHARGE) 
    into tBillAmount,tAsurcharge,tCAmount,tCsurcharge
    from (
    select customer_id,BILL_ID,BILLED_AMOUNT,CALCUALTESURCHARGE(BILL_ID,iCollectionDate) ACTUAL_SURCHARGE,
    nvl(COLLECTED_BILLED_AMOUNT,0) COLLECTED_BILLED_AMOUNT , nvl(COLLECTED_SURCHARGE,0) COLLECTED_SURCHARGE
    from bill_non_metered
    where customer_id=iCustomerId and status=1 AND TO_NUMBER (bill_year || LPAD (bill_month, 2, 0)) BETWEEN iFrom_month_year and iTo_month_year
    );
    
    
    tRemainAmount:=tBillAmount-tCAmount;
    tRemainSur:=tAsurcharge-tCsurcharge;
    
    
    iBalance:=tRemainAmount||'#'||tRemainSur;
    
    return iBalance;
    
 EXCEPTION
  WHEN NO_DATA_FOUND
  THEN
    RETURN '0#0'; 
    
    
END;
/