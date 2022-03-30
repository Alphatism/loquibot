package com.alphalaneous.SettingsPanels;

import com.alphalaneous.Components.CurvedButton;
import com.alphalaneous.Components.ListButton;
import com.alphalaneous.Components.ListView;
import com.alphalaneous.Defaults;
import com.alphalaneous.LoggedID;
import com.alphalaneous.Windows.DialogBox;
import com.alphalaneous.Windows.Window;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class RequestsLog {

    private static final ListView listView = new ListView("$LOGGED_IDS_SETTINGS$");
    private static final Path file = Paths.get(Defaults.saveDirectory + "\\loquibot\\requestsLog.txt");

    public static JPanel createPanel(){

        listView.addButton("\uF0CE", () -> new Thread(() -> {
            String option = DialogBox.showDialogBox("$CLEAR_LOGS_DIALOG_TITLE$", "$CLEAR_LOGS_DIALOG_INFO$", "", new String[]{"$YES$", "$NO$"});

            if (option.equalsIgnoreCase("YES")) {
                LoggedID.removeAll();
                listView.clearElements();
            }
        }).start());
        return listView;
    }

    public static void clear(){
        listView.clearElements();
    }

    public static void loadIDs(){
        new Thread(() -> {
            for(LoggedID loggedID : LoggedID.getLoggedIDS()){
                listView.addElement(createButton(String.valueOf(loggedID.getID())));
            }
        }).start();
    }
    public static CurvedButton createButton(String text){
        ListButton button = new ListButton(text, 80);
        button.addActionListener(e -> new Thread(() -> {
            String option = DialogBox.showDialogBox("$REMOVE_LOG_DIALOG_TITLE$", "$REMOVE_LOG_DIALOG_INFO$", "", new String[]{"$YES$", "$NO$"}, new Object[]{button.getLText()});

            if (option.equalsIgnoreCase("YES")) {
                LoggedID.removeID(Integer.parseInt(button.getLText()));
                listView.removeElement(button);
            }
        }).start());
        return button;
    }
}