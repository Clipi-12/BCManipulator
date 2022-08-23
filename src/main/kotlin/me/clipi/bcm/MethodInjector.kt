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

package me.clipi.bcm

import me.clipi.bcm.bytebuddy.ByteBuddyInstaller
import me.clipi.bcm.bytebuddy.InjectAtHead
import me.clipi.bcm.bytebuddy.getMatcherOfMethod
import me.clipi.bcm.operations.DelegateIfInstance
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.matcher.ElementMatcher

public object MethodInjector {
    public fun <C> delegate(
        clazz: Class<C>,
        delegateTo: Class<*>,
        methodName: String,
        vararg params: Class<*>
    ): Unit = delegateIfInstanceOf(clazz, clazz, delegateTo, methodName, *params)

    public fun <C> delegateIfInstanceOf(
        clazz: Class<C>,
        instanceOf: Class<out C>,
        methodName: String,
        vararg params: Class<*>
    ): Unit = delegateIfInstanceOf(clazz, instanceOf, instanceOf, methodName, *params)

    public fun <C> delegateIfInstanceOf(
        clazz: Class<C>,
        instanceOf: Class<out C>,
        delegateTo: Class<*>,
        methodName: String,
        vararg params: Class<*>
    ) {
        val matcher: ElementMatcher<MethodDescription> = getMatcherOfMethod(methodName, *params)
        ByteBuddyInstaller.install()
        ByteBuddy()
            .redefine(clazz)
            .visit(
                InjectAtHead(
                    matcher,
                    DelegateIfInstance.create(
                        clazz,
                        instanceOf,
                        delegateTo,
                        methodName,
                        *params
                    )
                )
            )
            .make()
            .load(clazz.classLoader, ClassReloadingStrategy.fromInstalledAgent())
    }

}
