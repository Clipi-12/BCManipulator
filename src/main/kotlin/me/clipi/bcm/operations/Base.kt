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

package me.clipi.bcm.operations

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.implementation.bytecode.StackManipulation
import net.bytebuddy.jar.asm.MethodVisitor

internal val DEBUG = Clinit.debug(false)

private object Clinit {
    fun debug(DEBUG: Boolean): Boolean {
        if (DEBUG) {
            println("Currently debugging Clipi->ByteCodeManipulation")
        }
        return DEBUG
    }
}

public abstract class StackOperation : StackManipulation.AbstractBase() {
    protected var initialNextLocal: Int = 0
        private set

    internal fun initialNextLocal(value: Int) {
        initialNextLocal = value
    }

    internal var maxLocals = initialNextLocal
    public var newLocals: Int = 0
        protected set(value) {
            maxLocals = maxLocals.coerceAtLeast(value + initialNextLocal)
            field = value
        }

    final override fun apply(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context
    ): StackManipulation.Size {
        maxLocals = initialNextLocal
        newLocals = 0
        return execute(methodVisitor, implementationContext)
    }

    protected abstract fun execute(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context
    ): StackManipulation.Size

    /**
     * @see [StackManipulation.Compound][apply]
     */
    public open class Compound(protected vararg val operations: StackManipulation) : StackOperation() {
        override fun execute(
            methodVisitor: MethodVisitor,
            implementationContext: Implementation.Context
        ): StackManipulation.Size {
            var size = StackManipulation.Size.ZERO
            for (op in operations) {
                if (op is StackOperation) {
                    op.initialNextLocal = newLocals + initialNextLocal
                    size = size.aggregate(op.apply(methodVisitor, implementationContext))
                    newLocals += op.newLocals
                    maxLocals = maxLocals.coerceAtLeast(op.maxLocals)
                } else {
                    size = size.aggregate(op.apply(methodVisitor, implementationContext))
                }
            }
            return size
        }


        public class Parallel(vararg operations: StackManipulation) : Compound(*operations) {
            override fun execute(
                methodVisitor: MethodVisitor,
                implementationContext: Implementation.Context
            ): StackManipulation.Size {
                maxLocals = initialNextLocal
                var size = 0
                var sizeImpact: Int? = null
                for (op in operations) {
                    val opSize = op.apply(methodVisitor, implementationContext)
                    if (sizeImpact != null) {
                        if (op !is EndsInRetOrThr) check(sizeImpact == opSize.sizeImpact) {
                            return@check "Parallel stack operations must have the same size impact on the stack." +
                                "$op had an impact of ${opSize.sizeImpact} elements while" +
                                "the previous operation had an impact of $sizeImpact elements."
                        }
                    } else if (op !is EndsInRetOrThr) {
                        sizeImpact = opSize.sizeImpact
                    }
                    size = size.coerceAtLeast(opSize.maximalSize)
                    if (op is StackOperation) maxLocals = maxLocals.coerceAtLeast(op.maxLocals)
                }
                return StackManipulation.Size(sizeImpact ?: 0 /* They all were EndsInRetOrThr */, size)
            }

            public class EndsInRetOrThr(vararg operations: StackManipulation) : Compound(*operations)
        }
    }
}

public abstract class BytecodeOperation : ByteCodeAppender {
    final override fun apply(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ): ByteCodeAppender.Size {
        val op = execute(methodVisitor, implementationContext, instrumentedMethod)
        val maxLocals: Int
        val opSize: StackManipulation.Size
        if (op is StackOperation) {
            op.initialNextLocal(instrumentedMethod.stackSize)
            opSize = op.apply(methodVisitor, implementationContext)
            maxLocals = op.maxLocals
        } else {
            opSize = op.apply(methodVisitor, implementationContext)
            maxLocals = instrumentedMethod.stackSize
        }
        return ByteCodeAppender.Size(opSize.maximalSize, maxLocals)
    }

    public abstract fun execute(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ): StackManipulation
}