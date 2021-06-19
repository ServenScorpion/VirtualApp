
#include <jni.h>
#include "Helper.h"

ScopeUtfString::ScopeUtfString(jstring j_str) {
    _j_str = j_str;
    _c_str = getEnv()->GetStringUTFChars(j_str, NULL);
}

ScopeUtfString::~ScopeUtfString() {
    getEnv()->ReleaseStringUTFChars(_j_str, _c_str);
}
