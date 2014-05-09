LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := dosbox_main

CG_SUBDIRS := \
src/dos \
src/hardware \
src/hardware/serialport \
src \
src/cpu \
src/cpu/core_dynrec \
src/cpu/core_dyn_x86 \
src/cpu/core_full \
src/cpu/core_normal \
src/fpu \
src/gui \
src/gui/gui_tk \
src/gui/zmbv \
src/ints \
src/libs \
src/misc \
src/shell \

MY_PATH := $(LOCAL_PATH)

#Fix me
LOCAL_PATH := "/home/gene/Downloads/DosBoxTurbo_Source/com.fishstix.dbox/jni/dosbox"

CG_SRCDIR := $(LOCAL_PATH)
LOCAL_CFLAGS :=	-I$(LOCAL_PATH)/include \
				$(foreach D, $(CG_SUBDIRS), -I$(CG_SRCDIR)/$(D)) \
				-I$(LOCAL_PATH)/../sdl/include \
				-I$(LOCAL_PATH)/../sdl_net/include \
				-I$(LOCAL_PATH)/../fishstix/include \
				-I$(LOCAL_PATH) 
LOCAL_PATH := $(MY_PATH)

#Change C++ file extension as appropriate
LOCAL_CPP_EXTENSION := .cpp

LOCAL_SRC_FILES := $(foreach F, $(CG_SUBDIRS), $(addprefix $(F)/,$(notdir $(wildcard $(LOCAL_PATH)/$(F)/*.cpp))))

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	LOCAL_CFLAGS += -DC_TARGETCPU=ARMV7LE 
	LOCAL_ARM_MODE := arm
	# Enable NEON optimizations for big speed boost
	# LOCAL_ARM_NEON := true
	# LOCAL_CFLAGS += -DHAVE_NEON=1
endif

ifeq ($(TARGET_ARCH_ABI),armeabi)
    LOCAL_CFLAGS += -DC_TARGETCPU=ARMV4LE
endif

LOCAL_CPPFLAGS := $(LOCAL_CFLAGS)
LOCAL_CXXFLAGS := $(LOCAL_CFLAGS)
LOCAL_STATIC_LIBRARIES := sdl_net mt32emu

include $(BUILD_STATIC_LIBRARY)

