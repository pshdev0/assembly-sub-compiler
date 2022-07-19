package asc.huffman;

import java.util.*;

public class HCode {

   public static void buildAsmCode(HTree tree, StringBuffer prefix, String tab, StringBuilder asm) {
      assert tree != null;

      /*

         // this is the structure of the decompression tree

         .node
            jsr readNeedle
            beq node1
            .node0
               lda #255// LEAF !
               jmp write
            .node1
               lda #0// LEAF !
               jmp write

       */

      asm.append(tab).append("node").append(prefix).append(":\n");
      if (tree instanceof HLeaf) {
         HLeaf leaf = (HLeaf)tree;

         // print out character, frequency, and code for this leaf (which is just the prefix)
         asm.append(tab).append("   ").append("lda #").append(((HLeaf) tree).value).append(" // LEAF !\n");
         asm.append(tab).append("   ").append("jmp write\n");
      } else if (tree instanceof HNode node) {
         tab += "   ";
         asm.append(tab).append("jsr readNeedle\n").append(tab).append("beq node").append(prefix).append("0\n").append(tab).append("jmp node").append(prefix).append("1\n");

         // traverse left
         prefix.append('0');
         buildAsmCode(node.left, prefix, tab + "   ", asm);
         prefix.deleteCharAt(prefix.length() - 1);

         // traverse right
         prefix.append('1');
         buildAsmCode(node.right, prefix, tab + "   ", asm);
         prefix.deleteCharAt(prefix.length() - 1);
      }
   }

   // input is an array of frequencies, indexed by character codeß
   public static HTree buildTree(int [] charFreqs) {
      PriorityQueue<HTree> trees = new PriorityQueue<>();
      for (int i = 0; i < charFreqs.length; i++) if (charFreqs[i] > 0) trees.offer(new HLeaf(charFreqs[i], i));
      assert trees.size() > 0;
      while (trees.size() > 1) { // loop until there is only one tree left
         HTree left = trees.poll(); // two trees with least frequency
         HTree right = trees.poll();
         assert right != null;
         trees.offer(new HNode(left, right)); // put into new node and re-insert into queue
      }
      return trees.poll();
   }

   public static void buildCodes(HTree tree, StringBuffer prefix, HashMap<Integer, String> codes) {
      assert tree != null;
      if (tree instanceof HLeaf leaf) {

         // print out character, frequency, and code for this leaf (which is just the prefix)
//         System.out.println(leaf.value + "\t" + leaf.frequency + "\t" + prefix);
         codes.put(leaf.value, prefix.toString());
      } else if (tree instanceof HNode node) {

         // traverse left
         prefix.append('0');
         buildCodes(node.left, prefix, codes);
         prefix.deleteCharAt(prefix.length() - 1);

         // traverse right
         prefix.append('1');
         buildCodes(node.right, prefix, codes);
         prefix.deleteCharAt(prefix.length() - 1);
      }
   }
}
