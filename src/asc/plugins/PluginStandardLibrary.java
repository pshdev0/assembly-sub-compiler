package asc.plugins;

import asc.Plugin;
import asc.AssemblySubCompiler;
import asc.CompilerState;
import asc.Symbol;
import asc.huffman.HTree;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import static asc.huffman.HCode.*;

public class PluginStandardLibrary extends Plugin {

   public PluginStandardLibrary() {
      addAddressingMode("blank_space", "^[\\s\\n]*$", PluginStandardLibrary::blank_space);
      addAddressingMode("function()", "^\\s*(?<command>[a-zA-Z0-9._+-]+)\\(\\s*\\)\\s*$", PluginStandardLibrary::function_no_params);
      addAddressingMode("function(1, 2, ...)", "^\\s*(?<command>[a-zA-Z0-9._+-]+)\\((?<params>.*[^,x])\\)\\s*$", PluginStandardLibrary::function_with_params);
      addAddressingMode("command {...", "^(\\s*(?<command>[.+a-zA-Z0-9]+)\\s*\\{)", PluginStandardLibrary::command_block);
      addAddressingMode("label:","^\\s*(?<label>[a-zA-Z0-9]+):\\s*$", PluginStandardLibrary::label);
   }

   @Override
   public boolean commandBlocks(CompilerState state) throws Exception {
      switch (state.command) {
         case ".byte" -> {
            log(state.tab + ".byte " + state.commandBlock);
            ArrayList<Integer> rtn = new ArrayList<>();
            String [] values = state.commandBlock.split(",");
            for(String val : values) rtn.add(parseExpression(val, state.symbols));
            state.machineCode.addAll(rtn);
            return true;
         }
         case ".forever" -> {
            log(state.tab + Color.WHITE_BOLD + ".forever");
            int absPos = asmStart + state.machineCode.size();
            AssemblySubCompiler.compile(new CompilerState(state.commandBlock, state.machineCode, state.symbols, state.tab)); // compile the block
            state.machineCode.addAll(add(state.machineCode.size(), state.tab + "jmp " + absPos, "4c", lowByte(absPos), highByte(absPos)));
            return true;
         }
         case ".groovy" -> {
            state.commandBlock += "\n" + GROOVY_INLINE_CODE;
            log("Executing Groovy code.");

            Binding binding = new Binding();
            GroovyShell shell = new GroovyShell(binding);
            AssemblySubCompiler subCompiler = new AssemblySubCompiler();
            shell.setProperty("pixel", subCompiler);

            // we need to replace any define and labels with those known
            // note that any labels used must have already been defined above
            state.commandBlock = replaceKnownSymbols(state.commandBlock, state.symbols);

            try {
               int [] byteData = (int[]) shell.evaluate(String.valueOf(state.commandBlock));
               ArrayList<Integer> rtn = new ArrayList<>();
               for (int i : byteData) rtn.add(i);
               state.machineCode.addAll(rtn);
               return true;
            } catch (Exception e) {
               System.out.println("Groovy code processing error:");
               System.out.println(e.getMessage());
               System.out.println(e.getLocalizedMessage());
               for (int c1 = 0; c1 < Math.min(e.getStackTrace().length, 10); c1++)
                  System.out.println("      " + e.getStackTrace()[c1].toString());
            }

            return false;
         }
      }

      return false;
   }

   @Override
   public boolean functionNoParams(CompilerState state) throws Exception {
      switch (state.command) {
         case ".prgBasicEntry" -> {
            log(".basic_entry");
            /*

              -- .PRG FILE LOAD ADDRESS
                  N/A - N.A   : $01, $08 -> $0801; loading address for .PRG file data (not itself loaded)

              -- BASIC NEXT LINE POINTER
                  $0801-$0802 : $0C, $08 -> $080C; pointer to next line of BASIC code

              -- BASIC LINE 10 -> "10 SYS 2064"
                  $0803-$0804 : $0A, $00 -> $000A; BASIC line number (10)
                  $0805       : $9E -> BASIC "SYS" command
                  $0806-$080A : ASCII characters " 2064"
                  $080B       : null byte terminating BASIC line

              -- BASIC NEXT LINE POINTER
                  $080C-$080D : $00, $00 -> $0000: pointer to next line of BASIC code (null = end of program)
              --

              so on disk this would be:

                  $01, $08, $0C, $08, $0A, $00, $9E, $20, $32, $30, $36, $34, $0, $0, $0

              and only $0C, $08, ... would be loaded into memory starting from $0801

            */
            state.machineCode.addAll(add(state.machineCode.size(), ".byte", "00", "0C", "08", "0A", "00", "9E", "20", "32", "30", "36", "34", "00", "00"));
            return true;
         }
      }

      return false;
   }

