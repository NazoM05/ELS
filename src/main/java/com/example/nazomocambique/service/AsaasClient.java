package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.nazomocambique.config.AsaasProperties;

/** Cliente REST do Asaas. A API Key é enviada exclusivamente no header access_token. */
@Service
public class AsaasClient implements GatewayPagamento {
    private static final Logger log = LoggerFactory.getLogger(AsaasClient.class);
    private static final ParameterizedTypeReference<Map<String,Object>> MAP = new ParameterizedTypeReference<>() {};
    private final AsaasProperties props;
    private final RestClient http;

    public AsaasClient(AsaasProperties props) {
        this.props = props;
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(props.timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(props.timeout());
        this.http = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(factory).build();
    }

    @Override public Pix criarPix(SolicitacaoPix s) { return criarPix(s, false); }

    /** Se a conta ainda não tem chave Pix, cria uma chave aleatória (EVP) e tenta de novo uma única vez. */
    private Pix criarPix(SolicitacaoPix s, boolean jaTentouCriarChave) {
        try {
            String customerReference = "NAZO-" + s.referencia();
            Map<String,Object> customerResp = localizarCliente(customerReference);
            if (customerResp == null) {
                Map<String,Object> customer = new LinkedHashMap<>();
                customer.put("name", s.nome() == null || s.nome().isBlank() ? "Cliente Nazo" : s.nome().trim());
                if (s.email() != null && !s.email().isBlank()) customer.put("email", s.email().trim());
                String cpf = s.cpf() == null ? "" : s.cpf().replaceAll("\\D", "");
                if (cpf.length() == 11 || cpf.length() == 14) customer.put("cpfCnpj", cpf);
                customer.put("externalReference", customerReference);
                customerResp = http.post().uri("/customers")
                    .headers(h -> h.set("access_token", props.apiKey())).contentType(MediaType.APPLICATION_JSON)
                    .body(customer).retrieve().body(MAP);
            }
            String customerId = text(customerResp == null ? null : customerResp.get("id"));
            if (customerId.isBlank()) throw new PagamentoIndisponivelException("O Asaas não retornou o cadastro do cliente.");

            // Clientes já existentes no Asaas podem ter sido criados antes da coleta obrigatória do CPF.
            String cpfInformado = s.cpf() == null ? "" : s.cpf().replaceAll("\\D", "");
            String cpfNoAsaas = text(customerResp.get("cpfCnpj")).replaceAll("\\D", "");
            if ((cpfNoAsaas.length() != 11 && cpfNoAsaas.length() != 14)
                    && (cpfInformado.length() == 11 || cpfInformado.length() == 14)) {
                Map<String,Object> atualizacao = new LinkedHashMap<>();
                atualizacao.put("cpfCnpj", cpfInformado);
                http.put().uri("/customers/{id}", customerId)
                    .headers(h -> h.set("access_token", props.apiKey()))
                    .contentType(MediaType.APPLICATION_JSON).body(atualizacao).retrieve().toBodilessEntity();
            }

            Map<String,Object> payment = localizarCobranca(s.referencia());
            if (payment == null) {
                Map<String,Object> charge = new LinkedHashMap<>();
                charge.put("customer", customerId);
                charge.put("billingType", "PIX");
                charge.put("value", s.valor().setScale(2, RoundingMode.HALF_UP));
                charge.put("dueDate", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString());
                charge.put("description", s.descricao());
                charge.put("externalReference", s.referencia());
                payment = http.post().uri("/payments")
                    .headers(h -> h.set("access_token", props.apiKey())).contentType(MediaType.APPLICATION_JSON)
                    .body(charge).retrieve().body(MAP);
            }
            String id = text(payment == null ? null : payment.get("id"));
            if (id.isBlank()) throw new PagamentoIndisponivelException("O Asaas não retornou o identificador da cobrança.");

            Map<String,Object> qr = http.get().uri("/payments/{id}/pixQrCode", id)
                .headers(h -> h.set("access_token", props.apiKey())).retrieve().body(MAP);
            String payload = text(qr == null ? null : qr.get("payload"));
            String encoded = text(qr == null ? null : qr.get("encodedImage"));
            if (payload.isBlank()) throw new PagamentoIndisponivelException("O Asaas não retornou o Pix Copia e Cola.");
            OffsetDateTime expiry;
            try {
                String rawExpiry = text(qr.get("expirationDate"));
                try { expiry = OffsetDateTime.parse(rawExpiry); }
                catch (RuntimeException ignored) { expiry = LocalDateTime.parse(rawExpiry, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.of("America/Sao_Paulo")).toOffsetDateTime(); }
            } catch (RuntimeException ex) { expiry = OffsetDateTime.now(ZoneId.of("America/Sao_Paulo")).plusMinutes(props.pixExpiracaoMinutos()); }
            return new Pix(id, text(payment.get("status")), payload, encoded, expiry);
        } catch (RestClientResponseException e) {
            if (!jaTentouCriarChave && semChavePix(e)) {
                log.warn("A conta Asaas não tem chave Pix; criando uma chave aleatória (EVP) e repetindo o pedido {}.", s.referencia());
                criarChaveEvp();
                return criarPix(s, true);
            }
            log.warn("Asaas recusou a cobrança do pedido {}: HTTP {} - {}", s.referencia(), e.getStatusCode().value(), resumo(e.getResponseBodyAsString()));
            throw new PagamentoIndisponivelException("Não foi possível gerar o Pix no Asaas. Confira os dados do cliente e tente novamente.", e);
        } catch (RestClientException e) {
            log.warn("Falha de comunicação com Asaas ao gerar Pix do pedido {}: {}", s.referencia(), e.getMessage());
            throw new PagamentoIndisponivelException("Não foi possível comunicar com o Asaas agora.", e);
        }
    }

    private static boolean semChavePix(RestClientResponseException e) {
        String corpo = e.getResponseBodyAsString();
        return e.getStatusCode().value() == 400 && corpo != null && corpo.toLowerCase().contains("chave pix");
    }

    private void criarChaveEvp() {
        try {
            http.post().uri("/pix/addressKeys").headers(h -> h.set("access_token", props.apiKey()))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("type", "EVP")).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Não foi possível criar a chave Pix EVP no Asaas: HTTP {} - {}", e.getStatusCode().value(), resumo(e.getResponseBodyAsString()));
            throw new PagamentoIndisponivelException("A conta Asaas não tem chave Pix e não foi possível criá-la automaticamente.", e);
        } catch (RestClientException e) {
            throw new PagamentoIndisponivelException("Não foi possível comunicar com o Asaas agora.", e);
        }
    }

