/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wms.jarfx;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

/**
 *
 * @author mstemen
 */
class jarFXListHandler implements ChangeListener<String> {

    JarFXController myController = null;

    jarFXListHandler(JarFXController controller) {
        myController = controller;
    }

    public void setController(JarFXController controller) {
        myController = controller;
    }

    jarFXListHandler() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void changed(ObservableValue<? extends String> ov, String t, String t1) {
        this.myController.ShowZipInfo();
    }

}

class jarFXMouseHandler implements EventHandler<Event> {

    JarFXController myController = null;

    jarFXMouseHandler(JarFXController controller) {
        myController = controller;
    }

    public void setController(JarFXController controller) {
        myController = controller;
    }

    jarFXMouseHandler() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handle(Event evt) {
        MouseEvent me = null;
        if (MouseEvent.class.isInstance(evt)) {
            me = MouseEvent.class.cast(evt);
        }
        if (me == null) {
            return;
        }
        System.out.println("Event is: " + evt.getEventType());
        System.out.println("Event source is: " + evt.getSource());
        if (me.getButton() == MouseButton.PRIMARY) {
            if (me.getClickCount() == 2) {
                myController.ExpandClassViaMouse();
            } else {
                myController.ShowZipInfo();
            }
        }

    }
}

public class JarFXController implements Initializable {

    // InputStream theFile = new FileInputStream(args[0]);             
    @FXML
    private Label label;
    @FXML
    private Button setJarPathButton;
    @FXML
    private TitledPane mainPane;
    @FXML
    private TextField jarPathValueDisplay;
    @FXML
    private ListView<String> jarListView;
    @FXML
    private TextField classSizeField;
    @FXML
    private TextField classDateField;
    @FXML
    private Tab classSourceTab;
    @FXML
    private TabPane jarContentTabPane;
    @FXML
    private Tab classeListTab;
    @FXML
    private Tab imageTab;
    @FXML
    private AnchorPane imagePane;
    @FXML
    private ImageView jarImageView;

    private ArrayList<Tab> sourceTabs = null;

    private File jarFile;
    ObservableList<String> items = null;
    HashMap<String, ZipEntry> zipEntryMap = new HashMap<>();
    private ZipFile classZipFile = null;

    private String jadPath = "C:\\WMS_Dev\\";

