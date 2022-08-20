package me.clipi.bcm.operations

import me.clipi.bcm.Import
import me.clipi.bcm.bytebuddy.ByteBuddyInstaller
import me.clipi.bcm.bytebuddy.getMatcherOfMethod
import me.clipi.bcm.bytebuddy.getMatcherOfMethodWithSelfParam
import me.clipi.bcm.bytebuddy.getMethodFromClassAndFilter
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.*
import net.bytebuddy.implementation.bytecode.assign.InstanceCheck
import net.bytebuddy.implementation.bytecode.assign.TypeCasting
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant
import net.bytebuddy.implementation.bytecode.constant.NullConstant
import net.bytebuddy.implementation.bytecode.constant.TextConstant
import net.bytebuddy.implementation.bytecode.member.FieldAccess
import net.bytebuddy.implementation.bytecode.member.MethodInvocation
import net.bytebuddy.implementation.bytecode.member.MethodReturn
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess
import net.bytebuddy.jar.asm.*
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers
import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.reflect.KFunction1
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod

class DelegateIfInstance(
        clazz: Class<*>,
        instanceOf: Class<*>,
        delegateTo: Class<*>,
        matcherClazz: ElementMatcher<MethodDescription>,
        matcherDelegateTo: ElementMatcher.Junction<MethodDescription>,
        vararg params: Class<*>
) : BytecodeOperation() {
    companion object Factory {
        fun <C> create(
                clazz: Class<C>,
                instanceOf: Class<out C>,
                delegateTo: Class<*>,
                methodName: String,
                vararg params: Class<*>
        ) = create(
                clazz,
                instanceOf,
                delegateTo,
                getMatcherOfMethod(methodName, *params),
                getMatcherOfMethodWithSelfParam(methodName, instanceOf, *params),
                *params
        )

        @Suppress("MemberVisibilityCanBePrivate")
        fun <C> create(
                clazz: Class<C>,
                instanceOf: Class<out C>,
                delegateTo: Class<*>,
                matcherClazz: ElementMatcher<MethodDescription>,
                matcherDelegateTo: ElementMatcher.Junction<MethodDescription>,
                vararg params: Class<*>
        ) = DelegateIfInstance(
                clazz,
                instanceOf,
                delegateTo,
                matcherClazz,
                matcherDelegateTo,
                *params
        )
    }

    private val returnType: TypeDescription
    private val returnStat: StackManipulation
    private val methodMatcher: ElementMatcher<MethodDescription> = matcherDelegateTo.and(ElementMatchers.isStatic())
    private val clazz: TypeDescription
    private val delegateTo: Class<*>
    private val instanceOf: TypeDescription
    private val thisInstanceOfChecker: ThisInstanceOf
    private val delegate: Invoke
    private val ifFalseGoToLabel: IfFalseGoToLabel
    private val addLabel: AddLabel

    init {
        this.clazz = TypeDescription.ForLoadedType.of(clazz)
        this.instanceOf = if (clazz == instanceOf) this.clazz else TypeDescription.ForLoadedType.of(instanceOf)
        this.delegateTo = delegateTo
        returnType = getMethodFromClassAndFilter(this.clazz, matcherClazz).returnType.asErasure()
        returnStat = MethodReturn.of(returnType)
        if (!returnType.isInHierarchyWith(
                        getMethodFromClassAndFilter(
                                TypeDescription.ForLoadedType.of(delegateTo),
                                methodMatcher
                        ).returnType.asErasure()
                )
        ) throw AssertionError("The delegated method must return an object that can be casted safely to the original method's return type")

        val usualExec = Label()
        ifFalseGoToLabel = IfFalseGoToLabel(usualExec)
        addLabel = AddLabel(usualExec, AddLabel.Frame.SAME)
        thisInstanceOfChecker = ThisInstanceOf(instanceOf, clazz.classLoader)
        delegate = Invoke(
                delegateTo,
                methodMatcher,
                params,
                true to true,
                clazz.classLoader
        )
    }


    override fun execute(
            methodVisitor: MethodVisitor,
            implementationContext: Implementation.Context,
            instrumentedMethod: MethodDescription
    ): StackOperation.Compound {
        return StackOperation.Compound(
                if (clazz.isAssignableTo(instanceOf)) IntegerConstant.forValue(true)
                else thisInstanceOfChecker,  // push this; push instanceOf; instanceof;
                ifFalseGoToLabel,  // if i == 0 -> goto _usualExec_;
                delegate, // pushAll args; INVOKEXXX pckg/clazz/method (paramTypes)X;
                TypeCasting.to(returnType),
                returnStat,  // XReturn
                addLabel // _usualExec_:
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as DelegateIfInstance
        return methodMatcher == that.methodMatcher && clazz == that.clazz && delegateTo == that.delegateTo
    }

    override fun hashCode(): Int {
        return Objects.hash(methodMatcher, clazz, delegateTo)
    }

}

class ThisInstanceOf constructor(private val clazz: Class<*>, private val classLoader: ClassLoader) :
        StackOperation() {
    override fun execute(
            methodVisitor: MethodVisitor,
            implementationContext: Implementation.Context
    ): StackManipulation.Size {
        MakeClassAccessible(clazz, classLoader)
        return Compound(
                MethodVariableAccess.REFERENCE.loadFrom(0), // push this;
                InstanceCheck.of(TypeDescription.ForLoadedType.of(clazz)) // push clazz; instanceof;
        ).apply(methodVisitor, implementationContext)
    }
}

/**
 * @param pushThisBeforeInvoking If the first element is set to false, then the method will be
 * treated as a static method. Otherwise, the method will be treated as an instance-dependant
 * method if the second element is false. If both the first and the second element are true,
 * the method will be treated as a static method that takes $this$ as its first parameter
 */
class Invoke(
        private val clazz: Class<*>,
        method: ElementMatcher<MethodDescription>,
        private val params: Array<out Class<*>>,
        private val pushThisBeforeInvoking: Pair<Boolean, Boolean>,
        private val classLoader: ClassLoader,
) : StackOperation() {
    private val pushArgs: Array<StackManipulation>
    private val method: MethodDescription.InDefinedShape =
            getMethodFromClassAndFilter(TypeDescription.ForLoadedType.of(clazz), method)


    init {
        var stackIndex = 0
        val pushActualArgs = { i: Int ->
            val type = params[i]
            val param = MethodVariableAccess.of(TypeDescription.ForLoadedType.of(type))
            val result = param.loadFrom(stackIndex) // push param[i];
            stackIndex += when (type) {
                Long::class.javaPrimitiveType, Double::class.javaPrimitiveType -> 2
                else -> 1
            }
            result
        }
        pushArgs = when {
            pushThisBeforeInvoking.first -> Array(params.size + 1) { i ->
                when (i) {
                    0 -> MethodVariableAccess.REFERENCE.loadFrom(stackIndex++) // push this;
                    else -> pushActualArgs(i - 1)
                }
            }

            else -> Array(params.size, pushActualArgs)
        }
    }

    override fun execute(
            methodVisitor: MethodVisitor,
            implementationContext: Implementation.Context
    ): StackManipulation.Size {
        MakeClassAccessible(clazz, classLoader)
        return Compound(
                Compound(*pushArgs),  // pushAll args;
                MethodInvocation.invoke(method)  // INVOKEXXX pckg/clazz/method (paramTypes)X;
        ).apply(methodVisitor, implementationContext)
    }
}

class TryCatch(private vararg val run: StackManipulation) : StackOperation() {
    companion object {
        private val runtimeExceptionConstructor: MethodDescription.InDefinedShape
        private val uncheckedIoExceptionConstructor: MethodDescription.InDefinedShape

        init {
            runtimeExceptionConstructor = MethodDescription.ForLoadedConstructor(
                    @Suppress("RedundantLambdaOrAnonymousFunction")
                    { f: KFunction1<Throwable, RuntimeException> -> f }(::RuntimeException)
                            .javaConstructor!!
            )
            uncheckedIoExceptionConstructor = MethodDescription.ForLoadedConstructor(
                    @Suppress("RedundantLambdaOrAnonymousFunction")
                    { f: KFunction1<IOException, UncheckedIOException> -> f }(::UncheckedIOException)
                            .javaConstructor!!
            )
        }
    }

    private val start = Label()
    private val end = Label()
    private val direct = Label()
    private val wrap = Label()
    private val wrapIo = Label()
    private val _throw = Label()

    override fun execute(
            methodVisitor: MethodVisitor,
            implementationContext: Implementation.Context
    ): StackManipulation.Size {
        methodVisitor.visitTryCatchBlock(
                start,
                end,
                direct,
                Type.getInternalName(RuntimeException::class.java)
        )
        methodVisitor.visitTryCatchBlock(
                start,
                end,
                wrapIo,
                Type.getInternalName(IOException::class.java)
        )
        methodVisitor.visitTryCatchBlock(start, end, wrap, Type.getInternalName(Exception::class.java))

        return Compound.Parallel(
                Compound.Parallel.EndsInRetOrThr(
                        GOTO(start),
                        Compound.Parallel(
                                Compound(
                                        AddLabel(direct, AddLabel.Frame.NEW_STACK(RuntimeException::class.java)),
                                        GOTO(_throw),
                                ),
                                Compound(
                                        AddLabel(wrapIo, AddLabel.Frame.NEW_STACK(IOException::class.java)),
                                        TypeCreation.of(TypeDescription.ForLoadedType.of(UncheckedIOException::class.java)),
                                        Duplication.SINGLE,
                                        ROT,
                                        MethodInvocation.invoke(uncheckedIoExceptionConstructor),
                                        GOTO(_throw),
                                ),
                                Compound(
                                        AddLabel(wrap, AddLabel.Frame.NEW_STACK(Exception::class.java)),
                                        TypeCreation.of(TypeDescription.ForLoadedType.of(RuntimeException::class.java)),
                                        Duplication.SINGLE,
                                        ROT,
                                        MethodInvocation.invoke(runtimeExceptionConstructor)
                                )
                        ),
                        AddLabel(_throw, AddLabel.Frame.NEW_STACK(RuntimeException::class.java, false)),
                        Throw.INSTANCE
                ),
                Compound(
                        AddLabel(start, AddLabel.Frame.SAME),
                        *run,
                        AddLabel(end)
                )
        ).apply(methodVisitor, implementationContext)
    }
}

class IfFalseGoToLabel constructor(private val label: Label) : StackOperation() {
    override fun execute(
            methodVisitor: MethodVisitor,
            implementationContext: Implementation.Context
    ): StackManipulation.Size {
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, label) // if i == 0 -> goto _label_;
        return StackSize.SINGLE.toDecreasingSize()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as IfFalseGoToLabel
        return label == that.label
    }

    override fun hashCode(): Int {
        return label.hashCode()
    }
}

