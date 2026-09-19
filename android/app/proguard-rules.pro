# Proguard rules for Parkeo
-keep class pe.parkeo.data.remote.dto.** { *; }
-keep class pe.parkeo.domain.model.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
