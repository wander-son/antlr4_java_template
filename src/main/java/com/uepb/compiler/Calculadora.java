package com.uepb.compiler;

import com.uepb.ExprBaseVisitor;
import com.uepb.ExprParser.*;

public class Calculadora extends ExprBaseVisitor<Void> {

    private final ScopeControl scopes = new ScopeControl();
    private final MemoryMapper mapper = new MemoryMapper();
    private final StringBuilder code = new StringBuilder();
    private int label = 0;
    private boolean temErro = false;

    private String createLabel() {
        return "L" + (label++);
    }

    public String getCode() {
        return code.toString();
    }

    public boolean temErro() {
        return temErro;
    }

    private void erroSemantico(String mensagem) {
        System.err.println("Erro semantico: " + mensagem);
        temErro = true;
    }

    @Override
    public Void visitProg(ProgContext ctx) {
        for (var stat : ctx.stat()) {
            visit(stat);
        }
        code.append("hlt\n");
        return null;
    }

    @Override
    public Void visitDeclVar(DeclVarContext ctx) {
        var varName = ctx.ID().getText();
        var tk = ctx.ID().getSymbol();
        var currentScope = scopes.getCurrentScope();

        if (currentScope.exists(varName)) {
            erroSemantico(
                    "A variavel '%s' presente na linha %d e coluna %d ja foi declarada."
                            .formatted(varName, tk.getLine(), tk.getCharPositionInLine()));
            return null;
        }

        var address = mapper.alloc();
        currentScope.insert(varName, address);

        code.append("push $").append(address).append("\n");
        if (ctx.expr() != null) {
            visit(ctx.expr());
        } else {
            code.append("push 0\n");
        }
        code.append("sto\n");

        return null;
    }

    @Override
    public Void visitAtribuicao(AtribuicaoContext ctx) {
        var varName = ctx.ID().getText();
        var tk = ctx.ID().getSymbol();
        var declaracaoOpt = scopes.lookup(varName);

        if (declaracaoOpt.isEmpty()) {
            erroSemantico(
                    "A variavel '%s' nao foi declarada na linha %d e coluna %d."
                            .formatted(varName, tk.getLine(), tk.getCharPositionInLine()));
            return null;
        }

        var address = declaracaoOpt.get().address();
        code.append("push $").append(address).append("\n");
        visit(ctx.expr());
        code.append("sto\n");

        return null;
    }

    @Override
    public Void visitIfStat(IfStatContext ctx) {
        var endLabel = createLabel();
        var nextLabels = new java.util.ArrayList<String>();

        for (int i = 0; i < ctx.elseifClause().size(); i++) {
            nextLabels.add(createLabel());
        }
        var elseLabel = ctx.elseClause() != null ? createLabel() : endLabel;

        visit(ctx.boolExpr());
        var firstNext = nextLabels.isEmpty() ? elseLabel : nextLabels.get(0);
        code.append("fjp ").append(firstNext).append("\n");
        scopes.createScope();
        for (var s : ctx.stat())
            visit(s);
        scopes.dropScope();
        code.append("ujp ").append(endLabel).append("\n");

        for (int i = 0; i < ctx.elseifClause().size(); i++) {
            code.append(nextLabels.get(i)).append(":\n");
            var clause = ctx.elseifClause(i);
            visit(clause.boolExpr());
            var nextNext = (i + 1 < nextLabels.size()) ? nextLabels.get(i + 1) : elseLabel;
            code.append("fjp ").append(nextNext).append("\n");
            scopes.createScope();
            for (var s : clause.stat())
                visit(s);
            scopes.dropScope();
            code.append("ujp ").append(endLabel).append("\n");
        }

        if (ctx.elseClause() != null) {
            code.append(elseLabel).append(":\n");
            scopes.createScope();
            for (var s : ctx.elseClause().stat())
                visit(s);
            scopes.dropScope();
        }

        code.append(endLabel).append(":\n");
        return null;
    }

    @Override
    public Void visitWhileStat(WhileStatContext ctx) {
        var startLabel = createLabel();
        var endLabel = createLabel();

        code.append(startLabel).append(":\n");
        visit(ctx.boolExpr());
        code.append("fjp ").append(endLabel).append("\n");

        scopes.createScope();
        for (var s : ctx.stat())
            visit(s);
        scopes.dropScope();

        code.append("ujp ").append(startLabel).append("\n");
        code.append(endLabel).append(":\n");

        return null;
    }

    @Override
    public Void visitPrintStat(PrintStatContext ctx) {
        visit(ctx.printArg());
        code.append("out\n");
        code.append("push \"\\n\"\n");
        code.append("out\n");
        return null;
    }

    @Override
    public Void visitPrintExpr(PrintExprContext ctx) {
        visit(ctx.expr());
        return null;
    }

