# BC-Manipulator
## A simple library with stack operations that can be used with [ByteBuddy](https://bytebuddy.net/)
### Copyright (C) 2022  Clipi (GitHub: [Clipi-12](https://github.com/Clipi-12))

This library provides stack operations that can be integrated into the ByteBuddy toolchain, such us
* Simple operations like GOTO
* [Forth-like operations](https://www.forth.com/starting-forth/2-stack-manipulation-operators-arithmetic/#:~:text=Here%20is%20a%20list%20of%20several%20stack%20manipulation%20operators%3A)
* Very simple [Frame operations](https://asm.ow2.io/asm4-guide.pdf#page=45)
* JVM-specific operations like Try-Catch statements and delegation invocations

It also contains useful ASM visitors that allow injecting bytecode in different ways, supporting
ByteBuddy's runtime class creation and class redefinition.

## License
> Lesser General Public License version 3

> BC-Manipulator is free software: you can redistribute it and/or modify
> it under the terms of the GNU Lesser General Public License as published by
> the Free Software Foundation, either version 3 of the License, or
> (at your option) any later version.  
> BC-Manipulator is distributed in the hope that it will be useful,
> but WITHOUT ANY WARRANTY; without even the implied warranty of
> MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
> GNU Lesser General Public License for more details.  
> You should have received a copy of the GNU Lesser General Public License
> along with BC-Manipulator. If not, see <https://www.gnu.org/licenses/>.
