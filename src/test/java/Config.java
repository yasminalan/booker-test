public class Config {
    public static String BASE_URL = "https://restful-booker.herokuapp.com";

    public static String generateUrlFromBaseUrl(String extension){
        return BASE_URL + extension;
    }
}
