package com.transport.authservice.exception;

public class InvalidCredentialsException extends RuntimeException{

    public InvalidCredentialsException(String msg){
        super(msg);
    }
}
