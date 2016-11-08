package com.hesine.nmsg.business;

public interface Pipe {
    int MEMORY_SUCCESS = 2;
    int LOCAL_SUCCESS = 1;
    int NET_SUCCESS = 0;
    int PARSE_FAIL = -1;
    int NET_FAIL = -2;
    int NET_TIMEOUT = -3;
    int SERVER_ERROR = -4;

    void complete(Object owner, Object data, int success);
}
