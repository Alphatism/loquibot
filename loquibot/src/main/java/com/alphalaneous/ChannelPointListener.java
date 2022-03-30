package com.alphalaneous;

import com.alphalaneous.FileUtils.FileList;
import com.alphalaneous.FileUtils.GetInternalFiles;
import com.alphalaneous.FileUtils.InternalFile;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

public class ChannelPointListener extends WebSocketClient {


	private boolean pingSuccess = false;

	ChannelPointListener(URI serverURI) {
		super(serverURI);
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		while (true) {
			try {
				send("{\n" +
						"  \"type\": \"LISTEN\",\n" +
						"  \"data\": {\n" +
						"    \"topics\": [\"channel-points-channel-v1." + APIs.getUserID() + "\"],\n" +
						"    \"auth_token\": \"" + Settings.getSettings("oauth").asString() + "\"\n" +
						"  }\n" +
						"}");
				System.out.println("> Connected to Channel Points");
				break;

			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			Utilities.sleep(1000);
		}
		new Thread(() -> {
			while (true) {
				send("{\n" +
						"  \"type\": \"PING\"\n" +
						"}");
				pingSuccess = false;
				Utilities.sleep(300000);
				if (!pingSuccess) {
					send("{\n" +
							"  \"type\": \"RECONNECT\"\n" +
							"}");
				}
			}
		}).start();
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		System.out.println("closed with exit code " + code + " additional info: " + reason);
	}

	@Override
	public void onMessage(String message) {
		JSONObject object = new JSONObject(message);
		String event = object.get("type").toString().replaceAll("\"", "");
		if (event.equalsIgnoreCase("PONG")) {
			pingSuccess = true;
		}
		if (event.equalsIgnoreCase("MESSAGE")) {
			String topic = object.getJSONObject("data").get("topic").toString().replaceAll("\"", "");

			if (topic.startsWith("channel-points-channel-v1")) {
				System.out.println(message);
				String redemptionA = object.getJSONObject("data").get("message").toString().replaceAll("\\\\\"", "\"").replaceAll("\r", "").replaceAll("\n", "");
				System.out.println(redemptionA);
				String redemption = new JSONObject(redemptionA).getJSONObject("data").getJSONObject("redemption").getJSONObject("reward").get("title").toString().replaceAll("\"", "");
				String username = new JSONObject(redemptionA).getJSONObject("data").getJSONObject("redemption").getJSONObject("user").get("login").toString().replaceAll("\"", "");
				System.out.println(username);
				boolean isUserinput = new JSONObject(redemptionA).getJSONObject("data").getJSONObject("redemption").getJSONObject("reward").getBoolean("is_user_input_required");
				String userInput = "";
				if (isUserinput) {
					userInput = new JSONObject(redemptionA).getJSONObject("data").getJSONObject("redemption").get("user_input").toString().replaceAll("\"", "");
					System.out.println(redemption + " redeemed by " + username + " with " + userInput);
				} else {
					System.out.println(redemption + " redeemed by " + username);
				}
				String finalUserInput = userInput;
				try {
					boolean comExists = false;
					Path comPath = Paths.get(Defaults.saveDirectory + "/loquibot/points/");
					if (Files.exists(comPath)) {
						Stream<Path> walk1 = Files.walk(comPath, 1);
						for (Iterator<Path> it = walk1.iterator(); it.hasNext(); ) {
							Path path = it.next();
							String[] file = path.toString().split("\\\\");
							String fileName = file[file.length - 1];
							if (fileName.equalsIgnoreCase(redemption + ".js")) {
								comExists = true;
								new Thread(() -> {
									try {
										while (BotHandler.processing) {
											Utilities.sleep(50);
										}
										Main.sendMessage(Command.run(username, finalUserInput, Files.readString(path, StandardCharsets.UTF_8)));
									} catch (Exception e) {
										e.printStackTrace();
									}
								}).start();
							}
						}
					}
					if (!comExists) {
						GetInternalFiles getInternalFiles = new GetInternalFiles("points/");
						FileList files = getInternalFiles.getFiles();
						for (InternalFile file : files) {
							if (file.getName().equalsIgnoreCase(redemption + ".js")) {
								Main.sendMessage(Command.run(username, finalUserInput, file.getString()));
								break;
							}
						}
					}
				} catch (Exception ignored) {
				}

			}
		}
	}

	@Override
	public void onMessage(ByteBuffer message) {
		System.out.println("received ByteBuffer");
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}

	void disconnectBot() {
		try {
			send("{\n" +
					"  \"type\": \"UNLISTEN\"\n" +
					"}");
		} catch (WebsocketNotConnectedException ignored) {

		}
	}

	public void reconnectBot() {
		send("{\n" +
				"  \"type\": \"LISTEN\",\n" +
				"  \"data\": {\n" +
				"    \"topics\": [\"channel-points-channel-v1." + APIs.getUserID() + "\"],\n" +
				"    \"auth_token\": \"" + Settings.getSettings("oauth").asString() + "\"\n" +
				"  }\n" +
				"}");
	}
}