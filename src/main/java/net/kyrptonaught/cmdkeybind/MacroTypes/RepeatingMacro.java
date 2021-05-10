package net.kyrptonaught.cmdkeybind.MacroTypes;

import net.minecraft.client.network.ClientPlayerEntity;

public class RepeatingMacro extends BaseMacro {
    private int delay;
    private long sysTimePressed = 0;
    private long currentTime;

    public RepeatingMacro(String key, String keyMod, String command, int delay) {
        super(key, keyMod, command);
        this.delay = delay;
    }

    @Override
    public void tick(long hndl, ClientPlayerEntity player, long currentTime) {
        this.currentTime = currentTime;
        if (isTriggered(hndl) && canExecute()) {
             execute(player);
        } else if (canExecute()){
            sysTimePressed = 0;
        }
    }

    private boolean canExecute() {
        if (sysTimePressed == 0)
        {
            return true;
        }

        return currentTime - sysTimePressed > delay;
    }

    protected void execute(ClientPlayerEntity player) {
        sysTimePressed = currentTime;
        super.execute(player);
    }
}
