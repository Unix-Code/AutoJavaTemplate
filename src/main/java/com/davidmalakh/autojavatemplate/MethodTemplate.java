package com.davidmalakh.autojavatemplate;

import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author David
 */
public class MethodTemplate extends JavaTemplate {

    public Map<Integer, ArrayList<String>> addAllMethodTemplates(String path) {
        Map<Integer, ArrayList<String>> allMethodTemplatesInfo = new HashMap<>();
        List<JavaClass> classes = this.getClassesFromFile(path, false);
        List<JavaClass> classesAndInterfaces = this.getClassesFromFile(path, true);

        classes.forEach((c) -> allMethodTemplatesInfo.putAll(this.addClassMethodTemplates(c, classesAndInterfaces)));

        return allMethodTemplatesInfo;
    }

    public Map<Integer, ArrayList<String>> addClassMethodTemplates(JavaClass c, List<JavaClass> classes) {
        Map<Integer, ArrayList<String>> classMethodTemplatesInfo = new HashMap<>();
        c.getMethods().stream().filter((m) -> !m.isAbstract()).collect(Collectors.toList()).forEach((m) -> {
            ArrayList<String> template = new ArrayList<>();

            ArrayList<String> methodParams = this.addMethodParams(m);
            ArrayList<String> fieldsFromParams = this.addFieldsFromParams(m, classes);
            ArrayList<String> methodsFromParams = this.addMethodsFromParams(m, classes);
            if (/*!fieldsFromParams.isEmpty() || */!methodParams.isEmpty() || !methodsFromParams.isEmpty()) {
                template.add("\t\t/*-");
                template.addAll(methodParams);
//                template.addAll(fieldsFromParams);
                template.addAll(methodsFromParams);
                template.add("\t\t */");
            }
            classMethodTemplatesInfo.put(m.getLineNumber(), template);
        });

        return classMethodTemplatesInfo;
    }

    public ArrayList<String> addMethodParams(JavaMethod m) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> methodParams = new ArrayList<>();
        
        m.getParameters().forEach((p) -> methodParams.add("\t\t * " + p.getName() + " --" + p.getType().getGenericValue()));
        
        if (!methodParams.isEmpty()) {
            template.add("\t\t * PARAMS:");
            template.addAll(methodParams);
        }
        else {
            template.add("\t\t * EVERYTHING FROM CLASS TEMPLATE");
        }
        
        return template;
    }
    
    public ArrayList<String> addFieldsFromParams(JavaMethod m, List<JavaClass> classes) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> fieldsFromParams = new ArrayList<>();

        m.getParameters().forEach((p) -> {
            JavaClass pClass = classes.stream().filter((c) -> c.getName().equals(p.getJavaClass().getName())).findFirst().orElse(null);

            if (pClass != null) {
                pClass.getFields().forEach((ffp) -> {
                    fieldsFromParams.add("\t\t * " + p.getName() + "." + ffp.getName() + " --" + ffp.getType().getGenericValue());
                });
            }
        });

        if (!fieldsFromParams.isEmpty()) {
            template.add("\t\t * FIELDS FROM PARAMS:");
            template.addAll(fieldsFromParams);
        }

        return template;
    }

    public ArrayList<String> addMethodsFromParams(JavaMethod m, List<JavaClass> classes) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> methodsFromParams = new ArrayList<>();

        m.getParameters().forEach((p) -> {
            JavaClass pClass = classes.stream().filter((c) -> c.getName().equals(p.getJavaClass().getName())).findFirst().orElse(null);

            if (pClass != null) {
                pClass.getMethods().stream().filter((pMethods) -> !pMethods.isAbstract()).forEach((mfp) -> {
                    methodsFromParams.add("\t\t * " + p.getName() + "." + mfp.getName() + "(" + this.getParamList(mfp) + ") --" + mfp.getReturns().getGenericValue());
                });
            }
        });

        if (!methodsFromParams.isEmpty()) {
            template.add("\t\t * METHODS FROM PARAMS:");
            template.addAll(methodsFromParams);
        }

        return template;
    }
}
