/**
 * $RCSfile: ,v $
 * $Revision: $
 * $Date: $
 * 
 * Copyright (C) 2004-2011 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.sparkimpl.plugin.systray;

import java.awt.MouseInfo;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.commons.collections.functors.WhileClosure;
import org.jivesoftware.Spark;
import org.jivesoftware.resource.Default;
import org.jivesoftware.resource.Res;
import org.jivesoftware.resource.SparkRes;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.spark.ChatManager;
import org.jivesoftware.spark.NativeHandler;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.Workspace;
import org.jivesoftware.spark.plugin.Plugin;
import org.jivesoftware.spark.ui.ChatFrame;
import org.jivesoftware.spark.ui.status.CustomStatusItem;
import org.jivesoftware.spark.ui.status.StatusBar;
import org.jivesoftware.spark.ui.status.StatusItem;
import org.jivesoftware.spark.util.log.Log;
import org.jivesoftware.sparkimpl.settings.local.LocalPreferences;
import org.jivesoftware.sparkimpl.settings.local.SettingsManager;

import com.sun.javafx.tk.Toolkit;

import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smackx.chatstates.ChatStateListener;


public class SysTrayPlugin implements Plugin, NativeHandler, ChatManagerListener, ChatStateListener {
	private JPopupMenu popupMenu = new JPopupMenu();

	private JMenu statusMenu;

	private LocalPreferences pref = SettingsManager.getLocalPreferences();
    private ImageIcon availableIcon;
    private ImageIcon dndIcon;
    private ImageIcon awayIcon;
    private ImageIcon offlineIcon;
    private ImageIcon connectingIcon;
    private ImageIcon newMessageIcon;
    private ImageIcon typingIcon;
    private TrayIcon trayIcon;
    private boolean newMessage = false;
	private Presence presence;
	private ChatFrame chatFrame;
	public static boolean flag;
	
    ChatMessageHandlerImpl chatMessageHandler = new ChatMessageHandlerImpl();

    @Override
    public boolean canShutDown() {
    	return true;
    }

    @Override
    public void initialize() {

	if (SystemTray.isSupported()) {

		JMenuItem openMenu = new JMenuItem( Res.getString( "menuitem.open" ) );
		JMenuItem minimizeMenu = new JMenuItem( Res.getString( "menuitem.hide" ) );
		JMenuItem exitMenu = new JMenuItem( Res.getString( "menuitem.exit" ) );
	    statusMenu = new JMenu(Res.getString("menuitem.status"));
		JMenuItem logoutMenu = new JMenuItem(
				Res.getString( "menuitem.logout.no.status" ) );

	    SystemTray tray = SystemTray.getSystemTray();
	    SparkManager.getNativeManager().addNativeHandler(this);
	    ChatManager.getInstance().addChatMessageHandler(chatMessageHandler);
	    //XEP-0085 suport (replaces the obsolete XEP-0022)
		org.jivesoftware.smack.chat.ChatManager.getInstanceFor( SparkManager.getConnection() ).addChatListener(this);

	    if (Spark.isLinux()) {
		newMessageIcon = SparkRes
			.getImageIcon(SparkRes.MESSAGE_NEW_TRAY_LINUX);
		typingIcon = SparkRes.getImageIcon(SparkRes.TYPING_TRAY_LINUX);
	    } else {
		newMessageIcon = SparkRes
			.getImageIcon(SparkRes.MESSAGE_NEW_TRAY);
		typingIcon = SparkRes.getImageIcon(SparkRes.TYPING_TRAY);
	    }

	    availableIcon = Default.getImageIcon(Default.TRAY_IMAGE);
	    if (Spark.isLinux()) {
		if (availableIcon == null) {
		    availableIcon = SparkRes
			    .getImageIcon(SparkRes.TRAY_IMAGE_LINUX);
		    Log.error(availableIcon.toString());
		}
		awayIcon = SparkRes.getImageIcon(SparkRes.TRAY_AWAY_LINUX);
		dndIcon = SparkRes.getImageIcon(SparkRes.TRAY_DND_LINUX);
		offlineIcon = SparkRes
			.getImageIcon(SparkRes.TRAY_OFFLINE_LINUX);
		connectingIcon = SparkRes
			.getImageIcon(SparkRes.TRAY_CONNECTING_LINUX);
	    } else {
		if (availableIcon == null) {
		    availableIcon = SparkRes.getImageIcon(SparkRes.TRAY_IMAGE);
		}
		awayIcon = SparkRes.getImageIcon(SparkRes.TRAY_AWAY);
		dndIcon = SparkRes.getImageIcon(SparkRes.TRAY_DND);
		offlineIcon = SparkRes.getImageIcon(SparkRes.TRAY_OFFLINE);
		connectingIcon = SparkRes
			.getImageIcon(SparkRes.TRAY_CONNECTING);
	    }
	    
	    popupMenu.add( openMenu );
	    
	    openMenu.addActionListener( new AbstractAction() {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent event) {
		    SparkManager.getMainWindow().setVisible(true);
		    SparkManager.getMainWindow().toFront();
		}

	    });
	    
	    popupMenu.add( minimizeMenu );
	    
	    minimizeMenu.addActionListener( new AbstractAction() {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent event) {
		    SparkManager.getMainWindow().setVisible(false);
		}
	    });
	    
	    // See if we should disable ability to change presence status	    
	    if (!Default.getBoolean("DISABLE_PRESENCE_STATUS_CHANGE")) {	    
	    //	popupMenu.addSeparator();
	    	addStatusMessages();
	    //	popupMenu.add(statusMenu);
	    }
	    
	    statusMenu.addActionListener(new AbstractAction() {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent event) {

		}
	    });

	    if (Spark.isWindows()) {
//		if (!Default.getBoolean("DISABLE_EXIT"))
//		    popupMenu.add( logoutMenu );

		logoutMenu.addActionListener( new AbstractAction() {
		    private static final long serialVersionUID = 1L;

		    @Override
		    public void actionPerformed(ActionEvent e) {
			SparkManager.getMainWindow().logout(false);
		    }
		});
	    }
	    // Exit Menu
	    exitMenu.addActionListener( new AbstractAction() {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			/**
			 * @author litao
			 * 在退出Spark时，同时退出boinc
			 */
			try{
				String cmd1 = "cmd /k taskkill /f /im boinc.exe ";
				String cmd2 = "cmd /k taskkill /f /im start.exe ";
				Runtime.getRuntime().exec(cmd1);
				Runtime.getRuntime().exec(cmd2);
				System.out.println(cmd1);
				System.out.println(cmd2);
			}
			catch (IOException e1) {  
	            e1.printStackTrace();  
	        }
			
		    SparkManager.getMainWindow().shutdown();
		}
	    });
	    if (!Default.getBoolean("DISABLE_EXIT"))
		popupMenu.add( exitMenu );

	    /**
	     * If connection closed set offline tray image
	     */
	    SparkManager.getConnection().addConnectionListener(
		    new ConnectionListener() {

			@Override
			public void connected( XMPPConnection xmppConnection ) {
				trayIcon.setImage( availableIcon.getImage() );
			}

			@Override
			public void authenticated( XMPPConnection xmppConnection, boolean b ) {
				trayIcon.setImage( availableIcon.getImage() );
			}

			@Override
			public void connectionClosed() {
			    trayIcon.setImage(offlineIcon.getImage());
			}

			@Override
			public void connectionClosedOnError(Exception arg0) {
			    trayIcon.setImage(offlineIcon.getImage());
			}

			@Override
			public void reconnectingIn(int arg0) {
			    trayIcon.setImage(connectingIcon.getImage());
			}

			@Override
			public void reconnectionSuccessful() {
			    trayIcon.setImage(availableIcon.getImage());
			}

			@Override
			public void reconnectionFailed(Exception arg0) {
			    trayIcon.setImage(offlineIcon.getImage());
			}
		    });

	    SparkManager.getSessionManager().addPresenceListener(
				presence -> {
                    if (presence.getMode() == Presence.Mode.available) {
                    trayIcon.setImage(availableIcon.getImage());
                    } else if (presence.getMode() == Presence.Mode.away
                        || presence.getMode() == Presence.Mode.xa) {
                    trayIcon.setImage(awayIcon.getImage());
                    } else if (presence.getMode() == Presence.Mode.dnd) {
                    trayIcon.setImage(dndIcon.getImage());
                    } else {
                    trayIcon.setImage(availableIcon.getImage());
                    }
                } );

	    try {
	
		
		
		trayIcon = new TrayIcon(availableIcon.getImage(),
			Default.getString(Default.APPLICATION_NAME), null);
		trayIcon.setImageAutoSize(true);

		trayIcon.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent event) {
				// if we are using double click on tray icon
				if(flag){
					if (   (!pref.isUsingSingleTrayClick()
							&& event.getButton() == MouseEvent.BUTTON1
							&& event.getClickCount() % 2 == 0) 
							||
							// if we using single click on tray icon
							(pref.isUsingSingleTrayClick()
							&& event.getButton() == MouseEvent.BUTTON1
							&& event.getClickCount() == 1)) {
						presence = Workspace.getInstance().getStatusBar().getPresence();
						if (presence.getMode() == Presence.Mode.available) {
							trayIcon.setImage(availableIcon.getImage());
						} else if (presence.getMode() == Presence.Mode.away
								|| presence.getMode() == Presence.Mode.xa) {
							trayIcon.setImage(awayIcon.getImage());
						} else if (presence.getMode() == Presence.Mode.dnd) {
							trayIcon.setImage(dndIcon.getImage());
						} else {
							trayIcon.setImage(availableIcon.getImage());
						}
				    	newMessage = false;
				    	chatMessageHandler.clearUnreadMessages();
				    	flag = false;
				    	SparkManager.getNativeManager().stopFlashing(SparkManager.getChatManager()
				        	    .getChatContainer().getChatFrame());
				    	if ((SparkManager.getMainWindow().isVisible())
								&& (SparkManager.getMainWindow().getState() == java.awt.Frame.NORMAL)) {
								SparkManager.getMainWindow().setVisible(false);
							} else {
								SparkManager.getMainWindow().setVisible(true);
								SparkManager.getMainWindow().setState(java.awt.Frame.NORMAL);
								SparkManager.getMainWindow().toFront();
							}		
				    	
					}else if (event.getButton() == MouseEvent.BUTTON1) {
						presence = Workspace.getInstance().getStatusBar().getPresence();
						if (presence.getMode() == Presence.Mode.available) {
							trayIcon.setImage(availableIcon.getImage());
						} else if (presence.getMode() == Presence.Mode.away
								|| presence.getMode() == Presence.Mode.xa) {
							trayIcon.setImage(awayIcon.getImage());
						} else if (presence.getMode() == Presence.Mode.dnd) {
							trayIcon.setImage(dndIcon.getImage());
						} else {
							trayIcon.setImage(availableIcon.getImage());
						}
				    	newMessage = false;
				    	chatMessageHandler.clearUnreadMessages();
				    	flag = false;
				    	SparkManager.getNativeManager().stopFlashing(SparkManager.getChatManager()
				        	    .getChatContainer().getChatFrame());
				    	SparkManager.getMainWindow().toFront();
					}else{
						flag = false;
						SparkManager.getNativeManager().stopFlashing(SparkManager.getChatManager()
				        	    .getChatContainer().getChatFrame());
						if (popupMenu.isVisible()) {
							popupMenu.setVisible(false);
						} else {

							double x = MouseInfo.getPointerInfo()
									.getLocation().getX();
							double y = MouseInfo.getPointerInfo()
									.getLocation().getY();

							if (Spark.isMac()) {
								popupMenu.setLocation((int) x, (int) y);
							} else {
								popupMenu.setLocation(event.getX(),
										event.getY());
							}

							popupMenu.setInvoker(popupMenu);
							popupMenu.setVisible(true);
						}
					}
				}else{
					if (   (!pref.isUsingSingleTrayClick()
							&& event.getButton() == MouseEvent.BUTTON1
							&& event.getClickCount() % 2 == 0) 
							||
							// if we using single click on tray icon
							(pref.isUsingSingleTrayClick()
							&& event.getButton() == MouseEvent.BUTTON1
							&& event.getClickCount() == 1)) {

						// bring the mainwindow to front
						if ((SparkManager.getMainWindow().isVisible())
							&& (SparkManager.getMainWindow().getState() == java.awt.Frame.NORMAL)) {
							SparkManager.getMainWindow().setVisible(false);
						} else {
							SparkManager.getMainWindow().setVisible(true);
							SparkManager.getMainWindow().setState(java.awt.Frame.NORMAL);
							SparkManager.getMainWindow().toFront();
						}		
						
					} else if (event.getButton() == MouseEvent.BUTTON1) {
						SparkManager.getMainWindow().toFront();
						// SparkManager.getMainWindow().requestFocus();
					} else if (event.getButton() == MouseEvent.BUTTON3) {

						if (popupMenu.isVisible()) {
							popupMenu.setVisible(false);
						} else {

							double x = MouseInfo.getPointerInfo()
									.getLocation().getX();
							double y = MouseInfo.getPointerInfo()
									.getLocation().getY();

							if (Spark.isMac()) {
								popupMenu.setLocation((int) x, (int) y);
							} else {
								popupMenu.setLocation(event.getX(),
										event.getY());
							}

							popupMenu.setInvoker(popupMenu);
							popupMenu.setVisible(true);
						}
					}
				}
				
			}

		    @Override
		    public void mouseEntered(MouseEvent event) {

		    }

		    @Override
		    public void mouseExited(MouseEvent event) {

		    }

		    @Override
		    public void mousePressed(MouseEvent event) {

			// on Mac i would want the window to show when i left-click the Icon
			if (Spark.isMac() && event.getButton()!=MouseEvent.BUTTON3) {
			    SparkManager.getMainWindow().setVisible(false);
			    SparkManager.getMainWindow().setVisible(true);
			    SparkManager.getMainWindow().requestFocusInWindow();
			    SparkManager.getMainWindow().bringFrameIntoFocus();
			    SparkManager.getMainWindow().toFront();
			    SparkManager.getMainWindow().requestFocus();
			}
		    }

		    @Override
		    public void mouseReleased(MouseEvent event) {

		    }

		});

		tray.add(trayIcon);
	    } catch (Exception e) {
		// Not Supported
	    }
	} else {
	    Log.error("Tray don't supports on this platform.");
	}
    }

    public void addStatusMessages() {
	StatusBar statusBar = SparkManager.getWorkspace().getStatusBar();
	for (Object o : statusBar.getStatusList()) {
	    final StatusItem statusItem = (StatusItem) o;

	    final AbstractAction action = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {

		    StatusBar statusBar = SparkManager.getWorkspace()
			    .getStatusBar();

		    SparkManager.getSessionManager().changePresence(
			    statusItem.getPresence());
		    statusBar.setStatus(statusItem.getText());
		}
	    };
	    action.putValue(Action.NAME, statusItem.getText());
	    action.putValue(Action.SMALL_ICON, statusItem.getIcon());

	    boolean hasChildren = false;
	    for (Object aCustom : SparkManager.getWorkspace().getStatusBar()
		    .getCustomStatusList()) {
		final CustomStatusItem cItem = (CustomStatusItem) aCustom;
		String type = cItem.getType();
		if (type.equals(statusItem.getText())) {
		    hasChildren = true;
		}
	    }

	    if (!hasChildren) {
		JMenuItem status = new JMenuItem(action);
		statusMenu.add(status);
	    } else {
		final JMenu status = new JMenu(action);
		statusMenu.add(status);

		status.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent mouseEvent) {
			action.actionPerformed(null);
			popupMenu.setVisible(false);
		    }
		});

		for (Object aCustom : SparkManager.getWorkspace()
			.getStatusBar().getCustomStatusList()) {
		    final CustomStatusItem customItem = (CustomStatusItem) aCustom;
		    String type = customItem.getType();
		    if (type.equals(statusItem.getText())) {
			AbstractAction customAction = new AbstractAction() {
			    private static final long serialVersionUID = 1L;

			    @Override
			    public void actionPerformed(ActionEvent e) {
				StatusBar statusBar = SparkManager
					.getWorkspace().getStatusBar();

				Presence oldPresence = statusItem.getPresence();
				Presence presence = StatusBar
					.copyPresence(oldPresence);
				presence.setStatus(customItem.getStatus());
				presence.setPriority(customItem.getPriority());
				SparkManager.getSessionManager()
					.changePresence(presence);

				statusBar.setStatus(statusItem.getName()
					+ " - " + customItem.getStatus());
			    }
			};
			customAction.putValue(Action.NAME,
				customItem.getStatus());
			customAction.putValue(Action.SMALL_ICON,
				statusItem.getIcon());
			JMenuItem menuItem = new JMenuItem(customAction);
			status.add(menuItem);
		    }
		}

	    }
	}
    }

    @Override
    public void shutdown() {
    	if (SystemTray.isSupported()) {
    		SystemTray tray = SystemTray.getSystemTray();
    		tray.remove(trayIcon);
    	}
    	ChatManager.getInstance().removeChatMessageHandler(chatMessageHandler);
    }

    @Override
    public void uninstall() {
    	ChatManager.getInstance().removeChatMessageHandler(chatMessageHandler);
    }

    // Info on new Messages
    @Override
    public void flashWindow(Window window)  {
    	if (pref.isSystemTrayNotificationEnabled()) {
    		trayIcon.setImage(newMessageIcon.getImage());
			if (window instanceof JFrame) {
				((JFrame) window).setTitle(getCounteredTitle(
						((JFrame) window).getTitle(), chatMessageHandler.getUnreadMessages()));
			}
			
//			trayIcon.setImage(new ImageIcon("").getImage());  
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			trayIcon.setImage(newMessageIcon.getImage());
			newMessage = true;
    	}
    	flag = true;
    	/*
         * author zhang
         */
		new Thread() {
		    @Override
		    public void run() {
//		    	System.out.println("执行了");
		    	while(true){
		    		if(flag){
		    			try{
			    			trayIcon.setImage(new ImageIcon("").getImage());  
							Thread.sleep(500);
							trayIcon.setImage(availableIcon.getImage());
							Thread.sleep(500);
		    			}catch(Exception e){
		    				e.printStackTrace();
		    			}
		    		}else{
//		    			System.out.println("结束了");
		    			trayIcon.setImage(availableIcon.getImage());
		    			break;
		    		}
		    	}
		    }
		}.start();
    }

    
    
    
	private String getCounteredTitle(String title, int counter) {
		String stringCounter = String.format("[%s] ", counter);
		String MESSAGE_COUNTER_REG_EXP = "\\[\\d+\\] ";
		return counter > 0 ? stringCounter + title.replaceFirst( MESSAGE_COUNTER_REG_EXP, "") : title.replaceFirst( MESSAGE_COUNTER_REG_EXP, "");
	}  
    
    @Override
    public void flashWindowStopWhenFocused(Window window) {
		presence = Workspace.getInstance().getStatusBar().getPresence();
		if (presence.getMode() == Presence.Mode.available) {
			trayIcon.setImage(availableIcon.getImage());
		} else if (presence.getMode() == Presence.Mode.away
				|| presence.getMode() == Presence.Mode.xa) {
			trayIcon.setImage(awayIcon.getImage());
		} else if (presence.getMode() == Presence.Mode.dnd) {
			trayIcon.setImage(dndIcon.getImage());
		} else {
			trayIcon.setImage(availableIcon.getImage());
		}
    	newMessage = false;
    	chatMessageHandler.clearUnreadMessages();
    	flag = false;
    	String className = Thread.currentThread().getStackTrace()[4].getClassName();
        String methodName = Thread.currentThread().getStackTrace()[4].getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[4].getLineNumber();
        
        System.out.println(className);
        System.out.println(methodName);
        System.out.println(lineNumber);
       
	}

    @Override
    public boolean handleNotification() {
    	return true;
    }

    @Override
    public void stopFlashing(Window window) {
		presence = Workspace.getInstance().getStatusBar().getPresence();
		if (presence.getMode() == Presence.Mode.available) {
			trayIcon.setImage(availableIcon.getImage());
		} else if (presence.getMode() == Presence.Mode.away
				|| presence.getMode() == Presence.Mode.xa) {
			trayIcon.setImage(awayIcon.getImage());
		} else if (presence.getMode() == Presence.Mode.dnd) {
			trayIcon.setImage(dndIcon.getImage());
		} else {
			trayIcon.setImage(availableIcon.getImage());
		}
    	newMessage = false;
    	chatMessageHandler.clearUnreadMessages();
    	flag = false;
//    	String className = Thread.currentThread().getStackTrace()[4].getClassName();
//        String methodName = Thread.currentThread().getStackTrace()[4].getMethodName();
//        int lineNumber = Thread.currentThread().getStackTrace()[4].getLineNumber();
//        
//        System.out.println(className);
//        System.out.println(methodName);
//        System.out.println(lineNumber);
    }

    // For Typing
	@Override
	public void processMessage(Chat arg0, Message arg1) {
		// Do nothing - stateChanged is in charge
		
	}

	@Override
	public void stateChanged(Chat chat, ChatState state) {
		presence = Workspace.getInstance().getStatusBar().getPresence();
		if (ChatState.composing.equals(state)) {
			changeSysTrayIcon();
		} else {
			if (!newMessage) {
				if (presence.getMode() == Presence.Mode.available) {
					trayIcon.setImage(availableIcon.getImage());
				} else if (presence.getMode() == Presence.Mode.away
						|| presence.getMode() == Presence.Mode.xa) {
					trayIcon.setImage(awayIcon.getImage());
				} else if (presence.getMode() == Presence.Mode.dnd) {
					trayIcon.setImage(dndIcon.getImage());
				} else {
					trayIcon.setImage(newMessageIcon.getImage());
				}
			}
		}
	}

	@Override
	public void chatCreated(Chat chat, boolean isLocal) {
		chat.addMessageListener(this);		
	}
	
	private void changeSysTrayIcon() {
    	if (pref.isTypingNotificationShown()) {
    		trayIcon.setImage(typingIcon.getImage());
    	}
	}

}
