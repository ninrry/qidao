#include <jni.h>
#include <string>
#include <unistd.h>
#include <pthread.h>
#include <fcntl.h>
#include <android/log.h>

#define TAG "PikafishJni"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

// 声明 Pikafish 的主函数入口（刚才在 main.cpp 中已重命名）
extern int pikafish_main(int argc, char* argv[]);

static int stdin_pipe[2];
static int stdout_pipe[2];
static pthread_t engine_thread;
static bool is_initialized = false;

// 运行 Pikafish 引擎主循环的后台线程
void* engine_thread_func(void* arg) {
    LOGD("Native: Starting Pikafish engine main thread...");
    char* argv[] = { (char*)"pikafish" };
    pikafish_main(1, argv);
    LOGD("Native: Pikafish engine main thread finished.");
    return nullptr;
}

extern "C" {

JNIEXPORT jintArray JNICALL
Java_com_example_chessarena_engine_PikafishJni_startEngine(JNIEnv* env, jobject thiz) {
    jintArray result = env->NewIntArray(2);
    if (is_initialized) {
        // 如果已经初始化，直接复制当前活跃的管道 FD 并返回
        jint active_fds[2] = { stdin_pipe[1], stdout_pipe[0] };
        env->SetIntArrayRegion(result, 0, 2, active_fds);
        return result;
    }

    // 1. 创建 POSIX 双向重定向管道
    if (pipe(stdin_pipe) < 0 || pipe(stdout_pipe) < 0) {
        LOGD("Native Error: Failed to create POSIX pipes");
        return nullptr;
    }

    // 2. 将引擎的输入输出重定向至管道的读写端
    dup2(stdin_pipe[0], STDIN_FILENO);   // STDIN_FILENO (0) 从 stdin_pipe 读
    dup2(stdout_pipe[1], STDOUT_FILENO); // STDOUT_FILENO (1) 写入 stdout_pipe

    // 关闭不必要的辅助端以避免句柄泄漏
    close(stdin_pipe[0]);
    close(stdout_pipe[1]);

    // 3. 在 Native 后台线程中并发运行主搜索循环
    if (pthread_create(&engine_thread, nullptr, engine_thread_func, nullptr) != 0) {
        LOGD("Native Error: Failed to create engine pthread");
        return nullptr;
    }

    is_initialized = true;

    // 4. 将控制端的文件描述符返回给 Kotlin (fds[0] 为写入端，fds[1] 为读取端)
    // 注意：在 Native 侧我们不能关闭 stdin_pipe[1] 和 stdout_pipe[0]，它们将在 JVM/Kotlin 侧以 FileDescriptor 形式接管并由 ParcelFileDescriptor 管理关闭。
    jint active_fds[2] = { stdin_pipe[1], stdout_pipe[0] };
    env->SetIntArrayRegion(result, 0, 2, active_fds);
    return result;
}

JNIEXPORT void JNICALL
Java_com_example_chessarena_engine_PikafishJni_stopEngine(JNIEnv* env, jobject thiz) {
    if (!is_initialized) return;

    // 向 stdin 管道中写入 quit 指令，通知 Stockfish 退出循环
    write(stdin_pipe[1], "quit\n", 5);
    
    // 关闭 JVM 持有的管道端口
    close(stdin_pipe[1]);
    close(stdout_pipe[0]);

    // 等待线程回收
    pthread_join(engine_thread, nullptr);
    is_initialized = false;
    LOGD("Native: Engine successfully stopped.");
}

}
