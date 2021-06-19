//
// VirtualApp Native Project
//

#ifndef NDK_HELPER
#define NDK_HELPER

#include "VAJni.h"

class ScopeUtfString {
public:
    ScopeUtfString(jstring j_str);

    const char *c_str() {
        return _c_str;
    }

    ~ScopeUtfString();

private:
    jstring _j_str;
    const char *_c_str;
};

#endif //NDK_HELPER
