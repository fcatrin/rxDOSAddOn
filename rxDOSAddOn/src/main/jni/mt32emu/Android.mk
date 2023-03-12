LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES:= src/AReverbModel.cpp \
  src/BReverbModel.cpp \
  src/DelayReverb.cpp \
  src/File.cpp \
  src/FileStream.cpp \
  src/FreeverbModel.cpp \
  src/LA32Ramp.cpp \
  src/LA32WaveGenerator.cpp \
  src/LegacyWaveGenerator.cpp \
  src/Part.cpp \
  src/Partial.cpp \
  src/PartialManager.cpp \
  src/Poly.cpp \
  src/ROMInfo.cpp \
  src/Synth.cpp \
  src/Tables.cpp \
  src/TVA.cpp \
  src/TVF.cpp \
  src/TVP.cpp \
  src/freeverb/allpass.cpp \
  src/freeverb/comb.cpp \
  src/freeverb/revmodel.cpp \
  src/sha1/sha1.cpp
#include $(BUILD_EXECUTABLE)

LOCAL_CFLAGS	:= 	 -DSYS_ANDROID=1 \
					 -I$(LOCAL_PATH)/src \
					 -I$(LOCAL_PATH)/../dosbox/include

LOCAL_LDLIBS := -llog
LOCAL_MODULE:= mt32emu

include $(BUILD_STATIC_LIBRARY)
