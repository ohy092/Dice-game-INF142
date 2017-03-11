/*
 * By Øyvind Hytten, University of Bergen, Department of Informatics
 * Client implementation from my solution to Assignment 1 spring 2013, INF142 Computer Networks
 */
 
package assignment1;
import java.io.*; 
import java.net.*;

/*
 * Because of my server implementation,the clients can only connect in a specific order: first A, then B.
 * However, if B tries to connect first, they will continually retry the connection with a given delay
 * until A has connected.
 */

public class Client {

	/*
	 * This field variable defines which player you currently are playing as, and
	 * need to be set manually at the start of each game
	 * 
	 * Player A = true, Player B = false
	 */
	private static boolean playerA = false; 

	private static String hostname = "127.0.0.1";							// The hostname of the server

	/*
	 * The main method connects both clients and continue to launch new games until one of the players
	 * refuse to play any more
	 */
	public static void main(String args[]) throws IOException {

		System.out.print("Initiating player ");
		if(playerA){
			System.out.println("A");
			System.out.println("Waiting for player B to connect...");
			port = 8787;													// Client A's port to connect to the server by
		}
		else{
			System.out.println("B");
			port = 8788;													// Client B's port to connect to the server by
		}

		server = connectToServer(port);										// Connects to the server by the specified port
		input = new BufferedReader(new InputStreamReader(server.getInputStream())); // New input stream for the server
		output = new DataOutputStream(server.getOutputStream());			// New input stream for the server
		
		if(getString().equals("both_connected"))							// Waits for response from server that both clients are connected
			System.out.println("Both players are now connected!");

		while(gameIsOn()){													// Repeats as long as both players want to play
			System.out.println(" Game is on!");
			newGame();														// Starts a new game
		}
		if(firstGame) System.out.println(" Closing...");
		else {																// Given that there has been played at least one game...
			System.out.println();
			int TA = getInt(), TB = getInt();								// Gets the running totals from the server after one player has refused to play
			if(TA == TB)
				System.out.println("The game was a tie!");
			else if(((TA > TB) == playerA))
				System.out.println("You won "+Math.max(TA, TB)+" to "+Math.min(TA, TB)+"!");
			else 
				System.out.println("You lost "+Math.min(TA, TB)+" to "+Math.max(TA, TB)+"!");
		}
	}

	/*
	 * This method starts a new game, reading in numbers from both players and a bonus number.
	 * Then it starts the first round.
	 */
	private static void newGame() throws IOException {
		firstGame = false;													// A game has started
		System.out.println();
		System.out.println("Running totals are T(A): "+getInt()+" and T(B): "+getInt());
		System.out.println("Score for both players is 0.");


		/*
		 * The following boolean value is true if A starts and this is player A, or if A does not 
		 * start and this player is not A. This will determine whether this player or the other
		 * one is the one to start the game.
		 * 
		 * Whether A starts or not is determined by the server.
		 */
		boolean thisPlayerStarts = ((getString().equals("A_starts")) == playerA);

		System.out.print("A coin was tossed. Player ");
		if(thisPlayerStarts == playerA) System.out.print("A");
		else System.out.print("B");
		System.out.println(" starts!");
		System.out.println();

		if(thisPlayerStarts){												// If this is the player to start
			sendString(readNumbers());										// Sends this player's chose numbers to the server
			System.out.println("You selected the numbers "+getString()+"!");// Prints the numbers the server has registered for this player
			String otherPlayersNumbers = getString();						// Waits for and gets the numbers the server has registered for the other player
			int bonusNumber = getInt();										// Waits for and gets the bonus number the server has registered
			if(playerA) System.out.print("Player B");
			else System.out.print("Player A");
			System.out.println(" selected the numbers "+otherPlayersNumbers+", and the bonus number "+bonusNumber);
		}

		else {																// If the other player starts, this player waits for their turn
			String otherPlayersNumbers = getString();						// Waits for and gets the numbers the server has registered for the other player
			if(playerA) System.out.print("Player B");
			else System.out.print("Player A");
			System.out.println(" selected the numbers "+otherPlayersNumbers+"!");
			sendString(readNumbers());										// Sends this player's chose numbers to the server
			System.out.println("You selected the numbers "+getString()+"!");// Prints the numbers the server has registered for this player
			System.out.print("Now s");
			sendInt(readBonus());											// Sends the bonus number
			System.out.println();
			System.out.println("You selected bonus number "+getInt());		// Prints the bonus number the server has registered
		}
		System.out.println();
		newRound(thisPlayerStarts);											// Starts a new game round, telling whether this client is to start or not
	}

