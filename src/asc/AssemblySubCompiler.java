package asc;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// todo - fix bne +, beq, bne. etc bug ... (only seems to be in the main code, not e.g. in decompressors etc)
// addSelfModify -> do we not need to generify this ?
// Plugin - strip to bare minimum for general plugin support

// todo - will addSelfModify work with

public class AssemblySubCompiler extends Plugin {
    private static final Pattern commentsPattern = Pattern.compile("//.*$|^\\s*/\\*(([^\\\\?}]]*)*)\\s*\\*/|\\s*//.*$", Pattern.MULTILINE);

    public static void loadPluginClasses(File pluginClassDir) {
        System.out.println();
        System.out.println("Loading plugins:");
        System.out.println();

        // attempt to load all compiler plugins

        // set up the class loader
        assert pluginClassDir != null;
        ClassLoader cl = null;
        try {
            URL url = pluginClassDir.toURI().toURL();
            URL[] urls = new URL[]{url};
            cl = new URLClassLoader(urls); // create a new class loader with the directory
        } catch (MalformedURLException ignored) {
        }

        // get the plugins from the directory
        assert cl != null;
        File[] list = pluginClassDir.listFiles();
        assert list != null;
        List<String> pluginList = Stream.of(list).filter(f -> !f.isDirectory()).map(File::getName).collect(Collectors.toList());

        // load all plugins
        for (String pluginStr : pluginList) {

            if(!pluginStr.endsWith(".class")) continue; // only deal in Java class objects
            if(!pluginStr.startsWith("asc.plugins.")) pluginStr = "asc.plugins." + pluginStr; // prepend the package

            pluginStr = pluginStr.replaceAll(".class", "");
            System.out.println("   " + pluginStr);
            String className = String.valueOf(Path.of(pluginStr).getFileName());
            try {
                plugins.add((Plugin) cl.loadClass(className).getDeclaredConstructor().newInstance());
            }
            catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                System.out.println("\nCould not load plugin: " + pluginStr);
                e.printStackTrace();
                System.exit(1);
            }
        }
        System.out.println();
    }

    public static void compile(CompilerState state) throws Exception {

        // sometimes we want to keep sub-compile symbols separate to those of the caller
        if (state.symbols == null) {
//            log(state.tab + Colors.BLUE_BOLD + "symbols = []");
            state.symbols = new TreeMap<>((s1, s2) -> {
                if (s1.length() > s2.length()) return -1;
                else if (s1.length() < s2.length()) return 1;
                else return s1.compareTo(s2);
            });
        }

        state.program = commentsPattern.matcher(state.program).replaceAll("");
        state.program = convertBinaryToDecimal(state.program);
        state.program = convertHexToDecimal(state.program);

        // create pattern matchers for all addressing modes for this node's code
        HashMap<AddressingMode, Matcher> commands = new HashMap<>();
        for(AddressingMode am : addressingModes) commands.put(am, Pattern.compile(am.regex, Pattern.MULTILINE).matcher(state.program));

        state.tab2 = state.tab;
        state.tab += "   ";
        log(state.tab2 + Colors.YELLOW_BOLD + "{"); // sub-compilation always begins a new scope

        // compile
        while (state.cursor < state.program.length()) {

            // find next addressing mode
            Map.Entry<AddressingMode, Matcher> nextCommand = null;
            int min = Integer.MAX_VALUE;
            for (Map.Entry<AddressingMode, Matcher> x : commands.entrySet()) {
                AddressingMode am = x.getKey();
                Matcher m = x.getValue();
                if (m.find(state.cursor)) {
                    am.start = m.start();
                    am.end = m.end();
                    if(am.start < min) {
                        min = am.start;
                        nextCommand = x;
                    }
                }
            }
            if (nextCommand == null) throw new Exception("! Command not recognised:\n" + state.program.substring(state.cursor));

            // check there's no garbage between current cursor and the next command
            if (state.program.substring(state.cursor, nextCommand.getKey().start).replaceAll("[\s\n]", "").length() != 0) throw new Exception("Garbage found error:\n\n" + state.program.substring(state.cursor, nextCommand.getKey().start) + "\n\n");

            // next cursor position
            state.cursor = nextCommand.getKey().end;

            // get command
            try { state.command = nextCommand.getValue().group("command").trim(); } catch(Exception ignored) { }

            // get label
            try { state.label = nextCommand.getValue().group("label").trim(); } catch(Exception ignored) { }

            // get any parameters
            try {
                state.params = nextCommand.getValue().group("params").split(",");
                for (int c1 = 0; c1 < state.params.length; c1++) {
                    state.params[c1] = state.params[c1].trim();
                    state.params[c1] = replaceKnownSymbols(state.params[c1], state.symbols);
                }
            }
            catch (Exception ignore) { state.params = null; }

            // run the command
            nextCommand.getKey().func.run(state);
        }

        // replace known symbols
        int unknownSymbols = 0;
        ArrayList<Usage> removeList = new ArrayList<>();
        for(Map.Entry<String, Symbol> s : state.symbols.entrySet()) {
            Symbol symbol = s.getValue();
            if((symbol.value = parseExpression(s.getKey(), state.symbols)) == null) {
                log(state.tab2 + Color.CYAN + "Unknown symbol: " + s.getKey(), VERBOSE_EXTRA);
                unknownSymbols++;
                continue;
            }

            if(symbol.type == Symbol.Type.LABEL && !symbol.defined) continue;

            removeList.clear();
            for(Usage loc : symbol.usages) {
                // run the corresponding infill code
                infillFunctions.get(state.machineCode.get(loc.pc)).run(loc, symbol, state);

                // remove the infill later
                removeList.add(loc);
                Plugin.sortedListOfActiveUnknownSymbolUsageLineNumbers.remove(loc.line);
            }
            symbol.usages.removeAll(removeList);
        }

        if(unknownSymbols == 0 && state.symbols.size() > 0) log(state.tab2 + Colors.BLUE + "All symbols resolved.", VERBOSE_EXTRA);
        else if(state.symbols.size() > 0)
            log(state.tab2 + Colors.BLUE + unknownSymbols + " unknown symbols: " + Colors.WHITE +
                state.symbols.entrySet().stream().filter(stringSymbolEntry -> stringSymbolEntry.getValue().usages.size() > 0).map(Map.Entry::getKey).collect(Collectors.joining(", ")), VERBOSE_EXTRA);

        log(state.tab2 + Colors.YELLOW + "}"); // end scope
    }
}

// Pattern worWdExpandPattern = Pattern.compile("\\s*hword\\(\\s*([0-9]+)\\s*\\)\\s*");

//    String wordExpand(String lineOfCode) {
//        // replaces hword(number) with "lo(number), hi(number)"
//        Matcher m = wordExpandPattern.matcher(lineOfCode);
//        return m.replaceAll(matchResult -> {
//            int address = Integer.parseInt(matchResult.group(1).trim());
//            return " " + (address & 255) + ", " + (address >>> 8);
//        });
//    }
//
//    MyLineNumberFactory.setAsmInfo(asmInfoList); // set the byte code / line numbers
//    for(String x : compilerOut) log(x); // show the compilation result in the console
//    return assembler.stream().mapToInt(i -> i).toArray(); // return the assembly byte code

