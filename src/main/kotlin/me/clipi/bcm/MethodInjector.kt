package me.clipi.bcm

import me.clipi.bcm.bytebuddy.ByteBuddyInstaller
import me.clipi.bcm.bytebuddy.InjectAtHead
import me.clipi.bcm.bytebuddy.getMatcherOfMethod
import me.clipi.bcm.operations.DelegateIfInstance
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.matcher.ElementMatcher

object MethodInjector {
    fun <C> delegate(
            clazz: Class<C>,
            delegateTo: Class<*>,
            methodName: String,
            vararg params: Class<*>
    ) {
        delegateIfInstanceOf(clazz, clazz, delegateTo, methodName, *params)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun <C> delegateIfInstanceOf(
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
