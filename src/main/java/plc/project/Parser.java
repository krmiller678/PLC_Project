package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        // Initialize source to return
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();
        Ast.Source source = new Ast.Source(fields, methods);

        while (tokens.has(0)) {
            Token t = tokens.get(0);
            if (peek("LET")) {
                fields.add(parseField());
            }
            else if (peek("DEF")) {
                methods.add(parseMethod());
            }
            else {
                throw new ParseException("Expected a field or method at index: ", tokens.get(0).getIndex());
            }
        }
        return source;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        String name = "";
        Ast.Field field = new Ast.Field(name, true,Optional.empty());

        match("LET");
        if (peek("CONST")) {
            match("CONST");
        }
        // move on to identifier //TODO
        return field;
        //throw new UnsupportedOperationException();
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        String name = "";
        List<String> parameters = new ArrayList<>();
        List<Ast.Statement> statements = new ArrayList<>();
        Ast.Method method = new Ast.Method(name, parameters, statements );

        match("DEF");
        // move on to identifier TODO

        return method;

        //throw new UnsupportedOperationException();
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")) {
            return parseDeclarationStatement(); // match ';' in method
        }
        else if (peek("IF")) {
            return parseIfStatement(); // match ';' in method
        }
        else if (peek("FOR")) {
            return parseForStatement(); // match ';' in method
        }
        else if (peek("WHILE")) {
            return parseWhileStatement(); // match ';' in method
        }
        else if (peek("RETURN")) {
            return parseReturnStatement(); // match ';' in method
        }
        else {
            Ast.Expression expr = parseExpression();
            if (match("=")) {
                Ast.Expression expr2 = parseExpression();
                if (!match(";")) {
                    throw new ParseException("Expected semicolon at index: ", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
                return new Ast.Statement.Assignment(expr, expr2);
            }
            if (!match(";")) {
                throw new ParseException("Expected semicolon at index: ", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }
            return new Ast.Statement.Expression(expr);
        }

        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression expr = parseEqualityExpression();
        while (match("AND") || match("OR") || match("&&") || match("||")) {
            // Added in match on "||" and "&&" because test cases don't align with project spec
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseEqualityExpression();
            expr = new Ast.Expression.Binary(operator,expr,right);
        }
        return expr;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression expr = parseAdditiveExpression();
        while (match("<") || match("<=") || match(">") || match(">=") || match("==") || match("!=") ) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            expr = new Ast.Expression.Binary(operator,expr,right);
        }
        return expr;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression expr = parseMultiplicativeExpression();
        while (match("+") || match("-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            expr = new Ast.Expression.Binary(operator,expr,right);
        }
        return expr;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression expr = parseSecondaryExpression();
        while (match("*") || match("/")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseSecondaryExpression();
            expr = new Ast.Expression.Binary(operator,expr,right);
        }
        return expr;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression expr = parsePrimaryExpression();
        while (match(".")) {
            if (match(Token.Type.IDENTIFIER)) {
                String name = tokens.get(-1).getLiteral();
                if (match("(")) {
                    List<Ast.Expression> args = new ArrayList<>();
                    if (!match(")")) {
                        do {
                            Ast.Expression expr2 = parseExpression();
                            args.add(expr2);
                        } while (match(","));

                        if (!match(")")) {
                            throw new ParseException("Expected ')' at index: ",
                                    tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                        }
                    }
                    expr = new Ast.Expression.Function(Optional.of(expr), name, args);
                }
                else {
                    expr = new Ast.Expression.Access(Optional.of(expr), name);
                }
            }
            else {
                throw new ParseException("Expected identifier at index: ",
                        tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }
        }
        return expr;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("TRUE")) {
            return new Ast.Expression.Literal(Boolean.TRUE);
        }
        else if (match("FALSE")) {
            return new Ast.Expression.Literal(Boolean.FALSE);
        }
        else if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        }
        else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.CHARACTER)) {
            String c = tokens.get(-1).getLiteral();
            c = c.substring(1, c.length()-1);
            c = c.replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\r", "\r")
                    .replace("\\b", "\b")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            return new Ast.Expression.Literal(c.charAt(0));
        }
        else if (match(Token.Type.STRING)) {
            String s = tokens.get(-1).getLiteral();
            s = s.substring(1, s.length()-1);
            s = s.replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\r", "\r")
                    .replace("\\b", "\b")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            return new Ast.Expression.Literal(s);
        }
        else if (match("(")) {
            Ast.Expression expr = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected ')' at index: ",
                        tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }
            return new Ast.Expression.Group(expr);
        }
        else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if (match("(")) {
                List<Ast.Expression> args = new ArrayList<>();
                if (match(")")) {return new Ast.Expression.Function(Optional.empty(),name, args);}
                do {
                    Ast.Expression expr2 = parseExpression();
                    args.add(expr2);
                } while (match(","));

                if (!match(")")) {
                    throw new ParseException("Expected ')' at index: ",
                            tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                return new Ast.Expression.Function(Optional.empty(),name,args);
            }

            return new Ast.Expression.Access(Optional.empty(), tokens.get(-1).getLiteral());
        }
        else {
            throw new ParseException("End of Syntax Tree Error at: ",
                    tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i <patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
        //throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
        //throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
