package dev.psh0.asmsubcompiler.core;

import java.util.ArrayList;

public class Symbol {
   public enum Type { LABEL, DEFINITION, EXPRESSION };
   public Type type;
   public Integer value;
   public ArrayList<Usage> usages = new ArrayList<>(); // where is the label location used ?
   public boolean defined = false; // true when we come across a label definition

   public Symbol(Type type) {
      this.type = type;
   }
}
