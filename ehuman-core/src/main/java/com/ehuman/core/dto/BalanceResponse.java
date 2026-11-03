package com.ehuman.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 账户余额信息响应 DTO
 */
@Data
public class BalanceResponse {

    @JsonProperty("is_available")
    private Boolean isAvailable;

    @JsonProperty("balance_infos")
    private List<BalanceInfo> balanceInfos;

    @Data
    public static class BalanceInfo {
        @JsonProperty("currency")
        private String currency;

        @JsonProperty("total_balance")
        private String totalBalance;

        @JsonProperty("granted_balance")
        private String grantedBalance;

        @JsonProperty("topped_up_balance")
        private String toppedUpBalance;
    }
}