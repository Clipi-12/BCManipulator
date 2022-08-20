package me.clipi.bcm.bytebuddy

import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers

fun getMatcherOfMethodWithSelfParam(
        methodName: String,
        clazz: Class<*>,
        vararg params: Class<*>
): ElementMatcher.Junction<MethodDescription> {
    var matcher = ElementMatchers.named<NamedElement>(methodName)
            .and(ElementMatchers.takesArguments(params.size + 1))
    matcher = matcher.and(ElementMatchers.takesArgument(0, ElementMatchers.isSuperTypeOf(clazz)))
    for (i in params.indices) {
        matcher = matcher.and(ElementMatchers.takesArgument(i + 1, ElementMatchers.isSuperTypeOf(params[i])))
    }
    return matcher
}

fun getMatcherOfMethod(
        methodName: String,
        vararg params: Class<*>
): ElementMatcher.Junction<MethodDescription> {
    var matcher = ElementMatchers.named<NamedElement>(methodName).and(ElementMatchers.takesArguments(params.size))
    for (i in params.indices) {
        matcher = matcher.and(ElementMatchers.takesArgument(i, ElementMatchers.isSuperTypeOf(params[i])))
    }
    return matcher
}

fun getMethodFromClassAndFilter(
        clazz: TypeDescription,
        filter: ElementMatcher<MethodDescription>
): MethodDescription.InDefinedShape {
    return clazz.declaredMethods.filter(filter).only
}
