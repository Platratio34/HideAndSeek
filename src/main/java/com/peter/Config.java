package com.peter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

public class Config {

    private static final String FILE_NAME = "hideAndSeek.json";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private File configFile;

    public float hideTime = 2 * 60f;

    public float[] hintTimes = new float[] {
            120, // testing
            5 * 60,
            8 * 60,
            10 * 60,
            12 * 60,
            14 * 60,
            16 * 60,
            18 * 60,
            20 * 60,
            22 * 60,
            24 * 60,
            26 * 60,
            28 * 60,
            30 * 60,
            32 * 60,
            34 * 60,
            36 * 60,
            38 * 60,
            40 * 60,
            42 * 60,
            44 * 60,
            46 * 60,
            48 * 60,
            50 * 60,
            52 * 60,
            54 * 60,
            56 * 60,
            58 * 60,
            60 * 60
    };

    public double centerX = 0.5;
    public double centerY = 100;
    public double centerZ = 0.5;

    public Config() {
        try {
            configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), FILE_NAME);
            if (!configFile.exists()) {
                configFile.createNewFile();
                save();
            }
        } catch (IOException e) {
            HideAndSeek.LOGGER.error("IO Error in opening config");
            HideAndSeek.LOGGER.error(e.toString());
        }
    }

    public void load() {
        try {
            FileReader reader = new FileReader(configFile);
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            reader.close();
            hideTime = obj.get("hideTime").getAsFloat();

            JsonArray jHintTimes = obj.getAsJsonArray("hintTimes");
            hintTimes = new float[jHintTimes.size()];
            for (int i = 0; i < jHintTimes.size(); i++) {
                hintTimes[i] = jHintTimes.get(i).getAsFloat();
            }

            JsonObject center = obj.getAsJsonObject("center");
            centerX = center.get("x").getAsDouble();
            centerY = center.get("y").getAsDouble();
            centerZ = center.get("z").getAsDouble();
        } catch (IOException e) {
            HideAndSeek.LOGGER.error("IO Error in loading config");
            HideAndSeek.LOGGER.error(e.toString());
        }
    }

    public void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("hideTime", hideTime);

        JsonArray jHintTimes = new JsonArray(hintTimes.length);
        for (int i = 0; i < hintTimes.length; i++) {
            jHintTimes.add(hintTimes[i]);
        }
        obj.add("hintTimes", jHintTimes);

        JsonObject center = new JsonObject();
        center.addProperty("x", centerX);
        center.addProperty("y", centerY);
        center.addProperty("z", centerZ);
        obj.add("center", center);

        try {
            FileWriter writer = new FileWriter(configFile);
            GSON.toJson(obj, writer);
            writer.close();
        } catch (IOException e) {
            HideAndSeek.LOGGER.error("IO Error in saving config");
            HideAndSeek.LOGGER.error(e.toString());
        }
    }
    
    public void debug() {
        HideAndSeek.LOGGER.info(String.format("hideTime %.1f", hideTime));
    }

}
