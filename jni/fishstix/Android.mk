LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := dosbox

CG_SUBDIRS := .\

# Add more subdirs here, like src/subdir1 src/subdir2

MY_PATH := $(LOCAL_PATH)

LOCAL_PATH := $(DEVEL_PATH)/fishstix

CG_SRCDIR := $(LOCAL_PATH)
LOCAL_CFLAGS :=	-I$(LOCAL_PATH)/include \
				$(foreach D, $(CG_SUBDIRS), -I$(CG_SRCDIR)/$(D)) \
				-I$(LOCAL_PATH)/../sdl/include \
				-I$(LOCAL_PATH)/../dosbox \
				-I$(LOCAL_PATH)/../dosbox/include \
				-I$(LOCAL_PATH)/../dosbox/src \
				-I$(LOCAL_PATH) 
				
LOCAL_PATH := $(MY_PATH)

#Change C++ file extension as appropriate
LOCAL_CPP_EXTENSION := .cpp

LOCAL_SRC_FILES := $(foreach F, $(CG_SUBDIRS), $(addprefix $(F)/,$(notdir $(wildcard $(LOCAL_PATH)/$(F)/*.cpp))))

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	LOCAL_CFLAGS += -DC_TARGETCPU=ARMV7LE
endif

ifeq ($(TARGET_ARCH_ABI),armeabi)
    LOCAL_CFLAGS += -DC_TARGETCPU=ARMV4LE
endif

LOCAL_CPPFLAGS := $(LOCAL_CFLAGS)
LOCAL_CXXFLAGS := $(LOCAL_CFLAGS)
LOCAL_STATIC_LIBRARIES := locnet_al dosbox_main 
#LOCAL_LDFLAGS := /home/gene/workspace/DosBox/libs/armeabi/libandprof.a

include $(BUILD_SHARED_LIBRARY)

