// ════════════════════════════════════════════════════════════════════
//  SERVICE — AuthenticationService
// ════════════════════════════════════════════════════════════════════

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.io.*;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

class AuthenticationService
{
    private static final int MAX_ATTEMPTS = 3;

    private int    failedAttempts = 0;
    private String sessionToken   = null;
    private boolean locked        = false;

    private final SystemLog sysLog;

    public AuthenticationService(SystemLog log) { this.sysLog = log; }

    public boolean authenticateUser(User user, String plainPassword)
    {
        if (locked)
        {
            sysLog.recordAction(user.getEmail(), "Login blocked — account locked");
            return false;
        }
        if (user.checkPassword(plainPassword))
        {
            failedAttempts = 0;
            sessionToken   = UUID.randomUUID().toString();
            sysLog.recordAction(user.getEmail(), "Successful login");
            return true;
        }
        failedAttempts++;
        sysLog.recordAction(user.getEmail(),
                "Failed login attempt (" + failedAttempts + "/" + MAX_ATTEMPTS + ")");
        if (failedAttempts >= MAX_ATTEMPTS) lockAccount(user.getEmail());
        return false;
    }

    private void lockAccount(String email)
    {
        locked = true;
        sysLog.recordAction(email, "Account locked after " + MAX_ATTEMPTS + " failed attempts");
    }

    public void unlockAccount()        { locked = false; failedAttempts = 0; }
    public boolean validateSession()   { return sessionToken != null; }
    public void    invalidateSession() { sessionToken = null; }
    public boolean isLocked()          { return locked; }
    public int     getFailedAttempts() { return failedAttempts; }
    public int     getMaxAttempts()    { return MAX_ATTEMPTS; }
    public String  hashPassword(String p) { return PasswordUtil.hash(p); }
}