    private Map<String,Object> localizarCliente(String externalReference) {
        Map<String,Object> response = http.get().uri(uri -> uri.path("/customers").queryParam("externalReference", externalReference).build())
            .headers(h -> h.set("access_token", props.apiKey())).retrieve().body(MAP);
        return primeiro(response);
    }

    private Map<String,Object> localizarCobranca(String externalReference) {
        Map<String,Object> response = http.get().uri(uri -> uri.path("/payments").queryParam("externalReference", externalReference).build())
            .headers(h -> h.set("access_token", props.apiKey())).retrieve().body(MAP);
        return primeiro(response);
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> primeiro(Map<String,Object> response) {
        if (response == null || !(response.get("data") instanceof List<?> data) || data.isEmpty()) return null;
        Object first = data.get(0);
        return first instanceof Map<?,?> map ? (Map<String,Object>) map : null;
    }

    @Override public Optional<Pagamento> consultar(String paymentId) {
        try {
            Map<String,Object> r = http.get().uri("/payments/{id}", paymentId)
                .headers(h -> h.set("access_token", props.apiKey())).retrieve().body(MAP);
            if (r == null || text(r.get("id")).isBlank()) return Optional.empty();
            BigDecimal value = r.get("value") instanceof Number n ? new BigDecimal(n.toString()).setScale(2, RoundingMode.HALF_UP) : null;
            return Optional.of(new Pagamento(text(r.get("id")), text(r.get("status")), text(r.get("externalReference")), value));
        } catch (HttpClientErrorException.NotFound e) { return Optional.empty(); }
        catch (RestClientException e) { throw new PagamentoIndisponivelException("Falha ao consultar a cobrança no Asaas.", e); }
    }

    @Override public void estornar(String paymentId) {
        try {
            http.post().uri("/payments/{id}/refund", paymentId).headers(h -> h.set("access_token", props.apiKey()))
                .contentType(MediaType.APPLICATION_JSON).retrieve().toBodilessEntity();
        } catch (RestClientException e) { throw new PagamentoIndisponivelException("Não foi possível solicitar o estorno no Asaas.", e); }
    }
    private static String text(Object o) { return o == null ? "" : o.toString().trim(); }
    private static String resumo(String s) { return s == null ? "" : s.substring(0, Math.min(s.length(), 350)); }
}