    @FXML
    public void setJarPath(ActionEvent ae) {
        System.out.println("Click");

        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Java Class Jars (*.jar)", "*.jar");
        fileChooser.getExtensionFilters().add(extFilter);

        //Show open <span class="adtext" id="adtext_1">file dialog</span>
        jarFile = fileChooser.showOpenDialog(null);

        //Open directory from existing directory
        if (jarFile != null) {
            File existDirectory = jarFile.getParentFile();
            fileChooser.setInitialDirectory(existDirectory);
        }
        // labelFile.setText(file.getPath());
        if (jarFile != null) {
            jarPathValueDisplay.setText(jarFile.getPath());
            this.mainPane.setVisible(true);
            this.mainPane.setText("Jar Contents for: " + jarFile.getName());
        }
        try {
            unjarAndDisplay(jarFile);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(JarFXController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JarFXController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void ExpandClassViaMouse() {
        String selected = this.jarListView.getSelectionModel().getSelectedItem();
        ZipEntry zipEntry = this.zipEntryMap.get(selected);
        // test the zip entry to see if it is a class or image
        String name = zipEntry.getClass().getName();
        System.out.println("Class name is: " + name);
        explodeAndWrite(selected, zipEntry);
        String classExploded = readInClass(selected);
        System.out.println("Code is: " + classExploded);
        if (classExploded.length() > 0) {

            // first see if this source listing is already in the tabPane, test the ArrayList of Tabs
            Iterator<Tab> it = this.sourceTabs.iterator();
            boolean tabExists = false;
            // String targetTabName = "Source for: " + selected;
            String targetTabName = selected;

            Tab tabThatWasFound = null;

            while (!tabExists && it.hasNext()) {
                Tab testTab = it.next();
                if (testTab.getText().equals(targetTabName)) {
                    tabExists = true;
                    tabThatWasFound = testTab;
                }
            }
            if (tabExists) {
                // all we want to do is focus the found tab
                SingleSelectionModel<Tab> selectionModel = this.jarContentTabPane.getSelectionModel();
                if (selectionModel != null) {
                    selectionModel.select(tabThatWasFound);
                }
            } else {

                //create a new tab, and a textpane, then add the source to the new pane
                Tab newTab = new Tab();
                //newTab.setText("Source for: " + selected);
                newTab.setText(selected);
                TextArea newTextArea = new TextArea();
                newTextArea.setText(classExploded);
                ScrollPane scrollPane = new ScrollPane();
                double taheight = this.jarListView.getHeight();
                double tawidth = this.jarListView.getWidth();
                newTextArea.setPrefHeight(taheight);
                newTextArea.setPrefWidth(tawidth);
                scrollPane.setPrefSize(tawidth, taheight);
//                System.out.println("dynamic tab width is: " + tawidth);
//                System.out.println("dyamic tab height is: " + taheight);
//                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
//                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPane.setContent(newTextArea);
                newTab.setContent(scrollPane);
                this.jarContentTabPane.setPrefSize(tawidth, taheight);
                this.jarContentTabPane.getTabs().add(newTab);
                SingleSelectionModel<Tab> selectionModel = this.jarContentTabPane.getSelectionModel();
                if (selectionModel != null) {
                    selectionModel.select(newTab);
                }
            }
//            this.jarContentTabPane
//            this.classSourceTab.setText("Source for: " + selected);
//            this.classTextArea.setText(classExploded);
//            SingleSelectionModel<Tab> selectionModel = this.jarContentTabPane.getSelectionModel();
//            if (selectionModel != null) {
//                selectionModel.select(classSourceTab);
//            }

        }
        // explodeAndWriteFile(selected, zipEntry);
    }

    protected void ShowZipInfo() {
        String selected = this.jarListView.getSelectionModel().getSelectedItem();
        ZipEntry zipEntry = this.zipEntryMap.get(selected);
        setSelectedZipInfo(zipEntry);
    }

    protected String readInClass(String className) {

        String cmd = jadPath + "jad";
        ProcessBuilder pb = new ProcessBuilder(cmd, "-p", className);

        System.out.println(" Command Run was: " + pb.toString());
        StringBuilder codeSb = new StringBuilder();

        try {
            Process process = pb.start();
            if (process != null) {
                InputStream in = process.getInputStream();
                int byteRead;
                while ((byteRead = in.read()) != -1) {
                    codeSb.append((char) byteRead);
                }
            }
        } catch (Exception err) {
            System.out.println("Got Error: " + err.getMessage());

            System.out.println("path to jad is curently: " + jadPath);
        }
        System.out.println("JAD path contained JAD...");
        return codeSb.toString();
    }

    protected void explodeAndWrite(String className, ZipEntry entry) {
        try {
            byte[] data = new byte[300000];
            int byteRead;

            BufferedOutputStream bout = null;
            int fLen = (int) entry.getSize();
            InputStream in = classZipFile.getInputStream(entry);
            byteRead = 0;
            data = new byte[fLen];
            // now either write an image or a class...
            boolean isImage = (entry.getName().toLowerCase().contains("jpg") || entry.getName().toLowerCase().contains("png"));
            if (isImage) {
                Image image = null;
                BufferedImage bfi = ImageIO.read(in);
                WritableImage wr = null;
                if (bfi != null) {
                    wr = new WritableImage(bfi.getWidth(), bfi.getHeight());
                    PixelWriter pw = wr.getPixelWriter();
                    for (int x = 0; x < bfi.getWidth(); x++) {
                        for (int y = 0; y < bfi.getHeight(); y++) {
                            pw.setArgb(x, y, bfi.getRGB(x, y));
                        }
                    }
                    ScrollPane scrollPane = new ScrollPane();

                    imagePane.getChildren().clear();
                    image = SwingFXUtils.toFXImage(bfi, null);
                    if (image != null) {
                        jarImageView = new ImageView(wr);
                        jarImageView.setImage(image);
                        imagePane.getChildren().add(jarImageView);
                     // this.imageTab.setContent(scrollPane);
                        // focus the image tab
                        SingleSelectionModel<Tab> selectionModel = this.jarContentTabPane.getSelectionModel();
                        if (selectionModel != null) {
                            selectionModel.select(imageTab);
                        }
                    }

                }

            } else {
                while ((byteRead = in.read(data, 0, fLen)) != -1) {
                    this.writeBinaryFile(data, className);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeBinaryFile(byte[] aBytes, String aFileName) throws IOException {
        Path path = Paths.get(aFileName);
        Files.write(path, aBytes); //creates, overwrites
    }

//    protected void explodeAndWriteFile(String className, ZipEntry entry) {
//        String fileName = className;
//        int c;
//        InputStreamReader in = null;
//        BufferedReader br = null;
//        int byteRead;
//        BufferedOutputStream bout = null;
//
//        char[] data = new char[1000];
//
//        try {
//            // ZipInputStream zin = new ZipInputStream( new BufferedInputStream( )
//            // bout = new OutputStreamWriter(new FileOutputStream(fileName));
//            in = new InputStreamReader(classZipFile.getInputStream(entry), "UTF-8");
//            br = new BufferedReader(in);
//        } catch (IOException ex) {
//            Logger.getLogger(JarFXController.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        try {
//            FileWriter fstream = new FileWriter(fileName);
//            BufferedWriter fbw = new BufferedWriter(fstream);
//
//            while ((byteRead = in.read(data, 0, 1000)) != -1) {
//                //bout.write(data, 0, byteRead);
//            }
//
//            fbw.close();
//        } catch (IOException ex) {
//            Logger.getLogger(JarFXController.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//    }
    protected void setSelectedZipInfo(ZipEntry entry) {
        if( entry == null )
            return;
        SimpleDateFormat sdf = new SimpleDateFormat();
        this.classSizeField.setText(String.valueOf(entry.getSize()));
        this.classDateField.setText(sdf.format(new Date(entry.getTime())));
    }

    private void unjarAndDisplay(File fileToUnJar) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(fileToUnJar.getPath());
        ZipInputStream zis = new ZipInputStream(fis);
        this.classZipFile = new ZipFile(fileToUnJar.getPath());
        ZipEntry entry;

        this.clearJarList();
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().toLowerCase().contains("jpg") || entry.getName().toLowerCase().contains("png")) {
                System.out.println("Got image: " + entry.getName());

            } else if (!entry.getName().contains("class")) {
                continue;
            }

            StringBuilder sb = new StringBuilder("");
            String className = this.getClassName(entry.getName());
            // sb.append( className ).append(",").append(entry.getSize());
            sb.append(className);
            zipEntryMap.put(className, entry);
            // System.out.println(sb.toString());
            items.add(sb.toString());
            this.jarListView.getBaselineOffset();
            // consume all the data from this entry
            while (zis.available() > 0) {
                zis.read();
            }
            // I could close the entry, but getNextEntry does it automatically
            // zis.closeEntry()
            jarListView.setItems(items);
        }

    }

    protected void clearJarList() {
        if (items != null) {
            items.clear();
            zipEntryMap.clear();
            items = null;
        }
        items = jarListView.<String>getItems();
        jarListView.setItems(items);
    }

    private ZipEntry getEntry(String key) {
        ZipEntry foundEntry = null;
        if (this.zipEntryMap != null) {
            foundEntry = this.zipEntryMap.get(key);
        }

        return foundEntry;
    }

    private String getClassName(String entry) {
        String retValue = null;
        if (entry != null) {
            String[] tokens = entry.split("/");
            // we want the last token
            retValue = tokens[tokens.length - 1];

        }
        return retValue;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sourceTabs = new ArrayList<Tab>();
        jarListView.setOnMouseClicked(new jarFXMouseHandler(this));
        jarListView.getSelectionModel().selectedItemProperty().addListener(new jarFXListHandler(this));
//        jarListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
//            public void changed(ObservableValue<? extends String> observable,
//                    String oldValue, String newValue) {
//                System.out.println("ListView selection changed (new Value: " + newValue + "\n");
//                
//            }
//        });
//        // TODO         
//        jarListView = new ListView(FXCollections.observableList(Arrays.asList("one", "2", "3")));
//        jarListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handleClick(MouseEvent event )
//            {
//                
//            }
//        }

///    });
    }
}
