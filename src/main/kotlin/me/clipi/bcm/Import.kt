package me.clipi.bcm

/**
 * It cannot automatically detect classes inside methods (local classes).
 * It cannot automatically detect classes inside kotlin files [KT-16479](https://youtrack.jetbrains.com/issue/KT-16479)
 *
 * > ***Note***: Lambdas are local classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Import(vararg val value: String)
