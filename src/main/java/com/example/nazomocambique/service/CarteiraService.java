package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.nazomocambique.model.*;
import com.example.nazomocambique.repository.*;

@Service
public class CarteiraService {
 private final MovimentoCarteiraRepository movimentos;
 private final ConfiguracaoCashbackRepository configuracoes;
 private final UsuarioRepository usuarios;
 public CarteiraService(MovimentoCarteiraRepository m, ConfiguracaoCashbackRepository c, UsuarioRepository u){movimentos=m;configuracoes=c;usuarios=u;}
 @Transactional public BigDecimal saldo(Long usuarioId){
  expirarCashbackVencido(usuarioId);
  return movimentos.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream().map(MovimentoCarteira::getValor).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
 }
 @Transactional public List<MovimentoCarteira> historico(Long usuarioId){
  expirarCashbackVencido(usuarioId);
  return movimentos.findByUsuarioIdOrderByCriadoEmDesc(usuarioId);
 }

 /** Cashback de compra expira após 90 dias corridos; o lançamento de expiração é idempotente. */
 private void expirarCashbackVencido(Long usuarioId) {
  LocalDateTime limite = LocalDateTime.now().minusDays(90);
  List<MovimentoCarteira> historico = movimentos.findByUsuarioIdOrderByCriadoEmDesc(usuarioId);
  BigDecimal disponivel = historico.stream().map(MovimentoCarteira::getValor)
      .reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO);
  // Processa os créditos antigos primeiro e nunca deixa a carteira com saldo negativo.
  for (int i = historico.size() - 1; i >= 0; i--) {
   MovimentoCarteira m = historico.get(i);
   String ref = m.getReferencia();
   if ("CREDITO".equals(m.getTipo()) && ref != null && ref.startsWith("CASHBACK-PEDIDO-")
       && m.getValor().signum() > 0 && m.getCriadoEm().isBefore(limite)) {
    String expiracao = "EXPIRACAO-" + ref;
    if (movimentos.findByReferencia(expiracao).isEmpty()) {
     BigDecimal vencido = m.getValor().min(disponivel);
     if (vencido.signum() > 0) {
      movimentos.save(new MovimentoCarteira(usuarios.getReferenceById(usuarioId), vencido.negate(),
        "EXPIRACAO", expiracao, "Cashback expirado após 90 dias sem utilização"));
      disponivel = disponivel.subtract(vencido);
     }
    }
   }
  }
 }
 @Transactional public BigDecimal creditarCashback(Long usuarioId, BigDecimal base, String referencia){
  if(movimentos.findByReferencia(referencia).isPresent()) return BigDecimal.ZERO;
  ConfiguracaoCashback c=configuracoes.findById(1L).orElseGet(()->configuracoes.save(new ConfiguracaoCashback()));
  if(!c.isAtivo() || base.compareTo(c.getMinimoCompra())<0) return BigDecimal.ZERO;
  BigDecimal valor=base.multiply(c.getPercentual()).divide(new BigDecimal("100"),2,RoundingMode.DOWN);
  if(c.getLimitePorCompra()!=null && c.getLimitePorCompra().signum()>0) valor=valor.min(c.getLimitePorCompra());
  if(valor.signum()>0) movimentos.save(new MovimentoCarteira(usuarios.getReferenceById(usuarioId),valor,"CREDITO",referencia,"Cashback do pedido"));
  return valor;
 }
 @Transactional public void debitar(Long usuarioId, BigDecimal valor, String referencia){
  if(valor==null || valor.signum()<=0) return;
  if(movimentos.findByReferencia(referencia).isPresent()) return;
  if(saldo(usuarioId).compareTo(valor)<0) throw new IllegalArgumentException("Saldo insuficiente na carteira.");
  movimentos.save(new MovimentoCarteira(usuarios.getReferenceById(usuarioId),valor.negate(),"DEBITO",referencia,"Utilização de cashback no checkout"));
 }
 @Transactional public void estornarCashback(Long usuarioId, BigDecimal valor, String referencia){
  String chave="ESTORNO-"+referencia;
  if(movimentos.findByReferencia(chave).isEmpty() && valor!=null && valor.signum()>0)
   movimentos.save(new MovimentoCarteira(usuarios.getReferenceById(usuarioId),valor.negate(),"ESTORNO",chave,"Estorno do cashback do pedido"));
 }
 @Transactional public void tratarCancelamentoPedido(Long usuarioId, Long pedidoId, BigDecimal cashbackUsado){
  String credito="CASHBACK-PEDIDO-"+pedidoId;
  movimentos.findByReferencia(credito).ifPresent(m->{String ref="ESTORNO-CASHBACK-PEDIDO-"+pedidoId;if(movimentos.findByReferencia(ref).isEmpty())movimentos.save(new MovimentoCarteira(usuarios.getReferenceById(usuarioId),m.getValor().negate(),"ESTORNO",ref,"Reversão do cashback de pedido cancelado"));});
  if(cashbackUsado!=null && cashbackUsado.signum()>0){String ref="DEVOLUCAO-CASHBACK-PEDIDO-"+pedidoId;if(movimentos.findByReferencia(ref).isEmpty())movimentos.save(new MovimentoCarteira(usuarios.getReferenceById(usuarioId),cashbackUsado,"CREDITO",ref,"Devolução do cashback usado em pedido cancelado"));}
 }

}
