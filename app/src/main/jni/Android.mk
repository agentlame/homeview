LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := avcodec
LOCAL_SRC_FILES := ../libs/$(TARGET_ARCH_ABI)/libavcodec.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../include/$(TARGET_ARCH_ABI)
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := avutil
LOCAL_SRC_FILES := ../libs/$(TARGET_ARCH_ABI)/libavutil.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../include/$(TARGET_ARCH_ABI)
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := swresample
LOCAL_SRC_FILES := ../libs/$(TARGET_ARCH_ABI)/libswresample.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../include/$(TARGET_ARCH_ABI)
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := homeviewJNI
LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%)
LOCAL_CPP_EXTENSION := .cpp
LOCAL_SRC_FILES :=  util/Log.cpp \
                    AudioFrameBuffer.cpp \
                    AudioCodecFFmpeg.cpp \
                    homeviewJNI.cpp

LOCAL_LDLIBS := -llog -lm -lz
LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%)
LOCAL_STATIC_LIBRARIES := avcodec avutil swresample
include $(BUILD_SHARED_LIBRARY)