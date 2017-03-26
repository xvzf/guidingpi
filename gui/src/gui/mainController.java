package gui;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import gui.lib.CommandBuilder;
import gui.lib.CommandID;
import gui.lib.Settingsmanager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.*;

import gui.dialogs.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * GUIDINGPI
 * <p>
 * Copyright (C) 2017 Matthias Riegler <matthias@xvzf.tech>
 * Copyright (C) 2017 Dominik  Laa     <dominik@xvzf.tech>
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
public class mainController extends Stage{

    // set this to false to safely stop the worker thread
    private boolean runThread = true;

    // contains our worker thread that gets the pictures from the pi
    private Thread thread;

    // image counter used for debug reasons for now
    private int counter;

    // directory where the images should be saved (debug images)
    private File selectedDirectory;

    private int iso = 100;

    @FXML
    ImageView imgPreview;

    @FXML
    Label lblTimestamp;

    @FXML
    Slider sliderISO;

    @FXML
    Label lblISO;


    /**
     * starts our worker thread
     */
    @FXML
    protected void btnStart_OnMouseClicked() {
        // TODO: This code only works once
        if (thread != null)
            thread.interrupt();
        thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * stops our worker thread
     */
    @FXML
    protected void btnStop_OnMouseClicked() {
        runThread = false;
    }

    /**
     * display setting dialog
     */
    @FXML
    protected void mItemConnectionSettings_OnAction() {
        connectionSettingsController settingsController = new connectionSettingsController(null);
        settingsController.showAndWait();
    }

    /**
     * display directory selection and start
     * saving files afterwards
     */
    @FXML
    protected void mItemDebug500_onAction() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Directory to save the images");
        selectedDirectory = chooser.showDialog(Main.primaryStage);
        counter = 100;
    }

    /**
     * send settings over the control port
     */
    @FXML
    protected void btnApplySettings_OnAction()
    {
        try {
            Socket socket = new Socket(Settingsmanager.getPreference_String("IP"), Settingsmanager.getPreference_int("PortControl"));
            CommandBuilder.sendCommandParam(socket, CommandID.SetISOMode,iso);

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * receive settings from the control port
     */
    @FXML
    protected void btnGetSettings_OnAction()
    {
        try {
            Socket socket = new Socket(Settingsmanager.getPreference_String("IP"), Settingsmanager.getPreference_int("PortControl"));
            iso = CommandBuilder.sendAndReceive(socket,CommandID.GetISOMode);

            lblISO.setText(""+iso);

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize gui
     */
    @FXML
    public void initialize() {

        // add change listener for the iso slider
        sliderISO.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                lblISO.setText(""+ new_val.intValue()*100);
                iso=new_val.intValue()*100;
            }
        });
    }

    /**
     * gets pictures from the given server and displays them
     */
    Task<Integer> task = new Task<Integer>() {
        @Override
        protected Integer call() throws Exception {
            runThread = true; // ensures we don't stop before actually starting ;)
            while (runThread) {
                try {
                    Socket socket = new Socket(Settingsmanager.getPreference_String("IP"), Settingsmanager.getPreference_int("PortImages"));

                    InputStream in = socket.getInputStream();
                    OutputStream out = new FileOutputStream("temp.jpg");

                    byte[] bytes = new byte[32 * 1024]; // read only 32kByte at once

                    int count;
                    while ((count = in.read(bytes)) > 0) {
                        out.write(bytes, 0, count);
                    }

                    out.close();
                    in.close();
                    socket.close();

                    Image image = new Image("file:temp.jpg");
                    imgPreview.setImage(image);

                    Metadata metadata = ImageMetadataReader.readMetadata(new File("temp.jpg"));
                    ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                    String date = directory.getString(ExifSubIFDDirectory.TAG_IMAGE_UNIQUE_ID);

                    // set new text (threadsafe)
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            lblTimestamp.setText(date);
                        }
                    });

                    if (counter > 0) {
                        counter--;
                        InputStream is = null;
                        OutputStream os = null;
                        try {
                            is = new FileInputStream("temp.jpg");
                            os = new FileOutputStream(selectedDirectory.getPath() + "\\" + date.replace(':', '_') + ".jpg");
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = is.read(buffer)) > 0) {
                                os.write(buffer, 0, length);
                            }
                        } finally {
                            is.close();
                            os.close();
                        }
                    }


                    // TODO: change value dynamically
                    // value should be a little bit longer than the pi needs
                    // from one image to another, to ensure we don't get the
                    // same image two times (and the new image 3 seconds to late)
                    // TODO: add logic that ensure we get the most actual image every time
                    Thread.sleep(2200);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    };
}

