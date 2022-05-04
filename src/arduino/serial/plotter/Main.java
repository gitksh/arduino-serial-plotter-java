package arduino.serial.plotter;

import com.fazecast.jSerialComm.SerialPort;
import com.opencsv.CSVWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

public class Main {
    static SerialPort chosenPort;
    static int x = 0;
    public static String saveFile = "";
    public static JLabel saveFileNameLabel = new JLabel("");

    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setTitle("Arduino Plotter");
        window.setSize(600, 400);
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JComboBox<String> portList = new JComboBox<>();
        JButton connectButton = new JButton("Connect");
        JButton saveFileButton = new JButton("Save File");

        JPanel topPanel = new JPanel();
        topPanel.add(portList);
        topPanel.add(connectButton);
        topPanel.add(saveFileButton);
        topPanel.add(saveFileNameLabel);
        window.add(topPanel, BorderLayout.NORTH);

        SerialPort[] portNames = SerialPort.getCommPorts();
        for (SerialPort portName : portNames) portList.addItem(portName.getSystemPortName());

        XYSeries series = new XYSeries("Graph");
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Graph", "Time (seconds)", "Value", dataset);
        window.add(new ChartPanel(chart), BorderLayout.CENTER);

        saveFileButton.addActionListener(e -> {
            try {
                saveFile = String.valueOf(getSaveFile(""));
            } catch (IOException ignore) {}
        });

        connectButton.addActionListener(arg0 -> {
            if(connectButton.getText().equals("Connect")) {
                chosenPort = SerialPort.getCommPort(Objects.requireNonNull(portList.getSelectedItem()).toString());
                chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
                if(chosenPort.openPort()) {
                    connectButton.setText("Disconnect");
                    portList.setEnabled(false);
                }

                Thread thread = new Thread(() -> {
                    Scanner scanner = new Scanner(chosenPort.getInputStream());
//                    CSVWriter writer = null;
                    FileWriter writer = null;
                    while(scanner.hasNextLine()) {
                        try {
                            String line = scanner.nextLine();
                            Float value = Float.parseFloat(line);
//                            int value = Integer.parseInt(line);
                            series.add(x++, value);
                            window.repaint();

                            if (!Objects.equals(saveFile, "")) {
//                                writer = new CSVWriter(new FileWriter(saveFile));
                                writer = new FileWriter(saveFile, true);
//                                String [] record = {String.valueOf(x), String.valueOf(value)};
                                writer.append(String.valueOf(x)).append(",").append(String.valueOf(value)).append("\n");
                                writer.flush();
                            }
                        } catch(Exception ignored) {}
                    }
                    try {
                        Objects.requireNonNull(writer).close();
                    } catch (IOException ignore) {}

                    scanner.close();
                });
                thread.start();
            } else {
                chosenPort.closePort();
                portList.setEnabled(true);
                connectButton.setText("Connect");
                series.clear();
                x = 0;
            }
        });

        window.setVisible(true);
    }

    public static File getSaveFile(String fileName) throws IOException{
        Frame f = new Frame();
        File fileTmp = null;
        String fileNameTmp;

        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setAcceptAllFileFilterUsed(false);

        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV (*.csv)", "csv", "CSV");

        fileChooser.addChoosableFileFilter(filter);

        if (!Objects.equals(fileName, "")) {
            fileChooser.setSelectedFile(new File(fileName));
        }

        if (fileChooser.showSaveDialog(f) == JFileChooser.APPROVE_OPTION) {
            fileNameTmp = fileChooser.getSelectedFile().toString();

            if(!fileNameTmp.endsWith(".csv") && !fileNameTmp.endsWith(".CSV")) {
                fileNameTmp += ".csv";
            }

            fileTmp = new File(fileNameTmp);
            if (fileTmp.exists()) {
                int r = JOptionPane.showConfirmDialog(f, fileTmp.getName() + "이(가) 이미 있습니다. 바꾸시겠습니까?", "다른 이름으로 저장 확인", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.NO_OPTION) {
                    getSaveFile(fileNameTmp);
                }
            }

            CSVWriter writer = new CSVWriter(new FileWriter(fileTmp));
            String[] record = {"Time", "Value"};
            writer.writeNext(record);
            writer.close();
        }

        saveFileNameLabel.setText(Objects.requireNonNull(fileTmp).getName());

        return fileTmp;
    }
}
