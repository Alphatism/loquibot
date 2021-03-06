package com.alphalaneous;

import com.alphalaneous.ChatBot.ServerBot;
import com.alphalaneous.Interfaces.Function;
import com.alphalaneous.Services.GeometryDash.LoadGD;
import com.alphalaneous.Services.GeometryDash.RequestFunctions;
import com.alphalaneous.Services.GeometryDash.Requests;
import com.alphalaneous.Services.GeometryDash.RequestsUtils;
import com.alphalaneous.Images.Assets;
import com.alphalaneous.Interactive.ChannelPoints.ChannelPointData;
import com.alphalaneous.Interactive.ChannelPoints.LoadPoints;
import com.alphalaneous.Interactive.Commands.Command;
import com.alphalaneous.Interactive.Commands.CommandData;
import com.alphalaneous.Interactive.Commands.LoadCommands;
import com.alphalaneous.Interactive.Keywords.KeywordData;
import com.alphalaneous.Interactive.Keywords.LoadKeywords;
import com.alphalaneous.Interactive.MediaShare.MediaShare;
import com.alphalaneous.Interactive.Timers.LoadTimers;
import com.alphalaneous.Interactive.Timers.TimerData;
import com.alphalaneous.Interactive.Timers.TimerHandler;
import com.alphalaneous.Interactive.Variables;
import com.alphalaneous.Running.CheckIfRunning;
import com.alphalaneous.Running.LoquibotSocket;
import com.alphalaneous.Services.Twitch.TwitchAPI;
import com.alphalaneous.Settings.Account;
import com.alphalaneous.Settings.ChannelPoints;
import com.alphalaneous.Settings.Outputs;
import com.alphalaneous.Settings.SettingsHandler;
import com.alphalaneous.Settings.Logs.LoggedID;
import com.alphalaneous.Swing.Components.LevelDetailsPanel;
import com.alphalaneous.Swing.Components.VideoDetailsPanel;
import com.alphalaneous.Services.Twitch.TwitchChatListener;
import com.alphalaneous.Services.YouTube.YouTubeChatListener;
import com.alphalaneous.Services.YouTube.YouTubeAccount;
import com.alphalaneous.Tabs.*;
import com.alphalaneous.Services.Twitch.TwitchAccount;
import com.alphalaneous.Services.Twitch.TwitchListener;
import com.alphalaneous.Theming.Themes;
import com.alphalaneous.Utils.*;
import com.alphalaneous.Utils.KeyListener;
import com.alphalaneous.Windows.*;
import com.alphalaneous.Windows.Window;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    static {
        BackwardsCompatibilityLayer.setNewLocation();
        LogWindow.createWindow();
    }

    public static boolean programLoaded = false;
    public static boolean sendMessages = false;
    public static boolean allowRequests = false;
    public static Thread keyboardHookThread;

    private static TwitchListener channelPointListener;
    private static TwitchChatListener chatReader;
    private static ServerBot serverBot = null;
    private static boolean failed = false;
    private static final ArrayList<Image> iconImages = new ArrayList<>();
    private static final ImageIcon icon = Assets.loquibot;
    private static final Image newIcon16 = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
    private static final Image newIcon32 = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
    private static final JFrame starting = new JFrame("loquibot");
    private static ConnectorSocket streamDeckSocket;


    public static void main(String[] args) throws IOException {

        new Thread(() -> {
            Utilities.sleep(21600000);
            if(SettingsHandler.getSettings("runAtStartup").asBoolean() && !Window.getWindow().isVisible()) {
                restart();
            }
        }).start();


        SettingsHandler.loadSettings();

        boolean reopen = SettingsHandler.getSettings("hasUpdated").asBoolean();
        SettingsHandler.writeSettings("hasUpdated", "false");

        if(SettingsHandler.getSettings("channel").exists() && !SettingsHandler.getSettings("twitchEnabled").exists()){
            SettingsHandler.writeSettings("twitchEnabled", "true");
        }

        FindLoquibot.setup();
        FindLoquibot.findPath();

        try {
            URI originalURI = new URI("ws://127.0.0.1:18562");
            CheckIfRunning checkIfRunning = new CheckIfRunning(originalURI);
            checkIfRunning.connectBlocking();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        new Thread(new LoquibotSocket()).start();

        iconImages.add(newIcon16);
        iconImages.add(newIcon32);

        Defaults.setSystem(false);

        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
        new Thread(Main::runKeyboardHook).start();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        starting.setSize(200, 200);
        starting.setResizable(false);
        starting.setLocationRelativeTo(null);
        starting.setUndecorated(true);
        starting.setBackground(new Color(0, 0, 0, 0));
        starting.add(new JLabel(Assets.loquibotLarge));
        starting.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        starting.setIconImages(iconImages);
        starting.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        starting.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Main.close();
            }
        });


        if(!SettingsHandler.getSettings("runAtStartup").asBoolean() || reopen) starting.setVisible(true);

        System.out.println("> Start");

        //Save defaults of UI Elements before switching to Nimbus
        //Sets to Nimbus, then sets defaults back
        setUI();
        LoadGD.load();
        Themes.loadTheme();

        System.out.println("> Settings Loaded");

        if (SettingsHandler.getSettings("onboarding").exists()) {
            try {
                TwitchAccount.setInfo();
                new Thread(ChannelPoints::refresh).start();
                try {
                    if (SettingsHandler.getSettings("youtubeEnabled").asBoolean()) YouTubeAccount.setCredential(false);
                }
                catch (Exception e){
                    YouTubeAccount.setCredential(true);
                }
                YouTubeAccount.setInfo();
            } catch (Exception e) {
                e.printStackTrace();
                if(SettingsHandler.getSettings("twitchEnabled").asBoolean()) TwitchAPI.setOauth();
                TwitchAccount.setInfo();
                new Thread(ChannelPoints::refresh).start();
                try {
                    if (SettingsHandler.getSettings("youtubeEnabled").asBoolean()) YouTubeAccount.setCredential(false);
                }
                catch (Exception f){
                    YouTubeAccount.setCredential(true);
                }
                YouTubeAccount.setInfo();
            }
        }
        System.out.println("> Twitch Loaded");
        try {
            try {
                Language.loadLanguage();
                Language.startFileChangeListener();
            }
            catch (IllegalArgumentException e){
                System.out.println("> Language Change Listener Failed");
            }

            Assets.loadAssets();
            new Thread(Defaults::startMainThread).start();
            new Thread(streamDeckSocket = new ConnectorSocket()).start();

            System.out.println("> Main Threads Started");

            Window.initFrame();
            CommandEditor.createPanel();
            RequestsTab.createPanel();
            MediaShareTab.createPanel();
            ChatbotTab.createPanel();
            SettingsTab.createPanel();
            OfficerWindow.create();
            Window.loadTopComponent();
            LoadCommands.loadCommands();
            LoadTimers.loadTimers();
            LoadPoints.loadPoints();
            LoadKeywords.loadKeywords();
            LoggedID.loadLoggedIDs();
            TimerHandler.startTimerHandler();

            LevelDetailsPanel.setPanel(null);
            VideoDetailsPanel.setPanel(null);

            new JFXPanel(); //Initialize JavaFX Graphics Toolkit (Hacky Solution)

            Platform.setImplicitExit(false);
            Platform.runLater(MediaShare::init);

            System.out.println("> Panels Created");

            UpdateChecker.checkForUpdates();
            Window.loadSettings();
            Defaults.initializeThemeInfo();
            Themes.refreshUI();


            starting.setVisible(false);

            //If first time launch, the user has to go through onboarding
            //Show it and wait until finished

            if (!SettingsHandler.getSettings("onboarding").exists()) {
                Onboarding.createPanel();
                Window.setVisible(true);
                System.out.println("> Window Visible");

                Onboarding.refreshUI();
                Onboarding.isLoading = true;
                while (Onboarding.isLoading) {
                    Utilities.sleep(100);
                }
                TwitchAccount.setInfo();
                YouTubeAccount.setInfo();
                if(SettingsHandler.getSettings("youtubeEnabled").asBoolean()) Account.refreshYouTube(YouTubeAccount.name);
                new Thread(ChannelPoints::refresh).start();
            }
            else {
                if(!SettingsHandler.getSettings("runAtStartup").asBoolean() || reopen) Window.setVisible(true);
                System.out.println("> Window Visible");
            }



            new Thread(Variables::loadVars).start();
            System.out.println("> Command Variables Loaded");

            new Thread(() -> {
                serverBot = new ServerBot();
                serverBot.connect();
            }).start();

            if(SettingsHandler.getSettings("youtubeEnabled").asBoolean()){
                new Thread(() -> YouTubeChatListener.startChatListener(null)).start();
            }

            if(SettingsHandler.getSettings("twitchEnabled").asBoolean()) {
                new Thread(() -> {
                    chatReader = new TwitchChatListener(TwitchAccount.login);
                    chatReader.connect(SettingsHandler.getSettings("oauth").asString(), TwitchAccount.login);
                }).start();
                new Thread(() -> {
                    try {
                        channelPointListener = new TwitchListener(new URI("wss://pubsub-edge.twitch.tv"));
                        channelPointListener.connect();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }).start();
            }


            Window.setOnTop(SettingsHandler.getSettings("onTop").asBoolean());
            try {
                Outputs.setOutputStringFile(RequestsUtils.parseInfoString(SettingsHandler.getSettings("outputString").asString()));
            }
            catch (Exception e){
                SettingsHandler.writeSettings("outputFileLocation", Paths.get(Defaults.saveDirectory + "\\loquibot").toString());
                //OutputSettings.setOutputStringFile(RequestsUtils.parseInfoString(Settings.getSettings("outputString").asString(), 0));
            }
            Path initialJS = Paths.get(Defaults.saveDirectory + "\\loquibot\\initial.js");
            if (Files.exists(initialJS)) {
                new Thread(() -> {
                    try {
                        if (!Files.readString(initialJS, StandardCharsets.UTF_8).equalsIgnoreCase("")) {
                            Command.run(TwitchAccount.display_name, true, true, new String[]{"dummy"}, Files.readString(initialJS, StandardCharsets.UTF_8), 0, null, -1);
                        }
                    } catch (Exception ignored) {
                    }
                }).start();
            } else {
                Files.createFile(initialJS);
            }

            Path file = Paths.get(Defaults.saveDirectory + "\\loquibot\\saved.json");
            if (Files.exists(file)) {
                String levelsJson = Files.readString(file, StandardCharsets.UTF_8);
                try {
                    JSONObject object = new JSONObject(levelsJson);
                    Requests.loadLevels(object);
                }
                catch (Exception ignored){
                }
            }
            allowRequests = true;
            RequestFunctions.saveFunction();
            RequestsTab.getLevelsPanel().setSelect(0);
            new Thread(TwitchAPI::checkViewers).start();

            sendMessages = true;
            if(SettingsHandler.getSettings("twitchEnabled").asBoolean()) {
                TwitchAPI.setAllViewers();

                SettingsHandler.writeSettings("isMod", String.valueOf(TwitchAPI.isLoquiMod()));

                if (!SettingsHandler.getSettings("isHigher").exists()) {
                    if (TwitchAPI.allMods.contains("loquibot") || TwitchAPI.allVIPs.contains("loquibot"))
                        SettingsHandler.writeSettings("isHigher", "true");
                    else SettingsHandler.writeSettings("isHigher", "false");
                }
            }
            programLoaded = true;
            if(!TwitchChatListener.sentStartupMessage) {
                Main.sendMessage(Utilities.format("???? | $STARTUP_MESSAGE$"));
                TwitchChatListener.sentStartupMessage = true;
            }
            startSaveLoop();

        } catch (Exception e) {
            e.printStackTrace();
            DialogBox.showDialogBox("Error!", e + ": " + e.getStackTrace()[0], "Please report to Alphalaneous#9687 on Discord.", new String[]{"Close"});
            close(true, false);
        }
    }

    public static void sendMessageConnectedService(String message){
        if(streamDeckSocket != null) {
            streamDeckSocket.sendMessage(message);
        }
    }

    public static void restart(){
        try {
            Main.forceClose();
            Runtime.getRuntime().exec(SettingsHandler.getSettings("installPath").asString());
            System.exit(0);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ArrayList<Image> getIconImages() {
        return iconImages;
    }

    public static void sendMainMessage(String message) {
        TwitchChatListener.getCurrentListener().sendMessage(message);
    }

    public static void sendToServer(String message) {
        try {
            ServerBot.getCurrentServerBot().sendMessage(message);
        } catch (Exception ignored) {
        }
    }

    public static void sendMessageWithoutCooldown(String message){
        if(SettingsHandler.getSettings("twitchEnabled").asBoolean()) {
            JSONObject messageObj = new JSONObject();
            messageObj.put("request_type", "send_message");
            messageObj.put("message", message);
            ServerBot.getCurrentServerBot().sendMessage(messageObj.toString());
        }
    }

    public static void sendMessage(String messageA, boolean whisper, String user) {
        if(SettingsHandler.getSettings("twitchEnabled").asBoolean()) {

            String[] messages = messageA.split("??");
            for (String message : messages) {

                if (!SettingsHandler.getSettings("silentMode").asBoolean() || message.equalsIgnoreCase(" ")) {
                    if (!message.equalsIgnoreCase("")) {

                        JSONObject messageObj = new JSONObject();
                        messageObj.put("request_type", "send_message");
                        if (SettingsHandler.getSettings("antiDox").asBoolean()) {
                            message = Language.uwuify(message.replaceAll(System.getProperty("user.name"), "*****"));
                        }
                        if (whisper) {
                            messageObj.put("message", "/w " + user + " " + message);
                        } else {
                            messageObj.put("message", message);
                        }
                        try {
                            ServerBot.getCurrentServerBot().sendMessage(messageObj.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (whisper) {
                    if (!message.equalsIgnoreCase("")) {
                        JSONObject messageObj = new JSONObject();
                        messageObj.put("request_type", "send_message");
                        if (SettingsHandler.getSettings("antiDox").asBoolean()) {
                            message = message.replaceAll(System.getProperty("user.name"), "*****");
                        }
                        messageObj.put("message", "/w " + user + " " + message);
                        try {
                            ServerBot.getCurrentServerBot().sendMessage(messageObj.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static void sendYTMessage(String messageA){
        if(SettingsHandler.getSettings("youtubeEnabled").asBoolean()){
            String[] messages = messageA.split("??");
            for(String message : messages) {

                if (!SettingsHandler.getSettings("silentMode").asBoolean() || message.equalsIgnoreCase(" ")) {
                    if (!message.equalsIgnoreCase("")) {

                        JSONObject messageObj = new JSONObject();
                        messageObj.put("request_type", "send_yt_message");
                        messageObj.put("liveChatId", YouTubeAccount.liveChatId);

                        if (SettingsHandler.getSettings("antiDox").asBoolean()) {
                            message = Language.uwuify(message.replaceAll(System.getProperty("user.name"), "*****"));
                        }
                        messageObj.put("message", message);

                        if(YouTubeAccount.liveChatId != null) {
                            try {
                                ServerBot.getCurrentServerBot().sendMessage(messageObj.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public static void sendMessage(String message) {
        System.out.println(message);
        sendMessage(message, false, null);
    }
    public static void sendMessage(String message, boolean doAnnounce) {
        if(SettingsHandler.getSettings("twitchEnabled").asBoolean()) {
            SettingsHandler.writeSettings("isMod", String.valueOf(TwitchAPI.isLoquiMod()));
            if (doAnnounce && SettingsHandler.getSettings("isMod").asBoolean() && !message.startsWith("/"))
                sendMessage("/announce " + message, false, null);
            else sendMessage(message, false, null);
        }
    }

    private static void runKeyboardHook() {
        var runHookRef = new Object() {
            boolean runHook = true;
        };
        if (keyboardHookThread != null) {
            if (keyboardHookThread.isAlive()) {
                runHookRef.runHook = false;
            }
        }
        try {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.unregisterNativeHook();
            }
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new KeyListener());
            while (GlobalScreen.isNativeHookRegistered()) {
                Utilities.sleep(100);
            }
        } catch (Exception e) {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException e1) {
                e1.printStackTrace();
            }
            failed = true;
        }
        keyboardHookThread = new Thread(() -> {
            while (runHookRef.runHook) {
                if (failed) {
                    runKeyboardHook();
                }
                Utilities.sleep(100);
            }
        });
        keyboardHookThread.start();
    }

    public static void save(){
        try {
            Variables.saveVars();
            SettingsHandler.saveSettings();
            Themes.saveTheme();
            CommandData.saveCustomCommands();
            CommandData.saveDefaultCommands();
            CommandData.saveGeometryDashCommands();
            CommandData.saveMediaShareCommands();
            TimerData.saveCustomTimers();
            KeywordData.saveCustomKeywords();
            ChannelPointData.saveCustomPoints();
            LoggedID.saveLoggedIDs();
        }
        catch (Exception e){
            System.exit(0);
        }
    }

    public static void startSaveLoop(){
        new Thread(() -> {
            while(true){
                save();
                Utilities.sleep(10000);
            }
        }).start();
    }

    public static void close(boolean forceLoaded, boolean load) {
        boolean loaded = Main.programLoaded;

        if (forceLoaded) {
            loaded = load;
        }
        Main.save();
        if(!SettingsHandler.getSettings("runAtStartup").asBoolean()) {
            Utilities.disposeTray();
            if (!SettingsHandler.getSettings("onboarding").asBoolean() && loaded) {
                forceClose();
            }
            System.exit(0);
        }
        else {
            Window.setVisible(false);
        }
    }

    public static void forceClose(){
        Main.save();
        Utilities.disposeTray();
        Window.setVisible(false);
        Window.setSettings();
        try {
            if(TwitchListener.getCurrentTwitchListener() != null) {
                TwitchListener.getCurrentTwitchListener().disconnectBot();
            }
            if(TwitchChatListener.getCurrentListener() != null) {
                TwitchChatListener.getCurrentListener().disconnect();
            }
            if(ServerBot.getCurrentServerBot() != null) {
                ServerBot.getCurrentServerBot().disconnect();
            }
            GlobalScreen.unregisterNativeHook();
        } catch (Exception e) {
            System.out.println("Failed closing properly, forcing close");
            e.printStackTrace();
            System.exit(0);
        }
    }


    public static void setUI() {
        HashMap<Object, Object> defaults = new HashMap<>();
        for (Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) {
            if (entry.getKey().getClass() == String.class && ((String) entry.getKey()).startsWith("ProgressBar")) {
                defaults.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) {
            if (entry.getKey().getClass() == String.class && ((String) entry.getKey()).startsWith("ToolTip")) {
                defaults.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) {
            if (entry.getKey().getClass() == String.class && ((String) entry.getKey()).startsWith("MenuItem")) {
                defaults.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) {
            if (entry.getKey().getClass() == String.class && ((String) entry.getKey()).startsWith("ScrollBar")) {
                defaults.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<Object, Object> entry : defaults.entrySet()) {
            UIManager.getDefaults().put(entry.getKey(), entry.getValue());
        }


        System.setProperty("sun.awt.noerasebackground", "true");
        UIManager.put("Menu.selectionBackground", Color.RED);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("Menu.background", Color.WHITE);
        UIManager.put("Menu.foreground", Color.BLACK);
        UIManager.put("Menu.opaque", false);
        UIManager.put("ToolTipManager.enableToolTipMode", "allWindows");

    }

    public static JFrame getStartingFrame(){
        return starting;
    }

    public static void close() {
        close(false, false);
    }
}
