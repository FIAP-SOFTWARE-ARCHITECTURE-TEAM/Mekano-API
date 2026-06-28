package com.fiap.mekano.shared.exception;

public class AppException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final int statusCode;

    public AppException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public AppException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    public int statusCode() {
        return statusCode;
    }
       
    
    public int getStatus() {
        return statusCode;
    }
}