class AddLabel constructor(
        private val label: Label,
        private val frameChange: Frame = Frame.NOT_NEEDED
) : StackOperation() {
    @Suppress("ClassName")
    sealed class Frame {
        object NOT_NEEDED : Frame()
        object SAME : Frame()
        data class NEW_STACK(val newStack: Class<*>, val increaseStackSize: Boolean = true) : Frame()
        data class NEW_LOCAL(val newLocal: Class<*>, val increaseStackSize: Boolean = true) : Frame()
    }

    override fun execute(
            methodVisitor: MethodVisitor,
            implementationContext: Implementation.Context
    ): StackManipulation.Size {
        methodVisitor.visitLabel(label) // _label_:
        if (implementationContext.classFileVersion.isAtLeast(ClassFileVersion.JAVA_V6)) {
            when (frameChange) {
                Frame.NOT_NEEDED -> {}
                Frame.SAME -> methodVisitor.visitFrame(
                        Opcodes.F_SAME,
                        0,
                        arrayOf(),
                        0,
                        arrayOf()
                ) // [frame: F_SAME]

                is Frame.NEW_STACK -> {
                    methodVisitor.visitFrame(
                            Opcodes.F_SAME1,
                            0,
                            arrayOf(),
                            1,
                            arrayOf(Type.getInternalName(frameChange.newStack))
                    ) // [frame: F_SAME1]
                    if (frameChange.increaseStackSize) return StackSize.SINGLE.toIncreasingSize()
                }

                is Frame.NEW_LOCAL -> {
                    methodVisitor.visitFrame(
                            Opcodes.F_APPEND,
                            1,
                            arrayOf(Type.getInternalName(frameChange.newLocal)),
                            0,
                            arrayOf()
                    ) // [frame: F_APPEND]
                    if (frameChange.increaseStackSize) ++newLocals
                }
            }
        }
        return StackManipulation.Size.ZERO
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val addLabel = other as AddLabel
        return label == addLabel.label
    }

    override fun hashCode(): Int {
        return label.hashCode()
    }
}


