package gui.lib;

import java.util.prefs.Preferences;

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
public class Settingsmanager
{
    private static Preferences preferences = Preferences.userRoot();

    public static void setPreference_String(String key, String value)
    {
        preferences.put(key,value);
    }

    public static void setPreference_int(String key, int value)
    {
        preferences.putInt(key,value);
    }

    public static String getPreference_String(String key)
    {
        return preferences.get(key,"");
    }

    public static int getPreference_int(String key)
    {
        return preferences.getInt(key,-1);
    }
}
