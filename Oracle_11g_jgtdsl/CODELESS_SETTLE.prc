CREATE OR REPLACE PROCEDURE JALALABAD.CODELESS_SETTLE(
   iCodeID              in number,
   iCustomerId          In VARCHAR2,
   iFrom_month          IN VARCHAR2,
   iFrom_year           IN VARCHAR2,
   iTo_month            IN VARCHAR2,
   iTo_year             IN VARCHAR2,
   iInsertBy            In VARCHAR2, 
   oResponse            OUT  NUMBER,
   oRespMsg             OUT  VARCHAR2
)
is
vSqCode NUMBER;
vCollectedAmount number;
vCollectedSurcharge number;
vCollectionDate varchar2(15);
vBankId varchar2(20);
vBranchId varchar2(20);
vAcountNo varchar2(20);
iFrom_month_year number;
iTo_month_year  number;

iTotalBill number;
cnt number;
iTotalSur number;
iprevBill number;
 iprevSur number;
tRemain_sur number;
vSurCharge number;
vCollectionId number;
tRemain_amount number;
cntCus number;

BEGIN   

      select count(*) into cntCus from customer where CUSTOMER_ID=iCustomerId;
      
     if( cntCus>0) then   


        select COLLECTED_AMOUNT,COLLECTED_SURCHARGE, to_char(COLLECTION_DATE,'dd-mm-rrrr'), 
           BANK_ID, BRANCH_ID, ACCOUNT_NO 
           into vCollectedAmount, vCollectedSurcharge, vCollectionDate,
           vBankId, vBranchId, vAcountNo
           from CODELESS_PAYMENT where CODELESS_NO=iCodeID and status=0;
                   
           
           
           
        iFrom_month_year :=TO_NUMBER(iFrom_year||LPAD (iFrom_month, 2, 0));
        iTo_month_year :=TO_NUMBER(iTo_year||LPAD (iTo_month, 2, 0));
            
        
        
        iTotalBill :=nvl(vCollectedAmount,0);
        iTotalSur  :=nvl(vCollectedSurcharge,0);
    
        select count(*) into cnt from BILL_COLL_ADVANCED WHERE CUSTOMER_ID=trim(iCustomerId) and STATUS=1;
    
        if(cnt>0) then
        
            SELECT NVL(ADVANCED_AMOUNT,0),nvl(ADVANCED_SURCHARGE,0) into iprevBill, iprevSur FROM BILL_COLL_ADVANCED WHERE CUSTOMER_ID=trim(iCustomerId) and STATUS=1;
            
            iTotalBill :=nvl(iprevBill,0)+iTotalBill;      
            iTotalSur := nvl(iprevSur,0)+iTotalSur;
            
            UPDATE BILL_COLL_ADVANCED SET STATUS=2 WHERE CUSTOMER_ID=iCustomerId and STATUS=1;
            
           INSERT INTO BILL_COLL_ADVANCED (ADV_TRANS_ID, TRANS_DATE, CUSTOMER_ID, BANK_ID,BRANCH_ID, ACCOUNT_NO,ADVANCED_AMOUNT,COLLECTED_BY,INSERTED_ON, STATUS, REMARKS,ADVANCED_SURCHARGE)
           VALUES (SQN_ADVANCED_BILL.NEXTVAL,TO_DATE(vCollectionDate, 'DD-MM-YYYY'),iCustomerId,vBankId,vBranchId,vAcountNo,iTotalBill,iInsertBy,sysdate,1,'Advanced',iTotalSur);
         
        else
            
           INSERT INTO BILL_COLL_ADVANCED (ADV_TRANS_ID, TRANS_DATE, CUSTOMER_ID, BANK_ID,BRANCH_ID, ACCOUNT_NO,ADVANCED_AMOUNT,COLLECTED_BY,INSERTED_ON, STATUS, REMARKS,ADVANCED_SURCHARGE)
           VALUES (SQN_ADVANCED_BILL.NEXTVAL,TO_DATE(vCollectionDate, 'DD-MM-YYYY'),iCustomerId,vBankId,vBranchId,vAcountNo,iTotalBill,iInsertBy,sysdate,1,'Advanced',iTotalSur);
             
               
        end if; 
        
        
        
        
           
        FOR rec in (select BILL_ID,BILLED_AMOUNT,CALCUALTESURCHARGE(BILL_ID,vCollectionDate) ACTUAL_SURCHARGE,nvl(COLLECTED_BILLED_AMOUNT,0) COLLECTED_BILLED_AMOUNT from bill_non_metered
                where customer_id=iCustomerId and status=1 AND TO_NUMBER (bill_year || LPAD (bill_month, 2, 0)) BETWEEN iFrom_month_year and iTo_month_year ORDER BY  BILL_ID)
        LOOP
            
            
            SELECT NVL(ADVANCED_AMOUNT,0),nvl(ADVANCED_SURCHARGE,0) into iprevBill, iprevSur FROM BILL_COLL_ADVANCED WHERE CUSTOMER_ID=iCustomerId and STATUS=1;
                 
           tRemain_sur:=iprevSur;
           
            IF(iprevBill>=rec.BILLED_AMOUNT)
                THEN
                
                    if(iprevSur>=rec.ACTUAL_SURCHARGE) then
                        vSurCharge :=rec.ACTUAL_SURCHARGE;
                        tRemain_sur := tRemain_sur-rec.ACTUAL_SURCHARGE;
                    else    
                        vSurCharge :=iprevSur;
                        tRemain_sur :=0;
                    end if;
                
                
                update BILL_NON_METERED set COLLECTED_BILLED_AMOUNT=rec.BILLED_AMOUNT ,
                                            ACTUAL_SURCHARGE=rec.ACTUAL_SURCHARGE,
                                            COLLECTED_SURCHARGE=vSurCharge,
                                            ACTUAL_PAYABLE_AMOUNT=rec.BILLED_AMOUNT+rec.ACTUAL_SURCHARGE,
                                            COLLECTED_PAYABLE_AMOUNT=rec.BILLED_AMOUNT+vSurCharge,
                                            STATUS=2,COLLECTION_DATE=TO_DATE(vCollectionDate, 'DD-MM-YYYY')
               WHERE  BILL_ID=rec.BILL_ID;
               
               
                select SQN_COLLECTION_NM.nextval into vCollectionId from dual;
               
                INSERT INTO BILL_COLLECTION_NON_METERED (
                   COLLECTION_ID, CUSTOMER_ID, BILL_ID, 
                   BANK_ID, BRANCH_ID, ACCOUNT_NO, 
                   COLLECTION_DATE, COLLECTED_BILL_AMOUNT, COLLECTED_SURCHARGE_AMOUNT, 
                   TOTAL_COLLECTED_AMOUNT, REMARKS, COLLECED_BY, 
                   INSERTED_ON, SURCHARGE_PER_COLL) 
                VALUES ( vCollectionId,iCustomerId, rec.BILL_ID,
                 vBankId, vBranchId, vAcountNo,
                 TO_DATE(vCollectionDate, 'DD-MM-YYYY'), rec.BILLED_AMOUNT, nvl(vSurCharge,0),
                 rec.BILLED_AMOUNT+nvl(vSurCharge,0), NULL, iInsertBy,
                 sysdate,  null );                
               
               
               
               tRemain_amount:=iprevBill-rec.BILLED_AMOUNT;
                              
               
                UPDATE BILL_COLL_ADVANCED SET STATUS=2, BILL_ID=rec.BILL_ID, DEDUCT_AMOUNT=rec.BILLED_AMOUNT, DEDUCT_SURCHARGE= nvl(vSurCharge,0)
                WHERE CUSTOMER_ID=iCustomerId and STATUS=1;
               
               
               INSERT INTO BILL_COLL_ADVANCED (ADV_TRANS_ID, TRANS_DATE, CUSTOMER_ID, BANK_ID,BRANCH_ID, ACCOUNT_NO,ADVANCED_AMOUNT,COLLECTED_BY,INSERTED_ON, STATUS, REMARKS,ADVANCED_SURCHARGE)
               VALUES (SQN_ADVANCED_BILL.NEXTVAL,TO_DATE(vCollectionDate, 'DD-MM-YYYY'),iCustomerId,vBankId,vBranchId,vAcountNo,tRemain_amount,iInsertBy,sysdate,1,'Advanced',tRemain_sur);
            
                     
                               
            END IF;                       
        
        end loop;
        
        update CODELESS_PAYMENT set status=1, CUSTOMER_ID=iCustomerId  where CODELESS_NO=iCodeID;
       
    
        update BANK_ACCOUNT_LEDGER set CUSTOMER_ID=iCustomerId where REF_ID='C-'||iCodeID;
    
    
        oResponse:=1;
        oRespMsg:='Successfully Saved Code Less Settle';
        COMMIT; 
     else
        oResponse:=1;
        oRespMsg:='Customer is Invalid';

    end if;
    
    
    
    
    EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK;
      oResponse:=500;
      IF SQLCODE=-1 THEN
      oRespMsg:='Od';
      ELSE
      oRespMsg:='Exception Occured : '|| 'Error Code : '||SQLCODE|| ', Error Message : ' || SUBSTR(SQLERRM, 1, 400);
      END IF;
     



END CODELESS_SETTLE;
/
