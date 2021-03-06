package com.bayviewglen.zork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Class Game - the main class of the "Zork" game.
 *
 * Original Author: Michael Kolling Version: 1.1 Date: March 2000 Additions made
 * by Sabina Beleuz Neagu (Game, Inventory, and Item Levels), by Ross West
 * (Parser, Command, Rooms) and Zane Feder (Characters)
 * 
 * This class is the main class of the "Zork" application. Zork is a very
 * simple, text based adventure game. Users can walk around some scenery. That's
 * all. It should really be extended to make it more interesting!
 * 
 * To play this game, create an instance of this class and call the "play"
 * routine.
 * 
 * This main class creates and initialises all the others: it creates all rooms,
 * creates the parser and starts the game. It also evaluates the commands that
 * the parser returns.
 */

class Game implements java.io.Serializable {
	private Parser parser;
	private Room currentRoom;

	private ArrayList<Room> roomHistory; // Use to keep track of the previous
											// rooms so you can use the 'back'
											// command.
	private HashMap<String, Room> masterRoomMap; // Keeps track of all available
													// rooms.
	private Inventory playerInventory; // The player's items - initally empty by
										// default.
	final static String SAVED_GAME_FILE = "savegame.data"; // File will be used
															// on 'save'command.
	int playerHealth = 800; // Global variable for player health. It starts at 800

