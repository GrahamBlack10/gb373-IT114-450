package M3;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
Challenge 3: Mad Libs Generator (Randomized Stories)
-----------------------------------------------------
- Load a **random** story from the "stories" folder
- Extract **each line** into a collection (i.e., ArrayList)
- Prompts user for each placeholder (i.e., <adjective>) 
    - Any word the user types is acceptable, no need to verify if it matches the placeholder type
    - Any placeholder with underscores should display with spaces instead
- Replace placeholders with user input (assign back to original slot in collection)
*/

public class MadLibsGenerator extends BaseClass {
    private static final String STORIES_FOLDER = "M3/stories";
    private static String ucid = "gb373"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 3,
                "Objective: Implement a Mad Libs generator that replaces placeholders dynamically.");

        Scanner scanner = new Scanner(System.in);
        File folder = new File(STORIES_FOLDER);

        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length == 0) {
            System.out.println("Error: No stories found in the 'stories' folder.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }
        List<String> lines = new ArrayList<>();

        //gb373 06/09/2025
        //Brief Summary:
        // This program implements a Mad Libs generator that loads a random story from a the folder.
        // It extracts each line into a collection (ArrayList), then is asks the user for each placeholder.
        // The user input replaces the placeholders in the story, and the final story is displayed to the user.
        // The placeholders with underscores are displayed with spaces instead, and the code handles cases where no stories are available.



        // Start edits

        // load a random story file

        // parse the story lines

        // iterate through the lines

        // prompt the user for each placeholder (note: there may be more than one
        // placeholder in a line)

        // apply the update to the same collection slot

        File[] storyFiles = folder.listFiles();
        if (storyFiles == null || storyFiles.length == 0) {
            System.out.println("No stories available.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }

        int randomIndex = (int) (Math.random() * storyFiles.length);
        File selectedStory = storyFiles[randomIndex];
        System.out.println("Selected story: " + selectedStory.getName());
        try (Scanner fileScanner = new Scanner(selectedStory)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                lines.add(line);
            }
        } catch (Exception e) {
            System.out.println("Error reading story file: " + e.getMessage());
            printFooter(ucid, 3);
            scanner.close();
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            while (line.contains("<")) {
                int startIndex = line.indexOf("<");
                int endIndex = line.indexOf(">", startIndex);
                if (endIndex == -1) {
                    break; 
                }
                String placeholder = line.substring(startIndex, endIndex + 1);
                String displayPlaceholder = placeholder.replace("_", " ");
                System.out.print("Please enter a " + displayPlaceholder + ": ");
                String userInput = scanner.nextLine();
                line = line.replace(placeholder, userInput);
            }
            lines.set(i, line);
        }
        // End edits
        System.out.println("\nYour Completed Mad Libs Story:\n");
        StringBuilder finalStory = new StringBuilder();
        for (String line : lines) {
            finalStory.append(line).append("\n");
        }
        System.out.println(finalStory.toString());

        printFooter(ucid, 3);
        scanner.close();
    }
}
