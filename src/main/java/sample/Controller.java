package sample;

import chart.BarData;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import request.RequestService;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static sample.ChartController.getChart;

public class Controller implements Initializable {


    public static final int CANDLE_GAP = 15;

    static RequestService service = new RequestService();
    public List<BarData> bars;
    @FXML
    public GridPane main;

    @FXML
    public BorderPane borderPane;
    @FXML
    public Button btnAddPattern;
    @FXML
    public Button btnReloadData;
    @FXML
    public Button btnSaveToMemory;
    @FXML
    public Button btnAddCompany;
    @FXML
    public TextField textCompany;
    @FXML
    public TextField textPattern;
    @FXML
    public ComboBox companyType;
    @FXML
    public ComboBox patternType;
    @FXML
    public ComboBox intervalType;


    public double high, low;//межі аксіса
    public int direct = 0;
    public int indexMinElement, indexMaxElement;//для збереження індексів масиву
    public ObjectProperty<Point2D> mouseProperty = new SimpleObjectProperty<>();
    List<BarData> subList;
    ScrollPane pane = new ScrollPane();
    Rectangle rectangle = new Rectangle();
    Group group;
    ObservableList<String> patternItems = FXCollections.observableArrayList();
    ObservableList<String> companyItems = FXCollections.observableArrayList();
    ObservableList<String> intervalItems = FXCollections.observableArrayList("1 minute", "5 minutes", "10 minutes");
    private int countPatterns;
    private int firstCountPatterns;

    private int leftPos = 0;
    private int leftBound = 0;
    private int rightBound = 200;
    private int rightPos = 300;

    private int boundShift = 100;

    private ArrayList<Line> lines = new ArrayList<>();


    private int fistrIndexInRange;
    private int lastIndexInRange;

    public Controller() {
    }


