package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // create a "class Main {"
        print("public class Main {");
        newline(0);

        // ____declare fields -> properties
        indent++;

        if (!ast.getFields().isEmpty()) {
            for (Ast.Field field : ast.getFields()) {
                newline(indent);
                visit(field);
            }
            newline(0);
        }


        // ____declare "public static void main(String[] args) {
        // _________System.exit(new Main().main());
        // ____}"
        newline(indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(0);

        // ____declare each our functions -> methods
        // ____one of our functions -> methods is called main()!
        for (Ast.Method m : ast.getMethods()) {
            newline(indent);
            visit(m);
            newline(0);
        }

        // print "}" to close the class Main
        newline(--indent); // indent should be 0 after decrement here
        print("}");
        return null;
        //TODO
    }

    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getConstant()) {
            print("final ");
        }
        print(ast.getVariable().getType().getJvmName(), " ");
        print(ast.getVariable().getJvmName()); // generating JVM name in case special var identifier used

        if (ast.getValue().isPresent()) {
            print(" = ");
            print(ast.getValue().get());
        }
        print(";");

        return null; //TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        print(ast.getFunction().getType().getJvmName(), " ");
        print(ast.getFunction().getJvmName(),"("); // using JVM name here in case a special method is defined

        if (!ast.getParameters().isEmpty()) {
            print(ast.getFunction().getParameterTypes().get(0).getJvmName(), " ");
            print(ast.getParameters().get(0));
            for (int i = 1; i < ast.getParameters().size(); i++) {
                print(", ");
                print(ast.getFunction().getParameterTypes().get(i).getJvmName(), " ");
                print(ast.getParameters().get(i));
            }
        }
        print(") {");

        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");

        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression());
        print(";");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(),
                " ",
                ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");

        return null;
        //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ");
        print(ast.getValue());
        print(";");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (", ast.getCondition(), ") {");
        if (!ast.getThenStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getThenStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getThenStatements().get(i));
            }
            newline(--indent);
        }
        print("}");

        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(++indent);
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getElseStatements().get(i));
            }
            newline(--indent);
            print("}");
        }

        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        print("for ( ");
        if (ast.getInitialization() != null) {
            print(ast.getInitialization());
        }
        else {
            print(";");
        }
        print(" ", ast.getCondition(), ";");
        if (ast.getIncrement() != null) {
            print(" ");
            print(((Ast.Statement.Assignment)ast.getIncrement()).getReceiver());
            print(" = ");
            print(((Ast.Statement.Assignment)ast.getIncrement()).getValue());
        }
        // NOTE - If present, the increment must have form "identifier = expression"
        print(" ) {");


        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }

        print("}");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");

        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }

        print("}");
        return null;

        //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        print(ast.getValue());
        print(";");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getType().equals(Environment.Type.STRING)) {
            print("\"", ast.getLiteral(), "\"");
        }
        else if (ast.getType().equals(Environment.Type.CHARACTER)) {
            print("'", ast.getLiteral(), "'");
        }
        else if (ast.getType().equals(Environment.Type.NIL)) {
            print("null");
        }
        else {
            print(ast.getLiteral());
        }

        return null; //TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        print(ast.getExpression());
        print(")");
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        print(ast.getLeft());
        print(" ", ast.getOperator(), " ");
        print(ast.getRight());
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get());
            print(".");
        }
        print(ast.getVariable().getJvmName());
        return null; //TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get());
            print(".");
        }

        print(ast.getFunction().getJvmName(), "(");
        if (!ast.getArguments().isEmpty()) {
            print(ast.getArguments().get(0));
            for (int i = 1; i < ast.getArguments().size(); i++) {
                print(", ");
                print(ast.getArguments().get(i));
            }
        }
        print(")");

        return null; //TODO
    }
}
