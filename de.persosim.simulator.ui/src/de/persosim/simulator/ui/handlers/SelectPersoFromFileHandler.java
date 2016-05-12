package de.persosim.simulator.ui.handlers;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.globaltester.logging.BasicLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import de.persosim.simulator.ui.Activator;

/**
 * This class implements the handler for the select personalization from file menu entry.
 * 
 * @author slutters
 *
 */
public class SelectPersoFromFileHandler extends SelectPersoHandler {

	@Execute
	public void execute(Shell shell){
		FileDialog dialog = new FileDialog(shell);
		dialog.open();
		
		String fileName = dialog.getFileName();
		
		if(fileName.length() > 0) {
			String pathName = dialog.getFilterPath() + "/" + fileName;
			
			loadPersonalization(pathName);
		}
	}

	public SelectPersoFromFileHandler()
	{

	    final Bundle bundle = FrameworkUtil.getBundle(SelectPersoFromFileHandler.class);
	    if (bundle == null)
	    {
			BasicLogger.log(getClass(), "Cannot find bundle");
	
	    	return;
	    }
	
	    final BundleContext ctx = Activator.getContext();
	    if (ctx == null)
	    {
			BasicLogger.log(getClass(), "Cannot find context");
	
	    	return;
	    }

	    EventHandler handler = new EventHandler() {
	    	public void handleEvent(final Event event) {
	    		final Object val = event.getProperty("PERSONALIZATION_FILE_PATH");
	    		if (val == null || !(val instanceof String))
	    		{
	    			return;
	    		}

	    		final String filePath = (String) val;
	    		final Display display = Display.getDefault();
	    		if (display == null)
	    		{
	    			loadPersonalization(filePath);

	    			return;
	    		}

	    		if (display.getThread() == Thread.currentThread())
	    		{
	    			loadPersonalization(filePath);
	    		}
	    		else
	    		{
	    			display.syncExec(new Runnable() {
	    				public void run() {
	    					loadPersonalization(filePath);
	    				}
	    			});
	    		}
	    	}
	    };

	    final Dictionary<String, String> properties = new Hashtable<String, String>();
	    properties.put(EventConstants.EVENT_TOPIC, "repl_request/select-perso");
	    ctx.registerService(EventHandler.class, handler, properties);
	}
}
