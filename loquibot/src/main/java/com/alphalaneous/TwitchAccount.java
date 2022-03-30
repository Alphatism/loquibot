package com.alphalaneous;

import com.alphalaneous.Windows.DialogBox;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class TwitchAccount {

	public static String broadcaster_type;
	public static String offline_image_url;
	public static String description;
	public static String created_at;
	public static String profile_image_url;
	public static BufferedImage profileImage;
	public static String id;
	public static String login;
	public static String display_name;
	public static String type;
	public static long view_count;


	public static void setInfo() {
		JSONObject data = APIs.getInfo();


		APIs.setUser(Settings.getSettings("channel").asString());

		broadcaster_type = data.getString("broadcaster_type");
		offline_image_url = data.getString("offline_image_url");
		description = data.getString("description");
		created_at = data.getString("created_at");
		profile_image_url = data.getString("profile_image_url");
		id = data.getString("id");
		login = data.getString("login");
		display_name = data.getString("display_name");
		type = data.getString("type");
		view_count = data.getLong("view_count");
		try {
			profileImage = ImageIO.read(new URL(profile_image_url));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}