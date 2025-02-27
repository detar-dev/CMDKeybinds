package net.kyrptonaught.cmdkeybind.MacroTypes;

import net.kyrptonaught.cmdkeybind.CmdKeybindMod;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public abstract class BaseMacro {
    public enum MacroType {
        Delayed, Repeating, SingleUse, DisplayOnly, ToggledRepeating
    }

    private InputUtil.Key primaryKey, modifierKey;
    protected String command;

    BaseMacro(String key, String keyMod, String command) {
        this.primaryKey = InputUtil.fromTranslationKey(key);
        this.modifierKey = InputUtil.fromTranslationKey(keyMod);
        this.command = command;

    }

    public void tick(long hndl, ClientPlayerEntity player, long currentTime) {
    }

    public boolean isDupeKeyModPressed(long hndl, InputUtil.Key testKey) {
        if (modifierKey.getCode() == -1) return false;
        if (testKey.getCode() == primaryKey.getCode())
            return isKeyTriggered(hndl, modifierKey);
        if (testKey.getCode() == modifierKey.getCode())
            return isKeyTriggered(hndl, primaryKey);
        return false;
    }

    private boolean findDupesWModPress(long hndl) {
        for (BaseMacro macro : CmdKeybindMod.activeMacros)
            if (macro.isDupeKeyModPressed(hndl, primaryKey))
                return true;
        return false;
    }

    protected boolean isTriggered(long hndl) {
        if (isKeyTriggered(hndl, primaryKey)) {
            if (modifierKey.getCode() != -1) {
                return isKeyTriggered(hndl, modifierKey);
            } else {
                return !findDupesWModPress(hndl);
            }
        }
        return false;
    }

    protected void execute(ClientPlayerEntity player) {
        player.sendChatMessage(this.command);
    }

    private static boolean isKeyTriggered(long hndl, InputUtil.Key key) {
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(hndl, key.getCode()) == GLFW.GLFW_PRESS;
        } else {
            return GLFW.glfwGetKey(hndl, key.getCode()) == GLFW.GLFW_PRESS;
        }
    }
}