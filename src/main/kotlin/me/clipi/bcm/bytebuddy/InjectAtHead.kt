package me.clipi.bcm.bytebuddy

import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.field.FieldList
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.method.MethodList
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.jar.asm.ClassVisitor
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.pool.TypePool
import net.bytebuddy.utility.CompoundList
import net.bytebuddy.utility.OpenedClassReader

public class InjectAtHead(
    private val methodMatcher: ElementMatcher<MethodDescription>,
    private val impl: ByteCodeAppender
) :
    AsmVisitorWrapper.AbstractBase() {
    override fun wrap(
        instrumentedType: TypeDescription,
        classVisitor: ClassVisitor,
        implementationContext: Implementation.Context,
        typePool: TypePool,
        fields: FieldList<FieldDescription.InDefinedShape?>,
        methods: MethodList<*>,
        writerFlags: Int,
        readerFlags: Int
    ): ClassVisitor {
        val mappedMethods: HashMap<String, MethodDescription> = HashMap()
        for (methodDescription in CompoundList.of(
            methods,
            MethodDescription.Latent.TypeInitializer(instrumentedType)
        )) {
            mappedMethods[methodDescription.internalName + methodDescription.descriptor] = methodDescription
        }
        return ClassV(classVisitor, methodMatcher, mappedMethods, impl, implementationContext)
    }

    private class ClassV(
        classVisitor: ClassVisitor?,
        private val methodMatcher: ElementMatcher<MethodDescription>,
        private val methods: HashMap<String, MethodDescription>,
        private val impl: ByteCodeAppender,
        private val implementationContext: Implementation.Context
    ) : ClassVisitor(OpenedClassReader.ASM_API, classVisitor) {
        override fun visitMethod(
            modifiers: Int,
            internalName: String,
            descriptor: String,
            signature: String?,
            exception: Array<String?>?
        ): MethodVisitor? {
            val methodVisitor =
                super.visitMethod(modifiers, internalName, descriptor, signature, exception) ?: return null
            val methodDescription = methods[internalName + descriptor]
            return if (methodDescription == null || !methodMatcher.matches(methodDescription)) methodVisitor else MethodV(
                methodVisitor,
                impl,
                implementationContext,
                methodDescription
            )
        }
    }

    private class MethodV(
        methodVisitor: MethodVisitor,
        private val impl: ByteCodeAppender,
        private val implementationContext: Implementation.Context,
        private val methodDescription: MethodDescription
    ) : MethodVisitor(OpenedClassReader.ASM_API, methodVisitor) {
        private lateinit var size: ByteCodeAppender.Size

        /**
         * Starts the visit of the method's code, if any (i.e. non abstract method).
         */
        override fun visitCode() {
            super.visitCode()
            size = impl.apply(mv, implementationContext, methodDescription)
        }

        /**
         * Visits the maximum stack size and the maximum number of local variables of the method.
         *
         * @param maxStack  maximum stack size of the method.
         * @param maxLocals maximum number of local variables for the method.
         */
        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            mv.visitMaxs(
                size.operandStackSize.coerceAtLeast(maxStack),
                size.localVariableSize.coerceAtLeast(maxLocals)
            )
        }
    }
}