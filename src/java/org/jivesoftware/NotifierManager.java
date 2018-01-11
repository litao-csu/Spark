package org.jivesoftware;

import java.util.Hashtable;

public class NotifierManager {
	private Hashtable<String, AuthorizationNotifier> notifiers = null;
	private static NotifierManager nm = null;
	
	private NotifierManager() {
		
		notifiers = new Hashtable<String, AuthorizationNotifier>();
	}
	
	public static NotifierManager getInstance(){
		
		synchronized(NotifierManager.class){
			
			if(nm==null){
				nm = new NotifierManager();
			}
		}
		
		return nm;
	}
	
	public void addNotify(String name, AuthorizationNotifier tan){
		
		notifiers.put(name, tan);
	}
	
	public void notifyIT(String name){
		
		AuthorizationNotifier tan = (AuthorizationNotifier)notifiers.get(name);
		tan.notifyMe();
	}
	
	public void notifyITSth(String name, String msg){
		
		AuthorizationNotifier tan = (AuthorizationNotifier)notifiers.get(name);
		tan.notifyMeSth(msg);
	}
}
