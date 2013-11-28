package lsfusion.server.logics;

import lsfusion.server.classes.AbstractCustomClass;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CurrentComputerFormulaProperty;
import lsfusion.server.logics.property.CurrentUserFormulaProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;


public class AuthenticationLogicsModule extends ScriptingLogicsModule{
    BusinessLogics BL;

    public ConcreteCustomClass computer;
    public AbstractCustomClass user;
    public ConcreteCustomClass systemUser;
    public ConcreteCustomClass customUser;

    public LCP isLockedCustomUser;
    public LCP loginCustomUser;
    public LCP customUserLogin;
    public LCP sha256PasswordCustomUser;
    public LCP calculatedHash;
    public LCP currentUser;
    public LCP currentUserName;

    public LCP hostnameComputer;
    public LCP scannerComPortComputer;
    public LCP scannerSingleReadComputer;
    public LCP scannerBytesCountComputer;
    
    public LCP currentComputer;
    public LCP hostnameCurrentComputer;

    public LCP useLDAP;
    public LCP serverLDAP;
    public LCP portLDAP;

    public LAP generateLoginPassword;

    public AuthenticationLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(AuthenticationLogicsModule.class.getResourceAsStream("/scripts/system/Authentication.lsf"), baseLM, BL);
        this.BL = BL;
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        computer = (ConcreteCustomClass) getClassByName("Computer");
        user = (AbstractCustomClass) getClassByName("User");
        systemUser = (ConcreteCustomClass) getClassByName("SystemUser");
        customUser = (ConcreteCustomClass) getClassByName("CustomUser");
    }

    @Override
    public void initProperties() throws RecognitionException {
        // Текущий пользователь
        currentUser = addProperty(null, new LCP<PropertyInterface>(new CurrentUserFormulaProperty("currentUser", user)));
        currentComputer = addProperty(null, new LCP<PropertyInterface>(new CurrentComputerFormulaProperty("currentComputer", computer)));

        super.initProperties();

        currentUserName = getLCPByName("currentUserName");

        // Компьютер
        hostnameComputer = getLCPByName("hostnameComputer");
        scannerComPortComputer = getLCPByName("scannerComPortComputer");
        scannerSingleReadComputer = getLCPByName("scannerSingleReadComputer");
        scannerBytesCountComputer = getLCPByName("scannerBytesCountComputer");

        hostnameCurrentComputer = getLCPByName("hostnameCurrentComputer");

        isLockedCustomUser = getLCPByName("isLockedCustomUser");

        loginCustomUser = getLCPByName("loginCustomUser");
        customUserLogin = getLCPByName("customUserLogin");

        sha256PasswordCustomUser = getLCPByName("sha256PasswordCustomUser");
        sha256PasswordCustomUser.setEchoSymbols(true);

        calculatedHash = getLCPByName("calculatedHash");

        useLDAP = getLCPByName("useLDAP");
        serverLDAP = getLCPByName("serverLDAP");
        portLDAP =  getLCPByName("portLDAP");

        generateLoginPassword = getLAPByName("generateLoginPassword");


    }
}
