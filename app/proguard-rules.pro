# R8 rules for the release build.
#
# Room, Compose, AndroidX and Kotlin all ship consumer rules, so this file only covers what is
# specific to this app: readable crash traces, and the Room database implementation that is
# looked up by name at runtime.

# Keep line numbers in crash reports, but drop the source file name so paths stay private.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# AppDatabase is opened through Room.databaseBuilder, which resolves the generated
# <name>_Impl class reflectively. The entity/DAO names are referenced directly by the
# generated implementation, so only the implementation itself needs an explicit keep.
-keep class com.example.cet6vocabulary.data.local.AppDatabase_Impl { *; }

# Compose keeps its runtime metadata through its own consumer rules; this only makes sure the
# Kotlin metadata stays available for the reflection used by the Compose compiler runtime.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
