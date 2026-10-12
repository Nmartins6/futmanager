package com.futmanager.service;

import com.futmanager.config.TenantContext;
import com.futmanager.domain.entity.SchoolConfig;
import com.futmanager.domain.entity.Tenant;
import com.futmanager.dto.SchoolConfigDto.SchoolConfigRequest;
import com.futmanager.dto.SchoolConfigDto.SchoolConfigResponse;
import com.futmanager.repository.SchoolConfigRepository;
import com.futmanager.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolConfigService {

    private final SchoolConfigRepository configRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public SchoolConfigResponse getConfigs() {
        return new SchoolConfigResponse(
                getVal("school_name", "Escolinha de Futebol"),
                getVal("cnpj", ""),
                getVal("phone", ""),
                getVal("address", ""),
                getVal("timezone", "America/Sao_Paulo"),
                Integer.parseInt(getVal("lesson_horizon_days", "30")),
                Double.parseDouble(getVal("low_frequency_threshold", "75.0"))
        );
    }

    @Transactional
    public SchoolConfigResponse updateConfigs(SchoolConfigRequest request) {
        setVal("school_name", request.schoolName());
        setVal("cnpj", request.cnpj() != null ? request.cnpj() : "");
        setVal("phone", request.phone() != null ? request.phone() : "");
        setVal("address", request.address() != null ? request.address() : "");
        setVal("timezone", request.timezone());
        setVal("lesson_horizon_days", String.valueOf(request.lessonHorizonDays()));
        setVal("low_frequency_threshold", String.valueOf(request.lowFrequencyThreshold()));

        return getConfigs();
    }

    public String getTimezone() {
        return getVal("timezone", "America/Sao_Paulo");
    }

    public int getLessonHorizonDays() {
        return Integer.parseInt(getVal("lesson_horizon_days", "30"));
    }

    public double getLowFrequencyThreshold() {
        return Double.parseDouble(getVal("low_frequency_threshold", "75.0"));
    }

    private String getVal(String key, String defaultValue) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return configRepository.findByTenantIdAndConfigKey(tenantId, key)
                    .map(SchoolConfig::getConfigValue)
                    .orElse(defaultValue);
        }
        return defaultValue;
    }

    private void setVal(String key, String value) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return;
        }

        SchoolConfig config = configRepository.findByTenantIdAndConfigKey(tenantId, key)
                .orElse(SchoolConfig.builder()
                        .tenant(tenant)
                        .configKey(key)
                        .build());

        config.setConfigValue(value);
        configRepository.save(config);
    }
}
