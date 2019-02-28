package com.jiujiu.annotation.binder_compiler;

import com.google.auto.service.AutoService;
import com.jiujiu.annotation.binder_annotation.BindView;
import com.jiujiu.annotation.binder_annotation.Keep;
import com.jiujiu.annotation.binder_annotation.OnClick;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(javax.annotation.processing.Processor.class)
public class Processor extends AbstractProcessor {

    private Messager messager;
    private Filer filer;
    private Elements elementsUtils;

    private Set<TypeElement> typeElementsOfAnnotation;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementsUtils = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
        typeElementsOfAnnotation = new HashSet<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        // set are annotation, which are also TypeElement
        for (TypeElement typeElement : set) {
            Set<? extends Element> annotatedElement = roundEnvironment.getElementsAnnotatedWith(typeElement);
            findTypeElementOfAnnotation(annotatedElement);
        }

        // for each class which contains the annotations, create a corresponding Binding class
        for (TypeElement typeElement : this.typeElementsOfAnnotation) {
            String packageName = elementsUtils.getPackageOf(typeElement).getQualifiedName().toString();
            String typeName = typeElement.getSimpleName().toString();
            // class name of the TypeElement
            ClassName activityName = ClassName.get(packageName, typeName);
            // the generated class name with suffix
            ClassName bindingActivityName = ClassName.get(packageName, typeName + Constant.BINDING_SUFFIX);

            // create a class builder for generating binding class
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(bindingActivityName)
                    .addModifiers(Modifier.PUBLIC)
                    // add constructor method
                    .addMethod(createConstructorBuilder(activityName).build())
                    // add bindView method
                    .addMethod(createBindViewBuilder(activityName, typeElement).build())
                    // add bindOnClick method
                    .addMethod(createOnClickBuilder(activityName, typeElement).build());

            JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build())
                    .addFileComment("This file is auto-generated and should not be edited.")
                    .build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            messager.printMessage(Diagnostic.Kind.OTHER, "this is my note");
        }
        return false;
    }

    private MethodSpec.Builder createConstructorBuilder(ClassName activityName) {
        MethodSpec.Builder constructorSpec = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(activityName, Constant.Parameter.STR_ACTIVITY)
                .addStatement("$N($N)", Constant.Method.STR_BINDVIEW, Constant.Parameter.STR_ACTIVITY)
                .addStatement("$N($N)", Constant.Method.STR_BINDONCLICK, Constant.Parameter.STR_ACTIVITY);
        return constructorSpec;
    }

    private MethodSpec.Builder createBindViewBuilder(ClassName activityName, TypeElement typeElement) {
        MethodSpec.Builder bindViewSpec = MethodSpec.methodBuilder(Constant.Method.STR_BINDVIEW)
                .addModifiers(Modifier.PRIVATE)
                .returns(TypeName.VOID)
                .addParameter(activityName, Constant.Parameter.STR_ACTIVITY);

        for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
            BindView bindView = variableElement.getAnnotation(BindView.class);
            if (bindView != null) {
                //activity.tvContent = (TextView)activity.findViewById(2131165322);
                bindViewSpec.addStatement("$N.$N = ($T)$N.findViewById($L)",
                        Constant.Parameter.STR_ACTIVITY,
                        variableElement.getSimpleName(),
                        variableElement,
                        Constant.Parameter.STR_ACTIVITY,
                        bindView.value());
            }
        }

        return bindViewSpec;
    }

    private MethodSpec.Builder createOnClickBuilder(ClassName activityName, TypeElement typeElement) {
        MethodSpec.Builder bindViewSpec = MethodSpec.methodBuilder(Constant.Method.STR_BINDONCLICK)
                .addModifiers(Modifier.PRIVATE)
                .returns(TypeName.VOID)
                .addParameter(activityName, Constant.Parameter.STR_ACTIVITY, Modifier.FINAL);

        ClassName androidOnClickListenerClassName = ClassName.get(
                Constant.Package.ANDROID_VIEW,
                Constant.Class.ANDROID_VIEW,
                Constant.Class.ANDROID_VIEW_ON_CLICK_LISTENER);

        ClassName androidViewClassName = ClassName.get(Constant.Package.ANDROID_VIEW, Constant.Class.ANDROID_VIEW);

        for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            OnClick onClick = executableElement.getAnnotation(OnClick.class);
            if (onClick != null) {
                //activity.findViewById(2131165218).setOnClickListener(new View.OnClickListener() {
                //            @Override
                //            public void onClick(View view) {
                //               activity.bt1Click(view);
                //            }
                //        });

                TypeSpec onClickListener = TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(androidOnClickListenerClassName)
                        .addMethod(MethodSpec.methodBuilder("onClick")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(TypeName.VOID)
                                .addParameter(androidViewClassName, Constant.Parameter.STR_VIEW)
                                .addStatement("$N.$N($N)", Constant.Parameter.STR_ACTIVITY, executableElement.getSimpleName(), Constant.Parameter.STR_VIEW)
                                .build())
                        .build();


                bindViewSpec.addStatement("$N.findViewById($L).setOnClickListener($L)",
                        Constant.Parameter.STR_ACTIVITY,
                        onClick.value(), onClickListener);
            }
        }

        return bindViewSpec;
    }

    /**
     * Find out these annotations are inside which class.
     *
     * @param annotatedElement Annotations
     */
    private void findTypeElementOfAnnotation(Set<? extends Element> annotatedElement) {
        for (Element e : annotatedElement) {
            Element enclosingElement = e.getEnclosingElement();
            if (enclosingElement.getKind() == ElementKind.CLASS) {
                typeElementsOfAnnotation.add((TypeElement) enclosingElement);
            }
        }
    }


    private void note(Element e, String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.OTHER, String.format(msg, args), e);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(BindView.class.getCanonicalName());
        types.add(OnClick.class.getCanonicalName());
        types.add(Keep.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
