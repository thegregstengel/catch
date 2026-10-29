#!/system/bin/sh
ui_print "- Catch priv-app"
set_perm_recursive "$MODPATH/system/priv-app" 0 0 0755 0644
set_perm "$MODPATH/system/etc/permissions/privapp-permissions-catch.xml" 0 0 0644
ui_print "- Boot animation"
set_perm "$MODPATH/system/media/bootanimation.zip" 0 0 0644
ui_print "- Overlays"
set_perm_recursive "$MODPATH/system/product/overlay" 0 0 0755 0644
