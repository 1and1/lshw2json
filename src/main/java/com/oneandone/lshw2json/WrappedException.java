/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oneandone.lshw2json;

/**
 * A runtime exception used for wrapping checked exceptions.
 * Useful in the environment of functional programming.
 * @author stephan
 */
public class WrappedException extends RuntimeException {

    public WrappedException(Exception cause) {
        super(cause);
    }
}