	// load available rooms from the rooms.dat data file
	private void initRooms(String fileName) throws Exception {
		masterRoomMap = new HashMap<String, Room>();
		Scanner roomScanner;
		try {
			HashMap<String, HashMap<String, String>> exits = new HashMap<String, HashMap<String, String>>();
			roomScanner = new Scanner(new File(fileName));
			while (roomScanner.hasNext()) {
				Room room = new Room();
				// Read the Name
				String roomName = roomScanner.nextLine();
				room.setRoomName(roomName.split(":")[1].trim());
				// Read the Description
				String roomDescription = roomScanner.nextLine();
				room.setDescription(roomDescription.split(":")[1].replaceAll(
						"<br>", "\n").trim());
				// Read the Exits
				String roomExits = roomScanner.nextLine();
				// An array of strings in the format E-RoomName
				String[] rooms = roomExits.split(":")[1].split(",");
				HashMap<String, String> temp = new HashMap<String, String>();
				for (String s : rooms) {
					temp.put(s.split("-")[0].trim(), s.split("-")[1]);
				}

				exits.put(roomName.substring(10).trim().toUpperCase()
						.replaceAll(" ", "_"), temp);

				// This puts the room we created (Without the exits in the
				// masterMap)
				masterRoomMap.put(roomName.toUpperCase().substring(10).trim()
						.replaceAll(" ", "_"), room);
				// Now we better set the exits.
			}

			for (String key : masterRoomMap.keySet()) {
				Room roomTemp = masterRoomMap.get(key);
				HashMap<String, String> tempExits = exits.get(key);
				for (String s : tempExits.keySet()) {
					// s = direction
					// value is the room.

					String roomName2 = tempExits.get(s.trim());
					Room exitRoom = masterRoomMap.get(roomName2.toUpperCase()
							.replaceAll(" ", "_"));
					roomTemp.setExit(s.trim().charAt(0), exitRoom);

				}

			}

			roomScanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// Loads available items from the items.dat data file.
	private void initItems(String fileName) throws Exception {
		Scanner itemScanner;
		try {
			itemScanner = new Scanner(new File(fileName));
			while (itemScanner.hasNext()) {
				// Room: Room 106
				String roomLine = itemScanner.nextLine();
				String fullRoomName = roomLine.split(":")[1].trim();
				String keyRoom = fullRoomName.toUpperCase()
						.replaceAll(" ", "_");

				// (example) Item: USB; using USB;0.001
				String itemList = itemScanner.nextLine();
				String[] itemV = itemList.split(":")[1].split(";");
				Item myItem = new Item(itemV[0], itemV[1],
						Double.parseDouble(itemV[2]));
				Room myRoom = masterRoomMap.get(keyRoom);
				if (myRoom != null) {
					myRoom.getRoomInventory().addItem(myItem);
				} else {
					// no room with this name
				}
			}

			itemScanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void initCharacters(String fileName) throws Exception {
		Scanner characterScanner;
		try {
			characterScanner = new Scanner(new File(fileName));
			while (characterScanner.hasNext()) {
				// Room: Room 106
				String roomLine = characterScanner.nextLine();
				String fullRoomName = roomLine.split(":")[1].trim();
				String keyRoom = fullRoomName.toUpperCase().replaceAll(" ", "_");

				// (example) Item: USB; using USB;0.001
				String characterDescription = characterScanner.nextLine();
				String[] characterDescriptionArr = characterDescription.split(";");
				String content = characterScanner.nextLine();
				String[] items = content.split("/");
				String[] itemsSplitOne = items[0].split(";");
				String[] itemsSplitTwo = items[1].split(";");
				Item itemOne = new Item(itemsSplitOne[0], itemsSplitOne[1], Double.parseDouble(itemsSplitOne[2]));
				Item itemTwo = new Item(itemsSplitTwo[0], itemsSplitTwo[1], Double.parseDouble(itemsSplitTwo[2]));
				Inventory Items = new Inventory();
				Items.addItem(itemOne);
				Items.addItem(itemTwo);
				Character parsedCharacter = new Character(characterDescriptionArr[0], characterDescriptionArr[1], Integer.parseInt(characterDescriptionArr[2]), Items);
				Room myRoom = masterRoomMap.get(keyRoom);
				if (myRoom != null) {
					myRoom.setRoomCharacter(parsedCharacter);
					
					
				} else {
					// no room with this name
				}
			}
			
				characterScanner.close();
			
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Create the game and initialize its internal map, room history, player
	 * inventory, parser, etc. This is the game constructor.
	 */
	public Game() {
		try {
			playerInventory = new Inventory();
			initRooms("data/rooms.dat");
			initItems("data/items.dat");
			initCharacters("data/characters.dat");
			currentRoom = masterRoomMap.get("ROOM_106");
			roomHistory = new ArrayList<Room>();
		} catch (Exception e) {
			e.printStackTrace();
		}
		parser = new Parser();
	}

	/**
	 * Main play routine. Loops until end of play.
	 */
	public void play() {
		printWelcome();

		// Enter the main command loop. Here we repeatedly read commands and
		// execute them until the game is over.

		boolean finished = false;
		while (!finished) {
			Command command = parser.getCommand();
			finished = processCommand(command);
		}
		System.out.println("Thank you for playing.  Good bye.");
	}

	/**
	 * Print out the opening message for the player.
	 */
	private void printWelcome() {
		System.out.println();
		System.out.println("Welcome to Zork: The Search for the Lost Code.");
		try {Thread.sleep(700);} catch (InterruptedException e) {e.printStackTrace();}
		System.out.println("Zork is a new, incredibly boring adventure game. Finish your Computer Science Project successfully to win.");
		try {Thread.sleep(700);} catch (InterruptedException e) {e.printStackTrace();}
		System.out.println("Type 'help' if you need help.");
		System.out.println();
		System.out.println(currentRoom.longDescription());
	}

	/**
	 * Given a command, process (that is: execute) the command. If this command
	 * ends the game, true is returned, otherwise false is returned.
	 */
	private boolean processCommand(Command command) {
		if (command.isUnknown()) {
			System.out.println("I don't know what you mean...");
			return false;
		}

		String commandWord = command.getCommandWord();
		if (commandWord.equals("help"))
			printHelp();
		else if (commandWord.equals("go"))
			goRoom(command);
		else if (commandWord.equals("back"))
			backRoom(command);
		else if (commandWord.equals("quit")) {
			if (command.hasSecondWord())
				System.out.println("Quit what?");
			else
				return true; // signal that we want to quit
		} else if (commandWord.equals("eat")) {
			System.out
					.println("Do you really think you should be eating at a time like this?");
		} else if (commandWord.equals("use")) {
			useItem(command);
		} else if (commandWord.equals("items")) {
			playerInventory.displayInventory();
		} else if (commandWord.equals("drop")) {
			dropItem(command);
		} else if (commandWord.equals("take")) {
			takeItem(command);
		} else if (commandWord.equals("give")) {
			giveItem(command);
		} else if (commandWord.equals("hit")) {
			hitThing(command);
		} else if (commandWord.equals("smash")) {
			System.out.println("smash what?"); // Placeholder
		} else if (commandWord.equals("attack") || commandWord.equals("brawl") || commandWord.equals("fight") || commandWord.equals("battle")) {
			attackCharacter(command); // Only for characters
		} else if (commandWord.equals("n")) {
			processCommand(new Command("go", "north", "", ""));
		} else if (commandWord.equals("e")) {
			processCommand(new Command("go", "east", "", ""));
		} else if (commandWord.equals("s")) {
			processCommand(new Command("go", "south", "", ""));
		} else if (commandWord.equals("w")) {
			processCommand(new Command("go", "west", "", ""));
		} else if (commandWord.equals("u")) {
			processCommand(new Command("go", "up", "", ""));
		} else if (commandWord.equals("d")) {
			processCommand(new Command("go", "down", "", ""));
		} else if (commandWord.equals("save")) {
			// Write to savegame.data with FileOutputStream
			try {
				FileOutputStream f_out = new FileOutputStream(SAVED_GAME_FILE);

				// Write object with ObjectOutputStream
				ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
				System.out.println("Game saved to file: " + SAVED_GAME_FILE);
				// Write object out to disk
				obj_out.writeObject(this);
				obj_out.close();
			} catch (Exception ex) {

			} // Load for serialize is done in the main.

		} else if (commandWord.equals("xyzzy")) {
			System.out.println("What game do you think this is?"); // Placeholder
		}
		return false;
	}

	// implementations of user commands:

	private void attackCharacter(Command command) {
		String characterToAttack = command.getSecondWord();
		
		if (characterToAttack == null) {
				System.out.println(currentRoom.getRoomCharacter().getName() + " is in the room.  Who do you want to attack?");
		} else {
			if (currentRoom.getRoomCharacter().getName() == null) {
				System.out.println("There is no character in the room to attack");
			} else {
			int health = currentRoom.getRoomCharacter().getHp(); // character's health
			int healthToSave = health; // copy the character health to add to first player's health later in the case of a fight
			System.out.println("You have "+ playerHealth + " health and " + currentRoom.getRoomCharacter().getName() + " has " + health + " health.  If you win the fight, you get an additional " + health + " health and "+ currentRoom.getRoomCharacter().getName() + "'s ");
			currentRoom.getRoomCharacter().getInventory().displayInventory();
			System.out.println("Are you sure you want to fight? (Y/N)");
			Scanner keyboard = new Scanner(System.in);
			String response = keyboard.nextLine();
			if (response.equalsIgnoreCase("y")){
				while (playerHealth > 0 && health > 0) {
					int playerHit = (int) (Math.random() * 100); // Power of hits are randomized each turn
					int characterHit = (int) (Math.random() * 100);
					System.out.println("You hit " + currentRoom.getRoomCharacter().getName() + " with " + playerHit + " power.");
					try {Thread.sleep(700);} catch (InterruptedException e) {e.printStackTrace();} 
					System.out.println(currentRoom.getRoomCharacter().getName() + " hits you with " + characterHit + " power.");
					try {Thread.sleep(700);} catch (InterruptedException e) {e.printStackTrace();}
					playerHealth -= characterHit; // adjust healths
					health -= playerHit;
					System.out.println("Your health: " + playerHealth);
					try {Thread.sleep(700);} catch (InterruptedException e) {e.printStackTrace();}
					System.out.println("Opponent's health: " + health);
					try {Thread.sleep(700);} catch (InterruptedException e) {e.printStackTrace();}
				}
				if (health <= 0) {
					playerHealth += healthToSave;
					System.out.println("You won! You suck the health out of " + currentRoom.getRoomCharacter().getName() + "'s sleeping body.");
					System.out.println("Your health is now: " + playerHealth);
					try {Thread.sleep(700);} catch (InterruptedException e) {e.printStackTrace();}
					System.out.println("You take " + currentRoom.getRoomCharacter().getName() + "'s");
					String[] inventoryItems = currentRoom.getRoomCharacter().getInventory().stringItems();
					for (int i = 0; i < inventoryItems.length; i++) {
						if (i > 0) {
							System.out.print(" and ");
						}
						System.out.print(inventoryItems[i]);
						Item item = currentRoom.getRoomCharacter().getInventory().getItem(inventoryItems[i]);
						playerInventory.addItem(item);
						currentRoom.getRoomCharacter().getInventory().removeItem(inventoryItems[i]);
					}
				} else {
					System.out.println("You have been killed...");
					try {Thread.sleep(700);} catch (InterruptedException e) {e.printStackTrace();}
					System.out.println("The game is now over.  Try again!");
					System.exit(0);
				}
			}
			}
		}
			
		
	}

	private void useItem(Command command) {
		String itemToUse = command.getSecondWord();
		// Add this item to the roomInventory

		if (itemToUse == null) {
			System.out.println("Use What?");
		} else {
			Item myItem = playerInventory.getItem(itemToUse);
			if (myItem == null) {
				System.out
						.println("You don't have a " + itemToUse + " to use!");
			} else {
				myItem.use();
				System.out.println("You have used the " + itemToUse + ".");
			}
		}

	}

	private void takeItem(Command command) {
		String itemToTake = command.getSecondWord();
		// Add this item to the playerInventory

		if (itemToTake == null) {
			System.out.println("Take What?");
		} else {
			Item i = currentRoom.getRoomInventory().removeItem(itemToTake);

			if (i == null) {
				System.out.println("The room doesn't have a "
						+ command.getSecondWord() + " to take!");
			} else {
				if (playerInventory.addItem(i)) {
					System.out
							.println("You have taken the " + itemToTake + ".");
				} else {
					System.out.println("You cannot take the " + itemToTake
							+ ".");
				}
			}
		}
	}

	private void dropItem(Command command) {
		String itemToDrop = command.getSecondWord();
		// Add this item to the roomInventory

		if (itemToDrop == null) {
			System.out.println("Drop What?");
		} else {
			Item i = playerInventory.removeItem(itemToDrop);
			if (i == null) {
				System.out.println("You don't have a " + itemToDrop
						+ " to drop!");
			} else {
				currentRoom.getRoomInventory().addItem(i);
				System.out.println("You have dropped the " + itemToDrop + ".");
			}
		}

	}

	private void giveItem(Command command) {
		//gives item to non-player character
		//not called anywhere as characters not yet implemented
		if (!command.hasSecondWord()) // if it lacks a second word
			System.out.println("give what?"); // Placeholder
		else if (playerInventory.hasItem(command.getSecondWord())) {
			if (!command.hasThirdWord()) {
				System.out
						.println("give " + command.getSecondWord() + " what?");
			} else if (command.getThirdWord().equals("to")) { // Only possible
																// word right
																// now
				if (command.hasFourthWord()) {
					if (command.getFourthWord().equals(
							currentRoom.getRoomCharacter().getName()));
					{
						Item i = playerInventory.removeItem(command
								.getSecondWord()); // Removes item from player
						currentRoom.getRoomCharacter().getInventory()
								.addItem(i); // Gives it to non-player character
					}
				} else {
					System.out.println("That character is not in this room!");
				}
			} else
				System.out.println("give " + command.getSecondWord() + " "
						+ command.getThirdWord() + " what?");
		} else {
			System.out.println("You don't have that item!");
		}
	}

	private void hitThing(Command command) {
		if (!command.hasSecondWord()) // if it lacks a second word
			System.out.println("hit what?"); // Placeholder
		if (command.getSecondWord().equals(currentRoom.getRoomCharacter().getName()))
			;
		{
			if (!command.hasThirdWord()) {
				System.out.println("hit " + command.getSecondWord() + " what?");
			} else if (command.getThirdWord().equals("with")) { // Only possible
																// word right
																// now
				if (command.hasFourthWord()) {
					if (playerInventory.hasItem(command.getFourthWord())) {

					}
				} else {
					System.out.println("You don't have that weapon!");
				}
			} else
				System.out.println("hit " + command.getSecondWord() + " "
						+ command.getThirdWord() + " what?");
		}
		if (!command.getSecondWord().equals(
				currentRoom.getRoomCharacter().getName())) {
			System.out.println("That character is not in the room.");
		}
	} 

	//Prints the help text.
	private void printHelp() {
		System.out.println("You are lost. You are alone. You wander");
		System.out.println("around at Bayview Glen, Moatfield Campus.");
		System.out.println();
		System.out.println("Your command words are:");
		parser.showCommands();
	}

	/**
	 * Try to go to one direction. If there is an exit, enter the new room,
	 * otherwise print an error message.
	 */
	private void goRoom(Command command) {
		if (!command.hasSecondWord()) {
			// if there is no second word, we don't know where to go...
			System.out.println("Go where?");
			return;
		}

		String direction = command.getSecondWord();

		// Try to leave current room.
		Room nextRoom = currentRoom.nextRoom(direction);

		if (nextRoom == null)
			System.out.println("There is no door!");
		else {
			roomHistory.add(currentRoom);
			currentRoom = nextRoom;
			System.out.println(currentRoom.longDescription());
			if (currentRoom.getRoomCharacter().getName() != null) {
			System.out.println("In this room is " + currentRoom.getRoomCharacter().getName());
			}
		}
	}

	/**
	 * Try to go back to previous room, if there is one. If there is one, go to
	 * the previous room, otherwise print an error message. This can be done
	 * infinitely until the first room as it is an ArrayList.
	 */
	private void backRoom(Command command) {
		if (roomHistory.size() > 0) {
			currentRoom = roomHistory.remove(roomHistory.size() - 1);
			System.out.println(currentRoom.longDescription());
		} else {
			System.out.println("There is no room to go back to.");
		}
	}

}
