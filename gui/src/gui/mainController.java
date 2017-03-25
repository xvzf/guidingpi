package gui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.*;

public class mainController {

    Thread thread;

    @FXML
    ImageView imgPreview;

    @FXML
    protected void btnStart_OnMouseClicked(){
        thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    Task<Integer> task = new Task<Integer>() {
        @Override protected Integer call() throws Exception {
            int iterations;
            while (true)
            {
                try {
                    Socket socket = new Socket("192.168.1.236",8001);

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

                    Thread.sleep(3000);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}

