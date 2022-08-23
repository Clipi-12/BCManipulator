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

/**
 * It cannot automatically detect classes inside methods (local classes).
 * It cannot automatically detect classes inside kotlin files [KT-16479](https://youtrack.jetbrains.com/issue/KT-16479)
 *
 * > ***Note***: Lambdas are local classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Import(vararg val value: String)
