/*
 * By Øyvind Hytten, University of Bergen, Department of Informatics
 * Server implementation from my solution to Assignment 1 spring 2013, INF142 Computer Networks
 */

package assignment1;
import java.io.*; 
import java.net.*;
import java.util.*;

public class Server {
	
	/*
	 * The main method simply connects both clients and initiates the game. Connection from players will need to
	 * happen in precise order; otherwise, I could have circumvented this by using threads and mutexes, but I prefer
	 * not to complicate the code any further at this point. I feel the code is complex enough already.
	 */
	public static void main(String args[]) throws IOException {
		socketA = new ServerSocket(PORT_A);									// Creates a new socket for player A
		connectA = socketA.accept();										// Listens for a new connection on this socket
		inputA = new BufferedReader(new InputStreamReader(connectA.getInputStream())); // Creates a new input stream explicitly for this player
		outputA = new DataOutputStream(connectA.getOutputStream());			// Creates a new output stream explicitly for this player

		socketB = new ServerSocket(PORT_B);
		connectB = socketB.accept();
		inputB = new BufferedReader(new InputStreamReader(connectB.getInputStream()));
		outputB = new DataOutputStream(connectB.getOutputStream());
		
		sendString("both_connected");										// Tells both clients that they are both connected

		/*
		 * This implementation is based on TCP. I chose TCP because it offers a reliable connection both server
		 * and client side. It handles both direct packet errors (corruption) and faulty packet orders, and this 
		 * whole application relies entirely on transmitting and receiving valid packets in a correct order.
		 */

		initiateGame();														// Initiates the first game
	}

	/*
	 * This method starts a new game, and when the game is done, it calls initiateGame(),
	 * which in turn requests a new game from the players
	 */
	private static void newGame() throws IOException {


		firstGame = false;													// Do not check for a winner unless there has been at least one game
		System.out.println("New game is on!");

		sendTwoIntegers(TA, TB);											// Tell the clients the initial running totals 
		VA = 0; VB = 0;														// Reset scores
		System.out.println("Scores are 0");

		boolean heads = (new Random()).nextBoolean();						// Coin toss (pseudorandomly)
		String starts = "";
		System.out.print("Cointoss: ");		
		if(heads) starts = "A";												// A get heads, B get tails
		else starts = "B";
		System.out.println(starts+" starts");
		sendString(starts+"_starts");										// Tell the clients who starts					

		aNumbers = new ArrayList<Integer>();
		bNumbers = new ArrayList<Integer>();
		tempNum1 = new ArrayList<Integer>();
		tempNum2 = new ArrayList<Integer>();
		int bonus = 0;

		if(heads) {
			bonus = bonus(inputA, outputA, inputB, outputB);				// A starts by the order bonus() takes its parameters
			aNumbers = tempNum1;
			bNumbers = tempNum2;
		}
		else {
			bonus = bonus(inputB, outputB, inputA, outputA);				// B starts by the order bonus() takes its parameters
			bNumbers = tempNum1;
			aNumbers = tempNum2;
		}

		System.out.print("A numbers: ");
		for(int i=0; i<aNumbers.size(); i++){
			System.out.print(aNumbers.get(i)+" ");
		}
		System.out.println();
		System.out.print("B numbers: ");
		for(int i=0; i<bNumbers.size(); i++){
			System.out.print(bNumbers.get(i)+" ");
		}
		System.out.println();

		bonus = Math.min(Math.max(bonus, 2), 10);							// This is to make sure the bonus number is within allowed limits
		sendInt(bonus);														// Tells both client what the bonus number is
		System.out.println("Bonus: "+bonus);
		System.out.println("------------");

		while(newRound(heads, bonus)){
			sendString("new_round");										// Tells the clients a new round is commencing
			System.out.println("------------");
			System.out.println("New round");
		}
		initiateGame();														// Initiates the game

	}

