APP_ABI := armeabi-v7a x86
APP_PLATFORM := android-21

APP_STL := c++_shared

APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -frtti

APP_CFLAGS := -g -ggdb -O0

#LIBCXX_FORCE_REBUILD := true
NDK_TOOLCHAIN_VERSION := 4.9
