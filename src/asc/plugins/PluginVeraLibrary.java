package asc.plugins;

import asc.*;
import asc.huffman.HTree;

import java.io.File;
import java.util.ArrayList;

import static asc.plugins.PluginStandardLibrary.huffmanCompress;
import static asc.huffman.HCode.*;

public class PluginVeraLibrary extends Plugin {

   @Override
   public boolean commandBlocks(CompilerState state) throws Exception {
      return false;
   }

   @Override
   public boolean functionNoParams(CompilerState state) throws Exception {
      switch (state.command) {
         case ".vera_sprites_on" -> {
            log(state.tab + Colors.WHITE_BOLD + ".vera_sprites_on");
            AssemblySubCompiler.compile(new CompilerState(loadASMCode("vera_sprites_on"), state.machineCode, null, state.tab));
            return true;
         }
         case ".vera_sprites_off" -> {
            log(state.tab + Colors.WHITE_BOLD + ".vera_sprites_off");
            AssemblySubCompiler.compile(new CompilerState(loadASMCode("vera_sprites_off"), state.machineCode, null, state.tab));
            return true;
         }
      }

      return false;
   }

   public boolean functionWithParams(CompilerState state) throws Exception {
      switch (state.command) {
         case ".vera_upload_init" -> {
            log(state.tab + Colors.WHITE_BOLD + ".vera_init");

            // .vera_init(bank, stride, address, data0/data1)
            // .vera_init(0, 2, $0000, 0)

            int bank = Integer.parseInt(state.params[0]);
            int temp = Integer.parseInt(state.params[1]);
            int autoIncrement = 0;
            switch (temp) {
               case 0 -> autoIncrement = 0;
               case 1 -> autoIncrement = 1;
               case 2 -> autoIncrement = 2;
               case 4 -> autoIncrement = 3;
               case 8 -> autoIncrement = 4;
               case 16 -> autoIncrement = 5;
               case 32 -> autoIncrement = 6;
               case 64 -> autoIncrement = 7;
               case 128 -> autoIncrement = 8;
               case 256 -> autoIncrement = 9;
               case 512 -> autoIncrement = 10;
               case 40 -> autoIncrement = 11;
               case 80 -> autoIncrement = 12;
               case 160 -> autoIncrement = 13;
               case 320 -> autoIncrement = 14;
               case 640 -> autoIncrement = 15;
               default -> {
                  log("auto increment value error, allowed values are 0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 40, 80, 160, 320, 640");
                  throw new Exception();
               }
            }
            autoIncrement = autoIncrement << 4; // offset to the right 4 bits
            int address = Integer.parseInt(state.params[2]);
            int data01 = Integer.parseInt(state.params[3]);

            if (!(data01 == 0 || data01 == 1)) throw new Exception("data0 or data1 allowed only.");

            AssemblySubCompiler.compile(new CompilerState(loadASMCode("vera_upload_init")
                    .replaceAll("VAR0", "" + (bank + autoIncrement))
                    .replaceAll("VAR1", "" + address)
                    .replaceAll("VAR2", "" + data01), state.machineCode, null, state.tab));

            return true;
         }
         case ".vera_upload" -> {
            log(state.tab + Colors.WHITE_BOLD + ".vera_upload");

            int address = Integer.parseInt(state.params[0]);
            int size = Integer.parseInt(state.params[1]);
            int data01 = Integer.parseInt(state.params[2]);

            if (!(data01 == 0 || data01 == 1)) throw new Exception("Use data0 or data1 only");
            if (size < 0) throw new Exception("Size must be positive.");

            if (size > 255) {
               AssemblySubCompiler.compile(new CompilerState(loadASMCode("vera_upload_a")
                       .replaceAll("VAR0", "" + address)
                       .replaceAll("VAR1", "" + data01) // todo - currently unused
                       .replaceAll("VAR2", "" + size), state.machineCode, null, state.tab));
            } else {
               AssemblySubCompiler.compile(new CompilerState(loadASMCode("vera_upload_b")
                       .replaceAll("VAR0", "" + address)
                       .replaceAll("VAR1", "" + data01)
                       .replaceAll("VAR2", "" + size), state.machineCode, null, state.tab));
            }

            return true;
         }
         case ".vera_decompress_upload" -> {
            log(state.tab + Colors.WHITE_BOLD + ".vera_decompress_upload");

            // inserts compressed data and a decoder directly into the code

            String itemName = state.params[0];
            int bank01 = Integer.parseInt(state.params[1]);

            File file = new File(workingDirectory + "bin/" + itemName + ".bin");
            ArrayList<Integer> loadedData = readBytesFromFile(file);

            log(state.tab + Colors.CYAN + "Decompress \"" + itemName + "\"", VERBOSE_EXTRA);
            int[] compressedBytes = huffmanCompress(loadedData, state.tab); // get the compressed bytes

            // build the Huffman decompression tree in 6510 assembler

            // we will assume that all our characters will have code less than 256, for simplicity
            int[] charFreqs = new int[256];
            for (int c : loadedData) charFreqs[c]++; // read each character and record the frequencies

            HTree tree = buildTree(charFreqs); // build tree

            // VAR0 - assembler.size()
            // VAR1 - address
            // VAR2 - mb.machineCode.length
            // VAR3 - buildAsmCode(tree, new StringBuffer(), "", output);

            // todo - needs testing / debugging
            String str = loadASMCode("vera_decompress_upload");
            assert str != null;
            String output = str
                    .replaceAll("VAR0", "" + state.machineCode.size())
                    // note due to the way the assembler is currently coded we need length + 1 here:
                    .replaceAll("VAR5", "" + ((loadedData.size() + 1) % 256))
                    .replaceAll("VAR6", "" + ((loadedData.size() + 1) / 256));

            StringBuilder asmString = new StringBuilder();
            buildAsmCode(tree, new StringBuffer(), "", asmString);
            output = output.replaceAll("VAR3", asmString.toString());

            StringBuilder data = new StringBuilder();
            for (int c1 = 0; c1 < compressedBytes.length - 1; c1++) data.append(compressedBytes[c1]).append(", ");
            data.append(compressedBytes[compressedBytes.length - 1]).append("\n");

            AssemblySubCompiler.compile(new CompilerState(output.replaceAll("VAR4", data.toString()), state.machineCode, null, state.tab));
            return true;
         }
         case ".vera_sprite_bytes" -> {
            log(state.tab + Colors.WHITE_BOLD + ".vera_sprite_byte");

            // converts the input 6 parameters to vera compatible sprite bytes
            //  .byte word($200), SPRITE_8BPP, word(160), word(120), SPRITE_DEPTH_FGD, SPRITE_SIZE_32X32
            //  .vera_sprite_bytes($200, SPRITE_8BPP, 160, 120, SPRITE_DEPTH_FGD, SPRITE_SIZE_32X32)

            //  .vera_upload_sprite(sprite0, num_sprites, data01)
            //  .sprite0
            //      // pointer (lo, hi), bpp, x, y, depth, width, height
            //      .byte $00, $00, 1, 255, 255, 3, 64, 64

            int address = Integer.parseInt(state.params[0]);
            int bpp = Integer.parseInt(state.params[1]);
            int xpos = Integer.parseInt(state.params[2]);
            int ypos = Integer.parseInt(state.params[3]);
            int depth = Integer.parseInt(state.params[4]);
            int size = Integer.parseInt(state.params[5]);

            int[] bytes = new int[8];

            // .vera_sprite_bytes(ptr id, bpp, x, y, depth, size)
            int[] depthValue = {0, 4, 8, 12};

            bytes[0] = address & 255; // lo byte address
            bytes[1] = ((address >>> 8) & 15); // hi byte address
            bytes[1] += (bpp == 8 ? 128 : 0);  // set the bpp bit
            bytes[2] = xpos & 255; // lo byte x pos
            bytes[3] = xpos >>> 8; // hi byte x pos
            bytes[4] = ypos & 255; // lo byte y pos
            bytes[5] = ypos >>> 8; // hi byte y pos
            bytes[6] = depthValue[depth]; // depth

            switch (size) {
               case 32 -> bytes[7] = 160;  // 32 x 32 sprite
               case 64 -> bytes[7] = 240;  // 64 x 64 sprite
               default -> bytes[7] = 160;
            }

            for (int x : bytes) state.machineCode.add(x);
            return true;
         }
         case ".vera_upload_sprite" -> {
            log(state.tab + Colors.WHITE_BOLD + ".vera_upload_sprite");

            //                        .vera_upload_sprite(sprite0, num_sprites, data01)
            //                                .sprite0
            //                                // pointer (lo, hi), bpp, x, y, depth, width, height
            //                                .byte $00, $00, 1, 255, 255, 3, 64, 64

            int address = Integer.parseInt(state.params[0]);
            int num = Integer.parseInt(state.params[1]);
            int data01 = Integer.parseInt(state.params[2]);

            if (!(data01 == 0 || data01 == 1)) {
               log("Use data0 or data1 only");
               throw new Exception();
            }

            if (num < 0) {
               log("Number must be positive.");
               throw new Exception();
            }

            if (num > 1) {
               log("TODO - IMPLEMENT >= 1 NUM SPRITE UPLOAD !");
               throw new Exception();
            }

            String str = loadASMCode("vera_upload_sprite");
            assert str != null;
            AssemblySubCompiler.compile(new CompilerState(str
                    .replaceAll("VAR0", "" + address)
                    .replaceAll("VAR1", "" + data01), state.machineCode, null, state.tab));
            return true;
         }
         case ".vera_video_mode" -> {
            log(state.tab + Colors.WHITE_BOLD + ".vera_video_mode");

            int scale = Integer.parseInt(state.params[0]);
            int hborder = Integer.parseInt(state.params[1]);
            int vborder = Integer.parseInt(state.params[2]);
            int color = Integer.parseInt(state.params[3]);

            String str = loadASMCode("vera_video_mode");
            assert str != null;
            AssemblySubCompiler.compile(new CompilerState(str
                    .replaceAll("VAR0", "" + scale)
                    .replaceAll("VAR1", "" + color)
                    .replaceAll("VAR2", "" + hborder)
                    .replaceAll("VAR3", "" + vborder), state.machineCode, null, state.tab));
            return true;
         }
         case ".vera_level_init" -> {
            log(state.tab + Colors.WHITE_BOLD + ".vera_level_init");

            // .vera_level_init(0, 256, 8, 40, 16, 20)
            // .vera_level_init(layer 0/1,
            //                  size 32/64/128/256,
            //                  bpp 2/4/8,
            //                  mapBaseAddress / 512,
            //                  tileSize 8 / 16,
            //                  tileBaseAddress / 2048);

            int layer = Integer.parseInt(state.params[0]);
            int mapSize = Integer.parseInt(state.params[1]);
            int bpp = Integer.parseInt(state.params[2]);
            int mapAddress512 = Integer.parseInt(state.params[3]);
            int tileSize = Integer.parseInt(state.params[4]);
            int tileAddress2048 = Integer.parseInt(state.params[5]);

            int layerFlags = 0;
            switch(layer) {
               case 0 -> layerFlags += 16;
               case 1 -> layerFlags += 32;
               default -> throw new Exception("Layer flags must be 0 or 1.");
            }

            int L0_CONFIG = 0;
            switch(mapSize) {
               case 32 -> L0_CONFIG += 0;
               case 64 -> L0_CONFIG += 64 + 16;
               case 128 -> L0_CONFIG += 128 + 32;
               case 256 -> L0_CONFIG += 128 + 64 + 32 + 16;
               default -> throw new Exception("Map size ,ust be 32, 64, 128 or 256");
            }
            switch(bpp) {
               case 1 -> L0_CONFIG += 0;
               case 2 -> L0_CONFIG += 1;
               case 4 -> L0_CONFIG += 2;
               case 8 -> L0_CONFIG += 1 + 2;
               default -> throw new Exception("BPP must be one of 1, 2, 4, or 8");
            }

            int L0_TILEBASE = tileAddress2048 << 2;
            switch(tileSize) {
               case 8 -> L0_TILEBASE += 0;
               case 16 -> L0_TILEBASE += 1 + 2;
               default -> throw new Exception("tile size must be 8 or 16");
            }

            int L0_MAPBASE;
            L0_MAPBASE = mapAddress512;
            String str = loadASMCode("vera_level_init_layer" + layer);
            assert str != null;
            AssemblySubCompiler.compile(new CompilerState(str
                    .replaceAll("LAYER_FLAG", "" + layerFlags)
                    .replaceAll("CONFIG", "" + L0_CONFIG)
                    .replaceAll("MAPBASE", "" + L0_MAPBASE)
                    .replaceAll("TILEBASE", "" + L0_TILEBASE), state.machineCode, null, state.tab));

            return true;
         }
      }

      return false;
   }
}
