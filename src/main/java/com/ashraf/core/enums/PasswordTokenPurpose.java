package com.ashraf.core.enums;

public enum PasswordTokenPurpose {
    /** Staff invite: the account was created by a restaurant owner and has no usable password yet. */
    INVITE,
    /** Normal "forgot password" flow. */
    RESET
}
