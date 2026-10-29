# Release builds are not minified (isMinifyEnabled = false). If that changes, keep
# the uid-1000 display host entry point, which app_process loads by name:
# -keep class app.catchrower.sysdisplay.SystemDisplayHost { public static void main(java.lang.String[]); }
