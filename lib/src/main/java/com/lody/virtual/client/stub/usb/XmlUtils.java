package com.lody.virtual.client.stub.usb;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

class XmlUtils {
    public static final void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != parser.START_TAG
                && type != parser.END_DOCUMENT) {
            ;
        }
    }
}