	/*
	 * This method starts a new round of a game. In a game round, dice are shook by the coin toss winning player
	 */
	private static boolean newRound(boolean heads, int bonus) throws IOException {

		// Shaking the dice
		String whoShook = "";

		/*
		 * The following conditional blocks make sure we get the 'shake' command from the client who won the coin toss.
		 * Also, because of the order Java evaluates the conditions, the server will read input only from either A or B 
		 */
		if(heads && inputA.readLine().equals("shake")) whoShook = "A";
		else if(inputB.readLine().equals("shake")) whoShook = "B";

		int die1 = ((int) (Math.random()*6))+1;								// Pseudorandom die #1
		int die2 = ((int) (Math.random()*6))+1;								// Pseudorandom die #2
		int sum = die1+die2;												// Sum of pips on both dice

		sendString(whoShook+"_shook");										// Tells the clients who shook the dice
		System.out.println(whoShook+" shook dice, result "+die1+"+"+die2+"= "+sum);

		sendTwoIntegers(die1, die2);										// Sends results of both dice to the clients

		int aHits = popIfHit(sum, aNumbers);  								// 0 if no hit
		int bHits = popIfHit(sum, bNumbers);  								// 0 if no hit

		if(aHits != 0) {
			System.out.println("A hit "+aHits);
			VA += aHits; 
		}
		else System.out.println("A no hit");

		if(bHits != 0) {
			System.out.println("B hit "+bHits);
			VB += bHits;
		}
		else System.out.println("B no hit");

		sendTwoIntegers(aHits, bHits);										// Sends the hits to the clients, a 0 is interpreted as a miss

		sendRemainingNumbers(aNumbers);										// Sends player A's remaining numbers to each player												
		sendRemainingNumbers(bNumbers);										// Sends player B's remaining numbers to each player

		System.out.print("A numbers left: ");
		for(int i=0; i<aNumbers.size(); i++){
			System.out.print(aNumbers.get(i)+" ");
		}
		if(aNumbers.isEmpty()) {
			System.out.print("none");
			if(!bNumbers.isEmpty()) VA += bonus;							// A gets bonus if they have no more numbers, and B does
		}
		System.out.println();
		System.out.print("B numbers left: ");
		for(int i=0; i<bNumbers.size(); i++){
			System.out.print(bNumbers.get(i)+" ");
		}
		if(bNumbers.isEmpty()) {
			System.out.print("none");
			if(!aNumbers.isEmpty()) VB += bonus;							// B gets bonus if they have no more numbers, and A does
		}
		System.out.println();

		sendTwoIntegers(VA, VB);											// Sends current scores to both player after the dice have been shook

		if(aNumbers.isEmpty() || bNumbers.isEmpty()){						// If either of the players' numbers are depleted, game is over
			TA += VA;														// Adds current score to A's running totals
			TB += VB;														// Adds current score to B's running totals
			sendString("finished");											// Tells both clients this game is over
			System.out.println("------------");
			System.out.println("Game finished");
			System.out.println();
			System.out.println();
			sendTwoIntegers(TA, TB);										// Sends both players the running totals after this game
			return false;
		}
		return true;

	}

	/*
	 * This method reads the numbers from the clients, and validates them and converts them into an ArrayList.
	 * It starts with the client winning the coin toss, determining this from the order of its parameters.
	 * It also reads the bonus number from the client who lost the coin toss. 
	 */
	private static int bonus(BufferedReader in1, DataOutputStream out1, BufferedReader in2,
			DataOutputStream out2) throws IOException{

		String temp = in1.readLine();
		if(!validateNumbers(temp)) temp = "2,2,2";							// Sets the client's number to a predefined set if not valid
		tempNum1 = convertNumbers(temp);									// Converts the client's numbers into an ArrayList
		temp = tempNum1.get(0)+", "+tempNum1.get(1)+" and "+tempNum1.get(2);
		sendString(temp);													// Sends the starting player's numbers to both clients

		temp = in2.readLine();
		if(!validateNumbers(temp)) temp = "2,2,2";							// Sets the client's number to a predefined set if not valid
		tempNum2 = convertNumbers(temp);									// Converts the client's numbers into an ArrayList
		temp = tempNum2.get(0)+", "+tempNum2.get(1)+" and "+tempNum2.get(2);
		sendString(temp);													// Sends the next player's numbers to both clients

		return in2.read();													// Returns the bonus number read from the second client
	}

	/*
	 * This method initiates a new game by asking both players if they want to play, in which case it calls the method newGame()
	 */
	private static void initiateGame() throws IOException {
		boolean bothWillPlay = true;										// Whether both players want to play, we assume this is true
		if(askToPlay(inputA)){												// Reply to whether A wants to play or not
			System.out.println("A will play");
			outputB.writeBytes("a_accepted" + '\n');

			if(askToPlay(inputB)){											// Reply to whether B wants to play or not
				System.out.println("B will play");
				outputA.writeBytes("b_accepted" + '\n');
				newGame();													// Starts a new game
			}
			else {
				System.out.println("B will NOT play");
				outputA.writeBytes("b_denied" + '\n');						// Tells client A that client B does not want to play
				bothWillPlay = false;
			}
		}
		else {
			System.out.println("A will NOT play");
			outputB.writeBytes("a_denied" + '\n');							// Tells client B that client A does not want to play
			bothWillPlay = false;
		}
		if(!bothWillPlay && !firstGame){									// If either player refuse to play AND there has already been played a game
			sendTwoIntegers(TA, TB);										// Tells both clients each player's running totals
			System.out.println();
			if(TA == TB) System.out.println("Tie");
			else {
				if(TA > TB) System.out.print("A");
				else if(TA < TB) System.out.print("B");
				System.out.println(" won "+Math.max(TA, TB)+" to "+Math.min(TA, TB));
			}

		}
	}

