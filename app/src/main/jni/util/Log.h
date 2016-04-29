//
// Created by drew on 4/23/16.
//

#ifndef HOMEVIEW_LOG_H
#define HOMEVIEW_LOG_H

#include <string>

enum LogType
{
    Error,
    Warning,
    Info,
    Debug,
    Verbose
};

class Log
{
public:

	static void Message(LogType type, const std::string &message);
    static void Message(LogType type, const char* message);
};


inline std::string format(const char* fmt, ...){
    int size = 512;
    char* buffer = 0;
    buffer = new char[size];
    va_list vl;
    va_start(vl, fmt);
    int nsize = vsnprintf(buffer, size, fmt, vl);
    if(size<=nsize){ //fail delete buffer and try again
        delete[] buffer;
        buffer = 0;
        buffer = new char[nsize+1]; //+1 for /0
        nsize = vsnprintf(buffer, size, fmt, vl);
    }
    std::string ret(buffer);
    va_end(vl);
    delete[] buffer;
    return ret;
}

#endif //HOMEVIEW_LOG_H
