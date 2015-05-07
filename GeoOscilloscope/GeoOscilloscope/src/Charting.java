/*
 * Copyright 2013 Jason Winnebeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.gillius.jfxutils.JFXUtil;
import org.gillius.jfxutils.chart.ChartPanManager;
import org.gillius.jfxutils.chart.FixedFormatTickFormatter;
import org.gillius.jfxutils.chart.JFXChartUtil;
import org.gillius.jfxutils.chart.StableTicksAxis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.chart.XYChart.Data;
import javafx.stage.StageStyle;
import jnpout32.pPort;

public class Charting extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @FXML
    private LineChart<Number, Number> chart;

    @FXML
    private Label outputLabel;

    private XYChart.Series<Number, Number> series;

    private long startTime;
    private long lastTime;

    private Timeline addDataTimeline;

    private int i = 0;

    private boolean outputEnable = false;

    private static File archivo = null;
    private static FileReader fr = null;
    private static BufferedReader br = null;

    // For the parallel 
    IoTest paralellPort = new IoTest();
    int data = -1;

    @FXML
    void addSample(int point) {
        lastTime = System.currentTimeMillis() - startTime;
        series.getData().add(new XYChart.Data<Number, Number>(lastTime, toAnalogic(point)));  // Valores entre 0 y 255

    }

    @FXML
    void autoZoom() {
        chart.getXAxis().setAutoRanging(true);
        chart.getYAxis().setAutoRanging(true);
        //There seems to be some bug, even with the default NumberAxis, that simply setting the
        //auto ranging does not recompute the ranges. So we clear all chart data then re-add it.
        //Hopefully I find a more proper way for this, unless it's really bug, in which case I hope
        //it gets fixed.
        ObservableList<XYChart.Series<Number, Number>> data = chart.getData();
        chart.setData(FXCollections.<XYChart.Series<Number, Number>>emptyObservableList());
        chart.setData(data);
    }

    @FXML
    void saveData() {
        System.out.println("Save data");
        System.out.println(toBinary(toDigital(5)));
        System.out.println(toBinary(toDigital(0)));
        System.out.println(toBinary(toDigital(-5)));
        FileWriter fichero = null;

        try {
            fichero = new FileWriter(System.getProperty("user.dir") + "" + File.separator + "src" + File.separator + "data.txt");
            System.out.println(System.getProperty("user.dir") + "" + File.separator + "src" + File.separator + "data.txt");
            PrintWriter pw = new PrintWriter(fichero);
            for (int i = 0; i < series.getData().size(); i++) {
                pw.println(series.getData().get(i).getXValue().toString() + "," + toBinary(toDigital(Float.parseFloat(series.getData().get(i).getYValue().toString()))));
            }

        } catch (IOException ex) {
            Logger.getLogger(Charting.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @FXML
    void loadData() {
        System.out.println("Load Data");
        series.getData().clear(); // Limpio lo que este ahi, 
        i = 0;
        startTime = System.currentTimeMillis(); // Vuelvo a iniciar

        String[] data = configReaderInit();
        for (int i = 0; i < data.length; i++) {

            String[] coordenadas = data[i].split(",");
            series.getData().add(new XYChart.Data<Number, Number>(Float.parseFloat(coordenadas[0]), toAnalogic(toDecimal(coordenadas[1]))));

        }
        // Despues de cargar, le dare autozoom 
        autoZoom();
    }

    @FXML
    void exit() {
        System.exit(0);
    }

    /*
     Conversion binary to decimal and viceversa
     */
    //De binario a decimal
    private int toDecimal(String numeroBinario) {

        int num = Integer.parseInt(numeroBinario, 2);
        return num;
        //System.out.println(numeroBinario + " base 2 = " + num + " base 10");
    }

    //De decimal a binario
    private String toBinary(int numeroDecimal) {
        String binario = Integer.toBinaryString(numeroDecimal);
        //System.out.println(numeroDecimal+ " base 10 = " + binario + " base 2");
        return binario;
    }

    // Decimal a Hex
    private String hexToDecimal(Integer a) {
        String hex = Integer.toHexString(a);
        System.out.println("Hex value is " + hex);
        return hex;
    }

    /*
     Conversion binario to analogic 
     */
    private Float toAnalogic(int valorDecimal) {
        Float valor = null;
        valor = (float) (((valorDecimal * 10.0) / 255.0) - 5);

        return valor;
    }

    private int toDigital(float valorAnalogico) {
        int valorDigital = 0;
        valorDigital = (int) ((int) (valorAnalogico + 5) * (255) / (10.0));
        return valorDigital;
    }

    public static boolean fileExists() {
        try {
            archivo = new File(System.getProperty("user.dir") + "" + File.separator + "src" + File.separator + "data.txt");

            fr = new FileReader(archivo);
            br = new BufferedReader(fr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String[] configReaderInit() {
        ArrayList<String> listaDatos = new ArrayList<String>();
        String[] result = null;

        if (fileExists()) {
            try {

                String linea;
                String cadena = "";

                while ((linea = br.readLine()) != null) {

                    cadena = linea.toString();
                    listaDatos.add(cadena);

                }

            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                try {
                    if (null != fr) {
                        fr.close();
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

        result = Arrays.copyOf(listaDatos.toArray(), listaDatos.size(), String[].class); // conversion a arreglo 

        return result;
    }

    void isEnableFire() {
        System.out.println("Entro al ciclo ");
        paralellPort.Addr = 0x379;
        paralellPort.do_read();

        switch (paralellPort.datum) {  // inicio del switch 
            case 120:
                outputEnable = true;
                /*
                 try {
                 Thread.sleep(300);
                 } catch (InterruptedException ex) {
                 ex.printStackTrace();
                 }*/
                System.out.println("Es 120");

                break;
            case 56:
                outputEnable = false;
                /*
                 try {
                 Thread.sleep(300);
                 } catch (InterruptedException ex) {
                 ex.printStackTrace();
                 }*/
                System.out.println("Es 56");
                break;

        }//fin del switch  

    }

    @FXML
    void toggleAdd() {
        switch (addDataTimeline.getStatus()) {
            case PAUSED:
            case STOPPED:
                i = 0;
                try {
                    series.getData().clear(); // limpio, para los siguientes 5000
                } catch (Exception e) {

                }
                startTime = System.currentTimeMillis(); // Vuelvo a iniciar
                isEnableFire(); // Va a ver si es enable == true 
                if (outputEnable) {
                    addDataTimeline.play();
                }
                chart.getXAxis().setAutoRanging(true);
                chart.getYAxis().setAutoRanging(true);
                //Animation looks horrible if we're updating a lot
                chart.setAnimated(false);
                chart.getXAxis().setAnimated(false);
                chart.getYAxis().setAnimated(false);
                break;
            case RUNNING:
                addDataTimeline.stop();
                //Return the animation since we're not updating a lot
                chart.setAnimated(true);
                chart.getXAxis().setAnimated(true);
                chart.getYAxis().setAnimated(true);
                break;

            default:
                throw new AssertionError("Unknown status");
        }
    }

    @Override
    public void start(Stage stage) throws Exception {

        /*
         For the Parallel Port 
         */
        paralellPort.lpt = (new pPort());

        paralellPort.Addr = 0x37A;
        paralellPort.datum = 0x20;
        paralellPort.do_write();

        // paralellPort.do_read((short)(0x378));
        /*
         while (true) {
         if (i != 5000) {
         if (outputEnable) {
         int point = paralellPort.do_read((short) (0x378));
         addSample(point);
         i++;
         }

         }
         break;
         }
         */
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Charting.fxml"));
        Region contentRootRegion = (Region) loader.load();

        StackPane root = JFXUtil.createScalePane(contentRootRegion, 960, 540, false);
        Scene scene = new Scene(root, root.getPrefWidth(), root.getPrefHeight());
        stage.setScene(scene);
        stage.setTitle("Geo's Oscilloscope");
        stage.initStyle(StageStyle.UNDECORATED);

        stage.show();
    }

    @FXML
    void initialize() {
        startTime = System.currentTimeMillis();

        //Set chart to format dates on the X axis
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        ((StableTicksAxis) chart.getXAxis()).setAxisTickFormatter(
                new FixedFormatTickFormatter(format));

        series = new XYChart.Series<Number, Number>();
        series.setName("Voltaje vs Tiempo");

        chart.getData().add(series);

        addDataTimeline = new Timeline(new KeyFrame(
                Duration.millis(1),
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        isEnableFire();
                        if (i != 5000) {
                            if (outputEnable) {

                                paralellPort.Addr = 0x378;
                                paralellPort.do_read();

                                addSample(paralellPort.datum);
                                i++;
                            }

                        } 

                    }
                }
        ));
        addDataTimeline.setCycleCount(Animation.INDEFINITE);

        chart.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                double xStart = chart.getXAxis().getLocalToParentTransform().getTx();
                double axisXRelativeMousePosition = mouseEvent.getX() - xStart;
                outputLabel.setText(String.format(
                        "%d, %d (%d, %d); %d - %d",
                        (int) mouseEvent.getSceneX(), (int) mouseEvent.getSceneY(),
                        (int) mouseEvent.getX(), (int) mouseEvent.getY(),
                        (int) xStart,
                        chart.getXAxis().getValueForDisplay(axisXRelativeMousePosition).intValue()
                ));
            }
        });

        //Panning works via either secondary (right) mouse or primary with ctrl held down
        ChartPanManager panner = new ChartPanManager(chart);
        panner.setMouseFilter(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseButton.SECONDARY
                        || (mouseEvent.getButton() == MouseButton.PRIMARY
                        && mouseEvent.isShortcutDown())) {
                    //let it through
                } else {
                    mouseEvent.consume();
                }
            }
        });
        panner.start();

        //Zooming works only via primary mouse button without ctrl held down
        JFXChartUtil.setupZooming(chart, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() != MouseButton.PRIMARY
                        || mouseEvent.isShortcutDown()) {
                    mouseEvent.consume();
                }
            }
        });

    }

    class DataGetClass implements Runnable {

        Thread datas = null;
        int hola;

        public DataGetClass() {
            hola = -1;
            datas = new Thread(this);
            datas.start();
        }

        @Override
        public void run() {
            do {
                System.out.println("Entro al ciclo ");
                paralellPort.Addr = 0x379;
                paralellPort.do_read();

                switch (paralellPort.datum) {  // inicio del switch 
                    case 120:

                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        System.out.println("Es 120");

                        break;
                    case 56:

                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        System.out.println("Es 56");
                        break;

                }//fin del switch  

            } while (outputEnable == false);    // fin del while 
        }

    }
}
