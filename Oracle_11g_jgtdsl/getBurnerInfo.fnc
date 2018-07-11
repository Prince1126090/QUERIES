CREATE OR REPLACE FUNCTION JALALABAD.getBurner(iCustomer_id  varchar2)
   return varchar2
as  
  
   vBurner varchar2(100);
  
    
    vSing  number;
    vDouble number;
   
BEGIN
    
    begin
        select NEW_APPLIANCE_QNT into vSing
        from BURNER_QNT_CHANGE where PID IN(select max(PID)
        from BURNER_QNT_CHANGE where customer_id=iCustomer_id                                     
        and APPLIANCE_TYPE_CODE='01');
    
    EXCEPTION
    WHEN NO_DATA_FOUND THEN
    vSing:=0;
    end;
    
    begin    
        select NEW_APPLIANCE_QNT into vDouble
        from BURNER_QNT_CHANGE where PID IN(select max(PID)
        from BURNER_QNT_CHANGE where customer_id=iCustomer_id                                     
        and APPLIANCE_TYPE_CODE='02');
    
    EXCEPTION
    WHEN NO_DATA_FOUND THEN
    vDouble:=0;
    end;
   
    
    vBurner:=vSing||'#'||vDouble;

    RETURN vBurner;
    
EXCEPTION
  WHEN OTHERS
  THEN
    RETURN '0#0';    
END;
/