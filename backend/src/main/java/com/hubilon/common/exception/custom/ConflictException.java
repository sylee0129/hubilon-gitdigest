package com.hubilon.common.exception.custom;

public class ConflictException extends ServiceException {

    private final Object conflictData;

    public ConflictException(String message, Object conflictData) {
        super(message);
        this.conflictData = conflictData;
    }

    public Object getConflictData() {
        return conflictData;
    }
}
