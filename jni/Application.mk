# The ARMv7 is significanly faster due to the use of the hardware FPU
#APP_ABI := armeabi armeabi-v7a x86
APP_ABI := armeabi-v7a x86
APP_STL := stlport_static
#STLPORT_FORCE_REBUILD := true
APP_CPPFLAGS += -O2 -fPIC
APP_PLATFORM := android-8
APP_OPTIM := release
NDK_TOOLCHAIN_VERSION := 4.6
