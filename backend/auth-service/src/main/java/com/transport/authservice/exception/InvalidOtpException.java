package com.transport.authservice.exception;

public class InvalidOtpException extends RuntimeException{

    public InvalidOtpException(String msg){
        super(msg);
    }
}
