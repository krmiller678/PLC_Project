import plc.project.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Harness {
    public static void main(String[] args) {
        String source_file = args[0];
        String java_file = new String("Main.java");
        String source = new String();
        Scanner scanner;

        System.out.println("Generate Java source from " + source_file);

        try {
            File file = new File(source_file);
            scanner = new Scanner(file);

            StringBuilder source_contents = new StringBuilder((int) file.length());

            while (scanner.hasNextLine()) {
                source_contents.append(scanner.nextLine() + System.lineSeparator());
            }

            source = source_contents.toString();
            scanner.close();
        } catch (FileNotFoundException e) {}

        Lexer lexer = new Lexer(source);
        System.out.println("Lexing...");

        Parser parser = new Parser(lexer.lex());
        Ast.Source ast = parser.parseSource();
        System.out.println("Parsing Complete");

        Interpreter interpreter = new Interpreter(null);
        Environment.PlcObject visit = interpreter.visit(ast);
        System.out.println(visit.getValue());

    }
}