    @Override
    public Void visitPrintStr(PrintStrContext ctx) {
        var text = ctx.STRING().getText();
        text = text.replace("\\n", "\n");
        code.append("push ").append(text).append("\n");
        return null;
    }

    // pelo amor de deus não mexa nisso aqui so funciona desse jeito
    @Override
    public Void visitPotencia(PotenciaContext ctx) {
        var baseAddr = mapper.alloc();
        var expAddr = mapper.alloc();
        var resultAddr = mapper.alloc();
        var loopLabel = createLabel();
        var endLabel = createLabel();

        code.append("push $").append(baseAddr).append("\n");
        visit(ctx.O1);
        code.append("sto\n");

        code.append("push $").append(expAddr).append("\n");
        visit(ctx.O2);
        code.append("sto\n");

        code.append("push $").append(resultAddr).append("\n");
        code.append("push 1\n");
        code.append("sto\n");

        code.append(loopLabel).append(":\n");
        code.append("push $").append(expAddr).append("\n");
        code.append("lod\n");
        code.append("push 0\n");
        code.append("grt\n");
        code.append("fjp ").append(endLabel).append("\n");

        code.append("push $").append(resultAddr).append("\n");
        code.append("push $").append(resultAddr).append("\n");
        code.append("lod\n");
        code.append("push $").append(baseAddr).append("\n");
        code.append("lod\n");
        code.append("mul\n");
        code.append("sto\n");

        code.append("push $").append(expAddr).append("\n");
        code.append("push $").append(expAddr).append("\n");
        code.append("lod\n");
        code.append("push 1\n");
        code.append("sub\n");
        code.append("sto\n");

        code.append("ujp ").append(loopLabel).append("\n");
        code.append(endLabel).append(":\n");

        code.append("push $").append(resultAddr).append("\n");
        code.append("lod\n");

        mapper.restore(baseAddr);

        return null;
    }

    @Override
    public Void visitMulDiv(MulDivContext ctx) {
        visit(ctx.O1);
        visit(ctx.O2);
        code.append(ctx.OP.getText().equals("/") ? "div\n" : "mul\n");
        return null;
    }

    @Override
    public Void visitSomaSub(SomaSubContext ctx) {
        visit(ctx.O1);
        visit(ctx.O2);
        code.append(ctx.OP.getText().equals("+") ? "add\n" : "sub\n");
        return null;
    }

    @Override
    public Void visitParenteses(ParentesesContext ctx) {
        visit(ctx.NESTED);
        return null;
    }

    @Override
    public Void visitNumero(NumeroContext ctx) {
        code.append("push ").append(ctx.NUMBER().getText()).append("\n");
        return null;
    }

    @Override
    public Void visitUsoVariavel(UsoVariavelContext ctx) {
        var varName = ctx.ID().getText();
        var tk = ctx.ID().getSymbol();
        var declaracaoOpt = scopes.lookup(varName);

        if (declaracaoOpt.isEmpty()) {
            erroSemantico(
                    "A variavel '%s' nao foi declarada na linha %d e coluna %d."
                            .formatted(varName, tk.getLine(), tk.getCharPositionInLine()));
            return null;
        }

        var address = declaracaoOpt.get().address();
        code.append("push $").append(address).append("\n");
        code.append("lod\n");
        return null;
    }

    @Override
    public Void visitInput(InputContext ctx) {
        code.append("in\n");
        return null;
    }

    @Override
    public Void visitNot(NotContext ctx) {
        visit(ctx.B);
        code.append("not\n");
        return null;
    }

    @Override
    public Void visitAnd(AndContext ctx) {
        visit(ctx.O1);
        visit(ctx.O2);
        code.append("and\n");
        return null;
    }

    @Override
    public Void visitOr(OrContext ctx) {
        visit(ctx.O1);
        visit(ctx.O2);
        code.append("or\n");
        return null;
    }

    @Override
    public Void visitBoolAtomPassthrough(BoolAtomPassthroughContext ctx) {
        visit(ctx.boolAtom());
        return null;
    }

    @Override
    public Void visitBoolParenteses(BoolParentesesContext ctx) {
        visit(ctx.NESTED);
        return null;
    }

    @Override
    public Void visitComparacao(ComparacaoContext ctx) {
        visit(ctx.O1);
        visit(ctx.O2);
        code.append(switch (ctx.OP.getText()) {
            case "<" -> "let\n";
            case ">" -> "grt\n";
            case "<=" -> "lte\n";
            case ">=" -> "gte\n";
            case "==" -> "equ\n";
            case "!=" -> "neq\n";
            default -> {
                erroSemantico("Operador desconhecido: " + ctx.OP.getText());
                yield "";
            }
        });
        return null;
    }

    @Override
    public Void visitBoolTrue(BoolTrueContext ctx) {
        code.append("push true\n");
        return null;
    }

    @Override
    public Void visitBoolFalse(BoolFalseContext ctx) {
        code.append("push false\n");
        return null;
    }
}