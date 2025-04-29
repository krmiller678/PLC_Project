package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class EndToEndGeneratorTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, String input, String expected) {
        test(input, expected, new Scope(null), Parser::parseSource);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Hello, World!",
                        // DEF main(): Integer DO
                        //     print("Hello, World!");
                        //     RETURN 0;
                        // END
                        "DEF main(): Integer DO\n    print(\"Hello, World!\");\n    RETURN 0;\nEND",
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Multiple Fields and Methods",
                            //LET x: Integer;
                            //LET y: Decimal;
                            //LET z: String;
                            //DEF f(): Integer DO RETURN x; END
                            //DEF g(): Decimal DO RETURN y; END
                            //DEF h(): String DO RETURN z; END
                            //DEF main(): Integer DO END
                        "LET x: Integer;\nLET y: Decimal;\nLET z: String;\nDEF f(): Integer DO RETURN x; END\n" +
                                "DEF g(): Decimal DO RETURN y; END\nDEF h(): String DO RETURN z; END\nDEF main(): Integer DO END",
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    int x;",
                                "    double y;",
                                "    String z;",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int f() {",
                                "        return x;",
                                "    }",
                                "",
                                "    double g() {",
                                "        return y;",
                                "    }",
                                "",
                                "    String h() {",
                                "        return z;",
                                "    }",
                                "",
                                "    int main() {}",
                                "",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethod(String test, String input, String expected) {
        test(input, expected, new Scope(null), Parser::parseMethod);
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
                Arguments.of("Square",
                        // LET name: Integer;
                        "DEF square(num: Decimal): Decimal DO\n" +
                        "    RETURN num * num;\n" +
                        "END",
                        String.join(System.lineSeparator(),
                                "double square(double num) {",
                                "    return num * num;",
                                "}"
                        )
                ),
                Arguments.of("Multiple Statements",
                        // LET name: Integer;
                        "DEF func(x: Integer, y: Decimal, z: String) DO\n" +
                                "    print(x);\n" +
                                "    print(y);\n" +
                                "    print(z);\n" +
                                "END",
                        String.join(System.lineSeparator(),
                                "Void func(int x, double y, String z) {",
                                "    System.out.println(x);",
                                "    System.out.println(y);",
                                "    System.out.println(z);",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhileStatement(String test, String input, String expected) {
        test(input, expected, init(new Scope(null), scope -> {
            scope.defineVariable("num", "num", Environment.Type.INTEGER, false, Environment.NIL);
        }), Parser::parseStatement);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("Multiple Statements",
                        "WHILE num < 10 DO\n" +
                                "    print(num + \"\\n\");\n" +
                                "    num = num + 1;\n" +
                                "END",
                        String.join(System.lineSeparator(),
                                "while (num < 10) {",
                                "    System.out.println(num + \"\\n\");",
                                "    num = num + 1;",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, String input, String expected) {
        test(input, expected, new Scope(null), Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        "LET name: Integer;",
                        "int name;"
                ),
                Arguments.of("Initialization",
                        // LET name = 1.0;
                        "LET name = 1.0;",
                        "double name = 1.0;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, String input, String expected) {
        test(input, expected, new Scope(null), Parser::parseStatement);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        // IF TRUE DO
                        //     print(1);
                        // END
                        "IF TRUE DO\n    print(1);\nEND",
                        String.join(System.lineSeparator(),
                                "if (true) {",
                                "    System.out.println(1);",
                                "}"
                        )
                ),
                Arguments.of("Else",
                        // IF FALSE DO
                        //     print(1);
                        // ELSE
                        //     print(0);
                        // END
                        "IF FALSE DO\n    print(1);\nELSE\n    print(0);\nEND",
                        String.join(System.lineSeparator(),
                                "if (false) {",
                                "    System.out.println(1);",
                                "} else {",
                                "    System.out.println(0);",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testForStatement(String test, String input, String expected) {
        test(input, expected, init(new Scope(null), scope -> {
            scope.defineVariable("num", "num", Environment.Type.INTEGER, false, Environment.NIL);
        }), Parser::parseStatement);
    }

    private static Stream<Arguments> testForStatement() {
        return Stream.of(
                Arguments.of("For",
                        // FOR (num = 0; num < 5; num = num + 1)
                        //     print(num);
                        // END
                        "FOR (num = 0; num < 5; num = num + 1)\n    print(num);\nEND",
                        String.join(System.lineSeparator(),
                                "for ( num = 0; num < 5; num = num + 1 ) {",
                                "    System.out.println(num);",
                                "}"
                        )
                ),
                Arguments.of("Condition",
                        // FOR (num = 0; num < 5; num = num + 1)
                        //     print(num);
                        // END
                        "FOR (; num < 5;)\n" +
                                "    print(num);\n" +
                                "    num = num + 1;\n" +
                                "END ",
                        String.join(System.lineSeparator(),
                                "for ( ; num < 5; ) {",
                                "    System.out.println(num);",
                                "    num = num + 1;",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, String input, String expected) {
        test(input, expected, new Scope(null), Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        // TRUE && FALSE
                        "TRUE && FALSE",
                        "true && false"
                ),
                Arguments.of("Concatenation",
                        // "Ben" + 10
                        "\"Ben\" + 10",
                        "\"Ben\" + 10"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, String input, String expected) {
        test(input, expected, new Scope(null), Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Print",
                        // print("Hello, World!")
                        "print(\"Hello, World!\")",
                        "System.out.println(\"Hello, World!\")"
                )
        );
    }

    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static <T extends Ast> void test(String input, String expected, Scope scope, Function<Parser, T> function) {
        StringWriter writer = new StringWriter();
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer.lex());

        Ast ast = function.apply(parser);

        Analyzer analyzer = new Analyzer(scope);
        analyzer.visit(ast);
        new Generator(new PrintWriter(writer)).visit(ast);
        Assertions.assertEquals(expected, writer.toString());
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }
}
