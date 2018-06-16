package org.xtext.example.mydsl.websockets2;

import java.io.ByteArrayOutputStream;

import javax.websocket.RemoteEndpoint;

import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer;
import org.eclipse.lsp4j.jsonrpc.messages.Message;

/**
 * LSP4J message consumer that forwards messages to a web socket.
 */
public class WebSocketMessageConsumer extends StreamMessageConsumer {
	
	final RemoteEndpoint.Async remote;
	
	public WebSocketMessageConsumer(RemoteEndpoint.Async remote, MessageJsonHandler jsonHandler) {
		super(new ByteArrayOutputStream(), jsonHandler);
		this.remote = remote;
	}
	
	WebSocketMessageConsumer(RemoteEndpoint.Async remote, String encoding, MessageJsonHandler jsonHandler) {
		super(new ByteArrayOutputStream(), encoding, jsonHandler);
		this.remote = remote;
	}
	
	public void consume(Message message) {
		super.consume(message);
		final ByteArrayOutputStream out = (ByteArrayOutputStream) getOutput();
		remote.sendText(out.toString());
		out.reset();
	}
	
}