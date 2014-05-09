/*
 *  Copyright (C) 2012 Fishstix - (ruebsamen.gene@gmail.com)
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
#define CPU_AUTODETERMINE_NONE		0x00
#define CPU_AUTODETERMINE_CORE		0x01
#define CPU_AUTODETERMINE_CYCLES	0x02
/*
#define LOGD(LOG_TAG, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGV(LOG_TAG, ...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGE(LOG_TAG, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_TAG "DosBoxTurbo"
*/
#include <jni.h>
#include <stdlib.h>
//#include <android/log.h>
#include "config.h"
#include "loader.h"
#include "render.h"
int dosbox_main(int argc, const char* argv[]);
void swapInNextDisk(bool pressed);
void DOSBOX_UnlockSpeed( bool pressed );
void CPU_CycleIncrease(bool pressed);
void CPU_CycleDecrease(bool pressed);

char arg_start_command[256]="";
extern struct loader_config myLoader;
extern struct loader_config *loadf;

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeStart(JNIEnv * env, jobject obj, jobject bitmap, jint width, jint height, jstring confpath)
{
	Android_Init(env, obj, bitmap, width, height);
	
	const char * argv[] = { "dosbox", "-conf", (env)->GetStringUTFChars(confpath,NULL), "-c", arg_start_command  };
	dosbox_main((!arg_start_command[0])?3:5, argv);
	
	Android_ShutDown();
}

extern Render_t render;
extern bool CPU_CycleAutoAdjust;
extern bool CPU_SkipCycleAutoAdjust;
//bool fastforward_on = false;
//extern Bit32s CPU_Cycles;
extern Bit32s CPU_CycleMax;
extern Bit32s CPU_CycleLimit;
extern Bit32s CPU_OldCycleMax;
extern Bit32s CPU_CyclePercUsed;
extern Bitu CPU_AutoDetermineMode;
extern bool ticksLocked;

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeSetOption(JNIEnv * env, jobject obj, jint option, jint value, jobject value2, jboolean extra)
{
	switch (option) {
		case 1:
			myLoader.soundEnable = value;
			enableSound = (value != 0);
			break;
		case 2:
			myLoader.memsize = value;
			break;
		case 10:
			CPU_CycleAutoAdjust = false;
			CPU_CycleMax = value;
			CPU_OldCycleMax = value;
			CPU_SkipCycleAutoAdjust = false;
			CPU_CycleLimit = value;
			myLoader.cycles = value;
			break;
		case 11:
			myLoader.frameskip = value;
			render.frameskip.max = value;
			break;
		case 12:
			myLoader.refreshHack = value;
			enableRefreshHack = (value != 0);
			break;
		case 13:
			myLoader.cycleHack = value;
			enableCycleHack = (value != 0);
			break;
		case 14:
			myLoader.mixerHack = value;
			enableMixerHack = (value != 0);
			break;
		case 15:
			//myLoader.autoCPU = value;

			if (!CPU_CycleAutoAdjust) {
				if (!value) {
					CPU_AutoDetermineMode=CPU_AUTODETERMINE_NONE;
				} else {
					CPU_AutoDetermineMode|=CPU_AUTODETERMINE_CYCLES;
					CPU_CyclePercUsed = 100;
				}
			}
			break;
		case 16:
			DOSBOX_UnlockSpeed((value != 0));
			break;
		case 17:	// cycleadjust
			if (value) {
				CPU_CycleIncrease(true);
			} else {
				CPU_CycleDecrease(true);
			}
			break;
		case 21:
			swapInNextDisk(true);
			break;
		case 50:
			strcpy(arg_start_command, (env)->GetStringUTFChars((jstring)value2, 0));
			break;
	}
}

extern "C" jint Java_com_fishstix_dosbox_DosBoxControl_nativeGetCycleCount(JNIEnv * env, jobject obj) {
	if (CPU_CycleAutoAdjust) {
		return CPU_CyclePercUsed;
	}
	return CPU_CycleMax;
}

extern "C" jboolean Java_com_fishstix_dosbox_DosBoxControl_nativeGetAutoAdjust(JNIEnv * env, jobject obj) {
	return (jboolean)CPU_CycleAutoAdjust;
}

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeInit(JNIEnv * env, jobject obj, jobject ctx)
{
	loadf = 0;
	myLoader.memsize = 2;
	myLoader.bmph = 0;
	myLoader.videoBuffer = 0;

	myLoader.abort = 0;
	myLoader.pause = 0;

	myLoader.frameskip = 0;
	myLoader.cycles = 3000;
	myLoader.soundEnable = 1;
	myLoader.cycleHack = 1;
	myLoader.refreshHack = 1;
	//myLoader.mixerHack = 1;
	//myLoader.autoCPU = 1;
}

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativePause(JNIEnv * env, jobject obj, jint state)
{
	if ((state == 0) || (state == 1))
		myLoader.pause = state;
	else
		myLoader.pause = (myLoader.pause)?0:1;
}

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeStop(JNIEnv * env, jobject obj)
{
	myLoader.abort = 1;
}

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeShutDown(JNIEnv * env, jobject obj)
{
	myLoader.bmph = 0;
	myLoader.videoBuffer = 0;
}

