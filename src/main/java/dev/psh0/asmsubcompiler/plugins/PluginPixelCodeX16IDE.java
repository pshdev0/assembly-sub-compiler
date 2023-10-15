package dev.psh0.asmsubcompiler.plugins;

import dev.psh0.asmsubcompiler.core.Plugin;
import dev.psh0.asmsubcompiler.core.CompilerState;

import java.io.File;

public class PluginPixelCodeX16IDE extends Plugin {

    @Override
    public boolean commandBlocks(CompilerState state) throws Exception {
        return false;
    }

    @Override
    public boolean functionNoParams(CompilerState state) throws Exception {
        return false;
    }

    @Override
    public boolean functionWithParams(CompilerState state) throws Exception {
        if (".image".equals(state.command)) {
            String itemName = state.params[0];
            File file = new File(workingDirectory + "images/" + itemName);
            state.machineCode.addAll(readBytesFromFile(file));
            return true;
        }

        return false;
    }
}
