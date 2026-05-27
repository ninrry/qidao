cmake_minimum_required(VERSION 3.22.1)

project(chessarena-native)

# 全局编译器设置
add_compile_options(
    -std=c++17
    -O3
    -DNDEBUG
    -DANDROID
)

# ----------------- libpikafish.so -----------------
set(PIKAFISH_DIR ${CMAKE_CURRENT_SOURCE_DIR}/pikafish)

# 递归搜集所有 Pikafish 的 C++ 源码
file(GLOB_RECURSE PIKAFISH_SOURCES "${PIKAFISH_DIR}/*.cpp")

# 排除 universal 文件夹以避免与非多架构通用编译冲突
list(FILTER PIKAFISH_SOURCES EXCLUDE REGEX ".*/universal/.*")

add_library(pikafish SHARED
    pikafish_jni.cpp
    ${PIKAFISH_SOURCES}
)

target_include_directories(pikafish PRIVATE ${PIKAFISH_DIR})

# 按目标架构配置 SIMD 和 CPU 指令
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_definitions(pikafish PRIVATE
        IS_64BIT
        USE_NEON=8
        USE_POPCNT
    )
    target_compile_options(pikafish PRIVATE
        -march=armv8-a
    )
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    target_compile_definitions(pikafish PRIVATE
        IS_64BIT
        USE_SSE2
        USE_POPCNT
        ZSTD_DISABLE_ASM
    )
    target_compile_options(pikafish PRIVATE
        -msse2
    )
else()
    target_compile_definitions(pikafish PRIVATE
        IS_64BIT
        USE_POPCNT
    )
endif()

find_library(log-lib log)
target_link_libraries(pikafish
    ${log-lib}
    android
)

# ----------------- libslowrenju.so -----------------
set(SLOWRENJU_DIR ${CMAKE_CURRENT_SOURCE_DIR}/slowrenju)

# 搜集 SlowRenju 源码，排除桌面端的 Common/main.cpp
file(GLOB_RECURSE SLOWRENJU_SOURCES "${SLOWRENJU_DIR}/*.cpp")
list(FILTER SLOWRENJU_SOURCES EXCLUDE REGEX ".*Common/main\\.cpp$")

add_library(slowrenju SHARED
    ${SLOWRENJU_SOURCES}
)

# 包含 SlowRenju 目录头文件
target_include_directories(slowrenju PRIVATE 
    ${SLOWRENJU_DIR}
    ${SLOWRENJU_DIR}/Headers
)

target_compile_definitions(slowrenju PRIVATE
    ANDROID
)

target_link_libraries(slowrenju
    ${log-lib}
)
