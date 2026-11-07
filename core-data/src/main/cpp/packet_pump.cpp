// Native packet pump for the local firewall.
//
// The Java/Kotlin VpnService hands us the TUN file descriptor; we read and discard packets from
// apps that were allowed into the tunnel. Nothing is forwarded anywhere, so no packet leaves the
// device through SecureBox.

#include <jni.h>
#include <unistd.h>
#include <cerrno>

extern "C" JNIEXPORT void JNICALL
Java_io_securebox_core_data_network_NativePacketPump_drain(JNIEnv * /*env*/, jobject /*thiz*/, jint fd) {
    char buffer[32768];
    for (;;) {
        ssize_t readBytes = ::read(fd, buffer, sizeof(buffer));
        if (readBytes < 0) {
            if (errno == EINTR) continue;
            break;
        }
        if (readBytes == 0) break;
    }
}
