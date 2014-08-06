package de.persosim.simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.security.Security;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.persosim.simulator.jaxb.PersoSimJaxbContextProvider;
import de.persosim.simulator.perso.DefaultPersoTestPki;
import de.persosim.simulator.perso.Personalization;

public class PersoSim implements Runnable {

	private SocketSimulator simulator;
	
	/*
	 * This variable holds the currently used personalization.
	 * It may explicitly be null and must not be read directly from here.
	 * As there exist several ways of providing a personalization of which none at all may be used the variable may remain null/unset.
	 * Due to this possibility access to this variable must be performed by calling the getPersonalization() method. 
	 */
	private Personalization currentPersonalization;
	
	public static final String CMD_START                      = "start";
	public static final String CMD_RESTART                    = "restart";
	public static final String CMD_STOP                       = "stop";
	public static final String CMD_EXIT                       = "exit";
	public static final String CMD_LOAD_PERSONALIZATION       = "loadPerso";
	public static final String CMD_LOAD_PERSONALIZATION_SHORT = "-p";
	public static final String CMD_SEND_APDU                  = "sendapdu";
	public static final String CMD_HELP                       = "help";
	
	//XXX adjust host/port (e.g. from command line args)
	private String simHost = "localhost";
	private int simPort = 9876;

	public PersoSim() {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Default command line main method.
	 * 
	 * This starts the PersoSim simulator within its own thread and accepts user
	 * commands to control it on the existing thread on a simple command prompt.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PersoSim sim = new PersoSim();
		
		try {
			sim.handleArgs(args);
			sim.run();
		} catch (IllegalArgumentException e) {
			System.out.println("simulation aborted, reason is: " + e.getMessage());
			sim.stopSimulator();
		}
	}

	@Override
	public void run() {
		System.out.println("Welcome to PersoSim");

		startSimulator();
		handleUserCommands();
	}
	
	public static void showExceptionToUser(Exception e) {
		System.out.println("Exception: " + e.getMessage());
		e.printStackTrace();
	}

	/**
	 * This method implements the behavior of the user command prompt. E.g.
	 * prints the prompt, reads the user commands and forwards this to the
	 * respective method for processing.
	 */
	private void handleUserCommands() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean executeUserCommands = true;

