package io.mitallast.lambda;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GenerateProduct {
    public static void main(String... args) throws IOException {
        for (int i = 1; i <= 22; i++) {
            generateFunction(i);
        }
    }

    private static void generateFunction(int arity) throws IOException {
        var builder = new StringBuilder();
        builder.append("package io.mitallast.lambda;\n\n");
        builder.append("import io.mitallast.hlist.*;\n");
        builder.append("\n");
        builder.append("@FunctionalInterface\n");
        builder.append("public interface Function");
        builder.append(arity);
        builder.append("<");
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i);
        }
        builder.append(", A> {\n\n");

        // A apply(T$ t$)

        builder.append("    A apply(");
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i).append(" t").append(i);
        }
        builder.append(");\n\n");

        // curried functions

        for (int c = arity - 1; c >= 1; c--) { // c = curried
            builder.append("    default Function").append(c).append("<");
            for (int i = arity - c + 1; i <= arity; i++) {
                builder.append('T').append(i).append(", ");
            }
            builder.append("A> apply(");
            for (int i = 1; i <= arity - c; i++) {
                if (i > 1) builder.append(", ");
                builder.append('T').append(i).append(" t").append(i);
            }
            builder.append(") {\n");
            builder.append("        return (");
            for (int i = arity - c + 1; i <= arity; i++) {
                if (i > arity - c + 1) builder.append(", ");
                builder.append('t').append(i);
            }
            builder.append(") -> apply(");
            for (int i = 1; i <= arity; i++) {
                if (i > 1) builder.append(", ");
                builder.append("t").append(i);
            }
            builder.append(");\n");
            builder.append("    }\n\n");
        }

        // A apply(HList)

        builder.append("    default A apply(");
        renderHListType(builder, arity);
        builder.append(" hlist").append(arity);
        builder.append(") {\n");
        for (int i = arity - 1; i > 0; i--) {
            builder.append("        var hlist").append(i).append(" = hlist").append(i + 1).append(".tail();\n");
        }
        builder.append("        return apply(");
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("hlist").append(i).append(".head()");
        }
        builder.append(");\n");
        builder.append("    }\n");


        builder.append("}\n"); // end of class

        System.out.println(builder.toString());

        var file = new File("src/main/java/io/mitallast/lambda/Function" + arity + ".java");
        Files.writeString(file.toPath(), builder);
    }

    private static void renderHListType(StringBuilder builder, int arity) {
        if (arity == 0) {
            builder.append("HNil");
        } else {
            builder.append("HCons<T").append(arity).append(", ");
            renderHListType(builder, arity - 1);
            builder.append(">");
        }
    }
}
