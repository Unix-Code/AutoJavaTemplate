package com.davidmalakh.autojavatemplate;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author David
 */
abstract class JavaTemplate {

    public String getParamList(JavaMethod m) {
        return m.getParameters().stream()
                .map((p) -> p.getType().getGenericValue() + " " + p.getName())
                .collect(Collectors.joining(", "));
    }

    public List<JavaClass> getClassesFromFile(String path, boolean includeInterfaces) {
        JavaProjectBuilder builder = new JavaProjectBuilder();
        List<JavaClass> classes = new ArrayList<>();
        JavaSource src;
        try {
            src = builder.addSource(new FileReader(path));
            classes = src.getClasses();
            if (!includeInterfaces) classes = classes.stream().filter((c) -> !c.isInterface()).collect(Collectors.toList());
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        return classes.stream().filter((c) -> !this.isExamplesClass(c)).collect(Collectors.toList());
    }
    
    private boolean isExamplesClass(JavaClass c) {
        return c.getName().startsWith("Example");
    }
}
