package com.arangodb.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@SupportedAnnotationTypes("com.arangodb.codegen.GenerateSyncApi")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class GenerateSyncApiProcessor extends AbstractProcessor {
    private TypeElement syncParentClient;
    private TypeElement syncParentClientImpl;

    private static String getPackageName(Element e) {
        while (e.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            e = e.getEnclosingElement();
        }
        return ((PackageElement) e.getEnclosingElement()).getQualifiedName().toString();
    }

    private Element findExactOneElementAnnotatedWith(Class<? extends Annotation> annotation, RoundEnvironment roundEnv) {
        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
        if (elements.isEmpty()) {
            throw new RuntimeException("No class annotated with @" + annotation.getSimpleName() + " found!");
        } else if (elements.size() > 1) {
            throw new RuntimeException("More than one class annotated with @" + annotation.getSimpleName() + " found!");
        } else {
            return elements.iterator().next();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (syncParentClient == null) {
            syncParentClient = (TypeElement) findExactOneElementAnnotatedWith(SyncClientParent.class, roundEnv);
        }

        if (syncParentClientImpl == null) {
            syncParentClientImpl = (TypeElement) findExactOneElementAnnotatedWith(SyncClientParentImpl.class, roundEnv);
        }

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "found @GenerateSyncApi at " + element);
                if (!element.getKind().isInterface()) {
                    throw new IllegalArgumentException("@GenerateSyncApi can only be applied to interfaces!");
                }

                List<MethodSpec> implMethods = extractMethodSpecImpl(element);
                TypeSpec interfaceSpec = createInterface(element, implMethods);
                JavaFile interfaceJavaFile = JavaFile.builder(getPackageName(element), interfaceSpec)
                        .skipJavaLangImports(true)
                        .build();

                TypeSpec implSpec = createImplementation(element, interfaceSpec, implMethods);
                JavaFile implJavaFile = JavaFile.builder(getPackageName(element) + ".impl", implSpec)
                        .skipJavaLangImports(true)
                        .build();

                try {
                    interfaceJavaFile.writeTo(processingEnv.getFiler());
                    implJavaFile.writeTo(processingEnv.getFiler());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }

        return true;
    }

    private MethodSpec mapMethod(MethodSymbol symbol) {
        String methodName = symbol.getSimpleName().toString();
        List<ParameterSpec> parameters = symbol.getParameters().stream()
                .map(varSymbol -> ParameterSpec
                        .builder(
                                TypeName.get(varSymbol.asType()),
                                varSymbol.getSimpleName().toString()
                        )
                        .addModifiers(varSymbol.getModifiers())
                        .addModifiers(Modifier.FINAL)
                        .build()
                )
                .collect(Collectors.toList());
        MethodSpec.Builder specBuilder = MethodSpec
                .methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameters(parameters);
        Type returnType = symbol.getReturnType();
        String enclosingTypeString = returnType.asElement().getQualifiedName().toString();
        CodeBlock delegationArguments = CodeBlock.join(parameters.stream()
                        .map(i -> CodeBlock.of(i.name))
                        .collect(Collectors.toList()),
                ", ");

        if (Mono.class.getCanonicalName().equals(enclosingTypeString)) {
            Type argumentType = returnType.getTypeArguments().get(0);
            String argumentTypeString = argumentType.asElement().getQualifiedName().toString();
            if (Void.class.getCanonicalName().equals(argumentTypeString)) {
                specBuilder
                        .returns(TypeName.VOID)
                        .addStatement("reactive().$L($L).block()", methodName, delegationArguments);
            } else {
                specBuilder
                        .returns(TypeName.get(argumentType))
                        .addStatement("return reactive().$L($L).block()", methodName, delegationArguments);
            }
        } else if (Flux.class.getCanonicalName().equals(enclosingTypeString)) {
            Type argumentType = returnType.getTypeArguments().get(0);
            specBuilder
                    .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(argumentType)))
                    .addStatement("return reactive().$L($L).collectList().block()", methodName, delegationArguments);
        } else if (symbol.getAnnotation(SyncApiDelegator.class) != null) {

            // sync interface delegator canonical name
            String syncClassName = returnType.toString() + "Sync";

            // sync implementation delegator canonical name
            String[] returnTypeParts = syncClassName.split("\\.");
            StringBuilder syncImplClassNameBuilder = new StringBuilder();
            for (int i = 0; i < returnTypeParts.length - 1; i++) {
                syncImplClassNameBuilder.append(returnTypeParts[i]);
                syncImplClassNameBuilder.append(".");
            }
            syncImplClassNameBuilder.append("impl.");
            syncImplClassNameBuilder.append(returnTypeParts[returnTypeParts.length - 1]);
            syncImplClassNameBuilder.append("Impl");
            String syncImplClassName = syncImplClassNameBuilder.toString();

            specBuilder
                    .returns(ClassName.bestGuess(syncClassName))
                    .addStatement("return new $T(reactive().$L($L))", ClassName.bestGuess(syncImplClassName), methodName, delegationArguments);
        } else {
            specBuilder
                    .returns(TypeName.get(returnType))
                    .addStatement("return reactive().$L($L)", methodName, delegationArguments);
        }
        return specBuilder.build();
    }

    private List<MethodSpec> extractMethodSpecImpl(Element e) {
        return e.getEnclosedElements().stream()
                .filter(i -> i.getKind().equals(ElementKind.METHOD))
                .map(i -> (MethodSymbol) i)
                .filter(Predicate.not(MethodSymbol::isStatic))
                .filter(i -> i.getAnnotation(SyncApiIgnore.class) == null)
                .map(this::mapMethod)
                .collect(Collectors.toList());
    }

    private TypeSpec createInterface(Element e, List<MethodSpec> methodImpls) {
        List<MethodSpec> methods = methodImpls.stream()
                .map(m -> {
                            List<ParameterSpec> parameters = m.parameters.stream()
                                    .map(p -> ParameterSpec.builder(p.type, p.name).build())
                                    .collect(Collectors.toList());
                            return MethodSpec
                                    .methodBuilder(m.name)
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .returns(m.returnType)
                                    .addParameters(parameters)
                                    .addJavadoc(
                                            "Synchronous version of {@link $L#$L($L)}",
                                            e.getSimpleName().toString(),
                                            m.name,
                                            m.parameters
                                                    .stream()
                                                    .map(p -> p.type.toString())
                                                    .collect(Collectors.joining(", "))
                                    )
                                    .build();
                        }
                )
                .collect(Collectors.toList());

        return TypeSpec
                .interfaceBuilder(e.getSimpleName() + "Sync")
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(syncParentClient),
                        ClassName.get(getPackageName(e), e.getSimpleName().toString())))
                .addModifiers(Modifier.PUBLIC)
                .addMethods(methods)
                .addJavadoc("Synchronous version of {@link $T}", e)
                .build();
    }


    private TypeSpec createImplementation(Element e, TypeSpec interfaceSpec, List<MethodSpec> implMethods) {
        String packageName = getPackageName(e);
        String className = e.getSimpleName().toString();
        ClassName superInterface = ClassName.get(packageName, interfaceSpec.name);

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(packageName, className), "delegate", Modifier.FINAL)
                .addStatement("super(delegate)")
                .build();

        return TypeSpec
                .classBuilder(className + "SyncImpl")
                .addSuperinterface(superInterface)
                .superclass(
                        ParameterizedTypeName.get(
                                ClassName.get(syncParentClientImpl),
                                ClassName.get(packageName, className)
                        )
                )
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(constructor)
                .addMethods(implMethods)
                .addJavadoc("Implementation of {@link $T}", superInterface)
                .build();
    }

}
