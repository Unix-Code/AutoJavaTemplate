package com.davidmalakh.autojavatemplate;

import com.thoughtworks.qdox.model.JavaClass;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author David
 */
public class ClassTemplate extends JavaTemplate {

    public ArrayList<String> addFieldsToTemplate(JavaClass c) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> fields = new ArrayList<>();

        c.getFields().forEach((f) -> {
            fields.add("\t * this." + f.getName() + " --" + f.getType().getGenericValue());
        });

        if (!fields.isEmpty()) {
            template.add("\t * FIELDS:");
            template.addAll(fields);
        }

        return template;
    }

    public ArrayList<String> addMethodsToTemplate(JavaClass c) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> methods = new ArrayList<>();

        c.getMethods().forEach((m) -> {
            if (!m.isAbstract()) {
                String paramList = this.getParamList(m);
                methods.add("\t * this." + m.getName() + "(" + paramList + ") --" + m.getReturns().getGenericValue());
            }
        });

        if (!methods.isEmpty()) {
            template.add("\t * METHODS:");
            template.addAll(methods);
        }

        return template;
    }

    public ArrayList<String> addMethodsFromFieldsToTemplate(List<JavaClass> classes, JavaClass c) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> methodsFromFields = new ArrayList<>();

        c.getFields().forEach((f) -> {
            JavaClass className = classes.stream().filter((cl) -> cl.getName().equals(f.getType().getName())).findFirst().orElse(null);
            if (className != null) {
                f.getType().getMethods().forEach((me) -> {
                    if (!me.isAbstract()) {
                        String paramList = this.getParamList(me);
                        methodsFromFields.add("\t * this." + f.getName() + "." + me.getName() + "(" + paramList + ") --" + me.getReturns().getGenericValue());
                    }
                });
            }
        });

        if (!methodsFromFields.isEmpty()) {
            template.add("\t * METHODS FROM FIELDS:");
            template.addAll(methodsFromFields);
        }

        return template;
    }

    public Map<Integer, ArrayList<String>> addClassTemplates(String path) {
        List<JavaClass> classes = this.getClassesFromFile(path, false);

        Map<Integer, ArrayList<String>> templatesInfo = new HashMap<>();

        classes.forEach((c) -> {
            ArrayList<String> template = new ArrayList<>();

            ArrayList<String> fields = this.addFieldsToTemplate(c);
            ArrayList<String> methods = this.addMethodsToTemplate(c);
            ArrayList<String> methodsFromFields = this.addMethodsFromFieldsToTemplate(classes, c);
            if (!fields.isEmpty() || !methods.isEmpty() || !methodsFromFields.isEmpty()) {
                template.add("\t/*-");
                template.addAll(fields);
                template.addAll(methods);
                template.addAll(methodsFromFields);
                if (!c.getSuperJavaClass().getName().equals(c.getName()) && !c.getSuperJavaClass().getName().equals("Object")) {
                    template.add("\t *");
                    template.add("\t * PLUS EVERYTHING FROM SUPER CLASS:  " + c.getSuperJavaClass().getSimpleName());
                }
                template.add("\t */");
            }
            templatesInfo.put(c.getLineNumber(), template);
        });

        return templatesInfo;
    }
}
