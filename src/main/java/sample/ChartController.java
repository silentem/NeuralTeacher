package sample;

import chart.CandleStickChart;
import chart.DecimalAxisFormatter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import request.RequestService;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Created by Влад on 14.02.2017.
 */
public class ChartController implements Initializable {
    static CandleStickChart chart;
    @FXML
    public Group group;
    RequestService service = new RequestService();

    public static CandleStickChart getChart() {
        return chart;
    }

    public void initialize(URL location, ResourceBundle resources) {

        chart = new CandleStickChart("Trades", new ArrayList<>());
        service.setCompany("BIDU");
        service.setInterval(3600);
        chart.setYAxisFormatter(new DecimalAxisFormatter("#0.000"));
        group.getChildren().add(chart);

    }
}