class MakeClassAccessible(clazz: Class<*>, to: ClassLoader) {
    private fun hasToBeInjected(clazz: Class<*>) = try {
        classLoader.loadClass(clazz.name)
        false
    } catch (ignored: ClassNotFoundException) {
        true
    }

    private val classLoader = to

    init {
        val alsoLoad = run {
            val alsoLoad = clazz.getDeclaredAnnotation(Import::class.java)?.value?.toMutableSet() ?: mutableSetOf()
            alsoLoad.add(clazz.name)
            alsoLoad
        }.mapTo(mutableSetOf(), clazz.classLoader::loadClass).flatMapTo(mutableSetOf()) { original ->
            val result = mutableSetOf<Class<*>>()
            lateinit var addToResult: (Class<*>) -> Unit
            addToResult = add@{
                if (!result.add(it)) return@add
                if (it.superclass != null) addToResult(it.superclass)
                if (it.enclosingClass != null) addToResult(it.enclosingClass)
                if (it.declaringClass != null) addToResult(it.declaringClass)
                if (it.enclosingMethod != null) addToResult(it.enclosingMethod.declaringClass)
                if (it.enclosingConstructor != null) addToResult(it.enclosingConstructor.declaringClass)
                if (it.classes.isNotEmpty()) it.classes.forEach(addToResult)
            }
            addToResult(original)
            if (DEBUG) {
                println("Posible addition to the classloader's classpath: ")
                println("Without considering already loaded classes: ${result.joinToString()}")
                println("Considering already loaded classes: ${result.filter(this::hasToBeInjected).joinToString()}")
            }
            result
        }.filter(this::hasToBeInjected)

        if (alsoLoad.isNotEmpty()) {
            if (DEBUG) println("Adding to classloader's classpath: " + alsoLoad.joinToString())

            ByteBuddyInstaller.install()
            alsoLoad.forEach {
                ByteBuddy()
                        .redefine(it)
                        .make()
                        .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
            }

            if (DEBUG) println("Starting sanity check for the loaded classes")
            alsoLoad.forEach { c -> classLoader.loadClass(c.name) }
            if (DEBUG) println("Starting check done")
        }
    }
}

