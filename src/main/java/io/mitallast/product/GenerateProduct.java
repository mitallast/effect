package io.mitallast.product;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GenerateProduct {
    public static void main(String... args) throws IOException {
        for (int i = 1; i <= 22; i++) {
            generateProduct(i);
            generateTuple(i);
        }
        generateTupleBase();
        generateHList();
    }

    private static void generateProduct(int arity) throws IOException {
        var builder = new StringBuilder();
        builder.append("package io.mitallast.product;\n\n");
        builder.append("import io.mitallast.lambda.Function").append(arity).append(";\n");
        builder.append("import io.mitallast.hlist.*;\n");
        builder.append("\n");
        builder.append("public abstract class Product");
        builder.append(arity);
        builder.append("<");
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i);
        }
        builder.append("> implements Product {\n");

        // properties

        for (int i = 1; i <= arity; i++) {
            builder.append("    private final T").append(i).append(" t").append(i).append(";\n");
        }
        builder.append("\n");

        // constructor

        builder.append("    protected Product").append(arity).append('(');
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i).append(" t").append(i);
        }
        builder.append(") {\n");
        for (int i = 1; i <= arity; i++) {
            builder.append("        this.t").append(i).append(" = t").append(i).append(";\n");
        }
        builder.append("    }\n\n");

        // Object productElement(int n);

        builder.append("    @Override\n");
        builder.append("    public Object productElement(int n) {\n");
        builder.append("        switch (n) {\n");
        for (int i = 1; i <= arity; i++) {
            builder.append("            case ").append(i).append(":\n");
            builder.append("                return t").append(i).append(";\n");
        }
        builder.append("            default:\n");
        builder.append("                throw new IndexOutOfBoundsException(n);\n");
        builder.append("        }\n");
        builder.append("    }\n\n");

        // int productArity();

        builder.append("    @Override\n");
        builder.append("    public int productArity() {\n");
        builder.append("        return ").append(arity).append(";\n");
        builder.append("    }\n\n");

        // T1 t1()

        for (int i = 1; i <= arity; i++) {
            builder.append("    public T").append(i).append(" t").append(i).append("() {\n");
            builder.append("        return t").append(i).append(";\n");
            builder.append("    }\n\n");
        }

        // toTuple()

        builder.append("    public Tuple").append(arity).append('<');
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i);
        }
        builder.append("> toTuple() {\n");
        builder.append("        return new Tuple").append(arity).append("<>(");
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("t").append(i);
        }
        builder.append(");\n");
        builder.append("    }\n\n");

        // to[A](FunctionT<T1, T2, .. A>)])

        builder.append("    public <A> A to(Function").append(arity).append('<');
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i);
        }
        builder.append(", A> f) {\n");
        builder.append("        return f.apply(");
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("t").append(i);
        }
        builder.append(");\n");
        builder.append("    }\n\n");

        // toHList()

        builder.append("    public ");
        renderHListType(builder, arity);
        builder.append(" toHList() {\n");
        builder.append("        return HList.nil");
        for (int i = 1; i <= arity; i++) {
            builder.append("\n");
            builder.append("            .prepend(t").append(i).append(")");
        }
        builder.append(";\n");
        builder.append("    }\n\n");

        // toString()

        builder.append("    @Override\n");
        builder.append("    public String toString() {\n");
        builder.append("        StringBuilder builder = new StringBuilder();\n");
        builder.append("        builder.append('(');\n");
        for (int i = 1; i <= arity; i++) {
            builder.append("        builder");
            if (i > 1) builder.append(".append(',')");
            builder.append(".append(t").append(i).append(");\n");
        }
        builder.append("        builder.append(')');\n");
        builder.append("        return builder.toString();\n");
        builder.append("    }\n\n");

        // static fromHList()

        builder.append("}\n"); // end of class

        System.out.println(builder.toString());

        var file = new File("src/main/java/io/mitallast/product/Product" + arity + ".java");
        Files.writeString(file.toPath(), builder);
    }

    private static void generateTupleBase() throws IOException {
        var builder = new StringBuilder();
        builder.append("package io.mitallast.product;\n\n");

        builder.append("public interface Tuple extends Product {\n\n");

        for (int arity = 1; arity <= 22; arity++) {
            builder.append("    static ");
            renderGenericType(builder, arity);
            builder.append(" Tuple").append(arity);
            renderGenericType(builder, arity);
            builder.append(" of(");
            for (int i = 1; i <= arity; i++) {
                if (i > 1) builder.append(", ");
                builder.append("T").append(i).append(" t").append(i);
            }
            builder.append(") {\n");
            builder.append("        return new Tuple").append(arity).append("<>(");
            for (int i = 1; i <= arity; i++) {
                if (i > 1) builder.append(", ");
                builder.append("t").append(i);
            }
            builder.append(");\n");
            builder.append("    }\n\n");
        }

        builder.append("}\n\n"); // end of class

        var file = new File("src/main/java/io/mitallast/product/Tuple.java");
        Files.writeString(file.toPath(), builder);
    }

    private static void generateTuple(int arity) throws IOException {
        var builder = new StringBuilder();
        builder.append("package io.mitallast.product;\n\n");

        builder.append("public final class Tuple").append(arity).append('<');
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i);
        }
        builder.append("> extends Product").append(arity).append('<');
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i);
        }
        builder.append("> implements Tuple {\n");

        // constructor

        builder.append("    protected Tuple").append(arity).append('(');
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i).append(" t").append(i);
        }
        builder.append(") {\n");
        builder.append("        super(");
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append('t').append(i);
        }
        builder.append(");\n");
        builder.append("    }\n");
        builder.append("}\n\n"); // end of class

        var file = new File("src/main/java/io/mitallast/product/Tuple" + arity + ".java");
        Files.writeString(file.toPath(), builder);
    }

    private static void generateHList() throws IOException {
        var builder = new StringBuilder();
        builder.append("package io.mitallast.hlist;\n\n");
        builder.append("import io.mitallast.lambda.*;\n");
        builder.append("\n");
        builder.append("public interface HList {\n");
        builder.append("    HNil nil = new HNil();\n\n");

        // fromHList()

        for (int arity = 1; arity <= 22; arity++) {

            builder.append("    static ");
            renderGenericTypeWithA(builder, arity);
            builder.append(" A apply(");
            renderHListType(builder, arity);
            builder.append(" hlist").append(arity).append(", ");
            renderAFunctionType(builder, arity);
            builder.append(" f");
            builder.append(") {\n");
            for (int i = arity - 1; i > 0; i--) {
                builder.append("        var hlist").append(i).append(" = hlist").append(i + 1).append(".tail();\n");
            }

            builder.append("        return f.apply(");
            for (int i = 1; i <= arity; i++) {
                if (i > 1) builder.append(", ");
                builder.append("hlist").append(i).append(".head()");
            }
            builder.append(");\n");
            builder.append("    }\n\n");
        }

        builder.append("}\n\n"); // end of class
        var file = new File("src/main/java/io/mitallast/hlist/HList.java");
        Files.writeString(file.toPath(), builder);
    }

    private static void renderAFunctionType(StringBuilder builder, int arity) {
        builder.append("Function").append(arity);
        renderGenericTypeWithA(builder, arity);
    }

    private static void renderGenericTypeWithA(StringBuilder builder, int arity) {
        builder.append('<');
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i);
        }
        builder.append(", A>");
    }

    private static void renderGenericType(StringBuilder builder, int arity) {
        builder.append('<');
        for (int i = 1; i <= arity; i++) {
            if (i > 1) builder.append(", ");
            builder.append("T").append(i);
        }
        builder.append(">");
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