    public void initialize(URL location, ResourceBundle resources) {
        try {

            companyType.setItems(companyItems);
            patternType.setItems(patternItems);
            intervalType.setItems(intervalItems);

            readParams();
            setSartParameters();
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("fxml/chart.fxml"));
            pane.setContent(root);

            borderPane.setCenter(pane);

            group = ((Group) root);
            group.getChildren().add(rectangle);

            btnReloadData.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                getChart().getYAxis().translateXProperty().unbind();
                int interval;
                int selectedIndex = intervalType.getSelectionModel().getSelectedIndex();
                switch (selectedIndex) {
                    case 0:
                        interval = 60;
                        break;
                    case 1:
                        interval = 300;
                        break;
                    case 2:
                        interval = 600;
                        break;
                    default:
                        interval = 600;
                }
                service.setCompany(companyType.getSelectionModel().getSelectedItem().toString());
                service.setInterval(interval);
                bars = service.getBarData();
                subList = bars.subList(leftPos, rightPos);

                getChart().setBarsToDisplay(subList);
                getChart().setPrefWidth(bars.size() * CANDLE_GAP);

                ((CategoryAxis) getChart().getXAxis()).setEndMargin(bars.size() * CANDLE_GAP - subList.size() * CANDLE_GAP);

                for (Line l : lines) {
                    group.getChildren().remove(l);
                }

                lines.clear();

                for (Integer date : service.getStartDates()) {
                    System.out.println("date = " + date);
                    double pos = date * CANDLE_GAP;
                    double shiftX = getChart().getYAxis().getWidth();
                    Line line = new Line();

                    line.setStyle("-fx-stroke: red;");
                    System.out.println("p: " + pos);
                    line.setStartX(pos + shiftX);
                    line.setEndX(pos + shiftX);
                    line.setStartY(0);
                    line.setEndY(getChart().getHeight());
                    lines.add(line);
                    group.getChildren().add(line);
                }

                System.out.println("size = " + service.getStartDates().size());


                double offset = pane.getHvalue();
                offset *= 1000;

                System.out.println((int) pane.getHvalue());
                double lowerBound = getMinBar(bars, (int) offset);
                double upperBound = getMaxBar(bars, (int) offset);
                ((NumberAxis) getChart().getYAxis()).setTickUnit((upperBound - lowerBound) / 10);

                low = getMinBar(bars, 0);
                high = getMaxBar(bars, 0);
                ((NumberAxis) getChart().getYAxis()).setTickUnit((high - low) / 10);
                ((NumberAxis) getChart().getYAxis()).setLowerBound(low - 0.5);
                ((NumberAxis) getChart().getYAxis()).setUpperBound(high + 0.5);
                getChart().getYAxis().translateXProperty().bind(
                        pane.hvalueProperty()
                                .multiply(
                                        getChart().widthProperty()
                                                .subtract(
                                                        pane.getViewportBounds().getWidth())));
            });


            getChart().setOnScroll((event) -> {
                double deltaY = event.getDeltaY();
                if (deltaY < 0) {
                    if (getChart().getPrefHeight() > pane.getHeight()) {
                        getChart().setPrefWidth(getChart().getWidth() - 60);
                        getChart().setPrefHeight(getChart().getHeight() - 20);
                        for (Line l :
                                lines) {
                            l.setEndY(l.getEndY() - 20);
                        }
                        scale();
                    }
                } else {
                    getChart().setPrefWidth(getChart().getWidth() + 60);
                    getChart().setPrefHeight(getChart().getHeight() + 20);
                    for (Line l :
                            lines) {
                        l.setEndY(l.getEndY() + 20);
                    }
                    scale();
                }
            });

            pane.hvalueProperty().addListener((observable, oldValue, newValue) ->
                    scale()
            );

            getChart().setOnMousePressed(event -> {
                Node chartPlotBackground = getChart().lookup(".chart-plot-background");
                final double shiftX = xSceneShift(chartPlotBackground);

                double x = event.getSceneX() - shiftX;

                fistrIndexInRange = getIndexToCopyFromData(getChart().getXAxis().getValueForDisplay(x), getChart().getData().get(0).getData());


                mouseProperty.set(new Point2D(event.getX(), event.getY()));

                rectangle.setX(event.getX());
                rectangle.setY(event.getY());
                rectangle.setWidth(0);
                rectangle.setHeight(0);
            });
            getChart().setOnMouseDragged(event -> {
                double x = event.getX();
                double y = event.getY();
                rectangle.setX(Math.min(x, mouseProperty.get().getX()));
                rectangle.setY(Math.min(y, mouseProperty.get().getY()));
                rectangle.setWidth(Math.abs(x - mouseProperty.get().getX()));
                rectangle.setHeight(Math.abs(y - mouseProperty.get().getY()));
            });
            getChart().setOnMouseReleased(event -> {

                rectangle.setFill(new Color(0.0f, 0.3f, 0.0f, 0.2f));
                rectangle.setStroke(Color.AQUA);

                Node chartPlotBackground = getChart().lookup(".chart-plot-background");
                final double shiftX = xSceneShift(chartPlotBackground);
                double x = event.getSceneX() - shiftX;
                lastIndexInRange = getIndexToCopyFromData(getChart().getXAxis().getValueForDisplay(x), getChart().getData().get(0).getData());
                System.out.println(fistrIndexInRange + " " + lastIndexInRange);
            });
            btnSaveToMemory.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if (countPatterns != firstCountPatterns) {
                    ArrayList<String> entries = new ArrayList<>();

                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new FileReader("memory.csv"));
                        String entry;
                        while ((entry = reader.readLine()) != null) {
                            entry += ", 0.0";
                            entries.add(entry);
                        }
                    } catch (IOException ex) {

                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException ex) {
                                System.out.println("Rewriting" + ex.getMessage());
                            }
                        }
                    }

                    BufferedWriter writer = null;

                    try {
                        writer = new BufferedWriter(new FileWriter("memory.csv"));
                        for (String entry : entries) {
                            writer.write(entry);
                            writer.newLine();
                        }
                    } catch (IOException ex) {
                        System.out.println("Rewrite" + ex.getMessage());
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException ex) {
                                System.out.println(ex.getMessage());
                            }
                        }
                    }

                    firstCountPatterns = countPatterns;
                }

                if (Math.abs(fistrIndexInRange - lastIndexInRange) > 20 && Math.abs(fistrIndexInRange - lastIndexInRange) <= 30) {

                    ///writing to memory.csv
                    btnSaveToMemory.setDisable(true);
                    System.out.println("First candle in range: " + fistrIndexInRange + "Second candle in range: " + lastIndexInRange);


                    ArrayList<BarData> resultList = service.getParametersToWrite(fistrIndexInRange, lastIndexInRange);
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter("memory.csv", true));
                        int sizeOfList = resultList.size();
                        //доповнення нулями
                        if (sizeOfList < 30) {
                            while (sizeOfList++ < 30) {
                                writer.write("0.0, 0.0, 0.0, 0.0, ");
                            }
                        }
                        ///запис барів
                        for (BarData bar : resultList) {
                            writer.write(bar.getStringToFile());
                            System.out.println(bar.getStringToFile());
                        }

                        double[] patterns = new double[countPatterns];
                        patterns[patternType.getSelectionModel().getSelectedIndex()] = 1.0;
                        for (int i = 0; i < patterns.length - 1; i++) {
                            writer.write(patterns[i] + ", ");
                        }
                        writer.write(String.valueOf(patterns[patterns.length - 1]) + "\r\n");
                        writer.flush();


                    } catch (IOException ex) {
                        System.out.println("Write to file: " + ex.getMessage());
                    }

                    btnSaveToMemory.setDisable(false);
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Information");
                    alert.setHeaderText("Incorrect list of parameters!");
                    alert.setContentText("Incorrect number of parameters for network");
                    alert.showAndWait();
                }
            });
            btnAddCompany.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {

            });
            btnAddPattern.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scale() {
        double minX = pane.getViewportBounds().getMinX();


        int offset = (int) (pane.getHvalue() * 1000);
        if (offset != direct) {


            int current = (int) -(minX / CANDLE_GAP);
            if (current > rightBound) {
                rightPos += boundShift;
                leftPos += boundShift;
                rightBound += boundShift;
                leftBound += boundShift;

                subList = bars.subList(leftPos, rightPos);

                ((CategoryAxis) getChart().getXAxis()).setStartMargin(leftPos * CANDLE_GAP);
                ((CategoryAxis) getChart().getXAxis()).setEndMargin(bars.size() * CANDLE_GAP - rightPos * CANDLE_GAP);

                getChart().setBarsToDisplay(subList);


            }
            if (current < leftBound) {
                rightPos -= boundShift;
                leftPos -= boundShift;
                rightBound -= boundShift;
                leftBound -= boundShift;

                subList = bars.subList(leftPos, rightPos);

                getChart().setBarsToDisplay(subList);

                ((CategoryAxis) getChart().getXAxis()).setStartMargin(leftPos * CANDLE_GAP);
                ((CategoryAxis) getChart().getXAxis()).setEndMargin(bars.size() * CANDLE_GAP - rightPos * CANDLE_GAP);

            }
            System.out.println("current = " + current);
            low = getMinBar(bars, current) - 0.5;
            high = getMaxBar(bars, current) + 0.5;

            ((NumberAxis) getChart().getYAxis()).setLowerBound(low);
            ((NumberAxis) getChart().getYAxis()).setUpperBound(high);
        }
        direct = offset;
    }

    private void readParams() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("patterns.txt"));
            String pattern;

            while ((pattern = reader.readLine()) != null) {
                patternItems.add(pattern);
                countPatterns++;
            }
            firstCountPatterns = countPatterns;

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        try {
            reader = new BufferedReader(new FileReader("company.txt"));
            String company;

            while ((company = reader.readLine()) != null)
                companyItems.add(company);

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }

    }

    private void setSartParameters() {
        companyType.getSelectionModel().selectFirst();
        patternType.getSelectionModel().selectFirst();
        intervalType.getSelectionModel().selectFirst();
    }

    private double xSceneShift(Node node) {
        return node.getParent() == null ? 0 : node.getBoundsInParent().getMinX() + xSceneShift(node.getParent());
    }

    private int getIndexToCopyFromData(String xValue, ObservableList<XYChart.Data<String, Number>> serie) {
        int index = 0;
        for (XYChart.Data<String, Number> data : serie) {
            if (xValue == data.getXValue()) {
                return index;
            }
            ++index;
        }
        return -1;
    }

    private double getMaxBar(List<BarData> bars, int ofsett) {
        double max = bars.get(ofsett).getHigh();
        System.out.println("searching for MAX");
        for (int i = 1 + ofsett; i < 66 + ofsett; i++) {
            double high = bars.get(i).getHigh();
            if (high > max) {
                max = high;
            }
        }
        return max;
    }

    private double getMinBar(List<BarData> bars, int ofsett) {
        double min = bars.get(ofsett).getLow();
        System.out.println("searching for MIN");
        for (int i = 1 + ofsett; i < 66 + ofsett; i++) {
            double low = bars.get(i).getLow();
            if (low < min) {
                min = low;
            }
        }
        return min;
    }
}