/**
 * @see [net.bytebuddy.implementation.bytecode.constant.SerializedConstant]
 */
object SerializedConstant {
    /**
     * A charset that does not change the supplied byte array upon encoding or decoding.
     */
    private val CHARSET = StandardCharsets.ISO_8859_1
    private val CHARSET_FIELD = StandardCharsets::ISO_8859_1.javaField!!

    fun of(obj: Serializable?): StackManipulation {
        if (obj == null) return NullConstant.INSTANCE
        val byteArrayOutputStream = ByteArrayOutputStream()
        ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(obj) }
        val serialization = byteArrayOutputStream.toString(CHARSET.name())

        if (DEBUG) {
            println("----------")
            println("Serializing $obj")
            println()
            println("Object as string")
            println(serialization)
            println()
            val bArr = byteArrayOutputStream.toByteArray()
            println("Object as byte array")
            println(Arrays.toString(bArr))
            println()
            println("Deserialized object with byte array")
            println(ObjectInputStream(ByteArrayInputStream(bArr)).use { it.readObject() })
            println()
            println("Deserialized object with string (actual bytecode implementation)")
            println(ObjectInputStream(ByteArrayInputStream(serialization.toByteArray(CHARSET))).use { it.readObject() })
            println("----------")
        }

        return StackOperation.Compound(
                TypeCreation.of(TypeDescription.ForLoadedType.of(ObjectInputStream::class.java)),
                Duplication.SINGLE,
                TypeCreation.of(TypeDescription.ForLoadedType.of(ByteArrayInputStream::class.java)),
                Duplication.SINGLE,
                TextConstant(serialization),
                FieldAccess.forField(FieldDescription.ForLoadedField(CHARSET_FIELD)).read(),
                MethodInvocation.invoke(
                        getMethodFromClassAndFilter(
                                TypeDescription.STRING,
                                getMatcherOfMethod("getBytes", Charset::class.java)
                        )
                ),
                MethodInvocation.invoke(
                        MethodDescription.ForLoadedConstructor(
                                @Suppress("RedundantLambdaOrAnonymousFunction")
                                { f: KFunction1<ByteArray, ByteArrayInputStream> -> f }(::ByteArrayInputStream).javaConstructor!!
                        )
                ),
                MethodInvocation.invoke(MethodDescription.ForLoadedConstructor(::ObjectInputStream.javaConstructor!!)),
                Duplication.SINGLE,
                MethodInvocation.invoke(MethodDescription.ForLoadedMethod(ObjectInputStream::readObject.javaMethod!!)),
                SWAP,
                MethodInvocation.invoke(MethodDescription.ForLoadedMethod(ObjectInputStream::close.javaMethod!!)),
        )
    }
}
