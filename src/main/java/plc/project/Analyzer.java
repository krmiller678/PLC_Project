package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method m : ast.getMethods()) {
            visit(m);
        }

        requireAssignable(Environment.Type.INTEGER, scope.lookupFunction("main", 0).getReturnType());

        return null; // TODO
    }

    @Override
    public Void visit(Ast.Field ast) {
        String name = ast.getName();
        Environment.Type type = Environment.getType(ast.getTypeName());

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get()); // have to visit before variable is defined
            requireAssignable(type, ast.getValue().get().getType());
        }

        if (ast.getConstant()) {
            if (ast.getValue().isEmpty()) {
                throw new RuntimeException("Constant field must be initialized");
            }
        }

        ast.setVariable(scope.defineVariable(name, name, type, ast.getConstant(), Environment.NIL));

        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        String name = ast.getName();
        Environment.Type returnType = Environment.Type.NIL;
        List<Environment.Type> paramTypes = new ArrayList<>();

        if (ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }
        for (String paramType : ast.getParameterTypeNames()) {
            paramTypes.add(Environment.getType(paramType));
        }

        Environment.Function func = scope.defineFunction(name, name, paramTypes, returnType,args -> Environment.NIL);
        ast.setFunction(func);

        try {
            scope = new Scope(scope);
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
                if (statement instanceof Ast.Statement.Return) {
                    requireAssignable(returnType, ((Ast.Statement.Return) statement).getValue().getType());
                }
            }
        }
        finally {
            scope = scope.getParent();
        }

        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expected an Ast.Expression.Function");
        }
        visit(ast.getExpression());
        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name = ast.getName();
        String type = "";
        if (ast.getTypeName().isPresent()) {
            type = ast.getTypeName().get();
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get()); // must be visited before variable is defined.
            }
        }
        else if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            type = ast.getValue().get().getType().getName();
        }
        else {
            throw new RuntimeException("No type found");
        }

        Environment.Type officialType = Environment.getType(type);
        ast.setVariable(scope.defineVariable(name, name, officialType, false, Environment.NIL));

        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Expected Ast.Expression.Access");
        }
        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        if (((Ast.Expression.Access) ast.getReceiver()).getVariable().getConstant()) {
            throw new RuntimeException("Assigning to a constant field");
        }

        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);

        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Empty if statement body");
        }
        try {
            scope = new Scope(scope);
            for (Ast.Statement statement : ast.getThenStatements()) {
                visit(statement);
            }
        }
        finally {
            scope = scope.getParent();
        }
        try {
            scope = new Scope(scope);
            for (Ast.Statement statement : ast.getElseStatements()) {
                visit(statement);
            }
        }
        finally {
            scope = scope.getParent();
        }

        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        visit(ast.getInitialization()); // If initialization is present, must be COMPARABLE
        visit(ast.getIncrement()); // If initialization is present, increment needs to be same type
        if (ast.getInitialization() instanceof Ast.Statement.Assignment) {
            requireAssignable(((Ast.Statement.Assignment)(ast.getInitialization())).getReceiver().getType(), Environment.Type.COMPARABLE);
            requireAssignable(((Ast.Statement.Assignment)(ast.getIncrement())).getReceiver().getType(), ((Ast.Statement.Assignment)(ast.getInitialization())).getReceiver().getType());
        }

        visit(ast.getCondition());
        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);

        if (ast.getStatements().isEmpty()) {
            throw new RuntimeException("Empty for loop body");
        }

        try {
            scope = new Scope(scope);
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        }
        finally {
            scope = scope.getParent();
        }

        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition()); // to set type
        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);
        try {
            scope = new Scope(scope);
            ast.getStatements().forEach(this::visit);
        }
        finally {
            scope = scope.getParent();
        }
        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object lit = ast.getLiteral();
        if (lit instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (lit instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else if (lit instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (lit == null) {
            ast.setType(Environment.Type.NIL);
        }
        else if (lit instanceof BigInteger) {
            if (((BigInteger) lit).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) < 1){
                ast.setType(Environment.Type.INTEGER);
            }
            else {
                throw new RuntimeException("BigInteger Literal exceeds Integer.MAX_VALUE");
            }
        }
        else if (lit instanceof BigDecimal) { // TODO still need to check notes on this
            if (((BigDecimal) lit).compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) < 1){
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("BigDecimal Literal exceeds Double.MAX_VALUE");
            }
        }

        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (ast.getExpression() instanceof Ast.Expression.Binary) {
            throw new RuntimeException("Contained expression is not binary ");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
        // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String op = ast.getOperator();
        Ast.Expression left = ast.getLeft();
        visit(left);
        Ast.Expression right = ast.getRight();
        visit(right);

        switch (op) {
            case "AND", "OR": // TODO Remove for final submission && / ||
                requireAssignable(left.getType(), Environment.Type.BOOLEAN);
                requireAssignable(right.getType(), Environment.Type.BOOLEAN);
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "<", "<=", ">", ">=", "==", "!=":
                requireAssignable(left.getType(), Environment.Type.COMPARABLE);
                requireAssignable(right.getType(), Environment.Type.COMPARABLE);
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                if (left.getType() == Environment.Type.STRING) {
                    ast.setType(Environment.Type.STRING);
                }
                else if (left.getType() == Environment.Type.INTEGER) {
                    requireAssignable(right.getType(), Environment.Type.INTEGER);
                    ast.setType(Environment.Type.INTEGER);
                }
                else if (left.getType() == Environment.Type.DECIMAL) {
                    requireAssignable(right.getType(), Environment.Type.DECIMAL);
                    ast.setType(Environment.Type.DECIMAL);
                }
                else
                    throw new RuntimeException("Not BigInt, BigDec, or String");
                break;
            case "-", "*", "/":
                if (left.getType() == Environment.Type.INTEGER) {
                    requireAssignable(right.getType(), Environment.Type.INTEGER);
                    ast.setType(Environment.Type.INTEGER);
                }
                else if (left.getType() == Environment.Type.DECIMAL) {
                    requireAssignable(right.getType(), Environment.Type.DECIMAL);
                    ast.setType(Environment.Type.DECIMAL);
                }
                else
                    throw new RuntimeException("Not BigInt, BigDec");
                break;
            default:
                throw new RuntimeException("Expected a valid operator, got " + op);
        }
        return null; // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        Environment.Variable var;
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            var = ast.getReceiver().get().getType().getField(ast.getName());
        }
        else {
            var = scope.lookupVariable(ast.getName());
        }

        ast.setVariable(var);

        return null;// TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function func;

        String name = ast.getName(); // name of the function (identifier)
        List<Ast.Expression> arguments = ast.getArguments(); // arguments to be passed in

        if (ast.getReceiver().isPresent()) { // it is a method, first argument needs to be 'this'
            visit(ast.getReceiver().get()); // have to visit to ensure that type is set
            func = ast.getReceiver().get().getType().getFunction(ast.getName(), arguments.size());
        }
        else {
            func = scope.lookupFunction(name, arguments.size());
        }

        for (int i = 0; i < arguments.size(); i++){
            visit(arguments.get(i)); // to ensure type is assigned
            requireAssignable(func.getParameterTypes().get(i), arguments.get(i).getType());
        }
        ast.setFunction(func);  // TODO
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(type)) {
            // good to go
        }
        else if (target == Environment.Type.ANY) {
            // good to go
        }
        else if (target == Environment.Type.COMPARABLE) {
            if (type.getScope().getParent() != Environment.Type.COMPARABLE.getScope()) {
                throw new RuntimeException("Target type does not match assignment: " + target);
            }
        }
        else {
            throw new RuntimeException("Target type does not match assignment: " + target);
        }

    }

}
