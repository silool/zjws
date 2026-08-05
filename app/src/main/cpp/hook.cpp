#include <jni.h>
#include <string.h>
#include <cstdio>
#include <dlfcn.h>
#include <unistd.h>
#include <sys/mman.h>
#include <android/log.h>

#define TAG "TimFix"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define MUL 5

// ============ ARM64 inline hook ============
static void write_jump(char *addr, void *target) {
    size_t page = (size_t)addr & ~0xFFF;
    mprotect((void *)page, 0x1000, PROT_READ | PROT_WRITE | PROT_EXEC);
    uint32_t *insn = (uint32_t *)addr;
    // LDR X17, #8; BR X17; .quad target
    insn[0] = 0x58000051; // ldr x17, #8
    insn[1] = 0xD61F0220; // br x17
    *(void **)(insn + 2) = target;
    __builtin___clear_cache(addr, addr + 24);
    mprotect((void *)page, 0x1000, PROT_READ | PROT_EXEC);
}

// ============ Timing patcher ============
static void patch_timing(char *buf, ssize_t len) {
    if (!buf || len < 10) return;
    char *p = buf;
    char *end = buf + len;
    while (p < end - 10) {
        // Match "duration": 12345
        if (memcmp(p, "\"duration\"", 10) == 0) {
            p += 10;
            while (p < end && (*p == ' ' || *p == ':' || *p == '\t')) p++;
            if (*p >= '0' && *p <= '9') {
                long v = 0;
                char *start = p;
                while (p < end && *p >= '0' && *p <= '9') {
                    v = v * 10 + (*p - '0');
                    p++;
                }
                if (v >= 1000) {
                    int new_len = snprintf(start, p - start + 10, "%ld", v * MUL);
                    if (new_len > 0 && new_len < (p - start)) {
                        memset(start + new_len, ' ', (p - start) - new_len);
                    }
                }
            }
            continue;
        }
        // Match "totalTime": / "battleTime": / "elapsed":
        if ((memcmp(p, "\"totalTime\"", 9) == 0 && (p += 9)) ||
            (memcmp(p, "\"battleTime\"", 11) == 0 && (p += 11)) ||
            (memcmp(p, "\"playTime\"", 9) == 0 && (p += 9)) ||
            (memcmp(p, "\"elapsed\"", 7) == 0 && (p += 7))) {
            while (p < end && (*p == ' ' || *p == ':' || *p == '\t')) p++;
            if (*p >= '0' && *p <= '9') {
                long v = 0;
                char *start = p;
                while (p < end && *p >= '0' && *p <= '9') {
                    v = v * 10 + (*p - '0');
                    p++;
                }
                if (v >= 1000) {
                    int new_len = snprintf(start, p - start + 10, "%ld", v * MUL);
                    if (new_len > 0 && new_len < (p - start)) {
                        memset(start + new_len, ' ', (p - start) - new_len);
                    }
                }
            }
            continue;
        }
        p++;
    }
}

// ============ Original function pointers ============
typedef ssize_t (*send_t)(int, const void *, size_t, int);
typedef int (*SSL_write_t)(void *, const void *, int);

static send_t orig_send = NULL;
static SSL_write_t orig_ssl_write = NULL;

ssize_t hooked_send(int fd, const void *buf, size_t len, int flags) {
    char *data = (char *)buf;
    patch_timing(data, len);
    return orig_send(fd, buf, len, flags);
}

int hooked_ssl_write(void *ssl, const void *buf, int len) {
    char *data = (char *)buf;
    patch_timing(data, len);
    return orig_ssl_write(ssl, buf, len);
}

// ============ Init ============
__attribute__((constructor))
void init_hook() {
    LOGI("TimFix loaded — installing hooks...");

    void *handle = dlopen("libc.so", RTLD_NOW);
    if (handle) {
        orig_send = (send_t)dlsym(handle, "send");
        if (orig_send) {
            write_jump((void *)orig_send, (void *)hooked_send);
            LOGI("send hooked");
        }
    }

    handle = dlopen("libssl.so", RTLD_NOW);
    if (handle) {
        orig_ssl_write = (SSL_write_t)dlsym(handle, "SSL_write");
        if (orig_ssl_write) {
            write_jump((void *)orig_ssl_write, (void *)hooked_ssl_write);
            LOGI("SSL_write hooked");
        }
    }
}
