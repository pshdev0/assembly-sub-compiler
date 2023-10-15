// VAR*3 - buildAsmCode(tree, new StringBuffer(), "", output);
// VAR*4 - compressed data
// VAR*5 - mb.machineCode.length % 256
// VAR*6 - mb.machineCode.length / 256

// Huffman decompressor

ldx #0                        // load the bit needle

// decompress
nodeRoot:
   inc counter
   lda #0
   cmp counter
   bne+
   {
      inc counter+1
   }

   lda counter
   cmp #VAR5      // lo max counter
   bne+
   {
      lda counter+1
      cmp #VAR6   // hi max counter
      bne+
      {
         jmp finishDecompress
      }
   }

VAR3

write:
   sta $9f23 // upload A to vera !
   jmp nodeRoot

// read the contents of the needle and test if the current bit is set or not
readNeedle:
   lda readNeedleSelfModify:compressedData
   and bits, x
   cmp #0
   php // we want to remember Z processor status

   // increment the needle
   inx
   cpx #8
   bne+
   {
      ldx #0
      clc
      .halfword++(readNeedleSelfModify, $FFFF) // we can read a max of 65536 bytes
   }

   plp // restore z processor status
   rts

// decompression read needle
counter:
   .byte { 0, 0 }

bits:
   .byte { 128, 64, 32, 16, 8, 4, 2, 1 }

compressedData:
   .byte { VAR4 }

finishDecompress:
clc
