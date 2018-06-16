package org.xtext.example.mydsl.websockets2;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.eclipse.xtext.ide.server.ServerModule;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class MyDslWebsocketServer {

	private static final int PORT = 8080;

	public static class DebuggableServerEndpoint extends LanguageServerEndpoint {
		@Override
		public void onOpen(Session session, EndpointConfig config) {
			super.onOpen(session, config);
		}

		@Override
		public void onError(Session session, Throwable thr) {
			super.onError(session, thr);
		}
	}

	public static class InjectingEndpointConfigurator extends ServerEndpointConfig.Configurator {

		@Inject
		Injector injector;

		public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
			T instance = super.getEndpointInstance(endpointClass);
			DebuggableServerEndpoint endpoint = (DebuggableServerEndpoint) instance;
			injector.injectMembers(endpoint);
			return instance;
		}
	}

	public static void main(String[] args) {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(PORT);
		server.addConnector(connector);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);

		try {
			ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
			Injector injector = Guice.createInjector(new ServerModule());

			ServerEndpointConfig.Builder endpointConfigBuilder = ServerEndpointConfig.Builder
					.create(DebuggableServerEndpoint.class, "/lsp");
			endpointConfigBuilder.configurator(injector.getInstance(InjectingEndpointConfigurator.class));
			wscontainer.addEndpoint(endpointConfigBuilder.subprotocols(Lists.newArrayList("mydsl")).build());

			server.start();
			server.dump(System.err);
			server.join();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}