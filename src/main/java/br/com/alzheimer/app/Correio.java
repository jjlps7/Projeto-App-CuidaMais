package br.com.alzheimer.app;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.UUID;

final class Correio {
    static void enviar(String destino,String assunto,String token) {
        String base = System.getenv().getOrDefault("CUIDA_URL","http://localhost:"+Integer.getInteger("cuida.porta",8080));
        String mensagem = assunto + "\n\nAbra este link em até 30 minutos:\n" + base + "/validar?token=" + token;
        String chave = System.getenv("RESEND_API_KEY");
        try {
            if (chave == null || chave.isBlank()) {
                Path caixa = Path.of(System.getProperty("cuida.caixa","dados/emails"));
                Files.createDirectories(caixa);
                Files.writeString(caixa.resolve(System.currentTimeMillis()+"-"+UUID.randomUUID()+".txt"),
                    "Para: "+destino+"\n"+mensagem,StandardOpenOption.CREATE_NEW);
                return;
            }
            String de = System.getenv("CUIDA_REMETENTE");
            if(de==null || de.isBlank()) throw new IOException("Remetente ausente");
            String json = "{\"from\":"+json(de)+",\"to\":["+json(destino)+"],\"subject\":"+json(assunto)+",\"text\":"+json(mensagem)+"}";
            var pedido=HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(8)).header("Authorization","Bearer "+chave)
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
            var resposta=HttpClient.newHttpClient().send(pedido,HttpResponse.BodyHandlers.discarding());
            if(resposta.statusCode()/100!=2) throw new IOException("Envio recusado");
        } catch(IOException | InterruptedException e) {
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("O serviço de e-mail está indisponível.",e);
        }
    }
    private static String json(String s) {
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r") + "\"";
    }
}
