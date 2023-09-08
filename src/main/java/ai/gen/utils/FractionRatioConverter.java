package ai.gen.utils;

public class FractionRatioConverter {

    public static String convertToRatio(String fraction) {
        // Split the fraction into its numerator and denominator
        String[] parts = fraction.split("/");

        if (parts.length != 2) {
            return fraction;
        }

        try {
            int numerator = Integer.parseInt(parts[0]);
            int denominator = Integer.parseInt(parts[1]);

            // Calculate the greatest common divisor (GCD) of the numerator and denominator
            int gcd = gcd(numerator, denominator);

            // Calculate the equivalent ratio
            int equivalentNumerator = numerator / gcd;
            int equivalentDenominator = denominator / gcd;

            return equivalentNumerator + "/" + equivalentDenominator;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return fraction;
        }
    }

    // Function to calculate the greatest common divisor (GCD) using Euclidean algorithm
    private static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

}

