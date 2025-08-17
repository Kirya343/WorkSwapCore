package org.workswap.core.services.components;

import java.io.InputStream;

import org.springframework.core.io.InputStreamResource;

public class MultipartInputStreamFileResource extends InputStreamResource {

    private final String filename;
    private final long contentLength;

    public MultipartInputStreamFileResource(InputStream inputStream, String filename, long contentLength) {
        super(inputStream);
        this.filename = filename;
        this.contentLength = contentLength;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long contentLength() {
        return this.contentLength;
    }
}