   public boolean functionWithParams(CompilerState state) throws Exception {
      switch (state.command) {
         case ".export" -> {
            try {
               exportType = ExportType.valueOf(state.params[0]);
            }
            catch(Exception unused) {
               throw new Exception("Export type not recognised, use one of: " + ExportType.values());
            }

            if(exportType == ExportType.PRG && state.params.length > 1) {

               try {
                  int address = Integer.parseInt(convertHexToDecimal(state.params[1]));
                  exportHeaderLow = lowByte(address);
                  exportHeaderHigh = highByte(address);
               } catch (Exception unused) {
                  throw new Exception("Header value error.");
               }
            }
            else log("Using default header $0801.");

            log("Export details set ok.");
            return true;
         }
         case ".org" -> {
            String address = state.params[0];
            log(".org " + address);
            if (state.machineCode.size() == 0) asmStart = Integer.parseInt(address);
            else {
               int diff = Integer.parseInt(address) - (asmStart + state.machineCode.size());
               for(int c1 = 0; c1 < diff; c1++) state.machineCode.add(0); // fill with zeros
               if (diff < 0) throw new Exception("Cannot org to an earlier position.");
            }
            return true;
         }
         case ".halfword--" -> {
            log(state.tab + Color.WHITE_BOLD + ".halfword--");
            int address = Integer.parseInt(state.params[0]);
            int limit = Integer.parseInt(state.params[1]);

            String str = loadASMCode("word_dec");
            assert str != null;
            AssemblySubCompiler.compile(new CompilerState(str
                    .replaceAll("VAR0", "" + address)
                    .replaceAll("VAR1", "" + limit), state.machineCode, null, state.tab));
            return true;
         }
         case ".halfword++" -> {
            log(state.tab + Color.WHITE_BOLD + ".halfword++");
            int address = Integer.parseInt(state.params[0]);
            int limit = Integer.parseInt(state.params[1]);

            String str = loadASMCode("word_inc");
            assert str != null;
            AssemblySubCompiler.compile(new CompilerState(str
                    .replaceAll("VAR0", "" + address)
                    .replaceAll("VAR1", "" + limit), state.machineCode, null, state.tab));
            return true;
         }
         case ".sleep" -> {
            log(state.tab + Color.WHITE_BOLD + ".sleep");
            String str = loadASMCode("sleep");
            assert str != null;
            AssemblySubCompiler.compile(new CompilerState(str
                    .replaceAll("VAR0", "" + Integer.parseInt(state.params[0]))
                    .replaceAll("VAR1", "" + Integer.parseInt(state.params[1])), state.machineCode, null, state.tab));
            return true;
         }
         case ".decompress" -> {
            log(state.tab + ".decompress");

            // inserts compressed data and a decoder directly into the code

            String itemName = state.params[0];
            int address = Integer.parseInt(state.params[1]);

            File file = new File(workingDirectory + "bin/" + itemName + ".bin");
            ArrayList<Integer> loadedData = readBytesFromFile(file);

            log(Color.CYAN + "Decompress \"" + itemName + "\" to address " + address);
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
            String str = loadASMCode("decompress");
            assert str != null;
            String output = str.replaceAll("VAR0", "" + state.machineCode.size())
                    .replaceAll("VAR1", "" + address)
                    // note due to the way the assembler is currently coded we need length + 1 here:
                    .replaceAll("VAR5", "" + ((loadedData.size() + 1)% 256))
                    .replaceAll("VAR6", "" + ((loadedData.size() + 1) / 256));

            StringBuilder asmStr = new StringBuilder();
            buildAsmCode(tree, new StringBuffer(), "", asmStr);
            output = output.replaceAll("VAR3", asmStr.toString());

            StringBuilder data = new StringBuilder();
            for(int c1 = 0; c1 < compressedBytes.length - 1; c1++) data.append(compressedBytes[c1]).append(", ");
            data.append(compressedBytes[compressedBytes.length - 1]).append("\n");
            String rtn = output.replaceAll("VAR4", data.toString());
            AssemblySubCompiler.compile(new CompilerState(rtn, state.machineCode, null, state.tab));
            return true;
         }
         case ".insert_compressed" -> {
            log(state.tab + ".insert_compressed");

            String itemName = state.params[0];

            File file = new File(workingDirectory + "bin/" + itemName + "bin");
            ArrayList<Integer> loadedData = readBytesFromFile(file);

            int[] bytes = huffmanCompress(loadedData, state.tab);
            ArrayList<Integer> rtn = new ArrayList<>();
            for(int c1 = 0; c1 < bytes.length; c1++) rtn.add(bytes[c1]);

            state.machineCode.addAll(rtn);
            return true;
         }
         case ".insert_decompressor" -> {
            log(state.tab + ".insert_decompressor");

            // auto insert 6502 Huffman decompressor

            String itemName = state.params[0];

            File file = new File(workingDirectory + "bin/" + itemName + "bin");
            ArrayList<Integer> loadedData = readBytesFromFile(file);

            // this will output the Assembly Huffman decompression tree
            // it doesn't insert any code

            // we will assume that all our characters will have code less than 256, for simplicity
            int [] charFreqs = new int[256];
            for (int c : loadedData) charFreqs[c]++; // read each character and record the frequencies

            HTree tree = buildTree(charFreqs); // build tree
            StringBuilder output = new StringBuilder();
            buildAsmCode(tree, new StringBuffer(), "", output); // todo - add to asm here?

            AssemblySubCompiler.compile(new CompilerState(output.toString(), state.machineCode, null, state.tab));
            return true;
         }
         case ".define" -> {
            String symbol = state.params[0];
            int replaceWith = Integer.parseInt(state.params[1]);
            log(".define " + symbol + " := " + replaceWith);
            if (state.symbols.containsKey(symbol)) throw new Exception("Redefinition of symbol " + symbol + " error.");
            Symbol ll = new Symbol(Symbol.Type.DEFINITION);
            ll.value = replaceWith;
            ll.defined = true;
            state.symbols.put(symbol, ll); // symbol is defined, so no need to increase total
            return true;
         }
         default -> {
            return false;
         }
      }
   }

