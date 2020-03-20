# The ARMv7 is significanly faster due to the use of the hardware FPU
APP_ABI := armeabi-v7a x86
APP_STL := stlport_static
#STLPORT_FORCE_REBUILD := true
APP_CPPFLAGS += -O2 -fPIC -D_ANDROID_
# APP_CPPFLAGS += -D_ANDROID_LOG_
APP_PLATFORM := android-19
APP_OPTIM := release
NDK_TOOLCHAIN_VERSION := 4.6
