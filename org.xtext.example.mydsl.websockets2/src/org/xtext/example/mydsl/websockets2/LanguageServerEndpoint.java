package org.xtext.example.mydsl.websockets2;

import java.util.LinkedHashMap;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethodProvider;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;

import com.google.inject.Inject;

/**
 * Web socket endpoint for language servers including the diagram extension.
 */
public class LanguageServerEndpoint extends Endpoint {
	
	@Inject 
	private LanguageServer languageServer;
	
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		 final LinkedHashMap<String, JsonRpcMethod> supportedMethods = new LinkedHashMap<String, JsonRpcMethod>();
	    supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(LanguageClient.class));
	    if ((this.languageServer instanceof JsonRpcMethodProvider)) {
	      supportedMethods.putAll(((JsonRpcMethodProvider)this.languageServer).supportedMethods());
	    }
		final MessageJsonHandler jsonHandler = new MessageJsonHandler(supportedMethods);
		final WebSocketMessageConsumer outgoingMessageStream = new WebSocketMessageConsumer(session.getAsyncRemote(), jsonHandler);
		final RemoteEndpoint serverEndpoint = new RemoteEndpoint(outgoingMessageStream, ServiceEndpoints.toEndpoint(languageServer));
		jsonHandler.setMethodProvider(serverEndpoint);
		final StreamMessageProducer incomingMessageStream = new StreamMessageProducer(null, jsonHandler);
		session.addMessageHandler(new LanguageMessageHandler(incomingMessageStream, serverEndpoint));
		
		final LanguageClient remoteProxy = ServiceEndpoints.toServiceObject(serverEndpoint, LanguageClient.class);
		if (languageServer instanceof LanguageClientAware) {
			((LanguageClientAware)languageServer).connect(remoteProxy);
		}
	}
	
}