	/*
	 * This method starts a new round, and at the end of each round it calls itself recursively if the server tells it to.
	 * At the end of the last round it prints each player's running totals.
	 */
	private static boolean newRound(boolean startingPlayer) throws IOException {
		if(startingPlayer && shakeDice())									// shakeDice() is only evaluated if startingPlayer == true
			sendString("shake");											// Sending command 'shake' if this is the starting (shaking) player

		String whoShook = getString();										// Waiting for information about who shook the dice
		if(startingPlayer)
			System.out.print("You shook the dice!");
		else
			System.out.print("Player "+whoShook.substring(0, 1)+" shook the dice!");
		int die1 = getInt();												// Pseudorandom die #1 from server
		int die2 = getInt();												// Pseudorandom die #2 from server
		System.out.println("The result was "+die1+" + "+die2+" = "+(die1+die2)+"!");
		System.out.println();

		String status = "Player A ";
		if(getInt() == 0) status += "did not ";								// Receives 0 from server if player A missed, otherwise it is the number they hit
		status += "hit, player B ";
		if(getInt() == 0) status += "did not ";								// Receives 0 from server if player B missed, otherwise it is the number they hit
		status += "hit!";
		System.out.println(status);
		int longestLine = status.length();									// This is only for formatting the length of a line printed to stdout

		status = printStatus("A");											// Prints player A's remaining numbers
		System.out.println(status);
		longestLine = Math.max(longestLine, status.length());

		status = printStatus("B");											// Prints player B's remaining numbers
		System.out.println(status);
		longestLine = Math.max(longestLine, status.length());

		System.out.println("Scores for this game is V(A): "+getInt()+" and V(B): "+getInt());// Prints both players' current scores

		printLine(longestLine);												// Prints a graphical line to stdout
		String serverRequestsNewRound = getString();						// Waits for acknowledge of new round from server
		if(serverRequestsNewRound.equals("new_round")){
			return newRound(startingPlayer);								// Starts a new round
		}
		System.out.println("The game is finished!");
		System.out.println("Running totals are T(A): "+getInt()+" and T(B): "+getInt());// Prints both players' running totals
		System.out.println();
		return false;														// This is the end of the current game, as no new round is to be played
	}

	/*
	 * This method prints a given player's remaining numbers, in a formatted way, to stdout
	 */
	private static String printStatus (String player) throws IOException{
		String status = "Player "+player+" has ";
		int[] numbers = {getInt(), getInt(), getInt()};						// Gets the player's remaining numbers from the server
		if(numbers[0] == 0)
			return (status+"no numbers left!");
		status += "the number";
		if(numbers[1] != 0){
			status += "s";
			if(numbers[2] != 0)
				return (status+" "+numbers[0]+", "+numbers[1]+" and "+numbers[2]+" left!");
			return (status+" "+numbers[0]+" and "+numbers[1]+" left!");
		}
		return (status+" "+numbers[0]+" left!");
	}

	/*
	 * This method determines wether both players want to play a new game
	 */
	private static boolean gameIsOn() throws IOException{
		if(playerA && wantToPlay()){										// First, given that this is player A and they want to play...
			System.out.println(" Waiting for player B...");
			String reply = getString();										// Gets B's reply from the server
			if(reply.equals("b_accepted")) {
				System.out.print("Player B also wants to play!");
				return true;												// Game is on, B wants to play
			}
			else if(reply.equals("b_denied")){					
				System.out.print("Player B does"+anotherGame());			// Formatted print to stdout
				return false;												// Game is off, B does not want to play
			}	
		}
		else if(!playerA) {													// Otherways, given that this player is not A...
			System.out.println("Waiting for player A...");
			String reply = getString();										// Gets A's reply from the server

			if(reply.equals("a_denied")){
				System.out.print("Player A does"+anotherGame());			// Formatted print to stdout
				return false;												// Game is off, A does not want to play
			}
			
			else if(reply.equals("a_accepted")){							//...and if A does want to play...
				System.out.println("Player A wants to play! ");
				return wantToPlay(); 										// Returns B's willingness to play
			}
		}
		return false;														// Player is in fact A, but does not want to play
	}

	/*
	 * This method determines whether the current player wants to play or not
	 */
	private static boolean wantToPlay() throws IOException {

		System.out.print("Do you want to play a ");
		if(!firstGame)
			System.out.print("new ");
		System.out.print("game? (y/n) ");

		String response = keyboard.readLine().toUpperCase();				// Current player's response to initiate a game
		System.out.println();

		switch(response) {
		case "Y": System.out.print("You have accepted to play a game!");
		sendString(response);
		return true;														// Current player's wants to play

		case "N": System.out.print("You do"+anotherGame());					// Formatted print to stdout
		sendString(response);
		return false;														// Current player's does not want to play

		default: System.out.println("Command not recognized, please try again!");
		return wantToPlay();												// User has entered an invalid value and must try again
		}
	}

