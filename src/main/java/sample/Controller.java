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
import javafx.scene.layout.HBox;
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


    public static final int CANDLE_GAP = 5;

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
    @FXML
    public GridPane bottomGrid;
    @FXML
    public Label segSize;
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
            pane.setPannable(true);

            getChart().setPrefWidth(1000);
            getChart().setPrefHeight(850);

            borderPane.setCenter(pane);


            group = ((Group) root);

            btnReloadData.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {


                leftPos = 0;
                leftBound = 0;
                rightBound = (int) (pane.getViewportBounds().getWidth() / 4);
                rightPos = (int) (pane.getViewportBounds().getWidth() / 6) * 3;

                boundShift = (int) (pane.getViewportBounds().getWidth() / 6);

                getChart().getYAxis().translateXProperty().unbind();
                getChart().getData().clear();

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
                String company = companyType.getSelectionModel().getSelectedItem().toString().toUpperCase();
                service.setCompany(company);
                service.setInterval(interval);
                try {
                    bars = service.getBarData();
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Information");
                    alert.setHeaderText(null);
                    alert.setContentText("No internet connection or wrong URL");
                    alert.initOwner(borderPane.getScene().getWindow());
                    alert.showAndWait();
                }

                pane.hvalueProperty().set(0);
                pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
                pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                leftPos = 0;
                leftBound = 0;
                rightBound = bars.size() > rightPos ? rightBound : bars.size();
                rightPos = bars.size() > rightPos ? rightPos : bars.size();

                subList = bars.subList(leftPos, rightPos);

                ((CategoryAxis) getChart().getXAxis()).setStartMargin(leftPos * CANDLE_GAP);
                ((CategoryAxis) getChart().getXAxis()).setEndMargin(bars.size() * CANDLE_GAP - rightPos * CANDLE_GAP);

                getChart().setBarsToDisplay(subList);
                getChart().setPrefWidth(bars.size() * CANDLE_GAP);

                System.out.println("size = " + service.getStartDates().size());

                setupLines();

                double offset = pane.getHvalue();
                offset *= 1000;

                double lowerBound = getMinBar(bars, (int) offset);
                double upperBound = getMaxBar(bars, (int) offset);
                ((NumberAxis) getChart().getYAxis()).setTickUnit((upperBound - lowerBound) / 10);

                low = getMinBar(bars, 0);
                high = getMaxBar(bars, 0);
                ((NumberAxis) getChart().getYAxis()).setTickUnit((high - low) / 10);
                ((NumberAxis) getChart().getYAxis()).setLowerBound(low - 0.2);
                ((NumberAxis) getChart().getYAxis()).setUpperBound(high + 0.2);
                getChart().getYAxis().translateXProperty().bind(
                        pane.hvalueProperty()
                                .multiply(
                                        getChart().widthProperty()
                                                .subtract(
                                                        pane.getViewportBounds().getWidth())));

                pane.widthProperty().addListener((observableValue, oldSceneWidth, newSceneWidth) -> getChart().getYAxis().translateXProperty().bind(
                        pane.hvalueProperty()
                                .multiply(
                                        getChart().widthProperty()
                                                .subtract((Double) newSceneWidth))));
                pane.heightProperty().addListener((observableValue, oldSceneWidth, newSceneWidth) -> getChart().setPrefHeight(pane.getHeight() - 50));

            });

            ToggleGroup toggleGroup = new ToggleGroup();
            ToggleButton on = new ToggleButton("On");
            ToggleButton off = new ToggleButton("Off");
            off.setSelected(true);
            on.setToggleGroup(toggleGroup);
            off.setToggleGroup(toggleGroup);
            on.selectedProperty().addListener(observable -> {
                pane.setPannable(false);
                rectangle.setManaged(false);
                rectangle.setHeight(0);
                rectangle.setWidth(0);
                rectangle.setFill(Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
                ((Group) root).getChildren().add(rectangle);
                setUpZooming(rectangle, getChart());
            });
            off.selectedProperty().addListener(observable -> {
                pane.setPannable(true);
                ((Group) root).getChildren().remove(rectangle);
            });

            HBox box = new HBox();
            box.getChildren().add(on);
            box.getChildren().add(off);
            bottomGrid.add(box, 4, 0);


//scroll

//            pane.setOnScroll(new EventHandler<ScrollEvent>() {
//                @Override
//                public void handle(ScrollEvent event) {
//                    double deltaX  = event.getDeltaX();
//                    double i = pane.getHvalue();
//                    if(deltaX < 0){
//                        i-=0.001;
//                        pane.setHvalue(i < 0 ? 0 : i);
//                    }else{
//                        i+=0.001;
//                        pane.setHvalue(i > 1 ? 1 : i);
//                    }
//                    event.consume();
//                }
//            });

            pane.hvalueProperty().addListener((observable, oldValue, newValue) ->
                    scale()
            );

            btnSaveToMemory.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                System.out.println(countPatterns + " and " + firstCountPatterns);
                if (countPatterns != firstCountPatterns) {
                    System.out.println("in here");
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
                        System.out.println("size = " + entries.size());
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


                    List<BarData> resultList = getParametersToWrite(subList, fistrIndexInRange, lastIndexInRange);
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter("memory.csv", true));
                        int sizeOfList = resultList.size();
                        //доповнення нулями
                        if (sizeOfList < 30) {
                            BarData firstBar = resultList.get(0);
                            while (sizeOfList++ < 30) {
                                writer.write(firstBar.getStringToFile());
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
                    alert.setHeaderText(null);
                    alert.setContentText("Incorrect number of parameters for network");
                    alert.initOwner(borderPane.getScene().getWindow());
                    alert.showAndWait();
                }
            });
            btnAddCompany.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                String company;
                company = textCompany.getText();
                company.toUpperCase();
                if (!company.equals("")) {
                    companyItems.add(company);
                    textCompany.clear();
                    appendParameter("company.txt", company);

                }
            });
            btnAddPattern.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                String pattern;
                pattern = textPattern.getText();
                if (!pattern.equals("")) {
                    patternItems.add(pattern);
                    textPattern.clear();
                    countPatterns += 1;
                    appendParameter("patterns.txt", pattern);

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<BarData> getParametersToWrite(List<BarData> mainList, int from, int to) {
        List<BarData> resultList = new ArrayList<>(Math.abs(from - to));
        if (from != to) {
            resultList.addAll(mainList.subList(Math.min(from, to), Math.max(from, to)));
        }
        return resultList;
    }

    private void appendParameter(String filePath, String parameter) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath, true));
            writer.write(parameter + "\n");
        } catch (IOException e) {
            System.out.println("Write " + filePath + " exception: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.out.println("Can't close " + parameter + " " + e.getMessage());
                }
            }
        }
    }

    private void scale() {
        double minX = pane.getViewportBounds().getMinX();


        int offset = (int) (pane.getHvalue() * 1000);
        if (offset != direct) {


            int current = (int) -(minX / CANDLE_GAP);
            if (current > rightBound) {
                rightPos = rightPos + boundShift > bars.size() ? bars.size() : rightPos + boundShift;
                leftPos += boundShift;
                rightBound += boundShift;
                leftBound += boundShift;

                subList = bars.subList(leftPos, rightPos);


                ((CategoryAxis) getChart().getXAxis()).setStartMargin(leftPos * CANDLE_GAP);
                ((CategoryAxis) getChart().getXAxis()).setEndMargin(bars.size() * CANDLE_GAP - rightPos * CANDLE_GAP);

                getChart().setBarsToDisplay(subList);

                setupLines();


            }
            if (current < leftBound) {
                rightPos -= boundShift;
                leftPos -= boundShift;
                rightBound -= boundShift;
                leftBound -= boundShift;

                subList = bars.subList(leftPos, rightPos);

                ((CategoryAxis) getChart().getXAxis()).setStartMargin(leftPos * CANDLE_GAP);
                ((CategoryAxis) getChart().getXAxis()).setEndMargin(bars.size() * CANDLE_GAP - rightPos * CANDLE_GAP);
                getChart().setBarsToDisplay(subList);

                setupLines();

            }

            low = getMinBar(bars, current) - 0.5;
            high = getMaxBar(bars, current) + 0.5;

            ((NumberAxis) getChart().getYAxis()).setLowerBound(low);
            ((NumberAxis) getChart().getYAxis()).setUpperBound(high);

        }
        direct = offset;
    }

    private void setupLines() {
        for (Line l : lines) {
            group.getChildren().remove(l);
        }

        lines = new ArrayList<>();

        for (Integer date : service.getStartDates()) {
            if (date < rightPos && date >= leftPos) {
                System.out.println("date = " + date);

                String currDate = ((CategoryAxis) getChart().getXAxis()).getCategories().get(date - leftPos);

                double pos = getChart().getXAxis().getDisplayPosition(currDate);
                System.out.println("pos = " + pos);
                System.out.println("currentADate = " + currDate);
                System.out.println("pane = " + pane.getViewportBounds().getWidth());
                System.out.println("chart = " + getChart().getWidth());
                double shiftX = 69;
                Line line = new Line();

                line.setStrokeWidth(0.5);
                line.setStyle("-fx-stroke: red;");
                System.out.println("p: " + pos);
                line.setStartX(pos + shiftX);
                line.setEndX(pos + shiftX);
                line.setStartY(0);
                line.setEndY(getChart().getHeight());
                lines.add(line);
                group.getChildren().add(line);
            }
        }
        for (Integer date : service.getDays()) {
            if (date < rightPos && date >= leftPos) {
                System.out.println("day = " + date);
                String currDate = ((CategoryAxis) getChart().getXAxis()).getCategories().get(date - leftPos);
                double pos = getChart().getXAxis().getDisplayPosition(currDate);
                System.out.println("currentDay = " + bars.get(date).getDateTime().getTime());
                double shiftX = 69;
                Line line = new Line();

                line.setStrokeWidth(0.5);
                line.setStyle("-fx-stroke: blue;");
                System.out.println("p: " + pos);
                line.setStartX(pos + shiftX);
                line.setEndX(pos + shiftX);
                line.setStartY(0);
                line.setEndY(getChart().getHeight());
                lines.add(line);
                group.getChildren().add(line);
            }
        }
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

    private int getIndexToCopyFromData(String xValue, ObservableList<XYChart.Data<String, Number>> series) {
        int index = 0;
        for (XYChart.Data<String, Number> data : series) {
            if (xValue.equals(data.getXValue())) {
                return index;
            }
            ++index;
        }
        return -1;
    }

    private double getMaxBar(List<BarData> bars, int offset) {
        double max = bars.get(offset).getHigh();
        for (int i = 1 + offset; i < validate((int) (pane.getViewportBounds().getWidth() / CANDLE_GAP) + offset); i++) {
            double high = bars.get(i).getHigh();
            if (high > max) {
                max = high;
            }
        }
        return max;
    }

    private int validate(int left) {
        return left > bars.size() ? bars.size() : left;
    }

    private double getMinBar(List<BarData> bars, int offset) {
        double min = bars.get(offset).getLow();
        for (int i = 1 + offset; i < validate((int) (pane.getViewportBounds().getWidth() / CANDLE_GAP) + offset); i++) {
            double low = bars.get(i).getLow();
            if (low < min) {
                min = low;
            }
        }
        return min;
    }

    private void setUpZooming(final Rectangle rect, final Node zoomingNode) {
        final ObjectProperty<Point2D> mouseAnchor = new SimpleObjectProperty<>();
        zoomingNode.setOnMousePressed(event -> {
            Node chartPlotBackground = getChart().lookup(".chart-plot-background");
            final double shiftX = xSceneShift(chartPlotBackground);
            double x = event.getSceneX() - shiftX;
            fistrIndexInRange = getIndexToCopyFromData(getChart().getXAxis().getValueForDisplay(x), getChart().getData().get(0).getData());

            mouseAnchor.set(new Point2D(event.getX(), event.getY()));
            rect.setWidth(0);
            rect.setHeight(0);
        });
        zoomingNode.setOnMouseDragged(event -> {
            double x = event.getX();
            double y = event.getY();
            rect.setX(Math.min(x, mouseAnchor.get().getX()));
            rect.setY(Math.min(y, mouseAnchor.get().getY()));
            rect.setWidth(Math.abs(x - mouseAnchor.get().getX()));
            rect.setHeight(Math.abs(y - mouseAnchor.get().getY()));
        });
        zoomingNode.setOnMouseReleased(event -> {
            Node chartPlotBackground = getChart().lookup(".chart-plot-background");
            final double shiftX = xSceneShift(chartPlotBackground);

            double x = event.getSceneX() - shiftX;
            lastIndexInRange = getIndexToCopyFromData(getChart().getXAxis().getValueForDisplay(x), getChart().getData().get(0).getData());

            firstCountPatterns = fistrIndexInRange;

            segSize.setText(String.valueOf(Math.abs(lastIndexInRange - fistrIndexInRange)));
        });
    }
}
