package com.tunestore.action;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.DynaActionForm;

import com.tunestore.util.IWithDataSource;

public class LoginAction extends Action implements IWithDataSource {
  private DataSource dataSource;
  
  private static Log log = LogFactory.getLog(LoginAction.class);
  
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }
  
  public ActionForward execute(ActionMapping mapping, ActionForm form,
      HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    DynaActionForm df = (DynaActionForm)form;
    String login = (String)df.get("username");
    String password = (String)df.get("password");
    Boolean stayLogged = df.get("stayLogged") == null ? new Boolean(false) : (Boolean)df.get("stayLogged");
    ActionMessages errors = getErrors(request);
    ActionMessages messages = getMessages(request);
    
    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      String sql = "SELECT USERNAME, PASSWORD, BALANCE FROM TUNEUSER"
        + " WHERE TUNEUSER.USERNAME = '"
        + login
        + "' AND PASSWORD = '"
        + password
        + "'";
      Statement stmt = conn.createStatement();
      stmt.setMaxRows(1);
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("login.successful"));
        request.getSession(true).setAttribute("USERNAME", rs.getString("USERNAME"));
        request.getSession(true).setAttribute("BALANCE", rs.getString("BALANCE"));
        request.setAttribute("msg", "Logged in successfully");
        
        // See if they wanted to stay logged in 
        if (stayLogged.booleanValue()) {
          log.info("User requesting to stay logged in");
          String chooseFrom = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ/+=`~!@#$%^&*()-_{}[]|";
          StringBuffer token = new StringBuffer(50);
          Random rnd = new Random();
          for (int i = 0; i < 50; i++) {
            token.append(chooseFrom.charAt(rnd.nextInt(chooseFrom.length())));
          }
          sql = "INSERT INTO PERSISTENTLOGIN (TOKEN,TUNEUSER) VALUES ('"
            + token.toString()
            + "','"
            + request.getSession(true).getAttribute("USERNAME")
            + "')";
          log.info(sql);
          stmt.executeUpdate(sql);
          
          // Set the cookie
          Cookie logincookie = new Cookie("persistenttoken",token.toString());
          logincookie.setMaxAge(60*60*24*365);
          response.addCookie(logincookie);
        }
      } else {
        errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("login.error", login));
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    } finally {
      if (conn != null) {
        try {conn.close();} catch (Exception e) {}
      }
    }
    
    saveMessages(request, messages);
    saveErrors(request, errors);
    return mapping.findForward("index");
  }
}