	/*
	 * This really simple method prints a graphical, horizontal line of any given length
	 */
	private static void printLine(int size) {
		for(int i=0; i<size; i++){
			System.out.print("-");
		}
		System.out.println();
	}

	/*
	 * This even simpler method reads a line (string) from the server
	 */
	private static String getString() throws IOException {
		return input.readLine();
	}

	/*
	 * This method reads an int from the server
	 */
	private static int getInt() throws IOException {
		return input.read();
	}

	/*
	 * This method sends a string to the server, appending a line shift
	 */
	private static void sendString(String s) throws IOException {
		output.writeBytes(s+'\n');
	}
	
	/*
	 * This method sends an int to the server
	 */
	private static void sendInt(int i) throws IOException {
		output.write(i);
	}

	/*
	 * This method asks the user to manually shake the dice
	 */
	private static boolean shakeDice() throws IOException {
		System.out.print("Type 'shake' to shake the dice: ");
		String temp = keyboard.readLine();									// Reads input from stdin
		System.out.println();
		if(!temp.equals("shake")){											// User has failed to type 'shake'...
			System.out.println("Sorry, command not recognized! Try again!");
			return shakeDice();												// ...and must try again, until...
		}
		return true;														// ...user has successfully typed 'shake'. They should get a medal for this!
	}

	/*
	 * This method reads three numbers from the user's stdin, and verifies them
	 */
	private static String readNumbers() throws IOException {
		System.out.println("Select three numbers [2-12], separated by commas:");
		String temp = keyboard.readLine().replaceAll(" ", "");				// User types in the desired numbers

		String[] tempArray = temp.split(",");								// The string is split into an array

		if(tempArray.length == 3) {											// Makes sure the array contains exactly three numbers
			for(String s : tempArray){
				try {
					int i = Integer.parseInt(s);							// Converts each number to int
					if(i < 2 || i > 12){									// Makes sure the int is in the accepted range...
						System.out.println("All numbers must be between 2 and 12! Try again!");
						return readNumbers();								// ...or the user must try again
					}
				} catch(NumberFormatException e) {
					System.out.println("Only numbers allowed! Try again!");
					return readNumbers();									// If the input does not contain only valid numbers, user must try again
				}
			}
			return temp;													// Input contains three valid numbers within the accepted range
		}
		System.out.println("Three numbers are needed! Try again!");
		return readNumbers();												// Amount of numbers inputted is not three
	}

	/*
	 * This method reads a bonus number from the user's stdin, and verifies it
	 */
	private static int readBonus() throws IOException {
		System.out.print("elect a bonus number [0-10]: ");
		String temp = keyboard.readLine().replaceAll(" ", "");				// Input from user's stdin
		try {
			int i = Integer.parseInt(temp);									// Converts the number to an int
			if(i < 0 || i > 10){											// If the number is outside the accepted range...
				System.out.println("The number must be between 0 and 10! Try again!");
				System.out.print("S");
				return readBonus();											// ...the user must try again
			}
			return i;														// The input is a valid number
		} catch(NumberFormatException e) { 
			System.out.println("Only numbers allowed! Try again!");
			System.out.print("S");
			return readBonus();												// The input is not a number
		}
	}

	/*
	 * A simple method for formatting a string to be printed to stdout
	 */
	private static String anotherGame() {
		if(firstGame) return " not wish to play a game at this time!";
		return " not wish to play another game at this time!";
	}

	/*
	 * This method connects the current client to the server.
	 * It will continually retry the connection with a given delay until it connects.
	 */
	private static Socket connectToServer(int port){
		int timeoutSeconds = 3;												// The amount of seconds between each connection retry
		try{
			return new Socket(hostname, port);								// Sets up a new socket (connection) to the server
		}
		catch (IOException e) {
			System.out.println("Player A must connect to server first! Retrying in "+timeoutSeconds+" seconds!");
			long retryTime = System.currentTimeMillis();					// The timestamp for when connection failed
			while(System.currentTimeMillis() < retryTime+(timeoutSeconds*1000));// Waits for given amount of seconds
			return connectToServer(port);									// Retries the connection to server
		}
	}
	
	/*
	 * Field variables
	 */
	private static int port;												// The port that the current user will connect to the server by
	private static BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));// Standard input (keyboard)
	private static Socket server; 											// The socket (connection) to the server
	private static BufferedReader input;									// Input stream from server
	private static DataOutputStream output;									// Output stream to server
	private static boolean firstGame = true;								// Initially, the very first game starts
}
