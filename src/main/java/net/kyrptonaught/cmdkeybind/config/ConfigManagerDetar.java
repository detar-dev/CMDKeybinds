package net.kyrptonaught.cmdkeybind.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManagerDetar {
    private final File config_file;
    private final Path dir;
    private final Path config_path;
    public CmdConfig config;

    public ConfigManagerDetar(String mod_id)
    {
        config_file = new File(mod_id + ".json");
        dir = FabricLoader.getInstance().getConfigDir();
        config_path = dir.resolve(config_file.getPath());

        try {
            registerConfigFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save()
    {
        System.out.println("Saving CMDconfig");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Writer writer = Files.newBufferedWriter(Paths.get(String.valueOf(config_path)));
            gson.toJson(config, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load()
    {
        Gson gson = new Gson();
        try {
            config = gson.fromJson(new JsonReader(new FileReader(String.valueOf(config_path))), CmdConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerConfigFile() throws IOException
    {
        if (!Files.exists(config_path))
        {
            System.out.println("Creating config file for CMDKeybinds");
            Writer writer = Files.newBufferedWriter(Paths.get(String.valueOf(config_path)));
            CmdConfig tmp = new CmdConfig();
            ConfigOptions config = new ConfigOptions();
            config.macros.add(new ConfigOptions.ConfigMacro());
            tmp.profiles.put("default", config);
//
//            ConfigOptions config2 = new ConfigOptions();
//            config2.macros.add(new ConfigOptions.ConfigMacro());
//            tmp.profiles.put("dragoon", config2);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(tmp, writer);
            writer.close();
        }
    }
}

