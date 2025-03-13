package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), false, visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), false, Environment.NIL);
        }
        return Environment.NIL;
        //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
        //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(Environment.create(visit(ast.getValue())));  //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
        //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String op = ast.getOperator();
        Ast.Expression left = ast.getLeft();
        Ast.Expression right = ast.getRight();
        Environment.PlcObject toReturn;

        switch (op) {
            case "&&", "AND":
                Boolean l = requireType(Boolean.class, visit(left));
                Boolean r =requireType(Boolean.class, visit(right));
                toReturn = Environment.create(Boolean.logicalAnd(l, r));
                return toReturn;
            case "||", "OR":
                Boolean l1 = requireType(Boolean.class, visit(left));
                if (l1.booleanValue()) {
                    toReturn = Environment.create(Boolean.TRUE);
                    return toReturn;
                }
                Boolean r1 =requireType(Boolean.class, visit(right));
                toReturn = Environment.create(Boolean.logicalOr(l1, r1));
                return toReturn;
            case "<":
                Comparable llt = requireType(Comparable.class, visit(left));
                Comparable rlt = requireType(llt.getClass(), visit(right));
                toReturn = Environment.create(llt.compareTo(rlt) < 0);
                return toReturn;
            case "<=":
                Comparable llte = requireType(Comparable.class, visit(left));
                Comparable rlte = requireType(llte.getClass(), visit(right));
                toReturn = Environment.create(llte.compareTo(rlte) <= 0);
                return toReturn;
            case ">":
                Comparable lgt = requireType(Comparable.class, visit(left));
                Comparable rgt = requireType(lgt.getClass(), visit(right));
                toReturn = Environment.create(lgt.compareTo(rgt) > 0);
                return toReturn;
            case ">=":
                Comparable lgte = requireType(Comparable.class, visit(left));
                Comparable rgte = requireType(lgte.getClass(), visit(right));
                toReturn = Environment.create(lgte.compareTo(rgte) >= 0);
                return toReturn;
            case "==":
                toReturn = Environment.create(visit(left).getValue() == visit(right).getValue());
                return toReturn;
            case "!=":
                toReturn = Environment.create(visit(left).getValue() != visit(right).getValue());
                return toReturn;
            case "+":
                if (visit(left).getValue() instanceof BigDecimal) {
                    BigDecimal bd = (BigDecimal) visit(left).getValue();
                    BigDecimal bd2 = requireType(BigDecimal.class, visit(right));
                    toReturn = Environment.create(bd.add(bd2));
                }
                else if (visit(left).getValue() instanceof BigInteger) {
                    BigInteger bi = (BigInteger) visit(left).getValue();
                    BigInteger bi2 = requireType(BigInteger.class, visit(right));
                    toReturn = Environment.create(bi.add(bi2));
                }
                else if (visit(left).getValue() instanceof String) {
                    String s = (String) visit(left).getValue();
                    String s2 = requireType(String.class, visit(right));
                    toReturn = Environment.create(s+s2);
                }
                else
                    throw new RuntimeException("Not BigInt, BigDec, or String");
                return toReturn;
            case "-":
                if (visit(left).getValue() instanceof BigDecimal) {
                    BigDecimal bd = (BigDecimal) visit(left).getValue();
                    BigDecimal bd2 = requireType(BigDecimal.class, visit(right));
                    toReturn = Environment.create(bd.subtract(bd2));
                }
                else if (visit(left).getValue() instanceof BigInteger) {
                    BigInteger bi = (BigInteger) visit(left).getValue();
                    BigInteger bi2 = requireType(BigInteger.class, visit(right));
                    toReturn = Environment.create(bi.subtract(bi2));
                }
                else
                    throw new RuntimeException("Not BigInt, BigDec");
                return toReturn;
            case "*":
                if (visit(left).getValue() instanceof BigDecimal) {
                    BigDecimal bd = (BigDecimal) visit(left).getValue();
                    BigDecimal bd2 = requireType(BigDecimal.class, visit(right));
                    toReturn = Environment.create(bd.multiply(bd2));
                }
                else if (visit(left).getValue() instanceof BigInteger) {
                    BigInteger bi = (BigInteger) visit(left).getValue();
                    BigInteger bi2 = requireType(BigInteger.class, visit(right));
                    toReturn = Environment.create(bi.multiply(bi2));
                }
                else
                    throw new RuntimeException("Not BigInt, BigDec");
                return toReturn;
            case "/":
                if (visit(left).getValue() instanceof BigDecimal) {
                    BigDecimal bd = (BigDecimal) visit(left).getValue();
                    BigDecimal bd2 = requireType(BigDecimal.class, visit(right));
                    toReturn = Environment.create(bd.divide(bd2, RoundingMode.HALF_EVEN));
                }
                else if (visit(left).getValue() instanceof BigInteger) {
                    BigInteger bi = (BigInteger) visit(left).getValue();
                    BigInteger bi2 = requireType(BigInteger.class, visit(right));
                    toReturn = Environment.create(bi.multiply(bi2));
                }
                else
                    throw new RuntimeException("Not BigInt, BigDec");
                return toReturn;
            default:
                throw new RuntimeException("Expected a valid operator, got " + op);
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        String receiver = "";
        if (ast.getReceiver().isPresent()){
            receiver = (String)visit(ast.getReceiver().get()).getValue();
            return Environment.create(receiver + "." + ast.getName());
        }
        scope.defineVariable(ast.getName(), false, Environment.NIL);
        return Environment.create(ast.getName());
    } //TODO

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
