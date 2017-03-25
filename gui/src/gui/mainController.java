package gui;

import gui.lib.Settingsmanager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.*;

import gui.dialogs.*;

public class mainController {

    // set this to false to safely stop the worker thread
    private boolean runThread = true;

    // contains our worker thread that gets the pictures from the pi
    private Thread thread;

    @FXML
    ImageView imgPreview;

    /**
     *  starts our worker thread
     */
    @FXML
    protected void btnStart_OnMouseClicked(){
        runThread=true;
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

    /**
     * gets pictures from the given server and displays them
     */
    Task<Integer> task = new Task<Integer>() {
        @Override protected Integer call() throws Exception {
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

                    //TODO: change value dynamically
                    Thread.sleep(3000);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    };
}

