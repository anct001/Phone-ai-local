-keep class com.phoneai.local.llm.LlamaEngine { *; }
-keepclassmembers class com.phoneai.local.llm.LlamaEngine {
    private void onToken(java.lang.String);
}
-keep class com.phoneai.local.model.** { *; }
