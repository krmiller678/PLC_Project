package plc.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {

    private final CharStream chars;
    private final List<Token> tokens = new ArrayList<Token>();

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        while (chars.has(0)) {
            char c = chars.get(0);
            switch(c) {
                case ' ':
                case '\b':
                case '\n':
                case '\r':
                case '\t':
                    lexEscape(); break;
                default:
                    tokens.add(lexToken()); break;
            }
        }
        return tokens;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {

        // Identifier
        boolean result = peek("[A-za-z]");
        if (result) { return lexIdentifier(); }

        // Number starting with + or -
        result = peek("[+-]", "[0-9]");
        if (result) { return lexNumber(); }

        // Number starting with digit
        result = peek("[0-9]");
        if (result) { return lexNumber(); }

        // Character
        result = peek("'");
        if (result) { return lexCharacter(); }

        // String
        result = peek("\"");
        if (result) { return lexString(); }

        result = peek("[<>!=]") || peek(".");
        if (result) { return lexOperator(); }

        throw new ParseException("Error at ", chars.index);
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexIdentifier() {
        match("[A-za-z]"); // eat up first letter
        while (match("[A-Za-z0-9_-]")) {}
        return chars.emit(Token.Type.IDENTIFIER);
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexNumber() {
        match("[+-]"); // Eat up plus or minus at start
        if (peek("0"))
        {
            match("0"); // Eat up leading 0
            if (!peek("\\.", "\\d"))
                return chars.emit(Token.Type.INTEGER);
            else {
                match("\\.");
                while (match("\\d")) {} // continue to match digits
                return chars.emit(Token.Type.DECIMAL);
            }
        }
        else if (peek("\\d")) {
            while (match("\\d")) {}
            if (!peek("\\.", "\\d")) {
                return chars.emit(Token.Type.INTEGER);
            }
            match("\\.");
            while (match("\\d")) {}
            return chars.emit(Token.Type.DECIMAL);
        }
        throw new ParseException("Error at ", chars.index);
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexCharacter() {
        match("'"); // Eat up first single quote
        if (peek("[^'\\n\\r\\\\]")) {
            match("[^'\\n\\r\\\\]");
            if (peek("'")) {
                match("'");
                return chars.emit(Token.Type.CHARACTER);
            }
        }
        else if (peek("\\\\", "[bnrt'\"\\\\]")) {
            match("\\\\", "[bnrt'\"\\\\]");
            if (peek("'")) {
                match("'");
                return chars.emit(Token.Type.CHARACTER);
            }
        }
            throw new ParseException("Error at ", chars.index);
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexString() {
        match("\""); // Eat up first double quote
        while (match("[^\"\\n\\r\\\\]") || match("\\\\", "[bnrt'\"\\\\]"))  {}
        if (peek("\"")) {
            match("\"");
            return chars.emit(Token.Type.STRING);
        }
        throw new ParseException("Error at ", chars.index);
        //throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        chars.advance();
        chars.skip();
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        if (peek("[<>!=]")) {
            match("[<>!=]");
            match("=");
        }
        else {
            match(".");
        }
        return chars.emit(Token.Type.OPERATOR);
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        // Stick to peeking one character at a time: "//d" gives us digit
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
        //throw new UnsupportedOperationException();
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
        //throw new UnsupportedOperationException();
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
