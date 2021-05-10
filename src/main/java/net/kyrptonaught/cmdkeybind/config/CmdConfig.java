package net.kyrptonaught.cmdkeybind.config;

import java.util.HashMap;

public class CmdConfig {
    public Boolean enable = true;
    public HashMap<String, ConfigOptions> profiles = new HashMap<>();
    public String defaultProfile = "default";
}
