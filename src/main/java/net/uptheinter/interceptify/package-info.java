/**
 * <p>Interceptify provides a convenient means of performing runtime class interception.</p>
 *
 * <p>The first thing you will want to do is create a class that implements
 * {@link net.uptheinter.interceptify.interfaces.StartupConfig StartupConfig}</p>
 *
 * <p>You will then need to define your own classes to override whatever it is that
 * you want to hook into. This is achieved through the usage of annotations.
 * See:
 * <p>{@link net.uptheinter.interceptify.annotations.InterceptClass @InterceptClass}</p>
 * <p>{@link net.uptheinter.interceptify.annotations.OverwriteConstructor @OverwriteConsutructor}</p>
 * <p>{@link net.uptheinter.interceptify.annotations.OverwriteMethod @OverwriteMethod}</p>
 *
 * <p>You can also define a list of classes which should be made public, including
 * their methods and fields. This is of course purely for convenience as you could
 * achieve the same means through runtime reflection.
 * There is not yet a formal API for dumping these converted class files - however,
 * you can fairly easily create your own implementation of {@link java.lang.instrument.Instrumentation Instrumentation}
 * and pass it to
 * {@link net.uptheinter.interceptify.EntryPoint#premain(java.lang.String, java.lang.instrument.Instrumentation) EntryPoint.premain}</p>
 */
package net.uptheinter.interceptify;