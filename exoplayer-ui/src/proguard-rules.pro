# Add any ProGuard configurations specific to this
# extension here.

-keep public class com.dreamers.exoplayerui.ExoplayerUi {
    public *;
 }
-keeppackagenames gnu.kawa**, gnu.expr**

-optimizationpasses 4
-allowaccessmodification
-mergeinterfacesaggressively

-repackageclasses 'com/dreamers/exoplayerui/repack'
-flattenpackagehierarchy
-dontpreverify

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn kotlin.annotations.jvm.**
-dontwarn javax.annotation.**

# Don't warn about core library
-dontwarn com.google.android.exoplayer2.**

#-keep class com.google.android.exoplayer2** {*;}

# Proguard rules specific to the common module.
###############################################

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn kotlin.annotations.jvm.**
-dontwarn javax.annotation.**

# From https://github.com/google/guava/wiki/UsingProGuardWithGuava
-dontwarn java.lang.ClassValue
-dontwarn java.lang.SafeVarargs
-dontwarn javax.lang.model.element.Modifier
-dontwarn sun.misc.Unsafe

# Don't warn about Guava's compile-only dependencies.
# These lines are needed for ProGuard but not R8.
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Workaround for https://issuetracker.google.com/issues/112297269
# This is needed for ProGuard but not R8.
-keepclassmembernames class com.google.common.base.Function { *; }

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn kotlin.annotations.jvm.**
-dontwarn javax.annotation.**


# Proguard rules specific to the UI module.
#############################################

# Constructor method accessed via reflection in TrackSelectionDialogBuilder
-dontnote androidx.appcompat.app.AlertDialog.Builder
-keepclassmembers class androidx.appcompat.app.AlertDialog$Builder {
  <init>(android.content.Context, int);
  public android.content.Context getContext();
#  public androidx.appcompat.app.AlertDialog$Builder setTitle(java.lang.CharSequence);
  public androidx.appcompat.app.AlertDialog$Builder setView(android.view.View);
  public androidx.appcompat.app.AlertDialog$Builder setPositiveButton(int, android.content.DialogInterface$OnClickListener);
  public androidx.appcompat.app.AlertDialog$Builder setNegativeButton(int, android.content.DialogInterface$OnClickListener);
  public androidx.appcompat.app.AlertDialog create();
}