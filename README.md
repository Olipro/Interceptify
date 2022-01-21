[![Snapshot Pipeline](https://github.com/Olipro/Interceptify/actions/workflows/snapshot.yml/badge.svg?branch=master)](https://github.com/Olipro/Interceptify/actions/workflows/snapshot.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Olipro_Interceptify&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Olipro_Interceptify)
[![codecov](https://codecov.io/gh/Olipro/Interceptify/branch/master/graph/badge.svg?token=9FBTJVGC2T)](https://codecov.io/gh/Olipro/Interceptify)
# Interceptify

This library is designed to provide a convenient and simple means of performing runtime interception of Java methods/constructors.

## Usage

There is no API in the traditional sense, just a few annotations and a single interface to implement which are used to determine what you want to do - they are as follows:

### `@InterceptClass("[target]")`
This annotation requires you to provide the fully-qualified name of the class you want to intercept. It should be applied to your class that has the methods that will be doing the interception.

### `@OverwriteMethod("[target]")`
This annotation applies to your intercepting method and is used to determine which method of the target class it intends to intercept. You method can provide a parameter that receives the original method which can be called (or not) as you desire. If the intercepted method is not static, you can also receive a parameter for the instance.

### `@OverwriteConstructor(before = false, after = true)`

This annotation also applies to your method and indicates that you want to overwrite a constructor, the boolean values above are the defaults and determine whether your method should execute before the constructor, after it, or both.

Due to the vagaries of how Java is implemented, you cannot prevent the constructor from executing (although in a future release I might support completely rewriting it) - thus you currently have the options above. If you set both values to `false`, it will simply fail.

So, for example, say you wanted to intercept the following class:

```
package foo.bar.baz;
private class InterceptMe {
    public InterceptMe(int a, String b) { ... }
    private Foo doFoo(Bar t, char x) { ... }
    private static Bar doBar(List<String> a, Stream<int>) { ... }
    
    public static void main(String[] args) { ... }
}
```

You would write a class that looks like so:

```
@InterceptClass("foo.bar.baz.InterceptMe")
public class Interceptor {
    @OverwriteConstructor
    public static void getConstructor(InterceptMe instance, int a, String b) { ... }
    
    @OverwriteMethod("doFoo")
    public static Foo interceptFoo(InterceptMe instance, Method original, Bar t, char x) { ... }
    
    @OverwriteMethod("doBar")
    public static Bar interceptBar(Method original, List<String> a, Stream<int> b) { ... }
}
```

There are a few things to note here:

1. Everything related to the interception should be `public`.
2. The name of your method doesn't matter.
3. The order of arguments matters.

## Runtime Execution

Since the classes you want to intercept need to **not** be loaded into the JVM already, you should define your own `main()` and have the JVM execute that first instead. You will need to create a class that `implements StartupConfig`.

Also of important note is that all classes performing interception need to live inside a jar file - since a jar is really just a zip file (and consequently, trivially created) - this decision was made to avoid inevitable confusion caused by working with directory paths and poorly chosen directory names.

So, continuing the example, let's say the original executable was run via `java -jar Foo.jar` or perhaps `java -cp Foo.jar foo.bar.baz.InterceptMe` - you would want to change it to `java -cp MyInterceptor.jar;Foo.jar MyMainClass`.

Note that it is *strongly* recommended that you provide everything needed on the classpath via the commandline; you don't have to under certain circumstances, but not doing so is requisite upon knowing how ClassLoaders work.

## See Also

[The Javadoc!](https://olipro.github.io/Interceptify)
