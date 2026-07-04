package com.beanchiuling.common.utils;

public final class PermissionUtils {

    private PermissionUtils() {}

    public static final long VIEW_CHANNELS       = 1L;
    public static final long SEND_MESSAGES       = 1L << 1;
    public static final long READ_HISTORY        = 1L << 2;
    public static final long MANAGE_MESSAGES     = 1L << 3;
    public static final long MANAGE_CHANNELS     = 1L << 4;
    public static final long MANAGE_ROLES        = 1L << 5;
    public static final long KICK_MEMBERS        = 1L << 6;
    public static final long BAN_MEMBERS         = 1L << 7;
    public static final long ADMINISTRATOR       = 1L << 8;
    public static final long MANAGE_SERVER       = 1L << 9;
    public static final long MENTION_EVERYONE    = 1L << 10;
    public static final long EMBED_LINKS         = 1L << 11;
    public static final long ATTACH_FILES        = 1L << 12;
    public static final long ADD_REACTIONS       = 1L << 13;

    public static final long DEFAULT_PERMISSIONS =
            VIEW_CHANNELS | SEND_MESSAGES | READ_HISTORY | EMBED_LINKS | ATTACH_FILES | ADD_REACTIONS;

    public static boolean has(long permissions, long flag) {
        if ((permissions & ADMINISTRATOR) == ADMINISTRATOR) return true;
        return (permissions & flag) == flag;
    }

    public static long grant(long permissions, long flag) {
        return permissions | flag;
    }

    public static long revoke(long permissions, long flag) {
        return permissions & ~flag;
    }
}
