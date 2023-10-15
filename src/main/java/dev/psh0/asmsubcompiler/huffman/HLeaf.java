package dev.psh0.asmsubcompiler.huffman;

class HLeaf extends HTree {
   public final int value;
   public HLeaf(int freq, int val) {
      super(freq);
      value = val;
   }
}
