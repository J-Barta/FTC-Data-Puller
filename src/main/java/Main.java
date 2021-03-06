import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Stream;

public class Main {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static List<String> TOA_API_KEYS;
    static DataPuller puller;
    static SheetsInterface sheetsInterface;
    public static final String VERSION = "1.0-Beta";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    static Sheets service;

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = Main.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Prints the names and majors of students in a sample spreadsheet:
     * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
     */
    public static void main(String... args) throws IOException, GeneralSecurityException, InterruptedException, URISyntaxException {

        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String keyPath = "C:/program files/scout/key.txt";
        TOA_API_KEYS = new ArrayList<>();

        try (Stream<String> stream = Files.lines(Paths.get(keyPath))) {
            stream.forEach(TOA_API_KEYS::add);
        }

        for(String key : TOA_API_KEYS) {
            System.out.println(key);
        }

        puller = new DataPuller(service, TOA_API_KEYS);
        sheetsInterface = new SheetsInterface(service);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        Thread.sleep(1000);

        System.out.println("------ FTC Data Puller - v." + VERSION + " -----");

        System.out.println("Please enter the document ID of the spreadsheet you'd like to use");
        String id = reader.readLine();

        System.out.println("Please enter the option you would like to use:" +
                "\nTo update team stats (including this competition): 'update'" +
                "\nTo pull match stats and format them into a new sheet: 'matches'" +
                "\nTo pull teams in the match schedule of a competition: 'match-schedule'");
        String choice = reader.readLine();

        if (choice.toLowerCase().contains("update")) {
            System.out.println("Please enter the name of the sheet of the spreadsheet you want to use");
            String sheet = reader.readLine();

            List<Integer> numbers = new ArrayList<>();
            System.out.println("Enter 'event' to pull a TOA event, or 'teams' to enter individual teams");
            String numberType = reader.readLine();
            if(numberType.equals("event")) {
                System.out.println("Please enter the event key (in the URL of the Orange Alliance)");
                String title = reader.readLine();
                numbers = puller.getTeamsInEvent(title);
            } else {
                System.out.println("Please enter each team separated by a line. Type 'done' when finished");
                String input = reader.readLine();
                while(!input.equalsIgnoreCase("done")) {
                    numbers.add(Integer.parseInt(input));
                    input = reader.readLine();
                }
            }

            System.out.println("Numbers length: " + numbers.size());
            System.out.println("Numbers" + numbers);
            List<List<String>> stats = puller.getStats(numbers);

            sheetsInterface.updateSheetValues(id, sheetsInterface.cellRange("'" + sheet + "'", numbers.size() + 1, 9),  "RAW",
                    sheetsInterface.makeCellsfromPreliminaryData(numbers, stats, new ArrayList<>() {{
                        add("Team Number");
                        add("Team Name");
                        add("WLT ratio");
                        add("Average OPR");
                        add("Top OPR");
                        add("Average Ranking Points");
                        add("Top Ranking Points");
                        add("Average Rank");
                        add("Top Rank");
                    }}));
        } else if(choice.toLowerCase().contains("matches")) {
            //TODO: Take in all the match data and format to work
            System.out.println("Please enter the name of the sheet you would like to pull stats from");
            String sheet = reader.readLine();

            System.out.println("Please enter the name of the sheet you would like to pull the team names from");
            String teamSheet = reader.readLine();

            System.out.println("Please enter the name of the sheet you would like to place the averaged stats into");
            String outputSheet = reader.readLine();

            List<List<Object>> stats = sheetsInterface.retrieveStats(sheet +"!B2:P", id, teamSheet + "!A2:A");
            sheetsInterface.updateSheetValues(id, sheetsInterface.cellRangeOffset(1, outputSheet, stats.size() + 1, 12), "RAW",
                    sheetsInterface.makeCellsFromScoutingData(stats, new ArrayList<>() {{
                        add("Team Number");
                        add("Preload success");
                        add("Duck Success");
                        add("Park Success");
                        add("Average extra");
                        add("Average high");
                        add("Average mid");
                        add("Average low");
                        add("Average shared");
                        add("Capping success");
                        add("endgame duck average");
                        add("endgame park average");
                    }}));
        }else if(choice.toLowerCase().contains("match-schedule")) {
            System.out.println("Please enter the name of the event");
            String event = reader.readLine();
            System.out.println("Please enter the name of the sheet you want data to go into");
            String sheet = reader.readLine();

            List<List<Object>> matchList = puller.getMatchSchedule(event);

            sheetsInterface.updateSheetValues(id, sheetsInterface.cellRangeSchedule(sheet, matchList.size()), "RAW", matchList);

        } else {
            System.out.println("You've entered an invalid option. Rerun the program");
        }

        reader.close();
    }
}