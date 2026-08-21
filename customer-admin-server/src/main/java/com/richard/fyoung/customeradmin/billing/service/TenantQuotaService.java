package com.richard.fyoung.customeradmin.billing.service;

import com.richard.fyoung.customeradmin.billing.config.QuotaGatewayProvider;
import com.richard.fyoung.customeradmin.billing.dto.TenantQuotaSaveRequest;
import com.richard.fyoung.customeradmin.billing.dto.TenantQuotaVO;
import com.richard.fyoung.customerwork.safety.quota.MybatisTenantQuotaStore;
import com.richard.fyoung.customerwork.safety.quota.QuotaExceedAction;
import com.richard.fyoung.customerwork.safety.quota.QuotaPeriod;
import com.richard.fyoung.customerwork.safety.quota.TenantQuota;
import com.richard.fyoung.customerwork.safety.quota.TenantQuotaStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

/**
 * 后台的租户配额维护：直接读写客服端库的 {@code cw_tenant_quota}，客服端轮询/实时读取即生效。
 *
 * <p>复用 starter 的 {@link MybatisTenantQuotaStore} 而不是重写一套 CRUD——同一张表、同一套语义，
 * 重写只会多出一份要同步维护的代码（照内容风控"写侧复用 starter Mapper"的先例）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface TenantQuotaService {

    public abstract List<TenantQuotaVO> listByTenant(String tenantId);

    public abstract void save(TenantQuotaSaveRequest request);

    public abstract void delete(String tenantId, String period);
}
