# 📚 ANTLR4 Compiler Shell

Este é um projeto base para a disciplina de Compiladores, desenvolvido em Java utilizando o ecossistema Maven. O projeto integra o ANTLR4 para análise léxica e sintática, o Picocli para uma interface de linha de comando robusta e uma interface gráfica customizada para inspeção de árvores sintáticas (Parse Trees).

.\mvnw.cmd clean package

java -jar target/compiler.jar -i example.expr

java -jar pcode.jar -Input=out.pcode

java -jar pcode.jar -Input=C:\dev\antlr4_java_template\out.pcode -Debug=false