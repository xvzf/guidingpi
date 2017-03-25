package gui;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import gui.lib.Settingsmanager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.*;

import gui.dialogs.*;
import javafx.stage.DirectoryChooser;

public class mainController {

    // set this to false to safely stop the worker thread
    private boolean runThread = true;

    // contains our worker thread that gets the pictures from the pi
    private Thread thread;

    // image counter used for debug reasons for now
    private int counter;
    private File selectedDirectory;

    @FXML
    ImageView imgPreview;

    @FXML
    Label lblTimestamp;

    /**
     *  starts our worker thread
     */
    @FXML
    protected void btnStart_OnMouseClicked(){
        if(thread!= null)
            thread.interrupt();
        thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * stops our worker thread
     */
    @FXML
    protected void btnStop_OnMouseClicked(){
        runThread=false;
    }

    /**
     * display setting dialog
     */
    @FXML
    protected void mItemConnectionSettings_OnAction(){
        connectionSettingsController settingsController = new connectionSettingsController(null);
        settingsController.showAndWait();
    }

    @FXML
     protected void mItemDebug500_onAction(){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Directory to save the images");
        selectedDirectory = chooser.showDialog(Main.primaryStage);
        counter = 100;
    }

    /**
     * gets pictures from the given server and displays them
     */
    Task<Integer> task = new Task<Integer>() {
        @Override protected Integer call() throws Exception {
            runThread=true; // ensures we don't stop before actually starting ;)
            while (runThread)
            {
                try {
                    Socket socket = new Socket(Settingsmanager.getPreference_String("IP"),Settingsmanager.getPreference_int("PortImages"));

                    InputStream in = socket.getInputStream();
                    OutputStream out = new FileOutputStream("temp.jpg");

                    byte[] bytes = new byte[32*1024]; // read only 32kByte at once

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
                    Platform.runLater(new Runnable(){
                        @Override
                        public void run() {
                            lblTimestamp.setText(date);
                        }
                    });

                    if(counter > 0)
                    {
                        counter--;
                        InputStream is = null;
                        OutputStream os = null;
                        try {
                            is = new FileInputStream("temp.jpg");
                            os = new FileOutputStream(selectedDirectory.getPath()+"\\"+date.replace(':','_')+".jpg");
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
                    Thread.sleep(3050);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    };
}

