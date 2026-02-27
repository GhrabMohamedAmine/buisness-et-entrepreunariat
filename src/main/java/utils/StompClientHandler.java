package utils;

import javafx.application.Platform;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * STOMP-over-WebSocket client for JavaFX.
 * Uses SockJS transport because the server endpoint is registered with .withSockJS().
 */
public class StompClientHandler {

    private volatile StompSession stompSession;
    private final String serverUrl;
    private final int currentUserId;

    private volatile boolean connected = false;

    public StompClientHandler(String serverUrl, int currentUserId) {
        this.serverUrl = serverUrl;
        this.currentUserId = currentUserId;
    }

    public void connect(StompSessionConnectCallback callback, IncomingCallHandler handler) {
        new Thread(() -> runConnect(callback, handler), "STOMP-Connect-" + currentUserId).start();
    }

    private void runConnect(StompSessionConnectCallback callback, IncomingCallHandler handler) {
        try {
            WebSocketStompClient stompClient = new WebSocketStompClient(
                    new SockJsClient(
                            Collections.singletonList(
                                    new WebSocketTransport(new StandardWebSocketClient())
                            )
                    )
            );

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

            stompClient.connect(serverUrl, headers, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    Platform.runLater(() -> {
                        stompSession = session;
                        connected = true;
                        if (callback != null) callback.onConnected(session);
                    });
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    Platform.runLater(() -> {
                        connected = false;
                        System.err.println("STOMP transport error: " + exception.getMessage());
                        exception.printStackTrace();
                    });
                }

                @Override
                public void handleException(StompSession session, StompCommand command,
                                            StompHeaders headers, byte[] payload, Throwable exception) {
                    Platform.runLater(() -> {
                        System.err.println("STOMP exception: " + exception.getMessage());
                        exception.printStackTrace();
                    });
                }
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                connected = false;
                System.err.println("STOMP connect init error: " + e.getMessage());
                e.printStackTrace();
            });
        }
    }

    public boolean isConnected() {
        return connected && stompSession != null && stompSession.isConnected();
    }

    /** Subscribe and receive raw UTF-8 payload text. */
    public StompSession.Subscription subscribeRaw(String destination, IncomingCallHandler handler) {
        if (stompSession == null || !stompSession.isConnected()) return null;

        return stompSession.subscribe(destination, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return byte[].class; }

            @Override public void handleFrame(StompHeaders headers, Object payload) {
                if (payload == null) return;

                String msg = (payload instanceof byte[])
                        ? new String((byte[]) payload, StandardCharsets.UTF_8)
                        : payload.toString();

                if (handler != null) handler.onIncomingCall(msg);
            }
        });
    }

    /** Send raw bytes to STOMP destination (e.g. /app/call.start). */
    public void send(String destination, byte[] payload) {
        if (!isConnected()) {
            System.err.println("STOMP not connected: cannot send to " + destination);
            return;
        }
        try {
            stompSession.send(destination, payload);
        } catch (Exception e) {
            System.err.println("STOMP send error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public interface StompSessionConnectCallback {
        void onConnected(StompSession session);
    }

    public interface IncomingCallHandler {
        void onIncomingCall(String messagePayload);
    }
}
