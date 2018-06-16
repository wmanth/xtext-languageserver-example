package org.xtext.example.mydsl.websockets2;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.MessageHandler;

import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;

public class LanguageMessageHandler implements MessageHandler.Partial<String> {

	public static class PartialMessageInputStream extends FilterInputStream {

		private final List<byte[]> messages;
		private int currentMessageIndex = 0;

		public PartialMessageInputStream(final List<byte[]> messages) {
			super(new ByteArrayInputStream(messages.get(0)));
			this.messages = messages;
		}

		protected boolean nextMessage() {
			currentMessageIndex++;
			if (currentMessageIndex < messages.size()) {
				in = new ByteArrayInputStream(messages.get(currentMessageIndex));
				return true;
			} else {
				return false;
			}
		}

		@Override
		public int available() throws IOException {
			final int current = super.available();
			if (current <= 0 && nextMessage()) {
				return super.available();
			} else {
				return current;
			}
		}

		@Override
		public int read() throws IOException {
			final int current = super.read();
			if (current < 0 && nextMessage()) {
				return super.read();
			} else {
				return current;
			}
		}

		@Override
		public int read(final byte[] b) throws IOException {
			final int current = super.read(b);
			if (current <= 0 && nextMessage()) {
				return super.read(b);
			} else {
				return current;
			}
		}

		@Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
			final int current = super.read(b, off, len);
			if (current <= 0 && nextMessage()) {
				return super.read(b, off, len);
			} else {
				return current;
			}
		}

		@Override
		public boolean markSupported() {
			return false;
		}
	}

	private final StreamMessageProducer streamMessageProducer;
	private final RemoteEndpoint serverEndpoint;
	private final List<byte[]> messages = new ArrayList<byte[]>();

	@Override
	public void onMessage(final String partialMessage, final boolean last) {
		if (partialMessage.length() > 0) {
			messages.add(partialMessage.getBytes(StandardCharsets.UTF_8));
		}
		if (last && !messages.isEmpty()) {
			streamMessageProducer.setInput(new LanguageMessageHandler.PartialMessageInputStream(messages));
			streamMessageProducer.listen(serverEndpoint);
			messages.clear();
		}
	}

	public LanguageMessageHandler(final StreamMessageProducer messageProducer, final RemoteEndpoint serverEndpoint) {
		super();
		this.streamMessageProducer = messageProducer;
		this.serverEndpoint = serverEndpoint;
	}
}
