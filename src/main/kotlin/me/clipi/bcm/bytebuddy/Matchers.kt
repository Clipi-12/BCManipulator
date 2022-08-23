/*
 * BC-Manipulator, a simple library with stack operations that can be used with ByteBuddy
 * Copyright (C) 2022  Clipi (GitHub: Clipi-12)
 *
 * This file is part of BC-Manipulator.
 * BC-Manipulator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BC-Manipulator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with BC-Manipulator.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.clipi.bcm.bytebuddy

import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers

public fun getMatcherOfMethodWithSelfParam(
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

public fun getMatcherOfMethod(
    methodName: String,
    vararg params: Class<*>
): ElementMatcher.Junction<MethodDescription> {
    var matcher = ElementMatchers.named<NamedElement>(methodName).and(ElementMatchers.takesArguments(params.size))
    for (i in params.indices) {
        matcher = matcher.and(ElementMatchers.takesArgument(i, ElementMatchers.isSuperTypeOf(params[i])))
    }
    return matcher
}

public fun getMethodFromClassAndFilter(
    clazz: TypeDescription,
    filter: ElementMatcher<MethodDescription>
): MethodDescription.InDefinedShape {
    return clazz.declaredMethods.filter(filter).only
}
