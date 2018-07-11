CREATE OR REPLACE PROCEDURE JALALABAD.CODELESS_COLLECTION(
   iCustomerId          In VARCHAR2,
   iName                In VARCHAR2,
   iAddress             In VARCHAR2,
   iBankId              In VARCHAR2,
   iBranchId            In VARCHAR2,
   iAcountNo            In VARCHAR2,
   iCollectionDate      In VARCHAR2,     
   iBill_amount         IN Number,
   iSurcharge           IN Number,
   iFrom_month          IN VARCHAR2,
   iFrom_year           IN VARCHAR2,
   iTo_month            IN VARCHAR2,
   iTo_year             IN VARCHAR2,
   iInsertBy            In VARCHAR2, 
   iAreaId              in VARCHAR2,
   oResponse            OUT  NUMBER,
   oRespMsg             OUT  VARCHAR2
   
)
is
vSqCode NUMBER;


BEGIN   

   
    select SQN_CODELESS.nextval into vSqCode from dual; 
    
        INSERT INTO CODELESS_PAYMENT (
           CODELESS_NO, CUSTOMER_ID, CUSTOMER_NAME, 
           ADDRESS, MONTH_FROM, YEAR_FROM, 
           MONTH_TO, YEAR_TO, COLLECTED_AMOUNT,COLLECTED_SURCHARGE, 
           COLLECTION_DATE, BANK_ID, BRANCH_ID, 
           ACCOUNT_NO, STATUS, INSERTED_BY, 
           INSERTED_ON, AREA_ID) 
        VALUES (vSqCode,trim(iCustomerId),iName,
                iAddress,iFrom_month, iFrom_year,
                iTo_month,iTo_year, iBill_amount,iSurcharge,
                to_date(iCollectionDate,'dd-mm-yyyy'),iBankId,iBranchId,
                 iAcountNo, 0, iInsertBy,
                 sysdate,  iAreaId );
                 
                 
                 
        INSERT INTO BANK_ACCOUNT_LEDGER (
                   TRANS_ID, TRANS_DATE, TRANS_TYPE, 
                   PARTICULARS, BANK_ID, BRANCH_ID, 
                   ACCOUNT_NO, DEBIT, CREDIT, 
                   BALANCE, REF_ID, INSERTED_ON, 
                   INSERTED_BY, CUSTOMER_ID, STATUS, 
                   METER_RENT, SURCHARGE, ACTUAL_REVENUE, 
                   MISCELLANEOUS) 
                VALUES ( SQN_BAL.nextval, TO_DATE(iCollectionDate, 'DD-MM-YYYY'), 1,
                 'Collection By Bank ', iBankId, iBranchId, 
                 iAcountNo, nvl(iSurcharge,0)+nvl(iBill_amount,0), 0,
                 0, 'C-'||vSqCode, sysdate,
                 iInsertBy, trim(iCustomerId), 0,
                 0, nvl(iSurcharge,0), nvl(iBill_amount,0),
                 0 );             
                 
                 
    
    oResponse:=1;
    oRespMsg:='Successfully Saved Code Less Information';
    COMMIT; 
    
    EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK;
      oResponse:=500;
      IF SQLCODE=-1 THEN
      oRespMsg:='Od';
      ELSE
      oRespMsg:='Exception Occured : '|| 'Error Code : '||SQLCODE|| ', Error Message : ' || SUBSTR(SQLERRM, 1, 400);
      END IF;
     



END CODELESS_COLLECTION;
/