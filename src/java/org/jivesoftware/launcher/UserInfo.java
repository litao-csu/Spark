package org.jivesoftware.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class UserInfo {

	public String userID = null;
	public String userPWD = null;
	public String userServer = null;
	public String userMAC = null;
	public String userName = null;
	public String userLab = null;
	public int user_status = 0;
	private static UserInfo ui=null;
	
//	private static final String serverIP = "202.197.61.231";
//	private static final String openfirePort = "25222";
	private static String serverIP ;//= "192.168.1.164";
	private static  String openfirePort ;//= "5222";
//	private static final String serverPort;// = "123456";
	public static String IP_ADDR ;//= "192.168.1.164";
	public static int PORT ;//= 12345;

	/**
	 * @author litao
	 * 从外部文件读取服务器的IP和端口
	 */
	public static  void readServerIP(){
		try {
            // read file content from file
            StringBuffer sb= new StringBuffer("");
            FileReader reader = new FileReader("./readServerInformation/readServerInformation.ini");
            BufferedReader br = new BufferedReader(reader);
           
            String str = null;
            String[] data = new String[4];
            int i = 0;
           
            while((str = br.readLine()) != null) {
                  sb.append(str+"/n");
                  data[i++] = str;
            }
            
            IP_ADDR = data[0];
            serverIP = data[1];
            PORT = Integer.parseInt(data[2]);
            openfirePort = data[3];         
            br.close();
            reader.close();
		}
		catch(FileNotFoundException e) {
            e.printStackTrace();
        }
		catch(IOException e) {
            e.printStackTrace();
		}
			
	}
	/**
	 * @author litao
	 * 在类加载到内存时就执行该函数，获取服务器的IP和端口
	*/ 
	static{
		readServerIP();
	}
	
	public void setUI(String id, String pwd){
		
		ui.userID = id;
		ui.userPWD = pwd;
	}
	
	public void setUI(String id, String pwd, String mac){
		
		ui.userID = id;
		ui.userPWD = pwd;
		ui.userMAC = mac;
	}
	
	public void setUI(String id, String pwd, String server, String mac){
		
		ui.userID = id;
		ui.userPWD = pwd;
		ui.userServer = server;
		ui.userMAC = mac;
	}

	public void setUI(String id, String pwd, String server, String mac, String name, String lab){
		
		ui.userID = id;
		ui.userPWD = pwd;
		ui.userServer = server;
		ui.userMAC = mac;
		ui.userName = name;
		ui.userLab = lab;
	}
	
	private UserInfo(){
		super(); 
	}
	
	public static UserInfo getInstance(){
		
		synchronized(UserInfo.class){
			if(ui == null){
				
				ui = new UserInfo();
			}
		}
		
		return ui;
	}
	
	
	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}	

	public String getUserPWD() {
		return userPWD;
	}

	public void setUserPWD(String userPWD) {
		this.userPWD = userPWD;
	}

	public String getOpenFireServer(){
		
		//return serverIP+":"+openfirePort;
		return serverIP;
	}
	public static String get_IP_ADDR(){
		return IP_ADDR;
	}
	public static int get_PORT(){
		return PORT;
	}
/**
	public static void main(String[] args){
		System.out.println(getOpenFireServer() +" " +  get_IP_ADDR() + " " + get_PORT());
	}
*/	
}
