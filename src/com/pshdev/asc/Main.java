package com.pshdev.asc;

/*
    Assembly Sub Compiler
    (C) Pixel 2021
 */

import asc.Plugin;
import asc.AssemblySubCompiler;
import asc.CompilerState;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {

        String file = "UNKNOWN";
        String source = "";

// java -jar AssemblySubCompiler.jar ~/Code/X16Dev/SpriteDemo/src/sprites.asm ~/Code/X16Dev/SpriteDemo/ ~/Desktop/X16Plugins

//        args = new String [] { "/Users/paul/Code/X16Dev/SpriteDemo/src/sprites.asm",
//                               "/Users/paul/Code/X16Dev/SpriteDemo/",
//                               "/Users/paul/Desktop/X16Plugins/" };

        if(args.length != 3) throw new AssertionError("   error, use arguments: [asm file] [project folder] [plugin dir]");

        System.out.println();
        System.out.println("┌────────────────────────────┐");
        System.out.println("│ Assembly Sub Compiler v0.1 │");
        System.out.println("└────────────────────────────┘");
        System.out.println();

        System.out.println("File: " + args[0]);
        System.out.println("Root: " + args[1]);

        AssemblySubCompiler.loadPluginClasses(new File(args[2]));

        try {
            file = args[0];
            System.out.println("Attempting to read: " + file);
            source = Files.readString(Path.of(file));
            source += "\n\n\n\n";
        } catch (IOException e) {
            System.out.println("Could not read the source file: " + file);
            return;
        }

        System.out.println("Source file loaded.");
        AssemblySubCompiler.workingDirectory = args[1] += (args[1].endsWith("/") ? "" : "/");

        CompilerState state = new CompilerState(source);
        try {
            System.out.println();
            System.out.println("Compiling...");
            AssemblySubCompiler.compile(state);
        } catch (Exception e) {
            System.out.println("Compiler exited with error: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            Files.createDirectories(Path.of(AssemblySubCompiler.workingDirectory + "bin/"));
            Files.createDirectories(Path.of(AssemblySubCompiler.workingDirectory + "prg/"));
            System.out.println("bin/ and prg/ directories created.");
        } catch (IOException e) {
            System.out.println("Could not create /bin and /prg directory");
            return;
        }

//        for(String str : Plugin.compilerOutput) System.out.println(str);

        String exportAs;

        if(Plugin.exportType == Plugin.ExportType.BIN) {
            exportAs = AssemblySubCompiler.workingDirectory + "bin/" + Path.of(args[0]).getFileName();
            exportAs = exportAs.substring(0, exportAs.lastIndexOf(".")) + ".bin";
            Plugin.writeBytesToFile(new File(exportAs), state.machineCode.stream().mapToInt(Integer::intValue).toArray(), null, null);
        }
        else if(Plugin.exportType == Plugin.ExportType.PRG) {
            exportAs = AssemblySubCompiler.workingDirectory + "prg/" + Path.of(args[0]).getFileName();
            exportAs = exportAs.substring(0, exportAs.lastIndexOf(".")) + ".prg";
            Plugin.writeBytesToFile(new File(exportAs), state.machineCode.stream().mapToInt(Integer::intValue).toArray(), 0, 8);
        }
        else {
            System.out.println("Input files must be .asm or .res files.");
            return;
        }

        for(Integer x : state.machineCode) System.out.print(x + ", ");
        System.out.println();

        System.out.println("EXPORTED AS: " + exportAs);
    }
}
