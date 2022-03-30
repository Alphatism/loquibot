package com.alphalaneous;


import com.alphalaneous.Panels.LevelsPanel;
import com.alphalaneous.Tabs.RequestsTab;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.*;

public class StreamDeckSocket extends WebSocketServer {

    private static final int portNumber = 19236;

    public StreamDeckSocket() {
        super(new InetSocketAddress(portNumber));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {

    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        System.out.println("> Stream Deck: " + s);
        LevelData data = RequestsTab.getRequest(RequestsUtils.getSelection()).getLevelData();

        switch (s.toLowerCase()){
            case "next":
                RequestFunctions.skipFunction();
                break;
            case "random":
                RequestFunctions.randomFunction();
                break;
            case "undo":
                RequestFunctions.undoFunction();
                break;
            case "toggle":
                RequestFunctions.requestsToggleFunction();
                break;
            case "opened":
                Main.sendMessageToStreamDeck(RequestsUtils.getInfoObject(data).toString());
                break;
            case "block_id":
                RequestFunctions.blockFunction(true);
                break;
            case "clear":
                RequestFunctions.clearFunction(true);
                break;
            case "youtube":
                if(data.getYoutubeURL() != null) {
                    try {
                        Utilities.openURL(new URI(data.getYoutubeURL()));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            default:
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onStart() {

    }

    public void sendMessage(String message){
        new Thread(() -> broadcast(message)).start();
    }
}