	/*
	 * This method does not explicitly ask the player whether they want to play, that question is implicit.
	 * However, it receives the response to this question and translates it into a boolean value.
	 */
	private static boolean askToPlay(BufferedReader input) throws IOException {

		String response = input.readLine().toUpperCase();					// Gets the response from the client, represented by a single character
		if(response.equals("Y")){
			return true;													// Says 'yes' if the response is y or Y
		}
		return false;														// Says by default 'no' if not
	}

	/*
	 * This method checks the submitted numbers from one of the clients to see if they are withing the accepted
	 * range, that there is three of them, and that they in fact are numbers
	 */
	private static boolean validateNumbers(String numbers){
		String[] splitted = numbers.split(",");								// Splits the substrings between the commas into an array
		if(splitted.length != 3)											// Checks that the array has 3 elements
			return false;
		for(String s : splitted){
			try {
				int i = Integer.parseInt(s);								// Converts each number from a string to an int
				if(i < 2 || i > 12){										// Checks that each number is within range
					return false;
				}
			} catch(NumberFormatException e) {								// If any of the elements in the array cannot be converted to int
				return false;
			}
		}
		return true;														// The set of numbers has passed the test
	}

	/*
	 * This method converts a client's numvers from a string to an ArrayList. The string has already
	 * been validated by validateNumbers() through bonus() and possibly replaced by a valid, predefined string.
	 */
	private static ArrayList<Integer> convertNumbers(String numbers){
		ArrayList<Integer> n = new ArrayList<Integer>();
		String[] splitted = numbers.split(",");								// Splits the substrings between the commas into an array
		for(int i = 0; i < splitted.length; i++){
			n.add(Integer.parseInt(splitted[i]));							// Adds each converted int to the ArrayList
		}
		return n;															// Returns the ArrayList
	}

	/*
	 * This method removes a number from the corresponding ArrayList and returns if the number is a match.
	 * ArrayList's remove() method removes only the first occurence of an element, the rest stays as is.
	 *  
	 * Otherwise, this method returns 0.
	 */
	private static int popIfHit(int sum, ArrayList<Integer> numbers){
		for(int i=0; i<numbers.size(); i++)
			if(numbers.get(i) == sum){
				return numbers.remove(i);
			}
		return 0;
	}

	/*
	 * This is just a simple method that ensures that a string is sent to both clients.
	 * This also makes sure that a line shift is appended to each string that is to be sent.
	 * The order in which two separate clients receive any one given message is not important
	 * in this implementation. The important thing is the order in which each message is sent
	 * to one client.
	 */
	private static void sendString(String message) throws IOException {
		outputA.writeBytes(message+'\n');									// Sends the string to client A
		outputB.writeBytes(message+'\n');									// Sends the string to client B
	}

	/*
	 * This method does the same as the above, except it sends an int
	 */
	private static void sendInt(int number) throws IOException {
		outputA.write(number);												// Sends the int to client A
		outputB.write(number);												// Sends the int to client B
	}

	/*
	 * This method uses the above method to send two ints instead of one
	 */
	private static void sendTwoIntegers(int a, int b) throws IOException{
		sendInt(a);
		sendInt(b);
	}

	/*
	 * This method sends one player's remaining numbers to both clients
	 */
	private static void sendRemainingNumbers(ArrayList<Integer> numbers) throws IOException {
		for(int i=0; i<3; i++){
			if(i<numbers.size())
				sendInt(numbers.get(i));									// Sends the next number
			else
				sendInt(0);													// Sends 0, interpreted as an 'empty slot'
		}
	}

	private static final int PORT_A = 8787, PORT_B = 8788;					// Client ports

	/*
	 * Field variables
	 */
	private static int VA, VB;												// Scores for current game
	private static int TA = 0, TB = 0;										// Running totals, 0 by default
	private static ArrayList<Integer> aNumbers, bNumbers;					// ArrayLists containing each player's numbers
	private static ArrayList<Integer> tempNum1, tempNum2;					// Temporary ArrayLists for reading input need to be defined as field variables
	private static boolean firstGame = true;

	/*
	 * TCP-related objects briefly described in the main method
	 */
	private static ServerSocket socketA, socketB;
	private static Socket connectA, connectB;
	private static BufferedReader inputA, inputB;
	private static DataOutputStream outputA, outputB;
}
