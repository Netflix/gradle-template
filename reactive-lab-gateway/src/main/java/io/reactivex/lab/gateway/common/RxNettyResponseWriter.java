package io.reactivex.lab.gateway.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

import rx.Observable;
import rx.subjects.ReplaySubject;

public class RxNettyResponseWriter extends Writer {

    private final HttpServerResponse<ByteBuf> response;
    private final ReplaySubject<Void> completionObservable = ReplaySubject.create();

    public RxNettyResponseWriter(HttpServerResponse<ByteBuf> response) {
        this.response = response;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        // assertions stole from StringWriter
        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        response.write(Unpooled.copiedBuffer(cbuf, off, len, Charset.defaultCharset()));
    }

    @Override
    public void flush() throws IOException {
        response.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            response.close();
            completionObservable.onCompleted();
        } catch (Throwable e) {
            completionObservable.onError(e);
        }
    }

    public Observable<Void> asObservable() {
        return completionObservable;
    }

}
