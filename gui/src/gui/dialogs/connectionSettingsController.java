package gui.dialogs;

import gui.lib.Settingsmanager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


/**
 * GUIDINGPI
 *
 *   Copyright (C) 2017 Matthias Riegler <matthias@xvzf.tech>
 *   Copyright (C) 2017 Dominik  Laa     <dominik@xvzf.tech>
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */
public class connectionSettingsController extends Stage implements Initializable {
    @FXML
    private TextField txtIP;

    @FXML
    private TextField txtPortImages;

    @FXML
    private TextField txtPortControl;

    public connectionSettingsController(Parent parent)
    {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("connectionSettings.fxml"));
        fxmlLoader.setController(this);
        try
        {
            setScene(new Scene((Parent) fxmlLoader.load()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * check the values, when they are okay save them and close the dialog
     */
    @FXML
    protected void btnOK_OnAction()
    {
        String ip = txtIP.getText();
        String portimages = txtPortImages.getText();
        String portcontrol = txtPortControl.getText();

        try {
            int pimages = Integer.parseInt(portimages);
            int pcontrol = Integer.parseInt(portcontrol);

            if (validIP(ip)) {
                Settingsmanager.setPreference_String("IP", ip);
                Settingsmanager.setPreference_int("PortControl", pcontrol);
                Settingsmanager.setPreference_int("PortImages", pimages);
            }
        }
        catch (NumberFormatException e)
        {

        }

        // close the dialog
        super.close();
    }

    /**
     * Loads the current values from the settings into our dialog
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        txtIP.setText(Settingsmanager.getPreference_String("IP"));
        txtPortControl.setText(""+Settingsmanager.getPreference_int("PortControl"));
        txtPortImages.setText(""+Settingsmanager.getPreference_int("PortImages"));
    }

    /**
     * Tests the given string if it is an ip address
     * http://stackoverflow.com/questions/4581877/validating-ipv4-string-in-java
     */
    private static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

}
