package org.cdp_sample;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonMappingException;

public class Main extends WebSocketServer {
    private ObjectMapper mapper;
    private int count;
    public Main(InetSocketAddress address) {
        super(address);
        this.mapper = new ObjectMapper();
        this.count = 0;
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 9090;

        WebSocketServer server = new Main(new InetSocketAddress(host, port));
        server.run();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake clientHandshake) {
    }

    @Override
    public void onClose(WebSocket conn, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket conn, String s) {
        JsonRpcRequest req = parseJson(s);
        Map<String, Object> map = new HashMap<>();
        JsonRpcResponse res = new JsonRpcResponse();
        String json;
        switch (req.getMethod()) {
            case "Debugger.enable":
                res.setId(req.getId());
                res.setResult(map);
                json = convToJson(res);
                conn.send(json);
                String scriptParsed = createDummyScriptParsed();
                conn.send(scriptParsed);
                String paused = createDummyPaused();
                conn.send(paused);
                break;
            case "Debugger.stepOver":
            case "Debugger.stepInto":
            case "Debugger.resume":
                res.setId(req.getId());
                res.setResult(map);
                json = convToJson(res);
                conn.send(json);
                this.count ++;
                String pausedAgain = createDummyPaused();
                conn.send(pausedAgain);
                break;
            case "Debugger.getScriptSource":
                Path p = Paths.get("src/main/java/org/cdp_sample/sample.txt");
                try {
                    String text = Files.readString(p);
                    res.setId(req.getId());
                    map.put("scriptSource", text);
                    res.setResult(map);
                    json = convToJson(res);
                    conn.send(json);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    private String createDummyScriptParsed() {
        JsonRpcEvent evt = new JsonRpcEvent();
        Map<String, Object> map = new HashMap<>();
        evt.setMethod("Debugger.scriptParsed");
        map.put("scriptId", 1);
        map.put("url", "http://example.com/sample.txt");
        map.put("startLine", 0);
        map.put("endLine", 0);
        evt.setParams(map);
        return convToJson(evt);
    }

    private String createDummyPaused() {
        JsonRpcEvent evt = new JsonRpcEvent();
        Map<String, Object> params = new HashMap<>();
        evt.setMethod("Debugger.paused");
        params.put("reason", "other");
        Object[] callFrames = new Object[1];
        Map<String, Object> callFrame = new HashMap<>();
        callFrame.put("callFrameId", "2c6720c3f1d0fc4e324d10c64cb322ff");
        callFrame.put("functionName", "<main>");
        Map<String, Object> location = new HashMap<>();
        location.put("lineNumber", this.count);
        location.put("scriptId", 1);
        callFrame.put("location", location);
        callFrame.put("url","http://example.com/sample.txt");
        Map<String, Object> th = new HashMap<>();
        th.put("type", "object");
        callFrame.put("this", th);
        Object[] scopeChain = new Object[1];
        Map<String, Object> scope = new HashMap<>();
        scope.put("type", "local");
        Map<String, Object> object = new HashMap<>();
        object.put("type", "undefined");
        scope.put("object", object);
        scopeChain[0] = scope;
        callFrame.put("scopeChain", scopeChain);
        callFrames[0] = callFrame;
        params.put("callFrames", callFrames);
        evt.setParams(params);
        return convToJson(evt);
    }

    private JsonRpcRequest parseJson(String s) {
        try {
            return mapper.readValue(s, JsonRpcRequest.class);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String convToJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
//        System.err.println("an error occurred on connection " + webSocket.getRemoteSocketAddress()  + ":" + e);
    }

    @Override
    public void onStart() {
        System.out.println("server started!");
    }
}

class JsonRpcRequest {
    private int id;
    private String method;
    private JsonNode params;
    public int getId() {
        return id;
    }
    public String getMethod() {
        return method;
    }
    public JsonNode getParams() {
        return  params;
    }
}

class JsonRpcResponse {
    @JsonProperty("id")
    private int id;
    @JsonProperty("result")
    private Object result;
    public void setId(int id) {
        this.id = id;
    }
    public void setResult(Object result) {
        this.result = result;
    }
}

class JsonRpcEvent {
    @JsonProperty("method")
    private String method;
    @JsonProperty("params")
    private Object params;
    public void setMethod(String method) {
        this.method = method;
    }
    public void setParams(Object params) {
        this.params = params;
    }
}

