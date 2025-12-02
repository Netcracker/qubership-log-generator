/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.log.http;

import org.qubership.log.generator.Generator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ApplicationHttpServer {

    public static void startHTTPServer(PrometheusMeterRegistry prometheusRegistry) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/customLogEditorPage", new StartPageHandler());
            server.createContext("/editor/editLogs", new PostRequestHandler());
            server.createContext("/metrics", httpExchange -> {
                String response = prometheusRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            new Thread(server::start).start();
            IO.println("Server started on port 8080");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class StartPageHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equalsIgnoreCase("GET")) {
                exchange.getResponseHeaders().set(Constants.CONTENTTYPE, Constants.TEXTHTML);
                exchange.sendResponseHeaders(200, 0);
                OutputStream responseBody = exchange.getResponseBody();
                responseBody.write(Constants.getLogEditorPage());
                responseBody.close();
            } else {
                new NotImplementedHandler().handle(exchange);
            }
        }
    }

    static class PostRequestHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equalsIgnoreCase("POST")) {
                String body = new BufferedReader(
                        new InputStreamReader(
                                exchange.getRequestBody()
                        )).lines().collect(Collectors.joining("\n"));
                exchange.getResponseHeaders().set(Constants.CONTENTTYPE, Constants.APPLICATIONJSON);
                exchange.getResponseHeaders().set("Location", "/customLogEditorPage");
                exchange.sendResponseHeaders(302, -1);
                OutputStream responseBody = exchange.getResponseBody();
                Generator.parseAndSendMessages(body);
                responseBody.write(Constants.getLogEditorPage());
                responseBody.close();
            } else {
                new NotImplementedHandler().handle(exchange);
            }
        }
    }

    static class Constants {
        static final String TEXTHTML = "text/html";
        static final String CONTENTTYPE = "Content-Type";
        static final String APPLICATIONJSON = "application/json";

        static byte[] getLogEditorPage() throws IOException {
            String content = new String(
                    Files.readAllBytes(Path.of("./static/customLogEditorPage.html").toRealPath())
            );
            return content.getBytes();
        }
    }

    static class NotImplementedHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(501, -1);
            exchange.close();
        }
    }
}
