package dev.psh0.asmsubcompiler.core;

import java.util.ArrayList;
import java.util.TreeMap;

public class CompilerState {
    public String program;
    public ArrayList<Integer> machineCode = new ArrayList<>();
    public TreeMap<String, Symbol> symbols = null;
    public String tab = "";
    public String tab2;
    public int cursor = 0;
    public String command = null;
    public String [] params = null;
    public String label;
    public String commandBlock;

    public CompilerState(String program) {
        this.program = program;
    }

    public CompilerState(String program, ArrayList<Integer> machineCode, TreeMap<String, Symbol> symbols, String tab) {
        this.program = program;
        this.machineCode = machineCode;
        this.symbols = symbols;
        this.tab = tab;
    }
}
