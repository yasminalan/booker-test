import com.github.javafaker.Faker;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class BookerTest {

    static Faker faker;
    static String firstName;
    static String lastName;
    static int price;
    static String additionalNeeds;

    @BeforeAll
    static void setupFakerData() {
        faker = new Faker();
        firstName = faker.name().firstName();
        lastName = faker.name().lastName();
        price = faker.number().numberBetween(100, 1000);
        additionalNeeds = faker.options().option("Breakfast", "Lunch", "Dinner", "None");
    }

    private int createBooking() {
        String body = String.format("""
                {
                    "firstname": "%s",
                    "lastname": "%s",
                    "totalprice": %d,
                    "depositpaid": true,
                    "bookingdates": {
                        "checkin": "2024-12-01",
                        "checkout": "2024-12-10"
                    },
                    "additionalneeds": "%s"
                }
                """, firstName, lastName, price, additionalNeeds);

        HttpResponse<JsonNode> createResponse = Unirest.post(Config.generateUrlFromBaseUrl("/booking"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(body)
                .asJson();

        assertThat(createResponse.getStatus()).isBetween(200, 299);

        return createResponse.getBody().getObject().getInt("bookingid");
    }

    private String getAuthToken() {
        String body = """
                {
                  "username": "admin",
                  "password": "password123"
                }
                """;
        HttpResponse<JsonNode> authResponse = Unirest.post(Config.generateUrlFromBaseUrl("/auth"))
                .header("Content-Type", "application/json")
                .body(body)
                .asJson();

        assertThat(authResponse.getStatus()).isEqualTo(200);
        return authResponse.getBody().getObject().getString("token");
    }

    @Test
    void shouldCreateToken() {
        // Given
        String body = """
                {
                "username": "admin",
                "password": "password123"
                }
                """;

        // When
        HttpResponse<JsonNode> response = Unirest.post(Config.generateUrlFromBaseUrl("/auth"))
                .header("Content-Type", "application/json")
                .body(body)
                .asJson();

        // Then
        assertThat(response.getStatus())
                .isBetween(200, 299);
        assertThat(response.getBody().getObject().has("token")).isTrue();
    }

    @Test
    void testPing() {
        HttpResponse<String> response = Unirest.get(Config.generateUrlFromBaseUrl("/ping"))
                .asString();
        assertThat(response.getStatus())
                .isBetween(200, 299);
        assertThat(response.getBody()).isEqualTo("Created");
    }

    @Test
    void shouldCreateAndGetBooking() {

        int bookingId = createBooking();

        HttpResponse<String> getResponse = Unirest.get(Config.generateUrlFromBaseUrl("/booking/" + bookingId))
                .header("Accept", "application/json")
                .asString();

        assertThat(getResponse.getStatus()).isBetween(200, 299);

        JSONObject getBody = new JSONObject(getResponse.getBody());

        // Check that the result of the GET matches the data posted via POST
        assertThat(getBody.getString("firstname")).isEqualTo(firstName);
        assertThat(getBody.getString("lastname")).isEqualTo(lastName);
        assertThat(getBody.getInt("totalprice")).isEqualTo(price);
        assertThat(getBody.getString("additionalneeds")).isEqualTo(additionalNeeds);

    }

    @Test
    void shouldCreateAndUpdateBooking() {

        // 1. Get booking id
        int bookingId = createBooking();

        // 2. Get token
        String token = getAuthToken();

        // 3. Update booking
        HttpResponse<String> updateResponse = Unirest.patch("https://restful-booker.herokuapp.com/booking/" + bookingId)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Cookie", "token=" + token)
                .body("""
                        {
                        "firstname": "James",
                        "lastname": "Brown"
                        }
                        """)
                .asString();

        assertThat(updateResponse.getStatus()).isBetween(200, 299);

        JSONObject fetchedBooking = new JSONObject(updateResponse.getBody());
        assertThat(fetchedBooking.getString("firstname")).isEqualTo("James");
        assertThat(fetchedBooking.getString("lastname")).isEqualTo("Brown");
    }

    @Test
    void shouldCreateAndDeleteBooking() {

        // 1. Get booking id
        int bookingId = createBooking();

        // 2. Get token
        String token = getAuthToken();

        // 3. Delete booking
        HttpResponse<String> deleteResponse = Unirest.delete(Config.generateUrlFromBaseUrl("/booking/" + bookingId))
                .header("Cookie", "token=" + token)
                .asString();

        assertThat(deleteResponse.getStatus()).isBetween(200, 299);

        HttpResponse<String> getAfterDelete = Unirest.get(Config.generateUrlFromBaseUrl("/booking/" + bookingId))
                .header("Accept", "application/json")
                .asString();

        // 4. Verify the deleting process
        assertThat(getAfterDelete.getStatus()).isEqualTo(404);
        System.out.println("Confirmed: Booking no longer exists.");
    }

}
