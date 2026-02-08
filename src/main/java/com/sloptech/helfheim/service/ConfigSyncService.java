package com.sloptech.helfheim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigSyncService {

    private final CoreService coreService;

    @Scheduled(fixedDelay = 120000)
    public void ensureConsistency() {
        try {
            log.info("Запуск плановой синхронизации конфигураций WireGuard для устранения расхождений");
            coreService.regenerateAndApplyWireGuardConfig();
            log.info("Плановая синхронизация конфигураций WireGuard выполнена успешно");
        } catch (Exception e) {
            log.error("Ошибка плановой синхронизации", e);
        }
    }
}