package ai.gen.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AppUtils {

    public static String prettyFormat(ObjectMapper mapper, String response ) {
        try {
            Object json = mapper.readValue(response, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public static String convertRatingToString(int rating) {
//        if (rating < 0 || rating > 5) {
//            throw new IllegalArgumentException("Rating must be between 0 and 5.");
//        }

        if (rating >= 4) {
            return "good";
        } else if (rating >= 2) {
            return "okay";
        } else {
            return "bad";
        }
    }

    public static int extractNumerator(String fraction) {
        String[] parts = fraction.split("/");
        if (parts.length != 2) {
            return 1;
        }

        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 1;
        }
    }
}
