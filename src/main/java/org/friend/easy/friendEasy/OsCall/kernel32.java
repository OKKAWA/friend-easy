package org.friend.easy.friendEasy.OsCall;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface kernel32 extends Library {
    kernel32 INSTANCE = Native.load("kernel32", kernel32.class);
    boolean Beep(int freq, int duration);

}
