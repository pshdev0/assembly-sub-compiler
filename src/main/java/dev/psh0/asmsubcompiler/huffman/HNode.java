package dev.psh0.asmsubcompiler.huffman;

class HNode extends HTree {
   public final HTree left, right;
   public HNode(HTree left, HTree right) {
      super(left.freq + right.freq);
      this.left = left;
      this.right = right;
   }
}
