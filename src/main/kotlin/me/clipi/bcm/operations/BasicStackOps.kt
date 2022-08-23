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

import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.Duplication
import net.bytebuddy.implementation.bytecode.Removal
import net.bytebuddy.implementation.bytecode.StackManipulation
import net.bytebuddy.jar.asm.Label
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes

public object SWAP : StackOperation() {
    override fun execute(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context
    ): StackManipulation.Size {
        methodVisitor.visitInsn(Opcodes.SWAP)
        return StackManipulation.Size.ZERO
    }
}

public class GOTO(private val label: Label) : StackOperation() {
    override fun execute(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context
    ): StackManipulation.Size {
        methodVisitor.visitJumpInsn(Opcodes.GOTO, label)
        return StackManipulation.Size.ZERO
    }
}

/**
 * ***Non-standard operation.***
 *
 * Assuming all objects v<sub>1</sub>, v<sub>2</sub>, and v<sub>3</sub> were 32-bit objects on a stack like so
 * > &nbsp;&nbsp;&nbsp;&nbsp;..., v<sub>1</sub>, v<sub>2</sub>, v<sub>3</sub>
 *
 * this operation would modify the stack so that it would end up like so
 * > &nbsp;&nbsp;&nbsp;&nbsp;..., v<sub>**3**</sub>, v<sub>2</sub>, v<sub>**1**</sub>
 */
public object SWAP1 : StackOperation.Compound(
    REVERSE_ROT,
    SWAP
)

/**
 * ***Non-standard operation.***
 * [ROT](https://www.forth.com/starting-forth/2-stack-manipulation-operators-arithmetic/#:~:text=Here%20is%20a%20list%20of%20several%20stack%20manipulation%20operators%3A)
 */
public object ROT : StackOperation.Compound(
    REVERSE_ROT,
    REVERSE_ROT
)

/**
 * ***Non-standard operation.***
 * [OVER](https://www.forth.com/starting-forth/2-stack-manipulation-operators-arithmetic/#:~:text=Here%20is%20a%20list%20of%20several%20stack%20manipulation%20operators%3A)
 */
public object OVER : StackOperation.Compound(
    Duplication.DOUBLE,
    Removal.SINGLE
)

/**
 * ***Non-standard operation.***
 * @see [ROT]
 */
@Suppress("ClassName")
public object REVERSE_ROT : StackOperation() {
    override fun execute(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context
    ): StackManipulation.Size {
        methodVisitor.visitInsn(Opcodes.DUP_X2)
        methodVisitor.visitInsn(Opcodes.POP)
        return StackManipulation.Size(0, 1)
    }
}