		while (executeUserCommands) {
			System.out.println("PersoSim commandline: ");
			String cmd = null;
			try {
				cmd = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (cmd != null) {
					cmd = cmd.trim();
					String[] args = parseArgs(cmd);
					
					if(args.length > 0) {
						switch (args[0]) {
				            case CMD_LOAD_PERSONALIZATION:
				            	if(args.length == 2) {
				            		try{
				            			setPersonalization(args[1]);
				            			stopSimulator();
				            			startSimulator();
				            		} catch(IllegalArgumentException e) {
				            			System.out.println("unable to set personalization, reason is: " + e.getMessage());
				            			System.out.println("simulation is stopped");
				            			stopSimulator();
				            		}
				            	} else{
				            		System.out.println("set personalization command requires one single file name");
				            	}
				            	break;
				            case CMD_SEND_APDU:
				            	cmdSendApdu(cmd);
				            	break;
				            case CMD_START:
				            	startSimulator();
				            	break;
				            case CMD_RESTART:
				            	stopSimulator();
				            	startSimulator();
				            case CMD_STOP:
				            	stopSimulator();
				            	break;
				            case CMD_EXIT:
				            	stopSimulator();
								executeUserCommands = false;
								break;
				            case CMD_HELP:
				            	System.out.println("Available commands:");
								System.out.println(CMD_SEND_APDU + " <hexstring>");
								System.out.println(CMD_LOAD_PERSONALIZATION + " <file name>");
								System.out.println(CMD_START);
								System.out.println(CMD_RESTART);
								System.out.println(CMD_STOP);
								System.out.println(CMD_EXIT);
								System.out.println(CMD_HELP);
								break;
				            default: System.out.println("unrecognized command \"" + args[0] + "\" and parameters will be ignored");
				                     break;
						}
					}
				}
			} catch (RuntimeException e) {
				showExceptionToUser(e);
			}
		}
	}
	
	/**
	 * This method parses the provided String object for commands and possible arguments.
	 * It will return an array of length 0-2 depending on the trimmed encoded command String.
	 * If the String is empty an array of length 0 will be returned.
	 * If the String contains white space an array of length 2 will be returned with the substring up to the white space followed by the trimmed rest.
	 * Otherwise the an array of length 1 is returned only bearing a command.
	 * @param args the argument String to be parsed
	 * @return the parsed arguments
	 */
	public static String[] parseArgs(String args) {
		String argsInput = args.trim().toLowerCase();
		
		int index = argsInput.indexOf(" ");
		
		if(index >= 0) {
			String cmd = args.substring(0, index);
			String params = args.substring(index).trim();
			return new String[]{cmd, params};
		} else{
			if(args.length() > 0) {
				return new String[]{args};
			} else{
				return new String[0];
			}
			
		}
	}

	/**
	 * This method handles instantiation and (re)start of the SocketSimulator.
	 */
	private void startSimulator() {
		if (simulator == null) {
			simulator = new SocketSimulator(getPersonalization(), simPort);
		}
		
		if (!simulator.isRunning()) {
			simulator.start();
		}

	}

	/**
	 * This method returns the content of {@link #currentPersonalization}, the
	 * currently used personalization. If no personalization is set, i.e. the
	 * variable is null, it will be set to the default personalization which
	 * will be returned thereafter. This mode of accessing personalization
	 * opportunistic assumes that a personalization will always be set and
	 * generating a default personalization is an overhead only to be spent as a
	 * measure of last resort.
	 * 
	 * @return the currently used personalization
	 */
	public Personalization getPersonalization() {
//		// try to read perso from provided file
//		String persoFileName = "perso.xml";
//		
//		currentPersonalization = parsePersonalization(persoFileName);
		
		if(currentPersonalization == null) {
			System.out.println("Loading default personalization");
			currentPersonalization = new DefaultPersoTestPki();
		}
		
		return currentPersonalization;
	}
	
	public static Personalization parsePersonalization(String persoFileName) {
		// try to read perso from provided file
		File persoFile = new File(persoFileName);
		
		Unmarshaller um;
		try {
			um = PersoSimJaxbContextProvider.getContext().createUnmarshaller();
			System.out.println("Loading personalization from file " + persoFileName);
			return (Personalization) um
					.unmarshal(new FileReader(persoFile));
		} catch (JAXBException e) {
			throw new IllegalArgumentException("Unable to parse personalization from file " + persoFileName);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Perso file " + persoFileName + " not found");
		}
	}
	
	public void setPersonalization(String persoFileName) {
		Personalization perso = parsePersonalization(persoFileName);
		System.out.println("personalization successfully read from file " + persoFileName);
		currentPersonalization = perso;
	}

	/**
	 * Stops the simulator thread and returns when the thread is stopped.
	 */
	private void stopSimulator() {
		if (simulator != null) {
			simulator.stop();
		}
	}

	/**
	 * Transmit an APDU to the card
	 * 
	 * @param cmd
	 *            string containing the command
	 */
	private void cmdSendApdu(String cmd) {
		cmd = cmd.trim();

		Pattern cmdSendApduPattern = Pattern
				.compile("^send[aA]pdu ([0-9a-fA-F\\s]+)$");
		Matcher matcher = cmdSendApduPattern.matcher(cmd);
		if (!matcher.matches()) {
			throw new RuntimeException("invalid arguments to sendApdu");
		}
		String apdu = matcher.group(1);
		exchangeApdu(apdu);

	}

	/**
	 * Transmit the given APDU to the simulator, which processes it and returns
	 * the response. The response APDU is received from the simulator via its
	 * socket interface and returned to the caller as HexString.
	 * 
	 * @param cmdApdu
	 *            HexString containing the CommandAPDU
	 * @return
	 */
	private String exchangeApdu(String cmdApdu) {
		cmdApdu = cmdApdu.replaceAll("\\s", ""); // remove any whitespace

		Socket socket;
		try {
			socket = new Socket(simHost, simPort);
		} catch (IOException e) {
			socket = null;
			showExceptionToUser(e);
			return null;
		}

		PrintStream out = null;
		BufferedReader in = null;
		try {
			out = new PrintStream(socket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch (IOException e) {
			showExceptionToUser(e);
		}

		out.println(cmdApdu);
		out.flush();

		String respApdu = null;
		try {
			respApdu = in.readLine();
		} catch (IOException e) {
			showExceptionToUser(e);
		} finally {
			System.out.println("> " + cmdApdu);
			System.out.println("< " + respApdu);
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					showExceptionToUser(e);
				}
			}
		}

		return respApdu;

	}

	public void handleArgs(String[] args) {
		Iterator<String> argsIterator = Arrays.asList(args).iterator();
		
		String currentArgument;
		while(argsIterator.hasNext()) {
			currentArgument = argsIterator.next();
			
			switch (currentArgument) {
            case CMD_LOAD_PERSONALIZATION_SHORT:
            	if(argsIterator.hasNext()) {
            		String fileName = argsIterator.next();
            		try{
            			setPersonalization(fileName);
            		} catch(IllegalArgumentException e) {
            			throw new IllegalArgumentException("unable to set personalization, reason is: " + e.getMessage());
            		}
            	} else{
            		System.out.println("set personalization command requires file name");
            	}
            	break;
            default: System.out.println("unrecognized command or parameter \"" + currentArgument + "\" will be ignored");
                     break;
			}
		}
		
	}

}
