
package com.farsunset.httpserver.netty.exception;

public class IllegalPathNotFoundException extends Exception {
    public IllegalPathNotFoundException() {
        super("PATH NOT FOUND");
    }
}
