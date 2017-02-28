package request;

import chart.BarData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Влад on 14.02.2017.
 */
public class RequestService implements Request {
    private final String request = "https://www.google.com/finance/getprices?";
    private final int COUNTBARS = 3000;
    private String company;
    private Integer interval;
    private GregorianCalendar date = new GregorianCalendar();
    private Date nextDate;
    private List<BarData> barDatas = new ArrayList<BarData>();
    private ArrayList<Integer> startDates = new ArrayList<Integer>();
    private ArrayList<Integer> days = new ArrayList<>();
    private int currentDay;
    private int nextDay;
    private int index = 0;

    private String generateRequestString() {
        StringBuilder requestString = new StringBuilder();
        requestString.append(request).append("q=" + this.company).append("&i=" + this.interval);
        return requestString.toString();
    }

    public ArrayList<Integer> getDays() {
        return days;
    }

    public List<BarData> getBarData() throws IOException {

        List<String> result = makeRequest(generateRequestString());

        barDatas.clear();
        startDates.clear();
        days.clear();
        index = 0;
        for (String cortage : result) {
            barDatas.add(parseBarFromString(cortage));
            if (nextDay == currentDay) {
                //додавання початків днів
                days.add(index);
                System.out.println("Start day: " + index);
                currentDay = nextDay;
                nextDay += 1;

            }
            if (index++ > COUNTBARS) break;
        }
        return barDatas;
    }

    public ArrayList<Integer> getStartDates() {
        return startDates;
    }

    public BarData parseBarFromString(String bar) {
        DateFormat formate = new SimpleDateFormat("dd");
        String[] params = bar.split(",");
        if (params[0].startsWith("a")) {
            String time = params[0].substring(1) + "000";
            Date d = new Date(Long.parseLong(time));
            currentDay = Integer.parseInt(formate.format(d));
            System.out.println("Current day: " + this.index);
            nextDay = currentDay + 1;
            startDates.add(this.index);
            date.setTime(d);
        }
        BarData newBar = new BarData((GregorianCalendar) date.clone(),
                Double.parseDouble(params[4]),//open
                Double.parseDouble(params[2]),//high
                Double.parseDouble(params[3]),//low
                Double.parseDouble(params[1]),//close
                1);
        currentDay = Integer.parseInt(formate.format(date.getTime()));
        date.add(Calendar.MINUTE, interval / 60);
        return newBar;
    }

    public BarData getBarRightByIndex(int index) {
        return barDatas.get(128 + index);
    }

    public BarData getBarLeftByIndex(int index) {
        return barDatas.get(index);
    }

    public List<String> makeRequest(String uri) throws IOException {
        List<String> result = new ArrayList<>(1000);

        BufferedReader reader = null;

        try {
            URL url = new URL(generateRequestString());
            reader = new BufferedReader(new InputStreamReader(url.openStream()));

            int coutRow = 1;

            while (coutRow++ < 8) {
                reader.readLine();
            }

            String resultString;
            while ((resultString = reader.readLine()) != null) {
                result.add(resultString);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }


        return result;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public ArrayList<BarData> getParametersToWrite(int from, int to) {
        ArrayList<BarData> resultList = new ArrayList<BarData>(Math.abs(from - to));
        if (from != to) {
            resultList.addAll(barDatas.subList(Math.min(from, to), Math.max(from, to)));
        }
        return resultList;
    }
}
