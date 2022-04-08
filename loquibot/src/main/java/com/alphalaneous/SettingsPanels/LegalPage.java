package com.alphalaneous.SettingsPanels;

import com.alphalaneous.Components.SettingsPage;
import com.alphalaneous.Main;
import com.alphalaneous.Utilities;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;

public class LegalPage {

    private static final BufferedReader legalFileReader = new BufferedReader(
            new InputStreamReader(Objects.requireNonNull(
                    Main.class.getClassLoader().getResourceAsStream("legal.txt"))));

    public static JPanel createPanel() {
        SettingsPage settingsPage = new SettingsPage("$LEGAL_SETTINGS$");
        String text = Utilities.readIntoString(legalFileReader, true);
        settingsPage.addInput("", "", 20, null, text, false);

        return settingsPage;
    }
}
