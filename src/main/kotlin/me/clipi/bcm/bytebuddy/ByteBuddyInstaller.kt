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

import net.bytebuddy.agent.ByteBuddyAgent

public object ByteBuddyInstaller {
    private var installed = false

    public fun install() {
        if (installed) return

        val currentAllowAttachSelf = System.getProperty("jdk.attach.allowAttachSelf", "false")
        if ("" == currentAllowAttachSelf || "true" == currentAllowAttachSelf) {
            /*
             The property can be changed after the JVM has started, so we have to take that possibility into account.
             At first, I thought it wouldn't be necessary, but here comes fucking IntelliJ IDEA trying to feed gradle
             a bigillion different system properties like "file.encoding", "java.vm.vendor", "line.separator",
             and guess fucking what, "jdk.attach.allowAttachSelf". So basically the entire purpose of that property
             is lost, as it wasn't set via a CLI, but instead was executed in code, which simply doesn't do anything
             as the value is already fetched, so we just have to scratch that property completely because we can't
             know if it was set in code or via a CLI. Disabling this IntelliJ IDEA behaviour is probably a setting
             that the user can change, but there is not much we can do during code execution. Fuck you IntelliJ IDEA
             - Clipi
             */
            System.setProperty("jdk.attach.allowAttachSelf", "false")
            try {
                ByteBuddyAgent.install()
                installed = true
            } finally {
                System.setProperty("jdk.attach.allowAttachSelf", currentAllowAttachSelf)
            }
        } else {
            ByteBuddyAgent.install()
            installed = true
        }
    }
}
