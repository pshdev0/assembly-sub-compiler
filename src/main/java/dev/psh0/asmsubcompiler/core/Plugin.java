package dev.psh0.asmsubcompiler.core;

import groovy.lang.GroovyShell;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Plugin {
    public static String workingDirectory = "";
    protected static int asmStart = 0;
    public static ArrayList<String> compilerOutput = new ArrayList<>();
    public static TreeSet<Integer> sortedListOfActiveUnknownSymbolUsageLineNumbers = new TreeSet<>();
    public static int printedToLine = 0;

    public static final int VERBOSE_STANDARD = 1000;
    public static final int VERBOSE_EXTRA = 2000;
    static int verbosity = VERBOSE_EXTRA;

    private static final Pattern binaryPattern = Pattern.compile("(%[01]{1,8})");
    private static final Pattern hexPattern = Pattern.compile("(\\$[a-fA-F0-9]+)");
    private static final Pattern commentsPattern = Pattern.compile("//.*$|^\\s*/\\*(([^\\\\?}]]*)*)\\s*\\*/|\\s*//.*$", Pattern.MULTILINE);

    protected static final String GROOVY_INLINE_CODE = """
            int lo(int x) { return x & 255; }
            int hi(int x) { return x >>> 8; }
            """;

    static GroovyShell shell = new GroovyShell();
    protected static ArrayList<Plugin> plugins = new ArrayList<>();
    public static ArrayList<AddressingMode> addressingModes = new ArrayList<>();

    public enum ExportType {PRG, BIN}

    public static ExportType exportType = ExportType.PRG;
    protected int exportHeaderLow = 1;
    protected int exportHeaderHigh = 8;

    protected static HashMap<String, HashMap<String, String>> addressingModeHashMaps = new HashMap<>();
    static HashMap<Integer, InfillLambda> infillFunctions = new HashMap<>();

    protected static void addMnemonic(String details) {
        // format: MNEMONIC [MODE OPCODE]
        // e.g. lda i a9 zp a5 zpx b5 a ad ax bd ay b9 ix a1 iy b1
        String[] tokens = details.split(" ");
        int counter = 1;
        while (counter < tokens.length) {
            String addressingMode = tokens[counter];
            String opCode = tokens[counter + 1];
            if (!addressingModeHashMaps.containsKey(addressingMode))
                addressingModeHashMaps.put(addressingMode, new HashMap<>());
            addressingModeHashMaps.get(addressingMode).put(tokens[0], opCode);
            counter += 2;
        }
    }

    protected static void addLabel(String label, TreeMap<String, Symbol> symbols, int asmSize) throws Exception {
        // the label exists already?
        if (symbols.containsKey(label)) {
            if (symbols.get(label).defined) throw new Exception("Symbol (" + label + ") already defined error.");
            symbols.get(label).value = asmStart + asmSize; // we found the location of this label !
        } else {
            Symbol ll = new Symbol(Symbol.Type.LABEL);
            ll.value = asmStart + asmSize;
            symbols.put(label, ll); // add a new label populator
        }
        symbols.get(label).defined = true;
    }

    public static void log(int moveBack, String text) {
        int index = compilerOutput.size() - moveBack;
        String out = compilerOutput.get(index).trim();
        compilerOutput.set(index, out + Color.YELLOW + "↓ " + Color.WHITE + text); // move-back logs are generally used to infill data
    }

    public static void log(String text) {
        log(text, VERBOSE_STANDARD);
    }

    public static void log(String text, int priority) {
        if (priority > verbosity) return;
        log("", text, "");
    }

    public static void log(String text1, String text2, String text3) {
        String out = String.format("%-5s %-50s %s" + Color.WHITE, text1, text2, text3);
        compilerOutput.add(out);
        if (Plugin.sortedListOfActiveUnknownSymbolUsageLineNumbers.size() == 0 || Plugin.sortedListOfActiveUnknownSymbolUsageLineNumbers.first() >= compilerOutput.size()) {
            for (int c1 = Plugin.printedToLine; c1 < compilerOutput.size(); c1++)
                System.out.println(compilerOutput.get(c1));
            Plugin.printedToLine = compilerOutput.size();
        }
    }

    protected static Integer parseExpression(String expression, TreeMap<String, Symbol> symbols) throws Exception {
        expression = replaceKnownSymbols(expression, symbols);

        // evaluate Groovy expression
        try {
            return (Integer) shell.evaluate(GROOVY_INLINE_CODE + "return (int)(" + expression + ");");
        } catch (Exception e) {
            return null;
        }
    }

    protected static String replaceKnownSymbols(String lineOfCode, TreeMap<String, Symbol> symbols) {
        // need to avoid replacing e.g. the known symbol "node" in strings like "node0" since "node0" is a different thing
        // so in fact symbols are only ever recognised if delimited correctly by certain rules, e.g. "node+0" should replace "node"
        // acceptable delimitations are: " ,+-/*%"
        for (Map.Entry<String, Symbol> x : symbols.entrySet()) {
            if (!x.getValue().defined) continue;
            Matcher m = Pattern.compile("\\b" + x.getKey() + "\\b").matcher(lineOfCode);
            if (m.find()) lineOfCode = m.replaceAll("" + x.getValue().value);
        }
        return lineOfCode;
    }

    protected static int lowByte(int value) {
        return value & 255;
    }

    protected static int highByte(int value) {
        return value >>> 8;
    }

//   protected static ArrayList<Integer> addWithLabel(int asmSize, String str, String command, TreeMap<String, Symbol> symbols, int usedAtBackStep, Object... values) throws Exception {
//      ArrayList<Integer> rtn = add(asmSize, str, values);
//      if(symbols.containsKey(command)) symbols.get(command).usages.add(new Usage(asmSize + rtn.size() - usedAtBackStep));
//      else {
//         Symbol l = new Symbol(Symbol.Type.EXPRESSION);
//         l.usages.add(new Usage(asmSize + rtn.size() - usedAtBackStep));
//         symbols.put(command, l);
//      }
//      return rtn;
//   }

    protected static ArrayList<Integer> addWithLabel(int asmSize, String str, String command, TreeMap<String, Symbol> symbols, int usedAtBackStep, Object... values) throws Exception {
        ArrayList<Integer> rtn = add(asmSize, str, values);
        Usage usage = new Usage(asmSize + rtn.size() - usedAtBackStep);
        if (symbols.containsKey(command)) symbols.get(command).usages.add(usage);
        else {
            Symbol l = new Symbol(Symbol.Type.EXPRESSION);
            l.usages.add(usage);
            symbols.put(command, l);
        }
        Plugin.sortedListOfActiveUnknownSymbolUsageLineNumbers.add(usage.line);
        return rtn;
    }

    protected static ArrayList<Integer> add(int pc, String str, Object... values) throws Exception {
        // outputs str to the console, then adds values to the machine code array
        // and String type values are assumed to be hex values and converted to decimal
        ArrayList<Integer> rtn = new ArrayList<>();
        StringBuilder output = new StringBuilder();
        for (Object x : values) {
            if (x instanceof String) {
                rtn.add(Integer.parseInt((String) x, 16));
                output.append(x).append(" ");
            } else {
                int val = (int) x;
                if (val >= -128) val = twosComplement(val); // we don't want to do this for negative INFILL_X values !
                rtn.add(val);

                if (val >= -128)
                    output.append(Integer.toHexString(val)).append(" "); // don't output infill codes (we will add the real values later)
            }
        }
//      if(str != null) log(String.format("%-20s %-50s %s", Integer.toHexString(pc), str, ": " + Color.CYAN + output));
        if (str != null) log(pad4(Integer.toHexString(pc + asmStart)), str, ": " + Color.CYAN + output);
        return rtn;
    }

    public static void addAddressingMode(String name, String regex, AddressingModeLambda func) {
        addressingModes.add(new AddressingMode(name, regex, func));
    }

    public static void addInfillFunction(Integer id, InfillLambda func) throws Exception {
        if (infillFunctions.containsKey(id))
            throw new Exception("Duplicate infill function id found, choose a different id.");
        infillFunctions.put(id, func);
    }

    static int twosComplement(int x) {
        return x < 0 ? x + 256 : x;
    }

    static String convertBinaryToDecimal(String str) {
        Matcher m = binaryPattern.matcher(str);
        return m.replaceAll(x -> {
            int val = Integer.parseInt(x.group(1).substring(1), 2);
            if (val < 0 || val > 65535) return ("[ VALUE OUT OF BOUND ERROR ]");
            return "" + val;
        });
    }

    protected static String convertHexToDecimal(String str) {
        Matcher m = hexPattern.matcher(str);
        return m.replaceAll(x -> {
            int val = Integer.parseInt(x.group(1).substring(1), 16);
            if (val < 0 || val > 65535) return ("[ VALUE OUT OF BOUND ERROR ]");
            return "" + val;
        });
    }

    static String removeComments(String str) {
        return commentsPattern.matcher(str).replaceAll("");
    }

    protected String loadASMCode(String fileName) {
        String resourceName = "../../../../asm/" + fileName + ".asm";
        System.out.println("Looking for: " + resourceName);
        URL url = getClass().getResource(resourceName);

        System.out.println("Found: " + url);
        assert url != null;
        System.out.println("File: " + url.getFile());

        File file = new File(url.getFile());
        try {
            return Files.readString(Path.of(file.getPath()));
        } catch (IOException e) {
            return null;
        }
    }

    public static ArrayList<Integer> readBytesFromFile(File file) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        ArrayList<Integer> rtn = new ArrayList<>();
        for (int c1 = 0; c1 < data.length; c1++) rtn.add(data[c1] & 0xFF);
        return rtn;
    }

    public static void writeBytesToFile(File file, int[] bytes, Integer exportAddressLo, Integer exportAddressHi) {
        System.out.println("Exporting file: " + file.getAbsoluteFile());

        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            if (exportAddressLo != null) {
                out.writeByte(exportAddressLo);
                out.writeByte(exportAddressHi);
            }
            for (Integer b : bytes) out.writeByte(b);
            out.close();
            System.out.println("Exported " + bytes.length + " bytes.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ignored) {
        }
    }

    static String pad4(String x) {
        return switch (x.length()) {
            case 1 -> "000" + x;
            case 2 -> "00" + x;
            case 3 -> "0" + x;
            default -> x;
        };
    }

    static String pad2(String x) {
        return x.length() == 1 ? "0" + x : x;
    }

    protected enum Color {
        //Color end string, color reset
        RESET("\033[0m"),

        // Regular Colors. Normal color, no bold, background color etc.
        BLACK("\033[0;30m"),    // BLACK
        RED("\033[0;31m"),      // RED
        RED_LIGHT("\033[1;31m"),      // LIGHT RED
        GREEN("\033[0;32m"),    // GREEN
        YELLOW("\033[0;33m"),   // YELLOW
        BLUE("\033[0;34m"),     // BLUE
        MAGENTA("\033[0;35m"),  // MAGENTA
        CYAN("\033[0;36m"),     // CYAN
        WHITE("\033[0;37m"),    // WHITE

        // Bold
        BLACK_BOLD("\033[1;30m"),   // BLACK
        RED_BOLD("\033[1;31m"),     // RED
        GREEN_BOLD("\033[1;32m"),   // GREEN
        YELLOW_BOLD("\033[1;33m"),  // YELLOW
        BLUE_BOLD("\033[1;34m"),    // BLUE
        MAGENTA_BOLD("\033[1;35m"), // MAGENTA
        CYAN_BOLD("\033[1;36m"),    // CYAN
        WHITE_BOLD("\033[1;37m"),   // WHITE

        // Underline
        BLACK_UNDERLINED("\033[4;30m"),     // BLACK
        RED_UNDERLINED("\033[4;31m"),       // RED
        GREEN_UNDERLINED("\033[4;32m"),     // GREEN
        YELLOW_UNDERLINED("\033[4;33m"),    // YELLOW
        BLUE_UNDERLINED("\033[4;34m"),      // BLUE
        MAGENTA_UNDERLINED("\033[4;35m"),   // MAGENTA
        CYAN_UNDERLINED("\033[4;36m"),      // CYAN
        WHITE_UNDERLINED("\033[4;37m"),     // WHITE

        // Background
        BLACK_BACKGROUND("\033[40m"),   // BLACK
        RED_BACKGROUND("\033[41m"),     // RED
        GREEN_BACKGROUND("\033[42m"),   // GREEN
        YELLOW_BACKGROUND("\033[43m"),  // YELLOW
        BLUE_BACKGROUND("\033[44m"),    // BLUE
        MAGENTA_BACKGROUND("\033[45m"), // MAGENTA
        CYAN_BACKGROUND("\033[46m"),    // CYAN
        WHITE_BACKGROUND("\033[47m"),   // WHITE

        // High Intensity
        BLACK_BRIGHT("\033[0;90m"),     // BLACK
        RED_BRIGHT("\033[0;91m"),       // RED
        GREEN_BRIGHT("\033[0;92m"),     // GREEN
        YELLOW_BRIGHT("\033[0;93m"),    // YELLOW
        BLUE_BRIGHT("\033[0;94m"),      // BLUE
        MAGENTA_BRIGHT("\033[0;95m"),   // MAGENTA
        CYAN_BRIGHT("\033[0;96m"),      // CYAN
        WHITE_BRIGHT("\033[0;97m"),     // WHITE

        // Bold High Intensity
        BLACK_BOLD_BRIGHT("\033[1;90m"),    // BLACK
        RED_BOLD_BRIGHT("\033[1;91m"),      // RED
        GREEN_BOLD_BRIGHT("\033[1;92m"),    // GREEN
        YELLOW_BOLD_BRIGHT("\033[1;93m"),   // YELLOW
        BLUE_BOLD_BRIGHT("\033[1;94m"),     // BLUE
        MAGENTA_BOLD_BRIGHT("\033[1;95m"),  // MAGENTA
        CYAN_BOLD_BRIGHT("\033[1;96m"),     // CYAN
        WHITE_BOLD_BRIGHT("\033[1;97m"),    // WHITE

        // High Intensity backgrounds
        BLACK_BACKGROUND_BRIGHT("\033[0;100m"),     // BLACK
        RED_BACKGROUND_BRIGHT("\033[0;101m"),       // RED
        GREEN_BACKGROUND_BRIGHT("\033[0;102m"),     // GREEN
        YELLOW_BACKGROUND_BRIGHT("\033[0;103m"),    // YELLOW
        BLUE_BACKGROUND_BRIGHT("\033[0;104m"),      // BLUE
        MAGENTA_BACKGROUND_BRIGHT("\033[0;105m"),   // MAGENTA
        CYAN_BACKGROUND_BRIGHT("\033[0;106m"),      // CYAN
        WHITE_BACKGROUND_BRIGHT("\033[0;107m");     // WHITE

        private final String code;

        Color(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }
    }

    public boolean functionNoParams(CompilerState state) throws Exception {
        return false;
    }

    public boolean functionWithParams(CompilerState state) throws Exception {
        return false;
    }

    public boolean commandBlocks(CompilerState state) throws Exception {
        return false;
    }
}
