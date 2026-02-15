package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class SubscriptionService {

    private final UsuarioRepository usuarioRepository;

    public SubscriptionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void activateSubscription(Long usuarioId, String stripeCustomerId, String stripeSubscriptionId,
                                     String status, Instant currentPeriodEnd) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuarioId));
        usuario.setStripeCustomerId(stripeCustomerId);
        usuario.setStripeSubscriptionId(stripeSubscriptionId);
        usuario.setSubscriptionStatus(status);
        usuario.setSubscriptionCurrentPeriodEnd(toLocalDateTime(currentPeriodEnd));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void updateSubscription(String stripeSubscriptionId, String status, Instant currentPeriodEnd) {
        Usuario usuario = usuarioRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElse(null);
        if (usuario == null) return;
        usuario.setSubscriptionStatus(status);
        usuario.setSubscriptionCurrentPeriodEnd(toLocalDateTime(currentPeriodEnd));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void cancelSubscription(String stripeSubscriptionId) {
        Usuario usuario = usuarioRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElse(null);
        if (usuario == null) return;
        usuario.setSubscriptionStatus("canceled");
        usuario.setStripeSubscriptionId(null);
        usuarioRepository.save(usuario);
    }

    public boolean hasActiveSubscription(Usuario usuario) {
        if (usuario == null) return false;
        if (!"active".equals(usuario.getSubscriptionStatus()) && !"trialing".equals(usuario.getSubscriptionStatus())) {
            return false;
        }
        LocalDateTime periodEnd = usuario.getSubscriptionCurrentPeriodEnd();
        return periodEnd != null && periodEnd.isAfter(LocalDateTime.now());
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
