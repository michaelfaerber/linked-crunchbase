package servlets;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import utilities.MappingUtility;

/**
 * Application Lifecycle Listener implementation class Listener
 *
 */
@WebListener
public class Listener implements ServletContextListener {
	public static String MAPPING = "mapping";
	/**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent arg0)  { 
         // TODO Auto-generated method stub
    }

	/**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent event)  { 
         ServletContext ctx = event.getServletContext();
         MappingUtility mu = new MappingUtility(ctx.getResourceAsStream("/organization-mappings.nt"), ctx.getResourceAsStream("/people-mappings.nt"));
         ctx.setAttribute(MAPPING, mu);
    }
	
}
