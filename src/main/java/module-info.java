module asmsubcompiler {
    requires org.apache.groovy;

    exports dev.psh0.asmsubcompiler.core;
    exports dev.psh0.asmsubcompiler.huffman;
    exports dev.psh0.asmsubcompiler.plugins;

    opens dev.psh0.asmsubcompiler;
}
