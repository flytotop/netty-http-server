
package com.farsunset.httpserver.netty.exception;

public class IllegalMethodNotAllowedException extends Exception {
    public IllegalMethodNotAllowedException() {
        super("METHOD NOT ALLOWED");
    }
}