   public static int[] huffmanCompress(ArrayList<Integer> data, String tab) {
      // Huffman compress

      // we will assume that all our characters will have code less than 256, for simplicity
      int[] charFreqs = new int[256];
      for (int c : data) charFreqs[c]++; // read each character and record the frequencies

      HTree tree = buildTree(charFreqs); // build tree
      HashMap<Integer, String> codes = new HashMap<>();
      buildCodes(tree, new StringBuffer(), codes);

      // build the compressed bit stream
      String compressed = "";
      for(int c1 = 0; c1 < data.size(); c1++) compressed += codes.get(data.get(c1));

//      log("compressed bit stream: " + compressed);

      // generate the compressed bytes
      int[] compressedBytes = new int[compressed.length() / 8 + 1];
      for (int c1 = 0; c1 < compressed.length(); c1 += 8) {
         String bits = compressed.substring(c1, Math.min(compressed.length(), c1 + 8));
         while (bits.length() < 8) bits = bits + "0";
         compressedBytes[c1 / 8] = Integer.parseInt(bits, 2);
      }

      log(tab + Color.CYAN + "Compression ratio: " + compressed.length() / 8 + " / " + data.size() + " bytes = " + (1f - (compressed.length() / 8f) / data.size()) * 100 + "% saving !", VERBOSE_EXTRA);

      return compressedBytes;
   }

   public static void blank_space(CompilerState state) {
      state.cursor++;
   }

   public static void function_no_params(CompilerState state) throws Exception {
      int plugin = 0;
      for(Plugin p : plugins) plugin += (p.functionNoParams(state) ? 1 : 0);
      if(plugin == 0) throw new Exception("function() command (" + state.command + ") not found error.");
      else if(plugin != 1) throw new Exception("Multiple plugin commands (" + state.command + ") found error.");
   }

   public static void function_with_params(CompilerState state) throws Exception {
      int plugin = 0;
      for(Plugin p : plugins) plugin += (p.functionWithParams(state) ? 1 : 0);
      if(plugin == 0) throw new Exception("function(...) error, no function found (" + state.command + ")");
      else if(plugin != 1) throw new Exception("function(...) error, multiple functions found (" + state.command + ")");
   }

   public static void command_block(CompilerState state) throws Exception {
      // code hoover
      int cursorStart = state.cursor;
      int braceCounter = 1;
      String code = null;
      while(state.cursor < state.program.length()) {
         int open = state.program.indexOf("{", state.cursor);
         int close = state.program.indexOf("}", state.cursor);

         if(open != -1 && open < close) {
            braceCounter++;
            state.cursor = open + 1;
         }
         else if(close != -1) {
            braceCounter--;
            state.cursor = close + 1;
         }
         else throw new Exception("Scope delimiter error.");

         if(braceCounter == 0) {
            code = state.program.substring(cursorStart, close);
            break;
         }
      }
      if(code == null) throw new Exception("Scope code hoover error.");

      int plugin = 0;
      state.commandBlock = code;
      for(Plugin p : plugins) plugin += (p.commandBlocks(state) ? 1 : 0);
      if(plugin == 0) throw new Exception("Command block error, command not found (" + state.command + ")");
      else if(plugin != 1) throw new Exception("Command block error, multiple commands found (" + state.command + ")");
   }

   public static void label(CompilerState state) throws Exception {
      addLabel(state.label, state.symbols, state.machineCode.size());
      log(state.tab + Color.GREEN_BOLD + state.label + ":");
   }
}
