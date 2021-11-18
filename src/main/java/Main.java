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
import java.security.GeneralSecurityException;
import java.util.*;

public class Main {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String TOA_API_KEY = "bd382f2fbf8f7dd0cf5751f1caeac968b2d576d8cf5a9ece2b159abba4a47659";
    static DataPuller puller;
    static SheetsInterface sheetsInterface;

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

        puller = new DataPuller(service, TOA_API_KEY);
        sheetsInterface = new SheetsInterface(service);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Please enter the document ID of the spreadsheet you'd like to use");
        String id = reader.readLine();

        System.out.println("Please enter the option you would like to use:" +
                "\nTo update team stats (including this competition): 'update'" +
                "\nTo pull match stats and format them into a new sheet: 'matches'");
        String choice = reader.readLine();

        if (choice.toLowerCase().contains("update")) {
            System.out.println("Please enter the event key (in the URL of the Orange Alliance" );

            String title = reader.readLine();
            //reader.close();

            List<Integer> numbers = puller.getTeamsInEvent(title);
            List<String> names = puller.getTeamNames(numbers);
            List<List<String>> stats = puller.getStats(numbers);

            //String id = "1Un2xhSQ3o9lmKDpIL8uNcFfzvpKcfoxScxQuIFJBwq0";

            sheetsInterface.updateSheetValues(id, sheetsInterface.cellRange("'Overview'", numbers.size() + 1, 9),  "RAW",
                    sheetsInterface.makeCellsfromPreliminaryData(numbers, names, stats, new ArrayList<>() {{
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
            sheetsInterface.updateSheetValues(id, sheetsInterface.cellRange(outputSheet, stats.size() + 1, 11), "RAW",
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
                    }}));
        } else {
            System.out.println("You've entered an invalid option. Rerun the program");
        }

        reader.close();
    }
}