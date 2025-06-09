package M3;

/*
Challenge 2: Simple Slash Command Handler
-----------------------------------------
- Accept user input as slash commands
  - "/greet <name>" → Prints "Hello, <name>!"
  - "/roll <num>d<sides>" → Roll <num> dice with <sides> and returns a single outcome as "Rolled <num>d<sides> and got <result>!"
  - "/echo <message>" → Prints the message back
  - "/quit" → Exits the program
- Commands are case-insensitive
- Print an error for unrecognized commands
- Print errors for invalid command formats (when applicable)
- Capture 3 variations of each command except "/quit"
*/

import java.util.Scanner;

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "gb373"; // <-- change to your UCID

    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);

        // Can define any variables needed here

        while (true) {
            System.out.print("Enter command: ");
            // get entered text

            // check if greet
            //// process greet

            // check if roll
            //// process roll
            //// handle invalid formats

            // check if echo
            //// process echo

            // check if quit
            //// process quit

            // handle invalid commnads

            // delete this condition/block, it's just here so the sample runs without edits

            //gb373 06/09/2025
            // Brief Summary:
            // The code is a simple slash command handler that processes user input commands.
            // It supports commands that were required like greeting a user, rolling dice, showing messages, and quitting the program.
            // The program keeps going until the user enters the "/quit" command.
            // Commands are case-insensitive, and the program prints an error message for unrecognized commands or invalid formats.
            
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("/quit")) {
                System.out.println("Exiting program.");
                break;
            } else if (input.startsWith("/greet ")) {
                String name = input.substring(7).trim();
                System.out.println("Hello, " + name + "!");
            } else if (input.startsWith("/roll ")) {
                String[] parts = input.substring(6).split("d");
                if (parts.length != 2) {
                    System.out.println("Error: Invalid roll format. Use '/roll <num>d<sides>'.");
                    continue;
                }
                try {
                    int num = Integer.parseInt(parts[0].trim());
                    int sides = Integer.parseInt(parts[1].trim());
                    int result = (int) (Math.random() * sides) + 1; // Simulate a dice roll
                    System.out.println("Rolled " + num + "d" + sides + " and got " + result + "!");
                } catch (NumberFormatException e) {
                    System.out.println("Error: Invalid numbers in roll command.");
                }
            } else if (input.startsWith("/echo ")) {
                String message = input.substring(6).trim();
                System.out.println(message);
            } else {
                System.out.println("Error: Unrecognized command.");
            }
        }

        printFooter(ucid, 2);
        scanner.close();
    }
}
