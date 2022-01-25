import com.google.api.services.sheets.v4.Sheets;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataPuller {
    Sheets service;
    List<String> TOA_API_KEYS;
    HttpClient client;
    int totalRequests;
    int activeKey;

    public DataPuller(Sheets service, List<String> TOA_API_KEY) {
        this.service = service;
        this.TOA_API_KEYS = TOA_API_KEY;
        client = HttpClient.newHttpClient();
        totalRequests = 0;
        activeKey = 0;
    }

    public List<List<String>> getStats(List<Integer> numbersIn) throws IOException, InterruptedException, URISyntaxException {

        System.out.println("Retrieving Team Statistics...");

        //The Stats I'll be pulling
        List<String> numbers = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> WLT = new ArrayList<>();
        List<String> avgOPR = new ArrayList<>();
        List<String> topOPR = new ArrayList<>();
        List<String> avgRP = new ArrayList<>();
        List<String> topRP = new ArrayList<>();
        List<String> avgRank = new ArrayList<>();
        List<String> topRank = new ArrayList<>();

        List<Stats[]> stats = new ArrayList<>();

        //Pull all the stats from orange alliance
        for(int num : numbersIn) {
            //Setup the URL

            System.out.println("Current number " + num);

            String response = makeTOARequest("https://theorangealliance.org/api/team/" + num + "/results/2122");

            System.out.println(response);

            Gson gson = new GsonBuilder().create();
            stats.add(gson.fromJson(response, Stats[].class));
        }

        //Do the calculations for the stats
        int index = 0;
        for(Stats[] statArr : stats) {
            if(statArr.length != 0) {
                if(!numbers.contains(Integer.toString(statArr[0].team.team_number))) {
//                    System.out.println("Added team " + statArr[0].team.team_number + ", " + statArr[0].team.team_name_short);
                    numbers.add(Integer.toString(statArr[0].team.team_number));
                    names.add(statArr[0].team.team_name_short);
                }
                int events = 0;

                int wins = 0;
                int losses =0;
                int ties =0;

                double totalOPR = 0;
                double bestOPR = -500;

                double totalRank = 0;
                int bestRank = 50;

                int totalRP = 0;
                double bestRP = -500;
                int statIndex = 0;
                for(Stats stat : statArr) {

                    statIndex++;
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
                topRP.add(Double.toString(bestRP));
                avgRank.add(Double.toString(totalRank / ((double) events)));
                topRank.add(Double.toString(bestRank));

            } else {
                //Set all stats to "NA"
                numbers.add(Integer.toString(numbersIn.get(index)));

                Gson gson = new GsonBuilder().create();
                String response = makeTOARequest("https://theorangealliance.org/api/team/" + numbersIn.get(index));
                //System.out.println(response);
                Team[] team = gson.fromJson(response, Team[].class);
                names.add(team[0].team_name_short);

//                System.out.println("Added team with no event " + Integer.toString(numbersIn.get(index)) + ", " + team[0].team_name_short);

                WLT.add("N/A");
                avgOPR.add("0");
                topOPR.add("0");
                avgRP.add("0");
                topRP.add("0");
                avgRank.add("0");
                topRank.add("0");
            }

            index++;
        }

        List<List<String>> returnList = new ArrayList<>() {{
            add(numbers);
            add(names);
            add(WLT);
            add(avgOPR);
            add(topOPR);
            add(avgRP);
            add(topRP);
            add(avgRank);
            add(topRank);
        }};

        Thread.sleep(5000);

        return returnList;
    }

    public List<Integer> getTeamsInEvent(String eventKey) throws IOException, URISyntaxException, InterruptedException {

        System.out.println("Retrieving teams in the event...");

        Gson gson = new GsonBuilder().create();
        TeamWrapper[] teams = gson.fromJson(makeTOARequest("https://theorangealliance.org/api/event/" + eventKey + "/teams"), TeamWrapper[].class);

        List<Integer> numbers = new ArrayList<>();
        for(int i =0; i < teams.length; i++) {
            numbers.add(teams[i].team.team_number);
            System.out.println(teams[i].team.team_number);
        }

        Collections.sort(numbers);

        Thread.sleep(2000);

        return numbers;
    }

    public List<List<Object>> getMatchSchedule(String eventKey) throws URISyntaxException, IOException, InterruptedException {
        System.out.println("Retrieving match schedule");

        Gson gson = new GsonBuilder().create();
        Type listType = new TypeToken<List<Match>>() {}.getType();
        List<Match> matches = gson.fromJson(makeTOARequest("https://theorangealliance.org/api/event/" + eventKey + "/matches"), listType);

        for(int i = 0; i< matches.size(); i++) {
            if(!matches.get(i).match_key.toLowerCase().contains("q")) {
                matches.remove(i);
                i--;
            }
        }
        for(Match m : matches) {
            System.out.println(m.match_key + ": " + m.participants.get(0).team_key + ", " + m.participants.get(1).team_key + ", " + m.participants.get(2).team_key + ", " + m.participants.get(3).team_key + ", ");
        }

        List<List<Object>> matchList = new ArrayList<>();

        for(Match m : matches) {
            matchList.add(List.of(m.match_key, m.participants.get(0).team_key, m.participants.get(1).team_key, m.participants.get(2).team_key, m.participants.get(3).team_key));
        }
        return matchList;
    }

    String makeTOARequest(String link) throws URISyntaxException, IOException, InterruptedException {
        totalRequests++;
        if(((double) totalRequests ) % 30 == 0) {
            activeKey = 0;
//            System.out.println("Switched back to key 0");
            System.out.println("Waiting for 1 minute to avoid sending too many requests");
            Thread.sleep(60000);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(link))
                .header("Content-Type", "application/json")
                .header("X-TOA-Key", TOA_API_KEYS.get(activeKey))
                .header("X-Application-Origin", "TOA Data Puller")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
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
    double ranking_points;
    Team team;

    public Stats(int rank, double opr, double np_opr, int wins, int losses, int ties, int highest_qual_score, double ranking_points, Team team) {
        this.rank = rank;
        this.opr = opr;
        this.np_opr = np_opr;
        this.wins = wins;
        this.losses = losses;
        this.ties = ties;
        this.highest_qual_score = highest_qual_score;
        this.ranking_points = ranking_points;
        this.team = team;
    }
}

class Team {
    int team_number;
    String team_name_short;

    public Team(int team_number, String team_name_short) {
        this.team_number = team_number;
        this.team_name_short = team_name_short;
    }
}

class TeamWrapper {

    Team team;

    public TeamWrapper(Team team) {
        this.team = team;
    }
}

class Match {
    String match_key;
    List<Participants> participants;

    public Match(String match_key, List<Participants> participants) {
        this.match_key = match_key;
        this.participants = participants;
    }
}

class Participants {
    String team_key;

    public Participants(String team_key) {
        this.team_key = team_key;
    }
}


