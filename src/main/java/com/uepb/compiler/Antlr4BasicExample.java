package com.uepb.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.uepb.ExprLexer;
import com.uepb.ExprParser;
import com.uepb.gui.GuiVizualizerTask;
import com.uepb.interfaces.CompilerEngine;

public class Antlr4BasicExample implements CompilerEngine {

    @Override
    public void execute(File input, File output, boolean verbose) throws IOException {
        var charStream = CharStreams.fromPath(input.toPath());
        var lexer = new ExprLexer(charStream);
        var tokens = new CommonTokenStream(lexer);
        var parser = new ExprParser(tokens);
        var tree = parser.prog();

        if (parser.getNumberOfSyntaxErrors() > 0) {
            System.err.println("Compilacao abortada: erros sintaticos encontrados.");
            return;
        }

        var calculadora = new Calculadora();
        calculadora.visitProg(tree);

        if (calculadora.temErro()) {
            System.err.println("Compilacao abortada: erros semanticos encontrados.");
            return;
        }

        var code = calculadora.getCode();
        Files.writeString(output.toPath(), code);
        System.out.println("Gerou o codigo");

        if (verbose) {
            var guiTask = new GuiVizualizerTask(parser, tree);
            guiTask.run();
        }
    }
}