package org.hotamachisubaru.miniutility;

import org.bukkit.plugin.java.JavaPlugin;

public final class MiniutilityLoader extends JavaPlugin {

    private Miniutility miniutility;

    @Override
    public void onLoad() {
        miniutility = new Miniutility(this);
    }

    @Override
    public void onEnable() {
        if (miniutility == null) {
            miniutility = new Miniutility(this);
        }
        miniutility.enable();
    }

    @Override
    public void onDisable() {
        if (miniutility != null) {
            miniutility.disable();
        }
    }

    public Miniutility getMiniutility() {
        return miniutility;
    }
}
