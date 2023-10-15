package dev.psh0.asmsubcompiler.huffman;

public abstract class HTree implements Comparable<HTree> {
   public final int freq;
   public HTree(int freq) { this.freq = freq; }
   public int compareTo(HTree tree) {
      return freq - tree.freq;
   }
}
