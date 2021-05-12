package net.kyrptonaught.cmdkeybind;

import com.mojang.brigadier.Command; import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.kyrptonaught.cmdkeybind.MacroTypes.*;
import net.kyrptonaught.cmdkeybind.config.ConfigManagerDetar;
import net.kyrptonaught.cmdkeybind.config.ConfigOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;


public class CmdKeybindMod implements ClientModInitializer {
    public static final String MOD_ID = "cmdkeybind-detar";
    public static List<BaseMacro> activeMacros = new ArrayList<>();
    public static List<String> userProfiles = new ArrayList<>();
    public static ConfigManagerDetar configManagerDetar = new ConfigManagerDetar(MOD_ID);
    public static String currentProfile;

    @Override
    public void onInitializeClient() {
        configManagerDetar.load();
        currentProfile = configManagerDetar.config.defaultProfile;

        if (configManagerDetar.config.profiles.get(currentProfile).macros.size() == 0)
        {
            addEmptyMacro();
        }

        userProfiles.addAll(configManagerDetar.config.profiles.keySet());
        buildMacros();

        // cmd help (default)
        // cmd list (list all profile)
        // cmd add <profile>
        // cmd remove <profile>
        // cmd set_default <profile>
        ClientCommandManager.DISPATCHER.register(
            ClientCommandManager.literal("cmd")
                        .then(ClientCommandManager.literal("add")
                            .then(ClientCommandManager.argument("profile", StringArgumentType.word())
                                    .executes(ctx ->
                                            addProfile(ctx.getSource().getPlayer(),
                                                    StringArgumentType.getString(ctx, "profile")))
                            )
                        )
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("profile", StringArgumentType.word())
                                        .executes(ctx ->
                                                removeProfile(ctx.getSource().getPlayer(),
                                                        StringArgumentType.getString(ctx, "profile")))
                                )
                        )
                        .then(ClientCommandManager.literal("change")
                                .then(ClientCommandManager.argument("profile", StringArgumentType.word())
                                        .executes(ctx ->
                                                changeProfile(ctx.getSource().getPlayer(),
                                                        StringArgumentType.getString(ctx, "profile")))
                                )
                        )
                        .then(ClientCommandManager.literal("set_default")
                                .then(ClientCommandManager.argument("profile", StringArgumentType.word())
                                        .executes(ctx ->
                                                setDefaultProfile(ctx.getSource().getPlayer(),
                                                        StringArgumentType.getString(ctx, "profile")))
                                )
                        )
                        .then(ClientCommandManager.literal("list")
                                .executes(context -> {
                                    ClientPlayerEntity player = context.getSource().getPlayer();
                                    player.sendSystemMessage(new LiteralText("All profiles: ").formatted(Formatting.GRAY), player.getUuid());
                                    for (String s : userProfiles)
                                    {
                                        if (s.equals(currentProfile))
                                        {
                                            continue;
                                        }
                                        player.sendSystemMessage(new LiteralText(String.format(" * %s", s)).formatted(Formatting.GRAY), player.getUuid());
                                    }
                                    player.sendSystemMessage(new LiteralText(String.format(" * %s", currentProfile)).formatted(Formatting.GREEN), player.getUuid());

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
        );

        // every tick check if a macro has been pressed
        ClientTickEvents.START_WORLD_TICK.register(clientWorld ->
        {
            if (MinecraftClient.getInstance().currentScreen == null) {
                long hndl = MinecraftClient.getInstance().getWindow().getHandle();
                long curTime = System.currentTimeMillis();
                for (BaseMacro macro : activeMacros)
                    macro.tick(hndl, MinecraftClient.getInstance().player, curTime);
            }
        });
    }


    public static void buildMacros() {
        activeMacros.clear();
        ConfigOptions options = configManagerDetar.config.profiles.get(currentProfile);
        if (configManagerDetar.config.enable)
        {
            for (ConfigOptions.ConfigMacro macro : options.macros)
            {
                if (macro.macroType == null)
                {
                    macro.macroType = BaseMacro.MacroType.SingleUse;
                }

                switch (macro.macroType)
                {
                    case Delayed:
                        activeMacros.add(new DelayedMacro(macro.keyName, macro.keyModName, macro.command, macro.delay));
                        break;
                    case Repeating:
                        activeMacros.add(new RepeatingMacro(macro.keyName, macro.keyModName, macro.command, macro.delay));
                        break;
                    case SingleUse:
                        activeMacros.add(new SingleMacro(macro.keyName, macro.keyModName, macro.command));
                        break;
                    case DisplayOnly:
                        activeMacros.add(new DisplayMacro(macro.keyName, macro.keyModName, macro.command));
                        break;
                    case ToggledRepeating:
                        activeMacros.add(new ToggledRepeating(macro.keyName, macro.keyModName, macro.command, macro.delay));
                        break;
                }
            }
        }
    }

    public static void addEmptyMacro() {
        configManagerDetar.config.profiles.get(currentProfile).macros.add(new ConfigOptions.ConfigMacro());
        buildMacros();
        configManagerDetar.save();
    }

    public static int addProfile(ClientPlayerEntity player, String profile_name)
    {
        if (!configManagerDetar.config.profiles.containsKey(profile_name))
        {
            userProfiles.add(profile_name);
            ConfigOptions config = new ConfigOptions();
            config.macros.add(new ConfigOptions.ConfigMacro());
            configManagerDetar.config.profiles.put(profile_name, config);

            player.sendSystemMessage(
                    new LiteralText(String.format("Added new profile: %s", profile_name)).formatted(Formatting.GRAY),
                    player.getUuid());

            configManagerDetar.save();
            return Command.SINGLE_SUCCESS;
        }
        else
        {
            player.sendSystemMessage(
                    new LiteralText(String.format("Profile: %s is already exists!", profile_name)).formatted(Formatting.RED),
                    player.getUuid());

            return 0;
        }
    }

    public static int removeProfile(ClientPlayerEntity player, String profile_name)
    {
        if (configManagerDetar.config.profiles.containsKey(profile_name))
        {
            if (profile_name.equals(configManagerDetar.config.defaultProfile))
            {
                player.sendSystemMessage(
                        new LiteralText("Cannot delete default profile").formatted(Formatting.RED),
                        player.getUuid());

                return 0;
            }
            userProfiles.remove(profile_name);

            configManagerDetar.config.profiles.remove(profile_name);
            configManagerDetar.save();

            player.sendSystemMessage(
                    new LiteralText(String.format("Removed profile: %s", profile_name)).formatted(Formatting.RED),
                    player.getUuid());

            if (profile_name.equals(currentProfile))
            {
                currentProfile = "default";
                player.sendSystemMessage(
                        new LiteralText(String.format("changing profile to: %s", currentProfile)).formatted(Formatting.GRAY),
                        player.getUuid());
                buildMacros();
            }

            return Command.SINGLE_SUCCESS;
        }
        else
        {
            player.sendSystemMessage(
                    new LiteralText(String.format("Profile: %s does not exists!", profile_name)).formatted(Formatting.RED),
                    player.getUuid());

            return 0;
        }
    }

    private int changeProfile(ClientPlayerEntity player, String profile_name)
    {
        if (configManagerDetar.config.profiles.containsKey(profile_name))
        {
            currentProfile = profile_name;
            player.sendSystemMessage(
                    new LiteralText(String.format("Changing profile to: %s", currentProfile)).formatted(Formatting.GREEN),
                    player.getUuid());
            buildMacros();

            return Command.SINGLE_SUCCESS;
        }
        else
        {
            player.sendSystemMessage(
                    new LiteralText(String.format("Profile: %s does not exists!", profile_name)).formatted(Formatting.RED),
                    player.getUuid());

            return 0;
        }
    }

    private int setDefaultProfile(ClientPlayerEntity player, String profile_name)
    {
        if (configManagerDetar.config.profiles.containsKey(profile_name))
        {
            configManagerDetar.config.defaultProfile = profile_name;
            player.sendSystemMessage(
                    new LiteralText(String.format("Profile %s is now the default profile", currentProfile)).formatted(Formatting.GREEN),
                    player.getUuid());
            configManagerDetar.save();

            return Command.SINGLE_SUCCESS;
        }
        else
        {
            player.sendSystemMessage(
                    new LiteralText(String.format("Profile: %s does not exists!", profile_name)).formatted(Formatting.RED),
                    player.getUuid());

            return 0;
        }
    }
}