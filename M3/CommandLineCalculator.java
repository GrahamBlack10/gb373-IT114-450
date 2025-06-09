package M3;

/*
Challenge 1: Command-Line Calculator
------------------------------------
- Accept two numbers and an operator as command-line arguments
- Supports addition (+) and subtraction (-)
- Allow integer and floating-point numbers
- Ensures correct decimal places in output based on input (e.g., 0.1 + 0.2 → 1 decimal place)
- Display an error for invalid inputs or unsupported operators
- Capture 5 variations of tests
*/

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "gb373"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

        try {
            System.out.println("Calculating result...");
            // gb373 06/09/2025
            // Brief Summary:
            // First it accepts two numbers and an operator from command-line arguments. This supports addition and subtraction and it handles both integer and floating-point numbers. 
            // It also displays the result with the correct number of decimal places based on the input. If the inputs are invalid or the operator is unsupported, it displays an error message. 
            // Finnaly it checks if the arguments are valid numbers and operator and ensures the output is formatted correctly.

            // extract the equation (format is <num1> <operator> <num2>)
             
            // check if operator is addition or subtraction

            // check the type of each number and choose appropriate parsing

            // generate the equation result (Important: ensure decimals display as the
            // longest decimal passed)
            // i.e., 0.1 + 0.2 would show as one decimal place (0.3), 0.11 + 0.2 would shows
            // as two (0.31), etc

            String num1Str = args[0];
            String operator = args[1];
            String num2Str = args[2];
            double num1 = Double.parseDouble(num1Str);
            double num2 = Double.parseDouble(num2Str);
            double result = 0.0;
            int decimalPlaces = Math.max(num1Str.length() - num1Str.indexOf('.') - 1, 
                                         num2Str.length() - num2Str.indexOf('.') - 1);
           
            if (!operator.equals("+") && !operator.equals("-")) {
                System.out.println("Error: Unsupported operator. Use '+' or '-'.");
                return;
            }

            if (operator.equals("+")) {
                result = num1 + num2;
            } else if (operator.equals("-")) {
                result = num1 - num2;
            }

            String format = "%." + decimalPlaces + "f";
            System.out.printf("Result: " + format + "\n", result);
           
        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }
}
