cmake_minimum_required(VERSION 3.22)
project(openmpt_jni C)

set(LIBOPENMPT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/libopenmpt")
set(LIBOPENMPT_PREBUILT_ROOT "${LIBOPENMPT_ROOT}/prebuilt")
set(LIBOPENMPT_ABI "${ANDROID_ABI}")

set(LIBOPENMPT_INCLUDE_DIR "${LIBOPENMPT_ROOT}/source")
set(LIBOPENMPT_SOURCE_INCLUDE_DIR "${LIBOPENMPT_ROOT}/source/libopenmpt")
set(LIBOPENMPT_LIBRARY "${LIBOPENMPT_PREBUILT_ROOT}/${LIBOPENMPT_ABI}/${LIBOPENMPT_ABI}/libopenmpt.so")

if(NOT EXISTS "${LIBOPENMPT_LIBRARY}")
    message(FATAL_ERROR
        "Prebuilt libopenmpt library not found at ${LIBOPENMPT_LIBRARY}.\n"
        "Run scripts/build-libopenmpt.sh first to download and build libopenmpt for ${LIBOPENMPT_ABI}.")
endif()

find_library(log-lib log)
find_library(z-lib z)

add_library(openmpt SHARED IMPORTED GLOBAL)
set_target_properties(openmpt PROPERTIES
    IMPORTED_LOCATION "${LIBOPENMPT_LIBRARY}"
    INTERFACE_INCLUDE_DIRECTORIES "${LIBOPENMPT_INCLUDE_DIR};${LIBOPENMPT_SOURCE_INCLUDE_DIR}"
)

add_library(openmpt_jni SHARED openmpt_jni.c)
target_include_directories(openmpt_jni PRIVATE "${LIBOPENMPT_INCLUDE_DIR}")
target_link_libraries(openmpt_jni openmpt ${log-lib} ${z-lib})
