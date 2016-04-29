//
// Created by drew on 4/23/16.
//

#include "Log.h"

#include <stdio.h>
#include <android/log.h>

void Log::Message(LogType type, const std::string &message)
{
	Log::Message(type, message.c_str());
}

void Log::Message(LogType type, const char* message)
{

	int level;
	switch(type)
	{
	case Verbose:
		level = ANDROID_LOG_VERBOSE;
		break;
	case Debug:
		level = ANDROID_LOG_DEBUG;
		break;
	case Info:
		level = ANDROID_LOG_INFO;
		break;
	case Warning:
		level = ANDROID_LOG_WARN;
		break;
	default:
	case Error:
		level = ANDROID_LOG_ERROR;
		break;
	}


	__android_log_print(level ,"com.monsterbutt.homeview.native", "%s\n", message);
}