package dev.psh0.asmsubcompiler.core;

public class Usage {
   public Integer pc;
   public Integer line;

   public Usage(int _pc) {
      pc = _pc; // the program counter for this usage
      line = Plugin.compilerOutput.size() - 1;
   }
}
