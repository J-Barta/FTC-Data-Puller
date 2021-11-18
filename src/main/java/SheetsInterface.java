import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

public class SheetsInterface {
    Sheets service;


    public SheetsInterface(Sheets service) {
        this.service = service;
    }

    public String cellRange(String sheet, int teams, int statistics) {
        return sheet + "!A1:" + Constants.intToLetter.get(statistics) + (teams);
    }

    public List<List<Object>> makeCellsfromPreliminaryData(List<Integer> numbers, List<String> names, List<List<String>> stats, List<Object> statsNames) {
        List<List<Object>> cells = new ArrayList<>();

        cells.add(statsNames);

        for(int i = 0; i < numbers.size(); i++) {
            int finalI = i;
            cells.add(new ArrayList<Object>() {{
                add((numbers.get(finalI).toString()));
                add((names.get(finalI)));
                for(int o = 0; o < stats.size(); o++) {
                    System.out.println("Stats.size() " + stats.size());
                    System.out.println("Current stat size" + stats.get(o).size());
                    System.out.println("o " + o);
                    System.out.println("i " + finalI);
                    add((stats.get(o).get(finalI)));
                }
            }});
        }
        return cells;
    }

    public List<List<Object>> makeCellsFromScoutingData(List<List<Object>> stats, List<Object> statsNames) {
        List<List<Object>> cells = new ArrayList<>();

        cells.add(statsNames);

        for(List<Object> stat :stats) {
            cells.add(stat);
        }
        return cells;
    }


    public String createSheet(String title) throws IOException {
        // [START sheets_create]
        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties()
                        .setTitle(title));
        spreadsheet = service.spreadsheets().create(spreadsheet)
                .setFields("spreadsheetId")
                .execute();
        System.out.println("Spreadsheet ID: " + spreadsheet.getSpreadsheetId());
        // [END sheets_create]
        return spreadsheet.getSpreadsheetId();
    }

    public UpdateValuesResponse updateSheetValues(String spreadsheetId, String range,
                                                         String valueInputOption, List<List<Object>> _values)
            throws IOException {
        List<List<Object>> values = _values;
        // [END_EXCLUDE]
        ValueRange body = new ValueRange()
                .setValues(values);
        UpdateValuesResponse result =
                service.spreadsheets().values().update(spreadsheetId, range, body)
                        .setValueInputOption(valueInputOption)
                        .execute();
        System.out.printf("%d cells updated.", result.getUpdatedCells());
        // [END sheets_update_values]
        return result;
    }

    public List<List<Object>> retrieveStats(String range, String id, String teamRange) throws IOException {

        List<List<Object>> stats = new ArrayList<>();

        ValueRange response = service.spreadsheets().values()
                .get(id, range)
                .execute();
        List<List<Object>> values = response.getValues();

        ValueRange teamResponse = service.spreadsheets().values()
                .get(id, teamRange)
                .execute();
        List<List<Object>> teamValues = teamResponse.getValues();


        for(List<Object> team : teamValues) {
            String teamString = (String) team.get(0);

            //Get the entries for the current team
            //this returns a list where each stat goes in the following order:
                //AUTON: Number, pre-load (at the end because it got messed up), parking type, duck delivery, extra freight,
                //TELEOP: High, Medium, Low, Shared
                //ENDGAME: Capping, Did they deliver ducks, number of ducks delivered
            List<List<Object>> teamStats = values.stream()
                    .filter(value -> parseInt(teamString) == parseInt(value.get(0).toString()))
                    .collect(Collectors.toList());

            //Collect statistics here
            double matchesPlayed = teamStats.size();

            double preLoadSuccess = 0;

            double successfulPark = 0;

            double autonDuckTries = 0;
            double autonDuckSuccess = 0;

            double totalExtraFreight = 0;
            double totalHighFreight = 0;
            double totalMidFreight = 0;
            double totalLowFrieght = 0;
            double totalSharedFreight = 0;

            double caps = 0;

            double duckAttempts = 0;
            double totalEndDucks = 0;

            double endPark = 0;

            for(List<Object> s : teamStats) {
                preLoadSuccess += s.get(3) == "Yes" ? 1 : 0;

                String parkType = (String) s.get(5);

                boolean completePark = (parkType == "Completely in warehouse") || (parkType == "Completely in warehouse offset") || (parkType == "Completely in storage unit");
                successfulPark += (completePark) ? 1 : 0;

                String autoDuck = (String) s.get(4);
                boolean triedDuck = ("Yes".equals(autoDuck)) || ("Failed".equals(autoDuck));
                autonDuckTries += triedDuck ? 1 : 0;
                autonDuckSuccess += "Yes".equals(autoDuck) ? 1 : 0;

                totalExtraFreight += parseInt((String) s.get(6));
                totalHighFreight += parseInt((String) s.get(7));
                totalMidFreight += parseInt((String) s.get(8));
                totalLowFrieght += parseInt((String) s.get(9));
                totalSharedFreight += parseInt((String) s.get(10));

                caps += s.get(13) == "Yes" ? 1 : 0;

                //Endgame ducks
                String endDuck = (String) s.get(11);
                duckAttempts += ("Yes".equals(endDuck)) ? 1 : 0;
                totalEndDucks += ("Yes".equals(endDuck)) ? Double.parseDouble((String) s.get(12)) : 0;

                //Endgame Parking
                 endPark += ("Yes".equals(s.get(14))) ? 1 : 0;
            }

            List<Object> addList = new ArrayList<>();

            if(teamStats.size() != 0) {
                addList.add(teamString);

                //Preload success rate
                addList.add(Double.toString(preLoadSuccess / matchesPlayed));
                //Duck Success rate
                addList.add(Double.toString(autonDuckSuccess / autonDuckTries));
                //Park Success Rate
                addList.add(Double.toString(successfulPark / matchesPlayed));
                //Different freight scores
                addList.add(Double.toString(totalExtraFreight / matchesPlayed));
                addList.add(Double.toString(totalHighFreight / matchesPlayed));
                addList.add(Double.toString(totalMidFreight / matchesPlayed));
                addList.add(Double.toString(totalLowFrieght / matchesPlayed));
                addList.add(Double.toString(totalSharedFreight / matchesPlayed));
                //Capping sucess rate
                addList.add(Double.toString(caps / matchesPlayed));
                //Endgame Ducks
                addList.add(Double.toString(totalEndDucks / duckAttempts));
                //Endgame Parking
                addList.add(Double.toString(endPark / matchesPlayed));

            } else {
                //TODO: Add all the other stats that I'm analyzing
                addList.add(teamString);
                addList.add("No stats");
                addList.add("No stats");
                addList.add("No stats");
                addList.add("No stats");
                addList.add("No stats");
                addList.add("No stats");
                addList.add("No stats");
                addList.add("No stats");
                addList.add("No stats");
                addList.add("No stats");
                addList.add("No stats");
            }
            stats.add(addList);
        }
        return stats;
    }
}