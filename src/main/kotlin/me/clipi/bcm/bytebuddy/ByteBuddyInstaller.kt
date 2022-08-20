package me.clipi.bcm.bytebuddy

import net.bytebuddy.agent.ByteBuddyAgent

object ByteBuddyInstaller {
    private var installed = false

    fun install() {
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
