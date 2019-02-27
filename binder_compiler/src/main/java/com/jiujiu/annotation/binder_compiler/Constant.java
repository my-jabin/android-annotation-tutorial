package com.jiujiu.annotation.binder_compiler;

public class Constant {


    public static final String BINDING_SUFFIX = "$Binding";

    public static class Package{
        public static final String ANDROID_VIEW = "android.view";
    }

    public static class Class{
        public static final String ANDROID_VIEW = "View";
        public static final String ANDROID_VIEW_ON_CLICK_LISTENER = "OnClickListener";
    }

    public static class Parameter {
        public static final String STR_ACTIVITY = "activity";
        public static final String STR_VIEW = "view";
    }

    public static class Method{
        public static final String STR_BINDVIEW = "bindView";
        public static final String STR_BINDONCLICK = "bindOnClick";
    }

}
