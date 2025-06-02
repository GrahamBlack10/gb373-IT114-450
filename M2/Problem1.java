package M2;

public class Problem1 extends BaseClass {
    private static int[] array1 = {0,1,2,3,4,5,6,7,8,9};   
    private static int[] array2 = {9,8,7,6,5,4,3,2,1,0};
    private static int[] array3 = {0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9};
    private static int[] array4 = {9,9,8,8,7,7,6,6,5,5,4,4,3,3,2,2,1,1,0,0}; 
    private static void printOdds(int[] arr, int arrayNumber){
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfo(arr, arrayNumber);

        // Challenge: Print odd values only in a single line separated by commas
        // Step 1: sketch out plan using comments (include ucid and date)
        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)
        System.out.print("Output Array: ");
        // Start Solution Edits
        
        // gb373 06/2/2025
        
        // Step 1: iterate through the array by using a for loop
        // Step 2: Check if the value is odd by using an if statement 
        // Step 3: If the value is odd, print it out and use a comma to separate the values and not add a comma for the last number
        for(int i = 0; i < arr.length; i++){
            if(arr[i] % 2 != 0){
                System.out.print(arr[i]);
                if(i < arr.length - 1) {
                    System.out.print(", ");
                }
            }
        }

        
        // End Solution Edits
        System.out.println("");
        System.out.println("______________________________________");
    }
    public static void main(String[] args) {
        final String ucid = "gb373"; // <-- change to your UCID
        // no edits below this line
        printHeader(ucid, 1);
        printOdds(array1,1);
        printOdds(array2,2);
        printOdds(array3,3);
        printOdds(array4,4);
        printFooter(ucid, 1);
        
    }
}