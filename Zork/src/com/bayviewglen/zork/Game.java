package com.bayviewglen.zork;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Class Game - the main class of the "Zork" game.
 *
 * Author: Michael Kolling Version: 1.1 Date: March 2000
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

	private ArrayList<Room> roomHistory;
	// This is a MASTER object that contains all of the rooms and is easily
	// accessible.
	// The key will be the name of the room -> no spaces (Use all caps and
	// underscore -> Great Room would have a key of GREAT_ROOM
	// In a hashmap keys are case sensitive.
	// masterRoomMap.get("GREAT_ROOM") will return the Room Object that is the
	// Great Room (assuming you have one).
	private HashMap<String, Room> masterRoomMap;
	private Inventory playerInventory;

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

	private void initItems(String fileName) throws Exception {
		Scanner itemScanner;
		try {
			itemScanner = new Scanner(new File(fileName));
			while (itemScanner.hasNext()) {
				// Room: bla bla bla
				String roomLine = itemScanner.nextLine();
				String fullRoomName = roomLine.split(":")[1].trim();
				String keyRoom = fullRoomName.toUpperCase().replaceAll(" ", "_");

				// Item: USB; using USB;0.001
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

	/**
	 * Create the game and initialize its internal map.
	 */
	public Game() {
		try {
			playerInventory = new Inventory();
			initRooms("data/rooms.dat");
			initItems("data/items.dat");
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
		System.out.println("Welcome to Zork!");
		System.out.println("Zork is a new, incredibly boring adventure game.");
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
			// Write to disk with FileOutputStream
			try {
				FileOutputStream f_out = new FileOutputStream("savegame1.data");

				// Write object with ObjectOutputStream
				ObjectOutputStream obj_out = new ObjectOutputStream(f_out);

				// Write object out to disk
				obj_out.writeObject(this);
				obj_out.close();
			} catch (Exception ex) {

			}

			// Load cannot be done with Serialize here but it is done in main in
			// the Zork class. The main automatically loads the saved file from
			// the beginning.

		} else if (commandWord.equals("xyzzy")) {
			System.out.println("What game do you think this is?"); // Placeholder
		}
		return false;
	}

	// implementations of user commands:

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
			} else {// x
				playerInventory.addItem(i);
				System.out.println("You have taken the " + itemToTake + ".");
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
							currentRoom.getRoomCharacter().getName()))
						;
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
		if (command.getSecondWord().equals(
				currentRoom.getRoomCharacter().getName()))
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
	} // Technically not supposed to do that, but eclipse disallowed an "else"
		// there.
		// Don't worry, that "if" and the other big "if" can't both be executed
		// at once.

	/**
	 * Print out some help information. Here we print some stupid, cryptic
	 * message and a list of the command words.
	 */
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
