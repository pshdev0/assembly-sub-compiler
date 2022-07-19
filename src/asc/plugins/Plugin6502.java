package asc.plugins;

import asc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class Plugin6502 extends Plugin {

   static final int INFILL_REL = -100000;
   static final int INFILL_ABS = -200000;
   static final int INFILL_IMM = -300000;
   static final int INFILL_IND = -400000;

   static HashMap<String, String> immediateHashmap = new HashMap<>();
   static HashMap<String, String> impliedHashmap = new HashMap<>();
   static HashMap<String, String> zeroPageHashmap = new HashMap<>();
   static HashMap<String, String> zeroPageXHashmap = new HashMap<>();
   static HashMap<String, String> absoluteHashmap = new HashMap<>();
   static HashMap<String, String> absoluteXHashmap = new HashMap<>();
   static HashMap<String, String> absoluteYHashmap = new HashMap<>();
   static HashMap<String, String> relativeHashmap = new HashMap<>();
   static HashMap<String, String> indirectHashmap = new HashMap<>();
   static HashMap<String, String> bIndirectXBHashmap = new HashMap<>();
   static HashMap<String, String> bIndirectBYHashmap = new HashMap<>();
   static HashMap<String, String> accumulatorHashmap = new HashMap<>();

   public Plugin6502() throws Exception {

      addAddressingMode("immediate","^\\s*(?<command>[a-z]*)\\s*#(?<params>[a-zA-Z0-9+\\-\\/*()]+)$", Plugin6502::immediate);
      addAddressingMode("implied; accumulator","^\\s*(?<command>[a-zA-Z]+)\\s*$", Plugin6502::imp_acc);
      addAddressingMode("zero_page; absolute; relative","^\\s*(?<command>[a-zA-Z]+)(?!\\s*$)\\s+(?<params>[a-zA-Z0-9+\\-\\/*():]+)\\s*$", Plugin6502::zp_abs_rel);
      addAddressingMode("zero page, x; absolute, x","^\\s*(?<command>[a-zA-Z]+)\\s+(?<params>[a-zA-Z0-9+\\-\\/*()]+)\\s*,\\s*x\\s*$", Plugin6502::zpx_absx);
      addAddressingMode("absolute, y","^\\s*(?<command>[a-zA-Z]+)\\s+(?<params>[a-zA-Z0-9+\\-\\/*()]+)\\s*,\\s*y\\s*$", Plugin6502::absolute_y);
      addAddressingMode("(indirect, x)","^\\s*(?<command>[a-zA-Z]*)\\s*\\(\\s*(?<params>[a-zA-Z0-9+\\-\\/*()]+)\\s*, x\\s*\\)$", Plugin6502::indirect_x);
      addAddressingMode("(indirect), y","^\\s*(?<command>[a-zA-Z]+)\\s*\\(\\s*(?<params>[a-zA-Z0-9+\\-\\/*()]+)\\s*\\)\\s*,\\s*y\\s*$", Plugin6502::indirect_y);
      addAddressingMode("(indirect)","^\\s*(?<command>[a-zA-Z]+)\\s*\\(\\s*(?<params>[a-zA-Z0-9+\\-\\/*()]+)\\s*\\)\\s*$", Plugin6502::indirect);

      addMnemonic("adc i 69 zp 65 zpx 75 a 6d ax 7d ay 79 ix 61 iy 71");
      addMnemonic("and i 29 zp 25 zpx 35 a 2d ax 3d ay 39 ix 21 iy 31");
      addMnemonic("asl acc 0a zp 06 zpx 16 a 0e ax 1e");
      addMnemonic("bcc r 90");
      addMnemonic("bcs r b0");
      addMnemonic("beq r f0");
      addMnemonic("bit zp 24 a 2c");
      addMnemonic("bmi r 30");
      addMnemonic("bne r d0");
      addMnemonic("bpl r 10");
      addMnemonic("brk imp 00");
      addMnemonic("bvc r 50");
      addMnemonic("bvs r 70");
      addMnemonic("clc imp 18");
      addMnemonic("cld imp d8");
      addMnemonic("cli imp 58");
      addMnemonic("clv imp b8");
      addMnemonic("cmp i c9 zp c5 zpx d5 a cd ax dd ay d9 ix c1 iy d1");
      addMnemonic("cpx i e0 zp e4 a ec");
      addMnemonic("cpy i c0 zp c4 a cc");
      addMnemonic("dec zp c6 zpx d6 a ce ax de");
      addMnemonic("dex imp ca");
      addMnemonic("dey imp 88");
      addMnemonic("eor i 49 zp 45 zpx 55 a 4d ax 5d ay 59 ix 41 iy 51");
      addMnemonic("inc zp e6 zpx f6 a ee ax fe");
      addMnemonic("inx imp e8");
      addMnemonic("iny imp c8");
      addMnemonic("jmp a 4c ind 6c");
      addMnemonic("jsr a 20");
      addMnemonic("lda i a9 zp a5 zpx b5 a ad ax bd ay b9 ix a1 iy b1");
      addMnemonic("ldx i a2 zp a6 zpy b6 a ae ay be");
      addMnemonic("ldy i a0 zp a4 zpx b4 a ac ax bc");
      addMnemonic("lsr acc 4a zp 46 zpx 56 a 4e ax 5e");
      addMnemonic("nop imp ea");
      addMnemonic("ora i 09 zp 05 zpx 15 a 0d ax 1d ay 19 ix 01 iy 11");
      addMnemonic("pha imp 48");
      addMnemonic("php imp 08");
      addMnemonic("pla imp 68");
      addMnemonic("plp imp 28");
      addMnemonic("rol acc 2a zp 26 zpx 36 a 2e ax 3e");
      addMnemonic("ror acc 6a zp 66 zpx 76 a 6e ax 7e");
      addMnemonic("rti imp 40");
      addMnemonic("rts imp 60");
      addMnemonic("sbc i e9 zp e5 zpx f5 a ed ax fd ay f9 ix e1 iy f1");
      addMnemonic("sec imp 38");
      addMnemonic("sed imp f8");
      addMnemonic("sei imp 78");
      addMnemonic("sta zp 85 zpx 95 a 8d ax 9d ay 99 ix 81 iy 91");
      addMnemonic("stx zp 86 zpy 96 a 8e");
      addMnemonic("sty zp 84 zpx 94 a 8c");
      addMnemonic("tax imp aa");
      addMnemonic("tay imp a8");
      addMnemonic("tsx imp ba");
      addMnemonic("txa imp 8a");
      addMnemonic("txs imp 9a");
      addMnemonic("tya imp 98");

      addInfillFunction(INFILL_ABS, Plugin6502::infill_abs);
      addInfillFunction(INFILL_REL, Plugin6502::infill_rel);
      addInfillFunction(INFILL_IMM, Plugin6502::infill_imm);

      immediateHashmap = addressingModeHashMaps.get("i");
      impliedHashmap = addressingModeHashMaps.get("imp");
      zeroPageHashmap = addressingModeHashMaps.get("zp");
      zeroPageXHashmap = addressingModeHashMaps.get("zpx");
      absoluteHashmap = addressingModeHashMaps.get("a");
      absoluteXHashmap = addressingModeHashMaps.get("ax");
      absoluteYHashmap = addressingModeHashMaps.get("ay");
      relativeHashmap = addressingModeHashMaps.get("r");
      indirectHashmap = addressingModeHashMaps.get("ind");
      bIndirectXBHashmap = addressingModeHashMaps.get("indx");
      bIndirectBYHashmap = addressingModeHashMaps.get("indy");
      accumulatorHashmap = addressingModeHashMaps.get("acc");
   }

   @Override
   public boolean functionNoParams(CompilerState state) throws Exception {
      return false;
   }

   @Override
   public boolean functionWithParams(CompilerState state) throws Exception {
      return false;
   }

   @Override
   public boolean commandBlocks(CompilerState state) throws Exception {
      if ("bne+".equals(state.command)) {
         state.machineCode.addAll(add(state.machineCode.size(), state.tab + "bne +", "d0", INFILL_REL)); // add the branch code
         int co = compilerOutput.size() - 1;
         int relPos = state.machineCode.size() - 1; // remember the jump delta position
         AssemblySubCompiler.compile(new CompilerState(state.commandBlock, state.machineCode, state.symbols, state.tab)); // compile the block
         if (state.machineCode.size() > relPos + 126)
            throw new Exception("Branch too far error, asm size = " + state.machineCode.size() + " max = " + (relPos + 126));
         int delta = state.machineCode.size() - relPos - 1; // relative jump (not absolute)
         int consoleDelta = compilerOutput.size() - co;
         log(consoleDelta, "" + delta);
         state.machineCode.set(relPos, delta); // jump !
         return true;
      }
      return false;
   }

   public static void immediate(CompilerState state) throws Exception {
      if(!immediateHashmap.containsKey(state.command)) throw new Exception("Immediate addressing mode error, multiple commands found (" + state.command + ")");
      Integer value = parseExpression(state.params[0], state.symbols);
      if (value != null) state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " #" + value, immediateHashmap.get(state.command), lowByte(value)));
      else state.machineCode.addAll(addWithLabel(state.machineCode.size(), state.tab + state.command + " #" + state.params[0], state.params[0], state.symbols, 1, immediateHashmap.get(state.command), INFILL_IMM));
   }

   public static void imp_acc(CompilerState state) throws Exception {
      if(impliedHashmap.containsKey(state.command)) state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command, impliedHashmap.get(state.command)));
      else if(accumulatorHashmap.containsKey(state.command)) state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command, accumulatorHashmap.get(state.command)));
      else throw new Exception("Implied addressing mode error, multiple commands found (" + state.command + ")");
   }

   public static void indirect(CompilerState state) throws Exception {
      if(!indirectHashmap.containsKey(state.command)) throw new Exception("(indirect) addressing mode error, multiple commands found (" + state.command + ")");
      Integer value = parseExpression(state.params[0], state.symbols);
      if (value == null) throw new Exception(state.command + " (indirect) operand not understood.");
      state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " (" + value + "), y", indirectHashmap.get(state.command), lowByte(value), highByte(value)));
   }

   public static void indirect_x(CompilerState state) throws Exception {
      if(!bIndirectXBHashmap.containsKey(state.command)) throw new Exception(state.command + " (indirect, x) operand not understood.");
      Integer value = parseExpression(state.params[0], state.symbols);
      if (value == null) throw new Exception(state.command + " (indirect, x) operand not understood.");
      state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " (" + value + ", x)", bIndirectXBHashmap.get(state.command), lowByte(value)));
   }

   public static void indirect_y(CompilerState state) throws Exception {
      if(!bIndirectBYHashmap.containsKey(state.command)) throw new Exception(state.command + " (indirect), Y operand not understood.");
      Integer value = parseExpression(state.params[0], state.symbols);
      if (value != null) {
         state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " (" + value + "), y", bIndirectBYHashmap.get(state.command), lowByte(value)));
         return;
      }
      else if(!state.params[0].contains(":")) state.machineCode.addAll(addWithLabel(state.machineCode.size(), state.tab + "(" + state.command + " " + state.params[0] + "), y", state.params[0], state.symbols, 2, indirectHashmap.get(state.command), INFILL_IND, INFILL_IND));
      else state.machineCode.addAll(addSelfModify(state.machineCode.size(), state.command, state.symbols, state.params[0], INFILL_IND));

      // todo - is this correct ? - should we not have return for previous else if's ?
      throw new Exception(state.command + " (indirect), Y operand not understood.");
   }

   public static void zp_abs_rel(CompilerState state) throws Exception {
      Integer value = parseExpression(state.params[0], state.symbols);
      if(relativeHashmap.containsKey(state.command)) {
         if(value == null) {
            state.machineCode.addAll(addWithLabel(state.machineCode.size(), state.tab + state.command + " " + state.params[0], state.params[0], state.symbols, 1, relativeHashmap.get(state.command), INFILL_REL)); // relative branch
            return;
         }
         int delta = value - asmStart - state.machineCode.size() - 2;
         state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " " + delta, relativeHashmap.get(state.command), delta));
         return;
      }
      if(!(zeroPageHashmap.containsKey(state.command) || absoluteHashmap.containsKey(state.command))) throw new Exception("Zero page, absolute, relative addressing mode error, multiple commands found (" + state.command + ")");
      if(value != null) {
         if(value < 256) state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " " + value, zeroPageHashmap.get(state.command), value));
         else state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " " + value, absoluteHashmap.get(state.command), lowByte(value), highByte(value)));
      }
      else if(!state.params[0].contains(":")) state.machineCode.addAll(addWithLabel(state.machineCode.size(), state.tab + state.command + " " + state.params[0], state.params[0], state.symbols, 2, absoluteHashmap.get(state.command), INFILL_ABS, INFILL_ABS));
      else state.machineCode.addAll(addSelfModify(state.machineCode.size(), state.command, state.symbols, state.params[0], INFILL_ABS));
   }

   public static void zpx_absx(CompilerState state) throws Exception {
      if(!(zeroPageXHashmap.containsKey(state.command) || absoluteXHashmap.containsKey(state.command))) throw new Exception("zero page, x / absolute, x addressing mode error, multiple commands found (" + state.command + ")");
      Integer value = parseExpression(state.params[0], state.symbols);
      if (value != null) {
         if(value < 256) state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " " + value + ", x", zeroPageXHashmap.get(state.command), value));
         else state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " " + value + ", x", absoluteXHashmap.get(state.command), lowByte(value), highByte(value)));
      }
      else if(!state.params[0].contains(":")) state.machineCode.addAll(addWithLabel(state.machineCode.size(), state.tab + state.command + " " + state.params[0] + ", x", state.params[0], state.symbols, 2, absoluteXHashmap.get(state.command), INFILL_ABS, INFILL_ABS));
      else state.machineCode.addAll(addSelfModify(state.machineCode.size(), state.command, state.symbols, state.params[0], INFILL_ABS));
   }

   public static void absolute_y(CompilerState state) throws Exception {
      if(!absoluteYHashmap.containsKey(state.command)) throw new Exception("absolute, y addressing mode error, multiple commands found (" + state.command + ")");
      Integer value = parseExpression(state.params[0], state.symbols);
      if (value != null) state.machineCode.addAll(add(state.machineCode.size(), state.tab + state.command + " " + value + ", y", absoluteYHashmap.get(state.command), lowByte(value), highByte(value))); // abs
      else if(!state.params[0].contains(":")) state.machineCode.addAll(addWithLabel(state.machineCode.size(), state.tab + state.command + " " + state.params[0] + ", y", state.params[0], state.symbols, 2, absoluteYHashmap.get(state.command), INFILL_ABS, INFILL_ABS));
      else state.machineCode.addAll(addSelfModify(state.machineCode.size(), state.command, state.symbols, state.params[0], INFILL_ABS));
   }

   static ArrayList<Integer> addSelfModify(int asmSize, String command, TreeMap<String, Symbol> symbols, String param, int infillValue) throws Exception {
      String [] label = param.split(":");

      if(label.length == 1)  {
         addLabel(label[0].trim(), symbols, asmSize + 1);
         return add(asmSize, "", 0, 0);
      }
      else if(label.length == 2) {
         addLabel(label[0].trim(), symbols, asmSize + 1);
         String param2 = label[1].trim();
         try {
            // if the second parameter is a value
            int value2 = Integer.parseInt(param2);
            return add(asmSize, "", absoluteHashmap.get(command), lowByte(value2), highByte(value2));
         }
         catch(Exception ignore) {
            // if the second parameter is am expression or label
            return addWithLabel(asmSize, command + " " + param2, param2, symbols, 2, absoluteHashmap.get(command), infillValue, infillValue);
         }
      }
      else throw new Exception("Self modifying label format error. Use only:\n\nlabel: or label:value or label:labelValue");
   }

   static void infill_abs(Usage loc, Symbol symbol, CompilerState state) {
      state.machineCode.set(loc.pc, lowByte(symbol.value));
      state.machineCode.set(loc.pc + 1, highByte(symbol.value));
      log(compilerOutput.size() - loc.line, Integer.toHexString(lowByte(symbol.value)) + ", " + Integer.toHexString(highByte(symbol.value)));
   }

   static void infill_imm(Usage loc, Symbol symbol, CompilerState state) {
      state.machineCode.set(loc.pc, lowByte(symbol.value));
      log(compilerOutput.size() - loc.line, "" + Integer.toHexString(lowByte(symbol.value)));
   }

   static void infill_rel(Usage loc, Symbol symbol, CompilerState state) throws Exception {
      int delta = symbol.value - loc.pc - asmStart - 1;
      if (delta > 127) throw new Exception("Branch too far error.");
      state.machineCode.set(loc.pc, delta);
      log(compilerOutput.size() - loc.line,  "" + Integer.toHexString(delta));
   }
}
