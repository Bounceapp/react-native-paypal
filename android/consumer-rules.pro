# Braintree depends on WorkManager, which resolves to Room 2.6.1. Room 2.6's own
# rule, `-keep class * extends androidx.room.RoomDatabase`, names no members, and
# R8's full mode (the AGP default) then keeps the class but not its no-arg
# constructor. WorkManager creates its database through that constructor by
# reflection, so a minified release build crashed at startup with
# `NoSuchMethodException: androidx.work.impl.WorkDatabase_Impl.<init> []`,
# before any JavaScript ran. Later Room versions keep the constructor themselves.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
