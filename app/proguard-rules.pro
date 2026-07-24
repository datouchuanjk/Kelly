#///////////////////////////////////////////////////////////////////云信///////////////////////////////////////////////////////////////////
-dontwarn com.netease.nim.**
-keep class com.netease.nim.** {*;}

-dontwarn com.netease.nimlib.**
-keep class com.netease.nimlib.** {*;}

-dontwarn com.netease.share.**
-keep class com.netease.share.** {*;}

-dontwarn com.netease.mobsec.**
-keep class com.netease.mobsec.** {*;}

-dontwarn com.netease.lava.**
-keep class com.netease.lava.** {*;}
#全文检索插件需要添加
-dontwarn org.apache.lucene.**
-keep class org.apache.lucene.** {*;}

#数据库功能需要添加
-keep class net.sqlcipher.** {*;}
# 补充云信 yunxin.lite 相关类（包含 ContextUtils）
-dontwarn com.netease.yunxin.lite.**
-keep class com.netease.yunxin.lite.** {
    *;  # 保留所有类、方法（包括静态方法 setContext）、字段
}

# 确保之前的 RTC 规则已覆盖 lava 包
-dontwarn com.netease.lava.**
-keep class com.netease.lava.** {*;}

# 云信主 SDK 规则（已存在，无需修改）
-dontwarn com.netease.nim.**
-keep class com.netease.nim.** {*;}
-dontwarn com.netease.nimlib.**
-keep class com.netease.nimlib.** {*;}
# 1. 保留 Handler 基类本身（包含泛型逻辑和反射相关代码）
-keep class com.helper.im.handler.Handler {
    *;  # 保留所有成员（属性、方法、构造方法）
}

# 2. 保留 Handler 的所有外部子类（如 IMSystemMessageHandler 这类继承自 Handler 的类）
-keep class * extends com.helper.im.handler.Handler {
    *;  # 保留子类的类名、所有方法（包括回调方法如 onReceiveMessages）和构造方法
}

# 3. 保留 Handler 的内部类（若有嵌套类，如 Handler.InnerClass）
-keep class com.helper.im.handler.Handler$** {
    *;  # 保留所有内部类的成员
}

# 4. 关键：保留泛型签名信息（否则无法通过 ParameterizedType 获取 T 的实际类型）
-keepattributes Signature


#///////////////////////////////////////////////////////////////////retrofit///////////////////////////////////////////////////////////////////
# 保留 Retrofit 相关的注解（必须保留，否则无法解析接口注解）
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# 保留所有使用 Retrofit 注解的接口（你的 API 接口类）
# 例如你的接口在 com.example.api 包下，需保留该包下的接口
-keep interface com.module.**.api.service { *; }

# 保留接口中的方法参数注解（如 @Path、@Query 等）
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
# 保留 Retrofit 动态代理相关的反射类（避免 Proxy 生成失败）
-keep class java.lang.reflect.Proxy { *; }
-keep class retrofit2.HttpServiceMethod { *; }

# 保留接口的所有方法参数类型（包括 suspend 方法的 Continuation 参数）


# 如果使用了 Kotlin 协程（返回 suspend 函数）
-keepclassmembers class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }

# 如果使用了 Gson 解析（Retrofit 配合 Gson 转换器）
# 需额外保留数据模型类（实体类）不被混淆，例如：
-keep class com.module.**.api.data.** { *; }
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keepattributes EnclosingMethod  # Gson 解析内部类需要
# 1. 保留泛型签名信息，让 Gson 识别 BaseResponse<LoginResponse> 这类泛型类型
-keepattributes Signature

# 2. 显式保留无参构造方法，防止 R8 优化移除（Gson 必须用它创建实例）
-keepclassmembers class com.module.**.api.data.** {
    <init>();  # 显式保留无参构造
}
# 保留所有 Retrofit 相关类（包括适配器、动态代理依赖）
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# 保留所有协程相关类
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }

# 保留所有 Gson 相关类
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
#///////////////////////////////////////////////////////////////////koin///////////////////////////////////////////////////////////////////
# 保留 Koin 框架本身的类和接口（避免框架核心逻辑被混淆）
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }

# 保留 Koin 的注解（如 @KoinInternalApi 等，避免注解解析失败）
-keepattributes *Annotation*
-keep @org.koin.core.annotation.** class *

# 保留所有通过 Koin 注入的依赖类（关键！你的业务类）
# 例如：所有在 module 中通过 single/factory 注册的类
# 假设你的依赖类在 com.module 包下，递归保留所有类
-keep class com.module.** { *; }

# 保留 Koin 模块类（如果你自定义了 Module 类，如 AppModule）
-keep class * extends org.koin.core.module.Module { *; }

# 保留构造方法（Koin 需要通过构造方法实例化依赖）
-keepclassmembers class * {
    <init>(...);
}
#///////////////////////////////////////////////////////////////////viewModel///////////////////////////////////////////////////////////////////
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);  # 保留任意参数的构造方法
}

# app/proguard-rules.pro
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }