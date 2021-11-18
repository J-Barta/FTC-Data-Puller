import com.google.api.services.sheets.v4.Sheets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataPuller {
    Sheets service;
    String TOA_API_KEY;
    HttpClient client;

    public DataPuller(Sheets service, String TOA_API_KEY) {
        this.service = service;
        this.TOA_API_KEY = TOA_API_KEY;
        client = HttpClient.newHttpClient();

    }

    public List<List<String>> getStats(List<Integer> numbers) throws IOException, InterruptedException, URISyntaxException {

        System.out.println("Retrieving Team Statistics...");

        //The Stats I'll be pulling
        List<String> WLT = new ArrayList<>();
        List<String> avgOPR = new ArrayList<>();
        List<String> topOPR = new ArrayList<>();
        List<String> avgRP = new ArrayList<>();
        List<String> topRP = new ArrayList<>();
        List<String> avgRank = new ArrayList<>();
        List<String> topRank = new ArrayList<>();

        List<Stats[]> stats = new ArrayList<>();

        //Pull all the stats from orange alliance
        for(int num : numbers) {
            //Setup the URL
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://theorangealliance.org/api/team/" + num + "/results/2122"))
                    .header("Content-Type", "application/json")
                    .header("X-TOA-Key", TOA_API_KEY)
                    .header("X-Application-Origin", "TOA Data Puller")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response.body());

            Thread.sleep(2000);

            Gson gson = new GsonBuilder().create();
            stats.add(gson.fromJson(response.body(), Stats[].class));
        }

        //Do the calculations for the stats
        for(Stats[] statArr : stats) {
            if(statArr.length != 0) {
                int events = 0;

                int wins = 0;
                int losses =0;
                int ties =0;

                double totalOPR = 0;
                double bestOPR = -500;

                double totalRank = 0;
                int bestRank = 50;

                int totalRP = 0;
                int bestRP = -500;

                for(Stats stat : statArr) {
                    events++;

                    //WLT Ratio
                    wins += stat.wins;
                    losses += stat.losses;
                    ties += stat.ties;

                    //OPR Stuff
                    totalOPR += stat.opr;
                    if(stat.opr > bestOPR) {
                        bestOPR = stat.opr;
                    }

                    totalRank += stat.rank;
                    if(stat.rank < bestRank) {
                        bestRank = stat.rank;
                    }

                    totalRP += stat.ranking_points;

                    if(stat.ranking_points > bestRP) {
                        bestRP = stat.ranking_points;
                    }

                }

                WLT.add(wins + "-" + losses + "-" + ties );
                avgOPR.add(Double.toString(totalOPR / ((double) events)));
                topOPR.add(Double.toString(bestOPR));

                avgRP.add(Double.toString(totalRP / ((double) events)));
                topRP.add(Integer.toString(bestRP));
                avgRank.add(Double.toString(totalRank / ((double) events)));
                topRank.add(Double.toString(bestRank));


            } else {
                //Set all stats to "NA"
                WLT.add("N/A");
                avgOPR.add("0");
                topOPR.add("0");
                avgRP.add("0");
                topRP.add("0");
                avgRank.add("0");
                topRank.add("0");
            }
        }

        List<List<String>> returnValue = new ArrayList<>() {{
            add(WLT);
            add(avgOPR);
            add(topOPR);
            add(avgRP);
            add(topRP);
            add(avgRank);
            add(topRank);
        }};

        Thread.sleep(5000);

        return returnValue;
    }


    public List<String> getTeamNames(List<Integer> numbers) throws IOException, InterruptedException, URISyntaxException {
        System.out.println("Retrieving Team Names");

        List<String> names = new ArrayList<>();

        for(int num : numbers) {
            //Setup the URL
            URL url = new URL("https://theorangealliance.org/api/team/" + num);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://theorangealliance.org/api/team/" + num))
                    .header("Content-Type", "application/json")
                    .header("X-TOA-Key", TOA_API_KEY)
                    .header("X-Application-Origin", "TOA Data Puller")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String responseString = response.body();

            String teamName = responseString.substring(responseString.indexOf("\"team_name_short\":\"") + 19, responseString.indexOf("\",\"team_name_long\""));
            names.add(teamName);
            System.out.println(teamName);

            Thread.sleep(2000);
        }
        return names;

    }


    public List<Integer> getTeamNumbers() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Please enter the team names you would like to pull data for. \n Press enter after each one and type 'end' when you are finished");

        List<String> numberStrings = new ArrayList<>();

        String currentString = "null";

        while(!currentString.contains("end")) {
            currentString = reader.readLine();
            numberStrings.add(currentString);
        }

        System.out.println("ended");
        numberStrings.remove("end");

        List<Integer> numbers = new ArrayList<>();

        for(String i : numberStrings) {
            numbers.add(Integer.parseInt(i));
        }

        return numbers;
    }

    public List<Integer> getTeamsInEvent(String eventKey) throws IOException, URISyntaxException, InterruptedException {

        System.out.println("Retrieving teams in the event...");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("https://theorangealliance.org/api/event/" + eventKey + "/teams"))
            .header("Content-Type", "application/json")
            .header("X-TOA-Key", TOA_API_KEY)
            .header("X-Application-Origin", "TOA Data Puller")
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new GsonBuilder().create();
        Team[] teams = gson.fromJson(response.body(), Team[].class);

        List<Integer> numbers = new ArrayList<>();
        for(int i =0; i < teams.length; i++) {
            numbers.add(Integer.parseInt(teams[i].team_number));
            //System.out.println(teams[i].team_number);
        }

        Collections.sort(numbers);

        //System.out.println(numbers.size());

        Thread.sleep(2000);

        return numbers;
    }

}

class Stats {
    int rank;
    double opr;
    double np_opr;
    int wins;
    int losses;
    int ties;
    int highest_qual_score;
    int ranking_points;

    public Stats(int rank, double opr, double np_opr, int wins, int losses, int ties, int highest_qual_score, int ranking_points) {
        this.rank = rank;
        this.opr = opr;
        this.np_opr = np_opr;
        this.wins = wins;
        this.losses = losses;
        this.ties = ties;
        this.highest_qual_score = highest_qual_score;
        this.ranking_points = ranking_points;
    }
}

class Team {

    String team_number;

    public Team(String event_participant_key, String event_key, String teamKey, String teamNumber, boolean isActive, String cardStatus, Team team) {
        this.team_number = teamNumber;
    }

    public Team() {
        super();
    }

}



