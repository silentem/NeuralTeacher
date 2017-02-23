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
    private String company;
    private Integer interval;
    private GregorianCalendar date = new GregorianCalendar();
    private List<BarData> barData = new ArrayList<BarData>();
    private ArrayList<Integer> startDates = new ArrayList<Integer>();
    private ArrayList<String> days = new ArrayList<>();
    private final int COUNT_BARS = 1000;
    private int index = 0;
    private String generateRequestString(){
        StringBuilder requestString = new StringBuilder();
        requestString.append(request).append("q="+ this.company).append("&i=" + this.interval);
        return requestString.toString();
    }

    public List<BarData> getBarData(){

        List<String> result = makeRequest(generateRequestString());
        barData.clear();
        startDates.clear();
        int i = 0;
        index = 0;
        for (String cortage: result) {
            barData.add(parseBarFromString(cortage));
            if(i++ > COUNT_BARS)
                break;
            ++index;
        }
        return barData;
    }

    public ArrayList<Integer> getStartDates() {
        return startDates;
    }

    public BarData parseBarFromString(String bar){
        GregorianCalendar tmpCalendar = new GregorianCalendar();
        String[] params = bar.split(",");
        if(params[0].startsWith("a")){
            String time = params[0].substring(1);
            Date d = new Date(Long.parseLong(time));
            DateFormat formatte = new SimpleDateFormat("dd hh:mm:ss");
            System.out.println(formatte.format(d));
            startDates.add(index);
            date.setTime(d);
        }
        BarData newBar = new BarData((GregorianCalendar)date.clone(),
                Double.parseDouble(params[4]),//open
                Double.parseDouble(params[2]),//high
                Double.parseDouble(params[3]),//low
                Double.parseDouble(params[1]),//close
                1);
        date.add(Calendar.MINUTE, interval/60);
        return  newBar;
    }
    public BarData getBarRightByIndex(int index){
        return barData.get(128 + index);
    }
    public BarData getBarLeftByIndex(int index){
        return barData.get(index);
    }
    public List<String> makeRequest(String uri) {
        List<String> result = new ArrayList<String>(1000);

        BufferedReader reader = null;

        try{
            URL url = new URL(generateRequestString());
            reader = new BufferedReader(new InputStreamReader(url.openStream()));

            int coutRow = 1;

            while(coutRow++ < 8){
                reader.readLine();
            }

            String resultString;
            while ((resultString = reader.readLine())!= null){
                result.add(resultString);
            }
            return result;
        }catch (Exception ex){
            System.out.println("Make request " + ex.getMessage());
        }finally {
            if(reader!= null){
                try{
                    reader.close();
                }catch(IOException ex){
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
    public ArrayList<BarData> getParametersToWrite(int from, int to){
        ArrayList<BarData> resultList =  new ArrayList<BarData>(Math.abs(from-to));
        if(from!= to) {
            resultList.addAll(barData.subList(Math.min(from, to), Math.max(from, to)));
        }

        return resultList;
    }
}
